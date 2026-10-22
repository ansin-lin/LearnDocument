# 附录：日志检查脚本 `check_log.sh`（旧案例）

本页保留原课程的 `check_log.sh` 日志调查案例，供已学完 Batch 主线后选读。它使用独立的 `input/app.log`，不属于销售数据导入案例；请从本页的测试数据开始。正文中提到的旧章节编号仅用于理解原案例的增量过程，不是当前课程导航。

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
- 日志中的 request ID 作为独立字段出现，前后是行边界或空白；只提取该字段的值**完整等于**指定 ID 的行，并保留行号。
- 结果写入脚本所在目录的 `output/<request-id>.log`。
- 不修改输入日志；重复执行不会追加重复记录。
- 无匹配时不发布新结果；若同名旧结果已存在，应明确提示它属于先前运行，不能作为本次证据。
- 使用第 5 章定义的退出状态。

## 6.2 先看最小处理过程

以下代码仅用于理解参数传递与 `grep` 调用，**不满足最终需求；学习此片段时不要求创建或运行最终的 `check_log.sh`**。它未处理参数缺失、相似 ID、读写失败等情况，不能用于真实项目：

```bash
# 从第一个参数读取日志路径。
log_file="$1"
# 从第二个参数读取请求 ID。
request_id="$2"
# 按普通文字搜索并显示行号；这个片段还不能排除相似 ID。
grep -n -F "request_id=$request_id" "$log_file"
```

等第 6.5 节创建完整脚本后，运行 `bash check_log.sh input/app.log R001` 时，第一个参数成为 `log_file`，第二个成为 `request_id`；Bash 展开变量后，`grep` 读取日志并输出带行号的匹配行。`-F` 只保证“按普通文字搜索”，会把 `R0012` 中的 `R001` 也算进去，所以它只用于看清数据流，不是完成规格的版本。

## 6.3 再补输入判断与精确匹配

正式搜索前，脚本必须检查**参数数量恰好为 2**、日志是可读普通文件、ID 只含允许的字符；各项失败分别返回第 05 章约定的状态。核心判断片段如下，仍不是完整脚本：

```bash
# 参数数量不为二时进入错误分支。
if [[ $# -ne 2 ]]; then
  # 用状态 2 拒绝错误调用。
  exit 2
# 结束数量判断。
fi
# 保存已经确认存在的第一个参数。
log_file="$1"
# 保存第二个参数作为目标 ID。
request_id="$2"
# 拒绝不存在、非普通文件或不可读的日志。
if [[ ! -f "$log_file" || ! -r "$log_file" ]]; then
  # 文件错误返回状态 3。
  exit 3
# 结束文件判断。
fi
# 只接受字母、数字、下划线和连字符组成的 ID。
if [[ ! "$request_id" =~ ^[A-Za-z0-9_-]+$ ]]; then
  # ID 格式错误返回状态 2。
  exit 2
# 结束 ID 判断。
fi
# 搜索前后为行边界或空白的完整 ID，并保留原始行号。
grep -n -E "(^|[[:space:]])request_id=${request_id}([[:space:]]|$)" "$log_file"
```

这里的 `-ne 2` 表示“不等于 2”；`-f`、`-r` 分别查普通文件与可读性；`=~` 校验 ID。最后一行要求 `request_id=R001` 前后是空白或行边界，所以不会把 `R0012` 当成 R001。片段省略了可理解的错误消息、正式输出和搜索错误分支，**不要把它当成最终脚本**。

## 6.4 最终版本的数据流

```text
启动 → 检查参数 → 检查文件和 ID → 确定脚本旁的 output 目录
     → 建立临时结果 → grep 搜索完整 ID
        ├─ 匹配：移动为正式结果，返回 0
        ├─ 无匹配：不发布新结果，提示旧结果风险，返回 1
        └─ 执行错误：说明原因，返回 3 或 4
     → 退出时清理仍存在的临时文件
```

| 变量 | 来源 | 在脚本中的作用 |
|---|---|---|
| `log_file` | 第一个参数 | 待读日志路径 |
| `request_id` | 第二个参数 | 搜索目标和输出文件名的一部分 |
| `script_dir` | 当前脚本自身路径 | 使输出位置不依赖调用者的工作目录 |
| `output_dir` | `script_dir/output` | 正式结果与临时文件所在目录 |
| `output_file` | `output_dir` 与 ID | 成功时发布的结果文件 |
| `tmp_file` | `mktemp` 生成 | 失败时可清理的临时结果 |

## 6.5 完整实现

把下面**完整文件内容**保存为练习目录中的 `check_log.sh`。执行前核对文件路径；不要覆盖已有项目脚本。

