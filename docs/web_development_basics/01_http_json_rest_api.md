# 第1章 HTTP、JSON 与 REST API

> 本章是 Python Web、Java Web、前端开发和接口自动化课程的共同基础。重点不是背诵名词，而是能够读懂一次请求和响应，判断数据放在哪里，并看懂企业项目中的接口设计书。

## 一、学习目标

完成本章后，学员应能够：

- 说明 HTTP 在 Web 系统中的作用
- 按顺序描述浏览器访问后端接口时发生的主要过程
- 拆解一个完整 URL，并说明各部分的作用
- 读懂 HTTP 请求行、请求头和请求体
- 读懂 HTTP 状态行、响应头和响应体
- 根据业务目的选择 `GET`、`POST`、`PUT`、`PATCH` 或 `DELETE`
- 区分 Path、Query、Header、Cookie 和 Body 中的数据
- 说明常见状态码的含义，并区分 `401` 与 `403`
- 编写合法的 JSON，并说明 JSON 与程序对象之间的转换
- 按照基础 REST 规则设计资源 URL
- 区分 Cookie、Session 和 Token 的基本作用
- 使用浏览器开发者工具、`curl`、Postman 或 Swagger UI 查看接口
- 阅读接口设计书并整理基本接口测试观点

## 二、前置知识与难度

学习本章前，建议先完成[Web 开发基础概念](00_web_development_basics.md)，理解前端、后端、数据库和请求响应的基本关系。

本章难度分为：

- **必须掌握**：URL、请求、响应、HTTP 方法、参数位置、状态码、JSON
- **会使用、能看懂**：REST API、Cookie、Session、Token、接口调试工具
- **后续了解**：DNS、TLS、HTTP/2、HTTP/3、缓存、反向代理、负载均衡和 CORS 的深入机制

## 三、为什么 Web 开发必须学习 HTTP

HTTP 是浏览器、前端程序、后端服务以及不同系统之间传递信息时常用的应用层协议。

可以把一次 HTTP 通信理解为：

```text
客户端提出请求：我想查询编号为 1001 的用户
服务器返回响应：查询成功，这是用户数据
```

无论后端使用 Python、Java、C# 还是其他语言，都会处理以下内容：

- 请求访问哪个地址
- 请求要执行什么操作
- 请求携带什么参数
- 当前用户是否已经登录
- 服务器是否处理成功
- 返回的是 JSON、HTML、图片还是文件

如果不理解 HTTP，学习框架时容易只会照着代码写，却不知道参数为什么放在路径、查询字符串、Header 或 Body 中，也难以调查接口联调问题。

## 四、一次 HTTP 请求经历了什么

假设用户在浏览器中访问：

```text
https://example.com/api/users/1001
```

主要过程如下：

1. 浏览器读取 URL，确认使用 HTTPS 访问 `example.com`。
2. DNS 将域名 `example.com` 查询为服务器可以连接的 IP 地址。
3. 浏览器与服务器建立网络连接。
4. 使用 HTTPS 时，浏览器与服务器建立 TLS 加密通信。
5. 浏览器发送 HTTP 请求。
6. Web 服务器或反向代理接收请求，并将业务请求转给应用程序。
7. 应用程序执行参数校验、权限判断、业务处理和数据库查询。
8. 应用程序生成 HTTP 响应。
9. 浏览器接收响应，根据状态码和响应内容更新页面。

概念流程如下：

```text
浏览器
  ↓ 查询域名
DNS
  ↓ 返回 IP 地址
浏览器
  ↓ 建立连接，HTTPS 还会建立 TLS 加密通信
Web 服务器 / 反向代理
  ↓ 转发业务请求
应用服务器
  ↓ 执行业务并访问数据
数据库 / 文件 / 外部 API
  ↑ 返回处理结果
应用服务器
  ↑ HTTP 响应
浏览器
```

这里需要先区分几个概念：

| 概念 | 主要作用 |
| --- | --- |
| DNS | 将域名查询为 IP 地址 |
| HTTP | 规定客户端和服务器如何组织请求与响应 |
| HTTPS | 在 HTTP 通信外增加 TLS 加密和身份验证 |
| Web 服务器 | 接收网络请求、提供静态资源或转发请求，例如 Nginx |
| 应用服务器 | 运行 Python、Java 等后端程序并处理业务 |
| API | 系统对外提供的调用入口和数据约定 |

> 本章中的 HTTP 报文使用便于教学的 HTTP/1.1 文本形式。HTTP/2 和 HTTP/3 的底层传输形式不同，但请求方法、URL、Header、状态码和 Body 等核心语义仍然存在。

## 五、URL 详解

### 5.1 URL 是什么

URL（Uniform Resource Locator）用于表示网络资源的位置。浏览器页面、图片、文件和后端接口都可以有自己的 URL。

观察下面的地址：

```text
https://api.example.com:8443/api/users/1001?include=orders&lang=ja#profile
```

可以拆分为：

