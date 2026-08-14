# 第8章 UPDATE 与 DELETE

> 本章目标：掌握修改和删除数据的基本写法，理解 `WHERE` 条件、影响行数和安全执行的重要性。

## 一、UPDATE 是什么

`UPDATE` 用于修改已有数据。

基本语法：

```sql
UPDATE 表名
SET 列1 = 新值1,
    列2 = 新值2
WHERE 条件;
```

`WHERE` 用于限制修改范围。

## 二、UPDATE 示例

修改员工 ID 为 1 的部门和邮箱：

```sql
UPDATE employees
SET department_id = 20,
    email = 'tanaka.dev@example.com'
WHERE id = 1;
```

执行前：

| id | name | department_id | email |
| --- | --- | --- | --- |
| 1 | Tanaka | 10 | tanaka@example.com |

执行后：

| id | name | department_id | email |
| --- | --- | --- | --- |
| 1 | Tanaka | 20 | tanaka.dev@example.com |

## 三、四种数据库 UPDATE 差异

基础 `UPDATE` 写法在四种数据库中基本一致。

| 数据库 | 写法 |
| --- | --- |
| MySQL | `UPDATE employees SET email = 'a@example.com' WHERE id = 1;` |
| Oracle | `UPDATE employees SET email = 'a@example.com' WHERE id = 1;` |
| PostgreSQL | `UPDATE employees SET email = 'a@example.com' WHERE id = 1;` |
| SQL Server | `UPDATE employees SET email = 'a@example.com' WHERE id = 1;` |

## 四、DELETE 是什么

`DELETE` 用于删除表中的数据。

基本语法：

```sql
DELETE FROM 表名
WHERE 条件;
```

## 五、DELETE 示例

删除员工 ID 为 3 的数据：

```sql
DELETE FROM employees
WHERE id = 3;
```

执行前：

| id | name |
| --- | --- |
| 3 | Sato |

执行后：

| id | name |
| --- | --- |

## 六、DELETE、TRUNCATE、DROP 区别

| 命令 | 作用 | 是否保留表结构 |
| --- | --- | --- |
| `DELETE` | 删除数据 | 保留 |
| `TRUNCATE` | 清空整张表数据 | 保留 |
| `DROP` | 删除表结构和数据 | 不保留 |

四种数据库清空表写法：

```sql
TRUNCATE TABLE employees;
```

`TRUNCATE` 属于 DDL，通常不能像普通 `DELETE` 那样逐行控制。

## 七、安全写法

执行 `UPDATE` 或 `DELETE` 前，先用 `SELECT` 确认影响范围。

```sql
SELECT id, name, department_id
FROM employees
WHERE department_id = 20;
```

确认无误后再执行：

```sql
UPDATE employees
SET salary = salary + 10000
WHERE department_id = 20;
```

不要直接执行没有 `WHERE` 的写操作：

```sql
UPDATE employees
SET salary = 0;
```

上面 SQL 会修改整张表。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 忘记 `WHERE` | 修改或删除整张表 | 写操作前先写 `SELECT` 确认 |
| 条件写错 | 影响了错误数据 | 使用主键或明确条件 |
| 字符串使用双引号 | 不同数据库行为不同 | 字符串统一使用单引号 |
| 不确认影响行数 | 不知道实际改了几行 | 执行后查看 affected rows |

## 九、本章练习

请完成：

1. 修改一名员工的邮箱。
2. 修改某个部门员工的工资。
3. 删除一条指定 ID 的员工数据。
4. 写出 `DELETE`、`TRUNCATE`、`DROP` 的区别。

## 十、本章总结

- `UPDATE` 用于修改数据。
- `DELETE` 用于删除数据。
- 写操作必须注意 `WHERE` 条件。
- 执行写操作前应先用 `SELECT` 确认范围。
