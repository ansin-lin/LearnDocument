# 第12章 窗口函数与条件表达式

> 本章目标：掌握窗口函数、排名函数、累计统计、前后行比较、`CASE WHEN` 和空值处理，能够完成常见报表类查询。

## 一、为什么需要窗口函数

有些统计需求既要保留每一行明细，又要计算排名、累计值或前后行差异。

例如：

- 查询每个部门内员工工资排名。
- 查询每名员工工资和部门平均工资的差距。
- 查询每条订单和上一条订单金额的差值。
- 查询员工工资累计值。

这些需求用 `GROUP BY` 会把多行合并成一行，不适合保留明细。

窗口函数可以在不合并行的情况下进行统计。

## 二、GROUP BY 和窗口函数的区别

| 对比 | GROUP BY | 窗口函数 |
| --- | --- | --- |
| 是否合并行 | 会合并 | 不合并 |
| 是否保留明细 | 不保留全部明细 | 保留每一行 |
| 常见用途 | 汇总统计 | 排名、累计、前后比较、组内统计 |

示例：

`GROUP BY` 查询每个部门平均工资：

```sql
SELECT department_id,
       AVG(salary) AS average_salary
FROM employees
GROUP BY department_id;
```

窗口函数查询每名员工以及所在部门平均工资：

```sql
SELECT id,
       name,
       department_id,
       salary,
       AVG(salary) OVER (PARTITION BY department_id) AS department_average_salary
FROM employees;
```

## 三、示例数据

`employees`：

| id | name | department_id | salary | hire_date |
| --- | --- | --- | --- | --- |
| 1 | Tanaka | 10 | 300000 | 2024-04-01 |
| 2 | Suzuki | 20 | 360000 | 2024-05-01 |
| 3 | Sato | 20 | 360000 | 2024-06-01 |
| 4 | Yamada | 20 | 280000 | 2024-07-01 |
| 5 | Kato | 10 | 260000 | 2024-08-01 |

## 四、窗口函数基本语法

```sql
窗口函数() OVER (
    PARTITION BY 分组列
    ORDER BY 排序列
)
```

组成说明：

| 部分 | 作用 |
| --- | --- |
| `窗口函数()` | 要执行的计算，例如 `ROW_NUMBER()`、`SUM()` |
| `OVER` | 表示这是窗口函数 |
| `PARTITION BY` | 指定分组范围 |
| `ORDER BY` | 指定组内排序规则 |

`PARTITION BY` 可以不写。

不写时表示整张结果作为一个窗口。

## 五、PARTITION BY

`PARTITION BY` 表示按什么分组计算窗口函数。

需求：查询每名员工和所在部门平均工资。

```sql
SELECT id,
       name,
       department_id,
       salary,
       AVG(salary) OVER (PARTITION BY department_id) AS department_average_salary
FROM employees;
```

结果：

| id | name | department_id | salary | department_average_salary |
| --- | --- | --- | --- | --- |
| 1 | Tanaka | 10 | 300000 | 280000 |
| 5 | Kato | 10 | 260000 | 280000 |
| 2 | Suzuki | 20 | 360000 | 333333.33 |
| 3 | Sato | 20 | 360000 | 333333.33 |
| 4 | Yamada | 20 | 280000 | 333333.33 |

## 六、ORDER BY

窗口函数中的 `ORDER BY` 表示窗口内部按什么顺序计算。

需求：按部门内工资从高到低编号。

```sql
SELECT id,
       name,
       department_id,
       salary,
       ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) AS row_no
FROM employees;
```

## 七、排名函数

常见排名函数：

| 函数 | 作用 | 并列时 |
| --- | --- | --- |
| `ROW_NUMBER()` | 给每行连续编号 | 不考虑并列 |
| `RANK()` | 排名 | 并列后跳号 |
| `DENSE_RANK()` | 密集排名 | 并列后不跳号 |

示例：

```sql
SELECT id,
       name,
       department_id,
       salary,
       ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) AS row_no,
       RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) AS rank_no,
       DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) AS dense_rank_no
FROM employees;
```

部门 20 的结果示例：

| name | salary | row_no | rank_no | dense_rank_no |
| --- | --- | --- | --- | --- |
| Suzuki | 360000 | 1 | 1 | 1 |
| Sato | 360000 | 2 | 1 | 1 |
| Yamada | 280000 | 3 | 3 | 2 |

说明：

- `ROW_NUMBER()` 不管工资是否相同，直接编号 1、2、3。
- `RANK()` 两个 360000 都是第 1 名，下一个变成第 3 名。
- `DENSE_RANK()` 两个 360000 都是第 1 名，下一个是第 2 名。

## 八、累计求和

需求：按入职日期计算累计工资。

```sql
SELECT id,
       name,
       hire_date,
       salary,
       SUM(salary) OVER (ORDER BY hire_date) AS running_total
FROM employees;
```

结果示例：

| name | hire_date | salary | running_total |
| --- | --- | --- | --- |
| Tanaka | 2024-04-01 | 300000 | 300000 |
| Suzuki | 2024-05-01 | 360000 | 660000 |
| Sato | 2024-06-01 | 360000 | 1020000 |

## 九、LAG 与 LEAD

`LAG` 用于取得上一行的值。

`LEAD` 用于取得下一行的值。

语法：

```sql
LAG(列名, 偏移量, 默认值) OVER (ORDER BY 排序列)
```

```sql
LEAD(列名, 偏移量, 默认值) OVER (ORDER BY 排序列)
```

需求：查询当前员工工资、上一名员工工资、下一名员工工资。

```sql
SELECT id,
       name,
       salary,
       LAG(salary, 1, 0) OVER (ORDER BY id) AS previous_salary,
       LEAD(salary, 1, 0) OVER (ORDER BY id) AS next_salary
FROM employees;
```

