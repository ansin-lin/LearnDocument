# 多表查询与高级查询操作

## 一、多表连接（JOIN）

在实际项目中，我们往往需要同时查询多张表的数据，比如学生信息和成绩信息。此时就需要使用 **JOIN（连接查询）**。

| 类型 | 说明 | 示例 |
|------|------|------|
| INNER JOIN | 仅返回匹配记录 | `SELECT * FROM A INNER JOIN B ON A.id = B.aid;` |
| LEFT JOIN | 返回左表所有记录 | `SELECT * FROM A LEFT JOIN B ON A.id = B.aid;` |
| RIGHT JOIN | 返回右表所有记录 | `SELECT * FROM A RIGHT JOIN B ON A.id = B.aid;` |
| FULL JOIN | 返回左右表所有记录 | `SELECT * FROM A FULL JOIN B ON A.id = B.aid;` |

---

### 1.1 内连接（INNER JOIN）

**语法：**

```sql
SELECT A.列名, B.列名
FROM 表A AS A
INNER JOIN 表B AS B
ON A.公共列 = B.公共列;
```

**示例：**

```sql
SELECT s.name, c.course_name
FROM students s
INNER JOIN courses c
ON s.course_id = c.id;
```

💡 **特点**：只返回两表中匹配的记录。

---

### 1.2 左连接（LEFT JOIN）

**语法：**

```sql
SELECT A.*, B.*
FROM 表A
LEFT JOIN 表B
ON A.公共列 = B.公共列;
```

**示例：**

```sql
SELECT s.name, c.course_name
FROM students s
LEFT JOIN courses c
ON s.course_id = c.id;
```

💡 左表数据全部显示，即使右表无匹配值（右表对应列为 NULL）。

---

### 1.3 右连接（RIGHT JOIN）

```sql
SELECT s.name, c.course_name
FROM students s
RIGHT JOIN courses c
ON s.course_id = c.id;
```

💡 右表数据全部显示，即使左表无匹配值。

---

### 1.4 全连接（FULL JOIN）

```sql
SELECT s.name, c.course_name
FROM students s
FULL JOIN courses c
ON s.course_id = c.id;
```

💡 返回左表或右表中有匹配或未匹配的所有行。

⚠️ **注意**：MySQL 不支持 FULL JOIN，可通过 `UNION` 模拟：

```sql
SELECT * FROM A LEFT JOIN B ON ...
UNION
SELECT * FROM A RIGHT JOIN B ON ...;
```

---

### 1.5 自连接（SELF JOIN）

一个表自己与自己连接，常用于表示层级关系。

**示例：**

```sql
SELECT e1.name AS 员工, e2.name AS 上级
FROM employees e1
LEFT JOIN employees e2
ON e1.manager_id = e2.id;
```

---

### 1.6 多表连接

可以同时连接三张及以上表：

```sql
SELECT s.name, c.course_name, t.teacher_name
FROM students s
JOIN courses c ON s.course_id = c.id
JOIN teachers t ON c.teacher_id = t.id;
```

---

### 1.7 不同数据库 JOIN 对比

| 数据库 | 支持的连接类型 | 特殊说明 |
|---------|----------------|-----------|
| MySQL | INNER, LEFT, RIGHT | 不支持 FULL JOIN，可用 UNION 模拟 |
| SQL Server | INNER, LEFT, RIGHT, FULL | 支持全连接 |
| PostgreSQL | 全部支持 | 完整标准实现 |
| Oracle | INNER, LEFT, RIGHT, FULL | 旧版使用 `(+)` 语法 |

---

## 二、子查询（Subquery）

**定义**：子查询是嵌套在另一条 SQL 语句中的查询结果，用于提供动态条件或中间结果集。

---

### 2.1 子查询类型

| 类型 | 返回结果 | 使用位置 |
|------|-----------|-----------|
| 标量子查询 | 单个值 | WHERE、SELECT |
| 列子查询 | 一列多行 | IN、NOT IN |
| 行子查询 | 多列 | 比较运算 |
| 表子查询 | 一张虚拟表 | FROM |
| EXISTS 子查询 | 布尔结果 | EXISTS、NOT EXISTS |

