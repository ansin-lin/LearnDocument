# 第 17 章 错误处理与浏览器调试

代码出现问题时，最重要的能力不是猜测，而是读取证据、定位原因、完成最小修正并重新验证。本章建立一套可以重复使用的错误处理和调试方法，为后续 Promise、接口请求、Vue 和 React 的排错打基础。

完成本章后，你应当能够：

- 区分错误信息、程序异常、业务校验失败和页面故障现象。
- 阅读错误类型、消息、文件、行号和调用栈。
- 使用 `Error`、`throw` 和 `try...catch...finally` 处理异常。
- 使用合适的 Console 方法观察运行状态。
- 使用 Sources 面板的断点、单步执行、Scope、Watch 和 Call Stack。
- 排查 DOM、JSON、本地存储和多页面共用脚本的常见问题。
- 按“复现—定位—验证—修正—回归”的顺序完成排错。

## 1. 先区分错误、异常和业务校验

### 1.1 故障现象是用户看到的结果

常见故障现象包括：

- 点击按钮没有反应。
- 页面列表没有数据。
- 表单提交后页面突然刷新。
- 页面一直显示“读取中”。
- 控制台出现红色错误。

现象只是排查起点，不能直接说明根本原因。例如“按钮没有反应”可能由脚本路径错误、选择器错误、事件没有绑定或处理函数提前报错造成。

### 1.2 异常会打断当前执行流程

```js
console.log("处理开始");

const application = null;
console.log(application.id);

console.log("处理结束");
```

读取 `null.id` 会产生 `TypeError`。当前执行流程在错误位置中断，所以“处理结束”不会输出。

### 1.3 业务校验失败不一定是异常

用户没有填写必填项属于可以预期的输入情况，通常使用条件判断并显示提示：

```js
function validateReason(reason) {
  if (reason.trim() === "") {
    return "申请理由为必填项";
  }

  return "";
}
```

`trim()` 返回删除首尾空白后的新字符串。`validateReason(reason)` 在校验失败时返回提示文字，通过时返回空字符串。

不要把所有普通业务分支都写成异常。异常更适合表示当前操作无法按照正常流程继续的情况，例如存储内容损坏、必要数据结构错误或请求失败。

## 2. 阅读浏览器错误信息

### 2.1 一条错误通常包含什么

浏览器控制台可能显示：

```text
Uncaught TypeError: Cannot read properties of null (reading 'id')
    at showApplication (app.js:24:27)
    at HTMLButtonElement.<anonymous> (app.js:41:5)
```

应按顺序读取：

| 部分 | 示例 | 含义 |
| --- | --- | --- |
| 是否处理 | `Uncaught` | 错误没有被当前代码捕获处理 |
| 错误类型 | `TypeError` | 对值执行了不适合其类型的操作 |
| 错误消息 | `Cannot read...` | 具体失败内容 |
| 文件 | `app.js` | 错误所在脚本 |
| 行和列 | `24:27` | 第 24 行、第 27 列附近 |
| 调用栈 | 后续 `at ...` | 哪些函数依次调用到错误位置 |

先点击控制台中的文件和行号，到 Sources 面板查看真实代码。不要只搜索错误消息后直接复制不理解的修正。

### 2.2 优先处理第一条有效错误

前面的错误可能导致后续代码连续失败。控制台出现多条红色错误时，通常先处理最早发生、最接近自己代码的第一条错误，再刷新页面重新观察。

## 3. JavaScript 常见错误类型

### 3.1 `SyntaxError`：语法无法解析

下面是故意写错的代码，不能直接运行：

```text
const name = "田中;
```

字符串缺少结束引号，JavaScript 在运行前就无法正确解析代码。常见原因包括括号、引号、大括号或逗号不成对。

### 3.2 `ReferenceError`：名称不存在

```js
const userName = "田中";
console.log(username);
```

声明的是 `userName`，使用的却是 `username`。JavaScript 区分大小写，因此会产生 `ReferenceError`。

检查变量拼写、声明位置和作用域。

### 3.3 `TypeError`：值不支持当前操作

```js
const submitButton = document.querySelector("#notExists");
submitButton.addEventListener("click", () => {});
```

`querySelector(selector)` 接收 CSS 选择器，返回第一个匹配元素；找不到时返回 `null`。对 `null` 调用 `addEventListener()` 会产生 `TypeError`。

排查时先观察：

```js
console.log(submitButton);
```

然后确认 HTML 中是否存在目标元素、`id` 是否一致、脚本是否在 DOM 准备好之后运行。

### 3.4 `RangeError`：值超出允许范围

```js
function repeatForever() {
  repeatForever();
}

repeatForever();
```

