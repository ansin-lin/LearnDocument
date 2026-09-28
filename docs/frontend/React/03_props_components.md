# 第 3 章 Props 与组件拆分

## 本章目标

- 【必须掌握】说明 Props 是什么，以及父组件如何向子组件传递数据。
- 【必须掌握】传递字符串、数字、布尔值、变量和对象。
- 【必须掌握】使用参数解构、默认值和 `children` 接收组件输入。
- 【必须掌握】说明 Props 为什么应视为只读数据。
- 【必须掌握】根据职责、复用和数据归属拆分组件。

## 1. 为什么组件需要 Props

第二章创建的组件显示固定内容：

```jsx
function EmployeeSummary() {
  return <p>当前登记员工：12 人</p>;
}
```

如果另一个页面需要显示 25 人，再复制一个组件并把 `12` 改成 `25`，就会产生多份几乎相同的代码。

Props 用来把组件外部的数据传入组件。可以把组件理解成一个接收输入、返回 JSX 的函数：

```text
输入 Props
    ↓
Component
    ↓
输出 JSX
```

先把人数改成可传入的数据：

```jsx
function EmployeeSummary(props) {
  return <p>当前登记员工：{props.count} 人</p>;
}
```

父组件使用该组件时，通过 JSX 属性传值：

```jsx
function App() {
  return <EmployeeSummary count={12} />;
}
```

这里发生了以下过程：

```text
App 写入 count={12}
        ↓
React 组成 Props 对象 { count: 12 }
        ↓
调用 EmployeeSummary(props)
        ↓
组件读取 props.count
        ↓
显示“当前登记员工：12 人”
```

Props 是 Properties（属性）的简称。父组件负责提供值，子组件负责根据收到的值显示内容。

## 2. Props 实际上是一个对象

下面的调用传入三个 Props：

```jsx
<EmployeeCard
  name="田中太郎"
  department="営業部"
  employeeNumber={1001}
/>
```

React 调用组件时，组件接收到的内容可以理解为：

```js
{
  name: '田中太郎',
  department: '営業部',
  employeeNumber: 1001,
}
```

组件可以通过一个参数接收整个 Props 对象：

```jsx
function EmployeeCard(props) {
  return (
    <article>
      <h2>{props.name}</h2>
      <p>员工编号：{props.employeeNumber}</p>
      <p>部门：{props.department}</p>
    </article>
  );
}
```

`props` 只是常用参数名，并不是 JavaScript 关键字。虽然可以改成其他名称，但统一写成 `props` 更容易阅读。

### 2.1 使用参数解构

当组件需要多个字段时，可以在参数位置直接解构：

```jsx
function EmployeeCard({ name, department, employeeNumber }) {
  return (
    <article>
      <h2>{name}</h2>
      <p>员工编号：{employeeNumber}</p>
      <p>部门：{department}</p>
    </article>
  );
}
```

这与先接收 `props` 再读取 `props.name` 的结果相同。解构适合字段明确的组件，可以直接看出组件依赖哪些输入。

下面两种写法都正确：

```jsx
function EmployeeCard(props) {
  const { name, department } = props;
  return <p>{name} / {department}</p>;
}
```

```jsx
function EmployeeCard({ name, department }) {
  return <p>{name} / {department}</p>;
}
```

本课程后续主要使用第二种写法。

## 3. 不同种类的值如何传递

JSX 属性使用引号还是花括号，取决于传入的是固定字符串还是 JavaScript 表达式。

### 3.1 传递字符串

固定字符串可以直接使用引号：

```jsx
<EmployeeCard name="田中太郎" department="営業部" />
```

也可以使用花括号传递字符串表达式，但固定文字没有必要这样写：

```jsx
<EmployeeCard name={'田中太郎'} />
```

### 3.2 传递数字

数字放在花括号中：

```jsx
<EmployeeSummary count={12} />
```

下面传入的是字符串 `'12'`，不是数字 `12`：

```jsx
<EmployeeSummary count="12" />
```

显示文字时两者看起来可能相同，但执行 `count + 1` 时结果不同：数字得到 `13`，字符串可能得到 `'121'`。Props 的值应与组件期待的数据一致。

### 3.3 传递布尔值

```jsx
<EmployeeCard active={true} />
<EmployeeCard active={false} />
```

