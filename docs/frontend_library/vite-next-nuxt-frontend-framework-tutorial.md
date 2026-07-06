# Vite、Next.js、Nuxt 新人教学文档

> 适合对象：刚开始学习 React、Vue 或前端开发的新人。  
> 学习目标：看完后能分清 Vite、Next.js、Nuxt 的定位，知道什么时候用哪个，能看懂基本项目结构，并能写出最小示例。  
> 更新时间：2026-07-06  

---

## 目录

1. 先建立整体概念
2. 新人必须先懂的基础词汇
3. Vite 详细讲解
4. Next.js 详细讲解
5. Nuxt 详细讲解
6. 三者对比与选择方法
7. 与 Spring Boot / Java 后端项目的关系
8. 三个完整入门示例
9. 常用命令速查表
10. 新人必须记住的知识点
11. 面试回答模板
12. 学习路线
13. 练习题
14. 参考资料

---

## 1. 先建立整体概念

很多新人第一次听到 Vite、Next.js、Nuxt 时会混在一起，觉得它们都是“前端框架”。其实不完全对。

最重要的一句话：

```text
Vite 是构建工具。
Next.js 是 React 的全栈框架。
Nuxt 是 Vue 的全栈框架。
```

可以这样记：

| 名称 | 类型 | 基于什么 | 主要作用 |
| --- | --- | --- | --- |
| Vite | 构建工具 / 开发服务器 | 不绑定具体框架 | 启动项目、热更新、编译、打包 |
| Next.js | React 全栈框架 | React | React 页面 + 路由 + SSR + API + 服务端能力 |
| Nuxt | Vue 全栈框架 | Vue | Vue 页面 + 路由 + SSR + API + 服务端能力 |

用盖房子来比喻：

```text
React / Vue：砖、木头、水泥，也就是写页面的基础材料
Vite：电钻、脚手架、施工工具，帮助你快速开发和打包
Next.js / Nuxt：一整套房屋建设方案，路由、渲染、服务端、部署都帮你安排好
```

---

## 2. 新人必须先懂的基础词汇

在学 Vite、Next.js、Nuxt 前，下面这些词必须先理解。否则后面很容易乱。

---

### 2.1 Library、Framework、Build Tool 的区别

### Library：库

库一般是帮你解决某一类问题。

例如：

```text
React：主要负责构建 UI 组件
Axios：主要负责发送 HTTP 请求
Day.js：主要负责处理日期
```

库的特点：

```text
你主动调用它。
它只解决某一部分问题。
项目整体结构通常需要你自己决定。
```

---

### Framework：框架

框架通常会规定项目结构和开发方式。

例如：

```text
Next.js
Nuxt
Angular
Spring Boot
```

框架的特点：

```text
它会告诉你文件应该放在哪里。
它会自动扫描特定目录。
它会帮你处理路由、构建、服务端渲染等很多事情。
```

---

### Build Tool：构建工具

构建工具负责把你写的源代码变成浏览器能运行的代码。

例如：

```text
Vite
Webpack
Rollup
Rspack
Parcel
```

构建工具主要负责：

```text
启动本地开发服务器
热更新 HMR
编译 TypeScript
编译 JSX / TSX
处理 CSS
打包生产环境文件
压缩和优化代码
```

---

### 2.2 SPA 是什么？

SPA 是 Single Page Application，中文叫单页应用。

普通 SPA 项目大致是：

```text
浏览器第一次加载 index.html
然后通过 JavaScript 控制页面切换
页面切换时不整页刷新
接口数据通过 API 获取
```

常见组合：

```text
React + Vite + React Router
Vue + Vite + Vue Router
```

SPA 适合：

```text
后台管理系统
社内系统
业务录入系统
用户登录后使用的系统
```

SPA 的优点：

```text
交互体验好
页面切换快
前后端分离清晰
适合后台管理系统
```

SPA 的缺点：

```text
首屏可能需要加载较多 JS
SEO 相对弱一些
如果不做优化，搜索引擎看到的内容可能较少
```

---

### 2.3 CSR、SSR、SSG 是什么？

这是理解 Next.js 和 Nuxt 的关键。

---

### CSR：Client Side Rendering

CSR 是客户端渲染。

流程：

```text
浏览器下载空 HTML
浏览器下载 JS
JS 在浏览器里运行
JS 请求 API
JS 把数据渲染到页面上
```

典型项目：

```text
React + Vite
Vue + Vite
```

适合：

```text
后台管理系统
登录后使用的业务系统
SEO 不重要的系统
```

---

### SSR：Server Side Rendering

SSR 是服务端渲染。

流程：

```text
用户访问页面
服务器先生成 HTML
浏览器拿到已经有内容的 HTML
浏览器再加载 JS，接管页面交互
```

典型框架：

```text
Next.js
Nuxt
```

适合：

```text
官网
新闻网站
博客
电商商品页
需要 SEO 的页面
首屏速度要求高的页面
```

---

### SSG：Static Site Generation

SSG 是静态站点生成。

流程：

```text
构建时提前生成 HTML 文件
用户访问时直接返回静态 HTML
```

适合：

```text
公司官网
文档网站
博客
产品介绍页
内容更新不频繁的网站
```

