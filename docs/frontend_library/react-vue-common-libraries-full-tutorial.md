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

普通写法：

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

问题：

```text
每个页面都要写 loading / error
重复请求没有缓存
切换页面再回来又请求
更新数据后不知道怎么刷新
分页、筛选、重试逻辑麻烦
```

TanStack Query / SWR 解决的是“服务端状态 server state”问题。

服务端状态是：

```text
从 API 来的数据
用户一覧
商品一覧
订单详情
统计图数据
```

它们和 Redux / Pinia 这种“客户端状态”不一样。

---

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

表单开发会遇到：

```text
输入框值管理
错误信息显示
提交处理
校验规则
默认值
重置表单
复杂嵌套字段
```

小表单可以自己写，大表单建议用表单库。

---

## 26. React Hook Form

### 26.1 安装

```bash
npm install react-hook-form
```

### 26.2 基础示例

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
  } = useForm<FormValues>();

  const onSubmit = (data: FormValues) => {
    console.log(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <label>用户名</label>
        <input
          {...register('name', { required: '用户名不能为空' })}
        />
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

      <button type="submit">保存</button>
    </form>
  );
}
```

记忆点：

| API | 作用 |
| --- | --- |
| `useForm` | 创建表单 |
| `register` | 注册输入项 |
| `handleSubmit` | 提交表单 |
| `errors` | 错误信息 |
| `reset` | 重置表单 |
| `watch` | 监听字段变化 |

---

## 27. Formik

Formik 也是 React 表单库，老项目中可能遇到。新项目更常见 React Hook Form。

---

## 28. Vue Element Plus Form

Element Plus 自带 Form 组件，Vue 后台项目很常见。

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
  email: [{ required: true, message: 'Email不能为空', trigger: 'blur' }],
};

async function submit() {
  await formRef.value?.validate();
  console.log(form);
}
</script>
```

---

## 29. vee-validate

vee-validate 是 Vue 常用表单校验库，适合复杂表单，也可以配合 Zod / Yup。

```bash
npm install vee-validate zod @vee-validate/zod
```

示例：

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
  console.log(values);
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

## 30. Zod

Zod 是 TypeScript 优先的 schema 校验库。

适合：

```text
表单校验
API 返回值校验
类型自动推导
前后端共通校验规则
```

### 示例

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

记忆点：

| API | 作用 |
| --- | --- |
| `z.string()` | 字符串 |
| `z.number()` | 数字 |
| `z.object()` | 对象 |
| `parse()` | 校验失败会 throw |
| `safeParse()` | 返回 success / error |
| `z.infer` | 从 schema 推导 TypeScript 类型 |

---

## 31. Yup

Yup 也是 schema 校验库，在老项目和 Formik 项目里比较常见。

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

新人建议：

```text
TypeScript 新项目：优先 Zod
既存项目 / Formik 项目：可能用 Yup
```

---

## 32. SCSS

SCSS 是 CSS 的增强写法，支持变量、嵌套、mixin。

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

React / Vue 都可以用。

---

## 33. CSS Modules

CSS Modules 让 CSS class 只在当前组件生效，避免全局污染。

`Button.module.css`：

```css
.primary {
  background-color: #1677ff;
  color: white;
  padding: 8px 16px;
}
```

React 使用：

```tsx
import styles from './Button.module.css';

export function Button() {
  return <button className={styles.primary}>保存</button>;
}
```

Vue 使用：

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

## 34. Tailwind CSS

Tailwind 是 utility-first CSS 框架，用很多小 class 组合样式。

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

优点：

```text
不用频繁起 class 名
样式直接写在组件附近
适合快速开发和统一设计系统
```

缺点：

```text
class 很长
新人刚看会觉得乱
需要团队规范
```

---

## 35. styled-components / Emotion

这是 React 里常见 CSS-in-JS 方案。

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

MUI 默认使用 Emotion 作为样式引擎之一。

---

## 36. UnoCSS

UnoCSS 和 Tailwind 类似，也是原子化 CSS 方案。Vue / Vite 项目中有时会看到。

新人先学 Tailwind，遇到 UnoCSS 再迁移理解即可。

---

## 37. dayjs

dayjs 是轻量日期库，写法类似 moment。

```bash
npm install dayjs
```

