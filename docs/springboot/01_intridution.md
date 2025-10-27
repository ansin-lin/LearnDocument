# Spring Boot 入门指南

## 🎯 目标

了解 Spring 家族体系结构，掌握如何快速创建并运行第一个 Spring Boot 项目。

---

## 一、Spring 体系概述

### 1. Spring Framework 是什么？

Spring 是一个轻量级、开源的 Java 企业级开发框架，主要用于简化企业应用开发。
其核心是 **IoC（控制反转）** 和 **AOP（面向切面编程）**。

**Spring 的核心模块包括：**

- **Spring Core**：IoC 容器，管理对象的创建与依赖注入。
- **Spring AOP**：面向切面编程，用于日志、事务等横切逻辑。
- **Spring MVC**：构建 Web 应用的 MVC 框架。
- **Spring Data**：简化数据库访问（整合 JPA、MyBatis 等）。
- **Spring Security**：认证与授权安全框架。
- **Spring Cloud**：微服务架构开发工具集。
- **Spring Boot**：快速构建 Spring 应用的开发框架。

---

## 二、Spring 核心理念

Spring 的强大之处在于它通过 **IoC** 与 **AOP** 两大机制实现了松耦合与可扩展的架构。

---

### 1️⃣ IoC（控制反转，Inversion of Control）

**概念：**  
传统开发中，对象的创建与依赖关系通常由开发者手动管理（`new` 对象）。  
而在 Spring 中，这一职责被“反转”给了容器，由容器自动创建与注入依赖对象。

```java
@Service
public class UserService {
    private final UserRepository repo;

    // Spring 自动注入 UserRepository
    public UserService(UserRepository repo) {
        this.repo = repo;
    }
}
```

Spring 启动时会：

1. 扫描注解（如 `@Component`, `@Service`, `@Repository`）；
2. 将这些类注册为 **Bean**；
3. 根据依赖关系自动注入（即 DI，Dependency Injection）。

**好处：**

- 解耦模块之间的依赖；
- 提高可测试性；
- 统一管理对象生命周期。

---

### 2️⃣ 什么是 Bean？

**定义：**  
> Bean 是由 Spring IoC 容器创建、管理、装配和销毁的对象。

当你使用诸如 `@Component`、`@Service`、`@Bean` 等注解时，
Spring 会自动将这些类实例化并交由容器统一管理。

**示例：**

```java
@Component  //表示当前类是一个由 Spring 管理的组件（Bean）；
public class HelloService {
    public String sayHi() {
        return "Hi from Bean!";
    }
}
```

使用时无需手动 `new`，容器会自动注入：

```java
@RestController
public class HelloController {
    private final HelloService service;

    public HelloController(HelloService service) {
        this.service = service;
    }

    @GetMapping("/hi")
    public String hi() {
        return service.sayHi();
    }
}
```

👉 `HelloService` 在 Spring 容器中就是一个 **Bean**。

---

### 3️⃣ AOP（面向切面编程，Aspect-Oriented Programming）

**概念：**  
AOP 通过“横切关注点（Cross-Cutting Concerns）”将日志、事务、安全等通用逻辑从业务逻辑中抽离。  
这样可以避免在多个类中重复编写相同的代码。

**常见横切逻辑：**

- 日志记录
- 事务控制
- 权限验证
- 性能统计

**AOP 的核心思想：**
> 在不修改原有代码的前提下，动态地为方法添加额外功能。

**示例：**

```java
//表示该类是一个 切面（Aspect）；
//启用后，Spring AOP 会在运行时通过代理机制,在目标方法执行前后织入这些切面代码。
@Aspect
@Component
public class LogAspect {

    //定义一个 前置通知（Before Advice）；
    @Before("execution(* com.example.demo.service.*.*(..))")
    public void beforeMethod() {
        System.out.println("方法开始执行");
    }

    //定义一个 后置通知（After Advice）；
    @After("execution(* com.example.demo.service.*.*(..))")
    public void afterMethod() {
        System.out.println("方法执行结束");
    }
}
```

结果：

- 每次调用 `service` 包中的任意方法，都会自动打印前后日志；
- 无需修改原有业务类的任何代码。

---

## 三、Spring Boot 简介

Spring Boot 是 Spring 官方推出的快速开发框架，  
通过“约定大于配置”的理念，让开发者无需复杂配置即可启动应用。

**Spring Boot 的优势：**

1. 无需繁琐的 XML 配置（基于注解与自动装配）。  
2. 内嵌 Web 服务器（Tomcat/Jetty）。  
3. 内置依赖管理（Starter）。  
4. 一键启动运行（`main()` 方法即可运行）。

---

## 四、创建 Spring Boot 项目

Spring 官方提供了一个项目生成器网站 —— **Spring Initializr**，可以快速生成标准化的 Spring Boot 项目骨架。

### 1. 访问入口

