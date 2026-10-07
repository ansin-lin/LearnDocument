# 第10章 关联查询与 ResultMap

> 本章目标：理解数据库关系与 Java 对象关系的区别，掌握单对象、集合对象的关联结果映射，能够根据 SQL 结果检查 `ResultMap`。

## 一、为什么需要关联查询

实际业务中，数据通常分散在多张表中。

例如：

- `departments` 保存部门信息。
- `employees` 保存员工信息。

查询员工时，可能需要一起查询部门名称。

数据库执行 JOIN 后返回的是“行”，Java 程序需要的是“对象”。`ResultMap` 的作用就是告诉 MyBatis：

- 哪些列属于员工对象。
- 哪些列属于部门对象。
- 多行结果中哪些数据代表同一个对象。
- 子对象应放入单个属性还是集合属性。

本章主线统一使用“一条 JOIN SQL + 嵌套结果映射”。嵌套查询属于扩展写法，只在后文说明它和 N+1 的关系。

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

下面是用于说明关联属性的实体类片段，getter、setter 和构造方法沿用前面章节的写法。

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

## 四、单对象关联：association

一个员工属于一个部门。

从 Java 对象看，`Employee` 中只有一个 `Department department` 属性，因此使用 `<association>` 映射单个复杂对象。

从数据库关系看，多个员工可以属于同一个部门，所以本例是“员工到部门的多对一关系”，不是严格意义上的一对一关系。`<association>` 表示“映射一个对象属性”，不能直接等同于数据库的一对一关系。

Mapper 接口：

```java
Employee selectEmployeeWithDepartment(Long id);
```

Mapper XML：

```xml
<resultMap id="employeeWithDepartmentMap" type="Employee">
    <id property="id" column="employee_id"/>
    <result property="name" column="employee_name"/>
    <result property="departmentId" column="employee_department_id"/>
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
        e.department_id AS employee_department_id,
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

示例使用不同的 SQL 别名区分两个来源：

| SQL 列别名 | 来源 | Java 属性 |
| --- | --- | --- |
| `employee_id` | `employees.id` | `Employee.id` |
| `employee_department_id` | `employees.department_id` | `Employee.departmentId` |
| `department_id` | `departments.id` | `Employee.department.id` |
| `department_name` | `departments.name` | `Employee.department.name` |

即使 `employees.department_id` 和 `departments.id` 在正常数据中值相同，也应使用不同别名表示各自来源。这样字段变化或排查映射问题时不会产生歧义。

查询成功后，对象结构可以理解为：

```text
Employee
├─ id: 1
├─ name: Tanaka
├─ departmentId: 10
├─ email: tanaka@example.com
└─ department
   ├─ id: 10
   └─ name: Sales
```

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
    <collection property="employees" ofType="Employee" notNullColumn="employee_id">
        <id property="id" column="employee_id"/>
        <result property="name" column="employee_name"/>
        <result property="departmentId" column="employee_department_id"/>
        <result property="email" column="email"/>
    </collection>
</resultMap>

<select id="selectDepartmentWithEmployees" parameterType="long" resultMap="departmentWithEmployeesMap">
    SELECT
        d.id AS department_id,
        d.name AS department_name,
        e.id AS employee_id,
        e.name AS employee_name,
        e.department_id AS employee_department_id,
        e.email
    FROM departments d
    LEFT JOIN employees e
        ON d.id = e.department_id
    WHERE d.id = #{id}
</select>
```

`<collection>` 用于映射集合属性。

这里把多个员工映射到 `department.employees`。

### 5.1 一条 SQL 为什么能得到一个集合

假设 Development 部门有两名员工，数据库返回的是两行：

| department_id | department_name | employee_id | employee_name |
| --- | --- | --- | --- |
| 20 | Development | 2 | Suzuki |
| 20 | Development | 3 | Sato |

MyBatis 根据父对象的 `<id property="id" column="department_id"/>` 判断两行属于同一个部门，再根据集合元素的 `<id property="id" column="employee_id"/>` 区分两名员工，最终组装成：

```text
Department(id=20, name=Development)
└─ employees
   ├─ Employee(id=2, name=Suzuki)
   └─ Employee(id=3, name=Sato)
```

