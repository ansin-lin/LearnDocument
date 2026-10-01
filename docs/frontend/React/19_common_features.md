# 第 19 章 React 企业项目常见业务 UI 实战

## 本章目标

- 用稳定状态模型实现 Loading、Dialog、Toast、分页、搜索和排序。
- 区分 URL 状态、局部 UI 状态和服务器查询条件。
- 为每个功能提供可观察成功与失败结果。
- 能完成 Employee 表单、Validation、Table、筛选和基本 CRUD 流程。

## 1. 企业业务页面由哪些部分组成

员工管理列表通常包含：

```text
员工管理
────────────────────────────
关键字 [              ]
部门   [全部 ▼]  状态 [全部 ▼]
[搜索] [清除]
────────────────────────────
员工编号 | 姓名 | 部门 | 状态 | 操作
E001     | 张三 | 开发 | 有效 | 编辑 删除
────────────────────────────
< 上一页  1 / 10  下一页 >
[新增员工]
```

这一张页面已经组合了 Form、Input、Select、Table、Search、Filter、Sort、Pagination、Loading、Empty、Error、Dialog、Toast 和按钮状态。本章先分别掌握，再把它们连接成一个 CRUD 流程。

## 2. Form 与 Controlled Component

React 中常用 State 同步管理输入值，这类输入称为 Controlled Component：

```jsx
const [name, setName] = useState('');

<input
  value={name}
  onChange={(event) => setName(event.target.value)}
/>
```

`value` 决定画面当前显示什么，`onChange` 把用户输入写回 State。两者缺少一个，就容易出现无法输入或画面与 State 不一致。

Employee 表单包含多个字段时，可以使用一个对象：

```jsx
const initialEmployee = {
  employeeCode: '',
  name: '',
  email: '',
  department: '',
  joinedDate: '',
  status: 'ACTIVE',
  role: 'USER',
};

export function EmployeeForm({ initialValue = initialEmployee, onSubmit }) {
  const [form, setForm] = useState(initialValue);
  const [errors, setErrors] = useState({});

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = validateEmployee(form);
    setErrors(nextErrors);

    if (Object.keys(nextErrors).length > 0) return;
    onSubmit(form);
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <label htmlFor="employeeCode">员工编号</label>
      <input
        id="employeeCode"
        name="employeeCode"
        value={form.employeeCode}
        onChange={updateField}
        aria-describedby="employeeCode-error"
      />
      {errors.employeeCode && (
        <p id="employeeCode-error" role="alert">
          {errors.employeeCode}
        </p>
      )}

      <label htmlFor="employeeName">姓名</label>
      <input
        id="employeeName"
        name="name"
        value={form.name}
        onChange={updateField}
      />

      <label htmlFor="employeeEmail">邮箱</label>
      <input
        id="employeeEmail"
        name="email"
        type="email"
        value={form.email}
        onChange={updateField}
      />

      <label htmlFor="department">部门</label>
      <select
        id="department"
        name="department"
        value={form.department}
        onChange={updateField}
      >
        <option value="">请选择</option>
        <option value="DEV">开发</option>
        <option value="SALES">营业</option>
        <option value="HR">人事</option>
      </select>

      <label htmlFor="joinedDate">入职日期</label>
      <input
        id="joinedDate"
        name="joinedDate"
        type="date"
        value={form.joinedDate}
        onChange={updateField}
      />

      <label htmlFor="role">角色</label>
      <select
        id="role"
        name="role"
        value={form.role}
        onChange={updateField}
      >
        <option value="USER">普通用户</option>
        <option value="ADMIN">管理员</option>
      </select>

      <fieldset>
        <legend>状态</legend>
        <label>
          <input
            type="radio"
            name="status"
            value="ACTIVE"
            checked={form.status === 'ACTIVE'}
            onChange={updateField}
          />
          有效
        </label>
        <label>
          <input
            type="radio"
            name="status"
            value="INACTIVE"
            checked={form.status === 'INACTIVE'}
            onChange={updateField}
          />
          无效
        </label>
      </fieldset>

      <button type="submit">保存</button>
    </form>
  );
}
```

