# FastAPI 员工管理 API 最终参考项目

完成各章练习后，可以下载 [FastAPI 员工管理 API 参考项目](../downloads/fastapi_employee_api.zip) 核对文件组合方式。

参考项目不是起步模板。建议先按照章节逐步完成请求响应、Schema、SQLAlchemy、Alembic、分层、认证和测试，再用它调查以下问题：

- Department、Employee和UserAccount是否符合项目规格。
- 数据库配置、Alembic迁移和种子数据能否从空环境重建。
- OAuth2 表单登录能否通过 `/docs` 使用。
- 员工列表、详情、新增、修改和逻辑删除是否形成闭环。
- `/api/departments` 是否继续返回数据库中的部门列表。
- 无认证返回 `401`，无权限返回 `403`。
- 测试数据库是否与开发数据库隔离。
- Alembic 是否能在空数据库执行升级、回退和再次升级。

项目说明、运行命令、测试方法和 Docker 边界包含在压缩包的 `README.md` 中。

参考项目默认使用独立SQLite文件，便于在本地完成组合验证；数据库章节使用MySQL练习连接和环境配置。切换数据库时必须使用专用数据库和账号，并在目标数据库重新执行Alembic迁移。
