# 第 14 章 异步结果与外部数据

请求的发送方法已经在 JavaScript 课程中讲过，本章只补充 TypeScript 最关键的部分：异步函数的返回类型，以及为什么外部数据必须先校验再使用。

## 1. 用 Promise<T> 表示异步结果

`Promise<T>`中的`T`表示异步操作成功后得到的数据类型。

```ts
async function getCount(): Promise<number> {
  return 3;
}

async function showCount(): Promise<void> {
  const count = await getCount();
  console.log(count); // 3
}

showCount();
```

- `getCount()`最终得到`number`，所以返回类型是`Promise<number>`。
- `showCount()`没有返回业务数据，所以是`Promise<void>`。
- `await`取得 Promise 成功后的值；这里`count`被推断为`number`。

失败原因不写在`Promise<T>`的`T`中。捕获到的错误仍应按`unknown`处理。

## 2. 给异步业务函数写清返回类型

```ts
interface User {
  id: number;
  name: string;
}

async function findUsers(): Promise<User[]> {
  return [
    { id: 1, name: "田中" },
    { id: 2, name: "佐藤" }
  ];
}

async function showFirstUser(): Promise<void> {
  const users = await findUsers();
  console.log(users[0]?.name ?? "用户不存在"); // 田中
}

showFirstUser();
```

显式写`Promise<User[]>`可以让函数的使用者知道成功时得到用户数组，也能防止函数内部误返回其他结构。

## 3. 接口返回值为什么先是 unknown

下面的断言看起来方便，但不会检查实际数据：

```ts
interface User {
  id: number;
  name: string;
}

const raw: unknown = { id: "错误", name: "田中" };
const user = raw as User;
console.log(user.id); // 运行时仍然是字符串“错误”
```

`as User`只是在编译阶段告诉 TypeScript“把它当作 User”，不会转换或验证数据。接口响应、浏览器存储和用户输入都来自程序外部，不能只靠断言保证安全。

## 4. 使用类型守卫校验外部数据

### 4.1 校验一条数据

```ts
interface User {
  id: number;
  name: string;
}

function isUser(value: unknown): value is User {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  return "id" in value && typeof value.id === "number"
    && "name" in value && typeof value.name === "string";
}

const raw: unknown = { id: 1, name: "田中" };

if (isUser(raw)) {
  console.log(raw.name); // 田中；这里已经收窄为 User
} else {
  console.log("用户数据格式不正确");
}
```

`value is User`表示：函数返回`true`时，参数可以安全地收窄为`User`。函数内部仍必须逐项检查，不能只返回固定的`true`。

### 4.2 校验数组

```ts
interface User {
  id: number;
  name: string;
}

function isUser(value: unknown): value is User {
  return typeof value === "object" && value !== null
    && "id" in value && typeof value.id === "number"
    && "name" in value && typeof value.name === "string";
}

function isUserArray(value: unknown): value is User[] {
  return Array.isArray(value) && value.every(isUser);
}

const raw: unknown = [{ id: 1, name: "田中" }];

if (isUserArray(raw)) {
  console.log(raw[0]?.name); // 田中
}
```

`Array.isArray()`先确认外层是数组，`every()`再确认每一项都通过`isUser()`。

实际项目的数据结构复杂时，团队也可能使用专门的校验库。本课程先掌握`unknown → 校验 → 收窄`这一原则，具体库在 React/Vue 项目中按选型学习。

## 5. 处理异步错误

```ts
async function runTask(): Promise<void> {
  try {
    throw new Error("读取失败");
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "未知错误";
    console.log(message); // 读取失败
  }
}

runTask();
```

`catch`中的值不一定是`Error`对象。先使用`instanceof Error`收窄，再读取`message`，比直接写`error.message`更安全。

请求地址、HTTP 方法、请求头、请求体、文件上传、取消和 Axios 的用法属于 JavaScript HTTP 课程或框架项目的请求层，本章不重复展开。

## 6. 练习

1. 定义`Product`，包含数字编号、名称和价格。
2. 编写返回`Promise<Product[]>`的异步函数，并用`await`读取结果。
3. 将一份数据保存为`unknown`，编写`isProduct()`检查所有字段。
4. 构造字段错误的数据，确认程序进入校验失败分支。

完成后，应能解释为什么接口响应不能直接写成某个业务类型，以及`Promise<Product[]>`中的`Product[]`代表什么。
