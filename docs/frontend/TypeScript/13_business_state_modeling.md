# 第 13 章 组件数据与界面状态建模

React 和 Vue 组件通常需要同时管理接口数据、查询条件、选中项以及加载和保存状态。本章把前面学过的对象、联合类型、泛型和收窄组合起来，练习如何在写组件前先把这些数据表达清楚。

## 1. 把画面需要的数据写成类型

假设商品列表要显示编号、名称、价格和库存状态：

```ts
type StockStatus = "inStock" | "soldOut";

interface Product {
  readonly id: number;
  name: string;
  price: number;
  stockStatus: StockStatus;
}

const product: Product = {
  id: 1,
  name: "キーボード",
  price: 5000,
  stockStatus: "inStock"
};

console.log(product.name); // キーボード
```

这种类型以后可以用于组件接收的数据、列表项和接口返回值。`readonly id`表示代码不能重新赋值，字面量联合限制了状态的可选范围。

类型只约束代码中的写法，不能自动判断价格是否合理，也不能证明接口真的返回了这些字段。

## 2. 可选数据要明确处理

商品说明可能不存在，可以使用可选属性：

```ts
interface Product {
  id: number;
  name: string;
  description?: string;
}

function getDescription(product: Product): string {
  return product.description ?? "暂无说明";
}

console.log(getDescription({ id: 1, name: "キーボード" })); // 暂无说明
```

`description?: string`表示属性可以不存在。读取时结果可能是`string | undefined`，所以示例用空值合并运算符`??`提供默认文本。

不要为了省事把全部字段都改成可选。只有业务上确实允许缺少的字段才使用`?`。

### 2.1 查询条件也是独立的数据结构

查询表单不应直接复用商品本身的类型，因为两者字段和“必填”规则不同：

```ts
type StockStatus = "inStock" | "soldOut";

interface SearchCondition {
  keyword: string;
  stockStatus: StockStatus | "all";
  page: number;
}

const condition: SearchCondition = {
  keyword: "keyboard",
  stockStatus: "all",
  page: 1,
};

console.log(condition.keyword); // keyboard
```

`"all"`是画面查询条件使用的值，不一定是商品数据本身允许的库存状态。

### 2.2 使用泛型表示分页结果

```ts
interface Page<T> {
  items: T[];
  total: number;
  page: number;
}

interface Product {
  id: number;
  name: string;
}

const result: Page<Product> = {
  items: [{ id: 1, name: "キーボード" }],
  total: 1,
  page: 1,
};

console.log(result.items[0]?.name); // キーボード
```

`Page<T>`统一分页字段，`T`决定当前页面中的业务数据类型。

## 3. 用可辨识联合表示界面状态

### 3.1 为什么不使用互不相关的变量

如果分别保存`isLoading`、`data`和`error`，可能出现“正在加载，同时又有错误”等矛盾组合。可以把每一种合法状态写成一个对象：

```ts
type LoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; products: Product[] }
  | { status: "error"; message: string };
```

共同的`status`属性用于区分成员，这种结构叫**可辨识联合**。

### 3.2 根据状态安全地读取字段

```ts
interface Product {
  id: number;
  name: string;
}

type LoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; products: Product[] }
  | { status: "error"; message: string };

function getMessage(state: LoadState): string {
  switch (state.status) {
    case "idle": return "尚未读取";
    case "loading": return "读取中";
    case "success": return `${state.products.length}件`;
    case "error": return state.message;
  }
}

console.log(getMessage({ status: "success", products: [] })); // 0件
```

判断`status`后，TypeScript 会自动收窄类型。只有成功状态能读取`products`，只有失败状态能读取`message`。

## 4. 选中项与保存状态

详情区域在尚未选择商品时没有数据，应明确包含`null`：

```ts
interface Product {
  id: number;
  name: string;
}

let selectedProduct: Product | null = null;
selectedProduct = { id: 1, name: "キーボード" };

console.log(selectedProduct.name); // キーボード
```

保存过程也可以只允许规定的状态：

```ts
type SaveState = "idle" | "saving" | "success" | "error";

let saveState: SaveState = "idle";
saveState = "saving";
console.log(saveState); // saving
```

`Product | null`解决“有没有选中数据”，`SaveState`解决“保存进行到哪一步”，不要用空对象或任意字符串代替。

## 5. 【项目代码阅读】用 never 检查遗漏状态

```ts
type SaveState = "idle" | "saving" | "success" | "error";

function assertNever(value: never): never {
  throw new Error(`未处理的状态: ${String(value)}`);
}

function getSaveMessage(state: SaveState): string {
  switch (state) {
    case "idle": return "尚未保存";
    case "saving": return "保存中";
    case "success": return "保存成功";
    case "error": return "保存失败";
    default: return assertNever(state);
  }
}

console.log(getSaveMessage("saving")); // 保存中
```

所有成员都处理完后，`default`中的`state`会收窄为`never`。将来新增成员却忘记增加`case`时，类型检查会在`assertNever(state)`处报错。

简单的两三个状态不必机械地加入`assertNever`；当遗漏分支可能造成界面错误时再使用。

## 6. 把一张列表画面的类型放在一起

下面只汇总类型和初始状态，不涉及 React 或 Vue API：

```ts
type StockStatus = "inStock" | "soldOut";
type SaveState = "idle" | "saving" | "success" | "error";

interface Product {
  readonly id: number;
  name: string;
  price: number;
  stockStatus: StockStatus;
}

interface SearchCondition {
  keyword: string;
  stockStatus: StockStatus | "all";
  page: number;
}

interface Page<T> {
  items: T[];
  total: number;
  page: number;
}

type LoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; pageData: Page<Product> }
  | { status: "error"; message: string };

const searchCondition: SearchCondition = {
  keyword: "",
  stockStatus: "all",
  page: 1,
};
let selectedProduct: Product | null = null;
let loadState: LoadState = { status: "idle" };
let saveState: SaveState = "idle";

console.log(searchCondition, selectedProduct, loadState, saveState);
```

这些类型分别负责查询、列表结果、当前选中项、读取过程和保存过程。后续框架课程会把它们放入组件状态中，本章只要求能正确设计和读懂类型。

## 7. 练习

1. 定义`User`，包含编号、姓名和`"active" | "inactive"`状态。
2. 定义用户列表的`idle`、`loading`、`success`、`error`四种状态。
3. 编写`getMessage()`，成功时返回用户数量，失败时返回错误消息。
4. 给联合类型新增`"empty"`状态，观察遗漏分支，再补充处理。
5. 为用户列表增加查询条件、`Page<User>`、`User | null`选中项和保存状态。
