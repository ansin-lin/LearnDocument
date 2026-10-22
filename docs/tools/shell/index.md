# Shell 新人培训：销售数据日次导入 Batch

本课程面向零经验 Shell 学员及 Java / Spring Boot 新人 PG。完成后应能阅读、修改和测试日本项目中的业务 Batch Shell：接收业务日期、检查 CSV、同步调用 Java、正确返回状态、记录日志、成功归档，并依据改修票留存正常/异常/退化测试证据。重点是程序开发与调查，不是 Linux 运维或复杂调度配置。

先学 [Linux 基础命令课程](../linux/index.md)的目录、文件和权限；第 5 章起的可执行模拟案例需 Linux Bash、GNU date、JDK 17+。没有 JDK 可先学第 1～4 章，第 5～8 章记录环境限制，不在项目服务器自行安装。练习统一在**个人副本**进行；样例 CSV 是虚构数据。模拟 Java 程序只做简化校验与计数，不是 Spring Batch，也不写数据库。

## 八章主线

1. [Shell 基础与脚本执行](01_shell_basic.md)：运行第一份脚本，辨认解释器与状态。
2. [变量、参数与环境变量](02_variables_arguments.md)：接收业务日期和输入路径。
3. [条件判断、循环与函数](03_control_functions.md)：拒绝错误参数、日期格式和文件。
4. [文件、文本与输入输出](04_file_text_processing.md)：准备 CSV，理解文件与标准流。
5. [Shell 与 Java Batch 联动](05_batch_execution.md)：同步运行可执行模拟 JAR 并保存状态。
6. [返回码、日志与异常处理](06_error_logging.md)：区分正常/异常并保留调查资料。
7. [Job 调度与日本项目运用](07_job_scheduling.md)：理解 cron、JP1/AJS、Jobnet 与再执行。
8. [销售数据日次导入综合实战](08_batch_project_practice.md)：按详细设计和日语改修票完成开发、自测和 Review。

主线贯穿的[案例源码、脚本与 CSV](sample/sales-import/MockSalesImport.java)从最小示例逐步扩展；第 8 章提供完整可运行版本与待改修起点。[教师参考](teacher_reference.md)用于评审，[Shell 速查表](shell_quick_reference.md)用于课后回看。旧课程的 `check_log.sh` 不再占主线，保留为[日志调查附录](appendix/check_log.md)。

实际项目运行账号、路径、编码、调度和再执行权限以项目手顺为准；练习不用 sudo、不接触生产数据、不自行重跑真实业务 Job。
