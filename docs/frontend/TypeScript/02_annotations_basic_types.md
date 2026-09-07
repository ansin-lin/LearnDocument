# 第 2 章 类型标注、基本类型与类型转换

第一章已经运行了最小 TypeScript 程序。本章先学习怎样描述一个值的类型，再区分类型推断、运行时转换和类型断言。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。

完成本章后，你应当能够：

- 使用类型标注描述字符串、数字和布尔值。
- 说明声明时赋值与先声明后赋值的推断差异。
- 区分 `null`、`undefined`、`any` 和 `unknown`。
- 使用 `String()`、`Number()` 和 `Boolean()`转换值。
- 检查数字转换产生的 `NaN`。
- 说明类型断言只影响编译检查，不会转换或验证数据。

## 1. 值、类型和类型标注

### 1.1 值和类型不是同一件事

`"田中"` 是实际保存的数据，`string` 是对这一类数据的描述：

```text
"田中"  → 一个字符串值
string  → 字符串类型
```

类型标注写在变量名后面，基本形式是：

```text
变量名: 类型
```

### 1.2 给变量添加类型标注

```ts
let personName: string = "田中";
let age: number = 20;
let active: boolean = true;

console.log(personName, age, active); // 田中 20 true
// age = "二十"; // 错误：string不能赋给number
```

`personName: string` 表示变量只能保存字符串，`age: number` 表示只能保存数字。TypeScript 会在编译时检查赋值是否符合约定。

`let` 和 `const` 仍遵守 JavaScript 规则：`let` 允许重新赋值，`const` 不允许。TypeScript 增加的是“赋入的值还必须符合类型”。

类型标注在编译后会消失：

```ts
const message: string = "Hello";
```

生成的 JavaScript 类似：

```js
const message = "Hello";
```

因此类型不能代替运行时的数据校验。

## 2. 常见基本类型

### 2.1 字符串、数字和布尔值

| TypeScript 类型 | 表示什么 | 值的例子 |
| --- | --- | --- |
| `string` | 文本 | `"hello"`、`"田中"` |
| `number` | 普通整数、小数及特殊数字值 | `20`、`1.5`、`NaN` |
| `boolean` | 逻辑真假 | `true`、`false` |

```ts
const employeeName: string = "田中";
const leaveDays: number = 1.5;
const approved: boolean = false;

console.log(employeeName); // 田中
console.log(leaveDays);    // 1.5
console.log(approved);     // false
```

TypeScript 没有单独的 `int` 和 `float` 基本类型，普通整数和小数都使用 `number`。

类型名称使用小写 `string`、`number`、`boolean`。不要把它们写成包装对象类型 `String`、`Number`、`Boolean`。

### 2.2 `bigint` 和 `symbol`：了解

`bigint`用于表示超出普通安全整数范围的大整数，`symbol`用于创建唯一标识。普通 React/Vue 页面业务很少直接使用它们，本课程不要求编写，只需在已有类型中看到时知道它们不是`number`和`string`。

## 3. 类型推断

**类型推断**是编译器根据代码中的信息判断类型，不要求每次都手写冒号。

### 3.1 声明时直接赋值

```ts
let score = 80;
score = 90;

console.log(score); // 90
// score = "优秀"; // 错误：score已推断为number
```

“没有写类型标注”不等于“没有类型检查”。声明时提供了数字，`score` 被推断为 `number`，后续仍然只能赋入数字。

没有必要写出所有显而易见的类型：

```ts
const employeeName = "田中"; // 推断为string
const age = 20;              // 推断为number
const active = true;         // 推断为boolean

console.log(employeeName, age, active); // 田中 20 true
```

### 3.2 先声明，不写类型，下一行再赋值

```ts
let score;

score = 80;
console.log(score + 10); // 90，此处按number检查

score = "优秀";
console.log(score.toUpperCase()); // 优秀，此处按string检查
// console.log(score * 2); // 错误：此处score是字符串
```

`toUpperCase()` 返回转换为大写后的字符串；中文没有大小写变化，因此仍输出“优秀”。

在本课程的严格检查环境中，按执行位置理解：

1. `let score;` 没有提供初始值，也没有声明固定类型，运行时初始值是 `undefined`。
2. `score = 80` 后，编译器在接下来的位置把它按数字检查。
3. `score = "优秀"` 后，编译器在接下来的位置把它按字符串检查。

这类变量初始带有 `any` 的特征，编译器再根据各次赋值跟踪当前位置的类型。不要把它理解为“第一次赋值后永远固定”，也不要理解为“后续完全不检查”。

### 3.3 稍后赋值但用途固定时，明确写类型

如果分数始终只能是数字，应在声明时写清约定：

```ts
let score: number;

// console.log(score); // 错误：赋值前使用变量
score = 80;
console.log(score + 10); // 90
// score = "优秀"; // 错误：string不能赋给number
```

`let score: number;` 不会自动创建数字 `0`。它只规定未来的值必须是数字，读取前仍要先完成赋值。

`const` 必须在声明时提供初始值，不能写 `const score;`。

## 4. `null` 与 `undefined`

它们表示两种不同的空值：

