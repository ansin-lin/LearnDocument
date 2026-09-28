# 第 8 章 业务表单与校验

## 本章目标

- 【必须掌握】使用受控组件管理 `input`、`textarea`、`select`、radio 和 checkbox。
- 【必须掌握】根据控件类型正确读取字符串、布尔值和数字。
- 【必须掌握】在提交前执行必填、长度、格式和业务校验。
- 【必须掌握】把错误信息与对应控件关联。
- 【必须掌握】使用 `saving` 防止重复提交，并在失败后恢复。
- 【必须掌握】区分前端校验和后端校验的职责。

## 1. 表单中有哪些状态

员工新增表单不仅包含输入值，还可能包含：

```text
form          当前输入值
errors        各字段错误
saving        是否正在保存
serverError   页面级保存错误
```

这些状态的职责不同，不要把错误文字或保存状态混进员工业务对象。

```jsx
const [form, setForm] = useState({
  name: '',
  email: '',
  department: '',
  role: 'USER',
  joinedDate: '',
  active: true,
  note: '',
});

const [errors, setErrors] = useState({});
const [saving, setSaving] = useState(false);
const [serverError, setServerError] = useState('');
```

## 2. 什么是受控组件

输入框的显示值由 React State 决定，并通过 `onChange` 更新 State，这种写法称为受控组件：

```jsx
<label htmlFor="employee-name">姓名</label>
<input
  id="employee-name"
  value={form.name}
  onChange={(event) =>
    setForm((previous) => ({
      ...previous,
      name: event.target.value,
    }))
  }
/>
```

数据流：

```text
State form.name
      ↓ value
Input 显示
      ↓ 用户输入 / onChange
setForm
      ↓
新的 form.name
```

只写 `value` 不写 `onChange`，输入框会变成只读。只写 `onChange` 而不绑定 `value`，值由 DOM 自己保存，属于非受控方式。本课程业务表单主线使用受控组件。

## 3. 使用通用 change 处理函数

多个文本类控件可以用 `name` 对应表单字段：

```jsx
function handleChange(event) {
  const { name, value } = event.target;

  setForm((previous) => ({
    ...previous,
    [name]: value,
  }));
}
```

```jsx
<input name="name" value={form.name} onChange={handleChange} />
<input name="email" value={form.email} onChange={handleChange} />
```

`name="email"` 与对象字段 `form.email` 必须一致。通用处理器适合更新规则相同的控件；特殊转换或业务处理应使用单独函数，不要把所有分支都塞进一个巨大处理器。

## 4. 各种表单控件

### 4.1 文本输入框

```jsx
<input
  id="employee-email"
  name="email"
  type="email"
  value={form.email}
  onChange={handleChange}
/>
```

浏览器输入值通常是字符串。`type="email"` 改善输入体验和基础约束，但业务仍需自行校验。

### 4.2 textarea

React 的 `textarea` 使用 `value`，不把正文写在开始和结束标签之间：

```jsx
<textarea
  id="employee-note"
  name="note"
  value={form.note}
  onChange={handleChange}
/>
```

### 4.3 select

```jsx
<select
  id="employee-department"
  name="department"
  value={form.department}
  onChange={handleChange}
>
  <option value="">请选择</option>
  <option value="SALES">営業部</option>
  <option value="DEVELOPMENT">開発部</option>
</select>
```

State 保存稳定业务值 `SALES`，页面显示日文标签“営業部”。不要把显示文字当作接口值，除非 API 规格就是这样定义。

### 4.4 radio

同一组 radio 使用相同 `name`，每个选项有不同 `value`：

```jsx
<fieldset>
  <legend>权限</legend>
  <label>
    <input
      type="radio"
      name="role"
      value="USER"
      checked={form.role === 'USER'}
      onChange={handleChange}
    />
    一般用户
  </label>
  <label>
    <input
      type="radio"
      name="role"
      value="ADMIN"
      checked={form.role === 'ADMIN'}
      onChange={handleChange}
    />
    管理员
  </label>
</fieldset>
```

radio 使用 `checked` 判断当前选项是否选中，更新值仍从 `event.target.value` 取得。

### 4.5 checkbox

checkbox 的业务值是布尔值，应读取 `checked`，不是 `value`：

```jsx
function handleActiveChange(event) {
  setForm((previous) => ({
    ...previous,
    active: event.target.checked,
  }));
}

<label>
  <input
    type="checkbox"
    checked={form.active}
    onChange={handleActiveChange}
  />
  在职
</label>
```

### 4.6 数字输入

即使 `type="number"`，`event.target.value` 仍是字符串：

