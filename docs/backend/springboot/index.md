# Spring Boot 新版主线

本路线以“员工管理后端 API”为贯穿项目，使用Spring Boot 3.5.16、Java 17、MyBatis Spring Boot Starter 3.0.x和MySQL 8.0.16及以上版本构建一个可运行、可测试、可交付的Web后端系统。

## 技术基线

主线固定使用以下组合，后续章节不随意切换大版本：

| 技术 | 课程基线 | 说明 |
| --- | --- | --- |
| Java | 17 | 编译和运行都使用同一主版本 |
| Spring Boot | 3.5.16 | 保持Spring Framework 6和Jakarta命名空间一致 |
| MyBatis Spring Boot Starter | 3.0.x | 与Spring Boot 3.2～3.5、Java 17兼容 |
| MySQL | 8.0.16+ | 从8.0.16起会实际执行本课程使用的CHECK约束；语法按MySQL 8.0说明 |

Spring Boot 4和MyBatis Starter 4属于另一套兼容组合。学习主线时不要只升级其中一个依赖；参与实际项目时，应以项目的依赖清单、构建结果和升级方针为准。

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
- 按改修票完成影响调查、实现、Review和回归测试
- 整理可重建、可验收、可部署和可回退的交付包

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
| 12 | [用自动化测试保护员工CRUD](mainline/12_automated_testing_and_regression.md) | 建立Service、Web与数据库测试，根据改修选择回归范围 |
| 13 | [用事务保证多步写入一致](mainline/13_transaction_and_consistency.md) | 更新员工并写变更履历，用成功与失败测试证明提交和回滚 |
| 14 | [让员工列表支持条件查询与分页](mainline/14_search_sort_pagination.md) | 实现筛选、稳定排序和分页，验证总件数、空页与非法参数 |
| 15 | [用Session识别当前登录用户](mainline/15_session_login.md) | 使用数据库账号和BCrypt登录，通过Session完成当前用户识别与安全退出 |
| 16 | [权限与访问范围](mainline/16_authorization_and_data_scope.md) | 用权限矩阵、URL规则和Service方法检查限制角色操作与本人数据范围 |
| 17 | [多环境配置与打包](mainline/17_profiles_configuration_packaging.md) | 拆分dev/test/prod配置，校验配置并生成可脱离IDE运行的可执行JAR |
| 18 | [部署与运行检查](mainline/18_deployment_operations_check.md) | 在Ubuntu上受控部署JAR，用systemd管理服务并完成四层检查与回滚 |
| 19 | [按改修票完成一次日本项目变更](mainline/19_japanese_project_change_practice.md) | 按确定规格完成影响调查、多选查询改修、Review、回归测试和改修报告 |
| 20 | [综合验收与交付](mainline/20_acceptance_and_delivery.md) | 从干净环境重建、验收、部署并交付可追踪的完整成果 |
| 21 | [结业综合练习：为Vue有給休暇系统实现后台API](mainline/21_paid_leave_vue_backend_practice.md) | 用Spring Boot与MyBatis替换参考后台，并与既有Vue完成联调 |

完成第20章后，先使用第21章独立完成一次前后端联调综合练习，再按需要使用附录补足进入既存项目时的代码识读能力。第21章和附录都不会改变Employee主线最终状态：

| 附录 | 学习成果 |
| --- | --- |
| [Appendix学习入口](appendix/index.md) | 按A01～A05选择实际开发或既存项目识读主题 |
| [Spring Boot调用外部REST API](appendix/external_api_restclient.md) | 用RestClient识别GET、POST、超时和外部异常转换 |
| [如何阅读一个陌生的Spring项目](appendix/existing_project_reading.md) | 从构建、启动、配置、业务链到共通机制完成调查 |
| [日本既存Spring项目的新旧结构识读](appendix/A03_legacy_spring_project_reading.md) | 识别Boot 2/3、JAR/WAR、外部Tomcat和XML配置 |
| [Lombok与Spring Data JPA识读](appendix/A04_lombok_jpa_reading.md) | 识别编译期生成代码、Entity和Repository |
| [Spring项目常见注解速查](appendix/spring_annotation_reference.md) | 按框架、位置、处理时机和课程场景理解注解 |
| [常见运行与数据库迁移组件识读](appendix/operations_components_reading.md) | 初步识别Actuator、Flyway和Liquibase |
| [常见问题与排查索引](appendix/troubleshooting_review_index.md) | 按失败阶段回到对应内容查找原因 |

基础主线现已形成从系统认识、工程实现、测试改修到验收交付的完整学习顺序。附录建议按当前项目实际使用的技术选择阅读，不要求一次背完。

## 四、推荐前置知识

- Java 基础语法
- 面向对象基础
- SQL 基础查询与 DML
- HTTP 请求响应基础
- MyBatis 基础概念

不熟悉 HTTP 时，先学习 [Web 开发基础](../../web_basics/00_frontend_backend_request_response.md)。
