# Spring Boot 企业扩展：Redis、批处理与账票

这里提供可分别学习的企业扩展方向。Redis 专题独立讲概念、常用调用与适用场景；[Spring Batch + Scheduling 六章专题](spring-batch-training/README.md)以考勤日次汇总为背景，自带独立工程、练习和运行手册。账票教材及月度账票项目可从[第18章的员工管理 API](../mainline/18_deployment_operations_check.md)继续，并将Batch专题学到的机制应用到员工变更履历。它们都不是完成普通 CRUD 的先修条件。

## 学完能做到什么

Redis 专题的目标是会解释、会调用、会判断，不要求项目交付。选择 Batch 与账票方向的学员，可继续完成“月度员工变更账票”扩展：管理员提交月份，后台批量读取 `employee_change_logs` 并生成 Excel；MySQL 保存任务事实和文件元数据，Redis 只是可选的状态读取缓存。

| 顺序 | 内容 | 阶段成果 |
| --- | --- | --- |
| 1 | [Redis 单文件专题](redis.md) | 理解数据类型、TTL、Spring Boot 调用、缓存与 Session 使用条件；无需接入员工项目 |
| 2 | [Spring Batch + Scheduling：六章考勤批处理](spring-batch-training/README.md) | 在独立工程中学习 Job、Step、Tasklet、Chunk、参数、重启、定时触发与 CSV 交付；进入月度账票项目时迁移处理思路，不复用考勤代码 |
| 3 | [账票基础](01_report_overview.md) | 从日本项目业务场景判断格式、模板方式和在线/Batch 输出 |
| 4 | [账票设计与架构](02_report_design_and_architecture.md) | 阅读抽出条件、字段映射和分层架构，理解设计书如何对应代码 |
| 5 | [无模板账票](03_report_without_template.md) | 理解 CSV、POI 对象和従業員一覧表的查询、生成与下载流程 |
| 6 | [有模板账票](04_report_with_template.md) | 阅读請求書模板，掌握固定字段和简单动态明细的填充方式 |
| 7 | [账票交付与 Batch](05_report_delivery_and_batch.md) | 理解下载、保存、Batch、大文件、权限、异常和 PDF 的基本边界 |
| 8 | [综合理解与代码阅读](06_report_comprehensive_practice.md) | 通过设计阅读、调用分析和改修影响调查确认学习成果 |
| 参考 | [完整参考代码](reporting-reference/README.md) | 查阅进阶 Generator、模板插行、FileStore 和交付实现；不作为新人必修 |
| 9 | [月度员工变更账票项目](monthly-report-project.md) | 整合批处理、Excel 与可选 Redis，留下可运行、可测试、可交接的成果 |

Redis 专题建议先了解 Java、Spring Boot 的 Service 与依赖注入；第 2 节给出个人 Docker 实验。月度账票方向的 Batch 与账票建议先完成第 9、12、13、15～18 章；不了解 SQL 范围查询时先复习[MyBatis 查询](../../mybatis/06_select_result_mapping.md)。考勤六章专题按自己的[学习入口](spring-batch-training/README.md)准备独立数据库和工程，不要求先完成月度账票。Redis 不是月度账票项目的必需前置。

## Batch 与账票学习起点

六章账票教材以概念理解、代码阅读和小改修影响分析为主，不要求先复制完整工程。需要实际运行选做代码或月度账票项目时，再在独立目录复制第18章通过运行检查的员工管理项目，不在主线工程直接实验。保留 `employees`、`employee_change_logs`、`app_users` 三张表及现有权限、测试；数据库另建个人练习库，不连接共享/生产库。Java 17、Spring Boot 4.1.x（POM固定4.1.1）、MyBatis Starter与Test Starter 4.1.0、MySQL 8.0.16+ 不变。Redis 和 Batch 的依赖交给Spring Boot 4.1的依赖管理；Apache POI 本练习锁定5.5.1，升级前从[官方发布页](https://poi.apache.org/download.html)核对并运行完整回归。

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

Redis 专题可用少量命令输出确认自己会调用和解释结果，不需要提交项目级证据。账票六章以第6章的判断、代码阅读、流程排序和影响分析为验收，不要求完整实现两套系统。希望继续挑战生产型整合时，再进入[月度员工变更账票项目](monthly-report-project.md)；记录中应删除姓名、邮箱、账号、会话标识等敏感值。

## 官方资料

- [Spring Boot 4.1 文档](https://docs.spring.io/spring-boot/reference/index.html)
- [Spring Data Redis 模板](https://docs.spring.io/spring-data/redis/reference/redis/template.html)
- [Spring Batch 5.2 参考文档](https://docs.spring.io/spring-batch/reference/5.2/index.html)
- [Apache POI Spreadsheet](https://poi.apache.org/components/spreadsheet/)
