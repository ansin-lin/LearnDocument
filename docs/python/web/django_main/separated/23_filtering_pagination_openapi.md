# 第23章 过滤、排序、分页与 OpenAPI

## 本章成果

把员工 API 改成可筛选、搜索、安全排序和分页的企业列表，并生成可供前端与 Reviewer 查看、但不代替需求确认的 OpenAPI 文档。

## 本章开始状态与修改清单

- 第22章已经完成 JWT、操作权限、部门数据范围和离职数据权限。
- 本章新增 `employees/filters.py`，修改 `settings.py`、`EmployeeViewSet` 和项目路由。
- 所有查询条件只能缩小第22章已经授权的 QuerySet，不能扩大用户可见范围。

## 本章在整体架构中的位置

```text
已授权 QuerySet → Filter → Ordering → Pagination → Serializer → JSON
                         ↑ 本章重点
                                      OpenAPI 描述整条外部契约
```

完成后，客户端可以稳定查询大规模列表，开发、前端和测试人员也能基于同一份接口说明协作。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| Filter | 按允许条件缩小 QuerySet 的组件 | 避免在 View 中散落查询参数处理 | 列表需要精确条件查询时 |
| Ordering | 按白名单字段确定结果顺序 | 保证顺序可控并避免暴露任意字段 | 客户端需要切换列表排序时 |
| Pagination | 把大结果集拆成稳定页面的机制 | 控制响应大小、数据库负载和前端渲染成本 | 列表数据可能持续增长时 |
| OpenAPI | 机器可读的 HTTP API 契约格式 | 统一文档、联调、测试和变更审查依据 | API 需要交付给前端或其他团队时 |

## 1. 先理解列表查询的处理顺序

员工 API 使用以下能力：

- `django-filter`：声明可接受的查询条件。
- DRF 内置 Search/Ordering/PageNumberPagination：搜索、排序、分页。
- `drf-spectacular`：根据代码生成 OpenAPI schema 和开发文档。

库减少重复代码，不替代权限、查询性能、契约评审和测试。

一次列表请求不是直接把整张员工表返回给前端，而是按固定顺序逐步处理：

```text
request.query_params
→ get_queryset() 限制用户数据范围
→ Filter 按精确条件缩小结果
→ Search 按关键字搜索
→ Ordering 决定稳定顺序
→ Pagination 只取当前页
→ Serializer 转换字段
→ Response 返回分页 JSON
```

- **过滤与搜索的区别**：过滤使用部门、日期、状态等明确条件；搜索通常让一个关键字匹配多个允许字段。
- **排序为什么需要白名单**：客户端只能选择后端允许的字段，不能借排序暴露内部字段或触发不合理查询。
- **分页为什么需要稳定排序**：没有稳定顺序时，同一条记录可能在连续请求中跨页移动，造成重复或遗漏。
- **OpenAPI 的作用**：把方法、路径、参数和响应结构描述为机器可读契约，供文档、联调和自动检查使用；它不能自动证明权限与业务规则正确。

`django-filter` 和 `drf-spectacular` 都是第三方直接依赖，需要写入 `requirements.txt` 并由团队锁定兼容版本。DRF 的 `SearchFilter`、`OrderingFilter` 和 `PageNumberPagination` 属于已安装的 `djangorestframework`，不需要再次安装。

## 2. 安装与 settings

在项目根目录激活虚拟环境后安装并记录两个直接依赖：

```powershell
python -m pip install "django-filter==26.1" "drf-spectacular==0.30.0"
```

在项目根目录的 `requirements.txt` 中追加：

```text
django-filter==26.1
drf-spectacular==0.30.0
```

在 `company_portal/settings.py` 中合并以下配置，不要覆盖第22章已有的认证和权限项：

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

`dict.update(mapping)` 接收字典或其他键值映射，原地把内容合并到现有字典并返回 `None`；遇到同名键时，新值会覆盖旧值。本例保留第22章已有的认证和权限配置，只增加或更新筛选、分页与 schema 项，因此不能重新定义一个只含本章键的 `REST_FRAMEWORK`。

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

在 `employees/api_views.py` 顶部导入 `EmployeeFilter`，并把以下属性加入 `EmployeeViewSet`：

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

`ordering_fields` 必须白名单，避免客户端按敏感字段排序或触发不合理查询。稳定分页应追加唯一字段作为次排序；本项目在第22章使用 `employee_number, pk`。

`is_active` 不能绕过第22章的数据范围：没有 `view_inactive_employee` 权限时，基础 QuerySet 已排除离职员工，因此传入 `is_active=false` 只会得到空结果；具有该权限时才可能查询离职数据。Filter Backend 在 `get_queryset()` 之后执行，客户端参数只能继续缩小授权集合。

