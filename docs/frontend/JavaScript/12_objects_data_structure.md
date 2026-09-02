# 第十二章 对象与数据结构

## 学习目标

完成本章后，你应能够：

- 使用对象字面量创建和组织业务数据。
- 读取、新增、修改和删除对象属性。
- 区分点语法和方括号语法的使用场景。
- 定义对象方法，并理解方法中的 `this`。
- 处理嵌套对象和对象数组。
- 遍历对象，并判断属性是否属于对象自身。
- 说明对象赋值、修改和比较时的引用特点。
- 看懂构造函数、实例、`prototype` 和原型链的基础代码。

## 掌握要求

- **必须掌握**：对象字面量、属性操作、对象方法、嵌套对象、对象数组、常用遍历方式和引用特点。
- **需要掌握**：计算属性名、属性存在性判断、`this`、浅复制和`structuredClone()`的适用边界。
- **会使用、能看懂**：构造函数、实例、`prototype`、原型链查找、自有属性与继承属性。
- **后续学习**：对象解构、完整展开语法、`Map`、`Set`、`Proxy` 和 `Reflect`。

## 1. 认识和创建对象

### 1.1 对象解决什么问题

对象用于把一条数据的多个相关字段组织在一起。

```js
const user = {
  accountId: "yamada.taro",
  name: "山田 太郎",
  department: "development",
  employeeNumber: "EMP-00001"
};
```

对象中的每一项都是一组“属性名和属性值”：

```text
accountId: "yamada.taro"
属性名       属性值
```

- 属性名也称为键，即 key。
- 属性值也称为 value，可以是任意 JavaScript 数据类型。
- 不同属性之间使用逗号分隔。

数组适合保存一组有顺序的数据，对象适合描述一条数据有哪些字段。项目中经常用数组保存多条记录，再用对象描述每条记录。

### 1.2 使用对象字面量创建对象

使用 `{}` 创建对象的写法称为对象字面量，是项目中最常用的方式。

```js
const application = {
  id: "REQ-20260820-001",
  leaveType: "paid",
  days: 2,
  urgent: false,
  note: "",
  approver: null
};
```

一个对象可以同时保存不同类型的值：

| 属性 | 值 | 数据类型 | 含义 |
| --- | --- | --- | --- |
| `id` | `"REQ-20260820-001"` | string | 申请编号 |
| `days` | `2` | number | 申请天数 |
| `urgent` | `false` | boolean | 是否紧急 |
| `note` | `""` | string | 当前是空字符串 |
| `approver` | `null` | null | 当前没有审批人 |

空对象写作 `{}`。虽然之后可以逐步添加属性，但业务对象通常应在创建时写出稳定的主要字段，方便理解数据结构。

### 1.3 属性名的写法

#### 1.3.1 普通属性名

普通标识符形式的属性名可以不加引号：

```js
const user = {
  name: "山田 太郎",
  department: "development"
};
```

包含连字符、空格等特殊字符时，需要使用字符串形式：

```js
const responseHeaders = {
  "content-type": "application/json",
  "request id": "REQ-001"
};
```

#### 1.3.2 计算属性名

属性名来自变量或表达式时，在对象字面量中使用 `[]`：

```js
const statusKey = "pending";

const statusLabels = {
  [statusKey]: "申請中"
};

console.log(statusLabels.pending); // 申請中
```

实际创建的属性名是变量保存的 `"pending"`，不是字符串 `"statusKey"`。

## 2. 操作对象属性

### 2.1 读取对象属性

#### 2.1.1 点语法

属性名固定并且是普通标识符时，优先使用点语法：

```js
console.log(user.name);           // 山田 太郎
console.log(user.employeeNumber); // EMP-00001
```

#### 2.1.2 方括号语法

属性名来自变量时使用方括号：

```js
const key = "department";

console.log(user[key]); // development
```

属性名包含特殊字符时也使用方括号：

```js
console.log(responseHeaders["content-type"]);
```

下面两种写法含义不同：

```js
const key = "name";

console.log(user[key]);    // 读取 name 属性
console.log(user["key"]); // 读取名为 key 的属性
```

#### 2.1.3 读取不存在的属性

读取不存在的属性会得到 `undefined`：

```js
console.log(user.phoneNumber); // undefined
```

但继续访问 `undefined` 的内部属性会报错：

```js
console.log(user.manager.name);
// TypeError: Cannot read properties of undefined
```

第二十章会学习使用可选链 `?.` 安全读取可能不存在的嵌套属性。

