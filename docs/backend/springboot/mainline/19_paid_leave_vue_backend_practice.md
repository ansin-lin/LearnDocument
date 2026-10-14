# 第19章 为Vue有給休暇系统实现后台API

本章是Spring Boot主线唯一的结业综合任务。前18章已经完成员工管理API；本章另建`paid-leave-api`工程，把学过的Spring Boot、MyBatis、MySQL、Validation、Session、事务、日志和自动化测试组合起来，为既有Vue项目提供可以实际联调的后台。

本章不重新讲一遍各项技术，也不要求继续扩展Employee Management。遇到实现问题时，应先回到前18章对应内容复习，再返回本任务完成交付。

## 一、先确认前后端共同规格

开始编码前必须阅读：

- [Vue第22章：用Vue重构有給休暇申請系统](../../../frontend/Vue/22_vue_project.md)
- [Vue第23章：SES改修与影响范围调查](../../../frontend/Vue/23_ses_change_practice.md)
- [有給休暇申請系统API入出力规格](../../../frontend/Vue/appendix_paid_leave_api.md)

三份前端资料与本章的关系如下：

| 资料 | 后台需要配合的内容 |
| --- | --- |
| Vue第22章 | 登录、注册、首页、申请输入、完成、一览、取消和退出的完整页面流程 |
| API附录 | URL、HTTP方法、字段名、代码值、状态码、错误码、Cookie和响应JSON |
| Vue第23章 | 为一览接口增加申请期间筛选，并完成影响调查和回归测试 |

发生理解冲突时，接口的外部行为以API附录为准，不根据Java类名、数据库列名或个人习惯擅自修改契约。前端资料中的`localhost:3000`是参考后台地址；改用Spring Boot后只把Vue的环境变量改为：

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

Vue的页面字段、路由和业务流程不因后台语言改变。后台内部可以使用不同的Java类名和表结构，但返回给Vue的URL、JSON和状态码必须一致。

## 二、开始状态与完成结果

开始前应具备：

- 已完成Spring Boot第1～18章；
- 已取得通过Vue第22章验收的前端工程；
- 本机MySQL和数据库客户端可以正常使用；
- Node.js参考后台只用于确认既有行为，不复制其内部实现。

完成后应能证明：

1. Spring Boot从Eclipse启动，`GET /api/health`返回`200`；
2. Vue只切换API基本地址即可完成注册到退出的完整流程；
3. 登录状态可跨请求恢复，退出后旧会话失效；
4. 用户只能查询、读取和取消自己的申请；
5. 受付番号生成与申请保存发生异常时能够一起回滚；
6. 第23章的日期条件能与状态、关键字共同筛选；
7. 自动化测试、Vue联调和数据库核对都有可复核证据。

## 三、固定技术和任务边界

使用Java 17、Spring Boot 4.1.x、Spring Web MVC、Validation、Spring Security、MyBatis Starter 4.1.0、MyBatis Test Starter 4.1.0、MySQL、BCrypt、JUnit、Mockito和MockMvc。代码继续按`Controller → Service → Mapper → MySQL`组织，请求DTO、响应DTO和Entity分开建立。

本任务不增加JWT、Redis、JPA、Docker、消息队列、微服务、审批人画面、邮件、前端分页或管理后台。不要为了展示技术而改变Vue接口。

本任务的会话外部行为必须满足：

- Cookie名为`paidLeaveSession`；
- Cookie启用`HttpOnly`，本地HTTP使用`SameSite=Lax`，生产环境还要启用`Secure`；
- 服务器Session只保存识别当前用户所需的内部ID，不保存密码和完整个人资料；
- 未登录调用`GET /api/auth/me`返回`200`和`data: null`，其他受保护接口返回`401`；
- 退出会使服务器Session和浏览器Cookie失效。

前端API附录以Node.js参考实现解释了`user_sessions`。Java版可以使用Servlet Session；如果要求重启后仍保留会话，再使用Spring Session JDBC持久化。两种方案都不能改变Vue看到的Cookie名和接口行为。Vue契约当前没有CSRF Token取得接口，因此本地研修版需要明确记录对`/api/**`采用的CSRF处理；不得假装`SameSite`已经等同于完整CSRF防护。正式项目上线前必须由前后端共同补充Token契约并恢复写请求防护。

## 四、数据库与数据边界

数据库名建议使用`paid_leave_training`，字符集使用`utf8mb4`。至少建立：

| 表 | 主要数据 | 关键约束 |
| --- | --- | --- |
| `departments` | 部门代码、显示名、显示顺序 | 部门代码唯一 |
| `users` | 社员番号、账号、密码哈希、姓名、部门、初始有给日数、有效状态 | 社员番号和账号唯一 |
| `leave_receipt_counters` | 受付日期、当日序号 | 受付日期唯一，用于并发生成番号 |
| `leave_applications` | 用户、休假种类、期间、日数、理由、引继、备注、状态和时间 | 受付番号唯一，用户和查询条件建立必要索引 |

