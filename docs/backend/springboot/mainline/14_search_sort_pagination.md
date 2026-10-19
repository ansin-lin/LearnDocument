# 第14章 前后端分离项目中的REST列表查询、筛选、排序与分页

> 从第13章的 Employee Management API 稳定状态继续，只改修现有 `GET /employees` 列表能力。完成后，你应能依据一张社員一覧改修票，设计接口契约，定位 Controller→Service→Mapper→MySQL 的改动，独立实现并以测试证据交付；详情、新增、修改、删除、事务及履历保持不变。技术基线为 Java 17、Spring Boot 4.1.1、Spring MVC、MyBatis、MySQL 8.0 InnoDB。

本章面向独立前端应用调用 REST API。前端可由 React、Vue 或其他技术实现；本章只展示一个简短请求，不要求编写完整页面。第15章才加入 Session 登录，第16章才收紧列表角色权限。完整源码只供完成练习后在[R14最终源码](../reference/R14_search_pagination_complete_source.md)核对，不应先复制答案。

## 一、从员工列表画面理解REST API

日本项目的社員一覧画面可能有氏名输入框、部署和在籍状态选择、查询按钮、排序控件、每页件数、页码以及数据表格。用户点“查询”时，前端把当前条件发给后端；后端必须在数据库中筛选、排序和分页，不能先把所有员工发到浏览器再处理。

| 功能 | 前端职责 | 后端职责 |
| --- | --- | --- |
| 输入与点击查询 | 收集表单值，发起请求，显示加载/失败状态 | 接收并校验参数 |
| 选择排序和页码 | 提交字段、方向、页码 | 按白名单生成排序SQL并取指定页 |
| 显示总件数与按钮 | 使用 `total`、`totalPages` 更新界面 | 用相同筛选条件执行COUNT |
| 输入校验 | 提前提示用户 | 做可信校验，拒绝非法请求 |
| 权限 | 调整可见按钮 | 最终决定能否访问；前端隐藏不能代替授权 |

一次请求沿下列链路处理；每一层的输出是下一层的输入：

```text
React/Vue列表页：姓名、部门、状态、排序、页码
    ↓ GET /employees?department=Sales&page=2&pageSize=20
Controller：把Query Parameters绑定为EmployeeSearchRequest，触发校验
    ↓ 已校验的查询对象
Service：把空白可选条件整理为null，计算/传递查询参数
    ↓ 同一组有效筛选条件
MyBatis Mapper/XML：列表SELECT（WHERE→ORDER BY→LIMIT/OFFSET）
                  + COUNT（相同WHERE，不分页）
    ↓ 当前页员工Entity + 全部匹配件数
Service：转换为EmployeeListItemResponse，组装PageResponse
    ↓ items、page、pageSize、total、totalPages
Controller：包入ApiResponse并返回JSON
    ↓
前端：更新表格、当前页、总件数与翻页按钮
```

下面是**前端调用示意片段**，不是完整 React/Vue 组件。假定前端与API同源；跨Origin部署时须使用获批的API地址，并按第15～16章处理认证、Cookie与CORS。

```javascript
const params = new URLSearchParams({
  name: "Tanaka", department: "Sales",
  page: "2", pageSize: "20",
  sortBy: "createdAt", sortDirection: "desc"
});
const response = await fetch(`/employees?${params}`);
if (!response.ok) throw new Error(`查询失败：HTTP ${response.status}`);
const result = await response.json();
console.log(result.data.items, result.data.totalPages);
```

`URLSearchParams`负责查询参数编码；`fetch` 返回的HTTP 400/500不会自动抛异常，先检查 `ok` 再使用JSON。真实页面还要处理网络失败和取消过期请求，本章不展开前端组件实现。

## 二、确定列表API的请求与响应规格

沿用已有路径 `GET /employees`，不增加第二个列表入口。例如：

```http
GET /employees?department=Sales&page=2&pageSize=20
```

