# 第 15 章 正则表达式与格式校验

正则表达式（Regular Expression）用于按照规则查找、检查或替换字符串。账号格式、员工编号、申请编号、搜索关键词和文本清理中经常能看到正则表达式。

完成本章后，你应当能够：

- 说明正则表达式适合解决什么问题。
- 看懂正则字面量中的主体和标志。
- 使用字符、字符组、数量词、边界、分组和选择规则。
- 使用 `test()` 完成常见的格式校验。
- 使用正则完成简单的查找、提取和替换。
- 根据任务选择 `test()`、`match()`、`replace()` 等常用方法。
- 识别过度复杂或不安全的正则写法。

本章不要求背下所有符号。重点是把业务规格逐步翻译成规则，并用正确和错误数据验证结果。

## 1. 正则表达式是什么、适合做什么

### 1.1 正则表达式是什么

正则表达式是一种描述字符串模式的规则。JavaScript 会使用这条规则检查目标字符串，判断哪些位置符合要求，这个过程称为匹配。

例如，`/^EMP-\d{5}$/` 不是员工编号本身，而是对员工编号外形的描述：必须以 `EMP-` 开头，后面是 5 位数字，并且不能包含其他字符。

### 1.2 为什么需要字符串模式

普通字符串方法适合查找固定文字：

```js
const applicationId = "REQ-20260820-001";
console.log(applicationId.startsWith("REQ-")); // true
```

`startsWith(searchText)` 检查字符串是否以指定文字开头。参数 `searchText` 是必填的字符串，符合时返回 `true`，不符合时返回 `false`。它适合检查固定前缀，不负责检查后续数字位数。

如果需求变成“必须以 `REQ-` 开头，后面是 8 位日期、连字符和 3 位序号”，就需要同时检查多个位置和字符数量。正则表达式适合描述这类字符串模式：

```js
const applicationIdPattern = /^REQ-\d{8}-\d{3}$/;

console.log(applicationIdPattern.test("REQ-20260820-001")); // true
console.log(applicationIdPattern.test("REQ-20260820-01")); // false
```

`test(text)` 是正则对象最常用的判断方法。参数 `text` 是要检查的字符串，找到符合规则的内容时返回 `true`，没有找到时返回 `false`。这里同时使用 `^` 和 `$`，所以检查的是整个申请编号。

### 1.3 适用范围和不适用范围

| 场景 | 示例 |
| --- | --- |
| 完整格式校验 | 账号是否只包含允许的字符，长度是否为 4～20 |
| 查找 | 文本中是否包含申请编号 |
| 提取 | 从日志中取得所有申请编号 |
| 替换 | 把连续空白统一成一个空格 |
| 拆分 | 按逗号、分号或空白拆分输入 |

正则适合处理有明确文字规律的内容，不适合代替所有业务判断。例如：

- 正则可以检查日期是不是 `YYYY-MM-DD` 的外形，但不能单独证明 2 月 30 日真实存在。
- 正则可以检查密码长度和字符种类，但不能判断密码是否泄露。
- 正则可以检查员工编号格式，但不能判断编号是否存在或当前用户是否有权访问。

## 2. 正则表达式的基本写法

### 2.1 正则字面量

JavaScript 中最常用的写法是把规则写在两个 `/` 之间：

```js
const pattern = /^EMP-\d{5}$/;
```

这条正则可以拆成：

```text
/ ^ EMP- \d{5} $ /
  │ │     │    │
  │ │     │    └─ 字符串结束
  │ │     └────── 5 位数字
  │ └──────────── 固定文字 EMP-
  └────────────── 字符串开始
```

最外层的 `/.../` 是 JavaScript 正则字面量的边界，不是要匹配的斜杠字符。

## 3. 匹配普通字符和特殊字符

### 3.1 普通字符按原样匹配

```js
const pattern = /pending/;

console.log(pattern.test("status=pending")); // true
console.log(pattern.test("status=approved")); // false
```

字母和数字大多表示它们本身。默认情况下英文字母区分大小写。