### 2.2 新增、修改和删除属性

#### 2.2.1 修改已有属性

```js
user.department = "quality";
user["department"] = "sales";
```

#### 2.2.2 新增属性

为不存在的属性赋值，就会创建新属性：

```js
user.email = "yamada@example.com";
user["active"] = true;
```

#### 2.2.3 使用 `delete` 删除属性

```js
delete user.email;
console.log(user.email); // undefined
```

`delete` 删除对象属性并返回布尔值。业务数据如果要求结构稳定，通常更适合保留属性并设置为 `null` 或约定值，而不是随意删除。

#### 2.2.4 `const` 对象仍能修改属性

```js
const user = {
  name: "山田 太郎"
};

user.name = "山田 花子"; // 可以
```

`const` 禁止变量重新指向另一个值，但不会自动冻结对象内部：

```js
user = { name: "鈴木 一郎" };
// TypeError: Assignment to constant variable
```

### 2.3 判断属性是否存在

#### 2.3.1 `in` 与 `Object.hasOwn()`

`in` 判断属性能否在对象或其原型链上找到；`Object.hasOwn()` 只判断属性是否直接属于当前对象。

```js
console.log("status" in application);                // true
console.log("toString" in application);              // true
console.log(Object.hasOwn(application, "status"));   // true
console.log(Object.hasOwn(application, "toString")); // false
```

`Object.hasOwn()` 参数：

| 参数 | 可接受的值 | 是否必填 | 作用 |
| --- | --- | --- | --- |
| 第一个参数 | 要检查的对象 | 必填 | 指定检查对象 |
| 第二个参数 | 字符串或 Symbol 属性键 | 必填 | 指定属性名 |

旧代码中也常见 `application.hasOwnProperty("status")`，项目新代码优先使用 `Object.hasOwn(object, key)`。

#### 2.3.2 属性存在不等于属性有值

```js
const data = {
  reason: undefined
};

console.log(data.reason);                       // undefined
console.log(data.missingReason);                // undefined
console.log(Object.hasOwn(data, "reason"));    // true
console.log(Object.hasOwn(data, "missingReason")); // false
```

需要判断“是否定义了这个字段”时，不能只检查读取结果，应使用 `Object.hasOwn()` 或 `in`。

## 3. 对象方法与 `this`

### 3.1 定义和调用对象方法

属性值也可以是函数。保存在对象属性中的函数通常称为方法。

```js
const user = {
  employeeNumber: "EMP-00001",
  name: "山田 太郎",
  getLabel: function () {
    return `${this.employeeNumber} ${this.name}`;
  }
};
```

调用方法时需要写圆括号：

```js
console.log(user.getLabel());
// EMP-00001 山田 太郎
```

- `user.getLabel` 取得函数本身，没有执行。
- `user.getLabel()` 调用函数并取得返回值。

#### 3.1.1 方法简写

对象字面量中的方法通常使用简写形式：

```js
const application = {
  status: "pending",
  changeStatus(nextStatus) {
    this.status = nextStatus;
    return this.status;
  }
};

console.log(application.changeStatus("approved")); // approved
```

函数、参数和返回值会在下一章系统讲解。本节先掌握对象方法的定义和调用形式。

### 3.2 理解方法中的 `this`

对象通过点语法调用普通方法时，方法中的 `this` 指向点号左侧的对象。

```js
const application = {
  id: "REQ-001",
  status: "pending",
  showStatus() {
    console.log(`${this.id}: ${this.status}`);
  }
};

application.showStatus(); // REQ-001: pending
```

`this` 的值主要由函数的调用方式决定：

```js
function showName() {
  console.log(this.name);
}

const userA = { name: "山田 太郎", showName };
const userB = { name: "鈴木 花子", showName };

userA.showName(); // 山田 太郎
userB.showName(); // 鈴木 花子
```

#### 3.2.1 不要用箭头函数定义需要自身 `this` 的方法

箭头函数没有自己的 `this`。下面的 `this` 不会自动变成 `user`：

```js
const user = {
  name: "山田 太郎",
  showName: () => {
    console.log(this.name);
  }
};
```

需要读取当前对象时，使用普通方法简写：

```js
const user = {
  name: "山田 太郎",
  showName() {
    console.log(this.name);
  }
};
```

## 4. 使用对象组织业务数据

### 4.1 嵌套对象

对象的属性值还可以是另一个对象：

```js
const user = {
  employeeNumber: "EMP-00001",
  name: "山田 太郎",
  department: {
    code: "DEV",
    name: "開発部"
  },
  contact: {
    email: "yamada@example.com",
    extensionNumber: "1234"
  }
};
```

