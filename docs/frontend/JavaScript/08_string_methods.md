# 第八章 字符串常用方法

## 学习目标

完成本章后，你应能够：

- 使用 `length` 和下标读取字符串内容。
- 使用 `includes()`、`indexOf()`、`startsWith()` 和 `endsWith()` 查找文字。
- 使用 `slice()`、`trim()` 和大小写转换方法整理文字。
- 使用 `replace()`、`replaceAll()`、`split()` 处理字符串。
- 说明字符串方法通常返回新字符串，不会修改原字符串。
- 看懂 `replace()` 和 `replaceAll()` 接收回调函数的写法。

## 1. 字符串不会被原地修改

字符串属于原始类型。字符串方法通常返回处理后的新字符串，不会直接修改原字符串。

```js
const accountId = "  YAMADA.TARO  ";
const normalizedAccountId = accountId.trim().toLowerCase();

console.log(accountId);           // "  YAMADA.TARO  "
console.log(normalizedAccountId); // "yamada.taro"
```

`trim()` 去除字符串开头和结尾的空白，`toLowerCase()` 把英文字母转换成小写。这里连续调用两个方法，得到整理后的账号字符串。

需要保存处理结果时，应把返回值赋给新变量，或者重新赋值给使用 `let` 声明的变量。

## 2. `length`：取得字符串长度

`length` 属性表示字符串包含多少个 UTF-16 代码单元。对于本课程中的普通中文、日文、英文和数字，可以先把它理解为字符数量。

```js
const password = "training123";

console.log(password.length); // 11
```

`length` 是属性，后面不写括号。

```js
password.length;  // 正确
password.length(); // 错误
```

包含部分 emoji 或特殊组合字符时，`length` 可能与肉眼看到的字符数量不同，本课程暂不展开这一边界。

## 3. 使用下标读取字符

字符串下标从 `0` 开始。

```js
const status = "pending";

console.log(status[0]); // p
console.log(status[1]); // e
```

读取不存在的下标会得到 `undefined`。字符串不能像数组一样通过下标修改单个字符。

```js
status[0] = "P"; // 不会把原字符串改成 "Pending"
```

需要修改时，应通过字符串方法生成新字符串。

## 4. 查找字符串内容

### 4.1 `includes()`：判断是否包含

```js
const message = "申请已成功提交";

console.log(message.includes("成功")); // true
console.log(message.includes("失败")); // false
```

- 参数：要查找的字符串；可选的开始位置。
- 返回值：包含时为 `true`，否则为 `false`。
- 是否修改原字符串：否。

### 4.2 `indexOf()`：取得第一次出现的位置

```js
const accountId = "yamada.taro";

console.log(accountId.indexOf(".")); // 6
console.log(accountId.indexOf("@")); // -1
```

- 参数：要查找的字符串；可选的开始位置。
- 返回值：第一次出现的下标；找不到返回 `-1`。
- 是否修改原字符串：否。

### 4.3 `startsWith()` 和 `endsWith()`

```js
const requestId = "REQ-20260820-001";

console.log(requestId.startsWith("REQ-")); // true
console.log(requestId.endsWith("-001"));   // true
```

`startsWith()` 判断开头，`endsWith()` 判断结尾，结果都是布尔值。它们不会修改原字符串。

## 5. `slice()`：截取字符串

`slice()` 返回从开始下标到结束下标之前的内容。

```js
const requestId = "REQ-20260820-001";
const datePart = requestId.slice(4, 12);

console.log(datePart); // 20260820
```

```js
text.slice(开始下标, 结束下标);
```

- 开始下标：包含在结果中；省略时从 `0` 开始。
- 结束下标：不包含在结果中；省略时截取到末尾。
- 返回值：截取出的新字符串。
- 是否修改原字符串：否。

负数下标表示从字符串末尾开始计算：

```js
console.log(requestId.slice(-3)); // 001
```

## 6. `trim()`：去除首尾空白

```js
const inputName = "  山田 太郎  ";

console.log(inputName.trim());      // "山田 太郎"
console.log(inputName.trimStart()); // "山田 太郎  "
console.log(inputName.trimEnd());   // "  山田 太郎"
```

- `trim()`：去除开头和结尾的空白。
- `trimStart()`：只去除开头空白。
- `trimEnd()`：只去除结尾空白。

这些方法不会删除文字中间的空格，也不会修改原字符串。

## 7. 大小写转换

```js
const accountId = "Yamada.Taro";

console.log(accountId.toLowerCase()); // yamada.taro
console.log(accountId.toUpperCase()); // YAMADA.TARO
```

- `toLowerCase()` 返回小写字符串。
- `toUpperCase()` 返回大写字符串。
- 两者都不会修改原字符串。

