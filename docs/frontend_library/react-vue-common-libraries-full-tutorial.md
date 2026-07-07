# React / Vue 常用库包完整教学教程

> 面向对象：刚学习 React / Vue / 前端开发的新人  
> 目标：看懂项目里常见的库包分别解决什么问题，知道什么时候用，能写出基础代码示例，并能在日本 IT 现场面试中说清楚。

---

## 0. 先记住一句话

前端项目不是只靠 React 或 Vue 本身完成的。真实项目通常是：

```text
框架本体：React / Vue
构建工具：Vite / Webpack / Next.js / Nuxt
路由：React Router / Vue Router
状态管理：Redux Toolkit / Zustand / Pinia
API 请求：Axios / fetch
服务端数据缓存：TanStack Query / SWR
UI 组件库：MUI / Ant Design / Element Plus / Vuetify
表单与校验：React Hook Form / vee-validate / Zod / Yup
样式：Tailwind CSS / SCSS / CSS Modules
测试：Vitest / Jest / Testing Library / Vue Test Utils / Playwright
代码规范：ESLint / Prettier / Husky / lint-staged
```

新人最容易犯的错误是：把所有库都当成“框架”。其实它们分工不同。

---

## 1. 整体分类对照表

| 分类 | React 常见库 | Vue 常见库 | 主要作用 |
| --- | --- | --- | --- |
| 构建工具 | Vite / Webpack / Next.js | Vite / Vue CLI / Nuxt | 项目启动、开发服务器、打包 |
| 路由 | React Router | Vue Router | 页面跳转、URL 参数、权限路由 |
| 状态管理 | Redux Toolkit / Zustand / Context API | Pinia / Vuex | 登录信息、token、权限、全局状态 |
| API 请求 | Axios / fetch | Axios / fetch | 调后端接口 |
| 服务端数据缓存 | TanStack Query / SWR | TanStack Query for Vue / SWRV | 接口缓存、loading、重新请求 |
| UI 组件库 | MUI / Ant Design / Chakra UI / Mantine | Element Plus / Ant Design Vue / Naive UI / Vuetify | 按钮、表格、弹窗、表单等 |
| 表单 | React Hook Form / Formik | Element Plus Form / vee-validate / Vuelidate | 表单状态管理 |
| 校验 | Zod / Yup | Zod / Yup | 输入校验、schema 校验 |
| 样式 / CSS | Tailwind CSS / SCSS / CSS Modules / styled-components / Emotion | Tailwind CSS / SCSS / CSS Modules / UnoCSS | 页面样式 |
| 日期 | dayjs / date-fns | dayjs / date-fns | 日期格式化、日期比较 |
| 工具函数 | lodash / clsx / classnames | lodash-es / VueUse | 通用工具函数 |
| 表格 | TanStack Table / MUI Data Grid / AG Grid | Element Plus Table / vxe-table / AG Grid Vue | 一览、分页、排序、筛选 |
| 图表 | Recharts / ECharts / Chart.js | ECharts / vue-echarts / Chart.js | dashboard、统计图 |
| 图标 | React Icons / MUI Icons / Lucide React | Element Plus Icons / Iconify / Lucide Vue | icon 图标 |
| 国际化 | react-i18next | vue-i18n | 多语言，日英中切换 |
| 动画 | Framer Motion / React Spring | Vue Transition / Motion One / GSAP | 动画效果 |
| 拖拽 | dnd-kit / react-beautiful-dnd | Vue Draggable Plus / SortableJS | 拖拽排序 |
| 富文本 | React Quill / TipTap | TipTap / Vue Quill | 编辑器、公告、文章输入 |
| 文件上传 | react-dropzone / UI 库 Upload | Element Plus Upload / vue-upload-component | 上传图片、CSV、Excel |
| Excel / CSV | xlsx / papaparse | xlsx / papaparse | 导入导出 Excel / CSV |
| PDF | react-pdf / pdf-lib | vue-pdf / pdf-lib | PDF 预览、生成 |
| 测试 | Vitest / Jest / React Testing Library / Playwright | Vitest / Jest / Vue Test Utils / Playwright | 单体测试、组件测试、E2E |
| Mock | MSW / Mock Service Worker | MSW / vite-plugin-mock | 没有后端 API 时模拟数据 |
| 代码规范 | ESLint / Prettier / Husky / lint-staged | ESLint / Prettier / Husky / lint-staged | 代码检查、提交前检查 |

---

## 2. 学习顺序建议

不要一上来全部学。建议按这个顺序：

```text
第一阶段：React / Vue 基础 + Vite
第二阶段：路由 + API 请求 + UI 组件库
第三阶段：表单 + 校验 + 样式
第四阶段：状态管理 + 服务端数据缓存
第五阶段：表格 + 图表 + 文件上传
第六阶段：测试 + Mock + 代码规范
第七阶段：Next.js / Nuxt / SSR / BFF
```

新人优先掌握：

```text
Vite
React Router / Vue Router
Axios / fetch
UI 组件库
表单校验
Pinia / Zustand / Redux Toolkit
Vitest / Jest
ESLint / Prettier
```

---

## 3. Vite 是什么？

Vite 是现代前端构建工具，负责：

```text
启动本地开发服务器
支持热更新 HMR
编译 React / Vue / TypeScript
打包生产环境文件
```

它不是 React，也不是 Vue。它是“开发和打包工具”。

### 3.1 创建 React + Vite 项目

```bash
npm create vite@latest my-react-app -- --template react-ts
cd my-react-app
npm install
npm run dev
```

### 3.2 创建 Vue + Vite 项目

```bash
npm create vite@latest my-vue-app -- --template vue-ts
cd my-vue-app
npm install
npm run dev
```

### 3.3 package.json 常见命令

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

含义：

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 本地开发启动 |
| `npm run build` | 打包生产环境文件 |
| `npm run preview` | 本地预览打包后的结果 |

---

## 4. Webpack 是什么？

Webpack 是老牌构建工具，很多既存项目还在用。

它也负责：

```text
模块打包
JS / CSS / 图片处理
代码压缩
开发服务器
```

但是相比 Vite，Webpack 配置通常更复杂。新人现在可以先学 Vite，遇到老项目再补 Webpack。

---

## 5. Next.js 是什么？

Next.js 是 React 的全栈框架。

它包含：

```text
React 页面
文件路由
服务端渲染 SSR
静态生成 SSG
API Routes
Server Components
图片优化
SEO 支持
```

### 5.1 创建 Next.js 项目

```bash
npx create-next-app@latest my-next-app
cd my-next-app
npm run dev
```

### 5.2 Next.js App Router 示例

```text
app/
  layout.tsx
  page.tsx
  users/
    page.tsx
  api/
    users/
      route.ts
```

`app/users/page.tsx`：

```tsx
export default function UsersPage() {
  return <h1>用户一覧</h1>;
}
```

`app/api/users/route.ts`：

```ts
import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json([
    { id: 1, name: 'Taro' },
    { id: 2, name: 'Hanako' },
  ]);
}
```

浏览器访问：

```text
/users
/api/users
```

---

## 6. Nuxt 是什么？

Nuxt 是 Vue 的全栈框架，对应 React 生态里的 Next.js。

它包含：

```text
Vue 页面
文件路由
SSR
SSG
server/api
自动导入
SEO 支持
```

### 6.1 创建 Nuxt 项目

```bash
npx nuxi@latest init my-nuxt-app
cd my-nuxt-app
npm install
npm run dev
```

### 6.2 Nuxt 页面示例

```text
pages/
  index.vue
  users.vue
server/
  api/
    users.get.ts
```

`pages/users.vue`：

```vue
<template>
  <h1>用户一覧</h1>
</template>
```

`server/api/users.get.ts`：

```ts
export default defineEventHandler(() => {
  return [
    { id: 1, name: 'Taro' },
    { id: 2, name: 'Hanako' },
  ];
});
```

---

## 7. 路由是什么？

路由就是 URL 和页面组件的对应关系。

例如：

```text
/login       登录页面
/users       用户一覧页面
/users/100   用户详情页面
```

---

## 8. React Router

React 项目常用 React Router。

### 8.1 安装

```bash
npm install react-router-dom
```

### 8.2 基础配置

```tsx
// src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';

function Home() {
  return <h1>首页</h1>;
}

function UserList() {
  return <h1>用户一覧</h1>;
}

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/">首页</Link>
        <Link to="/users">用户一覧</Link>
      </nav>

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/users" element={<UserList />} />
      </Routes>
    </BrowserRouter>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />);
```

### 8.3 URL 参数

```tsx
import { useParams } from 'react-router-dom';

function UserDetail() {
  const { id } = useParams();
  return <h1>用户ID：{id}</h1>;
}
```

路由配置：

```tsx
<Route path="/users/:id" element={<UserDetail />} />
```

访问：

```text
/users/100
```

`id` 就是 `100`。

### 8.4 权限路由示例

```tsx
type Props = {
  isLogin: boolean;
  children: React.ReactNode;
};

function PrivateRoute({ isLogin, children }: Props) {
  if (!isLogin) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
```

使用：

```tsx
<Route
  path="/admin"
  element={
    <PrivateRoute isLogin={true}>
      <AdminPage />
    </PrivateRoute>
  }
/>
```

---

## 9. Vue Router

Vue 项目常用 Vue Router。

### 9.1 安装

```bash
npm install vue-router
```

### 9.2 基础配置

```ts
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router';
import Home from '../pages/Home.vue';
import UserList from '../pages/UserList.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/users', component: UserList },
  ],
});

export default router;
```

```ts
// src/main.ts
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';

createApp(App).use(router).mount('#app');
```

`App.vue`：

```vue
<template>
  <nav>
    <RouterLink to="/">首页</RouterLink>
    <RouterLink to="/users">用户一覧</RouterLink>
  </nav>

  <RouterView />
</template>
```

### 9.3 URL 参数

```ts
// router
{
  path: '/users/:id',
  component: UserDetail,
}
```

```vue
<script setup lang="ts">
import { useRoute } from 'vue-router';

const route = useRoute();
const id = route.params.id;
</script>

<template>
  <h1>用户ID：{{ id }}</h1>
</template>
```

### 9.4 路由守卫

```ts
router.beforeEach((to) => {
  const isLogin = localStorage.getItem('token');

  if (to.path.startsWith('/admin') && !isLogin) {
    return '/login';
  }
});
```

---

## 10. 状态管理是什么？

状态就是页面中会变化的数据，例如：

```text
登录用户信息
token
当前语言
菜单展开状态
权限信息
购物车内容
多个页面都要用的数据
```

普通组件内部状态可以用：

```text
React：useState
Vue：ref / reactive
```

多个页面共享的状态，就需要状态管理库。

---

## 11. React Context API

Context 是 React 自带的轻量全局状态方案。

适合：

```text
主题 theme
当前语言 locale
登录用户信息
简单全局状态
```

### 示例

```tsx
import { createContext, useContext, useState } from 'react';

type AuthContextType = {
  token: string | null;
  login: (token: string) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);

  const login = (newToken: string) => setToken(newToken);
  const logout = () => setToken(null);

  return (
    <AuthContext.Provider value={{ token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
```

使用：

```tsx
function LoginButton() {
  const { login } = useAuth();
  return <button onClick={() => login('abc-token')}>登录</button>;
}
```

---

## 12. Redux Toolkit

Redux Toolkit 是 React 项目里常见的状态管理方案，适合中大型项目。

适合：

```text
状态很多
多个页面共享
状态变化规则复杂
需要 DevTools 追踪状态变化
团队开发规范化
```

### 12.1 安装

```bash
npm install @reduxjs/toolkit react-redux
```

### 12.2 创建 slice

```ts
// src/store/authSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

type AuthState = {
  token: string | null;
  userName: string;
};

const initialState: AuthState = {
  token: null,
  userName: '',
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    login(state, action: PayloadAction<{ token: string; userName: string }>) {
      state.token = action.payload.token;
      state.userName = action.payload.userName;
    },
    logout(state) {
      state.token = null;
      state.userName = '';
    },
  },
});

export const { login, logout } = authSlice.actions;
export default authSlice.reducer;
```

