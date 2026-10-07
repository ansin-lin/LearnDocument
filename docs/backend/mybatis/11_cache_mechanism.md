# 第11章 缓存机制

> 本章目标：理解 MyBatis 一级缓存和二级缓存的作用、范围、失效条件和使用注意点。

## 一、为什么需要缓存

如果同一个 SQL 在短时间内重复查询，缓存可以减少数据库访问次数。

但是缓存也可能带来数据不一致问题。

因此学习 MyBatis 缓存时，需要同时理解：

- 缓存能提高什么。
- 缓存什么时候失效。
- 缓存为什么不能随便启用。

判断缓存是否生效，不能只比较两次查询的返回值，因为两次访问数据库也可能得到相同内容。本章统一通过 SQL 日志观察数据库实际执行次数。

缓存只影响“是否需要再次读取数据库”，不会自动修正低效 SQL，也不能代替正确的查询设计。N+1 等 SQL 调用问题请参阅[附录A01 项目常见SQL调用陷阱与Review](appendix/A01_project_sql_call_pitfalls.md)。

## 二、一级缓存

一级缓存是 `SqlSession` 级别缓存。

默认配置下，同一个 `SqlSession` 中再次执行相同语句并传入相同参数时，MyBatis 会优先从一级缓存取得结果，不再访问数据库。

示例：

```java
try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
    EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);

    Employee employee1 = mapper.selectById(1L);
    Employee employee2 = mapper.selectById(1L);

    System.out.println(employee1 == employee2);
}
```

观察 SQL 日志时，`selectById` 应只有一次 `Preparing` 记录。`employee1 == employee2` 通常为 `true`，因为默认的会话级一级缓存可能返回同一个对象引用。

不要修改查询结果对象后，再期待同一 `SqlSession` 中重新查询一定能恢复数据库原值。修改缓存中的对象引用可能影响本会话后续取得的结果；需要重新读取时，应清理缓存或重新设计会话边界。

## 三、一级缓存失效

一级缓存常见失效情况：

| 情况 | 说明 |
| --- | --- |
| `SqlSession` 关闭 | 缓存随会话结束 |
| 执行 insert/update/delete | MyBatis 会清理本地缓存 |
| 调用 `commit()` | 提交时会清理本地缓存 |
| 调用 `rollback()` | 回滚时会清理本地缓存 |
| 调用 `clearCache()` | 手动清理缓存 |
| 不同 `SqlSession` | 缓存不共享 |

一级缓存默认作用于整个会话，对应配置：

```xml
<setting name="localCacheScope" value="SESSION"/>
```

如果改为：

```xml
<setting name="localCacheScope" value="STATEMENT"/>
```

一级缓存只在一次语句执行期间使用，连续调用同一个 Mapper 查询仍会再次访问数据库。`STATEMENT` 不是“彻底关闭一级缓存”，MyBatis 在处理嵌套结果等内部过程时仍需要本地缓存。

| 可接受的值 | 默认值 | 作用 |
| --- | --- | --- |
| `SESSION` | 是 | 同一 `SqlSession` 的多次查询可以复用本地缓存 |
| `STATEMENT` | 否 | 本地缓存只保留到一次语句执行结束 |

## 四、二级缓存

二级缓存是 Mapper namespace 级别缓存。

它可以让同一个 Mapper namespace 下的查询结果在多个 `SqlSession` 之间共享。一级缓存创建会话时就存在；二级缓存需要明确配置后才能使用。

本节需要修改三个位置：

```text
src/main/java/com/example/mybatis/entity/Employee.java
src/main/resources/mybatis-config.xml
src/main/resources/mapper/EmployeeMapper.xml
```

如果查询结果中还包含 `Department` 等嵌套对象，这些对象也要一起检查。

### 4.1 第一步：让查询结果对象可以序列化

本章使用默认的读写二级缓存。先修改 `Employee.java`，让查询结果对象实现 `Serializable`：

```java
package com.example.mybatis.entity;

import java.io.Serializable;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    // 保留前面章节已经创建的字段、getter 和 setter
}
```

如果 `Employee` 中包含 `Department department`，`Department` 也要实现 `Serializable`。否则在缓存包含部门对象的查询结果时，仍可能出现序列化异常。

这里不是要求删除原有字段后只留下示例中的内容，而是在现有实体类声明上增加 `implements Serializable`、导入和 `serialVersionUID`。

### 4.2 第二步：确认全局总开关

打开：

```text
src/main/resources/mybatis-config.xml
```

`cacheEnabled` 是二级缓存的全局总开关，默认值已经是 `true`。为了让本次实验配置更明确，可以把它添加到现有的 `<settings>` 中：

```xml
<settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
    <setting name="logImpl" value="STDOUT_LOGGING"/>
    <setting name="cacheEnabled" value="true"/>
</settings>
```

注意：

