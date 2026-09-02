# 第 21 章 Set、Map 与元编程基础

## 本章目标

学完本章后，你应当能够：

- 使用 `Set` 保存不重复的数据，并完成添加、判断、删除和遍历；
- 使用 `Map` 保存“键和值”的对应关系，并根据业务场景选择 `Map` 或普通对象；
- 理解 `Symbol`、`Proxy`、`Reflect` 分别解决什么问题；
- 阅读框架中与“唯一标识”“数据代理”“响应式”有关的基础代码。

> 本章分为两个掌握层级：
>
> - **必须掌握**：`Set`、`Map`；
> - **理解用途、能够阅读**：`WeakSet`、`WeakMap`、`Symbol`、`Proxy`、`Reflect`。
>
> Vue、React 等框架会使用其中一些机制，但零基础阶段不要求手写框架底层。

---

## 1. 为什么还需要新的数据结构

前面学习过数组和普通对象：

- 数组适合保存一组有顺序的数据；
- 普通对象适合保存一组固定属性。

但在实际开发中，还会遇到下面的问题：

1. 用户选择的申请日期不能重复；
2. 需要判断某个编号是否已经处理；
3. 需要建立“状态代码 → 显示文本”的对应关系；
4. 希望使用对象本身作为键；
5. 希望在读写对象属性时执行校验或记录日志。

这些问题分别适合使用 `Set`、`Map`、`Symbol`、`Proxy` 和 `Reflect` 解决。

---

## 第一部分：Set

### 2. Set 是什么

`Set` 是用于保存**不重复值**的集合。

```js
const selectedDates = new Set();
```

`new Set()` 会创建一个空集合。构造方法可以接收一个可迭代对象，例如数组。

```js
const selectedDates = new Set([
  "2026-09-01",
  "2026-09-02",
  "2026-09-01"
]);

console.log(selectedDates);
// Set(2) {"2026-09-01", "2026-09-02"}
```

重复的 `"2026-09-01"` 只会保留一份。

#### 2.1 Set 的主要特点

- 值不会重复；
- 按照值加入集合的先后顺序遍历；
- 没有数组下标，不能写 `selectedDates[0]`；
- 使用 `size` 属性获取元素数量；
- 可以保存字符串、数字、布尔值、对象等数据。

> `Set` 是“集合”，不要把它理解成“没有顺序的数组”。JavaScript 的 `Set` 会保留插入顺序，但不能通过下标读取。

---

### 3. Set 的常用属性和方法

```js
const selectedDates = new Set();
```

| 写法 | 参数 | 返回值 | 作用 |
| --- | --- | --- | --- |
| `set.size` | 无 | 元素数量 | 获取集合大小 |
| `set.add(value)` | 要加入的值 | 当前 Set | 添加一个值，可连续调用 |
| `set.has(value)` | 要查找的值 | 布尔值 | 判断值是否存在 |
| `set.delete(value)` | 要删除的值 | 布尔值 | 删除值；成功为 `true` |
| `set.clear()` | 无 | `undefined` | 清空全部值 |

#### 3.1 add：添加值

`add(value)` 把一个值加入集合。重复添加同一个值不会报错，也不会增加数量。

```js
selectedDates.add("2026-09-01");
selectedDates.add("2026-09-02");
selectedDates.add("2026-09-01");

console.log(selectedDates.size); // 2
```

`add()` 返回当前 `Set`，所以可以连续调用。

```js
selectedDates
  .add("2026-09-03")
  .add("2026-09-04");
```

#### 3.2 has：判断值是否存在

`has(value)` 返回布尔值，适合在添加数据前进行重复检查。

```js
const date = "2026-09-01";

if (selectedDates.has(date)) {
  console.log("该日期已经选择");
} else {
  selectedDates.add(date);
}
```

#### 3.3 delete：删除一个值

`delete(value)` 删除指定值。成功删除返回 `true`，不存在则返回 `false`。

```js
const deleted = selectedDates.delete("2026-09-02");
console.log(deleted);
```

#### 3.4 clear：清空集合

`clear()` 删除集合中的全部值。

