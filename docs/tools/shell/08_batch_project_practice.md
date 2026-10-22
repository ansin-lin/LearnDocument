# 第 8 章：销售数据日次导入 Batch 综合实战

## 本章学习目标与前置知识

前置：第 1～7 章；在个人练习副本有 Bash、GNU `date` 和 JDK 17+。最终成果是依据详细设计与日语改修票，修改已有 Shell，验证正常系、异常系、デグレ（退化），留下エビデンス（测试证据）。[案例源码与样例数据](sample/sales-import/MockSalesImport.java)可直接复制到练习目录。不要在共享/生产服务器运行。

## 为什么日本项目需要

PG 常接手已有 `.sh` 而不是新建空文件。能把仕様（规格）转成代码、测试和 Review 说明，比只会背命令更重要。销售数据仅为虚构教学数据，模拟 Java **不运行 Spring Batch、不写数据库**；真实系统的事务与再执行判断必须参照项目设计。

## 简易 Shell 详细设计书

| 项目 | 规格 |
|---|---|
| Job 名 | `sales-daily-import`（日次销售取込） |
| 调用 | `bash run_sales_import.sh YYYYMMDD`，恰好一个业务日期参数 |
| 输入 | `input/sales_YYYYMMDD.csv`；UTF-8，表头 `sale_id,amount` |
| 外部程序 | 同步运行 `java -jar MockSalesImport.jar 日期 输入路径`；模拟程序校验简化 CSV 并计数 |
| 成功 | Java 状态 0 后移动输入到 `archive/`，写 `SUCCESS`，脚本返回 0 |
| 异常 | 参数/日期 2，输入/JAR 3，目录/旧归档冲突 4，Java 失败 10，归档失败 11；保留输入与日志 |
| 日志 | `logs/sales_YYYYMMDD.log`，至少有 START、Java 输出/错误、SUCCESS 或 ERROR |
| 调度 | 只在脚本 0 时允许后续日报 Job 继续 |

**流程图（说明，不是脚本）**：

```text
Jobnet → Shell 收业务日期 → 校验日期/输入/JAR
                           ↓
                   Java 模拟程序处理 CSV
                    ├─ 非 0 → 记录 ERROR、保留输入、返回 10
                    └─ 0 → 归档输入 → 记录 SUCCESS、返回 0
                              └─ 归档失败 → 返回 11、人工调查
```

## 起点资料与运行方法

案例目录含[待改修脚本](sample/sales-import/starter_run_sales_import.sh)、[完整参考脚本](sample/sales-import/run_sales_import.sh)、[模拟 Java 源码](sample/sales-import/MockSalesImport.java)、两份正常 CSV 与一份坏 CSV。先用第 5 章的 `javac`/`jar` 命令在**个人副本**生成 JAR；JAR 是生成物，不包含在仓库。`starter_...` 是已有 Shell：只收参数并调用 Java，尚缺日期/文件检查、Java 失败分支、日志和归档。学员先按下面的改修票自行修改，不照抄参考答案。

**日语改修票（教学模拟）**：

```text
件名：売上日次取込シェルの異常終了・退避対応
対象：starter_run_sales_import.sh
要件：処理対象日を厳密に確認し、入力CSV未着・空ファイルはJavaを起動しない。
      Java異常終了時は入力を残し、終了コード10と調査可能なログを出す。
      正常終了時のみCSVをarchiveへ移動する。
      同名退避済みファイルがあれば上書きせず停止する。
確認：正常系、異常系、再実行、既存動作のデグレを単体テストで示す。
```

译：严格检查业务日期；缺失/空 CSV 不启动 Java；Java 失败保留输入、写日志并返回 10；成功才归档；不得覆盖同名旧归档；以单体测试证明新旧行为。这里的“退避”是归档备份，不等同数据库回滚。

