# 02 文件、目录与文本命令

本章继续使用 `~/learndoc-linux-lab`，目标是在限定目录内完成文件整理和日志初步调查。

## 2.1 路径和目录确认

- 绝对路径从 `/` 开始
- 相对路径以当前目录为基准
- `.` 表示当前目录，`..` 表示上一级，`~` 表示当前用户主目录

```bash
cd "$HOME/learndoc-linux-lab"
pwd
ls -ld -- input output
```

`ls -d` 显示目录本身；`--` 防止以连字符开头的路径被当成选项。
`cd` 不给参数时通常回到主目录，这里给出完整练习路径以避免走错地方；`pwd` 应显示该目录。`ls -ld -- input output` 只显示这两个目录自身的详细信息，不列出里面的文件。

## 2.2 查看目录内容

```bash
ls
ls -l
ls -a
ls -lh
ls -la
ls -lh -- input
ls -lt -- input
```

`ls [选项] [路径]` 列出目录内容；省略路径时查看当前目录，不修改文件。这里的 `input` 是第 01 章创建的目录。

- **必会**：`-l` 显示权限、所有者、大小和时间；`-a` 连隐藏项目一起显示。
- **常用**：`-h` 与 `-l` 组合，让大小用易读单位显示；`-t` 按修改时间从新到旧排列；`-d` 与 `-l` 组合时只看目录自身。

`ls -lt -- input` 用于找最近变化的练习文件；结果中的时间是文件元数据，不一定是业务事件发生时间。
先分别观察 `-l` 的详细列、`-a` 增加的隐藏项和 `-lh` 的大小单位，再读组合写法：`-la` 相当于同时给出 `-l` 和 `-a`，不是单独一种选项。`ls -l` 的一行例如 `-rw-r--r-- 1 learner learner 128 Sep 1 09:00 app.log`，依次可看到类型与权限、链接数、所有者、组、字节数、修改时间和名称。此处先认列，第 03 章再拆解权限位；文件大小、时间依实际练习环境而变。

## 2.3 创建练习数据

在 `input` 中创建应用日志和配置样例：

```bash
cat > input/app.log <<'EOF'
2026-09-01T09:00:00 INFO  request_id=R001 start
2026-09-01T09:00:01 ERROR request_id=R001 employee not found
2026-09-01T09:01:00 INFO  request_id=R002 completed
2026-09-01T09:02:00 WARN  request_id=R003 slow response
EOF

printf 'mode=batch\n' > input/app.conf
ls -lh -- input
```

`<<'EOF'` 用于按原样写入多行文本，Shell 课程会进一步解释。
`cat > input/app.log` 创建或覆盖日志样例；最后单独一行的 `EOF` 表示输入结束。`printf 'mode=batch\n'` 输出一行配置，并用 `>` 保存到 `input/app.conf`。重复执行本节会重置这两个文件，先确认没有需要保留的练习修改。

## 2.4 创建、复制和移动

```bash
mkdir -p output/archive
cp -- input/app.conf output/app.conf.backup
mv -- output/app.conf.backup output/archive/app.conf.backup
ls -l -- output/archive
```

- `mkdir -p` 按需要创建多级目录
- `cp` 复制文件
- `mv` 移动或重命名；目标已存在时可能覆盖，执行前先 `ls`

这里 `cp -- 源文件 目标文件` 保留原文件并生成备份；`mv -- 旧路径 新路径` 把备份移进 `archive`，旧路径随之消失。`--` 后面的内容按文件路径处理。用最后的 `ls` 确认目标文件出现，若目标已存在，先停止并检查，避免覆盖。
对照核对：`cp` 后用 `ls input/app.conf output/archive/app.conf.backup` 应看到原件和备份同时存在；`mv` 后 `output/app.conf.backup` 通常不再存在。两者目标已存在时都可能覆盖，`-i` 会询问，`-n` 则跳过覆盖；遇到跳过仍要查最终路径，不能只看退出状态。

`cp [选项] 源 目标` 用于复制，`mv [选项] 源 目标` 用于移动或改名。先在自己的练习目录看额外写法：

```bash
cp -i -- input/app.conf output/app.conf.review
cp -p -- input/app.conf output/app.conf.with-meta
mkdir -p output/input-copy
cp -R -i -- input/. output/input-copy/
mv -i -- output/app.conf.review output/archive/app.conf.review
mv -n -- output/app.conf.with-meta output/archive/app.conf.with-meta
```

