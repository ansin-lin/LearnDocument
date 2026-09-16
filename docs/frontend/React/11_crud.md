# 第 11 章 员工 CRUD 数据流

## 本章目标

- 为 List、Detail、Create、Update、Delete 定义 Service。
- 在写入成功后选择本地更新或重新读取。
- 处理确认、并发和失败恢复。

## 1. 接口函数

进入分页 CRUD 后，用 `searchEmployees` 替换第 10 章仅用于单点请求练习的无分页 `getEmployees`。后续实战统一使用下面的查询契约：

```tsx
export type EmployeeInput = Omit<Employee, 'id'>;
export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
};
export type EmployeeQuery = {
  keyword: string;
  department: string;
  page: number;
  size: number;
  sort: 'name,asc' | 'name,desc';
};

export async function searchEmployees(
  query: EmployeeQuery,
  signal?: AbortSignal,
): Promise<PageResponse<Employee>> {
  return (await httpClient.get<PageResponse<Employee>>('/employees', {
    params: query,
    signal,
  })).data;
}

export async function getEmployee(
  id: number,
  signal?: AbortSignal,
): Promise<Employee> {
  return (await httpClient.get<Employee>(`/employees/${id}`, { signal })).data;
}

export async function createEmployee(input: EmployeeInput): Promise<Employee> {
  return (await httpClient.post<Employee>('/employees', input)).data;
}

export async function updateEmployee(
  id: number,
  input: EmployeeInput,
): Promise<Employee> {
  return (await httpClient.put<Employee>(`/employees/${id}`, input)).data;
}

export async function deleteEmployee(id: number): Promise<void> {
  await httpClient.delete(`/employees/${id}`);
}
```

`PageResponse<T>` 表达分页成功响应；页面仍用第 10 章的 loading、error、empty、success 四态呈现它。第 17 章会把此类型原样移动到共享类型文件。URL 参数必须来自已验证的数字 ID。不要把对象直接拼进 URL，也不要假设前端隐藏操作就能保证权限。

## 2. 完整数据流

```text
表单/按钮
   ↓ event + validation
Page / Custom Hook
   ↓ EmployeeInput
Service
   ↓ HTTP
Backend → Database
   ↓ response
State 更新 / 重新读取
   ↓
UI + message
```

写入成功后有两种常见策略：使用响应结果更新当前列表，响应快；或重新读取后端列表，最接近服务端事实。选择取决于排序、分页、服务端默认值和缓存策略，不要无条件乐观删除。

## 3. 删除流程

```tsx
async function handleDelete(id: number) {
  if (!window.confirm('确定删除该员工吗？')) return;
  setDeletingId(id);
  setError(null);
  try {
    await deleteEmployee(id);
    setEmployees((previous) => previous.filter((item) => item.id !== id));
  } catch {
    setError('删除失败，请刷新后重试');
  } finally {
    setDeletingId(null);
  }
}
```

企业项目宜把确认框替换为可访问的 Dialog，但数据流程相同。只禁用正在删除的行；失败时恢复按钮且不要从 UI 移除数据。

## 4. 更新冲突

用户打开编辑页后，数据可能已被别人修改。实际接口可使用版本号或 ETag 检测并发更新；收到冲突时应提示重新加载，不要静默覆盖。该策略由 API 规格决定。

## 5. 练习

1. 完成查询、详情、创建、更新和删除 Service，并为输入/输出写类型。
2. 完成删除的确认、禁用、成功更新和失败恢复。
3. 比较“本地更新列表”与“重新 GET”的适用场景。
4. 设计 404、409 和 500 时编辑页的行为。

## 本章检查点

- [ ] 能说明 `getEmployees` 练习如何迁移为分页 `searchEmployees`。
- [ ] 读取接口可接收 AbortSignal，写入接口返回值与契约一致。
- [ ] 删除与更新失败时 UI 不会提前伪造成功状态。
