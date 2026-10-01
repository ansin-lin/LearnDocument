# 第 24 章 React 实战：登录、注册与首页

本章完成七个业务页面中的前三个：登录、注册和首页。开始状态为第 23 章的路由、目录和共用 Axios 实例已经建立。

## 1. 共用 Axios 与 Cookie Session

`src/api/httpClient.js`：

```js
import axios from 'axios';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});
```

`withCredentials: true` 让跨端口请求携带后台设置的 `HttpOnly` Cookie。前端不读取 Cookie，也不把密码、Session ID 保存到 Zustand 或浏览器存储。

## 2. Auth Service 与 Store

`authService.js` 集中定义认证 API：

```js
export async function getCurrentUser(signal) {
  return (await httpClient.get('/auth/me', { signal })).data.data;
}

export async function login(input) {
  return (await httpClient.post('/auth/login', input)).data.data;
}

export async function logout() {
  await httpClient.post('/auth/logout');
}

export async function getDepartments(signal) {
  return (await httpClient.get('/master/departments', { signal })).data.data;
}

export async function registerUser(input) {
  return (await httpClient.post('/users', input)).data.data;
}
```

Auth Store 沿用第 16 章的三态：

```text
checking → 正在确认后台会话
anonymous → 没有有效会话
authenticated → 已取得当前用户
```

首次启动调用 `GET /api/auth/me`。没有 Cookie 时接口返回 `200` 且 `data` 为 `null`，这是正常未登录状态，不应作为 Console 错误显示。

Store 至少提供：

```text
restoreSession(signal)
signIn(input)
signOut()
status
user
errorMessage
```

`signOut()` 调用后台后清除用户，同时清除 Application Store 的草稿和页面状态；MySQL 中的申请记录不会被删除。

## 3. 登录页面

`LoginForm` 只负责账号、密码、即时错误和提交事件。`LoginPage` 调用 Store 并处理路由跳转。

```text
LoginForm
  ↓ onSubmit(credentials)
LoginPage
  ↓ authStore.signIn()
Auth Service
  ↓ POST /api/auth/login
Backend / Session
  ↓ user
Auth Store → authenticated
  ↓
安全返回原页面或首页
```

实现要求：

- 账号和密码在输入/离开控件时进行基础校验；
- 全表单校验失败时不调用 API；
- `submitting` 期间禁用登录按钮；
- 登录失败使用统一消息，不泄漏账号是否存在；
- 密码不回显、不写日志、不进入 Store；
- `location.state.from` 只接受应用内部路径。

验证：错误账号留在登录页；成功后进入安全目标；刷新业务页能够通过 Session 恢复用户。

## 4. 注册页面

进入注册页时先读取部门选项。页面必须区分：

```text
loading → 部门读取中
error → 部门读取失败，可重试
success → 显示注册表单
```

`RegisterForm` 收集 API 规格中的字段，把前端错误和后台 `details` 显示在对应控件附近。提交过程：

```text
即时字段校验
  ↓
点击注册后执行全表单校验
  ↓
POST /api/users
  ├─ 成功：显示生成的社员番号并返回登录页
  └─ 失败：保留输入并显示字段/业务错误
```

注册成功必须能够用新账号登录，证明数据已经写入 MySQL。密码只在注册或登录请求中发送，后台保存哈希，前端不保存明文；正式环境必须使用 HTTPS。

## 5. 认证路由和布局

路由结构：

```text
AuthLayout
├─ /login
└─ /register

RequireAuth
└─ BusinessLayout
   ├─ /
   ├─ /applications/new
   ├─ /applications/confirm
   ├─ /applications/:id/complete
   └─ /applications
```

`RequireAuth` 在 `checking` 时显示确认中，在 `anonymous` 时转到登录页，在 `authenticated` 时显示 `Outlet`。`BusinessLayout` 负责标题、导航、当前用户和退出按钮，各 Page 不复制导航栏。

## 6. 首页

首页调用 `GET /api/dashboard`，显示：

- 当前员工的社员番号、姓名和部门；
- 有給残日数；
- 申请中件数；
- 当月承認済日数；
- 进入申请输入和申请一览的业务入口。

首页必须覆盖读取中、读取失败和成功状态。没有申请时汇总值为 `12、0、0`，不是“没有数据”的空白页。

API 时间保持 ISO UTC 原值，显示时使用：

```js
export function formatJapanDateTime(value) {
  return new Intl.DateTimeFormat('ja-JP', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Asia/Tokyo',
  }).format(new Date(value));
}
```

无效日期应显示约定的替代文字，不能让 `Invalid Date` 直接出现在页面。

## 7. 本章验证

| Case | 操作 | 期待结果 |
| --- | --- | --- |
| AUTH-01 | 首次打开，无 Cookie | 不产生 401 Console 错误，进入未登录状态 |
| AUTH-02 | 错误账号或密码 | 显示统一错误，不泄漏账号存在性 |
| AUTH-03 | 正确登录 | 建立 Session，进入原目标或首页 |
| AUTH-04 | 刷新首页 | 恢复当前用户，不错误闪回登录页 |
| REG-01 | 注册字段不正确 | 对应字段附近显示错误，不提交 |
| REG-02 | 正确注册 | MySQL 保存用户，显示社员番号，可以登录 |
| HOME-01 | 没有申请 | 显示剩余 12、申请中 0、当月承认 0 |
| LOGOUT-01 | 退出后访问业务 URL | 返回登录页，申请记录仍保存在数据库 |

同时使用 Network 确认 URL、Method、Status、Payload 和 Response；截图或日志中隐藏密码与 Cookie。

## 本章检查点

- [ ] 登录、注册和首页三个页面可通过路由进入。
- [ ] Auth Store 是 Header、Route 和登录页面的唯一认证状态。
- [ ] 注册用户真实写入 MySQL，密码未保存在前端。
- [ ] 首页正确处理 checking、loading、error 和 success。
- [ ] 退出会清理前端状态，但不会删除业务数据。
