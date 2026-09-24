# 附录：有給休暇申請系统 API 入出力规格

本附录是第 22 章综合练习使用的 API 契约。开发页面时，应以这里规定的 URL、字段名、状态码和错误码为准，不根据画面文字自行改变字段。

## 1. 共通规则

### 1.1 基本地址与请求格式

开发环境的 API 基本地址如下：

```text
http://localhost:3000/api
```

例如登录接口的完整 URL 是 `http://localhost:3000/api/auth/login`。除 URL 查询参数外，请求数据使用 JSON，并发送 `Content-Type: application/json`。

登录成功后，后台会设置名为 `paidLeaveSession` 的 `HttpOnly` Cookie。浏览器中的 JavaScript 不能读取这个 Cookie，但后续请求必须携带它。共用 Axios 实例应设置：

```js
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true
})
```

`.env` 中设置：

```dotenv
VITE_API_BASE_URL=http://localhost:3000/api
```

`withCredentials: true` 允许浏览器在跨端口请求中携带会话 Cookie。它不等于把密码保存在前端。密码只在注册或登录请求中发送，不应写入 Pinia、`localStorage`、日志或页面 URL。

### 1.2 输入位置

| 输入位置 | 写法示例 | 作用 |
| --- | --- | --- |
| Path 参数 | `/leave-applications/12` 中的 `12` | 指定一条申请记录 |
| Query 参数 | `/leave-applications?status=pending` | 筛选申请一览，不修改数据 |
| JSON Body | `{ "account": "training.user" }` | 向新增或登录接口发送数据 |
| Cookie | `paidLeaveSession` | 由浏览器自动携带，用于确认当前登录用户 |

“必填”表示发送该请求时必须提供。“可省略”表示不传也可以；不要为了凑齐字段发送 `null`。

### 1.3 正常响应格式

多数正常响应把结果放在 `data` 中：

```json
{
  "data": {
    "status": "ok"
  }
}
```

申请一览还会返回件数：

```json
{
  "data": [],
  "meta": {
    "count": 0
  }
}
```

退出接口成功时返回 `204 No Content`，没有 JSON 响应体。因此不要在退出成功后读取 `response.data.data`。

### 1.4 异常响应格式

一般异常返回 `error.code` 和 `error.message`：

```json
{
  "error": {
    "code": "AUTH_REQUIRED",
    "message": "ログインが必要です"
  }
}
```

输入校验失败时还会返回 `details`。`field` 是表单字段名，`message` 是应显示在该字段附近的消息：

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

| HTTP 状态码 | 共通错误码 | 含义与前端处理 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 输入格式或字段组合不正确；显示字段错误并保留输入 |
| `401` | `AUTH_REQUIRED` | 未登录、会话过期或用户已不可用；清除前端登录状态并转到登录页 |
| `404` | `NOT_FOUND` | 请求的 API URL 不存在；检查 URL 和请求方法 |
| `409` | `DUPLICATE_DATA` | 数据库中的唯一数据发生重复；不要自动重复提交 |
| `500` | `INTERNAL_ERROR` | 后台或数据库处理失败；显示统一系统错误，不把内部信息展示给用户 |

各业务接口还可能返回专用错误码，后续各节会分别列出。

### 1.5 日期和日期时间

- `startDate`、`endDate` 是没有时刻和时区的业务日期，格式为 `YYYY-MM-DD`，例如 `2026-09-21`。
- `submittedAt`、`cancelledAt` 是 ISO UTC 日期时间，例如 `2026-09-18T03:15:20.123Z`。末尾的 `Z` 表示 UTC。
- 前端保存 API 原值，需要显示时再转换为日本时间，不修改原始数据。

```js
export function formatDateTime(isoUtc) {
  if (!isoUtc) return '-'

  return new Intl.DateTimeFormat('ja-JP', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Asia/Tokyo'
  }).format(new Date(isoUtc))
}
```

示例中的 UTC 时间会显示为对应的日本日期时间。`cancelledAt` 为 `null` 时显示 `-`。

