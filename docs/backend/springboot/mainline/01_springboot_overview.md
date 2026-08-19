# 第1章 Spring Boot 是什么

> 本章目标：理解 Spring Boot 在企业 Web 后端项目中的作用，能说明它解决了什么问题，以及一个请求进入 Spring Boot 后大致经过哪些部分。

## 一、Spring Boot 的定位

Spring Boot 是 Java 后端开发中常用的 Web 应用框架。

它主要用于：

- 创建 Web API
- 接收前端请求
- 调用业务逻辑
- 访问数据库
- 返回 JSON 响应
- 管理配置、日志、测试和部署

在企业项目中，Spring Boot 通常不是单独存在，而是和 MyBatis、MySQL、Redis、日志框架、测试框架一起组成后端系统。

## 二、Spring Boot 解决的问题

传统 Spring 项目需要大量 XML 配置和手动整合。Spring Boot 的作用是减少重复配置，让项目能更快启动。

| 问题 | Spring Boot 的处理方式 |
| --- | --- |
| 创建 Web 项目复杂 | 提供起步依赖 |
| 配置分散 | 使用 `application.yml` 管理配置 |
| 运行方式复杂 | 内置 Web 服务器，可直接运行 |
| 依赖版本难管理 | 提供依赖版本管理 |
| 测试和部署麻烦 | 提供测试支持和 jar 打包方式 |

## 三、后端请求处理流程

一个典型请求会经过以下流程：

```text
浏览器或前端
  ↓ HTTP 请求
Controller
  ↓ 调用
Service
  ↓ 调用
Mapper
  ↓ 执行 SQL
MySQL
  ↓ 返回数据
Mapper
  ↓ 返回对象
Service
  ↓ 处理业务结果
Controller
  ↓ JSON 响应
前端页面
```

员工管理后端 API 会按照这个流程组织代码：Controller 接收请求，Service 处理业务，Mapper 访问数据库，最后返回 JSON 响应。

## 四、Spring Boot 项目中的常见文件

| 文件或目录 | 作用 |
| --- | --- |
| `pom.xml` | Maven 依赖管理文件 |
| `src/main/java` | Java 源代码 |
| `src/main/resources` | 配置文件、Mapper XML、静态资源 |
| `application.yml` | Spring Boot 配置文件 |
| `Application.java` | 项目启动类 |
| `controller` | 接收请求 |
| `service` | 编写业务逻辑 |
| `mapper` | 操作数据库 |
| `dto` | 接收请求数据 |
| `vo` | 返回响应数据 |

## 五、本章练习

请完成：

1. 说明 Spring Boot 在 Web 系统中负责什么。
2. 画出“前端请求员工列表”的后端处理流程。
3. 说明 Controller、Service、Mapper 的基本职责。

## 六、本章总结

- Spring Boot 是 Java Web 后端开发的主流框架。
- 它让项目创建、配置、运行和部署更简单。
- 企业后端项目通常按 Controller、Service、Mapper 分层。
- 员工管理 API 会持续使用这个分层思路完成接口、数据库访问、异常处理、测试和部署。
