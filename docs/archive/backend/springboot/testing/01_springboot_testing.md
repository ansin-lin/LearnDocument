# 🧪 Spring Boot 测试

> 课程目标：
>> 理解 Spring Boot 测试的**目的与体系**，
>> 熟练使用 **JUnit5、Mockito、Spring Test 切片、MockMvc、DataJpaTest / MybatisTest、AssertJ / Hamcrest、Jacoco** 等工具；
>> 能够在企业项目中高质量地编写 **Service / Mapper / Controller** 三层测试。  
>> 适用读者：Java/Spring 开发者、测试开发、教学培训。

---


## 测试的目的与意义

**为什么要写测试？**

- 验证业务逻辑正确性，防止“改一处、崩全局”。
- 降低维护成本，便于代码重构与升级。
- 配合 CI/CD，自动化保障发布质量。
- 为复杂系统（多模块/多服务）提供**回归保护网**。

**单元测试 vs 集成测试**

| 类型 | 范围 | 速度 | 依赖 | 典型工具 |
|---|---|---|---|---|
| 单元测试 Unit | 单个类/方法 | 快 | 无外部资源（Mock 依赖） | JUnit5 + Mockito |
| 集成测试 IT | 多组件协作 | 慢 | 需要 Spring 容器/DB/HTTP | SpringBootTest / MockMvc / DataJpaTest / MybatisTest |

> 建议：**单元测试覆盖大多数业务分支**，核心调用链再补充集成测试。

---

## 测试框架与依赖配置（Eclipse）

### Maven 依赖（Spring Boot 3.5.x）

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

**该 starter 包含：** JUnit5、Mockito、AssertJ、Hamcrest、Spring Test、JSONPath 等。

### 在 Eclipse 中运行测试

- 右键类/方法 → **Run As → JUnit Test**
- 快捷键：`Alt + Shift + X`, `T`
- 打开 **JUnit** 视图查看执行结果（绿条 ✅ / 红条 ❌）。

> 小贴士：在多模块工程中，确保 `maven-surefire-plugin` 版本兼容 JUnit5（通常 Spring Boot 默认合适）。

---

## 测试工具与使用场景总览

| 工具 | 主要用途 | 典型场景 | 是否随 starter 内置 |
|---|---|---|---|
| **JUnit5** | 定义与执行测试、管理生命周期 | 所有测试 | ✅ |
| **Mockito** | 模拟依赖/桩行为、验证交互 | Service 层 Mock DAO/外部服务 | ✅ |
| **Spring Test** | 加载 Spring 上下文（切片/全量） | 集成测试 | ✅ |
| **MockMvc** | 模拟 HTTP 请求/响应 | Controller 层 | ✅ |
| **@DataJpaTest** | Repository 层（JPA） | 数据访问 | ✅ |
| **@MybatisTest** | Mapper 层（MyBatis） | SQL/映射验证 | ✅（spring-boot-starter-test + mybatis-spring-boot） |
| **AssertJ/Hamcrest** | 丰富断言语义 | 集合/对象/JSON 断言 | ✅ |

---

## JUnit 5：框架与生命周期

**常用注解**

- @Test
    - 含义：声明一个测试方法。
    - 定义位置：方法上。
    - 说明：用于标识普通测试用例。方法无返回值、无参数。
- @DisplayName("说明")
    - 含义：设置测试的显示名称。
    - 定义位置：类上 / 方法上。
    - 说明：为报告输出或控制台展示提供可读标题。
- @BeforeEach
    - 含义：每个测试执行前运行。
    - 定义位置：方法上。
    - 说明：用于初始化测试数据、打开连接等操作。
- @AfterEach
    - 含义：每个测试执行后运行。
    - 定义位置：方法上。
    - 说明：用于清理资源、关闭连接、重置状态等操作。
- @BeforeAll
    - 含义：所有测试执行前运行一次。
    - 定义位置：方法上（通常为 static）。
    - 说明：用于加载配置、初始化全局资源；需为 static 方法，或在类上使用 @TestInstance(PER_CLASS) 时可省略 static。
- @AfterAll
    - 含义：所有测试执行后运行一次。

**执行顺序**

```java
@BeforeAll → @BeforeEach → @Test → @AfterEach → @AfterAll
```

---

## Mockito：模拟依赖与交互验证

**注解与作用**

- @ExtendWith(MockitoExtension.class)
    - 功能：启用 Mockito 扩展（JUnit5 机制）。
    - 典型用法：写在测试类上，让 @Mock / @InjectMocks 自动初始化。
    - 使用位置：类上（Class 级）。
    - 说明：相当于告诉 JUnit 启用 Mockito 框架的支持，使 Mock 注解自动生效。
