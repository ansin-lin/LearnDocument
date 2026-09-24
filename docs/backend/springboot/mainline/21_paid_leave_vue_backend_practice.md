# 第21章 结业综合练习：为Vue有給休暇系统实现后台API

本章是Spring Boot主线学完后的独立综合练习。知识讲解仍以第1～20章为准，本章不增加新的框架和业务范围。你需要使用已经学过的Spring Boot、MyBatis、MySQL、Validation、Session、事务、日志和自动化测试，替换课程提供的Node.js参考后台，让既有Vue页面可以直接运行。

## 一、开始状态与完成结果

开始前应具备：

- 已完成Spring Boot第1～20章；
- 已阅读[Vue有給休暇综合练习](../../../frontend/Vue/22_vue_project.md)；
- 已阅读[API入出力规格](../../../frontend/Vue/appendix_paid_leave_api.md)；
- 本机MySQL运行在`localhost:3306`，研修账号和密码均为`root`；
- 已取得教师提供的Vue项目，Node.js后台只用于对照接口行为。

完成后应能证明：

1. Spring Boot在`8080`端口启动，`GET /api/health`返回成功；
2. Vue只切换API基本地址，不修改页面字段和业务流程；
3. 注册、登录、会话恢复、首页汇总、申请、详情、检索、取消和退出全部可运行；
4. 用户只能读取和修改本人的申请；
5. 多步写入失败时数据库能够回滚；
6. 自动化测试通过，并可生成脱离IDE运行的JAR。

## 二、固定技术与禁止范围

使用课程主线的固定组合：Java 17、Spring Boot 3.5.x、Spring Web、Validation、MyBatis、MySQL、服务端Session、BCrypt和JUnit。代码按`Controller → Service → Mapper → MySQL`组织，请求DTO、响应对象和数据库Entity分别建立。

本练习不要求JWT、Redis、JPA、Docker、消息队列、微服务、工作流、邮件、管理员审批、Spring Session JDBC或前端分页。不要为了展示技术而改变现有Vue接口。

## 三、数据库准备

数据库名固定为`paid_leave_training`，字符集使用`utf8mb4`。需要建立：

| 表 | 用途 |
| --- | --- |
| `departments` | 部门主数据 |
| `users` | 用户、密码哈希和有給基准日数 |
| `leave_receipt_counters` | 按日期安全生成受付番号 |
| `leave_applications` | 保存申请、状态和取消时间 |

部门初始值为`development`、`quality`、`sales`、`general-affairs`、`human-resources`。用户必须通过注册API创建，不在初始化脚本中保存固定明文密码。

## 四、必须实现的接口

| 编号 | 方法与URL | 登录 | 完成条件 |
| --- | --- | --- | --- |
| API-01 | `GET /api/health` | 不需要 | 同时确认应用和数据库可用 |
| API-02 | `GET /api/master/departments` | 不需要 | 按显示顺序返回部门选项 |
| API-03 | `POST /api/users` | 不需要 | 校验、BCrypt、重复账号和社员番号事务 |
| API-04 | `POST /api/auth/login` | 不需要 | 验证密码并建立Session |
| API-05 | `GET /api/auth/me` | 不需要 | 未登录返回`200`和`data: null` |
| API-06 | `POST /api/auth/logout` | 需要 | 销毁Session并返回`204` |
| API-07 | `GET /api/dashboard` | 需要 | 返回本人资料、余额和汇总 |
| API-08 | `GET /api/leave-applications` | 需要 | 本人数据、状态和关键字组合检索 |
| API-09 | `GET /api/leave-applications/{id}` | 需要 | 只允许取得本人申请 |
| API-10 | `POST /api/leave-applications` | 需要 | 完整校验、事务和受付番号生成 |
| API-11 | `PATCH /api/leave-applications/{id}/cancel` | 需要 | 只把本人的`pending`记录改为`cancelled` |

请求字段、响应字段、状态码和错误码以API入出力规格为准。成功结果放在`data`中；列表另外返回`meta.count`；失败结果统一放在`error`中，不得返回Java堆栈或SQL。

## 五、实施任务

### 任务1：建立工程和第一条调用链

创建工程，配置`8080`端口、MySQL、MyBatis XML位置和日本业务时区。实现健康检查，并分别验证应用未启动、数据库不可用和正常连接三种状态。

### 任务2：完成数据库映射

为四张表建立必要的Entity、Mapper接口和Mapper XML。至少验证一条部门查询和一条用户查询。SQL参数使用`#{}`，不能拼接用户输入。