```js
selectedDates.clear();
console.log(selectedDates.size); // 0
```

---

### 4. Set 如何判断重复

对于字符串、数字等原始值，值相同就会被认为重复。

```js
const values = new Set([1, 1, "1", true, true, NaN, NaN]);

console.log(values);
// 1、"1"、true、NaN 各保留一份
```

这里需要注意：

- 数字 `1` 和字符串 `"1"` 类型不同，不是同一个值；
- `Set` 会把多个 `NaN` 视为同一个值；
- 对象按照“是否为同一个对象”判断，而不是比较对象内容。

```js
const applicationA = { id: 101 };
const applicationB = { id: 101 };

const applications = new Set([
  applicationA,
  applicationA,
  applicationB
]);

console.log(applications.size); // 2
```

虽然两个对象内容一样，但 `applicationA` 和 `applicationB` 是两个不同对象。

> 如果要根据对象的 `id` 去重，通常先提取 `id`，或使用 `Map` 以 `id` 作为键。不能只把对象放入 `Set` 就期待按内容去重。

---

### 5. 遍历与转换 Set

#### 5.1 使用 for...of 遍历

`for...of` 会按照加入顺序依次取得集合中的值。

```js
for (const date of selectedDates) {
  console.log(date);
}
```

#### 5.2 使用 forEach 遍历

`forEach(callback)` 会对每个值执行一次回调函数。

```js
selectedDates.forEach((date) => {
  console.log(`已选择：${date}`);
});
```

Set 的 `forEach` 回调会收到 `(value, value, set)`。前两个参数相同是为了与 `Map` 的回调形式保持一致。初学阶段通常只使用第一个参数。

#### 5.3 Set 转数组

展开语法和 `Array.from()` 都可以把 Set 转成数组。

```js
const dateArray1 = [...selectedDates];
const dateArray2 = Array.from(selectedDates);

console.log(dateArray1);
console.log(dateArray2);
```

#### 5.4 使用 Set 为数组去重

```js
const dates = [
  "2026-09-01",
  "2026-09-02",
  "2026-09-01"
];

const uniqueDates = [...new Set(dates)];

console.log(uniqueDates);
// ["2026-09-01", "2026-09-02"]
```

这是项目中最常见的 Set 用法之一。

---

## 第二部分：Map

### 6. Map 是什么

`Map` 用于保存“键和值”的对应关系。

```js
const statusTextMap = new Map();
```

例如，后端返回状态代码，页面需要显示中文文本：

```js
statusTextMap.set("draft", "草稿");
statusTextMap.set("submitted", "已提交");
statusTextMap.set("approved", "已通过");
```

读取时把状态代码作为键：

```js
console.log(statusTextMap.get("approved")); // 已通过
```

#### 6.1 创建时直接传入数据

`new Map(entries)` 可以接收由“键值对”组成的可迭代对象。最常见的是二维数组。

```js
const statusTextMap = new Map([
  ["draft", "草稿"],
  ["submitted", "已提交"],
  ["approved", "已通过"]
]);
```

每个内部数组都必须表示 `[键, 值]`。

#### 6.2 Map 的主要特点

- 一个键对应一个值；
- 键不会重复，再次设置同一个键会覆盖旧值；
- 键可以是字符串、数字、对象、函数等任意值；
- 按照键首次加入的先后顺序遍历；
- 使用 `size` 获取键值对数量。

---

### 7. Map 的常用属性和方法

| 写法 | 参数 | 返回值 | 作用 |
| --- | --- | --- | --- |
| `map.size` | 无 | 键值对数量 | 获取 Map 大小 |
| `map.set(key, value)` | 键、值 | 当前 Map | 新增或更新数据 |
| `map.get(key)` | 键 | 对应值或 `undefined` | 读取数据 |
| `map.has(key)` | 键 | 布尔值 | 判断键是否存在 |
| `map.delete(key)` | 键 | 布尔值 | 删除指定键值对 |
| `map.clear()` | 无 | `undefined` | 清空全部键值对 |

#### 7.1 set：新增或更新

