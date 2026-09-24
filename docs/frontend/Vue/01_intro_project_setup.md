# 第 1 章 认识 Vue 与创建项目

Vue 是用于构建用户界面的 JavaScript 框架。它在 HTML、CSS 和 JavaScript 之上提供声明式模板、响应式状态和组件模型。本章创建后续课程使用的本地项目，并确认开发、构建和预览命令可以执行。

## 本章目标

完成本章后，你应当能够：

- 【必须掌握】说明浏览器、Vue、Vite、Node.js和npm分别负责什么。
- 【必须掌握】使用`create-vue`创建并启动最小Vue项目。
- 【必须掌握】沿着`index.html → main.js → App.vue`找到页面入口。
- 【必须掌握】执行开发、构建和本地预览命令，并判断结果是否成功。
- 【会使用、能看懂】认识ESLint和热更新在正式项目中的作用。

## 前置知识检查

Vue课程不会重新完整讲解下面这些基础。若多数项目不能独立完成，应先回到对应课程复习。

### JavaScript

- 使用`let`、`const`、普通函数和箭头函数。
- 创建对象和数组，使用解构、展开语法、`map()`、`filter()`、`find()`、`some()`和`every()`。
- 使用`import`、`export`组织模块。
- 使用`Promise`、`async/await`和`try...catch`处理异步操作。

### 浏览器与事件

- 知道DOM表示浏览器中的页面结构。
- 能说明事件对象、`event.target`与`event.currentTarget`的基本区别。
- 使用过`submit`、`input`、`change`事件和`preventDefault()`。

### HTTP

- 知道GET用于读取数据、POST常用于提交新数据。
- 能识别常见HTTP状态和JSON数据。
- 使用过`fetch()`发送基础请求。

### Node.js与npm

- 知道`npm install`会根据`package.json`安装依赖。
- 知道`scripts`保存项目命令，`node_modules`保存本地安装的包。
- 能在终端进入指定目录并执行npm命令。

### Git快速确认

- `git status`确认当前分支以及哪些文件发生了变化。
- `git diff`在提交前检查实际修改内容。
- `git add <文件>`只把本次任务需要的文件加入暂存区，`git commit`保存一组有意义的修改记录。
- `.gitignore`声明不应进入版本库的本地文件；`node_modules`体积大且可由锁文件重新安装，不能提交。
- `package-lock.json`记录实际解析出的依赖版本。团队项目通常应提交锁文件，使其他成员和构建环境得到一致依赖。

这里只快速确认Vue项目开发所需操作。提交规则、分支方式和Commit格式以所在项目为准。

本章只检查这些能力是否具备。Vue特有的响应式、指令和组件通信将在后续章节逐步学习。

## 1. Vue 解决什么问题

原生 JavaScript 通常要手动查询 DOM、修改文本并管理事件。Vue 让开发者先声明“状态对应怎样的界面”，状态变化后由框架更新相关 DOM。

### 1.1 Vue是什么

Vue是运行在浏览器中的**前端用户界面框架**。它建立在HTML、CSS和JavaScript之上，不会取代这些基础：

- HTML仍然负责表达页面结构和语义；
- CSS仍然负责页面外观和布局；
- JavaScript仍然负责数据处理和业务逻辑；
- Vue负责把数据、操作逻辑和页面结构组织起来，并保持数据与界面同步。

可以把Vue理解为浏览器页面中的“界面组织与更新层”。开发者描述当前有哪些状态、界面如何使用这些状态，以及用户操作后怎样修改状态；Vue负责把变化正确反映到DOM。

### 1.2 Vue在Web系统中承担什么任务

在普通业务系统中，Vue主要承担以下任务：

| 任务 | 示例 |
| --- | --- |
| 根据数据生成界面 | 把任务数组显示为任务列表 |
| 响应用户操作 | 点击新增、删除、提交和筛选按钮 |
| 管理页面状态 | 保存输入内容、选中项、加载状态和错误信息 |
| 自动更新DOM | 任务状态改变后更新文字、按钮和样式 |
| 拆分与组合界面 | 把搜索区、表单、列表和任务项拆成组件 |
| 组织页面之间的导航 | 配合Vue Router切换列表页和详情页 |
| 共享前端业务状态 | 配合Pinia让多个页面使用同一份任务数据 |
| 接入后端接口 | 调用API并显示加载、成功、空数据和失败状态 |

以WorkHub任务列表为例，Vue中的处理流程可以概括为：

```text
后端返回任务数据
        ↓
Vue保存页面状态
        ↓
模板生成任务列表
        ↓
用户点击“完成”
        ↓
事件函数修改任务状态
        ↓
Vue更新对应的页面内容
```

开发者不需要在每次状态变化后分别查找标题、按钮和样式节点再手工修改，界面由状态统一驱动。

