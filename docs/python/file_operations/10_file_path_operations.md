# 第10章 文件与路径操作、文件读写与异常处理

> 🎯 学习目标  
>
> - 掌握 Python 文件与路径操作、文件读写模式、异常与日志记录机制  
> - 在 Web 系统中能安全地进行文件导入导出、日志记录与错误捕获

---

## 一、路径与文件操作模块

### 使用的模块

- **os**：操作系统接口（文件、目录）
- **pathlib**：面向对象的路径操作（推荐）
- **shutil**：文件复制与压缩
- **glob**：文件通配符搜索

### 常用函数与参数

| 模块 | 函数 | 说明 |
|------|------|------|
| os | os.getcwd() | 获取当前工作目录 |
| os | os.listdir(path) | 列出目录下的文件 |
| os | os.makedirs(path, exist_ok=True) | 递归创建目录 |
| shutil | shutil.copy(src, dst) | 复制文件 |
| shutil | shutil.move(src, dst) | 移动文件 |
| pathlib | Path().exists() | 判断路径是否存在 |
| pathlib | Path().mkdir(exist_ok=True) | 创建目录 |
| glob | glob("*.txt") | 匹配所有 txt 文件 |

示例：

```python
from pathlib import Path
import os, shutil, glob

base = Path("data")
base.mkdir(exist_ok=True)
print(os.getcwd())
for f in glob.glob("*.py"):
    shutil.copy(f, base / f)
```

---

## 二、文件读写操作

### 1️⃣ open() 函数模式

| 模式 | 含义 |
|------|------|
| r | 只读（默认） |
| w | 写入（覆盖原文件） |
| a | 追加写入 |
| x | 创建新文件写入 |
| b | 二进制模式 |
| t | 文本模式（默认） |
| + | 读写模式 |

示例：

```python
with open("example.txt", "w", encoding="utf-8") as f:
    f.write("Hello Web Developer!\n")
with open("example.txt", "r", encoding="utf-8") as f:
    print(f.read())
```

---

### 2️⃣ 文件指针与二进制文件

```python
with open("binary.dat", "wb") as f:
    f.write(b"PythonBytes")

with open("binary.dat", "rb") as f:
    data = f.read()
    print(data)
```

---

## 三、异常处理机制

### 使用的模块

- **logging**：日志记录
- **traceback**：异常堆栈追踪

### try/except/finally 结构

```python
try:
    with open("config.yaml") as f:
        data = f.read()
except FileNotFoundError:
    print("文件不存在")
except PermissionError:
    print("无访问权限")
except Exception as e:
    print(f"未知错误: {e}")
finally:
    print("执行完成")
```

### logging 用法

```python
import logging

logging.basicConfig(
    filename="logs/app.log",
    level=logging.ERROR,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

try:
    1 / 0
except Exception as e:
    logging.exception("程序出错：%s", e)
```

---

## 四、Web 应用中的文件操作场景

| 场景 | 功能 | 相关模块 |
|------|------|----------|
| 上传文件 | 保存用户上传内容 | os, pathlib |
| 导出报表 | 写入 CSV/Excel | open, pandas |
| 记录异常 | 写入日志文件 | logging |
| 处理配置 | 读取 JSON/YAML | open, json, yaml |

---

## 五、实战练习

> 任务：编写一个脚本，将当前目录的所有 `.txt` 文件复制到 `backup/` 文件夹中，并将失败信息记录在日志。

```python
from pathlib import Path
import shutil, logging

logging.basicConfig(filename="copy.log", level=logging.INFO)

src = Path(".")
dst = Path("backup")
dst.mkdir(exist_ok=True)

for file in src.glob("*.txt"):
    try:
        shutil.copy(file, dst / file.name)
        logging.info(f"复制成功: {file.name}")
    except Exception as e:
        logging.error(f"复制失败: {file.name} - {e}")
```

---

## ✅ 小结

| 模块 | 功能 | Web 应用价值 |
|------|------|---------------|
| os/pathlib | 文件与路径管理 | 静态资源管理 |
| open() | 文件IO | 数据存储/日志 |
| logging | 异常日志 | 稳定性保障 |
| shutil | 复制移动文件 | 上传/备份 |

---
