# 第 10 章 泛型

有些函数和数据结构的处理方式完全相同，只是内部保存的数据类型不同。例如“返回传入值”“取得数组第一项”“把一个值转换成另一个值”，都不应该为字符串、数字和对象各复制一份实现。泛型可以先用类型参数表示暂时未确定的类型，并在每次使用时保留具体的类型关系。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。注释掉的错误代码用于观察编译提示。

完成本章后，你应当能够：

- 说明泛型与 `any`、联合类型的区别。
- 编写并调用基础泛型函数。
- 让 TypeScript 推断类型参数，并在必要时显式指定。
- 使用泛型安全处理数组和回调转换。
- 使用多个类型参数表达不同位置之间的关系。
- 定义泛型类型别名和接口。
- 使用泛型约束限制类型参数必须具备的能力。
- 判断什么时候不需要使用泛型。

## 1. 泛型解决什么问题

### 1.1 `any` 会丢失输入与输出的关系

下面的函数原样返回输入值：

```ts
function identity(value: any): any {
  return value;
}

const employeeName = identity("田中");
const remainingDays = identity(10);

console.log(employeeName.toUpperCase()); // 田中
console.log(remainingDays + 1);           // 11
```

代码暂时能够运行，但 `identity()` 的返回类型是 `any`。TypeScript 不再知道第一次结果是字符串、第二次结果是数字，因此下面的错误也可能通过编译：

```ts
function identity(value: any): any {
  return value;
}

const remainingDays = identity(10);

// 编译器不会阻止，但运行时会报错
// console.log(remainingDays.toUpperCase());
console.log(remainingDays); // 10
```

`any` 的问题不只是“可以接收任意值”，而是它会关闭后续关键检查。

### 1.2 联合类型会把每次结果都变成多种可能

```ts
function identity(value: string | number): string | number {
  return value;
}

const result = identity("hello");

// console.log(result.toUpperCase());
// 错误：返回类型仍是string | number
console.log(result.toString()); // hello
```

虽然这次明确传入字符串，但函数声明只说明“输入和输出都可能是字符串或数字”，没有表达“输出与本次输入是同一种类型”。而且以后增加对象、数组时，还要继续扩大联合。

### 1.3 泛型保留类型关系

```ts
function identity<T>(value: T): T {
  return value;
}

const employeeName = identity("tanaka");
const remainingDays = identity(10);

console.log(employeeName.toUpperCase()); // TANAKA
console.log(remainingDays + 1);           // 11
```

`T` 是一个**类型参数（type parameter）**，可以理解为类型位置上的占位符：

```text
function identity<T>(value: T): T
//                ^ 输入类型 ^ 返回类型
```

第一次调用时，`T` 被确定为字符串；第二次调用时，`T` 被确定为数字。函数逻辑只写一份，但每次调用都保留了具体类型。

泛型的重点不是“接受任意类型”，而是**表达多个位置之间的类型关系**。

## 2. 泛型函数的基本写法

### 2.1 声明类型参数

```ts
function wrapValue<T>(value: T): T[] {
  return [value];
}

const nameList = wrapValue("田中");
const dayList = wrapValue(10);

console.log(nameList); // ["田中"]
console.log(dayList);  // [10]
```

逐段阅读 `function wrapValue<T>(value: T): T[]`：

1. `<T>` 声明类型参数；
2. `value: T` 表示参数使用本次确定的类型；
3. `T[]` 表示返回由同类型元素组成的数组。

`T` 不会成为运行时变量，也不会出现在编译后的 JavaScript 中。

### 2.2 类型参数名称

简单泛型常使用短名称：

| 名称 | 常见含义 |
| --- | --- |
| `T` | Type，某个通用类型 |
| `K` | Key，键的类型 |
| `V` | Value，值的类型 |
| `E` | Element，元素类型 |

名称只是惯例，不是固定关键字。业务关系复杂时应使用更明确的名称：

```ts
function createList<ElementType>(value: ElementType): ElementType[] {
  return [value];
}

console.log(createList("开发部")); // ["开发部"]
```

无论使用 `T` 还是 `ElementType`，关键是同一个名称出现的位置代表同一种类型。

## 3. 类型参数的推断与显式指定

### 3.1 通常让 TypeScript 推断

```ts
function identity<T>(value: T): T {
  return value;
}

const text = identity("hello");
const count = identity(10);
const employee = identity({
  employeeCode: "EMP-001",
  employeeName: "田中",
});

console.log(text.toUpperCase());       // HELLO
console.log(count.toFixed(2));         // 10.00
console.log(employee.employeeName);    // 田中
```

TypeScript 根据实参推断本次 `T`。能够正确推断时，不必重复写类型参数。

### 3.2 必要时显式指定类型参数

