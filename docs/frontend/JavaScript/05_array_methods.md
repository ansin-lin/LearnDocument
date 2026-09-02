# 第五章 数组基本方法

## 学习目标

完成本章后，你应能够：

- 使用 `push()`、`pop()`、`unshift()` 和 `shift()` 在数组两端增删元素。
- 使用 `splice()` 在指定位置增加、删除或替换元素。
- 使用 `slice()` 和 `concat()` 生成新数组。
- 使用 `indexOf()` 和 `includes()` 查找简单值。
- 使用 `join()` 把数组连接成字符串。
- 使用 `reverse()` 反转数组顺序。
- 区分会修改原数组的方法和返回新数组的方法。

## 1. 数组方法是什么

数组方法是 JavaScript 为数组提供的常用操作。方法写在数组变量后面，中间使用点号连接。

```js
const statuses = ["pending", "approved"];

statuses.push("cancelled");
console.log(statuses);
```

`statuses.push("cancelled")` 表示调用 `statuses` 数组的 `push()` 方法，在数组末尾添加一个元素。

注意：JavaScript 数组没有 `append()` 方法。在数组末尾追加元素时使用 `push()`。DOM 元素有 `append()`，不要把两者混淆。

## 2. 是否修改原数组

有些数组方法直接修改原数组，有些方法返回新数组。

```js
const statuses = ["pending", "approved"];
statuses.push("cancelled");

console.log(statuses);
// ["pending", "approved", "cancelled"]
```

`push()` 直接修改了 `statuses`。

```js
const statuses = ["pending", "approved", "cancelled"];
const firstTwo = statuses.slice(0, 2);

console.log(statuses); // 原数组不变
console.log(firstTwo); // ["pending", "approved"]
```

`slice()` 返回新数组，不修改 `statuses`。Vue 和 React 项目经常需要保留原数组，因此使用方法前要确认这一点。

## 3. 在末尾增删元素

### 3.1 `push()`：在末尾添加

`push()` 可以在数组末尾添加一个或多个元素，并返回添加后的数组长度。

```js
const statuses = ["pending"];
const newLength = statuses.push("approved", "cancelled");

console.log(statuses);  // ["pending", "approved", "cancelled"]
console.log(newLength); // 3
```

- 参数：一个或多个要添加的值。
- 返回值：添加后的数组长度。
- 是否修改原数组：是。

### 3.2 `pop()`：删除末尾元素

`pop()` 删除并返回最后一个元素，不需要参数。

```js
const statuses = ["pending", "approved", "cancelled"];
const removedStatus = statuses.pop();

console.log(removedStatus); // cancelled
console.log(statuses);      // ["pending", "approved"]
```

- 参数：无。
- 返回值：被删除的元素；空数组返回 `undefined`。
- 是否修改原数组：是。

## 4. 在开头增删元素

### 4.1 `unshift()`：在开头添加

`unshift()` 可以在数组开头添加一个或多个元素，并返回添加后的数组长度。

```js
const statuses = ["approved", "cancelled"];
const newLength = statuses.unshift("pending");

console.log(statuses);  // ["pending", "approved", "cancelled"]
console.log(newLength); // 3
```

- 参数：一个或多个要添加的值。
- 返回值：添加后的数组长度。
- 是否修改原数组：是。

### 4.2 `shift()`：删除开头元素

`shift()` 删除并返回第一个元素，不需要参数。

```js
const statuses = ["pending", "approved", "cancelled"];
const removedStatus = statuses.shift();

console.log(removedStatus); // pending
console.log(statuses);      // ["approved", "cancelled"]
```

- 参数：无。
- 返回值：被删除的元素；空数组返回 `undefined`。
- 是否修改原数组：是。

### 4.3 四种方法对比

| 方法 | 操作位置 | 作用 | 返回值 | 是否修改原数组 |
| --- | --- | --- | --- | --- |
| `push()` | 末尾 | 添加一个或多个元素 | 新长度 | 是 |
| `pop()` | 末尾 | 删除一个元素 | 被删除的元素 | 是 |
| `unshift()` | 开头 | 添加一个或多个元素 | 新长度 | 是 |
| `shift()` | 开头 | 删除一个元素 | 被删除的元素 | 是 |

## 5. `splice()`：在指定位置增删改

```text
array.splice(开始下标, 删除数量, 要插入的元素...);
```

- 第一个参数：开始操作的下标。
- 第二个参数：要删除的数量；写 `0` 表示不删除。
- 后续参数：可选，要插入的新元素。
- 返回值：由被删除元素组成的新数组。
- 是否修改原数组：是。

### 5.1 删除元素

```js
const statuses = ["pending", "approved", "cancelled"];
const removedStatuses = statuses.splice(1, 1);

console.log(statuses);        // ["pending", "cancelled"]
console.log(removedStatuses); // ["approved"]
```

`splice(1, 1)` 表示从下标 `1` 开始删除 `1` 个元素。

### 5.2 插入元素

```js
const statuses = ["pending", "cancelled"];
statuses.splice(1, 0, "approved");

console.log(statuses); // ["pending", "approved", "cancelled"]
```

第二个参数为 `0`，因此不删除元素，只在下标 `1` 的位置插入新值。

### 5.3 替换元素

```js
const statuses = ["pending", "approved", "cancelled"];
statuses.splice(1, 1, "rejected");

console.log(statuses); // ["pending", "rejected", "cancelled"]
```

这次先删除下标 `1` 的一个元素，再在同一位置插入 `"rejected"`，因此形成替换效果。

## 6. `slice()`：截取数组

`slice()` 截取一部分元素并返回新数组，不修改原数组。