`/employees` 是资源路径；`?` 开始查询字符串，`&` 分隔参数，`name=value` 给出参数名和值。省略参数时使用下表默认值；含空格、日文或特殊符号的值须URL编码。GET列表条件通常写在查询参数中，便于链接、调试和复现，不把查询条件作为JSON请求体传给这个接口。

| 参数 | 必填 | 默认值 | 可接受的值 | 作用 |
| --- | --- | --- | --- | --- |
| `name` | 否 | 无 | 最长50字符；空白视为未筛选 | 姓名包含匹配 |
| `department` | 否 | 无 | Sales、Development、Support；空白视为未筛选 | 部门完全匹配 |
| `status` | 否 | 无 | ACTIVE、INACTIVE；空串视为未筛选 | 状态完全匹配 |
| `page` | 否 | 1 | 1以上整数 | 当前页，从1开始 |
| `pageSize` | 否 | 20 | 1～100整数 | 每页件数 |
| `sortBy` | 否 | id | id、createdAt | 排序字段白名单 |
| `sortDirection` | 否 | asc | asc、desc | 排序方向白名单 |

`@RequestParam`适合接收少量独立参数；当前有七个相关参数，Controller用 `@ModelAttribute` 把查询参数绑定为一个对象；`@RequestBody` 则用于第10章POST/PUT的JSON业务数据。这里的片段**只替换现有 EmployeeController 列表方法**，不是新建Controller：

```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<EmployeeListItemResponse>>> search(
        @Valid @ModelAttribute EmployeeSearchRequest request) {
    return ResponseEntity.ok(ApiResponse.success(employeeService.search(request)));
}
```

`@Valid`触发 Jakarta Bean Validation。请求DTO中的代表性字段如下，完整文件见R14；未传时保留字段初值，显式传 `page=0` 或 `pageSize=101` 时应返回400：

```java
@NotNull @Min(1)
private Integer page = 1;
@NotNull @Min(1) @Max(100)
private Integer pageSize = 20;
```

`@Size`限制姓名/部门长度，`@Pattern`限制状态和排序值，`@NotBlank`拒绝空排序文字；前端校验只是操作提示，不能替代这些后端约束。`page=abc` 是绑定失败，不是“超出最大页码”；两种错误都要验证HTTP 400，但响应体字段要按实际异常处理器核对。当前 `GlobalExceptionHandler` 对字段校验错误返回 `success=false`、`message=参数校验失败`，`data` 是字段名到消息的Map；不要假设所有绑定错误和业务错误都具有相同 `data` 结构。

成功响应仍是第7章 `ApiResponse`，其中 `data` 改为分页对象。以下JSON是**契约示例**，不是固定测试库结果：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [{"id": 1003, "name": "Tanaka Taro", "department": "Sales"}],
    "page": 2,
    "pageSize": 20,
    "total": 35,
    "totalPages": 2
  }
}
```

`items`只含当前页，`items.length`不等于`total`；`page`和`pageSize`回显本次查询，`total`是全部匹配件数，`totalPages`是页数。前端用 `items` 填表格、`page` 标当前位置、`total` 显示件数、`totalPages` 控制页码与下一页。无匹配时返回200、空items、total=0、totalPages=0；请求页码超出末页仍返回200和空items，但保留真实total。

## 三、把可选条件交给MyBatis动态SQL

“氏名部分一致、部署絞り込み、在籍状态筛选”意味着用户可能不填条件、只填一个或组合多个。部门用 `department = 'Sales'` 精确匹配；姓名用 `LIKE '%Tanaka%'` 包含匹配。MySQL 8.0 中 `%` 匹配任意长度，`_` 匹配一个字符；本章规格允许用户输入的这两个通配符参与LIKE匹配，`#{name}` **不会自动转义**它们。若客户要求按字面搜索百分号/下划线，须另立转义规格。大小写是否相同也取决于列的Collation，不能脱离测试库断言。