```jsx
function handleAgeChange(event) {
  const value = event.target.value;
  setAge(value === '' ? '' : Number(value));
}
```

是否在输入时转换，取决于表单是否需要暂时允许空字符串和不完整输入。提交到 API 前必须按接口契约规范化。

### 4.7 file

文件输入不能像普通文本框一样通过字符串 `value` 控制。通过 `event.target.files` 读取文件：

```jsx
function handleFileChange(event) {
  const file = event.target.files?.[0] ?? null;
  setSelectedFile(file);
}
```

文件上传流程会在第二十一章详细讲解。

## 5. 什么时候进行校验

常见校验时机：

| 时机 | 适合内容 | 注意点 |
| --- | --- | --- |
| 输入时 | 长度提示、即时格式反馈 | 不要用户刚输入一个字符就堆满错误 |
| 失去焦点时 | 单字段格式检查 | 适合邮箱、日期等字段 |
| 提交时 | 全部规则最终确认 | 必须执行，不能只依赖前两种 |
| 后端处理时 | 权限、重复、数据库与最终业务规则 | 永远不能省略 |

本章主线采用“提交时完整校验”，并在需要时为单字段增加 `onBlur`。

## 6. 编写纯校验函数

校验函数接收表单值，返回错误对象，不修改 State、不发送请求：

```jsx
function validateEmployee(value) {
  const nextErrors = {};

  if (!value.name.trim()) {
    nextErrors.name = '姓名为必填项';
  } else if (value.name.trim().length > 50) {
    nextErrors.name = '姓名不能超过 50 个字符';
  }

  if (!value.email.trim()) {
    nextErrors.email = '邮箱为必填项';
  } else if (!/^\S+@\S+\.\S+$/.test(value.email)) {
    nextErrors.email = '邮箱格式不正确';
  }

  if (!value.department) {
    nextErrors.department = '请选择部门';
  }

  return nextErrors;
}
```

使用 `else if` 可以避免同一字段同时显示“必填”和“格式错误”。前端正则只做基础体验校验，不代表邮箱一定真实存在。

## 7. 提交表单

```jsx
async function handleSubmit(event) {
  event.preventDefault();

  const nextErrors = validateEmployee(form);
  setErrors(nextErrors);

  if (Object.keys(nextErrors).length > 0) {
    return;
  }

  if (saving) return;

  setSaving(true);
  setServerError('');

  try {
    await saveEmployee(form);
  } catch {
    setServerError('保存失败，请稍后重试。');
  } finally {
    setSaving(false);
  }
}
```

执行顺序：

```text
submit
  ↓ preventDefault
前端校验
  ├─ 失败 → 显示字段错误，不请求 API
  └─ 成功 → saving=true → 保存
                         ├─ 成功 → 成功处理
                         └─ 失败 → 页面错误
                         ↓ finally
                       saving=false
```

`finally` 确保成功和失败后都恢复按钮。`saving` 可以减少双击，但关键业务还需要后端的重复请求或幂等设计。

## 8. 可访问的错误提示

每个控件需要可关联的 `label`。错误出现时使用 `aria-invalid` 和 `aria-describedby`：

```jsx
<label htmlFor="employee-email">邮箱</label>
<input
  id="employee-email"
  name="email"
  type="email"
  value={form.email}
  onChange={handleChange}
  aria-invalid={Boolean(errors.email)}
  aria-describedby={errors.email ? 'employee-email-error' : undefined}
/>
{errors.email && (
  <p id="employee-email-error" role="alert">
    {errors.email}
  </p>
)}
```

错误不能只用红色表示，还要有明确文字。页面级保存错误放在表单附近：

```jsx
{serverError && <p role="alert">{serverError}</p>}
```

校验失败后，实际项目可把焦点移动到第一个错误控件，但不要破坏用户正常输入流程。

## 9. 前端校验不能替代后端校验

前端校验用于尽早反馈和减少无效请求，但用户可以绕过页面直接发送 HTTP 请求。后端必须重新检查：

- 必填、长度和格式。
- 当前用户权限。
- 数据是否重复。
- 关联数据是否存在。
- 业务状态是否允许操作。
- 并发更新和数据库约束。

后端返回字段错误时，应映射到相应控件；权限或系统错误显示为页面级信息。不要把 SQL、堆栈或敏感响应直接显示给用户。

## 10. 完整 EmployeeForm

新建 `src/components/EmployeeForm.jsx`：

