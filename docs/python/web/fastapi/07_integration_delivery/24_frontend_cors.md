# 第24章 前后端联调与CORS

> 本章目标：理解前后端分离中的接口联调流程，掌握 CORS 的作用、配置方式和常见错误定位。

本章联调第20章完成并经第21至23章自动测试保护的员工API。浏览器前端通过JSON请求使用登录、员工查询和写入接口。

## 一、前后端联调是什么

前后端分离项目中：

```text
Vue / React 页面
-> 发送 HTTP 请求
-> FastAPI 接口
-> 数据库
-> 返回 JSON
-> 前端渲染页面
```

联调要确认：

- URL 是否正确
- HTTP 方法是否正确
- 请求参数是否正确
- Header 是否正确
- 响应结构是否符合约定
- 错误状态码是否能被前端处理

## 二、CORS 是什么

CORS 是浏览器的跨域访问控制机制。

例如：

| 前端地址 | 后端地址 | 是否跨域 |
| --- | --- | --- |
| `http://localhost:5173` | `http://localhost:8000` | 是 |
| `http://localhost:8000` | `http://localhost:8000` | 否 |

CORS 不是认证，也不是授权。它只决定浏览器是否允许前端 JavaScript 读取跨域响应。

## 三、配置 CORS

文件：`app/main.py`  
操作：追加导入和Middleware配置  
代码类型：项目代码片段

```python
from fastapi.middleware.cors import CORSMiddleware  # 导入跨域处理中间件

app.add_middleware(  # 为应用注册CORS中间件
    CORSMiddleware,  # 指定中间件类型
    allow_origins=["http://localhost:5173"],  # 只允许本地前端来源
    allow_credentials=True,  # 允许认证请求头等凭据
    allow_methods=["*"],  # 允许项目接口使用的全部HTTP方法
    allow_headers=["*"],  # 允许前端发送Authorization等请求头
    expose_headers=["X-Request-ID"],  # 允许前端读取请求编号响应头
)  # 完成中间件配置
```

把导入放到现有`app/main.py`的导入区域，把`app.add_middleware(...)`放在第16章创建的唯一`app`对象之后。不要再次执行`app = FastAPI()`，否则此前注册的Lifespan、Router、异常处理器和健康检查都会丢失。

`app.add_middleware()` 的第一个参数接收 Middleware 类，后面的关键字参数会传给该 Middleware。`CORSMiddleware` 本示例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `allow_origins` | Origin 字符串列表，例如 `["http://localhost:5173"]`；也可使用 `["*"]` | 默认 `[]` | 指定允许跨域访问的前端来源 |
| `allow_credentials` | `True` 或 `False` | 默认 `False` | 是否允许浏览器在跨域请求中携带 Cookie、Authorization 等凭据 |
| `allow_methods` | HTTP 方法字符串列表或 `["*"]` | 默认 `["GET"]` | 指定跨域请求允许使用的方法 |
| `allow_headers` | 请求头名称列表或 `["*"]` | 默认 `[]` | 指定跨域请求允许携带的请求头 |
| `expose_headers` | 响应头名称列表 | 默认 `[]` | 指定允许前端 JavaScript 读取的自定义响应头 |
| `max_age` | 非负整数秒数 | 默认 `600` | 设置浏览器缓存预检响应的时间 |

如果前端需要读取第17章添加的`X-Request-ID`，还要设置`expose_headers=["X-Request-ID"]`。

生产环境不要随意使用 `allow_origins=["*"]` 配合认证信息。

## 四、前端请求示例

文件：前端项目中的`src/api/employees.js`  
操作：新建  
代码类型：前端项目代码片段（Fetch版本）

```javascript
async function loadEmployees(token) { // 定义带Token的员工列表请求函数
  const controller = new AbortController(); // 创建可取消请求的控制器
  const timeoutId = setTimeout(() => controller.abort(), 5000); // 5秒后取消未完成请求

  try {
    const response = await fetch(
      "http://127.0.0.1:8000/api/employees",
      {
        headers: { Authorization: `Bearer ${token}` },
        signal: controller.signal,
      },
    );
    if (!response.ok) {
      const requestId = response.headers.get("X-Request-ID");
      throw new Error(`HTTP ${response.status}, request_id=${requestId}`);
    }
    return await response.json();
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error("请求超时");
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

const employees = await loadEmployees("<登录接口返回的临时访问令牌>");
console.log(employees);
```

