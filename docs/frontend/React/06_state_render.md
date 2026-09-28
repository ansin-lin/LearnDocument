# 第 6 章 对象、数组 State 与重新渲染

## 本章目标

- 【必须掌握】说明为什么不能直接修改对象或数组 State。
- 【必须掌握】使用展开、`map()` 和 `filter()` 创建下一份 State。
- 【必须掌握】完成对象字段修改，以及数组的追加、删除和更新。
- 【必须掌握】区分触发更新、Render 和 Commit。
- 【会使用、能看懂】处理嵌套对象、排序和组件 State 重置。

## 1. 为什么对象和数组 State 需要特殊处理

数字、字符串和布尔值更新时，通常直接提供新值：

```jsx
setCount(2);
setKeyword('田中');
setOpen(true);
```

对象和数组是引用值。直接修改它们不会创建新引用：

```jsx
employee.name = '山田太郎';
employees.push(newEmployee);
```

这种写法改变了当前 State 本身，却没有通过 Setter 提供一份新的 State。它会带来以下问题：

- React 可能无法正确判断数据已经变化。
- 当前渲染保存的旧快照被改写，调查历史值变得困难。
- 多个组件共享同一对象时会发生意外联动。
- 后续的 memo、撤销和测试更难保持可靠。

React State 应当视为只读快照。更新时创建下一份对象或数组，再交给 Setter。

```text
不要修改旧 State
        ↓
根据旧 State 创建新值
        ↓
Setter 接收新值
        ↓
React 安排重新渲染
```

## 2. 更新对象 State

```jsx
const [employee, setEmployee] = useState({
  id: 1001,
  name: '田中太郎',
  department: '営業部',
  status: 'ACTIVE',
});
```

错误写法：

```jsx
employee.name = '田中一郎';
setEmployee(employee);
```

`employee` 仍然是原来的对象引用。正确做法是创建新对象：

```jsx
setEmployee({
  ...employee,
  name: '田中一郎',
});
```

`...employee` 先复制原对象的所有字段，后面的 `name` 再覆盖旧值。没有修改的 `id`、`department` 和 `status` 会被保留。

新值依赖旧 State 时，使用函数式更新更稳定：

```jsx
setEmployee((previous) => ({
  ...previous,
  status: previous.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
}));
```

### 2.1 更新字段名来自变量

表单中经常根据控件的 `name` 更新对应字段：

```jsx
const fieldName = 'department';
const fieldValue = '開発部';

setEmployee((previous) => ({
  ...previous,
  [fieldName]: fieldValue,
}));
```

`[fieldName]` 是计算属性名。本例实际更新的是 `department`。

### 2.2 更新嵌套对象

展开语法只复制一层。假设员工对象包含地址：

```jsx
const [employee, setEmployee] = useState({
  name: '田中太郎',
  address: {
    prefecture: '東京都',
    city: '新宿区',
  },
});
```

只复制外层仍会修改旧的 `address`：

```jsx
// 错误
employee.address.city = '渋谷区';
setEmployee({ ...employee });
```

应逐层创建新对象：

```jsx
setEmployee((previous) => ({
  ...previous,
  address: {
    ...previous.address,
    city: '渋谷区',
  },
}));
```

嵌套过深会让更新难以阅读。业务允许时，可以把 State 设计得更扁平，而不是默认使用深层对象。

## 3. 更新数组 State

```jsx
const [employees, setEmployees] = useState(initialEmployees);
```

数组更新的基本原则相同：不要改变旧数组，创建新数组。

### 3.1 追加数据

```jsx
const newEmployee = {
  id: 1004,
  name: '高橋次郎',
  department: '総務部',
  status: 'ACTIVE',
};

setEmployees((previous) => [
  ...previous,
  newEmployee,
]);
```

需要添加到开头时：

```jsx
setEmployees((previous) => [newEmployee, ...previous]);
```

不要对旧数组使用 `push()`：

```jsx
// 错误
employees.push(newEmployee);
setEmployees(employees);
```

### 3.2 删除数据

`filter()` 返回不包含目标项的新数组：

```jsx
setEmployees((previous) =>
  previous.filter((employee) => employee.id !== targetId),
);
```

如果没有符合删除条件的数据，`filter()` 会返回内容相同的新数组。实际项目可以先确认目标是否存在，或根据接口结果决定是否刷新。

