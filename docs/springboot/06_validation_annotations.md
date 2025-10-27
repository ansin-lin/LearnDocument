
# 数据校验与验证注解（JSR 380/Bean Validation 2.0）

> 覆盖字段验证、对象校验、请求体/请求参数校验与校验分组。需引入 `spring-boot-starter-validation`。

---

## 触发入口：@Valid / @Validated

**作用**：触发 Bean Validation；`@Validated` 支持 `groups` 与 `@GroupSequence`；`@Valid` 不支持分组但支持级联校验。  
**定义位置**：控制器方法参数、服务层方法参数、类上。

```java
@PostMapping("/users")
public User create(@Validated(Create.class) @RequestBody UserDTO dto) { /*...*/ }
```

---

## 常用字段约束

- `@NotNull`：字段不为 `null`（对字符串不判断空白）。
- `@NotEmpty`：集合/数组/字符串长度 > 0。
- `@NotBlank`：字符串非空且去除空白后长度 > 0。
- `@Email`：邮箱格式。
- `@Size(min,max)`：字符串、集合大小范围。
- `@Pattern(regexp)`：正则匹配。
- `@Min/@Max`、`@DecimalMin/@DecimalMax`：最小/最大值。
- `@Positive/@PositiveOrZero`、`@Negative/@NegativeOrZero`：正负性约束。
- `@Past/@PastOrPresent`、`@Future/@FutureOrPresent`：时间约束。
- `@AssertTrue/@AssertFalse`：布尔断言。
- `@Null`：必须为 `null`。

```java
public class UserDTO {
  @NotBlank String name;
  @Email String email;
  @Size(min=8,max=20) String password;
  @Pattern(regexp="^1[3-9]\d{9}$") String phone;
}
```

---

## 级联校验：@Valid

**作用**：对嵌套对象继续进行校验。  
**定义位置**：字段或方法参数。

```java
class OrderDTO { @Valid @NotNull AddressDTO address; }
```

---

## 分组校验：@Validated(groups=...) 与 @GroupSequence

**作用**：根据使用场景激活不同规则。  
**示例**：

```java
interface Create {}
interface Update {}

class UserDTO {
  @Null(groups=Create.class)    // 新建时由数据库生成
  @NotNull(groups=Update.class) // 更新时必须提供
  Long id;
}

@PostMapping("/users") void create(@Validated(Create.class) @RequestBody UserDTO dto){}
@PutMapping("/users") void update(@Validated(Update.class) @RequestBody UserDTO dto){}
```

---

## 请求体与请求参数校验

- **请求体**：`@Valid/@Validated + @RequestBody`，失败抛出 `MethodArgumentNotValidException`。  
- **请求参数/路径变量**：`@Validated` 作用在类上 + 具体参数字段上使用约束，失败抛出 `ConstraintViolationException`。

```java
@Validated
@RestController
class UserApi {
  @GetMapping("/u")
  public String u(@NotBlank @RequestParam String name) { return name; }
}
```

---

## 自定义约束注解

**作用**：实现业务特定校验。  
**步骤**：定义注解 + `ConstraintValidator<A, T>` 实现 + 注册。

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {
  String message() default "手机号格式错误";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}

public class MobileValidator implements ConstraintValidator<Mobile, String> {
  public boolean isValid(String value, ConstraintValidatorContext c) {
    return value != null && value.matches("^1[3-9]\d{9}$");
  }
}
```

---

## 校验失败结果定制

- 全局处理：`@RestControllerAdvice` + `@ExceptionHandler(MethodArgumentNotValidException|ConstraintViolationException)`。
- 指定状态码：`@ResponseStatus(HttpStatus.BAD_REQUEST)`。
