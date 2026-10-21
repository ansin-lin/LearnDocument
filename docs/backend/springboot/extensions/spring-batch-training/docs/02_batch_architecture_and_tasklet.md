# 02 创建第一个 Job：Spring Batch 架构与 Tasklet

## 本章学习目标

能够运行一个只有 `helloStep` 的 `simpleJob`，再运行“检查业务日期→记录结果”的双 Tasklet Job；根据日志和 Batch 元数据区分 Job 定义、执行尝试与业务结果，并解释 Tasklet 由谁、在何时调用。

## 前置知识

完成[第 01 章](01_batch_overview.md)，会启动 Spring Boot、读 Maven 依赖与 SQL 查询。本章使用[独立工程](../examples/attendance-batch/pom.xml)的教学开关 `app.lesson.enabled=true`；正式考勤汇总 Job 仍保持原样，不需要修改 Employee Management 主线。

## 实际业务场景

普通 Service 已能读取、计算和写入，却要自行设计运行身份和失败记录。考勤 Job 的第一步是检查 `businessDate`，结束时记录汇总件数；这两项“一次做完”的工作适合 Tasklet。先用一句日志理解 Job/Step，再引入业务日期，最后才运行完整的四 Step 考勤 Job。

## 核心概念

### 先让最小 Job 跑起来

- `Job`：开发者定义的一项完整批处理工作，例如 `simpleJob`；它是配置，不等于某次运行。
- `Step`：Job 内可观察的阶段。`helloStep`、`lessonValidateStep`、`lessonResultStep` 分别有自己的执行结果。
- `JobLauncher`：启动入口；测试代码或应用入口主动调用 `run(job, parameters)`，它再驱动 Job。它本身不计算考勤。
- `JobRepository`：保存执行元数据。工程使用 JDBC 仓库，将状态写入 `BATCH_*` 表，不把元数据塞进业务汇总表。Spring Batch 6 也提供不持久化的仓库，但它不能满足本专题跨进程恢复的目标。
- 第一次先记住 `Job`、`Step`、`Tasklet` 三个名字：完整任务、其中一个阶段、阶段执行的业务回调。先运行下方 `simpleJob`，再用执行记录认识其余对象。

`COMPLETED`、`FAILED`、`STOPPED` 是运行状态。`COMPLETED` 说明框架没有以失败结束，**不自动证明**汇总件数或 CSV 内容满足业务规格，仍要查业务表和文件。Job/Step 由 Spring 容器通过 `@Bean` 方法建立定义；JobLauncher 发起 Job，Spring Batch 执行 Step，并调用开发者提供的 Tasklet。

运行最小 Job 后再看元数据：`JobInstance` 是 Job 名称加标识性参数确定的业务身份；`JobExecution` 是一次运行尝试；`StepExecution` 记录该次尝试中实际运行的 Step；`ExecutionContext` 可保存重启位置。第 04 章会用故障实验把这些对象连接起来。

### Tasklet 的执行边界

`Tasklet.execute(StepContribution, ChunkContext)` 是框架在 Step 执行期间调用的回调，不是开发者自己逐条调用。`StepContribution` 关联本次 Step 的处理贡献与状态；`ChunkContext` 可取得 Step 上下文和 Job 参数。返回 `RepeatStatus.FINISHED` 表示完成；返回 `CONTINUABLE` 会要求再次执行，误用可导致重复循环；抛异常则当前 Step 失败，后续顺序 Step 不运行。`tasklet(tasklet, transactionManager)` 指定数据库事务管理器；文件和外部 API 不能随 JDBC 事务自动回滚。大量逐条数据改用下一章 Chunk。

## 执行流程

容器启动只创建定义，不执行业务：

```mermaid
flowchart LR
    A[Spring Boot 启动] --> B[Spring 容器扫描配置]
    B --> C[调用 @Bean 方法创建 Step]
    C --> D[调用 @Bean 方法创建 Job]
    D --> E[等待明确的启动入口]
```

测试调用 Launcher 后才真正执行：

