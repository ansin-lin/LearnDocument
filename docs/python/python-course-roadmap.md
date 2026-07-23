# Python 课程总路线文件

> 本文件作为 Python 课程整理与重构时的指导文件使用，不直接作为学员主讲义章节。

## 一、当前课程总结构

当前 Python 课程分为四部分：

1. `common`：Python 通用基础
2. `web`：Python Web 路线
3. `data_analysis`：数据分析路线
4. `automation`：自动化路线

当前 Web 路线采用：

- Django 作为主线
- FastAPI 作为扩展专题
- Flask 作为扩展专题

这份路线文件以后续整理时的实际课程结构为准。

---

## 二、Python 通用基础主线

当前 `docs/python/common/` 对应以下主线：

| 章节 | 文件 | 核心内容 |
| ---: | --- | --- |
| 0 | `00_intro.md` | Python 简介、编辑器、终端、环境搭建、虚拟环境、`pip`、运行程序 |
| 1 | `01_variables_types_operators.md` | 变量、常量、数据类型、类型转换、输入输出、运算符、注释、命名规范 |
| 2 | `02_control_flow.md` | `if`、`match-case`、`for`、`while`、`break`、`continue`、循环 `else` |
| 3 | `03_collections.md` | `list`、`tuple`、`dict`、`set`、遍历、推导式、可变与不可变、拷贝 |
| 4 | `04_strings.md` | 索引、切片、查找、替换、分割、拼接、格式化、Unicode、编码与解码 |
| 5 | `05_functions.md` | 函数、参数、返回值、作用域、默认参数、关键字参数、`*args`、`**kwargs`、类型提示、`lambda` |
| 6 | `06_iterators_generators.md` | 可迭代对象、迭代器、`iter()`、`next()`、`yield`、生成器表达式 |
| 7 | `07_exceptions.md` | Traceback、异常处理、`raise`、自定义异常、`with`、调试 |
| 8 | `08_oop.md` | 类、对象、属性、方法、封装、继承、多态、组合、魔术方法 |
| 9 | `09_modules_stdlib.md` | 模块、包、`import`、`pathlib`、`datetime`、`re`、`logging`、`typing` |
| 10 | `10_file_data_processing.md` | 文本、二进制、CSV、JSON、Excel、大文件、编码问题 |
| 11 | `11_pymysql_database_access.md` | PyMySQL、连接、游标、SQL 执行、事务、异常处理 |

### 当前边界说明

- `common` 负责 Python 语言、标准库、文件处理和基础数据库驱动认知
- `HTTP` 基础不放在 `common`，而放在公共 Web 基础中
- `Git` 已独立为全站课程，不并入 `common`
- `测试基础` 目前主要在对应方向课程中落地，不单独占用 `common` 章节编号

---

## 三、Python Web 路线

## 1. Web 公共概览

| 章节 | 文件 | 核心内容 |
| ---: | --- | --- |
| 1 | `web/overview/01_web_overview.md` | Python Web 职责、框架定位与路线选择 |

框架相关环境和项目结构放入对应框架路线，不在公共概览中提前固定实现方式。

## 2. Django 主线

Django 是 Python Web 的零基础主线，分为两个连续阶段：

1. `web/django_main/server_rendered/`：先完成服务端渲染业务系统
2. `web/django_main/separated/`：再学习 REST、DRF 和前后端分离

### 2.1 服务端渲染正式路线

服务端渲染阶段使用同一个业务项目持续推进。每章必须留下可以继续用于下一章的稳定状态。