### 12.3 创建 store

```ts
// src/store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

### 12.4 注入 Provider

```tsx
import { Provider } from 'react-redux';
import { store } from './store';

root.render(
  <Provider store={store}>
    <App />
  </Provider>
);
```

### 12.5 组件中使用

```tsx
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from './store';
import { login, logout } from './store/authSlice';

function AuthPanel() {
  const dispatch = useDispatch();
  const userName = useSelector((state: RootState) => state.auth.userName);

  return (
    <div>
      <p>当前用户：{userName}</p>
      <button onClick={() => dispatch(login({ token: 'abc', userName: 'Taro' }))}>
        登录
      </button>
      <button onClick={() => dispatch(logout())}>退出</button>
    </div>
  );
}
```

---

## 13. Zustand

Zustand 是 React 里比较轻量的状态管理库。

适合：

```text
不想写 Redux 那么多模板代码
项目规模中小
想快速管理全局状态
```

### 13.1 安装

```bash
npm install zustand
```

### 13.2 示例

```ts
// src/stores/useAuthStore.ts
import { create } from 'zustand';

type AuthState = {
  token: string | null;
  userName: string;
  login: (token: string, userName: string) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  userName: '',
  login: (token, userName) => set({ token, userName }),
  logout: () => set({ token: null, userName: '' }),
}));
```

组件使用：

```tsx
function AuthPanel() {
  const { userName, login, logout } = useAuthStore();

  return (
    <div>
      <p>当前用户：{userName}</p>
      <button onClick={() => login('abc', 'Taro')}>登录</button>
      <button onClick={logout}>退出</button>
    </div>
  );
}
```

---

## 14. Pinia

Pinia 是 Vue 3 常用状态管理库。

适合：

```text
登录用户信息
token
权限
全局设置
多个组件共享状态
```

### 14.1 安装

```bash
npm install pinia
```

### 14.2 main.ts 配置

```ts
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';

createApp(App).use(createPinia()).mount('#app');
```

### 14.3 创建 store

```ts
// src/stores/auth.ts
import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null as string | null,
    userName: '',
  }),
  actions: {
    login(token: string, userName: string) {
      this.token = token;
      this.userName = userName;
    },
    logout() {
      this.token = null;
      this.userName = '';
    },
  },
});
```

### 14.4 组件中使用

```vue
<script setup lang="ts">
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
</script>

<template>
  <p>当前用户：{{ auth.userName }}</p>
  <button @click="auth.login('abc', 'Taro')">登录</button>
  <button @click="auth.logout()">退出</button>
</template>
```

---

## 15. Vuex

Vuex 是 Vue 旧项目里常见的状态管理库。Vue 3 新项目更常用 Pinia。

新人重点：

```text
新项目：Pinia
老项目：可能是 Vuex
```

---

## 16. fetch

fetch 是浏览器原生 API，不需要安装。

### 基础 GET

```ts
async function getUsers() {
  const res = await fetch('/api/users');

  if (!res.ok) {
    throw new Error('API 请求失败');
  }

  return res.json();
}
```

### POST

```ts
async function createUser(user: { name: string; email: string }) {
  const res = await fetch('/api/users', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(user),
  });

  if (!res.ok) {
    throw new Error('登録失敗');
  }

  return res.json();
}
```

---

## 17. Axios

Axios 是常用 HTTP 客户端，适合统一配置 baseURL、headers、interceptor。

### 17.1 安装

```bash
npm install axios
```

### 17.2 创建 API Client

```ts
// src/api/client.ts
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);
```

### 17.3 API 函数

```ts
// src/api/userApi.ts
import { apiClient } from './client';

export type User = {
  id: number;
  name: string;
  email: string;
};

export async function fetchUsers(): Promise<User[]> {
  const res = await apiClient.get<User[]>('/users');
  return res.data;
}

export async function createUser(data: { name: string; email: string }) {
  const res = await apiClient.post('/users', data);
  return res.data;
}
```

项目里建议把 API 写成函数，不要在组件里到处写 axios。

错误写法：

```tsx
axios.get('/api/users') // 到处散落
```

推荐写法：

```tsx
fetchUsers() // 统一从 api/userApi.ts 调用
```

---

## 18. 为什么需要 TanStack Query / SWR？

在 React / Vue 项目中，页面经常需要从后端 API 获取数据，例如用户一覧、商品一覧、订单详情、统计图数据等。

普通写法一般会这样写：

```tsx
const [data, setData] = useState([]);
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

useEffect(() => {
  setLoading(true);

  fetchUsers()
    .then(setData)
    .catch(setError)
    .finally(() => setLoading(false));
}, []);
```

这种写法可以正常工作，但是项目变大后会出现很多重复问题。

### 普通写法的问题

```text
每个页面都要重复写 loading / error
接口数据没有统一缓存
切换页面再回来又会重新请求
多个组件使用同一个接口时容易重复请求
更新数据后不知道如何自动刷新
分页、筛选、重试、重新请求逻辑比较麻烦
窗口重新聚焦时不会自动刷新数据
网络失败后的重试逻辑需要自己写
```

所以在中大型前端项目中，通常会使用 **TanStack Query** 或 **SWR** 来管理 API 数据。

---

## TanStack Query / SWR 解决什么问题？

TanStack Query 和 SWR 主要解决的是 **服务端状态 server state** 的管理问题。

所谓服务端状态，就是从后端 API 获取的数据。

例如：

```text
用户一覧
商品一覧
订单详情
案件一覧
要员一覧
统计图数据
登录用户信息
通知一覧
分页查询结果
```

这些数据的来源不是前端本地，而是后端服务器。

它们和 Redux / Zustand / Pinia 管理的“客户端状态”不完全一样。

---

## 服务端状态和客户端状态的区别

| 类型 | 说明 | 例子 | 常用工具 |
| ------------------ | ------------- | ----------------------- | ---------------------------------- |
| 客户端状态 client state | 前端自己控制的状态 | 弹窗开关、当前 tab、主题色、表单临时输入值 | Redux / Zustand / Pinia / useState |
| 服务端状态 server state | 从后端 API 获取的数据 | 用户一覧、订单详情、统计数据、分页结果 | TanStack Query / SWR |

简单理解：

```text
客户端状态：前端自己产生、自己控制的数据
服务端状态：后端 API 返回的数据
```

---

## TanStack Query 的作用

TanStack Query 主要用于管理 API 请求和服务端数据。

它可以帮我们处理：

```text
API 请求
loading 状态
error 状态
接口数据缓存
重复请求去重
数据重新获取 refetch
分页查询
条件查询
失败重试
数据失效 invalidate
窗口重新聚焦后自动刷新
```

使用 TanStack Query 后，不需要每个页面都手动写 `useState + useEffect + loading + error`。

---

## TanStack Query 使用场景

TanStack Query 适合这些场景：

```text
后台管理系统的一览画面
分页查询
详情画面
Dashboard 统计数据
多个组件使用同一个 API 数据
新增、修改、删除后需要刷新列表
需要缓存接口数据
需要统一处理 loading / error / retry
```

例如：

```text
用户一覧画面
案件一覧画面
要员一覧画面
订单管理画面
商品管理画面
通知一覧画面
统计图 dashboard
```

---

## TanStack Query 常用 Hook 说明

TanStack Query 在 React 中主要通过 Hook 使用。

常见 Hook 有：

| Hook | 作用 |
| ------------------- | ---------------------------------- |
| `useQuery` | 查询数据，主要用于 GET 请求 |
| `useMutation` | 修改数据，主要用于 POST / PUT / DELETE 请求 |
| `useQueryClient` | 获取 QueryClient，用来刷新缓存、让数据失效、手动操作缓存 |
| `refetch` | 重新执行当前查询 |
| `invalidateQueries` | 让指定缓存失效，并触发重新请求 |

简单理解：

```text
useQuery：
用来取得数据，例如查询用户列表、订单详情。

useMutation：
用来更新数据，例如新增、修改、删除。

useQueryClient：
用来操作缓存，例如新增成功后刷新用户列表。

refetch：
手动重新请求当前接口。

invalidateQueries：
告诉 TanStack Query：这个数据旧了，需要重新取得。
```

---

## TanStack Query 示例

### 1. 安装

```bash
npm install @tanstack/react-query
```

或者：

```bash
pnpm add @tanstack/react-query
```

---

### 2. 在入口文件中配置 QueryClientProvider

```tsx
import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <QueryClientProvider client={queryClient}>
    <App />
  </QueryClientProvider>
);
```

### 这里的作用说明

```text
QueryClient：
TanStack Query 的核心对象，用来管理所有 API 数据缓存。

QueryClientProvider：
把 queryClient 提供给整个 React 应用。
这样项目中的任意组件都可以使用 useQuery、useMutation 等 Hook。
```

如果没有配置 `QueryClientProvider`，组件里直接使用 `useQuery` 会报错。

---

### 3. 封装 API 请求

```tsx
import axios from "axios";

export type User = {
  id: number;
  name: string;
  email: string;
};

export async function fetchUsers(): Promise<User[]> {
  const response = await axios.get("/api/users");
  return response.data;
}
```

### 这里的作用说明

```text
fetchUsers：
真正发送 API 请求的方法。

axios.get("/api/users")：
调用后端用户一覧接口。

return response.data：
只把后端返回的数据部分交给页面使用。
```

这里注意：

```text
TanStack Query 本身不负责发送 HTTP 请求。
真正请求 API 的还是 axios 或 fetch。
TanStack Query 负责管理请求状态、缓存、刷新、重试等逻辑。
```

---

### 4. 在页面中使用 useQuery

```tsx
import { useQuery } from "@tanstack/react-query";
import { fetchUsers } from "./userApi";

export function UserListPage() {
  const {
    data: users,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["users"],
    queryFn: fetchUsers,
  });

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (isError) {
    return <div>数据取得失败：{String(error)}</div>;
  }

  return (
    <div>
      <h2>用户一覧</h2>

      <button onClick={() => refetch()}>重新取得</button>

      <ul>
        {users?.map((user) => (
          <li key={user.id}>
            {user.name} / {user.email}
          </li>
        ))}
      </ul>
    </div>
  );
}
```

### useQuery 的作用说明

`useQuery` 用来执行查询请求，一般用于 GET 请求。

例如：

```text
查询用户列表
查询订单详情
查询商品一覧
查询统计数据
```

这段代码中：

```tsx
useQuery({
  queryKey: ["users"],
  queryFn: fetchUsers,
});
```

含义是：

```text
queryKey: ["users"]
表示这次请求的缓存 key 是 users。

queryFn: fetchUsers
表示真正执行请求的方法是 fetchUsers。
```

TanStack Query 会根据 `queryKey` 管理缓存。

如果其他组件也写了：

```tsx
useQuery({
  queryKey: ["users"],
  queryFn: fetchUsers,
});
```

TanStack Query 会知道它们使用的是同一份用户列表数据。

---

### useQuery 返回值说明

```tsx
const {
  data: users,
  isLoading,
  isError,
  error,
  refetch,
} = useQuery({
  queryKey: ["users"],
  queryFn: fetchUsers,
});
```

| 返回值 | 作用 |
| ------------- | ---------------------- |
| `data` | API 返回的数据 |
| `data: users` | 把 data 重命名为 users，方便理解 |
| `isLoading` | 是否正在第一次加载 |
| `isError` | 是否请求失败 |
| `error` | 失败时的错误信息 |
| `refetch` | 手动重新请求当前接口 |

也就是说，原来需要自己写的：

```tsx
const [data, setData] = useState([]);
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);
```

现在 TanStack Query 已经帮我们处理好了。

---

### 5. 新增用户后刷新列表

```tsx
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

