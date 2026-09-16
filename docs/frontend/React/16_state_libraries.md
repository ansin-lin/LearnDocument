# 第 16 章 Zustand 与 Redux Toolkit

## 本章目标

- 理解 Store、State、Action、Selector 和 Dispatch。
- 用 Zustand 完成轻量登录状态。
- 用 Redux Toolkit 阅读企业既有项目的数据流。

## 1. Zustand：轻量 Store

安装时以 [Zustand 官方文档](https://zustand.docs.pmnd.rs/) 当前说明为准：

```bash
npm install --save-exact zustand@5.0.15
```

```tsx
import { create } from 'zustand';
import type { AuthState, AuthUser } from '../features/auth/authTypes';

type AuthStore = AuthState & {
  setUser: (user: AuthUser | null) => void;
};

export const useAuthStore = create<AuthStore>((set) => ({
  status: 'checking',
  user: null,
  setUser: (user) => set(
    user
      ? { status: 'authenticated', user }
      : { status: 'anonymous', user: null },
  ),
}));
```

组件只选择需要的部分：

```tsx
const userName = useAuthStore((state) =>
  state.status === 'authenticated' ? state.user.name : null,
);
```

```text
Component → Action → Store State → Selector → Component
```

不要无选择地订阅整个 Store；也不要把临时表单输入都全局化。

## 2. Redux Toolkit：可预测的数据流

```bash
npm install --save-exact @reduxjs/toolkit@2.12.0 react-redux@9.3.0
```

```tsx
// src/stores/authSlice.ts
import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { AuthState, AuthUser } from '../features/auth/authTypes';

const initialState: AuthState = { status: 'checking', user: null };

const authSlice = createSlice({
  name: 'auth',
  initialState: initialState as AuthState,
  reducers: {
    sessionChecking: (): AuthState => ({ status: 'checking', user: null }),
    loginSucceeded: (_state, action: PayloadAction<AuthUser>): AuthState => ({
      status: 'authenticated',
      user: action.payload,
    }),
    loggedOut: (): AuthState => ({ status: 'anonymous', user: null }),
  },
});

export const { sessionChecking, loginSucceeded, loggedOut } = authSlice.actions;
export default authSlice.reducer;
```

本例直接返回统一的 `AuthState`，因此不会产生 `authenticated + null user`。Redux Toolkit 也允许 Reducer 写出看似修改 draft state 的语法，因为它使用 Immer 生成不可变结果；不要把 draft 写法复制到普通 React State。

```tsx
// src/stores/store.ts
import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';

export const store = configureStore({ reducer: { auth: authReducer } });
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

在 `main.tsx` 用 Provider 把 Store 提供给组件树：

```tsx
import { Provider } from 'react-redux';
import { store } from './stores/store';

createRoot(document.getElementById('root')!).render(
  <Provider store={store}>
    <App />
  </Provider>,
);
```

React Redux 9.1+ 官方推荐在独立文件创建 typed hooks：

```tsx
// src/stores/hooks.ts
import { useDispatch, useSelector } from 'react-redux';
import type { AppDispatch, RootState } from './store';

export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppSelector = useSelector.withTypes<RootState>();
```

若既有项目锁定在不支持 `.withTypes()` 的 React Redux 版本，应使用该版本官方的 typed hook 写法，不要只为照抄示例擅自升级依赖。组件使用 Selector 读取、通过 Dispatch 发 Action：

```tsx
function LoginSummary() {
  const auth = useAppSelector((state) => state.auth);
  const dispatch = useAppDispatch();

  function handleLoginSuccess(user: AuthUser) {
    dispatch(loginSucceeded(user));
  }

  if (auth.status !== 'authenticated') return null;
  return (
    <p>
      {auth.user.name}
      <button type="button" onClick={() => dispatch(loggedOut())}>
        退出
      </button>
    </p>
  );
}
```

`handleLoginSuccess` 展示登录 API 成功后如何派发 Action；实际调用应放在登录提交成功分支。

完整链路：

```text
Component → dispatch(Action) → Slice Reducer → Store → Selector → Component
```

## 3. 如何选择

- Context：传递稳定的全局依赖，机制最少。
- Zustand：API 小、选择订阅直接，适合较轻的客户端共享状态。
- Redux Toolkit：事件/状态约束清晰、工具与团队惯例成熟，适合复杂客户端业务或既有 Redux 项目。

Server State 仍需缓存与失效策略。无论选哪一种，登录权限最终由后端决定。

## 4. 常见错误

- 忘记 Provider：组件无法读取 Redux Store。
- 直接使用未类型化的 dispatch/selector：异步 Action 与 RootState 容易失去类型检查。
- Redux Slice 再声明另一套 AuthState：Route、Header 与 Store 会出现不可能状态。
- 把员工 API 列表默认全部塞进 Redux：服务器状态的缓存与失效仍需单独设计。

## 5. 练习

1. 用 Zustand 保存登录用户，只订阅姓名。
2. 用 Redux Toolkit 实现登录成功/退出 Slice。
3. 画出一次 `dispatch(loginSucceeded(user))` 的完整流程。
4. 给员工编辑表单选状态工具，并用共享范围与更新复杂度说明理由。

参考：[Redux Toolkit Quick Start](https://redux-toolkit.js.org/tutorials/quick-start)。

## 本章检查点

- [ ] 能搭起 Slice、Store、Provider、typed hooks 与 Component 的完整链路。
- [ ] 能沿 Action → Reducer → Store → Selector 调查状态变化。
- [ ] 能根据共享范围选择 Context、Zustand 或 Redux，并单独判断 Server State。
