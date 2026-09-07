# 第 4 章 对象类型与类型别名

第三章用数组保存多项同类数据。但一名员工、一条申请通常包含编号、姓名、状态等不同字段，需要使用对象组织。本章学习怎样描述对象必须具有哪些属性，以及每个属性允许保存什么类型。

本章示例均为独立实验。每次把一个完整代码块放入第一章创建的 `src/example.ts`，编译后运行。注释掉的错误代码用于观察编译提示。

完成本章后，你应当能够：

- 区分对象值和对象类型。
- 直接描述对象的属性名与属性类型。
- 使用类型别名复用对象结构。
- 说明必填、可选和只读属性的区别。
- 描述嵌套对象和对象数组。
- 说明对象类型只进行静态检查，不会验证运行时外部数据。

## 1. 对象类型解决什么问题

JavaScript 对象把一条数据的多个字段组织在一起：

```js
const employee = {
  employeeNumber: "EMP-001",
  name: "田中",
  remainingLeaveDays: 12,
};

console.log(employee.name); // 田中
```

仅看当前值可以知道各字段现在保存了什么，但没有明确写出长期约定。例如，下面的修改在 JavaScript 运行时是允许的：

```js
employee.remainingLeaveDays = "十二日";
```

如果后续代码要计算休假天数，字符串会破坏计算。TypeScript 对象类型可以明确规定：

```text
employeeNumber 必须是字符串
name 必须是字符串
remainingLeaveDays 必须是数字
```

## 2. 直接描述对象类型

### 2.1 对象值和对象类型都有花括号

```ts
const employee: {
  employeeNumber: string;
  name: string;
  remainingLeaveDays: number;
} = {
  employeeNumber: "EMP-001",
  name: "田中",
  remainingLeaveDays: 12,
};

console.log(employee.name);               // 田中
console.log(employee.remainingLeaveDays); // 12
```

这段代码有两组花括号，职责不同：

```text
const employee: { name: string } = { name: "田中" };
                ^^^^^^^^^^^^^^^^   ^^^^^^^^^^^^^^^^
                对象类型            对象值
```

- 冒号后面的 `{ name: string }` 是类型，规定属性名称和类型。
- 等号后面的 `{ name: "田中" }` 是运行时真正保存的对象。
- 类型中的 `name: string` 表示要求，不是给 `name` 赋值。
- 对象值中的 `name: "田中"` 才保存实际数据。

类型结构中的属性通常使用分号分隔，对象值中的属性使用逗号分隔。TypeScript 也允许类型属性使用逗号，但项目中保持一种写法更容易阅读。

### 2.2 读取和修改属性

```ts
const employee: {
  name: string;
  remainingLeaveDays: number;
} = {
  name: "田中",
  remainingLeaveDays: 12,
};

employee.name = "佐藤";
employee.remainingLeaveDays = 10;

console.log(employee.name);               // 佐藤
console.log(employee.remainingLeaveDays); // 10

// employee.remainingLeaveDays = "十日";
// 错误：string不能赋给number属性
```

TypeScript 会检查创建对象时的值，也会检查之后对属性的赋值。

### 2.3 `const` 对象仍能修改属性

```ts
const employee: { name: string } = {
  name: "田中",
};

employee.name = "佐藤";
console.log(employee.name); // 佐藤

// employee = { name: "鈴木" };
// 错误：const变量不能重新赋值
```

`const` 限制变量不能改为指向另一个对象，不会自动把对象内部变成只读。限制属性修改需要使用第 5 节的 `readonly`。

## 3. 必填属性及其检查

### 3.1 默认都是必填属性

对象类型中正常写出的属性默认必须存在：

```ts
const employee: {
  employeeNumber: string;
  name: string;
} = {
  employeeNumber: "EMP-001",
  name: "田中",
};

console.log(employee.employeeNumber); // EMP-001
```

缺少 `name` 会在编译时提示错误：

```ts
// const employee: {
//   employeeNumber: string;
//   name: string;
// } = {
//   employeeNumber: "EMP-001",
// };
// 错误：缺少必填属性name
```

### 3.2 属性名和属性类型都要一致

```ts
const employee: {
  employeeNumber: string;
  active: boolean;
} = {
  employeeNumber: "EMP-001",
  active: true,
};

console.log(employee.active); // true

// active: "true"  // 错误：字符串不是boolean
// employeeNo: "EMP-001" // 错误：属性名与约定不同
```

