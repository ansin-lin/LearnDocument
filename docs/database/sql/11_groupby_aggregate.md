# 第11章 分组与聚合函数

> 本章目标：掌握 `COUNT`、`SUM`、`AVG`、`MAX`、`MIN`、`GROUP BY`、`HAVING` 和条件聚合，能够完成常见统计类查询。

## 一、为什么需要分组统计

业务系统中经常需要统计数据。

例如：

- 每个部门有多少员工。
- 每个部门平均工资是多少。
- 每名员工订单总金额是多少。
- 每个月订单数量是多少。
- 工资大于 300000 的员工有多少。

这类 SQL 不是查询每一行明细，而是把多行数据汇总成统计结果。

## 二、示例数据

`employees`：

| id | name | department_id | salary | email |
| --- | --- | --- | --- | --- |
| 1 | Tanaka | 10 | 300000 | tanaka@example.com |
| 2 | Suzuki | 20 | 360000 | suzuki@example.com |
| 3 | Sato | 20 | 280000 | NULL |
| 4 | Yamada | 30 | NULL | yamada@example.com |

后续示例都基于这张表。

## 三、聚合函数是什么

聚合函数用于对多行数据进行统计，并返回统计结果。

| 函数 | 作用 | 示例 |
| --- | --- | --- |
| `COUNT()` | 统计数量 | 统计员工数 |
| `SUM()` | 求和 | 统计工资总和 |
| `AVG()` | 求平均值 | 统计平均工资 |
| `MAX()` | 最大值 | 查询最高工资 |
| `MIN()` | 最小值 | 查询最低工资 |

四种数据库都支持这些常用聚合函数。

## 四、COUNT 统计数量

### 4.1 COUNT(*)

`COUNT(*)` 用于统计行数。

```sql
SELECT COUNT(*) AS employee_count
FROM employees;
```

结果：

| employee_count |
| --- |
| 4 |

`COUNT(*)` 会统计所有行，即使某些列是 `NULL`。

### 4.2 COUNT(列名)

`COUNT(列名)` 只统计该列不为 `NULL` 的行。

```sql
SELECT COUNT(email) AS email_count
FROM employees;
```

结果：

| email_count |
| --- |
| 3 |

因为 `Sato` 的 `email` 是 `NULL`，所以没有被统计。

### 4.3 COUNT(DISTINCT 列名)

`COUNT(DISTINCT 列名)` 用于统计去重后的数量。

```sql
SELECT COUNT(DISTINCT department_id) AS department_count
FROM employees;
```

结果：

| department_count |
| --- |
| 3 |

## 五、SUM、AVG、MAX、MIN

统计工资：

```sql
SELECT SUM(salary) AS total_salary,
       AVG(salary) AS average_salary,
       MAX(salary) AS max_salary,
       MIN(salary) AS min_salary
FROM employees;
```

结果：

| total_salary | average_salary | max_salary | min_salary |
| --- | --- | --- | --- |
| 940000 | 313333.33 | 360000 | 280000 |

说明：

- `SUM(salary)` 统计工资总和。
- `AVG(salary)` 统计平均工资。
- `MAX(salary)` 查询最高工资。
- `MIN(salary)` 查询最低工资。
- `salary` 为 `NULL` 的行不会参与 `SUM` 和 `AVG`。

## 六、GROUP BY 是什么

`GROUP BY` 用于按照指定列分组统计。

语法：

```sql
SELECT 分组列,
       聚合函数(统计列)
FROM 表名
GROUP BY 分组列;
```

需求：按部门统计员工数量。

```sql
SELECT department_id,
       COUNT(*) AS employee_count
FROM employees
GROUP BY department_id;
```

结果：

| department_id | employee_count |
| --- | --- |
| 10 | 1 |
| 20 | 2 |
| 30 | 1 |

执行理解：

1. 先从 `employees` 取数据。
2. 按 `department_id` 分组。
3. 每组执行 `COUNT(*)`。
4. 输出每个部门的统计结果。

## 七、多字段分组

`GROUP BY` 可以按多个字段分组。

语法：

```sql
GROUP BY 列名1, 列名2
```

例如按部门和邮箱是否为空进行统计：

```sql
SELECT department_id,
       CASE
           WHEN email IS NULL THEN '未设置'
           ELSE '已设置'
       END AS email_status,
       COUNT(*) AS employee_count
FROM employees
GROUP BY department_id,
         CASE
             WHEN email IS NULL THEN '未设置'
             ELSE '已设置'
         END;
```

多字段分组表示：多个字段组合相同的数据才属于同一组。

## 八、SELECT 与 GROUP BY 的规则

使用 `GROUP BY` 时，`SELECT` 中通常只能出现：

- `GROUP BY` 中的分组列
- 聚合函数

正确：

```sql
SELECT department_id,
       COUNT(*) AS employee_count
FROM employees
GROUP BY department_id;
```

