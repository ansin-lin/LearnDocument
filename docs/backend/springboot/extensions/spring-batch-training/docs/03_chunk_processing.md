# 03 使用 Chunk 读取、计算并写入考勤

## 本章学习目标

能跟踪一条考勤从 `ItemReader` 到 `ItemProcessor`、`ItemWriter` 的过程，解释 25 条数据为何形成 10/10/5 三个 Chunk，并用日志、StepExecution 与业务表核对结果。

## 前置知识

完成[第 02 章](02_batch_architecture_and_tasklet.md)的最小 Job/Tasklet 实验，了解 MyBatis Mapper 和数据库事务。本章回到原有 [attendanceSummaryJob](../examples/attendance-batch/src/main/java/com/example/attendance/BatchConfig.java)：它在 `validateStep` 后执行 `summarizeStep`，随后才导出 CSV、记录结果。开始状态是独立测试库含 `2026-10-19` 的 25 条虚构考勤，汇总表为空。

## 实际业务场景

如果将百万条记录一次读进 `List`，内存可能不足；如果每写一条就提交，事务和数据库往返又过于频繁。Chunk 将“读一组、算一组、写一组、提交”作为处理单元。本例每组至多 10 条，25 条形成 10、10、5；本章先观察正常路径，第 13 条故障留给[第 04 章](04_transaction_restart_and_job_management.md)。

## 核心概念

`ItemReader<AttendanceRecord>.read()` 每次向框架提供一条输入，返回 `null` 表示没有下一条；`ItemProcessor<AttendanceRecord,DailySummary>.process()` 校验、计算并转换；`ItemWriter<DailySummary>.write(Chunk)` 接收已组成的一组输出。Batch 调用这三个接口，开发者实现方法并交给 Step。`chunk(10)` 是正常情况下的事务提交间隔，不是 MyBatis `LIMIT 10`，也不保证每条 SQL 一次读取 10 行。本例 Reader 的 SQL 恰好每次查一条，这只是该 Reader 的实现选择。

`Tasklet` 适合参数检查、文件准备等一次性工作；`Chunk` 适合同一规则处理多条记录。`readCount` 表示 Step 读取件数，`writeCount` 表示成功写入件数；如果 Processor 返回 `null` 过滤一条或发生 Skip，两者就可能不同，不能只比较它们断言数据库正确。

Reader 使用按 `business_date`、唯一 `id` 递增的 keyset 查询。`lastId` 放在 Reader 内存，`ItemStreamReader.open/update` 则利用 `ExecutionContext` 恢复检查点。稳定排序是必要条件；若运行期间有人新增/修改了源记录，仅有 `lastId` 并不能形成一致性快照。Writer 通过 Spring 管理的 MyBatis Mapper 写入，不另开独立 SqlSession，以便参与 Chunk 事务。

## 执行流程

先用 1～5 的独立内存实验观察框架调用；它不访问考勤业务表：

```mermaid
flowchart LR
    A[JobLauncher] --> B[numberChunkJob]
    B --> C[numberChunkStep]
    C --> D[Reader 依次返回 1 到 5 最后返回 null]
    D --> E[Processor 各乘 10]
    E --> F[Writer 收到 10/20、30/40、50]
    F --> G[JobRepository 记录状态]
```

| 顺序 | 执行者 | 执行内容 | 结果 |
| --- | --- | --- | --- |
| 1 | 测试代码 | 调用 `JobLauncher.run` | 启动内存教学 Job |
| 2 | Spring Batch | 调用 Reader、Processor | 取得 1～5，转换为 10～50 |
| 3 | Spring Batch | 满 2 个输入或遇到结束 | 调用 Writer，得到 2/2/1 三组 |
| 4 | Spring Batch | 完成 Step 并更新 Repository | 记录 `readCount=5`、`writeCount=5` |