### 3.3 更新数组中的一项

`map()` 为每一项返回下一份结果。目标员工创建新对象，其他项保持原对象：

```jsx
setEmployees((previous) =>
  previous.map((employee) =>
    employee.id === targetId
      ? { ...employee, status: 'INACTIVE' }
      : employee,
  ),
);
```

执行过程：

```text
遍历每名员工
 ├─ ID 等于 targetId → 返回更新后的新对象
 └─ 其他员工          → 返回原对象
               ↓
          得到新数组
```

### 3.4 替换整项

服务器返回更新后的完整对象时，可以替换目标项：

```jsx
setEmployees((previous) =>
  previous.map((employee) =>
    employee.id === updatedEmployee.id
      ? updatedEmployee
      : employee,
  ),
);
```

不要只根据前端输入猜测服务器最终结果；服务端可能补充更新时间、默认值或规范化后的字段。

## 4. 排序与反转

`sort()` 和 `reverse()` 会直接改变原数组。State 数组不能直接调用：

```jsx
// 错误：修改原数组
employees.sort((a, b) => a.name.localeCompare(b.name));
```

先复制再排序：

```jsx
const sortedEmployees = [...employees].sort((a, b) =>
  a.name.localeCompare(b.name, 'ja'),
);
```

如果排序只影响当前显示，可以把 `sortedEmployees` 作为派生值，不必再保存一份 State。只有用户的排序操作真正改变业务数据顺序时，才考虑写回 State 或后端。

## 5. 常见数组操作对照

| 目的 | 避免直接用于 State | 推荐方式 |
| --- | --- | --- |
| 追加 | `push()` | `[...previous, item]` |
| 删除末尾 | `pop()` | `slice(0, -1)` |
| 删除指定项 | `splice()` | `filter()` |
| 修改指定项 | 直接修改 `array[index]` | `map()` |
| 排序 | 直接 `sort()` | `[...array].sort()` |
| 反转 | 直接 `reverse()` | `[...array].reverse()` |

重点不是禁止这些 JavaScript 方法，而是不要让它们直接修改当前 State。

## 6. React 更新画面的三个阶段

State 更新后，不是立即重写整个 DOM。可以把过程分为三个阶段：

### 6.1 Trigger：触发更新

初次由 `createRoot(...).render(<App />)` 触发；后续通常由 Setter 触发。

### 6.2 Render：计算下一份 UI

React 调用组件函数，根据当前 Props、State 和 Context 得到新的 JSX。

### 6.3 Commit：提交必要的 DOM 变化

React 比较前后结果，把必要变化提交到浏览器 DOM。组件函数重新执行不等于所有真实 DOM 都被删除重建。

```text
Event / Setter
      ↓ Trigger
调用组件函数
      ↓ Render
得到下一份 JSX
      ↓ Commit
修改必要 DOM
```

Render 阶段应保持纯粹：读取输入并计算 JSX，不发送请求、不写 Storage、不修改外部变量。外部同步会在第九章讲解。

## 7. 父组件重新渲染时会发生什么

父组件重新渲染时，React 默认也会计算它使用的子组件：

```text
App State 更新
   ↓
App 再次执行
   ↓
EmployeeList 再次计算
   ↓
EmployeeCard 再次计算
```

“组件函数再次执行”和“浏览器 DOM 全部变化”是两件事。React 会在 Commit 阶段只提交必要的 DOM 更新。

不要看到组件函数执行多次就立即添加 `memo`、`useMemo` 或 `useCallback`。先确认用户是否真的遇到性能问题，并使用 Profiler 测量；性能优化会在第十九章讲解。

## 8. State 如何保留或重置

React 根据组件在 UI 树中的位置保存 State。同一个组件继续出现在同一位置时，State 通常会保留。

```jsx
{showCounter && <Counter />}
```

当 `Counter` 被移除后，它的 State 也会被丢弃；再次显示时从初始值开始。

`key` 也会影响组件身份。需要明确把编辑表单切换为另一名员工并重置内部草稿时，可以使用稳定业务 ID：

```jsx
<EmployeeEditForm key={employee.id} employee={employee} />
```

不要用随机 `key` 强制所有组件反复重置。是否保留 State 应由明确的业务需求决定。

## 9. StrictMode 为什么会额外执行

