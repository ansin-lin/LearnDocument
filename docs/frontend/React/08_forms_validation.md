# 第 8 章 业务表单与校验

## 本章目标

- 用受控组件处理 input、textarea、select、radio 和 checkbox。
- 完成提交、必填、长度、格式校验与错误提示。
- 在提交期间禁用操作并保持可访问性。

## 1. 受控表单

```tsx
type EmployeeForm = {
  name: string;
  email: string;
  department: string;
  role: 'ADMIN' | 'USER';
  joinedDate: string;
  status: EmployeeStatus;
};

const [form, setForm] = useState<EmployeeForm>({
  name: '', email: '', department: '', role: 'USER',
  joinedDate: '', status: 'ACTIVE',
});

<label htmlFor="name">姓名</label>
<input
  id="name"
  value={form.name}
  onChange={(event) =>
    setForm((previous) => ({ ...previous, name: event.target.value }))
  }
/>
```

```text
Input → onChange → setForm → State → value → Input
```

`select`、`textarea` 同样使用 `value`；checkbox 使用 `checked` 与 `event.target.checked`。同名 radio 通过固定字符串更新联合类型。

## 2. 提交与校验

```tsx
type FormErrors = Partial<Record<keyof EmployeeForm, string>>;

function validate(value: EmployeeForm): FormErrors {
  const errors: FormErrors = {};
  if (!value.name.trim()) errors.name = '姓名为必填项';
  if (value.name.length > 50) errors.name = '姓名不能超过 50 个字符';
  if (!/^\S+@\S+\.\S+$/.test(value.email)) errors.email = '邮箱格式不正确';
  return errors;
}

function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault();
  const nextErrors = validate(form);
  setErrors(nextErrors);
  if (Object.keys(nextErrors).length > 0) return;
  void saveEmployee(form);
}
```

错误文字应与控件关联：

```tsx
<input aria-invalid={Boolean(errors.email)} aria-describedby="email-error" />
{errors.email && <p id="email-error" role="alert">{errors.email}</p>}
```

HTML 的 `required` 等约束能改善体验，但不能替代 React 业务校验，更不能替代后端校验。

## 3. 提交状态

```tsx
<button type="submit" disabled={saving}>
  {saving ? '保存中...' : '保存'}
</button>
```

提交逻辑使用 `try/finally` 保证 `saving` 恢复。服务端返回字段错误时映射到对应控件；未知失败显示页面级错误。不要只靠按钮禁用防止重复写入，关键接口还应由后端考虑幂等或重复请求边界。

## 4. 常见错误与练习

- 写了 `value` 却没有 `onChange`：输入框变成只读。
- 所有字段都存为 `string` 后不转换：提交契约与类型不一致。
- 只在颜色上表示错误：屏幕阅读器无法理解。
- 校验失败仍继续请求：校验函数返回后必须中止。

练习：完成姓名、邮箱、部门、权限、入社日与状态表单；实现 3 条校验、首个错误聚焦、保存中禁用和失败恢复。这个结构在第 17 章统一命名为 `EmployeeInput`。测试键盘提交，而不只点击按钮。

## 本章检查点

- [ ] 能实现主要表单控件的受控写法。
- [ ] 校验错误与对应控件关联，失败时不继续请求。
- [ ] 保存期间防重复提交，并在 finally 恢复状态。
