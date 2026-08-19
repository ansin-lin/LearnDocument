# 第3章 第一个 URL 与 View

## 一、本章完成目标

本章在第2章项目上新增第一个页面。完成后，你应能够：

- 编写最小函数视图
- 使用 `path()` 把 URL 连接到视图
- 说明 `HttpRequest` 和 `HttpResponse` 的作用
- 按顺序解释请求如何到达视图
- 使用浏览器和终端验证结果
- 调查常见的404、导入和返回值错误

本章先完成一条最小请求链，再用健康检查地址重复一次相同操作；不提前加入 App 路由拆分、路径参数、命名路由和模板。

## 二、本章开始状态

项目应满足：

```text
company_portal/
├── .gitignore
├── company_portal/
│   ├── settings.py
│   └── urls.py
├── employees/
│   └── views.py
├── manage.py
└── requirements.txt
```

先执行：

```powershell
python manage.py check
```

如果检查失败，先回到第2章解决环境或配置问题。尚未应用迁移的提示在本章仍然属于预期状态，不影响练习当前页面。

## 三、本章要完成的请求链

浏览器访问：

```text
http://127.0.0.1:8000/
```

Django 执行：

```text
GET /
  ↓
company_portal/urls.py
  ↓
employees.views.home
  ↓
HttpResponse
  ↓
浏览器显示“员工管理系统正在运行”
```

## 四、编写第一个 View

打开：

```text
employees/views.py
```

将文件改为：

```python
from django.http import HttpRequest, HttpResponse


def home(request: HttpRequest) -> HttpResponse:
    return HttpResponse("员工管理系统正在运行")
```

这是一个函数视图。

- **是什么**：函数 View 是接收 `HttpRequest` 并返回 `HttpResponse` 的 Python 函数。
- **为什么需要**：URL 只负责匹配地址，实际读取请求、调用业务处理和构造结果需要由 View 完成。
- **什么时候使用**：本课程的页面、表单、删除确认和文件下载都从函数 View 开始实现。

### 4.1 函数参数 `request`

Django 收到浏览器请求后会建立请求对象。URL 匹配成功时，Django 把这个对象作为第一个参数传给视图：

```python
request: HttpRequest
```

请求对象中可以读取请求方法、查询条件、登录用户等信息。本章暂时不读取，只确认它是视图处理当前请求的入口。

类型提示帮助阅读和静态检查，不会在运行时强制参数类型。

### 4.2 返回值 `HttpResponse`

视图必须返回 Django 能处理的响应对象：

```python
return HttpResponse("员工管理系统正在运行")
```

这里的响应体是一段文本。浏览器收到后会直接显示。

## 五、配置第一个 URL

打开：

```text
company_portal/urls.py
```

将文件改为：

```python
from django.contrib import admin
from django.urls import path

from employees import views


urlpatterns = [
    path("admin/", admin.site.urls),
    path("", views.home),
]
```

`path()` 的作用是把路径规则和 View 建立对应关系。第一个参数接受路径字符串，空字符串表示网站根路径；第二个参数接受可调用的 View，这里传 `views.home` 而不是执行函数。`path()` 返回 URL pattern，放入 `urlpatterns` 后由 Django 按顺序匹配请求。

这里保留 Django 默认的 Admin 路由，并新增首页路由。

## 六、理解 `path()`

当前路由是：

```python
path("", views.home)
```

| 部分 | 当前值 | 作用 |
| --- | --- | --- |
| 路径 | `""` | 匹配项目根路径 `/` |
| 视图 | `views.home` | 匹配后调用的函数 |

URL 配置中的路径不写开头的 `/`。因此根路径 `/` 在 `path()` 中写成空字符串 `""`。Django 会按照 `urlpatterns` 中从上到下的顺序查找，找到第一条匹配规则后调用对应视图。

注意：这里写的是 `views.home`，不是 `views.home()`。

- `views.home`：把函数交给 Django，在请求到达时调用
- `views.home()`：配置加载时立刻调用，而且没有 request 参数

## 七、运行并验证

修改 URL 和 View 后先检查配置：

```powershell
python manage.py check
```

预期结果：

```text
System check identified no issues (0 silenced).
```

然后启动开发服务器：

```powershell
python manage.py runserver
```

访问：

```text
http://127.0.0.1:8000/
```

预期页面：

```text
员工管理系统正在运行
```

同时完成三种验证。

### 7.1 浏览器页面

确认页面文字正确，没有显示 Django 安装成功默认页面。

### 7.2 浏览器 Network

在开发者工具 Network 面板确认：

```text
Request Method: GET
Status Code: 200
Content-Type: text/html; charset=utf-8
```

