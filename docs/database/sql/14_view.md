# 第14章 视图

> 本章目标：理解视图是什么、为什么企业项目会使用视图，掌握 MySQL、Oracle、PostgreSQL、SQL Server 中创建、查询、修改和删除视图的基本写法。

## 一、视图是什么

视图（View）是基于查询语句保存下来的“虚拟表”。

视图本身通常不直接保存数据，而是保存一段查询定义。查询视图时，数据库会根据视图中的 `SELECT` 语句从真实表中取数据。

例如有员工表和部门表：

```text
employees
departments
```

如果项目中经常需要查询“员工姓名 + 部门名称”，每次都写 JOIN 会比较重复：

```sql
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

可以把这段查询保存成一个视图：

```sql
CREATE VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

之后查询视图：

```sql
SELECT id,
       employee_name,
       department_name,
       email
FROM v_employee_department;
```

从使用方式上看，视图很像一张表；从本质上看，它是一个被命名的查询。

## 二、为什么需要视图

视图常用于以下场景：

| 场景 | 说明 |
| --- | --- |
| 简化复杂查询 | 把多表 JOIN、字段别名、条件封装起来 |
| 统一查询口径 | 报表和系统功能使用同一套查询定义 |
| 控制字段暴露 | 只给调用者看到允许访问的列 |
| 兼容旧系统 | 表结构调整后，通过视图保持旧查询接口 |
| 报表统计 | 把常用统计查询保存成固定入口 |

日本项目中，视图经常出现在报表、帳票出力、CSV 导出、权限受限查询、旧系统改修和数据库迁移场景中。详细设计书中可能会直接指定使用某个视图名，也可能要求开发人员根据既有视图调查数据来源。

## 三、准备示例表

本章继续使用员工和部门示例。

部门表：

| id | name |
| ---: | --- |
| 10 | Sales |
| 20 | Development |

员工表：

| id | name | department_id | email | salary |
| ---: | --- | ---: | --- | ---: |
| 1 | Tanaka | 10 | tanaka@example.com | 300000 |
| 2 | Suzuki | 20 | suzuki@example.com | 350000 |
| 3 | Sato | 20 | sato@example.com | 330000 |

如果需要重新准备数据，可以使用下面的最小结构。不同数据库的自增列语法前面章节已经讲过，这里用文字表示主键自增即可：

```sql
-- departments：id 为主键，name 为部门名称
-- employees：id 为主键，department_id 关联 departments.id
```

视图章节重点是 `CREATE VIEW`、查询视图和维护视图，不重复展开建表语法。

## 四、创建视图

### 4.1 基本语法

标准写法：

```sql
CREATE VIEW 视图名 AS
SELECT 列名
FROM 表名
WHERE 条件;
```

含义：

| 写法 | 作用 |
| --- | --- |
| `CREATE VIEW` | 创建视图 |
| `视图名` | 给查询结果起一个可复用的名称 |
| `AS` | 表示后面跟着视图对应的查询语句 |
| `SELECT ...` | 定义视图实际返回哪些数据 |

### 4.2 四种数据库通用示例

MySQL / Oracle / PostgreSQL / SQL Server 都支持下面这种基本写法：

```sql
CREATE VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email,
       e.salary
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

这段 SQL 创建了一个名为 `v_employee_department` 的视图。

视图返回的列包括：

| 列名 | 来源 |
| --- | --- |
| `id` | `employees.id` |
| `employee_name` | `employees.name` |
| `department_name` | `departments.name` |
| `email` | `employees.email` |
| `salary` | `employees.salary` |

视图名常见命名方式：

| 命名方式 | 示例 | 说明 |
| --- | --- | --- |
| `v_业务含义` | `v_employee_department` | 常见写法，表示 View |
| `vw_业务含义` | `vw_employee_department` | SQL Server 项目中也常见 |
| `业务名_view` | `employee_department_view` | 可读性较高 |

项目中优先遵守既有命名规则，不要同一个系统里混用多套风格。

## 五、查询视图

查询视图和查询表的写法基本一样：

```sql
SELECT id,
       employee_name,
       department_name,
       email,
       salary
