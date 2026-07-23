# PL/SQL 触发器（Trigger）

> 本章系统讲解 **PL/SQL 触发器（Trigger）** 的概念、类型和使用场景。触发器常用于 **审计日志、数据校验、自动维护字段** 等，但在项目中必须谨慎使用，是一个**“会用加分，用错扣分”**的知识点。

---

## 1. 什么是触发器

触发器是 **绑定在表或视图上的 PL/SQL 程序单元**，当特定事件发生时，Oracle 会自动执行触发器中的逻辑。

### 触发器的特点

* 自动执行，无需显式调用
* 与表事件强绑定
* 在事务内执行（与 DML 同生共死）

---

## 2. 触发器的常见使用场景

在实际项目中，触发器通常用于以下场景：

* 自动维护审计字段（created_at / updated_at）
* 记录数据变更日志（审计表）
* 简单的数据校验（不推荐复杂业务）
* 防止非法操作（如禁止删除核心表数据）

> ⚠️ 不推荐：
>
> * 在触发器中写复杂业务逻辑
> * 在触发器中调用远程服务

---

## 3. 触发器的分类

### 3.1 按触发事件分类

| 类型 | 说明 |
| ------ | ----- |
| INSERT | 插入时触发 |
| UPDATE | 更新时触发 |
| DELETE | 删除时触发 |

---

### 3.2 按触发时间分类

| 类型 | 说明 |
| ------ | --------- |
| BEFORE | DML 执行前触发 |
| AFTER | DML 执行后触发 |

---

### 3.3 行级触发器 vs 语句级触发器

| 类型 | 说明 |
| ------ | -------------------- |
| 行级触发器 | 对每一行触发（FOR EACH ROW） |
| 语句级触发器 | 对整条语句触发一次 |

---

## 4. 触发器的基本语法

```sql
create or replace trigger trigger_name
before | after insert | update | delete on table_name
for each row
begin
  -- 触发器逻辑
end;
/
```

---

## 5. 行级触发器中的 :NEW 与 :OLD

在 **行级触发器** 中，可以使用以下伪记录：

| 变量 | 含义 |
| ---- | -------- |
| :NEW | 当前要写入的新值 |
| :OLD | 修改前的旧值 |

### 示例：维护更新时间

```sql
create or replace trigger trg_emp_update_time
before update on emp
for each row
begin
  :new.updated_at := systimestamp;
end;
/
```

---

## 6. INSERT / UPDATE / DELETE 中 :OLD / :NEW 的可用性

| 操作 | :OLD | :NEW |
| ------ | ---- | ---- |
| INSERT | 不可用 | 可用 |
| UPDATE | 可用 | 可用 |
| DELETE | 可用 | 不可用 |

---

## 7. 审计日志触发器示例（项目常用）

```sql
create or replace trigger trg_emp_audit
after insert or update or delete on emp
for each row
begin
  if inserting then
 insert into emp_audit(empno, action_type, action_time)
 values(:new.empno, 'INSERT', systimestamp);
  elsif updating then
 insert into emp_audit(empno, action_type, action_time)
 values(:new.empno, 'UPDATE', systimestamp);
  elsif deleting then
 insert into emp_audit(empno, action_type, action_time)
 values(:old.empno, 'DELETE', systimestamp);
  end if;
end;
/
```

---

## 8. 触发器与事务

* 触发器 **不能显式 COMMIT / ROLLBACK**
* 触发器中的异常会导致 **整个 DML 失败并回滚**

```sql
raise_application_error(-20001, 'Operation not allowed');
```

---

## 9. 使用触发器的风险与注意点（面试高频）

* 触发器逻辑隐蔽，调试困难
* 多个触发器可能引发执行顺序问题
* 不当使用会导致性能问题
* 业务逻辑分散，不利于维护

> 📌 项目原则：
>
> * **简单、通用、不可绕过的规则** 才放触发器
> * 复杂业务逻辑应放在 PROCEDURE / PACKAGE 中

---

## 10. 本章小结

* 触发器是自动执行的 PL/SQL 程序单元
* 常用于审计、字段自动维护
* 行级触发器可使用 :NEW / :OLD
* 触发器与 DML 同一事务
* 项目中必须谨慎使用触发器
