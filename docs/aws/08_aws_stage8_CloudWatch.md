# 第八章：CloudWatch（监控、告警与自动化依据）

> 本章目标：  
> 在已经完成 EC2、ALB、Auto Scaling 架构的基础上，  
> 系统性掌握 CloudWatch 的监控、告警与配置方法，  
> 理解 AWS 如何通过监控数据驱动自动化运维与扩缩容。

---

## 8.1 为什么一定要有监控？

在前面的章节中，我们已经构建了一个完整的 Web 架构：

前端（S3 + CloudFront）  
→ ALB  
→ Auto Scaling Group  
→ EC2（Spring Boot）  
→ RDS  

但如果没有监控，就会出现：

- CPU 已经 100%，却无人察觉  
- 服务变慢，用户先发现问题  
- 无法判断是否需要扩容  
- 故障发生后无法快速定位  

结论是：

**没有监控的系统，只是“能跑”，谈不上稳定。**

---

## 8.2 CloudWatch 是什么？

CloudWatch 是 AWS 提供的统一监控与日志平台，主要负责三件事：

1. 采集系统指标（Metrics）  
2. 集中管理日志（Logs）  
3. 基于指标触发告警（Alarms）  

一句话理解：

**CloudWatch = 系统的眼睛 + 黑匣子 + 报警器**

---

## 8.3 CloudWatch 的三大核心概念

### 8.3.1 Metrics（指标）

Metrics 是按时间持续采集的数值型数据，例如：

- CPU 使用率  
- 网络流量  
- 请求数量  

Auto Scaling 的扩缩容判断依据，正是 Metrics。

---

### 8.3.2 Logs（日志）

Logs 是系统运行时产生的文本信息，例如：

- Spring Boot 应用日志  
- Nginx 访问日志  
- 错误堆栈信息  

日志主要用于问题排查，而不是扩容判断。

---

### 8.3.3 Alarms（告警）

Alarm 是对指标的条件判断，例如：

如果 CPU > 70%，持续 5 分钟 → 触发告警。

告警可以触发：

- Auto Scaling 扩缩容  
- 通知运维人员  
- 自动化响应动作  

---

## 8.4 EC2 默认监控指标

EC2 实例无需任何配置，就会自动上报：

- CPUUtilization  
- NetworkIn / NetworkOut  
- DiskRead / DiskWrite  

学习阶段重点关注 **CPUUtilization** 即可。

---

## 8.5 查看 EC2 监控指标（实操）

步骤：

1. 打开 AWS 控制台  
2. 进入 EC2 → 选择实例  
3. 点击「Monitoring」标签  

你可以看到 CPU、网络等指标的变化曲线。

---

## 8.6 创建 CloudWatch 告警（实战）

### 8.6.1 创建 CPU 扩容告警

1. 进入 CloudWatch → Alarms  
2. 点击「Create alarm」  
3. 选择 EC2 / Auto Scaling 的 CPUUtilization 指标  
4. 设置条件：  
   - Average CPU > 70%  
   - 持续 5 分钟  
5. 选择动作：触发 Auto Scaling 扩容  
6. 命名并创建告警  

---

## 8.7 CloudWatch 与 Auto Scaling 的联动机制

完整自动扩容流程：

EC2 CPU 升高  
→ CloudWatch 采集指标  
→ Alarm 触发  
→ Scaling Policy 生效  
→ ASG 创建新 EC2  
→ 新实例加入 Target Group  
→ ALB 开始分发请求  

---

## 8.8 ALB 的常见监控指标（了解）

- RequestCount：请求数量  
- TargetResponseTime：后端响应时间  
- HTTPCode_ELB_5XX：错误数  

进阶阶段可用 RequestCount 作为扩容依据。

---

## 8.9 CloudWatch Logs 的作用

推荐将应用日志集中到 CloudWatch Logs，用于：

- 快速定位异常  
- 集中查看日志  
- 排查生产问题  

---

## 8.10 新手常见误区

- 只配置 Auto Scaling，不配置告警  
- 以为 AWS 会自动判断扩容  
- 把所有问题寄希望于扩容  

正确理解是：

**Metrics 提供数据 → Alarm 做判断 → Auto Scaling 执行动作**

---

## 8.11 本章小结

- CloudWatch 是 AWS 自动化体系的基础  
- 告警是监控与扩缩容的桥梁  
- 没有监控，就谈不上高可用  

---

