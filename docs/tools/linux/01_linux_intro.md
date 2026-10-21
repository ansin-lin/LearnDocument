# 01 Linux 环境与命令入门

## 1.1 Linux 在 Web 项目中的位置

Linux 是服务器和开发工具常用的操作系统家族。严格来说 Linux 是内核；Ubuntu、Rocky Linux 等发行版把内核、系统工具和软件组合成可使用的系统。

**终端（Terminal）**是输入命令和查看文字结果的窗口；**Shell** 是接收命令、解释写法并启动程序的软件；**Bash** 是一种常见 Shell。本课程在 Bash 中输入命令。按回车后，Bash 先处理写法，再启动对应程序；结果或错误显示在终端。屏幕上形如 `learner@host:~$` 的部分叫**提示符**，表示终端正在等待输入，不需要复制进命令。

Web/批处理项目中的程序员通常需要登录开发、测试或调查环境，确认部署文件、进程、批处理结果和日志，并在授权范围内提供调查证据。是否可以登录、查看或修改某个环境，由项目权限和作业手顺决定。
这里的 **Web 请求**是用户访问页面或接口的一次操作；**批处理**是按计划或手动启动、集中处理一批数据的程序。PG 通常负责确认自己的程序运行情况和提供证据，不负责随意修改服务器设置。

```text
开发人员的终端 → SSH → 已授权的 Linux 开发/测试服务器
                              ├─ Java / Spring Boot 应用
                              ├─ 配置文件与应用日志
                              └─ 批处理脚本
```

项目可能把环境称为「開発環境（开发）」「テスト環境（测试）」「検証環境（验证）」和「本番環境（生产）」。名称与分工以项目手顺为准；生产环境只在授权范围内调查，不把本教程的练习命令直接用于生产。

## 1.2 确认练习环境

以下命令是只读操作：

```bash
whoami
hostname
pwd
printf 'shell=%s\n' "$SHELL"
cat /etc/os-release
```

常见输出片段：

```text
learner
test-app-01
/home/learner
shell=/bin/bash
PRETTY_NAME="Ubuntu 24.04 LTS"
```

- `whoami` 显示当前用户
- `hostname` 显示主机名
- `pwd` 显示当前工作目录
- `$SHELL` 是登录 Shell 的路径
- `/etc/os-release` 保存发行版信息；读取即可，不要修改

这里的 `cat` 表示“显示文件内容”，`/etc/os-release` 是它要读取的文件。`printf` 按给定格式输出文字，`%s` 会由后面的 `"$SHELL"` 填入，`\n` 表示换行。逐条执行后，核对用户名、主机和目录是不是预期练习环境；`$SHELL` 反映登录 Shell，不一定是当前正在运行的解释器。

如果输出不同，应记录实际环境。发行版和权限不同，日志路径等行为也可能不同。

### 在授权环境中用 SSH 连接

SSH 是加密的远程终端连接方式。只有项目提供了账号、目标主机、认证方式和作业许可时才连接；以下地址是文档示例，不能实际连接。基本写法是 `ssh [选项] 用户名@主机`：

```bash
ssh learner@192.0.2.10
ssh -p 2222 learner@192.0.2.10
```

`ssh` 是连接命令，`learner` 是远端账号，`192.0.2.10` 代表远端主机；`-p 2222` 把默认的 22 端口改为项目指定的 2222。认证可能使用密码或项目发放的密钥，不能把凭据写进教材或提交到 Git。首次连接显示的主机指纹应与项目提供的信息核对；指纹突变时停止连接并报告，不忽略警告。成功登录后先运行 `whoami`、`hostname` 和 `pwd` 核对身份与环境；能连接不等于有管理员权限。用 `exit` 退出远程会话。

连接前输入的命令在本机执行；连接成功后，在同一终端输入的命令通常在远程主机执行。本机路径与远程主机上的路径不是同一位置，不要看到同名目录就认为文件已传过去。

### 初识 PATH 与环境变量

```bash
printf 'PATH=%s\n' "$PATH"
printenv
command -v java
```

环境变量是 Shell 传给启动程序的配置值。`PATH` 是用冒号分隔的命令搜索目录；输入 `java` 时，Shell 会按这些目录寻找程序。`printenv` 列出当前环境变量，输出可能包含敏感值，不要整份提交；`command -v java` 只显示找到的命令位置，未找到通常返回非 0。不同服务器的 `PATH` 或软件安装情况不同，可能一台能执行 `java`，另一台提示 `command not found`。如何设置和传递变量在 [Shell 第 02 章](../shell/02_variables_arguments.md)学习。