### 3.2 点号 `.` 表示任意字符

```js
console.log(/A.C/.test("ABC")); // true
console.log(/A.C/.test("A-C")); // true
```

`.` 通常匹配除换行符之外的任意单个字符。如果要匹配真正的点号，需要写 `\.`：

```js
console.log(/example\.com/.test("example.com")); // true
console.log(/example\.com/.test("example-com")); // false
```

### 3.3 需要转义的常见字符

下列字符在正则中通常具有特殊含义：

```text
\  /  .  ^  $  *  +  ?  (  )  [  ]  {  }  |
```

要匹配这些字符本身，通常在前面写反斜杠。例如 `\.` 匹配点号，`\+` 匹配加号。正则字面量中要匹配 `/` 时写成 `\/`。

## 4. 字符类型和字符组

### 4.1 常用字符类型

| 写法 | 含义 | 示例 |
| --- | --- | --- |
| `\d` | 一位数字，等同于 `[0-9]` | `/\d/` 匹配 `8` |
| `\D` | 一位非数字字符 | `/\D/` 匹配 `A` |
| `\s` | 一个空白字符 | 空格、制表符、换行符 |
| `\S` | 一个非空白字符 | 字母、数字等 |
| `\w` | 英文字母、数字或下划线一类的单词字符 | `A`、`8`、`_` |
| `\W` | 非 `\w` 字符 | `-`、空格、日文等 |

不要用 `\w` 校验日文姓名。它不能表示“所有语言中的文字”，姓名规则也不应简单限制为英文字母、数字和下划线。

### 4.2 字符组 `[]`

字符组表示“当前位置可以是其中任意一个字符”：

```js
console.log(/[ABC]/.test("B")); // true
console.log(/[ABC]/.test("D")); // false
```

范围写法：

```js
const uppercaseLetter = /[A-Z]/;
const lowercaseLetter = /[a-z]/;
const digit = /[0-9]/;
```

账号允许小写字母、数字、点、下划线和连字符，可以写：

```js
const allowedAccountCharacter = /[a-z0-9._-]/;
```

字符组中的 `.` 通常表示普通点号，不需要写成 `\.`。连字符放在末尾可以减少它被误解为范围连接符的可能。

### 4.3 排除字符组 `[^]`

字符组开头的 `^` 表示“不是其中的字符”：

```js
const nonDigit = /[^0-9]/;

console.log(nonDigit.test("123A")); // true
console.log(nonDigit.test("1234")); // false
```

注意位置不同，含义不同：`/^EMP/` 中的 `^` 表示字符串开头，`/[^0-9]/` 中的 `^` 表示排除字符。

## 5. 控制字符出现次数

### 5.1 常用数量词

数量词控制它前面的字符、字符组或分组出现多少次。

| 写法 | 出现次数 | 示例 |
| --- | --- | --- |
| `?` | 0 次或 1 次 | `/https?/` 匹配 `http`、`https` |
| `*` | 0 次或多次 | `/\d*/` 可以匹配空字符串或多位数字 |
| `+` | 1 次或多次 | `/\d+/` 至少需要一位数字 |
| `{n}` | 恰好 n 次 | `/\d{5}/` 匹配 5 位数字 |
| `{n,}` | 至少 n 次 | `/\d{2,}/` 至少 2 位数字 |
| `{n,m}` | n～m 次 | `/[a-z]{4,20}/` 匹配 4～20 个小写字母 |

### 5.2 数量词只控制前一项

```js
const wrongPattern = /EMP-\d{5}/;
```

`{5}` 只控制前面的 `\d`，不是控制整个 `EMP-\d`。需要把多个字符作为整体重复时，要使用分组。

### 5.3 `*` 和 `+` 的区别

```js
console.log(/^\d*$/.test("")); // true
console.log(/^\d+$/.test("")); // false
```

`*` 允许出现 0 次，所以空字符串也可能通过。要求至少输入一个字符时，通常使用 `+` 或明确的 `{n,m}`。

## 6. 限制匹配位置

### 6.1 `^` 和 `$` 检查完整字符串

