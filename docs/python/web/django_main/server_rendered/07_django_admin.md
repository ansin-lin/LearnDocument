# 第7章 Django Admin

## 本章成果

为部门和员工提供内部管理后台，并录入第8章需要的测试数据。Admin 是维护工具，不是面向普通用户的正式业务页面。

## 本章开始状态

第6章迁移已经执行，数据库中存在空的部门表和员工表。本章新建或修改 `employees/admin.py`，不修改业务列表 View。

## 注册模型

编辑 `employees/admin.py`：

```python
from django.contrib import admin

from .models import Department, Employee


@admin.register(Department)
class DepartmentAdmin(admin.ModelAdmin):
    search_fields = ["name"]


@admin.register(Employee)
class EmployeeAdmin(admin.ModelAdmin):
    list_display = ["employee_number", "name", "department", "joined_on", "is_active"]
    list_filter = ["department", "is_active"]
    search_fields = ["employee_number", "name", "email"]
    autocomplete_fields = ["department"]
```

`list_display` 控制列表列，`list_filter` 提供筛选，`search_fields` 提供搜索。`autocomplete_fields` 依赖关联模型配置 `search_fields`。

## 创建管理员并登录

```bash
python manage.py createsuperuser
python manage.py runserver
```

访问 `http://127.0.0.1:8000/admin/`，依次录入部门和员工。至少准备两个部门、三名员工，员工编号不要重复。

建议使用下列不含真实个人信息的练习数据：

| 员工编号 | 姓名 | 部门 | 入职日期 |
|---|---|---|---|
| E001 | 山田太郎 | 开发部 | 2026-04-01 |
| E002 | 佐藤花子 | 营业部 | 2025-10-01 |
| E003 | 铃木一郎 | 开发部 | 2024-07-15 |

## 用业务规则检查后台

尝试删除仍有员工的部门，应因第6章的 `PROTECT` 而失败。这说明安全限制应放在数据模型或服务端，而不是只隐藏按钮。

## 现场注意事项

- 不共享管理员账号；每个人使用自己的账号。
- 只授予工作需要的权限；普通运营人员不应默认成为超级用户。
- 后台数据也是真实业务数据，批量操作前确认范围并保留审计线索。
- 不把 Admin 当作客户页面交付；它适合内部维护和故障调查。

## 常见问题

- 登录后看不到模型：确认模型已注册，并检查用户权限。
- 修改模型后后台报数据库错误：确认迁移已生成并执行。
- 部门下拉项太多：使用自动完成，不加载巨大的完整下拉框。

## 动手任务

1. 在员工列表中增加邮箱列。
2. 按部门和在职状态组合筛选。
3. 创建一个非超级用户，只授予查看员工的权限，确认其不能修改数据。

### 验证证据

保存一张不含真实个人信息的 Admin 员工列表截图，画面应同时包含配置后的列、搜索框和筛选器。再记录“查看账号能打开列表但没有新增、修改权限”的实际结果。

现场说明可写：`初期データをAdminから登録し、一覧・検索・絞り込みを確認しました。`

参考方向见[章节练习参考答案](practice_answers.md)。

## 完成检查

- [ ] 能通过 Admin 录入部门和员工
- [ ] 能解释超级用户与普通后台用户的区别
- [ ] 知道数据安全不能只依靠页面按钮

下一章会让第5章的员工列表改为读取这些数据库数据。

## 常用 `ModelAdmin` 配置

```python
@admin.register(Employee)
class EmployeeAdmin(admin.ModelAdmin):
    list_display = ["employee_number", "name", "department", "joined_on", "is_active"]
    search_fields = ["employee_number", "name", "email"]
    list_filter = ["department", "is_active"]
    ordering = ["employee_number"]
    readonly_fields = ["created_at", "updated_at"]
    fieldsets = [
        ("基本信息", {"fields": ("employee_number", "name", "department")}),
        ("任职信息", {"fields": ("joined_on", "is_active")}),
    ]
```

只有 Model 实际存在对应字段时才能加入 `readonly_fields`。`fieldsets` 负责组织编辑页面，不改变后端权限和模型约束。装饰器 `@admin.register(Employee)` 与 `admin.site.register(Employee, EmployeeAdmin)` 作用相近，现场遵守项目既有风格。

自定义 Admin Form 用于后台独有的输入与校验；若规则必须被 API、批处理和普通页面共同遵守，应放在 Model、数据库约束或共通业务层，而不是只写在 Admin Form。

## Admin 改修的验证矩阵

至少按“列表列/搜索/筛选/排序/只读字段/新增/修改/删除权限”逐项验证，并使用普通后台账号复测。超级用户能操作成功不能证明权限配置正确。批量 action、导入导出和生产数据修正属于高风险操作，应确认影响件数、审计记录和恢复方法。
