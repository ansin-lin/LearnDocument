# 第 20 章 ES6+ 常用进阶语法

现代 JavaScript 项目会频繁使用解构、剩余语法、展开语法、可选链和空值合并。TypeScript、Vue 和 React 的组件参数、状态更新和接口数据处理中，也会直接出现这些写法。

完成本章后，你应当能够：

- 使用对象解构完成取值、改名、默认值、嵌套取值和剩余属性收集。
- 使用数组解构按位置取值、跳过元素、设置默认值和收集剩余元素。
- 根据 `...` 所在位置区分剩余语法与展开语法。
- 使用展开语法复制、合并和更新数组或对象。
- 说明直接赋值、浅克隆与深克隆的区别。
- 使用三种可选链和空值合并安全读取数据。
- 区分 `||=`、`&&=` 和 `??=`。
- 使用 `Array.from()` 把类数组或可迭代对象转换为数组。
- 阅读包含 `class`、`constructor`、`extends` 和 `super` 的基础代码。

对象字面量、计算属性名和对象遍历已在[第 6 章](06_objects_data_structure.md)讲解；箭头函数、默认参数和函数剩余参数已在[第 7 章](07_functions_scope_callbacks.md)讲解。本章只在组合使用时进行必要回顾。

## 1. 现代语法主要解决什么问题

下面是一条申请数据：

```js
const application = {
  id: 101,
  applicant: {
    employeeNumber: "EMP-00001",
    name: "田中太郎",
  },
  leaveType: "有給休暇",
  dates: ["2026-09-10", "2026-09-12"],
  status: "pending",
};
```

传统写法可以逐项读取：

```js
const name = application.applicant.name;
const leaveType = application.leaveType;
const status = application.status;
```

现代语法可以更直接地表达几种常见意图：

- 从数据中取出当前需要的部分。
- 在保留旧数据的基础上创建新数组或新对象。
- 数据可能缺失时安全读取。
- 为 `null` 或 `undefined` 提供默认值。

现代语法不是越短越好。一次解构过深、同一行组合太多符号，反而会降低可读性。

## 2. 对象解构

对象解构按照属性名取值。

### 2.1 基本取值

```js
const application = {
  id: 101,
  leaveType: "有給休暇",
  status: "pending",
};

const { id, leaveType } = application;

console.log(id);        // 101
console.log(leaveType); // 有給休暇
```

等价于：

```js
const id = application.id;
const leaveType = application.leaveType;
```

花括号左侧的名字必须与对象属性名对应。

### 2.2 解构时改名

```js
const application = {
  leaveType: "有給休暇",
};

const { leaveType: typeLabel } = application;

console.log(typeLabel); // 有給休暇
```

`leaveType: typeLabel` 表示读取 `leaveType` 属性，并把结果保存到变量 `typeLabel`。这里不会创建名为 `leaveType` 的变量。

### 2.3 解构默认值

```js
const application = {
  leaveType: "有給休暇",
};

const {
  status = "pending",
  note = "没有备注",
} = application;

console.log(status); // pending
console.log(note);   // 没有备注
```

默认值只在属性值为 `undefined` 时生效：

```js
const values = {
  count: 0,
  note: "",
  approved: false,
  reviewer: null,
};

const {
  count = 1,
  note = "未填写",
  approved = true,
  reviewer = "未指定",
} = values;

console.log(count);    // 0
console.log(note);     // ""
console.log(approved); // false
console.log(reviewer); // null
```

`0`、空字符串、`false` 和 `null` 都不会触发解构默认值。

### 2.4 嵌套对象解构

```js
const application = {
  id: 101,
  applicant: {
    employeeNumber: "EMP-00001",
    name: "田中太郎",
  },
};

const {
  applicant: { employeeNumber, name },
} = application;

console.log(employeeNumber); // EMP-00001
console.log(name);           // 田中太郎
```

这个写法创建 `employeeNumber` 和 `name`，不会自动创建 `applicant` 变量。

嵌套属性可能不存在时，可以给中间对象提供默认值：

```js
const application = {};

const {
  applicant: {
    name = "未设置",
  } = {},
} = application;

console.log(name); // 未设置
```

如果数据层级很深，使用第 8 节的可选链通常更容易阅读。

### 2.5 收集剩余属性