| 部分 | 示例 | 作用 |
| --- | --- | --- |
| 协议 | `https` | 说明使用哪种通信方式 |
| 主机名 | `api.example.com` | 指定要访问的服务器 |
| 端口 | `8443` | 指定服务器上的网络服务入口 |
| 路径 | `/api/users/1001` | 指定要访问的资源 |
| 查询字符串 | `include=orders&lang=ja` | 追加筛选、分页或显示选项 |
| 片段 | `profile` | 定位页面内部位置，通常不会发送给服务器 |

### 5.2 协议与默认端口

常见情况：

| 协议 | 常见默认端口 | 说明 |
| --- | ---: | --- |
| HTTP | `80` | 通信内容默认不加密 |
| HTTPS | `443` | 通过 TLS 加密通信 |

使用默认端口时，URL 通常省略端口：

```text
https://example.com/users
```

学习环境中常见 `8000`、`8080`、`3000`、`5173` 等开发端口。这些是应用或开发工具选择的端口，不是 HTTP 或 HTTPS 的固定端口。

### 5.3 路径参数

路径参数用于标识一个具体资源：

```text
/api/users/1001
```

这里的 `1001` 表示用户编号。

常见形式：

```text
/api/orders/5001
/api/products/ABC-001
/api/users/1001/orders
```

### 5.4 查询参数

查询参数位于 `?` 后面，多个参数使用 `&` 分隔：

```text
/api/users?page=2&size=20&status=active
```

表示：

- `page=2`：查询第 2 页
- `size=20`：每页 20 条
- `status=active`：只查询有效用户

查询参数常用于筛选、排序、分页和可选条件。

### 5.5 URL 编码

URL 不能直接安全表示所有字符。空格、中文、日文和某些特殊符号在传输时通常会进行百分号编码。

例如，“東京”可能被编码为：

```text
%E6%9D%B1%E4%BA%AC
```

实际开发中通常由浏览器、HTTP 客户端或框架完成编码与解码，不建议手工拼接复杂 URL。

### 5.6 URL 中不要放敏感信息

以下写法不合适：

```text
/api/login?password=Secret123
```

URL 可能出现在浏览器历史、服务器日志、代理日志和监控记录中。密码、访问令牌和其他敏感数据不应放在 URL 查询参数中。

## 六、HTTP 请求报文

### 6.1 请求报文的组成

一个 HTTP 请求在逻辑上包含：

1. 请求行
2. 请求头（Request Headers）
3. 空行
4. 请求体（Request Body，可选）

下面是一个创建用户的概念报文：

```http
POST /api/users HTTP/1.1
Host: api.example.com
Content-Type: application/json
Accept: application/json
Authorization: Bearer example-token

{
  "userName": "tanaka",
  "email": "tanaka@example.com"
}
```

> 这是用于理解报文结构的概念示例，不包含真实令牌，也不是要求学员直接执行的命令。

### 6.2 请求行

```http
POST /api/users HTTP/1.1
```

请求行包含：

| 内容 | 示例 | 含义 |
| --- | --- | --- |
| HTTP 方法 | `POST` | 希望服务器执行的操作类型 |
| 请求目标 | `/api/users` | 要访问的资源路径 |
| 协议版本 | `HTTP/1.1` | 当前示例使用的 HTTP 版本 |

### 6.3 请求头

请求头用于传递请求的附加信息。

常见请求头：

| Header | 示例 | 作用 |
| --- | --- | --- |
| `Host` | `api.example.com` | 指定目标主机，HTTP/1.1 请求中很重要 |
| `Content-Type` | `application/json` | 说明请求体的数据格式 |
| `Accept` | `application/json` | 说明客户端希望接收的格式 |
| `Authorization` | `Bearer ...` | 携带认证凭据 |
| `Cookie` | `sessionId=...` | 将浏览器保存的 Cookie 发给服务器 |
| `User-Agent` | 浏览器或客户端信息 | 说明请求由什么客户端发出 |
| `Accept-Language` | `ja-JP` | 表示客户端偏好的语言 |

`Content-Type` 与 `Accept` 容易混淆：

- `Content-Type`：我发送的 Body 是什么格式
- `Accept`：我希望服务器返回什么格式

### 6.4 空行

请求头结束后有一个空行，用于分隔 Header 与 Body。阅读原始报文时，不要忽略这个分隔位置。

### 6.5 请求体

请求体用于携带需要提交给服务器的数据，例如：

- JSON
- 表单数据
- 上传文件
- 二进制数据

常见 `Content-Type`：

| Content-Type | 常见用途 |
| --- | --- |
| `application/json` | 提交 JSON 数据 |
| `application/x-www-form-urlencoded` | 提交普通 HTML 表单 |
| `multipart/form-data` | 上传文件或同时提交文件和字段 |
| `text/plain` | 提交纯文本 |

不是所有请求都有 Body。普通查询请求通常通过路径参数和查询参数表达条件。

