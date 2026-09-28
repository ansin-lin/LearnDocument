# 第 24 章 用 React 重构有給休暇申請系统：规格与起始结构

前面的 HTML、CSS、JavaScript 综合练习已经完成了“有給休暇申請システム”。本项目保留相同的页面、字段和业务流程，使用 React 重新组织前端，并连接课程提供的 Node.js + MySQL API。

重构不是把七个 HTML 文件逐行改写成七个 JSX 文件。React 项目应按组件职责拆分：Page 负责路由级协调，布局、表单、检索、表格和状态显示由组件承担，API 请求集中在 Service 中。

## 1. 七个业务页面

| 页面 | React 路由 | 主要职责 |
| --- | --- | --- |
| 登录 | `/login` | 输入账号和密码，建立登录会话 |
| 注册 | `/register` | 取得部门选项并注册用户 |
| 首页 | `/` | 显示员工信息、剩余日数和申请汇总 |
| 申请输入 | `/applications/new` | 输入休假类型、日期、理由和引继状态 |
| 申请确认 | `/applications/confirm` | 确认草稿并提交申请 |
| 申请完成 | `/applications/:id/complete` | 显示受付番号和申请结果 |
| 申请一览 | `/applications` | 检索申请、显示状态并取消申请 |

路由数量不决定组件数量。登录和注册可以共用认证布局；输入与确认可以共用字段显示；首页和一览可以共用状态标签。

## 2. 必须保持一致的业务规格

- 社员番号由后台生成，格式为 `EMP-00001`；
- 部门值：`development`、`quality`、`sales`、`general-affairs`、`human-resources`；
- 休假类型：`paid`、`half-am`、`half-pm`、`special`；
- 引继状态：`done`、`not-required`；
- 申请状态：`pending`、`approved`、`returned`、`cancelled`；
- 受付番号格式：`REQ-YYYYMMDD-NNN`；
- 登录、注册、日期、理由、引继和申请日数规则由后台再次校验；
- 新增申请时，后台随机决定 `pending`、`approved` 或 `returned`；
- 只有 `pending` 申请可以取消；
- API 日期时间使用 ISO UTC，React 在显示时转换为日本日期时间。

## 3. 数据保存位置

| 数据 | 保存位置 | React 的职责 |
| --- | --- | --- |
| 登录会话 | MySQL Session + `HttpOnly` Cookie | Axios 携带 Cookie，JavaScript 不读取 Cookie |
| 用户、部门 | MySQL | 通过注册、登录、当前用户和部门 API 取得 |
| 申请记录 | MySQL | 通过申请 API 新增、查询、详情和取消 |
| 当前表单输入 | `LeaveApplyPage` 局部 State | 即时校验并保留输入 |
| 跨页面确认草稿 | Zustand Application Store | 确认提交成功后清除 |
| 当前登录用户 | Zustand Auth Store | 登录、恢复会话和退出共用 |
| 最后受付番号 | 提交响应或详情 API | 完成页显示，刷新时重新读取详情 |

MySQL 是业务数据的最终来源。React State 和 Zustand 只保存当前浏览器运行期间需要的副本，不再把用户和申请数组写入 `localStorage`。

## 4. 页面与 API 对应

| 页面或操作 | API | 主要结果 |
| --- | --- | --- |
| 登录 | `POST /api/auth/login` | 建立 Session 并返回当前用户 |
| 当前用户 | `GET /api/auth/me` | 首次进入或刷新时恢复登录状态 |
| 退出 | `POST /api/auth/logout` | 删除 Session 并清除 Cookie |
| 注册 | `GET /api/master/departments`、`POST /api/users` | 取得部门并保存用户 |
| 首页 | `GET /api/dashboard` | 用户信息、剩余日数和申请汇总 |
| 申请提交 | `POST /api/leave-applications` | 保存申请并返回 ID、受付番号和状态 |
| 完成页刷新 | `GET /api/leave-applications/:id` | 重新取得已提交申请，不重复新增 |
| 申请一览 | `GET /api/leave-applications` | 取得当前用户的筛选结果 |
| 取消申请 | `PATCH /api/leave-applications/:id/cancel` | 把 `pending` 更新为 `cancelled` |

完整入力、出力和错误响应以[有給休暇申請系统 API 入出力规格](../Vue/appendix_paid_leave_api.md)为准。后台程序位于 `docs/frontend/training/paid-leave-system/backend/`，本练习不要求学员修改 Express 源码。