```bash
#!/usr/bin/env bash

# 未设置变量被引用时立即报错。
set -u
# 管道前段失败也反映到整体状态。
set -o pipefail

# 初始没有临时文件，供退出清理函数判断。
tmp_file=""

# 定义用法提示函数；此时不执行函数体。
print_usage() {
  # 将调用格式写到标准错误。
  printf 'Usage: %s <log-file> <request-id>\n' "$0" >&2
# 结束用法提示函数。
}

# 定义带时间、级别和内容的错误消息函数。
write_message() {
  # 保存本次消息的级别。
  local level="$1"
  # 保存本次消息的文本。
  local message="$2"
  # 将格式化的消息写到标准错误；反斜杠表示命令延续到下一行。
  printf '%s level=%s message=%s\n' \
    "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$level" "$message" >&2 # 用时间、级别和文本填充格式串。
# 结束消息函数。
}

# 定义退出时删除本次临时文件的函数。
cleanup() {
  # 只在临时路径非空且文件仍存在时删除。
  if [[ -n "$tmp_file" && -f "$tmp_file" ]]; then
    # 删除脚本自己创建的临时文件。
    rm -- "$tmp_file"
  # 结束临时文件检查。
  fi
# 结束清理函数。
}

# 登记脚本退出时执行 cleanup；此行不立即调用它。
trap cleanup EXIT

# 必须恰好收到日志路径和请求 ID 两个参数。
if [[ $# -ne 2 ]]; then
  # 输出用法提示。
  print_usage
  # 参数数量错误返回状态 2。
  exit 2
# 结束数量判断。
fi

# 保存日志路径。
log_file="$1"
# 保存请求 ID。
request_id="$2"

# 检查日志是当前账号可读的普通文件。
if [[ ! -f "$log_file" || ! -r "$log_file" ]]; then
  # 指出不能读取的路径。
  write_message ERROR "log file is missing or unreadable: $log_file"
  # 文件错误返回状态 3。
  exit 3
# 结束日志文件检查。
fi

# 拒绝超出允许字符范围的请求 ID。
if [[ ! "$request_id" =~ ^[A-Za-z0-9_-]+$ ]]; then
  # 报告无效 ID。
  write_message ERROR "invalid request ID: $request_id"
  # 参数错误返回状态 2。
  exit 2
# 结束 ID 检查。
fi

# 计算脚本所在目录；失败则进入花括号中的错误处理。
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)" || {
  # 报告无法定位脚本目录。
  write_message ERROR 'cannot determine script directory'
  # 输出位置无法确定时返回状态 4。
  exit 4
# 结束目录定位失败分支。
}
# 将结果目录固定在脚本旁边。
output_dir="$script_dir/output"
# 用请求 ID 构造正式结果路径。
output_file="$output_dir/$request_id.log"

# 防止输入日志与现有输出文件实际指向同一文件。
if [[ "$log_file" -ef "$output_file" ]]; then
  # 报告输入输出冲突。
  write_message ERROR 'input log and output file must be different'
  # 返回输入路径错误状态 3。
  exit 3
# 结束同文件检查。
fi

# 创建输出目录；创建失败进入错误分支。
if ! mkdir -p -- "$output_dir"; then
  # 报告无法创建的目录。
  write_message ERROR "cannot create output directory: $output_dir"
  # 输出准备失败返回状态 4。
  exit 4
# 结束目录创建检查。
fi

# 在输出目录创建唯一的临时结果文件。
tmp_file="$(mktemp "$output_dir/.check-log.XXXXXX")" || {
  # 报告临时文件创建失败。
  write_message ERROR "cannot create temporary file in: $output_dir"
  # 输出准备失败返回状态 4。
  exit 4
# 结束临时文件创建失败分支。
}

# 将完整 ID 的匹配行及行号先写进临时文件。
if grep -n -E "(^|[[:space:]])request_id=${request_id}([[:space:]]|$)" "$log_file" > "$tmp_file"; then
  # 统计本次临时结果中的匹配行数。
  line_count="$(wc -l < "$tmp_file")"
  # 仅在搜索成功后把临时文件发布成正式结果。
  if ! mv -- "$tmp_file" "$output_file"; then
    # 报告结果发布失败。
    write_message ERROR "cannot save output: $output_file"
    # 返回输出错误状态 4。
    exit 4
  # 结束移动结果检查。
  fi
  # 正式文件已发布，清空变量避免退出清理误删。
  tmp_file=""
  # 把 ID、匹配数和结果路径打印给调用者；反斜杠续行。
  printf 'request_id=%s matches=%s output=%s\n' \
    "$request_id" "$line_count" "$output_file" # 用实际 ID、行数和路径填充格式串。
  # 搜索并发布成功，返回状态 0。
  exit 0
# 搜索未成功时进入失败处理。
else
  # 保存 grep 的退出状态，区分无匹配与执行错误。
  grep_status=$?
  # grep 状态 1 表示没有匹配行。
  if [[ $grep_status -eq 1 ]]; then
    # 提醒本次没有命中。
    write_message WARN "no matching record: request_id=$request_id"
    # 如果旧的同名结果仍存在，要明确警告它不是本次证据。
    if [[ -e "$output_file" ]]; then
      # 输出旧结果风险提示。
      write_message WARN "previous result remains; do not use as current evidence: $output_file"
    # 结束旧结果判断。
    fi
    # 无匹配返回状态 1。
    exit 1
  # 结束无匹配判断。
  fi
  # grep 发生其他执行错误时记录状态和文件。
  write_message ERROR "log search failed: status=$grep_status file=$log_file"
  # 搜索执行错误返回状态 3。
  exit 3
# 结束搜索成功/失败分支。
fi
```

