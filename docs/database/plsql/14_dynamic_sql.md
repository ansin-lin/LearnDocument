# PL/SQL 动态 SQL（EXECUTE IMMEDIATE / DBMS_SQL）

> 本章系统讲解 **PL/SQL 动态 SQL**。这是从“会写固定 SQL”迈向“可应对复杂业务场景”的关键一步，也是**银行 / 批处理 / 通用框架类项目的高频考点**。

---

## 1. 什么是动态 SQL

**动态 SQL** 指的是：

> SQL 语句在**运行时才确定内容**，而不是在编译期写死。

典型特征：

* 表名不固定
* 字段名不固定
* WHERE 条件动态组合
* 动态执行 DDL

---

## 2. 什么时候必须使用动态 SQL

以下场景**无法用静态 SQL**：

| 场景 | 说明 |
| ------ | ----------------- |
| 动态表名 | 分表 / 月表 / 临时表 |
| 动态字段 | 报表 / 可配置查询 |
| 动态 DDL | CREATE / TRUNCATE |
| 通用框架 | 通用导入、导出、清理 |

📌 原则：

> **能用静态 SQL，绝不用动态 SQL**

---

## 3. EXECUTE IMMEDIATE（最常用）

### 3.1 基本语法

```plsql
execute immediate sql_string;
```

示例：

```plsql
execute immediate 'truncate table t_tmp';
```

---

### 3.2 动态 DML

```plsql
execute immediate
  'update emp set sal = sal * 1.1 where deptno = :1'
  using 10;
```

📌 必须使用 **绑定变量**，防止 SQL 注入。

---

### 3.3 INTO（接收返回值）

```plsql
declare
  v_cnt number;
begin
  execute immediate
  'select count(*) from emp where deptno = :1'
  into v_cnt
  using 10;
end;
/
```

---

## 4. 动态 SQL 中的绑定变量（重点）

### 错误写法（高风险）

```plsql
execute immediate
  'select * from emp where deptno = ' || v_deptno;
```

风险：

* SQL 注入
* 硬解析增多

---

### 正确写法

```plsql
execute immediate
  'select * from emp where deptno = :1'
  using v_deptno;
```

---

## 5. EXECUTE IMMEDIATE 的局限

EXECUTE IMMEDIATE **不擅长处理：**

* 列数不固定的 SELECT
* 返回结果集（多行多列）

这时需要：**DBMS_SQL**。

---

## 6. DBMS_SQL（了解即可，新人不必精通）

### 6.1 使用场景

* 列结构完全动态
* 通用 SQL 执行引擎

### 6.2 基本流程（认知级）

```text
OPEN_CURSOR
→ PARSE
→ BIND_VARIABLE
→ DEFINE_COLUMN
→ EXECUTE
→ FETCH_ROWS
→ CLOSE_CURSOR
```

📌 项目中：

* 90% 使用 EXECUTE IMMEDIATE
* DBMS_SQL 多见于框架层

---

## 7. 动态 SQL 与事务

* 动态 SQL **不影响事务模型**
* COMMIT / ROLLBACK 规则完全一致

---

## 8. 项目实践注意点（非常重要）

* 严禁拼接用户输入
* 动态 SQL 封装在 PACKAGE 中
* 尽量控制动态范围

---

## 9. 面试高频对比

| 项目 | EXECUTE IMMEDIATE | DBMS_SQL |
| ---- | ----------------- | -------- |
| 易用性 | 高 | 低 |
| 性能 | 好 | 略低 |
| 动态能力 | 一般 | 最强 |
| 使用频率 | 高 | 低 |

---

## 10. 本章小结

* 动态 SQL 用于运行期确定 SQL
* 首选 EXECUTE IMMEDIATE
* 必须使用绑定变量
* DBMS_SQL 用于高度动态场景

