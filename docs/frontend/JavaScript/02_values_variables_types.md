# 第二章 变量、值与数据类型

## 学习目标

完成本章后，你应能够：

- 使用 `const` 和 `let` 声明变量，并看懂旧代码中的 `var`。
- 说明三种声明方式在重新赋值、重复声明、作用域和变量提升方面的区别。
- 区分 JavaScript 的原始数据类型和引用数据类型。
- 使用 `typeof` 检查常见数据类型。
- 使用 `String()`、`Number()` 和 `Boolean()` 完成基础类型转换。
- 使用模板字符串组合变量和文字。

## 1. 变量解决什么问题

程序需要保存姓名、剩余天数、登录状态等数据。变量就是给数据起一个可以重复使用的名字。

```js
const employeeName = "山田 太郎";
let remainingPaidLeaveDays = 12;
```

- `employeeName` 保存员工姓名。
- `remainingPaidLeaveDays` 保存有给休假剩余天数。
- `=` 表示把右侧的值赋给左侧变量。

变量名应能表达数据的含义。推荐使用小驼峰命名法：第一个单词首字母小写，后续单词首字母大写，例如 `employeeName`、`remainingDays`。

## 2. 变量的三种声明方式

JavaScript 可以使用 `const`、`let` 和 `var` 声明变量。

### 2.1 `const`：声明后不再重新赋值

```js
const employeeNumber = "EMP-00001";
```

`const` 声明变量时必须同时赋值，之后不能再次使用 `=` 给它换一个值。

```js
const employeeNumber = "EMP-00001";
employeeNumber = "EMP-00002"; // TypeError
```

只要变量不需要重新赋值，就优先使用 `const`。

注意：`const` 限制的是“不能重新赋值”，不等于对象或数组内部的数据完全不能修改。对象和数组将在后续章节详细讲解。

### 2.2 `let`：声明后允许重新赋值

```js
let remainingDays = 12;
remainingDays = 10;
```

当变量保存的值确实需要变化时使用 `let`，例如剩余天数、计数器和当前选中的项目。

`let` 可以先声明，之后再赋值：

```js
let selectedApplication;
selectedApplication = "AP-0001";
```

在赋值之前，变量的值是 `undefined`。

### 2.3 `var`：旧代码中常见的声明方式

```js
var employeeName = "山田 太郎";
```

`var` 是 ES6 以前常用的写法。它允许重新赋值，也允许在同一作用域中重复声明：

```js
var status = "申請中";
var status = "承認済み";
console.log(status); // 承認済み
```

重复声明不一定立即报错，容易意外覆盖已有变量。因此，新代码不推荐使用 `var`，但阅读旧项目时必须能够看懂。

### 2.4 三种声明方式的区别

| 声明方式 | 声明时必须赋值 | 可以重新赋值 | 同一作用域可重复声明 | 作用域 | 声明前访问 | 使用建议 |
| --- | --- | --- | --- | --- | --- | --- |
| `const` | 是 | 否 | 否 | 块级作用域 | 报错 | 默认优先使用 |
| `let` | 否 | 是 | 否 | 块级作用域 | 报错 | 值需要变化时使用 |
| `var` | 否 | 是 | 是 | 函数作用域 | 得到 `undefined` | 只用于阅读和维护旧代码 |

简单记忆：先选 `const`，确实需要重新赋值时选 `let`，新代码通常不选 `var`。

## 3. 作用域

作用域表示变量可以在哪些位置被访问。

### 3.1 块级作用域

一对花括号 `{}` 可以形成一个代码块。`const` 和 `let` 具有块级作用域，只能在声明它们的代码块内部访问。

```js
{
  const message = "登录成功";
  let loginCount = 1;

  console.log(message);    // 登录成功
  console.log(loginCount); // 1
}

console.log(message); // ReferenceError
```

代码块外不能访问 `message`，因为它是在代码块内部使用 `const` 声明的。

### 3.2 `var` 没有块级作用域

`var` 不受普通代码块限制：

```js
{
  var oldMessage = "旧写法";
}

console.log(oldMessage); // 旧写法
```

这会让变量作用范围比预想的更大，也更容易发生变量名冲突。

`var` 具有函数作用域：如果它声明在函数内部，就只能在该函数内部访问。函数作用域会在第七章继续讲解。

## 4. 变量提升

JavaScript 执行代码前，会先处理当前作用域中的变量声明。这个现象称为变量提升（hoisting）。

### 4.1 `var` 的提升

```js
console.log(userName); // undefined
var userName = "山田 太郎";
```

上面的代码不会在第一行立即报告“变量不存在”，因为 `var userName` 的声明会被提升。可以暂时把它理解为：

