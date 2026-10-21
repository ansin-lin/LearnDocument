# 01 脚本结构与执行方式

## 1.1 Bash 与脚本

Shell 读取命令并启动程序；Shell 脚本把一组命令保存到文件。课程统一使用 Bash，不混用 PowerShell、sh 或 zsh 语法。
在终端输入一条命令，Shell 会解析命令名、选项和参数，再启动对应程序；把多条命令写入 `.sh` 文件，就能按顺序重复执行。`.sh` 是常见文件名后缀，不会单独决定使用哪种 Shell。

Linux 是运行环境，终端是输入窗口，Bash 是一种 Shell 解释器，脚本是待解释的文本。`if`、变量赋值、`>` 等由 Bash 解释；`grep` 等外部程序由它启动。过程是：

```text
终端输入 bash hello.sh → Bash 读取文件 → 展开变量、解释语法
                      → 逐条执行命令 → 把退出状态交给调用者
```

```bash
bash --version | head -n 1
printf 'login_shell=%s\n' "$SHELL"
```

`$SHELL` 表示登录 Shell，不保证当前脚本一定由它解释；脚本实际解释器由执行方式决定。
`bash --version` 查看 Bash 版本；`head -n 1` 只保留输出第一行。先确认当前目录为 `~/learndoc-linux-lab`，并且使用普通用户练习。

## 1.2 第一个脚本

在 `~/learndoc-linux-lab` 新建 `hello.sh`，把下面两行保存到文件中（可用已学的 Vim）。这是**文件内容**，不是两条要直接粘贴到终端的命令：

```bash
#!/usr/bin/env bash

printf 'Hello Bash\n'
```

第一行指定直接执行时使用 Bash；第二行用 `printf` 输出 `Hello Bash` 和一个换行符。保存后先用 `cat hello.sh` 核对文件内容。

shebang 是第一行的解释器指令：直接执行 `./hello.sh` 时，系统用它寻找 Bash。显式执行 `bash hello.sh` 时，由命令中的 `bash` 选择解释器，即使没有 shebang 也能运行。

两种执行方式：

```bash
bash hello.sh

chmod u+x hello.sh
./hello.sh
```

直接执行还要求文件有执行权限。项目采用哪种方式，以调用方、cron 或作业手顺为准。
`bash hello.sh` 明确让 Bash 读取文件，不要求文件本身有执行权限；`chmod u+x` 只给文件所有者增加执行权限；`./hello.sh` 中的 `./` 表示当前目录。两次运行都应只输出一行 `Hello Bash`。

```text
bash hello.sh → 命令里的 Bash 读取脚本 → 输出 Hello Bash
./hello.sh   → 系统检查执行权限和首行 shebang → Bash 读取脚本 → 同样输出
```

若第二种方式提示 `Permission denied`，先用 `ls -l hello.sh` 看执行位；不要因第一种能运行就以为文件已有执行权限。

## 1.3 语法和退出状态

```bash
bash -n hello.sh
printf 'syntax_status=%s\n' "$?"

./hello.sh
printf 'run_status=%s\n' "$?"
```

`bash -n` 只检查语法，不执行脚本。退出状态 `0` 表示成功；脚本可用 `exit 1` 等非 0 状态表示失败。
`$?` 是**紧挨着上一条命令**的退出状态，执行下一条命令后它就会改变。先检查语法，再实际执行；两处状态都应为 `0`。

## 1.4 常见失败

- `Permission denied`：直接执行时缺少执行权限，或所在目录不可执行。
- `bad interpreter`：shebang 路径不存在，或文件含 Windows CRLF。
- `command not found`：命令拼写错误或环境 `PATH` 不同。

不要通过 sudo 运行未知脚本。先阅读脚本、检查路径和权限。

## 1.5 本章任务

创建 `hello.sh`，分别用两种方式执行，并保存语法检查和运行退出状态。

[返回课程入口](index.md) · [下一章：变量、引用与参数](02_variables_arguments.md)

本章自查：**必须掌握**脚本保存与 `bash 文件` 执行；**应当掌握**shebang、直接执行和 `bash -n`；**了解即可**登录 Shell 与脚本解释器可能不同。

课堂检查：① 哪种调用显式指定 Bash？② 哪种调用依赖 shebang 和执行权限？③ `bash -n` 会打印 `Hello Bash` 吗？④ 直接执行提示 `Permission denied` 先查什么？参考：`bash hello.sh`；`./hello.sh`；不会；目标文件与目录权限。
