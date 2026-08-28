# 第七章 函数、作用域、闭包与递归

## 学习目标

完成本章后，你应能够：

- 使用函数声明、函数表达式和箭头函数定义函数。
- 使用形参、实参、默认参数、剩余参数和返回值。
- 说明参数少传、多传时的结果，并检查实际参数个数。
- 区分函数声明与函数表达式的提升表现。
- 理解局部作用域、外层作用域和词法作用域。
- 定义和使用回调函数。
- 看懂立即执行函数 IIFE。
- 说明闭包为什么能够保留外层变量。
- 编写包含终止条件的简单递归函数。
- 看懂使用递归克隆普通数组和对象的基本思路。

## 掌握要求

- **必须掌握**：函数声明、参数、返回值、作用域、箭头函数和回调函数。
- **需要掌握**：默认参数、剩余参数、参数个数、函数表达式和函数提升。
- **会使用、能看懂**：闭包和简单递归。
- **了解即可**：`arguments`、IIFE 和递归克隆实现；实际项目优先采用更清楚的现代写法。

## 1. 函数解决什么问题

函数用于封装一段可以重复执行的代码。

```js
function showMessage() {
  console.log("処理が完了しました");
}

showMessage();
showMessage();
```

- `function` 表示定义函数。
- `showMessage` 是函数名。
- `{}` 中是函数体。
- `showMessage()` 表示调用函数。

定义函数不会自动执行函数体。只有调用函数时，函数体中的代码才会执行。

## 2. 函数的三种常见定义方式

### 2.1 函数声明

```js
function calculateRemainingDays(totalDays, usedDays) {
  return totalDays - usedDays;
}
```

函数声明使用 `function 函数名()` 的形式，适合定义项目中的主要业务函数。

### 2.2 函数表达式

函数也可以作为值保存到变量中：

```js
const calculateRemainingDays = function (totalDays, usedDays) {
  return totalDays - usedDays;
};
```

右侧没有名字的函数称为匿名函数。整个函数被赋给 `calculateRemainingDays` 变量，之后通过变量名调用。

```js
const remainingDays = calculateRemainingDays(12, 2);
```

### 2.3 箭头函数

箭头函数是现代 JavaScript 中常见的函数表达式写法：

```js
const calculateRemainingDays = (totalDays, usedDays) => {
  return totalDays - usedDays;
};
```

箭头函数经常用于数组方法、事件处理和短小的工具函数。它与普通函数在 `this`、`arguments` 和构造函数能力方面存在差异，本章先掌握常用写法；复杂 `this` 后续了解。

### 2.4 三种定义方式的选择

| 定义方式 | 常见用途 | 是否有自己的 `arguments` | 能否在定义语句之前调用 |
| --- | --- | --- | --- |
| 函数声明 | 主要业务函数、通用工具函数 | 有 | 可以 |
| 函数表达式 | 把函数保存到变量或作为值传递 | 有 | 使用 `const` / `let` 时不可以 |
| 箭头函数 | 回调函数、短小转换函数 | 没有 | 使用 `const` / `let` 时不可以 |

## 3. 参数与返回值

### 3.1 形参与实参

定义函数时写在括号中的变量称为形参，调用函数时传入的具体值称为实参。

```js
function showUserName(name) {
  console.log(`${name}さん`);
}

showUserName("山田 太郎");
```

- `name` 是形参。
- `"山田 太郎"` 是实参。

多个参数按照位置对应：

```js
function calculateRemainingDays(totalDays, usedDays) {
  return totalDays - usedDays;
}

const result = calculateRemainingDays(12, 2);
console.log(result); // 10
```

### 3.2 `return` 返回结果

`return` 把函数的处理结果交给调用位置，同时立即结束当前函数。

```js
function validateDays(days) {
  if (days <= 0) {
    return false;
  }

  return true;
}
```

函数没有执行到 `return` 时，返回值是 `undefined`。

```js
function showMessage() {
  console.log("完了しました");
}

const result = showMessage();
console.log(result); // undefined
```

## 4. 参数个数与默认值

JavaScript 调用函数时，实参数量不要求与形参数量完全相同。

### 4.1 少传参数

没有收到实参的形参值为 `undefined`。

```js
function showUser(accountId, name) {
  console.log(accountId); // yamada
  console.log(name);      // undefined
}

showUser("yamada");
```

如果缺少的参数参与计算，可能得到意外结果：

```js
function add(a, b) {
  return a + b;
}

console.log(add(10)); // NaN
```

### 4.2 多传参数

多出的实参不会自动报错。普通形参只接收对应位置的值：