字符串 `"true"` 和布尔值 `true` 不同。属性名也必须按照类型和业务规格书统一，不能认为含义接近就会自动对应。

### 3.3 直接写对象时会检查多余属性

```ts
const employee: {
  employeeNumber: string;
  name: string;
} = {
  employeeNumber: "EMP-001",
  name: "田中",
  // department: "开发部",
  // 错误：当前类型没有声明department
};

console.log(employee.name); // 田中
```

直接把对象字面量赋给已标注变量时，TypeScript 会检查未声明属性，有助于发现字段拼写错误。通过其他变量赋值时存在更细的结构兼容规则，第八章再详细讲解。

## 4. 使用 `type` 给对象结构命名

### 4.1 为什么需要类型别名

直接类型适合一次性的小对象。同一结构需要创建多条数据时，重复写会很长，也容易让字段要求不一致：

```ts
const firstEmployee: { employeeNumber: string; name: string } = {
  employeeNumber: "EMP-001",
  name: "田中",
};

const secondEmployee: { employeeNumber: string; name: string } = {
  employeeNumber: "EMP-002",
  name: "佐藤",
};

console.log(firstEmployee.name, secondEmployee.name); // 田中 佐藤
```

可以使用类型别名集中定义一次。

### 4.2 定义并使用类型别名

```ts
type Employee = {
  employeeNumber: string;
  name: string;
  remainingLeaveDays: number;
};

const firstEmployee: Employee = {
  employeeNumber: "EMP-001",
  name: "田中",
  remainingLeaveDays: 12,
};

const secondEmployee: Employee = {
  employeeNumber: "EMP-002",
  name: "佐藤",
  remainingLeaveDays: 8,
};

console.log(firstEmployee.name);                // 田中
console.log(secondEmployee.remainingLeaveDays); // 8
```

类型别名的基本写法是：

```text
type 类型名称 = 类型表达式;
```

- `type` 用于声明类型别名。
- `Employee` 是类型名称，项目中通常采用大驼峰命名。
- 等号右侧是被命名的对象类型。
- `firstEmployee: Employee` 表示对象必须符合这套结构。

### 4.3 类型别名不是变量或构造函数

```ts
type Employee = {
  name: string;
};

const employee: Employee = {
  name: "田中",
};

console.log(employee.name); // 田中
```

`Employee` 只在类型检查时存在：

- 不能写 `console.log(Employee)`，因为它不是运行时值。
- 不能写 `new Employee()`，因为它不是类或构造函数。
- 编译后的 JavaScript 中不会保留 `type Employee`。

类型别名不只可以命名对象。第五章会用它命名联合类型，第七章会用它描述函数类型。

## 5. 可选属性与只读属性

### 5.1 可选属性 `?`

有些字段不是每条数据都必须提供。例如员工昵称可以不存在：

```ts
type Employee = {
  employeeNumber: string;
  name: string;
  nickname?: string;
};

const employeeA: Employee = {
  employeeNumber: "EMP-001",
  name: "田中",
};

const employeeB: Employee = {
  employeeNumber: "EMP-002",
  name: "佐藤",
  nickname: "さとうさん",
};

console.log(employeeA.nickname); // undefined
console.log(employeeB.nickname); // さとうさん
```

`nickname?: string` 表示属性可以不存在；如果存在，值必须是字符串。它不是“可以填写任意类型”。

读取可选属性时可能得到 `undefined`。调用字符串方法前要先判断：

```ts
type Employee = {
  name: string;
  nickname?: string;
};

const employee: Employee = { name: "田中" };

if (employee.nickname === undefined) {
  console.log(employee.name); // 田中
} else {
  console.log(employee.nickname.toUpperCase());
}
```

条件已经确认 `nickname` 不是 `undefined` 后，才能安全调用 `toUpperCase()`。第六章会系统讲解这种类型收窄。

### 5.2 `readonly` 只读属性

员工编号创建后不应由前端随意改写，可以标记为只读：

```ts
type Employee = {
  readonly employeeNumber: string;
  name: string;
};

const employee: Employee = {
  employeeNumber: "EMP-001",
  name: "田中",
};

employee.name = "佐藤";
console.log(employee.name); // 佐藤

// employee.employeeNumber = "EMP-999";
// 错误：只读属性不能重新赋值
```

