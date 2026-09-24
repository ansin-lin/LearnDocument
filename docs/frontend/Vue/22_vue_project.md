# 第 22 章 用 Vue 重构有給休暇申請系统

前面的 HTML、CSS、JavaScript 综合练习已经完成了一套“有給休暇申請システム”。本章不更换业务题目，而是把同一套页面和处理流程重构为 Vue 3 项目，并连接课程提供的 Node.js + MySQL API。

这里的“相同内容”是指画面功能、字段和申请流程保持一致，不是继续沿用原来的文件划分。Vue 项目按组件职责组织：路由 View 只负责组合和页面级处理，布局、表单、检索、列表和状态显示拆成可以复用的组件。注册用户、登录会话和申请记录从浏览器存储迁移到 MySQL。

## 1. 保持一致的页面与业务字段

### 1.1 保留画面功能，不照搬 HTML 文件结构

| 业务入口 | Vue 路由 | 主要组合内容 |
| --- | --- | --- |
| 登录 | `/login` | 认证布局、登录表单、API 错误消息 |
| 注册 | `/register` | 认证布局、注册表单、部门选项、API 错误消息 |
| 首页 | `/` | 业务布局、员工信息、休假汇总和业务入口 |
| 申请输入 | `/applications/new` | 业务布局、共用申请表单 |
| 申请确认 | `/applications/confirm` | 业务布局、申请内容摘要和提交操作 |
| 申请完成 | `/applications/:id/complete` | 业务布局、提交结果和受付番号 |
| 申请一览 | `/applications` | 业务布局、检索条件、结果件数、申请表格和取消操作 |

路由仍然对应使用者能看到的业务画面，但路由数量不决定组件数量。一个 View 可以组合多个组件，同一个组件也可以被多个 View 使用。例如申请输入和确认可以共用申请字段显示组件，首页和申请一览可以共用状态标签。

### 1.2 字段、状态和值保持一致

以下内容必须继续使用原练习规格，不得为了重构自行改名：

- 社员番号由后台在注册成功后生成，格式为 `EMP-00001`；
- 部门值：`development`、`quality`、`sales`、`general-affairs`、`human-resources`；
- 休假类型：`paid`、`half-am`、`half-pm`、`special`；
- 引继状态：`done`、`not-required`；
- 申请状态：`pending`、`approved`、`returned`、`cancelled`；
- 受付番号格式：`REQ-YYYYMMDD-NNN`；
- 登录、注册、日期、理由、引继和申请日数规则由后台统一校验；
- 首页剩余日数与申请汇总规则；
- 一览的状态筛选、关键字检索和取消规则。

### 1.3 从浏览器存储迁移到后台 API

HTML/CSS/JavaScript 阶段为了只练习前端，曾把用户、草稿和申请保存在 `localStorage` 或 `sessionStorage`。Vue 版改为以下职责：

| 数据 | 保存位置 | 前端处理 |
| --- | --- | --- |
| 登录会话 | 后台 MySQL Session + `HttpOnly` Cookie | Axios 使用 `withCredentials: true`，前端不读取 Cookie |
| 用户与部门 | MySQL | 通过注册、登录、当前用户和部门 API 取得 |
| 申请记录 | MySQL | 通过申请 API 新增、查询和取消 |
| 当前表单输入 | 当前 Vue 页面状态 | 校验失败时保留，刷新后不保证恢复 |
| 确认页草稿 | Pinia 的当前申请状态 | 确认提交成功后清除 |
| 最后受付番号 | 提交 API 的响应 | 传给完成页显示，刷新时可按申请详情重新取得 |

MySQL 是业务数据的唯一来源。不要同时把申请数组写入 `localStorage`，否则会出现前端数据与数据库不一致。

### 1.4 页面与 API 的对应关系

