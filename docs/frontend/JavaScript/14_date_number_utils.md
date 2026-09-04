# 第 14 章 日期、数字与常用工具函数

业务程序经常需要处理“日期”“时间点”“天数”“编号”和“显示文字”。这些值看起来只是字符串或数字，但如果忽略时区、无效输入和编号规则，就容易产生难以发现的错误。

完成本章后，你应当能够：

- 区分业务日期、带时区的时间点和时间戳。
- 创建并检查 `Date` 对象，理解本地时间与 UTC 的区别。
- 安全计算两个 `YYYY-MM-DD` 业务日期之间的自然日数。
- 使用手工方式和 `Intl.DateTimeFormat` 格式化日期。
- 判断数字是否有效，并使用项目需要的 `Math` 方法处理数字。
- 按练习规格生成申请编号，并说明前端编号的边界。
- 编写不依赖 DOM 的可复用工具函数，并验证边界情况。

## 1. 为什么业务数据需要转换

### 1.1 保存值和显示值可能不同

程序中通常保存稳定、便于计算的数据，页面上则显示用户容易理解的文字。

| 数据用途 | 保存值 | 显示值 |
| --- | --- | --- |
| 请假类型 | `paid` | `有給休暇` |
| 申请日期 | `2026-09-01` | `2026年09月01日` |
| 请假天数 | `2` | `2日` |
| 申请状态 | `pending` | `申請中` |

不要为了显示方便，过早把数字变成带单位的字符串。比如 `2` 可以继续参与计算，`"2日"` 则需要再次转换。

### 1.2 转换逻辑适合封装成函数

```js
function getLeaveTypeLabel(type) {
  const labels = {
    paid: "有給休暇",
    morning: "午前休",
    afternoon: "午後休",
  };

  const label = labels[type];
  return label === undefined || label === null ? "不明" : label;
}

console.log(getLeaveTypeLabel("paid")); // 有給休暇
```

`getLeaveTypeLabel(type)` 接收请假类型代码，返回对应显示文字；没有匹配项时返回 `"不明"`。把转换规则放进函数后，列表页、确认页和完成页可以复用同一规则。

## 2. 日期与时间基础

### 2.1 先区分日期和时间点

下面两类数据含义不同：

```text
2026-09-01
2026-09-01T10:30:00+09:00
```

- `2026-09-01` 是业务日期，只表示日历上的某一天，例如请假开始日。
- `2026-09-01T10:30:00+09:00` 是带时区偏移的时间点，可以表示申请提交的具体时刻。

生日、请假日期通常不需要时分秒；创建时间、更新时间和日志时间通常需要明确时间点。二者不能随意混用。

可以先把日期时间理解成三层：

| 层次 | 示例 | 解决的问题 |
| --- | --- | --- |
| 业务日期 | `2026-09-01` | 日历上的哪一天 |
| 时间点 | 日本时间 `2026-09-01 10:30` | 某件事究竟在什么时候发生 |
| 显示文字 | `2026年9月1日 10:30` | 按用户习惯怎样显示 |

同一个时间点可以在日本显示为 10:30，在 UTC 显示为 01:30。显示文字不同，但事件没有变成两次。

### 2.2 `Date` 对象和时间戳

`Date` 是 JavaScript 用来表示时间点的内置对象。它内部记录的是一个毫秒时间戳；控制台或格式化方法再根据时区把这个时间点显示成人类可读的年月日和时分秒。

下面先用固定时间实验，保证可以核对结果：

```js
const submittedAt = new Date("2026-09-01T10:30:00+09:00");

console.log(submittedAt.getTime());       // 1788226200000
console.log(submittedAt.toISOString());  // 2026-09-01T01:30:00.000Z
```

`getTime()` 返回时间戳，即从 `1970-01-01 00:00:00 UTC` 到目标时间点经过的毫秒数。时间戳只是一个计数值，没有“日本格式”或“中文格式”，适合比较、排序和计算，不适合直接显示给用户。

这里的 `1788226200000` 是 13 位的毫秒时间戳。部分后端系统使用 10 位的秒时间戳，双方必须先约定单位：

```js
const milliseconds = 1788226200000;
const seconds = milliseconds / 1000;

console.log(seconds); // 1788226200
```

