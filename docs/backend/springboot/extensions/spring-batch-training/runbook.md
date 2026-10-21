# 运行手册：勤怠日次集計・CSV連携バッチ

适用范围：个人实验环境的 [attendance-batch](examples/attendance-batch/pom.xml)，不适用于生产或主线 Employee Management 数据库。运行前先确认当前数据库、输出目录和业务日期。操作中不得输出密码或真实员工信息。

## 1. 环境和第一次准备

- JDK 17 或更高、Maven 3.9、个人 MySQL 8 实例；本专题源码以 Java 17 编译。当前自动测试实测环境见 [验证报告](verification_report.md)。
- 建专用空库 `attendance_batch_training` 与该库专用的最小权限账号；不要用生产账号。先在数据库客户端明确选中该库，再执行 [schema.sql](sql/schema.sql)、[sample_data.sql](sql/sample_data.sql)。样例插入只做一次。
- `BATCH_DB_URL` 应指向此库，例如 `jdbc:mysql://127.0.0.1:3306/attendance_batch_training?serverTimezone=Asia%2FTokyo`；`BATCH_DB_USER` 与 `BATCH_DB_PASSWORD` 通过环境变量传入。`BATCH_OUTPUT_DIR` 为个人可写的测试目录。
- 初次空库的 Batch 元数据由当前依赖附带的 MySQL 脚本初始化：**仅第一次**附加 `--spring.batch.jdbc.initialize-schema=always`。之后使用默认 `never`。不要手写旧版 `BATCH_*` 结构，也不要在已有元数据上反复初始化。

PowerShell 示例（替换成自己的测试账号与路径，密码输入不写进命令历史）：

```powershell
cd docs/backend/springboot/extensions/spring-batch-training/examples/attendance-batch
mvn test
mvn package
java -jar target/attendance-batch-1.0.0.jar --app.batch.business-date=2026-10-19 --spring.batch.jdbc.initialize-schema=always
```

上面 `java -jar` 的成功前提是三个 `BATCH_DB_*` 环境变量已在启动进程中正确设置。教材不提供密码值。若元数据已存在，去掉最后的初始化选项。`spring.batch.job.enabled=false` 使 Job 不会由 Boot 在启动时自动执行；`ManualBatchRunner` 仅在传入 `app.batch.business-date` 时启动。

## 2. 正常日次与空日验收

样例日期 `2026-10-19`：

```sql
SELECT COUNT(*) AS source_count FROM attendance_records
 WHERE business_date = '2026-10-19';
SELECT COUNT(*) AS summary_count FROM attendance_daily_summary
 WHERE business_date = '2026-10-19';
SELECT employee_id, worked_minutes, overtime_minutes
 FROM attendance_daily_summary
 WHERE business_date = '2026-10-19' ORDER BY employee_id;
SELECT JOB_EXECUTION_ID, STATUS, START_TIME, END_TIME, EXIT_CODE
 FROM BATCH_JOB_EXECUTION ORDER BY JOB_EXECUTION_ID DESC;
SELECT JOB_EXECUTION_ID, STEP_NAME, STATUS, READ_COUNT, WRITE_COUNT,
       COMMIT_COUNT, ROLLBACK_COUNT, SKIP_COUNT
 FROM BATCH_STEP_EXECUTION ORDER BY STEP_EXECUTION_ID DESC;
```

基线应有源 25、汇总 25，`attendance-2026-10-19.csv` 共 26 行；Job 与四个 Step 应为 `COMPLETED`。抽查员工 1001：工作 480 分钟、加班 0 分钟。另选没有样例数据的日期运行，预期汇总 0、CSV 仅表头；此新日期会形成独立 JobInstance。

核对顺序：先找到与业务日期对应的 `JOB_INSTANCE_ID` / `JOB_EXECUTION_ID`，再查看它的 Step 行、业务表与文件。不能只看“应用进程正常退出”或“Scheduler 触发”日志。按本手册的元数据关联 SQL 定位参数，并结合[第 04 章](docs/04_transaction_restart_and_job_management.md)判断重启，避免把别的日期误判为本次执行。

