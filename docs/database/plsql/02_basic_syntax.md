# PLSQL 基本语法

> 本章系统介绍 **PL/SQL 的基本语法结构与核心概念**。

---

## 1. PL/SQL 是什么

PL/SQL（Procedural Language/SQL）是 Oracle 提供的一种 **块结构（Block Structure）过程式语言**，用于在数据库端编写业务逻辑。

特点：

* 与 SQL 紧密结合（在 PL/SQL 中直接使用 SQL）
* 支持变量、条件判断、循环、异常处理
* 适合封装数据库业务逻辑（Procedure / Function / Package）

---

## 2. PL/SQL 的基本块结构

PL/SQL 程序由一个或多个 **PL/SQL 块（Block）** 组成，每个块最多包含三个部分：

1. **声明部分（DECLARE）**（可选）
   定义变量、游标、常量、子程序等。

2. **可执行部分（BEGIN … END）**（必须）
   编写可执行的 PL/SQL 与 SQL 语句。

3. **异常处理部分（EXCEPTION）**（可选）
   处理运行时异常。

### 基本结构示意

```sql
declare
  <declarations section>
begin
  <executable statements>
exception
  <exception handling>
end;
/
```

> ⚠️ 注意：
>
> * `END;` 仅表示语句结束
> * `/` 表示 **执行整个 PL/SQL 块**（在 SQL*Plus / SQL Developer 脚本模式中必须）

---

## 3. Hello World 示例

```sql
set serveroutput on;

declare
  message varchar2(20) := 'Hello, World!';
begin
  dbms_output.put_line(message);
end;
/
```

执行结果：

```text
Hello World
PL/SQL procedure successfully completed.
```

---

## 4. PL/SQL 标识符（Identifier）

PL/SQL 标识符用于命名：

* 变量
* 常量
* 游标
* 过程 / 函数
* 异常

### 标识符规则

* 以 **字母** 开头
* 可包含：字母、数字、`$`、`_`、`#`
* 最大长度：**30 个字符**
* **不区分大小写**（`integer` 等同于 `INTEGER`）
* **不能使用保留关键字**

---

## 5. PL/SQL 分隔符（Delimiter）

分隔符是具有特殊含义的符号：

| 分隔符     | 说明             |
|------------|------------------|
| + - * /    | 加、减、乘、除   |
| %          | 属性绑定         |
| '          | 字符串分隔符     |
| .          | 成员选择符       |
| ( )        | 表达式分组       |
| :=         | 赋值运算符       |
| \|\|       | 字符串连接       |

---

## 6. PL/SQL 注释

### 单行注释

```sql
-- 这是单行注释
```

### 多行注释

```sql
/*
  这是多行注释
*/
```

### 示例

```sql
declare
  -- 变量声明
  message varchar2(20) := 'Hello, World!';
begin
  /*
   * 输出信息
   */
  dbms_output.put_line(message);
end;
/
```

---

## 7. PL/SQL 程序单元（Program Unit）

PL/SQL 中常见的程序单元包括：

* PL/SQL 块（Anonymous Block）
* 过程（Procedure）
* 函数（Function）
* 包（Package）
* 包体（Package Body）
* 触发器（Trigger）
* 类型（Type）
* 类型体（Type Body）

> 在实际项目中，**核心业务逻辑通常封装在 Package 中**，由 Java / 批处理 / 作业调度调用。

---

## 8. 本章小结（面试版）

> PL/SQL 是 Oracle 提供的块结构过程语言，
> 每个 PL/SQL 块由声明区、执行区和异常区组成，
> 执行时需要使用 `/` 触发运行，
> 支持变量、流程控制、异常处理，
> 在项目中常以 Procedure / Function / Package 的形式使用。

---

👉 下一章建议阅读：**变量与数据类型（%TYPE / %ROWTYPE）**
