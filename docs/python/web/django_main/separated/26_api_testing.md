# 第26章 DRF API 自动测试

## 本章成果

使用 `APITestCase` 和 `APIClient` 为JWT、操作权限、部门数据范围、CRUD、筛选分页和文件接口建立可重复的回归测试。测试失败时能够判断问题属于认证、权限、契约、数据库还是文件存储。

## 本章开始状态与测试文件

- 第22章已生成用户—部门访问关系迁移。
- 第23—25章的查询、前端和文件接口已经完成。
- 本章创建或整理 `employees/tests_api.py`，测试不依赖 Admin 手工数据。

## 本章在整体架构中的位置

```text
Test Data → APIClient → Router / ViewSet / Permission / Serializer → Database
                ↑                         ↑ 本章验证范围
             Assert ← Status / JSON / 数据与文件副作用
```

完成后，前面建立的调用链将拥有可重复执行的安全网，接口改修不再只依赖手工点击确认。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| `APITestCase` | 为 DRF API 测试准备客户端和隔离数据库的测试类 | 让请求、数据准备和断言可重复执行 | 验证 API 契约及数据库副作用时 |
| `APIClient` | 测试代码中的 DRF HTTP 客户端 | 模拟认证、JSON 和 multipart 请求 | 调用 URL 并检查完整响应流程时 |
| 回归测试 | 固定已确认行为、防止旧缺陷重现的测试 | 降低改修破坏权限和既有契约的风险 | 修复缺陷或变更接口前后 |

## 先理解 API 测试在验证什么

API 测试不是检查某个函数是否被调用，而是从客户端入口观察接口的外部行为和副作用：

```text
Arrange：建立部门、员工、用户和权限
→ Act：APIClient 发送真实 URL 请求
→ Assert：检查状态码、响应字段、数据库和文件
→ Cleanup：测试数据库回滚，临时文件删除
```

- **APITestCase 是什么**：DRF 基于 Django 测试机制提供的 API 测试基类，包含隔离测试数据库和 `self.client`。
- **APIClient 是什么**：测试进程内使用的 HTTP 客户端，可以发送 JSON、multipart 和认证请求。
- **为什么测试不使用 Admin 数据**：测试必须在任何新环境中得到同样结果，不能依赖开发者手工创建的账号或记录。
- **什么时候写回归测试**：新增功能、修复缺陷和修改权限前后；先让用例重现风险，再修改实现。

每个重要请求至少检查四个方面：HTTP 状态是否正确，JSON 契约是否正确，数据库是否发生预期变化，失败时是否没有产生不应出现的副作用。只断言200，无法证明返回了正确员工，也无法发现越权或错误写入。

JWT 端点需要少量真实登录与刷新测试，确认路由和配置可以协作；大量业务权限测试可以使用 `force_authenticate()` 直接指定用户，把失败原因集中在本项目的 Permission 和数据范围。后者不能替代真实 JWT 流程测试。

## 1. 建立完整测试基线

新建或替换 `employees/tests_api.py`。以下是该文件的完整导入、测试类和共享数据基线；后续各节的方法都继续追加到 `EmployeeApiTests` 类内：

```python
from datetime import date
from tempfile import TemporaryDirectory

from django.contrib.auth.models import Permission, User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import (
    Department,
    Employee,
    UserDepartmentAccess,
)


class EmployeeApiTests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        cls.development = Department.objects.create(name="开发部")
        cls.sales = Department.objects.create(name="营业部")
        cls.employee = Employee.objects.create(
            employee_number="E001",
            name="山田太郎",
            department=cls.development,
            joined_on=date(2026, 4, 1),
        )
        cls.sales_employee = Employee.objects.create(
            employee_number="E002",
            name="佐藤花子",
            department=cls.sales,
            joined_on=date(2025, 10, 1),
        )
        cls.inactive_employee = Employee.objects.create(
            employee_number="E003",
            name="鈴木次郎",
            department=cls.development,
            joined_on=date(2024, 4, 1),
            is_active=False,
        )

        cls.viewer = User.objects.create_user(
            "api-viewer",
            password="test-password-123",
        )
        cls.maintainer = User.objects.create_user(
            "api-maintainer",
            password="test-password-123",
        )
        UserDepartmentAccess.objects.create(
            user=cls.viewer,
            department=cls.development,
        )
        UserDepartmentAccess.objects.create(
            user=cls.maintainer,
            department=cls.development,
        )

        permissions = Permission.objects.filter(
            content_type__app_label="employees",
        )
        cls.viewer.user_permissions.add(
            permissions.get(codename="view_employee"),
        )
        cls.maintainer.user_permissions.add(
            *permissions.filter(
                codename__in=[
                    "view_employee",
                    "add_employee",
                    "change_employee",
                    "delete_employee",
                    "add_employeeattachment",
                ],
            )
        )

        cls.list_url = reverse("employee-list")
        cls.detail_url = reverse(
            "employee-detail",
            args=[cls.employee.pk],
        )
        cls.upload_url = reverse(
            "employee-attachments",
            args=[cls.employee.pk],
        )
```