```js
statusTextMap.set("rejected", "已驳回");
statusTextMap.set("draft", "编辑中");

console.log(statusTextMap.get("draft")); // 编辑中
```

`set()` 返回当前 Map，也可以连续调用。

#### 7.2 get：根据键读取值

```js
const label = statusTextMap.get("submitted");
console.log(label); // 已提交
```

找不到键时，`get()` 返回 `undefined`。

```js
console.log(statusTextMap.get("unknown")); // undefined
```

但是，值本身也可能被设置为 `undefined`。因此需要准确判断“键是否存在”时应使用 `has()`。

```js
statusTextMap.set("pending", undefined);

console.log(statusTextMap.get("pending")); // undefined
console.log(statusTextMap.has("pending")); // true
```

#### 7.3 delete 和 clear

```js
statusTextMap.delete("rejected");
statusTextMap.clear();
```

---

### 8. 遍历 Map

#### 8.1 同时取得键和值

```js
const statusTextMap = new Map([
  ["draft", "草稿"],
  ["submitted", "已提交"],
  ["approved", "已通过"]
]);

for (const [code, label] of statusTextMap) {
  console.log(`${code}：${label}`);
}
```

直接遍历 Map 时，每次取得一个 `[key, value]`。

#### 8.2 keys、values 和 entries

| 方法 | 作用 |
| --- | --- |
| `map.keys()` | 返回全部键的迭代器 |
| `map.values()` | 返回全部值的迭代器 |
| `map.entries()` | 返回全部 `[键, 值]` 的迭代器 |

```js
for (const code of statusTextMap.keys()) {
  console.log(code);
}

for (const label of statusTextMap.values()) {
  console.log(label);
}

for (const [code, label] of statusTextMap.entries()) {
  console.log(code, label);
}
```

#### 8.3 forEach

Map 的 `forEach(callback)` 回调参数顺序是 `(value, key, map)`，先值后键。

```js
statusTextMap.forEach((label, code) => {
  console.log(`${code}：${label}`);
});
```

不要误写成数组回调中常见的 `(item, index)`。

---

### 9. 普通对象和 Map 如何选择

| 比较项 | 普通对象 | Map |
| --- | --- | --- |
| 常见用途 | 表示一条有固定字段的数据 | 保存动态的键值对应关系 |
| 键类型 | 字符串或 Symbol | 任意类型 |
| 获取数量 | `Object.keys(obj).length` | `map.size` |
| 判断键 | `Object.hasOwn(obj, key)` | `map.has(key)` |
| 遍历 | `Object.keys/values/entries` | 可直接 `for...of` |
| JSON 转换 | 可以直接 `JSON.stringify` | 需要先转换 |
| 项目示例 | 一条申请记录 | 状态代码与文本的对应表 |

```js
const application = {
  id: 101,
  applicantName: "山田太郎",
  status: "submitted"
};

const statusTextMap = new Map([
  ["draft", "草稿"],
  ["submitted", "已提交"]
]);
```

判断原则：

- 数据有固定字段，通常使用对象；
- 键值对会动态增删，或键不只是字符串，优先考虑 Map；
- 需要直接转换成 JSON 发送给后端时，普通对象更方便。

#### 9.1 Map 与对象互相转换

`Object.entries(object)` 把对象转换成键值对数组，可用于创建 Map。

```js
const statusObject = {
  draft: "草稿",
  submitted: "已提交"
};

const statusMap = new Map(Object.entries(statusObject));
```

`Object.fromEntries(iterable)` 把键值对转换成对象。

```js
const convertedObject = Object.fromEntries(statusMap);
console.log(convertedObject);
```

这种转换最适合键为字符串或 Symbol 的 Map。如果 Map 使用对象作为键，转换成普通对象会失去原来的键语义。

---

### 10. 使用对象作为 Map 的键

普通对象会把大多数属性键转换成字符串，而 Map 可以直接使用对象作为键。

```js
const applicantA = { id: 1, name: "山田太郎" };
const applicantB = { id: 2, name: "佐藤花子" };

const applicationCountMap = new Map();

applicationCountMap.set(applicantA, 2);
applicationCountMap.set(applicantB, 1);

console.log(applicationCountMap.get(applicantA)); // 2
```

