# 第三章 运算符、条件判断与真值判断

## 学习目标

完成本章后，你应能够：

- 使用比较运算判断输入是否合法。
- 使用 `if` 根据条件执行不同逻辑。
- 使用 `switch...case` 处理多个固定值分支。
- 使用 `&&`、`||`、`!` 组合条件。
- 默认使用严格相等 `===`。

## 1. 算术运算

常见算术运算：

| 运算符 | 含义 | 示例 |
| --- | --- | --- |
| `+` | 加法或字符串拼接 | `1 + 2` |
| `-` | 减法 | `12 - 2` |
| `*` | 乘法 | `2 * 3` |
| `/` | 除法 | `10 / 2` |
| `%` | 取余 | `5 % 2` |

示例：

```js
const totalDays = 12;
const usedDays = 2;
const remainingDays = totalDays - usedDays;
```

### 1.1 复合赋值运算符

复合赋值运算符把“计算”和“重新赋值”写在一起。

| 写法 | 等价写法 | 含义 |
| --- | --- | --- |
| `value += 2` | `value = value + 2` | 加后重新赋值 |
| `value -= 2` | `value = value - 2` | 减后重新赋值 |
| `value *= 2` | `value = value * 2` | 乘后重新赋值 |
| `value /= 2` | `value = value / 2` | 除后重新赋值 |
| `value %= 2` | `value = value % 2` | 取余后重新赋值 |

```js
let remainingDays = 12;

remainingDays -= 2;
console.log(remainingDays); // 10
```

因为复合赋值会改变变量保存的值，所以变量必须使用 `let` 声明。

### 1.2 自增和自减

`++` 让数字增加 `1`，`--` 让数字减少 `1`。

```js
let count = 1;

count++;
console.log(count); // 2

count--;
console.log(count); // 1
```

`count++` 和 `count += 1` 都能让计数器增加 `1`。课程示例优先使用含义更直观的 `count += 1`。

## 2. 严格相等

判断两个值是否相等，默认使用 `===`。

```js
const status = "pending";
console.log(status === "pending"); // true
console.log(status === "approved"); // false
```

常见比较：

| 写法 | 含义 |
| --- | --- |
| `a === b` | 值和类型都相等 |
| `a !== b` | 值或类型不相等 |
| `a > b` | 大于 |
| `a >= b` | 大于等于 |
| `a < b` | 小于 |
| `a <= b` | 小于等于 |

不建议使用 `==`。它会发生隐式类型转换，新人很难判断结果。

## 3. if 条件判断

```js
const remainingDays = 8;

if (remainingDays >= 1) {
  console.log("有可用休假天数");
} else {
  console.log("剩余天数不足");
}
```

`if` 后面的条件为 `true` 时执行第一段，否则执行 `else`。

## 4. 多条件判断

```js
const days = 2;

if (days <= 0) {
  console.log("申請日数が不正です");
} else if (days > 12) {
  console.log("残日数を超えています");
} else {
  console.log("申請できます");
}
```

多个条件从上到下判断，先满足哪个就执行哪个。

## 5. 逻辑运算符

| 写法 | 含义 | 示例 |
| --- | --- | --- |
| `&&` | 并且，两个条件都要满足 | `days > 0 && days <= 12` |
| <code>&#124;&#124;</code> | 或者，满足一个即可 | <code>status === "pending" &#124;&#124; status === "approved"</code> |
| `!` | 取反 | `!isLoggedIn` |

示例：

```js
const accountId = "yamada.taro";
const password = "training123";

if (accountId !== "" && password !== "") {
  console.log("入力済み");
}
```

这里先分别比较：账号不是空字符串得到 `true`，密码不是空字符串也得到 `true`，`&&` 才得到 `true`。把其中一个值改为 `""` 后重新运行，消息就不会输出。

`&&` 表示条件必须同时满足，`||` 表示至少一个满足，`!` 把判断结果反过来。复杂条件可以用括号明确组合范围。

## 6. 真值和假值

在条件判断中，不是只有 `true` 和 `false` 会被判断。

常见假值：

