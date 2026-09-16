# 第 18 章 错误处理与重复提交

## 本章目标

- 把技术错误转换为安全、可行动的用户反馈。
- 统一处理 400/401/403/404/500、网络与超时。
- 保证提交状态在成功和失败后都一致。

## 1. 错误分类

```tsx
export type AppError =
  | { kind: 'validation'; message: string; fields?: Record<string, string> }
  | { kind: 'unauthorized'; message: string }
  | { kind: 'forbidden'; message: string }
  | { kind: 'notFound'; message: string }
  | { kind: 'conflict'; message: string }
  | { kind: 'network'; message: string }
  | { kind: 'timeout'; message: string }
  | { kind: 'server'; message: string; traceId?: string };
```

正式项目把它作为共享请求状态的错误参数，例如 `AsyncState<PageResponse<Employee>, AppError>`。简单章节里的字符串错误不传入正式项目的 `ErrorPanel`。

在请求边界统一映射：400 → `validation`、401 → `unauthorized`、403 → `forbidden`、404 → `notFound`、409 → `conflict`、500 → `server`；无 HTTP 响应时再区分 `timeout` 与其他 `network` 错误。UI 根据 kind 决定字段提示、登录恢复、无权限页、重新读取或重试。记录 trace ID 可以帮助后端查日志，但不要显示内部堆栈或敏感响应。

## 2. 防止重复提交

```tsx
async function handleSave(input: EmployeeInput) {
  if (saving) return;
  setSaving(true);
  setError(null);
  try {
    await updateEmployee(employeeId, input);
    navigate(`/employees/${employeeId}`);
  } catch (cause) {
    setError(toAppError(cause));
  } finally {
    setSaving(false);
  }
}
```

```text
Click → saving=true → Button disabled → API → finally → saving=false
```

UI 防护能减少误操作，但网络重试、双标签页或恶意请求仍可能重复到达。创建付款、订单等关键写入还应采用后端幂等策略。

## 3. Error Boundary 的边界

Error Boundary 用于捕获子树渲染中的未处理错误并显示备用 UI；它不自动捕获事件处理器、普通异步请求或服务端返回 500。请求错误仍应用明确 State 处理。函数组件项目常使用框架/路由提供的错误边界，类式 Error Boundary 放在附录阅读。

## 4. 练习

1. 建立状态码到 AppError 的映射表。
2. 让保存按钮在快速双击时只发一个请求，并在失败后恢复。
3. 为字段错误、页面错误和全局错误选择展示位置。
4. 模拟超时、401、403、404、409、500 和离线，记录恢复路径。

## 本章检查点

- [ ] 统一 AppError 能区分字段、认证、权限、冲突、网络与服务器错误。
- [ ] 写入失败后 saving 状态一定恢复，且不会伪造成功。
- [ ] 能区分 Error Boundary 与请求错误处理的职责。
