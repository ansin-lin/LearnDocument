# 04 进程、退出状态与资源确认

PG 需要确认应用或批处理是否运行，并安全处理自己启动的练习进程。本章不包含系统服务启停。

## 4.1 进程和 PID

进程是正在运行的程序，每个进程有 PID。查看自己的进程：

```bash
ps -f
ps -u "$(whoami)" -f
```

常见字段包括 UID、PID、PPID 和 CMD。查找时优先使用 `pgrep`，避免把 `grep` 自身算进去：

```bash
pgrep -a -u "$(id -u)" bash
```

## 4.2 建立可控练习进程

```bash
sleep 300 &
demo_pid=$!
printf 'demo_pid=%s\n' "$demo_pid"
ps -p "$demo_pid" -o pid,ppid,user,stat,etime,cmd
```

`&` 让命令在后台运行，`$!` 是最近启动的后台进程 PID。只对刚刚保存的 `demo_pid` 操作，不复制其他环境的 PID。

## 4.3 安全结束进程

```bash
kill -TERM "$demo_pid"
wait "$demo_pid"
printf 'wait_status=%s\n' "$?"
```

`TERM` 请求进程正常结束，使其有机会清理资源。只有负责人确认进程无法响应、影响和恢复方法都明确时，才考虑 `KILL`；不要在共享环境按名称批量结束进程。

确认进程已结束：

```bash
ps -p "$demo_pid"
printf 'ps_status=%s\n' "$?"
```

`ps` 返回非 0 表示没有找到该 PID。

## 4.4 命令退出状态

每条命令都会返回退出状态，必须紧接命令读取：

```bash
grep 'ERROR' input/app.log
status=$?
printf 'grep_status=%s\n' "$status"
```

后续任何命令都会覆盖 `$?`，所以要立即保存。脚本应根据命令文档解释状态，而不是把所有非 0 都当成同一种错误。

## 4.5 只读资源确认

```bash
uptime
free -h
df -h "$HOME"
du -sh "$HOME/learndoc-linux-lab"
```

这些命令分别帮助确认负载概况、内存、主目录所在文件系统容量和练习目录大小。PG 只负责记录异常现象；扩容、挂载和系统调优不在本课程范围。

## 4.6 本章任务

1. 启动一个 `sleep 300` 后台进程。
2. 使用保存的 PID 显示进程信息。
3. 使用 `TERM` 结束并验证进程不存在。
4. 把 PID、结束前状态和验证结果写入 `output/process-evidence.txt`。

[上一章：用户、组与文件权限](03_user_permission.md) · [下一章：用户级定时任务](05_crontab_scheduler.md)
