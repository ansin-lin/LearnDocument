# 第 7 章 函数类型

JavaScript 函数接收输入、执行处理并返回结果。TypeScript 可以进一步规定参数的类型、参数是否可以省略、返回值的类型，以及“一个变量中保存的函数必须是什么形状”。这些检查可以在调用函数和修改函数实现时提前发现错误。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。注释掉的错误代码用于观察编译提示。

完成本章后，你应当能够：

- 为普通函数和箭头函数标注参数及返回值。
- 区分必填参数、可选参数、默认参数和剩余参数。
- 使用对象类型描述包含多个字段的参数。
- 为保存函数的变量和回调函数定义类型。
- 正确理解 `void`和`never`的常见用途。

## 1. 函数类型解决什么问题

### 1.1 JavaScript 函数可能收到错误输入

```js
function calculateTotal(price, quantity) {
  return price * quantity;
}

console.log(calculateTotal(1200, 2));    // 2400
console.log(calculateTotal("错误", 2)); // NaN
```

JavaScript 允许调用方传入任意类型。第二次调用能够执行，但字符串不能完成预期的金额计算，结果成为 `NaN`。

TypeScript 可以明确输入和输出约定：

```ts
function calculateTotal(price: number, quantity: number): number {
  return price * quantity;
}

console.log(calculateTotal(1200, 2)); // 2400
// console.log(calculateTotal("错误", 2));
// 错误：第一个参数必须是number
```

函数类型主要检查两个方向：

- 调用方有没有按照约定提供参数；
- 函数实现有没有按照约定返回结果。

## 2. 参数类型与返回值类型

### 2.1 参数类型写在哪里

```ts
function createGreeting(employeeName: string, remainingDays: number): string {
  return `${employeeName}的剩余休假：${remainingDays}日`;
}

const message = createGreeting("田中", 10);
console.log(message); // 田中的剩余休假：10日
```

参数名后的 `: 类型` 限制调用时可以传入的值：

```ts
function createGreeting(
  employeeName: string,
  remainingDays: number
): string {
  return `${employeeName}的剩余休假：${remainingDays}日`;
}
```

`employeeName` 只能接收字符串，`remainingDays` 只能接收数字。参数名称不参与类型匹配，但清楚的名称可以说明每个位置需要什么数据。

### 2.2 返回值类型写在哪里

右括号后的 `: string` 是返回值类型：

```ts
function getStatusText(approved: boolean): string {
  if (approved) {
    return "已批准";
  }

  return "审批中";
}

console.log(getStatusText(true));  // 已批准
console.log(getStatusText(false)); // 审批中
```

所有能够正常结束的返回路径都必须返回字符串：

```ts
function getStatusText(approved: boolean): string {
  if (approved) {
    return "已批准";
  }

  // return 0;
  // 错误：number不能作为string返回
  return "审批中";
}

console.log(getStatusText(false)); // 审批中
```

显式返回类型的价值不只是告诉调用方结果类型，也能检查函数内部有没有意外返回错误数据。

## 3. 返回值类型可以推断

### 3.1 根据 `return` 推断结果

```ts
function calculateTax(price: number) {
  return price * 0.1;
}

const tax = calculateTax(1000);
console.log(tax); // 100
```

虽然没有写 `: number`，TypeScript 根据乘法结果推断返回值是数字。因此，简单的内部函数可以利用类型推断，避免重复标注。

### 3.2 什么时候建议明确写返回类型

下列情况通常适合明确标注：

- 被多个文件调用的公开函数；
- 业务规则较多、存在多个返回分支的函数；
- 希望修改实现时仍保持稳定返回约定的函数；
- 团队规范要求明确返回类型的函数。

```ts
function calculateDiscount(price: number, member: boolean): number {
  if (member) {
    return price * 0.9;
  }

  return price;
}

console.log(calculateDiscount(1000, true));  // 900
console.log(calculateDiscount(1000, false)); // 1000
```

