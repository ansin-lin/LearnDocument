# 第七章：负载均衡与自动扩缩容

> 学习目标：
>
> - 理解 ALB + Auto Scaling 在真实 Web 项目中的角色
> - 掌握 Auto Scaling 的核心配置依据
> - 理解前后端分离架构下，ALB + Auto Scaling 如何配合使用
>
> 本章是从「能访问」升级到「**能稳定承载流量**」的关键章节。

---

## 7.1 从单机到高可用

在第六章之前，我们的典型架构是：

```text
浏览器 → EC2（Nginx / Spring Boot）
```

这个结构存在的问题：

- 单点故障：EC2 挂了，服务就没了
- 无法应对流量波动
- 人工扩容成本高、反应慢

📌 **结论：**
> 想让 Web 系统“像线上系统一样稳定”，必须引入：
>
> - 统一入口（ALB）
> - 自动伸缩能力（Auto Scaling）

---

## 7.2 ALB：系统的统一入口

ALB（Application Load Balancer）负责：

- 接收所有用户请求
- 根据规则转发请求
- 屏蔽后端实例变化

### 7.2.1 在前后端分离架构中的位置

典型前后端分离架构：

```text
浏览器
 ↓
CloudFront（可选）
 ↓
ALB
 ↓
EC2（Spring Boot API）
```

说明：

- 前端页面通常放在 **S3 + CloudFront**
- ALB 专注处理 **API 请求**
- 后端 EC2 可随时增减，对前端透明

---

### 7.2.2 ALB 是如何进行 API 分发的

在真实的 Web 项目中，ALB 并不是“随便转发请求”，  
而是**按照规则，把不同的 HTTP 请求转发到对应的后端服务**。

可以理解为一句话：

> **ALB = 根据请求内容，决定“这次请求该交给谁处理”**

---

#### ALB 分发请求的核心机制

ALB 的请求转发，依赖三个核心概念：

```text
Listener（监听器）
   ↓
Rule（规则）
   ↓
Target Group（目标组）
```

整体流程如下：

```text
客户端请求
  ↓
ALB Listener（监听端口 80 / 443）
  ↓
匹配 Listener Rule（规则）
  ↓
转发到 Target Group
  ↓
EC2 / Auto Scaling 中的实例
```

---

#### Listener（监听器）是什么？

Listener 定义了：

- ALB 监听 **哪个端口**
- 使用 **什么协议**

常见配置：

| 协议 | 端口 | 用途 |
| ------ | ------ | ------ |
| HTTP | 80 | 普通 API / 学习环境 |
| HTTPS | 443 | 生产环境（加密） |

📌 **只要用户请求打到这个端口，ALB 就会接收并处理请求。**

---

#### Rule（规则）：ALB 如何判断请求走向？

Rule 决定：

> **“什么样的请求，转发到哪个后端”**

ALB 支持多种判断条件，最常用的是以下两种。

##### 1️⃣ 基于路径的路由（Path-based Routing）

示例规则：

| 请求路径 | 转发目标 |
| ---------- | ---------- |
| `/api/*` | Spring Boot API 服务 |
| `/admin/*` | 管理后台服务 |
| `/health` | 健康检查接口 |

示例说明：

```text
https://api.example.com/api/users
→ 转发到 API 服务

https://api.example.com/health
→ 转发到健康检查接口
```

---

##### 2️⃣ 基于域名的路由（Host-based Routing）

当系统存在多个子域名时：

```text
api.example.com
admin.example.com
```

可以配置规则：

| Host | 转发目标 |
| ------ | ---------- |
| api.example.com | API 服务 |
| admin.example.com | 管理后台 |

📌 **一个 ALB 可以承载多个系统入口。**

---

#### Target Group（目标组）是什么？

Target Group 定义了：

- 请求最终转发到哪里
- 使用什么协议和端口
- 健康检查方式

Spring Boot 常见配置示例：

| 配置项 | 示例 |
| -------- | ------ |
| Target Type | Instance |
| 协议 | HTTP |
| 端口 | 8080 |
| 健康检查路径 | `/actuator/health` |

📌 **ALB 不直接与 EC2 通信，而是通过 Target Group 管理后端实例。**

---

#### ALB + Spring Boot API 的典型分发示例

假设 Spring Boot 应用监听在：

```text
EC2:8080
```

ALB 的配置逻辑为：

