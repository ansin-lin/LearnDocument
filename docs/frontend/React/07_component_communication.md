# 第 7 章 组件通信与状态提升

## 本章目标

- 【必须掌握】区分父传子的数据 Props 和子通知父的回调 Props。
- 【必须掌握】让子组件报告用户意图，由父组件更新 State。
- 【必须掌握】把兄弟组件需要共享的 State 提升到最近共同父组件。
- 【必须掌握】使用唯一事实来源避免重复 State。
- 【必须掌握】根据数据使用范围判断 State 应放在哪里。

## 1. 组件之间为什么需要通信

组件拆分后仍需共同完成一个业务流程。例如：

```text
EmployeeListPage 保存员工数组
       ↓
EmployeeList 显示列表
       ↓
EmployeeCard 提供删除按钮
```

员工数据属于页面，但删除按钮位于子组件。子组件不能直接修改父组件的 State，因此需要清楚的数据和事件通道：

```text
父组件 ──数据 Props──▶ 子组件
父组件 ◀─回调 Props── 子组件的用户操作
```

数据向下传递，事件意图向上报告，这是 React 常见的单向数据流。

## 2. 父组件向子组件传递数据

第三章已经学习数据 Props：

```jsx
function EmployeeCard({ employee }) {
  return (
    <article>
      <h3>{employee.name}</h3>
      <p>{employee.department}</p>
    </article>
  );
}

<EmployeeCard employee={employee} />
```

`EmployeeCard` 读取 `employee`，但不修改它。真正的数据所有者仍是父组件。

## 3. 子组件如何通知父组件

父组件可以把函数作为 Prop 传入：

```jsx
function EmployeeCard({ employee, onDelete }) {
  return (
    <article>
      <h3>{employee.name}</h3>
      <button
        type="button"
        onClick={() => onDelete(employee.id)}
      >
        删除
      </button>
    </article>
  );
}
```

父组件定义真正的更新逻辑：

```jsx
function EmployeePage() {
  const [employees, setEmployees] = useState(initialEmployees);

  function handleDelete(employeeId) {
    setEmployees((previous) =>
      previous.filter((employee) => employee.id !== employeeId),
    );
  }

  return (
    <EmployeeCard
      employee={employees[0]}
      onDelete={handleDelete}
    />
  );
}
```

完整执行顺序：

```text
父组件把 handleDelete 作为 onDelete 传入
       ↓
子组件点击删除按钮
       ↓
子组件调用 onDelete(employee.id)
       ↓
父组件的 handleDelete(employee.id) 执行
       ↓
父组件更新 employees State
       ↓
父子组件使用新数据重新渲染
```

子组件只报告“用户请求删除哪个员工”，父组件决定如何更新数据。接入后端后，父组件或业务 Hook 还可以先调用 API，成功后再更新列表。

### 3.1 回调 Props 的命名

常见命名方式：

- 父组件内部处理函数：`handleDelete`、`handleSearch`。
- 传给子组件的 Prop：`onDelete`、`onSearch`。

`on...` 表示组件对外提供的事件接口，`handle...` 表示当前组件如何处理该事件。这不是语法强制要求，但统一命名能让数据流更容易追踪。

### 3.2 回调应该传什么参数

子组件应传达必要的业务信息：

```jsx
onDelete(employee.id);
onStatusChange(employee.id, 'INACTIVE');
onSelect(employee);
```

不要把子组件内部 DOM 结构暴露给父组件，例如要求父组件从按钮文字或 DOM 属性猜测员工 ID。回调参数应直接表达业务含义。

## 4. 让列表中的子组件报告操作

```jsx
function EmployeeList({ employees, onDelete, onStatusChange }) {
  if (employees.length === 0) {
    return <p>没有员工数据。</p>;
  }

  return (
    <ul>
      {employees.map((employee) => (
        <li key={employee.id}>
          <EmployeeCard
            employee={employee}
            onDelete={onDelete}
            onStatusChange={onStatusChange}
          />
        </li>
      ))}
    </ul>
  );
}
```

`EmployeeList` 自己不处理删除，只把页面传入的回调继续交给 `EmployeeCard`。这种一两层的 Props 传递很正常，不需要立刻引入全局 Store。

回调函数不要在渲染时调用：

