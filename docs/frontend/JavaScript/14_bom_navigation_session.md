# 第 14 章 BOM、页面跳转与浏览器信息

本章学习 JavaScript 如何读取浏览器地址、控制页面跳转、操作历史记录、使用定时器，以及在多个页面之间传递必要信息。完成本章后，你应当能够：

- 区分 DOM 和 BOM，说明 `window` 的作用。
- 使用 `location` 读取地址、刷新页面和跳转页面。
- 使用 `URLSearchParams` 读取和生成查询参数。
- 使用 `history` 完成基本的前进与后退操作。
- 正确处理原生弹窗的返回值。
- 创建并清理一次性定时器和重复定时器。
- 根据数据用途选择 URL 参数或 `sessionStorage`。

本章示例应通过本地 Web 服务器运行，并在浏览器开发者工具的 Console、Network 和 Application 面板中验证。

## 1. 浏览器环境与 BOM

### 1.1 DOM 和 BOM 的区别

JavaScript 在浏览器中运行时，既可以操作网页内容，也可以使用浏览器提供的功能。

| 名称 | 主要对象 | 关注内容 | 常见能力 |
| --- | --- | --- | --- |
| DOM | `document` | 当前页面的 HTML 结构 | 查询元素、修改内容、绑定事件 |
| BOM | `window` 及其相关对象 | 浏览器窗口和页面运行环境 | 地址、跳转、历史记录、弹窗、定时器 |

简单理解：

- 修改页面中的标题、表单和列表，主要使用 DOM。
- 读取地址栏、跳转页面、后退或启动定时器，主要使用 BOM。

“BOM”是对浏览器相关对象的习惯称呼，并不像 DOM 那样对应一棵页面节点树。实际开发中应关注 `window`、`location`、`history` 等具体对象。

### 1.2 BOM 常见对象

```js
console.log(window);
console.log(window.document);
console.log(window.location);
console.log(window.history);
console.log(window.navigator);
```

| 对象 | 作用 | 本章掌握程度 |
| --- | --- | --- |
| `window` | 表示当前浏览器窗口，也是浏览器脚本的全局对象 | 必须掌握 |
| `location` | 读取和修改当前页面地址 | 必须掌握 |
| `history` | 操作当前标签页的会话历史 | 会使用 |
| `navigator` | 提供语言、联网状态等浏览器环境信息 | 能看懂 |
| `document` | 表示当前 HTML 页面 | 已在 DOM 章节学习 |

## 2. `window` 全局对象

### 2.1 为什么有时可以省略 `window.`

在普通浏览器脚本中，很多全局浏览器 API 都可以通过 `window` 访问：

```js
window.alert("申请成功");
window.setTimeout(() => {
  console.log("延迟执行");
}, 1000);
```

通常也会写成：

```js
alert("申请成功");
setTimeout(() => {
  console.log("延迟执行");
}, 1000);
```

两种写法调用的是相同的浏览器功能。保留 `window.` 可以强调该能力来自浏览器；省略后代码更简洁。

### 2.2 浏览器 API 不能在所有环境中使用

`window`、`document` 和 `location` 是浏览器环境提供的对象。直接在普通 Node.js 程序中运行下面的代码会找不到 `window`：

```js
console.log(window.location.href);
```

因此，看到某个 API 时，要先判断它属于 JavaScript 语言本身，还是属于浏览器运行环境。

### 2.3 读取窗口大小

```js
console.log(window.innerWidth);
console.log(window.innerHeight);
```

- `innerWidth`：页面可视区域的宽度，单位是 CSS 像素。
- `innerHeight`：页面可视区域的高度，单位是 CSS 像素。

可以监听窗口尺寸变化：

```js
function showWindowSize() {
  console.log(`${window.innerWidth} × ${window.innerHeight}`);
}

window.addEventListener("resize", showWindowSize);
```

`resize` 可能在拖动窗口时频繁触发，不要在其中执行大量计算。响应式页面布局仍应优先使用 CSS 媒体查询，而不是依靠 JavaScript 不断修改样式。

## 3. 使用 `location` 读取地址和跳转

### 3.1 地址由哪些部分组成

以这个地址为例：

```text
https://example.com:8443/app/confirm.html?id=APP-001&mode=view#detail
```

`location` 的常用属性如下：

