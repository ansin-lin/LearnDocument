# B05 Spring项目常见注解速查

本附录收录Spring Boot主线第1～18章正文中实际写过或讲到的注解。它用于查阅，不要求背诵。遇到注解时，先看完整import，再确认它写在类、方法、字段还是参数上，最后判断由Java编译器、Spring容器、Spring MVC、Validation、Security、MyBatis或测试框架中的哪一方处理。

课程基线是Java 17、Spring Boot 4.1.x、Spring Framework 7.x和MyBatis Starter/Test Starter 4.1.0。旧项目中的同名注解可能来自不同包，也可能已经弃用或移除，不能只按短名称复制。

## 一、怎样阅读下面的表

- “无属性”表示注解本身没有可填写的属性，不表示它没有运行效果。
- `{}` 表示空数组；`""` 表示空字符串。
- `value` 经常是简写属性，但不同注解中的含义不同，必须分别确认。
- `value` 与 `path`、`value` 与 `name` 等别名通常二选一，不重复填写。
- 表中列出课程和项目最常用属性，不穷举框架全部API。

一行示例只展示注解写法；完整类、import、执行过程和验证方式应回到对应主线章节查看。

## 二、第1～18章注解分布

| 章节 | 本章写过或讲到的注解 |
| --- | --- |
| 第1～2章 | 没有引入框架注解，先理解Spring Boot定位与分层职责 |
| 第3章 | `@SpringBootApplication`、`@SpringBootConfiguration`、`@EnableAutoConfiguration`、`@ComponentScan`、`@Configuration`、`@Component`、`@Service`、`@Controller`、`@RestController`、`@ResponseBody`、`@RequestMapping`、`@GetMapping`、`@SpringBootTest`、`@Test` |
| 第4章 | `@Service`、`@RestController`、`@GetMapping`、`@Autowired`、`@Qualifier`、`@Primary` |
| 第5章 | `@Entity` 只作为“不使用JPA”的对照说明 |
| 第6章 | `@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PathVariable`、`@RequestParam`、`@RequestBody`、`@RequestHeader`、`@CookieValue`、`@RequestPart` |
| 第7章 | `@ControllerAdvice`、`@RestControllerAdvice`、`@ExceptionHandler`、`@Order`、`@Priority` 以及前面已学的MVC注解 |
| 第8章 | `@Valid`、`@Validated`、`@NotNull`、`@NotBlank`、`@NotEmpty`、`@Size`、`@Email`、`@Pattern`、`@Min`、`@Max`、`@Positive`、`@PositiveOrZero` |
| 第9～10章 | `@Mapper`、`@Param`、`@Transactional`、`@Override`，并继续使用MVC、Validation和Service注解 |
| 第11章 | `@Component`、`@Order`、`@Override`、`@ExceptionHandler` |
| 第12章 | JUnit、Mockito、Spring Test、MyBatis Test和Spring Boot Test相关注解 |
| 第13～14章 | `@Transactional`、`@Sql`、`@ActiveProfiles`、`@ModelAttribute` 以及数据库集成测试注解 |
| 第15～16章 | `@Bean`、`@Configuration`、`@EnableMethodSecurity`、`@PreAuthorize`、`@WithMockUser`、`@CrossOrigin` 以及Security测试注解 |
| 第17章 | `@ConfigurationProperties`、`@ConfigurationPropertiesScan`、`@Validated`、`@Value` |
| 第18章 | 没有新增Java注解，重点转为部署、运行检查和运维边界 |

`@employeeAccess` 是 `@PreAuthorize("@employeeAccess...")` 表达式中的Bean引用，不是Java注解。文档占位文字中的 `@example` 也不是代码注解。

## 三、Java语言、Boot启动与配置