优先利用清楚的推断，但不要为了少写几个字符，让重要函数的输入输出约定变得难以确认。

## 4. TypeScript 会检查参数数量

### 4.1 必填参数必须提供

```ts
function formatName(familyName: string, givenName: string): string {
  return `${familyName} ${givenName}`;
}

console.log(formatName("田中", "太郎")); // 田中 太郎
// console.log(formatName("田中"));
// 错误：应提供2个参数，但只提供了1个
```

没有 `?` 或默认值的参数是必填参数。调用时缺少参数，TypeScript 会报错。

### 4.2 通常不能随意增加参数

```ts
function formatName(familyName: string, givenName: string): string {
  return `${familyName} ${givenName}`;
}

// formatName("田中", "太郎", "先生");
// 错误：应提供2个参数，但提供了3个
console.log(formatName("田中", "太郎")); // 田中 太郎
```

JavaScript 运行时可能忽略多余参数，但 TypeScript 直接调用函数时会检查参数数量。若确实允许不定数量的参数，应使用后文的剩余参数，而不是依赖多余参数被忽略。

## 5. 可选参数和默认参数

### 5.1 可选参数 `?`

```ts
function greet(employeeName: string, prefix?: string): string {
  if (prefix === undefined) {
    return employeeName;
  }

  return `${prefix}${employeeName}`;
}

console.log(greet("田中"));           // 田中
console.log(greet("田中", "你好，")); // 你好，田中
```

`prefix?: string` 表示调用方可以省略第二个参数。函数内部的 `prefix` 实际类型是 `string | undefined`，所以使用字符串功能前要先处理 `undefined`。

必填参数应放在可选参数之前：

```ts
function greet(employeeName: string, prefix?: string): string {
  return prefix === undefined
    ? employeeName
    : `${prefix}${employeeName}`;
}

console.log(greet("佐藤")); // 佐藤
```

不能把一个必填参数直接放在可选参数后面，否则调用方无法清楚地跳过前一个位置再提供后一个位置。

### 5.2 默认参数

```ts
function calculatePrice(price: number, quantity = 1): number {
  return price * quantity;
}

console.log(calculatePrice(1200));    // 1200
console.log(calculatePrice(1200, 3)); // 3600
```

`quantity = 1` 表示调用方省略该参数时，运行时使用默认值 `1`。TypeScript 也根据默认值推断 `quantity` 是数字。

调用时显式传入 `undefined` 也会使用默认值：

```ts
function createLabel(name: string, suffix = "様"): string {
  return `${name}${suffix}`;
}

console.log(createLabel("田中"));            // 田中様
console.log(createLabel("田中", undefined)); // 田中様
console.log(createLabel("田中", "さん"));   // 田中さん
```

默认参数与可选参数都允许省略，但含义不同：默认参数会在运行时得到准备好的值；可选参数仍可能是 `undefined`，要由函数决定如何处理。

## 6. 对象参数适合多个相关字段

参数较多时，只靠位置很难判断每个值的含义：

```ts
function createRequest(
  employeeCode: string,
  startDate: string,
  endDate: string,
  reason: string
): string {
  return `${employeeCode}: ${startDate}～${endDate} ${reason}`;
}

console.log(createRequest(
  "EMP-001",
  "2026-09-10",
  "2026-09-11",
  "私用"
));
// EMP-001: 2026-09-10～2026-09-11 私用
```

这些值都是字符串，顺序写错时 TypeScript 也无法发现。可以把相关字段组合成对象：

```ts
type LeaveRequestInput = {
  employeeCode: string;
  startDate: string;
  endDate: string;
  reason?: string;
};

function createRequest(input: LeaveRequestInput): string {
  const reasonText = input.reason === undefined
    ? "理由未填写"
    : input.reason;

  return `${input.employeeCode}: ${input.startDate}～${input.endDate} ${reasonText}`;
}

console.log(createRequest({
  employeeCode: "EMP-001",
  startDate: "2026-09-10",
  endDate: "2026-09-11",
}));
// EMP-001: 2026-09-10～2026-09-11 理由未填写
```