type CreateUserRequest = {
  name: string;
  email: string;
};

async function createUser(request: CreateUserRequest) {
  const response = await axios.post("/api/users", request);
  return response.data;
}

export function CreateUserButton() {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });

  const handleCreate = () => {
    mutation.mutate({
      name: "山田太郎",
      email: "yamada@example.com",
    });
  };

  return (
    <button onClick={handleCreate} disabled={mutation.isPending}>
      {mutation.isPending ? "登録中..." : "用户登録"}
    </button>
  );
}
```

### useMutation 的作用说明

`useMutation` 用来执行会改变数据的请求。

一般用于：

```text
新增 POST
修改 PUT / PATCH
删除 DELETE
状态更新
审批处理
上传文件
```

和 `useQuery` 的区别是：

```text
useQuery：
主要用于查询数据，不改变服务器数据。

useMutation：
主要用于更新数据，会改变服务器数据。
```

例如：

```tsx
const mutation = useMutation({
  mutationFn: createUser,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ["users"] });
  },
});
```

含义是：

```text
mutationFn: createUser
表示真正执行新增用户的 API 方法是 createUser。

onSuccess
表示新增成功后要执行的处理。

queryClient.invalidateQueries({ queryKey: ["users"] })
表示让 users 这个缓存失效，然后重新取得用户列表。
```

---

### useQueryClient 的作用说明

`useQueryClient` 用来取得 TanStack Query 的缓存管理对象。

它常用于：

```text
让某个查询缓存失效
手动刷新某个查询
手动设置缓存数据
手动取得缓存数据
```

例如：

```tsx
const queryClient = useQueryClient();
```

表示取得 QueryClient 对象。

然后可以调用：

```tsx
queryClient.invalidateQueries({ queryKey: ["users"] });
```

表示：

```text
users 这份数据已经旧了，请重新请求最新数据。
```

---

### mutation.mutate 的作用说明

```tsx
mutation.mutate({
  name: "山田太郎",
  email: "yamada@example.com",
});
```

`mutate` 是真正触发新增请求的方法。

也就是说：

```text
useMutation 只是定义一个“新增用户”的操作。
mutation.mutate(...) 才是真正开始执行这个操作。
```

例如点击按钮时调用：

```tsx
<button onClick={handleCreate}>
  用户登録
</button>
```

点击按钮后：

```text
handleCreate 执行
mutation.mutate 执行
createUser API 被调用
新增成功后 onSuccess 执行
users 缓存失效
用户列表重新请求
页面自动更新
```

---

### mutation.isPending 的作用说明

```tsx
disabled={mutation.isPending}
```

表示新增请求正在执行时，按钮禁用。

```tsx
{mutation.isPending ? "登録中..." : "用户登録"}
```

表示：

```text
请求中显示：登録中...
请求结束显示：用户登録
```

这样可以防止用户连续点击按钮，造成重复提交。

---

## SWR 的作用

SWR 也是用于管理 API 请求和服务端状态的库。

SWR 的名字来自：

```text
stale-while-revalidate
```

意思是：

```text
先返回缓存中的旧数据，然后在后台重新请求最新数据，最后更新页面。
```

SWR 的特点是比较轻量，写法简单，非常适合普通数据获取场景。

---

## SWR 使用场景

SWR 适合这些场景：

```text
简单的一览数据取得
详情数据取得
用户信息取得
通知数据取得
Dashboard 简单数据取得
不想写复杂状态管理逻辑的 API 请求
```

例如：

```text
当前登录用户信息
通知件数
用户详情
商品详情
简单列表
```

如果项目 API 请求比较简单，SWR 很轻便。

如果项目有大量分页、复杂缓存、复杂 mutation、依赖查询，TanStack Query 功能会更完整。

---

## SWR 常用 Hook 说明

SWR 在 React 中主要通过 `useSWR` 使用。

| Hook / 方法 | 作用 |
| ----------- | ------------------------ |
| `useSWR` | 查询数据，管理 loading、error、缓存 |
| `mutate` | 手动重新请求，或者手动更新缓存 |
| `isLoading` | 是否正在加载 |
| `error` | 请求失败时的错误信息 |
| `data` | API 返回的数据 |

简单理解：

```text
useSWR：
用来取得 API 数据。

mutate：
用来重新请求数据，或者手动更新缓存。

data：
接口返回的数据。

isLoading：
是否正在加载。

error：
是否请求失败。
```

---

## SWR 示例

### 1. 安装

```bash
npm install swr
```

或者：

```bash
pnpm add swr
```

---

### 2. 封装 fetcher

```tsx
import axios from "axios";

export const fetcher = (url: string) =>
  axios.get(url).then((response) => response.data);
```

### fetcher 的作用说明

`fetcher` 是 SWR 中真正发送 API 请求的方法。

这里的意思是：

```text
SWR 负责管理数据状态和缓存。
axios 负责真正发送 HTTP 请求。
fetcher 负责把请求方法统一封装起来。
```

`useSWR` 会把 key 传给 `fetcher`。

例如：

```tsx
useSWR("/api/users", fetcher);
```

实际执行时相当于：

```tsx
fetcher("/api/users");
```

---

### 3. 在页面中使用 useSWR

```tsx
import useSWR from "swr";
import { fetcher } from "./fetcher";

type User = {
  id: number;
  name: string;
  email: string;
};

export function UserListPage() {
  const {
    data: users,
    error,
    isLoading,
    mutate,
  } = useSWR<User[]>("/api/users", fetcher);

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>数据取得失败</div>;
  }

  return (
    <div>
      <h2>用户一覧</h2>

      <button onClick={() => mutate()}>重新取得</button>

      <ul>
        {users?.map((user) => (
          <li key={user.id}>
            {user.name} / {user.email}
          </li>
        ))}
      </ul>
    </div>
  );
}
```

### useSWR 的作用说明

`useSWR` 用来取得 API 数据，并自动管理：

```text
loading
error
data
缓存
重新请求
窗口聚焦后的自动刷新
```

这段代码中：

```tsx
useSWR<User[]>("/api/users", fetcher);
```

含义是：

```text
"/api/users"
表示请求的 key，同时也是 API 地址。

fetcher
表示真正执行请求的方法。

User[]
表示返回的数据类型是用户数组。
```

---

### useSWR 返回值说明

```tsx
const {
  data: users,
  error,
  isLoading,
  mutate,
} = useSWR<User[]>("/api/users", fetcher);
```

| 返回值 | 作用 |
| ------------- | ----------------- |
| `data` | API 返回的数据 |
| `data: users` | 把 data 重命名为 users |
| `error` | 请求失败时的错误信息 |
| `isLoading` | 是否正在加载 |
| `mutate` | 手动重新请求或更新缓存 |

---

### SWR 中 mutate 的作用说明

```tsx
<button onClick={() => mutate()}>重新取得</button>
```

这里的 `mutate()` 表示手动重新请求 `/api/users`。

简单理解：

```text
mutate()：
重新取得当前接口的最新数据。
```

它适合这些场景：

```text
点击“刷新”按钮
新增数据后刷新列表
修改数据后刷新详情
删除数据后刷新一览
```

例如：

```tsx
await axios.post("/api/users", {
  name: "山田太郎",
  email: "yamada@example.com",
});

mutate();
```

表示：

```text
先新增用户
新增成功后重新请求用户一覧
页面显示最新数据
```

---

## TanStack Query 和 SWR 的区别

| 项目 | TanStack Query | SWR |
| ----------- | --------------------------- | ---------------- |
| 定位 | 功能完整的服务端状态管理库 | 轻量的数据请求缓存库 |
| 适合项目 | 中大型业务系统 | 简单到中等规模项目 |
| 分页支持 | 强 | 可以实现，但需要自己处理较多 |
| Mutation 支持 | 强，适合新增、修改、删除后刷新 | 有 mutate，但整体能力较轻 |
| DevTools | 强 | 有基本工具 |
| 学习成本 | 稍高 | 较低 |
| 常见场景 | 后台系统、复杂列表、Dashboard、CRUD 系统 | 用户信息、简单列表、详情数据 |

简单选择：

```text
API 数据简单，想快速请求和缓存：SWR
API 数据复杂，有分页、筛选、CRUD、缓存刷新：TanStack Query
```

---

## 和 Redux / Zustand / Pinia 的关系

TanStack Query / SWR 不是用来完全替代 Redux、Zustand、Pinia 的。

它们负责的范围不一样。

```text
Redux / Zustand / Pinia：
管理前端本地状态，例如登录状态、菜单权限、弹窗开关、当前 tab、主题色等。

TanStack Query / SWR：
管理从后端 API 获取的数据，例如用户一覧、订单详情、统计数据等。
```

实际项目中经常是一起使用：

```text
Redux / Zustand / Pinia 管理客户端状态
TanStack Query / SWR 管理服务端状态
Axios 负责真正发送 HTTP 请求
```

---

## 实际项目中的常见组合

### React 项目

```text
React
TypeScript
Axios
TanStack Query / SWR
Redux Toolkit / Zustand
React Router
MUI / Ant Design
```

### Vue 项目

```text
Vue 3
TypeScript
Axios
TanStack Query for Vue
Pinia
Vue Router
Element Plus
```

---

## 面试回答示例

可以这样回答：

```text
TanStack Query 和 SWR 主要用于管理服务端状态，也就是从后端 API 获取的数据。

如果不用这些库，每个页面都需要自己写 useState、useEffect、loading、error、重新请求和缓存逻辑，代码会比较重复。

使用 TanStack Query 或 SWR 后，可以统一处理 API 请求状态、缓存、重复请求、重新获取、失败重试等问题。

例如用户一覧、订单详情、Dashboard 统计数据这类从服务器取得的数据，就比较适合使用 TanStack Query 或 SWR。

Redux、Zustand、Pinia 更适合管理客户端状态，例如登录用户信息、菜单权限、弹窗开关、当前 tab 等。TanStack Query / SWR 更适合管理服务端返回的数据。
```

更简单一点可以这样说：

```text
TanStack Query / SWR 是用来管理 API 数据的。
它们可以帮我们处理 loading、error、缓存、重新请求、失败重试等逻辑。
普通 useEffect 写法在页面多了以后会有大量重复代码，所以中大型项目中会使用 TanStack Query 或 SWR 来统一管理服务端状态。
```

## 19. React 使用 TanStack Query

### 19.1 安装

```bash
npm install @tanstack/react-query
```

### 19.2 配置 QueryClient

```tsx
// src/main.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <QueryClientProvider client={queryClient}>
    <App />
  </QueryClientProvider>
);
```

### 19.3 查询数据

```tsx
import { useQuery } from '@tanstack/react-query';
import { fetchUsers } from './api/userApi';