`name` 必须与 `form` 的字段名一致，`updateField()` 才能更新正确字段。编辑页面把读取到的 Employee 作为 `initialValue`，新增页面使用空的初始值。

编辑数据通常通过 API 异步取得。父页面应先完成读取，再渲染表单：

```jsx
if (loading) return <p role="status">读取中...</p>;
if (error) return <ErrorPanel error={error} onRetry={loadEmployee} />;
if (!employee) return <p>员工不存在</p>;

return (
  <EmployeeForm
    initialValue={employee}
    onSubmit={handleUpdate}
  />
);
```

不要先用空对象建立表单，再误以为 `initialValue` 变化后 `useState(initialValue)` 会自动重新初始化；`useState()` 只在组件首次建立时使用初始值。

## 3. Validation

### 3.1 三层 Validation

| 层 | 作用 | 能否代替 Backend |
| --- | --- | --- |
| HTML Validation | `required`、`maxLength`、输入类型等基础浏览器提示 | 不能 |
| Frontend Validation | 更及时、符合画面规格的提示 | 不能 |
| Backend Validation | 保护正式业务数据和业务规则 | 必须存在 |

用户可以绕过前端直接调用 API，因此前端 Validation 主要改善操作体验，不能成为安全边界。

### 3.2 Employee 校验函数

```js
export function validateEmployee(employee) {
  const errors = {};

  if (!employee.employeeCode.trim()) {
    errors.employeeCode = '员工编号为必填项';
  } else if (employee.employeeCode.length > 10) {
    errors.employeeCode = '员工编号不能超过 10 个字符';
  }

  if (!employee.name.trim()) {
    errors.name = '姓名为必填项';
  } else if (employee.name.length > 50) {
    errors.name = '姓名不能超过 50 个字符';
  }

  if (!employee.email.trim()) {
    errors.email = '邮箱为必填项';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(employee.email)) {
    errors.email = '邮箱格式不正确';
  }

  if (!employee.department) {
    errors.department = '请选择部门';
  }

  if (!employee.joinedDate) {
    errors.joinedDate = '请选择入职日期';
  }

  return errors;
}
```

校验应覆盖 required、最大长度、格式、数值范围、日期和业务规则。Backend 返回的字段错误也应合并到相同的 `errors` 结构，再显示在对应输入项附近。

## 4. 常见表单控件

| 控件 | 读取方式 | 注意点 |
| --- | --- | --- |
| Text / Date | `event.target.value` | State 通常保存字符串 |
| Select | `event.target.value` | 提供明确空选项 |
| Radio | `checked={value === 当前值}` | 同组使用相同 `name` |
| Checkbox | `event.target.checked` | 返回布尔值，不是文字 |

Checkbox 示例：

```jsx
<label>
  <input
    type="checkbox"
    checked={form.sendNotification}
    onChange={(event) => setForm((current) => ({
      ...current,
      sendNotification: event.target.checked,
    }))}
  />
  保存后发送通知
</label>
```

## 5. Table 与页面四种状态

列表不能只考虑“有数据”。至少要区分：

```text
Loading
Success + Data
Success + Empty
Error
```

Empty 表示请求成功但没有符合条件的数据，不是 Error。

