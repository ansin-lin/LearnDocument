# Spring Boot 新版主线

本路线以“员工管理后端 API”为贯穿项目，使用Spring Boot 4.1.x（示例固定为4.1.1）、Java 17、MyBatis Spring Boot Starter 4.1.0、MyBatis Spring Boot Starter Test 4.1.0和MySQL 8.0.16及以上版本构建一个可运行、可测试、可交付的Web后端系统。

## 技术基线

主线固定使用以下组合，后续章节不随意切换大版本：

| 技术 | 课程基线 | 说明 |
| --- | --- | --- |
| Java | 17 | 编译和运行都使用同一主版本 |
| Spring Boot | 4.1.x（POM固定4.1.1） | 使用Spring Framework 7、Jakarta EE 11相关API和Boot 4模块化依赖 |
| MyBatis Spring Boot Starter | 4.1.0 | 与Spring Boot 4.1、Java 17及以上版本兼容 |
| MyBatis Spring Boot Starter Test | 4.1.0 | Mapper测试专用Starter，与正式Starter保持同一版本 |
| MySQL | 8.0.16+ | 从8.0.16起会实际执行本课程使用的CHECK约束；语法按MySQL 8.0说明 |

本课程把Spring Boot 4.1.x与MyBatis Starter 4.1.0作为一套兼容组合。学习主线时不要只改其中一个依赖；参与实际项目时，应以项目的依赖清单、构建结果和升级方针为准。

## 本地开发与验证工具

第1～18章统一采用下面的本地工作流：

```text
Eclipse编写和调试代码
  → 从启动类运行Spring Boot并查看Console
  → Postman验证HTTP方法、URL、参数、Body、状态、Headers和Cookie
  → MySQL客户端核对数据库结果
  → 整理脱敏的自测证据
```

- 第3章讲解工程导入及Eclipse启动，日常开发不要求每次通过命令行启动Spring Boot。
- 第6章集中讲解Postman的环境、Collection、Params、Headers、Body、响应和Cookie入口，后续章节直接复用。
- 第12章的JUnit、Mockito和MockMvc属于自动化测试，不用Postman替代。
- Maven仍负责依赖、测试、构建和打包；第17章保留可执行JAR构建与脱离IDE运行。
- 第18章建立部署、systemd、日志、健康检查、发布和回滚的基本认识；复杂服务器操作放入附录。

## 一、学习目标

完成学习后，应能够：

- 创建并运行 Spring Boot 项目
- 编写 REST API 接口
- 接收请求参数并返回 JSON
- 使用 Controller、Service、Mapper 分层开发
- 使用 DTO、VO 和统一响应结构
- 接入 MySQL 与 MyBatis
- 完成员工增删改查、查询分页
- 使用事务、统一异常和日志
- 实现登录与权限基础
- 编写基础单元测试和接口测试
- 打包并在 Linux 环境运行后端服务
- 根据前端规格实现可联调的后台API
- 按改修票完成影响调查、实现、Review、回归测试和交付

## 二、贯穿项目

项目名称：员工管理后端 API

主要功能：

- 员工列表查询
- 员工详情查询
- 新增员工
- 修改员工
- 删除员工
- 条件查询与分页
- 登录与基础权限
- 日志与异常排查

## 三、学习顺序

### 基础主线

按下面的顺序先理解后端职责，再理解后端内部的分工：

