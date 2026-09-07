# 第 5 章 联合类型与字面量类型

前面学习的类型大多表示“一种可能”：`string` 表示字符串，`Employee` 表示符合员工结构的对象。但实际业务中，一个值有时允许多种形式，或者只能从几个固定值中选择。本章学习使用联合类型列出可能性，再使用字面量类型限制固定选项。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。注释掉的错误代码用于观察编译提示。

完成本章后，你应当能够：

- 使用联合类型表示一个值允许的多种类型。
- 使用字面量联合表示业务中的固定选项。
- 区分普通类型和字面量类型。
- 正确阅读带有联合类型的数组和对象属性。
- 说明联合类型只列出可能性，使用成员特有功能前仍要判断。

## 1. 联合类型解决什么问题

### 1.1 一项数据可能有多种合法形式

假设旧系统使用数字员工编号，新系统使用带字母的字符串编号。两种数据都合法，只写 `string` 或 `number` 都不能完整描述要求：

```ts
let employeeCode: string | number = 1001;
console.log(employeeCode); // 1001

employeeCode = "EMP-1001";
console.log(employeeCode); // EMP-1001

// employeeCode = true;
// 错误：boolean不属于string或number
```

`string | number` 是一个**联合类型（union type）**，读作“字符串或数字”。竖线 `|` 连接允许的类型，只要值符合其中一种类型就可以。

| 写法 | 所在位置 | 作用 |
| --- | --- | --- |
| `string | number` | 类型 | 声明允许的类型范围 |
| `left || right` | JavaScript 表达式 | 根据真值选择运行时结果 |

联合类型不会自动转换数据。数字 `1001` 在运行时仍是数字，字符串 `"EMP-1001"` 仍是字符串。

### 1.2 什么时候使用联合类型

联合类型适用于业务本来就允许多种情况的位置，例如：

- 编号兼容数字和字符串；
- 查询结果可能是对象，也可能是 `null`；
- 数据可能是字符串，也可能尚未提供；
- 状态只能从几个固定字符串中选择。

不要只为了消除错误，不断把类型追加到联合中。如果规格规定数量必须是数字，就应保留 `number` 并修正错误数据，而不是写成 `number | string | boolean`。

## 2. 联合类型的基本写法

### 2.1 联合成员可以超过两个

被 `|` 连接的每一种类型称为联合类型的一个**成员**：

```ts
let inputValue: string | number | null = null;
console.log(inputValue); // null

inputValue = "10";
console.log(inputValue); // 10

inputValue = 10;
console.log(inputValue); // 10
```

这里有 `string`、`number` 和 `null` 三个成员。变量允许三种情况，但在某个时刻保存的仍然只是一个实际值。

| 类型 | 常见含义 |
| --- | --- |
| `string | null` | 已取得结果，但结果可能明确为空 |
| `string | undefined` | 值可能尚未提供或属性可能不存在 |

具体使用哪一种，应遵守项目的数据规格，不要随意混用。

### 2.2 给联合类型起名字

第四章的 `type` 不只能命名对象，也能命名联合类型：

```ts
type EmployeeCode = string | number;

const firstCode: EmployeeCode = 1001;
const secondCode: EmployeeCode = "EMP-1002";

console.log(firstCode);  // 1001
console.log(secondCode); // EMP-1002
```

`EmployeeCode` 是类型别名，不是变量，也不会出现在编译后的 JavaScript 中。同一个联合会重复出现，或具有明确业务含义时，类型别名能让代码更容易阅读。

## 3. 联合类型只能使用各成员共有的功能

假设一个值的类型是 `string | number`。字符串可以调用 `toUpperCase()`，数字不能；两者都能调用 `toString()`。因此，尚未确认实际类型时，只能直接使用所有成员都支持的功能：

```ts
let code: string | number;

if (Math.random() > 0.5) {
  code = "emp-1001";
} else {
  code = 1001;
}

console.log(code.toString());
// console.log(code.toUpperCase());
// 错误：number没有toUpperCase方法
```

`Math.random()` 返回一个大于等于 `0`、小于 `1` 的随机数，所以运行时 `code` 可能得到字符串，也可能得到数字。错误并不是说字符串不能转大写，而是当前代码位置还不能确定它一定是字符串。第六章会学习先判断类型，再使用对应方法。

