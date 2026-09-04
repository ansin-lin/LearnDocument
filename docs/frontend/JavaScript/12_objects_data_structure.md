# 第十二章 对象与数据结构

## 学习目标

完成本章后，你应能够：

- 使用对象字面量创建和组织业务数据。
- 读取、新增、修改和删除对象属性。
- 区分点语法和方括号语法的使用场景。
- 定义对象方法，并能根据调用方式判断普通函数中的 `this`。
- 处理嵌套对象和对象数组。
- 遍历对象，并判断属性是否属于对象自身。
- 说明对象赋值、修改和比较时的引用特点。
- 看懂构造函数、实例、`prototype` 和原型链的基础代码。

## 掌握要求

- **必须掌握**：对象字面量、属性操作、对象方法、嵌套对象、对象数组、常用遍历方式和引用特点。
- **需要掌握**：计算属性名、属性存在性判断、对象方法中的 `this`、浅复制和`structuredClone()`的适用边界。
- **会使用、能看懂**：方法被单独取出时的 `this`、`call()`、`apply()`、`bind()`、构造函数、实例、`prototype`、原型链查找、自有属性与继承属性。
- **后续学习**：对象解构、完整展开语法、`Map`、`Set`、`Proxy` 和 `Reflect`。

## 示例运行约定

本章不需要页面控件，示例主要观察控制台输出。创建 `object-demo.html` 和同目录的 `object-demo.js`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>对象实验</title>
  <script src="object-demo.js" defer></script>
</head>
<body>
  <p>打开开发者工具的 Console，查看对象实验结果。</p>
</body>
</html>
```

每个独立实验都替换整个 `object-demo.js`，保存后刷新。同一小节中明确要求追加的代码，放在该小节已有代码之后；不要把整章所有示例拼成一个文件，否则重复声明 `const user` 等变量会报错。错误示例单独实验，观察后移除。

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

## 2. 操作对象属性

### 2.1 读取对象属性

#### 2.1.1 点语法

属性名固定并且是普通标识符时，优先使用点语法。这个独立实验包含对象创建和读取：

```js
const user = {
  name: "山田 太郎",
  department: "development"
};

console.log(user.name);       // 山田 太郎
console.log(user.department); // development
```

点号左边是要读取的对象，右边是确定的属性名。

#### 2.1.2 方括号语法

属性名来自变量时，使用方括号。独立实验：

```js
const user = { name: "山田 太郎", department: "development" };
const key = "department";

console.log(user[key]);   // development
console.log(user["key"]); // undefined
console.log(user.key);    // undefined
```

`user[key]` 先读取变量 `key` 的值，再查找名为 `department` 的属性。`user["key"]` 和 `user.key` 都查找固定名称 `key`，本例没有这个属性。把变量值改为 `"name"`，第一行读取结果随之变为姓名。

属性名包含连字符等特殊字符时，也需要方括号。下面是另一个独立实验：

```js
const labels = {
  "display-name": "显示名称"
};

console.log(labels["display-name"]); // 显示名称
```

带特殊字符的属性名在创建时加引号，访问时写成 `对象["属性名"]`。不能写 `labels.display-name`，那会被理解为减法表达式。

#### 2.1.3 读取不存在的属性

读取不存在的属性得到 `undefined`：

```js
const user = { name: "山田" };
console.log(user.phoneNumber); // undefined
```

在这段代码后单独追加下面的错误示例，会因为 `user.manager` 是 `undefined` 而报错：

```js
console.log(user.manager.name);
// TypeError: Cannot read properties of undefined
```

修正时不能只看最后的 `name`，要先确认中间的 `manager` 是否存在。用下面完整脚本替换错误实验：

```js
const user = { name: "山田", manager: null };

if (user.manager !== null && user.manager !== undefined) {
  console.log(user.manager.name);
} else {
  console.log("暂未设置负责人");
}
```

`&&` 从左向右判断，两个条件都成立才进入读取分支。本例假定 `manager` 只可能是负责人对象、`null` 或未设置；来自外部的不确定数据还要检查类型和字段。

先运行应输出“暂未设置负责人”；再把 `manager: null` 改为 `manager: { name: "佐藤" }`，应输出“佐藤”。第二十章的可选链 `?.` 可以简化这类取值，当前使用 `if` 已能处理缺失情况。

### 2.2 新增、修改和删除属性

#### 2.2.1 修改已有属性

```js
const user = { department: "development" };
user.department = "quality";
console.log(user.department); // quality

