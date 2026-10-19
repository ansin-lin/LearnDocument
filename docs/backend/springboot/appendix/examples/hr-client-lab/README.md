# B05 隔离实验工程

这是可编译的 Spring Boot 4.1.1 / Java 17 目标实验，不是 Employee Management API 的改修。运行 Maven 的 JDK 可以高于 17；编译目标由 `pom.xml` 的 `java.version=17` 固定。实验把教材中的 HR Client、配置、DTO、Service、Controller、Advice 和 Security 组合在 `com.example.hrlab.external` 包中；原教材提供集成主线时使用的正式包路径。

在本目录执行 `mvn test`。自动化测试不需要 MySQL 或真实 HR：Client 用 `MockRestServiceServer`，Service 用 Mockito Mapper，Controller/Security 用 MockMvc；读取超时测试只绑定本机 `127.0.0.1` 的随机端口，结束时关闭服务器。`mvn package` 可构建可执行JAR；若要手动启动，先准备本机模拟 HR 服务，并通过 `HR_API_BASE_URL`、`HR_API_TOKEN` 配置，再运行 `java -jar target/hr-client-lab-1.0-SNAPSHOT.jar`。配置默认值 `lab-placeholder` 仅供本机实验，绝不能用于生产。手动运行后停止进程即可清理；没有数据库写入。

实验 Mapper 是只读内存替身（仅员工 1001），用于验证 Service 的调用顺序与不写库策略；**没有验证主线 MyBatis SQL、真实 HR 凭据、生产 Security 规则或跨系统事务**。正式集成时应使用 B05 正文的 Employee 实体/Mapper，并按主线的 Security 和全局异常约定审查合并。

2026-10-19 执行 `mvn test`：3 个测试类、22 个测试、22 通过、0 失败、0 错误、0 跳过；`mvn -q -DskipTests package` 成功，`java -jar target/hr-client-lab-1.0-SNAPSHOT.jar --server.port=0` 成功启动Tomcat后手动停止。Maven 运行 JDK 21.0.10，javac 以 `--release 17` 编译，Spring Boot 启动日志为 4.1.1，依赖树解析 `spring-core` 为 7.0.9。真实延迟响应测试观察到 `HrApiConnectionException → ResourceAccessException → HttpTimeoutException`；连接超时受操作系统/网络路径影响，本实验未声称稳定复现。