错误：

```sql
SELECT department_id,
       name,
       COUNT(*) AS employee_count
FROM employees
GROUP BY department_id;
```

错误原因：

- `department_id` 是分组列，可以写。
- `COUNT(*)` 是聚合函数，可以写。
- `name` 既不是分组列，也不是聚合函数，不能直接写。

## 九、WHERE 和 HAVING 的区别

`WHERE` 用于分组前筛选行。

`HAVING` 用于分组后筛选统计结果。

| 对比 | WHERE | HAVING |
| --- | --- | --- |
| 执行时机 | 分组前 | 分组后 |
| 能否使用聚合函数 | 通常不能 | 可以 |
| 常见用途 | 先过滤明细数据 | 过滤统计结果 |

需求：统计工资不为空的员工，并查询员工数大于 1 的部门。

```sql
SELECT department_id,
       COUNT(*) AS employee_count
FROM employees
WHERE salary IS NOT NULL
GROUP BY department_id
HAVING COUNT(*) > 1;
```

执行理解：

1. `WHERE salary IS NOT NULL` 先排除工资为空的员工。
2. `GROUP BY department_id` 按部门分组。
3. `COUNT(*)` 统计每组数量。
4. `HAVING COUNT(*) > 1` 只保留员工数大于 1 的部门。

## 十、条件聚合

条件聚合是指在统计时加入条件判断。

需求：统计每个部门的员工总数和高工资员工数。

```sql
SELECT department_id,
       COUNT(*) AS total_count,
       SUM(CASE WHEN salary >= 300000 THEN 1 ELSE 0 END) AS high_salary_count
FROM employees
GROUP BY department_id;
```

结果：

| department_id | total_count | high_salary_count |
| --- | --- | --- |
| 10 | 1 | 1 |
| 20 | 2 | 1 |
| 30 | 1 | 0 |

`CASE WHEN salary >= 300000 THEN 1 ELSE 0 END` 的含义：

- 满足条件时记为 1。
- 不满足条件时记为 0。
- 最后用 `SUM` 求和。

四种数据库都支持 `CASE WHEN`。

## 十一、NULL 与聚合函数

| 写法 | 说明 |
| --- | --- |
| `COUNT(*)` | 统计所有行 |
| `COUNT(列名)` | 只统计该列不为 `NULL` 的行 |
| `SUM(列名)` | 忽略 `NULL` |
| `AVG(列名)` | 忽略 `NULL` |
| `MAX(列名)` | 忽略 `NULL` |
| `MIN(列名)` | 忽略 `NULL` |

如果希望 `NULL` 按 0 参与计算，可以使用空值处理函数。

通用写法：

```sql
SELECT AVG(COALESCE(salary, 0)) AS average_salary
FROM employees;
```

`COALESCE(salary, 0)` 表示如果 `salary` 是 `NULL`，就当作 0。

## 十二、四种数据库差异

本章常用聚合函数、`GROUP BY`、`HAVING`、`CASE WHEN` 在四种数据库中基本都支持。

需要注意的差异：

| 内容 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 字符串空值处理 | `IFNULL` / `COALESCE` | `NVL` / `COALESCE` | `COALESCE` | `ISNULL` / `COALESCE` |
| 日期分组函数 | `DATE_FORMAT` | `TO_CHAR` | `TO_CHAR` | `FORMAT` |
| 分组规则严格度 | 新版本更严格，旧设置可能宽松 | 严格 | 严格 | 严格 |

学习阶段建议按严格规则写 SQL：`SELECT` 中非聚合字段必须出现在 `GROUP BY` 中。

## 十三、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| SELECT 中出现未分组字段 | 不符合分组规则 | 非聚合字段必须写进 `GROUP BY` |
| 用 WHERE 过滤聚合结果 | 聚合后条件不能写 WHERE | 使用 `HAVING` |
| 忽略 NULL | 统计结果和预期不同 | 区分 `COUNT(*)` 和 `COUNT(列)` |
| 认为 GROUP BY 不改变行数 | 分组会把多行合并为统计结果 | 观察结果行数 |
| HAVING 中条件写错 | 混淆明细条件和统计条件 | 明细用 WHERE，统计用 HAVING |

## 十四、本章练习

请完成：

1. 统计员工总数。
2. 统计邮箱不为空的员工数。
3. 按部门统计员工数量。
4. 按部门统计平均工资。
5. 查询平均工资大于 300000 的部门。
6. 按部门统计工资大于等于 300000 的员工数量。
7. 说明 `COUNT(*)` 和 `COUNT(email)` 的区别。

## 十五、本章总结

- 聚合函数用于统计多行数据。
- `GROUP BY` 用于分组。
- `HAVING` 用于筛选分组后的结果。
- `WHERE` 是分组前筛选，`HAVING` 是分组后筛选。
- 聚合函数处理 `NULL` 时要特别注意。