user["department"] = "sales";
console.log(user.department); // sales
```

点语法和方括号都能赋值。属性已经存在时，是修改它的值。

#### 2.2.2 新增属性

为不存在的属性赋值，会创建新属性：

```js
const user = { name: "山田" };
user.email = "yamada@example.com";
user["active"] = true;

console.log(user.email);  // yamada@example.com
console.log(user.active); // true
```

#### 2.2.3 使用 `delete` 删除属性

```js
const user = { name: "山田", email: "yamada@example.com" };
delete user.email;
console.log(user.email); // undefined
```

`delete` 删除对象属性，本例删除成功，表达式结果为 `true`。它不是把属性值改为空，而是移除这个属性；后面的属性存在性判断可以验证区别。

业务数据如果要求结构稳定，通常更适合保留属性并设置为 `null` 或约定值，而不是随意删除。

#### 2.2.4 `const` 对象仍能修改属性

```js
const user = { name: "山田 太郎" };
user.name = "山田 花子";
console.log(user.name); // 山田 花子
```

`const` 禁止变量重新指向另一个值，但不会自动冻结对象内部。在上面脚本末尾单独追加以下错误示例：

```js
user = { name: "鈴木 一郎" };
// TypeError: Assignment to constant variable
```

修改 `user.name` 是修改原对象的内容；给 `user` 重新赋值，是要求变量指向另一个对象。这两种操作不同。

### 2.3 判断属性是否存在

#### 2.3.1 自有属性与继承属性

直接定义在对象上的属性叫**自有属性**。对象还可以沿着一种称为“原型”的关联关系，找到其他对象提供的属性或方法，这些称为**继承属性**。例如普通对象通常可以使用继承来的 `toString` 方法，即使没有在自己的花括号里定义它。

这里只比较“自己有”和“能找到”，原型的创建和查找过程见第 8 节。`in` 检查自己或原型上能否找到属性；`Object.hasOwn()` 只检查对象自身。

```js
const application = { id: "REQ-001", status: "pending" };

console.log("status" in application);                // true
console.log("toString" in application);              // true
console.log(Object.hasOwn(application, "status"));   // true
console.log(Object.hasOwn(application, "toString")); // false
```

这里检查的是名为 `toString` 的方法是否存在，没有调用它。`toString()` 通常用于字符串表示，但当前不需要依靠它转换业务数据。

`Object.hasOwn()` 返回布尔值，参数如下：

| 参数 | 可接受的值 | 是否必填 | 作用 |
| --- | --- | --- | --- |
| 第一个参数 | 要检查的对象 | 必填 | 指定检查对象 |
| 第二个参数 | 属性名字符串；也支持后续学习的 Symbol 键 | 必填 | 指定属性名 |

旧代码中的 `application.hasOwnProperty("status")` 也用于判断自有属性。新代码优先使用 `Object.hasOwn(application, "status")`，即使对象自定义了同名 `hasOwnProperty` 属性，也不会影响调用。

#### 2.3.2 属性存在不等于属性有值

```js
const data = { reason: undefined };

console.log(data.reason);                          // undefined
console.log(data.missingReason);                   // undefined
console.log(Object.hasOwn(data, "reason"));         // true
console.log(Object.hasOwn(data, "missingReason"));  // false

delete data.reason;
console.log(Object.hasOwn(data, "reason"));         // false
```

值为 `undefined` 时，属性仍然可以存在；`delete` 才移除了它。需要判断“是否定义了这个字段”时，不能只检查读取结果。

同样，值为 `0`、`false`、`""` 不表示字段不存在。不能用 `if (data.someField)` 代替属性存在性检查。

## 3. 属性名与简写形式

### 3.1 普通属性名

普通标识符形式的属性名可以不加引号：

```js
const user = {
  name: "山田 太郎",
  department: "development"
};
```

包含连字符、空格等特殊字符时，需要使用字符串形式：

```js
const labels = {
  "display-name": "显示名称",
  "help text": "帮助文字"
};
```

### 3.2 属性简写

属性名与已有变量名相同时，可以只写变量名：

```js
const name = "山田";
const status = "pending";
const user = { name, status };
console.log(user); // { name: "山田", status: "pending" }
```

`{ name, status }` 等价于 `{ name: name, status: status }`。冒号左侧是属性名，右侧是变量的值；简写不是省略变量声明，`name` 和 `status` 必须已存在。属性值为函数时也适用。

### 3.3 计算属性名

属性名来自变量或表达式时，在对象字面量中使用 `[]`：

```js
const statusKey = "pending";

