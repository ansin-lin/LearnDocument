# 第12章 登录与 Session

## 本章成果

用户可以登录、退出；未登录用户访问员工页面时会被送到登录页，登录后返回原地址。

## 本章开始状态与修改清单

第11章 CRUD、搜索和分页均可匿名访问。本章修改 `settings.py` 和项目总路由，新建 `templates/registration/login.html`，并给员工 View 加登录限制。课程从第5章开始统一使用项目级 `templates/`，不要再在 App 内创建第二份同名登录模板。

## 使用 Django 内置认证

- **Cookie 是什么**：浏览器为某个站点保存并在后续请求中携带的小段数据。
- **Session 是什么**：服务器保存的会话状态，浏览器通常只持有对应的 Session 标识。
- **为什么需要**：HTTP 请求本身彼此独立，系统需要在多次请求之间识别同一登录用户。
- **什么时候使用**：登录成功后保存身份状态；每次受保护请求通过 `request.user` 判断当前用户。

```text
用户名和密码
→ authenticate() 验证
→ login() 建立 Session
→ 浏览器保存 Session Cookie
→ 后续请求携带 Cookie
→ Django 恢复 request.user
```

`authenticate()` 负责验证凭据并返回用户或 `None`，`login()` 把已验证身份写入Session。当前先理解它们在流程中的职责；本章“认证代码的两种形式”会说明完整参数和返回行为。

先在 `company_portal/settings.py` 设置：

```python
LOGIN_URL = "login"
LOGIN_REDIRECT_URL = "employees:list"
LOGOUT_REDIRECT_URL = "login"
```

在项目总路由加入：

```python
from django.contrib.auth import views as auth_views

urlpatterns = [
    path("login/", auth_views.LoginView.as_view(template_name="registration/login.html"), name="login"),
    path("logout/", auth_views.LogoutView.as_view(), name="logout"),
    # 原有路由……
]
```

三个 settings 分别决定未登录跳转地址、登录成功默认地址和退出后地址。`LoginView.as_view()` 把 Django 提供的登录类视图转换为可注册到路由的 View；`template_name` 指定当前项目自己的登录模板。`LogoutView` 负责清除当前 Session。

| API或配置 | 当前参数 | 可接受的值与必填性 | 返回值或行为 |
|---|---|---|---|
| `LoginView.as_view(**initkwargs)` | `template_name` | 可传该类允许的配置；模板路径为字符串 | 返回可交给 `path()` 的View函数 |
| `LogoutView.as_view()` | 无 | 当前不传参数 | 返回退出View；收到允许的请求后清除登录Session |
| `LOGIN_URL` | 登录目标 | URL或路由名，项目应配置 | `login_required` 未通过时的跳转目标 |
| `LOGIN_REDIRECT_URL` | 登录后目标 | URL或路由名，项目应配置 | 登录请求没有 `next` 时的默认目标 |
| `LOGOUT_REDIRECT_URL` | 退出后目标 | URL、路由名或 `None` | 退出成功后的目标 |

本课程退出只接受POST表单，不使用普通链接触发状态变化。

创建 `templates/registration/login.html`：

```html
{% extends "base.html" %}
{% block content %}
  <h1>登录</h1>
  {% if form.errors %}<p>用户名或密码不正确。</p>{% endif %}
  <form method="post">
    {% csrf_token %}
    {{ form.as_p }}
    {% if next %}<input type="hidden" name="next" value="{{ next }}">{% endif %}
    <button type="submit">登录</button>
  </form>
{% endblock %}
```

退出会改变登录状态，应使用 POST：

```html
{% if user.is_authenticated %}
  <span>{{ user.username }}</span>
  <form method="post" action="{% url 'logout' %}">
    {% csrf_token %}<button type="submit">退出</button>
  </form>
{% endif %}
```

## 限制员工页面

在每个员工 View 上增加：

```python
from django.contrib.auth.decorators import login_required


@login_required
def employee_list(request):
    ...
```

`@login_required` 不带括号时直接装饰View；匿名请求会返回指向 `LOGIN_URL` 的重定向响应，并把原地址放进默认参数 `next`，已登录请求则继续调用原View。它返回包装后的View函数，不改变原函数参数。

模板隐藏链接不是权限控制；直接输入 URL 仍可能访问，所以限制必须在后端执行。

## Session 是什么

登录成功后，浏览器保存一个 Session cookie；用户身份数据主要保存在服务端。不要把密码、完整个人信息或大对象塞进 Session，也不要在日志中输出 cookie。

## 必测场景

1. 未登录访问 `/employees/`，跳到登录页并带 `next`。
2. 登录后返回原页面。
3. POST 退出后再次访问员工页需要重新登录。
4. 密码错误时不泄露“用户名是否存在”。

## 课堂任务

