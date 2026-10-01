# 第 17 章 React 项目结构与代码职责划分

> 从简单 Demo 到实际企业项目

## 本章目标

- 按职责组织页面、组件、请求、状态与数据结构。
- 保持依赖方向清晰，避免组件中散落 HTTP 细节。
- 能从入口一路追到 API。
- 能判断一段代码应该放在 Page、Component、Hook、Service、Store 还是 Util。
- 能按照固定顺序阅读陌生 React 项目。

## 1. 为什么项目变大后需要分层

刚开始学习 React 时，下面的结构没有问题：

```text
src/
├─ App.jsx
├─ EmployeeList.jsx
├─ EmployeeDetail.jsx
└─ EmployeeEdit.jsx
```

但是，如果把员工列表的所有处理都写进一个组件，它会逐渐变成这样：

```jsx
function EmployeeListPage() {
  // 搜索条件 State
  // 分页和排序 State
  // axios.get() 读取列表
  // axios.delete() 删除员工
  // Loading 和 Error
  // 删除确认 Dialog
  // Toast
  // 日期和部门名称格式化
  // 大量 JSX
}
```

这段代码不一定立刻报错，但功能增加后会出现以下问题：

- Page 越来越长，很难找到要修改的位置；
- 相同的 API 和格式转换散落在多个组件；
- Backend 修改 URL 或字段时，需要到处修改；
- UI、业务流程和 HTTP 细节混在一起，不容易测试；
- 新成员不知道一次请求经过了哪些文件。

目录结构和职责划分的目的不是“让文件变多”，而是让每段代码有稳定的归属。小项目仍应从最少文件开始，出现明确职责后再拆分。

可以按下面的信号判断是否需要拆分：

| 当前情况 | 建议操作 |
| --- | --- |
| 页面只有少量 JSX 和一个局部 State | 暂时保留在一个组件中 |
| 同一组 JSX 在多个位置重复 | 提取 Component |
| 多个组件重复相同的 State + Effect 流程 | 提取自定义 Hook |
| 多个页面直接写相同 API URL | 提取业务 Service |
| 多个无直接父子关系的页面共享并修改数据 | 评估 Context 或 Store |
| 纯格式化、计算逻辑被重复使用 | 提取 Util |

拆分应由实际问题驱动，不是组件超过某个固定行数就机械拆文件。

## 2. React 企业项目常见目录结构

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

### 2.1 每个目录负责什么

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

### 2.2 Feature 目录怎样展开

Feature 是围绕一组业务功能组织代码的目录。员工管理可以写成：

```text
features/employees/
├─ pages/
│  ├─ EmployeeListPage.jsx
│  ├─ EmployeeDetailPage.jsx
│  └─ EmployeeEditPage.jsx
├─ components/
│  ├─ EmployeeSearchForm.jsx
│  ├─ EmployeeTable.jsx
│  └─ EmployeeForm.jsx
├─ hooks/
│  └─ useEmployees.js
├─ services/
│  └─ employeeService.js
├─ models/
│  └─ employee.js
└─ utils/
   └─ employeeFormatters.js
```

`EmployeeTable` 只服务员工业务，因此先放在 `features/employees/components`。只有当它被多个业务以相同方式使用时，才考虑提升为顶层通用组件。过早把业务组件放进全局 `components`，会让“通用组件”目录逐渐变成杂物箱。

### 2.3 Layout 与普通 Component 的区别

Layout 表示多个页面共用的页面外框，通常配合 React Router 的 `Outlet`：

```jsx
import { Outlet } from 'react-router-dom';
import { Header } from '../components/Header';
import { Sidebar } from '../components/Sidebar';

export function MainLayout() {
  return (
    <>
      <Header />
      <div className="app-body">
        <Sidebar />
        <main><Outlet /></main>
      </div>
    </>
  );
}
```

Header、Sidebar 和内容区域属于 Layout；员工搜索表单和员工表格属于业务 Component。Layout 不负责调用员工 API。

### 2.4 Service、Store、Model 与 Util 的边界

- `httpClient`：Axios 的 `baseURL`、timeout、共通 Header 等通信配置。
- `employeeService`：员工 API 的 URL、参数和响应转换。
- `store`：多个无直接父子关系的页面需要共享的客户端状态。
- `model`：Employee 字段、允许值和运行时数据约定。
- `util`：不依赖 React State 和 UI 的纯转换函数。

