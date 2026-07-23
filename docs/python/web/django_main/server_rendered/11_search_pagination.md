# 第11章 搜索与分页

## 本章成果

把员工列表变成企业项目常见的可搜索、可分页页面，并在换页时保留搜索条件。

## 本章开始状态与修改清单

列表只显示在职员工且已有稳定排序。本章只替换 `employee_list()` 的查询组合和列表模板，不改变 CRUD 路由。为了观察分页，请在练习数据库中准备超过10名员工。

## 列表 View

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

## 模板

```html
<form method="get">
  <label for="q">员工编号、姓名或部门</label>
  <input id="q" name="q" value="{{ keyword }}">
  <button type="submit">搜索</button>
</form>

{% for employee in page_obj %}
  <!-- 复用第8章的员工行 -->
{% empty %}
  <p>没有符合条件的员工。</p>
{% endfor %}

<nav aria-label="分页">
  {% if page_obj.has_previous %}
    <a href="?q={{ keyword|urlencode }}&page={{ page_obj.previous_page_number }}">上一页</a>
  {% endif %}
  <span>第 {{ page_obj.number }} / {{ page_obj.paginator.num_pages }} 页</span>
  {% if page_obj.has_next %}
    <a href="?q={{ keyword|urlencode }}&page={{ page_obj.next_page_number }}">下一页</a>
  {% endif %}
</nav>
```

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

## 完成检查

- [ ] 搜索和分页可组合
- [ ] 下一页仍保留搜索词
- [ ] 空结果、非法页码和特殊字符都不会产生 500

下一阶段会增加登录和角色权限；从那时起，按钮显示和后端访问限制必须同时实现。

## Page 对象与安全排序

`Paginator(queryset, per_page)` 负责分页规则，`get_page()` 返回 `Page` 对象。模板常读取 `page_obj.number`、`paginator.num_pages`、`has_next()`、`has_previous()`、`next_page_number()` 和 `previous_page_number()`。分页必须有稳定 `order_by()`，否则数据变化时可能重复或遗漏。

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
