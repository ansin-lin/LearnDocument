# 第9章 Alembic迁移基础

> 本章目标：单独掌握 `Alembic` 这个迁移工具本身，理解它为什么存在、如何初始化、如何生成迁移、如何升级和回退数据库版本。

本章建议按三个检查点完成：先建立独立目录并初始化Alembic；再连接模型元数据并生成、检查迁移脚本；最后执行升级、回退和再次升级。不要在迁移脚本尚未检查时直接进入下一步。

本章使用独立目录`examples/alembic_demo`和独立数据库`alembic_training_db`，不会复用第8章的练习表，也不会占用第10章员工项目的Alembic目录。配置片段分别属于`models.py`、`alembic.ini`、`alembic/env.py`和自动生成的`alembic/versions/<revision>_*.py`。

开始前确认第7章已经设置`ALEMBIC_TRAINING_URL`。在`employee_api`项目根目录执行：

```powershell
New-Item -ItemType Directory -Path examples/alembic_demo  # 创建独立迁移实验目录
Set-Location examples/alembic_demo  # 进入本章命令使用的实验根目录
Get-Location  # 确认当前目录末尾是examples\alembic_demo
```

文件：`examples/alembic_demo/models.py`  
操作：新建文件  
代码类型：完整练习模型

```python
from sqlalchemy import String  # 导入字符串数据库类型
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column  # 导入ORM模型工具


class Base(DeclarativeBase):  # 定义迁移实验使用的ORM基类
    pass  # 基类不增加业务字段


class User(Base):  # 定义迁移实验使用的用户表
    __tablename__ = "alembic_users_demo"  # 使用独立练习表名

    id: Mapped[int] = mapped_column(primary_key=True)  # 定义自增主键
    user_code: Mapped[str] = mapped_column(  # 定义唯一用户编号
        String(20),  # 限制字段长度
        unique=True,  # 增加唯一约束
        nullable=False,  # 禁止空值
    )  # 完成用户编号字段
```

此时只定义模型，不调用`create_all()`。第9章要让Alembic根据模型生成迁移并创建表。

## 一、Alembic 是什么

`Alembic` 是 `SQLAlchemy` 常用的数据库迁移工具。

它的核心作用不是“查数据”，而是管理数据库结构变化。

比如项目开发过程中经常会发生这些事情：

- 新增一个表
- 给表增加字段
- 修改字段长度
- 新增索引
- 删除无用字段

如果没有迁移工具，团队通常会陷入这些问题：

- 每个人本地数据库结构不一致
- 口头通知改表，容易遗漏
- 测试环境、开发环境、生产环境升级顺序混乱

所以企业项目里，`Alembic` 的价值非常明确：

- 记录数据库结构变化
- 按版本管理数据库升级
- 让团队成员使用统一的结构变更方式

## 二、为什么不能只靠手写 SQL 改表

手写 SQL 当然能改表，但真实项目中只靠手写 SQL 会有几个明显问题：

1. 变更历史不好追踪
2. 不同环境容易漏执行
3. 回退困难
4. 团队协作成本高

迁移工具的重点不是“替代 SQL”，而是把结构变更变成可追踪、可执行、可回退的过程。

## 三、安装 Alembic

安装命令：

```bash
python -m pip install alembic
```

确认安装：

```bash
python -m pip show alembic
```

如果前面已经安装过 `sqlalchemy pymysql alembic`，这里主要是确认环境可用。

## 四、初始化 Alembic

在当前`examples/alembic_demo`目录执行：

```bash
alembic init alembic
```

初始化后通常会生成这些内容：

```text
project_root/
├── alembic/
│   ├── env.py
│   ├── script.py.mako
│   └── versions/
└── alembic.ini
```

### 4.1 `alembic init alembic` 做了什么

这条命令的含义是：

- 初始化 Alembic 工作目录
- 生成默认配置文件
- 生成默认迁移脚本模板

参数说明：

| 部分 | 含义 |
| --- | --- |
| `init` | 初始化 Alembic 环境 |
| `alembic` | 生成目录的名称 |

也就是说：

```bash
alembic init alembic
```

会在当前目录下创建一个名为 `alembic` 的迁移目录。

## 五、初始化后几个文件分别做什么

| 文件或目录 | 作用 |
| --- | --- |
| `alembic.ini` | Alembic 基础配置文件 |
| `alembic/env.py` | 迁移运行环境配置 |
| `alembic/versions/` | 保存每次生成的迁移脚本 |
| `script.py.mako` | 迁移脚本模板 |

新人阶段最需要关注的主要是：

- `alembic.ini`
- `alembic/env.py`
- `alembic/versions/`

### 5.1 为什么 `versions/` 目录很重要

这个目录保存的是：

- 每一次数据库结构变化的历史记录

也就是说，团队后来想知道：

