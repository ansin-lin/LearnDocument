# 第6章 查询与结果映射

> 本章目标：掌握 `<select>`、`resultType`、`resultMap`、字段别名和下划线转驼峰映射。

## 一、查询结果为什么需要映射

数据库查询结果是表格数据。

Java 程序中使用的是对象。

MyBatis 需要把查询结果中的列映射到 Java 对象的属性。

例如：

| 数据库列 | Java 属性 |
| --- | --- |
| `id` | `id` |
| `name` | `name` |
| `department_id` | `departmentId` |

## 二、resultType

`resultType` 用于简单结果映射。

Mapper 接口：

```java
Employee selectById(Long id);
```

Mapper XML：

```xml
<select id="selectById" parameterType="long" resultType="Employee">
    SELECT
        id,
        name,
        department_id AS departmentId,
        email
    FROM employees
    WHERE id = #{id}
</select>
```

`resultType="Employee"` 表示把查询结果转换成 `Employee` 对象。

## 三、查询多条数据

Mapper 接口：

```java
List<Employee> selectAll();
```

Mapper XML：

```xml
<select id="selectAll" resultType="Employee">
    SELECT
        id,
        name,
        department_id AS departmentId,
        email
    FROM employees
    ORDER BY id
</select>
```

返回多条数据时，XML 的 `resultType` 仍然写单条数据的类型。

## 四、字段别名

如果数据库列名和 Java 属性名不一致，可以使用 SQL 别名。

```sql
department_id AS departmentId
```

这样 MyBatis 可以把 `department_id` 映射到 `departmentId`。

## 五、下划线转驼峰

也可以在 `mybatis-config.xml` 中开启自动映射：

```xml
<settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
</settings>
```

开启后：

| 数据库列 | Java 属性 |
| --- | --- |
| `department_id` | `departmentId` |
| `create_time` | `createTime` |

## 六、resultMap

`resultMap` 用于明确指定列和属性的映射关系。

当数据库列名和 Java 属性名不一致，或者查询结果比较复杂时，推荐使用 `resultMap`。

```xml
<resultMap id="employeeResultMap" type="Employee">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <result property="departmentId" column="department_id"/>
    <result property="email" column="email"/>
</resultMap>
```

使用：

```xml
<select id="selectById" parameterType="long" resultMap="employeeResultMap">
    SELECT id, name, department_id, email
    FROM employees
    WHERE id = #{id}
</select>
```

### 6.1 resultMap 的常用属性

语法：

```xml
<resultMap id="映射规则名称" type="Java类型">
    ...
</resultMap>
```

常用属性：

| 属性 | 作用 | 示例 |
| --- | --- | --- |
| `id` | 当前 `resultMap` 的唯一名称 | `employeeResultMap` |
| `type` | 要映射成的 Java 类型 | `Employee` |
| `extends` | 继承已有 `resultMap` | `baseEmployeeMap` |
| `autoMapping` | 是否启用自动映射 | `true` / `false` |

最常用的是 `id` 和 `type`。

示例：

```xml
<resultMap id="employeeResultMap" type="Employee">
</resultMap>
```

这里：

- `id="employeeResultMap"` 表示这套映射规则的名字。
- `type="Employee"` 表示查询结果要映射成 `Employee` 对象。

### 6.2 resultMap 的内部标签

`resultMap` 内部常用标签：

| 标签 | 作用 |
| --- | --- |
| `<id>` | 映射主键列 |
| `<result>` | 映射普通列 |
| `<association>` | 映射一个关联对象 |
| `<collection>` | 映射一个关联集合 |
| `<constructor>` | 使用构造方法映射 |
| `<discriminator>` | 根据条件选择不同映射 |

本章重点掌握 `<id>` 和 `<result>`。

`<association>` 和 `<collection>` 只先了解含义，第 10 章会详细讲关联查询。

### 6.3 id 标签

`<id>` 用于映射主键列。

语法：

```xml
<id property="Java属性名" column="数据库列名"/>
```

示例：

```xml
<id property="id" column="id"/>
```

常用属性：

| 属性 | 作用 | 示例 |
| --- | --- | --- |
| `property` | Java 对象属性名 | `id` |
| `column` | 数据库查询结果列名 | `id` |
| `javaType` | Java 类型，一般可省略 | `Long` |
| `jdbcType` | JDBC 类型，一般可省略 | `BIGINT` |

`<id>` 不只是普通字段映射。

在关联查询和一对多映射中，MyBatis 会根据 `<id>` 判断对象是否是同一个对象。

### 6.4 result 标签

`<result>` 用于映射普通字段。

语法：

```xml
<result property="Java属性名" column="数据库列名"/>
```

示例：

```xml
<result property="departmentId" column="department_id"/>
```

常用属性：

| 属性 | 作用 | 示例 |
| --- | --- | --- |
| `property` | Java 对象属性名 | `departmentId` |
| `column` | 数据库查询结果列名 | `department_id` |
| `javaType` | Java 类型，一般可省略 | `Long` |
| `jdbcType` | JDBC 类型，一般可省略 | `BIGINT` |
| `typeHandler` | 指定类型转换处理器 | 自定义类型处理器 |

基础阶段最常用的是 `property` 和 `column`。

### 6.5 association 标签简介

`<association>` 用于映射一个对象属性。

例如员工对象中有一个部门对象：

```java
private Department department;
```

可以使用：

```xml
<association property="department" javaType="Department">
    ...
</association>
```

这里先知道：

- `property` 表示 Java 中的对象属性名。
- `javaType` 表示这个对象的 Java 类型。

详细写法放到第 10 章关联查询中讲。

### 6.6 collection 标签简介

`<collection>` 用于映射集合属性。

例如部门对象中有多个员工：

```java
private List<Employee> employees;
```

可以使用：

```xml
<collection property="employees" ofType="Employee">
    ...
</collection>
```

这里先知道：

- `property` 表示集合属性名。
- `ofType` 表示集合中每个元素的类型。

详细写法放到第 10 章关联查询中讲。

### 6.7 resultMap 使用建议

推荐使用 `resultMap` 的情况：

- 数据库列名和 Java 属性名不一致。
- 查询结果需要明确映射。
- 查询结果包含关联对象。
- 查询结果包含集合。
- SQL 中使用了多个表，列名容易重复。

简单查询可以使用 `resultType`。

复杂查询推荐使用 `resultMap`。

## 七、resultType 和 resultMap 的区别

| 对比 | resultType | resultMap |
| --- | --- | --- |
| 适合场景 | 简单映射 | 复杂映射 |
| 配置量 | 少 | 多 |
| 字段不一致 | 需要别名或驼峰设置 | 可以明确指定 |
| 关联关系 | 不适合 | 适合 |

同一个 `<select>` 中一般不要同时使用 `resultType` 和 `resultMap`。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 查询到了数据但对象属性为 null | 列名和属性名不一致 | 使用别名、驼峰设置或 resultMap |
| 多条查询返回单对象 | 方法返回值类型写错 | 使用 `List<Employee>` |
| resultMap 没生效 | `resultMap` 名称写错 | 检查 `id` 和引用名称 |

## 九、本章练习

请完成：

1. 使用 `resultType` 查询单个员工。
2. 使用 `resultType` 查询员工列表。
3. 使用 SQL 别名映射 `department_id`。
4. 使用 `resultMap` 完成同样查询。

## 十、本章总结

- `resultType` 适合简单映射。
- `resultMap` 适合复杂映射。
- 数据库列名和 Java 属性名必须能对应。
