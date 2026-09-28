# 第 11 章 员工 CRUD 数据流

## 本章目标

- 理解 CRUD 与 HTTP Method 的关系。
- 根据 API 规格建立查询、详情、新增、修改和删除方法。
- 正确管理读取、保存、删除和错误状态。
- 判断写入成功后应更新本地 State，还是重新读取服务器数据。

第 10 章只完成了“读取员工列表”。本章沿用 Axios 和 Service 分层，补全员工管理功能。

## 1. CRUD 是什么

| 名称 | 含义 | 员工管理示例 | HTTP Method |
| --- | --- | --- | --- |
| Create | 创建 | 新增员工 | `POST` |
| Read | 读取 | 列表、详情 | `GET` |
| Update | 更新 | 修改员工 | `PUT` 或 `PATCH` |
| Delete | 删除 | 删除员工 | `DELETE` |

本课程采用以下 API。真实项目必须以 API 设计书为准。

| 功能 | Method | URL | 输出 |
| --- | --- | --- | --- |
| 查询 | `GET` | `/employees` | 分页列表 |
| 详情 | `GET` | `/employees/{id}` | 一名员工 |
| 新增 | `POST` | `/employees` | 新增后的员工 |
| 修改 | `PUT` | `/employees/{id}` | 修改后的员工 |
| 删除 | `DELETE` | `/employees/{id}` | 通常无 Body |

## 2. 先确认 API 契约

API 契约就是请求要传什么、响应会返回什么。分页查询条件示例：

```js
const query = {
  keyword: '田中',
  department: '开发部',
  page: 1,
  size: 10,
  sort: 'employeeCode,asc',
};
```

分页响应示例：

```js
{
  items: [
    {
      id: 1,
      employeeCode: 'EMP0001',
      name: '田中太郎',
      department: '开发部',
      email: 'tanaka@example.com'
    }
  ],
  page: 1,
  size: 10,
  total: 24
}
```

新增时不传数据库 ID。成功后使用后端返回的完整对象，不要由前端猜测新 ID。

## 3. Employee Service

`src/services/employeeService.js`：

```js
import { httpClient } from './httpClient';

export async function searchEmployees(query, signal) {
  const response = await httpClient.get('/employees', {
    params: query,
    signal,
  });
  return response.data;
}

export async function getEmployee(id, signal) {
  const response = await httpClient.get(`/employees/${id}`, { signal });
  return response.data;
}

export async function createEmployee(input) {
  return (await httpClient.post('/employees', input)).data;
}

export async function updateEmployee(id, input) {
  return (await httpClient.put(`/employees/${id}`, input)).data;
}

export async function deleteEmployee(id) {
  await httpClient.delete(`/employees/${id}`);
}
```

Service 只负责通信，不显示弹窗、不迁移页面，也不直接修改组件 State。第 10 章的 `getEmployees()` 是无分页练习；从本章开始，列表改用 `searchEmployees()`。

## 4. 完整数据流

```text
用户操作 → 前端校验 → Page → Service → Axios
        → Backend → Database → Response → 更新 State → 重新渲染
```

前端校验用于尽早提示；后端仍必须二次校验。

## 5. Read：列表与详情

列表仍然处理 `loading`、`error`、`empty`、`success` 四态：

```jsx
async function loadEmployees(signal) {
  setLoading(true);
  setErrorMessage('');

  try {
    const result = await searchEmployees({
      keyword,
      department,
      page,
      size: 10,
      sort: 'employeeCode,asc',
    }, signal);

    setEmployees(result.items);
    setTotal(result.total);
  } catch (error) {
    if (error.name !== 'CanceledError') {
      setErrorMessage('员工列表读取失败');
    }
  } finally {
    setLoading(false);
  }
}
```

搜索条件改变时通常把 `page` 恢复为 `1`，避免停留在不存在的页码。

详情不能依赖列表已经加载。用户可能直接打开详情页面，所以应按 ID 请求：

```jsx
const id = Number(employeeId);

if (!Number.isInteger(id) || id <= 0) {
  setErrorMessage('员工 ID 不正确');
  return;
}

const employee = await getEmployee(id, controller.signal);
setEmployee(employee);
```

本章尚未学习 Router，先把 `employeeId` 看作父组件传入的 Props。先验证 ID，再拼入 URL。

## 6. Create：新增员工

```text
点击保存 → 防止重复提交 → 前端校验
校验失败 → 显示错误，不调用 API
校验成功 → 调用 API → 成功后更新页面
```

```jsx
async function handleCreate(event) {
  event.preventDefault();
  if (saving) return;

  const errors = validateEmployee(formValues);
  setFieldErrors(errors);
  if (Object.keys(errors).length > 0) return;

  setSaving(true);
  setErrorMessage('');

  try {
    const created = await createEmployee(formValues);
    setEmployees((previous) => [...previous, created]);
    setSuccessMessage('员工新增成功');
  } catch (error) {
    setErrorMessage('员工新增失败，请确认输入内容');
  } finally {
    setSaving(false);
  }
}
```

