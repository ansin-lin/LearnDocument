# Appendix B：如何阅读一个陌生的Spring项目

从零写过员工管理API，不代表拿到已有仓库就能安全改修。既存项目阅读的目标不是尽快看完所有文件，而是先确定技术基线和运行方式，再沿一个业务用例纵向追踪，最后调查会影响全局的共通机制。

## 一、开始前先明确调查成果

收到Repository后先看README、构建文件和目录结构，再写清楚要回答的问题：这是什么项目、怎样构建启动、入口URL在哪里、请求数据怎样转换、业务规则在哪一层、数据或外部系统在哪里访问、有哪些权限和事务、哪些测试能证明没有回归。调查产出至少包括调用链、配置来源、影响文件、待确认事项和测试范围。

不要一开始就按目录读完所有Controller，再读所有Service。那样容易失去一个业务行为前后状态的联系。

## 二、Step 1：先看README、构建入口和目录

不要先随机打开一个Java文件。先阅读README、仓库根目录和模块目录，找出构建命令、启动方式、必要中间件、环境准备和主要模块。README可能过期，因此后续必须用构建文件、配置和实际结果交叉确认，不能只照抄说明。

先回答：这是单体还是多模块项目、Java几、Spring几、使用Maven还是Gradle、主要框架是什么、怎样构建和启动。无法确认的项目不要直接执行生产脚本或数据库写入。

## 三、Step 2：分析构建文件与依赖

Maven项目先看 `pom.xml`，Gradle项目先看 `build.gradle` 或 `build.gradle.kts`。确认：

| 调查项 | 常见线索 | 要得到的结论 |
| --- | --- | --- |
| Java版本 | `maven.compiler.release`、toolchain | 本机和CI需要哪个JDK |
| Spring版本 | Boot parent、BOM、plugin | 是Boot 2、3还是其他组合 |
| Web和安全 | web、security starter | MVC接口和认证组件是否存在 |
| 数据访问 | MyBatis、JPA、JDBC依赖 | SQL中心还是ORM中心 |
| 数据库 | MySQL等driver | 运行时连接什么数据库 |
| 辅助工具 | Lombok、MapStruct | 哪些代码会在编译期生成 |
| 共通任务 | Batch、Scheduler、AOP | 是否存在批处理、定时和横切处理 |
| HTTP客户端 | RestClient相关代码、其他库 | 怎样调用外部系统 |
| 测试 | test、security-test、Testcontainers | 可执行的回归入口是什么 |

同时查看模块列表、私有仓库地址、dependency management和构建profile。不能仅凭源码中的import猜版本；有效依赖还可能来自父POM或BOM。保存依赖树时注意不要公开私有仓库凭据。

既存POM中至少要能识别这些能力线索，不要求背GroupId：`spring-boot-starter-web`、`validation`、`security`、MyBatis Starter、`spring-boot-starter-data-jpa`、MySQL/PostgreSQL Driver、Lombok、`spring-boot-starter-test` 和 `spring-batch`。HTTP客户端可能由Spring Framework本身提供，也可能来自项目的其他依赖；要继续搜索Bean配置和实际调用，不能只看依赖名称下结论。

## 四、Step 3：寻找启动入口和部署方式

现代Boot项目优先搜索：

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

确认启动类所在包，因为默认组件扫描从该包向下进行。若找不到main方法，继续按[Appendix C：日本既存Spring项目的新旧结构识读](A03_legacy_spring_project_reading.md)检查WAR、ServletInitializer、XML和外部Tomcat。不要因此断言项目“不能启动”。

## 五、Step 4：确认配置从哪里来

依次查看：

```text
application.yml / application.properties
application-dev.yml / application-test.yml / application-prod.yml
环境变量与启动参数
部署脚本、systemd或容器配置
```

调查active profile、数据库、外部API地址、端口、日志、文件目录、超时和密钥来源。环境变量和命令行参数可能覆盖文件值，因此“配置文件写了什么”不一定等于“运行时最终是什么”。不要把真实密码、Token或连接串复制进调查报告。

## 六、Step 5：沿一个业务用例纵向追踪

从改修票中的URL、画面操作或批处理名开始：

```text
URL / Job
  ↓
Controller或任务入口
  ↓
Request DTO与Validation
  ↓
Service与事务
  ↓
Mapper / Repository / API Client
  ↓
DB / External System
  ↓
Response DTO或输出文件
```

每走一步记录方法名、输入输出、异常去向和测试。搜索接口实现、Mapper XML、配置属性和调用方，不要只看当前Java文件。若Service同时访问数据库和外部API，还要确认失败时哪些动作可以回滚、哪些不可以。

## 七、Step 6：调查横向共通机制

理解业务链后，再检查：

- `ControllerAdvice`、`ExceptionHandler`：异常怎样转成响应；
- Security配置和方法注解：谁能调用；
- Filter、Interceptor：请求前后是否被记录或拦截；
- AOP、Transaction：Bean方法外是否存在代理处理；
- Validation和JSON配置：数据进入方法前怎样校验、转换；
- Scheduler、Batch：同一Service是否还有非HTTP调用方；
- 外部API Client：超时、异常转换和敏感日志规则；
- Tests：当前行为和回归范围怎样被证明。

第11章给出了这些机制在请求路径中的课程级简化位置。共通机制可能改变所有接口，因此必须在影响调查中单列。

## 八、Step 7：用测试确认既有规格

检查Unit Test、`@WebMvcTest`、`@SpringBootTest`、Integration Test、test profile、测试数据、SQL和Mock。测试可以帮助确认正常、异常和边界行为，但“现有测试没有覆盖”不等于“业务允许任意修改”；缺口要登记为待确认或补测范围。

## 九、Step 8：建立业务调用图

完成阅读后至少画出：

```text
Request
  ↓ Filter / Security
Controller
  ↓ Validation
Service  ← Transaction / Method Security
  ├── Mapper
  ├── Repository
  └── API Client
       ↓
Database / External System
  ↓
Exception Handling → Response
```

标出Validation、权限、事务和异常转换在哪个位置生效。该图是当前调查结论，不是所有Spring项目绝对相同的源码顺序。

## 十、一次改修调查模板

```text
改修票：
现行技术基线：Java / Spring / 数据访问 / 打包方式
启动与Profile：
业务调用链：入口 → DTO → Service → DB或外部系统 → 响应
横向机制：Security / Validation / Exception / Filter / AOP / Transaction
影响文件：
待确认事项：
正常、异常、边界和权限测试：
回归范围：
残留风险：
```

练习时任选一个未读过的Spring仓库，只做只读调查。提交构建基线、启动方式、一条纵向调用链、一张横向机制表和待确认事项；在能说明现行行为前不要开始修改。

## 十一、阅读完成标准

你不需要记住全部代码，但应能用证据回答：项目怎样构建和启动、运行时配置来自哪里、改修用例经过哪些组件、数据写到哪里、失败怎样返回、谁有权限、哪些测试和日志可以验证。未知项要明确标成待确认，不用猜测补齐。
