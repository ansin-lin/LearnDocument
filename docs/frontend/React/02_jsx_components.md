# 第 2 章 JSX 与组件

## 本章目标

- 用 JSX 表达动态 UI，并说明它与 HTML 的关键差异。
- 创建、导出、导入和组合函数组件。
- 根据页面职责识别 Page、Business 和 Common Component。

## 1. 从实际问题开始

员工页包含标题、搜索区、表格和分页。如果全部写在一个函数里，修改与排查会越来越困难。React 用组件把 UI 和相关逻辑拆成可命名的单位。

```tsx
export function PageHeader() {
  return <header><h1>员工管理</h1></header>;
}
```

组件名称必须大写。小写标签会被当作浏览器原生元素。

## 2. JSX 不是 HTML 文件

JSX 是 TypeScript/JavaScript 中描述 UI 的语法。常见规则：

- 使用 `className`，不是 HTML 的 `class`。
- 属性通常使用 camelCase，如 `onClick`、`tabIndex`。
- JavaScript 表达式放在 `{}` 中；`if` 语句不能直接塞进 JSX 表达式位置。
- 组件必须返回一个根节点，可用 `<>...</>` Fragment 避免无意义容器。
- 标签必须正确闭合；可访问性仍应优先使用 `button`、`label`、`table` 等语义元素。

```tsx
const systemName = 'Employee Management System';

function App() {
  const formatTitle = (name: string) => name.toUpperCase();

  return (
    <>
      <h1 className="page-title">{formatTitle(systemName)}</h1>
      <p>当前日期：{new Date().toLocaleDateString('zh-CN')}</p>
    </>
  );
}
```

`{}` 接受能计算出值的表达式。不要在渲染中执行请求、写存储或修改外部变量；组件渲染应保持纯粹。

## 3. 组合页面

```tsx
function SearchArea() {
  return <section aria-label="员工搜索">搜索条件</section>;
}

function EmployeeTable() {
  return <section aria-label="员工列表">员工列表</section>;
}

export default function EmployeeListPage() {
  return (
    <main>
      <PageHeader />
      <SearchArea />
      <EmployeeTable />
    </main>
  );
}
```

```text
EmployeeListPage（Page）
├─ PageHeader（Common）
├─ SearchArea（Business）
└─ EmployeeTable（Business）
```

Page 对应路由页面；Business 表达当前业务；Common 是跨业务仍有明确复用价值的通用 UI。不要只因代码有几行就机械拆分。

## 4. 常见错误与练习

- `employeeCard` 作为标签：React 把它当 HTML 标签；改为 `EmployeeCard`。
- 相邻元素无共同根：使用语义容器或 Fragment。
- 在 JSX 写 `class`：改为 `className`。
- 在组件顶层调用修改状态的函数：会造成渲染循环，修改动作应由事件或 Effect 的同步需求触发。

练习：

1. 创建 `PageHeader.tsx` 并从 `App.tsx` 导入。
2. 用 Fragment 组合 Header、Main、Footer。
3. 把当前大组件拆成搜索区和表格，并说明每个组件职责。
4. 制造一个未闭合标签错误，用编译信息定位并修复。

参考：[React：创建和嵌套组件](https://zh-hans.react.dev/learn#components-ui-building-blocks)。

## 本章检查点

- [ ] 能区分 JSX、HTML 与组件函数。
- [ ] 能正确导入、组合并命名函数组件。
- [ ] 能根据页面、业务和复用职责说明组件边界。
