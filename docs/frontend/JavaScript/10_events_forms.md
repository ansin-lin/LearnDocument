# 第十章 事件监听、事件传播与表单操作

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

各个实验相互独立，不需要第九章的页面。先创建以下两个文件：

```text
event-demo/
├─ index.html
└─ js/
   └─ app.js
```

`index.html` 使用下面的外壳：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>事件实验</title>
  <script src="js/app.js" defer></script>
</head>
<body>
  <!-- 在这里放入当前实验的 HTML -->
</body>
</html>
```

- 每次实验，替换 `body` 中的内容，并用配套 JavaScript **替换整个** `js/app.js`，保存后刷新页面。
- 同一知识点的替代写法会明确说明保留哪份 HTML；不同版本的脚本不要叠加。
- 输出使用 `console.log()` 时，打开浏览器开发者工具的 Console，先清空旧日志，再按说明点击或输入。
- 标注为“片段”“基本语法”或“错误示例”的代码不作为独立程序。第 14 节练习使用自己的完整页面。

## 第一部分：绑定并处理常见交互

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

点击按钮后显示一条日志。HTML：

```html
<button id="demoButton" type="button">测试点击</button>
```

JavaScript：

```js
const demoButton = document.getElementById("demoButton");

demoButton.onclick = function () {
  console.log("按钮被点击");
};
```

页面加载时只完成绑定；每点击一次按钮，才执行一次函数。

也可以使用具名函数。保留上面的 HTML，把整个脚本替换为：

```js
const demoButton = document.getElementById("demoButton");

function handleClick() {
  console.log("按钮被点击");
}

demoButton.onclick = handleClick;
```

错误写法对照片段（不要追加到正常脚本）：

```js
demoButton.onclick = handleClick();
```

写括号会立即调用函数，并把返回值赋给 `onclick`。这里函数没有返回事件处理函数，因此会出现“刷新时输出日志，点击时没有响应”。把 `handleClick()` 改为 `handleClick` 即可。

#### 2.2 onXXX 只能保留一个处理函数

继续使用 2.1 的按钮 HTML，整个脚本替换为：

```js
const demoButton = document.getElementById("demoButton");

demoButton.onclick = function () {
  console.log("第一个处理函数");
};

demoButton.onclick = function () {
  console.log("第二个处理函数");
};
```

点击一次，只输出“第二个处理函数”，因为第二次赋值覆盖了第一次赋值。

#### 2.3 移除 onXXX 处理函数

把事件属性设为 `null` 即可移除。使用独立的两个按钮观察移除前后的变化：

```html
<button id="demoButton" type="button">测试点击</button>
<button id="stopButton" type="button">停止响应</button>
```

```js
const demoButton = document.getElementById("demoButton");
const stopButton = document.getElementById("stopButton");

demoButton.onclick = function () {
  console.log("按钮被点击");
};

stopButton.onclick = function () {
  demoButton.onclick = null;
  console.log("已移除点击处理函数");
};
```

先点击“测试点击”，再点击“停止响应”，最后再次点击“测试点击”：最后一次不再输出日志。刷新页面可重新开始实验。

#### 2.4 HTML 行内事件属性

以下只是旧代码的识别片段，不作为独立实验；其中的 `handleSubmitClick` 需要由页面脚本另行定义：

```html
<button type="button" onclick="handleSubmitClick(event)">提交申请</button>
```

这种写法把 HTML 结构和 JavaScript 行为混在一起，不利于维护，也会增加内容安全策略配置难度。课程项目不使用行内事件属性，但需要能够识别。

### 3. 使用 addEventListener() 绑定事件

`addEventListener()` 是现代项目的推荐方式。

```html
<button id="submitButton" type="button">提交申请</button>
```

```js
const submitButton = document.getElementById("submitButton");

