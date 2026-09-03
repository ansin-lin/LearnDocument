# 第九章 DOM 基础、元素获取与节点操作

## 学习目标

完成本章后，你应能够：

- 说明 DOM、`document`、元素节点和节点树之间的关系。
- 使用 id、标签名、class、name 和 CSS 选择器获取页面元素。
- 区分单个元素、`HTMLCollection` 和 `NodeList`。
- 读取和修改文本、表单值、HTML 属性、`data-*` 属性、样式和 class。
- 通过父子、兄弟关系查找相邻元素。
- 创建、插入、移动、替换、删除和克隆元素。

## 示例运行约定

- 标注为“HTML”的短代码是 `<body>` 内的内容，直接放入练习页面的 `<body>` 中。
- 标注为“JavaScript”的代码放入通过 `defer` 加载的 `js/app.js`。
- 标注为“CSS”的样式放入练习页面 `head` 内的 `<style>` 标签，或放入该页面已加载的样式文件；只有添加 class、没有定义相应样式时，外观可能不变。
- 标明“继续使用”的片段接在指定准备代码后；每遇到“独立实验”，用对应 HTML 和脚本替换上一组内容后保存、刷新。
- 查询表格中的短调用是写法对照，不把整张表复制成程序。标明“错误示例”“语法格式”或“替代写法”的代码不与正确示例连续执行。
- 不把全章脚本拼成一个文件。同名变量在不同实验中会重复出现，合并会导致重复声明错误。
- 每个 DOM 示例都提供所需元素，不需要自行猜测或补写 id、class、name。
- 使用控制台和 Elements 面板检查 DOM 操作结果。

## 掌握要求

- **必须掌握**：`getElementById()`、`querySelector()`、`querySelectorAll()`、`textContent`、`value`、`classList`、属性操作和元素创建。
- **需要掌握**：`getElementsByTagName()`、`getElementsByClassName()`、元素集合、节点关系、插入和删除。
- **会使用、能看懂**：`getElementsByName()`、`insertBefore()`、`replaceWith()`、`cloneNode()` 和 `DocumentFragment`。
- **了解即可**：文本节点、注释节点和 `nodeType`；实际项目优先操作元素节点。

## 本章学习路线

先操作一个元素，再处理多个元素和页面结构。每个阶段都要在浏览器中确认变化，不能只看到控制台没有报错就认为完成。

| 阶段 | 对应小节 | 学习成果 |
| --- | --- | --- |
| 找到并修改一个元素 | 第1～7节 | 修改文字、输入值和 class，处理未找到的情况 |
| 多元素查询与集合 | 第8～12节 | 理解集合，再逐一学习查询方式并比较结果 |
| 查询范围与属性 | 第13～15节 | 缩小查询范围，读写标准属性和自定义数据 |
| 节点关系与操作 | 第16～19节 | 区分子元素和子节点，创建、插入、移动与删除 |
| 集合验证与节点扩展 | 第20～22节 | 验证动态、静态集合，使用克隆和 Fragment |
| 综合操作与排错 | 第23～25节 | 运行、修改并验证 DOM 任务 |

## 1. DOM 是什么

浏览器读取 HTML 后，会把页面转换成可以由 JavaScript 操作的对象结构。这套结构称为 DOM（Document Object Model，文档对象模型）。

HTML：

```html
<body>
  <main>
    <h1>休假申请</h1>
    <p id="message">请输入申请内容</p>
  </main>
</body>
```

可以简单理解为下面的树形关系：

```text
document
└─ html
   ├─ head
   └─ body
      └─ main
         ├─ h1
         └─ p#message
```

这棵树描述的是页面中谁包含谁。比如 `main` 包含标题和提示段落。要修改提示文字，JavaScript 需要先找到那一个段落，再修改它的文本；只知道文字内容，并不能直接确定应该操作哪个元素。

## 2. document 与节点

`document` 表示当前浏览器页面的 HTML 文档，是查找和创建页面节点的主要入口。

```js
console.log(document);
console.log(document.documentElement); // <html>
console.log(document.head);            // <head>
console.log(document.body);            // <body>
```

DOM 树中的每一项称为节点。常见节点包括：

| 节点类型 | `nodeType` | 示例 | 本课程要求 |
| --- | --- | --- | --- |
| 元素节点 | `1` | `<p>`、`<input>` | 必须掌握 |
| 文本节点 | `3` | 元素中的文字和换行空白 | 能识别 |
| 注释节点 | `8` | `<!-- comment -->` | 了解 |
| 文档节点 | `9` | `document` | 能识别 |

```js
console.log(document.body.nodeType); // 1
console.log(document.nodeType);      // 9
```

`nodeType` 是只读数字属性，用来判断当前节点的种类；`documentElement`、`head`、`body` 分别取得根元素、头部元素和正文元素。节点不等于标签：标签之间的换行也可能成为文本节点。后面读取子节点时，这一区别会影响集合数量。

## 3. 获取元素前先确认脚本执行时机

JavaScript 只能获取已经被浏览器解析的元素。下面是可直接运行的最小页面。本课程统一在 `head` 中使用外部脚本并添加 `defer`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DOM 练习</title>
  <script src="js/app.js" defer></script>
</head>
<body>
  <p id="message">脚本尚未执行</p>
</body>
</html>
```

```js
const message = document.getElementById("message");
console.log(message);
```

`document.getElementById("message")` 按 id 值查找元素，参数是字符串且不带 `#`，找到时返回元素，找不到返回 `null`。这里输出的应是 `<p id="message">…</p>`，不是段落的文字。

如果元素不存在、名称写错或脚本执行过早，获取结果可能是 `null`。

```js
const missingElement = document.getElementById("notExists");
console.log(missingElement); // null
```

## 4. 通过 id 获取单个元素

### 4.1 `document.getElementById()`

