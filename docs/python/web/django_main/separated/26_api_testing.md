# 第26章 DRF API 自动测试

## 本章成果

使用 `APITestCase` 和 `APIClient` 为 JWT/权限、CRUD、筛选分页和文件接口建立回归测试，并能根据失败信息判断是契约、权限、数据库还是文件问题。

## 1. 与 Django TestCase 的关系

`APITestCase` 继承 Django 测试能力，增加适合 API 的 Client 和响应处理。测试仍使用隔离数据库，不依赖 Admin 手工数据。目录较大时可拆为 `tests/test_employee_api.py` 等文件，命名表达业务行为。

```python
from datetime import date

from django.contrib.auth.models import Permission, User
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from employees.models import Department, Employee


class EmployeeApiTests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        cls.department = Department.objects.create(name="开发部")
        cls.employee = Employee.objects.create(
            employee_number="E001",
            name="山田太郎",
            department=cls.department,
            joined_on=date(2026, 4, 1),
        )
        cls.viewer = User.objects.create_user("viewer", password="test-password-123")
        permission = Permission.objects.get(codename="view_employee")
        cls.viewer.user_permissions.add(permission)
```

## 2. 认证与权限测试

业务 View 测试可使用 `force_authenticate()` 聚焦权限，不必每例先获取 JWT：

```python
def test_anonymous_user_cannot_list_employees(self):
    response = self.client.get(reverse("employee-list"))
    self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

def test_viewer_can_list_but_cannot_create(self):
    self.client.force_authenticate(self.viewer)
    list_response = self.client.get(reverse("employee-list"))
    create_response = self.client.post(reverse("employee-list"), {}, format="json")
    self.assertEqual(list_response.status_code, status.HTTP_200_OK)
    self.assertEqual(create_response.status_code, status.HTTP_403_FORBIDDEN)
```

另外为 token 获取、刷新、过期和用户停用写少量集成测试，证明真实认证配置正确。不要在普通 CRUD 测试中重复测试第三方库所有内部行为。

## 3. CRUD 与数据库断言

```python
def test_maintainer_can_create_employee(self):
    self.client.force_authenticate(self.maintainer)
    payload = {
        "employee_number": "e010",
        "name": "田中一郎",
        "department": self.department.pk,
        "email": "tanaka@example.test",
        "joined_on": "2026-04-01",
        "is_active": True,
    }
    response = self.client.post(reverse("employee-list"), payload, format="json")
    self.assertEqual(response.status_code, status.HTTP_201_CREATED)
    self.assertEqual(response.data["employee_number"], "E010")
    self.assertTrue(Employee.objects.filter(employee_number="E010").exists())
```

错误用例确认400字段结构和数据库件数不变。PATCH 确认未提交字段保持原值；DELETE 确认204、记录仍存在且 `is_active=False`；无权限写操作确认完全无变化。

## 4. 筛选、排序和分页

为每个参数至少覆盖单独有效、无效和关键组合，不需要穷举所有排列。分页测试确认 `count`、`results`、next/previous 和稳定顺序。URL 查询参数由 Client 的 `data` 构造：

```python
response = self.client.get(
    reverse("employee-list"),
    {"joined_from": "2025-04-01", "ordering": "-joined_on", "page": 1},
)
```

不要只断言200；要断言包含与排除的员工、顺序和总数。

## 5. multipart 文件测试

```python
from django.core.files.uploadedfile import SimpleUploadedFile

pdf = SimpleUploadedFile(
    "resume.pdf",
    b"%PDF-1.4\n% test file\n",
    content_type="application/pdf",
)
response = self.client.post(upload_url, {"file": pdf}, format="multipart")
```

测试使用临时 `MEDIA_ROOT`，结束后清理。覆盖正常、空文件、超限、扩展名错误、无权限和下载404。流式下载读取后调用 `response.close()`。示例字节只用于课程的轻量校验；若生产实现真正解析 PDF，测试文件也必须是有效 PDF。

## 6. API 测试分层

- Serializer：字段、跨字段和只读/写入规则。
- Permission：action 与对象/数据范围矩阵。
- View/API：状态、响应契约、数据库与文件副作用。
- Schema：生成与验证，关键路径存在。
- 前后端联调/E2E：少量关键用户流程。

每个缺陷先增加再现测试，再修复并运行相关与全量测试。避免只用 Mock 让测试通过却没有验证 ORM、权限或路由集成。

## 7. 运行与证据

```powershell
python manage.py test employees
python manage.py check
python manage.py spectacular --file schema.yml --validate
```

Review 记录 Python/Django/依赖版本、命令、通过件数和未执行条件。偶发失败先调查共享状态、时间、随机值、文件清理和顺序依赖，不用简单重跑掩盖。

## 现场任务

缺陷：查看组能逻辑删除员工。先添加失败回归测试，修复权限类，再运行员工 API 全量测试。提交说明包含再现条件、原因、修复、测试和影响范围。

## 完成检查

- [ ] APIClient 能发送 JSON、查询参数、认证和 multipart。
- [ ] 状态码、响应契约和持久化结果一起断言。
- [ ] 权限矩阵包含匿名、无权限、有权限和越权。
- [ ] 文件与测试状态可清理、可重复运行。

下一章整理 API 环境配置、结构化日志和故障调查入口。
