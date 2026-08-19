# 第18章 用户账号与密码哈希

> 本章成果：创建用户账号表和迁移，使用Argon2保存密码哈希，并通过可重复执行的种子脚本准备本地管理员账号。

## 一、认证数据从哪里开始

FastAPI提供请求解析和依赖工具，但不会替项目决定账号表、密码算法和角色字段。本章先完成认证的数据基础：

```text
账号表
→ 密码哈希
→ 本地种子账号
```

接口安全要求：

- 数据库不保存明文密码。
- 日志和响应不输出密码或密码哈希。
- 账号禁用状态保存在数据库中，后续认证时必须检查。

## 二、安装直接依赖

在项目根目录执行：

```powershell
python -m pip install "pwdlib[argon2]"
```

| 依赖 | 作用 |
| --- | --- |
| `pwdlib[argon2]` | 使用推荐设置哈希和验证密码 |

把依赖同步记录到`requirements.txt`。JWT和登录表单依赖将在第19章首次使用时安装。

## 三、增加用户账号模型

文件：`app/models.py`  
操作：追加  
代码类型：项目代码片段

```python
class UserAccount(Base):  # 定义登录账号ORM模型
    __tablename__ = "user_accounts"  # 指定数据库表名

    id: Mapped[int] = mapped_column(Integer, primary_key=True)  # 定义自增主键
    login_id: Mapped[str] = mapped_column(  # 定义唯一登录账号
        String(100),  # 限制数据库字段长度
        unique=True,  # 增加唯一约束
        nullable=False,  # 禁止空值
    )  # 结束登录账号字段配置
    password_hash: Mapped[str] = mapped_column(  # 只保存密码哈希
        String(255),  # 为算法信息和哈希结果预留长度
        nullable=False,  # 禁止空值
    )  # 结束密码哈希字段配置
    role_code: Mapped[str] = mapped_column(  # 保存权限规则使用的角色代码
        String(30),  # 限制角色代码长度
        nullable=False,  # 每个账号必须有角色
    )  # 结束角色字段配置
    is_active: Mapped[bool] = mapped_column(  # 保存账号是否可登录
        Boolean,  # 使用布尔字段
        nullable=False,  # 禁止空值
        default=True,  # 新对象默认启用
    )  # 结束状态字段配置
```

`UserAccount`字段必须与[项目规格](../project_spec.md)一致：`login_id`唯一，`role_code`使用`VIEWER`、`HR_STAFF`或`SYSTEM_ADMIN`，`is_active=False`的账号不能登录。

生成、检查并执行迁移：

```powershell
alembic revision --autogenerate -m "add user accounts"
alembic upgrade head
alembic current
```

自动生成后先打开迁移脚本，确认只新增`user_accounts`表及预期约束。不要直接在数据库工具中临时建表，否则迁移历史不能重建环境。

## 四、密码哈希

文件：`app/security.py`  
操作：新建  
代码类型：完整文件

```python
from pwdlib import PasswordHash  # 导入密码哈希工具


password_hash = PasswordHash.recommended()  # 创建采用推荐算法参数的哈希对象


def hash_password(password: str) -> str:  # 定义明文密码哈希函数
    return password_hash.hash(password)  # 返回包含算法和参数信息的哈希字符串


def verify_password(plain_password: str, hashed_password: str) -> bool:  # 定义密码验证函数
    return password_hash.verify(plain_password, hashed_password)  # 返回密码是否匹配
```

`PasswordHash`本例方法：

| 方法 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `recommended()` | 不接收参数 | 无参数 | 创建采用当前推荐密码哈希设置的对象 |
| `hash(password)` | 明文密码字符串或字节数据 | `password`必填 | 生成带算法和参数信息的密码哈希 |
| `verify(password, hash)` | 明文密码与已有哈希 | 两个参数都必填 | 验证明文密码是否与已有哈希匹配 |

数据库只保存`password_hash`。同一个明文密码多次哈希通常会得到不同字符串，这是随机盐的正常结果；验证时必须调用`verify()`，不能比较两个哈希字符串是否相等。

在项目根目录执行独立验证：

```powershell
python -c "from app.security import hash_password; print(hash_password('local-test-password'))"
```

不要把示例明文密码用于生产环境，也不要把终端输出的哈希提交到教程仓库。

## 五、准备初始管理员账号

文件：`app/config.py`  
操作：向现有`Settings`追加字段  
代码类型：配置代码片段

```python
class Settings(BaseSettings):  # 在现有配置模型中保留其他字段
    # 保留已有配置
    seed_admin_password: str | None = None  # 可选读取本地种子管理员密码
```

文件：`.env.example`  
操作：追加  
代码类型：配置片段

```text
SEED_ADMIN_PASSWORD=replace-with-a-local-seed-password
```

文件：`app/seed.py`  
操作：在`SessionLocal.begin()`代码块末尾追加  
代码类型：项目代码片段

```python
from sqlalchemy import select  # 导入账号查询构造函数

from app.config import settings  # 导入种子密码配置
from app.models import UserAccount  # 导入账号模型
from app.security import hash_password  # 导入密码哈希函数


if settings.seed_admin_password:  # 只在明确设置本地种子密码时创建账号
    admin = session.execute(  # 查询管理员是否已经存在
        select(UserAccount).where(UserAccount.login_id == "admin")  # 按唯一登录账号筛选
    ).scalar_one_or_none()  # 返回一条账号或None
    if admin is None:  # 避免重复运行时重复插入
        session.add(  # 把新账号加入当前事务
            UserAccount(  # 创建管理员模型对象
                login_id="admin",  # 设置本地管理员登录账号
                password_hash=hash_password(  # 在写库前生成密码哈希
                    settings.seed_admin_password  # 使用环境配置中的明文密码
                ),  # 完成哈希
                role_code="SYSTEM_ADMIN",  # 赋予系统管理员角色
            )  # 完成账号对象
        )  # 加入Session
```

这段代码与部门、员工种子数据使用同一个事务。没有设置`SEED_ADMIN_PASSWORD`时不创建账号；设置后重复运行`python -m app.seed`也不会重复插入管理员。

| 配置 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `SEED_ADMIN_PASSWORD` | 非空字符串，或不设置 | 默认`None` | 仅在受控环境首次创建本地管理员 |

## 六、运行验证

1. 执行迁移，确认`user_accounts`表、唯一约束和非空约束存在。
2. 设置本地`SEED_ADMIN_PASSWORD`后执行`python -m app.seed`。
3. 再执行一次种子脚本，确认没有重复管理员。
4. 查询数据库，确认保存的是哈希而不是明文密码。
5. 临时把账号设为`is_active=False`，记录状态后恢复；第19章会验证禁用账号无法登录。

## 七、完成检查

- [ ] 账号字段与项目规格一致。
- [ ] 数据库只保存密码哈希。
- [ ] 迁移可以从空数据库重建账号表。
- [ ] 种子脚本可以重复执行。
- [ ] 密码和哈希都没有写入日志或响应。

完成后保留账号表、密码工具和本地管理员，第19章将在此基础上实现登录和Token签发。
