# 第 15 章 认证路由与权限边界

## 本章目标

- 区分认证（是谁）和授权（能做什么）。
- 实现最小 Protected Route 与登录后返回原页面。
- 理解前端路由保护不是安全边界。

## 1. 最小认证状态

本章直接使用第 14 章 `authTypes.ts` 中统一的 `AuthUser`、`AuthState`、`AuthContextValue` 和 `useAuth`，不再声明另一套认证类型：

```tsx
import { useAuth } from './AuthContext';
```

首次打开应用必须先确认会话。不要在确认完成前把用户当作未登录，否则会产生错误跳转闪烁。

## 2. Protected Route

```tsx
import { Navigate, Outlet, useLocation } from 'react-router-dom';

function RequireAuth() {
  const auth = useAuth();
  const location = useLocation();

  if (auth.status === 'checking') {
    return <p role="status">登录状态确认中...</p>;
  }
  if (auth.status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
```

路由组合：

```tsx
<Route element={<RequireAuth />}>
  <Route element={<MainLayout />}>
    <Route path="forbidden" element={<ForbiddenPage />} />
    <Route path="employees" element={<EmployeeListPage />} />
  </Route>
</Route>
```

登录成功后验证 `from` 是否为应用允许的内部位置，再返回原页面；避免开放重定向。

统一认证流程如下：

```text
App 启动 → status=checking → getCurrentUser
                         ├─ 有会话 → authenticated + AuthUser
                         └─ 无会话 → anonymous + null

Login 成功 → setUser(user) → authenticated
Logout 完成/失败清理 → setUser(null) → anonymous
```

`RequireAuth` 只消费 Auth Context，不自行保存第二份登录 State。这样 Header、权限按钮、登录页和路由保护看到的是同一事实来源。

## 3. 权限控制

按钮级权限改善操作体验，页面级权限阻止用户通过地址栏进入不属于自己的画面：

```tsx
function EmployeeActions() {
  const auth = useAuth();
  const canDelete =
    auth.status === 'authenticated' &&
    auth.user.permissions.includes('employee:delete');

  return canDelete ? <DeleteButton /> : null;
}
```

```tsx
function RequirePermission({ permission }: { permission: string }) {
  const auth = useAuth();

  if (auth.status === 'checking') {
    return <p role="status">权限确认中...</p>;
  }
  if (auth.status === 'anonymous') {
    return <Navigate to="/login" replace />;
  }
  if (!auth.user.permissions.includes(permission)) {
    return <Navigate to="/forbidden" replace />;
  }
  return <Outlet />;
}
```

```tsx
<Route element={<RequireAuth />}>
  <Route element={<MainLayout />}>
    <Route path="forbidden" element={<ForbiddenPage />} />
    <Route path="employees" element={<EmployeeListPage />} />
    <Route path="employees/:id" element={<EmployeeDetailPage />} />
    <Route element={<RequirePermission permission="employee:write" />}>
      <Route path="employees/new" element={<EmployeeCreatePage />} />
      <Route path="employees/:id/edit" element={<EmployeeEditPage />} />
    </Route>
  </Route>
</Route>
```

权限必须形成三层一致边界：列表/详情中的写操作按钮按权限显示；新增、编辑等页面由路由门禁保护；后端对每个写接口再次校验并以 403 拒绝越权请求。前两层不能替代第三层。

这改善体验，但不是授权。攻击者可以直接发 HTTP 请求，后端必须对每个受保护接口检查会话/令牌和权限。401 表示需要认证恢复；403 表示当前身份无权访问。

认证信息的存储与 Cookie/Token 策略依赖后端架构。若使用 Cookie，应考虑 Secure、HttpOnly、SameSite 与 CSRF；不要在教程代码中放真实密钥，也不要声称 localStorage 可安全保存任何敏感凭据。

## 4. 常见错误

- Route、Header 和 Login 各存一份 user：退出后容易不一致，应统一通过 Auth Context/Store。
- `checking` 时立即跳登录：会话恢复成功后页面仍发生闪烁或错误导航。
- 把前端隐藏按钮当授权：后端仍必须检查权限并返回 403。
- 不校验 `location.state.from` 就导航：可能跳向不允许的位置。

## 5. 练习

1. 实现 checking/authenticated/anonymous 三态。
2. 未登录访问详情时跳登录，成功后安全返回详情。
3. 分别模拟 401 和 403，验证不同 UI。
4. 隐藏删除按钮后直接调用 DELETE，说明为什么后端仍必须鉴权。
5. USER 直接输入 `/employees/new` 与 `/employees/1/edit`，验证进入 403 页面；ADMIN 能进入同一路径。

## 本章检查点

- [ ] RequireAuth、Login、Logout 和权限显示使用同一 AuthState。
- [ ] 能区分 checking、anonymous 与 authenticated 的页面行为。
- [ ] 能解释 401、403 和前端权限显示之间的边界。
- [ ] 新增/编辑同时具备按钮级、页面级与后端权限边界。
