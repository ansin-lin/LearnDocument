# 第 10 章 REST API 与异步 UI

## 本章目标

- 沿着 Component → Service → HTTP → Backend 读取数据。
- 同时设计 loading、error、empty 和 success。
- 处理状态码、超时/取消与外部数据校验边界。

## 1. 请求链路

```text
EmployeeListPage
      ↓ 调用
employeeService
      ↓ HTTP/JSON
Backend API
      ↓ 权限、业务、数据库
Response
```

React 不负责 HTTP。课程主线使用 Axios，并把请求集中在 Service。安装：

```bash
npm install --save-exact axios@1.20.0
```

```tsx
// src/services/httpClient.ts
import axios from 'axios';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});
```

前端环境变量会进入浏览器包，不能存真实密钥。

## 2. Service 与契约

```tsx
// src/services/employeeService.ts
import { httpClient } from './httpClient';
import type { Employee } from '../types/employee';

export async function getEmployees(signal?: AbortSignal): Promise<Employee[]> {
  const response = await httpClient.get<Employee[]>('/employees', { signal });
  return response.data;
}
```

泛型只约束开发时的使用方式，不会验证服务器 JSON。高风险外部数据应通过 schema 或明确的运行时校验再进入业务层。

## 3. 四种 UI 状态

```tsx
const [employees, setEmployees] = useState<Employee[]>([]);
const [loading, setLoading] = useState(true);
const [error, setError] = useState<string | null>(null);

useEffect(() => {
  const controller = new AbortController();
  setLoading(true);
  setError(null);

  getEmployees(controller.signal)
    .then(setEmployees)
    .catch((cause: unknown) => {
      if (!axios.isCancel(cause)) setError('员工列表读取失败');
    })
    .finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });

  return () => controller.abort();
}, []);
```

渲染顺序：loading → error → empty → success。失败时保留“重试”入口；空数据不是错误。真实项目也可使用专门的服务器状态库处理缓存与失效，但先理解基本异步 UI。

## 4. HTTP 责任边界

- 400：请求内容不符合接口，可显示字段/业务错误。
- 401：未认证或会话失效，进入统一登录恢复流程。
- 403：已识别用户但无权操作；不能改成“重新登录就一定解决”。
- 404：资源不存在，显示返回列表或刷新入口。
- 409：当前数据与服务器版本冲突，提示重新读取后再决定是否修改。
- 500：服务端失败，记录可追踪信息并给用户安全提示。
- Network/timeout：服务不可达或超时，不应伪装成 500。

不要把后端堆栈、SQL 或敏感响应原样显示给用户。

### 4.1 用 Network 验证请求

打开 Chrome/Edge DevTools 的 Network，选择对应请求并依次检查：

```text
URL → Method → Status → Request Query/Body → Response → Timing
```

如果没有请求，问题发生在 Event、Effect 或 Service 调用之前；如果 URL/Method 错误，先修 Service；如果响应正确但画面错误，再检查 State、Props 和 JSX。不要只看 Console 猜测后端结果。保存证据时遮盖 Cookie、Authorization 和个人信息。

## 5. 练习

1. 创建 Axios 实例与 `getEmployees` Service。
2. 分别模拟慢请求、空数组、500 和成功，验证四种 UI。
3. 快速离开页面，确认请求被取消且无过期状态更新。
4. 在 Network 中记录 URL、方法、状态码、请求与响应。

## 本章检查点

- [ ] Service 接收 AbortSignal，并设置合理 timeout。
- [ ] 页面能区分 loading、error、empty 与 success。
- [ ] 能用 Network 的 URL、Method、Status、Request、Response 和 Timing 定位请求问题。