### 1.3 Vue不负责什么

理解Vue的边界同样重要：

- Vue不是数据库，不负责永久保存业务数据；
- Vue不是Java或Node.js后端，不负责服务端业务规则和数据库事务；
- Vue不能代替服务端校验、登录认证和权限控制；
- Vue不是构建工具，开发服务器和生产构建由Vite等工具负责；
- Vue不会自动决定页面需求、组件边界或接口规格，这些仍要根据设计书和项目规范实现。

因此，一个常见Web系统中的职责关系是：

```text
浏览器显示和接收操作
        ↓
Vue组织前端页面与状态
        ↓ HTTP请求
后端执行权限、业务规则和数据处理
        ↓
数据库持久化数据
```

前端隐藏“删除”按钮只改变用户看到的界面，后端仍然必须判断当前用户是否真的具有删除权限。

Vue 的两个核心特点是：

- 声明式渲染：模板描述状态应如何显示。
- 响应式更新：Vue 跟踪状态，在状态变化后更新界面。

完整 SPA 通常还会使用 Vue Router、Pinia、测试工具和构建工具。这些属于 Vue 生态，不等于 Vue 核心本身。

## 2. 本课程采用的开发方式

主线使用：

- Vue 3；
- Composition API；
- `.vue`单文件组件；
- `<script setup>`和JavaScript；
- Vite 开发与构建；
- npm 管理依赖。

如果项目已有锁定文件和版本要求，应遵守项目说明，不要自行升级。

## 3. Vite 是什么

Vite 是前端开发和构建工具，不是 Vue 框架，也不是新的编程语言。本课程使用它处理浏览器不能直接完成的开发工作。

### 3.1 为什么 Vue 项目需要构建工具

浏览器不能直接识别`.vue`单文件组件，也不会自动完成下面这些工作：

- 把组件中的`<script>`、`<template>`和`<style>`转换为浏览器可以执行的内容；
- 解析从`node_modules`导入的Vue和其他依赖；
- 修改源码后立即把变化反映到正在打开的页面；
- 整理并优化生产环境需要的JavaScript、CSS和静态资源。

Vite连接源码、Vue插件和浏览器。开发者仍然编写Vue组件，Vite负责在开发和构建阶段处理这些文件。

### 3.2 开发服务器与生产构建

Vite主要承担两类工作：

| 阶段 | npm命令 | Vite做什么 | 学员观察什么 |
| --- | --- | --- | --- |
| 开发 | `npm run dev` | 启动本地服务器，处理模块和Vue组件，监听文件修改 | 保存代码后页面快速更新 |
| 构建 | `npm run build` | 整理、打包和优化生产资源 | 生成`dist`目录 |
| 本地预览 | `npm run preview` | 用本地服务器预览已经构建的`dist` | 确认生产构建可以打开 |

开发服务器提供的快速更新通常称为**热更新（HMR）**。它可以在修改组件后更新相关模块，减少整页重新加载，但不能代替最终构建检查。

### 3.3 Vue、Vite、Node.js和npm的关系

| 名称 | 本课程中的职责 |
| --- | --- |
| Vue | 提供组件、模板和响应式等界面开发能力 |
| Vite | 启动开发服务器并构建生产资源 |
| Node.js | 在本地运行Vite、脚手架等开发工具 |
| npm | 安装依赖并执行`package.json`中的脚本 |
| create-vue | 创建符合Vue官方结构的新项目 |

页面最终仍在浏览器中运行。Node.js在这里主要用于开发工具，不代表当前项目是Node.js后端。

第一阶段不需要自己编写复杂的Vite配置。先会运行`dev`、`build`和`preview`，并知道配置文件属于构建工具即可。

## 4. 创建项目

先确认已安装 Vue 官网当前要求的 Node.js 版本，并完成本仓库的 Node.js/npm 基础课程。在准备保存练习项目的目录执行：

```bash
npm create vue@latest
```

脚手架询问功能时，本课程统一选择：

```text
Project name: vue-task-app
Add TypeScript: No
Add JSX Support: No
Add Vue Router: No
Add Pinia: No
Add Vitest: No
Add an End-to-End Testing Solution: No
Add ESLint: Yes
Add Prettier: No
```

ESLint用于检查代码中不符合规则或容易出错的写法。正式项目通常会使用Lint，本课程先启用基础配置，第21章再系统整理检查与交付流程。如果脚手架继续询问其他实验性功能，本课程选择`No`，避免提前引入主线之外的工具。

提示项目可能随脚手架版本调整，以终端实际文字为准。创建后执行：

```bash
cd vue-task-app
npm install
npm run dev
```

