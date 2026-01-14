# PL/SQL 包（PACKAGE）

> 本章系统讲解 **PL/SQL 包（PACKAGE）** 的设计与使用。
> 包是大型 PL/SQL 项目（金融、账务、请求书系统）的**核心组织单元**，用于封装接口、隐藏实现、提升性能与可维护性，是**面试必考**内容。

---

## 1. 为什么项目一定要用 PACKAGE

在真实项目中，单独的过程/函数会迅速失控，PACKAGE 能解决：

* **接口与实现分离**：对外只暴露规范（SPEC）
* **统一命名空间**：避免同名冲突
* **隐藏私有逻辑**：只在 BODY 内可见
* **性能优势**：一次加载，减少解析与 I/O
* **版本可控**：只改 BODY 不影响调用方

---

## 2. PACKAGE 的基本结构

PACKAGE 由两部分组成：

| 组成 | 作用 |
| ------------ | -------- |
| PACKAGE SPEC | 对外声明（接口） |
| PACKAGE BODY | 具体实现（逻辑） |

> 📌 **原则**：
>
> * 调用方只依赖 **SPEC**
> * 实现细节全部放在 **BODY**

---

## 3. PACKAGE SPEC（规范）

### 3.1 规范中可以声明什么

* 公共常量
* 公共变量（慎用）
* 公共过程（PROCEDURE）
* 公共函数（FUNCTION）
* 类型定义（TYPE / SUBTYPE）

### 示例：PACKAGE SPEC

```sql
create or replace package pkg_account is
  -- 常量
  c_max_amount constant number := 10000;

  -- 公共函数
  function f_calc_fee(p_amount number) return number;

  -- 公共过程
  procedure p_transfer(
    p_from in number,
    p_to   in number,
    p_amt  in number
  );
end pkg_account;
/
```

---

## 4. PACKAGE BODY（实现）

### 4.1 BODY 中可以额外定义什么

* **私有过程 / 函数**（SPEC 中未声明）
* 私有变量
* 私有类型

### 示例：PACKAGE BODY

```sql
create or replace package body pkg_account is

  -- 私有函数（外部不可见）
  function f_internal_check(p_amt number) return boolean is
  begin
    return p_amt <= c_max_amount;
  end;

  function f_calc_fee(p_amount number) return number is
  begin
    return p_amount * 0.02;
  end;

  procedure p_transfer(
    p_from in number,
    p_to   in number,
    p_amt  in number
  ) is
  begin
    if not f_internal_check(p_amt) then
 raise_application_error(-20001, 'Amount exceeds limit');
    end if;

    update account set balance = balance - p_amt where id = p_from;
    update account set balance = balance + p_amt where id = p_to;
  end;

end pkg_account;
/
```

---

## 5. 调用 PACKAGE 中的过程与函数

### 调用过程

```sql
begin
  pkg_account.p_transfer(1, 2, 500);
end;
/
```

### 调用函数

```sql
declare
  v_fee number;
begin
  v_fee := pkg_account.f_calc_fee(1000);
end;
/
```

---

## 6. PACKAGE 初始化块（Initialization Section）

PACKAGE BODY 中可以包含 **初始化代码**，在第一次引用包时执行一次。

```sql
create or replace package body pkg_init is
  g_start_time timestamp;

begin
  g_start_time := systimestamp;
end pkg_init;
/
```

> 📌 特点：
>
> * 每个会话（Session）执行一次
> * 常用于缓存、初始化变量

---

## 7. PACKAGE 与权限管理（项目实务）

```sql
grant execute on pkg_account to app_user;
```

> 📌 项目中：
>
> * 应用账号只授予 PACKAGE 的 EXECUTE 权限
> * 不直接开放表的 DML 权限

---

## 8. PACKAGE 的常见设计规范

* 一个业务域一个 PACKAGE
* PACKAGE 名以 `pkg_` 开头
* 过程名以 `p_`，函数名以 `f_`
* 只在 SPEC 暴露必要接口
* 禁止在 SPEC 中声明可变全局变量

---

## 9. PACKAGE vs 单独 PROCEDURE / FUNCTION

| 项目 | PACKAGE | 单独对象 |
| ---- | ------- | ---- |
| 组织能力 | 强 | 弱 |
| 私有逻辑 | 支持 | 不支持 |
| 性能 | 更好 | 一般 |
| 可维护性 | 高 | 低 |

---

## 10. 项目实践总结

* 核心业务逻辑 **必须放在 PACKAGE 中**
* 对外接口稳定，内部实现可随时调整
* PACKAGE 是大型 PL/SQL 项目的基础设施

---

## 11. 本章小结（

* PACKAGE 由 SPEC 与 BODY 组成
* SPEC 定义接口，BODY 实现逻辑
* 支持私有过程/函数
* 提升可维护性与性能
* 项目中强烈推荐使用 PACKAGE
