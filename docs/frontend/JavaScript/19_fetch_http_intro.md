# 第 19 章 HTTP 请求、fetch 与 Axios 基础

前面的页面数据主要来自 JavaScript 数组、`localStorage` 或本地模拟函数。真实项目中，员工信息、申请记录等业务数据通常保存在服务器，由前端通过 HTTP 请求读取或提交。

完成本章后，你应当能够：

- 说明一次前后端请求经过的基本过程。
- 区分 URL、请求方法、请求头、请求体、状态码和响应体。
- 使用 `fetch()` 发送 GET 请求并读取 JSON。
- 使用 `options` 对象配置请求方法、请求头、请求体、凭据和取消信号。
- 正确检查 `response.ok`，处理 HTTP 错误。
- 使用查询参数发送检索条件。
- 使用 POST 发送 JSON、URL 编码表单和 `FormData`。
- 使用 `fetch()` 或 Axios 上传单个文件和多个文件。
- 说明普通文件上传与大文件分片上传的区别。
- 了解 PUT、PATCH 和 DELETE 的基本用途。
- 使用 `AbortController` 取消超时请求。
- 在页面中处理加载中、成功、空数据和失败状态。
- 使用浏览器 Network 面板检查请求和响应。
- 使用Axios发送基础GET、POST请求并读取`response.data`。
- 根据项目规范选择`fetch()`或Axios，不在同一功能中无理由混用。

本章先使用浏览器原生的 `fetch()` 理解HTTP请求，再学习第三方请求库Axios的基础用法。

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

## 4. fetch 为什么必须检查响应状态

### 4.1 404 和 500 不一定进入 `catch`

`fetch()` 在网络无法连接、请求被取消等情况下会失败。但服务器正常返回 `404` 或 `500` 时，`fetch()` 通常仍会成功得到 Response。

下面是async函数内部的错误处理不完整片段，不要单独运行；不能只写：

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

到这里能区分三种失败：无法连接时请求失败；收到 404 等响应时由 `response.ok` 检查发现；响应不是合法 JSON 时由 `response.json()` 解析发现。上面的短实验只观察请求与解析，页面使用还要补上等待上限和状态恢复，下面继续完成。

## 5. 请求超时和取消

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

## 6. 页面中的完整请求状态

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
        const controller = new AbortController();
        const timerId = setTimeout(() => controller.abort(), 5000);

        try {
          const response = await fetch(requestUrl, {
            signal: controller.signal,
          });
          if (!response.ok) {
            throw new Error(
              `文章列表读取失败：HTTP ${response.status}`,
            );
          }
          return await response.json();
        } finally {
          clearTimeout(timerId);
        }
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
| 请求超时 | 在 Network 限速或临时缩短等待上限后请求 | 显示失败，取消等待并恢复按钮 |
| 请求结束 | 成功或失败 | 按钮恢复可用 |

测试完成后，把 URL 恢复为第 3 节使用的完整地址。

先验证本节完整页面：请求期间显示加载状态，成功显示数据，失败显示错误，并且处理结束后恢复按钮。下面的 `/api/applications` 示例用于阅读请求结构，需要配套后端，不替换已经能运行的公开接口实验；应用到页面时仍要保留刚才的超时、状态检查和失败处理。

## 7. 使用查询参数发送检索条件

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

## 8. 发送数据与普通文件

请求体采用什么格式，不由前端随意决定，必须和后端接口规格中的 `Content-Type`、字段名和数据结构一致。

| 发送内容 | 常见请求体格式 | 常见 `Content-Type` |
| --- | --- | --- |
| 结构化业务数据 | JSON 字符串 | `application/json` |
| 只有简单文本字段的传统表单 | URL 编码文字 | `application/x-www-form-urlencoded` |
| 文本字段和文件 | `FormData` | `multipart/form-data; boundary=...` |
| 分片文件的单个二进制块 | `Blob` | 常见为 `application/octet-stream`，以接口规格为准 |

### 8.1 使用 POST 发送 JSON

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

这里开始使用 `fetch(url, options)` 的第二个参数；`options` 是控制发送方式的对象，不是直接交给后端的数据。其中：`method` 指定 POST，`headers` 声明请求体格式，`body` 保存 JSON 字符串。

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