| 属性 | 示例结果 | 含义 |
| --- | --- | --- |
| `location.href` | 完整地址 | 当前页面的完整 URL |
| `location.protocol` | `https:` | 协议 |
| `location.host` | `example.com:8443` | 主机名和端口 |
| `location.hostname` | `example.com` | 主机名 |
| `location.port` | `8443` | 端口；使用默认端口时可能为空字符串 |
| `location.pathname` | `/app/confirm.html` | 路径 |
| `location.search` | `?id=APP-001&mode=view` | 查询字符串，包含开头的 `?` |
| `location.hash` | `#detail` | 片段标识，包含开头的 `#` |

可以在控制台观察当前页面的真实结果：

```js
console.log(location.href);
console.log(location.pathname);
console.log(location.search);
console.log(location.hash);
```

### 3.2 使用 `location.href` 跳转

```js
location.href = "complete.html";
```

给 `location.href` 赋新地址会跳转页面，并把当前页面保留在历史记录中。用户通常可以点击“后退”回到原页面。

常见场景包括登录成功后进入首页、申请提交后进入完成页。

### 3.3 使用 `location.assign()` 跳转

```js
location.assign("complete.html");
```

`assign(url)` 跳转到指定地址，效果与给 `location.href` 赋值接近，当前页面通常会保留在历史记录中。

| 参数 | 可接受的值 | 默认值或必填性 | 返回值 |
| --- | --- | --- | --- |
| `url` | 相对地址或合法的绝对 URL 字符串 | 必填 | `undefined`；浏览器开始跳转 |

项目中普通跳转选择 `location.href` 或 `assign()` 之一并保持一致即可。

### 3.4 使用 `location.replace()` 替换当前记录

```js
location.replace("login.html");
```

`replace(url)` 会用新页面替换当前历史记录。用户点击“后退”时，通常不会回到被替换的页面。

| 参数 | 可接受的值 | 默认值或必填性 | 返回值 |
| --- | --- | --- | --- |
| `url` | 相对地址或合法的绝对 URL 字符串 | 必填 | `undefined`；浏览器开始跳转 |

例如，失效页面跳回登录页时可以考虑使用 `replace()`。但前端跳转不能代替后端身份与权限检查。

### 3.5 使用 `location.reload()` 刷新

```js
location.reload();
```

`reload()` 重新加载当前页面。它不需要参数，返回 `undefined`。正式业务代码不要通过反复刷新掩盖状态管理问题；只有确实需要重新请求整个页面时再使用。

### 3.6 相对地址和绝对地址

```js
location.href = "complete.html";
location.href = "/paid-leave/complete.html";
location.href = "https://example.com/paid-leave/complete.html";
```

| 写法 | 解析方式 |
| --- | --- |
| `complete.html` | 相对于当前页面所在目录 |
| `/paid-leave/complete.html` | 相对于当前网站根路径 |
| 完整 `https://...` 地址 | 跳转到指定的完整地址，可能跨网站 |

路径写错时，常见现象是出现 404 页面。可以在 Network 面板观察浏览器实际请求的 URL。

## 4. URL 与查询参数

### 4.1 查询参数适合传递什么

查询参数位于 URL 的 `?` 后面：

```text
confirm.html?id=APP-001&mode=view
```

它适合传递页面定位所需的短小、非敏感信息，例如：

- 申请编号
- 页码
- 搜索关键词
- 排序方式
- 筛选条件

URL 会出现在地址栏、浏览器历史、日志和分享链接中，所以不要放密码、令牌、身份证号或完整业务对象。

### 4.2 使用 `URLSearchParams` 读取参数

假设当前地址是：

```text
http://localhost:5500/confirm.html?id=APP-001&mode=view
```

读取参数：

```js
const params = new URLSearchParams(location.search);
const applicationId = params.get("id");
const mode = params.get("mode");

console.log(applicationId); // APP-001
console.log(mode); // view
```

`new URLSearchParams(search)` 创建用于处理查询参数的对象。当前示例把 `location.search` 中的查询字符串交给它。

`get(name)` 接收参数名，返回第一个匹配值；参数不存在时返回 `null`。

```js
const page = params.get("page");

if (page === null) {
  console.log("没有提供页码");
}
```

### 4.3 判断、增加、修改和删除参数