`django_filters.FilterSet` 声明允许的过滤参数；`DateFilter(field_name, lookup_expr)` 把日期字符串转换并对指定模型字段应用查询，`lookup_expr="gte"`/`"lte"` 分别表示大于等于/小于等于。`filterset_class` 指定 ViewSet 使用的 FilterSet；`search_fields`、`ordering_fields` 是允许搜索和排序的字段白名单，`ordering` 是未传参数时的默认顺序。

`DEFAULT_FILTER_BACKENDS` 按列表顺序启用精确过滤、搜索和排序；这些字符串必须是可导入类路径。`DEFAULT_PAGINATION_CLASS` 指定页码分页类，`PAGE_SIZE` 接受正整数并设置默认每页件数。当前配置不允许客户端任意指定页大小。

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

`prefetch_related(*lookups)` 接收一个或多个关系路径字符串并返回新的 QuerySet。执行查询时，Django 先读取主表，再用额外查询批量取得多值关系并在 Python 中完成关联，适合反向外键和多对多关系。`select_related(*fields)` 同样返回新 QuerySet，但通过 SQL JOIN 读取单值的外键或一对一关系。本项目只有在响应确实使用附件集合时才预取 `"attachments"`，不能为“可能以后会用”而增加查询。

关键字模糊搜索在大数据量下可能无法利用普通索引。数据量和响应时间确有需要时，再评估数据库全文搜索或专用搜索服务，不要在没有测量结果时增加组件。

## 6. 生成 OpenAPI

在 `company_portal/urls.py` 顶部追加导入，并把以下条目加入 `urlpatterns`：

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

在 `company_portal/settings.py` 中至少追加标题和版本：

```python
SPECTACULAR_SETTINGS = {
    "TITLE": "Employee Management API",
    "VERSION": "1.0.0",
}
```

在项目根目录执行 schema 生成检查；`--file` 指定输出文件，`--validate` 要求同时验证生成结果：

```powershell
python manage.py spectacular --file schema.yml --validate
```

自动生成只能反映它能从代码推断的内容。复杂查询参数、错误响应、上传和自定义 action 需要注解和人工评审。API 文档页面在生产是否公开由安全策略决定。

`SpectacularAPIView.as_view()` 返回 OpenAPI schema 端点；`SpectacularSwaggerView.as_view(url_name="schema")` 返回读取该命名 schema 路由的开发文档页面。`DEFAULT_SCHEMA_CLASS` 让 DRF 使用 drf-spectacular 的 `AutoSchema`，`SPECTACULAR_SETTINGS` 设置文档元数据，不改变业务 API 行为。

## 7. 兼容性与版本

增加可选响应字段通常比删除或改名风险低，但也要确认严格客户端。改变字段类型、含义、枚举、分页结构或错误格式通常是破坏性变更。版本策略可以采用 URL、请求头或兼容演进；选择前先确认既有规范和调用方清单。

## 日本企业项目中的实际使用

列表接口通常由后端固定允许的查询字段、默认排序、最大页大小和响应格式。OpenAPI 是跨团队确认与自动检查的依据，但不能替代业务规格、权限设计或真实联调。

## 新人常见错误

- 允许客户端传任意排序字段，暴露内部字段或造成慢查询。
- 在过滤之前使用未限制的数据集合，导致条件查询扩大授权范围。
- 修改分页结构后只改后端，没有更新前端、测试和 OpenAPI。
- 生成 schema 成功就认为接口实现一定正确。

## 企业项目调查路径

```text
Query params → 已授权 QuerySet → Filter → Ordering → Pagination
→ Serializer → JSON → OpenAPI / 前端契约对照
```

查询异常先记录完整参数组合，再分别确认数据范围、过滤条件、排序和分页；性能问题同时查看 SQL 数量与索引计划。

## 现场任务

规格：支持部门、入职 From/To、关键字和入职日期倒序，并保留分页。为无效日期、未允许排序字段、条件组合和第二页补验证；更新 OpenAPI，让前端无需读后端代码即可构造请求。

## 完成检查

- [ ] 过滤、搜索、排序和分页职责清楚。
- [ ] 查询参数不能扩大部门或离职数据范围。
- [ ] 排序字段与页大小受到服务端限制。
- [ ] QuerySet 的关联加载与响应字段一致。
- [ ] OpenAPI 经过验证且不被当作完整业务规格。

下一章使用固定后的分页与错误契约完成一个可运行的最小前端，并在浏览器中验证JWT、Fetch和CORS。
