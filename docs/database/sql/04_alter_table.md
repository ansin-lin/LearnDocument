# 第4章 ALTER、DROP 与 TRUNCATE

> 本章目标：掌握修改表结构、删除表和清空表的基本语法，理解结构变更的风险。

## 一、ALTER TABLE 是什么

`ALTER TABLE` 用于修改已有表结构。

常见操作：

- 添加字段
- 修改字段类型
- 修改字段名
- 删除字段
- 添加或删除约束

## 二、添加字段

需求：给员工表增加手机号字段 `phone`。

四种数据库添加字段的写法不完全一样。

| 数据库 | 语法 |
| --- | --- |
| MySQL | `ALTER TABLE 表名 ADD [COLUMN] 列名 数据类型;` |
| Oracle | `ALTER TABLE 表名 ADD (列名 数据类型);` |
| PostgreSQL | `ALTER TABLE 表名 ADD COLUMN 列名 数据类型;` |
| SQL Server | `ALTER TABLE 表名 ADD 列名 数据类型;` |

MySQL：

```sql
ALTER TABLE employees
ADD phone VARCHAR(20);
```

Oracle：

```sql
ALTER TABLE employees
ADD (phone VARCHAR2(20));
```

PostgreSQL：

```sql
ALTER TABLE employees
ADD COLUMN phone VARCHAR(20);
```

SQL Server：

```sql
ALTER TABLE employees
ADD phone VARCHAR(20);
```

## 三、修改字段类型

需求：把邮箱长度改为 300。

| 数据库 | 写法 |
| --- | --- |
| MySQL | `ALTER TABLE 表名 MODIFY [COLUMN] 列名 新数据类型;` |
| Oracle | `ALTER TABLE 表名 MODIFY (列名 新数据类型);` |
| PostgreSQL | `ALTER TABLE 表名 ALTER COLUMN 列名 TYPE 新数据类型;` |
| SQL Server | `ALTER TABLE 表名 ALTER COLUMN 列名 新数据类型;` |

MySQL：

```sql
ALTER TABLE employees
MODIFY [COLUMN] email VARCHAR(300);
```

Oracle：

```sql
ALTER TABLE employees
MODIFY (email VARCHAR2(300));
```

PostgreSQL：

```sql
ALTER TABLE employees
ALTER COLUMN email TYPE VARCHAR(300);
```

SQL Server：

```sql
ALTER TABLE employees
ALTER COLUMN email VARCHAR(300);
```

## 四、修改字段名

需求：把 `phone` 改为 `phone_number`。

| 数据库 | 写法 |
| --- | --- |
| MySQL | `ALTER TABLE 表名 RENAME COLUMN 旧列名 TO 新列名;` |
| Oracle | `ALTER TABLE 表名 RENAME COLUMN 旧列名 TO 新列名;` |
| PostgreSQL | `ALTER TABLE 表名 RENAME COLUMN 旧列名 TO 新列名;` |
| SQL Server | `EXEC sp_rename '表名.旧列名', '新列名', 'COLUMN';` |

MySQL：

```sql
ALTER TABLE employees
RENAME COLUMN phone TO phone_number;
```

Oracle：

```sql
ALTER TABLE employees
RENAME COLUMN phone TO phone_number;
```

PostgreSQL：

```sql
ALTER TABLE employees
RENAME COLUMN phone TO phone_number;
```

SQL Server：

```sql
EXEC sp_rename 'employees.phone', 'phone_number', 'COLUMN';
```

## 五、删除字段

```sql
ALTER TABLE 表名
DROP COLUMN 列名;
```

四种数据库都支持类似写法。

员工表示例：

```sql
ALTER TABLE employees
DROP COLUMN phone_number;
```

删除字段会删除该列保存的所有数据，执行前必须确认影响范围。

## 六、DROP TABLE

`DROP TABLE` 用于删除整张表。

```sql
DROP TABLE 表名;
```

执行后表结构和数据都会消失。

员工表示例：

```sql
DROP TABLE employees;
```

## 七、TRUNCATE TABLE

`TRUNCATE TABLE` 用于清空整张表的数据，但保留表结构。

```sql
TRUNCATE TABLE 表名;
```

员工表示例：

```sql
TRUNCATE TABLE employees;
```

对比：

| 命令 | 删除数据 | 删除表结构 |
| --- | --- | --- |
| `DELETE FROM employees` | 是 | 否 |
| `TRUNCATE TABLE employees` | 是 | 否 |
| `DROP TABLE employees` | 是 | 是 |

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 直接删除字段 | 未确认字段是否仍被程序使用 | 先做影响调查 |
| 混用四种数据库语法 | 各数据库 ALTER 写法不同 | 按数据库选择语法 |
| 把 TRUNCATE 当成普通 DELETE | TRUNCATE 风险更高 | 执行前备份或确认环境 |

## 九、本章练习

请完成：

1. 给 `employees` 表添加 `phone` 字段。
2. 修改 `email` 字段长度。
3. 写出四种数据库修改字段类型的语法差异。
4. 说明 `DELETE`、`TRUNCATE`、`DROP` 的区别。

## 十、本章总结

- `ALTER TABLE` 修改表结构。
- `DROP TABLE` 删除表。
- `TRUNCATE TABLE` 清空表数据。
- 结构变更前必须确认影响范围。
