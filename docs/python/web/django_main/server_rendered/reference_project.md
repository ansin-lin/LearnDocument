# 员工管理系统最终参考项目

完成各章练习后，可以下载[最终参考项目](downloads/django_employee_system.zip)核对文件组合方式。参考项目不是起步模板；如果一开始就复制最终代码，会失去观察系统逐步变化的机会。

## 参考项目包含什么

- Django 5.2 LTS 与 SQLite 开发配置
- 部门、员工和员工附件 Model 及初始迁移
- Admin、列表、详情、增删改、搜索、日期筛选和分页
- 登录、用户组权限、逻辑删除和受控文件下载
- 日志、请求 ID、403/404/500 页面
- 11个覆盖权限、筛选、CRUD 和附件的自动测试
- README、依赖文件、环境变量示例和 `.gitignore`
- DRF 员工 CRUD、JWT、权限、筛选分页、附件 API 与 OpenAPI
- 服务端页面测试和 API 自动测试，可同时验证一体式与分离式开发

## 从零运行

解压后，在项目根目录执行：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver
```

随后访问 `/admin/` 创建部门、员工、用户组和测试用户。运行测试：

```powershell
python manage.py check
python manage.py test
python manage.py makemigrations --check
python manage.py spectacular --file schema.yml --validate
```

API 入口为 `/api/employees/`，JWT 获取入口为 `/api/auth/token/`，开发文档为 `/api/docs/`。完整学习顺序见[前后端分离路线](../separated/index.md)，不要只运行最终代码跳过接口契约、权限和测试过程。

## 章节检查点

| 章节 | 运行后必须观察到的结果 | 主要变化文件 |
|---:|---|---|
| 01–04 | 首页、员工列表入口和详情入口返回正确状态码 | `views.py`、两级 `urls.py` |
| 05 | 固定员工列表、空数据状态和 CSS 请求 | `templates/`、`static/` |
| 06 | 两张业务表及初始迁移 | `models.py`、`migrations/` |
| 07 | Admin 可维护部门和员工 | `admin.py` |
| 08 | 列表与详情来自数据库，不存在数据返回404 | `views.py`、列表与详情模板 |
| 09–10 | 新增、编辑、逻辑删除和消息提示 | `forms.py`、CRUD View 与模板 |
| 11 | 关键字搜索、日期条件和分页可组合 | 搜索 Form、列表 View 与模板 |
| 12–13 | 匿名用户、查看者和维护者行为不同 | 认证路由、装饰器、模板按钮 |
| 14 | PDF 上传受限，下载必须经过权限 View | 附件 Model、Form、View 与模板 |
| 15 | 响应含请求 ID，错误页不泄露调试信息 | 日志配置、中间件、错误模板 |
| 16 | 回归测试可重复通过 | `tests.py` |
| 17–18 | 新环境可重建，日期改修有规格和测试证据 | README、配置、测试、交付记录 |

## 截图检查点

截图用于证明结果，不用于代替操作。建议保存以下画面，并在文件名中写章节和场景：

1. `ch05_static_list.png`：列表、三条数据和样式同时可见。
2. `ch07_admin_employee.png`：Admin 员工列表列、搜索和筛选可见；不要包含真实个人信息。
3. `ch11_search_empty.png`：搜索条件、空结果提示和地址栏查询参数可见。
4. `ch13_permission_403.png`：已登录但无权限用户看到403页面。
5. `ch14_upload_error.png`：非 PDF 或超大文件的服务端错误信息。
6. `ch18_date_filter.png`：From/To、筛选结果和保留条件的分页链接可见。

提交截图前遮盖账号、路径、Cookie、密钥和真实个人信息。错误调查优先保存文本日志、状态码和复现步骤，截图只是辅助证据。