| 注解 | 处理者与作用 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@Override` | Java编译器；确认方法确实覆盖父类或接口方法 | 无属性 | 无 | `@Override public String toString() { return name; }` |
| `@SpringBootApplication` | Spring Boot；组合主要配置、自动配置和组件扫描 | `scanBasePackages`、`scanBasePackageClasses`、`exclude`、`excludeName`、`proxyBeanMethods` | 包名字符串数组、标记类数组、自动配置类数组、完整类名数组；`proxyBeanMethods`默认`true` | `@SpringBootApplication(scanBasePackages = "com.example.employee", exclude = SomeAutoConfiguration.class)` |
| `@SpringBootConfiguration` | Spring Boot；标记应用主要配置类，是`@Configuration`的特化 | `proxyBeanMethods` | `true`或`false`，默认`true` | `@SpringBootConfiguration(proxyBeanMethods = true)` |
| `@EnableAutoConfiguration` | Spring Boot；根据依赖、配置和已有Bean启用自动配置 | `exclude`、`excludeName` | 自动配置类数组或完整类名数组，默认`{}` | `@EnableAutoConfiguration(exclude = SomeAutoConfiguration.class)` |
| `@ComponentScan` | Spring；扫描组件类 | `value`/`basePackages`、`basePackageClasses`、`includeFilters`、`excludeFilters`、`useDefaultFilters`、`lazyInit` | 包名数组、标记类数组、Filter数组；两个boolean默认分别为`true`、`false` | `@ComponentScan(basePackageClasses = EmployeeApplication.class, useDefaultFilters = true)` |
| `@Configuration` | Spring；声明配置类 | `value`、`proxyBeanMethods` | Bean名称字符串默认`""`；代理配置方法默认`true` | `@Configuration(proxyBeanMethods = false)` |
| `@ConfigurationProperties` | Spring Boot；把同一前缀下的外部配置绑定到对象 | `prefix`/`value`、`ignoreInvalidFields`、`ignoreUnknownFields` | 规范前缀字符串；两个boolean默认分别为`false`、`true` | `@ConfigurationProperties(prefix = "app.export", ignoreUnknownFields = true)` |
| `@ConfigurationPropertiesScan` | Spring Boot；扫描配置属性类 | `value`/`basePackages`、`basePackageClasses` | 包名数组或标记类数组，默认从声明位置所在包扫描 | `@ConfigurationPropertiesScan(basePackageClasses = EmployeeApplication.class)` |
| `@Value` | Spring；注入属性占位符或SpEL结果 | `value` | 必填字符串，如`${key}`、`${key:default}`或`#{expression}` | `@Value("${app.export.limit:100}") private int limit;` |

`@SpringBootApplication` 通常只写在一个启动类上。主线不要求把它拆成三个注解分别书写；第3章拆开解释是为了理解复合注解。

## 四、Bean、组件与依赖注入

