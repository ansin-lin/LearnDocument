# 第 13 章 JSON 与浏览器本地存储

本章学习如何在 JavaScript、JSON 文本和浏览器存储之间转换数据。完成后，你应当能够：

- 说明 JSON 的作用和常见应用场景。
- 区分 JavaScript 对象、JSON 文本和 Java 对象。
- 使用 `JSON.stringify()` 和 `JSON.parse()` 转换数据。
- 使用 `localStorage` 和 `sessionStorage` 保存、读取和删除数据。
- 区分 `sessionStorage`、Cookie 和服务端 Session。

本章示例需要在浏览器中运行。建议通过本地 Web 服务器打开 HTML 页面；不同浏览器对 `file://` 页面的存储处理可能不同。

## 1. JSON 解决什么问题

### 1.1 程序之间需要交换数据

JavaScript 对象适合在 JavaScript 程序运行期间使用：

```js
const application = {
  employeeName: "田中太郎",
  leaveType: "有給休暇",
  approved: false,
};
```

但是，网络请求、文本文件和浏览器存储不能直接保存“正在内存中运行的 JavaScript 对象”。它们需要一种可以传输或保存的文本格式。

JSON（JavaScript Object Notation）是一种表示数据的文本格式：

```json
{
  "employeeName": "田中太郎",
  "leaveType": "有給休暇",
  "approved": false
}
```

JSON 的文字形式参考了 JavaScript 对象字面量，所以二者看起来很像，但它们不是同一种东西。

### 1.2 JSON 的常见应用场景

| 场景 | JSON 的作用 |
| --- | --- |
| 前端调用后端 API | 在请求体中发送数据，或读取响应体中的数据 |
| 不同语言之间交换数据 | 让 JavaScript、Java、Python 等程序使用共同的数据格式 |
| 浏览器本地存储 | 先把对象或数组转成 JSON 文本，再保存到 Web Storage |
| 配置文件或数据文件 | 用文本记录结构化数据 |
| 调试与日志 | 用容易观察的文本形式查看数据结构 |

JSON 只负责表示数据。它不是编程语言，不会执行判断、循环和函数；它也不是数据库，不负责查询、权限、并发和长期可靠保存。

## 2. JSON、JavaScript 和 Java 的数据不是一回事

### 2.1 JavaScript 对象是运行时的值

下面的 `application` 是 JavaScript 对象。它存在于当前程序的内存中，可以包含方法和 JavaScript 特有的值。

```js
const application = {
  employeeName: "田中太郎",
  approved: false,
  note: undefined,
  printSummary() {
    console.log(this.employeeName);
  },
};

console.log(application.employeeName);
application.printSummary();
```

这里的 `undefined` 和函数可以存在于 JavaScript 对象中，但不是合法的 JSON 值。

### 2.2 JSON 是文本

下面的内容是 JSON 文本，不是 JavaScript 对象：

```js
const jsonText = '{"employeeName":"田中太郎","approved":false}';

console.log(typeof jsonText); // string
```

因为 `jsonText` 是字符串，所以不能直接写 `jsonText.employeeName`。必须先使用 `JSON.parse()` 把它解析为 JavaScript 对象。

### 2.3 Java 对象由 Java 类型约束

Java 中通常先定义类，再根据类创建对象。下面是用于说明字段类型的 Java 类片段：

```java
public class ApplicationRequest {
    private String employeeName;
    private boolean approved;
}
```

Java 字段已经声明为 `String` 和 `boolean`。后端收到 JSON 后，需要由 JSON 转换库按照字段名和类型，将 JSON 数据转换成 Java 对象。Java 对象本身并不是 JSON。

反过来，Java 后端返回数据时，也通常由转换库把 Java 对象序列化为 JSON 响应。具体转换方式会在 Java Web 或 Spring 课程中学习。

### 2.4 三者对照

| 对比项 | JavaScript 对象 | JSON | Java 对象 |
| --- | --- | --- | --- |
| 本质 | 程序运行时的对象 | 表示数据的文本格式 | 按 Java 类创建的运行时对象 |
| 是否是字符串 | 否 | 是 | 否 |
| 属性或字段名 | 标识符或字符串 | 必须是双引号包围的字符串 | 由类中的字段定义 |
| 字符串写法 | 可用单引号、双引号或反引号 | 只能用双引号 | 使用双引号 |
| 类型约束 | 动态类型 | 只有 JSON 规定的几种数据类型 | 字段具有明确的 Java 类型 |
| 能否包含方法 | 可以 | 不可以 | 可以 |
| 能否包含 `undefined` | 可以 | 不可以 | Java 没有 `undefined` |
| 注释和末尾逗号 | JavaScript 源代码中可以出现 | 不允许 | 按 Java 语法书写，与 JSON 规则不同 |