`getElementById()` 根据元素的 `id` 属性获取一个元素。

```html
<p id="statusText">未提交</p>
```

```js
const statusText = document.getElementById("statusText");

console.log(statusText);
```

- 参数：id 的实际值，例如 `"statusText"`。
- 可接受的值：字符串，不写 `#`。
- 返回值：匹配的元素；找不到时返回 `null`。

正确：

```js
document.getElementById("statusText");
```

错误：

```js
document.getElementById("#statusText");
```

因为 `getElementById()` 接收 id 值，不接收 CSS 选择器。

先确认能输出元素本身，再使用下一节的文本属性读取内容。如果结果是 `null`，应先修正查询，而不是继续读写它的属性。

一个页面中的 id 应保持唯一。确定元素有稳定且唯一的 id 时，`getElementById()` 简单直观，在实际项目中仍然常用。

## 5. 读取和修改元素内容

### 5.1 `textContent`

`textContent` 读取或修改元素内部的纯文本。

```html
<p id="statusText">未提交</p>
```

```js
const statusText = document.getElementById("statusText");

console.log(statusText.textContent); // 未提交
statusText.textContent = "申请已提交";
```

设置 `textContent` 时，字符串中的 `<`、`>` 不会被当作 HTML 标签执行，适合显示用户输入和接口数据。

### 5.2 `innerText`

本小节接着使用 5.1 的 HTML 和变量 `statusText`；不需要再次声明变量。

`innerText` 更接近页面上实际可见的文字，会受 CSS 显示状态和布局影响。

```js
console.log(statusText.innerText);
```

一般文本读写优先使用 `textContent`。只有明确需要“页面上可见的文字”时再使用 `innerText`。

### 5.3 `innerHTML`

`innerHTML` 读取或替换元素内部的 HTML。

```html
<div id="resultArea"></div>
```

```js
const resultArea = document.getElementById("resultArea");
resultArea.innerHTML = "<strong>申请成功</strong>";
```

不要把用户输入、URL 参数或未经可信处理的接口内容直接拼入 `innerHTML`，否则可能造成跨站脚本攻击。显示普通文字使用 `textContent`；需要新建页面结构时，使用后面“创建元素”与“插入元素”的方法。

## 6. 读取和修改表单状态

### 6.1 `value`

输入框、文本域和下拉框的当前值使用 `value`。

```html
<label for="userName">姓名</label>
<input id="userName" type="text" value="田中">
```

```js
const userNameInput = document.getElementById("userName");

console.log(userNameInput.value); // 田中
userNameInput.value = "佐藤";
```

### 6.2 `checked`、`selected` 和 `disabled`

```html
<label>
  <input id="agree" type="checkbox" checked>
  确认申请内容
</label>
<button id="submitButton" type="button">提交</button>
```

```js
const agreeCheckbox = document.getElementById("agree");
const submitButton = document.getElementById("submitButton");

console.log(agreeCheckbox.checked); // 初始为 true
submitButton.disabled = true;
```

| 控件或状态 | 常用属性 | 可接受的值 |
| --- | --- | --- |
| 文本框、日期、文本域、下拉框 | `value` | 字符串 |
| 单选框、复选框 | `checked` | `true`、`false` |
| `<option>` | `selected` | `true`、`false` |
| 可禁用控件 | `disabled` | `true`、`false` |

`checked` 表示是否勾选，`disabled` 表示是否禁用，二者都读写布尔值。上例会让提交按钮不可操作；把赋值改为 `false` 后刷新，按钮恢复可用。

`selected` 表示某个下拉选项是否选中。下面是独立实验：

```html
<label for="demoType">休假类型</label>
<select id="demoType">
  <option id="paidOption" value="paid">有给休假</option>
  <option id="halfOption" value="half-am">上午休</option>
</select>
```

```js
const halfOption = document.getElementById("halfOption");
const demoType = document.getElementById("demoType");
halfOption.selected = true;
console.log(demoType.value); // half-am
```

普通单选下拉框也可以直接设置 `demoType.value = "paid"` 来选择指定值；读取整张表单时通常使用下拉框的 `value`。

HTML 属性表示初始状态，DOM 属性通常表示当前状态。例如用户点击复选框后，应读取 `checked`，而不是只读取 HTML 中是否写了 `checked` 属性。

## 7. 修改样式与 class

本节示例统一使用下面的元素：

```html
<p id="styleMessage">请输入申请内容</p>
```

### 7.1 `style`

`style` 用于读取和设置元素的行内样式。

```js
const message = document.getElementById("styleMessage");

message.style.color = "red";
message.style.backgroundColor = "#fff4f4";
```

CSS 中带连字符的属性，在 JavaScript 中通常写成小驼峰形式：

```text
background-color → backgroundColor
font-size        → fontSize
```

`style` 主要读取和修改行内样式，不能用来完整判断外部样式表计算后的结果。

### 7.2 `className`

`className` 读取或替换完整的 class 字符串。

```js
message.className = "message is-error";
```

重新赋值会覆盖原有全部 class，因此项目中添加和删除单个 class 时优先使用 `classList`。

### 7.3 `classList`

`classList` 是管理元素 class 名的列表接口，不是 class 字符串。下面继续使用 7.1 创建的 `message` 变量；先执行 7.2，再运行本节代码。每个方法操作一个或多个 class 名，不需要重写整个 class 字符串。

```js
message.classList.add("is-error");
message.classList.remove("is-error");
message.classList.toggle("is-hidden");
console.log(message.classList.contains("is-error")); // false
message.classList.replace("message", "notice");
```

