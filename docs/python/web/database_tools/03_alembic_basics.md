# Alembic 迁移基础

> 本章目标：单独掌握 `Alembic` 这个迁移工具本身，理解它为什么存在、如何初始化、如何生成迁移、如何升级和回退数据库版本。

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

在项目根目录执行：

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

在最简单的练习阶段，可以先直接写数据库地址：

```ini
sqlalchemy.url = mysql+pymysql://root:root123@127.0.0.1:3306/training_db?charset=utf8mb4
```

这里最需要学员先看懂的是：

- `sqlalchemy.url` 表示 Alembic 连接数据库时使用的地址

如果这个地址写错了，后面的迁移命令通常会直接失败。

### 6.1 `alembic.ini` 里当前阶段最需要关注的是什么

基础阶段先重点关注两件事：

1. Alembic 是否能连接数据库
2. 迁移命令是否能正常执行

也就是说，这一章先不追求把 `alembic.ini` 每一项都讲完，而是先把最关键的迁移流程跑通。

### 6.1 这样做的目的

目的不是说企业项目一定要硬编码在这里，而是先把迁移工具跑通。

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

示例：

```python
from sqlalchemy.orm import DeclarativeBase  # 导入声明式基类


class Base(DeclarativeBase):  # 定义 ORM 基类
    pass


target_metadata = Base.metadata  # 告诉 Alembic 当前要比较的元数据对象
```

### 8.1 为什么还要导入模型

这也是新人最容易漏掉的地方。

即使你有 `Base`，如果没有让模型类真正被导入，Alembic 也可能不知道当前项目到底有哪些表。

例如：

```python
from app.models.user import User  # 导入模型，确保模型注册到 Base.metadata
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
def upgrade():
    ...


def downgrade():
    ...
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

`--autogenerate`` 很方便，但它不是说生成完就一定完全正确。

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

下一章开始，才正式把这些数据库工具放进员工管理项目结构中使用。