开发者工具的显示形式可能略有不同，但应能确认响应类型是 HTML，字符集是 UTF-8。

### 7.3 开发服务器终端

终端应出现类似记录：

```text
"GET / HTTP/1.1" 200
```

这条记录说明服务器收到了根路径的 GET 请求，并返回200。

## 八、请求执行顺序

现在可以把第1章的地图对应到真实文件：

1. 浏览器发送 `GET /`。
2. Django 建立 `HttpRequest`，并读取根 URL 配置 `company_portal/urls.py`。
3. Django 按顺序检查 `urlpatterns`，`path("", views.home)` 匹配成功。
4. Django 调用 `home(request)`。
5. `home()` 返回 `HttpResponse`。
6. Django 把状态码、响应头和响应体发送给浏览器。

本章没有模板。响应内容直接写在 Python 中只是为了观察最小流程，第5章会使用 HTML 模板替换这种方式。

## 九、修改响应并观察变化

把视图暂时改为：

```python
def home(request: HttpRequest) -> HttpResponse:
    return HttpResponse("员工管理系统：第一个 Django 页面")
```

保存后刷新浏览器。开发服务器默认会检测 Python 文件变化并重新加载。

如果没有变化，检查：

- 文件是否保存
- 浏览器访问的端口是否正确
- 终端是否出现异常
- 修改的是否是当前项目中的 `employees/views.py`

确认后保留新的文字，保证后续章节从相同状态继续。

## 十、增加健康检查 URL

现在使用同样方法增加一个简单的健康检查地址。这个地址会保留到下一章。当前实现只确认 Django 能够返回响应，不代表数据库等外部依赖也处于正常状态。

在 `employees/views.py` 末尾追加：

```python
def health(request: HttpRequest) -> HttpResponse:
    return HttpResponse("OK")
```

在 `company_portal/urls.py` 的 `urlpatterns` 中追加：

```python
path("health/", views.health),
```

此时完整的 `urlpatterns` 是：

```python
urlpatterns = [
    path("admin/", admin.site.urls),
    path("", views.home),
    path("health/", views.health),
]
```

如果开发服务器已经停止，先执行：

```powershell
python manage.py check
```

然后重新启动开发服务器。如果开发服务器仍在运行，保存文件后先观察终端是否正常重新加载，不需要在同一个终端重复启动。

访问：

```text
http://127.0.0.1:8000/health/
```

页面应显示 `OK`，Network 状态码应为200，开发服务器终端应出现 `GET /health/`。如果开发服务器仍在运行，不需要重复启动，保存文件后刷新页面即可。

## 十一、常见错误与调查方法

### 11.1 页面返回404

症状：

```text
Page not found (404)
```

检查：

- 是否访问 `/`
- `urlpatterns` 中是否存在空字符串路由
- 是否把路径误写成 `"home/"`

如果配置的是 `path("home/", ...)`，正确访问地址应是 `/home/`。

### 11.2 `NameError: name 'views' is not defined`

原因通常是缺少：

```python
from employees import views
```

修复后重新执行 `python manage.py check`。

### 11.3 `TypeError` 提示缺少 request

检查是否错误写成：

```python
path("", views.home())
```

应改为：

```python
path("", views.home)
```

### 11.4 视图没有返回响应

错误写法：

```python
def home(request: HttpRequest) -> HttpResponse:
    message = "员工管理系统正在运行"
```

函数没有 `return`，实际返回值是 `None`。访问页面时，Django 会报告视图没有返回 `HttpResponse`。修复为：

```python
def home(request: HttpRequest) -> HttpResponse:
    message = "员工管理系统正在运行"
    return HttpResponse(message)
```

## 十二、练习

### 练习1：修改并恢复健康检查响应

把 `/health/` 的响应暂时改为：

```python
return HttpResponse("employee-service: OK")
```

完成以下验证：

- 页面显示新的响应文字
- Network 状态码为200
- 终端记录访问路径 `/health/`

验证后恢复为 `OK`，让项目保持本章规定的结束状态。

### 练习2：制造并修复404

访问一个没有注册的 `/missing/`，观察404后完成：

1. 记录请求路径和状态码。
2. 说明路由为什么没有匹配。
3. 不修改代码，把地址改回正确路径并验证恢复。

### 练习3：画出当前真实流程

使用文件名和函数名画出 `/health/` 的执行流程，不只写“路由 → 视图”。

## 十三、请求处理运行检查

- [ ] `python manage.py check` 通过
- [ ] `/` 显示自定义文本
- [ ] `/health/` 显示 `OK`
- [ ] 能指出根路由所在文件
- [ ] 能指出视图所在文件
- [ ] 能解释为什么路由写 `views.home` 而不是 `views.home()`
- [ ] 能从浏览器和终端确认状态码200
- [ ] 完成 `/health/` 练习并恢复为规定的响应