`readonly` 限制通过当前类型进行属性赋值。它是编译阶段的保护，不会在运行时自动冻结对象。

### 5.3 `readonly` 默认是浅层限制

```ts
type Employee = {
  readonly profile: {
    displayName: string;
  };
};

const employee: Employee = {
  profile: {
    displayName: "田中",
  },
};

employee.profile.displayName = "佐藤";
console.log(employee.profile.displayName); // 佐藤

// employee.profile = { displayName: "鈴木" };
// 错误：不能替换只读的profile属性
```

这里只限制 `profile` 属性不能指向另一个对象，内部的 `displayName` 没有被标记为只读，因此仍能修改。需要限制嵌套字段时，要在对应层级继续写 `readonly`。

## 6. 嵌套对象

### 6.1 直接描述嵌套结构

对象的属性值还可以是另一个对象：

```ts
const employee: {
  name: string;
  department: {
    code: string;
    name: string;
  };
} = {
  name: "田中",
  department: {
    code: "DEV",
    name: "开发部",
  },
};

console.log(employee.department.code); // DEV
console.log(employee.department.name); // 开发部
```

外层 `department` 必须是对象，内层对象又必须具有字符串类型的 `code` 和 `name`。

### 6.2 为内外层分别定义类型别名

嵌套结构会重复使用时，可以分别命名：

```ts
type Department = {
  code: string;
  name: string;
};

type Employee = {
  employeeNumber: string;
  name: string;
  department: Department;
};

const employee: Employee = {
  employeeNumber: "EMP-001",
  name: "田中",
  department: {
    code: "DEV",
    name: "开发部",
  },
};

console.log(employee.department.name); // 开发部
```

先定义 `Department`，再由 `Employee` 使用它。这样多个员工可以共享同一套部门结构。

不要为了拆分而给每个只有一处使用的小结构都起名字。类型别名应帮助表达业务含义和复用关系。

## 7. 对象数组

### 7.1 一条对象和多条对象

`Employee` 表示一名员工，`Employee[]` 表示多名员工组成的数组：

```ts
type Employee = {
  employeeNumber: string;
  name: string;
};

const employees: Employee[] = [
  { employeeNumber: "EMP-001", name: "田中" },
  { employeeNumber: "EMP-002", name: "佐藤" },
];

console.log(employees[0].name); // 田中
console.log(employees.length);  // 2
```

读取 `employees[0].name` 时，先取得数组第一项，再读取该对象的 `name`。

### 7.2 加入对象时也会检查结构

```ts
type Employee = {
  employeeNumber: string;
  name: string;
};

const employees: Employee[] = [];

employees.push({
  employeeNumber: "EMP-001",
  name: "田中",
});

console.log(employees.length); // 1
console.log(employees[0].name); // 田中

// employees.push({ employeeNumber: "EMP-002" });
// 错误：缺少必填属性name
```

`push()` 的运行行为与 JavaScript 相同，在数组末尾加入元素并返回新长度。TypeScript 会进一步检查新对象是否符合 `Employee`。

### 7.3 遍历对象数组

```ts
type Employee = {
  employeeNumber: string;
  name: string;
};

const employees: Employee[] = [
  { employeeNumber: "EMP-001", name: "田中" },
  { employeeNumber: "EMP-002", name: "佐藤" },
];

for (const employee of employees) {
  console.log(`${employee.employeeNumber}: ${employee.name}`);
}
// EMP-001: 田中
// EMP-002: 佐藤
```

`for...of` 每次取得一个 `Employee`，因此循环内可以读取类型中已经声明的属性。

数组下标仍可能越界。读取来自用户输入的下标时，要保留第三章讲过的范围检查。

## 8. 对象类型不是运行时数据校验

### 8.1 类型只在编译阶段工作

```ts
type Employee = {
  employeeNumber: string;
  name: string;
};

const employee: Employee = {
  employeeNumber: "EMP-001",
  name: "田中",
};

console.log(employee.name); // 田中
```

编译后，`type Employee` 会消失。TypeScript 不会自动生成下面这些运行时处理：

- 检查接口是否真的返回 `employeeNumber`；
- 把错误字段名自动改正确；
- 把数字姓名自动转换成字符串；
- 为缺失字段自动补默认值。

### 8.2 类型断言也不能验证对象