```js
function showUserName(name) {
  console.log(name);
}

showUserName("山田 太郎", "development");
// 山田 太郎
```

JavaScript 不会像 Java 那样根据参数个数自动选择同名重载函数。项目中应通过明确的函数名、默认参数或对象参数表达不同用途。

### 4.3 默认参数

默认参数在实参为 `undefined` 或没有传入时生效。

```js
function formatUserName(name, suffix = "さん") {
  return `${name}${suffix}`;
}

console.log(formatUserName("山田"));       // 山田さん
console.log(formatUserName("山田", "様")); // 山田様
```

默认参数通常放在必填参数之后。

### 4.4 剩余参数

剩余参数使用 `...` 收集多出的实参，得到一个真正的数组。

```js
function calculateTotal(...daysList) {
  let total = 0;

  for (const days of daysList) {
    total += days;
  }

  return total;
}

console.log(calculateTotal(1, 2, 3)); // 6
```

剩余参数必须写在参数列表最后，并且一个函数只能有一个剩余参数。

```js
function saveApplication(userId, ...applicationIds) {
  console.log(userId);
  console.log(applicationIds);
}
```

### 4.5 `arguments`

普通函数内部可以使用 `arguments` 取得本次调用收到的全部实参。

```js
function showArguments() {
  console.log(arguments.length); // 3
  console.log(arguments[0]);     // A
  console.log(arguments[1]);     // B
}

showArguments("A", "B", "C");
```

`arguments` 是类数组对象：可以使用下标和 `length`，但不能直接使用全部数组方法。新代码需要收集不定数量参数时，优先使用剩余参数，因为剩余参数得到真正的数组，含义也更明确。

箭头函数没有自己的 `arguments`：

```js
const showArguments = (...values) => {
  console.log(values);
};
```

### 4.6 两种“参数个数”

```js
function createUser(accountId, name, department) {
  console.log(arguments.length);
}

console.log(createUser.length); // 3
createUser("yamada", "山田");   // arguments.length 是 2
```

- `函数名.length`：函数定义中，默认参数之前声明了多少个形参。
- `arguments.length`：本次调用实际传入了多少个实参。

```js
function createUser(accountId, name = "未设置", department) {
}

console.log(createUser.length); // 1
```

因为第一个默认参数是 `name`，`createUser.length` 只统计它之前的 `accountId`。

## 5. 箭头函数的常用写法

### 5.1 参数写法

没有参数时必须写空括号：

```js
const showComplete = () => {
  console.log("完了しました");
};
```

只有一个简单参数时可以省略括号：

```js
const showUserName = name => {
  console.log(name);
};
```

有两个或更多参数时必须写括号：

```js
const add = (a, b) => {
  return a + b;
};
```

### 5.2 隐式返回

函数体只有一个返回表达式时，可以省略花括号和 `return`：

```js
const add = (a, b) => a + b;
```

使用花括号时必须明确写 `return`：

```js
const add = (a, b) => {
  return a + b;
};
```

返回对象字面量时，需要使用括号包住对象：

```js
const createUser = name => ({
  name: name,
  active: true
});
```

## 6. 函数提升

### 6.1 函数声明可以提前调用

函数声明会在执行当前作用域代码前完成初始化，因此可以写在调用语句之后。

```js
showMessage(); // 正常执行

function showMessage() {
  console.log("完了しました");
}
```

这称为函数提升。虽然语法允许，但为了阅读顺序清楚，项目中仍建议先定义主要函数，再在入口位置调用。

### 6.2 函数表达式不能在定义前调用

```js
showMessage(); // ReferenceError

const showMessage = function () {
  console.log("完了しました");
};
```

这里提升的是 `showMessage` 变量的声明规则，而不是让右侧函数提前可用。箭头函数保存到 `const` 或 `let` 时也一样。

```js
showMessage(); // ReferenceError

const showMessage = () => {
  console.log("完了しました");
};
```

## 7. 作用域与词法作用域

函数内部声明的变量只能在函数内部访问。

```js
function createMessage() {
  const message = "完了しました";
  return message;
}

console.log(createMessage());
console.log(message); // ReferenceError
```

函数可以访问自己内部的变量，也可以访问定义位置外层的变量：

```js
const systemName = "有給休暇申請システム";

function showSystemName() {
  console.log(systemName);
}

showSystemName();
```

函数能访问哪些变量，由函数写在代码中的位置决定，这称为词法作用域。

不要把所有数据都放到全局作用域。全局变量过多会让不同函数互相影响，增加排错难度。

## 8. 回调函数

回调函数是作为参数传给另一个函数，并由接收方在适当时机调用的函数。

