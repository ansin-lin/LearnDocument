# Appendix：常见运行与数据库迁移组件识读

本附录只建立Actuator、Flyway和Liquibase的项目阅读能力。Employee主线仍按现有部署检查和SQL准备方式运行，不为展示组件而更改数据库初始化流程。

## 一、Spring Boot Actuator

Actuator为运行中的应用提供健康、信息、指标等管理端点。项目加入 `spring-boot-starter-actuator` 后，常见端点包括 `/actuator/health`；实际暴露范围由配置决定。

阅读项目时确认：依赖是否存在、management端口和base path、哪些端点暴露、是否有Security保护、健康检查是否泄露数据库或外部系统细节、监控平台怎样采集。不要在生产环境直接暴露全部端点；“能访问health”也不等于完整业务验收通过。

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

上述只是识读示例。真实项目应由安全和运维规格确定端口、网络边界、认证与详情级别。

## 二、Flyway

Flyway通常按版本化迁移脚本管理数据库结构变化。常见目录和文件名类似：

```text
src/main/resources/db/migration/
V1__create_employee.sql
V2__add_employee_status.sql
```

应用启动时，Flyway根据历史表判断哪些迁移尚未执行。已在共享环境执行的版本脚本通常不应直接重写，应按团队规则新增下一版本。调查时确认执行账号、schema、baseline策略、失败后的恢复方式以及测试环境是否使用同一迁移集。

## 三、Liquibase

Liquibase使用changelog描述数据库变更，可采用YAML、XML、JSON或SQL。常见入口配置类似：

```yaml
spring:
  liquibase:
    change-log: classpath:/db/changelog/db.changelog-master.yaml
```

changelog包含changeSet及执行历史。阅读时追踪主changelog的include顺序、changeSet id/author、前置条件、回滚定义和环境差异。不要同时让Hibernate自动建表、`schema.sql`、Flyway和Liquibase无规划地管理同一套schema。

## 四、三者解决的问题不同

| 组件 | 主要用途 | 不是用来做什么 |
| --- | --- | --- |
| Actuator | 观察应用运行健康和指标 | 代替业务测试或开放管理后台 |
| Flyway | 按版本执行数据库迁移 | 自动决定业务表设计 |
| Liquibase | 用changelog管理数据库变更 | 与Flyway同时无规则修改同一schema |

练习：只读检查一个既存项目，分别回答管理端点的访问边界、数据库迁移入口、最新已执行版本/changeset和失败时的恢复手册在哪里。若项目没有这些组件，记录“未使用”及其现行替代流程，不要擅自加入依赖。
