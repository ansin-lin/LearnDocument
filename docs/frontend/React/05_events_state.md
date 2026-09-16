# 第 5 章 事件与 useState

## 本章目标

- 把函数作为事件处理器传给 JSX。
- 用 State 保存会影响 UI 的组件记忆。
- 解释 State 快照、批处理与函数式更新。

## 1. 事件处理

```tsx
function SaveButton() {
  function handleClick() {
    console.log('save');
  }

  return <button onClick={handleClick}>保存</button>;
}
```

`onClick={handleClick}` 传递函数，点击时才调用。`onClick={handleClick()}` 会在渲染时立即调用，并把返回值当作处理器。

需要参数时使用包装函数：

```tsx
<button onClick={() => deleteEmployee(employee.id)}>删除</button>
```

常见事件包括 `onClick`、`onChange`、`onSubmit`、`onBlur` 和 `onFocus`。TypeScript 可明确事件来源：

```tsx
function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
  console.log(event.currentTarget.value);
}
```

## 2. State 是组件的记忆

```tsx
import { useState } from 'react';

function Counter() {
  const [count, setCount] = useState(0);

  return (
    <button onClick={() => setCount((previous) => previous + 1)}>
      点击次数：{count}
    </button>
  );
}
```

State 是组件内部需要保存、且变化后要反映到 UI 的数据。

```text
用户操作 → Event → setState 请求更新 → 新 State 渲染 → React 提交 UI
```

Hook 必须在组件或自定义 Hook 的顶层调用，不能放在条件、循环或普通函数中。

## 3. 快照与函数式更新

一次渲染中的 `count` 是固定快照：

```tsx
function handleWrong() {
  setCount(count + 1);
  setCount(count + 1);
  setCount(count + 1);
}
```

三次都基于同一个旧 `count` 请求相同结果。需要连续根据前一次待处理结果更新时：

```tsx
function handleAddThree() {
  setCount((previous) => previous + 1);
  setCount((previous) => previous + 1);
  setCount((previous) => previous + 1);
}
```

React 会把同一事件中的更新排队并批处理。Setter 不是立即改写当前函数中的变量；旧快照仍可被当前事件处理器读取。

## 4. 何时不用 State

能从 Props/State 直接计算的值通常不另存 State：

```tsx
const activeEmployees = employees.filter((item) => item.status === 'ACTIVE');
```

重复保存 `employees` 与 `activeEmployees` 会制造同步问题。普通局部变量也不能跨渲染保存，且修改它不会触发渲染。

## 5. 常见错误与练习

- 渲染期间调用 Setter：产生无限重新渲染。
- 读取 Setter 后的变量并期待新值：当前事件仍看到旧快照。
- 条件调用 Hook：破坏 Hook 调用顺序。

练习：实现计数器、员工状态切换和“一次加 3”；用 Console 记录 Setter 前后值并解释结果。参考：[React：state 如同一张快照](https://zh-hans.react.dev/learn/state-as-a-snapshot) 与 [把一系列 state 更新加入队列](https://zh-hans.react.dev/learn/queueing-a-series-of-state-updates)。

## 本章检查点

- [ ] 能区分传递事件函数与渲染时调用函数。
- [ ] 能解释 State 快照、批处理和函数式更新。
- [ ] 能判断一个值是否真的需要成为 State。