递归没有结束条件，调用栈不断增长，最终通常产生 `RangeError: Maximum call stack size exceeded`。它表示某个值或操作范围超出运行环境能够处理的边界。

### 3.5 普通 `Error`：主动表示失败

```js
const error = new Error("申请数据不存在");
console.log(error);
```

`new Error(message)` 创建错误对象。`message` 是可选的错误说明字符串，返回新的 `Error` 对象。业务代码可以主动创建它，交给 `throw` 或 Promise 的失败处理。

新人主线重点掌握 `SyntaxError`、`ReferenceError`、`TypeError`、`RangeError` 和普通 `Error`，不需要一次记住所有错误类型。

## 4. `Error` 对象保存哪些信息

### 4.1 常用属性

```js
const error = new Error("申请数据不存在");

console.log(error.name); // Error
console.log(error.message); // 申请数据不存在
console.log(error.stack); // 调用栈信息，不同浏览器格式可能不同
```

| 属性 | 作用 |
| --- | --- |
| `name` | 错误类型名称 |
| `message` | 创建错误时提供的说明 |
| `stack` | 错误发生位置和函数调用路径；格式由运行环境决定 |

给用户显示的提示和给开发者排查的错误信息应分开。下面是页面错误处理函数中的代码片段，假设已经查询到 `statusMessage` 元素：

```js
console.error(error);
statusMessage.textContent = "申请数据读取失败，请稍后再试";
```

控制台可以保留技术信息，页面提示应便于用户理解。不要在日志或页面中输出密码、令牌和隐私数据。

## 5. 使用 `throw` 主动中断错误流程

### 5.1 基本写法

```js
function getApplication(application) {
  if (application === null) {
    throw new Error("申请数据不存在");
  }

  return application;
}
```

`throw value` 抛出一个值，并立即停止当前函数后续代码。虽然 JavaScript 允许抛出任意值，但项目代码应优先抛出 `Error` 对象，这样能保留名称、消息和调用栈。

### 5.2 `return` 和 `throw` 的区别

| 写法 | 含义 | 调用方如何处理 |
| --- | --- | --- |
| `return value` | 正常完成并返回结果 | 接收返回值 |
| `return null` | 按函数约定表示“没有结果” | 判断 `null` |
| `throw new Error(...)` | 当前操作无法正常继续 | 使用 `try...catch` 或上层错误机制 |

使用哪种方式要由函数约定决定。同一函数不要有时返回 `null`、有时随意抛字符串，让调用方无法判断。

## 6. 使用 `try...catch...finally`

### 6.1 捕获可能发生的异常

```js
function parseApplications(jsonText) {
  try {
    return JSON.parse(jsonText);
  } catch (error) {
    console.error("JSON 解析失败", error);
    return null;
  }
}

console.log(parseApplications('[{"id":"REQ-001"}]'));
console.log(parseApplications("not-json"));
```

执行顺序：

1. 先执行 `try` 中的代码。
2. 没有异常时跳过 `catch`。
3. 出现异常时，`try` 中剩余代码停止，控制流程进入 `catch`。
4. `catch (error)` 中的变量接收被抛出的错误。

`JSON.parse(text)` 接收 JSON 字符串，成功时返回对应 JavaScript 值，文本不合法时抛出 `SyntaxError`。

### 6.2 `finally` 负责收尾

```js
function saveApplication(jsonText) {
  console.log("开始保存");

  try {
    const application = JSON.parse(jsonText);
    console.log(application);
  } catch (error) {
    console.error("保存失败", error);
  } finally {
    console.log("结束保存处理");
  }
}
```

`finally` 中的代码无论成功还是失败都会执行，适合恢复按钮、关闭加载状态或释放资源。不要在 `finally` 中写会掩盖原结果的 `return`。

### 6.3 不要写空的 `catch`

```js
try {
  JSON.parse("not-json");
} catch (error) {
  // 什么都不做
}
```

空 `catch` 会让故障表面上消失，却没有留下原因或用户提示。至少应记录必要的错误信息，并按照业务要求返回安全结果、显示错误或继续向上抛出。

### 6.4 只包住真正可能失败的范围

```js
const savedText = '[{"id":"REQ-001"}]';
let applications;

try {
  applications = JSON.parse(savedText);
} catch (error) {
  console.error("申请数据无法解析", error);
}
```

过大的 `try` 会让学员难以判断具体哪一步失败。应先缩小可能抛出异常的代码范围，再处理错误。

## 7. 正确处理本地存储和 JSON

### 7.1 key 不存在不等于 JSON 损坏

```js
const savedText = localStorage.getItem("paidLeaveApplications");

if (savedText === null) {
  console.log("还没有保存申请数据");
}
```

`getItem(key)` 在 key 不存在时返回 `null`。这是正常的“无数据”状态，不是异常。