```js
const loosePattern = /EMP-\d{5}/;
const strictPattern = /^EMP-\d{5}$/;

console.log(loosePattern.test("XX-EMP-00001-YY")); // true
console.log(strictPattern.test("XX-EMP-00001-YY")); // false
```

- `^`：字符串开始位置。
- `$`：字符串结束位置。

格式校验通常要求整个输入都符合规则，因此常同时使用 `^` 和 `$`。文本搜索只需要寻找其中一段时，不一定使用它们。

### 6.2 单词边界 `\b`

```js
const pattern = /\bpending\b/;

console.log(pattern.test("status pending today")); // true
console.log(pattern.test("pendingItem")); // false
```

`\b` 表示单词字符与非单词字符之间的边界，常用于英文代码值或单词搜索。它不是通用的日文分词工具。

## 7. 分组和选择

### 7.1 使用 `()` 把规则组合起来

```js
const halfDayPattern = /^(morning|afternoon)$/;

console.log(halfDayPattern.test("morning")); // true
console.log(halfDayPattern.test("paid")); // false
```

圆括号把内容组成一组，`|` 表示“或者”。这里要求完整值只能是 `morning` 或 `afternoon`。

### 7.2 分组控制重复范围

```js
const repeatedPattern = /^(EMP-){2}\d{5}$/;

console.log(repeatedPattern.test("EMP-EMP-00001")); // true
```

`{2}` 控制整个 `(EMP-)` 分组重复两次。这个示例用于观察分组范围，不是项目中的员工编号规格。

### 7.3 捕获分组

普通圆括号除了组合规则，还会保存匹配到的部分：

```js
const pattern = /^(REQ)-(\d{8})-(\d{3})$/;
const result = pattern.exec("REQ-20260820-001");

if (result !== null) {
  console.log(result[0]); // 完整匹配
  console.log(result[1]); // REQ
  console.log(result[2]); // 20260820
  console.log(result[3]); // 001
}
```

`exec(text)` 返回包含完整匹配和各捕获分组的数组；没有匹配时返回 `null`。只想组合而不需要保存分组时，可以使用非捕获分组 `(?:...)`，当前阶段能看懂即可。

## 8. 正则标志

### 8.1 标志写在哪里

标志写在正则字面量最后一个 `/` 的后面：

```js
const pattern = /pending/gi;
```

### 8.2 常用标志

| 标志 | 名称 | 作用 | 常见场景 |
| --- | --- | --- | --- |
| `i` | ignore case | 忽略英文字母大小写 | 不区分 `pending` 与 `PENDING` |
| `g` | global | 查找全部匹配，而不是只处理第一个 | 全部替换、提取多个编号 |
| `m` | multiline | 让 `^`、`$` 对每一行生效 | 多行文本逐行检查 |
| `u` | unicode | 按 Unicode 模式解释规则 | 处理 Unicode 字符时使用 |

```js
console.log(/pending/i.test("PENDING")); // true
console.log("pending pending".replace(/pending/g, "approved"));
// approved approved
```

这里第一次使用的 `replace(pattern, replacement)` 在字符串中查找 `pattern`，并用 `replacement` 替换匹配内容，返回新的字符串。原字符串不会被修改；传入带 `g` 的正则时会替换全部匹配。本章第 9 节会统一比较它与其他正则方法。

标志可以组合，但不要因为“可能有用”就全部添加。格式校验通常不需要 `g`。

### 8.3 使用带 `g` 的正则进行 `test()` 时要谨慎

带 `g` 或 `y` 标志的正则对象会记录上次匹配位置，重复调用同一个对象的 `test()` 可能得到交替结果：

```js
const pattern = /EMP/g;

console.log(pattern.test("EMP")); // true
console.log(pattern.test("EMP")); // false
```

因此，用于表单格式校验的正则通常不要添加 `g`。`g` 主要用于查找或替换全部匹配。

## 9. JavaScript 中使用正则的常用方法

