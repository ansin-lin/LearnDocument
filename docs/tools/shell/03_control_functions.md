# 第 3 章：条件判断、循环与函数

## 本章学习目标与前置知识

在第 2 章的参数脚本上增加数量、日期格式与文件检查；会写 `if`、`case`、`for`，读懂 `while read`，理解函数的 `return` 与脚本的 `exit`。前置：变量、参数、引用与脚本执行。

## 为什么日本项目需要

ジョブ传来错误业务日期、输入文件未到达时，取込処理不应继续调用 Java。正常終了（正常结束）与異常終了（异常结束）必须由明确分支决定。

## 语法、最小脚本与逐行说明

`[[ ... ]]` 是 Bash 判断语法；`-ne` 为数字“不等于”，`-f` 判断普通文件，`-s` 判断存在且大小大于零，`-z` 判断空字符串；`!` 取反，`||` 表示“或”，任一条件成立便进入对应分支。`if/elif/else/fi` 依条件选择；`case` 对固定模式分支；`for/do/done` 逐项执行。

以下是**完整可执行脚本** `check_sales_input.sh`，只检查参数和文件，不处理销售数据：

```bash
#!/usr/bin/env bash
# 如果参数个数不是两个，就进入用法错误分支。
if [[ $# -ne 2 ]]; then
  # 将用法提示写到标准错误。
  printf 'Usage: %s YYYYMMDD CSV_FILE\n' "$0" >&2
  # 以状态 2 结束整个脚本。
  exit 2
# 结束参数数量判断。
fi
# 保存已检查存在的第一个参数作为业务日期。
business_date="$1"
# 保存第二个参数作为输入文件路径。
input_file="$2"
# 如果日期不是八位数字，就拒绝继续。
if [[ ! "$business_date" =~ ^[0-9]{8}$ ]]; then
  # 把日期格式错误写到标准错误。
  printf 'ERROR: date must be YYYYMMDD\n' >&2
  # 日期格式错误返回状态 2。
  exit 2
# 结束日期格式判断。
fi
# 若文件不存在、不可读或大小为零，任一情况都拒绝输入。
if [[ ! -f "$input_file" || ! -r "$input_file" || ! -s "$input_file" ]]; then
  # 报告有问题的文件路径。
  printf 'ERROR: input is missing, unreadable or empty: %s\n' "$input_file" >&2
  # 输入文件错误返回状态 3。
  exit 3
# 结束文件检查。
fi
# 所有检查通过，输出可继续处理的信息。
printf 'ready date=%s file=%s\n' "$business_date" "$input_file"
```

脚本先拒绝非两个参数，`>&2` 把说明写到标准错误；随后赋值，再用最小正则检查八位数字（还**不能**证明是真实日历日期）；最后查文件类型、可读、非空并输出可继续处理的结果。执行流程是“数量 → 格式 → 文件 → ready”，任一失败 `exit` 使脚本立即结束。第 5 章再加入严格日历检查。

在个人目录准备测试 CSV（`>` 会覆盖同名练习文件）：

```bash
# 创建仅供练习的 CSV；重跑会覆盖这个同名文件。
printf 'sale_id,amount\nS001,100\n' > sales_20260901.csv
# 用正确的日期和现有 CSV 运行检查脚本。
bash check_sales_input.sh 20260901 sales_20260901.csv
# 立即显示上一轮检查的退出状态。
printf 'status=%s\n' "$?"
# 改用不存在的 CSV，观察失败分支。
bash check_sales_input.sh 20260901 missing.csv
# 立即显示失败分支的退出状态。
printf 'status=%s\n' "$?"
```

**示例输出**：第一轮 `ready date=20260901 file=sales_20260901.csv`、`status=0`；第二轮错误提示、`status=3`。`$?` 必须紧接运行读取。

以下都是**局部片段**，用于读懂项目现有脚本，不能单独当成完整文件：

```bash
# 按输入文件名进入其中一个固定模式分支。
case "$input_file" in
  # 后缀为 .csv 时输出类型提示；两个分号结束这个分支。
  *.csv) printf 'CSV input\n' ;;
  # 其他后缀写到标准错误。
  *) printf 'not CSV\n' >&2 ;;
# 结束 case 结构。
esac
# 按传入顺序逐个打印参数；"$@" 保持参数边界。
for file in "$@"; do printf '%s\n' "$file"; done
# 从练习 CSV 逐行读取并原样输出；这里只示意文本读取。
while IFS= read -r line; do printf '%s\n' "$line"; done < sales_20260901.csv
```

`case` 按模式选一支，`*.csv` 指“以 `.csv` 结尾”，`*` 是其他情况，`;;` 结束该支；`for` 中 `"$@"` 保留每个参数边界；`while read` 一行一行读取，`IFS=` 和 `-r` 保留空白与反斜杠。本课程要求会看懂 `while read`，不把业务 CSV 解析交给 Shell。

函数定义与调用的**局部片段**：

```bash
# 定义函数：输出第一个函数参数并返回状态 1；定义本身不执行函数。
show_error() { printf 'ERROR: %s\n' "$1" >&2; return 1; }
# 调用刚定义的函数，传入错误说明。
show_error 'input missing'
```

定义时不执行；调用时函数的 `$1` 是这次调用参数。`return 1` 只结束函数，`exit 1` 结束整个脚本。函数返回数字状态，不返回任意字符串。

## 常见错误、练习与完成标准

`-s` 不等于“CSV 内容有效”，只说明文件非空；漏引号会拆开路径；`20260231` 能通过八位格式检查却不是合法日期。请为脚本增设“只有 `.csv` 后缀才继续”的 `case` 分支，并用正常、缺文件、少参数、错误后缀四组输入记录状态。能解释每次停在哪一段，且正常状态 0、参数状态 2、文件状态 3，即完成。真实项目中更复杂的 CSV 校验由 Java Batch 负责。

[上一章：变量与参数](02_variables_arguments.md) · [下一章：文件与文本](04_file_text_processing.md)