```js
const application = {
  id: 101,
  leaveType: "有給休暇",
  status: "pending",
  note: "家庭原因",
};

const { id, ...applicationDetails } = application;

console.log(id); // 101
console.log(applicationDetails);
```

`applicationDetails` 是一个不包含 `id` 的新对象。`...applicationDetails` 在解构左侧负责收集剩余属性，并且必须放在最后。

### 2.6 解构函数参数

```js
function renderApplication({
  id,
  leaveType,
  status = "pending",
} = {}) {
  console.log(`${id}：${leaveType}：${status}`);
}

renderApplication({
  id: 101,
  leaveType: "有給休暇",
});
```

这里有两层默认值：

- `status = "pending"`：对象中没有 `status` 时使用。
- 参数末尾的 `= {}`：没有传入参数或传入 `undefined` 时，先使用空对象，避免解构报错。

如果调用方明确传入 `null`，参数默认值不会生效，仍应根据接口规格处理。

### 2.7 给已经声明的变量解构赋值

```js
let leaveType;
let status;

const application = {
  leaveType: "有給休暇",
  status: "approved",
};

({ leaveType, status } = application);

console.log(leaveType);
console.log(status);
```

外层圆括号不能省略，否则开头的花括号可能被解析成代码块。新代码通常在声明变量时直接解构；这种写法主要用于识读既有代码。

## 3. 数组解构

数组解构按照位置取值，不看变量名称。

### 3.1 基本取值

```js
const dates = ["2026-09-10", "2026-09-12"];
const [startDate, endDate] = dates;

console.log(startDate); // 2026-09-10
console.log(endDate);   // 2026-09-12
```

### 3.2 跳过元素

```js
const statuses = ["pending", "approved", "rejected"];
const [firstStatus, , thirdStatus] = statuses;

console.log(firstStatus); // pending
console.log(thirdStatus); // rejected
```

两个逗号之间留空，表示跳过第二个元素。需要跳过很多位置时，使用下标可能更清楚。

### 3.3 默认值

```js
const dates = ["2026-09-10"];
const [startDate, endDate = startDate] = dates;

console.log(startDate); // 2026-09-10
console.log(endDate);   // 2026-09-10
```

数组解构默认值同样只在对应元素为 `undefined` 时生效。

### 3.4 收集剩余元素

```js
const applications = [
  { id: 101 },
  { id: 102 },
  { id: 103 },
];

const [firstApplication, ...remainingApplications] =
  applications;

console.log(firstApplication);      // { id: 101 }
console.log(remainingApplications); // [{ id: 102 }, { id: 103 }]
```

剩余元素会组成一个新数组，并且必须写在解构的最后。

### 3.5 交换变量

```js
let primaryStatus = "pending";
let secondaryStatus = "approved";

[primaryStatus, secondaryStatus] = [
  secondaryStatus,
  primaryStatus,
];

console.log(primaryStatus);   // approved
console.log(secondaryStatus); // pending
```

这种写法适合简单交换。变量很多时，不要为了缩短代码而使用难以理解的多项交换。

### 3.6 接收函数返回的多个结果

```js
function splitApplications(applications) {
  const pending = applications.filter((item) => {
    return item.status === "pending";
  });

  const completed = applications.filter((item) => {
    return item.status !== "pending";
  });

  return [pending, completed];
}

const [pendingApplications, completedApplications] =
  splitApplications([
    { id: 101, status: "pending" },
    { id: 102, status: "approved" },
  ]);
```

数组返回值依赖位置。结果字段较多或含义可能变化时，返回对象通常更清楚。

## 4. 剩余语法与展开语法

两者都使用 `...`，含义由所在位置决定。

| 所在位置 | 名称 | 数据变化 |
| --- | --- | --- |
| 解构左侧 | 剩余属性或剩余元素 | 多个值被收集为一个对象或数组 |
| 函数参数位置 | 剩余参数 | 多个实参被收集为一个数组 |
| 数组、对象或函数调用内部 | 展开语法 | 一个值的内容被展开到当前位置 |

```js
const values = [1, 2, 3];

const [first, ...rest] = values; // 收集
const copied = [...values];      // 展开

console.log(first);  // 1
console.log(rest);   // [2, 3]
console.log(copied); // [1, 2, 3]
```

可以用一句话判断：

```text
多个内容进入一个变量：剩余
一个值拆开放到当前位置：展开
```