---

### 2.4 Hydration 是什么？

Hydration 可以理解为“注水”或“激活”。

SSR 返回给浏览器的是已经生成好的 HTML，但这个 HTML 一开始只是静态内容。浏览器加载 JavaScript 后，会把事件绑定上去，让按钮、输入框、菜单等变得可交互。

简单理解：

```text
SSR：服务器先把页面画出来
Hydration：浏览器再把页面变成可点击、可交互
```

---

### 2.5 BFF 是什么？

BFF 是 Backend For Frontend，意思是“专门为前端服务的后端层”。

常见结构：

```text
浏览器
↓
Next.js / Nuxt
↓
Java / Spring Boot API
↓
数据库
```

BFF 层可以负责：

```text
接口聚合
权限判断
数据格式转换
隐藏后端接口细节
处理登录状态
为前端页面提供更方便的数据结构
```

在日本项目里，经常看到：

```text
前端：Next.js / Nuxt
BFF：Next.js / Nuxt 的 server 部分
后端：Spring Boot / Java
DB：Oracle / PostgreSQL / MySQL
```

---

## 3. Vite 详细讲解

### 3.1 Vite 是什么？

Vite 是一个现代前端构建工具。

官方文档中，Vite 被描述为用于现代 Web 项目的快速、轻量开发体验的构建工具。它主要包含两部分：

```text
开发服务器 dev server
生产构建 build command
```

Vite 的主要工作：

```text
创建前端项目
启动本地开发服务器
提供快速热更新 HMR
编译 TypeScript / JSX / Vue
处理 CSS / 图片等资源
打包生产环境文件
```

---

### 3.2 Vite 不是什么？

Vite 不是 React。
Vite 不是 Vue。
Vite 不是后端框架。
Vite 不是全栈框架。

更准确地说：

```text
Vite 是帮 React / Vue / Svelte 等项目启动和打包的工具。
```

---

### 3.3 为什么 Vite 很常用？

因为传统构建工具在大项目里容易出现：

```text
启动慢
热更新慢
构建配置复杂
开发体验差
```

Vite 的优势：

```text
启动快
热更新快
配置相对简单
对 TypeScript / JSX / CSS 支持友好
和 React / Vue 搭配方便
现代项目默认选择之一
```

---

### 3.4 创建 Vite + React 项目

```bash
npm create vite@latest my-react-app -- --template react-ts
cd my-react-app
npm install
npm run dev
```

运行后通常会看到：

```text
Local: http://localhost:5173/
```

访问这个地址就能看到页面。

---

### 3.5 创建 Vite + Vue 项目

```bash
npm create vite@latest my-vue-app -- --template vue-ts
cd my-vue-app
npm install
npm run dev
```

---

### 3.6 Vite 项目常见结构

以 React 项目为例：

