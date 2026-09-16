# 第 3 章 Props 与组件拆分

## 本章目标

- 从父组件向子组件传递只读数据。
- 使用 TypeScript 定义 Props、可选值、默认值和 `children`。
- 根据数据职责拆分员工行组件。

## 1. 为什么需要 Props

父组件持有员工数据，子组件负责显示一行。Props 是父组件给子组件的输入：

```text
Parent
   │ props（只读）
   ↓
Child
```

```tsx
import type { ReactNode } from 'react';

type EmployeeSummary = {
  id: number;
  name: string;
  department: string;
};

type EmployeeRowProps = {
  employee: EmployeeSummary;
  showDepartment?: boolean;
};

function EmployeeRow({
  employee,
  showDepartment = true,
}: EmployeeRowProps) {
  return (
    <tr>
      <td>{employee.name}</td>
      {showDepartment && <td>{employee.department}</td>}
    </tr>
  );
}
```

调用处：

```tsx
<EmployeeRow employee={employee} showDepartment={false} />
```

`showDepartment?` 表示可省略，参数解构中的 `= true` 提供默认值。不要在子组件中执行 `employee.name = ...`；Props 属于调用方，修改数据应由拥有状态的组件决定。

`EmployeeSummary` 是本章独立显示示例所需的最小类型。贯穿项目仍以课程入口定义的完整 `Employee` 为唯一业务模型；进入 Service 和实战后统一从 `types/employee.ts` 导入，不再重复声明不同版本的 `Employee`。

## 2. children

```tsx
type PanelProps = {
  title: string;
  children: ReactNode;
};

function Panel({ title, children }: PanelProps) {
  return (
    <section>
      <h2>{title}</h2>
      {children}
    </section>
  );
}

<Panel title="检索结果">
  <EmployeeTable />
</Panel>
```

`children` 适合容器不知道内部具体内容的组合场景。不要为了“通用”给业务组件增加大量可选 Props；边界应以职责和实际复用为依据。

## 3. 数据流与实际项目

父组件每次渲染都会计算传给子组件的 Props。Props 变化时，子组件将使用新输入再次渲染。它不是子组件自己的可变仓库。

```text
EmployeeListPage 持有 employees
        ↓ employee
EmployeeTable
        ↓ employee
EmployeeRow 显示
```

## 4. 常见错误

- 修改 Props：导致数据所有权不清；由父组件更新 State。
- 类型写成 `any`：失去字段拼写和调用约束；定义明确 Props 类型。
- 同时传整个对象和所有单字段：接口重复；选择满足职责的最小一致输入。
- 把父组件内部实现细节做成 Props：组件难以理解；用业务含义命名，如 `onDelete`。

## 5. 练习

1. 为 `EmployeeRow` 增加 `status` 显示并完善类型。
2. 创建带 `children` 的 `Panel`，包装搜索区和列表。
3. 尝试修改 Props，观察 TypeScript/ESLint 提示并改为父组件负责。
4. 解释“Props 是只读输入”与“对象本身在 JavaScript 可变”并不矛盾。

## 本章检查点

- [ ] 能为 Props、可选值、默认值和 children 编写类型。
- [ ] 能解释 Parent → Props → Child 的单向数据流。
- [ ] 不在子组件中直接修改 Props。