```jsx
<button type="submit" disabled={saving}>
  {saving ? '保存中...' : '保存'}
</button>
```

失败时保留输入内容；只有成功后才显示成功信息或迁移页面。

## 7. Update：修改员工

修改页先读取现有数据，再把编辑结果发送给后端：

```jsx
async function handleUpdate(event) {
  event.preventDefault();
  if (saving) return;

  const errors = validateEmployee(formValues);
  setFieldErrors(errors);
  if (Object.keys(errors).length > 0) return;

  setSaving(true);
  setErrorMessage('');

  try {
    const updated = await updateEmployee(employee.id, formValues);
    setEmployees((previous) => previous.map((item) =>
      item.id === updated.id ? updated : item,
    ));
    setSuccessMessage('员工资料修改成功');
  } catch (error) {
    setErrorMessage('员工资料修改失败');
  } finally {
    setSaving(false);
  }
}
```

## 8. Delete：删除员工

删除前确认，只禁用正在删除的行；失败时保留数据并恢复按钮。

```jsx
async function handleDelete(id, name) {
  const confirmed = window.confirm(
    `${name} を削除してもよろしいですか？`,
  );
  if (!confirmed || deletingId !== null) return;

  setDeletingId(id);
  setErrorMessage('');

  try {
    await deleteEmployee(id);
    setEmployees((previous) =>
      previous.filter((item) => item.id !== id),
    );
  } catch (error) {
    setErrorMessage('删除失败，请重新读取后再试');
  } finally {
    setDeletingId(null);
  }
}
```

```jsx
<button
  type="button"
  disabled={deletingId === employee.id}
  onClick={() => handleDelete(employee.id, employee.name)}
>
  {deletingId === employee.id ? '删除中...' : '删除'}
</button>
```

`window.confirm()` 用于先学习流程。真实项目通常使用符合设计和无障碍要求的 Dialog。

## 9. 写入后怎样同步数据

| 方法 | 适合场景 |
| --- | --- |
| 使用 API 响应更新 State | 简单列表，响应已包含完整对象 |
| 写入成功后重新查询 | 服务端排序、分页、计算字段或权限过滤 |

删除当前页最后一件数据时，还要判断是否回到上一页并重新查询。不存在适用于所有项目的唯一方案。

本章采用“等待 API 成功后再更新页面”。先改 UI 再请求的乐观更新虽然更快，但失败时必须恢复数据，新人主线暂不采用。

## 10. HTTP 错误与页面处理

| 状态 | 常见含义 | 页面处理 |
| --- | --- | --- |
| `400` | 输入或业务条件错误 | 保留表单并显示错误 |
| `401` | 未登录或登录失效 | 引导重新登录 |
| `403` | 无权限 | 显示权限错误 |
| `404` | 员工已不存在 | 提示并返回列表或重新读取 |
| `409` | 编号重复或更新冲突 | 提示冲突并读取最新数据 |
| `500` | 后端异常 | 显示通用错误并允许重试 |

出现问题时使用第 10 章的 Network 面板检查 Request、Status 和 Response，不要只猜错误原因。

## 11. 并发更新

用户打开编辑页后，其他人可能已修改同一数据。后端可通过版本号或 ETag 检查冲突，并返回 `409 Conflict`。前端应提示重新读取，不应静默覆盖。具体规则以 API 规格为准。

## 12. 常见错误

- 把 Axios 调用散落在组件中：统一放入 Service。
- 前端自己生成数据库 ID：使用新增 API 的响应。
- 保存中仍可连续点击：使用 `saving` 禁用按钮。
- API 失败仍关闭表单：失败时保留输入。
- 删除成功前先移除数据：新人主线先等待响应。
- 删除后留下空白高页码：调整页码并重新查询。

## 13. 练习

1. 完成五个 Employee Service 方法，并记录 Method、URL、输入和输出。
2. 完成新增流程：校验失败不请求、保存中禁用、失败保留输入。
3. 完成删除流程：确认、行级禁用、成功更新、失败恢复。
4. 分别设计新增、修改和删除遇到 `400`、`404`、`409`、`500` 时的页面行为。

## 本章检查点

- [ ] 能说明 CRUD 与 HTTP Method 的对应关系。
- [ ] 能读懂分页查询的输入与输出。
- [ ] 能说明 `getEmployees()` 如何迁移为 `searchEmployees()`。
- [ ] 能通过 Service 完成查询、详情、新增、修改和删除。
- [ ] 能区分 `loading`、`saving` 和 `deletingId`。
- [ ] 校验失败时不会调用写入 API。
- [ ] 写入失败时不会提前显示成功。
- [ ] 能判断本地更新和重新查询的适用场景。
