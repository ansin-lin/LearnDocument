# 第11章 搜索与分页

## 本章成果

把员工列表变成企业项目常见的可搜索、可分页页面，并在换页时保留搜索条件。

## 本章开始状态与修改清单

列表只显示在职员工且已有稳定排序。本章只替换 `employee_list()` 的查询组合和列表模板，不改变 CRUD 路由。为了观察分页，请在练习数据库中准备超过10名员工。

## 列表 View

- **Q 对象是什么**：把多个 ORM 条件组合成 AND、OR 或 NOT 的查询表达式。
- **Paginator 是什么**：按照每页数量切分 QuerySet，并根据页码返回 `Page` 对象。
- **为什么需要**：企业列表需要组合搜索，同时不能一次把全部记录发送到页面。
- **什么时候使用**：多个字段执行 OR 搜索时使用 Q；记录数量可能增长时在查询完成后分页。

```python
from django.core.paginator import Paginator
from django.db.models import Q


def employee_list(request: HttpRequest) -> HttpResponse:
    keyword = request.GET.get("q", "").strip()
    employees = Employee.objects.filter(is_active=True).select_related("department")

    if keyword:
        employees = employees.filter(
            Q(employee_number__icontains=keyword)
            | Q(name__icontains=keyword)
            | Q(department__name__icontains=keyword)
        )

    page_obj = Paginator(employees, 10).get_page(request.GET.get("page"))
    return render(
        request,
        "employees/list.html",
        {"page_obj": page_obj, "keyword": keyword},
    )
```

搜索不修改数据，因此使用 GET；搜索条件会显示在 URL 中，方便刷新和分享。`Q` 用来组合 OR 条件，用户输入始终作为 ORM 参数传入，不拼接 SQL。

代码先从 `request.GET` 取得关键字，再建立只包含在职员工的 QuerySet。关键字非空时追加 OR 条件，最后把筛选结果交给 `Paginator`。`get_page()` 能处理空页码和越界页码，返回值 `page_obj` 同时包含当前记录与前后页信息。

`Q(**lookups)` 接收与 `filter()` 相同的字段查找条件并返回查询表达式；使用 `|` 组合OR、`&` 组合AND、`~` 取反。`icontains` 是不区分大小写的包含查询，双下划线前是字段或关联路径，后面是查找方式。

`Paginator(object_list, per_page)` 的两个参数都必填：`object_list` 是QuerySet或序列，`per_page` 是正整数每页件数；返回Paginator对象。`get_page(number)` 接受页码字符串、整数或 `None`，返回Page对象；非数字通常回到第一页，越界通常回到最后一页。

```text
GET 查询参数
→ 清理 keyword
→ QuerySet 基础范围
→ Q 组合搜索条件
→ 稳定排序
→ Paginator
→ Page
→ Template
```

## 模板

用下面内容完整替换 `templates/employees/list.html`。它在第8章数据库表格的基础上加入搜索表单，并把循环对象从 `employees` 改为当前页 `page_obj`：

```html
{% extends "base.html" %}

{% block title %}员工列表 | 员工管理系统{% endblock %}

{% block content %}
<section class="page-heading">
  <div>
    <p class="eyebrow">社員一覧</p>
    <h1>员工列表</h1>
    <p>可以按员工编号、姓名或部门搜索在职员工。</p>
  </div>
  <div>
    <button id="toggle-email" type="button" aria-pressed="false">
      隐藏邮箱
    </button>
    <a href="{% url 'employees:create' %}">新增员工</a>
  </div>
</section>

<form method="get">
  <label for="q">员工编号、姓名或部门</label>
  <input id="q" name="q" value="{{ keyword }}">
  <button type="submit">搜索</button>
  <a href="{% url 'employees:list' %}">清除</a>
</form>

<div class="table-wrap">
  <table>
    <thead>
      <tr>
        <th scope="col">员工编号</th>
        <th scope="col">姓名</th>
        <th class="email-column" scope="col">邮箱</th>
        <th scope="col">部门</th>
        <th scope="col">入职日期</th>
        <th scope="col">操作</th>
      </tr>
    </thead>
    <tbody>
      {% for employee in page_obj %}
      <tr>
        <td>{{ employee.employee_number }}</td>
        <td>{{ employee.name }}</td>
        <td class="email-column">{{ employee.email|default:"未登记" }}</td>
        <td>{{ employee.department.name }}</td>
        <td>{{ employee.joined_on|date:"Y-m-d" }}</td>
        <td>
          <a href="{% url 'employees:detail' employee.pk %}">查看详情</a>
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

{% if page_obj.paginator.num_pages > 1 %}
<nav aria-label="分页">
  {% if page_obj.has_previous %}
    <a href="?q={{ keyword|urlencode }}&amp;page={{ page_obj.previous_page_number }}">上一页</a>
  {% endif %}
  <span>第 {{ page_obj.number }} / {{ page_obj.paginator.num_pages }} 页</span>
  {% if page_obj.has_next %}
    <a href="?q={{ keyword|urlencode }}&amp;page={{ page_obj.next_page_number }}">下一页</a>
  {% endif %}
</nav>
{% endif %}
{% endblock %}
```