从外到内逐层读取和修改：

```js
console.log(user.department.name); // 開発部
user.department.name = "品質管理部";
```

嵌套结构应以业务含义为准。层级过深会增加读取和修改的难度。

### 4.2 对象数组

实际项目经常使用“数组保存多条记录，对象描述每条记录”的结构。

```js
const applications = [
  {
    id: "REQ-20260820-001",
    status: "pending",
    applicant: {
      employeeNumber: "EMP-00001",
      name: "山田 太郎"
    }
  },
  {
    id: "REQ-20260821-001",
    status: "approved",
    applicant: {
      employeeNumber: "EMP-00002",
      name: "鈴木 花子"
    }
  }
];
```

读取第一条记录：

```js
console.log(applications[0].id);
console.log(applications[0].applicant.name);
```

依次处理所有记录：

```js
for (const application of applications) {
  console.log(`${application.id}: ${application.applicant.name}`);
}
```

第七章已经讲解使用数组回调方法查找、筛选和转换元素；本章把这些方法用于对象数组。

### 4.3 映射对象与稳定的数据结构

映射对象可以把程序保存值转换成页面显示文字：

```js
const statusLabels = {
  pending: "申請中",
  approved: "承認済",
  rejected: "差戻し",
  cancelled: "取消済"
};

const application = {
  id: "REQ-001",
  status: "pending"
};

console.log(statusLabels[application.status]); // 申請中
```

这里必须使用方括号，因为属性名来自 `application.status` 的值。

同一类业务对象应尽量保持相同字段名称和数据类型：

```js
const applications = [
  { id: "REQ-001", status: "pending", approver: null },
  { id: "REQ-002", status: "approved", approver: "EMP-00010" }
];
```

- `""` 表示字符串字段当前为空。
- `null` 通常表示有这个字段，但当前明确没有值。
- `undefined` 常表示没有赋值或读取了不存在的属性。

稳定结构更容易校验、渲染，也便于后续使用 TypeScript 描述对象类型。

## 5. 遍历对象属性

### 5.1 使用 `for...in`

`for...in` 每次取得一个可枚举属性名：

```js
const application = {
  id: "REQ-001",
  leaveType: "paid",
  status: "pending"
};

for (const key in application) {
  if (Object.hasOwn(application, key)) {
    console.log(key, application[key]);
  }
}
```

示例输出：

```text
id REQ-001
leaveType paid
status pending
```

`key` 是变量，因此读取值时必须写 `application[key]`。`Object.hasOwn()` 用于排除从原型继承的属性。

### 5.2 `Object.keys()`、`Object.values()` 和 `Object.entries()`

```js
console.log(Object.keys(application));
// ["id", "leaveType", "status"]

console.log(Object.values(application));
// ["REQ-001", "paid", "pending"]

console.log(Object.entries(application));
// [["id", "REQ-001"], ["leaveType", "paid"], ["status", "pending"]]
```

| 方法 | 参数及可接受值 | 默认值 | 返回值 |
| --- | --- | --- | --- |
| `Object.keys(object)` | 要读取的对象，必填 | 无 | 自有可枚举属性名数组 |
| `Object.values(object)` | 要读取的对象，必填 | 无 | 自有可枚举属性值数组 |
| `Object.entries(object)` | 要读取的对象，必填 | 无 | `[属性名, 属性值]` 数组 |

同时取得键和值：

```js
for (const [key, value] of Object.entries(application)) {
  console.log(key, value);
}
```

`[key, value]` 是数组解构，当前可以理解为把每组中的两个值分别取出，第二十章会系统讲解。

不要使用 `for...in` 遍历数组。数组需要按元素处理时优先使用 `for...of` 或数组方法。

## 6. 理解对象的引用特点

原始值赋值时会复制值本身：

```js
let statusA = "pending";
let statusB = statusA;
statusB = "approved";

console.log(statusA); // pending
```

对象赋值时，两个变量会指向同一个对象：

```js
const applicationA = { status: "pending" };
const applicationB = applicationA;

applicationB.status = "approved";

console.log(applicationA.status); // approved
console.log(applicationB.status); // approved
```

### 6.1 对象比较的是引用

```js
const applicationA = { id: "REQ-001" };
const applicationB = { id: "REQ-001" };
const applicationC = applicationA;

console.log(applicationA === applicationB); // false
console.log(applicationA === applicationC); // true
```

