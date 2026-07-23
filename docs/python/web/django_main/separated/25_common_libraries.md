# 第25章 过滤、排序、分页与 OpenAPI

## 本章成果

把员工 API 改成可筛选、搜索、安全排序和分页的企业列表，并生成可供前端与 Reviewer 查看、但不代替需求确认的 OpenAPI 文档。

## 1. 为什么本章只选这些库

DRF 项目常见库很多，主线只引入最终员工 API 实际使用的能力：

- `django-filter`：声明可接受的查询条件。
- DRF 内置 Search/Ordering/PageNumberPagination：搜索、排序、分页。
- `drf-spectacular`：根据代码生成 OpenAPI schema 和开发文档。

库减少重复代码，不替代权限、查询性能、契约评审和测试。

## 2. 安装与 settings

```powershell
python -m pip install django-filter drf-spectacular
```

```python
INSTALLED_APPS += ["django_filters", "drf_spectacular"]

REST_FRAMEWORK.update({
    "DEFAULT_FILTER_BACKENDS": [
        "django_filters.rest_framework.DjangoFilterBackend",
        "rest_framework.filters.SearchFilter",
        "rest_framework.filters.OrderingFilter",
    ],
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 20,
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
})
```

## 3. 明确日期范围 FilterSet

创建 `employees/filters.py`：

```python
import django_filters

from .models import Employee


class EmployeeFilter(django_filters.FilterSet):
    joined_from = django_filters.DateFilter(field_name="joined_on", lookup_expr="gte")
    joined_to = django_filters.DateFilter(field_name="joined_on", lookup_expr="lte")

    class Meta:
        model = Employee
        fields = ["department", "is_active"]
```

ViewSet：

```python
class EmployeeViewSet(viewsets.ModelViewSet):
    filterset_class = EmployeeFilter
    search_fields = ["employee_number", "name", "department__name"]
    ordering_fields = ["employee_number", "name", "joined_on"]
    ordering = ["employee_number"]
```

请求示例：

```text
GET /api/employees/?department=2&joined_from=2025-04-01&search=田中&ordering=-joined_on&page=2
```

`ordering_fields` 必须白名单，避免客户端按敏感字段排序或触发不合理查询。稳定分页可追加唯一字段作为次排序。若基础 QuerySet 固定 `is_active=True`，暴露 `is_active` 筛选没有意义；应根据接口是否允许查看离职员工调整契约，而不是制造假参数。

## 4. 分页响应

默认页码分页响应通常包含：

```json
{
  "count": 42,
  "next": "http://127.0.0.1:8000/api/employees/?page=2",
  "previous": null,
  "results": []
}
```

前端读取 `results`，不能假设列表根节点永远是数组。每页大小是否允许客户端指定要谨慎；若开放 `page_size`，必须限制最大值，防止一次取出全部数据。

## 5. 查询性能

筛选和分页在数据库执行。嵌套部门字段配合 `select_related("department")`；附件数量等反向关系按实际响应使用 `prefetch_related()` 或聚合。确认 SQL 次数、索引、数据分布和执行计划，不用“加了分页一定快”代替测量。

关键字模糊搜索在大数据量下可能无法利用普通索引。真实项目可考虑数据库全文搜索或专用搜索服务，但必须根据量级和需求，不在课程项目提前堆组件。

## 6. 生成 OpenAPI

项目路由：

```python
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView

urlpatterns += [
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    path(
        "api/docs/",
        SpectacularSwaggerView.as_view(url_name="schema"),
        name="api-docs",
    ),
]
```

settings 至少写标题和版本：

```python
SPECTACULAR_SETTINGS = {
    "TITLE": "Employee Management API",
    "VERSION": "1.0.0",
}
```

执行 schema 生成检查：

```powershell
python manage.py spectacular --file schema.yml --validate
```

自动生成只能反映它能从代码推断的内容。复杂查询参数、错误响应、上传和自定义 action 需要注解和人工评审。API 文档页面在生产是否公开由安全策略决定。

## 7. 兼容性与版本

增加可选响应字段通常比删除/改名风险低，但也要确认严格客户端。改变字段类型、含义、枚举、分页结构或错误格式通常是破坏性变更。版本策略可能采用 URL、请求头或演进兼容，课程不强制一种；现场先读既有规范和调用方清单。

## 现场任务

规格：支持部门、入职 From/To、关键字和入职日期倒序，并保留分页。为无效日期、未允许排序字段、条件组合和第二页补验证；更新 OpenAPI，让前端无需读后端代码即可构造请求。

## 完成检查

- [ ] 过滤、搜索、排序和分页职责清楚。
- [ ] 排序字段与页大小受到服务端限制。
- [ ] QuerySet 的关联加载与响应字段一致。
- [ ] OpenAPI 经过验证且不被当作完整业务规格。

下一章用 APIClient 建立认证、CRUD、查询和文件的回归测试。
