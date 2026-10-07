# 第8章 参数映射

> 本章目标：掌握单参数、多参数、对象参数、集合参数、`@Param`、`#{}` 和 `${}` 的用法与区别。

## 一、参数映射是什么

参数映射是指把 Java 方法参数传递给 SQL。

例如：

```java
Employee selectById(Long id);
```

对应：

```xml
WHERE id = #{id}
```

调用 Mapper 方法时，MyBatis 会把方法参数整理成一个“参数对象”，然后读取其中的值并交给 SQL。可以先把执行过程理解为：

```text
调用 Mapper 方法
    ↓
MyBatis 整理方法参数
    ↓
读取 #{} 中指定的参数值
    ↓
把 SQL 中的 #{} 变成 JDBC 占位符 ?
    ↓
通过 PreparedStatement 设置参数并执行 SQL
```

参数只有一个、参数有多个或参数本身是 Java 对象时，MyBatis 读取参数值的方式会有所不同。本章依次说明这些情况。

## 二、单参数

Mapper 接口：

```java
Employee selectById(Long id);
```

Mapper XML：

```xml
<select id="selectById" parameterType="long" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    WHERE id = #{id}
</select>
```

这里的方法只有一个简单类型参数。调用 `selectById(1L)` 时，`#{id}` 取得的值就是 `1L`。

单个简单参数虽然可以直接绑定，但仍建议让 Java 参数名、`#{}` 中的名称和业务含义保持一致，方便 Review 时对应接口与 SQL。

## 三、对象参数

Mapper 接口：

```java
List<Employee> selectByCondition(Employee condition);
```

Mapper XML：

```xml
<select id="selectByCondition" parameterType="Employee" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    WHERE department_id = #{departmentId}
</select>
```

`#{departmentId}` 会读取 `Employee` 对象中的 `departmentId` 属性。

MyBatis 会按照 JavaBean 属性规则查找这个值，通常相当于调用 `condition.getDepartmentId()`。因此，XML 中写的是 Java 属性名 `departmentId`，不是数据库列名 `department_id`。

对象参数适合字段较少、含义一致的场景。如果查询条件逐渐增多，实际项目中通常会创建专门的查询条件类，例如 `EmployeeSearchCondition`，避免把数据库实体同时当作查询条件对象使用。

## 四、多参数与 @Param

多参数建议使用 `@Param` 明确命名。

```java
import org.apache.ibatis.annotations.Param;

List<Employee> selectByDepartmentAndName(
        @Param("departmentId") Long departmentId,
        @Param("name") String name
);
```

Mapper XML：

```xml
<select id="selectByDepartmentAndName" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    WHERE department_id = #{departmentId}
      AND name = #{name}
</select>
```

`@Param("departmentId")` 把第一个方法参数命名为 `departmentId`，因此 XML 可以通过 `#{departmentId}` 读取它。

不使用 `@Param` 时，多个参数可以通过 `param1`、`param2` 等默认名称访问，但这种写法不能直接表达参数含义，也不利于修改和 Review，因此本课程统一使用 `@Param`。

## 五、集合参数

集合参数常用于 `IN` 查询。

```java
List<Employee> selectByIds(@Param("ids") List<Long> ids);
```

Mapper XML：

```xml
<select id="selectByIds" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

这里首次使用 `<foreach>`，它会依次读取 `ids` 中的元素并生成逗号分隔的占位符。调用 `selectByIds(List.of(1L, 3L))` 时，最终 SQL 可以理解为：

```sql
SELECT id, name, department_id AS departmentId, email
FROM employees
WHERE id IN (?, ?)
```

对应参数是 `1L` 和 `3L`。第9章会继续讲解 `<foreach>` 的各个属性和动态 SQL 生成过程。

空集合不能生成有效的 `IN` 条件。调用 Mapper 前应先处理：

```java
if (ids == null || ids.isEmpty()) {
    return List.of();
}

return employeeMapper.selectByIds(ids);
```

这里选择“没有 ID 就返回空列表”，而不是执行无条件查询。实际项目应根据接口规格决定返回空列表还是提示参数错误。

## 六、#{} 和 ${} 的区别

| 写法 | 作用 | 是否安全 | 常见用途 |
| --- | --- | --- | --- |
| `#{}` | 参数绑定 | 安全 | 条件值、插入值、修改值 |
| `${}` | 字符串替换 | 有 SQL 注入风险 | 表名、列名、排序字段等有限场景 |

推荐优先使用 `#{}`。