JavaScript 的 `Date` 构造参数和 `getTime()` 默认使用毫秒。误把秒传给 `new Date()`，结果会落到 1970 年附近。

`new Date()` 创建当前时间的 `Date` 对象，`Date.now()` 直接取得当前毫秒时间戳：

```js
const now = new Date();
const timestamp = Date.now();

console.log(now);       // 示例：Fri Sep 04 2026 09:15:30 GMT+0900 ...
console.log(timestamp); // 示例：1788480930000
```

这两行的实际结果取决于运行时刻和电脑时区，不会固定等于注释中的示例值。需要对照固定答案时，应像前一个示例那样使用固定输入。

### 2.3 创建 `Date` 对象的常见方式

```js
const currentTime = new Date();
const submittedTime = new Date("2026-09-01T10:30:00+09:00");
const restoredTime = new Date(1788226200000);
const localTime = new Date(2026, 8, 1, 10, 30, 0);

console.log(submittedTime.toISOString()); // 2026-09-01T01:30:00.000Z
console.log(restoredTime.toISOString());  // 2026-09-01T01:30:00.000Z
console.log(localTime.getMonth());         // 8
```

| 写法 | 可接受的值 | 默认值或必填性 | 结果 |
| --- | --- | --- | --- |
| `new Date()` | 无 | 无参数 | 当前时间 |
| `new Date(dateText)` | 可解析的日期时间字符串 | 字符串必填 | 字符串表示的时间 |
| `new Date(timestamp)` | 毫秒时间戳 | 数字必填 | 时间戳对应的时间 |
| `new Date(y, m, d, h, min, s)` | 数字 | 年和月必填，其余有默认值 | 使用本地时区创建时间 |

`submittedTime` 和 `restoredTime` 来自不同输入，却表示同一个时间点，所以转成 ISO 字符串后完全相同。

数值构造方式中的月份从 `0` 开始，因此 `8` 表示 9 月。这是常见错误来源。`localTime` 使用运行环境的本地时区创建，因此它代表的绝对时间会受电脑时区影响；适合创建“当前电脑所在地的 2026 年 9 月 1 日 10:30”，不适合冒充固定 UTC 时间。

日期字符串尽量使用明确格式：

- 时间点使用带偏移的 ISO 形式，例如 `2026-09-01T10:30:00+09:00`。
- UTC 时间点使用结尾带 `Z` 的形式，例如 `2026-09-01T01:30:00.000Z`。
- 纯业务日期保持 `YYYY-MM-DD`，不要随意补时分秒后当作时间点。
- 避免依赖 `2026/09/01 10:30` 等非标准字符串的自动解析，不同运行环境可能产生不同结果。

### 2.4 读取本地时间

```js
const date = new Date(2026, 8, 1, 10, 30, 0);

console.log(date.getFullYear()); // 2026
console.log(date.getMonth());    // 8，表示 9 月
console.log(date.getDate());     // 1
console.log(date.getHours());    // 10
console.log(date.getMinutes());  // 30
console.log(date.getDay());      // 2，表示星期二
```

这些 `get...()` 方法按电脑或浏览器的本地时区读取：

| 方法 | 返回范围 | 当前示例结果 | 含义 |
| --- | --- | --- | --- |
| `getFullYear()` | 四位年份 | `2026` | 本地年份 |
| `getMonth()` | `0`～`11` | `8` | `0` 是 1 月，显示时通常加 1 |
| `getDate()` | `1`～`31` | `1` | 一个月中的第几日 |
| `getHours()` | `0`～`23` | `10` | 本地小时 |
| `getMinutes()` | `0`～`59` | `30` | 分钟 |
| `getDay()` | `0`～`6` | `2` | 星期日为 `0`，星期六为 `6` |

`getDate()` 是“几号”，`getDay()` 是“星期几”，名称相近但用途不同。

### 2.5 本地时间与 UTC

UTC（协调世界时）是全球统一的时间基准。日本标准时间 JST 是 `UTC+09:00`，表示日本时间比 UTC 快 9 小时。例如：

```text
日本时间：2026-09-01 10:30:00 +09:00
UTC 时间：2026-09-01 01:30:00 Z
```

