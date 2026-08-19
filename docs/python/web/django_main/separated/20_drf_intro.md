# 第20章 安装 DRF 与编写 Serializer

## 本章成果

安装 Django REST framework（DRF），用 `ModelSerializer` 统一员工 JSON 输出与新增输入校验，并通过第一个 DRF 接口观察标准 `Request`、`Response` 和错误结构。

## 本章开始状态与修改清单

- 第19章的健康检查和手写员工 JSON 列表可以运行。
- 本章记录 DRF 依赖，注册 `rest_framework`，新建 `employees/serializers.py`。
- 使用 DRF 函数 View 替换手写员工列表；Model、迁移和数据库保持不变。

## 本章在整体架构中的位置

```text
Request → DRF View → Serializer → Model / ORM
                         ↑ 本章重点
Serializer → Response → JSON
```

完成后，手写 JSON 转换和零散字段校验将进入统一边界，后续 CRUD 可以复用同一份输入输出契约。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| DRF | 建立在 Django 上的 REST API 开发框架 | 统一请求解析、序列化、认证、权限和响应 | 使用 Django 开发可维护 API 时 |
| Serializer | 在外部数据与 Python/Model 对象之间转换并校验的组件 | 防止 View 手写重复转换与校验 | 定义 API 输入或输出契约时 |
| DRF `Response` | 由 DRF 渲染器处理的 HTTP 响应 | 统一 JSON 输出、状态码和内容协商 | 从 DRF View 返回数据或错误时 |

## 1. 先理解 DRF、Serializer 与 Response

Django 本身能够返回 JSON，但第19章需要手工处理请求体、字段校验、Model 转换和错误响应。DRF 建立在 Django 之上，继续复用 Django 的 URL、View、Model、ORM 和数据库，同时补充 API 开发常用的请求解析、序列化、认证、权限和响应机制。

Serializer 位于 HTTP 数据与 Python/Model 对象之间：

```text
输入 JSON
→ DRF Request.data
→ Serializer 反序列化与校验
→ validated_data
→ Model / Database

Model / QuerySet
→ Serializer 序列化
→ Python 字典或列表
→ DRF Response
→ JSON
```

- **Serializer 是什么**：定义 API 接受和返回哪些字段，并负责类型转换与输入校验的组件。
- **为什么需要**：View 不应为每个接口重复手写日期转换、外键处理、必填检查和错误结构。
- **什么时候使用**：接收新增或修改数据，以及把 Model 或 QuerySet 转为响应数据时。

`ModelSerializer` 是与 Django Model 配合使用的 Serializer。它可以根据 Model 字段生成常见字段和基础校验，但仍必须用 `Meta.fields` 明确接口白名单，并为当前业务场景补充校验。Serializer 不负责 URL 匹配、用户权限和 QuerySet 数据范围。

输入校验通常分为三层：字段校验处理单个值，对象校验处理多个字段之间的关系，Model 或数据库约束保证所有写入入口都遵守数据完整性。`Response` 接收 Serializer 产生的 Python 基础数据，再由 DRF 渲染为 JSON；它不会自动决定应该查询哪些员工。

## 2. 安装与注册

项目使用 Django 5.2 LTS；DRF 版本由依赖文件锁定在与 Django 兼容的范围。在项目根目录激活虚拟环境后安装并记录直接依赖：

```powershell
python -m pip install "djangorestframework==3.17.2"
```

在项目根目录的 `requirements.txt` 中追加：

```text
djangorestframework==3.17.2
```

然后在 `company_portal/settings.py` 的 `INSTALLED_APPS` 中注册：

```python
INSTALLED_APPS = [
    # Django 与既有 App
    "rest_framework",
    "employees.apps.EmployeesConfig",
]
```

执行 `python manage.py check`。首次看到 DRF 可浏览 API 页面只代表开发辅助界面可用，不代表认证、权限和生产配置已经完成。

## 3. 部门与员工 Serializer

创建 `employees/serializers.py`：