- 一个 `mybatis-config.xml` 中只保留一个 `<settings>`。
- 如果文件中已经有 `<settings>`，就在其中追加 `<setting>`，不要再新建第二个 `<settings>`。
- 如果还没有 `<settings>`，应把它放在 `<properties>` 后、`<typeAliases>` 和 `<environments>` 前。MyBatis 主配置标签有固定顺序。
- `cacheEnabled=true` 只是允许使用二级缓存，不会自动为所有 Mapper 创建缓存。

### 4.3 第三步：在 EmployeeMapper.xml 声明缓存

打开：

```text
src/main/resources/mapper/EmployeeMapper.xml
```

把 `<cache/>` 放在根元素 `<mapper>` 的内部，并放在 `<resultMap>`、`<sql>`、`<select>`、`<insert>` 等映射定义之前。它不能写在某个 `<select>` 内部，也不能写到 `mybatis-config.xml` 中。

`EmployeeMapper.xml` 的位置关系如下：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mybatis.mapper.EmployeeMapper">

    <!-- 为当前 EmployeeMapper namespace 声明二级缓存 -->
    <cache/>

    <select id="selectById" parameterType="long" resultType="Employee">
        SELECT id, name, department_id AS departmentId, email
        FROM employees
        WHERE id = #{id}
    </select>

</mapper>
```

这个完整位置关系可以理解为：

```text
<mapper>
    <cache/>
    各种 resultMap、sql 和 CRUD 映射
</mapper>
```

`<cache/>` 只为它所在的 `EmployeeMapper` namespace 声明二级缓存。`DepartmentMapper.xml` 不会因为这里的配置自动获得二级缓存。

只有 `cacheEnabled=true` 而没有 `<cache/>` 时，`EmployeeMapper` 不会自动拥有二级缓存；只有 `<cache/>` 但把全局 `cacheEnabled` 设为 `false` 时，二级缓存也不会生效。

### 4.4 第四步：使用两个 SqlSession 验证

```java
try (SqlSession session1 = sqlSessionFactory.openSession()) {
    EmployeeMapper mapper1 = session1.getMapper(EmployeeMapper.class);
    mapper1.selectById(1L);
}

try (SqlSession session2 = sqlSessionFactory.openSession()) {
    EmployeeMapper mapper2 = session2.getMapper(EmployeeMapper.class);
    mapper2.selectById(1L);
}
```

可以按下面的顺序理解：

1. `session1` 第一次查询数据库。
2. 查询结果先处于当前会话的缓存管理范围内。
3. 会话正常完成后，结果才可以提供给同 namespace 的其他会话。
4. `session2` 使用相同语句和参数查询时，才可能命中二级缓存。

如果第一个会话回滚，未完成的缓存变更不会发布给其他会话。验证二级缓存时必须明确两个 `SqlSession` 的创建、提交或关闭顺序。

观察 SQL 日志：如果两次查询的语句和参数相同，且第一个会话已经正常结束，第二个会话应不再出现相同查询的 `Preparing` 记录。

如果仍然执行 SQL，按下面的顺序检查：

1. `<cache/>` 是否在正确的 Mapper XML 中。
2. `<cache/>` 是否位于 `<mapper>` 内部、其他映射定义之前。
3. 两次查询是否属于同一个 namespace。
4. 两次查询的语句和参数是否相同。
5. 第一个会话是否已经正常结束。
6. 结果对象及其嵌套对象是否可以序列化。

### 4.5 最后再了解 `<cache>` 属性

确认最小的 `<cache/>` 能工作后，再根据项目需要设置属性：

```xml
<cache
    eviction="LRU"
    flushInterval="60000"
    size="512"
    readOnly="false"/>
