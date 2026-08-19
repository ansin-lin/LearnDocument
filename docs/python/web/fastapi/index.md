# FastAPI 员工管理 API 学习路线

本路线是一套独立的 FastAPI 后端课程。完成 Python 基础和 SQL 基础后，可以从第1章开始学习，不要求先学习其他 Python Web 框架。

课程围绕“员工管理 API”逐步完成一个可以迁移、认证、测试、联调和部署的后端项目。前3章先使用内存数据观察请求和响应；掌握 Router、Depends 和资源生命周期后，再接入 SQLAlchemy、MySQL 和 Alembic。

## 学习前提

- 能使用 Python 函数、类、模块、异常、类型提示和虚拟环境。
- 掌握表、主键、外键、增删改查和事务等 SQL 基础。
- 已学习 [Web 开发公共基础](../../../web_basics/00_frontend_backend_request_response.md)，能区分请求、响应、HTTP 方法和状态码。
- 不要求具备 FastAPI、其他Web框架或SQLAlchemy使用经验。

课程第3阶段直接讲解SQLAlchemy与Alembic，不需要先跳转到其他教程目录。

## 贯穿项目

课程使用 [员工管理 API 项目规格](project_spec.md)。项目包含部门、员工、账号和角色权限，主线保持业务名称、字段、接口和逻辑删除规则连续。

最终项目使用 SQLAlchemy 2.x 映射数据、Alembic 管理迁移、Pydantic 定义接口数据、Depends 管理请求依赖，并使用 pytest 验证主要业务和权限。

## 八个阶段

| 阶段 | 章节 | 主要问题 | 可检查成果 |
| --- | --- | --- | --- |
| 01 Web API基础 | 第1～3章 | 请求怎样进入路径函数并形成响应 | 内存版员工CRUD、请求校验和OpenAPI文档 |
| 02 FastAPI核心 | 第4～6章 | 怎样组织Router、复用公共依赖并安全释放资源 | 多文件Router、分页依赖、可验证的yield清理流程 |
| 03 SQLAlchemy | 第7～9章 | Engine、Session、ORM、事务和迁移怎样工作 | SQLAlchemy CRUD与Alembic迁移练习 |
| 04 数据库项目 | 第10～12章 | 怎样建立持久化模型和清晰的业务分层 | 模型、迁移、Repository、Service和事务边界 |
| 05 数据库API | 第13～14章 | 怎样把请求生命周期连接到数据库Session | 只读查询API、写入API、响应Schema与异常转换 |
| 06 工程化 | 第15～23章 | 怎样让项目可配置、可观察、可认证、可测试 | 目录、Config、Lifespan、Middleware、日志、JWT和pytest |
| 07 联调与交付 | 第24～27章 | 怎样完成浏览器联调、部署和交付验收 | CORS、安全检查、容器运行和完整验收证据 |
| 08 扩展 | 选修专题 | 怎样接入项目需要的外部能力 | 文件、外部API、Redis、S3或AI扩展 |

## 章节入口

### 01 Web API基础

1. [FastAPI入门](01_web_api_basics/01_intro.md)
2. [FastAPI请求与响应](01_web_api_basics/02_request_response.md)
3. [Pydantic、响应模型与OpenAPI](01_web_api_basics/03_pydantic_openapi.md)

这一阶段完成内存版员工CRUD。重启服务后数据会恢复，目的是先看清HTTP输入、校验、路径函数和响应之间的关系。

### 02 FastAPI核心

4. [APIRouter与多文件路由](02_fastapi_core/04_apirouter.md)
5. [Depends与请求级复用](02_fastapi_core/05_depends.md)
6. [yield依赖与资源清理](02_fastapi_core/06_yield_resource_lifecycle.md)

这一阶段不连接数据库。第5章先使用分页参数理解函数依赖，第6章再使用演示资源观察获取和释放过程。第13章会把相同机制用于数据库Session。

### 03 SQLAlchemy

7. [SQLAlchemy基础概念与常用对象](03_sqlalchemy/07_core_concepts.md)
8. [SQLAlchemy ORM CRUD与事务](03_sqlalchemy/08_orm_crud_transactions.md)
9. [Alembic迁移基础](03_sqlalchemy/09_alembic_migrations.md)

完成后应能区分Engine、Connection、Session和事务，并能独立执行一次迁移。

### 04 数据库项目

10. [SQLAlchemy员工模型与Alembic迁移](04_database_project/10_models_migrations.md)
11. [EmployeeRepository与数据库CRUD](04_database_project/11_crud_transactions.md)
12. [EmployeeService、事务边界与DTO](04_database_project/12_repository_service_dto.md)

### 05 FastAPI数据库API

13. [Session依赖与只读查询API](05_database_api/13_session_dependency_query.md)
14. [写入API与异常边界](05_database_api/14_crud_response_exception.md)

### 06 工程化

15. [FastAPI项目结构](06_engineering/15_project_structure.md)
16. [配置管理与Lifespan](06_engineering/16_config_lifespan.md)
17. [Middleware、日志与统一异常](06_engineering/17_middleware_logging_exceptions.md)
18. [用户账号与密码哈希](06_engineering/18_user_accounts_passwords.md)
19. [JWT签发与登录接口](06_engineering/19_jwt_login.md)
20. [当前用户与角色权限](06_engineering/20_current_user_permissions.md)
21. [pytest、TestClient与隔离数据库](06_engineering/21_testing_foundation.md)
22. [员工CRUD与事务测试](06_engineering/22_testing_crud.md)
23. [认证、权限与迁移测试](06_engineering/23_testing_auth_migrations.md)

### 07 联调与交付

24. [前后端联调与CORS](07_integration_delivery/24_frontend_cors.md)
25. [安全与性能基础](07_integration_delivery/25_security_performance.md)
26. [FastAPI运行与容器交付](07_integration_delivery/26_deployment.md)
27. [FastAPI项目验收](07_integration_delivery/27_project_completion.md)
- [最终参考项目说明](07_integration_delivery/reference_project.md)

### 08 扩展

- [文件、CSV与Excel](08_extensions/01_file_csv_excel.md)
- [外部API、邮件与批处理](08_extensions/02_external_api_email_batch.md)
- [Redis缓存](08_extensions/03_redis_cache.md)
- [S3对象存储](08_extensions/04_s3_storage.md)
- [AI员工资料摘要接口](08_extensions/05_ai_integration.md)

时间有限时先完成第1～27章。扩展专题按项目需要选学，不作为主线的隐藏前置要求。

## 最终成果

完成后应能独立交付：

- `GET /health`健康检查。
- `POST /api/auth/token`登录接口。
- 员工列表、详情、新增、修改和逻辑删除接口。
- 关键字查询和分页。
- 认证、角色授权、稳定错误响应和请求日志。
- Alembic迁移、pytest测试、配置示例、启动与验收说明。
- 可以本地运行和通过容器启动的项目。

最终验收不以“可以打开`/docs`”为完成标准。还需要证明迁移可执行、数据能够保存、未认证和无权限请求会被拒绝、逻辑删除不会物理移除记录，并且自动测试可以重复运行。
