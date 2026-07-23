# 第13章 用户组与权限控制

## 本章成果

建立“员工查看者”和“员工维护者”两种角色：两者都能查看，只有维护者能新增、编辑和删除。

## 本章开始状态与修改清单

第12章只判断是否登录。本章先在 Admin 创建用户组和测试账号，再给列表、详情和写操作 View 增加权限装饰器，最后根据相同权限调整模板按钮。

## 认证与授权不是一回事

- 认证：这个人是谁，是否已经登录。
- 授权：这个人能执行什么操作。

Django 会为 Model 自动创建 `view`、`add`、`change`、`delete` 权限。可在 Admin 中创建用户组并分配权限：

| 用户组 | 权限 |
|---|---|
| 员工查看者 | `view_employee` |
| 员工维护者 | `view_employee`、`add_employee`、`change_employee`、`delete_employee` |

## 后端强制检查

```python
from django.contrib.auth.decorators import login_required, permission_required


@login_required
@permission_required("employees.view_employee", raise_exception=True)
def employee_list(request):
    ...


@login_required
@permission_required("employees.add_employee", raise_exception=True)
def employee_create(request):
    ...


@login_required
@permission_required("employees.change_employee", raise_exception=True)
def employee_update(request, employee_id):
    ...


@login_required
@permission_required("employees.delete_employee", raise_exception=True)
def employee_delete(request, employee_id):
    ...
```

装饰器由下向上应用、请求由上向下检查。这里先用外层 `login_required` 让匿名用户跳转登录页，再让已经登录但无权限的用户得到 403。未登录与无权限是不同状态，测试时不要混为一谈。

## 模板只负责改善体验

```html
{% if perms.employees.add_employee %}
  <a href="{% url 'employees:create' %}">新增员工</a>
{% endif %}
```

按钮隐藏与后端装饰器应同时存在：前者避免误点，后者真正保护数据。

## 权限设计原则

- 权限授给用户组，用户加入组；减少逐个账号维护。
- 默认拒绝，只开放岗位需要的最小权限。
- 超级用户只用于管理和紧急调查，不作为日常业务账号。
- 个人信息的查看权限也需要控制，不能只保护编辑操作。

## 权限矩阵测试

用未登录、查看者、维护者、超级用户四类账号分别检查列表、新增、编辑和删除；记录期望的 302、200 或 403，不凭“页面看起来正常”判断。

| 账号 | 列表/详情 | 新增 | 编辑 | 删除 |
|---|---:|---:|---:|---:|
| 匿名 | 302 | 302 | 302 | 302 |
| 查看者 | 200 | 403 | 403 | 403 |
| 维护者 | 200 | 200 | 200 | 200 |

实际权限以项目分配为准。维护者如果未授予某一权限，对应操作就应为403。

## 课堂任务

隐藏编辑按钮后，手工输入编辑 URL；后端仍应返回403。截图应只使用练习账号，并同时记录请求 URL、账号角色和状态码。

现场报告：`画面上のボタン制御に加えて、View側でも権限チェックを実施しています。`

参考方向见[章节练习参考答案](practice_answers.md)，更多表达见[SES现场日语](ses_japanese.md)。

## 完成检查

- [ ] 能区分登录检查与权限检查
- [ ] 模板按钮和后端权限保持一致
- [ ] 无权限用户不能通过手工输入 URL 绕过限制

下一章将相同权限原则应用到员工附件的上传与下载。

## 代码中的权限判断

装饰器适合整条 View 的固定权限；分支判断可使用：

```python
if not request.user.has_perm("employees.change_employee"):
    raise PermissionDenied
```

模板中的 `perms.employees.change_employee` 只用于隐藏或显示操作入口。攻击者可以直接构造 URL，所以 View/API 必须重复实施真正的后端检查。

Django 默认 Model 权限控制“能否查看/新增/修改/删除这一类对象”，不会自动限制“只能查看自己部门的员工”。这种数据范围属于对象级或业务级授权，需要在 QuerySet 和操作入口共同实施，并为越权访问编写测试。

## 权限调查清单

现场新增一个权限点时，调查用户、Group、Permission 初始化方式、View、模板、API、后台、测试和既有数据。分别验证匿名用户、已登录无权限用户、有权限用户以及不在数据范围内的用户。401/登录跳转与403不要混淆：前者是身份未建立，后者是身份已知但不允许操作。
