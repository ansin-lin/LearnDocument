# 第14章 Web 基础与 Flask 入门

> 学习目标
>
> - 理解 HTTP 协议、请求与响应的基本概念
> - 掌握 Flask 框架的基本使用方式
> - 能够创建路由、渲染模板、管理静态资源
> - 实现表单提交与 API 返回数据

---

## 一、HTTP 协议基础

### HTTP 概念

HTTP（HyperText Transfer Protocol）是 Web 世界中 **客户端与服务器通信的协议**。  
浏览器（客户端）通过 HTTP 向服务器发送 **请求（Request）**，服务器返回 **响应（Response）**。

#### 请求组成

- 请求行（如 `GET /index.html HTTP/1.1`）  
- 请求头（Header）  
- 请求体（Body，可选）

#### 响应组成

- 状态行（如 `HTTP/1.1 200 OK`）  
- 响应头（Header）  
- 响应体（Body）

---

### 常见请求方法

| 方法 | 说明 |
|------|------|
| GET | 获取资源（访问网页、下载文件） |
| POST | 提交数据（表单提交、接口上传） |
| PUT | 更新资源 |
| DELETE | 删除资源 |

---

## 二、Flask 框架简介

Flask 是一个轻量级 Web 框架，遵循 MVC 思想，常用于快速搭建网站与 RESTful API。

### 安装

```bash
pip install flask
```

### 快速示例

```python
from flask import Flask

app = Flask(__name__)

@app.route("/")
def home():
    return "Hello, Flask!"

if __name__ == "__main__":
    app.run(debug=True)
```

运行后访问：<http://127.0.0.1:5000/>

✅ **debug=True**：可自动重载并显示详细错误信息。

---

## 三、Flask 核心模块与常用函数

| 模块 / 函数 | 功能说明 |
|---------------|-----------|
| `Flask()` | 创建应用对象 |
| `@app.route()` | 定义路由 |
| `render_template()` | 渲染 HTML 模板 |
| `request` | 访问请求数据 |
| `redirect()` | 页面跳转 |
| `url_for()` | 动态生成链接 |
| `jsonify()` | 返回 JSON 响应 |

---

## 四、Flask 项目结构

```python
my_flask_app/
 ├── app.py
 ├── templates/
 │   ├── index.html
 │   └── result.html
 ├── static/
 │   ├── css/
 │   │   └── style.css
 │   └── img/
 │       └── logo.png
```

- `templates/`：存放 HTML 模板文件
- `static/`：存放静态资源（CSS/JS/图片）

---

## 五、路由与模板渲染

### 定义路由

```python
from flask import Flask, render_template
app = Flask(__name__)

@app.route("/")
def home():
    return render_template("index.html", title="首页")
```

### 模板示例（templates/index.html）

```html
<!DOCTYPE html>
<html>
<head>
    <title>{{ title }}</title>
    <link rel="stylesheet" href="{{ url_for('static', filename='css/style.css') }}">
</head>
<body>
    <h1>欢迎使用 Flask！</h1>
    <p>今天是：{{ date }}</p>
</body>
</html>
```

✅ Flask 模板引擎使用 **Jinja2**：通过 `{{ 变量 }}` 与 `{% 控制语句 %}` 进行动态渲染。

---

## 六、静态文件管理

Flask 自动映射 `static/` 目录下的文件：  

```html
<link rel="stylesheet" href="{{ url_for('static', filename='css/style.css') }}">
<img src="{{ url_for('static', filename='img/logo.png') }}" alt="Logo">
```

示例：`static/css/style.css`

```css
body {
    font-family: Arial, sans-serif;
    color: #333;
}
```

---

## 七、表单提交与请求处理

### 表单页面（templates/form.html）

```html
<form method="POST" action="/submit">
  姓名：<input type="text" name="username"><br>
  邮箱：<input type="email" name="email"><br>
  <input type="submit" value="提交">
</form>
```

### Flask 处理 POST 请求

```python
from flask import request, render_template

@app.route("/submit", methods=["GET", "POST"])
def submit():
    if request.method == "POST":
        name = request.form["username"]
        email = request.form["email"]
        return render_template("result.html", name=name, email=email)
    return render_template("form.html")
```

### 结果页面（templates/result.html）

```html
<h2>提交成功！</h2>
<p>姓名：{{ name }}</p>
<p>邮箱：{{ email }}</p>
```

✅ **Web 应用场景：**  

- 用户注册表单  
- 意见反馈提交  
- 数据录入接口  

---

## 八、JSON 响应与 API 基础

### 示例：返回 JSON 数据

```python
from flask import jsonify

@app.route("/api/user/<name>")
def get_user(name):
    return jsonify({"name": name, "role": "developer"})
```

返回结果（访问 `/api/user/tom`）：

```json
{
  "name": "tom",
  "role": "developer"
}
```

✅ **常见应用：**  

- 前后端分离接口（Vue / React 前端调用）  
- 移动端 API  
- 异步数据更新

---

## 九、表单 + JSON 综合示例

```python
from flask import Flask, render_template, request, jsonify

app = Flask(__name__)

@app.route("/")
def index():
    return render_template("form.html")

@app.route("/submit", methods=["POST"])
def handle_submit():
    name = request.form.get("username")
    email = request.form.get("email")
    return jsonify({"name": name, "email": email, "status": "success"})

if __name__ == "__main__":
    app.run(debug=True)
```

✅ 浏览器提交表单 → Flask 返回 JSON → 前端显示成功提示。

---

## 十、HTTP 状态码速查表

| 状态码 | 含义 | 示例 |
|--------|------|------|
| 200 | 请求成功 | 正常响应 |
| 301 | 永久重定向 | 页面跳转 |
| 400 | 客户端请求错误 | 缺少参数 |
| 401 | 未授权 | 登录失败 |
| 404 | 资源不存在 | 页面错误 |
| 500 | 服务器错误 | 程序异常 |

---

## ✅ 小结

| 技术 | 功能 | Web 应用 |
|------|------|----------|
| HTTP 协议 | 定义通信规则 | 客户端与服务器交互 |
| Flask 框架 | Web 服务开发 | 网站与 API |
| render_template | 模板渲染 | 动态页面生成 |
| static 文件 | CSS/JS/图片 | 前端展示资源 |
| request/form | 表单与请求处理 | 用户输入 |
| jsonify | JSON 响应 | API 接口 |

---

## 💡 课后练习

1. 创建一个 Flask 项目，实现：  
   - 首页显示欢迎文字；  
   - 一个表单提交姓名与邮箱；  
   - 提交后返回 JSON 格式结果。  

2. 扩展功能：  
   - 添加 CSS 美化页面；  
   - 为 `/api/user` 接口增加错误处理与状态码返回。

---
