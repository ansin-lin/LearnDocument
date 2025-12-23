# SSH 登录服务器

> 本节目标：让零基础学员理解 SSH 是什么、如何登录 EC2 服务器、为什么需要密钥、不同系统如何操作，以及常见错误的解决方法。

---

## SSH 是什么？

SSH（Secure Shell）是一种**安全的远程登录方式**。  
它可以让你在自己的电脑上，远程登录 AWS EC2 服务器，就像坐在服务器前面一样操作。

SSH 的特点：

- 通信加密，安全性高  
- 使用密钥而不是密码  
- 是管理服务器最常用的方式  

可以理解为：

> “你拿着一把安全的钥匙，远程打开 EC2 服务器的大门。”

---

## SSH 登录原理（通俗解释）

SSH 使用**一对钥匙**：

- 私钥（你保存）
- 公钥（AWS 保存）

你登录 EC2 时：

1. 你发送“私钥签名”
2. EC2 用公钥验证
3. 两把钥匙匹配 → 放你进来

📌 服务器从不需要你的私钥  
📌 若别人拿到你的私钥 = 拿到服务器访问权（非常危险）

---

## SSH 登录 EC2 的完整流程

---

## 1. 创建 EC2 时获取密钥（.pem）

在 Launch EC2 Instance 时：

- 选择 **Create new key pair**
- 下载 `.pem` 文件
- 妥善保存（AWS 不会提供第二次下载）

这是你登录 EC2 的“钥匙”。

---

## 2. 获取 EC2 公网 IP

在 EC2 控制台查看：

```bash
Public IPv4 address: 18.xxx.xxx.xxx
```

这是 SSH 登录的目标地址。

---

## 3. Windows 登录方式（PowerShell / Windows Terminal，推荐）

Windows 10/11 已内置 SSH，可直接用。

### 步骤 1：将密钥放在安全位置

例如：

```text
C:\keys\mykey.pem
```

### 步骤 2：给私钥设置正确权限（必须）

```powershell
icacls C:\keys\mykey.pem /inheritance:r
icacls C:\keys\mykey.pem /grant:r "$($env:USERNAME):(R)"
```

### 步骤 3：执行 SSH 登录命令

```powershell
ssh -i C:\keys\mykey.pem ec2-user@EC2_PUBLIC_IP
```

示例：

```powershell
ssh -i C:\keys\mykey.pem ec2-user@18.180.xxx.xxx
```

登录成功会看到：

```bash
[ec2-user@ip-xxx ~]$
```

---

## 4. macOS / Linux 登录方式

macOS 和 Linux 自带 SSH。

### 步骤 1：设置私钥权限

```bash
chmod 400 mykey.pem
```

### 步骤 2：登录

```bash
ssh -i mykey.pem ec2-user@EC2_PUBLIC_IP
```

示例：

```bash
ssh -i mykey.pem ec2-user@18.180.xxx.xxx
```

---

## 5. 不同系统使用的默认用户名（常见问题）

| 系统 | 默认 SSH 用户名 |
|------|------------------|
| Amazon Linux | ec2-user |
| Ubuntu | ubuntu |
| CentOS | centos |
| Debian | admin 或 debian |
| RHEL | ec2-user 或 root（较少） |

登录用户名错了会导致：

```bash
Permission denied (publickey)
```

---

## 6. 登录后可以做的事

- 安装软件（yum / apt）
- 查看日志
- 上传/下载文件
- 部署项目
- 配置 Nginx / Docker
- 管理服务器进程

SSH 是服务器管理的核心技能。

---

## 7. SSH 登录常见错误与解决方法

---

### 错误 1：Permission denied (publickey)

原因：

- 用错用户名
- 私钥权限不正确
- 私钥和 EC2 不匹配

解决：

```bash
chmod 400 mykey.pem
```

或检查用户名（如 ubuntu/ec2-user）

---

### 错误 2：Connection timed out（超时）

原因：

- 安全组没有开放端口 22
- 子网没有 IGW（不是公网子网）
- 实例没有公网 IP

解决检查：

- Security Group → 入站允许 SSH(22)
- 路由表存在：

```text
0.0.0.0/0 → igw-xxxx
```

---

### 错误 3：Operation not permitted（Windows 权限）

Windows 私钥权限不正确。

解决：

```powershell
icacls mykey.pem /inheritance:r
icacls mykey.pem /grant:r "$($env:USERNAME):(R)"
```

---

## 最简 SSH 登录流程总结（适合放在教学 PPT）

### 步骤 1  

创建 EC2 时下载密钥：`mykey.pem`

### 步骤 2  

在控制台查看 EC2 公网 IP

### 步骤 3  

执行登录命令：

#### Windows

```powershell
ssh -i C:\keys\mykey.pem ec2-user@EC2_PUBLIC_IP
```

#### macOS / Linux

```bash
ssh -i mykey.pem ec2-user@EC2_PUBLIC_IP
```

### 步骤 4  

看到 `[ec2-user@ip-xxx ~]$` → 登录成功！

---

如果你需要，我可以继续整理：

- 《Nginx 安装与部署网站》
- 《EC2 安装常用软件》
- 《Linux 入门命令教程》
- 《SSH 公钥登录 / SFTP 文件上传》

告诉我下一步需要哪一节？