const statusLabels = {
  [statusKey]: "申請中"
};

console.log(statusLabels.pending); // 申請中
```

实际创建的属性名是变量保存的 `"pending"`，不是字符串 `"statusKey"`。

先确认自己能创建一条对象、读取一个属性并修改它。接下来改变的不是读写规则，而是数据规模：一个属性可以保存另一条对象，一个数组可以保存多条对象。

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

第七章用数字学习了数组回调。对象数组也一样，只是回调接收的是一条记录，需要读取属性后再判断。

#### 4.2.1 查找一条记录

`find()` 在第七章用于查找数组元素。对象数组中，回调参数每次收到一条对象，需要按属性判断。独立实验：

```js
const applications = [
  { id: "REQ-001", days: 1 },
  { id: "REQ-002", days: 2 }
];

const target = applications.find(app => app.id === "REQ-002");
if (target !== undefined) {
  console.log(target.days); // 2
} else {
  console.log("没有找到申请");
}
```

`app` 是回调参数名，不是特殊关键字。`app.id` 读取当前记录编号。把目标编号改为 `"REQ-999"` 再运行，验证未找到分支。

#### 4.2.2 筛选多条记录

`filter()` 保留符合条件的元素，返回新数组。独立实验：

```js
const applications = [
  { id: "REQ-001", status: "pending" },
  { id: "REQ-002", status: "approved" },
  { id: "REQ-003", status: "pending" }
];

const pendingApplications = applications.filter(app => app.status === "pending");
console.log(pendingApplications.length); // 2
console.log(pendingApplications[0].id);  // REQ-001
```

把筛选条件改为 `"approved"`，结果应只有一条。没有匹配项时得到空数组，不是 `undefined`。

#### 4.2.3 提取一组属性值

`map()` 把每个元素转换成回调返回的值。独立实验：

```js
const applications = [
  { id: "REQ-001", days: 1 },
  { id: "REQ-002", days: 2 }
];

const ids = applications.map(app => app.id);
console.log(ids); // ["REQ-001", "REQ-002"]
```

原数组保存对象，结果数组保存字符串。把 `app.id` 改为 `app.days`，结果应为 `[1, 2]`。这里没有修改原记录。

#### 4.2.4 汇总数值

`reduce()` 把每条记录的数值累加到一个结果中。独立实验：

```js
const applications = [
  { id: "REQ-001", days: 1 },
  { id: "REQ-002", days: 2 },
  { id: "REQ-003", days: 3 }
];

const totalDays = applications.reduce((total, app) => total + app.days, 0);
console.log(totalDays); // 6
```

初始累计值为 `0`，每次加上当前记录的 `days`，依次得到 `1 → 3 → 6`。把数组改为空数组再运行，结果为 `0`。

#### 4.2.5 排序时保留原数组顺序

`sort()` 会直接改变调用它的数组。`slice()` 不传参数时，先复制整个数组，再对复制结果排序：

```js
const applications = [
  { id: "REQ-001", days: 1 },
  { id: "REQ-002", days: 3 },
  { id: "REQ-003", days: 2 }
];

const sortedApplications = applications.slice();
sortedApplications.sort((a, b) => b.days - a.days);

