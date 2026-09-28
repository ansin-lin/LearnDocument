# 第 5 章 事件与 useState

## 本章目标

- 【必须掌握】把事件处理函数正确传给 JSX，而不是在渲染时调用。
- 【必须掌握】读取事件对象，并在需要时向处理函数传递业务参数。
- 【必须掌握】使用 `useState` 保存会影响画面的组件状态。
- 【必须掌握】说明 Setter、重新渲染和 State 快照之间的关系。
- 【必须掌握】在依赖旧值时使用函数式更新。
- 【必须掌握】判断一个值应该成为 State，还是应该直接计算。

## 1. 页面为什么需要事件和 State

前四章的页面根据固定数据生成 UI，但用户操作还不能改变画面。一个可交互页面通常需要两个部分：

- **事件（Event）**：用户做了什么，例如点击、输入、提交或聚焦。
- **状态（State）**：组件需要记住什么，例如当前次数、是否展开、选择了哪个条件。

```text
用户点击按钮
     ↓
事件处理函数执行
     ↓
更新 State
     ↓
组件使用新 State 重新渲染
     ↓
页面显示新结果
```

事件负责发起变化，State 负责保存变化后的数据。

## 2. React 事件的基本写法

React 事件属性采用 camelCase，例如 `onClick`、`onChange`、`onSubmit`。

```jsx
function SaveButton() {
  function handleClick() {
    console.log('开始保存');
  }

  return (
    <button type="button" onClick={handleClick}>
      保存
    </button>
  );
}
```

`handleClick` 是事件处理函数。`onClick={handleClick}` 把函数交给 React，只有用户点击按钮时才执行。

### 2.1 传函数，不要立即调用

```jsx
// 正确：把函数传给 onClick
<button onClick={handleClick}>保存</button>

// 错误：渲染时立即执行函数
<button onClick={handleClick()}>保存</button>
```

第二种写法会在组件渲染期间调用 `handleClick()`，然后把它的返回值交给 `onClick`。这通常会导致页面一打开就执行操作；如果函数中更新 State，还可能造成无限重新渲染。

### 2.2 内联箭头函数

操作很短时，可以直接传入箭头函数：

```jsx
<button type="button" onClick={() => console.log('保存')}>
  保存
</button>
```

业务步骤较多时，使用有名称的函数更容易阅读、调试和测试：

```jsx
function handleSave() {
  console.log('检查输入');
  console.log('开始保存');
}

<button type="button" onClick={handleSave}>保存</button>
```

## 3. 常见事件

| React 属性 | 触发时机 | 常见用途 |
| --- | --- | --- |
| `onClick` | 点击元素 | 按钮操作、切换显示 |
| `onChange` | 表单值发生变化 | 读取输入框、下拉框和复选框 |
| `onSubmit` | 提交表单 | 校验并保存数据 |
| `onFocus` | 元素获得焦点 | 显示输入提示 |
| `onBlur` | 元素失去焦点 | 离开输入框时校验 |
| `onKeyDown` | 按下键盘按键 | 快捷键或特殊键盘操作 |

优先使用符合语义的元素。例如操作使用 `button`，页面跳转使用链接，不要为了点击效果把普通 `div` 当按钮。

## 4. 事件对象是什么

React 调用事件处理函数时，会把事件对象作为第一个参数传入：

```jsx
function handleClick(event) {
  console.log(event.currentTarget);
}

<button type="button" onClick={handleClick}>
  查看事件目标
</button>
```

事件对象包含本次操作的信息。常用字段和方法包括：

| 内容 | 作用 |
| --- | --- |
| `event.currentTarget` | 当前绑定事件处理器的元素 |
| `event.target` | 实际触发事件的最深层元素 |
| `event.preventDefault()` | 阻止浏览器默认行为 |
| `event.stopPropagation()` | 阻止事件继续冒泡，应谨慎使用 |

### 4.1 target 与 currentTarget

```jsx
function ActionButton() {
  function handleClick(event) {
    console.log('target:', event.target);
    console.log('currentTarget:', event.currentTarget);
  }

  return (
    <button type="button" onClick={handleClick}>
      <span>保存</span>
    </button>
  );
}
```