### 8.2 发送 URL 编码的表单数据

部分登录接口或传统表单接口要求 `application/x-www-form-urlencoded`。可以使用 `URLSearchParams` 生成请求体：

```js
async function sendLoginForm(accountId, password) {
  const body = new URLSearchParams();
  body.set("accountId", accountId);
  body.set("password", password);

  const response = await fetch("/api/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body,
  });

  if (!response.ok) {
    throw new Error(`登录请求失败：HTTP ${response.status}`);
  }

  return response.json();
}
```

`URLSearchParams.set(name, value)` 设置一个字段。发送时，请求体类似下面经过编码的文字：

```text
accountId=yamada.taro&password=example
```

字段值会执行 URL 编码，不要自己拼接用户输入。这个示例只说明请求格式；正式登录必须使用 HTTPS，并按后端认证规格处理。不要在日志中打印密码。

### 8.3 使用 `FormData` 发送文本和文件

当接口同时接收说明文字和附件时，使用 `FormData`。下面是可直接复制的页面结构，但 `/api/applications/attachments` 必须由配套后端提供。

```html
<form id="attachmentForm">
  <label>
    申请编号
    <input name="applicationId" value="REQ-001" required>
  </label>
  <label>
    附件
    <input id="attachment" name="attachment" type="file" required>
  </label>
  <button id="uploadButton" type="submit">上传</button>
</form>
<p id="uploadStatus" aria-live="polite"></p>
<script src="upload.js" defer></script>
```

把下面代码保存为同目录的 `upload.js`：

```js
const attachmentForm = document.querySelector("#attachmentForm");
const attachmentInput = document.querySelector("#attachment");
const uploadButton = document.querySelector("#uploadButton");
const uploadStatus = document.querySelector("#uploadStatus");

attachmentForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const file = attachmentInput.files[0];

  if (file === undefined) {
    uploadStatus.textContent = "请选择文件";
    return;
  }

  const maxSize = 5 * 1024 * 1024;
  if (file.size > maxSize) {
    uploadStatus.textContent = "文件不能超过 5 MB";
    return;
  }

  const formData = new FormData(attachmentForm);
  uploadButton.disabled = true;
  uploadStatus.textContent = "上传中……";

  try {
    const response = await fetch("/api/applications/attachments", {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`文件上传失败：HTTP ${response.status}`);
    }

    const result = await response.json();
    console.log(result);
    uploadStatus.textContent = "上传完成";
  } catch (error) {
    console.error(error);
    uploadStatus.textContent = "上传失败，请稍后重试";
  } finally {
    uploadButton.disabled = false;
  }
});
```

`attachmentInput.files` 是 `FileList`，保存用户本次选择的文件；`files[0]` 是第一个 `File`。`File` 对象常用属性如下：

| 属性 | 示例 | 作用 |
| --- | --- | --- |
| `name` | `receipt.pdf` | 原始文件名 |
| `size` | `245760` | 文件大小，单位为字节 |
| `type` | `application/pdf` | 浏览器报告的 MIME 类型，可能为空且不能作为安全保证 |
| `lastModified` | 毫秒时间戳 | 文件最后修改时间 |

`new FormData(form)` 收集表单中具有 `name` 的有效字段，文件字段会作为 `File` 加入。也可以手工创建：

```js
const formData = new FormData();
formData.append("applicationId", "REQ-001");
formData.append("attachment", file);
```

`append(name, value)` 追加字段。`name` 必须与后端接口字段名一致；`value` 可以是字符串、`File` 或 `Blob`，普通数字会转成字符串。

`File` 表示用户通过文件控件选择的文件，除了二进制内容，还带有文件名、大小等信息。`Blob` 表示一段不可变的二进制数据，不一定对应用户磁盘上的完整文件；`File` 可以看作带文件信息的 `Blob`。普通上传直接使用 `File`，分片时 `slice()` 会得到 `Blob`。

使用 `FormData` 时，**不要手工设置 `Content-Type`**。浏览器会生成类似下面的请求头，并自动加入用于分隔各字段的 `boundary`：

