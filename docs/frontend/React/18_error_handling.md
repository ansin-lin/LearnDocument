# 第 18 章 错误处理与重复提交

## 本章目标

- 把技术错误转换为安全、可行动的用户反馈。
- 统一处理 400/401/403/404/500、网络与超时。
- 保证提交状态在成功和失败后都一致。

## 1. 错误分类

正式项目统一使用包含 `kind` 和 `message` 的错误对象。字段校验错误还可以包含 `fields`，服务器错误可以包含用于调查的 `traceId`：

```jsx
const validationError = {
  kind: 'validation',
  message: '输入内容不正确',
  fields: { email: '邮箱格式不正确' },
};

const serverError = {
  kind: 'server',
  message: '服务器处理失败',
  traceId: 'example-trace-id',
};
```

正式项目的请求失败状态把该错误对象保存在 `error` 字段中。不要让同一个 `ErrorPanel` 有时收到字符串、有时收到结构化对象。

在请求边界统一映射：400 → `validation`、401 → `unauthorized`、403 → `forbidden`、404 → `notFound`、409 → `conflict`、500 → `server`；无 HTTP 响应时再区分 `timeout` 与其他 `network` 错误。UI 根据 kind 决定字段提示、登录恢复、无权限页、重新读取或重试。记录 trace ID 可以帮助后端查日志，但不要显示内部堆栈或敏感响应。

### 1.1 实现统一转换函数

新建 `src/errors/toAppError.js`：

```js
import axios from 'axios';

export function toAppError(cause) {
  if (!axios.isAxiosError(cause)) {
    return { kind: 'unknown', message: '处理过程中发生错误' };
  }

  if (cause.code === 'ERR_CANCELED') {
    return { kind: 'canceled', message: '请求已取消' };
  }

  if (cause.code === 'ECONNABORTED' || cause.code === 'ETIMEDOUT') {
    return { kind: 'timeout', message: '请求超时，请稍后重试' };
  }

  if (!cause.response) {
    return { kind: 'network', message: '无法连接服务器' };
  }

  const status = cause.response.status;
  const data = cause.response.data;

  if (status === 400) {
    return {
      kind: 'validation',
      message: data?.message ?? '输入内容不正确',
      fields: data?.details ?? {},
    };
  }
  if (status === 401) return { kind: 'unauthorized', message: '请重新登录' };
  if (status === 403) return { kind: 'forbidden', message: '没有操作权限' };
  if (status === 404) return { kind: 'notFound', message: '数据不存在' };
  if (status === 409) return { kind: 'conflict', message: data?.message ?? '数据发生冲突' };

  return {
    kind: 'server',
    message: '服务器处理失败，请稍后重试',
    traceId: data?.traceId,
  };
}
```

`axios.isAxiosError()` 判断错误是否来自 Axios。存在 `response` 表示服务器已经响应；不存在时才考虑网络中断或 CORS 等连接问题。取消请求不是普通失败，不应总是显示红色错误。

项目 API 的错误 Body 结构可能不同，`message`、`details`、`traceId` 必须按照接口规格调整。不要直接把后端堆栈或原始敏感内容显示给用户。

### 1.2 错误显示位置

| 错误 | 推荐位置 | 恢复方式 |
| --- | --- | --- |
| 字段校验 | 对应输入项附近 | 修正输入 |
| 页面读取失败 | 当前内容区域 | 重试读取 |
| 401 | 统一认证流程 | 重新登录 |
| 403 | 禁止访问页或当前操作附近 | 返回允许页面 |
| 409 | 编辑表单上方 | 重新读取最新数据 |
| 全局渲染错误 | Error Boundary 备用 UI | 重新进入或报告问题 |

`ErrorPanel` 始终接收统一错误对象：

```jsx
export function ErrorPanel({ error, onRetry }) {
  return (
    <section role="alert">
      <p>{error.message}</p>
      {error.traceId && <p>問い合わせID：{error.traceId}</p>}
      {onRetry && <button type="button" onClick={onRetry}>重试</button>}
    </section>
  );
}
```

## 2. 防止重复提交

```jsx
async function handleSave(input) {
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

对于必须严格阻止同一组件内瞬间重复进入的处理，可以同时使用 Ref 作为同步锁：

```jsx
const savingRef = useRef(false);

async function handleSave(input) {
  if (savingRef.current) return;

  savingRef.current = true;
  setSaving(true);
  setError(null);

  try {
    await updateEmployee(employeeId, input);
    navigate(`/employees/${employeeId}`);
  } catch (cause) {
    setError(toAppError(cause));
  } finally {
    savingRef.current = false;
    setSaving(false);
  }
}
```

State 用来更新按钮画面，Ref 用来立即阻止重复进入。它们仍不能代替后端幂等和数据库约束。

## 3. Error Boundary 的边界

Error Boundary 用于捕获子树渲染中的未处理错误并显示备用 UI；它不自动捕获事件处理器、普通异步请求或服务端返回 500。请求错误仍应用明确 State 处理。函数组件项目常使用框架/路由提供的错误边界，类式 Error Boundary 放在附录阅读。

例如路由支持错误页面时，可以为路由配置 `errorElement`：

```jsx
<Route
  path="employees"
  element={<EmployeeListPage />}
  errorElement={<UnexpectedErrorPage />}
/>
```

这类边界处理“组件渲染或路由执行出现未处理异常”。员工列表 API 返回 500 时，仍应由页面的 `try...catch` 转换为 `AppError` 并提供重试。

## 4. 一次请求的完整状态

```text
idle
 ↓ 开始请求
loading / saving
 ├─ success → 清除旧错误并显示结果
 ├─ expected error → AppError → 页面恢复入口
 └─ canceled → 不覆盖新结果
 ↓ finally
恢复 loading / saving
```

## 5. 练习

1. 建立状态码到统一错误对象的映射表。
2. 让保存按钮在快速双击时只发一个请求，并在失败后恢复。
3. 为字段错误、页面错误和全局错误选择展示位置。
4. 模拟超时、401、403、404、409、500 和离线，记录恢复路径。

## 本章检查点

- [ ] 统一错误对象能区分字段、认证、权限、冲突、网络与服务器错误。
- [ ] 写入失败后 saving 状态一定恢复，且不会伪造成功。
- [ ] 能区分 Error Boundary 与请求错误处理的职责。
