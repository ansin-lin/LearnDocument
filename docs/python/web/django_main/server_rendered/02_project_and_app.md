# 第2章 创建 Django 项目与 App

## 一、本章完成目标

本章将创建贯穿课程使用的 Django 项目。完成后，你应能够：

- 在虚拟环境中安装指定范围的 Django
- 创建 `company_portal` 项目和 `employees` App
- 说明项目目录与 App 的职责区别
- 注册 App 并启动开发服务器
- 使用命令和浏览器验证项目状态
- 根据检查顺序调查常见启动问题

## 二、本章开始状态

开始前应已经完成 [Python 入门与环境](../../../common/00_intro.md)，并能够：

- 打开 PowerShell 或 Bash
- 创建和激活 `.venv`
- 使用 `python -m pip`
- 确认当前 Python 解释器

环境准备课程中的 `employee_api` 只是命令练习目录。本章会另外创建正式课程项目 `company_portal`；不要在原来的 `employee_api` 目录中继续执行下面的命令。

课程基线如下：

| 项目 | 基线 |
| --- | --- |
| Python | 3.12 或更高版本 |
| Django | 5.2 LTS 系列 |
| 初始数据库 | SQLite |
| 项目目录 | `company_portal` |
| 业务 App | `employees` |

Django 5.2 是长期支持版本。Django 5.2 官方支持 Python 3.10 及以上版本；本课程把 Python 3.12 及以上作为统一的教学基线。课程使用 `Django>=5.2,<5.3`，允许安装5.2系列的安全和错误修复版本，同时避免自动升级到下一个大版本。

## 三、创建课程项目目录

下面的命令会新建项目目录。路径可根据自己的电脑调整。

Windows PowerShell：

```powershell
New-Item -ItemType Directory -Path C:\workspace\company_portal -Force
Set-Location C:\workspace\company_portal
```

Linux 或 macOS Bash：

```bash
mkdir -p ~/workspace/company_portal
cd ~/workspace/company_portal
```

确认当前目录：

```text
company_portal
```

PowerShell 的 `-Force` 可以在目录已存在时继续执行，但不会清空目录。若该目录已经存放其他项目或文件，请换一个空目录，不要覆盖原有内容。

`New-Item -ItemType Directory -Path <路径> -Force` 创建目录：`-ItemType` 指定资源类型，`-Path` 必须提供目标路径，`-Force` 允许目录已存在时继续；返回创建或取得的目录对象。`Set-Location <路径>` 切换当前目录。Bash中的 `mkdir -p <路径>` 和 `cd <路径>` 分别完成相同目的。执行后必须确认自己位于空的项目目录。

## 四、创建并激活虚拟环境

Windows PowerShell：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

Linux 或 macOS Bash：

```bash
python3 -m venv .venv
source .venv/bin/activate
```

`python -m venv <目录>` 使用当前Python创建隔离环境，目录参数必填；成功后生成独立解释器和包目录，不会自动激活。PowerShell的 `Activate.ps1` 和Bash的 `source .../activate` 修改当前终端环境，使后续 `python`、`pip` 指向 `.venv`；只对当前终端会话有效。

激活后确认解释器：

```powershell
python -c "import sys; print(sys.executable)"
```

`python -c <代码>` 执行命令行给出的Python代码并退出；`-c` 后的代码字符串必填。这里的结果通过终端输出解释器路径，用于验证环境，不修改项目文件。

输出路径应指向当前项目的 `.venv`。如果仍然指向系统 Python，先解决虚拟环境问题再继续。

### 创建基础 `.gitignore`

在项目根目录创建 `.gitignore`：

```gitignore
.venv/
__pycache__/
*.py[cod]
db.sqlite3
```

这些内容是本机生成的虚拟环境、缓存和开发数据库，不应提交到 Git。`.gitignore` 只会忽略尚未被 Git 跟踪的文件；如果文件已经提交过，还需要先确认团队处理方式，不能直接删除他人的文件。

## 五、记录并安装 Django

在项目根目录新建：

```text
requirements.txt
```

