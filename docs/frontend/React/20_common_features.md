# 第 20 章 常见业务 UI 功能

## 本章目标

- 用稳定状态模型实现 Loading、Dialog、Toast、分页、搜索和排序。
- 区分 URL 状态、局部 UI 状态和服务器查询条件。
- 为每个功能提供可观察成功与失败结果。

## 1. Loading、Dialog 与 Toast

Loading 应说明正在读取什么，长操作保留上下文；不要用全屏遮罩阻塞无关区域。确认 Dialog 至少需要标题、说明、确认/取消、初始焦点、焦点圈定与 Esc 行为，优先使用项目中经过可访问性验证的组件。

Toast 适合短暂的非阻塞反馈，如“保存成功”；字段错误和阻止流程的错误不应只用会消失的 Toast。重要消息同时保留在页面或日志中。

```tsx
type Toast = { id: string; tone: 'success' | 'error'; message: string };
```

### 1.1 删除、确认、Loading 与 Toast 的最小流程

下面是贯穿项目中的一个完整状态流程。`deleteEmployee` 复用第 11 章 Service；示例中的简化 Dialog 用于观察数据流，正式项目应换成团队已经处理焦点圈定、Esc、背景不可操作和动画的 UI Library Dialog。

```tsx
type DeleteEmployeeFlowProps = {
  employeeId: number;
  employeeName: string;
  onDeleted: (id: number) => void;
};

export function DeleteEmployeeFlow({
  employeeId,
  employeeName,
  onDeleted,
}: DeleteEmployeeFlowProps) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState<Toast | null>(null);

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

```tsx
const [draftKeyword, setDraftKeyword] = useState(keyword);

function handleSearch(event: React.FormEvent) {
  event.preventDefault();
  setSearchParams({ keyword: draftKeyword.trim(), page: '1' });
}
```

输入草稿属于表单状态；已提交查询条件属于 URL/页面状态。每次查询条件变化时页码通常重置为 1。若采用输入即检索，应明确防抖、取消旧请求和键盘体验。

## 3. Pagination

本章继续使用第 17 章 `types` 中的 `PageResponse<T>`，不在分页组件内重新声明另一套响应类型。`items`、`page`、`size` 和 `total` 必须直接对应后端分页契约。

页码、每页条数与总数来自同一接口契约。删除最后一页最后一条后，要处理当前页超出新总页数的情况。按钮提供可理解名称和当前页标记。

## 4. Sort

```text
点击“姓名”表头
→ 更新 sort=name,asc
→ URL 更新并请求服务端
→ 旧请求取消
→ 表头显示排序方向
```

服务端分页时必须由服务端排序；只排序当前页会制造看似正确但整体错误的结果。字段白名单由前后端共同约定，不把任意字符串直接传入数据库排序。

## 5. 练习

1. 实现显式提交搜索，条件进入 URL。
2. 实现上一页/下一页并禁用越界操作。
3. 实现姓名升降序并使用 `aria-sort`。
4. 删除当前页最后一条，验证页码修正。
5. 为成功、错误、Dialog 和 Loading 做键盘/读屏检查。

## 6. 常见错误

- 点击确认后未禁用按钮：重复请求可能同时到达后端。
- 删除失败仍关闭 Dialog 并移除行：UI 与服务器事实不一致。
- Toast 是唯一错误出口：消息消失后用户无法恢复。
- 自制 Dialog 没有焦点管理：正式项目应优先使用团队验证过的组件。

## 本章检查点

- [ ] 能说明 Dialog、Loading、API、列表更新与 Toast 的事件顺序。
- [ ] 能让搜索、排序与分页使用同一 URL 查询状态。
- [ ] 能处理删除失败、当前页越界和服务端整体排序。
