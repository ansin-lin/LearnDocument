# PL/SQL 游标（Cursor）

> 本章系统讲解 **PL/SQL 中的游标（Cursor）**。
> 游标用于处理 **多行查询结果**，是 PL/SQL 与 SQL 结合的核心机制，在批处理、账务处理、请求书系统中被大量使用，也是面试中的**高频重点**。

---

## 1. 为什么需要游标

在 PL/SQL 中：

* `SELECT ... INTO` **只能返回一行**
* 当查询结果为多行时，必须使用 **游标** 逐行处理

典型使用场景：

* 批量处理表数据
* 对账、汇总、逐条校验
* 生成报表、账票数据

---

## 2. 游标的分类

PL/SQL 中常见的游标类型如下：

| 类型 | 说明 |
| --------- | --------------------- |
| 隐式游标 | Oracle 自动创建（DML、单行查询） |
| 显式游标 | 程序员显式声明，用于多行查询 |
| 游标 FOR 循环 | 最常用、最安全的游标写法 |
| 参数化游标 | 带参数的显式游标 |

---

## 3. 隐式游标（Implicit Cursor）

Oracle 对以下语句会自动创建隐式游标：

* INSERT
* UPDATE
* DELETE
* SELECT INTO

可通过 **游标属性** 获取执行结果信息。

### 常用隐式游标属性

| 属性 | 说明 |
| ------------ | --------------- |
| SQL%FOUND | 是否影响到至少一行 |
| SQL%NOTFOUND | 是否未影响任何行 |
| SQL%ROWCOUNT | 影响的行数 |
| SQL%ISOPEN | 是否打开（始终为 FALSE） |

### 示例

```sql
begin
  update emp set sal = sal + 100 where deptno = 10;

  if sql%rowcount = 0 then
    dbms_output.put_line('No rows updated');
  else
    dbms_output.put_line(sql%rowcount || ' rows updated');
  end if;
end;
/
```

---

## 4. 显式游标（Explicit Cursor）

当查询返回多行数据时，需要显式游标。

### 4.1 显式游标基本步骤

1. 声明游标
2. 打开游标（OPEN）
3. 取数据（FETCH）
4. 关闭游标（CLOSE）

### 基本语法

```sql
declare
  cursor c_emp is
    select empno, ename, sal from emp;
  v_empno emp.empno%type;
  v_ename emp.ename%type;
  v_sal   emp.sal%type;
begin
  open c_emp;
  loop
    fetch c_emp into v_empno, v_ename, v_sal;
    exit when c_emp%notfound;

    dbms_output.put_line(v_empno || ' ' || v_ename || ' ' || v_sal);
  end loop;
  close c_emp;
end;
/
```

---

## 5. 游标属性（Explicit Cursor Attributes）

| 属性 | 说明 |
| ---------- | --------------- |
| cursorName%FOUND | 最近一次 FETCH 是否成功 |
| cursorName%NOTFOUND | 最近一次 FETCH 是否失败 |
| cursorName%ROWCOUNT | 已 FETCH 的行数 |
| cursorName%ISOPEN | 游标是否打开 |

---

## 6. 游标 FOR 循环（强烈推荐）

游标 FOR 循环是 **最简洁、最安全、最常用** 的方式：

* 自动 OPEN / FETCH / CLOSE
* 不易写错
* 可读性最好

### 示例

```sql
begin
  for r in (
    select empno, ename, sal from emp where deptno = 10
  ) loop
    dbms_output.put_line(r.empno || ' ' || r.ename || ' ' || r.sal);
  end loop;
end;
/
```

---

## 7. 参数化游标（Parameterized Cursor）

参数化游标用于 **动态条件查询**。

### 示例

```sql
declare
  cursor c_emp(p_deptno number) is
    select empno, ename, sal
 from emp
 where deptno = p_deptno;
begin
  for r in c_emp(20) loop
    dbms_output.put_line(r.ename || ':' || r.sal);
  end loop;
end;
/
```

---

## 8. 游标变量（REF CURSOR）简介

REF CURSOR 是 **指向查询结果集的指针**，常用于：

* 存储过程向外部程序返回结果集
* 与 Java / 应用程序交互

```sql
type t_ref_cursor is ref cursor;
```

> 📌 REF CURSOR 的详细使用将在后续高级章节讲解。

---

## 9. 游标使用注意事项（项目经验）

* 能用 **游标 FOR 循环** 就不要手写 OPEN/FETCH/CLOSE
* 游标中避免复杂业务逻辑
* 大数据量场景注意性能（后续讲 BULK COLLECT）
* 游标内不要随意 COMMIT（需整体事务设计）

---

## 10. 本章小结

* SELECT INTO 只能处理单行结果
* 多行数据处理必须使用游标
* 游标 FOR 循环最安全、最常用
* 隐式游标通过 SQL%ROWCOUNT 判断 DML 结果
* 参数化游标用于动态条件查询

👉 下一章建议：**批量处理（BULK COLLECT / FORALL）与性能优化**