| 值 | 常见含义 |
| --- | --- |
| `null` | 程序主动表示“当前没有值” |
| `undefined` | 尚未赋值、没有提供或不存在 |

```ts
const emptyValue: null = null;
const missingValue: undefined = undefined;

console.log(emptyValue);   // null
console.log(missingValue); // undefined
// const text: string = null; // strict模式下不允许
```

`null`、字符串 `"null"` 和空字符串 `""` 是三个不同的值。一个变量需要同时允许字符串和空值时，要使用第五章的联合类型。

## 5. `any` 与 `unknown`

### 5.1 `any`：放弃大部分检查

```ts
let looseValue: any = "hello";
looseValue = 100;

console.log(looseValue); // 100
// looseValue.trim(); // 编译器可能不阻止，运行时会报错
```

`any` 可以接收不同类型，也允许执行几乎任何操作。它会让编译器失去发现错误的能力，不应作为默认类型。

### 5.2 `unknown`：允许接收，但要求先确认

```ts
let unknownValue: unknown = "hello";

console.log(unknownValue); // hello
// unknownValue.trim(); // 错误：尚未确认是字符串
```

`unknown` 可以接收任意值，但不能直接当成字符串、数字或其他具体类型使用。

| 类型 | 能否接收不同类型 | 使用前是否要求确认 | 主要风险 |
| --- | --- | --- | --- |
| `any` | 能 | 通常不要求 | 容易把错误留到运行时 |
| `unknown` | 能 | 要求 | 需要多写真实检查，但更安全 |

第六章会使用 `typeof` 等判断安全地缩小 `unknown` 的范围。

## 6. 类型转换会改变运行时值

### 6.1 类型标注不负责转换

下面的字符串不能因为写了 `: number` 就自动变成数字：

```ts
const inputText = "12";
// const wrongCount: number = inputText; // 错误：标注不会转换值

const count: number = Number(inputText);
console.log(count);        // 12
console.log(typeof count); // number
console.log(count + 1);    // 13
```

`Number(inputText)` 在程序运行时执行转换，`: number` 只在编译时检查结果能否赋给变量。

### 6.2 使用 `String()` 转成字符串

```ts
const count = 12;
const active = true;

const countText = String(count);
const activeText = String(active);

console.log(countText);        // 12
console.log(typeof countText); // string
console.log(activeText);       // true
```

`String(value)` 接收任意值，返回对应的字符串表示。数字 `12` 转换后是字符串 `"12"`，控制台外观看起来相同，可以用 `typeof` 确认类型。

### 6.3 使用 `Number()` 转成数字

```ts
console.log(Number("12"));    // 12
console.log(Number("1.5"));   // 1.5
console.log(Number(""));      // 0
console.log(Number(" 20 "));  // 20
console.log(Number(true));     // 1
console.log(Number(false));    // 0
console.log(Number("12px"));  // NaN
```

`Number(value)` 尝试把整个值转换成数字。空字符串会得到 `0`，所以表单必填判断应在转换前完成，不能把空字符串直接当成有效的零。

`NaN` 表示“不是一个有效数字结果”。它的 TypeScript 类型仍然是 `number`：

```ts
const result = Number("abc");

console.log(result);                 // NaN
console.log(typeof result);          // number
console.log(Number.isNaN(result));   // true
console.log(Number.isFinite(result)); // false
```

- `Number.isNaN(value)` 只在值确实为 `NaN` 时返回 `true`。
- `Number.isFinite(value)` 只在值是有限数字时返回 `true`，不会自动转换字符串。

类型是 `number` 只说明它属于 JavaScript 数字体系，不保证一定适合业务计算。

### 6.4 提取整数或小数

```ts
console.log(Number.parseInt("12px", 10)); // 12
console.log(Number.parseInt("1.8", 10));  // 1
console.log(Number.parseFloat("1.8kg"));  // 1.8
console.log(Number.parseFloat("abc"));    // NaN
```

- `Number.parseInt(text, radix)` 从字符串开头读取整数；`radix` 是进制，十进制通常明确写 `10`。
- `Number.parseFloat(text)` 从字符串开头读取小数。

它们可能忽略后面的无效字符，因此不适合直接证明整段用户输入都是合法数字。表单要求完整数字时，通常先检查空值，再使用 `Number()` 并检查有限性。

### 6.5 使用 `Boolean()` 转成布尔值

```ts
console.log(Boolean(1));           // true
console.log(Boolean(0));           // false
console.log(Boolean("hello"));     // true
console.log(Boolean(""));          // false
console.log(Boolean("false"));     // true
console.log(Boolean(null));        // false
console.log(Boolean(undefined));   // false
console.log(Boolean(Number("x"))); // false，因为结果是NaN
```

`Boolean(value)` 按 JavaScript 真值规则返回 `true` 或 `false`。需要特别注意：非空字符串 `"false"` 仍然是真值，不会自动理解成布尔值 `false`。

常见假值包括 `false`、`0`、`""`、`null`、`undefined` 和 `NaN`。业务代码不能只凭真假判断区分“没有值”和合法的 `0`。

## 7. 类型断言不会转换数据