例如，登录用户和全局主题可以进入 Store；`EmployeeEdit` 中尚未提交的输入内容、当前 Dialog 是否打开，通常留在组件。`formatJoinedDate()` 可以是纯 Util，但“当前用户是否能删除员工”属于权限规则，不应随意伪装成无业务含义的通用工具。

### 2.5 推荐依赖方向

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

## 3. 什么是职责分离

| 层 | 主要职责 | 不负责 |
| --- | --- | --- |
| Page | 组织页面流程、连接 Hook 与业务组件 | Axios 共通配置 |
| Component | 显示 UI、接收 Props、通知事件 | 决定 API URL |
| Hook | 复用 React State、Effect 和页面流程 | 渲染完整页面 |
| Service | 调用 API、转换边界数据 | 显示 Dialog 或 Toast |
| Store | 保存跨页面共享状态和 Action | 保存所有输入框状态 |
| Util | 纯格式化和计算 | 使用 Hook 或渲染 JSX |

员工列表的推荐结构是：

```text
EmployeeListPage
├─ EmployeeSearchForm
├─ EmployeeTable
└─ Pagination
        │
        └→ useEmployees
              ↓
       employeeService
              ↓
          httpClient
              ↓
           Backend
```

Page 决定这些部分如何组合；Hook 管理读取状态和 Effect；Service 负责 HTTP；子组件只通过 Props 接收显示所需数据并通知用户操作。

最小 Page 可以写成：

```jsx
import { ErrorPanel } from '../../../components/ErrorPanel';
import { EmployeeTable } from '../components/EmployeeTable';
import { useEmployees } from '../hooks/useEmployees';

export function EmployeeListPage() {
  const {
    employees,
    loading,
    error,
    reload,
  } = useEmployees();

  return (
    <section>
      <h1>员工管理</h1>
      {loading && <p role="status">读取中...</p>}
      {error && <ErrorPanel error={error} onRetry={reload} />}
      {!loading && !error && (
        <EmployeeTable employees={employees} />
      )}
    </section>
  );
}
```

看到这段代码时，新人可以快速知道：页面组合在这里，数据读取细节应继续追踪 `useEmployees()`。

## 4. API Layer 深入讲解

### 4.1 Service 是什么

Service 是前端与 Backend API 之间的业务通信层。它把“员工业务需要调用哪些接口”集中成容易理解的函数：

```js
getEmployees()
getEmployee(id)
createEmployee(input)
updateEmployee(id, input)
deleteEmployee(id)
```

组件只需要表达“读取员工”或“删除员工”，不必反复处理 Axios、URL、HTTP Method 和响应结构。

```text
Component / Page
只知道：我要读取员工
        ↓
employeeService.getEmployee(id)
知道：员工 API 的 URL、Method、参数和响应格式
        ↓
httpClient
知道：API base URL、timeout 和共通 Header
        ↓
Backend
```

Service 不是 React Component，也不是 Store。它通常是一个普通 JavaScript 模块，导出用于访问某类业务 API 的函数。

### 4.2 为什么不全部写在组件中

```jsx
// 不推荐：组件同时知道 Axios、URL 和画面显示
async function handleDelete(id) {
  await axios.delete(`/api/employees/${id}`);
  setEmployees((current) => current.filter((item) => item.id !== id));
}
```

小型 Demo 中只有一个请求，直接写在组件里看不出问题。员工管理功能增加以后，列表页、详情页、编辑页和删除 Dialog 都可能发送请求：

```text
EmployeeListPage    → axios.get('/api/employees')
EmployeeDetailPage  → axios.get('/api/employees/1')
EmployeeEditPage    → axios.put('/api/employees/1')
DeleteDialog        → axios.delete('/api/employees/1')
```

这会逐渐产生以下问题：

- URL 和 HTTP 细节散落在多个组件；
- Backend 修改路径或字段时，不知道要修改多少处；
- 每个组件可能用不同方式处理同一响应；
- UI 代码中混入大量通信细节，主要流程不容易阅读；
- 测试组件时必须同时处理 Axios；
- 新成员很难快速找到项目有哪些 Employee API。

建立 `employeeService.js` 后，接口入口集中在一个位置：

```text
EmployeeListPage ───┐
EmployeeDetailPage ─┼→ employeeService → httpClient → Backend
EmployeeEditPage ───┤
DeleteDialog ───────┘
```

### 4.3 Service 一般负责什么

业务 Service 通常负责以下内容：