函数剩余参数的完整规则已在第 7 章讲解。

## 5. 数组展开语法

### 5.1 复制和合并数组

```js
const pendingApplications = [{ id: 101 }];
const completedApplications = [{ id: 102 }];

const copiedPending = [...pendingApplications];

const allApplications = [
  ...pendingApplications,
  ...completedApplications,
];

console.log(copiedPending);
console.log(allApplications);
```

展开顺序就是新数组的元素顺序。

### 5.2 新增数组元素

追加到末尾：

```js
const applications = [{ id: 101 }];

const nextApplications = [
  ...applications,
  { id: 102 },
];
```

插入到中间：

```js
const applications = [
  { id: 101 },
  { id: 103 },
];

const nextApplications = [
  applications[0],
  { id: 102 },
  ...applications.slice(1),
];
```

`slice(start)` 从指定下标开始复制元素并返回新数组，不修改原数组。

### 5.3 展开为函数实参

```js
const scores = [72, 91, 85];
const highestScore = Math.max(...scores);

console.log(highestScore); // 91
```

`Math.max(...numbers)` 接收一个或多个数字并返回最大值。展开语法把数组中的三个数字分别放入参数位置。

不能直接写 `Math.max(scores)`，因为这只会传入一个数组。

## 6. 对象展开语法

### 6.1 复制和合并对象

```js
const defaultApplication = {
  status: "pending",
  urgent: false,
};

const inputApplication = {
  leaveType: "有給休暇",
  urgent: true,
};

const application = {
  ...defaultApplication,
  ...inputApplication,
};

console.log(application.urgent); // true
```

对象展开把对象自身可枚举属性复制到新对象中。后展开的同名属性会覆盖先前属性。

### 6.2 在不修改原对象的情况下更新

```js
const application = {
  id: 101,
  leaveType: "有給休暇",
  status: "pending",
};

const approvedApplication = {
  ...application,
  status: "approved",
};

console.log(application.status);         // pending
console.log(approvedApplication.status); // approved
```

覆盖属性必须写在展开语法后面。顺序写反会被旧值覆盖：

```js
const wrongApplication = {
  status: "approved",
  ...application,
};

console.log(wrongApplication.status); // pending
```

这种创建新对象的方式在 React 状态更新和 Vue 数据转换中很常见。

## 7. 直接赋值、浅克隆与深克隆

### 7.1 直接赋值不会复制对象

```js
const original = {
  status: "pending",
};

const copied = original;
copied.status = "approved";

console.log(original.status); // approved
```

两个变量指向同一个对象。

### 7.2 展开语法只进行浅克隆

```js
const original = {
  id: 101,
  applicant: {
    name: "田中太郎",
  },
};

const copied = { ...original };
copied.applicant.name = "鈴木花子";

console.log(original.applicant.name); // 鈴木花子
```

外层对象已经不同，但两者的 `applicant` 仍然指向同一个嵌套对象。数组展开也只复制第一层。

如果只更新某个已知嵌套层级，可以逐层展开：

```js
const copied = {
  ...original,
  applicant: {
    ...original.applicant,
    name: "鈴木花子",
  },
};

console.log(original.applicant.name); // 田中太郎
```

### 7.3 使用 `structuredClone()` 深克隆

```js
const original = {
  id: 101,
  applicant: {
    name: "田中太郎",
  },
  statuses: ["pending", "approved"],
};

const copied = structuredClone(original);
copied.applicant.name = "鈴木花子";
copied.statuses.push("rejected");

console.log(original.applicant.name); // 田中太郎
console.log(original.statuses);       // ["pending", "approved"]
```

`structuredClone(value, options?)` 使用结构化克隆算法创建深层副本：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `value` | 可被结构化克隆的值 | 必填 | 指定要复制的数据 |
| `options` | 包含 `transfer` 等配置的对象 | 可选 | 转移特殊对象的所有权；基础项目通常省略 |

返回值是深层复制后的新值，不修改原数据。它可以处理普通对象、数组、`Date`、`Map`、`Set` 和循环引用等常见数据，但不能克隆函数。

```js
const value = {
  handler: () => {
    console.log("click");
  },
};

structuredClone(value); // DataCloneError
```

选择方式：