```jsx
// 错误：渲染 EmployeeCard 时就执行删除
<button onClick={onDelete(employee.id)}>删除</button>

// 正确：点击时才调用
<button onClick={() => onDelete(employee.id)}>删除</button>
```

## 5. 什么是状态提升

假设 `EmployeeSearch` 保存一份关键字，`EmployeeList` 也保存一份关键字：

```text
EmployeeSearch  keyword = '田中'
EmployeeList    keyword = ''
```

两份 State 可能不一致。兄弟组件需要读取或修改同一个值时，应把 State 移到它们最近的共同父组件，这叫状态提升（Lifting State Up）。

```text
         EmployeeListPage
         keyword / setKeyword
              │
       ┌──────┴──────┐
       ↓             ↓
EmployeeSearch   EmployeeList
修改 keyword     使用 keyword 的结果
```

父组件：

```jsx
function EmployeeListPage() {
  const [keyword, setKeyword] = useState('');

  const filteredEmployees = employees.filter((employee) =>
    employee.name.toLowerCase().includes(keyword.toLowerCase()),
  );

  return (
    <>
      <EmployeeSearch
        keyword={keyword}
        onKeywordChange={setKeyword}
      />
      <EmployeeList employees={filteredEmployees} />
    </>
  );
}
```

搜索组件：

```jsx
function EmployeeSearch({ keyword, onKeywordChange }) {
  return (
    <label>
      员工姓名
      <input
        value={keyword}
        onChange={(event) => onKeywordChange(event.target.value)}
      />
    </label>
  );
}
```

`EmployeeSearch` 不再保存自己的关键字，它由父组件的 `keyword` 控制，并通过 `onKeywordChange` 请求父组件更新。这种组件也称为受控组件。

## 6. 唯一事实来源

同一业务数据应有一个明确所有者，也就是唯一事实来源（Single Source of Truth）。

不推荐把 Props 直接复制到 State：

```jsx
function EmployeeSearch({ keyword }) {
  const [localKeyword, setLocalKeyword] = useState(keyword);
  // keyword 后续变化时，localKeyword 不会自动同步
}
```

此时出现父级 `keyword` 和子级 `localKeyword` 两份来源。应根据需求选择：

- 子组件始终跟随父组件：直接使用 Props。
- 子组件编辑独立草稿：明确创建 Draft State，并设计保存、取消和重新初始化规则。

“复制 Props 到 State”不是绝对禁止，但必须有清楚的草稿语义，不能只是为了方便读取。

## 7. State 应该放在哪里

按下面顺序判断：

1. 只有一个组件使用：放在该组件中。
2. 多个兄弟组件共享：提升到最近共同父组件。
3. 一小片组件树跨多层共享：评估组合或 Context。
4. 跨页面、有较多业务 Action：再评估全局 Store。
5. 来自服务器并需要缓存、失效和重新读取：按服务器状态管理。

| State 示例 | 推荐位置 |
| --- | --- |
| 当前 Dialog 是否打开 | 使用该 Dialog 的页面或组件 |
| 表单输入草稿 | 表单组件或其父页面 |
| 搜索框与列表共享的关键字 | 最近共同父组件 |
| 当前登录用户 | Auth Context 或 Store |
| API 员工列表 | 页面请求 Hook、Store 或服务器状态方案 |

状态应尽量靠近真正使用它的位置，但不能为了“靠近”而复制多份。

## 8. 组合可以减少无意义透传

如果中间组件只负责把一段 UI 继续传下去，可以使用 `children`：

```jsx
function EmployeePageLayout({ children }) {
  return (
    <main className="employee-page">
      {children}
    </main>
  );
}

<EmployeePageLayout>
  <EmployeeList employees={employees} />
</EmployeePageLayout>
```

这不代表所有 Props drilling 都是错误。业务数据经过清楚的两三层传递通常容易追踪；只有共享范围和维护成本真正扩大时，再使用 Context 或 Store。

## 9. 完整可运行示例

新建 `src/components/EmployeeSearch.jsx`：

```jsx
export function EmployeeSearch({ keyword, onKeywordChange }) {
  return (
    <label>
      员工姓名
      <input
        value={keyword}
        onChange={(event) => onKeywordChange(event.target.value)}
      />
    </label>
  );
}
```

在 `EmployeeCard.jsx` 增加回调：

