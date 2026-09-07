# 第 11 章 常量与固定值的表达

页面状态、排序方向和选项代码通常只允许几个固定值。本章学习字面量联合、`as const`常量对象和`enum`的区别。新代码优先采用简单、明确的表达；维护已有项目时遵守项目约定。

## 1. let、const 与字面量推断

```ts
let editable = "red";
const fixed = "red";

editable = "blue";
console.log(editable, fixed); // blue red
```

`editable`通常推断为`string`，因为以后可以重新赋值。`fixed`不能重新赋值，因此会保留更具体的`"red"`类型。

但`const`只限制变量不能指向另一个值，对象属性和数组内容仍然可以修改：

```ts
const setting = { theme: "light" };
setting.theme = "dark";
console.log(setting.theme); // dark
```

## 2. as const 常量断言

### 2.1 对象使用 as const

```ts
const STATUS = {
  PENDING: "pending",
  APPROVED: "approved",
  REJECTED: "rejected",
} as const;

console.log(STATUS.PENDING); // pending
// STATUS.PENDING = "waiting"; // 错误：属性只读
```

`as const`写在字面量后面，主要产生三个效果：

1. 字符串和数字保留具体字面量类型，不扩大成普通`string`或`number`。
2. 对象属性推断为只读。
3. 数组推断为只读元组。

它是 TypeScript 编译阶段语法，不会改变运行时的值。

### 2.2 数组使用 as const

```ts
const directions = ["asc", "desc"] as const;

console.log(directions[0]); // asc
// directions.push("other"); // 错误：只读元组不能push
```

普通`["asc", "desc"]`通常推断为可修改的`string[]`；添加`as const`后会保留两个位置的具体值。

### 2.3 不要机械添加 as const

适合使用的场景：

- 状态代码、排序方向等固定选项；
- 运行时需要读取一组常量；
- 后续需要从常量派生联合类型；
- 固定位置和具体值都不应修改的数组。

普通表单对象、计数数组等数据需要修改，不应添加`as const`后再用断言绕过只读检查。单个`const`字符串已经能正确推断时，也不必重复添加。

## 3. 从常量对象取得允许的值类型

```ts
const STATUS = {
  PENDING: "pending",
  APPROVED: "approved",
  REJECTED: "rejected",
} as const;

type Status = typeof STATUS[keyof typeof STATUS];

const currentStatus: Status = "pending";
console.log(currentStatus); // pending
// const wrongStatus: Status = "unknown"; // 错误：不是允许的值
```

按顺序理解这段类型：

1. 类型位置的`typeof STATUS`取得常量对象的静态类型。
2. `keyof typeof STATUS`取得属性名联合：`"PENDING" | "APPROVED" | "REJECTED"`。
3. `typeof STATUS[...]`根据这些属性名取得值类型：`"pending" | "approved" | "rejected"`。

这种写法在框架项目的选项、配置和状态代码中比较常见。初学阶段先会照现有模式使用，不要求设计更复杂的类型运算。

如果运行时不需要常量对象，直接使用字面量联合更简单：

```ts
type Status = "pending" | "approved" | "rejected";
```

## 4. enum 枚举（会使用、能阅读）

`enum`也能把一组固定值组织成具名成员：

```ts
enum Color {
  Red = "red",
  Blue = "blue",
}

const color: Color = Color.Red;
console.log(color); // red
```

与类型别名不同，`enum`会生成 JavaScript 对象，因此运行时可以读取`Color.Red`。已有项目使用`enum`时，要沿用同一套成员和取值，不要再重复维护另一套常量。

### 4.1 字符串枚举与数字枚举

上面的示例是字符串枚举，成员值清楚，日志和接口数据也容易理解。

```ts
enum Step {
  Input,
  Confirm,
  Complete,
}

console.log(Step.Input);   // 0
console.log(Step.Confirm); // 1
```

没有显式赋值的数字枚举默认从`0`递增。调整成员顺序可能改变数值，因此业务状态不应依赖不明确的自动编号。

### 4.2 enum 的使用原则

- 维护已经使用`enum`的项目：遵守现有约定。
- 新增枚举成员：确认接口、数据库和其他模块使用的实际值。
- 新写简单固定选项：优先考虑字面量联合或`as const`对象。
- 不要同时维护内容相同的`enum`、常量对象和手写联合类型。

本课程要求能够声明和使用简单的字符串枚举，并能读懂数字枚举；不要求把所有固定值都改成`enum`。

## 5. as const 不等于运行时冻结

```ts
const config = { mode: "development" } as const;
console.log(Object.isFrozen(config)); // false
```

`as const`只提供编译阶段限制，不会调用运行时冻结功能。它也不会校验接口响应、浏览器存储或用户输入。

`Object.freeze()`属于 JavaScript 运行时方法，解决的是另一个问题。本课程只需知道两者不同；需要运行时不可变设计时再按项目要求处理。

## 6. 如何选择

| 需求 | 常见选择 |
| --- | --- |
| 只限制变量允许哪些值 | 字面量联合 |
| 运行时需要读取常量，同时派生值类型 | 对象加`as const` |
| 维护已经使用枚举的项目 | 按现有约定使用`enum` |
| 外部数据是否合法 | 运行时校验，不能依赖以上写法 |

## 7. 常见错误

- 认为`const`对象的属性会自动只读。
- 对所有数据机械添加`as const`，导致正常修改也被阻止。
- 认为`as const`会验证或冻结运行时数据。
- 同时维护多套相同的状态定义，新增选项时漏改。
- 使用数字枚举表示外部状态，却没有明确确认实际编号。

## 8. 练习

1. 创建普通字符串数组和带`as const`的字符串数组，比较编辑器推断类型，并分别尝试`push()`。
2. 使用`as const`定义申请状态常量对象，再派生允许的状态值类型。
3. 使用字面量联合重新表达同一组状态，说明什么时候不需要常量对象。
4. 声明一个字符串`enum`，创建变量并通过枚举成员赋值。
5. 声明一个数字枚举并输出成员值，说明为什么业务状态更适合使用明确值。

## 本章检查点

- 能说明普通`const`为什么不等于对象属性只读。
- 能说明`as const`对对象和数组推断结果的影响。
- 能从常量对象取得值类型，并能阅读`typeof`、`keyof`和索引访问组合。
- 能区分字面量联合、`as const`对象和`enum`。
- 能声明并使用简单字符串枚举，能读懂数字枚举。
- 能说明`as const`和`enum`都不能代替外部数据校验。
