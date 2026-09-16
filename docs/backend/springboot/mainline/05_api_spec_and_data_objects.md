# 第5章 接口规格与数据对象为什么要分开

> 本章目标：从员工接口规格中判断哪些数据由客户端提交、哪些数据由系统返回，并建立职责明确的请求DTO、详情响应对象和列表响应对象。

第4章已经建立 `Controller → Service` 调用，但接口只返回固定文本。真正开发员工管理接口前，不能立刻把所有员工字段塞进同一个Java类。必须先回答：谁提供这些数据、谁可以看到这些数据、每个接口到底需要哪些字段？

本章只解决“接口数据边界”。请求怎样绑定到Java方法在第6章实现，成功和失败怎样包装在第7章实现，数据库对象在第9章根据真实表定义创建。

## 一、开始状态与完成结果

继续使用第4章完成后的工程。当前至少包含：

```text
src/main/java/com/example/employee/
├── EmployeeManagementApiApplication.java
├── controller/
│   ├── EmployeeController.java
│   └── HealthController.java
└── service/
    └── EmployeeService.java
```

本章只新增三个普通Java类：

```text
src/main/java/com/example/employee/dto/
├── request/
│   └── EmployeeCreateRequest.java
└── response/
    ├── EmployeeListItemResponse.java
    └── EmployeeResponse.java
```

当前Controller和Service保持不变，因此第4章的接口仍然返回原有文本。本章完成后应达到两个可检查结果：

- Maven能够编译三个新类；
- 能根据接口规格说明每个字段为什么属于请求、详情响应或列表响应。

## 二、先阅读本章使用的接口规格

“接口规格”是前端、后端、测试及调用方共同确认的约定。它至少要说明接口用途、HTTP方法、路径、输入、成功输出和失败情况。没有规格就直接写类，开发者只能靠猜测决定字段。

本章先为后续三个员工接口确定数据边界。第6章才把这些规格连接到Controller方法。

### 1. 新增员工接口

| 项目 | 规格 |
| --- | --- |
| 规格编号 | `EMP-API-01` |
| 用途 | 新增一名员工 |
| HTTP方法 | `POST` |
| 路径 | `/employees` |
| 客户端提交 | `name`、`department`、`email` |
| 系统生成 | `id` |
| 成功状态 | `201 Created` |
| 成功响应 | `id`、`name`、`department`、`email` |
| 主要失败 | 输入不符合字段规格、邮箱重复、系统处理失败 |

客户端不能提交 `id`，因为编号由系统生成。如果把 `id` 放进新增请求对象，调用方可能误以为它可以指定员工编号。

### 2. 员工详情接口

| 项目 | 规格 |
| --- | --- |
| 规格编号 | `EMP-API-02` |
| 用途 | 根据编号查看一名员工 |
| HTTP方法 | `GET` |
| 路径 | `/employees/{id}` |
| 路径数据 | `id` |
| 成功状态 | `200 OK` |
| 成功响应 | `id`、`name`、`department`、`email` |
| 主要失败 | 编号格式错误、员工不存在、系统处理失败 |

详情画面需要联系邮箱，因此详情响应包含 `email`。

### 3. 员工列表接口

| 项目 | 规格 |
| --- | --- |
| 规格编号 | `EMP-API-03` |
| 用途 | 查看员工概要列表 |
| HTTP方法 | `GET` |
| 路径 | `/employees` |
| 查询条件 | 本章暂不定义 |
| 成功状态 | `200 OK` |
| 每个列表项 | `id`、`name`、`department` |
| 主要失败 | 查询条件错误、系统处理失败 |

列表只用于快速浏览，不需要邮箱。后端应按照规格只返回必要字段，不能先返回所有内部数据，再要求前端自己忽略。

## 三、把字段规格写清楚

接口名称只能说明“做什么”，字段规格进一步说明每项数据怎样使用。本章采用下面的字段定义：

| 字段 | Java类型 | 新增请求 | 详情响应 | 列表响应 | 规则 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 不允许提交 | 必须返回 | 必须返回 | 系统生成的员工编号 |
| `name` | `String` | 必填 | 必须返回 | 必须返回 | 1～50个字符，不能只有空白 |
| `department` | `String` | 必填 | 必须返回 | 必须返回 | 1～50个字符，不能只有空白 |
| `email` | `String` | 必填 | 必须返回 | 不返回 | 最多100个字符，必须符合邮箱格式 |

表中的“必填”和格式限制属于规格。当前先用它们决定字段和对象结构；第8章加入Validation依赖后，再把这些约束实现为可执行校验。