结果示例：

| id | name | salary | previous_salary | next_salary |
| --- | --- | --- | --- | --- |
| 1 | Tanaka | 300000 | 0 | 360000 |
| 2 | Suzuki | 360000 | 300000 | 360000 |
| 3 | Sato | 360000 | 360000 | 280000 |

参数说明：

| 参数 | 说明 |
| --- | --- |
| 第 1 个参数 | 要取值的列 |
| 第 2 个参数 | 向前或向后移动几行 |
| 第 3 个参数 | 没有上一行或下一行时使用的默认值 |

## 十、窗口范围 ROWS

窗口函数可以指定计算范围。

需求：计算当前行和前一行的工资合计。

```sql
SELECT id,
       name,
       salary,
       SUM(salary) OVER (
           ORDER BY id
           ROWS BETWEEN 1 PRECEDING AND CURRENT ROW
       ) AS recent_two_total
FROM employees;
```

含义：

- `1 PRECEDING` 表示前 1 行。
- `CURRENT ROW` 表示当前行。
- 合起来表示“前 1 行到当前行”。

窗口范围中常用的写法如下：

| 写法 | 含义 |
| --- | --- |
| `CURRENT ROW` | 当前行 |
| `1 PRECEDING` | 当前行前 1 行 |
| `2 PRECEDING` | 当前行前 2 行 |
| `1 FOLLOWING` | 当前行后 1 行 |
| `2 FOLLOWING` | 当前行后 2 行 |
| `UNBOUNDED PRECEDING` | 从分区第一行开始 |
| `UNBOUNDED FOLLOWING` | 一直到分区最后一行 |

## 十一、CASE WHEN

`CASE WHEN` 用于条件表达式。

语法：

```sql
CASE
    WHEN 条件1 THEN 结果1
    WHEN 条件2 THEN 结果2
    ELSE 默认结果
END
```

需求：根据工资分级。

```sql
SELECT id,
       name,
       salary,
       CASE
           WHEN salary >= 350000 THEN 'A'
           WHEN salary >= 300000 THEN 'B'
           ELSE 'C'
       END AS salary_rank
FROM employees;
```

结果：

| id | name | salary | salary_rank |
| --- | --- | --- | --- |
| 1 | Tanaka | 300000 | B |
| 2 | Suzuki | 360000 | A |
| 3 | Sato | 360000 | A |
| 4 | Yamada | 280000 | C |

`CASE WHEN` 从上到下判断，匹配到第一个满足条件的分支后就返回对应结果。

## 十二、条件聚合中的 CASE WHEN

`CASE WHEN` 经常和聚合函数一起使用。

需求：按部门统计高工资人数。

```sql
SELECT department_id,
       SUM(CASE WHEN salary >= 300000 THEN 1 ELSE 0 END) AS high_salary_count
FROM employees
GROUP BY department_id;
```

这个写法在统计报表中很常见。

## 十三、NULL 处理函数差异

| 数据库 | 常用写法 |
| --- | --- |
| MySQL | `IFNULL(email, '未设置')` 或 `COALESCE(email, '未设置')` |
| Oracle | `NVL(email, '未设置')` 或 `COALESCE(email, '未设置')` |
| PostgreSQL | `COALESCE(email, '未设置')` |
| SQL Server | `ISNULL(email, '未设置')` 或 `COALESCE(email, '未设置')` |

推荐优先理解 `COALESCE`，因为它更通用。

示例：

```sql
SELECT id,
       name,
       COALESCE(email, '未设置') AS email_text
FROM employees;
```

## 十四、四种数据库支持情况

| 功能 | MySQL | Oracle | PostgreSQL | SQL Server |
| --- | --- | --- | --- | --- |
| `ROW_NUMBER()` | 支持 | 支持 | 支持 | 支持 |
| `RANK()` | 支持 | 支持 | 支持 | 支持 |
| `DENSE_RANK()` | 支持 | 支持 | 支持 | 支持 |
| `LAG()` / `LEAD()` | 支持 | 支持 | 支持 | 支持 |
| `CASE WHEN` | 支持 | 支持 | 支持 | 支持 |
| `COALESCE()` | 支持 | 支持 | 支持 | 支持 |

MySQL 需要 8.0 以上才支持常用窗口函数。

## 十五、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 把窗口函数当 GROUP BY | 窗口函数不合并行 | 区分统计和逐行计算 |
| 排名没有 ORDER BY | 不知道按什么排名 | 窗口中写清排序字段 |
| 不理解并列排名 | `RANK` 和 `DENSE_RANK` 不同 | 根据业务选择 |
| 忘记 PARTITION BY | 结果变成全表范围 | 需要组内计算时写 `PARTITION BY` |
| CASE WHEN 顺序错误 | 先匹配了宽泛条件 | 从严格条件到宽泛条件排列 |

## 十六、本章练习

请完成：

1. 按部门对员工工资排名。
2. 对比 `ROW_NUMBER()`、`RANK()`、`DENSE_RANK()` 的结果。
3. 查询每名员工和所在部门平均工资。
4. 查询每名员工的上一条工资记录。
5. 使用 `CASE WHEN` 给工资分级。
6. 使用四种数据库的空值处理函数显示默认邮箱。

## 十七、本章总结

- 窗口函数适合排名、累计、前后行比较。
- `PARTITION BY` 决定窗口分组范围。
- `ORDER BY` 决定窗口内部计算顺序。
- `ROW_NUMBER`、`RANK`、`DENSE_RANK` 的并列处理不同。
- `CASE WHEN` 用于条件判断。
- 空值处理函数在四种数据库中存在差异。
