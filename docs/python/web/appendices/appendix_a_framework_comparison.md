# 第34章 FastAPI、Flask 与 Django 对比

> 使用三套员工管理实现比较框架职责和选型依据。

## 一、框架对比

| 维度 | FastAPI | Flask | Django |
| --- | --- | --- | --- |
| 主要定位 | API 开发 | 轻量 Web 与自由组装 | 完整 Web 平台 |
| 数据校验 | Pydantic | 自选库或手动实现 | Form、Serializer |
| ORM | 课程使用 SQLAlchemy | 常用 Flask-SQLAlchemy | 内置 Django ORM |
| 管理后台 | 无内置 | 无内置 | 内置 Admin |
| 工程约束 | 中等 | 较少 | 较强 |

## 二、选型时确认

- 项目主要是 API、服务端页面还是后台管理
- 团队现有技术和维护经验
- 是否需要框架内置认证、ORM 和 Admin
- 部署环境、性能目标和长期维护要求
- 既有系统和公司开发规范

## 三、综合练习

针对“企业内部员工管理系统”和“对外 API”分别编写框架选型说明，并说明不选择其他框架的理由。
