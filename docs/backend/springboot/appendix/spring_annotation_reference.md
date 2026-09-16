# Appendix E：Spring项目常见注解速查

这不是背诵表。阅读既存项目时，先确认注解所属框架、写在什么位置、由谁在什么时候处理，再沿本课程对应章节回看完整场景。同名短名称可能来自不同包，判断时以import为准。

## 一、Bean与依赖注入

| 注解（所属框架） | 写在哪里、解决什么问题 | 本课程/既存项目怎样读 |
| --- | --- | --- |
| `@Component`（Spring） | 类；注册通用Bean | 第4章；看到后检查组件扫描范围 |
| `@Service`（Spring） | 业务类；表达Service职责并注册Bean | 第4章；事务和方法权限常通过该Bean代理生效 |
| `@Repository`（Spring） | 数据访问类；表达Repository职责 | JPA附录；Spring Data接口不一定显式书写 |
| `@Controller`（Spring MVC） | Web控制器类；常与视图返回配合 | 既存MVC项目可能返回页面名 |
| `@RestController`（Spring MVC） | REST控制器类；相当于Controller加响应体语义 | 第3章；方法返回值通常写入HTTP响应体 |
| `@Autowired`（Spring） | 构造器、方法或字段；请求依赖注入 | 第4章主要用单构造器省略写法；优先识别注入位置 |
| `@Qualifier`（Spring） | 注入点或Bean；多个同类型Bean时按名称/限定符选择 | 第4章；与字段名巧合匹配不同，应看明确限定规则 |
| `@Primary`（Spring） | Bean定义；多个候选中提供默认首选 | 第4章；显式Qualifier通常比默认首选更具体 |
| `@Configuration`（Spring） | 配置类；声明Bean定义来源 | 第4、16章；方法间调用和代理行为按项目配置理解 |
| `@Bean`（Spring） | 配置类方法；把返回对象注册为Bean | 第4章；方法名通常成为默认Bean名 |

## 二、Boot与配置

| 注解（所属框架） | 写在哪里、解决什么问题 | 本课程/既存项目怎样读 |
| --- | --- | --- |
| `@SpringBootApplication`（Spring Boot） | 启动类；组合配置、自动配置、组件扫描 | 第3章；先确认启动类包位置 |
| `@ConfigurationProperties`（Spring Boot） | 配置类；把一组外部配置绑定为类型安全对象 | 第17章及外部API附录；核对prefix、注册方式和校验 |
| `@Value`（Spring） | 字段、参数或方法；注入单个配置表达式 | 第17章识读；大量相关配置更适合配置属性类 |
| `@Profile`（Spring） | Bean或配置类；限制在哪些Profile创建 | 第17章；与配置文件后缀不是同一机制 |

## 三、Spring MVC请求映射与输入

这些注解由Spring MVC在选择Controller方法和绑定参数时处理。必填参数缺失、类型转换或消息读取失败，通常在进入业务方法前形成4xx错误。

| 注解（Spring MVC） | 输入位置/作用 | 本课程/既存项目怎样读 |
| --- | --- | --- |
| `@RequestMapping` | 类或方法；路径、方法、媒体类型等通用映射 | 第3、6章；类与方法条件会组合 |
| `@GetMapping` | 方法；GET映射 | 查询，不应承担普通写入 |
| `@PostMapping` | 方法；POST映射 | 新增、登录或动作型请求 |
| `@PutMapping` | 方法；PUT映射 | 第10章更新 |
| `@PatchMapping` | 方法；PATCH映射 | 既存项目中的局部更新，先确认字段语义 |
| `@DeleteMapping` | 方法；DELETE映射 | 第10章删除 |
| `@PathVariable` | URL路径变量 | 第6章；缺少路径通常是不匹配路由 |
| `@RequestParam` | Query String或表单参数 | 第6、14、19章；常用`value`、`required`、`defaultValue` |
| `@RequestBody` | HTTP body，经消息转换器读取JSON等 | 第6章；通常只能有一个主要请求体对象 |
| `@RequestHeader` | HTTP Header | 第6章；常用`value`、`required`、`defaultValue`，Header名语义不区分大小写 |
| `@CookieValue` | Cookie | 第6章；JSESSIONID通常由Session/Security处理 |
| `@RequestPart` | multipart中的某个part | 第6章；可绑定JSON metadata或文件part，常用`value`、`required` |
| `@ModelAttribute` | 请求参数绑定到对象，也可处理模型属性 | 第6章；REST课程主要使用参数绑定部分 |
| `@CrossOrigin` | Controller或方法；局部声明CORS规则 | 第16章；不等于认证、授权或CSRF保护 |