如果业务要求统一得到文本，可以进行真实转换：

```ts
let code: string | number = 1001;
const textCode = String(code);

console.log(textCode);        // 1001
console.log(typeof textCode); // string
```

`String(code)` 是 JavaScript 的字符串转换函数，会返回转换后的字符串。类型标注只用于编译检查，不会完成转换。

## 4. 字面量类型限制一个具体值

### 4.1 普通类型与字面量类型

`string` 允许任意字符串，而字符串字面量类型只允许指定字符串：

```ts
let mode: "view" = "view";
console.log(mode); // view

// mode = "edit";
// 错误：只能保存view
```

`let mode: "view" = "view"` 中，冒号后的 `"view"` 是类型，等号后的 `"view"` 是实际值。数字和布尔值也能成为字面量类型：

```ts
let retryCount: 3 = 3;
let featureEnabled: true = true;

console.log(retryCount);     // 3
console.log(featureEnabled); // true
```

单独限制一个值的场景不多，更常见的是把多个字面量组成联合。

### 4.2 `const` 为什么会推断得更具体

```ts
const fixedStatus = "pending";
let changeableStatus = "pending";

changeableStatus = "approved";

console.log(fixedStatus);      // pending
console.log(changeableStatus); // approved
```

`fixedStatus` 不能重新赋值，所以 TypeScript 可以保留更具体的 `"pending"` 类型。`changeableStatus` 以后可以保存其他字符串，所以通常被推断为更宽的 `string`。

本节只需要理解“能否重新赋值会影响推断结果”。对象和数组怎样整体保留字面量类型，将在第十一章的 `as const` 中讲解。

## 5. 字面量联合表示固定选项

审批状态不是任意字符串，而是有限的几种值：

```ts
type RequestStatus = "pending" | "approved" | "rejected";

let requestStatus: RequestStatus = "pending";
console.log(requestStatus); // pending

requestStatus = "approved";
console.log(requestStatus); // approved

// requestStatus = "completed"; // 不在允许范围内
// requestStatus = "Approved";  // 大小写不一致
```

这种类型称为**字面量联合**。编辑器可以提示候选值，编译器也能发现拼写、大小写和规格外取值。常见用途包括：

| 业务含义 | 类型示例 |
| --- | --- |
| 审批状态 | `"pending" | "approved" | "rejected"` |
| 排序方向 | `"asc" | "desc"` |
| 页面显示方式 | `"list" | "grid"` |
| 消息级别 | `"info" | "warning" | "error"` |
| 重试次数选项 | `0 | 1 | 2 | 3` |

实际取值必须与接口或业务规格完全一致。第十一章还会比较字面量联合与 `enum`。

## 6. 在对象属性中使用联合类型

对象的某个属性也可以声明为联合类型：

```ts
type RequestStatus = "pending" | "approved" | "rejected";

type LeaveRequest = {
  requestId: string;
  employeeCode: string | number;
  reason: string | null;
  status: RequestStatus;
};

const request: LeaveRequest = {
  requestId: "REQ-001",
  employeeCode: 1001,
  reason: null,
  status: "pending",
};

console.log(request.employeeCode); // 1001
console.log(request.reason);       // null
console.log(request.status);       // pending
```

`employeeCode` 允许两种编号形式，`reason` 允许文字或明确空值，`status` 只能使用三个固定字符串。联合类型只应写在确实存在多种可能的属性上。

## 7. 联合类型与数组

### 7.1 每个数组元素允许多种类型

```ts
const codes: (string | number)[] = [1001, "EMP-1002", 1003];

console.log(codes[0]); // 1001
console.log(codes[1]); // EMP-1002
```

`(string | number)[]` 表示数组中的每一项都可以是字符串或数字。括号先把联合类型组合成整体，再由 `[]` 表示数组。

`Array<string | number>` 与它含义相同：

```ts
const codes: Array<string | number> = [1001, "EMP-1002"];
console.log(codes.length); // 2
```

本课程主线优先使用 `(string | number)[]`。

### 7.2 括号会改变类型含义

