# 开始：从零创建 Django 项目

## 完成结果

从空目录创建独立的 Python 虚拟环境、Django 项目和 `employees` App，连接本地 SQLite 数据库，建立部门与员工表，并通过 Django Admin 写入第一组练习数据。完成后得到一个可以继续开发 REST API 的员工管理后端。

开始前需要能够使用 PowerShell 进入目录，并掌握 Python 类、模块、异常以及表、主键、外键等基础知识。这里不要求已有 Django 项目，也不要求学过 Template、Form 或服务端渲染。

## 1. 项目准备涉及哪些组件

一个 Django 后端不只是几段 Python 代码。它还需要独立的运行环境、项目配置、业务模块、数据模型和数据库结构。各组件的关系如下：

```text
Python
  ↓ 创建隔离环境
虚拟环境（venv）
  ↓ 安装并记录依赖
Django + requirements.txt
  ↓ 创建代码结构
Project（全局配置） + App（业务功能）
  ↓ 定义和转换数据结构
Model → Migration → Database
  ↓ 管理和读取数据
Admin / ORM
```

| 组件 | 主要职责 | 本项目中的位置或结果 |
|---|---|---|
| 虚拟环境 | 隔离不同项目的 Python 依赖 | `.venv/` |
| 依赖清单 | 固定可重复安装的直接依赖 | `requirements.txt` |
| Project | 保存整个 Django 服务的配置和入口 | `company_portal/` |
| App | 按业务职责组织功能代码 | `employees/` |
| Model | 使用 Python 类描述业务数据和关系 | `employees/models.py` |
| Migration | 记录并执行数据库结构变化 | `employees/migrations/` |
| ORM | 使用 Python 对象查询和修改数据库 | `Employee.objects...` |
| Admin | 提供开发和运营管理用的数据后台 | `/admin/` |
| Git | 保存可比较、可回退的代码基线 | `.git/` |

接下来先分别理解这些知识，再从空目录完成一次完整实践。

## 2. 虚拟环境与依赖管理

### 2.1 虚拟环境

| 问题 | 说明 |
|---|---|
| 是什么（What） | 虚拟环境是一个项目专用的 Python 运行环境，拥有独立的解释器入口和第三方库目录。 |
| 为什么需要（Why） | 不同项目可能依赖不同版本的 Django；隔离后，安装或升级一个项目的库不会破坏其他项目。 |
| 什么时候使用（When） | 每个 Python 项目开始开发前都应创建并激活自己的虚拟环境。 |

Python 标准库中的 `venv` 模块负责创建虚拟环境。本项目把环境放在 `.venv/`，这个目录只属于当前计算机，不应提交到 Git。激活环境后，后续使用的 `python` 和 `pip` 都指向这个环境。

### 2.2 pip 与 requirements.txt

`pip` 是 Python 的包管理器，用于安装 Django 等第三方库。使用 `python -m pip` 时，Python 会以模块方式运行 `pip`，可以明确保证安装目标就是当前 `python` 所属的环境。

`requirements.txt` 是依赖清单。它记录项目需要的库及版本，使其他开发人员、测试环境和部署环境能够安装相同依赖。虚拟环境解决“依赖放在哪里”，依赖清单解决“需要安装什么版本”。

```text
requirements.txt
  ↓ python -m pip install -r requirements.txt
虚拟环境中的第三方库
```

## 3. Django Project、App 与配置

### 3.1 Project 与 App

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| Project | 整个 Django 服务的配置和启动入口 | 集中管理 settings、根 URL、WSGI/ASGI 和运行环境 | 创建一个新的 Django 服务时 |
| App | 按业务职责组织 Model、迁移、Admin、View 和测试的 Python 包 | 让业务代码与项目级配置分开，并让 Django 能按 App 管理迁移、权限和组件发现 | 一组功能具有明确业务职责时 |

本项目的 Project 名为 `company_portal`，负责全局配置；App 名为 `employees`，负责部门与员工业务。二者不是两个独立服务，而是同一个 Django 后端中的不同职责层次。

员工管理包含部门、员工、后续 API、权限和测试，因此这些代码放在同一个 `employees` App 中。如果直接写入 `company_portal`，全局配置和业务实现会混在一起，项目扩展后难以判断修改范围。

App 也不等于数据库表。部门与员工虽然对应两张表，但属于同一业务范围，因此不需要分别创建 App。只有出现边界清楚的另一组业务，例如账号管理或请假审批，并且拥有自己的模型、路由和权限时，才考虑建立新的 App。

### 3.2 生成文件的职责

