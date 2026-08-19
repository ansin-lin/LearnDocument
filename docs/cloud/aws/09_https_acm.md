
# 第九章：HTTPS 与 ACM（生产级安全入口）

> 本章目标：
>
> - 从原理层面理解 HTTPS 到底解决了什么问题
> - 明确 HTTPS、证书、ACM 三者之间的关系
> - 掌握在 AWS 架构中配置 HTTPS 的正确位置与方式
> - 理解前后端分离架构下 HTTPS 的真实落点

---

## 9.1 为什么生产环境必须使用 HTTPS？

HTTP 明文传输存在以下风险：

- 请求内容可被窃听
- 登录信息、Token、Cookie 不安全
- 数据可能被篡改
- 浏览器标记为“不安全”

**结论：**
> 只要是对外系统，HTTPS 是强制要求。

---

## 9.2 HTTPS 的本质

HTTPS = HTTP + TLS（SSL）

解决三大问题：

1. **数据加密**
2. **身份校验**
3. **数据完整性校验**

---

## 9.3 HTTPS、TLS、证书的关系

- HTTPS：通信方式
- TLS：加密协议
- 证书：身份与密钥载体

> 没有证书，就无法建立 HTTPS 通道

---

## 9.4 ACM（AWS Certificate Manager）是什么？

ACM 是 AWS 提供的证书管理服务，负责：

- 申请证书
- 管理证书
- 自动续期

ACM 本身不处理流量、不转发请求。

---

## 9.5 HTTPS 在 AWS 架构中的正确位置

推荐架构：

浏览器  
↓ HTTPS  
ALB（ACM 证书，TLS 终止）  
↓ HTTP（内网）  
EC2（Spring Boot）

HTTPS 应终止在 **ALB** 上，而不是 EC2。

---

## 9.6 使用 ACM 配置 HTTPS（实战）

### 9.6.1 申请证书

1. 进入 ACM 控制台
2. Request certificate
3. Public certificate
4. 填写域名（如 *.example.com）
5. 选择 DNS validation

### 9.6.2 DNS 验证

在 DNS 服务商处添加 ACM 提供的 CNAME 记录  
状态变为 **Issued** 即成功

---

## 9.7 在 ALB 上启用 HTTPS

- 新增 Listener：HTTPS / 443
- 绑定 ACM 证书
- 转发至 Target Group

### HTTP → HTTPS 重定向

80 端口配置 Redirect 到 HTTPS

---

## 9.8 前后端分离中的 HTTPS 使用

### 前端

```js
axios.defaults.baseURL = 'https://api.example.com'
```

### 后端

- 监听 HTTP（如 8080）
- 不配置证书

HTTPS 对后端透明

---

## 9.9 常见误区

- 把证书放在 EC2
- 每台 EC2 配一个证书
- 证书文件提交到 Git
- HTTPS 暴露后端端口

正确模式：

> 域名 + ACM → ALB → 后端服务

---

## 9.10 本章小结

- HTTPS 是生产系统基础能力
- ACM 负责证书生命周期
- ALB 是 HTTPS 承载点
- 前后端分离架构中 HTTPS 只存在于入口层
