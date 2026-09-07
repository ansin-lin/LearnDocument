# 第 6 章 类型收窄与安全判断

第五章可以用联合类型列出一个值的所有可能性，但列出可能性之后，还不能直接使用某一种类型专有的属性或方法。本章解决的问题是：怎样通过真正会运行的判断，确认当前值究竟属于哪一种类型。

第二章已经介绍过类型断言的基本语法。本章会把“实际判断”和“直接断言”放在一起比较，重点掌握安全判断，避免用断言掩盖数据问题。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。函数参数和返回值后的类型标注，本章只用于承载联合类型；函数类型的完整知识将在第七章讲解。

完成本章后，你应当能够：

- 解释类型收窄不会转换实际数据。
- 使用 `typeof`、相等判断和控制流程收窄基本类型与空值。
- 使用 `in`、`instanceof` 和判别属性区分对象。
- 使用 `Array.isArray()` 判断数组。
- 编写简单的自定义类型守卫检查未知数据。
- 说明普通断言和非空断言的风险，并优先选择真实判断。

## 1. 为什么联合类型需要收窄

### 1.1 编译器必须照顾所有可能性

```ts
function formatCode(code: string | number): string {
  // return code.toUpperCase();
  // 错误：number没有toUpperCase方法
  return code.toString();
}

console.log(formatCode("emp-001")); // emp-001
console.log(formatCode(1001));      // 1001
```

`formatCode()` 接收字符串或数字。`toUpperCase()` 只有字符串支持，因此它不能保证对所有合法输入都能运行；`toString()` 两者都支持，所以可以直接调用。

如果想对字符串转大写、对数字增加固定前缀，就要先判断实际类型。

### 1.2 判断使可能范围变小

```ts
function formatCode(code: string | number): string {
  if (typeof code === "string") {
    return code.toUpperCase();
  }

  return `NO-${code}`;
}

console.log(formatCode("emp-001")); // EMP-001
console.log(formatCode(1001));      // NO-1001
```

`typeof code === "string"` 是运行时判断。执行过程如下：

| 代码位置 | 编译器理解的类型 | 原因 |
| --- | --- | --- |
| 进入函数 | `string | number` | 两种输入都合法 |
| `if` 内部 | `string` | 条件已经确认是字符串 |
| `if` 后面 | `number` | 字符串分支已经 `return` |

这种根据条件和执行路径缩小可能类型范围的过程称为**类型收窄（narrowing）**。

收窄不会把数字转换成字符串，也不会修改变量的实际值。它只是让 TypeScript 在某一段代码中获得更准确的信息。

## 2. 使用 `typeof` 收窄基本类型

### 2.1 `typeof` 会返回什么

JavaScript 的 `typeof` 运算符在运行时返回类型名称字符串：

```ts
const employeeName = "田中";
const remainingDays = 10;
const approved = false;

console.log(typeof employeeName);  // string
console.log(typeof remainingDays); // number
console.log(typeof approved);      // boolean
```

常见返回结果如下：

| 判断写法 | 确认的值 |
| --- | --- |
| `typeof value === "string"` | 字符串 |
| `typeof value === "number"` | 数字，包括 `NaN` |
| `typeof value === "boolean"` | 布尔值 |
| `typeof value === "undefined"` | `undefined` |
| `typeof value === "function"` | 函数 |
| `typeof value === "object"` | 对象，也可能是数组或 `null` |

注意，`typeof null` 的结果也是 `"object"`。检查对象时不能只写 `typeof value === "object"`，还要排除 `null`。

### 2.2 对每种类型分别处理

```ts
function showInput(value: string | number | boolean): string {
  if (typeof value === "string") {
    return value.trim();
  }

  if (typeof value === "number") {
    return value.toFixed(2);
  }

  return value ? "是" : "否";
}

console.log(showInput("  hello  ")); // hello
console.log(showInput(12.5));         // 12.50
console.log(showInput(false));        // 否
```

