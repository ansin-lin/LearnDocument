# Redis：为员工详情增加可失效的缓存

## 1. 先判断要不要缓存

`GET /employees/{id}` 在每次请求都读 MySQL。若它被频繁读取而员工资料很少修改，可把已查询的详情短期放进 Redis。Redis 是内存数据存储；这里仅把它当作**可丢弃的副本**。第一次读取 MySQL，后续读取缓存叫 cache-aside；更新成功后删除旧缓存，下一次请求重新从 MySQL 装载。不要用缓存值替代事务、权限判断或履历表。

进入本章前，从[扩展入口](index.md)的独立练习副本启动工程，确认现有详情查询与修改接口、MySQL 数据和第12章测试均正常。实验只新增 Redis 依赖、配置与缓存适配器，修改详情读路径和成功更新后的失效点。

## 2. 环境与连接

在 `pom.xml` 增加依赖，版本由 Spring Boot 4.1.1 管理：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

个人 Redis 实例启动后，在 `application-dev.yml` 配置（生产用受控配置源，不提交口令）：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      connect-timeout: 1s
      timeout: 1s
```

`spring-boot-starter-data-redis` 默认使用 Lettuce 客户端；`StringRedisTemplate` 负责字符串键和值。先在个人实例执行 `PING`，应得到 `PONG`。不要把 Redis 测试账号或端口暴露到公网，也不要在日志打印完整连接串。连接失败先核对地址、端口、认证和服务日志；`PING` 成功仍不代表应用序列化、TTL 或业务逻辑正确。

## 3. 单点最小示例：写入、读取、到期

在 `src/main/java/com/example/employee/cache/RedisProbe.java` 建立只用于练习的组件：

```java
@Component
public class RedisProbe {
    private final StringRedisTemplate redis;

    public RedisProbe(StringRedisTemplate redis) { this.redis = redis; }

    public void put(String value) {
        redis.opsForValue().set("lab:redis:hello", value, Duration.ofSeconds(30));
    }