只有同一个对象才能取到对应值。

```js
console.log(applicationCountMap.get({ id: 1, name: "山田太郎" }));
// undefined
```

新写的对象虽然内容相同，但不是原来的 `applicantA`。

---

## 第三部分：弱引用集合

> 到这里已经完成本章必须掌握的`Set`和`Map`。第三至第五部分用于阅读框架或既有代码，可以在完成核心练习后再学习。

### 11. WeakSet 和 WeakMap

`WeakSet` 和 `WeakMap` 是与对象生命周期有关的特殊集合，主要用于框架、库和底层工具代码。

初学阶段只需要理解：

- 本章示例以对象作为其中的成员或键；
- 它们不会阻止对象被垃圾回收；
- 不能遍历；
- 没有 `size`；
- 不提供 `clear()`。

#### 11.1 WeakSet

`WeakSet` 常用于记录“某个对象是否处理过”。

```js
const validatedApplications = new WeakSet();
const application = { id: 101 };

validatedApplications.add(application);

console.log(validatedApplications.has(application)); // true
validatedApplications.delete(application);
```

#### 11.2 WeakMap

`WeakMap` 常用于给对象附加额外信息，而不希望额外信息影响对象回收。

```js
const metadataMap = new WeakMap();
const formElement = document.querySelector("#application-form");

metadataMap.set(formElement, {
  dirty: false,
  lastValidatedAt: null
});

console.log(metadataMap.get(formElement));
```

> 业务代码中如果需要遍历全部数据或显示数量，应使用 `Set` 或 `Map`，不要使用弱引用集合。

---

## 第四部分：Symbol

### 12. Symbol 是什么

`Symbol` 是 JavaScript 的一种原始数据类型。每次调用 `Symbol()` 都会创建一个唯一值。

```js
const internalIdA = Symbol("internalId");
const internalIdB = Symbol("internalId");

console.log(internalIdA === internalIdB); // false
```

括号中的 `"internalId"` 只是描述，方便调试，不决定两个 Symbol 是否相同。

#### 12.1 使用 Symbol 作为属性键

```js
const internalId = Symbol("internalId");

const application = {
  id: 101,
  applicantName: "山田太郎",
  [internalId]: "internal-001"
};

console.log(application[internalId]);
```

计算属性名 `[internalId]` 表示使用 Symbol 值作为键。

普通的 `Object.keys()` 不会列出 Symbol 键：

```js
console.log(Object.keys(application));
// ["id", "applicantName"]

console.log(Object.getOwnPropertySymbols(application));
// [Symbol(internalId)]
```

`Object.getOwnPropertySymbols(object)` 返回对象自身的全部 Symbol 属性键。

> Symbol 属性并不是真正的私有属性。只要取得对应 Symbol，或者调用反射 API，仍然可以读取它。

#### 12.2 全局 Symbol 注册表

`Symbol.for(key)` 会在全局 Symbol 注册表中查找或创建 Symbol。同一个键会得到同一个 Symbol。

```js
const first = Symbol.for("application.status");
const second = Symbol.for("application.status");

console.log(first === second); // true
console.log(Symbol.keyFor(first)); // application.status
```

`Symbol.keyFor(symbol)` 用于取得通过 `Symbol.for()` 注册的键。普通 `Symbol()` 创建的值不在该注册表中。

#### 12.3 Symbol 的常见注意事项

```js
const token = Symbol("token");

console.log(String(token));       // Symbol(token)
console.log(token.description);   // token
```

- 不要直接使用 `"" + token` 拼接字符串，应使用 `String(token)`；
- `JSON.stringify()` 会忽略对象中的 Symbol 键；
- 业务数据需要发送给后端时，不应依赖 Symbol 属性；
- JavaScript 还内置了 `Symbol.iterator` 等“知名 Symbol”，用于定义语言级行为。初学阶段能够识别即可。

---

## 第五部分：Proxy 与 Reflect

### 13. Proxy 是什么

`Proxy` 可以在外部读写一个对象时进行拦截。

