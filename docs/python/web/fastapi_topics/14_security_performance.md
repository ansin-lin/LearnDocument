# 第14章 安全与性能基础

> 本章目标：理解 FastAPI 项目中常见安全风险和基础性能问题，掌握参数校验、认证授权、分页、超时和日志边界。

## 一、安全不是最后才做

接口开发中常见风险：

| 风险 | 说明 |
| --- | --- |
| 参数未校验 | 脏数据进入系统 |
| SQL 注入 | 字符串拼接 SQL |
| 密码明文保存 | 数据泄露后风险极高 |
| Token 泄露 | 用户身份被冒用 |
| 权限缺失 | 普通用户访问管理接口 |
| 文件上传无限制 | 上传恶意文件或超大文件 |

## 二、参数校验

使用 Pydantic 和类型提示进行基础校验。

```python
from pydantic import BaseModel, Field  # 导入模型和字段约束


class EmployeeCreate(BaseModel):  # 新增员工请求模型
    employee_code: str = Field(min_length=1, max_length=20)  # 限制员工编号长度
    name: str = Field(min_length=1, max_length=100)  # 限制姓名长度
```

## 三、SQL 安全

推荐使用 ORM 或参数化查询，不拼接用户输入。

不推荐：

```python
sql = f"select * from employees where name = '{keyword}'"  # 不推荐，存在 SQL 注入风险
```

推荐：

```python
statement = select(Employee).where(Employee.name.contains(keyword))  # 推荐，使用 SQLAlchemy 构建查询
```

## 四、分页

列表接口不能一次返回无限数据。

```python
@router.get("/employees")  # 员工列表接口
def list_employees(page: int = 1, size: int = 20):  # 接收页码和每页数量
    offset = (page - 1) * size  # 计算偏移量
    return {"page": page, "size": size, "offset": offset}  # 返回分页信息
```

建议限制 `size` 最大值，避免一次查询过多数据。

## 五、外部请求超时

```python
with httpx.Client(timeout=5.0) as client:  # 设置 5 秒超时
    response = client.get("https://example.com/api")  # 调用外部 API
```

外部请求必须设置超时和异常处理。

## 六、日志安全

日志不能输出：

- 密码
- Token
- 个人敏感信息
- 数据库真实密码
- 内部密钥

可以输出：

- 请求路径
- 状态码
- 耗时
- 业务 ID
- 错误摘要

## 七、性能基础

常见性能关注点：

| 问题 | 处理方向 |
| --- | --- |
| 列表数据太多 | 分页 |
| 重复查询 | 关联查询或减少循环查询 |
| 外部接口慢 | 超时、缓存、异步任务 |
| 文件太大 | 限制大小、流式处理 |
| 日志过多 | 控制日志级别 |

## 八、基础练习

请完成：

1. 给列表接口增加分页参数
2. 限制每页最大 100 条
3. 确认外部 API 调用有超时
4. 检查日志中是否输出 Token
5. 说明 CORS 为什么不是授权

## 九、本章总结

- 安全要贯穿接口开发全过程
- 参数校验可以减少脏数据
- 不拼接用户输入生成 SQL
- 列表接口必须考虑分页
- 外部请求必须设置超时
- 日志不能泄露敏感信息