```mermaid
flowchart LR
    A[测试代码调用 JobLauncher.run] --> B[Batch 在 JobRepository 创建 JobExecution]
    B --> C[Batch 启动 helloStep]
    C --> D[Batch 调用 Tasklet.execute]
    D --> E[返回 FINISHED]
    E --> F[Batch 更新 StepExecution 与 JobExecution]
```

```text
测试或手动入口 → JobLauncher → simpleJob
                              └─ helloStep → 日志 → COMPLETED
                    ↘ JobRepository → BATCH_JOB_EXECUTION / BATCH_STEP_EXECUTION

测试入口 → JobLauncher → attendanceTaskletJob(businessDate=2026-10-19)
                         ├─ lessonValidateStep：格式与源件数
                         └─ lessonResultStep：汇总件数（此时仍为 0）
                         ↘ JDBC JobRepository 保存执行状态
```

`attendanceTaskletJob` 只做检查和结果记录，**不会生成日次汇总或 CSV**。完整业务 Job 在 [BatchConfig.java](../examples/attendance-batch/src/main/java/com/example/attendance/BatchConfig.java) 中有四个 Step；此处不提前复制它的 Chunk 实现。

## 关键类与接口

| 对象 | 所属与作用 | 本例中的位置与结果 |
| --- | --- | --- |
| `@SpringBootApplication` | Spring Boot 启动与组件扫描 | `AttendanceBatchApplication.main` 创建容器 |
| `@Configuration` / `@Bean` | Spring 建立 Job、Step Bean 定义 | `TaskletLessonConfig` 在教学开关打开时生效 |
| `JobBuilder` / `StepBuilder` | Spring Batch 6 的定义构建器 | 将 Step 串成 Job；不使用旧教程的 `*BuilderFactory` |
| `JobLauncher.run` | 启动并返回 `JobExecution` | 测试主动调用；返回后检查最终状态 |
| `JobRepository` | 持久化 Instance/Execution/Context | H2 测试中的 `BATCH_*` 表可查询 |
| `PlatformTransactionManager` | 管理 Step 的数据库事务 | Tasklet Step 显式传入 |
| `@ConditionalOnProperty` | Spring Boot 按配置创建 Bean | 默认不创建教学 Job，避免干扰正式入口 |

直接依赖和版本见 [pom.xml](../examples/attendance-batch/pom.xml)：Boot 4.1.1 的 `spring-boot-starter-batch-jdbc`、MyBatis Starter 4.1.0、测试用 H2/JUnit/Spring Batch Test。实际解析为 Spring Batch 6.0.5、Spring Framework 7.0.9；`application.yml` 设 `spring.batch.job.enabled=false`，因此应用启动本身不会自动执行 Job。Spring Boot 对 `@EnableBatchProcessing` 的使用会影响自动配置，本例不额外添加它。

## 代码示例

下面是**完整教学配置类** [TaskletLessonConfig.java](../examples/attendance-batch/src/main/java/com/example/attendance/TaskletLessonConfig.java)。工程已有 [AttendanceBatchApplication.java](../examples/attendance-batch/src/main/java/com/example/attendance/AttendanceBatchApplication.java) 作为启动类；`main` 创建容器，不直接调用 Tasklet。教学配置只在 `app.lesson.enabled=true` 时创建，不影响正式四 Step Job。JDBC JobRepository 仍需要测试 H2 或个人 MySQL 数据源及 `BATCH_*` 元数据表。