调用处能够看到属性名，不再只依赖位置。对象类型还可以使用第四章学过的必填、可选和只读属性。

### 6.1 解构对象参数

JavaScript 的对象解构也可以与 TypeScript 类型一起使用：

```ts
type Employee = {
  employeeCode: string;
  employeeName: string;
};

function showEmployee(
  { employeeCode, employeeName }: Employee
): string {
  return `${employeeCode}: ${employeeName}`;
}

const employee: Employee = {
  employeeCode: "EMP-001",
  employeeName: "田中",
};

console.log(showEmployee(employee)); // EMP-001: 田中
```

花括号 `{ employeeCode, employeeName }` 从参数对象中取出两个属性，后面的 `: Employee` 描述整个参数对象的类型。不要给每个解构变量分别重复写类型。

## 7. 剩余参数接收不定数量的值

```ts
function sum(...values: number[]): number {
  let total = 0;

  for (const value of values) {
    total += value;
  }

  return total;
}

console.log(sum());          // 0
console.log(sum(10));        // 10
console.log(sum(10, 20, 5)); // 35
```

`...values` 是 JavaScript 的剩余参数语法，会把当前参数位置之后传入的所有值收集成一个数组。`number[]` 表示这个数组的每一项都必须是数字。

剩余参数必须放在参数列表最后，因为它要接收剩余的全部实参：

```ts
function createMessage(prefix: string, ...names: string[]): string {
  return `${prefix}${names.join("、")}`;
}

console.log(createMessage("参加者：", "田中", "佐藤"));
// 参加者：田中、佐藤
```

`join("、")` 是数组方法，使用指定分隔符连接所有元素，并返回字符串。这里的第一个参数固定作为前缀，其余字符串都进入 `names` 数组。

## 8. 箭头函数怎样标注类型

### 8.1 直接标注参数和返回值

```ts
const calculateTotal = (
  price: number,
  quantity: number
): number => {
  return price * quantity;
};

console.log(calculateTotal(1500, 2)); // 3000
```

箭头函数的参数类型仍写在参数名后，返回值类型写在参数列表与 `=>` 之间。这里的第一个冒号描述 `price`，第二个冒号描述 `quantity`，右括号后的冒号描述返回值。

只有一个表达式时可以省略函数体花括号和 `return`：

```ts
const calculateTotal = (
  price: number,
  quantity: number
): number => price * quantity;

console.log(calculateTotal(800, 3)); // 2400
```

### 8.2 不要把两个箭头混在一起

后面会看到这种写法：

```ts
type Calculator = (left: number, right: number) => number;

const add: Calculator = (left, right) => left + right;

console.log(add(2, 3)); // 5
```

这段代码中两个 `=>` 的职责不同：

- `type Calculator` 中的 `=> number` 是类型语法，说明函数返回数字，没有函数体；
- 赋值右侧的 `=> left + right` 是 JavaScript 箭头函数，提供真正执行的实现。

## 9. 函数本身也可以有类型

### 9.1 用类型别名描述调用方式

```ts
type Calculator = (left: number, right: number) => number;

const add: Calculator = (left, right) => left + right;
const subtract: Calculator = (left, right) => left - right;

console.log(add(10, 3));      // 13
console.log(subtract(10, 3)); // 7
```

`Calculator` 规定：必须是一个接收两个数字并返回数字的函数。这种输入和输出要求称为函数的**调用签名**。

变量已经标注为 `Calculator` 后，右侧箭头函数的 `left` 和 `right` 会根据上下文推断为数字，不需要再次标注。

### 9.2 名称不同不影响匹配

```ts
type Formatter = (value: number, unit: string) => string;

const formatPrice: Formatter = (price, currency) => {
  return `${price}${currency}`;
};

console.log(formatPrice(1200, "円")); // 1200円
```

