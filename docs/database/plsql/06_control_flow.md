# PL/SQL 流程控制

> 本章介绍 **PL/SQL 中的流程控制语句**。流程控制用于决定程序的执行顺序，是实现业务判断、循环处理和批量逻辑的核心内容，在实际项目与面试中都非常重要。

---

## 1. 流程控制概述

PL/SQL 的流程控制主要分为三大类：

* 条件控制（IF / CASE）
* 循环控制（LOOP / WHILE / FOR）
* 跳转与退出控制（EXIT / CONTINUE）

---

## 2. IF 语句

### 2.1 IF … THEN

```sql
if condition then
  statements;
end if;
```

### 示例

```sql
declare
  v_score number := 75;
begin
  if v_score >= 60 then
    dbms_output.put_line('Pass');
  end if;
end;
/
```

---

### 2.2 IF … THEN … ELSE

```sql
if condition then
  statements;
else
  statements;
end if;
```

### 示例

```sql
declare
  v_age number := 16;
begin
  if v_age >= 18 then
    dbms_output.put_line('Adult');
  else
    dbms_output.put_line('Minor');
  end if;
end;
/
```

---

### 2.3 IF … ELSIF … ELSE

```sql
if condition1 then
  statements;
elsif condition2 then
  statements;
else
  statements;
end if;
```

### 示例

```sql
declare
  v_score number := 85;
begin
  if v_score >= 90 then
    dbms_output.put_line('Excellent');
  elsif v_score >= 60 then
    dbms_output.put_line('Pass');
  else
    dbms_output.put_line('Fail');
  end if;
end;
/
```

---

## 3. CASE 语句

CASE 语句用于多分支判断，结构清晰，**推荐在多条件场景使用**。

---

### 3.1 简单 CASE

```sql
case expression
  when value1 then statements;
  when value2 then statements;
  else statements;
end case;
```

### 示例

```sql
declare
  v_grade char(1) := 'B';
begin
  case v_grade
    when 'A' then dbms_output.put_line('Excellent');
    when 'B' then dbms_output.put_line('Good');
    when 'C' then dbms_output.put_line('Average');
    else dbms_output.put_line('Fail');
  end case;
end;
/
```

---

### 3.2 搜索 CASE（类似 IF/ELSIF）

```sql
case
  when condition1 then statements;
  when condition2 then statements;
  else statements;
end case;
```

### 示例

```sql
declare
  v_salary number := 8000;
begin
  case
    when v_salary >= 10000 then dbms_output.put_line('High');
    when v_salary >= 5000 then dbms_output.put_line('Middle');
    else dbms_output.put_line('Low');
  end case;
end;
/
```

---

## 4. LOOP 循环

### 4.1 基本 LOOP

```sql
loop
  statements;
  exit when condition;
end loop;
```

### 示例

```sql
declare
  v_cnt number := 1;
begin
  loop
    dbms_output.put_line(v_cnt);
    v_cnt := v_cnt + 1;
    exit when v_cnt > 5;
  end loop;
end;
/
```

---

## 5. WHILE 循环

```sql
while condition loop
  statements;
end loop;
```

### 示例

```sql
declare
  v_cnt number := 1;
begin
  while v_cnt <= 5 loop
    dbms_output.put_line(v_cnt);
    v_cnt := v_cnt + 1;
  end loop;
end;
/
```

---

## 6. FOR 循环（最常用）

FOR 循环语法最简洁，**推荐在计数场景使用**。

```sql
for counter in lower_bound .. upper_bound loop
  statements;
end loop;
```

### 示例

```sql
begin
  for i in 1 .. 5 loop
    dbms_output.put_line(i);
  end loop;
end;
/
```

### 反向循环

```sql
begin
  for i in reverse 1 .. 5 loop
    dbms_output.put_line(i);
  end loop;
end;
/
```

---

## 7. EXIT 与 CONTINUE

### EXIT

用于立即退出循环。

```sql
exit when condition;
```

### CONTINUE（Oracle 11g+）

用于跳过本次循环，进入下一次循环。

```sql
continue when condition;
```

### 示例

```sql
begin
  for i in 1 .. 5 loop
    continue when i = 3;
    dbms_output.put_line(i);
  end loop;
end;
/
```

---

## 8. 流程控制中的 NULL 注意点

* IF / CASE 中，条件结果为 NULL 时视为 **不成立**
* 判断 NULL 必须使用 `IS NULL`

```sql
if v_val is null then
  dbms_output.put_line('Value is NULL');
end if;
```

---

## 9. 项目实践建议

* 多分支判断优先使用 CASE，可读性更好
* 计数循环优先使用 FOR
* 循环内避免复杂 SQL，必要时使用批量处理（后续章节讲）
* 一定设置退出条件，防止死循环

---

## 10. 本章小结（面试版）

* IF / CASE 用于条件判断
* CASE 比 IF/ELSIF 更清晰
* LOOP / WHILE / FOR 用于循环控制
* FOR 循环最常用、最安全
* EXIT / CONTINUE 控制循环流程
