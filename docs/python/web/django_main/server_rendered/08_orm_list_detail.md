# 第8章 ORM 列表与详情

## 本章成果

从数据库显示在职员工列表和详情页，并让不存在或已离职的编号返回 404。

## 本章开始状态与修改范围

- 第7章已经录入至少三名员工。
- 替换 `employee_list()` 和 `employee_detail()`，不改变第4章的路由名。
- 修改列表模板字段并新建详情模板；第5章的固定数据从 View 删除。

## 把列表 View 接到数据库

这里第一次正式使用 Manager、QuerySet、`select_related()` 和 `get_object_or_404()`：

- **Manager 是什么**：Model 的查询入口，默认名称是 `objects`。需要从某张表开始查询时使用 `Employee.objects`。
- **QuerySet 是什么**：表示一组可继续组合的数据库查询条件，通常在真正需要结果时才访问数据库。
- **为什么需要 `select_related()`**：列表读取外键时提前用一次关联查询取得部门，避免循环中重复查询。
- **什么时候使用 `get_object_or_404()`**：详情页按主键等条件取得单条数据，找不到属于正常业务结果时使用。

```text
Employee.objects（Manager）
→ filter() 组成 QuerySet
→ select_related() 准备关联数据
→ 执行 SQL
→ Model 对象
→ Template 显示
```

编辑 `employees/views.py`，删除第5章的固定列表：

```python
from django.http import HttpRequest, HttpResponse
from django.shortcuts import get_object_or_404, render

from .models import Employee


def employee_list(request: HttpRequest) -> HttpResponse:
    employees = (
        Employee.objects
        .filter(is_active=True)
        .select_related("department")
    )
    return render(request, "employees/list.html", {"employees": employees})


def employee_detail(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(
        Employee.objects.select_related("department"),
        pk=employee_id,
        is_active=True,
    )
    return render(request, "employees/detail.html", {"employee": employee})
```

`filter()` 返回可继续组合的 `QuerySet`；`get_object_or_404()` 找不到时返回正常的 404 响应；`select_related()` 一次取出部门，避免列表中为每名员工重复查询。

| API | 当前参数 | 可接受的值与必填性 | 返回值或异常 | 什么时候使用 |
|---|---|---|---|---|
| `filter(**lookups)` | 字段查找条件 | 可传零个或多个关键字条件 | 新的 `QuerySet`；没有结果时为空，不抛异常 | 列表筛选或结果可能为多条时 |
| `select_related(*fields)` | 关联字段路径 | 零个或多个ForeignKey/OneToOne字段名 | 带关联查询计划的新 `QuerySet` | 后续会访问单值关联对象时 |
| `get_object_or_404(model_or_queryset, **lookups)` | Model/QuerySet、查询条件 | 第一项必填；查询条件按目标唯一性提供 | 唯一Model对象；没有时抛 `Http404`，多条时仍会报错 | 详情页取得一条必须唯一的记录 |

这些方法不会修改数据。QuerySet方法通常返回新的QuerySet，因此需要赋值、继续链式调用或交给模板使用。

列表函数先建立在职员工 QuerySet，再把它作为 context 交给模板。详情函数把查询条件、主键和在职状态一起传入 `get_object_or_404()`；成功时返回一个 `Employee`，不存在时由 Django 进入404处理，不让正常的“查无数据”变成500。

## 调整列表模板

用下面内容完整替换 `templates/employees/list.html`。字段从第5章的字典键切换为 Model 属性，同时保留邮箱列的 `email-column` class，使显示/隐藏按钮继续工作：

```html
{% extends "base.html" %}

{% block title %}员工列表 | 员工管理系统{% endblock %}

{% block content %}
<section class="page-heading">
  <div>
    <p class="eyebrow">社員一覧</p>
    <h1>员工列表</h1>
    <p>当前数据来自数据库，只显示在职员工。</p>
  </div>
  <button id="toggle-email" type="button" aria-pressed="false">
    隐藏邮箱
  </button>
</section>

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
      {% for employee in employees %}
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
        <td class="empty-cell" colspan="6">没有在职员工</td>
      </tr>
      {% endfor %}
    </tbody>
  </table>
</div>
{% endblock %}
```

这里发生了三项明确变化：`employee_code` 改为 Model 字段 `employee_number`；部门字符串改为外键对象的 `employee.department.name`；列表新增 `joined_on` 的日期显示。View 已经使用 `select_related("department")`，因此模板循环读取部门名称时不会为每名员工重复查询一次部门。

## 新增详情模板

创建 `templates/employees/detail.html`：

```html
{% extends "base.html" %}

{% block title %}{{ employee.name }} | 员工管理系统{% endblock %}

{% block content %}
  <h1>{{ employee.name }}</h1>
  <dl>
    <dt>员工编号</dt><dd>{{ employee.employee_number }}</dd>
    <dt>部门</dt><dd>{{ employee.department.name }}</dd>
    <dt>邮箱</dt><dd>{{ employee.email|default:"未登记" }}</dd>
    <dt>入职日期</dt><dd>{{ employee.joined_on|date:"Y-m-d" }}</dd>
  </dl>
  <a href="{% url 'employees:list' %}">返回员工列表</a>
{% endblock %}
```

## 在 Shell 中观察 ORM

```bash
python manage.py shell
```

```python
from employees.models import Department, Employee

Employee.objects.all()
Employee.objects.filter(name__icontains="山田")
Employee.objects.filter(department__name="开发部")
Department.objects.get(name="开发部").employees.all()
```

## 验证

访问员工列表、一个存在的详情地址和 `/employees/999999/`。最后一个地址必须返回 404，而不是 500。

