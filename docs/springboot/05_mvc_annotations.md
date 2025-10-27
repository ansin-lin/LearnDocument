
# Spring MVC 与参数绑定注解

> 包含控制器、请求映射、参数解析、返回处理等核心注解。

---

## @Controller / @RestController

**作用**：声明控制器；`@RestController` = `@Controller` + `@ResponseBody`。  
**定义位置**：类上。

---

## @RequestMapping（复合注解基底）

**作用**：映射请求路径、方法、参数条件、消费/生产媒体类型等。`@GetMapping`、`@PostMapping` 等是其派生复合注解。  
**定义位置**：类或方法上。

```java
@RequestMapping(path = "/users", produces = "application/json")
public class UserController {}
```

### @GetMapping / @PostMapping / @PutMapping / @DeleteMapping / @PatchMapping

**作用**：分别限定 HTTP 方法的 `@RequestMapping` 语法糖。  
**定义位置**：方法上。

```java
@GetMapping("/{id}")
public User detail(@PathVariable Long id) { /*...*/ }
```

---

## @PathVariable

**作用**：获取路径中的变量并进行类型转换。  
**定义位置**：方法参数。

```java
@GetMapping("/books/{isbn}")
public Book get(@PathVariable("isbn") String isbn) { /*...*/ }
```

---

## @RequestParam

**作用**：绑定查询参数、表单字段到方法参数；支持默认值与必填控制。  
**定义位置**：方法参数。

```java
@GetMapping("/search")
public List<User> search(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(required = false) String keyword) { /*...*/ }
```

---

## @RequestBody

**作用**：将请求体反序列化为对象（如 JSON -> POJO），需要 `HttpMessageConverter`。  
**定义位置**：方法参数。

```java
@PostMapping("/users")
public User create(@Valid @RequestBody UserCreateDTO dto) { /*...*/ }
```

---

## @ResponseBody

**作用**：将方法返回值通过消息转换器写入响应体（通常 JSON）。  
**定义位置**：方法或类上。

---

## @ModelAttribute

**作用**：将请求参数绑定到对象、或在调用控制器方法前向模型添加属性。  
**定义位置**：方法参数、方法上。

```java
@PostMapping("/submit")
public String submit(@ModelAttribute Form form) { /* form 已绑定 */ return "ok"; }
```

---

## @RequestHeader / @CookieValue / @MatrixVariable

**作用**：分别绑定请求头、Cookie、矩阵变量到方法参数。  
**定义位置**：方法参数。

```java
@GetMapping("/me")
public User me(@RequestHeader("X-Auth") String token) { /*...*/ }
```

---

## @CrossOrigin

**作用**：启用方法/类级别的 CORS。  
**定义位置**：类或方法上。

```java
@CrossOrigin(origins = "https://example.com")
@GetMapping("/public")
public String pub(){ return "ok"; }
```

---

## @ResponseStatus

**作用**：声明固定的响应状态码；也常用于异常处理方法上。  
**定义位置**：方法或异常类上。

```java
@ResponseStatus(HttpStatus.CREATED)
@PostMapping("/users")
public void create(@RequestBody UserDTO dto) {}
```

---

## @ControllerAdvice / @RestControllerAdvice（全局增强）

**作用**：全局异常、数据绑定、模型属性等增强；`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`。  
**定义位置**：类上。

```java
@RestControllerAdvice
public class GlobalAdvice {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Map<String, Object> bindErr(MethodArgumentNotValidException e) { /*...*/ return Map.of(); }
}
```

---

## 参数校验桥接：@Valid / @Validated

**作用**：触发 Bean Validation 校验；`@Validated` 支持校验分组。  
**定义位置**：方法参数、类上。详见第 06 章。

---

## 其他常用

- `@InitBinder`：定制数据绑定与类型转换器。
- `@SessionAttributes` / `@SessionAttribute`：声明/读取会话属性。
- `@RequestPart`：处理 `multipart/form-data` 的部分。
- `@ExceptionHandler`：控制器或全局异常处理方法。
- `@JsonView`：按视图分组控制 JSON 字段输出（详见第 08 章）。