FROM v_employee_department
ORDER BY id;
```

预期结果：

| id | employee_name | department_name | email | salary |
| ---: | --- | --- | --- | ---: |
| 1 | Tanaka | Sales | tanaka@example.com | 300000 |
| 2 | Suzuki | Development | suzuki@example.com | 350000 |
| 3 | Sato | Development | sato@example.com | 330000 |

也可以继续加条件：

```sql
SELECT employee_name,
       department_name,
       salary
FROM v_employee_department
WHERE department_name = 'Development'
ORDER BY salary DESC;
```

预期结果：

| employee_name | department_name | salary |
| --- | --- | ---: |
| Suzuki | Development | 350000 |
| Sato | Development | 330000 |

注意：视图可以简化 SQL，但不会自动保证性能。查询视图时，数据库仍然需要访问底层表。是否能高效执行，要看底层表索引、视图定义和最终查询条件。

## 六、修改视图

视图定义需要变更时，例如增加员工邮箱域名或隐藏工资字段，不同数据库写法有差异。

### 6.1 MySQL

MySQL 常用 `CREATE OR REPLACE VIEW`：

```sql
CREATE OR REPLACE VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

这里重新定义了视图，去掉了 `salary` 列。

### 6.2 Oracle

Oracle 常用 `CREATE OR REPLACE VIEW`：

```sql
CREATE OR REPLACE VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

Oracle 项目中修改视图后，相关对象可能出现无效状态，需要根据项目规范重新编译或检查依赖对象。

### 6.3 PostgreSQL

PostgreSQL 支持 `CREATE OR REPLACE VIEW`，但替换视图时不能随意删除已有列或改变已有列的数据类型。

可以追加列：

```sql
CREATE OR REPLACE VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email,
       e.salary
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

如果要删除列或大幅改变结构，常见处理方式是先删除再重建：

```sql
DROP VIEW v_employee_department;

CREATE VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

删除再重建可能影响依赖该视图的对象或权限，执行前必须确认影响范围。

### 6.4 SQL Server

SQL Server 常用 `CREATE OR ALTER VIEW`：

```sql
CREATE OR ALTER VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

旧版本或既有项目中也可能看到 `ALTER VIEW`：

```sql
ALTER VIEW v_employee_department AS
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name,
       e.email
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

## 七、删除视图

删除视图使用 `DROP VIEW`。

MySQL / Oracle / PostgreSQL / SQL Server 基本写法：

```sql
DROP VIEW v_employee_department;
```

MySQL / PostgreSQL / SQL Server 支持避免不存在时报错的写法：

```sql
DROP VIEW IF EXISTS v_employee_department;
```

Oracle 通常写：

```sql
DROP VIEW v_employee_department;
```

删除视图不会删除底层表数据，但会影响依赖这个视图的程序、报表、存储过程或权限配置。执行删除前，应先确认有没有对象正在使用该视图。

## 八、可更新视图

有些简单视图可以执行 `INSERT`、`UPDATE`、`DELETE`，这种视图叫可更新视图。

例如只基于一张表，并且没有聚合、分组、去重、集合运算的简单视图：

```sql
CREATE VIEW v_active_employees AS
SELECT id,
       name,
       department_id,
       email
FROM employees
WHERE status = 'ACTIVE';
```

可以尝试更新：

```sql
UPDATE v_active_employees
SET email = 'tanaka.new@example.com'
WHERE id = 1;
```

这个更新实际会影响底层 `employees` 表。

但下面这种视图通常不可直接更新：

```sql
CREATE VIEW v_department_salary AS
SELECT d.name AS department_name,
       COUNT(e.id) AS employee_count,
       SUM(e.salary) AS total_salary
FROM departments d
INNER JOIN employees e
    ON d.id = e.department_id
