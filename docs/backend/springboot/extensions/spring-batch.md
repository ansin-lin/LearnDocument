# Spring Batch：把批量处理做成可检查、可恢复的任务

## 1. 为什么不是在 Controller 里循环

一次处理数万条员工变更履历时，HTTP 请求可能超时；处理中途失败后，也不能只凭一条“500”判断处理了多少。Spring Batch 把一次批量执行表示为 `Job`，由一个或多个 `Step` 组成，并用 `JobRepository` 保存执行元数据。一个 Step 可按固定数量读取、处理、写出并提交，这叫 chunk；每个 chunk 是独立事务。`JobLauncher` 启动 Job，`JobExplorer` 可查询执行状态。它们分别解决执行、记录和观察问题，不会自动替业务保证“同月只生成一份”。

本章在[扩展入口](index.md)的独立员工项目副本中练习，仍用 MySQL 与 MyBatis 管理员工业务。Batch 的元数据表与业务表同库但不同表；不直接改写主线员工数据。先做 3 行 CSV 导入的最小例子，再把 `employee_change_logs` 分块写入报表临时明细。文件生成放到[账票章节](reporting.md)。

## 2. Job、Step、Execution、Instance 的关系

```text
Job 定义：monthlyChangeJob
  └─ Step 定义：collectChangeRowsStep
       ├─ ItemReader：逐条读
       ├─ ItemProcessor：校验/转换（不需要时可省略）
       └─ ItemWriter：按 chunk 写

JobInstance = Job 名称 + 标识性 JobParameters（同一业务实例）
JobExecution = 某次尝试运行，失败后重启会产生新的 Execution
StepExecution = 该次尝试中某 Step 的执行结果/计数
ExecutionContext = 重启时读取位置等状态
```

`COMPLETED` 的同一 JobInstance 不能原样再执行；`FAILED` 可以按设计重启。提交另一个月份应成为另一个 Instance。若想“重新生成同月”，需明确版本/申请号及覆盖规则，不能偷偷加随机 `run.id` 绕过业务唯一性。`readCount`、`writeCount`、`skipCount` 是运行证据，最终仍需核对业务表。

## 3. 环境与第一次运行

在 `pom.xml` 增加 `spring-boot-starter-batch` 和已有数据库驱动。Boot 3.5 使用 Spring Batch 5 系列，具体受 Boot 依赖管理约束，不手工混配另一主版本。`application-dev.yml` 加：

```yaml
spring:
  batch:
    job:
      enabled: false # 启动 Web 服务不自动执行所有 Job
    jdbc:
      initialize-schema: never
```

