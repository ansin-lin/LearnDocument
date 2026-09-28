# 第 10 章 REST API、Axios 与异步 UI

## 本章目标

- 【必须掌握】说明浏览器、React、API、后端和数据库之间的请求链路。
- 【必须掌握】识别 HTTP Method、URL、Query、Headers、Body、Status 和 Response。
- 【必须掌握】建立 Axios 共用实例和业务 Service。
- 【必须掌握】使用 Effect 读取数据并处理 loading、error、empty、success。
- 【必须掌握】使用 Network 面板调查 API 问题。
- 【会使用、能看懂】处理 timeout、取消、CORS 和外部数据校验边界。

## 1. React 和 API 分别负责什么

React 负责组件、State 和画面更新，不负责数据库访问。前端通过 HTTP API 请求后端，后端确认权限、执行业务并访问数据库。

```text
User
 ↓ 操作
React Component
 ↓ 调用函数
Employee Service
 ↓ Axios / HTTP
Backend API
 ↓ 业务、权限
Database
 ↓
JSON Response
 ↓
State
 ↓
React UI
```

浏览器不能安全地直接连接业务数据库。数据库账号、SQL 和最终权限判断应留在后端。

## 2. 一次 HTTP 请求包含什么

以读取员工列表为例：

```text
GET /api/employees?department=SALES
Accept: application/json
```

后端可能返回：

```text
Status: 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": 1001,
    "name": "田中太郎",
    "department": "SALES",
    "status": "ACTIVE"
  }
]
```

| 项目 | 作用 | 示例 |
| --- | --- | --- |
| Method | 表示操作意图 | `GET`、`POST` |
| URL | 表示目标资源 | `/api/employees` |
| Query | 表示检索、分页等条件 | `?department=SALES` |
| Headers | 传递内容格式、认证等附加信息 | `Accept: application/json` |
| Body | 写入时发送的数据 | 员工 JSON |
| Status | 表示处理结果 | `200`、`400`、`500` |
| Response Body | 后端返回的数据 | 员工数组或错误对象 |

REST 是一种接口设计风格，不等于 JSON，也不等于 React。前端必须按照项目 API 规格使用正确 URL、Method、参数和字段。

## 3. 为什么本课程使用 Axios

第九章用浏览器 `fetch()` 观察 Effect 和取消。本章开始使用 Axios 统一项目请求。Axios 提供：

- 共用 `baseURL` 和 timeout。
- 自动处理常见 JSON 响应。
- `params`、请求体和响应对象的统一写法。
- 请求/响应拦截器扩展点。
- 较一致的错误对象。

Axios 不是 React 的一部分，需要安装：

```bash
npm install --save-exact axios@1.20.0
```

安装后 `package.json` 和 `package-lock.json` 会变化，应一并提交。既有项目先遵守自己的锁文件版本，不要直接覆盖依赖。

## 4. 建立 Axios 共用实例

在项目根目录创建 `.env.development`：

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

新建 `src/services/httpClient.js`：

```jsx
import axios from 'axios';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10_000,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
});
```

`axios.create(options)` 创建共用 Axios 实例：

| 配置 | 当前值 | 作用 |
| --- | --- | --- |
| `baseURL` | `VITE_API_BASE_URL` | 自动添加到相对 API 路径前 |
| `timeout` | `10000` 毫秒 | 请求超过等待时间后失败 |
| `Accept` | `application/json` | 表示希望接收 JSON |
| `Content-Type` | `application/json` | 表示发送 JSON；文件上传时不要固定它 |

Vite 的 `VITE_` 变量会进入浏览器构建产物，不能保存数据库密码、私钥、真实密钥或其他秘密。环境变量改变后通常需要重启开发服务器。

## 5. 建立员工 Service

新建 `src/services/employeeService.js`：

```jsx
import { httpClient } from './httpClient.js';

export async function getEmployees(signal) {
  const response = await httpClient.get('/employees', {
    signal,
  });

  return response.data;
}
```

`httpClient.get(url, options)` 发送 GET 请求：

- `'/employees'` 会与 `baseURL` 组成完整 URL。
- `signal` 用于取消请求，可以省略。
- 返回的 Promise 成功后得到 Axios Response。
- `response.data` 是响应体中的业务数据。

带查询条件时使用 `params`，不要手工拼接未编码字符串：

```jsx
export async function getEmployeesByDepartment(department, signal) {
  const response = await httpClient.get('/employees', {
    params: { department },
    signal,
  });
  return response.data;
}
```

Service 负责 URL、Method、参数和响应提取；组件负责 loading、错误提示和画面。不要在每个组件中重复 `axios.get(...)`。

## 6. 异步页面的四种状态

读取页面至少需要考虑：

```text
loading  正在等待响应
error    请求失败
empty    请求成功，但数组为空
success  请求成功，并有数据
```

空数据不是错误。后端正常返回 `[]` 时，应显示“没有数据”，而不是“系统异常”。

## 7. 在 Effect 中读取员工列表

新建 `src/pages/EmployeeListPage.jsx`：