| 职责 | Employee 示例 |
| --- | --- |
| 确定 API 路径 | `/employees/${id}` |
| 确定 HTTP Method | GET、POST、PUT、DELETE |
| 组织 Query 参数 | `keyword`、`department`、`page`、`size`、`sort` |
| 组织 Request Body | 新增和修改 Employee 的输入对象 |
| 传递请求选项 | `signal`、必要的 Header |
| 取出响应数据 | 从 Axios Response 取得 `response.data` |
| 转换外部字段 | `employee_code` 转为 `employeeCode` |
| 返回稳定结果 | 返回 Employee 或分页对象，而不是整个 Axios Response |

Service 的重点是“通信边界”。它把 Backend 的接口形式转换成前端项目内部容易使用的函数和数据。

### 4.4 Service 通常不负责什么

| 不应放入 Service 的处理 | 应放置的位置 |
| --- | --- |
| `setLoading(true)` | Page、Hook 或 Store |
| 打开或关闭 Dialog | Component / Page |
| 显示 Toast | Component / Page 的 UI 流程 |
| `navigate('/employees')` | Page 或事件处理逻辑 |
| 保存输入框文字 | Form Component |
| 返回 JSX | Component |

下面的写法把通信层和 UI 混在了一起：

```js
// 不推荐
export async function deleteEmployee(id) {
  await httpClient.delete(`/employees/${id}`);
  alert('删除成功');
  window.location.href = '/employees';
}
```

Service 无法知道当前页面应该显示 Toast、关闭 Dialog、停留原页还是返回列表。它只负责请求成功或抛出错误，由调用方决定 UI 行为。

```js
// Service：只负责通信
export async function deleteEmployee(id) {
  await httpClient.delete(`/employees/${id}`);
}
```

```jsx
// Page / Component：负责用户流程
async function handleDelete(id) {
  setDeleting(true);

  try {
    await deleteEmployee(id);
    setDialogOpen(false);
    setMessage('删除成功');
  } catch {
    setErrorMessage('删除失败，请重试');
  } finally {
    setDeleting(false);
  }
}
```

### 4.5 `httpClient` 与业务 Service 的区别

这两个文件都会出现 Axios，但职责不同。

`src/services/httpClient.js` 保存所有 API 共用的技术配置：

```js
import axios from 'axios';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
});
```

`features/employees/services/employeeService.js` 保存 Employee 专用接口：

```text
httpClient
→ 所有业务共用：服务器地址、超时、Cookie 等

employeeService
→ Employee 专用：接口路径、查询参数、请求 Body、响应转换
```

不要把所有业务接口都塞进 `httpClient.js`。否则这个文件会同时包含员工、部门、认证等大量无关业务路径，再次失去职责边界。

### 4.6 一个完整的 Employee Service

```js
// features/employees/services/employeeService.js
import { httpClient } from '../../../services/httpClient';
import { normalizeEmployee } from '../models/employee';

export async function getEmployees(signal) {
  const response = await httpClient.get('/employees', { signal });
  return response.data.map(normalizeEmployee);
}

export async function searchEmployees(query, signal) {
  const response = await httpClient.get('/employees', {
    params: query,
    signal,
  });

  return {
    items: response.data.items.map(normalizeEmployee),
    page: response.data.page,
    size: response.data.size,
    total: response.data.total,
  };
}

export async function getEmployee(id, signal) {
  const response = await httpClient.get(`/employees/${id}`, { signal });
  return normalizeEmployee(response.data);
}

export async function createEmployee(input) {
  const response = await httpClient.post('/employees', input);
  return normalizeEmployee(response.data);
}

export async function updateEmployee(id, input) {
  const response = await httpClient.put(`/employees/${id}`, input);
  return normalizeEmployee(response.data);
}

export async function deleteEmployee(id) {
  await httpClient.delete(`/employees/${id}`);
}
```

逐个理解：

- `getEmployees(signal)`：简单取得全部员工，返回 Employee 数组。
- `searchEmployees(query, signal)`：把搜索、筛选、分页和排序作为 Query 参数发送，返回分页对象。
- `getEmployee(id, signal)`：根据 ID 读取一个员工。
- `createEmployee(input)`：把新增内容作为 POST Body 发送。
- `updateEmployee(id, input)`：把修改内容作为 PUT Body 发送。
- `deleteEmployee(id)`：删除成功时不需要返回业务数据，因此只等待请求完成。

`signal` 来自 `AbortController`，页面切换或查询条件变化时可以用它取消已经不需要的读取请求。新增、修改、删除是否允许取消，要根据业务规格决定。

