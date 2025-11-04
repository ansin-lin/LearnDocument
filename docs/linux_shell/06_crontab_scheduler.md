# 第6章 定时任务管理

## crontab 基础

**作用**：在指定时间自动执行任务。

### 常用命令

```bash
crontab -e    # 编辑任务
crontab -l    # 查看任务
crontab -r    # 删除任务
```

### 时间格式

`分 时 日 月 周 命令`
例如：

```bash
0 2 * * * /usr/bin/backup.sh    # 每天凌晨2点执行
*/5 * * * * echo "Hello" >> /tmp/test.log
```

## 实战练习

- 每小时输出当前时间到日志文件：

```bash
echo "date >> /tmp/hourly.log" | crontab -
```

- 每天凌晨备份指定目录：

```bash
0 0 * * * tar -czf /backup/home_$(date +\%F).tar.gz /home
```

## 练习题

- 编写 crontab 任务，每 10 分钟记录一次 CPU 使用率到文件。
