# 第 8 章 接口与类型组合

第四章已经使用 `type` 为对象结构命名。本章学习另一种常见的对象类型声明方式 `interface`，并学习怎样在已有类型的基础上添加要求、组合结构，以及理解 TypeScript 为什么主要比较对象结构，而不是只比较类型名称。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。注释掉的错误代码用于观察编译提示。

完成本章后，你应当能够：

- 使用 `interface` 描述对象属性和方法。
- 说明接口不会创建对象，也不会生成运行时代码。
- 使用 `extends` 扩展接口。
- 使用交叉类型 `&` 组合已有类型。
- 解释结构兼容与对象字面量的多余属性检查。
- 根据表达内容和项目约定选择 `type` 或 `interface`。

## 1. `interface` 解决什么问题

### 1.1 用名字表示一类对象结构

员工对象需要统一具有编号、姓名和剩余休假天数：

```ts
interface Employee {
  employeeCode: string;
  employeeName: string;
  remainingLeaveDays: number;
}

const employee: Employee = {
  employeeCode: "EMP-001",
  employeeName: "田中",
  remainingLeaveDays: 10,
};

console.log(employee.employeeName);      // 田中
console.log(employee.remainingLeaveDays); // 10
```

`interface Employee` 声明一个名为 `Employee` 的接口。花括号中列出对象必须满足的属性要求：属性名必须对应，属性值也必须符合指定类型。

接口与第四章的对象类型一样，会检查缺少字段和错误类型：

```ts
interface Employee {
  employeeCode: string;
  employeeName: string;
}

const employee: Employee = {
  employeeCode: "EMP-001",
  employeeName: "佐藤",
};

// const missingName: Employee = {
//   employeeCode: "EMP-002",
// };
// 错误：缺少employeeName

// employee.employeeCode = 1001;
// 错误：employeeCode必须是string

console.log(employee.employeeCode); // EMP-001
```

### 1.2 接口不是对象或类

`interface` 只描述类型，不会创建数据，也不能用 `new` 生成实例：

```ts
interface Employee {
  employeeCode: string;
  employeeName: string;
}

const employee: Employee = {
  employeeCode: "EMP-001",
  employeeName: "田中",
};

console.log(employee); // { employeeCode: "EMP-001", employeeName: "田中" }
```

编译后的 JavaScript 中会保留 `employee` 对象，但 `interface Employee` 会消失。因此接口：

- 不保存实际数据；
- 不为属性提供默认值；
- 不会自动验证接口响应；
- 不能写成 `new Employee()`；
- 不能用于 `value instanceof Employee`。

需要运行时创建行为时使用对象、函数或类；需要检查外部数据时使用第六章的真实判断。

## 2. 接口中的属性

### 2.1 必填、可选和只读属性

第四章学过的属性规则也适用于接口：

```ts
interface Employee {
  readonly employeeCode: string;
  employeeName: string;
  nickname?: string;
}

const employee: Employee = {
  employeeCode: "EMP-001",
  employeeName: "田中",
};

employee.employeeName = "田中 太郎";
console.log(employee.employeeName); // 田中 太郎
console.log(employee.nickname);     // undefined

// employee.employeeCode = "EMP-999";
// 错误：只读属性不能重新赋值
```

三种属性的含义如下：

| 写法 | 调用方是否必须提供 | 赋值后的限制 |
| --- | --- | --- |
| `name: string` | 必须 | 可以修改为同类型值 |
| `name?: string` | 可以省略 | 读取结果可能是 `undefined` |
| `readonly name: string` | 必须 | 不能通过当前类型重新赋值 |

`readonly` 仍是编译阶段的浅层限制，不会把运行时对象自动冻结。

### 2.2 属性也可以使用已有类型

```ts
type RequestStatus = "pending" | "approved" | "rejected";

interface LeaveRequest {
  requestId: string;
  employeeCode: string;
  status: RequestStatus;
}

const request: LeaveRequest = {
  requestId: "REQ-001",
  employeeCode: "EMP-001",
  status: "pending",
};

console.log(request.status); // pending
```

接口属性不只可以使用基本类型，也可以使用类型别名、联合类型、数组或另一个接口。应复用已经表达清楚的业务类型，不必重复写相同联合。

## 3. 接口中的方法

### 3.1 方法签名只规定调用方式

```ts
interface RequestService {
  findStatus(requestId: string): string;
  printMessage(message: string): void;
}

const requestService: RequestService = {
  findStatus(requestId) {
    return `${requestId}: pending`;
  },
  printMessage(message) {
    console.log(message);
  },
};

const statusText = requestService.findStatus("REQ-001");
requestService.printMessage(statusText);
// REQ-001: pending
```

