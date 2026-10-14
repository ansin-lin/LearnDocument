# Spring Boot附录学习入口

01～18章是Employee Management教学主线，第19章是与Vue对接的独立结业任务。A开头的附录与主线章号对应，没有专属附录的章号直接跳过；同一章有多个附录时使用 `A03-1`、`A03-2` 这样的子编号。B开头的是跨章节通用附录。附录不要求连续学习，也不要求把所有实验代码永久加入主线工程。

附录继续使用主线技术基线：Java 17、Spring Boot 4.1.x（示例POM固定4.1.1）、MyBatis Starter与Test Starter 4.1.0。既存项目附录会同时展示Boot 2或Boot 3写法，看到旧写法时应先确认项目版本，不能直接复制到Boot 4主线。

## 一、先按当前任务选择路线

| 路线 | 适合什么任务 | 建议附录 |
| --- | --- | --- |
| 接口与数据表达扩展 | 调用外部API、调整JSON、读取Header/Cookie、上传文件、返回HTML、使用校验分组、处理日期时间或独立MyBatis配置 | B01、A03-1、A05、A06、A08、A09-1、A09-2 |
| 既存项目识读与Spring机制 | 接手陌生项目、识别旧结构、阅读Lombok/JPA、查注解、多Bean排错、判断共通处理放在哪一层 | B02～B06、A04 |
| 质量保证、开发效率与故障排查 | 调整日志、辨认请求处理时机、补充测试断言、比较H2与MySQL测试、使用本地开发辅助工具，或根据现象定位构建、HTTP、数据库、安全及部署问题 | A03-2、A11-1、A11-2、A12-1、A12-2、B07 |
| 配置、交付与运维识读 | 调查配置覆盖、检查构建产物、阅读Linux部署手顺、识别Actuator和数据库迁移组件 | A17、A18-1、A18-2 |

同一个现场任务可能跨越多条路线。例如“服务器上的更新接口返回403”应先用B07确定是Security还是部署问题，再回到第15～16章检查认证、CSRF和权限；只有证据指向服务器配置时才进入A17和A18-1。

## 二、附录知识点与完成成果

| 附录 | 建议前置 | 核心知识点 | 学完后应能完成 |
| --- | --- | --- | --- |
| [A03-1 使用@Controller返回HTML页面](A03_01_spring_mvc_html_views.md) | 第3～7章 | `@Controller`、`@ResponseBody`、视图名、`Model`、Thymeleaf | 返回静态内容与带模型数据的HTML页面，并区分视图和JSON响应 |
| [A03-2 使用DevTools加快本地开发](A03_02_devtools_local_development.md) | 第3章 | DevTools依赖、开发期默认设置、Eclipse自动编译、自动重启、双类加载器、生产边界 | 完成一次代码修改后的自动重启，并区分编译、重启和接口验证问题 |
| [A04 多个Bean候选的选择与排错](A04_multiple_bean_candidates.md) | 第4章 | Bean名称、`@Qualifier`、`@Primary`、候选不唯一异常 | 复现并修复多个同类型Bean导致的注入失败，实验后恢复主线 |
| [A05 Jackson字段映射与输出规则](A05_jackson_json_mapping.md) | 第5～7章 | Jackson 3边界、`@JsonProperty`、`@JsonIgnore`、`@JsonInclude`、`@JsonFormat` | 判断JSON字段为何改名、缺失或改变格式，并保持Response DTO边界 |
| [A06 Header、Cookie与文件上传](A06_http_headers_cookies_file_upload.md) | 第6、8章 | `@RequestHeader`、`@CookieValue`、`multipart/form-data`、`@RequestPart`、`MultipartFile` | 用Postman验证三类HTTP输入，并限制文件大小、类型和保存边界 |
| [A08 使用@Validated选择校验分组](A08_validated_validation_groups.md) | 第8章 | `@Valid`与`@Validated`、分组接口、约束groups、类级方法校验 | 对同一DTO按草稿和提交阶段执行不同校验，并解释适用边界 |
| [A09-1 Java Web项目中的日期时间与时区](A09_01_java_web_datetime.md) | 第5、6、9章 | `LocalDateTime`、`OffsetDateTime`、`Instant`、UTC、ZoneId、MySQL `DATETIME`/`TIMESTAMP` | 从浏览器、JSON、Java到MySQL追踪时间语义，发现丢失的时区信息 |
| [A09-2 Spring Boot怎样读取独立MyBatis配置](A09_02_springboot_mybatis_config.md) | 第9章 | `config-location`、核心配置XML、Mapper XML、YAML设置冲突、Spring管理的数据源和事务 | 在可恢复实验中切换独立配置文件，并定位配置未读取或重复定义问题 |
| [A11-1 Spring Boot日志配置进阶](A11_logging_configuration_advanced.md) | 第11章 | Logger等级、文件输出、滚动策略、容量与保留、MDC格式 | 调整局部日志等级，验证文件滚动配置，并恢复安全的默认状态 |
| [A11-2 Filter、OncePerRequestFilter与RequestLoggingFilter的执行时机](A11_2_filter_execution_timing.md) | 第11章 | Filter接口、Spring基类、项目类、Servlet链与MVC处理时机 | 根据200、404、401请求判断Filter、拦截器、全局异常和AOP各何时参与 |
| [A12-1 常用测试断言库与断言方法](A12_01_testing_assertions_reference.md) | 第12章 | Mapper、Service、Controller和完整集成测试所用的JUnit、AssertJ、MockMvc与Mockito；Postman检查边界 | 根据被测层选择断言，并读懂expected/actual和HTTP测试失败 |
| [A12-2 使用H2完成独立数据库测试实验](A12_02_h2_test_database.md) | 第12章Mapper案例后 | H2内存库、独立profile、测试脚本、Mapper切片、与MySQL的边界 | 独立运行一个H2 Mapper实验，区分Mapper切片测试与纯单元测试，并说明为什么它不能代替MySQL测试 |
| [A17 配置、构建与交付进阶](A17_configuration_build_delivery_advanced.md) | 第17章 | 配置覆盖调查、`@ConfigurationProperties`、产物摘要、外部配置模板、交付清单 | 确认最终生效配置和构建来源，整理不含秘密的交付资料 |
| [A18-1 Linux部署与运维手顺参考](A18_01_linux_deployment_operations_reference.md) | 第18章及Linux基础 | systemd、目录权限、发布检查、切り戻し、端口/日志调查、反向代理与HTTPS边界 | 阅读和执行受控部署手顺，保留发布、验证和回退记录 |
| [A18-2 常见运行与数据库迁移组件识读](A18_02_operations_components_reading.md) | 第9、17、18章 | Actuator、Flyway、Liquibase、管理端点安全、迁移历史与职责冲突 | 在既存项目中找到监控和迁移入口，说明当前组件及其替代流程 |
| [B01 Spring Boot调用外部REST API](B01_external_api_restclient.md) | 第4、6、7、11、12章 | `RestClient`、配置Bean、连接/读取超时、Header与JSON、外部4xx/5xx/网络失败分类、Mock测试 | 在Service之外建立API Client边界，并为超时、错误转换和日志提供证据 |
| [B02 如何阅读一个陌生的Spring项目](B02_existing_project_reading.md) | 完成主线或至少第1～12章 | README、POM、启动入口、配置来源、纵向业务链、横向共通机制、测试证据 | 输出一份带文件位置和调用关系的既存项目调查记录 |
| [B03 日本既存Spring项目的新旧结构识读](B03_legacy_spring_project_reading.md) | B02 | Boot 2/3/4差异、`javax`与`jakarta`、JAR/WAR、外部Tomcat、XML与Java Config | 根据版本证据识别旧结构，不进行危险的机械迁移 |
| [B04 Lombok与Spring Data JPA识读](B04_lombok_jpa_reading.md) | 第2、4、5、9章 | Lombok生成代码、构造器注入、JPA Entity、Repository派生查询、MyBatis与JPA边界 | 读懂使用Lombok/JPA的既存代码，并指出实体边界和查询风险 |
| [B05 Spring项目常见注解速查](B05_spring_annotation_reference.md) | 遇到陌生注解时使用 | Bean、配置、MVC、Validation、异常、事务、安全、JSON、测试和生命周期注解 | 根据import、使用位置、处理者和触发时机调查注解，而不是只记名称 |
| [B06 Spring共通处理机制](B06_spring_common_processing_mechanisms.md) | 第7、11、13、16章 | Filter、Security Filter Chain、Interceptor、Controller Advice、AOP与Spring Proxy | 根据处理位置选择共通机制，并画出请求进入业务方法的执行边界 |
| [B07 Spring Boot常见问题与排查索引](B07_troubleshooting_review_index.md) | 完成相关主线章节 | 构建、启动、HTTP、校验、数据库、事务、安全、部署的分阶段排查 | 从最早错误和可重复现象进入正确章节，提交可复核的调查记录 |

