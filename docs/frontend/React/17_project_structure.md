# 第 17 章 项目目录、API Layer 与类型

## 本章目标

- 按职责组织页面、组件、请求、状态与类型。
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
├─ types/
├─ utils/
├─ App.tsx
└─ main.tsx
```

小项目不必一次创建所有空目录。先按职责建立最少结构，功能增长后再按 feature 聚合。目录名不是架构本身，关键是依赖方向和所有权一致。

## 2. API Layer

不推荐在每个组件直接 `axios.get(...)`。组件只表达用户流程：

```text
Page / Hook → employeeService → httpClient → Backend
```

- `httpClient`：base URL、timeout、通用请求/响应处理。
- `employeeService`：员工接口路径、参数和返回类型。
- Hook/Page：loading、页面状态、取消与用户消息。

拦截器应保持克制。若在拦截器中刷新令牌，要处理并发刷新、重试上限和失败退出，避免无限重试。

## 3. 类型位置

```tsx
// src/types/employee.ts
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';
export type Employee = {
  id: number;
  name: string;
  email: string;
  department: string;
  role: 'ADMIN' | 'USER';
  joinedDate: string;
  status: EmployeeStatus;
};
export type EmployeeInput = Omit<Employee, 'id'>;

export type EmployeeQuery = {
  keyword: string;
  department: string;
  page: number;
  size: number;
  sort: 'name,asc' | 'name,desc';
};

// src/types/pagination.ts
export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
};

// src/types/asyncState.ts
export type AsyncState<T, E = string> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: E };
```

默认的 `string` 只服务于前期独立小示例。进入第 24～26 章正式项目后，请求状态必须显式写成 `AsyncState<T, AppError>`；接收该状态的 `ErrorPanel` 也统一接收 `AppError`，不能让同一组件有时收到字符串、有时收到结构化错误。

认证类型统一使用第 14 章 `features/auth/authTypes.ts` 的 `AuthUser`、`AuthState` 与 `AuthContextValue`；错误类型使用第 18 章的 `AppError`。只被一个组件使用的小 Props 类型可就近放置；跨 Service/Page 共享的接口契约放在功能目录或 types。不要建立一个包含所有项目类型的巨大文件，也不要在 Page、Store 和 Context 中分别重新声明同名业务模型。

### 3.1 Service 签名清单

项目统一沿用第 10～11 章的函数：

```text
getEmployees(signal?)                         → Promise<Employee[]>（基础练习）
searchEmployees(query, signal?)               → Promise<PageResponse<Employee>>
getEmployee(id, signal?)                      → Promise<Employee>
createEmployee(input)                         → Promise<Employee>
updateEmployee(id, input)                     → Promise<Employee>
deleteEmployee(id)                            → Promise<void>
```

实战列表使用 `searchEmployees`；基础 `getEmployees` 只保留为学习取消请求的单点示例。不要在实战中混用两套列表响应。

## 4. 入口调查

```text
package.json
  ↓ scripts/dependencies
main.tsx
  ↓ Provider/Router
AppRouter
  ↓ route
EmployeeListPage
  ↓ hook/component
employeeService
  ↓ httpClient
Backend API
```

## 5. 练习

1. 把散落在三个组件中的 GET 移入 Service。
2. 统一 Employee 与分页响应类型。
3. 从 `/employees/:id/edit` 画出文件依赖链。
4. 找到一个循环依赖风险并通过调整职责消除。

## 本章检查点

- [ ] Employee、EmployeeInput、EmployeeQuery、AuthState、AsyncState 与 PageResponse 各有唯一主要定义。
- [ ] 能沿 Page/Hook → Service → httpClient → Backend 调查请求。
- [ ] 能解释基础 getEmployees 与项目 searchEmployees 的迁移边界。