当值为 `true` 时，可以使用简写：

```jsx
<EmployeeCard active />
```

`active` 等价于 `active={true}`。没有写这个 Prop 不等于传入 `false`；此时收到的是 `undefined`，除非组件提供默认值。

### 3.4 传递变量和计算结果

```jsx
function App() {
  const employeeCount = 12;
  const retiredCount = 2;

  return (
    <EmployeeSummary
      count={employeeCount}
      activeCount={employeeCount - retiredCount}
    />
  );
}
```

花括号中会先执行 JavaScript 表达式，再把结果作为 Prop 传入。

### 3.5 传递对象

员工有多个相关字段时，可以把整个对象作为一个 Prop：

```jsx
function App() {
  const employee = {
    id: 1001,
    name: '田中太郎',
    department: '営業部',
    status: 'ACTIVE',
  };

  return <EmployeeCard employee={employee} />;
}
```

子组件通过 `employee` 读取字段：

```jsx
function EmployeeCard({ employee }) {
  return (
    <article>
      <h2>{employee.name}</h2>
      <p>员工编号：{employee.id}</p>
      <p>部门：{employee.department}</p>
      <p>状态：{employee.status}</p>
    </article>
  );
}
```

是否传整个对象，要根据组件职责判断：

- 组件需要员工的大部分字段：传 `employee` 对象通常更清楚。
- 组件只需要姓名：只传 `name`，可以减少依赖。
- 不要同时传 `employee` 又重复传 `name={employee.name}`，否则出现两份来源。

函数也可以作为 Prop 传递，用于把子组件操作通知父组件。这个用法会在第七章“组件通信与状态提升”中详细讲解。

## 4. 可省略的 Props 与默认值

有些显示选项可以不传。组件可以在解构时设置默认值：

```jsx
function EmployeeCard({ employee, showDepartment = true }) {
  return (
    <article>
      <h2>{employee.name}</h2>
      <p>员工编号：{employee.id}</p>
      {showDepartment && <p>部门：{employee.department}</p>}
    </article>
  );
}
```

`showDepartment && <p>...</p>` 表示值为 `true` 时显示部门，值为 `false` 时不显示。这里用它观察布尔 Props 的效果，完整的条件渲染规则会在下一章讲解。

父组件没有传 `showDepartment` 时，默认使用 `true`：

```jsx
<EmployeeCard employee={employee} />
```

明确传入 `false` 时，不显示部门：

```jsx
<EmployeeCard employee={employee} showDepartment={false} />
```

默认值只在 Prop 为 `undefined` 时生效。下面显式传入 `null`，不会自动变成 `true`：

```jsx
<EmployeeCard employee={employee} showDepartment={null} />
```

如果一个 Prop 对组件正常工作必不可少，就不要用默认值掩盖漏传。例如员工卡片没有 `employee` 就无法显示，应由调用方提供该对象。

## 5. children：把标签之间的内容传给组件

组件开始标签和结束标签之间的内容会通过特殊 Prop `children` 传入：

```jsx
function Panel({ title, children }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      <div className="panel-content">
        {children}
      </div>
    </section>
  );
}
```

使用 `Panel` 时，把内容写在标签之间：

```jsx
<Panel title="员工检索结果">
  <p>符合条件的员工共有 12 人。</p>
  <EmployeeSummary count={12} />
</Panel>
```

React 可以把上面的调用理解为：

```text
Panel 的 title    → “员工检索结果”
Panel 的 children → p 和 EmployeeSummary 组成的 JSX
```

`children` 适合 Panel、Dialog、Layout 等“负责外框，但不知道内部具体内容”的组件。容器组件负责边框、标题和布局，调用方决定里面放什么。

如果组件内部内容完全固定，就不必为了使用 `children` 而增加复杂度。

## 6. Props 是从父组件流向子组件的

React 的基本数据方向是从父组件向子组件：

```text
EmployeeListPage
      │ employee
      ↓
EmployeeCard
      │ employee.name
      ↓
浏览器画面
```

父组件传入新值后，React 会用新的 Props 再次计算子组件输出。子组件不负责保存或修改父组件的数据。

### 6.1 为什么不能修改 Props

下面的写法直接修改了父组件传入的对象：