因此，关联结果映射中的 `<id>` 不只是普通字段映射，它还帮助 MyBatis 识别和合并对象。主对象或集合元素的主键映射错误时，可能出现对象重复、数据覆盖或集合内容异常。

### 5.2 LEFT JOIN 和空集合

这里使用 `LEFT JOIN`，因此即使部门没有员工，部门行也可以返回。此时员工相关列均为 `NULL`。

`notNullColumn="employee_id"` 表示只有 `employee_id` 不为 `NULL` 时才创建并加入员工对象，可以清楚表达“没有员工时得到空集合”的意图。

## 六、association 和 collection 对比

| 标签 | 映射目标 | 本章示例 | Java 属性 |
| --- | --- | --- | --- |
| `<association>` | 单个复杂对象 | 员工所属部门 | `Department department` |
| `<collection>` | 多个对象组成的集合 | 部门下的员工 | `List<Employee> employees` |

选择标签时先看 Java 属性是单个对象还是集合，再结合数据库关系和查询规格设计 SQL。

## 七、嵌套结果映射与嵌套查询

本章前面的写法属于嵌套结果映射：先用 JOIN 一次取得数据，再通过 `<association>` 或 `<collection>` 组装对象。

MyBatis 也支持在关联标签中通过 `select` 调用另一条查询。例如下面只是写法示意：

```xml
<association
        property="department"
        column="employee_department_id"
        select="com.example.mybatis.mapper.DepartmentMapper.selectById"/>
```

这种写法会先查询员工，再根据 `department_id` 查询部门，称为嵌套查询。它有时便于复用简单查询，但在查询员工列表时可能为每一行额外执行一次部门查询，形成 N+1 问题。

主线示例继续使用 JOIN。嵌套查询的调用次数、N+1 判断和改进方案请参阅[附录A01 项目常见SQL调用陷阱与Review](appendix/A01_project_sql_call_pitfalls.md)。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 部门对象为空 | `<association>` 映射列名不一致 | 检查 column 和 SQL 别名 |
| 员工列表重复 | 主表 `<id>` 没配置正确 | 在 resultMap 中配置主键 |
| 一对多映射失败 | `ofType` 类型错误 | 确认集合元素类型 |
| `departmentId` 来源不清楚 | JOIN 的两个列使用相同别名 | 为员工外键和部门主键设置不同别名 |
| 空部门出现一个空员工对象 | 没有明确子对象创建条件 | 为集合设置 `notNullColumn="employee_id"` |
| 查询列表时 SQL 数量突然增加 | 使用了嵌套查询 | 检查 SQL 日志，并评估 JOIN 或批量查询 |

## 九、验证关联映射

完成关联查询后，至少验证以下数据：

1. 一个有一名员工的部门。
2. 一个有多名员工的部门。
3. 一个没有员工的部门。
4. 一个能够正常关联部门的员工。

检查内容包括：

- SQL 日志中执行了几条 SQL。
- JOIN 结果实际返回几行。
- Java 最终得到几个主对象。
- 集合中有几个子对象。
- 没有关联数据时是 `null`、空对象还是空集合。

不能只看到“查询没有报错”就认为关联映射正确。

## 十、本章练习

请完成：

1. 查询员工和所属部门，输出员工外键和部门对象 ID，确认两个字段映射来源正确。
2. 查询 Development 部门及员工列表，对照数据库结果行和 Java 集合元素数量。
3. 新增一个没有员工的部门，确认查询结果中的员工集合为空，不包含空员工对象。
4. 临时把集合元素的 `<id>` 从 `employee_id` 错写为 `department_id`，观察多名员工被错误合并的现象，修正后说明 `<id>` 对对象识别的作用。
5. 根据 SQL 日志确认 JOIN 方案只执行一条查询，并与附录中的 N+1 示例进行对照。
6. 说明为什么本章员工到部门的关系是多对一，但 Java 属性仍使用 `<association>`。

## 十一、本章总结

- `<association>` 用于单个复杂对象属性，不等同于数据库的一对一关系。
- `<collection>` 用于一对多集合映射。
- 关联查询需要为不同来源的列设置清楚的别名。
- 嵌套结果映射通过 `<id>` 识别并合并主对象和集合元素。
- `LEFT JOIN` 没有子数据时，应明确空集合和子对象的创建条件。
- 嵌套查询可能产生 N+1，必须通过 SQL 日志确认调用次数。