内容看起来相同的两个对象，仍然是分别创建的对象。严格相等比较的是它们是否为同一个对象。

直接赋值不是复制对象。下面继续比较浅复制和深复制。

### 6.2 浅复制

展开语法 `{ ...original }` 会创建一个新的最外层对象，但嵌套对象仍然共享引用，这称为**浅复制**。

```js
const original = {
  id: "REQ-001",
  applicant: { name: "山田" }
};

const copied = { ...original };
copied.id = "REQ-002";
copied.applicant.name = "鈴木";

console.log(original.id);             // REQ-001
console.log(original.applicant.name); // 鈴木
```

最外层的 `id` 已经独立，但 `applicant` 仍指向同一个对象。展开语法会在第二十章系统讲解；这里先观察复制结果。

### 6.3 使用 structuredClone() 深复制

**深复制**会为嵌套数据也创建独立副本。`structuredClone(value)` 使用浏览器的结构化克隆算法复制支持的数据：

```js
const original = {
  applicant: { name: "山田" },
  dates: ["2026-09-01", "2026-09-02"]
};

const copied = structuredClone(original);
copied.applicant.name = "鈴木";

console.log(original.applicant.name); // 山田
console.log(copied.applicant.name);   // 鈴木
```

`structuredClone()` 不能复制函数和 DOM 节点，复制类实例时也不会保留完整的自定义原型行为。不要使用 `JSON.stringify()` 和 `JSON.parse()` 充当通用深复制方法，因为它们会丢失部分数据类型。

### 6.4 用递归理解深复制过程（了解即可）

下面的函数只演示普通数组和普通对象如何逐层复制，不是通用克隆库：

```js
function clonePlainData(value) {
  if (Array.isArray(value)) {
    return value.map(clonePlainData);
  }

  if (value !== null && typeof value === "object") {
    const result = {};

    for (const key in value) {
      if (Object.hasOwn(value, key)) {
        result[key] = clonePlainData(value[key]);
      }
    }

    return result;
  }

  return value;
}
```

它没有处理循环引用、`Date`、`Map`、`Set`、函数、DOM节点和对象原型。实际项目应先确认数据类型和复制目的，再选择浅复制或 `structuredClone()`。

## 7. 构造函数、实例与原型

### 7.1 构造函数与实例

需要按照相同规则反复创建对象时，可以使用构造函数。构造函数本质上仍是函数，约定以大写字母开头，并使用 `new` 调用。

```js
function Application(id, leaveType, status) {
  this.id = id;
  this.leaveType = leaveType;
  this.status = status;
}

const applicationA = new Application("REQ-001", "paid", "pending");
const applicationB = new Application("REQ-002", "half-pm", "approved");
```

- `Application` 是构造函数。
- `new Application(...)` 创建并返回一个新对象。
- `applicationA` 和 `applicationB` 是两个不同实例。
- 构造函数中的 `this` 指向本次新创建的对象。

```js
console.log(applicationA.id);     // REQ-001
console.log(applicationB.status); // approved
console.log(applicationA === applicationB); // false
```

#### 7.1.1 `new` 的基本过程

1. 创建一个新对象。
2. 让构造函数中的 `this` 指向新对象。
3. 执行构造函数，为新对象设置属性。
4. 构造函数没有显式返回其他对象时，返回这个新对象。

忘记 `new` 时，在现代严格模式或 ES 模块中会因为 `this` 是 `undefined` 而报错。看到首字母大写的构造函数时，应使用 `new` 调用。

### 7.2 `prototype` 与共享方法

如果在构造函数内部创建方法，每个实例会得到不同的函数对象：

```js
function Application(id) {
  this.id = id;
  this.showId = function () {
    console.log(this.id);
  };
}

const applicationA = new Application("REQ-001");
const applicationB = new Application("REQ-002");

console.log(applicationA.showId === applicationB.showId); // false
```

实例可以访问构造函数 `prototype` 对象上的方法，因此共享方法通常放到原型上：

```js
function Application(id, status) {
  this.id = id;
  this.status = status;
}

Application.prototype.showStatus = function () {
  console.log(`${this.id}: ${this.status}`);
};

const applicationA = new Application("REQ-001", "pending");
const applicationB = new Application("REQ-002", "approved");

applicationA.showStatus(); // REQ-001: pending
applicationB.showStatus(); // REQ-002: approved
console.log(applicationA.showStatus === applicationB.showStatus); // true
```

### 7.3 原型链查找

读取对象属性时，JavaScript 会按顺序查找：

