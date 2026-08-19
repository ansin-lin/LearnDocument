
# 第十一章：Lambda 与 Serverless 架构（事件驱动与无服务器）

> 本章目标：
>
> - 理解什么是 Serverless，以及它解决了什么问题
> - 明确 Lambda 与 EC2 / 容器的本质区别
> - 掌握 Lambda 的核心概念：函数、事件、执行角色
> - 能构建一个最小可用的 Lambda + API Gateway 示例
> - 知道在前后端分离架构中，Lambda 应该放在什么位置

---

## 11.1 什么是 Serverless？

Serverless 并不是“没有服务器”，而是：

> **你不再需要管理服务器。**

在 Serverless 模式下：

- 不需要创建 EC2
- 不需要配置 Auto Scaling
- 不需要关心系统补丁
- 不需要考虑服务器容量

你只需要：

- 写业务代码
- 定义触发方式（事件）

AWS 负责其余所有事情。

---

## 11.2 Lambda 是什么？

AWS Lambda 是一种 **事件驱动的计算服务**。

你可以把 Lambda 理解为：

> **“只在需要时运行的一段代码”**

核心特点：

- 无需服务器管理
- 按执行次数和执行时间计费
- 自动扩缩容
- 支持多种语言（Java / Python / Node.js 等）

---

## 11.3 Lambda 与 EC2 的本质区别

| 对比项 | EC2 | Lambda |
| ------ | ----- | -------- |
| 服务器管理 | 需要 | 不需要 |
| 运行方式 | 持续运行 | 事件触发 |
| 扩缩容 | 手动/ASG | 自动 |
| 计费方式 | 按时间 | 按调用 |
| 适合场景 | 长时间服务 | 短任务、事件 |

📌 **一句话总结：**
> EC2 是“常开服务器”，Lambda 是“随叫随到”。

---

## 11.4 Lambda 的核心组成

### 11.4.1 Function（函数）

- 一段可执行代码
- 有入口方法（Handler）

### 11.4.2 Event（事件）

- 触发 Lambda 执行的输入
- 可以来自：API Gateway / S3 / 定时器 等

### 11.4.3 Execution Role（执行角色）

- Lambda 使用的 IAM Role
- 决定 Lambda 能访问哪些 AWS 资源

📌 再次强调：
> **服务访问资源，一定用 Role，不用 User。**

---

## 11.5 Lambda 的执行模型（新手重点）

一次 Lambda 执行过程：

1. 事件触发
2. AWS 创建执行环境
3. 注入 Role 权限
4. 执行业务代码
5. 返回结果
6. 执行环境可能被复用

重要限制：

- 单次执行最长 15 分钟
- 不是用来跑“常驻服务”的

---

## 11.6 API Gateway + Lambda（最经典组合）

在 Serverless 架构中，最常见模式是：

```text
浏览器 / 前端
 ↓
API Gateway（HTTPS 入口）
 ↓
Lambda（业务逻辑）
```

API Gateway 的职责：

- 提供 HTTPS 接口
- 路由请求
- 鉴权、限流

Lambda 专注：

- 处理业务逻辑

---

## 11.7 第一个 Lambda 示例（概念版）

示例：返回一个 JSON

```json
{
  "message": "Hello from Lambda"
}
```

触发方式：

- HTTP 请求（GET /hello）

前端访问：

```text
https://api.example.com/hello
```

---

## 11.8 Lambda 在前后端分离架构中的位置

### 11.8.1 与 EC2 架构对比

EC2 架构：

```text
前端 → ALB → EC2 → RDS
```

Serverless 架构：

```text
前端 → API Gateway → Lambda → DynamoDB / RDS
```

---

## 11.9 什么时候适合用 Lambda？

适合：

- 轻量 API
- 定时任务
- 文件处理（上传后处理）
- 事件驱动逻辑

不适合：

- 长连接服务
- 高性能计算
- 状态强依赖应用

---

## 11.10 Lambda 的日志与监控

- Lambda 日志自动进入 CloudWatch Logs
- 每次调用都有独立日志流
- 可设置错误告警

📌 **你在第十章学到的 CloudWatch，在这里继续复用。**

---

## 11.11 本章小结

- Lambda 是 Serverless 的核心
- 不需要管理服务器
- 按需执行、自动扩容
- 与 API Gateway 天然配合
- 非常适合现代前后端分离系统

---
