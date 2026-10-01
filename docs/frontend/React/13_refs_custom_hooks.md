# 第 13 章 useRef 与自定义 Hook

## 本章目标

- 用 Ref 获取 DOM 或保存不影响画面的可变值。
- 区分 useRef 与 useState。
- 把可复用状态逻辑提取为自定义 Hook。

`useState` 保存会影响画面的数据；`useRef` 保存一个在多次渲染之间保持不变、但修改后不要求立即重绘画面的容器。

```jsx
const inputRef = useRef(null);
```

`useRef(initialValue)` 返回 `{ current: initialValue }`。这个对象在组件后续渲染中保持同一个引用。修改 `current` 不会触发重新渲染。

## 1. 获取 DOM

```jsx
const inputRef = useRef(null);

function focusKeyword() {
  inputRef.current?.focus();
}

<input ref={inputRef} aria-label="员工姓名" />
```

首次渲染期间 `inputRef.current` 是 `null`；React 把 `<input>` 放入 DOM 后，才把真实元素写入 `current`。因此通常在点击事件或 Effect 中使用，并通过 `?.` 处理尚未存在的情况。

使用 Ref 处理焦点、测量或非 React 小部件。能通过 Props/State 声明的 DOM 内容不要用 Ref 手工修改，否则 React 与手工 DOM 会争夺事实来源。

## 2. 保存不触发渲染的数据

```jsx
const requestIdRef = useRef(0);

async function search() {
  const requestId = ++requestIdRef.current;
  const result = await getEmployees();
  if (requestId === requestIdRef.current) setEmployees(result);
}
```

Ref 变化不会触发重新渲染。画面需要显示的值必须使用 State；计时器 ID、以前的值或外部实例可使用 Ref。不要在渲染中随意读写 `ref.current`。

### 2.1 State 与 Ref 的选择

| 问题 | 使用 State | 使用 Ref |
| --- | --- | --- |
| 值变化后画面是否要更新 | 是 | 否 |
| 是否跨渲染保留 | 是 | 是 |
| 常见用途 | 输入值、loading、列表 | DOM、timer ID、请求序号 |

计时器示例：

```jsx
const timerRef = useRef(null);

function startMessageTimer() {
  window.clearTimeout(timerRef.current);
  timerRef.current = window.setTimeout(() => {
    setMessage('');
  }, 3000);
}

useEffect(() => {
  return () => window.clearTimeout(timerRef.current);
}, []);
```

cleanup 在组件卸载时清理计时器，避免已经离开的页面继续执行旧任务。

## 3. 自定义 Hook

组件负责 UI，自定义 Hook 复用有状态逻辑：

自定义 Hook 是普通 JavaScript 函数，但名称必须以 `use` 开头，并且可以在内部调用其他 Hook。它不返回 JSX，而是返回组件需要的数据和操作。

```text
输入：查询条件
  ↓
useEmployees：管理请求、状态、取消
  ↓
输出：state、reload
  ↓
Page：决定显示 Loading、Error、Empty 或 List
```

下面使用带 `status` 的对象表示请求状态。第 17～18 章会把状态结构与错误对象统一到项目共通文件中，第 25～26 章正式项目继续复用相同约定。

```jsx
function useEmployees({ keyword, department, page }) {
  const [state, setState] = useState({
    status: 'loading',
  });

  const reload = useCallback(async (signal) => {
    setState({ status: 'loading' });
    try {
      const data = await searchEmployees({
        keyword,
        department,
        page,
        size: 10,
      }, signal);
      if (!signal?.aborted) {
        setState({ status: 'success', data });
      }
    } catch {
      if (!signal?.aborted) {
        setState({ status: 'error', error: '读取失败' });
      }
    }
  }, [keyword, department, page]);

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);

    return () => controller.abort();
  }, [reload]);

  return { state, reload };
}
```

这里沿用第 11 章的 `searchEmployees(query, signal)`。组件卸载或查询条件变化时，cleanup 中止旧请求；中止后即使 Promise 进入失败分支，也不会写入过期状态。请求逻辑移入 Hook 不代表可以省略取消和竞态处理。

这里的 `useCallback` 让 `reload` 在依赖不变时保持同一函数引用，使 Effect 不会因函数引用每次变化而重复执行。它只应在依赖稳定或经过测量确有优化需要时使用，不要把它加到所有函数上。自定义 Hook 名称以 `use` 开头，并遵守 Hook 规则。它复用逻辑，不自动共享 State；两个组件各调用一次会得到两份独立状态。纯格式化函数应放 `utils`，不要伪装成 Hook。

调用示例：

```jsx
const { state, reload } = useEmployees({
  keyword,
  department,
  page,
});
```

Hook 内部只把 `keyword`、`department`、`page` 这些原始值放入依赖。不要把每次渲染都会重新创建的整个对象直接作为 Effect 依赖，否则可能重复请求。也不要为了“所有逻辑都复用”而把只使用一次的简单 State 强行提取成 Hook。

## 4. Hook 规则

- 只在 React 函数组件或自定义 Hook 的顶层调用 Hook。
- 不在 `if`、循环、事件函数或普通工具函数中调用 Hook。
- 每次渲染都保持 Hook 的调用顺序一致。
- 自定义 Hook 用 `use` 开头，让工具能够检查规则。

错误示例：

```jsx
if (isLoggedIn) {
  const [user, setUser] = useState(null);
}
```

条件变化会改变 Hook 调用顺序。应始终调用 Hook，再根据状态决定渲染内容。

## 5. 常见错误

- 把请求提取到 Hook 后删除 AbortController：组件卸载或参数切换时仍会产生旧结果覆盖。
- 用 Ref 保存需要显示的 loading：Ref 更新不触发画面变化，应使用 State。
- 两个组件调用同一 Hook却期待自动共享数据：每次调用都有独立状态，需要共享时再评估 Context、Store 或服务器状态库。

## 6. 练习

1. 页面打开后聚焦搜索框。
2. 用 Ref 保存 timer ID 并正确清理。
3. 提取 `useEmployees`，让 Page 只负责四种 UI。
4. 两个组件调用同一个 Hook，观察状态是否共享并解释。

## 本章检查点

- [ ] 能区分 State 与 Ref 对重新渲染的影响。
- [ ] 能写出遵守 Hook 规则的自定义 Hook。
- [ ] 能在自定义请求 Hook 中取消旧请求并阻止过期状态更新。
