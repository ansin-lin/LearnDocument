# 第 10 章 DOM 基础、元素获取与节点操作

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
- 同一小节连续出现的代码块属于同一个实验；标明“错误示例”“语法格式”或“替代写法”的代码不与正确示例连续执行。
- 每个 DOM 示例都提供所需元素，不需要自行猜测或补写 id、class、name。
- 使用控制台和 Elements 面板检查 DOM 操作结果。

## 掌握要求

- **必须掌握**：`getElementById()`、`querySelector()`、`querySelectorAll()`、`textContent`、`value`、`classList`、属性操作和元素创建。
- **需要掌握**：`getElementsByTagName()`、`getElementsByClassName()`、元素集合、节点关系、插入和删除。
- **会使用、能看懂**：`getElementsByName()`、`insertBefore()`、`replaceWith()`、`cloneNode()` 和 `DocumentFragment`。
- **了解即可**：文本节点、注释节点和 `nodeType`；实际项目优先操作元素节点。

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

JavaScript 可以从 `document` 开始找到某个元素，再读取或修改它。

```js
const message = document.getElementById("message");
message.textContent = "申请内容已填写";
```

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

新人项目主要操作元素节点。使用 `children`、`parentElement` 等元素专用属性，可以避开换行产生的文本节点。

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

如果元素不存在、选择器写错或脚本执行过早，获取结果可能是 `null`。

```js
const missingElement = document.getElementById("notExists");
console.log(missingElement); // null
```

继续访问 `null.textContent` 会产生错误。排错时先输出查询结果，确认不是 `null`。

## 4. 通过 id 获取单个元素

### 4.1 `document.getElementById()`

`getElementById()` 根据元素的 `id` 属性获取一个元素。

```html
<p id="statusText">未提交</p>
```

```js
const statusText = document.getElementById("statusText");

console.log(statusText);
console.log(statusText.textContent); // 未提交
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

一个页面中的 id 应保持唯一。确定元素有稳定且唯一的 id 时，`getElementById()` 简单直观，在实际项目中仍然常用。

## 5. 通过标签名获取多个元素

### 5.1 `document.getElementsByTagName()`

`getElementsByTagName()` 根据标签名获取所有匹配元素，返回 `HTMLCollection`。

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

## 6. 通过 class 获取多个元素

### 6.1 `document.getElementsByClassName()`

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

查找同时拥有多个 class 的元素时，使用空格分隔 class 名：

```js
const errorMessages = document.getElementsByClassName("message is-error");
```

## 7. 通过 name 获取表单元素

### 7.1 `document.getElementsByName()`

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
- 返回值：包含匹配元素的 `NodeList`。

现代代码也经常使用属性选择器完成相同查询：

```js
const leaveTypeRadios = document.querySelectorAll('[name="leaveType"]');
```

`querySelectorAll(selector)` 使用 CSS 选择器取得所有匹配元素，并返回一个静态 `NodeList`；它会在下一节详细说明。

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

`querySelectorAll()` 使用 CSS 选择器查找所有匹配元素，返回静态 `NodeList`。

```html
<button class="action-button">申请</button>
<button class="action-button">取消</button>
```

```js
const buttons = document.querySelectorAll(".action-button");

console.log(buttons.length); // 2

for (const button of buttons) {
  console.log(button.textContent);
}
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

### 8.4 获取方式如何选择

| 方法 | 查询方式 | 返回结果 | 常见用途 |
| --- | --- | --- | --- |
| `getElementById()` | id 值 | 单个元素或 `null` | 已知唯一 id |
| `getElementsByTagName()` | 标签名 | 动态 `HTMLCollection` | 阅读旧代码、按标签批量查询 |
| `getElementsByClassName()` | class 名 | 动态 `HTMLCollection` | 阅读旧代码、按 class 批量查询 |
| `getElementsByName()` | name 值 | `NodeList` | 表单同名控件 |
| `querySelector()` | CSS 选择器 | 单个元素或 `null` | 现代项目获取单个元素 |
| `querySelectorAll()` | CSS 选择器 | 静态 `NodeList` | 现代项目获取多个元素 |

项目主线推荐：唯一 id 可以使用 `getElementById()`；其他单个或组合条件使用 `querySelector()`；多个元素使用 `querySelectorAll()`。传统方法必须能看懂和维护。

## 9. 在指定元素内部继续查询

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

## 10. 单个元素与元素集合

单个元素可以直接访问属性：

```js
const message = document.querySelector(".message");
console.log(message.textContent);
```

集合必须先取得其中的某个元素，或者使用循环：

```js
const messages = document.querySelectorAll(".message");

console.log(messages[0].textContent);

for (const message of messages) {
  console.log(message.textContent);
}
```

错误示例：

```js
const messages = document.querySelectorAll(".message");
console.log(messages.textContent); // undefined
```

### 10.1 动态集合与静态集合

`getElementsByTagName()` 和 `getElementsByClassName()` 返回的 `HTMLCollection` 会随着 DOM 变化自动更新。

```html
<ul id="list">
  <li>第一项</li>
</ul>
```

