# FastAPI 开发环境准备

> 本章目标：建立一个可以重复搭建、便于团队协作的 Python Web 项目环境。完成后，终端和编辑器应使用同一个虚拟环境，依赖可以通过文件重新安装，敏感配置不会被提交到 Git。

## 一、学习目标

完成本章后，学员应能够：

- 创建 Python Web 项目目录
- 确认当前终端使用的 Python 版本和解释器路径
- 为项目创建、激活和退出虚拟环境
- 说明虚拟环境与环境变量的区别
- 使用 `python -m pip` 安装和确认项目依赖
- 使用 `requirements.txt` 记录并重建依赖环境
- 说明 `pyproject.toml` 的基本作用
- 区分操作系统环境变量、`.env` 和 Python 虚拟环境
- 编写适合 Python Web 项目的基础 `.gitignore`
- 在 VS Code 或 PyCharm 中选择项目虚拟环境
- 根据检查清单判断开发环境是否准备完成
- 调查常见的解释器、依赖和终端问题

## 二、项目目录结构

本章使用员工管理系统作为练习背景，Python 后端项目目录命名为 `employee_api`。

完成本章后，目录结构如下：

```text
employee_api/
├── .venv/                 # 本机虚拟环境，不提交到 Git
├── app/                   # 应用程序代码
│   ├── __init__.py        # 将 app 标识为 Python 包
│   └── main.py            # Web应用入口
├── tests/                 # 自动化测试代码
│   └── __init__.py
├── .env                   # 本地配置和秘密信息，不提交到 Git
├── .env.example           # 配置项目示例，可以提交
├── .gitignore             # Git 忽略规则
├── pyproject.toml         # 现代 Python 项目配置，当前先了解
├── requirements.txt       # 项目依赖声明文件
└── README.md              # 项目说明和环境搭建步骤
```

各文件当前职责：

| 文件或目录 | 主要作用 | 是否提交到 Git |
| --- | --- | --- |
| `.venv/` | 保存当前项目的 Python 和第三方库 | 否 |
| `app/` | 保存 Web 应用代码 | 是 |
| `tests/` | 保存测试代码 | 是 |
| `.env` | 保存当前电脑的环境配置 | 否 |
| `.env.example` | 告诉成员需要配置哪些变量 | 是 |
| `.gitignore` | 声明不提交的文件 | 是 |
| `requirements.txt` | 声明项目依赖 | 是 |
| `pyproject.toml` | 保存项目元数据或工具配置 | 是 |
| `README.md` | 说明项目用途、搭建和运行方法 | 是 |

不要提交 `.venv/`。它体积较大，并且可能包含与操作系统、Python 路径和本机环境相关的文件。团队成员应根据依赖文件在自己的电脑上重新创建环境。

## 三、创建项目目录

### 3.1 确认项目保存位置

建议将项目放在路径清楚、权限正常的位置，避免一开始放到系统目录中。

Windows 示例：

```text
C:\workspace\employee_api
```

Linux 或 macOS 示例：

```text
~/workspace/employee_api
```

路径可以包含中文或空格，但某些旧工具、脚本和构建流程可能处理不一致。企业培训和团队项目中，建议优先使用简短英文路径。

### 3.2 Windows PowerShell 创建目录

```powershell
New-Item -ItemType Directory -Path C:\workspace\employee_api
Set-Location C:\workspace\employee_api
```

命令说明：

- `New-Item -ItemType Directory`：创建目录
- `-Path`：指定目录位置
- `Set-Location`：进入目录，相当于常见的 `cd`

确认当前位置：

```powershell
Get-Location
```

预期结果应以项目目录结尾：

```text
Path
----
C:\workspace\employee_api
```

### 3.3 Linux 或 macOS Bash 创建目录

```bash
mkdir -p ~/workspace/employee_api
cd ~/workspace/employee_api
pwd
```

预期结果应以项目目录结尾：

```text
/home/user/workspace/employee_api
```

实际用户目录会因电脑而不同。

## 四、确认 Python 解释器

### 4.1 确认 Python 版本

Windows PowerShell：

```powershell
python --version
```

Linux 或 macOS Bash：

