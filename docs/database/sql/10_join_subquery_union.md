# 第10章 多表查询、子查询与集合操作

> 本章目标：掌握 JOIN、子查询、UNION / UNION ALL 的基本用法，能够跨表查询业务数据。

## 一、为什么需要多表查询

业务数据通常不会全部放在一张表中。

例如：

- 部门信息放在 `departments`。
- 员工信息放在 `employees`。
- 订单信息放在 `orders`。

查询员工和部门名称时，需要连接两张表。

## 二、示例数据

`departments`：

| id | name |
| --- | --- |
| 10 | Sales |
| 20 | Development |

`employees`：

| id | name | department_id |
| --- | --- | --- |
| 1 | Tanaka | 10 |
| 2 | Suzuki | 20 |

## 三、INNER JOIN

查询员工和部门名称：

```sql
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

结果：

| id | employee_name | department_name |
| --- | --- | --- |
| 1 | Tanaka | Sales |
| 2 | Suzuki | Development |

`INNER JOIN` 只返回两张表中能匹配上的数据。

## 四、LEFT JOIN

```sql
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name
FROM employees e
LEFT JOIN departments d
    ON e.department_id = d.id;
```

`LEFT JOIN` 会保留左表 `employees` 的所有数据。

如果右表没有匹配数据，右表字段返回 `NULL`。

## 五、RIGHT JOIN

```sql
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name
FROM employees e
RIGHT JOIN departments d
    ON e.department_id = d.id;
```

`RIGHT JOIN` 会保留右表 `departments` 的所有数据。

实际开发中，`LEFT JOIN` 更常见。

## 六、FULL JOIN

`FULL JOIN` 也叫 `FULL OUTER JOIN`。

它会同时保留左表和右表的数据：

- 左表和右表能匹配上的数据，会合并显示。
- 左表有、右表没有的数据，也会显示，右表字段为 `NULL`。
- 右表有、左表没有的数据，也会显示，左表字段为 `NULL`。

可以理解为：`LEFT JOIN` 的结果加上 `RIGHT JOIN` 的结果。

| 数据库 | 支持情况 |
| --- | --- |
| MySQL | 不直接支持，可用 `LEFT JOIN UNION RIGHT JOIN` 模拟 |
| Oracle | 支持 `FULL OUTER JOIN` |
| PostgreSQL | 支持 `FULL OUTER JOIN` |
| SQL Server | 支持 `FULL OUTER JOIN` |

Oracle / PostgreSQL / SQL Server：

```sql
SELECT e.name AS employee_name,
       d.name AS department_name
FROM employees e
FULL OUTER JOIN departments d
    ON e.department_id = d.id;
```

示例数据：

`departments`：

| id | name |
| --- | --- |
| 10 | Sales |
| 20 | Development |
| 30 | Human Resources |

`employees`：

| id | name | department_id |
| --- | --- | --- |
| 1 | Tanaka | 10 |
| 2 | Suzuki | 20 |
| 3 | Sato | 99 |

结果：

| employee_name | department_name |
| --- | --- |
| Tanaka | Sales |
| Suzuki | Development |
| Sato | NULL |
| NULL | Human Resources |

结果说明：

- `Tanaka` 和 `Suzuki` 能匹配到部门。
- `Sato` 的 `department_id = 99`，部门表中没有对应部门，所以部门名是 `NULL`。
- `Human Resources` 部门没有员工，但也会显示，员工名是 `NULL`。

MySQL 模拟 `FULL JOIN`：

```sql
SELECT e.name AS employee_name,
       d.name AS department_name
FROM employees e
LEFT JOIN departments d
    ON e.department_id = d.id
UNION
SELECT e.name AS employee_name,
       d.name AS department_name
FROM employees e
RIGHT JOIN departments d
    ON e.department_id = d.id;
