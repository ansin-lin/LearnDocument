# 第12章 登录与 Session

## 本章成果

用户可以登录、退出；未登录用户访问员工页面时会被送到登录页，登录后返回原地址。

## 本章开始状态与修改清单

第11章 CRUD、搜索和分页均可匿名访问。本章修改 `settings.py` 和项目总路由，新建 `templates/registration/login.html`，并给员工 View 加登录限制。若继续使用 App 模板目录，也可以放在 `employees/templates/registration/login.html`，但全项目只保留一份。

## 使用 Django 内置认证

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

创建 `employees/templates/registration/login.html`：

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

## 完成检查

- [ ] 使用 Django 密码系统，不自行保存明文密码
- [ ] 退出使用 POST
- [ ] 页面入口和直接 URL 都受到登录限制

下一章在“已经登录”的基础上区分可查看、可新增、可修改的角色。

## 读懂认证代码的两种形式

内置 `LoginView` 适合标准页面；既有项目也可能直接调用：

```python
from django.contrib.auth import authenticate, login, logout

user = authenticate(request, username=username, password=password)
if user is not None:
    login(request, user)

logout(request)
```

`authenticate()` 验证凭据并返回用户或 `None`，`login()` 把身份写入 Session，`logout()` 清除当前登录状态。密码由安全哈希保存，不能解密回原密码，也不能自己用普通哈希代替 Django 密码处理器。

## Cookie、Session 与 `request.user`

浏览器 Cookie 通常只保存 Session 标识，服务端 Session 保存状态。中间件根据它恢复 `request.user`；未登录时是 `AnonymousUser`，不要假设它为 `None`。

```python
request.session["employee_list_size"] = 20
page_size = request.session.get("employee_list_size", 10)
```

Session 适合少量服务端状态，不用于无限堆积业务数据。生产需要 HTTPS，并正确配置 Secure、HttpOnly、SameSite、过期时间和 Session 存储。登录解决“你是谁”，第13章授权解决“你能做什么”。
