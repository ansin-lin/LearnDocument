#!/usr/bin/env bash
# 学员改修起点：尚需补充输入校验、返回码处理和归档。
# 读取 $1 前，先要求恰好传入一个业务日期参数。
if [[ $# -ne 1 ]]; then
  # 把调用方法提示写到标准错误。
  printf 'Usage: bash starter_run_sales_import.sh YYYYMMDD\n' >&2
  # 参数错误返回状态 2。
  exit 2
# 结束参数数量检查。
fi
# 保存传入的日期，供调用 Java 时使用。
business_date="$1"
# 取得当前脚本所在目录，使路径不依赖调用者的工作目录。
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)" || exit 2
# 按日期定位脚本旁 input 目录中的 CSV。
input="$script_dir/input/sales_${business_date}.csv"
# 同步运行模拟 Java；起点脚本尚未处理它的返回状态。
java -jar "$script_dir/MockSalesImport.jar" "$business_date" "$input"
