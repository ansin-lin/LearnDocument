# 第 2 章 JSX 与组件

## 本章目标

- 【必须掌握】说明 JSX 是什么，以及它和 HTML、JavaScript 的关系。
- 【必须掌握】在 JSX 中编写标签、属性和 JavaScript 表达式。
- 【必须掌握】定义、使用、导入和导出函数组件。
- 【必须掌握】通过组件嵌套组成一个可以运行的页面。

## 1. JSX 是什么

React 组件需要描述“画面应该显示什么”。JSX 是写在 JavaScript 文件中的一种界面描述语法，它的外观接近 HTML：

```jsx
const pageTitle = <h1>员工管理</h1>;
```

上面的 `<h1>` 不是一个 HTML 文件，也不是字符串。Vite 会先转换 JSX，React 再根据转换结果创建和更新浏览器中的 DOM。

```text
JSX
 ↓ Vite 转换
JavaScript
 ↓ React 处理
浏览器 DOM
```

使用 JSX 的好处是：标签结构、动态数据和显示逻辑可以放在同一个组件中阅读，而不必先用字符串拼接 HTML，再手动查找 DOM。

JSX 只能在支持 JSX 转换的项目中使用。本课程使用第一章创建的 Vite + React 项目，所以 `.jsx` 文件可以直接编写 JSX。

## 2. 第一个函数组件

组件是可以重复使用的 UI 单位。React 新项目通常使用函数定义组件：

```jsx
function PageHeader() {
  return (
    <header>
      <h1>员工管理系统</h1>
    </header>
  );
}
```

这段代码可以分成三部分理解：

1. `function PageHeader()` 定义名为 `PageHeader` 的 JavaScript 函数。
2. `return` 返回当前组件要显示的 JSX。
3. `<header>` 和 `<h1>` 描述浏览器中最终出现的页面结构。

定义组件后，可以像使用标签一样使用它：

```jsx
function App() {
  return (
    <main>
      <PageHeader />
      <p>请选择左侧菜单。</p>
    </main>
  );
}
```

`<PageHeader />` 表示让 React 渲染 `PageHeader` 组件。React 会执行该组件函数，取得它返回的 JSX，再组合到 `App` 的输出中。

```text
main.jsx
  ↓ 渲染
App
  ↓ 使用
PageHeader
  ↓ 返回
header 和 h1
```

### 2.1 组件名称为什么必须大写

React 根据标签首字母判断它是什么：

| 写法 | React 的理解 |
| --- | --- |
| `<header>` | 浏览器原生 HTML 元素 |
| `<button>` | 浏览器原生 HTML 元素 |
| `<PageHeader>` | 自己定义或导入的 React 组件 |

下面的名称以小写字母开头，React 会把它当成未知的 HTML 标签，而不是组件：

```jsx
function pageHeader() {
  return <h1>员工管理系统</h1>;
}

// 错误：pageHeader 会被当成原生标签名
<pageHeader />
```

组件使用大驼峰命名法，例如 `PageHeader`、`EmployeeTable`、`LoginForm`。

## 3. JSX 的基本书写规则

JSX 看起来像 HTML，但它位于 JavaScript 中，因此有几条必须遵守的规则。

### 3.1 标签必须正确闭合

有开始标签的元素必须有结束标签：

```jsx
<section>
  <h2>员工信息</h2>
</section>
```

没有子内容时使用自闭合写法：

```jsx
<img src="/employee.png" alt="员工头像" />
<input type="text" />
<PageHeader />
```

如果漏写 `/` 或结束标签，Vite 会在终端和浏览器中显示编译错误，并指出附近的代码位置。

### 3.2 组件必须返回一个根节点

下面的组件同时返回两个并列元素，无法通过编译：

```jsx
// 错误示例
function App() {
  return (
    <h1>员工管理</h1>
    <p>员工信息一览</p>
  );
}
```

可以使用有语义的父元素包起来：

```jsx
function App() {
  return (
    <main>
      <h1>员工管理</h1>
      <p>员工信息一览</p>
    </main>
  );
}
```

如果不希望增加额外 DOM，可以使用 Fragment（片段）：

```jsx
function App() {
  return (
    <>
      <h1>员工管理</h1>
      <p>员工信息一览</p>
    </>
  );
}
```

`<>...</>` 是 Fragment 的简写。它只负责把多个并列元素组成一个返回结果，不会生成额外 HTML 标签。

### 3.3 常见属性名与 HTML 不完全相同

JSX 属性会进入 JavaScript，因此部分名称采用 JavaScript 风格：

| HTML 中常见写法 | JSX 写法 | 说明 |
| --- | --- | --- |
| `class` | `className` | 设置 CSS 类名 |
| `for` | `htmlFor` | 让 `label` 关联表单控件 |
|  |
| `onclick` | `onClick` | React 事件属性使用 camelCase |

