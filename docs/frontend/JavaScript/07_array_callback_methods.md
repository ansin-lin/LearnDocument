# 第七章 数组回调方法

## 学习目标

完成本章后，你应能够：

- 说明数组方法中的回调函数如何接收当前元素、下标和原数组。
- 使用 `forEach()` 依次处理数组元素。
- 使用 `find()` 和 `findIndex()` 查找对象数组。
- 使用 `filter()` 筛选数据，使用 `map()` 转换数据。
- 使用 `some()` 和 `every()` 判断数组是否满足条件。
- 使用比较函数完成数字和对象数组排序。
- 看懂常见的链式数组处理。

## 1. 数组为什么需要回调函数

第五章的方法已经知道要添加、删除或查找哪个具体值，因此可以直接传入数据：

```js
statuses.includes("pending");
```

对象数组的判断规则通常更复杂。例如，要查找 `id` 为 `"REQ-002"` 的申请，数组方法需要知道“应该比较每个对象的哪个属性”。这个判断规则通过回调函数传入。

```js
const target = applications.find(app => app.id === "REQ-002");
```

`find()` 会依次检查数组元素，并返回第一个使回调函数结果为真值的元素；找不到时返回 `undefined`。

这里的回调函数是：

```js
app => app.id === "REQ-002"
```

- `app` 是当前正在处理的数组元素。
- `app.id === "REQ-002"` 是判断规则。
- 每处理一个元素，数组方法就会调用一次这个函数。

## 2. 回调函数可以接收哪些参数

常见数组回调方法会依次向回调函数传入三个参数：

```js
(currentValue, index, array) => {
  // 处理规则
}
```

| 参数 | 可接受的值 | 是否必写 | 作用 |
| --- | --- | --- | --- |
| `currentValue` | 当前数组元素 | 按需声明 | 取得当前正在处理的元素 |
| `index` | 从 `0` 开始的整数下标 | 可省略 | 取得当前元素的位置 |
| `array` | 调用当前方法的原数组 | 可省略 | 访问原数组 |

参数名可以自行命名，但顺序不能改变。只需要当前元素时，写一个参数即可：

```js
applications.forEach(app => {
  console.log(app.id);
});
```

`forEach()` 会按照数组顺序，对每个元素执行一次回调函数。

同时需要元素和下标时：

```js
applications.forEach((app, index) => {
  console.log(`${index}: ${app.id}`);
});
```

本章统一使用下面的对象数组：

```js
const applications = [
  { id: "REQ-001", status: "pending", days: 1 },
  { id: "REQ-002", status: "approved", days: 2 },
  { id: "REQ-003", status: "pending", days: 3 }
];
```

## 3. `forEach()`：依次处理每个元素

`forEach()` 对数组中的每个元素执行一次回调函数。

```js
applications.forEach(app => {
  console.log(`${app.id}: ${app.status}`);
});
```

- 参数：回调函数。
- 返回值：`undefined`。
- 是否修改原数组：方法本身不会自动修改；是否发生修改取决于回调中的代码。

`forEach()` 适合输出内容、记录日志或对每一项执行操作，不适合生成新的结果数组。

`forEach()` 不能使用 `break` 提前结束。需要提前结束循环时，使用 `for` 或 `for...of`。

## 4. `find()`：查找一个元素

`find()` 返回第一个使回调函数结果为真值的元素，找不到时返回 `undefined`。

```js
const target = applications.find(app => app.id === "REQ-002");

console.log(target);
// { id: "REQ-002", status: "approved", days: 2 }
```

- 参数：返回真值或假值的回调函数。
- 返回值：第一个满足条件的元素；找不到返回 `undefined`。
- 是否修改原数组：否。

`find()` 适合根据员工编号、申请编号等唯一条件查找一条数据。

## 5. `findIndex()`：查找元素下标

`findIndex()` 返回第一个满足条件的元素下标，找不到时返回 `-1`。

```js
const targetIndex = applications.findIndex(app => app.id === "REQ-002");

console.log(targetIndex); // 1
```

- 参数：返回真值或假值的回调函数。
- 返回值：第一个匹配下标；找不到返回 `-1`。
- 是否修改原数组：否。

需要配合 `splice()` 删除目标对象时，可以先查找下标：

```js
const targetIndex = applications.findIndex(app => app.id === "REQ-002");

if (targetIndex !== -1) {
  applications.splice(targetIndex, 1);
}
```

## 6. `filter()`：筛选多个元素

`filter()` 把所有使回调函数结果为真值的元素放入一个新数组。

```js
const pendingApplications = applications.filter(
  app => app.status === "pending"
);

console.log(pendingApplications);
```

- 参数：返回真值或假值的回调函数。
- 返回值：包含全部匹配元素的新数组；没有匹配项时返回空数组 `[]`。
- 是否修改原数组：否。

`find()` 只返回第一项，`filter()` 返回所有匹配项。

## 7. `map()`：转换每个元素

`map()` 对每个元素执行转换，并把每次返回的结果组成新数组。新数组长度与原数组相同。

```js
const applicationIds = applications.map(app => app.id);

console.log(applicationIds);
// ["REQ-001", "REQ-002", "REQ-003"]
```

- 参数：负责转换元素的回调函数。
- 返回值：由每次回调结果组成的新数组。
- 是否修改原数组：否。

箭头函数使用花括号时，需要明确写 `return`：

```js
const labels = applications.map(app => {
  return `${app.id}: ${app.days}日`;
});
```

