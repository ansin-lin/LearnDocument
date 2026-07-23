# 第15章 Linux、Docker 与部署基础

> 本章目标：理解 FastAPI 项目从本地运行到服务器部署的基本流程，掌握依赖安装、环境变量、启动命令、Docker 基础和健康检查。

## 一、部署要解决什么

本地运行只说明开发环境可用。部署要让项目在服务器上稳定运行。

部署流程：

```text
准备服务器
-> 安装 Python 和依赖
-> 配置环境变量
-> 启动应用服务
-> 配置反向代理
-> 检查日志
-> 验证接口
```

## 二、依赖文件

文件位置：

```text
requirements.txt
```

```text
fastapi
uvicorn
sqlalchemy
pymysql
pydantic-settings
python-jose
passlib[bcrypt]
python-multipart
httpx
pytest
```

安装：

```powershell
pip install -r requirements.txt  # 根据依赖文件安装项目依赖
```

## 三、环境变量

`.env` 示例：

```text
DATABASE_URL=mysql+pymysql://user:password@db:3306/employee_management?charset=utf8mb4
SECRET_KEY=change-me
```

真实项目不要提交真实 `.env`。

## 四、生产启动命令

开发环境：

```powershell
uvicorn app.main:app --reload  # 开发环境启动，支持自动重载
```

服务器环境：

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 8000  # 服务器启动，监听所有网卡
```

`--reload` 不适合生产环境。

## 五、Dockerfile

文件位置：

```text
Dockerfile
```

```dockerfile
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

## 六、健康检查

接口：

```python
@app.get("/health")  # 健康检查接口
def health_check():  # 定义健康检查函数
    return {"status": "ok"}  # 返回服务状态
```

验证：

```powershell
curl http://127.0.0.1:8000/health  # 访问健康检查接口
```

预期：

```json
{"status":"ok"}
```

## 七、部署关注点

| 内容 | 说明 |
| --- | --- |
| 配置 | 环境变量和密钥管理 |
| 数据库 | 连接地址、账号权限、迁移 |
| 日志 | 错误日志和访问日志 |
| 健康检查 | 判断服务是否可用 |
| 回滚 | 发布失败时恢复旧版本 |
| 备份 | 数据库变更前备份 |

## 八、基础练习

请完成：

1. 创建 `requirements.txt`
2. 使用环境变量配置数据库
3. 添加 `/health`
4. 使用非 `--reload` 命令启动
5. 编写 Dockerfile

## 九、本章总结

- 部署关注运行环境、配置、进程、数据库、日志和验证
- 生产环境不要使用 `--reload`
- 密钥和密码不能写入代码
- 健康检查不能只看进程，要确认接口可响应
