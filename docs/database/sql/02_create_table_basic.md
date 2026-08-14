# 第2章 CREATE DATABASE、SCHEMA 与 CREATE TABLE

> 本章目标：掌握创建数据库、Schema 和表的基本语法，理解字段、数据类型和四种数据库建表差异。

## 一、创建数据库或 Schema

创建表之前，需要先准备保存表的空间。

四种数据库写法：

| 数据库 | 写法 | 说明 |
| --- | --- | --- |
| MySQL | `CREATE DATABASE 数据库名;` | 创建数据库 |
| PostgreSQL | `CREATE DATABASE 数据库名;` | 创建数据库 |
| SQL Server | `CREATE DATABASE 数据库名;` | 创建数据库 |
| Oracle | `CREATE USER 用户名 IDENTIFIED BY 密码;` | 通常先创建用户，用户拥有自己的 Schema |

MySQL 示例：

```sql
CREATE DATABASE training_db;
```

PostgreSQL 示例：

```sql
CREATE DATABASE training_db;
```

SQL Server 示例：

```sql
CREATE DATABASE training_db;
```

Oracle 示例：

```sql
CREATE USER training_user IDENTIFIED BY password;
GRANT CONNECT, RESOURCE TO training_user;
```

## 二、CREATE TABLE 基本语法

建表用于定义数据保存格式。

通用结构：

```sql
CREATE TABLE 表名 (
    列名 数据类型 约束,
    列名 数据类型 约束
);
```

员工表设计：

| 字段 | 含义 |
| --- | --- |
| `id` | 员工 ID |
| `name` | 员工姓名 |
| `department_id` | 部门 ID |
| `email` | 邮箱 |
| `hire_date` | 入职日期 |
| `salary` | 工资 |

## 三、常用数据类型对比

| 用途 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| 整数 | `INT` | `NUMBER(10)` | `INTEGER` | `INT` |
| 长整数 | `BIGINT` | `NUMBER(19)` | `BIGINT` | `BIGINT` |
| 小数 | `DECIMAL(10,2)` | `NUMBER(10,2)` | `NUMERIC(10,2)` | `DECIMAL(10,2)` |
| 字符串 | `VARCHAR(100)` | `VARCHAR2(100)` | `VARCHAR(100)` | `VARCHAR(100)` |
| 日期 | `DATE` | `DATE` | `DATE` | `DATE` |
| 日期时间 | `DATETIME` | `TIMESTAMP` | `TIMESTAMP` | `DATETIME2` |

## 四、四种数据库建表示例

MySQL：

```sql
CREATE TABLE employees (
    id BIGINT,
    name VARCHAR(100),
    department_id BIGINT,
    email VARCHAR(200),
    hire_date DATE,
    salary DECIMAL(10, 2)
);
```

Oracle：

```sql
CREATE TABLE employees (
    id NUMBER(19),
    name VARCHAR2(100),
    department_id NUMBER(19),
    email VARCHAR2(200),
    hire_date DATE,
    salary NUMBER(10, 2)
);
```

PostgreSQL：

```sql
CREATE TABLE employees (
    id BIGINT,
    name VARCHAR(100),
    department_id BIGINT,
    email VARCHAR(200),
    hire_date DATE,
    salary NUMERIC(10, 2)
);
```

SQL Server：

```sql
CREATE TABLE employees (
    id BIGINT,
    name VARCHAR(100),
    department_id BIGINT,
    email VARCHAR(200),
    hire_date DATE,
    salary DECIMAL(10, 2)
);
```

## 五、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 忘记分号 | SQL 结束位置不明确 | 每条 SQL 后写 `;` |
| 字段类型随便写 | 不同数据库类型名称不同 | 按数据库选择类型 |
| Oracle 使用 `VARCHAR` | Oracle 项目更常用 `VARCHAR2` | 使用 `VARCHAR2` |
| 表名字段名不统一 | 后续 SQL 难以维护 | 使用清晰命名 |

## 六、本章练习

请完成：

1. 分别写出 MySQL、Oracle、PostgreSQL、SQL Server 创建员工表的 SQL。
2. 给员工表增加 `phone` 字段设计。
3. 说明 `VARCHAR(100)` 中的 `100` 表示什么。

## 七、本章总结

- 建表前要先准备数据库或 Schema。
- `CREATE TABLE` 用于定义表结构。
- 四种数据库的数据类型名称存在差异。