| 需求 | 推荐方式 |
| --- | --- |
| 只复制一层数组 | `[...array]` |
| 只复制一层对象 | `{ ...object }` |
| 更新已知嵌套属性 | 对需要修改的层级逐层展开 |
| 深层复制可结构化克隆的数据 | `structuredClone(value)` |
| 类实例、函数等特殊数据 | 编写明确的转换或复制逻辑 |

不要把 `JSON.parse(JSON.stringify(value))` 当作通用深克隆方案。它会丢失某些值，不能正确保留所有 JavaScript 类型，也无法处理循环引用。

## 8. 可选链 `?.`

当左侧可能是 `null` 或 `undefined` 时，可选链会停止后续访问并返回 `undefined`。

### 8.1 可选属性访问

```js
const loginUser = null;
const displayName = loginUser?.profile?.name;

console.log(displayName); // undefined
```

每个可能为空的层级都要写 `?.`。如果 `loginUser` 存在但 `profile` 可能为空，只写 `loginUser?.profile.name` 仍可能报错。

### 8.2 可选下标访问

```js
const applications = [];
const firstType = applications?.[0]?.leaveType;

console.log(firstType); // undefined
```

`value?.[key]` 适合数组下标或动态属性名。

### 8.3 可选函数调用

```js
const options = {};

options.onComplete?.({
  id: 101,
  status: "approved",
});
```

`functionValue?.(arguments)` 只在左侧不是 `null` 或 `undefined` 时尝试调用。左侧存在但不是函数时，仍会抛出 `TypeError`。

### 8.4 可选链的边界

下面是语法错误，不能运行：

```js
const user = null;
// user?.name = "田中"; // SyntaxError：不能放在赋值左侧
```

可选链也不能保护完全未声明的变量：

```js
console.log(notDeclared?.name); // ReferenceError
```

只有变量已经声明，但值可能是 `null` 或 `undefined` 时，才使用可选链。

## 9. 空值合并 `??`

`left ?? right` 只在左侧是 `null` 或 `undefined` 时使用右侧值。

```js
const loginUser = null;
const displayName =
  loginUser?.profile?.name ?? "访客";

console.log(displayName); // 访客
```

### 9.1 `??` 与 `||` 的区别

| 左侧值 | `value || "默认值"` | `value ?? "默认值"` |
| --- | --- | --- |
| `0` | `"默认值"` | `0` |
| `""` | `"默认值"` | `""` |
| `false` | `"默认值"` | `false` |
| `null` | `"默认值"` | `"默认值"` |
| `undefined` | `"默认值"` | `"默认值"` |

`||` 根据真假值选择，`??` 只处理缺少值。数字 `0`、空字符串和 `false` 是合法业务值时，应优先考虑 `??`。

### 9.2 与 `&&`、`||` 混合时加括号

```js
const configuredName = "";
const fallbackName = "访客";

const displayName =
  (configuredName || fallbackName) ?? "未设置";
```

JavaScript 不允许在没有括号时直接把 `??` 与 `&&`、`||` 混写。复杂组合应通过中间变量或括号表达清楚。

## 10. 逻辑赋值运算符

逻辑赋值把判断和赋值写在一起。

### 10.1 `||=`：左侧是假值时赋值

```js
let displayName = "";
displayName ||= "访客";

console.log(displayName); // 访客
```

`||=` 会把 `0`、空字符串、`false`、`null` 和 `undefined` 等假值都视为需要替换。

### 10.2 `??=`：左侧缺少值时赋值

```js
let remainingDays = 0;
remainingDays ??= 10;

console.log(remainingDays); // 0
```

`??=` 只在左侧是 `null` 或 `undefined` 时赋值，适合保留合法的假值。

### 10.3 `&&=`：左侧是真值时赋值

```js
let canApprove = true;
const hasManagerRole = false;

canApprove &&= hasManagerRole;

console.log(canApprove); // false
```

`&&=` 只在左侧为真值时计算并赋予右侧结果。条件复杂时，普通 `if` 往往更清楚。

| 写法 | 判断条件 | 常见用途 |
| --- | --- | --- |
| `value ||= fallback` | 左侧是假值 | 不需要保留 `0`、`""`、`false` 的默认值 |
| `value ??= fallback` | 左侧是 `null` 或 `undefined` | 保留合法的假值 |
| `value &&= next` | 左侧是真值 | 简单的条件更新 |

## 11. 使用 `Array.from()` 转换为数组

