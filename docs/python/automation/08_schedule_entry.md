# 定时任务与执行入口

自动化脚本最终通常不是手动运行，而是由 Windows 任务计划程序、Linux cron、JP1、Systemwalker 或其他调度平台执行。

本章学习：

- `main()` 固定入口。
- `sys.exit()` 返回码。
- Windows `.bat` 调用。
- Linux cron 调用。
- 锁文件防止重复执行。
- 调度脚本需要记录日志和参数。

## 一、`main()` 固定入口

```python
def main():
    # 作用：放置脚本主流程
    # 使用场景：让自动化任务有固定入口
    print("job start")
    print("job end")


if __name__ == "__main__":
    # 作用：只有直接运行当前文件时才执行 main()
    # 使用场景：脚本既可以被直接运行，也可以被其他文件导入
    main()
```

## 二、`sys.exit()` 返回码

```python
import sys


def main() -> int:
    # 作用：返回 0 表示成功
    # 使用场景：调度器根据返回码判断任务状态
    print("job success")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

常见返回码：

| 返回码 | 含义 | 调度器判断 |
| --- | --- | --- |
| `0` | 成功 | 正常结束 |
| `1` | 处理失败 | 异常结束 |
| `2` | 参数错误 | 参数不正确 |

## 三、命令行执行入口

```powershell
python job.py --target-date 2026-07-26 --input-dir input --output-dir output
```

自动化脚本应该能从命令行执行，因为调度器本质上也是执行命令。

## 四、Windows 批处理入口

`run_job.bat` 示例：

```bat
@echo off
cd /d D:\project\automation_job
python job.py --target-date 2026-07-26 --input-dir input --output-dir output
exit /b %ERRORLEVEL%
```

说明：

| 写法 | 作用 |
| --- | --- |
| `@echo off` | 不显示每条命令本身 |
| `cd /d` | 切换到脚本目录 |
| `python job.py ...` | 执行 Python 脚本 |
| `exit /b %ERRORLEVEL%` | 把 Python 返回码传给调度器 |

## 五、Linux cron 入口

```bash
0 9 * * * cd /opt/automation_job && python job.py --target-date 2026-07-26 >> logs/cron.log 2>&1
```

说明：

| 写法 | 作用 |
| --- | --- |
| `0 9 * * *` | 每天 9 点执行 |
| `cd /opt/automation_job` | 切换到脚本目录 |
| `>> logs/cron.log` | 追加标准输出 |
| `2>&1` | 把错误输出也写入日志 |

## 六、锁文件防止重复执行

如果上一次任务还没跑完，下一次任务又启动，可能造成重复处理。简单做法是使用锁文件。

```python
from pathlib import Path

lock_file = Path("job.lock")

# 作用：检查锁文件是否存在
# 使用场景：避免同一个批处理重复启动
if lock_file.exists():
    raise RuntimeError("job is already running")

try:
    # 作用：创建锁文件
    # 使用场景：标记当前任务正在执行
    lock_file.write_text("running", encoding="utf-8")

    print("job running")  # 例如：job running

finally:
    # 作用：删除锁文件
    # 使用场景：任务结束后允许下次执行
    if lock_file.exists():
        lock_file.unlink()
```

## 七、本章完整案例

```python
from pathlib import Path
import argparse
import sys


def parse_args() -> argparse.Namespace:
    # 作用：解析命令行参数
    # 使用场景：定时任务或手动补跑时指定处理日期
    parser = argparse.ArgumentParser()
    parser.add_argument("--target-date", required=True)
    parser.add_argument("--output-dir", default="output")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    lock_file = output_dir / "job.lock"

    if lock_file.exists():
        print("job is already running")
        return 1

    try:
        lock_file.write_text("running", encoding="utf-8")

        result_file = output_dir / f"result_{args.target_date}.txt"
        result_file.write_text("status=success\n", encoding="utf-8")

        print(result_file.exists())  # 例如：True 表示结果文件已生成
        return 0

    except Exception as e:
        print(f"job failed: {e}")
        return 1

    finally:
        if lock_file.exists():
            lock_file.unlink()


if __name__ == "__main__":
    sys.exit(main())
```

执行：

```powershell
python job.py --target-date 2026-07-26 --output-dir output
```

## 八、方法总结表

| 写法 | 作用 | 使用场景 |
| --- | --- | --- |
| `main()` | 固定入口 | 命令行和调度器统一执行 |
| `if __name__ == "__main__"` | 判断直接运行 | 避免导入时自动执行 |
| `sys.exit()` | 返回退出码 | 调度器判断成功失败 |
| `.bat` | Windows 执行入口 | Windows 任务计划 |
| `cron` | Linux 定时入口 | Linux 服务器定时执行 |
| 锁文件 | 防止重复执行 | 任务可能运行较久时 |

## 九、本章练习

1. 给第 05 章的批量处理脚本增加 `main()`。
2. 成功时返回 `0`，失败时返回 `1`。
3. 写一个 `.bat` 调用脚本。
4. 增加锁文件，防止重复执行。
5. 思考：如果脚本异常结束，锁文件是否会被删除？

