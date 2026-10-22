# 第 4 章：文件、文本与输入输出

## 本章学习目标与前置知识

前置：第 3 章的文件判断与 `if`。你将安全准备输入、复制备份、移动归档，读懂常用文本命令和标准流；复杂 CSV 业务处理仍交给 Java。

## 为什么日本项目需要

取込処理前要核对文件是否到齐；处理后会归档，失败时要保留原始输入供调查。Shell 做文件编排，不能把“成功移动文件”误当成“业务导入成功”。

## 概念、命令与最小示例

以下为**可在个人练习目录逐行执行的命令**。`mkdir -p` 创建目录（已存在不报错），`cp --` 复制，`mv --` 移动或改名，`--` 表示后续是路径而非选项。示例路径只属于个人练习：

```bash
# 建立输入、归档、日志目录；已有目录不报错。
mkdir -p input archive logs
# 把三行练习 CSV 写入文件；> 会覆盖已有同名内容。
printf 'sale_id,amount\nS001,100\nS002,200\n' > input/sales_20260901.csv
# 复制原始输入，后续只移动副本。
cp -- input/sales_20260901.csv input/sales_copy.csv
# 由标准输入读取副本并统计行数，预期输出 3。
wc -l < input/sales_copy.csv
# 将副本移到归档目录；原始输入仍留在 input。
mv -- input/sales_copy.csv archive/sales_copy.csv
```

执行顺序：创建目录 → `>` 覆盖/创建 CSV → 复制 → 统计行数 → 移动副本。**示例输出**：`wc -l` 显示 `3`；`input/sales_20260901.csv` 未变，`archive/sales_copy.csv` 存在。此处只是练习副本；正式原件的归档必须等 Java 成功后进行。

常见只读文本命令：`grep -n 'S001' 文件` 找匹配并显示行号；`wc -l` 数行；`cut -d, -f1` 按逗号取首列；`sort` 排序；`uniq -c` 统计**相邻**相同值，因此通常先 `sort`；`awk -F, '{print $1}'` 与 `sed -n '1,2p'` 只要求看懂，不用它们实现复杂 CSV 解析（引号内逗号等情况不适合这种简化写法）。

标准输入 stdin 是程序读取内容的入口；标准输出 stdout 是正常结果；标准错误 stderr 是错误信息。`< 文件` 把文件作为 stdin；`>` 覆盖 stdout 目标，`>>` 追加；`2>` 保存 stderr；`2>&1` 把 stderr 指向 stdout **当时**的去处；`|` 把左侧 stdout 送给右侧 stdin。`<<'EOF'` 是 here-document：直到独占一行的 `EOF` 为止的多行文字作为输入，带引号时不会展开变量。

下面是**终端命令**，只在个人副本创建样例，演示 `<<`（目标文件会被覆盖）：

```bash
cat > input/sales_20260902.csv <<'EOF'
sale_id,amount
S003,300
EOF
```

逐行看：`cat > ... <<'EOF'` 指定将后面的文字原样写入目标 CSV；`sale_id,amount` 是表头；`S003,300` 是一条练习数据；独占一行的 `EOF` 只标记输入结束，不写进文件。这里不能在 CSV 数据行插入 `#` 注释，否则注释也会成为文件内容。用 `cat input/sales_20260902.csv` 应看到两行。

以下是**完整可执行脚本** `count_sales.sh`，读取已有 CSV，输出数据行数：

```bash
#!/usr/bin/env bash
# 读取第一个参数作为待统计的文件路径；缺失时用空串。
input_file="${1:-}"
# 不是普通文件或当前用户不可读时进入错误分支。
if [[ ! -f "$input_file" || ! -r "$input_file" ]]; then
  # 将失败路径写到标准错误。
  printf 'ERROR: unreadable input: %s\n' "$input_file" >&2
  # 以状态 3 结束脚本，不继续统计。
  exit 3
# 结束文件检查。
fi
# 使用 wc 读取文件行数，命令替换将数字保存到变量。
line_count="$(wc -l < "$input_file")"
# 减去标题行，输出数据行数。
printf 'data_rows=%s\n' "$((line_count - 1))"
```

`$(...)` 取得 `wc` 输出，`$((...))` 作整数运算并扣除一行标题。运行 `bash count_sales.sh input/sales_20260901.csv`，**示例输出** `data_rows=2`。这个演示假设每条记录占一行，不能作为真正 CSV 记录数校验；空文件、只有标题等边界会在综合案例由 Java 判断。

练习输出流：

```bash
# 将匹配行写到正常日志，将可能的错误另写到错误日志。
grep -n 'S001' input/sales_20260901.csv > logs/search.log 2> logs/search.err
# 读回正常日志，确认搜索结果。
cat logs/search.log
# 在正常日志末尾追加一行练习文字；重复运行会重复追加。
printf 'again\n' >> logs/search.log
```

先 `>` 再 `2>` 分开保存正常和错误；`>>` 追加，重复运行会重复写。`grep` 无匹配返回 1，不一定是故障。`grep ... | wc -l` 默认状态通常只反映末端 `wc`，第 6 章再讲 `pipefail`。`tee -a logs/run.log` 可同时显示并追加来自管道的 stdout，但要留意管道状态。

文本编码：练习统一 UTF-8、LF 换行。项目 CSV 的编码、换行和表头以规格为准；出现乱码或 `^M` 不要随意转换原件，先确认输入约定和副本。

## 常见错误、练习与完成标准

`>` 覆盖已有文件；`mv` 成功后旧路径不再存在；`uniq` 未排序只能统计相邻重复；空 CSV 不等于导入成功。复制一份 CSV 到个人练习目录，提取第一列、排序去重，将结果写入新文件，展示原件未变、结果内容及命令状态。能说明三个标准流与 `<`、`<<`、`>`、`>>`、`2>`、`2>&1`、`|` 的方向，即完成。

[上一章：条件与函数](03_control_functions.md) · [下一章：Java Batch 联动](05_batch_execution.md)
