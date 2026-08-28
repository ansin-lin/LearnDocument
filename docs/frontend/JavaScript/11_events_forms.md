# 第 11 章 事件监听、事件传播与表单操作

## 学习目标

完成本章后，你应能够：

- 说明事件、事件类型、事件处理函数和事件对象的关系。
- 使用 `onclick` 等 `onXXX` DOM 属性绑定事件。
- 使用 `addEventListener()` 添加一个或多个事件监听器。
- 比较 `onXXX` 与 `addEventListener()`，并根据场景选择。
- 说明事件的捕获阶段、目标阶段和冒泡阶段。
- 使用 `target`、`currentTarget` 和 `eventPhase` 判断事件来源及阶段。
- 使用 `preventDefault()` 阻止默认行为，使用 `stopPropagation()` 控制传播。
- 使用事件委托处理批量元素和动态创建元素。
- 处理点击、输入、选择、键盘、焦点和表单提交事件。
- 在不再需要时移除事件监听器。

## 掌握要求

- **必须掌握**：`addEventListener()`、事件对象、冒泡、`target`、`currentTarget`、`preventDefault()`、表单提交。
- **需要掌握**：`onXXX`、捕获阶段、`stopPropagation()`、事件委托和 `removeEventListener()`。
- **会使用、能看懂**：`once`、`passive`、`stopImmediatePropagation()` 和 `eventPhase`。

## 示例运行约定

- 标注为“HTML”的短代码放入练习页面的 `<body>`，JavaScript 放入通过 `defer` 加载的 `js/app.js`。
- 同一小节的 HTML 与 JavaScript 配套使用；需要延续上一小节变量时会明确说明。
- 标明“基本语法”或“错误示例”的代码用于理解格式或观察错误，不作为独立可运行程序。

## 第一部分：绑定事件

### 1. 认识事件

事件表示浏览器中发生了某个动作，例如用户点击按钮、输入文字、切换选项或提交表单。

```text
事件发生
→ 浏览器创建事件对象
→ 浏览器调用已绑定的事件处理函数
→ 处理函数读取事件信息并执行页面逻辑
```

常见事件：

| 事件类型 | 触发时机 | 常见用途 |
| --- | --- | --- |
| `click` | 点击元素 | 按钮操作、菜单切换 |
| `input` | 输入值立即变化 | 实时校验、字数统计 |
| `change` | 值完成变更 | 下拉框、单选框、复选框 |
| `submit` | 表单准备提交 | 校验和收集表单数据 |
| `focus` | 元素获得焦点 | 显示输入提示 |
| `blur` | 元素失去焦点 | 离开输入框时校验 |
| `keydown` | 键盘按键被按下 | 回车操作、快捷键 |
| `DOMContentLoaded` | HTML 已解析完成 | 未使用 `defer` 时初始化页面 |

事件名称本身不带 `on`。使用 `addEventListener()` 时写 `"click"`，使用事件属性时才写 `onclick`。

#### 1.1 事件处理函数

事件发生后被浏览器调用的函数称为事件处理函数或事件监听器。

```js
function handleClick() {
  console.log("按钮被点击");
}
```

仅仅定义函数不会执行，也不会自动监听事件。还需要把函数绑定到某个元素和事件类型。

### 2. 使用 onXXX 绑定事件

`onXXX` 表示元素对象上的事件处理属性，其中 `XXX` 是事件名称，例如：

- `onclick`
- `oninput`
- `onchange`
- `onsubmit`
- `onkeydown`

#### 2.1 DOM 属性写法

HTML：

```html
<button id="submitButton" type="button">提交申请</button>
```

JavaScript：

```js
const submitButton = document.getElementById("submitButton");

submitButton.onclick = function (event) {
  console.log("按钮被点击");
  console.log(event.type); // click
};
```

也可以把具名函数赋给 `onclick`：

```js
function handleSubmitClick(event) {
  console.log(event.type);
}

submitButton.onclick = handleSubmitClick;
```

注意不要写成：

```js
submitButton.onclick = handleSubmitClick();
```

写括号会立即调用函数，并把函数的返回值赋给 `onclick`。

#### 2.2 onXXX 只能保留一个处理函数

```js
submitButton.onclick = function () {
  console.log("第一个处理函数");
};

submitButton.onclick = function () {
  console.log("第二个处理函数");
};
```

