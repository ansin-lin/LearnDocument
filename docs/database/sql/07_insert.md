# 第7章 INSERT 新增数据

> 本章目标：掌握向表中插入数据的基本写法，理解单行插入、多行插入、默认值和基于查询插入的差异。

## 一、INSERT 是什么

`INSERT` 用于向表中新增数据。

基本语法：

```sql
INSERT INTO 表名 (列1, 列2, 列3)
VALUES (值1, 值2, 值3);
```

建议明确写出列名，不要依赖表字段顺序。

## 二、指定列名插入和不指定列名插入

`INSERT` 有两种常见写法。

### 2.1 指定列名插入

语法：

```sql
INSERT INTO 表名 (列1, 列2, 列3)
VALUES (值1, 值2, 值3);
```

示例：

```sql
INSERT INTO employees (id, name, department_id)
VALUES (1, 'Tanaka', 10);
```

这种写法明确说明每个值要插入到哪一列。

优点：

- 可读性高。
- 表字段顺序变化时影响较小。
- 可以省略允许为空或有默认值的列。

实际项目中推荐使用这种写法。

### 2.2 不指定列名插入

语法：

```sql
INSERT INTO 表名
VALUES (值1, 值2, 值3);
```

示例：

```sql
INSERT INTO employees
VALUES (1, 'Tanaka', 10, 'tanaka@example.com', '2024-04-01', 300000);
```

这种写法要求值的数量和顺序必须和表字段完全一致。

风险：

- 表字段顺序不清楚时容易写错。
- 表新增字段后 SQL 可能失败。
- 阅读 SQL 时不容易看出每个值对应哪一列。

学习时可以了解这种写法，但实际项目中不推荐长期使用。

## 三、示例表

本章使用员工表：

```sql
employees(id, name, department_id, email, hire_date, salary)
```

## 四、单行插入

MySQL / PostgreSQL / SQL Server：

```sql
INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (1, 'Tanaka', 10, 'tanaka@example.com', '2024-04-01', 300000);
```

Oracle：

```sql
INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (1, 'Tanaka', 10, 'tanaka@example.com', DATE '2024-04-01', 300000);
```

执行后数据：

| id | name | department_id | email | hire_date | salary |
| --- | --- | --- | --- | --- | --- |
| 1 | Tanaka | 10 | tanaka@example.com | 2024-04-01 | 300000 |

## 五、多行插入

MySQL / PostgreSQL / SQL Server：

```sql
INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES
    (2, 'Suzuki', 20, 'suzuki@example.com', '2024-05-01', 320000),
    (3, 'Sato', 20, 'sato@example.com', '2024-06-01', 280000);
```

Oracle 常用写法：

```sql
INSERT ALL
    INTO employees (id, name, department_id, email, hire_date, salary)
    VALUES (2, 'Suzuki', 20, 'suzuki@example.com', DATE '2024-05-01', 320000)
    INTO employees (id, name, department_id, email, hire_date, salary)
    VALUES (3, 'Sato', 20, 'sato@example.com', DATE '2024-06-01', 280000)
SELECT 1 FROM dual;
```

`dual` 是 Oracle 中常用的虚拟表。

## 六、插入 NULL 和默认值

如果字段允许为空，可以插入 `NULL`。

```sql
INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (4, 'Yamada', 30, NULL, '2024-07-01', 310000);
```

如果字段有默认值，可以省略该字段。

```sql
INSERT INTO employees (id, name, department_id, hire_date, salary)
VALUES (5, 'Kato', 30, '2024-08-01', 290000);
```

## 七、基于查询插入

用于备份或迁移数据。

```sql
INSERT INTO employees_backup (id, name, department_id, email, hire_date, salary)
SELECT id, name, department_id, email, hire_date, salary
FROM employees
WHERE department_id = 20;
```

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 列数量和值数量不一致 | `INSERT` 列和值没有对应 | 检查列和值个数 |
| 字符串没加引号 | 字符串必须用单引号 | 使用 `'Tanaka'` |
| 日期写法不兼容 | Oracle 日期写法不同 | Oracle 使用 `DATE 'YYYY-MM-DD'` |
| 违反主键唯一 | 插入了重复 ID | 确认主键值 |
| 不指定列名导致错位 | 值顺序必须和表字段一致 | 推荐指定列名插入 |

## 九、本章练习

请完成：

1. 新增 3 条员工数据。
2. 新增 1 条邮箱为 `NULL` 的员工数据。
3. 分别写出指定列名插入和不指定列名插入。
4. 说明为什么实际项目中推荐指定列名插入。
5. 写出四种数据库多行插入的差异。
6. 使用 `INSERT INTO ... SELECT` 备份某个部门员工。

## 十、本章总结

- `INSERT` 用于新增数据。
- 建议明确写出列名。
- 不指定列名插入要求值的顺序和表字段顺序完全一致。
- 多行插入在 Oracle 中写法不同。
- 写入日期时要注意数据库差异。