两行表示同一瞬间。`+09:00` 是时区偏移，`Z` 表示偏移为 `+00:00` 的 UTC。UTC 不是某种“日常显示格式”，而是统一表达和交换时间点的基准。

同一个时间戳在世界各地代表同一个时间点，但用本地方法读取出的年月日和时分可能不同。

| 本地时间方法 | UTC 方法 |
| --- | --- |
| `getFullYear()` | `getUTCFullYear()` |
| `getMonth()` | `getUTCMonth()` |
| `getDate()` | `getUTCDate()` |
| `getHours()` | `getUTCHours()` |

`toISOString()` 把有效的 `Date` 转换成 UTC 的 ISO 字符串：

```js
const submittedAt = new Date("2026-09-01T10:30:00+09:00");
console.log(submittedAt.toISOString());
// 2026-09-01T01:30:00.000Z
```

`toISOString()` 返回符合 ISO 8601 形式的 UTC 字符串，固定使用 `YYYY-MM-DDTHH:mm:ss.sssZ` 结构：

```text
2026-09-01 T 01:30:00.000 Z
日期         时间（含毫秒） UTC
```

字母 `T` 分隔日期和时间，`.000` 是毫秒，结尾 `Z` 表示 UTC。原来的日本时间 10:30 和转换后的 UTC 01:30 表示同一个时间点。

可以同时观察同一对象的 UTC 值和指定时区显示值：

```js
const submittedAt = new Date("2026-09-01T10:30:00+09:00");

const utcFormatter = new Intl.DateTimeFormat("ja-JP", {
  dateStyle: "medium",
  timeStyle: "medium",
  timeZone: "UTC",
});

const tokyoFormatter = new Intl.DateTimeFormat("ja-JP", {
  dateStyle: "medium",
  timeStyle: "medium",
  timeZone: "Asia/Tokyo",
});

console.log(utcFormatter.format(submittedAt));
// 2026/09/01 1:30:00

console.log(tokyoFormatter.format(submittedAt));
// 2026/09/01 10:30:00
```

不同浏览器对分隔符和是否补零可能略有差异，但 UTC 与东京相差 9 小时这一含义不会改变。需要完全固定的文字格式时，使用后面介绍的手工格式化函数；需要符合地区习惯时，使用 `Intl.DateTimeFormat`。

### 2.6 项目中通常怎样保存和显示时间

一个常见处理流程是：

```text
用户操作时间
    ↓ new Date()
Date 对象
    ↓ toISOString()
UTC 的 ISO 字符串，用于接口传输或保存
    ↓ new Date(savedText)
恢复为 Date 对象
    ↓ Intl.DateTimeFormat
按用户所在时区显示
```

下面是完整的转换示例：

```js
const submittedAt = new Date("2026-09-01T10:30:00+09:00");
const savedText = submittedAt.toISOString();

console.log(savedText);
// 2026-09-01T01:30:00.000Z

const restoredDate = new Date(savedText);
const displayFormatter = new Intl.DateTimeFormat("ja-JP", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
  timeZone: "Asia/Tokyo",
});

console.log(displayFormatter.format(restoredDate));
// 2026/09/01 10:30:00
```

这里分别保留了两种值：

- `savedText` 是稳定的 UTC 时间点，适合与后端约定后传输或保存。
- 格式化结果是给用户看的文字，可以随语言、地区和时区改变。

不要把格式化后的 `2026/09/01 10:30:00` 再当作可靠输入交给 `new Date()`，也不要用它进行排序。排序和计算保留原始时间点，显示时才格式化。

对于请假日、生日等纯业务日期，不需要执行这套时区转换，继续保存 `YYYY-MM-DD` 即可。是否保存 UTC、带偏移字符串或其他后端类型，应以接口规格为准。

### 2.7 判断无效日期

```js
const date = new Date("not-a-date");

console.log(date); // Invalid Date
console.log(Number.isNaN(date.getTime())); // true
```

第一行在控制台显示 `Invalid Date`，第二行显示 `true`。即使日期无效，变量中仍然存在一个 `Date` 对象。判断方法是读取 `getTime()`，再使用 `Number.isNaN()` 检查结果。

## 3. 安全处理 `YYYY-MM-DD` 业务日期