点击时只输出：

```text
第二个处理函数
```

第二次赋值覆盖了第一次赋值。

#### 2.3 移除 onXXX 处理函数

把属性设为 `null` 即可移除：

```js
submitButton.onclick = null;
```

#### 2.4 HTML 行内事件属性

旧代码中还可能看到：

```html
<button onclick="handleSubmitClick(event)">提交申请</button>
```

这种写法把 HTML 结构和 JavaScript 行为混在一起，不利于维护，也会增加内容安全策略配置难度。课程项目不使用行内事件属性，但需要能够识别。

### 3. 使用 addEventListener() 绑定事件

`addEventListener()` 是现代项目的推荐方式。

```html
<button id="submitButton" type="button">提交申请</button>
```

```js
const submitButton = document.getElementById("submitButton");

submitButton.addEventListener("click", event => {
  console.log("按钮被点击");
  console.log(event.type); // click
});
```

基本语法：

```text
target.addEventListener(type, listener, options);
```

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `type` | 不带 `on` 的事件名称字符串，如 `"click"` | 必填 | 指定监听的事件类型 |
| `listener` | 函数或具有 `handleEvent()` 的对象 | 必填 | 事件发生时执行 |
| `options` | 布尔值或选项对象 | 可省略，默认在冒泡阶段处理 | 设置捕获、单次执行等行为 |

#### 3.1 可以添加多个处理函数

```js
function writeLog() {
  console.log("记录操作日志");
}

function updateMessage() {
  console.log("更新提示信息");
}

submitButton.addEventListener("click", writeLog);
submitButton.addEventListener("click", updateMessage);
```

点击一次，两个函数都会执行。`addEventListener()` 不会像 `onclick` 赋值那样相互覆盖。

如果使用完全相同的事件类型、函数对象和捕获设置重复注册，浏览器不会重复添加同一个监听器。

#### 3.2 onXXX 与 addEventListener() 对比

| 对比项 | `onXXX` | `addEventListener()` |
| --- | --- | --- |
| 基本写法 | `element.onclick = handler` | `element.addEventListener("click", handler)` |
| 同类型监听器数量 | 一个，后赋值覆盖前一个 | 可以添加多个 |
| 捕获阶段 | 不支持配置 | 支持 |
| `once`、`passive` | 不支持 | 支持 |
| 移除方式 | 赋值为 `null` | `removeEventListener()` |
| 适用场景 | 阅读旧代码、极小型单一处理 | 现代项目主线 |

课程项目统一优先使用 `addEventListener()`，同时要求能看懂和维护 `onXXX`。

## 第二部分：理解事件对象与传播

### 4. 认识事件对象

事件发生时，浏览器会创建事件对象，并把它传给处理函数。

HTML：

```html
<button id="eventObjectButton" type="button">查看事件对象</button>
```

JavaScript：

```js
const eventObjectButton = document.getElementById("eventObjectButton");

eventObjectButton.addEventListener("click", event => {
  console.log(event);
});
```

参数名可以写成 `event`、`e` 或其他名称，推荐使用含义清楚的 `event`。

常用属性和方法：

| 成员 | 含义 |
| --- | --- |
| `type` | 事件类型 |
| `target` | 最初触发事件的元素 |
| `currentTarget` | 当前正在执行监听器的元素 |
| `eventPhase` | 当前传播阶段 |
| `bubbles` | 当前事件是否支持冒泡 |
| `defaultPrevented` | 默认行为是否已被阻止 |
| `preventDefault()` | 阻止默认行为 |
| `stopPropagation()` | 阻止事件继续传播 |
| `stopImmediatePropagation()` | 同时阻止传播和当前元素后续监听器 |

#### 4.1 target 与 currentTarget

HTML：

```html
<button id="submitButton" type="button">
  <span>提交申请</span>
</button>
```

JavaScript：

```js
const submitButton = document.getElementById("submitButton");

submitButton.addEventListener("click", event => {
  console.log(event.target);
  console.log(event.currentTarget);
});
```

如果点击 `<span>` 文字：

- `event.target` 是实际被点击的 `<span>`。
- `event.currentTarget` 是绑定监听器的 `<button>`。

不要在箭头函数中依赖 `this` 指向元素。使用 `event.currentTarget` 含义更明确。

### 5. 理解事件传播

