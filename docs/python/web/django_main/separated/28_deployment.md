# 第28章 REST API 部署设计与发布手顺

## 本章成果

能读懂生产请求链，完成发布前检查，并编写包含健康验证、监控和回滚条件的发布手顺。没有真实云资源、域名和生产账号时，应交付经过检查的仓库与可执行手顺，不得把本地运行写成“已经部署到生产”。

## 本章开始状态与交付物

- 第27章已经完成环境配置入口、请求 ID、日志和 JSON 异常响应。
- 本章不新增业务接口，输出依赖清单、环境变量清单、启动说明、迁移计划、健康检查、发布/回滚手顺和未验证条件。
- 真实云资源、域名、证书和生产数据库必须在目标项目中另行验证。

## 本章在整体架构中的位置

```text
Client → TLS / Reverse Proxy → Application Server → Django REST API
                                             ├→ Database
                                             └→ Private Storage
       Deploy → Migration → Health Check → Monitoring → Rollback
```

完成后，代码、配置、数据库迁移、前端产物和运行基础设施将形成可检查的发布链，而不只是“本地能够启动”。

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| 反向代理 | 在公网与 Django 应用服务器之间转发请求的组件 | 集中处理 TLS、域名、限制和转发 | 生产环境对外提供 API 时 |
| 应用服务器 | 通过 WSGI 或 ASGI 运行 Django 的进程 | 提供适合生产的进程与并发管理 | 部署 Django 服务时 |
| 健康检查 | 判断实例存活、就绪或关键业务可用的检查 | 支持流量切换、监控和故障发现 | 发布前后及系统持续运行期间 |
| 回滚 | 达到停止条件后恢复兼容状态的处置 | 控制发布故障影响范围 | 新版本无法在时间窗内安全修复时 |

## 1. 生产请求链

```text
用户浏览器
├─ /          → CDN/Web服务器 → 前端构建产物
└─ /api/      → 反向代理/TLS → WSGI或ASGI应用服务器 → Django
                                      ├─ 数据库
                                      └─ 私有文件存储
```

`runserver` 只用于开发。生产由 Gunicorn 等 WSGI 服务器或支持 ASGI 的服务器运行 Django，反向代理处理 TLS、域名、请求限制和转发。具体产品与命令由项目基础设施规范决定。

同域部署 `/api/` 可减少 CORS 配置，但仍要实施认证、权限和 CSRF/令牌安全。跨域部署则必须配置准确 origin。架构选择不能靠 CORS 配置代替安全评审。

## 2. 后端发布前准备

交付物包括源代码/构建产物、锁定依赖、迁移、环境变量清单、启动方式、健康检查、测试证据和回滚说明。

先把项目根目录的 `requirements.txt` 整理为当前完整直接依赖：

```text
Django==5.2.17
djangorestframework==3.17.2
djangorestframework-simplejwt==5.5.1
django-filter==26.1
drf-spectacular==0.30.0
django-cors-headers==4.9.0
waitress==3.0.2
```

`waitress` 是可在 Windows 和 Unix 运行的 WSGI 应用服务器，本章用它完成本地生产式启动验证。实际项目如果指定 Gunicorn、uWSGI 或 ASGI 服务器，应按目标运行环境替换并重新验证。新建虚拟环境时执行：

```powershell
python -m pip install -r requirements.txt
```

在 `company_portal/settings.py` 的 Static 配置处确认：

```python
STATIC_URL = "/static/"
STATIC_ROOT = BASE_DIR / "staticfiles"
```

`STATIC_ROOT` 是 `collectstatic` 的输出目录，不是源文件目录；它已经在 `.gitignore` 中排除。生产 Web 服务器或对象存储负责提供收集后的 Static，Waitress 不应承担公开静态文件服务。

在当前 PowerShell 进程设置一组仅供本地发布检查的环境变量：