---

### 2.2 标量子查询

返回单个值。

```sql
SELECT name, age
FROM students
WHERE score = (SELECT MAX(score) FROM students);
```

---

### 2.3 列子查询

```sql
SELECT name FROM students
WHERE id IN (SELECT student_id FROM scores WHERE course='Math');
```

---

### 2.4 行子查询

```sql
SELECT name FROM employees
WHERE (dept_id, salary) IN (SELECT dept_id, MAX(salary) FROM employees GROUP BY dept_id);
```

---

### 2.5 表子查询（FROM 子句中）

```sql
SELECT sub.dept_id, AVG(sub.salary)
FROM (
  SELECT dept_id, salary FROM employees WHERE salary > 3000
) AS sub
GROUP BY sub.dept_id;
```

---

### 2.6 EXISTS 与 NOT EXISTS

```sql
SELECT name FROM students s
WHERE EXISTS (SELECT 1 FROM scores sc WHERE sc.student_id = s.id);
```

💡 EXISTS 判断子查询结果是否存在，通常性能优于 IN。

---

### 2.7 子查询优化建议

- 尽量用 **JOIN** 替代复杂子查询。
- 对子查询中的过滤列建立索引。
- 使用 **EXISTS** 替代 **IN**（在大数据量下更高效）。

---

## 三、集合操作（Set Operations）

集合操作用于将多个查询结果合并。要求列数与类型一致。

---

### 3.1 UNION（并集，去重）

```sql
SELECT name FROM students_2024
UNION
SELECT name FROM students_2025;
```

💡 自动去重。

---

### 3.2 UNION ALL（并集，不去重）

```sql
SELECT name FROM students_2024
UNION ALL
SELECT name FROM students_2025;
```

💡 性能更好，不去重。

---

### 3.3 INTERSECT（交集）

```sql
SELECT name FROM students_2024
INTERSECT
SELECT name FROM students_2025;
```

💡 MySQL 不支持，可用 INNER JOIN 模拟：

```sql
SELECT s1.name FROM students_2024 s1
INNER JOIN students_2025 s2 ON s1.name = s2.name;
```

---

### 3.4 EXCEPT / MINUS（差集）

```sql
-- PostgreSQL / SQL Server
SELECT name FROM students_2024
EXCEPT
SELECT name FROM students_2025;

-- Oracle
SELECT name FROM students_2024
MINUS
SELECT name FROM students_2025;
```

💡 返回仅在第一个查询中存在的数据。

---

### 3.5 集合操作规则

| 项目 | 要求 |
|------|------|
| 列数 | 必须相同 |
| 列类型 | 对应位置兼容 |
| 列名 | 取第一个 SELECT 的列名 |
| ORDER BY | 放在整个语句末尾 |

---

## 四、综合实例：多表 + 子查询 + 集合操作

```sql
-- 查询所有平均分高于全校平均分的学生姓名
SELECT s.name, AVG(sc.score) AS avg_score
FROM students s
JOIN scores sc ON s.id = sc.student_id
GROUP BY s.name
HAVING AVG(sc.score) > (SELECT AVG(score) FROM scores);
```

```sql
-- 合并不同年度选课学生（去重）
SELECT student_id FROM enrollment_2024
UNION
SELECT student_id FROM enrollment_2025;
```

```sql
-- 查询2024年选课但2025年未选课的学生
SELECT student_id FROM enrollment_2024
EXCEPT
SELECT student_id FROM enrollment_2025;
```

---

## 五、总结

| 分类 | 关键语法 | 说明 |
|------|-----------|------|
| 表连接 | JOIN | 多表间数据整合 |
| 子查询 | SELECT 嵌套 | 动态条件查询 |
| 集合操作 | UNION / INTERSECT / EXCEPT | 合并、交集、差集 |
| 性能优化 | 索引 / EXISTS / JOIN 替代 | 提升执行效率 |

---