```js
const params = new URLSearchParams("status=pending&page=1");

console.log(params.has("status")); // true

params.set("page", "2");
params.set("sort", "date");
params.delete("status");

console.log(params.toString()); // page=2&sort=date
```

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 返回值 |
| --- | --- | --- | --- | --- |
| `get(name)` | 参数名 | 字符串 | 必填 | 第一个参数值；不存在时为 `null` |
| `has(name)` | 参数名 | 字符串 | 必填 | 是否存在，返回布尔值 |
| `set(name, value)` | 参数名、参数值 | 字符串或可转换为字符串的值 | 两项必填 | `undefined` |
| `append(name, value)` | 参数名、参数值 | 字符串或可转换为字符串的值 | 两项必填 | 追加同名参数，返回 `undefined` |
| `delete(name)` | 参数名 | 字符串 | 必填 | 删除同名参数，返回 `undefined` |
| `toString()` | 无 | 无 | 无参数 | 不带开头 `?` 的查询字符串 |

`set()` 会替换同名参数，`append()` 可以保留多个同名参数。初学阶段大多数业务筛选使用 `set()` 即可。

### 4.4 生成带参数的跳转地址

```js
const params = new URLSearchParams();
params.set("id", "APP-001");
params.set("mode", "view");

location.href = `confirm.html?${params.toString()}`;
```

使用 `URLSearchParams` 可以自动处理空格、日文等字符的编码，比手工拼接查询字符串更可靠。

### 4.5 查询参数不能作为可信数据

用户可以直接修改地址栏中的内容。因此：

- 参数不存在时要提供默认处理或错误提示。
- 参数格式不正确时要停止后续操作。
- 前端不能根据 `?role=admin` 判断用户权限。
- 后端收到编号等参数后，仍然必须检查身份和数据访问权限。

## 5. 使用 `history` 操作历史记录

### 5.1 后退、前进和跳转指定步数

```js
history.back();
history.forward();
history.go(-2);
```

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 作用与返回值 |
| --- | --- | --- | --- | --- |
| `back()` | 无 | 无 | 无参数 | 后退一条记录，返回 `undefined` |
| `forward()` | 无 | 无 | 无参数 | 前进一条记录，返回 `undefined` |
| `go(delta)` | 跳转步数 | 整数 | 必填 | 负数后退、正数前进、`0` 重新加载；返回 `undefined` |

如果没有对应的历史记录，页面可能不会发生变化。历史记录属于用户当前标签页，代码不能读取用户访问过的完整地址清单。

### 5.2 返回按钮的选择

```js
const backButton = document.querySelector("#backButton");

backButton.addEventListener("click", () => {
  history.back();
});
```

这个示例要求 HTML 中存在：

```html
<button id="backButton" type="button">返回</button>
```

如果业务规格要求固定返回申请页，应直接链接或跳转到 `apply.html`；如果要求回到用户刚才访问的页面，才适合使用 `history.back()`。

### 5.3 单页应用相关接口：了解

现代前端路由常使用：

- `history.pushState()`：增加一条历史记录，但不自动重新加载页面。
- `history.replaceState()`：替换当前历史记录，但不自动重新加载页面。
- `popstate` 事件：用户前进或后退时通知 JavaScript。

```js
history.pushState({ page: "detail" }, "", "?id=APP-001");

window.addEventListener("popstate", (event) => {
  console.log(event.state);
});
```

`pushState(state, unused, url)` 的 `state` 可以保存与该历史记录相关的数据，第二个参数保留但通常传空字符串，`url` 是同源的新地址。它不会自动请求新页面，也不会自动更新 DOM。

Vue Router 和 React Router 会封装类似能力。当前阶段要求能看懂用途，不要求自己实现路由器。

## 6. 浏览器原生弹窗

### 6.1 `alert()` 显示提示

```js
alert("保存成功");
```

`alert(message)` 显示一段提示文字。`message` 可以是字符串或可转换为字符串的值，是必填内容；方法返回 `undefined`。

### 6.2 `confirm()` 让用户确认

```js
const confirmed = confirm("确定要删除这条申请吗？");

if (confirmed) {
  console.log("执行删除");
}
```

`confirm(message)` 接收提示文字：点击“确定”返回 `true`，点击“取消”返回 `false`。

### 6.3 `prompt()` 获取简单文本

```js
const employeeName = prompt("请输入姓名", "田中太郎");

if (employeeName === null) {
  console.log("用户取消了输入");
} else {
  console.log(employeeName);
}
```

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `message` | 字符串或可转换为字符串的值 | 可选，默认无提示文字 | 显示给用户的问题 |
| `defaultValue` | 字符串或可转换为字符串的值 | 可选，默认输入框为空 | 输入框初始值 |