```bash
python3 --version
```

项目使用Python 3.12或更高版本。输出形式类似：

```text
Python 3.12.10
```

补丁版本可能不同，只要符合课程和项目要求即可。

### 4.2 确认解释器路径

Windows PowerShell：

```powershell
Get-Command python
```

Linux 或 macOS Bash：

```bash
which python3
```

创建虚拟环境前，显示的是系统安装的 Python。激活虚拟环境后，需要再次执行检查，确认路径已经切换到项目内的 `.venv`。

### 4.3 使用 Python 自己显示解释器路径

下面的命令在 Windows、Linux 和 macOS 中都可以使用；如果系统只提供 `python3`，请将开头的 `python` 改为 `python3`。

```console
python -c "import sys; print(sys.executable)"
```

`sys.executable` 表示当前实际执行的 Python 程序路径。调查“明明安装了库却无法导入”时，这是非常重要的确认信息。

## 五、创建和使用虚拟环境

### 5.1 创建 `.venv`

Windows PowerShell：

```powershell
python -m venv .venv
```

Linux 或 macOS Bash：

```bash
python3 -m venv .venv
```

命令含义：

- `python` 或 `python3`：使用刚才确认的解释器
- `-m venv`：运行 Python 标准库中的 `venv` 模块
- `.venv`：把虚拟环境创建在项目内的 `.venv` 目录

创建动作每个项目通常只做一次，不需要每次打开终端都重新创建。

### 5.2 激活虚拟环境

Windows PowerShell：

```powershell
.\.venv\Scripts\Activate.ps1
```

Linux 或 macOS Bash：

```bash
source .venv/bin/activate
```

Git Bash for Windows：

```bash
source .venv/Scripts/activate
```

激活成功后，终端提示符通常会出现 `(.venv)`：

```text
(.venv) PS C:\workspace\employee_api>
```

提示符只是辅助信息，最终应通过解释器路径确认是否真的激活成功。

### 5.3 激活后会发生什么

激活虚拟环境并不是“启动 Python”，也不是“打开一个服务器”。

激活脚本主要调整当前终端会话中的环境变量，使 `.venv` 的可执行文件目录排在查找路径前面。因此后续输入 `python` 或 `pip` 时，会优先使用 `.venv` 中的程序。

```text
激活前：python → 系统 Python
激活后：python → employee_api/.venv 中的 Python
```

虚拟环境与环境变量不是同一个概念：

- 虚拟环境：隔离 Python 解释器和第三方库的目录
- 环境变量：操作系统传给程序的键值配置

### 5.4 确认虚拟环境是否生效

Windows PowerShell：

```powershell
Get-Command python
python -c "import sys; print(sys.executable)"
python -m pip --version
```

解释器路径应包含：

```text
employee_api\.venv\Scripts\python.exe
```

Linux 或 macOS Bash：

```bash
which python
python -c "import sys; print(sys.executable)"
python -m pip --version
```

解释器路径应包含：

```text
employee_api/.venv/bin/python
```

### 5.5 退出虚拟环境

Windows、Linux 和 macOS 激活环境后都可以执行：

```console
deactivate
```

退出只会恢复当前终端的命令查找路径，不会删除 `.venv`。

## 六、PowerShell 无法激活时怎么办

Windows PowerShell 可能显示类似错误：

```text
无法加载文件 Activate.ps1，因为在此系统上禁止运行脚本。
```

这不是 Python 语法错误，而是 PowerShell 执行策略阻止脚本运行。

企业电脑的安全策略可能由管理员统一管理，不应为了运行课程命令擅自降低系统安全设置。可以选择：

1. 按照公司或培训环境规定申请正确设置。
2. 使用 Command Prompt 的激活脚本：

```bat
.venv\Scripts\activate.bat
```

3. 不激活环境，直接调用虚拟环境中的解释器：

```powershell
.\.venv\Scripts\python.exe -m pip --version
```

不激活也可以使用虚拟环境，关键是明确执行 `.venv` 中的 Python。

## 七、使用 pip 管理依赖

### 7.1 使用 `python -m pip`

下面两条命令看起来相似：