```ts
const mixedItems: (string | number)[] = ["A", 1, "B", 2];

let textOrNumberList: string | number[] = "没有数据";
console.log(textOrNumberList); // 没有数据

textOrNumberList = [10, 20, 30];
console.log(textOrNumberList); // [10, 20, 30]
```

| 类型 | 含义 | 合法值示例 |
| --- | --- | --- |
| `(string | number)[]` | 数组的每一项是字符串或数字 | `["A", 1]` |
| `string | number[]` | 整个值是字符串，或者是纯数字数组 | `"none"`、`[1, 2]` |

看到联合类型与数组同时出现时，先确认 `|` 连接的是元素类型，还是整个值的类型。

## 8. 联合类型不会验证外部数据

```ts
type RequestStatus = "pending" | "approved" | "rejected";

const requestStatus: RequestStatus = "pending";
console.log(requestStatus); // pending
```

这个类型能检查 TypeScript 源代码中的赋值，却不会自动检查接口、表单或本地存储传入的字符串。外部系统仍可能返回 `"finished"` 或空字符串。

因此需要区分：

- 联合类型负责描述允许范围；
- TypeScript 在编译时检查源代码；
- 外部数据到达运行时后仍需实际验证；
- 类型断言不能代替验证。

第六章会学习通过条件判断缩小联合类型；第十五章会把运行时检查应用到请求数据。

## 9. 常见错误与排查

### 9.1 把 `|` 写成 `||`

类型联合使用单竖线 `|`。双竖线 `||` 是运行时逻辑运算符，不能连接类型。

### 9.2 联合范围写得过宽

状态写成 `string | null` 后，任何字符串都会通过检查，无法阻止状态拼写错误。固定选项应使用字面量联合。

### 9.3 调用某个成员独有的方法

看到“某类型上不存在某方法”时，检查变量是否为联合类型。若操作只适用于其中一个成员，需要先判断实际类型，不能用断言压过错误。

### 9.4 混淆 `(A | B)[]` 和 `A | B[]`

前者是“由 A 或 B 组成的数组”，后者是“一个 A 或一个 B 数组”。括号位置决定 `[]` 修饰的范围。

### 9.5 误以为类型会修改数据

`string | number` 不会把数字转换为字符串，字面量联合也不会把错误文本改成合法状态。转换和验证都需要实际运行的代码。

## 10. 本章练习

### 10.1 编号的两种形式

1. 定义 `EmployeeCode`，允许字符串或数字。
2. 创建一个数字编号和一个字符串编号并输出。
3. 尝试赋入布尔值，确认编译器报错后恢复。
4. 使用 `String()` 转换数字编号，输出转换前后的 `typeof`。

### 10.2 固定的申请状态

1. 定义 `RequestStatus`，只允许 `"pending"`、`"approved"` 和 `"rejected"`。
2. 声明变量并依次赋入两个合法状态。
3. 尝试赋入 `"completed"` 和 `"Approved"`，分别观察错误。
4. 说明为什么改成 `string` 会失去检查效果。

### 10.3 完善对象类型

定义 `LeaveRequest`，包含字符串 `requestId`、字符串或数字 `employeeCode`、字符串或 `null` 的 `reason`，以及 `RequestStatus` 类型的 `status`。创建合法数据并输出，再分别尝试错误状态、布尔编号和数字理由，观察错误并修正。

### 10.4 区分两种数组类型

1. 创建 `(string | number)[]`，在同一个数组中保存字符串和数字。
2. 创建 `string | number[]`，先保存字符串，再改为数字数组。
3. 分别尝试把 `["A", 1]` 赋给第二个变量、把单独字符串赋给第一个变量。
4. 用一句话说明两种类型的区别。

## 本章检查点

- 能解释联合类型中的 `|`，并与 `||` 区分。
- 能根据业务要求决定是否使用联合类型。
- 能使用类型别名为联合类型命名。
- 能说明为什么不能直接使用某个联合成员独有的方法。
- 能区分普通类型、字面量类型和字面量联合。
- 能使用字面量联合限制固定选项。
- 能在对象属性中使用联合类型。
- 能区分 `(string | number)[]` 与 `string | number[]`。
- 能说明联合类型不会转换数据，也不会验证外部输入。
