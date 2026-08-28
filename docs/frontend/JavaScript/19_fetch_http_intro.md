# 第 19 章 HTTP 基础与 fetch

前面的页面数据主要来自 JavaScript 数组、`localStorage` 或本地模拟函数。真实项目中，员工信息、申请记录等业务数据通常保存在服务器，由前端通过 HTTP 请求读取或提交。

完成本章后，你应当能够：

- 说明一次前后端请求经过的基本过程。
- 区分 URL、请求方法、请求头、请求体、状态码和响应体。
- 使用 `fetch()` 发送 GET 请求并读取 JSON。
- 使用 `options` 对象配置请求方法、请求头、请求体、凭据和取消信号。
- 正确检查 `response.ok`，处理 HTTP 错误。
- 使用查询参数发送检索条件。
- 使用 POST 发送 JSON 数据。
- 了解 PUT、PATCH 和 DELETE 的基本用途。
- 使用 `AbortController` 取消超时请求。
- 在页面中处理加载中、成功、空数据和失败状态。
- 使用浏览器 Network 面板检查请求和响应。

本章只使用浏览器原生的 `fetch()`。第 23 章会在掌握 HTTP 和 ES 模块后，再学习第三方请求库 Axios。

如果需要系统学习前后端职责、REST、Cookie、Session 和 CORS，请参阅 [HTTP、REST、Cookie、Session 与 CORS](../../web_basics/01_http_rest_cookie_cors.md)。本章只回顾编写请求代码所需的 HTTP 概念。

## 1. 前端为什么要发送请求

真实项目中的常见场景包括：

- 登录时把账号和密码提交给后端验证。
- 打开首页时读取当前用户信息。
- 打开申请列表时读取申请记录。
- 提交申请时把表单数据发送给后端。
- 删除或修改申请后，重新读取最新状态。

一次请求可以先这样理解：

```text
浏览器中的 JavaScript
    │ 发送 HTTP 请求
    ▼
后端接口
    │ 校验、执行业务处理、访问数据库
    ▼
HTTP 响应
    │ 状态码、响应头、响应体
    ▼
JavaScript 处理结果并更新页面
```

这是一张简化图，省略了 DNS、TLS、Web 服务器、反向代理等组件。本章重点是浏览器代码怎样发出请求并处理响应。

## 2. HTTP 请求和响应的基本组成

### 2.1 请求包含什么

下面是一条用于查询申请列表的地址：

```text
https://example.com/api/applications?status=pending
```

| 部分 | 示例 | 作用 |
| --- | --- | --- |
| 协议 | `https` | 规定通信方式；实际项目优先使用 HTTPS |
| 主机 | `example.com` | 指定访问哪台服务器 |
| 路径 | `/api/applications` | 指定后端资源或接口 |
| 查询参数 | `status=pending` | 向查询接口传递筛选条件 |

HTTP 请求还可能包含：

| 名称 | 作用 |
| --- | --- |
| method | 表示查询、新增、修改或删除等操作 |
| headers | 传递数据格式、认证信息等附加信息 |
| body | 向后端提交的主要数据，GET 通常不使用请求体 |

### 2.2 常见请求方法

| 方法 | 常见用途 | 示例 |
| --- | --- | --- |
| `GET` | 查询数据 | 读取申请列表 |
| `POST` | 新增数据或执行提交操作 | 新建申请、登录 |
| `PUT` | 整体替换一条数据 | 提交申请的完整新状态 |
| `PATCH` | 修改部分字段 | 只修改申请状态 |
| `DELETE` | 删除数据 | 删除一条草稿 |

接口的实际含义由后端规格决定，不能只凭方法名猜测。基础阶段重点掌握 GET 和 POST，并能读懂其他三种方法。

### 2.3 响应包含什么

服务器返回的 HTTP 响应主要包括：

| 名称 | 作用 |
| --- | --- |
| status | 状态码，例如 `200`、`404`、`500` |
| headers | 返回数据格式、缓存规则等信息 |
| body | JSON、文本、文件等实际响应内容 |

常见状态码：