内容：

```text
Django>=5.2,<5.3
```

安装依赖：

```powershell
python -m pip install -r requirements.txt
```

这条命令中，`python -m` 使用当前Python解释器运行模块，避免把依赖装到其他Python环境；`pip install` 安装依赖；`-r` 后必须提供依赖文件路径。成功后，当前虚拟环境中会安装 `requirements.txt` 列出的Django版本；执行时机是创建或更新项目环境之后。

确认版本：

```powershell
python -m django --version
```

预期输出以 `5.2` 开头，例如：

```text
5.2.x
```

补丁版本会随安全更新变化，不要求和示例完全相同。

## 六、创建 Django 项目

确认当前仍在外层 `company_portal` 目录，然后执行：

```powershell
python -m django startproject company_portal .
```

`startproject` 创建Django项目骨架；第一个位置参数 `company_portal` 是项目配置包名，第二个位置参数 `.` 是目标目录，表示写入当前目录。命令成功时会生成 `manage.py` 和配置包；它只在项目初始化时执行，不要在已有项目目录中重复执行。

完成后的关键结构如下：

```text
company_portal/
├── .gitignore
├── .venv/
├── company_portal/
│   ├── __init__.py
│   ├── asgi.py
│   ├── settings.py
│   ├── urls.py
│   └── wsgi.py
├── manage.py
└── requirements.txt
```

外层 `company_portal` 是工作目录，内层 `company_portal` 是 Python 配置包。两者同名不会影响运行，但职责不同。

## 七、认识最常用的项目文件

| 文件 | 当前作用 |
| --- | --- |
| `manage.py` | 执行启动、迁移、测试等项目命令 |
| `company_portal/settings.py` | 保存 App、数据库、模板、语言和时区等配置 |
| `company_portal/urls.py` | 项目最外层 URL 入口 |
| `wsgi.py`、`asgi.py` | 部署和服务器接入入口，当前先认识名称 |

本章只修改必要配置，不一次讲完 `settings.py` 的所有选项。

## 八、创建 employees App

Django 项目可以包含多个 App。项目负责整体配置，App 负责一组相关业务。

执行：

```powershell
python manage.py startapp employees
```

`startapp` 创建App代码骨架，必填位置参数 `employees` 同时成为Python包名和默认App标签。执行结果是新增 `employees/` 目录，但不会自动写入 `INSTALLED_APPS`；每个业务App通常只创建一次，创建后还必须注册。

新增结构如下：

```text
employees/
├── migrations/
│   └── __init__.py
├── __init__.py
├── admin.py
├── apps.py
├── models.py
├── tests.py
└── views.py
```

后续员工页面、模型、表单和测试都以这个 App 为主线。

## 九、注册 employees App

打开：

```text
company_portal/settings.py
```

找到 `INSTALLED_APPS`，追加 `employees.apps.EmployeesConfig`：

```python
INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "employees.apps.EmployeesConfig",
]
```

这里是“追加”，不要删除 Django 默认 App。默认 App 为后台管理、登录状态、消息和静态文件等功能提供基础支持。

## 十、设置语言和时区

在同一个 `settings.py` 中确认：

```python
LANGUAGE_CODE = "zh-hans"
TIME_ZONE = "Asia/Tokyo"
USE_I18N = True
USE_TZ = True
```

| 配置 | 当前值 | 可接受的值与必填性 | 作用 |
|---|---|---|---|
| `LANGUAGE_CODE` | `"zh-hans"` | Django支持的语言代码；项目必须有有效值 | 决定框架默认显示语言 |
| `TIME_ZONE` | `"Asia/Tokyo"` | IANA时区名称；项目必须有有效值 | 定义项目默认业务时区 |
| `USE_I18N` | `True` | 布尔值，默认启用 | 是否启用Django翻译系统 |
| `USE_TZ` | `True` | 布尔值，Django新项目默认启用 | 是否使用时区感知的日期时间 |