这里使用Java包装类型 `Long` 表示员工编号。`Long` 能表达“暂时没有编号”的状态，而基本类型 `long` 总会有数值，未赋业务值时也会得到默认值0。新增请求还没有系统生成的编号，所以请求对象根本不设置 `id` 字段；响应对象中的 `id` 则必须由系统提供。

## 四、完整示例

按照前面的规格新建下面三个文件。先完成全部代码，再从第五节开始逐个解释第一次出现的内容。

### 1. 新建EmployeeCreateRequest.java

文件位置：

```text
src/main/java/com/example/employee/dto/request/EmployeeCreateRequest.java
```

完整内容：

```java
package com.example.employee.dto.request;

public class EmployeeCreateRequest {

    private String name;
    private String department;
    private String email;

    public EmployeeCreateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

### 2. 新建EmployeeResponse.java

文件位置：

```text
src/main/java/com/example/employee/dto/response/EmployeeResponse.java
```

完整内容：

```java
package com.example.employee.dto.response;

public class EmployeeResponse {

    private final Long id;
    private final String name;
    private final String department;
    private final String email;

    public EmployeeResponse(
            Long id,
            String name,
            String department,
            String email) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmail() {
        return email;
    }
}
```

### 3. 新建EmployeeListItemResponse.java

文件位置：

```text
src/main/java/com/example/employee/dto/response/EmployeeListItemResponse.java
```

完整内容：

```java
package com.example.employee.dto.response;

public class EmployeeListItemResponse {

    private final Long id;
    private final String name;
    private final String department;

    public EmployeeListItemResponse(
            Long id,
            String name,
            String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }
}
```

三个类都只是项目自己定义的普通Java类，没有Spring注解，也不是Spring Bean。它们负责携带数据，不负责接收URL、执行业务判断或访问数据库。

## 五、理解请求DTO

### 1. DTO表示什么

DTO是Data Transfer Object，通常译为“数据传输对象”。它用于在系统边界或层之间携带一组数据。

`EmployeeCreateRequest` 表示“客户端为了新增员工而提交的数据”。类名中的三个部分分别说明：

- `Employee`：数据属于员工业务；
- `Create`：用于新增操作；
- `Request`：数据方向是客户端到后端。

DTO不是一个新的业务层。`dto.request` 只是存放请求数据类的包，Controller层、Service层和数据访问层仍然按照第2章的职责划分。

### 2. 为什么请求对象没有id

新增员工时，客户端只负责提供姓名、部门和邮箱。员工编号由系统生成，所以类中只有：

```java
private String name;
private String department;
private String email;
```

如果未来数据库表还有创建时间、更新时间、逻辑删除标志等内部字段，也不能因此自动把它们加入请求对象。客户端能够提交什么，必须由接口规格决定，而不是由数据库有多少列决定。

### 3. 无参数构造方法

```java
public EmployeeCreateRequest() {
}
```

这是公开的无参数构造方法：方法名与类名相同，没有返回值类型，参数列表为空。它创建一个字段尚未赋业务值的请求对象。第6章进行JSON请求绑定时，框架需要能够建立请求对象并写入字段，因此这里明确保留无参数构造方法。

当前代码没有再声明带参数构造方法。如果一个类完全不写构造方法，Java会提供默认无参数构造方法；一旦开发者声明了其他构造方法，默认构造方法就不会自动保留。显式写出可以让这个要求在代码中保持清楚。

### 4. getter和setter

以姓名为例：

```java
public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}
```

`getName()` 没有参数，返回当前对象的 `name` 字段。`setName(String name)` 接收一个字符串参数，返回类型是 `void`，表示不返回结果；方法把参数保存到当前对象字段中。

`department` 和 `email` 的getter、setter作用相同。第6章会说明JSON字段怎样通过这些Java属性进入请求对象。

## 六、理解详情响应对象

`EmployeeResponse` 表示后端返回给客户端的一名员工详情。它包含系统生成的 `id`，因为调用方需要用编号识别员工；同时包含规格允许详情接口公开的邮箱。

### 1. 构造方法一次建立完整响应

```java
public EmployeeResponse(
        Long id,
        String name,
        String department,
        String email) {
    this.id = id;
    this.name = name;
    this.department = department;
    this.email = email;
}
```

创建 `EmployeeResponse` 时必须依次提供编号、姓名、部门和邮箱。构造方法把四个参数分别保存到同名字段。参数顺序和类型必须与调用代码一致。

响应字段使用 `private final`。`private` 阻止类外直接修改字段；`final` 表示构造完成后不能让字段改指向另一个值。响应数据由后端准备完成后再交给客户端，因此本章不给响应对象添加setter。

### 2. getter提供读取入口

```java
public Long getId() {
    return id;
}
```

每个getter返回一个字段值。第6章把响应对象交给Spring Web时，JSON转换组件会通过这些可读取属性生成响应字段。本章先确认类的字段、构造方法和getter与规格一致。

## 七、为什么列表和详情不能强行共用一个响应类

详情规格允许返回：

```text
id、name、department、email
```

列表规格只允许返回：

```text
id、name、department
```

如果列表也使用 `EmployeeResponse`，邮箱字段就已经存在于返回对象中。让前端“不显示邮箱”不等于后端没有公开邮箱：数据仍然经过网络传给了客户端，也可能出现在浏览器开发者工具、日志或缓存中。

因此本章建立专用的 `EmployeeListItemResponse`。它没有 `email` 字段，也没有 `getEmail()` 方法，后续生成列表JSON时就不会产生邮箱属性。

列表对象和详情对象有三个相同字段并不代表设计错误。它们服务不同接口规格，可以独立变化。只有当多个接口的含义、字段和变化原因确实一致时，才考虑共用类型；不能只为了减少文件数量就合并边界不同的对象。

## 八、数据库对象为什么也要分开

数据库中的一行员工记录，通常还会包含接口不应该直接控制或公开的内容，例如系统时间、状态标志或内部管理字段。相反，响应也可能包含经过组合或格式化、但数据库没有直接保存的显示数据。

因此三类对象的方向不同：

```text
客户端提交JSON
  → EmployeeCreateRequest
  → Service执行业务处理
  → 数据库对象（第9章根据真实表定义）

