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

## 2.2 查看目录内容

```bash
ls
ls -la
ls -lh -- input
```

| 选项 | 作用 |
|---|---|
| `-l` | 显示权限、所有者、大小和时间 |
| `-a` | 包含隐藏项目 |
| `-h` | 与 `-l` 配合，以易读单位显示大小 |

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

## 2.5 查看文本

```bash
cat input/app.conf
head -n 2 input/app.log
tail -n 2 input/app.log
less input/app.log
```

- `cat` 适合短文件
- `head`、`tail` 读取开头或结尾
- `less` 适合长文件；输入 `/ERROR` 搜索，按 `n` 查下一个，按 `q` 退出

持续观察正在追加的日志：

```bash
tail -f input/app.log
```

按 `Ctrl+C` 停止。它会持续占用终端，不是命令卡死。

## 2.6 使用 grep 过滤

```bash
grep -n 'ERROR' input/app.log
grep -Ei 'ERROR|WARN' input/app.log
grep -v 'INFO' input/app.log
grep -c 'ERROR' input/app.log
```

没有匹配时 `grep` 通常返回状态 `1`：

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

## 2.10 本章任务

1. 从样例日志中提取 ERROR 和 WARN 行到 `output/error_lines.txt`。
2. 使用 `wc -l` 验证结果为 2 行。
3. 保留结果供最终调查演习使用。

```bash
test "$(wc -l < output/error_lines.txt)" -eq 2
printf 'exit_status=%s\n' "$?"
```

[上一章：Linux 环境与命令入门](01_linux_intro.md) · [下一章：用户、组与文件权限](03_user_permission.md)
