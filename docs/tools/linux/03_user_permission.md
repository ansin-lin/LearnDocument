# 03 用户、组与文件权限

PG 的重点是读懂权限并定位问题，不是创建系统用户或修改 sudo 策略。本章只操作自己的练习文件。

## 3.1 确认身份

```bash
whoami
id
groups
```

- UID 是用户 ID，GID 是主组 ID
- 一个用户有一个主组，也可以属于附加组
- sudo 按系统策略允许用户以其他身份执行命令，默认目标通常是 root；并非所有用户都有 sudo 权限

## 3.2 读懂 `ls -l`

```bash
cd "$HOME/learndoc-linux-lab"
ls -l -- input/app.conf
```

示例输出：

```text
-rw-r--r-- 1 learner learner 11 Sep 1 09:00 input/app.conf
```

- 第一个字符 `-` 表示普通文件，`d` 表示目录，`l` 表示符号链接
- 后九位依次是所有者、所属组、其他用户的权限
- `r` 读取，`w` 写入，`x` 执行或进入目录

目录的 `x` 表示能否进入和访问其中项目；是否能删除目录内文件主要取决于父目录的写和执行权限。

## 3.3 chmod 只修改自己的练习文件

```bash
cp -- input/app.conf output/permission-demo.conf
chmod 600 output/permission-demo.conf
ls -l -- output/permission-demo.conf
```

`600` 表示所有者可读写，组和其他用户无权限。脚本需要直接执行时可以只给所有者增加执行权限：

```bash
printf '#!/usr/bin/env bash\nprintf "ok\\n"\n' > output/demo.sh
chmod u+x output/demo.sh
ls -l -- output/demo.sh
```

不要用 `chmod 777` 解决权限错误；它会把写入和执行能力开放给所有用户。

## 3.4 chown 和 sudo 的项目边界

`chown` 改变所有者或所属组，通常需要额外权限。PG 应能读懂已有命令，但不在共享环境自行执行：

```text
chown <user>:<group> <path>
```

遇到权限问题，先收集：

```bash
whoami
id
ls -ld -- <parent-directory>
ls -l -- <target-file>
```

然后报告需要的最小权限、目标路径和业务原因。不要执行递归 `chown -R`、递归 `chmod` 或修改系统用户。

## 3.5 权限错误调查

制造仅影响自己的可恢复练习：

```bash
printf 'secret\n' > output/read-demo.txt
chmod 000 output/read-demo.txt
cat output/read-demo.txt
printf 'cat_status=%s\n' "$?"
```

预期出现 `Permission denied` 且退出状态非 0。恢复：

```bash
chmod 600 output/read-demo.txt
cat output/read-demo.txt
```

## 3.6 本章任务

1. 把 `output/permission-demo.conf` 设置为仅自己可读写。
2. 记录 `whoami`、`id` 和 `ls -l` 输出到 `output/permission-evidence.txt`。
3. 确认权限字符串以 `-rw-------` 开头。

```bash
ls -l output/permission-demo.conf | grep '^-rw-------'
printf 'exit_status=%s\n' "$?"
```

[上一章：文件、目录与文本命令](02_file_dir_command.md) · [下一章：进程、退出状态与资源确认](04_process_system.md)