`cp -i`、`mv -i` 在目标已存在时询问是否覆盖，适合人工练习；`mv -n` 遇到已存在目标时不覆盖，但不能只凭退出状态判断是否真的移动，应再用 `ls` 核对。`cp -p` 尽量保留时间与权限等元数据，是否能保留所有属性取决于权限和文件系统；`cp -R` 递归复制目录内容，`input/.` 表示该目录中的内容。执行前确认来源和目标，避免复制大量项目数据。所有示例目标都在 `output` 下，不对共享目录练习。

## 2.5 查看文本

测试人员说日志里有错误时，先按文件大小和想找的信息选命令，不必把长日志一次打印到屏幕：

| 想解决的问题 | 先选的命令 |
|---|---|
| 短配置内容是什么 | `cat` |
| 长文件需要翻页 | `less` |
| 只看文件开头／末尾 | `head`／`tail` |
| 观察新追加的日志 | `tail -f` |
| 只找符合条件的行 | `grep` |

```bash
cat input/app.conf
head -n 2 input/app.log
tail -n 2 input/app.log
less input/app.log
```

- `cat` 适合短文件
- `head`、`tail` 读取开头或结尾
- `-n 2` 指定显示 2 行；换成 `-n 20` 就显示 20 行
- `less` 适合长文件；输入 `/ERROR` 搜索，按 `n` 查下一个，按 `q` 退出

持续观察正在追加的日志：

```bash
tail -f input/app.log
```

按 `Ctrl+C` 停止。它会持续占用终端，不是命令卡死。
`tail -f 文件` 跟随当前打开的文件；日志轮转后它可能继续跟随旧文件。GNU `tail -F 文件` 按文件名重新打开并重试，适合需要观察轮转后新日志的场景。两者都是只读观察，用后按 `Ctrl+C` 停止；第 06 章再结合日志轮转使用。

## 2.6 使用 grep 过滤

样例日志已有四行，要定位错误而不逐行查阅，可用 `grep [选项] 搜索内容 文件路径`；`[选项]` 可省略，搜索内容与路径是本次输入。它只读取文件。先运行第一条并读输出，再尝试其他选项：

```bash
grep -n 'ERROR' input/app.log
grep -Ei 'ERROR|WARN' input/app.log
grep -v 'INFO' input/app.log
grep -c 'ERROR' input/app.log
```

第一条应显示 `2:2026-09-01T09:00:01 ERROR request_id=R001 employee not found`。`2:` 是原日志第 2 行；其后依次是记录时间、ERROR 级别、请求编号和程序记下的现象。下一步用同一个 ID 查请求开始等记录；这行本身还不能证明数据库里真的没有员工。没有匹配时 `grep` 通常返回状态 `1`。

`grep` 按行搜索：`-n` 附行号，`-E` 允许用 `|` 表示“或”，`-i` 忽略大小写，`-v` 排除匹配行，`-c` 只显示匹配行数。本例四条命令分别定位 ERROR、查 ERROR/WARN、排除 INFO、统计 ERROR；搜索不会修改原日志。

基本写法为 `grep [选项] 搜索模式 文件`。项目调查还会用到：

```bash
grep -n -C 1 'ERROR' input/app.log
grep -n -F 'request_id=R001' input/app.log
grep -r -n 'mode=' input
```

`-C 1` 显示匹配行及前后各 1 行；`-A 1` 只看后 1 行，`-B 1` 只看前 1 行。`-F` 把模式当作普通字符串，不解释正则符号，但**不保证 request ID 完整匹配**；如果日志存在 `R0012`，还须检查字段边界。`-r` 递归搜索给定目录，必须限制在已授权的项目目录；这里仅搜索练习目录 `input`。这些命令输出匹配行，不改变日志。

```bash
grep 'FATAL' input/app.log
printf 'grep_status=%s\n' "$?"
```

状态 `1` 表示没有匹配；大于 `1` 通常表示路径、权限等实际错误。

## 2.7 管道、重定向和统计

管道 `|` 把左侧标准输出交给右侧命令：

```bash
grep -E 'ERROR|WARN' input/app.log | wc -l
```

预期输出为 `2`。保存调查结果：

`wc -l` 计算输入行数。`grep` 把符合条件的行送入管道，`wc` 数出 2 行；如果左侧命令失败，单看 `wc` 的结果可能误以为只是 0 行，排错时要单独确认 `grep` 的错误信息和退出状态。

`wc -l [文件]` 统计换行结束的行数；不带文件而从管道读入时只输出数字。`sort` 排序文本行，`uniq -c` 统计**相邻的相同行**，因此通常先排序：

```bash
printf 'ERROR\nWARN\nERROR\n' | sort | uniq -c
```

