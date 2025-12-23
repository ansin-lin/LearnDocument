# 第 1 章：AWS 核心基础服务

> 本章内容包含 AWS 入门必须掌握的 5 个核心服务：IAM、EC2、S3、RDS 和 VPC。所有概念均为 0 基础学员设计，配合类比和实战练习，让学习者从理解到动手部署逐步掌握 AWS 基本能力。

---

## 1.1 IAM（权限管理）

### IAM 是什么？

IAM 全称 **Identity and Access Management（身份与访问管理）**。

它就像 AWS 的“门禁管理系统”，用于：

- 控制 **谁**（用户）  
- 可以对 **哪些资源**（EC2、S3、RDS 等）  
- 执行 **哪些操作**（读、写、删除等）

---

### 用户、组、角色（Role）

#### IAM 用户（User）

- 表示“一个具体的人”或“一个程序脚本”
- 可以有登录用的密码（Console 登录）
- 可以生成 API 密钥（编程使用）

📌 类比：**拿工牌进入 AWS 的人**

---

#### IAM 组（Group）

- 用户组（比如“开发组”、“测试组”）
- 把权限给组里所有用户一次继承

📌 类比：**部门门禁权限统一授权**

---

#### IAM 角色（Role）

- 不是绑定给某一个用户，而是谁“扮演角色”就获得权限
- 常用于 EC2 等服务访问 S3、DynamoDB 等资源

类比：**“系统管理员”这种身份，谁临时穿上制服，谁就有权限**

---

### Policy 的结构与写法

IAM Policy 就是一份“权限说明书”，用 JSON 编写。

示例权限模板：

```json
{
  "Effect": "Allow",
  "Action": ["s3:GetObject"],
  "Resource": ["arn:aws:s3:::my-bucket/*"]
}
```

- `Effect`: Allow（允许）或 Deny（拒绝）
- `Action`: 允许的动作，如 `s3:GetObject`
- `Resource`: 作用的对象，用 ARN 表示资源范围

---

### 最小权限原则（Principle of Least Privilege）

- 用户应该 **只拥有其职责所需的最小权限**
- 禁止偷懒给开发者 `AdministratorAccess`
- 权限跟着职能走，不随意扩张

---

### MFA（多因素认证）

MFA = 多因素认证，建议对以下账户开启：

- root 账号（必须！）
- IAM 管理员账号

开启后登录时，需要：

1. 密码
2. 动态验证码（手机上的 MFA App 生成）

---

### 实战练习

1. 使用 root 账号登录  
2. 创建一个“开发者组”  
3. 添加 `AmazonS3ReadOnlyAccess` 权限  
4. 创建 IAM 用户 `dev-user` 加入该组  
5. 为 root 和 dev-user 开启 MFA  
6. 使用 dev-user 登录控制台并测试权限限制

---

## 1.2 EC2（计算服务）

### EC2 是什么？

EC2 = **弹性计算服务（Elastic Compute Cloud）**，我们常说它是：

> 一台“云上的 Linux / Windows 服务器”，随时创建、按需计费。

---

### 实例类型（t / m / c 系列）

- **t 系列**：适合学习测试、小流量网站（如 t2.micro）
- **m 系列**：通用计算能力，适合常规应用
- **c 系列**：计算增强型，适合高计算场景
- **r 系列**：内存增强型，适合数据库、缓存

新手学习时可使用 **t2.micro 或 t3.micro**

---

### AMI 与 EBS

#### AMI（镜像）

- 是 EC2 的系统“模板”
- 含默认系统、软件

常见 AMI：

- Amazon Linux
- Ubuntu
- Windows Server

#### EBS（弹性块存储）

- 就是 EC2 的 “硬盘”
- 可以挂载、多次快照备份

---

### 安全组（SG） vs NACL

| 项目 | 安全组（SG） | NACL |
|------|--------------|------|
| 作用范围 | 实例级别 | 子网级别 |
| 是否有状态 | 有状态 | 无状态 |
| 用于 | 开端口、控制访问 | 屏蔽 IP 段等高级控制 |

📌 新手：**只需掌握安全组即可**

---

### SSH 登录流程概述

1. 创建 EC2 并下载 `.pem` 密钥  
2. 查看 Public IP  
3. 命令行执行：