个人空练习库中，先用当前依赖所附的 MySQL Batch schema 脚本创建 `BATCH_*` 元数据表；之后用受控迁移管理，不在共享库使用 `DROP` 或每次启动自动重建。元数据表不是业务结果表。数据库账号需有这些表所需的读写权限。先查官方 [Boot Batch 配置](https://docs.spring.io/spring-boot/3.5/how-to/batch.html) 和 [Batch Repository](https://docs.spring.io/spring-batch/reference/5.2/job.html) 对当前版本的要求。

先用三行 `employees.csv` 练习，文件列为 `employee_id,name,department`，包含表头且仅使用虚构数据。以 `FlatFileItemReader` 读入独立的 `ImportRow`，`ItemProcessor` 验证 `employee_id` 和空名称，`JdbcBatchItemWriter` 写入个人练习表 `employee_import_stage`。不要导入现有 `employees`，否则会与主线业务规则冲突。练习表：

```sql
CREATE TABLE employee_import_stage (
  employee_id BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  department VARCHAR(50) NOT NULL
);
```

Batch 5 的定义写法如下，Reader/Writer 的具体 Bean 使用 Spring Batch 对应 Builder；示例省略常规 import 与 DTO getter，但关键连接点不能省：

```java
@Bean
Job importEmployeesJob(JobRepository repository, Step importEmployeesStep) {
    return new JobBuilder("importEmployeesJob", repository)
            .start(importEmployeesStep).build();
}

@Bean
Step importEmployeesStep(JobRepository repository,
                         PlatformTransactionManager tx,
                         FlatFileItemReader<ImportRow> reader,
                         JdbcBatchItemWriter<ImportRow> writer) {
    return new StepBuilder("importEmployeesStep", repository)
            .<ImportRow, ImportRow>chunk(2, tx)
            .reader(reader)
            .processor(row -> {
                if (row.employeeId() <= 0 || row.name().isBlank())
                    throw new IllegalArgumentException("invalid import row");
                return row;
            })
            .writer(writer)
            .build();
}
```

`chunk(2)` 使三行样例分成两次提交。Reader 必须关闭，文件编码设 UTF-8、列数和引号规则必须与规格一致；Writer 使用绑定参数，不拼接 CSV 字段为 SQL。运行前给 Job 传入固定的标识性文件批次号，例如 `sourceId=demo-001`，由 `JobLauncher.run(job, new JobParametersBuilder().addString("sourceId", "demo-001").toJobParameters())` 发起。在本地专用测试中核对 stage 表恰好 3 行，JobExecution 为 `COMPLETED`，read/write 均为 3；再次用完全相同参数运行会被 Batch 拒绝，不能默默追加 3 行。删除或修改练习表前先确认只在个人库。

Reader 和 Writer 的最小配置可写成以下两个 Bean。`ImportRow` 是 `record ImportRow(long employeeId, String name, String department) {}`；把虚构样例放在 `src/main/resources/data/employees.csv`。示例仅处理不带逗号/换行的固定三列；真实 CSV 需另定引号、编码和坏行策略。

```java
@Bean
FlatFileItemReader<ImportRow> importReader() {
    return new FlatFileItemReaderBuilder<ImportRow>()
            .name("importEmployeesReader")
            .resource(new ClassPathResource("data/employees.csv"))
            .encoding("UTF-8")
            .linesToSkip(1)
            .delimited().names("employee_id", "name", "department")
            .fieldSetMapper(fields -> new ImportRow(
                    fields.readLong("employee_id"),
                    fields.readString("name"),
                    fields.readString("department")))
            .build();
}

@Bean
JdbcBatchItemWriter<ImportRow> importWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<ImportRow>()
            .dataSource(dataSource)
            .sql("INSERT INTO employee_import_stage "
                    + "(employee_id, name, department) VALUES (?, ?, ?)")
            .itemPreparedStatementSetter((item, ps) -> {
                ps.setLong(1, item.employeeId());
                ps.setString(2, item.name());
                ps.setString(3, item.department());
            })
            .build();
}
```

练习文件内容：

```csv
employee_id,name,department
2001,Sato,Sales
2002,Li,Development
2003,Kim,Support
```

## 4. 月度履历的分块处理

进入实际任务时，先定义统计期间：日本业务时区的 2026 年 9 月对应 `[2026-09-01 00:00:00, 2026-10-01 00:00:00)`。现有 `created_at` 是 MySQL `DATETIME`、Java `LocalDateTime`，不自带时区；数据库和应用必须约定存储为日本业务本地时间。如果现有部署不是这个约定，应先完成时间语义调查，不要直接用服务器默认时区。采用“左闭右开”，避免月底毫秒精度和跨月重复。

新增报表明细暂存表，不改变原履历：

```sql
CREATE TABLE report_change_rows (
  report_id BIGINT NOT NULL,
  log_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  detail VARCHAR(200) NOT NULL,
  changed_at DATETIME NOT NULL,
  PRIMARY KEY (report_id, log_id),
  INDEX idx_report_rows_order (report_id, changed_at, log_id)
);
```

Reader 选择稳定排序 `created_at, id`，列出具体列，不用 `SELECT *`：

```sql
SELECT id, employee_id, action, detail, created_at
FROM employee_change_logs
WHERE created_at >= :from AND created_at < :to
ORDER BY created_at, id
```

MyBatis 适合原有在线查询；Batch Reader 可以用 JDBC 游标或分页。游标容易入门，但必须由 `ItemStream` 保存位置，源数据在重启间改变时，按行号恢复可能漏读/重复；分页也需要稳定且唯一的排序键。实际账票要求固定快照时，应先确定截止边界（例如申请时的最大 log ID）并在重启时复用，或把当次源数据物化到暂存表。不能仅凭 `ORDER BY` 宣称获得了快照隔离。Writer 按 `report_id, log_id` 写入，唯一键防止重复；若重启可能重放 chunk，用 MySQL `INSERT ... ON DUPLICATE KEY UPDATE` 将相同值写回，或在受控重跑前清理**本报表 ID**的暂存行。不要跨报表清理。

Job 参数至少包含 `reportId`、`month`、`from`、`to`、`cutoffLogId`。月份和边界作为标识性参数；`reportId` 关联业务申请。参数只能是可序列化的简单值，Reader 用 `@StepScope` 在执行时读取参数；不要从可变全局字段取月份。Step 的处理逻辑保持“读履历 → 只做必要字段转换 → 写暂存行”，Excel 文件不在每条记录的事务中反复打开。

## 5. 失败、重启、幂等

假设 chunk 大小 100：前 200 条已提交，第 201 条失败，当前 chunk 回滚；若状态可恢复，重启从最后提交点继续。业务结果也必须幂等，否则元数据说“从 201 继续”，Writer 却可能重复生成。独立验收要同时查 `BATCH_JOB_EXECUTION` 与 `report_change_rows`，而不是只看 `COMPLETED`。

`retry` 只适合暂时性错误（如短暂数据库连接失败），须限定异常类型、次数和退避；格式错误不会因重试而变好。`skip` 仅在规格允许“带错误清单的部分成功”时使用，并规定上限、错误行记录与最终状态；财务/人事正式账票通常不应悄悄跳过。异常被 Processor 捕获后直接返回 `null` 会过滤该行，不等同于失败，须有明确业务规格。事务外部副作用（邮件、远程 API、写文件）不会随数据库 chunk 自动回滚，需要单独补偿或放在完成后处理。

并发也要分两层：Batch 同一 JobInstance 的重入控制，以及业务表对“同月唯一申请”的约束。只靠 Java `synchronized` 在多实例部署时无效。Job 状态不等同于用户可见的账票状态；[最终项目](monthly-report-project.md)会建独立 `report_requests` 表。

## 6. 测试和运行检查

先用三行文件证明最小 Job，再用固定的 5 条履历测试月初、月末、跨月、同一时间不同 ID、空月。用测试库制造第 3 条写入失败：核对已提交 chunk、失败状态、修复后用**相同参数**重启、最终无重复行。另测两个同时提交同月申请，只有一个业务申请被接受。测试不要对真实数据文件做破坏操作；失败注入放在独立测试库。

运维观察至少包括 Job/Step 名称、执行 ID、月份、开始/结束时间、read/write/skip 数、耗时、错误摘要；日志中不要写完整履历或个人资料。记录可按执行 ID 关联，不把动态异常文本作为用户提示。保留 Batch 元数据和暂存行多久，应由业务审计与保留政策决定，不随意清库。

阶段稳定状态：导入样例和履历采集 Job 能运行、失败可诊断、重启无重复。下一章只负责把已确定的数据变成文件，不修改 Batch 的事务规则。

参考：[Spring Batch 领域模型](https://docs.spring.io/spring-batch/reference/5.2/domain.html)、[Reader/Writer](https://docs.spring.io/spring-batch/reference/5.2/readersAndWriters.html)、[重启](https://docs.spring.io/spring-batch/reference/5.2/step/chunk-oriented-processing/restart.html)、[重试](https://docs.spring.io/spring-batch/reference/5.2/step/chunk-oriented-processing/retry-logic.html)。
