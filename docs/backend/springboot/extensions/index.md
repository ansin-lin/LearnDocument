# Spring Boot 企业扩展：Redis、批处理与账票

这里提供可分别学习的企业扩展方向。Redis 专题独立讲概念、常用调用与适用场景；Spring Batch、账票及其综合项目可从[第18章的员工管理 API](../mainline/18_deployment_operations_check.md)继续。它们都不是完成普通 CRUD 的先修条件。

## 学完能做到什么

Redis 专题的目标是会解释、会调用、会判断，不要求项目交付。选择 Batch 与账票方向的学员，可继续完成“月度员工变更账票”扩展：管理员提交月份，后台批量读取 `employee_change_logs` 并生成 Excel；MySQL 保存任务事实和文件元数据，Redis 只是可选的状态读取缓存。

| 顺序 | 内容 | 阶段成果 |
| --- | --- | --- |
| 1 | [Redis 单文件专题](redis.md) | 理解数据类型、TTL、Spring Boot 调用、缓存与 Session 使用条件；无需接入员工项目 |
| 2 | [Spring Batch：从 Job 到可重启任务](spring-batch.md) | 批量读取履历，理解分块事务、参数、重启、幂等、跳过与重试 |
| 3 | [账票：CSV、Excel、下载与安全](reporting.md) | 根据账票规格生成可核对的 CSV/Excel，完成权限、文件管理和测试 |
| 4 | [月度员工变更账票项目](monthly-report-project.md) | 整合批处理、Excel 与可选 Redis，留下可运行、可测试、可交接的成果 |

Redis 专题建议先了解 Java、Spring Boot 的 Service 与依赖注入；第 2 节给出个人 Docker 实验。Batch 与账票方向建议先完成第 9、12、13、15～18 章；不了解 SQL 范围查询时先复习[MyBatis 查询](../../mybatis/06_select_result_mapping.md)。Redis 不是月度账票项目的必需前置。

## Batch 与账票项目的练习起点

学习 Batch、账票及其项目时，在独立目录复制第18章通过运行检查的员工管理项目，不在原主线工程直接实验。保留 `employees`、`employee_change_logs`、`app_users` 三张表及现有权限、测试；数据库另建个人练习库，不连接共享/生产库。Java 17、Spring Boot 4.1.x（POM固定4.1.1）、MyBatis Starter与Test Starter 4.1.0、MySQL 8.0.16+ 不变。Redis 和 Batch 的依赖交给Spring Boot 4.1的依赖管理；Apache POI 本练习锁定5.5.1，升级前从[官方发布页](https://poi.apache.org/download.html)核对并运行完整回归。

Redis 使用自己的个人练习实例即可；Batch 与账票若进入最终项目，再从稳定快照逐步整合。不要把 Redis 的实验状态当作其他方向的前置条件。

## 三项技术各管什么

```text
MySQL：员工、变更履历、任务状态、账票文件元数据（事实）
Redis：员工详情或任务状态的短期副本（可丢弃）
Spring Batch：有状态、可观察、可恢复的批量读取/处理/写入
账票组件：CSV / Excel / PDF 文件的内容与交付
```

“定时触发”不是 Batch 的定义，“生成 Excel”也不是 Batch 自动提供的能力。短小同步导出可不使用 Batch；当数据量、运行时长、失败恢复、审计要求上升时才引入它。Redis 不应成为账票完成状态的唯一记录。

## 验收证据

Redis 专题可用少量命令输出确认自己会调用和解释结果，不需要提交项目级证据。Batch 与账票的最终验收见[项目章节](monthly-report-project.md)；记录中应删除姓名、邮箱、账号、会话标识等敏感值。

## 官方资料

- [Spring Boot 4.1 文档](https://docs.spring.io/spring-boot/reference/index.html)
- [Spring Data Redis 模板](https://docs.spring.io/spring-data/redis/reference/redis/template.html)
- [Spring Batch 5.2 参考文档](https://docs.spring.io/spring-batch/reference/5.2/index.html)
- [Apache POI Spreadsheet](https://poi.apache.org/components/spreadsheet/)
