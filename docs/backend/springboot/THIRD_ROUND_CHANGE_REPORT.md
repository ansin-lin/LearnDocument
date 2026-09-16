# Spring Boot教学文档第三轮最终修改报告

## 一、完成状态

| No | 修改项 | 文件 | 状态 | 验证 |
| --- | --- | --- | --- | --- |
| 1 | 乐观锁字段修正 | 第13章 | 完成 | SQL统一使用`name`和`#{name}` |
| 2 | Lost Update案例 | 第13章 | 完成 | 使用department与email完整PUT，不再出现phone |
| 3 | 并发实验定位 | 第13章 | 完成 | 标记为设计识读与SQL机制实验，列明缺少的完整实现 |
| 4 | CORS auth范围 | 第16章 | 完成 | 同时覆盖`/auth/**`与`/employees/**`及完整Session流程 |
| 5 | External API | Appendix A | 完成 | RestClient、GET/POST、超时、错误分类、日志和Review |
| 6 | 既存项目阅读 | Appendix B | 完成 | README、构建、启动、配置、纵向业务链、横向机制与测试 |
| 7 | 老项目识读 | Appendix C | 完成 | Boot 2/3、javax/jakarta、JAR/WAR、ServletInitializer和XML |
| 8 | Lombok/JPA | Appendix D | 完成 | 构造器注入、Builder、Entity、Repository与MyBatis对比 |
| 9 | Annotation Reference | Appendix E | 完成 | Bean、MVC、Validation、Persistence、Security、JSON、Testing等速查 |
| 10 | Cross-File Review | 01～20章与Appendix | 完成 | 字段、API、版本、实验恢复、链接和导航一致性检查通过 |

## 二、修改过的文件

- `mainline/13_transaction_and_consistency.md`
- `mainline/16_authorization_and_data_scope.md`
- `appendix/external_api_restclient.md`
- `appendix/existing_project_reading.md`
- `appendix/spring_annotation_reference.md`
- `index.md`
- `SECOND_ROUND_CHANGE_REPORT.md`：补充第三轮文件迁移历史说明
- 仓库根目录 `mkdocs.yml`
- 本地维护文件 `REWRITE_CHECKLIST.local.md`：不进入MkDocs和Git提交

## 三、新建的文件

- `appendix/index.md`
- `appendix/A03_legacy_spring_project_reading.md`
- `appendix/A04_lombok_jpa_reading.md`
- `THIRD_ROUND_CHANGE_REPORT.md`

原 `appendix/lombok_existing_project.md` 与 `appendix/jpa_for_mybatis_learners.md` 的有效内容已经合并到Appendix D，旧文件已移除，避免同一知识维护两份正文。

## 四、有意未修改的文件

主线第1～12章、第14～15章、第17～20章没有继续扩展大型内容。`appendix/operations_components_reading.md` 与 `appendix/troubleshooting_review_index.md` 内容保持不变，只加入新的Appendix入口导航。Employee业务、MyBatis主线、Session/Spring Security方案、EMP-241、部署和最终交付状态均未更换。

## 五、发现并修复的跨章节问题

1. 第13章乐观锁SQL错误使用 `employee_name`、`employeeName`，已改为主线字段 `name`、`#{name}`。
2. 第13章悲观锁SELECT错误读取 `employee_name`，已改为 `name`。
3. Lost Update使用主线不存在的phone，已改成“B修改email但完整PUT覆盖A的department”。
4. 并发示例原表述容易让学员误以为片段可直接运行，现已声明它不包含完整DTO、Entity、Mapper、Controller、Advice和集成测试。
5. 第16章CORS只覆盖员工路径，无法完成跨域登录；现已补充auth路径、CSRF、登录、Session Cookie、权限和退出流程。
6. 第二轮旧项目识读与Lombok/JPA文件边界分散；现拆出Appendix C、合并Appendix D，并建立A01～A05统一入口。
7. 原JPA识读示例使用 `employee_name/employeeName`，Appendix D已统一为主线表字段和Java字段 `name`。
8. 注解速查缺少Testing分类，现已补充JUnit、Boot Test、Spring Test和Security Test常见注解。

第5章的 `employeeName/employee_name` 明确标记为不加入主线的Jackson独立映射示例；第8章phone明确标记为临时Validation练习并要求完成后删除。两者不会成为后续Employee稳定字段。

## 六、一致性Review结果

| 检查对象 | 结论 |
| --- | --- |
| Employee稳定表字段 | `id/name/department/email/status/created_at/updated_at`保持一致 |
| 临时数据库字段 | version仅用于第13章设计实验；deleted系列仅用于第19章改修设计 |
| Employee稳定Java字段 | `name/department/email/status`保持一致 |
| CRUD API | GET列表/详情、POST、PUT、DELETE的方法与状态规格一致 |
| Auth API | csrf、login、me、logout在第15、16章保持一致 |
| 课程基线 | Java 17、Spring Boot 3.5.16、MySQL 8.0.16+未改变 |
| 实验恢复 | Payment、Interceptor、version、CORS和逻辑删除均注明删除、恢复或不进入主线 |
| Appendix边界 | 独立识读/实验，不改变第20章最终交付状态 |

## 七、验证结果

| 验证 | 结果 |
| --- | --- |
| Spring Boot教学文档审计 | 32个Markdown；P0=0、P1=0、P2=0 |
| 全docs审计 | 已执行；发现的18个P0、122个P1、18个P2均位于既有frontend备份/node_modules或docs入口，不属于本轮Spring Boot范围 |
| Cross-file自动断言 | 字段、CRUD/Auth API、CORS范围、实验定位和版本全部通过 |
| Java示例编译 | Java 17 release、Spring Boot 3.5.16通过 |
| 编译覆盖 | RestClient、CORS、Security、ServletInitializer、Lombok、JPA等新增接口 |
| `mkdocs.yml`解析 | 通过 |
| MkDocs严格构建 | 通过 |
| Git差分格式检查 | 通过；仅有仓库既有LF/CRLF提示 |
| Git操作 | 未执行add、commit或push |

## 八、有意不处理的进阶知识

本轮没有加入Kafka、RabbitMQ、Redis、Spring Cloud、WebFlux、Kubernetes、微服务治理、分布式事务、OAuth2 Server、复杂JWT、DDD、CQRS、Event Sourcing、Hibernate高级专题、Spring源码、自定义ClassLoader和高级AOP。它们属于后续进阶课程，不是当前新人进入日本Spring Web项目的必要前置。

## 九、冻结结论

当前教材已形成“能实现主线功能、能解释Spring机制、能处理常见项目问题、能阅读既存日本项目”的完整范围。后续原则上进入内容冻结和实际教学，只根据学员反馈、版本变化或发现的明确错误做小范围修正，不再以增加知识点数量作为完整性标准。