GROUP BY d.name;
```

原因是它包含分组和聚合结果，数据库无法把“修改 total_salary”明确转换成某一行真实表数据的修改。

判断视图是否可更新时，需要关注：

| 视图内容 | 是否通常可更新 |
| --- | --- |
| 单表简单列查询 | 可能可以 |
| 多表 JOIN | 通常受限制 |
| `GROUP BY` | 通常不可以 |
| 聚合函数 | 通常不可以 |
| `DISTINCT` | 通常不可以 |
| `UNION` | 通常不可以 |
| 计算列 | 通常不能直接更新计算结果 |

实际项目中，不建议把视图更新作为默认设计。更常见做法是：视图用于查询，写入操作直接面向真实表，并由应用或存储过程控制业务规则。

## 九、普通视图与物化视图

普通视图保存查询定义，查询时再访问底层表。

物化视图（Materialized View）会保存查询结果数据，适合复杂统计和报表场景，但需要刷新数据。

| 类型 | 是否保存结果数据 | 常见用途 |
| --- | --- | --- |
| 普通视图 | 通常不保存 | 简化查询、权限控制、统一字段 |
| 物化视图 | 保存 | 大型报表、复杂聚合、性能优化 |

四种数据库支持情况不同：

| 数据库 | 普通视图 | 物化视图 |
| --- | --- | --- |
| MySQL | 支持 | 不提供原生物化视图 |
| Oracle | 支持 | 支持 |
| PostgreSQL | 支持 | 支持 |
| SQL Server | 支持 | 使用索引视图实现类似能力 |

物化视图涉及刷新策略、存储空间、数据一致性和权限，不适合在 SQL 入门阶段作为主线。当前只需要知道它和普通视图的区别。

## 十、四种数据库语法差异总结

| 操作 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 创建视图 | `CREATE VIEW 视图名 AS SELECT...` | 同左 | 同左 | 同左 |
| 替换视图 | `CREATE OR REPLACE VIEW` | `CREATE OR REPLACE VIEW` | `CREATE OR REPLACE VIEW`，但结构变更有限制 | `CREATE OR ALTER VIEW` 或 `ALTER VIEW` |
| 删除视图 | `DROP VIEW 视图名` | `DROP VIEW 视图名` | `DROP VIEW 视图名` | `DROP VIEW 视图名` |
| 不存在时不报错 | `DROP VIEW IF EXISTS` | 常用动态 SQL 或先查询对象 | `DROP VIEW IF EXISTS` | `DROP VIEW IF EXISTS` |
| 物化视图 | 无原生语法 | `CREATE MATERIALIZED VIEW` | `CREATE MATERIALIZED VIEW` | 索引视图 |

不同数据库的视图权限、依赖对象处理、可更新视图限制并不完全相同。企业项目中要以当前数据库产品和项目规范为准。

## 十一、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 把视图当成真实表 | 没理解视图本质是查询定义 | 先查看视图定义和底层表 |
| 视图中使用 `SELECT *` | 底层表字段变化会影响视图结果 | 明确写出需要的列 |
| 修改视图前不查依赖 | 报表、程序或权限可能依赖视图 | 先做影响调查 |
| 删除视图以为会删除表数据 | 混淆视图和表 | `DROP VIEW` 只删除视图对象 |
| 在复杂视图上直接更新 | 聚合或 JOIN 结果无法明确映射到底层行 | 写操作优先操作真实表 |
| 视图很慢就认为视图本身有问题 | 底层查询、索引或条件设计不合理 | 查看视图 SQL 和执行计划 |

## 十二、基础练习

请完成：

1. 创建 `v_employee_department` 视图，包含员工编号、员工姓名、部门名称、邮箱。
2. 查询 `v_employee_department` 中所有员工。
3. 查询部门为 `Development` 的员工。
4. 使用 `CREATE OR REPLACE VIEW` 或当前数据库对应写法，为视图增加 `salary` 列。
5. 删除视图，并确认 `employees` 和 `departments` 表数据仍然存在。

## 十三、综合练习

请完成一个报表用视图：

1. 创建 `v_department_employee_summary`。
2. 统计每个部门的员工数量和平均工资。
3. 查询员工数量大于等于 2 的部门。
4. 写出该视图是否适合直接更新，并说明原因。
5. 写出修改或删除该视图前需要调查哪些影响范围。

参考方向：

```sql
CREATE VIEW v_department_employee_summary AS
SELECT d.id AS department_id,
       d.name AS department_name,
       COUNT(e.id) AS employee_count,
       AVG(e.salary) AS average_salary
FROM departments d
LEFT JOIN employees e
    ON d.id = e.department_id
GROUP BY d.id,
         d.name;
```

## 十四、本章总结

- 视图是被命名的查询，使用方式类似表。
- 视图适合封装复杂查询、统一报表口径和控制字段暴露。
- 普通视图通常不保存结果数据，物化视图会保存结果数据。
- 修改或删除视图前必须确认依赖对象和权限影响。
- 企业项目中，视图更常用于查询和报表，不建议默认通过复杂视图写入数据。
