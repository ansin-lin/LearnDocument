# 02 变量、引用与参数

## 2.1 变量必须安全引用

```bash
log_file="$HOME/learndoc-linux-lab/input/app.log"
printf 'log_file=%s\n' "$log_file"
```

赋值等号两侧不能有空格。使用变量时优先写 `"$log_file"`；未加引号会触发单词分割和通配符展开，带空格或 `*` 的路径可能被拆成多个参数。

## 2.2 命令替换

```bash
line_count="$(wc -l < "$log_file")"
printf 'line_count=%s\n' "$line_count"
```

`$(...)` 执行其中命令并取得标准输出。命令失败状态不会自动被所有外层写法正确处理，关键命令仍需显式检查。

## 2.3 位置参数

创建 `show_args.sh`：

```bash
#!/usr/bin/env bash

printf 'script=%s\n' "$0"
printf 'count=%s\n' "$#"

for argument in "$@"; do
  printf 'argument=%s\n' "$argument"
done
```

- `$0`：脚本调用名称
- `$#`：参数数量
- `$1`、`$2`：第一个、第二个参数
- `"$@"`：逐个保留所有参数的边界

不要用未加引号的 `$@` 遍历路径。

```bash
bash show_args.sh 'input/app.log' 'request id=R001'
```

## 2.4 默认值和必填参数

```bash
log_file="${1:-}"
request_id="${2:-}"

if [[ -z "$log_file" || -z "$request_id" ]]; then
  printf 'Usage: %s <log-file> <request-id>\n' "$0" >&2
  exit 2
fi
```

`${1:-}` 在第一个参数缺失或为空时使用空字符串。`-z` 判断字符串为空。

## 2.5 本章任务

让 `show_args.sh` 在参数不足时输出 Usage 到标准错误并返回 2；正常输入时逐个显示参数。

[上一章：脚本结构与执行方式](01_shell_basic.md) · [下一章：输入输出、重定向与管道](03_io_pipelines.md)