    public String get() { return redis.opsForValue().get("lab:redis:hello"); }
}
```

在测试中先 `put("ok")` 再断言 `get()` 是 `ok`，查看 TTL 大于 0；30 秒后值应不存在。测试键必须用 `lab:` 前缀，清理只删除自己创建的键，不对共用实例执行 `FLUSHALL`。字符串之外，Redis 还有 Hash、List、Set、Sorted Set；本任务不需为熟悉 API 而逐个引入。删除 `RedisProbe` 后再开始业务缓存。

## 4. 员工详情的 cache-aside

先明确缓存契约：键 `employee:detail:v1:{id}`；值只含允许该详情接口公开的字段；TTL 5 分钟；不存在的员工不缓存；缓存异常退回 MySQL。`v1` 使字段格式变化时可换键前缀，不能将 Java Entity 连同内部状态直接序列化公开。若详情响应依用户权限不同，应按权限设计键，或在权限检查后仅缓存不含用户特有数据的公共 DTO。

新建 `cache/EmployeeDetailCache.java`，以主线的 `EmployeeResponse` 作为 JSON 负载；实际构造器/字段按第9章工程中的 DTO 调整。Boot 4默认使用Jackson 3，下面的 `ObjectMapper` 与 `JacksonException` 分别从 `tools.jackson.databind` 和 `tools.jackson.core` 导入：

```java
@Component
public class EmployeeDetailCache {
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Logger log = LoggerFactory.getLogger(EmployeeDetailCache.class);
    private record CachedEmployee(Long id, String name, String department,
                                  String email) {}
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public EmployeeDetailCache(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    private String key(long id) { return "employee:detail:v1:" + id; }

    public Optional<EmployeeResponse> find(long id) {
        try {
            String value = redis.opsForValue().get(key(id));
            if (value == null) return Optional.empty();
            CachedEmployee item = json.readValue(value, CachedEmployee.class);
            return Optional.of(new EmployeeResponse(item.id(), item.name(),
                    item.department(), item.email()));
        } catch (JacksonException | DataAccessException ex) {
            // 缓存坏值或连接故障不能让详情接口失效；记录键和原因，不打印个人数据。
            log.warn("Employee detail cache read failed, key={}", key(id), ex);
            return Optional.empty();
        }
    }

    public void put(long id, EmployeeResponse value) {
        try {
            CachedEmployee item = new CachedEmployee(value.getId(),
                    value.getName(), value.getDepartment(), value.getEmail());
            redis.opsForValue().set(key(id), json.writeValueAsString(item), TTL);
        } catch (JacksonException | DataAccessException ex) {
            // DB 查询已经成功；缓存写入失败只降低性能。
            log.warn("Employee detail cache write failed, key={}", key(id), ex);
        }
    }

    public void evict(long id) {
        try {
            redis.delete(key(id));
        } catch (DataAccessException ex) {
            // 数据库已经提交，不能把缓存故障伪装成业务回滚。
            log.warn("Employee detail cache eviction failed, key={}", key(id), ex);
        }
    }
}
```

给现有详情 Service 的**权限检查之后**加入：

```java
return cache.find(id).orElseGet(() -> {
    EmployeeResponse value = loadEmployeeDetailFromDatabase(id); // 复用原有查询、404与DTO转换
    cache.put(id, value);
    return value;
});
```

这里的 `loadEmployeeDetailFromDatabase` 代表把原有详情查询逻辑提成私有方法，不是新 Mapper。`Optional.empty()` 同时表示未命中和缓存故障；生产日志/指标应区分两种原因。首次请求查询 MySQL 并写 Redis，第二次命中 Redis。可在测试中用 Spy 验证 Mapper 只调用一次，而不要用“感觉响应更快”代替证据。

## 5. 更新后失效、并发与故障边界

第13章的员工修改事务会同时更新 `employees` 和 `employee_change_logs`。**不能在事务提交前删除缓存**：并发读可能立即把尚未提交的旧值重新写入。将失效放在事务成功提交后；一个集中做法是发布事件，由 `@TransactionalEventListener(phase = AFTER_COMMIT)` 处理：

```java
public record EmployeeChanged(long employeeId) {}

@Component
public class EmployeeCacheInvalidator {
    private final EmployeeDetailCache cache;
    public EmployeeCacheInvalidator(EmployeeDetailCache cache) { this.cache = cache; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(EmployeeChanged event) { cache.evict(event.employeeId()); }
}
```

在现有 `@Transactional` 更新方法完成两次写入后发布 `EmployeeChanged`；新增、删除或改变接口可见字段的路径也要按键失效。此事件只在活动事务成功提交时触发；不能从无事务路径指望它运行。`evict` 失败时数据库仍已提交，所以需要记录告警，TTL 是最后的过期上限。特别注意“读未命中→查询旧值→并发更新提交并删除键→读者写入旧值”的竞态，简单 cache-aside 无法保证强一致。要求严格实时正确的接口应绕过缓存，或设计版本键/短 TTL 并在测试中验证。Redis 故障、清空或重启时服务应走 MySQL；大量键同时过期可能击穿数据库，应观察负载、加适度 TTL 抖动与热点保护，而非盲目延长缓存时间。

`@Cacheable` 是 Spring Cache 的声明式选择，但缓存名称、键、序列化、失效时机仍需设计；本章用显式适配器让行为可见。Session 存储是另一项课题，不能把本章的详情缓存直接当作[第15章](../mainline/15_session_login.md)的登录会话方案。

## 6. 实操与验收

1. 实现单键 30 秒写/读/过期，再删掉探针组件。提交缓存适配器和详情读路径。
2. 连续读取员工 1001 两次，断言响应一致且第二次不调用 Mapper；检查键前缀与 TTL。
3. 修改部门并写履历，提交后读取详情应显示新值；故意让履历插入失败，事务回滚后旧缓存仍与数据库一致。
4. 停止个人 Redis，详情读取仍从 MySQL 返回正确结果；恢复 Redis 后再次填充。
5. 记录权限不同的用户是否可能看到不属于自己的数据；若有可能，必须调整缓存位置或键设计后才通过验收。

留下稳定状态：数据库是权威数据源，缓存键有 TTL，所有写路径有提交后失效，测试通过。下一步 Batch 不依赖 Redis；最终项目只会把它用于可重建的状态副本。

参考：[Spring Boot Redis 连接](https://docs.spring.io/spring-boot/3.5/how-to/nosql.html)、[Spring Data Redis Template](https://docs.spring.io/spring-data/redis/reference/redis/template.html)、[Spring 事务事件](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)。