### 3.1 为什么不能随意解析业务日期

HTML 的 `<input type="date">` 在有值时通常提供 `YYYY-MM-DD` 字符串。只包含日期的 ISO 字符串在 `new Date("2026-09-01")` 中按 UTC 解析；随后使用本地时间方法时，部分时区可能显示成前一天。

对于请假日期这类“日历日期”，更稳妥的做法是拆分年月日，并使用 UTC 统一计算自然日差。

### 3.2 把日期文本转换为 UTC 时间戳

下面先用 `Number.isInteger(value)` 检查年月日是否都是整数：参数可以是任意值，是数字且为整数时返回true，否则返回false，例如 `Number.isInteger(12)` 为true、`Number.isInteger(12.5)` 为false。

先观察合法日期的转换过程。下面是独立实验，不代替后面的完整校验函数：

```js
const dateText = "2026-09-01";
const parts = dateText.split("-");
console.log(parts); // ["2026", "09", "01"]
const year = Number(parts[0]);
const month = Number(parts[1]);
const day = Number(parts[2]);
console.log(year, month, day); // 2026 9 1
const timestamp = Date.UTC(year, month - 1, day);
const date = new Date(timestamp);
console.log(date.toISOString()); // 2026-09-01T00:00:00.000Z
console.log(date.getUTCMonth() + 1); // 9
```

`Date.UTC(年, 月下标, 日)` 按 UTC 计算时间戳，月份下标从 0 开始，因此输入月份 9 要传入 8。这里传入数字年月日，结果是毫秒数；再用 `new Date(timestamp)` 读取其 UTC 年月日。

把日期改为 `"2026-02-30"` 再观察：Date 会自动调整到三月，而不是直接拒绝。所以完整函数必须增加四道检查：输入类型 → 文本结构 → 是否整数 → 转换后的年月日是否与原输入一致。每道检查失败都返回 `null`，不继续计算。

下面将这些步骤合并为可复用函数：

```js
function parseDateTextToUtc(dateText) {
  if (typeof dateText !== "string") {
    return null;
  }

  const parts = dateText.split("-");

  if (
    parts.length !== 3 ||
    parts[0].length !== 4 ||
    parts[1].length !== 2 ||
    parts[2].length !== 2
  ) {
    return null;
  }

  const year = Number(parts[0]);
  const month = Number(parts[1]);
  const day = Number(parts[2]);

  if (
    !Number.isInteger(year) ||
    !Number.isInteger(month) ||
    !Number.isInteger(day)
  ) {
    return null;
  }

  const timestamp = Date.UTC(year, month - 1, day);
  const date = new Date(timestamp);

  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return null;
  }

  return timestamp;
}

console.log(parseDateTextToUtc("2026-09-01")); // 1788220800000
console.log(parseDateTextToUtc("2026-02-30")); // null
```

`parseDateTextToUtc(dateText)` 接收严格的 `YYYY-MM-DD` 字符串，合法时返回该 UTC 日期的毫秒时间戳，不合法时返回 `null`。

不能只依赖 `Date.UTC()` 判断日期是否合法，因为 JavaScript 可能自动把 2 月 30 日调整到 3 月。示例重新比较年月日，避免接受被自动调整的日期。

先运行完整函数的两个调用，分别确认合法日期得到数字、2 月 30 日得到 `null`。下一节代码接在该函数后面：日数计算依赖这个校验结果，不能只复制后面的计算函数。

### 3.3 计算包含首尾日期的自然日数

```js
const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

function calculateLeaveDays(startDateText, endDateText) {
  const startTimestamp = parseDateTextToUtc(startDateText);
  const endTimestamp = parseDateTextToUtc(endDateText);

  if (startTimestamp === null || endTimestamp === null) {
    return null;
  }

  if (endTimestamp < startTimestamp) {
    return null;
  }

  return (endTimestamp - startTimestamp) / MILLISECONDS_PER_DAY + 1;
}

console.log(calculateLeaveDays("2026-09-01", "2026-09-03")); // 3
console.log(calculateLeaveDays("2026-09-01", "2026-09-01")); // 1
console.log(calculateLeaveDays("2026-09-03", "2026-09-01")); // null
```