必须记住这条转换关系：

```text
JavaScript 对象 --JSON.stringify()--> JSON 文本
JavaScript 对象 <--JSON.parse()------ JSON 文本

Java 对象 <------JSON 转换库--------> JSON 文本
```

JSON 是二者交换数据时使用的共同格式，不是 JavaScript 对象和 Java 对象的共同类型。

### 2.5 类型名称相似，也不代表完全一致

| JSON 值 | 转成 JavaScript 后 | Java 后端常见接收类型 | 注意点 |
| --- | --- | --- | --- |
| 字符串 | `string` | `String` | 日期在 JSON 中通常也是字符串，JSON 没有日期类型 |
| 数字 | `number` | `int`、`long`、`double`、`BigDecimal` 等 | JSON 不区分整数类型；Java 需要选择具体类型 |
| 布尔值 | `boolean` | `boolean` 或 `Boolean` | JSON 中只能写小写 `true`、`false` |
| `null` | `null` | 可为空的引用类型 | Java 基本类型如 `int`、`boolean` 不能保存 `null` |
| 对象 | 普通对象 | DTO、JavaBean、`Map` 等 | 字段名和结构需要能够对应 |
| 数组 | 数组 | 数组或 `List` | 数组中的元素类型也需要对应 |

还要注意两个常见的跨语言问题：

- JSON 没有 `Date`、枚举、`char` 等专用类型，通常需要约定用字符串或数字表示。
- Java 的 `long` 可以表示比 JavaScript 安全整数范围更大的整数。编号特别大时，前后端常把编号约定为字符串，避免 JavaScript 读取后发生精度丢失。

## 3. JSON 能表示哪些数据

### 3.1 JSON 的六类值

| JSON 类型 | 示例 |
| --- | --- |
| 对象 | `{"name":"田中"}` |
| 数组 | `["有給休暇","午前休"]` |
| 字符串 | `"田中太郎"` |
| 数字 | `10`、`3.5`、`-1` |
| 布尔值 | `true`、`false` |
| 空值 | `null` |

JSON 最外层可以是以上任意一种值。接口和业务数据中最常见的最外层结构是对象或数组。

### 3.2 JSON 的基本语法规则

- 对象使用 `{}`，数组使用 `[]`。
- 对象的属性名和字符串必须使用双引号。
- 属性或数组元素之间使用逗号分隔，最后一项后面不能写逗号。
- 布尔值和 `null` 必须写成小写。
- JSON 中不能写注释、函数、`undefined`、`Symbol`、`BigInt`、`NaN` 或 `Infinity`。

下面是合法 JSON：

```json
{
  "employeeId": "E001",
  "days": 2,
  "approved": false,
  "note": null,
  "types": ["有給休暇", "午前休"]
}
```

下面不是合法 JSON：

```text
{
  employeeId: 'E001', // 未使用双引号，还包含注释
  approved: false,
}
```

这段写法接近 JavaScript 对象字面量，却不符合 JSON 语法。不要根据“看起来像对象”判断它是不是 JSON。

## 4. 在 JavaScript 中转换 JSON

### 4.1 使用 `JSON.stringify()` 序列化

`JSON.stringify()` 把 JavaScript 值转换为 JSON 文本，这个过程称为序列化。

```js
const application = {
  employeeName: "田中太郎",
  leaveType: "有給休暇",
};

const jsonText = JSON.stringify(application);
console.log(jsonText);
console.log(typeof jsonText); // string
```

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `value` | 任意 JavaScript 值 | 必填 | 要转换的数据 |
| `replacer` | 函数、属性名数组或 `null` | 可选，默认不筛选 | 筛选或修改属性；主线暂不使用 |
| `space` | 数字、字符串或省略 | 可选，默认不缩进 | 设置输出缩进，便于阅读 |

返回结果通常是 JSON 字符串。某些不能独立表示为 JSON 的值可能返回 `undefined`，循环引用和 `BigInt` 等情况还可能抛出错误。

