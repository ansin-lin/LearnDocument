# 运维辅助自动化项目

这一章完成一个安全范围内的运维辅助自动化项目。

目标不是写危险的服务器维护脚本，而是完成新人可以掌握的辅助检查：

- 检查文件是否到达。
- 检查文件大小是否为 0。
- 检查接口是否正常。
- 归档文件。
- 输出检查报告。

## 一、完整代码

```python
from pathlib import Path
from datetime import datetime
import argparse
import logging
import shutil
import sys

import pandas as pd
import requests


def parse_args() -> argparse.Namespace:
    # 作用：解析命令行参数
    # 使用场景：指定检查文件、接口地址和输出目录
    parser = argparse.ArgumentParser()
    parser.add_argument("--target-file", required=True)
    parser.add_argument("--health-url", required=True)
    parser.add_argument("--archive-dir", default="archive")
    parser.add_argument("--output-dir", default="output")
    return parser.parse_args()


def check_file_exists(file_path: Path) -> dict:
    # 作用：检查文件是否存在
    # 使用场景：确认上游系统是否已放置文件
    return {
        "check_type": "file_exists",
        "target": str(file_path),
        "status": "ok" if file_path.exists() else "ng",
        "message": "",
    }


def check_file_not_empty(file_path: Path) -> dict:
    # 作用：检查文件是否为空
    # 使用场景：文件存在但大小为 0 时不能继续处理
    if not file_path.exists():
        return {
            "check_type": "file_not_empty",
            "target": str(file_path),
            "status": "ng",
            "message": "file not found",
        }

    file_size = file_path.stat().st_size

    return {
        "check_type": "file_not_empty",
        "target": str(file_path),
        "status": "ok" if file_size > 0 else "ng",
        "message": f"size={file_size}",
    }


def check_api_status(url: str) -> dict:
    # 作用：检查接口是否正常
    # 使用场景：批处理前确认外部系统可访问
    try:
        response = requests.get(url=url, timeout=10)
        return {
            "check_type": "api_status",
            "target": url,
            "status": "ok" if response.status_code == 200 else "ng",
            "message": f"status_code={response.status_code}",
        }

    except requests.RequestException as e:
        return {
            "check_type": "api_status",
            "target": url,
            "status": "ng",
            "message": str(e),
        }


def archive_file(file_path: Path, archive_dir: Path) -> dict:
    # 作用：复制文件到归档目录
    # 使用场景：保留处理对象，避免直接删除
    if not file_path.exists():
        return {
            "check_type": "archive_file",
            "target": str(file_path),
            "status": "ng",
            "message": "file not found",
        }

    archive_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    archive_path = archive_dir / f"{file_path.stem}_{timestamp}{file_path.suffix}"
    shutil.copy(file_path, archive_path)

    return {
        "check_type": "archive_file",
        "target": str(file_path),
        "status": "ok",
        "message": str(archive_path),
    }


def main() -> int:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    logging.basicConfig(
        filename=output_dir / "ops_check_job.log",
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        encoding="utf-8",
    )

    try:
        target_file = Path(args.target_file)

        # 作用：教学用样例文件
        # 使用场景：如果文件不存在，先创建一个可检查文件
        target_file.parent.mkdir(parents=True, exist_ok=True)
        if not target_file.exists():
            target_file.write_text("sample data\n", encoding="utf-8")

        results = [
            check_file_exists(target_file),
            check_file_not_empty(target_file),
            check_api_status(args.health_url),
            archive_file(target_file, Path(args.archive_dir)),
        ]

        result_df = pd.DataFrame(results)
        result_df.to_csv(output_dir / "ops_check_result.csv", index=False, encoding="utf-8-sig")

        ng_count = (result_df["status"] == "ng").sum()
        logging.info("ops check finished ng_count=%s", ng_count)

        print((output_dir / "ops_check_result.csv").exists())  # 例如：True
        return 0 if ng_count == 0 else 1

    except Exception:
        logging.exception("ops check failed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
```

## 二、执行方式

```powershell
python ops_check_job.py --target-file input/orders_20260726.csv --health-url https://api.example.com/health --output-dir output --archive-dir archive
```

## 三、输出结果

```text
output/
├── ops_check_result.csv
└── ops_check_job.log

archive/
└── orders_20260726_YYYYMMDDHHMMSS.csv
```

## 四、检查结果说明

| 字段 | 含义 |
| --- | --- |
| `check_type` | 检查类型 |
| `target` | 检查对象 |
| `status` | `ok` 或 `ng` |
| `message` | 补充说明 |

## 五、验收标准

| 检查项 | 标准 |
| --- | --- |
| 文件存在 | 能检查目标文件是否存在 |
| 文件大小 | 能检查文件是否为空 |
| 接口状态 | 请求设置 `timeout=10` |
| 归档 | 使用复制，不直接删除原文件 |
| CSV | 输出 `ops_check_result.csv` |
| 日志 | 输出 `ops_check_job.log` |
| 返回码 | 有任意 `ng` 返回 `1` |

## 六、安全边界

新人阶段不建议写自动删除脚本。归档优先使用复制或移动到明确目录，不要批量删除共享目录或正式目录文件。

