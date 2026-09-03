# 第一章 认识 JavaScript 与运行方式

## 学习目标

完成本章后，你应能够：

- 说明 JavaScript 在网页中负责什么。
- 区分 JavaScript 的行内、内部和外部三种引入方式。
- 使用外部 `.js` 文件加载脚本，并说明推荐它的原因。
- 理解 `defer` 的作用。
- 使用浏览器控制台查看输出和错误。

## 1. JavaScript 解决什么问题

HTML 负责页面结构，CSS 负责页面样式，JavaScript 负责页面行为。

常见的网页行为包括：

- 点击按钮后显示或隐藏一段内容。
- 在输入框中输入内容后，立即显示输入结果。
- 提交表单前，检查必填项是否已经填写。
- 切换标签页、菜单或图片。
- 根据数据变化，更新页面上显示的文字和数字。

例如，页面上原本显示“未登录”，用户完成登录后变成“已登录”。HTML 提供显示文字的位置，CSS 决定文字的外观，JavaScript 根据用户操作修改文字内容。

现阶段先理解 JavaScript 可以让页面根据操作和数据发生变化。后续学习 DOM 和事件时，再编写这类交互功能。

## 2. JavaScript 的三种引入方式

浏览器只有在 HTML 中加载了 JavaScript，才会执行相应代码。JavaScript 有三种常见的引入方式。

### 2.1 行内引入

行内引入是把 JavaScript 直接写在 HTML 元素的事件属性中。

```html
<button onclick="alert('按钮被点击了')">点击按钮</button>
```

`onclick` 表示单击事件，属性值中的代码会在按钮被单击时执行。
`alert(message)` 用于让浏览器显示一个包含提示文字的对话框；当前示例中的提示文字是“按钮被点击了”。

这种写法虽然直观，但 HTML 结构和 JavaScript 行为混在一起，代码较多时不便阅读和维护。本课程只用它认识写法，不作为项目中的推荐方式。

### 2.2 内部引入

内部引入是使用 `<script>` 标签，把 JavaScript 写在当前 HTML 文件中。

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>JavaScript 示例</title>
</head>
<body>
  <h1>JavaScript 示例</h1>

  <script>
    console.log("页面脚本开始执行");
  </script>
</body>
</html>
```

`console.log(value)` 用于把指定内容输出到浏览器开发者工具的 Console 面板，常用于确认代码是否执行以及查看数据。本例会输出“页面脚本开始执行”。

内部引入适合代码量很少的临时实验。代码增多后，HTML 和 JavaScript 仍然会混在同一个文件中，也不能方便地供多个页面共用。

### 2.3 外部引入（推荐）

外部引入是把 JavaScript 单独保存在 `.js` 文件中，再通过 `<script>` 标签的 `src` 属性加载。

HTML：

```html
<script src="js/app.js" defer></script>
```

目录：

```text
paid-leave-system/
├─ index.html
└─ js/
   └─ app.js
```

`src="js/app.js"` 表示从当前 HTML 文件所在目录开始，进入 `js` 文件夹，加载 `app.js`。

`app.js`：

```js
console.log("外部 JavaScript 文件已加载");
```

外部引入具有以下优点：

- HTML 和 JavaScript 分开，文件职责清楚。
- 多个 HTML 页面可以共用同一个 JavaScript 文件。
- 代码较多时更容易查找、修改和维护。

因此，本课程和后续项目统一优先使用外部引入。

注意：带有 `src` 的 `<script>` 标签用于加载外部文件，不要再在标签内部编写代码。

```html
<!-- 错误示例：加载外部文件时，不要再在标签内部写代码 -->
<script src="js/app.js">
  console.log("这段代码不会执行");
</script>
```

## 3. 如何选择引入方式

| 引入方式 | 代码位置 | 适用场景 | 是否推荐用于项目 |
| --- | --- | --- | --- |
| 行内引入 | HTML 元素的事件属性中 | 认识事件写法、极小的临时演示 | 不推荐 |
| 内部引入 | HTML 文件的 `<script>` 标签中 | 少量临时实验 | 一般不推荐 |
| 外部引入 | 独立的 `.js` 文件中 | 课程练习和实际项目 | 推荐 |

实际开发中优先选择外部引入。本课程统一采用下面的写法：

```html
<head>
  <meta charset="UTF-8">
  <title>JavaScript 示例</title>
  <script src="js/app.js" defer></script>