第一章的开发项目使用 `StrictMode`。开发环境中，React 可能额外调用组件函数或更新函数，帮助发现修改外部数据等不纯逻辑。

下面的函数式更新直接修改旧数组，就可能暴露问题：

```jsx
// 错误
setEmployees((previous) => {
  previous.push(newEmployee);
  return previous;
});
```

正确代码应无论执行检查多少次，都不会修改传入的旧 State。不要通过删除 `StrictMode` 掩盖问题。

## 10. 完整可运行示例

将 `src/App.jsx` 替换为以下示例：

```jsx
import { useState } from 'react';
import { EmployeeList } from './components/EmployeeList.jsx';
import { PageHeader } from './components/PageHeader.jsx';
import { Panel } from './components/Panel.jsx';

const initialEmployees = [
  { id: 1001, name: '田中太郎', department: '営業部', status: 'ACTIVE' },
  { id: 1002, name: '佐藤花子', department: '開発部', status: 'ACTIVE' },
];

export default function App() {
  const [employees, setEmployees] = useState(initialEmployees);

  function handleAdd() {
    const newEmployee = {
      id: 1003,
      name: '鈴木一郎',
      department: '人事部',
      status: 'ACTIVE',
    };

    setEmployees((previous) => {
      const alreadyExists = previous.some(
        (employee) => employee.id === newEmployee.id,
      );
      return alreadyExists ? previous : [...previous, newEmployee];
    });
  }

  function handleToggleStatus(targetId) {
    setEmployees((previous) =>
      previous.map((employee) =>
        employee.id === targetId
          ? {
              ...employee,
              status: employee.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
            }
          : employee,
      ),
    );
  }

  function handleDelete(targetId) {
    setEmployees((previous) =>
      previous.filter((employee) => employee.id !== targetId),
    );
  }

  return (
    <>
      <PageHeader />
      <main>
        <Panel title="员工 State 操作">
          <button type="button" onClick={handleAdd}>追加员工</button>
          <button type="button" onClick={() => handleToggleStatus(1001)}>
            切换田中状态
          </button>
          <button type="button" onClick={() => handleDelete(1002)}>
            删除佐藤
          </button>
          <EmployeeList employees={employees} />
        </Panel>
      </main>
    </>
  );
}
```

本章暂时让 `App` 同时放置按钮和更新函数，以便集中观察数组更新。下一章会把按钮拆进子组件，再通过回调 Props 把操作通知父组件。

## 11. 常见错误与练习

- 页面不更新：检查是否直接修改旧对象/数组，或把同一引用传回 Setter。
- 其他组件数据意外变化：检查多个位置是否共享并修改同一个对象。
- 排序后原列表也变化：检查是否直接调用了 `sort()`。
- 嵌套字段修改后旧快照变化：确认每一层对象都创建了新值。
- State 意外重置：检查组件是否被移除、位置是否改变或 `key` 是否变化。
- 开发环境结果重复：检查更新函数是否包含 `push()`、日志以外的外部写入等副作用。

练习：

1. 增加员工、删除员工并切换状态，确认每次都创建新数组。
2. 实现按 ID 修改员工姓名。
3. 为员工增加 `address`，正确更新其中的 `city`。
4. 按姓名排序显示，但不改变原 State 数组。
5. 使用 React DevTools 观察更新前后的 State。
6. 使用 Console 保存旧数组引用，确认更新后旧数组内容没有变化。
7. 切换显示 `Counter`，观察卸载后 State 如何重置。

## 本章检查点

- [ ] 能解释对象和数组 State 为什么必须创建新引用。
- [ ] 能使用展开语法更新对象和嵌套对象。
- [ ] 能使用展开、`filter()` 和 `map()` 完成数组增删改。
- [ ] 能安全地排序或反转 State 数组。
- [ ] 能区分 Trigger、Render 和 Commit。
- [ ] 能说明组件位置和 `key` 如何影响 State 保留。

参考：[React：更新 State 中的对象](https://zh-hans.react.dev/learn/updating-objects-in-state)、[React：更新 State 中的数组](https://zh-hans.react.dev/learn/updating-arrays-in-state)、[React：渲染和提交](https://zh-hans.react.dev/learn/render-and-commit)、[React：保留和重置 State](https://zh-hans.react.dev/learn/preserving-and-resetting-state)。