同时在 Django Debug Toolbar 尚未引入的情况下，用 Shell 打印 `employees.query` 观察 SQL 只是了解手段；主线验证仍以页面数据、404和后续自动测试为准。

## 课堂任务

1. 在 Admin 将一名员工设为离职，确认列表和详情都不再显示该员工。
2. 暂时移除 `select_related("department")`，说明列表可能出现的重复关联查询问题，再恢复代码。
3. 把一个不存在的主键写入地址栏，记录状态码和页面，不接受500作为“找不到”。

现场报告：`存在しない社員IDは404となり、500エラーにならないことを確認しました。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 数据读取运行检查

- [ ] 页面数据来自数据库，不再来自固定列表
- [ ] 能区分 `get()`、`filter()` 和 `get_object_or_404()`
- [ ] 能说明为什么列表关联查询使用 `select_related()`

## 现场识读：QuerySet 的组合与返回结果

`all()`、`filter()`、`exclude()` 和 `order_by()` 通常返回可继续组合的 `QuerySet`；`get()` 返回单个对象，找不到或找到多条都会抛异常；`first()`、`last()` 返回对象或 `None`。`exists()` 判断是否存在，`count()` 让数据库计数。QuerySet 通常延迟到迭代、切片、长度计算等实际需要结果时才查询数据库。

| API | 主要参数 | 可接受的值与必填性 | 返回值 |
|---|---|---|---|
| `all()` | 无 | 不接参数 | 包含当前范围全部记录的新QuerySet |
| `exclude(**lookups)` | 排除条件 | 零个或多个字段条件 | 新QuerySet |
| `get(**lookups)` | 唯一记录条件 | 应提供能唯一定位记录的条件 | 单个对象；0条或多条均抛异常 |
| `order_by(*fields)` | 排序字段 | 零个或多个字段名；前缀 `-` 表示降序 | 排序后的新QuerySet |
| `first()` / `last()` | 无 | 不接参数 | 一个对象或 `None` |
| `exists()` | 无 | 不接参数 | 布尔值 |
| `count()` | 无 | 不接参数 | 整数件数 |

```python
active = Employee.objects.filter(is_active=True)
developers = active.filter(department__name__icontains="开发")
recent = developers.filter(joined_on__gte="2025-04-01").order_by("-joined_on")
recent.exists()
recent.count()
recent.first()
```

常见 lookup 写在双下划线后：`exact`、`contains`、`icontains`、`startswith`、`endswith`、`in`、`range`、`gte`、`lte`、`isnull`。先根据规格选准确条件，不要因为名字相似就套用。

## 关联、组合条件与性能

`employee.department` 是外键正向访问，`department.employees.all()` 来自 `related_name`。`select_related()` 适合 ForeignKey/OneToOne；`prefetch_related()` 适合多对多和反向多条关系。两者都应从页面实际访问的数据出发，而不是无条件添加。

```python
from django.db.models import Q

Employee.objects.filter(
    Q(name__icontains=keyword) | Q(employee_number__icontains=keyword),
    is_active=True,
)
```

`&` 表示 AND，`|` 表示 OR，复杂组合用括号明确优先级。ORM 会参数化用户输入，但动态排序字段等结构部分仍需白名单。

`Q(**lookups)` 接收字段查询条件并返回可组合的查询表达式；它本身不访问数据库，只有交给 `filter()` 等QuerySet方法后才参与SQL。第11章会把它用于员工列表的多字段OR搜索。

`prefetch_related(*lookups)` 接收多值关联路径并返回新的QuerySet；主查询后通常再执行额外查询，并在Python中组合结果。它适合ManyToMany和反向多条关系，不等同于本章 `select_related()` 的单条SQL关联方式。

## 写操作和安全边界

`create()` 创建并保存，实例 `save()` 保存变更，QuerySet `update()` 批量更新，`delete()` 删除。批量方法不会逐个执行所有实例逻辑；使用前确认信号、审计和业务副作用。现场执行写操作前先限定 QuerySet、查看预计件数并确认事务与恢复方案。页面详情继续使用 `get_object_or_404()`，不要让正常的“不存在”变成500。

`create(**fields)` 接收字段值并返回已保存的Model对象；`save()` 保存当前实例，通常返回 `None`；`update(**fields)` 返回数据库实际更新件数；`delete()` 返回删除总数和按Model分类的明细字典。写方法会改变数据库，不能为了观察结果在生产环境随意执行。

## 本章总结

QuerySet 可以逐步组合过滤、关联和排序条件；列表使用 `select_related()` 避免重复查询，详情使用 `get_object_or_404()` 把正常的“不存在”转换为404。下一章开始让普通用户从业务页面新增员工。

## 日本项目中的实际使用

企业列表页通常先限定业务范围，再追加搜索和排序，最后分页。Review 不只看查询结果，还会确认是否遗漏逻辑删除条件、权限范围和关联查询优化。详情不存在返回404，比捕获所有异常后返回200更容易监控和调查。

## 新人常见错误

- 忘记从 `objects` 开始查询，或把 Manager 当成查询结果。
- 用 `get()` 取得可能有多条的数据，触发 `MultipleObjectsReturned`。
- 在模板循环中逐条访问外键却未使用 `select_related()`，产生 N+1 查询。
- 详情页直接调用 `get()` 而不处理不存在情况，正常的无数据变成500。
- QuerySet 尚未执行时就误判已经访问数据库；应理解其延迟执行特点。

## 本章知识将在后续章节继续使用

```text
Manager → QuerySet → SQL → Model 对象
→ 第9章 ModelForm 保存
→ 第10章更新与逻辑删除
→ 第11章 Q 查询和分页
→ 第13章权限范围过滤
```