```text
summarizeStep 启动 → Reader.open(检查点)
第 1 组：read 1～10 → process 1～10 → write 10 → 提交、保存 lastId
第 2 组：read 11～20 → process 11～20 → write 10 → 提交、保存 lastId
第 3 组：read 21～25 → process 21～25 → write 5  → 提交、保存 lastId
Reader.read() 返回 null → 处理余项并结束 Step → csvStep → resultStep
```

这是**正常处理的简化时间线**。实际框架在探测结束、过滤、重试或回滚时可能多调用 Reader/Processor，不能据此推断“每次 read 必定紧接一次 write”。要看框架记录与业务表的最终证据。

## 关键类与接口

| 源文件 | 输入、输出与职责 | 重要边界 |
| --- | --- | --- |
| [AttendanceRecord.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceRecord.java) | 输入对象：ID、员工、日期、上下班与休息分钟 | 只用虚构 ID，不输出姓名 |
| [DailySummary.java](../examples/attendance-batch/src/main/java/com/example/attendance/DailySummary.java) | 输出对象：员工、日期、工作和加班分钟 | 与汇总表字段一致 |
| [AttendanceReader.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceReader.java) | MyBatis 按日期/最后 ID 读取；保存检查点 | `null` 结束，`lastId` 要在提交时持久化 |
| [AttendanceProcessor.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceProcessor.java) | 验证时间、计算工作与加班 | 非法数据抛 `InvalidAttendanceException` |
| [AttendanceWriter.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceWriter.java) | 遍历 Chunk 调 Mapper 写入 | 不自己开脱离 Step 的事务 |
| [AttendanceMapper.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceMapper.java) | 参数化 SQL 与 UPSERT | `id` 排序唯一，汇总键为员工+日期 |
| [BatchConfig.java](../examples/attendance-batch/src/main/java/com/example/attendance/BatchConfig.java) | 把 Reader/Processor/Writer 接入 `summarizeStep` | Chunk Size 10 与事务管理器 |

Spring Batch 6 的 `ItemReader`、`ItemProcessor`、`ItemWriter`、`Chunk`、`ExecutionContext` 位于 `org.springframework.batch.infrastructure.item` 包；Job/Step 的 Builder 使用当前项目实际编译通过的 API，不照搬旧教程。MyBatis 的 `#{...}` 负责参数绑定，不将日期拼接进 SQL。

## 代码示例

### 先运行内存 Chunk

以下是**完整教学配置类** [NumberChunkLessonConfig.java](../examples/attendance-batch/src/main/java/com/example/attendance/NumberChunkLessonConfig.java)。只在 `app.lesson.chunk-enabled=true` 的隔离测试中创建三个数字 Job；它们分别观察 Chunk Size 2、Chunk Size 5 和过滤数字 3，不属于正式考勤 Job。`ItemReader` 的 `null` 结束输入；Writer 只把每组结果存入内存记录器，**不证明数据库业务事务提交**。