学会阅读正则规则后，还需要选择合适的方法执行它。方法主要分为两组：正则对象提供的 `test()`、`exec()`，以及字符串提供的 `match()`、`search()`、`replace()` 等方法。

### 9.1 方法总览

| 调用方 | 方法 | 主要作用 | 没有匹配时 | 常见用途 |
| --- | --- | --- | --- | --- |
| `RegExp` | `test(text)` | 判断是否存在匹配 | 返回 `false` | 格式校验 |
| `RegExp` | `exec(text)` | 取得一次匹配及捕获分组 | 返回 `null` | 提取分组 |
| `String` | `match(regexp)` | 取得一次匹配，或在 `g` 模式下取得全部完整匹配 | 返回 `null` | 查找和提取 |
| `String` | `matchAll(regexp)` | 取得全部匹配及各自捕获分组 | 返回空的可迭代结果 | 多次分组提取 |
| `String` | `search(regexp)` | 查找第一个匹配位置 | 返回 `-1` | 查找位置 |
| `String` | `replace(pattern, replacement)` | 替换第一个或全部匹配 | 返回未改变的新字符串 | 文本清理 |
| `String` | `replaceAll(pattern, replacement)` | 替换全部匹配 | 返回未改变的新字符串 | 全部替换 |
| `String` | `split(separator)` | 按匹配规则拆分字符串 | 返回包含原字符串的数组 | 多分隔符拆分 |

### 9.2 参数和返回值

| 方法 | 主要参数 | 可接受的值 | 默认值或必填性 | 返回值 |
| --- | --- | --- | --- | --- |
| `test(text)` | `text` | 字符串或可转换为字符串的值 | 必填 | 布尔值 |
| `exec(text)` | `text` | 字符串或可转换为字符串的值 | 必填 | 匹配数组或 `null` |
| `match(regexp)` | `regexp` | 正则对象 | 必填 | 匹配数组或 `null` |
| `matchAll(regexp)` | `regexp` | 带 `g` 的正则对象 | 必填 | 可迭代对象 |
| `search(regexp)` | `regexp` | 正则对象 | 必填 | 索引或 `-1` |
| `replace(pattern, replacement)` | 匹配规则、替换内容 | 字符串或正则；替换字符串或函数 | 两项必填 | 新字符串 |
| `replaceAll(pattern, replacement)` | 匹配规则、替换内容 | 字符串或带 `g` 的正则；替换字符串或函数 | 两项必填 | 新字符串 |
| `split(separator, limit)` | 分隔规则、数量上限 | 字符串或正则；非负整数 | 分隔规则可选，`limit` 可选 | 字符串数组 |

### 9.3 如何选择

- 只判断格式是否符合：`test()`。
- 需要一个匹配结果和捕获分组：`exec()` 或不带 `g` 的 `match()`。
- 需要全部完整匹配：带 `g` 的 `match()`。
- 需要全部匹配及每次的分组：`matchAll()`。
- 只需要第一个匹配位置：`search()`。
- 需要修改文本：`replace()` 或 `replaceAll()`。
- 需要按规则拆成数组：`split()`。

本章后续小节会分别展示这些方法在格式校验、编号提取和文本清理中的写法。

### 观察同一规则怎样逐步收紧

用已学的 `test(字符串)` 验证下表。它返回是否匹配的布尔值，表中的每个规则都可单独赋给 `pattern` 再测试。

| 规则 | 新增限制 | 可匹配 | 不可匹配 |
| --- | --- | --- | --- |
| `/EMP/` | 含有固定文字 | XEMPY | ABC |
| `/^EMP-/` | 必须以 EMP- 开头 | EMP-X | XEMP- |
| `/^EMP-\d{5}/` | 后面连续五位数字 | EMP-00001X | EMP-12 |
| `/^EMP-\d{5}$/` | 五位数字后必须结束 | EMP-00001 | EMP-00001X |

只增加 `$` 就会改变对尾部多余字符的判断。编写校验时应同时测试正确输入、位数不足、位数过多和前后多余内容，不要只测试一个能通过的值。

## 10. 从业务规格写出正则

