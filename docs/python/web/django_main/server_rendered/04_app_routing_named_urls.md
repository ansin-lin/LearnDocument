# 第4章 App 路由与命名路由

## 一、本章完成目标

本章把员工功能的 URL 从项目总路由拆到 `employees` App。完成后，你应能够：

- 区分项目级路由与 App 级路由
- 使用 `include()` 接入 App 路由
- 使用路径参数建立详情入口
- 使用 `app_name` 和 `name` 组成命名空间路由
- 使用 `reverse()` 生成 URL
- 根据404和导入错误定位路由问题

## 二、本章开始状态

第3章完成后应有：

```text
company_portal/urls.py
employees/views.py
```

根路径 `/` 已经能够显示自定义响应。

同时确认以下地址仍可访问：

```text
/          → 自定义首页
/health/   → OK
```

本章新增：

```text
employees/urls.py
```

完成后请求链变为：

```text
GET /employees/1001/
  ↓
company_portal/urls.py
  ↓ include("employees.urls")
employees/urls.py
  ↓
employee_detail(request, employee_id=1001)
```

## 三、为什么拆分 App 路由

如果所有业务 URL 都写在 `company_portal/urls.py`，项目变大后会混在一起：

- 员工页面
- 部门页面
- 登录页面
- 日志页面

项目级路由负责第一层分发，App 级路由负责当前业务内部的地址。

```text
项目级路由
├── admin/       → Django Admin
└── employees/   → employees.urls
```

## 四、补充员工视图

打开：

```text
employees/views.py
```

文件顶部已经导入 `HttpRequest` 和 `HttpResponse`，保留第3章完成的 `home()` 和 `health()`，在文件末尾追加：

```python
def employee_list(request: HttpRequest) -> HttpResponse:
    return HttpResponse("员工列表页面")


def employee_detail(
    request: HttpRequest,
    employee_id: int,
) -> HttpResponse:
    return HttpResponse(f"员工详情：{employee_id}")
```

`employee_detail()` 有两个参数：

| 参数 | 来源 |
| --- | --- |
| `request` | Django 创建的请求对象 |
| `employee_id` | URL 中匹配到的整数 |

## 五、创建 employees App 路由

新建：

```text
employees/urls.py
```

内容：

```python
from django.urls import path

from . import views


app_name = "employees"

urlpatterns = [
    path("", views.employee_list, name="list"),
    path("<int:employee_id>/", views.employee_detail, name="detail"),
]
```

`from . import views` 中的 `.` 表示当前 `employees` Python 包，因此这里导入的是 `employees/views.py`。

### 5.1 空字符串路径

App 路由中的：

```python
path("", views.employee_list, name="list")
```

表示匹配项目路由已经去掉 `employees/` 后剩余的空路径。

### 5.2 整数路径参数

```python
path("<int:employee_id>/", views.employee_detail, name="detail")
```

可以匹配：

```text
/employees/1001/
```

Django 把 `1001` 转换成整数，并用参数名 `employee_id` 传给视图。

下面地址不会匹配整数转换器：

```text
/employees/abc/
```

因此会返回404，而不是进入视图后再判断。

### 5.3 `name`

每条路由的 `name` 是它在当前 URL 配置中的名称：

```python
path("", views.employee_list, name="list")
```

这里的路径是 `""`，路由名称是 `"list"`。路由名称不是浏览器访问路径，也不会自动调用视图；它用于从代码或模板反向生成路径。

### 5.4 `app_name`

```python
app_name = "employees"
```

为当前 App 路由设置应用命名空间。它与每条路由的 `name` 通过冒号组成完整名称：

```text
employees:list
employees:detail
```

其中 `employees` 来自 `app_name`，`list` 和 `detail` 来自对应 `path()` 的 `name`。即使其他 App 也有 `list` 路由，也可以使用不同命名空间区分。

## 六、在项目路由中使用 include

打开：

```text
company_portal/urls.py
```

修改为：