```js
var userName;
console.log(userName); // undefined
userName = "山田 太郎";
```

只有声明被提升，赋值仍然留在原来的位置。因此，在赋值之前访问变量得到 `undefined`，这可能隐藏代码顺序问题。

### 4.2 `let` 和 `const` 的提升表现

`let` 和 `const` 的声明也会在执行前被处理，但从代码块开始到声明语句执行之前，变量处于“暂时性死区”（Temporal Dead Zone，TDZ），不能访问。

```js
console.log(employeeName); // ReferenceError
const employeeName = "山田 太郎";
```

不要为了使用变量提升而把变量写在声明之前。无论使用哪种声明方式，都应先声明，再使用。

## 5. JavaScript 的数据类型

数据类型表示一个值是什么种类，以及可以对它执行什么操作。JavaScript 的数据类型分为原始类型和引用类型。

| 分类 | 数据类型 | 常见写法 | 主要用途 |
| --- | --- | --- | --- |
| 原始类型 | `string` | `"山田 太郎"` | 保存文字 |
| 原始类型 | `number` | `12`、`0.5`、`NaN` | 保存整数、小数和特殊数值 |
| 原始类型 | `boolean` | `true`、`false` | 表示成立或不成立 |
| 原始类型 | `undefined` | `undefined` | 表示尚未赋值或不存在 |
| 原始类型 | `null` | `null` | 明确表示没有值 |
| 原始类型 | `bigint` | `9007199254740993n` | 表示超出安全整数范围的大整数 |
| 原始类型 | `symbol` | `Symbol("id")` | 创建唯一标识 |
| 引用类型 | `object` | `{ name: "山田" }`、`[1, 2]` | 保存一组相关数据 |

本课程主线重点掌握 `string`、`number`、`boolean`、`undefined`、`null` 和 `object`。`bigint` 和 `symbol` 现阶段能识别即可，后续现代对象章节会再次介绍 `symbol`。

### 5.1 字符串 `string`

字符串用于表示文本，可以使用单引号、双引号或反引号。

```js
const employeeName = "山田 太郎";
const statusText = '申請中';
const message = `${employeeName}さん、ログインしました`;
```

反引号定义的是模板字符串。`${}` 可以把变量或计算结果放进字符串中。

### 5.2 数字 `number`

JavaScript 中的整数和小数都属于 `number` 类型。

```js
const totalDays = 12;
const halfDay = 0.5;
const remainingDays = totalDays - halfDay;
```

计算无法得到有效数字时，可能产生 `NaN`，意思是“不是一个有效数字”。

```js
const result = Number("十二");
console.log(result); // NaN
console.log(Number.isNaN(result)); // true
```

`Number(value)` 尝试把传入值转换为数字；无法转换时得到 `NaN`。`Number.isNaN(value)` 用于判断传入值是否就是 `NaN`。

`NaN` 本身仍属于 `number` 类型。检查一个值是否为 `NaN` 时，推荐使用 `Number.isNaN()`。

### 5.3 布尔值 `boolean`

布尔值只有 `true` 和 `false`，常用于表示登录状态、校验结果和开关状态。

```js
const isLoggedIn = true;
const hasError = false;
```

### 5.4 `undefined` 和 `null`

| 值 | 含义 | 常见场景 |
| --- | --- | --- |
| `undefined` | 还没有值 | 变量声明后未赋值，或对象没有相应属性 |
| `null` | 明确表示没有值 | 当前没有登录用户、查询不到目标数据 |

```js
let selectedApplication;
console.log(selectedApplication); // undefined

const currentUser = null;
```

如果程序需要主动表达“现在没有这个值”，通常使用 `null` 会更明确。

### 5.5 对象 `object`

对象可以把多项相关数据放在一起。数组、普通对象和日期对象都属于引用类型。

```js
const employee = {
  employeeNumber: "EMP-00001",
  name: "山田 太郎"
};

const applicationStatuses = ["申請中", "承認済み"];
```

本章先识别对象和数组的类型，后续章节再详细学习它们的操作方法。

## 6. 使用 `typeof` 检查类型

`typeof` 运算符会返回表示数据类型的字符串。

```js
console.log(typeof "山田");          // string
console.log(typeof 12);              // number
console.log(typeof true);            // boolean
console.log(typeof undefined);       // undefined
console.log(typeof 900n);            // bigint
console.log(typeof Symbol("id"));    // symbol
console.log(typeof { name: "山田" }); // object
```

需要注意两个特殊情况：

```js
console.log(typeof null);   // object
console.log(typeof [1, 2]); // object
```