| 状态码 | 常见含义 | 前端通常怎样处理 |
| --- | --- | --- |
| `200 OK` | 查询或处理成功 | 读取并显示数据 |
| `201 Created` | 新数据创建成功 | 显示成功结果或跳转 |
| `204 No Content` | 成功，但没有响应体 | 不调用 `response.json()` |
| `400 Bad Request` | 请求格式或内容错误 | 显示输入或请求错误 |
| `401 Unauthorized` | 未认证或认证失效 | 引导重新登录 |
| `403 Forbidden` | 已识别用户但没有权限 | 显示无权限提示 |
| `404 Not Found` | 地址或资源不存在 | 检查 URL 或显示不存在 |
| `409 Conflict` | 数据状态冲突 | 提示刷新或重新操作 |
| `500 Internal Server Error` | 后端内部错误 | 显示系统错误并保留排查信息 |

状态码只表示 HTTP 层结果。项目还可能在 JSON 中返回业务错误代码，应按接口规格处理。

## 3. 使用 `fetch()` 发送 GET 请求

### 3.1 先看懂请求 URL

本节使用公开测试接口：

```text
https://jsonplaceholder.typicode.com/posts?_limit=3
```

这条 URL 可以分成：

| 部分 | 当前内容 | 作用 |
| --- | --- | --- |
| 协议 | `https` | 使用加密的 HTTP 通信 |
| 主机 | `jsonplaceholder.typicode.com` | 测试接口所在服务器 |
| 路径 | `/posts` | 请求文章列表资源 |
| 查询参数 | `_limit=3` | 要求测试接口最多返回三条数据 |

URL 是完整字符串，必须放在引号中。企业项目中也可能使用相对 URL：

```text
/api/applications
```

相对 URL 没有协议和主机，浏览器会把它发送到当前页面所在的来源。前端究竟使用完整 URL、相对 URL 还是开发服务器代理地址，应以项目配置为准。

公开测试接口依赖网络和外部服务，只用于课堂练习。如果暂时无法访问，可以先阅读代码和 Network 检查步骤，之后在网络可用时验证。

### 3.2 发送请求并查看 Response

以下代码可以放入浏览器页面的 `app.js`：

```js
async function loadPosts() {
  const url =
    "https://jsonplaceholder.typicode.com/posts?_limit=3";
  const response = await fetch(url);
  console.log(response);
}

loadPosts().catch((error) => {
  console.error("请求执行失败", error);
});
```

`fetch(resource, options)` 启动 HTTP 请求，并返回 Promise：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `resource` | URL 字符串、URL 对象或 Request 对象 | 必填 | 指定请求地址 |
| `options` | 配置对象 | 可选 | 指定 method、headers、body、signal 等 |

Promise 的成功值是 `Response` 对象。它表示浏览器已经收到 HTTP 响应，但它不是最终的申请数组。

### 3.3 使用 `response.json()` 读取 JSON

把 `loadPosts()` 改为：

```js
async function loadPosts() {
  const url =
    "https://jsonplaceholder.typicode.com/posts?_limit=3";
  const response = await fetch(url);
  const posts = await response.json();
  return posts;
}

loadPosts()
  .then((posts) => {
    console.log(posts);
  })
  .catch((error) => {
    console.error("请求执行失败", error);
  });
```

`response.json()` 读取响应体，并把 JSON 转换为 JavaScript 值。它也返回 Promise，因此前面需要写 `await`。

执行顺序是：

```text
fetch() 等待 HTTP 响应
        ↓
得到 Response 对象
        ↓
response.json() 读取并解析响应体
        ↓
得到包含三篇文章的 JavaScript 数组
```

响应体通常只能读取一次。不要对同一个 Response 重复调用 `json()`、`text()` 等读取方法。

每条测试数据包含 `userId`、`id`、`title` 和 `body`。这些字段由测试接口规定；真实项目应根据接口设计书确认字段名称和类型。

### 3.4 `options` 配置对象

`fetch()` 的第二个参数是可选的 `options` 对象，也常写作 `init`。只发送普通 GET 请求时可以省略：