```text
my-react-app/
├── public/
├── src/
│   ├── assets/
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

说明：

| 文件 / 目录 | 作用 |
| --- | --- |
| `index.html` | 页面入口 HTML |
| `src/main.tsx` | React 应用入口 |
| `src/App.tsx` | 根组件 |
| `public/` | 不需要编译的静态资源 |
| `vite.config.ts` | Vite 配置文件 |
| `package.json` | 依赖和脚本配置 |

---

### 3.7 Vite 常用命令

```bash
npm run dev
```

启动本地开发服务器。

```bash
npm run build
```

构建生产环境文件，一般输出到 `dist/`。

```bash
npm run preview
```

本地预览生产构建结果。

---

### 3.8 Vite 配置示例

```ts
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': '/src',
    },
  },
});
```

说明：

| 配置 | 作用 |
| --- | --- |
| `plugins` | 使用 React / Vue 等插件 |
| `server.port` | 指定开发服务器端口 |
| `server.open` | 启动后自动打开浏览器 |
| `server.proxy` | 解决开发环境 API 跨域问题 |
| `resolve.alias` | 设置路径别名 |

---

### 3.9 Vite 环境变量

Vite 中环境变量通常写在：

```text
.env
.env.development
.env.production
```

注意：暴露给前端代码的变量必须以 `VITE_` 开头。

示例：

```env
VITE_API_BASE_URL=http://localhost:8080
```

使用：

```ts
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
```

新人必须记住：

```text
Vite 前端环境变量必须以 VITE_ 开头。
不要把数据库密码、服务器密钥写进前端环境变量。
```

---

### 3.10 Vite 适合什么项目？

适合：

```text
React 普通前端项目
Vue 普通前端项目
后台管理系统
社内业务系统
SPA 项目
学习项目
组件库开发
```

不一定适合：

```text
SEO 要求很高的网站
需要服务端渲染的电商商品页
需要在框架内写 API 和服务端逻辑的全栈项目
```

这些场景可以考虑 Next.js 或 Nuxt。

---

## 4. Next.js 详细讲解

### 4.1 Next.js 是什么？

Next.js 是基于 React 的全栈 Web 框架。

React 本身主要负责 UI 组件，但实际项目还需要：

```text
路由
布局
服务端渲染
静态生成
数据获取
API 接口
SEO
图片优化
部署优化
```

Next.js 就是在 React 基础上，把这些能力整合起来。

可以这样记：

```text
React = 负责写组件
Next.js = React + 路由 + SSR + SSG + API + 服务端能力 + 项目规范
```

---

### 4.2 创建 Next.js 项目

推荐使用官方脚手架：

```bash
npx create-next-app@latest my-next-app
cd my-next-app
npm run dev
```

也可以用 pnpm：

```bash
pnpm create next-app my-next-app
cd my-next-app
pnpm dev
```

常见提示项：

```text
TypeScript: Yes
ESLint: Yes
Tailwind CSS: 看项目需要
App Router: Yes
src directory: 看团队规范
Import alias: Yes，例如 @/*
```

---

### 4.3 Next.js 项目结构

App Router 项目常见结构：

```text
my-next-app/
├── app/
│   ├── layout.tsx
│   ├── page.tsx
│   ├── users/
│   │   └── page.tsx
│   └── api/
│       └── users/
│           └── route.ts
├── components/
├── lib/
├── public/
├── next.config.ts
├── package.json
└── tsconfig.json
```

说明：

| 文件 / 目录 | 作用 |
| --- | --- |
| `app/` | App Router 的核心目录 |
| `app/page.tsx` | 首页，对应 `/` |
| `app/layout.tsx` | 全局布局 |
| `app/users/page.tsx` | `/users` 页面 |
| `app/api/users/route.ts` | API 路由，对应 `/api/users` |
| `components/` | 公共组件 |
| `lib/` | 工具函数、数据库连接、API 封装等 |
| `public/` | 静态资源 |
| `next.config.ts` | Next.js 配置 |

---

### 4.4 Next.js 文件路由

Next.js 使用文件系统路由。

也就是说，目录结构会自动变成 URL。

```text
app/page.tsx                  -> /
app/about/page.tsx            -> /about
app/users/page.tsx            -> /users
app/users/[id]/page.tsx       -> /users/123
app/products/[slug]/page.tsx  -> /products/abc
```

新人必须记住：

```text
在 App Router 中，一个可访问的页面通常需要 page.tsx。
文件夹表示路径，page.tsx 表示这个路径下显示的页面。
```

---

### 4.5 layout.tsx 是什么？

`layout.tsx` 是布局文件。

例如：

```tsx
// app/layout.tsx
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'My Next App',
  description: 'Next.js 入门示例',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="zh-CN">
      <body>
        <header>网站头部</header>
        <main>{children}</main>
        <footer>网站底部</footer>
      </body>
    </html>
  );
}
```

`children` 就是当前页面内容。

---

### 4.6 page.tsx 是什么？

`page.tsx` 是页面文件。

```tsx
// app/page.tsx
export default function HomePage() {
  return (
    <div>
      <h1>首页</h1>
      <p>这是 Next.js 首页。</p>
    </div>
  );
}
```

对应地址：

```text
/
```

---

### 4.7 Server Component 和 Client Component

这是 Next.js 新人最容易混的点。

在 App Router 中，组件默认是 Server Component。

### Server Component

Server Component 在服务器上执行。

适合：

```text
读取数据库
调用后端 API
读取文件
处理不需要浏览器事件的页面内容
隐藏服务端逻辑
```

Server Component 不能直接使用：

```text
useState
useEffect
onClick
浏览器 API，例如 window、document、localStorage
```

---

### Client Component

Client Component 在浏览器中执行。

如果组件需要点击、输入、状态管理，就要写：

```tsx
'use client';
```

示例：

```tsx
// components/Counter.tsx
'use client';

import { useState } from 'react';

export default function Counter() {
  const [count, setCount] = useState(0);

  return (
    <div>
      <p>count: {count}</p>
      <button onClick={() => setCount(count + 1)}>加一</button>
    </div>
  );
}
```

在页面中使用：

```tsx
// app/page.tsx
import Counter from '@/components/Counter';

export default function HomePage() {
  return (
    <div>
      <h1>首页</h1>
      <Counter />
    </div>
  );
}
```

新人必须记住：

```text
要用 useState / useEffect / onClick，就需要 Client Component。
Client Component 文件顶部写 'use client'。
```

---

### 4.8 Next.js API Route / Route Handler

Next.js 可以在项目里写 API。

示例：

```ts
// app/api/users/route.ts
import { NextResponse } from 'next/server';

export async function GET() {
  const users = [
    { id: 1, name: '田中太郎' },
    { id: 2, name: '佐藤花子' },
  ];

  return NextResponse.json(users);
}
```

访问：

```text
/api/users
```

返回：

```json
[
  { "id": 1, "name": "田中太郎" },
  { "id": 2, "name": "佐藤花子" }
]
```

---

### 4.9 Next.js 数据获取示例

Server Component 可以直接写 async。

```tsx
// app/users/page.tsx
type User = {
  id: number;
  name: string;
};

async function getUsers(): Promise<User[]> {
  const res = await fetch('http://localhost:3000/api/users', {
    cache: 'no-store',
  });

  if (!res.ok) {
    throw new Error('用户数据获取失败');
  }

  return res.json();
}

export default async function UsersPage() {
  const users = await getUsers();

  return (
    <div>
      <h1>用户列表</h1>
      <ul>
        {users.map((user) => (
          <li key={user.id}>{user.name}</li>
        ))}
      </ul>
    </div>
  );
}
```

说明：

```text
这个页面在服务端获取数据。
浏览器拿到时，HTML 里已经有用户列表。
```

---

### 4.10 Next.js 的优点

```text
React 官方生态里非常主流
文件路由方便
SSR / SSG 支持强
SEO 友好
可以写 API
可以作为 BFF 层
适合官网、电商、博客、内容站、全栈应用
部署到 Vercel 很方便
```

---

### 4.11 Next.js 的注意点

```text
比普通 React + Vite 学习成本高
Server Component / Client Component 对新人有点绕
缓存机制需要认真理解
部署环境和普通 SPA 不完全一样
如果只是普通后台管理系统，Next.js 不一定必要
```

---

### 4.12 Next.js 适合什么项目？

适合：

```text
官网
博客
新闻站
电商商品页
SEO 重要的网站
需要 SSR 的 React 项目
需要 BFF 的 React 项目
中小型全栈应用
```

不一定适合：

```text
纯后台管理系统
只在公司内网使用的系统
SEO 完全不重要的业务系统
团队还没有 React 基础时直接上复杂 Next.js 项目
```

---

## 5. Nuxt 详细讲解

### 5.1 Nuxt 是什么？

Nuxt 是基于 Vue 的全栈 Web 框架。

可以这样记：

```text
Vue = 负责写组件
Nuxt = Vue + 路由 + SSR + SSG + API + 服务端能力 + 项目规范
```

Nuxt 和 Next.js 定位很像：

```text
Next.js 对应 React
Nuxt 对应 Vue
```

---

### 5.2 创建 Nuxt 项目

推荐当前官方方式：

```bash
npm create nuxt@latest my-nuxt-app
cd my-nuxt-app
npm install
npm run dev
```

也可能看到旧写法：

```bash
npx nuxi init my-nuxt-app
```

新人看到两种写法不用慌：

```text
npm create nuxt@latest 是现在更推荐的新项目创建方式。
nuxi 是 Nuxt 的 CLI 工具，很多文章和既存项目仍然会提到。
```

---

### 5.3 Nuxt 项目结构

Nuxt 4 常见结构：

```text
my-nuxt-app/
├── app/
│   ├── app.vue
│   ├── pages/
│   │   ├── index.vue
│   │   └── users.vue
│   ├── components/
│   │   └── AppHeader.vue
│   └── layouts/
│       └── default.vue
├── server/
│   └── api/
│       └── users.get.ts
├── public/
├── nuxt.config.ts
├── package.json
└── tsconfig.json
```

说明：

| 文件 / 目录 | 作用 |
| --- | --- |
| `app/app.vue` | Nuxt 应用主组件 |
| `app/pages/` | 页面目录，自动生成路由 |
| `app/components/` | 组件目录，通常支持自动导入 |
| `app/layouts/` | 布局目录 |
| `server/api/` | API 接口目录 |
| `public/` | 静态资源 |
| `nuxt.config.ts` | Nuxt 配置文件 |

注意：

```text
Nuxt 3 项目里经常看到 pages/、components/ 在根目录。
Nuxt 4 新结构中，很多应用代码会放在 app/ 目录下。
维护既存项目时，要先看当前项目采用的是 Nuxt 3 风格还是 Nuxt 4 风格。
```

---

### 5.4 app.vue 是什么？

`app.vue` 是 Nuxt 应用的主组件。

如果使用页面路由，通常写：

```vue
<!-- app/app.vue -->
<template>
  <div>
    <AppHeader />
    <NuxtPage />
  </div>
</template>
```

`<NuxtPage />` 表示显示当前路由对应的页面。

---

### 5.5 Nuxt 文件路由

Nuxt 也使用文件路由。

```text
app/pages/index.vue             -> /
app/pages/about.vue             -> /about
app/pages/users.vue             -> /users
app/pages/users/[id].vue        -> /users/123
```

示例：

```vue
<!-- app/pages/index.vue -->
<template>
  <div>
    <h1>首页</h1>
    <p>这是 Nuxt 首页。</p>
  </div>
</template>
```

对应地址：

```text
/
```

---

### 5.6 Nuxt 组件自动导入

在 Nuxt 中，放在 `app/components/` 里的组件，通常可以自动使用，不需要手动 import。

例如：

```vue
<!-- app/components/AppHeader.vue -->
<template>
  <header>
    <h2>我的网站</h2>
  </header>
</template>
```

在页面中直接使用：

```vue
<!-- app/pages/index.vue -->
<template>
  <div>
    <AppHeader />
    <h1>首页</h1>
  </div>
</template>
```

新人必须记住：

```text
Nuxt 很多东西靠目录约定自动识别。
文件放对位置，比手动配置更重要。
```

---

### 5.7 Nuxt API 示例

Nuxt 可以在 `server/api/` 下写 API。

示例：

```ts
// server/api/users.get.ts
export default defineEventHandler(() => {
  return [
    { id: 1, name: '田中太郎' },
    { id: 2, name: '佐藤花子' },
  ];
});
```

访问：

```text
/api/users
```

返回：

```json
[
  { "id": 1, "name": "田中太郎" },
  { "id": 2, "name": "佐藤花子" }
]
```

---

### 5.8 Nuxt 页面调用 API

```vue
<!-- app/pages/users.vue -->
<script setup lang="ts">
type User = {
  id: number;
  name: string;
};

const { data: users, pending, error } = await useFetch<User[]>('/api/users');
</script>

<template>
  <div>
    <h1>用户列表</h1>

    <p v-if="pending">加载中...</p>
    <p v-else-if="error">数据获取失败</p>

    <ul v-else>
      <li v-for="user in users" :key="user.id">
        {{ user.name }}
      </li>
    </ul>
  </div>
</template>
```

说明：

```text
useFetch 是 Nuxt 提供的数据获取方法。
它可以很好地配合 SSR 和 Nuxt 的数据管理机制。
```

---

### 5.9 Nuxt 配置示例

```ts
// nuxt.config.ts
export default defineNuxtConfig({
  devtools: { enabled: true },
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    apiSecret: process.env.API_SECRET,
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL,
    },
  },
});
```

说明：

| 配置 | 作用 |
| --- | --- |
| `devtools` | 开发工具 |
| `css` | 全局 CSS |
| `runtimeConfig` | 运行时配置 |
| `runtimeConfig.public` | 可以暴露给前端的配置 |

新人必须记住：

```text
只有 public 下的 runtimeConfig 才能安全暴露给浏览器。
不要把密钥放到 public 里。
```

---

### 5.10 Nuxt 的优点

```text
Vue 项目开发体验好
文件路由方便
组件自动导入方便
SSR / SSG 支持好
可以写 API
适合 Vue 全栈项目
约定式目录结构清晰
```

---

### 5.11 Nuxt 的注意点

```text
比普通 Vue + Vite 学习成本高
目录约定多，新人需要记住文件放哪里
SSR 场景下不能随便使用 window、document、localStorage
Nuxt 3 和 Nuxt 4 目录结构有差异，维护项目时要先确认版本
```

---

### 5.12 Nuxt 适合什么项目？

适合：

```text
Vue 官网
Vue 博客
Vue 电商页面
SEO 重要的网站
需要 SSR 的 Vue 项目
中小型 Vue 全栈应用
Vue 项目的 BFF 层
```

不一定适合：

```text
非常简单的 Vue 小页面
纯后台管理系统且不需要 SSR
团队刚学 Vue，还没有掌握组件、路由、状态管理时直接上复杂 Nuxt
```

---

## 6. 三者对比与选择方法

### 6.1 总体对比

| 对比项 | Vite | Next.js | Nuxt |
| --- | --- | --- | --- |
| 类型 | 构建工具 | React 全栈框架 | Vue 全栈框架 |
| 基础技术 | 可配合 React/Vue 等 | React | Vue |
| 是否自带路由 | 否 | 是 | 是 |
| 是否支持 SSR | 可配合框架实现 | 是 | 是 |
| 是否能写 API | 不负责 | 可以 | 可以 |
| 典型项目 | SPA、后台系统 | 官网、电商、SEO、React 全栈 | 官网、电商、SEO、Vue 全栈 |
| 学习难度 | 较低 | 中高 | 中高 |
| 新人建议 | 先学 | React 熟后学 | Vue 熟后学 |

---

### 6.2 如何选择？

### 场景一：我要做 React 后台管理系统

推荐：

```text
React + Vite + React Router + TanStack Query / Axios
```

理由：

```text
后台管理一般 SEO 不重要。
主要是登录后使用。
Vite 启动快、配置简单。
```

---

### 场景二：我要做 Vue 后台管理系统

推荐：

```text
Vue + Vite + Vue Router + Pinia + Axios
```

---

### 场景三：我要做 React 官网 / 博客 / 电商商品页

推荐：

```text
Next.js
```

理由：

```text
SEO 重要。
首屏速度重要。
需要 SSR / SSG。
```

---

### 场景四：我要做 Vue 官网 / 博客 / 电商商品页

推荐：

```text
Nuxt
```

---

### 场景五：我要做小型全栈应用

例如：

```text
预约系统
博客后台
小型管理系统
个人产品 MVP
```

可以考虑：

```text
Next.js + PostgreSQL + Prisma
Nuxt + PostgreSQL + Prisma
Next.js + Supabase
Nuxt + Firebase
```

---

### 场景六：公司已有 Java / Spring Boot 后端

常见选择：

```text
前端：React + Vite / Vue + Vite
后端：Spring Boot
```

或者：

```text
前端：Next.js / Nuxt
后端：Spring Boot
```

如果用了 Next.js / Nuxt，也不代表必须把后端全部写在 Next.js / Nuxt 里。

---

## 7. 与 Spring Boot / Java 后端项目的关系

这是日本现场里很重要的理解。

---

### 7.1 普通前后端分离结构

```text
浏览器
↓
React / Vue / Vite 前端
↓ HTTP API
Spring Boot 后端
↓
数据库
```

特点：

```text
前端只负责页面
后端负责业务逻辑、事务、数据库
前后端通过 REST API 通信
```

适合：

```text
业务系统
社内系统
管理后台
金融、保险、医疗、自治体项目
```

---

### 7.2 Next.js / Nuxt 只做前端

```text
浏览器
↓
Next.js / Nuxt 页面
↓ HTTP API
Spring Boot 后端
↓
数据库
```

这种情况下，Next.js / Nuxt 的 API 能力可能不用，或者只少量使用。

---

### 7.3 Next.js / Nuxt 做 BFF

```text
浏览器
↓
Next.js / Nuxt BFF
↓
Spring Boot / 微服务
↓
数据库
```

BFF 负责：

```text
聚合多个接口
转换数据格式
隐藏后端接口
处理前端登录状态
为页面提供更适合显示的数据
```

核心业务仍然在 Java 后端。

---

### 7.4 Next.js / Nuxt 真正全栈

```text
浏览器
↓
Next.js / Nuxt
↓
数据库
```

适合小型项目或创业产品。

但在大型企业系统里，核心后端通常还是：

```text
Spring Boot
.NET
Go
Node.js NestJS
```

---

## 8. 三个完整入门示例

---

### 8.1 Vite + React：计数器示例

### 创建项目

```bash
npm create vite@latest vite-react-counter -- --template react-ts
cd vite-react-counter
npm install
npm run dev
```

### 修改 `src/App.tsx`

```tsx
import { useState } from 'react';
import './App.css';

export default function App() {
  const [count, setCount] = useState(0);

  return (
    <main>
      <h1>Vite + React 计数器</h1>
      <p>当前数量：{count}</p>
      <button onClick={() => setCount(count + 1)}>加一</button>
      <button onClick={() => setCount(0)}>重置</button>
    </main>
  );
}
```

### 修改 `src/App.css`

```css
main {
  max-width: 640px;
  margin: 40px auto;
  font-family: sans-serif;
}

button {
  margin-right: 8px;
  padding: 8px 16px;
}
```

### 这个示例要学会什么？

```text
Vite 负责启动和打包。
React 负责组件和状态。
按钮点击属于浏览器端交互。
这是典型 CSR / SPA 的写法。
```

---

### 8.2 Next.js：页面 + Client Component + API 示例

### 创建项目

```bash
npx create-next-app@latest next-users-demo
cd next-users-demo
npm run dev
```

### 创建 API

```ts
// app/api/users/route.ts
import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json([
    { id: 1, name: '田中太郎', role: '管理者' },
    { id: 2, name: '佐藤花子', role: '一般用户' },
  ]);
}
```

### 创建用户列表页面

```tsx
// app/users/page.tsx
type User = {
  id: number;
  name: string;
  role: string;
};

async function getUsers(): Promise<User[]> {
  const res = await fetch('http://localhost:3000/api/users', {
    cache: 'no-store',
  });

  if (!res.ok) {
    throw new Error('用户数据获取失败');
  }

  return res.json();
}

export default async function UsersPage() {
  const users = await getUsers();

  return (
    <main>
      <h1>用户列表</h1>
      <ul>
        {users.map((user) => (
          <li key={user.id}>
            {user.name} / {user.role}
          </li>
        ))}
      </ul>
    </main>
  );
}
```

访问：

```text
http://localhost:3000/users
```

### 创建 Client Component

```tsx
// components/SearchBox.tsx
'use client';

import { useState } from 'react';

export default function SearchBox() {
  const [keyword, setKeyword] = useState('');

  return (
    <div>
      <input
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="请输入关键字"
      />
      <p>输入内容：{keyword}</p>
    </div>
  );
}
```

在页面中使用：

```tsx
// app/page.tsx
import SearchBox from '@/components/SearchBox';

export default function HomePage() {
  return (
    <main>
      <h1>Next.js 首页</h1>
      <SearchBox />
    </main>
  );
}
```

### 这个示例要学会什么？

```text
app/users/page.tsx 是页面。
app/api/users/route.ts 是 API。
默认组件是 Server Component。
需要 useState / onChange 时，要写 'use client'。
```

---

### 8.3 Nuxt：页面 + API + useFetch 示例

### 创建项目

```bash
npm create nuxt@latest nuxt-users-demo
cd nuxt-users-demo
npm install
npm run dev
```

### 创建主入口

```vue
<!-- app/app.vue -->
<template>
  <div>
    <header>
      <h2>Nuxt 用户系统</h2>
      <NuxtLink to="/">首页</NuxtLink>
      <NuxtLink to="/users">用户列表</NuxtLink>
    </header>

    <NuxtPage />
  </div>
</template>
```

### 创建首页

```vue
<!-- app/pages/index.vue -->
<template>
  <main>
    <h1>Nuxt 首页</h1>
    <p>这是一个 Nuxt 入门示例。</p>
  </main>
</template>
```

### 创建 API

```ts
// server/api/users.get.ts
export default defineEventHandler(() => {
  return [
    { id: 1, name: '田中太郎', role: '管理者' },
    { id: 2, name: '佐藤花子', role: '一般用户' },
  ];
});
```

### 创建用户页面

```vue
<!-- app/pages/users.vue -->
<script setup lang="ts">
type User = {
  id: number;
  name: string;
  role: string;
};

const { data: users, pending, error } = await useFetch<User[]>('/api/users');
</script>

<template>
  <main>
    <h1>用户列表</h1>

    <p v-if="pending">加载中...</p>
    <p v-else-if="error">用户数据获取失败</p>

    <ul v-else>
      <li v-for="user in users" :key="user.id">
        {{ user.name }} / {{ user.role }}
      </li>
    </ul>
  </main>
</template>
```

访问：

```text
http://localhost:3000/users
```

### 这个示例要学会什么？

```text
app/pages/users.vue 自动变成 /users。
server/api/users.get.ts 自动变成 /api/users。
useFetch 用于页面获取数据。
NuxtLink 用于页面跳转。
```

---

## 9. 常用命令速查表

### 9.1 Vite

```bash
npm create vite@latest my-app -- --template react-ts
npm create vite@latest my-app -- --template vue-ts
npm install
npm run dev
npm run build
npm run preview
```

---

### 9.2 Next.js

```bash
npx create-next-app@latest my-next-app
npm install
npm run dev
npm run build
npm run start
npm run lint
```

说明：

```text
npm run dev：开发环境启动
npm run build：生产环境构建
npm run start：启动生产环境服务，需要先 build
```

---

### 9.3 Nuxt

```bash
npm create nuxt@latest my-nuxt-app
npm install
npm run dev
npm run build
npm run preview
npm run generate
```

说明：

```text
npm run dev：开发环境启动
npm run build：生产环境构建
npm run preview：本地预览构建结果
npm run generate：生成静态站点，具体取决于项目配置
```

---

## 10. 新人必须记住的知识点

### 10.1 一句话记忆

```text
Vite 是工具，不是框架。
Next.js 是 React 的全栈框架。
Nuxt 是 Vue 的全栈框架。
```

---

### 10.2 Vite 必须掌握

```text
Vite 用来启动、编译、热更新、打包。
React + Vite 常用于 SPA 和后台管理系统。
Vue + Vite 常用于 SPA 和后台管理系统。
npm run dev 是开发启动。
npm run build 是生产构建。
开发环境代理可以在 vite.config.ts 的 server.proxy 里配置。
前端环境变量必须以 VITE_ 开头。
```

---

### 10.3 Next.js 必须掌握

```text
Next.js 基于 React。
App Router 使用 app/ 目录。
app/page.tsx 对应首页 /。
app/users/page.tsx 对应 /users。
layout.tsx 是布局。
默认是 Server Component。
需要 useState、useEffect、onClick 时写 'use client'。
app/api/xxx/route.ts 可以写 API。
Next.js 适合 SEO、SSR、官网、电商、全栈 React 项目。
```

---

### 10.4 Nuxt 必须掌握

```text
Nuxt 基于 Vue。
Nuxt 使用文件路由。
app/pages/index.vue 对应 /。
app/pages/users.vue 对应 /users。
app/app.vue 是应用主组件。
<NuxtPage /> 用来显示当前页面。
server/api/xxx.get.ts 可以写 API。
useFetch 用于获取数据。
Nuxt 适合 SEO、SSR、官网、电商、全栈 Vue 项目。
```

---

### 10.5 SSR 场景必须注意

在 SSR 中，代码可能在服务器执行。

所以不能随便写：

```ts
window.localStorage.getItem('token');
```

因为服务器环境没有：

```text
window
document
localStorage
sessionStorage
```

如果必须使用，要放在客户端执行逻辑中。

---

## 11. 面试回答模板

### 11.1 Vite 是什么？

中文回答：

```text
Vite 是现代前端构建工具，主要用于本地开发服务器、热更新和生产环境打包。
在 React 或 Vue 项目中，可以用 Vite 快速创建项目，并且开发时启动速度和热更新速度比较快。
它本身不是 React 或 Vue，而是帮助前端项目编译、运行和打包的工具。
```

日语回答：

```text
Vite はモダンなフロントエンドビルドツールです。
主にローカル開発サーバー、HMR、TypeScript や JSX のコンパイル、本番ビルドに利用します。
React や Vue のプロジェクトでよく使われますが、Vite 自体はフレームワークではなく、開発とビルドを支援するツールです。
```

---

### 11.2 Next.js 是什么？

中文回答：

```text
Next.js 是基于 React 的全栈 Web 框架。
它在 React 的基础上提供了文件路由、服务端渲染、静态生成、API Route、布局、SEO 优化等功能。
如果项目需要 SEO、SSR 或者 React 全栈能力，可以考虑使用 Next.js。
```

日语回答：

```text
Next.js は React ベースのフルスタック Web フレームワークです。
React の UI 開発に加えて、ファイルベースルーティング、SSR、SSG、API Route、レイアウト、SEO 対応などの機能を提供します。
SEO やサーバーサイドレンダリングが必要な React プロジェクトでよく使われます。
```

---

### 11.3 Nuxt 是什么？

中文回答：

```text
Nuxt 是基于 Vue 的全栈 Web 框架。
它在 Vue 的基础上提供文件路由、服务端渲染、静态生成、自动导入、API 接口和 SEO 支持。
可以把 Nuxt 理解为 Vue 生态中类似 Next.js 的框架。
```

日语回答：

```text
Nuxt は Vue ベースのフルスタック Web フレームワークです。
Vue のコンポーネント開発に加えて、ファイルベースルーティング、SSR、SSG、自動インポート、API、SEO 対応などを提供します。
Vue プロジェクトで SEO やサーバーサイドレンダリングが必要な場合によく使われます。
```

---

### 11.4 Vite、Next.js、Nuxt 的区别？

中文回答：

```text
Vite 是构建工具，主要负责开发服务器、热更新、编译和打包。
Next.js 是基于 React 的全栈框架，提供路由、SSR、SSG 和 API 等功能。
Nuxt 是基于 Vue 的全栈框架，定位和 Next.js 类似，但属于 Vue 生态。
所以它们不是同一类工具，Vite 更偏构建工具，Next.js 和 Nuxt 更偏应用框架。
```

日语回答：

```text
Vite はビルドツールで、開発サーバー、HMR、コンパイル、本番ビルドを担当します。
Next.js は React ベースのフルスタックフレームワークで、ルーティング、SSR、SSG、API などを提供します。
Nuxt は Vue ベースのフルスタックフレームワークで、Next.js と似た位置づけですが Vue エコシステムのものです。
```

---

## 12. 学习路线

### 12.1 React 方向

```text
1. HTML / CSS / JavaScript 基础
2. TypeScript 基础
3. React 基础：组件、props、state、事件、hooks
4. Vite + React 项目创建和运行
5. React Router
6. Axios / fetch 调 API
7. 状态管理：Context、Zustand、Redux Toolkit 等
8. 表单和校验：React Hook Form、Zod 等
9. 测试：Vitest / Jest + React Testing Library
10. Next.js：App Router、SSR、SSG、Server Component、API Route
```

新人建议：

```text
先学 React + Vite，再学 Next.js。
不要一开始直接学复杂 Next.js，否则容易把 React 基础和框架规则混在一起。
```

---

### 12.2 Vue 方向

```text
1. HTML / CSS / JavaScript 基础
2. TypeScript 基础
3. Vue 基础：template、ref、reactive、computed、watch、props、emit
4. Vite + Vue 项目创建和运行
5. Vue Router
6. Pinia
7. Axios / fetch 调 API
8. 表单和校验
9. 测试：Vitest + Vue Test Utils
10. Nuxt：文件路由、SSR、SSG、server/api、useFetch
```

新人建议：

```text
先学 Vue + Vite，再学 Nuxt。
```

---

## 13. 练习题

### 13.1 概念题

1. Vite 是框架还是构建工具？
2. Next.js 是基于 React 还是 Vue？
3. Nuxt 是基于 React 还是 Vue？
4. CSR 和 SSR 的区别是什么？
5. 什么场景更适合 SSR？
6. Vite 项目中 `npm run build` 的作用是什么？
7. Next.js 中 `app/page.tsx` 对应什么路径？
8. Nuxt 中 `<NuxtPage />` 的作用是什么？
9. Next.js 中什么时候需要写 `'use client'`？
10. Next.js / Nuxt 能不能完全代替 Spring Boot？为什么？

---

### 13.2 实操题

### 练习一：Vite + React

要求：

```text
创建一个 Vite + React 项目。
页面上显示一个输入框和一个按钮。
输入名字后点击按钮，页面显示：你好，xxx。
```

---

### 练习二：Next.js

要求：

```text
创建一个 Next.js 项目。
创建 /users 页面。
创建 /api/users API。
/users 页面从 /api/users 获取数据并显示用户列表。
```

---

### 练习三：Nuxt

要求：

```text
创建一个 Nuxt 项目。
创建 /products 页面。
创建 /api/products API。
/products 页面使用 useFetch 获取商品列表并显示。
```

---

## 14. 参考资料

以下资料建议优先看官方文档：

- Vite 官方文档：<https://vite.dev/guide/>
- Vite 官方首页：<https://vite.dev/>
- React 官方文档：Build a React app from Scratch：<https://react.dev/learn/build-a-react-app-from-scratch>
- Next.js 官方文档：<https://nextjs.org/docs>
- Next.js App Router 文档：<https://nextjs.org/docs/app>
- Next.js create-next-app 文档：<https://nextjs.org/docs/app/api-reference/cli/create-next-app>
- Nuxt 官方文档：<https://nuxt.com/docs>
- Nuxt 4 目录结构：<https://nuxt.com/docs/4.x/directory-structure>
- Nuxt create-nuxt 命令：<https://nuxt.com/docs/4.x/api/commands/init>

---

## 最后总结

新人只要先记住这张表，就不会乱：

| 名称 | 一句话理解 | 适合场景 |
| --- | --- | --- |
| Vite | 前端开发和打包工具 | React/Vue SPA、后台管理、普通前端项目 |
| Next.js | React 全栈框架 | React 官网、SEO、SSR、电商、BFF、全栈应用 |
| Nuxt | Vue 全栈框架 | Vue 官网、SEO、SSR、电商、BFF、全栈应用 |

最终记忆：

```text
先学基础框架，再学全栈框架。
React 方向：React + Vite → Next.js。
Vue 方向：Vue + Vite → Nuxt。
日本现场常见：前端 React/Vue/Next/Nuxt，后端 Spring Boot，数据库 Oracle/PostgreSQL。
```
