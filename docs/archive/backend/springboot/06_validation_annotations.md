# 数据校验与验证注解（JSR 380 / Bean Validation 2.0）

> 覆盖触发入口、常用字段约束、级联/分组校验、请求体与请求参数校验、容器元素校验、自定义约束、消息与国际化以及错误处理实践。建议在 Spring Boot 中引入：`spring-boot-starter-validation`（默认集成 Hibernate Validator 作为实现）。

---

## 一、触发入口：@Valid 与 @Validated

### 1. 作用与差异

- `@Valid`：标准注解（javax/jakarta），**触发校验**并**支持级联**（嵌套对象继续校验）；**不支持分组**。
- `@Validated`：Spring 注解，**触发校验**且**支持分组**（`groups`、`@GroupSequence`），级联需在属性上再标 `@Valid`。

### 2. 定义位置

- 控制器方法参数、服务层方法参数、类上（用于方法级校验）

```java
@PostMapping("/users")
public User create(@Validated(Create.class) @RequestBody UserDTO dto) { ... }

@Validated // 类级开启方法参数校验（配合 AOP 代理生效）
@Service
public class UserService {
  public void update(@NotNull Long id, @Valid UserDTO dto) { ... }
}
```

> 说明：方法级校验需要将类交给 Spring 管理并通过 **代理调用** 才会生效（与事务/缓存同理）。

---

## 二、常用字段约束（内置）

> 绝大多数约束在值为 `null` 时**默认视为通过**（除非显式要求非空，如 `@NotNull/@NotBlank/@NotEmpty`）。

| 注解 | 说明 | 示例 |
| --- | --- | --- |
| `@NotNull` | 不为 `null` | `@NotNull Long id` |
| `@NotEmpty` | 长度 > 0（字符串/集合/数组） | `@NotEmpty List<String> tags` |
| `@NotBlank` | 字符串非空白 | `@NotBlank String name` |
| `@Email` | 邮箱格式 | `@Email String email` |
| `@Size(min,max)` | 字符串/集合长度范围 | `@Size(min=8,max=20) String pwd` |
| `@Pattern(regexp)` | 正则匹配 | `@Pattern(regexp="^1[3-9]\d{9}$") String phone` |
| `@Min/@Max` | 数值下/上限（含） | `@Min(1) int page` |
| `@DecimalMin/@DecimalMax` | 小数边界 | `@DecimalMin("0.01") BigDecimal price` |
| `@Positive/@PositiveOrZero` | 正数/非负 | `@Positive Integer count` |
| `@Negative/@NegativeOrZero` | 负数/非正 | |
| `@Past/@PastOrPresent` | 早于/早于等于现在 | `@Past LocalDate birthday` |
| `@Future/@FutureOrPresent` | 晚于/晚于等于现在 | `@Future LocalDateTime expireAt` |
| `@AssertTrue/@AssertFalse` | 必须为真/假 | `@AssertTrue Boolean enabled` |
| `@Null` | 必须为 `null` | 用于创建场景的 `id` |

**DTO 示例：**

```java
public class UserDTO {
  @NotBlank(message="姓名不能为空")
  private String name;

  @Email(message="邮箱格式不正确")
  private String email;

  @Size(min = 8, max = 20, message="密码长度应在8~20之间")
  private String password;

  @Pattern(regexp="^1[3-9]\d{9}$", message="手机号格式错误")
  private String phone;
  // getter/setter
}
```

---

## 三、级联校验（嵌套对象）：@Valid

在**属性**或**集合元素**上标注 `@Valid`，触发对嵌套对象的继续校验。

```java
class AddressDTO {
  @NotBlank String city;
  @NotBlank String street;
}

class OrderDTO {
  @Valid @NotNull
  private AddressDTO address; // 递归校验
}
```

> 注意：`@Validated` 不会自动级联，**嵌套属性需加 `@Valid`**。

---

## 四、分组校验与顺序：@Validated(groups) 与 @GroupSequence

### 1. 分组接口

```java
public interface Create {}
public interface Update {}
```

### 2. 在同一字段上根据分组定义不同规则

```java
class UserDTO {
  @Null(groups = Create.class)    // 新建时由数据库生成
  @NotNull(groups = Update.class) // 更新时必须提供
  private Long id;

  @NotBlank(groups = {Create.class, Update.class})
  private String name;
}
```

### 3. 控制器中按场景触发

```java
@PostMapping("/users") 
void create(@Validated(Create.class) @RequestBody UserDTO dto){}

@PutMapping("/users") 
void update(@Validated(Update.class) @RequestBody UserDTO dto){}
```

### 4. 校验顺序

使用 `@GroupSequence` 指定分组执行顺序：前一组不通过则停止后续校验。

```java
@GroupSequence({Create.class, Update.class})
public interface CheckOrder {}
```

---

## 五、请求体与请求参数校验的区别

### 1. 请求体（JSON/XML）

- 写法：`@Valid/@Validated + @RequestBody`
- 失败异常：`MethodArgumentNotValidException`（绑定结果里有 FieldError 列表）

```java
@PostMapping("/users")
public User create(@Valid @RequestBody UserDTO dto) { ... }
```

### 2. 请求参数/路径变量（Query/Path/Form）

- 类上加：`@Validated`
- 在方法参数上直接标注约束；
- 失败异常：`ConstraintViolationException`

