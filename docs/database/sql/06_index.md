# 第6章 索引基础

> 本章目标：理解索引的作用，掌握普通索引、唯一索引的创建和删除，知道索引不是越多越好。

## 一、索引是什么

索引用于提高查询速度。

可以把索引理解为书的目录。没有目录时，需要从头到尾查找；有目录时，可以更快定位。

常见使用场景：

- 经常作为查询条件的字段
- 经常作为 JOIN 条件的字段
- 经常排序的字段
- 需要保证唯一的字段

## 二、创建普通索引

需求：经常根据部门查询员工。

MySQL / PostgreSQL / SQL Server / Oracle：

```sql
CREATE INDEX 索引名
ON 表名 (列名);
```

示例：

```sql
CREATE INDEX idx_employees_department_id
ON employees (department_id);
```

## 三、创建唯一索引

需求：邮箱不能重复。

```sql
CREATE UNIQUE INDEX 唯一索引名
ON 表名 (列名);
```

唯一索引既能提高查询速度，也能限制重复值。

示例：

```sql
CREATE UNIQUE INDEX uk_employees_email
ON employees (email);
```

## 四、创建联合索引

联合索引是指一个索引包含多个列。

语法：

```sql
CREATE INDEX 索引名
ON 表名 (列名1, 列名2);
```

需求：经常根据部门和入职日期查询员工。

MySQL / Oracle / PostgreSQL / SQL Server：

```sql
CREATE INDEX idx_employees_department_hire_date
ON employees (department_id, hire_date);
```

这个索引包含两个列：

| 顺序 | 列名 | 作用 |
| --- | --- | --- |
| 第 1 列 | `department_id` | 联合索引中的第一个列 |
| 第 2 列 | `hire_date` | 联合索引中的第二个列 |

### 4.1 创建联合唯一索引

联合索引也可以是唯一索引。

需求：同一个部门内员工姓名不能重复。

```sql
CREATE UNIQUE INDEX uk_employees_department_name
ON employees (department_id, name);
```

这表示：

| department_id | name | 是否允许 |
| --- | --- | --- |
| 10 | Tanaka | 允许 |
| 20 | Tanaka | 允许 |
| 10 | Tanaka | 不允许 |

因为 `department_id` 和 `name` 的组合不能重复。

## 五、删除索引

| 数据库 | 写法 |
| --- | --- |
| MySQL | `DROP INDEX 索引名 ON 表名;` |
| Oracle | `DROP INDEX 索引名;` |
| PostgreSQL | `DROP INDEX 索引名;` |
| SQL Server | `DROP INDEX 索引名 ON 表名;` |

## 六、索引的代价

索引不是越多越好。

原因：

- 索引需要占用存储空间。
- 新增、修改、删除数据时，索引也要更新。
- 无用索引会增加维护成本。

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 所有字段都建索引 | 写入性能下降 | 只给高频查询字段建索引 |
| 只看字段名建索引 | 不知道实际查询条件 | 根据 SQL 使用情况判断 |
| 认为索引一定会被使用 | 优化器会选择执行计划 | 结合执行计划确认 |
| 已有联合索引还重复建单列索引 | 可能增加维护成本 | 根据实际查询和执行计划判断 |

## 八、本章练习

请完成：

1. 给 `employees.department_id` 创建普通索引。
2. 给 `employees.email` 创建唯一索引。
3. 给 `employees.department_id` 和 `employees.hire_date` 创建联合索引。
4. 给 `employees.department_id` 和 `employees.name` 创建联合唯一索引。
5. 写出四种数据库删除索引的语法。
6. 说明索引为什么不是越多越好。

## 九、补充：联合索引的具体使用

下面内容需要结合 `SELECT`、`WHERE` 和执行计划理解。

如果还没有学习查询语句，可以先知道结论：联合索引的列顺序会影响查询是否容易使用索引。

### 9.1 联合索引的列顺序

联合索引中，列顺序很重要。

例如：

```sql
CREATE INDEX idx_employees_department_hire_date
ON employees (department_id, hire_date);
```

这个索引比较适合下面的查询：

```sql
SELECT id, name, department_id, hire_date
FROM employees
WHERE department_id = 20;
```

也适合：

```sql
SELECT id, name, department_id, hire_date
FROM employees
WHERE department_id = 20
  AND hire_date >= DATE '2024-01-01';
```

但如果查询条件只有 `hire_date`：

```sql
SELECT id, name, department_id, hire_date
FROM employees
WHERE hire_date >= DATE '2024-01-01';
```

这个联合索引不一定能被有效使用。

因为索引的第一列是 `department_id`。

这通常叫最左前缀原则，也可以理解为前导列原则。

### 9.2 联合索引使用场景

适合创建联合索引的情况：

- 多个字段经常一起出现在 `WHERE` 条件中。
- 多个字段经常一起用于 `JOIN` 条件。
- 查询经常先按一个字段过滤，再按另一个字段排序。
- 多个字段组合起来需要保证唯一。

不适合创建联合索引的情况：

- 两个字段很少一起查询。
- 字段顺序和实际查询条件不一致。
- 已经有太多重复或无效索引。

### 9.3 学完 SELECT 后需要回头确认的点

学习完基础查询后，需要重新确认：

1. `WHERE department_id = 20` 是否能使用联合索引。
2. `WHERE department_id = 20 AND hire_date >= DATE '2024-01-01'` 是否能使用联合索引。
3. `WHERE hire_date >= DATE '2024-01-01'` 为什么不一定适合这个联合索引。

## 十、本章总结

- 索引用于提高查询速度。
- 普通索引用于查询优化。
- 唯一索引还能限制重复。
- 联合索引用于多个列共同参与查询或唯一判断。
- 联合索引的具体使用需要结合 `SELECT` 和 `WHERE` 理解。
- 索引会增加写入和维护成本。
