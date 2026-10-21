# Bash 脚本课程

本课程面向日本 Web/批处理项目中的 PG，使用 Bash 编写小型、可验证的项目辅助脚本。目标是安全处理文件和日志，不是编写系统管理框架。
开始前先完成 [Linux 命令课程](../linux/index.md)的文件、文本和权限练习。这里沿用同一份 `input/app.log`；如果没有该文件，第 06 章提供测试数据。

## 最终成果

你将完成 `check_log.sh`：接收日志路径和 request ID，校验输入，提取相关记录，输出调查摘要，正确返回退出状态，并能重复执行而不破坏原始文件。

## 学习顺序

1. [脚本结构与执行方式](01_shell_basic.md)
2. [变量、引用与参数](02_variables_arguments.md)
3. [输入输出、重定向与管道](03_io_pipelines.md)
4. [条件、循环与函数](04_control_functions.md)
5. [错误处理、日志与清理](05_error_logging.md)
6. [日志检查脚本增量实现](06_log_checker.md)
7. [日本项目改修与交付演习](07_project_task.md)

完成主线后可用 [Linux 与 Bash 新人速查表](linux_shell_quick_reference.md) 回看命令、语法和操作边界。

## 环境与边界

- Linux 普通用户、Bash 4 或更高版本
- 工作目录：`~/learndoc-linux-lab`
- 不使用 sudo，不启停系统服务，不删除原始日志
- 所有路径参数都要校验并用双引号引用
- 示例执行前可用 `bash --version` 和 `pwd` 确认环境
- 本课程的命令块在 Bash 终端执行；代码块标明的脚本内容要先保存成文件，不是逐行粘贴到终端

建议安装了 ShellCheck 的环境运行静态检查；没有时记录未执行，不自行安装软件包。