为了便于观察，可以设置缩进：

```js
const readableText = JSON.stringify(application, null, 2);
console.log(readableText);
```

对象属性中的 `undefined` 和函数会被忽略：

```js
const data = {
  name: "田中",
  note: undefined,
  print() {
    console.log("打印");
  },
};

console.log(JSON.stringify(data)); // {"name":"田中"}
```

因此，`JSON.stringify()` 不是完整克隆所有 JavaScript 数据的工具。

### 4.2 使用 `JSON.parse()` 反序列化

`JSON.parse()` 读取 JSON 文本，并转换成对应的 JavaScript 值，这个过程称为反序列化或解析。

```js
const jsonText = '{"employeeName":"田中太郎","approved":false}';
const application = JSON.parse(jsonText);

console.log(application.employeeName); // 田中太郎
console.log(typeof application); // object
```

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `text` | 合法 JSON 文本 | 必填 | 要解析的字符串 |
| `reviver` | 函数或省略 | 可选，默认不转换 | 在解析过程中修改值；主线暂不使用 |

`JSON.parse()` 返回由 JSON 表示的 JavaScript 值。输入不是合法 JSON 时会抛出 `SyntaxError`。

### 4.3 处理无效 JSON

外部接口、手工编辑的数据或旧版存储内容可能不是合法 JSON。解析不可信内容时，应使用 `try...catch`：

```js
function parseApplication(jsonText) {
  try {
    return JSON.parse(jsonText);
  } catch (error) {
    console.error("申请数据不是合法 JSON", error);
    return null;
  }
}

const application = parseApplication('{"employeeName":"田中"}');

if (application !== null) {
  console.log(application.employeeName);
}
```

`parseApplication()` 接收 JSON 字符串；成功时返回 JavaScript 值，失败时返回 `null`。`try...catch` 的完整用法将在错误处理章节继续学习。

## 5. 浏览器 Web Storage 的作用和边界

### 5.1 Web Storage 是什么

浏览器提供了两种键值形式的 Web Storage：

- `localStorage`：保存刷新或重新打开页面后仍要保留的少量数据。
- `sessionStorage`：保存当前标签页使用的临时数据。

它们都按照 `key → value` 保存数据，而且 key 和 value 最终都是字符串。保存对象或数组时，需要先转换为 JSON 文本。

### 5.2 数据属于哪个网站

Web Storage 按源隔离。源通常由协议、域名和端口共同决定。不同源的页面不能随意读取彼此的数据。

```text
http://localhost:5500
http://localhost:8080
```

以上两个地址端口不同，属于不同的源。

### 5.3 Web Storage 不适合保存什么

- 不保存密码、密钥、身份证号等敏感信息。
- 不把登录令牌或 Session ID 当作普通练习数据保存到 `localStorage`。
- 不把“前端存了已登录”当作真正的身份认证；用户可以修改或清除浏览器存储。
- 不保存大量数据，也不把它当成数据库使用。

Web Storage API 同步执行。少量练习数据通常没有问题，但频繁读写大量数据会影响页面响应。

## 6. 使用 `localStorage`

### 6.1 保存和读取字符串

`setItem()` 保存一条数据，`getItem()` 按 key 读取数据：

```js
localStorage.setItem("paidLeave.displayName", "田中太郎");

const displayName = localStorage.getItem("paidLeave.displayName");
console.log(displayName); // 田中太郎
```

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 返回值 |
| --- | --- | --- | --- | --- |
| `setItem(key, value)` | `key`、`value` | 会转换为字符串的值 | 两项必填 | `undefined` |
| `getItem(key)` | `key` | 字符串形式的 key | 必填 | 字符串；不存在时为 `null` |
| `removeItem(key)` | `key` | 字符串形式的 key | 必填 | `undefined` |
| `clear()` | 无 | 无 | 无参数 | `undefined` |

不能直接依靠自动转换保存对象：

```js
const application = { employeeName: "田中太郎" };

localStorage.setItem("wrongApplication", application);
console.log(localStorage.getItem("wrongApplication")); // [object Object]
```

对象结构已经丢失。正确做法是先使用 `JSON.stringify()`。

### 6.2 保存和读取对象

