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

A01偏实际开发；A02～A05偏既存项目阅读。遇到陌生代码时不必一次读完全部附录，应从当前改修票使用的技术进入，再沿引用补足前置知识。

## 补充索引

- [常见运行与数据库迁移组件识读](operations_components_reading.md)：Actuator、Flyway和Liquibase基础识读。
- [常见问题与排查索引](troubleshooting_review_index.md)：按失败阶段回到对应主线内容。

## 完成标准

完成必要附录后，你应能从README和构建文件确认项目基线，找到启动和配置入口，沿一个URL追踪Controller、Service、Mapper/Repository/API Client，识别Security、Validation、Transaction和异常处理位置，并根据改修票整理確認事項、影響範囲、测试证据和改修报告。