先写出影响范围：调用参数、输入目录、日志文件、归档目标、返回码、后续 Job。建议在个人副本将 starter 脚本复制为 `my_run_sales_import.sh` 后修改；不覆盖参考脚本。[完整参考脚本](sample/sales-import/run_sales_import.sh)是**完整可执行脚本**，先独立完成再用于对照。它从上到下依次是：参数与日期检查 → 计算相对脚本的绝对目录 → 文件/JAR/归档冲突检查 → 建目录与 START 日志 → 同步 Java → 立即保存状态 → 异常保留输入 / 成功移动归档 → 退出。代码里 `${BASH_SOURCE[0]}` 是当前脚本路径，`dirname` 取其所在目录；`${business_date:0:4}` 等写法截取年份、月份、日期；`date -d` 用 GNU date 判断真实日历日期，`grep -qx` 安静地检查结果是否为整行完全一致，故本例要求相应 Linux 环境。`-L` 还把已有符号链接归档视为冲突，避免覆盖。写日志失败不能再报告正常结束；其他恢复策略按项目规格确定。

## 单体测试与执行证据

每个用例从**独立个人副本或恢复后的输入状态**开始；先 `bash -n my_run_sales_import.sh` 检查语法，再逐例运行。不要在同一份已归档数据上连续期待两次成功。测试记录至少含命令、日期、退出码、日志片段、输入/归档状态、预期与实际；必要时用 `sha256sum` 对比原件前后内容。

| 用例 | 输入/准备 | 预期 |
|---|---|---|
| T01 正常系 | `20260901` 与正常 CSV | 0；`rows=2`；输入移到 archive；SUCCESS |
| T02 参数异常 | 无参数 | 2；不启动 Java、不归档 |
| T03 日期异常 | `20260230` | 2；不启动 Java |
| T04 文件未到 | `20260905` | 3；不生成归档 |
| T05 Java 异常 | 坏 CSV 复制为 `input/sales_20260903.csv` | 10；日志含数据错误；输入保留；无 SUCCESS |
| T06 旧归档冲突 | 先在个人副本准备同名 archive 文件 | 4；旧归档与输入均不覆盖 |
| T07 再执行/退化 | T01 后再次运行同日期 | 非 0；不重复生成成功；记录为何不能擅自重跑 |

例如 T01 在案例目录执行以下**可逐行运行的命令**：

```bash
# 只检查参考脚本的 Bash 语法，不运行业务。
bash -n run_sales_import.sh
# 用正常练习 CSV 执行日次导入模拟流程。
bash run_sales_import.sh 20260901
# 紧接上条命令保存 Shell 的退出状态。
status=$?
# 打印刚保存的状态，不受后续检查命令影响。
printf 'status=%s\n' "$status"
# 查看本次日志中的 START、模拟 Java 输出和 SUCCESS。
cat logs/sales_20260901.log
# 同时查看原输入和归档路径；原输入不存在是预期现象。
ls -l input/sales_20260901.csv archive/sales_20260901.csv
```

**示例结果**：`status=0`；日志出现 `START`、`MOCK_IMPORT date=20260901 rows=2`、`SUCCESS`；`ls` 报原输入不存在、归档存在。末尾 `ls` 因原输入不存在而返回非 0 是**预期的检查结果**，不是 Batch 失败。查看 Job 状态以先前保存的 `status` 为准。

异常测试结束后，区分“确认事实”和“原因推测”：例如“Java 返回 10、输入未归档”是事实，“上游生成文件有误”仍需调查。リラン／再実行前必须确认真实 Java 是否已有部分业务写入；模拟程序无法替代这个确认。結合テスト（结合测试）还要验证调度系统只在 Shell 返回 0 时启动后续 Job，本地单体测试不能证明平台配置正确。

## Review 与完成标准

交付修改脚本、T01～T07 测试表、必要日志/文件状态/返回码证据、未执行项原因及再执行风险说明。可按[证据记录样式](sample/sales-import/test_evidence.md)填写，但示例结果不能冒充本人实测。Review 指摘回复写明“修改位置 → 原因 → 再测试结果”。教师可用[Review 参考](teacher_reference.md)核对，不要求代码逐字相同。能独立依据票据改修、通过正常/异常/退化测试，并解释 Shell 与 Java 的责任边界，即完成主线。

[上一章：Job 调度](07_job_scheduling.md) · [返回课程入口](index.md)
