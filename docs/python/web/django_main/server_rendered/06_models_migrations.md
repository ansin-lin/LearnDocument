# 第6章 Model 与数据库迁移

## 本章成果

把第5章写死在 Python 列表中的部门和员工，变成真正保存在数据库中的数据。本章结束时，项目中会出现 `Department` 和 `Employee` 两张表。

## 本章开始状态

- 使用第5章完成的 `company_portal` 和 `employees`。
- 员工列表仍来自 `views.py` 中的固定字典，不要提前删除；第8章再替换数据来源。
- 默认 SQLite 配置保持不变，本章只修改 `employees/models.py` 并生成迁移文件。

## 先理解三个角色

| 角色 | 本章中的作用 |
|---|---|
| Model | 用 Python 类描述数据结构 |
| migration | 记录“数据库结构要怎样变化” |
| SQLite | 开发阶段实际保存数据的数据库 |

不要手工创建表。开发者修改 Model，Django 根据迁移文件修改数据库。

### Model 与 Migration 的第一次理解

- **是什么**：Model 是用 Python 类描述业务数据的定义；Migration 是记录数据库结构变化的版本文件。
- **为什么需要**：团队需要让代码中的数据结构与各环境数据库保持一致，并能追踪每次变化。
- **什么时候使用**：新增或修改字段、关联和约束后，先生成并审查 Migration，再更新数据库。

```text
修改 Model
→ makemigrations 生成 Migration
→ 阅读迁移操作
→ migrate 修改 Database
→ check / 页面 / Admin 验证
```

## 定义部门和员工

编辑 `employees/models.py`：

```python
from django.db import models


class Department(models.Model):
    name = models.CharField("部门名", max_length=100, unique=True)

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

这里先掌握四件事：字段决定可保存的数据；`unique=True` 防止编号重复；`ForeignKey` 表示多名员工属于一个部门；`PROTECT` 防止误删仍有员工的部门。

当前字段参数必须能读懂：

| 写法或参数 | 可接受的值与必填性 | 当前作用 |
|---|---|---|
| 字段第一个位置参数 | 人类可读名称，可选 | Admin和Form显示“员工编号”等标签 |
| `max_length` | 正整数；`CharField`必填 | 限制字符串最大长度并影响表结构 |
| `unique` | 布尔值，默认 `False` | `True` 时由数据库和校验共同限制重复值 |
| `ForeignKey(to, on_delete, ...)` | 目标Model和删除策略必填 | 建立多对一关系 |
| `related_name` | 合法属性名，可选 | 允许从部门通过 `employees` 反向取得员工 |
| `blank` | 布尔值，默认 `False` | `True` 时表单允许留空 |
| `default` | 固定值或可调用对象，可选 | 新记录未提供该字段时使用初始值 |

字段实例在Model类定义阶段描述数据库列；真正保存记录后，访问 `employee.name` 等属性得到对应Python值。`Meta.ordering` 是默认排序字段列表，不创建新列。

## 生成并执行迁移

```bash
python manage.py makemigrations
python manage.py migrate
python manage.py showmigrations employees
python manage.py check
```

| 命令 | 参数 | 可接受的值与必填性 | 执行结果 | 什么时候执行 |
|---|---|---|---|---|
| `makemigrations [app_label]` | App标签 | 可选；省略时检查全部App | 根据Model差异生成迁移文件，不修改业务表 | 修改Model后，先生成并审查 |
| `migrate [app_label] [migration_name]` | App、目标迁移 | 均可选；省略时应用全部待执行迁移 | 对当前数据库执行目标范围迁移 | 审查迁移后，在目标环境执行 |
| `showmigrations [app_label]` | App标签 | 可选；省略时显示全部App | 输出迁移清单；`[X]` 表示已应用 | 调查环境状态或发布确认时 |
| `check` | 无 | 本章不传参数 | 输出配置和Model系统检查结果 | 改动后及提交前 |

迁移文件属于源代码，必须提交Git。`makemigrations`成功不代表数据库已改变，必须再执行 `migrate`。

## 读懂结果

看到 `employees/migrations/0001_initial.py` 和带 `[X]` 的迁移记录，表示两张表已经创建。不要为了“重来”随意删除团队项目的迁移文件或数据库。

预期状态类似：

```text
employees
 [X] 0001_initial