```ts
function createEmptyList<T>(): T[] {
  return [];
}

const employeeCodes = createEmptyList<string>();
employeeCodes.push("EMP-001");

console.log(employeeCodes); // ["EMP-001"]
```

`createEmptyList()` 没有输入参数，TypeScript 无法从实参推断 `T`，所以调用时使用 `<string>` 明确指定元素类型。

显式类型参数写在函数名与调用括号之间：

```ts
function createEmptyList<T>(): T[] {
  return [];
}

const values = createEmptyList<number>();
//                              ^ 类型参数
//                                      ^ 运行时参数列表

values.push(10);
console.log(values); // [10]
```

`<number>` 只参与编译检查，不是运行时实参。

### 3.3 显式指定后，实参必须匹配

```ts
function identity<T>(value: T): T {
  return value;
}

const value = identity<number>(10);
console.log(value); // 10

// identity<number>("10");
// 错误：已经明确要求T是number
```

不要用显式类型参数强迫错误数据符合期望类型。出现不匹配时，应确认实参、业务规格和类型参数是否正确。

## 4. 泛型函数内部只能使用已知能力

### 4.1 `T` 并不保证具有某个属性

```ts
function printValue<T>(value: T): void {
  console.log(value);

  // console.log(value.length);
  // 错误：T可能是number等没有length的类型
}

printValue("hello"); // hello
printValue(10);      // 10
```

`T` 可以在不同调用中代表不同类型。函数实现必须对所有允许的 `T` 都成立，因此不能未经限制就读取 `length`、`id` 等属性。

泛型不会让函数内部获得“所有类型的所有方法”。它反而要求实现只使用当前类型参数明确保证的能力。第九节会通过泛型约束增加这种保证。

### 4.2 与 `unknown` 的区别

```ts
function keepValue<T>(value: T): T {
  return value;
}

function inspectValue(value: unknown): string {
  return typeof value;
}

const keptValue = keepValue("hello");
const inspectedType = inspectValue("hello");

console.log(keptValue.toUpperCase()); // HELLO
console.log(inspectedType);           // string
```

`unknown` 表示调用前后都不知道具体类型，使用前需要判断；泛型 `T` 表示类型暂时未写死，但调用时会确定并在相关位置保留下来。

如果只需要安全接收未知数据并检查内容，使用 `unknown`；如果需要表达输入、输出或多个成员之间的类型关系，考虑泛型。

## 5. 泛型与数组

### 5.1 安全取得第一项

```ts
function firstItem<T>(items: readonly T[]): T | undefined {
  return items[0];
}

const firstNumber = firstItem([10, 20, 30]);
const firstName = firstItem(["田中", "佐藤"]);
const emptyResult = firstItem<string>([]);

console.log(firstNumber); // 10
console.log(firstName);   // 田中
console.log(emptyResult); // undefined
```

`readonly T[]` 表示函数接收由 `T` 组成的只读数组视图，不通过这个参数修改原数组。数组可能为空，因此返回类型必须包含 `undefined`。

结果会保持元素类型：数字数组得到 `number | undefined`，字符串数组得到 `string | undefined`。使用结果前按第六章处理空值。

### 5.2 `Array<T>` 本身就是泛型类型

```ts
const first: string[] = ["A", "B"];
const second: Array<string> = ["C", "D"];

const readonlyFirst: readonly number[] = [1, 2];
const readonlySecond: ReadonlyArray<number> = [3, 4];

console.log(first[0]);          // A
console.log(second[0]);         // C
console.log(readonlyFirst[0]);  // 1
console.log(readonlySecond[0]); // 3
```

对应关系如下：

| 简写 | 泛型写法 | 含义 |
| --- | --- | --- |
| `string[]` | `Array<string>` | 可修改的字符串数组 |
| `readonly number[]` | `ReadonlyArray<number>` | 通过当前引用不能修改的数字数组 |

之前已经一直在使用泛型数组类型，只是采用了更简洁的方括号写法。

## 6. 多个类型参数

### 6.1 不同位置可以使用不同类型

```ts
function createPair<FirstType, SecondType>(
  first: FirstType,
  second: SecondType
): [FirstType, SecondType] {
  return [first, second];
}

const employeeEntry = createPair("EMP-001", 10);
const statusEntry = createPair("REQ-001", "pending");

console.log(employeeEntry); // ["EMP-001", 10]
console.log(statusEntry);   // ["REQ-001", "pending"]
```

`FirstType` 连接第一个参数与元组第一项，`SecondType` 连接第二个参数与元组第二项。两个类型参数彼此独立，不要求相同。

### 6.2 泛型回调表达输入到输出的转换