### 1.6 Cookie 与后台 Session 分别保存什么

本项目使用的是“服务器端 Session”。浏览器和后台不会各保存一份完整用户资料，而是通过 Session ID 建立对应关系。

```text
浏览器 Cookie 中的 Session ID
        ↓ 每次请求自动携带
后台读取 MySQL 的 user_sessions 表
        ↓ 从 Session 取得 userId
后台再按 userId 查询用户或申请数据
```

#### 浏览器 Cookie 保存的内容

登录成功后，响应头中会出现类似下面的内容。尖括号部分是示意值，每次登录生成的实际值都不同：

```http
Set-Cookie: paidLeaveSession=s%3A<session-id>.<signature>; Path=/; Expires=<UTC-date>; HttpOnly; SameSite=Lax
```

| 项目 | 示例或可接受的值 | 作用 |
| --- | --- | --- |
| Cookie 名称 | `paidLeaveSession` | 后续请求用这个名称携带会话标识 |
| Cookie 值 | 签名后的 Session ID | 用于查找后台 Session；不是用户 ID，也不包含用户资料 |
| `Path` | `/` | 该站点下的请求都可以携带此 Cookie |
| `Expires` / `Max-Age` | 登录或最近一次访问后约 30 分钟 | 控制浏览器中的 Cookie 有效期 |
| `HttpOnly` | 启用 | `document.cookie` 不能读取，降低脚本窃取会话的风险 |
| `SameSite` | `Lax` | 限制跨站请求携带 Cookie |
| `Secure` | 生产环境启用 | 生产环境只通过 HTTPS 发送 Cookie；本地 HTTP 开发环境不启用 |

Cookie 中不保存以下内容：

- 密码或密码哈希；
- `userId`、社员番号、姓名和部门；
- 申请记录、剩余日数或页面状态。

`HttpOnly` 表示页面 JavaScript 不能读取 Cookie，但浏览器开发者工具的 Application（应用）面板仍可供开发者确认 Cookie 是否存在。前端代码不需要也不应该手动取得这个值。

#### MySQL Session 保存的内容

后台把 Session 保存到 `user_sessions` 表：

| 数据库字段 | 示例 | 作用 |
| --- | --- | --- |
| `session_id` | `<random-session-id>` | 与 Cookie 中的 Session ID 对应，是该表的主键 |
| `expires_at` | `1789704000000` | 到期时间的 Unix 毫秒数；本示例对应 `2026-09-18T04:00:00.000Z`，过期记录不能恢复登录状态 |
| `session_data` | JSON 字符串 | 保存该 Session 的数据和 Cookie 到期配置 |

登录成功后，`session_data` 的内容可以理解为下面的结构。具体日期和内部字段由 `express-session` 生成：

```json
{
  "cookie": {
    "originalMaxAge": 1800000,
    "expires": "2026-09-18T04:00:00.000Z",
    "secure": false,
    "httpOnly": true,
    "path": "/",
    "sameSite": "lax"
  },
  "userId": 1
}
```

本项目放入 Session 的业务数据只有 `userId`。后台收到请求后使用 `userId` 查询最新的用户和申请数据，因此姓名、部门或权限变化后不需要改写 Cookie。Session 中也不保存密码。

#### Session 是否会返回给前端

Session JSON 不会作为 API 响应体返回给前端。登录成功时，前端会同时得到两个不同位置的输出：

1. HTTP 响应体中的当前用户对象，供 Vue 立即显示并保存到 Pinia；
2. `Set-Cookie` 响应头，浏览器自动保存其中的 Session ID。

以后发送 `GET /api/auth/me` 等请求时，浏览器通过 `Cookie` 请求头自动携带会话 Cookie：

```http
Cookie: paidLeaveSession=s%3A<session-id>.<signature>
```

后台验证签名并读取对应 Session，然后根据其中的 `userId` 返回当前用户。前端刷新后 Pinia 内存会消失，但只要 Cookie 和后台 Session 都未过期，就可以通过 `/api/auth/me` 重新取得用户。

