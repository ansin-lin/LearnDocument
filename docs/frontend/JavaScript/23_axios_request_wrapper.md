# 第 23 章 Axios 请求与接口层封装

## 本章目标

学完本章后，你应当能够：

- 说明 Axios 与 Fetch 的关系和选择原则；
- 使用 Axios 发送常见 HTTP 请求；
- 正确配置 URL、查询参数、请求体、请求头、超时和取消信号；
- 区分服务器错误、网络错误和请求配置错误；
- 创建统一的 Axios 实例；
- 理解请求拦截器和响应拦截器；
- 把请求代码整理到独立的接口层；
- 使用浏览器 Network 面板验证请求和响应。

> 第 19 章已经学习 HTTP、Fetch、状态码和异步请求流程。本章不重新讲一遍 HTTP，而是学习项目中如何使用 Axios 统一管理请求。

---

### 1. Axios 是什么

Axios 是一个基于 Promise 的 HTTP 客户端。它可以在浏览器和 Node.js 环境中发送请求。

```text
页面代码
   ↓ 调用业务接口函数
Axios 请求模块
   ↓ 发送 HTTP 请求
后端 API
   ↓ 返回响应
Axios 请求模块
   ↓ 返回业务数据
页面更新
```

Axios 不是浏览器内置功能，需要通过 CDN 或 npm 引入。

#### 1.1 Axios 与 Fetch

| 比较项 | Fetch | Axios |
| --- | --- | --- |
| 来源 | 浏览器内置 | 第三方库 |
| 是否安装 | 不需要 | 需要 |
| JSON 响应 | 手动调用 `response.json()` | 默认解析到 `response.data` |
| 4xx / 5xx | 默认不会自动抛错 | 默认会拒绝 Promise |
| 超时 | 配合 AbortController | 可直接设置 `timeout` |
| 请求/响应统一处理 | 自行封装 | 提供实例和拦截器 |
| 适合场景 | 原生 API、简单请求 | 需要统一配置的项目 |

选择原则：

- 项目已经统一使用 Fetch，就继续保持一致；
- 项目已经引入 Axios，或需要统一接口层、超时和拦截器，可以使用 Axios；
- 不要只为了发送一次简单请求同时引入两套方案。

---

### 2. 引入 Axios

本章介绍两种方式，但项目主线使用 npm。

#### 2.1 CDN：只用于快速体验

```html
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>Axios 快速体验</title>
</head>
<body>
  <button id="load-button" type="button">读取用户</button>
  <pre id="result"></pre>

  <script src="https://cdn.jsdelivr.net/npm/axios@1/dist/axios.min.js"></script>
  <script>
    const loadButton = document.querySelector("#load-button");
    const result = document.querySelector("#result");

    loadButton.addEventListener("click", async () => {
      const response = await axios.get(
        "https://jsonplaceholder.typicode.com/users/1"
      );

      result.textContent = JSON.stringify(
        response.data,
        null,
        2
      );
    });
  </script>
</body>
</html>
```

CDN 方式会提供全局变量 `axios`，适合单页验证。正式项目应固定版本并遵守团队的依赖管理方式。

#### 2.2 npm：项目主线

在已有前端工程中安装 Axios 1.x：

```bash
npm install axios
```

然后在模块中导入：

```js
import axios from "axios";
```

这里的 `"axios"` 是裸模块名称，由 Vite 等构建工具从项目依赖中解析。它不能直接用于没有构建工具的普通浏览器模块页面。

---

## 第一部分：发送基本请求

### 3. 发送 GET 请求

`axios.get(url, config?)` 用于读取资源：

- `url`：请求地址；
- `config`：可选配置对象；
- 返回值：Promise，成功结果是 Axios 响应对象。

```js
import axios from "axios";

async function loadUser() {
  const response = await axios.get(
    "https://jsonplaceholder.typicode.com/users/1"
  );

  console.log(response.data);
}

loadUser();
```

Axios 会根据响应类型处理数据。常见 JSON 响应可以直接从 `response.data` 读取，不需要再调用 `response.json()`。

#### 3.1 响应对象

