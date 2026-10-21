# 03 输入输出、重定向与管道

## 3.1 三种标准流

- 标准输入 stdin：程序读取的数据
- 标准输出 stdout：正常结果
- 标准错误 stderr：诊断和错误

```bash
printf 'normal output\n'
printf 'error output\n' >&2
```

第一条写到 stdout，第二条的 `>&2` 把同样的文字送到 stderr。屏幕上可能看起来都一样，但调用脚本或定时任务时可以分别保存。`stdin` 是程序从外部读取内容的入口；例如后面的 `wc -l` 可以从管道接收文本。

重定向：

```bash
printf 'replace\n' > output/demo.txt
printf 'append\n' >> output/demo.txt
grep 'ERROR' missing.log 2> output/error.log
```

`>` 会覆盖目标，`>>` 会追加；执行前确认路径。
`2>` 只保存 stderr：示例里 `missing.log` 不存在，`grep` 的错误信息进入 `output/error.log`，不会进入 `output/demo.txt`。执行后分别用 `cat` 查看两份文件；`grep` 会返回非 0，这是刻意制造的失败。

顺序上先确认 `>` 覆盖和 `>>` 追加，再用 `2>` 单独收集错误，最后才看把两种输出合流的 `2>&1`。例如下节组合命令里的数据流是：

```text
grep 读取日志 → 匹配行走 stdout → > 写入 search.log
                         错误走 stderr → 2>&1 指向同一文件
```

需要把正常输出与错误输出保存到同一份**练习文件**时，可这样读重定向顺序：

```bash
grep 'ERROR' input/app.log > output/search.log 2>&1
grep 'FATAL' missing.log >> output/search.log 2>&1
```

第一条先用 `>` 覆盖 `output/search.log`，再用 `2>&1` 让 stderr 去 stdout 当前指向的位置；第二条用 `>>` 追加，因文件缺失，错误文字也会追加进去。顺序会影响结果：先写 `2>&1` 再写 `> 文件`，stderr 仍可能留在原终端。执行后用 `cat output/search.log` 核对；不要把生产错误输出直接写入未经授权的共享目录。

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
上例中 `grep` 读不到文件，而 `wc -l` 仍可能输出 `0`；`set -o pipefail` 后，紧接着读取 `$?` 才能看出整条管道失败。这个设置只影响当前 Bash 会话或脚本，不能修复缺失的文件。

既想在终端看，又想保存，可用 `tee`：

```bash
printf 'hello\n' | tee output/example.txt
printf 'again\n' | tee -a output/example.txt
```

`tee [选项] 文件` 从标准输入读取，同时输出到屏幕和文件；默认覆盖目标，`-a` 才追加。`tee` 只保存进入管道的 stdout，左侧命令的 stderr 仍需另行处理；不要误以为看到屏幕输出就代表整个管道成功。

## 3.3 here-document

```bash
cat > input/batch.log <<'EOF'
INFO job_id=J001 start
ERROR job_id=J001 input missing
EOF
```

带引号的 `EOF` 会按原样保存 `$`、反斜杠等字符，适合固定测试数据。
`cat > input/batch.log` 会创建或覆盖该文件；从下一行到单独的 `EOF` 都是要写入的内容。执行后用 `cat input/batch.log` 确认恰好两行。不要把已有项目日志用此方法覆盖。

## 3.4 本章任务

把 ERROR/WARN 行输出到 `output/filtered.log`，把错误信息写入独立文件，并验证正常和缺失文件两种情况的退出状态。

[上一章：变量、引用与参数](02_variables_arguments.md) · [下一章：条件、循环与函数](04_control_functions.md)

本章自查：**必须掌握**三种标准流、`>`、`>>`、`2>`、管道；**应当掌握**`2>&1`、`tee` 与 `pipefail`；**了解即可**重定向顺序的组合细节。

课堂检查：① stdin、stdout、stderr 各传什么？② `>` 与 `>>` 对已有文件有什么不同？③ `grep ... > file 2>&1` 中错误输出去哪？④ `grep ... | wc -l` 输出 0 能证明没有错误吗？参考：输入／正常结果／错误；覆盖／追加；同一文件；不能，还需检查左侧状态。
