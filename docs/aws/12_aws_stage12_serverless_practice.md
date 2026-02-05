
# 第十二章：Serverless 实战 —— S3 + Lambda + API Gateway

> 本章目标：
>
> - 通过完整实战理解 Serverless 架构的真实用法
> - 掌握 S3 事件触发 Lambda 的典型模式
> - 掌握 API Gateway + Lambda 构建后端 API
> - 理解 Serverless 在前后端分离项目中的实际落点

---

## 12.1 为什么需要 Serverless 实战？

在第十一章中，你已经理解了：

- Lambda 是什么
- Serverless 的核心思想
- Lambda 与 EC2 的本质区别

但 **Serverless 是否真的好用，必须通过实战来体会**。

📌 本章将通过两个典型场景：

1. **文件上传自动处理**
2. **无服务器后端 API**

让你真正“用过一次 Serverless”。

---

## 12.2 实战一：S3 触发 Lambda（文件处理）

### 12.2.1 场景说明

典型需求：

> 用户上传文件 → 系统自动处理（压缩 / 校验 / 转码 / 解析）

传统做法：

```text
上传 → EC2 → 后端处理
```

Serverless 做法：

```text
上传 → S3 → Lambda 自动执行
```

📌 优势：

- 无需服务器
- 按需执行
- 成本极低

---

### 12.2.2 架构图（逻辑）

```text
用户
 ↓
S3（上传文件）
 ↓ Event
Lambda（处理文件）
 ↓
CloudWatch Logs
```

---

### 12.2.3 创建 S3 Bucket

注意事项：

- Bucket 名称全局唯一
- 建议开启版本控制（可选）

---

### 12.2.4 创建 Lambda 函数

- Runtime：Python 3.x / Node.js
- 执行角色：
    - 允许访问 S3
    - 允许写 CloudWatch Logs

---

### 12.2.5 配置 S3 事件触发 Lambda

在 S3 中配置：

- Event type：ObjectCreated
- Prefix / Suffix（可选）
- Target：Lambda Function

这样每次文件上传都会触发 Lambda。

---

### 12.2.6 Lambda 示例逻辑（概念）

```python
def handler(event, context):
    for record in event['Records']:
        bucket = record['s3']['bucket']['name']
        key = record['s3']['object']['key']
        print(f"File uploaded: {bucket}/{key}")
```

日志会自动进入 CloudWatch。

---

## 12.3 实战二：API Gateway + Lambda（无服务器后端）

### 12.3.1 场景说明

典型前后端分离需求：

> 前端需要一个 API，但不想维护服务器

Serverless 架构：

```text
前端
 ↓
API Gateway
 ↓
Lambda
 ↓
数据库 / 其他服务
```

---

### 12.3.2 创建 API Gateway

选择：

- HTTP API（新手推荐）
- REST API（功能更强，配置更复杂）

---

### 12.3.3 绑定 Lambda

- 创建路由（如 GET /hello）
- 绑定 Lambda 函数
- 启用自动部署

---

### 12.3.4 Lambda 返回示例

```json
{
  "statusCode": 200,
  "body": "{\"message\": \"Hello from Serverless API\"}"
}
```

---

### 12.3.5 前端如何调用

```js
fetch("https://api.example.com/hello")
  .then(res => res.json())
  .then(data => console.log(data))
```

前端无需知道服务器在哪里。

---

## 12.4 Serverless 与数据库的关系

常见组合：

- Lambda + DynamoDB（最常见）
- Lambda + RDS（需注意连接数）

📌 新手建议：
> 先理解“无状态 API”，再接数据库

---

## 12.5 Serverless 的监控与日志

- Lambda 日志 → CloudWatch Logs
- API Gateway 请求 → CloudWatch Metrics
- 错误率可直接告警

你在第十章学到的内容，在这里全部复用。

---

## 12.6 Serverless 的优缺点总结

### 优点

- 不用管服务器
- 自动扩缩容
- 成本低（低频调用）

### 缺点

- 冷启动
- 执行时间限制
- 不适合长连接

---

## 12.7 本章小结

- Serverless 非常适合事件驱动场景
- S3 + Lambda 是经典组合
- API Gateway + Lambda 可直接作为后端
- 现代 AWS 项目大量使用 Serverless

---