### 7.1 `as` 是告诉编译器怎样看待值

类型断言的常见语法是：

```text
值 as 类型
```

```ts
const value: unknown = "hello";
const text = value as string;

console.log(text.toUpperCase()); // HELLO
console.log(typeof text);        // string
```

`value as string` 告诉编译器“请在这里把它当成字符串”。本例的实际值确实是字符串，所以可以正常运行。

类型断言不会生成转换代码，也不会在运行时检查值。编译后的 JavaScript 中，`as string` 会消失。

### 7.2 错误断言可能骗过编译器

下面是用于观察风险的独立实验：

```ts
const input: unknown = "12";
const count = input as number;

console.log(typeof count); // string
console.log(count + 1);    // 121
```

TypeScript 被告知 `count` 是数字，但运行时真实值仍是字符串 `"12"`，所以加号执行字符串拼接并得到 `"121"`。

正确做法是实际转换并检查：

```ts
const input: unknown = "12";

if (typeof input === "string") {
  const count = Number(input);

  if (Number.isFinite(count)) {
    console.log(count + 1); // 13
  }
}
```

`typeof input === "string"` 是运行时真实检查，`Number(input)` 是运行时真实转换。第六章会详细讲解类型收窄以及确实需要断言的场景。

### 7.3 初学阶段的使用原则

- 能通过 `typeof` 等条件确认时，优先真实检查。
- 需要把字符串转数字时使用 `Number()`，不要写 `as number`。
- 来自接口、存储和用户输入的数据不能靠断言变安全。
- 不要连续写 `as unknown as 某类型` 强行绕过错误。
- 项目代码优先使用 `as` 语法；另一种尖括号断言在 JSX 文件中容易与标签混淆。

`as const` 虽然也使用 `as` 关键字，但它不是把值强行指定为某个普通类型，而是要求编译器保留字面量的具体值并生成只读类型。它将在[第十一章](11_constants_enum.md)结合常量对象和只读元组详细讲解。

## 8. 四个概念不要混淆

| 概念 | 示例 | 是否改变运行时值 | 作用 |
| --- | --- | --- | --- |
| 类型标注 | `let age: number` | 否 | 明确规定变量类型 |
| 类型推断 | `let age = 20` | 否 | 编译器从代码判断类型 |
| 类型转换 | `Number("20")` | 是 | 运行时生成数字值 `20` |
| 类型断言 | `value as number` | 否 | 要求编译器暂时按指定类型检查 |

看到类型问题时，先判断需求：是要描述约定、让编译器推断、真正改变值，还是在有可靠依据时补充编译器无法知道的信息。

## 9. 常见错误与排查

### 9.1 认为标注可以转换值

`: number` 不会把字符串转成数字。使用 `Number()`，并检查空值、`NaN` 和业务范围。

### 9.2 没检查 `Number()` 的结果

`Number("abc")` 得到 `NaN`，但类型仍是 `number`。计算前使用 `Number.isFinite()` 或按需求使用 `Number.isNaN()`。

### 9.3 认为 `Boolean("false")` 是 `false`

任何非空字符串都是真值。接口中的布尔值应该使用真正的 JSON 布尔值，表单字符串则按明确规则转换。

### 9.4 使用断言消除所有报错

断言不会修复错误数据。看到 `as` 时应能说明证据来自哪里；无法说明时，应增加真实检查或修正类型定义。

### 9.5 默认使用 `any`

`any` 会让错误操作通过编译。尚未确认的值优先用 `unknown`，并在使用前检查。

## 10. 本章练习

### 10.1 类型标注与推断

1. 声明姓名、年龄和启用状态，分别使用合适的类型。
2. 对有明确初始值的变量省略类型标注，尝试赋入错误类型并记录编译错误。
3. 先声明 `let score;`，分别赋数字和字符串，观察不同位置允许的操作。
4. 改为 `let score: number;`，确认字符串赋值被阻止。

### 10.2 类型转换

1. 把字符串 `"25"` 转成数字，加一后输出 `26`。
2. 分别转换 `""`、`"abc"` 和 `"12px"`，记录结果。
3. 使用 `Number.isFinite()` 判断哪些结果可以继续参与计算。
4. 将数字 `100` 转成字符串，通过 `typeof` 验证。
5. 分别转换 `"false"` 和空字符串，解释布尔结果为什么不同。

### 10.3 类型断言风险

1. 运行第 7.2 节的错误断言示例，确认结果为字符串拼接的 `121`。
2. 删除断言，改用 `typeof`、`Number()` 和 `Number.isFinite()`。
3. 用一句话分别说明类型转换和类型断言是否改变运行时数据。

## 本章检查点

- 能使用小写基本类型完成变量标注。
- 能解释声明时赋值与先声明后赋值的类型推断差异。
- 能区分 `null`、`undefined`、`any` 和 `unknown`。
- 能使用字符串、数字和布尔转换，并说明常见边界值。
- 能检查 `NaN` 和非有限数字。
- 能区分标注、推断、转换和断言。
- 能说明为什么 `as number` 不能代替 `Number()`。
- 面对外部数据时，知道真实检查优先于类型断言。
