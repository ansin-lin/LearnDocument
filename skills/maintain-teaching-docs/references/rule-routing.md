# 教学规则分发表

维护教学文档时先按任务性质选择通用规则，再根据路径和正文内容组合所有匹配的专项规则。路径只是提示；章节实际涉及的语言、框架、数据库、前端、工具和部署内容优先。

## 通用规则

| 任务性质 | 加载规则 |
| --- | --- |
| 新增、修改、重构教学内容 | `.codex/rules/teaching.md` |
| 包含代码、命令、SQL、配置、输出或操作步骤 | `.codex/rules/examples.md` |
| 包含框架、第三方库、组件库、插件或关键依赖 | `.codex/rules/framework-library.md` |
| 检查、审核、评价或 Review | `.codex/rules/review.md` |

审核任务仍应加载正文、示例和框架实际涉及的其他通用规则，不能只读 `review.md`。

## 路径分发

| 目标路径或课程 | 专项规则 |
| --- | --- |
| `docs/web_development_basics/` | `web-basics.md` |
| `docs/python/common/` | `python.md` |
| `docs/python/data_analysis/` | `python.md`、`python-data-analysis.md` |
| `docs/python/automation/` | `python.md`、`python-automation.md` |
| `docs/python/web/overview/` | `python.md`、`web-basics.md` |
| Django 课程 | `python.md`、`web-basics.md`、`django.md` |
| FastAPI 课程 | `python.md`、`web-basics.md`、`fastapi.md` |
| Flask 课程 | `python.md`、`web-basics.md`、`flask.md` |
| SQLAlchemy、Alembic 或 Python 数据库项目 | `python.md`、`sql.md`、`sqlalchemy.md` |
| `docs/java/` | `java.md` |
| `docs/java/jdbc/` 或 JDBC 内容 | `java.md`、`sql.md`、`jdbc.md` |
| `docs/springboot/` | `java.md`、`web-basics.md`、`spring-boot.md` |
| `docs/mybatis/` | `java.md`、`mybatis.md`、`sql.md` |
| `docs/sql/` | `sql.md` |
| `docs/PLSQL/` | `sql.md`、`plsql.md` |
| HTML 内容 | `html.md` |
| CSS 内容 | `css.md` |
| JavaScript 内容 | `javascript.md` |
| TypeScript 内容 | `javascript.md`、`typescript.md` |
| Vue 内容 | `html.md`、`css.md`、`javascript.md`、`vue.md` |
| React 内容 | `html.md`、`css.md`、`javascript.md`、`react.md` |
| 前端测试内容 | `javascript.md`、`frontend-testing.md`，并加载所测框架规则 |
| `docs/ui/` 或通用 UI 内容 | `html.md`、`css.md`、`ui.md` |
| `docs/ui/bootstrap/` | `html.md`、`css.md`、`javascript.md`、`ui.md`、`bootstrap.md` |
| `docs/ui/vuetify/` | `html.md`、`css.md`、`javascript.md`、`vue.md`、`ui.md`、`vuetify.md` |
| `docs/git/` | `git.md` |
| Linux 内容 | `linux.md` |
| Shell 脚本内容 | `shell.md`，并按运行系统加载 `linux.md` 或其他相关规则 |
| `docs/aws/` | `aws.md`，并按任务加载 `linux.md`、`docker.md` 或 `deployment.md` |
| Docker 内容 | `docker.md` |
| 部署、发布、运行维护内容 | `deployment.md`，并加载应用、操作系统、云或容器规则 |
| `docs/training_project/` | 根据项目实际技术组合加载，不按目录预设单一技术 |
| `docs/all/`、`docs/frontend_library/` | 根据正文内容加载，不依赖目录名判断 |

## 内容关键词补充分发

- 出现 HTTP、REST、Cookie、Session、CORS、TLS、反向代理或请求响应链时，加载 `web-basics.md`。
- 出现数据库表、SQL、事务、索引或执行计划时，加载 `sql.md`；出现 Oracle PL/SQL 时再加载 `plsql.md`。
- 出现框架特有对象、注解、组件、插件或依赖时，加载对应框架规则和 `framework-library.md`。
- 出现容器构建或 Compose 时加载 `docker.md`；出现发布、反向代理、证书、监控或回滚时加载 `deployment.md`。
- 出现多种技术时加载全部匹配规则，不选择一个规则代替其他规则。

## 缺失与兼容处理

- 没有独立专项规则的小型库，加载所属语言或框架规则与 `framework-library.md`。
- 无法明确技术栈时，先检查课程入口、相邻章节、依赖文件和代码导入，再选择规则。
- 必需专项规则缺失时继续完成安全范围内的任务，并在结果中报告缺口；不要临时把专项政策写入 Skill。
- `java-backend-database.md`、`frontend.md` 和 `infrastructure-tools.md` 是已拆分兼容文件，不再分发。
- `BK/` 与 `_archive/` 仅在用户明确要求或需要历史对照时读取，不作为现行规则或课程主线依据。
