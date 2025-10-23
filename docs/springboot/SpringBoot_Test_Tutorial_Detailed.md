# 🧪 Spring Boot 单元测试与工具教学（Eclipse 环境 · 适配 Spring Boot 3.5）

> 本文是完整教学课件，包含三段典型测试代码逐行讲解（Service / Mapper / Controller）  
> 以及常用测试工具（JUnit5、Mockito、Spring Test、MockMvc、Jacoco 等）的系统说明与使用场景分析。  
> 适用于企业内部培训、教学课程或自学笔记整理。

---

## Part A：三段代码示例精讲（先案例、后原理）

---

### 第1章：Service 层单元测试（Mockito + JUnit5）

**教学目标**：掌握如何使用 Mockito 和 JUnit5 进行“纯业务逻辑”单元测试。  

#### 示例代码（带详细注释）

```java
// 启用 Mockito 扩展，使 @Mock/@InjectMocks 注解在 JUnit5 中生效
@ExtendWith(MockitoExtension.class)
public class AdminReportingDetailsServiceTest {

    // @Mock：创建一个模拟对象，不访问真实数据库或接口
    @Mock
    private UserTblMapper userTblMapper;

    // @InjectMocks：自动创建被测对象实例，并将上方 mock 注入进去
    @InjectMocks
    private AdminReportingDetailsService adminReportingDetailsService;

    // @BeforeEach：每个测试用例前执行，用于初始化 mock 环境
    @BeforeEach
    void setUp() {
        // 手动初始化 mock，如果没有 @ExtendWith，也可使用此方式
        MockitoAnnotations.openMocks(this);
    }

    // @Test：声明一个测试方法
    @Test
    void testGetAdminUserList_NormalCase() {
        // 准备输入参数
        A0301VueForm form = new A0301VueForm();

        // 定义 mock 行为：当调用 mapper.getAdminUserList(any()) 时，返回一个包含新对象的列表
        when(userTblMapper.getAdminUserList(any())).thenReturn(List.of(new UserTblPo()));

        // 调用被测方法
        var result = adminReportingDetailsService.getAdminUserList(form);

        // 验证返回结果不为空
        assertNotNull(result);
    }
}
```

#### 教学要点

- 不依赖 Spring 容器，测试速度快。
- 使用 Mockito 实现“依赖替身”。
- 当 Service 依赖多个 DAO，可通过多个 @Mock 模拟。
- 可添加 `verify(mapper, times(1)).getAdminUserList(any())` 验证方法是否被调用。

---

### 第2章：Mapper 层测试（MyBatis 专用切片）

**教学目标**：掌握 `@MybatisTest` 与数据库交互验证。

#### 示例代码

```java
// @MybatisTest：仅加载 MyBatis Bean，不启动完整 Spring 容器
@MybatisTest
// @AutoConfigureTestDatabase：控制数据源替换策略
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserTblMapperTest {

    // 注入 MyBatis Mapper
    @Autowired
    private UserTblMapper userTblMapper;

    // 注入 JdbcTemplate 用于初始化测试数据
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 每个测试前执行
    @BeforeEach
    void setUp() {
        // 创建表并插入初始数据
        jdbcTemplate.execute("CREATE TABLE user_tbl (user_cd INT, first_name VARCHAR(50));");
        jdbcTemplate.execute("INSERT INTO user_tbl VALUES (1, 'Tom');");
    }

    @Test
    void testFindById() {
        // 测试 Mapper 查询功能
        assertEquals("Tom", userTblMapper.findById(1).getFirstName());
    }
}
```

#### 教学要点

- `@MybatisTest` 会自动加载 `SqlSessionFactory`、`Mapper` 等 MyBatis 组件。
- 默认使用内存数据库（H2）；可通过 `replace = NONE` 使用真实库。
- 若想测试事务回滚或多表关系，可添加 `@Transactional`。

---

### 第3章：Controller 层测试（Mockito 风格）

**教学目标**：学习如何不启动服务器，测试 Controller 层逻辑。

#### 示例代码

```java
// 启用 Mockito 扩展
@ExtendWith(MockitoExtension.class)
class AdminListControllerTest {

    // 模拟 Service 层依赖
    @Mock
    private AdminReportingDetailsService adminReportingDetailsService;

    // 注入被测 Controller
    @InjectMocks
    private AdminListController adminListController;

    @Test
    void search_NullForm_ReturnsBadRequest() {
        // 调用 Controller 方法，传入空参数
        var res = adminListController.search(null);

        // 验证状态码为 400
        assertEquals(400, res.getCode());
    }
}
```

#### 教学要点

- Mockito 模拟 Service，直接调用 Controller 方法。
- 不依赖 Web 环境，执行速度快。
- 若需测试 HTTP 请求，可使用 `MockMvc`。
- 与 `@WebMvcTest` 对比：
    - Mockito：仅验证业务逻辑；
    - MockMvc：验证请求路由、JSON、状态码。

---

## Part B：测试工具与注解系统讲解（使用场景 + 常用注解）

---

### 第4章：JUnit 5（核心测试框架）

**使用场景**：用于管理测试生命周期与执行。

