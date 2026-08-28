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

## 4. 控制台输出

在 `app.js` 中写：

```js
console.log("JavaScript が読み込まれました");
```

打开浏览器开发者工具的 Console 面板，可以看到输出。

`console.log()` 常用于：

- 确认脚本是否加载。
- 查看变量当前值。
- 临时确认代码执行到了哪里。

注意：不要在控制台输出真实密码、密钥、令牌或个人隐私信息。

## 5. 看懂常见错误

示例：

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

练习使用 `docs/frontend/training/paid-leave-system/` 中的现有项目文件。本章只修改下面两个文件：

- `login.html`
- `js/app.js`

开始前先确认两个文件已经存在，不新建同名文件。

1. 打开 `js/app.js`，在文件末尾临时添加 `console.log("JS loaded");`。
2. 打开 `login.html`，确认 `head` 中通过外部引入方式加载 `js/app.js`，并添加 `defer`。
3. 在浏览器中打开 `login.html`，打开开发者工具的 Console 面板。
4. 确认 Console 显示 `JS loaded`，证明外部脚本已经成功加载。
5. 分别说明行内、内部和外部引入的代码写在哪里，并指出项目中推荐的方式。
6. 在 `app.js` 末尾故意输出一个不存在的变量，观察 `ReferenceError` 后删除该行，恢复可正常运行的状态。

## 本章检查点

- 能说明 HTML、CSS、JavaScript 的职责区别。
- 能区分 JavaScript 的三种引入方式。
- 能使用并说明为什么推荐外部 `.js` 文件。
- 能说明为什么使用 `defer`。
- 能打开 Console 查看输出和错误。