使用两个浏览器会话分别验证匿名状态和登录状态。Network 中应观察到匿名请求302、登录页200、登录POST 302、目标页200。退出后不要只看页面按钮，应重新直接访问员工 URL。

现场报告：`未ログイン時はログイン画面へ遷移し、ログイン後は元のURLへ戻ることを確認しました。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 登录功能运行检查

- [ ] 使用 Django 密码系统，不自行保存明文密码
- [ ] 退出使用 POST
- [ ] 页面入口和直接 URL 都受到登录限制

## 现场识读：认证代码的两种形式

内置 `LoginView` 适合标准页面；既有项目也可能直接调用：

```python
from django.contrib.auth import authenticate, login, logout

user = authenticate(request, username=username, password=password)
if user is not None:
    login(request, user)

logout(request)
```

`authenticate()` 验证凭据并返回用户或 `None`，`login()` 把身份写入 Session，`logout()` 清除当前登录状态。密码由安全哈希保存，不能解密回原密码，也不能自己用普通哈希代替 Django 密码处理器。

`authenticate(request=None, **credentials)` 的请求可选，凭据名称取决于认证后端；默认后端使用 `username` 和 `password`。`login(request, user, backend=None)` 需要当前请求和已认证用户，成功后更新Session，业务代码不依赖返回值。`logout(request)` 需要当前请求并清空当前Session，返回 `None`。

## Cookie、Session 与 `request.user`

浏览器 Cookie 通常只保存 Session 标识，服务端 Session 保存状态。中间件根据它恢复 `request.user`；未登录时是 `AnonymousUser`，不要假设它为 `None`。

- **AnonymousUser是什么**：Django为未登录请求提供的匿名用户对象。
- **为什么需要**：View和模板可以统一访问 `request.user`，无需先判断对象是否存在。
- **什么时候使用**：通过 `is_authenticated` 判断身份时；匿名对象没有正常用户主键，也不应当作已授权用户。

密码哈希是从密码计算出的不可逆验证值。Django保存带算法和参数的哈希而不是明文，需要它来降低数据库泄漏后的直接暴露风险；创建用户、修改密码和认证时必须使用Django提供的密码接口。

```python
request.session["employee_list_size"] = 20
page_size = request.session.get("employee_list_size", 10)
```

`request.session` 提供类似字典的接口。赋值会标记Session需要保存；`get(key, default=None)` 在键存在时返回保存值，不存在时返回默认值。Session值必须可被当前Session序列化器处理，且只保存完成请求所需的少量状态。

Session 适合少量服务端状态，不用于无限堆积业务数据。生产需要 HTTPS，并正确配置 Secure、HttpOnly、SameSite、过期时间和 Session 存储。登录解决“你是谁”，第13章授权解决“你能做什么”。

## 现场识读：自定义用户模型

课程项目直接使用 Django 默认 `User`。企业项目经常通过 `AUTH_USER_MODEL` 使用自定义用户模型，因此关联用户时优先采用可替换写法：

```python
from django.conf import settings

uploaded_by = models.ForeignKey(
    settings.AUTH_USER_MODEL,
    on_delete=models.PROTECT,
)
```

业务代码需要用户类时使用 `get_user_model()`，不要到处直接导入默认 `User`：

```python
from django.contrib.auth import get_user_model

User = get_user_model()
```

`get_user_model()` 不接参数，返回当前 `AUTH_USER_MODEL` 指向的用户类；业务代码需要调用用户Manager或做类型关联时使用，避免写死默认 `User`。

是否自定义用户模型应在项目初期决定。已有迁移和数据后再更换会影响外键、迁移和认证流程，不应作为普通字段修改处理。进入既有项目时先搜索 `AUTH_USER_MODEL`、登录入口、认证后端和用户创建方式。

## 本章总结

Django 认证系统负责验证身份并通过 Session 恢复 `request.user`。密码必须交给 Django 的密码系统处理，退出使用 POST。下一章在“已经登录”的基础上区分可查看、可新增、可修改的角色。

## 日本项目中的实际使用

企业项目优先使用框架提供的密码哈希、Session 和认证流程，不自行保存或比较明文密码。测试时会区分未登录、登录成功、账号无效和退出后的状态。生产环境还要结合 HTTPS、安全 Cookie、超时和账号管理规则。

## 新人常见错误

- 把密码直接保存到普通字段，绕过 Django 密码哈希系统。
- 认为 Cookie 中保存了完整用户信息；默认 Session 模式通常只保存标识。
- 只隐藏菜单，没有给 View 加 `login_required`，手工输入 URL 仍可访问。
- 退出使用 GET，使链接预取或误点击改变登录状态；应使用 POST。
- 把 `AnonymousUser` 当成 `None`，导致判断逻辑错误。

## 本章知识将在后续章节继续使用

```text
Cookie
→ Session
→ request.user
→ login_required
→ 第13章 Group / Permission
→ 第14章附件访问控制
→ 第22章 API 认证
```