```jsx
<label className="form-label" htmlFor="employee-name">
  员工姓名
</label>
<input id="employee-name" tabIndex={0} />
```

`onClick` 等事件会在第五章详细讲解，本章先能识别这种属性写法即可。

## 4. 在 JSX 中使用 JavaScript 表达式

固定文字可以直接写在标签中；动态值放在一对花括号 `{}` 中：

```jsx
function App() {
  const systemName = 'Employee Management System';
  const employeeCount = 12;

  return (
    <main>
      <h1>{systemName}</h1>
      <p>当前员工人数：{employeeCount}</p>
      <p>下月预计人数：{employeeCount + 1}</p>
    </main>
  );
}
```

浏览器会显示：

```text
Employee Management System
当前员工人数：12
下月预计人数：13
```

花括号中可以放能够计算出值的 JavaScript 表达式，例如：

- 变量：`{systemName}`
- 属性读取：`{employee.name}`
- 计算：`{price * count}`
- 函数调用：`{formatDate(joinedDate)}`
- 三元表达式：`{active ? '在职' : '离职'}`

普通语句不能直接写在花括号中：

```jsx
// 错误：if 是语句，不能放在这个位置
<p>{if (active) '在职'}</p>
```

条件显示会在第四章系统讲解。本章先记住：JSX 的 `{}` 中放表达式，不直接放 `if`、`for` 等语句。

### 4.1 字符串属性和表达式属性

引号表示固定字符串，花括号表示 JavaScript 表达式：

```jsx
const imagePath = '/employee.png';

<img src="/logo.png" alt="公司标志" />
<img src={imagePath} alt="员工头像" />
```

- `src="/logo.png"`：属性值固定为 `/logo.png`。
- `src={imagePath}`：属性值来自变量 `imagePath`。

不要把花括号放进引号中：`src="{imagePath}"` 只会得到普通文字 `{imagePath}`。

### 4.2 JSX 中的注释

标签结构内部的注释也要放在花括号中：

```jsx
return (
  <main>
    {/* 页面主标题 */}
    <h1>员工管理</h1>
  </main>
);
```

## 5. 组件如何组成页面

页面通常不是一个巨大的组件，而是由多个职责明确的组件组合而成：

```jsx
function PageHeader() {
  return (
    <header>
      <h1>员工管理系统</h1>
    </header>
  );
}

function EmployeeSummary() {
  return (
    <section aria-labelledby="summary-title">
      <h2 id="summary-title">员工概况</h2>
      <p>当前登记员工：12 人</p>
    </section>
  );
}

function App() {
  return (
    <>
      <PageHeader />
      <main>
        <EmployeeSummary />
      </main>
    </>
  );
}
```

组件关系如下：

```text
App
├─ PageHeader
└─ main
   └─ EmployeeSummary
```

这种关系称为组件树。`App` 是父组件，`PageHeader` 和 `EmployeeSummary` 是它使用的子组件。这里的“父子”描述的是 JSX 中的组合关系，不是 JavaScript 类继承。

组件函数应在模块顶层定义，不要为了拆分代码就在另一个组件函数内部定义新组件。内部定义会在每次渲染时创建新的组件函数，后续加入 State 时还可能造成状态被重置。

组件在渲染时应专注于根据已有数据返回 JSX。不要在组件函数执行期间修改外部变量、写入存储或发送请求；事件和外部同步会在后续章节分别讲解。

## 6. 把组件拆到不同文件

当组件职责已经明确，可以把它们放进单独文件。下面的代码是在第一章项目基础上的完整修改结果。

### 6.1 新建 PageHeader.jsx

新建 `src/components/PageHeader.jsx`：

```jsx
export function PageHeader() {
  return (
    <header>
      <h1>员工管理系统</h1>
    </header>
  );
}
```

`export` 表示允许其他文件导入这个组件。这里使用的是命名导出，导入时名称要与 `PageHeader` 一致。

### 6.2 新建 EmployeeSummary.jsx

新建 `src/components/EmployeeSummary.jsx`：

```jsx
export default function EmployeeSummary() {
  return (
    <section aria-labelledby="summary-title">
      <h2 id="summary-title">员工概况</h2>
      <p>当前登记员工：12 人</p>
    </section>
  );
}
```

`export default` 表示这是当前文件的默认导出。一个文件只能有一个默认导出。

### 6.3 替换 App.jsx

将 `src/App.jsx` 替换为：

```jsx
import { PageHeader } from './components/PageHeader.jsx';
import EmployeeSummary from './components/EmployeeSummary.jsx';

export default function App() {
  return (
    <>
      <PageHeader />
      <main>
        <EmployeeSummary />
      </main>
    </>
  );
}
```

两种导入写法的区别如下：

