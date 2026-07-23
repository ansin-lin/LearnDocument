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

## 生成并执行迁移

```bash
python manage.py makemigrations
python manage.py migrate
python manage.py showmigrations employees
python manage.py check
```

`makemigrations` 生成变更记录，`migrate` 执行变更。迁移文件属于源代码，必须提交 Git。

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

## 完成检查

- [ ] 能说明 Model、迁移文件和数据库的区别
- [ ] 两个 Model 可通过系统检查
- [ ] 知道模型变更的固定顺序：修改、生成迁移、审查、执行、测试

下一章不急着写业务页面，先使用 Admin 安全地录入初始数据。

## 字段与选项的现场读法

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
