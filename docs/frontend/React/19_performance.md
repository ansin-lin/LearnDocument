# 第 19 章 性能分析与优化

## 本章目标

- 按“发现 → 测量 → 定位 → 优化 → 再测量”处理性能问题。
- 理解 React.memo、useMemo 和 useCallback 的用途与成本。
- 避免把缓存 Hook 当默认模板。

本章属于“会使用、能看懂”的工程能力。页面功能、错误处理和测试先正确完成；只有出现可测量的问题时，才实施优化。

## 1. 先确认问题

使用 React DevTools Profiler 记录真实交互：哪个提交慢、哪些组件反复渲染、计算还是 DOM 才是瓶颈。开发模式额外检查会影响日志次数，不应只凭 `console.log` 判断生产性能。

Profiler 的基本流程：

1. 打开 React DevTools 的 Profiler；
2. 开始记录；
3. 执行一次明确操作，例如输入搜索关键字；
4. 停止记录；
5. 查看 Commit 时间和重复渲染组件；
6. 只修改确认的瓶颈；
7. 用同一数据、同一操作再次记录。

优化前后必须保留相同条件的证据。一次偶然更快不能证明修改有效。

## 2. React.memo

```jsx
const EmployeeRow = memo(function EmployeeRow({ employee }) {
  return <tr><td>{employee.name}</td></tr>;
});
```

当父组件频繁渲染、子组件成本明显、且 Props 大多稳定时，memo 可以跳过部分重新渲染。它是性能优化，不是正确性保证；总是新建的对象或函数会破坏浅比较收益。

`memo()` 比较新旧 Props。父组件重新渲染不等于子组件一定需要跳过；普通轻量组件重新渲染的成本可能低于比较和维护成本。

## 3. useMemo

```jsx
const sortedEmployees = useMemo(
  () => expensiveSort(employees, sort),
  [employees, sort],
);
```

它缓存计算结果。只有计算确实昂贵或稳定引用对 memo 子组件有价值时使用。普通筛选往往无需缓存。

## 4. useCallback

```jsx
const handleDelete = useCallback((id) => {
  setEmployees((items) => items.filter((item) => item.id !== id));
}, []);
```

它缓存函数引用，不缓存执行结果。常与 memo 子组件或 Hook 依赖配合；若下游不比较引用，增加 useCallback 反而只会提高阅读成本。

## 5. 更常见的收益

- 避免重复请求和不必要 Effect。
- 分页或虚拟化超大列表。
- 把状态放近使用处，缩小更新范围。
- 懒加载大型页面模块。
- 减少过大的 Context value 更新范围。

React 新版本可能提供编译器优化，但项目是否启用、可支持哪些模式必须以当前构建配置和官方文档为准，不应因此忽略纯渲染和测量。

### 5.1 页面模块懒加载

大型且不常访问的页面可以按路由拆分：

```jsx
import { lazy, Suspense } from 'react';

const EmployeeEditPage = lazy(() => import('./pages/EmployeeEditPage.jsx'));

<Suspense fallback={<p role="status">页面读取中...</p>}>
  <EmployeeEditPage />
</Suspense>
```

`lazy()` 延迟加载模块，`Suspense` 在模块到达前显示备用 UI。是否拆分应结合构建产物和实际访问路径判断，不要把每个小组件都拆成独立网络请求。

### 5.2 大列表先考虑数据量

如果后端已经分页，一般先确保只请求当前页。只有必须同时显示大量行时，再评估虚拟化。不要先用 `memo` 掩盖一次渲染数万条数据的设计问题。

## 6. 优化前后的确认表

| 项目 | 优化前 | 优化后 |
| --- | --- | --- |
| 操作与测试数据 | 必须相同 | 必须相同 |
| 最慢 Commit | 记录时间 | 记录时间 |
| 重复渲染组件 | 记录名称和次数 | 再次记录 |
| 功能测试 | 通过 | 必须仍通过 |
| 维护成本 | 当前代码 | 是否值得增加复杂度 |

## 7. 常见错误

- 没有性能证据就给所有组件加 memo：比较和维护本身也有成本。
- 用 useMemo 修复错误结果：缓存不是正确性工具。
- 使用 useCallback 但下游不比较引用：没有收益却增加依赖维护。
- 只看开发环境日志次数：应使用 Profiler 记录实际提交。

## 8. 练习

1. 用 Profiler 记录一次搜索输入，找出最慢提交。
2. 给慢计算加 useMemo，比较前后证据。
3. 删除一个没有收益的 useCallback 并说明理由。
4. 排查大列表卡顿，分别评估分页、虚拟化和 memo。

## 本章检查点

- [ ] 能按发现、测量、定位、优化、复测的顺序工作。
- [ ] 能分别解释 memo、useMemo 与 useCallback 缓存什么。
- [ ] 不把性能 Hook 当作默认编码模板。