示例：

```xml
WHERE name = #{name}
```

不推荐：

```xml
WHERE name = '${name}'
```

如果用户输入是：

```text
' OR '1' = '1
```

`${}` 可能拼接出危险 SQL。

### 6.1 `#{}` 的执行过程

下面的条件：

```xml
WHERE id = #{id}
```

发送给 JDBC 时可以理解为：

```sql
WHERE id = ?
```

MyBatis 再通过 TypeHandler 把 Java 的 `Long` 值设置到 JDBC 参数中。TypeHandler 负责在 Java 类型和 JDBC 类型之间转换；基础阶段不需要自己编写 TypeHandler，但需要知道参数值不是直接拼接进 SQL 字符串。

启用 SQL 日志后，通常可以分别观察到预编译 SQL 和参数：

```text
Preparing: SELECT id, name, department_id AS departmentId, email FROM employees WHERE id = ?
Parameters: 1(Long)
```

### 6.2 `null` 和 `jdbcType`

MyBatis 通常可以根据 Java 属性推断参数类型。某些数据库驱动在参数值为 `null` 时无法正确判断 JDBC 类型，可以显式指定：

```xml
email = #{email,jdbcType=VARCHAR}
```

`jdbcType=VARCHAR` 表示以 JDBC 的字符串类型设置参数。不要为了统一格式给所有参数都机械添加 `jdbcType`；只有驱动、存储过程或明确的空值绑定要求需要时再使用。

## 七、${} 的有限使用场景

例如动态排序字段：

```java
List<Employee> selectOrderBy(@Param("orderBy") String orderBy);
```

```xml
<select id="selectOrderBy" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    ORDER BY ${orderBy}
</select>
```

这种写法必须在 Java 代码中限制允许值。

例如只允许：

```text
id
name
department_id
```

不能直接使用用户输入。

例如，把外部输入转换成固定白名单值后再传给 Mapper：

```java
String safeOrderBy = switch (requestedOrderBy == null ? "" : requestedOrderBy) {
    case "name" -> "name";
    case "department" -> "department_id";
    default -> "id";
};

List<Employee> employees = employeeMapper.selectOrderBy(safeOrderBy);
```

这里传入 `${orderBy}` 的字符串只能来自程序中写死的三个列名。请求参数不能绕过 `switch` 直接传给 Mapper。

如果可选项很少，也可以在 Mapper XML 中使用第9章介绍的 `<choose>` 直接生成固定的 `ORDER BY` 子句，从而完全避免 `${}`。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 多参数取不到值 | 没有使用 `@Param` | 给参数命名 |
| 对象属性名写错 | `#{}` 名称和 Java 属性不一致 | 检查 getter/setter |
| 滥用 `${}` | 可能 SQL 注入 | 优先使用 `#{}` |
| foreach 集合名错误 | `collection` 和 `@Param` 不一致 | 保持名称一致 |
| `IN` 查询生成语法错误 | 集合为 `null` 或空集合 | 调用 Mapper 前按接口规格返回空列表或报告参数错误 |
| `null` 参数绑定失败 | 数据库驱动无法判断 JDBC 类型 | 必要时在 `#{}` 中指定 `jdbcType` |
| 排序条件被注入 | 把请求参数直接传给 `${}` | 使用固定白名单或 `<choose>` |

## 九、本章练习

请完成：

1. 使用单参数查询员工，并从日志中确认 SQL 使用了 `?` 占位符。
2. 使用对象参数按部门查询员工，并故意把 `#{departmentId}` 写错一次，记录错误现象和修正结果。
3. 使用 `@Param` 传递部门 ID 和姓名，分别验证两个参数都能正确绑定。
4. 使用 `<foreach>` 完成 ID 列表查询，同时验证包含两个 ID 和空集合两种输入。
5. 为排序字段实现白名单，只允许按 `id`、`name`、`department_id` 排序。
6. 对照 SQL 日志，说明 `#{}` 和 `${}` 在最终 SQL 中的区别。

## 十、本章总结

- `#{}` 是参数绑定，安全。
- `${}` 是字符串替换，有 SQL 注入风险。
- 多参数推荐使用 `@Param`。
- 集合参数常配合 `<foreach>`。
- `#{}` 通常会生成 JDBC 占位符，并通过 TypeHandler 设置参数。
- 集合为空和参数为 `null` 时，需要按照接口规格处理边界。
- `${}` 只能接收程序控制的白名单内容，不能直接接收外部输入。