```console
pip install package-name
python -m pip install package-name
```

推荐使用：

```console
python -m pip
```

这样可以明确表示“使用当前这个 Python 对应的 pip”，减少电脑中存在多个 Python 时安装到错误环境的问题。

### 7.2 确认 pip

激活虚拟环境后执行：

```console
python -m pip --version
```

输出路径应指向项目 `.venv`。

### 7.3 更新 pip

创建虚拟环境后，可以在该虚拟环境中更新 pip：

```console
python -m pip install --upgrade pip
```

这条命令需要访问包仓库。公司网络可能使用代理或内部镜像，应优先遵循项目提供的安装说明，不要随意绕过公司网络和证书策略。

### 7.4 安装 Web 项目依赖

安装FastAPI的标准依赖组合：

```console
python -m pip install "fastapi[standard]"
```

`fastapi[standard]` 会安装 FastAPI 以及官方标准可选依赖，其中包括后续运行开发服务和使用常见功能所需的工具。

安装完成后确认：

```console
python -m pip show fastapi
```

输出中应包含包名、版本和安装位置。安装位置应位于当前项目的 `.venv` 中。

再执行导入检查：

```console
python -c "import fastapi; print(fastapi.__version__)"
```

输出应为当前实际安装的 FastAPI 版本。

### 7.5 查看已安装依赖

```console
python -m pip list
```

FastAPI 还会依赖其他包，因此列表中出现多个包是正常现象。

- 直接依赖：项目主动选择安装的库
- 间接依赖：直接依赖为了运行而需要的其他库

## 八、requirements.txt

### 8.1 requirements.txt 的作用

`requirements.txt` 用于记录项目需要安装的 Python 分发包。

它解决的问题是：

```text
新成员拿到代码后，怎样安装项目依赖？
测试环境怎样重建依赖？
部署环境怎样知道需要哪些库？
```

### 8.2 创建 requirements.txt

在项目根目录创建 `requirements.txt`：

```text
fastapi[standard]
```

然后可以使用文件安装：

```console
python -m pip install -r requirements.txt
```

参数说明：

- `install`：安装依赖
- `-r`：从指定 Requirements 文件读取依赖
- `requirements.txt`：当前项目的依赖文件

### 8.3 版本写法

常见版本约束：

| 写法 | 含义 | 示例 |
| --- | --- | --- |
| 不写版本 | 安装当前可用版本 | `fastapi` |
| `==` | 固定为指定版本 | `package-name==1.2.3` |
| `>=` | 不低于指定版本 | `package-name>=1.2.3` |
| `~=` | 使用兼容版本范围 | `package-name~=1.2` |

课程示例不长期固定某个 FastAPI 版本，避免文档中的版本快速过期。实际企业项目为了可重复构建，通常会固定或锁定经过验证的版本。

版本策略应由项目统一决定，不要每位成员各自修改。

### 8.4 pip freeze

查看当前环境中全部已安装包及精确版本：

```console
python -m pip freeze
```

`pip freeze` 会列出当前环境中的直接依赖和间接依赖。它适合记录一个可以重建的环境快照，但输出不等于经过整理的项目依赖设计。

如果项目要求把输出保存为锁定文件，应使用项目指定的命令和文件名。不同 Shell 的重定向与文本编码行为可能不同，不要未经确认就覆盖现有依赖文件。真实项目也可能使用其他锁定工具或文件，应遵循项目现有方案，不要同时维护多套互相冲突的依赖来源。

### 8.5 重新创建环境

虚拟环境出现问题时，一般不需要把 `.venv` 发送给别人。正确思路是：

1. 确认依赖文件已经保存。
2. 删除或移走本机损坏的 `.venv`。
3. 重新创建虚拟环境。
4. 激活新环境。
5. 从依赖文件重新安装。

删除目录可能造成数据丢失。操作前必须确认目标确实是当前项目的 `.venv`，而不是项目代码或其他目录。

## 九、pyproject.toml 基础

### 9.1 pyproject.toml 是什么

`pyproject.toml` 是现代 Python 项目常见的统一配置入口，可以保存：