```java
package com.example.attendance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnProperty(prefix = "app.lesson", name = "chunk-enabled", havingValue = "true")
public class NumberChunkLessonConfig {
    @Bean
    NumberChunkRecorder numberChunkRecorder() {
        return new NumberChunkRecorder();
    }

    @Bean
    Job numberChunkJob(JobRepository repository, @Qualifier("numberChunkStep") Step step) {
        return new JobBuilder("numberChunkJob", repository).start(step).build();
    }

    @Bean
    Job numberChunkFiveJob(JobRepository repository, @Qualifier("numberChunkFiveStep") Step step) {
        return new JobBuilder("numberChunkFiveJob", repository).start(step).build();
    }

    @Bean
    Job numberFilterJob(JobRepository repository, @Qualifier("numberFilterStep") Step step) {
        return new JobBuilder("numberFilterJob", repository).start(step).build();
    }

    @Bean
    Step numberChunkStep(JobRepository repository, PlatformTransactionManager transactionManager,
                         NumberChunkRecorder recorder, @Qualifier("numberChunkReader") ItemReader<Integer> reader) {
        return numberStep("numberChunkStep", 2, false, repository, transactionManager, recorder, reader);
    }

    @Bean
    Step numberChunkFiveStep(JobRepository repository, PlatformTransactionManager transactionManager,
                             NumberChunkRecorder recorder, @Qualifier("numberChunkFiveReader") ItemReader<Integer> reader) {
        return numberStep("numberChunkFiveStep", 5, false, repository, transactionManager, recorder, reader);
    }

    @Bean
    Step numberFilterStep(JobRepository repository, PlatformTransactionManager transactionManager,
                          NumberChunkRecorder recorder, @Qualifier("numberFilterReader") ItemReader<Integer> reader) {
        return numberStep("numberFilterStep", 2, true, repository, transactionManager, recorder, reader);
    }

    @Bean
    @StepScope
    ItemReader<Integer> numberChunkReader() {
        return newNumberReader();
    }

    @Bean
    @StepScope
    ItemReader<Integer> numberChunkFiveReader() {
        return newNumberReader();
    }

    @Bean
    @StepScope
    ItemReader<Integer> numberFilterReader() {
        return newNumberReader();
    }

    private ItemReader<Integer> newNumberReader() {
        AtomicInteger next = new AtomicInteger(1);
        return () -> {
            int value = next.getAndIncrement();
            return value <= 5 ? value : null;
        };
    }

    private Step numberStep(String name, int chunkSize, boolean filterThree,
                            JobRepository repository, PlatformTransactionManager transactionManager,
                            NumberChunkRecorder recorder, ItemReader<Integer> reader) {
        ItemProcessor<Integer, Integer> processor =
                value -> filterThree && value == 3 ? null : value * 10;
        ItemWriter<Integer> writer = chunk -> recorder.record(name, chunk.getItems());
        return new StepBuilder(name, repository)
                .<Integer, Integer>chunk(chunkSize)
                .reader(reader).processor(processor).writer(writer)
                .transactionManager(transactionManager).build();
    }

    public static class NumberChunkRecorder {
        private final Map<String, List<List<Integer>>> groups = new ConcurrentHashMap<>();

        void record(String step, List<? extends Integer> values) {
            groups.computeIfAbsent(step, ignored -> new ArrayList<>())
                    .add(List.copyOf(values));
        }

        public List<List<Integer>> groups(String step) {
            return List.copyOf(groups.getOrDefault(step, List.of()));
        }
    }
}
```

`numberChunkJob` 的 Step 以 2 为 Chunk 大小。Spring 容器在创建 Step 时注入 Reader 的代理；`@StepScope` 使真正的 `AtomicInteger` Reader 在**每次 StepExecution** 中重新创建，从 1 开始。框架执行 Step 时才调用 `read()`；Processor 每条乘 10，Writer 在每组准备好后被调用。测试 [NumberChunkLessonIntegrationTest.java](../examples/attendance-batch/src/test/java/com/example/attendance/NumberChunkLessonIntegrationTest.java) 分别启动三项，并用新的标识参数独立再运行一次 `numberChunkJob`，检查第二次仍读取 1～5；记录器是容器级内存对象，因此两次输出会累计。原样重跑已 `COMPLETED` 的同参数 JobInstance 会被拒绝，这与 Reader 是否重置是两回事。过滤实验的 `readCount=5`、`writeCount=4`；第 3 个输入不占 Writer 输出位置，但它所在读取组仍结束，不会跨组重新凑数。**此教学 Reader 仅实现 `ItemReader`，不保存 `ExecutionContext`，故不支持失败后的持久化 Restart；独立新运行正常不等于可恢复。**

| 对照 | 数字内存 Reader | 正式 `AttendanceReader` |
| --- | --- | --- |
| 创建时机与输入 | `@StepScope`：每次 StepExecution 新建，从 1 到 5 | `@StepScope`：从标识参数 `businessDate` 与 MyBatis 查询取考勤 |
| 结束标志 | 数字超过 5 返回 `null` | 查不到下一条返回 `null` |
| 执行位置 | `AtomicInteger` 只在本次内存中 | `lastId` 在内存中，提交检查点写入 `ExecutionContext` |
| 失败后恢复 | 不支持持久化 Restart | `open/update` 恢复已提交位置；仍须核对业务数据和 CSV |