```ts
import dayjs from 'dayjs';

const now = dayjs();

console.log(now.format('YYYY-MM-DD'));
console.log(dayjs('2026-07-06').add(7, 'day').format('YYYY-MM-DD'));
console.log(dayjs('2026-07-06').isBefore('2026-08-01'));
```

常见用途：

```text
日期格式化
日期比较
日期加减
表格中显示日期
查询条件日期处理
```

---

## 38. date-fns

date-fns 是函数式日期工具库。

```bash
npm install date-fns
```

```ts
import { format, addDays, isBefore } from 'date-fns';

console.log(format(new Date(), 'yyyy-MM-dd'));
console.log(addDays(new Date(), 7));
console.log(isBefore(new Date('2026-07-06'), new Date('2026-08-01')));
```

选择建议：

```text
想写法简单：dayjs
想函数式、按需引入：date-fns
```

---

## 39. lodash / lodash-es

lodash 提供很多常用工具函数。

```bash
npm install lodash-es
```

```ts
import { debounce, uniqBy, groupBy } from 'lodash-es';

const users = [
  { id: 1, name: 'Taro' },
  { id: 1, name: 'Taro' },
  { id: 2, name: 'Hanako' },
];

const uniqueUsers = uniqBy(users, 'id');
console.log(uniqueUsers);

const handleSearch = debounce((keyword: string) => {
  console.log('検索:', keyword);
}, 500);
```

常用函数：

| 函数 | 作用 |
| --- | --- |
| `debounce` | 防抖 |
| `throttle` | 节流 |
| `cloneDeep` | 深拷贝 |
| `uniqBy` | 按字段去重 |
| `groupBy` | 分组 |
| `sortBy` | 排序 |

---

## 40. clsx / classnames

用于条件拼接 class。

```bash
npm install clsx
```

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

## 41. VueUse

VueUse 是 Vue 生态常用工具 hooks 集合。

```bash
npm install @vueuse/core
```

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

## 42. 表格库解决什么问题？

业务系统最常见的页面就是“一览画面”。通常需要：

```text
分页
排序
筛选
列显示/隐藏
行选择
固定列
编辑单元格
大量数据虚拟滚动
服务端分页
```

---

## 43. React TanStack Table

TanStack Table 是 headless table 引擎：它负责表格逻辑，你自己负责 UI。

适合：

```text
需要高度自定义表格 UI
不想被组件库样式限制
复杂排序、筛选、分页
```

简化示例：

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

## 44. MUI Data Grid / AG Grid

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

## 45. Vue Element Plus Table

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

## 46. vxe-table

vxe-table 是 Vue 里强大的表格库，适合复杂业务表格。

适合：

```text
可编辑表格
复杂合并单元格
大量数据
Excel 风格操作
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

```bash
npm install recharts
```

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
      <Line type="monotone" dataKey="sales" stroke="#1677ff" />
    </LineChart>
  );
}
```

---

## 49. ECharts

ECharts 是强大的通用图表库，React / Vue 都能用。

Vue 常用 `vue-echarts`。

```bash
npm install echarts vue-echarts
```

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

## 50. React Icons

```bash
npm install react-icons
```

```tsx
import { FaSearch, FaSave } from 'react-icons/fa';

function Buttons() {
  return (
    <div>
      <button><FaSearch /> 查询</button>
      <button><FaSave /> 保存</button>
    </div>
  );
}
```

---

## 51. Lucide React / Lucide Vue

Lucide 是现代简洁风格图标库。

React：

```bash
npm install lucide-react
```

```tsx
import { Search } from 'lucide-react';

function SearchButton() {
  return <button><Search size={16} /> 查询</button>;
}
```

Vue：

```bash
npm install lucide-vue-next
```

```vue
<script setup lang="ts">
import { Search } from 'lucide-vue-next';
</script>

<template>
  <button><Search :size="16" /> 查询</button>
</template>
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

```bash
npm install i18next react-i18next
```

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

组件使用：

```tsx
import { useTranslation } from 'react-i18next';

function SaveButton() {
  const { t } = useTranslation();
  return <button>{t('save')}</button>;
}
```

---

## 54. Vue vue-i18n

```bash
npm install vue-i18n
```

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

组件：

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

## 55. React Framer Motion

```bash
npm install framer-motion
```

```tsx
import { motion } from 'framer-motion';