页面元素通常存在嵌套关系：

```html
<div id="outer">
  <section id="inner">
    <button id="actionButton" type="button">执行</button>
  </section>
</div>
```

点击按钮时，事件不是只出现在按钮上。浏览器会沿 DOM 树传播事件，传播过程分为三个阶段：

```text
捕获阶段
window → document → html → body → div → section

目标阶段
button

冒泡阶段
button → section → div → body → html → document → window
```

#### 5.1 捕获阶段

事件从外层向实际目标元素传播。需要在捕获阶段执行监听器时，使用 `addEventListener()` 的第三个参数。

#### 5.2 目标阶段

事件到达最初触发事件的元素，也就是 `event.target`。

#### 5.3 冒泡阶段

事件从目标元素开始向外层祖先元素传播。`addEventListener()` 默认在冒泡阶段执行监听器，大多数可交互事件都会冒泡。

#### 5.4 观察捕获和冒泡顺序

HTML：

```html
<div id="outer">
  外层
  <button id="actionButton" type="button">执行</button>
</div>
```

JavaScript：

```js
const outer = document.getElementById("outer");
const actionButton = document.getElementById("actionButton");

outer.addEventListener("click", () => {
  console.log("外层：捕获");
}, true);

actionButton.addEventListener("click", () => {
  console.log("按钮：目标");
});

outer.addEventListener("click", () => {
  console.log("外层：冒泡");
});
```

点击按钮后的输出顺序：

```text
外层：捕获
按钮：目标
外层：冒泡
```

第三个参数为 `true` 表示在捕获阶段执行；省略或写 `false` 表示在冒泡阶段执行。

#### 5.5 使用 eventPhase 判断传播阶段

`event.eventPhase` 表示当前事件传播阶段。

| 值 | 常量 | 含义 |
| --- | --- | --- |
| `0` | `Event.NONE` | 当前没有传播 |
| `1` | `Event.CAPTURING_PHASE` | 捕获阶段 |
| `2` | `Event.AT_TARGET` | 目标阶段 |
| `3` | `Event.BUBBLING_PHASE` | 冒泡阶段 |

```js
outer.addEventListener("click", event => {
  console.log(event.eventPhase);
}, true);
```

实际业务很少直接判断数字，但调试捕获与冒泡顺序时很有帮助。

### 6. 配置 addEventListener() 的监听方式

第三个参数也可以使用选项对象：

基本语法（用于说明选项结构，不单独运行）：

```text
element.addEventListener("click", handleClick, {
  capture: false,
  once: true,
  passive: false
});
```

| 选项 | 可接受的值 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `capture` | `true`、`false` | `false` | 是否在捕获阶段执行 |
| `once` | `true`、`false` | `false` | 是否执行一次后自动移除 |
| `passive` | `true`、`false` | 通常为 `false`，部分浏览器对特定滚动事件有不同默认处理 | 声明监听器不会调用 `preventDefault()` |
| `signal` | `AbortSignal` | 无 | 通过中止信号统一移除监听器 |

#### 6.1 once

HTML：

```html
<button id="onceButton" type="button">只执行一次</button>
```

JavaScript：

```js
const onceButton = document.getElementById("onceButton");

onceButton.addEventListener("click", () => {
  console.log("只执行一次");
}, { once: true });
```

#### 6.2 passive

```js
function handleTouchMove() {
  console.log("触摸位置发生变化");
}

document.addEventListener("touchmove", handleTouchMove, {
  passive: true
});
```

`passive: true` 表示处理函数不会阻止默认行为。在被动监听器中调用 `preventDefault()` 不会按预期阻止默认行为。普通按钮点击和表单提交不需要主动设置 `passive`。

### 7. 阻止事件传播

#### 7.1 stopPropagation()

`stopPropagation()` 阻止事件继续向后续节点传播。

```html
<div id="panel">
  <button id="openButton" type="button">打开</button>
</div>
```

```js
const panel = document.getElementById("panel");
const openButton = document.getElementById("openButton");

panel.addEventListener("click", () => {
  console.log("panel click");
});

openButton.addEventListener("click", event => {
  event.stopPropagation();
  console.log("button click");
});
```

点击按钮只输出 `button click`，事件不会继续冒泡到 `panel`。

`stopPropagation()` 不会阻止当前元素上其他同类型监听器执行。

