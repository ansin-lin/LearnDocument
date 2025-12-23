# 第 3 章：前后端分离项目部署指南（重点）

适用于 Vue + Spring Boot + MySQL 项目部署方案。

## 架构一：经典高可用方案（推荐）  

- 前端：S3 + CloudFront  
- 后端：EC2 + ALB  
- 数据库：RDS  
- 文件：S3  
- DNS：Route53  
- 实战练习：部署完整前后端应用  

## 架构二：自动伸缩方案  

- 使用 Auto Scaling + ALB 处理高峰流量  
- 动态扩容、健康检查配置  

## 架构三：Serverless 轻量化架构（免运维）  

- API Gateway + Lambda  
- DynamoDB 存储  
- 实战练习：构建无服务器架构