这个函数按照“开始日和结束日都计算”的规格返回自然日数。使用 UTC 日期进行相减，可以避免夏令时造成一天不是 24 小时的问题。

当前练习项目采用以下简化规则：

- 全天休假按包含首尾日期的自然日计算。
- 半日休假固定为 `0.5` 日，并且开始日与结束日必须相同。
- 不排除周末和日本节假日。
- 无效日期或结束日早于开始日时返回 `null`，由页面代码显示错误。

真实考勤系统通常由后端按照公司制度、工作日历和节假日数据计算，不能只依靠前端结果。

## 4. 日期格式化

### 4.1 手工格式化业务日期

业务日期已经是稳定的 `YYYY-MM-DD` 字符串时，可以验证后直接组合，避免不必要的时区转换：

```js
function formatBusinessDate(dateText) {
  const timestamp = parseDateTextToUtc(dateText);

  if (timestamp === null) {
    return "日期不明";
  }

  const date = new Date(timestamp);
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");

  return `${year}年${month}月${day}日`;
}

console.log(formatBusinessDate("2026-09-01"));
// 2026年09月01日
```

`padStart(targetLength, padText)` 在字符串长度不足时从开头补字符。这里目标长度是 `2`，填充文字是 `"0"`，返回补齐后的新字符串。

### 4.2 使用 `Intl.DateTimeFormat` 本地化显示

`Intl.DateTimeFormat` 根据语言和格式选项显示日期，适合国际化页面：

```js
const formatter = new Intl.DateTimeFormat("ja-JP", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  timeZone: "Asia/Tokyo",
});

const submittedAt = new Date("2026-09-01T10:30:00+09:00");
console.log(formatter.format(submittedAt));
// 2026/09/01
```

| 构造参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `locales` | 如 `"ja-JP"`、`"zh-CN"`、语言数组或省略 | 可选，默认使用运行环境语言 | 指定显示语言和地区习惯 |
| `options` | 配置对象或省略 | 可选，使用默认日期格式 | 指定年月日、时区等显示方式 |

常用选项值：

| 选项 | 常用值 | 作用 |
| --- | --- | --- |
| `year` | `"numeric"`、`"2-digit"` | 年份显示方式 |
| `month` | `"numeric"`、`"2-digit"`、`"long"`、`"short"` | 月份显示方式 |
| `day` | `"numeric"`、`"2-digit"` | 日期显示方式 |
| `weekday` | `"long"`、`"short"`、`"narrow"` | 星期显示方式 |
| `timeZone` | IANA 时区名，如 `"Asia/Tokyo"`、`"UTC"` | 按哪个时区显示 |

`format(date)` 接收有效的 `Date` 或时间戳，返回格式化字符串。格式化结果用于显示，不要再用它进行日期计算或接口传输。

多个位置使用同一格式时，集中创建 `Intl.DateTimeFormat` 对象更容易统一规则。

如果页面需要显示日期和时间，可以增加时分秒选项：

```js
const dateTimeFormatter = new Intl.DateTimeFormat("ja-JP", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
  timeZone: "Asia/Tokyo",
});

const submittedAt = new Date("2026-09-01T10:30:00+09:00");
console.log(dateTimeFormatter.format(submittedAt));
// 2026/09/01 10:30:00
```

`hour12: false` 表示使用 24 小时制；设为 `true` 时会使用上午、下午或 AM、PM 等地区化表示。浏览器可能使用不同的空格或分隔符，因此不要拿本地化结果作为固定业务代码或接口值。

### 4.3 `toLocaleDateString()` 和 `toLocaleString()`

`Date` 对象也提供快捷的本地化方法：

```js
const submittedAt = new Date("2026-09-01T10:30:00+09:00");

console.log(
  submittedAt.toLocaleDateString("ja-JP", {
    timeZone: "Asia/Tokyo",
  }),
);
// 2026/9/1

console.log(
  submittedAt.toLocaleString("ja-JP", {
    timeZone: "Asia/Tokyo",
  }),
);
// 2026/9/1 10:30:00
```

- `toLocaleDateString(locales, options)` 主要显示日期。
- `toLocaleString(locales, options)` 可以显示日期和时间。
- 两者都返回显示字符串，不修改原 `Date` 对象。