## 七、HTTP 响应报文

### 7.1 响应报文的组成

一个 HTTP 响应在逻辑上包含：

1. 状态行
2. 响应头（Response Headers）
3. 空行
4. 响应体（Response Body，可选）

```http
HTTP/1.1 201 Created
Content-Type: application/json; charset=utf-8
Location: /api/users/1001

{
  "id": 1001,
  "userName": "tanaka",
  "email": "tanaka@example.com"
}
```

### 7.2 状态行

```http
HTTP/1.1 201 Created
```

状态行包含：

- HTTP 版本：`HTTP/1.1`
- 状态码：`201`
- 原因短语：`Created`

程序判断结果时主要使用状态码，不应依赖原因短语或错误消息中的自然语言。

### 7.3 响应头

常见响应头：

| Header | 作用 |
| --- | --- |
| `Content-Type` | 说明响应体的数据格式和字符编码 |
| `Content-Length` | 表示响应体的字节长度，某些传输方式下可能没有 |
| `Location` | 表示新资源地址或重定向地址 |
| `Set-Cookie` | 要求浏览器保存 Cookie |
| `Cache-Control` | 控制客户端或中间节点如何缓存 |
| `Access-Control-Allow-Origin` | CORS 处理中允许访问的来源之一 |

### 7.4 响应体

响应体可能是：

- HTML 页面
- JSON 数据
- 图片或 PDF
- CSV 或 Excel 文件
- 错误信息

`204 No Content` 表示请求处理成功，但响应不包含 Body。客户端不应继续期待 JSON 内容。

## 八、常见 HTTP 方法

HTTP 方法表示客户端希望对资源执行什么操作。

| 方法 | 常见用途 | 示例 | 是否通常有 Body |
| --- | --- | --- | --- |
| `GET` | 查询资源 | `GET /api/users/1001` | 通常没有 |
| `POST` | 创建资源或提交处理 | `POST /api/users` | 通常有 |
| `PUT` | 整体替换或按项目约定更新资源 | `PUT /api/users/1001` | 通常有 |
| `PATCH` | 部分更新资源 | `PATCH /api/users/1001` | 通常有 |
| `DELETE` | 删除资源 | `DELETE /api/users/1001` | 通常没有 |

### 8.1 GET：查询数据

```http
GET /api/users/1001 HTTP/1.1
Host: api.example.com
Accept: application/json
```

`GET` 应用于读取数据，不应因为“调用方便”而用来修改数据库状态。

### 8.2 POST：创建或提交处理

```http
POST /api/users HTTP/1.1
Host: api.example.com
Content-Type: application/json

{
  "userName": "tanaka"
}
```

创建成功常返回 `201 Created`。具体返回结构应遵循项目接口规范。

### 8.3 PUT 与 PATCH

假设原用户数据是：

```json
{
  "userName": "tanaka",
  "email": "tanaka@example.com",
  "status": "active"
}
```

`PUT` 常用于提交资源的完整表示：

```json
{
  "userName": "tanaka",
  "email": "new-tanaka@example.com",
  "status": "active"
}
```

`PATCH` 常用于只提交需要修改的字段：

```json
{
  "email": "new-tanaka@example.com"
}
```

不同项目对 `PUT` 的更新语义可能有具体约定，开发前应以接口设计书为准。

### 8.4 DELETE：删除资源

```http
DELETE /api/users/1001 HTTP/1.1
Host: api.example.com
Authorization: Bearer example-token
```

删除成功可能返回 `204 No Content`，也可能按照项目统一格式返回 `200 OK`。不要只凭个人习惯决定，应遵循团队约定。

### 8.5 安全方法与幂等性

- **安全方法**：调用的目的不是修改服务器状态，例如 `GET`。
- **幂等**：同一个请求执行一次或多次，预期最终状态相同。

基础理解：

| 方法 | 安全 | 通常幂等 |
| --- | --- | --- |
| `GET` | 是 | 是 |
| `POST` | 否 | 通常不是 |
| `PUT` | 否 | 是 |
| `PATCH` | 否 | 不一定 |
| `DELETE` | 否 | 是 |

“幂等”不代表每次响应完全相同，也不代表服务器内部没有日志等附带变化。它关注的是目标资源的最终状态。

## 九、参数应该放在哪里

以用户管理接口为例：

| 位置 | 示例 | 适合保存的内容 |
| --- | --- | --- |
| Path | `/api/users/1001` | 标识具体资源的必要信息 |
| Query | `?page=2&status=active` | 筛选、排序、分页和可选条件 |
| Header | `Authorization: Bearer ...` | 认证信息、内容格式、客户端元数据 |
| Cookie | `sessionId=...` | 浏览器自动携带的会话标识等信息 |
| Body | `{"userName":"tanaka"}` | 创建或修改时提交的结构化数据 |

### 9.1 Path 与 Query 的区别

```text
GET /api/users/1001
```