#### 7.2 stopImmediatePropagation()

```js
openButton.addEventListener("click", event => {
  event.stopImmediatePropagation();
  console.log("第一个监听器");
});

openButton.addEventListener("click", () => {
  console.log("第二个监听器");
});
```

第二个监听器不会执行，事件也不会继续传播。

不要为了“保险”随意阻止传播。传播是事件委托等功能的基础，只在业务确实需要隔离事件时使用。

### 8. 阻止浏览器默认行为

事件传播和默认行为是两件不同的事情：

- 传播：事件在 DOM 树中的移动过程。
- 默认行为：浏览器收到事件后原本要执行的动作。

常见默认行为：

- 点击链接后跳转。
- 点击提交按钮后提交表单。
- 在复选框上点击后切换选中状态。

`preventDefault()` 用于阻止可取消事件的默认行为。

```html
<a id="helpLink" href="help.html">帮助</a>
```

```js
const helpLink = document.getElementById("helpLink");

helpLink.addEventListener("click", event => {
  event.preventDefault();
  console.log("暂时不跳转");
  console.log(event.cancelable);       // 是否可以阻止默认行为
  console.log(event.defaultPrevented); // 是否已经阻止
});
```

`preventDefault()` 不会阻止冒泡；`stopPropagation()` 也不会自动阻止默认行为。

### 9. 使用事件委托处理列表

事件委托利用冒泡，把多个后代元素的事件统一交给祖先元素处理。

HTML：

```html
<ul id="applicationList">
  <li data-id="REQ-001">
    <button class="cancel-button" type="button">取消</button>
  </li>
  <li data-id="REQ-002">
    <button class="cancel-button" type="button">取消</button>
  </li>
</ul>
```

JavaScript：

```js
const applicationList = document.getElementById("applicationList");

applicationList.addEventListener("click", event => {
  const cancelButton = event.target.closest(".cancel-button");

  if (!cancelButton || !applicationList.contains(cancelButton)) {
    return;
  }

  const item = cancelButton.closest("li");
  console.log(item.dataset.id);
});
```

`closest(selector)` 从当前元素开始向上寻找第一个匹配元素；`contains(node)` 判断指定节点是否位于当前元素内部。这里的 `contains()` 检查找到的取消按钮确实属于当前申请列表。

执行过程：

1. 用户点击后代按钮。
2. `click` 事件冒泡到 `<ul>`。
3. `<ul>` 上的监听器执行。
4. `event.target.closest()` 确认点击位置是否属于取消按钮。
5. 读取对应 `<li>` 的申请编号。

事件委托适合：

- 大量结构相同的按钮或列表项。
- 后续通过 JavaScript 动态添加的元素。
- 希望统一维护一处处理逻辑。

注意，并非所有事件都会冒泡。例如 `focus`、`blur`、`mouseenter` 和 `mouseleave` 不适合直接依赖冒泡委托；可按场景使用会冒泡的 `focusin`、`focusout`、`mouseover` 或 `mouseout`。

## 第三部分：处理页面和表单事件

### 10. 常用页面事件

#### 10.1 输入事件：`input` 与 `change`

HTML：

```html
<label for="reason">申请理由</label>
<textarea id="reason" maxlength="100"></textarea>
<p id="countText">0 / 100</p>
```

##### 10.1.1 input

`input` 在用户输入导致值变化时立即触发，适合实时处理。

```js
const reason = document.getElementById("reason");
const countText = document.getElementById("countText");

reason.addEventListener("input", event => {
  countText.textContent = `${event.currentTarget.value.length} / 100`;
});
```

##### 10.1.2 change

`change` 表示控件值已经完成一次变更。不同控件的触发时机略有差异。

HTML：

```html
<label for="leaveType">休假类型</label>
<select id="leaveType">
  <option value="paid">有给休假</option>
  <option value="half-am">上午休</option>
</select>
```

JavaScript：

```js
const leaveType = document.getElementById("leaveType");

leaveType.addEventListener("change", event => {
  console.log(event.currentTarget.value);
});
```

常见选择：

| 需求 | 事件 |
| --- | --- |
| 输入过程中实时更新 | `input` |
| 下拉框选项变化 | `change` |
| 单选框、复选框状态变化 | `change` |
| 文本输入完成并离开控件 | `change` 或 `blur` |

#### 10.2 焦点事件