| 方法 | 可接受的值 | 默认值或必填性 | 返回值 | 作用 |
| --- | --- | --- | --- | --- |
| `add()` | 一个或多个不含空白的 class 名字符串 | 至少一个，必填 | 无 | 添加 class |
| `remove()` | 一个或多个不含空白的 class 名字符串 | 至少一个，必填 | 无 | 删除 class |
| `toggle()` | class 名；第二个参数可为 `true` 或 `false` | class 名必填；布尔值可省略 | 布尔值 | 切换或强制设置 class |
| `contains()` | 一个不含空白的 class 名字符串 | 必填 | 布尔值 | 判断是否存在 class |
| `replace()` | 旧 class 名、新 class 名 | 两个参数必填 | 布尔值 | 替换 class |

推荐让 CSS 负责具体样式，JavaScript 只切换表达状态的 class：

```css
.is-hidden {
  display: none;
}

.is-error {
  color: #b42318;
}
```

### 阶段验证：确认修改发生在页面上

用前面的小示例分别完成三项检查：文字从“未提交”变为“申请已提交”；输入框从“田中”变为“佐藤”；在 Elements 中确认 `classList` 操作确实改变了 class。修改 `textContent`、`value` 和 class 是三种不同操作，不能互相替代。

前面的脚本在页面加载时执行一次。现在手动输入新文字，并不会让脚本自动再次执行；需要响应用户操作时，要绑定下一章讲解的事件。

## 8. 使用 CSS 选择器获取元素

### 8.1 `document.querySelector()`

`querySelector()` 使用 CSS 选择器查找第一个匹配元素。

HTML：

```html
<form id="applyForm">
  <p class="message">请输入申请内容</p>
  <label>
    姓名
    <input type="text" required>
  </label>
  <button type="submit">提交</button>
</form>
```

JavaScript：

```js
const applyForm = document.querySelector("#applyForm");
const firstMessage = document.querySelector(".message");
const firstButton = document.querySelector("button");
const requiredInput = document.querySelector("input[required]");
```

- 参数：有效的 CSS 选择器字符串。
- 返回值：第一个匹配元素；找不到返回 `null`。
- 选择器语法错误：抛出 `SyntaxError`。

### 8.2 `document.querySelectorAll()`

一个元素只能代表一个按钮。要一次取得两个按钮，浏览器会把匹配结果装进一个**集合**：集合本身不是按钮，需要取出其中一项才能修改按钮。

`NodeList` 的意思是“节点列表”，是浏览器提供的集合类型。它有长度和下标，形状像数组，但不是 JavaScript 数组。列表中允许出现哪些节点，由产生它的接口决定；这里查找的是元素，所以结果中只包含元素节点。

`querySelectorAll(selector)` 按 CSS 选择器查找所有匹配元素，返回一个 `NodeList`。它的成员名单在查询完成时确定，之后不会自动增减，这称为**静态集合**。与之相对，**动态集合**会随着相关 DOM 的变化更新成员。静态不表示元素不可修改，也不表示复制了元素。

```html
<button class="action-button">申请</button>
<button class="action-button">取消</button>
```

```js
const buttons = document.querySelectorAll(".action-button");

console.log(buttons.length); // 2
console.log(buttons[0].textContent); // 申请
console.log(buttons[1].textContent); // 取消
```

- 参数：有效的 CSS 选择器字符串。
- 返回值：静态 `NodeList`；没有匹配项时长度为 `0`。

### 8.3 常见选择器

| 查询目标 | CSS 选择器 | JavaScript 示例 |
| --- | --- | --- |
| id | `#statusText` | `document.querySelector("#statusText")` |
| class | `.message` | `document.querySelectorAll(".message")` |
| 标签 | `button` | `document.querySelectorAll("button")` |
| 属性存在 | `[required]` | `document.querySelectorAll("[required]")` |
| 属性值 | `[type="email"]` | `document.querySelector('[type="email"]')` |
| 后代元素 | `form input` | `document.querySelectorAll("form input")` |
| 多个条件 | `input.is-error` | `document.querySelector("input.is-error")` |

### 8.4 从集合取出元素并遍历

以下代码继续使用 8.2 的两个按钮和 `buttons` 变量。长度 `length` 表示数量，下标从 0 开始；取得单个元素后，才访问它的 `textContent` 等属性。

```js
if (buttons.length > 0) {
  buttons[0].textContent = "新建申请";
}

for (const button of buttons) {
  console.log(button.textContent);
}
```

这里先修改第一项，再依次输出“新建申请”和“取消”。`button` 每轮接收一个元素，而 `buttons` 始终是整个集合。

`NodeList` 也提供 `forEach(callback)`：参数是逐项执行的函数，会收到当前节点、下标和该列表，方法返回 `undefined`。下面是上面循环的替代写法，不要误以为它返回新数组：

```js
buttons.forEach((button, index) => {
  console.log(index, button.textContent);
});
```

输出下标 0 对应“新建申请”，下标 1 对应“取消”。`forEach` 的回调思路与第七章相同，但现在处理的是 DOM 节点。

### 8.5 没找到元素时怎样判断

下面仍使用 8.2 的页面。页面没有 `.not-exists` 元素：

```js
const missingOne = document.querySelector(".not-exists");
const missingMany = document.querySelectorAll(".not-exists");

console.log(missingOne);         // null
console.log(missingMany.length); // 0
console.log(missingMany[0]);     // undefined
console.log(missingMany.item(0)); // null
```

`item(index)` 按从 0 开始的整数下标取得节点，下标必填，越界返回 `null`；方括号越界则返回 `undefined`。两种写法任选一种即可，不要混淆它们的空结果。

- 单个查询用 `result === null` 判断未找到。
- 集合查询用 `result.length === 0` 判断没有匹配项。
- 空集合不是 `null`，也不会因为长度为 0 就在 `if (result)` 中变成假。
- 即使只匹配一个元素，`querySelectorAll()` 返回的也仍然是集合。

以下为错误用法对照，不与正确实验一起运行：