`MultipartFile` 是 `org.springframework.web.multipart` 接口，不是注解；它表示上传文件part。文件名、Content-Type和大小仍需安全检查。

## 四、Validation

`@Valid` 来自Jakarta Validation；`@Validated` 来自Spring。约束注解写在字段、参数等目标上，具体支持位置应看注解定义和项目版本。

| 注解 | 主要规则 | 本课程/识读重点 |
| --- | --- | --- |
| `@Valid` | 触发对象级联校验 | 第8章；没有它时嵌套对象约束可能不执行 |
| `@Validated` | Spring校验入口并支持分组 | 第8章；类/方法级校验需结合代理和配置 |
| `@NotNull` | 值不能为null | 不限制空字符串或空集合 |
| `@NotBlank` | 字符串不能为null且去除空白后非空 | 姓名等文本 |
| `@NotEmpty` | 字符串、集合、数组等不能为null且非空 | 不会自动去除字符串空白 |
| `@Size` | 字符串、集合等长度/件数范围 | 第8、19章；不是数值大小 |
| `@Email` | 邮箱格式约束 | 第8章；不证明邮箱真实存在 |
| `@Pattern` | 字符串匹配正则 | 第8章；错误消息应让用户可理解 |
| `@Min` / `@Max` | 数值下限/上限 | 第14章页码等数值；注意类型和边界 |

字段格式通过不代表业务规则通过。邮箱重复、员工状态变更许可等仍由Service和数据库约束负责。

## 五、异常与响应

| 注解（所属框架） | 写在哪里、解决什么问题 | 本课程/既存项目怎样读 |
| --- | --- | --- |
| `@ControllerAdvice`（Spring MVC） | 类；对Controller提供共通绑定、模型或异常处理 | 第7章；REST项目常用其特化版本 |
| `@RestControllerAdvice`（Spring MVC） | 类；ControllerAdvice加响应体语义 | 第7章全局异常JSON |
| `@ExceptionHandler`（Spring MVC） | 方法；处理指定异常类型 | 第7章；看参数类型、返回状态和更具体匹配 |
| `@ResponseStatus`（Spring MVC） | 异常类或方法；声明HTTP状态 | 既存项目识读；不要与业务错误码混为一谈 |

安全过滤器在Controller之前拒绝的401/403通常不会进入ControllerAdvice，第16章使用Security专用处理器。

## 六、MyBatis、事务与安全

| 注解（所属框架） | 写在哪里、解决什么问题 | 本课程/既存项目怎样读 |
| --- | --- | --- |
| `@Mapper`（MyBatis） | Mapper接口；注册映射器 | 第9章；XML namespace需匹配接口完整类名 |
| `@MapperScan`（MyBatis-Spring） | 配置类；批量扫描Mapper包 | 既存项目可能用它替代每个接口的Mapper注解 |
| `@Param`（MyBatis） | Mapper参数；给SQL中的参数明确命名 | 第9、19章；XML名称必须一致 |
| `@Transactional`（Spring） | 类或方法；声明事务边界 | 第13章；依赖Spring代理，默认回滚规则需确认 |
| `@EnableMethodSecurity`（Spring Security） | 配置类；启用方法级授权 | 第16章 |
| `@PreAuthorize`（Spring Security） | Bean方法或类；调用前执行授权表达式 | 第16章；不代替URL规则和数据查询条件 |

## 七、Jackson JSON映射

