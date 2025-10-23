# Spring Boot 学习路线（零基础版本）

## 一、阶段总览

| 阶段 | 学习周期 | 学习目标 |
|------|-----------|-----------|
| 第1阶段：入门与环境搭建 | 1周 | 理解Spring Boot是什么，会搭建运行第一个项目 |
| 第2阶段：核心注解与配置 | 2周 | 掌握依赖注入、配置文件、控制层基础 |
| 第3阶段：Web开发核心 | 3周 | 掌握Controller、Restful接口、参数传递、模板引擎 |
| 第4阶段：数据库操作 | 3周 | 学会用Spring Data JPA/MyBatis连接数据库 |
| 第5阶段：综合项目实战 | 2~3周 | 做出一个小型管理系统（如用户管理/博客） |
| 第6阶段：进阶与部署 | 2周 | 掌握打包部署、配置文件分环境、异常处理、拦截器等 |

---

## 二、阶段详细学习内容与代码结构

### ✅ 阶段1：Spring Boot 入门

**目标**：了解Spring体系、创建第一个Spring Boot项目

**示例代码结构：**
```
hello-springboot/
 ├─ src/
 │   ├─ main/
 │   │   ├─ java/com/example/demo/
 │   │   │   ├─ DemoApplication.java
 │   │   │   └─ controller/HelloController.java
 │   │   └─ resources/application.yml
 └─ pom.xml
```

---

### ✅ 阶段2：核心注解与配置

**目标**：掌握IOC与配置文件

**示例代码结构：**
```
config-demo/
 ├─ controller/UserController.java
 ├─ service/UserService.java
 ├─ config/AppConfig.java
 └─ resources/application.yml
```

---

### ✅ 阶段3：Web 开发与 Restful 接口

**目标**：掌握后端接口开发流程

**示例代码结构：**
```
web-demo/
 ├─ controller/UserController.java
 ├─ dto/UserDTO.java
 ├─ service/UserService.java
 ├─ entity/User.java
 └─ resources/application.yml
```

---

### ✅ 阶段4：数据库与持久层

**目标**：掌握数据库连接与ORM

**JPA示例结构：**
```
db-jpa-demo/
 ├─ entity/User.java
 ├─ repository/UserRepository.java
 ├─ service/UserService.java
 ├─ controller/UserController.java
 └─ resources/application.yml
```

**MyBatis示例结构：**
```
db-mybatis-demo/
 ├─ entity/User.java
 ├─ mapper/UserMapper.java
 ├─ mapper/xml/UserMapper.xml
 ├─ service/UserService.java
 ├─ controller/UserController.java
 └─ resources/application.yml
```

---

### ✅ 阶段5：综合项目实战

**目标**：整合所有知识，开发一个完整系统

**项目结构：**
```
user-system/
 ├─ controller/
 ├─ service/
 ├─ repository/
 ├─ entity/
 ├─ config/
 ├─ common/
 ├─ exception/
 └─ resources/application-dev.yml
```

---

### ✅ 阶段6：进阶与部署

**目标**：能独立部署上线

**示例结构：**
```
server-deploy-demo/
 ├─ config/WebConfig.java
 ├─ interceptor/LoginInterceptor.java
 ├─ task/ScheduleTask.java
 ├─ common/Result.java
 ├─ controller/*
 ├─ service/*
 └─ resources/application-prod.yml
```

---

## 三、推荐资源

| 类型 | 推荐内容 |
|------|-----------|
| 视频 | 黑马程序员、尚硅谷《Spring Boot 从入门到精通》 |
| 文档 | [Spring 官方文档](https://spring.io/projects/spring-boot) |
| 书籍 | 《Spring Boot 实战（第3版）》 |
| 工具 | IDEA、Maven、Git、Postman、MySQL |

---

## 四、进阶方向

- Spring Security / JWT 认证授权  
- Redis 缓存管理  
- 异步任务与消息队列（RabbitMQ/Kafka）  
- Spring Cloud 微服务  
- Docker 容器化部署  
