# 第16章 FastAPI 项目验收

> 本章目标：整合前面章节内容，完成一个员工管理 API 项目的运行、测试和交付验收。

## 一、项目范围

本项目实现员工管理 API。

功能范围：

| 功能 | 接口 |
| --- | --- |
| 健康检查 | `GET /health` |
| 登录 | `POST /auth/login` |
| 员工列表 | `GET /employees` |
| 员工详情 | `GET /employees/{employee_id}` |
| 新增员工 | `POST /employees` |
| 修改员工 | `PUT /employees/{employee_id}` |
| 设置离职 | `DELETE /employees/{employee_id}` |
| CSV 导出 | `GET /files/employees.csv` |

## 二、最终目录结构

```text
app/
├── main.py
├── config.py
├── database.py
├── dependencies.py
├── models.py
├── schemas/
│   └── employee.py
├── repositories/
│   └── employee_repository.py
├── services/
│   └── employee_service.py
└── routers/
    ├── auth.py
    ├── employees.py
    └── files.py
tests/
└── test_employees.py
requirements.txt
```

## 三、启动前确认

| 检查项 | 说明 |
| --- | --- |
| 依赖安装 | `pip install -r requirements.txt` |
| 数据库存在 | MySQL 中存在 `employee_management` |
| 表结构存在 | 已创建部门和员工表 |
| 环境变量 | `.env` 中有数据库地址和密钥 |
| 路由注册 | `main.py` 注册 auth、employees、files |

## 四、运行项目

```powershell
uvicorn app.main:app --reload  # 开发环境启动项目
```

访问：

```text
http://127.0.0.1:8000/docs
```

## 五、验收顺序

```text
访问 /health
-> 登录取得 Token
-> 使用 Token 查询员工列表
-> 新增员工
-> 查询员工详情
-> 修改员工
-> 设置员工离职
-> 导出 CSV
-> 执行 pytest
```

## 六、接口验收表

| 接口 | 验收内容 |
| --- | --- |
| `GET /health` | 返回 `{"status":"ok"}` |
| `POST /auth/login` | 正确账号返回 Token |
| `GET /employees` | 返回员工列表 |
| `POST /employees` | 新增成功返回 201 |
| `PUT /employees/{id}` | 修改后返回新数据 |
| `DELETE /employees/{id}` | 员工状态变为离职 |
| `GET /files/employees.csv` | 可以下载 CSV |

## 七、测试验收

执行：

```powershell
pytest  # 运行自动化测试
```

应至少覆盖：

- 健康检查
- 登录成功
- 登录失败
- 查询员工
- 新增员工
- 参数校验失败
- 未登录访问受保护接口

## 八、日本项目交付材料

交付时应能说明：

| 材料 | 内容 |
| --- | --- |
| 接口一覧 | 接口路径、方法、参数、响应 |
| 単体試験結果 | 测试用例和执行结果 |
| 環境設定 | 环境变量和启动方式 |
| DB 定義 | 表结构和主要字段 |
| 障害時確認 | 日志位置和常见错误处理 |

## 九、综合练习

请完成最终项目：

1. 所有接口可以在 `/docs` 中测试
2. 数据保存到 MySQL
3. 登录后才能访问员工管理接口
4. 员工编号重复时返回业务错误
5. 列表接口支持关键字和分页
6. CSV 导出可以被 Excel 打开
7. pytest 测试通过

## 十、本章总结

- FastAPI 项目最终要能运行、能测试、能验收
- 项目结构应保持 router、schema、model、repository、service 分层清晰
- 数据库、认证、异常、日志、测试和部署都属于接口交付的一部分
- 自动文档有帮助，但不能代替测试和验收