终端会显示本地访问地址。浏览器打开后应看到 Vue 初始页面。`npm run dev`启动开发服务器并持续监听修改，使用`Ctrl+C`停止。

## 5. 认识项目结构

```text
vue-task-app/
├─ public/
├─ src/
│  ├─ assets/
│  ├─ components/
│  ├─ App.vue
│  └─ main.js
├─ index.html
├─ package.json
├─ package-lock.json
└─ vite.config.js
```

- `index.html`是浏览器最先读取的HTML入口，其中包含Vue应用的挂载位置。
- `main.js`是JavaScript入口，创建并挂载Vue应用。
- `App.vue`是根组件，相当于整个组件树的起点。
- `components`保存页面中使用的子组件。
- `assets`保存需要经过Vite处理的CSS、图片等资源。
- `public`保存不经过源码导入、按原文件名直接提供的静态资源。
- `package.json`记录直接依赖和脚本，锁定文件记录实际安装版本。
- `vite.config.js`保存Vite配置，不是Vue组件配置。

脚手架版本可能调整欢迎页组件和文件名称，但`index.html → main.js → App.vue`这条启动关系不会因为欢迎页外观变化而改变。

## 6. 初始项目是怎样显示出来的

### 6.1 从index.html找到挂载位置

初始`index.html`的核心结构通常如下：

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vite App</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- `<div id="app"></div>`是Vue应用要接管的位置，初始时内部是空的。
- `<script type="module" src="/src/main.js">`让浏览器从`main.js`开始执行项目代码。
- `type="module"`表示使用ES Module，因此可以在JavaScript源码中使用`import`和`export`。
- 页面标题、`lang`等内容应在正式项目中按项目语言和画面规格修改。

不要把业务页面直接全部写入这个`index.html`。Vue页面结构主要写在`.vue`组件中。

### 6.2 main.js的初始内容

脚手架生成的`src/main.js`通常类似下面这样：

```js
import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'

createApp(App).mount('#app')
```

逐行理解：

```js
import './assets/main.css'
```

导入项目公共CSS。它没有接收返回值，只是让Vite把这份样式加入页面。组件自己的局部样式后续写在对应`.vue`文件中。

```js
import { createApp } from 'vue'
```

从Vue包中导入`createApp()`。这个函数用于创建一个Vue应用实例。

```js
import App from './App.vue'
```

导入根组件`App.vue`。`./`表示从当前`main.js`所在目录开始查找。

```js
createApp(App).mount('#app')
```

这行可以拆成两步理解：

```js
const app = createApp(App)
app.mount('#app')
```

- `createApp(App)`以`App.vue`作为根组件创建Vue应用。
- `mount('#app')`查找`index.html`中的`id="app"`元素，并把应用挂载进去。

如果`#app`写错、`index.html`中没有对应元素，应用就无法正确显示。后续安装Router和Pinia时，也会在`mount()`之前通过这个应用实例完成注册。

### 6.3 App.vue的初始内容

不同版本的`create-vue`可能生成不同欢迎页。常见的初始`App.vue`会导入演示组件：

```vue
<script setup>
import HelloWorld from './components/HelloWorld.vue'
import TheWelcome from './components/TheWelcome.vue'
</script>

<template>
  <header>
    <img alt="Vue logo" src="./assets/logo.svg">
    <HelloWorld msg="You did it!" />
  </header>

  <main>
    <TheWelcome />
  </main>
</template>

<style scoped>
/* 脚手架生成的根组件演示样式 */
</style>
```

这段代码的职责是：

- `<script setup>`导入当前模板要使用的`HelloWorld`和`TheWelcome`组件。
- `<template>`描述根组件的页面结构。
- `<img>`显示`src/assets/logo.svg`中的Vue标志。
- `<HelloWorld>`和`<TheWelcome>`是脚手架提供的演示子组件，不是HTML原生标签。
- `msg="You did it!"`把一段欢迎文字交给`HelloWorld`；这种父子传值会在第11章详细学习。
- `<style scoped>`保存主要作用于当前组件的样式，`scoped`会在第2章说明。

欢迎页组件用于证明项目创建成功，并不是Vue项目必须保留的文件。开始课程示例后，可以按章节要求删除演示组件和演示样式，但不要同时删除仍被`App.vue`导入的文件，否则构建会提示找不到模块。

### 6.4 components和assets中的初始文件

脚手架常在`src/components`中放置`HelloWorld.vue`、`TheWelcome.vue`等欢迎页组件，在`src/assets`中放置Vue标志、公共CSS和示例样式。

处理初始文件时遵循以下顺序：

1. 先确认某个文件是否仍被`App.vue`、`main.js`或其他文件导入。
2. 从使用方删除对应导入和组件标签。
3. 保存并确认页面、终端和Console没有错误。
4. 再删除已经没有引用的演示文件。

