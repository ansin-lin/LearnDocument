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

`whoami` 只显示用户名；`id` 还显示用户 ID、主组和附加组；`groups` 只列所属组。对照这三条输出，可以判断自己是否属于某文件的所属组。这里不运行 `sudo`。

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
例如 `-rw-r--r--` 可拆成 `- | rw- | r-- | r--`：普通文件、所有者可读写、组和其他人只读。当前用户先看自己是否是所有者，再看所属组，最后才看“其他人”的权限。

```text
- | rw- | r-- | r--
│    │     │     └─ 其他用户
│    │     └─────── 所属组
│    └───────────── 所有者
└────────────────── 文件类型（- 是普通文件）
```

对普通文件，`r` 是读内容、`w` 是改内容、`x` 是作为程序执行；对目录，`r` 是列出目录项、`w` 是增删目录项、`x` 是进入或经过目录。访问文件时还要有父目录所需的权限。数字写法把 `r=4`、`w=2`、`x=1` 相加，依次写所有者、组、其他人三位：

| 写法 | 对应权限 | 常见用途 |
|---|---|---|
| `600` | `rw- --- ---` | 自己的私有文件 |
| `644` | `rw- r-- r--` | 一般文本文件；仍须确认内容可否被其他人读取 |
| `700` | `rwx --- ---` | 自己的私有目录 |
| `755` | `rwx r-x r-x` | 需要他人读取、进入的目录或程序 |

表格用于读懂现有权限，**不是要求把项目文件统一改为某个数字**。
计算时逐位相加：`6=4+2=rw-`，`4=r--`，所以 `644` 是 `rw- r-- r--`；`7=4+2+1=rwx`、`5=4+1=r-x`，所以 `755` 是 `rwx r-x r-x`。同理，`600` 是 `6/0/0`，`700` 是 `7/0/0`。数字不表示文件一定安全，仍要看内容和谁能经过父目录。

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

`chmod` 修改文件权限，不改变文件内容；`600` 中三位依次对应所有者、组、其他人，`6=4(读)+2(写)`。第一个 `ls` 应看到 `-rw-------`。`chmod u+x` 中的 `u` 指所有者，`+x` 指增加执行权限；最后一个 `ls` 应显示所有者有 `x`，不会把脚本开放给所有人。

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

调查顺序可以记为：`Permission denied` → `whoami` 确认用户 → `id` 确认所属组 → `ls -l -- 文件` 看文件 → `ls -ld -- 父目录` 看路径。若用户、组或父目录权限不匹配，只报告已确认事实及所需的最小访问权限；不要用 `chmod 777` 掩盖问题。

## 3.5 权限错误调查

制造仅影响自己的可恢复练习：

```bash
printf 'secret\n' > output/read-demo.txt
chmod 000 output/read-demo.txt
cat output/read-demo.txt
printf 'cat_status=%s\n' "$?"
```

预期出现 `Permission denied` 且退出状态非 0。恢复：

`000` 会暂时去掉三类用户的读写执行权限，仅用于自己创建的练习文件。请连续执行下面的恢复命令；如果练习中断，返回目录后先恢复权限，不要对真实项目文件照做。

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

本章自查：**必须掌握**读懂 `rwx` 和定位自己练习文件的权限错误；**应当掌握**`600/644/700/755` 的含义；**了解即可**`chown`、`sudo` 的用途与授权边界。

课堂检查：① `-rw-r--r--` 的首字符和后三组分别表示什么？② `755` 的组权限为何是 `5`？③ 读文件失败时，为何还要查父目录？④ 练习文件改成 `000` 后如何恢复？参考：普通文件、所有者／组／其他人；`4+1=r-x`；需要路径上的访问权限；按本章步骤改回 `600`。
