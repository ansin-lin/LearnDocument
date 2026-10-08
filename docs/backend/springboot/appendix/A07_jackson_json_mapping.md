# A07 Jackson字段映射与输出规则

本附录只讲Java对象与JSON字段之间的映射规则。它建立在第5章的数据边界和第6章的序列化、反序列化之上，不替代请求DTO和响应DTO的职责划分。

## 一、前置知识与实验边界

开始前应完成第6章，并能说明Jackson何时把JSON转成Java对象、何时把Java对象写成JSON。本实验使用独立的识读类，不改变Employee主线接口。
## 二、完整实验：JSON与Java字段映射

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

## 三、验证与Review任务

为实验类临时增加一个只返回该对象的测试接口，分别准备非null和null数据，核对实际JSON字段名、被忽略字段、空值省略和日期格式。完成后删除临时接口与实验类并执行 `clean test`。

Review时至少回答：这些注解改变的是JSON表示还是业务字段权限；字段改名是否同时影响请求与响应；日期文本是否携带时区。不能用 `@JsonIgnore` 代替明确的响应DTO边界。