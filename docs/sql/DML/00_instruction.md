# DML 基础语法详解

DML（Data Manipulation Language，数据操作语言）主要用于对数据库表中已存在的数据进行操作。

常见命令包括：

| 类型 | 命令 | 作用 |
|------|------|------|
| 插入数据 | `INSERT` | 向表中添加新行 |
| 更新数据 | `UPDATE` | 修改已存在的行 |
| 删除数据 | `DELETE` | 删除特定数据 |
| 查询数据 | `SELECT` | 检索数据（有时归类为 DQL） |

---

## 一、INSERT（插入数据）

### 插入单行

```sql
INSERT INTO 表名 (列1, 列2, 列3, ...)
VALUES (值1, 值2, 值3, ...);
```

示例：

```sql
INSERT INTO employees (id, name, salary)
VALUES (1, 'Alice', 6000);
```

### 省略列名（需所有列顺序一致）

```sql
INSERT INTO employees VALUES (2, 'Bob', 7000);
```

### 批量插入多行（MySQL / PostgreSQL 支持）

```sql
INSERT INTO employees (id, name, salary)
VALUES 
  (3, 'Cathy', 6500),
  (4, 'David', 8000);
```

### 基于查询插入（INSERT INTO ... SELECT）

```sql
INSERT INTO backup_employees (id, name, salary)
SELECT id, name, salary FROM employees WHERE salary > 7000;
```

💡 用于数据迁移或备份。

---

## 二、UPDATE（更新数据）

### 基本语法

```sql
UPDATE 表名
SET 列1 = 新值1, 列2 = 新值2, ...
WHERE 条件;
```

示例：

```sql
UPDATE employees
SET salary = salary + 500
WHERE department_id = 10;
```

⚠️ **没有 WHERE 会更新整张表！**

## 三、DELETE（删除数据）

### 删除基本语法

```sql
DELETE FROM 表名
WHERE 条件;
```

示例：

```sql
DELETE FROM employees WHERE salary < 4000;
```

⚠️ **省略 WHERE 会删除整张表所有数据。**

### 删除全部数据但保留结构

```sql
DELETE FROM employees;
```

💡 若要更快清空表（不记录日志）：

```sql
TRUNCATE TABLE employees;
```

（属于 DDL，但效果类似）

---

## 四、SELECT（查询数据）

虽然通常归类为 DQL，但在 DML 学习中也必须掌握。

### 查询的基本语法

```sql
SELECT 列1, 列2, ...
FROM 表名
WHERE 条件
GROUP BY 分组列
HAVING 分组条件
ORDER BY 排序列
LIMIT 限制行数;   -- MySQL / PostgreSQL
```

示例：

```sql
SELECT name, salary
FROM employees
WHERE department_id = 1
ORDER BY salary DESC
LIMIT 5;
```

---

## 五、DML 与 DDL、DCL 区别

| 类别 | 全称 | 作用 | 示例 |
|------|------|------|------|
| **DML** | Data Manipulation Language | 操作数据 | INSERT / UPDATE / DELETE / SELECT |
| **DDL** | Data Definition Language | 定义结构 | CREATE / ALTER / DROP / TRUNCATE |
| **DCL** | Data Control Language | 控制权限 | GRANT / REVOKE |

---

## 九、示例总结（最常用操作）

```sql
-- 插入数据
INSERT INTO users (id, name, age) VALUES (1, 'Lin', 25);

-- 更新数据
UPDATE users SET age = 26 WHERE id = 1;

-- 删除数据
DELETE FROM users WHERE id = 1;

-- 查询数据
SELECT id, name, age FROM users;
```
