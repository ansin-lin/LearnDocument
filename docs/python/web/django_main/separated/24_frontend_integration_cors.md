# 第24章 前端联调、Fetch 与 CORS

## 本章成果

创建一个可以独立启动的原生 HTML/JavaScript 前端，完成登录、access token 自动刷新、员工分页列表、退出和错误状态显示。能够在浏览器 Network 中区分前端错误、CORS、认证、权限、输入和后端异常。

## 本章开始状态与目录

- 第23章的员工列表已经具有稳定的分页结构和 OpenAPI。
- 后端运行在 `http://127.0.0.1:8000`。
- 本章在项目根目录新建 `frontend/`，不修改 Django Template。

```text
frontend/
├── index.html
├── api.js
└── app.js
```

这里使用浏览器原生 ES Module 和 Python 静态服务器，不引入 Node.js 或前端框架。生产项目中的环境配置和构建方式应遵循前端工程规范。

## 本章在整体架构中的位置

```text
Browser → Fetch → CORS / Preflight → JWT → Django API → JSON
             ↑             ↑ 本章重点                 ↓
             └──────────── UI 状态与错误提示 ←────────┘
```

完成后，API 将由真实浏览器前端调用，可以从 Network 面板观察完整请求、认证、跨域和响应流程。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| Fetch | 浏览器发起 HTTP 请求的标准接口 | 让前端以代码调用 REST API | 页面需要登录、查询或提交数据时 |
| CORS | 浏览器对跨来源请求实施的访问控制协议 | 让后端明确哪些前端来源可读取响应 | 前端与 API 的 scheme、host 或 port 不同时 |
| Preflight | 浏览器用 `OPTIONS` 预先确认跨域规则的请求 | 在真正请求前验证方法和请求头是否允许 | 带认证头或非简单方法的跨域调用前 |

## 1. 先固定联调契约

| 项目 | 本章约定 |
|---|---|
| 前端地址 | `http://localhost:5173` |
| 后端地址 | `http://127.0.0.1:8000` |
| API 基础地址 | `http://127.0.0.1:8000/api` |
| 登录 | `POST /api/auth/token/` |
| 刷新 | `POST /api/auth/token/refresh/` |
| 员工列表 | `GET /api/employees/?page=1` |
| 认证头 | `Authorization: Bearer <access>` |
| 列表结构 | `count`、`next`、`previous`、`results` |

协议、域名和端口任一不同就是不同 origin。浏览器同源策略限制页面读取跨源响应；CORS 是后端声明允许哪些 origin 的机制，不是 Django 权限系统。

先把浏览器中的处理过程分成四个部分：

```text
页面事件
→ Fetch 构造 HTTP 请求
→ 浏览器检查同源策略，必要时发送 Preflight
→ Django 认证、授权并处理业务
→ Fetch 读取 Response
→ 页面显示加载、成功、空数据或失败状态
```

- **Fetch 是什么**：浏览器提供的异步 HTTP 客户端接口，返回 Promise。Promise 是表示“异步操作以后成功或失败”的对象；成功状态称为 fulfilled，可以取得结果，失败状态称为 rejected，会进入错误处理。
- **为什么需要封装**：基础地址、认证头、超时、JSON 解析和错误转换会被多个页面重复使用。
- **什么时候使用**：页面需要登录、读取列表、提交数据或下载文件时。

`fetch()` 只在网络层失败时拒绝 Promise；收到401、403或500时仍然会得到 `Response`，因此代码必须检查 `response.ok` 或 `response.status`。页面也不能只处理“有数据”一种情况，至少要区分加载中、空结果、成功和失败。

当请求跨 origin 且包含 `Authorization` 等非简单请求头时，浏览器通常先发送 `OPTIONS` Preflight。预检通过只表示浏览器允许继续发送请求，不表示 JWT 正确，也不表示当前用户拥有员工权限。

## 2. 配置允许的开发来源

在项目根目录激活虚拟环境后安装并记录直接依赖：

```powershell
python -m pip install "django-cors-headers==4.9.0"
```

在项目根目录的 `requirements.txt` 中追加：

```text
django-cors-headers==4.9.0
```

