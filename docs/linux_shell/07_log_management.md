# 第7章 日志管理

## 系统日志路径

| 文件 | 说明 |
|------|------|
| /var/log/syslog | 系统日志 |
| /var/log/auth.log | 登录与认证日志 |
| /var/log/messages | 通用消息 |
| /var/log/dmesg | 内核启动信息 |

## 查看日志命令

```bash
cat /var/log/syslog
tail -f /var/log/messages
grep "error" /var/log/syslog
```

## 实战脚本：监控 error 日志

```bash
#!/bin/bash
logfile=/var/log/syslog
tail -n 0 -f $logfile | grep --line-buffered "error" >> /tmp/error_watch.log
```

## 练习题

1. 使用 grep 搜索日志中所有 ssh 登录记录。
2. 写脚本实时监控 nginx 日志中出现的 404。
