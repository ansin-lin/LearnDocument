# 整改与验证报告

初始验证日期：2026-10-20；六章整改复核日期：2026-10-21。范围仅为 [spring-batch-training](README.md) 独立专题及站点导航入口；未改写 Spring Boot 主线工程。结论应按下方“已验证 / 未验证”区分，不把 H2 测试宣称为 MySQL 验收。

## 交付检查

| 项目 | 结果 |
| --- | --- |
| 六章正式教材 | [01～06 章](README.md)按“认识 Batch → 架构/Tasklet → Chunk → 事务/重启/管理 → Scheduling → 综合实践”组织，站点导航仅保留六章 |
| 独立工程 | [Maven 工程](examples/attendance-batch/pom.xml)，四 Step Job、手动/定时入口、Tasklet、Chunk、SQL、CSV |
| 数据 | [MySQL 业务建表](sql/schema.sql)与 [25 条虚构样例](sql/sample_data.sql)；测试专用 H2 schema/data |
| 教学配套 | [学员任务](exercises/assignments.md)、[教师参考](exercises/instructor_answers.md)、[运行手册](runbook.md) |
| 主线隔离 | 使用独立文件夹与 `attendance_batch_training` 库名；未修改 `employee-management-api` |

旧九章原文保存在 [历史备份](backup/2026-10-21-nine-chapter/)（`.md.txt`，不参与正式导航）：旧 01 → 新 01；旧 02+03 → 新 02；旧 04 → 新 03；旧 05+06 → 新 04；旧 07+08 → 新 05；旧 09 → 新 06。旧九章已从正式 `docs/` 移除，避免两套课程并列。

## 依赖与执行环境

`pom.xml` 使用 Spring Boot parent 4.1.1、`java.version=17`、MyBatis Starter 4.1.0。实际 `mvn dependency:tree` 解析 Spring Batch `spring-batch-core 6.0.5`、Spring Framework `spring-context 7.0.9`。使用 `spring-boot-starter-batch-jdbc`，测试中的 `BATCH_JOB_EXECUTION` 可查询到持久化执行记录；没有添加 `@EnableBatchProcessing` 覆盖 Boot 自动配置。

本机执行环境：Java **21.0.10**，Maven **3.9.14**，测试数据库 H2 **2.4.240**（MySQL 兼容模式）。本轮 `mvn -q clean test`、`mvn -q package` 均退出码 0，后者也执行测试。编译目标为 Java 17，但**未在 JDK 17 实际运行**。

## 自动测试实绩

最终复跑：Surefire XML 共 **11 tests / 0 failures / 0 errors / 0 skipped**，覆盖以下十个测试类：

| 测试类 | 用例数 | 实测关注点 |
| --- | ---: | --- |
| `AttendanceProcessorTest` | 2 | 工时/加班计算、非法休息时间 |
| `PlainAttendanceSummaryServiceTest` | 1 | 非 Batch Service 汇总 |
| `AttendanceBatchIntegrationTest` | 1 | 正常 25 条、Chunk 写入、空日、参数错误、重复拒绝、第 13 条失败与同参数重启 |
| `SkipIntegrationTest` | 1 | 无效记录 Skip 一条、汇总 24 行 |
| `RetryIntegrationTest` | 1 | Writer 暂时故障后有限 Retry、汇总 25 行 |
| `CsvFailureIntegrationTest` | 1 | CSV 输出路径故障、同参数恢复 |
| `AttendanceSchedulerTest` | 1 | 日本时间前一日的日期计算 |
| `ScheduledLaunchIntegrationTest` | 1 | 定时触发后产生 JDBC JobExecution |
| `TaskletLessonIntegrationTest` | 1 | 独立教学 Job/Step 的执行记录、顺序和缺参数失败 |
| `NumberChunkLessonIntegrationTest` | 1 | 数字 Chunk 的 2/2/1、Size 5、过滤输出、Reader 结束、第二次独立运行与业务表不受影响 |

`AttendanceBatchIntegrationTest` 中多个场景在同一个测试方法内连续执行，因此“11 tests”是 JUnit 方法数，**不是**仅验证十一个业务现象。测试还核对了第 13 条失败后的 Reader 检查点源 ID 110、重启保持同一 JobInstance；Skip/Retry 测试补查了 Step 状态、读写/跳过件数与最终检查点。Tasklet 和数字 Chunk 教学 Job 与正式四 Step Job 分开，且只在对应教学测试中启用。数字实验 Writer 把结果留在内存，不证明业务数据库提交。测试运行会打印预期的失败 Step 异常日志，不表示测试失败。CSV 故障测试只清理其专用 `target/csv-blocker` 下的已知生成文件。

## 本轮教学质量复核