它们的 `locales` 和 `options` 与 `Intl.DateTimeFormat` 的用途相近。只格式化一两个位置时可以使用快捷方法；多个位置采用相同格式时，复用一个 `Intl.DateTimeFormat` 更容易保持一致。

### 4.4 怎样选择格式化方式

| 需求 | 推荐方式 | 原因 |
| --- | --- | --- |
| 固定显示 `YYYY年MM月DD日` | 手工读取年月日并拼接 | 输出结构完全受业务规格控制 |
| 按日本、中文等地区习惯显示 | `Intl.DateTimeFormat` | 自动处理地区格式和指定时区 |
| 页面只有一处简单显示 | `toLocaleDateString()` 或 `toLocaleString()` | 写法简洁 |
| 保存或接口传输时间点 | `toISOString()`，并遵循接口规格 | 得到统一的 UTC 表示，不是面向用户的显示文字 |
| 保存请假日、生日等纯日期 | `YYYY-MM-DD` 字符串 | 避免无意义的时区转换 |

没有一个格式同时适合保存、计算和所有用户显示。实际项目通常保留稳定的原始值，在页面显示的最后一步再格式化。

## 5. 数字转换与有效性

### 5.1 表单值默认是字符串

即使 `<input type="number">` 显示数字输入框，读取 `.value` 通常仍得到字符串：

```js
const daysText = "2";
const days = Number(daysText);

console.log(days); // 2
console.log(typeof days); // number
```

`Number(value)` 的基础转换规则已在第二章学习。业务代码在转换后还要判断结果是否有效。

### 5.2 `NaN`、有限数和整数

```js
console.log(Number.isNaN(Number("abc"))); // true
console.log(Number.isFinite(12.5)); // true
console.log(Number.isFinite(Infinity)); // false
console.log(Number.isInteger(12)); // true
console.log(Number.isInteger(12.5)); // false
```

| 方法 | 可接受的值 | 默认值或必填性 | 返回值与作用 |
| --- | --- | --- | --- |
| `Number.isNaN(value)` | 任意值 | 必填 | 只有值本身是 `NaN` 时返回 `true` |
| `Number.isFinite(value)` | 任意值 | 必填 | 是有限的数字时返回 `true`，不会自动转换字符串 |
| `Number.isInteger(value)` | 任意值 | 必填 | 是整数时返回 `true` |

例如，申请天数必须是正的有限数字：

```js
function isValidLeaveDays(value) {
  return Number.isFinite(value) && value > 0;
}
```

## 6. 项目中需要的数字处理

### 6.1 向上取整和取得最大值

```js
console.log(Math.ceil(1.2)); // 2
console.log(Math.max(3, 1, 8)); // 8
```

| 方法 | 参数 | 可接受的值 | 默认值或必填性 | 返回值 |
| --- | --- | --- | --- | --- |
| `Math.ceil(value)` | 一个值 | 数字 | 必填 | 向正无穷方向取整 |
| `Math.max(...values)` | 一个或多个值 | 数字 | 可变参数 | 最大值；无参数时为 `-Infinity` |

取整规则必须来自业务规格。例如分页总页数通常向上取整，可以使用 `Math.ceil(total / pageSize)`。两个或多个确定的数字可以直接交给Math.max；下节对长度不固定的序号数组使用循环逐项比较。

## 7. 生成练习项目的申请编号

### 7.1 编号规则

练习项目规定编号格式为 `REQ-YYYYMMDD-NNN`，同一天从 `001` 递增。下面的函数从已有申请中查找当天最大序号：

```js
function formatBasicDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}${month}${day}`;
}

function createApplicationId(applications, now = new Date()) {
  const dateText = formatBasicDate(now);
  const prefix = `REQ-${dateText}-`;

  const sequenceNumbers = applications
    .filter((application) => application.id.startsWith(prefix))
    .map((application) => Number(application.id.slice(prefix.length)))
    .filter((number) => Number.isInteger(number));

  let maxSequence = 0;
  for (const sequence of sequenceNumbers) {
    if (sequence > maxSequence) {
      maxSequence = sequence;
    }
  }
  const nextSequence = String(maxSequence + 1).padStart(3, "0");

  return `${prefix}${nextSequence}`;
}