```jsx
function EmployeeCard({ employee }) {
  // 错误：修改了调用方拥有的数据
  employee.name = '山田太郎';

  return <p>{employee.name}</p>;
}
```

问题不只是“语法不推荐”，而是数据责任变得不清楚：

- 父组件不知道自己的数据何时被改动。
- 其他使用同一对象的组件也可能突然显示新值。
- 页面结果依赖组件执行顺序，调查问题会变得困难。
- 后续使用 State 时，直接修改对象还可能无法触发正确的重新渲染。

因此应把 Props 当作只读输入。需要修改数据时，由拥有该数据的组件负责更新，再把新值向下传递。State 更新会在第五、六章讲解，子组件通知父组件的回调方式会在第七章讲解。

## 7. 组件应该如何拆分

组件拆分的目的不是增加文件数量，而是让每个组件拥有清楚的职责。

假设员工页面全部写在 `App` 中：

```jsx
function App() {
  const employee = {
    id: 1001,
    name: '田中太郎',
    department: '営業部',
    status: 'ACTIVE',
  };

  return (
    <>
      <header>
        <h1>员工管理系统</h1>
      </header>
      <main>
        <section>
          <h2>员工信息</h2>
          <article>
            <h3>{employee.name}</h3>
            <p>员工编号：{employee.id}</p>
            <p>部门：{employee.department}</p>
            <p>状态：{employee.status}</p>
          </article>
        </section>
      </main>
    </>
  );
}
```

当前代码还能阅读，但继续增加检索区、员工列表和操作按钮后，`App` 会同时承担太多职责。可以按下面的边界拆分：

```text
App                    组织整个页面
├─ PageHeader          显示系统标题
└─ Panel               提供带标题的内容区域
   └─ EmployeeCard     显示一名员工
```

### 7.1 常用拆分判断

遇到以下情况时，可以考虑拆成组件：

1. **职责明确**：这一块可以用一个业务名称说明，例如 EmployeeCard。
2. **需要复用**：同样的显示结构会出现多次。
3. **数据边界明确**：这一块只需要一组清楚的 Props。
4. **独立变化**：这一块以后可能拥有自己的样式、事件、状态或测试。
5. **父组件过长**：多个业务区域混在一起，已经难以定位代码。

不建议只根据代码行数拆分。下面的组件没有独立职责，也没有复用价值：

```jsx
function NameText({ name }) {
  return <span>{name}</span>;
}
```

如果它只使用一次，直接写 `<span>{employee.name}</span>` 通常更清楚。

### 7.2 数据应该放在哪里

本章还没有学习 State，但已经可以判断数据归属：

- 页面同时需要完整员工数据：对象暂时放在页面组件。
- `EmployeeCard` 只负责显示：通过 Props 接收数据。
- `Panel` 只负责外框：通过 `children` 接收内部内容。
- 不要在每个子组件中复制一份相同员工对象。

数据尽量由需要协调多个子组件的最近父组件持有，再通过 Props 向下传递。

## 8. 完整可运行示例

下面继续修改第一、二章创建的 `employee-app`。本章新增 `Panel.jsx` 和 `EmployeeCard.jsx`，并替换 `App.jsx`。

### 8.1 新建 Panel.jsx

新建 `src/components/Panel.jsx`：

```jsx
export function Panel({ title, children }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      <div className="panel-content">{children}</div>
    </section>
  );
}
```

### 8.2 新建 EmployeeCard.jsx

新建 `src/components/EmployeeCard.jsx`：

```jsx
export function EmployeeCard({ employee, showDepartment = true }) {
  return (
    <article className="employee-card">
      <h3>{employee.name}</h3>
      <p>员工编号：{employee.id}</p>
      {showDepartment && <p>部门：{employee.department}</p>}
      <p>状态：{employee.status}</p>
    </article>
  );
}
```

`employee` 是必须提供的业务对象，`showDepartment` 是可省略的显示选项。

### 8.3 替换 App.jsx

将 `src/App.jsx` 替换为：

