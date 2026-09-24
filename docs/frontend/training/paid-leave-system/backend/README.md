# 有給休暇申請系统：Node.js API

这是 Vue 综合练习配套的后台参考程序。学员不需要先学习 Node.js 后端框架，只需要按步骤启动服务，再从 Vue 通过 Axios 调用 API。

后台使用 Node.js、Express 和 MySQL。它负责用户注册与登录、服务端校验、申请保存、首页汇总、受付番号生成、筛选和申请取消。旧 HTML/CSS/JavaScript 练习仍可单独使用浏览器存储运行；Vue 综合项目改为使用本 API。

第一次配置环境时，请按[《Node.js 后台程序启动手顺》](STARTUP_GUIDE.md)从环境检查、数据库初始化开始操作。

## 1. 环境要求

- Node.js `22.12.0` 或更高的 LTS 兼容版本；
- npm；
- MySQL 8.4 LTS；
- Vue 开发服务器固定地址 `http://localhost:5174`。

依赖版本已经固定在 `package-lock.json`。进入本目录后使用 `npm ci`，不要随意改用 `@latest`。

## 2. 初始化 MySQL

先以具有建库权限的管理员账号执行：

```bash
mysql -u root -p < db/01_schema.sql
mysql -u root -p paid_leave_training < db/02_seed.sql
```

本地培训环境直接使用 MySQL `root` 账号，不需要另外创建应用专用账号。`01_schema.sql` 会创建表和约束，`02_seed.sql` 只建立部门主数据。系统没有固定研修账号，启动后先通过注册页面创建用户。

## 3. 配置和启动

复制 `.env.example` 为 `.env`，再按本机 MySQL 环境填写数据库地址、端口、`root`账号和密码。同时把 `SESSION_SECRET` 换成至少 32 个字符的随机值。

```dotenv
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=paid_leave_training
DB_USER=root
DB_PASSWORD=本机MySQL的root密码
```

`DB_HOST`可填写 MySQL 所在主机的 IP 地址或主机名。如果本机`root`账号没有密码，可以写成`DB_PASSWORD=`。`.env`只保存在本机，不要提交到 Git。

```bash
npm ci
npm run dev
```

成功时终端显示：

```text
Paid Leave API started: http://localhost:3000
```

打开以下地址检查数据库连接：

```text
GET http://localhost:3000/api/health
```

预期响应：

```json
{
  "data": {
    "status": "ok"
  }
}
```

## 4. API 一览

所有请求和响应使用 JSON。登录后由 `HttpOnly` Cookie 保存会话，Vue 的 Axios 实例必须设置 `withCredentials: true`。

| 方法 | URL | 登录 | 用途 |
| --- | --- | --- | --- |
| `GET` | `/api/health` | 不需要 | 确认 API 与数据库可用 |
| `GET` | `/api/master/departments` | 不需要 | 取得注册页面的部门选项 |
| `POST` | `/api/users` | 不需要 | 注册用户并写入 MySQL |
| `POST` | `/api/auth/login` | 不需要 | 登录并建立会话 |
| `GET` | `/api/auth/me` | 不需要 | 检查会话；已登录时取得当前用户 |
| `POST` | `/api/auth/logout` | 需要 | 删除当前会话 |
| `GET` | `/api/dashboard` | 需要 | 取得员工信息和首页汇总 |
| `GET` | `/api/leave-applications` | 需要 | 取得当前用户申请并组合筛选 |
| `GET` | `/api/leave-applications/:id` | 需要 | 取得一条申请 |
| `POST` | `/api/leave-applications` | 需要 | 新增申请并生成受付番号 |
| `PATCH` | `/api/leave-applications/:id/cancel` | 需要 | 取消仍处于申請中的申请 |

列表接口支持以下可选 Query 参数。第22章只使用 `status` 和 `keyword`；`startDate`、`endDate` 留到第23章改修任务使用。

