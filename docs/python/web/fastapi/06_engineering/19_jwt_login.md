# 第19章 JWT签发与登录接口

> 本章成果：验证账号密码，通过OAuth2表单登录接口签发带过期时间的JWT，并能说明登录失败为什么返回`401`。

## 一、登录请求的执行流程

第18章已经准备账号表、密码哈希函数和本地管理员。本章完成登录闭环：

```text
提交OAuth2表单
→ 查询启用账号
→ 验证密码
→ 签发Access Token
→ 返回Bearer Token
```

Token只是后续请求携带的认证信息。本章先证明能够登录并取得Token，第20章再验证Token、取得当前用户并执行角色权限检查。

## 二、安装直接依赖

在项目根目录执行：

```powershell
python -m pip install pyjwt python-multipart
```

| 依赖 | 作用 |
| --- | --- |
| `PyJWT` | 签发和验证JWT |
| `python-multipart` | 解析OAuth2登录表单 |

把两个依赖同步记录到`requirements.txt`。

## 三、签发Access Token

文件：`app/security.py`  
操作：追加  
代码类型：项目代码片段

```python
from datetime import datetime, timedelta, timezone  # 导入UTC时间和时间差工具

import jwt  # 导入PyJWT模块

from app.config import settings  # 导入签名密钥配置


ALGORITHM = "HS256"  # 固定当前项目允许的签名算法
ACCESS_TOKEN_MINUTES = 30  # 设置访问令牌有效分钟数


def create_access_token(  # 定义访问令牌签发函数
    subject: str,  # 接收账号唯一标识
    role_code: str,  # 接收当前角色代码
) -> str:  # 返回编码后的JWT字符串
    expires_at = datetime.now(timezone.utc) + timedelta(  # 计算UTC过期时间
        minutes=ACCESS_TOKEN_MINUTES  # 增加配置的有效分钟数
    )  # 完成过期时间计算
    payload = {  # 组织JWT声明
        "sub": subject,  # 使用sub保存账号标识
        "role": role_code,  # 保存签发时的角色代码
        "exp": expires_at,  # 设置强制过期时间
    }  # 完成声明字典
    return jwt.encode(  # 签名并编码JWT
        payload,  # 传入声明字典
        settings.secret_key,  # 使用服务端密钥签名
        algorithm=ALGORITHM,  # 指定固定算法
    )  # 返回Token字符串
```

`jwt.encode()`本例参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `payload` | 字典 | 必填 | 保存`sub`、`role`、`exp`等Token声明 |
| `key` | 字符串或字节数据 | 必填 | 对Token进行签名的密钥 |
| `algorithm` | PyJWT支持的算法名称字符串 | 默认`"HS256"` | 指定签名算法；编码和解码必须匹配 |
| `headers` | 字典或`None` | 默认`None` | 添加JWT头部字段；本例不需要 |

`sub`标识当前账号，`exp`限制有效期。JWT签名用于发现内容被篡改，不会加密载荷，因此不能把密码或敏感个人数据写入Token。

## 四、认证Service

文件：`app/services/auth_service.py`  
操作：新建  
代码类型：完整文件

```python
from sqlalchemy import select  # 导入账号查询构造函数
from sqlalchemy.orm import Session  # 导入Session类型

from app.models import UserAccount  # 导入账号模型
from app.security import verify_password  # 导入密码验证函数


def authenticate_user(  # 定义登录认证用例
    db: Session,  # 接收请求级Session
    login_id: str,  # 接收登录账号
    password: str,  # 接收本次验证使用的明文密码
) -> UserAccount | None:  # 成功返回账号，失败返回None
    statement = select(UserAccount).where(  # 构建账号查询
        UserAccount.login_id == login_id  # 按唯一登录账号筛选
    )  # 完成查询语句
    user = db.execute(statement).scalar_one_or_none()  # 执行并取得单条结果

    if user is None or not user.is_active:  # 统一处理不存在和禁用账号
        return None  # 不暴露具体失败原因
    if not verify_password(password, user.password_hash):  # 验证明文密码
        return None  # 密码错误时返回统一失败结果
    return user  # 返回通过认证的账号
```