不要看到`components`和`assets`中的文件“暂时用不到”就一次全部删除。先查引用再修改，是实际项目中避免影响遗漏的基本习惯。

### 6.5 初始项目的执行顺序

```text
浏览器读取index.html
        ↓
加载src/main.js和公共CSS
        ↓
main.js导入App.vue
        ↓
createApp(App)创建Vue应用
        ↓
mount('#app')挂载到页面
        ↓
App.vue继续组合各个子组件
```

学员现阶段不需要理解Vue内部如何把模板编译成渲染函数，但需要能够沿着导入关系找到“页面从哪里开始、当前内容由哪个组件生成”。

## 7. 开发、构建和预览

```bash
npm run build
npm run preview
```

`build`成功后生成`dist`，`preview`会显示另一个本地访问地址，用于预览这次构建结果。预览结束后按`Ctrl+C`停止。Router、Pinia和测试工具会在真正使用它们的章节分别安装；前20章统一使用JavaScript，第21章再为已经掌握的Vue写法增加TypeScript约束。

不要直接双击`index.html`，也不要把`dist`手工改成源码。开发时修改`src`，由 Vite 处理模块和单文件组件。

## 8. WorkHub项目的起始状态

后续章节会逐步把这个最小项目改造成WorkHub任务管理系统。本章不实现任务功能，只建立一个可持续使用的项目起点：

```text
vue-task-app/
├─ src/
│  ├─ App.vue
│  └─ main.js
├─ index.html
├─ package.json
└─ package-lock.json
```

本章结束时必须满足：开发服务器可以启动，欢迎页可以显示，构建能够生成`dist`，预览能够打开构建结果。不要提前安装Router、Pinia或测试工具；它们会在实际使用的章节加入。

## 9. 常见错误与排查

### 9.1 终端提示找不到npm

先执行`node --version`和`npm --version`。如果命令不存在，说明Node.js尚未正确安装或终端没有读取到安装路径。修复环境后重新打开终端。

### 9.2 在错误目录执行命令

`npm install`和`npm run dev`必须在包含`package.json`的项目目录中执行。出现“找不到package.json”一类提示时，先确认当前目录是否为`vue-task-app`。

### 9.3 页面空白或终端提示找不到模块

检查`App.vue`或`main.js`是否仍然导入了已删除的文件，再检查`mount('#app')`与`index.html`中的`id="app"`是否一致。浏览器Console和开发服务器终端都要查看。

### 9.4 修改文件后页面没有变化

确认开发服务器仍在运行、浏览器打开的是终端显示的本地地址，并确认保存的是当前项目中的文件。HMR失效时可以先刷新页面，再查看终端是否存在编译错误。

### 9.5 把dist当成源码修改

`dist`是构建结果，下次执行`npm run build`会重新生成。开发时只修改`src`等源码文件。

## 10. 实际项目注意事项

- 进入已有项目时，先阅读README和`package.json`，使用仓库约定的Node.js版本与npm命令。
- 已存在`package-lock.json`时不要随意删除或改用其他包管理器；依赖版本变化可能影响整个团队。
- 不要把`node_modules`和构建生成的`dist`当作自己编写的源码提交，是否提交以仓库规则为准。
- 修改或删除脚手架文件前先查找引用，并同时检查页面、终端和浏览器Console。

## 11. 本章练习

1. 创建项目并保存终端选择结果。
2. 找到`index.html`、`main.js`、`App.vue`和`package.json`，说明各自职责。
3. 启动开发服务器，记录访问地址。
4. 分别执行`dev`、`build`和`preview`，记录各自产生的可观察结果；如果启用了ESLint，再执行`npm run lint`。
5. 用自己的话说明Vue、Vite、Node.js和npm分别负责什么。
6. 把`createApp(App).mount('#app')`拆成两行，并确认页面结果不变。
7. 沿着`App.vue`的导入找到欢迎页子组件和图片资源。
8. 先移除一个演示组件的导入和标签，再删除文件，确认终端和Console没有错误。

提交练习结果时，至少保留项目创建选项、各命令的成功结果和最终页面截图，作为自测证据。

## 本章检查点

- [ ] 能解释浏览器、Vue、Vite、Node.js和npm各自的职责。
- [ ] 能独立创建项目并在正确目录安装依赖。
- [ ] 能说明`index.html → main.js → App.vue`的启动顺序。
- [ ] 能说明`createApp(App)`与`mount('#app')`分别做什么。
- [ ] 能执行开发、Lint、构建和预览命令，并根据终端输出判断是否成功。
- [ ] 能通过终端、浏览器Console和导入关系排查最基本的空白页问题。
- [ ] 能在删除初始演示文件前先移除引用，保持项目可运行。




