
# 第6章 · 异常与错误处理（Exceptions）— 现代风格 · 零基础友好

> 目标：学会在 Python 中**优雅地处理错误**，保护程序稳定性。  
> 风格：所有无序列表使用 `-`（横线），`*` 仅用于加粗；每个知识点都有**解释 + 代码 + 调用演示**。

---

## 1.什么是“异常”？为什么不用返回码？

- **异常（Exception）**：运行时出现错误时，由解释器或你的代码**抛出**的对象；如果没人处理就会**冒泡并终止程序**，附带**栈回溯**信息。
- **对比返回码**：返回码常被**忽略**或被覆盖；异常会**强制你关注**异常路径，并能携带完整上下文（类型、消息、堆栈）。
- Python 中的异常都是**类**，大都继承自 `Exception`。

```python
def divide(a, b):
    return a / b

# === 调用演示 ===
# divide(10, 0)  # 取消注释将触发 ZeroDivisionError 并终止程序
```

---

## 2.基本语法

- `try` 放**可能出错**的代码块。  
- `except` **按类型**捕获异常。  
- `else` 在 **未发生异常** 时执行（可放“成功路径”）。  
- `finally` **无论是否异常**都执行（资源清理）。

```python
def parse_int(text: str):
    try:
        value = int(text)       # 可能 ValueError
    except ValueError as e:
        return f"不是整数：{e}"
    else:
        return value            # 成功时走这里
    finally:
        pass                    # 这里通常做清理/关闭资源
# === 调用演示 ===
print(parse_int("123"))   # -> 123
print(parse_int("12x"))   # -> 不是整数：invalid literal for int() with base 10: '12x'
```

---

## 3. 捕获方式

- **具体类型优先**：只捕获你**确实能处理**的异常类型。  
- **多异常**：`except (TypeError, ValueError) as e:`。  
- `as e` 可拿到异常对象，便于记录/拼装新信息。

```python
def safe_calc(a, b):
    try:
        return a / b
    except (TypeError, ZeroDivisionError) as e:
        return f"计算失败：{type(e).__name__} - {e}"

print(safe_calc(10, 2))    # -> 5.0
print(safe_calc(10, 0))    # -> 计算失败：ZeroDivisionError - division by zero
print(safe_calc("10", 2))  # -> 计算失败：TypeError - unsupported operand type(s) for /: 'str' and 'int'
```

!!! warning "反模式：裸 except"
    - 禁止 `except:` 或 `except Exception:` **兜底一切**，容易吞掉编程错误。  
    - 如果必须兜底，请：**日志记录 + 重新抛出或返回明确错误**。

---

## 4. 主动抛出：`raise`、二次抛出与异常链

- **主动抛出**：当检测到“**不满足前置条件**”时，用 `raise` 明确失败。  
- **保留栈**重新抛出：裸 `raise` 在 `except` 中使用，保留原始堆栈。  
- **异常链**：`raise NewError(...) from e`，把上下游错误**关联**起来。

```python
def require_positive(n):
    if n <= 0:
        raise ValueError("n 必须为正数")

def wrapper(n):
    try:
        require_positive(n)
    except ValueError as e:
        # 添加业务上下文，并保留原始异常链
        raise RuntimeError(f"参数校验失败：n={n}") from e

# === 调用演示 ===
try:
    wrapper(-1)
except Exception as e:
    print(type(e).__name__, "->", e.__cause__)  # -> RuntimeError -> ValueError('n 必须为正数')
```

---

## 5. 自定义异常：分层设计，让错误可读可判

- 继承 `Exception`（或某个标准异常）创建**语义清晰**的异常层级。  
- 好处：`except MyBaseError:` 一把抓住本模块的所有业务异常。

```python
class AppError(Exception): ...
class ConfigError(AppError): ...
class DatabaseError(AppError): ...

def load_config(path):
    if not path.endswith(".yaml"):
        raise ConfigError("仅支持 .yaml 配置文件")
    return {"ok": True}

# === 调用演示 ===
try:
    load_config("conf.json")
except AppError as e:  # 统一入口
    print("应用错误：", type(e).__name__, "-", e)

# 预期输出：
#   应用错误： ConfigError - 仅支持 .yaml 配置文件
```

---

## 6. 资源清理：`finally` 与 `with`（上下文管理）

