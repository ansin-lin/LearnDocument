# Django 企业级 REST API 开发（DRF）

企业 REST API 不只负责返回 JSON，还要处理认证、授权、数据范围、测试、日志和部署。下面从虚拟环境和 Django 项目创建开始，以员工管理项目为例，逐步完成一套可联调、可测试、可部署的 Django REST API。

## 为什么学习 REST API

传统 Django 页面由后端组合 HTML；REST API 则以 HTTP 和 JSON 提供稳定的数据契约，使不同客户端能够复用同一套业务能力：

```text
传统方式：Browser → Django View → Template → HTML

现代方式：Vue / React / Flutter / Android / iOS
                         ↓ HTTP
                    Django REST API
                         ↓ JSON
                  Model / ORM → Database
```

前端框架和移动端不直接连接数据库。它们只调用经过认证、授权和数据范围控制的 API，因此后端可以统一保护业务规则和数据。

## 开始前准备

开始前应掌握 Python 函数、类、模块与异常，能够读懂 HTTP 请求、JSON，以及表、主键、外键和基本 SQL 查询。不需要先学习 Django Template、Form 或服务端渲染。

```text
Python → HTTP / JSON → SQL → 虚拟环境 → 创建 Django 项目 → Django REST API
```

- 首先完成[从零创建 Django 项目](setup.md)：创建虚拟环境、安装 Django、创建 Project 与 App、建立 Model、迁移数据库并写入第一组练习数据。
- HTTP、JSON 和 REST 不熟悉时，可先查看[Web 公共基础](../../../../web_basics/01_http_rest_cookie_cors.md)。
- 第24章包含联调所需的原生 JavaScript 页面，无须预先安装 Vue 或 React；该章会先说明示例直接使用的 ES Module、异步函数和 DOM 接口。

## 完成目标

- 理解 REST API、HTTP 方法、状态码和 JSON 契约。
- 使用 DRF 编写 Serializer、ViewSet、Router 和 CRUD API。
- 使用 JWT 完成登录认证，并实施操作权限和数据范围控制。
- 实现过滤、排序、分页、文件接口和统一异常响应。
- 生成 OpenAPI，编写 API 自动测试并完成前后端联调。
- 整理环境配置、日志、部署、回滚和交接证据。
- 在指导与 Review 下承担日本 SES 项目的小型 API 调查与改修。

## 整体架构

```text
Browser / Mobile App
        ↓
Vue / React / Fetch
        ↓ HTTP Request
URL / Router
        ↓
ViewSet / APIView
        ↓
Authentication → request.user → Permission → QuerySet 数据范围
        ↓
Filter → Ordering → Pagination
        ↓
Serializer
        ↓
Model → ORM → DATABASES → Database
        ↓
Response → JSON → Client
```

后续操作将逐层实现并验证这条调用链，再用 OpenAPI、测试、日志和发布手顺保证 API 可维护、可调查、可交付。

## 可选对照：传统 Django 与 DRF

| 服务端渲染中的职责 | REST API 中的对应职责 | 共同目标 |
|---|---|---|
| Template | JSON 响应 | 向客户端提供结果 |
| Form / ModelForm | Serializer | 输入校验与数据转换 |
| View | APIView / ViewSet | 组织请求处理 |
| URL pattern | Router / URL pattern | 把路径交给处理组件 |
| Session | Session 或 JWT | 识别当前用户 |
| `render()` / `HttpResponse` | `Response()` | 返回 HTTP 响应 |

右栏可以独立阅读；左栏用于把已有的 Django 页面开发经验映射到 DRF。

## 数据库连接原则

REST API 改变的是客户端与 Django 的通信方式，不会改变 Django 连接数据库的基本机制：

```text
Client → REST API → Model / ORM → DATABASES → Database
```

在[从零创建 Django 项目](setup.md)中会建立 `company_portal` 配置、`DATABASES`、Model、迁移文件和本地开发数据库。DRF 不需要另一套数据库连接，前端也不能直接连接数据库。

完成[从零创建 Django 项目](setup.md)后，在项目根目录激活虚拟环境，并确认 `company_portal/settings.py` 连接的是本地练习数据库，再执行以下检查：

```powershell
python manage.py check
python manage.py showmigrations employees
python manage.py migrate
python manage.py shell -c "from employees.models import Employee; print(Employee.objects.count())"
```

迁移已显示 `[X]` 时不需要重做。只有新迁移出现 `[ ]` 时才按项目手顺执行；不要对生产环境或来历不明的数据库直接执行。生产环境若改用 PostgreSQL、MySQL 或托管数据库，在第28章按部署要求处理驱动、连接参数、迁移、备份和回滚，业务代码仍通过 Model 与 ORM 访问数据库。

`manage.py check` 检查 Django 配置；`showmigrations employees` 显示 `employees` App 的迁移及应用状态；`migrate` 应用尚未执行的迁移并可能改变数据库结构；`shell -c "..."` 在 Django 环境中执行给定 Python 语句。`Employee.objects` 是员工模型的默认管理器，`count()` 在数据库中执行计数并返回整数，本例只读取员工件数。执行迁移前必须确认目标数据库和备份要求。

## 实现顺序

| 范围 | 章节 | 可交付成果 |
|---|---:|---|
| 项目准备 | 开始 | 虚拟环境、Django 项目、业务 Model、SQLite 数据库 |
| API 基础 | 19–21 | REST 契约、Serializer、ViewSet/Router CRUD |
| 权限与契约 | 22–23 | JWT、后端权限、数据范围、过滤分页与 OpenAPI |
| 联调与文件 | 24–25 | 可运行前端、Fetch/CORS、安全文件 API |
| 质量与配置 | 26–27 | API 自动测试、日志、异常响应与环境配置 |
| 部署与现场 | 28–29 | 发布/回滚手顺、一次完整 SES API 改修 |

## 操作建议

开始修改前先确认当前调用链和项目状态。使用 HTTP 客户端与浏览器 Network 观察请求，同时检查响应、数据库、权限和日志；保留失败现象、调查过程和修复证据。先完成基础 HTTP 与 JSON 验证，再进入 ViewSet，避免只会复制代码却无法判断错误所在层。

## 完成边界

完成全部改修后，应能追踪常见调用链，处理小型 CRUD、筛选和权限需求，并提交测试与交接证据。大型架构、复杂异步系统、云平台、安全专项和高并发优化需要结合实际系统继续验证。