| 注解 | 处理者与作用 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@Component` | Spring容器；注册通用组件Bean | `value` | Bean名称字符串，默认`""` | `@Component("requestLoggingFilter")` |
| `@Service` | Spring容器；注册业务层Bean并表达职责 | `value` | Bean名称字符串，默认`""` | `@Service("employeeService")` |
| `@Controller` | Spring MVC；注册通常返回视图的控制器 | `value` | Bean名称字符串，默认`""`；不是URL | `@Controller("employeePageController")` |
| `@RestController` | Spring MVC；组合`@Controller`和`@ResponseBody` | `value` | Bean名称字符串，默认`""`；不是URL | `@RestController("employeeController")` |
| `@ResponseBody` | Spring MVC；把返回值写入响应体 | 无属性 | 无 | `@ResponseBody public EmployeeResponse find() { ... }` |
| `@Autowired` | Spring容器；按类型注入依赖 | `required` | `true`或`false`，默认`true` | `@Autowired(required = true) public EmployeeServiceImpl(EmployeeMapper mapper) { ... }` |
| `@Qualifier` | Spring容器；在多个同类型候选中按限定符选择 | `value` | 必填限定符字符串 | `@Qualifier("databaseEmployeeRepository")` |
| `@Primary` | Spring容器；把某个候选设为默认首选 | 无属性 | 无 | `@Primary @Service public class DefaultEmployeeService { ... }` |
| `@Bean` | Spring容器；把配置方法返回对象注册为Bean | `value`/`name`、`initMethod`、`destroyMethod`、`autowireCandidate` | 名称数组默认`{}`；初始化方法默认`""`；销毁方法默认推断；候选默认`true` | `@Bean(name = "passwordEncoder", destroyMethod = "")` |

单构造器可以省略 `@Autowired`。主线优先使用构造器注入，因为依赖明确、对象容易测试，也不会把必需依赖隐藏在字段中。

## 五、Spring MVC映射与请求输入

这些注解在Controller方法执行前参与路由选择和参数绑定。路径不匹配、必填参数缺失、类型转换失败或请求体无法读取时，业务方法可能尚未进入就已经产生4xx响应。

| 注解 | 作用位置 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@RequestMapping` | 类或方法；声明通用映射条件 | `name`、`value`/`path`、`method`、`params`、`headers`、`consumes`、`produces` | 字符串/字符串数组；`method`为`RequestMethod[]`；默认数组为空 | `@RequestMapping(path = "/employees", method = RequestMethod.GET, produces = "application/json")` |
| `@GetMapping` | 方法；GET映射 | `name`、`value`/`path`、`params`、`headers`、`consumes`、`produces` | 字符串数组，默认`{}` | `@GetMapping(path = "/{id}", produces = "application/json")` |
| `@PostMapping` | 方法；POST映射 | 与`@GetMapping`相同 | 字符串数组，默认`{}` | `@PostMapping(path = "/employees", consumes = "application/json")` |
| `@PutMapping` | 方法；PUT映射 | 与`@GetMapping`相同 | 字符串数组，默认`{}` | `@PutMapping(path = "/employees/{id}", consumes = "application/json")` |
| `@DeleteMapping` | 方法；DELETE映射 | 与`@GetMapping`相同 | 字符串数组，默认`{}` | `@DeleteMapping(path = "/employees/{id}")` |
| `@PathVariable` | 参数；读取URI模板变量 | `value`/`name`、`required` | 变量名字符串；`required`默认`true` | `find(@PathVariable(name = "id", required = true) Long id)` |
| `@RequestParam` | 参数；读取Query String或表单参数 | `value`/`name`、`required`、`defaultValue` | 参数名；`required`默认`true`；设置默认值后相当于非必填 | `search(@RequestParam(name = "page", defaultValue = "1") int page)` |
| `@RequestBody` | 参数；从请求体反序列化对象 | `required` | `true`或`false`，默认`true` | `create(@Valid @RequestBody(required = true) EmployeeCreateRequest request)` |
| `@RequestHeader` | 参数；读取请求Header | `value`/`name`、`required`、`defaultValue` | Header名；`required`默认`true` | `trace(@RequestHeader(name = "X-Request-Id", required = false) String id)` |
| `@CookieValue` | 参数；读取Cookie | `value`/`name`、`required`、`defaultValue` | Cookie名；`required`默认`true` | `read(@CookieValue(name = "locale", defaultValue = "ja-JP") String locale)` |
| `@RequestPart` | 参数；读取multipart指定part | `value`/`name`、`required` | part名；`required`默认`true` | `upload(@RequestPart(name = "file", required = true) MultipartFile file)` |
| `@ModelAttribute` | 参数或方法；绑定请求参数或准备模型属性 | `value`/`name`、`binding` | 属性名默认`""`；`binding`默认`true` | `search(@ModelAttribute(name = "condition", binding = true) EmployeeSearchCondition condition)` |
| `@CrossOrigin` | Controller类或方法；声明局部CORS | `origins`、`originPatterns`、`methods`、`allowedHeaders`、`exposedHeaders`、`allowCredentials`、`maxAge` | 字符串数组、`RequestMethod[]`；凭据用`"true"`、`"false"`或空值；`maxAge`默认`-1` | `@CrossOrigin(origins = "http://localhost:5173", methods = RequestMethod.GET, allowCredentials = "true")` |

