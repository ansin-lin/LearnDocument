# B06 Spring Boot常见问题与排查索引

本索引用于员工管理API完成主线后的快速定位。先记录请求、时间、环境、release-id、requestId和最早错误，再进入对应章节；不要同时修改多个层，也不要用反复重启代替原因调查。

前置：主线日志、测试、事务与权限，以及[B01](B01_spring_common_processing_mechanisms.md)～[B05](B05_external_api_restclient.md)中与当前故障有关的机制。下方日志均为**教学模拟**，不能冒充真实生产证据。

## 一、先判断故障发生在哪个阶段

```text
构建前就失败
  → JDK、Maven Wrapper、依赖、编译或测试

JAR启动失败
  → Profile、配置、Bean、端口、数据库或Mapper

请求没有到Controller
  → URL、HTTP方法、绑定、校验或Security Filter

Controller已进入但业务失败
  → Service规则、权限、事务、Mapper或数据库

本机成功而服务器失败
  → 产物、外部配置、服务账号、目录、端口、代理或数据库网络
```

## 二、构建与启动

| 现象 | 先查 | 常见原因 | 主要章节 |
| --- | --- | --- | --- |
| `java`或Wrapper不可用 | `java -version`、Wrapper文件 | JDK版本、目录或执行方式错误 | 第3、17章 |
| 编译失败 | 第一条编译错误和对应文件 | 类型、导入、方法签名不一致 | 相关实现章 |
| 测试失败 | 失败测试、Profile和测试数据 | 代码、断言、配置或隔离错误 | 第12章 |
| JAR没有BOOT-INF | Maven Plugin和本次构建日志 | 不是Spring Boot可执行JAR | 第17章 |
| 端口已占用 | 监听端口和目标进程 | 旧实例或其他程序仍在运行 | 第17～18章 |
| Bean找不到 | 启动类包、组件注解、构造器 | 扫描范围或注册方式错误 | 第3～4章 |
| 配置绑定失败 | active Profile和缺失键 | 占位符、名称或值格式错误 | 第17章 |
| Mapper XML找不到 | mapper-locations和归档内容 | 路径、namespace或资源打包错误 | 第9章 |

## 三、HTTP、校验与异常

| 状态或现象 | 先查 | 判断方向 | 主要章节 |
| --- | --- | --- | --- |
| 404 | HTTP方法、URL、路径变量 | 映射不存在或资源不存在 | 第6～7章 |
| 400 | 请求头、JSON、查询参数、错误字段 | 绑定失败、字段校验或业务规则 | 第6～8、14、19章 |
| 401 | Cookie、Session、登录结果 | 没有可用认证或会话已失效 | 第15～16章 |
| 403 | CSRF、角色、数据范围 | 已登录但请求不被允许 | 第15～16章 |
| 409 | 唯一键和当前数据库状态 | 邮箱等资源冲突 | 第7、10章 |
| 500 | requestId和最早异常 | Service、Mapper、数据库或未处理异常 | 第11章 |
| 返回字段多或少 | Response DTO和转换方法 | 请求、Entity与响应边界混用 | 第5、9章 |

## 四、数据库、动态SQL与事务

| 现象 | 先查 | 常见原因 | 主要章节 |
| --- | --- | --- | --- |
| 查询为空 | 当前数据库、测试数据、参数和SQL | 环境或条件不一致 | 第9、14章 |
| items正确但total错误 | search与count生成条件 | 两条SQL条件不同 | 第14、19章 |
| SQL出现IN () | 规范化结果和foreach外层if | 空集合仍生成条件 | 第14章 |
| 插入失败 | 非空、长度、唯一约束 | DTO与表定义不一致 | 第8～10章 |
| 事务没有回滚 | Service入口、异常传播 | 异常被吞掉或事务入口错误 | 第13章、A13 |
| 员工更新但无履历 | 两个Mapper结果和事务日志 | 第二次写入失败或未检查行数 | 第13章、A13 |
| 后提交的更新覆盖先提交结果 | UPDATE条件、version和影响行数 | 缺少乐观锁、原子UPDATE或锁定读 | A13 |
| 请求长时间等待或偶发死锁 | 长事务、索引、加锁顺序和数据库死锁信息 | 锁范围过大、访问顺序相反或事务内有慢操作 | A13 |

### 外部HR API联携故障索引

