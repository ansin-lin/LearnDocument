# PL/SQL 数据类型

> 本章面向 **PL/SQL 初学者**，系统介绍 Oracle PL/SQL 中常用的数据类型及使用场景，为后续学习变量、过程、函数和包打下基础。

---

## 1. 数据类型概述

在 PL/SQL 中，**变量、常量、参数都必须声明数据类型**。数据类型决定了：

* 存储格式
* 可用的操作
* 取值范围与约束

PL/SQL 的数据类型主要分为以下四类：

* **标量类型（SCALAR）**：单一值，没有内部结构
* **大对象类型（LOB）**：用于存储超大数据
* **复合类型**：由多个元素组成（记录、集合）
* **引用类型**：指向其他数据项

> 本章重点介绍 **标量类型** 和 **LOB 类型**，复合类型与引用类型将在后续章节讲解。

---

## 2. 标量（SCALAR）数据类型

标量类型表示一个不可再分的单一值，主要包括以下几类：

| 分类 | 说明 |
| ------ | ------ |
| 数值类型 | 执行算术运算的数值 |
| 字符类型 | 字符或字符串 |
| 布尔类型 | 逻辑判断 |
| 日期时间类型 | 日期和时间 |

---

## 3. 数值数据类型

PL/SQL 提供了多种数值类型及其子类型：

| 类型 | 描述 |
| ------ | ------ |
| PLS_INTEGER | 32 位带符号整数，性能最好 |
| BINARY_INTEGER | 与 PLS_INTEGER 类似（旧版本） |
| BINARY_FLOAT | 单精度浮点数 |
| BINARY_DOUBLE | 双精度浮点数 |
| NUMBER(p,s) | 最常用，支持定点与浮点 |
| INTEGER / INT | NUMBER 的子类型 |
| FLOAT / REAL / DOUBLE PRECISION | ANSI 浮点类型 |

### 示例

```sql
declare
  num1 integer;
  num2 real;
  num3 double precision;
begin
  null;
end;
/
```

> 📌 实际项目建议：
>
> * **计数、ID：PLS_INTEGER**
> * **金额、精度计算：NUMBER(p,s)**

---

## 4. 字符数据类型

| 类型 | 描述 |
| --------- | ------------ |
| CHAR | 固定长度字符串 |
| VARCHAR2 | 可变长度字符串（最常用） |
| RAW | 二进制数据 |
| NCHAR | 固定长度国家字符集 |
| NVARCHAR2 | 可变长度国家字符集 |
| LONG | 旧类型，不推荐 |
| LONG RAW | 旧类型，不推荐 |
| ROWID | 表中行的物理地址 |
| UROWID | 通用行标识符 |

> 📌 项目建议：
>
> * 统一使用 **VARCHAR2**
> * 不使用 LONG / LONG RAW

---

## 5. 布尔（BOOLEAN）类型

BOOLEAN 用于逻辑判断，仅存在于 **PL/SQL 中**：

* TRUE
* FALSE
* NULL

### 使用限制

BOOLEAN **不能用于**：

* SQL 语句
* SQL 内置函数
* SQL 直接调用的函数返回值

```sql
declare
  v_flag boolean := true;
begin
  if v_flag then
    dbms_output.put_line('TRUE');
  end if;
end;
/
```

> 📌 实务中：
>
> * 若需与 SQL / Java 交互，通常用 `NUMBER(1)` 或 `CHAR(1)` 代替 BOOLEAN

---

## 6. 日期时间类型

### DATE 类型

* 存储：年、月、日、时、分、秒
* 范围：公元前 4712 年 ~ 公元 9999 年

```sql
declare
  v_date date := sysdate;
begin
  dbms_output.put_line(v_date);
end;
/
```

默认格式由 `NLS_DATE_FORMAT` 控制（如：DD-MON-YY）。

### 日期时间字段范围（概念）

| 字段 | 取值范围 |
| ------ | ------------ |
| YEAR | -4712 ~ 9999 |
| MONTH | 01 ~ 12 |
| DAY | 01 ~ 31 |
| HOUR | 00 ~ 23 |
| MINUTE | 00 ~ 59 |
| SECOND | 00 ~ 59.9 |

> 📌 项目建议：
>
> * 表字段使用 `DATE` 或 `TIMESTAMP`
> * 显式使用 `TO_DATE / TO_CHAR` 控制格式

---

## 7. 大对象（LOB）数据类型

LOB 类型用于存储超大数据，如文本、图片、视频等。

| 类型 | 描述 | 大小 |
| ----- | --------- | ----------- |
| BFILE | 数据库存外二进制文件 | ≤ 4GB |
| BLOB | 数据库存内二进制 | 8TB ~ 128TB |
| CLOB | 大字符数据 | 8TB ~ 128TB |
| NCLOB | 国家字符大对象 | 8TB ~ 128TB |

> 📌 常见场景：
>
> * CLOB：日志、报文、XML、JSON
> * BLOB：附件、图片

---

## 8. 子类型（SUBTYPE）

子类型是 **已有数据类型的子集**，用于增强可读性和类型约束。

### 预定义子类型（STANDARD 包）

```sql
SUBTYPE CHARACTER IS CHAR;
SUBTYPE INTEGER   IS NUMBER(38,0);
```

### 用户自定义子类型示例

```sql
declare
  subtype name_t is char(20);
  subtype msg_t  is varchar2(100);

  salutation name_t;
  greetings  msg_t;
begin
  salutation := 'Reader ';
  greetings  := 'Welcome to the World of PL/SQL';

  dbms_output.put_line('Hello ' || salutation || greetings);
end;
/
```

---

## 9. PL/SQL 中的 NULL

NULL 表示 **未知或不存在的值**：

* 不等于 0
* 不等于空字符串
* `NULL = NULL` 的结果仍为 NULL

```sql
if v_val is null then
  -- 正确判断方式
end if;
```

> ⚠️ 不能使用 `=` 判断 NULL，必须使用 `IS NULL / IS NOT NULL`

---

## 10. 本章小结（新手版）

* PL/SQL 变量必须声明数据类型
* 常用标量类型：NUMBER、VARCHAR2、DATE
* BOOLEAN 只能在 PL/SQL 内使用
* LOB 用于存储大文本或二进制数据
* 子类型可提高代码可读性
* NULL 表示未知值，判断方式特殊

👉 下一章建议：**变量声明与 %TYPE / %ROWTYPE 的使用**