| 章节 | 本轮新增或补强 | 正文独立性与验证 |
| --- | --- | --- |
| 01 | 3 行纸面数据、Mapper 查询与 Processor/UPSERT 职责、while 循环逐轮追踪、失败事务边界 | 明确与正式 25 条样例不同；普通 Service 测试只证明调用，不冒充恢复验证 |
| 02 | 完整 `TaskletLessonConfig`、Bean 创建与 Job 执行两条 Mermaid 流程、调用顺序表、第二个 Step | 不打开 Java 文件也可读懂 Job/Step/Tasklet 的创建、调用、参数、异常与元数据 |
| 03 | 新增可运行 `NumberChunkLessonConfig`；完整 Reader/Processor/Writer 类、员工 1001 的输入输出、事务件数表 | 数字实验三种分组有自动断言；过滤后 Writer 为 2/1/1，业务表不被写入 |
| 04 | JobInstance 与 Execution 身份示例、内存/持久化检查点、失败与重启流程、Skip 与 Retry 分别解释 | 三个隔离测试核对状态、件数与 Context；未把实验容错策略当正式业务默认值 |
| 05 | 完整 Scheduling、Scheduler、BatchTrigger 类；rate/delay 示意时间线、同步/异步边界、异常调查表 | 定时测试验证单进程触发；未把示意时间线写成实测日志 |
| 06 | 真实工程结构、入口到输出的阅读路线、四 Step 输入输出/事务表、交付证据模板 | 综合任务仍沿同一考勤案例，不重复前五章的完整接口讲解 |

技术复核将正文中的 Builder、`ItemStreamReader`、`Chunk`、`JobLauncher` 与工程编译/API 对照，并参考 [Spring Batch 6.0.5 官方参考](https://docs.spring.io/spring-batch/reference/) 和 [Spring Framework Scheduling 官方说明](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)。未新增 Redis、Kafka 等无关依赖，也未修改 Employee Management 主线。仍需教师在目标 Java 17/MySQL 8 环境试讲并实操，以检验新人的独立完成程度。

## 尚未完成的实际环境验证与残留风险

1. 本机没有可用的 MySQL 测试账号，因此**未运行 MySQL 8 建库、框架元数据初始化、MySQL 方言、权限或实际 JDBC 故障恢复**。交付前需按 [运行手册](runbook.md)在个人 MySQL 8 环境首跑并留证据。H2 成功不等同 MySQL 成功。
2. 本机只有 JDK 21；未在 Java 17 JVM 下执行。教学采用 Java 17 源码目标，正式基线签收需在 JDK 17 复跑构建、测试与 Jar。
3. 定时集成测试验证“触发并创建执行记录”；完整业务结果由手动 Job 集成测试验证。真实 02:00 JST、时钟切换、多实例同时触发未在真实部署环境测试，只有设计说明和风险分析。
   rate/delay 的三轮实际 JAR 日志实验也未在个人 MySQL 环境执行；教材时间线标明为示意，需课堂现场留证据。
4. CSV 的 `ATOMIC_MOVE` 依赖同文件系统支持；测试环境成功不保证目标共享文件系统支持。外部消费与数据库提交不是同一事务，交付前需验证下游取文件策略。
5. 数据源运行中变动、百万级吞吐和压力场景未测试。本例面向普通新人改修，复杂劳动法规、跨日班与大规模并行属于后续规格或扩展课程。

验收建议：教学文档与 H2 自动测试可进入教师试讲；若要求“在指定 Java 17 + MySQL 8 环境完整运行已验证”，当前**不能**给出通过结论，待补上述实测记录。

## 文档审计

2026-10-21 六章整改后，正式 `docs/` 仅保留六章，每章 15 个教学小节；章节、导航、入口和练习引用已同步。移动到 `docs/backend/springboot/extensions/spring-batch-training/` 后，本轮专题审计扫描 **11 个 Markdown 文件，P0=0、P1=0、P2=0**；`mkdocs.yml` 可解析，旧路径在正式课程与导航中无残留。示例工程在新位置复跑 **11 tests / 0 failures / 0 errors**。此前结构迁移时按规则执行过全库审计：扫描 1620 个 Markdown 文件，报告 **P0=356、P1=274、P2=97**，扩展区无发现；其他发现主要位于旧备份、`node_modules` 等内容。本轮未改导航，专题范围审计不替代上述真实运行验证。

## 第三轮精修复核（2026-10-21）

本轮保留六章结构与统一考勤案例，局部补充业务日期来源、Bean 参数注入、执行身份/第 13 条时间线、rate/delay 完整对照及交付验收层次。数字教学 Reader 改为 `@StepScope`，每次独立 StepExecution 新建位置；新增同一 Job 用新标识参数第二次运行、再次读取 1～5 的断言。它仍未实现持久化 Restart，正式考勤 Reader 的 `ItemStreamReader` 才负责保存检查点。

本机 Java 21.0.10 执行 `mvn -q clean test` 退出码 0；Surefire XML 共 **10 个类、11 tests / 0 failures / 0 errors / 0 skipped**。`mvn -q -DskipTests package` 退出码 0；此打包命令跳过测试，测试结论只来自前述完整测试命令。`mvn dependency:tree '-Dincludes=org.springframework.batch:spring-batch-core'` 解析 `spring-batch-core 6.0.5`，Spring Boot parent 仍为 4.1.1。专题审计扫描 11 个 Markdown：**P0=0、P1=0、P2=0**。Java 17 只作为编译目标、未用 JDK 17 实测；H2 自动测试已通过；MySQL 8、真实 JAR 定时日志实验和业务方验收仍**未验证**。
