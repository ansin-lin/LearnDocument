
# Spring Data JPA 与 JSON 字段过滤注解

---

## JPA 核心注解

### @Entity

**作用**：声明持久化实体。  
**定义位置**：类上。

```java
@Entity
@Table(name = "users")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(unique = true)
  private String email;
}
```

### @Id / @GeneratedValue

**作用**：主键及其生成策略。`IDENTITY`、`SEQUENCE`、`TABLE`、`AUTO`。

### @Column / @Table / @UniqueConstraint / @Index

**作用**：字段/表结构映射与约束。

### @Transient

**作用**：不参与持久化。

### 关联映射：@OneToOne / @OneToMany / @ManyToOne / @ManyToMany + @JoinColumn / @JoinTable

**作用**：定义实体关系与连接信息。

```java
@OneToMany(mappedBy="user", cascade=CascadeType.ALL, orphanRemoval=true)
private List<Order> orders = new ArrayList<>();
```

### @Enumerated / @Lob / @Version

**作用**：枚举存储、长文本/二进制、大对象版本乐观锁。

---

## Spring Data JPA 注解

### @Repository

**作用**：持久层标识并启用异常转换（也可省略，`JpaRepository` 子接口会被自动代理）。

### @EnableJpaRepositories

**作用**：启用仓库接口扫描（Boot 自动配置通常已开启）。

### @EntityGraph

**作用**：定义抓取策略以避免 N+1（指定属性在查询时 EAGER 抓取）。

```java
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();
```

### @Query / @Modifying / @Param

**作用**：声明自定义 JPQL/SQL 查询与更新。

```java
@Modifying
@Query("update User u set u.name=:name where u.id=:id")
int rename(@Param("id") Long id, @Param("name") String name);
```

### 派生查询关键字

**说明**：通过方法名解析生成查询，如 `findByEmailAndStatusOrderByCreatedAtDesc`。

---

## Jackson JSON 字段控制与过滤

### @JsonIgnore / @JsonIgnoreProperties

**作用**：忽略字段或指定字段集合的序列化/反序列化。

```java
@JsonIgnoreProperties({"password", "salt"})
public class UserVO { /*...*/ }
```

### @JsonProperty

**作用**：重命名字段名或仅序列化/反序列化。

```java
@JsonProperty("user_name")
private String name;
```

### @JsonInclude

**作用**：控制包含策略，如 `NON_NULL`、`NON_EMPTY`、`NON_DEFAULT`。

### @JsonFormat

**作用**：格式化时间/数字。

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
private LocalDateTime createdAt;
```

### @JsonView

**作用**：按视图分组输出不同字段集合。控制器方法上指定视图类。

```java
public class Views { public static class Public{} public static class Detail extends Public{} }

public class UserVO {
  @JsonView(Views.Public.class) Long id;
  @JsonView(Views.Public.class) String name;
  @JsonView(Views.Detail.class) String email;
}

@GetMapping("/users/{id}")
@JsonView(Views.Public.class)
public UserVO get(@PathVariable Long id){ /*...*/ }
```

### @JsonFilter（动态过滤）

**作用**：基于过滤器 ID 动态决定输出字段，需要配置 `FilterProvider`。

```java
@JsonFilter("userFilter")
public class UserVO { Long id; String name; String email; String phone; }

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

---

## JPA + 校验 + JSON 综合示例

```java
@Entity
public class Product {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank @Column(nullable=false, length=100)
  private String name;

  @Positive @Column(nullable=false)
  private BigDecimal price;

  @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="Asia/Tokyo")
  private LocalDateTime createdAt = LocalDateTime.now();
}

@RestController
@RequestMapping("/products")
class ProductApi {
  @PostMapping
  public Product create(@Valid @RequestBody Product p){ return repo.save(p); }
}
```