## 5. React 项目目录

```text
paid-leave-react/
├─ src/
│  ├─ api/
│  │  ├─ httpClient.js              # Axios 共用实例、API 地址、Cookie、超时
│  │  ├─ authService.js             # 注册、登录、当前用户、退出
│  │  └─ applicationService.js      # 汇总、申请查询、新增、详情、取消
│  ├─ components/
│  │  ├─ common/
│  │  │  ├─ AppMessage.jsx          # 成功、警告和错误消息
│  │  │  └─ LoadingIndicator.jsx    # 读取或提交中的状态
│  │  ├─ layout/
│  │  │  ├─ AuthLayout.jsx          # 登录和注册的共同外框
│  │  │  └─ BusinessLayout.jsx      # 登录后标题、导航、退出和 Outlet
│  │  ├─ auth/
│  │  │  ├─ LoginForm.jsx           # 登录输入和前端校验
│  │  │  └─ RegisterForm.jsx        # 注册输入和字段错误
│  │  └─ applications/
│  │     ├─ LeaveForm.jsx            # 申请输入与字段错误
│  │     ├─ ApplicationSummary.jsx   # 确认内容或首页汇总
│  │     ├─ ApplicationSearch.jsx    # 状态和关键字条件
│  │     ├─ ApplicationTable.jsx     # 一览和取消事件
│  │     └─ ApplicationStatusBadge.jsx # 状态显示文字
│  ├─ constants/masterData.js        # 选项值与显示文字
│  ├─ router/AppRouter.jsx           # 路由和认证门禁
│  ├─ stores/
│  │  ├─ authStore.js                # 当前用户和认证状态
│  │  └─ applicationStore.js         # 申请草稿、列表和异步状态
│  ├─ utils/date.js                  # ISO UTC 转日本日期时间
│  ├─ pages/                         # 七个路由页面
│  ├─ App.jsx                        # 应用最外层组件
│  └─ main.jsx                       # React 根入口
├─ .env.example                      # 非敏感 API 地址示例
├─ package.json                      # 依赖和命令
├─ package-lock.json                 # 锁定依赖版本
└─ README.md                         # 启动、测试、构建和联调说明
```

## 6. 组件职责与依赖方向

```text
Page → Component
Page / Store → Service → httpClient → Backend
Router → Page / Layout
Component → Props / Callback → Page
```

- Page 读取路由参数、协调 Store 和 Service、决定页面跳转；
- Component 使用 Props 接收数据，通过回调通知父组件，不直接调用 Router 或 Axios；
- Service 只处理 HTTP 契约，不显示 Message；
- Auth Store 保存当前用户；Application Store 保存跨页草稿和共享申请状态；
- 表单输入优先留在表单或页面，不把每次按键都全局化。

## 7. 从旧实现迁移到 React

| 旧 JavaScript 写法 | React 写法 |
| --- | --- |
| `querySelector()` 读取控件 | 受控表单 State |
| `textContent` 修改内容 | JSX 根据 State 渲染 |
| 手动创建表格行 | `map()` + 稳定 `key` |
| `classList` 切换显示 | 条件渲染或 `className` |
| `addEventListener()` | `onSubmit`、`onClick` |
| `location.href` | `Link`、`Navigate`、`useNavigate` |
| 浏览器存储保存业务数据 | Axios 调用后台 API |

不要在 Effect 中重新查询整页 DOM，也不要把七个旧脚本直接复制进七个 Page。

## 8. 分阶段完成结果

1. 建立 JavaScript React 项目，安装 Router、Axios、Zustand和测试依赖。
2. 建立七个 Page、两种 Layout、404 页面和认证路由骨架。
3. 迁移共用 CSS，检查语义、键盘操作和 360px 宽度。
4. 按第 25 章完成登录、注册、首页。
5. 按第 26 章完成申请输入、确认、完成和一览。
6. 按第 27 章实施日期范围筛选改修并完成交付。

## 本章检查点

- [ ] 七个页面的业务字段和流程与原练习一致。
- [ ] 页面数量没有被误解为组件数量。
- [ ] 用户、会话和申请均由后台与 MySQL 管理。
- [ ] Page、Component、Store、Service 和 Router 职责清楚。
- [ ] 项目没有继续使用旧脚本的手动 DOM 操作。