```js
const response = await fetch(url);
```

需要指定请求方法、请求头、请求体、认证信息或取消信号时再传入：

```js
const response = await fetch("/api/applications", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
  body: JSON.stringify({
    type: "休假申请",
    startDate: "2026-09-10",
  }),
  credentials: "same-origin",
});
```

这个对象不是后端收到的业务数据。它是浏览器发送请求时使用的配置。

常见属性如下：

| 属性 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `method` | `GET`、`POST`、`PUT`、`PATCH`、`DELETE` 等方法字符串 | 可选，默认 `GET` | 指定 HTTP 请求方法 |
| `headers` | 普通键值对象、`Headers` 对象或键值对数组 | 可选，默认没有自定义请求头 | 设置 Content-Type、Accept 等请求头 |
| `body` | 字符串、`FormData`、`URLSearchParams`、Blob 等 | 可选，默认没有请求体 | 设置发送给后端的请求内容；GET 和 HEAD 不能设置 |
| `signal` | `AbortSignal` 对象 | 可选 | 接收取消通知，用于主动取消或实现超时 |
| `credentials` | `omit`、`same-origin`、`include` | 可选，默认 `same-origin` | 控制是否发送和接收 Cookie 等凭据 |
| `mode` | `cors`、`same-origin`、`no-cors` | 可选，普通跨来源请求通常使用默认的 `cors` | 控制请求的跨来源模式 |
| `cache` | `default`、`no-store`、`reload`、`no-cache`、`force-cache`、`only-if-cached` | 可选，默认 `default` | 控制请求怎样使用浏览器 HTTP 缓存 |
| `redirect` | `follow`、`error`、`manual` | 可选，默认 `follow` | 控制遇到重定向响应时怎样处理 |

#### 3.4.1 `method`、`headers` 和 `body`

这三个属性通常配合使用：

```js
const response = await fetch("/api/applications", {
  method: "PATCH",
  headers: {
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    status: "approved",
  }),
});
```

- `method` 决定本次请求要执行的操作。
- `Content-Type` 告诉后端当前请求体的格式。
- `body` 放置真正提交的数据。
- 发送 JSON 时，需要先使用 `JSON.stringify()` 转成字符串。
- 使用 `FormData` 上传表单或文件时，通常不要手动设置 `Content-Type`，浏览器会自动添加包含 boundary 的正确请求头。

查询参数不属于 `options`。GET 的检索条件仍然写在 URL 中，第 5 节会说明安全的构造方式。

#### 3.4.2 `credentials`

`credentials` 控制浏览器是否在请求中携带 Cookie 等凭据：

| 值 | 含义 |
| --- | --- |
| `omit` | 不发送凭据，也忽略响应中用于设置凭据的信息 |
| `same-origin` | 只在同源请求中使用凭据，默认值 |
| `include` | 同源和跨来源请求都尝试使用凭据 |

跨来源请求使用 `include` 时，后端还必须返回允许指定来源和凭据的 CORS 响应头，Cookie 本身也会受到 SameSite 等规则限制。仅修改前端选项不能绕过服务器限制。

#### 3.4.3 `signal`

```js
const controller = new AbortController();

const response = await fetch("/api/applications", {
  signal: controller.signal,
});
```

`signal` 本身不会自动取消请求。其他代码调用 `controller.abort()` 后，信号才会通知 `fetch()` 停止请求。第 8 节会用它实现超时。

#### 3.4.4 `mode`、`cache` 和 `redirect`

这三个属性在有明确项目需求时再设置：

- 普通跨来源接口请求通常保持 `mode: "cors"`。
- `mode: "same-origin"` 会阻止向其他来源发送请求。
- 不要使用 `mode: "no-cors"` 解决 CORS 错误；得到的通常是不允许读取状态和响应体的 opaque 响应。
- `cache: "no-store"` 表示不使用也不保存 HTTP 缓存，适合明确要求每次获取最新结果的场景。
- `cache: "no-cache"` 并不等于完全不用缓存，而是要求先向服务器验证缓存是否仍然有效。
- `redirect: "follow"` 会自动跟随重定向，也是默认行为。