数字实验完整配置类的逐行索引（行号从 `package` 起；空行、单独的右括号只界定代码范围）：

| 行 | 功能与首次出现的对象/方法 |
| --- | --- |
| 1、3～21 | 包名与导入；`AtomicInteger` 是本次 Reader 的可变读取位置，`ItemReader/Processor/Writer` 分别提供输入、转换、成组输出。 |
| 23～25 | `@Configuration` 与条件注解让配置只在教学开关开启时生效；随后声明配置类。 |
| 26～29 | `@Bean numberChunkRecorder()` 返回一个内存记录器，不写业务表。 |
| 31～34 | 注册 Size 2 Job，注入 Repository 与名为 `numberChunkStep` 的 Step，`JobBuilder` 返回定义。 |
| 36～39 | 注册 Size 5 Job，接 `numberChunkFiveStep`，不立即运行。 |
| 41～44 | 注册过滤 Job，接 `numberFilterStep`。 |
| 46～50 | 注册第一个 Step；`@Qualifier` 选专属 Reader；`numberStep(...,2,false,...)` 设置组大小 2、不过滤。 |
| 52～56 | 注册第二个 Step；使用另一个 Reader，组大小改为 5。 |
| 58～62 | 注册过滤 Step；组大小 2、`filterThree=true`。 |
| 64～68 | `@Bean @StepScope` 为第一次数字 Step 注册 Reader；每次执行调用 `newNumberReader()` 得到新计数。 |
| 70～74 | 为 Size 5 Step 注册另一 Reader，不共用读取位置。 |
| 76～80 | 为过滤 Step 注册第三个 Reader。 |
| 82～83 | `newNumberReader()` 建 `AtomicInteger(1)`，初始下一项为 1。 |
| 84～87 | Lambda 实现 `read()`：`getAndIncrement()` 取当前数后自增，1～5 返回整数，超过 5 返回 `null`。 |
| 90～92 | `numberStep` 接收名称、组大小、过滤开关及框架对象，返回一个 Step 定义。 |
| 93～94 | Processor 对值 3 在过滤模式返回 `null`，其余值乘 10。 |
| 95 | Writer 取得本组输出 `chunk.getItems()`，连同 Step 名交给记录器。 |
| 96～99 | `StepBuilder` 命名 Step，设置 Chunk 大小、读/算/写三组件和事务管理器。 |
| 100 | `build()` 返回 Step 定义，不立即处理数字。 |
| 102～103 | 声明结果记录器，`ConcurrentHashMap` 按 Step 名保存各组列表；教学测试串行运行。 |
| 105～108 | `record` 首次遇到 Step 名时创建列表，`List.copyOf(values)` 保存本组结果副本。 |
| 110～112 | `groups(step)` 返回该 Step 的不可修改快照；没有记录时返回空列表。 |

`@StepScope` 管 Reader **对象的创建时机**，不是恢复点持久化；`ItemReader.read()` 返回 `null` 与 Processor 返回 `null` 含义不同，前者结束输入，后者过滤当前输入。

### 再进入考勤业务

以下为工程中的**完整业务对象定义**，分别保存于 `AttendanceRecord.java` 与 `DailySummary.java`（两个独立文件）：

```java
public record AttendanceRecord(long id, long employeeId, LocalDate businessDate,
                               LocalDateTime startAt, LocalDateTime endAt, int breakMinutes) {}

public record DailySummary(long employeeId, LocalDate businessDate,
                           long workedMinutes, long overtimeMinutes) {}
```

