# MyBatis 学完后的综合练习

## 任务名称

为任务管理核心功能实现完整的 MySQL 数据结构和 MyBatis 数据访问层。

## 开始条件

- 已完成 MyBatis 课程和必要的 SQL 基础。
- 使用 MySQL 8，数据库名为 `workhub_task_practice`。
- 包名使用 `com.example.workhubtask`。
- SQL 放在 `backend/db/`，Mapper XML 放在 `backend/src/main/resources/mapper/`。

## 要完成的内容

### 数据库准备

- 按统一规格创建 projects、employees、tasks 三张表。
- 设置主键、唯一约束、必填约束和外键。
- 插入统一规格中的项目、员工和三条固定任务。
- 提供可重复执行或有明确清理步骤的建表、初始数据脚本。

### Java 与映射

- 建立 TaskEntity、TaskSearchCondition 和用于关联查询的读取模型。
- Java 属性使用 camelCase，数据库字段使用 snake_case，通过显式 resultMap 映射。
- 创建 TaskMapper 和同 namespace 的 XML。
- SQL 明确列名和别名，不使用 `SELECT *`。

### Mapper 功能

- `selectById`：按 id 查询详情并返回 projectName、assigneeName。
- `search`：按关键字、项目、负责人、状态进行组合查询，固定排序并分页。
- `count`：使用与 search 相同的过滤条件统计总数。
- `insert`：新增任务并回填自增 id。
- `update`：按 id 更新允许编辑的字段，并返回影响行数。
- `updateStatus`：按 id 更新状态，并返回影响行数。

### SQL 和安全要求

- keyword 同时模糊匹配 task_code 和 title，空白关键字按无条件处理。
- 使用 where/if 等动态 SQL 正确处理任意条件组合。
- 所有外部值使用 `#{}` 参数绑定，不用 `${}` 拼接值。
- 项目和负责人名称通过 JOIN 一次取得，不能循环查询形成 N+1。
- 状态迁移是否合法由后续 Service 判断，Mapper 负责执行参数化更新。

## 必做测试

1. id=1 返回完整任务，id=9999 返回 null。
2. 空条件返回三条；状态、关键字和两个条件组合结果正确。
3. 无结果时 search 返回空列表、count 返回 0。
4. pageSize=2 时第一页两条、第二页一条，total 为 3。
5. 关联查询返回正确的项目名和负责人名，SQL 没有 N+1。
6. 新增后取得自增 id，并能重新查询。
7. 编辑存在任务时更新一行，再次查询得到新值。
8. 编辑不存在的 id 时更新行数为 0。
9. 状态更新成功后再次查询得到新状态。
10. 在事务测试中执行写操作，测试结束回滚，固定初始数据不被污染。

## 限制

- 数据库密码使用本地环境变量或未提交配置，不写入仓库。
- 不通过修改数据库列名解决 Java 映射问题。
- 不把启用缓存作为 SQL 或查询次数问题的默认修复。
- 本阶段不实现 Controller、页面、权限、评论和附件。

## 提交物

- 建表和初始数据 SQL。
- 实体、查询条件、读取模型、Mapper 接口和 XML。
- Mapper 测试及执行结果。
- `evidence/mybatis-check.md`，记录十个测试、实际 SQL 和一次问题修正。

完成标准：数据库层能够独立完成任务查询、关联、分页、新增、编辑和状态更新，并能正确判断更新影响行数。