## 1.3 只掌握项目常用路径

| 路径 | PG 常见用途 | 操作边界 |
|---|---|---|
| `/home/<user>` | 用户文件和练习目录 | 可在授权范围内读写 |
| `/tmp` | 临时文件 | 可能被系统清理，不存放长期成果 |
| `/etc` | 系统和应用配置 | 通常只读，修改需作业手顺 |
| `/var/log` | 部分系统或应用日志 | 是否可读取取决于权限和发行版 |
| `/opt` | 部分项目或第三方应用 | 以项目约定为准 |

应用路径没有统一答案。先阅读项目 README、部署手顺或环境定义，不要根据教程猜测生产路径。

## 1.4 命令、选项和参数

命令通常写成：

```text
command [options] [arguments]
```

方括号表示“这一部分可以省略”，不是要输入的字符；`options` 调整命令行为，`arguments` 指定处理对象。例如：

```bash
ls -la -- "$HOME"
```

- `ls` 是命令
- `-la` 是选项组合
- `--` 表示后续按路径处理，不再解析成选项
- `"$HOME"` 是当前用户主目录，双引号防止路径被错误拆分

例如 `ls -la -- "$HOME"` 会列出主目录中的项目，不会修改文件。`$HOME` 是环境变量，Shell 会先把它替换成实际路径；双引号保证路径中即使有空格仍作为一个参数。`-l` 显示详细信息，`-a` 也显示隐藏项目。

查看帮助：

```bash
ls --help
man ls
```

`man` 不一定安装；缺少时使用 `--help`，不要自行安装软件包。
`ls --help` 在终端显示可用选项；`man ls` 打开手册，通常按 `q` 退出。输出较长时可向上滚动查看。

## 1.5 建立连续练习目录

后续 Linux 和 Shell 课程复用同一目录：

```bash
mkdir -p "$HOME/learndoc-linux-lab/input"
mkdir -p "$HOME/learndoc-linux-lab/output"
cd "$HOME/learndoc-linux-lab"
pwd
ls -la
```

预期当前目录以 `/learndoc-linux-lab` 结尾，并能看到 `input`、`output`。

`mkdir` 创建目录，`-p` 表示父目录不存在时一并创建、已存在时不报错；`cd` 切换当前目录。`pwd` 确认切换后的路径，`ls -la` 确认目录确实建立。后续命令均以这个目录为起点。

生成环境记录：

```bash
whoami > output/environment.txt
hostname >> output/environment.txt
pwd >> output/environment.txt
cat output/environment.txt
```

`>` 创建或覆盖 `output/environment.txt`，`>>` 在文件末尾继续追加。这样三条命令的结果依次保存为用户名、主机名和当前目录；重复执行第一条会重建文件，避免旧记录混进本次结果。最后用 `cat` 检查应有三行。

## 1.6 常见失败

```text
bash: cd: /path/to/dir: No such file or directory
```

路径不存在或拼写错误。先用 `pwd` 和 `ls -la -- <父目录>` 检查。

```text
Permission denied
```

当前用户没有所需权限。记录路径、`ls -ld` 结果和错误信息，向负责人确认；不要直接改权限或使用 sudo。

## 1.7 本章任务

1. 创建连续练习目录。
2. 生成 `output/environment.txt`。
3. 验证文件非空：

```bash
test -s output/environment.txt
printf 'exit_status=%s\n' "$?"
```

预期退出状态为 `0`。

本章自查：**必须掌握**终端、Shell、当前用户与目录的区别；**应当掌握**按授权信息阅读 SSH 写法并核对连接目标；**了解即可**环境变量如何影响命令查找。

课堂检查：① SSH 登录前后，命令分别在哪台机器执行？② `ls -la -- "$HOME"` 中 `-l`、`-a` 与 `"$HOME"` 分别做什么？③ `environment.txt` 应有几行，怎样确认？④ 若 `hostname` 不是预期主机，下一步应做什么？参考：本机／远端；详细信息／隐藏项／主目录；三行、用 `cat` 核对；停止并确认环境。

[返回课程入口](index.md) · [下一章：文件、目录与文本命令](02_file_dir_command.md)