```text
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
```

如果只手工写 `multipart/form-data`，缺少匹配的 boundary，后端可能无法拆分字段和文件。

前端的文件大小、扩展名和 `file.type` 检查只用于尽早提示用户。后端仍必须重新校验权限、大小、实际内容和文件名，并按照项目安全要求存储和扫描文件。

### 8.4 上传多个文件

HTML 文件控件添加 `multiple` 后可以选择多个文件：

```html
<input id="attachments" name="attachments" type="file" multiple>
```

读取并逐个追加：

```js
const attachmentsInput = document.querySelector("#attachments");
const formData = new FormData();

for (const file of attachmentsInput.files) {
  formData.append("attachments", file);
}

const response = await fetch("/api/applications/attachments", {
  method: "POST",
  body: formData,
});

if (!response.ok) {
  throw new Error(`多个文件上传失败：HTTP ${response.status}`);
}
```

这里多次使用同一个字段名 `attachments`。有的后端要求 `attachments[]` 或不同字段名，必须以接口设计书为准。前端还应检查文件数量和每个文件大小，后端则必须再次检查整个请求的总大小。

### 8.5 上传进度不是上传结果

原生 `fetch()` 很适合直接上传 `FormData`，但常规写法没有简单的上传进度回调。只把状态文字改成“上传中”不能得到真实百分比。

既有项目使用 Axios 时，可以通过它的 `onUploadProgress` 读取已发送字节数，具体示例见 15.4 节。底层 `XMLHttpRequest.upload` 也能报告进度，但不作为本课程主线再引入一套完整请求写法。

无论进度是否到达 100%，都要继续等待服务器响应。100% 通常只说明请求内容已经发送，不代表后端已完成校验、保存、病毒扫描和业务登记。

## 9. 大文件上传

### 9.1 普通上传为什么不一定适合大文件

把一个大文件放进 `FormData` 并不代表浏览器一定先把整个文件复制进 JavaScript 内存，但它仍然是一次完整 HTTP 请求。网络在 95% 时中断，通常要从头重新上传。请求还可能超过浏览器、反向代理、Web 服务器、应用服务器或对象存储设置的大小与超时限制。

“大文件”没有统一的 MB 数值。项目应根据接口限制、代理配置、移动网络、超时和存储方案决定。选择方案前先确认接口规格：

| 条件 | 常见方案 |
| --- | --- |
| 文件较小、失败后重传成本低 | 单次 `multipart/form-data` 上传 |
| 文件较大、需要断点续传或失败重试 | 分片上传 |
| 文件最终保存到云对象存储 | 后端签发受限上传地址，浏览器直接上传到对象存储 |

### 9.2 分片上传需要前后端共同设计

前端不能单方面把文件切开后发送到普通文件接口。后端必须提供配套协议，常见流程如下：

```text
1. 初始化上传：发送文件名、大小、类型
2. 后端返回 uploadId 和允许的分片大小
3. 前端使用 file.slice() 切出各分片
4. 逐片上传，并记录成功的分片编号
5. 失败分片按有限次数重试
6. 全部分片成功后，请求后端合并或完成上传
7. 用户取消时，中止请求并通知后端清理临时分片
```

`uploadId` 用于区分一次上传任务，分片编号用于确定顺序。后端还应验证分片归属、数量、大小和完整性，不能只按前端提供的文件名直接合并。

### 9.3 使用 `slice()` 生成分片

下面只观察切片结果，不发送网络请求：

```js
const PART_SIZE = 5 * 1024 * 1024;

function createFileParts(file) {
  const parts = [];

  for (let start = 0; start < file.size; start += PART_SIZE) {
    const end = Math.min(start + PART_SIZE, file.size);
    parts.push(file.slice(start, end));
  }

  return parts;
}
```

`file.slice(start, end)` 返回一个表示原文件部分内容的 `Blob`，范围包含 `start`，不包含 `end`，单位都是字节。它不会修改原文件。

例如文件大小为 12 MB、分片大小为 5 MB 时，得到三片：5 MB、5 MB、2 MB。

### 9.4 分片上传的前端代码骨架