本项目启用了滚动过期：登录后约 30 分钟没有继续访问，Cookie 和 Session 会过期；持续正常访问时，到期时间会向后延长。浏览器有 Cookie 但数据库 Session 已过期或被删除时，仍视为未登录。

## 2. 共通输出对象

### 2.1 当前用户对象

登录和当前用户接口返回相同结构：

| 字段 | 类型 | 示例 | 含义 |
| --- | --- | --- | --- |
| `employeeNumber` | string | `EMP-00001` | 后台生成的社员番号 |
| `account` | string | `training.user` | 登录账号 |
| `name` | string | `山田 太郎` | 姓名 |
| `department` | string | `development` | 部门代码，供程序判断 |
| `departmentName` | string | `システム開発部` | 部门显示名称 |

### 2.2 申请对象

新增、详情和取消接口返回一个申请对象；一览接口返回该对象的数组。

| 字段 | 类型 | 示例 | 含义 |
| --- | --- | --- | --- |
| `id` | number | `12` | 数据库中的申请 ID |
| `receiptNumber` | string | `REQ-20260918-001` | 后台生成的受付番号 |
| `leaveType` | string | `paid` | 休假类型代码 |
| `startDate` | string | `2026-09-21` | 开始日期，`YYYY-MM-DD` |
| `endDate` | string | `2026-09-22` | 结束日期，`YYYY-MM-DD` |
| `leaveDays` | number | `2` | 后台计算的申请日数；半日为 `0.5` |
| `reason` | string | `私用のため` | 申请理由 |
| `handoverStatus` | string | `done` | 引继状态 |
| `note` | string | `連絡先は携帯電話` | 备注；没有内容时为空字符串 |
| `status` | string | `approved` | 申请状态 |
| `submittedAt` | string | `2026-09-18T03:15:20.123Z` | 提交时刻，ISO UTC |
| `cancelledAt` | string 或 null | `null` | 取消时刻；未取消时为 `null` |

各代码值如下：

| 分类 | 可接受的值 | 显示含义 |
| --- | --- | --- |
| `leaveType` | `paid`、`half-am`、`half-pm`、`special` | 有給休暇、午前半休、午後半休、特別休暇 |
| `handoverStatus` | `done`、`not-required` | 引継済、引継不要 |
| `status` | `pending`、`approved`、`returned`、`cancelled` | 申請中、承認済、差戻し、取消済 |

完整示例：

```json
{
  "id": 12,
  "receiptNumber": "REQ-20260918-001",
  "leaveType": "paid",
  "startDate": "2026-09-21",
  "endDate": "2026-09-22",
  "leaveDays": 2,
  "reason": "私用のため",
  "handoverStatus": "done",
  "note": "",
  "status": "approved",
  "submittedAt": "2026-09-18T03:15:20.123Z",
  "cancelledAt": null
}
```

## 3. 运行确认 API

### 3.1 `GET /api/health`

确认后台进程和数据库连接是否正常。此接口不要求登录，也没有 Path、Query 或 Body 输入。

正常响应：`200 OK`

```json
{
  "data": {
    "status": "ok"
  }
}
```

| 异常状态 | 错误码 | 发生条件 |
| --- | --- | --- |
| `500` | `INTERNAL_ERROR` | 后台无法访问 MySQL 等系统错误 |

如果浏览器完全没有收到 HTTP 响应，应先确认后台是否启动、URL 是否正确，而不是按 `500` 处理。

## 4. 部门选项 API

### 4.1 `GET /api/master/departments`

取得注册表单使用的部门选项。此接口不要求登录，也没有 Path、Query 或 Body 输入。

正常响应：`200 OK`

```json
{
  "data": [
    {
      "value": "development",
      "label": "システム開発部"
    },
    {
      "value": "quality",
      "label": "品質管理部"
    }
  ]
}
```

`value` 用作注册请求的 `department`，`label` 用于画面显示。前端不要把显示名称作为部门代码发送。

| 异常状态 | 错误码 | 发生条件 |
| --- | --- | --- |
| `500` | `INTERNAL_ERROR` | 部门数据读取失败 |

