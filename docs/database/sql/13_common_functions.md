# 第13章 SQL 常用函数

> 本章目标：掌握字符串、日期、数值、转换和空值处理函数，理解四种数据库常用函数差异。

## 一、字符串函数

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 长度 | `LENGTH(name)` | `LENGTH(name)` | `LENGTH(name)` | `LEN(name)` |
| 截取 | `SUBSTRING(name,1,3)` | `SUBSTR(name,1,3)` | `SUBSTRING(name,1,3)` | `SUBSTRING(name,1,3)` |
| 拼接 | `CONCAT(a,b)` | `a || b` | `a || b` | `CONCAT(a,b)` |
| 转大写 | `UPPER(name)` | `UPPER(name)` | `UPPER(name)` | `UPPER(name)` |
| 去空格 | `TRIM(name)` | `TRIM(name)` | `TRIM(name)` | `TRIM(name)` |

示例：

```sql
SELECT UPPER(name) AS upper_name
FROM employees;
```

## 二、日期函数

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 当前日期时间 | `NOW()` | `SYSDATE` | `CURRENT_TIMESTAMP` | `GETDATE()` |
| 当前日期 | `CURRENT_DATE` | `TRUNC(SYSDATE)` | `CURRENT_DATE` | `CAST(GETDATE() AS DATE)` |
| 日期加 1 天 | `DATE_ADD(hire_date, INTERVAL 1 DAY)` | `hire_date + 1` | `hire_date + INTERVAL '1 day'` | `DATEADD(day, 1, hire_date)` |
| 日期格式化 | `DATE_FORMAT(hire_date, '%Y-%m-%d')` | `TO_CHAR(hire_date, 'YYYY-MM-DD')` | `TO_CHAR(hire_date, 'YYYY-MM-DD')` | `FORMAT(hire_date, 'yyyy-MM-dd')` |

日期函数在项目中经常用于：

- 查询最近 7 天数据
- 计算入职天数
- 查询本月数据
- 生成月报、日报
- 判断订单是否超过期限

### 2.1 日期加减

日期加减用于在某个日期基础上增加或减少天数、月份、年份。

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 加 7 天 | `DATE_ADD(hire_date, INTERVAL 7 DAY)` | `hire_date + 7` | `hire_date + INTERVAL '7 days'` | `DATEADD(day, 7, hire_date)` |
| 减 7 天 | `DATE_SUB(hire_date, INTERVAL 7 DAY)` | `hire_date - 7` | `hire_date - INTERVAL '7 days'` | `DATEADD(day, -7, hire_date)` |
| 加 1 个月 | `DATE_ADD(hire_date, INTERVAL 1 MONTH)` | `ADD_MONTHS(hire_date, 1)` | `hire_date + INTERVAL '1 month'` | `DATEADD(month, 1, hire_date)` |
| 减 1 个月 | `DATE_SUB(hire_date, INTERVAL 1 MONTH)` | `ADD_MONTHS(hire_date, -1)` | `hire_date - INTERVAL '1 month'` | `DATEADD(month, -1, hire_date)` |
| 加 1 年 | `DATE_ADD(hire_date, INTERVAL 1 YEAR)` | `ADD_MONTHS(hire_date, 12)` | `hire_date + INTERVAL '1 year'` | `DATEADD(year, 1, hire_date)` |

示例：查询入职日期 7 天后的日期。

MySQL：

```sql
SELECT name,
       hire_date,
       DATE_ADD(hire_date, INTERVAL 7 DAY) AS after_7_days
FROM employees;
```

Oracle：

```sql
SELECT name,
       hire_date,
       hire_date + 7 AS after_7_days
FROM employees;
```

PostgreSQL：

```sql
SELECT name,
       hire_date,
       hire_date + INTERVAL '7 days' AS after_7_days
FROM employees;
```

SQL Server：

```sql
SELECT name,
       hire_date,
       DATEADD(day, 7, hire_date) AS after_7_days
FROM employees;
```

### 2.2 日期差

日期差用于计算两个日期之间相差多少天、多少月或多少年。

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 相差天数 | `DATEDIFF(CURRENT_DATE, hire_date)` | `TRUNC(SYSDATE) - hire_date` | `CURRENT_DATE - hire_date` | `DATEDIFF(day, hire_date, GETDATE())` |
| 相差月数 | `TIMESTAMPDIFF(MONTH, hire_date, CURRENT_DATE)` | `MONTHS_BETWEEN(SYSDATE, hire_date)` | 需要结合 `AGE()` 或年月计算 | `DATEDIFF(month, hire_date, GETDATE())` |
| 相差年数 | `TIMESTAMPDIFF(YEAR, hire_date, CURRENT_DATE)` | `TRUNC(MONTHS_BETWEEN(SYSDATE, hire_date) / 12)` | `EXTRACT(YEAR FROM AGE(CURRENT_DATE, hire_date))` | `DATEDIFF(year, hire_date, GETDATE())` |

示例：计算员工入职天数。

MySQL：

```sql
SELECT name,
       hire_date,
       DATEDIFF(CURRENT_DATE, hire_date) AS working_days
FROM employees;
```

Oracle：