类型中的参数名 `value`、`unit` 用来帮助阅读。真正实现可以使用 `price`、`currency`；匹配时主要比较参数的位置、类型和返回值类型。

函数类型不要求实现使用箭头函数，普通函数也可以赋给同样的类型：

```ts
type Formatter = (value: number) => string;

function formatDays(days: number): string {
  return `${days}日`;
}

const formatter: Formatter = formatDays;
console.log(formatter(5)); // 5日
```

这里赋值的是函数 `formatDays` 本身，而不是调用结果，所以没有写括号 `formatDays()`。

## 10. 回调函数类型

### 10.1 什么是回调函数

把一个函数作为参数传给另一个函数，这个被传入的函数称为**回调函数（callback function）**：

```ts
type NumberRule = (value: number) => boolean;

function selectNumbers(
  values: number[],
  rule: NumberRule
): number[] {
  return values.filter(rule);
}

const positiveNumbers = selectNumbers(
  [-2, 0, 3, 5],
  value => value > 0
);

console.log(positiveNumbers); // [3, 5]
```

执行过程如下：

1. `selectNumbers()` 接收数字数组和一个规则函数；
2. `filter()` 逐项调用 `rule`；
3. 回调参数 `value` 依次得到 `-2`、`0`、`3`、`5`；
4. 回调返回 `true` 的 `3`、`5` 被保留；
5. `filter()` 返回新的数字数组。

`filter()` 是数组筛选方法，回调对每一项返回布尔结果，它会返回所有判断为真的元素组成的新数组，不修改原数组。

### 10.2 回调参数可以从上下文推断

```ts
const employeeNames = ["田中", "佐藤", "鈴木"];

const longNames = employeeNames.filter(name => name.length >= 2);

console.log(longNames); // ["田中", "佐藤", "鈴木"]
```

数组是 `string[]`，所以 `filter()` 知道回调参数 `name` 是字符串。这个根据使用位置得到的类型称为**上下文类型（contextual typing）**。

一般不需要把它重复写成 `filter((name: string) => ...)`。当编辑器无法正确推断或显式标注能明显提高可读性时再补充。

### 10.3 回调类型描述约定，不规定函数名

```ts
type TextHandler = (text: string) => void;

function processMessage(
  message: string,
  handler: TextHandler
): void {
  const normalizedMessage = message.trim();
  handler(normalizedMessage);
}

processMessage("  保存完成  ", text => {
  console.log(text); // 保存完成
});
```

`handler` 可以是任何符合“接收字符串、不要求业务返回值”的函数。函数名不是类型要求的一部分。

## 11. `void`：调用方不使用业务返回值

### 11.1 没有业务返回值的普通函数

```ts
function printMessage(message: string): void {
  console.log(message);
}

printMessage("保存完成"); // 保存完成
```

`void` 常用于输出、通知、更新页面等不需要向调用方提供业务结果的函数。本例没有写 `return`；JavaScript 运行时仍会自然返回 `undefined`。

显式声明返回值为 `void` 后，不能把业务数据作为函数结果返回：

```ts
function printMessage(message: string): void {
  console.log(message);
  // return message;
  // 错误：声明为void的普通函数不能返回字符串结果
}

printMessage("处理完成"); // 处理完成
```

### 11.2 回调中的 `void` 表示忽略结果

```ts
type MessageHandler = (message: string) => void;

const saveMessage = (message: string): number => {
  console.log(message);
  return message.length;
};

const handler: MessageHandler = saveMessage;
handler("保存完成"); // 保存完成
```

在回调类型 `() => void` 中，`void` 主要表示调用方不会使用返回结果。具体函数可能返回值并仍能赋给该回调类型，但通过 `handler` 调用时，不应依赖这个结果。

因此不要把 `void` 简单记成“运行时一定没有返回值”。对于自己声明的普通函数，它通常表示不应返回业务结果；对于回调约定，它表示调用者忽略结果。

## 12. `never`：函数不能正常结束

