# 第7章 INSERT、UPDATE、DELETE

> 本章目标：掌握 MyBatis 中新增、修改、删除数据的 XML 写法，理解提交、影响行数和主键回填。

## 一、写操作和查询的区别

查询使用 `<select>`。

新增、修改、删除分别使用：

| 操作 | 标签 |
| --- | --- |
| 新增 | `<insert>` |
| 修改 | `<update>` |
| 删除 | `<delete>` |

写操作会改变数据库数据，需要注意提交。

## 二、写操作标签的属性总览

`<insert>`、`<update>`、`<delete>` 都是 Mapper XML 中的 SQL 标签。

它们通过标签属性控制：

- 对应哪个 Mapper 方法
- 参数类型是什么
- 是否刷新缓存
- SQL 超时时间
- 是否回填主键

### 2.1 insert 标签属性

`<insert>` 用于新增数据。

常见属性：

| 属性 | 作用 |
| --- | --- |
| `id` | SQL 语句 ID，必须和 Mapper 接口方法名对应 |
| `parameterType` | 参数类型，可以是基本类型、Map 或 Java 对象 |
| `flushCache` | 执行后是否清空缓存，写操作默认通常为 `true` |
| `timeout` | SQL 执行超时时间，单位是秒 |
| `statementType` | SQL 执行方式，常见默认值是 `PREPARED` |
| `useGeneratedKeys` | 是否使用 JDBC 自动生成主键 |
| `keyProperty` | Java 对象中接收主键值的属性名 |
| `keyColumn` | 数据库表中的主键列名 |
| `databaseId` | 多数据库环境下指定数据库厂商 |

### 2.2 update 标签属性

`<update>` 用于修改数据。

常见属性：

| 属性 | 作用 |
| --- | --- |
| `id` | SQL 语句 ID，必须和 Mapper 接口方法名对应 |
| `parameterType` | 参数类型 |
| `flushCache` | 执行后是否清空缓存，写操作默认通常为 `true` |
| `timeout` | SQL 执行超时时间，单位是秒 |
| `statementType` | SQL 执行方式，常见默认值是 `PREPARED` |
| `databaseId` | 多数据库环境下指定数据库厂商 |

### 2.3 delete 标签属性

`<delete>` 用于删除数据。

常见属性：

| 属性 | 作用 |
| --- | --- |
| `id` | SQL 语句 ID，必须和 Mapper 接口方法名对应 |
| `parameterType` | 参数类型 |
| `flushCache` | 执行后是否清空缓存，写操作默认通常为 `true` |
| `timeout` | SQL 执行超时时间，单位是秒 |
| `statementType` | SQL 执行方式，常见默认值是 `PREPARED` |
| `databaseId` | 多数据库环境下指定数据库厂商 |

## 三、常用属性重点说明

基础阶段重点掌握下面几个属性。

| 属性 | 必须程度 | 说明 |
| --- | --- | --- |
| `id` | 必须掌握 | 对应 Mapper 接口方法名 |
| `parameterType` | 建议掌握 | 指定传入参数类型，很多场景可省略但要能看懂 |
| `useGeneratedKeys` | 插入自增主键时掌握 | 用于 MySQL 自增主键回填 |
| `keyProperty` | 插入自增主键时掌握 | 指定 Java 对象接收主键的属性 |
| `keyColumn` | 插入自增主键时掌握 | 指定数据库主键列 |
| `timeout` | 能看懂 | 控制 SQL 超时时间 |
| `flushCache` | 学缓存时掌握 | 写操作后通常清理缓存 |

### 3.1 id

`id` 必须和 Mapper 接口方法名一致。

Mapper 接口：

```java
int insert(Employee employee);
```

Mapper XML：

```xml
<insert id="insert" parameterType="Employee">
```

如果 `id` 写错，调用 Mapper 方法时会找不到 SQL。

### 3.2 parameterType

`parameterType` 表示传入 SQL 的参数类型。

对象参数：