### 10.1 账号格式

规格：账号统一转成小写，只允许小写字母、数字、点、下划线和连字符，长度为 4～20。

```js
function isValidAccountId(value) {
  return /^[a-z0-9._-]{4,20}$/.test(value);
}

console.log(isValidAccountId("yamada.taro")); // true
console.log(isValidAccountId("abc")); // false
console.log(isValidAccountId("Yamada")); // false
console.log(isValidAccountId("yamada@taro")); // false
```

翻译过程：

1. 完整输入都要符合规则，所以使用 `^` 和 `$`。
2. 允许的字符放进 `[a-z0-9._-]`。
3. 长度 4～20 使用 `{4,20}`。

调用前先统一输入：

```js
const inputValue = "  Yamada.Taro  ";
const normalizedAccountId = inputValue.trim().toLowerCase();

if (!isValidAccountId(normalizedAccountId)) {
  console.log("账号格式不正确");
}
```

`trim()` 删除字符串首尾空白并返回新字符串，`toLowerCase()` 把英文字母转换为小写并返回新字符串。二者都不修改原字符串。账号先统一格式，再交给正则校验，可以避免大小写和首尾空格造成不一致。

正则只负责格式。账号是否重复仍要查询已有用户数组或后端数据。

### 10.2 员工编号格式

项目中的员工编号为 `EMP-` 加 5 位数字：

```js
function isValidEmployeeNumber(value) {
  return /^EMP-\d{5}$/.test(value);
}

console.log(isValidEmployeeNumber("EMP-00001")); // true
console.log(isValidEmployeeNumber("EMP-001")); // false
```

### 10.3 申请编号格式

```js
function isValidApplicationId(value) {
  return /^REQ-\d{8}-\d{3}$/.test(value);
}

console.log(isValidApplicationId("REQ-20260820-001")); // true
```

这只能证明编号外形正确，不能证明中间八位是合法日期，也不能证明该申请真实存在。

### 10.4 基础邮箱格式

```js
function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

console.log(isValidEmail("test@example.com")); // true
console.log(isValidEmail("test example.com")); // false
```

这个简化规则只检查：

- `@` 前后有内容。
- 不包含空白。
- `@` 后面存在点号和后续内容。

真实邮箱地址规则复杂，前端不应使用超长正则试图证明邮箱一定真实。最终仍需要后端校验，必要时通过验证邮件确认邮箱归属。

### 10.5 密码不一定要写成一个复杂正则

如果规格只是长度 8～32，普通判断更清楚：

```js
function isValidPasswordLength(password) {
  return password.length >= 8 && password.length <= 32;
}
```

如果还要求包含数字，可以组合简单判断：

```js
function isValidPassword(password) {
  const hasValidLength = password.length >= 8 && password.length <= 32;
  const hasDigit = /\d/.test(password);

  return hasValidLength && hasDigit;
}
```

多个业务条件分开判断，能为用户显示更准确的错误原因。不要为了“只写一条正则”而牺牲可读性和可维护性。

## 11. 与表单校验结合

### 11.1 配套 HTML

```html
<form id="registerForm" novalidate>
  <label for="accountId">账号</label>
  <input
    id="accountId"
    name="accountId"
    type="text"
    aria-describedby="accountIdError"
  >
  <p id="accountIdError"></p>

  <button type="submit">注册</button>
</form>
<script src="regex-demo.js" defer></script>
```

### 11.2 完整 JavaScript

```js
const registerForm = document.querySelector("#registerForm");
const accountIdInput = document.querySelector("#accountId");
const accountIdError = document.querySelector("#accountIdError");

function isValidAccountId(value) {
  return /^[a-z0-9._-]{4,20}$/.test(value);
}

registerForm.addEventListener("submit", (event) => {
  event.preventDefault();

  const accountId = accountIdInput.value.trim().toLowerCase();

  if (accountId === "") {
    accountIdError.textContent = "账号为必填项";
    accountIdInput.setAttribute("aria-invalid", "true");
    accountIdInput.focus();
    return;
  }

  if (!isValidAccountId(accountId)) {
    accountIdError.textContent =
      "账号只能使用小写字母、数字、点、下划线和连字符，长度为4～20";
    accountIdInput.setAttribute("aria-invalid", "true");
    accountIdInput.focus();
    return;
  }

  accountIdError.textContent = "";
  accountIdInput.setAttribute("aria-invalid", "false");
  accountIdInput.value = accountId;
  console.log("账号格式校验通过");
});
```