`trim()` 返回去除首尾空白后的字符串；`toFixed(2)` 返回保留两位小数的字符串。前两个分支分别排除了字符串和数字，因此最后只剩布尔值。

## 3. 收窄 `null` 与 `undefined`

### 3.1 明确检查缺失值

```ts
function showReason(reason: string | null): string {
  if (reason === null) {
    return "未填写理由";
  }

  return reason.trim();
}

console.log(showReason(null));          // 未填写理由
console.log(showReason("  私用  "));   // 私用
```

`reason === null` 只检查 `null`。进入后续代码意味着空值分支已经结束，所以 `reason` 被收窄为 `string`。

对于 `undefined`，也应使用明确判断：

```ts
function showDepartment(name: string | undefined): string {
  if (name === undefined) {
    return "部门未设置";
  }

  return name;
}

console.log(showDepartment(undefined)); // 部门未设置
console.log(showDepartment("开发部"));  // 开发部
```

### 3.2 真值判断可能误伤合法值

```ts
function showRemainingDays(days: number | undefined): string {
  if (days === undefined) {
    return "未取得";
  }

  return `${days}日`;
}

console.log(showRemainingDays(undefined)); // 未取得
console.log(showRemainingDays(0));         // 0日
console.log(showRemainingDays(5));         // 5日
```

如果改成 `if (!days)`，`undefined` 和数字 `0` 都会满足条件。但剩余天数为零是合法业务数据，不应被当成“未取得”。空字符串和 `false` 也是假值；它们有业务含义时，应明确检查 `null` 或 `undefined`。

真值判断适合“所有假值都按同一种方式处理”的场景，不能机械替代空值检查。

## 4. 相等判断与字面量类型收窄

第五章用字面量联合表示固定状态。与具体字面量比较后，当前分支中的类型也会变得更具体：

```ts
type RequestStatus = "pending" | "approved" | "rejected";

function getStatusText(status: RequestStatus): string {
  if (status === "pending") {
    return "审批中";
  }

  if (status === "approved") {
    return "已批准";
  }

  return "已驳回";
}

console.log(getStatusText("pending"));  // 审批中
console.log(getStatusText("approved")); // 已批准
console.log(getStatusText("rejected")); // 已驳回
```

第一个分支中，`status` 只能是 `"pending"`；继续向后执行时，这一可能性已被排除。最后只剩 `"rejected"`，所以不需要再做第四种猜测。

固定状态较多时也可以使用 JavaScript 的 `switch`：

```ts
type RequestStatus = "pending" | "approved" | "rejected";

function getStatusText(status: RequestStatus): string {
  switch (status) {
    case "pending":
      return "审批中";
    case "approved":
      return "已批准";
    case "rejected":
      return "已驳回";
  }
}

console.log(getStatusText("rejected")); // 已驳回
```

每个 `case` 都会把 `status` 收窄为对应的字面量。更完整的状态建模和遗漏分支检查将在第十三章讲解。

## 5. 使用 `in` 根据属性区分对象

### 5.1 判断对象是否具有某个属性

```ts
type Employee = {
  employeeCode: string;
  employeeName: string;
};

type Department = {
  departmentCode: string;
  departmentName: string;
};

function showTarget(target: Employee | Department): string {
  if ("employeeName" in target) {
    return `员工：${target.employeeName}`;
  }

  return `部门：${target.departmentName}`;
}

console.log(showTarget({
  employeeCode: "EMP-001",
  employeeName: "田中",
})); // 员工：田中

console.log(showTarget({
  departmentCode: "DEV",
  departmentName: "开发部",
})); // 部门：开发部
```

`"employeeName" in target` 是运行时属性检查：如果能在对象或其原型链上找到 `employeeName`，结果就是 `true`。这里只在 `Employee` 中声明了这个必填属性，所以 TypeScript 可以区分两个对象类型。

### 5.2 `in` 只确认属性存在

`in` 不会确认属性值是不是正确类型。如果数据来自接口或存储，还要继续检查字段值，例如确认 `typeof value.employeeName === "string"`。

