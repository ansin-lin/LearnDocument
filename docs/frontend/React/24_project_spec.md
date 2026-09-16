# 第 24 章 实战项目规格与起始状态

## 本章目标

- 按规格建立完整项目，不边写边猜业务规则。
- 明确目录、数据契约、角色、页面、完成条件和验证证据。
- 创建可构建、可测试的起始状态。

## 1. 业务规格

系统供公司员工维护员工资料。

| 角色 | 查看 | 新增/编辑 | 删除 |
| --- | --- | --- | --- |
| ADMIN | 是 | 是 | 是 |
| USER | 是 | 否 | 否 |

页面：登录、员工列表、员工详情、新增、编辑、404、403。列表支持姓名/部门搜索、姓名排序与分页。删除前确认，写入期间阻止重复提交。

训练接口中，ADMIN 至少拥有 `employee:write` 与 `employee:delete`，USER 不拥有这两个权限。角色用于规格说明，前端实际判断服务端返回的 `permissions`；后端仍按自己的授权规则检查每次写请求。

## 2. API 契约

```text
POST /api/login
request:  { "email": "admin@example.com", "password": "training-only" }
response: { "user": { "id": 1, "name": "Admin", "permissions": [...] } }

GET /api/employees?keyword=&department=&page=1&size=10&sort=name,asc
response: { "items": [...], "page": 1, "size": 10, "total": 27 }
```

详情和写入沿用课程入口中的契约。密码仅为本地训练样例，不提交真实凭据。真实认证使用后端批准的 Cookie/Token 方案。

## 3. 页面状态

每个请求页面都必须覆盖：

```text
initial/checking → loading → empty 或 success
                         ↘ error → retry
```

表单覆盖 pristine、editing、invalid、saving、success、failure。离开有未保存草稿的页面时，按项目规格提示。

## 4. 起始目录

```text
employee-app/
├─ src/
│  ├─ components/
│  ├─ features/auth/
│  ├─ features/employees/
│  ├─ layouts/
│  ├─ router/
│  ├─ services/
│  ├─ stores/
│  ├─ test/
│  ├─ types/
│  ├─ App.tsx
│  └─ main.tsx
├─ .env.example
├─ package.json
├─ package-lock.json
└─ vite.config.ts
```

`.env.example` 只写非敏感变量名和安全示例值。先按课程入口的封版基线创建依赖并提交 `package-lock.json`；从全新环境或 CI 重建时执行：

```bash
npm ci
npm run dev
npm run test -- --run
npm run build
```

`npm ci` 会按锁文件精确重建，并在清单与锁文件不一致时失败。若脚手架脚本名称不同，统一到团队 `package.json`，不要在文档、CI 和本地使用三套名称。

### 4.1 最终 Router 基线

```text
BrowserRouter
└─ Routes
   ├─ /login → LoginPage
   └─ RequireAuth（未登录时回到 /login）
      └─ MainLayout（Header / Sidebar / Outlet）
         ├─ /forbidden → ForbiddenPage
         ├─ /employees → EmployeeListPage
         ├─ /employees/:id → EmployeeDetailPage
         └─ RequirePermission(employee:write)
            ├─ /employees/new → EmployeeCreatePage
            └─ /employees/:id/edit → EmployeeEditPage
```

Auth Guard 只决定能否进入已登录区域，MainLayout 只提供共同框架，Permission Guard 决定能否进入写页面，Page 负责当前业务。登录返回地址必须经过内部路径校验；权限按钮、页面 Guard 与后端授权三层都要验收。

## 5. 阶段验收

- 登录后进入列表，刷新仍按后端会话恢复身份。
- Anonymous 访问 `/employees` 转到登录，成功后安全返回原 URL。
- USER 可查看列表/详情，直接访问新增/编辑进入 403，直接调用写 API 也得到后端 403。
- ADMIN 可进入全部 CRUD 页面并完成写操作。
- 搜索、排序、分页可从 URL 恢复。
- 详情/新增/编辑/删除均处理 loading、error 和权限。
- 400/401/403/404/409/500、网络错误和超时有不同恢复路径。
- 关键流程有组件/集成测试，构建成功，无浏览器 Console 错误。
- README 记录环境、启动、测试、构建、接口前提和已知限制。

## 6. 练习

1. 把页面和权限整理为验收表。
2. 创建目录、依赖和空路由，确认 dev/test/build。
3. 用浏览器 Network 确认 API base URL，不提交本地秘密。
4. 评审契约中仍不明确的字段、状态码和并发规则，并先提出问题而非自行假设。

## 本章检查点

- [ ] 页面、角色、路由、接口和验收标准之间没有矛盾。
- [ ] 项目继续复用前面定义的 Auth、Service、类型与错误模型。
- [ ] 能从全新环境执行 dev、test 和 build 并保留证据。
