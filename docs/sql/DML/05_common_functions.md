# SQL 常用函数大全（跨数据库对照版）

---

## 一、字符串函数（String Functions）

| 功能 | MySQL | SQL Server | PostgreSQL | Oracle | 示例 |
|------|--------|-------------|-------------|---------|------|
| 获取字符串长度 | `LENGTH(str)` | `LEN(str)` | `LENGTH(str)` | `LENGTH(str)` | `SELECT LENGTH('abc');` |
| 转大写 | `UPPER(str)` | `UPPER(str)` | `UPPER(str)` | `UPPER(str)` | `SELECT UPPER('abc');` |
| 转小写 | `LOWER(str)` | `LOWER(str)` | `LOWER(str)` | `LOWER(str)` | `SELECT LOWER('ABC');` |
| 拼接字符串 | `CONCAT(a,b,...)` | `a + b` | `a || b` | `a || b` | `SELECT CONCAT('A','B');` |
| 取子串 | `SUBSTRING(str, start, len)` | `SUBSTRING(str, start, len)` | `SUBSTRING(str FROM start FOR len)` | `SUBSTR(str, start, len)` | `SELECT SUBSTRING('abcdef',2,3);` |
| 去空格 | `TRIM(str)` | `LTRIM()/RTRIM()` | `TRIM(str)` | `TRIM(str)` | `SELECT TRIM(' abc ');` |
| 替换字符串 | `REPLACE(str, old, new)` | `REPLACE(str, old, new)` | `REPLACE(str, old, new)` | `REPLACE(str, old, new)` | `SELECT REPLACE('abc','a','A');` |

---

## 二、数值函数（Numeric Functions）

| 功能 | MySQL | SQL Server | PostgreSQL | Oracle | 示例 |
|------|--------|-------------|-------------|---------|------|
| 绝对值 | `ABS(x)` | `ABS(x)` | `ABS(x)` | `ABS(x)` | `SELECT ABS(-5);` |
| 向上取整 | `CEIL(x)` / `CEILING(x)` | `CEILING(x)` | `CEIL(x)` | `CEIL(x)` | `SELECT CEIL(4.2);` |
| 向下取整 | `FLOOR(x)` | `FLOOR(x)` | `FLOOR(x)` | `FLOOR(x)` | `SELECT FLOOR(4.8);` |
| 四舍五入 | `ROUND(x, d)` | `ROUND(x, d)` | `ROUND(x, d)` | `ROUND(x, d)` | `SELECT ROUND(3.1415, 2);` |
| 取余 | `MOD(x, y)` | `x % y` | `MOD(x, y)` | `MOD(x, y)` | `SELECT MOD(10,3);` |
| 随机数 | `RAND()` | `RAND()` | `RANDOM()` | `DBMS_RANDOM.VALUE` | `SELECT RAND();` |
| 幂运算 | `POW(x, y)` | `POWER(x, y)` | `POWER(x, y)` | `POWER(x, y)` | `SELECT POWER(2,3);` |

---

## 三、日期与时间函数（Date/Time Functions）

| 功能 | MySQL | SQL Server | PostgreSQL | Oracle | 示例 |
|------|--------|-------------|-------------|---------|------|
| 当前日期 | `CURDATE()` | `GETDATE()` | `CURRENT_DATE` | `SYSDATE` | `SELECT CURDATE();` |
| 当前时间 | `CURTIME()` | `GETDATE()` | `CURRENT_TIME` | `SYSDATE` | `SELECT CURTIME();` |
| 当前日期时间 | `NOW()` | `GETDATE()` | `CURRENT_TIMESTAMP` | `SYSDATE` | `SELECT NOW();` |
| 提取年 | `YEAR(date)` | `YEAR(date)` | `EXTRACT(YEAR FROM date)` | `EXTRACT(YEAR FROM date)` | `SELECT YEAR(NOW());` |
| 提取月 | `MONTH(date)` | `MONTH(date)` | `EXTRACT(MONTH FROM date)` | `EXTRACT(MONTH FROM date)` | `SELECT MONTH(NOW());` |
| 提取日 | `DAY(date)` | `DAY(date)` | `EXTRACT(DAY FROM date)` | `EXTRACT(DAY FROM date)` | `SELECT DAY(NOW());` |
| 日期加减 | `DATE_ADD(date, INTERVAL n DAY)` | `DATEADD(DAY,n,date)` | `date + INTERVAL 'n day'` | `date + n` | `SELECT DATE_ADD(NOW(), INTERVAL 5 DAY);` |
| 日期差 | `DATEDIFF(d1, d2)` | `DATEDIFF(d1,d2)` | `AGE(d1, d2)` | `MONTHS_BETWEEN(d1, d2)` | `SELECT DATEDIFF('2025-10-01','2025-09-01');` |