`AttendanceRecord` 的前后两行分别列出源记录的 ID、员工、业务日、上下班时刻和休息分钟；`DailySummary` 的前后两行列出汇总键（员工、业务日）和工作/加班分钟。Java `record` 自动提供 `item.id()` 一类读取方法，但不会自动查询或写库。完整文件各有 `package com.example.attendance;` 与 `java.time` import，以上是并列展示的两个 record 声明，不是要粘贴进同一个文件。以下是**完整业务 Reader 类** [AttendanceReader.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceReader.java)：

```java
package com.example.attendance;

import java.time.LocalDate;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

public class AttendanceReader implements ItemStreamReader<AttendanceRecord> {
    private static final String LAST_ID = "attendanceReader.lastId";
    private final AttendanceMapper mapper;
    private final LocalDate businessDate;
    private long lastId;

    public AttendanceReader(AttendanceMapper mapper, LocalDate businessDate) {
        this.mapper = mapper;
        this.businessDate = businessDate;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        lastId = executionContext.getLong(LAST_ID, 0);
    }

    @Override
    public AttendanceRecord read() {
        AttendanceRecord item = mapper.findNext(businessDate, lastId);
        if (item != null) {
            lastId = item.id();
        }
        return item;
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_ID, lastId);
    }

    @Override
    public void close() {
        // No open cursor: each read is a keyset query.
    }
}
```

Reader 逐行解读（行号从本类 `package` 起）：

| 行 | 功能与首次出现的对象/方法 |
| --- | --- |
| 1、3～5 | 包名与导入；`ItemStreamReader` 在普通 `ItemReader` 之外提供 `open/update/close` 检查点生命周期。 |
| 7～11 | 类声明与字段；`LAST_ID` 是 Context 中稳定的键；`mapper` 负责 SQL；`businessDate` 是本次输入范围；`lastId` 是当前内存读取位置。 |
| 13～16 | 构造器保存容器注入的 Mapper 和业务日期，不在 Reader 内计算“今天”。 |
| 18～21 | `@Override open(context)` 在读取前被框架调用；`getLong(LAST_ID,0)` 从已保存 Context 恢复位置，首次没有值时取 0。 |
| 23～30 | 框架反复调用 `read()`；`findNext` 查日期内 ID 大于 `lastId` 的下一行；找到则更新内存 ID，找不到返回 `null` 告知结束。 |
| 32～35 | `update(context)` 将当前位置放入 Context，供成功提交的检查点保存；调用到这里不代表外部 CSV 已成功。 |
| 37～40 | `close()` 是接口生命周期方法；此实现每次查询一行、未保持游标，所以不需关闭游标资源。 |

`AttendanceReader` 由 [BatchConfig.attendanceReader](../examples/attendance-batch/src/main/java/com/example/attendance/BatchConfig.java) 的 `@StepScope` Bean 在 Step 启动时创建，`businessDate` 从 JobParameters 注入并解析为日期；不是应用启动时固定取今天。Batch 先调 `open`，再调 `read` 直到 `null`，提交检查点时调 `update`，结束时调 `close`。`lastId` 的内存值与持久化 `ExecutionContext` 不同；异常后只能从上次成功保存的位置恢复。

以下是**完整业务 Processor 类** [AttendanceProcessor.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceProcessor.java)：

```java
package com.example.attendance;

import java.time.temporal.ChronoUnit;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class AttendanceProcessor implements ItemProcessor<AttendanceRecord, DailySummary> {
    @Override
    public DailySummary process(AttendanceRecord item) {
        long minutes = ChronoUnit.MINUTES.between(item.startAt(), item.endAt());
        if (minutes <= 0 || item.breakMinutes() < 0 || item.breakMinutes() >= minutes) {
            throw new InvalidAttendanceException(item.id(), "invalid time or break");
        }
        long worked = minutes - item.breakMinutes();
        return new DailySummary(item.employeeId(), item.businessDate(),
                worked, Math.max(0, worked - 480));
    }
}
```