- 什么时候新增了某张表
- 什么时候新增了某个字段
- 什么时候删掉了某个索引

通常都要回到这里去看。

## 六、`alembic.ini` 的作用

`alembic.ini` 是 Alembic 的基础配置文件。

`alembic.ini`必须保留`sqlalchemy.url`配置项，但本章不把本地密码写进文件。先保留一个不会实际使用的占位地址：

```ini
sqlalchemy.url = mysql+pymysql://placeholder:placeholder@127.0.0.1:3306/alembic_training_db
```

这里最需要学员先看懂的是：

- `sqlalchemy.url`是Alembic要求的配置项
- 实际连接地址将在`env.py`中由环境变量覆盖

这样既保留标准配置结构，也不会把真实练习密码提交到文件。

### 6.1 `alembic.ini` 里当前阶段最需要关注的是什么

基础阶段先重点关注两件事：

1. Alembic 是否能连接数据库
2. 迁移命令是否能正常执行

也就是说，这一章先不追求把 `alembic.ini` 每一项都讲完，而是先把最关键的迁移流程跑通。

### 6.2 这样做的目的

目的不是把密码硬编码到迁移文件，而是先确认Alembic从哪里取得最终数据库地址。

新人阶段先看懂这件事更重要：

- Alembic 也需要知道连接哪个数据库
- 它要靠这个地址执行迁移

后面进入正式项目时，再把数据库地址改成从项目配置中读取会更合理。

## 七、`env.py` 的作用

`env.py` 可以理解成 Alembic 的运行入口配置文件。

它主要负责：

- 读取数据库连接信息
- 确定目标元数据 `target_metadata`
- 决定如何执行迁移

其中最重要的是 `target_metadata`。

文件：`examples/alembic_demo/alembic/env.py`  
操作：在现有`config = context.config`后追加  
代码类型：迁移配置片段

```python
import os  # 导入环境变量读取工具


config.set_main_option(  # 覆盖alembic.ini中的占位连接地址
    "sqlalchemy.url",  # 指定Alembic数据库地址配置名
    os.environ["ALEMBIC_TRAINING_URL"],  # 读取第7章设置的迁移数据库地址
)  # 完成运行时配置覆盖
```

`config.set_main_option(name, value)`接收配置名和值，两个参数都是字符串且必填，返回`None`。环境变量缺失时命令会立即失败，避免误连其他数据库。

### 7.1 `env.py` 为什么这么关键

因为 Alembic 本身并不知道你的项目里有哪些表。

它只能通过：

- 连接数据库
- 读取 `target_metadata`

去比较：

- 当前数据库结构
- 当前模型定义结构

然后再决定是否生成迁移差异。

## 八、什么是 `target_metadata`

Alembic 自动生成迁移脚本时，必须知道：

- 当前项目有哪些 ORM 模型
- 这些模型对应哪些表结构

这些结构信息最终会落在 `Base.metadata` 上。

所以在 `env.py` 中，最关键的一步是把 `target_metadata` 指向模型元数据。

文件：`examples/alembic_demo/alembic/env.py`  
操作：替换默认`target_metadata = None`并增加模型导入  
代码类型：迁移配置片段

```python
from models import Base, User  # 导入基类并加载User模型


target_metadata = Base.metadata  # 告诉Alembic比较当前练习模型元数据
```

### 8.1 为什么还要导入模型

这也是新人最容易漏掉的地方。

即使你有 `Base`，如果没有让模型类真正被导入，Alembic 也可能不知道当前项目到底有哪些表。

在分包项目中可能会看到类似导入：

```python
from app.models.user import User  # 项目分包时导入模型以注册元数据
```

这行导入的目的，不是为了直接使用 `User` 变量，而是为了让模型定义被加载。

### 8.2 可以把 `target_metadata` 理解成什么

对新人来说，可以先把它理解成：

- “当前项目模型结构的总目录”

Alembic 要靠它知道：

- 项目里有哪些表
- 每张表有哪些字段
- 字段定义是否发生了变化

## 九、生成迁移脚本

常用命令：

```bash
alembic revision --autogenerate -m "create users table"
```

这条命令会做什么：

1. 读取当前模型元数据
2. 对比数据库当前结构
3. 生成一份迁移脚本

生成后的脚本会出现在：

```text
alembic/versions/
```

### 9.1 `revision --autogenerate -m` 各部分是什么意思

| 部分 | 含义 |
| --- | --- |
| `revision` | 创建一个新的迁移版本 |
| `--autogenerate` | 根据模型和数据库差异自动生成脚本 |
| `-m "..."` | 给这次迁移写说明文字 |

说明文字常见写法：

- `create users table`
- `add email column`
- `add employee status index`

### 9.2 这条命令的结果是什么

执行成功后，最直接的结果是：

- `alembic/versions/` 下多一个新的迁移文件

这个文件里通常会包含：