```ts
function transform<InputType, OutputType>(
  items: readonly InputType[],
  converter: (item: InputType) => OutputType
): OutputType[] {
  return items.map(converter);
}

const employeeCodes = ["EMP-001", "EMP-002"];

const codeLengths = transform(
  employeeCodes,
  code => code.length
);

console.log(codeLengths); // [7, 7]
```

执行时的类型关系是：

1. 输入数组确定 `InputType` 为 `string`；
2. 回调参数 `item` 因此是字符串；
3. 回调返回数字，所以 `OutputType` 是 `number`；
4. 整个函数返回 `number[]`。

`map()` 是数组转换方法，会逐项调用回调并把每个返回值组成新数组，不修改原数组。泛型在这里保留了输入元素、回调参数、回调结果和最终数组之间的关系。

## 7. 泛型类型别名与接口

### 7.1 泛型类型别名

```ts
type DataResult<T> = {
  data: T;
  updatedAt: string;
};

type Employee = {
  employeeCode: string;
  employeeName: string;
};

const employeeResult: DataResult<Employee> = {
  data: {
    employeeCode: "EMP-001",
    employeeName: "田中",
  },
  updatedAt: "2026-09-01",
};

const countResult: DataResult<number> = {
  data: 3,
  updatedAt: "2026-09-02",
};

console.log(employeeResult.data.employeeName); // 田中
console.log(countResult.data);                 // 3
```

`DataResult<T>` 的外层结构固定，`data` 的具体类型由使用方传入。这样不用分别创建 `EmployeeResult`、`CountResult` 等大量重复类型。

### 7.2 泛型接口

```ts
interface PageData<T> {
  items: T[];
  total: number;
}

interface Employee {
  employeeCode: string;
  employeeName: string;
}

const employeePage: PageData<Employee> = {
  items: [
    { employeeCode: "EMP-001", employeeName: "田中" },
    { employeeCode: "EMP-002", employeeName: "佐藤" },
  ],
  total: 2,
};

console.log(employeePage.items[0].employeeName); // 田中
console.log(employeePage.total);                 // 2
```

`PageData<Employee>` 表示 `items` 必须是员工数组，`PageData<string>` 则表示字符串数组。泛型不是放宽字段类型，而是在使用时把结构中的类型参数替换成明确类型。

### 7.3 泛型接口中的方法

```ts
interface StorageBox<T> {
  value: T;
  replace(nextValue: T): void;
}

const nameBox: StorageBox<string> = {
  value: "田中",
  replace(nextValue) {
    this.value = nextValue;
  },
};

nameBox.replace("佐藤");
console.log(nameBox.value); // 佐藤

// nameBox.replace(100);
// 错误：StorageBox<string>只能接收字符串
```

接口确定为 `StorageBox<string>` 后，属性 `value`、方法参数 `nextValue` 都必须是字符串。同一个类型参数把这些位置关联起来。


## 8. 泛型约束 `extends`

### 8.1 为什么需要约束

未受约束的 `T` 不能保证具有 `length`。如果函数只允许带有数字 `length` 属性的值，可以添加约束：

```ts
function getLength<T extends { length: number }>(value: T): number {
  return value.length;
}

console.log(getLength("hello"));      // 5
console.log(getLength([10, 20, 30])); // 3

// getLength(100);
// 错误：number没有length属性
```

`T extends { length: number }` 表示 T 至少要满足 `{ length: number }` 这个结构。字符串和数组都有数字 `length`，数字没有。

这里的 `extends` 是泛型约束，不是类继承，也不会创建运行时父子关系。

### 8.2 约束后仍然保留具体类型

```ts
function keepIdentified<T extends { id: string }>(value: T): T {
  console.log(value.id);
  return value;
}

const employee = keepIdentified({
  id: "EMP-001",
  employeeName: "田中",
});

console.log(employee.id);           // EMP-001
console.log(employee.employeeName); // 田中
```

函数内部只保证可以读取字符串 `id`，但返回值仍保留完整输入类型，所以调用后还能读取 `employeeName`。

如果参数直接写成 `{ id: string }`，返回类型也只声明这个最小结构：

```ts
function keepIdOnly(value: { id: string }): { id: string } {
  return value;
}

const sourceEmployee = {
  id: "EMP-001",
  employeeName: "田中",
};

const employee = keepIdOnly(sourceEmployee);

console.log(employee.id); // EMP-001
// console.log(employee.employeeName);
// 错误：返回类型只保留了id
```

泛型约束的价值是：既限制最低要求，又保留调用时更具体的类型信息。

### 8.3 约束为有限的类型范围

```ts
function toDisplayText<T extends string | number>(value: T): string {
  return String(value);
}

console.log(toDisplayText("EMP-001")); // EMP-001
console.log(toDisplayText(1001));      // 1001

// toDisplayText(true);
// 错误：boolean不符合string | number约束
```

