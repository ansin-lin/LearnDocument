# 第 4 章 条件渲染与列表

## 本章目标

- 【必须掌握】根据条件决定显示、隐藏或切换 UI。
- 【必须掌握】区分提前返回、`&&` 和三元表达式的使用场景。
- 【必须掌握】使用 `map()` 把数组转换成组件列表。
- 【必须掌握】为列表选择稳定且唯一的 `key`。
- 【必须掌握】处理空数据，并区分过滤与列表渲染。

## 1. 什么是条件渲染

真实页面不会永远显示同一种内容。例如：

- 没有员工数据时显示空数据提示。
- 用户有新增权限时才显示新增按钮。
- 员工在职时显示“在职”，离职时显示“离职”。
- 数据读取中显示 Loading，失败时显示错误信息。

React 不提供专用的 `v-if` 指令，而是使用 JavaScript 的 `if`、三元表达式和逻辑运算符决定返回哪些 JSX。

```text
条件
 ├─ 成立   → 返回一组 JSX
 └─ 不成立 → 返回另一组 JSX，或者什么都不返回
```

本章先使用固定变量和 Props 观察显示结果。第五章学习 State 后，用户操作将能够改变条件并触发页面更新。

## 2. 使用 if 提前返回

当整个组件只能处于一种主要状态时，优先使用 `if` 提前返回：

```jsx
function EmployeeResult({ loading, error, employees }) {
  if (loading) {
    return <p role="status">员工数据读取中...</p>;
  }

  if (error) {
    return <p role="alert">{error}</p>;
  }

  if (employees.length === 0) {
    return <p>没有符合条件的员工。</p>;
  }

  return <p>读取成功，共 {employees.length} 人。</p>;
}
```

React 从上到下执行组件函数。遇到 `return` 后，本次函数执行立即结束，后面的 JSX 不再计算。

```text
loading 为 true  → 返回 Loading
否则 error 有值 → 返回 Error
否则数组为空    → 返回 Empty
否则            → 返回 Success
```

这种写法适合 loading、error、empty、success 等互斥页面状态。它比把多个三元表达式嵌套在同一个 JSX 中更容易阅读。

## 3. 使用 && 显示或隐藏局部内容

只有一个局部元素需要按条件显示时，可以使用逻辑与 `&&`：

```jsx
function EmployeeActions({ canCreate }) {
  return (
    <div>
      <h2>员工操作</h2>
      {canCreate && <button type="button">新增员工</button>}
    </div>
  );
}
```

- `canCreate` 为 `true`：表达式结果是 `<button>`，React 显示按钮。
- `canCreate` 为 `false`：表达式结果是 `false`，React 不显示内容。

`false`、`null` 和 `undefined` 通常不会生成可见内容，因此组件也可以明确返回 `null`：

```jsx
function AdminNotice({ isAdmin }) {
  if (!isAdmin) return null;

  return <p>管理员可以维护员工数据。</p>;
}
```

### 3.1 注意数字 0

下面的写法在 `count` 为 `0` 时可能把数字 `0` 显示在页面上：

```jsx
// 不推荐
{count && <p>共有 {count} 人</p>}
```

应把左侧写成明确的布尔条件：

```jsx
{count > 0 && <p>共有 {count} 人</p>}
```

## 4. 使用三元表达式二选一

必须在两种结果中选择一种时，使用三元表达式：

```jsx
function StatusLabel({ status }) {
  return (
    <span>
      {status === 'ACTIVE' ? '在职' : '离职'}
    </span>
  );
}
```

三元表达式的结构是：

```text
条件 ? 条件成立时的结果 : 条件不成立时的结果
```

也可以在两组 JSX 之间选择：

```jsx
function LoginMessage({ loggedIn }) {
  return loggedIn
    ? <p>欢迎回来。</p>
    : <p>请先登录。</p>;
}
```

不要嵌套多层三元表达式：

```jsx
// 难以阅读
{loading ? <Loading /> : error ? <Error /> : empty ? <Empty /> : <List />}
```

遇到多个互斥状态时，改用 `if` 提前返回。

## 5. 三种条件写法如何选择

| 场景 | 推荐写法 | 示例 |
| --- | --- | --- |
| 整个组件处于互斥状态 | `if` 提前返回 | loading / error / empty / success |
| 条件成立才显示局部内容 | `&&` | 有权限才显示按钮 |
| 两种内容必须选择一种 | 三元表达式 | 在职 / 离职 |
| 完全不显示组件 | 返回 `null` | 非管理员不显示通知 |