大多数业务请求只需要 `method`、`headers`、`body` 和必要的 `signal`。不要为了显得配置完整而机械填写所有属性。

## 4. fetch 为什么必须检查响应状态

### 4.1 404 和 500 不一定进入 `catch`

`fetch()` 在网络无法连接、请求被取消等情况下会失败。但服务器正常返回 `404` 或 `500` 时，`fetch()` 通常仍会成功得到 Response。

因此，不能只写：

```js
const response = await fetch(
  "https://jsonplaceholder.typicode.com/unknown-path",
);
const data = await response.json();
```

还必须检查状态。

### 4.2 使用 `response.ok`

```js
async function loadPosts() {
  const url =
    "https://jsonplaceholder.typicode.com/posts?_limit=3";
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`文章列表读取失败：HTTP ${response.status}`);
  }

  return response.json();
}
```

- `response.ok` 是布尔值，状态码在 200～299 时为 `true`。
- `response.status` 是数字状态码。
- `throw new Error(message)` 抛出错误，使当前 `async` 函数返回的 Promise 失败。

调用方可以统一使用 `try...catch`：

```js
async function initializePage() {
  try {
    const posts = await loadPosts();
    console.log(posts);
  } catch (error) {
    console.error(error);
  }
}

initializePage();
```

## 5. 使用查询参数发送检索条件

查询参数放在 URL 的 `?` 后面：

```text
/api/applications?status=pending&employeeNumber=EMP-00001
```

不要手动拼接未经处理的用户输入。可以使用 `URLSearchParams`：

```js
async function searchApplications(status, employeeNumber) {
  const params = new URLSearchParams({
    status,
    employeeNumber,
  });

  const response = await fetch(`/api/applications?${params.toString()}`);

  if (!response.ok) {
    throw new Error(`申请检索失败：HTTP ${response.status}`);
  }

  return response.json();
}
```

`new URLSearchParams(init)` 创建查询参数对象。本例传入普通对象，属性名成为参数名，属性值成为参数值。

`params.toString()` 返回经过 URL 编码的查询字符串，例如：

```text
status=pending&employeeNumber=EMP-00001
```

参数名和取值必须与后端接口规格一致。

## 6. 使用 POST 发送 JSON

下面是向后端新增申请的请求函数。它属于接口代码片段，需要由后端提供 `/api/applications` 接口才能实际运行。

```js
async function createApplication(application) {
  const response = await fetch("/api/applications", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(application),
  });

  if (!response.ok) {
    throw new Error(`申请提交失败：HTTP ${response.status}`);
  }

  return response.json();
}
```

第 3.4 节已经说明 `options` 的常见属性。这里使用其中三个：`method` 指定 POST，`headers` 声明请求体格式，`body` 保存 JSON 字符串。

`Content-Type: application/json` 告诉后端请求体使用 JSON 格式。`JSON.stringify(application)` 把 JavaScript 对象转换成 JSON 字符串。

调用示例：

```js
const newApplication = {
  employeeNumber: "EMP-00001",
  type: "休假申请",
  startDate: "2026-09-10",
};

createApplication(newApplication)
  .then((createdApplication) => {
    console.log("创建成功", createdApplication);
  })
  .catch((error) => {
    console.error("创建失败", error);
  });
```

不要把密码、令牌或内部地址直接写死在前端源码中。浏览器中的代码和请求内容都可能被用户查看。

## 7. PUT、PATCH 和 DELETE 的基本结构

### 7.1 PATCH：修改部分字段

