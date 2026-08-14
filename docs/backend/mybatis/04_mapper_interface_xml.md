# 第4章 Mapper 接口与 XML 映射

> 本章目标：掌握 Mapper 接口方法和 Mapper XML SQL 的对应关系，理解 `namespace`、`id`、`parameterType`、`resultType` 的作用。

## 一、Mapper 是什么

Mapper 是 MyBatis 中负责数据库访问的组件。

一个 Mapper 通常由两部分组成：

| 文件 | 作用 |
| --- | --- |
| Mapper 接口 | Java 代码调用的方法 |
| Mapper XML | 实际执行的 SQL |

例如：

```text
EmployeeMapper.java
EmployeeMapper.xml
```

## 二、接口和 XML 的对应关系

Mapper 接口：

```java
package com.example.mybatis.mapper;

import com.example.mybatis.entity.Employee;

public interface EmployeeMapper {
    Employee selectById(Long id);
}
```

Mapper XML：

```xml
<mapper namespace="com.example.mybatis.mapper.EmployeeMapper">
    <select id="selectById" parameterType="long" resultType="Employee">
        SELECT
            id,
            name,
            department_id AS departmentId,
            email
        FROM employees
        WHERE id = #{id}
    </select>
</mapper>
```

对应规则：

| 接口 / XML | 必须对应 |
| --- | --- |
| 接口全限定名 | `<mapper namespace="">` |
| 方法名 | SQL 标签的 `id` |
| 方法参数 | `#{}` 中使用的参数 |
| 方法返回值 | `resultType` 或 `resultMap` |

## 三、namespace

`namespace` 用于指定 Mapper XML 属于哪个接口。

```xml
<mapper namespace="com.example.mybatis.mapper.EmployeeMapper">
```

如果 `namespace` 写错，MyBatis 找不到对应 SQL。

## 四、id

SQL 标签中的 `id` 对应接口方法名。

```xml
<select id="selectById">
```

对应：

```java
Employee selectById(Long id);
```

## 五、parameterType

`parameterType` 表示传入参数类型。

```xml
<select id="selectById" parameterType="long" resultType="Employee">
```

简单参数时，`parameterType` 可以省略，但新人阶段建议先理解它的作用。

## 六、resultType

`resultType` 表示查询结果要转换成什么 Java 类型。

```xml
<select id="selectAll" resultType="Employee">
```

如果返回多行数据，接口返回值可以是：

```java
List<Employee> selectAll();
```

XML 的 `resultType` 仍然写单个元素类型 `Employee`。

## 七、常见 SQL 标签

| 标签 | 作用 |
| --- | --- |
| `<select>` | 查询 |
| `<insert>` | 新增 |
| `<update>` | 修改 |
| `<delete>` | 删除 |

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| BindingException | `namespace` 或 `id` 不一致 | 检查接口全名和方法名 |
| 返回对象字段为空 | 列名和属性名不一致 | 使用别名或 `resultMap` |
| XML 未加载 | 没在配置文件注册 | 检查 `<mappers>` |

## 九、本章练习

请完成：

1. 创建 `EmployeeMapper` 接口。
2. 创建 `EmployeeMapper.xml`。
3. 编写 `selectById(Long id)`。
4. 故意改错 `id`，观察错误信息。

## 十、本章总结

- Mapper 接口定义 Java 方法。
- Mapper XML 编写 SQL。
- `namespace` 对应接口全限定名。
- SQL 标签 `id` 对应接口方法名。
