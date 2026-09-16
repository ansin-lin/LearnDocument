# 03 输入输出、重定向与管道

## 3.1 三种标准流

- 标准输入 stdin：程序读取的数据
- 标准输出 stdout：正常结果
- 标准错误 stderr：诊断和错误

```bash
printf 'normal output\n'
printf 'error output\n' >&2
```

重定向：

```bash
printf 'replace\n' > output/demo.txt
printf 'append\n' >> output/demo.txt
grep 'ERROR' missing.log 2> output/error.log
```

`>` 会覆盖目标，`>>` 会追加；执行前确认路径。

## 3.2 管道与退出状态

```bash
grep -E 'ERROR|WARN' input/app.log | wc -l
```

管道把左侧 stdout 传给右侧 stdin。默认情况下 `$?` 通常只反映最后一个命令，所以左侧失败可能被隐藏。

```bash
set -o pipefail
grep 'ERROR' missing.log | wc -l
printf 'pipeline_status=%s\n' "$?"
```

`pipefail` 使管道中任一命令失败时整条管道返回非 0。它不代替错误说明和恢复处理。

## 3.3 here-document

```bash
cat > input/batch.log <<'EOF'
INFO job_id=J001 start
ERROR job_id=J001 input missing
EOF
```

带引号的 `EOF` 会按原样保存 `$`、反斜杠等字符，适合固定测试数据。

## 3.4 本章任务

把 ERROR/WARN 行输出到 `output/filtered.log`，把错误信息写入独立文件，并验证正常和缺失文件两种情况的退出状态。

[上一章：变量、引用与参数](02_variables_arguments.md) · [下一章：条件、循环与函数](04_control_functions.md)