`params = "status=ACTIVE"`、`headers = "X-Mode=test"` 是附加匹配条件，不是读取值。读取值仍使用方法参数和相应绑定注解。

## 六、Validation字段校验

`@Valid` 来自Jakarta Validation，`@Validated` 来自Spring。约束注解共有 `message`、`groups`、`payload`；`groups` 是校验分组，不是字段允许值列表。

| 注解 | 校验规则 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@Valid` | 触发对象及嵌套对象级联校验 | 无属性 | 无 | `create(@Valid @RequestBody EmployeeCreateRequest request)` |
| `@Validated` | Spring校验入口，可选择分组 | `value` | `Class<?>[]`，默认`{}` | `@Validated({CreateGroup.class})` |
| `@NotNull` | 值不能为`null` | `message`、`groups`、`payload` | 消息字符串、分组类数组、Payload类数组；数组默认`{}` | `@NotNull(message = "departmentId is required")` |
| `@NotBlank` | 字符串去除空白后非空 | 共有属性 | 同上 | `@NotBlank(message = "name is required")` |
| `@NotEmpty` | 字符串、集合、Map或数组非空 | 共有属性 | 同上 | `@NotEmpty(message = "roles must not be empty")` |
| `@Size` | 限制长度或件数 | `min`、`max`及共有属性 | 非负`int`；默认`0`与`Integer.MAX_VALUE` | `@Size(min = 1, max = 100, message = "name length is invalid")` |
| `@Email` | 检查邮箱格式 | `regexp`、`flags`及共有属性 | 正则字符串；`flags`为`Pattern.Flag[]`，默认`{}` | `@Email(regexp = ".+@.+\\..+", message = "email is invalid")` |
| `@Pattern` | 字符串匹配正则 | `regexp`、`flags`及共有属性 | `regexp`必填；`flags`默认`{}` | `@Pattern(regexp = "ACTIVE|INACTIVE", message = "status is invalid")` |
| `@Min` | 数值不小于下限 | `value`及共有属性 | 必填`long` | `@Min(value = 1, message = "page must be at least 1")` |
| `@Max` | 数值不大于上限 | `value`及共有属性 | 必填`long` | `@Max(value = 100, message = "pageSize must be at most 100")` |
| `@Positive` | 数值大于0 | 共有属性 | 无额外数值属性 | `@Positive(message = "id must be positive")` |
| `@PositiveOrZero` | 数值大于或等于0 | 共有属性 | 无额外数值属性 | `@PositiveOrZero(message = "offset must not be negative")` |

格式校验通过不代表业务规则成立。邮箱重复、员工是否存在、状态是否允许变化以及当前用户能否操作，仍由Service、数据库约束和Security负责。

## 七、异常处理与执行顺序

| 注解 | 处理者与作用 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@ControllerAdvice` | Spring MVC；提供共通异常、绑定或模型处理 | `name`、`value`/`basePackages`、`basePackageClasses`、`assignableTypes`、`annotations` | 名称/包名字符串数组或类数组，默认空 | `@ControllerAdvice(basePackageClasses = EmployeeController.class)` |
| `@RestControllerAdvice` | 组合`@ControllerAdvice`和`@ResponseBody` | 与上面相同 | 与上面相同 | `@RestControllerAdvice(basePackages = "com.example.employee.controller")` |
| `@ExceptionHandler` | Spring MVC；指定异常处理方法 | `value`/`exception`、`produces` | 异常类数组默认`{}`，为空时可由参数推断；媒体类型数组默认`{}` | `@ExceptionHandler(exception = EmployeeNotFoundException.class, produces = "application/json")` |
| `@Order` | Spring；设置组件、Filter或Advice顺序 | `value` | 必填`int`或`Ordered`常量；数值越小越优先 | `@Order(Ordered.HIGHEST_PRECEDENCE)` |
| `@Priority` | Jakarta；声明优先级 | `value` | 必填`int` | `@Priority(10)` |