```java
@Validated
@RestController
class UserApi {
  @GetMapping("/u")
  public String u(@NotBlank @RequestParam String name,
                  @Min(1) @RequestParam int page) { return name; }
}
```

### 3. 使用 `BindingResult` 拦截异常（可选）

```java
@PostMapping("/users")
public ResponseEntity<?> create(@Valid @RequestBody UserDTO dto, BindingResult br) {
  if (br.hasErrors()) {
    var errors = br.getFieldErrors().stream()
      .map(e -> Map.of("field", e.getField(), "msg", e.getDefaultMessage()))
      .toList();
    return ResponseEntity.badRequest().body(Map.of("message","参数错误","errors",errors));
  }
  ...
}
```

---

## 六、容器元素校验（Bean Validation 2.0）

可对集合、Map、Optional 的**元素**进行约束：

```java
public class MailGroupDTO {
  @NotEmpty
  private List<@Email String> receivers; // 列表中每个元素必须是合法邮箱

  private Map<@NotBlank String, @Positive Integer> quota; // key/value 同时约束

  private Optional<@Pattern(regexp="^\+?\d+$") String> hotline;
}
```

---

## 七、自定义约束注解

### 1. 定义注解

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {
  String message() default "手机号格式错误";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
```

### 2. 实现验证器

```java
public class MobileValidator implements ConstraintValidator<Mobile, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext c) {
    if (value == null) return true; // 遵循“null 视为通过”的约定
    return value.matches("^1[3-9]\d{9}$");
  }
}
```

### 3. 组合注解与单一违例报告

可以通过在自定义注解上**组合多个约束**，并用 `@ReportAsSingleViolation` 让多个失败合并为一个消息。

```java
@Pattern(regexp="\d+")
@Size(min=11, max=11)
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface ChinaMobile {
  String message() default "手机号格式不正确";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
```

---

## 八、消息与国际化（Message Interpolation）

### 1. 消息来源优先级

1) 注解 `message` 属性 >  
2) `ValidationMessages.properties`（类路径根）>  
3) Spring 的 `MessageSource`（`messages.properties` 等）

### 2. 使用占位符与参数

```java
@Size(min=8, max=20, message="{user.password.size}")
private String password;
```

`ValidationMessages.properties`：

```pom
user.password.size=密码长度必须在 {min} 到 {max} 之间
```

### 3. 国际化

提供 `ValidationMessages_zh_CN.properties`、`ValidationMessages_en_US.properties` 等文件并配置 `LocaleResolver`。

---

## 九、错误处理与返回格式实践

### 1. 全局异常处理（推荐）

```java
@RestControllerAdvice
public class GlobalExceptionAdvice {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleBind(MethodArgumentNotValidException e) {
    var errors = e.getBindingResult().getFieldErrors().stream()
      .map(f -> Map.of("field", f.getField(), "msg", f.getDefaultMessage()))
      .toList();
    return Map.of("message", "参数校验失败", "errors", errors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleConstraint(ConstraintViolationException e) {
    var errors = e.getConstraintViolations().stream()
      .map(v -> Map.of("field", v.getPropertyPath().toString(), "msg", v.getMessage()))
      .toList();
    return Map.of("message", "参数非法", "errors", errors);
  }
}
```

### 2. 直接返回统一响应体

定义统一响应模型（如 `ApiError`）并在全局异常中返回，便于前端统一处理。

---

## 十、与 MVC 绑定、转换的配合

- 自定义类型转换：实现 `Converter<S,T>` 并在 `WebMvcConfigurer#addFormatters` 中注册，用于将 `@RequestParam` 字符串转为目标类型。  
- 日期格式化：`@DateTimeFormat`（入参）搭配 `@JsonFormat`（出参）保证一致性。  
- 枚举校验：可自定义注解或在枚举中实现 `fromCode` 并配合 `Converter`。

```java
@GetMapping("/logs")
public List<Log> list(@RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate day){ ... }
```

---

## 十一、Hibernate Validator 常用扩展（实现方特性）

> 以下注解并非规范的一部分，但在实际项目中非常常用：

| 注解 | 说明 |
| --- | --- |
| `@Length(min,max)` | 字符串长度（类似 `@Size`，但只用于字符串） |
| `@Range(min,max)` | 数值范围 |
| `@URL` | URL 格式 |
| `@UUID` | UUID 格式 |
| `@CreditCardNumber` | 信用卡号码（Luhn） |
| `@ISBN` | ISBN |
| `@CPF/@CNPJ` | 巴西个人/公司注册号 |

---

## 十二、实战配方（Recipes）

1) **创建 vs 更新**：用分组区分规则；`id` 在 Create 组 `@Null`，在 Update 组 `@NotNull`。  
2) **搜索条件 DTO**：所有字段可空，必要字段用逻辑判断，不滥用 `@NotNull`。  
3) **分页参数**：`@Min(1) int page`，`@Range(min=1,max=200) int size`。  
4) **容器元素**：`List<@NotBlank String> tags`、`Map<@NotBlank String, @Positive Integer> quota`。  
5) **服务层方法校验**：在类上 `@Validated`，方法参数用标准约束，避免仅在 Controller 校验。  
6) **避免 NPE**：大多约束对 `null` 通过，若需要必填请显式 `@NotNull/@NotBlank`。  
7) **统一错误返回**：使用 `@RestControllerAdvice`，不要在每个控制器重复拼装错误。

---