如果联合成员都包含同名可选属性，单靠这个属性也未必能准确区分。结构可控时，通常使用下一节的固定判别属性更清楚。

## 6. 使用判别属性区分对象

### 6.1 给每种对象设置固定标记

```ts
type SuccessResult = {
  resultType: "success";
  message: string;
};

type ErrorResult = {
  resultType: "error";
  errorCode: number;
};

type SaveResult = SuccessResult | ErrorResult;

function showResult(result: SaveResult): string {
  if (result.resultType === "success") {
    return result.message;
  }

  return `错误代码：${result.errorCode}`;
}

console.log(showResult({
  resultType: "success",
  message: "保存成功",
})); // 保存成功

console.log(showResult({
  resultType: "error",
  errorCode: 400,
})); // 错误代码：400
```

两个对象都有 `resultType`，但它们使用不同的字面量类型。这样的共同固定字段称为**判别属性**，整个联合称为**可辨识联合**或**判别联合（discriminated union）**。

判断 `result.resultType` 后，TypeScript 就知道同一对象还具有什么属性。它比通过某个业务字段是否存在来猜测对象类型更明确，适合成功/失败、加载状态、不同操作结果等场景。

## 7. 使用 `instanceof` 判断类实例

```ts
function formatDate(value: Date | string): string {
  if (value instanceof Date) {
    return value.toISOString();
  }

  return value;
}

console.log(formatDate(new Date("2026-09-01T10:00:00+09:00")));
// 2026-09-01T01:00:00.000Z
console.log(formatDate("2026-09-01"));
// 2026-09-01
```

`new Date(...)` 创建 JavaScript 日期对象。`value instanceof Date` 在运行时检查 `value` 是否通过 `Date` 构造函数创建；成立时，值被收窄为 `Date`，可以调用 `toISOString()` 返回 UTC 格式字符串。

需要注意：

- `instanceof` 适用于运行时真实存在的类或构造函数；
- `type` 类型别名编译后会消失，不能写 `value instanceof Employee`；
- `instanceof Date` 只确认是日期对象，不保证日期内容有效。

课程第九章会详细学习类，本节先掌握内置 `Date` 的判断方式。

## 8. 使用 `Array.isArray()` 判断数组

`typeof` 无法把普通对象和数组区分开，因为数组的 `typeof` 结果也是 `"object"`。应使用 `Array.isArray()`：

```ts
function countItems(value: string | string[]): number {
  if (Array.isArray(value)) {
    return value.length;
  }

  return value.length;
}

console.log(countItems(["A", "B", "C"])); // 3
console.log(countItems("hello"));          // 5
```

`Array.isArray(value)` 是 JavaScript 的静态方法，接收要检查的值，返回布尔值。结果为 `true` 时，TypeScript 会把这里的 `value` 收窄为数组。

虽然本例两个分支都读取 `length`，含义不同：数组得到元素个数，字符串得到字符数量。

## 9. 控制流程也会影响收窄

TypeScript 不只看单个 `if` 条件，还会跟踪 `return`、`throw` 等执行路径：

```ts
function normalizeName(name: string | undefined): string {
  if (name === undefined) {
    throw new Error("姓名不能为空");
  }

  return name.trim();
}

try {
  console.log(normalizeName("  田中  ")); // 田中
  console.log(normalizeName(undefined));
} catch (error) {
  if (error instanceof Error) {
    console.log(error.message); // 姓名不能为空
  }
}
```

`throw new Error(...)` 创建并抛出错误，中断当前函数的正常执行。能到达 `return name.trim()`，就说明 `undefined` 分支已经结束，因此 `name` 是字符串。

这种先处理异常情况并退出、再编写正常流程的写法，通常比层层嵌套更容易阅读。

## 10. 从 `unknown` 开始检查外部数据

### 10.1 为什么外部值适合先用 `unknown`

接口响应、JSON 解析结果和存储内容在运行时可能不符合预期。将尚未确认的数据视为 `unknown`，TypeScript 会要求先检查再使用：