需要特别注意：

```js
console.log(JSON.parse(null)); // null
```

`JSON.parse(null)` 会先把参数转换为字符串 `"null"`，结果是 JavaScript 的 `null`，不会因此直接抛出异常。但后续如果把它当数组使用，仍会发生 `TypeError`。

### 7.2 JSON 合法不代表数据结构正确

```js
function loadApplications() {
  const savedText = localStorage.getItem("paidLeaveApplications");

  if (savedText === null) {
    return [];
  }

  const parsedData = JSON.parse(savedText);

  if (!Array.isArray(parsedData)) {
    throw new Error("申请数据必须是数组");
  }

  return parsedData;
}
```

`Array.isArray(value)` 判断传入值是否为数组，返回布尔值。数据检查至少分为三层：

1. 存储中是否有值。
2. 文本是不是合法 JSON。
3. 解析结果是不是业务期望的结构。

### 7.3 读取失败时不要静默覆盖原数据

```js
function safeLoadApplications() {
  try {
    return loadApplications();
  } catch (error) {
    console.error("申请数据读取失败", error);
    return null;
  }
}
```

返回 `null` 表示读取失败，页面应显示错误并停止写回。不能发现数据损坏后立即用空数组覆盖，否则会破坏原始数据，使问题难以恢复和调查。

## 8. 使用 Console 获取证据

### 8.1 常用 Console 方法

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `console.log(...values)` | 一个或多个值 | 任意值 | 可变参数 | 输出一般调试信息 |
| `console.warn(...values)` | 一个或多个值 | 任意值 | 可变参数 | 输出警告信息 |
| `console.error(...values)` | 一个或多个值 | 任意值 | 可变参数 | 输出错误信息 |
| `console.table(data)` | 数据 | 数组或对象 | 必填 | 用表格观察同结构数据 |
| `console.group(label)` | 分组标题 | 字符串等值 | 可选 | 开始一组日志 |
| `console.groupEnd()` | 无 | 无 | 无参数 | 结束当前日志分组 |

### 8.2 日志要验证明确的问题

```js
const application = {
  id: "REQ-20260820-001",
  status: "pending",
};
const applications = [application];

console.group("申请提交调查");
console.log("事件已触发");
console.log("申请对象", application);
console.table(applications);
console.groupEnd();
```

不要无目的地到处打印。每条日志应回答一个问题，例如：

- 脚本是否加载？
- 事件是否触发？
- DOM 元素是否找到？
- 输入值和类型是否正确？
- 条件判断走了哪个分支？
- 数组是在修改前还是修改后出现异常？

排查完成后删除临时日志，避免把账号、密码摘要、令牌或内部数据留在控制台。

## 9. 使用 Sources 面板断点调试

### 9.1 设置行断点

1. 打开浏览器开发者工具。
2. 进入 Sources 面板。
3. 找到当前 JavaScript 文件。
4. 点击目标代码左侧的行号。
5. 操作页面，让代码运行到该位置。
6. 页面暂停后观察变量和调用栈。

### 9.2 常用调试操作

| 操作 | 作用 | 适用情况 |
| --- | --- | --- |
| Resume | 继续运行到下一个断点或结束 | 当前变量已经确认 |
| Step over | 执行当前行，不进入被调用函数内部 | 只关心当前函数流程 |
| Step into | 进入当前行调用的函数 | 怀疑被调用函数内部有问题 |
| Step out | 运行到当前函数结束并返回上层 | 已确认当前函数内部没有问题 |
| Scope | 查看当前作用域中的变量 | 确认变量值和类型 |
| Watch | 持续观察指定表达式 | 关注某个值如何变化 |
| Call Stack | 查看函数调用顺序 | 调查错误从哪个调用入口发生 |

### 9.3 条件断点

循环执行很多次时，可以右键行号添加条件断点，例如只在目标编号出现时暂停：

```js
application.id === "REQ-20260820-003"
```

条件表达式返回 `true` 时才暂停，可以减少大量重复单步操作。

### 9.4 `debugger` 语句

```js
function submitApplication(application) {
  debugger;
  console.log(application);
}
```

开发者工具打开时，执行到 `debugger` 通常会暂停。它不接收参数，也没有业务返回结果。提交代码前应删除临时 `debugger`，避免影响其他开发者和用户。

## 10. DOM 和多页面脚本常见故障

### 10.1 脚本路径错误

```html
<script src="app.js" defer></script>
```

如果真实文件位于 `js/app.js`，浏览器会请求错误地址。到 Network 面板检查脚本请求是否为 404，并改成：

```html
<script src="js/app.js" defer></script>
```

### 10.2 选择器和 HTML 不一致

