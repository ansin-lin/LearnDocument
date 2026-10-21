# 04 条件、循环与函数

本章只学习日志检查脚本真正会使用的控制结构。目标不是背语法，而是让脚本能判断输入、逐项处理参数，并把重复逻辑收进函数。

## 4.1 条件判断

先在练习目录执行一个最小判断：

```bash
if [[ -f input/app.log ]]; then
  printf 'log exists\n'
else
  printf 'log missing\n' >&2
fi
```

`if` 询问条件是否成立；`[[ -f 路径 ]]` 在路径是普通文件时成立，`then` 执行成功分支，`else` 执行另一分支，`fi` 结束判断。已有第 02 章的日志时应输出 `log exists`，文件不会被修改。脚本参数校验再把这个判断扩展为多个条件：

```text
判断 -f 是否成立 ├─ 是 → then：输出 log exists
                 └─ 否 → else：输出 log missing
```

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
`if ...; then ... fi` 表示条件成立时执行中间命令；`!` 把条件取反。三个判断分别处理“没给路径”“不是普通文件”“当前用户不可读”。`-f`、`-r` 只查看状态，不修改文件；失败时用 `exit` 返回不同状态，调用方才能区分参数问题和输入文件问题。

## 4.2 精确校验 request ID

课程约定 request ID 只能包含英文字母、数字、下划线和连字符：

```bash
request_id="${2:-}"

if [[ ! "$request_id" =~ ^[A-Za-z0-9_-]+$ ]]; then
  printf 'ERROR: invalid request ID: %s\n' "$request_id" >&2
  exit 2
fi
```

`=~` 使用正则表达式进行判断。这里先校验输入格式；真正搜索日志时还要限定字段前后边界，避免把相似编号误认为同一个 request ID。
`^` 和 `$` 分别表示字符串开头、结尾，`+` 表示前面的字符集合至少出现一次；因此空 ID、空格和斜杠都不能通过。`"$request_id"` 是待检查的值，右侧正则表达式不要整体加引号。后续搜索还要检查字段边界，不能只凭固定字符串判断完整 ID。

## 4.3 循环

`for` 适合逐个处理参数：

```bash
for name in R001 R002 R003; do
  printf 'checking=%s\n' "$name"
done
```

`for` 从列表依次取值：第一次 `name=R001`，之后变为 R002、R003，列表用尽后结束；`do` 到 `done` 是每次重复执行的部分。再把固定列表换成脚本收到的参数：

```text
取 R001 → 输出 checking=R001 → 取 R002 → 输出 checking=R002
       → 取 R003 → 输出 checking=R003 → 列表用尽，结束
```

```bash
for request_id in "$@"; do
  printf 'checking=%s\n' "$request_id"
done
```

第 02 章的 `show_args.sh` 此时可升级：把原来打印 `first`、`second` 的两行换成上述循环，传入三个参数并核对三行输出；`"$@"` 保留每个参数的边界，含空格的一个参数不会变成两轮。若还要拒绝参数不足，再把第 02 章的 `if [[ $# ... ]]` 片段放在循环前。

`while read` 适合逐行读取文本：

```bash
IFS= read -r line < input/app.log
printf 'first=%s\n' "$line"
```

`read` 从标准输入读取一行并存入 `line`；`< input/app.log` 使其读取文件首行，不等待键盘输入。`-r` 保留反斜杠原样，`IFS=` 保留行首尾空白。要继续读到文件结束，再放进 `while`：

```bash
while IFS= read -r line; do
  printf 'line=%s\n' "$line"
done < input/app.log
```

`IFS=` 保留行首、行尾空白，`-r` 防止反斜杠被当作转义字符。处理日志时不要写成 `for line in $(cat file)`，那会按空白拆散内容。
`done < input/app.log` 把日志文件逐行送入循环；每一行由变量 `line` 暂存。`for` 更适合处理参数列表，`while read` 更适合处理文件行。它们都只是读取和打印数据，不改原文件。

```text
read 取得一行 → 读取成功（空行也算）：打印 line → 再 read
             → 到达文件末尾：退出 while
```

## 4.4 函数与局部变量

先定义并调用无参数函数：

```bash
hello() {
  printf 'hello\n'
}
hello
```

`hello() { ...; }` 是 Bash 函数定义，不是独立的 Linux 命令；写出函数名 `hello` 才执行，预期输出 `hello`。需要复用带变化的数据时再传参数：

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
定义函数时不会立即运行；写 `write_message ERROR 'file missing'` 才会调用。函数内的 `$1`、`$2` 分别是这次调用传入的级别和消息；`date` 生成执行时刻，`>&2` 把消息送到 stderr。
函数中的 `return 0` 只结束**本次函数调用**并返回状态，后面的脚本还可以继续；`exit 2` 会结束**整个脚本**，把 2 交给调用者。两者传递的是退出状态，不是文字结果。

```text
定义 write_message（此时不输出）→ 调用 write_message ERROR 'file missing'
                               → $1=ERROR、$2=file missing → 输出消息 → 返回调用处
```

## 4.5 本章任务

创建 `validate_input.sh`，要求：

1. 只接受日志路径和 request ID 两个参数。
2. 参数数量错误时显示 Usage，返回 2。
3. 文件不存在或不可读时返回 3。
4. request ID 格式不正确时返回 2。
5. 正常时显示 `input valid`，返回 0。

分别使用正常文件、缺失文件和 `R 001` 验证三个分支，并用 `$?` 记录退出状态。
再写一个短函数 `print_id`，用 `for id in "$@"` 逐项调用它，验证两个 ID 分别输出；该练习只打印参数，不修改日志。

[上一章：输入输出、重定向与管道](03_io_pipelines.md) · [下一章：错误处理、日志与清理](05_error_logging.md)

本章自查：**必须掌握**简单 `if` 与 `for`；**应当掌握**逐行 `while read` 和简单函数；**了解即可**更复杂的正则判断。

课堂检查：① 样例日志存在时 `if` 走哪条分支？② `for name in R001 R002 R003` 会打印几行？③ 为何逐行读日志不写 `for line in $(cat file)`？④ 只定义函数而不调用会输出吗？参考：`then`；三行；后者会按空白拆行；不会。