| 参数 | 可接受的值 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `status` | `pending`、`approved`、`returned`、`cancelled` | 不筛选 | 按申请状态筛选 |
| `keyword` | 最多 100 字符 | 不筛选 | 匹配受付番号或申请理由 |
| `startDate` | `YYYY-MM-DD` | 不限制 | 显示结束日不早于该日的申请 |
| `endDate` | `YYYY-MM-DD` | 不限制 | 显示开始日不晚于该日的申请 |

## 5. 主要请求示例

注册：

```http
POST /api/users
Content-Type: application/json

{
  "account": "training.user",
  "password": "training123",
  "passwordConfirmation": "training123",
  "name": "山田 太郎",
  "department": "development"
}
```

注册成功后，使用相同账号登录：

```http
POST /api/auth/login
Content-Type: application/json

{
  "account": "training.user",
  "password": "training123"
}
```

新增申请：

```http
POST /api/leave-applications
Content-Type: application/json

{
  "leaveType": "paid",
  "startDate": "2026-10-01",
  "endDate": "2026-10-02",
  "reason": "私用のため",
  "handoverStatus": "done",
  "note": ""
}
```

Vue 页面在提交处理中禁用按钮，避免新人练习中因连续点击重复发送请求。后台仍使用事务和受付番号唯一约束保证数据一致性。

本练习不实现审批人画面。新增申请时，后台会从`pending`、`approved`、`returned`中随机选择一个状态，三种状态的概率大约各为三分之一。状态由后台生成，前端不传入。

申请响应中的 `submittedAt`、`cancelledAt` 使用 ISO UTC 格式，例如：

```text
2026-09-18T03:15:20.123Z
```

API 不返回日文画面格式。Vue 在显示时使用 `Intl.DateTimeFormat('ja-JP', ...)` 转换，数据库与通信数据继续保留 UTC。

## 6. 验证随机申请状态

连续提交几条申请，再在一览页使用`pending`、`approved`和`returned`条件筛选。随机结果不保证三次申请一定分别得到三种状态；如果暂时没看到某一种，可继续提交测试申请。

随机得到`pending`的申请会在一览页显示取消按钮。取消后状态更新为`cancelled`；`approved`和`returned`申请不能取消。

## 7. 响应和错误格式

成功响应把主要结果放在 `data` 中。失败响应统一为：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "入力内容を確認してください",
    "details": [
      {
        "field": "endDate",
        "message": "終了日は開始日以降にしてください"
      }
    ]
  }
}
```

前端根据 HTTP 状态和 `error.code` 区分输入错误、未登录、业务冲突和系统错误，不要只显示“发生错误”。

## 8. Vue 对接要点

共用 Axios 实例至少需要：

```js
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
  timeout: 10000
})
```

前端 `.env.development`：

```text
VITE_API_BASE_URL=http://localhost:3000/api
```

前端与后台都使用 `localhost`，不要一边使用 `localhost`、另一边使用 `127.0.0.1`，否则 Cookie 和 CORS 行为可能与预期不同。

`VITE_` 变量会进入浏览器，不能存放数据库密码或 `SESSION_SECRET`。这些秘密只保存在后台 `.env` 中。

新增申请接口没有幂等请求标识。前端应在请求处理中禁用提交按钮，并且不能为 `POST /api/leave-applications` 配置自动重试。发生超时或断网时，先查询申请一览确认是否已经保存，再决定是否重新提交。

## 9. 检查命令

```bash
npm run check
npm test
npm audit --omit=dev
```

当前自动测试只覆盖日期工具。完整联调仍需启动 MySQL，依次验证注册、登录、申请提交、筛选、取消、未登录访问和请求处理中防止连续提交。

## 10. 培训边界

本程序可用于本地研修和前后端联调，不是生产系统。生产环境还需要 HTTPS、CSRF 对策、登录失败限流、密码策略、审计日志、权限角色、数据库备份、监控和正式部署设计。