| 文件 | 职责 |
|---|---|
| `manage.py` | Django 项目的命令入口，用于检查、迁移、启动开发服务器等操作 |
| `company_portal/settings.py` | 保存已启用 App、数据库、时区、中间件等全局设置 |
| `company_portal/urls.py` | 保存整个服务的根 URL 入口 |
| `employees/apps.py` | 定义 `employees` App 的配置类 |
| `employees/models.py` | 定义部门、员工等业务数据模型 |
| `employees/admin.py` | 配置哪些 Model 可以在 Django Admin 中管理 |

创建 App 后还需要把它加入 `INSTALLED_APPS`。该列表是 Django 已启用组件的清单；未注册的 App 虽然文件存在，但 Django 不会按项目组件加载它的 Model、迁移和 Admin 配置。

## 4. Model、Migration、ORM 与数据库

### 4.1 四者的关系

Model、Migration、ORM 和数据库分别解决不同问题：

| 概念 | 是什么（What） | 为什么需要（Why） | 什么时候使用（When） |
|---|---|---|---|
| Model | 使用 Python 类描述业务数据、字段、约束和表之间关系 | 让业务结构能够由代码表达并被 Django 识别 | 新建或修改业务数据结构时 |
| Migration | 可追踪的数据库结构变更记录 | 让开发、测试和生产环境按相同顺序改变表结构 | Model 发生需要更新数据库的变化后 |
| ORM | 在 Python 对象与关系数据库之间进行转换的组件 | 用统一的 Python API 完成常见查询和写入，减少手写 SQL | View、命令、测试或 Shell 读写业务数据时 |
| Database | 实际保存表、约束和业务数据的系统 | 让数据在程序停止后仍能持久保存 | 应用运行期间持续使用 |

从 Model 到数据库结构的过程是：

```text
employees/models.py
  ↓ makemigrations：比较 Model 并生成变更记录
employees/migrations/0001_initial.py
  ↓ migrate：执行尚未应用的变更
SQLite 中的部门表和员工表
```

程序读写数据时走另一条路径：

```text
Python 代码
  ↓ ORM 查询
Model 对象
  ↓ Django 数据库后端
SQLite
```

REST API 以后也会复用同一套 Model、ORM 和数据库连接。前端只通过 HTTP 调用 API，不会直接连接数据库，因此不需要为“前后端分离”另外建立一条数据库连接。

### 4.2 SQLite 与 DATABASES

SQLite 把数据库保存在一个本地文件中，不需要单独安装数据库服务器，适合当前的开发练习。Django 通过 `settings.py` 中的 `DATABASES` 读取连接配置：`ENGINE` 选择数据库后端，`NAME` 指定数据库文件或数据库名称。

企业项目可能把 `ENGINE` 和连接参数改为 PostgreSQL、MySQL 等配置，但 Model、Migration 和 ORM 的基本使用方式不变。连接真实共享数据库前，还必须确认环境、账号权限、秘密管理和备份要求。

### 4.3 Django Admin

Django Admin 是 Django 提供的管理后台。Model 注册到 Admin 后，可以通过浏览器新增、查询和修改数据，适合准备开发数据和执行受控的运营管理。

Admin 不是提供给前端应用的 REST API，也不代替业务页面。这里使用它写入第一组数据，是为了先确认 Model、Migration、数据库连接和数据读写都已正常工作。

## 5. 从空目录完成项目准备

下面按照刚才建立的组件关系，从空目录完成项目。除特别说明外，所有命令都在 `django_employee_api/` 项目根目录执行。

### 5.1 创建目录和虚拟环境

选择练习代码的保存位置，在 PowerShell 中执行：

```powershell
python --version
mkdir django_employee_api
cd django_employee_api
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
```

本项目使用 Python 3.10 以上版本；版本过低时先升级 Python，再创建虚拟环境。激活成功后，PowerShell 提示符通常会出现 `(.venv)`。

`mkdir django_employee_api` 在当前位置创建项目目录，`cd django_employee_api` 把后续命令的工作目录切换进去。`python -m venv .venv` 调用标准库 `venv`，唯一的位置参数是虚拟环境目录，本例会新建 `.venv/` 并写入独立的 Python 入口和包目录。`Activate.ps1` 修改当前 PowerShell 进程中的路径，使后续 `python` 和 `pip` 指向该环境；它不安装依赖，关闭终端后也不会保持激活状态。

`python -m pip install --upgrade pip` 在当前虚拟环境中安装或升级 `pip`；`--upgrade` 表示已安装时也检查并更新到符合要求的较新版本。该命令会改变当前虚拟环境中的包，不会升级 Python 本身。

如果 PowerShell 阻止激活脚本，只为当前窗口临时放行后再激活：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

`-Scope Process` 把设置范围限制在当前 PowerShell 进程，关闭窗口后失效；`-ExecutionPolicy Bypass` 让这个进程不阻止脚本执行，也不会永久修改系统策略。该命令降低了当前窗口的脚本检查强度，只应在确认激活脚本来自刚创建的 `.venv` 后使用，不要为了运行练习而修改整台计算机的全局执行策略。

