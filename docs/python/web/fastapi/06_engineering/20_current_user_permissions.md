# 第20章 当前用户与角色权限

> 本章成果：验证Bearer Token、重新查询账号状态，并用当前用户与角色依赖保护员工和部门接口，准确区分`401`与`403`。

## 一、认证与授权的边界

第19章完成登录和Token签发。本章处理后续受保护请求：

```text
读取Bearer Token
→ 验证签名和过期时间
→ 查询当前账号
→ 检查账号启用状态
→ 检查操作所需角色
→ 执行业务接口
```

- Token缺失、无效、过期，或账号已禁用：返回`401`。
- 账号有效但角色不足：返回`403`。
- CORS和前端隐藏按钮都不能代替后端权限检查。

## 二、创建当前用户依赖

文件：`app/dependencies.py`  
操作：追加  
代码类型：项目代码片段

```python
from typing import Annotated  # 导入当前用户依赖类型标注工具

import jwt  # 导入PyJWT解码功能
from fastapi import Depends, HTTPException  # 导入依赖和HTTP异常工具
from fastapi.security import OAuth2PasswordBearer  # 导入Bearer Token读取工具
from jwt.exceptions import InvalidTokenError  # 导入无效Token异常
from sqlalchemy import select  # 导入账号查询构造函数

from app.config import settings  # 导入签名密钥
from app.models import UserAccount  # 导入账号模型
from app.security import ALGORITHM  # 导入允许的固定算法


oauth2_scheme = OAuth2PasswordBearer(  # 创建Bearer认证方案
    tokenUrl="/api/auth/token"  # 指向第19章登录端点
)  # 完成认证方案配置


def get_current_user(  # 定义当前用户依赖
    token: Annotated[str, Depends(oauth2_scheme)],  # 从Authorization头读取Token
    db: Session = Depends(get_db),  # 注入请求级Session
) -> UserAccount:  # 返回仍然有效的账号对象
    credentials_error = HTTPException(  # 准备统一认证失败响应
        status_code=401,  # 返回未认证状态码
        detail="认证信息无效",  # 不暴露具体校验步骤
        headers={"WWW-Authenticate": "Bearer"},  # 声明Bearer认证方式
    )  # 完成异常对象

    try:  # 捕获所有Token校验失败
        payload = jwt.decode(  # 验证签名、算法和时间声明
            token,  # 传入请求中的Token
            settings.secret_key,  # 使用服务端签名密钥
            algorithms=[ALGORITHM],  # 只允许项目固定算法
        )  # 返回解码后的声明字典
        login_id = payload.get("sub")  # 读取账号标识
        if not isinstance(login_id, str):  # 确认sub存在且类型正确
            raise credentials_error  # 缺失或错误时返回统一401
    except InvalidTokenError:  # 捕获篡改、过期和格式错误
        raise credentials_error  # 转换为稳定HTTP响应

    user = db.execute(  # 根据Token中的账号标识重新查询数据库
        select(UserAccount).where(UserAccount.login_id == login_id)  # 按唯一账号筛选
    ).scalar_one_or_none()  # 取得一条账号或None
    if user is None or not user.is_active:  # 检查账号仍存在且启用
        raise credentials_error  # 状态失效时返回401
    return user  # 把当前账号注入上层依赖或路径函数
```

`OAuth2PasswordBearer()`参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `tokenUrl` | 登录端点URL字符串 | 必填 | 告诉OpenAPI和Swagger UI从哪个端点取得Token |
| `scheme_name` | 字符串或`None` | 默认`None` | 设置OpenAPI中的安全方案名称 |
| `scopes` | `dict[str, str]`或`None` | 默认`None` | 声明Scope名称及说明 |
| `description` | 字符串或`None` | 默认`None` | 设置安全方案说明 |
| `auto_error` | `True`或`False` | 默认`True` | 缺少Authorization请求头时是否自动返回认证错误 |

`OAuth2PasswordBearer`对象被`Depends()`调用后，从`Authorization: Bearer <token>`读取Token字符串。`tokenUrl`用于生成文档和Authorize流程，不会自动实现登录端点。

`jwt.decode()`本例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `jwt` | JWT字符串或字节数据 | 必填 | 提交需要验证和解析的Token |
| `key` | 字符串或字节数据 | 必填 | 使用签发Token时对应的密钥验证签名 |
| `algorithms` | 允许的算法名称列表 | 必填 | 限定可接受的签名算法，不能信任Token自带算法 |
| `audience` | 字符串、字符串列表或`None` | 默认`None` | 需要校验`aud`声明时指定目标接收方 |
| `issuer` | 字符串、字符串列表或`None` | 默认`None` | 需要校验`iss`声明时指定签发方 |
| `leeway` | 秒数或`timedelta` | 默认`0` | 为时间声明允许少量时钟误差 |

