# 01 项目结构设计（Project Structure Design）

## 1. 设计背景

在 Spring Boot 项目中，**项目结构决定了项目的可维护性上限**。  
一个结构混乱的项目，即使功能能跑，也会在以下阶段迅速失控：

- 功能扩展
- 多人协作
- Bug 修复
- 面试讲解 / 交接

因此，在企业级开发（尤其是日本现场）中，项目结构设计是**基本功**。

---

## 2. 推荐的标准目录结构（MyBatis 场景）

```text
springboot-demo
 ├─ src
 │  ├─ main
 │  │  ├─ java
 │  │  │  └─ com.example.demo
 │  │  │     ├─ controller      // 表示层
 │  │  │     ├─ service         // 业务层
 │  │  │     ├─ service.impl    // 业务实现（可选）
 │  │  │     ├─ mapper          // 数据访问层
 │  │  │     ├─ entity          // 实体
 │  │  │     ├─ dto             // 传输对象
 │  │  │     ├─ config          // 配置类
 │  │  │     ├─ exception       // 全局异常
 │  │  │     └─ DemoApplication.java
 │  │  └─ resources
 │  │     ├─ application.yml
 │  │     └─ mapper
 │  │        └─ *.xml
 │  └─ test
 │     └─ java
 └─ pom.xml
```

---

## 3. 包结构设计原则（面试重点）

- 启动类放在**最外层包**
- 包按**职责（Layer）**而不是按功能堆叠
- Controller 只处理请求，不写业务
- Mapper 只做数据访问，不掺杂业务

---

## 4. 常见错误结构（反例）

```text
com.example.demo
 ├─ controller
 ├─ service
 ├─ mapper
 ├─ util
 ├─ common
``

问题：
- util / common 无限膨胀
- 职责不清
- 新人难以上手

---

## 5. 面试总结

> 良好的项目结构可以降低耦合、提高可维护性，是企业级 Spring Boot 项目的基础。