HTML：

```html
<label for="focusAccountId">账号</label>
<input id="focusAccountId" type="text">
```

JavaScript：

```js
const accountId = document.getElementById("focusAccountId");

accountId.addEventListener("focus", event => {
  event.currentTarget.classList.add("is-focused");
});

accountId.addEventListener("blur", event => {
  event.currentTarget.classList.remove("is-focused");
});
```

- `focus`：获得焦点。
- `blur`：失去焦点。
- `focusin`：获得焦点并且会冒泡。
- `focusout`：失去焦点并且会冒泡。

不要只依赖焦点样式传达错误，错误信息还应通过文字和适当的可访问性属性表达。

#### 10.3 键盘事件

HTML：

```html
<label for="searchInput">搜索</label>
<input id="searchInput" type="search">
```

JavaScript：

```js
const searchInput = document.getElementById("searchInput");

searchInput.addEventListener("keydown", event => {
  if (event.key === "Escape") {
    event.currentTarget.value = "";
  }
});
```

现代代码优先使用 `event.key`：

| 按键 | `event.key` |
| --- | --- |
| 回车 | `"Enter"` |
| Esc | `"Escape"` |
| Tab | `"Tab"` |
| 空格 | `" "` |
| 向上 | `"ArrowUp"` |
| 向下 | `"ArrowDown"` |

旧代码中的 `keyCode`、`which` 和 `keypress` 已不适合作为新代码主线，但需要知道它们是历史写法。

不要随意阻止 Tab 键等浏览器默认键盘行为，否则会影响键盘用户操作页面。

### 11. 处理表单提交

应该监听表单的 `submit`，而不是只监听提交按钮的 `click`。用户按回车、辅助技术触发表单提交时，不一定经过按钮点击处理。

`new FormData(form)` 会读取表单中具有 `name` 的有效控件并生成表单数据对象，之后可以使用 `get(name)` 读取指定字段。

HTML：

```html
<form id="loginForm">
  <label for="accountId">账号</label>
  <input id="accountId" name="accountId" type="text" required>

  <label for="password">密码</label>
  <input id="password" name="password" type="password" required>

  <button type="submit">登录</button>
</form>
```

JavaScript：

```js
const loginForm = document.getElementById("loginForm");

loginForm.addEventListener("submit", event => {
  event.preventDefault();

  const form = event.currentTarget;
  const formData = new FormData(form);

  const accountId = String(formData.get("accountId") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  console.log(accountId);
  console.log(password.length);
});
```

执行顺序：

1. 表单触发 `submit`。
2. `preventDefault()` 暂时阻止浏览器提交和页面跳转。
3. `event.currentTarget` 取得当前表单。
4. `FormData` 根据具有 `name` 的成功控件收集数据。
5. 使用 `get(name)` 读取字段值。

不要在控制台输出真实密码、令牌或个人隐私。示例只输出密码长度。

前端校验不能替代服务端校验。后续章节会继续讲错误信息和校验状态。

## 第四部分：清理、排错与练习

### 12. 管理事件监听器的生命周期

#### 12.1 使用 `removeEventListener()` 移除监听器

`removeEventListener()` 用于移除通过 `addEventListener()` 添加的监听器。

本小节的 JavaScript 使用下面的按钮：

```html
<button id="removeButton" type="button">测试监听器</button>
```

```js
const submitButton = document.getElementById("removeButton");

function handleClick() {
  console.log("click");
}

submitButton.addEventListener("click", handleClick);
submitButton.removeEventListener("click", handleClick);
```

移除时必须使用同一个函数对象。下面写法无法移除原监听器：

```js
submitButton.addEventListener("click", () => {
  console.log("click");
});

submitButton.removeEventListener("click", () => {
  console.log("click");
});
```

两个箭头函数虽然代码相同，但它们是不同函数对象。

捕获设置也必须一致：

```js
submitButton.addEventListener("click", handleClick, true);
submitButton.removeEventListener("click", handleClick, true);
```

长期存在的页面、组件、弹窗和定时创建的界面在销毁时，应移除不再需要的监听器。

#### 12.2 使用 AbortController 统一清理

多个监听器可以共享一个 `AbortSignal`：

`new AbortController()` 创建一个中止控制器。它的 `signal` 属性可以交给一个或多个事件监听器；调用该控制器的 `abort()` 后，所有使用这个 `signal` 注册的监听器都会被移除。