1. Listener：HTTP 80  
2. Rule：Path = `/api/*`  
3. Target Group：HTTP / 8080  

请求流程为：

```text
浏览器请求：
http://example.com/api/users

ALB 内部转发：
http://EC2_PRIVATE_IP:8080/api/users
```

📌 **浏览器永远不知道后端真实 IP 和端口。**

---

#### 为什么这种方式适合前后端分离？

- 后端端口不暴露公网
- 后端实例可随时扩缩容
- 前端配置始终稳定

前端只需配置：

```text
API_BASE_URL = https://api.example.com
```

---

#### 新手常见误区

- 直接让前端请求 EC2 IP
- 在前端写死 EC2 地址
- 不使用 ALB 进行统一入口管理

📌 **正确模式始终是：**

> 前端 → ALB → 后端服务

---

## 7.3 为什么 ALB 还不够？

即使有 ALB，如果：

- 后端只有 2 台 EC2
- 流量突然暴增 10 倍

那么结果仍然是：

- 所有 EC2 被打满
- 用户体验下降

📌 **问题本质：**
> ALB 只能“分流”，不能“变多”。

这正是 **Auto Scaling** 的作用。

---

## 7.4 Auto Scaling 是什么？

Auto Scaling 是 AWS 提供的：

> **根据负载情况，自动增加或减少 EC2 数量的机制。**

它解决的问题是：

- 流量大时 → 自动加机器
- 流量小了 → 自动减机器
- 全程无需人工介入

---

## 7.5 Auto Scaling 的三大核心组件

### 7.5.1 Launch Template（启动模板）

定义：

- 使用什么 AMI
- 实例类型（如 t3.micro）
- 安全组
- Key Pair

📌 可以理解为：

> **“EC2 的标准出厂模板”**

---

### 7.5.2 Auto Scaling Group（ASG）

ASG 负责管理 EC2 数量。

关键参数：

- Desired Capacity：期望实例数
- Min Size：最少实例数
- Max Size：最多实例数

示例：

```text
Min = 2
Desired = 2
Max = 6
```

含义：

- 平时至少 2 台保证可用
- 高峰期最多扩到 6 台

---

### 7.5.3 Scaling Policy（伸缩策略）

决定：

> **什么时候加机器？什么时候减机器？**

最常见依据：

- CPU 使用率
- 请求数
- 响应时间

---

## 7.6 常见的扩缩容配置依据（重点）

### 7.6.1 基于 CPU 使用率（新手首选）

示例策略：

- CPU > 70% 持续 5 分钟 → +1 台
- CPU < 30% 持续 10 分钟 → -1 台

优点：

- 简单直观
- 适合大多数 Web API 服务

---

### 7.6.2 基于 ALB 请求数（进阶）

- 根据每台 EC2 的请求数扩容
- 更贴近真实业务负载

适合场景：

- IO 密集型服务
- CPU 占用不明显的应用

---

## 7.7 ALB + Auto Scaling 的完整工作流程

```text
用户请求增加
   ↓
ALB 转发流量
   ↓
EC2 CPU / 请求数升高
   ↓
CloudWatch 触发告警
   ↓
Auto Scaling 扩容
   ↓
新 EC2 自动注册到 Target Group
```

📌 **整个过程完全自动。**

---

## 7.8 前后端分离项目中如何使用（实战思路）

### 7.8.1 前端

- Vue / React 构建后
- 部署到 S3
- 使用 CloudFront 加速

```text
浏览器 → CloudFront → S3
```

---

### 7.8.2 后端

- Spring Boot API
- EC2 + Auto Scaling Group
- 通过 ALB 暴露统一 API 域名

```text
前端 → ALB → EC2（Auto Scaling）
```

前端 **永远不关心**：

- 后端有几台机器
- 是否发生扩容

---

## 7.9 新手常见误区

- 只用 ALB，不开 Auto Scaling
- Auto Scaling Min 设为 1（有风险）
- 扩容太慢，阈值设置不合理
- 把前端页面也放在 EC2 上

📌 正确思路：

> 前端静态化 + 后端自动伸缩

---

## 7.10 本章总结

- ALB 解决的是「入口统一」
- Auto Scaling 解决的是「弹性能力」
- 两者必须配合使用
- 这是云架构区别于传统服务器的核心优势
- 本章是迈向“生产级架构”的关键一步

---