## 5. 用户注册 API

### 5.1 `POST /api/users`

把新用户保存到 MySQL。此接口不要求登录。

JSON Body：

| 字段 | 必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `account` | 是 | 4～20 个半角小写英数字或 `.`、`_`、`-` | 登录账号；后台会去除两端空白并转换为小写 |
| `password` | 是 | 8～32 个字符 | 登录密码 |
| `passwordConfirmation` | 是 | 必须与 `password` 相同 | 确认密码 |
| `name` | 是 | 去除两端空白后 2～40 个字符 | 姓名 |
| `department` | 是 | `development`、`quality`、`sales`、`general-affairs`、`human-resources` | 部门代码 |

请求示例：

```json
{
  "account": "training.user",
  "password": "Training123!",
  "passwordConfirmation": "Training123!",
  "name": "山田 太郎",
  "department": "development"
}
```

正常响应：`201 Created`

```json
{
  "data": {
    "employeeNumber": "EMP-00001",
    "account": "training.user",
    "name": "山田 太郎",
    "department": "development"
  }
}
```

注册响应不会返回密码、确认密码或密码哈希。

| 异常状态 | 错误码 | 发生条件 | 前端处理 |
| --- | --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 缺少字段、格式不正确或两次密码不一致 | 把 `details` 显示到相应字段附近 |
| `409` | `ACCOUNT_ALREADY_EXISTS` | 账号已经存在 | 提示用户更换账号 |
| `409` | `DUPLICATE_DATA` | 并发注册等原因造成唯一数据重复 | 显示重复数据错误，不自动重试 |
| `500` | `INTERNAL_ERROR` | 数据库或后台处理失败 | 显示统一系统错误 |

## 6. 登录与会话 API

### 6.1 `POST /api/auth/login`

校验账号和密码，成功后建立后台 Session，并通过响应设置 `paidLeaveSession` Cookie。

JSON Body：

| 字段 | 必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `account` | 是 | 去除空白后 4～20 个字符 | 登录账号；后台转换为小写 |
| `password` | 是 | 8～32 个字符 | 登录密码 |

请求示例：

```json
{
  "account": "training.user",
  "password": "Training123!"
}
```

正常响应：`200 OK`。响应体返回当前用户，响应头通过 `Set-Cookie` 设置会话 Cookie；两者不是同一份数据。

```json
{
  "data": {
    "employeeNumber": "EMP-00001",
    "account": "training.user",
    "name": "山田 太郎",
    "department": "development",
    "departmentName": "システム開発部"
  }
}
```

响应头示例：

```http
Set-Cookie: paidLeaveSession=s%3A<session-id>.<signature>; Path=/; Expires=<UTC-date>; HttpOnly; SameSite=Lax
```

浏览器保存 Cookie 后，前端只需要把响应体中的用户对象放入 `auth` Store。不要把 Cookie 值或密码放入 Store。

| 异常状态 | 错误码 | 发生条件 | 前端处理 |
| --- | --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 账号或密码长度不符合要求 | 显示字段错误 |
| `401` | `INVALID_CREDENTIALS` | 账号不存在、账号无效或密码错误 | 显示同一条登录失败消息，不区分账号和密码 |
| `500` | `INTERNAL_ERROR` | 数据库或后台处理失败 | 显示统一系统错误 |

### 6.2 `GET /api/auth/me`

根据 Cookie 中的会话取得当前用户。页面首次打开或刷新后，`auth` Store 可以调用此接口检查和恢复登录状态。此接口本身不要求已登录。

输入：没有 Path、Query 或 Body。如果浏览器已有`paidLeaveSession` Cookie，会自动携带；首次进入时可以没有 Cookie。

已登录时返回`200 OK`，`data`是第2.1节定义的当前用户对象。没有 Cookie、Session 已过期或用户已不可用时，仍返回`200 OK`，但`data`为`null`：

```json
{
  "data": null
}
```

这是正常的“未登录”状态，不是接口异常。首次进入页面时不会因为没有 Cookie 而在浏览器 Console 中产生`401`错误。