const applications = [
  { id: "REQ-20260820-001" },
  { id: "REQ-20260820-002" },
];

const now = new Date(2026, 7, 20, 11, 0, 0);
console.log(createApplicationId(applications, now));
// REQ-20260820-003
```

`formatBasicDate(date)` 把本地日期转换为八位日期文字。`createApplicationId(applications, now)` 接收已有申请数组和当前时间；`now` 可选，默认使用当前时间，返回下一个编号。

这个实现只适用于单浏览器练习：本章实验生成后立即把新申请加入当前内存数组，再生成下一条编号。如果两个用户或两个请求同时生成编号，仍可能重复。正式项目应由后端或数据库在并发控制下生成业务编号。

## 8. 映射业务代码和显示文字

### 8.1 集中保存映射关系

```js
const LEAVE_TYPE_LABELS = {
  paid: "有給休暇",
  morning: "午前休",
  afternoon: "午後休",
  special: "特別休暇",
};

const APPLICATION_STATUS_LABELS = {
  pending: "申請中",
  approved: "承認済",
  rejected: "却下",
  cancelled: "取消済",
};
```

保存值使用稳定代码，显示时再转换成文字。不要把页面显示文字作为判断条件，否则修改文案可能破坏业务逻辑。

### 8.2 编写通用转换函数

```js
function getLabel(labels, value, fallback = "不明") {
  const label = labels[value];
  return label === undefined || label === null ? fallback : label;
}

console.log(getLabel(LEAVE_TYPE_LABELS, "paid"));
console.log(getLabel(APPLICATION_STATUS_LABELS, "unknown"));
```

`getLabel(labels, value, fallback)` 接收映射对象、代码值和可选的默认文字；找不到对应项时返回 `fallback`，其默认值是 `"不明"`。

## 9. 编写可复用工具函数

### 9.1 工具函数与页面代码分工

工具函数负责计算或转换，页面代码负责读取 DOM、响应事件和显示结果：

```text
表单和 DOM → 取得输入字符串 → 调用工具函数 → 取得结果 → 更新页面
```

例如，`calculateLeaveDays()` 不需要知道输入来自哪个 HTML 元素，也不应该在函数内部修改错误提示。

### 9.2 完整的页面调用示例

下面是可直接配合使用的独立 HTML 示例：

```html
<form id="leaveForm">
  <label>
    开始日期
    <input id="startDate" type="date" required>
  </label>
  <label>
    结束日期
    <input id="endDate" type="date" required>
  </label>
  <button type="submit">计算</button>
</form>
<p id="result"></p>
<script src="date-demo.js" defer></script>
```

将以下代码保存为同目录的 `date-demo.js`。同时把 3.2 节的 `parseDateTextToUtc()` 和 3.3 节的 `calculateLeaveDays()` 放在文件开头：

```js
const leaveForm = document.querySelector("#leaveForm");
const startDateInput = document.querySelector("#startDate");
const endDateInput = document.querySelector("#endDate");
const result = document.querySelector("#result");

leaveForm.addEventListener("submit", (event) => {
  event.preventDefault();

  const days = calculateLeaveDays(
    startDateInput.value,
    endDateInput.value,
  );

  if (days === null) {
    result.textContent = "请输入有效的日期范围";
    return;
  }

  result.textContent = `申请日数：${days}日`;
});
```

这个示例中的 HTML `id` 和 JavaScript 选择器一一对应。选择 2026-09-01 到 2026-09-03 后，应显示“申请日数：3日”。

### 9.3 工具函数的基本原则

- 一个函数集中完成一个主要任务。
- 参数名、返回值和无效输入时的结果要明确。
- 相同输入尽量得到相同输出，不依赖隐藏的 DOM 状态。
- 计算函数不直接查询 DOM、写入存储或跳转页面。
- 不直接修改传入的对象、数组或日期，除非函数名称和说明明确要求修改。
- 把业务规则写成可以单独调用和验证的函数。

这些特点会让函数更容易迁移到模块、TypeScript、Vue 或 React，也更容易编写自动测试。

## 10. 常见错误与排查

### 10.1 忘记月份从 0 开始

`getMonth()` 返回 `0`～`11`，显示月份时需要加 `1`。本章的 `formatBasicDate()` 已经统一处理这个规则。

### 10.2 混淆秒时间戳和毫秒时间戳

JavaScript `Date` 使用毫秒时间戳。接口返回 10 位秒时间戳时，通常需要乘以 `1000` 后再创建日期；接口返回 13 位毫秒时间戳时直接使用。不能只凭位数永久判断，应以接口规格为准。

```js
const timestampInSeconds = 1788226200;
const date = new Date(timestampInSeconds * 1000);