### 4.7 为什么 Service 返回 `response.data`

Axios 返回的 Response 包含：

```text
response.data
response.status
response.headers
response.config
...
```

大多数业务组件只需要数据。如果 Service 直接返回整个 Response，调用方会到处写：

```js
const response = await getEmployee(id);
setEmployee(response.data);
```

Service 返回统一的 Employee 后，组件可以写成：

```js
const employee = await getEmployee(id);
setEmployee(employee);
```

只有下载文件、读取特定响应头等确实需要 Response 信息的接口，才应设计相应的返回结构。

### 4.8 Service 如何处理错误

最基础的 Service 不要吞掉错误：

```js
// 不推荐：失败后返回空数组，调用方会误以为读取成功但没有数据
export async function getEmployees() {
  try {
    const response = await httpClient.get('/employees');
    return response.data;
  } catch {
    return [];
  }
}
```

保持请求失败状态，让 Page、Hook 或 Store 捕获并显示对应 UI：

```js
export async function getEmployees() {
  const response = await httpClient.get('/employees');
  return response.data.map(normalizeEmployee);
}
```

```jsx
try {
  const employees = await getEmployees();
  setEmployees(employees);
} catch {
  setErrorMessage('员工列表读取失败');
}
```

如果项目规定统一错误结构，可以在 `httpClient` 的响应处理或 Service 边界转换错误，但必须保持同一种策略，不能有些 Service 抛出 AxiosError、有些返回 `null`、有些返回空数组。

### 4.9 什么时候应该建立 Service

以下情况建议建立业务 Service：

- 页面开始调用 Backend API；
- 同一业务存在列表、详情、新增、修改或删除等多个接口；
- 多个组件需要调用同一接口；
- 需要统一 Query、Body 或响应字段转换；
- 希望组件测试不直接依赖 Axios。

如果只是完全不涉及通信的计数器组件，不需要建立 Service。Service 是为 API 边界服务的，不是每个 React Component 都必须配一个 Service。

### 4.10 调查既有项目中的 Service

接手已有项目时，可以从页面中的函数名向下追踪：

```text
EmployeeListPage 中的 loadEmployees()
  ↓ 查找函数内部调用
searchEmployees(query, signal)
  ↓ 转到定义
employeeService.js
  ↓ 查看 httpClient.get() 的路径与 params
httpClient.js
  ↓ 查看 baseURL、timeout、Header
DevTools Network
  ↓ 确认实际 URL、Method、Query 和 Response
```

代码表示“应该怎样请求”，Network 表示浏览器“实际上请求了什么”。两者一起确认，才能判断问题发生在页面参数、Service、共通 Axios 配置还是 Backend。

### 4.11 Service 常见错误

- Service 中操作 Dialog、Toast 或 Router：把 UI 流程留给 Page。
- 每个函数重新 `axios.create()`：共通技术配置集中到 `httpClient`。
- 返回整个 Axios Response 让组件自己取数据：优先返回稳定业务数据。
- 捕获错误后返回空数组：会把失败伪装成正常 Empty。
- 在多个 Service 重复转换同一字段：统一边界和数据约定。
- 把任意排序字符串直接传给 Backend：前后端都应使用允许字段白名单。
- 文件名叫 Service，但里面只有 React State 和 JSX：文件名不能代替职责划分。

## 5. 数据契约与边界转换

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

本章示例把它作为 Employee 的统一数据结构。项目确定结构后，Page、Component、Hook 和 Service 都使用相同字段名，不要一部分代码写 `employeeCode`，另一部分又写 `code`。若 Backend 字段不同，应只在 Service 边界转换一次。

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

如果 Backend 返回 snake_case，应只在边界转换一次：

```js
export function normalizeEmployee(data) {
  return {
    id: data.id,
    employeeCode: data.employee_code,
    name: data.name,
    email: data.email,
    department: data.department,
    role: data.role,
    joinedDate: data.joined_date,
    status: data.status,
  };
}
```

不要让一部分组件使用 `employee_code`，另一部分使用 `employeeCode`。统一字段可以减少条件判断、拼写错误和接口改修范围。

分页响应固定包含 `items`、`page`、`size`、`total`；请求状态固定使用 `{ status: 'loading' }`、`{ status: 'success', data }` 或 `{ status: 'error', error }`。这些约定应写入 API 规格、函数注释或项目文档。服务器响应属于外部数据，必要时在 Service 边界使用 schema 或校验函数确认字段。