Processor 逐行解读：1～5 行声明包与依赖；7～8 行 `@Component` 使 Spring 创建对象，`ItemProcessor<输入,输出>` 指明类型；9～11 行的 `process(item)` 由 Batch 对每条输入调用，`ChronoUnit.MINUTES.between` 计算上下班相差分钟；12～14 行拒绝零/负时长及非法休息分钟并抛出业务异常；15 行扣除休息；16～17 行构造 `DailySummary`，`Math.max(0, worked-480)` 保证加班不为负。异常时没有返回汇总对象，不能把这条当成功输出。正常 09:00～18:00 减 60 分钟休息，工作 480 分钟，加班 0。

以下是**完整业务 Writer 类** [AttendanceWriter.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceWriter.java)：

```java
package com.example.attendance;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class AttendanceWriter implements ItemWriter<DailySummary> {
    private final AttendanceMapper mapper;

    public AttendanceWriter(AttendanceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void write(Chunk<? extends DailySummary> chunk) {
        for (DailySummary summary : chunk) {
            mapper.upsertSummary(summary);
        }
    }
}
```

Writer 逐行解读：1～5 行声明包、`Chunk`（本组输出容器）、`ItemWriter` 接口与 Spring 注解；7～9 行 `@Component` 使容器创建 Writer 并保存 Mapper 字段；11～13 行构造器接收并保存 Spring 注入的 Mapper；15～16 行由框架调用 `write(chunk)`；17～19 行遍历本组每个 `DailySummary` 并调用 MyBatis `upsertSummary`，20～21 行结束方法和类。调用 `write` **不等于已提交**：若本组后续失败，Step 的事务管理器回滚这一组。输入 09:00～19:00、休息 60 分钟时，计算为 `600-60=540`，加班 `60`。`480` 只是本练习阈值，不代表普遍劳动法规。

Mapper 中的 MySQL 8 关键查询和写入如下；`@ConstructorArgs` 的结果映射、全部方法及 import 在[完整 Mapper](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceMapper.java)：

```sql
SELECT id, employee_id, business_date, start_at, end_at, break_minutes
FROM attendance_records
WHERE business_date = #{businessDate} AND id > #{afterId}
ORDER BY id
LIMIT 1;

INSERT INTO attendance_daily_summary
    (employee_id, business_date, worked_minutes, overtime_minutes)
VALUES (#{employeeId}, #{businessDate}, #{workedMinutes}, #{overtimeMinutes})
ON DUPLICATE KEY UPDATE
    worked_minutes = VALUES(worked_minutes),
    overtime_minutes = VALUES(overtime_minutes);
```

SQL 逐行看：查询语句第 1 行只选六个输入列，第 2 行指定源表，第 3 行绑定日期与上次 ID，第 4 行稳定排序，第 5 行限取下一条；写入语句第 1 行指定汇总表，第 2 行列出四个字段，第 3 行绑定 `DailySummary` 属性，第 4 行声明主键冲突时更新，第 5～6 行分别更新工时与加班分钟。`VALUES(...)` 在此是现有 MySQL 工程写法，H2 兼容模式通过不等于已在 MySQL 8 实测。

这两段是 **MyBatis 注解内的 MySQL SQL 片段**，`#{...}` 由 MyBatis 绑定；不能原样粘贴到 MySQL 客户端。汇总表的主键为 `(employee_id,business_date)`，重复写同一员工同日记录会更新而不是新增。Chunk Step 的关键配置：

员工 1001 的实际流向如下；日期、ID 与 25 条[样例数据](../sql/sample_data.sql)一致：

```text
attendance_records：id=1, employee_id=1001, business_date=2026-10-19,
                    09:00 → 18:00, break_minutes=60
  → AttendanceReader.read：返回 AttendanceRecord
  → AttendanceProcessor.process：540 - 60 = 480，max(0, 480 - 480) = 0
  → AttendanceWriter.write：调用 AttendanceMapper.upsertSummary
  → attendance_daily_summary：(1001, 2026-10-19, 480, 0)
```