## 三、三条常用组合路线

### 1. 接口改修路线

```text
A05 JSON边界
  → A06 HTTP扩展输入 / A08校验分组 / A09-1时间语义
  → B01外部API 或 A03-1 HTML视图 或 A09-2 MyBatis配置
  → A12-1补充断言 / A12-2比较H2测试
```

只选择改修实际涉及的分支。例如新增CSV上传不需要同时学习HTML视图和独立MyBatis配置。

### 2. 既存项目调查路线

```text
B02建立调查顺序
  → B03确认版本与部署结构
  → B04识别Lombok/JPA
  → B05查陌生注解
  → A04或B06定位Bean与共通机制
```

调查成果应包含文件位置、依赖证据和调用关系。只写“使用了Spring Security”“这里有AOP”不能作为影响调查结论。

### 3. 发布与故障调查路线

```text
A11-1确认日志证据
  → A11-2区分请求处理时机
  → A17确认配置与产物
  → A18-1确认服务器运行状态
  → A18-2确认监控与迁移组件
  → B07按失败阶段汇总调查
```

生产环境操作必须以项目批准的手顺和权限为准。附录中的命令与目录用于教学和识读，不能自动成为实际项目的发布标准。

## 四、附录实验的统一边界

- 附录中的独立实验从已通过主线验收的工程副本开始。
- 修改前记录目标文件和开始状态；完成后验证结果，并按附录说明恢复或保留变更。
- 既存项目识读任务默认只读，不因看到旧写法就立即升级依赖或批量替换import。
- 凭据、Token、Session ID、CSRF令牌和生产连接信息不能写入源码、讲义示例或日志。
- 一项知识只在主要附录完整说明；其他附录通过链接回顾，避免复制形成互相矛盾的版本。

## 五、完成标准

完成当前任务需要的附录后，你应能说明为什么选择该附录、它依赖哪些主线知识，并提交至少一种可检查成果：运行结果、Postman证据、自动化测试、日志、配置来源调查、调用图、Review指摘或部署检查记录。

完成既存项目综合调查时，还应能从README和构建文件确认技术基线，找到启动和配置入口，沿一个URL追踪Controller、Service、Mapper/Repository/API Client，识别Security、Validation、Transaction和异常处理位置，并根据改修票整理確認事項、影響範囲、自测证据和改修报告。