在 `company_portal/settings.py` 中确认；已有列表应合并条目，不要复制出第二个 `INSTALLED_APPS` 或 `MIDDLEWARE`：

```python
INSTALLED_APPS = [
    "corsheaders",
    # 既有 App
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    # 既有中间件
]

CORS_ALLOWED_ORIGINS = [
    "http://localhost:5173",
]
```

生产来源通过环境配置并使用实际 HTTPS 域名。不要把 `CORS_ALLOW_ALL_ORIGINS=True` 当作修复方案后直接上线。

`django-cors-headers` 是第三方 CORS 响应头中间件；安装包名是 `django-cors-headers`，Python App 名是 `corsheaders`。`CorsMiddleware` 必须位于可能直接生成响应的 `CommonMiddleware` 之前。`CORS_ALLOWED_ORIGINS` 接受完整 origin 字符串列表，每项包含协议、主机和可选端口，不包含路径；本例只允许 `http://localhost:5173`。

## 3. 创建页面入口

创建 `frontend/index.html`：

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>员工 API 联调</title>
  </head>
  <body>
    <main>
      <h1>员工 API 联调</h1>

      <form id="login-form">
        <label>
          用户名
          <input id="username" name="username" required>
        </label>
        <label>
          密码
          <input id="password" name="password" type="password" required>
        </label>
        <button type="submit">登录</button>
      </form>

      <button id="logout-button" type="button">退出</button>
      <p id="status" role="status"></p>
      <ul id="employee-list"></ul>

      <nav aria-label="员工分页">
        <button id="previous-button" type="button">上一页</button>
        <span id="page-label"></span>
        <button id="next-button" type="button">下一页</button>
      </nav>
    </main>

    <script type="module" src="./app.js"></script>
  </body>
</html>
```

页面只包含本章需要的登录、状态、列表和分页。样式不是本章成果。

`<script type="module" src="./app.js">` 让浏览器按 ES 模块加载同目录脚本；`type="module"` 是必需属性，并要求通过 HTTP 服务器打开页面，不能依赖双击本地文件。表单控件的 `id` 供脚本定位，`name` 决定原生表单字段名称，`required` 只提供浏览器基础校验，不能替代后端验证。

## 4. 编写完整 Fetch 封装

### 4.1 读懂本章使用的 JavaScript 语法

本章只使用浏览器原生 JavaScript。下面这些写法负责组织请求代码，不需要安装额外工具：

- `const` 声明不能再次赋值的变量，`let` 声明之后还会变化的变量；对象本身的内容仍可能被修改。
- `function name(parameters) { ... }` 定义函数；`(parameters) => expression` 是简短的箭头函数，本章用它把“超时后取消请求”交给定时器执行。
- `{username, password}` 是对象简写，等价于 `{username: username, password: password}`；`{...options, signal: controller.signal}` 先复制 `options` 的属性，再设置或覆盖 `signal`。
- `` `${API_BASE_URL}/auth/token/` `` 是模板字符串，可以把 `${...}` 中的值插入文本；`value ?? ""` 只在左侧为 `null` 或 `undefined` 时采用右侧默认值。
- `===` 做不自动转换类型的严格相等比较，`!` 表示逻辑非，`&&` 表示条件同时成立；`condition ? value1 : value2` 根据条件返回两个值之一，`value instanceof Type` 判断对象是否由指定类型创建。
- `function loadEmployees(page = 1)` 中的 `= 1` 是默认参数，调用时省略或传入 `undefined` 才采用默认值；`for (const employee of employees)` 依次读取可迭代对象中的每一项。
- `class ApiError extends Error` 创建自定义错误类；`constructor(status, body)` 在 `new ApiError(...)` 时接收参数，`super(message)` 先初始化父类 `Error`。构造器返回新实例，不需要显式 `return`。
- `export` 公开当前模块中的类或函数，`import { name } from "./api.js"` 在另一个模块按名称导入。相对路径以当前模块文件为起点，并且必须包含浏览器能够请求到的文件路径。
- `async function` 调用后总是返回 Promise；`await` 等待 Promise 完成并取得结果。Promise 失败时会抛出异常，由 `try` / `catch` 处理；`finally` 无论成功或失败都会执行，适合清理定时器。

### 4.2 创建请求模块

创建 `frontend/api.js`：

```javascript
const API_BASE_URL = "http://127.0.0.1:8000/api";
const ACCESS_KEY = "employee_api_access";
const REFRESH_KEY = "employee_api_refresh";