- 项目名称和版本
- Python 版本要求
- 项目依赖
- 构建系统配置
- pytest、格式化工具和静态检查工具配置

它不是虚拟环境，也不会因为文件存在就自动安装依赖。

### 9.2 基础配置片段

下面通过一个基础片段认识 `pyproject.toml` 的结构：

```toml
[project]
name = "employee-api"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
    "fastapi[standard]",
]
```

内容说明：

| 配置 | 作用 |
| --- | --- |
| `[project]` | 项目元数据区域 |
| `name` | 项目名称，发布名称通常使用连字符 |
| `version` | 当前项目版本 |
| `requires-python` | 项目要求的 Python 版本 |
| `dependencies` | 项目运行依赖 |

### 9.3 requirements.txt 与 pyproject.toml 如何配合

`requirements.txt` 便于新人先理解“声明依赖”和“从文件安装依赖”的过程。

当前项目采用：

- 使用 `requirements.txt` 作为安装主线
- 使用 `pyproject.toml` 认识现代项目配置
- 工具配置也可以逐步放入 `pyproject.toml`
- 不在同一阶段要求学员同时掌握多个依赖管理工具

进入真实项目后，应先阅读现有 README 和构建文件，确认项目使用 `requirements.txt`、`pyproject.toml`、Poetry、uv 还是其他方案。

## 十、环境变量与 .env

### 10.1 什么是环境变量

环境变量是操作系统提供给程序的键值配置。

Web 项目经常使用环境变量保存：

- 当前运行环境，例如 `local`、`test`、`production`
- 数据库连接地址
- 外部 API 地址
- 日志级别
- 密钥和访问凭据

代码不应为每个环境写死不同配置：

```python
# 不推荐：把环境相关地址直接写死在代码中
database_url = "postgresql://user:password@production-server/app"
```

硬编码会增加泄密风险，也会让同一份代码难以在不同环境运行。

### 10.2 设置临时环境变量

Windows PowerShell：

```powershell
$env:APP_ENV = "local"
python -c "import os; print(os.getenv('APP_ENV'))"
```

输出：

```text
local
```

Linux 或 macOS Bash：

```bash
export APP_ENV="local"
python -c "import os; print(os.getenv('APP_ENV'))"
```

输出：

```text
local
```

这些设置通常只对当前终端会话以及从该终端启动的程序有效。关闭终端后不一定保留。

### 10.3 .env 文件

`.env` 是常见的本地配置文件约定，例如：

```dotenv
APP_ENV=local
DATABASE_URL=sqlite:///./employee.db
LOG_LEVEL=DEBUG
SECRET_KEY=replace-with-local-secret
```

需要明确：

- `.env` 只是普通文本文件，不是加密文件。
- Python 不会自动读取 `.env`。
- 需要由框架、配置库或应用代码加载。
- `.env` 可能包含秘密信息，不应提交到 Git。

本章只准备文件。具体读取方式将在“配置、日志与异常”章节讲解。

### 10.4 .env.example

团队成员需要知道项目要求哪些变量，但又不能共享真实秘密，因此应提供 `.env.example`：

```dotenv
APP_ENV=local
DATABASE_URL=sqlite:///./employee.db
LOG_LEVEL=DEBUG
SECRET_KEY=
```

`.env.example` 只保留字段名和安全的示例值，不应包含真实密码、Token 或正式环境地址。

### 10.5 环境变量通常是字符串

从环境变量读取的值通常是字符串：

```python
import os

debug_text = os.getenv("DEBUG", "false")
print(type(debug_text))  # <class 'str'>
```

读取布尔值、数字和列表时，需要由配置代码或配置库进行明确转换与校验。

## 十一、编写 .gitignore

### 11.1 `.gitignore` 的作用

`.gitignore` 告诉 Git 哪些未跟踪文件不应加入版本管理。

Python Web 项目通常不提交：

- 虚拟环境
- Python 缓存
- 测试与工具缓存
- 本地秘密配置
- 本地数据库和日志
- IDE 的个人配置

### 11.2 基础 .gitignore

在项目根目录创建 `.gitignore`：