| 页面 | 使用的 API | 主要结果 |
| --- | --- | --- |
| 登录 | `POST /api/auth/login`、`GET /api/auth/me` | 建立会话并取得当前用户 |
| 退出 | `POST /api/auth/logout` | 删除后台 Session 并清除会话 Cookie |
| 注册 | `GET /api/master/departments`、`POST /api/users` | 取得部门选项并把新用户保存到 MySQL |
| 首页 | `GET /api/dashboard` | 员工信息、剩余日数和申请汇总 |
| 申请输入、确认 | 暂不请求 | 用 Vue 状态保存尚未提交的输入 |
| 申请提交 | `POST /api/leave-applications` | 保存申请并取得受付番号 |
| 完成 | 提交响应；刷新时使用 `GET /api/leave-applications/:id` | 显示受付番号，不重复提交 |
| 申请一览 | `GET /api/leave-applications` | 取得当前用户的筛选结果 |
| 取消 | `PATCH /api/leave-applications/:id/cancel` | 把申請中记录更新为取消済 |

各接口的入力位置、字段、正常响应和异常响应见[附录：有給休暇申請系统 API 入出力规格](appendix_paid_leave_api.md)。开发时以该接口契约为准，不根据页面字段自行猜测。

后台参考程序位于 `docs/frontend/training/paid-leave-system/backend/`。按照其中的 README 初始化 MySQL 并启动服务，不要求修改 Express 源码。

## 2. 目标目录与职责

```text
paid-leave-vue/
├─ src/
│  ├─ api/
│  │  ├─ http.js                       # 创建共用 Axios 实例并设置 API 地址、Cookie 和超时
│  │  ├─ auth.js                       # 封装注册、登录、当前用户和退出请求
│  │  └─ applications.js               # 封装首页汇总、申请查询、新增、详情和取消请求
│  ├─ components/
│  │  ├─ common/
│  │  │  ├─ AppMessage.vue             # 显示成功、警告和错误消息
│  │  │  └─ LoadingIndicator.vue       # 显示数据读取或提交中的状态
│  │  ├─ layout/
│  │  │  ├─ AuthLayout.vue             # 提供登录和注册画面的共同外框
│  │  │  └─ BusinessLayout.vue         # 提供登录后画面的标题、导航和主内容区域
│  │  ├─ auth/
│  │  │  ├─ LoginForm.vue              # 收集登录账号和密码并通知 View 提交
│  │  │  └─ RegisterForm.vue           # 收集注册资料并显示后台字段错误
│  │  └─ applications/
│  │     ├─ LeaveForm.vue               # 收集休假申请字段并显示后台字段错误
│  │     ├─ ApplicationSummary.vue      # 显示申请确认内容或首页汇总数字
│  │     ├─ ApplicationSearch.vue       # 收集状态和关键字查询条件
│  │     ├─ ApplicationTable.vue        # 显示申请一览并通知取消操作
│  │     └─ ApplicationStatusBadge.vue  # 统一显示申請中、承認済等状态
│  ├─ constants/
│  │  └─ masterData.js                 # 保存休假类型和申请状态的值与显示文字
│  ├─ router/
│  │  └─ index.js                      # 定义路由、页面组件和登录导航守卫
│  ├─ stores/
│  │  ├─ auth.js                       # 保存当前用户并提供登录状态操作
│  │  └─ applications.js               # 保存申请草稿、查询结果和异步状态
│  ├─ utils/
│  │  └─ date.js                       # 把 API 的 ISO UTC 时间转换为日本显示格式
│  ├─ views/
│  │  ├─ LoginView.vue                 # 组合登录表单并处理登录后的跳转
│  │  ├─ RegisterView.vue              # 取得部门、提交注册并显示注册结果
│  │  ├─ HomeView.vue                  # 取得并显示员工信息和休假汇总
│  │  ├─ LeaveApplyView.vue            # 管理申请输入状态并进入确认画面
│  │  ├─ LeaveConfirmView.vue          # 显示草稿、提交申请并处理后台错误
│  │  ├─ LeaveCompleteView.vue         # 根据路由 ID 显示提交结果和受付番号
│  │  └─ ApplicationListView.vue       # 协调查询条件、申请一览和取消操作
│  ├─ App.vue                          # 放置 RouterView，作为应用最外层组件
│  └─ main.js                          # 创建 Vue 应用并注册 Router 和 Pinia
├─ package.json                        # 记录依赖、Node.js 要求和 npm 命令
└─ README.md                           # 说明安装、启动、API、测试和已知限制
```

