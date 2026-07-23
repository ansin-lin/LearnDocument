# PL/SQL 与 Java / Spring 的协作模式

> 本章系统讲解 **PL/SQL 与 Java / Spring 在真实项目中的协作方式**。这是银行、金融、核心系统中**最常见、也是最容易出问题的交互边界**。掌握这一章，意味着你不仅会写 PL/SQL，也真正理解 **“数据库逻辑 + 应用逻辑如何分工”**。

---

## 1. 为什么需要 PL/SQL + Java 协作

在企业级系统中，几乎不会出现：

> ❌ 全部逻辑写在 Java
> ❌ 全部逻辑写在 PL/SQL

而是 **职责分离**：

| 层级 | 主要职责 |
| ------------- | -------------- |
| Java / Spring | 接口、流程编排、事务协调 |
| PL/SQL | 数据密集型业务、校验、批处理 |

---

## 2. 常见协作架构模式

### 2.1 经典三层结构

```text
Controller
  ↓
Service（@Transactional）
  ↓
DAO / Mapper
  ↓
PL/SQL PACKAGE
  ↓
TABLE
```

📌 重点：

* **Java 不直接操作复杂 SQL**
* **所有核心数据逻辑收敛到 PACKAGE**

---

## 3. Java 调用 PL/SQL 的三种方式（含完整执行示例）

下面以一个 **账户转账 PACKAGE：pkg_account.p_transfer** 为例，贯穿说明 Java / Spring 的调用与执行流程。

### 示例 PACKAGE（数据库侧）

```sql
create or replace package pkg_account is
  procedure p_transfer(
    p_from_id in number,
    p_to_id   in number,
    p_amount  in number,
    o_code    out number,
    o_msg     out varchar2
  );
end pkg_account;
/
```

```sql
create or replace package body pkg_account is
  procedure p_transfer(
    p_from_id in number,
    p_to_id   in number,
    p_amount  in number,
    o_code    out number,
    o_msg     out varchar2
  ) is
  begin
    update account set balance = balance - p_amount where id = p_from_id;
    update account set balance = balance + p_amount where id = p_to_id;

    o_code := 0;
    o_msg  := 'SUCCESS';
  exception
    when others then
      o_code := -1;
      o_msg  := sqlerrm;
  end;
end pkg_account;
/
```

---

### 3.1 JDBC CallableStatement（最底层、最清晰）

```java
CallableStatement cs = conn.prepareCall(
  "{call pkg_account.p_transfer(?,?,?,?,?)}"
);
cs.setLong(1, fromId);
cs.setLong(2, toId);
cs.setBigDecimal(3, amount);
cs.registerOutParameter(4, Types.INTEGER);
cs.registerOutParameter(5, Types.VARCHAR);

cs.execute();

int code = cs.getInt(4);
String msg = cs.getString(5);
```

适合场景：

* 原生 JDBC
* 对执行细节、性能、事务边界要求高

---

### 3.2 Spring JdbcTemplate 调用示例

```java
jdbcTemplate.execute(
  "{call pkg_account.p_transfer(?,?,?,?,?)}",
  (CallableStatement cs) -> {
    cs.setLong(1, fromId);
    cs.setLong(2, toId);
    cs.setBigDecimal(3, amount);
    cs.registerOutParameter(4, Types.INTEGER);
    cs.registerOutParameter(5, Types.VARCHAR);

    cs.execute();

    int code = cs.getInt(4);
    String msg = cs.getString(5);
    if (code != 0) {
      throw new RuntimeException(msg);
    }
    return null;
  }
);
```

---

### 3.3 MyBatis 调用存储过程（现场最常见）

#### Mapper XML

```xml
<select id="transfer" statementType="CALLABLE">
  {call pkg_account.p_transfer(
    #{fromId, mode=IN},
    #{toId, mode=IN},
    #{amount, mode=IN},
    #{code, mode=OUT, jdbcType=INTEGER},
    #{msg,  mode=OUT, jdbcType=VARCHAR}
  )}
</select>
```

#### Java 调用

