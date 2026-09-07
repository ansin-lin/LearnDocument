# 第 3 章 数组与元组

上一章给单个变量标注了类型。实际程序还需要保存一组姓名、一组分数或一组固定位置的数据。本章学习怎样约束一组值中的元素，以及怎样在数组和元组之间作出选择。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。注释掉的错误代码用于观察编译提示，需要实验时再取消注释。

完成本章后，你应当能够：

- 声明、读取和修改指定元素类型的数组。
- 说明数组类型约束的是元素，不保证任意下标都有值。
- 根据初始值判断 TypeScript 推断出的数组类型。
- 区分普通数组和元组的用途。
- 看懂元组的必填、可选和剩余位置。
- 使用只读数组和只读元组限制意外修改。

## 1. 数组类型解决什么问题

JavaScript 数组允许加入不同类型的内容：

```js
const scores = [80, 90];
scores.push("优秀");
console.log(scores); // [80, 90, "优秀"]
```

这段代码可以运行，但字符串会破坏“所有元素都能参与分数计算”的前提。TypeScript 可以规定数组的元素类型：

```ts
const scores: number[] = [80, 90];
scores.push(100);
console.log(scores); // [80, 90, 100]
// scores.push("优秀"); // 错误：string不能作为number元素加入
```

`number[]` 读作“数字数组”。方括号写在元素类型后面：

```text
number[]
^^^^^^    元素类型是 number
      ^^  一组这样的元素
```

类型检查发生在开发和编译阶段。编译后的 JavaScript 仍然是普通数组，不会在浏览器中额外生成“TypeScript 数组”。

## 2. 声明不同元素类型的数组

### 2.1 基本写法

```ts
const employeeNames: string[] = ["田中", "佐藤"];
const scores: number[] = [80, 90];
const enabledFlags: boolean[] = [true, false, true];

console.log(employeeNames); // ["田中", "佐藤"]
console.log(scores);        // [80, 90]
console.log(enabledFlags);  // [true, false, true]
```

冒号左侧是变量，冒号右侧是数组类型，等号右侧才是实际数组值。

| 写法 | 允许的元素 | 不允许的元素示例 |
| --- | --- | --- |
| `string[]` | 字符串 | `100`、`true` |
| `number[]` | 数字 | `"100"`、`false` |
| `boolean[]` | `true`、`false` | `0`、`"true"` |

一个 `number[]` 可以有零个、一个或多个数字。它只规定元素类型，不规定固定长度。

### 2.2 空数组应明确元素类型

程序经常先准备空数组，再逐步加入数据：

```ts
const employeeNames: string[] = [];
employeeNames.push("田中");
employeeNames.push("佐藤");

console.log(employeeNames);        // ["田中", "佐藤"]
console.log(employeeNames.length); // 2
```

空数组中没有元素可供推断。明确写出 `string[]`，可以直接说明这个数组准备保存姓名，并阻止后续加入数字等错误值。

## 3. 数组类型推断

数组声明时已经有元素，TypeScript 通常会根据元素推断类型：

```ts
const prices = [100, 200];
prices.push(300);
console.log(prices); // [100, 200, 300]
// prices.push("免费"); // 错误：prices已推断为number[]
```

这里没有手写 `: number[]`，但 `[100, 200]` 提供了数字样本，因此 `prices` 被推断为 `number[]`。

声明时的元素类型不一致，TypeScript 会推断为能够包含这些元素的类型：

```ts
const mixedValues = [100, "pending"];
mixedValues.push(200);
mixedValues.push("approved");
console.log(mixedValues); // [100, "pending", 200, "approved"]
// mixedValues.push(true); // 错误：boolean不在推断结果中
```

这个数组会被推断为“元素可以是数字或字符串”。第五章会正式讲解这种联合类型。业务数组如果本来就应该只保存一种数据，不要为了消除报错而随意混入其他类型。

- 初始元素已经清楚表达类型时，可以使用推断。
- 初始值是空数组时，优先明确写出元素类型。
- 数组承担固定业务含义时，也可以保留类型标注帮助阅读。

## 4. 读取和修改数组

### 4.1 使用下标和 `length`

数组下标从 `0` 开始：

```ts
const names: string[] = ["田中", "佐藤", "鈴木"];

console.log(names[0]);     // 田中
console.log(names[1]);     // 佐藤
console.log(names.length); // 3
```

`names[0]` 读取第一项，`names[1]` 读取第二项，`length` 返回当前元素数量。可以通过下标修改已有位置：

```ts
const names: string[] = ["田中", "佐藤"];
names[1] = "鈴木";

console.log(names); // ["田中", "鈴木"]
// names[0] = 100; // 错误：只能写入字符串
```