`${BASH_SOURCE[0]}` 表示当前 Bash 脚本文件。`dirname` 取得所在目录，再由 `cd` 和 `pwd` 算出绝对路径，使结果固定写到脚本旁边的 `output`。`mkdir -p` 确保输出目录存在，`mktemp` 创建不会与其他文件重名的临时文件；成功时 `mv` 才把它变成正式结果，失败时 `trap` 清理。`grep -n -E` 使用已经校验过的 ID 搜索独立字段：`-n` 保留行号，`-E` 启用表达式，前后的边界要求为空白或行边界，因此 `R001` 不会误命中 `R0012`。这套边界适用于本课程的空白分隔日志；项目日志格式不同须按实际格式调整。
`-ef` 检查两个路径是否实际指向同一个已有文件，避免把输入日志当成结果覆盖；`exit 3` 报告输入路径不合要求。

按文件从上到下阅读时，分清“定义”和“执行”：

1. `set -u`、`pipefail` 生效，`tmp_file=""` 先准备空变量；此时尚未创建文件。
2. 三个函数只是定义，未调用就不会输出。`trap cleanup EXIT` 登记退出时的清理动作，此时也未执行 `cleanup`。
3. `[[ $# -ne 2 ]]` 先查参数个数。失败时输出 Usage 并 `exit 2`，后面的 `$1`、`$2` 不会被读取；成功才给 `log_file`、`request_id` 赋值。
4. `-f`、`-r` 检查输入文件，失败返回 3；正则检查 ID 字符，失败返回 2。这些检查只读输入，不产生结果文件。
5. 计算 `script_dir`、`output_dir`、`output_file`；若输入与已有输出实际是同一文件，返回 3，避免覆盖输入。创建输出目录失败返回 4。
6. `mktemp` 建立本次第一个**结果文件**，得到 `tmp_file`；若失败返回 4。从此以后脚本退出时会尝试清理此临时文件，正式结果尚未变化。
7. `grep` 把完整 ID 对应的原始行及行号写到临时文件。匹配成功时统计行数，`mv` 成功后才发布正式结果、清空临时路径变量并返回 0；保存失败返回 4。
8. `grep` 返回 1 时不发布新结果，若有同名旧文件就警告并返回 1；其他搜索错误返回 3。失败路径退出时触发 `cleanup`，不把半份文件当成结果。

以第 02 章的四行日志、参数 `input/app.log R001` 为例，跟踪一次成功路径：

| 执行到 | 变量或可观察结果 |
|---|---|
| 启动、检查数量 | 收到 2 个参数，通过检查；尚无新文件 |
| 读取参数 | `log_file=input/app.log`，`request_id=R001` |
| 检查文件与 ID | 普通、可读文件；ID 字符合法 |
| 计算输出位置 | `output_file=脚本所在目录/output/R001.log` |
| 建立临时文件 | `tmp_file` 指向 `output/.check-log.XXXXXX` 的实际生成路径 |
| 搜索 | 命中原日志第 1、2 行，临时文件有 2 行 |
| 发布与结束 | `mv` 生成 `output/R001.log`，打印 `matches=2`，返回 0；清理函数不删除正式结果 |

换成 `R999` 时，搜索走“无匹配”，临时文件在退出时清理，状态为 1；不能把先前留下的同名结果当本次证据。先对着代码找出每阶段的位置，再进入下一节运行测试。

## 6.6 准备测试数据

如果没有 Linux 课程的日志，可在练习目录执行：

```bash
# 创建本附录练习使用的输入和输出目录。
mkdir -p input output
# 把下面四行教学日志写入 input/app.log；重复运行会覆盖该练习文件。
cat > input/app.log <<'EOF'
2026-09-01T09:00:00 INFO  request_id=R001 start
2026-09-01T09:00:01 ERROR request_id=R001 employee not found
2026-09-01T09:01:00 INFO  request_id=R002 completed
2026-09-01T09:02:00 INFO  request_id=R0012 start
EOF
```