`Array.from(source, mapFunction?, thisArg?)` 从可迭代对象或类数组对象创建新数组。

### 11.1 把 NodeList 转成数组

配套 HTML：

```html
<ul>
  <li class="application-item">交通费申请</li>
  <li class="application-item">休假申请</li>
</ul>
```

页面脚本：

```js
const nodeList =
  document.querySelectorAll(".application-item");
const items = Array.from(nodeList);

const labels = items.map((item) => {
  return item.textContent;
});

console.log(labels);
```

`querySelectorAll()` 返回 NodeList，不是数组。`Array.from(nodeList)` 返回真正的数组，之后可以使用数组方法。

### 11.2 转换时同时处理元素

```js
const scores = Array.from(
  ["72", "91", "85"],
  (scoreText) => {
    return Number(scoreText);
  },
);

console.log(scores); // [72, 91, 85]
```

`Array.from()` 参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `source` | 可迭代对象或具有 `length` 的类数组对象 | 必填 | 指定转换来源 |
| `mapFunction` | 转换函数 | 可选 | 在放入新数组前处理每个元素 |
| `thisArg` | 任意值 | 可选 | 指定转换函数中的 `this`；箭头函数不使用该绑定 |

### 11.3 与展开语法的选择

```js
const text = "ABC";

console.log([...text]);        // ["A", "B", "C"]
console.log(Array.from(text)); // ["A", "B", "C"]
```

- 只想简短地把可迭代对象展开成数组，可以使用 `[...source]`。
- 来源是类数组对象，或者想通过第二个参数同时转换元素，使用 `Array.from()`。

Set 和 Map 的系统讲解放在第 21 章。

## 12. Class 基础识读

JavaScript 的 `class` 提供了创建同类对象的清晰语法。它的底层仍建立在原型机制上，不等同于 Java 的类系统。基础阶段重点是能阅读和编写简单类，不要求设计复杂继承体系。

### 12.1 定义类并创建实例

```js
class LeaveApplication {
  constructor({
    id,
    employeeNumber,
    leaveType,
    status = "pending",
  }) {
    this.id = id;
    this.employeeNumber = employeeNumber;
    this.leaveType = leaveType;
    this.status = status;
  }

  approve() {
    this.status = "approved";
  }

  getSummary() {
    return `${this.id}：${this.leaveType}：${this.status}`;
  }
}

const application = new LeaveApplication({
  id: 101,
  employeeNumber: "EMP-00001",
  leaveType: "有給休暇",
});

application.approve();
console.log(application.getSummary());
```

各部分作用：

| 写法 | 作用 |
| --- | --- |
| `class LeaveApplication` | 声明名为 `LeaveApplication` 的类 |
| `constructor(...)` | 使用 `new` 创建实例时执行初始化 |
| `this.id = id` | 把数据保存为当前实例的属性 |
| `approve()` | 定义所有实例共享的原型方法 |
| `new LeaveApplication(...)` | 创建并初始化一个实例 |

类中的方法使用方法简写，不需要写 `function`。调用实例方法时，`this` 指向调用该方法的实例。

### 12.2 静态方法

```js
class LeaveApplication {
  static isPending(application) {
    return application.status === "pending";
  }
}

const application = {
  id: 101,
  status: "pending",
};

console.log(
  LeaveApplication.isPending(application),
); // true
```

`static` 方法属于类本身，通过 `LeaveApplication.isPending()` 调用，不通过实例调用。适合不依赖某个实例状态的辅助判断。

### 12.3 使用 `extends` 和 `super`

```js
class LeaveApplication {
  constructor({ id, employeeNumber, status = "pending" }) {
    this.id = id;
    this.employeeNumber = employeeNumber;
    this.status = status;
  }

  getSummary() {
    return `${this.id}：${this.status}`;
  }
}

class PaidLeaveApplication extends LeaveApplication {
  constructor({
    id,
    employeeNumber,
    startDate,
    endDate,
  }) {
    super({
      id,
      employeeNumber,
    });

    this.startDate = startDate;
    this.endDate = endDate;
  }

  getSummary() {
    const baseSummary = super.getSummary();
    return `${baseSummary}：${this.startDate}～${this.endDate}`;
  }
}
```

- `extends LeaveApplication` 表示子类继承父类。
- 子类构造函数必须在访问 `this` 前调用 `super(...)`，完成父类初始化。
- `super.getSummary()` 调用父类的同名方法。
- 子类重新定义 `getSummary()`，称为覆盖父类方法。