```js
console.log(buttons.textContent); // undefined：集合没有整体文本属性
// buttons.classList.add("is-active"); // 错误：集合不是单个元素
// missingMany[0].textContent; // 错误：第 0 项不存在
```

### 8.6 集合为什么不能直接用数组方法

有 `length` 和下标不代表具有全部数组方法。DOM 集合没有数组的 `push()`、`map()`、`filter()`；`NodeList` 有自己的 `forEach()`，也仍然不是数组。

需要转换数据时，先用已经学过的循环，把想要的文字放入真正的数组：

```js
const buttonTexts = [];
for (const button of buttons) {
  buttonTexts.push(button.textContent);
}
console.log(buttonTexts); // ["新建申请", "取消"]
```

本例把文字放进数组，不会改变按钮数量。修改页面结构要使用后面的节点插入与删除方法，不能对 DOM 集合调用 `push()` 来增加元素。

## 9. 通过标签名获取多个元素

### 9.1 `document.getElementsByTagName()`

`getElementsByTagName()` 根据标签名获取所有匹配元素，返回另一种集合：`HTMLCollection`。

`HTMLCollection` 是**元素集合**，只包含元素，不包含换行形成的文本节点或注释节点。它也提供 `length`、下标和 `item(index)`，但不是数组。本章这些接口返回的 `HTMLCollection` 是动态的：匹配元素增加、删除或不再满足条件时，同一个集合会更新，不必重新查询。

下面先读取集合中的元素；动态变化的实验在完成节点增删后进行。

```html
<ul>
  <li>有给休假</li>
  <li>上午休</li>
  <li>下午休</li>
</ul>
```

```js
const items = document.getElementsByTagName("li");

console.log(items.length); // 3
console.log(items[0].textContent); // 有给休假

for (const item of items) {
  console.log(item.textContent);
}
```

- 参数：标签名字符串，例如 `"li"`、`"input"`；`"*"` 表示所有元素。
- 返回值：包含匹配元素的动态 `HTMLCollection`；没有匹配项时长度为 `0`。

即使只找到一个元素，返回结果仍然是集合，需要通过下标取得具体元素。

`HTMLCollection` 可以使用普通 `for` 或 `for...of` 遍历，但它没有 `NodeList` 的 `forEach()`。不要因为两种集合都有长度就认为方法完全相同。

下面继续使用本节 `items`，比较两种取值方式：

```js
console.log(items.item(0).textContent); // 有给休假
console.log(items[99]);                // undefined
console.log(items.item(99));           // null
```

`item(index)` 的参数、返回值规则与前面 `NodeList.item()` 相同。只要下标可能越界，就要先检查长度，不能直接读取越界结果的文本。

## 10. 通过 class 获取多个元素

### 10.1 `document.getElementsByClassName()`

`getElementsByClassName()` 根据 class 名获取元素，返回动态 `HTMLCollection`。

```html
<p class="message">账号不能为空</p>
<p class="message">密码不能为空</p>
```

```js
const messages = document.getElementsByClassName("message");

console.log(messages.length); // 2
console.log(messages[0].textContent); // 账号不能为空
```

- 参数：一个或多个 class 名，不写 `.`。
- 可接受的值：例如 `"message"`、`"message is-error"`。
- 返回值：动态 `HTMLCollection`。

正确：

```js
document.getElementsByClassName("message");
```

错误：

```js
document.getElementsByClassName(".message");
```

查找同时拥有多个 class 的元素时，使用空格分隔 class 名。下面继续在本节页面实验，先给第一段添加错误状态，再查询：

```js
messages[0].classList.add("is-error");
const errorMessages = document.getElementsByClassName("message is-error");
console.log(errorMessages.length); // 1
```

## 11. 通过 name 获取表单元素

### 11.1 `document.getElementsByName()`

`getElementsByName()` 根据 `name` 属性获取元素，常用于同组单选按钮和复选框。

```html
<label><input type="radio" name="leaveType" value="paid"> 有给休假</label>
<label><input type="radio" name="leaveType" value="half-am"> 上午休</label>
```

```js
const leaveTypeRadios = document.getElementsByName("leaveType");

console.log(leaveTypeRadios.length); // 2
console.log(leaveTypeRadios[0].value); // paid
```

- 参数：`name` 属性值字符串。
- 返回值：包含匹配元素的动态 `NodeList`；没有匹配项时长度为 0。

同样叫 `NodeList`，本方法的结果会自动更新，而 `querySelectorAll()` 的结果不会。因此不能把 `NodeList` 一律记成静态集合，要同时记住由哪个接口产生。

使用已经学过的属性选择器也能找到这些控件：

```js
const queriedRadios = document.querySelectorAll('[name="leaveType"]');
console.log(queriedRadios.length); // 2
```

`querySelectorAll(selector)` 使用 CSS 选择器取得所有匹配元素，并返回一个静态 `NodeList`；用法见第 8.2 节。

## 12. 查询方法与返回结果对照

| 方法 | 查询方式 | 返回结果 | 常见用途 |
| --- | --- | --- | --- |
| `getElementById()` | id 值 | 单个元素或 `null` | 已知唯一 id |
| `getElementsByTagName()` | 标签名 | 动态 `HTMLCollection` | 阅读旧代码、按标签批量查询 |
| `getElementsByClassName()` | class 名 | 动态 `HTMLCollection` | 阅读旧代码、按 class 批量查询 |
| `getElementsByName()` | name 值 | 动态 `NodeList` | 表单同名控件 |
| `querySelector()` | CSS 选择器 | 单个元素或 `null` | 现代项目获取单个元素 |
| `querySelectorAll()` | CSS 选择器 | 静态 `NodeList` | 现代项目获取多个元素 |

项目主线推荐：唯一 id 可以使用 `getElementById()`；其他单个或组合条件使用 `querySelector()`；多个元素使用 `querySelectorAll()`。按 id、class、标签查询的方法仍然有效，并非已经废弃；按具体需求选择，不需要把所有写法混在一个查询中。



