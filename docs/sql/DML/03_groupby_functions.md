# 分组查询与聚合函数详解

## 一、聚合函数（Aggregate Functions）

聚合函数用于对一组数据进行统计计算（如求总数、平均值、最大值等），常与 `GROUP BY` 一起使用。

| 函数名 | 作用 | 示例 |
|--------|------|------|
| COUNT() | 统计数量 | `SELECT COUNT(*) FROM students;` |
| SUM() | 求和 | `SELECT SUM(score) FROM scores;` |
| AVG() | 求平均值 | `SELECT AVG(score) FROM scores;` |
| MAX() | 求最大值 | `SELECT MAX(score) FROM scores;` |
| MIN() | 求最小值 | `SELECT MIN(score) FROM scores;` |

---

### 示例

```sql
SELECT COUNT(*) AS 学生总数,
       AVG(score) AS 平均分,
       MAX(score) AS 最高分,
       MIN(score) AS 最低分
FROM scores;
```

💡 注意：

- 聚合函数会忽略 `NULL` 值。  
- 若要统计包括 `NULL` 的数量，可使用 `COUNT(*)`。

---

## 二、GROUP BY 分组查询

### 2.1 基本语法

```sql
SELECT 分组字段, 聚合函数(字段)
FROM 表名
GROUP BY 分组字段;
```

**示例：** 统计每个班级的平均分：

```sql
SELECT class_id, AVG(score) AS 平均分
FROM scores
GROUP BY class_id;
```

---

### 2.2 多字段分组

```sql
SELECT class_id, subject, AVG(score)
FROM scores
GROUP BY class_id, subject;
```

💡 分组时应确保 `SELECT` 中的非聚合字段全部出现在 `GROUP BY` 中。

---

### 2.3 聚合函数结合分组

```sql
SELECT dept_id, COUNT(*) AS 员工数, AVG(salary) AS 平均工资
FROM employees
GROUP BY dept_id;
```

---

### 2.4 分组后的列约束

聚合查询中，`SELECT` 后的列必须满足以下条件之一：

1. 出现在 `GROUP BY` 中；  
2. 被聚合函数包裹。

否则会报错（如 MySQL 的 `ONLY_FULL_GROUP_BY` 模式）。

---

## 三、HAVING 子句

### 3.1 作用

`HAVING` 用于过滤分组后的结果，而 `WHERE` 用于过滤分组前的记录。

### 3.2 区别

| 比较项 | WHERE | HAVING |
|--------|--------|--------|
| 执行阶段 | 分组前 | 分组后 |
| 可使用聚合函数 | ❌ 否 | ✅ 是 |
| 示例 | `WHERE age > 18` | `HAVING AVG(score) > 80` |

---

### 3.3 示例

```sql
SELECT class_id, AVG(score) AS 平均分
FROM scores
GROUP BY class_id
HAVING AVG(score) > 85;
```

💡 HAVING 可以同时结合多条件：

```sql
HAVING AVG(score) > 85 AND COUNT(*) > 10;
```

---

## 四、常见聚合技巧

### 4.1 统计去重数量

```sql
SELECT COUNT(DISTINCT student_id) AS 不重复学生数
FROM scores;
```

---

### 4.2 条件聚合（CASE WHEN）

```sql
SELECT
  SUM(CASE WHEN score >= 60 THEN 1 ELSE 0 END) AS 及格人数,
  SUM(CASE WHEN score < 60 THEN 1 ELSE 0 END) AS 不及格人数
FROM scores;
```

---

### 4.3 NULL 值处理

```sql
SELECT AVG(COALESCE(score, 0)) AS 平均分
FROM scores;
```

💡 `COALESCE(a,b)` 表示当 `a` 为 NULL 时取 `b`。

---

## 五、性能优化与注意事项

1. 尽量在 `GROUP BY` 字段上建立索引。  
2. 避免在 `HAVING` 中使用非聚合条件，可提前放入 `WHERE`。  
3. 对大表的聚合，可考虑使用分区表或物化视图。  
4. 避免在 `SELECT` 中直接嵌套复杂聚合函数。

---

## 六、综合实战

```sql
-- 统计每个部门员工数、平均薪资，并筛选出平均薪资大于 8000 的部门
SELECT dept_id, COUNT(*) AS 员工数, AVG(salary) AS 平均薪资
FROM employees
GROUP BY dept_id
HAVING AVG(salary) > 8000
ORDER BY 平均薪资 DESC;
```

```sql
-- 使用 ROLLUP 汇总全公司薪资统计
SELECT dept_id, SUM(salary) AS 部门薪资
FROM employees
GROUP BY dept_id WITH ROLLUP;
```

---

## 七、总结

| 分类 | 关键语法 | 功能说明 |
|------|------------|------------|
| 聚合函数 | COUNT / SUM / AVG / MAX / MIN | 汇总统计 |
| GROUP BY | GROUP BY 字段 | 分组统计 |
| HAVING | HAVING 条件 | 过滤分组结果 |
| ROLLUP / CUBE | GROUP BY ... WITH ROLLUP / CUBE | 多层次聚合汇总 |
| CASE WHEN | 条件聚合 | 灵活分类统计 |

---