```powershell
$env:DJANGO_DEBUG = "False"
$env:DJANGO_SECRET_KEY = python -c "import secrets; print(secrets.token_urlsafe(50))"
$env:DJANGO_ALLOWED_HOSTS = "127.0.0.1,localhost"
```

这些变量只保存在当前进程及其子进程中，关闭 PowerShell 后失效。真实生产秘密应由部署平台的密钥管理能力提供，不能写入脚本、仓库或镜像。

`secrets` 是 Python 标准库中用于生成密码学安全随机值的模块。`secrets.token_urlsafe(nbytes=None)` 返回适合放入环境变量和 URL 的随机字符串；`nbytes` 接受正整数或 `None`，表示随机字节数，省略时使用 Python 当前默认值。本例传入 `50` 生成50个随机字节，再编码为长度约67个字符的字符串，因此参数不是最终字符数。该命令只为本地发布检查临时生成密钥，生产环境应由密钥管理系统生成、保存和轮换。

在待发布版本的项目根目录、已激活目标环境虚拟环境且连接确认属于本地、测试或预发布资源后执行：

```powershell
python manage.py check
python manage.py check --deploy
python manage.py makemigrations --check
python manage.py test
python manage.py spectacular --file schema.yml --validate
python manage.py collectstatic --noinput
```

`check --deploy` 提示需要结合真实 TLS、代理和 Cookie 配置处理，不是自动修复工具。

这些管理命令均使用当前 settings：`check` 检查配置，`check --deploy` 额外执行生产安全检查，`makemigrations --check` 在模型尚有未生成迁移时以非零状态结束，`test` 运行测试，`spectacular --file ... --validate` 生成并验证 OpenAPI，`collectstatic --noinput` 把 Static 收集到部署目录并会改变该目录内容。每一步都应记录退出状态和输出，不能只记录“已执行”。

REST API 本身不要求更换数据库。本地开发继续使用项目创建时的 SQLite 配置；如果目标环境选择 PostgreSQL、MySQL 或托管数据库，发布准备还必须包含对应驱动、环境化连接参数、最小权限账号、连接与超时设置、迁移演练、备份恢复验证。此时改变的是运行环境中的 `DATABASES` 配置，业务代码仍通过 Django Model 和 ORM 访问数据库，前端仍只调用 HTTP API。

## 3. 使用应用服务器启动验证

完成检查后，在同一个 PowerShell 中启动 Waitress：

```powershell
waitress-serve --listen=127.0.0.1:8000 company_portal.wsgi:application
```

`--listen=主机:端口` 指定监听地址，`company_portal.wsgi:application` 指向 Django 创建的 WSGI 应用对象。这里绑定环回地址，只允许本机访问；真实环境通常由受控反向代理连接应用服务器。

在另一个 PowerShell 中验证：

```powershell
curl.exe -i http://127.0.0.1:8000/api/health/
```

预期状态为200、正文包含 `"status": "ok"`，响应头包含 `X-Request-ID`。再执行一个代表性 JWT 登录和员工列表请求，确认数据库、认证和权限链均可用。验证完成后在 Waitress 窗口按 `Ctrl+C` 停止；`staticfiles/` 是可重新生成的构建产物，不应提交。

这个验证覆盖应用构建、配置、WSGI 启动、Static 收集和代表 API，但不等于已经完成公网生产部署。目标环境仍需单独验证服务管理、反向代理、TLS、域名、网络、数据库、私有存储和监控。

## 4. 数据库迁移顺序

迁移是发布风险最高的步骤之一。确认：

- 是否向后兼容旧代码，是否需要“先加字段、回填、切代码、再收紧”的多阶段发布。
- 表规模、锁时间、索引创建和停机影响。
- 备份、恢复验证和不可逆操作。
- 多实例部署时只有受控步骤执行迁移。

一般流程是备份/确认 → 执行兼容迁移 → 部署应用 → 健康/业务验证 → 监控。不能把 `migrate` 随意放到每个进程启动时并并发执行。

## 5. Static、前端产物与 Media

