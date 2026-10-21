# Linux 命令速查表

本页只收录 Web/批处理项目 PG 在授权范围内常用的命令。执行修改类命令前先确认 `whoami`、`hostname`、`pwd` 和目标路径。
第一次使用时先阅读对应章节，理解输入、结果和风险；这里用于回看写法，不代替正文解释。尖括号内是必须替换的占位内容。
需要逐项查看功能、语法、常用选项、项目场景和风险时，参见两门课共用的 [Linux 与 Bash 新人速查表](../shell/linux_shell_quick_reference.md)。

## 环境与路径

```bash
whoami
hostname
pwd
cat /etc/os-release
ls -la -- <path>
ls -ld -- <directory>
```

## 文件与目录

```bash
mkdir -p <directory>
cp -- <source> <destination>
mv -- <source> <destination>
touch <file>
rm -- <confirmed-file>
rmdir <empty-directory>
```

本速查表不提供递归强制删除。递归删除必须按项目作业手顺确认范围、备份和恢复方式。

## 文本与搜索

```bash
cat <small-file>
less <file>
head -n 20 <file>
tail -n 100 <file>
tail -f <log-file>
grep -n '<keyword>' <file>
grep -Ei 'ERROR|WARN' <file>
find <limited-directory> -type f -name '*.log' -print
wc -l <file>
diff -u <old-file> <new-file>
```

## 权限确认

```bash
id
groups
ls -l -- <file>
chmod 600 <own-practice-file>
chmod u+x <own-script>
```

共享环境遇到权限错误先报告，不使用 `777`、递归 chmod/chown 或 sudo 绕过。

## 进程和状态

```bash
ps -u "$(whoami)" -f
pgrep -a -u "$(id -u)" <name>
ps -p <pid> -o pid,ppid,user,stat,etime,cmd
kill -TERM <confirmed-own-pid>
printf 'exit_status=%s\n' "$?"
uptime
free -h
df -h "$HOME"
du -sh <limited-directory>
```

## 日志调查

```bash
grep -n 'request_id=<id>' <log-file>
grep -En 'ERROR|WARN' <log-file>
journalctl -u <known-unit> --since '<start>' --until '<end>' --no-pager
```

日志路径、服务名和读取权限以项目手顺为准。

## 用户级 cron

```bash
crontab -l
```

`crontab -l` 只读查看当前用户任务；`crontab -e` 会修改实际调度，仅在独立练习环境和项目授权下使用，详见[选学章节](05_crontab_scheduler.md)。

## Vim 最小操作

| 操作 | 按键 |
|---|---|
| 插入 | `i` |
| 返回普通模式 | `Esc` |
| 保存退出 | `:wq` |
| 放弃修改 | `:q!` |
| 查找 | `/keyword` |
| 删除/复制/粘贴行 | `dd` / `yy` / `p` |

[返回 Linux 课程入口](index.md)