console.log(sortedApplications[0].id); // REQ-002
console.log(applications[0].id);       // REQ-001
```

`b.days - a.days` 按天数降序排列。这只保证原数组的顺序不变，并不表示每条对象记录也被复制；第 6 节继续验证对象引用。

阶段练习：独立筛选已批准的记录，再统计它们的天数；使用 `some()` 判断是否有待审批记录，使用 `every()` 检查所有记录的天数是否大于零。

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

可枚举属性是允许出现在这类属性遍历中的属性。通过对象字面量创建的普通字段，默认可枚举；并非对象上所有能访问的属性都会被遍历出来，例如通常继承的 `toString` 不可枚举。

`for...in` 每次取得一个可枚举的字符串属性名，也可能遍历到继承的可枚举属性，因此配合 `Object.hasOwn()` 限制为自身字段：

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

下面三种方法分别把对象自身可枚举的字符串属性名、属性值和键值对组成数组。它们不会把继承属性一起收集。独立实验：

```js
const application = { id: "REQ-001", leaveType: "paid", status: "pending" };

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

在上面的脚本末尾追加以下代码，同时取得键和值：

```js
for (const entry of Object.entries(application)) {
  const key = entry[0];
  const value = entry[1];
  console.log(key, value);
}
```

`entry` 是一个包含两项的数组：`entry[0]` 为属性名，`entry[1]` 为属性值。这里沿用第四章的下标读取。

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

只创建新的最外层对象，而把属性值直接赋给它，嵌套对象仍然共享引用，这称为**浅复制**。

```js
const original = {
  id: "REQ-001",
  applicant: { name: "山田" }
};

const copied = {
  id: original.id,
  applicant: original.applicant
};
copied.id = "REQ-002";
copied.applicant.name = "鈴木";

console.log(original.id);             // REQ-001
console.log(original.applicant.name); // 鈴木
```

最外层的 `id` 已经独立，但 `applicant` 仍指向同一个对象。这是逐个属性赋值，和直接写 `const copied = original` 不同：后者连最外层对象也没有新建。

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

### 6.4 对象作为函数参数

把对象交给函数时，参数得到的是同一对象的引用值。通过参数修改属性，会影响调用者正在使用的对象：

```js
const application = { status: "pending" };

function approve(target) {
  target.status = "approved";
}

approve(application);
console.log(application.status); // approved
```

调用时 `application` 和参数 `target` 都指向同一条记录。`target.status = ...` 修改的就是这条记录。

但给参数重新赋值，只改变函数内部参数指向哪里。独立对照实验：

```js
const application = { status: "pending" };

function replaceApplication(target) {
  target = { status: "approved" };
  console.log(target.status); // approved
}

replaceApplication(application);
console.log(application.status); // pending
```

参数 `target` 改为指向新对象，外面的 `application` 仍指向原对象。JavaScript 传递的是值；对象情况下，这个值是引用，不是把外部变量本身交给函数重新绑定。

### 6.5 函数返回新对象

需要创建结构一致的数据，或者修改时保留原值，可以让普通函数返回一个新对象：

```js
function createUser(name) {
  return {
    name: name,
    active: true
  };
}

const firstUser = createUser("山田");
const secondUser = createUser("山田");
firstUser.active = false;

console.log(firstUser.active);             // false
console.log(secondUser.active);            // true
console.log(firstUser === secondUser);     // false
```

每调用一次，函数中的对象字面量就创建一个新对象，再由 `return` 交给调用者。这是“函数返回对象”，不等于“对象的方法”。

箭头函数也可以返回新对象。下面是相同功能的独立替代写法：

```js
const createUser = name => ({
  name: name,
  active: true
});

console.log(createUser("山田").name); // 山田
```

箭头函数在没有代码块时会返回表达式的值。`({ ... })` 外层圆括号让花括号被理解为对象表达式；如果写成函数代码块，就要像普通函数一样显式写 `return`。

### 6.6 对象数组中的引用

#### 6.6.1 find() 返回的对象不是副本

```js
const applications = [
  { id: "REQ-001", status: "pending" },
  { id: "REQ-002", status: "pending" }
];

const target = applications.find(app => app.id === "REQ-002");
if (target !== undefined) {
  target.status = "approved";
}

console.log(applications[1].status); // approved
```

`find()` 返回数组中匹配元素的值；本例元素是对象，因此通过 `target` 修改属性，会修改原记录。这适合“查到申请后更新它”，不适合在未保存的编辑预览中直接改原数据。

#### 6.6.2 新数组也可能共享原来的对象