```ts
const receivedValue: unknown = "hello";

if (typeof receivedValue === "string") {
  console.log(receivedValue.toUpperCase()); // HELLO
}
```

`unknown` 表示“目前不知道是什么类型”。它可以接收任何值，但不能在未经检查时直接调用字符串方法或读取对象属性。相比之下，`any` 会跳过检查，更容易把问题留到运行时。

### 10.2 编写简单的自定义类型守卫

重复检查可以封装成函数：

```ts
type Employee = {
  employeeCode: string;
  employeeName: string;
};

function isEmployee(value: unknown): value is Employee {
  return typeof value === "object" &&
    value !== null &&
    "employeeCode" in value &&
    typeof value.employeeCode === "string" &&
    "employeeName" in value &&
    typeof value.employeeName === "string";
}

const firstValue: unknown = {
  employeeCode: "EMP-001",
  employeeName: "田中",
};

const secondValue: unknown = {
  employeeCode: 1001,
  employeeName: "佐藤",
};

console.log(isEmployee(firstValue));  // true
console.log(isEmployee(secondValue)); // false

if (isEmployee(firstValue)) {
  console.log(firstValue.employeeName); // 田中
}
```

返回类型 `value is Employee` 称为**类型谓词（type predicate）**，表示“函数返回 `true` 时，参数 `value` 可以视为 `Employee`”。`isEmployee()` 本身仍然在运行时返回布尔值。

判断顺序不能随意颠倒：

1. `typeof value === "object"`：先确认是对象；
2. `value !== null`：排除 `null`；
3. `"属性名" in value`：确认属性存在；
4. `typeof value.属性名 === ...`：确认属性值类型。

`&&` 会从左向右判断，前面为 `false` 时不再执行后面，因此后续属性检查是安全的。

类型谓词是写给编译器的承诺。若函数没有真正检查完整结构，却错误地返回 `true`，TypeScript 不会替你发现这份承诺是假的。

## 11. 类型断言为什么不是收窄的首选

### 11.1 普通类型断言 `as`

第二章已经介绍过，类型断言要求编译器按指定类型理解一个值：

```ts
const value: unknown = "hello";
const text = value as string;

console.log(text.toUpperCase()); // HELLO
```

这里的实际值确实是字符串，所以代码能够运行。但 `as string` 没有检查值，也没有转换值：

```ts
const value: unknown = 100;
const text = value as string;

console.log(typeof text); // number
// console.log(text.toUpperCase());
// 编译器允许，但取消注释后运行时会报错
```

断言只改变编译器的看法，真实值仍是数字。来自接口、表单和存储的数据，应使用真实判断或可靠的校验工具，不能直接断言为期望类型。

### 11.2 非空断言 `!`

```ts
function upperName(name: string | undefined): string {
  return name!.toUpperCase();
}

console.log(upperName("tanaka")); // TANAKA
// console.log(upperName(undefined));
// 编译器允许调用，但运行时会报错
```

值后面的 `!` 称为**非空断言**，告诉编译器暂时排除 `null` 和 `undefined`。它不是逻辑非运算，也不会在运行时补默认值。

更安全的写法是明确处理缺失情况：

```ts
function upperName(name: string | undefined): string {
  if (name === undefined) {
    return "未设置";
  }

  return name.toUpperCase();
}

console.log(upperName("tanaka")); // TANAKA
console.log(upperName(undefined)); // 未设置
```

只有在程序逻辑已经由其他可靠条件保证值必定存在，而 TypeScript 无法得知时，才谨慎使用非空断言，并在代码旁保留能够验证该保证的依据。

### 11.3 判断、转换和断言的区别

| 操作 | 示例 | 运行时是否执行 | 是否验证实际值 | 主要用途 |
| --- | --- | --- | --- | --- |
| 类型判断 | `typeof value === "string"` | 是 | 是，检查当前条件 | 确认后安全使用 |
| 数据转换 | `String(value)` | 是 | 不是验证，会生成转换结果 | 得到另一种形式的数据 |
| 类型断言 | `value as string` | 否 | 否 | 补充编译器无法推断的信息 |
| 非空断言 | `value!` | 否 | 否 | 声明值不会为空 |