submitButton.addEventListener("click", () => {
  console.log("按钮被点击");
});
```

基本语法：

```text
target.addEventListener(type, listener, options);
```

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `type` | 不带 `on` 的事件名称字符串，如 `"click"` | 必填 | 指定监听的事件类型 |
| `listener` | 事件处理函数（本章使用这种写法） | 必填 | 事件发生时执行 |
| `options` | 布尔值或选项对象 | 可省略，默认不使用捕获监听 | 设置捕获、单次执行等行为 |

#### 3.1 可以添加多个处理函数

保留第 3 节的按钮 HTML，用下面代码替换整个脚本：

```js
const submitButton = document.getElementById("submitButton");

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
  console.log(event);      // 查看浏览器提供的事件信息
  console.log(event.type); // click
});
```

`event.type` 是事件类型字符串，本例为 `"click"`。处理函数的参数不需要自己创建或传入，由浏览器在事件发生时提供。

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

### 5. 常用页面事件

#### 5.1 输入事件：`input` 与 `change`

HTML：

```html
<label for="reason">申请理由</label>
<textarea id="reason" name="reason" maxlength="100"></textarea>
<p id="countText">0 / 100</p>
```

##### 5.1.1 input

`input` 在用户输入导致值变化时立即触发，适合实时处理。

```js
const reason = document.getElementById("reason");
const countText = document.getElementById("countText");

reason.addEventListener("input", event => {
  countText.textContent = `${event.currentTarget.value.length} / 100`;
});
```

输入“测试”后，页面计数变为 `2 / 100`；删除文字后回到 `0 / 100`。

##### 5.1.2 change

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

#### 5.2 焦点事件

输入框获得焦点时显示提示，离开时清空提示。这个实验不需要 CSS。

```html
<label for="accountInput">账号</label>
<input id="accountInput" name="account" type="text">
<p id="focusHint"></p>
```

```js
const accountInput = document.getElementById("accountInput");
const focusHint = document.getElementById("focusHint");

accountInput.addEventListener("focus", () => {
  focusHint.textContent = "请输入账号";
});

accountInput.addEventListener("blur", () => {
  focusHint.textContent = "";
});
```

点击输入框或用 Tab 键进入，提示出现；按 Tab 离开，提示消失。

- `focus`：元素获得焦点。
- `blur`：元素失去焦点，即使输入值没变也会触发。
- `focusin`、`focusout`：也表示焦点进入、离开，但可以冒泡到祖先元素；传播过程见第 8 节。

只想改变焦点外观时，可以使用 CSS 的 `:focus`；需要执行 JavaScript 逻辑时再监听焦点事件。

#### 5.3 键盘事件

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

### 6. 阻止浏览器默认行为

事件传播和默认行为是两件不同的事情：

- 传播：事件在 DOM 树中的移动过程。
- 默认行为：浏览器收到事件后原本要执行的动作。

常见默认行为：

- 点击链接后跳转。
- 点击提交按钮后提交表单。
- 在复选框上点击后切换选中状态。

`preventDefault()` 用于阻止可取消事件的默认行为。

```html
<a id="helpLink" href="#helpContent">查看帮助</a>
<p id="helpContent">这里是帮助内容。</p>
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

### 7. 处理表单提交

应把提交处理绑定到表单的 `submit` 事件，而不是仅绑定到某个按钮。这样，无论用户点击提交按钮还是在输入框中按回车，都可以进入同一段提交逻辑。

#### 7.1 在提交时读取输入

HTML：

```html
<form id="searchForm">
  <label for="keyword">搜索关键词</label>
  <input id="keyword" name="keyword" type="text">
  <button type="submit">搜索</button>
</form>
<p id="searchResult"></p>
```

JavaScript：

```js
const searchForm = document.getElementById("searchForm");
const keyword = document.getElementById("keyword");
const searchResult = document.getElementById("searchResult");

searchForm.addEventListener("submit", event => {
  event.preventDefault();
  searchResult.textContent = "本次关键词：" + keyword.value;
});
```

执行顺序：

1. 输入“会议”，点击“搜索”，浏览器触发 `submit`。
2. `preventDefault()` 阻止默认提交，页面不刷新。
3. 在回调中读取 `keyword.value`，得到点击提交时的当前值。
4. 页面显示“本次关键词：会议”。改成“培训”并按回车，应显示新的内容。

这里没有实际搜索、保存或发送请求，只观察提交事件与当前输入。也没有设置 `required`；空值如何判断见[第十一章](11_form_validation_errors.md)。

#### 7.2 扩展：使用 FormData 读取表单字段

当需要按字段名称收集表单数据时，可以使用 `FormData`。保留 7.1 的 HTML，用下面代码替换整个脚本：

```js
const searchForm = document.getElementById("searchForm");
const searchResult = document.getElementById("searchResult");

searchForm.addEventListener("submit", event => {
  event.preventDefault();
  const formData = new FormData(searchForm);
  console.log(formData.get("keyword"));
  searchResult.textContent = "字段读取结果请查看控制台";
});
```