| 属性 | 作用 |
| --- | --- |
| `response.data` | 后端返回的响应体 |
| `response.status` | HTTP 状态码 |
| `response.statusText` | HTTP 状态说明；在部分协议中可能为空 |
| `response.headers` | 响应头 |
| `response.config` | 本次 Axios 请求配置 |
| `response.request` | 底层请求对象，通常用于排查 |

```js
const response = await axios.get(
  "https://jsonplaceholder.typicode.com/users/1"
);

console.log(response.status);
console.log(response.headers);
console.log(response.data);
```

业务代码通常最关心 `data` 和 `status`。

---

### 4. 使用 params 发送查询参数

GET 请求的筛选条件通常放在 URL 查询字符串中。

```js
const response = await axios.get(
  "https://jsonplaceholder.typicode.com/posts",
  {
    params: {
      userId: 1
    }
  }
);

console.log(response.data);
```

Axios 会把 `params` 转换到 URL：

```text
https://jsonplaceholder.typicode.com/posts?userId=1
```

不要把 GET 查询条件写进 `data`：

```js
// 不推荐：GET 的筛选条件应使用 params
const config = {
  params: {
    userId: 1
  }
};
```

---

### 5. 发送带请求体的请求

#### 5.1 POST：新建资源

`axios.post(url, data?, config?)` 的第二个参数是请求体。

```js
const newApplication = {
  applicantName: "山田太郎",
  date: "2026-09-01",
  reason: "私事"
};

const response = await axios.post(
  "/api/applications",
  newApplication
);

console.log(response.data);
```

#### 5.2 PUT：整体更新

`axios.put(url, data?, config?)` 通常用于提交资源的完整新状态。

```js
await axios.put(
  "/api/applications/101",
  {
    applicantName: "山田太郎",
    date: "2026-09-02",
    reason: "家庭事务",
    status: "submitted"
  }
);
```

#### 5.3 PATCH：部分更新

`axios.patch(url, data?, config?)` 通常只提交需要改变的字段。

```js
await axios.patch(
  "/api/applications/101",
  {
    status: "approved"
  }
);
```

#### 5.4 DELETE：删除资源

```js
await axios.delete("/api/applications/101");
```

如果后端要求 DELETE 携带请求体，请把数据放在配置对象的 `data` 中：

```js
await axios.delete(
  "/api/applications/101",
  {
    data: {
      reason: "重复申请"
    }
  }
);
```

是否允许、是否需要 DELETE 请求体由后端接口规格决定，不应自行猜测。

#### 5.5 常用方法签名

| 方法 | 常用签名 |
| --- | --- |
| GET | `axios.get(url, config?)` |
| POST | `axios.post(url, data?, config?)` |
| PUT | `axios.put(url, data?, config?)` |
| PATCH | `axios.patch(url, data?, config?)` |
| DELETE | `axios.delete(url, config?)` |

要特别注意：GET、DELETE 的第二个参数是配置对象；POST、PUT、PATCH 的第二个参数是请求体。

---

## 第二部分：请求配置对象

### 6. 使用 axios(config)

除快捷方法外，也可以把完整请求写成一个配置对象。

```js
const response = await axios({
  baseURL: "https://api.example.com",
  url: "/applications",
  method: "get",
  params: {
    status: "submitted"
  },
  timeout: 5000
});
```

#### 6.1 常用配置

| 配置 | 常见取值 | 作用 |
| --- | --- | --- |
| `baseURL` | `"https://api.example.com"`、`"/api"` | 所有相对请求的基础地址 |
| `url` | `"/applications"` | 当前资源路径 |
| `method` | `"get"`、`"post"`、`"put"`、`"patch"`、`"delete"` | HTTP 方法 |
| `params` | 普通对象 | URL 查询参数 |
| `data` | 对象、字符串、FormData 等 | 请求体 |
| `headers` | 普通对象 | 请求头 |
| `timeout` | 毫秒数，`0` 表示不由 Axios 限时 | 超时时间 |
| `signal` | `AbortSignal` | 取消请求 |
| `withCredentials` | `true` / `false` | 跨源请求是否携带凭证 |
| `validateStatus` | 接收状态码的函数 | 决定哪些状态算成功 |