```js
const applications = [
  { id: "REQ-001", status: "pending" },
  { id: "REQ-002", status: "approved" }
];

const pending = applications.filter(app => app.status === "pending");
pending[0].status = "cancelled";

console.log(pending === applications);       // false
console.log(pending[0] === applications[0]);  // true
console.log(applications[0].status);          // cancelled
console.log(pending.length);                 // 1
```

数组是新建的，但保留下来的元素仍引用原对象。修改状态后，`pending` 不会自动重新筛选，所以长度仍为 1。

`slice()` 也有相同的浅复制特点。独立实验：

```js
const applications = [{ id: "REQ-001", days: 1 }];
const copied = applications.slice();

copied[0].days = 5;
console.log(applications[0].days); // 5

copied.push({ id: "REQ-002", days: 2 });
console.log(copied.length);       // 2
console.log(applications.length); // 1
```

修改共享记录的属性会相互影响；向新数组追加元素，不会给原数组追加元素。`map()` 是否得到新对象取决于回调：返回原对象仍共享引用，返回对象字面量才会创建新对象。

### 6.7 什么时候选择赋值、浅复制或深复制

| 需求 | 选择 | 需要注意 |
| --- | --- | --- |
| 查到一条记录后直接更新它 | 使用原对象引用 | 所有引用该对象的位置都会看到修改 |
| 改变列表顺序，不改记录本身 | 浅复制数组再排序 | 数组独立，记录仍可能共享 |
| 只修改副本的顶层字段 | 新建对象并复制所需属性 | 嵌套对象和数组仍可能共享 |
| 编辑包含嵌套数据的草稿，不影响原记录 | 对支持的数据使用 `structuredClone()` | 先确认没有函数、DOM 节点等不支持的数据 |

不需要每次都深复制。先确认“哪些内容必须独立”，再决定复制范围。下面对照实验只观察普通嵌套数据，不涉及浏览器存储：

```js
const original = { contact: { name: "山田" } };
const draft = structuredClone(original);

draft.contact.name = "佐藤";
console.log(original.contact.name); // 山田
console.log(draft.contact.name);    // 佐藤
```

### 6.8 用递归理解深复制过程（了解即可）

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

## 7. 对象方法与 `this`

### 7.1 定义和调用对象方法

属性值也可以是函数，保存在对象属性中的函数通常称为方法。先观察不需要 `this` 的独立示例：

```js
const calculator = {
  double: function (value) {
    return value * 2;
  }
};

console.log(calculator.double);    // 输出函数本身
console.log(calculator.double(3)); // 6
```

- `calculator.double` 取得函数，但不执行。
- `calculator.double(3)` 调用方法，把 `3` 传给参数 `value`，再取得返回值。
- `double` 是本例自己定义的方法名，不是 JavaScript 内置方法。

#### 7.1.1 方法简写

对象字面量中可以把 `double: function (value)` 简写为 `double(value)`。下面是完整替代版本：

```js
const calculator = {
  double(value) {
    return value * 2;
  }
};

console.log(calculator.double(3)); // 6
```

两种写法在这个实验中完成相同任务。接下来让方法读取它所属对象的字段，就需要认识 `this`。

### 7.2 先记住判断原则：看调用方式

`this` 是函数执行时取得的一个值，用于表示“本次调用时正在操作的对象”。它不是固定的对象名称，也不能只看函数写在哪里来判断。

判断普通函数中的 `this` 时，先看函数怎样被调用：

| 调用方式 | 示例 | 普通函数中的 `this` |
| --- | --- | --- |
| 对象方法调用 | `application.showStatus()` | 点号左侧的 `application` |
| 单独调用 | `showStatus()` | 严格模式下是 `undefined`；非严格的浏览器普通脚本中通常是 `window` |
| 指定对象后调用 | `showStatus.call(application)` | `call()` 指定的 `application` |
| 构造调用 | `new Application()` | `new` 创建的新对象 |
| 箭头函数 | `() => {}` | 不创建自己的 `this`，使用外层作用域的 `this` |

新人最常用的是“对象方法调用”。看到 `对象.方法()` 时，普通方法内的 `this` 通常就是点号左侧的对象。

### 7.3 作为对象方法调用