`prompt()` 点击确定时返回输入字符串，点击取消时返回 `null`。空字符串和 `null` 的含义不同，判断时不能混为一谈。

### 6.4 原生弹窗的使用边界

这些弹窗会阻塞当前页面的 JavaScript 执行和用户交互，并且样式难以定制：

- 教学实验和简单确认可以使用。
- 表单错误应优先显示在对应字段附近。
- 正式业务提示和对话框通常使用 DOM 元素或 UI 组件实现。
- 删除等重要操作不能只依赖前端确认，后端仍需检查权限和请求是否合法。

## 7. 定时器及清理

### 7.1 `setTimeout()` 延迟执行一次

```js
const timerId = setTimeout(() => {
  console.log("1.5 秒后执行一次");
}, 1500);

console.log(timerId);
```

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `handler` | 函数 | 必填 | 到时间后执行的函数 |
| `delay` | 毫秒数 | 可选，省略时按 `0` 处理 | 至少等待多长时间后安排执行 |
| 后续参数 | 任意值 | 可选 | 作为参数传给 `handler`；主线通常不用 |

`setTimeout()` 返回定时器 ID，可以用于取消任务。`1500` 毫秒等于 `1.5` 秒。

延迟时间不是精确执行时刻。浏览器会在等待时间到达且当前同步代码执行完毕后，才有机会执行回调。

### 7.2 使用 `clearTimeout()` 取消任务

```js
const timerId = setTimeout(() => {
  location.href = "index.html";
}, 1500);

const cancelButton = document.querySelector("#cancelButton");

cancelButton.addEventListener("click", () => {
  clearTimeout(timerId);
});
```

HTML 中需要存在：

```html
<button id="cancelButton" type="button">取消自动跳转</button>
```

`clearTimeout(timerId)` 接收 `setTimeout()` 返回的 ID，取消尚未执行的定时任务，返回 `undefined`。如果回调已经执行，取消不会撤销已经发生的结果。

### 7.3 `setInterval()` 按间隔重复执行

```js
let remainingSeconds = 3;

const intervalId = setInterval(() => {
  console.log(`剩余 ${remainingSeconds} 秒`);
  remainingSeconds -= 1;

  if (remainingSeconds < 0) {
    clearInterval(intervalId);
  }
}, 1000);
```

`setInterval(handler, delay)` 的参数含义与 `setTimeout()` 相同，但会重复安排回调。它返回定时器 ID；`clearInterval(intervalId)` 停止后续执行并返回 `undefined`。

### 7.4 为什么必须清理定时器

没有清理的重复定时器会继续执行，可能造成重复请求、重复修改页面或资源浪费。以下情况应考虑清理：

- 用户取消了操作。
- 倒计时已经结束。
- 对应页面区域已经不再使用。
- Vue 或 React 组件被卸载。

新人主线重点掌握 `setTimeout()`、`clearTimeout()`、`setInterval()` 和 `clearInterval()` 的成对使用。

## 8. 浏览器信息：会读取即可

### 8.1 页面语言

```js
console.log(navigator.language);
```

`navigator.language` 返回浏览器偏好的主要语言，例如 `ja`、`ja-JP` 或 `zh-CN`。它只能作为显示语言的参考，不能代表用户国籍或所在地区。

### 8.2 联网状态

```js
console.log(navigator.onLine);
```

`navigator.onLine` 返回布尔值，表示浏览器当前是否认为存在网络连接。但返回 `true` 不代表目标服务器一定可访问，真正的请求仍然可能失败。

可以监听状态变化：

```js
window.addEventListener("online", () => {
  console.log("浏览器报告网络已恢复");
});

window.addEventListener("offline", () => {
  console.log("浏览器报告网络已断开");
});
```

不要使用 `navigator.userAgent` 编写脆弱的浏览器判断。需要某项能力时，更适合直接检测对应 API 是否存在。

## 9. 页面之间传递数据

### 9.1 使用 URL 参数传递标识

列表页跳转详情页时，可以传递申请编号：

```js
const params = new URLSearchParams();
params.set("id", "APP-001");

location.href = `detail.html?${params.toString()}`;
```

详情页读取编号后，再根据编号查找数据：

```js
const params = new URLSearchParams(location.search);
const applicationId = params.get("id");

if (applicationId === null || applicationId === "") {
  console.error("缺少申请编号");
} else {
  console.log(`准备读取申请：${applicationId}`);
}
```