每次请求重新查询账号，才能及时拒绝已删除或禁用的账号。Token中的角色可以辅助展示，但权限判断使用数据库中的当前`role_code`。

## 三、创建角色权限依赖

文件：`app/dependencies.py`  
操作：继续追加  
代码类型：项目代码片段

```python
EDITOR_ROLES = {"SYSTEM_ADMIN", "HR_STAFF"}  # 定义允许新增和修改员工的角色集合


def require_editor(  # 定义编辑权限依赖
    current_user: Annotated[UserAccount, Depends(get_current_user)],  # 注入有效当前账号
) -> UserAccount:  # 成功时返回当前账号
    if current_user.role_code not in EDITOR_ROLES:  # 检查编辑角色集合
        raise HTTPException(status_code=403, detail="没有权限")  # 账号有效但权限不足
    return current_user  # 返回通过授权的账号


def require_admin(  # 定义管理员权限依赖
    current_user: Annotated[UserAccount, Depends(get_current_user)],  # 注入有效当前账号
) -> UserAccount:  # 成功时返回管理员账号
    if current_user.role_code != "SYSTEM_ADMIN":  # 只允许系统管理员
        raise HTTPException(status_code=403, detail="没有权限")  # 角色不足返回403
    return current_user  # 返回通过授权的账号
```

角色与操作对应关系：

| 操作 | 依赖 | 可接受的角色 |
| --- | --- | --- |
| 员工列表、详情 | `get_current_user` | 任意有效账号 |
| 员工新增、修改 | `require_editor` | `SYSTEM_ADMIN`、`HR_STAFF` |
| 员工逻辑删除 | `require_admin` | `SYSTEM_ADMIN` |
| 部门列表 | `get_current_user` | 任意有效账号 |

## 四、保护员工接口

文件：`app/routers/employees.py`  
操作：给第14章五个路径装饰器追加对应`dependencies`参数  
代码类型：装饰器修改对照

```text
@router.get(
    "",
    response_model=EmployeeListResponse,
    dependencies=[Depends(get_current_user)],
)

@router.get(
    "/{employee_number}",
    response_model=EmployeeResponse,
    dependencies=[Depends(get_current_user)],
)

@router.post(
    "",
    response_model=EmployeeResponse,
    status_code=201,
    dependencies=[Depends(require_editor)],
)

@router.put(
    "/{employee_number}",
    response_model=EmployeeResponse,
    dependencies=[Depends(require_editor)],
)

@router.delete(
    "/{employee_number}",
    status_code=204,
    dependencies=[Depends(require_admin)],
)
```

路径函数体保持不变。`dependencies`中的依赖会在进入函数前执行；这里仅要求检查成功，不需要在函数体中读取当前账号。

文件：`app/routers/departments.py`  
操作：给部门列表装饰器追加依赖  
代码类型：装饰器修改片段

```text
@router.get(
    "",
    response_model=list[DepartmentSummary],
    dependencies=[Depends(get_current_user)],
)
```

确保两个Router都导入`Depends`和实际使用的权限依赖，删除未使用导入。

## 五、验证认证与授权

按顺序验证：

1. 不带Token访问员工列表，确认`401`。
2. 使用过期、格式错误或篡改Token，确认`401`。
3. 正确登录后在`/docs`点击Authorize，再查询列表和详情。
4. 使用`VIEWER`执行新增、修改或删除，确认`403`且数据库不变。
5. 使用`HR_STAFF`新增和修改成功，删除返回`403`。
6. 使用`SYSTEM_ADMIN`逻辑删除成功，返回`204`。
7. 登录后禁用账号，再使用原Token请求，确认返回`401`。

## 六、安全边界

- 生产环境必须使用HTTPS。
- 日志不能记录完整Token、密码或密码哈希。
- JWT签名不等于加密，不能存放秘密数据。
- 修改密码、禁用账号和权限变更需要明确Token失效策略。
- 前端可见性只改善体验，所有权限规则必须在后端执行。

## 七、完成检查

- [ ] 当前用户依赖验证签名、过期时间和固定算法。
- [ ] 每次请求重新检查账号状态和当前角色。
- [ ] 未认证场景返回`401`。
- [ ] 已认证但权限不足返回`403`。
- [ ] 员工和部门接口都有符合规格的后端权限保护。

完成后保留登录、当前用户和角色依赖，第21～23章会用自动测试固定成功、`401`和`403`行为。
