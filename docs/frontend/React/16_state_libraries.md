# 第 16 章 Zustand 与 Redux Toolkit

## 本章目标

- 理解 Store、State、Action、Selector 和 Dispatch。
- 判断什么时候需要全局状态库，什么时候不需要。
- 用 Zustand 完成轻量共享认证状态。
- 读懂 Redux Toolkit 的 Slice、Store、Provider 和组件调用链。

本章介绍两种方案是为了建立选型和既有项目阅读能力。一个项目通常根据团队规范选择一种主要全局状态方案，不要同时保存两份认证状态。

## 1. 为什么还需要状态库

前面已经学习过：

- `useState`：管理组件自己的状态；
- 状态提升：让少量相邻组件共享状态；
- Context：向深层组件传递认证、主题等全局依赖；
- `useReducer`：用 Action 集中描述相关状态变化。

当许多相距较远的组件都要读取、修改同一份客户端状态，而且更新事件越来越多时，独立 Store 可以让状态所有者和修改入口更明确。

```text
Header ───────┐
Sidebar ──────┼→ Auth Store ← Login Page
Route Guard ──┘
```

但以下内容通常不需要放入全局 Store：

- 当前输入框内容；
- 一个页面专用的 Dialog 开关；
- 只由父子组件共享的数据；
- 可以直接根据已有 State 计算的值。

API 数据也不能因为放进 Store 就自动拥有缓存、失效和重新读取能力。服务器仍是业务数据的最终来源。

## 2. 先掌握五个术语

| 术语 | 含义 | 认证示例 |
| --- | --- | --- |
| Store | 集中保存共享状态的对象 | Auth Store |
| State | 当前保存的数据 | `status`、`user` |
| Action | 有业务含义的状态操作 | `setUser()`、`logout()` |
| Selector | 从 Store 选择组件需要的数据 | 只读取用户名 |
| Dispatch | 把 Action 发送给 Redux Store | `dispatch(loggedOut())` |

Zustand 通常直接调用 Action；Redux 通常先创建 Action 对象，再通过 `dispatch()` 发送。

## 3. 先判断状态是否应该进入 Store

| 状态 | 推荐位置 | 理由 |
| --- | --- | --- |
| 登录用户、权限 | Context 或 Store | 多个远距离组件需要 |
| 表单输入 | 表单组件 | 只属于当前编辑过程 |
| 当前页码写入 URL | Search Params | 刷新、分享后仍可恢复 |
| 员工数据库数据 | Backend | 需要永久保存 |
| 员工列表当前副本 | Page、请求 Hook 或专用方案 | 需要加载、失效和重取策略 |

判断重点不是“能不能放”，而是它的共享范围、更新复杂度和事实来源。

## 4. Zustand：轻量共享状态

Zustand 提供较小的 API，可以不使用额外 Provider 就建立 Store。课程项目锁定以下版本：

```bash
npm install --save-exact zustand@5.0.15
```

### 4.1 建立 Auth Store

新建 `src/stores/authStore.js`：

```js
import { create } from 'zustand';

export const useAuthStore = create((set) => ({
  status: 'checking',
  user: null,

  setChecking: () => set({
    status: 'checking',
    user: null,
  }),

  setUser: (user) => set(
    user
      ? { status: 'authenticated', user }
      : { status: 'anonymous', user: null },
  ),

  clearAuth: () => set({
    status: 'anonymous',
    user: null,
  }),
}));
```

`create()` 接收 Store 建立函数并返回一个 React Hook。建立函数中的 `set()` 用于产生下一份 State。Store 继续沿用第 14～15 章的三种认证状态，不能出现 `authenticated` 但 `user` 为 `null` 的组合。

### 4.2 使用 Selector 读取状态

```jsx
import { useAuthStore } from '../stores/authStore';

export function UserSummary() {
  const status = useAuthStore((state) => state.status);
  const userName = useAuthStore((state) => state.user?.name ?? '');

  if (status !== 'authenticated') return null;

  return <p>登录用户：{userName}</p>;
}
```

