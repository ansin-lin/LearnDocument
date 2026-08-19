# 第1章 Django 与请求响应流程

## 一、本章成果

本章不编写 Django 项目，而是先建立后续29章都会使用的系统地图。完成后，你应能够：

### 必须掌握

- 说明 Django 运行在服务器端，在 Web 系统中负责什么
- 区分 HTTP Request、Django `HttpRequest`、`HttpResponse` 和 HTTP Response
- 说明 URL Dispatcher、View、Model、Template 的职责
- 按顺序解释一次员工列表请求经过的主要环节
- 区分 GET 与 POST 的基本用途
- 使用浏览器开发者工具观察请求方法、URL、状态码和内容类型

### 会使用、能看懂

- 说明 Django 的 MTV 与常见 MVC 说法之间的关系
- 说明 Django Project 与 App 的基本区别
- 在项目中认出 `wsgi.py`、`asgi.py` 和 Middleware 所处的位置

第2章会创建项目和 App，第3章会让浏览器收到第一个真正由 Django 返回的响应。

## 二、开始前需要知道什么

开始本章前，应能运行简单 Python 程序，并了解 URL、浏览器和服务器这些基本概念。需要复习时可先阅读：

- [Python 通用基础](../../../common/00_intro.md)
- [前后端与请求响应基础](../../../../web_basics/00_frontend_backend_request_response.md)
- [HTTP、REST、Cookie、Session 与 CORS](../../../../web_basics/01_http_rest_cookie_cors.md)中的 HTTP 部分

当前只需要 HTTP 的基础知识。JSON 与 REST API 会在第19章以后的前后端分离阶段正式使用。

## 三、Django 是什么

Django 是使用 Python 开发 Web 应用的框架，运行在服务器端。浏览器不会直接执行 Django 代码，而是通过 HTTP 向服务器发送请求，再接收 Django 返回的 HTML、JSON 或文件。

Django 提供许多 Web 项目常用能力：

- 根据 URL 把请求交给对应处理代码
- 接收和读取请求数据
- 返回 HTML、JSON、文件和错误响应
- 使用模板生成 HTML 页面
- 使用 Model 操作数据库
- 校验表单输入
- 管理登录、Session 和权限
- 提供内部数据维护后台
- 记录日志并执行自动测试

Django 不知道“员工编号必须以 E 开头”或“离职员工不能继续显示”等业务规则。框架提供结构和工具，开发者根据规格实现业务。

本课程采用服务端渲染作为第一条主线：

```text
浏览器发送请求
    ↓
Django 处理业务并生成 HTML
    ↓
浏览器接收完整 HTML 页面
```

## 四、先认识 HTTP Request 与 Response

浏览器访问网页时，会向服务器发送 HTTP Request（HTTP 请求）。服务器处理后返回 HTTP Response（HTTP 响应）。

### 4.1 请求中有什么

```text
GET /employees/?q=E001 HTTP/1.1
Host: 127.0.0.1:8000
Accept: text/html
```

这个请求包含：

| 内容 | 示例 | 作用 |
|---|---|---|
| 方法 | `GET` | 表示这次请求要执行的操作类型 |
| 路径 | `/employees/` | 表示要访问的资源位置 |
| 查询参数 | `q=E001` | 表示搜索、筛选等附加条件 |
| 请求头 | `Accept: text/html` | 传递格式、认证、浏览器能力等信息 |
| 请求体 | GET 示例中没有 | POST 等请求可以在这里提交数据 |

### 4.2 响应中有什么

```text
HTTP/1.1 200 OK
Content-Type: text/html; charset=utf-8

<html>...</html>
```

这个响应包含：

| 内容 | 示例 | 作用 |
|---|---|---|
| 状态码 | `200` | 表示处理结果 |
| 响应头 | `Content-Type: text/html` | 表示响应内容类型 |
| 响应体 | `<html>...</html>` | 浏览器实际收到的内容 |

本课程经常使用的状态码包括：

| 状态码 | 当前先记住的含义 |
|---:|---|
| 200 | 请求成功 |
| 302 | 服务器要求浏览器访问另一个地址 |
| 403 | 服务器理解请求，但拒绝执行；常见于权限或 CSRF 检查失败 |
| 404 | 找不到请求的页面或数据 |
| 500 | 服务器发生未预期错误 |

## 五、GET 与 POST

GET 和 POST 都是 HTTP 请求方法，但常见用途不同。