---

## 四、聚合函数（Aggregate Functions）

| 函数 | 说明 | 示例 |
|------|------|------|
| `COUNT()` | 统计行数 | `SELECT COUNT(*) FROM table;` |
| `SUM()` | 求和 | `SELECT SUM(salary) FROM emp;` |
| `AVG()` | 平均值 | `SELECT AVG(score) FROM students;` |
| `MAX()` | 最大值 | `SELECT MAX(price) FROM goods;` |
| `MIN()` | 最小值 | `SELECT MIN(age) FROM users;` |

> 💡 所有数据库都支持以上函数，语法相同。

---

## 五、转换函数（Conversion Functions）

| 功能 | MySQL | SQL Server | PostgreSQL | Oracle | 示例 |
|------|--------|-------------|-------------|---------|------|
| 转为字符串 | `CAST(x AS CHAR)` | `CAST(x AS VARCHAR)` / `CONVERT(VARCHAR,x)` | `CAST(x AS TEXT)` | `TO_CHAR(x)` | `SELECT CAST(123 AS CHAR);` |
| 转为整数 | `CAST(x AS SIGNED)` | `CAST(x AS INT)` | `CAST(x AS INTEGER)` | `TO_NUMBER(x)` | `SELECT CAST('123' AS SIGNED);` |
| 转为日期 | `STR_TO_DATE(str,format)` | `CONVERT(DATETIME,str)` | `TO_DATE(str,format)` | `TO_DATE(str,format)` | `SELECT STR_TO_DATE('2025-01-01','%Y-%m-%d');` |

---

## 六、系统函数（System Functions）

| 功能 | MySQL | SQL Server | PostgreSQL | Oracle |
|------|--------|-------------|-------------|---------|
| 当前用户 | `USER()` | `SUSER_NAME()` | `CURRENT_USER` | `USER` |
| 当前数据库 | `DATABASE()` | `DB_NAME()` | `CURRENT_DATABASE()` | `ORA_DATABASE_NAME` |
| 版本号 | `VERSION()` | `@@VERSION` | `VERSION()` | `SELECT * FROM V$VERSION;` |

---

## 七、条件与空值函数

| 功能 | MySQL | SQL Server | PostgreSQL | Oracle | 示例 |
|------|--------|-------------|-------------|---------|------|
| 条件判断 | `IF(condition,a,b)` | `IIF(condition,a,b)` | `CASE WHEN ... THEN ... END` | `DECODE(expr,val1,res1,...)` | `SELECT IF(score>60,'Pass','Fail');` |
| 第一个非空 | `COALESCE(a,b,...)` | 同上 | 同上 | 同上 | `SELECT COALESCE(NULL, 'Default');` |
| 判断空值 | `IFNULL(a,b)` | `ISNULL(a,b)` | `COALESCE(a,b)` | `NVL(a,b)` | `SELECT IFNULL(NULL,'N/A');` |

---

## 八、数学扩展函数（补充）

| 功能 | MySQL / PostgreSQL | SQL Server | Oracle | 示例 |
|------|--------------------|-------------|---------|------|
| 平方根 | `SQRT(x)` | `SQRT(x)` | `SQRT(x)` | `SELECT SQRT(9);` |
| 指数 | `EXP(x)` | `EXP(x)` | `EXP(x)` | `SELECT EXP(1);` |
| 对数 | `LOG(x)` | `LOG(x)` | `LOG(x)` | `SELECT LOG(10);` |
| 正弦 | `SIN(x)` | `SIN(x)` | `SIN(x)` | `SELECT SIN(PI()/2);` |
| 余弦 | `COS(x)` | `COS(x)` | `COS(x)` | `SELECT COS(0);` |
| 反正切 | `ATAN(x)` | `ATAN(x)` | `ATAN(x)` | `SELECT ATAN(1);` |

---

## 九、汇总与建议

✅ **学习重点：**

1. 各类函数通用语法优先掌握（`SUM`, `COUNT`, `LENGTH`, `DATE_ADD` 等）。  
2. 熟悉不同数据库的差异（如字符串拼接、时间函数）。  
3. Oracle / PostgreSQL 使用 `||` 进行字符串拼接，SQL Server 使用 `+`。  
4. MySQL 的函数命名最接近 SQL 标准，推荐作为学习基准。

---