点击文字时，`target` 可能是内部的 `span`，`currentTarget` 是绑定 `onClick` 的 `button`。需要读取按钮自身属性时，通常使用 `currentTarget` 更稳定。

### 4.2 阻止默认行为

表单提交默认会让浏览器执行提交行为。React 表单通常先阻止默认行为，再执行校验和 JavaScript 保存逻辑：

```jsx
function handleSubmit(event) {
  event.preventDefault();
  console.log('执行 React 表单处理');
}

<form onSubmit={handleSubmit}>
  <button type="submit">保存</button>
</form>
```

表单状态和完整校验会在第八章详细讲解。

React 事件通常遵循浏览器的冒泡规律。只有组件确实需要阻止父级处理时才使用 `stopPropagation()`；不要把它当作修复所有重复触发问题的默认方法。

## 5. 向事件处理函数传递参数

删除某名员工时，处理函数需要知道员工 ID：

```jsx
function handleDelete(employeeId) {
  console.log('删除员工：', employeeId);
}

<button
  type="button"
  onClick={() => handleDelete(employee.id)}
>
  删除
</button>
```

不能写成：

```jsx
// 错误：渲染时立即调用
<button onClick={handleDelete(employee.id)}>删除</button>
```

箭头函数 `() => handleDelete(employee.id)` 是一个新的函数。用户点击后，React 才调用这个包装函数，再由它调用 `handleDelete`。

同时需要事件对象和业务参数时：

```jsx
function handleAction(event, employeeId) {
  console.log(event.currentTarget);
  console.log(employeeId);
}

<button
  type="button"
  onClick={(event) => handleAction(event, employee.id)}
>
  执行
</button>
```

## 6. 普通变量为什么不能保存画面状态

下面的组件尝试用普通变量记录点击次数：

```jsx
function Counter() {
  let count = 0;

  function handleClick() {
    count = count + 1;
    console.log(count);
  }

  return (
    <button type="button" onClick={handleClick}>
      点击次数：{count}
    </button>
  );
}
```

点击后 Console 中的变量可能变化，但 React 不知道需要重新渲染；组件下次执行时，`count` 又会重新初始化为 `0`。

需要满足下面两个条件的数据，应考虑使用 State：

1. 它需要在多次渲染之间保留。
2. 它变化后需要更新页面。

## 7. 使用 useState 保存组件状态

`useState` 是 React 提供的 Hook。先从 React 导入：

```jsx
import { useState } from 'react';
```

然后在组件顶层调用：

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);
  }

  return (
    <button type="button" onClick={handleClick}>
      点击次数：{count}
    </button>
  );
}
```

`useState(0)` 返回一个包含两个元素的数组，代码使用数组解构分别取得：

```text
count     当前 State，初始值为 0
setCount  请求更新 count 的 Setter 函数
```

完整过程：

```text
第一次渲染
count = 0
页面显示“点击次数：0”

用户点击
setCount(1)
React 安排更新
组件再次执行
count = 1
页面显示“点击次数：1”
```

Setter 名称通常写成 `set` 加 State 名称，例如：

```jsx
const [open, setOpen] = useState(false);
const [keyword, setKeyword] = useState('');
const [selectedId, setSelectedId] = useState(null);
```

## 8. State 是当前渲染的一张快照

调用 Setter 不会立即改写当前函数中的变量：

```jsx
function handleClick() {
  console.log('更新前：', count);
  setCount(count + 1);
  console.log('调用 Setter 后：', count);
}
```

两次日志会输出同一个旧值。原因是当前事件处理函数看到的是本次渲染的 State 快照。`setCount` 请求 React 使用新值进行下一次渲染，而不是修改当前 `count` 变量。

```text
当前渲染：count = 0
     ↓ 点击
事件处理函数仍看到 count = 0
     ↓ setCount(1)
React 安排下一次渲染
     ↓