`findStatus(requestId: string): string` 是**方法签名**，规定对象必须有一个名为 `findStatus` 的方法，该方法接收字符串并返回字符串。接口只声明要求，真正的函数体由 `requestService` 对象提供。

对象方法中的参数类型会从接口获得上下文推断，因此实现处的 `requestId` 和 `message` 不必重复标注。

### 3.2 方法写法与函数属性写法

接口中还可以把函数写成属性类型：

```ts
interface Formatter {
  format(value: number): string;
  print: (message: string) => void;
}

const formatter: Formatter = {
  format(value) {
    return `${value}日`;
  },
  print: message => {
    console.log(message);
  },
};

formatter.print(formatter.format(5)); // 5日
```

`format(value: number): string` 是方法签名；`print: (message: string) => void` 是保存函数的属性。普通业务接口中两者都能描述可调用成员，优先遵守当前项目的统一写法。需要表达“属性本身可替换或可选”等属性特征时，函数属性写法会更直观。

## 4. 使用 `extends` 扩展接口

### 4.1 在共同结构上增加字段

多个业务对象可能共享编号和名称：

```ts
interface NamedEntity {
  code: string;
  name: string;
}

interface Employee extends NamedEntity {
  remainingLeaveDays: number;
}

const employee: Employee = {
  code: "EMP-001",
  name: "田中",
  remainingLeaveDays: 10,
};

console.log(employee.code);               // EMP-001
console.log(employee.remainingLeaveDays); // 10
```

`Employee extends NamedEntity` 表示 `Employee` 继承 `NamedEntity` 的全部要求，再增加自己的属性。它不会复制或创建对象，只是在类型层组合约定。

### 4.2 扩展时保持原有约定

一个接口可以扩展多个接口，但只应组合确实稳定、相关的共同结构。扩展后的属性必须兼容原接口，不能把原来的`string`属性随意改成`number`。新人先掌握单一`extends`；多接口组合按项目现有类型阅读即可。



## 5. 使用交叉类型 `&` 组合结构

### 5.1 `A & B` 表示同时满足两边

```ts
type Identifiable = {
  id: string;
};

type Timestamped = {
  createdAt: string;
};

type LeaveRequest = Identifiable & Timestamped & {
  employeeCode: string;
};

const request: LeaveRequest = {
  id: "REQ-001",
  createdAt: "2026-09-01",
  employeeCode: "EMP-001",
};

console.log(request.id);          // REQ-001
console.log(request.employeeCode); // EMP-001
```

`&` 称为**交叉类型（intersection type）**。`A & B` 表示值必须同时满足 A 和 B，而不是从两边任选一个：

| 写法 | 含义 |
| --- | --- |
| `A | B` | 满足 A 或 B 中至少一种 |
| `A & B` | 同时满足 A 和 B |

### 5.2 组合时避免属性冲突

交叉类型可以组合接口和类型别名。如果两边存在同名但不兼容的属性，例如一边要求`id: string`、另一边要求`id: number`，结果将难以满足。应修正模型，而不是使用断言绕过冲突。



## 6. `extends` 和 `&` 怎样选择

两者都能复用对象结构，但表达方式不同：

| 场景 | 常见选择 | 说明 |
| --- | --- | --- |
| 新接口是已有接口的扩展 | `interface ... extends ...` | 关系清楚，冲突提示直观 |
| 组合多个现有类型表达式 | `A & B` | 适用于类型别名、接口及其他对象类型 |
| 普通对象只有少量字段 | 直接声明 | 不必为了复用而过度拆分 |
| 维护已有项目 | 保持项目约定 | 一致性通常比个人偏好重要 |

不要把 `extends` 理解成运行时复制，也不要把 `&` 理解成 JavaScript 对象合并。它们都只组合类型要求，不会生成新对象。

## 7. TypeScript 按结构判断兼容性

TypeScript 主要检查值是否具有目标类型要求的成员，而不是比较类型名称：

```ts
interface Named {
  name: string;
}

const employee = { name: "田中", employeeCode: "EMP-001" };
const named: Named = employee;
console.log(named.name); // 田中
```

`employee`包含必需的`name`，因此可以赋给`Named`。这称为结构兼容。它只是编译阶段的类型比较，不是运行时数据校验，也不表示业务含义不同的对象应该随意混用。


## 8. 对象字面量的多余属性检查

直接把对象字面量赋给明确类型时，TypeScript 会额外检查未知属性，帮助发现拼写错误：

```ts
interface Employee {
  employeeCode: string;
  employeeName: string;
}

const employee: Employee = {
  employeeCode: "EMP-001",
  employeeName: "田中",
  // departmantCode: "DEV", // 错误：类型中没有这个属性
};
```