function FadeInCard() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      这是一个淡入卡片
    </motion.div>
  );
}
```

---

## 56. Vue Transition

Vue 自带 Transition。

```vue
<template>
  <button @click="show = !show">切换</button>

  <Transition name="fade">
    <p v-if="show">显示内容</p>
  </Transition>
</template>

<script setup lang="ts">
import { ref } from 'vue';
const show = ref(true);
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
```

---

## 57. React dnd-kit

拖拽用于：

```text
排序列表
看板拖拽
菜单排序
图片排序
```

```bash
npm install @dnd-kit/core @dnd-kit/sortable
```

简化概念：

```text
DndContext：拖拽上下文
SortableContext：可排序列表
useSortable：让某个元素变成可拖拽
```

实际项目中代码会比较长，新人先理解用途和基本 API 即可。

---

## 58. Vue Draggable Plus / SortableJS

Vue 里常用 SortableJS 封装库。

```bash
npm install vue-draggable-plus
```

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { VueDraggable } from 'vue-draggable-plus';

const list = ref([
  { id: 1, name: '任务A' },
  { id: 2, name: '任务B' },
  { id: 3, name: '任务C' },
]);
</script>

<template>
  <VueDraggable v-model="list">
    <div v-for="item in list" :key="item.id">
      {{ item.name }}
    </div>
  </VueDraggable>
</template>
```

---

## 59. 富文本是什么？

富文本就是类似 Word / 编辑器，可以输入：

```text
标题
加粗
斜体
列表
链接
图片
表格
代码块
```

常见场景：

```text
公告编辑
文章发布
邮件模板
商品详情
CMS 内容管理
```

---

## 60. React Quill

```bash
npm install react-quill quill
```

```tsx
import { useState } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';

function Editor() {
  const [value, setValue] = useState('');

  return <ReactQuill theme="snow" value={value} onChange={setValue} />;
}
```

---

## 61. TipTap

TipTap 是更现代、可扩展的富文本编辑器，React / Vue 都能用。

适合：

```text
复杂编辑器
自定义节点
协同编辑
高扩展需求
```

---

## 62. React react-dropzone

```bash
npm install react-dropzone
```

```tsx
import { useDropzone } from 'react-dropzone';

function UploadBox() {
  const { getRootProps, getInputProps, acceptedFiles } = useDropzone({
    accept: {
      'image/*': [],
    },
  });

  return (
    <div {...getRootProps()} style={{ border: '1px dashed #aaa', padding: 20 }}>
      <input {...getInputProps()} />
      <p>文件拖到这里，或点击选择文件</p>
      <ul>
        {acceptedFiles.map((file) => (
          <li key={file.name}>{file.name}</li>
        ))}
      </ul>
    </div>
  );
}
```

上传到后端：

```ts
async function uploadFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  await apiClient.post('/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}
```

---

## 63. Vue Element Plus Upload

```vue
<template>
  <el-upload
    action="/api/upload"
    :headers="headers"
    :on-success="handleSuccess"
  >
    <el-button type="primary">上传文件</el-button>
  </el-upload>
</template>

<script setup lang="ts">
const headers = {
  Authorization: `Bearer ${localStorage.getItem('token')}`,
};

function handleSuccess(response: unknown) {
  console.log('上传成功', response);
}
</script>
```

---

## 64. xlsx

xlsx 用于读写 Excel。

```bash
npm install xlsx
```

导出 Excel：

```ts
import * as XLSX from 'xlsx';

const users = [
  { id: 1, name: 'Taro', email: 'taro@example.com' },
  { id: 2, name: 'Hanako', email: 'hanako@example.com' },
];

const worksheet = XLSX.utils.json_to_sheet(users);
const workbook = XLSX.utils.book_new();
XLSX.utils.book_append_sheet(workbook, worksheet, 'Users');
XLSX.writeFile(workbook, 'users.xlsx');
```

---

## 65. papaparse

papaparse 用于处理 CSV。

```bash
npm install papaparse
```

读取 CSV：

```ts
import Papa from 'papaparse';

function parseCsv(file: File) {
  Papa.parse(file, {
    header: true,
    complete: (result) => {
      console.log(result.data);
    },
  });
}
```