部门初始代码固定为`development`、`quality`、`sales`、`general-affairs`、`human-resources`。用户必须通过注册API创建；初始化脚本不得保存真实密码或固定明文密码。

数据库列可以使用`snake_case`，Java使用`camelCase`。`passwordHash`、内部用户ID和会话数据不得出现在公开响应中。申请详情、一览和取消的SQL都必须带当前用户范围，不能先按ID查出他人数据后再交给前端隐藏。

## 五、必须实现的11个接口

以下只是任务索引，完整输入、输出和异常必须逐项对照API附录。

| ID | 方法与URL | 登录 | 主要完成条件 |
| --- | --- | --- | --- |
| API-01 | `GET /api/health` | 不需要 | 同时确认应用和数据库可用 |
| API-02 | `GET /api/master/departments` | 不需要 | 按显示顺序返回`value`与`label` |
| API-03 | `POST /api/users` | 不需要 | 校验、BCrypt、重复账号和社员番号生成 |
| API-04 | `POST /api/auth/login` | 不需要 | 统一认证失败消息，建立Session和Cookie |
| API-05 | `GET /api/auth/me` | 不需要 | 已登录返回用户；未登录返回`data: null` |
| API-06 | `POST /api/auth/logout` | 需要 | 销毁Session、清除Cookie并返回`204` |
| API-07 | `GET /api/dashboard` | 需要 | 返回本人资料、余额和申请汇总 |
| API-08 | `GET /api/leave-applications` | 需要 | 本人数据，支持组合查询和稳定排序 |
| API-09 | `GET /api/leave-applications/{id}` | 需要 | 只允许取得本人申请 |
| API-10 | `POST /api/leave-applications` | 需要 | 完整校验、日数计算、事务和受付番号生成 |
| API-11 | `PATCH /api/leave-applications/{id}/cancel` | 需要 | 只把本人的`pending`记录改为`cancelled` |

成功结果放在`data`中，一览另返回`meta.count`，退出成功没有响应体。失败结果放在`error`中；字段校验错误还要返回`details`。不能把Java堆栈、SQL、密码、Cookie或内部类名返回给前端。

## 六、按可运行阶段实施

每完成一个阶段，都先从Eclipse启动服务，用Postman单独验证，再连接Vue。不要把11个接口全部写完后才第一次运行。

### 阶段1：工程、配置和健康检查

创建独立工程，配置`8080`端口、MySQL、MyBatis XML位置和日本业务时区。先完成API-01，并区分应用未启动、数据库不可用和正常连接三种结果。保存一条带requestId的正常日志和一条数据库故障日志。

### 阶段2：部门与用户注册

建立四张业务表、Entity、Mapper接口和XML。完成API-02和API-03。账号保存前去除两端空白并转为小写；密码只保存BCrypt哈希；密码确认值不进入Entity。注册时生成`EMP-00001`格式社员番号，并用数据库唯一约束处理并发重复。

### 阶段3：登录、会话恢复和退出

完成API-04～API-06。认证失败统一返回`401 INVALID_CREDENTIALS`，不能暴露账号是否存在。登录后用同一Postman Cookie Jar连续请求`/auth/me`；再由Vue刷新页面验证会话恢复。退出后重送旧Cookie也不能恢复身份。

### 阶段4：首页汇总

完成API-07。没有申请时返回剩余日数`12`、申请中`0`、本月批准日数`0`。只有`approved`且类型为`paid`、`half-am`或`half-pm`的记录扣减余额；`special`不扣余额。本月判断使用日本业务日期。

### 阶段5：申请新增和详情

完成API-09和API-10。校验日期、半日休假、特別休暇备注、理由、引继状态和剩余日数。`userId`、`leaveDays`、`receiptNumber`、`status`和提交时间由后台决定，不接收前端伪造值。

锁定当日计数器，生成`REQ-YYYYMMDD-NNN`后保存申请；两次写入处于同一事务，任何一步失败都回滚。为配合培训画面，新增状态从`pending`、`approved`、`returned`中随机产生；自动化测试不能依赖随机结果，应把状态决定器抽成可替换对象或固定测试桩。

### 阶段6：申请一览与取消

先完成Vue第22章需要的API-08基础版：支持`status`和`keyword`组合条件，关键字匹配受付番号或理由，结果按提交时间、主键稳定倒序。再完成API-11；取消SQL同时限制申请ID、当前用户ID和`pending`状态，不物理删除记录。

此阶段通过后，Vue第22章的注册→登录→首页→申请→完成→一览→取消→退出流程应全部可用。

### 阶段7：实施Vue第23章日期筛选改修

不要一开始就把日期条件混入基础版。先保存第22章通过时的测试结果，再按改修票为API-08增加`startDate`和`endDate`：

| 输入 | 后台查询规则 |
| --- | --- |
| 两项都省略 | 结果与改修前一致 |
| 只有`startDate` | `application.end_date >= startDate` |
| 只有`endDate` | `application.start_date <= endDate` |
| 两项都有 | 同时满足以上两项，即申请期间与查询期间重叠 |
| `endDate < startDate` | 返回`400 VALIDATION_ERROR`，不执行查询 |

