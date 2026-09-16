# 04 条件、循环与函数

本章只学习日志检查脚本真正会使用的控制结构。目标不是背语法，而是让脚本能判断输入、逐项处理参数，并把重复逻辑收进函数。

## 4.1 条件判断

`[[ ... ]]` 是 Bash 的条件语法。常用文件判断如下：

```bash
log_file="${1:-}"

if [[ -z "$log_file" ]]; then
  printf 'ERROR: log file is required\n' >&2
  exit 2
fi

if [[ ! -f "$log_file" ]]; then
  printf 'ERROR: not a regular file: %s\n' "$log_file" >&2
  exit 3
fi

if [[ ! -r "$log_file" ]]; then
  printf 'ERROR: file is not readable: %s\n' "$log_file" >&2
  exit 3
fi
```

- `-z`：字符串为空
- `-f`：路径存在且是普通文件
- `-r`：当前用户可读
- `!`：对判断结果取反

路径存在不等于可读，因此项目脚本要分别确认文件类型和权限。

## 4.2 精确校验 request ID

课程约定 request ID 只能包含英文字母、数字、下划线和连字符：

```bash
request_id="${2:-}"

if [[ ! "$request_id" =~ ^[A-Za-z0-9_-]+$ ]]; then
  printf 'ERROR: invalid request ID: %s\n' "$request_id" >&2
  exit 2
fi
```

`=~` 使用正则表达式进行判断。这里校验的是输入格式；真正搜索日志时将使用 `grep -F` 按普通文本匹配，避免把输入再次解释为正则表达式。

## 4.3 循环

`for` 适合逐个处理参数：

```bash
for request_id in "$@"; do
  printf 'checking=%s\n' "$request_id"
done
```

`while read` 适合逐行读取文本：

```bash
while IFS= read -r line; do
  printf 'line=%s\n' "$line"
done < input/app.log
```

`IFS=` 保留行首、行尾空白，`-r` 防止反斜杠被当作转义字符。处理日志时不要写成 `for line in $(cat file)`，那会按空白拆散内容。

## 4.4 函数与局部变量

```bash
print_usage() {
  printf 'Usage: %s <log-file> <request-id>\n' "$0" >&2
}

write_message() {
  local level="$1"
  local message="$2"
  printf '%s level=%s message=%s\n' \
    "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$level" "$message" >&2
}
```

函数通过参数接收数据。`local` 将变量限制在函数内，减少它意外覆盖脚本其他变量的风险。函数应只承担一个明确职责。

## 4.5 本章任务

创建 `validate_input.sh`，要求：

1. 只接受日志路径和 request ID 两个参数。
2. 参数数量错误时显示 Usage，返回 2。
3. 文件不存在或不可读时返回 3。
4. request ID 格式不正确时返回 2。
5. 正常时显示 `input valid`，返回 0。

分别使用正常文件、缺失文件和 `R 001` 验证三个分支，并用 `$?` 记录退出状态。

[上一章：输入输出、重定向与管道](03_io_pipelines.md) · [下一章：错误处理、日志与清理](05_error_logging.md)