```java
package com.example.attendance;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnProperty(prefix = "app.lesson", name = "enabled", havingValue = "true")
public class TaskletLessonConfig {
    private static final Logger log = LoggerFactory.getLogger(TaskletLessonConfig.class);

    @Bean
    Job simpleJob(JobRepository repository, @Qualifier("helloStep") Step helloStep) {
        return new JobBuilder("simpleJob", repository).start(helloStep).build();
    }

    @Bean
    Step helloStep(JobRepository repository, PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, context) -> {
            log.info("helloStep executed");
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("helloStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    Job attendanceTaskletJob(JobRepository repository,
                             @Qualifier("lessonValidateStep") Step validateStep,
                             @Qualifier("lessonResultStep") Step resultStep) {
        return new JobBuilder("attendanceTaskletJob", repository)
                .start(validateStep).next(resultStep).build();
    }

    @Bean
    Step lessonValidateStep(JobRepository repository,
                            PlatformTransactionManager transactionManager,
                            AttendanceMapper mapper) {
        Tasklet tasklet = (contribution, context) -> {
            String raw = (String) context.getStepContext()
                    .getJobParameters().get("businessDate");
            if (raw == null) {
                throw new IllegalArgumentException("businessDate is required");
            }
            LocalDate date = LocalDate.parse(raw);
            log.info("lessonValidateStep businessDate={} sourceCount={}",
                    date, mapper.countRecords(date));
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("lessonValidateStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    Step lessonResultStep(JobRepository repository,
                          PlatformTransactionManager transactionManager,
                          AttendanceMapper mapper) {
        Tasklet tasklet = (contribution, context) -> {
            LocalDate date = LocalDate.parse((String) context.getStepContext()
                    .getJobParameters().get("businessDate"));
            log.info("lessonResultStep businessDate={} summaryCount={}",
                    date, mapper.countSummaries(date));
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("lessonResultStep", repository)
                .tasklet(tasklet, transactionManager).build();
    }
}
```

逐行阅读上面的配置类（行号从 `package` 算起；空行和单独的右括号只划分代码范围）：

| 行 | 功能与首次出现的对象/方法 |
| --- | --- |
| 1、3～17 | 包名与导入。`Job` 是任务定义，`Step` 是其中一步，`Tasklet` 是框架运行 Step 时调用的一次性处理接口。 |
| 19～22 | `@Configuration` 使容器读取配置；`@ConditionalOnProperty` 要求教学开关为 `true`；`LoggerFactory.getLogger` 建日志对象，不执行 Job。 |
| 24～25 | `@Bean` 注册 `simpleJob`，方法签名要求容器注入 Repository 和指定的 `helloStep`。 |
| 26 | `JobBuilder` 命名 Job、连接 Repository，`start` 设首个 Step，`build` 返回定义。 |
| 29～30 | `@Bean` 注册 `helloStep`，要求容器提供 Repository 与事务管理器。 |
| 31 | 声明 Tasklet 回调；`contribution` 关联本次 Step 贡献，`context` 提供执行上下文。 |
| 32～34 | 执行时记一条日志，返回 `FINISHED` 告知本次 Tasklet 完成，随后结束 Lambda。 |
| 35～36 | `StepBuilder` 命名 Step 并关联 Repository，再接 Tasklet/事务管理器，`build` 返回定义。 |
| 39～42 | 注册双 Step Job；两个 `@Qualifier` 分别选校验和结果 Step。 |
| 43～44 | `start(validateStep).next(resultStep)` 设置运行顺序；`build` 返回 Job 定义。 |
| 47～50 | 注册校验 Step，并注入 Repository、事务管理器和 MyBatis Mapper。 |
| 51～53 | 定义回调并经 `context.getStepContext().getJobParameters().get("businessDate")` 取本次日期文本。 |
| 54～56 | 缺日期就抛 `IllegalArgumentException`，阻止继续执行。 |
| 57 | `LocalDate.parse(raw)` 将 ISO 日期文本转为日期对象，格式错也会失败。 |
| 58～59 | `mapper.countRecords(date)` 查询源表件数；占位符 `{}` 与后面两个值依序填入日志。 |
| 60～63 | 返回 `FINISHED` 结束回调；`StepBuilder` 把该回调与事务管理器装入校验 Step。 |
| 66～69 | 注册结果 Step，注入同类基础设施和 Mapper。 |
| 70～72 | 定义回调，从本次 Job 参数取日期文本并解析。 |
| 73～75 | `countSummaries(date)` 查询汇总件数、记录日志并返回 `FINISHED`。 |
| 77～78 | `StepBuilder` 装配结果 Tasklet 与事务管理器并返回 Step。 |