## 十四、现场识读：在 View 中观察 `request`

本章不要求处理复杂表单，但必须知道 View 收到的是 `HttpRequest` 对象。先增加一个只读观察用 View：

```python
from django.http import HttpRequest, HttpResponse


def request_info(request: HttpRequest) -> HttpResponse:
    keyword = request.GET.get("q", "")
    return HttpResponse(f"method={request.method}, q={keyword}")
```

访问 `/request-info/?q=E001`，应看到 `method=GET, q=E001`。`request.GET` 保存URL查询参数；即使名字叫GET，它也是类似字典的 `QueryDict`。`get(key, default=None)` 的 `key` 是要读取的参数名，`default` 是参数不存在时的返回值；存在时返回字符串，不存在时返回默认值。同名参数可能有多个值时应使用 `getlist()`，当前关键字搜索只取一个值。第9章处理表单时会使用 `request.POST`，第19章处理原始JSON时再使用 `request.body`。

## 十五、`render()`、`redirect()` 与其他响应

View 的最终职责是返回响应，不一定都直接构造 `HttpResponse`：

- `render(request, template_name, context)`：渲染 HTML 模板并返回响应，第5章使用。
- `redirect(...)`：返回 3xx 响应，请浏览器访问另一个地址，第9章使用。
- `JsonResponse(data)`：返回 JSON，第19章使用。
- 抛出 `Http404` 或使用 `get_object_or_404()`：让不存在的资源进入 404 流程，第8章使用。

这些工具不是“跳过 HTTP”，而是帮助开发者正确构造不同类型的 HTTP 响应。

| API | 作用 | 当前主要参数 | 可接受的值与必填性 | 返回值 | 什么时候使用 |
|---|---|---|---|---|---|
| `render()` | 用数据渲染模板 | `request`、模板路径、context 字典 | `request` 和模板路径必填；context 可省略，当前页面传字典 | `HttpResponse` | 返回 HTML 页面时 |
| `redirect()` | 要求浏览器访问另一个地址 | 路由名称及必要参数 | 路由名称必填；路径参数按目标路由决定 | 3xx 响应 | POST 成功后进入结果页时 |
| `JsonResponse()` | 把字典等数据编码为 JSON | JSON 数据 | 默认接受字典；其他类型需要额外配置 | JSON 响应 | 第19章以后 API 入门时 |

`render()` 必须接收 `request`，使模板处理能够使用当前请求上下文；模板路径告诉 Django加载哪个文件；context 必须是键值结构，模板通过键名读取数据。View 仍然必须 `return render(...)`，因为 `render()` 只是创建响应对象，不会替 View 自动返回。

## 十六、路径参数与查询参数不要混淆

`/employees/12/` 中的 `12` 用来确定一个具体资源，适合路径参数；`/employees/?q=dev` 中的 `q` 用来筛选资源，适合查询参数。路径转换器负责匹配和转换路径值，例如 `<int:employee_id>`；不要把任意用户输入直接当数据库查询或文件路径使用。

## 十七、本章总结

- URL 配置把访问路径交给 View
- 函数视图接收 `HttpRequest` 并返回响应
- `HttpResponse` 可以生成最简单的页面响应
- `urlpatterns` 按顺序保存 URL 与 View 的对应关系
- 浏览器、Django终端和 `manage.py check` 提供不同验证证据
- 第4章会把员工 URL 从项目总路由拆到自己的 App 中

## 十八、日本项目中的实际使用

企业项目通常要求每个 View 明确接收什么请求、返回什么状态。`return` 不只是 Python 语法要求：Django 必须取得一个响应对象，才能把状态码、响应头和响应体发回浏览器。健康检查地址也常用于发布后的存活确认，但不能代替数据库和外部服务检查。

## 十九、新人常见错误

- View 忘记 `return`，函数得到 `None`，Django 无法生成响应。
- 在 `path()` 中写错 View 名称或漏写导入，启动检查或请求时出现错误。
- 把路径参数和查询参数混用。资源标识放路径，筛选条件通常放查询字符串。
- 使用 `request.POST` 读取 GET 查询参数，结果始终为空。应根据请求位置选择 `request.GET` 或 `request.POST`。
- 把 `redirect()` 当作最终页面内容。它先返回重定向响应，浏览器还会发起下一次请求。

## 二十、本章知识将在后续章节继续使用

```text
path()
→ View(request)
→ HttpResponse / render() / redirect()
→ 第4章拆分 App 路由
→ 第5章返回 Template
→ 第8章查询数据库
```
