# 日志、异常、重试与可维护性

自动化脚本的交付标准不是“跑通一次”，而是失败时能调查、能补跑、能交接。

本章学习：

- 使用 `logging` 记录执行过程。
- 使用 `try except` 控制失败。
- 使用有限重试处理偶发异常。
- 输出处理结果摘要。

## 一、`logging.basicConfig()`：初始化日志

```python
import logging
from pathlib import Path

log_dir = Path("logs")
log_dir.mkdir(exist_ok=True)

# 作用：初始化日志配置
# 使用场景：把执行过程写入日志文件，方便失败后调查
logging.basicConfig(
    filename=log_dir / "job.log",
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    encoding="utf-8",
)

logging.info("job start")
logging.info("job finished")
```

日志示例：

```text
2026-07-27 09:00:00 INFO job start
2026-07-27 09:00:03 INFO job finished
```

常用参数：

| 参数 | 含义 | 使用场景 |
| --- | --- | --- |
| `filename` | 日志文件路径 | 输出到文件 |
| `level` | 日志级别 | `INFO`、`WARNING`、`ERROR` |
| `format` | 日志格式 | 包含时间、级别、信息 |
| `encoding` | 文件编码 | 日文、中文日志 |

## 二、日志级别

```python
# 作用：记录普通执行信息
# 使用场景：开始、结束、处理件数
logging.info("processed rows=%s", 1200)

# 作用：记录警告信息
# 使用场景：部分文件为空、部分数据未匹配
logging.warning("missing target count=%s", 10)

# 作用：记录错误信息
# 使用场景：处理失败但没有异常堆栈时
logging.error("file format invalid")
```

| 级别 | 使用场景 |
| --- | --- |
| `INFO` | 正常处理过程 |
| `WARNING` | 可继续但需要确认 |
| `ERROR` | 处理失败 |
| `EXCEPTION` | 异常和堆栈 |

## 三、`logging.exception()`：记录异常堆栈

```python
try:
    # 作用：执行主要处理逻辑
    # 使用场景：捕获处理过程中的异常并记录日志
    raise FileNotFoundError("input file not found")

except FileNotFoundError:
    logging.exception("input file not found")
    raise
```

`logging.exception()` 必须写在 `except` 中，它会记录异常堆栈，比只打印错误信息更适合排查。

## 四、有限重试

```python
import time
import requests


def get_with_retry(url: str, params: dict, max_retries: int = 3):
    # 作用：带有限重试的 GET 请求
    # 使用场景：接口偶发超时或 500 错误时
    for attempt in range(1, max_retries + 1):
        try:
            response = requests.get(url=url, params=params, timeout=10)
            response.raise_for_status()
            return response

        except requests.RequestException as e:
            logging.warning("request failed attempt=%s error=%s", attempt, e)

            if attempt == max_retries:
                raise

            time.sleep(2)
```

重试原则：

- 必须有限。
- 每次失败要记录日志。
- 最后一次失败要抛出异常。
- 不要对明显参数错误无限重试。

## 五、输出处理结果摘要

```python
import pandas as pd

# 作用：记录每个文件的处理结果
# 使用场景：批处理结束后输出执行摘要
results = [
    {"file_name": "orders_20260726_01.csv", "row_count": 1200, "status": "success"},
    {"file_name": "orders_20260726_02.csv", "row_count": 0, "status": "failed"},
]

result_df = pd.DataFrame(results)
result_df.to_csv("output/processed_files.csv", index=False, encoding="utf-8-sig")

print(result_df.shape)  # 例如：(2, 3)
```

## 六、本章完整案例

```python
from pathlib import Path
import logging
import sys

import pandas as pd


def setup_logging(log_dir: Path) -> None:
    # 作用：初始化日志
    # 使用场景：脚本开始时统一设置日志文件
    log_dir.mkdir(parents=True, exist_ok=True)
    logging.basicConfig(
        filename=log_dir / "job.log",
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        encoding="utf-8",
    )


def process_file(file_path: Path, output_dir: Path) -> dict:
    # 作用：处理单个 CSV 文件
    # 使用场景：批量处理中的单文件逻辑
    df = pd.read_csv(file_path, encoding="utf-8-sig")
    output_file = output_dir / f"{file_path.stem}_checked.csv"
    df.to_csv(output_file, index=False, encoding="utf-8-sig")

    logging.info("processed file=%s rows=%s", file_path.name, len(df))

    return {
        "file_name": file_path.name,
        "row_count": len(df),
        "status": "success",
        "message": "",
    }


def main() -> int:
    base_dir = Path("automation_logging_demo")
    input_dir = base_dir / "input"
    output_dir = base_dir / "output"
    log_dir = base_dir / "logs"

    input_dir.mkdir(parents=True, exist_ok=True)
    output_dir.mkdir(parents=True, exist_ok=True)
    setup_logging(log_dir)

    (input_dir / "orders_20260726_01.csv").write_text(
        "order_id,amount\nO001,1000\n",
        encoding="utf-8-sig",
    )

    logging.info("job start")

    results = []

    try:
        for file_path in input_dir.glob("orders_*.csv"):
            try:
                results.append(process_file(file_path, output_dir))

            except Exception as e:
                logging.exception("file failed file=%s", file_path.name)
                results.append(
                    {
                        "file_name": file_path.name,
                        "row_count": 0,
                        "status": "failed",
                        "message": str(e),
                    }
                )

        result_df = pd.DataFrame(results)
        result_df.to_csv(output_dir / "processed_files.csv", index=False, encoding="utf-8-sig")

        failed_count = (result_df["status"] == "failed").sum()
        logging.info("job finished failed_count=%s", failed_count)

        print((output_dir / "processed_files.csv").exists())  # 例如：True
        return 0 if failed_count == 0 else 1

    except Exception:
        logging.exception("job failed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
```

## 七、方法总结表

| 方法 / 写法 | 作用 | 使用场景 |
| --- | --- | --- |
| `logging.basicConfig()` | 初始化日志 | 设置日志文件、级别、格式 |
| `logging.info()` | 普通日志 | 开始、结束、件数 |
| `logging.warning()` | 警告日志 | 可继续但要确认 |
| `logging.error()` | 错误日志 | 明确失败信息 |
| `logging.exception()` | 异常堆栈 | `except` 中记录异常 |
| `try except` | 捕获异常 | 防止失败无记录 |
| `time.sleep()` | 等待 | 重试间隔 |
| 有限重试 | 重试不稳定操作 | 接口、网络、文件服务器 |

## 八、本章练习

1. 给第 05 章批量处理脚本增加日志文件。
2. 记录开始时间、结束时间、处理文件数、失败文件数。
3. 单个文件失败时记录 `logging.exception()`。
4. 输出 `processed_files.csv`。
5. 给接口调用函数增加最多 3 次重试。

