# 第 18 章 API 错误处理与故障调查

## 本章目标

- 把技术错误转换为安全、可行动的用户反馈。
- 统一处理 400/401/403/404/500、网络与超时。
- 保证提交状态在成功和失败后都一致。
- 能使用 DevTools Network 调查请求失败。
- 能区分 API Error、JavaScript Error 与 Render Error。

## 1. 什么叫错误处理

错误处理不只是 `catch` 后显示“失败”。前端首先要判断发生了哪一类问题，再决定显示位置、恢复方法和调查方向。

| 错误类型 | Employee 案例 | 主要处理位置 |
| --- | --- | --- |
| 输入错误 | 邮箱格式错误、姓名未填写 | 表单字段附近 |
| HTTP Error | API 返回 400、404、500 | Page / Hook 的请求状态 |
| Network Error | Backend 未启动、连接中断、CORS | 页面错误区域与 Network |
| Timeout | 请求超过 Axios timeout | 页面重试入口 |
| Canceled | 切换条件后旧请求被取消 | 通常不显示红色错误 |
| JavaScript Error | 读取 `undefined.name` | Console、代码修正 |
| React Render Error | 组件渲染期间抛出异常 | Error Boundary |

错误处理包含三个目标：

```text
用户：知道发生什么、下一步能做什么
程序：恢复 loading / saving 等状态
开发者：保留 Status、traceId 等调查线索
```

## 2. Axios 请求失败时发生了什么

```js
try {
  const employee = await getEmployee(employeeId);
  setEmployee(employee);
} catch (error) {
  console.error(error);
}
```

进入 `catch` 后，`error` 不一定都有相同结构。Axios 请求失败时通常得到 AxiosError，常用字段如下：

| 字段 | 含义 |
| --- | --- |
| `error.message` | Axios 生成的基础错误说明 |
| `error.code` | `ERR_CANCELED`、`ECONNABORTED` 等代码 |
| `error.response` | 服务器已经返回的响应；连接阶段失败时不存在 |
| `error.response.status` | HTTP Status，如 400、500 |
| `error.response.data` | Backend 返回的错误 Body |
| `error.request` | 请求已建立但没有得到可用响应时的底层请求信息 |

判断方向：

```text
有 error.response
→ Backend 或 Proxy 已经返回 HTTP 响应
→ 继续看 status 和 response.data

没有 error.response
→ 没有得到可用 HTTP 响应
→ 检查 Backend、URL、网络、CORS、timeout 或取消
```

不要直接把 `error.response.data` 全部显示给用户，其中可能包含内部实现或敏感信息。

## 3. HTTP Status 与 Employee 业务场景

| Status | Employee 案例 | 前端基本处理 |
| --- | --- | --- |
| 400 | 邮箱格式、必填项或请求 Body 不正确 | 显示字段或表单错误 |
| 401 | Session / Token 失效 | 进入统一的重新认证流程 |
| 403 | 普通 USER 尝试删除员工 | 显示无权限，不伪装成登录失效 |
| 404 | 打开已被删除的员工详情 | 显示数据不存在并提供返回入口 |
| 409 | 两个人同时编辑，同一版本发生冲突 | 提示重新读取最新数据 |
| 500 | Backend 发生未处理异常 | 显示通用错误并保留 traceId |

状态码只是分类起点，最终显示内容仍应以项目 API 规格为准。例如 400 的字段错误可能放在 `details`，也可能使用其他字段名。

## 4. AppError：统一前端错误模型

如果每个 Page 都重复判断：

```js
if (error.response?.status === 400) { /* ... */ }
if (error.response?.status === 401) { /* ... */ }
if (error.response?.status === 500) { /* ... */ }
```

同一错误会在不同页面显示成不同文字，API 错误 Body 改变时也要修改许多文件。因此在请求边界统一转换为应用内部的 `AppError`。

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

### 4.1 实现统一转换函数

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

### 4.2 错误显示位置

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

## 5. DevTools Network 调查

假设用户点击“保存”，页面只显示“保存失败”。不要马上修改代码或猜测 Backend 问题，先按以下顺序调查：