用 `application.showStatus()` 调用普通方法时，`this` 指向点号左侧的 `application`。因此方法可以通过 `this.status` 读取当前调用对象的状态：

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

`this.id` 和 `this.status` 分别读取“本次调用者”的两个属性。这样的方法不必把对象名称 `application` 写死，能够复用于其他具有相同字段的对象。

`this` 不是在定义函数时永久绑定到某个对象。下面是独立实验：同一个函数放到两个对象上，由谁以方法形式调用，就读取谁的姓名。

```js
function showName() {
  console.log(this.name);
}

const userA = { name: "山田 太郎", showName };
const userB = { name: "鈴木 花子", showName };

userA.showName(); // 山田 太郎
userB.showName(); // 鈴木 花子
```

下面是独立实验，观察方法怎样修改当前对象：

```js
const application = { status: "pending" };

application.changeStatus = function (nextStatus) {
  this.status = nextStatus;
  return this.status;
};

console.log(application.changeStatus("approved")); // approved
console.log(application.status);                   // approved
```

这里通过属性赋值添加方法，参数 `nextStatus` 接收新状态。`this.status` 修改本次调用该方法的对象，`return` 把修改后的值交给调用者。

### 7.4 方法被单独取出后，调用者会丢失

下面的 `showStatus` 变量只保存函数本身，不再保留前面的 `application.`：

```js
"use strict";

const application = {
  status: "pending",
  showStatus() {
    console.log(this.status);
  }
};

const showStatus = application.showStatus;
showStatus();
// TypeError: Cannot read properties of undefined
```

`"use strict"` 开启严格模式。严格模式下直接写 `showStatus()` 调用普通函数时，`this` 是 `undefined`，所以不能继续读取 `this.status`。

即使函数最初写在 `application` 对象中，把它取出后单独调用，也不会自动记住原对象。常见修正方式有两种：

```js
application.showStatus(); // 保留“对象.方法()”调用

const boundShowStatus = application.showStatus.bind(application);
boundShowStatus(); // 使用 bind() 固定调用对象
```

`bind(object)` 返回一个新函数，并把新函数运行时的 `this` 固定为指定对象。它不会立即执行原函数。

### 7.5 使用 `call()`、`apply()` 和 `bind()` 指定 `this`

这三个函数方法都能明确指定普通函数中的 `this`，区别在于是否立即调用以及怎样传入参数。

```js
function showApplication(prefix, suffix) {
  console.log(`${prefix}${this.id}${suffix}`);
}

const application = { id: "REQ-001" };

showApplication.call(application, "申请：", "（确认中）");
showApplication.apply(application, ["申请：", "（确认中）"]);

const boundShowApplication = showApplication.bind(
  application,
  "申请：",
  "（确认中）"
);
boundShowApplication();
```

三行都会输出 `申请：REQ-001（确认中）`。

| 方法 | 是否立即执行 | 参数写法 | 返回值 |
| --- | --- | --- | --- |
| `call(thisArg, arg1, arg2)` | 是 | 参数逐个传入 | 原函数的返回值 |
| `apply(thisArg, argsArray)` | 是 | 参数放在数组中 | 原函数的返回值 |
| `bind(thisArg, arg1, arg2)` | 否 | 可先传入部分或全部参数 | 固定了 `this` 的新函数 |

第一个参数 `thisArg` 是希望函数内部 `this` 指向的对象。`call()` 和 `apply()` 适合“现在就用指定对象执行”，`bind()` 适合“把函数交给其他代码，稍后再执行”。业务代码不需要为了使用 `this` 而刻意调用它们，但应能看懂既有代码。

### 7.6 箭头函数没有自己的 `this`

不要使用箭头函数定义依赖对象自身 `this` 的方法。下面虽然写在 `user` 对象中，但箭头函数不会因为 `user.showName()` 这种调用方式而得到 `user`：

```js
const user = {
  name: "山田 太郎",
  showName: () => {
    console.log(this.name);
  }
};
```

在本章的普通浏览器脚本中，这个箭头函数使用外层脚本的 `this`，而不是 `user`，所以结果不会是“山田 太郎”。需要读取当前对象时，改用普通方法简写：

