# 第8章 配置、日志与异常处理

> 本章目标：掌握 FastAPI 项目中的配置管理、日志输出、HTTP 异常和统一异常处理方式。

## 一、配置、日志和异常的作用

企业项目不能只关注接口成功时的结果，还要关注：

| 内容 | 作用 |
| --- | --- |
| 配置 | 区分本地、测试、生产环境 |
| 日志 | 排查请求、数据库和系统错误 |
| 异常 | 给前端返回明确错误信息 |

## 二、配置管理

文件位置：

```text
app/config.py
```

```python
from pydantic_settings import BaseSettings  # 导入 BaseSettings，用于读取环境变量


class Settings(BaseSettings):  # 定义项目配置类
    app_name: str = "employee-api"  # 应用名称
    database_url: str  # 数据库连接地址
    secret_key: str  # JWT 或签名使用的密钥

    class Config:  # 配置读取规则
        env_file = ".env"  # 从 .env 文件读取配置


settings = Settings()  # 创建配置对象
```

安装：

```powershell
pip install pydantic-settings  # 安装 Pydantic 配置扩展
```

`.env` 示例：

```text
DATABASE_URL=mysql+pymysql://root:password@127.0.0.1:3306/employee_management?charset=utf8mb4
SECRET_KEY=change-me
```

真实项目不能提交真实密码和密钥。

## 三、日志基础

```python
import logging  # 导入 logging 标准库

logger = logging.getLogger(__name__)  # 创建当前模块 logger

logger.info("employee created")  # 输出普通业务日志
logger.warning("employee not found")  # 输出警告日志
logger.exception("unexpected error")  # 输出异常日志和堆栈
```

日志级别：

| 级别 | 说明 |
| --- | --- |
| `DEBUG` | 调试信息 |
| `INFO` | 正常业务信息 |
| `WARNING` | 需要注意但未失败 |
| `ERROR` | 错误 |
| `CRITICAL` | 严重错误 |

## 四、HTTPException

业务错误可以使用 `HTTPException`。

```python
from fastapi import HTTPException  # 导入 HTTPException


def get_employee_or_error(employee):  # 定义员工检查函数
    if employee is None:  # 如果员工不存在
        raise HTTPException(status_code=404, detail="员工不存在")  # 返回 404 错误
    return employee  # 返回员工对象
```

响应示例：

```json
{"detail":"员工不存在"}
```

## 五、统一异常处理

文件位置：

```text
app/main.py
```

```python
import logging  # 导入 logging
from fastapi import FastAPI, Request  # 导入 FastAPI 和 Request
from fastapi.responses import JSONResponse  # 导入 JSONResponse

logger = logging.getLogger(__name__)  # 创建 logger
app = FastAPI()  # 创建应用对象


@app.exception_handler(Exception)  # 注册全局异常处理器
async def handle_unexpected_exception(request: Request, exc: Exception):  # 处理未捕获异常
    logger.exception("unexpected error: %s", request.url.path)  # 记录异常路径和堆栈
    return JSONResponse(status_code=500, content={"detail": "系统错误"})  # 返回统一错误响应
```

注意：不要把真实异常堆栈直接返回给前端。

## 六、日本项目中的关注点

| 场景 | 关注点 |
| --- | --- |
| 障害対応 | 日志中能否定位请求和错误 |
| レビュー | 是否泄露敏感信息 |
| 環境差異 | 配置是否区分环境 |
| 単体試験 | 异常时响应是否稳定 |

## 七、基础练习

请完成：

1. 创建 `config.py`
2. 使用 `.env` 保存数据库连接
3. 在新增员工时记录 `info` 日志
4. 员工不存在时返回 `404`
5. 注册全局异常处理器

## 八、本章总结

- 配置用于管理环境差异
- 日志用于排查问题和留下运行证据
- `HTTPException` 用于明确业务错误
- 全局异常处理不要泄露内部错误细节