遇到类型错误时，不要首先想到 `as` 或 `!`。先判断数据是否真的可能有多种类型、是否缺少必要检查，以及类型定义是否与业务规格一致。

## 12. 怎样选择收窄方式

| 需要确认的内容 | 常用方式 |
| --- | --- |
| 字符串、数字、布尔值、`undefined` | `typeof` |
| `null` 或具体固定值 | `===`、`!==` |
| 对象是否有某属性 | `in` |
| 对象联合有固定标记字段 | 判断判别属性 |
| 某个类创建的实例 | `instanceof` |
| 是否为数组 | `Array.isArray()` |
| 多处重复使用的结构检查 | 自定义类型守卫 |

选择标准是运行时真正能够观察到什么。类型别名和接口会在编译后消失，不能直接拿来做运行时判断。

## 13. 常见错误与排查

### 13.1 检查对象时忘记排除 `null`

`typeof null === "object"`，所以读取属性前要同时确认 `value !== null`。

### 13.2 用真值判断处理所有缺失情况

检查 `0`、空字符串或 `false` 是否是合法业务值。若是，应明确比较 `null` 或 `undefined`。

### 13.3 以为 `in` 会检查属性值类型

`"name" in value` 只能确认属性存在。处理未知数据时，还要检查 `typeof value.name`。

### 13.4 对类型别名使用 `instanceof`

`type Employee` 只存在于编译阶段，不能参与运行时判断。类实例使用 `instanceof`，普通对象结构使用属性和值检查。

### 13.5 类型守卫承诺与实际检查不一致

类型谓词不会自动证明函数实现正确。类型中有几个必要字段，就应逐项检查；业务上的非空、范围和格式要求也需要另行验证。

### 13.6 用断言消除所有红线

断言可能让错误暂时消失，却把风险推迟到运行时。先阅读错误信息，修正类型、控制流程或数据来源。

## 14. 本章练习

### 14.1 收窄基本类型

编写 `formatEmployeeCode()`，接收 `string | number`：字符串转为大写，数字前添加 `NO-`。分别使用两种输入验证结果，再尝试删除 `typeof` 判断并观察错误。

### 14.2 正确处理缺失值

编写函数接收 `number | undefined`，`undefined` 返回 `"未取得"`，其他数字返回 `"N日"`。至少测试 `undefined`、`0` 和 `5`，确认没有把零当成缺失。

### 14.3 区分两种结果对象

创建带有 `resultType` 判别属性的成功对象和失败对象。成功对象包含消息，失败对象包含错误代码。编写判断并输出对应内容，再尝试在错误分支读取成功对象专有属性。

### 14.4 检查未知员工数据

修改 `isEmployee()`，增加数字类型的 `remainingLeaveDays` 字段检查。依次测试：

- 完整且类型正确的对象；
- 缺少字段的对象；
- 天数为字符串的对象；
- `null`；
- 普通字符串。

记录每次返回结果，并说明检查顺序为什么不能从读取属性开始。

### 14.5 修复危险断言

将第 11.1 节第二个示例改为不使用 `as string`：先判断 `value`，字符串转大写，其他类型输出 `"不是字符串"`。测试字符串和数字，确认两条路径都不会发生运行时错误。

## 本章检查点

- 能解释收窄前后编译器理解的类型如何变化。
- 能说明类型收窄不会修改或转换实际数据。
- 能使用 `typeof` 处理基本类型联合。
- 能明确区分空值检查和真值判断。
- 能使用相等判断处理字面量联合。
- 能使用 `in`、判别属性和 `instanceof` 区分对象。
- 能使用 `Array.isArray()` 判断数组。
- 能从 `unknown` 开始逐步检查对象结构。
- 能解释类型谓词的含义和责任。
- 能区分判断、转换、普通断言和非空断言。
- 能在真实判断可行时避免不必要的 `as` 与 `!`。