```js
async function updateApplicationStatus(id, status) {
  const response = await fetch(`/api/applications/${id}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ status }),
  });

  if (!response.ok) {
    throw new Error(`状态更新失败：HTTP ${response.status}`);
  }

  return response.json();
}
```

### 7.2 DELETE：删除数据

```js
async function deleteApplication(id) {
  const response = await fetch(`/api/applications/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error(`申请删除失败：HTTP ${response.status}`);
  }
}
```

删除接口可能返回 `204 No Content`。这种响应没有 JSON 响应体，因此成功后直接结束，不要调用 `response.json()`。

`PUT` 的调用结构与 PATCH 接近，但通常发送资源的完整新状态。具体选择必须以接口设计书为准。

## 8. 请求超时和取消

`fetch()` 没有一个直接填写毫秒数的 `timeout` 属性。可以使用 `AbortController` 提供取消信号。

```js
async function loadApplicationsWithTimeout() {
  const controller = new AbortController();

  const timerId = setTimeout(() => {
    controller.abort();
  }, 5000);

  try {
    const response = await fetch("/api/applications", {
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`申请列表读取失败：HTTP ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error("请求超时，请稍后重试");
    }

    throw error;
  } finally {
    clearTimeout(timerId);
  }
}
```

`new AbortController()` 创建一个请求取消控制器：

- `controller.signal` 是传给 `fetch()` 的取消信号。
- `controller.abort()` 发出取消通知。
- 请求因取消而失败时，错误名称通常是 `AbortError`。
- `clearTimeout(timerId)` 清理已经不再需要的超时定时器。

项目是否设置五秒超时，应根据接口性能要求和业务规格决定，不能把示例数值机械用于所有请求。

## 9. 页面中的完整请求状态

下面的完整 HTML 直接请求公开测试 URL。保存为 `fetch-demo.html`，使用浏览器打开后点击按钮。示例依赖网络连接和公开测试服务。

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>fetch URL 请求示例</title>
  </head>
  <body>
    <button id="loadButton" type="button">读取文章</button>
    <p id="statusMessage" aria-live="polite">点击按钮开始读取。</p>
    <ul id="postList"></ul>

    <script>
      const loadButton = document.querySelector("#loadButton");
      const statusMessage =
        document.querySelector("#statusMessage");
      const postList = document.querySelector("#postList");

      async function loadPosts() {
        const requestUrl =
          "https://jsonplaceholder.typicode.com/posts?_limit=3";
        const response = await fetch(requestUrl);

        if (!response.ok) {
          throw new Error(
            `文章列表读取失败：HTTP ${response.status}`,
          );
        }

        return response.json();
      }

      function renderPosts(posts) {
        postList.replaceChildren();

        for (const post of posts) {
          const item = document.createElement("li");
          item.textContent = post.title;
          postList.append(item);
        }
      }

      async function handleLoad() {
        loadButton.disabled = true;
        statusMessage.textContent = "读取中……";
        postList.replaceChildren();

        try {
          const posts = await loadPosts();

          if (posts.length === 0) {
            statusMessage.textContent = "没有文章数据";
            return;
          }

          renderPosts(posts);
          statusMessage.textContent = "读取完成";
        } catch (error) {
          console.error(error);
          statusMessage.textContent =
            "文章列表读取失败，请稍后再试";
        } finally {
          loadButton.disabled = false;
        }
      }

      loadButton.addEventListener("click", () => {
        handleLoad().catch((error) => {
          console.error("未预期的页面错误", error);
        });
      });
    </script>
  </body>
</html>
```

验证以下状态：

| 场景 | 操作 | 页面结果 |
| --- | --- | --- |
| 加载中 | 点击按钮 | 按钮禁用，显示“读取中” |
| 成功 | 使用 `/posts?_limit=3` | 显示三条文章标题 |
| 空数据 | 把 URL 临时改为 `/posts?_limit=0` | 显示“没有文章数据” |
| HTTP 失败 | 把路径临时改为 `/unknown-path` | 显示失败提示，控制台保留状态信息 |
| 请求结束 | 成功或失败 | 按钮恢复可用 |

测试完成后，把 URL 恢复为第 3 节使用的完整地址。

## 10. CORS 是什么

浏览器会限制网页随意读取其他来源的响应。协议、主机或端口任意一项不同，通常就属于不同来源：

```text
页面：http://localhost:5500
接口：http://localhost:8080
```

即使主机都是 `localhost`，端口不同也属于跨来源请求。

CORS 是服务器通过响应头告诉浏览器“哪些来源可以读取响应”的机制。如果控制台出现 CORS 错误：