账号、邮箱等不区分大小写的数据，可以在比较前统一转换成小写。

## 8. `replace()`：替换第一个匹配内容

```js
const message = "申请状态：pending";
const result = message.replace("pending", "approved");

console.log(result);  // 申请状态：approved
console.log(message); // 申请状态：pending
```

- 第一个参数：要查找的字符串或正则表达式。
- 第二个参数：替换后的字符串或回调函数。
- 返回值：替换后的新字符串。
- 是否修改原字符串：否。

传入普通字符串时，`replace()` 只替换第一个匹配项。

## 9. `replaceAll()`：替换全部匹配内容

```js
const dateText = "2026/08/21";
const normalizedDate = dateText.replaceAll("/", "-");

console.log(normalizedDate); // 2026-08-21
```

- 第一个参数：要查找的字符串或全局正则表达式。
- 第二个参数：替换后的字符串或回调函数。
- 返回值：替换后的新字符串。
- 是否修改原字符串：否。

## 10. 使用回调函数决定替换结果

`replace()` 和 `replaceAll()` 的第二个参数可以是回调函数。每找到一个匹配内容，JavaScript 就调用一次回调函数，并使用函数返回值完成替换。

```js
const message = "申请编号：req-001";

const result = message.replace("req", matchedText => {
  return matchedText.toUpperCase();
});

console.log(result); // 申请编号：REQ-001
```

当前示例中：

- `matchedText` 接收匹配到的 `"req"`。
- `toUpperCase()` 返回 `"REQ"`。
- `replace()` 使用返回值替换原来的匹配内容。

这种写法适合替换结果需要根据匹配内容动态计算的场景。复杂匹配会在正则表达式章节继续讲解。

## 11. `split()`：把字符串拆成数组

`split()` 使用指定分隔符切割字符串，返回数组。

```js
const csvText = "pending,approved,cancelled";
const statuses = csvText.split(",");

console.log(statuses);
// ["pending", "approved", "cancelled"]
```

- 参数：分隔符字符串或正则表达式；还可以传入可选的最大结果数量。
- 返回值：切割后得到的新数组。
- 是否修改原字符串：否。

`split()` 与数组的 `join()` 方向相反：

```js
const statuses = "pending,approved".split(",");
const text = statuses.join(",");
```

## 12. 方法链

字符串方法返回字符串时，可以继续调用另一个字符串方法。

```js
const inputAccountId = "  YAMADA.TARO  ";
const accountId = inputAccountId.trim().toLowerCase();

console.log(accountId); // yamada.taro
```

执行顺序是先 `trim()`，再对结果调用 `toLowerCase()`。

## 13. 常用方法总结

| 需求 | 方法或属性 | 返回结果 |
| --- | --- | --- |
| 取得长度 | `length` | 数字 |
| 判断包含 | `includes()` | 布尔值 |
| 查找位置 | `indexOf()` | 下标或 `-1` |
| 判断开头、结尾 | `startsWith()`、`endsWith()` | 布尔值 |
| 截取内容 | `slice()` | 新字符串 |
| 去除首尾空白 | `trim()` | 新字符串 |
| 转换大小写 | `toLowerCase()`、`toUpperCase()` | 新字符串 |
| 替换内容 | `replace()`、`replaceAll()` | 新字符串 |
| 拆分为数组 | `split()` | 新数组 |

## 14. 本章练习

### 练习 1：整理邮箱

```js
const inputEmail = "  SUPPORT@EXAMPLE.COM  ";
```

使用字符串方法完成：

1. 去除首尾空白。
2. 转换为小写。
3. 判断整理后的邮箱是否包含 `"@"`。
4. 输出最终结果，应为 `"support@example.com"`。

### 练习 2：处理申请编号

```js
const documentId = "DOC-20260915-042";
```

完成：

1. 判断是否以 `"DOC-"` 开头。
2. 使用 `slice()` 取得日期部分 `"20260915"`。
3. 使用 `slice()` 取得最后三位流水号 `"042"`。

### 练习 3：替换与拆分

```js
const departmentText = "development|sales|quality";
```

完成：

1. 使用 `replaceAll()` 把 `"|"` 替换成逗号。
2. 使用 `split()` 把原字符串拆成数组。
3. 使用 `map()` 和 `toUpperCase()` 生成大写部门数组。

## 本章检查点

- 能说明字符串下标和数组下标都从 `0` 开始。
- 能使用查找、截取、清理、大小写转换和替换方法。
- 能说明字符串方法不会直接修改原字符串。
- 能使用 `split()` 在字符串和数组之间转换。
- 能看懂 `replace()` 接收回调函数的写法。
- 能组合多个字符串方法整理表单输入。
