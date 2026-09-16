# 01 Linux 环境与命令入门

## 1.1 Linux 在 Web 项目中的位置

Linux 是服务器和开发工具常用的操作系统家族。严格来说 Linux 是内核；Ubuntu、Rocky Linux 等发行版把内核、系统工具和软件组合成可使用的系统。

Web/批处理项目中的程序员通常需要登录开发、测试或调查环境，确认部署文件、进程、批处理结果和日志，并在授权范围内提供调查证据。是否可以登录、查看或修改某个环境，由项目权限和作业手顺决定。

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
/home/learner
shell=/bin/bash
PRETTY_NAME="Ubuntu 24.04 LTS"
```

- `whoami` 显示当前用户
- `hostname` 显示主机名
- `pwd` 显示当前工作目录
- `$SHELL` 是登录 Shell 的路径
- `/etc/os-release` 保存发行版信息；读取即可，不要修改

如果输出不同，应记录实际环境。发行版和权限不同，日志路径等行为也可能不同。

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

例如：

```bash
ls -la -- "$HOME"
```

- `ls` 是命令
- `-la` 是选项组合
- `--` 表示后续按路径处理，不再解析成选项
- `"$HOME"` 是当前用户主目录，双引号防止路径被错误拆分

查看帮助：

```bash
ls --help | head
man ls
```

`man` 不一定安装；缺少时使用 `--help`，不要自行安装软件包。

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

生成环境记录：

```bash
{
  printf 'user=%s\n' "$(whoami)"
  printf 'host=%s\n' "$(hostname)"
  printf 'directory=%s\n' "$(pwd)"
} > output/environment.txt

cat output/environment.txt
```

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

[返回课程入口](index.md) · [下一章：文件、目录与文本命令](02_file_dir_command.md)
