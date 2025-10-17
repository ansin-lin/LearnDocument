# SQL 基础查询语法（SELECT）

## 一、SELECT 语句概述

**SELECT** 是 SQL 中最核心、最常用的语句，用于从数据库中查询并返回指定的数据。

### 基本结构

```sql
SELECT [DISTINCT] 列名1, 列名2, ...
FROM 表名
[join 其他表]
[on 结合条件]
[WHERE 条件]
[GROUP BY 分组列]
[HAVING 聚合条件]
[ORDER BY 排序列 ASC|DESC]
[LIMIT n OFFSET m];  -- MySQL / PostgreSQL
```

### 执行顺序

1. FROM —— 指定数据来源表
2. join —— 其他表
3. on —— 结合条件
4. WHERE —— 筛选行  
5. GROUP BY —— 按列分组  
6. HAVING —— 过滤分组结果  
7. SELECT —— 选择输出列  
8. ORDER BY —— 排序  
9. LIMIT / OFFSET —— 限制返回行数

---

## 二、查询所有字段与指定字段

### 查询所有字段

```sql
SELECT * FROM students;
```

### 查询指定字段

```sql
SELECT name, age FROM students;
```

💡 建议：生产环境中尽量避免使用 `*`，仅查询需要的字段以提高性能。

---

## 三、列别名与表达式

### 使用 AS 定义列别名

```sql
SELECT name AS 姓名, age AS 年龄 FROM students;
```

### 使用表达式

```sql
SELECT name, age + 1 AS 明年年龄 FROM students;
```

### 拼接字符串（MySQL 示例）

```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM employees;
```

### 使用 AS 定义表别名

```sql
SELECT name FROM employees AS e;
```

---

## 四、去重与条件查询

### 去重（DISTINCT）

```sql
SELECT DISTINCT department FROM employees;
```

### 条件查询（WHERE）

| 操作符 | 说明 | 示例 |
|---------|------|------|
| = | 等于 | `WHERE age = 20` |
| <> 或 != | 不等于 | `WHERE gender <> 'F'` |
| >, <, >=, <= | 比较大小 | `WHERE score >= 90` |
| BETWEEN ... AND ... | 范围 | `WHERE age BETWEEN 18 AND 25` |
| IN (...) | 在集合中 | `WHERE city IN ('Tokyo','Osaka')` |
| LIKE | 模糊匹配 | `WHERE name LIKE 'A%'` |
| IS NULL / IS NOT NULL | 空值判断 | `WHERE phone IS NULL` |

常用通配符(搭配like)

|通配符|含义|示例|说明|
|---|------|------|------|
|%|任意长度的任意字符（可为0个）|'A%'|以 A 开头的所有值|
|_|任意单个字符|'A_'|A 开头且后面只有一个字符|
|[]|指定字符集中的任意一个字符（仅 SQL Server 支持）|'[AB]%'|以 A 或 B 开头的值|
|[^]|不在字符集内的任意一个字符（仅 SQL Server 支持）|'[^A]%'|不以 A 开头的值|

---

## 五、排序与分页

### 排序（ORDER BY）

```sql
SELECT * FROM students ORDER BY score DESC;
```

### 多列排序

```sql
SELECT * FROM students ORDER BY score DESC, age ASC;
```

### 分页查询（不同数据库语法）

| 数据库 | 语法 |
|--------|------|
| **MySQL / PostgreSQL** | `LIMIT 10 OFFSET 20` |
| **SQL Server** | `OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY` |
| **Oracle** | `OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY`（12c+） |

示例：

```sql
SELECT * FROM students ORDER BY id LIMIT 5 OFFSET 0;
```

---

## 六、视图与虚拟表查询

### 创建视图

```sql
CREATE VIEW top_students AS
SELECT name, score FROM students WHERE score >= 90;
```

### 更新视图

```sql
CREATE OR REPLACE VIEW top_students AS
SELECT name, score FROM students WHERE score >= 70;
```

### 查询视图

```sql
SELECT * FROM top_students;
```

### 删除视图

```sql
DROP VIEW top_students;
```

---