`JobRepository` 保存 Batch 执行元数据，不存考勤业务行；`PlatformTransactionManager` 管 Step 事务；`AttendanceMapper` 是 MyBatis 数据库访问接口。`@Bean` 方法参数由 Spring 容器按类型注入，不是执行时由学员手动填写：`JobRepository` 来自已配置的 Spring Batch JDBC 基础设施，`PlatformTransactionManager` 来自 Spring Boot 数据源/事务配置，`Step` 是同一配置类的 `@Bean` 方法注册的对象。这里有多个 Step，`@Qualifier` 按 Bean 名指定注入哪一个，避免接错步骤。启动时容器调用方法只创建 Job/Step **定义**；测试中的 `JobLauncher.run(job, parameters)` 才创建执行记录并调用 Tasklet。手动或定时入口同样必须启动 Job，不能把“Bean 已创建”当成“作业已执行”。

`helloStep` 只记日志；`lessonValidateStep` 读取标识参数、解析日期、查询源件数；它返回 `FINISHED` 后，Job 定义中的 `next(resultStep)` 才让框架调用 `lessonResultStep`。后者查询当前汇总数，本教学 Job 没有汇总 Step，所以为 0。若校验抛异常，框架将 Step/Job 记为 `FAILED`，不会进入结果 Step。下面是**完整测试启动入口** [TaskletLessonIntegrationTest.java](../examples/attendance-batch/src/test/java/com/example/attendance/TaskletLessonIntegrationTest.java)；学员可直接看出谁注入 Job、谁调用 Launcher、在哪里断言结果：

```java
package com.example.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.lesson.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:attendance_tasklet_lesson_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class TaskletLessonIntegrationTest {
    @Autowired JobLauncher launcher;
    @Autowired @Qualifier("simpleJob") Job simpleJob;
    @Autowired @Qualifier("attendanceTaskletJob") Job attendanceTaskletJob;
    @Autowired JdbcTemplate jdbc;

    @Test
    void runsOneStepThenTwoTaskletStepsAndRecordsFailure() throws Exception {
        JobExecution hello = launcher.run(simpleJob, new JobParameters());
        assertEquals(BatchStatus.COMPLETED, hello.getStatus());
        assertEquals(List.of("helloStep"), hello.getStepExecutions().stream()
                .map(step -> step.getStepName()).toList());

        JobParameters date = new JobParametersBuilder()
                .addString("businessDate", "2026-10-19", true).toJobParameters();
        JobExecution twoSteps = launcher.run(attendanceTaskletJob, date);
        assertEquals(BatchStatus.COMPLETED, twoSteps.getStatus());
        assertEquals(List.of("lessonValidateStep", "lessonResultStep"),
                twoSteps.getStepExecutions().stream().map(step -> step.getStepName()).toList());
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance_daily_summary", Long.class));

        JobExecution failed = launcher.run(attendanceTaskletJob, new JobParameters());
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        assertTrue(failed.getStepExecutions().stream()
                .anyMatch(step -> step.getStepName().equals("lessonValidateStep")
                        && step.getStatus() == BatchStatus.FAILED));
        assertEquals(3L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Long.class));
    }
}
```

测试入口逐行解读（行号从本测试代码块的 `package` 算起）：