```jsx
export function EmployeeTable({ employees, loading, error, onRetry }) {
  if (loading) return <p role="status">员工列表读取中...</p>;
  if (error) return <ErrorPanel error={error} onRetry={onRetry} />;
  if (employees.length === 0) return <p>没有符合条件的员工</p>;

  return (
    <table>
      <thead>
        <tr>
          <th>员工编号</th>
          <th>姓名</th>
          <th>部门</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        {employees.map((employee) => (
          <tr key={employee.id}>
            <td>{employee.employeeCode}</td>
            <td>{employee.name}</td>
            <td>{employee.department}</td>
            <td>{employee.status}</td>
            <td>
              <Link to={`/employees/${employee.id}/edit`}>编辑</Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

`key` 应使用稳定、唯一的 `employee.id`，不要在列表顺序会变化时使用数组下标。

## 6. Dialog、Loading 与 Toast

Loading 应说明正在读取什么，长操作保留上下文；不要用全屏遮罩阻塞无关区域。确认 Dialog 至少需要标题、说明、确认/取消、初始焦点、焦点圈定与 Esc 行为，优先使用项目中经过可访问性验证的组件。

Toast 适合短暂的非阻塞反馈，如“保存成功”；字段错误和阻止流程的错误不应只用会消失的 Toast。重要消息同时保留在页面或日志中。

Toast 使用包含 `id`、`tone` 和 `message` 的对象。`tone` 在本例中使用 `success` 或 `error`。

### 6.1 删除、确认、Loading 与 Toast 的最小流程

下面是贯穿项目中的一个完整状态流程。`deleteEmployee` 复用第 11 章 Service；示例中的简化 Dialog 用于观察数据流，正式项目应换成团队已经处理焦点圈定、Esc、背景不可操作和动画的 UI Library Dialog。

```jsx
export function DeleteEmployeeFlow({
  employeeId,
  employeeName,
  onDeleted,
}) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState(null);

  async function confirmDelete() {
    if (deleting) return;
    setDeleting(true);
    setToast(null);
    try {
      await deleteEmployee(employeeId);
      onDeleted(employeeId);
      setDialogOpen(false);
      setToast({ id: crypto.randomUUID(), tone: 'success', message: '删除成功' });
    } catch {
      setToast({ id: crypto.randomUUID(), tone: 'error', message: '删除失败，请重试' });
    } finally {
      setDeleting(false);
    }
  }

  return (
    <>
      <button type="button" onClick={() => setDialogOpen(true)}>
        删除
      </button>

      {dialogOpen && (
        <section role="dialog" aria-modal="true" aria-labelledby="delete-title">
          <h2 id="delete-title">删除员工</h2>
          <p>确定删除 {employeeName} 吗？</p>
          <button type="button" autoFocus disabled={deleting} onClick={() => setDialogOpen(false)}>
            取消
          </button>
          <button type="button" disabled={deleting} onClick={() => void confirmDelete()}>
            {deleting ? '删除中...' : '确认删除'}
          </button>
        </section>
      )}

      {toast && (
        <p role={toast.tone === 'error' ? 'alert' : 'status'}>
          {toast.message}
        </p>
      )}
    </>
  );
}
```

```text
Delete Button → dialogOpen=true → Dialog
Confirm → deleting=true → 按钮禁用/Loading → DELETE API
Success → 关闭 Dialog + 更新列表 + Success Toast
Failure → 保留 Dialog + 恢复按钮 + Error Message
```

失败提示不能只依赖稍后消失的 Toast；正式实现应让 Dialog 内也保留错误和重试入口。删除成功后由持有列表 State 的父组件执行 `onDeleted`，继续遵守单向数据流。

## 7. Search 与 Filter

搜索和筛选最终都转换成查询条件：

```text
keyword=tanaka
department=DEV
status=ACTIVE
page=1
size=10
sort=employeeCode,asc
```

```jsx
const [draftKeyword, setDraftKeyword] = useState(keyword);

function handleSearch(event) {
  event.preventDefault();
  const next = new URLSearchParams(searchParams);
  const keyword = draftKeyword.trim();

  if (keyword) next.set('keyword', keyword);
  else next.delete('keyword');

  next.set('page', '1');
  setSearchParams(next);
}
```

输入草稿属于表单状态；已提交查询条件属于 URL/页面状态。每次查询条件变化时页码通常重置为 1。若采用输入即检索，应明确防抖、取消旧请求和键盘体验。

这里复制现有 `searchParams` 后只修改关键字和页码，因此不会意外删除部门和排序。空关键字从 URL 删除，使复制、刷新后的条件清楚稳定。

页面从 URL 生成唯一查询对象：

```jsx
const keyword = searchParams.get('keyword') ?? '';
const department = searchParams.get('department') ?? '';
const status = searchParams.get('status') ?? '';
const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1);
const size = 10;
const sort = searchParams.get('sort') ?? 'employeeCode,asc';