```

| 属性 | 可接受的值 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `eviction` | `LRU`、`FIFO`、`SOFT`、`WEAK` | `LRU` | 缓存达到容量后如何移除对象 |
| `flushInterval` | 正整数，单位毫秒；也可省略 | 不定时刷新 | 按时间间隔清空缓存 |
| `size` | 正整数 | `1024` | 可保存的对象引用数量 |
| `readOnly` | `true`、`false` | `false` | 是否直接共享缓存对象引用 |

`readOnly=false` 时，默认缓存通过序列化提供对象副本；`readOnly=true` 时可能向调用者返回同一个缓存对象引用，速度更快，但调用者绝对不能修改该对象。

基础项目不要因为存在这些属性就立即调整参数。应先通过日志和测试确认缓存确有价值，再根据数据量、更新频率和一致性要求决定。

## 五、二级缓存注意点

二级缓存不是所有项目都适合开启。

需要注意：

- 数据更新后缓存可能失效。
- 多表关联查询缓存一致性更复杂。
- 分布式系统中本地缓存可能不一致。
- 默认读写缓存要求查询结果对象可以序列化。

### 5.1 namespace 与多表一致性

假设 `EmployeeMapper` 中有一条 JOIN 查询，同时读取 `employees` 和 `departments`：

```text
EmployeeMapper.selectEmployeeWithDepartment
```

如果通过 `DepartmentMapper` 修改部门名称，默认只会影响 `DepartmentMapper` 自己的 namespace 缓存，`EmployeeMapper` 中已经缓存的关联查询可能无法同时失效。

这就是“SQL 查询了多张表，但缓存按 Mapper namespace 管理”产生的不一致风险。可以通过 `<cache-ref>` 共享缓存区域，但它不能自动解决任意跨表、跨服务或分布式环境的一致性问题。

因此：

- 频繁更新或强一致性要求高的数据，不应默认启用二级缓存。
- 多表关联查询启用缓存前，必须列出哪些写操作会影响查询结果。
- 缓存设计需要和事务边界、数据更新路径一起 Review。

## 六、缓存和查询标签

`<select>` 常见属性：

```xml
<select id="selectById" resultType="Employee" useCache="true" flushCache="false">
```

| 属性 | 作用 |
| --- | --- |
| `useCache` | 是否使用二级缓存 |
| `flushCache` | 执行后是否清理缓存 |

`<select>` 的 `useCache` 默认是 `true`，表示存在二级缓存时允许使用；它不控制一级缓存。查询的 `flushCache` 默认是 `false`，写操作的 `flushCache` 默认是 `true`。

即使 `useCache="true"`，如果当前 namespace 没有声明 `<cache/>`，查询也不会因此自动获得二级缓存。

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 以为缓存一定提升性能 | 缓存也有维护成本 | 根据查询特点判断 |
| 写操作后读到旧数据 | 缓存一致性没处理好 | 理解缓存失效规则 |
| 二级缓存乱开 | 多表和频繁更新场景复杂 | 只在读多写少场景谨慎使用 |
| 设置 `cacheEnabled=true` 后仍不命中 | Mapper namespace 没有声明 `<cache/>` | 检查全局开关和 Mapper 配置 |
| 第二个会话仍然查询数据库 | 第一个会话尚未正常完成，或语句、参数不同 | 检查会话边界和 SQL 日志 |
| 缓存对象写入时报错 | 默认读写缓存中的对象不能序列化 | 检查结果对象及其嵌套对象 |
| 修改部门后员工关联结果仍是旧值 | 写操作与关联查询位于不同 namespace | 调查所有影响表和缓存失效范围 |
| 把 N+1 当作缓存问题处理 | 首次请求仍会执行大量 SQL | 优先修正查询调用方式 |

## 八、缓存验证步骤

验证缓存时保存以下证据：

| 验证项 | 操作 | 预期观察 |
| --- | --- | --- |
| 一级缓存命中 | 同一会话查询两次相同 ID | SQL 日志只有一次查询 |
| 一级缓存清理 | 查询、执行更新、再次查询 | 更新后再次出现查询 SQL |
| 不同会话 | 不启用二级缓存，分别查询相同 ID | 两个会话各执行一次 SQL |
| 二级缓存命中 | 启用 `<cache/>`，正常结束第一个会话后再查询 | 第二个会话不再执行相同 SQL |
| 参数不同 | 查询 ID 1，再查询 ID 2 | 两次查询都访问数据库 |
| namespace 一致性 | 缓存关联查询后通过另一 Mapper 修改关联表 | 检查是否出现旧数据 |

记录配置、操作顺序、SQL 日志数量和最终结果。只写“缓存成功”不能作为有效的自测证据。

## 九、本章练习

请完成：

1. 使用同一个 `SqlSession` 查询两次相同员工，从日志确认数据库访问次数。
2. 在两次查询之间调用 `clearCache()`，确认第二次查询重新访问数据库。
3. 把 `localCacheScope` 分别设置为 `SESSION` 和 `STATEMENT`，保存两次实验的 SQL 日志。
4. 使用两个不同 `SqlSession` 查询相同员工，先关闭二级缓存，再启用 `<cache/>`，比较结果。
5. 在查询和再次查询之间执行一次员工更新，确认缓存失效和查询结果变化。
6. 阅读一个跨表关联查询，列出所有可能影响其结果的写操作和 Mapper namespace。
7. 根据实验结果写出是否应在该员工管理项目启用二级缓存的 Review 结论，并附上证据。

## 十、本章总结

- 一级缓存是 `SqlSession` 级别。
- 二级缓存是 Mapper namespace 级别。
- 一级缓存默认是 `SESSION` 范围，也可以设置为 `STATEMENT`。
- `cacheEnabled` 是总开关，Mapper 中的 `<cache/>` 用于声明 namespace 缓存。
- 会话完成方式、写操作和 namespace 都会影响缓存可见性与失效。
- 默认读写二级缓存需要考虑结果对象序列化。
- 缓存不是通用优化方案。
- 缓存不能代替正确的 SQL 调用设计，也不能用于掩盖 N+1。
