# Spring MVC 与参数绑定注解

> 包含控制器、请求映射、参数解析、返回处理等核心注解。

---

## 🧩 @Controller / @RestController

**作用：**  

- `@Controller`：用于声明 MVC 控制器，返回视图（模板页）。  
- `@RestController`：复合注解，等价于 `@Controller` + `@ResponseBody`，返回数据（JSON/XML）。  

**定义位置：**  
类上。

```java
@Controller
public class PageController {
    @GetMapping("/home")
    public String home() { return "home"; } // 返回视图名
}

@RestController
@RequestMapping("/api")
public class ApiController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("msg", "ok");
    }
}
```

---

## 🧩 @RequestMapping（复合注解基底）

**作用：**  
定义请求路径、HTTP 方法、参数、Header 条件、媒体类型等映射规则。  
`@GetMapping`、`@PostMapping` 等是其语法糖。

**定义位置：**  
类或方法上。

```java
@RequestMapping(path = "/users", produces = "application/json")
public class UserController {}
```

**常用属性：**

| 属性 | 说明 |
|------|------|
| `path` / `value` | 映射的 URL 路径 |
| `method` | 限定 HTTP 方法（GET/POST/PUT/DELETE） |
| `params` | 限定请求参数 |
| `headers` | 限定请求头 |
| `consumes` | 指定请求体格式（如 application/json） |
| `produces` | 指定响应格式（如 application/json） |

---

### ✅ 派生注解

| 注解 | 对应 HTTP 方法 |
|------|----------------|
| `@GetMapping` | GET |
| `@PostMapping` | POST |
| `@PutMapping` | PUT |
| `@DeleteMapping` | DELETE |
| `@PatchMapping` | PATCH |

```java
@GetMapping("/{id}")
public User detail(@PathVariable Long id) { return userService.findById(id); }
```

---

## 🧩 @PathVariable

**作用：**  
绑定 URL 路径变量到方法参数，可自动类型转换。

**定义位置：**  
方法参数上。

```java
@GetMapping("/books/{isbn}")
public Book get(@PathVariable("isbn") String isbn) {
    return bookService.findByIsbn(isbn);
}
```

**补充说明：**

- 路径变量名必须与 URL 模板匹配；  
- 若变量名一致可省略 `("isbn")`。

---

## 🧩 @RequestParam

**作用：**  
绑定查询参数或表单字段到方法参数。可指定默认值与是否必填。

**定义位置：**  
方法参数上。

```java
@GetMapping("/search")
public List<User> search(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) String keyword) {
    return userService.search(keyword, page);
}
```

**常用属性：**

| 属性 | 说明 |
|------|------|
| `value` | 参数名 |
| `required` | 是否必填（默认 true） |
| `defaultValue` | 默认值 |

---

## 🧩 @RequestBody

**作用：**  
将请求体内容（JSON/XML）反序列化为 Java 对象。  
依赖 `HttpMessageConverter` 自动完成数据转换。

**定义位置：**  
方法参数上。

```java
@PostMapping("/users")
public User create(@Valid @RequestBody UserCreateDTO dto) {
    return userService.create(dto);
}
```

**补充说明：**

- 需在请求头中声明 `Content-Type: application/json`；  
- 常配合 `@Valid` 或 `@Validated` 做数据校验。

---

## 🧩 @ResponseBody

**作用：**  
将方法返回值通过 `HttpMessageConverter` 转为 JSON/XML 并写入响应体。  

**定义位置：**  
类或方法上。

```java
@ResponseBody
@GetMapping("/version")
public Map<String, String> version() {
    return Map.of("version", "1.0.0");
}
```

---

## 🧩 @ModelAttribute

**作用：**  
绑定请求参数到对象（自动填充字段），或在控制器方法调用前往模型中添加数据。  

**定义位置：**  
方法参数、方法上。

```java
@PostMapping("/submit")
public String submit(@ModelAttribute Form form) {
    // 表单参数自动绑定到 Form 实例
    return "ok";
}
```

**扩展用法：**

```java
@ModelAttribute
public void addGlobalAttr(Model model) {
    model.addAttribute("project", "DemoApp");
}
```

---

## 🧩 @RequestHeader / @CookieValue / @MatrixVariable

**作用：**  
分别从请求头、Cookie、矩阵变量中提取数据绑定到参数。  

**定义位置：**  
方法参数上。

```java
@GetMapping("/me")
public User me(@RequestHeader("X-Auth") String token) {
    return userService.findByToken(token);
}
```

**示例扩展：**

```java
@GetMapping("/preferences")
public String prefs(@CookieValue("theme") String theme) {
    return "当前主题：" + theme;
}
```

---

## 🧩 @CrossOrigin

**作用：**  
启用跨域访问（CORS），可限定来源域、方法、头部。  

**定义位置：**  
类或方法上。

```java
@CrossOrigin(origins = "https://example.com", methods = {RequestMethod.GET, RequestMethod.POST})
@GetMapping("/public")
public String pub() { return "ok"; }
```

**补充说明：**

- 可全局配置 `WebMvcConfigurer#addCorsMappings()`；  
- 适用于 REST API 的前后端分离项目。

---

## 🧩 @ResponseStatus

**作用：**  
声明方法执行后固定返回的 HTTP 状态码。  
常用于 REST API 或异常类上。

**定义位置：**  
方法或异常类上。

```java
@ResponseStatus(HttpStatus.CREATED)
@PostMapping("/users")
public void create(@RequestBody UserDTO dto) {}
```

**在异常类上使用：**

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {}
```

---

## 🧩 @ControllerAdvice / @RestControllerAdvice（全局增强）

**作用：**  
统一异常处理、数据绑定、模型属性注入。  
`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`。  

**定义位置：**  
类上。

```java
@RestControllerAdvice
public class GlobalAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> bindErr(MethodArgumentNotValidException e) {
        return Map.of("error", "参数校验失败", "detail", e.getMessage());
    }
}
```

**补充说明：**

- 可结合 `@InitBinder`、`@ModelAttribute` 定制全局行为。

---

## 🧩 参数校验桥接：@Valid / @Validated

**作用：**  
触发 Bean Validation（JSR 380）校验机制。  
`@Validated` 支持分组校验。

**定义位置：**  
类或方法参数上。

```java
@PostMapping("/users")
public User create(@Validated(CreateGroup.class) @RequestBody UserDTO dto) {
    return userService.create(dto);
}
```

---

## 🧩 其他常用注解

| 注解 | 说明 |
|------|------|
| `@InitBinder` | 定制数据绑定与类型转换器 |
| `@SessionAttributes` / `@SessionAttribute` | 声明 / 读取会话属性 |
| `@RequestPart` | 处理 `multipart/form-data` 上传部分 |
| `@ExceptionHandler` | 局部或全局异常处理方法 |
| `@JsonView` | 按视图分组控制 JSON 字段输出（详见 JSON 章节） |

---