- View 是路由入口，负责读取路由参数、调用 Store、处理页面跳转，并组合完成该画面所需的组件。View 不重复编写表单字段、表格和状态标签。
- Component 按界面职责拆分。布局组件管理共同外框；认证组件管理登录或注册输入；申请组件管理表单、摘要、检索和列表显示。
- Router 负责 URL、页面关系和登录前后的导航边界。
- API 模块负责 URL、请求参数、Cookie 凭据和响应边界，组件不直接创建 Axios 实例。
- `auth` Store 负责当前登录用户；`applications` Store 负责确认草稿、申请数据和异步状态。
- `constants` 保存部门、休假类型和状态的“值—显示文字”映射。

组件边界不以原 HTML 文件数量为依据，也不要把每个输入框机械拆成组件。一个区域有独立职责、需要复用、代码明显过长或可以单独测试时，再提取为组件。

## 3. 重构原则

### 3.1 保留 HTML 语义和 CSS 设计

把旧页面的 `header`、`nav`、`main`、表单、表格、标签关联和提示文字迁入 Vue 模板。原来的共用 CSS 可以先作为全局样式导入，再按组件边界逐步整理。

迁移后仍须满足：

- `label` 与控件正确关联；
- 必填、错误和状态不能只靠颜色表达；
- 键盘可以完成导航、输入和提交；
- 360px 宽度下不出现横向溢出；
- 用户输入通过文本插值显示，不使用 `v-html`。

### 3.2 用 Vue 替换手动 DOM 操作

| 旧 JavaScript 做法 | Vue 中的做法 |
| --- | --- |
| `querySelector()` 读取输入 | `v-model` 绑定响应式状态 |
| `textContent` 更新文字 | 模板插值或计算属性 |
| 手动创建列表节点 | `v-for` 渲染列表并提供稳定 `key` |
| 手动显示或隐藏区域 | `v-if` / `v-else` |
| `addEventListener()` | `@submit`、`@click` 等事件绑定 |
| 修改 class | `:class` 或 `:style` |
| `location.href` | `router.push()` 或 `RouterLink` |

不要在 `onMounted()` 中重新查询整页元素并复制七个旧脚本。Vue 应根据状态渲染页面。

### 3.3 保持单向数据流

表单组件通过 Props 接收初始数据，通过 Emit 通知页面提交或更新。子组件不直接修改父组件数据，也不直接操作 Router、Axios 或 MySQL。

## 4. 分阶段实施

### 4.1 建立项目和路由骨架

1. 使用 `create-vue` 创建 JavaScript 项目，并选择 Vue Router、Pinia、Vitest 和 ESLint。
2. 先配置业务所需路由和最小 View，View 中暂时只保留页面标题。
3. 在 `App.vue` 中放置 `RouterView`，把共同外框留给布局组件处理。
4. 确认所有 URL 可以直接打开和刷新，再开始拆分业务组件。

可观察结果：每个业务 URL 都能进入正确 View，未知地址有明确的未找到页面，Console 没有错误。

### 4.2 迁移共通布局和样式

1. 把原 CSS 的颜色、间距、表单、按钮、表格和响应式规则迁入 Vue 项目。
2. 创建认证画面使用的 `AuthLayout` 和登录后画面使用的 `BusinessLayout`。
3. 把标题、导航、主内容区域和退出入口放入相应布局组件；登录和注册不显示业务导航。
4. 检查桌面和 360px 宽度。

