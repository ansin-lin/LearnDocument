# 日志分析自动化项目

这一章完成一个日志分析自动化项目。

项目流程：

```text
日志目录
→ 按日期查找日志
→ 提取 ERROR / WARN
→ 提取 order_id、user_id、path
→ 输出 CSV / Excel
→ 记录日志
```

## 一、完整代码

```python
from pathlib import Path
import argparse
import logging
import re
import sys

import pandas as pd


def parse_args() -> argparse.Namespace:
    # 作用：解析命令行参数
    # 使用场景：按日期分析日志
    parser = argparse.ArgumentParser()
    parser.add_argument("--target-date", required=True)
    parser.add_argument("--log-dir", default="logs")
    parser.add_argument("--output-dir", default="output")
    return parser.parse_args()


def setup_logging(output_dir: Path) -> None:
    # 作用：初始化日志
    # 使用场景：记录日志分析任务执行情况
    output_dir.mkdir(parents=True, exist_ok=True)
    logging.basicConfig(
        filename=output_dir / "log_analysis_job.log",
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        encoding="utf-8",
    )


def create_sample_logs(log_dir: Path) -> None:
    # 作用：创建样例日志
    # 使用场景：教学练习中没有真实日志时
    log_dir.mkdir(parents=True, exist_ok=True)
    (log_dir / "app_20260726.log").write_text(
        "\n".join(
            [
                "2026-07-26 09:00:01 INFO user_id=U001 path=/api/orders message=start",
                "2026-07-26 09:01:10 ERROR user_id=U002 order_id=O202607260001 path=/api/orders email=taro@example.com message=payment failed",
                "2026-07-26 09:02:20 WARN user_id=U003 order_id=O202607260002 path=/api/customers message=response slow",
            ]
        ),
        encoding="utf-8",
    )


def find_log_files(log_dir: Path, target_date: str) -> list[Path]:
    # 作用：按日期查找日志文件
    # 使用场景：只分析指定日期日志
    target_date_text = target_date.replace("-", "")
    files = list(log_dir.glob(f"*{target_date_text}*.log"))

    if not files:
        raise FileNotFoundError(f"log files not found: {target_date}")

    return files


def analyze_logs(files: list[Path]) -> pd.DataFrame:
    # 作用：分析日志并提取 WARN / ERROR
    # 使用场景：输出障害调查明细
    level_pattern = re.compile(r"\b(INFO|WARN|ERROR)\b")
    order_pattern = re.compile(r"order_id=(O\d+)")
    user_pattern = re.compile(r"user_id=(U\d+)")
    path_pattern = re.compile(r"path=([/\w-]+)")
    email_pattern = re.compile(r"[\w\.-]+@[\w\.-]+")

    records = []

    for file_path in files:
        with file_path.open("r", encoding="utf-8") as f:
            for line in f:
                level_match = level_pattern.search(line)

                if not level_match:
                    continue

                level = level_match.group(1)

                if level not in ["ERROR", "WARN"]:
                    continue

                order_match = order_pattern.search(line)
                user_match = user_pattern.search(line)
                path_match = path_pattern.search(line)
                masked_line = email_pattern.sub("***@***", line.strip())

                records.append(
                    {
                        "file_name": file_path.name,
                        "level": level,
                        "order_id": order_match.group(1) if order_match else None,
                        "user_id": user_match.group(1) if user_match else None,
                        "api_path": path_match.group(1) if path_match else None,
                        "message": masked_line,
                    }
                )

    return pd.DataFrame(records)


def export_result(result_df: pd.DataFrame, output_dir: Path) -> None:
    # 作用：导出日志分析结果
    # 使用场景：生成调查附件
    if result_df.empty:
        result_df = pd.DataFrame(
            columns=["file_name", "level", "order_id", "user_id", "api_path", "message"]
        )

    level_summary = result_df.groupby("level", as_index=False).agg(count=("message", "count"))
    api_summary = result_df.groupby("api_path", as_index=False).agg(count=("message", "count"))

    result_df.to_csv(output_dir / "error_detail.csv", index=False, encoding="utf-8-sig")

    with pd.ExcelWriter(output_dir / "log_analysis_report.xlsx", engine="openpyxl") as writer:
        result_df.to_excel(writer, sheet_name="error_detail", index=False)
        level_summary.to_excel(writer, sheet_name="level_summary", index=False)
        api_summary.to_excel(writer, sheet_name="api_summary", index=False)


def main() -> int:
    args = parse_args()
    log_dir = Path(args.log_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    setup_logging(output_dir)

    try:
        logging.info("log analysis start target_date=%s", args.target_date)
        create_sample_logs(log_dir)
        files = find_log_files(log_dir, args.target_date)
        result_df = analyze_logs(files)
        export_result(result_df, output_dir)
        logging.info("log analysis success rows=%s", len(result_df))
        print((output_dir / "log_analysis_report.xlsx").exists())  # 例如：True
        return 0

    except Exception:
        logging.exception("log analysis failed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
```

## 二、执行方式

```powershell
python log_analysis_job.py --target-date 2026-07-26 --log-dir logs --output-dir output
```

## 三、输出结果

```text
output/
├── error_detail.csv
├── log_analysis_report.xlsx
└── log_analysis_job.log
```

## 四、验收标准

| 检查项 | 标准 |
| --- | --- |
| 日志查找 | 能按日期找到日志文件 |
| 正则提取 | 能提取 level、order_id、user_id、api_path |
| 脱敏 | 邮箱被替换成 `***@***` |
| CSV | 生成 `error_detail.csv` |
| Excel | 生成 `log_analysis_report.xlsx` |
| 日志 | 记录开始、成功、失败 |
| 返回码 | 成功返回 `0`，失败返回 `1` |

## 五、扩展任务

1. 增加 `--level ERROR`，只输出 ERROR。
2. 增加 `--keyword payment`，按关键字筛选。
3. 增加邮件通知，把 Excel 调查报告作为附件。