下面假定后端已经约定三个接口，属于需要配套后端才能运行的接口代码：

| 步骤 | 示例接口 | 请求和响应 |
| --- | --- | --- |
| 初始化 | `POST /api/uploads` | 发送文件信息，返回 `uploadId` |
| 上传一片 | `PUT /api/uploads/{uploadId}/parts/{partNumber}` | 请求体是一个 `Blob` |
| 完成 | `POST /api/uploads/{uploadId}/complete` | 发送总分片数，后端校验并合并，返回上传结果 JSON |

```js
const PART_SIZE = 5 * 1024 * 1024;

async function uploadLargeFile(file, signal, onProgress) {
  const initializeResponse = await fetch("/api/uploads", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      fileName: file.name,
      fileSize: file.size,
      contentType: file.type,
    }),
    signal,
  });

  if (!initializeResponse.ok) {
    throw new Error(`上传初始化失败：HTTP ${initializeResponse.status}`);
  }

  const initializeResult = await initializeResponse.json();
  const uploadId = initializeResult.uploadId;

  if (typeof uploadId !== "string" || uploadId === "") {
    throw new Error("上传初始化响应中缺少 uploadId");
  }

  const partCount = Math.ceil(file.size / PART_SIZE);
  let uploadedBytes = 0;

  for (let partIndex = 0; partIndex < partCount; partIndex += 1) {
    const start = partIndex * PART_SIZE;
    const end = Math.min(start + PART_SIZE, file.size);
    const part = file.slice(start, end);
    const partNumber = partIndex + 1;

    const partResponse = await fetch(
      `/api/uploads/${encodeURIComponent(uploadId)}/parts/${partNumber}`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/octet-stream",
        },
        body: part,
        signal,
      },
    );

    if (!partResponse.ok) {
      throw new Error(
        `第 ${partNumber} 片上传失败：HTTP ${partResponse.status}`,
      );
    }

    uploadedBytes += part.size;
    onProgress(uploadedBytes, file.size);
  }

  const completeResponse = await fetch(
    `/api/uploads/${encodeURIComponent(uploadId)}/complete`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ partCount }),
      signal,
    },
  );

  if (!completeResponse.ok) {
    throw new Error(`完成上传失败：HTTP ${completeResponse.status}`);
  }

  return completeResponse.json();
}
```

`uploadLargeFile(file, signal, onProgress)` 接收文件、取消信号和进度回调，按顺序上传每一片。当前进度在每个分片成功后更新，不表示单个分片内部的实时进度。

`encodeURIComponent(value)` 把值编码成能够安全放进 URL 路径片段的文字。例如空格会变成 `%20`。即使 `uploadId` 通常由后端生成，也不应未经编码直接拼进 URL。它只处理 URL 编码，不负责检查访问权限。

调用示例：

```js
const largeFileInput = document.querySelector("#largeFile");
const controller = new AbortController();
const selectedFile = largeFileInput.files[0];

if (selectedFile === undefined) {
  console.log("请选择大文件");
} else {
  uploadLargeFile(
    selectedFile,
    controller.signal,
    (uploadedBytes, totalBytes) => {
      const percent = Math.round((uploadedBytes / totalBytes) * 100);
      console.log(`大文件上传进度：${percent}%`);
    },
  ).catch((error) => {
    if (error.name === "AbortError") {
      console.log("用户取消了上传");
      return;
    }

    console.error("大文件上传失败", error);
  });
}
```

这个调用示例要求 HTML 中存在 `<input id="largeFile" type="file">`。`Math.round(value)` 把数字四舍五入到最接近的整数，因此页面可以显示整数百分比。调用 `controller.abort()` 可以停止当前和后续共用该信号的请求，但已经上传到服务器的分片不会自动删除；清理方式必须由后端接口规定。

这个基础版本采用顺序上传，便于理解和控制服务器压力。正式项目通常还要补充：

- 查询已经成功的分片，实现续传；
- 对网络错误和可重试状态码进行有限次数重试；
- 使用校验值确认文件完整性；
- 限制并发数，而不是一次发出所有分片；
- 上传任务过期和临时文件清理；
- 取消、失败和完成接口的幂等性。