`authenticate_user()`不执行`commit()`，因为登录只读取账号。账号不存在、已禁用和密码错误都返回`None`，避免通过错误消息判断账号是否存在。

## 五、OAuth2登录接口

文件：`app/routers/auth.py`  
操作：新建  
代码类型：完整文件

```python
from typing import Annotated  # 导入表单依赖类型标注工具

from fastapi import APIRouter, Depends, HTTPException  # 导入路由、依赖和HTTP异常
from fastapi.security import OAuth2PasswordRequestForm  # 导入OAuth2密码表单解析类
from sqlalchemy.orm import Session  # 导入Session类型

from app.dependencies import get_db  # 导入数据库Session依赖
from app.security import create_access_token  # 导入Token签发函数
from app.services.auth_service import authenticate_user  # 导入认证用例


router = APIRouter(prefix="/api/auth", tags=["auth"])  # 创建认证子路由


@router.post("/token")  # 注册Token登录接口
def login(  # 定义登录路径函数
    form: Annotated[OAuth2PasswordRequestForm, Depends()],  # 解析OAuth2表单
    db: Session = Depends(get_db),  # 注入请求级Session
):  # 成功时返回Token字典
    user = authenticate_user(db, form.username, form.password)  # 校验账号和密码
    if user is None:  # 认证失败时返回统一响应
        raise HTTPException(  # 中断请求
            status_code=401,  # 返回未认证状态码
            detail="用户名或密码错误",  # 不区分账号或密码错误
            headers={"WWW-Authenticate": "Bearer"},  # 声明Bearer认证方式
        )  # 结束异常对象

    token = create_access_token(user.login_id, user.role_code)  # 为通过认证的账号签发Token
    return {"access_token": token, "token_type": "bearer"}  # 返回OAuth2常用响应字段
```

`OAuth2PasswordRequestForm`接收表单数据，与Swagger UI的Authorize流程一致：

| 表单字段 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `username` | 字符串 | 必填 | 提交登录账号；本项目对应`login_id` |
| `password` | 字符串 | 必填 | 提交明文密码，只用于本次校验 |
| `scope` | 用空格分隔的权限范围字符串 | 默认空字符串 | 提交OAuth2 Scope；本项目暂未使用 |
| `client_id` | 字符串或不提交 | 默认`None` | 标识OAuth2客户端；本项目暂未使用 |
| `client_secret` | 字符串或不提交 | 默认`None` | 提交客户端密钥；本项目暂未使用 |

`HTTPException()`的`headers`接受响应头字典，默认`None`。认证失败时设置`WWW-Authenticate: Bearer`，告诉调用方应提供Bearer Token。

## 六、注册Router

文件：`app/main.py`  
操作：追加导入和注册  
代码类型：项目代码片段

```python
from app.routers.auth import router as auth_router  # 导入认证子路由

app.include_router(auth_router)  # 把登录接口注册到唯一应用对象
```

不要创建第二个`FastAPI()`对象。注册后打开`/docs`，应出现`POST /api/auth/token`。

## 七、运行验证

1. 不设置种子密码或使用不存在账号登录，确认返回`401`。
2. 使用错误密码登录，确认同样返回`401`和统一错误消息。
3. 使用第18章管理员账号登录，确认返回`access_token`和`token_type="bearer"`。
4. 解码Token只观察声明，确认有`sub`、`role`和`exp`；不要把Token写入日志或提交仓库。
5. 把管理员临时设为禁用，确认无法登录，然后恢复数据。

## 八、安全边界

- 生产环境必须使用HTTPS。
- `SECRET_KEY`必须来自秘密管理或环境配置。
- Token必须有过期时间，日志不能记录完整Token。
- 登录接口应结合速率限制和失败监控，不能无限尝试。
- 登录成功不代表任意业务操作都有权限；第20章继续实现授权。

## 九、完成检查

- [ ] 登录端点使用OAuth2表单格式。
- [ ] 登录失败统一返回`401`。
- [ ] 禁用账号和错误密码都不能取得Token。
- [ ] Token具有账号标识、角色和过期时间。
- [ ] 认证Router已注册且没有重复应用对象。

完成后保留登录接口和Token签发函数，第20章将验证Bearer Token并保护员工接口。