继承适合确实存在稳定“属于一种”的关系。业务数据只需要存取和转换时，普通对象和函数通常更简单。

### 12.4 Class 的常见边界

- 类声明不会像函数声明一样在声明前安全使用，应先声明再创建实例。
- 调用类必须使用 `new`。
- 原型方法与箭头函数字段不是同一种成员，不要在不了解 `this` 和原型差异时互相替换。
- 从实例中单独取出方法再调用，可能丢失原本的 `this`。
- Class 实例包含行为和原型关系，不要认为对象展开或 `structuredClone()` 后仍会自动保留完整类行为。

## 13. 常见错误与排查

### 13.1 解构不存在的嵌套对象

```js
const application = {};
const {
  applicant: { name },
} = application; // TypeError
```

原因是代码尝试从 `undefined` 中继续解构 `name`。可以给中间层默认对象，或使用 `application.applicant?.name`。

### 13.2 误以为默认值会替换 `null`

```js
const { reviewer = "未指定" } = {
  reviewer: null,
};

console.log(reviewer); // null
```

需要同时处理 `null` 时，可以在解构后使用 `reviewer ?? "未指定"`。

### 13.3 对象展开顺序写反

更新值写在展开前面时，后展开的旧属性会覆盖新值。通过控制台输出完整对象，检查同名属性的最终值。

### 13.4 把浅克隆当成深克隆

修改副本的嵌套对象时原数据也变化，应检查嵌套层是否仍共享引用。根据需求逐层展开或使用 `structuredClone()`。

### 13.5 用 `||` 覆盖合法假值

如果 `0`、空字符串或 `false` 是有效业务数据，应改用 `??` 或 `??=`。

### 13.6 在可选链左侧使用未声明变量

可选链只处理值为 `null` 或 `undefined`，不能处理变量根本没有声明。

### 13.7 忘记使用 `new` 调用类

下面是错误示例：

```js
const application = LeaveApplication({}); // TypeError
```

类需要写成 `new LeaveApplication({...})`。

## 14. 本章综合练习

### 14.1 初始数据

```js
const applications = [
  {
    id: 101,
    applicant: {
      employeeNumber: "EMP-00001",
      name: "田中太郎",
    },
    leaveType: "有給休暇",
    dates: ["2026-09-10", "2026-09-12"],
    status: "pending",
    note: "",
  },
  {
    id: 102,
    applicant: {
      employeeNumber: "EMP-00002",
      name: "鈴木花子",
    },
    leaveType: "午前休",
    dates: ["2026-09-15"],
    status: "approved",
  },
];
```

### 14.2 任务要求

1. 从第一条申请中解构出 `id`、申请人姓名、请假类型和状态。
2. 把申请人姓名改名为变量 `applicantName`。
3. 使用数组解构取得开始日期，并让缺少的结束日期默认等于开始日期。
4. 使用剩余属性收集除 `id` 以外的申请数据。
5. 使用数组展开新增第三条申请，不能修改原数组。
6. 使用对象展开把编号 101 的状态更新为 `approved`，不能修改原对象。
7. 建立嵌套副本，验证浅克隆和逐层展开的区别。
8. 使用 `?.` 和 `??` 读取可能不存在的审核人姓名，默认显示“未指定”。
9. 分别使用 `||=` 和 `??=` 处理空字符串，记录结果差异。
10. 使用 `Array.from()` 把页面中的申请列表 NodeList 转换为数组。
11. 编写一个最小 `LeaveApplication` 类，包含构造函数、`approve()` 和 `getSummary()`。
12. 故意去掉 `new` 调用类，观察错误后修正。

### 14.3 验证标准

- 解构得到的变量和值与原对象对应。
- 能说明对象解构按属性名、数组解构按位置。
- 能根据 `...` 的位置判断它是剩余还是展开。
- 新数组和新对象不会直接修改原数据。
- 能通过修改嵌套属性证明浅克隆仍可能共享引用。
- `0`、空字符串和 `false` 不会被错误默认值覆盖。
- 可选链覆盖属性、下标或函数可能不存在的情况。
- `Array.from()` 返回真正的数组。
- Class 示例可以创建实例并调用方法。
- 控制台没有未处理错误。