| 方法 | 课程中的主要用途 | 数据常见位置 | 示例 |
|---|---|---|---|
| GET | 读取页面、列表、详情和搜索结果 | 路径、查询参数 | `GET /employees/?q=E001` |
| POST | 提交表单并产生新增、修改等变化 | 请求体 | `POST /employees/new/` |

GET 通常不应产生删除或修改等业务变化。POST 也不代表请求自动安全；后端仍必须执行登录、权限、输入校验和业务规则检查。

第9章会实际处理 POST 表单。当前先能从浏览器 Network 中分辨 GET 和 POST。

## 六、HTTP 消息如何变成 Django 对象

浏览器发送的是 HTTP 请求消息，Django View 收到的不是一段原始文字，而是 Django 已经整理好的 `HttpRequest` 对象。

| 概念 | 是什么 | 为什么需要 | 什么时候使用 |
|---|---|---|---|
| `HttpRequest` | Django 对一次 HTTP 请求的对象表示 | 让 View 用统一属性读取方法、参数、文件和用户，而不必自行解析原始报文 | 每个 View 接收请求，以及读取 `method`、`GET`、`POST` 等信息时 |
| `HttpResponse` | Django 对一次 HTTP 响应的对象表示 | 把响应体、状态码和响应头统一交给 Django 返回浏览器 | View 完成处理并返回文本、HTML、文件或错误结果时 |

```text
浏览器发送 HTTP Request
        ↓
Django 解析请求
        ↓
View 收到 HttpRequest 对象
        ↓
View 返回 HttpResponse 对象
        ↓
Django 生成 HTTP Response
        ↓
浏览器接收响应
```

后续会从 `HttpRequest` 读取：

```python
request.method       # GET、POST 等方法
request.GET          # URL 查询参数
request.POST         # 普通表单提交数据
request.FILES        # 上传文件
request.user         # 当前用户
```

这些代码现在只需能看懂，不要求运行。第3章从 `request.method` 开始观察，第9章处理 `request.POST`，第14章处理 `request.FILES`。

View 处理完成后必须返回响应对象。后续常见写法包括：

```python
HttpResponse("OK")          # 直接构造响应
render(request, ...)        # 渲染模板并返回响应
redirect("employees:list") # 返回重定向响应
```

这里先建立三者的最小区别，具体模板和路由写法在第3～5章实际操作：

| API | 作用 | 当前参数 | 可接受的值与必填性 | 返回值 | 什么时候使用 |
|---|---|---|---|---|---|
| `HttpResponse(content)` | 直接构造响应 | `content` | 文本或字节内容，可省略为空内容 | `HttpResponse` | 健康检查或简单文本响应 |
| `render(request, template_name, context=None)` | 用模板和数据生成HTML响应 | 请求、模板路径、context | 请求与模板路径必填；`context` 是可选字典 | `HttpResponse` | 返回服务端渲染页面 |
| `redirect(to, *args, **kwargs)` | 生成重定向响应 | 目标及路由参数 | `to` 必填，通常是路由名；其余参数按目标路由决定 | 3xx响应 | POST成功后跳转结果页 |

`render()` 和 `redirect()` 只负责创建响应对象，View仍必须使用 `return` 把它交给Django；它们不是绕过HTTP。

## 七、Django 请求处理中的核心角色

先用一张表明确每个角色的职责：

| 角色 | 负责什么 | 不负责什么 |
|---|---|---|
| URL | 浏览器访问的地址 | 不会自己执行 Python 代码 |
| URL Dispatcher | 根据路径匹配路由规则 | 不负责页面显示和业务计算 |
| View | 接收 `HttpRequest`，组织处理并返回响应 | 不应承担所有显示和数据库细节 |
| Model | 描述数据并通过 ORM 访问数据库 | 不负责接收浏览器请求 |
| Template | 使用 View 提供的数据生成 HTML | 不负责长期保存数据和后端权限 |
| `HttpResponse` | 表示 View 要返回的结果 | 不代表页面一定成功，状态码可能是错误 |

这些角色的使用原因和时机如下：

| 概念 | 为什么需要 | 什么时候使用 |
|---|---|---|
| URL Dispatcher | 系统必须根据访问路径选择处理入口 | 每次请求进入 Django 时 |
| View | 需要一个位置组织请求、业务调用和响应 | 实现页面、提交处理、下载和健康检查时 |
| Model | 需要用 Python 对象统一描述和访问持久化数据 | 第6章以后保存和查询部门、员工时 |
| Template | 页面结构与 Python 处理代码需要分离 | 第5章以后生成服务端 HTML 时 |

Model 和 Template 都不是每次请求必定使用：