| 章节 | 建议文件 | 章节成果 | 主要知识 |
| ---: | --- | --- | --- |
| 1 | `01_django_request_response.md` | 看懂浏览器到 Django 的完整路径 | Django 定位、请求、路由、视图、响应 |
| 2 | `02_project_and_app.md` | 项目成功启动 | 环境、依赖、项目、App、配置、开发服务器 |
| 3 | `03_first_url_view.md` | 浏览器显示第一个响应 | 第一个 URL、函数视图、HttpRequest、HttpResponse |
| 4 | `04_app_routing_named_urls.md` | 建立员工列表和详情入口 | App 路由、include、路径参数、命名路由、反向解析 |
| 5 | `05_templates_static_list.md` | 显示带 CSS 的静态员工列表 | Template、变量、判断、循环、继承、Static |
| 6 | `06_models_migrations.md` | 创建部门表和员工表 | Model、字段、外键、迁移、数据库状态 |
| 7 | `07_django_admin.md` | 使用后台录入初始数据 | Admin、ModelAdmin、搜索、筛选、数据维护 |
| 8 | `08_orm_list_detail.md` | 显示数据库中的员工列表和详情 | QuerySet、关联查询、get_object_or_404、查询结果 |
| 9 | `09_form_create_validation.md` | 完成员工新增 | Form、ModelForm、POST、CSRF、输入校验 |
| 10 | `10_edit_delete_messages.md` | 完成员工编辑、删除和结果提示 | instance、删除确认、逻辑删除、messages |
| 11 | `11_search_pagination.md` | 完成企业常见列表页面 | GET 查询、Q、排序、分页、空数据 |
| 12 | `12_login_session.md` | 用户可以登录和退出 | 认证、Session、当前用户、登录跳转 |
| 13 | `13_groups_permissions.md` | 不同角色只能操作允许的功能 | Group、Permission、后端权限、模板反馈 |
| 14 | `14_file_upload_secure_download.md` | 安全上传和下载员工附件 | Media、FileField、类型/大小、文件名、下载权限 |
| 15 | `15_logging_errors_middleware.md` | 能调查错误并理解共通处理 | logging、404/500、请求流程、简单中间件 |
| 16 | `16_django_tests.md` | 为 CRUD 和权限编写基础测试 | TestCase、Client、表单、权限、回归测试 |
| 17 | `17_project_delivery.md` | 项目可以重新搭建、验收和交接 | 依赖、配置、README、启动、验收、已知限制 |
| 18 | `18_ses_change_delivery.md` | 完成一次日本 SES 改修流程 | 规格阅读、影响调查、改修、自测、Git、Review、交接 |

### 2.2 现有服务端渲染内容迁移

迁移时保留现有正确内容，按下面映射拆分或合并；不要先创建只有标题的新文件。

| 现有文件 | 新归属 | 处理方式 |
| --- | --- | --- |
| `_archive/django_server_rendered_v1/01_django_overview.md` | 新 01 | 已迁移：保留 Django 定位和请求响应全景；框架横向比较不进入当前主线 |
| `_archive/django_server_rendered_v1/02_django_project_setup.md` | 新 02 | 已迁移：保留项目、App、目录、配置和启动，补充版本基线、依赖重建与完成检查 |
| `_archive/django_server_rendered_v1/03_django_routing.md` | 新 03、04 | 已迁移：最小 URL 放入新 03；App 路由、参数、命名和反向解析放入新 04 |
| `_archive/django_server_rendered_v1/04_django_views.md` | 新 01、03、09、10 | 请求响应基础已进入新 01/03；GET/POST 留到表单与 CRUD；JSON 内容移到分离式阶段 |
| `_archive/django_server_rendered_v1/05_django_templates.md` | 新 05 | 已迁移：保留变量、循环、继承和命名路由，接入静态员工列表与 CSS |
| `_archive/django_server_rendered_v1/06_static_and_media.md` | 新 05、14 | 已迁移：Static 合并到模板阶段；Media 移到上传下载阶段 |
| `_archive/django_server_rendered_v1/07_django_model_orm.md` | 新 06、08、11 | 已迁移：Model、列表详情与搜索分页按成果拆开 |
| `_archive/django_server_rendered_v1/08_django_migrations.md` | 新 06 | 已迁移：与 Model 合并形成建表闭环 |
| `_archive/django_server_rendered_v1/09_forms_and_validation.md` | 新 09、10、11 | 已迁移：新增、编辑删除和查询职责分开 |
| `_archive/django_server_rendered_v1/10_authentication.md` | 新 12 | 已迁移：登录、退出、Session 和返回原页面 |
| `_archive/django_server_rendered_v1/11_authorization.md` | 新 13 | 已迁移：用户组、权限和后端强制检查 |
| `_archive/django_server_rendered_v1/12_middleware.md` | 新 15 | 已迁移并后置到日志与异常之后 |
| `_archive/django_server_rendered_v1/13_admin.md` | 新 07 | 已迁移并前移到 Model、迁移之后 |
| `_archive/django_server_rendered_v1/14_file_upload_download.md` | 新 14 | 已迁移并强化访问权限、文件名和下载安全 |
| `_archive/django_server_rendered_v1/15_server_rendered_project.md` | 新 08–17 | 已拆入渐进项目；交付内容集中到新 17 |
| 当前没有独立章节 | 新 16、18 | 已新增基础测试和 SES 改修实战 |