在这里，`2026-09-01T09:00:00` 行是 R001 开始，`09:00:01` 行是同一请求的错误，`09:01:00` 行是另一请求 R002 的完成记录，`09:02:00` 行是用来检查“R001 不应误匹配 R0012”的相似 ID。结束标记 `EOF` 不写入日志；不能在这些数据行加 Shell 注释，否则会改变测试数据。

## 6.7 四类验证

```bash
# 只检查脚本语法。
bash -n check_log.sh

# 查找存在的 R001，预期成功。
bash check_log.sh input/app.log R001
# 立即记录成功用例的退出状态。
printf 'status=%s\n' "$?"
# 核对正式结果恰好两行。
wc -l output/R001.log

# 查找不存在的 ID，预期无匹配。
bash check_log.sh input/app.log R999
# 立即记录无匹配状态。
printf 'status=%s\n' "$?"

# 使用不存在的日志路径，验证输入错误。
bash check_log.sh missing.log R001
# 立即记录文件错误状态。
printf 'status=%s\n' "$?"

# 使用包含空格的非法 ID，验证参数检查。
bash check_log.sh input/app.log 'R 001'
# 立即记录参数错误状态。
printf 'status=%s\n' "$?"
```

预期分别为：成功状态 0 且输出 2 行；无匹配状态 1；文件错误状态 3；参数格式错误状态 2。
样例还包含 `R0012`：检查 `output/R001.log` 不应出现该行，可运行 `grep -n 'R0012' output/R001.log`，预期无输出且退出状态为 1。

再次执行成功用例，并确认 `output/R001.log` 仍是 2 行；再执行：

```bash
# 只列出输出目录里本脚本的临时文件；正常结束应无输出。
find output -maxdepth 1 -name '.check-log.*' -print
```

应没有临时文件输出。若系统安装了 ShellCheck，再运行 `shellcheck check_log.sh`；没有安装则在测试记录中写“未执行（环境未提供）”。
ShellCheck 是检查 Shell 脚本常见写法问题的工具；它只能辅助检查，不能代替运行测试。

再用另一份日志查询同一个 `R001`，验证旧结果不会被误用：

```bash
# 准备只含 R002 的练习日志，覆盖时只影响此个人副本。
printf 'INFO request_id=R002 completed\n' > input/only-R002.log
# 用新日志查询 R001，应得到无匹配而非复用旧结果。
bash check_log.sh input/only-R002.log R001
# 立即记录这次查询的状态。
printf 'status=%s\n' "$?"
```

应得到状态 1，并看到“previous result remains”警告；此时旧 `output/R001.log` 不能作为新日志的调查结果。`>` 会创建或覆盖练习文件 `input/only-R002.log`，不要替换成原始日志路径。

最后核对路径边界。先在练习目录建立自己的日志副本，再从另一目录调用同一个脚本：

```bash
# 复制原日志作为含空格文件名的练习副本；已有目标时询问。
cp -i -- input/app.log 'input/app copy.log'
# 记下脚本所在练习目录的绝对路径。
lab_dir="$(pwd)"
# 在子 Shell 中切到家目录，再用绝对路径调用原脚本和输入副本。
(cd "$HOME" && bash "$lab_dir/check_log.sh" "$lab_dir/input/app copy.log" R001)
# 立即记录跨目录调用的退出状态。
printf 'status=%s\n' "$?"
# 回到原练习目录核对脚本旁的结果行数。
wc -l output/R001.log
```

首次复制应确认目标副本不存在；若已存在，`cp -i` 会询问是否覆盖，先核对文件再回答。预期仍返回 0、结果仍为 2 行，结果文件仍写在脚本旁的 `output`，不应出现在 `$HOME/output`。输入不可读、输出目录无法写入等情况受账号和文件系统权限影响：若练习环境允许，可用自己创建的副本做隔离测试并恢复权限；不能安全模拟时，记录“未验证”和原因，不为制造故障修改共享文件或系统目录。

## 6.8 完成标准

- 规格中的每一项都能对应到代码和测试证据。
- 原始日志的行数和内容没有变化。
- 成功、无数据、输入错误和输出错误可由不同退出状态区分。
- 错误消息可定位对象，但没有泄露敏感信息。

[返回 Shell 课程入口](../index.md) · [返回综合实战](../08_batch_project_practice.md)

本章自查：**必须掌握**从参数、判断到退出状态的执行顺序；**应当掌握**按示例修改精确匹配和输出位置；**了解即可**临时文件与退出清理的组合细节。

课堂检查：① 只读 6.2 的片段时需要创建正式脚本吗？② `R0012` 为什么不能算 R001？③ 哪一步创建临时结果文件？④ 无匹配但已有旧结果时能把旧文件提交作新证据吗？参考：不需要；完整字段边界不同；`mktemp` 建立临时文件；不能，脚本会警告。