| 异常状态 | 错误码 | 发生条件 |
| --- | --- | --- |
| `500` | `INTERNAL_ERROR` | 数据库或后台处理失败 |

### 6.3 `POST /api/auth/logout`

删除后台 Session 并清除 Cookie。没有 JSON Body，但必须携带有效会话。

正常响应：`204 No Content`，没有响应体。后台会删除 `user_sessions` 中对应记录，并通过响应头让浏览器清除 `paidLeaveSession` Cookie。成功后前端再清除 Pinia 中的当前用户、申请草稿和页面状态。

| 异常状态 | 错误码 | 发生条件 |
| --- | --- | --- |
| `401` | `AUTH_REQUIRED` | 没有有效登录会话 |
| `500` | `INTERNAL_ERROR` | Session 删除失败 |

## 7. 首页汇总 API

### 7.1 `GET /api/dashboard`

取得首页显示的员工资料、剩余日数和申请汇总。没有 Path、Query 或 Body；必须携带有效会话。

正常响应：`200 OK`

```json
{
  "data": {
    "employeeNumber": "EMP-00001",
    "name": "山田 太郎",
    "department": "development",
    "departmentName": "システム開発部",
    "remainingPaidLeaveDays": 12,
    "pendingCount": 0,
    "approvedDaysThisMonth": 0
  }
}
```

| 输出字段 | 类型 | 含义 |
| --- | --- | --- |
| `remainingPaidLeaveDays` | number | 初始有给日数减去已经批准的有给、上午半休和下午半休 |
| `pendingCount` | number | 当前用户处于 `pending` 的申请件数 |
| `approvedDaysThisMonth` | number | 日本时间本月内提交且已经批准的申请日数 |

| 异常状态 | 错误码 | 发生条件 |
| --- | --- | --- |
| `401` | `AUTH_REQUIRED` | 没有有效会话，或会话对应用户已不可用 |
| `500` | `INTERNAL_ERROR` | 汇总查询失败 |

## 8. 申请一览 API

### 8.1 `GET /api/leave-applications`

取得当前登录用户的申请。必须携带有效会话。所有 Query 参数都可以省略。

Query 参数：

| 参数 | 必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `status` | 否 | `pending`、`approved`、`returned`、`cancelled` | 只取得指定状态 |
| `keyword` | 否 | 最多 100 个字符 | 在受付番号和理由中进行部分匹配 |
| `startDate` | 否 | `YYYY-MM-DD` 或空字符串 | 取得结束日期不早于该日期的申请 |
| `endDate` | 否 | `YYYY-MM-DD` 或空字符串 | 取得开始日期不晚于该日期的申请 |

日期条件表示“与指定期间有重叠”。同时发送两项时，`endDate` 不得早于 `startDate`。第 22 章主线只要求状态和关键字；日期范围用于第 23 章改修任务。

请求示例：

```text
GET /api/leave-applications?status=pending&keyword=REQ-202609
```

正常响应：`200 OK`

```json
{
  "data": [
    {
      "id": 12,
      "receiptNumber": "REQ-20260918-001",
      "leaveType": "paid",
      "startDate": "2026-09-21",
      "endDate": "2026-09-22",
      "leaveDays": 2,
      "reason": "私用のため",
      "handoverStatus": "done",
      "note": "",
      "status": "pending",
      "submittedAt": "2026-09-18T03:15:20.123Z",
      "cancelledAt": null
    }
  ],
  "meta": {
    "count": 1
  }
}
```

没有符合条件的数据也是成功响应：`data` 为 `[]`，`meta.count` 为 `0`。前端应显示“没有申请”或“筛选无结果”，不能当作异常。

| 异常状态 | 错误码 | 发生条件 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 状态值、日期格式、长度或日期先后关系不正确 |
| `401` | `AUTH_REQUIRED` | 没有有效登录会话 |
| `500` | `INTERNAL_ERROR` | 查询失败 |

## 9. 申请详情 API

### 9.1 `GET /api/leave-applications/:id`