```java
return new StepBuilder("summarizeStep", repository)
        .<AttendanceRecord, DailySummary>chunk(10)
        .reader(reader).processor(processor).writer(writer)
        .transactionManager(transactionManager)
        .build();
```

Step 配置逐行看：第 1 行 `StepBuilder` 指定 Step 名及保存执行记录的 Repository；第 2 行泛型说明输入/输出类型、`chunk(10)` 设事务处理组大小；第 3 行按顺序接 Reader/Processor/Writer；第 4 行指定事务管理器；第 5 行 `build()` 返回 Step 定义，**没有立即读取数据**。这段是 [BatchConfig.java](../examples/attendance-batch/src/main/java/com/example/attendance/BatchConfig.java) 的正常模式片段；正式代码还包含仅在实验开关下启用的 Skip/Retry 分支，见第 04 章。

`@StepScope` Bean 的关键代码片段说明日期来源；它不是 `AttendanceReader` 类的一部分：

```java
@Bean
@StepScope
AttendanceReader attendanceReader(AttendanceMapper mapper,
                                  @Value("#{jobParameters['businessDate']}") String rawDate) {
    return new AttendanceReader(mapper, LocalDate.parse(rawDate));
}
```

此片段逐行看：第 1 行 `@Bean` 注册 Reader；第 2 行 `@StepScope` 延迟到每次 StepExecution 创建目标对象；第 3 行声明方法并由容器注入 Mapper；第 4 行 `@Value` 从本次 JobParameters 取必填日期文本；第 5 行解析为 `LocalDate` 后调用构造器并返回 Reader。缺失或非法日期会导致创建/执行失败，不会自动改用今天。

## 代码执行说明

1. 框架启动 `summarizeStep` 后，先调用 Reader 的 `open` 取得上次检查点；首次为 0。
2. Batch 调用 `read`；Reader 以 `businessDate` 和 `lastId` 执行 Mapper 查询。拿到一行就更新内存中的最后 ID；没有下一行则返回 `null`。
3. 每条输入进入 `process`。时间无效时抛业务异常；正常时生成 `DailySummary`。框架收集输出，累计到 10 条或遇到输入结束。
4. 框架调用 `write(Chunk)`；Writer 经 Spring 管理的 Mapper 逐条 UPSERT。事务管理器提交当前 Chunk，框架保存必要的 `ExecutionContext` 与 Step 计数；随后开始下一组。
5. Batch 结束 `summarizeStep` 后才继续 `csvStep`。CSV 不属于这些 Chunk 的 JDBC 事务；不能用“数据库已提交”推断文件也交付成功。

| 当前组 | Reader 成功返回 | Processor 输出 | Writer 接收 | 事务与可见结果 |
| --- | ---: | ---: | ---: | --- |
| 第 1 组 | 10 | 10 | 10 | Writer 正常返回后提交；汇总表累计 10 |
| 第 2 组 | 10 | 10 | 10 | 提交后累计 20 |
| 第 3 组 | 5 | 5 | 5 | 输入结束后写出并提交；最终 25 |

上表是无过滤、无失败的基线。Batch 的 `readCount`、`writeCount` 记录处理事实，数据库行数还须独立查询；Writer 已收到一组不等于事务最终提交。若第 13 条在 Processor 失败，第二组的未提交修改回滚，第一组保留；详见[第 04 章](04_transaction_restart_and_job_management.md)。

## 逐步操作