- 版本号
- 升级逻辑
- 回退逻辑

## 十、迁移脚本里最重要的两个函数

迁移脚本中通常会看到：

```python
def upgrade():  # 定义数据库结构向前升级时执行的操作
    ...  # 省略与当前重点无关的实现


def downgrade():  # 定义downgrade函数
    ...  # 省略与当前重点无关的实现
```

| 函数 | 作用 |
| --- | --- |
| `upgrade()` | 执行升级，向前应用变更 |
| `downgrade()` | 执行回退，撤销当前变更 |

### 10.1 为什么要有回退

真实项目中，新版本数据库变更未必永远正确。

如果上线后发现问题，回退能力非常重要。

### 10.2 `upgrade()` 和 `downgrade()` 为什么必须一起看

很多新人只关注升级，不关注回退。

但真实项目里，数据库变更是高风险操作之一。

所以一个迁移脚本至少要看清楚：

- 升级时做了什么
- 回退时能不能恢复

这也是 Review 迁移脚本时的重点之一。

## 十一、执行升级和回退

升级到最新版本：

```bash
alembic upgrade head
```

回退一个版本：

```bash
alembic downgrade -1
```

查看当前数据库版本：

```bash
alembic current
```

查看迁移历史：

```bash
alembic history
```

### 11.1 `upgrade head` 的含义

这里的 `head` 可以理解成：

- 当前迁移链中的最新版本

所以：

```bash
alembic upgrade head
```

的意思就是：

- 一直升级到最新版本

### 11.2 `downgrade -1` 的含义

这里的 `-1` 表示：

- 向后回退一个版本

它适合学习阶段观察：

- 迁移如何回退
- 回退后数据库结构会发生什么变化

## 十二、常用命令表

| 命令 | 作用 |
| --- | --- |
| `alembic init alembic` | 初始化迁移目录 |
| `alembic revision --autogenerate -m "..."` | 自动生成迁移脚本 |
| `alembic upgrade head` | 升级到最新版本 |
| `alembic downgrade -1` | 回退一个版本 |
| `alembic current` | 查看当前版本 |
| `alembic history` | 查看迁移历史 |

## 十三、Alembic 在真实项目中的常见使用场景

最常见的变更包括：

- 新增业务表
- 给现有表增加状态字段
- 给邮箱字段增加唯一约束
- 调整字段长度
- 增加索引提高查询性能

在日本项目里，这类数据库结构变更通常会配合：

- 设计书更新
- 版本管理
- 迁移脚本 Review
- 测试环境先验证

所以迁移脚本不是“随便生成就结束”，而是项目交付的一部分。

### 13.1 自动生成不代表不用看

`--autogenerate`很方便，但生成结果仍然需要人工检查。

你仍然需要检查：

- 是否多生成了不需要的变更
- 是否漏掉了想要的变更
- 字段变化是否符合预期

所以迁移脚本在企业项目里通常也要做 Review。

## 十四、常见错误

### 14.1 改了模型却没有生成迁移

现象：

- 本地模型变了
- 数据库结构没变

原因通常是没有执行：

```bash
alembic revision --autogenerate -m "..."
```

### 14.2 生成了迁移却没有执行升级

生成脚本只是第一步，真正应用到数据库还要执行：

```bash
alembic upgrade head
```

### 14.3 `target_metadata` 没配置对

现象：

- 自动生成时没识别到模型变化

### 14.4 模型没被导入

即使写了 `Base.metadata`，如果模型没有实际导入，也可能导致 Alembic 识别不到表。

## 十五、基础练习

1. 初始化 Alembic 目录。
2. 说明 `alembic.ini`、`env.py`、`versions/` 各自的作用。
3. 写出 `upgrade head` 和 `downgrade -1` 两条命令分别表示什么。

## 十六、综合练习

以一个简单练习模型为例，完成下面流程：

1. 初始化 Alembic
2. 配置数据库地址
3. 配置 `target_metadata`
4. 生成第一版迁移脚本
5. 执行升级
6. 查看当前迁移版本

完成后执行`alembic downgrade base`，确认`alembic_users_demo`被删除，再执行`alembic upgrade head`恢复。最后返回课程项目根目录：

```powershell
Set-Location ../..  # 从examples/alembic_demo返回employee_api项目根目录
```

## 十七、本章总结

| 知识点 | 作用 |
| --- | --- |
| Alembic | 管理数据库结构变更 |
| `alembic.ini` | 迁移基础配置 |
| `env.py` | 迁移运行环境配置 |
| `target_metadata` | 告诉 Alembic 当前模型结构 |
| `revision --autogenerate` | 生成迁移脚本 |
| `upgrade head` | 升级到最新版本 |
| `downgrade -1` | 回退一个版本 |

下一章会把这些数据库工具正式接入员工管理项目，建立部门、员工模型和可重建的迁移。
