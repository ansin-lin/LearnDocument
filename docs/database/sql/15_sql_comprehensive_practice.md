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

请完成：

1. 新增 2 个部门。
2. 新增 5 名员工。
3. 新增 8 条订单。
4. 修改一名员工的邮箱。
5. 删除一条指定订单。

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

## 六、参考查询

查询员工和部门：

```sql
SELECT e.id,
       e.name AS employee_name,
       d.name AS department_name
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

按员工统计订单总金额：

```sql
SELECT e.id,
       e.name,
       SUM(o.amount) AS total_amount
FROM employees e
INNER JOIN orders o
    ON e.id = o.employee_id
GROUP BY e.id, e.name;
```

按部门工资排名：

```sql
SELECT id,
       name,
       department_id,
       salary,
       ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) AS salary_rank
FROM employees;
```

创建部门统计视图：

```sql
CREATE VIEW v_department_order_summary AS
SELECT d.id AS department_id,
       d.name AS department_name,
       COUNT(DISTINCT e.id) AS employee_count,
       COALESCE(SUM(o.amount), 0) AS total_order_amount
FROM departments d
LEFT JOIN employees e
    ON d.id = e.department_id
LEFT JOIN orders o
    ON e.id = o.employee_id
GROUP BY d.id,
         d.name;
```

## 七、提交要求

请提交：

1. 建表 SQL。
2. 插入数据 SQL。
3. 修改和删除 SQL。
4. 查询 SQL。
5. 视图创建 SQL 和视图查询 SQL。
6. 每个查询的结果截图或结果表。

## 八、本章总结

- SQL 学习需要通过完整表结构和真实数据练习。
- DDL、DML、查询语句应该连起来使用。
- 视图可以把常用查询保存为稳定入口，适合报表和权限受限查询。
- 多数据库开发时，要先明确当前使用的数据库产品。
