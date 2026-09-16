# 05 用户级定时任务

cron 常用于定时执行批处理。本章只维护当前普通用户的练习任务，不管理 cron 服务，也不修改其他用户或系统级任务。

## 5.1 先备份，再编辑

```bash
mkdir -p "$HOME/learndoc-linux-lab/output"
crontab -l > "$HOME/learndoc-linux-lab/output/crontab.backup" 2>/dev/null || true
crontab -e
```

`crontab -l` 在没有既有任务时可能返回非 0；这里的 `|| true` 只用于允许“当前无任务”继续。不要使用 `crontab -r` 删除全部任务。

恢复已有备份前必须先阅读内容：

```bash
cat "$HOME/learndoc-linux-lab/output/crontab.backup"
crontab "$HOME/learndoc-linux-lab/output/crontab.backup"
```

第二条会替换当前用户的全部 crontab，只在确认备份正确且确需恢复时执行。

## 5.2 时间格式

```text
分钟 小时 日 月 星期 命令
```

```text
*/5 * * * *  每 5 分钟
0 2 * * *    每天 02:00
0 9 * * 1    每周一 09:00
```

日期、星期同时受限时的组合行为容易误解，复杂生产计划应按项目手顺确认，不凭记忆配置。

## 5.3 创建安全练习脚本

先完成 Shell 基础后再执行本节。创建 `~/learndoc-linux-lab/cron_demo.sh`：

```bash
cat > "$HOME/learndoc-linux-lab/cron_demo.sh" <<'EOF'
#!/usr/bin/env bash
printf '%s cron demo\n' "$(date '+%Y-%m-%dT%H:%M:%S')" \
  >> "$HOME/learndoc-linux-lab/output/cron-demo.log"
EOF
chmod u+x "$HOME/learndoc-linux-lab/cron_demo.sh"
```

先手动验证：

```bash
"$HOME/learndoc-linux-lab/cron_demo.sh"
tail -n 1 "$HOME/learndoc-linux-lab/output/cron-demo.log"
```

手动运行成功后，才在 `crontab -e` 中添加：

```text
*/5 * * * * /usr/bin/env bash "$HOME/learndoc-linux-lab/cron_demo.sh" >> "$HOME/learndoc-linux-lab/output/cron-run.log" 2>&1
```

cron 通常会设置 `HOME`，但它的 `PATH` 等执行环境比交互式终端更少。实际项目应按作业手顺确认执行用户、解释器、环境变量和绝对路径；排错时不能只以“手动执行成功”为依据。

## 5.4 验证与排错

```bash
crontab -l
tail -n 20 "$HOME/learndoc-linux-lab/output/cron-run.log"
tail -n 20 "$HOME/learndoc-linux-lab/output/cron-demo.log"
```

常见原因：绝对路径错误、脚本手动执行就失败、环境变量不同、输出未重定向、服务器时区与预期不同。先验证脚本，再调查调度。

## 5.5 清理练习任务

使用 `crontab -e` 只删除本章添加的一行，然后运行 `crontab -l` 确认其他任务仍保留。不要用删除全部任务的命令代替编辑。

## 5.6 本章任务

1. 备份当前用户 crontab。
2. 手动运行练习脚本并确认日志。
3. 添加练习任务，保存 `crontab -l` 作为证据。
4. 删除练习行并确认其他任务未变化。

[上一章：进程、退出状态与资源确认](04_process_system.md) · [下一章：日志查看与调查](06_log_management.md)