### 4.2 常用增删方法也会检查元素类型

```ts
const tasks: string[] = ["设计"];

const lengthAfterPush = tasks.push("实现");
console.log(tasks);           // ["设计", "实现"]
console.log(lengthAfterPush); // 2

const removedTask = tasks.pop();
console.log(removedTask); // 实现
console.log(tasks);       // ["设计"]
```

- `push(value)` 在末尾加入元素，返回加入后的长度；参数必须符合元素类型。
- `pop()` 删除并返回最后一项；空数组没有可删除元素时返回 `undefined`。

数组开头也可以增删：

```ts
const tasks: string[] = ["实现"];

tasks.unshift("设计");
console.log(tasks); // ["设计", "实现"]

const removedTask = tasks.shift();
console.log(removedTask); // 设计
console.log(tasks);       // ["实现"]
```

- `unshift(value)` 在开头加入元素，返回新长度。
- `shift()` 删除并返回第一项；空数组时返回 `undefined`。

这些方法的运行行为来自 JavaScript。TypeScript 在此基础上检查加入的值，并描述可能为空的返回结果。

### 4.3 `const` 数组仍然可以修改内容

```ts
const names: string[] = ["田中"];
names.push("佐藤");
names[0] = "鈴木";

console.log(names); // ["鈴木", "佐藤"]
// names = ["高橋"]; // 错误：const变量不能重新赋值
```

`const` 禁止变量改为指向另一个数组，但不会禁止修改当前数组内部。需要限制内容修改时，使用第 8 节的只读数组。

## 5. 数组下标不保证一定有值

数组类型只说明“存在的元素是什么类型”，不保证任意下标都存在：

```ts
const scores: number[] = [80, 90];
const thirdScore = scores[2];

console.log(thirdScore); // undefined
```

数组只有下标 `0` 和 `1`，所以读取 `scores[2]` 的运行结果是 `undefined`。在默认配置下，TypeScript 可能仍把它看作 `number`，但这不代表运行时一定有值。

读取不确定下标前，应先检查范围：

```ts
const scores: number[] = [80, 90];
const index = 2;

if (index >= 0 && index < scores.length) {
  console.log(scores[index]);
} else {
  console.log("指定位置没有分数");
}
// 指定位置没有分数
```

第十二章会介绍 `noUncheckedIndexedAccess`。启用后，编译器会更主动地提醒下标读取可能得到 `undefined`。无论配置如何，程序都要处理来自输入等不确定下标。

## 6. 元组解决什么问题

### 6.1 普通数组只统一元素类型

普通数组适合保存数量可能变化、每项用途相同的数据：

```ts
const scores: number[] = [80, 90, 100];
```

如果两个位置分别表示“员工姓名”和“剩余休假天数”，位置含义和类型都不同：

```text
第 0 项：员工姓名，string
第 1 项：剩余天数，number
```

这时可以使用元组。

### 6.2 元组按位置规定类型和长度

```ts
const employeeSummary: [string, number] = ["田中", 12];

console.log(employeeSummary[0]); // 田中
console.log(employeeSummary[1]); // 12

// const wrongOrder: [string, number] = [12, "田中"];
// 错误：第0项应为string，第1项应为number

// console.log(employeeSummary[2]);
// 错误：这个元组没有下标2
```

`[string, number]` 是元组类型，`["田中", 12]` 是实际值。它规定两个已知位置，第 0 项是字符串，第 1 项是数字，顺序不能交换。

元组仍然会编译成普通 JavaScript 数组，不会在运行时变成新的数据结构。

### 6.3 什么时候适合使用元组

元组适合位置少、顺序稳定且含义明确的数据，例如二维坐标 `[x, y]`，或函数返回的简单成对结果。

```ts
const employee: [string, string, string] = [
  "EMP-001",
  "田中",
  "开发部",
];

console.log(employee[2]); // 开发部，但只看下标不容易知道含义
```

姓名、部门、状态等业务字段不断增加时，只靠下标会很难阅读。这种有明确字段名的数据通常更适合使用下一章的对象类型。

## 7. 元组的扩展写法（阅读）

已有代码中可能看到可选位置`[number, number?]`，或剩余位置`[string, ...string[]]`。前者表示第二项可以省略，后者表示第一项固定、后面可以继续出现字符串。

```ts
const point: [number, number?] = [10];
const members: [string, ...string[]] = ["负责人", "田中", "佐藤"];

console.log(point[1]);   // undefined
console.log(members[0]); // 负责人
```

普通业务列表优先使用数组，有明确字段名的数据优先使用对象。只有少量位置具有固定含义时才使用这些元组扩展写法。

## 8. 只读数组和只读元组

