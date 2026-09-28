# 第 1 章 React 的定位与创建项目

## 本章目标

- 【必须掌握】说明 React、浏览器、Vite、Node.js 和 npm 的职责。
- 【必须掌握】区分 JavaScript 框架、React/Vue 和 UI 框架的职责。
- 【必须掌握】创建最基础的 React 项目并找到入口链路。
- 【必须掌握】运行开发、构建和预览命令。

## 1. React 解决什么问题

传统 DOM 编程经常在数据变化后逐个查找并修改节点。React 使用声明式方式：组件根据当前 Props 和 State 描述 UI，数据变化后 React 再计算组件输出并提交必要的 DOM 更新。

```text
传统方式：数据变化 → 手动寻找并修改 DOM
React：数据变化 → 组件重新计算 JSX → React 提交必要更新
```

可以先记住：

```text
UI = Component(Props, State)
```

React 是 UI 库，不是后端、数据库、路由器或构建工具。SPA 通常只加载一个 HTML 壳，之后由客户端路由根据 URL 组合页面；但服务器仍需把入口 HTML 正确返回，并且所有业务权限仍由后端确认。

## 2. JavaScript 框架与 UI 框架有什么区别

在前端项目中，“框架”这个词经常被用来表示不同层次的工具。先区分它们负责什么，可以避免把 React、Vue、Bootstrap 和 Vuetify 当成同一种技术。

### 2.1 JavaScript 是语言，框架和库使用这门语言

JavaScript 是浏览器能够执行的编程语言。变量、函数、对象、数组、DOM、事件和异步处理都属于 JavaScript 基础。

框架或库建立在 JavaScript 之上，帮助开发者组织大型页面：

```text
JavaScript
   ↓ 提供编程能力
React / Vue
   ↓ 组织组件、数据和画面
业务页面
```

即使使用 React 或 Vue，最终仍然需要编写 JavaScript。框架不会替代变量、函数、数组、对象、模块和异步请求等基础知识。

### 2.2 JavaScript 框架或 UI 库负责应用的画面逻辑

Vue 官方称自己为“渐进式 JavaScript 框架”。React 官方称自己为“用于构建 Web 和原生用户界面的库”。它们都可以承担前端应用的核心 UI 开发：

- 把页面拆成组件。
- 根据数据生成画面。
- 数据变化后更新需要变化的界面。
- 处理用户操作。
- 与路由、状态管理和 HTTP 请求工具组合成完整应用。

严格来说，React 是库，Vue 是框架。不过在日常项目沟通中，人们也会把 React 和 Vue 统称为“前端框架”。遇到这种说法时，应关注它们在项目中承担的职责，不必只纠结名称。

### 2.3 Vue 和 React 的主要区别

Vue 和 React 都采用组件化开发，也都能制作登录页、列表、表单和管理系统。主要区别在于表达 UI 和组织生态的方式。

| 对比项 | React | Vue |
| --- | --- | --- |
| 官方定位 | 构建用户界面的库 | 渐进式 JavaScript 框架 |
| 主要界面写法 | 使用 JSX，在 JavaScript 中描述 UI | 常用单文件组件，在 `template` 中描述 UI |
| 组件文件 | 常见 `.jsx` 文件 | 常见 `.vue` 文件 |
| 响应式状态 | 使用 `useState` 等 Hook 更新状态 | 使用 `ref`、`reactive` 等响应式 API |
| 常用路由 | React Router 等生态库 | Vue Router 是 Vue 官方路由方案 |
| 常用全局状态 | Context、Zustand、Redux Toolkit 等 | Pinia 是 Vue 官方推荐状态管理方案 |
| 学习时的重点 | JSX、Props、State、Hook 和单向数据流 | 模板指令、响应式数据、组件通信和组合式 API |

下面两段代码表达的目标相同，都是显示一个标题，但写法不同。

React：

```jsx
function PageTitle() {
  const title = '员工管理';
  return <h1>{title}</h1>;
}
```

Vue：

```vue
<script setup>
const title = '员工管理';
</script>

<template>
  <h1>{{ title }}</h1>
</template>
```

两者没有适用于所有项目的绝对优劣。真实项目通常根据现有技术栈、团队经验、组件生态、维护成本和客户要求选择。进入既有项目后，应遵守该项目已经确定的框架和版本，不要因为个人偏好擅自替换。

### 2.4 UI 框架或组件库负责现成的视觉组件

“UI 框架”在企业前端中通常指提供现成样式和组件的工具，例如：

- Bootstrap：提供布局、样式和常见 UI 组件，可用于普通网页。
- MUI、Ant Design：常用于 React 项目。
- Vuetify：基于 Vue 的 UI 组件框架。

