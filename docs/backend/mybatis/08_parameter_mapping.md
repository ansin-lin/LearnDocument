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

## 四、多参数与 @Param

多参数建议使用 `@Param` 明确命名。

```java
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

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 多参数取不到值 | 没有使用 `@Param` | 给参数命名 |
| 对象属性名写错 | `#{}` 名称和 Java 属性不一致 | 检查 getter/setter |
| 滥用 `${}` | 可能 SQL 注入 | 优先使用 `#{}` |
| foreach 集合名错误 | `collection` 和 `@Param` 不一致 | 保持名称一致 |

## 九、本章练习

请完成：

1. 使用单参数查询员工。
2. 使用对象参数查询员工。
3. 使用 `@Param` 传递多个参数。
4. 使用 `<foreach>` 完成 ID 列表查询。
5. 说明 `#{}` 和 `${}` 的区别。

## 十、本章总结

- `#{}` 是参数绑定，安全。
- `${}` 是字符串替换，有 SQL 注入风险。
- 多参数推荐使用 `@Param`。
- 集合参数常配合 `<foreach>`。