### 5.2 安装 Django 并记录依赖

在已激活的虚拟环境中执行：

```powershell
python -m pip install "Django==5.2.17"
python -m django --version
```

`python -m django --version` 通过 Django 模块显示已安装版本，结果应为 `5.2.17`。这里固定版本，使不同环境得到相同结果；以后升级时应先检查发布说明并运行全部测试。

在项目根目录创建 `requirements.txt`：

```text
Django==5.2.17
```

新环境可在项目根目录执行 `python -m pip install -r requirements.txt`，不需要逐个回忆安装命令。`-r`（`--requirement`）后面必须提供依赖文件路径，表示逐项读取该文件中的安装要求；命令会把依赖安装到当前 Python 所属环境。

### 5.3 创建 Project 和 employees App

在项目根目录执行：

```powershell
django-admin startproject company_portal .
python manage.py startapp employees
```

`django-admin` 是安装 Django 后提供的管理命令；`startproject` 创建 Project。命令末尾的点表示在当前目录生成 `manage.py`，避免再增加一层同名目录。

`python manage.py startapp employees` 通过当前项目的命令入口调用 `startapp`，生成 App 的标准目录和基础文件。此时主要目录如下：

```text
django_employee_api/
├── .venv/
├── company_portal/
│   ├── settings.py
│   └── urls.py
├── employees/
│   ├── admin.py
│   ├── apps.py
│   ├── models.py
│   └── migrations/
├── manage.py
└── requirements.txt
```

### 5.4 注册 App 并确认数据库配置

打开 `company_portal/settings.py`，在既有 `INSTALLED_APPS` 列表末尾加入 `employees.apps.EmployeesConfig`：

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

`EmployeesConfig` 是 `employees/apps.py` 中由 `startapp` 生成的 App 配置类。使用完整路径可以明确告诉 Django 应加载哪个配置类。

同一文件中已经生成以下 SQLite 配置，无需重复添加：

```python
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.sqlite3",
        "NAME": BASE_DIR / "db.sqlite3",
    }
}
```

`default` 是默认连接别名；`django.db.backends.sqlite3` 是 Django 内置的 SQLite 数据库后端；`BASE_DIR / "db.sqlite3"` 表示数据库文件位于项目根目录。

### 5.5 建立部门和员工 Model

将 `employees/models.py` 修改为：

```python
from django.db import models


class Department(models.Model):
    name = models.CharField("部门名", max_length=100, unique=True)
    description = models.TextField("说明", blank=True)

    def __str__(self) -> str:
        return self.name


class Employee(models.Model):
    employee_number = models.CharField("员工编号", max_length=20, unique=True)
    name = models.CharField("姓名", max_length=100)
    department = models.ForeignKey(
        Department,
        on_delete=models.PROTECT,
        related_name="employees",
        verbose_name="部门",
    )
    email = models.EmailField("邮箱", blank=True)
    joined_on = models.DateField("入职日期")
    is_active = models.BooleanField("在职", default=True)

    class Meta:
        ordering = ["employee_number"]

    def __str__(self) -> str:
        return f"{self.employee_number} {self.name}"
```

`django.db.models` 模块提供 Model 基类和各种字段类。`CharField` 保存限定长度的文本，`TextField` 保存较长文本，`EmailField` 增加邮箱格式校验，`DateField` 保存日期，`BooleanField` 保存真假状态。字段构造器返回供 Django 建表、校验和生成表单使用的字段定义，不会立即写入数据库。

字段的第一个位置参数或 `verbose_name=...` 接受说明文字，用作界面和管理后台中的字段名称；`max_length` 接受正整数，是 `CharField` 的必填最大字符数，并参与数据库字段和输入校验；`unique` 和 `blank` 接受布尔值，`unique=True` 建立唯一约束，`blank=True` 允许 Django 校验时留空，两者默认值都是 `False`；`default=True` 表示创建对象时没有提供 `is_active` 就使用 `True`。数据库是否允许 `NULL` 由另一个布尔参数 `null` 控制，本项目没有设置，因此保持默认的 `False`。

`ForeignKey` 表示多个员工可以属于同一部门。`on_delete=models.PROTECT` 阻止删除仍被员工引用的部门；`related_name="employees"` 允许从部门反向访问员工。`Meta.ordering` 规定没有显式指定排序时按员工编号返回。`__str__()` 定义对象转换为字符串时的显示内容，Admin 将使用它显示部门和员工。

### 5.6 生成并执行迁移

在项目根目录执行：

```powershell
python manage.py check
python manage.py makemigrations employees
python manage.py migrate
python manage.py showmigrations employees
```