### 2.3 前后端分离阶段编号影响

服务端渲染扩展为 18 章后，分离式阶段已顺延为第 19–29 章，并同步文件名、章标题和 `mkdocs.yml`。

### 2.4 迁移执行边界

- 新 01–18 已形成正式课程入口，旧版内容只保留在 `_archive/` 供历史核对。
- 后续内容修改继续按“可运行成果 → 新知识 → 验证 → 现场任务”的结构迭代。
- 后续可建立学员入口 `server_rendered/index.md`，只展示学习前提、阶段成果和学习路线，不展示旧文件迁移说明。

## 3. FastAPI 扩展专题

FastAPI 路线由三组连续内容组成：

1. `web/overview/02_web_development_environment.md` 与 `web/fastapi_topics/01`–`03`：环境、请求响应和数据校验。
2. `web/database_tools/`、`web/database_project/` 与 `web/fastapi_topics/04`–`05`：SQLAlchemy、Alembic、数据库项目和 FastAPI 集成。
3. `web/fastapi_topics/06`–`16`：依赖注入、项目结构、认证、测试、部署和验收。

数据库工具不再作为与框架并列的独立路线。当前项目用它为 FastAPI 的数据库会话、Repository、Service 和事务边界建立前置知识；Flask 在自己的路线中使用 Flask-SQLAlchemy 和 Flask-Migrate。

| 阶段 | 主要文件 | 核心内容 |
| --- | --- | --- |
| FastAPI 基础 | `overview/02_web_development_environment.md`、`fastapi_topics/01`–`03` | 环境、第一个接口、请求响应、Pydantic 校验 |
| 数据库基础 | `database_tools/01`–`03` | SQLAlchemy 2.x、CRUD、事务、Alembic |
| 数据库项目 | `database_project/01`–`03` | 模型、项目 CRUD、Repository、Service |
| 框架集成 | `fastapi_topics/04`–`05` | FastAPI 数据库会话、CRUD 与事务集成 |
| 工程化与交付 | `fastapi_topics/06`–`16` | 依赖注入、分层、认证、测试、安全、部署和验收 |

## 4. Flask 扩展专题

当前 Flask 扩展目录：

```text
docs/python/web/flask_topics/
```

建议专题顺序：

| 章节 | 文件 | 核心内容 |
| ---: | --- | --- |
| 1 | `01_flask_intro.md` | Flask 定位、特点、基础项目 |
| 2 | `02_routing_request_response.md` | 路由、请求、响应 |
| 3 | `03_blueprint_layering.md` | Blueprint、基础分层 |
| 4 | `04_database_migration.md` | 数据库访问与迁移 |
| 5 | `05_validation_auth_testing.md` | 校验、认证、测试概览 |
| 6 | `07_flask_project_completion.md` | Flask 项目验收与交付 |

## 5. 框架综合对比

| 文件 | 作用 |
| --- | --- |
| `web/appendices/appendix_a_framework_comparison.md` | 综合比较 Django、FastAPI 与 Flask |

---

## 四、当前内容归属原则

### 放在 `common`

- Python 语法基础
- 容器、字符串、函数、异常、面向对象
- 模块与标准库
- 文件与数据处理
- 基础数据库驱动 `PyMySQL`

### 放在 `web`

- Web 环境准备
- Django 主线
- FastAPI / Flask 扩展
- API、认证、权限、日志、异常、测试、部署
- 文件上传下载、外部服务、联调

### 不放回 `common`

- HTTP 基础：放在 `docs/web_development_basics/`
- Git：放在 `docs/tools/git/`
- 框架认证授权与部署：放在 `web`

---

## 五、当前整理原则

1. Django 是 Python Web 主线
2. FastAPI 与 Flask 不再承担主线入口职责
3. `common` 目录编号保持稳定，不为迁就 Web 主线反复改动
4. Web 路线按“入口 → Django 主线 → 扩展专题 → 框架对比”组织
5. 以后整理 `mkdocs.yml` 时，只加入内容完备的文件
6. 以后整理 `web` 时，优先补 Django 主线，再整理扩展专题