```jsx
export function EmployeeCard({ employee, onDelete, onStatusChange }) {
  const nextStatus = employee.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

  return (
    <article>
      <h3>{employee.name}</h3>
      <p>部门：{employee.department}</p>
      <p>状态：{employee.status}</p>
      <button
        type="button"
        onClick={() => onStatusChange(employee.id, nextStatus)}
      >
        切换状态
      </button>
      <button type="button" onClick={() => onDelete(employee.id)}>
        删除
      </button>
    </article>
  );
}
```

将 `src/components/EmployeeList.jsx` 更新为：

```jsx
import { EmployeeCard } from './EmployeeCard.jsx';

export function EmployeeList({ employees, onDelete, onStatusChange }) {
  if (employees.length === 0) return <p>没有符合条件的员工。</p>;

  return (
    <ul>
      {employees.map((employee) => (
        <li key={employee.id}>
          <EmployeeCard
            employee={employee}
            onDelete={onDelete}
            onStatusChange={onStatusChange}
          />
        </li>
      ))}
    </ul>
  );
}
```

然后在 `App.jsx` 统一保存 State：

```jsx
import { useState } from 'react';
import { EmployeeList } from './components/EmployeeList.jsx';
import { EmployeeSearch } from './components/EmployeeSearch.jsx';

const initialEmployees = [
  { id: 1001, name: '田中太郎', department: '営業部', status: 'ACTIVE' },
  { id: 1002, name: '佐藤花子', department: '開発部', status: 'ACTIVE' },
];

export default function App() {
  const [employees, setEmployees] = useState(initialEmployees);
  const [keyword, setKeyword] = useState('');

  const filteredEmployees = employees.filter((employee) =>
    employee.name.toLowerCase().includes(keyword.trim().toLowerCase()),
  );

  function handleDelete(employeeId) {
    setEmployees((previous) =>
      previous.filter((employee) => employee.id !== employeeId),
    );
  }

  function handleStatusChange(employeeId, nextStatus) {
    setEmployees((previous) =>
      previous.map((employee) =>
        employee.id === employeeId
          ? { ...employee, status: nextStatus }
          : employee,
      ),
    );
  }

  return (
    <main>
      <h1>员工管理</h1>
      <EmployeeSearch
        keyword={keyword}
        onKeywordChange={setKeyword}
      />
      <p>符合条件：{filteredEmployees.length} 人</p>
      <EmployeeList
        employees={filteredEmployees}
        onDelete={handleDelete}
        onStatusChange={handleStatusChange}
      />
    </main>
  );
}
```

本例的唯一事实来源：`employees` 和 `keyword` 都位于 `App`。搜索组件修改关键字，列表读取筛选结果，卡片报告删除和状态切换，但真正更新仍由 `App` 完成。

## 10. 常见错误与练习

- 点击前就执行回调：检查是否写成 `onClick={onDelete(id)}`。
- 回调收到事件对象而不是 ID：需要使用包装函数 `() => onDelete(id)`。
- 搜索框和列表条件不一致：检查是否存在两份 keyword State。
- 子组件直接修改数组：更新应交给 State 所有者。
- Props 传递两层就使用全局 Store：先确认普通单向数据流是否已经足够。
- 父组件越来越大：可提取业务 Hook 或按职责拆分，但不要复制 State。

练习：

1. 让 `EmployeeCard` 通过回调请求删除和切换状态。
2. 让搜索框、结果数量和列表共享同一 `keyword`。
3. 增加“清除搜索条件”按钮，并确认只更新唯一 State。
4. 设计编辑草稿的 State 位置，说明保存和取消时的数据流。
5. 故意让子组件保存第二份 keyword，观察不一致后改为受控组件。
6. 画出一次删除操作从按钮到父级 State 再回到 UI 的完整路径。

## 本章检查点

- [ ] 能画出 data Props 向下、callback Props 向上的数据流。
- [ ] 能让子组件传递业务 ID，由父组件更新 State。
- [ ] 能把兄弟组件共享的 State 提升到最近共同父组件。
- [ ] 能识别重复 State，并建立唯一事实来源。
- [ ] 能根据共享范围判断 Local State、Context、Store 和服务器状态。

参考：[React：组件间共享 State](https://zh-hans.react.dev/learn/sharing-state-between-components)、[React：将 Props 传递给组件](https://zh-hans.react.dev/learn/passing-props-to-a-component)。
