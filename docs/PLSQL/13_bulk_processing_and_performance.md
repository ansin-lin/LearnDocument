# PL/SQL 批量处理与性能优化

> 本章系统讲解 **PL/SQL 中的批量处理技术与性能优化手段**，重点包括 **BULK COLLECT、FORALL、LIMIT 分批处理**。
> 这是 PL/SQL 从“能跑”到“高性能”的分水岭，也是**金融 / 批处理系统与面试中的必考重点**。

---

## 1. 为什么需要批量处理

在传统写法中，最常见的问题是：

```text
SQL ↔ PL/SQL 上下文切换次数过多
```

例如：

* 游标一行一行 FETCH
* 循环中反复执行 INSERT / UPDATE

这会导致：

* CPU 消耗高
* 执行时间长
* 大数据量场景下性能急剧下降

**批量处理的目标只有一个：减少上下文切换次数。**

---

## 2. 游标逐行处理（性能对比基线）

```sql
for r in (select empno, sal from emp) loop
  update emp set sal = sal * 1.1 where empno = r.empno;
end loop;
```

问题：

* 每一行执行一次 UPDATE
* 大量 SQL/PLSQL 切换

---

## 3. BULK COLLECT（批量取数据）

`BULK COLLECT` 用于 **一次性将多行查询结果加载到集合中**。

---

### 3.1 基本用法

```sql
declare
  type t_empno_tab is table of emp.empno%type;
  v_empnos t_empno_tab;
begin
  select empno
    bulk collect into v_empnos
    from emp;

  dbms_output.put_line(v_empnos.count);
end;
/
```

---

### 3.2 BULK COLLECT + RECORD

```sql
declare
  type t_emp is record (
    empno emp.empno%type,
    sal   emp.sal%type
  );
  type t_emp_tab is table of t_emp;
  v_emps t_emp_tab;
begin
  select empno, sal
    bulk collect into v_emps
    from emp;
end;
/
```

---

## 4. FORALL（批量 DML）

`FORALL` 用于 **将集合中的数据一次性执行 DML**。

---

### 4.1 基本语法

```sql
forall i in lower_bound .. upper_bound
  dml_statement;
```

---

### 4.2 FORALL 示例

```sql
declare
  type t_empno_tab is table of emp.empno%type;
  v_empnos t_empno_tab := t_empno_tab(7369, 7499, 7521);
begin
  forall i in 1 .. v_empnos.count
    update emp set sal = sal * 1.1 where empno = v_empnos(i);
end;
/
```

---

## 5. BULK COLLECT + FORALL（标准高性能写法）

```sql
declare
  type t_emp is record (
    empno emp.empno%type,
    sal   emp.sal%type
  );
  type t_emp_tab is table of t_emp;
  v_emps t_emp_tab;
begin
  select empno, sal
    bulk collect into v_emps
    from emp
    where deptno = 10;

  forall i in 1 .. v_emps.count
    update emp
       set sal = v_emps(i).sal * 1.1
     where empno = v_emps(i).empno;
end;
/
```

> 📌 这是 **批处理 / 账务系统中的标准模板**。

---

## 6. LIMIT 分批处理（防止内存溢出）

当数据量极大时，不应一次性 BULK COLLECT 全部数据。

---

### 6.1 分批处理模板

```sql
declare
  cursor c_emp is
    select empno, sal from emp;

  type t_emp is record (
    empno emp.empno%type,
    sal   emp.sal%type
  );
  type t_emp_tab is table of t_emp;
  v_emps t_emp_tab;

  c_limit constant pls_integer := 1000;
begin
  open c_emp;
  loop
    fetch c_emp bulk collect into v_emps limit c_limit;
    exit when v_emps.count = 0;

    forall i in 1 .. v_emps.count
      update emp
         set sal = v_emps(i).sal * 1.1
       where empno = v_emps(i).empno;

    commit; -- 按批提交（项目策略）
  end loop;
  close c_emp;
end;
/
```

---

## 7. SAVE EXCEPTIONS（错误不中断）

默认情况下，FORALL 中一条 DML 失败会导致整体失败。

使用 `SAVE EXCEPTIONS` 可以记录错误并继续执行。

```sql
forall i in 1 .. v_emps.count save exceptions
  insert into emp values (...);
```

异常处理中可通过：

```sql
sql%bulk_exceptions
```

获取失败明细。

---

## 8. 批量处理中的事务策略（项目重点）

* 小数据量：一次提交
* 大数据量：**LIMIT + 分批 COMMIT**
* 账务系统：需结合业务一致性设计

> ⚠️ COMMIT 粒度是设计点，不是语法点。

---

## 9. 性能对比总结（面试必说）

| 写法 | 性能 | 说明 |
| ------------- | -- | -------------- |
| 游标逐行 | 最差 | SQL/PLSQL 切换最多 |
| BULK COLLECT | 好 | 批量读取 |
| FORALL | 很好 | 批量写入 |
| BULK + FORALL | 最优 | 批处理标准 |

---

## 10. 项目实践建议（非常重要）

* 批处理必须使用 BULK COLLECT + FORALL
* 大表一定要 LIMIT 分批
* 不要在 FORALL 内写复杂逻辑
* 结合日志、异常、事务整体设计

---

## 11. 本章小结（面试版）

* 批量处理核心是减少上下文切换
* BULK COLLECT 批量取数
* FORALL 批量执行 DML
* LIMIT 防止内存溢出
* 项目中必须配合事务策略