`check` 检查项目配置；`makemigrations employees` 根据 `employees` App 的 Model 生成迁移文件；`migrate` 把所有尚未执行的迁移应用到当前数据库；`showmigrations employees` 显示该 App 的迁移状态。看到 `[X] 0001_initial` 表示部门表和员工表已经创建。

本地首次执行后会出现 `db.sqlite3`。以后修改 Model 时应生成新的迁移，不要直接修改已经在其他环境执行过的迁移文件。对共享、测试或生产数据库执行 `migrate` 前，还必须确认连接目标、变更内容和备份要求。

### 5.7 准备管理账号和练习数据

在项目根目录执行：

```powershell
python manage.py createsuperuser
```

`createsuperuser` 交互式创建具有 Django Admin 登录权限的管理账号。练习密码只用于本机开发，不要复用真实系统密码，也不要把密码写入文档或提交到 Git。

将 `employees/admin.py` 修改为：

```python
from django.contrib import admin

from .models import Department, Employee


@admin.register(Department)
class DepartmentAdmin(admin.ModelAdmin):
    search_fields = ["name"]


@admin.register(Employee)
class EmployeeAdmin(admin.ModelAdmin):
    list_display = [
        "employee_number",
        "name",
        "department",
        "joined_on",
        "is_active",
    ]
    list_filter = ["department", "is_active"]
    search_fields = ["employee_number", "name", "email"]
```

`django.contrib.admin` 模块提供 Admin 功能。`admin.register()` 装饰器把 Model 注册到管理后台；`admin.ModelAdmin` 是后台页面的配置基类。`list_display` 决定列表列，`list_filter` 增加筛选项，`search_fields` 指定可搜索字段。这些设置只影响管理后台，不会生成 REST API。

启动开发服务器：

```powershell
python manage.py runserver
```

`runserver` 启动 Django 自带的本地开发服务器。浏览器访问 `http://127.0.0.1:8000/admin/`，使用刚创建的账号登录，然后依次建立：

1. 部门：开发部。
2. 员工：编号 `E001`、姓名 `山田太郎`、部门“开发部”、入职日期和在职状态。

终端出现异常时先停止操作并阅读最后一段错误。`runserver` 只用于本地开发，不能作为生产部署方式。

### 5.8 建立 Git 基线

在项目根目录创建 `.gitignore`：

```gitignore
.venv/
__pycache__/
*.py[cod]
db.sqlite3
media/
staticfiles/
.env
```

`.gitignore` 防止虚拟环境、本地数据库、上传文件、收集后的 Static 和秘密配置进入版本管理。它不会自动移除已经被 Git 跟踪的文件，因此必须在第一次暂存前确认内容。迁移文件属于数据库结构历史，必须提交，不能忽略 `employees/migrations/`。

已经安装 Git 时，在项目根目录执行：

```powershell
git init
git add .
git status
git commit -m "Initialize Django employee API"
```

`git init` 在当前目录创建本地仓库；`git add .` 暂存未被 `.gitignore` 排除的项目文件；`git status` 用于提交前确认没有密码、本地数据库和无关文件；`git commit -m "消息"` 用当前暂存内容建立提交，`-m` 后必须提供本次提交说明。本例建立后续改修可以比较和回退的基线。

若 Git 提示身份未配置，只为当前练习仓库设置姓名和测试邮箱：

```powershell
git config user.name "Training User"
git config user.email "training@example.test"
git commit -m "Initialize Django employee API"
```

这里不要求创建远程仓库。第29章需要推送时，再使用团队提供或自己创建的练习仓库地址配置 `origin`。

`git config <key> <value>` 写入 Git 配置；没有 `--global` 时只修改当前仓库的 `.git/config`。本例的 `user.name` 和 `user.email` 是提交作者信息，命令成功时没有普通文本结果，可用 `git config --get user.name` 和 `git config --get user.email` 验证。不要把练习身份写成真实项目中的他人身份。

### 5.9 完成检查

停止服务器后，在项目根目录执行：

```powershell
python manage.py check
python manage.py showmigrations employees
python manage.py shell -c "from employees.models import Employee; print(Employee.objects.count())"
```

最后一条命令通过 Django Shell 和 ORM 读取员工数量，不会修改数据。`Employee.objects` 是 Model 的默认管理器，`count()` 执行计数查询；结果应至少为 `1`。

- [ ] `.venv` 已创建并激活。
- [ ] Django 版本为 5.2.17。
- [ ] `company_portal` Project 和 `employees` App 均已创建。
- [ ] 员工迁移显示 `[X]`。
- [ ] Admin 可以登录并读取第一条员工数据。
- [ ] `.gitignore` 已排除本地环境、数据库、Media 和秘密文件。
- [ ] Git 已建立不包含敏感数据的本地基线提交。

现在已经得到一个包含业务模型、数据库结构和练习数据的 Django 后端。下一步进入第19章，在这个项目上实现第一个 JSON 接口。