```js
const user = {
  name: "山田 太郎",
  showName() {
    console.log(this.name);
  }
};

user.showName(); // 山田 太郎
```

箭头函数适合放在普通方法内部作为回调，因为它会沿用外层普通方法的 `this`：

```js
const application = {
  id: "REQ-001",
  reviewers: ["佐藤", "鈴木"],
  showReviewers() {
    this.reviewers.forEach((reviewer) => {
      console.log(`${this.id}: ${reviewer}`);
    });
  }
};

application.showReviewers();
// REQ-001: 佐藤
// REQ-001: 鈴木
```

`showReviewers()` 是普通方法，所以其中的 `this` 是 `application`。内部箭头函数不创建新 `this`，继续使用外层方法的 `this`。`forEach()` 已在第七章讲解，本例只观察 `this` 的传递。

### 7.7 构造函数调用与事件监听中的 `this`

使用 `new Application()` 调用构造函数时，构造函数中的 `this` 指向本次创建的新对象。完整创建过程和示例见下一节。

在 DOM 事件监听中，普通监听函数里的 `this` 通常与 `event.currentTarget` 相同，指向注册监听器的元素；箭头函数仍然没有自己的 `this`。为了让代码含义更明确，事件处理中优先读取 `event.currentTarget`。事件对象与传播过程见[第十章](10_events_forms.md)。

```js
const saveButton = document.querySelector("#saveButton");

saveButton.addEventListener("click", function (event) {
  console.log(this === event.currentTarget); // true
});
```

这个片段要求 HTML 中存在 `<button id="saveButton" type="button">保存</button>`。这里使用普通函数只是为了观察 `this`；业务代码使用 `event.currentTarget` 更容易表达“触发当前监听器的元素”。

### 7.8 判断 `this` 的顺序

遇到 `this` 时按下面顺序检查：

1. 是箭头函数吗？如果是，去外层作用域寻找 `this`。
2. 是通过 `new` 调用吗？如果是，`this` 是新对象。
3. 使用了 `call()`、`apply()` 或 `bind()` 吗？如果是，查看指定的对象。
4. 是 `对象.方法()` 吗？如果是，`this` 是点号左侧对象。
5. 只是 `函数()` 吗？严格模式下 `this` 是 `undefined`，不要依赖非严格模式下的隐式全局对象。

关键不是背住某个函数“属于谁”，而是找到本次调用时采用了哪一种形式。

## 8. 构造函数、实例与原型

### 8.1 构造函数与实例

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

#### 8.1.1 `new` 的基本过程

1. 创建一个新对象。
2. 让构造函数中的 `this` 指向新对象。
3. 执行构造函数，为新对象设置属性。
4. 构造函数没有显式返回其他对象时，返回这个新对象。

忘记 `new` 时，在现代严格模式或 ES 模块中会因为 `this` 是 `undefined` 而报错。看到首字母大写的构造函数时，应使用 `new` 调用。

### 8.2 `prototype` 与共享方法

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

### 8.3 原型链查找

读取对象属性时，JavaScript 会按顺序查找：

1. 先查找对象自身。
2. 没有时，到对象的原型上查找。
3. 沿着原型继续向上查找。
4. 到达原型链终点仍未找到时，返回 `undefined`。

下面代码追加到 8.2 的第二个完整示例末尾，复用其中的 `Application` 和 `applicationA`：

```js
console.log(Object.hasOwn(applicationA, "id"));         // true
console.log(Object.hasOwn(applicationA, "showStatus")); // false
console.log("showStatus" in applicationA);              // true

console.log(
  Object.getPrototypeOf(applicationA) === Application.prototype
); // true
```

`Object.getPrototypeOf(object)` 接收要检查的对象，返回它的直接原型（可能为 `null`）。本例返回 `Application.prototype`，所以比较结果为 `true`。

`id` 是实例的自有属性；`showStatus` 位于原型上，但实例仍能沿原型链访问它。

旧代码和控制台中可能看到 `__proto__`，新代码不要依赖它修改原型。零基础阶段重点理解“先查自身，再沿原型向上查找”。现代项目也常使用 `class`，构造函数和原型知识有助于阅读旧项目并理解 JavaScript 的对象机制。

## 9. 常见错误与排查