这段代码复用了 DOM 与事件章节的方法：

- `querySelector(selector)` 接收 CSS 选择器，返回第一个匹配元素；找不到时返回 `null`。本例的选择器都与上方 HTML 对应。
- `addEventListener(type, listener)` 接收事件类型和处理函数，用于注册事件监听器，返回 `undefined`。
- `preventDefault()` 取消表单提交的默认刷新行为，不需要参数，返回 `undefined`。
- `setAttribute(name, value)` 设置元素属性；这里更新 `aria-invalid`，两个参数必填，返回 `undefined`。
- `focus()` 不需要参数，把输入焦点移动到当前元素，返回 `undefined`。

这个示例先处理空值，再检查格式。空值、长度错误和非法字符属于不同问题，实际项目可以根据规格继续拆分提示。

### 11.3 HTML `pattern` 和 JavaScript 正则

HTML 输入框也可以使用 `pattern` 属性：

```html
<input
  id="accountId"
  name="accountId"
  type="text"
  pattern="[a-z0-9._\-]{4,20}"
  required
>
```

`pattern` 的值不写正则字面量两侧的 `/`。浏览器会按完整输入进行检查，因此通常也不需要手工添加 `^` 和 `$`。

HTML 约束校验可以改善用户体验，但不能代替 JavaScript 业务处理和后端校验。前端代码、后端规则和规格书应保持一致。

## 12. 使用正则查找、提取和替换

### 12.1 `search()` 查找第一个位置

```js
const message = "申请编号：REQ-20260820-001";
const index = message.search(/REQ-\d{8}-\d{3}/);

console.log(index); // 5
```

`search(regexp)` 返回第一个匹配位置的索引；没有匹配时返回 `-1`。

### 12.2 `match()` 提取一个匹配

```js
const message = "申请编号：REQ-20260820-001";
const result = message.match(/REQ-\d{8}-\d{3}/);

console.log(result);
```

没有 `g` 标志时，`match(regexp)` 返回包含完整匹配和捕获分组的数组；没有匹配时返回 `null`。

### 12.3 `match()` 和 `matchAll()` 提取全部匹配

```js
const message = "REQ-20260820-001, REQ-20260820-002";
const result = message.match(/REQ-\d{8}-\d{3}/g);
const applicationIds = result === null ? [] : result;

console.log(applicationIds);
```

带 `g` 时，`match()` 返回所有完整匹配组成的数组，但不提供每一项的捕获分组详情。

需要取得每次匹配的捕获分组时，可以使用 `matchAll()`：

```js
const message = "REQ-20260820-001, REQ-20260821-002";
const pattern = /REQ-(\d{8})-(\d{3})/g;
const matches = message.matchAll(pattern);

for (const match of matches) {
  console.log(match[0], match[1], match[2]);
}
```

`matchAll(regexp)` 要求正则带 `g` 标志，返回可迭代对象，即可以被for...of逐项读取的结果。每次取出的match是一个匹配数组，下标0是完整匹配，下标1、2是捕获分组；这里不需要先转换为数组。

### 12.4 `replace()` 替换匹配内容

```js
const message = "status=pending, previous=pending";

console.log(message.replace(/pending/, "approved"));
// status=approved, previous=pending

console.log(message.replace(/pending/g, "approved"));
// status=approved, previous=approved
```

`replace(pattern, replacement)` 返回替换后的新字符串，不修改原字符串。正则没有 `g` 时只替换第一个，带 `g` 时替换全部。

### 12.5 `split()` 按多个分隔符拆分