表示“查询编号为 1001 的用户”，没有用户编号就无法定位这个资源，因此使用 Path。

```text
GET /api/users?status=active
```

表示“查询用户列表，并选择只看有效用户”。即使没有 `status`，仍然可以查询列表，因此适合使用 Query。

### 9.2 Header 与 Body 的区别

- Header 描述请求本身，例如认证方式、发送格式和期望格式。
- Body 保存本次业务提交的数据，例如用户姓名、邮箱和订单明细。

不要把大段业务数据拆成大量自定义 Header。接口设计应让数据位置符合其职责。

## 十、HTTP 状态码

状态码由三位数字组成，第一位表示大类。

| 范围 | 分类 | 含义 |
| --- | --- | --- |
| `1xx` | 信息响应 | 请求正在继续处理，初学阶段了解即可 |
| `2xx` | 成功 | 请求已被正确接收和处理 |
| `3xx` | 重定向 | 客户端需要访问其他位置或使用缓存 |
| `4xx` | 客户端错误 | 请求数据、认证或权限等存在问题 |
| `5xx` | 服务器错误 | 服务器处理过程中发生问题 |

### 10.1 常见成功状态码

| 状态码 | 名称 | 常见场景 |
| --- | --- | --- |
| `200` | OK | 查询、修改或普通处理成功 |
| `201` | Created | 成功创建资源 |
| `204` | No Content | 处理成功且不返回响应体 |

### 10.2 常见客户端错误状态码

| 状态码 | 名称 | 常见场景 |
| --- | --- | --- |
| `400` | Bad Request | 请求格式错误或无法正确解析 |
| `401` | Unauthorized | 尚未通过身份认证，通常需要登录或提供有效凭据 |
| `403` | Forbidden | 身份已确认，但没有执行该操作的权限 |
| `404` | Not Found | 请求的资源或路径不存在 |
| `405` | Method Not Allowed | 路径存在，但不支持当前 HTTP 方法 |
| `409` | Conflict | 请求与当前资源状态冲突，例如账号重复 |
| `415` | Unsupported Media Type | 请求体格式不受支持，例如要求 JSON 却发送其他格式 |
| `422` | Unprocessable Content | 请求格式可识别，但内容无法按要求处理；是否使用取决于框架和项目规范 |
| `429` | Too Many Requests | 请求过于频繁，被限流 |

### 10.3 常见服务器错误状态码

| 状态码 | 名称 | 常见场景 |
| --- | --- | --- |
| `500` | Internal Server Error | 服务器内部发生未正确处理的异常 |
| `502` | Bad Gateway | 网关或代理没有从上游服务取得有效响应 |
| `503` | Service Unavailable | 服务暂时不可用或正在维护 |
| `504` | Gateway Timeout | 网关等待上游服务响应超时 |

### 10.4 401 与 403 的区别

```text
401：不知道你是谁，或者你的登录凭据无效
403：知道你是谁，但你没有权限
```

例如：

- 未登录访问员工信息接口：`401`
- 普通员工登录后访问管理员接口：`403`

### 10.5 不要全部返回 200

