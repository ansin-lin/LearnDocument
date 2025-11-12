# 3.1 Web 开发模式概述：一体式 vs 前后端分离

> 本节目标：让 0 基础学员听得懂 **“网站是怎么工作的”**，并搞清楚：  
>
> - 什么是 **一体式开发**；  
> - 什么是 **前后端分离开发**；  
> - Django 在两种模式下各做什么。

---

## 一、先搞清楚：访问一个网站时发生了什么？

我们在浏览器输入：

```text
http://example.com/articles/
```

大致流程：

1. 浏览器向服务器发送一个「请求」（Request）；  
2. 服务器程序（比如 Django）收到请求，去数据库拿数据、处理逻辑；  
3. 服务器把「结果」返回给浏览器；  
4. 浏览器把结果渲染成你看到的网页。

**区别在于：**  

- 服务器返回的是「一整个 HTML 页面」，还是「JSON 数据」？  
- 页面是由 **服务器** 渲染好的，还是由 **浏览器里的前端代码** 渲染的？  

这就引出了两种开发模式。

---

## 二、一体式开发（Monolithic，全都在后端搞定）

### 1. 概念（适合完全新手先理解）

- 浏览器：只要「成品网页」。  
- Django：负责 **拿数据 + 拼 HTML + 返回整页**。  
- 前端和后端写在一个项目里，前端以模板形式存在。

可以理解为：  
> “后端厨师把菜（HTML 页面）做好端上桌，前端只是服务员端给用户。”

### 2. 简单流程图

```text
浏览器(用户) → Django → 数据库
                    ↓
                HTML 页面
                    ↓
               返回给浏览器
```

### 3. 一体式 Django 示例

```python
# blog/views.py
from django.shortcuts import render
from .models import Article

def article_list(request):
    # 1. 从数据库读取数据
    articles = Article.objects.all()
    # 2. 渲染模板，生成 HTML
    return render(request, "articles.html", {"articles": articles})
```

```html
<!-- templates/articles.html -->
{% extends "base.html" %}
{% block content %}
  <h2>文章列表</h2>
  <ul>
    {% for a in articles %}
      <li>{{ a.title }} - {{ a.created_at }}</li>
    {% endfor %}
  </ul>
{% endblock %}
```

- 浏览器访问 `/articles/`；  
- Django 渲染出完整 HTML 页面；  
- 浏览器直接展示。

### 4. 一体式开发的优缺点

**优点：**

- 对初学者来说最简单易懂；  
- 项目结构相对简单，一个项目就搞定；  
- 模板 + 数据 + 逻辑都写在 Django 里，部署只要放一个服务。

**缺点：**

- 前端页面写法相对传统，不适合复杂交互；  
- 不利于前后端团队分工（都挤在 Django 里）；  
- 同一个后端无法很方便地同时服务：网页 + 手机 App + 小程序。

---

## 三、前后端分离开发（Back-End / Front-End Separate）

### 1. 概念（现代 Web 的主流）

在**前后端分离模式**中：

- Django（后端）：只负责「数据和业务逻辑」，返回 **JSON 数据**；  
- 前端（Vue / React 等）：负责「界面和交互」，拿到 JSON 自己渲染。

可以理解为：  
> “后端只负责把菜切好、煮熟，装到打包盒里（JSON）；  
> 前端负责摆盘、布置餐桌、设计餐厅氛围（页面 UI）。”

### 2. 流程图

```text
浏览器中的前端应用(Vue/React)
         ↓ 发送 HTTP 请求 (Axios / fetch)
      Django 后端 API (返回 JSON)
         ↓
       数据库 (MySQL / PostgreSQL 等)
```

### 3. Django 返回 JSON 的简单例子

```python
# blog/views.py
from django.http import JsonResponse
from .models import Article

def article_list_api(request):
    # values() 把对象转换成字典
    qs = Article.objects.values("id", "title", "content", "created_at")
    data = list(qs)  # QuerySet 转换为列表，方便 JSON 序列化
    return JsonResponse({"articles": data})
```

返回的数据样子大概是：

```json
{
  "articles": [
    {"id": 1, "title": "第一篇文章", "content": "...", "created_at": "2025-01-01T10:00:00"},
    {"id": 2, "title": "第二篇文章", "content": "...", "created_at": "2025-01-02T11:00:00"}
  ]
}
```

### 4. 前端（以 Vue 为例）拿这个数据

```js
import axios from "axios"

export default {
  data() {
    return {
      articles: []
    }
  },
  created() {
    axios.get("http://127.0.0.1:8000/api/articles/")
      .then(res => {
        this.articles = res.data.articles
      })
  }
}
```

```html
<template>
  <h2>文章列表</h2>
  <ul>
    <li v-for="a in articles" :key="a.id">
      {{ a.title }}
    </li>
  </ul>
</template>
```

### 5. 分离式开发的优缺点

**优点：**

- 前后端完全解耦，团队可以并行开发；  
- 后端 API 可复用（网页、App、小程序都可以用）；  
- 容易做复杂交互、单页应用（SPA）；  
- 符合现代企业的开发方式。

**缺点：**

- 多了一个「前端工程」，部署比一体式复杂；  
- 需要处理 **跨域**（CORS）问题；  
- 学习门槛比一体式略高（要理解 JSON、HTTP、API、前端框架等）。

---

## 四、两种模式的对比总结（适合给学生讲的版本）

| 对比项       | 一体式开发                             | 前后端分离开发                          |
|--------------|----------------------------------------|-----------------------------------------|
| 返回内容     | HTML 页面                              | JSON 数据                              |
| 页面由谁渲染 | Django 后端模板                        | 浏览器中的前端框架 (Vue/React)         |
| 适合项目     | 小型网站、后台管理                    | 中大型系统、前后端分工项目             |
| 体验         | 页面刷新为主                          | 异步加载、交互顺滑                      |
| 重用性       | 接口复用较难                          | 同一 API 可供多端使用                  |
| 上手难度     | 容易                                   | 稍高（需要多学前端知识）               |

---

## 五、Django 在你的课程中的定位

在你的教学中，可以这样安排：

1. **先用一体式模式讲解：**
   - 让学生理解：Django 收到请求 → 操作数据库 → 渲染模板 → 返回 HTML；  
   - 这是理解「网站怎么工作」的第一步。

2. **再逐步引导到前后端分离：**
   - 把原来返回 HTML 的视图，改成返回 JSON；  
   - 用 Vue 或简单的原生 JS 去请求数据并展示；  
   - 告诉学生：这就是现代企业真正使用的方式。

---

## ✅ 本节小结（给学生的记忆要点）

- 网站本质：**浏览器发请求 → 服务器处理 → 返回结果**；  
- 一体式：Django 把「页面 + 数据」全做好给你；  
- 分离式：Django 只给「数据」，页面由前端框架制作；  
- 现代公司更多用「前后端分离」，但学习时可以从一体式入门。
