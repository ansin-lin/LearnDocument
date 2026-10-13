# 综合项目：月度员工变更账票

## 交付目标与开始状态

在[第18章完成运行检查的员工管理API](../mainline/18_deployment_operations_check.md)的独立副本中，实现管理员申请指定月份的员工变更账票、查询处理状态和下载XLSX。已有`employees`、`employee_change_logs`、`app_users`及登录/权限链保持不变。先通过[Redis](redis.md)、[Spring Batch](spring-batch.md)、[账票](reporting.md)的单点练习，再开始整合；不要在主线仓库中直接做破坏性实验。

完成标准：从干净的个人环境部署后，给定同一组履历能得到件数、顺序和时间均正确的文件；并发同月请求只产生一项有效申请；失败有证据、可恢复；Redis 停机不损害申请和下载；未授权者不能获取文件。所有账号、履历和截图用虚构或脱敏数据。

## 1. 固定规格

| 项目 | 规则 |
| --- | --- |
| 申请 | `POST /reports/monthly`，JSON `{"month":"2026-09"}`；仅管理员；返回 202、`reportId`、`PENDING` |
| 查询 | `GET /reports/{id}`；返回 ID、月份、状态、件数、失败摘要、创建/完成时间；管理员可看全部，其他角色默认无权 |
| 下载 | `GET /reports/{id}/file`；仅 `COMPLETED` 且有权限时返回 XLSX；未完成 409 |
| 期间 | 日本业务本地时间 `[当月1日 00:00, 次月1日 00:00)`；`created_at` 按第13章的 DATETIME 约定解释 |
| 数据 | `employee_change_logs` 的 `id, employee_id, action, detail, created_at`；按时间、ID 升序 |
| 空月 | 正常完成，表头保留，件数 0 |
| 重复 | 同月同格式版本只允许一个有效申请；已完成直接返回已有 ID，失败可显式重试，不生成第二份“有效结果” |
| 输出 | `monthly-employee-changes-YYYY-MM-v1.xlsx`，工作表“员工变更”，内容规则遵照[账票规格](reporting.md) |

若实际项目需要“历史快照名称”“申请者仅可看本人账票”或“报表截止日补录”，先改规格、数据模型与测试，不用实现者临场猜测。

## 2. 数据模型和迁移

在个人练习库通过一次受控迁移增加以下表；不删除主线表或已有数据。Batch 自身的 `BATCH_*` 元数据按[上一章](spring-batch.md)初始化。

```sql
CREATE TABLE report_requests (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  report_type VARCHAR(40) NOT NULL,
  report_month CHAR(7) NOT NULL,
  format_version VARCHAR(10) NOT NULL,
  status VARCHAR(20) NOT NULL,
  row_count BIGINT NOT NULL DEFAULT 0,
  file_name VARCHAR(200) NULL,
  file_size BIGINT NULL,
  file_sha256 CHAR(64) NULL,
  error_code VARCHAR(40) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  UNIQUE KEY uk_report_period (report_type, report_month, format_version),
  CONSTRAINT ck_report_status CHECK
    (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);

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

`report_requests` 是权威状态；Redis 不能替代它。唯一键承担跨实例的重复申请防线，Controller 预查询只能改善提示，挡不住并发。对同月两个并发 INSERT，捕获唯一约束冲突后**重新查询既有申请并按规则返回**；不要把所有数据库异常都解释为重复申请。`created_by` 指向现有账号 ID；账号删除或改名后的审计保留规则需在真实项目另定。

## 3. 分层与文件位置

在原有 `com.example.employee` 包下增设：

```text
report/api/MonthlyReportController.java       参数、HTTP状态、下载响应
report/service/ReportRequestService.java      权限后的申请/状态规则与事务
report/mapper/ReportRequestMapper.java        report_requests 数据访问
report/batch/MonthlyChangeJobConfig.java      Job、Step、Reader、Writer
report/file/MonthlyWorkbookWriter.java        只负责生成及校验 XLSX
report/file/ReportFileStore.java              专用目录、原子发布、下载路径
report/cache/ReportStatusCache.java           可选的短 TTL 状态副本
```

Controller 不直接调用 Mapper、Redis 或 POI。API 入参用 `YearMonth` 解析并限制合理范围，例如当前月及过去 24 个月；非法格式返回 400。第16章现有权限检查仍应覆盖 Service，不能仅靠 URL 规则。申请事务只负责创建/取得 `PENDING` 记录，提交后再启动异步 Job；不要在未提交的事务里启动 Job，否则任务可能看不到申请记录。可由可靠的工作队列/调度器轮询 `PENDING` 并启动 Job；本地练习可在提交后触发执行，同时保留启动失败时的 `PENDING` 恢复扫描。不要使用无恢复保证的单次内存 `@Async` 就声称任务不会丢失。

状态流：

```text
PENDING ──领取任务──> RUNNING ──采集+生成+校验+发布成功──> COMPLETED
   │                     └──任一不可恢复错误──> FAILED
   └──启动失败仍为 PENDING，巡检后重试