第二章已经说明，类型断言只影响编译器。不要看到外部数据就直接写：

```ts
type Employee = {
  employeeNumber: string;
  name: string;
};

const receivedValue: unknown = {
  employeeNumber: "EMP-001",
  name: 100,
};

const employee = receivedValue as Employee;
console.log(typeof employee.name); // number
```

断言后编译器相信 `name` 是字符串，但运行时真实值仍然是数字。第六章会学习检查未知值，第十五章会把检查应用到接口响应。

## 9. 怎样组织对象类型

| 情况 | 建议写法 | 原因 |
| --- | --- | --- |
| 只使用一次的小对象 | 直接对象类型 | 定义位置直观 |
| 多处使用的业务结构 | `type` 类型别名 | 集中维护字段要求 |
| 可以缺少的字段 | 可选属性 `?` | 读取时会提醒处理缺失 |
| 创建后不应重新赋值的字段 | `readonly` | 阻止代码意外改写 |
| 对象中包含可复用子结构 | 分别定义类型别名 | 表达嵌套关系 |
| 多条相同结构的数据 | `TypeName[]` | 统一检查每个数组元素 |

第八章还会介绍 `interface`。描述普通对象时，`type` 和 `interface` 都可以，不要把当前使用 `type` 理解成只有这一种正确写法。

## 10. 常见错误与排查

### 10.1 混淆类型结构和对象值

类型中的 `name: string` 是要求，对象值中的 `name: "田中"` 才是数据。检查冒号所在位置以及等号左右两边。

### 10.2 缺少必填属性

确认属性是否确实必须存在。如果业务允许缺少，再根据规格改为可选属性；不要只为消除报错随意加 `?`。

### 10.3 属性名与规格不一致

`employeeNumber`、`employeeNo` 和 `employee_number` 是三个不同属性。前后端字段名以接口规格为准，不会根据含义自动匹配。

### 10.4 误以为 `const` 等于所有属性只读

`const` 限制变量重新赋值，`readonly` 限制指定属性赋值，两者作用位置不同。

### 10.5 直接使用可选属性

`nickname?: string` 可能得到 `undefined`。调用字符串方法或交给只接受字符串的位置前，要先检查。

### 10.6 用断言掩盖错误对象

`value as Employee` 不会校验字段。来自接口、存储和用户输入的值，应在运行时检查实际结构。

## 11. 本章练习

### 11.1 直接描述对象

创建一条设备数据，要求包含：

- `assetId`：字符串；
- `name`：字符串；
- `available`：布尔值。

输出三个属性，再分别尝试删除必填属性、把 `available` 写成字符串，记录编译错误后恢复。

### 11.2 使用类型别名

1. 定义 `Equipment` 类型别名。
2. 使用该类型创建两台设备。
3. 修改其中一台设备的名称并输出。
4. 尝试把数字赋给名称，确认编译器阻止。

### 11.3 可选和只读属性

为 `Equipment` 增加：

- 只读的 `assetId`；
- 可选的 `note`。

创建一条没有备注的数据和一条有备注的数据。读取备注前检查 `undefined`，再尝试修改 `assetId` 并观察编译错误。

### 11.4 嵌套对象和对象数组

1. 定义 `Location`，包含楼层和区域名称。
2. 在 `Equipment` 中加入 `location: Location`。
3. 创建 `Equipment[]`，保存两台设备。
4. 使用 `for...of` 输出每台设备的编号、名称和位置。
5. 尝试加入缺少位置的对象，记录错误后修复。

### 11.5 解释运行时边界

运行第 8.2 节的错误断言示例，回答：

1. 为什么代码中的 `employee.name` 被编译器当成字符串？
2. 为什么 `typeof employee.name` 仍然输出 `number`？
3. 正式处理接口数据时还需要增加什么？

## 本章检查点

- 能区分对象类型和对象值的两组花括号。
- 能描述必填属性，并解释属性名和属性类型的检查。
- 能使用 `type` 创建可复用的对象类型。
- 能说明类型别名不是变量、对象或构造函数。
- 能正确使用可选属性和只读属性。
- 能说明 `readonly` 默认只限制当前属性赋值。
- 能描述嵌套对象和对象数组。
- 能说明对象类型与类型断言都不会验证运行时外部数据。
- 能判断直接对象类型与类型别名各自适合的场景。