Service先把 `null`、空串和仅空格整理成同一个“未筛选”状态：

```java
private String normalizeOptionalText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
}
```

这是**EmployeeServiceImpl 内的片段**，不是额外可运行类。它处理业务语义；DTO注解处理输入是否符合格式与范围，两者职责不同。例如 `name=   ` 在本章规格下不加入姓名条件。原有部门允许值检查继续保留。

`EmployeeMapper.xml` 中的最小片段如下。`<if>` 仅在值有效时加入条件，`<where>` 仅在有条件时生成WHERE并整理开头的AND；`<sql>` 与 `<include>` 让列表与COUNT共享同一WHERE：

```xml
<sql id="searchConditions">
  <where>
    <if test="name != null">AND name LIKE CONCAT('%', #{name}, '%')</if>
    <if test="department != null">AND department = #{department}</if>
    <if test="status != null">AND status = #{status}</if>
  </where>
</sql>

<!-- 两条select内部都放同一行 -->
<include refid="searchConditions"/>
```

`#{...}` 经参数绑定传值；`${...}` 是SQL文本替换。不得把姓名或排序字段从URL直接拼入SQL，否则既可能改变语义，也有注入风险。[MyBatis动态SQL官方说明](https://mybatis.org/mybatis-3/dynamic-sql.html)可用于核对这些标签。项目改修时同时测试零条件、单条件和组合条件生成的SQL及结果。

## 四、排序、分页与COUNT怎样共同工作

排序先于分页。MySQL 8.0 的 `ORDER BY created_at DESC, id ASC` 先按登记时间由新到旧；时间相同时，再按唯一id从小到大。若只写创建时间，同值行没有确定的相对次序，[MySQL 8.0 LIMIT说明](https://dev.mysql.com/doc/refman/8.0/en/limit-optimization.html)也提示LIMIT可能影响这类行的返回顺序。

排序字段只允许 `id`、`createdAt`，方向只允许 `asc`、`desc`。下面是 **EmployeeMapper.xml 的排序分支片段**，实际完整分支和默认 `id` 排序见R14；不写 `ORDER BY ${sortBy}`：

```xml
<choose>
  <when test="sortBy == 'createdAt'">
    ORDER BY created_at
    <choose><when test="sortDirection == 'desc'">DESC</when><otherwise>ASC</otherwise></choose>,
    id ASC
  </when>
  <otherwise>ORDER BY id ASC</otherwise>
</choose>
```

此片段强调列名/关键字由代码选择；R14最终实现的 `id` 分支还按 `sortDirection` 选ASC或DESC。**稳定排序**解决同一数据集合里同值行的顺序；**跨请求分页一致性**是另一问题：第一页与第二页之间若有人插入或删除，OFFSET分页仍可能重复或遗漏。不要把“加id”说成并发快照保证。

MySQL 8.0 的 `LIMIT 20 OFFSET 20` 表示跳过前20条，再取最多20条。换算式为 `offset = (page - 1) × pageSize`：

| page | pageSize | offset |
| ---: | ---: | ---: |
| 1 | 20 | 0 |
| 2 | 20 | 20 |
| 3 | 20 | 40 |
| 5 | 10 | 40 |

请求DTO的 `getOffset()` 使用 `long` 参与乘法，避免先以int相乘溢出；`page` 已由 `@Min(1)` 校验，`pageSize` 上限100。分页SELECT带排序和LIMIT/OFFSET，COUNT只统计**同样筛选条件**，不需要排序与LIMIT：

```sql
-- MySQL 8.0；假定employees表有department列，Sales为示例数据
SELECT id, name, department, email, status, created_at, updated_at
FROM employees WHERE department = 'Sales'
ORDER BY id ASC LIMIT 20 OFFSET 20;

SELECT COUNT(*) FROM employees WHERE department = 'Sales';
```

第一条可能返回0行，第二条仍可能返回35；所以不能用 `items.length` 当总数。`PageResponse` 以 `total / pageSize + (total % pageSize == 0 ? 0 : 1)` 计算总页数：

| total | pageSize | totalPages |
| ---: | ---: | ---: |
| 0 | 20 | 0 |
| 1 | 20 | 1 |
| 20 | 20 | 1 |
| 21 | 20 | 2 |
| 41 | 20 | 3 |

两条SQL共用WHERE可以防止条件漏改，但并发写入时它们仍可能看到不同时间的数据；需要解释一致性要求时再看[A13-3 隔离级别与MVCC](../appendix/A13_03_isolation_mvcc.md)。深页OFFSET可能扫描/跳过更多行，先在测试库用与实际条件一致的 `EXPLAIN` 调查，再决定索引或游标分页；本章不新增分页框架，也不改变现有接口契约。

```sql
-- MySQL 8.0；只在授权测试库观察计划，不修改数据
EXPLAIN SELECT id, name, department, email, status, created_at, updated_at
FROM employees WHERE department = 'Sales'
ORDER BY created_at DESC, id ASC LIMIT 20 OFFSET 0;
```

执行后记录实际筛选值、索引、预计扫描行数和Extra；不凭字段数量机械建索引。EXPLAIN为只读调查，本实验无数据清理，但临时详细SQL日志用完应关闭。

## 五、按日本项目改修票调查与实施

**改修票：社員一覧検索・ページング機能追加。** “社員一覧画面に、氏名の部分一致検索、所属部署の絞り込み、登録日時のソート、およびページング機能を追加する。” 在编码前先把句子变成接口输入、输出与边界，而不是只看“要加几个字段”。

应向设计担当确认：姓名中的 `%`/`_` 是否作通配符、部门/状态是否单选、默认排序、每页上限、页码从1还是0起、无结果与越界页响应、非法参数格式、其他调用方是否依赖原来的数组列表，以及前端需要total还是totalPages。当前教材已给出的默认值和响应契约见第二模块，项目若要变更必须走设计确认，不能由开发者自行修改。

画面基本设计关注按钮、输入与分页展示；API基本设计关注URL、参数、状态码和JSON；详细设计关注Service整理、Mapper SQL与异常；DB设计关注字段和索引；单体测试规格书关注正常系、异常系、境界値及证据。本章改修的影响调查：

| 文件/资源 | 本次变更 | 需保护的既有行为 |
| --- | --- | --- |
| `EmployeeController` | 同一路径接收查询对象并返回分页响应 | 详情与写接口不变 |
| `EmployeeSearchRequest` | 增加筛选、排序、分页字段和校验 | 参数默认值、允许值一致 |
| `EmployeeService` / `EmployeeServiceImpl` | 列表方法签名、条件整理和结果组装 | 第13章更新事务/履历不变 |
| `EmployeeMapper` / `EmployeeMapper.xml` | 列表SELECT、COUNT、共享WHERE、受控排序 | 原CRUD SQL不变 |
| `PageResponse` | 封装items/page/pageSize/total/totalPages | 不泄露Entity内部字段 |
| `EmployeeSearchIntegrationTest` / `search-test-data.sql` | 固定数据与列表行为验证 | 只在专用测试库执行 |

推荐顺序：确认契约→查现有 `GET /employees` 及前端调用方→定请求/响应→定筛选和排序→定分页与COUNT→改Mapper→改Service→接Controller→写测试→回归CRUD/事务。团队可调整实现顺序，但不能省略影响调查与验收。

Review时逐项核对：默认值与参数白名单、非法输入400、`#{...}`绑定与无 `${...}` 注入、列表/COUNT的WHERE一致、同值排序的唯一补充列、空页真实total、前端是否能消费分页JSON、旧CRUD与履历是否保留。第15章Session认证可用于前后端分离REST API，并不要求服务端渲染HTML；第16章给 `GET /employees` 加ADMIN限制后，列表测试需补合适身份，同时保留本章所有分页断言。

## 六、测试设计与综合改修实践

开始状态：第13章稳定 Employee Management 工程的专用副本，MySQL 8.0测试库已按测试保护规则准备；不得对生产库执行测试初始化或清理。先固定员工1001～1006的数据与 `created_at`，再从接口和数据库两侧核对。以下表格是测试规格观点；不能只断言200。

| 分类 | 输入/操作 | 必须核对的预期 |
| --- | --- | --- |
| 正常系 | 不传条件 | 默认page=1、pageSize=20、id升序，items与total正确 |
| 正常系 | 姓名、部门、状态分别筛选 | 包含匹配/完全匹配符合规格 |
| 正常系 | 姓名+部门+状态组合 | items与COUNT都使用三个条件 |
| 正常系 | createdAt降序、第二页、最后一页 | 当前页ID顺序和剩余件数正确 |
| 异常系 | page=0、page=abc | HTTP 400；分别检查校验与绑定路径 |
| 异常系 | sortBy=email、sortDirection=up | HTTP 400，不进入任意排序SQL |
| 境界値 | pageSize=1、100、101 | 前二者200；101为400 |
| 境界値 | 无匹配 | 200、items空、total=0、totalPages=0 |
| 境界値 | 页码超过末页 | 200、items空、total仍是真实匹配数 |
| 一致性 | 相同筛选值执行列表与COUNT | total等于全部匹配数，不等于本页长度 |
| 排序 | 两名员工created_at相同 | 按id确定同值次序，连续页无同值排序歧义 |
| 回归 | 详情、新增、更新、删除、履历失败回滚 | 第13章行为仍成立 |

下列是 **MockMvc测试方法中的片段**；单看 `status().isOk()` 只能证明状态码，必须再断言分页内容与顺序：

```java
mockMvc.perform(get("/employees").param("page", "2").param("pageSize", "20"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.page").value(2))
    .andExpect(jsonPath("$.data.pageSize").value(20))
    .andExpect(jsonPath("$.data.total").isNumber())
    .andExpect(jsonPath("$.data.totalPages").isNumber());
```

实际 `items` 及ID顺序断言须使用固定测试数据。`pageSize=101` 的 `@Max` 校验在现有 `GlobalExceptionHandler` 中成为 `ApiResponse.failure("参数校验失败", fieldErrors)`，可核对 `$.success=false` 和 `$.data.pageSize`；但 `page=abc` 是类型绑定失败，先核对实际响应再写字段断言，不把不同失败统一写成同一JSON形状。

**综合任务：独立完成 `GET /employees` 改修。** 交付API请求/响应规格、处理流程图、影响文件表、修改后的源码、必要JUnit/MockMvc测试、Postman请求与脱敏响应、测试结果表及简短Review记录。至少展示默认列表、组合筛选、同值排序、第二页、空页、越界页、非法参数、COUNT一致性，以及第13章CRUD和事务回归。R14只用于做完之后对照。

在应用目录构建时可先执行 `.\mvnw.cmd -DskipTests package` 检查编译。完整集成测试会使用 `@Sql` 重建专用测试数据，执行 `.\mvnw.cmd test` **前必须核实连接的是可重建的专用MySQL测试库**；不能因教材给出命令就对未知库执行。Postman使用本章版本的匿名接口；进入第15章后需带Session，写请求还需CSRF。测试结束保留脱敏证据，按项目规则恢复/重建专用测试数据，不手工清理生产数据。

完成后，`GET /employees` 从原列表数组升级为分页JSON，详情与写接口及事务履历仍保持第13章行为。你应能说明一次页面操作如何成为查询参数、两条SQL与分页响应，并能以证据证明筛选、排序、COUNT和边界行为。下一章在此稳定状态上增加Session身份识别，不提前实现第16章的角色限制。