| 现象 | 先查 | 常见原因 | 主要章节 |
| --- | --- | --- | --- |
| 连接超时 | 目标地址、端口、建连耗时、网络策略 | DNS/路由/防火墙或服务不可达 | [B05](B05_external_api_restclient.md)、第17～18章 |
| 读取超时 | 已建连证据、HR耗时、读取超时配置 | HR处理慢、响应迟迟未完成 | B05 |
| DNS失败 | 主机名、解析结果、运行环境 | 拼写、DNS配置或环境差异 | B05、第18章 |
| 外部401/403 | 本系统HR Token来源与权限 | Token缺失、失效或HR权限不足；不等于当前用户未登录 | B05、第17章 |
| 外部404 | 本地员工是否存在、HR同步状态 | HR未同步或HR路径错误；不自动映射本系统404 | B05 |
| 外部500 | HR状态码、requestId、脱敏耗时 | 依赖系统故障或契约变化 | B05 |
| JSON不一致 | 外部DTO、脱敏响应样本、Content-Type | 字段改名、类型变化、空正文 | B05、A05 |
| Token配置错误 | active Profile、环境变量来源 | 部署未注入、过期或误用环境 | B05、第17章 |

直接执行SQL时只连接获批环境，先用 `SELECT DATABASE()` 确认目标。写操作限定主键和影响范围，不在生产环境用练习语句试错。

## 五、登录、权限与安全

| 现象 | 先查 | 常见原因 | 主要章节 |
| --- | --- | --- | --- |
| 登录200但me为401 | 是否保存SecurityContext和Cookie | 会话未持久化或客户端未保留Cookie | 第15章 |
| 正确密码也失败 | 数据库哈希、PasswordEncoder和enabled | 明文、算法不一致或账号停用 | 第15章 |
| POST总是403 | CSRF Cookie、token和headerName | 缺少或混用了CSRF令牌 | 第15章 |
| USER能访问列表 | SecurityConfig与权限测试 | URL规则被放宽 | 第16章 |
| USER能查看他人 | EmployeeAccess使用的当前身份 | 信任了请求中的用户编号 | 第16章 |
| 日志出现密码或Cookie | 请求体、Header和异常日志 | 记录范围过大 | 第11、15章 |

认证回答“当前是谁”，授权回答“能做什么、能访问哪些记录”，CSRF防止浏览器携带Cookie时被跨站利用；三者不能互相替代。

## 六、部署与运行

| 现象 | 先查 | 常见原因 | 主要章节 |
| --- | --- | --- | --- |
| systemd unit not found | 单元路径和daemon-reload | 文件未安装或未重新加载 | 第18章 |
| status为203/EXEC | ExecStart和Java绝对路径 | 文件不存在或不可执行 | 第18章 |
| Permission denied | User、Group和逐层目录权限 | 父目录不可进入或日志不可写 | 第18章 |
| active但health失败 | MainPID、监听地址和端口 | 进程存在但Web入口未就绪 | 第18章 |
| health成功但业务500 | 正式入口、数据库和requestId | health不访问数据库 | 第11、18章 |
| 浏览器登录后无Session | HTTPS、Secure Cookie和代理 | 从HTTP访问或代理边界错误 | 第15、18章 |
| 回退JAR仍失败 | 数据库结构和旧版本兼容性 | 普通JAR回退不足 | 第18章 |

## 七、三个带证据的故障调查

### 学员题目与交付

在授权的测试副本中任选下表三票，**先不要看下面的教师解析**。给学员的都是教学模拟证据，不是当前仓库的实测记录；测试数据和运行权限须由指导者准备。每票提交六段调查链：现象→证据→调查路径→根因（或假设）→修正方案→回归测试，并把每条结论标成已确认、推测或待确认。

| 票 | 给学员的现象与日志 | 必须核实的事实 |
| --- | --- | --- |
| START-BEAN | 应用未启动；`No qualifying bean of type 'HrApiClient'` | 扫描边界、Bean注册条件、最早`Caused by` |
| EMP-500 | `GET /employees/1001` 为500；`Unknown column 'department'`、`EmployeeMapper.findById` | 实际连接库、schema、Mapper SQL与迁移状态 |
| EMP-409 | 两次更新都带`version=3`；第一次UPDATE 1行、第二次0行/409 | 实际是否有version机制、员工是否被删除、权限范围条件 |