`fetch()` 接收请求 URL 和可选配置对象。`AbortController` 在 5 秒后取消仍未完成的请求；`try...catch...finally` 分别处理成功、失败和定时器清理。非成功响应不会让 `fetch()` 自动抛出异常，因此必须检查 `response.ok`。还可以根据接口需要设置：

| 配置项 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `method` | `"GET"`、`"POST"`、`"PUT"`、`"PATCH"`、`"DELETE"` 等 | 默认 `"GET"` | 设置 HTTP 请求方法 |
| `headers` | `Headers` 对象、键值对象或请求头数组 | 默认不添加自定义请求头 | 设置 Authorization、Content-Type 等请求头 |
| `body` | 字符串、`FormData`、`Blob` 等 Body 数据 | 默认无请求体 | 提交 JSON、表单或文件；GET 请求不设置 |
| `credentials` | `"omit"`、`"same-origin"`、`"include"` | 默认 `"same-origin"` | 决定请求是否携带 Cookie 等浏览器凭据 |
| `signal` | `AbortSignal` | 默认不设置 | 支持取消请求或实现超时控制 |

文件：前端项目中的`src/api/employees.js`  
操作：选择Axios时用本段替换Fetch版本  
代码类型：前端项目代码片段（Axios版本）

```javascript
import axios from "axios"; // 导入Axios客户端

async function loadEmployeesWithAxios(token) { // 定义Axios员工列表请求函数
  try {
    const response = await axios.get(
      "http://127.0.0.1:8000/api/employees",
      {
        headers: { Authorization: `Bearer ${token}` },
        timeout: 5000,
      },
    );
    return response.data;
  } catch (error) {
    const status = error.response?.status;
    const requestId = error.response?.headers?.["x-request-id"];
    throw new Error(
      status
        ? `HTTP ${status}, request_id=${requestId}`
        : "网络错误或请求超时",
    );
  }
}
```

`axios.get(url, config)` 的 `url` 必填，`config` 可省略。Axios 会把非成功状态码作为异常交给 `catch`，网络错误和超时则没有 `error.response`。示例中的配置项：

| 配置项 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `headers` | 请求头键值对象 | 默认不添加自定义请求头 | 设置 Bearer Token 等请求头 |
| `params` | 查询参数对象或 `URLSearchParams` | 默认无查询参数 | 把分页、筛选等数据放到 URL 查询字符串 |
| `timeout` | 非负毫秒数 | 默认 `0`，表示不主动超时 | 设置 Axios 等待响应的最长时间 |
| `signal` | `AbortSignal` | 默认不设置 | 取消不再需要的请求 |

## 五、携带 Token

文件：前端项目中的`src/api/employees.js`  
操作：只用于观察Token请求头，不与上面两个完整函数重复保留  
代码类型：语法片段

```javascript
const token = localStorage.getItem("access_token"); // 从 localStorage 读取 Token

const response = await fetch("http://127.0.0.1:8000/api/employees", {
  headers: { // 设置请求头
    Authorization: `Bearer ${token}`, // 携带 Bearer Token
  }, // 请求头结束
}); // 请求结束
```

注意：前端保存 Token 不是绝对安全，真实项目需要结合安全策略。

## 六、常见联调问题

| 现象 | 常见原因 | 处理方式 |
| --- | --- | --- |
| 浏览器提示 CORS | 后端没有允许前端域名 | 检查 `allow_origins` |
| 404 | URL 路径错误 | 对照 `/docs` |
| 405 | HTTP 方法错误 | 检查 GET/POST/PUT/DELETE |
| 422 | 参数校验失败 | 查看响应 detail |
| 401 | 未认证 | 检查 Token |
| 403 | 无权限 | 检查角色或权限 |
| 500 | 后端异常 | 查看后端日志 |

## 七、基础练习

请完成：

1. 配置 CORS 允许 `http://localhost:5173`
2. 用浏览器访问 `/docs`
3. 通过 `/api/auth/token` 登录并用 Fetch 调用员工列表接口
4. 故意传错参数，观察 422
5. 故意不传 Token，观察 401
6. 使用普通角色调用管理员接口，观察 403，并确认 CORS 设置不会改变权限结果

## 八、本章总结

- 前后端联调要检查 URL、方法、参数、Header 和响应
- CORS 是浏览器跨域机制，不是权限控制
- FastAPI 使用 `CORSMiddleware` 配置 CORS
- 认证接口需要前端携带 `Authorization: Bearer <token>`