约束也可以是联合类型。只有在函数还需要保留具体 `T` 与其他位置的关系时才使用这种写法；若只接收后转换成字符串，普通 `string | number` 参数往往已经足够。


## 9. 什么时候不需要泛型

### 9.1 只有一个位置使用类型参数

```ts
function printText<T>(value: T): void {
  console.log(value);
}

printText("hello"); // hello
```

这段代码虽然合法，但 `T` 没有把两个位置关联起来，通常直接使用明确类型或 `unknown` 更简单：

```ts
function printValue(value: unknown): void {
  console.log(value);
}

printValue("hello"); // hello
printValue(10);      // 10
```

### 9.2 业务本来只允许一种类型

```ts
function calculateTotal(price: number, quantity: number): number {
  return price * quantity;
}

console.log(calculateTotal(1200, 2)); // 2400
```

金额和数量必须是数字，不需要为了“通用”改成泛型。明确业务类型通常比抽象程度更重要。

### 9.3 联合类型已经能准确表达需求

```ts
function printCode(code: string | number): void {
  console.log(String(code));
}

printCode("EMP-001"); // EMP-001
printCode(1001);      // 1001
```

函数没有返回与输入类型相关的值，只是统一输出，联合参数已经足够。

可以用一个问题判断是否需要泛型：**这里是否需要让调用方传入的具体类型，在返回值、回调、属性或其他参数位置继续保持一致？** 如果没有这种关系，通常不需要泛型。

## 10. 常见错误与排查

### 10.1 把泛型当作更安全的 `any`

泛型不是关闭检查，而是推迟确定具体类型并保留关系。函数内部仍只能使用 `T` 已知支持的能力。

### 10.2 类型参数只出现一次

检查 `T` 是否连接了至少两个有意义的位置。若只出现一次，明确类型或 `unknown` 往往更合适。

### 10.3 不必要地显式指定类型参数

能从实参正确推断时优先推断。显式类型参数适用于没有可推断输入、需要扩大预期类型或团队约定要求的情况。

### 10.4 用显式类型参数掩盖错误数据

`identity<number>("10")` 应当报错。不要继续添加断言，应修正实际数据或进行真实转换。

### 10.5 忽略空数组结果

取得第一项或最后一项时，空数组会得到 `undefined`。泛型只能保留元素类型，不能保证数组非空。

### 10.6 误以为约束会删除其他属性

`T extends { id: string }` 只规定最低结构。返回 `T` 时仍会保留调用方传入的其他属性。


## 11. 本章练习

### 11.1 泛型原样返回

编写 `identity<T>()`，分别传入字符串、数字和员工对象。对返回结果调用各自合法的属性或方法，再与 `any` 版本对比错误提示。

### 11.2 数组最后一项

实现 `lastItem<T>(items: readonly T[]): T | undefined`。分别测试数字数组、字符串数组和空数组，并在使用结果前处理 `undefined`。

### 11.3 转换数组

编写具有 `InputType`、`OutputType` 两个类型参数的转换函数。把员工对象数组转换成员工姓名数组，再转换成显示字符串数组，确认每一步的返回类型。

### 11.4 泛型分页结构

定义 `PageData<T>`，包含 `items: T[]`、`total: number` 和 `page: number`。分别创建员工分页和字符串分页，尝试在员工数组中放入错误结构并观察提示。

### 11.5 泛型约束

编写函数接收至少具有 `id: string` 的对象并原样返回。分别传入员工对象和申请对象，确认返回值保留各自额外属性；再传入没有 `id` 的对象观察错误。

### 11.6 判断是否需要泛型

分别判断以下需求应使用明确类型、联合类型、`unknown` 还是泛型，并编写最小实现：

- 计算两个数字的乘积；
- 输出尚不确定类型的调试值；
- 接收字符串或数字编号并统一转成字符串；
- 返回数组第一项并保留元素类型。

## 本章检查点

- 能说明 `any` 为什么会丢失检查。
- 能说明联合类型为什么不能总是保留本次输入与输出的对应关系。
- 能读懂 `<T>`、参数中的 `T` 和返回类型中的 `T`。
- 能让 TypeScript 推断类型参数，并知道何时显式指定。
- 能说明泛型函数内部为什么不能随意读取 `T` 的属性。
- 能区分泛型和 `unknown` 的用途。
- 能使用泛型安全处理数组，并处理空数组结果。
- 能使用多个类型参数表达输入与输出的转换关系。
- 能定义泛型类型别名和接口。
- 能使用 `extends` 添加最低结构约束。
- 能说明泛型约束与类继承不同。
- 能识别不必要的泛型，优先使用更明确的类型。