如果忘记 `return`，新数组中的对应位置会得到 `undefined`。

## 8. `some()`：是否至少有一项满足条件

`some()` 只要找到一个满足条件的元素，就返回 `true`。

```js
const hasPending = applications.some(app => app.status === "pending");

console.log(hasPending); // true
```

- 参数：返回真值或假值的回调函数。
- 返回值：至少一项满足时为 `true`，否则为 `false`。
- 是否修改原数组：否。

## 9. `every()`：是否全部满足条件

`every()` 只有在所有元素都满足条件时才返回 `true`。

```js
const allDaysValid = applications.every(app => app.days > 0);

console.log(allDaysValid); // true
```

- 参数：返回真值或假值的回调函数。
- 返回值：全部满足时为 `true`，只要一项不满足就是 `false`。
- 是否修改原数组：否。

可以这样记忆：`some()` 问“有没有”，`every()` 问“是不是全部”。

## 10. `sort()`：使用比较函数排序

### 10.1 默认排序按字符串处理

```js
const numbers = [3, 20, 100];

numbers.sort();

console.log(numbers); // [100, 20, 3]
```

省略比较函数时，`sort()` 会把元素转换成字符串进行比较，所以不能直接用于数字大小排序。

### 10.2 数字排序

```js
const numbers = [3, 20, 100];

numbers.sort((a, b) => a - b);

console.log(numbers); // [3, 20, 100]
```

比较函数返回值的含义：

- 负数：`a` 排在 `b` 前面。
- 正数：`b` 排在 `a` 前面。
- `0`：保持两者原有的先后顺序。

从大到小排序时写成：

```js
numbers.sort((a, b) => b - a);
```

### 10.3 对象数组排序

```js
const sortedApplications = applications
  .slice()
  .sort((a, b) => a.days - b.days);
```

`sort()` 会修改调用它的数组。这里先使用 `slice()` 复制，再排序副本，因此原数组不变。

## 11. 链式调用

返回数组的方法可以继续调用其他数组方法，这种连续写法称为链式调用。

```js
const pendingIds = applications
  .filter(app => app.status === "pending")
  .map(app => app.id);

console.log(pendingIds); // ["REQ-001", "REQ-003"]
```

执行顺序是：

1. `filter()` 筛选出申请中的数据。
2. `map()` 从筛选结果中取出编号。
3. 最终结果赋给 `pendingIds`。

链式调用不要写得过长。处理步骤复杂时，可以拆成多个有意义的变量。

## 12. `reduce()`：累计得到一个结果

`reduce()` 将数组中的所有元素逐步合并成一个结果。它常用于求和和分组，但比其他数组方法更难阅读，本课程要求能够完成简单求和并看懂常见代码。

```js
const totalDays = applications.reduce((total, app) => {
  return total + app.days;
}, 0);

console.log(totalDays); // 6
```

```js
array.reduce(回调函数, 初始值);
```

| 参数 | 可接受的值 | 是否必写 | 作用 |
| --- | --- | --- | --- |
| 回调函数 | 接收累计值和当前元素的函数 | 是 | 计算下一轮累计值 |
| 初始值 | 数字、字符串、数组、对象等 | 建议必写 | 设置第一次累计时的起点 |

当前示例中：

- `total` 是累计值。
- `app` 是当前申请。
- `0` 是累计初始值。
- 每轮返回 `total + app.days`，最终得到所有申请天数的总和。

## 13. 方法选择总结

| 需求 | 选择的方法 | 返回结果 |
| --- | --- | --- |
| 对每项执行操作 | `forEach()` | `undefined` |
| 查找第一项 | `find()` | 元素或 `undefined` |
| 查找第一项下标 | `findIndex()` | 下标或 `-1` |
| 筛选多项 | `filter()` | 新数组 |
| 转换每一项 | `map()` | 新数组 |
| 判断至少一项满足 | `some()` | 布尔值 |
| 判断全部满足 | `every()` | 布尔值 |
| 自定义排序 | `sort()` | 排序后的原数组 |
| 累计为一个结果 | `reduce()` | 累计结果 |

## 14. 本章练习

使用下面数据：

```js
const users = [
  { accountId: "yamada", name: "山田 太郎", active: true, score: 80 },
  { accountId: "suzuki", name: "鈴木 花子", active: false, score: 65 },
  { accountId: "tanaka", name: "田中 一郎", active: true, score: 90 }
];
```

完成：

1. 使用 `forEach()` 输出每个用户的姓名。
2. 使用 `find()` 查找账号为 `"yamada"` 的用户。
3. 使用 `filter()` 取得所有启用用户。
4. 使用 `map()` 生成只包含姓名的新数组。
5. 使用 `some()` 判断是否存在停用用户。
6. 使用 `every()` 判断是否所有用户分数都大于等于 `60`。
7. 复制数组后，使用 `sort()` 按分数从高到低排列。
8. 使用 `reduce()` 计算所有用户的总分，预期结果为 `235`。

## 本章检查点

- 能说明数组回调函数中当前元素和下标的来源。
- 能根据“处理、查找、筛选、转换、判断”选择数组方法。
- 能说明 `find()` 和 `filter()` 的结果差异。
- 能说明 `forEach()` 和 `map()` 的结果差异。
- 能正确编写数字和对象数组的排序规则。
- 能使用 `reduce()` 完成简单求和。
- 能看懂由 `filter()` 和 `map()` 组成的链式调用。