| 注解（Jackson） | 作用位置和问题 | 本课程/既存项目怎样读 |
| --- | --- | --- |
| `@JsonProperty` | 字段、方法、参数；指定JSON属性名等 | 第5章；确认读写方向和命名 |
| `@JsonIgnore` | 字段或方法；忽略JSON读写 | 第5章；不能替代DTO边界和权限控制 |
| `@JsonInclude` | 类或属性；控制null、空值等是否输出 | 第5章；缺失字段与显式null可能有不同语义 |
| `@JsonFormat` | 属性；指定日期等JSON格式 | 第5、9章；不会给LocalDateTime增加时区 |

## 八、测试

| 注解（所属框架） | 写在哪里、解决什么问题 | 本课程/既存项目怎样读 |
| --- | --- | --- |
| `@Test`（JUnit Jupiter） | 测试方法；标记可由JUnit执行的测试 | 第12章；继续检查断言、准备数据和测试隔离 |
| `@SpringBootTest`（Spring Boot Test） | 测试类；加载完整Boot应用上下文 | 第12章；适合集成范围，不应替代所有快速单元测试 |
| `@WebMvcTest`（Spring Boot Test） | 测试类；聚焦Spring MVC切片 | 第12章；确认指定Controller及Mock依赖 |
| `@AutoConfigureMockMvc`（Spring Boot Test） | 测试类；在Boot测试中配置MockMvc | 第12章；仍通过过滤器链时要考虑Security和CSRF |
| `@ActiveProfiles`（Spring Test） | 测试类；选择测试使用的Profile | 第12、17章；检查对应配置文件和环境覆盖 |
| `@Sql`（Spring Test） | 测试类或方法；在指定阶段执行SQL脚本 | 第12、13章；确认执行时机、事务和清理策略 |
| `@WithMockUser`（Spring Security Test） | 安全测试类或方法；建立模拟认证身份 | 第15、16章；不是数据库真实登录流程 |

测试注解只能说明测试上下文怎样建立，不能代替业务断言。阅读既存测试时还要确认正常、异常、边界、权限、数据库状态和回归范围。

## 九、生命周期与后台执行

| 注解（所属框架） | 处理时机与用途 | 识读边界 |
| --- | --- | --- |
| `@PostConstruct`（Jakarta Annotation） | Bean依赖注入完成后的初始化回调 | 不适合长时间任务；异常可能使启动失败 |
| `@PreDestroy`（Jakarta Annotation） | Bean销毁前的清理回调 | 正常关闭时释放资源；强制终止未必执行 |
| `@Scheduled`（Spring） | 由调度器按fixed delay/rate或cron触发方法 | 需启用调度；多实例、时区、重复执行和失败重试要另行设计 |
| `@Async`（Spring） | 通过代理把方法提交到异步执行器 | 需启用异步；线程池、异常、事务和上下文不会自动与调用线程相同 |

最小识读示例：

```java
@Component
public class EmployeeCacheLifecycle {

    @PostConstruct
    void load() {
        // Bean初始化后加载少量必要数据
    }

    @PreDestroy
    void close() {
        // 正常关闭前释放本类持有的资源
    }
}
```

```java
@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Tokyo")
public void synchronizeEmployees() {
    // 需要自行设计幂等、并发控制、失败通知和执行证据
}
```

看到 `@Scheduled` 或 `@Async` 后，继续搜索 `@EnableScheduling`、`@EnableAsync`、Executor配置和测试。不要因为加了注解就认为批处理已经具备集群防重、重试或监控。

## 十、Lombok与JPA

| 注解 | 所属 | 识读方向 |
| --- | --- | --- |
| `@Getter` / `@Setter` / `@Data` / `@Builder` / `@RequiredArgsConstructor` | Lombok | 编译期生成代码；详见Appendix D |
| `@Entity` / `@Id` / `@GeneratedValue` / `@Column` | Jakarta Persistence | Entity和表列/主键映射；详见Appendix D |

## 十一、遇到陌生注解时的调查顺序

1. 看import得到完整类名和所属库。
2. 查项目锁定的版本，不拿其他大版本行为套用。
3. 看注解目标：类、方法、字段还是参数。
4. 确认是谁处理：编译器、Spring容器、MVC、Security、MyBatis或Jackson。
5. 找启用条件、配置、代理边界和对应测试。
6. 用最小用例验证，不在生产主线直接试错。