这些能力必须与后端和存储服务一起设计，不能只复制前端循环。

空文件、单文件最大大小、允许类型和单次上传有效期同样应写入接口规格。前端可以提前提示，后端必须作最终判定。

### 9.5 使用对象存储直传：了解

大文件最终保存在 Amazon S3 等对象存储时，常见方式不是让文件内容全部经过业务服务器：

```text
浏览器向业务后端申请受限上传地址
→ 后端检查用户权限并返回短期有效地址
→ 浏览器把文件或分片直接上传到对象存储
→ 浏览器通知后端上传完成
→ 后端验证对象并登记业务记录
```

上传地址必须限制有效期、对象位置、大小和允许的操作。前端不能保存云服务永久密钥。具体签名方式和分片协议属于后端、云服务与项目基础设施的共同设计。

## 10. 按需求选择 options 配置

本节的短代码均为 `async` 函数内部的配置片段，用来对比选项，不是完整页面脚本。将选项用于第 6 节的完整页面时，保留函数外壳、超时及失败处理；不要把带await的片段直接粘贴到普通app.js的顶层。涉及 `/api/applications` 的代码还需要对应后端接口。

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

### 10.1 `method`、`headers` 和 `body`

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

查询参数不属于 `options`。GET 的检索条件仍然写在 URL 中，见第 7 节的 `URLSearchParams` 示例。

### 10.2 `credentials`

`credentials` 控制浏览器是否在请求中携带 Cookie 等凭据：

| 值 | 含义 |
| --- | --- |
| `omit` | 不发送凭据，也忽略响应中用于设置凭据的信息 |
| `same-origin` | 只在同源请求中使用凭据，默认值 |
| `include` | 同源和跨来源请求都尝试使用凭据 |

跨来源请求使用 `include` 时，后端还必须返回允许指定来源和凭据的 CORS 响应头，Cookie 本身也会受到 SameSite 等规则限制。仅修改前端选项不能绕过服务器限制。

### 10.3 `signal`

```js
const controller = new AbortController();

const response = await fetch("/api/applications", {
  signal: controller.signal,
});
```

`signal` 本身不会自动取消请求。其他代码调用 `controller.abort()` 后，信号才会通知 `fetch()` 停止请求。第 5 节已经用它实现超时。

### 10.4 `mode`、`cache` 和 `redirect`

这三个属性在有明确项目需求时再设置：

- 普通跨来源接口请求通常保持 `mode: "cors"`。
- `mode: "same-origin"` 会阻止向其他来源发送请求。
- 不要使用 `mode: "no-cors"` 解决 CORS 错误；得到的通常是不允许读取状态和响应体的 opaque 响应。
- `cache: "no-store"` 表示不使用也不保存 HTTP 缓存，适合明确要求每次获取最新结果的场景。
- `cache: "no-cache"` 并不等于完全不用缓存，而是要求先向服务器验证缓存是否仍然有效。
- `redirect: "follow"` 会自动跟随重定向，也是默认行为。

大多数业务请求只需要 `method`、`headers`、`body` 和必要的 `signal`。不要为了显得配置完整而机械填写所有属性。

## 11. PUT、PATCH 和 DELETE 的基本结构

### 11.1 PATCH：修改部分字段

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

### 11.2 DELETE：删除数据

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

## 12. CORS 是什么

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

## 13. 使用 Network 面板排查请求

打开浏览器开发者工具的 Network 面板，重新执行请求，重点查看：

| 检查项 | 要确认的内容 |
| --- | --- |
| Request URL | 地址、路径和查询参数是否正确 |
| Request Method | 是否使用接口要求的 GET、POST 等方法 |
| Status Code | 是成功、前端请求错误还是后端错误 |
| Request Headers | Content-Type、认证信息是否符合规格 |
| Request Payload | JSON、表单字段、文件字段和分片编号是否符合规格 |
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

## 14. 常见错误

### 14.1 忘记检查 `response.ok`

症状：服务器返回 404 或 500，但代码仍继续解析或渲染。

修正：在读取响应体前检查 `response.ok`，不成功时抛出包含状态码的错误。

### 14.2 忘记等待 `response.json()`