```html
<button id="submitButton" type="button">提交</button>
```

```js
const submitButton = document.querySelector("#submitButton");
console.log(submitButton);
```

如果结果是 `null`，检查大小写、拼写、页面是否真的存在该元素，以及脚本加载时机。

### 10.3 多页面共用脚本

七个页面共用 `app.js` 时，某个页面不存在申请表单是正常情况：

```js
const applyForm = document.querySelector("#applyForm");

if (applyForm !== null) {
  applyForm.addEventListener("submit", (event) => {
    event.preventDefault();
  });
}
```

`addEventListener(type, listener)` 注册事件监听器。`preventDefault()` 取消表单提交的默认刷新。这里先判断元素存在，再绑定事件，避免在不相关页面调用 `null.addEventListener()`。

## 11. 标准排错流程

### 11.1 从现象到修正

```text
复现问题
→ 记录页面、操作步骤、输入和实际结果
→ 阅读第一条有效错误
→ 根据文件、行号和调用栈定位
→ 检查当前输入、变量值和数据类型
→ 提出一个具体原因假设
→ 用断点或日志验证假设
→ 进行最小范围修正
→ 重新验证原场景和关联场景
→ 删除临时日志并记录结果
```

### 11.2 一次只验证一个假设

不建议同时修改选择器、数据结构和事件代码。一次改动太多，即使功能恢复，也很难知道真正原因，还可能引入新问题。

### 11.3 修正后需要回归验证

例如修正“申请取消失败”后，至少重新确认：

- 可以取消 `pending` 申请。
- `approved` 申请仍然不能取消。
- 取消后记录保留，只改变状态。
- 其他用户的申请没有受到影响。
- 刷新页面后结果仍然存在。

## 12. 常见错误处理误区

### 12.1 只给用户看“发生错误”

用户提示要说明当前操作结果和可采取的动作，例如“申请列表读取失败，请稍后重试”。开发者日志则保留具体错误对象。

### 12.2 捕获后继续使用无效数据

解析失败后不能继续把 `null` 当数组渲染。应停止当前流程，显示错误或返回到安全页面。

### 12.3 用默认值静默掩盖损坏

“没有保存过数据”可以使用空数组；“已保存内容损坏”应显示错误并保留调查证据，二者不能都静默变成空数组。

### 12.4 只修正控制台错误，不验证业务结果

控制台没有红色错误不代表功能正确。还要检查页面显示、存储状态、数据所属用户和刷新后的结果。

## 13. 本章综合排错任务

### 13.1 初始文件

准备以下独立练习：

```text
debug-practice/
├─ index.html
└─ js/
   └─ app.js
```

页面包含申请表单、提交按钮、申请列表和错误提示区域，并通过外部脚本加载 JavaScript。

### 13.2 已知故障现象

练习初始代码包含以下问题：

1. HTML 中的脚本路径错误，Network 面板出现 404。
2. JavaScript 的按钮选择器与 HTML `id` 不一致。
3. 事件处理函数使用了拼写错误的变量名。
4. 表单提交后页面刷新，输入结果消失。
5. 本地存储 key 不存在时，代码把 `null` 当数组使用。
6. 存储内容不是合法 JSON 时，没有错误提示。
7. JSON 可以解析，但结果是对象而不是申请数组。

### 13.3 任务要求

1. 按标准排错流程逐个复现问题，不直接重写全部代码。
2. 为每个问题记录现象、错误类型、文件和行号。
3. 至少使用一次行断点、Step over、Scope 和 Call Stack。
4. 使用 `try...catch` 处理 JSON 解析错误。
5. 使用 `Array.isArray()` 验证申请数据结构。
6. 区分“首次使用没有数据”和“已有数据损坏”。
7. 修正后重新验证提交、刷新、列表显示和损坏数据场景。

### 13.4 提交证据

| 项目 | 需要记录的内容 |
| --- | --- |
| 复现步骤 | 从哪个页面执行了什么操作 |
| 实际结果 | 页面和控制台发生了什么 |
| 原因 | 哪个值或哪段代码不符合预期 |
| 调试方法 | 使用了日志、断点或哪个面板 |
| 修正内容 | 最小修改位置和理由 |
| 验证结果 | 原场景和关联场景是否通过 |

### 13.5 完成标准

- 七个问题均能稳定复现、说明原因并完成修正。
- 能区分 `SyntaxError`、`ReferenceError`、`TypeError` 和主动抛出的 `Error`。
- JSON 损坏时保留原数据，不静默覆盖。
- 页面显示适合用户理解的错误提示，控制台保留开发者需要的信息。
- 修正后的正常流程可以提交、显示并在刷新后恢复申请数据。
- 控制台没有未处理错误，也没有敏感数据日志。
