# 第21章 pytest、TestClient与隔离数据库

> 本章成果：创建不会污染开发数据库的FastAPI测试环境，使用Fixture、依赖覆盖和`TestClient`完成第一个可重复接口测试。

## 一、测试边界

本章先建立所有接口测试共用的基础设施：

```text
创建测试数据库
→ 准备固定账号和部门
→ 覆盖get_db依赖
→ 创建TestClient
→ 执行请求和断言
→ 清理覆盖与数据库
```

测试不能连接`employee_management_fastapi`开发数据库，否则测试可能覆盖开发数据。SQLite内存数据库用于快速接口测试；第23章还会在可丢弃MySQL测试库验证迁移。

## 二、安装依赖

在项目根目录执行：

```powershell
python -m pip install pytest httpx
```

把`pytest`和`httpx`记录到`requirements-dev.txt`。FastAPI的`TestClient`基于Starlette，并使用`httpx`发送测试请求。

## 三、创建隔离测试数据库

文件：`tests/conftest.py`  
操作：新建  
代码类型：完整测试配置文件

```python
import os  # 导入测试环境变量工具

import pytest  # 导入测试框架
from fastapi.testclient import TestClient  # 导入同步接口测试客户端
from sqlalchemy import create_engine  # 导入测试数据库引擎创建函数
from sqlalchemy.orm import Session, sessionmaker  # 导入Session类型和工厂
from sqlalchemy.pool import StaticPool  # 导入单连接内存数据库连接池

os.environ.setdefault("DATABASE_URL", "sqlite://")  # 在导入应用前设置测试数据库地址
os.environ.setdefault(  # 在导入应用前设置测试专用签名密钥
    "SECRET_KEY",  # 指定环境变量名称
    "test-only-secret-key-at-least-32-bytes-long",  # 使用仅供测试的固定值
)  # 完成测试密钥设置

from app.database import Base  # 环境变量准备后再导入ORM基类
from app.dependencies import get_db  # 导入需要覆盖的生产Session依赖
from app.main import app  # 导入唯一FastAPI应用
from app.models import Department, UserAccount  # 导入测试初始数据模型
from app.security import hash_password  # 导入真实密码哈希函数


test_engine = create_engine(  # 创建SQLite内存测试引擎
    "sqlite://",  # 使用内存数据库
    connect_args={"check_same_thread": False},  # 允许TestClient线程访问同一连接
    poolclass=StaticPool,  # 在测试期间复用同一个内存数据库连接
)  # 完成测试引擎配置
TestSessionLocal = sessionmaker(  # 创建测试Session工厂
    bind=test_engine,  # 绑定测试引擎
    autoflush=False,  # 保持与项目Session配置一致
    expire_on_commit=False,  # 提交后仍可读取对象属性
)  # 完成Session工厂配置


@pytest.fixture  # 默认每个测试函数重新执行Fixture
def db_session():  # 准备测试表、数据和Session
    Base.metadata.create_all(bind=test_engine)  # 为当前测试创建全部表
    with TestSessionLocal() as session:  # 打开测试Session并保证关闭
        session.add(Department(id=1, name="开发部"))  # 准备开发部
        session.add(Department(id=2, name="营业部"))  # 准备营业部
        session.add(  # 准备管理员账号
            UserAccount(  # 创建账号对象
                login_id="admin",  # 设置登录账号
                password_hash=hash_password("test-password"),  # 保存测试密码哈希
                role_code="SYSTEM_ADMIN",  # 设置管理员角色
            )  # 完成管理员对象
        )  # 加入Session
        session.add(  # 准备只读账号
            UserAccount(  # 创建账号对象
                login_id="viewer",  # 设置登录账号
                password_hash=hash_password("viewer-password"),  # 保存测试密码哈希
                role_code="VIEWER",  # 设置只读角色
            )  # 完成只读账号
        )  # 加入Session
        session.add(  # 准备人事账号
            UserAccount(  # 创建账号对象
                login_id="hr_staff",  # 设置登录账号
                password_hash=hash_password("hr-password"),  # 保存测试密码哈希
                role_code="HR_STAFF",  # 设置人事角色
            )  # 完成人事账号对象
        )  # 加入Session
        session.commit()  # 提交固定初始数据
        yield session  # 把Session交给测试函数
        session.rollback()  # 测试异常退出时撤销未提交事务
    Base.metadata.drop_all(bind=test_engine)  # 测试结束后删除全部表


@pytest.fixture  # 为每个测试创建客户端
def client(db_session: Session):  # 接收当前测试Session
    def override_get_db():  # 定义生产依赖的测试替代函数
        yield db_session  # 始终返回当前测试Session

    app.dependency_overrides[get_db] = override_get_db  # 覆盖生产Session依赖
    with TestClient(app) as test_client:  # 进入应用Lifespan并创建客户端
        yield test_client  # 把客户端交给测试函数
    app.dependency_overrides.clear()  # 测试结束后清除所有覆盖


@pytest.fixture  # 创建管理员认证头Fixture
def admin_headers(client: TestClient) -> dict[str, str]:  # 使用真实登录端点取得Token
    response = client.post(  # 提交OAuth2登录表单
        "/api/auth/token",  # 指定登录路径
        data={"username": "admin", "password": "test-password"},  # 提交测试账号密码
    )  # 得到登录响应
    token = response.json()["access_token"]  # 读取访问令牌
    return {"Authorization": f"Bearer {token}"}  # 返回可复用认证请求头


@pytest.fixture  # 创建只读账号认证头Fixture
def viewer_headers(client: TestClient) -> dict[str, str]:  # 使用真实登录端点取得Token
    response = client.post(  # 提交OAuth2登录表单
        "/api/auth/token",  # 指定登录路径
        data={"username": "viewer", "password": "viewer-password"},  # 提交只读账号密码
    )  # 得到登录响应
    token = response.json()["access_token"]  # 读取访问令牌
    return {"Authorization": f"Bearer {token}"}  # 返回可复用认证请求头


@pytest.fixture  # 创建人事账号认证头Fixture
def hr_staff_headers(client: TestClient) -> dict[str, str]:  # 使用真实登录端点取得Token
    response = client.post(  # 提交OAuth2登录表单
        "/api/auth/token",  # 指定登录路径
        data={"username": "hr_staff", "password": "hr-password"},  # 提交人事账号密码
    )  # 得到登录响应
    token = response.json()["access_token"]  # 读取访问令牌
    return {"Authorization": f"Bearer {token}"}  # 返回可复用认证请求头
```