| 比较点 | HTMLCollection | NodeList |
| --- | --- | --- |
| 表达什么 | 一组元素 | 一组节点，具体节点种类取决于来源 |
| 当前查询示例包含什么 | 元素 | 元素；节点关系中的列表还可能有文本和注释 |
| 是否数组 | 否 | 否 |
| 取得数量、下标取值、item() | 支持 | 支持 |
| for、for...of | 支持 | 支持 |
| forEach() | 不提供 | 提供 |
| map()、filter()、push() | 不提供 | 不提供 |
| 成员是否自动更新 | 本章返回的都是动态集合 | 取决于来源：querySelectorAll 静态，getElementsByName 动态 |

先决定要找一个还是多个，再决定按 id、标签、class、name 还是 CSS 条件查找。查到集合后，继续问“取哪一项，还是逐项处理”，不要直接把集合当作页面元素。

## 13. 在指定元素内部继续查询

`querySelector()`、`querySelectorAll()`、`getElementsByTagName()` 和 `getElementsByClassName()` 也可以从某个元素开始查询。

```html
<section id="applicationArea">
  <p class="message">申请区域提示</p>
</section>

<section id="loginArea">
  <p class="message">登录区域提示</p>
</section>
```

```js
const applicationArea = document.getElementById("applicationArea");
const message = applicationArea.querySelector(".message");

console.log(message.textContent); // 申请区域提示
```

先找到范围较小的父元素，再从内部查询，可以减少同名元素造成的混淆。

## 14. 读取和修改 HTML 属性

### 14.1 直接使用 DOM 属性

常见标准属性可以直接读写：

```html
<a id="completeLink" href="index.html">完成页面</a>
<img id="systemLogo" src="images/old-logo.png" alt="旧系统标志">
<button id="attributeSubmitButton" type="button">提交</button>
```

```js
const link = document.getElementById("completeLink");
const image = document.getElementById("systemLogo");
const submitButton = document.getElementById("attributeSubmitButton");

link.href = "complete.html";
image.src = "images/logo.png";
image.alt = "系统标志";
submitButton.disabled = true;
```

### 14.2 `getAttribute()` 和 `setAttribute()`

```html
<input id="accountId" type="text" aria-invalid="false">
```

```js
const accountId = document.getElementById("accountId");

console.log(accountId.getAttribute("aria-invalid")); // false
accountId.setAttribute("aria-invalid", "true");
```

- `getAttribute(name)`：读取指定属性，找不到返回 `null`。
- 找到属性时返回字符串。例如上例输出的 `false` 实际是字符串 `"false"`，不是布尔值 `false`；不要直接把它当作控件当前的布尔状态。
- `setAttribute(name, value)`：设置属性；值会转换为字符串。

### 14.3 `hasAttribute()` 和 `removeAttribute()`

下面的代码片段接在 14.2 的 JavaScript 后面运行，继续使用其中定义的 `accountId`：

```js
console.log(accountId.hasAttribute("required"));

accountId.setAttribute("required", "");
accountId.removeAttribute("required");
```

- `hasAttribute(name)` 返回布尔值。
- `removeAttribute(name)` 删除指定属性，没有返回值。

布尔属性更推荐使用对应 DOM 属性：

```js
const submitButton = document.getElementById("submitButton");

submitButton.disabled = true;
submitButton.disabled = false;
```

不要写：

```js
submitButton.setAttribute("disabled", "false");
```

只要 HTML 中存在 `disabled` 属性，即使属性值是字符串 `"false"`，控件仍然处于禁用状态。
这里的 `submitButton` 对应 6.2 中的按钮；单独实验时同时复制 6.2 的 HTML。

## 15. `data-*` 与 `dataset`

`data-*` 用于在元素上保存与页面行为相关的自定义数据。

```html
<button id="cancelButton" data-application-id="REQ-001">
  取消申请
</button>
```

```js
const cancelButton = document.getElementById("cancelButton");

console.log(cancelButton.dataset.applicationId); // REQ-001
cancelButton.dataset.applicationId = "REQ-002";
```

HTML 中的连字符名称会转换成小驼峰形式：

| HTML 属性 | `dataset` 写法 |
| --- | --- |
| `data-id` | `dataset.id` |
| `data-user-id` | `dataset.userId` |
| `data-application-status` | `dataset.applicationStatus` |

`dataset` 读写的值都是字符串。不要在 HTML 中保存密码、令牌或其他秘密数据。

## 16. 通过节点关系查找元素

有时已经取得一个元素，需要继续查找它的父元素、子元素或兄弟元素。

```html
<ul id="applicationList">
  <li class="application-item">第一条申请</li>
  <li class="application-item">第二条申请</li>
</ul>
```

### 16.1 父元素

本节的父子、兄弟实验使用上面的列表。16.1、16.2、16.3 的脚本依次追加；16.4、16.5 各自使用独立 HTML 与脚本。

```js
const firstItem = document.querySelector(".application-item");

console.log(firstItem.parentElement); // <ul id="applicationList">
```

`parentElement` 返回父元素，没有父元素时返回 `null`。

### 16.2 子元素

`children` 返回当前元素的直接子元素集合，是动态 `HTMLCollection`；它不包含孙子元素和空白文本。

```js
const list = document.getElementById("applicationList");

console.log(list.children);          // HTMLCollection
console.log(list.firstElementChild); // 第一个 li
console.log(list.lastElementChild);  // 最后一个 li
console.log(list.childElementCount); // 2
```

`firstElementChild`、`lastElementChild` 分别取得第一个、最后一个直接子元素，没有子元素时返回 `null`；`childElementCount` 是直接子元素数量，与 `children.length` 一致。

### 16.3 兄弟元素