```gitignore
# Virtual environment
.venv/

# Python cache
__pycache__/
*.py[cod]

# Test and tool caches
.pytest_cache/
.mypy_cache/
.ruff_cache/

# Local environment and secrets
.env
.env.*
!.env.example

# Local database and logs
*.db
*.sqlite3
*.log

# IDE local settings: follow the project policy
.idea/
.vscode/
```

说明：

- `!.env.example` 表示即使忽略 `.env.*`，仍允许提交 `.env.example`。
- `.idea/` 和 `.vscode/` 是否提交由团队决定。有些项目会共享部分 IDE 配置。
- `requirements.txt`、`pyproject.toml`、`.gitignore`、`.env.example` 和 `README.md` 应提交。

### 11.3 `.gitignore` 不能撤销已经提交的秘密

如果文件已经被 Git 跟踪，后来再加入 `.gitignore`，不会自动从提交历史中消失。

如果真实密钥已经提交，应立即：

1. 按项目流程报告。
2. 使泄露的密钥失效并重新签发。
3. 按团队方案处理仓库历史和受影响环境。

不能只删除文件然后认为风险已经解除。

## 十二、创建应用与测试目录

### 12.1 Windows PowerShell

```powershell
New-Item -ItemType Directory -Path app
New-Item -ItemType Directory -Path tests
New-Item -ItemType File -Path app\__init__.py
New-Item -ItemType File -Path app\main.py
New-Item -ItemType File -Path tests\__init__.py
```

### 12.2 Linux 或 macOS Bash

```bash
mkdir -p app tests
touch app/__init__.py app/main.py tests/__init__.py
```

### 12.3 `__init__.py` 的作用

`__init__.py` 用于把目录作为普通 Python 包使用。现代 Python 支持没有 `__init__.py` 的命名空间包，但新人课程和常见应用项目中显式保留它，更容易理解导入边界。

`app/main.py` 是FastAPI应用入口文件。

## 十三、编写 README.md

README 应让新成员知道项目是什么、如何准备环境以及怎样验证。

本阶段可以使用：

```markdown
# Employee API

员工管理 API 课程项目。

## 环境要求

- Python 3.12+

## 环境准备

1. 创建虚拟环境：`python -m venv .venv`
2. 激活虚拟环境
3. 安装依赖：`python -m pip install -r requirements.txt`
4. 复制 `.env.example` 为 `.env` 并填写本地配置

## 环境确认

- `python --version`
- `python -c "import sys; print(sys.executable)"`
- `python -m pip show fastapi`
```

README 中的命令必须随着项目变化同步更新。错误的 README 会让新成员重复踩坑。

## 十四、配置 VS Code

### 14.1 必要准备

安装 VS Code 的 Python 扩展后，打开项目根目录 `employee_api`，不要只打开单个 Python 文件。

### 14.2 选择解释器

1. 打开命令面板。
2. 执行 `Python: Select Interpreter`。
3. 选择项目 `.venv` 中的 Python。

Windows 路径通常是：

```text
employee_api\.venv\Scripts\python.exe
```

Linux 或 macOS 路径通常是：

```text
employee_api/.venv/bin/python
```

### 14.3 确认 VS Code 终端

新建终端后执行：

```console
python -c "import sys; print(sys.executable)"
```

输出应指向 `.venv`。如果编辑器可以自动补全 `fastapi`，但终端无法导入，或情况相反，通常表示两者使用了不同解释器。

## 十五、配置 PyCharm

1. 使用 PyCharm 打开 `employee_api` 项目目录。
2. 打开项目 Python Interpreter 设置。
3. 选择 Add Interpreter 或 Existing Environment。
4. 指向项目 `.venv` 中的解释器。

Windows：

```text
employee_api\.venv\Scripts\python.exe
```

Linux 或 macOS：

```text
employee_api/.venv/bin/python
```

配置后，在 PyCharm 内置终端再次检查 `sys.executable`。不要只根据界面显示的环境名称判断。

## 十六、API 调试工具准备

本章只确认工具可用，不重复讲 HTTP 调试方法。具体使用参见[HTTP、REST、Cookie、Session 与 CORS](../../../web_basics/01_http_rest_cookie_cors.md)。

