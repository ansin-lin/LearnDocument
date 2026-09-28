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

判断顺序可以是：

1. 只有一个组件使用：留在当前组件。
2. 父子组件使用：通过 Props 传递或提升到最近共同父组件。
3. 很深的不同区域都需要：考虑 Context。
4. 变化和业务 Action 较复杂：再评估第 16 章的 Store。

## 2. Context 解决什么问题

```text
App → Layout → Header → UserMenu
          每层都只为继续传 user
```

这种中间组件不使用数据、只负责继续传递的情况称为 Props Drilling。Context 让上层 Provider 直接向后代组件提供数据。

### 2.1 Context 的最小写法

```jsx
import { createContext, useContext } from 'react';

const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  return (
    <ThemeContext.Provider value="light">
      {children}
    </ThemeContext.Provider>
  );
}

export function ThemeLabel() {
  const theme = useContext(ThemeContext);
  return <p>当前主题：{theme}</p>;
}
```

- `createContext(defaultValue)` 创建 Context 对象；本例用 `null` 表示没有 Provider。
- `Provider` 的 `value` 是要提供给后代的数据。
- `useContext(ThemeContext)` 读取距离当前组件最近的 Provider 值。
- `children` 表示包在 Provider 标签内部的组件内容。

只有需要读取主题的组件才调用 `useContext`，中间层不再转交 `theme` Props。

### 2.2 从最小 Context 到认证 Context

认证状态统一使用下面三种对象结构：

```text
{ status: 'checking', user: null }
{ status: 'anonymous', user: null }
{ status: 'authenticated', user: { id, name, permissions } }
```

只有 `authenticated` 状态允许出现非空 `user`，这样可以避免“状态说已登录但 user 仍是 null”的矛盾。第 15、16、25 章都沿用这套数据约定。

```jsx
// src/features/auth/AuthContext.jsx（核心实现）
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import { getCurrentUser, logout as logoutApi } from './authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [authState, setAuthState] = useState({
    status: 'checking',
    user: null,
  });
  const [restoreError, setRestoreError] = useState(null);
  const restoreControllerRef = useRef(null);

  const setUser = useCallback((user) => {
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

`useAuth()` 把读取 Context 和缺少 Provider 时的检查集中起来。其他组件不应直接重复 `useContext(AuthContext)`。

这里在 `finally` 清除前端身份，是本课程选择的退出策略：即使退出 API 暂时失败，也不继续把当前浏览器画面当作已登录。它不是所有系统的通用答案。采用服务端 Session、可撤销 Token 或离线策略时，应根据威胁模型、后端返回和产品要求决定是否重试、是否保留提示，以及怎样使服务器凭据真正失效；前端清空 State 本身不能撤销服务端会话。

`getCurrentUser(signal)` 是认证 Service：有效会话返回包含 `id`、`name`、`permissions` 的用户对象，无会话返回 `null`；网络失败则抛出错误并显示重试，不会被误判成 anonymous。Provider 之下的组件可读取值。Context 更新会让读取该 Context 的组件重新渲染。Context 是传递机制，不自动提供复杂 Action、时间旅行、缓存或持久化。

## 3. useReducer：集中相关状态变化

当多个 State 总是一起变化，用分散 Setter 容易漏改。例如关键字变化时必须回到第 1 页：

```jsx
function searchReducer(state, action) {
  switch (action.type) {
    case 'keywordChanged':
      return { ...state, keyword: action.keyword, page: 1 };
    case 'pageChanged':
      return { ...state, page: action.page };
    default:
      throw new Error(`未知的 action：${action.type}`);
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

执行过程如下：

```text
用户输入关键字
  ↓
dispatch({ type: 'keywordChanged', keyword: '田中' })
  ↓
React 调用 searchReducer(旧 State, Action)
  ↓
Reducer 返回 { keyword: '田中', page: 1 }
  ↓
组件使用新 State 重新渲染
```

Reducer 不直接修改原 State，不请求 API，也不操作 DOM。相同的 State 和 Action 应得到相同结果，便于测试和调查。

`useReducer` 仍是当前组件的局部状态，不会自动成为全局 Store。第 16 章 Redux Toolkit 会复用这些词汇并增加集中 Store 与 Selector。

### 3.1 什么时候使用 useReducer

适合：多个字段经常一起变化，或希望用明确的 Action 表达业务事件。

不必使用：只有一个简单开关或输入值时，`useState` 更直接。

Context 与 `useReducer` 可以组合，让多个后代读取 State 和 Dispatch；但组合后仍没有自动持久化、服务器缓存和开发工具，不应把它描述成 Redux。

## 4. 选择指南

- `useState`：局部且直接。
- 状态提升：少数相邻组件共享。
- Context：低到中频、跨层级的全局依赖，如主题、认证接口。
- Zustand/Redux：跨区域状态有较多 Action、选择订阅或团队约束。
- 服务器状态库：缓存、失效、重试、后台刷新成为主要问题时评估。

不要把 API 返回的所有数据永久复制到多个 Store。决定谁是事实来源，并定义刷新/失效策略。

Context 的 `value` 改变时，读取该 Context 的组件会重新渲染。把主题、认证、高频输入和大型业务数据全部塞进同一个 Context，会扩大更新范围。按稳定职责拆分 Context，但不要为每个值建立一个 Context。

## 5. 常见错误

- Context 同时出现 `authenticated + null user`：统一状态对象结构，避免无效组合。
- Reducer 内请求 API 或修改外部变量：Reducer 必须保持纯粹，副作用放事件或 Effect。
- 把所有 API 数据放进 Context：服务器状态的缓存、失效与重新获取问题没有因此消失。

## 6. 练习

1. 给十个状态分类并说明所有者。
2. 用 AuthContext 消除三层 user 透传。
3. 把 Modal 状态从全局 Store 移回页面并说明收益。
4. 观察 Context value 更新造成的渲染范围，考虑按职责拆分 Context。
5. 用 `useReducer` 实现关键字变化自动重置页码，并为两种 Action 做测试。

## 本章检查点

- [ ] 能用统一认证状态对象表达检查中、未登录和已登录。
- [ ] 能解释 Context 传递机制与全局状态库的区别。
- [ ] 能说明 State、Action、Reducer、Dispatch，并保持 Reducer 纯粹。