| 章节 | 内容 | 阶段成果 |
| --- | --- | --- |
| 1 | [一个后端系统需要完成什么](mainline/01_springboot_overview.md) | 能拆解后端任务，理解请求响应与框架的分工 |
| 2 | [为什么需要三层架构](mainline/02_three_layer_architecture.md) | 能划分职责，解释调用与返回，分析改修影响 |
| 3 | [创建并运行第一个Spring Boot工程](mainline/03_create_project_and_first_api.md) | 从空目录创建、构建并运行工程，接口返回文本 |
| 4 | [Spring怎样创建并连接各层对象](mainline/04_spring_objects_dependency_injection.md) | 建立Controller→Service调用，解释Bean和构造器注入 |
| 5 | [接口规格与数据对象为什么要分开](mainline/05_api_spec_and_data_objects.md) | 根据接口规格建立请求DTO、详情响应和列表响应的数据边界 |
| 6 | [把HTTP请求连接到Java方法](mainline/06_http_request_to_java_method.md) | 实现三种输入方式，理解JSON转换并定位请求阶段错误 |
| 7 | [成功与失败怎样返回](mainline/07_success_and_failure_response.md) | 用HTTP状态、统一响应和异常处理表达成功与失败 |
| 8 | [输入校验与业务规则](mainline/08_validation_and_business_rules.md) | 实现字段校验，区分JSON错误、格式规则与业务规则 |
| 9 | [数据库记录怎样成为接口响应](mainline/09_database_and_mybatis.md) | 建立表、Entity、Mapper和Service的数据访问链 |
| 10 | [完成员工增删改查](mainline/10_employee_crud.md) | 实现真实CRUD，并同时核对HTTP响应与数据库状态 |
| 11 | [用日志定位接口故障](mainline/11_logging_and_diagnosis.md) | 建立请求追踪与关键日志，完成一次故障调查闭环 |
| 12 | [员工CRUD的分层测试与接口验证](mainline/12_automated_testing_and_regression.md) | 分别完成Mapper、Service、Controller测试和Postman确认，最后验证完整Spring Boot调用链 |
| 13 | [用事务保证多步写入一致](mainline/13_transaction_and_consistency.md) | 更新员工并写变更履历，用成功与失败测试证明提交和回滚 |
| 14 | [让员工列表支持条件查询与分页](mainline/14_search_sort_pagination.md) | 实现筛选、稳定排序和分页，验证总件数、空页与非法参数 |
| 15 | [用Session识别当前登录用户](mainline/15_session_login.md) | 使用数据库账号和BCrypt登录，通过Session完成当前用户识别与安全退出 |
| 16 | [权限与访问范围](mainline/16_authorization_and_data_scope.md) | 用权限矩阵、URL规则和Service方法检查限制角色操作与本人数据范围 |
| 17 | [多环境配置与打包](mainline/17_profiles_configuration_packaging.md) | 理解Profile与环境变量，用Maven生成并在本机运行可执行JAR |
| 18 | [部署与运行检查](mainline/18_deployment_operations_check.md) | 理解服务器运行、systemd、四层检查、日志调查、发布与回滚 |
| 19 | [为Vue有給休暇系统实现后台API](mainline/19_paid_leave_vue_backend_practice.md) | 实现与Vue第22章一致的后台，并按第23章改修日期筛选，完成测试、联调和交付 |

完成第18章后，使用第19章独立完成一次前后端联调综合练习。需要缓存、批量任务或账票交付时，再进入[Spring Boot 企业扩展](extensions/index.md)：先学[Redis](extensions/redis.md)、[Spring Batch](extensions/spring-batch.md)与[账票处理](extensions/reporting.md)，最后完成[月度员工变更账票项目](extensions/monthly-report-project.md)。企业扩展从第18章员工管理稳定成果的独立副本开始，不改变主线最终状态。进入既存项目时可按需要使用附录补足代码识读能力：

完整入口见[附录学习入口](appendix/index.md)。A开头的附录与01～18章的章号对应；一章没有专属附录时直接跳号，同一章有多个附录时使用 `A03-1` 这样的子编号。B开头的是跨章节通用附录。

