# 第四章 循环与数组基础

## 学习目标

完成本章后，你应能够：

- 使用数组保存多条数据。
- 读取和修改数组项。
- 使用 `for`、`while` 和 `for...of` 执行重复处理。
- 理解数组下标从 0 开始。

## 1. 数组解决什么问题

如果只有一条申请，可以用一个变量：

```js
const applicationId = "REQ-20260820-001";
```

如果有多条申请，就应该使用数组：

```js
const applicationIds = [
  "REQ-20260710-001",
  "REQ-20260801-001",
  "REQ-20260820-001"
];
```

数组用于保存一组有顺序的数据。

## 2. 定义数组

```js
const departments = ["development", "quality", "sales"];
```

数组中的每一项用逗号分隔。

## 3. 读取数组项

数组下标从 0 开始。

```js
const departments = ["development", "quality", "sales"];

console.log(departments[0]); // development
console.log(departments[1]); // quality
console.log(departments[2]); // sales
```

如果访问不存在的下标，会得到 `undefined`。

### 3.1 修改数组项

通过下标重新赋值，可以修改指定位置的元素。

```js
const departments = ["development", "quality", "sales"];

departments[1] = "support";

console.log(departments);
// ["development", "support", "sales"]
```

虽然数组变量使用 `const` 声明，但仍然可以修改数组内部的元素。`const` 禁止的是把变量重新赋值为另一个数组：

```js
departments = ["support"]; // TypeError
```

不要随意给远大于当前长度的下标赋值，否则中间会出现空位：

```js
const values = ["A", "B"];
values[5] = "F";

console.log(values);
// ["A", "B", empty × 3, "F"]
```

## 4. length

`length` 表示数组长度。

```js
const departments = ["development", "quality", "sales"];

console.log(departments.length); // 3
```

最后一项的下标是 `length - 1`。

```js
console.log(departments[departments.length - 1]);
```

## 5. 添加数组项

`push()` 用于在数组末尾添加一项。

```js
const applications = [];

applications.push("REQ-20260820-001");
applications.push("REQ-20260821-001");
```

## 6. for 循环

```js
const statuses = ["申請中", "承認済", "取消済"];

for (let i = 0; i < statuses.length; i += 1) {
  console.log(statuses[i]);
}
```

含义：

- `let i = 0`：从第 0 项开始。
- `i < statuses.length`：没有超过数组长度时继续。
- `i += 1`：每次循环后下标加 1。

## 7. while 循环

`while` 表示：只要指定条件为 `true`，就重复执行代码块。

```js
let count = 1;

while (count <= 3) {
  console.log(`第 ${count} 次处理`);
  count += 1;
}
```

输出结果：

```text
第 1 次处理
第 2 次处理
第 3 次处理
```

这段循环包含三个关键部分：

- `let count = 1`：循环开始前，设置计数变量的初始值。
- `count <= 3`：每次循环前检查条件；条件为 `true` 时继续执行。
- `count += 1`：每次执行后更新计数变量，使循环最终能够结束。

如果第一次检查时条件就是 `false`，循环体一次也不会执行。

```js
let count = 5;

while (count <= 3) {
  console.log(count);
}
```

上面的代码没有输出，因为 `5 <= 3` 一开始就是 `false`。

### 7.1 while 适合什么场景

当重复次数事先不确定，需要根据条件决定是否继续时，`while` 更容易表达。

```js
let remainingDays = 3;

while (remainingDays > 0) {
  console.log(`残り ${remainingDays} 日`);
  remainingDays -= 1;
}
```

如果已经明确知道循环次数，或者需要使用数组下标，通常使用 `for` 更直观；如果只需要依次读取数组中的每一项，通常使用 `for...of` 更直观。

### 7.2 注意死循环

如果循环条件始终为 `true`，循环就不会结束，这称为死循环。

```js
let count = 1;

while (count <= 3) {
  console.log(count);
  // 忘记编写 count += 1，count 会一直是 1
}
```

死循环可能使页面失去响应。编写 `while` 时必须确认：循环体中的某段代码会改变循环条件，使条件最终变成 `false`。

## 8. for...of

如果只需要读取每一项，`for...of` 更清楚。

```js
const statuses = ["申請中", "承認済", "取消済"];

for (const status of statuses) {
  console.log(status);
}
```

项目主线优先使用 `for...of` 或数组方法，只有需要下标时再使用普通 `for`。

## 9. break 和 continue

### 9.1 `break`：提前结束循环

`break` 会立即结束当前循环。

```js
const statuses = ["pending", "approved", "cancelled"];

for (const status of statuses) {
  if (status === "approved") {
    console.log("承認済みを見つけました");
    break;
  }
}
```

找到目标后不需要继续处理剩余元素时，可以使用 `break`。

### 9.2 `continue`：跳过当前一次

`continue` 跳过本次循环后面的代码，直接进入下一次循环。

```js
const statuses = ["pending", "cancelled", "approved"];

for (const status of statuses) {
  if (status === "cancelled") {
    continue;
  }

  console.log(status);
}
```

输出结果不包含 `"cancelled"`。

## 10. do...while 循环

`do...while` 会先执行一次循环体，再检查条件，因此至少执行一次。

```js
let count = 1;

do {
  console.log(count);
  count += 1;
} while (count <= 3);
```

输出 `1`、`2`、`3`。即使条件一开始就是 `false`，循环体也会执行一次：

```js
let count = 5;

do {
  console.log(count); // 5
} while (count <= 3);
```

`do...while` 使用频率低于 `for` 和 `while`，需要能够看懂。编写时注意结尾的 `while (条件);` 后有分号。

## 本章练习

创建一个 `leaveTypes` 数组：

```js
const leaveTypes = ["paid", "half-am", "half-pm", "special"];
```

要求：

1. 输出数组长度。
2. 输出第一项。
3. 使用 `for...of` 输出所有休假类型。
4. 使用 `push()` 添加 `"summer"`，再次输出长度。
5. 使用 `while` 输出数字 `1` 到 `5`，并确认循环能够正常结束。
6. 使用 `for...of` 遍历休假类型，遇到 `"special"` 时使用 `break` 结束循环。

## 本章检查点

- 能定义数组。
- 能用下标读取数组项。
- 能用下标修改数组项。
- 能说明数组下标从 0 开始。
- 能使用 `length` 和 `push()`。
- 能使用 `while`，并说明如何避免死循环。
- 能使用 `for...of` 遍历数组。
- 能说明 `break`、`continue` 和 `do...while` 的作用。