它们通常提供 Button、Dialog、Table、Form、Tabs、Pagination 等组件，帮助团队保持画面风格一致，并减少从零编写样式和交互的工作量。

```text
React / Vue
负责组件逻辑、状态和页面组织
        ↓
MUI / Ant Design / Vuetify 等 UI 组件库
提供现成的按钮、表格、对话框和样式
        ↓
业务页面
```

React 和 MUI 不是二选一，Vue 和 Vuetify 也不是二选一。常见组合是：

```text
React + MUI
React + Ant Design
Vue + Vuetify
Vue + 其他 Vue 组件库
```

UI 组件库不能替代 React 或 Vue 的状态管理和页面逻辑，也不能替代 HTML、CSS 和可访问性知识。组件库提供了一个 Button，但“何时禁用、点击后保存什么、失败时显示什么”仍由业务代码决定。

### 2.5 各层技术如何配合

可以用下面的关系理解本课程中的技术位置：

| 层次 | 主要职责 | 本课程示例 |
| --- | --- | --- |
| HTML | 表达页面语义和结构 | `button`、`form`、`table` |
| CSS | 控制布局和外观 | 颜色、间距、Flex、Grid |
| JavaScript | 提供数据和程序逻辑 | 函数、数组、事件、异步请求 |
| React | 用组件和状态组织 UI | Component、Props、State、Hook |
| UI 组件库 | 提供现成视觉组件 | Dialog、Table、Pagination |
| Vite | 创建、开发和构建项目 | 开发服务器、热更新、生产构建 |

本课程先使用 React 和基础 HTML/CSS 完成核心功能。只有在项目确实需要统一视觉组件时，再引入对应 UI 组件库。

## 3. 创建课程项目

本章使用 JavaScript 创建最基础的 React 项目。请在准备存放项目的目录中打开终端，然后执行：

```bash
npm create vite@9.2.1 employee-app -- --template react
cd employee-app
npm install
npm run dev
```

各条命令的作用如下：

- `npm create vite@9.2.1 employee-app -- --template react`：使用 Vite 的 React 模板创建 `employee-app` 项目。
- `cd employee-app`：进入刚创建的项目目录。
- `npm install`：根据 `package.json` 安装项目需要的依赖，并生成 `package-lock.json`。
- `npm run dev`：启动开发服务器。

命令中的 `employee-app` 是项目目录名，`react` 表示创建使用 JavaScript 的 React 项目。创建命令已经准备好 React 和 Vite 的基础配置，不需要逐个手动安装这些依赖。

终端显示本地地址后，在浏览器中打开该地址。修改并保存 `src/App.jsx`，页面应自动更新。项目创建成功后，应提交 `package.json` 和 `package-lock.json`，不要提交 `node_modules`。

## 4. 启动链路

```text
index.html
    ↓ 加载
src/main.jsx
    ↓ 渲染根组件
src/App.jsx
    ↓ 组合
其他 Component
```

`main.jsx` 的核心代码如下：

```jsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import './index.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
```

`document.getElementById('root')` 获取 `index.html` 中的根元素；`createRoot` 在该元素上创建 React 根；`render` 指定首先显示的根组件。这里的 `root` 必须与 `index.html` 中的 `id="root"` 保持一致。

## 5. 开发、构建与预览

```bash
npm run dev
npm run build
npm run preview
```

- `dev` 启动开发服务器与热更新。
- `build` 执行生产构建并生成 `dist`。
- `preview` 本地预览已构建结果，不是正式生产服务器。

成功标准：开发页可打开；修改组件后可观察变化；构建无错误；预览页可访问。停止持续运行的命令使用 `Ctrl+C`。

## 6. 常见错误

- 找不到 `package.json`：当前目录不是 `employee-app`。
- 白屏：同时检查开发终端与浏览器 Console；再确认根元素和导入路径。
- 把 `dist` 当源码：`dist` 会被下一次构建覆盖，只修改 `src`。
- 提交 `node_modules`：依赖应由锁文件重建，不应提交依赖目录。

## 7. 练习

1. 创建项目并记录 Node/npm 版本、创建命令和本地地址。
2. 修改 `App.jsx`，把欢迎页替换为“员工管理系统”，确认热更新。
3. 执行 build 和 preview，保存成功证据。
4. 故意写错 `App.jsx` 的导入路径，根据终端报错定位后恢复。
5. 分别说明 React、Vite 和 UI 组件库在项目中承担什么任务。

## 本章检查点

- [ ] 能画出 `index.html → main.jsx → App.jsx`。
- [ ] 能区分 JavaScript、React/Vue、UI 组件库与 Vite。
- [ ] 能说明 React 和 Vue 的共同点及主要写法差异。
- [ ] 能完成开发、构建和预览。