- `new FormData(searchForm)`：创建一份当前表单数据。这里的 `new` 表示创建浏览器提供的数据对象；传入的是表单元素。
- `formData.get("keyword")`：按控件的 **name** 读取第一个匹配值，不按 id 查找。本例文本输入得到字符串，空输入得到 `""`；没有这个字段时得到 `null`。
- 没有 `name`、被禁用的控件以及未勾选的复选框不会被收集。文件字段可能得到文件对象，不应一律当字符串处理。

输入文字并提交，观察日志；临时去掉输入框的 `name` 后刷新再提交，结果变成 `null`，恢复 `name` 后再继续。这个实验只比较读取方式，不涉及上传或请求。

## 第二部分：传播与事件委托

现在已经能够处理输入和提交。接下来要解释：为什么点击子元素时父元素的处理也会执行，以及如何把多个按钮的处理集中到父元素上。

### 8. 理解事件传播

下面是结构示意，用于理解传播路径；可运行实验见 8.4：

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

#### 8.1 捕获阶段

事件从外层向实际目标元素传播。需要在捕获阶段执行监听器时，使用 `addEventListener()` 的第三个参数。

#### 8.2 目标阶段

事件到达最初触发事件的元素，也就是 `event.target`。

#### 8.3 冒泡阶段

事件从目标元素开始向外层祖先元素传播。省略捕获配置时，祖先元素上的监听器在冒泡阶段执行；目标元素上的监听器仍属于目标阶段。`click` 等常见交互事件会冒泡，但不是所有事件都冒泡。

#### 8.4 观察捕获和冒泡顺序

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

第三个参数为 `true` 表示使用捕获监听；省略或写 `false` 表示使用非捕获监听。在目标元素自身，两种监听都属于目标阶段。

#### 8.5 使用 eventPhase 判断传播阶段

`event.eventPhase` 表示当前事件传播阶段。

| 值 | 常量 | 含义 |
| --- | --- | --- |
| `0` | `Event.NONE` | 当前没有传播 |
| `1` | `Event.CAPTURING_PHASE` | 捕获阶段 |
| `2` | `Event.AT_TARGET` | 目标阶段 |
| `3` | `Event.BUBBLING_PHASE` | 冒泡阶段 |

使用 8.4 的 HTML，整个脚本替换为下面版本，对照数字与阶段：

```js
const outer = document.getElementById("outer");
const actionButton = document.getElementById("actionButton");

outer.addEventListener("click", event => {
  console.log("外层", event.eventPhase); // 1
}, true);
actionButton.addEventListener("click", event => {
  console.log("按钮", event.eventPhase); // 2
});
outer.addEventListener("click", event => {
  console.log("外层", event.eventPhase); // 3
});
```

点击按钮后，依次看到 `1`、`2`、`3`；点击外层文字时，外层本身就是目标，不应期待同样的输出。

实际业务很少直接判断数字，但调试捕获与冒泡顺序时很有帮助。

### 9. 阻止事件传播

#### 9.1 stopPropagation()

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

#### 9.2 stopImmediatePropagation()

它还会阻止当前元素上尚未执行的后续监听器。保留 9.1 的 HTML，用以下代码替换整个脚本：

```js
const panel = document.getElementById("panel");
const openButton = document.getElementById("openButton");

panel.addEventListener("click", () => {
  console.log("外层监听器");
});

openButton.addEventListener("click", event => {
  event.stopImmediatePropagation();
  console.log("第一个监听器");
});

openButton.addEventListener("click", () => {
  console.log("第二个监听器");
});
```

点击按钮，只输出“第一个监听器”，第二个监听器和外层监听器都不执行。把 `stopImmediatePropagation()` 改为 `stopPropagation()` 后再刷新测试：两个按钮监听器都会执行，但外层仍不执行。

不要为了“保险”随意阻止传播。传播是事件委托等功能的基础，只在业务确实需要隔离事件时使用。

### 10. 使用事件委托处理列表

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

## 第三部分：监听选项与生命周期

### 11. 配置 addEventListener() 的监听方式

第三个参数也可以使用选项对象，把多个设置一起传给监听器。

例如，`{ once: true }` 是一个对象：花括号包住整组设置，`once` 是属性名（设置项名称），冒号右边的 `true` 是属性值（设置内容），表示只执行一次。多个属性之间用逗号分隔，例如 `{ capture: false, once: true }`。