```js
const liveItems = document.getElementsByTagName("li");
const staticItems = document.querySelectorAll("li");

const list = document.getElementById("list");
const item = document.createElement("li");
item.textContent = "第二项";
list.append(item);

console.log(liveItems.length);   // 2
console.log(staticItems.length); // 1
```

`createElement("li")` 创建一个新的 `<li>` 元素，`append(item)` 把它插入列表末尾。这两个方法会在本章后面的“创建元素”和“插入元素”中详细讲解。

`querySelectorAll()` 返回的 `NodeList` 是查询当时的静态结果。需要最新结果时重新查询。

修改动态集合的同时遍历可能导致下标和长度变化。新人项目需要稳定的查询结果时，优先使用 `querySelectorAll()`；DOM 变化后需要最新结果时，再重新查询。

## 11. 读取和修改元素内容

### 11.1 `textContent`

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

### 11.2 `innerText`

`innerText` 更接近页面上实际可见的文字，会受 CSS 显示状态和布局影响。

```js
console.log(statusText.innerText);
```

一般文本读写优先使用 `textContent`。只有明确需要“页面上可见的文字”时再使用 `innerText`。

### 11.3 `innerHTML`

`innerHTML` 读取或替换元素内部的 HTML。

```html
<div id="resultArea"></div>
```

```js
const resultArea = document.getElementById("resultArea");
resultArea.innerHTML = "<strong>申请成功</strong>";
```

不要把用户输入、URL 参数或未经可信处理的接口内容直接拼入 `innerHTML`，否则可能造成跨站脚本攻击。显示普通文字使用 `textContent`；创建结构使用 `createElement()`。

## 12. 读取和修改表单状态

### 12.1 `value`

输入框、文本域和下拉框的当前值使用 `value`。

```html
<input id="userName" type="text" value="田中">
```

```js
const userNameInput = document.getElementById("userName");

console.log(userNameInput.value); // 田中
userNameInput.value = "佐藤";
```

### 12.2 `checked`、`selected` 和 `disabled`

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

console.log(agreeCheckbox.checked); // true 或 false
submitButton.disabled = true;
```

| 控件或状态 | 常用属性 | 可接受的值 |
| --- | --- | --- |
| 文本框、日期、文本域、下拉框 | `value` | 字符串 |
| 单选框、复选框 | `checked` | `true`、`false` |
| `<option>` | `selected` | `true`、`false` |
| 可禁用控件 | `disabled` | `true`、`false` |

HTML 属性表示初始状态，DOM 属性通常表示当前状态。例如用户点击复选框后，应读取 `checked`，而不是只读取 HTML 中是否写了 `checked` 属性。

## 13. 读取和修改 HTML 属性

### 13.1 直接使用 DOM 属性

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

### 13.2 `getAttribute()` 和 `setAttribute()`

```html
<input id="accountId" type="text" aria-invalid="false">
```

```js
const accountId = document.getElementById("accountId");

console.log(accountId.getAttribute("aria-invalid")); // false
accountId.setAttribute("aria-invalid", "true");
```

- `getAttribute(name)`：读取指定属性，找不到返回 `null`。
- `setAttribute(name, value)`：设置属性；值会转换为字符串。

### 13.3 `hasAttribute()` 和 `removeAttribute()`

下面的代码片段接在 13.2 的 JavaScript 后面运行，继续使用其中定义的 `accountId`：

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
这里的 `submitButton` 对应 12.2 中的按钮；单独实验时同时复制 12.2 的 HTML。

## 14. `data-*` 与 `dataset`

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

## 15. 修改样式与 class

本节示例统一使用下面的元素：

```html
<p id="styleMessage">请输入申请内容</p>
```

### 15.1 `style`

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

### 15.2 `className`

`className` 读取或替换完整的 class 字符串。

```js
message.className = "message is-error";
```

重新赋值会覆盖原有全部 class，因此项目中添加和删除单个 class 时优先使用 `classList`。

### 15.3 `classList`

```js
message.classList.add("is-error");
message.classList.remove("is-error");
message.classList.toggle("is-hidden");
console.log(message.classList.contains("is-error"));
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

## 16. 通过节点关系查找元素

有时已经取得一个元素，需要继续查找它的父元素、子元素或兄弟元素。

```html
<ul id="applicationList">
  <li class="application-item">第一条申请</li>
  <li class="application-item">第二条申请</li>
</ul>
```

### 16.1 父元素

```js
const firstItem = document.querySelector(".application-item");

console.log(firstItem.parentElement); // <ul id="applicationList">
```

### 16.2 子元素

```js
const list = document.getElementById("applicationList");

console.log(list.children);          // HTMLCollection
console.log(list.firstElementChild); // 第一个 li
console.log(list.lastElementChild);  // 最后一个 li
console.log(list.childElementCount); // 2
```

### 16.3 兄弟元素

```js
const firstItem = list.firstElementChild;

console.log(firstItem.nextElementSibling); // 第二个 li
console.log(firstItem.previousElementSibling); // null
```

### 16.4 `closest()`

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

