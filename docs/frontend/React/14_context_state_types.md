# 第 14 章 Context、状态分类与 useReducer

## 本章目标

- 识别 Local UI、Form、Shared、Global 和 Server State。
- 用 Context 减少深层 Props 透传。
- 用 useReducer 集中表达相关状态变化，为 Redux 建立概念桥梁。
- 避免把所有状态都全局化。

## 1. 先分类，再选工具

| 类型 | 员工系统示例 | 常见位置 |
| --- | --- | --- |
| Local UI | Modal 是否打开、当前 Tab | 使用它的组件 |
| Form | 姓名、邮箱、字段错误 | 表单组件/表单 Hook |
| Shared | 搜索条件与结果列表共享 | 最近共同父组件 |
| Global | 登录用户、语言、主题、权限 | Context 或 Store |
| Server | API 员工列表、读取时间、缓存 | 请求 Hook/服务器状态库 |

“项目大”不是使用 Redux 的充分理由。先看共享范围、更新方式、调试要求和服务器缓存需求。

## 2. Context 解决什么问题

```text
App → Layout → Header → UserMenu
          每层都只为继续传 user
```

```tsx
// src/features/auth/authTypes.ts
export type AuthUser = {
  id: number;
  name: string;
  permissions: string[];
};

export type AuthState =
  | { status: 'checking'; user: null }
  | { status: 'anonymous'; user: null }
  | { status: 'authenticated'; user: AuthUser };

export type AuthContextValue = AuthState & {
  setUser: (user: AuthUser | null) => void;
  logout: () => Promise<void>;
};
```

`AuthState` 使用判别联合：只有 `authenticated` 状态允许出现非空 user，避免“状态说已登录但 user 仍是 null”的矛盾。第 15、16、25 章都沿用这套类型。

```tsx
// src/features/auth/AuthContext.tsx（核心实现）
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { getCurrentUser, logout as logoutApi } from './authService';
import type { AuthContextValue, AuthState, AuthUser } from './authTypes';

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>({
    status: 'checking',
    user: null,
  });
  const [restoreError, setRestoreError] = useState<string | null>(null);
  const restoreControllerRef = useRef<AbortController | null>(null);

  const setUser = useCallback((user: AuthUser | null) => {
    setAuthState(
      user
        ? { status: 'authenticated', user }
        : { status: 'anonymous', user: null },
    );
  }, []);

  const restoreSession = useCallback(async () => {
    restoreControllerRef.current?.abort();
    const controller = new AbortController();
    restoreControllerRef.current = controller;
    setAuthState({ status: 'checking', user: null });
    setRestoreError(null);

    try {
      setUser(await getCurrentUser(controller.signal));
    } catch {
      if (!controller.signal.aborted) {
        setRestoreError('登录状态确认失败，请重试');
      }
    }
  }, [setUser]);

  useEffect(() => {
    void restoreSession();
    return () => restoreControllerRef.current?.abort();
  }, [restoreSession]);

  async function logout() {
    try {
      await logoutApi();
    } finally {
      setUser(null);
    }
  }

  if (restoreError) {
    return (
      <section role="alert">
        <p>{restoreError}</p>
        <button type="button" onClick={() => void restoreSession()}>
          重试
        </button>
      </section>
    );
  }

  return (
    <AuthContext.Provider value={{ ...authState, setUser, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth 必须在 AuthProvider 内使用');
  return context;
}
```

这里在 `finally` 清除前端身份，是本课程选择的退出策略：即使退出 API 暂时失败，也不继续把当前浏览器画面当作已登录。它不是所有系统的通用答案。采用服务端 Session、可撤销 Token 或离线策略时，应根据威胁模型、后端返回和产品要求决定是否重试、是否保留提示，以及怎样使服务器凭据真正失效；前端清空 State 本身不能撤销服务端会话。

`getCurrentUser(signal)` 是认证 Service：有效会话返回 `AuthUser`，无会话返回 `null`；网络失败则抛出错误并显示重试，不会被误判成 anonymous。Provider 之下的组件可读取值。Context 更新会让读取该 Context 的组件重新渲染。Context 是传递机制，不自动提供复杂 Action、时间旅行、缓存或持久化。

## 3. useReducer：集中相关状态变化

当多个 State 总是一起变化，用分散 Setter 容易漏改。例如关键字变化时必须回到第 1 页：

```tsx
type SearchState = { keyword: string; page: number };
type SearchAction =
  | { type: 'keywordChanged'; keyword: string }
  | { type: 'pageChanged'; page: number };

function searchReducer(state: SearchState, action: SearchAction): SearchState {
  switch (action.type) {
    case 'keywordChanged':
      return { ...state, keyword: action.keyword, page: 1 };
    case 'pageChanged':
      return { ...state, page: action.page };
  }
}

const [searchState, dispatch] = useReducer(searchReducer, {
  keyword: '',
  page: 1,
});

dispatch({ type: 'keywordChanged', keyword: '田中' });
```

- State：当前数据。
- Action：发生了什么，以及这次变化所需的数据。
- Reducer：根据旧 State 和 Action 纯计算新 State。
- Dispatch：把 Action 交给 Reducer。

`useReducer` 仍是当前组件的局部状态，不会自动成为全局 Store。第 16 章 Redux Toolkit 会复用这些词汇并增加集中 Store 与 Selector。

## 4. 选择指南

- `useState`：局部且直接。
- 状态提升：少数相邻组件共享。
- Context：低到中频、跨层级的全局依赖，如主题、认证接口。
- Zustand/Redux：跨区域状态有较多 Action、选择订阅或团队约束。
- 服务器状态库：缓存、失效、重试、后台刷新成为主要问题时评估。

不要把 API 返回的所有数据永久复制到多个 Store。决定谁是事实来源，并定义刷新/失效策略。

## 5. 常见错误

- Context 同时出现 `authenticated + null user`：使用统一判别联合消除无效组合。
- Reducer 内请求 API 或修改外部变量：Reducer 必须保持纯粹，副作用放事件或 Effect。
- 把所有 API 数据放进 Context：服务器状态的缓存、失效与重新获取问题没有因此消失。

## 6. 练习

1. 给十个状态分类并说明所有者。
2. 用 AuthContext 消除三层 user 透传。
3. 把 Modal 状态从全局 Store 移回页面并说明收益。
4. 观察 Context value 更新造成的渲染范围，考虑按职责拆分 Context。
5. 用 `useReducer` 实现关键字变化自动重置页码，并为两种 Action 做测试。

## 本章检查点

- [ ] 能用统一 AuthState 表达检查中、未登录和已登录。
- [ ] 能解释 Context 传递机制与全局状态库的区别。
- [ ] 能说明 State、Action、Reducer、Dispatch，并保持 Reducer 纯粹。