传给 `useAuthStore()` 的函数就是 Selector。它从整个 Store 中选择当前组件需要的部分。不要习惯性写成 `useAuthStore()` 订阅整个 Store，否则任何字段变化都可能让组件重新渲染。

### 4.3 调用 Action 修改状态

登录成功时：

```jsx
const setUser = useAuthStore((state) => state.setUser);

async function handleLogin(credentials) {
  const user = await login(credentials);
  setUser(user);
}
```

退出时：

```jsx
const clearAuth = useAuthStore((state) => state.clearAuth);

async function handleLogout() {
  try {
    await logout();
  } finally {
    clearAuth();
  }
}
```

执行链路是：

```text
Component → Store Action → set() → Store State → Selector → Component
```

不要在多个组件中分别写 `setUser` 的替代逻辑，否则统一状态结构会再次分散。

### 4.4 Zustand 与第 15 章路由门禁

把 `useAuth()` 替换为 Store Selector 后，门禁仍然只消费统一状态：

```jsx
function RequireAuth() {
  const status = useAuthStore((state) => state.status);
  const location = useLocation();

  if (status === 'checking') {
    return <p role="status">登录状态确认中...</p>;
  }

  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
```

采用 Zustand 后，应移除原来负责同一认证数据的 Auth Context State。不要让 Header 使用 Zustand、Route 却继续使用另一份 Context 用户。

## 5. Redux Toolkit：可预测的数据流

Redux Toolkit 是官方推荐的 Redux 开发方式。它适合需要明确事件记录、统一约束，或已经使用 Redux 的项目。本章重点是能够搭建并读懂基本链路。

```bash
npm install --save-exact @reduxjs/toolkit@2.12.0 react-redux@9.3.0
```

### 5.1 Slice 同时定义 State 与 Reducer

新建 `src/stores/authSlice.js`：

```js
import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  status: 'checking',
  user: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    sessionChecking: () => ({
      status: 'checking',
      user: null,
    }),
    loginSucceeded: (_state, action) => ({
      status: 'authenticated',
      user: action.payload,
    }),
    loggedOut: () => ({
      status: 'anonymous',
      user: null,
    }),
  },
});

export const {
  sessionChecking,
  loginSucceeded,
  loggedOut,
} = authSlice.actions;

export default authSlice.reducer;
```

`createSlice()` 根据 `name`、初始 State 和 Reducer 定义生成：

- Reducer：计算下一份 State；
- Action Creator：建立 Action 对象；
- Action Type：例如 `auth/loginSucceeded`。

```js
console.log(loginSucceeded({ id: 1, name: '田中' }));
// { type: 'auth/loginSucceeded', payload: { id: 1, name: '田中' } }
```

`action.payload` 是调用 Action Creator 时传入的数据。Redux Toolkit 也允许在 Reducer 中写出修改 draft 的形式，因为内部使用 Immer 产生不可变结果；普通 React State 不能照搬这种写法。

### 5.2 建立唯一 Store

新建 `src/stores/store.js`：

```js
import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
  },
});
```

`configureStore()` 把各 Slice Reducer 组合为应用 Store。这里的 `auth` 决定认证状态位于 `state.auth`。

### 5.3 用 Provider 提供 Store

替换 `src/main.jsx` 的根渲染部分：

```jsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { App } from './App';
import { store } from './stores/store';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </StrictMode>,
);
```

`Provider` 让下层组件能够访问同一个 Redux Store。忘记包裹时，`useSelector()` 和 `useDispatch()` 会报找不到 Provider 的错误。

### 5.4 Selector 读取，Dispatch 修改

```jsx
import { useDispatch, useSelector } from 'react-redux';
import { loggedOut } from '../stores/authSlice';

export function LoginSummary() {
  const auth = useSelector((state) => state.auth);
  const dispatch = useDispatch();

  if (auth.status !== 'authenticated') return null;

  return (
    <p>
      {auth.user.name}
      <button
        type="button"
        onClick={() => dispatch(loggedOut())}
      >
        退出
      </button>
    </p>
  );
}
```