- @Mock
    - 功能：创建 Mock 对象。
    - 典型用法：模拟依赖组件，如 DAO、外部 API、Service 等。
    - 使用位置：字段上（Field 级）。
    - 说明：用于创建依赖的“虚拟替身”，防止访问真实数据库或外部服务。
- @InjectMocks
    - 功能：将上方的 Mock 自动注入被测类中。
    - 典型用法：自动创建被测类实例，并将使用 @Mock 标注的依赖注入进去。
    - 使用位置：字段上（Field 级）。
    - 说明：帮助我们快速组装被测试对象（System Under Test）。
- @Spy
    - 功能：创建部分真实对象（半 Mock）。
    - 典型用法：当只想替换部分方法逻辑、保留原始实现时使用。
    - 使用位置：字段上（Field 级）。
    - 说明：@Spy 对象会执行真实方法，除非被 when(...).thenReturn(...) 重写。
- @Captor
    - 功能：创建参数捕获器。
    - 典型用法：用于捕获 Mock 调用参数，以便在断言时验证传入参数的值。
    - 使用位置：字段上（Field 级） 或 方法参数上。
    - 说明：常与 verify(mock).method(captor.capture()) 搭配使用。

**核心 API**

```java
when(dep.method(arg)).thenReturn(value);   // 行为桩
when(dep.method(arg)).thenThrow(ex);
verify(dep, times(1)).method(arg);         // 交互验证
doNothing().when(dep).voidMethod();        // void 方法
ArgumentCaptor<T> c = ArgumentCaptor.forClass(T.class);
verify(dep).method(c.capture());
assertEquals("x", c.getValue());
```

**注意**：匹配器要成对使用（`any()` 与 `eq(x)` 混用会报错）。

---

## Spring Test 切片与 MockMvc

**切片注解**

- @SpringBootTest
    - 适用场景：全量集成测试。
    - 加载范围：完整的 Spring Boot 容器。
    - 说明：用于启动整个应用上下文进行全面测试；启动速度较慢、资源消耗最大，通常用于验证多模块协作或真实环境集成。
- @WebMvcTest(Controller.class)
    - 适用场景：Web 层测试。
    - 加载范围：仅加载 Spring MVC 相关组件及指定的 Controller。
    - 说明：适用于测试控制层逻辑（请求、响应、状态码、JSON 结构等）。
    - 补充说明：Service 层不会自动加载，若需要请使用 @MockBean 注入替代。
- @DataJpaTest
    - 适用场景：JPA Repository 层测试。
    - 加载范围：仅加载 Spring Data JPA 相关 Bean。
    - 说明：自动回滚事务，默认使用嵌入式数据库（如 H2），非常适合验证 JPA 映射、查询方法与事务行为。
- @MybatisTest
    - 适用场景：MyBatis Mapper 层测试。
    - 加载范围：仅加载 MyBatis 相关 Bean（SqlSessionFactory、Mapper 接口等）。
    - 说明：通常配合 JdbcTemplate 进行建表、插入、查询操作验证；可通过 @AutoConfigureTestDatabase(replace = NONE) 指定使用真实数据库。
- @MockBean
    - 适用场景：替换 Spring 容器中的 Bean。
    - 加载范围：将 Mock 对象注册到 Spring 容器中。
    - 说明：与 Mockito 的 @Mock 不同，@MockBean 会将 Mock 注入到 Spring 容器中，使其对容器内其他 Bean 可见；常用于 @WebMvcTest 或 @SpringBootTest 中替代真实 Service。