`APITestCase` 是 DRF 基于 Django 测试事务封装的 API 测试类，每个测试拥有隔离数据库并通过 `self.client` 调用接口。`setUpTestData(cls)` 是 Django 的类级测试数据钩子，在该测试类开始时建立只读基线；需要在单个测试修改的数据应在用例中另行准备。`reverse(name, args=...)` 根据路由名称返回 URL，避免测试硬编码路径。

`Model.objects.create(**fields)` 接收模型字段的关键字参数，立即向测试数据库执行 INSERT，并返回已经保存且具有主键的 Model 对象。本例使用它建立部门、员工和用户—部门关系；字段缺失、类型错误或违反数据库约束时会抛出异常。所有数据只存在于 `APITestCase` 的隔离测试数据库。

`User.objects.create_user(username, password, ...)` 在普通 `create()` 基础上正确处理密码哈希并返回用户对象，密码参数不可使用真实凭据。`Permission.objects.filter(**conditions)` 返回可以继续组合或遍历的 QuerySet；本例先用 `content_type__app_label` 限定为 `employees` App 的权限。`permissions.get(codename=...)` 从该集合取得唯一权限，不存在或重复时会抛出异常；`*permissions.filter(...)` 把第二次筛选得到的多个权限对象展开为 `add()` 的位置参数。`user_permissions.add(*objects)` 建立用户与一个或多个权限的多对多关系，没有需要使用的返回值。

`date(year, month, day)` 来自 Python 标准库 `datetime`，三个整数参数必填，返回不含时间和时区的日期对象，适合本项目 `DateField` 测试数据。

测试数据明确区分两个部门、在职与离职员工、查看者和维护者。所有测试都从同一基线开始，名称与接口契约一致。

## 2. 真实JWT与业务认证

token端点使用真实账号密码做少量集成测试：

以下方法继续追加到 `employees/tests_api.py` 的 `EmployeeApiTests` 类内：

Django 测试类继承 Python `unittest` 的断言方法。断言成功时没有需要使用的返回值，条件不成立时会抛出 `AssertionError` 并让当前测试失败：

- `self.assertEqual(first, second, msg=None)` 比较两个值是否相等；前两个参数必填，`msg` 是失败时可选的补充信息。本例用它比较实际状态码和期望状态码。
- `self.assertIn(member, container, msg=None)` 检查成员是否包含在字典、列表、集合或字符串中。本例确认刷新响应字典包含 `access` 键。
- `self.assertTrue(expr, msg=None)` 检查表达式结果是否为真，适合确认数据库记录存在等布尔条件。能直接比较具体值时优先使用 `assertEqual()`，失败信息通常更清楚。

```python
def test_token_can_be_obtained_and_refreshed(self):
    token_response = self.client.post(
        reverse("token-obtain-pair"),
        {
            "username": "api-viewer",
            "password": "test-password-123",
        },
        format="json",
    )
    self.assertEqual(
        token_response.status_code,
        status.HTTP_200_OK,
    )
    refresh_response = self.client.post(
        reverse("token-refresh"),
        {"refresh": token_response.data["refresh"]},
        format="json",
    )
    self.assertEqual(
        refresh_response.status_code,
        status.HTTP_200_OK,
    )
    self.assertIn("access", refresh_response.data)


def test_anonymous_user_cannot_list_employees(self):
    response = self.client.get(self.list_url)
    self.assertEqual(
        response.status_code,
        status.HTTP_401_UNAUTHORIZED,
    )
```

普通业务测试使用 `force_authenticate()` 聚焦本项目权限，不需要每个用例都重复测试第三方JWT内部实现。另补用户停用、错误密码和无效refresh用例。