快捷方法内部也使用同样的配置。

#### 6.2 baseURL 和 url

```js
const client = axios.create({
  baseURL: "/api"
});

await client.get("/applications");
```

最终请求地址相当于：

```text
/api/applications
```

`baseURL` 适合放接口公共前缀，`url` 只写资源路径。

#### 6.3 headers

```js
await axios.post(
  "/api/applications",
  {
    date: "2026-09-01"
  },
  {
    headers: {
      "X-Request-Id": crypto.randomUUID()
    }
  }
);
```

发送普通对象时，Axios 通常会按 JSON 处理请求体。是否需要手动设置 `Content-Type`，应根据数据类型和后端规格决定。

不要把真实密码、永久密钥或不应暴露的机密写在前端代码中。浏览器中的代码和请求都能被用户查看。

#### 6.4 timeout

```js
await axios.get(
  "/api/applications",
  {
    timeout: 5000
  }
);
```

`timeout: 5000` 表示 Axios 等待超过约 5 秒后中止本次请求。它不能保证服务器也停止处理已经收到的请求。

#### 6.5 validateStatus

Axios 默认把常见 2xx 状态作为成功。可以用 `validateStatus(status)` 调整判定规则。

```js
const response = await axios.get(
  "/api/applications/101",
  {
    validateStatus(status) {
      return status >= 200 && status < 500;
    }
  }
);
```

这样 404 也会进入成功分支，所以必须由调用方显式判断。

```js
if (response.status === 404) {
  console.log("申请不存在");
}
```

项目中不要随意放宽成功范围，否则容易漏掉错误。只有接口设计明确需要时才修改。

---

## 第三部分：错误与取消

### 7. Axios 的错误分类

Axios 请求失败时，应通过 `try...catch` 处理。

```js
import axios from "axios";

async function loadApplications() {
  try {
    const response = await axios.get("/api/applications");
    return response.data;
  } catch (error) {
    if (!axios.isAxiosError(error)) {
      throw error;
    }

    if (error.response) {
      console.error("服务器返回错误", error.response.status);
    } else if (error.request) {
      console.error("没有收到服务器响应");
    } else {
      console.error("请求配置失败", error.message);
    }

    throw error;
  }
}
```

`axios.isAxiosError(error)` 判断错误是否由 Axios 产生。

#### 7.1 三种常见情况

| 判断 | 含义 | 常见原因 |
| --- | --- | --- |
| `error.response` 存在 | 收到响应，但状态不符合成功规则 | 400、401、403、404、500 |
| `error.request` 存在 | 请求已发出，但没有收到可用响应 | 断网、服务器不可达、跨域失败 |
| 两者都不存在 | 发送前或配置阶段出错 | 配置值错误、代码异常 |

#### 7.2 常见错误属性

| 属性 | 作用 |
| --- | --- |
| `error.message` | 错误说明 |
| `error.code` | Axios 错误代码 |
| `error.config` | 原请求配置 |
| `error.response.status` | HTTP 状态码 |
| `error.response.data` | 后端错误响应体 |
| `error.request` | 已创建的底层请求 |

调试日志可以记录必要信息，但页面不应直接展示完整内部错误对象。

#### 7.3 401 和 403

- `401 Unauthorized`：通常表示尚未登录、凭证无效或凭证过期；
- `403 Forbidden`：通常表示已经识别用户，但用户没有权限执行操作。

最终含义以项目接口规格为准。前端隐藏按钮不能代替后端权限校验。

---

### 8. 取消请求

用户离开页面、重新搜索或不再需要旧请求时，可以取消请求。

```js
const controller = new AbortController();

const requestPromise = axios.get(
  "/api/applications",
  {
    signal: controller.signal
  }
);

controller.abort();

try {
  await requestPromise;
} catch (error) {
  if (axios.isCancel(error)) {
    console.log("请求已取消");
  } else {
    throw error;
  }
}
```

`new AbortController()` 创建取消控制器：

- `controller.signal` 传给 Axios；
- `controller.abort()` 发出取消通知。

Axios 取消错误通常可以通过 `axios.isCancel(error)` 或错误代码 `ERR_CANCELED` 识别。