多个Advice同时存在时，要一起检查Advice顺序、根异常、cause和实际匹配方法，不能只比较异常继承关系。

## 八、MyBatis、事务与持久化对照

| 注解 | 处理者与作用 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@Mapper` | MyBatis；注册Mapper代理 | 无属性 | 无 | `@Mapper public interface EmployeeMapper { ... }` |
| `@Param` | MyBatis；给方法参数指定SQL引用名 | `value` | 必填字符串 | `findPage(@Param("limit") int limit, @Param("offset") int offset)` |
| `@Transactional` | Spring；声明事务边界 | `value`/`transactionManager`、`propagation`、`isolation`、`timeout`、`readOnly`、`rollbackFor`、`noRollbackFor` | 管理器名；传播默认`REQUIRED`；隔离默认`DEFAULT`；超时`-1`；只读`false`；异常数组默认`{}` | `@Transactional(propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class)` |
| `@Entity` | Jakarta Persistence；声明JPA实体 | `name` | 实体名字符串，默认类名 | `@Entity(name = "Employee")` |

主线使用MyBatis，不使用JPA。第5章出现 `@Entity` 是为了说明“项目对象名叫Entity”不等于“JPA实体”，不要给MyBatis对象机械添加它。

## 九、Spring Security

| 注解 | 处理者与作用 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@EnableMethodSecurity` | Spring Security；启用方法授权 | `prePostEnabled`、`securedEnabled`、`jsr250Enabled`、`proxyTargetClass`、`mode` | boolean默认依次为`true`、`false`、`false`、`false`；`mode`通常为`AdviceMode.PROXY` | `@EnableMethodSecurity(prePostEnabled = true, securedEnabled = false)` |
| `@PreAuthorize` | Spring Security；方法调用前计算授权表达式 | `value` | 必填SpEL字符串 | `@PreAuthorize("hasRole('ADMIN') or @employeeAccess.canRead(#id, authentication)")` |
| `@WithMockUser` | Spring Security Test；建立模拟认证身份 | `value`/`username`、`roles`、`authorities`、`password`、`setupBefore` | 用户名默认`"user"`；角色默认`{"USER"}`；权限默认`{}`；密码默认`"password"` | `@WithMockUser(username = "admin", roles = "ADMIN")` |

`roles = "ADMIN"` 会形成 `ROLE_ADMIN`；`authorities = "employee:read"` 使用原始权限字符串。测试时按正式规则选用，不要把CSRF导致的403误判为角色校验成功。

## 十、JUnit与Mockito单元测试

