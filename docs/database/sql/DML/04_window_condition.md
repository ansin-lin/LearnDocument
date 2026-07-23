# 窗口函数与条件表达式详解

---

## 一、窗口函数（Window Functions）

窗口函数是 SQL 中用于对**一组相关行**执行计算的函数，  
与聚合函数不同的是，窗口函数不会将多行合并成一行，而是保留每一行的详细结果。

---

### 1.1 基本语法

```sql
函数名(表达式) OVER (
    [PARTITION BY 分组字段]
    [ORDER BY 排序字段]
    [ROWS BETWEEN 范围定义]
)
```

**参数说明：**

- `PARTITION BY`：定义分区（类似分组）
- `ORDER BY`：定义排序规则
- `ROWS BETWEEN`：定义计算窗口范围（如当前行前后几行）

---

### 1.2 常见窗口函数

| 类型 | 函数 | 说明 |
|------|------|------|
| 排名类 | ROW_NUMBER() | 连续排名，不重复 |
| 排名类 | RANK() | 遇到并列会跳过名次 |
| 排名类 | DENSE_RANK() | 遇到并列不会跳过名次 |
| 聚合类 | SUM()、AVG()、COUNT()、MAX()、MIN() | 在窗口内聚合 |
| 偏移类 | LAG(expr,n,default) | 获取当前行之前第 n 行的值 |
| 偏移类 | LEAD(expr,n,default) | 获取当前行之后第 n 行的值 |

---

### 1.3 示例：排名函数

```sql
SELECT
  student_id,
  class_id,
  score,
  ROW_NUMBER() OVER(PARTITION BY class_id ORDER BY score DESC) AS 排名
FROM scores;
```

📘 说明：  
按 `class_id` 分区（每个班独立排名），按 `score` 降序排列。

---

### 1.4 示例：累计求和（SUM）

```sql
SELECT
  emp_id,
  dept_id,
  salary,
  SUM(salary) OVER(PARTITION BY dept_id ORDER BY emp_id) AS 累计工资
FROM employees;
```

💡 每个部门内部按员工编号顺序，计算到当前行为止的工资累计。

---

### 1.5 示例：环比与同比（LAG / LEAD）

```sql
SELECT
  year,
  sales,
  LAG(sales, 1) OVER (ORDER BY year) AS 去年销售,
  sales - LAG(sales, 1) OVER (ORDER BY year) AS 同比增长
FROM sales_record;
```

💡 `LAG()` 向上取前一行数据，可计算同比增长。

---

### 1.6 示例：移动平均

```sql
SELECT
  month,
  sales,
  AVG(sales) OVER (
    ORDER BY month
    ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
  ) AS 移动平均
FROM sales_record;
```

💡 计算当前月及前 2 个月的平均销售额。

---

## 二、条件控制语句

条件控制语句用于在 SQL 中实现“逻辑判断”，常见于分类统计或条件聚合。

---

### 2.1 CASE WHEN 语法

```sql
CASE
  WHEN 条件1 THEN 结果1
  WHEN 条件2 THEN 结果2
  ELSE 默认值
END
```

#### 示例

```sql
SELECT
  name,
  score,
  CASE
    WHEN score >= 90 THEN '优秀'
    WHEN score >= 60 THEN '及格'
    ELSE '不及格'
  END AS 等级
FROM students;
```

---

### 2.2 各数据库差异

| 数据库 | 条件函数 | 示例 |
|---------|-----------|------|
| MySQL | `IF(condition, true_value, false_value)` | `SELECT IF(score>=60, 'Pass', 'Fail')` |
| SQL Server | `IIF(condition, true_value, false_value)` | `SELECT IIF(score>=60, 'Pass', 'Fail')` |
| Oracle | `DECODE(expr, value1, result1, value2, result2, default)` | `SELECT DECODE(status, 'A', '启用', 'B', '禁用', '未知')` |
| PostgreSQL | 仅支持标准 CASE WHEN | 同上标准语法 |

---

### 2.3 NULL 处理函数

| 数据库 | 函数 | 用法 |
|----------|------|------|
| 通用 | COALESCE(a,b,...) | 返回第一个非 NULL 值 |
| MySQL | IFNULL(a,b) | 若 a 为 NULL，返回 b |
| Oracle | NVL(a,b) | 若 a 为 NULL，返回 b |

#### 示例1

```sql
SELECT
  name,
  COALESCE(email, '未填写') AS 邮箱,
  IFNULL(phone, '无手机号') AS 手机号
FROM users;
```

---

### 2.4 条件聚合示例

```sql
SELECT
  SUM(CASE WHEN gender='M' THEN 1 ELSE 0 END) AS 男生数,
  SUM(CASE WHEN gender='F' THEN 1 ELSE 0 END) AS 女生数
FROM students;
```

💡 `CASE` 可以与聚合函数结合使用，用于分类计数或条件求和。

---

## 三、综合实战案例

### 3.1 案例：部门内薪资排名

```sql
SELECT
  emp_id,
  dept_id,
  salary,
  RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS 薪资排名
FROM employees;
```

---

### 3.2 案例：分类计数与条件求和

```sql
SELECT
  dept_id,
  SUM(CASE WHEN salary >= 8000 THEN 1 ELSE 0 END) AS 高薪人数,
  SUM(CASE WHEN salary < 8000 THEN 1 ELSE 0 END) AS 普通人数
FROM employees
GROUP BY dept_id;
```

---

### 3.3 案例：同比增长率计算

```sql
SELECT
  year,
  sales,
  LAG(sales,1) OVER (ORDER BY year) AS 上年销售额,
  ROUND((sales - LAG(sales,1) OVER (ORDER BY year)) / LAG(sales,1) OVER (ORDER BY year) * 100, 2) AS 增长率
FROM sales_record;
```

---

### 3.4 案例：近三期平均分

```sql
SELECT
  exam_id,
  student_id,
  AVG(score) OVER (
    PARTITION BY student_id
    ORDER BY exam_id
    ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
  ) AS 最近三次平均分
FROM exam_scores;
```

---

## 四、优化与注意事项

1. **窗口函数** 是在数据查询后执行的，不会减少结果集行数。  
2. 合理使用 `PARTITION BY` 与 `ORDER BY` 可提高性能。  
3. 对大数据量的窗口分析，可配合索引或物化视图优化。  
4. 在 MySQL 中，窗口函数从 8.0 版本开始支持。  
5. 在 Oracle / PostgreSQL 中，窗口函数功能更丰富（支持 `RANGE` 窗口）。

---

✅ **总结表：**

| 分类 | 关键函数 | 用途 |
|------|-----------|------|
| 排名类 | ROW_NUMBER / RANK / DENSE_RANK | 排名与分组排序 |
| 偏移类 | LAG / LEAD | 获取前后行值（同比环比） |
| 聚合类 | SUM / AVG / COUNT | 分区内累计或平均 |
| 条件类 | CASE / IF / IIF / DECODE | 实现条件判断 |
| 空值处理 | COALESCE / IFNULL / NVL | 替换 NULL |

---