```js
function runTask(task) {
  task();
}

function showComplete() {
  console.log("処理が完了しました");
}

runTask(showComplete);
```

`runTask(showComplete)` 没有写 `showComplete()`，因为这里传递的是函数本身。

也可以直接传入箭头函数：

```js
runTask(() => {
  console.log("処理が完了しました");
});
```

数组方法和事件处理中会大量使用回调函数：

```js
const applications = [
  { id: "REQ-001", status: "pending" },
  { id: "REQ-002", status: "approved" }
];

const pendingApplications = applications.filter(
  app => app.status === "pending"
);

console.log(pendingApplications);
```

`filter()` 会逐项调用回调函数，把判断结果为真值的元素组成新数组；这里得到所有状态为 `"pending"` 的申请。第八章会详细讲解它的参数和返回值。

HTML：

```html
<button id="callbackButton" type="button">执行</button>
```

JavaScript：

```js
const button = document.getElementById("callbackButton");

button.addEventListener("click", () => {
  console.log("クリックされました");
});
```

`addEventListener()` 用于给元素添加事件监听器；这里把箭头函数作为回调传入，按钮被点击时浏览器会调用它。第十一章会系统讲解事件监听。

## 9. 立即执行函数 IIFE

IIFE 是 Immediately Invoked Function Expression 的缩写，中文称为立即调用函数表达式。函数定义完成后会立刻执行一次。

```js
(function () {
  console.log("立即执行");
})();
```

执行过程：

1. `(function () { ... })` 把函数声明形式转换为函数表达式。
2. 最后的 `()` 立即调用这个函数。

箭头函数也可以写成 IIFE：

```js
(() => {
  console.log("箭头函数立即执行");
})();
```

### 9.1 IIFE 的作用

在 ES 模块普及之前，IIFE 常用于创建独立作用域，避免变量污染全局。

```js
(function () {
  const internalStatus = "pending";
  console.log(internalStatus);
})();

console.log(internalStatus); // ReferenceError
```

IIFE 也可以计算并返回结果：

```js
const message = (() => {
  const userName = "山田";
  return `${userName}さん、ログインしました`;
})();

console.log(message);
```

现代项目通常优先使用 ES 模块和普通函数。IIFE 需要能够阅读和维护，不要求在所有新代码中主动使用。

## 10. 闭包

闭包是函数与其定义时所在词法环境的组合。即使外层函数已经执行结束，内部函数仍然可以访问当时的外层变量。

### 10.1 观察闭包

```js
function createCounter() {
  let count = 0;

  return function () {
    count += 1;
    return count;
  };
}

const counter = createCounter();

console.log(counter()); // 1
console.log(counter()); // 2
console.log(counter()); // 3
```

执行过程：

1. 调用 `createCounter()`，创建局部变量 `count`。
2. 外层函数返回一个内部函数。
3. `counter` 保存这个内部函数。
4. 内部函数仍能访问并修改它定义时外层的 `count`。

这就是闭包表现出的“记忆”能力。

### 10.2 每个闭包相互独立

```js
const counterA = createCounter();
const counterB = createCounter();

console.log(counterA()); // 1
console.log(counterA()); // 2
console.log(counterB()); // 1
```

每次调用 `createCounter()` 都会创建新的 `count`，两个计数器互不影响。

### 10.3 闭包的常见用途

- 保存函数多次调用之间的状态。
- 创建只允许通过指定函数修改的数据。
- 生成带有固定配置的函数。
- 在事件处理和异步处理中保留外层数据。

```js
function createStatusChecker(expectedStatus) {
  return application => application.status === expectedStatus;
}

const isPending = createStatusChecker("pending");

console.log(isPending({ status: "pending" }));  // true
console.log(isPending({ status: "approved" })); // false
```

### 10.4 闭包的注意点

闭包会让它仍然使用的外层数据继续保留。如果长期保存不再需要的事件监听器、定时器或大型对象，可能增加内存占用。实际项目中应在组件销毁或页面功能结束时清理不再使用的监听器和定时器。

## 11. 递归

递归是函数在内部调用自身，把大问题逐步缩小为同类型的小问题。

递归函数必须包含：

- 终止条件：什么时候停止继续调用。
- 递归步骤：如何把问题缩小后再次调用自身。

### 11.1 倒计时示例

```js
function countdown(number) {
  if (number <= 0) {
    console.log("结束");
    return;
  }

  console.log(number);
  countdown(number - 1);
}

countdown(3);
```

输出：

```text
3
2
1
结束
```

### 11.2 递归返回结果

计算 `1` 到指定数字之和：

