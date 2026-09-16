# 06 日志检查脚本增量实现

本章把前五章内容组合为 `check_log.sh`。开始前应已有 Linux 课程中的工作目录和 `input/app.log`；脚本本身放在 `~/learndoc-linux-lab`。

## 6.1 规格

调用方式：

```text
bash check_log.sh <log-file> <request-id>
```

脚本必须满足：

- 恰好接收两个参数。
- 输入必须是当前用户可读的普通文件。
- request ID 只能包含英文字母、数字、下划线和连字符。
- 只提取包含 `request_id=<指定值>` 的行，并保留行号。
- 结果写入脚本所在目录的 `output/<request-id>.log`。
- 不修改输入日志；重复执行不会追加重复记录。
- 使用第 5 章定义的退出状态。

## 6.2 完整实现

```bash
#!/usr/bin/env bash

set -u
set -o pipefail

tmp_file=""

print_usage() {
  printf 'Usage: %s <log-file> <request-id>\n' "$0" >&2
}

write_message() {
  local level="$1"
  local message="$2"
  printf '%s level=%s message=%s\n' \
    "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$level" "$message" >&2
}

cleanup() {
  if [[ -n "$tmp_file" && -f "$tmp_file" ]]; then
    rm -- "$tmp_file"
  fi
}

trap cleanup EXIT

if [[ $# -ne 2 ]]; then
  print_usage
  exit 2
fi

log_file="$1"
request_id="$2"

if [[ ! -f "$log_file" || ! -r "$log_file" ]]; then
  write_message ERROR "log file is missing or unreadable: $log_file"
  exit 3
fi

if [[ ! "$request_id" =~ ^[A-Za-z0-9_-]+$ ]]; then
  write_message ERROR "invalid request ID: $request_id"
  exit 2
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)" || {
  write_message ERROR 'cannot determine script directory'
  exit 4
}
output_dir="$script_dir/output"
output_file="$output_dir/$request_id.log"

if ! mkdir -p -- "$output_dir"; then
  write_message ERROR "cannot create output directory: $output_dir"
  exit 4
fi

tmp_file="$(mktemp "$output_dir/.check-log.XXXXXX")" || {
  write_message ERROR "cannot create temporary file in: $output_dir"
  exit 4
}

if grep -n -F "request_id=$request_id" "$log_file" > "$tmp_file"; then
  line_count="$(wc -l < "$tmp_file")"
  if ! mv -- "$tmp_file" "$output_file"; then
    write_message ERROR "cannot save output: $output_file"
    exit 4
  fi
  tmp_file=""
  printf 'request_id=%s matches=%s output=%s\n' \
    "$request_id" "$line_count" "$output_file"
  exit 0
else
  grep_status=$?
  if [[ $grep_status -eq 1 ]]; then
    write_message WARN "no matching record: request_id=$request_id"
    exit 1
  fi
  write_message ERROR "log search failed: status=$grep_status file=$log_file"
  exit 3
fi
```

`${BASH_SOURCE[0]}` 表示当前 Bash 脚本文件。由它计算输出目录，可避免调用者从不同目录执行时把结果写到意外位置。`grep -F` 把搜索内容视为固定文本，不把 request ID 当作正则表达式。

## 6.3 准备测试数据

如果没有 Linux 课程的日志，可在练习目录执行：

```bash
mkdir -p input output
cat > input/app.log <<'EOF'
2026-09-01T09:00:00+0900 INFO request_id=R001 start
2026-09-01T09:00:01+0900 ERROR request_id=R001 database timeout
2026-09-01T09:01:00+0900 INFO request_id=R002 start
EOF
```

## 6.4 四类验证

```bash
bash -n check_log.sh

bash check_log.sh input/app.log R001
printf 'status=%s\n' "$?"
wc -l output/R001.log

bash check_log.sh input/app.log R999
printf 'status=%s\n' "$?"

bash check_log.sh missing.log R001
printf 'status=%s\n' "$?"

bash check_log.sh input/app.log 'R 001'
printf 'status=%s\n' "$?"
```

预期分别为：成功状态 0 且输出 2 行；无匹配状态 1；文件错误状态 3；参数格式错误状态 2。

再次执行成功用例，并确认 `output/R001.log` 仍是 2 行；再执行：

```bash
find output -maxdepth 1 -name '.check-log.*' -print
```

应没有临时文件输出。若系统安装了 ShellCheck，再运行 `shellcheck check_log.sh`；没有安装则在测试记录中写“未执行（环境未提供）”。

## 6.5 完成标准

- 规格中的每一项都能对应到代码和测试证据。
- 原始日志的行数和内容没有变化。
- 成功、无数据、输入错误和输出错误可由不同退出状态区分。
- 错误消息可定位对象，但没有泄露敏感信息。

[上一章：错误处理、日志与清理](05_error_logging.md) · [下一章：日本项目改修与交付](07_project_task.md)
