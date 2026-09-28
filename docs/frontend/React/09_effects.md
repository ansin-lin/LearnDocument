# 第 9 章 useEffect 与外部系统同步

## 本章目标

- 【必须掌握】区分渲染代码、事件处理器和 Effect。
- 【必须掌握】说明 Effect 为什么用于与 React 外部系统同步。
- 【必须掌握】正确理解依赖数组，而不是靠猜测控制执行次数。
- 【必须掌握】为计时器、订阅和请求编写 cleanup。
- 【必须掌握】识别不需要 Effect 的派生计算和用户操作。
- 【会使用、能看懂】使用 AbortController 取消旧请求。

## 1. 为什么需要 Effect

React 组件负责根据 Props 和 State 计算 JSX，但页面还需要和浏览器或外部系统配合，例如：

- 修改浏览器标签标题 `document.title`。
- 启动和清除计时器。
- 注册和移除浏览器事件监听。
- 连接聊天、WebSocket 或第三方 UI 小部件。
- 根据当前员工 ID 请求服务器数据。

这些操作不只是计算 JSX，而是在 React 之外产生影响，因此称为副作用。`useEffect` 用来让外部系统与当前组件状态保持同步。

```text
Props / State
     ↓ Render
React 提交 UI
     ↓ Effect
同步浏览器、网络或其他外部系统
```

Effect 在 React 把画面提交给浏览器之后运行，不应在 Render 阶段直接执行外部操作。

## 2. 三类代码放在哪里

### 2.1 渲染代码

根据 Props 和 State 计算 JSX，应保持纯粹：

```jsx
const activeEmployees = employees.filter(
  (employee) => employee.status === 'ACTIVE',
);
```

### 2.2 事件处理器

由明确用户操作触发：

```jsx
async function handleSave() {
  await saveEmployee(form);
}
```

### 2.3 Effect

因为组件当前正在画面上显示，所以需要保持某个外部系统同步：

```jsx
useEffect(() => {
  document.title = `${title} | Employee System`;
}, [title]);
```

判断方式：

```text
只是根据已有数据计算？       → 渲染中直接计算
由一次点击或提交触发？       → 事件处理器
因为组件正在显示而需同步外部？→ Effect
```

## 3. useEffect 的基本结构

先导入：

```jsx
import { useEffect } from 'react';
```

基本写法：

```jsx
useEffect(() => {
  // setup：建立同步

  return () => {
    // cleanup：撤销上一次同步
  };
}, [dependencies]);
```

- setup：Effect 开始时执行。
- cleanup：下一次 setup 前以及组件卸载时执行。
- dependencies：setup 和 cleanup 中读取的响应式值。

没有资源需要释放时，可以不返回 cleanup：

```jsx
useEffect(() => {
  document.title = title;
}, [title]);
```

## 4. 依赖数组怎么理解

依赖数组不是随意选择的“执行次数开关”。Effect 读取了哪些来自组件的 Props、State 或组件内变量，就应声明对应依赖。

```jsx
function EmployeeTitle({ employeeName }) {
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    document.title = editing
      ? `编辑：${employeeName}`
      : employeeName;
  }, [employeeName, editing]);
}
```

Effect 读取了 `employeeName` 和 `editing`，因此两者都在依赖数组中。任一值变化后，React 需要重新同步标题。

### 4.1 三种常见形式

```jsx
// 每次组件提交后运行
useEffect(() => {
  console.log('每次提交后');
});

// 组件进入画面后建立一次同步；开发 StrictMode 会额外检查
useEffect(() => {
  console.log('建立一次同步');
}, []);

// 初次以及 employeeId 变化后同步
useEffect(() => {
  console.log('当前员工：', employeeId);
}, [employeeId]);
```

没有依赖数组的 Effect 很容易因每次渲染都执行而造成循环，应确认确实需要。空数组表示 Effect 不读取会变化的响应式值，不表示可以故意省略依赖。

### 4.2 不要欺骗 Hooks Linter

```jsx
// 错误思路：读取 employeeId，却为了“只执行一次”写空数组
useEffect(() => {
  loadEmployee(employeeId);
}, []);
```

