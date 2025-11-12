# 3.3 模型 (Model) 与数据库操作

> 本节目标：理解 Django ORM 的概念与使用方法，掌握模型定义、迁移、查询及数据操作。

---

## 一、ORM 概述

**ORM（Object Relational Mapping）对象关系映射** 是 Django 的核心功能之一。  
它让开发者用 Python 类和对象操作数据库，而无需编写 SQL。

| 对象 | 数据库表 |
|------|----------|
| 类（Model） | 表（Table） |
| 属性（Field） | 列（Column） |
| 对象（Instance） | 行（Row） |

优点：

- 统一接口，兼容多种数据库（SQLite、MySQL、PostgreSQL 等）；
- 避免 SQL 注入风险；
- 提高开发效率。

---

## 二、定义模型

在每个应用的 `models.py` 中定义模型类，继承自 `django.db.models.Model`：

```python
# blog/models.py
from django.db import models

class Article(models.Model):
    title = models.CharField(max_length=100)
    content = models.TextField()
    author = models.CharField(max_length=50, default='匿名')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.title
```

字段类型常用说明：

| 字段类型 | 说明 |
|-----------|------|
| `CharField(max_length=...)` | 短文本字段 |
| `TextField()` | 长文本字段 |
| `IntegerField()` | 整数 |
| `DateTimeField()` | 日期时间 |
| `BooleanField()` | 布尔值 |
| `ForeignKey()` | 外键，建立一对多关系 |

---

## 三、创建数据库表

1. 生成迁移文件：

    ```bash
    python manage.py makemigrations
    ```

2. 执行迁移：

    ```bash
    python manage.py migrate
    ```

3. 查看迁移记录：

    ```bash
    python manage.py showmigrations
    ```

执行后 Django 会自动在数据库中创建表 `blog_article`。

---

## 四、模型注册到后台

在 `admin.py` 中注册：

```python
from django.contrib import admin
from .models import Article

admin.site.register(Article)
```

运行：

```bash
python manage.py createsuperuser
python manage.py runserver
```

访问 `/admin/` 即可在后台管理文章数据。

---

## 五、基本数据库操作（ORM）

进入交互环境：

```bash
python manage.py shell
```

### 1. 创建数据

```python
from blog.models import Article
Article.objects.create(title='Django ORM 入门', content='学习ORM操作')
```

### 2. 查询数据

```python
Article.objects.all()
Article.objects.filter(author='匿名')
Article.objects.get(id=1)
```

### 3. 更新数据

```python
a = Article.objects.get(id=1)
a.title = '更新后的标题'
a.save()
```

### 4. 删除数据

```python
Article.objects.get(id=1).delete()
```

---

## 六、模型关系

### 1. 一对多（ForeignKey）

```python
class Category(models.Model):
    name = models.CharField(max_length=50)

class Article(models.Model):
    title = models.CharField(max_length=100)
    category = models.ForeignKey(Category, on_delete=models.CASCADE)
```

### 2. 多对多（ManyToManyField）

```python
class Tag(models.Model):
    name = models.CharField(max_length=50)

class Article(models.Model):
    title = models.CharField(max_length=100)
    tags = models.ManyToManyField(Tag)
```

### 3. 一对一（OneToOneField）

```python
class Profile(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE)
    bio = models.TextField()
```

---

## 七、模型查询表达式

| 方法 | 示例 | 说明 |
|------|------|------|
| `all()` | `Article.objects.all()` | 查询所有数据 |
| `filter()` | `Article.objects.filter(author='Lin')` | 条件查询 |
| `exclude()` | `Article.objects.exclude(author='Lin')` | 排除结果 |
| `get()` | `Article.objects.get(id=1)` | 获取单条记录 |
| `order_by()` | `Article.objects.order_by('-created_at')` | 排序 |
| `count()` | `Article.objects.count()` | 统计数量 |
| `values()` | `Article.objects.values('title', 'author')` | 返回字段字典 |

---

## 八、数据分页与聚合

分页示例：

```python
from django.core.paginator import Paginator
articles = Article.objects.all()
paginator = Paginator(articles, 5)
page1 = paginator.page(1)
page1.object_list
```

聚合与分组：

```python
from django.db.models import Count
Article.objects.values('author').annotate(total=Count('id'))
```

---

## 九、迁移机制深入理解

Django 的迁移系统由以下两步构成：

1. **makemigrations**：根据模型变化生成迁移文件；
2. **migrate**：执行迁移文件并同步数据库。

迁移文件存储在：`app_name/migrations/`  
每个迁移文件都记录模型变化的历史。

---

## 十、调试与常见问题

| 问题 | 原因与解决办法 |
|------|----------------|
| 数据库字段未更新 | 忘记执行 `makemigrations` 与 `migrate` |
| 重复创建同名模型 | 应清理旧迁移或调整 `Meta` 类 |
| SQLite 无法支持某些类型 | 使用 MySQL 或 PostgreSQL 替代 |

---

## ✅ 本节总结

- Django ORM 让你用 Python 操作数据库；  
- 迁移系统自动同步模型与数据库结构；  
- 掌握 CRUD、模型关系与查询表达式是 Web 开发基础；  
- 下一节将讲解 **模板与视图结合** 实现数据展示。