1. 在 `examples/attendance-batch` 目录运行 `mvn -q -Dtest=AttendanceBatchIntegrationTest test`；这个 H2 测试检查 25 条写入和 `writeCount=25`，不依赖 MySQL 密码。
2. 在**可重建的工程副本**中，给 Reader 读取成功、Processor 开始、Writer 接收 Chunk 分别加只记录虚构 `id` 或件数的临时日志。运行样例日期，应观察 Reader/Processor 的 `1～25` 与 Writer 的 `10、10、5`；不要输出真实个人资料。还可在 Chunk 提交回调后记录提交计数，但必须区分“Writer 已调用”与“事务已提交”；最终以 StepExecution 的 `COMMIT_COUNT` 和业务表核对。实验后撤销临时日志。
3. 只在实验副本把 `chunk(10)` 改成 `chunk(5)`，预期 Writer 收到五组 5 条；复跑测试并查询业务汇总，完成后恢复 10。
4. 使用 [AttendanceProcessorTest.java](../examples/attendance-batch/src/test/java/com/example/attendance/AttendanceProcessorTest.java)核对合法加班记录：`worked=540`、`overtime=60`。在个人数据副本增加一条新的虚构考勤时，需同步更新源数、汇总数与 CSV 行数的预期；不能直接改共享样例库。
5. 运行 `mvn -q -Dtest=NumberChunkLessonIntegrationTest test`。测试仅在内存收集 Writer 分组，检查 Size 2、Size 5、Processor 过滤、Reader 返回 `null` 后正常结束，以及新标识参数下第二次独立运行仍读取 1～5；考勤汇总表仍为 0。这个教学实验的 Writer 分组不能代替业务数据库提交证据。

## 运行结果与验证

当前 H2 集成测试实测基线写入 25 条、`writeCount=25`、CSV 26 行。数字教学测试断言 Writer 的 2/2/1、Size 5 和过滤输出；考勤 Job 的 10/10/5 Writer 调用次数仍须通过临时日志观察，**不能把数字测试当成该业务断言**。可在个人 MySQL 8 库用[运行手册](../runbook.md)的只读 SQL 对照源表、汇总表和 `BATCH_STEP_EXECUTION`；MySQL 实际运行结果仍须由学员留下证据。

## 常见错误

- `read()` 永远不返回 `null` 会重复读取；只按非唯一字段排序，重启时可能漏读或重复。
- 将 `chunk(10)` 当成 SQL 每次查询 10 条。本 Reader 用 `LIMIT 1`，两者是不同层次。
- 认为 `readCount=writeCount` 必然成立。过滤、Skip 或异常会使计数不同；还需对照业务行。
- Writer 自行打开独立连接并提交，会让 Chunk 回滚失去对业务写入的控制。
- 只记录 `write` 调用日志就认定数据库已提交；事务仍可能回滚。

## 日本项目中的注意事项

已有项目可能用游标、分页或 keyset Reader；本课程只实际实现 keyset。读懂既有 SQL 时，检查日期条件、唯一排序键、输入是否在运行中被改写。Chunk Size 要结合锁、事务时长和内存实测，不能机械地越大越好。日志保留业务日期、虚构 ID 和件数即可；正式项目应避免输出个人考勤明细。

## 学员练习

1. 基础理解：说明 Reader 返回 `null`、Processor 返回 `null`、Writer 收到 5 条分别意味着什么。
2. 代码阅读：从 `BatchConfig.summarizeStep` 追到 Reader、Processor、Writer 与 Mapper，标出读取日期、稳定排序、计算和 UPSERT 的位置。
3. 实际操作：在独立副本把 Chunk Size 改为 5，记录 Writer 五次收到的件数、最终 `writeCount` 与汇总表件数；恢复为 10。
4. 执行判断：阅读 `NumberChunkLessonIntegrationTest` 中第二次使用新标识参数启动的断言。说明为什么两次都读到 5 件，却不能据此证明同一失败 JobInstance 可以 Restart；再指出若去掉 `@StepScope`，第二次可能观察到什么。
5. 故障预告：把第 13 条 `break_minutes` 改成 600，只观察首轮失败后汇总已有多少行，不在本章自行加随机参数重跑。

## 本章验收标准

能说明 25 条的 10/10/5 正常处理链、`null` 结束、ExecutionContext 检查点、MyBatis Mapper 参与事务的条件，并用执行记录和业务表双重确认结果。

## 本章总结

Chunk 已能分组处理数据；若第 13 条异常，前面哪些组已提交、同一业务日期如何恢复，需要先确定任务身份和事务边界。下一章处理这些问题。
