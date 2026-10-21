# 教师参考：评分要点与障害調査样例

本文件供教师使用，不嵌入 [学员任务书](assignments.md)。先核实学生自己的代码与数据库证据；下列数字以原始 25 条样例为基线，完成第 5 题追加后应改为 26 条。

## 知识与识读参考

1. `attendanceSummaryJob` 包含 `validateStep → summarizeStep → csvStep → resultStep`。`JobRepository` 保存 Job/Step 执行元数据；`AttendanceMapper` 访问独立业务表。`validateStep` 校验参数、统计输入；`summarizeStep` 以 Reader→Processor→Writer 汇总；`csvStep` 输出文件；`resultStep` 记录结果。
2. 一个 Job 名称加同一组标识参数只对应一个 JobInstance；两次失败、一次完成会形成三个 JobExecution。同参数再次启动已完成 Instance 抛出 `JobInstanceAlreadyCompleteException`。更换日期属于新业务实例；随机参数虽可制造新实例，却掩盖重复执行控制。
3. `@Scheduled` 负责时间触发，没有 Batch 持久执行历史、Chunk 恢复和业务幂等保证。必须核对 Batch 终态、业务件数与 CSV。多实例会各自触发，应由运维保证单调度实例或使用适当的分布式协调/平台调度；不能把本地线程锁当作跨实例方案。Oracle 组合应标出数据库调度器与 PL/SQL 过程的区别；Linux 组合应标出 cron 定时、Shell 传参/启动、Java 业务处理与数据库/文件结果，不能将“脚本执行完”直接等同业务成功。
4. 默认失败配置下，第 1 个 Chunk 的 10 条已经提交，第 2 个 Chunk 因第 13 条出错回滚，第 3 个尚未开始。Skip 用于经过规格允许的坏记录，Retry 用于有机会自行恢复的暂时故障，Restart 是保留同一 JobInstance 并从执行状态恢复；不能把重新传随机参数称为 Restart。

## 改修与测试参考

- 第 5 题：09:00–19:00 共 600 分钟，减休息 60 得 540，超过本练习 480 分钟阈值 60。应有 26 条汇总、27 行 CSV。数据库唯一键是 `(employee_id,business_date)`；需要使用新的虚构员工 ID 或确保同日无重复。
- 第 6 题：在 `validateStep` 取 `LocalDate.now(ZoneId.of("Asia/Tokyo"))` 比较，未来日期抛有明确消息的异常。测试应固定/注入时钟以避免跨日不稳定；若直接调用当前时间，必须避免用“今天”附近的测试日期。评估时优先认可可测试的时钟设计。
- 第 7、8 题：期望同时记录触发时间、JobExecution ID/状态、业务日期、业务表件数；Scheduler 调用成功不等于 Job `COMPLETED`。既存实现 [BatchTrigger.java](../examples/attendance-batch/src/main/java/com/example/attendance/BatchTrigger.java) 已做终态判断，可基于它扩展只读证据，不复制业务逻辑到 Scheduler。
- 第 9 题：示例中的 [AttendanceBatchIntegrationTest.java](../examples/attendance-batch/src/test/java/com/example/attendance/AttendanceBatchIntegrationTest.java) 将第 13 条置坏并验证失败后汇总 10 条、Reader 检查点为最后提交的源 ID，修复后同参数重启得 25 条。学生须给出 Step 读/写记录以及重复行检查。暂时性写入错误可参照 [RetryIntegrationTest.java](../examples/attendance-batch/src/test/java/com/example/attendance/RetryIntegrationTest.java)，无效行跳过另见 [SkipIntegrationTest.java](../examples/attendance-batch/src/test/java/com/example/attendance/SkipIntegrationTest.java)。
- 第 10 题：CSV 路径故障应发生在汇总提交后；修复并同参数重启时，已完成汇总 Step 不应被重新写入。正式 CSV 应完整出现，不能留下可被下游读取的半成品。外部文件系统不参与 JDBC 事务。
- 第 11 题：验证 `BATCH_JOB_EXECUTION` 确有记录，重复启动抛异常，并在异常前后核对汇总件数相同。测试中 H2 是快速隔离验证，MySQL 方言、权限与文件系统行为还须另测。

## 核验 SQL 与观察重点

教师在**学员自己的实验库**核查，不执行清表：

```sql
SELECT COUNT(*) FROM attendance_records WHERE business_date = '2026-10-19';
SELECT COUNT(*) FROM attendance_daily_summary WHERE business_date = '2026-10-19';
SELECT employee_id, worked_minutes, overtime_minutes
FROM attendance_daily_summary
WHERE business_date = '2026-10-19'
ORDER BY employee_id;
SELECT JOB_EXECUTION_ID, STATUS, START_TIME, END_TIME, EXIT_CODE
FROM BATCH_JOB_EXECUTION ORDER BY JOB_EXECUTION_ID DESC;
SELECT JOB_EXECUTION_ID, STEP_NAME, STATUS, READ_COUNT, WRITE_COUNT,
       COMMIT_COUNT, ROLLBACK_COUNT, SKIP_COUNT
FROM BATCH_STEP_EXECUTION ORDER BY STEP_EXECUTION_ID DESC;
```

如果学生的 Spring Batch 版本生成的元数据列与以上不同，以实际附带脚本为准。特别注意 `READ_COUNT` 在异常回滚时不能简单等同于“最终写入行数”。给分依据应以业务表、Step 终态和 CSV 三方一致为主。

## 障害調査报告样例

以下是**教学样例，不是本机真实事故或 MySQL 实测结果**。假设 `2026-10-20` 的 `csvStep` 因输出路径指向普通文件失败，`summarizeStep` 已完成 25 条。

> 発生日時：2026-10-21 02:00 JST（演習想定）  
> 対象ジョブ：attendanceSummaryJob / csvStep  
> 業務日付：2026-10-20  
> 発生事象：集計 25 件は DB に存在するが、CSV が未生成。Job は FAILED。  
> エラーログ：CSV 出力先ディレクトリ作成失敗。実際の例外名・時刻・Execution ID は現場ログから転記する。  
> 調査内容：JobExecution と StepExecution を照合し、validateStep・summarizeStep は COMPLETED、csvStep は FAILED と確認。業務表 25 件、正式 CSV なし、出力パスがファイルであることを確認。  
> 原因：出力先設定がディレクトリではなく通常ファイルを指していた。  
> 対応方針：設定を専用の書込可能ディレクトリに修正し、出力先に旧ファイル/一時ファイルがないか確認する。  
> 再実行可否：同じ識別パラメータで Restart 可。集計 Step の再実行有無と CSV 内容を実行後に照合する。新しいランダムパラメータで別 Instance を作らない。  
> 影響範囲：当該業務日付の CSV 連携先。DB 集計値は存在するが、下流への交付は未完了。下流取込済みかは別途確認。  
> 再発防止策：起動前の出力先検証、失敗通知、Step/ファイル双方の監視と運用手順への反映。

如果实际日志显示临时文件已存在或下游已读取正式文件，应先追加影响调查，不应套用“CSV 未生成”的模板结论。