`self.client.post(path, data, format="json")` 把字典编码为 JSON 并返回测试响应；`format` 可使用已配置的 DRF 测试格式，这里使用 `json` 或 `multipart`。`force_authenticate(user=...)` 只在测试客户端中直接指定认证用户，不签发 JWT，适合隔离业务权限，但不能替代少量真实 token 流程测试。

### 阶段检查：先运行认证基线

保存 `employees/tests_api.py` 后，在项目根目录执行：

```powershell
python manage.py test employees.tests_api.EmployeeApiTests.test_token_can_be_obtained_and_refreshed
python manage.py test employees.tests_api.EmployeeApiTests.test_anonymous_user_cannot_list_employees
```

两项都通过后再增加权限、CRUD和文件测试。第一项失败时调查 token 路由、账号和JWT配置；第二项失败时调查默认认证与权限配置。不要在认证基线尚未稳定时一次加入后面所有测试。

`python manage.py test [test_label]` 运行 Django 测试并返回进程退出状态；`test_label` 可省略以运行全部测试，也可以写 App、模块、测试类或完整测试方法路径。本例提供到方法级标签，只运行两条认证基线；后面的 `python manage.py test employees` 以 App 标签运行员工 App 的全部测试。

## 3. 操作权限与部门数据范围

以下方法继续追加到 `employees/tests_api.py` 的 `EmployeeApiTests` 类内：

```python
def test_viewer_only_sees_assigned_department(self):
    self.client.force_authenticate(self.viewer)
    response = self.client.get(self.list_url)

    self.assertEqual(response.status_code, status.HTTP_200_OK)
    numbers = {
        item["employee_number"]
        for item in response.data["results"]
    }
    self.assertEqual(numbers, {"E001"})


def test_other_department_detail_is_hidden(self):
    self.client.force_authenticate(self.viewer)
    response = self.client.get(
        reverse(
            "employee-detail",
            args=[self.sales_employee.pk],
        )
    )
    self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


def test_viewer_cannot_create(self):
    self.client.force_authenticate(self.viewer)
    response = self.client.post(self.list_url, {}, format="json")
    self.assertEqual(
        response.status_code,
        status.HTTP_403_FORBIDDEN,
    )
```

还要覆盖维护者提交未授权营业部ID、没有离职查看权限时查询 `is_active=false`、授予自定义权限后的离职查询。失败时断言数据库没有变化。

## 4. CRUD与数据库断言

以下方法继续追加到 `employees/tests_api.py` 的 `EmployeeApiTests` 类内：

```python
def test_maintainer_can_create_employee(self):
    self.client.force_authenticate(self.maintainer)
    payload = {
        "employee_number": "e010",
        "name": "田中一郎",
        "department": self.development.pk,
        "email": "tanaka@example.test",
        "joined_on": "2026-04-01",
        "is_active": True,
    }
    response = self.client.post(
        self.list_url,
        payload,
        format="json",
    )
    self.assertEqual(
        response.status_code,
        status.HTTP_201_CREATED,
    )
    self.assertEqual(response.data["employee_number"], "E010")
    self.assertTrue(
        Employee.objects.filter(employee_number="E010").exists()
    )
```

错误用例确认400字段结构和数据库件数不变。PATCH确认未提交字段保持原值；DELETE确认204、记录仍存在且 `is_active=False`；无权限写操作确认完全无变化。

## 5. 筛选、排序和分页

以下方法继续追加到 `employees/tests_api.py` 的 `EmployeeApiTests` 类内：

```python
def test_filters_and_pagination_keep_data_scope(self):
    self.client.force_authenticate(self.viewer)
    response = self.client.get(
        self.list_url,
        {
            "department": self.sales.pk,
            "search": "佐藤",
            "ordering": "-joined_on",
            "page": 1,
        },
    )
    self.assertEqual(response.status_code, status.HTTP_200_OK)
    self.assertEqual(response.data["count"], 0)
    self.assertEqual(response.data["results"], [])
```

查询测试不能只断言200；要断言包含与排除对象、稳定顺序、count、next和previous。为非法日期、From晚于To、未允许排序字段和最后一页补测试。

`self.client.get(path, data=None, follow=False, **extra)` 的路径必填；`data` 传入字典时会编码为查询字符串，`follow=True` 才会自动跟随重定向，省略时为 `False`，额外关键字参数可设置测试请求环境。方法执行一次 GET 并返回测试响应对象。本例把部门、搜索、排序和页码字典作为查询参数，不会把它们写入请求体。

