# Spring Data JPA 与 JSON 字段过滤注解（详细说明）

> 本章涵盖 JPA 实体映射注解、Spring Data 仓库扩展注解、以及 Jackson JSON 字段控制与动态过滤实践。

---

## 一、JPA 核心注解

### 1.1 `@Entity`

**作用：** 标识持久化实体类，对应数据库表中的一行记录。  
**定义位置：** 类上。

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true)
    private String email;
}
```

**补充说明：**

- 类必须有**无参构造函数**（JPA 反射实例化）。
- 实体默认表名为类名（可通过 `@Table` 修改）。

---

### 1.2 `@Id` 与 `@GeneratedValue`

**作用：** 定义主键及主键生成策略。

| 策略 | 说明 |
|------|------|
| `IDENTITY` | 数据库自增（MySQL 常用） |
| `SEQUENCE` | 序列（Oracle/PostgreSQL） |
| `TABLE` | 使用表维护主键序列 |
| `AUTO` | 由 JPA 自动选择 |

---

### 1.3 `@Column` / `@Table` / `@UniqueConstraint` / `@Index`

**作用：** 映射数据库表与列属性。

```java
@Table(
  name = "users",
  uniqueConstraints = @UniqueConstraint(columnNames = {"email"}),
  indexes = {@Index(name = "idx_name", columnList = "name")}
)
public class User { ... }
```

**常用属性：**

- `nullable`：是否允许为空
- `unique`：是否唯一
- `length`：字符字段长度
- `precision` / `scale`：数值精度

---

### 1.4 `@Transient`

**作用：** 排除字段不参与持久化（类似于 Java `transient` 但更明确）。

```java
@Transient
private String token; // 仅用于内存，不入库
```

---

### 1.5 关联映射

**作用：** 建立实体间关系并管理外键。

```java
@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
```

**说明：**

- `fetch`：加载策略（`EAGER`/`LAZY`）
- `cascade`：级联操作类型（`ALL`、`PERSIST`、`REMOVE` 等）
- `orphanRemoval`：孤儿删除，父对象移除后自动删除子对象

---

### 1.6 其他常见注解

| 注解 | 说明 |
|------|------|
| `@Enumerated(EnumType.STRING)` | 枚举以字符串存储 |
| `@Lob` | 大对象（TEXT/BLOB） |
| `@Version` | 乐观锁版本号 |
| `@Temporal` | 旧版日期类型标注（`Date`、`Calendar`） |

---

## 二、Spring Data JPA 特有注解

### 2.1 `@Repository`

**作用：** 标识持久层组件，并启用异常转换。  
> 对继承自 `JpaRepository` 的接口可省略，Spring 会自动识别。

---

### 2.2 `@EnableJpaRepositories`

**作用：** 开启 JPA 仓库扫描功能。  
**定义位置：** 配置类上。  
**Spring Boot 默认已开启。**

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.repo")
public class JpaConfig {}
```

---

### 2.3 `@EntityGraph`

**作用：** 指定在查询时一起抓取的关联字段，避免 N+1 查询。

```java
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();
```

**说明：**

- 默认抓取策略 `EAGER`，但可通过 `@EntityGraph` 动态控制。
- 常用于 `findAll()` 或自定义查询。

---

### 2.4 `@Query` / `@Modifying` / `@Param`

**作用：** 自定义 JPQL 或原生 SQL 查询。

```java
@Query("SELECT u FROM User u WHERE u.email = :email")
User findByEmail(@Param("email") String email);

@Modifying
@Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
int rename(@Param("id") Long id, @Param("name") String name);
```

**说明：**

- `@Modifying` 必须搭配 `@Transactional` 才能执行更新/删除。
- 可通过 `nativeQuery = true` 启用原生 SQL。

---

### 2.5 派生查询（Derived Query）

**作用：** 根据方法名自动生成查询语句。

```java
List<User> findByEmailAndStatusOrderByCreatedAtDesc(String email, int status);
```

**常见关键字：**

- `findBy` / `existsBy` / `countBy`
- `And` / `Or`
- `Between` / `LessThan` / `GreaterThan`
- `Like` / `StartingWith` / `EndingWith`
- `OrderBy...Asc/Desc`

