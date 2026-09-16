# 第 6 章 对象、数组状态与重新渲染

## 本章目标

- 不直接修改对象或数组 State。
- 用替换、追加、过滤和映射表达业务变化。
- 解释初次渲染、重新渲染与提交。

## 1. 对象 State

```tsx
const [employee, setEmployee] = useState<Employee>({
  id: 1,
  name: 'Tom',
  email: 'tom@example.com',
  department: 'Sales',
  role: 'USER',
  joinedDate: '2026-04-01',
  status: 'ACTIVE',
});
```

错误写法直接改变旧对象：

```tsx
employee.name = 'Mike';
```

正确写法创建下一份对象：

```tsx
setEmployee((previous) => ({
  ...previous,
  name: 'Mike',
}));
```

展开语法是浅拷贝。嵌套对象需要逐层复制，或重新设计扁平状态；不要误以为 `...` 会深拷贝所有层。

## 2. 数组 State

```tsx
setEmployees((previous) => [...previous, newEmployee]);

setEmployees((previous) =>
  previous.filter((item) => item.id !== targetId),
);

setEmployees((previous) =>
  previous.map((item) =>
    item.id === targetId ? { ...item, name: 'Mike' } : item,
  ),
);
```

避免对 State 原数组使用 `push`、`pop`、`splice` 或原地 `sort`。需要排序时先复制：`[...employees].sort(compareFn)`；如果只为显示排序，直接把结果作为派生值，不必再同步回 State。

## 3. Render / Re-render

```text
初次：createRoot.render → 调用组件函数 → 得到 JSX → 提交 DOM

更新：State/Props/Context 变化
                  ↓
          再次调用组件函数
                  ↓
             得到新 JSX
                  ↓
          提交必要的 DOM 变化
```

父组件重新渲染时，默认会计算其子组件。组件函数再次执行不等于真实 DOM 全部重建。渲染阶段应只计算；不要在其中请求 API、写存储或修改外部变量。

开发环境的 `StrictMode` 可能额外调用渲染逻辑，以暴露不纯代码；这不是生产环境“随机渲染”。修复副作用，而不是移除严格模式掩盖问题。

### 3.1 第一次观察组件树

安装浏览器的 React DevTools 后打开 Components 面板，可以选择组件并观察它的 Props、State 和父子关系。修改员工姓名时，先记录哪些组件重新执行，再检查对应 DOM 是否真的变化。这里仅用于验证 Render 概念；第 23 章会把 React DevTools 与 Router、Service、Network 组合成完整调查路径。

## 4. 常见错误与练习

- 直接修改后 Setter 传回同一对象：React 可能按 `Object.is` 判断没有变化。
- 为每个派生值建立 State：形成互相同步的多份事实来源。
- 把重新渲染等同于 DOM 全量刷新：混淆计算与提交。

练习：实现员工追加、删除、改名和按姓名排序；每一步都确认旧数组未被修改。使用 React DevTools 或日志观察父子组件函数执行，并说明实际 DOM 是否全部变化。

## 本章检查点

- [ ] 能用替换方式更新对象和数组 State。
- [ ] 能区分组件函数重新执行与 DOM 全量重建。
- [ ] 能在 React DevTools 中找到组件的 Props、State 和父子关系。
