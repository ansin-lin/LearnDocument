# 章节练习参考答案

先独立完成并记录验证结果，再展开参考方向。答案强调判断标准，不要求代码逐字一致。

<details><summary>第1–5章：请求、路由、模板和 Static</summary>

- 修改响应后应能在 Network 中看到200、正确 URL 和新的响应体。
- App 路由使用 `app_name = "employees"`，模板使用 `employees:list`、`employees:detail`，不硬编码 `/employees/...`。
- 空列表应进入 `{% empty %}` 或空数据分支；表格 `colspan` 与列数一致。
- CSS 文件名故意写错时应观察到404，恢复后为200。先查 Network，不先猜模板缓存。

</details>

<details><summary>第6章：Model 与迁移</summary>

`Department.description` 可写为 `models.TextField("说明", blank=True)`。随后依次执行 `makemigrations`、`sqlmigrate employees 0002`、`migrate` 和 `check`。迁移文件应进入 Git；本地数据库是否提交遵守项目约定。

</details>

<details><summary>第7–8章：Admin、ORM 列表和详情</summary>

- Admin 员工列表增加 `email` 后，搜索编号或姓名、按部门筛选都应生效。
- 查看者账号只授予 `view_employee` 时可以查看但不能修改。
- 列表 QuerySet 使用 `filter(is_active=True).select_related("department")`；详情使用 `get_object_or_404()`。不存在和离职员工详情返回404而不是500。

</details>

<details><summary>第9–10章：新增、编辑和逻辑删除</summary>

- 编号输入 `e003` 后保存为 `E003`；不以 E 开头和重复编号都显示表单错误。
- 编辑必须传 `instance=employee`，提交后主键和总记录数不变。
- GET 删除地址只显示确认页；POST 后记录仍存在但 `is_active=False`，列表不再显示它。

</details>

<details><summary>第11章：搜索与分页</summary>

空关键字返回全部在职员工；`q=开发` 可匹配部门；`page=abc` 不产生500。分页链接需要保留除 `page` 外的查询参数，推荐复制 `request.GET`、删除 `page` 后使用 `urlencode()`，避免手工拼接遗漏日期条件。

</details>

<details><summary>第12–13章：登录和权限</summary>

- 匿名访问员工列表：302到登录页，并包含 `next`。
- 已登录但没有 `view_employee`：403。
- 查看者：列表和详情200，新增/编辑/删除403。
- 维护者：按分配权限完成对应操作。模板隐藏按钮后仍要直接请求 URL，确认后端不能绕过。

</details>

<details><summary>第14–15章：文件、日志和异常</summary>

- 上传表单同时具有 `multipart/form-data` 和 `request.FILES`。
- 非 PDF、超过5 MB、无权限下载均失败且不留下无效数据库记录。
- 下载 View 只接受附件主键，不接受任意服务器路径。
- 正常响应包含 `X-Request-ID`；日志保留业务主键和用户主键，不记录密码、Cookie 和附件内容。

</details>

<details><summary>第16章：测试</summary>

最小回归集至少覆盖匿名跳转、查看权限、无权限403、搜索结果、新增成功、GET不删除、POST逻辑删除、附件类型错误和安全下载。文件响应测试结束后调用 `response.close()`，避免 Windows 文件锁影响清理。

</details>

<details><summary>第17–18章：交付与 SES 改修</summary>

- 新目录按 README 能完成安装、迁移、系统检查和测试，才算可重建。
- 日期筛选使用 GET Form 校验；From 晚于 To 时显示表单错误，不执行错误范围查询。
- QuerySet 使用 `joined_on__gte`、`joined_on__lte`，分页保留完整查询字符串。
- Review 说明包含规格、影响文件、测试证据、迁移/配置影响、回滚方法和未解决事项。

</details>