取得当前用户的一条申请，用于完成页刷新后的重新读取。

Path 参数：

| 参数 | 必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `id` | 是 | 大于 `0` 的整数 | 指定申请 ID |

请求示例：

```text
GET /api/leave-applications/12
```

正常响应：`200 OK`，`data` 是第2.2节定义的申请对象。

| 异常状态 | 错误码 | 发生条件 | 前端处理 |
| --- | --- | --- | --- |
| `400` | `VALIDATION_ERROR` | ID 不是正整数 | 显示无法读取并检查路由参数 |
| `401` | `AUTH_REQUIRED` | 没有有效登录会话 | 转到登录页 |
| `404` | `APPLICATION_NOT_FOUND` | 申请不存在，或申请不属于当前用户 | 显示找不到申请；不能尝试读取其他用户数据 |
| `500` | `INTERNAL_ERROR` | 查询失败 | 显示统一系统错误 |

## 10. 新增申请 API

### 10.1 `POST /api/leave-applications`

校验申请内容、计算日数、生成受付番号，随机决定审批结果后把申请保存到 MySQL。必须携带有效会话。

JSON Body：

| 字段 | 必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `leaveType` | 是 | `paid`、`half-am`、`half-pm`、`special` | 休假类型 |
| `startDate` | 是 | 有效的 `YYYY-MM-DD` | 开始日期，必须是日本时间的今天或以后 |
| `endDate` | 是 | 有效的 `YYYY-MM-DD` | 结束日期，不得早于开始日期 |
| `reason` | 是 | 去除两端空白后 1～200 个字符 | 申请理由 |
| `handoverStatus` | 是 | `done`、`not-required` | 引继状态 |
| `note` | 否 | 最多 300 个字符；默认空字符串 | 备注；特別休暇时必须填写制度名 |

补充规则：

- `half-am` 和 `half-pm` 的开始、结束日期必须相同，申请日数为 `0.5`。
- 其他类型按开始至结束的自然日数计算，开始和结束当天都计入。
- 申请日数不能超过剩余有给日数。
- `leaveDays`、`receiptNumber`、`status` 和提交时间均由后台生成，前端不得发送这些字段代替后台计算。
- 本练习没有审批人画面，所以新增时由后台从`pending`、`approved`、`returned`中随机选择一个`status`，概率大约各为三分之一。随机结果只用于培训项目模拟业务状态。

请求示例：

```json
{
  "leaveType": "paid",
  "startDate": "2026-09-21",
  "endDate": "2026-09-22",
  "reason": "私用のため",
  "handoverStatus": "done",
  "note": ""
}
```

正常响应：`201 Created`，`data` 是后台保存后的申请对象，其中`status`为`pending`、`approved`或`returned`。完成页使用响应中的 `data.id` 和 `data.receiptNumber`，不要再次发送新增请求。

| 异常状态 | 错误码 | 发生条件 | 前端处理 |
| --- | --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 字段缺失、格式错误、半日日期不一致或特別休暇未写制度名 | 显示 `details`，保留草稿并返回修正 |
| `400` | `START_DATE_IN_PAST` | 开始日期早于日本时间的今天 | 显示业务错误并保留草稿 |
| `401` | `AUTH_REQUIRED` | 没有有效会话，或用户已不可用 | 保存必要的页面状态后转到登录页 |
| `409` | `INSUFFICIENT_LEAVE_BALANCE` | 申请日数超过剩余日数 | 提示重新选择日期或类型 |
| `409` | `DUPLICATE_DATA` | 受付番号等唯一数据发生冲突 | 不自动重复提交，提示稍后重试 |
| `500` | `INTERNAL_ERROR` | 事务、数据库或后台处理失败 | 保留草稿并显示统一系统错误 |

提交按钮在请求处理中应禁用。网络超时后不能直接断定申请未保存；重新提交前应先查询申请一览，防止产生重复申请。

新增接口没有幂等请求标识，因此前端不能为该 `POST` 请求配置自动重试。“防止连续点击”指同一个页面请求处理中只发送一次，不代表两次已经到达后台的相同请求会合并为一条。