## 6. multipart文件测试与清理

以下方法继续追加到 `employees/tests_api.py` 的 `EmployeeApiTests` 类内：

```python
def test_pdf_can_be_uploaded(self):
    self.client.force_authenticate(self.maintainer)
    pdf = SimpleUploadedFile(
        "resume.pdf",
        b"%PDF-1.4\n% test file\n",
        content_type="application/pdf",
    )

    with TemporaryDirectory() as media_root:
        with override_settings(MEDIA_ROOT=media_root):
            response = self.client.post(
                self.upload_url,
                {"file": pdf},
                format="multipart",
            )

    self.assertEqual(
        response.status_code,
        status.HTTP_201_CREATED,
    )


def test_fake_pdf_is_rejected(self):
    self.client.force_authenticate(self.maintainer)
    fake_pdf = SimpleUploadedFile(
        "resume.pdf",
        b"plain text",
        content_type="application/pdf",
    )

    with TemporaryDirectory() as media_root:
        with override_settings(MEDIA_ROOT=media_root):
            response = self.client.post(
                self.upload_url,
                {"file": fake_pdf},
                format="multipart",
            )

    self.assertEqual(
        response.status_code,
        status.HTTP_400_BAD_REQUEST,
    )
```

继续覆盖空文件、超限、错误扩展名、无权限、其他部门员工和下载404。流式下载读取后调用 `response.close()`；该方法不接收参数，用于关闭响应持有的流和文件资源，没有需要使用的返回值。若生产实现真正解析PDF，测试文件也必须是有效PDF。

`SimpleUploadedFile(name, content, content_type=...)` 创建内存上传文件，`name` 和字节 `content` 必填；`content_type` 只是测试请求元数据。`TemporaryDirectory()` 创建在退出 `with` 后自动清理的临时目录；`override_settings(MEDIA_ROOT=...)` 只在上下文期间替换 Django 配置，防止测试文件写入开发 Media 目录。

## 7. 测试分层与运行证据

- Serializer：字段、跨字段、只读/写入和部门输入规则。
- Permission与access helper：action、部门和离职数据矩阵。
- View/API：状态码、响应契约、数据库与文件副作用。
- JWT：少量token获取、refresh、失效和停用用户集成测试。
- Schema：生成、验证和关键路径存在。
- 前端联调/E2E：登录、列表、refresh和上传等少量关键流程。

在项目根目录激活虚拟环境后运行：

```powershell
python manage.py test employees
python manage.py check
python manage.py spectacular --file schema.yml --validate
```

Review记录Python、Django、直接依赖版本、命令、通过件数和未执行条件。每个缺陷先增加再现测试，再修复并运行相关与全量测试。

## 日本企业项目中的实际使用

API 改修通常要求测试证据与代码同时提交。企业项目更重视可重复的权限矩阵、边界条件和数据副作用，而不是只写一个返回200的正常场景。

## 新人常见错误

- 测试依赖开发数据库或 Admin 中手工建立的数据。
- 只断言状态码，不检查响应字段和数据库变化。
- 全部使用 `force_authenticate()`，遗漏真实 JWT 获取与失效流程。
- 文件测试写入正式 Media 目录并留下残留文件。
- 先修代码再补测试，无法证明缺陷能够被再现。

## 企业项目调查路径

```text
失败测试 → 固定输入与期望 → Authentication / Permission
→ View / Serializer → Database / Storage 副作用 → 最小修复 → 全量回归
```

先单独运行最小失败场景，再根据断言位置判断层次；修复后同时运行相关测试、全量测试和 schema 验证。

## 现场任务

缺陷：过滤营业部后，开发部查看者看到了营业部员工。先添加失败回归测试，修复QuerySet与Filter Backend顺序，再验证列表、详情和文件接口都不能越权。

## 完成检查

- [ ] 测试基线定义了账号、权限、部门范围和业务数据。
- [ ] JWT真实集成测试与业务force认证测试职责清楚。
- [ ] 状态码、响应契约、数据范围和持久化结果一起断言。
- [ ] 文件测试使用临时存储并可重复运行。
- [ ] 查询参数不能扩大授权数据集合。

下一章整理环境配置、请求ID、结构化日志和稳定JSON异常响应。
