# 第1章 初识 Python

## 🐍 Python 简介

Python（英式发音：/ˈpaɪθən/；美式发音：/ˈpaɪθɑːn/）是由荷兰人吉多·范罗苏姆（Guido van Rossum）发明的一种编程语言，
是目前世界上最受欢迎和拥有最多用户的编程语言。Python 强调代码的可读性和语法的简洁性，
相较于 C、C++、Java 等编程语言，Python 让使用者能够用更少的代码表达自己的意图。

Python 广泛应用于：

- 大语言模型（GPT-3、GPT-4、BERT 等）
- 计算机视觉（图像识别、目标检测、图像生成等）
- 智能推荐（YouTube、Netflix、字节跳动等）
- 自动驾驶（Waymo、Apollo 等）
- 数据科学、量化交易、自动化测试与运维

---

## 📜 Python 编年史

| 时间 | 事件 |
|------|------|
| 1989年12月 | 吉多·范罗苏姆决定开发新脚本语言 Python |
| 1991年02月 | 发布 Python 0.9.0 |
| 1994年01月 | 发布 Python 1.0 |
| 2000年10月 | 发布 Python 2.0 |
| 2008年12月 | 发布 Python 3.0（不完全兼容 2.x） |
| 2011年04月 | pip 首次发布，Python 拥有包管理工具 |
| 2018年07月 | 吉多宣布从“终身仁慈独裁者”职位退休 |
| 2020年01月 | 官方停止 Python 2 支持，全面转向 Python 3 |

 💡 软件版本号通常为 A.B.C：

- **A**：大版本号（架构性变更或不兼容更新）  
- **B**：功能更新  
- **C**：Bug 修复或小调整  

---

## ⚖️ Python 优缺点

### ✅ 优点

- 语法简单，易学易用；  
- 开发效率高，用更少代码实现更多功能；  
- 免费开源，社区生态强大；  
- 拥有丰富的第三方库，几乎可以做任何事；  
- 解释型语言，跨平台运行；  
- 可与其他语言（C/C++/Java）结合使用，充当“胶水语言”。

### ⚠️ 缺点

- 运行速度较慢（解释执行）；  
- 移动端应用较弱；  
- 多线程受 GIL 限制（适合多进程任务）。

---

## 🧩 安装 Python 环境

> 工欲善其事，必先利其器。想要开始 Python 编程，首先要安装 Python 解释器。

### 🔹 Windows 环境

1. 前往 [Python 官网](https://www.python.org/downloads/) 下载最新版本（推荐 3.10+）。  
2. 安装时务必勾选：  
   - ✅ `Add Python to PATH`  
   - ✅ `Use admin privileges when installing py.exe`  
3. 点击 “Customize Installation”，全选 **Optional Features**，并勾选：  
   - `Add Python to environment variables`  
   - `Precompile standard library`  
4. 安装路径不要包含中文或空格，例如：`D:\Python312\`  
5. 安装完成后，打开命令提示符（cmd）：

```bash
   python --version
   pip --version
```

   若显示版本号，则安装成功。

### ⚙️ 安装失败修复

若安装失败，可通过安装 **Visual Studio 2022 生成工具** 修复缺少的依赖。  
可在微软官网下载或使用以下备用链接：  
[百度网盘链接](https://pan.baidu.com/s/1iNDnU5UVdDX5sKFqsiDg5Q)（提取码：cjs3）

---

### 🍎 macOS 环境

1. 下载 `.pkg` 安装包并双击安装。  
2. 打开终端执行：
  
```bash
   python3 --version
   pip3 --version
```

   若输出版本号，说明安装成功。

---

### 💡 其他安装方式

#### ✅ Anaconda（数据科学方向）

Anaconda 自带 Python 和常用数据分析库。  
缺点：体积大、路径复杂、自动激活环境。

#### ✅ Miniconda（轻量版推荐）

仅包含 Python 环境与基础工具，可自行安装所需包。

#### ✅ PyCharm

PyCharm 是 IDE（开发工具），**不是解释器**，需要依赖系统已安装的 Python。

---

## 🧾 总结

- Python 是一门强大、优雅、易学的语言；  
- 编程前需正确安装 Python 环境；  
- Windows 系统验证命令：`python --version`  
- macOS 系统验证命令：`python3 --version`  
- 建议使用 **VSCode** 或 **PyCharm** 编写 Python 程序。

---
