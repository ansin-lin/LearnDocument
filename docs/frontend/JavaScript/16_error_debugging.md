# 第 16 章 错误处理与浏览器调试

代码出现问题时，最重要的能力不是猜测，而是读取证据、定位原因、完成最小修正并重新验证。本章建立一套可以重复使用的错误处理和调试方法，为后续 Promise、接口请求、Vue 和 React 的排错打基础。

完成本章后，你应当能够：

- 区分错误信息、程序异常、业务校验失败和页面故障现象。
- 阅读错误类型、消息、文件、行号和调用栈。
- 使用 `Error`、`throw` 和 `try...catch...finally` 处理异常。
- 使用合适的 Console 方法观察运行状态。
- 使用 Sources 面板的断点、单步执行、Scope、Watch 和 Call Stack。
- 排查 DOM、数据类型、函数调用和页面脚本的常见问题。
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

不要把所有普通业务分支都写成异常。异常更适合表示当前操作无法按照正常流程继续的情况，例如函数收到了不符合约定的数据结构，导致当前操作无法继续。

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

### 2.3 完成一次定位、修复和复测

在独立练习页面加载的 `js/app.js` 中运行下面的故障代码，勿与其他实验合并：

```js
const requestedDays = 2;
const remainingDays = 5;
console.log(requestedDay <= remainingDays);
```

1. 刷新页面，在 Console 找到第一条错误：`requestedDay is not defined`。
2. 点击错误的文件与行号，查看第三行。不要急着修改比较运算符，因为错误指出的是变量名。
3. 对比第一行的声明：名称是 `requestedDays`，第三行少了末尾的 `s`。
4. 改为 `console.log(requestedDays <= remainingDays);`，保存并刷新，应输出 `true`。
5. 把申请天数改为 6，再刷新，应输出 `false`。这一步验证的是判断功能，而不仅是错误是否消失。

由此形成一个排查习惯：先读错误指向的证据，再修改最小范围，最后用成功和失败两种输入复测。

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

下面各小节为独立控制台实验，每次用当前示例替换整个实验脚本。示例不读取浏览器存储，也不发送请求。

### 6.1 捕获可能发生的异常

调用方要求取得申请状态，但提供的数据可能为空。函数遇到无法处理的数据时主动抛错，调用方负责捕获：

```js
function readStatus(application) {
  if (application === null || application === undefined) {
    throw new Error("申请数据不存在");
  }
  return application.status;
}

function showStatus(application) {
  try {
    console.log(readStatus(application));
    console.log("状态读取完成");
  } catch (error) {
    console.error("状态读取失败：", error.message);
  }
}

showStatus({ status: "pending" });
showStatus(null);
console.log("本次实验结束");
```

第一次调用输出 `pending` 和“状态读取完成”。第二次调用在 `readStatus()` 中抛错，控制流程转到调用方的 `catch`，不再输出“状态读取完成”；错误处理后仍会输出“本次实验结束”。

1. `try` 包住可能失败的操作，包括它调用的函数。
2. 没有异常时跳过 `catch`。
3. 出现异常时，跳过 `try` 中尚未执行的代码。
4. `catch (error)` 的参数接收异常对象。
5. `console.error()` 输出错误日志，不会自动修正数据。

本例约定参数是申请对象、`null` 或 `undefined`，不负责识别所有任意输入。对普通用户漏填字段，仍优先用第十一章的校验提示处理，不必一律抛异常。

### 6.2 `finally` 负责收尾

独立实验：

```js
function inspectApplication(application) {
  console.log("开始检查");
  try {
    if (application === null) {
      throw new Error("没有可检查的申请");
    }
    console.log(application.id);
  } catch (error) {
    console.error("检查失败：", error.message);
  } finally {
    console.log("结束检查");
  }
}

inspectApplication({ id: "REQ-001" });
inspectApplication(null);
```

两次调用都会输出“结束检查”。`finally` 用于成功、失败都需要执行的收尾，例如恢复操作按钮。它不是只在失败时执行，也不会让发生异常后被跳过的 `try` 代码重新执行。不要在 `finally` 中写会掩盖原结果的 `return`。

### 6.3 不要写空的 `catch`

下面是错误处理方式不恰当的独立对照实验：

```js
try {
  throw new Error("必要数据缺失");
} catch (error) {
  // 什么都不做
}
console.log("后续处理");
```