继续使用 16.1 的 `firstItem` 和 16.2 的 `list`，不要重复声明同名变量：

```js

console.log(firstItem.nextElementSibling); // 第二个 li
console.log(firstItem.previousElementSibling); // null
```

`nextElementSibling`、`previousElementSibling` 分别取得同一父元素下的下一个、上一个兄弟元素，没有对应兄弟时返回 `null`；它们跳过文本和注释节点。

### 16.4 children 与 childNodes 的区别

`childNodes` 返回直接子节点的动态 `NodeList`，其中可能包含元素、文本、注释；`children` 则只返回元素。这是 `NodeList` 不只用于元素查询的例子。

下面是独立实验，保留换行和注释以观察节点差异：

```html
<div id="nodeBox">
  提示文字
  <span>正文</span>
  <!-- 说明 -->
</div>
```

```js
const nodeBox = document.getElementById("nodeBox");
console.log(nodeBox.children.length); // 1：只有 span
for (const node of nodeBox.childNodes) {
  console.log(node.nodeType);
}
```

按原样复制时，节点类型依次输出 `3、1、3、8、3`：文字与空白、span、空白、注释、空白。改变排版空白可能改变文本节点数量，所以不能用 `childNodes.length` 统计子标签数量。`childNodes` 也是动态列表，后面的动态集合实验会验证这一点。

### 16.5 `closest()`

`closest()` 从当前元素开始向上查找第一个匹配 CSS 选择器的元素。

```html
<article class="application-card" data-id="REQ-001">
  <button class="cancel-button">取消</button>
</article>
```

```js
const cancelButton = document.querySelector(".cancel-button");
const card = cancelButton.closest(".application-card");

console.log(card.dataset.id); // REQ-001
```

参数是有效 CSS 选择器字符串，从自身开始逐级查找祖先；找不到返回 `null`。它与只向内部查找的 `querySelector()` 方向不同。上例返回外层 article；实际查询条件不确定时，先判断 `card !== null` 再读取属性。

## 17. 创建元素

### 17.1 `document.createElement()`

`createElement()` 创建一个尚未插入页面的元素。

```js
const item = document.createElement("li");

item.classList.add("application-item");
item.textContent = "2026-09-01：有给休假";
```

- 参数：HTML 标签名字符串，例如 `"li"`、`"p"`、`"button"`。
- 返回值：新创建的元素。

刚创建的元素还不属于页面，需要通过插入方法放进 DOM 树。

## 18. 插入元素

本节演示不同的插入位置。每次只选择一种插入方法：将下面的 HTML 放入页面，把准备代码与当前方法的代码放进 `js/app.js`，保存后刷新。替代写法不连续执行；否则同一个节点会被再次移动，不能看到各方法独立的效果。

准备下面的 HTML：

```html
<ul id="applicationList">
  <li class="application-item">第一条申请</li>
  <li class="application-item">第二条申请</li>
</ul>
```

准备代码：

```js
const list = document.getElementById("applicationList");
const item = document.createElement("li");
item.textContent = "有给休假";
```

### 18.1 `append()` 和 `appendChild()`

```js
list.append(item);
```

`append()` 可以在末尾插入元素或字符串，也可以一次传入多个节点。

```js
list.append("申请：", item);
```

传统代码中经常看到 `appendChild()`：

```js
list.appendChild(item);
```

`appendChild()` 只接收一个节点，并返回插入的节点。两者都能把元素插入父元素末尾。

### 18.2 `prepend()`

`prepend()` 把元素或字符串插入父元素开头。

```js
list.prepend(item);
```

### 18.3 `before()` 和 `after()`

```js
const referenceItem = list.firstElementChild;

referenceItem.before(item);
// 替代实验：把上一行改为 referenceItem.after(item)，保存并刷新。
```

- `before()`：插入到当前元素前面。
- `after()`：插入到当前元素后面。

两者都接收要插入的节点或字符串，可以有多个参数，返回 `undefined`。`append()` 与 `prepend()` 同样返回 `undefined`：它们把内容放在父元素里面；`before()` 与 `after()` 把内容放在参考元素旁边。

### 18.4 `insertBefore()`

旧代码和部分通用逻辑中常见：

```js
list.insertBefore(item, list.firstElementChild);
```

```text
parent.insertBefore(新节点, 参考节点);
```

- 新节点：要插入的节点。
- 参考节点：新节点插入到它之前；传入 `null` 时插入末尾。
- 返回值：插入后的节点。

### 18.5 清空并重新填充容器

`replaceChildren()` 不传参数时删除容器的全部子节点，容器本身仍保留；传入节点或字符串时用它们替换原来的子节点。常用于重新渲染列表，返回 `undefined`。

下面是独立实验，HTML 与脚本配套运行：

```html
<ul id="refreshList"><li>旧记录</li></ul>
```

```js
const list = document.getElementById("refreshList");
list.replaceChildren();
const item = document.createElement("li");
item.textContent = "新记录";
list.append(item);
```

页面最终只保留“新记录”。与直接删除整个 `ul` 不同，后续还可以向同一个容器继续添加内容。

## 19. 移动、替换和删除元素

下面的各个代码块分别作为一次独立实验。每次恢复下面含两条记录的 HTML，并只运行当前代码块，不沿用插入实验中的 `item` 变量。

```html
<ul id="applicationList">
  <li class="application-item">第一条申请</li>
  <li class="application-item">第二条申请</li>
</ul>
```

### 19.1 移动元素

一个元素已经在 DOM 中时，再把它插入其他位置，会移动原元素，而不是自动复制。

```js
const list = document.getElementById("applicationList");
const firstItem = list.firstElementChild;
list.append(firstItem);
```

执行后，顺序变成“第二条申请、第一条申请”，总数仍为 2。移动不是创建新元素。

### 19.2 `replaceWith()`