```python
from rest_framework import serializers

from .models import Department, Employee


class DepartmentSummarySerializer(serializers.ModelSerializer):
    class Meta:
        model = Department
        fields = ["id", "name"]


class EmployeeSerializer(serializers.ModelSerializer):
    department_detail = DepartmentSummarySerializer(
        source="department",
        read_only=True,
    )

    class Meta:
        model = Employee
        fields = [
            "id",
            "employee_number",
            "name",
            "department",
            "department_detail",
            "email",
            "joined_on",
            "is_active",
        ]
        read_only_fields = ["id"]

    def validate_employee_number(self, value):
        value = value.strip().upper()
        if not value.startswith("E"):
            raise serializers.ValidationError("员工编号必须以 E 开头。")
        return value
```

`department` 接收外键 ID，`department_detail` 只读返回部门摘要。这样输入简单、输出易读。企业接口是否采用嵌套结构应以现有契约为准，不要因个人喜好随意变化。

`serializers.ModelSerializer` 根据 `Meta.model` 和 `Meta.fields` 建立字段及常见校验；`fields` 是必填白名单。嵌套字段中的 `source="department"` 指定取值来源，`read_only=True` 表示只参与输出、不接受客户端写入。方法名 `validate_<字段名>(self, value)` 是 DRF 的单字段校验约定：返回规范化后的值表示通过，抛出 `serializers.ValidationError` 时生成该字段的400错误。

`fields` 使用明确白名单，避免 Model 新增内部字段后被自动暴露。密码、审计内部字段和不必要个人信息绝不能因为“Model 中有”就进入 API。

## 4. 最小 DRF View

将 `employees/api_views.py` 整理为下面的完整内容。第19章的 `api_health()` 保持不变，只把员工列表改为 DRF View：

```python
from django.http import JsonResponse
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .models import Employee
from .serializers import EmployeeSerializer


def api_health(request):
    if request.method != "GET":
        return JsonResponse(
            {"detail": "Method not allowed."},
            status=405,
        )
    return JsonResponse({"status": "ok"})


@api_view(["GET", "POST"])
@permission_classes([IsAuthenticated])
def employee_api_list(request):
    if request.method == "GET":
        employees = Employee.objects.filter(is_active=True).select_related("department")
        serializer = EmployeeSerializer(employees, many=True)
        return Response({"results": serializer.data})

    serializer = EmployeeSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    employee = serializer.save()
    return Response(EmployeeSerializer(employee).data, status=201)
```

`request.data` 根据内容类型解析 JSON 或表单数据；`Response` 根据渲染器生成响应；`many=True` 表示序列化多个对象。`serializer.data` 是输出，`validated_data` 只在校验成功后使用。

`@api_view(["GET", "POST"])` 把函数转换为 DRF View，并把允许的方法作为必填列表；其他方法由 DRF 返回405。`@permission_classes([IsAuthenticated])` 为该 View 指定权限类列表。输入 Serializer 的 `is_valid(raise_exception=True)` 执行校验，失败时抛出可转换为400的异常；`save()` 在校验成功后创建并返回 `Employee`。`Response(data, status=200)` 接受可渲染数据和可选状态码，本例成功创建时返回201。

## 5. 校验的三个层次

- 字段校验：`validate_employee_number()` 等单字段规则。
- 对象校验：`validate(self, attrs)` 比较多个字段。
- 模型/数据库约束：唯一性、外键和必须全入口遵守的完整性。

把以下对象级校验方法追加到 `employees/serializers.py` 的 `EmployeeSerializer` 类内，与 `validate_employee_number()` 保持同级缩进：

```python
def validate(self, attrs):
    if attrs.get("joined_on") and attrs["joined_on"].year < 2000:
        raise serializers.ValidationError(
            {"joined_on": "员工系统只接受2000年后的入职日期。"}
        )
    return attrs
```

示例业务限制只是练习，真实规格必须由需求确认。API 错误应稳定、可定位字段，但不要把数据库异常或堆栈原样返回。

## 6. POST 验证