### 任务3：建立统一响应与异常处理

实现字段校验错误、JSON格式错误、未登录、数据不存在、业务冲突和系统错误。分别返回`400`、`401`、`404`、`409`和`500`。日志记录requestId和异常位置，但不记录密码、Cookie和完整个人信息。

### 任务4：完成用户注册

账号只允许4～20位半角字母、数字和`._-`，保存前转为小写；密码8～32位且两次一致；姓名2～40字；部门必须存在。密码使用BCrypt保存。用户插入和`EMP-00001`格式社员番号更新必须在同一事务内。

### 任务5：完成登录与会话

登录成功后只把内部用户ID保存到服务端Session。Cookie名配置为`paidLeaveSession`并启用`HttpOnly`。允许`http://localhost:5174`携带Cookie访问。退出时销毁Session。当前Vue没有CSRF Token契约，本地研修版不追加该接口，但必须在README中记录生产环境需要重新设计CSRF对策。

### 任务6：完成首页汇总

没有申请时返回有給剩余`12`、申请中`0`、当月批准`0`。只有`approved`且类型为`paid`、`half-am`或`half-pm`的记录扣减余额；特别休假不扣余额；月份按日本时间判断。

### 任务7：完成申请新增与详情

校验日期、半日休假、特别休假说明、理由、引继状态和剩余日数。用户ID和状态必须由后台决定，不能信任请求值。使用事务锁定当日计数器，生成`REQ-YYYYMMDD-NNN`后保存申请；任一步失败都要回滚。为配合现有研修画面，新增状态从`pending`、`approved`、`returned`中生成。

### 任务8：完成一览和取消

一览支持`status`和`keyword`组合条件，关键字匹配受付番号或理由，并按提交时间、主键稳定倒序。所有SQL必须包含当前用户范围。取消SQL同时限制申请ID、用户ID和`pending`状态，不物理删除记录。

### 任务9：完成测试、打包和Vue联调

运行自动化测试和`mvn clean package`，再用`java -jar`启动。Vue本机配置只改为：

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

按注册→登录→首页→申请→完成→一览→取消→退出的顺序执行联调，并同时核对HTTP响应和MySQL状态。

## 六、最低测试范围

| 测试ID | 场景 | 预期结果 |
| --- | --- | --- |
| SB-01 | 健康检查 | 返回`200`和`status: ok` |
| SB-02 | 正常注册 | 返回`201`，数据库只保存密码哈希 |
| SB-03 | 重复账号 | 返回`409`，不新增用户 |
| SB-04 | 登录成功、刷新后调用`me` | Session可以恢复用户 |
| SB-05 | 错误密码 | 返回统一`401`，日志不含密码 |
| SB-06 | 无申请首页 | 返回`12、0、0` |
| SB-07 | 半休跨日 | 返回字段错误，不写入数据库 |
| SB-08 | 开始日早于当天 | 返回业务错误，不写入数据库 |
| SB-09 | 余额不足 | 返回`409` |
| SB-10 | 连续新增 | 受付番号唯一且顺序递增 |
| SB-11 | 新增中途异常 | 申请和计数器一起回滚 |
| SB-12 | 状态和关键字组合 | 只返回同时满足的本人数据 |
| SB-13 | 访问他人申请 | 返回`404`，不泄露是否存在 |
| SB-14 | 取消本人`pending`申请 | 更新为`cancelled`并保存时间 |
| SB-15 | 取消非`pending`申请 | 返回`409`，数据不变 |
| SB-16 | 退出后访问受保护接口 | 返回`401` |

## 七、追加改修票

基础验收完成后，再实现Vue第22章的日期区间检索：为一览接口增加`startDate`和`endDate`。单侧为空表示不限制；两侧都有值时返回与指定期间重叠的申请；日期逆转返回字段错误；日期条件要与状态、关键字同时生效。修改前提交影响调查，修改后执行SB-12和日期条件回归测试。

## 八、提交物与验收证据

- Spring Boot源码和`pom.xml`；
- 建库、建表和主数据脚本；
- 自动化测试代码与结果；
- README、配置项清单和启动手顺；
- 11个接口的请求响应证据；
- Vue全流程联调记录；
- 至少一条真实排错记录；
- 已知限制与生产环境安全差异；
- 可执行JAR的构建和启动证据。

教师参考代码用于讲解和验收，不作为学员起始代码。学员应根据本任务书和既有API规格独立完成实现。