数据库查询结果
  → 数据库对象
  → Service选择并转换字段
  → EmployeeResponse或EmployeeListItemResponse
  → 客户端接收JSON
```

本课程在第9章使用MyBatis，并根据真实表结构创建持久化对象。这里的“Entity”表示与数据库记录对应的项目数据对象，不表示本课程会改用JPA，也不需要添加JPA的 `@Entity` 注解。

当前表定义尚未确定，所以本章不提前创建一个靠猜测字段组成的 `EmployeeEntity`。

## 九、对象转换是什么

对象转换不是强制类型转换，而是根据目标对象的职责，明确取出允许的数据并创建新对象。

下面是Service中未来会出现的代码片段，不需要在本章加入工程：

```java
EmployeeResponse response = new EmployeeResponse(
        generatedId,
        request.getName(),
        request.getDepartment(),
        request.getEmail());
```

这段代码从 `EmployeeCreateRequest` 读取客户端提交的三个字段，再加入系统生成的 `generatedId`，创建一个新的详情响应对象。

转换时不是把所有字段原样复制，而要逐项判断：

1. 目标接口规格是否允许返回该字段；
2. 字段是否由系统生成；
3. 是否需要格式化、隐藏或组合；
4. 源对象缺少目标字段时，应该从哪里取得。

例如新增请求没有 `id`，响应中的 `id` 必须来自系统处理结果；列表规格没有 `email`，转换列表项时就不能复制邮箱。

## 十、Response、DTO和VO名称怎样理解

不同项目可能使用不同包名：

```text
dto/request/EmployeeCreateRequest.java
dto/response/EmployeeResponse.java
```

也可能使用：

```text
request/EmployeeCreateRequest.java
response/EmployeeResponse.java
```

有些项目把接口输出类放在 `vo` 包中，但VO也可能表示Value Object。仅看到“VO”三个字母，无法确定它一定是响应对象。进入既有项目时，应检查类的字段、使用位置和项目命名规则。

本课程统一采用：

- `dto.request`：客户端提交的数据；
- `dto.response`：后端返回给客户端的数据；
- 第9章的数据对象包：根据MyBatis和项目命名约定确定。

名称帮助表达职责，但后缀不能代替实际设计。一个类即使叫DTO，如果它同时接收请求、映射数据库并直接作为响应返回，仍然没有形成清楚的数据边界。

## 十一、JSON与Java字段映射常见注解

第6章会由Jackson把Java对象转换成JSON。既存项目的数据对象中常见下面四个注解，它们只调整JSON映射，不会改变对象是请求DTO、响应DTO还是数据库对象的职责。

### 1. 先看完整的识读示例

下面是独立识读示例，不加入Employee主线。它集中展示字段改名、隐藏、null输出和日期格式：

```java
package com.example.employee.dilab;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class EmployeeJsonView {

    @JsonProperty("employee_name")
    private final String employeeName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String note;

    @JsonIgnore
    private final String internalMemo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    public EmployeeJsonView(
            String employeeName,
            String note,
            String internalMemo,
            LocalDateTime createdAt) {
        this.employeeName = employeeName;
        this.note = note;
        this.internalMemo = internalMemo;
        this.createdAt = createdAt;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getNote() {
        return note;
    }

    public String getInternalMemo() {
        return internalMemo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
```

当值为姓名Tanaka、note为null、internalMemo有内容、时间为2026年9月16日9时30分时，序列化结果等价于：

```json
{
  "employee_name": "Tanaka",
  "createdAt": "2026-09-16 09:30:00"
}
```

`note` 因为是null而省略，`internalMemo` 无论是否为null都不输出。JSON字段名没有自动变成数据库列名，它只由Java属性和Jackson规则决定。

### 2. @JsonProperty：明确JSON属性名

`@JsonProperty` 属于 `com.fasterxml.jackson.annotation`，可用于字段、getter、setter、构造参数等JSON属性位置。本例的值 `employee_name` 是对外JSON字段名：

```text
Java属性 employeeName
        ↕ Jackson映射
JSON字段 employee_name
```

序列化时Java值写成 `employee_name`；反序列化时同名JSON也可写入对应Java属性。接口规格必须统一使用一个字段名，不能让不同Controller随意选择驼峰或下划线。

### 3. @JsonIgnore：不参与JSON映射

`@JsonIgnore` 也属于Jackson annotations。本例把 `internalMemo` 排除在JSON外，即使类中存在公共getter也不应输出该属性。

但“字段不会输出”不等于数据边界已经安全。响应类若混入密码哈希、内部权限或大量数据库字段，后续改动可能重新暴露数据。仍应优先使用职责明确的Response DTO，只把 `@JsonIgnore` 用于确实属于同一对象但不参与当前JSON映射的属性。

### 4. @JsonInclude：什么值可以省略

`@JsonInclude(JsonInclude.Include.NON_NULL)` 表示当前属性值为null时不写入JSON；非null时正常输出。它可以放在属性或类上。类级规则会影响多个字段，Review时要同时检查类和字段。

省略字段与输出 `"note": null` 对客户端不是完全相同的状态。接口规格应明确调用方如何解释“字段不存在”和“字段存在但为null”，不能只为了缩短JSON随意改变规则。

### 5. @JsonFormat：改变JSON表示，不增加时区

`@JsonFormat` 的 `pattern` 指定日期时间的文本格式。本例 `yyyy-MM-dd HH:mm:ss` 会输出四位年份、两位月份、两位日期以及时分秒。

`LocalDateTime` 本身没有时区。添加 `@JsonFormat` 只改变JSON字符串长什么样，不会自动说明它是UTC、Asia/Tokyo还是服务器本地时间。日期类型、时区、数据库字段和浏览器转换在第9章统一说明。

Jackson由 `spring-boot-starter-web` 间接提供，本课程主线不单独固定Jackson版本；实际版本由Spring Boot 3.5.16依赖管理决定。四个注解的定义可从[Jackson annotations项目文档](https://github.com/FasterXML/jackson-annotations)核对。

### 6. 既存DTO的阅读顺序

看到Jackson注解时按下面顺序调查：

```text
接口规格中的JSON字段
  → Java字段和getter/setter
  → 类级Jackson规则
  → 字段或方法级覆盖规则
  → null、隐藏字段和日期格式
  → Controller实际把哪种DTO作为输入或输出
```

识读练习：根据完整示例回答 `employee_name`、缺少的note、未输出的internalMemo分别由哪个规则产生；再说明为什么不能把数据库Entity加几个 `@JsonIgnore` 后直接当作所有接口响应。

## 十二、构建并检查结果

在项目根目录执行：

```powershell
.\mvnw.cmd clean test
```

预期结果：

```text
BUILD SUCCESS
```

这一步能够证明三个新类的包声明、类名、字段类型、构造方法和方法语法可以编译，也会回归加载Spring应用。它不能自动证明字段设计符合接口规格，因此还必须人工逐项对照第二、三节的规格表。

本章没有修改Controller和Service。需要运行应用时，第4章已有接口应继续保持原结果；三个新数据对象要到第6章连接HTTP请求和响应后，才会改变员工接口的JSON。

## 十三、常见问题与Review

| 现象或设计 | 原因 | 修正或Review意见 |
| --- | --- | --- |
| 新增请求中出现 `id` | 把系统生成字段交给客户端控制 | 从 `EmployeeCreateRequest` 删除 `id` |
| 列表响应包含 `email` | 直接复用了详情响应对象 | 使用 `EmployeeListItemResponse` |
| 请求类直接作为响应返回 | 输入和输出共用同一个边界 | 根据响应规格创建 `EmployeeResponse` |
| 根据想象创建数据库Entity | 表定义尚未确认 | 等第9章根据表定义和数据字典创建 |
| 把DTO当作新的分层 | 混淆代码职责和数据载体 | DTO是携带数据的对象，不是Controller、Service之外的新业务层 |
| 修改字段后只改一个类 | 没有做影响调查 | 检查规格、构造调用、转换代码、响应和自测项目 |
| 为响应类随意增加setter | 沿用了请求对象写法 | 响应由构造方法完整建立时只保留getter |
| JSON字段名与Java字段名不同却找不到原因 | 忽略了Jackson注解 | 检查类、字段、getter/setter和构造参数上的映射规则 |
| 给Entity增加`@JsonIgnore`后直接返回 | 用注解掩盖对象职责混乱 | 保留Request/Response DTO边界，再按规格处理少量映射差异 |

Review数据对象时按下面顺序检查：

```text
接口用途
  → 数据方向
  → 字段规格
  → Java类与字段
  → 创建或写入方式
  → 读取或返回方式
```

不能只看字段名是否拼写正确，还要确认调用方是否有权提供或看到这个字段。

## 十四、操作练习

### 练习1：从规格判断对象

阅读下面四项数据，分别判断应该出现在哪个对象中，并写出理由：

| 数据 | 条件 |
| --- | --- |
| 员工姓名 | 新增时由客户端填写，列表和详情都显示 |
| 员工编号 | 系统生成，列表和详情都显示 |
| 邮箱 | 新增时填写，只有详情显示 |
| 数据更新时间 | 系统维护，不向当前三个接口公开 |

验收：答案必须分别提到“谁提供”和“谁可以看到”，不能只根据字段名称猜测。

### 练习2：实现一次受控规格变更

收到下面的改修规格：

> `EMP-API-02` 员工详情响应新增只读字段 `displayName`，类型为 `String`；新增请求和员工列表不增加该字段。

只在 `EmployeeResponse` 中完成以下修改：

1. 新增 `private final String displayName`；
2. 在构造方法末尾增加同类型参数并完成赋值；
3. 新增 `getDisplayName()`；
4. 执行 `clean test` 并保存结果。

Review时确认 `EmployeeCreateRequest` 和 `EmployeeListItemResponse` 没有被连带增加 `displayName`。这说明三个对象可以根据各自规格独立变化。

这只是边界练习。记录证据后恢复 `EmployeeResponse`，重新执行 `clean test`，保持下一章仍使用本章完整示例的四个详情字段。

### 练习3：完成影响调查和指摘修正

有人提出下面的修改：

> 为了少写一个类，删除 `EmployeeListItemResponse`，所有员工接口都返回 `EmployeeResponse`，前端列表页面不显示邮箱即可。

请完成：

1. 指出违反的规格编号和字段；
2. 说明“前端不显示”和“后端不返回”的区别；
3. 列出受影响的响应类、未来转换代码、接口验证和安全Review项目；
4. 给出修正意见，并保留列表专用响应对象。

验收记录至少包含：指摘位置、影响、依据、修改方案和确认结果。

## 十五、本章稳定状态

完成练习并恢复临时规格变更后，新增文件应保持：

```text
src/main/java/com/example/employee/dto/
├── request/
│   └── EmployeeCreateRequest.java
└── response/
    ├── EmployeeListItemResponse.java
    └── EmployeeResponse.java
```

此时你应能够：

1. 从接口规格判断字段属于输入还是输出；
2. 解释新增请求为什么没有 `id`；
3. 解释列表响应为什么没有 `email`；
4. 区分请求DTO、响应对象和未来数据库对象；
5. 根据目标规格逐项转换对象，而不是无条件复制所有字段；
6. 说明DTO、Response和VO名称必须结合项目约定判断；
7. 识别 `@JsonProperty`、`@JsonIgnore`、`@JsonInclude` 和 `@JsonFormat` 对JSON的影响；
8. 解释Jackson映射规则不能代替职责明确的数据对象。

下一章将在不改变这些字段职责的前提下，让Spring把JSON请求写入 `EmployeeCreateRequest`，并把响应对象转换为JSON返回。
