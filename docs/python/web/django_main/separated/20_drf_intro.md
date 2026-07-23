# 第20章 安装 DRF 与编写 Serializer

## 本章成果

安装 Django REST framework（DRF），用 `ModelSerializer` 统一员工 JSON 输出与新增输入校验，并通过第一个 DRF 接口观察标准 `Request`、`Response` 和错误结构。

## 1. 安装与注册

课程基线继续使用 Django 5.2 LTS，DRF 使用团队依赖文件锁定的兼容版本。安装并记录直接依赖：

```powershell
python -m pip install djangorestframework
```

在 `requirements.txt` 中加入团队确认的版本范围，并在 settings 注册：

```python
INSTALLED_APPS = [
    # Django 与既有 App
    "rest_framework",
    "employees.apps.EmployeesConfig",
]
```

执行 `python manage.py check`。首次看到 DRF 可浏览 API 页面只代表开发辅助界面可用，不代表认证、权限和生产配置已经完成。

## 2. Serializer 的职责

Serializer 位于 HTTP 数据与 Python/Model 对象之间：

```text
输入 JSON → 反序列化与校验 → validated_data → Model
Model/QuerySet → 序列化 → Python 基础类型 → JSON
```

它类似 Form，但服务 API 数据，不负责 URL 匹配、查询范围或最终权限。Model 约束、Serializer 场景校验和数据库约束需要共同设计。

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

`fields` 使用明确白名单，避免 Model 新增内部字段后被自动暴露。密码、审计内部字段和不必要个人信息绝不能因为“Model 中有”就进入 API。

## 4. 最小 DRF View

替换第19章手工员工列表为：

```python
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .models import Employee
from .serializers import EmployeeSerializer


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

## 5. 校验的三个层次

- 字段校验：`validate_employee_number()` 等单字段规则。
- 对象校验：`validate(self, attrs)` 比较多个字段。
- 模型/数据库约束：唯一性、外键和必须全入口遵守的完整性。

```python
def validate(self, attrs):
    if attrs.get("joined_on") and attrs["joined_on"].year < 2000:
        raise serializers.ValidationError(
            {"joined_on": "课程系统只接受2000年后的入职日期。"}
        )
    return attrs
```

示例业务限制只是练习，真实规格必须由需求确认。API 错误应稳定、可定位字段，但不要把数据库异常或堆栈原样返回。

## 6. POST 验证

```powershell
curl.exe -i -X POST http://127.0.0.1:8000/api/employees/ `
  -H "Content-Type: application/json" `
  -d '{"employee_number":"e010","name":"田中一郎","department":1,"email":"tanaka@example.test","joined_on":"2026-04-01","is_active":true}'
```

当前使用 SessionAuthentication 时，命令行 POST 还会涉及登录与 CSRF。课堂可先通过 DRF 可浏览 API 登录操作；第22章会改为 JWT 客户端认证。无论使用哪种认证，未认证请求不能因为调试方便而开放写入。

验证非法编号、重复编号、不存在部门和缺少必填字段。失败应返回400且数据库不增加记录；成功应返回201、规范化后的 `E010` 和新 ID。

## 7. 常见错误

- `ModuleNotFoundError: rest_framework`：当前解释器未安装依赖。
- `Expected a list ... but got type QuerySet`：序列化集合时漏了 `many=True`。
- 访问 `.data` 前报错：输入 Serializer 尚未执行 `is_valid()`。
- 外键只出现数字：这是默认表示；是否追加摘要字段由契约决定。
- 重复查询部门：QuerySet 未配合 `select_related()`，Serializer 嵌套字段触发额外查询。

## 现场任务

为员工输出增加只读 `status_label`（在职/离职），不得让客户端提交该字段。写明修改 Serializer 的原因、响应示例和非法输入测试。调查前端若只需要部门名称，为什么仍可能保留 `department` ID。

## 完成检查

- [ ] 能说明 Serializer 与 Form 的相似处和边界。
- [ ] 输入、校验、保存和输出顺序正确。
- [ ] 使用字段白名单，未泄露非契约字段。
- [ ] 正常201和错误400都有可复现证据。

下一章把函数式 API 扩展为可维护的 CRUD，并认识 APIView、Generic View、ViewSet 与 Router。
