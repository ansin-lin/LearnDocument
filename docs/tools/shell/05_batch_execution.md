# 第 5 章：Shell 与 Java Batch 联动

## 本章学习目标与前置知识

前置：第 1～4 章的脚本、参数、分支和文件检查；本章还需本机或练习 Linux 有 JDK 17+（包含 `java`、`javac`、`jar`）。你将运行一个**可执行但不写数据库的模拟 Java 程序**，从 Shell 调用它并读取返回码。没有 JDK 时先读懂调用流程，记录环境限制，不在项目服务器自行安装。

## 为什么日本项目需要

取込処理（导入处理）常由调度系统触发 Shell，Shell 准备日期、文件、环境并调用 Java；Java / Spring Batch 执行业务校验、数据库更新与提交回滚。PG 要能区分“脚本启动成功”和“业务处理成功”。本章模拟程序只校验简化 CSV 并计数，**不是 Spring Batch，不写入数据库**。

## 概念与最小调用

`java -jar 文件.jar 参数...` 启动一个 Java 进程并等待其结束；`-jar` 指定可执行 JAR。Shell 中不加 `&` 是同步执行，下一行会在 Java 结束后运行；加 `&` 是后台启动，当前 Shell 立即继续，不能马上用 `$?` 当业务结束状态。日次 Job 默认使用同步调用。JAR 是程序文件，执行后才有进程。

先在 [案例目录](sample/sales-import/MockSalesImport.java) 所在位置执行以下**可逐行运行的准备命令**：

```bash
# 进入个人副本中的案例目录，以下相对路径均从这里计算。
cd docs/tools/shell/sample/sales-import
# 确认当前 Java 运行环境版本。
java -version
# 把模拟程序源码编译成当前目录下的 class 文件。
javac -d . MockSalesImport.java
# 把 class 打包成有主类入口的可执行 JAR。
jar --create --file MockSalesImport.jar --main-class MockSalesImport MockSalesImport.class
# 传入业务日期和对应 CSV，运行模拟导入检查。
java -jar MockSalesImport.jar 20260901 input/sales_20260901.csv
# 立即打印 Java 进程的退出状态。
printf 'java_status=%s\n' "$?"
```

`javac` 把源码编译为 `.class`，`jar` 把它打包为可执行 JAR；这些生成文件仅在个人练习副本保留，不提交。最后两行**示例输出**为 `MOCK_IMPORT date=20260901 rows=2`、`java_status=0`。实际 Spring Batch 项目 JAR 由项目构建流程提供，PG 通常不手工用 `jar` 打包。

下面是**完整可执行脚本** `call_mock.sh`，放在案例目录，输入 CSV 要先存在：

```bash
#!/usr/bin/env bash
# 只接受一个业务日期参数。
if [[ $# -ne 1 ]]; then
  # 参数不符时把用法写到标准错误。
  printf 'Usage: %s YYYYMMDD\n' "$0" >&2
  # 以状态 2 结束脚本。
  exit 2
# 结束参数数量判断。
fi
# 保存传入的业务日期。
business_date="$1"
# 按日期构造相对案例目录的输入路径。
input_file="input/sales_${business_date}.csv"
# 没有非空输入文件时不启动 Java。
if [[ ! -s "$input_file" ]]; then
  # 报告缺失或空文件路径。
  printf 'ERROR input missing or empty: %s\n' "$input_file" >&2
  # 输入错误返回状态 3。
  exit 3
# 结束输入检查。
fi
# 同步运行模拟 Java；Shell 等待它结束。
java -jar MockSalesImport.jar "$business_date" "$input_file"
# 紧接调用保存 Java 状态，避免被下一条命令覆盖。
java_status=$?
# 打印保存的 Java 状态供观察。
printf 'java_status=%s\n' "$java_status"
# 将同一状态交给调用此 Shell 的进程。
exit "$java_status"
```

此脚本依次拒绝参数数量错误、用业务日期构造输入路径、确认文件非空、同步调用模拟 Java，随后**立即**保存 Java 的終了コード（结束码）并交给调用者。流程为“参数 → 输入 → Java → 状态”；此最小脚本依赖从案例目录调用，不做归档或日志，第 6/8 章再补全。

```bash
# 用现有的正常 CSV 测试调用。
bash call_mock.sh 20260901
# 立即查看 Shell 的退出状态。
printf 'shell_status=%s\n' "$?"
# 用不存在的日期文件测试输入缺失分支。
bash call_mock.sh 20260903
# 立即查看失败时的 Shell 状态。
printf 'shell_status=%s\n' "$?"
```

预期第一轮输出 `rows=2`、`java_status=0`、`shell_status=0`；第二轮因输入不存在返回 3，Java 根本没有启动。若想测试 Java 失败，可在**个人副本**中临时复制 `testdata/sales_20260903_bad.csv` 为 `input/sales_20260903.csv` 再运行，预期 Java 报数据行错误并返回 10；测试后确认并清理该副本。

Spring Batch 项目常有 Profile（环境配置）、Job 参数和外部配置，具体格式以项目构建产物和详细设计书为准；不要把本模拟 JAR 的两个参数当成所有 Spring Batch 项目的通用接口。`APP_ENV`、`JAVA_HOME` 与 `PATH` 可影响运行环境：先用 `command -v java`、`java -version` 只读核对，不把密码写进脚本。

命令格式可对照 Oracle 的 [Java `-jar` 说明](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html)与 [`jar --create` 说明](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jar.html)；项目 JDK 版本仍以所属项目要求为准。

## 常见错误、练习与完成标准

`Unable to access jarfile`：路径错或文件不可读；`command not found`：Java 未在 PATH；Java 返回 10：本模拟程序发现数据错误；脚本返回 0 却没检查 Java 状态：严重漏判。请修改 `call_mock.sh`，Java 返回非 0 时额外打印 `ERROR Java failed` 到 stderr，仍原样返回 Java 状态；用正常和坏数据各记录一次输出与两层状态。能说明 Shell/Java 职责、同步/后台区别，并证明 Java 失败未被包装成成功，即完成。

[上一章：文件与文本](04_file_text_processing.md) · [下一章：返回码与日志](06_error_logging.md)