const query = { keyword, department, status, page, size, sort };
```

输入框草稿和已提交查询条件不要混为一份 State。用户输入尚未提交时，不应每按一个字符就改变 URL，除非规格明确要求即时搜索并已处理防抖与请求取消。

部门和状态变化后，通常把 `page` 重置为 `1`。清除按钮应删除 `keyword`、`department` 和 `status`，同时保留项目明确要求继续使用的其他参数。

## 8. Pagination

本章继续使用第 17 章的分页响应约定，不在分页组件内重新设计另一套结构。`items`、`page`、`size` 和 `total` 必须直接对应后端分页契约。

页码、每页条数与总数来自同一接口契约。删除最后一页最后一条后，要处理当前页超出新总页数的情况。按钮提供可理解名称和当前页标记。

```jsx
function Pagination({ page, size, total, onPageChange }) {
  const totalPages = Math.max(1, Math.ceil(total / size));

  return (
    <nav aria-label="员工列表分页">
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
      >
        上一页
      </button>
      <span aria-current="page">
        {page} / {totalPages}
      </span>
      <button
        type="button"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        下一页
      </button>
    </nav>
  );
}
```

父页面把页码写回 URL：

```js
function handlePageChange(nextPage) {
  const next = new URLSearchParams(searchParams);
  next.set('page', String(nextPage));
  setSearchParams(next);
}
```

`Math.ceil(total / size)` 计算总页数。按钮在边界禁用，不能发出第 0 页或超过总页数的请求。

例如 `total=95`、`size=10`：

```text
前 9 页最多各 10 条
第 10 页最多 5 条
totalPages = Math.ceil(95 / 10) = 10
```

删除后重新查询。如果当前页除被删除项外已无数据且 `page > 1`，先把 URL 页码减 1，再按新页码查询。

## 9. Sort

```text
点击“姓名”表头
→ 更新 sort=name,asc
→ URL 更新并请求服务端
→ 旧请求取消
→ 表头显示排序方向
```

服务端分页时必须由服务端排序；只排序当前页会制造看似正确但整体错误的结果。字段白名单由前后端共同约定，不把任意字符串直接传入数据库排序。

```jsx
const allowedSorts = new Set([
  'employeeCode,asc',
  'employeeCode,desc',
  'name,asc',
  'name,desc',
]);

function changeSort(field) {
  const [currentField, currentDirection] = sort.split(',');
  const nextDirection =
    currentField === field && currentDirection === 'asc' ? 'desc' : 'asc';
  const nextSort = `${field},${nextDirection}`;

  if (!allowedSorts.has(nextSort)) return;

  const next = new URLSearchParams(searchParams);
  next.set('sort', nextSort);
  next.set('page', '1');
  setSearchParams(next);
}
```

表头按钮示例：

```jsx
<th aria-sort={sort === 'name,asc' ? 'ascending'
  : sort === 'name,desc' ? 'descending'
  : 'none'}>
  <button type="button" onClick={() => changeSort('name')}>
    姓名
  </button>
</th>
```

前端白名单用于避免发送未约定字段，后端仍必须使用自己的排序白名单，不能把 Query 字符串直接拼入 SQL。

## 10. 查询与请求的完整连接

```jsx
useEffect(() => {
  const controller = new AbortController();

  loadEmployees(query, controller.signal);

  return () => controller.abort();
}, [keyword, department, status, page, size, sort]);
```

```text
Search / Sort / Pagination
  ↓ 修改 URL Search Params
从 URL 得到 query
  ↓ Effect 依赖变化并取消旧请求
searchEmployees(query, signal)
  ↓
loading → empty / success / error
```

实际代码中的 `loadEmployees` 应保持稳定，或直接在 Effect 内定义，避免函数引用造成无意重复请求。

## 11. Dirty Check 与离开确认

Dirty 表示“当前输入已经与最初数据不同，但尚未保存”：

```js
const dirty = JSON.stringify(form) !== JSON.stringify(initialValue);
```

教学示例可以这样比较简单对象；正式项目应根据字段和数据类型采用稳定比较方式。Dirty 常用于：

- 没有修改时禁用保存按钮；
- 点击返回或切换页面时提示尚未保存；
- 保存成功后更新基准数据，使 Dirty 恢复为 false。

浏览器关闭、刷新和 React Router 内部跳转的拦截机制不同，应使用项目当前 Router 版本支持的方式。不要到处直接覆盖 `window.onbeforeunload`，也不要在保存成功后仍显示离开确认。

## 12. 按钮状态与权限

```jsx
<button
  type="submit"
  disabled={!dirty || saving || hasValidationError}
