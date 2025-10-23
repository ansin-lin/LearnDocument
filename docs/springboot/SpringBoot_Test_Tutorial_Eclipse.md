# 🧪 Spring Boot 测试教程

> 版本：Spring Boot 3.5.0  
> 教学目标：掌握 Spring Boot 的单元测试与集成测试体系，能够独立编写 Service、Mapper、Controller 三层测试。

---

## 一、测试的基本概念

| 名称 | 含义 |
|------|------|
| **JUnit** | Java 的单元测试框架，Spring Boot 默认使用 JUnit 5（JUnit Jupiter） |
| **单元测试（Unit Test）** | 测试代码中最小单元（如 Service / Util）逻辑是否正确 |
| **集成测试（Integration Test）** | 验证多个组件之间的协作是否正常（Controller + Service + DB） |
| **Mock（模拟）** | 使用 Mockito 模拟依赖对象行为，避免依赖外部资源（数据库、API等） |

---

## 二、Eclipse 环境依赖配置

在 Maven 项目中，Spring Boot 3.5.0 默认使用 JUnit 5，无需额外引入 JUnit，只需在 `<dependencies>` 中加入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

该依赖自动包含：JUnit5、Mockito、AssertJ、Hamcrest、Spring Test、JSONPath 等常用测试库。

---

## 三、JUnit 常用注解

| 注解 | 说明 |
|------|------|
| `@Test` | 声明测试方法 |
| `@BeforeEach` / `@AfterEach` | 每个测试方法前/后执行 |
| `@BeforeAll` / `@AfterAll` | 所有测试开始/结束执行（需 static） |
| `@DisplayName` | 定义测试名称 |
| `@Disabled` | 跳过测试 |

---

## 四、Spring Boot 测试常用注解

```java
@SpringBootTest —— 启动完整 Spring 容器（用于集成测试）
@WebMvcTest —— 仅加载 Controller 层，用于 Web 层单测
@DataJpaTest —— 仅加载 Repository 层，默认自动回滚
@MockBean —— 向容器注入 Mock 对象
@ExtendWith(MockitoExtension.class) —— 启用 Mockito 支持
```

---

## 五、测试层级与策略

| 层级 | 注解 | 说明 |
|------|------|------|
| Service 层 | `@ExtendWith(MockitoExtension.class)` | Mock DAO 层依赖，测试业务逻辑 |
| Controller 层 | `@WebMvcTest / Mockito` | 模拟 HTTP 请求与响应验证 |
| Repository 层 | `@DataJpaTest / @MybatisTest` | 使用 H2 内存数据库测试 SQL 逻辑 |

---

## 六、Service 层示例

```java
// 使用 JUnit5 的扩展机制，加载 Mockito 框架功能，使 @Mock/@InjectMocks 注解生效
@ExtendWith(MockitoExtension.class)
public class AdminReportingDetailsServiceTest {

    // @Mock：创建一个模拟对象（Mock 对象），不会连接真实数据库或外部资源
    @Mock
    private UserTblMapper userTblMapper;

    // @InjectMocks：自动将上面的 Mock 对象注入到被测试的目标类中（自动依赖注入）
    @InjectMocks
    private AdminReportingDetailsService adminReportingDetailsService;

    // @BeforeEach：在每个测试方法执行之前都会执行，用于初始化测试环境
    @BeforeEach
    void setUp() {
        // 打开 Mockito 注解支持，使 @Mock/@InjectMocks 的注入生效
        MockitoAnnotations.openMocks(this);
    }

    // @Test：表示这是一个测试方法，JUnit 会自动执行此方法
    @Test
    void testGetAdminUserList_NormalCase() {
        // 创建输入参数对象，用于模拟 Service 接口调用
        A0301VueForm form = new A0301VueForm();

        // when(...).thenReturn(...)：定义 Mock 行为——当调用 getAdminUserList(any参数) 时返回一个伪造的 List
        when(userTblMapper.getAdminUserList(any())).thenReturn(List.of(new UserTblPo()));

        // 调用被测方法（Service逻辑）
        var result = adminReportingDetailsService.getAdminUserList(form);

        // 断言结果非空（验证逻辑执行正确）
        assertNotNull(result);
    }
}
```

---

## 七、Mapper 层示例（MyBatis）

```java
// @MybatisTest：仅加载 MyBatis 相关的 Bean（如 Mapper 接口），不启动整个 Spring Boot 容器
@MybatisTest
// @AutoConfigureTestDatabase：控制测试数据库的行为，
// replace = NONE 表示不使用内存数据库，而是使用真实配置的数据库连接（如 MySQL）
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserTblMapperTest {

    // @Autowired：自动注入 Mapper 对象，Spring 会在上下文中找到并注入对应的 Bean
    @Autowired
    private UserTblMapper userTblMapper;

    // 注入 JdbcTemplate，用于直接执行 SQL 语句（方便创建或初始化测试数据）
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 每个测试方法执行前运行，用于准备数据库环境
    @BeforeEach
    void setUp() {
        // 直接执行 SQL 建表语句（如果不存在则创建）
        jdbcTemplate.execute("CREATE TABLE user_tbl (user_cd INT, first_name VARCHAR(50));");
    }

    // 定义一个测试方法
    @Test
    void testFindById() {
        // 断言 mapper 执行查询后返回的 first_name 值为 "Tom"
        assertEquals("Tom", userTblMapper.findById(1).getFirstName());
    }
}

```

---

## 八、Controller 层示例

```java
// 启用 Mockito 扩展功能，用于识别 @Mock/@InjectMocks 注解
@ExtendWith(MockitoExtension.class)
class AdminListControllerTest {

    // @Mock：创建一个 AdminReportingDetailsService 的模拟对象，不会调用真实逻辑
    @Mock
    private AdminReportingDetailsService adminReportingDetailsService;

    // @InjectMocks：自动创建 AdminListController 实例，并将上面的 mock 对象注入进去
    @InjectMocks
    private AdminListController adminListController;

    // @Test：声明一个单元测试方法
    @Test
    void search_NullForm_ReturnsBadRequest() {
        // 调用 Controller 方法，传入 null 模拟无效请求参数
        var res = adminListController.search(null);

        // 断言返回结果中的状态码为 400（表示请求参数错误）
        assertEquals(400, res.getCode());
    }
}

```

---

## 九、测试目录结构建议（Eclipse）

```java
src
├── main/java/com/example/project
│   ├── controller/AdminListController.java
│   ├── service/AdminReportingDetailsService.java
│   └── mapper/UserTblMapper.java
└── test/java/com/example/project
    ├── controller/AdminListControllerTest.java
    ├── service/AdminReportingDetailsServiceTest.java
    └── mapper/UserTblMapperTest.java
```

---

## 十、课堂总结与扩展

✔ 理解单元测试与集成测试的区别  
✔ 掌握 Mockito 模拟依赖  
✔ 能够独立完成 Service、Mapper、Controller 层测试  
✔ 扩展任务：使用 Jacoco 测试覆盖率分析、在 GitHub Actions 中自动执行测试。

---