</head>
```

### 3.1 `src` 指定脚本路径

`src` 用来指定要加载的 JavaScript 文件。路径以当前 HTML 文件的位置为起点。

假设目录如下：

```text
paid-leave-system/
├─ index.html
└─ js/
   └─ app.js
```

因为 `index.html` 和 `js` 文件夹位于同一级，所以写成：

```html
<script src="js/app.js" defer></script>
```

如果路径写错，浏览器就无法加载脚本，`app.js` 中的代码也不会执行。

### 3.2 `defer` 控制执行时机

`defer` 表示：先解析 HTML，等页面结构准备好后再执行 JavaScript。

```html
<script src="js/app.js" defer></script>
```

这对 DOM 操作很重要。如果脚本太早执行，页面元素还没解析出来，JavaScript 就找不到对应元素。

因此，外部脚本放在 `head` 中时，本课程统一添加 `defer`。这样既能尽早开始加载脚本，又不会阻塞页面结构的解析。

```html
<head>
  <meta charset="UTF-8">
  <title>JavaScript 示例</title>
  <script src="js/app.js" defer></script>
</head>
```

`defer` 只适用于通过 `src` 加载的外部脚本。本课程现阶段不展开其他脚本加载方式。

## 4. 完成一次外部脚本运行

新建独立目录 `js-intro-practice`，创建 `index.html` 和 `js/app.js`。把第 2.2 节完整 HTML 保存到 `index.html`，删除其中的内部脚本，在 `head` 内加入 `<script src="js/app.js" defer></script>`。不要同时保留内部、外部两份实验脚本。

在 `js/app.js` 中写：

```js
console.log("JavaScript が読み込まれました");
```

保存两个文件，用浏览器打开 `index.html`，按 F12 打开开发者工具并选择 Console。应看到一行“JavaScript が読み込まれました”。页面正文不会因为 `console.log()` 自动增加文字。

把消息改为“脚本修改成功”，保存后刷新页面，应看到新的消息。如果没有输出，先确认已保存，再在 Network 中检查 `js/app.js` 是否加载成功；不要把脚本路径错误误认为 JavaScript 语法错误。

`console.log()` 常用于：

- 确认脚本是否加载。
- 查看变量当前值。
- 临时确认代码执行到了哪里。

注意：不要在控制台输出真实密码、密钥、令牌或个人隐私信息。

## 5. 看懂常见错误

在上一节脚本末尾临时追加下面的错误示例，保存并刷新。观察后删除这一行，再刷新确认恢复正常：

```js
console.log(userName);
```

如果 `userName` 没有定义，控制台可能显示：

```text
ReferenceError: userName is not defined
```

含义：

- `ReferenceError`：引用了不存在的变量。
- `userName is not defined`：变量 `userName` 没有定义。

初学阶段看到错误不要直接删除代码。先看三点：

1. 错误类型是什么。
2. 错误信息提到哪个变量或函数。
3. 错误发生在哪个文件、哪一行。

## 6. 本章练习

本章使用独立练习目录，不修改综合项目中尚未完成的页面脚本。

1. 新建 `js-intro-practice` 文件夹，在其中创建 `index.html` 和 `js/app.js`。
2. 将第 2.2 节的完整 HTML 复制到 `index.html`，删除内部的 `<script>...</script>`，在 `head` 中添加 `<script src="js/app.js" defer></script>`。
3. 在 `js/app.js` 中写入 `console.log("JS loaded");`。
4. 在浏览器中打开 `index.html`，打开 Console，确认显示 `JS loaded`。
5. 分别说明三种引入方式的位置，并指出推荐的方式。
6. 在 `app.js` 末尾临时添加 `console.log(userName);`，观察未声明变量的错误；删除该行后刷新，恢复正常。

目录中的文件由你在本次练习中新建。如果已有同名实验目录，另取名称，不覆盖原文件。

## 本章检查点

- 能说明 HTML、CSS、JavaScript 的职责区别。
- 能区分 JavaScript 的三种引入方式。
- 能使用并说明为什么推荐外部 `.js` 文件。
- 能说明为什么使用 `defer`。
- 能打开 Console 查看输出和错误。