| 行 | 功能与首次出现的对象/方法 |
| --- | --- |
| 1、3～18 | 包名及导入断言、JUnit、Batch、Spring 测试与 JDBC 类型；`assertEquals/True` 是测试判断，不是业务处理。 |
| 20～25 | `@SpringBootTest` 启动 Spring 测试容器；21 行开教学 Bean，22 行选独立 H2 URL；`@ActiveProfiles("test")` 读测试配置；JUnit 运行此测试类。 |
| 26～29 | 四行分别注入 Launcher、单 Step Job、双 Step Job 和 `JdbcTemplate`；两个 `@Qualifier` 消除 Job 名称歧义。 |
| 31～32 | `@Test` 标记方法；`throws Exception` 把启动异常交给测试框架判失败。 |
| 33 | `launcher.run(simpleJob, new JobParameters())` 用空参数启动，并保存返回的执行结果。 |
| 34～36 | 先断言完成，再从 `getStepExecutions()` 提取 Step 名，断言只有 `helloStep`。 |
| 38～39 | `JobParametersBuilder.addString` 建标识性业务日期参数。 |
| 40～41 | 启动双 Step Job，并断言最终状态完成。 |
| 42～43 | 从本次 StepExecution 列表抽出名称，断言校验先于结果。 |
| 44～45 | `JdbcTemplate.queryForObject` 对 H2 汇总表计数，预期 0；该教学 Job 没有汇总写入。 |
| 47～48 | 再用缺日期的空参数启动，断言 Job 失败。 |
| 49～51 | `anyMatch` 检查是否存在名为 `lessonValidateStep` 且状态失败的步骤。 |
| 52～53 | 查询 JDBC 元数据表，确认三次启动各形成一个 JobExecution。 |

`JobLauncher` 是执行入口；`JobParameters` 是一次启动携带的参数集合；`JobExecution` 是这次启动的执行结果对象，不等于 Job 定义。`addString` 的三个参数分别是名称、ISO 日期文本和“是否参与 JobInstance 身份”；`true` 表示标识性。此处还不讨论重启，统一在第 04 章做故障实验。

`Tasklet.execute(StepContribution contribution, ChunkContext chunkContext)` 由 Batch 在 Step 运行时调用；`contribution` 关联该次 Step 的贡献/计数，`chunkContext` 能取得 Step 上下文及上面的 JobParameters。`FINISHED` 表示本次 Tasklet 完成，`CONTINUABLE` 要求再调用，抛异常则该 Step 失败。定义 `@Bean` 与调用 `execute` 是两个不同时间点。

## 代码执行说明

| 顺序 | 执行者 | 执行内容 | 结果 |
| --- | --- | --- | --- |
| 1 | Spring 容器 | 按开关调用 `@Bean` 方法 | 建立 Job/Step 定义；没有业务日志 |
| 2 | 测试代码 | 调用 `JobLauncher.run` | 发起指定 Job 与参数 |
| 3 | Spring Batch / JobRepository | 建立 JobExecution、StepExecution | JDBC 元数据出现执行 ID |
| 4 | Spring Batch | 调用开发者的 `Tasklet.execute` | 日志或异常，返回 `FINISHED` |
| 5 | Spring Batch / JobRepository | 更新 Step 与 Job 状态 | `COMPLETED` 或 `FAILED`，再查业务表 |

1. Maven 启动测试，Spring Boot 读取 `application-test.yml`，创建独立 H2 数据源和 JDBC JobRepository；`@ConditionalOnProperty` 看到 `app.lesson.enabled=true` 才创建两个教学 Job。
2. Spring 在启动阶段调用 `@Bean` 方法构造 Job、Step **定义**，不会因此执行 Tasklet。
3. 测试主动调用 `launcher.run(simpleJob, ...)`；Batch 创建 JobExecution，开始 `helloStep`，在 Step 的事务内调用 Tasklet 的 `execute`，记录日志并返回 `FINISHED`。框架更新 Step/Job 状态到元数据表。
4. 第二次调用传入 `businessDate`，`lessonValidateStep` 读参数、解析日期、经 MyBatis Mapper 统计源记录；成功后 `lessonResultStep` 统计汇总表。没有 `summarizeStep`，所以正常会记录 `sourceCount=25`、`summaryCount=0`。
5. 测试最后故意不传日期：`lessonValidateStep` 抛异常，Job 为 `FAILED`，`lessonResultStep` 不执行。若从 `ManualBatchRunner` 输入错误日期，它可能在调用 Launcher **之前**解析失败；不能误以为任何参数错误都会产生 JobExecution。

## 逐步操作

