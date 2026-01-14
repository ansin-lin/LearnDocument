# PL/SQL 存储过程与函数

> 本章系统讲解 **PL/SQL 中的存储过程（PROCEDURE）与函数（FUNCTION）**。这是数据库端封装业务逻辑的核心手段，在真实项目（金融、账务、请求书系统）以及面试中都属于**必考重点**。

---

## 1. 为什么要使用存储过程和函数

在实际项目中，将业务逻辑写在存储过程或函数中，有以下优势：

* 提高代码复用性，避免重复 SQL
* 统一业务规则，保证数据一致性
* 减少应用与数据库之间的网络交互
* 提升安全性（只授予执行权限）

---

## 2. 存储过程（PROCEDURE）

### 2.1 存储过程的特点

* 不直接返回值
* 通过 **OUT / IN OUT 参数** 返回结果
* 适合执行 **业务动作**（新增、更新、批处理、对账等）

---

### 2.2 存储过程的基本语法

```sql
create or replace procedure procedure_name (
  p_param1 in datatype,
  p_param2 out datatype,
  p_param3 in out datatype
) is
begin
  -- 业务处理逻辑
end;
/
```

---

### 2.3 存储过程示例

```sql
create or replace procedure p_calc_bonus (
  p_salary in  number,
  p_bonus  out number
) is
begin
  p_bonus := p_salary * 0.1;
end;
/
```

调用示例：

```sql
declare
  v_bonus number;
begin
  p_calc_bonus(5000, v_bonus);
  dbms_output.put_line('Bonus=' || v_bonus);
end;
/
```

---

## 3. 参数模式（IN / OUT / IN OUT）

| 参数模式 | 说明 |
| ------ | ---------- |
| IN | 只读参数，默认模式 |
| OUT | 输出参数，调用前无值 |
| IN OUT | 输入输出参数 |

> 📌 项目建议：
>
> * 能用 `IN` 就不要用 `IN OUT`
> * 输出结果较多时，可考虑使用记录或对象类型

---

## 4. 存储函数（FUNCTION）

### 4.1 函数的特点

* **必须返回一个值**
* 可在 **SQL 语句中调用**（前提：无副作用）
* 适合做 **计算、判断、转换类逻辑**

---

### 4.2 存储函数的基本语法

```sql
create or replace function function_name (
  p_param in datatype
) return datatype is
begin
  return value;
end;
/
```

---

### 4.3 函数示例

```sql
create or replace function f_calc_tax (
  p_amount number
) return number is
begin
  return p_amount * 0.05;
end;
/
```

调用示例（PL/SQL）：

```sql
declare
  v_tax number;
begin
  v_tax := f_calc_tax(10000);
  dbms_output.put_line('Tax=' || v_tax);
end;
/
```

调用示例（SQL）：

```sql
select empno, sal, f_calc_tax(sal) as tax
from emp;
```

---

## 5. 存储过程 vs 存储函数

| 项目 | 存储过程 | 存储函数 |
| --------- | -------- | --------- |
| 是否返回值 | 否（用 OUT） | 是（RETURN） |
| 是否可用于 SQL | 否 | 是 |
| 使用场景 | 执行业务动作 | 计算 / 判断 |
| 副作用 | 允许 | 不推荐 |

---

## 6. 函数在 SQL 中使用的注意点（面试高频）

* 函数 **不能执行 DML（INSERT/UPDATE/DELETE）**
* 函数不能 COMMIT / ROLLBACK
* 函数应是 **确定性（无副作用）**

> ⚠️ 否则会导致 SQL 报错或性能问题

---

## 7. 异常处理与过程 / 函数

存储过程和函数中可以使用完整的异常处理逻辑。

```sql
create or replace procedure p_safe_divide (
  p_a in number,
  p_b in number,
  p_result out number
) is
begin
  p_result := p_a / p_b;
exception
  when zero_divide then
  raise_application_error(-20001, 'Divisor cannot be zero');
end;
/
```

---

## 8. 权限与调用（项目实务）

```sql
grant execute on p_calc_bonus to app_user;
```

> 📌 实际项目中：
>
> * 应用账号通常 **只有 EXECUTE 权限**
> * 不直接授予表的 DML 权限

---

## 9. 项目实践建议

* 核心业务逻辑放在 **PACKAGE + PROCEDURE** 中
* 单一返回值逻辑使用 FUNCTION
* 对外接口固定参数顺序与类型
* 所有过程/函数必须有异常处理

---

## 10. 本章小结（面试版）

* 存储过程用于执行业务动作
* 存储函数必须返回值，可在 SQL 中调用
* IN / OUT / IN OUT 参数要合理使用
* 函数应避免副作用
* 项目中通常结合 PACKAGE 使用
