# 第5章 Template 与静态员工列表

## 一、本章完成目标

本章把写在 `HttpResponse` 中的文字升级为真正的 HTML 页面。完成后，你应能够：

- 使用 `render()` 把数据交给模板
- 使用模板变量、循环和 `{% empty %}`
- 使用模板继承建立统一页面结构
- 使用 `{% url %}` 生成命名路由链接
- 使用 `{% static %}` 加载 App 静态 CSS
- 显示带样式的静态员工列表
- 调查模板和静态文件常见问题

本章仍然使用固定员工数据。第6章建立 Model 和数据库后，再把固定数据替换为 ORM 查询结果。

## 二、本章开始状态

项目应已经存在：

```text
company_portal/urls.py
employees/urls.py
employees/views.py
```

并且可以访问：

```text
/employees/
/employees/1001/
```

本章新增：

```text
employees/
├── static/
│   └── employees/
│       └── css/
│           └── style.css
└── templates/
    ├── base.html
    └── employees/
        └── list.html
```

模板和静态资源都使用 App 名作为中间目录，避免多个 App 出现同名文件时互相覆盖。

## 三、准备静态员工数据

打开：

```text
employees/views.py
```

确认顶部导入并修改 `employee_list()`：

```python
from django.http import HttpRequest, HttpResponse
from django.shortcuts import render


def employee_list(request: HttpRequest) -> HttpResponse:
    employees = [
        {
            "id": 1001,
            "employee_code": "EMP-1001",
            "name": "山田 太郎",
            "email": "taro.yamada@example.com",
            "department": "営業部",
            "status": "ACTIVE",
        },
        {
            "id": 1002,
            "employee_code": "EMP-1002",
            "name": "佐藤 花子",
            "email": "hanako.sato@example.com",
            "department": "人事部",
            "status": "LEAVE",
        },
        {
            "id": 1003,
            "employee_code": "EMP-1003",
            "name": "鈴木 一郎",
            "email": "ichiro.suzuki@example.com",
            "department": "開発部",
            "status": "RETIRED",
        },
    ]

    context = {
        "employees": employees,
    }
    return render(request, "employees/list.html", context)
```

`render()` 的三个常用参数：

| 参数 | 当前值 | 作用 |
| --- | --- | --- |
| 请求 | `request` | 当前请求对象 |
| 模板 | `"employees/list.html"` | 要使用的模板路径 |
| 上下文 | `context` | 交给模板的数据字典 |

返回值仍然是 `HttpResponse`。区别是响应体由模板生成，而不是直接写在 Python 字符串中。

## 四、创建基础模板

新建：

```text
employees/templates/base.html
```

内容：

```html
{% load static %}
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>{% block title %}员工管理系统{% endblock %}</title>
    <link rel="stylesheet" href="{% static 'employees/css/style.css' %}">
</head>
<body>
    <header class="site-header">
        <div class="container">
            <a class="brand" href="{% url 'employees:list' %}">员工管理系统</a>
        </div>
    </header>

    <main class="container">
        {% block content %}{% endblock %}
    </main>
</body>
</html>
```

关键语法：

| 语法 | 作用 |
| --- | --- |
| `{% load static %}` | 启用静态文件标签 |
| `{% block title %}` | 允许子模板替换页面标题 |
| `{% block content %}` | 允许子模板填入主要内容 |
| `{% static '...' %}` | 根据静态文件配置生成 URL |
| `{% url 'employees:list' %}` | 根据命名路由生成 URL |

基础模板保存所有页面共享的 HTML 结构。后续页面不需要重复写 `doctype`、`head` 和顶部导航。

## 五、创建员工列表模板

新建：

```text
employees/templates/employees/list.html
```

内容：

```html
{% extends "base.html" %}

{% block title %}员工列表 | 员工管理系统{% endblock %}

{% block content %}
<section class="page-heading">
    <div>
        <p class="eyebrow">社員一覧</p>
        <h1>员工列表</h1>
        <p>当前使用固定数据。后续章节会切换到数据库查询。</p>
    </div>
</section>

<div class="table-wrap">
    <table>
        <thead>
            <tr>
                <th scope="col">员工编号</th>
                <th scope="col">姓名</th>
                <th scope="col">邮箱</th>
                <th scope="col">部门</th>
                <th scope="col">状态</th>
                <th scope="col">操作</th>
            </tr>
        </thead>
        <tbody>
            {% for employee in employees %}
            <tr>
                <td>{{ employee.employee_code }}</td>
                <td>{{ employee.name }}</td>
                <td>{{ employee.email }}</td>
                <td>{{ employee.department }}</td>
                <td>
                    <span class="status status--{{ employee.status|lower }}">
                        {{ employee.status }}
                    </span>
                </td>
                <td>
                    <a href="{% url 'employees:detail' employee.id %}">查看详情</a>
                </td>
            </tr>
            {% empty %}
            <tr>
                <td class="empty-cell" colspan="6">没有符合条件的员工</td>
            </tr>
            {% endfor %}
        </tbody>
    </table>
</div>
{% endblock %}
```

