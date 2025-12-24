# 04 MVC 架构设计（Model-View-Controller）

## 1. MVC 是什么？

MVC 是一种**Web 应用的经典架构模式**，  
将系统分为：

- Model（模型）
- View（视图）
- Controller（控制器）

---

## 2. MVC 各组件职责

### 2.1 Model

- 业务数据
- 状态
- Entity / DTO

---

### 2.2 View

- 页面展示
- HTML / Thymeleaf / 前端框架

---

### 2.3 Controller

- 请求入口
- 调用 Model
- 返回 View 或 JSON

---

## 3. MVC 请求流程

```text
Client → Controller → Model → View → Client
```

---

## 4. MVC 与分层架构的关系（面试重点）

| MVC | 分层架构 |
| --- | --- |
| Controller | Controller |
| Model | Service / Entity |
| View | 前端 |

说明：

- MVC 关注**请求流程**
- 分层架构关注**代码组织**

---

## 5. Spring Boot 中的 MVC

- Spring Boot 默认集成 Spring MVC
- REST API 场景下 View 通常为 JSON

---

## 6. 面试总结

> MVC 是 Web 层的设计模式，而分层架构是整体系统的设计思想，二者通常结合使用。
