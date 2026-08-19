# 第26章 FastAPI运行与容器交付

> 本章成果：整理可重建依赖、运行时配置、Alembic 迁移、非 root Docker 镜像、健康检查、日志和回滚边界，形成员工 API 的交付说明。

## 一、本章边界

本章聚焦员工 API 的交付契约。Linux、Docker、反向代理、TLS、监控和发布体系还需要结合各工具的完整文档和实际平台要求。

部署不是“进程启动成功”：

```text
构建镜像
→ 提供运行时配置和秘密
→ 备份并执行迁移
→ 启动新版本
→ 健康检查
→ 业务冒烟测试
→ 观察日志和指标
→ 切换流量或回滚
```

## 二、整理依赖

生产依赖写入 `requirements.txt`：

```text
fastapi[standard]
sqlalchemy>=2,<3
pymysql
alembic
pydantic-settings
pyjwt
pwdlib[argon2]
python-multipart
```

测试和扩展依赖写入 `requirements-dev.txt`：

```text
-r requirements.txt
pytest
httpx
openpyxl
```

`requirements.txt` 和 `requirements-dev.txt` 记录项目直接依赖及允许范围。在只安装运行依赖的全新虚拟环境中生成生产锁定文件：

```powershell
python -m pip install -r requirements.txt
python -m pip freeze > requirements.lock.txt
```

在另一个全新虚拟环境中安装开发依赖并生成测试锁定文件：

```powershell
python -m pip install -r requirements-dev.txt
python -m pip freeze > requirements-dev.lock.txt
```

生产锁定文件不能包含 pytest 等仅用于测试的工具。使用开发锁定文件执行重建验证：

```powershell
python -m pip install -r requirements-dev.lock.txt
python -m pip check
pytest -q
```

只有这组具体版本通过测试后，才同时更新两个锁定文件。Docker 使用 `requirements.lock.txt`，开发和自动测试使用 `requirements-dev.lock.txt`。正文、依赖文件和导入必须一致；不要依赖某个库“碰巧被其他包间接安装”。

## 三、运行时配置

仓库提交 `.env.example`，只列配置名：

```text
DATABASE_URL=
SECRET_KEY=
```

真实 `.env` 不提交。容器构建时不能把数据库密码或 JWT 密钥写入镜像层。

环境责任：

| 环境 | 数据库和秘密 |
| --- | --- |
| 本地开发 | 开发专用数据库、本地 `.env` |
| 自动测试 | 隔离测试数据库、测试专用密钥 |
| 预发布 | 接近生产的独立资源和秘密 |
| 生产 | 最小权限账号、正式秘密管理和备份 |

## 四、启动契约

开发：

```powershell
uvicorn app.main:app --reload
```

服务器或容器：

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Uvicorn 启动参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `APP` | `模块路径:应用对象` 字符串 | 必填；本示例为 `app.main:app` | 指定需要加载的 ASGI 应用 |
| `--host` | 主机名或 IP 地址 | 默认 `127.0.0.1` | 设置监听地址；容器内使用 `0.0.0.0` 接收容器外请求 |
| `--port` | `0`～`65535` 的端口整数 | 默认 `8000` | 设置监听端口 |
| `--reload` | 开关参数，不接收值 | 默认关闭 | 文件变化时自动重启，只用于本地开发 |
| `--workers` | 正整数 | 默认 `1` | 设置工作进程数；不能与 `--reload` 同时使用 |

`--reload` 只用于开发。进程数量、超时和优雅停止应根据部署平台、同步数据库驱动和实际负载测试决定，不能机械增加 worker。

## 五、限制构建上下文

在项目根目录创建 `.dockerignore`：

```text
.env
.venv/
__pycache__/
*.py[cod]
.pytest_cache/
site/
.git/
```

Docker 构建会把当前目录作为构建上下文发送给构建器。`.dockerignore` 在发送前排除本地秘密、虚拟环境、缓存、站点输出和版本库元数据，既减少构建数据，也避免 `.env` 被后续宽范围 `COPY` 意外加入镜像。

## 六、Dockerfile

在项目根目录创建 `Dockerfile`：

