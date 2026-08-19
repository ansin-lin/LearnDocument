# 第27章 FastAPI项目验收

> 本章成果：在空环境中重建员工管理API，执行迁移、种子数据、认证、CRUD、权限、测试和交付检查，并整理可复查的交付证据。

## 一、验收基线

最终项目必须保持：

| 对象 | 字段或规则 |
| --- | --- |
| Department | `id`、`name`，名称唯一 |
| Employee | `id`、`employee_number`、`name`、`department_id`、`email`、`joined_on`、`is_active` |
| 删除 | 逻辑删除，记录保留 |
| 列表 | 默认只返回在职员工，支持关键字和分页 |
| 账号 | 密码哈希保存，账号可禁用 |
| 权限 | 查询需登录，写操作需人事或管理员，删除仅管理员 |

完整业务定义以[员工管理API项目规格](../project_spec.md)为准。验收时同时检查接口结果、数据库状态、权限行为和自动测试。

## 二、最终目录

```text
employee_api/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── config.py
│   ├── database.py
│   ├── dependencies.py
│   ├── models.py
│   ├── schemas.py
│   ├── security.py
│   ├── lifespan.py
│   ├── logging_config.py
│   ├── seed.py
│   ├── repositories/
│   │   ├── __init__.py
│   │   └── employee_repository.py
│   ├── services/
│   │   ├── __init__.py
│   │   ├── auth_service.py
│   │   └── employee_service.py
│   └── routers/
│       ├── __init__.py
│       ├── auth.py
│       ├── departments.py
│       └── employees.py
├── alembic/
│   └── versions/
├── tests/
├── .dockerignore
├── .env.example
├── .gitignore
├── alembic.ini
├── Dockerfile
├── requirements-dev.txt
├── requirements-dev.lock.txt
├── requirements.lock.txt
├── requirements.txt
└── README.md
```

该目录延续第15章确定的分包结构。最终组合不能退回同名扁平文件，也不能出现重复Model、重复Router或不清楚的事务边界。

## 三、接口范围

| 方法 | 路径 | 主要验收 |
| --- | --- | --- |
| `GET` | `/health` | 返回 `{"status":"ok"}` |
| `POST` | `/api/auth/token` | OAuth2 表单登录并返回 Bearer Token |
| `GET` | `/api/departments` | 返回数据库中的部门列表 |
| `GET` | `/api/employees` | 在职员工分页列表 |
| `GET` | `/api/employees/{employee_number}` | 员工详情或 `404` |
| `POST` | `/api/employees` | 新增成功 `201`，重复编号 `409` |
| `PUT` | `/api/employees/{employee_number}` | 修改可编辑字段 |
| `DELETE` | `/api/employees/{employee_number}` | 管理员逻辑删除，成功 `204` |

文件、Excel、外部API、邮件、批处理、Redis、S3和AI不属于基础接口验收范围。

## 四、从空环境重建

在新目录或可丢弃验证环境中：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements-dev.lock.txt
Copy-Item .env.example .env
alembic upgrade head
alembic current
python -m app.seed
uvicorn app.main:app --reload
```

执行前修改 `.env`，确认使用 FastAPI 独立数据库和本地测试秘密。不要在来源不明或生产数据库上重建项目。

## 五、业务验收顺序

```text
访问 /health
→ 未登录访问员工列表，确认 401
→ 错误密码登录，确认 401
→ 管理员登录并取得 Token
→ 查询样例员工 E001
→ 新增 E010
→ 重复新增 E010，确认 409 且件数不变
→ 修改 E010
→ 普通用户删除 E010，确认 403 且数据不变
→ 管理员删除 E010，确认 204
→ 数据库确认 E010 仍存在且 is_active=False
→ 默认列表确认不再包含 E010
```

每一步保存请求、状态码、关键响应和数据库验证证据。

## 六、测试验收

```powershell
pytest -q
pytest -q
```

两次结果必须一致。至少覆盖：

- 健康检查
- 登录成功和失败
- 未认证与无权限
- 员工新增、详情、更新
- 重复编号
- 不存在员工和部门
- 分页边界
- 逻辑删除
- 数据库状态断言

测试必须覆盖 `get_db` 并使用隔离数据库，不能污染开发数据库。

## 七、迁移验收

在可丢弃测试数据库验证：

```powershell
alembic upgrade head
alembic current
alembic downgrade -1
alembic upgrade head
```

检查：

- `departments`、`employees`、`user_accounts` 可以从空数据库创建。
- 唯一约束、外键和索引存在。
- 自动生成脚本已经人工阅读。
- 回退是否会删除数据，以及生产环境能否接受。

不能把“命令执行成功”当成生产迁移安全证明。

## 八、安全与配置验收

- [ ] 仓库没有 `.env`、真实密码、Token 或密钥。
- [ ] JWT 密钥使用足够随机的 ASCII 字符串且长度至少为 32。
- [ ] 密码使用 Argon2 哈希。
- [ ] 错误响应不包含堆栈、SQL 或数据库地址。
- [ ] 日志不记录密码、完整 Token 或连接秘密。
- [ ] CORS 只允许明确的前端来源。
- [ ] 文件扩展任务限制大小、类型、名称和下载范围。
- [ ] 生产入口使用 HTTPS。

## 九、容器和交付验收

```powershell
docker build -t employee-api:local .
docker run --rm --env-file .env employee-api:local alembic current
docker run --name employee-api-local --env-file .env -p 8000:8000 employee-api:local
curl.exe -i http://127.0.0.1:8000/health
docker logs employee-api-local
docker stop employee-api-local
docker rm employee-api-local
```

还要确认镜像以非 root 用户运行、没有包含秘密，数据库迁移是受控发布步骤，回滚和恢复边界已经记录。

## 十、请求链说明

最终报告选择“新增员工并逻辑删除”场景，说明：

| 关注点 | 应说明的内容 |
| --- | --- |
| 请求与校验 | Router怎样取得路径、查询和请求体数据 |
| 输入输出 | Pydantic Schema怎样约束请求与响应 |
| 数据访问 | Repository怎样使用Session执行查询和更新 |
| 事务 | Service在成功和失败时怎样commit或rollback |
| 认证授权 | 当前用户和角色依赖怎样产生401或403 |
| 错误响应 | 业务异常怎样转换为稳定HTTP响应 |
| 测试 | pytest怎样覆盖接口结果和数据库状态 |

报告必须结合实际请求、日志、SQL或测试证据，不能只复制类名和函数名。

## 十一、交付材料

| 材料 | 最少内容 |
| --- | --- |
| README | 环境、依赖、迁移、种子、启动、测试、停止 |
| `.env.example` | 配置名和安全说明，不含真实值 |
| API 一览 | 方法、路径、认证、主要状态码 |
| 数据库定义 | 表、字段、关系、约束和迁移版本 |
| 测试结果 | 命令、通过件数、失败处理 |
| 故障调查 | 日志入口、常见错误和恢复步骤 |
| 已知限制 | 当前未实现或未外部验证的内容 |

## 十二、参考项目

完成自己的实现后，使用 [FastAPI 最终参考项目](reference_project.md) 核对组合状态。参考项目用于调查差异，不替代逐章练习。

## 十三、完成标准

- [ ] 字段、样例数据、逻辑删除和验收场景符合项目规格。
- [ ] 项目使用自己的数据库配置和Alembic迁移历史。
- [ ] API、数据库、权限和测试形成闭环。
- [ ] 全新环境可以按 README 重建。
- [ ] 自动文档之外还有测试和人工验收证据。