DRF 默认可使用 `SessionAuthentication` 读取 Django Session Cookie，识别登录用户并设置 `request.user`。它适合可浏览 API 和同站点页面；POST、PATCH、DELETE 等非安全方法还必须通过 CSRF 校验，因此仅有登录状态并不等于可以跳过 CSRF。

`include(module_or_patterns)` 把另一个模块中的 URL patterns 交给当前项目路由继续匹配。参数可以是路由模块的导入路径或 URL pattern 列表，本例传入 DRF 自带路由模块；它返回供 `path()` 使用的子路由描述，不直接处理请求或返回响应。

为了观察这种认证方式，在 `company_portal/urls.py` 顶部确认已经导入 `include`，并在既有 `urlpatterns` 中追加开发用登录路由：

```python
from django.urls import include, path

urlpatterns += [
    path("api-auth/", include("rest_framework.urls")),
]
```

启动服务器，先访问 `http://127.0.0.1:8000/api-auth/login/`，使用项目创建时的超级用户登录，再访问 `http://127.0.0.1:8000/api/employees/`。在 DRF 可浏览 API 表单中提交 POST，可以同时满足 Session 登录和 CSRF 校验。

下面的 PowerShell 请求没有 Session Cookie 和 CSRF token，用于确认未认证写入会被拒绝，不是成功新增示例：

```powershell
curl.exe -i -X POST http://127.0.0.1:8000/api/employees/ `
  -H "Content-Type: application/json" `
  -d '{"employee_number":"e010","name":"田中一郎","department":1,"email":"tanaka@example.test","joined_on":"2026-04-01","is_active":true}'
```

当前使用 `SessionAuthentication`，命令行 POST 还会涉及 Session Cookie 与 CSRF token。成功新增使用上面的可浏览 API；第22章配置 JWT 后再使用带 access token 的命令行请求。无论使用哪种认证，都不能为了调试而开放未认证写入。

PowerShell 中 `curl.exe -H "名称: 值"` 添加请求头，`-d` 提交请求体；使用 `-d` 时 curl 会发送写请求，本例已用 `-X POST` 明确方法。`Content-Type: application/json` 必须与 JSON 请求体一致，示例数据会尝试新增员工，只能对本地练习数据库执行。

未认证的命令行请求预期返回403且数据库不增加记录。登录可浏览 API 后，再验证非法编号、重复编号、不存在部门和缺少必填字段；这些输入应返回400。合法输入应返回201、规范化后的 `E010` 和新 ID。

## 7. 新人常见错误

- `ModuleNotFoundError: rest_framework`：当前解释器未安装依赖。
- `Expected a list ... but got type QuerySet`：序列化集合时漏了 `many=True`。
- 访问 `.data` 前报错：输入 Serializer 尚未执行 `is_valid()`。
- 外键只出现数字：这是默认表示；是否追加摘要字段由契约决定。
- 重复查询部门：QuerySet 未配合 `select_related()`，Serializer 嵌套字段触发额外查询。

## 日本企业项目中的实际使用

Serializer 常被视为接口契约的实现入口。企业项目会明确字段白名单、读写属性、错误格式和兼容性；权限与查询范围仍放在 Permission 或 QuerySet，不让 Serializer 承担所有职责。

## 企业项目调查路径

```text
Request body → Serializer(data=...) → is_valid() → errors / validated_data
Model / QuerySet → Serializer(instance=...) → data → Response
```

输入问题先看 `serializer.errors`，输出问题再确认实例、字段声明和查询次数。不要一开始就修改 Model 或数据库。

## 现场任务

为员工输出增加只读 `status_label`（在职/离职），不得让客户端提交该字段。写明修改 Serializer 的原因、响应示例和非法输入测试。调查前端若只需要部门名称，为什么仍可能保留 `department` ID。

## 完成检查

- [ ] 能说明 Serializer 与 Form 的相似处和边界。
- [ ] 输入、校验、保存和输出顺序正确。
- [ ] 使用字段白名单，未泄露非契约字段。
- [ ] 正常201和错误400都有可复现证据。

下一章把函数式 API 扩展为可维护的 CRUD，并认识 APIView、Generic View、ViewSet 与 Router。