| 注解 | 处理者与作用 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@Test` | JUnit Jupiter；标记测试方法 | 无属性 | 无 | `@Test void returnsEmployeeWhenFound() { ... }` |
| `@BeforeEach` | JUnit Jupiter；每个测试前准备 | 无属性 | 无 | `@BeforeEach void setUp() { ... }` |
| `@AfterEach` | JUnit Jupiter；每个测试后清理 | 无属性 | 无 | `@AfterEach void tearDown() { ... }` |
| `@BeforeAll` | JUnit Jupiter；整个测试类的测试方法开始前执行一次 | 无属性 | 无；默认要求方法为`static`，配合`PER_CLASS`可使用实例方法 | `@BeforeAll void verifyTestDatabase() { ... }` |
| `@TestInstance` | JUnit Jupiter；选择测试类实例生命周期 | `value` | 必填`PER_METHOD`或`PER_CLASS` | `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` |
| `@TestMethodOrder` | JUnit Jupiter；指定方法排序策略 | `value` | 必填`Class<? extends MethodOrderer>` | `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` |
| `@ExtendWith` | JUnit Jupiter；注册Extension | `value` | 必填Extension类数组 | `@ExtendWith(MockitoExtension.class)` |
| `@Mock` | Mockito；创建普通mock | `answer`、`name`、`extraInterfaces`、`serializable`、`stubOnly` | Answer默认`RETURNS_DEFAULTS`；名称`""`；接口数组`{}`；boolean为`false` | `@Mock(answer = Answers.RETURNS_DEFAULTS, name = "employeeMapper")` |
| `@InjectMocks` | Mockito；创建被测对象并尝试注入mock | 无常用属性 | 无 | `@InjectMocks private EmployeeServiceImpl employeeService;` |

`@Mock` 不会把对象放入Spring容器。`@InjectMocks` 也不是Spring依赖注入的替代品；主线仍优先显式构造被测对象。

## 十一、Spring与Spring Boot测试

| 注解 | 处理者与作用 | 常用属性 | 可接受的值与默认值 | 一行示例 |
| --- | --- | --- | --- | --- |
| `@MockitoBean` | Spring Test；用Mockito mock覆盖测试上下文Bean | `value`/`name`、`types`、`extraInterfaces`、`answers`、`serializable`、`reset`、`enforceOverride`、`contextName` | Bean名/类型/接口；Answer默认`RETURNS_DEFAULTS`；`serializable=false`；`reset=AFTER`；`enforceOverride=false` | `@MockitoBean(name = "employeeService", answers = Answers.RETURNS_DEFAULTS)` |
| `@MockBean` | 旧Spring Boot Test；历史上用于覆盖Bean | 旧版常见`name`、`classes`/`value`、`answer`、`reset` | Boot 3.4已弃用并计划在4.0移除；Boot 4.1主线不可使用 | `@MockBean EmployeeService service; // 只用于识别旧代码` |
| `@SpringBootTest` | Spring Boot Test；加载完整应用上下文 | `classes`、`properties`/`value`、`args`、`webEnvironment`、`useMainMethod` | 类/字符串数组；Web环境默认`MOCK`；主方法默认不调用 | `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "app.test=true")` |
| `@WebMvcTest` | Spring Boot Test；加载MVC切片 | `value`/`controllers`、`properties`、`useDefaultFilters`、`includeFilters`、`excludeFilters`、`excludeAutoConfiguration` | Controller数组`{}`；属性数组`{}`；默认过滤`true` | `@WebMvcTest(controllers = EmployeeController.class)` |
| `@AutoConfigureMockMvc` | Spring Boot Test；配置MockMvc | `addFilters`、`print`、`printOnlyOnFailure`、`htmlUnit` | `addFilters=true`；`print=DEFAULT`；失败时打印`true`；`htmlUnit`使用Boot 4的嵌套配置，默认采用框架提供的设置 | `@AutoConfigureMockMvc(addFilters = true, printOnlyOnFailure = true)` |
| `@MybatisTest` | MyBatis Test Starter；加载Mapper测试切片 | `properties`、`useDefaultFilters`、`includeFilters`、`excludeFilters`、`excludeAutoConfiguration` | `key=value`数组；默认过滤`true`；其他数组默认`{}` | `@MybatisTest(properties = "mybatis.configuration.map-underscore-to-camel-case=true")` |
| `@AutoConfigureTestDatabase` | Spring Boot Test；控制测试数据源替换 | `replace`、`connection` | `replace`可为`NON_TEST`、`ANY`、`AUTO_CONFIGURED`、`NONE`，Boot 4默认`NON_TEST`；连接默认自动检测 | `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` |
| `@ActiveProfiles` | Spring Test；指定测试Profile | `value`/`profiles`、`inheritProfiles`、`resolver` | Profile数组；继承默认`true`；resolver默认框架解析器 | `@ActiveProfiles(profiles = "test", inheritProfiles = true)` |
| `@Sql` | Spring Test；在测试前后执行SQL | `value`/`scripts`、`statements`、`executionPhase`、`config` | 脚本/语句数组；阶段默认`BEFORE_TEST_METHOD` | `@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)` |

