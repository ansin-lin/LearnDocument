# 第 12 章 React Router 与页面导航

## 本章目标

- 建立 URL → Route → Page Component 的关系。
- 使用 Link、NavLink、useNavigate、useParams、useLocation 和 Search Params。
- 用 Layout Route、Nested Route、Index Route 与 Outlet 组织企业系统页面。
- 为列表、详情、新增和编辑设计稳定 URL。

React 本身负责组件和画面更新，但不负责根据 URL 选择页面。React Router 为单页应用补充这项能力：URL 改变时，不重新加载整个 HTML，而是让匹配的 Page Component 显示出来。

```text
浏览器 URL → React Router 匹配 Route → 显示对应 Page Component
```

## 1. 从最小路由开始

安装课程使用的路由库：

```bash
npm install --save-exact react-router-dom@7.18.4
```

先建立三个最小页面：

```jsx
function HomePage() {
  return <h1>首页</h1>;
}

function EmployeeListPage() {
  return <h1>员工列表</h1>;
}

function NotFoundPage() {
  return <h1>页面不存在</h1>;
}
```

然后在 `src/router/AppRouter.jsx` 中建立 URL 与页面的对应关系：

```jsx
import { BrowserRouter, Route, Routes } from 'react-router-dom';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/employees" element={<EmployeeListPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
```

| 接口 | 作用 | 当前写法的结果 |
| --- | --- | --- |
| `BrowserRouter` | 让子组件使用浏览器 URL 路由 | 整个路由树只包一次 |
| `Routes` | 从子 `Route` 中选择匹配项 | 显示最合适的路由 |
| `Route` | 声明路径与组件的对应 | `path` 匹配后渲染 `element` |
| `path="*"` | 匹配前面都未命中的路径 | 显示 404 页面 |

在 `src/main.jsx` 中渲染 `AppRouter`，访问 `/`、`/employees` 和不存在的路径，确认三个结果不同。

## 2. 设计业务路由表

```text
/login                 LoginPage
/forbidden             ForbiddenPage
/employees             EmployeeListPage
/employees/new         EmployeeCreatePage
/employees/:id         EmployeeDetailPage
/employees/:id/edit    EmployeeEditPage
```

```jsx
// src/router/AppRouter.jsx（页面组件的 import 省略）
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

下面先观察课程最终会使用的路由骨架。`RequireAuth` 和 `RequirePermission` 的实现放在第 15 章；当前重点是读懂父子路由与布局关系。`MainLayout` 负责共通外壳，Page 负责当前 URL 的业务内容。版本升级可能改变推荐配置方式，既有项目应以 [React Router 官方文档](https://reactrouter.com/) 和自己的锁定版本为准。

### 2.1 Layout Route、Nested Route 与 Outlet

上面的无 `path` Route 是 **Layout Route**。它不增加 URL 片段，可以承载门禁或共通布局。子 Route 是 **Nested Route**；`index` Route 在父级 URL 恰好匹配时显示默认内容。

```jsx
// src/layouts/MainLayout.jsx
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

## 3. 声明式和命令式导航

```jsx
<Link to={`/employees/${employee.id}`}>{employee.name}</Link>
<NavLink to="/employees">员工管理</NavLink>
```

普通可点击导航优先使用 Link，它保留链接语义。提交成功等流程性跳转使用：

```jsx
const navigate = useNavigate();
await createEmployee(input);
navigate('/employees');
```

不要用点击事件配合 `window.location` 代替应用内链接，否则会丢失 SPA 导航语义并整页刷新。

`NavLink` 与 `Link` 都能导航，但 `NavLink` 还能根据当前 URL 判断菜单是否激活，适合 Sidebar 和 Header 菜单。普通正文链接使用 `Link` 即可。

## 4. 路径参数

```jsx
const { id } = useParams();
const employeeId = Number(id);

if (!Number.isInteger(employeeId) || employeeId <= 0) {
  return <p role="alert">员工 ID 不正确</p>;
}
```

URL 输入不可信，路径参数始终需要按字符串读取并转换、校验，不能假设它天然是有效数字。

路由中的 `:id` 表示可变化的一段。例如 `/employees/12` 匹配 `/employees/:id`，`useParams()` 得到的 `id` 是字符串 `'12'`，不是数字。

## 5. Search Params

列表的关键字、页码和排序适合放在 URL，以支持刷新、收藏和共享：

```jsx
const [searchParams, setSearchParams] = useSearchParams();
const keyword = searchParams.get('keyword') ?? '';
const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1);

setSearchParams({ keyword: nextKeyword, page: '1' });
```

路径参数通常表示“哪一件资源”，Search Params 通常表示“怎样筛选或显示”。

| URL 部分 | 示例 | 适合保存 |
| --- | --- | --- |
| Path Param | `/employees/12` | 员工 ID |
| Search Params | `/employees?page=2&keyword=田中` | 页码、关键字、排序 |

## 6. useLocation 与 location.state

`useLocation` 返回当前位置对象。`pathname` 是当前路径；`state` 是一次客户端导航附带的临时信息，不会显示在 URL 中。

```jsx
const location = useLocation();

console.log(location.pathname); // 例如 /employees/12
navigate('/login', {
  replace: true,
  state: { from: location },
});
```

第 15 章的认证路由会使用 `state.from`，让用户登录后回到原页面。`location.state` 可能为空，也可能来自不可信的导航输入；读取前要检查结构，重定向目标只能接受应用内部允许的路径。需要刷新后仍保留的数据应写入 URL 或服务器，不能只依赖 `location.state`。

## 7. 页面返回与替换历史

```jsx
navigate(-1);
navigate('/employees', { replace: true });
```

`navigate(-1)` 返回浏览器历史中的上一页；`replace: true` 用新位置替换当前历史记录，适合登录跳转或提交成功后不希望返回旧提交页的场景。不要把“返回列表”一律写成 `navigate(-1)`，因为用户可能从外部链接直接进入详情页。

## 8. 常见错误

- 把 Layout 的 `Header` 复制到每个 Page：导航和权限显示容易不一致，应使用 Layout Route。
- 父路由有子路由却没有 `Outlet`：URL 匹配但子页面没有显示。
- 把必须刷新保留的搜索条件放进 `location.state`：刷新后可能丢失，应使用 Search Params。
- 把任意 `state.from` 直接用于跳转：可能产生开放重定向，应校验为允许的内部路径。

## 9. 服务端配置与练习

BrowserRouter 的深层 URL 被直接刷新时，Web 服务器需要回退到 SPA 入口，同时不能把真实静态资源和 API 404 都误回退为 HTML。

练习：建立五个路由；用 MainLayout 和 Outlet 共用导航；列表链接到详情；详情跳编辑；提交后返回列表；用 Search Params 保存关键字和页码；用 `location.state` 暂存登录前位置；输入非法 ID 与未知路径并验证错误页。

## 本章检查点

- [ ] 能解释 URL、Route、Page、Layout 和 Outlet 的关系。
- [ ] 能区分路径参数、Search Params 与 `location.state` 的保存范围。
- [ ] 能建立列表、详情、新增、编辑与 404 路由并通过刷新验证。
