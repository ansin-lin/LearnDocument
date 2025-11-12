# 3.2 Django 基础与项目结构

> 本节目标：掌握 Django 项目的创建流程、目录结构、核心文件作用及运行机制。

---

## 一、Django 简介

**Django** 是一个功能强大的 Web 框架，遵循 **MVT（Model-View-Template）** 模式。  
它帮助开发者以最少的代码快速构建安全、可维护的网站。

特点：

- 自带 ORM、模板系统、认证系统、管理后台；
- 遵循“约定优于配置”原则；
- 强调安全性与可扩展性。

---

## 二、创建 Django 项目

### 1. 安装 Django

```bash
pip install django
```

### 2. 创建新项目

```bash
django-admin startproject mysite
cd mysite
python manage.py runserver
```

浏览器访问 `http://127.0.0.1:8000/`  
若显示 “The install worked successfully!” 表示项目运行成功。

---

## 三、项目目录结构详解

创建完成后目录如下：

```text
mysite/
 ├─ manage.py
 ├─ mysite/
 │   ├─ __init__.py
 │   ├─ settings.py
 │   ├─ urls.py
 │   ├─ asgi.py
 │   └─ wsgi.py
```

### 📁 主要文件说明

| 文件 | 作用 |
|------|------|
| `manage.py` | 项目命令行工具入口，用于运行服务器、创建App、迁移数据库等 |
| `settings.py` | 项目全局配置（数据库、应用、模板、静态文件等） |
| `urls.py` | URL 路由定义，控制请求与视图的映射关系 |
| `asgi.py` | 异步服务入口，用于部署到 ASGI 服务器（如 uvicorn） |
| `wsgi.py` | 同步服务入口，用于部署到 WSGI 服务器（如 Gunicorn） |
| `__init__.py` | 声明这是一个 Python 包 |

---

## 四、创建 Django 应用（App）

Django 项目可以包含多个“应用（App）”，每个 App 负责一个功能模块。

### 1. 创建 App

```bash
python manage.py startapp blog
```

目录结构：

```text
blog/
 ├─ admin.py
 ├─ apps.py
 ├─ models.py
 ├─ tests.py
 ├─ views.py
 └─ migrations/
```

### 2. 文件说明

| 文件 | 作用 |
|------|------|
| `models.py` | 定义数据库模型（Model） |
| `views.py` | 定义视图函数或类（View） |
| `admin.py` | 配置后台管理界面 |
| `apps.py` | App 配置类 |
| `migrations/` | 存储数据库迁移文件 |
| `tests.py` | 单元测试 |

---

## 五、注册 App

在 `settings.py` 中注册：

```python
INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'blog',  # 新增应用
]
```

---

## 六、运行 Django 开发服务器

```bash
python manage.py runserver
```

默认运行在 `http://127.0.0.1:8000/`，可指定端口：

```bash
python manage.py runserver 8080
```

---

## 七、MVT 模式概览

| 层级 | 作用 | Django 对应组件 |
|------|------|----------------|
| Model | 负责与数据库交互 | models.py |
| View | 处理业务逻辑与请求响应 | views.py |
| Template | 展示页面内容 | templates/ |

工作流程：  
**用户请求 → URL 路由 → View → Model → Template → 响应页面**

---

## 八、推荐开发结构（多App项目）

```text
mysite/
 ├─ manage.py
 ├─ mysite/
 │   ├─ settings.py
 │   ├─ urls.py
 │   └─ ...
 ├─ blog/           # 博客模块
 ├─ users/          # 用户模块
 ├─ comments/       # 评论模块
 ├─ static/         # 静态文件(css/js)
 └─ templates/      # 全局模板
```

---

## 九、常用命令速查

| 命令 | 功能 |
|------|------|
| `django-admin startproject name` | 创建新项目 |
| `python manage.py startapp app_name` | 创建新应用 |
| `python manage.py runserver` | 启动开发服务器 |
| `python manage.py makemigrations` | 生成数据库迁移 |
| `python manage.py migrate` | 应用数据库迁移 |
| `python manage.py createsuperuser` | 创建管理员账户 |
| `python manage.py shell` | 打开 Django shell 环境 |

---

## ✅ 本节总结

- Django 项目以 MVT 架构为核心；  
- 每个模块（App）都是相对独立的；  
- 理解 `settings.py`、`urls.py`、`views.py` 是后续开发的关键；  
- 下一节将进入 **模型与数据库操作** 的学习。
