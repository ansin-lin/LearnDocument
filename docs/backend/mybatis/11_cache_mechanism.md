# 第11章 缓存机制

> 本章目标：理解 MyBatis 一级缓存和二级缓存的作用、范围、失效条件和使用注意点。

## 一、为什么需要缓存

如果同一个 SQL 在短时间内重复查询，缓存可以减少数据库访问次数。

但是缓存也可能带来数据不一致问题。

因此学习 MyBatis 缓存时，需要同时理解：

- 缓存能提高什么。
- 缓存什么时候失效。
- 缓存为什么不能随便启用。

## 二、一级缓存

一级缓存是 `SqlSession` 级别缓存。

同一个 `SqlSession` 中，执行相同查询时，MyBatis 可能直接返回缓存结果。

示例：

```java
try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
    EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);

    Employee employee1 = mapper.selectById(1L);
    Employee employee2 = mapper.selectById(1L);

    System.out.println(employee1 == employee2);
}
```

第二次查询可能从一级缓存中获取。

## 三、一级缓存失效

一级缓存常见失效情况：

| 情况 | 说明 |
| --- | --- |
| `SqlSession` 关闭 | 缓存随会话结束 |
| 执行 insert/update/delete | MyBatis 会清理缓存 |
| 调用 `clearCache()` | 手动清理缓存 |
| 不同 `SqlSession` | 缓存不共享 |

## 四、二级缓存

二级缓存是 Mapper namespace 级别缓存。

它可以在多个 `SqlSession` 之间共享。

开启方式：

```xml
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>
```

Mapper XML 中添加：

```xml
<cache/>
```

## 五、二级缓存注意点

二级缓存不是所有项目都适合开启。

需要注意：

- 数据更新后缓存可能失效。
- 多表关联查询缓存一致性更复杂。
- 分布式系统中本地缓存可能不一致。
- 查询结果对象通常需要可序列化。

## 六、缓存和查询标签

`<select>` 常见属性：

```xml
<select id="selectById" resultType="Employee" useCache="true" flushCache="false">
```

| 属性 | 作用 |
| --- | --- |
| `useCache` | 是否使用二级缓存 |
| `flushCache` | 执行后是否清理缓存 |

写操作默认会清理缓存。

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 以为缓存一定提升性能 | 缓存也有维护成本 | 根据查询特点判断 |
| 写操作后读到旧数据 | 缓存一致性没处理好 | 理解缓存失效规则 |
| 二级缓存乱开 | 多表和频繁更新场景复杂 | 只在读多写少场景谨慎使用 |

## 八、本章练习

请完成：

1. 使用同一个 `SqlSession` 查询两次相同员工。
2. 使用两个不同 `SqlSession` 查询相同员工，观察区别。
3. 说明一级缓存和二级缓存的作用范围。
4. 说明为什么二级缓存不能随便开启。

## 九、本章总结

- 一级缓存是 `SqlSession` 级别。
- 二级缓存是 Mapper namespace 级别。
- 写操作会影响缓存。
- 缓存不是通用优化方案。