选择标准是让业务条件容易读懂，而不是尽量把所有逻辑缩成一行。

## 6. 从数组生成列表

员工数据通常来自数组：

```js
const employees = [
  { id: 1001, name: '田中太郎', department: '営業部' },
  { id: 1002, name: '佐藤花子', department: '開発部' },
  { id: 1003, name: '鈴木一郎', department: '人事部' },
];
```

JavaScript 的 `map()` 会遍历数组，并返回一个长度相同的新数组：

```js
const names = employees.map((employee) => employee.name);
```

在 React 中，可以让 `map()` 每次返回 JSX：

```jsx
function EmployeeList({ employees }) {
  return (
    <ul>
      {employees.map((employee) => (
        <li key={employee.id}>
          {employee.name} / {employee.department}
        </li>
      ))}
    </ul>
  );
}
```

执行过程如下：

```text
employees 数组
      ↓ map()
每个 employee 转换成一个 li
      ↓
JSX 元素数组
      ↓ React 渲染
页面列表
```

### 6.1 map() 箭头函数的两种返回写法

圆括号写法会直接返回 JSX：

```jsx
employees.map((employee) => (
  <li key={employee.id}>{employee.name}</li>
))
```

花括号写法必须明确写 `return`：

```jsx
employees.map((employee) => {
  return <li key={employee.id}>{employee.name}</li>;
})
```

下面忘记了 `return`，每次回调都得到 `undefined`，页面不会显示列表：

```jsx
// 错误示例
employees.map((employee) => {
  <li key={employee.id}>{employee.name}</li>;
})
```

## 7. 使用组件渲染列表

第三章已经创建 `EmployeeCard`。列表可以让 `map()` 返回组件：

```jsx
function EmployeeList({ employees }) {
  return (
    <div className="employee-list">
      {employees.map((employee) => (
        <EmployeeCard
          key={employee.id}
          employee={employee}
        />
      ))}
    </div>
  );
}
```

`EmployeeList` 负责遍历数组，`EmployeeCard` 负责显示一名员工。不要让两个组件都重复执行相同的 `map()`。

使用语义列表时，可以由列表组件负责 `ul/li`：

```jsx
function EmployeeList({ employees }) {
  return (
    <ul>
      {employees.map((employee) => (
        <li key={employee.id}>
          <EmployeeCard employee={employee} />
        </li>
      ))}
    </ul>
  );
}
```

`key` 应放在 `map()` 直接返回的最外层元素上。本例直接返回的是 `li`，因此 `key` 写在 `li` 上，而不是写在里面的 `EmployeeCard` 上。

## 8. key 是什么

`key` 帮助 React 在同一个父节点下识别每一条数据。当列表重新排序、插入或删除时，React 使用 `key` 判断“这是原来的哪一项”。

```jsx
{employees.map((employee) => (
  <EmployeeCard key={employee.id} employee={employee} />
))}
```

合适的 `key` 应满足：

- 在当前列表兄弟元素之间唯一。
- 同一条业务数据在多次渲染中保持稳定。
- 来自数据本身，例如数据库 ID、员工编号或稳定业务编码。

### 8.1 不推荐使用数组索引

```jsx
employees.map((employee, index) => (
  <EmployeeCard key={index} employee={employee} />
))
```

当列表永远不会新增、删除、排序时，索引可能暂时工作。但员工列表会变化，删除第一项后，后面的索引全部改变，React 可能把旧组件状态对应到另一条数据。

### 8.2 不要使用随机值

```jsx
// 错误示例
<EmployeeCard key={Math.random()} employee={employee} />
```

随机值每次渲染都会变化。React 会把所有列表项当成全新组件，造成不必要的重建，并可能丢失输入状态。

### 8.3 key 不是普通 Prop

React 自己使用 `key`，不会自动把它放进组件 Props：

```jsx
<EmployeeCard
  key={employee.id}
  employeeId={employee.id}
  employee={employee}
/>
```

如果组件内部需要 ID，应通过 `employee.id` 或单独的 `employeeId` Prop 传入。

不同列表中可以重复使用相同 ID；`key` 只需要在当前同级列表中唯一，不要求整个应用全局唯一。

## 9. 空数据与过滤

空数组执行 `map()` 不会报错，但页面会什么都不显示。业务页面应明确告诉用户没有数据：

```jsx
function EmployeeList({ employees }) {
  if (employees.length === 0) {
    return <p>没有符合条件的员工。</p>;
  }

  return (
    <ul>
      {employees.map((employee) => (
        <li key={employee.id}>{employee.name}</li>
      ))}
    </ul>
  );
}
```

