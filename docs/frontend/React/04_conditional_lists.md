# 第 4 章 条件渲染与列表

## 本章目标

- 根据加载、权限、空数据等状态选择 UI。
- 用数组生成组件列表，并选择稳定的 key。
- 区分 `if`、`&&` 和三元表达式的使用场景。

## 1. 条件渲染

```tsx
if (loading) {
  return <p role="status">读取中...</p>;
}

if (error) {
  return <p role="alert">{error}</p>;
}

return (
  <>
    {canCreate && <button type="button">新增员工</button>}
    <p>{employees.length === 0 ? '没有符合条件的数据' : '检索结果'}</p>
  </>
);
```

- `if` 提前返回：整个页面处于互斥状态，最清楚。
- `condition && element`：条件成立才显示一个局部元素。
- `condition ? a : b`：两种互斥结果中必须选择一个。

当左侧可能是数字时避免 `{count && <List />}`，因为 `0` 会被显示；写成 `{count > 0 && <List />}`。

## 2. 列表渲染

```tsx
function EmployeeTable({ employees }: { employees: Employee[] }) {
  if (employees.length === 0) {
    return <p>没有员工数据</p>;
  }

  return (
    <table>
      <thead><tr><th>姓名</th><th>部门</th></tr></thead>
      <tbody>
        {employees.map((employee) => (
          <EmployeeRow key={employee.id} employee={employee} />
        ))}
      </tbody>
    </table>
  );
}
```

```text
Employee[] → map → React element[] → UI 列表
```

`key` 让 React 在同一父节点下识别每一项。优先使用数据库 ID 等稳定业务标识。数组索引在插入、删除、排序后可能指向别的数据，带输入框或局部状态的行尤其容易出现状态错位。`key` 不会作为普通 Props 自动传入，需要 ID 时应另外传 `employee.id` 或整个对象。

## 3. 常见错误与练习

- `map` 的箭头函数用了 `{}` 却没有 `return`：列表为空白。
- key 使用 `Math.random()`：每次渲染都变化，组件会被反复重建。
- 只处理成功列表：必须同时设计 loading、error、empty 和 success。
- 用隐藏按钮代替权限：隐藏只影响 UI，后端仍需授权。

练习：

1. 渲染 3 名员工并用 ID 作为 key。
2. 增加空数据提示和“仅 ACTIVE”条件显示。
3. 先用 index 作为 key，在首位插入可编辑行并观察问题，再改为 ID。
4. 为列表补充语义表头，并用键盘和浏览器 Elements 检查结构。

## 本章检查点

- [ ] 能按场景选择 if、&& 或三元表达式。
- [ ] 能用稳定业务 ID 作为 key，并解释 index 风险。
- [ ] 列表同时处理 loading、error、empty 与 success。
