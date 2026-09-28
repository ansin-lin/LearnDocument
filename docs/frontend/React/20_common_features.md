# 第 20 章 常见业务 UI 功能

## 本章目标

- 用稳定状态模型实现 Loading、Dialog、Toast、分页、搜索和排序。
- 区分 URL 状态、局部 UI 状态和服务器查询条件。
- 为每个功能提供可观察成功与失败结果。

## 1. Loading、Dialog 与 Toast

Loading 应说明正在读取什么，长操作保留上下文；不要用全屏遮罩阻塞无关区域。确认 Dialog 至少需要标题、说明、确认/取消、初始焦点、焦点圈定与 Esc 行为，优先使用项目中经过可访问性验证的组件。

Toast 适合短暂的非阻塞反馈，如“保存成功”；字段错误和阻止流程的错误不应只用会消失的 Toast。重要消息同时保留在页面或日志中。

Toast 使用包含 `id`、`tone` 和 `message` 的对象。`tone` 在本例中使用 `success` 或 `error`。

### 1.1 删除、确认、Loading 与 Toast 的最小流程

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

## 2. Search

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
const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1);
const size = 10;
const sort = searchParams.get('sort') ?? 'employeeCode,asc';

const query = { keyword, department, page, size, sort };
```

输入框草稿和已提交查询条件不要混为一份 State。用户输入尚未提交时，不应每按一个字符就改变 URL，除非规格明确要求即时搜索并已处理防抖与请求取消。

## 3. Pagination

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

删除后重新查询。如果当前页除被删除项外已无数据且 `page > 1`，先把 URL 页码减 1，再按新页码查询。

## 4. Sort

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

## 5. 查询与请求的完整连接

```jsx
useEffect(() => {
  const controller = new AbortController();

  loadEmployees(query, controller.signal);

  return () => controller.abort();
}, [keyword, department, page, size, sort]);
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

## 6. 练习

1. 实现显式提交搜索，条件进入 URL。
2. 实现上一页/下一页并禁用越界操作。
3. 实现姓名升降序并使用 `aria-sort`。
4. 删除当前页最后一条，验证页码修正。
5. 为成功、错误、Dialog 和 Loading 做键盘/读屏检查。

## 7. 常见错误

- 点击确认后未禁用按钮：重复请求可能同时到达后端。
- 删除失败仍关闭 Dialog 并移除行：UI 与服务器事实不一致。
- Toast 是唯一错误出口：消息消失后用户无法恢复。
- 自制 Dialog 没有焦点管理：正式项目应优先使用团队验证过的组件。
- 关键字提交时覆盖排序和部门：复制现有 Search Params 后只更新目标字段。
- 排序后保留高页码：排序或主要筛选变化时回到第 1 页。
- 只排序当前页数组：服务端分页时必须把排序条件发送给后端。

## 本章检查点

- [ ] 能说明 Dialog、Loading、API、列表更新与 Toast 的事件顺序。
- [ ] 能让搜索、排序与分页使用同一 URL 查询状态。
- [ ] 能处理删除失败、当前页越界和服务端整体排序。