下面的响应容易给调用方造成误解：

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": false,
  "message": "用户不存在"
}
```

如果资源确实不存在，通常更适合返回 `404`。调用方可以先根据 HTTP 状态码判断处理大类，再读取响应体中的业务错误信息。

某些既有系统会统一返回 `200` 并使用业务代码表示结果。维护项目时应先遵循现有接口规范，不能在没有影响分析的情况下单独改变返回规则。

## 十一、JSON 详解

### 11.1 JSON 是什么

JSON（JavaScript Object Notation）是一种文本数据格式，经常用于前端与后端、服务与服务之间交换结构化数据。

JSON 只是数据格式，不是编程语言，也不是数据库。

### 11.2 JSON 的基本数据类型

| JSON 类型 | 示例 | 说明 |
| --- | --- | --- |
| Object | `{"id": 1001}` | 使用键值对表示一个对象 |
| Array | `["admin", "user"]` | 按顺序保存多个值 |
| String | `"Tanaka"` | 字符串必须使用双引号 |
| Number | `1001`、`12.5` | 数字不加引号 |
| Boolean | `true`、`false` | 使用小写 |
| Null | `null` | 表示空值 |

### 11.3 一个完整 JSON 示例

```json
{
  "id": 1001,
  "userName": "tanaka",
  "email": "tanaka@example.com",
  "age": 28,
  "active": true,
  "department": null,
  "roles": ["user", "report_viewer"],
  "address": {
    "postalCode": "100-0001",
    "city": "Tokyo"
  }
}
```

结构说明：

- 最外层是 Object。
- `roles` 的值是 Array。
- `address` 的值是嵌套 Object。
- `active` 是 Boolean，不是字符串。
- `department` 当前没有值，因此是 `null`。

### 11.4 JSON 与常见语言类型的对应

| JSON | Python 常见类型 | Java 常见类型 | JavaScript / TypeScript |
| --- | --- | --- | --- |
| Object | `dict` | DTO、`Map` 或对象 | object |
| Array | `list` | `List` 或数组 | Array |
| String | `str` | `String` | string |
| Number | `int`、`float` | `Integer`、`Long`、`Double` 等 | number |
| Boolean | `bool` | `boolean`、`Boolean` | boolean |
| Null | `None` | `null` | null |

具体框架会根据类型声明和配置执行转换，不能认为 JSON 数字一定自动对应某一种固定 Java 数字类型。

### 11.5 序列化与反序列化

- **序列化**：将程序中的对象转换为 JSON 文本或 JSON 数据。
- **反序列化**：将收到的 JSON 转换为程序可以操作的对象。

```text
程序对象 ──序列化──> JSON
程序对象 <──反序列化── JSON
```

Python、Spring Boot、Vue 和 React 使用的具体 API 不同，但都需要经历数据格式转换。

### 11.6 JSON 语法规则

- 属性名必须使用双引号。
- 字符串必须使用双引号。
- 多个项目之间使用逗号分隔。
- 最后一个项目后面不能添加多余逗号。
- 布尔值写作 `true` 和 `false`。
- 空值写作 `null`。
- JSON 本身不支持注释。

### 11.7 常见 JSON 错误

错误写法：

```json
{
  'userName': 'tanaka',
  "active": True,
}
```

问题包括：

- 使用了单引号。
- 使用了 Python 写法 `True`。
- 最后一个字段后有多余逗号。

修正后：

```json
{
  "userName": "tanaka",
  "active": true
}
```

### 11.8 日期和大整数

JSON 没有专门的日期类型，接口通常用约定格式的字符串表示日期时间，例如：

```json
{
  "createdAt": "2026-07-15T09:30:00+09:00"
}
```

日期格式、时区和精度必须在接口规范中明确。

非常大的整数传给 JavaScript 时可能出现精度问题。跨系统传递长编号时，项目可能约定使用字符串：

```json
{
  "orderId": "9007199254740993"
}
```

是否使用字符串应以接口约定为准，不要由前后端各自猜测。

## 十二、REST API 基础

### 12.1 REST API 是什么

REST 是一种常见的 Web 接口设计风格。REST API 通常围绕“资源”设计 URL，并使用 HTTP 方法表达操作。

例如，用户是资源：

```text
/api/users
/api/users/1001
```

REST 不是某个 Python 或 Java 框架，也不是所有项目都必须完全采用的唯一标准。

### 12.2 使用名词表示资源

推荐：

```text
GET    /api/users
GET    /api/users/1001
POST   /api/users
PATCH  /api/users/1001
DELETE /api/users/1001
```

不推荐把所有操作都写进 URL：

```text
/api/getUsers
/api/createUser
/api/deleteUser?id=1001
```

因为 HTTP 方法已经可以表达查询、创建、修改和删除。

不过，“登录”“批准订单”等业务动作不一定能自然表示为普通 CRUD。项目可以设计动作型接口，例如：

```text
POST /api/sessions
POST /api/orders/5001/cancel
```

重点是保持团队规则一致，并让接口含义清楚。

### 12.3 集合资源与单个资源

```text
/api/users       用户集合
/api/users/1001  单个用户
```

建议在同一个项目中统一使用单数或复数形式。许多 REST API 使用复数名词表示集合。

### 12.4 资源之间的关系

查询某个用户的订单：

```text
GET /api/users/1001/orders
```

嵌套层级不宜过深。过深 URL 会增加理解和维护成本：

```text
/api/companies/1/departments/2/users/1001/orders/5001/items
```

实际项目中可以根据资源是否能独立识别，改为更短的接口。

### 12.5 分页、筛选与排序

```text
GET /api/users?page=1&size=20&status=active&sort=createdAt,desc
```

常见参数：

- `page`：页码
- `size`：每页数量
- `status`：筛选状态
- `sort`：排序字段和方向

分页从 `0` 还是从 `1` 开始、最大 `size` 是多少，都应在接口设计书中明确。

### 12.6 响应结构

返回单个用户时可以直接返回资源：

```json
{
  "id": 1001,
  "userName": "tanaka"
}
```

列表接口可能同时返回分页信息：

```json
{
  "items": [
    {
      "id": 1001,
      "userName": "tanaka"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 35
}
```

有些项目使用统一外层结构，例如 `data`、`message` 和 `code`；有些项目直接返回资源。两种方式都需要明确约定，不能把某一种项目习惯描述成 HTTP 的强制要求。

### 12.7 错误响应

一个便于调用方处理的错误响应可以包含：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "指定的用户不存在",
  "details": [],
  "traceId": "example-trace-id"
}
```

字段作用：

- `code`：稳定的业务错误代码，程序可以据此判断。
- `message`：便于人阅读的错误说明。
- `details`：字段校验等详细信息。
- `traceId`：用于在日志中关联本次请求，名称由项目决定。

错误响应不应向客户端暴露数据库密码、SQL、服务器内部路径、堆栈信息或密钥。

## 十三、Cookie、Session 与 Token

### 13.1 为什么需要保存登录状态

HTTP 本身通常被描述为无状态协议：每个请求应携带处理它所需的信息，服务器不能仅凭“这是同一个浏览器”自动知道当前用户是谁。

用户登录后，系统需要通过某种机制识别后续请求，这就会使用 Cookie、Session 或 Token。

### 13.2 Cookie

Cookie 是浏览器按服务器要求保存的一小段数据。符合域名、路径和安全规则时，浏览器会在后续请求中自动携带它。

服务器设置 Cookie：

```http
Set-Cookie: sessionId=example-session-id; HttpOnly; Secure; SameSite=Lax
```

浏览器后续发送：

```http
Cookie: sessionId=example-session-id
```

常见安全属性：

| 属性 | 作用 |
| --- | --- |
| `HttpOnly` | 限制前端 JavaScript 读取 Cookie，有助于降低令牌被脚本直接读取的风险 |
| `Secure` | 只通过 HTTPS 发送 Cookie |
| `SameSite` | 控制跨站请求时是否携带 Cookie，有助于降低 CSRF 风险 |
| `Max-Age` / `Expires` | 设置 Cookie 的有效期 |

Cookie 不适合保存密码等敏感原文，也不能因为设置了安全属性就忽略 XSS、CSRF 和 HTTPS 等其他安全措施。

### 13.3 Session

Session 通常表示服务器保存登录状态，浏览器只保存一个 Session ID。

```text
浏览器保存 Session ID
        ↓ Cookie
服务器根据 Session ID
        ↓
查询服务器端 Session 数据
        ↓
确认当前用户和登录状态
```

优点是敏感的会话数据主要保存在服务器端。需要考虑 Session 存储、过期、集群共享和注销等问题。

### 13.4 Token

Token 是客户端调用接口时携带的访问凭据，常放在 `Authorization` Header：

```http
Authorization: Bearer example-access-token
```

后端验证 Token 后确认用户身份和权限。

Token 不等于 JWT。JWT 是 Token 的一种常见格式，系统也可以使用不透明的随机 Token。

### 13.5 三者的关系

Cookie、Session 和 Token 不是完全同一层面的概念：

- Cookie 是浏览器保存和发送数据的机制。
- Session 是服务器端保存会话状态的方案。
- Token 是客户端证明身份或访问权限的凭据。
- Session ID 可以放在 Cookie 中。
- Token 也可能放在 Cookie 中或 `Authorization` Header 中。

具体认证实现、JWT、刷新令牌和权限模型应在认证授权课程中继续学习。

## 十四、认证、授权与 CORS 的基础区别

### 14.1 认证与授权

- **认证（Authentication）**：确认你是谁，例如登录。
- **授权（Authorization）**：确认你可以做什么，例如管理员能删除用户。

认证成功不代表拥有所有权限。

### 14.2 CORS

CORS（Cross-Origin Resource Sharing）是浏览器执行的跨源访问控制机制。

例如：

```text
前端：http://localhost:5173
后端：http://localhost:8000
```

协议、主机或端口不同，就可能形成不同源。后端需要返回合适的 CORS Header，浏览器才允许前端脚本读取响应。

CORS 不是登录认证，也不能代替后端权限检查。Postman 或服务器之间调用不受浏览器同源策略以同样方式限制，因此“Postman 可以、浏览器不行”经常是 CORS 调查线索。

## 十五、缓存、反向代理与负载均衡基础

这些概念在完整 Web 系统中经常出现，本章先建立职责认识。

| 概念 | 基础作用 |
| --- | --- |
| 缓存 | 保存可复用结果，减少重复计算和网络传输 |
| 反向代理 | 接收客户端请求，再转发到后端应用 |
| 负载均衡 | 将请求分配到多个后端实例 |

缓存可能存在于浏览器、代理、CDN 或应用内部。涉及用户隐私和实时数据时，必须谨慎设置缓存规则。

反向代理和负载均衡不会自动解决业务错误。出现 `502`、`503`、`504` 时，需要结合代理日志、应用日志和上游服务状态调查。

## 十六、使用工具观察接口

### 16.1 浏览器开发者工具

Chrome 或 Edge 中可以打开开发者工具的 Network 面板，查看：

- 请求 URL
- HTTP 方法
- 请求 Header
- Query 参数
- 请求 Body
- 响应状态码
- 响应 Header
- 响应 Body
- 请求耗时

基础操作：

1. 打开目标页面。
2. 打开开发者工具并选择 Network。
3. 刷新页面或执行按钮操作。
4. 选择一条请求。
5. 查看 Headers、Payload、Response 和 Timing。

不要在截图或问题报告中泄露真实 Token、Cookie、账号和内部地址。

### 16.2 curl

`curl` 是常见的命令行 HTTP 客户端。Windows PowerShell 5.1 中，`curl` 可能是 `Invoke-WebRequest` 的别名，因此下面的 Windows 示例明确使用 `curl.exe`。

Windows PowerShell 查询接口的命令结构：

```powershell
curl.exe --max-time 10 -i "https://api.example.com/api/users/1001"
```

Bash 查询接口的命令结构：

```bash
curl --max-time 10 -i "https://api.example.com/api/users/1001"
```

参数说明：

| 参数 | 作用 |
| --- | --- |
| `--max-time 10` | 最多等待 10 秒，避免请求无限等待 |
| `-i` | 同时显示响应 Header |
| URL | 指定要访问的接口 |

提交 JSON 时，可以先在当前目录创建 `request.json`：

```json
{
  "userName": "tanaka"
}
```

Windows PowerShell：

```powershell
curl.exe --max-time 10 -i -X POST "https://api.example.com/api/users" -H "Content-Type: application/json" --data-binary "@request.json"
```

Bash：

```bash
curl --max-time 10 -i -X POST "https://api.example.com/api/users" -H "Content-Type: application/json" --data-binary "@request.json"
```

这是命令结构示例，`api.example.com` 是示例域名，不能期待它返回课程中的业务数据。进入具体框架课程后，应使用本地启动的练习接口执行命令。

不要把真实 Token 直接写进会被提交到 Git 的脚本或教学截图中。

### 16.3 Postman

使用 Postman 调试接口时，一般按照以下顺序：

1. 选择 HTTP 方法。
2. 输入 URL。
3. 填写 Path 或 Query 参数。
4. 在 Headers 中设置认证信息和内容格式。
5. 需要提交数据时，在 Body 中选择正确格式。
6. 发送请求。
7. 检查状态码、响应 Header、响应 Body 和耗时。

保存请求时应使用环境变量管理不同环境的基础 URL，不要把真实密码和 Token 直接共享到公共集合。

### 16.4 OpenAPI 与 Swagger UI

- OpenAPI 是描述 HTTP API 的规范。
- Swagger UI 是读取 OpenAPI 定义并显示交互式接口文档的工具。

在 Swagger UI 中通常可以查看：

- 接口路径和方法
- 参数名称与类型
- 请求体结构
- 响应状态码
- 数据模型
- 在线试调用入口

Swagger UI 显示的内容来自接口定义。文档存在并不代表接口实现一定正确，仍需要测试正常、异常和边界情况。

## 十七、接口设计书应该看什么

企业项目中，开发者通常需要根据接口设计书实现或调用接口。

一份基础接口定义至少应说明：

| 项目 | 示例 |
| --- | --- |
| 接口名称 | 用户详情查询 |
| HTTP 方法 | `GET` |
| URL | `/api/users/{userId}` |
| Path 参数 | `userId`：用户编号，必填 |
| Query 参数 | `includeOrders`：是否包含订单，可选 |
| Header | `Authorization`、`Accept-Language` |
| 请求 Body | 本接口无请求体 |
| 成功响应 | `200` 和用户 JSON |
| 错误响应 | `400`、`401`、`403`、`404`、`500` |
| 认证要求 | 必须登录 |
| 权限要求 | 用户查看权限 |

检查接口定义时需要确认：

- 字段名称和大小写是否统一
- 类型、必填、长度、格式和允许值是否明确
- `null`、空字符串、空数组如何处理
- 日期时间格式和时区是否明确
- 正常与异常状态码是否明确
- 分页起始值和最大数量是否明确
- 认证和权限条件是否明确
- 错误代码是否稳定且可调查

## 十八、日本项目中的接口开发与联调

日本项目中常见的接口相关资料可能包括：

- API 仕様書：API 规格说明书
- インターフェース仕様書：接口规格说明书
- 項目定義：字段定义
- リクエスト／レスポンス：请求／响应
- 正常系：正常流程
- 異常系：异常流程
- 境界値：边界值
- 単体試験仕様書：单体测试规格书
- エビデンス：测试证据

实际工作中应重点确认：

1. 接口设计书与实际请求、响应是否一致。
2. 前端、后端和外部系统对字段含义是否理解一致。
3. 必填、长度、格式、范围和枚举值是否已经确认。
4. 空值、重复请求和边界值如何处理。
5. 异常时返回什么状态码、错误代码和消息。
6. 修改接口是否影响已有调用方。
7. 日志中是否有足够信息用于调查，同时避免记录敏感数据。

联调出现问题时，建议按以下证据说明：

```text
发生时间：2026-07-15 10:30 JST
请求环境：测试环境
请求方法：POST
请求路径：/api/users
响应状态：409
业务错误代码：USER_ALREADY_EXISTS
复现条件：使用已登记邮箱创建用户
期望结果：与接口设计书中的重复用户规则一致
```

报告问题时不要直接粘贴真实密码、完整 Token 或个人敏感信息。

## 十九、常见错误与调查方法

### 19.1 请求地址错误

现象：返回 `404 Not Found`。

检查：

- 主机和端口是否正确
- 路径是否缺少 `/api`
- 路径参数是否填写正确
- 服务是否使用了统一前缀

### 19.2 HTTP 方法错误

现象：返回 `405 Method Not Allowed`，或者访问了错误处理程序。

错误示例：

```text
GET /api/users
```

但接口设计要求创建用户：

```text
POST /api/users
```

修正方法：按照接口设计选择正确的 HTTP 方法。

### 19.3 Content-Type 错误

现象：服务器无法解析 Body，可能返回 `400`、`415` 或框架定义的校验错误。

提交 JSON 时应使用：

```http
Content-Type: application/json
```

同时保证 Body 本身是合法 JSON。

### 19.4 JSON 语法错误

现象：服务器提示 JSON 解析失败。

常见原因：

- 使用单引号
- 缺少逗号或括号
- 多余尾逗号
- 使用 `True`、`False`、`None` 等语言写法

### 19.5 401 与 403 判断错误

- `401`：先检查是否登录、Token 是否缺失或过期。
- `403`：检查当前用户角色和权限。

不要遇到所有认证相关问题都重新登录，也不要把权限不足误报为服务器异常。

### 19.6 只看响应 Body，不看状态码

调查接口时至少同时记录：

- 请求方法和 URL
- 请求参数
- 状态码
- 响应 Header
- 响应 Body
- 请求时间和追踪编号

### 19.7 Postman 成功但浏览器失败

可能原因：

- CORS 配置
- Cookie 的域名、路径、SameSite 或 Secure 属性
- 浏览器自动发送的预检请求
- 前端实际请求地址与 Postman 不同

应使用浏览器 Network 面板检查实际请求，不能只凭前端错误提示判断。

### 19.8 把 500 当作正常业务错误

用户不存在、参数不合法和权限不足通常不是服务器内部异常。应返回与项目规范匹配的 `4xx` 状态和业务错误信息。

如果确实出现 `500`，服务端应检查日志和异常原因，不应把内部堆栈直接返回给客户端。

## 二十、基础练习

### 练习一：拆解 URL

拆解以下 URL 的协议、主机、端口、路径、查询参数和片段：

```text
https://shop.example.com:8443/api/products/ABC-001?lang=ja&includeStock=true#detail
```

### 练习二：选择 HTTP 方法

为以下操作选择合适的 HTTP 方法，并说明理由：

1. 查询商品列表。
2. 查询编号为 `5001` 的订单。
3. 创建一个新订单。
4. 只修改用户邮箱。
5. 删除编号为 `1001` 的用户。

### 练习三：选择参数位置

判断以下数据更适合放在 Path、Query、Header 还是 Body：

1. 要查询的用户编号。
2. 列表页码和每页数量。
3. 访问令牌。
4. 创建用户时的姓名和邮箱。
5. 客户端希望接收的语言。

### 练习四：判断状态码

为以下情况选择合适的状态码：

1. 用户创建成功。
2. 未登录访问受保护接口。
3. 普通用户访问管理员接口。
4. 指定订单不存在。
5. 账号已经存在，无法重复创建。
6. 服务器发生未处理异常。

### 练习五：修正 JSON

找出并修正下面 JSON 的问题：

```json
{
  'orderId': 5001,
  "paid": False,
  "items": ["A001", "B002",],
}
```

## 二十一、综合练习

为“商品管理”设计下面五个接口：

1. 查询商品列表，支持名称、状态和分页条件。
2. 查询指定商品。
3. 创建商品。
4. 修改商品价格。
5. 删除商品。

每个接口需要整理：

- HTTP 方法
- URL
- Path 参数
- Query 参数
- Header
- 请求 Body
- 成功状态码
- 可能出现的客户端错误状态码
- 成功响应 JSON 示例
- 错误响应 JSON 示例

完成后检查：

- URL 是否围绕资源设计
- HTTP 方法是否符合操作目的
- 参数位置是否合理
- JSON 类型是否正确
- `401`、`403`、`404` 和 `409` 是否使用清楚

## 二十二、本章总结

- HTTP 规定了客户端与服务器组织请求和响应的基本方式。
- URL 由协议、主机、端口、路径、查询参数和片段等部分组成。
- 请求主要包含请求行、请求头和可选请求体。
- 响应主要包含状态行、响应头和可选响应体。
- HTTP 方法表达操作目的，状态码表达处理结果。
- Path、Query、Header、Cookie 和 Body 承担不同职责。
- JSON 是常见的数据交换格式，语法与 Python、Java 等语言对象写法并不完全相同。
- REST API 通常围绕资源设计 URL，并使用 HTTP 方法表达操作。
- Cookie、Session 和 Token 是相关但不同层面的机制。
- 接口联调需要同时检查请求、响应、状态码、认证、权限和实际证据。
- 企业项目中应以接口设计书和团队规范为准，并关注兼容性、安全性和可调查性。