`closest()` 在后续事件委托中非常常用。

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

第 18～20 节的每种操作都是独立实验。每次实验前刷新页面，复制下面的 HTML 和准备代码，再只复制当前小节要测试的代码。`append()` 与 `appendChild()`、`replaceWith()` 与 `replaceChild()`、`remove()` 与 `removeChild()` 是替代写法，不要把替代方案连续执行。

准备下面的 HTML：

```html
<ul id="applicationList">
  <li class="application-item">已有申请</li>
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
referenceItem.after(item);
```

- `before()`：插入到当前元素前面。
- `after()`：插入到当前元素后面。

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

## 19. 移动、替换和删除元素

### 19.1 移动元素

一个元素已经在 DOM 中时，再把它插入其他位置，会移动原元素，而不是自动复制。

```js
const firstItem = list.firstElementChild;
list.append(firstItem);
```

执行后，第一个元素被移动到列表末尾。

### 19.2 `replaceWith()`

```js
const oldItem = list.firstElementChild;
const newItem = document.createElement("li");
newItem.textContent = "替换后的申请";

oldItem.replaceWith(newItem);
```

`replaceWith()` 使用一个或多个节点或字符串替换当前元素。

旧代码中可能看到：

```js
const oldItem = list.firstElementChild;
const newItem = document.createElement("li");
newItem.textContent = "替换后的申请";

list.replaceChild(newItem, oldItem);
```

`replaceChild(newNode, oldNode)` 在父元素上用新节点替换指定的旧子节点，并返回被替换的旧节点。

### 19.3 `remove()`

```js
const item = list.firstElementChild;
item.remove();
```

`remove()` 直接从父元素中删除当前元素。

传统写法：

```js
const item = list.firstElementChild;
list.removeChild(item);
```

`removeChild(child)` 从父元素中删除指定子节点，并返回被删除的节点。

删除后元素对象仍可以保存在变量中，也可以再次插入页面。

## 20. 克隆元素

`cloneNode()` 克隆当前节点。

```js
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
- 使用 `addEventListener()` 添加的事件监听器不会通过 `cloneNode()` 一起复制。
- 表单控件的部分当前状态需要单独确认和设置。

## 21. 使用 DocumentFragment 批量插入

需要一次创建多项内容时，可以先放入 `DocumentFragment`，最后统一插入页面。

`document.createDocumentFragment()` 创建一个不直接显示在页面中的临时节点容器，适合先集中组织一批待插入节点。

HTML：

```html
<ul id="applicationList"></ul>
```

JavaScript：

```js
const applications = [
  { type: "有给休假", date: "2026-09-01" },
  { type: "上午休", date: "2026-09-05" }
];

const list = document.getElementById("applicationList");
const fragment = document.createDocumentFragment();

for (const application of applications) {
  const item = document.createElement("li");
  item.textContent = `${application.date}：${application.type}`;
  fragment.append(item);
}

list.append(fragment);
```

`DocumentFragment` 是临时容器。插入页面时，它的子节点会进入目标元素，Fragment 本身不会成为页面标签。

数据量不大时直接使用 `append()` 也完全可以。需要集中组织一批节点时，再使用 Fragment。

## 22. 完整操作顺序

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

statusText.textContent = "申请中";
statusText.classList.add("is-pending");

const item = document.createElement("li");
item.dataset.applicationId = "REQ-001";
item.textContent = "2026-09-01：有给休假";

applicationList.append(item);
```

## 23. 常见错误与排查

| 症状 | 常见原因 | 检查方法 | 修正方式 |
| --- | --- | --- | --- |
| `Cannot set properties of null` | 没找到元素 | 输出变量，检查 id 和选择器 | 修正名称，确认使用 `defer` |
| 集合的 `textContent` 是 `undefined` | 把集合当成单个元素 | 输出集合和 `length` | 使用下标或循环 |
| class 查询不到 | 把 `.` 写进 `getElementsByClassName()` | 检查参数 | 只传 class 名 |
| id 查询不到 | 把 `#` 写进 `getElementById()` | 检查参数 | 只传 id 值 |
| 设置 `disabled="false"` 仍然禁用 | 布尔属性只看是否存在 | Elements 面板查看属性 | 使用 `element.disabled = false` |
| 修改 `innerHTML` 后内容异常 | 拼接了不可信或格式错误的 HTML | 检查字符串来源 | 使用 `textContent` 和 `createElement()` |
| 克隆后出现重复 id | `cloneNode()` 复制了 id | Elements 面板搜索 id | 插入前删除或修改 id |

## 24. 本章练习

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

## 本章检查点

- 能说明 DOM 树、`document` 和元素节点的关系。
- 能根据查询目标选择 id、class、标签名、name 或 CSS 选择器。
- 能区分单个元素、动态 `HTMLCollection` 和静态 `NodeList`。
- 能读取和修改文本、表单状态、属性、`dataset`、样式和 class。
- 能通过父子、兄弟关系找到相邻元素。
- 能创建、插入、移动、替换、删除和克隆元素。
- 能通过控制台和 Elements 面板排查 DOM 获取与修改问题。
