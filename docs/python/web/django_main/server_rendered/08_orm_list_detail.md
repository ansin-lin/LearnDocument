# 第8章 ORM 列表与详情

## 本章成果

从数据库显示在职员工列表和详情页，并让不存在或已离职的编号返回 404。

## 本章开始状态与修改范围

- 第7章已经录入至少三名员工。
- 替换 `employee_list()` 和 `employee_detail()`，不改变第4章的路由名。
- 修改列表模板字段并新建详情模板；第5章的固定数据从 View 删除。

## 把列表 View 接到数据库

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

## 调整列表模板

第5章模板中的字段改为模型字段：

```html
<a href="{% url 'employees:detail' employee.pk %}">{{ employee.name }}</a>
<span>{{ employee.department.name }}</span>
```

## 新增详情模板

创建 `employees/templates/employees/detail.html`：

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

## 完成检查

- [ ] 页面数据来自数据库，不再来自固定列表
- [ ] 能区分 `get()`、`filter()` 和 `get_object_or_404()`
- [ ] 能说明为什么列表关联查询使用 `select_related()`

下一章开始让普通用户从业务页面新增员工。

## QuerySet 必须掌握的读法

`all()`、`filter()`、`exclude()` 和 `order_by()` 通常返回可继续组合的 `QuerySet`；`get()` 返回单个对象，找不到或找到多条都会抛异常；`first()`、`last()` 返回对象或 `None`。`exists()` 判断是否存在，`count()` 让数据库计数。QuerySet 通常延迟到迭代、切片、长度计算等实际需要结果时才查询数据库。

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

## 写操作和安全边界

`create()` 创建并保存，实例 `save()` 保存变更，QuerySet `update()` 批量更新，`delete()` 删除。批量方法不会逐个执行所有实例逻辑；使用前确认信号、审计和业务副作用。现场执行写操作前先限定 QuerySet、查看预计件数并确认事务与恢复方案。页面详情继续使用 `get_object_or_404()`，不要让正常的“不存在”变成500。
