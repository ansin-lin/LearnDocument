# Spring Boot教学文档第二轮增量修改报告

> 历史说明：第三轮已把旧项目识读拆为Appendix C，并将原Lombok、JPA两个文件合并为Appendix D。当前学习入口和有效路径以 `appendix/index.md` 为准；本报告保留第二轮完成时的文件快照。

## 一、处理结果

本轮保持01～20章顺序、Employee Management业务、Controller→Service→Mapper结构、MyBatis、Session与Spring Security、测试、部署和交付主线不变。新增内容采用原章补充、可恢复的小实验和独立附录；没有把Lombok或JPA强行引入Employee主线。

| 优先级 | 修改项 | 文件 | 新增章节/位置 | 状态 |
| --- | --- | --- | --- | --- |
| P0 | RequestHeader | `mainline/06_http_request_to_java_method.md` | 十二、常见HTTP输入位置补充 | 完成 |
| P0 | CookieValue | `mainline/06_http_request_to_java_method.md` | 十二、常见HTTP输入位置补充 | 完成 |
| P0 | RequestPart / MultipartFile | `mainline/06_http_request_to_java_method.md` | 十二、常见HTTP输入位置补充 | 完成 |
| P0 | Jackson | `mainline/05_api_spec_and_data_objects.md` | 十一、JSON与Java字段映射常见注解 | 完成 |
| P0 | Date/Time | `mainline/09_database_and_mybatis.md` | 十二、Java Web项目中的日期与时间 | 完成 |
| P0 | Filter / Interceptor / AOP | `mainline/11_logging_and_diagnosis.md` | 十三、Spring共通处理机制的位置 | 完成 |
| P0 | RestClient | `appendix/external_api_restclient.md` | 全文独立专题 | 完成 |
| P0 | Existing Project Reading | `appendix/existing_project_reading.md` | Step 1～5、调查模板 | 完成 |
| P0 | Boot 2 / Boot 3 / WAR / XML | `appendix/existing_project_reading.md` | 七、Spring Boot新旧项目常见差异 | 完成 |
| P1 | CORS | `mainline/16_authorization_and_data_scope.md` | 八、Session、同源与CORS的边界 | 完成 |
| P1 | Optimistic / Pessimistic Lock | `mainline/13_transaction_and_consistency.md` | 十二、并发更新为什么可能覆盖别人的修改 | 完成 |
| P1 | Logical Delete | `mainline/19_japanese_project_change_practice.md` | 十七、追加改修演练 | 完成 |
| P1 | Lombok | `appendix/lombok_existing_project.md` | 全文识读专题 | 完成 |
| P1 | JPA | `appendix/jpa_for_mybatis_learners.md` | 全文识读专题 | 完成 |
| P1 | Annotation Reference | `appendix/spring_annotation_reference.md` | Bean、MVC、Validation、安全、JSON等分类速查 | 完成 |
| P2 | `@Scheduled` / `@Async` | `appendix/spring_annotation_reference.md` | 八、生命周期与后台执行 | 完成 |
| P2 | `@PostConstruct` / `@PreDestroy` | `appendix/spring_annotation_reference.md` | 八、生命周期与后台执行 | 完成 |
| P2 | Actuator | `appendix/operations_components_reading.md` | 一、Spring Boot Actuator | 完成 |
| P2 | Flyway | `appendix/operations_components_reading.md` | 二、Flyway | 完成 |
| P2 | Liquibase | `appendix/operations_components_reading.md` | 三、Liquibase | 完成 |

## 二、第4章轻量处理

`mainline/04_spring_objects_dependency_injection.md` 保留PaymentService、两个实现、Bean名称、`@Qualifier`、`@Primary`、`NoUniqueBeanDefinitionException`、故障定位、实验恢复和主线回归。第一次Qualifier实验仍为完整代码；Primary及故障制造改为只展示差分，减少重复，没有删除知识点。

## 三、主线稳定性处理

- 第13章并发控制使用独立实验，version字段和实验Mapper方法在结束时删除，并要求重跑原有测试。
- 第16章CORS使用前后端分离实验，结束时删除CORS增量并恢复同源主线；认证、授权与CSRF规则不放宽。
- 第19章逻辑删除作为EMP-268调查练习，只提交确认事项、影响调查、差分草案、测试和回退步骤，不执行DDL，不替换EMP-241。
- Lombok、JPA、Actuator、Flyway、Liquibase均为既存项目识读，不改Employee实现。

## 四、修改文件

### 既有文件

- `index.md`
- `mainline/04_spring_objects_dependency_injection.md`
- `mainline/05_api_spec_and_data_objects.md`
- `mainline/06_http_request_to_java_method.md`
- `mainline/09_database_and_mybatis.md`
- `mainline/11_logging_and_diagnosis.md`
- `mainline/13_transaction_and_consistency.md`
- `mainline/16_authorization_and_data_scope.md`
- `mainline/19_japanese_project_change_practice.md`
- 仓库根目录 `mkdocs.yml`
- 本地维护文件 `REWRITE_CHECKLIST.local.md`；不加入MkDocs，且继续由本地排除规则管理

### 新增文件

- `appendix/external_api_restclient.md`
- `appendix/existing_project_reading.md`
- `appendix/lombok_existing_project.md`
- `appendix/jpa_for_mybatis_learners.md`
- `appendix/spring_annotation_reference.md`
- `appendix/operations_components_reading.md`
- `SECOND_ROUND_CHANGE_REPORT.md`

## 五、验证结果

| 检查 | 结果 |
| --- | --- |
| Spring Boot文档审计 | 扫描30个Markdown文件；P0=0、P1=0、P2=0 |
| `mkdocs.yml` YAML解析 | 通过 |
| MkDocs严格构建 | 通过，文档站点成功生成 |
| Java示例编译 | 以Java 17 release、Spring Boot 3.5.16独立编译通过 |
| 编译覆盖 | Jackson注解、Header/Cookie/Multipart、Interceptor、CORS、Security CORS、RestClient、JDK HTTP超时工厂 |
| Git操作 | 未执行add、commit或push |

导航变更后还执行了全docs审计。全库既有 `frontend/backup` 与其中 `node_modules` 存在18个P0、122个P1、18个P2历史问题，与本轮Spring Boot文件无关；本轮未越权修改这些旧备份。MkDocs构建提示Java答案、本地维护清单等文件未进入导航，符合其当前用途。

## 六、未执行的外部验证与风险

本轮没有连接真实HR System、真实MySQL并发会话、浏览器跨域环境、外部Tomcat或Boot 2/XML旧项目，因此这些部分使用兼容基线编译、配置检查、步骤审查和明确的实验验收条件验证。实际企业项目仍需按目标系统版本、网络、安全策略、数据库隔离级别和部署环境执行集成测试。