console.log(date.toISOString());
// 2026-09-01T01:30:00.000Z
```

### 10.3 把业务日期当成带时区时间点

请假日期应保持稳定的 `YYYY-MM-DD` 业务值。提交时间应保存带时区偏移的时间字符串或与后端约定的 UTC 时间。

### 10.4 没有检查无效结果

日期解析可能得到 `Invalid Date`，数字转换可能得到 `NaN`，工具函数也可能返回 `null`。使用结果前必须按函数约定检查。

### 10.5 使用格式化字符串继续计算

`Intl.DateTimeFormat.format()` 返回显示字符串。原始日期数据应另外保留，不要用格式化结果继续计算。

### 10.6 生成编号后没有更新内存数组

从已有数组计算“最大值加一”后，如果没有立即把新记录加入这个数组，下一次计算仍可能得到相同编号。本章只更新内存数组，刷新页面后重新开始；持久保存见 JSON 与浏览器存储章节。

### 10.7 把前端计算当作最终业务结果

用户可以修改前端代码和存储数据。余额、金额、正式编号和权限相关结果必须由后端重新验证或生成。

## 11. 本章综合练习

### 11.1 初始数据

```js
const applications = [
  {
    id: "REQ-20260820-001",
    leaveType: "paid",
    startDate: "2026-08-24",
    endDate: "2026-08-25",
    leaveDays: 2,
    status: "pending",
    submittedAt: "2026-08-20T11:00:00+09:00",
  },
];
```

### 11.2 任务要求

1. 完成 `parseDateTextToUtc(dateText)`，拒绝空值、错误格式和不存在的日期。
2. 完成 `calculateLeaveDays(startDateText, endDateText)`，按照包含首尾的自然日计算。
3. 半日休假返回 `0.5`，并限制开始日与结束日相同。
4. 完成 `formatBusinessDate(dateText)`，显示为 `YYYY年MM月DD日`。
5. 把 `submittedAt` 转换为时间戳和 UTC ISO 字符串，并说明两个结果表示的含义。
6. 使用 `Intl.DateTimeFormat` 按日本时区显示 `submittedAt`，结果应包含年月日和时分秒。
7. 完成 `createApplicationId(applications, now)`，生成 `REQ-YYYYMMDD-NNN`。
8. 使用映射对象转换请假类型和申请状态。
9. 使用本节初始数组和固定日期，在独立测试页调用工具函数并显示结果；DOM 查询和页面显示仍放在页面初始化代码中，不要求跨页读取或持久保存。

### 11.3 边界测试

| 输入或状态 | 预期结果 |
| --- | --- |
| `2026-09-01` 到 `2026-09-01` | `1` 日 |
| `2026-09-01` 到 `2026-09-03` | `3` 日 |
| `2026-09-03` 到 `2026-09-01` | 无效范围 |
| `2026-02-30` | 无效日期 |
| 半日休假且开始、结束日期不同 | 校验失败 |
| 当天已有 `001`、`002` | 新编号以 `003` 结尾 |
| 未知请假类型或状态 | 显示默认文字，不抛出异常 |

### 11.4 完成标准

- 日期计算不依赖本地一天固定为 24 小时。
- 能说明时间戳、UTC、时区偏移和日常显示文字之间的区别。
- 同一时间点按 UTC 和日本时区显示时，能够解释为何时刻不同但事件相同。
- 无效日期和反向日期范围有明确处理。
- 显示字符串与原始业务数据分开保存。
- 编号符合 `REQ-YYYYMMDD-NNN`，并说明只适用于单浏览器练习。
- 工具函数可以脱离 DOM 单独调用。
- 页面中的 HTML 元素和 JavaScript 选择器完全对应。
- 控制台没有未处理异常。