```js
const statuses = ["pending", "approved", "cancelled", "rejected"];
const selectedStatuses = statuses.slice(1, 3);

console.log(selectedStatuses); // ["approved", "cancelled"]
console.log(statuses);         // 原数组不变
```

```js
array.slice(开始下标, 结束下标);
```

- 开始下标：包含在结果中；省略时从 `0` 开始。
- 结束下标：不包含在结果中；省略时截取到末尾。
- 返回值：截取到的新数组。
- 是否修改原数组：否。

复制整个数组时可以省略两个参数：

```js
const copiedStatuses = statuses.slice();
```

## 7. `concat()`：合并数组

`concat()` 连接数组或其他值，返回新数组，不修改原数组。

```js
const activeStatuses = ["pending", "approved"];
const closedStatuses = ["cancelled", "rejected"];
const allStatuses = activeStatuses.concat(closedStatuses);

console.log(allStatuses);
// ["pending", "approved", "cancelled", "rejected"]
```

- 参数：一个或多个数组或值。
- 返回值：合并后的新数组。
- 是否修改原数组：否。

## 8. 查找简单值

### 8.1 `indexOf()`：查找下标

`indexOf()` 返回指定元素第一次出现的下标，找不到时返回 `-1`。

```js
const statuses = ["pending", "approved", "cancelled"];

console.log(statuses.indexOf("approved")); // 1
console.log(statuses.indexOf("rejected")); // -1
```

- 参数：要查找的值；还可以传入可选的开始下标。
- 返回值：第一次匹配的下标；找不到返回 `-1`。
- 是否修改原数组：否。

### 8.2 `includes()`：判断是否包含

`includes()` 判断数组是否包含指定元素。

```js
const statuses = ["pending", "approved", "cancelled"];

console.log(statuses.includes("pending"));  // true
console.log(statuses.includes("rejected")); // false
```

- 参数：要查找的值；还可以传入可选的开始下标。
- 返回值：包含时为 `true`，否则为 `false`。
- 是否修改原数组：否。

只需要判断“有没有”时，`includes()` 比 `indexOf() !== -1` 更容易阅读。这两个方法适合直接查找字符串、数字等简单值。第七章会使用回调方法查找对象数组。

## 9. `join()`：连接成字符串

`join()` 使用指定分隔符连接数组元素，返回字符串。

```js
const statuses = ["pending", "approved", "cancelled"];

console.log(statuses.join(", "));
// pending, approved, cancelled
```

- 参数：分隔符字符串；省略时默认使用逗号 `,`。
- 返回值：连接后的字符串。
- 是否修改原数组：否。

## 10. `reverse()`：反转顺序

`reverse()` 将数组元素顺序反转，并返回修改后的原数组。

```js
const statuses = ["pending", "approved", "cancelled"];
const result = statuses.reverse();

console.log(statuses); // ["cancelled", "approved", "pending"]
console.log(result);   // ["cancelled", "approved", "pending"]
```

- 参数：无。
- 返回值：反转后的原数组。
- 是否修改原数组：是。

需要保留原数组时，先复制再反转：

```js
const statuses = ["pending", "approved", "cancelled"];
const reversedStatuses = statuses.slice().reverse();

console.log(statuses);         // 原数组不变
console.log(reversedStatuses); // ["cancelled", "approved", "pending"]
```

## 11. 常用方法总结

| 目标 | 常用方法 | 返回结果 | 是否修改原数组 |
| --- | --- | --- | --- |
| 末尾添加、删除 | `push()`、`pop()` | 新长度或被删除元素 | 是 |
| 开头添加、删除 | `unshift()`、`shift()` | 新长度或被删除元素 | 是 |
| 指定位置增删改 | `splice()` | 被删除元素数组 | 是 |
| 截取数组 | `slice()` | 新数组 | 否 |
| 合并数组 | `concat()` | 新数组 | 否 |
| 查找简单值 | `indexOf()`、`includes()` | 下标或布尔值 | 否 |
| 连接成字符串 | `join()` | 字符串 | 否 |
| 反转顺序 | `reverse()` | 原数组 | 是 |

## 12. 本章练习

### 练习 1：数组两端增删

```js
const departments = ["development", "sales"];
```

依次完成：

1. 使用 `push()` 添加 `"quality"`。
2. 使用 `unshift()` 在开头添加 `"management"`。
3. 使用 `pop()` 删除末尾元素并输出返回值。
4. 使用 `shift()` 删除开头元素并输出返回值。
5. 输出最终数组，确认结果为 `["development", "sales"]`。

### 练习 2：指定位置修改

```js
const scores = [60, 75, 90];
```

依次完成：

1. 使用 `splice()` 把 `75` 替换成 `80`。
2. 使用 `slice()` 取得前两个元素。
3. 输出原数组和截取结果，说明哪个方法修改了原数组。

### 练习 3：查询和转换

```js
const skills = ["HTML", "CSS", "JavaScript"];
```

完成：

1. 使用 `includes()` 判断是否包含 `"CSS"`。
2. 使用 `indexOf()` 取得 `"JavaScript"` 的下标。
3. 使用 `join(" / ")` 得到 `"HTML / CSS / JavaScript"`。
4. 复制数组后调用 `reverse()`，确认原数组没有变化。

## 本章检查点

- 知道 JavaScript 数组使用 `push()` 追加元素，而不是 `append()`。
- 能使用四种基本方法在数组开头或末尾增删元素。
- 能使用 `splice()` 完成指定位置的增加、删除和替换。
- 能说明 `splice()` 与 `slice()` 的区别。
- 能使用 `indexOf()` 和 `includes()` 查找简单值。
- 能使用 `join()` 和 `reverse()`。
- 能判断常用数组基本方法是否修改原数组。
