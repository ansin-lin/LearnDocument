# 第10章 关联查询与 ResultMap

> 本章目标：掌握一对一、一对多关联查询的基本写法，理解 `<association>` 和 `<collection>` 的作用。

## 一、为什么需要关联查询

实际业务中，数据通常分散在多张表中。

例如：

- `departments` 保存部门信息。
- `employees` 保存员工信息。

查询员工时，可能需要一起查询部门名称。

## 二、示例表

`departments`：

| id | name |
| --- | --- |
| 10 | Sales |
| 20 | Development |

`employees`：

| id | name | department_id | email |
| --- | --- | --- | --- |
| 1 | Tanaka | 10 | tanaka@example.com |
| 2 | Suzuki | 20 | suzuki@example.com |

## 三、实体类

`Department`：

```java
public class Department {
    private Long id;
    private String name;
}
```

`Employee`：

```java
public class Employee {
    private Long id;
    private String name;
    private Long departmentId;
    private String email;
    private Department department;
}
```

## 四、一对一：association

一个员工属于一个部门。

Mapper 接口：

```java
Employee selectEmployeeWithDepartment(Long id);
```

Mapper XML：

```xml
<resultMap id="employeeWithDepartmentMap" type="Employee">
    <id property="id" column="employee_id"/>
    <result property="name" column="employee_name"/>
    <result property="departmentId" column="department_id"/>
    <result property="email" column="email"/>
    <association property="department" javaType="Department">
        <id property="id" column="department_id"/>
        <result property="name" column="department_name"/>
    </association>
</resultMap>

<select id="selectEmployeeWithDepartment" parameterType="long" resultMap="employeeWithDepartmentMap">
    SELECT
        e.id AS employee_id,
        e.name AS employee_name,
        e.department_id,
        e.email,
        d.id AS department_id,
        d.name AS department_name
    FROM employees e
    INNER JOIN departments d
        ON e.department_id = d.id
    WHERE e.id = #{id}
</select>
```

`<association>` 用于映射一个对象属性。

这里把部门信息映射到 `employee.department`。

## 五、一对多：collection

一个部门有多个员工。

`Department` 中增加员工列表：

```java
public class Department {
    private Long id;
    private String name;
    private List<Employee> employees;
}
```

Mapper 接口：

```java
Department selectDepartmentWithEmployees(Long id);
```

Mapper XML：

```xml
<resultMap id="departmentWithEmployeesMap" type="Department">
    <id property="id" column="department_id"/>
    <result property="name" column="department_name"/>
    <collection property="employees" ofType="Employee">
        <id property="id" column="employee_id"/>
        <result property="name" column="employee_name"/>
        <result property="departmentId" column="department_id"/>
        <result property="email" column="email"/>
    </collection>
</resultMap>

<select id="selectDepartmentWithEmployees" parameterType="long" resultMap="departmentWithEmployeesMap">
    SELECT
        d.id AS department_id,
        d.name AS department_name,
        e.id AS employee_id,
        e.name AS employee_name,
        e.email
    FROM departments d
    LEFT JOIN employees e
        ON d.id = e.department_id
    WHERE d.id = #{id}
</select>
```

`<collection>` 用于映射集合属性。

这里把多个员工映射到 `department.employees`。

## 六、association 和 collection 对比

| 标签 | 用途 | Java 属性 |
| --- | --- | --- |
| `<association>` | 一对一 | 单个对象 |
| `<collection>` | 一对多 | 集合对象 |

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 部门对象为空 | `<association>` 映射列名不一致 | 检查 column 和 SQL 别名 |
| 员工列表重复 | 主表 `<id>` 没配置正确 | 在 resultMap 中配置主键 |
| 一对多映射失败 | `ofType` 类型错误 | 确认集合元素类型 |

## 八、本章练习

请完成：

1. 查询员工和所属部门。
2. 查询部门和部门下员工列表。
3. 说明 `<association>` 和 `<collection>` 的区别。

## 九、本章总结

- `<association>` 用于一对一对象映射。
- `<collection>` 用于一对多集合映射。
- 关联查询需要注意 SQL 别名和 `resultMap` 映射。