1. 先确认请求地址是否正确。
2. 确认后端是否允许当前前端来源。
3. 开发环境可以使用经过配置的开发服务器代理。
4. 不要通过关闭浏览器安全功能解决正式项目问题。

CORS 是浏览器的读取限制，不等于后端权限控制。即使页面隐藏按钮或请求被浏览器拦截，后端仍必须进行认证、授权和数据校验。

## 11. 使用 Network 面板排查请求

打开浏览器开发者工具的 Network 面板，重新执行请求，重点查看：

| 检查项 | 要确认的内容 |
| --- | --- |
| Request URL | 地址、路径和查询参数是否正确 |
| Request Method | 是否使用接口要求的 GET、POST 等方法 |
| Status Code | 是成功、前端请求错误还是后端错误 |
| Request Headers | Content-Type、认证信息是否符合规格 |
| Request Payload | 发送的 JSON 字段和值是否正确 |
| Response | 后端实际返回了什么 |
| Timing | 请求是否长时间等待 |

常见排查顺序：

```text
浏览器是否发出了请求
→ URL 和 method 是否正确
→ 状态码是什么
→ 请求数据是否正确
→ 响应体是什么
→ 前端解析和渲染是否报错
```

不要看到页面没数据显示，就直接判断是“后端问题”。Network 面板可以帮助区分请求没有发出、接口返回错误、JSON 解析失败和 DOM 渲染失败。

## 12. 常见错误

### 12.1 忘记检查 `response.ok`

症状：服务器返回 404 或 500，但代码仍继续解析或渲染。

修正：在读取响应体前检查 `response.ok`，不成功时抛出包含状态码的错误。

### 12.2 忘记等待 `response.json()`

```js
const data = response.json();
console.log(data); // Promise，不是最终数据
```

修正为：

```js
const data = await response.json();
```

### 12.3 GET 请求错误地设置 `body`

GET 查询条件通常放在 URL 查询参数中。使用 `URLSearchParams`，并遵守后端接口规格。

### 12.4 POST 直接发送普通对象

```js
body: application // 错误：普通对象不能直接作为 JSON 请求体
```

应设置 JSON 请求头，并写成：

```js
body: JSON.stringify(application)
```

### 12.5 对 204 响应调用 `json()`

204 没有响应体，继续解析 JSON 可能报错。根据状态码和接口规格决定是否读取响应体。

## 13. 本章练习

### 13.1 初始文件

新建：

```text
fetch-practice/
├── index.html
└── app.js
```

练习直接请求下面的公开测试 URL：

```text
https://jsonplaceholder.typicode.com/todos?_limit=5
```

每条数据包含 `userId`、`id`、`title` 和 `completed`。

### 13.2 任务要求

1. 在 HTML 中准备“读取任务”按钮、状态区域和列表。
2. 使用 `fetch()` 请求上面的完整 URL。
3. 检查 `response.ok`。
4. 使用 `response.json()` 得到数组。
5. 点击按钮后显示加载状态并禁用按钮。
6. 成功时显示任务标题和完成状态，空数组时显示空状态。
7. 失败时显示用户提示，并在控制台记录状态码。
8. 使用 Network 面板确认请求 URL、方法、状态码和响应体。
9. 把 URL 路径临时改成 `/unknown-path`，记录 404 时页面和 Network 面板的结果，然后恢复。
10. 把 `_limit` 改为 `0`，确认页面能够显示空数据状态。
11. 写出一个 POST 请求代码片段，用于提交新的任务对象；不要求公开测试服务永久保存数据。

### 13.3 完成标准

- 能解释 `fetch()` 与 `response.json()` 为什么都需要等待。
- 能说明为什么 404 不一定自动进入 `catch`。
- 能说明 `options` 中常用属性的作用以及 `credentials` 的三个取值。
- 能区分查询参数和 JSON 请求体。
- 页面覆盖加载中、成功、空数据和失败状态。
- 能根据 Network 信息判断问题发生在请求、响应还是页面处理阶段。