```js
const proxy = new Proxy(target, handler);
```

| 部分 | 作用 |
| --- | --- |
| `target` | 原始目标对象 |
| `handler` | 保存拦截方法的对象 |
| `proxy` | 外部实际使用的代理对象 |

例如，读取申请记录中不存在的属性时给出提示：

```js
const application = {
  id: 101,
  applicantName: "山田太郎"
};

const applicationProxy = new Proxy(application, {
  get(target, property, receiver) {
    if (!(property in target)) {
      console.warn(`属性 ${String(property)} 不存在`);
    }

    return Reflect.get(target, property, receiver);
  }
});

console.log(applicationProxy.applicantName);
console.log(applicationProxy.status);
```

`get` 是读取属性时执行的拦截方法，也称为 trap（捕获器）。

#### 13.1 常见拦截方法

| 拦截方法 | 触发时机 |
| --- | --- |
| `get` | 读取属性 |
| `set` | 设置属性 |
| `has` | 使用 `in` 判断属性 |
| `deleteProperty` | 使用 `delete` 删除属性 |
| `ownKeys` | 获取对象自身的属性键 |

初学阶段重点理解 `get` 和 `set`。

#### 13.2 使用 set 校验赋值

```js
const allowedStatuses = new Set([
  "draft",
  "submitted",
  "approved",
  "rejected"
]);

const application = {
  id: 101,
  status: "draft"
};

const applicationProxy = new Proxy(application, {
  set(target, property, value, receiver) {
    if (property === "status" && !allowedStatuses.has(value)) {
      throw new RangeError(`不支持的状态：${value}`);
    }

    return Reflect.set(target, property, value, receiver);
  }
});

applicationProxy.status = "submitted";
console.log(application.status); // submitted
```

`set` 捕获器必须返回布尔值，表示赋值是否成功。`Reflect.set()` 正好返回布尔值，因此适合直接返回。

```js
// applicationProxy.status = "unknown";
// RangeError: 不支持的状态：unknown
```

故意触发错误的代码已注释，取消注释后可以观察错误。

#### 13.3 必须通过代理访问

```js
application.status = "unknown";
```

如果直接修改原始对象 `application`，不会经过 `applicationProxy` 的拦截逻辑。

因此创建 Proxy 后，外部代码应统一使用代理对象。Proxy 不是复制数据，而是包在原对象外面的一层访问控制。

---

### 14. Reflect 是什么

`Reflect` 是 JavaScript 提供的内置对象，集中提供对象的底层操作方法。

| 方法 | 作用 | 典型对应操作 |
| --- | --- | --- |
| `Reflect.get()` | 读取属性 | `object[key]` |
| `Reflect.set()` | 设置属性 | `object[key] = value` |
| `Reflect.has()` | 判断属性 | `key in object` |
| `Reflect.deleteProperty()` | 删除属性 | `delete object[key]` |
| `Reflect.ownKeys()` | 获取自身全部属性键 | 字符串键和 Symbol 键 |

```js
const application = {
  id: 101,
  status: "draft"
};

console.log(Reflect.get(application, "status"));

const updated = Reflect.set(application, "status", "submitted");
console.log(updated); // true

console.log(Reflect.has(application, "id")); // true
console.log(Reflect.ownKeys(application));   // ["id", "status"]
```

Reflect 方法通常会返回操作结果，便于统一处理成功和失败。

#### 14.1 为什么 Proxy 中常配合 Reflect

在 Proxy 捕获器中，通常先执行自定义逻辑，再通过 Reflect 完成原本的对象操作。

```js
const applicationProxy = new Proxy(application, {
  get(target, property, receiver) {
    console.log(`读取属性：${String(property)}`);
    return Reflect.get(target, property, receiver);
  },

  set(target, property, value, receiver) {
    console.log(`修改 ${String(property)} 为 ${String(value)}`);
    return Reflect.set(target, property, value, receiver);
  }
});
```

这里的 `receiver` 表示本次操作实际作用到的接收对象。使用 Reflect 并传递 `receiver`，能够更完整地保留 getter、setter 和继承等语言行为。

---