```dockerfile
FROM python:3.12-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

COPY requirements.lock.txt .
RUN python -m pip install --no-cache-dir -r requirements.lock.txt

COPY alembic.ini .
COPY alembic ./alembic
COPY app ./app

RUN chown -R app:app /app
USER app

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=2)"]

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

要点：

- 依赖文件先复制，便于复用构建缓存。
- 应用以非 root 用户运行。
- 镜像包含 Alembic 脚本，但不在 `CMD` 中自动迁移。
- `EXPOSE` 只声明容器端口，不会自动把端口发布到主机。

## 七、构建和本地运行

在项目根目录执行：

```powershell
docker build -t employee-api:local .
docker run --name employee-api-local --env-file .env -p 8000:8000 employee-api:local
```

`docker run` 本示例选项：

| 选项 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `--name` | 符合 Docker 命名规则的容器名称 | 可省略，由 Docker 自动生成 | 给容器设置便于日志、停止和删除时引用的名称 |
| `--env-file` | 环境变量文件路径 | 可省略 | 把文件中的运行时配置传入容器 |
| `-p`、`--publish` | `[主机地址:]主机端口:容器端口[/协议]` | 可省略 | 把容器端口发布到主机 |
| `IMAGE` | 镜像名和可选标签 | 必填 | 指定用于创建容器的镜像 |
| `COMMAND` | 命令及其参数 | 可省略，使用镜像 `CMD` | 覆盖镜像默认启动命令 |

`-p 8000:8000` 把主机端口发布到容器。数据库地址中的主机名必须从容器网络可访问，容器内的 `127.0.0.1` 指向容器自身。

另一个终端验证：

```powershell
curl.exe -i http://127.0.0.1:8000/health
docker logs employee-api-local
```

停止并清理学习容器：

```powershell
docker stop employee-api-local
docker rm employee-api-local
```

删除容器不会删除外部 MySQL 数据库。若使用命名卷，必须另行确认卷的保留和删除范围。

## 八、迁移发布顺序

迁移应作为受控发布步骤执行，而不是每个应用进程启动时并发执行：

```powershell
docker run --rm --env-file .env employee-api:local alembic current
docker run --rm --env-file .env employee-api:local alembic upgrade head
```

执行前确认：

- 目标数据库和账号正确。
- 迁移脚本已经人工审查。
- 备份及恢复方法已验证。
- 新旧代码在发布窗口内是否兼容。
- 不可逆迁移是否有替代回退方案。

`alembic downgrade` 不等于安全回滚。已经丢失或转换的数据可能无法恢复。

## 九、健康和业务验证

`GET /health` 只证明应用能够处理最小请求。生产发布还要验证：

- 数据库迁移版本正确。
- 测试账号可以登录。
- 受保护员工列表返回 `200`。
- 未认证请求返回 `401`。
- 日志没有秘密和完整 Token。
- 错误率、延迟和数据库连接状态正常。

可以把数据库连通性放入单独的 readiness 检查，不要让每次基础存活检查都执行昂贵查询。

## 十、反向代理与 TLS

生产环境通常由反向代理、负载均衡或平台入口终止 TLS，再把请求转发给 Uvicorn。需要统一确认：

- 外部 HTTPS 与内部 HTTP 的边界
- 可信代理和转发 Header
- 域名、证书和安全 Header
- 请求体大小和上传超时
- CORS 允许来源

本地 `docker run` 不是生产部署方案。

## 十一、故障与回滚

练习一次可恢复故障：

1. 使用错误数据库地址启动容器。
2. 查看容器状态和日志，确认没有输出密码。
3. 停止错误容器。
4. 恢复正确配置并重新启动。
5. 验证健康、登录和员工列表。

应用回滚与数据库回滚必须分开判断。若新版本迁移与旧代码不兼容，应先设计兼容发布顺序，不能等故障发生后再猜测。

## 十二、完成检查

- [ ] 生产和开发依赖可重建。
- [ ] `.dockerignore` 排除了本地秘密、虚拟环境和缓存。
- [ ] 镜像不包含真实秘密，并以非 root 用户运行。
- [ ] 构建、启动、日志、健康、停止和清理命令完整。
- [ ] Alembic 迁移是独立受控步骤。
- [ ] 健康检查之外还有业务冒烟验证。
- [ ] 发布说明包含监控、回滚和数据库恢复边界。

完成后保存运行命令、迁移记录、健康检查和容器验证结果。