export class ApiError extends Error {
  constructor(status, body) {
    super(`API request failed: ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

function saveTokens(tokens) {
  sessionStorage.setItem(ACCESS_KEY, tokens.access);
  sessionStorage.setItem(REFRESH_KEY, tokens.refresh);
}

export function clearTokens() {
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
}

async function readBody(response) {
  if (response.status === 204) return null;
  const contentType = response.headers.get("content-type") ?? "";
  return contentType.includes("application/json")
    ? response.json()
    : response.text();
}

async function fetchWithTimeout(url, options) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 10000);
  try {
    return await fetch(url, {...options, signal: controller.signal});
  } catch (error) {
    if (error.name === "AbortError") {
      throw new ApiError(0, {detail: "请求超时"});
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

export async function login(username, password) {
  const response = await fetchWithTimeout(
    `${API_BASE_URL}/auth/token/`,
    {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({username, password}),
    },
  );
  const body = await readBody(response);
  if (!response.ok) throw new ApiError(response.status, body);
  saveTokens(body);
}

async function refreshAccessToken() {
  const refresh = sessionStorage.getItem(REFRESH_KEY);
  if (!refresh) return false;

  const response = await fetchWithTimeout(
    `${API_BASE_URL}/auth/token/refresh/`,
    {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({refresh}),
    },
  );
  const body = await readBody(response);
  if (!response.ok) {
    clearTokens();
    return false;
  }
  sessionStorage.setItem(ACCESS_KEY, body.access);
  return true;
}

export async function apiFetch(path, options = {}, retry = true) {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  if (
    options.body
    && !(options.body instanceof FormData)
    && !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  const access = sessionStorage.getItem(ACCESS_KEY);
  if (access) headers.set("Authorization", `Bearer ${access}`);

  const response = await fetchWithTimeout(
    `${API_BASE_URL}${path}`,
    {...options, headers},
  );

  if (response.status === 401 && retry && await refreshAccessToken()) {
    return apiFetch(path, options, false);
  }

  const body = await readBody(response);
  if (!response.ok) throw new ApiError(response.status, body);
  return body;
}
```

`fetch()` 遇到404或500不会自动 reject，所以必须检查 `response.ok`。这里只对一次401执行一次refresh，避免无限重试。示例使用 `sessionStorage` 便于本地观察请求，不代表所有生产系统都应采用相同 token 存储策略；实际方案必须结合 XSS、CSRF、CSP 和整体架构评审。

首次使用的浏览器接口说明：

- `fetch(url, options)` 的 `url` 必填，`options` 可省略；返回 `Promise<Response>`，网络失败才 reject，HTTP 4xx/5xx 仍需检查 `response.ok`。
- `AbortController()` 创建取消控制器；把 `controller.signal` 传给 `fetch`，调用 `controller.abort()` 后请求以 `AbortError` 结束。本例超时固定为10000毫秒。
- `window.setTimeout(handler, delay)` 在至少等待 `delay` 毫秒后安排一次函数调用，并返回定时器 ID；本例把箭头函数作为 `handler`，把 `10000` 作为超时毫秒数。`window.clearTimeout(id)` 取消对应定时器，没有需要使用的返回结果；把它放在 `finally` 中可避免请求已经结束后仍触发取消。
- `sessionStorage.getItem(key)` 返回字符串或 `null`，`setItem(key, value)` 写入当前标签页会话，`removeItem(key)` 删除。这里仅保存本地联调用 token，不保存密码。
- `Headers(initial)` 创建请求头对象，`initial` 可省略；`set(name, value)` 覆盖字段，`has(name)` 返回布尔值。`apiFetch(path, options = {}, retry = true)` 返回解析后的响应体；`retry` 只允许一次401刷新，调用方通常不需要传入。
- `response.headers.get(name)` 返回指定响应头的字符串值或 `null`；`Response.json()` 和 `Response.text()` 都返回 Promise，分别解析 JSON 或读取文本；`JSON.stringify(value)` 返回 JSON 字符串。
- `FormData` 是浏览器表示表单及文件字段的对象类型。这里用 `options.body instanceof FormData` 判断请求体是否为该类型；如果是，就让浏览器自动生成带 boundary 的 `Content-Type`，不能强制改成 JSON。
- `ApiError(status, body)` 继承内置 `Error`，两个参数分别保存 HTTP 状态和已解析错误体，返回的错误实例供界面区分401、403和其他失败。

### 阶段检查：先确认请求封装的职责

进入页面逻辑前，先确认能够说明以下四点：

- `login()` 只负责获取并保存 token，不负责渲染员工列表。
- `apiFetch()` 统一添加认证头、处理一次 refresh、解析响应和抛出 `ApiError`。
- `fetchWithTimeout()` 只负责网络请求与10秒超时，不判断业务权限。
- 页面代码负责把加载、空数据、成功和失败转换为用户可见状态。

如果这四项仍混在一起理解，后面的页面事件会很难排查；先沿 `login() → apiFetch() → fetchWithTimeout() → Response` 重新阅读一次。

## 5. 实现登录、列表和四类状态

页面脚本会直接操作 DOM。`document.querySelector(selector)` 接收 CSS 选择器并返回第一个匹配元素，没有匹配时返回 `null`；本例使用固定 ID，因此这些元素必须先存在于 `index.html`。常用接口如下：

- `element.textContent = value` 把值作为纯文本显示，不会把 API 返回内容解析成 HTML；`input.value` 读取输入框当前字符串。
- `element.replaceChildren(...nodes)` 用给定节点替换全部子节点；不传参数时清空列表，返回值为 `undefined`。
- `document.createElement(tagName)` 按标签名创建新元素并返回该元素；`element.append(...nodes)` 把一个或多个节点或文本追加为子内容。
- `button.disabled = boolean` 使用布尔值控制按钮是否可操作；`form.reset()` 把表单控件恢复到初始值，没有需要使用的返回结果。
- `element.addEventListener(type, handler)` 为事件类型注册处理函数；处理函数接收事件对象。`event.preventDefault()` 阻止表单提交等浏览器默认行为，不会停止后端请求，因为请求由脚本另外发起。

创建 `frontend/app.js`：

```javascript
import {
  ApiError,
  apiFetch,
  clearTokens,
  login,
} from "./api.js";

const loginForm = document.querySelector("#login-form");
const statusElement = document.querySelector("#status");
const listElement = document.querySelector("#employee-list");
const pageLabel = document.querySelector("#page-label");
const previousButton = document.querySelector("#previous-button");
const nextButton = document.querySelector("#next-button");
const logoutButton = document.querySelector("#logout-button");

let currentPage = 1;

function showStatus(message) {
  statusElement.textContent = message;
}

function renderEmployees(employees) {
  listElement.replaceChildren();
  for (const employee of employees) {
    const item = document.createElement("li");
    item.textContent =
      `${employee.employee_number} ${employee.name}`
      + ` / ${employee.department_detail.name}`;
    listElement.append(item);
  }
}

async function loadEmployees(page = 1) {
  showStatus("读取中……");
  listElement.replaceChildren();

  try {
    const data = await apiFetch(`/employees/?page=${page}`);
    currentPage = page;
    pageLabel.textContent = `第 ${page} 页 / 共 ${data.count} 件`;
    previousButton.disabled = !data.previous;
    nextButton.disabled = !data.next;

    if (data.results.length === 0) {
      showStatus("没有符合条件的员工");
      return;
    }

    renderEmployees(data.results);
    showStatus("读取成功");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      showStatus("登录已失效，请重新登录");
    } else if (error instanceof ApiError && error.status === 403) {
      showStatus("当前账号没有查看权限");
    } else {
      showStatus("员工列表读取失败，请检查Network中的状态码和响应体");
    }
  }
}

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await login(
      document.querySelector("#username").value,
      document.querySelector("#password").value,
    );
    loginForm.reset();
    await loadEmployees(1);
  } catch {
    showStatus("登录失败，请确认账号、密码和用户状态");
  }
});

previousButton.addEventListener("click", () => {
  if (currentPage > 1) loadEmployees(currentPage - 1);
});

nextButton.addEventListener("click", () => {
  loadEmployees(currentPage + 1);
});

logoutButton.addEventListener("click", () => {
  clearTokens();
  listElement.replaceChildren();
  showStatus("已清除本地token");
});
```

使用 `textContent` 写入API字符串，避免把返回数据直接拼进 `innerHTML`。`loadEmployees(page = 1)` 的 `page` 默认是1，函数返回 Promise，并根据 `count`、`next`、`previous`、`results` 更新加载、空数据、成功和失败状态。

## 6. 启动并验证

终端一启动Django：

```powershell
python manage.py runserver
```

终端二在项目根目录启动前端：

```powershell
python -m http.server 5173 --directory frontend
```

`python -m http.server 5173 --directory frontend` 使用 Python 标准库在5173端口提供 `frontend/`；它只用于本地联调，不是生产 Web 服务器。两个终端都应保持运行，完成练习后按 `Ctrl+C` 停止。

访问 `http://localhost:5173/`，按顺序验证：

1. 正确账号登录后显示授权部门的员工。
2. `api-viewer` 不能读取其他部门员工，也不能执行写操作。
3. access过期后refresh成功并重新发出一次列表请求。
4. refresh失效或用户停用后显示重新登录。
5. 空数据、403和后端500分别显示不同信息。
6. Network中的请求地址、状态码、Authorization和响应结构符合契约；截图或日志不得暴露完整token。

停止前端服务器不会修改数据库，无需清理业务数据。

## 7. 预检、CSRF与调查顺序

跨源且带Authorization头时，浏览器通常先发送OPTIONS预检。预检失败时业务View可能根本没有执行。Network中先检查：

1. Console是否为JavaScript或DOM错误。
2. 是否发出实际请求，URL、方法、头和体是否符合契约。
3. 是否先出现失败的OPTIONS。
4. origin是否精确匹配，中间件顺序是否正确。
5. 响应是401、403、404、400还是500。
6. 同一请求能否使用HTTP客户端复现。
7. 记录请求时间、方法、URL、状态码和响应体；第27章加入请求ID后再重复关联日志调查。

CORS控制页面能否读取跨源响应；CSRF防止浏览器自动携带Cookie凭据进行跨站写操作。JWT放在Authorization头时通常不使用Django Session的CSRF流程，但仍要防止XSS和token泄露。若项目使用Cookie保存认证凭据，必须另外设计CSRF token、SameSite和可信来源。

## 日本企业项目中的实际使用

前后端联调通常以 OpenAPI、环境 URL、测试账号和错误证据为共同基线。CORS 是浏览器跨域读取响应的规则，不是登录认证，也不能代替后端权限。

## 新人常见错误

- 忘记 `await response.json()`，把 Promise 当成响应数据。
- 只看 Console，不在 Network 中确认请求与响应。
- 401后无限刷新 token，形成重试循环。
- 为解决 CORS 临时允许所有来源并带凭据。
- 把前端显示“无权限”当成后端权限已经正确。

## 企业项目调查路径

```text
UI 操作 → Network → Preflight → Request headers / body
→ Status / Response → Django Log → Database
```

先确认浏览器是否真正发出了业务请求；只有 preflight 失败时优先检查 origin、方法和请求头配置，业务响应失败再进入认证、权限和后端日志。

## 现场任务

故障：access过期后页面不断重复请求员工列表。使用Network确认401和refresh请求次数，修复为“最多refresh一次；失败后清除token并要求重新登录”，补充成功刷新和refresh失效两条证据。

## 完成检查

- [ ] `frontend/` 可以独立启动并显示员工列表。
- [ ] 登录、一次refresh、退出和分页形成可观察闭环。
- [ ] 加载、空数据、成功、401、403和其他失败状态已区分。
- [ ] 能解释origin、预检、CORS和CSRF的不同职责。
- [ ] token不进入URL、日志、仓库或公开截图。

下一章在同一个前端和授权QuerySet上加入文件上传、附件列表与安全下载。