| 导出方式 | 导出示例 | 导入示例 |
| --- | --- | --- |
| 命名导出 | `export function PageHeader()` | `import { PageHeader } from './components/PageHeader.jsx'` |
| 默认导出 | `export default function EmployeeSummary()` | `import EmployeeSummary from './components/EmployeeSummary.jsx'` |

命名导入必须写 `{}`，并使用导出时的名称；默认导入不写 `{}`。新人常见的 `does not provide an export named ...` 错误，通常就是两种方式混用了。

保存文件后，浏览器应显示“员工管理系统”和“当前登记员工：12 人”。第一章的 `main.jsx` 不需要修改，它仍然负责渲染 `App`。

## 7. 什么时候应该拆成组件

组件不是越多越好。遇到以下情况时，可以考虑拆分：

- 页面中的一块区域有明确职责，例如 Header、SearchArea、EmployeeTable。
- 同一 UI 会在多个位置重复使用。
- 一段内容已经很难在当前组件中阅读和修改。
- 某块区域后续会拥有自己的事件、状态或测试。

下面的拆分没有明显收益：

```jsx
function EmployeeName() {
  return <span>田中太郎</span>;
}
```

如果它只在一个位置使用，也没有独立职责，直接保留 `<span>` 可能更清楚。组件边界应服务于职责、复用和维护，不按行数机械拆分。

Page、Business 和 Common 是企业项目中常见的职责描述：

| 分类 | 作用 | 示例 |
| --- | --- | --- |
| Page Component | 表示一个路由页面 | `EmployeeListPage` |
| Business Component | 表示某项业务区域 | `EmployeeSearchForm`、`EmployeeTable` |
| Common Component | 跨业务复用的通用 UI | `PageHeader`、`Loading` |

本章只要求能根据职责初步识别，不需要一开始就创建完整目录。后续项目增长时再逐步整理。

## 8. 常见错误与调查方法

### 8.1 组件名称以小写开头

症状：组件没有按预期执行，浏览器把它当作未知标签。

修正：定义和使用时都改为大写开头，例如 `EmployeeCard` 和 `<EmployeeCard />`。

### 8.2 忘记闭合标签

症状：终端出现 JSX 编译错误，页面无法更新。

修正：检查报错行附近的开始标签、结束标签和自闭合 `/`。

### 8.3 返回多个根节点

症状：出现相邻 JSX 元素必须被包裹的错误。

修正：使用有语义的父元素，或使用 `<>...</>` Fragment。

### 8.4 导入路径或导出方式错误

症状：出现 `Failed to resolve import` 或 `does not provide an export named`。

调查顺序：

1. 文件是否真的位于 `src/components/`。
2. 文件名大小写是否一致。
3. 相对路径是否从当前文件出发。
4. 命名导出和默认导出是否使用了正确导入方式。

### 8.5 把对象直接放进 JSX

```jsx
const employee = { name: '田中太郎' };

// 错误：普通对象不能直接作为页面内容
<p>{employee}</p>

// 正确：读取要显示的字段
<p>{employee.name}</p>
```

遇到浏览器运行错误时，同时查看开发终端和 DevTools Console。编译错误通常先出现在终端；组件执行期间的错误通常也会出现在 Console。

## 9. 练习

在第一章创建的 `employee-app` 中完成以下任务：

1. 新建 `PageHeader.jsx` 和 `EmployeeSummary.jsx`，按照本章示例导入到 `App.jsx`。
2. 新建 `SystemNotice` 组件，显示“系统维护时间：每周日 02:00～03:00”。
3. 在 `App` 中组合三个组件，并用 `main`、`section`、标题和段落表达正确结构。
4. 将系统名称和员工人数保存为变量，通过 `{}` 显示并完成一次加法计算。
5. 故意把命名导入的 `{}` 删除，观察报错后恢复。
6. 故意漏写一个结束标签，根据终端错误找到并修复。
7. 执行 `npm run build`，确认代码能够完成生产构建。

完成后的组件树至少应为：

```text
App
├─ PageHeader
└─ main
   ├─ EmployeeSummary
   └─ SystemNotice
```

## 本章检查点

- [ ] 能说明 JSX 如何经过转换并最终成为浏览器 DOM。
- [ ] 能正确使用闭合标签、单一根节点、Fragment、`className` 和 `{}`。
- [ ] 能说明函数组件的名称、返回值和使用方式。
- [ ] 能正确区分原生元素与自定义组件。
- [ ] 能使用命名导出、默认导出及其对应的导入方式。
- [ ] 能根据职责拆分并组合一个简单页面。

参考：[React：创建和嵌套组件](https://zh-hans.react.dev/learn#components-ui-building-blocks)、[React：使用 JSX 编写标签语言](https://zh-hans.react.dev/learn/writing-markup-with-jsx)、[React：在 JSX 中通过大括号使用 JavaScript](https://zh-hans.react.dev/learn/javascript-in-jsx-with-curly-braces)。
