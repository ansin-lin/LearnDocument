# 01 脚本结构与执行方式

## 1.1 Bash 与脚本

Shell 读取命令并启动程序；Shell 脚本把一组命令保存到文件。课程统一使用 Bash，不混用 PowerShell、sh 或 zsh 语法。

```bash
bash --version | head -n 1
printf 'login_shell=%s\n' "$SHELL"
```

`$SHELL` 表示登录 Shell，不保证当前脚本一定由它解释；脚本实际解释器由执行方式决定。

## 1.2 第一个脚本

在 `~/learndoc-linux-lab` 创建 `hello.sh`：

```bash
#!/usr/bin/env bash

printf 'Hello Bash\n'
```

shebang 是第一行的解释器指令：直接执行 `./hello.sh` 时，系统用它寻找 Bash。显式执行 `bash hello.sh` 时，由命令中的 `bash` 选择解释器，即使没有 shebang 也能运行。

两种执行方式：

```bash
bash hello.sh

chmod u+x hello.sh
./hello.sh
```

直接执行还要求文件有执行权限。项目采用哪种方式，以调用方、cron 或作业手顺为准。

## 1.3 语法和退出状态

```bash
bash -n hello.sh
printf 'syntax_status=%s\n' "$?"

./hello.sh
printf 'run_status=%s\n' "$?"
```

`bash -n` 只检查语法，不执行脚本。退出状态 `0` 表示成功；脚本可用 `exit 1` 等非 0 状态表示失败。

## 1.4 常见失败

- `Permission denied`：直接执行时缺少执行权限，或所在目录不可执行。
- `bad interpreter`：shebang 路径不存在，或文件含 Windows CRLF。
- `command not found`：命令拼写错误或环境 `PATH` 不同。

不要通过 sudo 运行未知脚本。先阅读脚本、检查路径和权限。

## 1.5 本章任务

创建 `hello.sh`，分别用两种方式执行，并保存语法检查和运行退出状态。

[返回课程入口](index.md) · [下一章：变量、引用与参数](02_variables_arguments.md)