```python
from django.contrib import admin
from django.urls import include, path

from employees import views


urlpatterns = [
    path("admin/", admin.site.urls),
    path("", views.home, name="home"),
    path("health/", views.health, name="health"),
    path("employees/", include("employees.urls")),
]
```

与第3章相比，首页和健康检查路由也增加了 `name="home"`、`name="health"`。它们属于项目级路由，没有使用 `employees` 命名空间；本章重点使用的是 `employees:list` 和 `employees:detail`。

`include("employees.urls")` 的作用是：

1. 匹配 URL 开头的 `employees/`。
2. 去掉已经匹配的部分。
3. 把剩余路径交给 `employees/urls.py`。

## 七、运行并验证路由

先检查配置：

```powershell
python manage.py check
```

再启动：

```powershell
python manage.py runserver
```

### 7.1 员工列表

访问：

```text
http://127.0.0.1:8000/employees/
```

预期：

```text
员工列表页面
```

### 7.2 员工详情

访问：

```text
http://127.0.0.1:8000/employees/1001/
```

预期：

```text
员工详情：1001
```

### 7.3 非整数参数

访问：

```text
http://127.0.0.1:8000/employees/abc/
```

预期状态码为404。这是路由规则主动拒绝不符合格式的路径。

### 7.4 回归确认

再次访问 `/` 和 `/health/`，确认拆分员工路由后，上一章的首页和健康检查仍然正常。新增功能时也要确认原有功能没有被破坏。

## 八、使用命名路由反向生成 URL

硬编码：

```text
/employees/1001/
```

会把路径结构散落在代码和模板中。命名路由允许 Django 根据名称生成地址。

打开另一个终端，激活同一虚拟环境，在项目目录执行：

```powershell
python manage.py shell
```

输入：

```python
from django.urls import reverse

reverse("employees:list")
reverse("employees:detail", args=[1001])
```

预期结果：

```text
'/employees/'
'/employees/1001/'
```

`reverse()` 接收路由名称以及路径参数，返回一个 URL 字符串。它不会向服务器发送请求，也不会调用 `employee_list()` 或 `employee_detail()`。

当前存在两个相反方向：

```text
浏览器路径 /employees/1001/  → URL 匹配 → employee_detail()
路由名称 employees:detail    → reverse() → /employees/1001/
```

输入：

```python
exit()
```

退出 Shell。

第5章会在模板中使用相同路由名生成详情链接。

## 九、URL 末尾的斜线

本课程统一使用：

```text
/employees/
/employees/1001/
```

对应路由也保留末尾斜线：

```python
path("<int:employee_id>/", ...)
```

团队项目应统一风格，不在相似页面中混用带斜线和不带斜线的地址。

## 十、常见错误与调查方法

### 10.1 `ModuleNotFoundError: No module named 'employees.urls'`

检查：

- 是否真的创建 `employees/urls.py`
- 文件名是否误写为 `url.py`
- 当前启动的是否是正确项目

### 10.2 `NameError: name 'include' is not defined`

确认导入：

```python
from django.urls import include, path
```

### 10.3 详情视图提示缺少参数

路由参数名必须与视图参数名一致：

```python
path("<int:employee_id>/", views.employee_detail, name="detail")

def employee_detail(request: HttpRequest, employee_id: int) -> HttpResponse:
    ...
```

如果一边写 `id`，另一边写 `employee_id`，Django 无法按关键字传递参数。

例如路由改为 `employee_pk`、视图仍保留 `employee_id` 时，访问详情页会出现类似错误：

```text
TypeError: employee_detail() got an unexpected keyword argument 'employee_pk'
```

### 10.4 `NoReverseMatch`

下面的详情路由需要 `employee_id`，因此只写路由名称还不够：

```python
reverse("employees:detail")
```

Django 会报告 `NoReverseMatch`。应传入详情编号：

```python
reverse("employees:detail", args=[1001])
```

检查：

- `app_name` 和 `name` 是否拼写正确
- 是否遗漏路径参数
- 参数数量和格式是否符合路由规则

