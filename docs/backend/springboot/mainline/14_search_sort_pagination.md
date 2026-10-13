# 第14章 让员工列表支持条件查询与分页

> 本章目标：把原来的简单员工列表升级为可筛选、可稳定排序、可分页的列表接口，并用接口响应和数据库测试证明筛选条件、总件数、空页及非法参数都符合规格。

## 一、先确定接口规格

数据增多后，接口不能每次返回全部员工。本章修改 `GET /employees`：

```text
GET /employees?name=Tanaka&department=Sales&status=ACTIVE
    &page=1&pageSize=20&sortBy=createdAt&sortDirection=desc
```

| 参数 | 必填 | 默认值 | 可接受的值 | 含义 |
| --- | --- | --- | --- | --- |
| `name` | 否 | 无 | 最长50字符；空白按未传处理 | 姓名中包含的文字 |
| `department` | 否 | 无 | Sales、Development、Support；空白按未传处理 | 完全一致的部门 |
| `status` | 否 | 无 | ACTIVE、INACTIVE；空字符串按未传处理 | 完全一致的状态 |
| `page` | 否 | 1 | 1以上整数 | 从1开始的页码 |
| `pageSize` | 否 | 20 | 1～100整数 | 每页最多返回的件数 |
| `sortBy` | 否 | id | id、createdAt | 排序字段白名单 |
| `sortDirection` | 否 | asc | asc、desc | 升序或降序 |