**MockMvc（不启服务器测 HTTP）**

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
  @Autowired MockMvc mockMvc;
  @MockBean UserService userService;

  @Test
  void get_ok() throws Exception {
    when(userService.find(1L)).thenReturn(new User(1L, "Tom"));
    mockMvc.perform(get("/users/1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("Tom"));
  }
}
```

> `jsonPath` 使用 Hamcrest 匹配器；如需复杂 JSON，推荐断言字段存在、类型正确、值匹配。

---

## 实战案例：三层测试代码精讲

### A. Service 层（Mockito + JUnit5）

**场景**：隔离 DAO，只测业务逻辑。  
**代码（逐行注释示例）**

```java
// 启用 Mockito 扩展，使 @Mock/@InjectMocks 在 JUnit5 中生效
@ExtendWith(MockitoExtension.class)
public class AdminReportingDetailsServiceTest {

    // 创建 DAO 的替身，避免访问真实数据库
    @Mock
    private UserTblMapper userTblMapper;

    // 创建被测 Service，并自动注入上面的 mock
    @InjectMocks
    private AdminReportingDetailsService adminReportingDetailsService;

    // 每个用例前初始化（若使用 @ExtendWith，可选）
    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    // 正常路径：mock 返回非空列表
    @Test
    void getAdminUserList_ok() {
        A0301VueForm form = new A0301VueForm();
        when(userTblMapper.getAdminUserList(any())).thenReturn(List.of(new UserTblPo()));
        var vo = adminReportingDetailsService.getAdminUserList(form);
        assertNotNull(vo);
        // 交互验证：确保被调用一次
        verify(userTblMapper, times(1)).getAdminUserList(any());
    }

    // 异常路径：DAO 抛异常，Service 需透传/转换
    @Test
    void getAdminUserList_daoError() {
        when(userTblMapper.getAdminUserList(any())).thenThrow(new RuntimeException("DB error"));
        assertThrows(RuntimeException.class, () -> adminReportingDetailsService.getAdminUserList(new A0301VueForm()));
    }
}
```

**要点**

- Mock 只替代依赖，不改变被测类真实逻辑。
- 验证交互（`verify`）、参数捕获（`@Captor`）可增强用例质量。

---

### B. Mapper 层（@MybatisTest + JdbcTemplate 造数据）

**场景**：验证 SQL 与映射是否正确。  
**代码**

```java
@MybatisTest // 仅加载 MyBatis 相关 Bean
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 不强制替换数据源
class UserTblMapperTest {

    @Autowired
    private UserTblMapper userTblMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_tbl (user_cd INT PRIMARY KEY, first_name VARCHAR(50));");
        jdbcTemplate.update("INSERT INTO user_tbl(user_cd, first_name) VALUES (?, ?)", 1, "Tom");
    }

    @Test
    void findById_ok() {
        var po = userTblMapper.findById(1);
        assertEquals("Tom", po.getFirstName());
    }

    @Test
    void findById_notFound() {
        var po = userTblMapper.findById(99);
        assertNull(po); // 或者断言 Optional.empty()
    }
}
```

**要点**

- 造表/造数尽量覆盖边界值（空串、超长、特殊字符）。
- 复杂查询建议单独写 SQL 用例，配合数据快照验证。

---

### C. Controller 层

#### 方式 1：Mockito 轻量方式（直接调方法）

```java
@ExtendWith(MockitoExtension.class)
class AdminListControllerTest {

    @Mock
    private AdminReportingDetailsService adminReportingDetailsService;

    @InjectMocks
    private AdminListController adminListController;

    @Test
    void search_nullParam_400() {
        var res = adminListController.search(null);
        assertEquals(400, res.getCode());
        verifyNoInteractions(adminReportingDetailsService);
    }
}
```

**特点**：快、直接、适合只验证**返回对象**。

#### 方式 2：MockMvc（模拟 HTTP）

```java
@WebMvcTest(AdminListController.class)
class AdminListControllerMvcTest {

  @Autowired MockMvc mockMvc;
  @MockBean AdminReportingDetailsService adminReportingDetailsService;

  @Test
  void search_ok() throws Exception {
    var vo = new a0301Vo();
    when(adminReportingDetailsService.getAdminUserList(any())).thenReturn(vo);

    mockMvc.perform(post("/admin/list/search").contentType(APPLICATION_JSON).content("{}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(200))
           .andExpect(jsonPath("$.message").value("操作成功"));
  }
}
```

**特点**：验证路由、状态码、JSON 结构与国际化/异常处理。

---

## 课堂练习与常见陷阱

### 练习

1. 在 Service 单测中，补充“异常分支”和“空入参”用例。  
2. 在 Controller 用 MockMvc 断言 JSON 字段类型与必填校验错误返回。  
3. 生成 Jacoco 报告，列出未覆盖类/方法并补齐用例。  

### 常见陷阱

- **参数匹配器混用**（`any()` 与原值混用）→ 抛 `InvalidUseOfMatchersException`。  
- `@WebMvcTest` 下**拿不到 Service** → 需 `@MockBean`。  
- 盲目使用 `@SpringBootTest` 造成**测试缓慢**。  
- 只追求覆盖率，不写有效断言。  
- 忽略**异常/边界/国际化/时区**等非功能性场景。  

---

## 附录：推荐目录结构与常用命令

### 目录结构

```java
src
├── main/java/com/example/project
│   ├── controller/...
│   ├── service/...
│   ├── mapper/...
│   └── domain/...
└── test/java/com/example/project
    ├── controller/...
    ├── service/...
    └── mapper/...
```

---

**结语**  

通过本课件，你应当能够：

- 建立“单元优先、切片其后、全量兜底”的测试策略；
- 熟练运用 Mockito 与 MockMvc 编写可维护的单测/集成测；
- 以断言为核心、覆盖率为参考，构建稳定的 CI 质量门禁。

> 推荐记忆口诀：  
> **“JUnit 管节奏，Mockito 管依赖；切片快又准，MockMvc 测 HTTP；AssertJ 断言爽。”**
