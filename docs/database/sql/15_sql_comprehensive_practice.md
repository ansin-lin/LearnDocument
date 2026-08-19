# 第15章 SQL 综合练习

> 本章目标：使用员工、部门、订单三张表，综合练习 DDL、DML、查询语句和视图。

## 一、练习目标

完成本章后，应能够：

- 创建基础业务表。
- 插入练习数据。
- 修改和删除指定数据。
- 编写基础查询、多表查询、分组统计和窗口函数查询。
- 根据数据库类型调整语法差异。
- 创建并查询用于报表的视图。

## 二、业务背景

本练习模拟一个简单的员工订单管理数据。

表关系：

```text
departments 1 --- N employees
employees   1 --- N orders
```

## 三、建表任务

请创建：

| 表 | 说明 |
| --- | --- |
| `departments` | 部门表 |
| `employees` | 员工表 |
| `orders` | 订单表 |

字段建议：

| 表 | 字段 |
| --- | --- |
| `departments` | `id`、`name` |
| `employees` | `id`、`name`、`department_id`、`email`、`hire_date`、`salary` |
| `orders` | `id`、`employee_id`、`order_no`、`amount`、`order_date` |

## 四、基础练习

请按照下面的指定数据完成 DML 练习。

执行顺序：

1. 新增 2 个部门。
2. 新增 5 名员工。
3. 新增 8 条订单。
4. 修改一名员工的邮箱。
5. 删除一条指定订单。

### 4.1 部门数据

请向 `departments` 表新增下面 2 个部门。

| id | name |
| --- | --- |
| 10 | 开发部 |
| 20 | 营业部 |

参考 SQL：

```sql
INSERT INTO departments (id, name)
VALUES (10, '开发部');

INSERT INTO departments (id, name)
VALUES (20, '营业部');
```

### 4.2 员工数据

请向 `employees` 表新增下面 5 名员工。

| id | name | department_id | email | hire_date | salary |
| --- | --- | --- | --- | --- | --- |
| 1001 | 山田太郎 | 10 | yamada@example.com | 2022-04-01 | 320000 |
| 1002 | 佐藤花子 | 10 | sato@example.com | 2021-10-15 | 380000 |
| 1003 | 鈴木一郎 | 10 | suzuki@example.com | 2023-01-10 | 280000 |
| 1004 | 高橋美咲 | 20 | takahashi@example.com | 2020-07-01 | 410000 |
| 1005 | 田中健 | 20 | tanaka@example.com | 2024-04-01 | 260000 |

参考 SQL：

```sql
INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (1001, '山田太郎', 10, 'yamada@example.com', '2022-04-01', 320000);

INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (1002, '佐藤花子', 10, 'sato@example.com', '2021-10-15', 380000);

INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (1003, '鈴木一郎', 10, 'suzuki@example.com', '2023-01-10', 280000);

INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (1004, '高橋美咲', 20, 'takahashi@example.com', '2020-07-01', 410000);

INSERT INTO employees (id, name, department_id, email, hire_date, salary)
VALUES (1005, '田中健', 20, 'tanaka@example.com', '2024-04-01', 260000);
```

### 4.3 订单数据

请向 `orders` 表新增下面 8 条订单。

| id | employee_id | order_no | amount | order_date |
| --- | --- | --- | --- | --- |
| 5001 | 1001 | ORD-2026-0001 | 120000 | 2026-01-10 |
| 5002 | 1001 | ORD-2026-0002 | 85000 | 2026-01-18 |
| 5003 | 1002 | ORD-2026-0003 | 230000 | 2026-02-03 |
| 5004 | 1002 | ORD-2026-0004 | 175000 | 2026-02-20 |
| 5005 | 1003 | ORD-2026-0005 | 64000 | 2026-03-05 |
| 5006 | 1004 | ORD-2026-0006 | 310000 | 2026-03-12 |
| 5007 | 1004 | ORD-2026-0007 | 90000 | 2026-04-01 |
| 5008 | 1005 | ORD-2026-0008 | 45000 | 2026-04-15 |

参考 SQL：

```sql
INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5001, 1001, 'ORD-2026-0001', 120000, '2026-01-10');

INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5002, 1001, 'ORD-2026-0002', 85000, '2026-01-18');

INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5003, 1002, 'ORD-2026-0003', 230000, '2026-02-03');

INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5004, 1002, 'ORD-2026-0004', 175000, '2026-02-20');

INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5005, 1003, 'ORD-2026-0005', 64000, '2026-03-05');

INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5006, 1004, 'ORD-2026-0006', 310000, '2026-03-12');

INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5007, 1004, 'ORD-2026-0007', 90000, '2026-04-01');

INSERT INTO orders (id, employee_id, order_no, amount, order_date)
VALUES (5008, 1005, 'ORD-2026-0008', 45000, '2026-04-15');
```

### 4.4 修改指定员工邮箱

请将员工 `1003` 的邮箱从 `suzuki@example.com` 修改为 `suzuki.ichiro@example.com`。

参考 SQL：

```sql
UPDATE employees
SET email = 'suzuki.ichiro@example.com'
WHERE id = 1003;
```

执行后可以用下面的 SQL 确认结果。

```sql
SELECT id,
       name,
       email
FROM employees
WHERE id = 1003;
```

预期结果：

| id | name | email |
| --- | --- | --- |
| 1003 | 鈴木一郎 | suzuki.ichiro@example.com |

### 4.5 删除指定订单

请删除订单编号为 `ORD-2026-0008` 的订单。

参考 SQL：

```sql
DELETE FROM orders
WHERE order_no = 'ORD-2026-0008';
```

执行后可以用下面的 SQL 确认结果。

```sql
SELECT id,
       employee_id,
       order_no,
       amount,
       order_date
FROM orders
WHERE order_no = 'ORD-2026-0008';
```

预期结果：查询不到任何数据。

## 五、查询练习

请完成：

1. 查询所有员工的姓名和邮箱。
2. 查询工资大于等于 300000 的员工。
3. 按工资从高到低查询员工。
4. 查询员工姓名和部门名称。
5. 按部门统计员工数量。
6. 按员工统计订单总金额。
7. 查询订单总金额最高的前 3 名员工。
8. 使用窗口函数按部门给员工工资排名。
9. 创建一个按部门统计员工数量和订单金额的视图。

## 六、提交要求

请提交：

1. 建表 SQL。
2. 插入数据 SQL。
3. 修改和删除 SQL。
4. 查询 SQL。
5. 视图创建 SQL 和视图查询 SQL。
6. 每个查询的结果截图或结果表。

## 七、本章总结

- SQL 学习需要通过完整表结构和真实数据练习。
- DDL、DML、查询语句应该连起来使用。
- 视图可以把常用查询保存为稳定入口，适合报表和权限受限查询。
- 多数据库开发时，要先明确当前使用的数据库产品。