function UserList() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['users'],
    queryFn: fetchUsers,
  });

  if (isLoading) return <p>読み込み中...</p>;
  if (isError) return <p>エラー：{error.message}</p>;

  return (
    <ul>
      {data?.map((user) => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```

### 19.4 新增后刷新列表

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';

function CreateUserButton() {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  return (
    <button
      onClick={() => mutation.mutate({ name: 'Taro', email: 'taro@example.com' })}
    >
      新增用户
    </button>
  );
}
```

记忆点：

```text
useQuery：查数据
useMutation：新增、修改、删除
queryKey：缓存 key
invalidateQueries：让缓存失效并重新请求
```

---

## 20. React 使用 SWR

SWR 更轻量，常用于 Next.js / React 项目。

### 20.1 安装

```bash
npm install swr
```

### 20.2 示例

```tsx
import useSWR from 'swr';

const fetcher = (url: string) => fetch(url).then((res) => res.json());

function UserList() {
  const { data, error, isLoading } = useSWR('/api/users', fetcher);

  if (isLoading) return <p>loading...</p>;
  if (error) return <p>error</p>;

  return (
    <ul>
      {data.map((user: any) => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```

---

## 21. Vue 使用 TanStack Query

### 21.1 安装

```bash
npm install @tanstack/vue-query
```

### 21.2 配置

```ts
import { createApp } from 'vue';
import { VueQueryPlugin } from '@tanstack/vue-query';
import App from './App.vue';

createApp(App).use(VueQueryPlugin).mount('#app');
```

### 21.3 使用

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query';
import { fetchUsers } from '@/api/userApi';

const { data, isLoading, isError } = useQuery({
  queryKey: ['users'],
  queryFn: fetchUsers,
});
</script>

<template>
  <p v-if="isLoading">読み込み中...</p>
  <p v-else-if="isError">エラー</p>
  <ul v-else>
    <li v-for="user in data" :key="user.id">
      {{ user.name }}
    </li>
  </ul>
</template>
```

---

## 22. UI 组件库是什么？

UI 组件库提供现成的页面部件：

```text
按钮
输入框
表格
弹窗
分页
日期选择器
下拉框
菜单
标签页
通知消息
```

没有 UI 组件库时，你要自己写 HTML + CSS + 交互逻辑。用了 UI 组件库，可以快速做出统一风格的业务画面。

---

## 23. React 常见 UI 组件库

### 23.1 MUI

适合：Material Design 风格、后台管理、React 项目。

```bash
npm install @mui/material @emotion/react @emotion/styled
```

示例：

```tsx
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';

function UserForm() {
  return (
    <div>
      <TextField label="用户名" variant="outlined" />
      <Button variant="contained" color="primary">
        保存
      </Button>
    </div>
  );
}
```

### 23.2 Ant Design

适合：企业后台、中后台系统、表格表单很多的项目。

```bash
npm install antd
```

```tsx
import { Button, Input, Table } from 'antd';

function UserPage() {
  const columns = [
    { title: 'ID', dataIndex: 'id' },
    { title: '用户名', dataIndex: 'name' },
  ];

  const data = [
    { id: 1, name: 'Taro' },
    { id: 2, name: 'Hanako' },
  ];

  return (
    <div>
      <Input placeholder="请输入用户名" />
      <Button type="primary">查询</Button>
      <Table rowKey="id" columns={columns} dataSource={data} />
    </div>
  );
}
```

### 23.3 Chakra UI / Mantine

Chakra UI、Mantine 更偏现代 Web 应用，写法简洁，适合快速开发。

---

## 24. Vue 常见 UI 组件库

### 24.1 Element Plus

Element Plus 是 Vue 3 项目里非常常见的 UI 组件库，后台管理系统中使用很多。

```bash
npm install element-plus
```

`main.ts`：

```ts
import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';

createApp(App).use(ElementPlus).mount('#app');
```

示例：

```vue
<template>
  <el-input v-model="keyword" placeholder="请输入用户名" />
  <el-button type="primary" @click="search">查询</el-button>

  <el-table :data="users" style="width: 100%">
    <el-table-column prop="id" label="ID" />
    <el-table-column prop="name" label="用户名" />
  </el-table>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const keyword = ref('');
const users = ref([
  { id: 1, name: 'Taro' },
  { id: 2, name: 'Hanako' },
]);

function search() {
  console.log(keyword.value);
}
</script>
```

### 24.2 Vuetify

Vuetify 是 Vue 的 Material Design 风格组件库。

```vue
<template>
  <v-btn color="primary">保存</v-btn>
  <v-text-field label="用户名" />
</template>
```

### 24.3 Ant Design Vue / Naive UI

Ant Design Vue：适合企业后台。  
Naive UI：Vue 3 现代 UI 组件库，TypeScript 支持较好。

---

## 25. 表单库解决什么问题？

表单开发会遇到很多重复处理。

例如：

```text
输入框值管理
错误信息显示
提交处理
校验规则
默认值
重置表单
复杂嵌套字段
表单联动
异步校验
提交中按钮禁用
```

小表单可以自己写。
但是字段变多后，建议使用表单库。

表单库主要解决：

```text
统一管理表单值
统一处理校验
统一处理提交
统一显示错误信息
减少重复代码
提高大表单可维护性
```

---

## 26. React Hook Form

### 26.1 这个库是什么？

React Hook Form 是 React 中常用的表单库。

它主要用于：

```text
管理表单字段
处理提交
处理校验
显示错误信息
减少 useState 的重复代码
```

适合场景：

```text
登录表单
用户登録表单
检索条件表单
编辑画面
后台管理系统表单
复杂输入画面
```

---

### 26.2 安装

```bash
npm install react-hook-form
```

---

### 26.3 基础示例

```tsx
import { useForm } from 'react-hook-form';

type FormValues = {
  name: string;
  email: string;
};

function UserForm() {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    watch,
  } = useForm<FormValues>({
    defaultValues: {
      name: '',
      email: '',
    },
  });

  const onSubmit = (data: FormValues) => {
    console.log('提交数据:', data);
  };

  const nameValue = watch('name');

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <label>用户名</label>
        <input {...register('name', { required: '用户名不能为空' })} />
        {errors.name && <p>{errors.name.message}</p>}
      </div>

      <div>
        <label>Email</label>
        <input
          {...register('email', {
            required: 'Email不能为空',
            pattern: {
              value: /^[^@]+@[^@]+\.[^@]+$/,
              message: 'Email格式不正确',
            },
          })}
        />
        {errors.email && <p>{errors.email.message}</p>}
      </div>

      <p>当前输入的用户名：{nameValue}</p>

      <button type="submit">保存</button>
      <button type="button" onClick={() => reset()}>
        重置
      </button>
    </form>
  );
}
```

---

### 26.4 示例中 Hook / API 的作用说明

| API | 作用 |
| --------------- | ----------------------------------- |
| `useForm` | 创建并管理整个表单 |
| `register` | 注册输入项，把 input 交给 React Hook Form 管理 |
| `handleSubmit` | 提交前自动执行校验 |
| `errors` | 保存校验错误信息 |
| `reset` | 重置表单 |
| `watch` | 监听字段变化 |
| `defaultValues` | 设置表单默认值 |

说明：

```text
useForm：
创建表单管理对象，负责管理字段值、校验规则、错误信息和提交处理。

register：
把输入框注册到表单中。例如 register('name') 表示这个 input 对应 name 字段。

handleSubmit：
提交时先执行校验。校验成功才会执行 onSubmit。

errors：
保存校验失败后的错误信息，用来在页面上显示错误消息。

reset：
重置表单，可以清空表单，也可以恢复默认值。

watch：
监听字段变化，适合做实时预览、字段联动等。
```

---

### 26.5 面试记忆点

```text
React Hook Form 主要用于管理 React 表单。
通过 useForm 创建表单，通过 register 注册字段，通过 handleSubmit 处理提交，通过 errors 显示校验错误。
它可以减少 useState 管理表单字段的重复代码。
```

---

## 27. Formik

### 27.1 这个库是什么？

Formik 也是 React 表单库。
老项目中可能会遇到，新项目更常见 React Hook Form。

常见搭配：

```text
Formik + Yup
```

适合场景：

```text
老 React 项目
既存系统改修
使用 Yup 做校验的项目
```

---

### 27.2 安装

```bash
npm install formik yup
```

---

### 27.3 基础示例

```tsx
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as yup from 'yup';

const schema = yup.object({
  name: yup.string().required('用户名不能为空'),
  email: yup.string().email('Email格式不正确').required('Email不能为空'),
});

function UserForm() {
  return (
    <Formik
      initialValues={{ name: '', email: '' }}
      validationSchema={schema}
      onSubmit={(values) => {
        console.log('提交数据:', values);
      }}
    >
      <Form>
        <div>
          <label>用户名</label>
          <Field name="name" />
          <ErrorMessage name="name" component="p" />
        </div>

        <div>
          <label>Email</label>
          <Field name="email" />
          <ErrorMessage name="email" component="p" />
        </div>

        <button type="submit">保存</button>
      </Form>
    </Formik>
  );
}
```

---

### 27.4 示例中 API 的作用说明

| API | 作用 |
| ------------------ | ------------------ |
| `Formik` | 创建表单上下文 |
| `initialValues` | 设置初始值 |
| `validationSchema` | 绑定 Yup 校验规则 |
| `onSubmit` | 校验成功后执行提交 |
| `Form` | Formik 提供的 form 组件 |
| `Field` | Formik 提供的输入字段组件 |
| `ErrorMessage` | 显示字段错误信息 |

---

### 27.5 面试记忆点

```text
Formik 是 React 的表单库，老项目中比较常见。
它经常和 Yup 搭配使用，通过 validationSchema 统一管理校验规则。
新项目中 React Hook Form 更常见。
```

---

## 28. Element Plus Form

### 28.1 这个库是什么？

Element Plus Form 是 Element Plus 自带的表单组件。
Vue 3 后台管理系统中非常常见。

适合场景：

```text
后台管理系统
检索条件区域
新增编辑表单
用户登録
权限设置
审批画面
```

---

### 28.2 基础示例

```vue
<template>
  <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
    <el-form-item label="用户名" prop="name">
      <el-input v-model="form.name" />
    </el-form-item>

    <el-form-item label="Email" prop="email">
      <el-input v-model="form.email" />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
      <el-button @click="reset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';

const formRef = ref<FormInstance>();

const form = reactive({
  name: '',
  email: '',
});

const rules: FormRules = {
  name: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: 'Email不能为空', trigger: 'blur' },
    { type: 'email', message: 'Email格式不正确', trigger: 'blur' },
  ],
};

async function submit() {
  await formRef.value?.validate();
  console.log('提交数据:', form);
}

function reset() {
  formRef.value?.resetFields();
}
</script>
```

---

### 28.3 示例中 Hook / API 的作用说明

| API / 写法 | 作用 |
| ------------------- | --------------- |
| `reactive` | 创建响应式表单对象 |
| `ref<FormInstance>` | 获取 el-form 表单实例 |
| `:model` | 绑定表单数据 |
| `:rules` | 绑定校验规则 |
| `prop` | 指定当前表单项对应哪个字段 |
| `v-model` | 输入框和数据双向绑定 |
| `validate()` | 执行整个表单校验 |
| `resetFields()` | 重置表单字段和错误信息 |

说明：

```text
reactive：
管理表单数据，例如 form.name、form.email。

ref<FormInstance>：
获取 Element Plus Form 实例，用来调用 validate、resetFields 等方法。

rules：
定义校验规则。

v-model：
让输入框和 form 数据保持同步。

validate：
提交前执行校验。

resetFields：
重置表单。
```

---

### 28.4 面试记忆点

```text
Vue 项目中如果使用 Element Plus，一般用 el-form 做表单。
通过 model 绑定数据，通过 rules 定义校验规则，通过 ref 获取表单实例，然后调用 validate 方法进行提交前校验。
```

---

## 29. vee-validate

### 29.1 这个库是什么？

vee-validate 是 Vue 中常用的表单校验库。
它适合复杂表单，也可以配合 Zod / Yup 使用。

适合场景：

```text
复杂表单
多字段联动校验
schema 校验
跨组件表单
TypeScript 类型推导
```

---

### 29.2 安装

```bash
npm install vee-validate zod @vee-validate/zod
```

---

### 29.3 基础示例

```vue
<script setup lang="ts">
import { useForm, useField } from 'vee-validate';
import { toTypedSchema } from '@vee-validate/zod';
import * as z from 'zod';

const schema = toTypedSchema(
  z.object({
    name: z.string().min(1, '用户名不能为空'),
    email: z.string().email('Email格式不正确'),
  })
);

const { handleSubmit } = useForm({
  validationSchema: schema,
});

const { value: name, errorMessage: nameError } = useField<string>('name');
const { value: email, errorMessage: emailError } = useField<string>('email');

const onSubmit = handleSubmit((values) => {
  console.log('提交数据:', values);
});
</script>

<template>
  <form @submit.prevent="onSubmit">
    <input v-model="name" placeholder="用户名" />
    <p>{{ nameError }}</p>

    <input v-model="email" placeholder="Email" />
    <p>{{ emailError }}</p>

    <button type="submit">保存</button>
  </form>
