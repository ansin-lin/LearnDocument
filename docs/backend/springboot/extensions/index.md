# Spring Boot 企业扩展：Redis、批处理与账票

这组内容从[第18章完成部署与运行检查的员工管理API](../mainline/18_deployment_operations_check.md)出发。主线仍可独立结业；只有实际项目需要缓存、批量任务或文件报表时，才继续此路线。不要把 Redis、Spring Batch 和 Excel 当成完成普通 CRUD 的先修条件。

## 学完能交付什么

最终交付一个“月度员工变更账票”扩展：管理员提交月份，后台批量读取 `employee_change_logs`，生成 Excel 文件；接口能查询任务状态、下载结果。MySQL 保存任务事实和文件元数据，Redis 只加速可重建的状态读取。能证明同月请求不重复生成、任务失败可重试、文件只对授权用户开放。

| 顺序 | 内容 | 阶段成果 |
| --- | --- | --- |
| 1 | [Redis：从键值操作到缓存一致性](redis.md) | 为员工详情加缓存，验证命中、过期、改修失效与 Redis 故障降级 |
| 2 | [Spring Batch：从 Job 到可重启任务](spring-batch.md) | 批量读取履历，理解分块事务、参数、重启、幂等、跳过与重试 |
| 3 | [账票：CSV、Excel、下载与安全](reporting.md) | 根据账票规格生成可核对的 CSV/Excel，完成权限、文件管理和测试 |
| 4 | [月度员工变更账票项目](monthly-report-project.md) | 整合批处理、Excel 与可选 Redis，留下可运行、可测试、可交接的成果 |

推荐先完成第9、12、13、15～18章；不了解 SQL 范围查询时先复习[MyBatis 查询](../../mybatis/06_select_result_mapping.md)。Redis 环境操作可参照[Docker Redis](../../../tools/docker/14_redis.md)，但本路线不依赖 Docker：可用个人本地 Redis 或受控测试实例。

## 固定的练习起点

在独立目录复制第18章通过运行检查的员工管理项目，不在原主线工程直接实验。保留 `employees`、`employee_change_logs`、`app_users` 三张表及现有权限、测试；数据库另建个人练习库，不连接共享/生产库。Java 17、Spring Boot 3.5.16、MyBatis Starter 3.0.x、MySQL 8.0.16+ 不变。Redis 和 Batch 的依赖交给Spring Boot 3.5的依赖管理；Apache POI 本练习锁定5.5.1，升级前从[官方发布页](https://poi.apache.org/download.html)核对并运行完整回归。

在每章结束时提交一个可回退的练习提交。若只学某一方向，从同一验收快照分支开始；最终项目则顺序整合三项成果。这样不会把某个实验的半成品错误地当作下一个实验的前提。

## 三项技术各管什么

```text
MySQL：员工、变更履历、任务状态、账票文件元数据（事实）
Redis：员工详情或任务状态的短期副本（可丢弃）
Spring Batch：有状态、可观察、可恢复的批量读取/处理/写入
账票组件：CSV / Excel / PDF 文件的内容与交付
```

“定时触发”不是 Batch 的定义，“生成 Excel”也不是 Batch 自动提供的能力。短小同步导出可不使用 Batch；当数据量、运行时长、失败恢复、审计要求上升时才引入它。Redis 不应成为账票完成状态的唯一记录。

## 验收证据

每个阶段保留：使用的配置（隐去密钥）、输入样例、HTTP/命令输出、MySQL 查询、测试报告、失败与恢复记录。删除姓名、邮箱、账号、会话标识等敏感值后再提交证据。最终验收见[项目章节](monthly-report-project.md)。

## 官方资料

- [Spring Boot 3.5 文档](https://docs.spring.io/spring-boot/3.5/reference/index.html)
- [Spring Data Redis 模板](https://docs.spring.io/spring-data/redis/reference/redis/template.html)
- [Spring Batch 5.2 参考文档](https://docs.spring.io/spring-batch/reference/5.2/index.html)
- [Apache POI Spreadsheet](https://poi.apache.org/components/spreadsheet/)
