# 第7章 Django Admin

## 本章成果

为部门和员工提供内部管理后台，并录入第8章需要的测试数据。Admin 是维护工具，不是面向普通用户的正式业务页面。

## 本章开始状态

第6章迁移已经执行，数据库中存在空的部门表和员工表。本章新建或修改 `employees/admin.py`，不修改业务列表 View。

## Admin 与 ModelAdmin 是什么

- **是什么**：Django Admin 是框架提供的内部数据管理站点；`ModelAdmin` 是控制某个 Model 在后台如何显示和操作的配置类。
- **为什么需要**：项目初期和内部运维需要安全地录入、查询和修正数据，不必为每张表立即开发业务页面。
- **什么时候使用**：维护基础数据、准备测试数据和执行有权限控制的内部操作时使用；不把它当作面向客户的正式页面。

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

这些配置都接收字段名或ModelAdmin方法名组成的序列，默认是空或Django的基础显示方式：`list_display` 决定列表列及顺序；`list_filter` 决定右侧过滤器；`search_fields` 决定搜索框查询字段；`autocomplete_fields` 决定哪些外键使用搜索选择器。字段名无效时系统检查会报错；配置类本身没有业务返回值，而是在Admin加载时改变页面行为。

代码分为两部分：`@admin.register(...)` 把 Model 与后台配置关联；继承 `admin.ModelAdmin` 的类声明列表列、筛选条件和搜索字段。这些配置只改变 Admin 的使用方式，不改变数据库字段。

## 创建管理员并登录

```bash
python manage.py createsuperuser
python manage.py runserver
```

`createsuperuser` 交互式创建拥有全部Django权限的管理员账号。默认会询问用户名、邮箱和密码，成功后数据库新增用户记录；需要先执行认证相关迁移，只在初始化管理账号时执行。可选参数如 `--username`、`--email` 能预填非秘密信息，但密码仍应安全输入，不写进命令历史。

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

## Admin 运行检查

- [ ] 能通过 Admin 录入部门和员工
- [ ] 能解释超级用户与普通后台用户的区别
- [ ] 知道数据安全不能只依靠页面按钮

## 现场识读：常用 `ModelAdmin` 配置

```python
@admin.register(Employee)
class EmployeeAdmin(admin.ModelAdmin):
    list_display = ["employee_number", "name", "department", "joined_on", "is_active"]
    search_fields = ["employee_number", "name", "email"]
    list_filter = ["department", "is_active"]
    ordering = ["employee_number"]
    fieldsets = [
        ("基本信息", {"fields": ("employee_number", "name", "department")}),
        ("任职信息", {"fields": ("joined_on", "is_active")}),
    ]
```

`fieldsets` 负责组织编辑页面，不改变后端权限和模型约束。装饰器 `@admin.register(Employee)` 与 `admin.site.register(Employee, EmployeeAdmin)` 作用相近，现场遵守项目既有风格。

`@admin.register(*models)` 接收一个或多个Model类，把紧随其后的 `ModelAdmin` 类注册到默认Admin站点；注册成功后返回该配置类供Django加载。`fieldsets` 是“分组标题、选项字典”组成的序列，其中 `fields` 必须列出当前Model或Admin中存在的字段。

`readonly_fields` 用于显示 Model 中真实存在、但不允许通过 Admin 修改的字段。例如，某个既有项目的 Model 已经定义 `created_at` 和 `updated_at` 时，才可以写：

```python
readonly_fields = ["created_at", "updated_at"]
```

当前课程的 `Employee` 尚未定义这两个字段，因此不要把这一行复制到当前项目。把必填业务字段设为只读前，还要确认新增页面如何取得初始值，否则可能导致记录无法创建。

自定义 Admin Form 用于后台独有的输入与校验；若规则必须被 API、批处理和普通页面共同遵守，应放在 Model、数据库约束或共通业务层，而不是只写在 Admin Form。

## Admin 改修的验证矩阵

至少按“列表列/搜索/筛选/排序/新增/修改/删除权限”逐项验证；配置了只读字段时再验证其显示和不可修改行为。使用普通后台账号复测，超级用户能操作成功不能证明权限配置正确。批量 action、导入导出和生产数据修正属于高风险操作，应确认影响件数、审计记录和恢复方法。

## 本章总结

Admin 是受权限保护的内部数据维护工具，不是面向最终用户的业务页面。配置列表、搜索和筛选后，要同时验证新增、修改和删除权限。下一章会让第5章的员工列表改为读取这些数据库数据。

## 日本项目中的实际使用

日本企业项目通常限制 Admin 的使用人员和可操作范围。生产数据修正往往需要作业申请、影响件数确认、双人检查和执行记录。超级用户只用于管理或紧急调查，日常操作使用最小权限账号。

## 新人常见错误

- 定义了 Model 却没有注册，Admin 中看不到该数据。
- `autocomplete_fields` 指向的关联 Admin 没有 `search_fields`，系统检查失败。
- 把 Model 中不存在的字段写入 `list_display` 或 `readonly_fields`。
- 只用超级用户验证，遗漏普通后台账号的权限问题。

## 本章知识将在后续章节继续使用

```text
Model
→ ModelAdmin 注册和显示
→ Admin 录入部门与员工
→ 第8章 ORM 从同一数据库读取
→ 第12～13章用户、Group 与 Permission 管理
```