其他跨层数据也采用相同原则。例如认证状态可以统一为 `{ status, user }`，错误可以统一为 `{ kind, message }`。具体字段由项目规格决定，但不要在 Page、Store 和 Service 中分别设计不同结构。

### 5.1 Service 签名清单

本章 Employee Service 使用以下函数签名：

```text
getEmployees(signal)                          → 员工数组的 Promise（signal 可省略）
searchEmployees(query, signal)                → 分页响应对象的 Promise（signal 可省略）
getEmployee(id, signal)                       → 员工对象的 Promise（signal 可省略）
createEmployee(input)                         → 新员工对象的 Promise
updateEmployee(id, input)                     → 更新后员工对象的 Promise
deleteEmployee(id)                            → 无响应数据的 Promise
```

不带搜索和分页的简单画面可以使用 `getEmployees()`；需要搜索、筛选、分页或排序时使用 `searchEmployees()`。一个列表页面选择与其需求匹配的一种返回结构，不要在同一页面混用“数组”和“分页对象”两套响应。

## 6. 项目依赖方向与循环依赖

依赖应大致从 UI 向通信边界流动。下面的反向依赖有问题：

```text
EmployeeListPage → employeeService → EmployeeListPage
```

Service 导入 Page 后，通信层开始依赖 UI，可能形成循环导入，也无法独立测试。如果双方都需要同一个字段转换函数，应把函数下移到 `models` 或 `utils`，而不是让它们互相导入。

常见风险：

```text
EmployeeForm → employeeUtils → EmployeeForm
Page → Store → Page
Service → Component → Service
```

浏览器初始化错误、构建警告、导出值为 `undefined` 都可能与循环依赖有关。修正方法不是把所有代码合并，而是重新确认共同代码的归属。

## 7. 如何阅读一个陌生 React 项目

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

逐步确认：

1. `package.json`：启动命令、主要依赖和版本。
2. `main.jsx`：Provider、Router 和全局样式入口。
3. `App.jsx` 或 Router：应用从哪里进入页面。
4. Route：URL 对应哪个 Layout 和 Page。
5. Layout：页面共通外框以及 `Outlet` 在哪里。
6. Page：使用了哪些业务组件和 Hook。
7. Component / Hook：事件和请求从哪里触发。
8. Service：API URL、Method、参数和转换。
9. `httpClient`：base URL、timeout、Header 和拦截器。
10. DevTools Network：浏览器实际发送的请求是否与代码一致。

不要只依赖文件名猜测。使用编辑器的“转到定义”“查找引用”和实际 import，画出一条真实调用链。

## 8. 常见错误

- 为了显得规范预先创建大量空目录：有职责和实际文件时再拆分。
- 所有组件都放顶层 `components`：业务专用组件留在 Feature 内。
- Page 直接拼接 API URL：URL 和响应转换集中在 Service。
- Service 显示 Toast 或跳转页面：这些是 UI 流程职责。
- Store 保存每个输入框：没有共享需求的状态留在组件。
- 把所有函数都放 `utils`：有明确业务归属的函数放回 Feature。
- 只看目录名、不沿 import 调查：既有项目结构可能与推荐结构不同。

## 9. 实战练习

给定一个同时包含 Axios、搜索、删除、Dialog、格式转换和分页的 `EmployeeList.jsx`：

1. 标记其中属于 Page、Component、Hook、Service 和 Util 的代码。
2. 把散落的 GET、DELETE 移入 `employeeService`。
3. 拆出 `EmployeeSearchForm`、`EmployeeTable` 和 `Pagination`。
4. 保持 Page 负责组合流程，不要把所有 State 强行移入 Store。
5. 从 `/employees/:id/edit` 画出 Route → Page → Hook → Service → API 的依赖链。
6. 统一 Employee 与分页响应的字段约定。
7. 找到一个循环依赖风险并通过调整职责消除。
8. 用 Network 面板确认重构前后的 Request URL、Method 和 Query 没有变化。

## 本章检查点

- [ ] Employee、查询条件、请求状态与分页响应各有唯一数据约定。
- [ ] 能沿 Page/Hook → Service → httpClient → Backend 调查请求。
- [ ] 能根据页面是否需要查询和分页，选择 `getEmployees` 或 `searchEmployees`。
- [ ] 能说明 Page、Component、Hook、Service、Store 和 Util 的职责。
- [ ] 能按照入口顺序阅读陌生 React 项目，而不是只凭目录名猜测。