打开浏览器访问：  
👉 [https://start.spring.io](https://start.spring.io)

---

### 2. 界面配置说明

- Project：选择构建工具
      - 推荐：自定义名称(如：LearnProject)
- Language：选择开发语言
      - 推荐：Java
- Spring Boot Version：选择 Spring Boot 版本
      - 推荐使用稳定版，如 3.x 系列
- Project Metadata（项目基础信息）：
      - Group：组织或公司域名反转（如 com.example）
      - Artifact：项目名（如 demo）
      - Name：应用名，默认与 Artifact 一致
      - Description：项目描述（可选）
      - Package name：包路径，一般自动生成
      - Packaging：打包类型（推荐使用 Jar）
      - Java：选择当前使用的 JDK 版本（如 17 或 21）

---

### 3. 依赖选择

点击 **Add Dependencies** 按钮添加所需模块。常见依赖有：

- `Spring Web`：用于开发 RESTful Web 服务。  
- `Spring Boot DevTools`：热部署，提高开发效率。  
- `Lombok`：简化 Java 实体类代码。  
- `Spring Configuration Processor`：支持 YML 自动提示。  
- `Spring Data JPA`：数据库访问支持。

> ✅ 对于入门项目，只需添加 `Spring Web` 即可。

---

### 4. 生成并下载项目

配置完成后点击 **Generate**，即可下载项目压缩包。  
将其解压后即可通过 IDE（如 IntelliJ IDEA、Eclipse）打开。

---

### 5. 导入到 IDEA

1. 打开 Eclipse → `File → Import`。  
2. 选择解压后的项目文件夹。  
3. IDEA 会自动识别 Maven 并下载依赖。  
4. 等待依赖加载完成后即可运行 `DemoApplication`。

---

### 6. 项目结构说明

```java
hello-springboot/
 ├─ src/
 │   └─ main/
 │       ├─ java/com/example/demo/
 │       │   └─ DemoApplication.java
 │       └─ resources/application.yml
 └─ pom.xml
```


---

## 五、创建第一个 Spring Boot 项目

在上述项目中按照下述结构创建包和文件

```java
hello-springboot/
 ├─ src/
 │   └─ main/
 │       ├─ java/com/example/demo/
 │       │   ├─ DemoApplication.java
 │       │   └─ controller/HelloController.java
 │       └─ resources/application.yml
 └─ pom.xml
```


### 1. 关键文件说明

#### 📄 pom.xml

此文件内容保持不变

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>1.0.0</version>
    <name>hello-springboot</name>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.2</version>
    </parent>

    <dependencies>
        <!-- Web 模块 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 测试模块 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

#### 📘 DemoApplication.java

文件内容保持不变

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

> `@SpringBootApplication` 是核心注解，整合了 `@Configuration`、`@EnableAutoConfiguration`、`@ComponentScan`。

---

#### 📘 HelloController.java

```java
package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello Spring Boot!";
    }
}
```

---

#### ⚙ application.yml

```yaml
server:
  port: 8080
spring:
  application:
    name: hello-springboot
```

---

#### ⚙ application.properties（对比示例）

```properties
server.port=8080
spring.application.name=hello-springboot
```

---

### 🧩 `.yml` 与 `.properties` 文件的区别与适用范围

- **语法格式：**
    - `.yml` 使用缩进表示层级关系，更简洁清晰。
    - `.properties` 使用 `key=value` 形式，更接近传统 Java 配置。

- **可读性：**
    - `.yml` 结构清晰，适合复杂配置。
    - `.properties` 简单直接，适合少量参数。

- **注释：**
    - `.yml` 使用 `#`。
    - `.properties` 可使用 `#` 或 `!`。

- **优先级：**
    - 若两个文件同时存在，**`application.properties` 优先于 `application.yml`**。

- **适用范围：**
    - `.yml` 推荐用于现代项目和多环境配置（如 `application-dev.yml`）。
    - `.properties` 适合旧项目或迁移兼容场景。

---

## 六、运行项目

执行以下命令即可启动项目：

```bash
mvn spring-boot:run
```

或直接运行 `DemoApplication.java` 的 `main()` 方法。

浏览器访问：  
👉 <http://localhost:8080/hello>  
输出：  

```bash
Hello Spring Boot!
```

---

## 七、Spring Boot 启动流程（简述）

1. 执行 `SpringApplication.run()`  
2. 加载 Spring Boot 自动配置（`@EnableAutoConfiguration`）  
3. 创建 IoC 容器，注册 Bean  
4. 启动内嵌 Tomcat 并监听端口  
5. 完成应用加载并运行

---

## 八、延伸阅读

| 模块 | 说明 |
|------|------|
| `spring-boot-starter-web` | 提供 Web MVC、Tomcat、JSON 支持 |
| `spring-boot-starter-data-jpa` | 集成 JPA 与数据库操作 |
| `spring-boot-starter-security` | 添加安全认证与授权 |
| `spring-boot-starter-thymeleaf` | 模板引擎支持 |
| `spring-boot-devtools` | 热部署支持 |

---

> 💡 **总结：**
>
> - Spring Boot 简化了传统 Spring 项目的复杂配置。
> - 使用 Spring Initializr 可以快速生成标准项目结构。
> - `@SpringBootApplication` 即可快速启动一个 Web 服务。
> - 推荐结合 IDEA 的 Spring Initializr 自动生成项目骨架。