搜索表单使用 GET，因此输入值会出现在 URL 中。`page_obj` 既可以在 `{% for %}` 中迭代当前页员工，也提供当前页码、总页数和前后页判断。分页链接继续携带经过 `urlencode` 的关键字，避免换页时丢失搜索条件。

## 边界与性能

- 空关键字等同全部在职员工。
- 非法页码由 `get_page()` 容错，不应产生 500。
- 列表必须有稳定排序；第6章已在 Model 中按员工编号排序。
- 数据量增大后应根据查询和执行计划添加索引，不要凭感觉给每个字段加索引。
- 不在日志中完整记录可能包含个人信息的搜索词。

## 必测地址

- `/employees/`
- `/employees/?q=开发`
- `/employees/?q=不存在`
- `/employees/?page=2&q=开发`
- `/employees/?page=abc`

## 课堂任务

1. 同时按姓名和部门验证关键字 OR 条件。
2. 搜索后进入下一页，确认地址栏仍保留 `q`；返回上一页也要保留。
3. 输入 `%`、日文、中文和超长关键字，确认没有 SQL 错误或500。
4. 截取一次空结果画面，地址栏查询参数和空数据提示必须同时可见。

现场报告：`検索条件を保持したままページングできることを確認しました。`

参考方向见[章节练习参考答案](practice_answers.md)。第18章会在此基础上增加日期范围，并改用 Form 统一校验。

## 企业列表运行检查

- [ ] 搜索和分页可组合
- [ ] 下一页仍保留搜索词
- [ ] 空结果、非法页码和特殊字符都不会产生 500

## 现场识读：Page 对象与安全排序

`Paginator(queryset, per_page)` 负责分页规则，`get_page()` 返回 `Page` 对象。模板常读取 `page_obj.number`、`paginator.num_pages`、`has_next()`、`has_previous()`、`next_page_number()` 和 `previous_page_number()`。分页必须有稳定 `order_by()`，否则数据变化时可能重复或遗漏。

`number` 和 `num_pages` 返回整数；`has_next()`、`has_previous()` 返回布尔值；存在相邻页时，`next_page_number()`、`previous_page_number()` 返回整数页码，不存在时会抛出分页异常，因此模板应先用对应的 `has_*()` 判断。

允许用户选择排序时，绝不能把任意字符串直接交给 `order_by()`：

```python
allowed_orders = {"number": "employee_number", "joined_desc": "-joined_on"}
order_key = request.GET.get("order", "number")
employees = employees.order_by(allowed_orders.get(order_key, "employee_number"))
```

搜索、排序和分页都是 GET 条件。生成翻页链接时应保留除 `page` 外的所有已验证条件，而不是每增加一个筛选字段就手写一遍参数。

## 企业列表的验证观点

- 空条件、单条件、多条件、无结果和特殊字符。
- 第一页、中间页、最后一页、越界页。
- 排序字段白名单和稳定次排序。
- 逻辑删除与权限范围是否在分页前过滤。
- 数据量增加后的 SQL 次数和响应时间。

先过滤再分页；不要取出全部数据后在 Python 中分页。慢查询要用 SQL、执行计划和真实数据分布调查，不能只凭页面感觉判断。

## 本章总结

搜索、排序和分页都属于 GET 查询条件。先验证和过滤，再使用稳定排序分页；翻页链接必须保留其他查询条件。下一阶段会增加登录和角色权限，按钮显示和后端访问限制必须同时实现。

## 日本项目中的实际使用

搜索通常使用 GET，因为它不修改数据，URL 可以保存、共享和用于障害复现。团队会要求翻页时保留全部条件，并对排序字段使用白名单。数据量较大时还要结合 SQL、索引和执行计划确认性能。

## 新人常见错误

- 搜索使用 POST，刷新和分享条件变得困难。
- 翻页链接只保留 `page`，导致搜索条件丢失。
- 没有稳定排序就分页，数据变化时可能重复或漏项。
- 先把全部记录转换成列表，再在 Python 中分页，浪费内存和数据库能力。
- 将用户输入直接传给 `order_by()`，应先映射到允许的字段。

## 本章知识将在后续章节继续使用

```text
request.GET
→ Q 搜索
→ QuerySet 排序
→ Paginator / Page
→ 第13章先追加权限范围
→ 第18章追加日期条件并保持分页参数
```
