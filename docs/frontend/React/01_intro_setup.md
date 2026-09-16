# 第 1 章 React 的定位与创建项目

## 本章目标

- 【必须掌握】说明 React、浏览器、Vite、Node.js 和 npm 的职责。
- 【必须掌握】创建 React + TypeScript 项目并找到入口链路。
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

## 2. 创建课程项目

课程固定使用入口页“封版技术基线”中的版本。Vite 8 要求 Node.js `20.19+` 或 `22.12+`；先用 `node --version` 确认，再执行：

```bash
npm create vite@9.2.1 employee-app -- --template react-ts
cd employee-app
npm install --save-exact react@19.3.0 react-dom@19.3.0
npm install -D --save-exact vite@8.3.0 typescript@7.0.2 @vitejs/plugin-react@6.1.1
npm run dev
```

终端给出本地地址后在浏览器打开。保存 `src/App.tsx` 时应看到页面更新。创建后核对 `package.json`，确认 React、Vite 与 TypeScript 符合课程基线，并提交生成的 `package-lock.json`。以后从全新目录重建使用 `npm ci`，不再重新解析依赖。若模板生成版本与基线不同，先按课程基线调整并重新生成锁文件，再开始后续章节。

## 3. 启动链路

```text
index.html
    ↓ 加载
src/main.tsx
    ↓ 渲染根组件
src/App.tsx
    ↓ 组合
其他 Component
```

`main.tsx` 的核心代码如下：

```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.tsx';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
```

`createRoot` 在 `#root` 上创建 React 根；`render` 指定根组件。`!` 是 TypeScript 非空断言，表示开发者确认元素存在，必须与 `index.html` 中的 `id="root"` 保持一致。

## 4. 开发、构建与预览

```bash
npm run dev
npm run build
npm run preview
```

- `dev` 启动开发服务器与热更新。
- `build` 执行类型检查/生产构建并生成 `dist`。
- `preview` 本地预览已构建结果，不是正式生产服务器。

成功标准：开发页可打开；修改组件后可观察变化；构建无错误；预览页可访问。停止持续运行的命令使用 `Ctrl+C`。

## 5. 常见错误

- 找不到 `package.json`：当前目录不是 `employee-app`。
- 白屏：同时检查开发终端与浏览器 Console；再确认根元素和导入路径。
- 把 `dist` 当源码：`dist` 会被下一次构建覆盖，只修改 `src`。
- 提交 `node_modules`：依赖应由锁文件重建，不应提交依赖目录。

## 6. 练习

1. 创建项目并记录 Node/npm 版本、创建命令和本地地址。
2. 把欢迎页替换为“员工管理系统”，确认热更新。
3. 执行 build 和 preview，保存成功证据。
4. 故意写错 `App.tsx` 的导入路径，根据终端报错定位后恢复。

## 本章检查点

- [ ] 能画出 `index.html → main.tsx → App.tsx`。
- [ ] 能区分 React 与 Vite。
- [ ] 能完成开发、构建和预览。
