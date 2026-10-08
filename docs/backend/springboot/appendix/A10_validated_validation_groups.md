# A10 使用@Validated选择校验分组

第8章使用`@Valid`一次执行员工新增请求的全部约束。真实项目有时会让同一个数据对象经过不同处理阶段，例如“保存草稿”允许暂时缺少部门和邮箱，“正式提交”则要求全部填写。校验分组可以让调用入口明确选择本次需要执行的约束。

本附录只讲`@Validated`在Spring MVC请求DTO上的分组用法。Service方法级校验、自定义约束和动态业务规则不在本附录中展开。

## 一、@Valid与@Validated的区别

| 注解 | 所属框架 | 当前用法 | 能否直接选择校验组 |
| --- | --- | --- | --- |
| `@Valid` | Jakarta Validation | 触发对象及嵌套对象的标准校验 | 不能在注解参数中选择 |
| `@Validated` | Spring | 触发校验，并把指定类型作为校验组传给Validator | 可以 |

`@Validated`的完整名称是`org.springframework.validation.annotation.Validated`，可以写在类型、方法或参数等位置。本实验把它写在`@RequestBody`参数上，括号中的`DraftCheck.class`或`SubmitCheck.class`决定本次执行哪个分组。

`@Validated` 只有 `value` 一个属性，类型是 `Class<?>[]`，默认值是空数组。全部属性的一行写法如下；只有一个分组时，外层花括号可以省略：

```java
@Validated(value = {DraftCheck.class})
```

本附录后续使用的 `@Validated(DraftCheck.class)` 与上面写法含义相同。

两者都可以与`@RequestBody`组合触发请求对象校验。分组不是按注解书写顺序执行，也不会自动判断当前业务阶段；由Controller入口明确选择。

## 二、实验规格

本实验使用可删除的`validationlab`包，不修改Employee主线DTO。

| 字段 | 保存草稿 | 正式提交 |
| --- | --- | --- |
| `name` | 必填，最多50字符 | 必填，最多50字符 |
| `department` | 可以暂时省略 | 必填，最多50字符 |
| `email` | 可以省略；填写后必须符合邮箱格式 | 必填、符合邮箱格式、最多100字符 |

需要实现两个接口：

| 方法和路径 | 选择的分组 | 成功正文 |
| --- | --- | --- |
| `POST /validation-lab/employees/draft` | `DraftCheck` | `草稿校验通过` |
| `POST /validation-lab/employees/submit` | `SubmitCheck` | `正式提交校验通过` |

这两个接口只验证输入，不保存数据库。

## 三、完整示例

创建下面四个文件：

```text
src/main/java/com/example/employee/validationlab/
├── DraftCheck.java
├── SubmitCheck.java
├── EmployeeProfileRequest.java
└── ValidationGroupDemoController.java
```

### 1. DraftCheck.java

```java
package com.example.employee.validationlab;

public interface DraftCheck {
}
```

### 2. SubmitCheck.java

```java
package com.example.employee.validationlab;

public interface SubmitCheck {
}
```

这两个空接口没有业务方法，只作为类型安全的分组标识。使用`Class`对象选择分组，比在多个文件中约定`"draft"`、`"submit"`字符串更容易由编译器检查。

### 3. EmployeeProfileRequest.java

```java
package com.example.employee.validationlab;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmployeeProfileRequest {

    @NotBlank(
            groups = {DraftCheck.class, SubmitCheck.class},
            message = "员工姓名不能为空")
    @Size(
            groups = {DraftCheck.class, SubmitCheck.class},
            max = 50,
            message = "员工姓名不能超过50个字符")
    private String name;

    @NotBlank(
            groups = SubmitCheck.class,
            message = "正式提交时部门不能为空")
    @Size(
            groups = SubmitCheck.class,
            max = 50,
            message = "部门不能超过50个字符")
    private String department;

    @NotBlank(
            groups = SubmitCheck.class,
            message = "正式提交时邮箱不能为空")
    @Email(
            groups = {DraftCheck.class, SubmitCheck.class},
            message = "邮箱格式不正确")
    @Size(
            groups = {DraftCheck.class, SubmitCheck.class},
            max = 100,
            message = "邮箱不能超过100个字符")
    private String email;

    public EmployeeProfileRequest() {
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

所有约束的`groups`参数接受一个或多个分组类型：

| 写法 | 本次含义 |
| --- | --- |
| `groups = DraftCheck.class` | 只属于草稿组 |
| `groups = SubmitCheck.class` | 只属于正式提交组 |
| `groups = {DraftCheck.class, SubmitCheck.class}` | 两个阶段都执行 |

没有填写`groups`的约束属于Jakarta Validation的`Default`组。调用`@Validated(DraftCheck.class)`时，不会自动同时执行`Default`组。因此一旦采用分组，应逐项确认每条约束属于哪些组，不能假设未写`groups`的规则仍会执行。

`@Email`和`@Size`不会单独把`null`当成必填失败，所以草稿可以省略邮箱；正式提交通过`@NotBlank(groups = SubmitCheck.class)`补上必填规则。

### 4. ValidationGroupDemoController.java

```java
package com.example.employee.validationlab;

