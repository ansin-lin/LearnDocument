# 第 1 章 认识 Node.js 与运行环境

## 1. Node.js 是什么

Node.js 是 JavaScript 的运行环境。它让 JavaScript 可以在浏览器之外运行。

```text
JavaScript 语言
   ├─ 浏览器运行环境
   │    ├─ DOM
   │    ├─ window
   │    └─ localStorage
   └─ Node.js 运行环境
        ├─ 命令行程序
        ├─ 开发工具
        └─ 服务端程序
```

同一种语言运行在不同环境中，可以使用的 API 不完全相同。

## 2. 为什么前端项目需要 Node.js

现代前端项目通常需要执行：

- TypeScript 类型检查和编译；
- Vite 开发服务器和构建；
- Vue、React 项目工具；
- ESLint、Prettier；
- Vitest、Jest；
- CSS 预处理和资源打包。

这些工具在开发电脑上通过 Node.js 运行。生成后的网页仍由浏览器执行。

```text
开发阶段
Node.js → 运行 TypeScript、Vite、测试工具
                     ↓
                  生成网页资源
                     ↓
运行阶段
浏览器 → 执行 HTML、CSS、JavaScript
```

## 3. 浏览器与 Node.js 的区别

| 内容 | 浏览器 | Node.js |
| --- | --- | --- |
| DOM | 支持 | 默认不支持 |
| `window` | 支持 | 不支持 |
| `localStorage` | 支持 | 默认不支持 |
| 命令行工具 | 不负责 | 可以运行 |
| 服务端和本地文件能力 | 受浏览器限制 | 可以通过 Node API 使用 |
| npm 工具链 | 不执行 | 可以执行 |

下面的浏览器代码不能直接当作普通 Node.js 代码运行：

```js
document.querySelector("#message");
```

下面的 Node.js API也不能直接放进浏览器页面：

```js
import { readFile } from "node:fs/promises";
```

编写代码前必须确认目标环境。

## 4. 安装与确认

Node.js 版本变化较快，企业项目应优先遵守项目说明或 `.nvmrc` 等版本约定。安装完成后重新打开终端并执行：

```bash
node --version
npm --version
```

预期会分别输出版本号。数字不必和教程完全一致。

## 5. 执行 JavaScript 文件

创建 `hello.js`：

```js
const message = "Node.js が実行されました";

console.log(message);
```

运行：

```bash
node hello.js
```

预期输出：

```text
Node.js が実行されました
```

`node hello.js` 表示让 Node.js 执行该文件。

## 6. REPL

只输入 `node` 会进入交互环境：

```bash
node
```

可以输入简单表达式：

```js
1 + 2
```

退出可以按两次 `Ctrl+C`，或输入：

```text
.exit
```

项目代码应保存在文件中，REPL 只用于快速确认。

## 7. 常见错误

| 现象 | 常见原因 | 处理 |
| --- | --- | --- |
| 找不到 `node` | 未安装或 PATH 未刷新 | 安装后重新打开终端 |
| Cannot find module | 文件路径或依赖错误 | 确认当前目录和路径 |
| `document is not defined` | 在 Node 中执行浏览器代码 | 改到浏览器环境运行 |
| 文件没有执行预期版本 | 电脑存在多个 Node 版本 | 确认 `node --version` 和项目要求 |

## 8. 练习

1. 确认 Node.js 与 npm 版本；
2. 创建并运行 `hello.js`；
3. 在浏览器与 Node.js 中分别尝试输出 `typeof window`；
4. 说明 TypeScript 编译器在哪个环境运行；
5. 记录一个错误现象、原因和修正结果。