提交物另含脱敏requestId/启动日志、目标文件路径、待确认事项、最小安全验证步骤及回退方案。评分每票100分：现象与证据20，调查路径20，事实/假设/待确认区分20，修正与风险20，回归测试和可复核交接20；仅猜异常名最多得证据项的部分分。下面三个案例的“根因/修正”是**该教学设定的教师参考答案**，真实现场未核实前不得抄作已确认。

### 案例1：启动时找不到Bean

**现象与复现**：在专用副本把 `HrApiClient` 移到启动类扫描包之外，执行 `./mvnw.cmd spring-boot:run` 后应用未启动。模拟日志：`UnsatisfiedDependencyException: Error creating bean with name 'employeeService' ... No qualifying bean of type 'HrApiClient'`。HTTP请求尚未进入，改Controller路由无用。

**调查**：从第一条 `Caused by` 找缺失类型，检查 `@Component`、包名、启动类位置、`@ComponentScan` 和Profile条件；对照[B01](B01_spring_common_processing_mechanisms.md)理解这是容器装配而非Filter问题。判断依据是容器启动失败且找不到Client Bean定义。**根因**（此实验）是扫描范围之外。**修正**：把类放回主应用包之下，或按设计显式注册Bean；不要为了让错误消失而新建一个无配置的假Client。**回归**：启动成功、执行Client和Service测试、确认正式Profile没有相同遗漏。实验后恢复包位置。

### 案例2：API 500与Mapper SQL

**现象与复现**：专用测试库故意让 `employees` 表缺少SQL引用的 `department` 列，`GET /employees/1001` 返回500，响应头 `X-Request-Id: demo-500`。模拟日志：`[demo-500] request_complete ... status=500`；原因链 `BadSqlGrammarException → SQLSyntaxErrorException: Unknown column 'department' in 'field list'`，Mapper语句指向 `EmployeeMapper.findById`。

**调查**：按requestId关联Filter完成日志与最早异常；确认Controller进入后调用Service，再到Mapper XML的SELECT；只在测试库用只读 `SHOW COLUMNS FROM employees` 核对表结构，检查迁移记录、active Profile和实际连接库。依据是SQL列名与测试库schema不一致，不能只写“Mapper错误”。**根因**（此实验）是测试schema未按当前SQL迁移。**修正**：按项目受控迁移流程补齐测试schema，或若规格要求旧列名则修改映射；不要临时吞异常返回空员工。**回归**：正常查询、找不到员工404、Mapper集成测试以及同schema部署环境检查。不要在生产库执行教学DDL。

### 案例3：员工更新冲突409

**现象与复现**：在采用[A13-2](A13_02_concurrent_updates_locks.md)乐观锁的**实验副本**中，两次读取员工1001都取得 `version=3`；A先保存成功，B使用旧version提交，响应409。模拟证据：`UPDATE employees SET ..., version=version+1 WHERE id=1001 AND version=3`，A影响1行，B影响0行。主线第13章不默认有version列，必须先确认当前仓库SQL。

**调查**：沿[B02](B02_existing_project_reading.md)路径检查前端请求version、Controller DTO、Service事务、Mapper SQL条件、影响行数判断和Advice；用只读SELECT确认当前version已变4，排除员工被删除或权限范围条件。**根因**（此实验）是并发编辑造成旧version被拒绝，这个409是防止覆盖的正确行为，不一定是代码故障。**修正**：按设计书决定提示刷新、保留输入和重试方式；不要删除version条件或自动无条件覆盖。**回归**：正常版本更新1行、旧版本0行且409、员工不存在404、履历与主更新同事务、前端冲突提示。若真实生产证据不足，只能写“推测”，不能套用实验根因。

## 八、Review时按职责追踪

```text
请求格式和字段校验 → Request DTO、Controller
业务规则和事务边界 → Service
SQL与参数绑定       → Mapper接口、XML
数据库列与约束       → schema、Entity
公开字段和状态码     → Response DTO、Controller、异常处理
登录和权限          → Security、Service方法检查
配置和启动          → Profile、外部配置、JAR、systemd
证据                → 自动化测试、接口记录、日志和数据库状态
```

Review不能只看改变的几行。至少核对职责、异常、权限、SQL注入、兼容性、回归范围、配置和证据；指摘修正后重新执行受影响测试和必要的全量测试。

