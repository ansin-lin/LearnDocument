# B02 如何阅读一个陌生的Spring项目

从零写过员工管理API，不代表拿到已有仓库就能安全改修。既存项目阅读的目标不是尽快看完所有文件，而是先确定技术基线和运行方式，再沿一个业务用例纵向追踪，最后调查会影响全局的共通机制。

前置：完成主线员工CRUD、事务、日志与测试，并能按[B01](B01_spring_common_processing_mechanisms.md)区分请求入口和方法代理。练习只读调查，不更改主线工程。

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

既存POM中至少要能识别这些能力线索，不要求背GroupId：Boot 4主线使用的 `spring-boot-starter-webmvc`、旧项目常见且在Boot 4中已弃用的 `spring-boot-starter-web`、`validation`、`security`、MyBatis Starter、`spring-boot-starter-data-jpa`、MySQL/PostgreSQL Driver、Lombok、`spring-boot-starter-test` 和 `spring-batch`。HTTP客户端可能由Spring Framework本身提供，也可能来自项目的其他依赖；要继续搜索Bean配置和实际调用，不能只看依赖名称下结论。

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

`@SpringBootApplication` 属于Spring Boot，写在启动类上。应用启动时它同时提供配置类、自动配置入口和默认组件扫描语义；扫描范围通常从启动类所在包向下，因此包位置会直接影响Controller、Service等Bean能否被发现。组合注解的三个核心组成和各自作用见第3章。

确认启动类所在包，因为默认组件扫描从该包向下进行。若找不到main方法，继续按[B04：日本既存Spring项目的新旧结构识读](B04_legacy_spring_project_reading.md)检查WAR、ServletInitializer、XML和外部Tomcat。不要因此断言项目“不能启动”。

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

检查Unit Test、`@WebMvcTest`、`@SpringBootTest`、Integration Test、test profile、测试数据、SQL和Mock。

- `@WebMvcTest` 属于Spring Boot Test，写在测试类上；测试上下文创建时只聚焦Spring MVC切片，通常需要为Controller依赖准备Mock。
- `@SpringBootTest` 属于Spring Boot Test，写在测试类上；测试启动时加载完整Boot应用上下文，适合验证多个层和配置的集成，不应替代全部快速单元测试。

测试可以帮助确认正常、异常和边界行为，但“现有测试没有覆盖”不等于“业务允许任意修改”；缺口要登记为待确认或补测范围。

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

## 十一、改修票 EMP-409：员工保存偶尔返回409

调查目标：在**一个陌生仓库副本**中找出“编辑员工后偶发409”的真实来源。下面是假设的既存项目线索，不能当作当前主线已实现的乐观锁：主线第13章关注员工更新与履历的事务；`version` 乐观锁的可复现实验在[A13-2](A13_02_concurrent_updates_locks.md)。先确认仓库确实有 `version` 列和对应SQL，若没有就不能套用下述结论。

1. 从浏览器Network或改修票记录 `PUT /employees/1001`、请求体、响应体、时间和requestId。确认409是应用返回，还是代理/外部服务返回；不复制个人信息到报告。
2. 搜索 `@PutMapping` 与 `/employees` 找到Controller映射，记录Request DTO的 `version` 是否必填、Validation是否在方法进入前发生。IDEA用 Search Everywhere/Find in Files，Eclipse用 Search → File/Java Search；均可在Controller方法设断点。
3. 从Controller方法使用 IDEA Find Usages/Call Hierarchy 或 Eclipse References/Call Hierarchy 找到Service实现；用 Go to Implementation/Implementation 或 Open Type Hierarchy 确认接口绑定。记录是否有 `@Transactional`、`@PreAuthorize`，以及同一Service是否被Batch或其他Controller调用。
4. 跟入Mapper接口及XML，核对 `UPDATE employees SET ..., version = version + 1 WHERE id = #{id} AND version = #{version}`。`#{}` 是参数绑定；不能把输入拼接成 `${}`。检查Mapper返回的影响行数：0行可能表示版本已变化、员工已删除或权限范围排除，不是天然等于“并发冲突”。
5. 从Service的行数分支追踪冲突异常，再查 `@RestControllerAdvice` 把它映射到409的位置。调试时在SQL执行前、行数判断处和Advice各设断点；用两次读取同一version、先后保存的**专用测试数据**复现，不在生产库写入。
6. 调查前端：保存失败时是否显示冲突提示、保留用户输入、刷新最新员工、禁止静默覆盖；搜索调用该URL的Vue/React API模块和页面。回归范围覆盖正常更新、冲突、员工不存在、权限拒绝、履历事务和前端提示。