- 健康检查 View 可以直接返回 `HttpResponse("OK")`。
- 员工列表通常查询 Model，再渲染 Template。
- 文件下载 View 可以返回文件响应而不渲染 Template。

## 八、员工列表的一次完整请求

假设用户访问：

```text
http://127.0.0.1:8000/employees/
```

服务端渲染主线中的主要过程如下：

```text
1. 浏览器发送 GET /employees/
2. Django 创建 HttpRequest 对象
3. URL Dispatcher 找到 employee_list View
4. employee_list 根据需要通过 Employee Model 查询数据
5. View 把员工数据交给 Template
6. Template 生成 HTML
7. View 返回 HttpResponse
8. Django 发送 HTTP Response
9. 浏览器解析 HTML 并显示员工列表
```

第5章先使用固定员工数据完成页面，第6章建立 Model 和数据库表，第8章再让列表真正读取数据库。虽然实现会逐章变化，请求主线始终保持一致。

## 九、MTV、MVC 与 Django View

Django 项目常使用 MTV 说明三个主要职责：

| MTV | 名称 | 职责 |
|---|---|---|
| M | Model | 数据结构、关系和数据库访问 |
| T | Template | HTML 显示结构 |
| V | View | 请求处理与响应组织 |

很多企业资料使用更通用的 MVC 术语。可以用下面的近似关系帮助阅读：

| 通用 MVC | Django 中的近似对应 |
|---|---|
| Model | Model |
| View（显示层） | Template |
| Controller（请求控制） | URL Dispatcher + Django View |

这个对应用于理解职责，不表示两个框架结构必须逐项完全相同。最需要避免的误解是：Django 的 View 不是浏览器中看到的页面，它是处理请求的 Python 函数或类。

## 十、Project 与 App

Django 使用 Project 和 App 组织代码：

| 名称 | 课程示例 | 主要职责 |
|---|---|---|
| Project | `company_portal` | 全站配置、最外层路由和服务器入口 |
| App | `employees` | 员工业务相关的 View、Model、路由和测试 |

一个 Project 可以包含多个 App，例如员工、考勤和账号。App 应围绕一组业务职责组织，不是简单地把所有 Model 或所有 View 分别放成一个 App。

本章只认识区别。第2章会实际创建 `company_portal` 和 `employees`。

## 十一、在浏览器中观察真实请求

现在还没有创建 Django 项目，可以先观察一个普通网页：

1. 打开浏览器并访问一个普通网页。
2. 按 `F12` 打开开发者工具。
3. 选择 Network（网络）面板。
4. 刷新页面。
5. 选择 Type 为 document 的页面请求。
6. 查找 Request Method、Request URL、Status Code 和 Content-Type。

正常 HTML 页面通常会看到类似信息：

```text
Request Method: GET
Status Code: 200
Content-Type: text/html; charset=utf-8
```

如果发生跳转，可能先看到301或302，再看到最终200。只记录本次练习需要的字段，不复制 Cookie、Authorization、Token 或其他登录信息。

第3章运行第一个 Django View 后，需要用同样方式观察自己的请求。

## 十二、当前只需看懂的完整入口

生产环境中的请求链比本地开发服务器更长：

```text
浏览器
  → Web服务器或反向代理
  → WSGI或ASGI应用入口
  → Middleware
  → URL Dispatcher
  → View
  → Model、Template或其他服务
  → HttpResponse
  → 浏览器
```

当前只需知道：

- **WSGI是什么**：Python Web服务器与同步Web应用之间的标准调用接口。需要它，是为了让不同服务器能用统一方式运行Django；传统同步部署中使用。
- **ASGI是什么**：同时支持同步请求和异步连接的应用接口。需要它，是为了承载异步请求及WebSocket等长连接能力；使用ASGI服务器或异步功能时使用。

当前项目不直接调用WSGI/ASGI函数，只需知道服务器会通过 `wsgi.py` 或 `asgi.py` 进入Django。生产环境选择哪一种取决于项目部署方式和实际并发需求。
- Middleware 在 View 前后执行 Session、安全、认证、日志等共通处理。
- 反向代理位于 Django 前方，可处理 TLS、转发和静态资源等工作。

本课程所说的 request-response cycle 或 request lifecycle，指一次请求进入 Django 到响应返回的过程。Django 应用自身还有启动和停止过程，本章不展开。第15章会编写简单 Middleware，第28章再学习生产部署链路。

## 十三、常见理解错误

### 13.1 把 Django 当成浏览器程序

