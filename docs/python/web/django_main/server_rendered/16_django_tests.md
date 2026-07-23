# 第16章 Django 基础测试

## 本章成果

为员工列表、权限和逻辑删除建立最小回归测试。以后修改代码时，不再只靠手工点击判断。

## 本章开始状态与测试范围

第15章结束时功能已经可以手工操作。本章不依赖本地 Admin 数据，而是在测试代码中创建部门、员工、用户和权限。首先覆盖风险最高的权限、数据写入、逻辑删除和文件处理。

## 测试数据与列表测试

在 `employees/tests.py` 中编写：

```python
from datetime import date

from django.contrib.auth.models import Permission, User
from django.test import TestCase
from django.urls import reverse

from .models import Department, Employee


class EmployeeViewTests(TestCase):
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
        permission = Permission.objects.get(
            content_type__app_label="employees",
            content_type__model="employee",
            codename="view_employee",
        )
        cls.viewer.user_permissions.add(permission)

    def test_anonymous_user_is_redirected_to_login(self):
        response = self.client.get(reverse("employees:list"))
        self.assertEqual(response.status_code, 302)

    def test_viewer_can_open_list(self):
        self.client.force_login(self.viewer)
        response = self.client.get(reverse("employees:list"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "山田太郎")
```

## 逻辑删除测试

为测试用户增加删除权限后 POST 删除地址，再用 `refresh_from_db()` 确认记录仍存在但 `is_active=False`。同时补一个 GET 测试，确认 GET 不修改数据。

完整参考项目还覆盖：日期和关键字组合、错误日期范围、无权限403、非 PDF 拒绝、安全下载和404。查看[最终参考项目](reference_project.md)中的 `employees/tests.py`，但先自行实现最小用例。

## 怎么选择测试

- Model：约束、默认值、业务方法。
- Form：合法、边界和非法输入。
- View：状态码、模板、重定向、消息、数据库变化。
- 权限：至少覆盖匿名、无权限、有权限三类用户。

测试名称要表达条件和结果，不写 `test_1`。每个缺陷修复先增加一个会失败的回归测试，再修改实现。

## 运行

```bash
python manage.py test employees
python manage.py check
```

测试使用独立测试数据库，不应依赖本机 Admin 中手工录入的数据，也不应依赖执行顺序。

文件下载响应是流式响应。测试读取后应调用 `response.close()`，否则 Windows 可能因文件仍被占用而无法清理临时目录。

### 课堂故障任务

暂时把列表 View 的 `is_active=True` 删除，运行测试并阅读失败用例；恢复实现后确认全部通过。报告要写“哪个行为退化”，而不是只写“测试红了”。

现场报告：`不具合を再現するテストを追加してから修正し、回帰テスト全件成功を確認しました。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 完成检查

- [ ] 测试可重复运行且结果一致
- [ ] 正常路径、输入错误和权限错误都有代表性用例
- [ ] 失败信息能指出哪个行为被破坏

下一章将代码、配置、迁移、测试和说明整理成他人可重新搭建的交付物。

## Client 与常用断言

`self.client` 模拟浏览器请求，可发送 GET、POST、登录和文件。常用断言包括：

```python
self.assertEqual(response.status_code, 200)
self.assertContains(response, "山田太郎")
self.assertRedirects(response, reverse("employees:list"))
self.assertTemplateUsed(response, "employees/list.html")
self.assertFormError(response.context["form"], "employee_number", "员工编号必须以 E 开头。")
```

断言应验证业务结果，不要只验证“没抛异常”。写操作同时检查响应、数据库变化、未变化字段和消息。按 Arrange（准备）、Act（执行）、Assert（确认）组织测试，失败时更容易定位。

`setUpTestData()` 为测试类准备不会被用例直接修改的共通数据；`setUp()` 在每个测试前执行，适合每例都需重建的状态。测试数据库与开发数据库隔离，每个用例也应独立，不依赖执行顺序。

## 回归测试的现场用法

缺陷改修先写一个能失败的用例，证明测试确实捕获问题；修复后运行相关测试，再运行全量回归。文件测试使用临时文件和临时存储，结束时关闭流并清理资源。时间、外部 API 和随机值应可控制，避免偶发失败。

测试数量不是目标。优先覆盖权限越权、数据写入、边界条件、错误分支和过去出现过的缺陷，并在 Review 说明执行命令与结果。