取消是正常控制流程时，不要显示成“系统故障”。

---

## 第四部分：统一 Axios 实例

### 9. 为什么创建实例

如果每次请求都重复写基础地址、超时和请求头，容易出现配置不一致。

```js
axios.get("/api/applications", { timeout: 5000 });
axios.post("/api/applications", data, { timeout: 5000 });
```

`axios.create(config)` 创建一个具有公共配置的 Axios 实例。

```js
// src/api/client.js
import axios from "axios";

export const apiClient = axios.create({
  baseURL: "/api",
  timeout: 5000,
  headers: {
    Accept: "application/json"
  }
});
```

以后统一使用 `apiClient`：

```js
const response = await apiClient.get("/applications");
```

不要在各页面中重新创建实例，否则公共配置仍然会分散。

---

### 10. 请求拦截器

请求拦截器会在请求发出前执行。

```js
const requestInterceptorId =
  apiClient.interceptors.request.use(
    (config) => {
      config.headers["X-Request-Id"] =
        crypto.randomUUID();

      return config;
    },
    (error) => Promise.reject(error)
  );
```

`interceptors.request.use(onFulfilled, onRejected)` 注册请求拦截器，并返回编号。

成功回调必须返回 `config`，否则请求无法继续。

常见用途：

- 添加追踪编号；
- 从统一位置读取短期访问令牌；
- 记录不含敏感数据的调试信息。

不适合：

- 修改页面 DOM；
- 写具体页面业务判断；
- 输出密码、令牌或完整个人信息；
- 为所有请求盲目覆盖请求头。

如果组件或测试临时注册了拦截器，可以移除：

```js
apiClient.interceptors.request.eject(
  requestInterceptorId
);
```

---

### 11. 响应拦截器

响应拦截器在收到响应后、业务代码拿到结果前执行。

```js
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401
    ) {
      console.warn("登录状态可能已失效");
    }

    return Promise.reject(error);
  }
);
```

成功回调返回 `response`，失败回调使用 `Promise.reject(error)` 继续把错误交给调用方。

#### 11.1 是否统一返回 response.data

有些项目在响应拦截器中只返回 `response.data`：

```js
apiClient.interceptors.response.use(
  (response) => response.data
);
```

这样调用方便，但会失去直接读取 `status`、`headers` 等信息，而且 TypeScript 类型也需要同步调整。

本教程采用更清晰的规则：

- 拦截器保留完整 `response`；
- 每个业务接口函数明确返回 `response.data`。

团队应选择一种响应约定并保持一致。

---

## 第五部分：接口层封装

### 12. 推荐目录

在构建工具项目中，可以使用以下结构：

```text
src/
├─ api/
│  ├─ client.js
│  └─ applications.js
├─ pages/
│  └─ application-list.js
└─ main.js
```

| 文件 | 职责 |
| --- | --- |
| `client.js` | Axios 实例、公共配置、拦截器 |
| `applications.js` | 申请业务相关接口函数 |
| `application-list.js` | 页面状态和 DOM 更新 |
| `main.js` | 应用入口 |

第 22 章的本地存储模块可以替换为这里的接口模块，页面不直接拼接 Axios 配置。

---

### 13. 编写公共客户端

```js
// src/api/client.js
import axios from "axios";

export const apiClient = axios.create({
  baseURL: "/api",
  timeout: 5000,
  headers: {
    Accept: "application/json"
  }
});

apiClient.interceptors.request.use(
  (config) => {
    config.headers["X-Request-Id"] =
      crypto.randomUUID();

    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
);
```

真实项目中的 `baseURL` 往往来自环境配置，但环境变量写法取决于构建工具，应按照项目规则处理。

---

### 14. 编写申请接口模块

```js
// src/api/applications.js
import { apiClient } from "./client.js";

export async function getApplications(params = {}) {
  const response = await apiClient.get(
    "/applications",
    {
      params
    }
  );

  return response.data;
}

export async function getApplication(id) {
  const response = await apiClient.get(
    `/applications/${id}`
  );

  return response.data;
}

export async function createApplication(application) {
  const response = await apiClient.post(
    "/applications",
    application
  );

  return response.data;
}

export async function updateApplication(
  id,
  application
) {
  const response = await apiClient.put(
    `/applications/${id}`,
    application
  );

  return response.data;
}

export async function updateApplicationStatus(
  id,
  status
) {
  const response = await apiClient.patch(
    `/applications/${id}`,
    {
      status
    }
  );

  return response.data;
}

export async function deleteApplication(id) {
  await apiClient.delete(`/applications/${id}`);
}
```