```js
const data = response.json();
console.log(data); // Promise，不是最终数据
```

修正为：

```js
const data = await response.json();
```

### 14.3 GET 请求错误地设置 `body`

GET 查询条件通常放在 URL 查询参数中。使用 `URLSearchParams`，并遵守后端接口规格。

### 14.4 POST 直接发送普通对象

```js
body: application // 错误：普通对象不能直接作为 JSON 请求体
```

应设置 JSON 请求头，并写成：

```js
body: JSON.stringify(application)
```

### 14.5 对 204 响应调用 `json()`

204 没有响应体，继续解析 JSON 可能报错。根据状态码和接口规格决定是否读取响应体。

### 14.6 上传 `FormData` 时手工设置 `Content-Type`

症状：Network 中能看到请求，但后端报告缺少文件或无法解析 multipart 数据。

原因：手工写了 `Content-Type: multipart/form-data`，却没有浏览器生成的正确 boundary。

修正：把 `FormData` 直接放入 `body`，删除手工设置的 `Content-Type`，让浏览器自动生成请求头。

### 14.7 只在前端校验文件

前端的 `accept`、扩展名、`file.type` 和文件大小检查都可以被绕过，也可能与文件实际内容不一致。它们用于改善操作体验，不能代替后端的权限、大小、类型、内容和恶意文件检查。

### 14.8 把普通上传直接当成大文件方案

症状：文件接近完成时失败，重试又从零开始；或服务器返回 `413 Payload Too Large`、超时等错误。

修正：先确认各层限制。确实需要大文件、续传或分片重试时，与后端共同设计初始化、分片、完成、取消和清理接口，不只在前端增大超时时间。

## 15. Axios 基础使用

Axios 是基于 Promise 的 HTTP 客户端。它不是 JavaScript 内置功能，需要先安装或由页面加载。零基础阶段只要求会发送常见请求、读取响应数据并处理失败；实例、拦截器和认证封装应在具体框架或项目课程中继续学习。

### 15.1 在本章练习页面中引入

本章还没有进入构建工具，练习页面先使用CDN脚本。把Axios放在自己的`app.js`之前加载：

```html
<script src="https://cdn.jsdelivr.net/npm/axios@1/dist/axios.min.js"></script>
<script src="./js/app.js" defer></script>
```

第一个`<script>`加载Axios，并提供全局变量`axios`；第二个加载自己的页面脚本。这个示例需要网络连接。

本章统一采用上述CDN引入方式。构建项目中的依赖安装与模块导入见[第22章](22_modules_script_organization.md)，不要混用两种环境的代码。

### 15.2 发送 GET 请求

```js
async function loadApplications() {
  try {
    const response = await axios.get("/api/applications", {
      params: { status: "pending" },
      timeout: 5000
    });

    console.log(response.data);
  } catch (error) {
    console.error("申请列表读取失败", error);
  }
}
```

- `axios.get(url, config?)` 发送GET请求；
- `params` 把对象转换为URL查询参数；
- `timeout` 指定等待的毫秒数；
- `response.data` 是响应正文；
- Axios会把超出默认成功范围的HTTP状态作为失败交给`catch`。

### 15.3 发送 POST 请求

```js
async function createApplication(application) {
  const response = await axios.post(
    "/api/applications",
    application,
    { timeout: 5000 }
  );

  return response.data;
}
```

`axios.post(url, data?, config?)` 的第二个参数是请求数据，第三个参数是配置对象。传入普通对象时，Axios通常会按JSON请求处理。

### 15.4 上传文件并显示进度

Axios 的浏览器请求配置提供 `onUploadProgress`，适合既有 Axios 项目需要显示普通文件上传进度的情况：

```js
async function uploadAttachment(file, applicationId, signal) {
  const formData = new FormData();
  formData.append("applicationId", applicationId);
  formData.append("attachment", file);

  const response = await axios.post(
    "/api/applications/attachments",
    formData,
    {
      signal,
      timeout: 30000,
      onUploadProgress(progressEvent) {
        if (progressEvent.total === undefined) {
          console.log(`已发送 ${progressEvent.loaded} 字节`);
          return;
        }

        const percent = Math.round(
          (progressEvent.loaded / progressEvent.total) * 100,
        );
        console.log(`上传进度：${percent}%`);
      },
    },
  );

  return response.data;
}
```

