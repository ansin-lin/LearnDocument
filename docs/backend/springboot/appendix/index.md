# Spring Boot Appendix学习入口

01～20章是Employee Management主线，最终交付状态以第20章为准。下面的代码默认用于独立识读或独立实验，不要求把RestClient、Lombok、JPA、XML配置或其他扩展全部永久加入主线工程。

## 推荐顺序

| 顺序 | 内容 | 侧重点 |
| --- | --- | --- |
| A01 | [Spring Boot调用外部REST API](external_api_restclient.md) | 实际开发：Service访问数据库以外的系统 |
| A02 | [如何阅读一个陌生的Spring项目](existing_project_reading.md) | 既存项目：从构建、配置到纵向业务链和测试 |
| A03 | [日本既存Spring项目的新旧结构识读](A03_legacy_spring_project_reading.md) | 既存项目：Boot 2/3、JAR/WAR、外部Tomcat和XML |
| A04 | [Lombok与Spring Data JPA识读](A04_lombok_jpa_reading.md) | 既存项目：编译期生成代码和Repository数据访问 |
| A05 | [Spring项目常见注解速查](spring_annotation_reference.md) | 查阅：按框架、位置和处理时机理解注解 |
| A06 | [多个Bean候选的选择与排错](A06_multiple_bean_candidates.md) | 依赖注入扩展：Qualifier、Primary与候选不唯一 |
| A07 | [Jackson字段映射与输出规则](A07_jackson_json_mapping.md) | JSON扩展：字段改名、忽略、空值与日期格式 |
| A08 | [Header、Cookie与文件上传](A08_http_headers_cookies_file_upload.md) | HTTP输入扩展：请求头、Cookie与multipart |
| A09 | [使用@Controller返回HTML页面](A09_spring_mvc_html_views.md) | 服务端页面：视图名称、Model与Thymeleaf模板 |
| A10 | [使用@Validated选择校验分组](A10_validated_validation_groups.md) | Validation扩展：同一DTO按处理阶段执行不同约束 |
| A11 | [Java Web项目中的日期时间与时区](A11_java_web_datetime.md) | 时间扩展：Java类型、UTC、区域时区与MySQL列语义 |
| A12 | [Spring Boot怎样读取独立MyBatis配置](A12_springboot_mybatis_config.md) | MyBatis配置扩展：YAML、核心XML、Mapper XML与恢复实验 |
| A13 | [Spring共通处理机制](A13_spring_common_processing_mechanisms.md) | 既存项目识读：Filter、Interceptor、Controller Advice与AOP边界 |

A01偏实际开发；A02～A05偏既存项目阅读；A06～A13是基础主线后的单主题扩展。附录不要求一次读完，应从当前任务使用的技术进入，再沿引用补足前置知识。

## 补充索引

- [常见运行与数据库迁移组件识读](operations_components_reading.md)：Actuator、Flyway和Liquibase基础识读。
- [常见问题与排查索引](troubleshooting_review_index.md)：按失败阶段回到对应主线内容。

## 完成标准

完成必要附录后，你应能从README和构建文件确认项目基线，找到启动和配置入口，沿一个URL追踪Controller、Service、Mapper/Repository/API Client，识别Security、Validation、Transaction和异常处理位置，并根据改修票整理確認事項、影響範囲、测试证据和改修报告。
