# 企业自动化报表项目

这一章完成一个典型企业自动化报表流程。

本项目承接数据分析路线：数据分析课程负责“怎么读取、清洗、汇总、输出 Excel”，自动化课程负责“怎么让脚本稳定执行、通知、记录日志、可补跑”。

项目流程：

```text
订单 CSV
→ 批量读取
→ 汇总地区销售额
→ 输出 Excel 报表
→ 输出执行摘要
→ 记录日志
```

## 一、项目目录

```text
automation_report_project/
├── input/
├── output/
├── logs/
└── report_job.py
```

## 二、完整代码

```python
from pathlib import Path
import argparse
import logging
import sys

import pandas as pd


def setup_logging(log_dir: Path) -> None:
    # 作用：初始化日志
    # 使用场景：报表任务执行过程写入日志文件
    log_dir.mkdir(parents=True, exist_ok=True)
    logging.basicConfig(
        filename=log_dir / "report_job.log",
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        encoding="utf-8",
    )


def parse_args() -> argparse.Namespace:
    # 作用：解析命令行参数
    # 使用场景：手动执行、补跑、调度器执行时传入处理日期和目录
    parser = argparse.ArgumentParser()
    parser.add_argument("--target-date", required=True)
    parser.add_argument("--input-dir", default="input")
    parser.add_argument("--output-dir", default="output")
    parser.add_argument("--log-dir", default="logs")
    parser.add_argument("--encoding", default="utf-8-sig")
    return parser.parse_args()


def create_sample_files(input_dir: Path) -> None:
    # 作用：创建样例订单文件
    # 使用场景：教学练习中没有真实输入文件时
    input_dir.mkdir(parents=True, exist_ok=True)
    (input_dir / "orders_20260726_01.csv").write_text(
        "order_id,region,amount\nO001,Kanto,1000\nO002,Kansai,2000\n",
        encoding="utf-8-sig",
    )
    (input_dir / "orders_20260726_02.csv").write_text(
        "order_id,region,amount\nO003,Kanto,3000\n",
        encoding="utf-8-sig",
    )


def load_orders(input_dir: Path, target_date: str, encoding: str) -> pd.DataFrame:
    # 作用：按处理日期批量读取订单 CSV
    # 使用场景：一个日期下存在多个输入文件
    target_date_text = target_date.replace("-", "")
    files = list(input_dir.glob(f"orders_{target_date_text}_*.csv"))

    if not files:
        raise FileNotFoundError(f"order files not found: {target_date}")

    df_list = []

    for file_path in files:
        df = pd.read_csv(file_path, encoding=encoding, dtype={"order_id": "string"})
        df["source_file"] = file_path.name
        df_list.append(df)
        logging.info("loaded file=%s rows=%s", file_path.name, len(df))

    return pd.concat(df_list, ignore_index=True)


def create_region_summary(orders: pd.DataFrame) -> pd.DataFrame:
    # 作用：按地区汇总销售额和订单数
    # 使用场景：生成自动化报表的汇总 Sheet
    return (
        orders.groupby("region", as_index=False)
        .agg(
            sales_amount=("amount", "sum"),
            order_count=("order_id", "nunique"),
        )
        .sort_values("sales_amount", ascending=False, ignore_index=True)
    )


def export_report(
    orders: pd.DataFrame,
    region_summary: pd.DataFrame,
    output_dir: Path,
    target_date: str,
) -> Path:
    # 作用：导出 Excel 报表
    # 使用场景：业务方查看明细和汇总结果
    output_dir.mkdir(parents=True, exist_ok=True)
    report_file = output_dir / f"sales_report_{target_date}.xlsx"

    summary = pd.DataFrame(
        [
            {
                "target_date": target_date,
                "row_count": len(orders),
                "region_count": len(region_summary),
                "status": "success",
            }
        ]
    )

    with pd.ExcelWriter(report_file, engine="openpyxl") as writer:
        orders.to_excel(writer, sheet_name="orders", index=False)
        region_summary.to_excel(writer, sheet_name="region_summary", index=False)
        summary.to_excel(writer, sheet_name="summary", index=False)

    return report_file


def main() -> int:
    args = parse_args()
    setup_logging(Path(args.log_dir))

    try:
        logging.info("report job start target_date=%s", args.target_date)

        input_dir = Path(args.input_dir)
        output_dir = Path(args.output_dir)

        create_sample_files(input_dir)
        orders = load_orders(input_dir, args.target_date, args.encoding)
        region_summary = create_region_summary(orders)
        report_file = export_report(orders, region_summary, output_dir, args.target_date)

        logging.info("report job success report=%s rows=%s", report_file, len(orders))
        print(report_file.exists())  # 例如：True 表示报表已生成
        return 0

    except Exception:
        logging.exception("report job failed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
```

## 三、执行方式

```powershell
python report_job.py --target-date 2026-07-26 --input-dir input --output-dir output --log-dir logs
```

## 四、输出结果

```text
output/
└── sales_report_2026-07-26.xlsx

logs/
└── report_job.log
```

Excel 中包含：

| Sheet | 内容 |
| --- | --- |
| `orders` | 合并后的订单明细 |
| `region_summary` | 地区销售汇总 |
| `summary` | 执行摘要 |

## 五、验收标准

| 检查项 | 标准 |
| --- | --- |
| 参数 | 支持处理日期、输入目录、输出目录、日志目录 |
| 输入 | 能按日期读取多个订单 CSV |
| 输出 | 生成 Excel 报表 |
| 日志 | 记录开始、读取文件、成功或失败 |
| 返回码 | 成功返回 `0`，失败返回 `1` |
| 可补跑 | 修改 `--target-date` 可以补跑指定日期 |

## 六、扩展任务

1. 增加 `--dry-run`，只显示要处理的文件。
2. 增加邮件通知。
3. 增加 ZIP 压缩报表。
4. 把地区汇总替换成数据分析路线第 08 章的完整销售分析。