响应仍使用第7章的 `ApiResponse`，`data` 改为分页对象：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [
      {"id": 1003, "name": "Tanaka Taro", "department": "Sales"}
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

无结果或超过最后一页都返回200和空 `items`，同时保留真实 `total`。格式或范围非法则返回400，不静默改成其他值。

## 二、按案例完善同一个员工列表接口

从第13章稳定工程继续，始终修改现有 `GET /employees`，不创建第二个列表接口。阶段一先查询，阶段二确定稳定顺序，阶段三限定每页数据，阶段四补总数；每阶段保留上一阶段的条件。最终各文件的完整版本在章末，可用来核对导入、getter和未改动的CRUD方法。

### 案例14-01：增加姓名、部门和状态筛选

新建 `EmployeeSearchRequest.java`，先声明 `name`、`department`、`status` 三个可选字段及getter/setter；字段约束、分页与排序字段在后续案例继续加入。此时先保持列表响应为原来的数组；案例14-04才把它改为分页对象。在 `EmployeeService` 接口将原 `findList(String department)` 声明替换为 `List<EmployeeListItemResponse> search(EmployeeSearchRequest request)`，并导入请求类型。Controller原列表方法暂时完整替换为：

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeeSearchRequest {
    @Size(max = 50, message = "姓名关键字不能超过50个字符")
    private String name;
    @Size(max = 50, message = "部门不能超过50个字符")
    private String department;
    @Pattern(regexp = "^(|ACTIVE|INACTIVE)$",
            message = "状态必须是ACTIVE或INACTIVE")
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

以上是案例14-01的完整起步DTO；后续案例逐步追加排序和分页字段，最后用章末版本核对。Controller方法仍放在原 `EmployeeController.java` 内，不要另建Controller：

```java
@GetMapping
public ResponseEntity<ApiResponse<List<EmployeeListItemResponse>>> search(
        @Valid @ModelAttribute EmployeeSearchRequest request) {
    return ResponseEntity.ok(
            ApiResponse.success(employeeService.search(request)));
}
```

`@ModelAttribute` 来自Spring MVC：Spring先创建 `EmployeeSearchRequest`，再把URL查询参数中同名的值交给setter，例如 `GET /employees?name=Tanaka&status=ACTIVE` 分别设置 `name` 和 `status`；它不读取JSON请求体，后者是第6章的 `@RequestBody`。没有传的字段保持原值或默认值。`@Valid` 在Controller执行前检查DTO上的约束：`@Size` 限制长度，`@Pattern` 限制状态格式；非法值返回400。空字符串虽符合这里的状态正则式，但Service会将它整理为null，表示不筛选。第一次调试时可在Controller方法首行打断点，观察三个字段如何由URL进入对象。

Service把空白筛选值整理为null，调用Mapper查询并将Entity逐项转换为原有列表DTO；本案例不计算页码和total。案例14-04会完整替换这个临时方法的返回类型，不要在同一个Controller保留两个 `GET /employees`。

在Mapper XML的 `<mapper>` 中加入下面的条件片段，并在列表SELECT的 `FROM employees` 后使用 `<include refid="searchConditions"/>`：

```xml
<sql id="searchConditions">
    <where>
        <if test="name != null">AND name LIKE CONCAT('%', #{name}, '%')</if>
        <if test="department != null">AND department = #{department}</if>
        <if test="status != null">AND status = #{status}</if>
    </where>
</sql>
```

在原 `EmployeeMapper.xml` 的 `<mapper>` 内把旧列表SELECT替换为以下阶段查询；复用原有 `employeeResultMap`，其他CRUD SQL不变：

```xml
<select id="search"
        parameterType="com.example.employee.dto.request.EmployeeSearchRequest"
        resultMap="employeeResultMap">
    SELECT id, name, department, email, status, created_at, updated_at
    FROM employees
    <include refid="searchConditions"/>
    ORDER BY id ASC
</select>
```

Mapper接口把原 `findList(String department)` 改为 `List<Employee> search(EmployeeSearchRequest request)`；此阶段的SELECT选择员工原有列、加入上面的筛选片段，不写LIMIT/OFFSET。Service中把原 `findList()` 替换为以下完整阶段方法，并新增整理方法；沿用第13章已有的 `toListItemResponse()` 和 `validateDepartment()`：

```java
@Override
public List<EmployeeListItemResponse> search(EmployeeSearchRequest request) {
    request.setName(normalizeOptionalText(request.getName()));
    request.setDepartment(normalizeOptionalText(request.getDepartment()));
    request.setStatus(normalizeOptionalText(request.getStatus()));
    if (request.getDepartment() != null) {
        validateDepartment(request.getDepartment());
    }
    List<EmployeeListItemResponse> items = new ArrayList<>();
    for (Employee employee : employeeMapper.search(request)) {
        items.add(toListItemResponse(employee));
    }
    return items;
}

private String normalizeOptionalText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
}
```

确认Service构造器及其他CRUD方法不变。案例14-02在同一SELECT后追加排序，案例14-03再追加分页限制，案例14-04替换阶段Service方法。章末提供完整最终文件供核对。

Service先把空白条件转为null，并对非空部门使用已有 `validateDepartment()`；`<if>` 决定是否生成条件，`<where>` 负责去掉首个多余AND。这里的 `#{name}` 是参数绑定，不能换成 `${name}` 文本拼接。姓名中的 `%` 和 `_` 在LIKE中有通配符意义，当前接口将它们视作搜索语法；若规格要求“按字面字符搜索”，需另行定义转义规则，不能声称本实现已经转义。用Postman分别请求无条件、单条件、双条件和无匹配条件，核对返回员工与数据库结果。

### 案例14-02：为结果确定稳定顺序

在同一个列表SELECT后增加白名单排序。当前规格允许 `id`、`createdAt`，方向限 `asc`、`desc`；请求DTO用 `@Pattern` 限制取值，XML用 `<choose>` 选择预写好的列名和关键字。按创建时间排序时追加唯一 `id ASC`，使同一时间的员工也有确定顺序；按唯一id排序时不需第二列。不得用 `ORDER BY ${sortBy}` 接收原始输入。用两条相同 `created_at` 的测试记录重复查询，核对id次序始终一致。

在 `EmployeeSearchRequest` 追加以下字段和getter/setter；`@NotBlank` 拒绝空值，`@Pattern` 把可接受值限定到白名单：

```java
@NotBlank(message = "排序字段不能为空")
@Pattern(regexp = "^(id|createdAt)$", message = "排序字段必须是id或createdAt")
private String sortBy = "id";

@NotBlank(message = "排序方向不能为空")
@Pattern(regexp = "^(asc|desc)$", message = "排序方向必须是asc或desc")
private String sortDirection = "asc";

public String getSortBy() { return sortBy; }
public void setSortBy(String sortBy) { this.sortBy = sortBy; }
public String getSortDirection() { return sortDirection; }
public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
```

在 `EmployeeMapper.xml` 中，把案例14-01的 `<select id="search">` **完整替换**为下方版本，保留已定义的 `searchConditions`；此阶段仍返回数组、不加LIMIT：

```xml
<select id="search"
        parameterType="com.example.employee.dto.request.EmployeeSearchRequest"
        resultMap="employeeResultMap">
    SELECT id, name, department, email, status, created_at, updated_at
    FROM employees
    <include refid="searchConditions"/>
    <choose>
        <when test="sortBy == 'createdAt'">
            ORDER BY created_at
            <choose>
                <when test="sortDirection == 'desc'">DESC</when>
                <otherwise>ASC</otherwise>
            </choose>, id ASC
        </when>
        <otherwise>
            ORDER BY id
            <choose>
                <when test="sortDirection == 'desc'">DESC</when>
                <otherwise>ASC</otherwise>
            </choose>
        </otherwise>
    </choose>
</select>
```

外层 `<choose>` 决定列名，内层 `<choose>` 决定方向；`<otherwise>` 为不匹配时的兜底分支，但正常输入仍应先通过DTO校验。所有SQL关键字和列名都写死在XML里，不用 `${...}` 拼入用户输入。分别请求 `sortBy=id&sortDirection=desc` 与 `sortBy=createdAt&sortDirection=desc`，确认顺序和同时间的id次序。

### 案例14-03：只取指定页

在请求对象加入默认 `page=1`、`pageSize=20`，分别用 `@Min(1)` 和 `@Min(1) @Max(100)` 限制范围；`@NotNull` 防止显式空值。请求对象的 `getOffset()` 计算 `offset = (page - 1) × pageSize`，先转为long避免int乘法溢出；Mapper列表SQL尾部使用 `LIMIT #{pageSize} OFFSET #{offset}`。本章页码从1开始。Postman检查第1页、第2页、每页件数、越界空页，以及page=0和pageSize=101返回400。排序必须在LIMIT/OFFSET之前生效，否则翻页结果不可靠。

在 `EmployeeSearchRequest` 追加 `Integer page`、`Integer pageSize`、getter/setter及下面的偏移量方法；在案例14-02的排序块后、`</select>` 前追加 `LIMIT #{pageSize} OFFSET #{offset}`。此阶段响应仍是当前页数组，下一案例才增加total与分页响应对象。

```java
@NotNull(message = "页码不能为空")
@Min(value = 1, message = "页码必须从1开始")
private Integer page = 1;

@NotNull(message = "每页件数不能为空")
@Min(value = 1, message = "每页件数至少为1")
@Max(value = 100, message = "每页件数不能超过100")
private Integer pageSize = 20;

public Integer getPage() { return page; }
public void setPage(Integer page) { this.page = page; }
public Integer getPageSize() { return pageSize; }
public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

public long getOffset() {
    return (long) (page - 1) * pageSize;
}
```

Mapper接口此时仍只有一个 `search()`，确认它已从旧列表方法改成下面的签名；Service在案例14-01已将空白条件转为null并做部门校验，调用Mapper时传入**同一个请求对象**：

```java
// EmployeeMapper.java 中替换旧列表声明；导入 Employee、EmployeeSearchRequest、List。
List<Employee> search(EmployeeSearchRequest request);

// EmployeeServiceImpl.java 中沿用案例14-01的阶段方法：
for (Employee employee : employeeMapper.search(request)) {
    items.add(toListItemResponse(employee));
}
```

`getOffset()` 是MyBatis从 `request` 读取的JavaBean属性 `offset`：例如 `page=2`、`pageSize=20` 得到20，XML中的 `#{pageSize}` 为20、`#{offset}` 为20。Service**不另算一份**offset，也不把分页值作为未声明的额外Mapper参数传入。先确认请求对象通过 `@Valid`，再用Postman观察第1页、第2页、越界空页与非法页码；本阶段的 `search()` 仍返回当前页数组。

### 案例14-04：用COUNT补全分页响应

在同一Mapper接口增加 `long count(EmployeeSearchRequest request)`，对应XML追加：

```xml
<select id="count" parameterType="com.example.employee.dto.request.EmployeeSearchRequest" resultType="long">
    SELECT COUNT(*) FROM employees
    <include refid="searchConditions"/>
</select>
```

列表SQL只返回当前页；COUNT在相同筛选条件下返回全部匹配件数，不带ORDER BY、LIMIT或OFFSET。Service把 `items`、`page`、`pageSize`、`total` 交给 `PageResponse`，后者计算 `totalPages`；总数0时页数也为0。两次查询可能观察到不同时间点的并发变化，本章只保证查询条件一致，不承诺跨两次SQL的快照一致性。用Postman核对最后一页、越界页和空条件时的total。

新建 `src/main/java/com/example/employee/dto/response/PageResponse.java`，完整内容如下。`T` 是当前页元素的类型，这里实际使用 `EmployeeListItemResponse`；`total` 是符合条件的总件数，不是当前页件数。`totalPages` 用商与余数计算：有余数再多一页，total为0时得到0页。

```java
package com.example.employee.dto.response;

import java.util.List;

public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;
    private final long totalPages;

    public PageResponse(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = total / pageSize
                + (total % pageSize == 0 ? 0 : 1);
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public long getTotalPages() { return totalPages; }
}
```

同时将 `EmployeeService` 接口、实现类和Controller中的阶段性 `search()` 返回类型从 `List<EmployeeListItemResponse>` 改为 `PageResponse<EmployeeListItemResponse>`。在Service接口把声明改为 `PageResponse<EmployeeListItemResponse> search(EmployeeSearchRequest request);` 并导入该响应类。在Service实现类先加入下面的辅助方法，复用案例14-01已有的`normalizeOptionalText()`和部门检查方法：

```java
private void normalizeSearchRequest(EmployeeSearchRequest request) {
    request.setName(normalizeOptionalText(request.getName()));
    request.setDepartment(normalizeOptionalText(request.getDepartment()));
    request.setStatus(normalizeOptionalText(request.getStatus()));
    if (request.getDepartment() != null) {
        validateDepartment(request.getDepartment());
    }
}
```

然后把Service的阶段性 `search()` 方法完整替换为：

```java
@Override
public PageResponse<EmployeeListItemResponse> search(
        EmployeeSearchRequest request) {
    normalizeSearchRequest(request);
    List<Employee> employees = employeeMapper.search(request);
    long total = employeeMapper.count(request);
    List<EmployeeListItemResponse> items = new ArrayList<>();
    for (Employee employee : employees) {
        items.add(toListItemResponse(employee));
    }
    return new PageResponse<>(
            items, request.getPage(), request.getPageSize(), total);
}
```

Controller的 `@GetMapping` 路径不变，方法返回类型改为 `ResponseEntity<ApiResponse<PageResponse<EmployeeListItemResponse>>>`，方法体仍是 `return ResponseEntity.ok(ApiResponse.success(employeeService.search(request)));`。最后核对同一个类里只存在一个 `search()`；完整导入、getter和未改的CRUD代码见章末。

### 案例14-05：完整验收与回归

完成章末最终文件并运行独立分页测试，再用Postman验证无条件、姓名、部门、组合条件、排序、第二页、最后一页、越界页和非法参数。每次保存HTTP状态、items编号顺序、page、pageSize、total、totalPages；数据库客户端核对筛选和总数。现有详情、创建、修改、删除接口不变。后续第15章引入Security后，旧Postman请求还需附上登录会话，写请求还需CSRF令牌。

## 三、查询参数怎样变成请求对象

案例14-01已说明查询参数如何进入对象。到案例14-03加入分页字段后，参数省略时保留DTO字段初始化的默认值；显式传入非法数字或不在白名单中的文字时，绑定或校验失败并返回400。

`@Valid` 触发第8章已经使用的Bean Validation。`@Min`、`@Max`、`@NotNull`、`@NotBlank`、`@Size` 和 `@Pattern` 都来自 `jakarta.validation.constraints`；它们分别限制数字下限、数字上限、null、空白、长度和正则格式。Spring MVC 6.2对请求对象校验的处理见[官方说明](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-validation.html)。

未传、空值和非法值必须区分：

| 输入 | 结果 |
| --- | --- |
| 不传 `pageSize` | 使用默认20 |
| `name=` 或只有空格 | Service转为null，不生成姓名条件 |
| `status=` | 校验允许空字符串，Service转为null |
| `pageSize=0`、`pageSize=101` | 校验失败，返回400 |
| `sortBy=email` | 不在白名单，返回400 |

## 四、页码、偏移量和分页响应

MySQL 8.0的 `LIMIT` 限制返回件数，`OFFSET` 表示跳过多少条：

```text
offset = (page - 1) × pageSize
```

第2页、每页20条时offset为20。`getOffset()` 使用 `long` 参与乘法，避免先用int计算时溢出。页码超过最后一页不是请求格式错误，所以返回空列表；客户端仍可从total和totalPages判断真实范围。

`PageResponse<T>` 中的 `T` 是当前页元素类型，本章实际为 `EmployeeListItemResponse`。total是全部符合条件的件数，不是当前页件数；totalPages使用整数除法和余数计算，total为0时结果也是0。

## 五、动态SQL怎样只加入有效条件

`<if test="...">` 在条件成立时生成内部SQL；`<where>` 仅在至少一个条件存在时生成WHERE，并移除开头多余的AND。`<sql>` 定义可复用片段，`<include>` 将同一组筛选条件放入数据查询和总数查询，防止两条SQL逐渐不一致。语义可参考[MyBatis动态SQL官方文档](https://mybatis.org/mybatis-3/dynamic-sql.html)。

姓名使用 `#{name}` 参数绑定，`CONCAT` 只在数据库端添加 `%` 通配符。不得改成 `${name}`：后者是文本替换，用户输入可能改变SQL结构并造成SQL注入。

## 六、为什么分页必须稳定排序

如果只按 `created_at` 排序，两名员工创建时间相同时先后顺序未确定；不同请求可能使一条记录重复出现在相邻两页或被漏掉。本章在创建时间后追加唯一的 `id ASC`：

```sql
ORDER BY created_at DESC, id ASC
```

这样相同时间仍有确定顺序。MySQL也说明，当ORDER BY列存在相同值时，其他列的顺序可能不确定，LIMIT还可能影响执行计划；参见[MySQL 8.0 LIMIT优化说明](https://dev.mysql.com/doc/refman/8.0/en/limit-optimization.html)。

排序列和ASC/DESC不能通过 `ORDER BY ${sortBy} ${sortDirection}` 直接接收用户输入。本章先用校验限定接口值，再由MyBatis `<choose>` 只生成预先写好的列名和关键字，因此请求无法注入任意SQL片段。

## 七、数据查询和总数查询必须一致

分页需要两次查询：

```text
search() → 当前页items，带ORDER BY、LIMIT、OFFSET
count()  → 相同条件的total，不需要排序和分页
```

只要name、department或status有一个条件不一致，页面就可能显示“两条记录、总数却是十条”。共享 `searchConditions` 能减少复制错误，但新增条件时仍要同时更新请求规格、请求对象、SQL、测试和接口示例。


## 八、运行与验收

```powershell
.\mvnw.cmd clean test
```

在原有回归测试之外，确认本章新增的筛选、稳定排序、越界页和非法页码测试都实际执行。总数会随练习增加，不用固定数字判断结果；最终应看到：

```text
BUILD SUCCESS
```

手工验收至少保存请求URL、HTTP状态、items编号顺序、page、pageSize、total和totalPages。只截取第一页列表，不能证明总数、稳定排序和越界页正确。

## 九、常见失败与定位

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 无条件查询SQL出现孤立WHERE | 手工拼接WHERE | 使用 `<where>` 并测试全部条件为空 |
| items正确但total错误 | 两条SQL条件不同 | 复用同一筛选片段并增加组合条件测试 |
| 翻页出现重复或遗漏 | 排序值相同且无唯一补充列 | 在业务排序后追加id |
| pageSize过大仍返回200 | 请求对象未校验或未加 `@Valid` | 核对注解、Controller参数和400响应 |
| ORDER BY可被插入任意文字 | 使用 `${}` 拼接请求值 | 改为白名单和 `<choose>` |
| 深页查询逐渐变慢 | OFFSET需要跳过大量结果 | 用EXPLAIN调查，再评估索引或游标分页 |

## 十、规格理解、改修与Review练习

### 练习1：增加邮箱关键字

规格要求按邮箱包含文字筛选，最大100字符。完成请求对象、共享SQL片段和集成测试；证明items和total使用同一条件。

### 练习2：Review不稳定分页

把排序临时改成只有 `created_at DESC`，说明1003和1004顺序为何没有保证，再恢复 `id ASC` 并保存连续两页的编号证据。

### 练习3：非法参数测试

分别验证 `page=0`、`sortBy=email`、`sortDirection=up` 和无法转换为整数的 `page=abc`。记录每次HTTP状态和错误字段。

### 练习4：影响调查

改修票要求新增department和status多选筛选。先调查URL规格、请求对象类型、MyBatis `<foreach>`、空集合语义、SQL、索引和回归范围，不直接编码。

### 练习5：自测证据

提交默认查询、组合条件、相同时间稳定排序、无结果、越界页、最大pageSize和非法参数的请求与响应证据，并注明测试数据版本。

## 十一、章末完整代码

### 最终文件清单与完整实现

先沿一次 `GET /employees` 的处理链读本章代码。Spring MVC用 `@ModelAttribute` 从查询字符串绑定 `EmployeeSearchRequest`，与第6章读取JSON正文的 `@RequestBody` 不同；`@Valid` 在Controller执行前检查页码和pageSize。Service整理筛选条件；请求对象的 `getOffset()` 根据page和pageSize计算偏移量。Mapper只允许规定的排序字段和方向，不能把客户端任意文本拼进SQL；列表SQL与COUNT SQL必须使用同一组筛选条件。排序追加主键作为同值时的稳定次序，避免翻页时同一记录忽前忽后。最后 `PageResponse` 同时给出items、page、pageSize、total和totalPages；空页不等于查询失败。

下面是最终文件的完整代码。阅读顺序建议为“请求DTO及分页偏移量→Service整理条件→Mapper动态条件与白名单排序→COUNT→响应对象→测试”。pageSize设置上限是为了控制单次响应和数据库负担；EXPLAIN与索引分析放到完成正确性测试之后，不把性能优化当成输入绑定的前置要求。

从第13章稳定状态继续。本章不改员工详情和写入接口，新建两个数据对象，完整替换四个业务文件，并新增独立的分页测试和测试数据：

```text
src/main/java/com/example/employee/
├── controller/EmployeeController.java                  ← 完整替换
├── dto/request/EmployeeSearchRequest.java              ← 新建
├── dto/response/PageResponse.java                      ← 新建
├── mapper/EmployeeMapper.java                          ← 完整替换
└── service/
    ├── EmployeeService.java                            ← 完整替换
    └── impl/EmployeeServiceImpl.java                   ← 完整替换
src/main/resources/mapper/EmployeeMapper.xml            ← 完整替换
src/test/java/com/example/employee/integration/
└── EmployeeSearchIntegrationTest.java                 ← 新建
src/test/resources/search-test-data.sql                 ← 新建
```

### 1. 新建EmployeeSearchRequest.java

```java
package com.example.employee.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeeSearchRequest {

    @Size(max = 50, message = "姓名关键字不能超过50个字符")
    private String name;

    @Size(max = 50, message = "部门不能超过50个字符")
    private String department;

    @Pattern(
            regexp = "^(|ACTIVE|INACTIVE)$",
            message = "状态必须是ACTIVE或INACTIVE")
    private String status;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从1开始")
    private Integer page = 1;

    @NotNull(message = "每页件数不能为空")
    @Min(value = 1, message = "每页件数至少为1")
    @Max(value = 100, message = "每页件数不能超过100")
    private Integer pageSize = 20;

    @NotBlank(message = "排序字段不能为空")
    @Pattern(
            regexp = "^(id|createdAt)$",
            message = "排序字段必须是id或createdAt")
    private String sortBy = "id";

    @NotBlank(message = "排序方向不能为空")
    @Pattern(
            regexp = "^(asc|desc)$",
            message = "排序方向必须是asc或desc")
    private String sortDirection = "asc";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }
}
```

### 2. 新建PageResponse.java

```java
package com.example.employee.dto.response;

import java.util.List;

public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;
    private final long totalPages;

    public PageResponse(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = total / pageSize
                + (total % pageSize == 0 ? 0 : 1);
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public long getTotalPages() { return totalPages; }
}
```

### 3. 完整替换EmployeeMapper.java

```java
package com.example.employee.mapper;

import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {
    Employee findById(@Param("id") Long id);
    List<Employee> search(EmployeeSearchRequest request);
    long count(EmployeeSearchRequest request);
    int insert(Employee employee);
    int update(Employee employee);
    int deleteById(@Param("id") Long id);
}
```

### 4. 完整替换EmployeeMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeMapper">

    <resultMap id="employeeResultMap" type="Employee">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="department" column="department"/>
        <result property="email" column="email"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <sql id="searchConditions">
        <where>
            <if test="name != null">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="department != null">
                AND department = #{department}
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
    </sql>

    <select id="findById" parameterType="long"
            resultMap="employeeResultMap">
        SELECT id, name, department, email, status,
               created_at, updated_at
        FROM employees
        WHERE id = #{id}
    </select>

    <select id="search"
            parameterType="com.example.employee.dto.request.EmployeeSearchRequest"
            resultMap="employeeResultMap">
        SELECT id, name, department, email, status,
               created_at, updated_at
        FROM employees
        <include refid="searchConditions"/>
        <choose>
            <when test="sortBy == 'createdAt'">
                ORDER BY created_at
                <choose>
                    <when test="sortDirection == 'desc'">DESC</when>
                    <otherwise>ASC</otherwise>
                </choose>, id ASC
            </when>
            <otherwise>
                ORDER BY id
                <choose>
                    <when test="sortDirection == 'desc'">DESC</when>
                    <otherwise>ASC</otherwise>
                </choose>
            </otherwise>
        </choose>
        LIMIT #{pageSize} OFFSET #{offset}
    </select>

    <select id="count"
            parameterType="com.example.employee.dto.request.EmployeeSearchRequest"
            resultType="long">
        SELECT COUNT(*)
        FROM employees
        <include refid="searchConditions"/>
    </select>

    <insert id="insert" parameterType="Employee"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO employees (name, department, email, status)
        VALUES (#{name}, #{department}, #{email}, #{status})
    </insert>

    <update id="update" parameterType="Employee">
        UPDATE employees
        SET name = #{name}, department = #{department}, email = #{email}
        WHERE id = #{id}
    </update>

    <delete id="deleteById" parameterType="long">
        DELETE FROM employees WHERE id = #{id}
    </delete>
</mapper>
```

### 5. 完整替换EmployeeService.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.dto.response.PageResponse;

public interface EmployeeService {
    EmployeeResponse findById(Long id);
    PageResponse<EmployeeListItemResponse> search(EmployeeSearchRequest request);
    EmployeeResponse create(EmployeeCreateRequest request);
    EmployeeResponse update(Long id, EmployeeUpdateRequest request);
    void delete(Long id);
}
```

### 6. 完整替换EmployeeServiceImpl.java

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.dto.response.PageResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeChangeLog;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.EmployeeSystemException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeChangeLogMapper;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final String ACTION_EMPLOYEE_UPDATED =
            "EMPLOYEE_UPDATED";
    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeMapper employeeMapper;
    private final EmployeeChangeLogMapper employeeChangeLogMapper;

    public EmployeeServiceImpl(
            EmployeeMapper employeeMapper,
            EmployeeChangeLogMapper employeeChangeLogMapper) {
        this.employeeMapper = employeeMapper;
        this.employeeChangeLogMapper = employeeChangeLogMapper;
    }

    @Override
    public EmployeeResponse findById(Long id) {
        return toResponse(findEmployeeOrThrow(id));
    }

    @Override
    public PageResponse<EmployeeListItemResponse> search(
            EmployeeSearchRequest request) {
        normalizeSearchRequest(request);
        List<Employee> employees = employeeMapper.search(request);
        long total = employeeMapper.count(request);
        List<EmployeeListItemResponse> items = new ArrayList<>();
        for (Employee employee : employees) {
            items.add(toListItemResponse(employee));
        }
        log.debug(
                "employee_search_completed page={} pageSize={} total={}",
                request.getPage(), request.getPageSize(), total);
        return new PageResponse<>(
                items, request.getPage(), request.getPageSize(), total);
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        String department = request.getDepartment().trim();
        validateDepartment(department);
        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
        employee.setEmail(normalizeEmail(request.getEmail()));
        employee.setStatus("ACTIVE");
        try {
            int affectedRows = employeeMapper.insert(employee);
            if (affectedRows != 1 || employee.getId() == null) {
                throw new EmployeeSystemException("新增员工结果异常");
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        log.info("employee_created employeeId={} department={}",
                employee.getId(), employee.getDepartment());
        return findById(employee.getId());
    }

    @Override
    @Transactional
    public EmployeeResponse update(
            Long id, EmployeeUpdateRequest request) {
        String department = request.getDepartment().trim();
        validateDepartment(department);
        Employee beforeUpdate = findEmployeeOrThrow(id);
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
        employee.setEmail(normalizeEmail(request.getEmail()));
        try {
            int affectedRows = employeeMapper.update(employee);
            if (affectedRows > 1) {
                throw new EmployeeSystemException("修改员工影响多行");
            }
            if (affectedRows == 0 && employeeMapper.findById(id) == null) {
                throw new EmployeeNotFoundException(id);
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        EmployeeChangeLog changeLog = new EmployeeChangeLog(
                id, ACTION_EMPLOYEE_UPDATED,
                "department=" + beforeUpdate.getDepartment()
                        + "->" + employee.getDepartment());
        int logRows = employeeChangeLogMapper.insert(changeLog);
        if (logRows != 1 || changeLog.getId() == null) {
            throw new EmployeeSystemException("员工变更履历写入失败");
        }
        log.info("employee_updated employeeId={} department={}",
                id, employee.getDepartment());
        return findById(id);
    }

    @Override
    public void delete(Long id) {
        int affectedRows = employeeMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new EmployeeNotFoundException(id);
        }
        if (affectedRows != 1) {
            throw new EmployeeSystemException("删除员工影响多行");
        }
        log.info("employee_deleted employeeId={}", id);
    }

    private void normalizeSearchRequest(EmployeeSearchRequest request) {
        request.setName(normalizeOptionalText(request.getName()));
        request.setDepartment(normalizeOptionalText(request.getDepartment()));
        request.setStatus(normalizeOptionalText(request.getStatus()));
        if (request.getDepartment() != null) {
            validateDepartment(request.getDepartment());
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Employee findEmployeeOrThrow(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }
        return employee;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getName(),
                employee.getDepartment(), employee.getEmail());
    }

    private EmployeeListItemResponse toListItemResponse(Employee employee) {
        return new EmployeeListItemResponse(employee.getId(),
                employee.getName(), employee.getDepartment());
    }

    private void validateDepartment(String department) {
        if (!"Sales".equals(department)
                && !"Development".equals(department)
                && !"Support".equals(department)) {
            throw new InvalidDepartmentException(department);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
```

与第13章相比，只有列表路径从 `findList(String)` 改为 `search(EmployeeSearchRequest)`；事务、履历和其他CRUD逻辑保持原样。整文件给出是为了避免学员组合局部片段时误删事务代码。

### 7. 完整替换EmployeeController.java

```java
package com.example.employee.controller;

import com.example.employee.common.ApiResponse;
import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeSearchRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.dto.response.PageResponse;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(
            @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(employeeService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeListItemResponse>>>
            search(@Valid @ModelAttribute EmployeeSearchRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(employeeService.search(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.create(request);
        URI location = URI.create("/employees/" + employee.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.success(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(employeeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable(name = "id") Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 8. 新建search-test-data.sql

该文件由下一小节的独立测试在基础 `test-data.sql` 之后执行。先把基础数据中1001、1002的默认当前时间固定为更早的时间，再追加分页记录；这样按 `createdAt desc` 排序时，1003、1004会排在1001前面，断言不受执行日期影响：

```sql
UPDATE employees
SET created_at = '2026-01-01 09:00:00',
    updated_at = '2026-01-01 09:00:00'
WHERE id IN (1001, 1002);

INSERT INTO employees (
    id, name, department, email, status, created_at, updated_at
) VALUES
    (1003, 'Tanaka Taro', 'Sales', 'taro@example.com',
     'ACTIVE', '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
    (1004, 'Tanaka Aiko', 'Sales', 'aiko@example.com',
     'ACTIVE', '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
    (1005, 'Yamada', 'Support', 'yamada@example.com',
     'INACTIVE', '2026-01-03 09:00:00', '2026-01-03 09:00:00'),
    (1006, 'Ito', 'Development', 'ito@example.com',
     'ACTIVE', '2026-01-04 09:00:00', '2026-01-04 09:00:00');
```

### 9. 新建EmployeeSearchIntegrationTest.java

```java
package com.example.employee.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = {"/test-data.sql", "/search-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchUsesSameConditionsForItemsAndTotal() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("name", "Tanaka")
                        .param("department", "Sales")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("pageSize", "2")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(1003))
                .andExpect(jsonPath("$.data.items[1].id").value(1004))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void secondPageKeepsStableOrder() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("name", "Tanaka")
                        .param("page", "2")
                        .param("pageSize", "2")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(1001));
    }

    @Test
    void pageBeyondLastReturnsEmptyItemsAndRealTotal() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("department", "Support")
                        .param("page", "99")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(99));
    }

    @Test
    void invalidPageSizeReturns400() throws Exception {
        mockMvc.perform(get("/employees")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }
}
```

## 十二、本章稳定状态

完成后，原列表接口已经被分页查询替换，详情、新增、修改、删除和事务履历保持不变。你应能够：

1. 把筛选、排序和分页要求写成明确接口规格；
2. 区分参数省略、空白、非法和超出最后一页；
3. 计算offset并解释items、total和totalPages；
4. 使用 `<if>`、`<where>`、`<sql>`、`<include>` 和 `<choose>` 生成受控动态SQL；
5. 保证数据查询和总数查询的条件一致；
6. 用唯一补充列建立稳定排序，并拒绝ORDER BY文本注入；
7. 用接口响应和数据库数据验证排序、总数与边界。

下一章将在现有接口上增加登录和Session身份识别；本章不提前实现权限判断。

## 附录：深页性能与执行计划

当前列表主线先验证功能正确性。数据量增长后，可按真实查询和跳页需求再评估索引、EXPLAIN与游标分页；这些优化不能改变本章已确定的接口规格而不做影响调查。

### 只根据真实查询评估索引与EXPLAIN

先在个人练习MySQL中使用与实际接口一致的条件观察执行计划：

```sql
EXPLAIN
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
WHERE department = 'Sales' AND status = 'ACTIVE'
ORDER BY created_at DESC, id ASC
LIMIT 20 OFFSET 0;
```

重点观察访问类型、可能使用和实际使用的索引、预计扫描行数及Extra。是否增加组合索引取决于数据量、值分布、常用筛选组合和排序方向；不能因为查询出现三个字段就机械建立三个单列索引。MySQL对索引与排序的关系见[ORDER BY优化文档](https://dev.mysql.com/doc/refman/8.0/en/order-by-optimization.html)，执行计划和复杂优化继续参考SQL课程。

OFFSET越大，数据库通常需要跳过越多结果。当前接口适合基础管理页面；数据达到需要频繁访问深页时，应根据产品的跳页需求评估游标分页，而不是先修改本章接口契约。