## 六、理解模板中的数据读取

视图中的一个员工是字典：

```python
{
    "employee_code": "EMP-1001",
    "name": "山田 太郎",
}
```

模板使用点号读取：

```django
{{ employee.employee_code }}
{{ employee.name }}
```

Django 模板会处理字典键和对象属性。第8章把字典换成 Model 对象后，模板仍然可以使用相似写法。

## 七、理解循环与空数据

循环：

```django
{% for employee in employees %}
    {{ employee.name }}
{% endfor %}
```

空数据分支：

```django
{% empty %}
    没有符合条件的员工
```

把视图中的列表临时改为空列表：

```python
employees = []
```

刷新页面，应显示空数据提示而不是空白表格。验证后恢复三条员工数据。

## 八、理解过滤器

模板中使用：

```django
{{ employee.status|lower }}
```

`lower` 把 `ACTIVE` 转成 `active`，用于生成 CSS 类：

```text
status--active
```

过滤器适合简单显示转换，不应承载复杂业务规则。状态权限、数据合法性等规则仍由 Python 代码和数据库约束处理。

## 九、创建静态 CSS

新建：

```text
employees/static/employees/css/style.css
```

内容：

```css
:root {
    color-scheme: light;
    font-family: "Segoe UI", "Noto Sans JP", sans-serif;
    color: #1f2937;
    background: #f3f4f6;
}

* {
    box-sizing: border-box;
}

body {
    margin: 0;
    background: #f3f4f6;
}

a {
    color: #1d4ed8;
}

.container {
    width: min(1120px, calc(100% - 32px));
    margin-inline: auto;
}

.site-header {
    padding: 16px 0;
    color: #ffffff;
    background: #1e3a5f;
}

.brand {
    color: inherit;
    font-size: 1.1rem;
    font-weight: 700;
    text-decoration: none;
}

.page-heading {
    display: flex;
    justify-content: space-between;
    gap: 24px;
    align-items: end;
    padding: 40px 0 20px;
}

.page-heading h1 {
    margin: 4px 0 8px;
}

.eyebrow {
    margin: 0;
    color: #475569;
    font-size: 0.85rem;
    font-weight: 700;
    letter-spacing: 0.08em;
}

.table-wrap {
    overflow-x: auto;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    background: #ffffff;
}

table {
    width: 100%;
    border-collapse: collapse;
}

th,
td {
    padding: 12px 16px;
    border-bottom: 1px solid #e5e7eb;
    text-align: left;
    white-space: nowrap;
}

th {
    color: #334155;
    background: #f8fafc;
}

.status {
    display: inline-block;
    padding: 4px 8px;
    border-radius: 999px;
    font-size: 0.8rem;
    font-weight: 700;
}

.status--active {
    color: #166534;
    background: #dcfce7;
}

.status--leave {
    color: #854d0e;
    background: #fef9c3;
}

.status--retired {
    color: #475569;
    background: #e2e8f0;
}

.empty-cell {
    padding: 32px;
    color: #64748b;
    text-align: center;
}
```

这里使用横向滚动保护窄屏表格，避免列被强行压缩到无法阅读。

## 十、运行并验证

执行：

```powershell
python manage.py check
python manage.py runserver
```

访问：

```text
http://127.0.0.1:8000/employees/
```

应看到：

- 蓝色顶部区域
- 员工列表标题
- 三条员工数据
- 不同颜色的状态标签
- 每行的详情链接

点击第一行详情，应访问：

```text
/employees/1001/
```

当前详情页仍返回第4章的简单文本，这是预期状态。第8章接入数据库时再完成正式详情模板。

## 十一、使用浏览器检查静态文件

打开开发者工具 Network 面板，刷新页面并查找：

```text
style.css
```

确认状态码为200。也可以打开 Elements 面板，确认最终链接类似：

```html
<link rel="stylesheet" href="/static/employees/css/style.css">
```

不要把 `/static/...` 直接硬编码进模板；使用 `{% static %}` 可以让路径跟随配置。

## 十二、常见错误与调查方法

### 12.1 `TemplateDoesNotExist`