| 注解 | 说明 |
|------|------|
| `@Test` | 定义测试方法 |
| `@BeforeEach` / `@AfterEach` | 每个测试前/后执行 |
| `@BeforeAll` / `@AfterAll` | 所有测试前/后执行（需 static） |
| `@DisplayName` | 自定义测试名称 |
| `@Disabled` | 跳过测试方法 |

#### 常用断言

```java
assertEquals(expected, actual);
assertTrue(condition);
assertThrows(Exception.class, () -> { ... });
assertAll(() -> {...}, () -> {...});
```

#### 提示
- 可结合 AssertJ 做链式断言。
- 可与 Eclipse 的 JUnit 视图联动，绿色代表通过，红色代表失败。

---

### 第5章：Mockito（Mock 框架）

**使用场景**：模拟外部依赖（DAO、HTTP 调用、服务接口）。  

| 注解 | 说明 |
|------|------|
| `@ExtendWith(MockitoExtension.class)` | 启用 Mockito 扩展 |
| `@Mock` | 创建模拟对象 |
| `@InjectMocks` | 注入 Mock 对象到被测类 |
| `@Spy` | 创建部分真实对象（部分 Mock） |
| `@Captor` | 捕获方法参数以便断言 |

#### 核心方法
```java
when(mock.method()).thenReturn(value);  // 定义行为
verify(mock, times(1)).method();        // 验证交互
any(), eq(), argThat(...)               // 参数匹配器
```

#### 提示
- 不可混用参数匹配器和原值。
- 对于依赖较多的类，应只 Mock 必要部分，避免误测。

---

### 第6章：Spring Test 生态

**使用场景**：按层级加载最小 Spring 环境，提高测试性能。

| 注解 | 功能 |
|------|------|
| `@SpringBootTest` | 启动完整容器（集成测试） |
| `@WebMvcTest` | 仅加载 Web 层 |
| `@DataJpaTest` | 测试 JPA Repository |
| `@MybatisTest` | 测试 MyBatis Mapper |
| `@MockBean` | 向 Spring 容器中注入 Mock Bean |
| `@ExtendWith(SpringExtension.class)` | JUnit5 扩展，用于加载 Spring 环境 |

#### 注意
- `@WebMvcTest` 无法直接注入 Service，需要使用 `@MockBean`。
- “切片测试”理念：只加载当前层所需 Bean。

---

### 第7章：MockMvc（HTTP 层模拟）

**使用场景**：模拟 HTTP 请求与响应，无需启动服务器。

#### 示例
```java
@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  void testGetUser() throws Exception {
    when(userService.find(1L)).thenReturn(new User(1L, "Tom"));
    mockMvc.perform(get("/users/1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("Tom"));
  }
}
```

#### 常用断言
- `status().isOk()`
- `jsonPath()`
- `content().string(...)`

---

### 第8章：数据层测试（JPA/MyBatis）

| 场景 | 注解 | 特性 |
|------|------|------|
| JPA | `@DataJpaTest` | 自动回滚、嵌入式 DB |
| MyBatis | `@MybatisTest` | 测试 Mapper 映射、SQL 逻辑 |

#### 示例
```java
@DataJpaTest
class UserRepositoryTest {
    @Autowired
    UserRepository repo;

    @Test
    void testSave() {
        repo.save(new User("Tom"));
        assertThat(repo.findAll()).isNotEmpty();
    }
}
```

---

### 第9章：断言库（AssertJ / Hamcrest）

**AssertJ**
```java
assertThat(list)
  .isNotEmpty()
  .extracting(User::getName)
  .contains("Tom");
```

**Hamcrest**
```java
assertThat(value, allOf(greaterThan(0), lessThan(10)));
```

| 对比 | AssertJ | Hamcrest |
|------|----------|----------|
| 可读性 | 链式调用 | 匹配器风格 |
| 用途 | 通用逻辑 | MockMvc/REST 断言常用 |

---

### 第10章：覆盖率与报告（Jacoco）

**用途**：生成代码覆盖率报告。

#### 配置示例（pom.xml）
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

#### 报告说明
- 运行 `mvn test` 后在 `/target/site/jacoco/index.html` 查看。
- 建议行覆盖率 ≥ 80%，分支覆盖率 ≥ 60%。

---

## 🧭 教学流程建议（总时长约 90~120 分钟）

| 时间 | 环节 | 内容 |
|------|------|------|
| 15 min | 理论导入 | 单元测试与集成测试区别、Spring Test 切片理念 |
| 30 min | 实例讲解 | 讲解 Service / Mapper / Controller 三段代码 |
| 30 min | 工具深讲 | Mockito、WebMvcTest、MybatisTest、DataJpaTest 实战 |
| 15 min | 覆盖率与报告 | Jacoco 报告与常见错误 |
| 10-30 min | 练习 | 编写 MockMvc + 异常用例 |

---

**结语**  
通过本课程学习，你应能：
- 独立编写单元测试与集成测试；
- 理解各层的 Mock / 实测区别；
- 熟练使用 Eclipse + Maven + JUnit + Mockito 环境完成测试开发。