可观察结果：画面内容和旧项目保持一致，布局不会因组件拆分而改变。

### 4.3 先完成认证组件

1. 建立共用 Axios 实例，设置 API 地址、10 秒超时和 `withCredentials: true`。
2. 创建 `LoginForm` 和 `RegisterForm`，由组件收集输入并通过 Emit 通知 View 提交。
3. `LoginView` 调用登录 API，`auth` Store 再调用当前用户 API 恢复状态。
4. 路由全局前置守卫阻止未登录用户进入业务页面，并保留目标地址。
5. `RegisterView` 先取得部门选项，再把表单 Emit 的输入提交给注册 API。
6. 注册失败时把后台 `details` 交给表单组件显示；成功后显示社员番号并返回登录页。
7. 退出时调用退出 API，再清除 Pinia 中的用户和申请状态。

可观察结果：注册资料写入 MySQL 后可以登录；错误账号显示统一消息；刷新后能通过 Cookie 会话恢复当前用户；密码不进入 Pinia、浏览器存储、API 响应或日志。

### 4.4 建立可复用的申请组件

1. `LeaveForm` 负责申请字段、基础前端校验和后台字段错误显示，不直接操作 Router、Store 或 Axios。
2. 申请页把当前输入保存在局部响应式状态。
3. 休假类型、日期、必填项、字符数、半日休假和特别休假规则在选择、输入结束时即时校验；点击“確認画面へ”时再执行一次全表单校验。
4. 前端校验通过后才把草稿交给 Pinia 并进入确认页。确认页通过 `ApplicationSummary` 显示草稿，不再改变输入内容。
5. 确认页点击“申請する”后调用新增申请 API；后台重新执行完整校验，前端校验不能代替后台校验。
6. 使用 `submitting` 禁用提交按钮，防止连续点击重复发送请求。
7. 后台返回字段或业务错误时回到申请页，保留草稿并在对应字段附近显示错误；成功后清除草稿并进入完成页。

可观察结果：完成页根据路由 ID 显示申请和受付番号，刷新只重新读取详情、不会新增记录；请求失败时不清除草稿、不跳转成功页。

`submitting` 只负责防止请求处理中的连续点击，不表示新增接口具有幂等性。如果请求超时，不能立即再次提交；应先查询申请一览，确认刚才的申请是否已经保存，再决定是否重试。

API 返回的日期时间统一为 ISO UTC，例如 `2026-09-18T03:15:20.123Z`。前端保留原始值用于数据处理，显示时再使用 `Intl.DateTimeFormat('ja-JP', ...)` 转换为日本日期时间格式。不要要求后台直接返回 `2026年9月18日 12:15` 之类的画面文字。

### 4.5 组合首页和申请一览

1. 首页调用汇总 API，通过摘要组件显示有給残日数、申請中件数和当月承認済日数。
2. 一览页组合 `ApplicationSearch`、`ApplicationTable` 和 `ApplicationStatusBadge`，通过 Store 调用申请一览 API。
3. 状态和关键字作为 Query 参数交给 API；Store 保存返回结果和加载状态。
4. 区分“没有申请”“筛选无结果”和“读取失败”。
5. 随机得到`pending`的新申请显示取消按钮；取消前确认，再调用取消 API。`approved`和`returned`申请不能取消。

可观察结果：清除条件后重新取得完整列表，前端不能读取其他用户的申请。

### 4.6 完成退出和异常处理

退出时调用后台删除会话，并清除当前用户、草稿和页面状态。申请记录继续保存在 MySQL。页面不能只在 Console 报错；使用者可恢复的错误必须在页面显示。

## 5. Vue 测试要求

原 JavaScript 练习的业务结果继续作为回归基准，但浏览器存储已经替换为 API 和 MySQL，因此不照搬存储实现相关的测试步骤。