| 值 | 含义 |
| --- | --- |
| `false` | 布尔假 |
| `0` | 数字 0 |
| `""` | 空字符串 |
| `null` | 明确没有值 |
| `undefined` | 未定义 |
| `NaN` | 非数字 |

其他大多数值会被当成真值。

下面是独立实验。账号来自输入框时是字符串，空字符串在条件位置按假处理：

```js
const accountId = "";
if (!accountId) {
  console.log("アカウントを入力してください");
}
```

含义：空字符串按假处理，`!` 将它转换为 `true`，所以显示错误。对于已确定为字符串的输入值，`if (accountId && password)` 就是前面非空判断的简写；它不会自动去掉空格。

注意，`&&`、`||` 本身返回某个操作数，不一定返回布尔值：`console.log("田中" && "已填写")` 输出“已填写”。只有放在条件位置时，结果才按真值或假值决定是否进入分支。`&&` 遇到假值、`||` 遇到真值时会停止计算后续操作数，这称为短路。

## 7. 三元运算符

三元运算符适合简单二选一。

```js
const status = "pending";
const statusText = status === "pending" ? "申請中" : "処理済み";
console.log(statusText); // 申請中
```

写法为 `条件 ? 成立时的值 : 不成立时的值`。先判断条件，只选择其中一个值，最后把选中的值赋给 `statusText`。

复杂逻辑不要硬写成三元，使用 `if` 更清楚。

## 8. switch...case 多分支判断

当同一个变量需要与多个固定值进行比较时，可以使用 `switch...case`。

```js
const status = "approved";

switch (status) {
  case "pending":
    console.log("申請中");
    break;
  case "approved":
    console.log("承認済み");
    break;
  case "cancelled":
    console.log("取消済み");
    break;
  default:
    console.log("不明な状態");
}
```

执行过程如下：

1. `switch (status)` 取得要判断的值。
2. 按顺序与每个 `case` 后面的值进行严格比较。
3. 找到匹配项后，执行该项下面的代码。
4. `break` 结束整个 `switch`，防止继续执行后面的分支。
5. 如果所有 `case` 都不匹配，就执行 `default`。

### 8.1 为什么通常要写 break

如果省略 `break`，程序会从匹配的分支开始，继续执行后面的分支。这种现象称为贯穿执行（fall-through）。

```js
const status = "pending";

switch (status) {
  case "pending":
    console.log("申請中");
  case "approved":
    console.log("承認済み");
    break;
}
```

输出结果为：

```text
申請中
承認済み
```

因为第一个 `case` 后没有 `break`，第二个分支也被执行了。除非明确需要合并分支，否则每个 `case` 结尾都应写 `break`。

多个值执行相同处理时，可以有意合并分支：

```js
const status = "cancelled";

switch (status) {
  case "rejected":
  case "cancelled":
    console.log("処理終了");
    break;
  default:
    console.log("処理中");
}
```

### 8.2 选择 if...else 还是 switch...case

| 判断方式 | 适合的场景 | 示例 |
| --- | --- | --- |
| `if...else` | 范围判断、多个不同条件、复杂逻辑 | 天数是否小于 `0`、密码是否少于 8 位 |
| `switch...case` | 同一个值与多个固定值比较 | 状态是 `pending`、`approved` 还是 `cancelled` |

`switch...case` 不是 `if...else` 的完全替代品。根据判断条件选择更容易阅读的写法。

## 本章练习

编写校验逻辑：

```js
const requestedDays = 3;
const remainingDays = 5;
const hasReason = true;
```

要求：

1. 申请天数必须大于 `0`。
2. 申请天数不能超过剩余天数。
3. 必须已经填写申请理由。
4. 三个条件都满足时输出“申請可能”，否则输出“入力内容を確認してください”。
5. 另外定义值为 `"half-am"` 的 `leaveType`，使用 `switch...case` 输出“午前休”。

## 本章检查点

- 能使用 `===` 和 `!==`。
- 能写 `if / else if / else`。
- 能使用 `switch / case / break / default` 处理多个固定值分支。
- 能使用 `&&`、`||`、`!`。
- 能判断空字符串。