`@pytest.fixture`常用参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `scope` | `"function"`、`"class"`、`"module"`、`"package"`、`"session"` | 默认`"function"` | 决定Fixture在多大范围内复用 |
| `autouse` | `True`或`False` | 默认`False` | 是否让匹配范围内的测试自动使用Fixture |
| `params` | 可迭代对象或`None` | 默认`None` | 使用多组参数重复执行依赖该Fixture的测试 |
| `ids` | 字符串列表、生成函数或`None` | 默认`None` | 设置参数化用例在测试结果中的名称 |
| `name` | 字符串或`None` | 默认`None` | 给Fixture指定不同于函数名的公开名称 |

本例使用不带参数的`@pytest.fixture`，因此每个测试函数获得一套新数据和依赖覆盖。

`TestClient()`本例常用参数：

| 参数 | 可接受的值 | 默认值或必填性 | 作用 |
| --- | --- | --- | --- |
| `app` | ASGI应用对象 | 必填 | 指定要测试的FastAPI应用 |
| `base_url` | URL字符串 | 默认`"http://testserver"` | 为测试请求设置基础地址 |
| `raise_server_exceptions` | `True`或`False` | 默认`True` | 服务端异常是否直接在测试中抛出 |
| `root_path` | 路径字符串 | 默认`""` | 模拟应用部署在反向代理子路径下 |
| `headers` | 请求头字典或`None` | 默认包含测试客户端User-Agent | 给全部请求设置默认请求头 |
| `follow_redirects` | `True`或`False` | 默认`True` | 是否自动跟随重定向 |

`app.dependency_overrides`是以原依赖函数为键、替代函数为值的字典。测试结束后必须调用`clear()`，避免测试Session影响后续用例。

## 四、编写第一个接口测试

文件：`tests/test_health.py`  
操作：新建  
代码类型：完整测试文件

```python
def test_health_check(client):  # 验证健康检查接口
    response = client.get("/health")  # 向应用发送GET请求

    assert response.status_code == 200  # 断言成功状态码
    assert response.json() == {"status": "ok"}  # 断言完整JSON响应
```

`client.get(url, **kwargs)`的`url`接受路径或URL字符串且必填；常用关键字参数包括`params`、`headers`和`cookies`，本例只传路径。返回的响应对象通过`status_code`读取状态码，通过`json()`解析JSON正文。`assert`条件为假时，pytest把测试标记为失败并显示实际值。

在项目根目录执行：

```powershell
pytest -q
```

`-q`是pytest的简洁输出选项。连续执行两次，结果都应通过，且开发数据库内容不发生变化。

## 五、完成检查

- [ ] 测试数据库与开发数据库隔离。
- [ ] 每个测试获得固定初始数据。
- [ ] `get_db`覆盖在测试后清理。
- [ ] `TestClient`进入应用Lifespan。
- [ ] 管理员、人事和只读账号Fixture都通过真实登录端点取得Token。
- [ ] 健康检查测试可以连续运行两次。

完成后保留`conftest.py`和健康检查测试，第22章将使用同一环境验证员工CRUD和数据库状态。