>
  {saving ? '保存中...' : '保存'}
</button>

{canDelete && (
  <button type="button" onClick={openDeleteDialog}>
    删除
  </button>
)}
```

- `disabled` 防止当前状态下执行无效操作；
- `saving` 防止一般重复提交并显示处理状态；
- Validation 不通过时不应发送请求；
- 权限控制按钮显示只改善 UI，Backend 仍必须重新授权。

## 13. 一个完整 CRUD 页面怎样连接

```text
EmployeeListPage
├─ Search / Filter / Sort / Pagination
├─ GET /employees
├─ Loading / Empty / Error / Data
├─ 新增 → EmployeeCreatePage
├─ 编辑 → EmployeeEditPage
└─ 删除 → Dialog → DELETE → Toast → 重新读取

EmployeeCreatePage
└─ EmployeeForm → Validation → POST → 成功跳转

EmployeeEditPage
├─ GET /employees/{id}
├─ EmployeeForm
├─ Dirty Check
└─ Validation → PUT → 成功跳转
```

新增和编辑可以复用 `EmployeeForm`，但 Page 分别负责调用 `createEmployee()` 或 `updateEmployee()`。表单只负责输入和通知提交，不需要知道具体 API URL。

## 14. 调查既有业务页面

```text
URL 与 Route
  ↓
Page
  ↓
Form / Table / Dialog Component
  ↓
Hook 与事件处理器
  ↓
employeeService
  ↓
Network
```

调查“搜索条件为什么没生效”时，同时比较 Input State、URL Search Params、Service 参数和 Network Query。调查“删除后列表没变化”时，确认 DELETE 是否成功、父组件是否更新或重新读取，以及当前页是否越界。

## 15. 练习

1. 完成 Employee 新增表单，包含 Input、Select、Radio、Date 和 Validation。
2. 分别显示 Loading、Data、Empty、Error 四种列表状态。
3. 实现显式提交搜索及部门、状态筛选，并写入 URL。
4. 实现上一页/下一页、姓名升降序和越界禁用。
5. 删除当前页最后一条，验证页码修正以及失败时不移除画面数据。
6. 为编辑页增加 Dirty Check，并确认保存成功后不再提示离开。
7. 使用 Network 调查一次搜索、一次保存和一次删除请求。
8. 为 Dialog、Toast、Table 和表单错误做键盘及读屏检查。

## 16. 常见错误

- 点击确认后未禁用按钮：重复请求可能同时到达后端。
- 删除失败仍关闭 Dialog 并移除行：UI 与服务器事实不一致。
- Toast 是唯一错误出口：消息消失后用户无法恢复。
- 自制 Dialog 没有焦点管理：正式项目应优先使用团队验证过的组件。
- 关键字提交时覆盖排序和部门：复制现有 Search Params 后只更新目标字段。
- 排序后保留高页码：排序或主要筛选变化时回到第 1 页。
- 只排序当前页数组：服务端分页时必须把排序条件发送给后端。
- 把输入草稿直接当查询条件：用户每输入一个字符就改变 URL 和请求。
- Empty 显示为 Error：没有数据可能是正常查询结果。
- 只做前端 Validation：Backend 必须重新验证。
- Table 使用数组下标作为 key：排序、删除时可能复用错误行。
- 保存成功后 Dirty 仍为 true：需要更新表单基准数据。

## 本章检查点

- [ ] 能说明 Dialog、Loading、API、列表更新与 Toast 的事件顺序。
- [ ] 能让搜索、排序与分页使用同一 URL 查询状态。
- [ ] 能处理删除失败、当前页越界和服务端整体排序。
- [ ] 能实现受控 Employee Form 和字段 Validation。
- [ ] 能区分 Loading、Data、Empty 和 Error。
- [ ] 能说明 List、Create、Edit、Delete 的完整数据流。