```

实际项目中，是否需要 `FULL JOIN` 要看业务需求。多数业务查询更常使用 `INNER JOIN` 和 `LEFT JOIN`。

## 七、子查询

子查询是 SQL 内部再写一个查询。

需求：查询工资高于平均工资的员工。

```sql
SELECT id, name, salary
FROM employees
WHERE salary > (
    SELECT AVG(salary)
    FROM employees
);
```

## 八、EXISTS

`EXISTS` 用于判断子查询是否有结果。

它不关心子查询返回了什么值，只关心子查询有没有查到数据。

可以理解为：

```text
如果子查询能查到至少一行数据，EXISTS 就是 true。
如果子查询查不到任何数据，EXISTS 就是 false。
```

需求：查询至少有员工的部门。

```sql
SELECT d.id, d.name
FROM departments d
WHERE EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.department_id = d.id
);
```

示例数据：

`departments`：

| id | name |
| --- | --- |
| 10 | Sales |
| 20 | Development |
| 30 | Human Resources |

`employees`：

| id | name | department_id |
| --- | --- | --- |
| 1 | Tanaka | 10 |
| 2 | Suzuki | 20 |
| 3 | Sato | 20 |

执行结果：

| id | name |
| --- | --- |
| 10 | Sales |
| 20 | Development |

`Human Resources` 没有员工，所以不会被查询出来。

### 8.1 EXISTS 的执行理解

上面的 SQL 可以按下面方式理解：

```sql
SELECT d.id, d.name
FROM departments d
WHERE EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.department_id = d.id
);
```

执行理解：

1. 先取出 `departments` 中的一行。
2. 假设当前部门是 `Sales`，`d.id = 10`。
3. 执行子查询，检查 `employees` 中是否存在 `department_id = 10` 的员工。
4. 如果存在，`EXISTS` 为 true，这个部门保留。
5. 如果不存在，`EXISTS` 为 false，这个部门排除。
6. 对 `departments` 的每一行重复这个判断。

这里的子查询会引用外层查询的 `d.id`。

这种子查询叫关联子查询。

### 8.2 为什么 SELECT 1

在 `EXISTS` 中经常看到：

```sql
SELECT 1
```

原因是 `EXISTS` 不关心查询出来的具体列，只关心有没有结果。

下面写法也能表达类似含义：

```sql
SELECT e.id
FROM employees e
WHERE e.department_id = d.id
```

但是实际开发中常写 `SELECT 1`，表示“只判断是否存在数据”。

### 8.3 NOT EXISTS

`NOT EXISTS` 用于判断子查询没有结果。

需求：查询没有员工的部门。

```sql
SELECT d.id, d.name
FROM departments d
WHERE NOT EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.department_id = d.id
);
```

结果：

| id | name |
| --- | --- |
| 30 | Human Resources |

`Human Resources` 在员工表中没有对应员工，所以满足 `NOT EXISTS`。

### 8.4 EXISTS 和 IN 的区别

有些场景下，`EXISTS` 和 `IN` 可以实现类似效果。

使用 `IN`：

```sql
SELECT id, name
FROM departments
WHERE id IN (
    SELECT department_id
    FROM employees
);
```

使用 `EXISTS`：

```sql
SELECT d.id, d.name
FROM departments d
WHERE EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.department_id = d.id
);
```

区别：

| 对比 | IN | EXISTS |
| --- | --- | --- |
| 判断方式 | 判断某个值是否在子查询结果中 | 判断子查询是否存在结果 |
| 子查询和外层关系 | 可以不引用外层表 | 常用于引用外层表 |
| 常见场景 | 子查询结果比较小、逻辑简单 | 判断关联数据是否存在 |
| NULL 影响 | `NOT IN` 遇到 NULL 容易出问题 | `NOT EXISTS` 通常更安全 |

### 8.5 NOT IN 和 NOT EXISTS 的 NULL 问题

`NOT IN` 遇到 `NULL` 时容易出现不符合直觉的结果。

例如子查询结果中包含 `NULL`：

```sql
SELECT department_id
FROM employees;
```

结果：

| department_id |
| --- |
| 10 |
| 20 |
| NULL |

如果使用：

```sql
SELECT id, name
FROM departments
WHERE id NOT IN (
    SELECT department_id
    FROM employees
);
```

因为子查询中包含 `NULL`，结果可能不是预期的“没有员工的部门”。

更推荐使用 `NOT EXISTS`：

```sql
SELECT d.id, d.name
FROM departments d
WHERE NOT EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.department_id = d.id
);
```

### 8.6 EXISTS 常见使用场景

| 场景 | 示例 |
| --- | --- |
| 查询存在明细数据的主表 | 查询有员工的部门 |
| 查询不存在明细数据的主表 | 查询没有订单的客户 |
| 防止重复插入 | 插入前判断数据是否已经存在 |
| 替代复杂 IN 判断 | 判断关联表是否存在满足条件的数据 |

### 8.7 EXISTS 常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 认为 `SELECT 1` 是返回数字 1 | `EXISTS` 只判断是否有行 | 理解 `SELECT 1` 只是习惯写法 |
| 子查询没有关联外层表 | 每一行判断结果都一样 | 子查询中写清关联条件 |
| 混淆 `NOT IN` 和 `NOT EXISTS` | `NULL` 会影响 `NOT IN` | 判断不存在时优先考虑 `NOT EXISTS` |
| 把 EXISTS 当 JOIN | EXISTS 只判断存在，不返回右表字段 | 需要右表字段时使用 JOIN |

## 九、WITH AS 公共表表达式

`WITH AS` 用于把一个查询结果临时命名，然后在后面的主查询中使用。

这种写法也叫公共表表达式，英文是 Common Table Expression，简称 CTE。

可以把它理解为：

```text
先写一个临时查询结果
给这个结果起一个名字
后面的 SELECT 再使用这个名字
```

它不会真正创建数据库表，查询执行结束后这个临时结果就不存在了。

### 9.1 为什么需要 WITH AS

复杂查询中经常会出现很长的子查询。

如果直接把子查询写在 `FROM` 或 `WHERE` 中，SQL 会越来越难读。

`WITH AS` 可以把复杂 SQL 拆成几段，让每一段的业务含义更清楚。

常见用途：

| 场景 | 作用 |
| --- | --- |
| 复杂子查询 | 把子查询提前命名，主查询更容易阅读 |
| 多次使用同一份中间结果 | 避免重复写相同查询 |
| 分步骤统计 | 先求明细，再求汇总 |
| 配合 JOIN | 先过滤数据，再和其他表关联 |
| 配合窗口函数 | 先计算排名，再筛选排名结果 |

### 9.2 基本语法

标准写法：

```sql
WITH 临时结果名 AS (
    SELECT 列名
    FROM 表名
    WHERE 条件
)
SELECT 列名
FROM 临时结果名;
```

说明：

| 组成部分 | 作用 |
| --- | --- |
| `WITH` | 声明后面要定义公共表表达式 |
| `临时结果名` | 给中间查询结果起名字 |
| `AS` | 表示这个名字对应后面的查询结果 |
| 括号中的 `SELECT` | 生成临时结果 |
| 最后的 `SELECT` | 使用临时结果进行最终查询 |

### 9.3 单个 CTE 示例

需求：先找出工资大于等于 300000 的员工，再查询这些员工的姓名和工资。

```sql
WITH high_salary_employees AS (
    SELECT id,
           name,
           salary
    FROM employees
    WHERE salary >= 300000
)
SELECT id,
       name,
       salary