## 3. 手动、定时与重复执行

手动执行：传入 `--app.batch.business-date=YYYY-MM-DD`。日期是业务日，不一定是机器当天；格式错误或缺失应报错，不偷偷使用当前日。同一日期 `COMPLETED` 后再次以相同参数启动，应被拒绝；业务表件数不应变化。

定时执行：只有在批准的单实例测试环境设置 `app.schedule.enabled=true` 后才启用。`@Scheduled(cron="0 0 2 * * *", zone="Asia/Tokyo")` 每日 02:00 触发，业务日期取日本时间前一天。教学中的每秒 cron 仅限短时间测试，不可留在常态配置。多实例会重复触发；本例依赖同一 JDBC JobRepository 对相同 JobInstance 的状态约束，但实际部署还需单一调度责任、平台级锁或等效协调，以及告警与运行窗口设计。若各实例连不同元数据库，则此防护失效。

## 4. 失败与恢复手顺

1. 保留原始日志、业务日期、JobExecution ID、失败 Step、异常摘要、件数；不先清除元数据。
2. 判断失败位置：`summarizeStep` 异常看输入第 13 条与已提交的 10 条；`csvStep` 异常看输出目录类型、权限、剩余临时文件及下游是否已取走正式文件。
3. 评估源数据是否在运行中变化。更正错误数据/路径后，以**原 Job 名称和原标识业务日期**重启；不用随机 `run.id` 强行新建 Instance。
4. 重启后核对 JobExecution 终态、已完成 Step 是否跳过、业务表唯一键与件数、CSV 头部/行数/内容。若 CSV 已被外部系统消费，先确认下游处理状态，不能只在本系统覆盖文件。
5. 若状态为 `STOPPED`、`STARTED` 或执行记录与进程实际状态不符，先人工调查，不直接再次发起。特别是节点异常终止，元数据未及时更新可能阻止正常重启。

默认对无效考勤采取失败处理；仅在教学实验明确启用 `--app.batch.skip-invalid=true` 时，`InvalidAttendanceException` 最多 Skip 2 条，`TransientDataAccessException` 最多 Retry 3 次。生产中的可跳过异常清单必须由业务规格确定，不能照搬教学开关。

## 5. 常见症状

| 症状 | 先查 | 处理方向 |
| --- | --- | --- |
| 启动时报 `BATCH_*` 不存在 | JDBC Starter、当前数据库、首次初始化记录 | 仅对独立空库初始化元数据 |
| MySQL 连接失败 | URL、账号权限、服务、网络 | 修正个人测试环境；不打印密码 |
| Job `FAILED`、汇总 10 条 | `summarizeStep` 与第 13 条、回滚记录 | 修复输入，再用同参数重启 |
| 汇总 25 条但 CSV 缺失 | `csvStep`、输出路径、权限、临时文件 | 修复路径，确认下游后同参数恢复 |
| 同日再次启动被拒绝 | 该 JobInstance 已 `COMPLETED` | 属预期重复保护；新日期或批准的订正流程 |
| 定时触发却无结果 | Scheduler 日志、JobExecution、业务表、CSV | 逐层定位，不把触发当成功 |
| 多节点重复触发 | 部署实例数、是否共用 Repository | 统一调度责任与共享元数据 |

## 6. 清理与交接

先保存所需证据，再只清理个人实验输出目录和个人实验数据库；不得按模糊路径递归删除。需要重建时重新创建**个人专用库**并按第 1 节完整执行，不能删除共享或主线数据。交接记录建议包含：対象ジョブ、業務日付、Execution ID、開始/終了時刻、処理/異常件数、CSV 路径、下游状态、再実行可否、担当者与待确认事项。故障报告样例在 [教师参考](exercises/instructor_answers.md)。
