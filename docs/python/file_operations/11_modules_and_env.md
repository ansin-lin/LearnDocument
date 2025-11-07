# 第11章 模块与包管理、虚拟环境与正则表达式

> 🎯 学习目标  
>
> - 理解 import 原理、模块与包结构  
> - 掌握 pip/venv/poetry 的使用  
> - 学会使用正则表达式在 Web 系统中进行数据验证与文本处理

---

## 一、模块与包管理基础

### 常用命令与概念

| 工具 | 功能 | 命令示例 |
|------|------|-----------|
| pip | 安装包 | pip install requests |
| venv | 创建虚拟环境 | python -m venv venv |
| poetry | 管理依赖与发布 | poetry add flask |

项目结构示例：

```python
project/
 ├── app/
 │   ├── __init__.py
 │   ├── main.py
 │   └── utils.py
 ├── tests/
 └── run.py
```

`__init__.py` 使文件夹成为可导入包。

---

## 二、import 原理

Python 搜索模块的顺序：

1. 当前目录  
2. PYTHONPATH 环境变量  
3. 系统目录

可查看路径：

```python
import sys
print(sys.path)
```

---

## 三、正则表达式（re 模块）

### 常用函数

| 函数 | 功能 |
|------|------|
| re.match() | 从字符串开头匹配 |
| re.search() | 扫描整个字符串匹配 |
| re.findall() | 返回所有匹配项列表 |
| re.sub() | 替换匹配内容 |
| re.split() | 按模式分割字符串 |

### 示例：匹配邮箱与手机号

```python
import re

text = "Email: test@mail.com, Phone: 13800138000"
email = re.findall(r"[\w.-]+@[\w.-]+", text)
phone = re.findall(r"1[3-9]\d{9}", text)
print(email, phone)
```

### 参数说明

- `re.I`：忽略大小写
- `re.M`：多行匹配
- `re.S`：匹配换行符

---

## 四、常见正则场景

| 场景 | 示例 | 应用 |
|------|------|------|
| 表单验证 | 邮箱、手机号 | 用户注册 |
| 日志分析 | 提取 IP 地址 | 访问日志 |
| 数据清洗 | 提取数字或标签 | 报表导入 |

示例：

```python
log = "IP=192.168.1.8 user=admin"
ip = re.search(r"\d+\.\d+\.\d+\.\d+", log).group()
print(ip)
```

---

## ✅ 小结

| 模块 | 功能 | Web 应用 |
|------|------|----------|
| re | 文本匹配与清洗 | 数据验证、日志分析 |
| pip/venv/poetry | 包管理 | 依赖隔离 |
| import | 模块加载 | 组织项目结构 |

---