FROM high_salary_employees;
```

示例数据：

| id | name | salary |
| --- | --- | --- |
| 1 | Tanaka | 280000 |
| 2 | Suzuki | 350000 |
| 3 | Sato | 420000 |

执行结果：

| id | name | salary |
| --- | --- | --- |
| 2 | Suzuki | 350000 |
| 3 | Sato | 420000 |

执行理解：

1. 先执行 `WITH` 中的查询，得到工资大于等于 300000 的员工。
2. 把这个结果临时命名为 `high_salary_employees`。
3. 最后的 `SELECT` 从 `high_salary_employees` 中查询数据。

### 9.4 WITH AS 和普通子查询的对比

普通子查询写法：

```sql
SELECT id,
       name,
       salary
FROM (
    SELECT id,
           name,
           salary
    FROM employees
    WHERE salary >= 300000
) high_salary_employees;
```

`WITH AS` 写法：

```sql
WITH high_salary_employees AS (
    SELECT id,
           name,
           salary
    FROM employees
    WHERE salary >= 300000
)
SELECT id,
       name,
       salary
FROM high_salary_employees;
```

两种写法都能完成查询。

`WITH AS` 的优势是：先定义中间结果，再写主查询，复杂 SQL 更容易阅读和维护。

### 9.5 多个 CTE 示例

需求：先查询高工资员工，再查询有员工的部门，最后查询高工资员工所属部门。

```sql
WITH high_salary_employees AS (
    SELECT id,
           name,
           department_id,
           salary
    FROM employees
    WHERE salary >= 300000
),
active_departments AS (
    SELECT id,
           name
    FROM departments
    WHERE EXISTS (
        SELECT 1
        FROM employees e
        WHERE e.department_id = departments.id
    )
)
SELECT e.name AS employee_name,
       e.salary,
       d.name AS department_name
FROM high_salary_employees e
INNER JOIN active_departments d
    ON e.department_id = d.id;
```

多个 CTE 之间使用逗号分隔。

最后一个 CTE 后面不写逗号，直接写主查询。

### 9.6 CTE 与聚合查询

需求：先统计每个部门的员工人数，再查询员工人数大于等于 2 的部门。

```sql
WITH department_employee_count AS (
    SELECT department_id,
           COUNT(*) AS employee_count
    FROM employees
    GROUP BY department_id
)
SELECT department_id,
       employee_count