```js
const list = document.getElementById("applicationList");
const oldItem = list.firstElementChild;
const newItem = document.createElement("li");
newItem.textContent = "替换后的申请";

oldItem.replaceWith(newItem);
```

`replaceWith()` 使用一个或多个节点或字符串替换当前元素，返回 `undefined`。上例最后为“替换后的申请、第二条申请”。

旧代码中可能看到：

```js
const list = document.getElementById("applicationList");
const oldItem = list.firstElementChild;
const newItem = document.createElement("li");
newItem.textContent = "替换后的申请";

list.replaceChild(newItem, oldItem);
```

`replaceChild(newNode, oldNode)` 在父元素上用新节点替换指定的旧子节点，并返回被替换的旧节点。

### 19.3 `remove()`

```js
const list = document.getElementById("applicationList");
const item = list.firstElementChild;
item.remove();
```

`remove()` 不接收参数，直接从父元素中删除当前元素，返回 `undefined`。上例最后只剩“第二条申请”。

传统写法：

```js
const list = document.getElementById("applicationList");
const item = list.firstElementChild;
list.removeChild(item);
```

`removeChild(child)` 从父元素中删除指定子节点，并返回被删除的节点。

删除后元素对象仍可以保存在变量中，也可以再次插入页面。

## 20. 动态集合与静态集合的操作验证

### 20.1 HTMLCollection 自动更新，静态 NodeList 保留原成员

下面是独立实验，使用本节 HTML 和脚本：

```html
<ul id="liveList">
  <li>第一项</li>
</ul>
```

```js
const list = document.getElementById("liveList");
const liveItems = list.getElementsByTagName("li");
const staticItems = list.querySelectorAll("li");
console.log(liveItems.length, staticItems.length); // 1 1

const item = document.createElement("li");
item.textContent = "第二项";
list.append(item);
console.log(liveItems.length, staticItems.length); // 2 1

staticItems[0].textContent = "第一项已修改";
console.log(liveItems[0].textContent); // 第一项已修改
console.log(list.querySelectorAll("li").length); // 2：重新查询
```

保存集合变量后，新增节点仍使 `liveItems.length` 自动变为 2；`staticItems` 的成员仍只有最初的一项。两个集合中的第一项却是同一个页面元素，因此通过一个集合改文字，页面及另一个集合都能看到。

“静态”只表示成员名单不自动更新，不表示元素的内容被冻结，也不表示克隆了一份 DOM。

### 20.2 NodeList 也可以动态更新

下面是另一份独立 HTML。使用同名复选框，比较三个查询来源：

```html
<div id="choiceArea">
  <label><input type="checkbox" name="demoChoice" value="first"> 第一项</label>
</div>
```

```js
const area = document.getElementById("choiceArea");
const namedItems = document.getElementsByName("demoChoice");
const queriedItems = area.querySelectorAll('[name="demoChoice"]');
const childNodes = area.childNodes;
const oldChildCount = childNodes.length;

const label = document.createElement("label");
const checkbox = document.createElement("input");
checkbox.setAttribute("type", "checkbox");
checkbox.setAttribute("name", "demoChoice");
checkbox.value = "second";
label.append(checkbox, " 第二项");
area.append(label);

console.log(namedItems.length); // 2：动态 NodeList
console.log(queriedItems.length); // 1：静态 NodeList
console.log(childNodes.length === oldChildCount + 1); // true
```

`getElementsByName()` 的集合增加了新控件，`childNodes` 增加了新 label 节点，而之前保存的 `querySelectorAll()` 结果仍是一项。动态或静态必须看产生集合的接口，不能只看 `NodeList` 这个名称。

### 20.3 一边遍历动态集合，一边改变成员可能漏项

当查询条件是 class，移除 class 也会让元素离开集合，不一定要删除页面元素。

独立 HTML：

```html
<p class="pending-item">第一项</p>
<p class="pending-item">第二项</p>
<p class="pending-item">第三项</p>
```

下面是故意有问题的实验：

```js
const items = document.getElementsByClassName("pending-item");
for (let i = 0; i < items.length; i += 1) {
  items[i].classList.remove("pending-item");
}
console.log(items.length); // 1：仍有第二项没有处理
```

第一轮处理后，第一项离开集合，原第二项移动到下标 0；但 i 增加到 1，接着处理原第三项，于是漏掉原第二项。

将整份脚本替换为以下版本并刷新，让 HTML 恢复原来的三项：

```js
const items = document.querySelectorAll(".pending-item");
for (const item of items) {
  item.classList.remove("pending-item");
}
console.log(items.length); // 3：保存的成员名单不变
console.log(document.querySelectorAll(".pending-item").length); // 0
```

集合长度与当前页面匹配数量是两个问题。批量修改时使用静态查询结果，可以避免这里的下标移动问题；需要当前匹配数量时重新查询。

## 21. 克隆元素

`cloneNode()` 克隆当前节点。下面是独立实验，不依赖前面小节的 `list` 变量：

```html
<ul id="cloneList">
  <li class="application-item">原申请</li>
</ul>
```

```js
const list = document.getElementById("cloneList");
const originalItem = document.querySelector(".application-item");
const copiedItem = originalItem.cloneNode(true);

list.append(copiedItem);
```

- 参数：可选的布尔值，省略时默认为 `false`。
- `false`：只复制当前节点，不复制子节点。
- `true`：同时复制所有后代节点。
- 返回值：尚未插入页面的克隆节点。

克隆元素时注意：

- 克隆结果不会自动插入页面。
- `id` 也会被复制，插入前应避免产生重复 id。
- 克隆 DOM 节点不等于复制全部交互行为；下一章讲解事件绑定后，还需要分别确认原元素和克隆元素是否能响应操作。
- 表单控件的部分当前状态需要单独确认和设置。

## 22. 使用 DocumentFragment 批量插入