下一次渲染：count = 1
```

Setter 也不返回等待 State 更新完成的 Promise，因此不要写 `await setCount(...)` 来期待下一行得到新值。

## 9. 批处理与函数式更新

React 会把同一个事件中的多次 State 更新一起处理。下面三次更新都读取当前快照中的同一个 `count`：

```jsx
function handleWrongAddThree() {
  setCount(count + 1);
  setCount(count + 1);
  setCount(count + 1);
}
```

如果 `count` 是 `0`，三次代码都相当于请求设置为 `1`，最终通常只增加一次。

当新值依赖旧值时，把更新函数传给 Setter：

```jsx
function handleAddThree() {
  setCount((previous) => previous + 1);
  setCount((previous) => previous + 1);
  setCount((previous) => previous + 1);
}
```

React 会依次处理这些更新函数：

```text
previous = 0 → 1
previous = 1 → 2
previous = 2 → 3
```

切换布尔状态也适合函数式更新：

```jsx
function handleToggle() {
  setOpen((previous) => !previous);
}
```

判断标准：

- 直接替换成与旧值无关的新值：`setStatus('ACTIVE')`。
- 新值需要根据旧值计算：`setCount(previous => previous + 1)`。

## 10. Hook 的调用规则

`useState`、`useEffect`、`useRef` 等以 `use` 开头的 React API 称为 Hook。Hook 必须在组件或自定义 Hook 的顶层调用。

正确：

```jsx
function EmployeePage() {
  const [open, setOpen] = useState(false);

  if (!open) {
    return <button onClick={() => setOpen(true)}>打开</button>;
  }

  return <p>已经打开</p>;
}
```

错误：

```jsx
function EmployeePage({ enabled }) {
  if (enabled) {
    const [open, setOpen] = useState(false);
  }
}
```

也不能在循环、事件处理函数或普通 JavaScript 函数中调用 Hook：

```jsx
function handleClick() {
  // 错误：不能在事件处理函数内部调用 Hook
  const [count, setCount] = useState(0);
}
```

React 依赖每次渲染中 Hook 的调用顺序保持一致，才能把各个 State 与正确位置对应起来。

## 11. 一个组件可以有多个 State

互相独立的简单数据可以分别声明：

```jsx
function EmployeePage() {
  const [showOnlyActive, setShowOnlyActive] = useState(false);
  const [viewCount, setViewCount] = useState(0);

  // ...
}
```

不要因为它们位于同一个组件就强行合成一个对象。对象和数组 State 适合表达一组相关结构，但更新时必须遵守不可变原则，第六章会专门讲解。

每个组件实例都有自己的 State。页面同时渲染两个 `Counter` 时，它们的点击次数互不影响：

```jsx
<Counter />
<Counter />
```

如果两个组件必须共享同一状态，应把 State 放到最近共同父组件，再通过 Props 传递；这个过程会在第七章讲解。

## 12. 哪些值不应该成为 State

能够从现有 Props 或 State 直接计算出的值，通常不需要再存一份 State：

```jsx
const activeEmployees = employees.filter(
  (employee) => employee.status === 'ACTIVE',
);
```

不推荐同时保存原数组和派生数组：

```jsx
// 不推荐：两份数据可能不同步
const [employees, setEmployees] = useState(initialEmployees);
const [activeEmployees, setActiveEmployees] = useState([]);
```

判断一个值是否需要 State，可以问：

1. 它是否会随用户操作或外部结果变化？
2. 变化后是否需要重新渲染？
3. 它是否已经能从其他 Props 或 State 算出来？

只有前两项为“是”，并且第三项为“否”时，通常才需要独立 State。

## 13. 完整可运行示例

本例继续使用第四章的 `EmployeeList`。将 `src/App.jsx` 替换为：

```jsx
import { useState } from 'react';
import { EmployeeList } from './components/EmployeeList.jsx';
import { PageHeader } from './components/PageHeader.jsx';
import { Panel } from './components/Panel.jsx';

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