URL 适合保存“要查看哪一条数据”，刷新或分享地址后仍能保留这个定位信息。

### 9.2 使用 `sessionStorage` 传递临时对象

申请页跳转确认页前保存草稿：

```js
const draftApplication = {
  leaveType: "有給休暇",
  startDate: "2026-09-01",
};

sessionStorage.setItem(
  "paidLeave.draftApplication",
  JSON.stringify(draftApplication),
);

location.href = "confirm.html";
```

确认页读取草稿：

```js
const savedText = sessionStorage.getItem("paidLeave.draftApplication");

if (savedText === null) {
  location.replace("apply.html");
} else {
  const draftApplication = JSON.parse(savedText);
  console.log(draftApplication);
}
```

这里是在页面跳转场景中应用上一章的 Web Storage 知识。JSON 转换、异常处理和 `sessionStorage` 生命周期请回顾[第 13 章 JSON 与浏览器本地存储](13_json_browser_storage.md)。

### 9.3 如何选择

| 数据需求 | 推荐方式 | 示例 |
| --- | --- | --- |
| 地址应可刷新、收藏或分享 | URL 参数 | 申请编号、页码、筛选条件 |
| 只在当前多页面流程临时使用 | `sessionStorage` | 尚未提交的表单草稿 |
| 需要后端长期可靠保存 | 发送到后端 | 正式申请记录、用户权限 |

不要把完整对象转换后塞进 URL，也不要把 `sessionStorage` 当成正式数据库。

## 10. 常见错误与排查

### 10.1 跳转路径错误

症状通常是 404。检查当前页面路径、目标文件位置和 Network 面板中实际请求的地址。

### 10.2 把 `replace()` 当成普通跳转

使用 `replace()` 后，用户可能无法后退到原页面。普通业务跳转优先使用 `location.href` 或 `assign()`。

### 10.3 没有处理缺失参数

`URLSearchParams.get()` 在参数不存在时返回 `null`。必须检查 `null` 和业务要求的格式，不能直接继续查询或渲染。

### 10.4 手工拼接查询参数

包含空格、日文、`&` 等字符时，手工拼接容易产生错误。使用 `URLSearchParams` 生成查询字符串。

### 10.5 忘记清理定时器

如果控制台持续输出、页面反复跳转或逻辑执行多次，检查 `setInterval()` 是否在正确时机调用了 `clearInterval()`。

### 10.6 混淆前端跳转和权限控制

隐藏页面、修改 URL 或跳回登录页都不能保护后端数据。真正的身份认证和权限检查必须由后端执行。

## 11. 本章综合练习

### 11.1 初始状态

准备以下同目录页面：

```text
application-list.html
application-detail.html
complete.html
login.html
```

每个 HTML 文件都通过外部脚本加载对应 JavaScript。使用本地 Web 服务器打开 `application-list.html`。

### 11.2 任务要求

1. 列表页点击某条申请时，使用 `URLSearchParams` 生成带 `id` 的详情页地址。
2. 详情页读取 `id`；缺少 `id` 时显示错误信息，不继续执行业务处理。
3. 详情页提供“返回”按钮，根据规格固定返回列表页，不直接假设 `history.back()` 一定合适。
4. 完成页显示 5 秒倒计时，结束后使用 `location.replace()` 前往列表页。
5. 完成页提供“取消自动跳转”按钮，并使用 `clearTimeout()` 或清理倒计时定时器。
6. 在控制台分别观察 `location.pathname`、`location.search` 和 `history.length`。
7. 修改 URL 中的 `id`，确认页面会验证参数，而不是把参数当成可信权限信息。

### 11.3 实现限制

- 不手工拼接未经处理的用户输入，使用 `URLSearchParams` 生成查询字符串。
- URL 中不放密码、令牌、完整申请内容或其他敏感数据。
- 所有查询到的 DOM 元素必须与 HTML 中的 `id` 一致。
- 定时器结束或被取消后，不再继续输出或跳转。
- 页面信息使用 `textContent` 或 DOM 方法显示。

### 11.4 完成标准

- 列表页能够生成正确的详情页 URL。
- 刷新详情页后仍能从 URL 读取申请编号。
- 缺失或空白 `id` 有明确提示，控制台没有未处理异常。
- 普通跳转与替换历史记录的行为符合要求。
- 取消倒计时后不会发生自动跳转。
- Network 面板中实际访问路径与预期一致。
