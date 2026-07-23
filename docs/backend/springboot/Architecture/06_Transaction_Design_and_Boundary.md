# 06 事务设计与边界（@Transactional 教学版）

> 本章节用于 **Spring Boot + MyBatis 项目的事务教学 / 面试准备 / 架构规范说明**  
> 目标：讲清楚 **什么是事务、为什么要控制边界、@Transactional 如何正确使用**

---

## 一、什么是事务（Transaction）？

事务是一组**要么全部成功、要么全部失败**的数据库操作。

事务的核心目标：

- 保证数据一致性
- 防止中间状态被保存

---

## 二、事务的四大特性（ACID）【面试必背】

| 特性 | 含义 | 说明 |
| --- | --- | --- |
| Atomicity | 原子性 | 要么全成功，要么全失败 |
| Consistency | 一致性 | 事务前后数据保持一致 |
| Isolation | 隔离性 | 并发事务互不影响 |
| Durability | 持久性 | 提交后数据永久保存 |

面试一句话：

> 事务通过 ACID 特性保证数据的正确性和一致性。

---

## 三、为什么需要“事务边界”？

### 1. 典型业务场景

“下订单”流程：

1. 创建订单
2. 扣减库存
3. 写入支付记录

👉 三步必须 **作为一个整体**

如果中途失败：

- 订单写入了
- 库存没扣
- 数据就错了

---

## 四、Spring 中事务的基本使用方式

### 1. 最常见写法（Service 层）

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        // 1. 插入订单
        // 2. 扣减库存
        // 3. 写入日志
    }
}
```

**结论（教学重点）：**

- 事务一般加在 **Service 层**
- 不要加在 Controller / Mapper 上

---

## 五、为什么不在 Controller / Mapper 上加事务？

### 1. Controller 层

- 负责请求处理
- 不应关心数据库一致性

### 2. Mapper 层

- 单条 SQL
- 不具备业务语义

👉 **事务边界 = 业务边界 = Service 层**

---

## 六、@Transactional 的核心属性（面试高频）

### 1. rollbackFor（非常重要）

```java
@Transactional(rollbackFor = Exception.class)
```

说明：

- 默认只对 `RuntimeException` 回滚
- 若抛出 `Exception`，需要显式指定

---

### 2. propagation（事务传播行为）

| 类型 | 含义 |
| --- | --- |
| REQUIRED | 默认，有则加入，无则新建 |
| REQUIRES_NEW | 新建事务，挂起外部事务 |
| SUPPORTS | 有事务就用，没有就不用 |

面试常用：

- REQUIRED（默认）
- REQUIRES_NEW（子事务）

---

### 3. isolation（隔离级别）

| 级别 | 说明 |
| --- | --- |
| READ_COMMITTED | 常用，防止脏读 |
| REPEATABLE_READ | MySQL 默认 |
| SERIALIZABLE | 最严格，性能最差 |

---

## 七、事务不生效的常见原因（必考）

### 1. 方法不是 public

```java
@Transactional
private void doSomething() {}
```

❌ 不生效

---

### 2. 类内部方法调用（自调用）

```java
this.methodB();
```

❌ 事务不会生效

原因：绕过了 Spring 代理

---

### 3. 捕获异常但不抛出

```java
try {
} catch (Exception e) {
}
```

❌ 不回滚

---

## 八、正确的事务设计原则（教学总结）

- 事务范围要 **尽量小**
- 事务边界放在 Service
- 一个事务 = 一个完整业务动作
- 不在事务中做远程调用

---

## 九、对象分层 + 事务的配合（复习衔接）

```text
Controller
   ↓
Service（事务边界）
   ↓
DO / Entity（业务规则）
   ↓
Mapper（PO / SQL）
   ↓
Database
```

---

## 十、面试总结用语（可直接背）

> 在 Spring Boot 项目中，事务通常由 Service 层通过 @Transactional 进行控制，  
> 事务边界应与业务边界保持一致，  
> 通过合理的传播行为和回滚规则，保证系统数据的一致性和稳定性。