通过变量传递时可能只检查目标类型需要的属性。不要利用这个差异绕过检查；如果业务需要额外字段，应把它们写进正确的类型。


## 9. `type` 与 `interface` 怎样选择

### 9.1 两者都能描述普通对象

```ts
type EmployeeByType = {
  employeeCode: string;
  employeeName: string;
};

interface EmployeeByInterface {
  employeeCode: string;
  employeeName: string;
}

const first: EmployeeByType = {
  employeeCode: "EMP-001",
  employeeName: "田中",
};

const second: EmployeeByInterface = {
  employeeCode: "EMP-002",
  employeeName: "佐藤",
};

console.log(first.employeeName);  // 田中
console.log(second.employeeName); // 佐藤
```

描述普通对象时，两者通常都能完成任务。不要记成“对象只能用 `interface`”或“`type` 永远更好”。

### 9.2 根据表达内容选择

| 需求 | 常见选择 | 原因 |
| --- | --- | --- |
| 描述普通对象结构 | 两者都可以 | 能力重叠较多 |
| 命名联合类型 | `type` | `interface` 不能直接等于联合 |
| 命名元组 | `type` | 写法更直接 |
| 命名函数类型表达式 | `type` | 容易复用完整调用签名 |
| 表达可扩展的对象契约 | `interface` 常见 | `extends` 关系清楚 |
| 类需要满足某种结构 | `interface` 常见 | 第九章可配合 `implements` |
| 维护现有项目 | 遵循项目约定 | 减少无意义的混用与改写 |

`interface` 支持同名声明合并，这主要用于扩展既有库或环境类型。普通业务模型中不应依赖分散的同名声明来拼接字段；该机制放在附录中了解。

## 10. 常见错误与排查

### 10.1 以为接口会创建对象

接口只描述结构。需要真实数据时仍要创建对象；需要初始化过程和共享行为时再考虑第九章的类。

### 10.2 把接口用于运行时判断

`interface` 编译后消失，不能用于 `instanceof`。外部对象使用 `typeof`、`in` 和字段值检查。

### 10.3 把 `extends` 当作对象复制

接口扩展只增加类型要求，不会创建新对象、复制属性或填充默认值。

### 10.4 混淆联合与交叉

`A | B` 是满足其中一种，`A & B` 是同时满足两边。看到缺少属性错误时，先确认是否错误使用了 `&`。

### 10.5 交叉类型中出现同名冲突

交叉不会自动覆盖属性。检查参与组合的各类型是否对同名字段给出了不兼容要求。

### 10.6 用中间变量故意躲避多余属性检查

赋值能够通过不代表字段符合接口规格。发现直接对象字面量报错时，应检查拼写和数据契约。

## 11. 本章练习

### 11.1 创建员工接口

定义 `Employee` 接口，包含只读编号、姓名、可选昵称和剩余休假天数。创建两名员工，分别覆盖有昵称和无昵称的情况；尝试缺少必填字段、修改只读编号并观察错误。

### 11.2 为接口增加方法

定义 `LeaveCalculator` 接口，要求具有接收两个数字并返回数字的 `calculate()` 方法，以及接收字符串、不要求返回值的 `print()` 方法。创建对象实现这两个方法并验证结果。

### 11.3 扩展共同结构

1. 定义包含 `id` 的 `Identifiable`。
2. 定义包含创建日期和更新日期的 `Timestamped`。
3. 使用 `extends` 创建同时具有这些字段和申请状态的 `LeaveRequest`。
4. 再使用交叉类型表达相同结构，比较两种写法。

### 11.4 验证结构兼容

创建一个包含姓名、编号和部门的对象，将其赋给只要求姓名的变量并传给只要求姓名的函数。输出结果，再尝试通过窄类型变量读取编号，解释错误原因。

### 11.5 观察多余属性检查

把带有错误拼写字段的对象字面量直接赋给接口，记录错误；再观察已有变量赋值的行为。不要把中间变量作为修复方案，最终应把字段名改为规格中的正确名称。

## 本章检查点

- 能使用 `interface` 描述对象属性和方法。
- 能说明接口不是对象、类或运行时校验器。
- 能在接口中使用必填、可选和只读属性。
- 能区分方法签名与函数属性写法。
- 能使用 `extends` 扩展一个或多个接口。
- 能说明 `extends` 与交叉类型都不会生成对象。
- 能区分联合类型 `|` 和交叉类型 `&`。
- 能识别交叉类型的同名属性冲突。
- 能解释结构兼容和窄类型变量的可见成员。
- 能说明对象字面量为什么会执行多余属性检查。
- 能根据表达内容和项目约定选择 `type` 或 `interface`。