这个模块只描述业务接口：

- 请求哪个资源；
- 使用什么方法；
- 传递什么数据；
- 返回什么业务数据。

它不读取按钮、不操作列表、不显示消息。

> URL、字段名、返回结构必须以实际接口规格为准。本章的 `/api/applications` 是教学用约定，不代表任何现有后端一定提供这些接口。

---

### 15. 页面模块处理界面状态

页面加载数据至少应考虑四种结果：

```text
开始请求 → 加载中
             ├─ 有数据 → 成功列表
             ├─ 无数据 → 空状态
             └─ 失败   → 错误状态
```

```js
// src/pages/application-list.js
import axios from "axios";

import {
  getApplications
} from "../api/applications.js";

const message = document.querySelector("#message");
const list = document.querySelector("#application-list");

function renderApplications(applications) {
  list.replaceChildren();

  for (const application of applications) {
    const item = document.createElement("li");
    item.textContent =
      `${application.date}：${application.status}`;
    list.append(item);
  }
}

export async function initializeApplicationList() {
  message.textContent = "正在读取申请数据……";

  try {
    const applications = await getApplications();

    if (applications.length === 0) {
      message.textContent = "当前没有申请数据";
      list.replaceChildren();
      return;
    }

    renderApplications(applications);
    message.textContent =
      `已读取 ${applications.length} 条申请`;
  } catch (error) {
    if (axios.isCancel(error)) {
      message.textContent = "读取已取消";
      return;
    }

    console.error("申请数据读取失败", error);
    message.textContent =
      "数据读取失败，请稍后重试";
  }
}
```

页面模块负责用户可见状态，接口模块负责 HTTP 通信。错误日志保留给开发者，用户消息使用可理解且不过度暴露内部信息的文本。

---

### 16. 应用入口

```js
// src/main.js
import {
  initializeApplicationList
} from "./pages/application-list.js";

initializeApplicationList();
```

入口文件只负责启动应用，不堆积具体业务代码。

---

## 第六部分：认证、跨域与安全边界

### 17. withCredentials

跨源请求是否携带 Cookie 等凭证，可以使用 `withCredentials` 配置。

```js
const apiClient = axios.create({
  baseURL: "https://api.example.com",
  withCredentials: true
});
```

这并不是单方面设置就一定成功。后端还要正确配置：

- 允许的来源；
- 是否允许凭证；
- Cookie 的 `SameSite`、`Secure` 等属性；
- 允许的方法和请求头。

不要把跨域问题简单理解成“前端加一个选项”。

---

### 18. Authorization 请求头

接口使用访问令牌时，可能要求：

```js
await apiClient.get("/applications", {
  headers: {
    Authorization: `Bearer ${accessToken}`
  }
});
```

注意：

- 令牌来源和保存方式必须遵守项目安全设计；
- 不要在教程、仓库或日志中写真实令牌；
- 不要记录完整 Authorization 请求头；
- 前端鉴权逻辑不能替代后端验证；
- 是否自动附加令牌，应根据同源范围和接口域名谨慎决定。

---

## 第七部分：调试与验证

### 19. 使用 Network 面板

只看 Console 不足以判断请求问题。打开浏览器开发者工具的 Network 面板，检查：

1. Request URL 是否正确；
2. Request Method 是否符合接口规格；
3. Query String Parameters 是否正确；
4. Request Headers 是否包含需要的信息；
5. Request Payload 是否符合字段和类型要求；
6. Status Code 是多少；
7. Response Headers 和 Response 内容是什么；
8. 请求耗时是否异常。

#### 19.1 常见现象

