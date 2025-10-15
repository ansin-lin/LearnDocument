# 语法结构与基本查询

## 创建数据库

虽然我们叫创建数据库，但并不是真正意义上的建一个数据库系统，而是创建一个逻辑命名空间。

### 建库语法(mysql/postgresql/sqlserve)

```sql
CREATE DATABASE 数据库名称
```

示例

```sql
CREATE DATABASE my_db
```

### 建库语法(oracle)

```sql
CREATE USER <schema> IDENTIFIED BY <password>;
```

示例

```sql
CREATE USER mydb IDENTIFIED BY pwd;
```

## 创建表（CREATE TABLE）  

### 建表语法

```sql
CREATE TABLE 表名称
(
    列名称1 数据类型,
    列名称2 数据类型,
    列名称3 数据类型,
    ....
)
```

### 建表示例

```sql
CREATE TABLE 表名 (
    id INT ,
    name VARCHAR(50),
    age INT,
    score DECIMAL(5,2)
);
```

---

## 小结  

| 主题 | 关键命令 | 作用 |
|------|-----------|------|
| 建表与结构 | CREATE, ALTER, DROP | 定义或修改表结构 |
| 约束控制 | PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK | 保证数据有效性 |
| 辅助机制 | INDEX, VIEW, AUTO_INCREMENT | 提升性能与便利性 |

---
