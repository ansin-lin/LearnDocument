# PL/SQL 运算符

> 本章系统总结 **PL/SQL 中常用的运算符及其优先级规则**。运算符是编写条件判断、计算逻辑和字符串处理的核心内容，在实际项目和面试中都属于**高频基础知识点**。

---

## 1. 运算符概述

运算符用于对一个或多个操作数执行特定操作。在 PL/SQL 中，常见的运算符主要包括：

* 算术运算符
* 关系运算符
* 比较运算符
* 逻辑运算符
* 字符串运算符

---

## 2. 算术运算符

算术运算符用于执行数值计算。

| 运算符 | 说明 | 示例 |
| --- | ------- | --------- |
| + | 加法 | a + b |
| - | 减法 / 取负 | a - b, -a |
| * | 乘法 | a * b |
| / | 除法 | a / b |
| ** | 幂运算 | a ** b |

### 示例

```sql
declare
  a number := 10;
  b number := 3;
begin
  dbms_output.put_line(a + b); -- 13
  dbms_output.put_line(a / b); -- 3.333...
  dbms_output.put_line(a ** b);  -- 1000
end;
/
```

---

## 3. 关系运算符

关系运算符用于比较两个值的关系，返回结果为 **BOOLEAN**。

| 运算符 | 说明 |
| --- | ---- |
| = | 等于 |
| != | 不等于 |
| <> | 不等于 |
| ^= | 不等于 |
| ~= | 不等于 |
| > | 大于 |
| < | 小于 |
| >= | 大于等于 |
| <= | 小于等于 |

### 示例

```sql
declare
  a number := 10;
  b number := 20;
begin
  if a < b then
 dbms_output.put_line('a < b');
  end if;
end;
/
```

---

## 4. 比较运算符（特殊）

PL/SQL 中还提供了一些用于范围和模式匹配的比较运算符（通常用于 SQL，但在 PL/SQL 条件中同样常见）。

| 运算符 | 说明 | 示例 |
| ----------- | -------- | ------------------ |
| BETWEEN | 在指定范围内 | x BETWEEN 1 AND 10 |
| IN | 在指定集合中 | x IN (1,2,3) |
| LIKE | 模式匹配 | name LIKE 'A%' |
| IS NULL | 是否为 NULL | v IS NULL |
| IS NOT NULL | 是否非 NULL | v IS NOT NULL |

> ⚠️ 注意：
>
> * **NULL 不能用 = 或 != 比较**，必须使用 `IS NULL` / `IS NOT NULL`

---

## 5. 逻辑运算符

逻辑运算符用于组合多个条件表达式。

| 运算符 | 说明 |
| --- | --------- |
| AND | 逻辑与（同时成立） |
| OR | 逻辑或（任一成立） |
| NOT | 逻辑非 |

### 示例

```sql
declare
  age number := 25;
begin
  if age >= 18 and age < 60 then
 dbms_output.put_line('Working age');
  end if;
end;
/
```

---

## 6. 字符串运算符

PL/SQL 中主要的字符串运算符是 **连接运算符**。

| 运算符 | 说明 | 示例 |
| --- | -- | -- | ----- | ------- | - | -------- |
| \|\| | 字符串连接 | 'Hello' \|\| ' World' |

### 示例

```sql
declare
  first_name varchar2(20) := 'Oracle';
  last_name  varchar2(20) := 'PLSQL';
begin
  dbms_output.put_line(first_name || ' ' || last_name);
end;
/
```

---

## 7. 运算符优先级

当一个表达式中包含多个运算符时，PL/SQL 会按照 **运算符优先级** 依次计算。

### 常见优先级顺序（由高到低）

| 优先级 | 运算符 |
| --- | ------------------- |
| 1 | ** |
| 2 | + (正), - (负) |
| 3 | *, / |
| 4 | +, - |
| 5 | =, !=, <, >, <=, >= |
| 6 | NOT |
| 7 | AND |
| 8 | OR |

> 📌 建议：
>
> * 在复杂表达式中 **使用括号明确计算顺序**，可读性和安全性更高

### 示例

```sql
-- 不推荐（容易误解）
if a + b * c > 100 then
  ...
end if;

-- 推荐
if (a + (b * c)) > 100 then
  ...
end if;
```

---

## 8. 本章小结

* 算术运算符用于数值计算（+ - * / **）
* 关系运算符返回 BOOLEAN，用于条件判断
* NULL 判断必须使用 IS NULL / IS NOT NULL
* 逻辑运算符用于组合条件（AND / OR / NOT）
* 字符串使用 `||` 进行连接
* 理解运算符优先级，复杂表达式应使用括号