当 `employeeId` 变化时，上面的 Effect 仍使用第一次的值。正确做法是把 `employeeId` 加入依赖，并让同步能够安全重做。

如果对象或函数导致 Effect 频繁执行，先检查：

- 这个值能否在 Effect 内部创建。
- 这个值能否移到组件外部。
- 是否应通过 `useCallback` 保持函数引用。
- 该逻辑是否根本不需要 Effect。

不要只用注释关闭 Linter。

## 5. cleanup 为什么重要

组件离开画面或依赖变化时，旧同步必须停止，否则可能出现计时器重复、事件监听累积或旧请求覆盖新数据。

### 5.1 计时器

```jsx
useEffect(() => {
  const timerId = window.setInterval(() => {
    console.log('refresh');
  }, 1000);

  return () => {
    window.clearInterval(timerId);
  };
}, []);
```

setup 建立计时器，cleanup 使用同一个 `timerId` 清除计时器。

### 5.2 浏览器事件监听

```jsx
useEffect(() => {
  function handleResize() {
    console.log(window.innerWidth);
  }

  window.addEventListener('resize', handleResize);

  return () => {
    window.removeEventListener('resize', handleResize);
  };
}, []);
```

添加和移除时必须使用同一个函数引用。不要分别写两个不同的匿名函数，否则无法移除原监听器。

### 5.3 cleanup 的执行顺序

当依赖从 `employeeId=1` 变为 `employeeId=2`：

```text
employeeId=1 的 setup
        ↓ ID 变化
employeeId=1 的 cleanup
        ↓
employeeId=2 的 setup
        ↓ 组件卸载
employeeId=2 的 cleanup
```

cleanup 不只在最终卸载时执行，也会在下一次 Effect 重新同步前执行。

## 6. 根据参数读取数据

第十章会正式建立 Axios 和 Service。这里先使用浏览器 `fetch()` 观察 Effect 的依赖与取消边界：

```jsx
function EmployeeDetail({ employeeId }) {
  const [employee, setEmployee] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    async function loadEmployee() {
      setLoading(true);
      setError('');

      try {
        const response = await fetch(`/api/employees/${employeeId}`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        setEmployee(data);
      } catch (cause) {
        if (cause instanceof DOMException && cause.name === 'AbortError') {
          return;
        }
        setError('员工信息读取失败');
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void loadEmployee();

    return () => {
      controller.abort();
    };
  }, [employeeId]);

  if (loading) return <p role="status">读取中...</p>;
  if (error) return <p role="alert">{error}</p>;
  if (!employee) return <p>员工不存在。</p>;

  return <h2>{employee.name}</h2>;
}
```

`AbortController` 创建可以取消的控制器，`signal` 交给请求，cleanup 调用 `abort()`。当 ID 快速变化或组件卸载时，旧请求被取消，不能晚到后覆盖新页面。

这里的 `fetch()` 只用于理解 Effect。下一章会把 URL、timeout 和 HTTP 处理移入 Service，组件不再散落请求细节。

## 7. Race Condition 是什么

假设先请求员工 1，紧接着请求员工 2：

```text
请求员工 1 ───────────────▶ 较晚返回
请求员工 2 ───────▶ 较早返回
```

如果不取消或忽略旧请求，员工 1 的晚到结果可能覆盖员工 2，页面显示错误数据。这就是请求竞态的一种表现。

解决方式通常包括：

- cleanup 中取消旧请求。
- 无法取消时，在 cleanup 中标记旧结果应被忽略。
- 使用服务器状态库提供的取消、缓存和请求身份机制。

不要只检查“组件是否卸载”，还要考虑参数变化后旧请求覆盖新参数结果。

## 8. 哪些情况不需要 Effect

### 8.1 派生数据

```jsx
// 正确：渲染时直接计算
const activeEmployees = employees.filter(
  (employee) => employee.status === 'ACTIVE',
);
```

不推荐：

```jsx
// 多保存一份 State，并多执行一次渲染
useEffect(() => {
  setActiveEmployees(
    employees.filter((employee) => employee.status === 'ACTIVE'),
  );
}, [employees]);
```

### 8.2 用户提交

