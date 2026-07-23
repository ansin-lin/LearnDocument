# 第7章 日志、异常处理、重试与可恢复执行

在日本项目里，自动化脚本真正的交付标准，不是“跑通一次”，而是“失败时能查、能补、能重跑”。

这一章讲的就是现场最容易决定脚本质量的部分。

## 7.1 为什么日志很重要

如果脚本失败后只看到一句报错：

```text
error
```

几乎没有排查价值。

至少要记录：

- 脚本开始时间
- 当前处理到哪一步
- 关键输入参数
- 成功或失败状态
- 异常详情

## 7.2 用 `logging` 代替 `print`

```python
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)

logging.info("开始处理日报")
```

`print` 适合临时调试，正式自动化脚本建议用 `logging`。

## 7.3 捕获异常并写入日志

```python
import logging

try:
    logging.info("开始读取文件")
    raise ValueError("文件内容不符合预期")
except Exception as e:
    logging.exception(f"处理失败: {e}")
```

`logging.exception()` 会连 traceback 一起记录下来，排查时价值很高。

## 7.4 哪些错误适合重试

不是所有失败都要自动重试。常见判断方式：

- 网络抖动、临时超时：适合重试
- 文件不存在：通常不该盲目重试
- 数据格式错误：应先人工确认

也就是说，重试要有边界，不能把所有错误都包成无限循环。

## 7.5 最小重试示例

```python
import time

max_retries = 3

for attempt in range(1, max_retries + 1):
    try:
        print(f"第 {attempt} 次执行")
        # 假设这里是网络请求或数据库连接
        break
    except Exception:
        if attempt == max_retries:
            raise
        time.sleep(5)
```

## 7.6 断点思维和可恢复执行

成熟一点的自动化流程，最好能回答下面这些问题：

- 脚本失败时停在哪一步
- 之前已经成功的步骤需不需要重做
- 输出文件有没有生成一半
- 归档有没有提前执行

这就是“可恢复执行”的思维。

## 7.7 日本项目里常见的质量要求

- 失败时留下明确日志
- 关键步骤能定位
- 异常能区分系统错误和数据错误
- 支持补跑
- 不因一次失败导致历史结果被覆盖

这些要求往往比“多会几个库函数”更重要。

## 7.8 本章练习

请完成下面 3 个任务：

1. 用 `logging` 记录脚本开始和结束。
2. 用 `try/except` 捕获处理失败并写出异常日志。
3. 为一个模拟网络请求增加最多 3 次重试。

如果这三件事做不到，自动化脚本很难进入正式交付环境。