需要一次创建多项内容时，可以先放入 `DocumentFragment`，最后统一插入页面。

`document.createDocumentFragment()` 创建一个不直接显示在页面中的临时节点容器，适合先集中组织一批待插入节点。

HTML：

```html
<ul id="applicationList"></ul>
```

JavaScript：

```js
const applications = [
  "2026-09-01：有给休假",
  "2026-09-05：上午休"
];

const list = document.getElementById("applicationList");
const fragment = document.createDocumentFragment();

for (const application of applications) {
  const item = document.createElement("li");
  item.textContent = application;
  fragment.append(item);
}

list.append(fragment);
```

`DocumentFragment` 是临时容器。插入页面时，它的子节点会进入目标元素，Fragment 本身不会成为页面标签，原 Fragment 的子节点随之清空。本例先遍历字符串数组，创建两个 li，再将它们一起放入页面，不需要对象数组。

数据量不大时直接使用 `append()` 也完全可以。需要集中组织一批节点时，再使用 Fragment。

## 23. 完整操作顺序

DOM 操作通常按照以下顺序编写：

1. 获取目标元素。
2. 判断获取结果是否正确。
3. 读取需要的数据。
4. 创建或修改元素。
5. 插入、替换或删除节点。
6. 在 Elements 面板和页面中确认结果。

HTML：

```html
<p id="statusText">未提交</p>
<ul id="applicationList"></ul>
```

JavaScript：

```js
const statusText = document.getElementById("statusText");
const applicationList = document.getElementById("applicationList");

if (statusText === null || applicationList === null) {
  console.error("请检查 statusText 和 applicationList 是否存在");
} else {
  statusText.textContent = "申请中";
  statusText.classList.add("is-pending");

  const item = document.createElement("li");
  item.dataset.applicationId = "REQ-001";
  item.textContent = "2026-09-01：有给休假";
  applicationList.append(item);
}
```

## 24. 常见错误与排查

| 症状 | 常见原因 | 检查方法 | 修正方式 |
| --- | --- | --- | --- |
| `Cannot set properties of null` | 没找到元素 | 输出变量，检查 id 和选择器 | 修正名称，确认使用 `defer` |
| 集合的 `textContent` 是 `undefined` | 把集合当成单个元素 | 输出集合和 `length` | 使用下标或循环 |
| class 查询不到 | 把 `.` 写进 `getElementsByClassName()` | 检查参数 | 只传 class 名 |
| id 查询不到 | 把 `#` 写进 `getElementById()` | 检查参数 | 只传 id 值 |
| 设置 `disabled="false"` 仍然禁用 | 布尔属性只看是否存在 | Elements 面板查看属性 | 使用 `element.disabled = false` |
| 修改 `innerHTML` 后内容异常 | 拼接了不可信或格式错误的 HTML | 检查字符串来源 | 使用 `textContent` 和 `createElement()` |
| 克隆后出现重复 id | `cloneNode()` 复制了 id | Elements 面板搜索 id | 插入前删除或修改 id |

## 25. 本章练习

使用下面的初始 HTML：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DOM 练习</title>
  <script src="js/dom-practice.js" defer></script>
</head>
<body>
  <main>
    <h1 id="pageTitle">休假申请</h1>
    <p class="message">请输入申请内容</p>

    <label>
      休假类型
      <select id="leaveType">
        <option value="paid">有给休假</option>
        <option value="half-am">上午休</option>
      </select>
    </label>

    <ul id="applicationList">
      <li class="application-item" data-id="REQ-001">已有申请</li>
    </ul>
  </main>
</body>
</html>
```

在 `js/dom-practice.js` 中完成：

1. 分别使用 `getElementById()`、`getElementsByClassName()` 和 `querySelector()` 获取元素并输出。
2. 使用 `querySelectorAll()` 获取所有 `li`，输出集合长度。
3. 把标题修改为“休假申请一览”。
4. 读取下拉框当前的 `value`。
5. 给提示文字添加 `is-info` class 和 `aria-live="polite"` 属性。
6. 创建一个新的 `<li>`，设置文字和 `data-id` 后插入列表末尾。
7. 克隆新元素，修改克隆元素的 `data-id` 和文字后再次插入。
8. 使用节点关系取得列表的第一个和最后一个子元素。
9. 删除原有的 `REQ-001` 元素。
10. 在 Elements 面板确认最终只有两个新建元素，并且不存在重复 id。
11. 在新增前分别保存列表的 `children` 与 `querySelectorAll("li")` 结果，在新增后、删除后输出两者长度，并重新查询确认页面实际数量。
12. 把查询条件临时改为不存在的 class，验证空集合不会进入逐项循环；恢复后再完成验收。

## 本章检查点

- 能说明 DOM 树、`document` 和元素节点的关系。
- 能根据查询目标选择 id、class、标签名、name 或 CSS 选择器。
- 能区分单个元素、`HTMLCollection` 和 `NodeList`，并根据产生它们的接口判断动态或静态。
- 能用长度判断空集合，取出元素后修改；不会把 DOM 集合当作普通数组。
- 能解释 `children` 与 `childNodes` 在文本、注释和数量上的区别。
- 能读取和修改文本、表单状态、属性、`dataset`、样式和 class。
- 能通过父子、兄弟关系找到相邻元素。
- 能创建、插入、移动、替换、删除和克隆元素。
- 能通过控制台和 Elements 面板排查 DOM 获取与修改问题。


## 参考资料

集合的类型与动态性可对照 [MDN：HTMLCollection](https://developer.mozilla.org/en-US/docs/Web/API/HTMLCollection)、[MDN：NodeList](https://developer.mozilla.org/en-US/docs/Web/API/NodeList) 和 [MDN：getElementsByName](https://developer.mozilla.org/en-US/docs/Web/API/Document/getElementsByName)。