```jsx
import axios from 'axios';
import { useEffect, useState } from 'react';
import { EmployeeList } from '../components/EmployeeList.jsx';
import { getEmployees } from '../services/employeeService.js';

export function EmployeeListPage() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadEmployees() {
      setLoading(true);
      setErrorMessage('');

      try {
        const data = await getEmployees(controller.signal);
        if (!Array.isArray(data)) {
          throw new Error('员工列表响应格式不正确');
        }
        setEmployees(data);
      } catch (cause) {
        if (!axios.isCancel(cause)) {
          setErrorMessage('员工列表读取失败，请稍后重试。');
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void loadEmployees();
    return () => controller.abort();
  }, [reloadKey]);

  if (loading) {
    return <p role="status">员工列表读取中...</p>;
  }

  if (errorMessage) {
    return (
      <section role="alert">
        <p>{errorMessage}</p>
        <button type="button" onClick={() => setReloadKey((value) => value + 1)}>
          重试
        </button>
      </section>
    );
  }

  if (employees.length === 0) {
    return <p>没有符合条件的员工。</p>;
  }

  return <EmployeeList employees={employees} />;
}
```

`reloadKey` 只用于表达“重新读取一次”。点击重试后它发生变化，Effect 先清理旧请求，再执行新请求。更复杂的项目可以把读取流程提取到自定义 Hook 或服务器状态库。

## 8. 错误对象与状态码

Axios 请求失败时可能存在不同情况：

```jsx
if (axios.isAxiosError(cause)) {
  console.log(cause.response?.status);
  console.log(cause.response?.data);
  console.log(cause.code);
}
```

| 结果 | 基础理解 | 前端常见处理 |
| --- | --- | --- |
| 400 | 请求内容不正确 | 显示字段或业务错误 |
| 401 | 未认证或登录失效 | 进入统一登录恢复流程 |
| 403 | 已认证但没有权限 | 显示无权限，不应只提示重新登录 |
| 404 | API 或资源不存在 | 检查 URL，或显示资源不存在 |
| 409 | 数据冲突 | 提示重新读取或重新确认 |
| 500 | 后端异常 | 显示安全提示并保留调查信息 |
| timeout | 超过等待时间 | 提示超时并允许重试 |
| Network Error | 无法连接或被浏览器阻止 | 检查服务、地址、网络和 CORS |

不要把后端堆栈、SQL、Token、Cookie 或敏感响应直接显示给用户。开发日志也应避免记录个人信息。

## 9. 使用 Network 面板调查

打开浏览器开发者工具：

```text
F12
→ Network
→ 选择请求
→ Headers / Payload / Response / Timing
```

按以下顺序确认：

1. Request URL 是否正确。
2. Request Method 是否符合 API 规格。
3. Query String 或 Request Payload 是否正确。
4. Status Code 是多少。
5. Response 是否符合预期字段。
6. Timing 是否超时或长时间 Pending。

判断问题所在层：

```text
Network 中没有请求
→ Event / Effect / Service 调用之前的问题

URL 或 Method 错误
→ Service 配置问题

请求正确但返回 4xx/5xx
→ 根据 API 规格调查输入、认证、权限或后端

响应正确但画面错误
→ State / Props / 条件渲染问题
```

调查 API 问题时，Network 是前端工程师的基础工具，不要只看 Console 猜测。

## 10. CORS 与 Vite Proxy 的基础理解

浏览器页面与 API 的协议、主机或端口不同，就可能受到同源策略限制。例如页面在 `localhost:5173`，API 在 `localhost:8080`。

CORS 是否允许跨源访问由后端响应头决定。前端不能通过添加任意请求头“关闭 CORS”。开发环境也可使用 Vite Proxy 把 `/api` 转发到后端，但生产环境仍需正确设计域名、反向代理和 CORS。

看到 CORS 错误时先确认：

- 前端实际 Origin。
- API 地址和端口。
- 后端允许的 Origin。
- 是否携带 Cookie，以及服务端凭据配置。
- 请求是否先被其他错误导致预检失败。

## 11. 外部数据需要运行时确认

JavaScript 不会自动确认服务器字段。最基础的检查可以确认列表确实是数组：

```jsx
if (!Array.isArray(data)) {
  throw new Error('员工列表响应格式不正确');
}
```

高风险数据可以使用 schema 库或项目共通校验函数检查字段。不是每个普通请求都必须手写大量 Validator，应根据接口风险和团队方案决定。

## 12. 常见错误与练习

- URL 重复 `/api/api`：检查 `baseURL` 与 Service 路径如何组合。
- 页面一直 loading：检查 `finally` 是否恢复状态，以及请求是否 Pending。
- 取消请求后显示错误：先排除 Axios cancel。
- 响应有数据但页面为空：检查读取的是 `response.data` 还是完整 response。
- 环境变量为 `undefined`：确认 `VITE_` 前缀并重启开发服务器。
- CORS 报错：检查后端配置和实际 Origin，不要在前端伪造响应头。

练习：

1. 创建 Axios 实例和 `getEmployees` Service。
2. 分别模拟慢请求、空数组、500、timeout 和成功响应。
3. 验证 Loading、Error、Empty、Success 与 Retry。
4. 离开页面或重复读取，确认旧请求被取消。
5. 在 Network 记录 URL、Method、Status、Query、Response 和 Timing。
6. 故意写错 `baseURL`，根据 Network 证据定位并恢复。

## 本章检查点

- [ ] 能画出 Component → Service → HTTP → Backend → State → UI。
- [ ] 能识别请求和响应的主要组成部分。
- [ ] 能建立 Axios 共用实例，并把业务请求放入 Service。
- [ ] 能完整处理 loading、error、empty、success 和 retry。
- [ ] 能取消旧请求并区分取消与真实失败。
- [ ] 能用 Network 定位请求前、HTTP 和画面层问题。
- [ ] 能说明 CORS、环境变量和运行时数据检查的边界。

参考：[Axios 官方文档](https://axios-http.com/docs/intro)、[Vite：环境变量与模式](https://vite.dev/guide/env-and-mode)、[MDN：HTTP 概览](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Guides/Overview)。
