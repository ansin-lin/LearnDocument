# 第三阶段：S3 对象存储（完整教学版）

> 本章定位：  
> 本章是 AWS 新手从“会用 EC2”迈向“前后端分离架构”的**关键转折点**。  
> 我们将从 **概念 → 成本模型 → 实战部署** 三个层次，系统理解 S3。

---

## 1. S3 是什么？

S3（Simple Storage Service）是 AWS 提供的 **对象存储服务（Object Storage）**。

但对新手来说，更重要的不是官方定义，而是这句话：

> **S3 是一个可以通过 HTTP 直接访问的、无限扩展的文件存储系统。**

它的职责只有一件事：

- **存文件**
- 并通过 URL 对外提供访问

---

## 2. S3 与 EC2 的本质区别

| 对比维度 | EC2 | S3 |
| --------- | ----- | ---- |
| 本质 | 云服务器 | 文件存储 |
| 是否运行程序 | 可以 | 不可以 |
| 是否有操作系统 | 有 | 没有 |
| 是否支持 SSH | 支持 | 不支持 |
| 扩容方式 | 手动 / 自动扩容实例 | 天然无限 |
| 典型用途 | 后端服务 | 前端 / 静态资源 |

📌 **关键认知一定要建立：**

> **EC2 是“计算资源”，S3 是“存储资源”，两者职责完全不同。**

---

## 3. Bucket 与 Object（S3 的基本组成）

### 3.1 Bucket（存储桶）

Bucket 是 S3 的**顶级容器**，所有文件必须放在某个 Bucket 中。

特点：

- 名称 **全球唯一**
- 通常一个项目对应一个 Bucket
- Bucket 名称会出现在访问 URL 中

示例：

```text
https://my-frontend-demo.s3.amazonaws.com/index.html
```

📌 Bucket 名称 ≈ 网站域名的一部分。

---

### 3.2 Object（对象）

在 S3 中：

- 每一个文件 = 一个 Object
- Object 包含：
    - 文件内容
    - Key（路径 + 文件名）
    - 元数据

S3 **没有目录结构**，你看到的“文件夹”只是 Key 的前缀。

---

## 4. S3 Storage Class（存储类型，成本核心）

一个 S3 Bucket 中，**可以同时存在多种存储类型的文件**。  
存储类型决定的是：

- 存储成本
- 访问频率
- 取回速度

可以理解为：

> **同一个仓库里，不同“货架等级”的选择。**

---

### 4.1 S3 Standard（标准存储，最重要）

特点：

- 高性能
- 高可用
- 访问延迟最低
- 成本相对最高

适合：

- 前端静态资源（HTML / CSS / JS）
- 图片、视频
- 正在被访问的数据

📌 **新手结论：前端项目一律使用 Standard。**

---

### 4.2 S3 Intelligent-Tiering（智能分层）

特点：

- AWS 自动判断访问频率
- 自动在不同层级之间切换
- 你不需要做决策

适合：

- 不确定访问频率的数据
- 希望省心的场景

---

### 4.3 S3 Standard-IA（低频访问）

特点：

- 存储费用低
- 每次访问会产生取回费用
- 访问速度仍为毫秒级

适合：

- 日志
- 历史数据
- 备份文件

---

### 4.4 S3 Glacier / Glacier Deep Archive

特点：

- 成本极低
- 取回慢（分钟到小时）

适合：

- 长期归档
- 法律 / 审计数据

📌 **不适合网站或前端资源。**

---

### 4.5 新手选型速查表

| 使用场景 | 推荐存储类型 |
| -------- | ------------- |
| 前端 HTML / Vue | Standard |
| 不确定访问频率 | Intelligent-Tiering |
| 日志 / 备份 | Standard-IA |
| 长期归档 | Glacier |

---

## 5. 实战一：HTML 页面部署到 S3

### 5.1 准备 HTML 文件

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>S3 Demo</title>
</head>
<body>
  <h1>Hello from S3</h1>
  <p>This page is hosted on Amazon S3.</p>
</body>
</html>
```

---

### 5.2 创建 Bucket

- Bucket name：`my-simple-html-demo`
- Region：东京
- 关闭 Block Public Access

---

### 5.3 上传文件并开启静态网站托管

- 上传 `index.html`
- Properties → Static website hosting
- Index document：`index.html`

---

### 5.4 配置 Bucket Policy

```json
{
  "Version": "2012-10-17", //这是 AWS 规定的 Policy 语法版本，照写即可
  "Statement": [  //包含 多条权限规则
    {
      "Effect": "Allow",  //表示允许还是拒绝（Allow/Deny）
      "Principal": "*",  //表示 “谁”可以访问
      "Action": "s3:GetObject",  //表示 允许执行的操作
      "Resource": "arn:aws:s3:::my-simple-html-demo/*"  //作用范围
    }
  ]
}
```

---

### 5.5 浏览器访问

```text
http://my-simple-html-demo.s3-website-ap-northeast-1.amazonaws.com
```

---

## 6. 实战二：Vue 项目部署到 S3（真实前端形态）

### 6.1 构建 Vue 项目

```bash
npm run build
```

生成：

```text
dist/
 ├─ index.html
 └─ assets/
