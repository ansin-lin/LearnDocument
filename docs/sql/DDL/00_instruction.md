# SQL 基础入门

## 什么是 SQL  

**SQL（Structured Query Language，结构化查询语言）**  
是一种用于操作关系型数据库（如 MySQL、PostgreSQL、Oracle、SQL Server 等）的标准语言。

SQL 的主要功能包括：

- 定义数据库和表的结构；
- 插入、更新、删除数据；
- 查询和分析数据；
- 管理用户权限与安全性。

---

## SQL 语法分类

| 类型 | 名称 | 说明 | 常见命令 |
|------|------|------|----------|
| DDL | 数据定义语言 | 定义数据库结构 | `CREATE`, `ALTER`, `DROP` |
| DML | 数据操作语言 | 增删改查数据 | `INSERT`, `UPDATE`, `DELETE`, `SELECT`|
| DCL | 数据控制语言 | 控制权限 | `GRANT`, `REVOKE` |

**提示**：  
SQL 对大小写不敏感，但建议关键字使用 **大写**，以提高可读性。

---

## 常用数据类型  

| 类型 | 说明 | 示例 |
|------|------|------|
| INT | 整数 | `age INT` |
| DECIMAL(M,D) | 定点小数（共 M 位，小数 D 位） | `price DECIMAL(8,2)` |
| VARCHAR(n) | 可变长度字符串 | `name VARCHAR(50)` |
| CHAR(n) | 固定长度字符串 | `gender CHAR(1)` |
| DATE | 日期类型 | `birth DATE` |
| DATETIME | 日期 + 时间 | `created_at DATETIME` |
| BOOLEAN | 布尔值（真/假） | `is_active BOOLEAN` |

---