```js
const inputText = "paid, morning；afternoon";
const values = inputText.split(/[,；]\s*/);

console.log(values);
```

`split(separator)` 返回拆分后的数组。这里的分隔符允许英文逗号或中文分号，后面还可以有任意数量的空白。

## 13. 动态规则：RegExp 构造方法

规则固定时优先使用正则字面量。规则需要根据变量动态生成时，可以使用 `RegExp`：

```js
const prefix = "EMP";
const pattern = new RegExp(`^${prefix}-\\d{5}$`);

console.log(pattern.test("EMP-00001")); // true
```

`new RegExp(pattern, flags)` 创建正则对象：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `pattern` | 正则对象或表示规则的字符串 | 必填 | 要匹配的规则 |
| `flags` | 如 `"i"`、`"g"`、`"m"` 的字符串或省略 | 可选，默认无标志 | 修改匹配方式 |

字符串中的反斜杠本身也需要转义，所以正则中的 `\d` 在字符串里要写成 `"\\d"`。这使构造写法更难阅读，不需要动态规则时不要使用它。

如果动态内容来自用户输入，还必须先转义其中的正则特殊字符，否则内容可能改变原规则。本课程主线不要求自行拼接用户提供的正则。

## 14. 常见错误与安全边界

### 14.1 忘记完整匹配

校验格式时漏掉 `^`、`$`，可能让前后带有多余字符的输入通过。

### 14.2 忘记转义点号

邮箱规则中的 `.` 如果写成任意字符，会接受本不应接受的内容。匹配真实点号时使用 `\.`。

### 14.3 在校验正则中使用 `g`

复用带 `g` 的正则调用 `test()` 会受到上次匹配位置影响。校验完整输入时通常不添加 `g`。

### 14.4 把格式正确当成业务正确

员工编号、申请编号和邮箱通过正则，只代表外形符合要求。存在性、唯一性、权限和真实性必须另外验证。

### 14.5 正则过于复杂

复杂正则很难阅读、修改和排错，有些包含大量嵌套重复的规则还可能在特殊输入下消耗过多时间。

- 优先使用长度限制和简单规则。
- 能用清楚的字符串方法或多个条件表达式完成时，不强行合并成一条正则。
- 不直接执行用户提供的正则规则。
- 前端限制不能代替后端长度限制和格式校验。

## 15. 本章综合练习

### 15.1 初始状态

在注册页面中准备账号、员工编号和邮箱输入框，以及各自对应的错误提示元素。JavaScript 使用外部文件并通过 `defer` 加载。

### 15.2 任务要求

1. 账号统一执行 `trim()` 和 `toLowerCase()`，再按 `[a-z0-9._-]{4,20}` 校验。
2. 员工编号必须符合 `EMP-00001` 格式。
3. 邮箱使用本章的基础规则校验。
4. 分别处理空值、格式错误和重复账号，不把所有错误合并成同一提示。
5. 从一段测试日志中提取全部 `REQ-YYYYMMDD-NNN` 形式的申请编号。
6. 把日志中的连续空白替换为一个半角空格。
7. 为每条正则准备通过和不通过的测试数据。

### 15.3 测试数据

| 功能 | 输入 | 预期结果 |
| --- | --- | --- |
| 账号 | `yamada.taro` | 通过 |
| 账号 | `abc` | 长度不足 |
| 账号 | `yamada@taro` | 包含非法字符 |
| 员工编号 | `EMP-00001` | 通过 |
| 员工编号 | `E001` | 格式错误 |
| 邮箱 | `test@example.com` | 通过 |
| 邮箱 | `test example.com` | 格式错误 |
| 申请编号 | `REQ-20260820-001` | 格式通过，但仍需业务验证 |

### 15.4 完成标准

- 能逐段说明每条校验正则的含义。
- 空值和格式错误显示不同提示，并聚焦第一个错误字段。
- 正确输入可以通过，边界和错误输入不会误判。
- 提取和替换结果与预期一致。
- 没有使用一条难以说明的超长正则代替全部业务判断。
- 控制台没有未处理异常。
