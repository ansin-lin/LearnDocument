# 第13章 用户组与权限控制

## 本章成果

建立“员工查看者”和“员工维护者”两种角色：两者都能查看，只有维护者能新增、编辑和删除。

## 本章开始状态与修改清单

第12章只判断是否登录。本章先在 Admin 创建用户组和测试账号，再给列表、详情和写操作 View 增加权限装饰器，最后根据相同权限调整模板按钮。

## 认证与授权不是一回事

- 认证：这个人是谁，是否已经登录。
- 授权：这个人能执行什么操作。

Django 会为 Model 自动创建 `view`、`add`、`change`、`delete` 权限。可在 Admin 中创建用户组并分配权限：

- **Permission 是什么**：表示用户是否被允许执行某类操作的授权记录。
- **Group 是什么**：把多项权限组合成角色，再把用户加入角色。
- **为什么需要**：登录只能确认身份，不能说明每个人都能查看或修改全部数据。
- **什么时候使用**：页面、View、API 或后台操作需要按岗位限制时使用，并在后端执行最终检查。

```text
User
→ 加入 Group
→ Group 拥有 Permission
→ View 检查 Permission
→ 允许处理或返回403
```

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

`permission_required()` 的权限名由“App 标签.权限代号”组成；`raise_exception=True` 让已登录但无权限的用户得到403。装饰器没有改变 View 内部业务代码，而是在进入函数前完成统一检查。

`permission_required(perm, login_url=None, raise_exception=False)` 中，`perm` 必须是一个权限名或权限名序列；`login_url` 可选，用于未通过时跳转；`raise_exception` 默认为 `False`，本章设为 `True` 以返回403。它返回装饰器，再生成包装后的View函数。

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

## 权限功能运行检查

- [ ] 能区分登录检查与权限检查
- [ ] 模板按钮和后端权限保持一致
- [ ] 无权限用户不能通过手工输入 URL 绕过限制

## 现场识读：代码中的权限判断

装饰器适合整条 View 的固定权限；分支判断可使用：

```python
if not request.user.has_perm("employees.change_employee"):
    raise PermissionDenied
```

`PermissionDenied` 是表示当前请求已被拒绝的异常类，不需要构造参数即可抛出；未被业务代码捕获时由Django转换为403响应。需要在View分支中主动拒绝操作时使用，不用于“资源不存在”的404场景。

`has_perm(perm, obj=None)` 的权限名必填，`obj` 是可选对象；返回布尔值。Django默认Model权限主要判断全局权限，传入对象并不会自动产生部门范围规则，除非项目配置了支持对象权限的认证后端。

模板中的 `perms.employees.change_employee` 只用于隐藏或显示操作入口。攻击者可以直接构造 URL，所以 View/API 必须重复实施真正的后端检查。

Django 默认 Model 权限控制“能否查看/新增/修改/删除这一类对象”，不会自动限制“只能查看自己部门的员工”。这种数据范围属于对象级或业务级授权，需要在 QuerySet 和操作入口共同实施，并为越权访问编写测试。

## 权限调查清单

现场新增一个权限点时，调查用户、Group、Permission 初始化方式、View、模板、API、后台、测试和既有数据。分别验证匿名用户、已登录无权限用户、有权限用户以及不在数据范围内的用户。401/登录跳转与403不要混淆：前者是身份未建立，后者是身份已知但不允许操作。

## 现场识读：类视图与权限 Mixin

本课程主线使用函数 View，便于直接观察请求处理顺序。既有项目也常使用类视图和通用 View：

```python
from django.contrib.auth.mixins import LoginRequiredMixin, PermissionRequiredMixin
from django.views.generic import ListView


class EmployeeListView(
    LoginRequiredMixin,
    PermissionRequiredMixin,
    ListView,
):
    model = Employee
    template_name = "employees/list.html"
    context_object_name = "employees"
    permission_required = "employees.view_employee"

    def get_queryset(self):
        return (
            super()
            .get_queryset()
            .filter(is_active=True)
            .select_related("department")
        )
```

路由通过 `EmployeeListView.as_view()` 注册。阅读类视图时先确认父类和 Mixin 顺序，再追踪 `dispatch()`、`get_queryset()`、`get_context_data()`、`form_valid()` 等实际覆盖点。`ListView`、`DetailView`、`CreateView`、`UpdateView` 能减少重复代码，但不会自动补齐业务权限、数据范围和查询优化。

- **Mixin是什么**：通过多重继承向类视图增加单一职责行为的类；登录、权限等共通检查需要复用时使用。
- `get_queryset()` 不接业务参数，返回当前页面要使用的QuerySet；覆盖时通常先调用 `super().get_queryset()` 再追加范围。
- `as_view(**initkwargs)` 把类和可选初始化配置转换为路由可调用的View函数。Mixin顺序会影响请求先经过哪个检查。

## 本章总结

认证确认用户身份，授权决定可执行的操作。模板权限只改善体验，View 必须执行真正的后端检查；全局 Model 权限也不能代替部门或对象级数据范围。下一章将相同原则应用到员工附件的上传与下载。

## 日本项目中的实际使用

日本企业系统常用权限矩阵明确角色与操作，并把权限授给 Group 而不是逐个用户。规格和测试会分别确认匿名、无权限、有权限和数据范围外用户。画面按钮控制与后端权限检查必须对应，但安全判断以后端为准。

## 新人常见错误

- 把登录成功当成拥有全部权限，混淆认证与授权。
- 只在 Template 隐藏按钮，手工访问 URL 仍能执行操作。
- 权限字符串写错 App 标签或 codename，所有用户都被拒绝。
- 只测试超级用户，未发现普通角色缺少权限。
- 认为 Model Permission 会自动限制“只能看自己部门”，忽略业务数据范围。

## 本章知识将在后续章节继续使用

```text
request.user
→ Group
→ Permission
→ View 后端检查 + Template 显示控制
→ 第14章安全下载
→ 第16章权限回归测试
→ 第22章 API 权限
```
