# 第6章 APIRouter 与依赖注入

> 本章目标：掌握 APIRouter 拆分接口、Depends 复用公共逻辑、数据库 Session 依赖和基础资源生命周期。

## 一、为什么需要拆分路由

接口少时可以都写在 `main.py`。接口多了以后，所有代码放在一个文件会很难维护。

推荐按业务拆分：

```text
app/
├── main.py
├── database.py
├── models.py
└── routers/
    └── employees.py
```

## 二、创建 Router

文件位置：

```text
app/routers/employees.py
```

```python
from fastapi import APIRouter  # 导入 APIRouter，用于拆分路由

router = APIRouter(prefix="/employees", tags=["employees"])  # 创建员工路由对象


@router.get("")  # 注册 GET /employees 接口
def list_employees():  # 员工列表接口
    return [{"id": 1, "name": "Tanaka"}]  # 返回员工列表
```

参数说明：

| 参数 | 作用 |
| --- | --- |
| `prefix` | 给当前路由组统一追加路径前缀 |
| `tags` | 在自动文档中分组显示 |

## 三、注册 Router

文件位置：

```text
app/main.py
```

```python
from fastapi import FastAPI  # 导入 FastAPI
from app.routers import employees  # 导入员工路由模块

app = FastAPI()  # 创建应用对象
app.include_router(employees.router)  # 注册员工路由
```

访问：

```text
GET /employees
```

## 四、Depends 是什么

`Depends` 用于声明“当前接口执行前需要准备什么”。

最小示例：

```python
from fastapi import Depends, FastAPI  # 导入 Depends 和 FastAPI

app = FastAPI()  # 创建应用对象


def get_current_system():  # 定义依赖函数
    return "employee-management"  # 返回系统名称


@app.get("/system")  # 注册系统接口
def read_system(system_name: str = Depends(get_current_system)):  # 使用 Depends 调用依赖函数
    return {"system_name": system_name}  # 返回依赖函数结果
```

执行流程：

```text
请求 /system
-> 执行 get_current_system()
-> 把返回值传给 system_name
-> 执行 read_system()
```

## 五、数据库 Session 依赖

文件位置：

```text
app/dependencies.py
```

```python
from app.database import SessionLocal  # 导入 Session 工厂


def get_db():  # 定义数据库 Session 依赖
    db = SessionLocal()  # 创建数据库 Session
    try:  # 开始资源管理
        yield db  # 把 Session 提供给路由函数使用
    finally:  # 请求结束后执行
        db.close()  # 关闭 Session
```

`yield` 的作用：

| 阶段 | 说明 |
| --- | --- |
| `yield` 前 | 创建资源 |
| `yield db` | 把资源交给接口函数 |
| `finally` | 请求结束后释放资源 |

## 六、在接口中使用数据库依赖

文件位置：

```text
app/routers/employees.py
```

```python
from fastapi import APIRouter, Depends  # 导入 APIRouter 和 Depends
from sqlalchemy import select  # 导入 select，用于查询
from sqlalchemy.orm import Session  # 导入 Session 类型
from app.dependencies import get_db  # 导入数据库依赖
from app.models import Employee  # 导入员工模型

router = APIRouter(prefix="/employees", tags=["employees"])  # 创建员工路由


@router.get("")  # 注册员工列表接口
def list_employees(db: Session = Depends(get_db)):  # 通过依赖注入取得数据库 Session
    statement = select(Employee).where(Employee.is_active == True)  # 构建查询在职员工语句
    employees = db.execute(statement).scalars().all()  # 执行查询并取得员工对象列表
    return employees  # 返回员工列表
```

## 七、常见错误

| 错误 | 原因 | 处理方式 |
| --- | --- | --- |
| Router 接口不显示 | 没有 `include_router()` | 检查 `main.py` |
| 路径重复 | `prefix` 和装饰器路径重复写 | 统一规划路径 |
| Session 未关闭 | 没有 `finally: db.close()` | 使用 `yield` 依赖 |
| Depends 写成调用结果 | 写成 `Depends(get_db())` | 应写 `Depends(get_db)` |

## 八、基础练习

请完成：

1. 创建 `app/routers/employees.py`
2. 使用 `APIRouter` 定义员工路由
3. 在 `main.py` 注册路由
4. 创建 `get_db()` 依赖
5. 在员工列表接口中使用数据库 Session

## 九、本章总结

- `APIRouter` 用于拆分接口模块
- `include_router()` 用于注册路由
- `Depends` 用于声明依赖
- 数据库 Session 适合用 `yield` 依赖管理
- Router 和依赖注入是 FastAPI 项目结构化的基础