只读命令行定位示例（在仓库根目录执行；Windows PowerShell可使用 `rg`，Linux/Shell项目可用 `grep -R`）：

```powershell
rg -n 'PutMapping|/employees|version|Optimistic|@Transactional|ExceptionHandler' docs/backend/springboot
```

这条命令只搜索教材目录，因此结果**不是某个实际应用仓库的证据**。在实际仓库应把范围改成已确认的应用模块，例如 `src/main/java`、`src/main/resources`、`src/test`；先看命中文件，再读上下文和调用方。若机器没有 `rg`，在Linux项目根目录可用 `grep -RInE 'PutMapping|version|Transactional' src/main`。不要把搜索结果的“没有命中”直接写成“功能不存在”，还可能是父模块、生成代码、XML或另一种命名。

### 已填写的调查报告示例

以下是**教学用假设仓库**的报告格式；行号需在接手的仓库重新核对，不能复制为真实结论。

| 项目 | 结论与证据状态 |
| --- | --- |
| 改修票 | EMP-409：员工编辑保存偶发409；已确认：请求为 `PUT /employees/1001`，响应含requestId `demo-409` |
| 技术基线 | 已确认：示例POM声明Java 17、Boot 4.1.1、MyBatis；真实仓库还需核对有效依赖树 |
| 运行方式 | 已确认：示例为Maven可执行JAR、test profile；生产Profile与外部配置尚未确认 |
| 调用链 | 已确认（示例代码）：Controller → EmployeeService.update → EmployeeMapper.updateByIdAndVersion → MySQL；失败由ConflictException → Advice映射409 |
| 影响文件 | 已确认（示例）：Controller/DTO、Service、Mapper接口/XML、Advice、Service/Mapper/Web测试；前端保存页面为推测影响，需搜索实际调用方 |
| 现行处理 | 已确认（示例）：SQL的 `id + version` 条件更新，0行抛冲突；同一事务还写履历 |
| 问题原因 | **推测**：两个编辑会话读取同一version，后提交者0行；还未排除员工删除、数据范围条件和请求重复提交 |
| 待确认事项 | 客户是否期望冲突后自动刷新、保留输入或允许覆盖；实际SQL是否有额外条件；生产日志是否有相同requestId |
| 测试范围 | 正常更新1行、旧version更新0行、员工删除、权限/数据范围、履历回滚、前端409提示与重试流程 |
| 残留风险 | 未拿到真实日志和SQL执行证据前，不把“乐观锁导致”定为根因；不能在生产环境复现写入 |

## 十二、只读调查安全边界与验收

### 独立调查作业：从EMP-409票走到改修影响表

在一份已获授权的陌生项目副本中，从README、POM、一个员工保存URL和一条脱敏409日志开始；先只读，不运行未知初始化脚本。提交一页调查报告与一张调用图，不要求阅读全部源码。报告必须标出Java/Boot/MyBatis的**声明版本与实际解析版本的区别**、启动命令和Profile、数据源配置来源（只写变量名，不写秘密）、Controller入口、Controller→Service→Mapper→SQL、事务边界、异常映射、权限检查及现有单元/Web/Mapper测试覆盖范围。把409根因写成“已确认/推测/待确认”，再列影响文件与回归用例。无法访问运行环境时明确标注未验证，不猜测生产配置。

教师参考答案以本章前面的**假设仓库**为准：`PUT /employees/{id}` → 更新Service → `updateByIdAndVersion` → SQL的 `id+version` 条件 → 0行分支 → 冲突异常 → Advice 409；事务应查Service公开入口，权限应查Security规则和方法注解，版本应核对有效POM及依赖树。`version` 并非主线工程默认字段，因此真实仓库若没有此列，必须改走业务状态冲突调查，不可照抄答案。评分：技术基线/启动/配置15分，业务调用链25分，共通机制（事务、异常、权限、测试）20分，根因与证据状态20分，影响范围与可执行回归方案20分。只写结论无路径/证据不得满分。

陌生仓库先确认环境、权限和数据范围。不要运行名称不明的初始化/迁移脚本、不要对生产数据库做写入实验、不要把密钥、连接串、客户数据粘进工单或公开聊天。即使只是构建，也先看插件是否会执行迁移、代码生成或外部上传。交付物为一张带证据来源的调用图、上表格式的调查报告、未确认事项和测试矩阵；Reviewer应能沿相同文件与日志复核每个“已确认”结论。

## 十三、阅读完成标准

你不需要记住全部代码，但应能用证据回答：项目怎样构建和启动、运行时配置来自哪里、改修用例经过哪些组件、数据写到哪里、失败怎样返回、谁有权限、哪些测试和日志可以验证。未知项要明确标成待确认，不用猜测补齐。