| 原测试范围 | Vue 版对应结果 |
| --- | --- |
| JS-01～JS-02 | 已注册账号可以登录；错误账号显示统一错误；密码不保存到前端状态 |
| JS-03～JS-05 | 注册错误由后台返回并显示；成功时用户写入 MySQL 且密码只保存哈希 |
| JS-06～JS-07 | 未登录不能停留在业务页；无申请时首页显示 `12、0、0` |
| JS-08～JS-11 | 输入页即时提示字段错误，全表单校验通过后才进入确认页；后台仍拒绝无效输入并保留草稿 |
| JS-12～JS-13 | 同一页面请求处理中连续点击只发送一次；完成页刷新不会新增申请 |
| JS-14～JS-15 | 状态和关键字可以组合；新申请可以看到后台随机生成的申請中、承認済或差戻し状态，申請中记录可以取消 |
| JS-16 | 改为 API 读取或保存失败时显示错误，并保留可恢复的输入 |
| JS-17 | 用户输入只作为文字显示，不通过 `v-html` 执行 |
| JS-18 | 退出后重新登录，MySQL 中的申请记录仍然存在 |

另外至少补充以下 Vue 测试：

| 测试对象 | 主要验证 |
| --- | --- |
| `LeaveForm` | 输入收集、后台字段错误显示、提交中禁用 |
| `ApplicationTable` | Props 显示、稳定 key、取消事件 |
| `auth` Store | 登录成功、失败、会话恢复和退出 |
| `applications` Store | 加载、失败、重复提交保护和取消 |
| API 模块 | Query 参数、Cookie 凭据和错误响应转换 |
| Router | 未登录重定向、登录后访问业务页 |
| `ApplicationListView` | 空数据、筛选无结果、读取失败 |

测试组件公开行为，不依赖内部 `ref` 的变量名。前端单元测试使用 API Mock，不连接真实 MySQL；最终联调测试再启动课程提供的后台程序。

## 6. 验收清单

### 6.1 业务一致性

- [ ] 原有业务画面和操作都能通过 Vue 路由进入，但没有按原 HTML 文件机械拆分组件。
- [ ] 字段、选项值、显示文字和状态与原项目一致。
- [ ] 登录、注册、首页汇总、申请、确认、完成、一览和`pending`申请取消均可运行。
- [ ] JS-01～JS-18 的业务结果已经按 Vue + API 的实现方式完成对应验证。
- [ ] 后台 API 只实现既有业务所需功能，没有擅自增加业务范围。

### 6.2 Vue 实现

- [ ] 页面不再依赖七个旧 HTML 和七个入口脚本。
- [ ] 没有用手动 DOM 操作模拟 Vue 渲染。
- [ ] View 只负责路由级协调，布局、认证表单、申请表单、检索、表格和状态显示具有清楚的组件边界。
- [ ] Props 不被子组件直接修改，列表使用稳定 key。
- [ ] 读取、空数据、输入错误、处理中、成功和系统错误状态可以区分。
- [ ] 刷新业务页面时能够通过后台会话恢复，未登录时正确返回登录页。
- [ ] 请求超时时先查询申请一览，不把新增申请请求配置为自动重试。

### 6.3 质量与交付

```bash
npm run lint
npm run test:unit
npm run build
npm run preview
git status
git diff
```

所有命令应成功。预览环境中还要验证直接打开各路由、刷新、浏览器前进后退、键盘操作和 360px 布局。

## 7. 提交物

- Vue 项目源码和锁文件；
- 后台启动与联调记录；
- README（安装、启动、构建、测试、目录和研修边界）；
- JS-01～JS-18 业务结果的 Vue 版对应回归记录；
- Vue 组件、Store 和 Router 的测试结果；
- 新旧实现对应表；
- 至少一条真实排错记录；
- 已知问题和交接事项。

交接时必须明确说明：这是对原有有給休暇申請系统的 Vue 重构；用户、登录会话和申请由 Node.js API 与 MySQL 管理，业务校验以后台结果为准，前端负责输入、页面状态、错误显示和日期时间格式化。