```bash
ssh -i mykey.pem ec2-user@EC2_PUBLIC_IP
```

---

### 部署 Nginx 示例

```bash
sudo yum update -y
sudo yum install -y nginx
sudo systemctl start nginx
```

浏览器访问：

```bash
http://EC2_PUBLIC_IP
```

---

### 实战练习

- 创建一台 EC2（t2.micro）
- SSH 登录
- 安装 Nginx 并修改首页内容

---

## 1.3 S3（对象存储）

### S3 是什么？

S3（Simple Storage Service）是 AWS 的对象存储。

适合保存：

- 静态资源（图片、CSS、静态站点）
- 数据备份
- 大规模文件（如视频、数据包）

---

### Bucket 是什么？

- S3 存储文件的顶级目录
- 名称全局唯一
- 文件作为 Object 存储在 Bucket 中

---

### 存储类型

| 类型 | 特点 | 用途 |
|------|------|------|
| 标准 | 高可用、速度快 | 网站图片 |
| IA | 低频访问 | 日志、备份 |
| Glacier | 极低成本，有恢复延时 | 历史归档 |

---

### 版本控制

开启后：

- 覆盖文件不会直接丢失
- 可以恢复历史版本

---

### 生命周期规则

- 可自动将老数据转为 Glacier
- 可自动删除过期文件

---

### 静态网站托管

流程：

1. 打开 Bucket 配置「静态网站托管」  
2. 上传 `index.html`  
3. 访问 S3 提供的 URL

---

### CloudFront 加速 S3

CloudFront 是 AWS 的 CDN 服务，用于：

- 给静态站点提供全球加速
- 将资源缓存到 Edge 节点，让用户访问更快

---

### 实战练习

- 部署静态站点于 S3  
- 打开 CloudFront 加速  
- 测试 CDN 加速效果

---

## 1.4 RDS（关系型数据库）

### RDS 是什么？

> AWS 托管的关系型数据库服务，支持 MySQL、PostgreSQL、Oracle 等

优点：

- 无需手动安装数据库软件
- 自动备份、故障恢复
- 可一键升级实例

---

### 实例配置与计费

- 实例规格（决定 CPU/内存）
- 存储（SSD 容量）
- 是否 Multi-AZ 部署（高可用）

---

### Multi-AZ 架构

- 替你部署一个主库 + 一个备用库
- 主库故障时进行自动切换

---

### 访问安全与权限

- RDS 一般**部署在私网子网**
- 使用安全组控制可访问的应用服务器

---

### 备份策略与快照

- 启动自动备份可支持“时间点恢复”
- 手动快照可克隆出副本用于测试

---

### 实战练习

- 创建 MySQL RDS  
- 从 EC2 使用 MySQL Client 连接  
- 执行 CRUD：

```sql
CREATE DATABASE demo;
USE demo;
CREATE TABLE users(id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50));
INSERT INTO users(name) VALUES ("Alice"), ("Bob");
SELECT * FROM users;
```

---

## 1.5 VPC 网络基础

### VPC 是什么？

> 你在 AWS 上的私有虚拟网络空间，所有资源都在这里。

---

### 公网子网 / 私网子网

- 公网子网：可访问互联网  
- 私网子网：内部服务，联网需 NAT

---

### IGW 与 NAT

- **IGW（Internet Gateway）**  
    - VPC 连接互联网的网关  
    - 公网子网必须有 IGW 才能与公网交互

- **NAT Gateway**  
    - 私网访问互联网的出口  
    - 外网不能主动访问私网

---

### 安全组与路由表

- **安全组** 控制实例访问权限（端口/IP）
- **路由表** 控制子网内流量走向（local / IGW / NAT）

---

### 实战练习：构建三层 VPC

```text
VPC
├── 公网子网：ALB、EC2
└── 私网子网：应用服务器（EC2）、数据库（RDS）
```

---

本章小结：

你学会了 AWS 开发最核心的 5 类服务：

- IAM：身份与权限
- EC2：计算资源
- S3：对象存储
- RDS：托管数据库
- VPC：网络基础

这些都是你构建云端项目的基础砖瓦。下一章，我们将学习 ELB、AutoScaling、CloudFront 等中级服务，构建高可用架构！