### 15. 与前端框架的关系

Vue 等框架需要知道数据什么时候被读取、什么时候被修改。Proxy 可以拦截这些操作，因此可以作为响应式系统的基础之一。

下面只是帮助理解的简化流程：

```text
读取 proxy.status
        ↓
记录“当前页面使用了 status”
        ↓
修改 proxy.status
        ↓
通知使用 status 的页面重新更新
```

真实框架还要处理依赖收集、批量更新、嵌套对象、数组、缓存等复杂问题。

学习本章的目标不是手写 Vue，而是以后看到 Proxy、Reflect、Set、Map 时，能够理解它们为什么会出现在框架源码或工具代码中。

---

## 第六部分：练习、排错与总结

### 16. 常见错误

#### 16.1 把 Set 当数组使用

```js
const dates = new Set(["2026-09-01"]);

console.log(dates[0]); // undefined
```

需要数组下标时，先转换：

```js
const dateArray = [...dates];
console.log(dateArray[0]);
```

#### 16.2 认为 Set 会按照对象内容去重

两个分别创建的对象不是同一个对象。按 `id` 去重时可以使用 Map：

```js
const applications = [
  { id: 101, name: "山田太郎" },
  { id: 101, name: "山田太郎" },
  { id: 102, name: "佐藤花子" }
];

const applicationMap = new Map(
  applications.map((item) => [item.id, item])
);

const uniqueApplications = [...applicationMap.values()];
console.log(uniqueApplications);
```

#### 16.3 混淆 Map 的 get 和对象属性访问

```js
const statusMap = new Map([["draft", "草稿"]]);

console.log(statusMap.get("draft"));
// console.log(statusMap.draft); // 错误的读取方式
```

#### 16.4 忘记返回 Proxy 的 set 结果

```js
const proxy = new Proxy({}, {
  set(target, property, value, receiver) {
    return Reflect.set(target, property, value, receiver);
  }
});

proxy.status = "draft";
```

严格模式下，`set` 捕获器返回假值可能导致 `TypeError`。

---

### 17. 本章练习

#### 练习 1：Set 管理已选择日期

要求：

1. 创建 `selectedDates`；
2. 添加三个日期，其中一个日期重复；
3. 输出实际数量；
4. 判断指定日期是否存在；
5. 删除一个日期；
6. 使用 `for...of` 输出剩余日期。

#### 练习 2：Map 管理状态文本

要求：

1. 创建状态 Map；
2. 至少保存 `draft`、`submitted`、`approved`；
3. 根据 `"submitted"` 取得显示文本；
4. 增加 `"rejected"`；
5. 遍历输出所有状态代码和显示文本。

#### 练习 3：组合使用

给定申请数组：

```js
const applications = [
  { id: 101, date: "2026-09-01", status: "submitted" },
  { id: 102, date: "2026-09-01", status: "approved" },
  { id: 103, date: "2026-09-02", status: "draft" }
];
```

要求：

1. 使用 Set 得到所有不重复日期；
2. 使用 Map 保存状态代码和显示文本；
3. 输出每条申请的 `id`、日期和状态文本；
4. 遇到未知状态时显示 `"未知状态"`。

#### 扩展练习：状态校验代理

使用 Proxy 包装一条申请记录：

- 只允许把 `status` 修改为规定状态；
- 不合法时抛出 `RangeError`；
- 使用 Reflect 完成最终赋值。

---

### 18. 本章小结

| 知识 | 主要作用 | 掌握要求 |
| --- | --- | --- |
| Set | 保存不重复值 | 必须会使用 |
| Map | 保存动态键值对应关系 | 必须会使用 |
| WeakSet / WeakMap | 保存与对象生命周期有关的弱引用关系 | 理解用途 |
| Symbol | 创建唯一值或特殊属性键 | 理解用途 |
| Proxy | 拦截对象操作 | 能阅读基础代码 |
| Reflect | 统一执行对象底层操作 | 能与 Proxy 配合阅读 |

下一章会把申请管理代码拆分成多个 ES 模块，使数据、校验、存储和页面逻辑各自承担清晰职责。
