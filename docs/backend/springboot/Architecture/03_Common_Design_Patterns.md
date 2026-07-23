# 03 常用设计模式（Common Design Patterns）

## 1. 为什么要使用设计模式？

设计模式是对**高频问题的成熟解决方案总结**。  
Spring Boot 本身就是设计模式的集大成者。

---

## 2. Spring Boot 中的常见设计模式

### 2.1 单例模式（Singleton）

- Spring Bean 默认单例

---

### 2.2 工厂模式（Factory）

- Spring IOC 容器本质是工厂

---

### 2.3 代理模式（Proxy）

- Spring AOP
- 事务、日志、权限

---

### 2.4 模板方法模式（Template Method）

- JdbcTemplate
- MyBatis 执行流程

---

## 3. 面试高频问题

- Q：Spring AOP 使用了什么设计模式？  
  A：代理模式

---

## 4. 面试总结

> 设计模式提升的是系统的可扩展性和可维护性，而不是代码复杂度。