</template>
```

---

### 29.4 示例中 Hook / API 的作用说明

| API | 作用 |
| ------------------ | --------------------------------------- |
| `useForm` | 创建表单上下文 |
| `validationSchema` | 绑定 schema 校验规则 |
| `useField` | 管理单个字段 |
| `value` | 当前字段值 |
| `errorMessage` | 当前字段错误信息 |
| `toTypedSchema` | 把 Zod schema 转成 vee-validate 可用的 schema |
| `handleSubmit` | 提交前自动校验 |

---

### 29.5 面试记忆点

```text
vee-validate 适合 Vue 中复杂表单校验。
通过 useForm 管理整个表单，通过 useField 管理单个字段，也可以配合 Zod 或 Yup 使用 schema 统一定义校验规则。
```

---

## 30. Zod

### 30.1 这个库是什么？

Zod 是 TypeScript 优先的 schema 校验库。

适合：

```text
表单校验
API 返回值校验
类型自动推导
请求参数校验
前后端共通校验规则
```

---

### 30.2 安装

```bash
npm install zod
```

---

### 30.3 基础示例

```ts
import * as z from 'zod';

const UserSchema = z.object({
  name: z.string().min(1, '用户名不能为空'),
  age: z.number().min(0, '年龄不能小于0'),
  email: z.string().email('Email格式不正确'),
});

type User = z.infer<typeof UserSchema>;

const result = UserSchema.safeParse({
  name: 'Taro',
  age: 20,
  email: 'taro@example.com',
});

if (!result.success) {
  console.log(result.error.issues);
} else {
  console.log(result.data);
}
```

---

### 30.4 示例中 API 的作用说明

| API | 作用 |
| ------------- | --------------------------- |
| `z.object()` | 定义对象结构 |
| `z.string()` | 字符串校验 |
| `z.number()` | 数字校验 |
| `.min()` | 最小长度或最小值 |
| `.email()` | Email 格式校验 |
| `z.infer` | 从 schema 自动推导 TypeScript 类型 |
| `safeParse()` | 安全校验，返回 success / error |
| `parse()` | 校验失败时直接抛异常 |

---

### 30.5 面试记忆点

```text
Zod 是 TypeScript 优先的 schema 校验库。
它可以定义表单或 API 数据的校验规则，并通过 z.infer 自动推导 TypeScript 类型。
这样可以避免校验规则和类型定义重复维护。
```

---

## 31. Yup

### 31.1 这个库是什么？

Yup 也是 schema 校验库。
在老项目、Formik 项目中比较常见。

---

### 31.2 安装

```bash
npm install yup
```

---

### 31.3 基础示例

```ts
import * as yup from 'yup';

const schema = yup.object({
  name: yup.string().required('用户名不能为空'),
  email: yup.string().email('Email格式不正确').required('Email不能为空'),
});

async function validate() {
  try {
    const data = await schema.validate({ name: '', email: 'abc' });
    console.log(data);
  } catch (error) {
    console.log(error);
  }
}
```

---

### 31.4 示例中 API 的作用说明

| API | 作用 |
| ------------------- | ---------------- |
| `yup.object()` | 定义对象校验规则 |
| `yup.string()` | 定义字符串字段 |
| `.required()` | 必填校验 |
| `.email()` | Email 格式校验 |
| `schema.validate()` | 执行校验，失败时进入 catch |

---

### 31.5 面试记忆点

```text
Yup 是常见 schema 校验库，老项目和 Formik 项目中比较常见。
TypeScript 新项目中，现在更常见 Zod。
```

---

## 32. SCSS

### 32.1 这个库是什么？

SCSS 是 CSS 的增强写法，支持变量、嵌套、mixin。

适合：

```text
统一颜色变量
统一间距变量
组件样式嵌套
传统企业系统样式开发
```

---

### 32.2 示例

```scss
$primary-color: #1677ff;

.card {
  padding: 16px;
  border: 1px solid #ddd;

  .title {
    color: $primary-color;
    font-weight: bold;
  }
}
```

---

### 32.3 示例中语法的作用说明

| 语法 | 作用 |
| -------------------------- | ---- |
| `$primary-color` | 定义变量 |
| `.card { .title { ... } }` | 嵌套写法 |
| `padding` | 内边距 |
| `border` | 边框 |
| `font-weight` | 字体粗细 |

---

### 32.4 面试记忆点

```text
SCSS 是 CSS 的增强写法，支持变量和嵌套。
在项目中可以用来统一颜色、间距等样式变量，也可以让组件样式结构更清晰。
```

---

## 33. CSS Modules

### 33.1 这个库是什么？

CSS Modules 可以让 CSS class 只在当前组件生效，避免全局污染。

适合：

```text
组件化开发
避免 class 名冲突
React / Vue 单组件样式管理
```

---

### 33.2 React 示例

`Button.module.css`

```css
.primary {
  background-color: #1677ff;
  color: white;
  padding: 8px 16px;
}
```

```tsx
import styles from './Button.module.css';

export function Button() {
  return <button className={styles.primary}>保存</button>;
}
```

---

### 33.3 Vue 示例

```vue
<template>
  <button :class="$style.primary">保存</button>
</template>

<style module>
.primary {
  background-color: #1677ff;
  color: white;
}
</style>
```

---

### 33.4 示例中 API / 写法的作用说明

| 写法 | 作用 |
| ------------------------ | ------------------- |
| `.module.css` | 启用 CSS Modules |
| `import styles from ...` | React 中导入局部样式对象 |
| `styles.primary` | React 中使用局部 class |
| `<style module>` | Vue 中启用 CSS Modules |
| `$style.primary` | Vue 中使用局部 class |

---

### 33.5 面试记忆点

```text
CSS Modules 可以避免 class 名全局冲突。
每个组件的样式只在当前组件中生效，适合组件化开发。
```

---

## 34. Tailwind CSS

### 34.1 这个库是什么？

Tailwind CSS 是 utility-first CSS 框架。
它不是组件库，而是通过很多小 class 组合样式。

适合：

```text
快速写样式
统一设计规范
响应式布局
不想频繁起 class 名
```

---

### 34.2 示例

```tsx
export function Card() {
  return (
    <div className="rounded-lg border p-4 shadow-sm">
      <h2 className="text-lg font-bold text-gray-900">用户信息</h2>
      <p className="text-sm text-gray-500">这是一个卡片</p>
    </div>
  );
}
```

---

### 34.3 示例中 class 的作用说明

| class | 作用 |
| --------------- | ------- |
| `rounded-lg` | 大圆角 |
| `border` | 边框 |
| `p-4` | padding |
| `shadow-sm` | 小阴影 |
| `text-lg` | 较大字体 |
| `font-bold` | 加粗 |
| `text-gray-900` | 深灰色文字 |
| `text-sm` | 小号字体 |
| `text-gray-500` | 浅灰色文字 |

---

### 34.4 面试记忆点

```text
Tailwind CSS 不是 MUI、Element Plus 那种组件库，而是原子化 CSS 工具库。
它通过很多小 class 快速组合页面样式，适合快速开发和统一设计规范。
```

---

## 35. styled-components / Emotion

### 35.1 这个库是什么？

styled-components 和 Emotion 是 React 中常见的 CSS-in-JS 方案。

适合：

```text
React 组件内写样式
样式和组件绑定
动态样式
主题定制
```

MUI 默认使用 Emotion 作为样式引擎之一。

---

### 35.2 styled-components 示例

```bash
npm install styled-components
```

```tsx
import styled from 'styled-components';

const SaveButton = styled.button`
  background-color: #1677ff;
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
`;

export function App() {
  return <SaveButton>保存</SaveButton>;
}
```

---

### 35.3 示例中 API 的作用说明

| API / 写法 | 作用 |
| --------------- | ------------------ |
| `styled.button` | 创建一个带样式的 button 组件 |
| 模板字符串 | 在 JS / TS 中写 CSS |
| `<SaveButton>` | 像普通 React 组件一样使用 |

---

### 35.4 面试记忆点

```text
styled-components 和 Emotion 都属于 CSS-in-JS。
它们可以在 React 组件中直接定义样式组件，适合动态样式和主题定制。
```

---

## 36. UnoCSS

### 36.1 这个库是什么？

UnoCSS 和 Tailwind 类似，也是原子化 CSS 方案。
Vue / Vite 项目中有时会看到。

---

### 36.2 示例

```vue
<template>
  <div class="rounded border p-4">
    <p class="text-sm font-bold">用户信息</p>
  </div>
</template>
```

---

### 36.3 面试记忆点

```text
UnoCSS 和 Tailwind 类似，都是原子化 CSS。
新人可以先学 Tailwind，遇到 UnoCSS 时按类似思路理解。
```

---

## 37. dayjs

### 37.1 这个库是什么？

dayjs 是轻量日期处理库，写法类似 moment。

常见用途：

```text
日期格式化
日期比较
日期加减
表格中显示日期
查询条件日期处理
```

---

### 37.2 安装

```bash
npm install dayjs
```

---

### 37.3 示例

```ts
import dayjs from 'dayjs';

const now = dayjs();

console.log(now.format('YYYY-MM-DD'));
console.log(dayjs('2026-07-06').add(7, 'day').format('YYYY-MM-DD'));
console.log(dayjs('2026-07-06').isBefore('2026-08-01'));
```

---

### 37.4 示例中 API 的作用说明

| API | 作用 |
| ------------ | --------- |
| `dayjs()` | 创建当前日期对象 |
| `format()` | 格式化日期 |
| `add()` | 日期加算 |
| `isBefore()` | 判断是否早于某日期 |

---

### 37.5 面试记忆点

```text
dayjs 常用于日期格式化、日期比较和日期加减。
后台管理系统的一览画面、检索条件、日期显示中很常见。
```

---

## 38. date-fns

### 38.1 这个库是什么？

date-fns 是函数式日期工具库。

适合：

```text
按需引入日期函数
函数式写法
减少整体包体积
```

---

### 38.2 安装

```bash
npm install date-fns
```

---

### 38.3 示例

```ts
import { format, addDays, isBefore } from 'date-fns';

console.log(format(new Date(), 'yyyy-MM-dd'));
console.log(addDays(new Date(), 7));
console.log(isBefore(new Date('2026-07-06'), new Date('2026-08-01')));
```

---

### 38.4 示例中 API 的作用说明

| API | 作用 |
| ------------ | ------ |
| `format()` | 格式化日期 |
| `addDays()` | 增加天数 |
| `isBefore()` | 判断日期先后 |

---

### 38.5 选择建议

```text
想写法简单：dayjs
想函数式、按需引入：date-fns
```

---

## 39. lodash / lodash-es

### 39.1 这个库是什么？

lodash 提供很多常用工具函数。
在现代项目中，ESM 项目更常见 `lodash-es`。

常见用途：

```text
防抖
节流
深拷贝
数组去重
分组
排序
对象处理
```

---

### 39.2 安装

```bash
npm install lodash-es
```

---

### 39.3 示例

```ts
import { debounce, uniqBy, groupBy } from 'lodash-es';

const users = [
  { id: 1, name: 'Taro' },
  { id: 1, name: 'Taro' },
  { id: 2, name: 'Hanako' },
];

const uniqueUsers = uniqBy(users, 'id');
console.log(uniqueUsers);

const groupedUsers = groupBy(users, 'id');
console.log(groupedUsers);

