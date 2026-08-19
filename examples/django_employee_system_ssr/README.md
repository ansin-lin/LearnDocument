# Django 员工管理系统：基础业务项目

项目已经包含部门、员工、权限、附件、数据库配置、迁移和可运行的 Django 页面。

开发 REST API 时，先复用现有 Model、ORM、权限和数据库配置。已有 Template、Form 和页面 View 不需要修改，也不会成为 API 开发的前置条件。当前项目尚未安装 DRF，也没有 JWT、CORS 或 OpenAPI 配置；启动并确认基础状态后，再新增 API 相关文件。

## 环境

- Python 3.12+
- Django 5.2 LTS
- SQLite（本地学习环境）

## 从零启动

在本目录执行：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver
```

登录 Admin 后创建部门、员工、用户组和测试账号。员工列表需要 `employees.view_employee`；新增、编辑、逻辑删除和附件上传分别需要对应 Model 权限。

## 验证

```powershell
python manage.py check
python manage.py makemigrations --check
python manage.py test employees
python manage.py check --deploy
```

`check --deploy` 对本地开发配置给出安全提醒是预期结果。生产环境必须提供独立密钥、关闭 DEBUG、限制主机名，并配置 HTTPS、应用服务器、Static/Media、日志、备份和回滚。

## 主要地址

- `/admin/`：内部数据和权限维护
- `/login/`：登录
- `/employees/`：员工搜索与分页
- `/health/`：确认 Django 进程能够响应

用户上传文件保存在本地 `media/`，但没有配置公开 Media 路由；附件只能通过带权限检查的下载 View 获取。