`@WebMvcTest` 通常与 `@MockitoBean` 配合；`@SpringBootTest` 加 `@AutoConfigureMockMvc` 才是完整应用上下文中的MockMvc测试；`@MybatisTest` 只证明Mapper和SQL映射。

## 十二、扩展与既存项目识读注解

以下注解不是01～18章稳定工程的必需代码，但在本课程附录或既存项目中经常遇到。

| 注解 | 常用属性与值 | 识读重点 |
| --- | --- | --- |
| `@Repository` | `value`为Bean名，默认`""` | 数据访问职责；Spring Data接口可能不显式书写 |
| `@MapperScan` | `value`/`basePackages`、`basePackageClasses`、`annotationClass`、`markerInterface` | 批量扫描Mapper，可替代逐个`@Mapper` |
| `@Profile` | `value`为表达式数组，如`{"dev", "!prod"}` | 决定Bean是否创建，不等于选择配置文件 |
| `@PatchMapping` | 与其他快捷映射相同 | 局部更新前先定义缺失字段、null与未修改字段语义 |
| `@ResponseStatus` | `value`/`code`为状态值，`reason`为字符串 | 不要与业务错误码混为一谈 |
| `@JsonProperty` | `value`为字段名；`access`为`AUTO`、`READ_ONLY`、`WRITE_ONLY`或`READ_WRITE` | 参见A05，确认读写方向 |
| `@JsonIgnore` | `value`为boolean，默认`true` | 不能替代DTO和权限边界 |
| `@JsonInclude` | `value`/`content`常用`ALWAYS`、`NON_NULL`、`NON_EMPTY` | 缺失字段与显式null可能语义不同 |
| `@JsonFormat` | `pattern`、`shape`、`locale`、`timezone` | 不会给`LocalDateTime`增加真实时区语义 |
| `@PostConstruct` / `@PreDestroy` | 无属性 | 初始化后或正常销毁前回调，不适合长任务 |
| `@EnableScheduling` | 无属性 | 启用调度，不定义任务 |
| `@Scheduled` | `cron`、`zone`、`fixedDelay`、`fixedRate`、`initialDelay` | cron、延迟、频率三种方式选一；集群防重另行设计 |
| `@EnableAsync` | `mode`、`proxyTargetClass`、`order` | 默认代理模式，仍需确认Executor |
| `@Async` | `value`为Executor Bean名，默认`""` | 事务、异常、MDC和Security上下文不会自动跨线程 |
| Lombok的`@Getter`、`@Setter`、`@Data`、`@Builder`等 | 生成属性详见B04 | 编译期生成代码，不是Spring运行时注入 |
| JPA的`@Table`、`@Id`、`@GeneratedValue`、`@Column`等 | 表名、列名、生成策略、长度和可空性等，详见B04 | 用于识读JPA项目，不加入MyBatis主线对象 |

## 十三、遇到陌生注解时的调查顺序

1. 查看import，得到完整类名和所属依赖。
2. 确认项目锁定版本，不用其他大版本行为直接推断。
3. 查看目标位置：类、方法、字段、构造器还是参数。
4. 确认处理者和时机：编译期、容器启动、请求绑定、代理调用还是测试启动。
5. 检查属性、默认值、启用条件、扫描范围和代理边界。
6. 找到对应测试或建立最小实验，观察真实结果。

## 十四、官方查阅入口

- [Spring Framework注解式Controller](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html)
- [Spring测试中的Bean覆盖与MockitoBean](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/bean-overriding.html)
- [Spring Boot自动配置](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)
- [Spring Boot应用测试与WebMvcTest](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Jakarta Validation规范](https://jakarta.ee/specifications/bean-validation/)
- [MyBatis Spring Boot Starter](https://mybatis.org/spring-boot-starter/)
