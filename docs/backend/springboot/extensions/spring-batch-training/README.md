# Spring Batch + Spring Scheduling：员工考勤日次汇总

这是 Spring Boot 主线完成后的独立专题。练习工程在 [examples/attendance-batch](examples/attendance-batch/pom.xml)，只使用虚构员工与独立数据库，不依赖或修改 Employee Management API。目标是完成「勤怠日次集計・CSV連携バッチ」：按业务日期读取考勤、分块汇总、生成 CSV、保存执行状态，并能定位失败和安全重启。

技术基线：Java 源码兼容 17，Spring Boot 4.1.1，实际受 BOM 管理的 Spring Batch 6.0.5、Spring Framework 7.0.9，MyBatis Starter 4.1.0，MySQL 8。Maven 构建不显式混配 Batch 版本。当前测试环境实际运行 JDK 21.0.10、H2 2.4.240；Java 17 与 MySQL 运行尚未实测，见 [验证报告](verification_report.md)。

## 学习路线

| 阶段 | 章节 | 可检查成果 |
| --- | --- | --- |
| 认识任务 | [01 Batch 与项目实现方式](docs/01_batch_overview.md) | 区分单次请求与 Batch，读懂 PL/SQL、Shell、Java、Spring Batch 的常见组合 |
| 建立执行模型 | [02 Batch 架构与 Tasklet](docs/02_batch_architecture_and_tasklet.md) | Job/Step 可运行，能定位执行记录 |
| 处理数据与故障 | [03 Chunk](docs/03_chunk_processing.md)、[04 事务、重启与执行管理](docs/04_transaction_restart_and_job_management.md) | 25 条按 10/10/5 处理，故障后同参数重启无重复 |
| 自动触发 | [05 Scheduling 与 Batch](docs/05_scheduling_and_batch_integration.md) | 触发记录与 Job 完成结果分开核对 |
| 综合交付 | [06 勤怠日次集計・CSV連携](docs/06_comprehensive_practice.md) | 提交 CSV、SQL 核对、测试和障害調査记录 |

建议约 26 小时。先完成 Java、Spring Boot Service/MyBatis、SQL 事务、JUnit 和日志基础；本专题不要求先懂 Batch 或调度框架。[学员练习](exercises/assignments.md)与[教师参考](exercises/instructor_answers.md)分开保存。

第 02 章的 `simpleJob/attendanceTaskletJob` 和第 03 章的数字 Chunk 都是**独立教学 Job**，不改变正式考勤汇总。默认不开启：前者仅在 `app.lesson.enabled=true` 的 [TaskletLessonIntegrationTest](examples/attendance-batch/src/test/java/com/example/attendance/TaskletLessonIntegrationTest.java) 中运行，后者仅在 `app.lesson.chunk-enabled=true` 的 [NumberChunkLessonIntegrationTest](examples/attendance-batch/src/test/java/com/example/attendance/NumberChunkLessonIntegrationTest.java) 中运行。数字实验只观察内存分组；考勤数据库事务仍以正式 Job 测试核对。

| 章节 | 建议课时 | 课堂重点 |
| --- | ---: | --- |
| 01 Batch 与项目实现方式 | 3 小时 | 单次请求对照、常见技术组合、普通 Service 对照实验 |
| 02 Batch 架构与 Tasklet | 5 小时 | Job/Step/Repository、Tasklet 与执行历史 |
| 03 Chunk | 4 小时 | 25 条按 10/10/5 读取、转换、提交 |
| 04 事务、重启与执行管理 | 6 小时 | 第 13 条故障、Skip/Retry/Restart、业务日期与 JobInstance |
| 05 Scheduling 与 Batch | 4 小时 | fixedRate/fixedDelay/cron 独立实验与定时整合 |
| 06 综合实战 | 4 小时 | CSV 联携、测试、障害調査与交付 |
| **合计** | **26 小时** | 实操较慢的班级可延长 |

## 首次准备与运行

1. 在个人 MySQL 8 实例创建专用数据库与最小权限账号 `attendance_batch_training`。不要连接生产或共享业务库。
2. 选中该数据库后，在 MySQL Workbench 中依次执行 [业务表定义](sql/schema.sql)、[25 条虚构数据](sql/sample_data.sql)。`sample_data.sql` 只执行一次；重复执行会碰到主键。
3. 在本机环境中配置 `BATCH_DB_URL`、`BATCH_DB_USER`、`BATCH_DB_PASSWORD`；密码不写入源码或 Git。`BATCH_OUTPUT_DIR` 可指定可写的专用测试目录，默认是工程目录下 `output/`。
4. 在 `examples/attendance-batch` 目录用 Maven 构建。首次连接**空的 Batch 元数据表**时，临时使用 `--spring.batch.jdbc.initialize-schema=always` 创建当前依赖附带的 MySQL `BATCH_*` 表；后续运行恢复默认 `never`。不要对已经有元数据的库反复使用 `always`。

Windows PowerShell 的构建和手动执行命令：

```powershell
cd docs/backend/springboot/extensions/spring-batch-training/examples/attendance-batch
mvn test
mvn package
java -jar target/attendance-batch-1.0.0.jar --app.batch.business-date=2026-10-19 --spring.batch.jdbc.initialize-schema=always
```

首跑成功后，下一次换新业务日期时**去掉** `--spring.batch.jdbc.initialize-schema=always`。同一业务日期再次执行应被 Batch 拒绝；这不是故障，而是已经完成的 JobInstance 不可原样重复启动。命令中的日期为样例日期，不要把系统当天日期代入历史数据。测试命令使用 H2 独立内存库，不连接 MySQL。

`spring-boot-starter-batch-jdbc` 是本工程必须保留的依赖：仅用基础 Batch Starter 可能得到不持久化执行历史的仓库，无法满足跨进程恢复。项目不加 `@EnableBatchProcessing`，让 Boot 4.1.1 的 JDBC 自动配置生效。

## 预期结果与清理

样例业务日期 `2026-10-19`：源数据 25 条、汇总 25 条，CSV 有 1 行表头和 25 行数据；每条标准记录工作 480 分钟、加班 0 分钟。用 [运行手册](runbook.md)中的 SQL 核对 `BATCH_JOB_EXECUTION`、`BATCH_STEP_EXECUTION` 和业务表。`target/` 与 `output/` 是实验产物，不纳入 Git；清理时只处理个人实验库与个人输出目录，并先保存需要的调查证据。

## 验收

学员需交付：指定日期的 Job/Step 结构、成功及一次故障的执行记录、汇总表/CSV 对照、同参数重复与失败重启结果、定时触发与实际完成的分别证明，以及一份脱敏的障害調査报告。评分见 [练习题](exercises/assignments.md)；参考答案仅供教师使用。
