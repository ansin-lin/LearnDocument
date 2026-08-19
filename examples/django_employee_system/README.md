# Django 员工管理系统参考项目

这是第1–29章的最终参考状态，同时保留服务端渲染页面和 DRF API。建议先独立完成每章任务，再用本项目核对文件组合方式。

## 环境

- Python 3.10+
- Django 5.2 LTS
- SQLite（本地学习环境）

## 从零启动

以下命令在本目录执行：

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
python manage.py test
python manage.py check --deploy
python manage.py spectacular --file schema.yml --validate
```

`check --deploy` 会针对生产配置给出提醒；本示例默认是本地开发配置，出现安全提醒是预期的。生产环境必须提供独立密钥、关闭 DEBUG、限制主机名，并另行配置 HTTPS、Static/Media、日志、备份和回滚。

## 主要地址

- `/admin/`：内部数据和权限维护
- `/login/`：登录
- `/employees/`：员工搜索与分页
- `/health/`：仅确认 Django 进程可以响应，不代表数据库等依赖完全健康
- `/api/health/`：API 健康检查
- `/api/auth/token/`：获取 JWT access/refresh token
- `/api/employees/`：员工 CRUD、筛选、搜索、排序和分页
- `/api/docs/`：开发环境 OpenAPI 文档

用户上传文件保存在本地 `media/`，但没有配置公开 Media 路由；附件只能通过带权限检查的下载 View 获取。

API 默认使用 JWT。先在 Admin 为练习用户授予 employees 的查看或维护权限，再获取 token。允许联调的前端来源由 `DJANGO_CORS_ALLOWED_ORIGINS` 配置；生产不得保留不需要的本地来源。

普通API用户还需要在Admin的“用户部门访问”中分配可查看部门；超级用户可查看全部部门。启动最小联调前端：

```powershell
python -m http.server 5173 --directory frontend
```

然后访问 `http://localhost:5173/`。前端只用于课程联调，不保存真实账号或token。
