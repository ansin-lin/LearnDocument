# 第 4 章：AWS 安全（必须讲）

## IAM 最小权限原则  

- 禁用 root 用户登录  
- 按需赋予 IAM 权限  

## Security Group 设计  

- 默认拒绝入站、允许出站  
- 限定来源 IP、端口范围  
- 实战演练：构建安全组模板  

## 如何避免 S3 泄露  

- 禁止公共访问  
- 使用 Bucket Policy 与 IAM 控制权限  
- Block Public Access 设置讲解  

## 如何保护 RDS  

- 放在私网子网  
- 使用 SG 限制指定后端服务访问  
- 开启加密（KMS）与自动备份  

## WAF（Web Application Firewall）  

- 防御 SQL 注入、XSS 攻击  
- 实战练习：部署 WAF 用于保护 API
