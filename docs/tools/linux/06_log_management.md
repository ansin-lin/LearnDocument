# 06 日志查看与调查

日志调查的目标不是找到所有 `ERROR`，而是在明确环境和时间范围后，找到与现象相关的证据并区分症状与根因。

## 6.1 先确认调查条件

开始前记录：环境、主机、时间范围、现象、request ID 或批处理 ID、允许访问的日志路径。日志可能位于项目目录、容器平台或 systemd journal，不假定全部在 `/var/log`。

本章使用 `~/learndoc-linux-lab/input/app.log`。

## 6.2 分页和末尾查看

```bash
cd "$HOME/learndoc-linux-lab"
less input/app.log
tail -n 100 input/app.log
```

实时复现时可以使用 `tail -f`，完成后按 `Ctrl+C`。调查既有故障时优先限定时间和行数，避免无目的持续跟踪。

## 6.3 按标识和时间过滤

```bash
grep -n 'request_id=R001' input/app.log
grep -En 'ERROR|WARN' input/app.log
grep '2026-09-01T09:00' input/app.log
```

保存证据：

```bash
grep -n 'request_id=R001' input/app.log > output/R001-evidence.txt
wc -l output/R001-evidence.txt
```

预期有 2 行，能够看到请求开始和错误结果。

## 6.4 journalctl 只读调查

环境使用 systemd 且当前用户有读取权限时：

```bash
journalctl --since '10 minutes ago' --no-pager | tail -n 50
journalctl -u <unit-name> --since '2026-09-01 09:00:00' --until '2026-09-01 09:10:00' --no-pager
```

`<unit-name>` 必须来自项目手顺。权限不足、没有 systemd 或日志保存在其他平台时，记录条件并使用项目指定方法；本课程不要求启停服务。

## 6.5 调查结果模板

在 `output/investigation.md` 中记录：

```markdown
# 调查结果

- 环境/主机：
- 调查时间范围：
- 现象：
- 使用命令：
- 确认结果：
- 根因判断：
- 影响范围：
- 暂定对策：
- 未确认事项（残课题）：
- 证据文件：
```

只写日志能直接支持的结论；推测要明确标成“推测”或“未确认”。公开证据前删除个人信息、访问令牌和业务敏感数据。

## 6.6 常见失败

- 路径不存在：先确认项目规定和当前环境，不遍历整个服务器猜日志位置。
- `Permission denied`：记录 `ls -l` 和当前身份，请负责人授权，不自行改权限。
- 搜不到 ERROR：检查时间、大小写、request ID、日志轮转和目标环境。
- 日志过大：使用 `less`、`tail` 和限定条件，不用 `cat` 打印全部内容。

## 6.7 本章任务

1. 调查 `request_id=R001`。
2. 保存命令输出到 `output/R001-evidence.txt`。
3. 完成 `output/investigation.md`，结论应指出 employee not found。
4. 使用 `grep` 验证报告包含 request ID、影响范围和残课题。

[上一章：用户级定时任务](05_crontab_scheduler.md) · [下一章：Vim 最小编辑操作](07_vi_vim_basic.md)
