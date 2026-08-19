# 第0章 数据库与 SQL 入门

> 本章目标：理解数据库、表、行、列、主键和 SQL 的基本作用，知道 MySQL、Oracle、PostgreSQL、SQL Server 的定位差异。

## 一、为什么需要数据库

程序运行时，数据通常保存在内存中。程序关闭后，内存中的数据会消失。

数据库用于长期保存数据，这个过程叫持久化。

常见持久化方式：

| 方式 | 示例 | 特点 |
| --- | --- | --- |
| 文件 | TXT、CSV、Excel、JSON | 简单，但复杂查询和并发处理较弱 |
| 关系型数据库 | MySQL、Oracle、PostgreSQL、SQL Server | 适合业务系统，支持 SQL、事务和权限 |
| 非关系型数据库 | Redis、MongoDB | 适合缓存、文档、搜索等特殊场景 |

Web 系统开发中，用户、商品、订单、权限、日志等数据通常都保存在数据库中。

## 二、数据库中的基本概念

关系型数据库的数据像 Excel 表格一样，由表、行和列组成。

| 概念 | 英文 | 说明 |
| --- | --- | --- |
| 数据库 | Database | 保存一组业务数据的空间 |
| 表 | Table | 保存同一种业务数据 |
| 行 | Row / Record | 一条具体数据 |
| 列 | Column / Field | 数据中的一个项目 |
| 主键 | Primary Key | 唯一识别一行数据的字段 |

员工表示例：

| id | name | department | email |
| --- | --- | --- | --- |
| 1 | Tanaka | Sales | tanaka@example.com |
| 2 | Suzuki | Development | suzuki@example.com |

这里：

- `employees` 是表。
- `id`、`name`、`department`、`email` 是列。
- `Tanaka` 这一整行是一条记录。
- `id` 通常作为主键。

## 三、SQL 是什么

SQL 是 Structured Query Language 的缩写，中文叫结构化查询语言。

SQL 用于操作关系型数据库。

常见用途：

- 创建数据库和表
- 新增数据
- 修改数据
- 删除数据
- 查询数据
- 创建索引
- 控制权限

## 四、SQL 分类

| 分类 | 全称 | 作用 | 常见命令 |
| --- | --- | --- | --- |
| DDL | Data Definition Language | 定义表结构 | `CREATE`、`ALTER`、`DROP`、`TRUNCATE` |
| DML | Data Manipulation Language | 操作数据 | `INSERT`、`UPDATE`、`DELETE` |
| DQL | Data Query Language | 查询数据 | `SELECT` |
| DCL | Data Control Language | 控制权限 | `GRANT`、`REVOKE` |
| TCL | Transaction Control Language | 控制事务 | `COMMIT`、`ROLLBACK` |

本课程先讲 DDL，再讲 DML，最后详细讲查询。

## 五、四种常见数据库

| 数据库 | 类型 | 常见场景 |
| --- | --- | --- |
| MySQL | 开源关系型数据库 | Web 系统、中小型业务系统 |
| Oracle | 商业关系型数据库 | 金融、电信、保险、核心业务系统 |
| PostgreSQL | 开源关系型数据库 | 企业系统、数据分析、地理信息系统 |
| SQL Server | 微软关系型数据库 | Windows 系统、企业内部系统、.NET 项目 |

四种数据库都支持 SQL，但具体语法存在差异。

## 六、本课程统一样例表

后续章节主要使用三张表：

| 表名 | 作用 |
| --- | --- |
| `departments` | 部门表 |
| `employees` | 员工表 |
| `orders` | 订单表 |

基本关系：

```text
departments 1 --- N employees
employees   1 --- N orders
```

也就是说：

- 一个部门可以有多个员工。
- 一个员工可以有多个订单。

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 把数据库和表混为一谈 | 概念层级不清楚 | 数据库中包含多张表 |
| 认为所有数据库 SQL 完全一样 | 不同产品有方言差异 | 先理解通用概念，再看产品差异 |
| 一开始只背语法 | 不理解数据结构 | 先看表、行、列和业务数据 |

## 八、本章练习

请完成：

1. 说明数据库为什么能解决数据持久化问题。
2. 说明表、行、列、主键分别是什么意思。
3. 说出 DDL、DML、DQL 分别用于什么操作。
4. 对比 MySQL、Oracle、PostgreSQL、SQL Server 的常见使用场景。

## 九、本章总结

- 数据库用于长期保存业务数据。
- 关系型数据库用表、行、列保存结构化数据。
- SQL 是操作关系型数据库的语言。
- 四种数据库都支持 SQL，但存在语法差异。
