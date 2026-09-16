# 第 9 章 useEffect 与外部系统同步

## 本章目标

- 把 Effect 理解为与 React 外部系统同步，而不是“生命周期工具箱”。
- 正确声明依赖与清理。
- 识别不需要 Effect 的派生计算和用户事件。

## 1. 三类代码

- 渲染代码：根据 Props/State 计算 JSX，必须纯粹。
- 事件处理器：由一次明确的用户操作触发。
- Effect：页面提交后，为了当前渲染结果与外部系统保持同步而运行。

```tsx
useEffect(() => {
  document.title = `${title} | Employee System`;
}, [title]);
```

`document.title` 是浏览器外部系统，`title` 是响应式依赖。

## 2. 依赖数组

```tsx
useEffect(() => { /* 每次提交后 */ });
useEffect(() => { /* 挂载后；开发严格模式会做额外检查 */ }, []);
useEffect(() => { /* 挂载后以及 userId 变化后 */ }, [userId]);
```

依赖不是为了“控制执行次数”随意挑选，而应包含 Effect 读取的响应式值。遵守 Hook Linter；若对象或函数导致频繁运行，先检查它是否应在 Effect 内创建、是否可移出组件，或该逻辑根本不需要 Effect。

## 3. 清理

```tsx
useEffect(() => {
  const timerId = window.setInterval(refreshClock, 1000);
  return () => window.clearInterval(timerId);
}, []);
```

清理发生在下一次同步开始前以及卸载时。连接要断开、订阅要取消、计时器要清除、请求要取消或忽略旧结果。

## 4. API 请求边界

```tsx
useEffect(() => {
  const controller = new AbortController();

  async function load() {
    try {
      const response = await fetch(`/api/employees/${employeeId}`, {
        signal: controller.signal,
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      setEmployee((await response.json()) as Employee);
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        setError('员工信息读取失败');
      }
    }
  }

  void load();
  return () => controller.abort();
}, [employeeId]);
```

课程后续会把 HTTP 细节移到 Service。这里的重点是：ID 变化时旧请求不能晚到后覆盖新页面。

## 5. 不需要 Effect 的情况

```tsx
// 直接在渲染中计算，不要 Effect + setFilteredEmployees
const filtered = employees.filter((item) => item.name.includes(keyword));
```

保存按钮发出的 POST 属于明确事件，应放在提交处理器，不要先设一个 `shouldSave` 再由 Effect 观察。React 官方把 Effect 称为逃生舱口；无外部系统时通常不需要它。

## 6. 开发环境为什么可能执行两次

严格模式会在开发环境额外执行 setup → cleanup → setup，帮助发现缺少清理或不纯逻辑。正确结果应让用户无法区分执行一次还是该检查序列。不要用 Ref 阻止第二次 setup 来掩盖资源泄漏。

## 7. 常见错误与练习

- Effect 中无条件 Setter 且依赖该 State：无限循环。
- 用空数组骗过依赖：读取到陈旧 Props/State。
- 忽略旧请求：快速切换员工时显示错人。
- 用 Effect 同步两个可直接计算的 State：多一次渲染且容易失配。

练习：同步页面标题；创建可清理计时器；实现随 `employeeId` 更新且可取消的读取；把一个派生列表 Effect 改为渲染计算。参考：[使用 Effect 进行同步](https://zh-hans.react.dev/learn/synchronizing-with-effects) 和 [你可能不需要 Effect](https://zh-hans.react.dev/learn/you-might-not-need-an-effect)。

## 本章检查点

- [ ] 能区分渲染、事件与 Effect。
- [ ] Effect 依赖覆盖读取的响应式值，并提供必要 cleanup。
- [ ] 能识别派生计算和用户事件不需要 Effect。