- 在 `finally` 中**无条件清理资源**（关闭文件/连接/锁）。  
- 推荐 `with ... as ...`：由对象实现 `__enter__ / __exit__` 自动管理。

```python
# finally 方式
def read_first_line(path):
    f = open(path, "w+", encoding="utf-8")
    try:
        f.write("hello\nworld")
        f.seek(0)
        return f.readline().strip()
    finally:
        f.close()

# with 方式（更推荐）
def write_hello(path):
    with open(path, "w", encoding="utf-8") as f:
        f.write("Hello")

print(read_first_line("tmp.txt"))  # -> hello
write_hello("tmp2.txt")
print(open("tmp2.txt", "r", encoding="utf-8").read())  # -> Hello
```

!!! tip "自定义上下文管理器"
    - 实现对象的 `__enter__` / `__exit__`；或用 `contextlib.contextmanager` 装饰器写生成器式管理器。

```python
from contextlib import contextmanager

@contextmanager
def opened(path, mode="w", encoding="utf-8"):
    f = open(path, mode, encoding=encoding)
    try:
        yield f
    finally:
        f.close()

with opened("demo.txt") as f:
    f.write("OK")
print(open("demo.txt","r",encoding="utf-8").read())  # -> OK
```

---

## 7. 断言（`assert`）：开发期自检，而非业务分支

- 语义：**在开发/测试阶段**校验“**永远应该为真**”的条件；失败抛 `AssertionError`。  
- **生产环境可被优化关闭**（`python -O`），因此**不要**用 `assert` 做业务逻辑。

```python
def normalize(pct):
    assert 0.0 <= pct <= 1.0, "pct 应在 [0,1]"
    return f"{pct:.1%}"

print(normalize(0.256))   # -> 25.6%
# normalize(1.5)          # 取消注释会触发 AssertionError
```

---

## 8. 日志与重试：记录现场，合理兜底

- 用 `logging` 记录**异常类型、消息、栈**；必要时**重试**（注意退避/上限）。  
- 重试适合**临时性错误**（网络抖动、瞬时超时），**不适合**参数错误。

```python
import logging, time, random
logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")

def flaky_call():
    if random.random() < 0.6:
        raise TimeoutError("超时")
    return "OK"

def call_with_retry(max_retries=3, delay=0.1):
    for i in range(1, max_retries + 1):
        try:
            return flaky_call()
        except TimeoutError as e:
            logging.warning("第 %d 次失败：%s", i, e)
            time.sleep(delay * i)  # 线性退避
    raise RuntimeError("重试仍失败")

# === 调用演示（可能因随机性失败/成功） ===
try:
    print(call_with_retry())
except Exception as e:
    print("最终失败：", e)
```

---

## 9. 常见反模式与修正

- **裸 `except:`** → 换成精确异常或 `except Exception as e:` 并记录日志后抛出/返回明确错误。  
- **吞异常**（只打印不处理）→ 日志 + 重新抛出或返回可判定的错误值。  
- **过度广捕**（捕太多类型）→ 针对不同错误分支，精细化处理。  
- **用异常做正常流程**（如循环结束靠异常）→ 用清晰的条件/返回值控制。

```python
# 反例：吞异常
try:
    1/0
except Exception:
    pass   # 错误被静默，问题难以排查

# 修正：
import logging
try:
    1/0
except ZeroDivisionError as e:
    logging.exception("计算失败")  # 带栈日志
    raise                          # 继续向上抛，让调用方知晓
```

---

## 10. 实战范式清单（可直接套用）

- **函数边界**：先做**输入校验**（类型/取值范围），不满足就 `raise ValueError/TypeError`。  
- **资源操作**（文件/网络/数据库）：用 `with` 包裹；在**外层**捕获并加上业务语义后再抛。  
- **服务调用**：按**重试策略**区分“可重试 vs 不可重试”错误。  
- **库/框架交互**：阅读文档了解**它会抛什么异常**，针对性处理。  
- **对外接口**：统一捕获你的**自定义业务异常**，转为 HTTP/消息队列的错误码与清晰文案。

---

## 11. 练习

- 写 `safe_read(path)`：读文件的第一行，文件不存在返回 `"MISSING"`，其他异常记录日志后抛出。  
- 写 `retry(func, retries=3, backoff=0.2)`：可配置重试的装饰器，只对 `TimeoutError` 生效。  
- 设计异常层级：`PaymentError`（基类）→ `CardError`、`BalanceError`，并在支付流程中使用。