```js
function sumTo(number) {
  if (number <= 1) {
    return number;
  }

  return number + sumTo(number - 1);
}

console.log(sumTo(4)); // 10
```

调用关系可以理解为：

```text
sumTo(4)
= 4 + sumTo(3)
= 4 + 3 + sumTo(2)
= 4 + 3 + 2 + sumTo(1)
= 4 + 3 + 2 + 1
```

### 11.3 忘记终止条件

如果递归一直调用自身，就会产生调用栈溢出。

```js
function repeat() {
  repeat();
}

repeat(); // RangeError: Maximum call stack size exceeded
```

处理普通列表时优先使用循环和数组方法。递归更适合树形结构、嵌套数据和天然可以逐层缩小的问题。

## 12. 使用递归理解深克隆

直接赋值不会克隆对象：

```js
const original = {
  user: {
    name: "山田"
  }
};

const copied = original;
copied.user.name = "鈴木";

console.log(original.user.name); // 鈴木
```

`original` 和 `copied` 指向同一个对象。

下面的函数通过递归克隆普通数组和普通对象：

```js
function clonePlainData(value) {
  if (Array.isArray(value)) {
    const result = [];

    for (const item of value) {
      result.push(clonePlainData(item));
    }

    return result;
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

对象分支中的 `for...in` 用于依次取得对象的可枚举属性名。`Object.hasOwn(value, key)` 用于确认该属性直接属于当前对象，避免复制从原型链继承的属性。

执行思路：

1. 遇到数组，创建新数组，并递归克隆每一项。
2. 遇到普通对象，创建新对象，并递归克隆每个自有属性。
3. 遇到字符串、数字、布尔值、`null` 等值，直接返回。

```js
const original = {
  user: {
    name: "山田"
  },
  statuses: ["pending", "approved"]
};

const copied = clonePlainData(original);
copied.user.name = "鈴木";

console.log(original.user.name); // 山田
console.log(copied.user.name);   // 鈴木
```

这个函数只用于理解递归和深克隆原理，不是通用克隆库。它没有处理循环引用、`Date`、`Map`、`Set`、函数、DOM 节点和对象原型等特殊情况。实际项目中的浅克隆、深克隆和 `structuredClone()` 会在第二十章说明。

## 13. 函数职责要单一

一个函数应集中完成一个清楚的任务。

不推荐：

```js
function submitApplication() {
  // 校验、保存、跳转、渲染全部写在一起
}
```

更推荐拆分：

```js
function validateApplication(formData) {
  // 校验
}

function saveApplication(application) {
  // 保存
}

function goToCompletePage() {
  location.href = "complete.html";
}
```

## 14. 本章练习

### 练习 1：参数和返回值

编写 `formatEmployeeLabel(user, suffix)`：

- `user` 接收员工对象。
- `suffix` 默认值为 `"さん"`。
- 返回 `"EMP-00001 山田 太郎さん"` 形式的字符串。

### 练习 2：参数个数

编写普通函数 `inspectArguments()`，分别使用零个、一个和三个实参调用，输出每次调用的 `arguments.length`。

### 练习 3：函数提升

分别观察下面两种代码：

1. 在函数声明之前调用函数。
2. 在使用 `const` 保存的函数表达式之前调用函数。

记录结果，并把函数表达式的调用移动到定义之后恢复正常。

### 练习 4：闭包

编写 `createRequestNumberGenerator(prefix, start)`，返回一个每次调用都生成下一个编号的函数。例如连续调用后得到 `REQ-101`、`REQ-102`。分别创建 `REQ` 和 `TASK` 两个生成器，确认它们的编号状态互不影响。

### 练习 5：递归

编写 `multiplyTo(number)`，使用递归计算从 `1` 到 `number` 的乘积。验证 `multiplyTo(5)` 的结果是 `120`，并处理 `number <= 1` 的终止条件。

### 练习 6：IIFE

编写一个 IIFE，在内部组合系统名称和当前环境，输出 `"休假管理系统 - training"`。然后确认 IIFE 内部声明的 `environment` 无法在外部访问。

## 本章检查点

- 能定义和调用普通函数、函数表达式和箭头函数。
- 能区分形参、实参、默认参数和剩余参数。
- 能说明 `函数名.length` 与 `arguments.length` 的区别。
- 能说明函数声明和函数表达式的提升差异。
- 能传递并调用回调函数。
- 能看懂 IIFE，并说明它创建独立作用域的作用。
- 能使用闭包保存简单状态。
- 能为递归函数设置终止条件。
- 能说明递归克隆示例的适用范围和限制。