```java
Map<String, Object> param = new HashMap<>();
param.put("fromId", fromId);
param.put("toId", toId);
param.put("amount", amount);

mapper.transfer(param);

Integer code = (Integer) param.get("code");
String msg = (String) param.get("msg");
```

---

### 3.2 Spring JdbcTemplate

```java
jdbcTemplate.execute(
  "call pkg_account.p_transfer(?,?,?)",
  (CallableStatement cs) -> {
    cs.setLong(1, fromId);
    cs.setLong(2, toId);
    cs.setBigDecimal(3, amount);
    return cs.execute();
  }
);
```

---

### 3.3 MyBatis 调用存储过程（常用）

```xml
<select id="transfer" statementType="CALLABLE">
  {call pkg_account.p_transfer(
    #{fromId, mode=IN},
    #{toId, mode=IN},
    #{amount, mode=IN}
  )}
</select>
```

📌 银行 / 项目现场 **最常见**。

---

## 4. 入参 / 出参设计规范（非常重要）

### 4.1 推荐参数风格

```plsql
procedure p_transfer(
  p_from_id in number,
  p_to_id   in number,
  p_amount  in number,
  o_code    out number,
  o_msg     out varchar2
);
```

📌 原则：

* **不直接抛 DB 异常给 Java**
* 用返回码 + 消息

---

## 5. Java 与 PL/SQL 的事务边界（重点，含执行示例）

### 5.1 在线事务（Web 接口）——Java 控制事务

```java
@Service
public class TransferService {

  @Transactional
  public void transfer(Long fromId, Long toId, BigDecimal amount) {
    mapper.transfer(Map.of(
      "fromId", fromId,
      "toId", toId,
      "amount", amount
    ));

    // 这里可以继续调用其他 PACKAGE
    // 最终由 Spring 统一 COMMIT / ROLLBACK
  }
}
```

PL/SQL 侧原则：

* ❌ 不写 COMMIT
* ❌ 不写 ROLLBACK

---

### 5.2 批处理事务——PL/SQL 控制事务

```plsql
procedure p_batch_main is
begin
  for r in (select * from t_job where status = 'INIT') loop
    update t_job set status = 'DONE' where id = r.id;

    if mod(r.id, 1000) = 0 then
      commit;
    end if;
  end loop;

  commit;
end;
```

适用：

* 夜间批处理
* DBMS_SCHEDULER 作业

---

### 5.2 什么时候 PL/SQL 可以 COMMIT

| 场景 | 是否允许 |
| --------- | ---- |
| Java 在线事务 | ❌ |
| 夜间批处理 | ✅ |
| 自治事务日志 | ✅ |

---

## 6. 异常与错误码协作模式

### 6.1 PL/SQL 侧

```plsql
exception
  when others then
    o_code := -1;
    o_msg  := sqlerrm;
```

### 6.2 Java 侧

```java
if (code != 0) {
  throw new BusinessException(msg);
}
```

📌 避免：

* Java 捕获 ORA-xxxxx

---

## 7. 批处理中的协作方式（执行视角）

### 7.1 Java 触发，数据库执行（常见）

```java
// Java 只负责触发
schedulerClient.run("pkg_batch.main");
```

```sql
-- DB 中执行核心逻辑
begin
  pkg_batch.main;
end;
/
```

特点：

* Java 无业务循环
* 数据处理全部在 DB 内完成

---

### 7.2 数据库定时，Java 监控

* DBMS_SCHEDULER 定时执行
* Java 查询 batch_log 表
* 展示运行状态 / 告警

---

## 8. 性能与连接管理注意点

* PL/SQL 批处理尽量减少 Java 循环调用
* 避免 Java for 循环中调用存储过程
* 连接池大小与批处理时间协调

---

## 9. 面试高频问题（标准答案方向）

* 为什么不用 Java 写所有逻辑？
* 存储过程里能不能 COMMIT？
* Java 和 PL/SQL 谁控制事务？

关键词：

> **职责分离、事务统一、接口稳定**

---

## 10. 本章小结（工程师版）

* Java 负责流程与事务
* PL/SQL 负责数据密集型逻辑
* 核心逻辑必须封装为 PACKAGE
* 事务边界必须清晰

👉 下一章建议：**PL/SQL 安全、权限与部署规范**
