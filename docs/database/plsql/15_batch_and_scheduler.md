# PL/SQL 批处理与定时作业

> 本章系统讲解 **PL/SQL 在真实项目中的批处理设计与定时作业机制**。这是银行、账务、请求书、清算系统中**每天都会运行、但最容易出事故**的一类程序，也是新人迈向“能独立维护系统”的关键一章。

---

## 1. 什么是批处理（Batch Processing）

**批处理**指的是：

> 在**指定时间**，对**大量数据**进行**集中、自动化处理**的程序。

典型特征：

* 无人工干预
* 数据量大
* 对稳定性要求高
* 允许延迟，但不允许出错

---

## 2. 批处理在真实项目中的常见场景

| 场景 | 说明 |
| ------ | ----------- |
| 夜间清算 | 利息、手续费、余额结转 |
| 对账处理 | 银行 / 第三方对账 |
| 请求书生成 | 月度 / 日度帐票 |
| 数据归档 | 历史数据迁移 |
| 状态批量更新 | 订单、交易状态 |

---

## 3. 一个标准批处理程序的结构

在项目中，**批处理不是一段 SQL，而是一套结构**。

```text
主控过程（Main）
 ├─ 参数检查
 ├─ 业务处理（子过程）
 ├─ 结果汇总
 ├─ 日志记录
 └─ 返回状态
```

📌 原则：

* 一个批处理 = 一个 PACKAGE
* 一个入口过程

---

## 4. 批处理中的事务设计（核心）

### 4.1 为什么不能一次 COMMIT

* UNDO 过大
* 锁时间过长
* 回滚成本极高

### 4.2 标准做法：分批提交

```plsql
loop
  fetch c bulk collect into v_rows limit 1000;
  exit when v_rows.count = 0;

  forall i in 1 .. v_rows.count
    update ...;

  commit;
end loop;
```

📌 COMMIT 是**设计点**，不是语法点。

---

## 5. 批处理的幂等性设计（非常重要）

> 批处理**必须允许重跑**。

常见手段：

* 状态字段（未处理 / 已处理）
* 处理时间戳
* 批次号（batch_id）

```sql
where status = 'INIT'
```

---

## 6. 批处理日志与错误处理

### 6.1 为什么必须写日志表

* dbms_output 在生产不可用
* 作业失败需要可追踪

### 6.2 日志表示例

```sql
batch_log
(
  job_name,
  start_time,
  end_time,
  status,
  error_msg
)
```

### 6.3 自治事务写日志

```plsql
pragma autonomous_transaction;
```

---

## 7. 定时作业概述

批处理通常通过 **数据库定时作业** 自动触发。

Oracle 提供两套机制：

| 工具 | 说明 |
| -------------- | ----- |
| DBMS_SCHEDULER | 新系统首选 |
| DBMS_JOB | 老系统遗留 |

---

## 8. DBMS_SCHEDULER（重点）

### 8.1 创建定时作业

```plsql
begin
  dbms_scheduler.create_job(
    job_name        => 'JOB_DAILY_BATCH',
    job_type        => 'STORED_PROCEDURE',
    job_action      => 'pkg_batch.main',
    start_date      => systimestamp,
    repeat_interval => 'FREQ=DAILY;BYHOUR=1',
    enabled         => true
  );
end;
/
```

---

### 8.2 常见 repeat_interval 示例

| 规则 | 示例 |
| ---- | ------------------- |
| 每天 | FREQ=DAILY |
| 每月 | FREQ=MONTHLY |
| 指定时间 | BYHOUR=2;BYMINUTE=0 |

---

### 8.3 作业管理

```sql
-- 禁用
dbms_scheduler.disable('JOB_DAILY_BATCH');

-- 启用
dbms_scheduler.enable('JOB_DAILY_BATCH');

-- 手动执行
dbms_scheduler.run_job('JOB_DAILY_BATCH');
```

---

## 9. DBMS_JOB（了解即可）

```plsql
declare
  v_job number;
begin
  dbms_job.submit(
    job       => v_job,
    what      => 'pkg_batch.main;',
    next_date => sysdate,
    interval  => 'sysdate + 1'
  );
end;
/
```

📌 新项目 **不再推荐使用**。

---

## 10. 批处理常见失败场景与应对

| 问题 | 对策 |
| ---- | ------------ |
| 中途失败 | 支持重跑 |
| 重复执行 | 幂等控制 |
| 性能过慢 | BULK + LIMIT |
| 锁冲突 | 缩小事务 |

---

## 11. 面试高频问题（标准回答方向）

* 批处理如何设计？
* 定时作业失败怎么办？
* 如何避免重复执行？

📌 核心关键词：

> **分批、日志、幂等、可重跑**

---

## 12. 本章小结（工程师版）

* 批处理是系统稳定性的关键
* 必须支持失败重跑
* DBMS_SCHEDULER 是主流方案
* 批处理设计重于代码

👉 下一章建议：**PL/SQL 安全、权限与部署规范**