```sql
SELECT name,
       hire_date,
       TRUNC(SYSDATE) - hire_date AS working_days
FROM employees;
```

PostgreSQL：

```sql
SELECT name,
       hire_date,
       CURRENT_DATE - hire_date AS working_days
FROM employees;
```

SQL Server：

```sql
SELECT name,
       hire_date,
       DATEDIFF(day, hire_date, GETDATE()) AS working_days
FROM employees;
```

### 2.3 提取年月日

提取年月日常用于分组统计，例如按年、按月统计订单数量。

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 年 | `YEAR(hire_date)` | `EXTRACT(YEAR FROM hire_date)` | `EXTRACT(YEAR FROM hire_date)` | `YEAR(hire_date)` |
| 月 | `MONTH(hire_date)` | `EXTRACT(MONTH FROM hire_date)` | `EXTRACT(MONTH FROM hire_date)` | `MONTH(hire_date)` |
| 日 | `DAY(hire_date)` | `EXTRACT(DAY FROM hire_date)` | `EXTRACT(DAY FROM hire_date)` | `DAY(hire_date)` |

示例：按入职年月统计员工数量。

MySQL：

```sql
SELECT YEAR(hire_date) AS hire_year,
       MONTH(hire_date) AS hire_month,
       COUNT(*) AS employee_count
FROM employees
GROUP BY YEAR(hire_date), MONTH(hire_date);
```

Oracle / PostgreSQL：

```sql
SELECT EXTRACT(YEAR FROM hire_date) AS hire_year,
       EXTRACT(MONTH FROM hire_date) AS hire_month,
       COUNT(*) AS employee_count
FROM employees
GROUP BY EXTRACT(YEAR FROM hire_date),
         EXTRACT(MONTH FROM hire_date);
```

SQL Server：

```sql
SELECT YEAR(hire_date) AS hire_year,
       MONTH(hire_date) AS hire_month,
       COUNT(*) AS employee_count
FROM employees
GROUP BY YEAR(hire_date), MONTH(hire_date);
```

### 2.4 月初和月末

月初、月末常用于月报统计。

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 当前月月初 | `DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')` | `TRUNC(SYSDATE, 'MM')` | `DATE_TRUNC('month', CURRENT_DATE)` | `DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1)` |
| 当前月月末 | `LAST_DAY(CURRENT_DATE)` | `LAST_DAY(SYSDATE)` | `DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month - 1 day'` | `EOMONTH(GETDATE())` |

示例：查询本月入职的员工。

MySQL：

```sql
SELECT id, name, hire_date
FROM employees
WHERE hire_date >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
  AND hire_date <= LAST_DAY(CURRENT_DATE);
```

Oracle：

```sql
SELECT id, name, hire_date
FROM employees
WHERE hire_date >= TRUNC(SYSDATE, 'MM')
  AND hire_date <= LAST_DAY(SYSDATE);
```

PostgreSQL：

```sql
SELECT id, name, hire_date
FROM employees
WHERE hire_date >= DATE_TRUNC('month', CURRENT_DATE)
  AND hire_date < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month';
```

SQL Server：

```sql
SELECT id, name, hire_date
FROM employees
WHERE hire_date >= DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1)
  AND hire_date <= EOMONTH(GETDATE());
```

## 三、数值函数

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 四舍五入 | `ROUND(value,2)` | `ROUND(value,2)` | `ROUND(value,2)` | `ROUND(value,2)` |
| 向上取整 | `CEIL(value)` | `CEIL(value)` | `CEIL(value)` | `CEILING(value)` |
| 向下取整 | `FLOOR(value)` | `FLOOR(value)` | `FLOOR(value)` | `FLOOR(value)` |
| 绝对值 | `ABS(value)` | `ABS(value)` | `ABS(value)` | `ABS(value)` |

## 四、类型转换函数

| 数据库 | 写法示例 |
| --- | --- |
| MySQL | `CAST('100' AS SIGNED)` |
| Oracle | `TO_NUMBER('100')` |
| PostgreSQL | `CAST('100' AS INTEGER)` 或 `'100'::INTEGER` |
| SQL Server | `CAST('100' AS INT)` 或 `CONVERT(INT, '100')` |

## 五、空值处理

```sql
SELECT name,
       COALESCE(email, '未设置') AS email_text
FROM employees;
```

`COALESCE` 会返回第一个非 `NULL` 的值。

## 六、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 混用日期函数 | 四种数据库日期函数差异大 | 按数据库选择函数 |
| SQL Server 用 `LENGTH` | SQL Server 使用 `LEN` | 改为 `LEN(name)` |
| Oracle 字符拼接用 `+` | Oracle 字符串拼接使用 `||` | 改为 `a || b` |

## 七、本章练习

请完成：

1. 查询员工姓名的大写形式。
2. 查询入职日期格式化后的字符串。
3. 查询工资除以 10000 后保留 2 位小数。
4. 邮箱为 `NULL` 时显示 `未设置`。

## 八、本章总结

- 常用函数包括字符串、日期、数值、转换和空值处理。
- 函数是四种数据库差异最明显的部分之一。
- `COALESCE` 是比较通用的空值处理函数。