DRF 可浏览 API 和 Admin 仍可能需要 Django Static；前端应用有自己的构建产物。两者的构建来源、缓存策略和版本需要区分。

私有员工附件不能由公开静态目录直接提供。生产常使用私有对象存储和短期受控下载，或由应用鉴权后流式返回。备份数据库不等于备份附件，恢复演练必须覆盖两者一致性。

## 6. 代理与安全配置

- `DEBUG=False`，`ALLOWED_HOSTS` 为实际主机。
- TLS 在代理终止时，Django 正确识别安全协议和原始 Host。
- 限制请求体大小与超时，和5 MB业务校验相互配合。
- 安全头、Cookie/CSRF策略、CORS来源和 JWT 时长符合环境。
- 只信任受控代理写入的转发头，防止客户端伪造。
- 日志包含请求 ID，监控覆盖错误率、延迟、资源和业务健康。

具体配置取决于操作系统、容器、云和网络环境。先确认各组件职责和发布手顺，不复制未经项目确认的生产配置。

## 7. 健康检查

至少区分：

- liveness：进程是否能响应，用于发现卡死。
- readiness：是否能接流量，可检查必要依赖。
- business smoke test：登录、员工列表等关键业务是否可用。

健康接口不能泄露版本、数据库地址和秘密，也不应因非关键外部服务短暂异常导致所有实例不断重启。

## 8. 发布与回滚手顺模板

1. 记录版本、负责人、时间窗、影响和变更冻结条件。
2. 确认备份、依赖、配置、迁移和前后端兼容。
3. 部署到预发布，执行自动测试和业务验收。
4. 生产发布，执行迁移与应用/前端切换。
5. 检查健康、API契约、权限、日志、错误率和延迟。
6. 达到回滚条件时停止推进，按手顺恢复应用或前向修复；数据库/数据不能盲目回滚。
7. 记录结果、异常、处置和遗留事项。

蓝绿、滚动等策略只在应用和数据库变更保持兼容时安全。回滚前端却不回滚 API，或回滚 API 却保留破坏性迁移，都可能扩大故障。

## 日本企业项目中的实际使用

日本项目的发布手顺通常要求负责人、时间窗、执行命令、确认结果、停止条件和回滚判断都可追踪。部署完成不等于验收完成，还要观察代表 API、权限、错误率、延迟和日志。

## 新人常见错误

- 使用 `runserver` 作为生产应用服务器。
- 发布应用前未确认迁移兼容性、备份和恢复方法。
- 把 liveness 通过当成登录和业务 API 已正常。
- 前端、API 和数据库变更没有明确发布顺序。
- 回滚应用版本，却忽略不可逆的数据变化。

## 企业项目调查路径

```text
Client → DNS / TLS → Reverse Proxy → Application Server → Django
→ Database / Storage → Health / Metrics / Log → 发布差异与配置
```

先判断请求未到达、应用不可用还是业务失败，再按层次调查。发布后异常必须对照变更时间、版本、迁移和配置，达到停止条件时及时中止推进。

## 9. 故障演练

模拟新环境漏配 CORS origin：前端 OPTIONS 失败，但后端健康检查正常。要求用 Network、代理/Django 日志和环境配置定位；修复后验证允许来源成功、未允许来源仍被拒绝，并确认没有开启全来源。

再模拟迁移未执行导致 API 500。正确处理是停止流量/发布、确认迁移状态和手顺，不是在生产手工改表。

## 完成检查

- [ ] 能说明前端、代理、应用服务器、Django、数据库和存储职责。
- [ ] 发布包含迁移、Static、健康、监控与回滚条件。
- [ ] 私有 Media、秘密和真实数据不进入公开产物。
- [ ] 进程存活不被误认为业务验收完成。
- [ ] 明确记录了已在本地/测试执行的项目和仍需目标环境验证的项目。

下一章以通过发布前检查的既有仓库为基线，完成一次 REST API 的 SES 改修与交接。