HTML：

```html
<button id="abortButton" type="button">测试点击</button>
<form id="abortForm">
  <button type="submit">测试提交</button>
</form>
```

JavaScript：

```js
const submitButton = document.getElementById("abortButton");
const loginForm = document.getElementById("abortForm");
const controller = new AbortController();

function handleClick() {
  console.log("click");
}

function handleSubmit(event) {
  event.preventDefault();
  console.log("submit");
}

submitButton.addEventListener("click", handleClick, {
  signal: controller.signal
});

loginForm.addEventListener("submit", handleSubmit, {
  signal: controller.signal
});

controller.abort();
```

当前代码会在注册后立即调用 `abort()`，因此随后点击按钮或提交表单时不会输出日志。实际项目通常在页面功能或组件销毁时调用 `abort()`。这种写法在组件化页面中很有用，初学阶段能看懂即可。

### 13. 常见错误与排查

| 症状 | 原因 | 修正方式 |
| --- | --- | --- |
| 页面打开后函数立即执行 | 绑定时写成 `handler()` | 传入 `handler` |
| 第二个 `onclick` 覆盖第一个 | `onXXX` 只能保存一个函数 | 使用 `addEventListener()` |
| `preventDefault()` 没有阻止冒泡 | 默认行为和传播不是一回事 | 按需再用 `stopPropagation()` |
| `stopPropagation()` 没有阻止链接跳转 | 它只阻止传播 | 使用 `preventDefault()` |
| 事件委托取错元素 | 只使用了 `event.target` | 配合 `closest()` 和 `contains()` |
| `removeEventListener()` 无效 | 传入了不同函数对象或不同捕获设置 | 保存具名函数并保持设置一致 |
| 表单点击按钮有效，按回车无效 | 只监听了按钮 `click` | 监听表单 `submit` |
| 捕获和冒泡顺序看不懂 | 未检查监听器的 `capture` 设置 | 输出元素名称和 `eventPhase` |

### 14. 本章练习

使用下面的初始 HTML：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>任务通知设置</title>
  <script src="js/event-practice.js" defer></script>
</head>
<body>
  <main id="settingsPage">
    <form id="notificationForm">
      <label for="notificationMemo">通知备注</label>
      <textarea id="notificationMemo" name="notificationMemo" maxlength="80"></textarea>
      <p id="memoCount">0 / 80</p>

      <button id="previewButton" type="button">预览</button>
      <button id="saveButton" type="submit">保存设置</button>
    </form>

    <ul id="taskList">
      <li data-task-id="TASK-001">
        确认需求规格
        <button class="complete-button" type="button">完成</button>
      </li>
      <li data-task-id="TASK-002">
        提交测试结果
        <button class="complete-button" type="button">完成</button>
      </li>
    </ul>
  </main>
</body>
</html>
```

在 `js/event-practice.js` 中完成：

1. 先使用 `onclick` 给 `previewButton` 添加预览日志，再重新赋值一次，观察覆盖结果。
2. 改用 `addEventListener()` 给预览按钮添加“记录预览时间”和“显示预览完成”两个处理函数，确认都会执行。
3. 分别在 `settingsPage`、`notificationForm` 和 `saveButton` 上添加捕获、目标、冒泡日志，记录保存按钮被点击后的顺序。
4. 使用 `input` 更新 `memoCount`，实时显示通知备注字数。
5. 监听 `notificationForm` 的 `submit`，阻止默认提交并输出备注内容。
6. 在 `taskList` 上使用事件委托，输出被点击完成按钮所属任务的 `data-task-id`。
7. 在预览按钮处理函数中测试 `stopPropagation()`，观察外层日志是否停止。
8. 使用具名函数添加预览监听器，再通过 `removeEventListener()` 移除并验证。

## 本章检查点

- 能使用并比较 `onXXX` 与 `addEventListener()`。
- 能说明事件处理函数为什么不能在绑定时随意加 `()`。
- 能说明捕获、目标和冒泡三个阶段的方向与顺序。
- 能区分 `target` 和 `currentTarget`。
- 能区分阻止默认行为与阻止传播。
- 能使用事件委托处理列表和动态元素。
- 能监听 `input`、`change`、`keydown` 和 `submit`。
- 能正确移除不再需要的事件监听器。