```js
const application = {
  employeeId: "E001",
  leaveType: "有給休暇",
  days: 2,
};

localStorage.setItem(
  "paidLeave.latestApplication",
  JSON.stringify(application),
);
```

读取时先判断 key 是否存在，再解析：

```js
const savedText = localStorage.getItem("paidLeave.latestApplication");

if (savedText !== null) {
  const savedApplication = JSON.parse(savedText);
  console.log(savedApplication.leaveType);
}
```

### 6.3 保存和读取数组

```js
const applications = [
  { id: 1, type: "有給休暇", status: "申請中" },
  { id: 2, type: "午前休", status: "承認済" },
];

localStorage.setItem(
  "paidLeave.applications",
  JSON.stringify(applications),
);
```

```js
const savedText = localStorage.getItem("paidLeave.applications");
const applications = savedText === null ? [] : JSON.parse(savedText);

console.log(applications);
```

执行顺序是：读取 JSON 文本；key 不存在时使用空数组；存在时用 `JSON.parse()` 还原数组。如果内容可能被手工修改，还应参考 4.3 节处理解析失败。

### 6.4 删除数据

```js
localStorage.removeItem("paidLeave.latestApplication");
```

`removeItem()` 删除指定 key。`localStorage.clear()` 则会删除当前源下全部 `localStorage` 数据：

```js
localStorage.clear();
```

业务代码应优先使用 `removeItem()` 精确删除；`clear()` 更适合开发调试或明确的“清空全部数据”功能。

## 7. 使用 `sessionStorage`

### 7.1 生命周期和常见用途

`sessionStorage` 的方法与 `localStorage` 相同，但数据主要属于当前标签页的页面会话：

- 刷新当前页面后，数据通常仍然存在。
- 在同一标签页跳转到同源页面时，可以继续读取。
- 关闭该标签页或窗口后，这次页面会话的数据通常被清除。
- 不同标签页的 `sessionStorage` 通常彼此独立。

它适合申请页跳转确认页时保存草稿，不适合长期保存正式业务数据。

### 7.2 保存和读取草稿

申请页面保存草稿：

```js
const draftApplication = {
  leaveType: "有給休暇",
  startDate: "2026-09-01",
};

sessionStorage.setItem(
  "paidLeave.draftApplication",
  JSON.stringify(draftApplication),
);
```

确认页面读取草稿：

```js
const savedText = sessionStorage.getItem("paidLeave.draftApplication");

if (savedText !== null) {
  const draftApplication = JSON.parse(savedText);
  console.log(draftApplication.startDate);
}
```

确认或取消后删除草稿：

```js
sessionStorage.removeItem("paidLeave.draftApplication");
```

### 7.3 两种 Web Storage 的选择

| 对比项 | `localStorage` | `sessionStorage` |
| --- | --- | --- |
| 刷新页面后 | 保留 | 保留 |
| 关闭标签页后 | 通常保留 | 通常清除 |
| 标签页之间 | 同源页面可读取同一份数据 | 各标签页通常独立 |
| 常见用途 | 界面偏好、练习项目的持久数据 | 多页面流程中的临时草稿 |
| 是否自动随 HTTP 请求发送 | 否 | 否 |

## 8. `sessionStorage`、Cookie 和 Session 的关系

### 8.1 `sessionStorage` 不是服务端 Session

名字中虽然都有 `session`，但二者不是同一种技术：

- `sessionStorage` 是浏览器提供的前端存储，数据保存在当前标签页对应的浏览器环境中。
- 服务端 Session 是后端保存用户会话状态的一种方式，数据主要保存在服务器端。

### 8.2 Cookie 与 Session 的最小认识

Cookie 是浏览器保存的一小段数据。符合域名、路径、安全策略等条件时，浏览器可以在 HTTP 请求中自动携带 Cookie。

服务端 Session 常见的工作方式是：后端保存会话数据，把 Session ID 通过 Cookie 交给浏览器；后续请求携带该 Cookie，后端再通过 Session ID 找到对应的会话数据。

| 对比项 | `localStorage` | `sessionStorage` | Cookie | 服务端 Session |
| --- | --- | --- | --- | --- |
| 主要保存位置 | 浏览器 | 浏览器当前标签页会话 | 浏览器 | 后端 |
| 是否自动随 HTTP 请求发送 | 否 | 否 | 符合条件时会 | Session 数据本身不会 |
| 典型用途 | 界面偏好、少量持久数据 | 页面间临时数据 | 会话标识、少量请求相关数据 | 登录会话和服务端状态 |
| 能否作为前端随意信任的登录依据 | 不能 | 不能 | 不能只靠前端判断 | 必须由后端校验 |