```ts
function stopProcessing(message: string): never {
  throw new Error(message);
}

try {
  stopProcessing("数据格式错误");
} catch (error) {
  if (error instanceof Error) {
    console.log(error.message); // 数据格式错误
  }
}
```

`never` 表示函数没有正常返回到调用位置的可能。`throw new Error(message)` 创建并抛出错误，立即中断当前流程，所以符合 `never`。

无限循环且永远不会结束的函数也可能返回 `never`，但业务代码中更常见的是始终抛出错误的函数。

`void` 和 `never` 不同：

| 类型 | 函数是否能正常结束 | 常见情况 |
| --- | --- | --- |
| `void` | 能 | 完成输出或更新后结束 |
| `never` | 不能 | 抛出错误或永久循环 |

第十三章还会使用 `never` 检查状态分支是否遗漏。


## 13. 常见错误与排查

### 13.1 只标注参数，不确认返回路径

函数存在多个分支时，检查每条正常路径是否都返回了约定类型。给重要函数明确写返回类型，可以更早发现遗漏。

### 13.2 把可选参数当成一定存在

`prefix?: string` 在函数内部是 `string | undefined`。使用前先判断，或根据业务改成具有默认值的参数。

### 13.3 参数过多且类型相同

多个连续字符串容易传错顺序。把相关字段组成有明确属性名的对象参数。

### 13.4 调用函数与传递函数混淆

`formatDays` 表示函数本身，`formatDays(5)` 表示立即调用后的字符串结果。需要函数变量或回调时，通常传前者。

### 13.5 在类型别名中把 `=>` 当成实现

`type Handler = (value: string) => void` 只描述类型，不会创建能够运行的函数。还需要函数声明或箭头函数提供实现。

### 13.6 把 `void` 与 `never` 混淆

`void` 函数可以正常结束，只是不提供业务结果；`never` 函数根本不会正常返回。


## 14. 本章练习

### 14.1 参数与返回值

编写 `calculateLeaveDays()`，接收开始天数和结束天数两个数字，返回包含首尾两天的休假天数。测试 `1, 1` 应返回 `1`，`3, 5` 应返回 `3`；再尝试传入字符串并观察错误。

### 14.2 可选参数与默认参数

编写问候函数：姓名必填，前缀可选，语言默认值为 `"ja"`。分别测试省略可选参数、传入前缀、修改默认语言，确认每个参数的作用。

### 14.3 对象参数

定义 `SearchCondition`，包含必填的 `keyword` 和可选的 `departmentCode`。编写函数输出检索条件，分别测试有部门和无部门两种对象。

### 14.4 剩余参数

编写函数接收一个消息前缀和任意数量的员工姓名，使用 `join()` 生成一行消息。测试零个、一个和三个姓名。

### 14.5 函数类型与回调

1. 定义接收数字、返回布尔值的 `NumberRule`。
2. 编写 `selectNumbers()`，通过 `filter()` 使用该规则。
3. 分别传入“偶数”和“大于 10”两个回调。
4. 输出输入数组和两个筛选结果，确认原数组没有改变。

### 14.6 排查函数类型错误

为下面几类错误分别编写最小示例，并记录修正方法：

- 缺少必填参数；
- 返回值类型错误；
- 把函数调用结果赋给函数变量；
- 把 `string` 返回函数赋给 `number` 返回函数类型。

## 本章检查点

- 能为函数声明和箭头函数标注参数与返回值。
- 能说明返回值推断什么时候足够、什么时候适合显式标注。
- 能解释 TypeScript 对必填参数数量的检查。
- 能区分可选参数和默认参数的运行时行为。
- 能使用对象参数减少位置参数混淆。
- 能使用剩余参数接收不定数量的同类值。
- 能定义函数类型，并区分类型中的箭头和实现中的箭头。
- 能解释回调函数的执行过程和上下文类型。
- 能区分普通函数与回调类型中 `void` 的含义。
- 能区分 `void` 和 `never`。