```jsx
import { EmployeeCard } from './components/EmployeeCard.jsx';
import { PageHeader } from './components/PageHeader.jsx';
import { Panel } from './components/Panel.jsx';

export default function App() {
  const firstEmployee = {
    id: 1001,
    name: '田中太郎',
    department: '営業部',
    status: 'ACTIVE',
  };

  const secondEmployee = {
    id: 1002,
    name: '佐藤花子',
    department: '開発部',
    status: 'ACTIVE',
  };

  return (
    <>
      <PageHeader />
      <main>
        <Panel title="员工信息">
          <EmployeeCard employee={firstEmployee} />
          <EmployeeCard
            employee={secondEmployee}
            showDepartment={false}
          />
        </Panel>
      </main>
    </>
  );
}
```

本例暂时手动使用两次 `EmployeeCard`，目的是只观察 Props 传递。下一章学习列表渲染后，再使用数组和 `map()` 生成员工组件。

保存后应看到两名员工，其中第一名显示部门，第二名因为 `showDepartment={false}` 不显示部门。

### 8.4 检查 Props 的方法

安装 React DevTools 后，可以在 Components 面板中选择 `EmployeeCard`，查看它收到的 `employee` 和 `showDepartment`。如果页面显示 `undefined`，按下面顺序检查：

1. 父组件是否传了对应 Prop。
2. 传递名称和解构名称是否一致。
3. 对象中是否真的存在该字段。
4. 字符串、数字和布尔值是否使用了正确写法。

## 9. 常见错误

### 9.1 传递名称和接收名称不一致

```jsx
<EmployeeCard employeeName="田中太郎" />

function EmployeeCard({ name }) {
  return <p>{name}</p>;
}
```

父组件传的是 `employeeName`，子组件读取的是 `name`，因此显示 `undefined`。两边应使用同一个名称。

### 9.2 忘记使用花括号

```jsx
<EmployeeSummary count="employeeCount" />
```

这会传入固定字符串 `'employeeCount'`。要传变量应写成 `count={employeeCount}`。

### 9.3 把数字写成字符串

```jsx
<EmployeeSummary count="12" />
```

如果组件需要计算，应传 `count={12}`。

### 9.4 修改对象 Props

子组件直接执行 `employee.name = ...` 会修改调用方拥有的对象。子组件只读取并显示；修改流程交给数据拥有者。

### 9.5 拆分过细

每个 `span`、`td` 都做成组件，会增加文件跳转和 Props 传递，却没有形成清楚职责。先用业务名称说明组件作用；说不清时通常不必拆。

### 9.6 Props 层层透传过多

少量层级的 Props 传递是正常的数据流。如果很多中间组件完全不使用数据，只负责继续向下传，先检查组件边界和数据归属。更大范围共享会在 Context 和状态管理章节讨论，不要过早把所有数据放进全局 Store。

## 10. 练习

在本章完整示例的基础上完成以下任务：

1. 为员工对象增加 `email`，并让 `EmployeeCard` 显示邮箱。
2. 增加 `showStatus` Prop，默认显示状态；传入 `false` 时隐藏状态。
3. 新建 `DepartmentLabel`，只接收 `department`，判断它是否有独立职责并说明理由。
4. 使用 `Panel` 分别包装“在职员工”和“通知”两块内容，观察不同 `children` 如何进入同一个外框组件。
5. 故意把 `employee` 写成 `employeeInfo`，通过 React DevTools 或 Console 找到 `undefined` 原因并修复。
6. 尝试在 `EmployeeCard` 中修改 `employee.name`，说明它会影响哪些使用同一对象的代码，然后恢复为只读。
7. 执行 `npm run build`，确认所有组件导入、Props 名称和 JSX 均正确。

## 本章检查点

- [ ] 能说明父组件传值后，React 如何组成 Props 对象并交给子组件。
- [ ] 能正确传递字符串、数字、布尔值、变量和对象。
- [ ] 能使用 `props.xxx`、参数解构和默认值接收数据。
- [ ] 能使用 `children` 编写简单容器组件。
- [ ] 能解释 Props 只读和单向数据流的原因。
- [ ] 能根据职责、复用、数据边界和独立变化判断是否拆分组件。
- [ ] 能使用 React DevTools 或报错信息调查 Props 名称不一致的问题。

参考：[React：将 Props 传递给组件](https://zh-hans.react.dev/learn/passing-props-to-a-component)、[React：保持组件纯粹](https://zh-hans.react.dev/learn/keeping-components-pure)、[React：用组合替代层层传递](https://zh-hans.react.dev/learn/passing-data-deeply-with-context#before-you-use-context)。