## 十一、练习

### 练习1：增加新增入口路由

在 `employees/views.py` 添加名为 `employee_create` 的临时视图，让它返回“员工新增页面”。在 App 路由的详情规则之前配置：

```text
/employees/new/
```

路由名使用：

```text
employees:create
```

验证浏览器状态码和 `reverse("employees:create")`。

这是反向解析练习，不是本章正式功能。验证完成后删除临时视图和 `new/` 路由；第9章会实现真正的员工新增页面。

### 练习2：解释两层路由

使用当前真实文件说明 `/employees/1001/` 经过哪两个 `urls.py`，每一层分别匹配哪一段。

### 练习3：修复参数名不一致

只把路由参数暂时改为 `employee_pk`，保持视图参数不变，访问详情页并记录错误。然后把路由参数恢复为 `employee_id`，确认 `/employees/1001/` 再次返回200。

## 十二、本章完成检查

本章结束时不保留练习中的临时 `new/` 路由。正式路由状态如下：

| 地址 | 视图 | 路由名称 |
| --- | --- | --- |
| `/` | `home` | `home` |
| `/health/` | `health` | `health` |
| `/employees/` | `employee_list` | `employees:list` |
| `/employees/<employee_id>/` | `employee_detail` | `employees:detail` |

- [ ] `employees/urls.py` 已创建
- [ ] 项目路由使用 `include()` 接入员工路由
- [ ] `/employees/` 返回列表响应
- [ ] `/employees/1001/` 返回包含1001的详情响应
- [ ] `/employees/abc/` 返回404
- [ ] `/` 和 `/health/` 仍然正常
- [ ] 能使用 `reverse()` 生成列表和详情地址
- [ ] 能说明 `app_name` 与 `name` 的组合关系
- [ ] 已删除练习中的临时新增视图和路由

## 十三、本章总结

## 十四、反向解析的三种常见写法

命名路由的价值是避免在 Python 和模板里写死 URL。下面三种写法最终都依赖 `app_name`、`name` 和参数保持一致：

```python
from django.shortcuts import redirect
from django.urls import reverse

detail_url = reverse("employees:detail", kwargs={"employee_id": 12})
return redirect("employees:detail", employee_id=12)
```

```html
<a href="{% url 'employees:detail' employee_id=employee.pk %}">查看</a>
```

也可以使用位置参数 `args=[12]` 或 `{% url 'employees:detail' employee.pk %}`。同一项目应遵守已有风格；参数较多时，关键字参数通常更容易读懂。

## 十五、现场路由调查法

收到“这个页面在哪里处理”的问题时，按下面顺序调查：

1. 从浏览器 Network 记录方法、完整路径和状态码。
2. 在项目 `urls.py` 找到最外层 `include()`。
3. 在 App `urls.py` 找到匹配的 `path()`、路由名和参数。
4. 进入对应 View，再追踪模板、Form、Model 或 Serializer。
5. 修改后同时验证正向地址、无效参数、404 和反向解析。

自定义 path converter 适合复用复杂且稳定的路径规则，本课程只要求能识读。普通整数、字符串、slug、UUID 路径优先使用 Django 内置 converter，避免过早增加维护成本。

## 十六、路由设计的企业约定

- URL 表达资源或操作意图，命名保持一致。
- 项目路由负责入口和 App 分发，业务路由留在 App。
- 不让固定路径被宽泛的动态路径抢先匹配。
- 改路由名时搜索 `reverse()`、`redirect()`、`{% url %}`、测试和前端调用方。
- 不能用“页面能打开”替代 404、权限和 HTTP 方法验证。

- 项目级路由负责业务模块入口，App 级路由负责模块内部地址
- `include()` 把匹配后的剩余路径交给另一个 URL 配置
- 路径转换器可以限制参数格式并把值传给视图
- 命名空间和路由名称提供稳定的反向解析入口
- 第5章会使用这些路由渲染带详情链接的员工列表