```xml
<insert id="insert" parameterType="Employee">
```

简单参数：

```xml
<delete id="deleteById" parameterType="long">
```

### 3.3 timeout

`timeout` 表示 SQL 最长执行时间，单位是秒。

```xml
<update id="update" parameterType="Employee" timeout="30">
```

如果 SQL 执行超过指定时间，可能会报超时错误。

### 3.4 flushCache

写操作会改变数据库数据，所以执行后通常需要清理缓存。

```xml
<update id="update" parameterType="Employee" flushCache="true">
```

基础阶段先知道：`insert`、`update`、`delete` 默认通常会刷新缓存。

## 四、INSERT

Mapper 接口：

```java
int insert(Employee employee);
```

Mapper XML：

```xml
<insert id="insert" parameterType="Employee">
    INSERT INTO employees (name, department_id, email)
    VALUES (#{name}, #{departmentId}, #{email})
</insert>
```

返回值 `int` 表示影响行数。

新增一条成功通常返回 `1`。

## 五、主键回填

MySQL 自增主键可以使用 `useGeneratedKeys` 回填到 Java 对象。

```xml
<insert id="insert" parameterType="Employee"
        useGeneratedKeys="true"
        keyProperty="id"
        keyColumn="id">
    INSERT INTO employees (name, department_id, email)
    VALUES (#{name}, #{departmentId}, #{email})
</insert>
```

说明：

| 属性 | 作用 |
| --- | --- |
| `useGeneratedKeys` | 开启自动生成主键 |
| `keyProperty` | Java 对象接收主键的属性 |
| `keyColumn` | 数据库主键列 |

## 六、UPDATE

Mapper 接口：

```java
int update(Employee employee);
```

Mapper XML：

```xml
<update id="update" parameterType="Employee">
    UPDATE employees
    SET name = #{name},
        department_id = #{departmentId},
        email = #{email}
    WHERE id = #{id}
</update>
```

`WHERE id = #{id}` 用于限制修改范围。

## 七、DELETE

Mapper 接口：

```java
int deleteById(Long id);
```

Mapper XML：

```xml
<delete id="deleteById" parameterType="long">
    DELETE FROM employees
    WHERE id = #{id}
</delete>
```

删除数据时必须注意 `WHERE` 条件。

## 八、提交

普通 MyBatis 中，`openSession()` 默认不是自动提交。

写操作后需要：

```java
sqlSession.commit();
```

示例：

```java
try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
    EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
    Employee employee = new Employee();
    employee.setName("Yamada");
    employee.setDepartmentId(20L);
    employee.setEmail("yamada@example.com");

    int count = mapper.insert(employee);
    sqlSession.commit();

    System.out.println(count);
    System.out.println(employee.getId());
}
```

## 九、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 新增后数据库没有数据 | 没有 `commit()` | 写操作后提交 |
| 修改了多行数据 | `WHERE` 条件缺失 | 使用主键条件 |
| 主键没有回填 | 没配置 `useGeneratedKeys` | 设置 `keyProperty` 和 `keyColumn` |
| 返回值误解 | `int` 是影响行数 | 判断是否为 1 |
| XML 的 `id` 写错 | Mapper 方法找不到 SQL | 保持 `id` 和方法名一致 |
| 主键属性名写错 | `keyProperty` 和 Java 属性不一致 | 检查实体类属性名 |

## 十、本章练习

请完成：

1. 新增一名员工。
2. 修改员工邮箱。
3. 删除指定员工。
4. 打印新增后的自增主键。
5. 说明 `<insert>`、`<update>`、`<delete>` 的常用属性。

## 十一、本章总结

- `<insert>`、`<update>`、`<delete>` 用于写操作。
- `id` 对应 Mapper 接口方法名。
- `parameterType` 表示参数类型。
- 写操作返回值通常是影响行数。
- 普通 MyBatis 写操作后需要提交。
- MySQL 自增主键可以使用 `useGeneratedKeys` 回填。