## 11. 取消申请 API

### 11.1 `PATCH /api/leave-applications/:id/cancel`

把当前用户的一条 `pending` 申请更新为 `cancelled`。必须携带有效会话，没有 JSON Body。随机得到`pending`的新申请可以使用此接口取消。

Path 参数：

| 参数 | 必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `id` | 是 | 大于 `0` 的整数 | 指定要取消的申请 ID |

请求示例：

```text
PATCH /api/leave-applications/12/cancel
```

正常响应：`200 OK`，`data` 是更新后的申请对象。其中 `status` 为 `cancelled`，`cancelledAt` 为 ISO UTC 日期时间。

| 异常状态 | 错误码 | 发生条件 | 前端处理 |
| --- | --- | --- | --- |
| `400` | `VALIDATION_ERROR` | ID 不是正整数 | 检查传入组件和 API 模块的 ID |
| `401` | `AUTH_REQUIRED` | 没有有效登录会话 | 转到登录页 |
| `404` | `APPLICATION_NOT_FOUND` | 申请不存在，或不属于当前用户 | 重新取得当前用户的申请一览 |
| `409` | `APPLICATION_NOT_PENDING` | 申请已经批准、退回或取消 | 提示状态已变化并刷新一览 |
| `500` | `INTERNAL_ERROR` | 更新失败 | 保留当前画面并显示统一系统错误 |

取消前可以显示确认对话框，但是否允许取消最终由后台状态决定。隐藏非 `pending` 记录的取消按钮只改善操作体验，不能代替后台校验。

## 12. Vue 端统一处理方式

### 12.1 正常响应

API 模块返回业务数据，使组件不需要反复解析 Axios 响应结构：

```js
import { http } from './http.js'

export async function createApplication(input) {
  const response = await http.post('/leave-applications', input)
  return response.data.data
}
```

`http.post()` 的第一个参数是相对于 `baseURL` 的路径，第二个参数是 JSON Body。函数最终返回申请对象。

### 12.2 异常响应

Axios 收到 `4xx` 或 `5xx` 时会抛出异常。下面的函数把后台响应与网络错误转换为页面容易处理的结构：

```js
export function toApiError(error) {
  if (!error.response) {
    return {
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'サーバーに接続できませんでした',
      details: []
    }
  }

  return {
    status: error.response.status,
    code: error.response.data?.error?.code ?? 'UNKNOWN_ERROR',
    message: error.response.data?.error?.message ?? '処理に失敗しました',
    details: error.response.data?.error?.details ?? []
  }
}
```

`error.response` 存在时，后台已经返回了 HTTP 响应；不存在时，可能是后台未启动、网络中断、超时或浏览器阻止请求。网络错误没有本附录所列的后台 `error.code`，前端需要单独显示连接失败消息。

## 13. 联调检查

- [ ] `GET /api/health` 返回 `200` 后再开始页面联调。
- [ ] 注册成功后，MySQL 中新增用户，响应和日志中没有密码。
- [ ] 首次打开页面时`GET /api/auth/me`返回`200`和`data: null`，不产生`401`。
- [ ] 登录响应设置 Cookie，刷新页面后 `GET /api/auth/me` 仍能恢复用户。
- [ ] 未登录访问汇总或申请接口时返回 `401 AUTH_REQUIRED`。
- [ ] 新增申请成功时返回 `201`，数据库与响应中的受付番号一致，`status`为`pending`、`approved`或`returned`。
- [ ] 一览无数据时返回空数组，而不是错误。
- [ ] 连续新增测试申请后，可以验证`pending`、`approved`、`returned`筛选、取消和首页汇总。
- [ ] 当前用户不能读取或取消其他用户的申请。
- [ ] 非 `pending` 申请返回 `409 APPLICATION_NOT_PENDING`。
- [ ] ISO UTC 时间由前端转换为日本显示时间，业务日期不做时区换算。
- [ ] 前端显示字段错误、业务错误、网络错误和系统错误，不只写入 Console。
