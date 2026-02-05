# 第一阶段：账号与权限（IAM）

> 学习目标：
>
> - 能安全进入 AWS，不再使用 root 账号进行日常操作
> - 理解 IAM 的核心模型（User / Group / Role / Policy）
> - 掌握最小权限原则与 MFA
> - 能独立完成一套“新手安全 IAM 初始化配置”

---

## 1. IAM 是什么？

IAM（Identity and Access Management）是 AWS 的身份与权限管理系统。

一句话理解：

**IAM 决定了：谁（Identity）能对哪些 AWS 资源（Resource）执行哪些操作（Action）。**

可以把 AWS 理解为一栋大楼：

- User / Role：谁进楼
- Resource：楼里的房间（EC2 / S3 / RDS）
- Policy：门禁规则

---

## 2. root 账号

### 2.1 root 是什么？

root 是 AWS 账号的最高权限拥有者，拥有不可限制的全部权限。

### 2.2 root 正确用法

root 账号 **只用于：**

- 初次账号初始化
- 账单与支付管理
- 极少数账号级操作

root 账号 **不用于：**

- 创建 EC2
- 管理 S3
- 日常运维

### 2.3 root 必做事项

- 开启 MFA（必须）
- 设置强密码
- 不与他人共享

---

## 3. IAM 的四大核心对象

本节是 IAM 学习中最重要的一部分。
如果这一节没讲清楚，后面 EC2、S3、RDS、CI/CD 都会反复混乱。

---

### 3.1 IAM User（用户）

#### 3.1.1 User 是什么？

IAM User 表示一个具体的身份，可以是：

- 一个真实的人（开发、测试、运维）
- 一个程序或脚本（自动化任务、CLI、SDK）

User 的本质是：
**一个可以被授权、被识别的主体（Principal）。**

---

#### 3.1.2 User 的两种使用方式

##### ① 控制台登录（Console）

- 使用 用户名 + 密码
- 面向人
- 可以开启 MFA

适用场景：

- 学习 AWS
- 日常运维
- 手工管理资源

##### ② API 调用（Access Key）

- 使用 Access Key ID + Secret Access Key
- 面向程序
- 风险较高，一旦泄露后果严重

适用场景：

- AWS CLI
- SDK 调用
- 本地脚本

新手建议：
**只在必要时使用 Access Key，在 AWS 内部运行的服务尽量使用 Role。**

---

#### 3.1.3 User 的常见错误（新人必看）

- 使用 root 账号当普通用户
- 给 User 直接绑定大量权限
- 将 Access Key 写入代码并上传 GitHub
- Access Key 长期不轮换

---

#### 3.1.4 一句话记住

**User = 人或程序的身份（工牌）。**

---

### 3.2 IAM Group（用户组）

#### 3.2.1 Group 是什么？

Group 是一组 User 的集合，用于统一管理权限。

它解决的问题是：
“多人做同一类工作，如何统一授权？”

---

#### 3.2.2 为什么权限要给 Group？

把权限给 Group，而不是直接给 User，有三个好处：

1. 一致性：同岗位权限一致
2. 易维护：人员变动只需进出组
3. 可审计：清楚知道谁有什么权限

最佳实践：
**权限跟岗位走，岗位用 Group 表达。**

---

#### 3.2.3 常见 Group 设计示例

- Admins：管理员
- Developers：开发人员
- ReadOnly：只读用户

---

#### 3.2.4 Group 常见错误

- 所有人放进同一个 Group
- 给 Developers AdministratorAccess
- 用户加入多个冲突的 Group

---

#### 3.2.5 一句话记住

**Group = 岗位/部门的权限模板。**

---

### 3.3 IAM Policy（权限策略）

#### 3.3.1 Policy 是什么？

Policy 是一份 JSON 格式的权限规则说明书。

---

#### 3.3.2 Policy 示例

```json
{
  "Effect": "Allow",
  "Action": ["s3:GetObject"],
  "Resource": ["arn:aws:s3:::example-bucket/*"]
}
```

解释：

- Effect：Allow / Deny
- Action：允许的 API 操作
- Resource：作用的资源范围

---

#### 3.3.3 IAM 权限核心规则

1. 默认拒绝
2. Deny 永远优先
3. 权限是叠加计算的

---

#### 3.3.4 什么时候需要自定义 Policy？

- 只访问特定 bucket
- 限定路径或条件
- 精细化权限控制

---

#### 3.3.5 一句话记住

**Policy = 权限规则说明书。**

---

### 3.4 IAM Role（角色，新手重点）

#### 3.4.1 Role 是什么？

Role 是一种可被扮演（Assume）的身份，不长期属于某一个人。

---

#### 3.4.2 为什么服务要用 Role？

- 不需要保存 Access Key
- 权限可随时回收
- 更安全

---

#### 3.4.3 Role 的两部分

1. Trust Policy：谁可以扮演
2. Permission Policy：扮演后能做什么

---

#### 3.4.4 Role 常见误区

- 把 Role 当成 User
- 忘记配置 Trust Policy
- 给 Role 过大权限

---

#### 3.4.5 一句话记住

**人登录控制台用 User，AWS 服务访问 AWS 用 Role。**

---

## 本节总结

- User：身份
- Group：岗位权限
- Policy：权限规则
- Role：服务间授权

---

## 4. 最小权限原则

最小权限原则：
只给完成当前任务所需的最小权限。

错误示例：

- 给开发者 AdministratorAccess

正确做法：

- 从只读权限开始
- 按需追加写权限

---

## 5. MFA（多因素认证）

建议开启 MFA 的对象：

- root（必须）
- 管理员用户
- 重要业务用户

即使密码泄露，没有动态验证码也无法登录。

---

## 6. 新人常见 6 个坑

1. 使用 root 账号日常操作
2. 给所有用户管理员权限
3. Access Key 写入代码仓库
4. 不轮换 Access Key
5. 不开启 MFA
6. 不使用 Group 管理权限

---

## 7. 实战：新手标准 IAM 初始化

### 7.1 root 初始化

1. 登录 root
2. 开启 MFA
3. 创建 Group：Developers
4. 绑定策略：AmazonS3ReadOnlyAccess

### 7.2 创建 IAM 用户

1. 创建用户 dev-user
2. 启用控制台访问
3. 加入 Developers 组
4. 登录测试权限

### 7.3 为 IAM 用户开启 MFA

进入 Security credentials → MFA

---

## 8. Role 初体验：EC2 访问 S3

### 步骤

1. 创建 Role（可信实体：EC2）
2. 绑定 AmazonS3ReadOnlyAccess
3. 将 Role 绑定到 EC2

无需 Access Key，EC2 即可访问 S3。

---

## 9. 学完检查清单

你应该能够回答：

- root 为什么不能日常使用？
- User / Group / Role 区别？
- Policy 的三要素？
- 为什么要最小权限？
- EC2 为什么用 Role？

---

## 本章总结

- IAM 是 AWS 安全的入口
- root 只用于初始化
- Group 管理权限，Role 授权服务
- 最小权限 + MFA 是底线