export default function App() {
  const [showOnlyActive, setShowOnlyActive] = useState(false);
  const [operationCount, setOperationCount] = useState(0);

  function handleToggleFilter() {
    setShowOnlyActive((previous) => !previous);
    setOperationCount((previous) => previous + 1);
  }

  return (
    <>
      <PageHeader />
      <main>
        <Panel title="员工列表">
          <button type="button" onClick={handleToggleFilter}>
            {showOnlyActive ? '显示全部员工' : '只显示在职员工'}
          </button>

          <p>筛选操作次数：{operationCount}</p>

          <EmployeeList
            employees={employees}
            showOnlyActive={showOnlyActive}
          />
        </Panel>
      </main>
    </>
  );
}
```

操作结果：

1. 初次显示三名员工，按钮文字为“只显示在职员工”。
2. 点击按钮后，`showOnlyActive` 变为 `true`，列表只显示两名在职员工。
3. 按钮文字切换为“显示全部员工”。
4. 每次点击都会让 `operationCount` 增加 1。

数据流如下：

```text
Button click
   ↓ handleToggleFilter
setShowOnlyActive / setOperationCount
   ↓ React 重新渲染 App
新的 Props 传给 EmployeeList
   ↓
列表和按钮文字更新
```

## 14. 常见错误与调查方法

### 14.1 把处理函数写成调用结果

症状：页面打开时函数立即执行，或出现重复渲染。

```jsx
// 错误
<button onClick={handleToggleFilter()}>切换</button>
```

修正为 `onClick={handleToggleFilter}`。

### 14.2 直接修改普通变量

症状：Console 中数值变化，但页面不更新，或重新渲染后恢复初始值。

修正：需要保留并影响画面的值使用 State。

### 14.3 调用 Setter 后立即读取新值

症状：日志仍显示旧值。

原因：事件处理函数读取的是当前渲染快照。使用下一次渲染结果观察新值，不要把 Setter 当同步赋值。

### 14.4 根据旧值更新却不用函数式写法

症状：连续多次更新只生效一次，或快速操作时结果不稳定。

修正：使用 `setCount(previous => previous + 1)`。

### 14.5 在条件或事件中调用 Hook

症状：出现 Hook 调用顺序错误，组件行为不稳定。

修正：所有 Hook 移到组件顶层。

### 14.6 在渲染过程中调用 Setter

```jsx
function Counter() {
  const [count, setCount] = useState(0);
  setCount(count + 1);
  return <p>{count}</p>;
}
```

组件渲染时调用 Setter，会触发下一次渲染；下一次又调用 Setter，最终形成无限循环。Setter 应由事件或后续章节讲解的外部同步流程触发。

## 15. 练习

1. 创建 `Counter`，实现增加 1、减少 1 和归零。
2. 增加“一次加 3”按钮，先观察错误写法，再改成三次函数式更新。
3. 为第四章员工列表增加“全部/仅在职”切换按钮。
4. 增加显示/隐藏员工统计信息的布尔 State。
5. 在 Setter 前后输出 State，解释为什么当前事件看到旧快照。
6. 创建两个 `Counter` 实例，确认 State 相互独立。
7. 把一个可直接计算的 `activeEmployees` State 删除，改为从 `employees` 计算。
8. 故意把 `onClick={handleClick}` 改成 `onClick={handleClick()}`，观察问题后恢复。
9. 执行 `npm run build`，并用 React DevTools 观察 State 变化。

## 本章检查点

- [ ] 能区分传递事件函数、内联包装函数和渲染时立即调用。
- [ ] 能读取事件对象，并说明 `target` 与 `currentTarget` 的区别。
- [ ] 能说明普通变量为什么不能保存需要显示的组件状态。
- [ ] 能解释 `useState` 返回的当前值与 Setter。
- [ ] 能画出事件、Setter、重新渲染和 UI 更新的流程。
- [ ] 能解释 State 快照、批处理和函数式更新。
- [ ] 能遵守 Hook 顶层调用规则。
- [ ] 能判断一个值应成为 State，还是应从现有数据直接计算。

参考：[React：响应事件](https://zh-hans.react.dev/learn/responding-to-events)、[React：State——组件的记忆](https://zh-hans.react.dev/learn/state-a-components-memory)、[React：State 如同一张快照](https://zh-hans.react.dev/learn/state-as-a-snapshot)、[React：把一系列 State 更新加入队列](https://zh-hans.react.dev/learn/queueing-a-series-of-state-updates)。