日期条件还必须与`status`、`keyword`和当前用户范围使用AND组合。不要把用户输入直接拼进SQL；使用MyBatis条件判断和`#{}`参数。

改修前提交影响调查，至少确认Controller绑定、查询DTO、日期组合校验、Service、Mapper接口、Mapper XML、索引、自动化测试、Vue请求参数和既有筛选回归。改修后完成Vue第23章CHG-01～CHG-07，并记录实际修改文件与未修改理由。

## 七、日志与安全验收重点

每个请求使用requestId串联入口、Service和异常日志。日志至少能回答“哪个请求、哪个接口、哪个处理阶段、结果是什么”，但不得记录密码、密码哈希、完整Cookie、Session ID、完整姓名、完整理由或完整备注。

重点验证：

- 登录失败只记录账号的脱敏标识和失败类型；
- 事务失败能从同一requestId定位到受付番号生成或申请保存阶段；
- `401`、`404`、`409`和`500`来源可以区分；
- 访问他人申请统一返回`404 APPLICATION_NOT_FOUND`，不泄露记录是否存在；
- CORS只允许实际Vue开发地址并允许凭据，不能用`*`与Cookie组合；
- 响应和日志都不包含密码、哈希、Cookie、Session ID和SQL堆栈。

## 八、最低自动化测试

| ID | 场景 | 必须断言的结果 |
| --- | --- | --- |
| SB-01 | 健康检查 | `200`和`data.status=ok` |
| SB-02 | 正常注册 | `201`，数据库只保存密码哈希 |
| SB-03 | 重复账号 | `409`，用户件数不增加 |
| SB-04 | 登录后再次调用`me` | 同一Session恢复相同用户 |
| SB-05 | 错误密码 | `401 INVALID_CREDENTIALS`，日志无密码 |
| SB-06 | 退出后访问受保护接口 | `401`，旧Session失效 |
| SB-07 | 无申请首页 | 返回`12、0、0` |
| SB-08 | 半休跨日 | `400`，数据库不新增 |
| SB-09 | 开始日早于日本当天 | `400 START_DATE_IN_PAST`，数据库不新增 |
| SB-10 | 余额不足 | `409 INSUFFICIENT_LEAVE_BALANCE` |
| SB-11 | 连续新增 | 受付番号唯一且顺序递增 |
| SB-12 | 新增中途异常 | 申请和计数器一起回滚 |
| SB-13 | 状态和关键字组合 | 只返回同时满足条件的本人数据 |
| SB-14 | 访问他人申请 | `404`，响应不泄露所有者 |
| SB-15 | 取消本人`pending`申请 | 更新为`cancelled`并保存时间 |
| SB-16 | 取消非`pending`申请 | `409 APPLICATION_NOT_PENDING`，数据不变 |
| SB-17 | 日期条件为空 | 结果与改修前一致 |
| SB-18 | 单侧日期 | 分别验证开始日和结束日边界 |
| SB-19 | 完整日期期间 | 返回边界相交的记录，不返回不相交记录 |
| SB-20 | 日期逆转 | `400`且Mapper查询未执行 |
| SB-21 | 日期、状态、关键字组合 | 全部条件与当前用户范围同时生效 |

Service测试用于验证业务分支和事务协作，MockMvc测试用于验证URL、JSON、状态码、Cookie和权限，MyBatis集成测试用于验证动态SQL、用户范围和日期边界。测试不仅看HTTP状态，还要在写操作前后查询数据库件数与内容。

## 九、Vue联调与证据

先用Postman确认单个后台接口，再按真实页面顺序联调：

```text
健康检查
  → 注册
  → 登录与刷新恢复
  → 首页汇总
  → 输入并确认申请
  → 完成页刷新
  → 状态和关键字筛选
  → 日期期间筛选
  → 取消pending申请
  → 退出并验证旧会话失效
```

每个接口证据至少记录HTTP方法、URL、Path/Query参数、请求体、预期状态码、实际状态码、关键响应字段和数据库确认结果。Cookie、密码和个人信息必须遮蔽。Vue联调还要用浏览器Network面板确认实际请求参数、Cookie携带、错误响应以及没有重复提交。

## 十、提交物和完成判定

提交物包括：

- Spring Boot源码、`pom.xml`和必要配置模板；
- 建库、建表、索引和部门主数据脚本；
- 自动化测试代码与测试结果；
- README、配置项清单和Eclipse启动手顺；
- API-01～API-11的请求响应与数据库证据；
- Vue第22章全流程联调记录；
- Vue第23章影响调查、CHG-01～CHG-07结果和回归记录；
- 至少一条使用requestId完成的真实排错记录；
- 已知限制、CSRF差异和生产环境待办；
- Maven构建成功及可执行JAR启动证据。

只有当另一名学员根据README可以建立数据库、启动后台、连接Vue并复现主要测试时，才算完成。教师参考代码用于讲解和验收，不作为学员起始代码；学员应根据本任务书和API规格独立实现。