```jsx
import { useState } from 'react';

const initialForm = {
  name: '',
  email: '',
  department: '',
  role: 'USER',
  active: true,
  note: '',
};

function validateEmployee(value) {
  const nextErrors = {};

  if (!value.name.trim()) nextErrors.name = '姓名为必填项';
  else if (value.name.trim().length > 50) {
    nextErrors.name = '姓名不能超过 50 个字符';
  }

  if (!value.email.trim()) nextErrors.email = '邮箱为必填项';
  else if (!/^\S+@\S+\.\S+$/.test(value.email)) {
    nextErrors.email = '邮箱格式不正确';
  }

  if (!value.department) nextErrors.department = '请选择部门';
  return nextErrors;
}

export function EmployeeForm({ onSave }) {
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [serverError, setServerError] = useState('');

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function handleActiveChange(event) {
    setForm((previous) => ({
      ...previous,
      active: event.target.checked,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = validateEmployee(form);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0 || saving) return;

    setSaving(true);
    setServerError('');
    try {
      await onSave({ ...form, name: form.name.trim(), email: form.email.trim() });
      setForm(initialForm);
    } catch {
      setServerError('保存失败，请稍后重试。');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <label htmlFor="name">姓名</label>
      <input id="name" name="name" value={form.name} onChange={handleChange}
        aria-invalid={Boolean(errors.name)}
        aria-describedby={errors.name ? 'name-error' : undefined} />
      {errors.name && <p id="name-error" role="alert">{errors.name}</p>}

      <label htmlFor="email">邮箱</label>
      <input id="email" name="email" type="email" value={form.email}
        onChange={handleChange} aria-invalid={Boolean(errors.email)}
        aria-describedby={errors.email ? 'email-error' : undefined} />
      {errors.email && <p id="email-error" role="alert">{errors.email}</p>}

      <label htmlFor="department">部门</label>
      <select id="department" name="department" value={form.department}
        onChange={handleChange} aria-invalid={Boolean(errors.department)}>
        <option value="">请选择</option>
        <option value="SALES">営業部</option>
        <option value="DEVELOPMENT">開発部</option>
      </select>
      {errors.department && <p role="alert">{errors.department}</p>}

      <label>
        <input type="checkbox" checked={form.active}
          onChange={handleActiveChange} />
        在职
      </label>

      <label htmlFor="note">备注</label>
      <textarea id="note" name="note" value={form.note} onChange={handleChange} />

      {serverError && <p role="alert">{serverError}</p>}
      <button type="submit" disabled={saving}>
        {saving ? '保存中...' : '保存'}
      </button>
    </form>
  );
}
```

`onSave` 由父组件提供，表单负责输入、校验和提交状态，父组件负责实际保存方式。第十章接入 API 后，可以把 `onSave` 对应到 Service。

示例在 `form` 上使用 `noValidate`，是为了统一展示本章编写的错误消息；它会关闭浏览器默认校验提示，但不会关闭本章的 React 校验。实际项目也可以保留原生校验，关键是避免两套错误提示互相矛盾。

## 11. 常见错误与练习

- 输入框无法输入：检查是否有 `value` 却没有 `onChange`。
- checkbox 一直不变：检查是否错误读取了 `value`，应读取 `checked`。
- number 提交成字符串：在提交前按接口约定转换。
- radio 不能互斥：检查同组 `name` 是否一致，以及 `checked` 条件。
- 校验失败仍请求：在错误对象非空时立即 `return`。
- 连续点击发出多个请求：提交开始前检查 `saving`，并禁用按钮。
- 请求失败后按钮一直禁用：使用 `finally` 恢复 `saving`。
- Props 初始值晚到但表单未更新：明确等待数据后再挂载表单，或设计草稿重置规则，不要盲目同步。

练习：

1. 补全 role radio、joinedDate 和备注最大长度。
2. 实现姓名 1～50 字符、邮箱格式和部门必选。
3. 校验失败时不调用 `onSave`。
4. 模拟保存成功、保存失败和快速双击。
5. 让后端字段错误显示到对应控件，其他错误显示在页面级。
6. 用键盘完成输入和提交，确认 label、焦点和错误文字可理解。
7. 执行测试或日志验证传给 `onSave` 的对象字段和值。

## 本章检查点

- [ ] 能说明受控组件的 State → value → onChange → State 循环。
- [ ] 能正确处理文本、select、radio、checkbox、数字和文件输入。
- [ ] 能编写不修改外部状态的纯校验函数。
- [ ] 能阻止校验失败请求，并在保存期间防止重复提交。
- [ ] 能把错误文字与控件关联，并区分字段错误和页面错误。
- [ ] 能说明前端校验与后端校验的职责边界。

参考：[React：响应输入并使用 State](https://zh-hans.react.dev/learn/reacting-to-input-with-state)、[React：选择 State 结构](https://zh-hans.react.dev/learn/choosing-the-state-structure)。