`TIME_ZONE` 是 Django 的默认时区，会影响表单输入的日期时间如何解释，以及模板中日期时间如何显示。日志时间还会受到日志配置和操作系统环境影响，不能只依靠这个选项判断。课程以日本业务时间 `Asia/Tokyo` 统一练习。

## 十一、检查项目配置

执行：

```powershell
python manage.py check
```

预期结果：

```text
System check identified no issues (0 silenced).
```

`check` 会检查一部分 Django 配置，但不能证明所有业务功能都正确。

## 十二、启动开发服务器

执行：

```powershell
python manage.py runserver
```

`runserver [addrport]` 启动开发服务器；不传参数时默认监听 `127.0.0.1:8000`，传入 `8001` 可改端口，也可传 `IP:端口`。命令会持续运行并输出请求日志，使用 `Ctrl+C` 停止；只在本地开发和验证时执行，不能作为生产服务器。

终端应出现类似地址：

```text
Starting development server at http://127.0.0.1:8000/
```

第一次启动时，终端还可能出现 `You have ... unapplied migration(s)`。这是新项目尚未建立 Django 默认数据表的提示，不会阻止本章查看欢迎页面。本课程会在第6章学习 Model 和迁移后统一执行 `migrate`，现在先不要为了消除提示而跳过课程顺序。

浏览器访问：

```text
http://127.0.0.1:8000/
```

看到 Django 安装成功页面，说明：

- Python 可以导入 Django
- 项目配置可以加载
- 开发服务器可以监听端口
- 浏览器可以收到响应

按 `Ctrl+C` 停止开发服务器。`runserver` 只用于开发和学习，不用于生产环境。

## 十三、本章完成后的目录

```text
company_portal/
├── .gitignore
├── .venv/
├── company_portal/
│   ├── settings.py
│   ├── urls.py
│   └── ...
├── employees/
│   ├── migrations/
│   ├── admin.py
│   ├── apps.py
│   ├── models.py
│   ├── tests.py
│   └── views.py
├── manage.py
└── requirements.txt
```

第3章会在这个状态上添加第一个 View 和 URL，不重新创建项目。

## 十四、常见问题与调查顺序

### 14.1 `No module named django`

依次确认：

```powershell
python -c "import sys; print(sys.executable)"
python -m pip show Django
```

`python -m pip show <包名>` 的包名必填，输出已安装版本、位置和依赖等元数据，不修改环境。这里用于调查“当前解释器是否安装了Django以及装在哪里”；如果没有输出，应先确认虚拟环境和安装步骤。

常见原因是虚拟环境未激活，或者安装依赖时使用了另一个 Python。

### 14.2 找不到 `manage.py`

执行：

```powershell
Get-Location
Get-ChildItem
```

Linux 或 macOS 使用 `pwd` 和 `ls`。必须在包含 `manage.py` 的目录执行项目命令。

### 14.3 端口已被占用

学习阶段可以临时使用另一个端口：

```powershell
python manage.py runserver 8001
```

然后访问 `http://127.0.0.1:8001/`。同时应调查原端口由哪个程序占用，不要直接结束未知进程。

### 14.4 App 已创建但没有注册

确认 `INSTALLED_APPS` 中存在：

```python
"employees.apps.EmployeesConfig",
```

## 十五、练习

### 练习1：重新验证环境

关闭终端后重新打开，在项目目录完成：

1. 激活虚拟环境。
2. 确认解释器路径。
3. 确认 Django 版本。
4. 执行 `python manage.py check`。
5. 启动服务器并打开页面。

### 练习2：调查错误目录

故意进入上一级目录执行 `python manage.py check`，记录错误现象，然后返回正确目录修复。不要修改或删除文件。

### 练习3：解释项目与 App

根据当前目录，用自己的话说明：

- `company_portal/settings.py` 为什么属于项目配置
- `employees/views.py` 为什么属于员工业务 App

## 十六、项目运行检查

