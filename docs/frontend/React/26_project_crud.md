# 第 26 章 实战：详情、新增、编辑与删除

本章追加四个页面和共享 EmployeeForm。核心目标是保持 ID、表单草稿、接口响应和列表刷新之间的一致性。

## 本章目标

- 复用统一 EmployeeInput、Service、AsyncState 与 AppError 完成 CRUD。
- 正确初始化编辑草稿，并处理 400、403、404、409 和 500。
- 验证取消、重复提交、缓存/列表同步和权限边界。

## 1. 详情页

```tsx
const employeeId = parseEmployeeId(useParams().id);
if (!employeeId) return <InvalidId />;

const { state, reload } = useEmployee(employeeId);
```

Hook 负责随 ID 读取、取消旧请求和四态 UI。404 显示“员工不存在”与返回列表；403 显示权限不足；不要把所有失败统一成“系统异常”。

## 2. 共享表单

```tsx
type EmployeeFormProps = {
  initialValue: EmployeeInput;
  submitLabel: string;
  onSubmit: (value: EmployeeInput) => Promise<void>;
};
```

新增页传空值；编辑页等待详情成功后再挂载表单，或通过明确 key/重置规则初始化草稿。不要简单 `useState(props.initialValue)` 后期待异步 Props 自动同步。

```text
Edit Page server data
        ↓ 初始化一次
EmployeeForm draft
        ↓ user edits / validates
PUT input
        ↓ success response
Detail Page
```

## 3. Create 与 Update

```tsx
async function submitCreate(input: EmployeeInput) {
  const created = await createEmployee(input);
  navigate(`/employees/${created.id}`, { replace: true });
}

async function submitEdit(input: EmployeeInput) {
  const updated = await updateEmployee(employeeId, input);
  navigate(`/employees/${updated.id}`, { replace: true });
}
```

Service 返回服务端最终对象，避免自行猜测 ID、默认状态或规范化字段。400 字段错误映射到表单；409 提示数据已被更新并给出重新载入选项。

## 4. Delete

```text
点击删除 → Dialog 确认 → deleting=true → DELETE
        → 成功：返回列表并刷新/失效缓存
        ↘ 失败：保留页面、恢复按钮、显示可行动错误
```

详情页删除成功后不能继续显示已删除对象。列表最后一项删除后修正页码。路由与 Store/缓存策略必须共享同一失效规则。

## 5. 常见错误

- 异步详情到达后仍显示空表单：明确等待挂载或草稿重置规则。
- Create 自行生成 ID：应使用服务端返回的 Employee。
- 409 时静默覆盖：提示数据已变化并让用户重新读取。
- 删除成功后详情和列表仍保留旧数据：统一刷新或缓存失效规则。

## 6. 验收测试

1. 详情：合法 ID、非法 ID、404、慢请求切换 ID。
2. 新增：必填、格式、重复提交、成功后进入服务端 ID。
3. 编辑：初始值、修改、取消、400、409、成功。
4. 删除：取消不请求、确认只请求一次、失败恢复、成功列表同步。
5. 权限：USER 直接访问新增/编辑 URL 得到 403/受保护页面，列表/详情不显示写按钮，API 同样拒绝；ADMIN 通过按钮和直接 URL 都能进入。
6. 运行 `npm run test -- --run` 与 `npm run build`，浏览器 Console 无错误。

## 本章检查点

- [ ] 详情、新增、编辑和删除复用同一类型与 Service。
- [ ] 每个页面覆盖正常、边界、权限和失败状态。
- [ ] 写入成功后路由、列表与服务器状态保持一致。