Django 在服务器端运行。浏览器执行 HTML、CSS 和 JavaScript，并显示服务器返回的结果。

### 13.2 认为 URL 会直接执行 Python

URL 只是地址。URL Dispatcher 必须先匹配路由规则，才能找到并调用 View。

### 13.3 认为每个 View 都必须查询数据库

是否使用 Model 取决于请求需要。简单健康检查可以直接返回响应。

### 13.4 把 Template 当成数据库

Template 负责生成显示内容，不负责长期保存员工数据。

### 13.5 认为隐藏按钮等于权限控制

隐藏按钮只能改善页面体验。攻击者仍可直接发送请求，所以 View 必须执行后端权限检查。

### 13.6 把 HTTP Request 与 `HttpRequest` 当成两个无关概念

它们描述同一次请求的不同阶段：浏览器发送 HTTP 消息，Django 解析后把它表示为 Python `HttpRequest` 对象。

## 十四、练习

### 练习1：还原请求顺序

把下面步骤按正确顺序排列：

- Template 生成 HTML
- 浏览器发送 GET 请求
- URL Dispatcher 匹配 View
- View 返回 `HttpResponse`
- View 根据需要查询 Model
- 浏览器显示页面

完成标准：能够说明 Model 和 Template 为什么不一定在所有请求中出现。

### 练习2：判断组件职责

为下面问题选择首先调查的位置，并说明理由：

1. `/employees/` 返回404。
2. 页面为200，但员工姓名没有显示。
3. 数据库中没有保存新员工。
4. 用户隐藏按钮后仍能直接请求删除地址。

可选择：URL Dispatcher、View、Model、Template、后端权限检查。一个问题可能涉及多个位置，但必须先写最先确认的证据。

### 练习3：观察 HTTP

使用浏览器 Network 记录一个 document 请求的：

- Request Method
- Request URL
- Status Code
- Content-Type

再用一句话说明：浏览器发送的 HTTP Request 如何变成 View 中的 `HttpRequest`。

### 练习4：解释 MTV

不看正文，分别用一句话说明 Model、Template 和 View 的职责，并解释为什么 Django View 不是“页面文件”。

参考判断见[章节练习参考答案](practice_answers.md)。

## 十五、本章完成检查

- [ ] 能说明 Django 运行在服务器端
- [ ] 能区分 HTTP Request、`HttpRequest`、`HttpResponse` 和 HTTP Response
- [ ] 能区分 URL Dispatcher、View、Model 和 Template
- [ ] 能按顺序说明一次员工列表请求
- [ ] 能说明 GET 与 POST 的基本用途
- [ ] 能解释 MTV 三个字母代表什么
- [ ] 能说明 Project 与 App 的区别
- [ ] 能在 Network 找到方法、URL、状态码和内容类型
- [ ] 知道 WSGI、ASGI 和 Middleware 当前只要求能识别

## 十六、本章总结

- Django 是运行在服务器端的 Python Web 框架。
- 浏览器发送 HTTP Request，Django 把它解析为 `HttpRequest` 交给 View。
- URL Dispatcher 负责找到 View；View 根据需要调用 Model、Template 或其他服务。
- View 最终返回 `HttpResponse`，Django 再把它发送为 HTTP Response。
- MTV 分别是 Model、Template 和 View；Django View 负责请求处理，不是页面文件。
- Project 保存全站配置，App 组织具体业务。
- 第2章将创建 `company_portal` Project 和 `employees` App。

## 十七、日本项目中的实际使用

日本企业项目通常先用请求路径说明系统行为，再讨论代码实现。例如调查“员工列表打不开”时，会依次确认浏览器请求、URL、View、数据库访问和响应状态。这样记录调查过程，其他成员能够复现，也便于 Review 判断问题属于前端、Django 还是数据库。

## 十八、新人常见错误

- 把 View 理解成 HTML 页面。View 是处理请求的 Python 函数或类，Template 才负责页面结构。
- 把 Django 的 `HttpRequest` 当成浏览器发送的原始文本。它是 Django 解析 HTTP 请求后创建的对象。
- 看到页面就只记录200。跳转、403、404和500都表达不同结果，应同时确认状态码。
- 认为 GET 和 POST 只是名字不同。GET 主要读取资源，POST 常用于产生状态变化。

## 十九、本章知识将在后续章节继续使用

```text
HTTP Request
→ URL Dispatcher（第3～4章）
→ View（第3章以后）
→ Template / Model（第5～8章）
→ HttpResponse
→ 登录、权限、日志与测试（第12～16章）
```