这组花括号出现在函数的参数位置，表示配置数据，不是回调函数的代码块。选项名称由 `addEventListener()` 规定，不能随意改名；值的含义见下面的表格。

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

#### 11.1 once

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

#### 11.2 passive

这是触屏设备上的独立实验。使用页面外壳，并在 `body` 放入一段文字；整个脚本如下。没有触屏时可先阅读，不用鼠标点击来验证 `touchmove`。

```js
function handleTouchMove() {
  console.log("触摸位置发生变化");
}

document.addEventListener("touchmove", handleTouchMove, {
  passive: true
});
```

在页面上用手指触摸并移动，控制台会输出日志；单击鼠标不会触发这个事件。

`passive: true` 表示处理函数不会阻止默认行为。在被动监听器中调用 `preventDefault()` 不会按预期阻止默认行为。普通按钮点击和表单提交不需要主动设置 `passive`。

### 12. 管理事件监听器的生命周期

#### 12.1 使用 `removeEventListener()` 移除监听器

`removeEventListener(type, listener, capture)` 移除之前注册的监听器。事件名称、函数和捕获设置必须与注册时一致；没有匹配监听器时不做任何事。

HTML：

```html
<button id="demoButton" type="button">测试点击</button>
<button id="stopButton" type="button">移除监听器</button>
```

JavaScript：

```js
const demoButton = document.getElementById("demoButton");
const stopButton = document.getElementById("stopButton");

function handleClick() {
  console.log("测试按钮有响应");
}

demoButton.addEventListener("click", handleClick);

stopButton.addEventListener("click", () => {
  demoButton.removeEventListener("click", handleClick);
  console.log("监听器已移除");
});
```

先点击“测试点击”，应有日志；点击“移除监听器”后再次测试，不再输出。刷新页面后重新绑定。按钮本身没有被删除，只是不再执行这个处理函数。

错误对照实验：保留 HTML，把整个脚本替换为：

```js
const demoButton = document.getElementById("demoButton");
const stopButton = document.getElementById("stopButton");

demoButton.addEventListener("click", () => {
  console.log("测试按钮有响应");
});

stopButton.addEventListener("click", () => {
  demoButton.removeEventListener("click", () => {
    console.log("测试按钮有响应");
  });
});
```

点击移除后，测试按钮仍有响应。两个箭头函数虽然内容一样，但不是同一个函数；修正时使用前一个完整示例中的具名函数。

捕获设置也必须一致。以下只是注册与移除的对照片段，不追加到前面的实验：

```js
demoButton.addEventListener("click", handleClick, true);
demoButton.removeEventListener("click", handleClick, true);
```

长期存在的页面、组件或弹窗在清理功能时，应移除不再需要的监听器。

#### 12.2 使用 AbortController 统一清理（会使用、能看懂）

多个监听器需要一起移除时，可以共享同一个中止信号。

`new AbortController()` 创建浏览器提供的中止控制器；`controller.signal` 是它发出的信号对象。给多个监听器传入这个信号后，调用 `controller.abort()` 可以同时移除它们。`abort()` 本例不传参数，只用来通知清理。

HTML：

```html
<button id="firstButton" type="button">测试第一个</button>
<button id="secondButton" type="button">测试第二个</button>
<button id="stopButton" type="button">停止两个监听器</button>
```

JavaScript：

```js
const firstButton = document.getElementById("firstButton");
const secondButton = document.getElementById("secondButton");
const stopButton = document.getElementById("stopButton");
const controller = new AbortController();

firstButton.addEventListener("click", () => {
  console.log("第一个按钮有响应");
}, { signal: controller.signal });

secondButton.addEventListener("click", () => {
  console.log("第二个按钮有响应");
}, { signal: controller.signal });

stopButton.addEventListener("click", () => {
  controller.abort();
  console.log("两个测试监听器已移除");
});
```

先分别点击两个测试按钮，都会输出日志；点击“停止两个监听器”后，再点击它们，都不再输出。

`{ signal: controller.signal }` 是监听选项，表示这一监听器受哪个信号控制。“停止”按钮没有使用该信号，因此仍可点击。中止后旧信号不会恢复；刷新页面会重新创建控制器，才能重新实验。

## 第四部分：排错与练习

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

## 参考资料

- [MDN：addEventListener() 的参数与传播阶段](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener)
- [MDN：FormData() 与表单字段收集](https://developer.mozilla.org/en-US/docs/Web/API/FormData/FormData)