- `typeof null` 得到 `"object"` 是 JavaScript 的历史遗留行为，但 `null` 并不是对象。
- 数组属于对象，因此 `typeof` 数组也得到 `"object"`。需要判断数组时使用 `Array.isArray()`。

```js
console.log(Array.isArray([1, 2])); // true
```

## 7. 数据类型转换

网页输入框中取得的内容通常是字符串。进行计算、显示或条件判断前，经常需要把数据转换成合适的类型。

### 7.1 转换为字符串：`String()`

```js
console.log(String(12));        // "12"
console.log(String(true));      // "true"
console.log(String(null));      // "null"
console.log(String(undefined)); // "undefined"
```

`String(value)` 会返回对应的字符串，不会修改原变量。

### 7.2 转换为数字：`Number()`

```js
console.log(Number("12"));    // 12
console.log(Number("0.5"));   // 0.5
console.log(Number(""));      // 0
console.log(Number(true));     // 1
console.log(Number(false));    // 0
console.log(Number("十二"));  // NaN
```

把输入内容转换成数字后，应检查转换结果是否为 `NaN`：

```js
const inputValue = "12";
const days = Number(inputValue);

console.log(Number.isNaN(days)); // false
console.log(days + 1);           // 13
```

下一章学习条件判断后，就可以根据 `Number.isNaN(days)` 的结果决定显示错误还是继续计算。

`parseInt()` 和 `parseFloat()` 也能从字符串开头解析整数或小数：

```js
console.log(parseInt("12日", 10)); // 12
console.log(parseFloat("0.5日"));  // 0.5
```

`parseInt(value, 10)` 中的 `10` 表示按十进制解析。表单值必须是完整数字时，优先使用 `Number()`；明确需要读取字符串开头的数字时，再使用 `parseInt()` 或 `parseFloat()`。

### 7.3 转换为布尔值：`Boolean()`

```js
console.log(Boolean(1));         // true
console.log(Boolean("山田"));   // true
console.log(Boolean(0));         // false
console.log(Boolean(""));        // false
console.log(Boolean(null));      // false
console.log(Boolean(undefined)); // false
console.log(Boolean(NaN));       // false
```

转换为 `false` 的常见值包括：

- `false`
- `0`、`-0`
- `""`（空字符串）
- `null`
- `undefined`
- `NaN`

这些值称为假值（falsy）。其他常见值通常会转换为 `true`，称为真值（truthy）。特别注意，字符串 `"false"` 和字符串 `"0"` 都不是空字符串，因此会转换为 `true`。

### 7.4 隐式类型转换

JavaScript 在某些运算中会自动转换数据类型，这称为隐式类型转换。

```js
console.log("12" + 1); // "121"
console.log("12" - 1); // 11
```

使用 `+` 时，只要一侧是字符串，JavaScript 通常会进行字符串拼接；使用 `-` 时，JavaScript 会尝试把字符串转换为数字。这种差异容易产生错误。

```js
const inputDays = "12";
const nextDays = Number(inputDays) + 1;
console.log(nextDays); // 13
```

实际项目中，不要依赖难以看懂的隐式转换。先使用 `Number()`、`String()` 或 `Boolean()` 明确转换，再进行后续操作。

## 8. 本章练习

### 练习 1：声明变量并输出

```js
const employeeNumber = "EMP-00001";
const employeeName = "山田 太郎";
let remainingDays = 12;
const isLoggedIn = true;
```

使用模板字符串输出：

```text
EMP-00001 山田 太郎 さんの有給残日数は 12 日です。
```

然后把 `remainingDays` 修改为 `10`，再次输出并确认结果发生变化。

### 练习 2：观察作用域和变量提升

分别运行下面两段代码，记录控制台结果，并说明为什么不同。

```js
console.log(oldStatus);
var oldStatus = "申請中";
```

```js
console.log(currentStatus);
let currentStatus = "申請中";
```

第二段代码观察完错误后，将声明语句移动到输出语句之前，确认错误消失。

### 练习 3：转换输入数据

```js
const inputDays = "12";
```

完成以下任务：

1. 使用 `typeof` 确认 `inputDays` 的类型。
2. 将它转换成数字并加 `1`。
3. 输出转换后的值和类型，结果应分别为 `13` 和 `number`。

## 本章检查点

- 能说明什么时候使用 `const`、`let` 和 `var`。
- 能说明块级作用域、函数作用域和变量提升的基本区别。
- 能区分原始类型和引用类型。
- 能说明 `null` 与 `undefined` 的区别。
- 能正确理解 `typeof null` 和 `typeof []` 的结果。
- 能使用 `String()`、`Number()` 和 `Boolean()` 进行显式类型转换。
- 能识别字符串加法产生的隐式类型转换问题。