| 配置 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `signal` | `AbortSignal` | 可选 | 配合 `AbortController` 取消上传 |
| `timeout` | 非负毫秒数 | 可选；示例为 30 秒 | 超过等待时间时中止请求 |
| `onUploadProgress` | 接收进度对象的函数 | 可选 | 上传过程中读取 `loaded` 和可能存在的 `total` |

`loaded` 是已经发送的字节数；只有 `total` 可用时才能可靠计算百分比。不要为 `FormData` 手工设置 `Content-Type`，最终成功仍以服务器响应为准。

### 15.5 识别 Axios 错误

```js
async function inspectAxiosError() {
  try {
    const response = await axios.get(
      "https://jsonplaceholder.typicode.com/unknown-path",
      { timeout: 5000 }
    );
    console.log(response.data);
  } catch (error) {
    if (axios.isAxiosError(error)) {
      if (error.response !== undefined) {
        console.error("HTTP状态：", error.response.status);
      } else {
        console.error("未收到服务器响应");
      }
      console.error(error.message);
    } else {
      console.error("未知错误", error);
    }
  }
}

inspectAxiosError();
```

这段代码在已加载Axios的普通app.js中运行，不使用顶层await。测试URL用于观察404响应；网络不可达或超时时应显示未收到响应，不保证每次都得到404。

`axios.isAxiosError(error)` 判断捕获值是否是Axios错误。`error.response` 表示服务器返回了响应；没有响应时还可能是网络、超时或取消问题。页面仍应分别处理加载、成功、空数据和失败状态。

### 15.6 fetch 与 Axios 如何选择

| 场景 | 建议 |
| --- | --- |
| 不增加依赖，使用浏览器标准API | 使用`fetch()` |
| 既有项目已经统一使用Axios | 遵守项目约定使用Axios |
| 只需要发送一个简单请求 | 两者都可以，优先保持项目一致 |
| 需要统一客户端、拦截器和认证处理 | 在项目或框架课程中建立Axios接口层 |

不要在同一功能中无理由混用两套请求方式。

## 16. 本章练习

### 16.1 初始文件

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

### 16.2 任务要求

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
12. 新建一个独立附件上传页面，使 HTML 的文件字段与 JavaScript 选择器一一对应；请求地址使用课程中的占位接口，不要求没有后端时伪造成功结果。
13. 使用 `FormData` 同时加入申请编号和一个文件，确认代码没有手工设置 `Content-Type`。
14. 加入未选择文件、单文件超过 5 MB 两种前端提示，并说明后端仍需重复校验。
15. 写出 12 MB 文件按 5 MB 分片时的分片数量和每片大小，再说明完成分片上传至少需要哪些后端接口。

### 16.3 完成标准

- 能解释 `fetch()` 与 `response.json()` 为什么都需要等待。
- 能说明为什么 404 不一定自动进入 `catch`。
- 能说明 `options` 中常用属性的作用以及 `credentials` 的三个取值。
- 能区分查询参数和 JSON 请求体。
- 能根据接口规格选择 JSON、URL 编码表单或 `FormData`。
- 能使用 `FileList`、`File` 和 `FormData` 组织普通文件上传请求。
- 能说明为什么不能手工设置 multipart 的 `Content-Type`。
- 能说明上传进度、HTTP 成功和后端业务处理完成并不是同一件事。
- 能说明大文件分片上传需要初始化、分片、完成和清理等后端能力。
- 页面覆盖加载中、成功、空数据和失败状态。
- 能根据 Network 信息判断问题发生在请求、响应还是页面处理阶段。

## 17. 参考资料

- [MDN：使用 FormData 对象](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest_API/Using_FormData_Objects)
- [MDN：使用 Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch)
- [MDN：XMLHttpRequest 上传进度](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/upload)
- [Axios：请求配置](https://axios-http.com/docs/req_config)
- [Axios：multipart/form-data](https://axios-http.com/docs/multipart)