```

---

### 6.2 上传构建产物

- 上传 dist 目录中的所有文件
- 不要上传 dist 文件夹本身

---

### 6.3 SPA 项目注意事项

- Static website hosting
- Index document：`index.html`
- Error document：`index.html`

---

### 6.4 访问 Vue 项目

```text
http://my-vue-frontend-demo.s3-website-ap-northeast-1.amazonaws.com
```

---

## 7. 本章核心总结（必须记住）

- S3 是对象存储，不是服务器
- 一个 Bucket 可使用多种存储类型
- Storage Class 是成本策略
- 前端项目天然适合 S3
- S3 是前后端分离的起点

---

## 8. 生命周期规则（Lifecycle Rules）

> **S3 不只是“存文件”，而是“会自动管理文件生命周期的存储系统”**

---

## 8.1 什么是生命周期规则？

**Lifecycle Rule（生命周期规则）** 是 S3 提供的一种 **自动化管理机制**，用于：

- 根据文件存放时间
- 自动切换存储类型
- 或自动删除文件

一句话理解：

> **文件放久了，S3 会自动帮你换地方或清理掉。**

---

## 8.2 为什么需要生命周期规则？

如果你只使用 S3 Standard：

- 很多文件上传后几乎不再访问
- 却依然占用最高成本的存储

没有生命周期规则意味着：

> **不用的文件，也一直在交最贵的租金。**

Lifecycle 的核心价值就是：

> **自动降低“冷数据”的长期存储成本。**

---

## 8.3 生命周期规则可以做哪些事？

生命周期规则主要有三类用途。

### 8.3.1 自动切换存储类型（最常用）

示例策略：

- 文件创建 → Standard
- 30 天后 → Standard-IA
- 180 天后 → Glacier
- 365 天后 → 删除

这是企业中**最常见的 S3 成本控制方案**。

---

### 8.3.2 自动删除过期文件

适用于：

- 日志文件
- 临时数据
- 历史构建产物

示例：

> 超过 90 天的文件自动删除。

---

### 8.3.3 针对版本文件的生命周期规则

当 Bucket 开启 **Versioning** 后：

- 每次覆盖文件都会生成新版本
- Lifecycle 可以：
    - 自动删除旧版本
    - 只保留最新版本
    - 防止版本无限增长

---

## 8.4 仓库类比理解生命周期规则（教学用）

可以这样理解：

> S3 就像一个仓库：  
> 新货放在最方便的地方，  
> 老货逐渐移到仓库深处，  
> 最终清理掉没人要的货物。

Lifecycle 规则 = **仓库管理员的自动调度规则**。

---

## 8.5 生命周期规则不是“实时执行”的

新手必须知道两点：

1. Lifecycle 不是秒级触发  
   - AWS 会定期批量执行  
2. 生命周期操作通常不可逆  
   - 例如转入 Glacier 后取回变慢

> Lifecycle 是 **长期策略**，不是即时操作。

---

## 8.6 学习阶段如何使用生命周期规则？

在学习阶段：

- 不强制要求实际配置
- 但必须建立正确的使用意识

推荐教学示例：

| 文件类型 | 生命周期建议 |
| -------- | -------------- |
| 前端静态资源 | 不配置（长期 Standard） |
| 日志 / 构建产物 | 30 天 → IA |
| 备份 | 90 天 → Glacier |
| 临时文件 | 7～30 天删除 |

---

## 8.7 生命周期规则与前端项目的关系

对于 **S3 托管前端项目（HTML / Vue / React）**：

- 不建议设置生命周期规则
- 不要转 IA 或 Glacier

原因：

- 前端资源访问频繁
- 转低频或归档会导致：
    - 访问变慢
    - 取回费用增加

一句话总结：

> **生命周期规则主要用于“数据型文件”，不是“网站资源”。**

---

## 8.8 新手最容易犯的错误

- 给前端资源配置 Glacier
- 自动删除重要文件
- 以为 Lifecycle 是必须配置的

事实是：

> **Lifecycle 是优化手段，不是必选配置。**

---

## 8.9 本节核心总结（教学金句）

> - Storage Class 决定现在花多少钱  
> - Lifecycle 决定未来花多少钱  
> - 会用 S3 的人，一定会用 Lifecycle  

---

## 8.10 本章整体收束

通过本节，你已经理解：

- S3 的长期成本如何控制
- 生命周期规则的真实价值
- 为什么企业级 S3 一定会配置 Lifecycle

这为后续学习 **CloudFront、RDS、数据架构设计** 打下基础。

> 下一章：CloudFront（CDN）  
> 解决“存得住”之后的“访问快不快”。
