# 第 13 章 useRef 与自定义 Hook

## 本章目标

- 用 Ref 获取 DOM 或保存不影响画面的可变值。
- 区分 useRef 与 useState。
- 把可复用状态逻辑提取为自定义 Hook。

## 1. 获取 DOM

```tsx
const inputRef = useRef<HTMLInputElement>(null);

function focusKeyword() {
  inputRef.current?.focus();
}

<input ref={inputRef} aria-label="员工姓名" />
```

使用 Ref 处理焦点、测量或非 React 小部件。能通过 Props/State 声明的 DOM 内容不要用 Ref 手工修改，否则 React 与手工 DOM 会争夺事实来源。

## 2. 保存不触发渲染的数据

```tsx
const requestIdRef = useRef(0);

async function search() {
  const requestId = ++requestIdRef.current;
  const result = await getEmployees();
  if (requestId === requestIdRef.current) setEmployees(result);
}
```

Ref 变化不会触发重新渲染。画面需要显示的值必须使用 State；计时器 ID、以前的值或外部实例可使用 Ref。不要在渲染中随意读写 `ref.current`。

## 3. 自定义 Hook

组件负责 UI，自定义 Hook 复用有状态逻辑：

下面的 `AsyncState<T>` 是本章为了练习取消请求而定义的独立最小示例，错误固定为 `string`；它不是最终项目类型。第 17～18 章会建立共享的 `AsyncState<T, E>` 与 `AppError`，第 25～26 章正式项目统一使用 `AsyncState<T, AppError>`。

```tsx
type AsyncState<T> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: string };

function useEmployees() {
  const [state, setState] = useState<AsyncState<Employee[]>>({
    status: 'loading',
  });

  const reload = useCallback(async (signal?: AbortSignal) => {
    setState({ status: 'loading' });
    try {
      const data = await getEmployees(signal);
      if (!signal?.aborted) {
        setState({ status: 'success', data });
      }
    } catch {
      if (!signal?.aborted) {
        setState({ status: 'error', error: '读取失败' });
      }
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);

    return () => controller.abort();
  }, [reload]);

  return { state, reload };
}
```

`getEmployees` 沿用第 10 章的签名并接收 `AbortSignal`。组件卸载时，cleanup 中止未完成请求；中止后即使 Promise 进入失败分支，也不会写入过期状态。请求逻辑移入 Hook 不代表可以省略取消和竞态处理。

这里的 `useCallback` 让 `reload` 在依赖不变时保持同一函数引用，使 Effect 不会因函数引用每次变化而重复执行；第 19 章会说明它只应在依赖或性能确有需要时使用。自定义 Hook 名称以 `use` 开头，并遵守 Hook 规则。它复用逻辑，不自动共享 State；两个组件各调用一次会得到两份独立状态。纯格式化函数应放 `utils`，不要伪装成 Hook。

## 4. 常见错误

- 把请求提取到 Hook 后删除 AbortController：组件卸载或参数切换时仍会产生旧结果覆盖。
- 用 Ref 保存需要显示的 loading：Ref 更新不触发画面变化，应使用 State。
- 两个组件调用同一 Hook却期待自动共享数据：每次调用都有独立状态，需要共享时再评估 Context、Store 或服务器状态库。

## 5. 练习

1. 页面打开后聚焦搜索框。
2. 用 Ref 保存 timer ID 并正确清理。
3. 提取 `useEmployees`，让 Page 只负责四种 UI。
4. 两个组件调用同一个 Hook，观察状态是否共享并解释。

## 本章检查点

- [ ] 能区分 State 与 Ref 对重新渲染的影响。
- [ ] 能写出遵守 Hook 规则的自定义 Hook。
- [ ] 能在自定义请求 Hook 中取消旧请求并阻止过期状态更新。
