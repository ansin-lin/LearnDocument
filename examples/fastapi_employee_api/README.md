# FastAPI 员工管理 API

这是一个可独立运行的员工管理API，包含SQLAlchemy模型、Alembic迁移、Pydantic Schema、分层CRUD、JWT认证、角色权限和接口测试。

## 1. 准备环境

在本目录执行：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements-dev.lock.txt
Copy-Item .env.example .env
```

修改 `.env`：

- 为 `SECRET_KEY` 设置随机的本地开发值。
- 为 `SEED_ADMIN_PASSWORD` 设置仅用于本地验证的测试密码。
- 默认使用当前目录的 SQLite；需要 MySQL 时替换 `DATABASE_URL`，并先准备独立数据库和最小权限账号。

不要提交 `.env`。

## 2. 创建表和样例数据

```powershell
alembic upgrade head
alembic current
python -m app.seed
```

再次执行 `python -m app.seed` 不应重复创建 `E001`、`E002`、部门或管理员账号。

## 3. 启动

```powershell
uvicorn app.main:app --reload
```

访问：

- 健康检查：<http://127.0.0.1:8000/health>
- OpenAPI：<http://127.0.0.1:8000/docs>

在 `/docs` 使用 `POST /api/auth/token` 登录。表单中的 `username` 为 `admin`，密码使用本地 `.env` 中的 `SEED_ADMIN_PASSWORD`。取得 Token 后点击 Authorize。

## 4. 接口范围

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| `GET` | `/health` | 健康检查 |
| `POST` | `/api/auth/token` | OAuth2 表单登录 |
| `GET` | `/api/departments` | 部门列表 |
| `GET` | `/api/employees` | 在职员工分页列表 |
| `GET` | `/api/employees/{employee_number}` | 员工详情 |
| `POST` | `/api/employees` | 新增员工 |
| `PUT` | `/api/employees/{employee_number}` | 修改员工 |
| `DELETE` | `/api/employees/{employee_number}` | 逻辑删除员工 |

员工写接口需要 `SYSTEM_ADMIN` 或 `HR_STAFF`；逻辑删除仅允许 `SYSTEM_ADMIN`。

## 5. 测试

```powershell
pytest -q
pytest -q
```

两次结果应一致。测试使用隔离的 SQLite 内存数据库，不读取或修改开发数据库。

当前回归覆盖健康检查、登录失败、禁用账号、未认证和无效Token、三个角色的权限边界、部门列表、员工新增与修改、无效部门、重复编号、分页边界、逻辑删除、离职编号不可复用以及错误响应请求编号。

## 6. Docker

构建：

```powershell
docker build -t employee-api:local .
```

如果使用 SQLite，容器内数据库会随容器删除而丢失；该方式只用于观察启动契约。需要保留数据时应配置外部数据库或经过设计的卷。

运行迁移和应用：

```powershell
docker run --rm --env-file .env employee-api:local alembic upgrade head
docker run --name employee-api-local --env-file .env -p 8000:8000 employee-api:local
```

停止和删除本地验证容器：

```powershell
docker stop employee-api-local
docker rm employee-api-local
```

## 7. 主要请求链

一次员工写请求依次经过：

```text
APIRouter
→ Pydantic请求校验
→ 当前用户与角色依赖
→ EmployeeService事务控制
→ EmployeeRepository数据库操作
→ Pydantic响应模型
```

`get_db()`为每次请求创建并关闭SQLAlchemy Session；Repository不自行提交事务，Service根据完整业务用例执行`commit()`或`rollback()`。逻辑删除只修改`is_active`，不会物理删除员工记录。