建议至少准备一种：

- 浏览器及开发者工具
- `curl`
- Postman
- 框架提供的 Swagger UI

FastAPI启动后，可以直接使用浏览器和Swagger UI验证接口，也可以根据项目需要使用其他API客户端。

## 十七、从零搭建与从仓库恢复的区别

### 17.1 第一次创建项目

```text
创建项目目录
→ 创建 .venv
→ 激活并确认解释器
→ 创建 requirements.txt
→ 安装依赖
→ 创建 app 和 tests
→ 准备 .env.example、.gitignore、README
```

### 17.2 加入已有项目

```text
取得项目代码
→ 阅读 README
→ 确认要求的 Python 版本
→ 创建自己的 .venv
→ 从依赖文件安装依赖
→ 根据 .env.example 创建自己的 .env
→ 执行项目提供的验证或测试命令
```

加入已有项目时，不要自行更换依赖管理工具、升级全部版本或修改目录结构。先按照现有项目约定成功运行，再讨论改进方案。

## 十八、环境完成检查

依次执行：

```console
python --version
python -c "import sys; print(sys.executable)"
python -m pip --version
python -m pip show fastapi
python -c "import fastapi; print(fastapi.__version__)"
```

检查结果：

- Python 版本符合课程要求
- `sys.executable` 指向项目 `.venv`
- pip 的安装位置位于项目 `.venv`
- 可以找到 FastAPI
- 可以正常导入 FastAPI

同时确认文件：

```text
[ ] requirements.txt 已创建
[ ] .gitignore 已创建
[ ] .env 已被忽略
[ ] .env.example 不包含秘密
[ ] app/main.py 已创建
[ ] README 包含环境搭建步骤
[ ] VS Code 或 PyCharm 使用项目 .venv
```

检查全部通过后，开发环境才具备编写和运行Web接口的条件。

## 十九、常见错误与解决方法

### 19.1 ModuleNotFoundError: No module named 'fastapi'

错误信息：

```text
ModuleNotFoundError: No module named 'fastapi'
```

常见原因：

- FastAPI 没有安装
- 安装到了另一个 Python
- 虚拟环境没有激活
- IDE 与终端使用不同解释器

检查：

```console
python -c "import sys; print(sys.executable)"
python -m pip show fastapi
```

修正：确认解释器指向 `.venv`，再使用同一个 Python 安装依赖：

```console
python -m pip install -r requirements.txt
```

### 19.2 python 命令不存在

可能现象：

```text
python: command not found
```

处理方向：

- Linux 或 macOS 尝试 `python3`
- Windows 检查 Python 是否正确安装及 PATH 配置
- Windows 也可以检查 `py --version`
- 按项目要求确认 Python 版本

不要在不清楚系统 Python 用途时随意删除或替换系统解释器。

### 19.3 pip 安装成功但仍然无法导入

通常是 `pip` 与 `python` 不属于同一个环境。

对比：

```console
python -c "import sys; print(sys.executable)"
python -m pip --version
```

两个路径都应指向当前项目 `.venv`。

### 19.4 终端显示 (.venv)，解释器却不正确

提示符可能被自定义或残留。以 `sys.executable` 和 `Get-Command python` / `which python` 的实际结果为准。

### 19.5 requirements.txt 安装失败

检查：

- 是否已激活正确虚拟环境
- Python 版本是否符合要求
- 包名和版本是否存在
- 公司网络是否需要代理或内部镜像
- 错误发生在下载、构建还是版本冲突阶段

不要只截取错误最后一行。提交问题时应保留完整命令、Python 版本和关键错误上下文，同时删除账号、Token 和内部地址等敏感信息。

### 19.6 .env 没有生效

`.env` 文件不会被 Python 自动读取，需要由配置代码或配置库加载。

先确认问题属于：

- 操作系统环境变量没有设置
- `.env` 没有被加载
- 变量名拼写不一致
- 读取后没有进行类型转换

### 19.7 .gitignore 没有效果

`.gitignore` 主要影响尚未被 Git 跟踪的文件。已经提交过的文件不会因为后来增加忽略规则而自动消失。

