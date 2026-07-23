# SQLAlchemy 与 Alembic 课程规则

## 学习路线

- 先讲 Engine、连接、SQL 表达式或 ORM Session 的职责，再进入模型映射、CRUD、事务、关系、查询性能、Repository/Service 和 Alembic 迁移。
- 一条主线明确使用 SQLAlchemy 2.x 风格，不无说明混用旧式 Query API。

## 技术底线

- 区分 Engine、Connection、Session、事务和模型对象的生命周期。
- Session 不是数据库本身；说明 flush、commit、rollback、refresh 和关闭的状态变化。
- 查询示例说明返回对象类型、是否实际发出 SQL、关系加载方式和 N+1 风险。
- 事务边界由业务用例决定，不在底层方法中随意隐藏提交。
- Alembic 区分模型定义、迁移脚本和真实数据库状态；自动生成后必须人工检查。

## 项目与验证

- 数据库 URL、模型、表名、字段、事务和迁移版本跨章节一致。
- 提供建库/迁移、样例数据、CRUD、回滚和迁移状态验证；破坏性迁移说明备份与回退限制。