只看到“后续处理”，却看不到失败原因。问题不是已经修复，而是被隐藏了。将空注释替换为 `console.error(error.message)` 再运行，观察差异；实际处理还应决定是否终止当前操作或显示提示。

### 6.4 只包住真正可能失败的范围

独立实验：

```js
function requireArray(value) {
  if (!Array.isArray(value)) {
    throw new Error("申请列表必须是数组");
  }
  return value;
}

const input = { id: "REQ-001" };
let applications = null;

try {
  applications = requireArray(input);
} catch (error) {
  console.error(error.message);
}

if (applications !== null) {
  console.log("记录数：", applications.length);
}
```

`Array.isArray(value)` 判断参数是否为数组，返回布尔值。本例只把可能抛错的数据检查放在 `try` 中；失败后保留 `null`，不继续按正常数组处理。

把 `input` 改为 `[{ id: "REQ-001" }]`，应输出记录数 1；改为空数组，应输出 0。空数组是正常数据，不应仅因为长度为 0 就抛错。

## 7. 区分无数据与数据错误

### 7.1 空数组不等于错误类型

```js
function countApplications(value) {
  if (!Array.isArray(value)) {
    throw new Error("申请列表必须是数组");
  }
  return value.length;
}

console.log(countApplications([])); // 0

try {
  console.log(countApplications(null));
} catch (error) {
  console.error(error.message);
}
```

`[]` 明确表示列表中没有记录；`null` 不是数组，不能直接读取它的 `length`。是否接受 `null` 应由函数约定决定，本例不接受。

### 7.2 捕获后不要伪装成功

如果函数约定返回 `null` 表示失败，调用方就应检查它，停止当前处理，而不是无条件继续显示“处理成功”。如果返回空数组表示正常无记录，就不要把错误也悄悄改成空数组。

这种区别同样适用于后续的数据读取：无数据是正常状态，数据不符合约定则需要保留错误原因。当前先用变量和数组验证，不要求数据持久保存。

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
- 在同一次页面运行中，列表显示与内存数组一致；刷新后重新加载本章初始数据。

## 12. 常见错误处理误区

### 12.1 只给用户看“发生错误”

用户提示要说明当前操作结果和可采取的动作，例如“申请列表读取失败，请稍后重试”。开发者日志则保留具体错误对象。

### 12.2 捕获后继续使用无效数据

数据检查失败后不能继续把 `null` 当数组渲染。应停止当前流程，显示错误或返回到安全页面。

### 12.3 用默认值静默掩盖损坏

空数组表示正常无记录；函数收到错误类型时应报告原因。不能把所有错误输入都静默改成空数组，让调用者误以为处理正常完成。

### 12.4 只修正控制台错误，不验证业务结果

控制台没有红色错误不代表功能正确。还要检查页面显示、内存数组、数据所属用户以及重复操作后的结果。

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

以本章各节的小示例为基础，分别制作并复现以下独立故障，不要求拼接成一份大型脚本：

1. HTML 中的脚本路径错误，Network 面板出现 404。
2. JavaScript 的按钮选择器与 HTML `id` 不一致。
3. 事件处理函数使用了拼写错误的变量名。
4. 表单提交后页面刷新，输入结果消失。
5. 把 `null` 当成数组读取 `length`。
6. 函数主动抛错后，调用方没有处理错误。
7. 函数收到普通对象而不是数组，却继续按列表处理。

### 13.3 任务要求

1. 按标准排错流程逐个复现问题，不直接重写全部代码。
2. 为每个问题记录现象、错误类型、文件和行号。
3. 至少使用一次行断点、Step over、Scope 和 Call Stack。
4. 使用 `try...catch` 处理函数主动抛出的错误。
5. 使用 `Array.isArray()` 验证申请数据结构。
6. 区分空数组与不符合约定的数据类型。
7. 修正后重新验证提交、列表显示、空数组、错误类型以及重复操作。

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
- 数据检查失败时报告原因，不伪装成正常空列表。
- 页面显示适合用户理解的错误提示，控制台保留开发者需要的信息。
- 正常流程能在当前页面中提交并显示内存数据；刷新后重新使用初始数据，不要求恢复上一次输入。
- 控制台没有未处理错误，也没有敏感数据日志。
