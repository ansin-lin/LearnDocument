# A08 Header、Cookie与文件上传

本附录只讲三类HTTP扩展输入：请求头、Cookie和multipart文件。路径参数、查询参数与JSON请求体仍以第6章为主；认证授权、Session登录和文件持久化不在本附录中展开。

## 一、前置知识与完成结果

开始前应完成第6章，能够运行Employee工程并区分路由匹配、参数绑定和Controller执行阶段。完成后应能读取Header和Cookie，接收JSON part与文件part，并说明客户端提供的文件名、媒体类型和大小为什么都必须由服务端约束。
## 二、完整实验：三类扩展输入

Employee主线已经覆盖路径、查询参数和JSON请求体。本节使用可删除的 `dilab` 独立实验补充Header、Cookie和multipart文件输入；实验不修改员工接口，结束后删除实验文件并回归原测试。

### 1. 完整实验文件

新建 `src/main/java/com/example/employee/dilab/FileMetadataRequest.java`：

```java
package com.example.employee.dilab;

public class FileMetadataRequest {

    private String description;

    public FileMetadataRequest() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```

新建 `src/main/java/com/example/employee/dilab/HttpInputDemoController.java`：

```java
package com.example.employee.dilab;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class HttpInputDemoController {

    @GetMapping("/di-lab/request-info")
    public String requestInfo(
            @RequestHeader("User-Agent") String userAgent,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId) {
        return userAgent + " / " + requestId;
    }

    @GetMapping("/di-lab/language")
    public String language(
            @CookieValue(value = "language", required = false)
            String language) {
        return language == null ? "unset" : language;
    }

    @PostMapping(
            value = "/di-lab/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(
            @RequestPart("metadata") FileMetadataRequest metadata,
            @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "empty";
        }
        return metadata.getDescription()
                + " / " + file.getOriginalFilename()
                + " / " + file.getSize();
    }
}
```

这两个类只证明不同HTTP位置怎样进入Java参数，不保存文件、不修改数据库，也不构成文件管理功能。

### 2. @RequestHeader读取请求头

`@RequestHeader` 属于 `org.springframework.web.bind.annotation`，写在Controller方法参数上。Spring MVC匹配Controller方法后，从HTTP请求头取值并转换成参数类型：

| 属性 | 当前值 | 可接受的值 | 默认和结果 |
| --- | --- | --- | --- |
| `value` | `User-Agent`、`X-Request-Id` | 合法Header名称 | 指定从哪个请求头读取 |
| `required` | true或false | `true`、`false` | 默认true；缺少必填Header通常在进入方法前返回400 |

HTTP Header名称在协议语义上不区分大小写，但项目仍应统一写法，便于规格、日志和测试对照。`required = false` 时Header不存在会得到null；若配置 `defaultValue`，则可得到指定默认字符串。

```text
@PathVariable  → URL路径片段
@RequestParam  → Query String或表单参数
@RequestHeader → HTTP Header
@RequestBody   → 整个HTTP Body，由消息转换器读取
```

`Authorization`、`X-Request-Id` 等Header都来自客户端或中间代理。除非有经过认证的可信网关和明确安全设计，不能因为客户端写了 `X-Role: ADMIN` 就授予权限。

### 3. @CookieValue读取Cookie

`@CookieValue` 同样属于Spring Web注解，写在Controller方法参数上。本例从请求的Cookie头中读取名为 `language` 的Cookie值。`value` 是Cookie名称，`required = false` 表示缺少时允许进入方法并得到null；默认 `required = true` 时缺少Cookie通常返回400。

Cookie由浏览器保存并随符合规则的请求发送，但仍是HTTP请求数据。第15章登录使用的JSESSIONID通常由Servlet容器和Spring Security读取并恢复Session，业务Controller不需要自行读取、解析或记录JSESSIONID。

### 4. multipart/form-data与@RequestPart

`multipart/form-data` 可以把一次HTTP请求拆成多个part：

```text
POST /di-lab/files
Content-Type: multipart/form-data; boundary=...

part metadata → application/json → FileMetadataRequest
part file     → text/plain       → MultipartFile
```

`@RequestPart` 属于Spring Web注解，`value` 指定part名称，默认必填。本例中Jackson把 `metadata` part转换成 `FileMetadataRequest`，multipart解析器把 `file` part包装成 `MultipartFile`。part名称错误、必填part缺失或metadata不是可转换JSON时，Controller方法不会正常执行。

`MediaType.MULTIPART_FORM_DATA_VALUE` 是字符串常量 `multipart/form-data`。`consumes` 限制方法只处理这种请求媒体类型；发送普通 `application/json` 会因媒体类型不匹配而失败。

### 5. MultipartFile能读取什么

`MultipartFile` 的完整名称是 `org.springframework.web.multipart.MultipartFile`，表示本次请求中的上传文件，不等于服务器上已经保存的文件：

| 方法 | 返回 | 用途与边界 |
| --- | --- | --- |
| `getOriginalFilename()` | `String` | 客户端提供的原文件名，只用于显示或审计参考 |
| `getContentType()` | `String` | 客户端声明的媒体类型，不能单独作为安全判定 |
| `getSize()` | `long` | 文件字节数 |
| `isEmpty()` | `boolean` | 没有内容时为true |
| `getBytes()` | `byte[]` | 一次把内容读入内存，只适合已限制的小文件 |
| `getInputStream()` | `InputStream` | 流式读取；调用方需要按Java I/O规则关闭流 |

不能把 `getOriginalFilename()` 直接拼接为服务器保存路径，因为文件名来自客户端，可能包含路径片段、冲突名称或不安全字符。应由服务端生成存储标识、限定目录并校验规范化后的目标路径。`getContentType()` 也由请求声明，重要文件类型还要检查实际内容或使用可靠的内容检测策略。

文件大小应在进入业务处理前设置上限。独立实验可临时在 `application.yml` 中加入：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 6MB
```

`max-file-size` 限制单个文件，`max-request-size` 限制包含所有part的整个请求。项目还要根据业务类型限制文件数量、扩展名、内容和保存权限，不能只依赖浏览器前端校验。

### 6. 验证与恢复

使用浏览器开发者工具、Postman或其他能分别设置Header、Cookie和multipart part的HTTP客户端验证：

| 请求 | 条件 | 预期 |
| --- | --- | --- |
| `GET /di-lab/request-info` | 带User-Agent，不带X-Request-Id | 200，第二部分为null |
| `GET /di-lab/language` | Cookie为`language=ja` | 200，正文为ja |
| `POST /di-lab/files` | metadata为JSON、file为非空小文件 | 200，返回说明、原文件名和大小 |
| `POST /di-lab/files` | 缺少file part | 400，方法不正常执行 |
| `POST /di-lab/files` | 单文件超过5MB | 解析阶段拒绝，不进入业务保存 |

证据中不要保存Session ID、Authorization值或上传文件中的个人信息。实验后删除 `dilab` 两个Java文件，移除临时multipart配置，重新执行 `clean test`，确认Employee主线接口不变。

Spring MVC对Header、Cookie和multipart参数的正式说明见[Annotated Controller方法参数索引](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/)。

## 三、附录验收

提交四类证据：Header可选值、Cookie值、合法multipart请求、缺少part或超限文件的失败结果。证据必须隐藏Authorization、Session ID和个人信息。最后删除 `dilab` 文件和临时multipart配置，执行 `clean test`，确认Employee主线没有变化。