import com.example.employee.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/validation-lab/employees")
public class ValidationGroupDemoController {

    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<String>> saveDraft(
            @Validated(DraftCheck.class)
            @RequestBody EmployeeProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("草稿校验通过"));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<String>> submit(
            @Validated(SubmitCheck.class)
            @RequestBody EmployeeProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("正式提交校验通过"));
    }
}
```

两个方法接收相同的Java类型，但`@Validated`选择的分组不同：

```text
/draft  → DraftCheck  → 检查草稿阶段约束
/submit → SubmitCheck → 检查正式提交约束
```

请求JSON先由Jackson创建`EmployeeProfileRequest`，随后Spring根据`@Validated`选择分组并执行约束。失败时仍产生`MethodArgumentNotValidException`，所以第8章的全局异常处理器可以继续把字段错误转换成400响应。

## 四、验证分组差异

启动应用后，在另一个PowerShell窗口准备只有姓名的请求：

```powershell
$body = @{
    name = "Sato"
} | ConvertTo-Json
```

保存草稿：

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/validation-lab/employees/draft" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

预期返回200和`草稿校验通过`。

把相同正文正式提交：

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/validation-lab/employees/submit" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

预期返回400，字段错误至少包含`department`和`email`。

再准备完整正文：

```powershell
$completeBody = @{
    name = "Sato"
    department = "Development"
    email = "sato@example.com"
} | ConvertTo-Json
```

提交到`/submit`后，预期返回200和`正式提交校验通过`。最后把草稿邮箱改成`not-an-email`，验证即使邮箱可省略，一旦填写仍会执行`@Email`。

## 五、执行过程与职责边界

```text
HTTP请求
  → @RequestBody读取JSON
  → @Validated选择本次校验组
  → Validator只执行属于该组的约束
      ├─ 通过：进入Controller
      └─ 失败：MethodArgumentNotValidException
```

校验分组适合表达同一数据结构在不同入口或处理阶段的格式要求。它不适合代替Service业务规则：

- “正式提交时邮箱必填”可以是分组约束。
- “这个部门当前是否允许招聘”需要Service查询业务状态。
- “登录用户是否有提交权限”属于授权判断。

不要把数据库查询或权限判断写进分组接口。

## 六、类级@Validated为什么是另一种场景

既存Service中还可能看到`@Validated`写在类上，并把`@NotNull`、`@Min`等直接写在方法参数或返回值上。这属于方法校验，不是本附录演示的`@RequestBody`分组校验。

两种写法要分开判断：

| 位置 | 主要目的 |
| --- | --- |
| Controller请求DTO参数上的`@Validated(Group.class)` | 为当前请求对象选择校验组 |
| Service类上的`@Validated` | 让受Spring管理的方法参数或返回值进入方法校验机制 |

方法校验还涉及代理、异常类型和Spring MVC版本行为。没有实际需求时，不要同时在Controller类、方法和参数上重复添加`@Validated`。

## 七、常见问题与Review

| 现象 | 原因 | 修正 |
| --- | --- | --- |
| 选择分组后原有约束不再执行 | 约束仍属于`Default`组 | 明确补充目标`groups`，或按规格同时选择Default |
| 草稿也要求部门必填 | `@NotBlank`错误地加入Draft组 | 从该约束的草稿组中移除 |
| 草稿填写非法邮箱却通过 | `@Email`只属于Submit组 | 让格式约束同时属于两个组 |
| Controller已经进入后才发现字段错误 | 参数上遗漏`@Validated` | 在`@RequestBody`参数上选择分组 |
| 用分组判断数据库中的部门状态 | 把业务规则放进DTO约束 | 移到Service并保留明确异常 |
| 所有接口共用越来越多的分组 | DTO职责已经过度复杂 | 重新评估是否应拆分请求对象 |

Review时应从接口规格出发，逐字段制作“入口×约束”矩阵，不能只看到`groups`编译通过就认为规则正确。

## 八、操作练习与清理

临时增加`ReviewCheck`分组：姓名和邮箱必填，部门可以省略。新增独立`/review`入口并验证缺少姓名、非法邮箱和合法请求。记录每条约束属于哪些组以及三个入口的实际状态码。

完成后删除`validationlab`包中的所有实验文件，执行`.\mvnw.cmd clean test`，并回归第8章原接口。主线DTO继续使用`@Valid`，不保留没有正式规格支持的分组。

Spring MVC明确支持`@RequestBody`与`@Valid`或`@Validated`组合；默认失败会形成`MethodArgumentNotValidException`。参考[Spring MVC @RequestBody说明](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)和[Validated API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/validation/annotation/Validated.html)。