const handleSearch = debounce((keyword: string) => {
  console.log('検索:', keyword);
}, 500);
```

---

### 39.4 示例中 API 的作用说明

| 函数 | 作用 |
| ----------- | ---------------- |
| `debounce` | 防抖，常用于搜索框输入后延迟请求 |
| `throttle` | 节流，限制函数执行频率 |
| `cloneDeep` | 深拷贝 |
| `uniqBy` | 按字段去重 |
| `groupBy` | 分组 |
| `sortBy` | 排序 |

---

### 39.5 面试记忆点

```text
lodash 常用于数组、对象、函数节流防抖等通用处理。
例如搜索框输入时，可以用 debounce 避免每输入一个字符就请求 API。
```

---

## 40. clsx / classnames

### 40.1 这个库是什么？

clsx 和 classnames 用于条件拼接 className。

适合：

```text
按钮激活状态
菜单选中状态
错误样式切换
根据 props 切换样式
```

---

### 40.2 安装

```bash
npm install clsx
```

---

### 40.3 示例

```tsx
import clsx from 'clsx';

function Button({ active }: { active: boolean }) {
  return (
    <button
      className={clsx('btn', {
        'btn-active': active,
        'btn-normal': !active,
      })}
    >
      保存
    </button>
  );
}
```

---

### 40.4 示例中 API 的作用说明

| 写法 | 作用 |
| ----------------------- | -------------------- |
| `clsx('btn', {...})` | 拼接基础 class 和条件 class |
| `'btn-active': active` | active 为 true 时添加 |
| `'btn-normal': !active` | active 为 false 时添加 |

---

### 40.5 面试记忆点

```text
clsx 用于根据条件动态拼接 className。
在 React 项目中，按钮状态、菜单选中状态、错误样式切换时很常见。
```

---

## 41. VueUse

### 41.1 这个库是什么？

VueUse 是 Vue 生态常用工具 Hook 集合。

常见工具：

```text
useLocalStorage
useSessionStorage
useMouse
useWindowSize
useDebounceFn
useThrottleFn
useDark
useClipboard
```

---

### 41.2 安装

```bash
npm install @vueuse/core
```

---

### 41.3 示例

```vue
<script setup lang="ts">
import { useLocalStorage, useMouse } from '@vueuse/core';

const token = useLocalStorage('token', '');
const { x, y } = useMouse();
</script>

<template>
  <p>token: {{ token }}</p>
  <p>mouse: {{ x }}, {{ y }}</p>
</template>
```

---

### 41.4 示例中 Hook 的作用说明

| Hook | 作用 |
| ----------------- | -------------------------- |
| `useLocalStorage` | 把数据保存到 localStorage，并保持响应式 |
| `useMouse` | 获取鼠标当前位置 |
| `x` | 鼠标横坐标 |
| `y` | 鼠标纵坐标 |

---

### 41.5 面试记忆点

```text
VueUse 提供很多常用 Composition API 工具。
例如 useLocalStorage、useMouse、useWindowSize、useDebounceFn 等，可以减少自己封装通用 Hook 的代码。
```

---

## 42. 表格库解决什么问题？

业务系统最常见的页面就是“一览画面”。

通常需要：

```text
分页
排序
筛选
列显示 / 隐藏
行选择
固定列
编辑单元格
大量数据虚拟滚动
服务端分页
```

---

## 43. React TanStack Table

### 43.1 这个库是什么？

TanStack Table 是 headless table 引擎。
它负责表格逻辑，你自己负责 UI。

适合：

```text
高度自定义表格 UI
复杂排序
复杂筛选
分页
不想被组件库样式限制
```

---

### 43.2 示例

```tsx
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table';

type User = {
  id: number;
  name: string;
  email: string;
};

const columnHelper = createColumnHelper<User>();

const columns = [
  columnHelper.accessor('id', { header: 'ID' }),
  columnHelper.accessor('name', { header: '用户名' }),
  columnHelper.accessor('email', { header: 'Email' }),
];

const data: User[] = [
  { id: 1, name: 'Taro', email: 'taro@example.com' },
  { id: 2, name: 'Hanako', email: 'hanako@example.com' },
];