- [ ] 当前解释器位于 `.venv`
- [ ] `.gitignore` 已忽略虚拟环境、Python 缓存和开发数据库
- [ ] `requirements.txt` 记录 Django 5.2 LTS 范围
- [ ] `python -m django --version` 输出5.2系列版本
- [ ] `employees` 已创建并注册
- [ ] `python manage.py check` 没有问题
- [ ] 浏览器能够打开开发服务器
- [ ] 知道首次启动时未应用迁移提示的含义
- [ ] 知道如何停止开发服务器并重新启动

## 十七、现场识读：`django-admin` 与 `manage.py` 的关系

`django-admin` 是 Django 安装后提供的通用管理命令；`manage.py` 会自动指定当前项目的 settings，所以进入项目后通常使用 `python manage.py ...`。例如创建项目时使用 `django-admin startproject`，项目创建后使用 `python manage.py startapp`、`runserver`、`migrate` 和 `test`。

运行命令前先确认终端所在目录和虚拟环境。现场最常见的“同一命令在我电脑可以、在别人电脑不行”，往往来自 Python 解释器、依赖版本、环境变量或当前目录不同。

## 十八、读懂核心配置，而不是背配置名

| 配置 | 作用 | 开发现场的检查重点 |
|---|---|---|
| `BASE_DIR` | 计算项目内文件路径的基准 | 路径是否指向预期目录，不把操作系统绝对路径写死 |
| `SECRET_KEY` | 参与签名等安全功能 | 不提交真实生产值，通过环境配置提供 |
| `DEBUG` | 控制调试页面等开发行为 | 本地可为 `True`，生产必须关闭 |
| `ALLOWED_HOSTS` | `DEBUG=False` 时允许的 Host | 生产写实际域名，不用 `*` 草率放开 |
| `INSTALLED_APPS` | 启用项目使用的 App | 创建 App 后是否正确注册 |

`employees/apps.py` 中的 `EmployeesConfig` 保存 App 名称及启动配置。使用 `"employees.apps.EmployeesConfig"` 注册，比只写 `"employees"` 更明确。不要把访问数据库、调用外部 API 等重工作随意放进 App 启动过程。

## 十九、启动成功后的四项验证

只看到首页不代表环境完全正确。至少执行：

```powershell
python --version
python -m django --version
python manage.py check
python manage.py runserver
```

`python --version` 不接业务参数，输出当前终端正在使用的Python版本；它用于确认解释器基线。随后用 `python -m django --version` 确认同一解释器中的Django版本，二者应与项目README和依赖要求一致。

然后记录访问 URL、HTTP 状态、终端日志和停止服务器的方法。`runserver` 只用于开发，不是生产应用服务器；第28章会学习生产部署链路。

## 二十、本章总结

- 虚拟环境隔离当前项目依赖
- `.gitignore` 防止本机生成文件进入版本管理
- 依赖文件让其他成员可以重建环境
- Django 项目保存整体配置，App 组织具体业务
- `manage.py` 是开发阶段最常用的项目命令入口
- 本章完成了可启动的项目，第3章将在其中加入第一个页面

## 二十一、日本项目中的实际使用

企业项目强调“任何成员都能重建相同环境”。因此 Python 版本、Django 版本、依赖文件、环境变量和启动命令都要明确。`Project` 保存全站配置，`App` 按业务职责拆分代码，使多人修改不同业务时更容易调查影响范围。

## 二十二、新人常见错误

- 没有激活虚拟环境就安装 Django，导致依赖进入错误的 Python 环境。执行命令前先确认解释器和版本。
- 在错误目录执行 `manage.py`，出现“找不到文件”。先用当前目录和项目结构确认位置。
- 创建 App 后忘记加入 `INSTALLED_APPS`，导致 Model、Admin 等功能未被项目加载。
- 把 `runserver` 当作生产服务器。它只用于本地开发和验证。
- 将真实 `SECRET_KEY` 或本机 `.env` 提交到 Git。仓库只保存变量说明和安全示例。

## 二十三、本章知识将在后续章节继续使用

```text
Project 配置
├─ urls.py → 第3～4章路由
├─ settings.py → 第5章模板与Static、第12章认证、第15章日志
└─ App employees
   ├─ views.py → 第3章以后
   └─ models.py → 第6章以后
```