如果涉及秘密信息，应立即按项目安全流程处理，不能只依靠 `.gitignore`。

## 二十、企业项目与日本现场中的环境准备

环境搭建在日本项目中经常对应“環境構築”或“開発環境構築”。新人常见任务包括：

- 阅读环境构筑手顺书
- 安装指定 Python 版本
- 创建虚拟环境
- 从公司内部包仓库安装依赖
- 准备本地配置文件
- 启动项目并保存确认结果
- 报告步骤、结果和发生的问题

需要注意：

- 严格确认目标环境，不把测试配置用于正式环境
- 不擅自升级项目依赖
- 不提交密码、Token、证书和正式环境地址
- 修改环境步骤后同步 README 或环境构筑手顺书
- 报告问题时说明操作系统、Python 版本、执行命令和错误信息
- 保存证据时遮盖个人信息和秘密值

环境问题的有效报告示例：

```text
操作系统：Windows 11
Python：3.12.x
项目目录：C:\workspace\employee_api
执行命令：python -m pip install -r requirements.txt
发生阶段：安装依赖
实际结果：指定依赖版本无法解析
已确认：sys.executable 指向项目 .venv
影响范围：当前只有本地环境无法启动
```

这比只说“环境装不上”更容易让其他成员协助调查。

## 二十一、基础练习

### 练习一：确认解释器

1. 创建 `employee_api` 项目目录。
2. 创建并激活 `.venv`。
3. 输出 Python 版本。
4. 输出 `sys.executable`。
5. 确认路径中包含当前项目的 `.venv`。

### 练习二：安装和确认依赖

1. 创建包含 `fastapi[standard]` 的 `requirements.txt`。
2. 使用 `python -m pip install -r requirements.txt` 安装。
3. 使用 `python -m pip show fastapi` 确认安装位置。
4. 使用 Python 命令导入 FastAPI 并输出实际版本。

### 练习三：准备配置文件

1. 创建 `.env` 和 `.env.example`。
2. 两个文件都包含 `APP_ENV`、`DATABASE_URL`、`LOG_LEVEL` 和 `SECRET_KEY`。
3. `.env.example` 不填写真实秘密。
4. 编写 `.gitignore`，忽略 `.env` 但保留 `.env.example`。

## 二十二、综合练习

模拟一名新成员加入 `employee_api` 项目：

1. 根据本章结构创建项目文件。
2. 从 `requirements.txt` 重建虚拟环境。
3. 根据 `.env.example` 创建自己的 `.env`。
4. 在 VS Code 或 PyCharm 中选择 `.venv`。
5. 完成“环境完成检查”中的全部命令和文件检查。
6. 在 README 中记录从空目录到环境准备完成的步骤。
7. 故意退出虚拟环境，再观察解释器路径发生什么变化。
8. 重新激活环境并确认 FastAPI 可以导入。

练习结果以环境检查清单全部通过为准。

## 二十三、本章总结

- Python Web 项目应使用独立虚拟环境隔离依赖。
- 激活环境后仍应通过 `sys.executable` 确认实际解释器。
- 使用 `python -m pip` 可以减少把依赖安装到错误 Python 的情况。
- `requirements.txt` 用于声明和重建依赖，`pip freeze` 可以记录当前环境快照。
- `pyproject.toml` 是现代Python项目的统一配置入口，当前项目使用 `requirements.txt` 安装依赖。
- 虚拟环境、环境变量和 `.env` 是三个不同概念。
- `.env` 不是加密文件，也不会被 Python 自动读取。
- `.gitignore` 不能撤销已经提交到历史中的秘密。
- IDE 和终端必须使用同一个项目解释器。
- 环境准备完成的标准是可确认、可重建、可协作，而不只是“我的电脑能运行”。

## 官方参考

- [Python Packaging User Guide：使用 pip 和 venv 安装包](https://packaging.python.org/en/latest/guides/installing-using-pip-and-virtual-environments/)
- [Python Packaging User Guide：安装 Python 包](https://packaging.python.org/en/latest/tutorials/installing-packages/)
- [FastAPI：Virtual Environments](https://fastapi.tiangolo.com/virtual-environments/)
