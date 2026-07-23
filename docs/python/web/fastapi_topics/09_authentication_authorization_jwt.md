# 第9章 登录认证、授权与 JWT

> 本章目标：理解认证和授权的区别，掌握密码哈希、登录接口、JWT 生成、当前用户获取和基础权限判断。

## 一、认证和授权

| 内容 | 解决的问题 |
| --- | --- |
| 认证 | 当前用户是谁 |
| 授权 | 当前用户能做什么 |

API 项目中常见流程：

```text
用户提交用户名和密码
-> 后端校验密码
-> 返回 JWT
-> 前端后续请求携带 JWT
-> 后端解析 JWT 得到当前用户
-> 判断权限
```

## 二、安装依赖

```powershell
pip install python-jose passlib[bcrypt] python-multipart  # 安装 JWT、密码哈希和表单依赖
```

## 三、密码哈希

不能把明文密码保存到数据库。

```python
from passlib.context import CryptContext  # 导入密码上下文

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")  # 创建 bcrypt 密码哈希上下文


def hash_password(password: str) -> str:  # 定义密码哈希函数
    return pwd_context.hash(password)  # 返回哈希后的密码


def verify_password(plain_password: str, hashed_password: str) -> bool:  # 定义密码校验函数
    return pwd_context.verify(plain_password, hashed_password)  # 校验明文密码和哈希密码是否匹配
```

## 四、生成 JWT

```python
from datetime import datetime, timedelta, timezone  # 导入时间处理工具
from jose import jwt  # 导入 JWT 工具

SECRET_KEY = "change-me"  # JWT 密钥，正式项目必须来自环境变量
ALGORITHM = "HS256"  # JWT 签名算法


def create_access_token(data: dict, expires_minutes: int = 30):  # 定义 Token 生成函数
    to_encode = data.copy()  # 复制原始数据，避免修改传入对象
    expire = datetime.now(timezone.utc) + timedelta(minutes=expires_minutes)  # 计算过期时间
    to_encode.update({"exp": expire})  # 写入过期时间
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)  # 生成 JWT 字符串
```

## 五、登录接口

```python
from fastapi import APIRouter, HTTPException  # 导入路由和异常
from pydantic import BaseModel  # 导入 Pydantic 模型

router = APIRouter(prefix="/auth", tags=["auth"])  # 创建认证路由


class LoginRequest(BaseModel):  # 登录请求模型
    username: str  # 用户名
    password: str  # 密码


@router.post("/login")  # 注册登录接口
def login(request: LoginRequest):  # 接收登录请求
    if request.username != "admin" or request.password != "password":  # 示例校验，真实项目应查数据库
        raise HTTPException(status_code=401, detail="用户名或密码错误")  # 登录失败返回 401

    token = create_access_token({"sub": request.username})  # 生成访问 Token
    return {"access_token": token, "token_type": "bearer"}  # 返回 Token
```

## 六、获取当前用户

```python
from fastapi import Depends, HTTPException  # 导入 Depends 和 HTTPException
from fastapi.security import OAuth2PasswordBearer  # 导入 Bearer Token 解析工具
from jose import JWTError, jwt  # 导入 JWT 错误和解析工具

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")  # 定义 Token 获取地址


def get_current_user(token: str = Depends(oauth2_scheme)):  # 从请求头中取得 Bearer Token
    try:  # 开始解析 Token
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])  # 解码 JWT
        username = payload.get("sub")  # 获取用户名
        if username is None:  # 如果 Token 中没有用户名
            raise HTTPException(status_code=401, detail="Token 无效")  # 返回认证失败
        return {"username": username}  # 返回当前用户信息
    except JWTError:  # Token 解析失败
        raise HTTPException(status_code=401, detail="Token 无效")  # 返回认证失败
```

## 七、保护接口

```python
@router.get("/me")  # 当前用户接口
def read_me(current_user: dict = Depends(get_current_user)):  # 依赖当前用户
    return current_user  # 返回当前用户信息
```

请求时需要 Header：

```text
Authorization: Bearer <access_token>
```

## 八、权限判断

```python
def require_admin(current_user: dict = Depends(get_current_user)):  # 定义管理员权限依赖
    if current_user["username"] != "admin":  # 判断是否不是管理员
        raise HTTPException(status_code=403, detail="没有权限")  # 返回无权限
    return current_user  # 返回当前用户
```

使用：

```python
@router.delete("/employees/{employee_id}")  # 删除员工接口
def delete_employee(employee_id: int, current_user: dict = Depends(require_admin)):  # 要求管理员权限
    return {"message": "deleted", "employee_id": employee_id}  # 返回删除结果
```

## 九、安全注意点

- 密码必须哈希保存
- 密钥不能写死在代码中
- Token 要设置过期时间
- CORS 不是认证或授权
- 前端保存 Token 时要考虑 XSS 风险
- 生产环境必须使用 HTTPS

## 十、基础练习

请完成：

1. 密码哈希函数
2. 登录接口
3. JWT 生成
4. 当前用户依赖
5. 管理员权限依赖

## 十一、本章总结

- 认证解决“是谁”
- 授权解决“能做什么”
- JWT 常用于前后台分离 API
- `OAuth2PasswordBearer` 可以从请求头读取 Bearer Token
- 权限判断适合封装成依赖
