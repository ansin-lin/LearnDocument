# 第 12 章 React Router 与页面导航

## 本章目标

- 建立 URL → Route → Page Component 的关系。
- 使用 Link、NavLink、useNavigate、useParams、useLocation 和 Search Params。
- 用 Layout Route、Nested Route、Index Route 与 Outlet 组织企业系统页面。
- 为列表、详情、新增和编辑设计稳定 URL。

## 1. 安装与路由表

```bash
npm install --save-exact react-router-dom@7.18.4
```

```text
/login                 LoginPage
/forbidden             ForbiddenPage
/employees             EmployeeListPage
/employees/new         EmployeeCreatePage
/employees/:id         EmployeeDetailPage
/employees/:id/edit    EmployeeEditPage
```

```tsx
// src/router/AppRouter.tsx（页面组件的 import 省略）
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<MainLayout />}>
            <Route index element={<Navigate to="/employees" replace />} />
            <Route path="forbidden" element={<ForbiddenPage />} />
            <Route path="employees" element={<EmployeeListPage />} />
            <Route path="employees/:id" element={<EmployeeDetailPage />} />
            <Route element={<RequirePermission permission="employee:write" />}>
              <Route path="employees/new" element={<EmployeeCreatePage />} />
              <Route path="employees/:id/edit" element={<EmployeeEditPage />} />
            </Route>
          </Route>
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
```

这里先展示第 15 章将完成的最终路由骨架：`RequireAuth` 负责身份门禁，`RequirePermission` 负责页面级权限，`MainLayout` 负责共通外壳，Page 负责当前 URL 的业务内容。三个父级组件都通过 `Outlet` 显示匹配的子路由。版本升级可能改变推荐配置方式，既有项目应以 [React Router 官方文档](https://reactrouter.com/) 和自己的锁定版本为准。

### 1.1 Layout Route、Nested Route 与 Outlet

上面的无 `path` Route 是 **Layout Route**。它不增加 URL 片段，可以承载门禁或共通布局。子 Route 是 **Nested Route**；`index` Route 在父级 URL 恰好匹配时显示默认内容。

```tsx
// src/layouts/MainLayout.tsx
import { Outlet } from 'react-router-dom';

export function MainLayout() {
  return (
    <>
      <Header />
      <div className="app-shell">
        <Sidebar />
        <main>
          <Outlet />
        </main>
      </div>
    </>
  );
}
```

`Outlet` 显示当前匹配的子页面：

```text
App
└─ RequireAuth
   └─ MainLayout
      ├─ Header
      ├─ Sidebar
      └─ Outlet
         ├─ EmployeeListPage
         ├─ EmployeeDetailPage
         └─ RequirePermission → Create / Edit
```

布局组件保留框架与共通导航，Page 只负责当前 URL 对应的业务内容。不要在每个 Page 复制 Header 和 Sidebar。

## 2. 声明式和命令式导航

```tsx
<Link to={`/employees/${employee.id}`}>{employee.name}</Link>
<NavLink to="/employees">员工管理</NavLink>
```

普通可点击导航优先使用 Link，它保留链接语义。提交成功等流程性跳转使用：

```tsx
const navigate = useNavigate();
await createEmployee(input);
navigate('/employees');
```

不要用点击事件配合 `window.location` 代替应用内链接，否则会丢失 SPA 导航语义并整页刷新。

## 3. 路径参数

```tsx
const { id } = useParams<{ id: string }>();
const employeeId = Number(id);

if (!Number.isInteger(employeeId) || employeeId <= 0) {
  return <p role="alert">员工 ID 不正确</p>;
}
```

URL 输入不可信。TypeScript 泛型不会把运行时字符串自动变成数字。

## 4. Search Params

列表的关键字、页码和排序适合放在 URL，以支持刷新、收藏和共享：

```tsx
const [searchParams, setSearchParams] = useSearchParams();
const keyword = searchParams.get('keyword') ?? '';
const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1);

setSearchParams({ keyword: nextKeyword, page: '1' });
```

## 5. useLocation 与 location.state

`useLocation` 返回当前位置对象。`pathname` 是当前路径；`state` 是一次客户端导航附带的临时信息，不会显示在 URL 中。

```tsx
const location = useLocation();

console.log(location.pathname); // 例如 /employees/12
navigate('/login', {
  replace: true,
  state: { from: location },
});
```

第 15 章的认证路由会使用 `state.from`，让用户登录后回到原页面。`location.state` 可能为空，也可能来自不可信的导航输入；读取前要检查结构，重定向目标只能接受应用内部允许的路径。需要刷新后仍保留的数据应写入 URL 或服务器，不能只依赖 `location.state`。

## 6. 常见错误

- 把 Layout 的 `Header` 复制到每个 Page：导航和权限显示容易不一致，应使用 Layout Route。
- 父路由有子路由却没有 `Outlet`：URL 匹配但子页面没有显示。
- 把必须刷新保留的搜索条件放进 `location.state`：刷新后可能丢失，应使用 Search Params。
- 把任意 `state.from` 直接用于跳转：可能产生开放重定向，应校验为允许的内部路径。

## 7. 服务端配置与练习

BrowserRouter 的深层 URL 被直接刷新时，Web 服务器需要回退到 SPA 入口，同时不能把真实静态资源和 API 404 都误回退为 HTML。

练习：建立五个路由；用 MainLayout 和 Outlet 共用导航；列表链接到详情；详情跳编辑；提交后返回列表；用 Search Params 保存关键字和页码；用 `location.state` 暂存登录前位置；输入非法 ID 与未知路径并验证错误页。

## 本章检查点

- [ ] 能解释 URL、Route、Page、Layout 和 Outlet 的关系。
- [ ] 能区分路径参数、Search Params 与 `location.state` 的保存范围。
- [ ] 能建立列表、详情、新增、编辑与 404 路由并通过刷新验证。