### 8.1 只读数组

```ts
const colors: readonly string[] = ["red", "blue"];

console.log(colors[0]);     // red
console.log(colors.length); // 2
// colors[0] = "green";    // 错误：只读数组不能写入位置
// colors.push("green");   // 错误：只读数组不能调用push()
```

`readonly string[]` 表示可以读取字符串元素，但不能通过这个变量修改数组内容。

### 8.2 只读元组

```ts
const point: readonly [number, number] = [10, 20];

console.log(point[0]); // 10
console.log(point[1]); // 20
// point[0] = 30;      // 错误：只读位置不能赋值
```

普通可变元组仍然是数组，某些修改方法可能让运行时长度发生变化。固定位置是数据约定时，优先使用只读元组可以减少意外修改。

`readonly` 是编译阶段对当前引用的限制，不等于运行时冻结，也不保证其他可变引用绝不会修改同一数组。当前重点是用它表达“这段代码不应修改此集合”。

## 9. 数组和元组怎样选择

| 需求 | 选择 | 示例 |
| --- | --- | --- |
| 多项用途相同、数量可能变化 | 数组 | 多个姓名使用 `string[]` |
| 少量位置各有固定含义和类型 | 元组 | 坐标使用 `[number, number]` |
| 固定结构不应被修改 | 只读元组 | `readonly [number, number]` |
| 多个有字段名的业务数据 | 下一章的对象数组 | 员工列表、申请列表 |

不要只因为“正好有两个值”就使用元组。关键是使用者能否稳定理解每个位置的意义。

## 10. 常见错误与排查

### 10.1 数组类型写错位置

```ts
// 错误示意：const names[]: string = ["田中"];
const names: string[] = ["田中"];
```

TypeScript 类型写在变量名后的冒号右侧，`[]` 跟在元素类型后面。

### 10.2 把数组下标当成从 1 开始

`names[0]` 才是第一项。读取 `names[names.length]` 一定越过最后一项，因为最后一项的下标是 `length - 1`。

### 10.3 认为 `const` 数组不能修改元素

`const` 只阻止变量重新赋值。需要限制 `push()` 和下标写入时，使用 `readonly` 数组类型。

### 10.4 用元组保存不断增加的业务字段

当代码中出现难以解释的 `record[3]`、`record[4]` 时，应考虑使用下一章的对象类型，让字段拥有名称。

### 10.5 忽略 `pop()` 和 `shift()` 可能返回 `undefined`

```ts
const tasks: string[] = [];
const removedTask = tasks.pop();

if (removedTask === undefined) {
  console.log("没有可删除的任务");
} else {
  console.log(`已删除：${removedTask}`);
}
// 没有可删除的任务
```

空数组没有元素可删除。使用返回值前，应先判断是否为 `undefined`。

## 11. 本章练习

### 11.1 数字数组

1. 创建一个只能保存数字的空数组。
2. 依次加入 `80`、`90` 和 `100`。
3. 输出数组长度和第二个元素。
4. 尝试加入字符串 `"优秀"`，记录编译错误后恢复正确代码。
5. 使用 `pop()` 删除最后一个分数，输出返回值和剩余数组。

### 11.2 观察下标风险

1. 创建只包含两个姓名的 `string[]`。
2. 读取下标 `2`，观察运行结果。
3. 改为先使用 `index < names.length` 检查范围，再读取元素。
4. 说明为什么 `string[]` 不等于“任意下标都一定是字符串”。

### 11.3 选择数组或元组

分别为下面的数据选择数组或元组，并说明原因：

1. 数量会变化的考试分数列表。
2. 固定的二维坐标，第一项为 X，第二项为 Y。
3. 第一项是负责人，后面是零个或多个参加者姓名。
4. 员工编号、姓名、部门组成的业务数据。

第 4 项只需判断应使用下一章的对象，不需要提前编写对象类型。

### 11.4 只读练习

1. 创建只读字符串数组并正常读取第一项。
2. 尝试调用 `push()`，确认编译器阻止修改。
3. 创建 `readonly [number, number]` 坐标。
4. 尝试修改第一个位置，记录错误后恢复代码。

## 本章检查点

- 能解释 `number[]` 中 `number` 和 `[]` 各自表示什么。
- 能声明非空数组和带明确元素类型的空数组。
- 能说明 TypeScript 怎样根据初始元素推断数组类型。
- 能读取、修改、追加和删除数组元素，并处理可能的 `undefined`。
- 能区分数组只约束元素类型与元组约束已知位置。
- 能说明可选位置、剩余位置和只读写法的用途。
- 能根据数据是否依赖固定位置，在数组、元组和后续对象类型之间作出选择。
