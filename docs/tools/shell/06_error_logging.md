# 第 6 章：返回码、日志与异常处理

## 本章学习目标与前置知识

前置：第 5 章能同步调用模拟 Java。你将设计正常/异常分支、记录可调查日志、保留失败输入，并解释再执行风险。`trap`、`mktemp` 只要求看懂，不要求独立设计。

## 为什么日本项目需要

ジョブネット（多个相互依赖的作业）依 Shell 的終了コード决定是否启动后续作业。只打印 `ERROR` 却返回 0，会让调度系统误判正常終了；Java 失败后归档输入，又会破坏调查和再実行（重新执行）的依据。

## 返回码与输出流

课程完整案例约定：0 成功；2 参数或日期错误；3 输入或 JAR 缺失；4 输出目录或旧归档冲突；10 Java 失败；11 归档失败。它是**本案例的接口约定**，不是所有项目通用状态码。`$?` 仅是紧前一条命令的状态，先保存 `java_status=$?` 再写日志；`return` 结束函数，`exit` 结束脚本。

stdout 是正常输出，stderr 是错误输出。`>> log` 追加；`2> err` 只存错误；`>> log 2>&1` 先把 stdout 指向日志，再把 stderr 指向同一目标。顺序改变会改变去处。`tee -a log` 同时显示和追加，但管道默认状态通常只取末端；关键步骤要检查调用本身，而非只看屏幕。

以下是**完整可执行脚本** `call_with_log.sh`，放在案例目录，先按第 5 章生成 JAR：

```bash
#!/usr/bin/env bash
# 只接受一个业务日期参数。
if [[ $# -ne 1 ]]; then
  # 参数数量错误时向标准错误报告。
  printf 'ERROR YYYYMMDD required\n' >&2
  # 返回参数错误状态 2。
  exit 2
# 结束参数检查。
fi
# 保存业务日期。
business_date="$1"
# 根据日期拼出案例目录中的输入文件路径。
input_file="input/sales_${business_date}.csv"
# 文件不存在或为空时进入异常分支。
if [[ ! -s "$input_file" ]]; then
  # 报告无法处理的输入路径。
  printf 'ERROR input missing: %s\n' "$input_file" >&2
  # 返回输入错误状态 3。
  exit 3
# 结束输入检查。
fi
# 准备日志目录；创建失败返回状态 4。
mkdir -p logs || exit 4
# 按业务日期确定本次日志文件名。
log_file="logs/sales_${business_date}.log"
# 追加 START 记录；写入失败返回状态 4。
printf 'START date=%s\n' "$business_date" >> "$log_file" || exit 4
# 同步调用模拟 Java，把正常与错误输出都追加到日志。
java -jar MockSalesImport.jar "$business_date" "$input_file" >> "$log_file" 2>&1
# 必须立即保存 Java 的退出状态。
java_status=$?
# Java 非 0 时走失败分支，不能写 SUCCESS。
if [[ $java_status -ne 0 ]]; then
  # 在标准错误说明 Java 状态和保留的输入文件。
  printf 'ERROR java_status=%s input_kept=%s\n' "$java_status" "$input_file" >&2
  # 向调用方返回本案例约定的 Java 失败状态 10。
  exit 10
# 结束 Java 状态判断。
fi
# 只有 Java 成功才写 SUCCESS；日志写入失败返回 4。
printf 'SUCCESS date=%s\n' "$business_date" >> "$log_file" || exit 4
# 明确返回成功状态 0。
exit 0
```

脚本先拒绝错误调用、保留输入，再准备并写日志；随后同步调用 Java，把正常/错误输出追加到日志并立即保存返回状态。Java 非 0 时保留 CSV、返回 10；只有 Java 成功才写 SUCCESS。流程是“START → Java → 失败保留输入 / 成功写 SUCCESS”。它还没有文件归档，第 8 章完整脚本才加入。日期格式尚未严格验证，故本脚本只用于练习已有日期的运行。

```bash
# 用正常输入运行日志版脚本。
bash call_with_log.sh 20260901
# 立即保存并显示本次 Shell 的退出状态。
printf 'status=%s\n' "$?"
# 读回短练习日志，核对 START、模拟 Java 输出和 SUCCESS。
cat logs/sales_20260901.log
```

**示例输出**：状态 0；日志含 `START`、`MOCK_IMPORT ... rows=2`、`SUCCESS`。在个人副本放入坏 CSV 并以 `20260903` 运行，预期状态 10、stderr 有 `input_kept=...`，日志含 Java 错误且不含 `SUCCESS`。`cat` 用于短练习日志；长日志用 `less`、`tail`。

`set -u` 让未设置变量尽早报错，`set -o pipefail` 让管道早段失败不被末段成功遮盖。`set -e` 在条件、管道和函数等上下文有例外，不能代替显式状态判断。`bash -n 文件` 查语法；`bash -x` 会暴露展开值，仅用于无秘密练习。`trap cleanup EXIT` 可在退出时清理脚本自己创建的临时文件，`mktemp` 可安全建立临时文件；两者属于**看懂级**，本主线完整脚本不依赖它们。

再実行不是“重新敲同一命令”：先确认 Java 是否已写入部分数据、归档是否完成、上次是哪一步失败，以及业务是否允许重复导入。模拟程序不写数据库，无法证明真实系统幂等。日志至少记录业务日期、阶段、结果、可定位对象，不能输出密码、令牌或整份个人数据。

## 常见错误、练习与完成标准

把 `java_status=$?` 放在 `printf` 后会读错状态；`2>&1` 写在重定向前会使 stderr 留在原位置；Java 失败还输出 `SUCCESS` 是分支错误。请对正常、坏 CSV、缺文件分别运行并记录状态、日志末尾和输入文件是否保留。能解释为何异常系（失败路径）不归档，并提供三个可核对结果，即完成。

[上一章：Java Batch 联动](05_batch_execution.md) · [下一章：Job 调度](07_job_scheduling.md)