System check identified no issues (0 silenced).
```

输出中的迁移编号以自己的项目为准。

## 常见问题

- `No installed app with label 'employees'`：检查 `INSTALLED_APPS`。
- 新增必填字段时要求默认值：先考虑旧数据如何补值，不要盲选临时默认值。
- `no such table`：通常是尚未执行 `migrate`，或运行了错误环境中的数据库。

## 动手任务

1. 为部门增加可为空的 `description` 字段。
2. 生成迁移，先用 `python manage.py sqlmigrate employees 0002` 查看 SQL，再执行迁移。
3. 用 `python manage.py check` 完成自检。

### 现场任务

收到“部门需要增加说明栏”的改修后，先回答：是否允许为空、旧数据如何处理、是否影响 Admin 和页面。提交说明可写：`部署前に employees のマイグレーション実行が必要です。`（部署前需要执行 employees 迁移。）

参考方向见[章节练习参考答案](practice_answers.md)。

`sqlmigrate app_label migration_name` 的两个位置参数都必填，分别是App标签和迁移名称；它把该迁移预计执行的SQL打印到终端，不会直接修改数据库。迁移编号以实际生成文件为准，审查SQL后再执行 `migrate`。

## 数据库运行检查

- [ ] 能说明 Model、迁移文件和数据库的区别
- [ ] 两个 Model 可通过系统检查
- [ ] 知道模型变更的固定顺序：修改、生成迁移、审查、执行、测试

## 现场识读：字段与选项

不需要一次背完所有字段，但要能从定义判断数据库值、表单行为和业务约束。

| 写法 | 主要含义 | 注意点 |
|---|---|---|
| `CharField(max_length=...)` | 有最大长度的字符串 | `max_length` 同时影响校验和表结构 |
| `IntegerField` / `DecimalField` | 整数/精确小数 | 金额优先用 `DecimalField`，不要用浮点数 |
| `BooleanField(default=...)` | 布尔状态 | 默认值要符合业务初始状态 |
| `DateField` / `DateTimeField` | 日期/日期时间 | 注意时区与自动时间选项 |
| `ForeignKey` | 多对一 | 必须确认 `on_delete` 和反向名称 |
| `ManyToManyField` | 多对多 | 通过中间表保存关系 |
| `OneToOneField` | 一对一 | 常用于扩展资料，不等同于继承 |

`blank=True` 主要影响表单校验，`null=True` 主要影响数据库是否允许 `NULL`。字符串字段通常用空字符串表示“未填写”，不要机械地同时写 `null=True, blank=True`。`default` 解决新记录的初始值，不应掩盖旧数据迁移方案。

`Meta` 可设置默认排序、约束、表名等。`verbose_name` 服务于人类可读显示；`db_table`、`db_column` 常在接入既有数据库时出现。`inspectdb` 能根据既有表生成模型草稿，`managed=False` 表示 Django 不管理该表生命周期；这些生成结果必须人工审查，不能直接视为最终设计。

## 迁移是团队交付物

修改 Model 后固定执行“生成 → 阅读 → 执行 → 验证”。使用 `sqlmigrate` 观察将执行的 SQL，使用 `showmigrations` 核对环境状态。生产迁移还要确认锁表时间、旧代码兼容、数据回填、备份和回滚边界。多人同时创建冲突迁移时，应理解两条变更的业务含义后合并，不要随意删除别人已经部署的迁移。

### 结构迁移与数据迁移

`AddField`、`AlterField` 等操作修改表结构；`RunPython` 用于可重复执行的数据转换。数据迁移必须通过 `apps.get_model()` 取得当时版本的历史 Model，不要直接导入当前 `models.py`：

```python
def create_default_department(apps, schema_editor):
    Department = apps.get_model("employees", "Department")
    Department.objects.get_or_create(name="未分配")
```

`get_or_create(defaults=None, **lookups)` 先按查询条件查找唯一对象：存在时返回该对象和 `False`，不存在时用查询条件与可选 `defaults` 创建对象并返回它和 `True`。因此返回值是 `(object, created)` 二元组。并发创建仍应由数据库唯一约束兜底，不能只依赖这次查询。

把函数交给 `migrations.RunPython()` 前，要同时设计反向操作、重复执行影响和数据量。大表回填可能长时间锁表或占用资源，应分批、监控，并与旧代码保持兼容。课程主线不实际创建这条迁移；本节用于识读既有项目中的数据迁移。

`apps.get_model(app_label, model_name)` 的App标签和历史Model名都必填，返回该迁移时点对应的历史Model类；找不到时抛出查找错误。`migrations.RunPython(code, reverse_code=None)` 的正向函数必填，反向函数可选，返回一个迁移操作对象。正向和反向函数都会接收 `apps`、`schema_editor` 两个参数；数据迁移需要可回退时必须提供安全的反向逻辑。

## 本章总结

Model 描述业务数据，迁移文件记录结构变化，数据库保存实际状态。修改模型后必须生成、审查、执行并验证迁移，不能只修改 Python 代码。下一章不急着写业务页面，先使用 Admin 录入可供后续页面查询的初始数据。

## 日本项目中的实际使用

迁移文件与源代码一起 Review 和提交。担当者通常要说明表结构变化、既有数据处理、执行时间、锁表风险和回滚方案。已经在共享环境执行的迁移不能随意删除或改写，否则其他成员的数据库状态会与仓库记录不一致。

## 新人常见错误

- 只修改 Model，不执行 `makemigrations`，数据库结构不会自动变化。
- 生成迁移后不阅读内容，可能把意外字段删除或改成不可逆操作。
- 混淆 `null` 与 `blank`：前者主要影响数据库，后者主要影响表单校验。
- 修改字段后删除别人已经部署的迁移，导致环境历史不一致。

## 本章知识将在后续章节继续使用

```text
Model + Migration + Database
→ 第7章 Admin 录入数据
→ 第8章 ORM 查询
→ 第9～10章表单保存和修改
→ 第14章附件 Model
→ 第17章发布与迁移确认
```
