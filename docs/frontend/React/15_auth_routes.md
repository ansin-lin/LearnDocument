# 第 15 章 认证路由与权限边界

## 本章目标

- 区分认证（是谁）和授权（能做什么）。
- 实现最小 Protected Route 与登录后返回原页面。
- 理解前端路由保护不是安全边界。

## 1. 先区分认证与授权

| 概念 | 要回答的问题 | 示例 |
| --- | --- | --- |
| 认证 Authentication | 当前用户是谁 | 登录成功，取得当前用户 |
| 授权 Authorization | 当前用户能做什么 | 是否可以新增或删除员工 |

登录成功不代表拥有所有权限。前端负责显示适当页面和操作入口，后端负责最终安全检查。

## 2. 最小认证状态

本章直接使用第 14 章统一的认证状态对象和 `useAuth`，不再声明另一套认证数据：

```jsx
import { useAuth } from './AuthContext';
```

首次打开应用必须先确认会话。不要在确认完成前把用户当作未登录，否则会产生错误跳转闪烁。

| 状态 | `user` | 页面行为 |
| --- | --- | --- |
| `checking` | `null` | 显示确认中，不跳转 |
| `anonymous` | `null` | 可以进入登录页 |
| `authenticated` | 用户对象 | 可以继续检查权限 |

## 3. Protected Route

```jsx
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

```jsx
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

## 4. 登录提交与安全返回

认证 Service 负责 HTTP 通信。下面假设后端使用 Cookie Session，因此 Axios 需要允许发送 Cookie：

```js
// src/features/auth/authService.js
import { httpClient } from '../../services/httpClient';

export async function login(credentials) {
  const response = await httpClient.post('/auth/login', credentials, {
    withCredentials: true,
  });
  return response.data;
}
```

`credentials` 包含登录画面的账号和密码；响应返回当前用户，不把密码保存进 State。是否需要 `withCredentials` 以及 Cookie 属性由前后端认证方案决定。

登录页的主要流程：

```jsx
async function handleSubmit(event) {
  event.preventDefault();
  if (submitting) return;

  setSubmitting(true);
  setErrorMessage('');

  try {
    const user = await login({ loginId, password });
    setUser(user);
    navigate(getSafeReturnPath(location.state), { replace: true });
  } catch (error) {
    setErrorMessage('账号或密码不正确');
  } finally {
    setSubmitting(false);
  }
}
```

安全返回函数只接受应用内部路径：

```js
function getSafeReturnPath(state) {
  const pathname = state?.from?.pathname;

  if (typeof pathname === 'string' && pathname.startsWith('/')) {
    return pathname;
  }

  return '/employees';
}
```

实际项目还可以使用允许路径清单，并同时恢复 `search`。不能把外部 URL 或未经检查的输入直接交给 `navigate()`。

## 5. 权限控制

按钮级权限改善操作体验，页面级权限阻止用户通过地址栏进入不属于自己的画面：

```jsx
function EmployeeActions() {
  const auth = useAuth();
  const canDelete =
    auth.status === 'authenticated' &&
    auth.user.permissions.includes('employee:delete');

  return canDelete ? <DeleteButton /> : null;
}
```

```jsx
function RequirePermission({ permission }) {
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

```jsx
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

## 6. API 返回 401 时怎么办

即使页面最初已经通过 `RequireAuth`，Session 也可能在操作期间过期。API 返回 `401` 时，应用需要清除当前认证状态，并引导重新登录。`403` 则表示身份仍有效，但没有该操作权限，通常进入禁止访问页面或显示权限提示。

```text
401 → 身份无效 → 更新为 anonymous → 登录
403 → 身份有效但无权限 → Forbidden 或权限提示
```

是否统一在 Axios Interceptor 中处理，应根据项目设计决定。Interceptor 适合所有请求共同的技术处理；某个页面专用的错误 Message 仍由该页面负责。

## 7. Logout 的流程

```text
点击退出 → 调用后端 logout API → 清除前端认证状态 → 跳转登录页
```

只清除 React State 不能保证服务器 Session 或 Token 已失效。课程第 14 章采用即使退出 API 失败也清除当前浏览器身份的策略，但真实项目需按安全要求决定提示、重试与失效方式。

## 8. 常见错误

- Route、Header 和 Login 各存一份 user：退出后容易不一致，应统一通过 Auth Context/Store。
- `checking` 时立即跳登录：会话恢复成功后页面仍发生闪烁或错误导航。
- 把前端隐藏按钮当授权：后端仍必须检查权限并返回 403。
- 不校验 `location.state.from` 就导航：可能跳向不允许的位置。
- 把密码或敏感 Token 放进普通组件 State 后长期保存：只保留提交所需时间，并遵循项目认证方案。
- 登录成功后只跳转、不更新统一认证状态：路由仍会把用户判断为未登录。
- 把 401 和 403 显示成同一种错误：一个需要恢复身份，一个表示权限不足。

## 9. 练习

1. 实现 checking/authenticated/anonymous 三态。
2. 未登录访问详情时跳登录，成功后安全返回详情。
3. 分别模拟 401 和 403，验证不同 UI。
4. 隐藏删除按钮后直接调用 DELETE，说明为什么后端仍必须鉴权。
5. USER 直接输入 `/employees/new` 与 `/employees/1/edit`，验证进入 403 页面；ADMIN 能进入同一路径。
6. 让 Session 在员工编辑期间失效，模拟保存返回 401，确认能够返回登录页。
7. 修改 `state.from` 为外部地址，确认安全返回逻辑不会采用它。

## 本章检查点

- [ ] RequireAuth、Login、Logout 和权限显示使用同一认证状态。
- [ ] 能区分 checking、anonymous 与 authenticated 的页面行为。
- [ ] 能解释 401、403 和前端权限显示之间的边界。
- [ ] 新增/编辑同时具备按钮级、页面级与后端权限边界。