| 章节对应附录 | 学习成果 |
| --- | --- |
| [A03-1 使用@Controller返回HTML页面](appendix/A03_01_spring_mvc_html_views.md) | 理解视图名称、Model数据和Thymeleaf服务端渲染 |
| [A03-2 使用DevTools加快本地开发](appendix/A03_02_devtools_local_development.md) | 理解自动编译、自动重启及其生产边界 |
| [A04 多个Bean候选的选择与排错](appendix/A04_multiple_bean_candidates.md) | 处理同一接口多个实现时的选择规则与启动失败 |
| [A05 Jackson字段映射与输出规则](appendix/A05_jackson_json_mapping.md) | 学习字段改名、忽略、空值和日期格式 |
| [A06 Header、Cookie与文件上传](appendix/A06_http_headers_cookies_file_upload.md) | 扩展请求头、Cookie和multipart输入 |
| [A08 使用@Validated选择校验分组](appendix/A08_validated_validation_groups.md) | 让同一请求对象在不同阶段执行不同约束 |
| [A09-1 Java Web项目中的日期时间与时区](appendix/A09_01_java_web_datetime.md) | 区分本地时间、UTC瞬间、区域时区和MySQL时间列 |
| [A09-2 Spring Boot怎样读取独立MyBatis配置](appendix/A09_02_springboot_mybatis_config.md) | 在可恢复实验中切换YAML与mybatis-config.xml |
| [A11-1 Spring Boot日志配置进阶](appendix/A11_logging_configuration_advanced.md) | 理解有效等级、文件滚动、环境覆盖、结构化日志与自定义Logback边界 |
| [A11-2 Filter与请求处理执行时机](appendix/A11_2_filter_execution_timing.md) | 区分Servlet分派、Filter注册顺序、Security、MVC异常和MDC清理 |
| [A11-3 使用AOP统一记录方法日志](appendix/A11_3_aop_method_logging.md) | 使用精确切点和自定义注解记录Service耗时，并保持返回、异常和事务语义 |
| [A12-1 常用测试断言库与断言方法](appendix/A12_01_testing_assertions_reference.md) | 按检查对象选择JUnit、AssertJ、Hamcrest与MockMvc断言 |
| [A12-2 使用H2完成独立数据库测试实验](appendix/A12_02_h2_test_database.md) | 用独立profile完成H2 Mapper实验并理解与MySQL的差异 |
| [A13 事务、隔离级别、并发更新与锁](appendix/A13_transaction_isolation_concurrency_locks.md) | 理解ACID、隔离现象和InnoDB锁，并为并发更新选择约束、原子SQL、乐观锁或悲观锁 |
| [A17 配置、构建与交付进阶](appendix/A17_configuration_build_delivery_advanced.md) | 调查配置覆盖、构建产物与正式交付检查项 |
| [A18-1 Linux部署与运维手顺参考](appendix/A18_01_linux_deployment_operations_reference.md) | 查阅systemd Unit、更新、切り戻し和运维记录 |
| [A18-2 常见运行与数据库迁移组件识读](appendix/A18_02_operations_components_reading.md) | 初步识别Actuator、Flyway和Liquibase |

| 通用附录 | 学习成果 |
| --- | --- |
| [B01 Spring Boot调用外部REST API](appendix/B01_external_api_restclient.md) | 用RestClient识别GET、POST、超时和外部异常转换 |
| [B02 如何阅读一个陌生的Spring项目](appendix/B02_existing_project_reading.md) | 从构建、启动、配置、业务链到共通机制完成调查 |
| [B03 日本既存Spring项目的新旧结构识读](appendix/B03_legacy_spring_project_reading.md) | 识别Boot 2/3、JAR/WAR、外部Tomcat和XML配置 |
| [B04 Lombok与Spring Data JPA识读](appendix/B04_lombok_jpa_reading.md) | 识别编译期生成代码、Entity和Repository |
| [B05 Spring项目常见注解速查](appendix/B05_spring_annotation_reference.md) | 按框架、位置、处理时机和课程场景理解注解 |
| [B06 Spring共通处理机制](appendix/B06_spring_common_processing_mechanisms.md) | 区分Filter、Interceptor、Controller Advice与AOP的责任 |
| [B07 常见问题与排查索引](appendix/B07_troubleshooting_review_index.md) | 按失败阶段回到对应内容查找原因 |

基础主线现已形成从系统认识、工程实现、部署基础到前后端联调综合任务的完整学习顺序。附录建议按当前项目实际使用的技术选择阅读，不要求一次背完。

## 四、推荐前置知识

- Java 基础语法
- 面向对象基础
- SQL 基础查询与 DML
- HTTP 请求响应基础
- MyBatis 基础概念

不熟悉 HTTP 时，先学习 [Web 开发基础](../../web_basics/00_frontend_backend_request_response.md)。