检查文件是否位于：

```text
employees/templates/employees/list.html
```

以及 `render()` 是否写：

```python
render(request, "employees/list.html", context)
```

### 12.2 模板显示空白变量

检查上下文键：

```python
context = {"employees": employees}
```

模板循环变量也应使用 `employees`。Django 模板读取不到变量时经常显示空内容，不一定抛出异常。

### 12.3 CSS 没有加载

按顺序确认：

1. `base.html` 顶部有 `{% load static %}`。
2. 路径是 `employees/static/employees/css/style.css`。
3. `INSTALLED_APPS` 中已注册 `employees`。
4. Network 中 `style.css` 的状态码不是404。
5. 浏览器是否使用缓存；必要时强制刷新。

### 12.4 `NoReverseMatch`

检查：

- `employees/urls.py` 是否有 `app_name = "employees"`
- 路由名是否是 `detail`
- 模板是否传入 `employee.id`

正确写法：

```django
{% url 'employees:detail' employee.id %}
```

## 十三、练习

### 练习1：增加入职日期列

为每条固定员工数据增加 `joined_date`，并在表格中显示“入职日期”。完成后确认表头、数据列和空数据 `colspan` 同步更新。

### 练习2：增加状态显示文本

在固定数据中增加 `status_label`：

```text
在职
休职
离职
```

页面显示中文状态，同时继续使用 `status` 生成 CSS 类。不要在模板中编写复杂状态判断。

### 练习3：排查静态文件404

暂时把 CSS 文件名改错，使用 Network 面板确认404，再恢复正确文件名并确认200。记录你检查的路径。

### 练习4：验证空数据页面

把员工列表暂时设为空，确认页面出现明确提示；恢复数据后再次验证详情链接。

## 十四、本章完成检查

- [ ] 列表视图使用 `render()`
- [ ] `base.html` 和员工列表模板路径正确
- [ ] 页面显示三条固定员工数据
- [ ] 空列表显示明确提示
- [ ] 详情链接通过命名路由生成
- [ ] CSS 文件请求返回200
- [ ] 窄屏时表格可以横向滚动
- [ ] 能使用错误信息和 Network 面板排查模板或静态文件问题

## 十五、本章总结

## 十六、模板中还必须会读的语法

```html
{% if employee.is_active %}
  <span>在职</span>
{% else %}
  <span>离职</span>
{% endif %}

{% include "employees/_employee_row.html" with employee=employee %}

{% comment %}这段说明不会发送给浏览器{% endcomment %}
```

`extends` 建立页面骨架，`block` 让子模板替换区域，`include` 复用局部片段。不要把查询数据库或复杂业务判断塞进模板；View 应准备好页面需要的数据。

常见过滤器包括：`date` 格式化日期、`default` 提供空值显示、`length` 读取长度。`safe` 会把字符串标记为可信 HTML，应只用于服务端明确生成并清洗过的内容。用户输入默认自动转义，这是重要的 XSS 防线，不要为了显示 HTML 随意关闭 `autoescape`。

## 十七、Static 的完整最小认识

`STATIC_URL` 定义静态资源对外 URL 前缀，`{% static %}` 根据配置生成地址。开发阶段 `runserver` 可协助提供静态文件；生产环境通常由构建/收集步骤和 Web 服务器或对象存储提供，第17章会学习 `collectstatic`。

```html
{% load static %}
<link rel="stylesheet" href="{% static 'employees/css/style.css' %}">
<script src="{% static 'employees/js/list.js' %}" defer></script>
<img src="{% static 'employees/images/company-mark.svg' %}" alt="">
```

CSS、JavaScript 和图片都属于 Static；员工上传的附件属于 Media，两者不能混用。页面脚本首次加入时，用 Network 确认资源为200，并用 Console 确认没有语法错误。

## 十八、模板排错的三层证据

1. View 的 context 中是否存在正确变量。
2. 页面源代码/Elements 中是否生成了预期 HTML。
3. Network 中 CSS、JS、图片是否请求成功。

如果页面文字正确但样式不对，优先调查静态资源与 CSS；如果变量为空，调查 context 名称和数据；如果模板根本没打开，调查模板路径和 settings。把不同层的问题分开，能显著缩短现场调查时间。

- View 通过 context 把数据交给 Template
- 模板变量、循环和空数据分支负责生成不同页面内容
- 模板继承减少共享 HTML 重复
- 命名路由避免在模板中硬编码 URL
- App 静态目录和 `{% static %}` 负责加载 CSS
- 第一阶段已经形成可见员工列表，第6章会建立数据库模型和迁移