需要筛选时，先用 `filter()` 得到要显示的数据，再使用 `map()`：

```jsx
function ActiveEmployeeList({ employees }) {
  const activeEmployees = employees.filter(
    (employee) => employee.status === 'ACTIVE',
  );

  if (activeEmployees.length === 0) {
    return <p>没有在职员工。</p>;
  }

  return (
    <ul>
      {activeEmployees.map((employee) => (
        <li key={employee.id}>{employee.name}</li>
      ))}
    </ul>
  );
}
```

```text
原始数组 → filter() 选择数据 → map() 转换为 JSX → 页面
```

不要在渲染过程中使用 `sort()` 直接改变 Props 数组。排序时先复制数组，数组 State 的安全更新会在第六章详细讲解。

## 10. 完整可运行示例

新建 `src/components/EmployeeList.jsx`：

```jsx
import { EmployeeCard } from './EmployeeCard.jsx';

export function EmployeeList({ employees, showOnlyActive = false }) {
  const displayEmployees = showOnlyActive
    ? employees.filter((employee) => employee.status === 'ACTIVE')
    : employees;

  if (displayEmployees.length === 0) {
    return <p>没有符合条件的员工。</p>;
  }

  return (
    <ul className="employee-list">
      {displayEmployees.map((employee) => (
        <li key={employee.id}>
          <EmployeeCard employee={employee} />
        </li>
      ))}
    </ul>
  );
}
```

将 `src/App.jsx` 替换为：

```jsx
import { EmployeeList } from './components/EmployeeList.jsx';
import { PageHeader } from './components/PageHeader.jsx';
import { Panel } from './components/Panel.jsx';

export default function App() {
  const employees = [
    {
      id: 1001,
      name: '田中太郎',
      department: '営業部',
      status: 'ACTIVE',
    },
    {
      id: 1002,
      name: '佐藤花子',
      department: '開発部',
      status: 'ACTIVE',
    },
    {
      id: 1003,
      name: '鈴木一郎',
      department: '人事部',
      status: 'INACTIVE',
    },
  ];

  return (
    <>
      <PageHeader />
      <main>
        <Panel title="员工列表">
          <EmployeeList employees={employees} />
        </Panel>
      </main>
    </>
  );
}
```

成功结果：页面按数组顺序显示三名员工。把 `EmployeeList` 改为 `showOnlyActive` 后，只显示两名在职员工：

```jsx
<EmployeeList employees={employees} showOnlyActive />
```

把 `employees` 临时改为空数组，应显示“没有符合条件的员工”，而不是空白页面。

## 11. 常见错误与调查方法

- 列表空白：检查 `map()` 使用花括号后是否漏写 `return`。
- 出现 `map is not a function`：确认传入的是数组，而不是对象、`null` 或错误的响应结构。
- 出现 key 警告：检查 `map()` 直接返回的最外层元素是否有稳定 `key`。
- 页面显示数字 `0`：检查是否写了 `{count && ...}`，改为明确布尔条件。
- 过滤结果错误：先在 Console 检查 `filter()` 后的数组，再检查 JSX。
- 隐藏按钮后误以为完成授权：条件渲染只控制画面，后端仍必须检查权限。

## 12. 练习

1. 使用四名员工数据渲染 `EmployeeList`，以员工 ID 作为 `key`。
2. 增加 `showOnlyActive`，分别验证全部员工和仅在职员工。
3. 将数组改为空数组，确认显示 Empty 信息。
4. 为 `StatusLabel` 使用三元表达式显示“在职”或“离职”。
5. 增加 `canCreate`，使用 `&&` 控制新增按钮；说明这不能代替后端权限检查。
6. 故意删除 `key`，观察浏览器 Console 警告后修复。
7. 故意把箭头函数改成花括号但不写 `return`，调查空白原因后恢复。
8. 执行 `npm run build`，确认条件和列表代码可以构建。

## 本章检查点

- [ ] 能根据场景选择提前返回、`&&`、三元表达式或 `null`。
- [ ] 能说明 `map()` 如何把数据数组转换为 JSX 数组。
- [ ] 能把遍历职责和单条数据显示职责放在不同组件。
- [ ] 能为列表选择稳定业务 ID，并解释索引和随机值的风险。
- [ ] 能处理空数据，并正确组合 `filter()` 与 `map()`。
- [ ] 能根据终端、Console 和数据结构调查列表空白问题。

参考：[React：条件渲染](https://zh-hans.react.dev/learn/conditional-rendering)、[React：渲染列表](https://zh-hans.react.dev/learn/rendering-lists)。