FROM department_employee_count
WHERE employee_count >= 2;
```

示例结果：

| department_id | employee_count |
| --- | --- |
| 20 | 2 |

这个写法的好处是：先把“每个部门的人数”这个中间结果算出来，再对统计结果进行筛选。

### 9.7 四种数据库的语法差异

普通非递归 `WITH AS` 在四种数据库中的基本写法相同。

| 数据库 | 支持情况 | 注意点 |
| --- | --- | --- |
| MySQL | MySQL 8.0 及以上支持 | MySQL 5.7 不支持 CTE |
| Oracle | 支持 | 常用于复杂报表 SQL |
| PostgreSQL | 支持 | 对 CTE 支持完整 |
| SQL Server | 支持 | 如果前面还有 SQL，通常需要在 `WITH` 前加分号 |

SQL Server 中常见写法：

```sql
;WITH high_salary_employees AS (
    SELECT id,
           name,
           salary
    FROM employees
    WHERE salary >= 300000
)
SELECT id,
       name,
       salary
FROM high_salary_employees;
```

这里的分号用于结束前一条 SQL，避免 SQL Server 把 `WITH` 理解成上一条语句的一部分。

### 9.8 使用注意点

- CTE 只是当前 SQL 中的临时结果，不会创建真实表。
- CTE 名称要表达业务含义，例如 `high_salary_employees`。
- CTE 适合拆分复杂查询，不适合把简单 SQL 故意写复杂。
- 多个 CTE 使用逗号分隔。
- 主查询必须紧跟在 CTE 后面，中间不能单独插入其他 SQL。
- 递归 CTE 属于进阶内容，基础阶段先掌握普通 CTE。

## 十、集合操作是什么

集合操作用于把多个 `SELECT` 的结果进行合并、取交集或取差集。

常见集合操作：

| 操作 | 含义 | 常用关键字 |
| --- | --- | --- |
| 并集 | 合并两个查询结果 | `UNION`、`UNION ALL` |
| 交集 | 取两个查询结果都存在的数据 | `INTERSECT` |
| 差集 | 取第一个查询有、第二个查询没有的数据 | `EXCEPT`、`MINUS` |

集合操作处理的是“查询结果集”，不是直接处理表本身。

## 十一、集合操作的基本规则

使用集合操作时，多个 `SELECT` 必须满足基本规则：

1. 每个 `SELECT` 的列数量必须相同。
2. 对应位置的列类型要兼容。
3. 最终列名通常以第一个 `SELECT` 的列名为准。
4. 如果需要排序，`ORDER BY` 通常写在整个集合操作最后。

正确示例：

```sql
SELECT name
FROM employees
WHERE department_id = 10
UNION
SELECT name
FROM employees
WHERE department_id = 20;
```

错误示例：

```sql
SELECT id, name
FROM employees
UNION
SELECT name
FROM employees;
```

上面 SQL 两个查询的列数量不同，不能直接做集合操作。

## 十二、并集：UNION 与 UNION ALL

并集用于合并两个查询结果。

示例数据：

`tokyo_employees`：

| name |
| --- |
| Tanaka |
| Suzuki |
| Sato |

`osaka_employees`：

| name |
| --- |
| Suzuki |
| Yamada |

### 12.1 UNION

`UNION` 会合并结果并去重。

```sql
SELECT name
FROM tokyo_employees
UNION
SELECT name
FROM osaka_employees;
```

结果：

| name |
| --- |
| Tanaka |
| Suzuki |
| Sato |
| Yamada |

`Suzuki` 在两个查询结果中都出现，但最终只保留一条。

### 12.2 UNION ALL

`UNION ALL` 会合并结果，但不去重。

```sql
SELECT name
FROM tokyo_employees
UNION ALL
SELECT name
FROM osaka_employees;
```

结果：

| name |
| --- |
| Tanaka |
| Suzuki |
| Sato |
| Suzuki |
| Yamada |

`Suzuki` 会保留两条。

### 12.3 UNION 和 UNION ALL 的区别

| 写法 | 是否去重 | 特点 |
| --- | --- | --- |
| `UNION` | 去重 | 结果更干净，但需要去重处理 |
| `UNION ALL` | 不去重 | 性能通常更好，保留所有数据 |

如果业务允许重复，优先考虑 `UNION ALL`。

四种数据库都支持 `UNION` 和 `UNION ALL`。

## 十三、交集：INTERSECT

交集用于查询两个结果中共同存在的数据。

需求：查询东京员工列表和大阪员工列表中都出现过的姓名。

Oracle / PostgreSQL / SQL Server：

```sql
SELECT name
FROM tokyo_employees
INTERSECT
SELECT name
FROM osaka_employees;
```

结果：

| name |
| --- |
| Suzuki |

`Suzuki` 同时存在于两个查询结果中。

MySQL 不支持 `INTERSECT`，可以使用 `INNER JOIN` 或 `EXISTS` 模拟。

MySQL 模拟写法：

```sql
SELECT t.name
FROM tokyo_employees t
WHERE EXISTS (
    SELECT 1
    FROM osaka_employees o
    WHERE o.name = t.name
);
```

## 十四、差集：EXCEPT / MINUS

差集用于查询第一个结果中存在、第二个结果中不存在的数据。

需求：查询只在东京员工列表中出现、没有出现在大阪员工列表中的姓名。

PostgreSQL / SQL Server：

```sql
SELECT name
FROM tokyo_employees
EXCEPT
SELECT name
FROM osaka_employees;
```

Oracle：

```sql
SELECT name
FROM tokyo_employees
MINUS
SELECT name
FROM osaka_employees;
```

结果：

| name |
| --- |
| Tanaka |
| Sato |

MySQL 不支持 `EXCEPT` 或 `MINUS`，可以使用 `NOT EXISTS` 模拟。

MySQL 模拟写法：

```sql
SELECT t.name
FROM tokyo_employees t
WHERE NOT EXISTS (
    SELECT 1
    FROM osaka_employees o
    WHERE o.name = t.name
);
```

## 十五、四种数据库集合操作对比

| 操作 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| `UNION` | 支持 | 支持 | 支持 | 支持 |
| `UNION ALL` | 支持 | 支持 | 支持 | 支持 |
| `INTERSECT` | 不支持 | 支持 | 支持 | 支持 |
| 差集 | 不支持，可用 `NOT EXISTS` | `MINUS` | `EXCEPT` | `EXCEPT` |

## 十六、集合操作和 JOIN 的区别

| 对比 | 集合操作 | JOIN |
| --- | --- | --- |
| 处理对象 | 多个查询结果 | 多张表的横向关联 |
| 结果列 | 多个查询列数量必须一致 | 可以选择多张表的不同列 |
| 常见用途 | 合并名单、比较名单、取差异 | 查询员工和部门、订单和客户 |

简单理解：

- 集合操作是“纵向合并或比较结果”。
- JOIN 是“横向关联表数据”。

## 十七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| JOIN 忘记 `ON` | 产生大量错误组合数据 | 明确连接条件 |
| 不理解 LEFT JOIN 的 NULL | 右表没有匹配数据 | 判断右表字段是否为 NULL |
| CTE 后面没有主查询 | `WITH AS` 只是定义临时结果 | 在 CTE 后紧跟 `SELECT` |
| 多个 CTE 分隔错误 | CTE 之间缺少逗号或最后多写逗号 | 多个 CTE 中间用逗号，最后一个不写逗号 |
| MySQL 5.7 使用 WITH AS | MySQL 5.7 不支持 CTE | 使用子查询，或升级到 MySQL 8.0 及以上 |
| 集合操作字段数量不同 | 两个查询列数不一致 | 保持列数一致 |
| 对应列类型不兼容 | 第一个查询和第二个查询列类型冲突 | 调整 SELECT 字段或使用类型转换 |
| MySQL 中直接写 INTERSECT | MySQL 不支持 | 使用 EXISTS 模拟 |
| Oracle 中写 EXCEPT | Oracle 使用 `MINUS` | 改为 `MINUS` |
| 把 UNION ALL 当 UNION | `UNION ALL` 不去重 | 根据业务选择是否去重 |

## 十八、本章练习

请完成：

1. 查询员工姓名和部门名称。
2. 查询所有部门以及部门下员工。
3. 查询工资高于平均工资的员工。
4. 使用 `UNION ALL` 合并两个部门的员工姓名。
5. 使用 `UNION` 合并两个员工名单并去重。
6. 使用 `INTERSECT` 查询两个名单中都存在的员工。
7. 使用 `EXCEPT` 或 `MINUS` 查询只存在于第一个名单中的员工。
8. 写出 MySQL 中模拟交集和差集的写法。
9. 使用 `WITH AS` 先统计每个部门员工人数，再查询员工人数大于等于 2 的部门。

## 十九、本章总结

- JOIN 用于多表查询。
- 子查询用于把一个查询结果作为另一个查询条件。
- `WITH AS` 用于定义当前 SQL 内部可使用的临时结果，让复杂查询更清晰。
- `UNION` 合并并去重，`UNION ALL` 不去重。
- `INTERSECT` 用于取交集。
- `EXCEPT` 和 `MINUS` 用于取差集。
- MySQL 不支持 `INTERSECT`、`EXCEPT`、`MINUS`，可以使用 `EXISTS`、`NOT EXISTS` 模拟。