`useSelector()` 读取 Store；`useDispatch()` 取得 `dispatch` 函数；`dispatch(loggedOut())` 把 Action 送入 Redux。

```text
点击退出
  ↓
loggedOut() 建立 Action
  ↓
dispatch(Action)
  ↓
authSlice Reducer 计算新 State
  ↓
Store 保存新 State
  ↓
Selector 结果变化
  ↓
组件重新渲染
```

### 5.5 登录 API 放在哪里

Reducer 必须保持纯粹，不能在 Reducer 中调用登录 API。最容易理解的做法是组件先调用 Service，成功后再 Dispatch：

```jsx
async function handleLogin(credentials) {
  const user = await login(credentials);
  dispatch(loginSucceeded(user));
}
```

较复杂项目可能使用 `createAsyncThunk` 或其他异步方案。本课程主线先保持“Service 负责 HTTP，Reducer 负责同步状态变化”的清晰边界。

## 6. Zustand、Redux Toolkit 与 Context 怎样选择

| 方案 | 适合情况 | 主要特点 |
| --- | --- | --- |
| Context | 主题、认证接口等低到中频全局依赖 | React 内置，机制较少 |
| Zustand | 轻量客户端共享状态 | API 简洁，可直接选择订阅 |
| Redux Toolkit | Action 较多、团队约束和调查要求高 | 数据流明确，工具生态成熟 |

选择取决于项目规模之外的因素：既有技术栈、团队经验、状态变化复杂度、调试要求和维护周期。不要因为某个库流行就把所有局部 State 迁入 Store。

## 7. 调试状态变化

调查顺序：

1. 用户事件是否发生；
2. 是否调用正确 Action；
3. Action 参数或 Redux `payload` 是否正确；
4. Store State 是否变化；
5. Selector 是否选择了正确字段；
6. 组件是否根据 Selector 结果渲染。

Redux 项目可以使用 Redux DevTools 查看 Action 和 State 变化。Zustand 也可按项目配置开发工具中间件，但这不是完成最小 Store 的前提。

## 8. 常见错误

- Context、Zustand 和 Redux 同时保存同一用户：只保留一个事实来源。
- 把输入框和 Dialog 全部放进 Store：局部状态留在组件。
- Zustand 组件订阅整个 Store：使用 Selector 选择所需字段。
- 忘记 Redux `Provider`：组件无法取得 Store。
- 在 Reducer 中调用 API 或修改 DOM：副作用放在事件、Service 或专用异步层。
- Slice 使用另一套认证状态：继续沿用 `checking`、`anonymous`、`authenticated`。
- 把服务器数据放进 Store 后不设计重取：刷新、失效和错误问题仍然存在。

## 9. 练习

### 练习 1：Zustand 认证状态

建立 `authStore.js`，让 Header、Route Guard 和 Login Page 使用同一认证状态。登录和退出后分别验证三个位置是否同时变化。

### 练习 2：精简订阅

让 `UserSummary` 只订阅用户名。再增加一个不相关的 Store 字段，观察该字段变化时组件是否需要更新。

### 练习 3：阅读 Redux 数据流

实现 `authSlice.js`、`store.js` 和根 `Provider`。执行 `dispatch(loginSucceeded(user))`，通过 Redux DevTools 记录 Action Type、Payload 和更新后的 State。

### 练习 4：状态归属 Review

判断登录用户、员工表单、搜索页码、员工数据库数据、Dialog 开关分别应放在哪里，并写出判断理由。

参考：[Zustand 官方文档](https://zustand.docs.pmnd.rs/)、[Redux Toolkit Quick Start](https://redux-toolkit.js.org/tutorials/quick-start)。

## 本章检查点

- [ ] 能解释 Store、State、Action、Selector 和 Dispatch。
- [ ] 能判断局部 State、Context 和独立 Store 的适用范围。
- [ ] 能完成 Zustand 的 Store → Action → Selector → Component 链路。
- [ ] 能完成 Redux 的 Slice → Store → Provider → Component 链路。
- [ ] 能沿 Action → Reducer → Store → Selector 调查状态变化。
- [ ] 不会在同一项目中无意保存多份认证状态。