本章只要求区分这四个概念。Cookie、Session、HTTP 请求和身份认证的完整流程，请学习 [HTTP、REST、Cookie、Session 与 CORS](../../web_basics/01_http_rest_cookie_cors.md)。

## 9. 统一管理存储 key

### 9.1 为什么不能随意命名

同一个网站可能有多个功能使用浏览器存储。清楚、稳定的 key 可以减少重名和拼写错误。

```js
const STORAGE_KEYS = {
  displayName: "paidLeave.displayName",
  applications: "paidLeave.applications",
  draftApplication: "paidLeave.draftApplication",
};
```

属性值使用“应用名.数据名”的形式区分不同项目。

### 9.2 使用统一 key

```js
localStorage.setItem(STORAGE_KEYS.displayName, "田中太郎");

const displayName = localStorage.getItem(STORAGE_KEYS.displayName);
console.log(displayName);
```

以后需要修改实际 key 时，只修改 `STORAGE_KEYS` 中的定义即可。

## 10. 使用开发者工具验证

写入数据后，打开浏览器开发者工具，在 Application（应用）或 Storage（存储）面板中检查：

1. 选择 Local Storage 或 Session Storage。
2. 选择当前页面所在的源。
3. 确认 key 名称正确。
4. 确认对象和数组保存为 JSON 字符串，而不是 `[object Object]`。
5. 刷新页面，确认数据是否符合预期保留。
6. 删除数据后，确认对应 key 是否消失。

面板名称会因浏览器和语言设置略有不同。

## 11. 常见错误

### 11.1 把 JSON 文本当成对象

```js
const jsonText = '{"name":"田中"}';
console.log(jsonText.name); // undefined
```

应先执行 `JSON.parse(jsonText)`。

### 11.2 直接保存对象

症状是读取结果变成 `[object Object]`。保存前使用 `JSON.stringify()`，读取后使用 `JSON.parse()`。

### 11.3 没有处理不存在的 key

`getItem()` 在 key 不存在时返回 `null`。应先判断或准备默认值。

### 11.4 存储中不是合法 JSON

手工修改、旧版格式或错误写入都可能导致 `JSON.parse()` 抛出异常。先检查原始字符串，再使用 `try...catch` 提供回退结果。

### 11.5 混淆 `sessionStorage` 和 Session

`sessionStorage` 是浏览器标签页的临时存储；Session 通常是后端保存的用户会话。二者不能互相替代。

## 12. 本章练习

### 12.1 初始状态

新建一个独立练习页面，页面包含主题选择框、“保存设置”按钮、“恢复默认”按钮和用于显示当前设置的区域。

### 12.2 任务要求

1. 创建包含 `theme`、`fontSize` 和 `showHints` 的设置对象。
2. 点击“保存设置”后，把对象转换为 JSON 并保存到 `localStorage`。
3. 页面加载时读取设置；没有保存记录时使用默认设置。
4. 把读取到的设置显示到页面中。
5. 点击“恢复默认”后，只删除本练习使用的 key，不调用 `localStorage.clear()`。
6. 把“当前正在编辑的选项”保存到 `sessionStorage`，比较关闭标签页前后的结果。
7. 在开发者工具中把 JSON 改成无效内容，为读取逻辑增加异常处理，使页面能够恢复默认设置。

### 12.3 实现限制

- 对象和数组必须通过 `JSON.stringify()` 保存，通过 `JSON.parse()` 读取。
- key 使用“应用名.数据名”的形式，并集中定义。
- 不保存账号、密码、令牌或其他敏感数据。
- 页面输出使用 `textContent` 或 DOM 创建元素，不把用户可修改的数据直接拼入 `innerHTML`。

### 12.4 完成标准

- 刷新页面后，`localStorage` 中的设置仍然生效。
- `sessionStorage` 中的数据符合标签页会话的生命周期。
- key 不存在或 JSON 损坏时，页面不会中断，能够使用默认设置。
- 开发者工具中能看到合法 JSON 字符串，而不是 `[object Object]`。
- 删除设置时不会影响当前源下其他功能的数据。
