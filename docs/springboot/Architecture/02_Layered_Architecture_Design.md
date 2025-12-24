# 02 分层架构设计（Layered Architecture Design）

## 1. 什么是分层架构？

分层架构是一种**按职责拆分系统的设计思想**，  
在 Spring Boot 中通常分为以下四层：

- Controller（表示层）
- Service（业务层）
- Mapper / Repository（数据访问层）
- Entity（实体层）

---

## 2. 分层架构调用关系

```text
Controller
    ↓
Service
    ↓
Mapper
    ↓
Database
```

**单向依赖是硬性规则。**

---

## 3. 各层详细职责

### 3.1 Controller 层

- 接收 HTTP 请求
- 参数校验
- DTO 转换
- 返回响应

禁止：

- 写 SQL
- 直接操作 Mapper

---

### 3.2 Service 层

- 核心业务逻辑
- 事务边界
- 多 Mapper 组合

```java
@Transactional
public void createOrder() {
}
```

---

### 3.3 Mapper 层

- 数据库访问
- SQL 定义
- 不写业务逻辑

---

### 3.4 Entity 层

- 表结构映射
- 数据模型

---

## 4. 为什么不能跨层调用？

跨层调用会导致：

- 高耦合
- 难测试
- 架构失控

---

## 5. 面试总结

> 分层架构通过明确职责边界，提高了系统的稳定性和可扩展性。
