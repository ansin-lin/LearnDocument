# PL/SQL 复合数据类型（数组、记录、集合）

> 本章系统讲解 **PL/SQL 中可以存储大量数据的复合数据类型**，包括 **记录（RECORD）**、**集合（Collection）**（数组、嵌套表、关联数组）。
> 这些类型是实现 **批量处理、复杂结构封装、与游标/BULK COLLECT 配合** 的基础，在项目与面试中都非常重要。

---

## 1. 为什么需要复合数据类型

在实际项目中，单一变量往往无法满足需求：

* 一行数据包含多个字段（如一条订单）
* 一次需要处理多行数据（批处理、对账、报表）
* 希望将相关数据作为一个整体传递

PL/SQL 提供了 **复合数据类型** 来解决这些问题。

---

## 2. 复合数据类型分类

PL/SQL 中常见的复合数据类型如下：

| 类型 | 说明 |
| ---------- | ------------ |
| RECORD | 类似一行记录（多字段） |
| Collection | 类似数组/列表（多元素） |

Collection 又可细分为：

| 集合类型 | 说明 |
| ----------------------- | -------- |
| 关联数组（Associative Array） | 内存数组，最常用 |
| 嵌套表（Nested Table） | 可存入表列 |
| 可变数组（VARRAY） | 有长度上限 |

---

## 3. 记录类型（RECORD）

记录用于将 **多个不同类型的字段组合在一起**，类似一行表数据。

### 3.1 使用 %ROWTYPE 定义记录（最常用）

```sql
declare
  r_emp emp%rowtype;
begin
  select * into r_emp from emp where empno = 7369;
  dbms_output.put_line(r_emp.ename || ':' || r_emp.sal);
end;
/
```

> 📌 优点：
>
> * 自动与表结构同步
> * 维护成本最低

---

### 3.2 自定义 RECORD 类型

```sql
declare
  type t_emp is record (
  empno emp.empno%type,
  ename emp.ename%type,
  sal emp.sal%type
  );
  r_emp t_emp;
begin
  r_emp.empno := 1001;
  r_emp.ename := 'SCOTT';
  r_emp.sal := 3000;
end;
/
```

---

## 4. 集合类型（Collection）概述

集合用于存储 **多条同类型的数据**，是 PL/SQL 中“数组”的实现。

---

## 5. 关联数组（Associative Array，Index-By Table）

### 5.1 特点

* 存储在内存中
* 下标可以是 **整数或字符串**
* 不需要初始化大小
* **最常用、最推荐**

### 5.2 定义与使用

```sql
declare
  type t_num_tab is table of number index by pls_integer;
  v_tab t_num_tab;
begin
  v_tab(1) := 100;
  v_tab(2) := 200;

  dbms_output.put_line(v_tab(1));
end;
/
```

---

## 6. 嵌套表（Nested Table）

### 6.1 特点

* 可以存储在数据库表中
* 下标从 1 开始
* 可动态扩展

### 6.2 定义与使用

```sql
declare
  type t_num_nt is table of number;
  v_nt t_num_nt := t_num_nt();
begin
  v_nt.extend;
  v_nt(1) := 10;

  v_nt.extend(2);
  v_nt(2) := 20;
  v_nt(3) := 30;
end;
/
```

---

## 7. 可变数组（VARRAY）

### 7.1 特点

* 有最大长度限制
* 元素连续存储
* 适合“小而固定规模的数据”

### 7.2 定义与使用

```sql
declare
  type t_num_var is varray(5) of number;
  v_arr t_num_var := t_num_var(1,2,3);
begin
  dbms_output.put_line(v_arr.count);
end;
/
```

---

## 8. RECORD + Collection 组合使用

在项目中，常见结构是：

> **一条记录 + 多条记录集合**

```sql
declare
  type t_emp is record (
  empno emp.empno%type,
  ename emp.ename%type,
  sal emp.sal%type
  );
  type t_emp_tab is table of t_emp index by pls_integer;
  v_emps t_emp_tab;
begin
  v_emps(1).empno := 1001;
  v_emps(1).ename := 'A';
  v_emps(1).sal := 3000;
end;
/
```

---

## 9. 与游标 / BULK COLLECT 的关系（重要）

复合数据类型通常与 **BULK COLLECT** 搭配，实现高性能批量处理：

```sql
select empno, ename, sal
bulk collect into v_emps
from emp;
```

> 📌 这部分将在 **性能优化章节** 详细讲解。

---

## 10. 项目实践建议

* 单行数据：RECORD / %ROWTYPE
* 多行数据：Collection
* 批处理优先使用 Collection + BULK COLLECT
* 不要用游标一行一行处理大数据量

---

## 11. 本章小结

* RECORD 用于封装一行数据
* Collection 用于存储多行数据
* 关联数组最常用
* 嵌套表可存入数据库
* VARRAY 有长度限制
* 批量处理离不开复合数据类型