---

## 66. react-pdf / vue-pdf

用于 PDF 预览。

React：

```tsx
import { Document, Page } from 'react-pdf';

function PdfViewer() {
  return (
    <Document file="/sample.pdf">
      <Page pageNumber={1} />
    </Document>
  );
}
```

---

## 67. pdf-lib

用于生成或修改 PDF。

```bash
npm install pdf-lib
```

```ts
import { PDFDocument, StandardFonts } from 'pdf-lib';

async function createPdf() {
  const pdfDoc = await PDFDocument.create();
  const page = pdfDoc.addPage([600, 400]);
  const font = await pdfDoc.embedFont(StandardFonts.Helvetica);

  page.drawText('Hello PDF', {
    x: 50,
    y: 350,
    size: 24,
    font,
  });

  const pdfBytes = await pdfDoc.save();
  return pdfBytes;
}
```

---

## 68. 前端测试分类

| 测试类型 | 说明 | 工具 |
| --- | --- | --- |
| 单体测试 | 测函数、工具类 | Vitest / Jest |
| 组件测试 | 测组件显示和点击 | Testing Library / Vue Test Utils |
| E2E 测试 | 测真实浏览器流程 | Playwright / Cypress |

---

## 69. Vitest

Vitest 适合 Vite 项目。

```bash
npm install -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

测试函数：

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

## 70. React Testing Library

```tsx
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

记忆点：

```text
render：渲染组件
screen：查找页面元素
userEvent：模拟用户操作
expect：断言结果
```

---

## 71. Vue Test Utils

```bash
npm install -D vitest @vue/test-utils jsdom
```

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

## 72. Jest

Jest 是老牌测试框架，很多既存项目还在用。

```bash
npm install -D jest
```

```ts
test('加法测试', () => {
  expect(1 + 2).toBe(3);
});
```

Vitest 和 Jest API 很像：

| 功能 | Vitest | Jest |
| --- | --- | --- |
| mock 函数 | `vi.fn()` | `jest.fn()` |
| mock 模块 | `vi.mock()` | `jest.mock()` |
| 清理 mock | `vi.clearAllMocks()` | `jest.clearAllMocks()` |

---

## 73. Playwright

Playwright 用于 E2E 测试，真实打开浏览器测试页面流程。

```bash
npm init playwright@latest
```

示例：

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

## 74. Mock 是什么？

Mock 就是假数据 / 假接口。

什么时候用：

```text
后端 API 还没做好
测试时不想真的调用后端
想模拟接口成功 / 失败 / 超时
前端独立开发
```

---

## 75. MSW

MSW 是 Mock Service Worker，可以拦截浏览器或 Node.js 环境中的请求。

```bash
npm install -D msw
```

### 75.1 定义 handlers

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

### 75.2 浏览器环境启动

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

这样前端请求 `/api/users` 时，即使没有后端，也能拿到 mock 数据。

---

## 76. vite-plugin-mock

vite-plugin-mock 是 Vite 项目里常见 mock 插件，可以用文件方式模拟 API。

适合：

```text
本地开发 mock
快速模拟后端接口
简单项目
```

大型项目或测试场景更推荐 MSW。

---

## 77. ESLint

ESLint 用于检查 JavaScript / TypeScript 代码问题。

能检查：

```text
未使用变量
错误 Hook 用法
可能的 bug
不符合团队规则的写法
```

```bash
npm install -D eslint
```

常见命令：

```bash
npx eslint src
npx eslint src --fix
```

---

## 78. Prettier

Prettier 用于自动格式化代码。

```bash
npm install -D prettier
```

`.prettierrc`：

```json
{
  "singleQuote": true,
  "semi": true,
  "printWidth": 100,
  "trailingComma": "es5"
}
```

命令：

```bash
npx prettier "src/**/*.{ts,tsx,vue,css,scss,json,md}" --write
```

区别：

```text
ESLint：检查代码质量和规则
Prettier：格式化代码风格
```

---

## 79. Husky + lint-staged

Husky 可以在 Git commit 前执行检查。

lint-staged 只检查本次修改的文件。

```bash
npm install -D husky lint-staged
npx husky init
```

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

这样提交代码前会自动检查和格式化。

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