逐项签核：Controller与Service分层、API方法/URL/状态码、Request/Response DTO和个人信息边界、Validation与业务规则、Mapper参数绑定和索引、事务与回滚、并发version/影响行数、Session/方法权限/数据范围、异常映射、requestId和敏感日志、Profile/Token/超时配置、单元/Web/集成测试及前端与批处理调用方的回归范围。每项附文件、测试或日志证据；“已看过”不是通过证据。

## 九、调查报告范例与术语

**障害調査**（故障调查）票：`EMP-409`，测试环境，员工1001保存冲突。**原因調査**（原因调查）：已确认两次请求均携带version=3；第一次UPDATE影响1行、第二次0行，当前version=4；生产环境是否同因尚未确认。**影響範囲**（影响范围）：员工编辑页面、更新Service、Mapper、冲突Advice；读取接口未发现异常，但仍须回归。**暫定対応**（临时处置）：告知用户刷新最新数据并保留原输入，不关闭乐观锁。**恒久対応**（永久措施）：按规格补充前端冲突提示与二次确认，不做无条件重试。**再発防止**（防止重复发生）：补并发更新自动测试和Review检查。**確認事項**（待确认）：客户是否允许覆盖、是否需要差异合并。**横展開**（横向排查）：搜索其他更新接口是否也依赖version却没有0行处理。报告附requestId、SQL脱敏摘要、测试用例与执行时间；推测不能写成已确认。

## 十、综合考核

故障票：`EMP-HR-27`，“编辑页偶发显示员工状态不可用，同时保存后出现409”。给学员的模拟证据：`GET /lab/employees/1001/hr-status` 返回503，requestId `r-27`；应用日志同编号的根因 `SocketTimeoutException: read timed out`；随后 `PUT /employees/1001` 携带 `version=8` 返回409，Mapper日志 `UPDATE ... WHERE id=1001 AND version=8` 影响0行，数据库当前version=9。任务：分别画出HR读取链与本地更新链；区分已确认、推测、未确认；写出临时处置、恒久修正候选、影响文件与权限/前端/数据库/HR回归范围；给出至少两条可执行测试及预期结果。验收：不能把HR超时推断为本地409原因，不能让外部404或超时映射成员工不存在，也不能用删除version条件“修好”冲突；调查报告必须能由另一个开发者用同一证据复核。

教师参考：已确认的是两条**不同调用链**分别返回503和409；模拟日志支持HR读取超时与版本条件0行，不能证明超时导致冲突。推测是HR处理变慢、另一编辑者先更新；待确认的是外部耗时分布、实际更新者、权限/删除条件及客户冲突处置规格。临时处置应保留输入并提示刷新，不移除version条件；永久方案要先得到设计书确认。至少用“延迟HR响应→503且cause可追”和“旧version→0行/409且不覆盖新值”两项测试回归。综合评分：证据及链路30，事实/推测/待确认20，修正和风险20，可执行测试20，交接完整性10。若只写‘数据库问题’或‘重试即可’，不能判通过。

## 十一、仍无法定位时怎样记录

提交调查记录时包含：

- 环境、release-id、提交和发生时间；
- 请求方法、URL、HTTP状态和requestId；
- 影响用户与功能范围；
- 第一处异常及必要上下文；
- 已确认无问题的层及证据；
- 临时处置、恢复结果和是否回退；
- 尚未确认事项、负责人和期限。

“重启后恢复”“数据库问题”“代码有错”都不是充分根因。无法复现时也要保留当时状态和证据，不能把未查明写成已解决。

## 十二、完成主线后的能力自查

你应能够独立完成：

1. 从空目录创建并运行Spring Boot工程。
2. 解释Controller、Service、Mapper与数据库的调用链。
3. 区分Request DTO、Entity和Response。
4. 实现请求绑定、校验、统一响应和异常。
5. 使用MyBatis完成参数化CRUD、动态查询和分页。
6. 用事务保护员工更新与履历写入。
7. 使用日志和requestId定位500。
8. 编写Service、Web和数据库测试并选择回归范围。
9. 实现Session登录、CSRF、角色和数据范围。
10. 管理多环境配置，构建可执行JAR。
11. 在Linux受控部署、检查、停止和回退。
12. 按改修票完成调查、Review、自测和报告。
13. 制作可由他人独立重建和验收的交付包。

某项不能独立完成时，回到表中对应章节重新完成操作和证据，不只背诵注解名称。