1. 先查找对象自身。
2. 没有时，到对象的原型上查找。
3. 沿着原型继续向上查找。
4. 到达原型链终点仍未找到时，返回 `undefined`。

```js
console.log(Object.hasOwn(applicationA, "id"));         // true
console.log(Object.hasOwn(applicationA, "showStatus")); // false
console.log("showStatus" in applicationA);              // true

console.log(
  Object.getPrototypeOf(applicationA) === Application.prototype
); // true
```

`id` 是实例的自有属性；`showStatus` 位于原型上，但实例仍能沿原型链访问它。

旧代码和控制台中可能看到 `__proto__`，新代码不要依赖它修改原型。零基础阶段重点理解“先查自身，再沿原型向上查找”。现代项目也常使用 `class`，构造函数和原型知识有助于阅读旧项目并理解 JavaScript 的对象机制。

## 8. 常见错误与排查

| 现象 | 原因 | 修正方式 |
| --- | --- | --- |
| `user.key` 得不到变量指定的属性 | 点语法把 `key` 当成固定属性名 | 改为 `user[key]` |
| 特殊属性名访问时报错 | 对包含连字符的属性使用了点语法 | 改为 `object["content-type"]` |
| 调用方法没有结果 | 只写了 `user.showName` | 写成 `user.showName()` |
| 方法中的 `this` 不是当前对象 | 使用箭头函数定义了依赖自身 `this` 的方法 | 使用普通方法简写 |
| 修改副本时原对象也变化 | 直接赋值只复制了对象引用 | 根据嵌套层级选择浅复制或 `structuredClone()` |
| 两个内容相同的对象比较为 `false` | `===` 比较对象引用 | 按需要比较业务字段 |
| `for...in` 输出继承属性 | 没有限制为自有属性 | 配合 `Object.hasOwn()` |
| 构造函数调用时报错 | 忘记使用 `new` | 使用 `new Constructor(...)` |

## 9. 本章练习

### 9.1 创建和修改设备借用对象

```js
const equipmentLoan = {
  id: "LOAN-20260820-001",
  equipmentName: "测试用笔记本电脑",
  status: "reserved",
  borrower: {
    employeeNumber: "EMP-00001",
    name: "山田 太郎"
  }
};
```

完成：

1. 输出借用编号、设备名称和借用人姓名。
2. 把状态改成 `"borrowed"`。
3. 新增 `returnDueDate`，值为 `"2026-08-31"`。
4. 新增临时属性 `inspectionMemo`，再使用 `delete` 删除。
5. 使用 `Object.hasOwn()` 验证属性状态。

### 9.2 方法与 `this`

为设备借用对象添加 `getSummary()` 方法，通过 `this` 返回：

```text
LOAN-20260820-001 测试用笔记本电脑 borrowed
```

分别输出 `equipmentLoan.getSummary` 和 `equipmentLoan.getSummary()`，观察两者区别。

### 9.3 遍历对象

对下面的对象分别使用 `Object.keys()`、`Object.values()` 和 `Object.entries()`：

```js
const priorityLabels = {
  low: "低",
  normal: "普通",
  high: "高"
};
```

验证属性名数组长度为 `3`，属性值中包含 `"普通"`，并输出每组优先级代码和显示文字。

### 9.4 观察引用

把一个设备对象赋给另一个变量，再通过第二个变量修改保管位置。输出两个变量并解释结果。然后创建两个内容相同的新对象，使用 `===` 比较。

### 9.5 扩展练习：构造函数和原型

本练习用于阅读既有代码，不作为零基础主线验收要求。编写 `Equipment(assetId, name)`：

1. 使用 `new` 创建两个实例。
2. 在 `Equipment.prototype` 上定义 `showLabel()`。
3. 分别调用两个实例的方法。
4. 验证两个实例的 `showLabel` 是否为同一个函数。
5. 使用 `Object.hasOwn()` 检查 `assetId` 和 `showLabel`，解释差异。

## 本章检查点

- 能使用对象字面量组织业务数据。
- 能正确选择点语法或方括号语法。
- 能新增、修改和删除属性，并说明 `const` 对象仍可修改属性的原因。
- 能定义和调用对象方法，说明普通方法中的 `this`。
- 能读取嵌套对象，并遍历对象数组。
- 能使用常见方式处理对象的键和值。
- 能区分 `in` 与 `Object.hasOwn()`。
- 能说明对象赋值和严格相等比较的引用特点。
- 能看懂构造函数、实例、共享原型方法和原型链查找。