预期得到 `ERROR` 2 次、`WARN` 1 次；这里的固定小样例只练统计写法，不直接把整份生产日志导出。`diff -u 旧文件 新文件` 比较差异，统一格式中的 `-` 是旧行、`+` 是新行；相同返回 0，有差异通常返回 1，其他错误另行排查。第 07 章用它核对配置副本。

```bash
grep -En 'ERROR|WARN' input/app.log > output/error_lines.txt
cat output/error_lines.txt
```

`>` 覆盖目标文件，`>>` 追加内容。执行前要确认目标路径。

## 2.8 查找文件

只在练习目录中搜索：

```bash
find . -type f -name '*.log' -print
find output -maxdepth 2 -type f -print
```

不要对 `/` 或未知共享目录递归搜索，避免大量 I/O、权限错误和无关输出。
`find` 在指定目录下逐项寻找；`.` 是当前练习目录，`-type f` 只选普通文件，`-name '*.log'` 按文件名匹配日志，`-print` 输出路径。`*` 表示任意长度的字符；给它加引号，避免 Bash 在执行 `find` 前就展开。`-maxdepth 2` 把搜索限制在起点及其两层范围内。
`find 起点 [条件]` 默认会递归进入子目录；`-mtime -1` 在 GNU find 中表示大致最近 24 小时修改过。练习时可执行 `find input -maxdepth 1 -type f -mtime -1 -print`，结果取决于文件实际修改时间，不应为了得到某个输出去更改项目文件时间。

## 2.9 安全删除

本课程不把 `rm -rf` 作为日常命令。删除普通练习文件前先确认精确路径：

```bash
touch output/delete-me.txt
ls -l -- output/delete-me.txt
rm -- output/delete-me.txt
test ! -e output/delete-me.txt
printf 'deleted=%s\n' "$?"
```

删除空目录使用 `rmdir`。递归删除必须由作业手顺限定范围并说明备份；不在共享环境按教程自行执行。
`touch` 在这里创建空的练习文件；`rm --` 删除指定文件，通常不能直接恢复，因此先用 `ls` 核对路径。`test ! -e` 检查该路径已不存在，状态 `0` 表示检查成立。
`rm -i 文件` 会逐个询问是否删除。`rm -r` 会进入子目录递归删除，`rm -f` 会抑制部分提示并强制处理；二者组合尤其危险，本课程只要求**识别风险**，不安排执行。要删除自己创建的空目录，用 `rmdir 目录`，非空时它会拒绝删除。

## 2.10 归档练习目录（了解）

项目有时需要把已脱敏的调查文件打包。`tar` 把多个文件组成一个归档，`gzip` 再压缩；`.tar.gz` 同时表示这两层处理，不等于正式备份制度。仅对自己创建的 `input` 练习数据操作：

```bash
tar -czf output/input-sample.tar.gz input
tar -tzf output/input-sample.tar.gz
mkdir -p output/extracted
tar -xzf output/input-sample.tar.gz -C output/extracted
ls -l -- output/extracted/input
```

`tar [操作选项] -f 归档文件 [来源]` 中，`-c` 创建、`-t` 列出成员、`-x` 解开，三者每次只选一种；`-z` 使用 gzip，`-f` 后紧跟归档路径，`-v` 可显示处理文件名。先 `-t` 看成员，确认没有绝对路径或向上跳转路径，再把**可信的练习归档**用 `-C` 解到独立的 `output/extracted`，避免覆盖已有文件。真实项目归档可能包含客户数据，未经授权不得复制或上传。

## 2.11 本章任务

1. 从样例日志中提取 ERROR 和 WARN 行到 `output/error_lines.txt`。
2. 使用 `wc -l` 验证结果为 2 行。
3. 保留结果供最终调查演习使用。

```bash
test "$(wc -l < output/error_lines.txt)" -eq 2
printf 'exit_status=%s\n' "$?"
```

[上一章：Linux 环境与命令入门](01_linux_intro.md) · [下一章：用户、组与文件权限](03_user_permission.md)

本章自查：**必须掌握**路径、`ls`、`cp`、`mv`、`grep`、`tail`；**应当掌握**限定目录使用 `find`、阅读 `tar` 归档；**了解即可**递归删除、日志轮转后的跟随差异。

课堂检查：① `ls -la` 是哪两个选项？② 复制和移动后，原路径分别怎样变化？③ `grep -n` 输出前面的 `2:` 是什么？④ `grep ... | wc -l` 得到 0 时为什么还要查左侧状态？参考：`-l`、`-a`；复制保留、移动通常移走；原文件行号；左侧失败也可能使右侧显示 0。