function UserTable() {
  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  return (
    <table>
      <thead>
        {table.getHeaderGroups().map((headerGroup) => (
          <tr key={headerGroup.id}>
            {headerGroup.headers.map((header) => (
              <th key={header.id}>
                {flexRender(header.column.columnDef.header, header.getContext())}
              </th>
            ))}
          </tr>
        ))}
      </thead>

      <tbody>
        {table.getRowModel().rows.map((row) => (
          <tr key={row.id}>
            {row.getVisibleCells().map((cell) => (
              <td key={cell.id}>
                {flexRender(cell.column.columnDef.cell, cell.getContext())}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

---

### 43.3 示例中 Hook / API 的作用说明

| API | 作用 |
| ---------------------------- | --------------------- |
| `createColumnHelper<User>()` | 创建列定义辅助工具，并绑定 User 类型 |
| `columnHelper.accessor()` | 定义某一列显示哪个字段 |
| `useReactTable()` | 创建表格实例，管理表格逻辑 |
| `getCoreRowModel()` | 生成基础行数据模型 |
| `table.getHeaderGroups()` | 获取表头分组 |
| `table.getRowModel().rows` | 获取表格行数据 |
| `row.getVisibleCells()` | 获取当前行可见单元格 |
| `flexRender()` | 渲染表头或单元格内容 |

---

### 43.4 面试记忆点

```text
TanStack Table 是 headless table 库。
它不提供现成 UI，而是提供表格逻辑，例如列定义、行模型、排序、筛选、分页等。
```

---

## 44. MUI Data Grid / AG Grid

### 44.1 这两个库是什么？

MUI Data Grid：适合 MUI 项目。
AG Grid：功能非常强，适合复杂企业级表格。

AG Grid 常用于：

```text
大量数据
复杂筛选
列拖拽
单元格编辑
Excel 风格操作
```

---

### 44.2 MUI Data Grid 示例

```tsx
import { DataGrid } from '@mui/x-data-grid';

const columns = [
  { field: 'id', headerName: 'ID', width: 80 },
  { field: 'name', headerName: '用户名', width: 150 },
  { field: 'email', headerName: 'Email', width: 220 },
];

const rows = [
  { id: 1, name: 'Taro', email: 'taro@example.com' },
  { id: 2, name: 'Hanako', email: 'hanako@example.com' },
];

export function UserGrid() {
  return <DataGrid rows={rows} columns={columns} />;
}
```

---

### 44.3 示例中 API 的作用说明

| API | 作用 |
| ------------ | ----------- |
| `DataGrid` | MUI 提供的表格组件 |
| `columns` | 定义列 |
| `rows` | 定义行数据 |
| `field` | 指定字段名 |
| `headerName` | 表头显示名 |
| `width` | 列宽 |

---

### 44.4 面试记忆点

```text
MUI Data Grid 适合 MUI 项目中的表格。
AG Grid 功能更强，适合大量数据、复杂筛选、单元格编辑等企业级表格。
```

---

## 45. Vue Element Plus Table

### 45.1 这个库是什么？

Element Plus Table 是 Element Plus 自带的表格组件。
Vue 后台系统中非常常见。

---

### 45.2 示例

```vue
<template>
  <el-table :data="users">
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column prop="name" label="用户名" />
    <el-table-column prop="email" label="Email" />
  </el-table>

  <el-pagination
    v-model:current-page="page"
    :page-size="10"
    :total="100"
    layout="prev, pager, next"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue';

const page = ref(1);

const users = ref([
  { id: 1, name: 'Taro', email: 'taro@example.com' },
  { id: 2, name: 'Hanako', email: 'hanako@example.com' },
]);
</script>
```

---

### 45.3 示例中 API 的作用说明

| API / 写法 | 作用 |
| ---------------------- | ------- |
| `el-table` | 表格组件 |
| `:data` | 表格数据 |
| `el-table-column` | 表格列 |
| `prop` | 对应数据字段 |
| `label` | 表头显示名 |
| `el-pagination` | 分页组件 |
| `v-model:current-page` | 当前页双向绑定 |

---

### 45.4 面试记忆点

```text
Vue 后台项目中，Element Plus Table 很常见。
通常配合 el-pagination 实现一览画面的分页、查询、排序等功能。
```

---

## 46. vxe-table

### 46.1 这个库是什么？

vxe-table 是 Vue 中强大的表格库，适合复杂业务表格。

适合：

```text
可编辑表格
复杂合并单元格
大量数据
Excel 风格操作
复杂企业系统
```

---

### 46.2 简化示例

```vue
<template>
  <vxe-table :data="users">
    <vxe-column field="id" title="ID" width="80" />
    <vxe-column field="name" title="用户名" />
    <vxe-column field="email" title="Email" />
  </vxe-table>
</template>

<script setup lang="ts">
const users = [
  { id: 1, name: 'Taro', email: 'taro@example.com' },
  { id: 2, name: 'Hanako', email: 'hanako@example.com' },
];
</script>
```

---

### 46.3 面试记忆点

```text
vxe-table 适合 Vue 中复杂业务表格。
如果只是普通一览画面，Element Plus Table 就够用。
如果需要编辑单元格、大量数据、复杂合并单元格，可以考虑 vxe-table。
```

---

## 47. 图表库用在哪里？

常见场景：

```text
Dashboard
销售统计
访问量统计
订单趋势
用户增长
饼图、柱状图、折线图
```

---

## 48. React Recharts

### 48.1 这个库是什么？

Recharts 是 React 常用图表库，写法比较接近 React 组件。

---

### 48.2 安装

```bash
npm install recharts
```

---

### 48.3 示例

```tsx
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';

const data = [
  { month: '1月', sales: 100 },
  { month: '2月', sales: 150 },
  { month: '3月', sales: 120 },
];

function SalesChart() {
  return (
    <LineChart width={500} height={300} data={data}>
      <CartesianGrid strokeDasharray="3 3" />
      <XAxis dataKey="month" />
      <YAxis />
      <Tooltip />
      <Line type="monotone" dataKey="sales" />
    </LineChart>
  );
}
```

---

### 48.4 示例中 API 的作用说明

| API | 作用 |
| --------------- | ------ |
| `LineChart` | 折线图容器 |
| `Line` | 折线 |
| `XAxis` | X 轴 |
| `YAxis` | Y 轴 |
| `Tooltip` | 鼠标悬浮提示 |
| `CartesianGrid` | 网格线 |
| `dataKey` | 指定数据字段 |

---

### 48.5 面试记忆点

```text
Recharts 是 React 常用图表库，适合 Dashboard、统计图、折线图、柱状图等场景。
```

---

## 49. ECharts / vue-echarts

### 49.1 这个库是什么？

ECharts 是功能强大的通用图表库。
React / Vue 都能使用，Vue 常用 `vue-echarts`。

---

### 49.2 安装

```bash
npm install echarts vue-echarts
```

---

### 49.3 Vue 示例

```vue
<script setup lang="ts">
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent]);

const option = {
  tooltip: {},
  xAxis: { type: 'category', data: ['1月', '2月', '3月'] },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: [100, 150, 120] }],
};
</script>

<template>
  <v-chart :option="option" style="height: 300px" />
</template>
```

---

### 49.4 示例中 API 的作用说明

| API | 作用 |
| ------------------ | ------------------- |
| `VChart` | Vue 中渲染 ECharts 的组件 |
| `use()` | 按需注册 ECharts 模块 |
| `CanvasRenderer` | Canvas 渲染器 |
| `BarChart` | 柱状图类型 |
| `GridComponent` | 坐标网格 |
| `TooltipComponent` | 鼠标提示 |
| `option` | 图表配置对象 |
| `series` | 图表数据系列 |

---

### 49.5 面试记忆点

```text
ECharts 功能比较强，适合复杂图表和 Dashboard。
Vue 项目中可以使用 vue-echarts 封装组件。
```

---

## 50. React Icons

### 50.1 这个库是什么？

React Icons 是 React 中常用图标库。
它整合了多个图标集。

---

### 50.2 安装

```bash
npm install react-icons
```

---

### 50.3 示例

```tsx
import { FaSearch, FaSave } from 'react-icons/fa';

function Buttons() {
  return (
    <div>
      <button>
        <FaSearch /> 查询
      </button>
      <button>
        <FaSave /> 保存
      </button>
    </div>
  );
}
```

---

### 50.4 示例中 API 的作用说明

| API | 作用 |
| -------------- | ---------------- |
| `FaSearch` | 搜索图标 |
| `FaSave` | 保存图标 |
| `<FaSearch />` | 像 React 组件一样使用图标 |

---

### 50.5 面试记忆点

```text
React Icons 可以把图标当作 React 组件使用，常用于按钮、菜单、操作列等位置。
```

---

## 51. Lucide React / Lucide Vue

### 51.1 这个库是什么？

Lucide 是现代简洁风格图标库。
React 和 Vue 都有对应版本。

---

### 51.2 React 示例

```bash
npm install lucide-react
```

```tsx
import { Search } from 'lucide-react';

function SearchButton() {
  return (
    <button>
      <Search size={16} /> 查询
    </button>
  );
}
```

---

### 51.3 Vue 示例

```bash
npm install lucide-vue-next
```

```vue
<script setup lang="ts">
import { Search } from 'lucide-vue-next';
</script>

<template>
  <button>
    <Search :size="16" /> 查询
  </button>
</template>
```

---

### 51.4 面试记忆点

```text
Lucide 是简洁风格图标库，React 和 Vue 都可以使用。
图标可以像组件一样引入和渲染。
```

---

## 52. 国际化是什么？

国际化就是多语言支持。

例如：

```text
中文：保存
日语：保存
英语：Save
```

项目里常用于：

```text
中日英切换
错误消息多语言
按钮文字多语言
菜单多语言
```

---

## 53. React react-i18next

### 53.1 这个库是什么？

react-i18next 是 React 项目常用国际化库。

---

### 53.2 安装

```bash
npm install i18next react-i18next
```

---

### 53.3 示例

```ts
// src/i18n.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

i18n.use(initReactI18next).init({
  resources: {
    ja: {
      translation: {
        save: '保存',
        search: '検索',
      },
    },
    en: {
      translation: {
        save: 'Save',
        search: 'Search',
      },
    },
  },
  lng: 'ja',
  fallbackLng: 'en',
});

export default i18n;
```

```tsx
import { useTranslation } from 'react-i18next';

function SaveButton() {
  const { t } = useTranslation();

  return <button>{t('save')}</button>;
}
```

---

### 53.4 示例中 Hook / API 的作用说明

| API | 作用 |
| ---------------------------- | ------------------ |
| `i18n.use(initReactI18next)` | 把 i18next 接入 React |
| `resources` | 定义多语言文本 |
| `lng` | 当前语言 |
| `fallbackLng` | 备用语言 |
| `useTranslation()` | 在 React 组件中取得翻译函数 |
| `t('save')` | 根据当前语言取得 save 对应文本 |

---

### 53.5 面试记忆点

```text
react-i18next 用于 React 项目的国际化。
通过 resources 管理多语言文本，组件中使用 useTranslation 取得 t 函数，然后通过 t('key') 显示对应语言的文案。
```

---

## 54. Vue vue-i18n

### 54.1 这个库是什么？

vue-i18n 是 Vue 项目常用国际化库。

---

### 54.2 安装

```bash
npm install vue-i18n
```

---

### 54.3 示例

```ts
import { createI18n } from 'vue-i18n';

const i18n = createI18n({
  legacy: false,
  locale: 'ja',
  fallbackLocale: 'en',
  messages: {
    ja: { save: '保存', search: '検索' },
    en: { save: 'Save', search: 'Search' },
  },
});

export default i18n;
```

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
</script>

<template>
  <button>{{ t('save') }}</button>
</template>
```

---

### 54.4 示例中 Hook / API 的作用说明

| API | 作用 |
| ---------------- | --------------------- |
| `createI18n()` | 创建 Vue 国际化实例 |
| `legacy: false` | 使用 Composition API 模式 |
| `locale` | 当前语言 |
| `fallbackLocale` | 备用语言 |
| `messages` | 多语言文本定义 |
| `useI18n()` | 在 Vue 组件中取得国际化方法 |
| `t('save')` | 根据当前语言取得文案 |

---

### 54.5 面试记忆点

```text
vue-i18n 用于 Vue 项目的国际化。
通过 createI18n 配置语言资源，在组件中使用 useI18n 取得 t 函数，然后通过 t('key') 显示多语言文本。
```

---

## 68. 前端测试分类

| 测试类型 | 说明 | 工具 |
| ------ | -------- | -------------------------------- |
| 单体测试 | 测函数、工具类 | Vitest / Jest |
| 组件测试 | 测组件显示和点击 | Testing Library / Vue Test Utils |
| E2E 测试 | 测真实浏览器流程 | Playwright / Cypress |

---

## 69. Vitest

### 69.1 这个库是什么？

Vitest 是适合 Vite 项目的测试框架。
可以测试函数、工具类、组件逻辑。

---

### 69.2 安装

```bash
npm install -D vitest
```

---

### 69.3 示例

```ts
// calc.ts
export function add(a: number, b: number) {
  return a + b;
}
```

```ts
// calc.test.ts
import { describe, expect, test } from 'vitest';
import { add } from './calc';

describe('add', () => {
  test('两个数字相加', () => {
    expect(add(1, 2)).toBe(3);
  });
});
```

---

### 69.4 示例中 API 的作用说明

| API | 作用 |
| ------------ | ---------- |
| `describe()` | 测试分组 |
| `test()` | 一个测试用例 |
| `expect()` | 断言 |
| `toBe()` | 判断结果是否严格相等 |

---

### 69.5 面试记忆点

```text
Vitest 适合 Vite 项目，常用于单体测试和组件测试。
它的 API 和 Jest 很像，例如 describe、test、expect。
```

---

## 70. React Testing Library

### 70.1 这个库是什么？

React Testing Library 用于测试 React 组件。
它更关注用户行为，而不是组件内部实现。

---

### 70.2 示例

```tsx
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, test } from 'vitest';

function Counter() {
  const [count, setCount] = React.useState(0);

  return (
    <div>
      <p>count: {count}</p>
      <button onClick={() => setCount(count + 1)}>加一</button>
    </div>
  );
}

test('点击按钮后 count 增加', async () => {
  const user = userEvent.setup();

  render(<Counter />);

  await user.click(screen.getByRole('button', { name: '加一' }));

  expect(screen.getByText('count: 1')).toBeInTheDocument();
});
```

---

### 70.3 示例中 API 的作用说明

| API | 作用 |
| --------------------- | ----------------- |
| `render()` | 把 React 组件渲染到测试环境 |
| `screen` | 从页面上查找元素 |
| `getByRole()` | 按用户可感知的角色查找元素 |
| `userEvent.setup()` | 创建模拟用户操作对象 |
| `user.click()` | 模拟用户点击 |
| `expect()` | 断言测试结果 |
| `toBeInTheDocument()` | 判断元素是否存在于页面中 |

---

### 70.4 面试记忆点

```text
React Testing Library 更关注用户行为。
测试时不是直接检查组件内部状态，而是通过 render 渲染组件，通过 screen 查找画面元素，通过 userEvent 模拟用户操作，最后断言页面结果是否正确。
```

---

## 71. Vue Test Utils

### 71.1 这个库是什么？

Vue Test Utils 是 Vue 官方常用组件测试工具。

---

### 71.2 示例

```vue
<!-- Counter.vue -->
<template>
  <p>count: {{ count }}</p>
  <button @click="count++">加一</button>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const count = ref(0);
</script>
```

```ts
import { mount } from '@vue/test-utils';
import { expect, test } from 'vitest';
import Counter from './Counter.vue';

test('点击按钮后 count 增加', async () => {
  const wrapper = mount(Counter);

  await wrapper.get('button').trigger('click');

  expect(wrapper.text()).toContain('count: 1');
});
```

---

### 71.3 示例中 API 的作用说明

| API | 作用 |
| ---------------------- | ------------- |
| `mount()` | 挂载 Vue 组件 |
| `wrapper` | 测试中的组件包装对象 |
| `wrapper.get()` | 查找组件中的元素 |
| `trigger('click')` | 触发点击事件 |
| `wrapper.text()` | 获取组件渲染后的文本 |
| `expect().toContain()` | 判断文本中是否包含指定内容 |

---

### 71.4 面试记忆点

```text
Vue Test Utils 是 Vue 官方常用组件测试工具。
通过 mount 挂载组件，通过 wrapper 查找元素和触发事件，最后断言页面显示结果是否符合预期。
```

---

## 73. Playwright

### 73.1 这个库是什么？

Playwright 用于 E2E 测试。
它会真实打开浏览器，测试用户操作流程。

适合：

```text
登录流程
检索流程
登録流程
编辑流程
删除流程
完整业务流程测试
```

---

### 73.2 示例

```ts
import { test, expect } from '@playwright/test';

test('登录成功后跳转到首页', async ({ page }) => {
  await page.goto('http://localhost:5173/login');

  await page.getByLabel('用户名').fill('taro');
  await page.getByLabel('密码').fill('password');
  await page.getByRole('button', { name: '登录' }).click();

  await expect(page.getByText('首页')).toBeVisible();
});
```

---

### 73.3 示例中 API 的作用说明

| API | 作用 |
| --------------------------- | -------------- |
| `test()` | 定义一个 E2E 测试用例 |
| `page` | 浏览器页面对象 |
| `page.goto()` | 打开指定 URL |
| `getByLabel()` | 根据 label 查找输入框 |
| `fill()` | 输入文本 |
| `getByRole()` | 根据角色查找按钮 |
| `click()` | 点击按钮 |
| `expect(...).toBeVisible()` | 判断元素是否可见 |

---

### 73.4 面试记忆点

```text
Playwright 用于 E2E 测试，可以真实打开浏览器测试用户操作流程。
例如登录、检索、登録、编辑、删除等完整业务流程，都可以用 Playwright 自动化测试。
```

---

## 75. MSW

### 75.1 这个库是什么？

MSW 是 Mock Service Worker，可以在网络请求层拦截 API。

适合：

```text
后端 API 还没做好
前端独立开发
测试时不想调用真实后端
模拟成功、失败、超时等接口情况
```

---

### 75.2 安装

```bash
npm install -D msw
```

---

### 75.3 示例

```ts
// src/mocks/handlers.ts
import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('/api/users', () => {
    return HttpResponse.json([
      { id: 1, name: 'Taro' },
      { id: 2, name: 'Hanako' },
    ]);
  }),
];
```

```ts
// src/mocks/browser.ts
import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);
```

```ts
// src/main.tsx 或 main.ts
if (import.meta.env.DEV) {
  const { worker } = await import('./mocks/browser');
  await worker.start();
}
```

---

### 75.4 示例中 API 的作用说明

| API | 作用 |
| --------------------- | -------------------- |
| `http.get()` | 拦截 GET 请求 |
| `'/api/users'` | 要拦截的接口地址 |
| `HttpResponse.json()` | 返回 JSON 假数据 |
| `handlers` | 所有 mock 接口定义 |
| `setupWorker()` | 创建浏览器环境的 mock worker |
| `worker.start()` | 启动 mock 拦截 |

---

### 75.5 面试记忆点

```text
MSW 可以在前端开发和测试时模拟后端 API。
它不是简单地 mock 函数，而是在网络请求层拦截 API，所以更接近真实接口调用方式。
```

---

## 77. ESLint

### 77.1 这个库是什么？

ESLint 用于检查 JavaScript / TypeScript 代码问题。

能检查：

```text
未使用变量
错误 Hook 用法
可能的 bug
不符合团队规则的写法
import 顺序
any 滥用
```

---

### 77.2 示例命令

```bash
npx eslint src
npx eslint src --fix
```

---

### 77.3 命令说明

| 命令 | 作用 |
| ---------------- | -------------- |
| `npx eslint src` | 检查 src 目录下代码问题 |
| `--fix` | 自动修复可以修复的问题 |

---

### 77.4 面试记忆点

```text
ESLint 主要用于检查代码质量和团队编码规则，例如未使用变量、Hook 规则错误、潜在 bug 等。
```

---

## 78. Prettier

### 78.1 这个库是什么？

Prettier 用于自动格式化代码。

它主要处理：

```text
缩进
换行
单引号 / 双引号
分号
尾逗号
代码宽度
```

---

### 78.2 配置示例

```json
{
  "singleQuote": true,
  "semi": true,
  "printWidth": 100,
  "trailingComma": "es5"
}
```

---

### 78.3 命令示例

```bash
npx prettier "src/**/*.{ts,tsx,vue,css,scss,json,md}" --write
```

---

### 78.4 配置说明

| 配置 | 作用 |
| -------------------- | ----------------- |
| `singleQuote: true` | 使用单引号 |
| `semi: true` | 语句末尾加分号 |
| `printWidth: 100` | 每行最大长度建议为 100 |
| `trailingComma: es5` | 在 ES5 支持的位置添加尾随逗号 |
| `--write` | 直接格式化并写回文件 |

---

### 78.5 面试记忆点

```text
Prettier 主要负责代码格式化，例如缩进、换行、单双引号、分号等。
ESLint 偏代码质量检查，Prettier 偏代码风格统一。
```

---

## 79. Husky + lint-staged

### 79.1 这两个库是什么？

Husky 可以在 Git commit 前执行检查。
lint-staged 只检查本次修改的文件。

适合：

```text
提交前自动检查
提交前自动格式化
避免不规范代码进入仓库
减少代码 review 中的格式问题
```

---

### 79.2 安装

```bash
npm install -D husky lint-staged
npx husky init
```

---

### 79.3 配置示例

`package.json`：

```json
{
  "lint-staged": {
    "*.{ts,tsx,vue,js,jsx}": [
      "eslint --fix",
      "prettier --write"
    ],
    "*.{css,scss,json,md}": [
      "prettier --write"
    ]
  }
}
```

`.husky/pre-commit`：

```bash
npx lint-staged
```

---

### 79.4 示例中工具的作用说明

| 工具 | 作用 |
| ------------------ | ------------------------- |
| `Husky` | 在 Git commit、push 等时机执行脚本 |
| `lint-staged` | 只处理本次修改过的文件 |
| `pre-commit` | commit 前自动执行 |
| `eslint --fix` | 自动修复代码规则问题 |
| `prettier --write` | 自动格式化代码 |

执行流程：

```text
执行 git commit
触发 Husky 的 pre-commit
执行 npx lint-staged
lint-staged 找出本次修改的文件
对这些文件执行 ESLint 和 Prettier
检查通过后才允许提交
```

---

### 79.5 面试记忆点

```text
项目中可以使用 Husky + lint-staged 在提交前自动执行 ESLint 和 Prettier。
这样可以避免不规范代码进入仓库，也能减少代码 review 时关于格式问题的讨论。
```

---

## 80. React 后台管理项目常见组合

```text
React + Vite + TypeScript
React Router
TanStack Query
Zustand 或 Redux Toolkit
Ant Design 或 MUI
React Hook Form + Zod
Axios
dayjs
Vitest + React Testing Library
ESLint + Prettier + Husky
```

适合：

```text
管理画面
用户一覧
订单管理
权限管理
社内系统
```

---

## 81. Vue 后台管理项目常见组合

```text
Vue 3 + Vite + TypeScript
Vue Router
Pinia
Element Plus
Axios
TanStack Query for Vue 或直接组合 Pinia/API
vee-validate 或 Element Plus Form
Zod / Yup
dayjs
Vitest + Vue Test Utils
ESLint + Prettier + Husky
```

---

## 82. Next.js 项目常见组合

```text
Next.js + TypeScript
App Router
Server Components / Client Components
SWR 或 TanStack Query
Zod
React Hook Form
MUI / Ant Design / shadcn/ui
Prisma / Supabase / 外部 API
Playwright
```

适合：

```text
官网
SEO 页面
电商
博客
预约系统
BFF 层
小型全栈系统
```

---

## 83. Nuxt 项目常见组合

```text
Nuxt + TypeScript
Vue Router 文件路由
Pinia
useFetch / $fetch
Element Plus / Vuetify / Naive UI
vue-i18n
vee-validate + Zod
Playwright / Vitest
```

---

## 84. React / Vue 与这些库的关系

```text
React / Vue：写组件和页面
Vite：启动和打包项目
Router：页面跳转
State：全局状态
Axios：调后端接口
TanStack Query：管理接口数据缓存
UI 组件库：快速做画面
Form：管理输入框和提交
Zod / Yup：校验数据
Table：复杂一览画面
Chart：统计图
Test：保证代码正确
ESLint / Prettier：统一代码质量和风格
```

---

## 85. Client State 和 Server State

### Client State

前端自己掌握的数据：

```text
登录状态
token
主题色
语言
菜单展开状态
```

常用：

```text
Redux Toolkit
Zustand
Pinia
Context API
```

### Server State

从后端 API 来的数据：

```text
用户一覧
订单详情
商品列表
统计数据
```

常用：

```text
TanStack Query
SWR
SWRV
```

不要把所有 API 数据都塞到 Redux / Pinia 里。很多查询数据更适合 TanStack Query。

---

## 86. 表单库和校验库的区别

```text
表单库：管理表单状态、提交、错误显示
校验库：定义输入规则、判断数据是否合法
```

例如：

```text
React Hook Form + Zod
vee-validate + Zod
Element Plus Form + rules
```

---

## 87. UI 组件库和 CSS 框架的区别

```text
UI 组件库：给你现成组件，例如 Button、Table、Dialog
CSS 框架：给你样式能力，例如 Tailwind、Bootstrap、SCSS
```

例子：

```text
Ant Design：UI 组件库
Element Plus：UI 组件库
Tailwind CSS：CSS 工具类框架
SCSS：CSS 预处理器
CSS Modules：局部作用域 CSS
```

---

## 88. 中文回答

```text
在 React / Vue 项目中，除了框架本身，一般还会配合很多常用库。
例如构建工具会用 Vite，路由会用 React Router 或 Vue Router，
全局状态管理会用 Redux Toolkit、Zustand 或 Pinia，
API 请求一般使用 Axios 或 fetch。

对于从后端取得的一览数据、详情数据，如果需要缓存、loading、重新请求等功能，
可以使用 TanStack Query 或 SWR。

UI 方面，React 常见的是 Ant Design、MUI，Vue 常见的是 Element Plus、Vuetify。
表单方面 React 可以使用 React Hook Form，Vue 可以使用 Element Plus Form 或 vee-validate。
校验方面常用 Zod 或 Yup。

测试方面，Vite 项目中常用 Vitest，老项目也会看到 Jest，
React 组件测试配合 React Testing Library，Vue 组件测试配合 Vue Test Utils，
E2E 测试可以使用 Playwright。

代码规范方面一般使用 ESLint、Prettier、Husky 和 lint-staged 来保证提交前代码质量。
```

---

## 89. 日语回答

```text
React や Vue の開発では、フレームワーク本体だけではなく、
プロジェクトの目的に応じていくつかのライブラリを組み合わせて使用します。

例えば、ビルドツールには Vite、ルーティングには React Router や Vue Router、
状態管理には Redux Toolkit、Zustand、Pinia などを使用します。
API 呼び出しには Axios や fetch を使うことが多いです。

サーバーから取得する一覧データや詳細データについては、
キャッシュ、ローディング状態、再取得などを管理するために、
TanStack Query や SWR を使用することがあります。

UI コンポーネントライブラリとしては、React では Ant Design や MUI、
Vue では Element Plus や Vuetify などがよく使われます。
フォーム処理では React Hook Form、Element Plus Form、vee-validate などを使い、
入力チェックには Zod や Yup を使うことがあります。

テストでは Vitest や Jest を使い、React では React Testing Library、
Vue では Vue Test Utils、E2E テストでは Playwright を使用します。
コード品質のために ESLint、Prettier、Husky、lint-staged を導入することもあります。
```

---

## 90. 基础练习

1. 用 Vite 创建一个 React + TypeScript 项目。
2. 添加 React Router，创建 `/login` 和 `/users` 两个页面。
3. 用 Axios 封装一个 `apiClient`。
4. 用 mock 数据显示用户一覧。
5. 用 Ant Design 或 MUI 做一个用户表格。
6. 用 React Hook Form 做一个新增用户表单。
7. 用 Zod 校验用户名和 Email。
8. 用 Zustand 保存登录用户信息。
9. 用 TanStack Query 请求用户一覧。
10. 用 Vitest 写一个函数测试。

## 91. Vue 练习

1. 用 Vite 创建一个 Vue 3 + TypeScript 项目。
2. 添加 Vue Router，创建 `/login` 和 `/users` 页面。
3. 添加 Pinia 保存 token。
4. 添加 Element Plus。
5. 用 `el-table` 显示用户一覧。
6. 用 `el-form` 做新增用户表单。
7. 用 Axios 封装 API。
8. 用 dayjs 格式化日期。
9. 用 Vitest + Vue Test Utils 测试一个按钮组件。
10. 用 MSW 或 vite-plugin-mock 模拟 API。

---

## 92. 最重要的记忆图

```text
项目启动和打包：Vite / Webpack / Next.js / Nuxt
页面跳转：React Router / Vue Router
前端全局状态：Redux Toolkit / Zustand / Pinia
后端 API 请求：Axios / fetch
后端数据缓存：TanStack Query / SWR
画面组件：MUI / Ant Design / Element Plus / Vuetify
表单处理：React Hook Form / Element Plus Form / vee-validate
输入校验：Zod / Yup
样式：Tailwind CSS / SCSS / CSS Modules
一览表格：TanStack Table / AG Grid / Element Plus Table
图表：Recharts / ECharts
测试：Vitest / Jest / Testing Library / Vue Test Utils / Playwright
Mock：MSW / vite-plugin-mock
规范：ESLint / Prettier / Husky / lint-staged
```

新人真正需要做到：

```text
知道每个库解决什么问题
知道什么时候需要用
能看懂项目配置
能写基础代码
面试时能说明使用场景
```

---

## 参考资料

以下为本教程参考的官方文档或项目文档：

- Vite: <https://vite.dev/guide/>
- React Router: <https://reactrouter.com/>
- Vue Router: <https://router.vuejs.org/>
- Redux Toolkit: <https://redux-toolkit.js.org/>
- Zustand: <https://zustand.docs.pmnd.rs/>
- Pinia: <https://pinia.vuejs.org/>
- Axios: <https://axios-http.com/>
- Fetch API: <https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API>
- TanStack Query: <https://tanstack.com/query/latest>
- SWR: <https://swr.vercel.app/>
- MUI: <https://mui.com/>
- Ant Design: <https://ant.design/>
- Element Plus: <https://element-plus.org/>
- Vuetify: <https://vuetifyjs.com/>
- React Hook Form: <https://react-hook-form.com/>
- vee-validate: <https://vee-validate.logaretm.com/>
- Vuelidate: <https://vuelidate-next.netlify.app/>
- Zod: <https://zod.dev/>
- Yup: <https://github.com/jquense/yup>
- Tailwind CSS: <https://tailwindcss.com/>
- CSS Modules: <https://github.com/css-modules/css-modules>
- styled-components: <https://styled-components.com/>
- Emotion: <https://emotion.sh/>
- TanStack Table: <https://tanstack.com/table/latest>
- Apache ECharts: <https://echarts.apache.org/>
- Recharts: <https://recharts.org/>
- Vitest: <https://vitest.dev/>
- Jest: <https://jestjs.io/>
- Testing Library: <https://testing-library.com/>
- Vue Test Utils: <https://test-utils.vuejs.org/>
- Playwright: <https://playwright.dev/>
- MSW: <https://mswjs.io/>
- ESLint: <https://eslint.org/>
- Prettier: <https://prettier.io/>