| 现象 | 优先检查 |
| --- | --- |
| 404 | URL、baseURL、路径参数、服务器路由 |
| 400 | 请求体字段、类型、必填项 |
| 401 | 登录状态、凭证、令牌是否过期 |
| 403 | 用户权限、服务端授权规则 |
| 415 | `Content-Type` 与请求体格式 |
| 500 | 后端日志、请求编号、服务端异常 |
| CORS 错误 | 来源、预检请求、后端跨域配置 |
| 一直等待 | 服务状态、网络、超时设置 |
| 返回 HTML | URL 可能指向前端页面或 404 页面 |

---

### 20. 常见代码错误

#### 20.1 忘记 await

```js
const responsePromise = apiClient.get("/applications");
console.log(responsePromise); // Promise
```

正确：

```js
const response = await apiClient.get("/applications");
console.log(response.data);
```

#### 20.2 把 Axios 响应当成 Fetch 响应

```js
const response = await apiClient.get("/applications");

// 错误：Axios 响应没有 Fetch 的 json() 用法
// const data = await response.json();

const data = response.data;
```

#### 20.3 把 params 和 data 混淆

- URL 查询条件使用 `params`；
- POST、PUT、PATCH 请求体通常使用 `data`，在快捷方法中就是第二个参数。

#### 20.4 拦截器没有返回

```js
apiClient.interceptors.request.use((config) => {
  config.headers["X-Request-Id"] = crypto.randomUUID();
  return config;
});
```

必须返回 `config`。响应成功拦截器也必须返回约定的值。

#### 20.5 在页面中重复 baseURL

```js
// 不推荐
apiClient.get("/api/applications");
```

如果实例的 `baseURL` 已经是 `"/api"`，这里只写 `"/applications"`。

#### 20.6 捕获错误后完全吞掉

```js
try {
  await apiClient.get("/applications");
} catch (error) {
  console.error(error);
  throw error;
}
```

接口层如果无法恢复，应继续抛出错误，让页面决定如何显示。不要让失败悄悄变成 `undefined`。

---

### 21. 本章练习

#### 练习 1：直接 URL 验证 Axios

使用 Axios 请求：

```text
https://jsonplaceholder.typicode.com/posts
```

要求：

1. 使用 `params` 传递 `userId: 1`；
2. 输出状态码；
3. 输出返回数组；
4. 显示加载中、成功和失败状态；
5. 在 Network 面板确认最终 URL。

此练习只用于验证 Axios 基础请求，不依赖本地 JSON 文件。

#### 练习 2：整理申请接口层

在已有构建工具项目中创建：

```text
src/api/client.js
src/api/applications.js
src/pages/application-list.js
src/main.js
```

要求：

1. 创建统一 Axios 实例；
2. 设置 `baseURL` 和 `timeout`；
3. 封装查询、新建、修改状态、删除函数；
4. 页面模块不得直接导入 Axios 实例；
5. 页面必须区分加载、空数据、成功、失败；
6. 根据实际接口规格调整 URL 和字段；
7. 使用 Network 面板记录一次成功请求和一次失败请求。

#### 扩展练习：取消旧查询

为筛选查询增加取消功能：

1. 新查询开始前取消旧查询；
2. 把 `signal` 传给接口函数；
3. 取消时不显示系统错误；
4. 真正失败时显示重试提示。

---

### 22. 本章小结

本章形成了项目请求代码的基本边界：

```text
main.js
   ↓
页面模块：处理 DOM 和页面状态
   ↓
业务接口模块：描述资源和业务操作
   ↓
Axios 实例：统一基础地址、超时和拦截器
   ↓
后端 API
```

必须掌握：

- GET、POST、PUT、PATCH、DELETE 的常见写法；
- `params`、`data`、`headers`、`timeout`；
- `response.data`；
- `try...catch` 与 Axios 错误分类；
- `axios.create()`；
- 接口模块与页面模块的职责分离；
- Network 面板验证。

需要理解：

- `signal` 取消请求；
- `validateStatus`；
- 请求与响应拦截器；
- `withCredentials`、Cookie、CORS 和认证的边界；
- 为什么团队需要统一响应约定。

完成本章后，可以进入 JavaScript 综合项目，把前面学习的 DOM、事件、校验、异步请求、模块化和接口层整合起来。