| 现象 | 原因 | 修正方式 |
| --- | --- | --- |
| `user.key` 得不到变量指定的属性 | 点语法把 `key` 当成固定属性名 | 改为 `user[key]` |
| 特殊属性名访问时报错 | 对包含连字符的属性使用了点语法 | 改为 `object["content-type"]` |
| 调用方法没有结果 | 只写了 `user.showName` | 写成 `user.showName()` |
| 方法中的 `this` 不是当前对象 | 使用箭头函数定义了依赖自身 `this` 的方法 | 使用普通方法简写 |
| 取出方法后调用时报错 | `const fn = object.method` 丢失了方法调用者 | 保留 `object.method()` 调用，或使用 `bind()` |
| `call()`、`apply()` 执行过早 | 它们会立即调用函数 | 需要稍后执行时使用 `bind()` 返回新函数 |
| 修改副本时原对象也变化 | 直接赋值只复制了对象引用 | 根据嵌套层级选择浅复制或 `structuredClone()` |
| 两个内容相同的对象比较为 `false` | `===` 比较对象引用 | 按需要比较业务字段 |
| `for...in` 输出继承属性 | 没有限制为自有属性 | 配合 `Object.hasOwn()` |
| 构造函数调用时报错 | 忘记使用 `new` | 使用 `new Constructor(...)` |

## 10. 本章练习

### 10.1 创建和修改设备借用对象

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

### 10.2 方法与 `this`

为设备借用对象添加 `getSummary()` 方法，通过 `this` 返回：

```text
LOAN-20260820-001 测试用笔记本电脑 borrowed
```

分别输出 `equipmentLoan.getSummary` 和 `equipmentLoan.getSummary()`，观察两者区别。

然后继续完成：

1. 把 `getSummary` 赋给变量 `showSummary`，在严格模式下单独调用并观察错误。
2. 使用 `bind(equipmentLoan)` 创建 `boundShowSummary`，确认稍后调用时能够正常返回摘要。
3. 编写普通函数 `showField(fieldName)`，通过 `this[fieldName]` 读取字段，再分别使用 `call()` 和 `apply()` 让它读取 `equipmentLoan` 的 `id`。

练习重点是根据调用形式解释 `this`，不是在每个业务函数中强制使用 `call()`、`apply()` 或 `bind()`。

### 10.3 遍历对象

对下面的对象分别使用 `Object.keys()`、`Object.values()` 和 `Object.entries()`：

```js
const priorityLabels = {
  low: "低",
  normal: "普通",
  high: "高"
};
```

验证属性名数组长度为 `3`，属性值中包含 `"普通"`，并输出每组优先级代码和显示文字。

### 10.4 观察引用

把一个设备对象赋给另一个变量，再通过第二个变量修改保管位置。输出两个变量并解释结果。然后创建两个内容相同的新对象，使用 `===` 比较。

### 10.5 函数参数与复制

使用新的独立设备对象完成：

1. 编写函数接收设备对象并修改其状态，验证调用后原对象发生变化。
2. 改为在函数内给参数赋一个新对象，验证原对象不被替换。
3. 编写创建函数，每次返回一条新设备记录，验证两个结果不是同一个对象。
4. 为设备添加嵌套的保管人数据，分别浅复制和深复制后修改保管人，记录原值是否改变。

再创建含两条设备记录的数组，使用 `find()` 找到一条并修改状态；使用 `filter()` 得到新数组后修改其中记录的属性。分别输出原数组，确认哪些修改共享、哪些不共享。

### 10.6 扩展练习：构造函数和原型

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
- 能定义和调用对象方法，并根据方法调用、单独调用、显式指定、构造调用和箭头函数说明 `this`。
- 能读取嵌套对象，并遍历对象数组。
- 能使用常见方式处理对象的键和值。
- 能区分 `in` 与 `Object.hasOwn()`。
- 能说明对象赋值和严格相等比较的引用特点。
- 能区分函数内修改对象属性与给参数重新赋值。
- 能通过函数返回新对象，并判断对象数组中的记录是否共享引用。
- 能根据修改范围选择直接使用引用、浅复制或深复制。
- 能看懂构造函数、实例、共享原型方法和原型链查找。