1. 工作目录是 `docs/backend/springboot/extensions/spring-batch-training/examples/attendance-batch`；确认 [测试配置](../examples/attendance-batch/src/test/resources/application-test.yml)的 H2 与自动初始化已启用，`app.schedule.enabled=false`。不需要个人 MySQL 账号。
2. 执行 `mvn -q -Dtest=TaskletLessonIntegrationTest test`。查看 `helloStep executed`、`lessonValidateStep businessDate=2026-10-19 sourceCount=25`、`lessonResultStep ... summaryCount=0`，再看缺少日期的预期失败日志。
3. 测试断言 Job 状态、两个 Step 顺序、`BATCH_JOB_EXECUTION` 记录数以及业务汇总表仍为空。要在个人 MySQL 上运行完整业务 Job，请按 [README](../README.md) 与[运行手册](../runbook.md)初始化专用库，不要把测试 H2 当成 MySQL 实测。
4. 在工程副本交换 `attendanceTaskletJob` 的两个 Step 顺序，重新运行测试，观察先记录结果、后做校验的问题；恢复原顺序。此练习只改教学配置。

## 运行结果与验证

本机 JDK 21/H2 上该测试实测：`simpleJob=COMPLETED`；带日期的 `attendanceTaskletJob=COMPLETED` 且执行两个 Step；无日期的尝试为 `FAILED`；`BATCH_JOB_EXECUTION` 共 3 行，业务汇总表 0 行。运行中会出现预期故障的异常栈，不代表 JUnit 失败。Java 17/MySQL 的完整运行仍需单独验证，见[验证报告](../verification_report.md)。

## 常见错误

| 现象 | 先确认 | 修正 |
| --- | --- | --- |
| 启动没有执行 Job | `spring.batch.job.enabled=false`，测试是否调用 Launcher | 通过明确入口调用，不靠创建 Bean 自动运行 |
| `BATCH_*` 不存在 | JDBC Starter、测试/个人库初始化 | 在独立空库初始化当前版本元数据 |
| 缺少日期后仍执行结果 Step | 是否捕获异常后返回 `FINISHED` | 校验失败时抛异常，停止后续 Step |
| 结果日志为 0 | 本 Job 只含两个 Tasklet，无汇总 Step | 到第 03 章加入 Chunk 后再查 25 行 |
| `COMPLETED` 但业务表不对 | 只看框架状态 | 核对输入、输出表和文件 |

## 日本项目中的注意事项

调查既存 Job 时，先找 Job Bean、Step 顺序、启动入口、参数和 Repository；再看本次 JobExecution ID 与每个 StepExecution 的状态。检查日志只写业务日期、件数和执行 ID，不输出密码或真实个人信息。生产元数据表应使用受控迁移，不在每次启动时重新初始化。参考 [Spring Boot Batch 文档](https://docs.spring.io/spring-boot/reference/io/spring-batch.html)与 [Spring Batch 6 JobRepository 文档](https://docs.spring.io/spring-batch/reference/job/configuring-repository.html)。

## 学员练习

1. 基础理解：画出 `@Bean` 创建定义、`JobLauncher.run` 启动、框架调用 `Tasklet.execute` 的时间顺序。
2. 代码阅读：指出 `TaskletLessonConfig.java` 中哪两个 Step 属于 `attendanceTaskletJob`、哪一行读取 `businessDate`、哪一行查询数据库、哪一行决定 Step 顺序。
3. 实际操作：在个人副本给 `lessonValidateStep` 增加“禁止未来业务日期”的规则；执行昨天与明天两个测试输入，并保存状态和业务表证据。然后恢复副本。
4. 故障观察：让 `lessonResultStep` 抛出教学异常，说明前一个 Step 的状态、Job 状态和元数据记录；不要直接删除 `BATCH_*` 来重试。

## 本章验收标准

能运行最小 Job 与双 Tasklet Job，指出 Job/Step 定义由 Spring 创建、Tasklet 回调由 Batch 执行；能用执行记录与业务表区分“框架完成”和“业务正确”。

## 本章总结

现在已经能定义 Job、让 Tasklet 处理一次性工作，并检查运行历史。下一章把逐条考勤交给 Reader、Processor、Writer，以 Chunk 方式处理 25 条数据。
