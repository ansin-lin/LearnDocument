#!/usr/bin/env bash
# 教学用调用脚本；MockSalesImport.jar 不写入真实业务数据。
# 引用未设置变量时报错；预期错误仍由下方分支明确处理。
set -u

# 拒绝缺少、多余或不是八位数字的业务日期参数。
if [[ $# -ne 1 || ! "$1" =~ ^[0-9]{8}$ ]]; then
  # 将正确调用格式写到标准错误。
  printf 'ERROR usage: bash run_sales_import.sh YYYYMMDD\n' >&2
  # 参数错误返回状态 2。
  exit 2
# 结束参数检查。
fi
# 日期格式初步通过后保存业务日期。
business_date="$1"
# 取得脚本所在目录，避免路径依赖调用者的工作目录。
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)" || exit 2
# 根据业务日期构造输入 CSV 路径。
input="$script_dir/input/sales_${business_date}.csv"
# 构造仅在成功后使用的归档路径。
archive="$script_dir/archive/sales_${business_date}.csv"
# 构造本次追加写入的日志路径。
log_file="$script_dir/logs/sales_${business_date}.log"
# 定位由教学 Java 源码构建的 JAR。
jar_file="$script_dir/MockSalesImport.jar"

# 用 GNU date 校验日历日期，并要求转换结果与原日期完全一致。
if ! date -d "${business_date:0:4}-${business_date:4:2}-${business_date:6:2}" +%Y%m%d 2>/dev/null | grep -qx "$business_date"; then
  # 报告 20260230 这类格式正确但实际不存在的日期。
  printf 'ERROR invalid calendar date: %s\n' "$business_date" >&2
  # 日期错误返回状态 2。
  exit 2
# 结束日历日期检查。
fi
# Java 启动前拒绝不存在、不可读或空的输入文件。
if [[ ! -f "$input" || ! -r "$input" || ! -s "$input" ]]; then
  # 在错误信息中指出有问题的输入路径。
  printf 'ERROR input missing, unreadable or empty: %s\n' "$input" >&2
  # 输入错误返回状态 3。
  exit 3
# 结束输入文件检查。
fi
# 确认个人练习副本中已经构建模拟 JAR。
if [[ ! -f "$jar_file" ]]; then
  # 报告缺少的构建产物。
  printf 'ERROR mock jar missing: %s\n' "$jar_file" >&2
  # 缺少 JAR 返回状态 3。
  exit 3
# 结束 JAR 检查。
fi
# 若同名归档或符号链接已存在，拒绝覆盖以前的结果。
if [[ -e "$archive" || -L "$archive" ]]; then
  # 提示再执行前需要调查已有归档。
  printf 'ERROR archived input already exists; confirm rerun procedure: %s\n' "$archive" >&2
  # 归档冲突返回状态 4。
  exit 4
# 结束归档冲突检查。
fi
# 只创建本案例自己的日志和归档目录。
if ! mkdir -p -- "$script_dir/logs" "$script_dir/archive"; then
  # 报告目录准备失败。
  printf 'ERROR cannot create log or archive directory\n' >&2
  # 目录准备失败返回状态 4。
  exit 4
# 结束目录创建检查。
fi

# 追加 START 记录；写日志失败时不启动 Java。
printf 'START date=%s input=%s\n' "$business_date" "$input" >> "$log_file" || exit 4
# 同步运行模拟 Java，并把正常与错误输出都追加到日志。
java -jar "$jar_file" "$business_date" "$input" >> "$log_file" 2>&1
# 立即保存 Java 返回码，避免其他命令改变 $?。
java_status=$?
# Java 返回非 0 时进入失败分支。
if [[ $java_status -ne 0 ]]; then
  # 追加失败详情，同时保留原始 CSV。
  if ! printf 'ERROR java_status=%s input_kept=%s\n' "$java_status" "$input" >> "$log_file"; then
    # 日志写入失败时通过标准错误通知操作人员。
    printf 'ERROR cannot write log: %s\n' "$log_file" >&2
    # 日志错误返回状态 4。
    exit 4
  # 结束失败日志写入检查。
  fi
  # 向调用方的标准错误报告 Java 失败。
  printf 'ERROR java_status=%s input_kept=%s\n' "$java_status" "$input" >&2
  # 按本案例约定把 Java 失败映射为 Shell 状态 10。
  exit 10
# 结束 Java 失败分支。
fi
# 只有 Java 报告成功后才归档输入。
if ! mv -- "$input" "$archive"; then
  # 记录 Java 成功但移动文件失败的情况。
  if ! printf 'ERROR archive failed; investigate before rerun: %s\n' "$input" >> "$log_file"; then
    # 日志不可写时改向标准错误报告。
    printf 'ERROR cannot write log: %s\n' "$log_file" >&2
    # 日志错误返回状态 4。
    exit 4
  # 结束归档错误日志检查。
  fi
  # 提醒调用方调查归档与再执行风险。
  printf 'ERROR archive failed; investigate before rerun: %s\n' "$input" >&2
  # 归档失败返回状态 11。
  exit 11
# 结束归档移动检查。
fi
# 只有 Java 与归档都成功后才写 SUCCESS。
if ! printf 'SUCCESS date=%s archive=%s\n' "$business_date" "$archive" >> "$log_file"; then
  # 成功日志写入失败仍属于调用方可见的错误。
  printf 'ERROR cannot write log: %s\n' "$log_file" >&2
  # 日志错误返回状态 4。
  exit 4
# 结束成功日志写入检查。
fi
# 向操作人员或 Job 输出简短成功信息。
printf 'SUCCESS date=%s archive=%s\n' "$business_date" "$archive"
# 向调用方返回成功状态 0。
exit 0