FAILED ──管理员显式重试──> PENDING（同一 reportId，记录新尝试）
```

领取任务用条件更新 `WHERE id=? AND status='PENDING'`，受影响行数为 1 才可启动；避免多个实例都执行。要处理进程在 `RUNNING` 崩溃的情况：记录尝试/心跳或从 Batch 元数据核对，人工或受控巡检将超时任务恢复，不能自动无限重跑。状态更新与文件发布跨数据库/文件系统，须按账票章节的临时文件、校验、原子移动、失败清理顺序执行；发现“文件存在但状态非完成”时先核对哈希与执行 ID，再恢复或清理。`FAILED` 只保存错误码/脱敏摘要，详细堆栈在受控日志。

## 4. Batch 与文件衔接

对申请 2026-09，保存不可变参数 `reportId`、`month`、`from=2026-09-01T00:00:00`、`to=2026-10-01T00:00:00` 和申请时确定的 `cutoffLogId`。采集 Step 按稳定顺序读履历、只写本 `reportId` 的 `report_change_rows`，每 100 条提交；唯一键使重启写入可幂等。`cutoffLogId` 限定本次报表可见范围，后补录数据是否纳入应由业务另定。采集完成后由独立步骤从暂存表排序读取，生成临时 XLSX，按件数和关键字段重新打开校验，再发布文件。不能在每个 chunk 向同一个 XLSX 追加并假设数据库回滚会撤销文件写入。

完成后查 `SELECT COUNT(*) FROM report_change_rows WHERE report_id=?` 与工作表数据行数相等；`row_count` 更新为该值。空月仍得到只有表头的有效文件。状态写入 MySQL 后，删除或更新 Redis 的短期状态键；缓存故障仅导致状态查询退回 MySQL。`COMPLETED` 状态的文件丢失不能返回空文件，应报警并返回明确错误，按恢复流程处理。

## 5. 启动、测试与验收

1. 在干净个人库执行第18章项目原有表迁移和本章新增迁移；创建Batch元数据表；准备9月边界、10月边界、同时间不同ID、空月等虚构样例。
2. 配置 MySQL、专用且可写的报表目录、可选 Redis。运行原有自动化测试，再运行 Batch 和账票测试。构建与 JAR 运行沿用主线打包规范；部署时核对服务、日志、端口、健康检查和回退步骤。
3. 管理员申请 9 月，记录 202 与 ID。轮询状态直到 `COMPLETED`，核对 `row_count`、SQL 明细、XLSX 表头/顺序/边界、文件 SHA-256 与下载响应。不要只看 HTTP 200。
4. 同月重复与并发申请，验证唯一业务申请；异月申请应独立。普通员工和匿名请求不可申请、看状态或下载。非法月份返回 400，未完成下载返回 409。
5. 在独立测试库注入第 3 条写入失败，核对 Batch 失败计数、数据库已提交块、`FAILED` 与无可下载文件；修复后按同一业务申请重试，验证无重复行。
6. 停 Redis：申请、状态查询与下载仍正确。停 MySQL 或写盘空间不足：不应伪报完成；恢复后核对残留 `.part` 文件与状态，再受控重试。做 0、3、1000 行和超过单工作表上限的容量策略测试。
7. 保留脱敏的接口证据、SQL 件数、Batch 执行 ID、测试报告、配置变量清单、报表样张/哈希、失败恢复记录、回退步骤。回退应用版本前先确认新表和已生成文件的兼容/保留策略，不随意删除业务成果。

结业交付不仅是“文件能下载”：接收者应能在新环境按步骤重建、执行、核对结果、定位失败并安全回退。若目标组织要求 PDF、邮件投递或定时夜间批量，把它们作为后续独立改修票，分别补规格、权限、失败补偿和验收，不塞进本次最小可交付成果。