```text
F12
  ↓
Network
  ↓
重新执行保存操作
  ↓
找到 POST /employees
```

选中请求后检查：

| 项目 | 确认内容 |
| --- | --- |
| Request URL | 是否请求了正确环境和路径 |
| Request Method | 应该是 GET、POST、PUT 还是 DELETE |
| Status Code | 400、401、500，还是没有响应 |
| Request Headers | Content-Type、认证信息是否符合规格 |
| Request Payload | 字段名、值、空值和日期格式是否正确 |
| Response | Backend 返回的 message、details、traceId |
| Timing | 是否长时间等待或发生 timeout |

Network 没有请求时，问题通常还在前端事件、Validation、按钮 disabled 或 JavaScript Error；Network 已有请求时，再根据请求和响应判断前后端边界。

## 6. API 500 的标准调查流程

```text
① 使用相同账号和输入复现操作
② 在 Network 找到失败 API
③ 确认 URL、Method 和 Status=500
④ 确认 Request Payload 是否符合规格
⑤ 确认 Response，记录 message 与 traceId
⑥ 查看 Console 是否同时存在前端异常
⑦ 判断前端请求错误，还是 Backend 处理错误
⑧ 整理复现步骤、期待结果、实际结果
⑨ 必要时把 traceId 交给 Backend 调查服务器日志
```

不要只提交“保存失败”的缺陷信息。至少记录操作条件、API、Status、必要的请求字段、响应摘要和发生时间。不得把密码、Token 或不必要的个人信息贴入缺陷单。

## 7. 防止重复提交

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

只写 `if (saving) return` 可以改善一般操作，但 State 更新会在 React 后续渲染中生效；同一瞬间快速触发的两个事件可能都读取到旧的 `saving=false`。因此需要严格阻止同一组件内重复进入时，可以用 Ref 保存立即变化的锁。

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

## 8. Error Boundary 的边界

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

| 情况 | Error Boundary 是否负责 |
| --- | --- |
| 子组件 Render 时抛出异常 | 是 |
| 路由 Loader / Render 未处理异常 | 取决于路由边界配置 |
| API 返回 500 | 否，使用请求 State |
| 点击事件内部抛出异常 | 通常不自动捕获 |
| 普通 `async` 函数 Reject | 否，使用 `try...catch` |

## 9. 一次请求的完整状态

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

## 10. Debug 实战

| Case | 现象 | 第一调查点 |
| --- | --- | --- |
| 1 | Network 返回 400 | Payload 和 Response 字段错误 |
| 2 | Network 返回 200，但页面报错 | Console、响应结构与前端数据转换 |
| 3 | 点击后 Network 没有请求 | 事件、Validation、disabled、Console |
| 4 | Network 返回 500 | Response、traceId、Backend 日志 |
| 5 | `ERR_NETWORK` | URL、Backend、Proxy、CORS、网络 |
| 6 | `ERR_CANCELED` | 是否为切换条件时主动取消旧请求 |
| 7 | 快速双击出现两次 POST | 按钮状态、Ref 锁和 Backend 幂等 |

### 练习

1. 建立状态码到统一错误对象的映射表，并给每种错误选择显示位置。
2. 用 DevTools 调查一次 400，记录 Request Payload 和 Response。
3. 模拟 500，整理复现步骤、期待结果、实际结果和 traceId。
4. 制造“Network 200 但画面报错”，通过 Console 找到错误字段。
5. 让保存按钮在快速双击时只发一个请求，并在失败后恢复。
6. 模拟超时、401、403、404、409、离线和取消，说明各自恢复路径。

## 本章检查点

- [ ] 统一错误对象能区分字段、认证、权限、冲突、网络与服务器错误。
- [ ] 写入失败后 saving 状态一定恢复，且不会伪造成功。
- [ ] 能区分 Error Boundary 与请求错误处理的职责。
- [ ] 能解释 AxiosError 中 response 存在与不存在的区别。
- [ ] 能通过 Network 的 Request、Response 和 Status 调查 API 失败。
- [ ] 能按照标准流程整理一次 API 500 的调查结果。
