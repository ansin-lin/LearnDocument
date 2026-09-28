# 第 17 章 项目目录、API Layer 与数据约定

## 本章目标

- 按职责组织页面、组件、请求、状态与数据结构。
- 保持依赖方向清晰，避免组件中散落 HTTP 细节。
- 能从入口一路追到 API。

## 1. 推荐起点

```text
src/
├─ assets/
├─ components/          通用 UI
├─ features/
│  ├─ auth/
│  └─ employees/        员工业务组件、Hook、页面
├─ layouts/
├─ router/
├─ services/            HTTP client 与跨功能服务
├─ stores/
├─ models/              共享业务字段与运行时校验
├─ utils/
├─ App.jsx
└─ main.jsx
```

小项目不必一次创建所有空目录。先按职责建立最少结构，功能增长后再按 feature 聚合。目录名不是架构本身，关键是依赖方向和所有权一致。

### 1.1 每个目录负责什么

| 目录 | 放入内容 | 不应该放入 |
| --- | --- | --- |
| `components` | 跨业务复用的 Button、Message、Loading | 员工 API 和页面路由 |
| `features/employees` | 员工 Page、业务组件、Hook、校验 | 其他业务的共通工具 |
| `layouts` | Header、Sidebar、`Outlet` 外框 | 员工表单业务规则 |
| `router` | 路由表、认证和权限门禁 | Axios 请求实现 |
| `services` | `httpClient`、跨功能基础通信 | JSX 和页面 Message |
| `stores` | 跨页面客户端共享状态 | 每个输入框的临时值 |
| `models` | 字段契约、运行时校验、固定结构说明 | 无关的杂项函数 |
| `utils` | 无 React State 的纯格式化和转换函数 | API、Hook、组件 |

只被员工功能使用的组件、Hook 和 Service 可以放在 `features/employees` 内；真正跨功能复用后再提升到顶层共通目录。不要一开始就创建大量空文件夹。

### 1.2 推荐依赖方向

```text
main.jsx
  ↓
Router → Layout → Page → Component
                    ↓
                  Hook / Store
                    ↓
              employeeService
                    ↓
                httpClient
                    ↓
                  Backend
```

下层模块不应反向导入 Page。例如 `employeeService.js` 不能导入 `EmployeeListPage.jsx`。反向依赖很容易形成循环导入，也让 Service 无法脱离画面测试。

## 2. API Layer

不推荐在每个组件直接 `axios.get(...)`。组件只表达用户流程：

```text
Page / Hook → employeeService → httpClient → Backend
```

- `httpClient`：base URL、timeout、通用请求/响应处理。
- `employeeService`：员工接口路径、参数和响应字段约定。
- Hook/Page：loading、页面状态、取消与用户消息。

拦截器应保持克制。若在拦截器中刷新令牌，要处理并发刷新、重试上限和失败退出，避免无限重试。

## 3. 共享数据约定

JavaScript 项目仍然需要统一字段名称和对象结构。例如员工对象固定包含：

```text
Employee
├─ id
├─ employeeCode
├─ name
├─ email
├─ department
├─ role          ADMIN / USER
├─ joinedDate
└─ status        ACTIVE / INACTIVE
```

这是第 1～23 章员工案例的最终字段集合。第 11 章最小 CRUD 示例已经使用 `employeeCode`、`name`、`department` 和 `email`；进入工程化阶段后补充 `role`、`joinedDate` 和 `status`。后续代码不得再把同一字段改成 `code`、`joined_date` 等另一名称。若后端字段不同，应只在 Service 边界转换一次。

```js
export function normalizeEmployee(data) {
  return {
    id: data.id,
    employeeCode: data.employeeCode,
    name: data.name,
    email: data.email,
    department: data.department,
    role: data.role,
    joinedDate: data.joinedDate,
    status: data.status,
  };
}
```

`normalizeEmployee()` 把外部响应转换成项目统一对象。它不能凭空补造缺失业务数据；必要字段不正确时，应在 Service 边界报告契约错误。

分页响应固定包含 `items`、`page`、`size`、`total`；请求状态固定使用 `{ status: 'loading' }`、`{ status: 'success', data }` 或 `{ status: 'error', error }`。这些约定应写入 API 规格、函数注释或项目文档。服务器响应属于外部数据，必要时在 Service 边界使用 schema 或校验函数确认字段。

认证状态统一使用第 14 章的结构化对象；错误对象使用第 18 章的 `kind` 与 `message` 约定。跨 Service/Page 共享的数据契约放在功能目录或 `models`，不要在 Page、Store 和 Context 中分别设计不同字段名。

### 3.1 Service 签名清单

项目统一沿用第 10～11 章的函数：

```text
getEmployees(signal)                          → 员工数组的 Promise（signal 可省略）
searchEmployees(query, signal)                → 分页响应对象的 Promise（signal 可省略）
getEmployee(id, signal)                       → 员工对象的 Promise（signal 可省略）
createEmployee(input)                         → 新员工对象的 Promise
updateEmployee(id, input)                     → 更新后员工对象的 Promise
deleteEmployee(id)                            → 无响应数据的 Promise
```

实战列表使用 `searchEmployees`；基础 `getEmployees` 只保留为学习取消请求的单点示例。不要在实战中混用两套列表响应。

## 4. 入口调查

```text
package.json
  ↓ scripts/dependencies
main.jsx
  ↓ Provider/Router
AppRouter
  ↓ route
EmployeeListPage
  ↓ hook/component
employeeService
  ↓ httpClient
Backend API
```

调查既有项目时，先沿实际 import 和调用逐层确认，不要只根据目录名猜测职责。找到一次请求从按钮到 Network 的完整路径后，再扩展到相邻功能。

## 5. 循环依赖与边界检查

常见风险：

```text
EmployeeForm → employeeUtils → EmployeeForm
Page → Store → Page
Service → Component → Service
```

修正方法不是把所有内容搬进一个文件，而是把双方共同需要的纯数据或函数下移到不依赖 UI 的模块。可通过浏览器运行错误、构建警告和 import 关系调查循环依赖。

## 6. 练习

1. 把散落在三个组件中的 GET 移入 Service。
2. 统一 Employee 与分页响应的字段约定。
3. 从 `/employees/:id/edit` 画出文件依赖链。
4. 找到一个循环依赖风险并通过调整职责消除。
5. 为目录中的每个实际文件标注所有者，并删除没有用途的空目录。

## 本章检查点

- [ ] Employee、查询条件、认证状态、请求状态与分页响应各有唯一数据约定。
- [ ] 能沿 Page/Hook → Service → httpClient → Backend 调查请求。
- [ ] 能解释基础 getEmployees 与项目 searchEmployees 的迁移边界。