---

## 三、Jackson JSON 字段控制注解

### 3.1 `@JsonIgnore` / `@JsonIgnoreProperties`

**作用：** 忽略字段或字段集合的序列化与反序列化。

```java
@JsonIgnoreProperties({"password", "salt"})
public class UserVO {
  private String username;
  private String password;
  private String salt;
}
```

**说明：**

- `@JsonIgnore` 用于单个字段。
- `@JsonIgnoreProperties` 用于类级，支持多个字段。

---

### 3.2 `@JsonProperty`

**作用：** 重命名字段、或控制单向序列化/反序列化。

```java
@JsonProperty("user_name")
private String name;
```

**说明：**

- 可用 `access = JsonProperty.Access.WRITE_ONLY` 实现“只反序列化、不序列化”。

---

### 3.3 `@JsonInclude`

**作用：** 控制哪些字段被包含。

| 模式 | 说明 |
|------|------|
| `ALWAYS` | 默认，所有字段均输出 |
| `NON_NULL` | 忽略 null |
| `NON_EMPTY` | 忽略空字符串、空集合 |
| `NON_DEFAULT` | 忽略默认值 |

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
private String phone;
```

---

### 3.4 `@JsonFormat`

**作用：** 格式化时间或数字输出。

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
private LocalDateTime createdAt;
```

---

### 3.5 `@JsonView`

**作用：** 通过视图分组输出不同层级字段集合。

```java
public class Views {
  public static class Public {}
  public static class Detail extends Public {}
}

public class UserVO {
  @JsonView(Views.Public.class) Long id;
  @JsonView(Views.Public.class) String name;
  @JsonView(Views.Detail.class) String email;
}

@GetMapping("/users/{id}")
@JsonView(Views.Public.class)
public UserVO get(@PathVariable Long id){ return service.getUser(id); }
```

**说明：**

- `Detail` 继承 `Public`，输出时包含父类字段。

---

### 3.6 `@JsonFilter`（动态过滤）

**作用：** 通过 `FilterProvider` 动态指定序列化字段。

```java
@JsonFilter("userFilter")
public class UserVO {
  Long id; String name; String email; String phone;
}

@GetMapping("/users/{id}")
public MappingJacksonValue get(@PathVariable Long id) {
  UserVO vo = service.get(id);
  var value = new MappingJacksonValue(vo);
  var filters = new SimpleFilterProvider()
      .addFilter("userFilter", SimpleBeanPropertyFilter.filterOutAllExcept("id","name"));
  value.setFilters(filters);
  return value;
}
```

**补充：**

- 需在 `ObjectMapper` 中注册 FilterProvider。
- 常用于动态字段输出（如用户隐私控制）。

---

## 四、综合示例：JPA + Validation + JSON

```java
@Entity
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false, length = 100)
  private String name;

  @Positive
  @Column(nullable = false)
  private BigDecimal price;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
  private LocalDateTime createdAt = LocalDateTime.now();
}

@RestController
@RequestMapping("/products")
public class ProductApi {
  private final ProductRepository repo;
  public ProductApi(ProductRepository repo){ this.repo = repo; }

  @PostMapping
  public Product create(@Valid @RequestBody Product p) {
    return repo.save(p);
  }

  @GetMapping("/{id}")
  public Product get(@PathVariable Long id){
    return repo.findById(id).orElseThrow();
  }
}
```

---

## 五、最佳实践总结

1. **实体设计**：字段必须与业务逻辑匹配；避免双向多对多；尽量用单向关系。
2. **延迟加载**：默认使用 `LAZY`，必要时显式 `@EntityGraph`。
3. **DTO/VO 分离**：输出层使用 `@JsonView` 或 MapStruct，避免直接暴露实体。
4. **校验 + JSON**：搭配 Bean Validation 与 Jackson 注解实现前后端一致约束。
5. **动态过滤**：`@JsonFilter` 用于隐私控制或接口字段定制。
6. **分页与排序**：使用 `Pageable` 参数，避免手写分页逻辑。
7. **异常处理**：结合全局 `@RestControllerAdvice` 捕获 `DataIntegrityViolationException`。

---