保存操作由明确点击触发，应放在事件处理器：

```jsx
async function handleSubmit(event) {
  event.preventDefault();
  await saveEmployee(form);
}
```

不要先设置 `shouldSave=true`，再用 Effect 观察它发送请求。这会把一个清楚的用户事件变成难以追踪的间接流程。

### 8.3 为了同步两个 State

如果 `fullName` 能由 `firstName` 和 `lastName` 得到，直接计算：

```jsx
const fullName = `${firstName} ${lastName}`;
```

不要用 Effect 把它同步到第三个 State。

## 9. StrictMode 为什么看起来执行两次

开发环境的 StrictMode 会执行额外的 setup → cleanup → setup 检查，帮助发现 cleanup 缺失和不纯逻辑。

```text
开发检查：setup → cleanup → setup
生产运行：setup
卸载时：cleanup
```

正确的 Effect 应让用户无法区分“只 setup 一次”和“setup、cleanup、再 setup”。例如连接建立后必须能断开，计时器创建后必须能清除。

不要使用 Ref 阻止第二次 setup 来掩盖问题，也不要为了日志看起来只出现一次而删除 StrictMode。

## 10. 一个可运行的同步示例

```jsx
import { useEffect, useState } from 'react';

export function EmployeeClock() {
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    document.title = `停留 ${seconds} 秒`;
  }, [seconds]);

  useEffect(() => {
    const timerId = window.setInterval(() => {
      setSeconds((previous) => previous + 1);
    }, 1000);

    return () => window.clearInterval(timerId);
  }, []);

  return <p>当前页面已停留 {seconds} 秒。</p>;
}
```

第一个 Effect 根据 State 同步浏览器标题；第二个 Effect 建立外部计时器并在 cleanup 中清除。计时器回调使用函数式更新，因此无需读取 `seconds`，依赖数组可以保持为空。

## 11. 常见错误与调查方法

- 无限循环：Effect 中更新某个 State，同时依赖每次更新都会变化的值。
- 读取旧参数：为了“只执行一次”遗漏依赖。
- 计时器越来越快：没有 cleanup，或 cleanup 清除的不是同一 timer ID。
- resize 处理多次触发：监听器重复注册但没有移除。
- 快速切换员工显示错人：旧请求未取消或未忽略。
- 开发环境请求两次：先检查 cleanup 和幂等边界，不要立即移除 StrictMode。
- Effect 太多：检查派生计算和用户事件是否被错误放进 Effect。

调查时可以：

1. 在 setup 和 cleanup 分别记录依赖值。
2. 检查 Hooks Linter 提示。
3. 在 Network 查看请求开始、取消和返回顺序。
4. 确认每个外部资源是否有对应撤销操作。

## 12. 练习

1. 根据当前页面标题 State 同步 `document.title`。
2. 创建每秒更新的计时器，并在组件隐藏后确认计时停止。
3. 注册 resize 监听器，显示窗口宽度并正确清理。
4. 实现随 `employeeId` 变化的请求，快速切换 ID 并观察旧请求取消。
5. 把一个 `Effect + setFilteredEmployees` 改成渲染时直接计算。
6. 把由保存按钮触发的 Effect 改回事件处理器。
7. 在 StrictMode 下记录 setup/cleanup 顺序，并解释结果。

## 本章检查点

- [ ] 能区分渲染计算、用户事件和外部系统同步。
- [ ] 能说明 setup、cleanup 和依赖数组各自的职责。
- [ ] Effect 读取的响应式值与依赖数组一致。
- [ ] 能为计时器、监听器和请求提供正确 cleanup。
- [ ] 能说明请求竞态，并取消或忽略旧请求。
- [ ] 能识别不需要 Effect 的派生值和提交操作。
- [ ] 能正确解释 StrictMode 的额外检查。

参考：[React：使用 Effect 进行同步](https://zh-hans.react.dev/learn/synchronizing-with-effects)、[React：Effect 的生命周期](https://zh-hans.react.dev/learn/lifecycle-of-reactive-effects)、[React：你可能不需要 Effect](https://zh-hans.react.dev/learn/you-might-not-need-an-effect)。
