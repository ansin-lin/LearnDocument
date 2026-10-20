# Redis 独立扩展专题：八章内容速览

本专题面向已了解 Java、Spring Boot 基础的新人。学习目标是：知道 Redis 能解决什么问题，会用常见命令和 Spring Boot 方法，并能判断什么情况下适合使用缓存、共享 Session 或计数。这里使用独立练习环境，不要求改造员工管理项目。

## 1. Redis 是什么，为什么不能直接替代 MySQL

Redis 是以内存操作为主的键值数据存储系统，支持多种数据结构，常用于高频读取缓存、共享 Session、计数和排行榜。MySQL 通常保存员工、订单等权威业务数据；Redis 中的缓存往往只是可以重新生成的副本。

| 问题 | MySQL | Redis |
|---|---|---|
| 如何组织数据 | 表、行、列与关系 | Key、Value 与多种数据类型 |
| 如何读取 | SQL | Redis 命令或客户端 API |
| 常见职责 | 权威业务数据、关系与事务 | 快速访问、临时或共享状态 |
| 持久化与事务 | 关系数据库事务机制 | 可配置 RDB/AOF；事务语义不同 |

例如同一员工详情短时间被大量重复查询时，可以先查 Redis；未命中再查 MySQL，并给缓存设置过期时间。若数据频繁变化、很少重复读取、要求强一致，或不同用户可见内容不同，增加缓存可能得不偿失。Redis 快，不代表所有接口都应该加缓存。

## 2. 启动 Redis，掌握 Key 与 TTL

以下命令用于已安装 Docker 的个人电脑，在 PowerShell 中运行。端口只绑定本机；正式环境还需认证与网络访问控制。

```powershell
docker run -d --name redis-training -p 127.0.0.1:6379:6379 redis:7
docker exec -it redis-training redis-cli
```

进入 `redis-cli` 后输入：

```redis
PING
SET training:employee:1001 Tanaka EX 300
GET training:employee:1001
TTL training:employee:1001
EXISTS training:employee:1001
TYPE training:employee:1001
SCAN 0 MATCH training:employee:* COUNT 50
```

`PING` 应返回 `PONG`；`SET ... EX 300` 表示保存 300 秒；`GET` 读取，`TTL` 查看剩余秒数，`EXISTS` 判断 Key 是否存在，`TYPE` 查看类型。`TTL=-1` 表示 Key 存在但未设置过期；`-2` 表示 Key 不存在。`SCAN` 需要按返回游标继续遍历直到游标为 0，单次结果不保证包含全部匹配 Key。不要把 `KEYS *` 当作大型生产实例的日常查询方法。

练习结束可用 `DEL training:employee:1001` 只删除自己的 Key。R03～R08 若仍需使用 Redis，请保留容器；专题全部结束后再停止并删除个人练习容器，容器中未持久化的数据可能丢失。

## 3. 五种常见数据类型怎么选

| 类型 | 最小命令示例 | 常见用途 |
|---|---|---|
| String | `SET`、`GET`、`INCR` | 文本/JSON 缓存、计数 |
| Hash | `HSET`、`HGET`、`HGETALL` | 对象的少量字段 |
| List | `RPUSH`、`LRANGE` | 有序元素练习、简单列表 |
| Set | `SADD`、`SISMEMBER` | 去重、判断成员是否存在 |
| Sorted Set | `ZADD`、`ZRANGE` | 按分数排序的排行榜 |

JSON 文本放进 Redis 仍是 String，不会自动变成 Hash。一个 Key 当前只对应一种数据类型；用不匹配的命令读取会得到 `WRONGTYPE`。Key 建议带业务前缀，例如 `training:employee:detail:1001`；不要在 Key 中放密码、令牌或个人敏感信息。生产任务队列还要处理确认、失败恢复与重复消费，不能只凭 List 的入队/出队命令认定其可靠。

## 4. Spring Boot 如何调用 Redis

先区分两件事：R02 的容器运行的是 Redis **服务器**；Spring Boot 中的依赖只让 Java 程序获得连接 Redis 的**客户端**。本节使用独立的 Spring Boot 4.1.1 / Java 17 练习工程，不接入员工管理项目。Maven `pom.xml` 增加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

此 Starter 由 Spring Boot 父 POM 管理版本，默认使用 Lettuce 客户端；它不会替你启动 Redis 服务器。把下面配置保存为 `src/main/resources/application-local.yml`，运行时激活 `local` Profile：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3s
```

`host`、`port` 指向 R02 在本机开放的 Redis；`timeout` 限制等待时间，避免故障时长时间卡住。正式环境的认证信息不能写死在教材或仓库中。若应用也在 Docker 容器内，`localhost` 指应用容器自己，应改用可访问的 Redis 服务地址。

先学 `StringRedisTemplate`：它把 Key 和 Value 当作字符串，适合姓名、计数文本或已经转换好的 JSON 文本。Spring 根据依赖和配置创建该 Bean；业务 Service 用构造器接收它，而不是在方法里反复 `new` 客户端。下面是独立练习工程 `com.example.redispractice` 包中 `EmployeeNameStore.java` 的完整类；启动类须位于同包或父包，且 Redis 已启动：

```java
package com.example.redispractice;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmployeeNameStore {
    private final StringRedisTemplate redis;

    public EmployeeNameStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(Long employeeId, String name) {
        redis.opsForValue().set(key(employeeId), name, Duration.ofMinutes(5));
    }

    public String find(Long employeeId) {
        return redis.opsForValue().get(key(employeeId));
    }

    public boolean remove(Long employeeId) {
        return Boolean.TRUE.equals(redis.delete(key(employeeId)));
    }

    private String key(Long employeeId) {
        return "training:employee:name:" + employeeId;
    }
}
```

调用顺序可以是 `save(1001L, "Tanaka")` → `find(1001L)` 得到 `Tanaka` → `remove(1001L)` → 再次 `find(1001L)` 得到 `null`。`opsForValue()` 对应 Redis String；`set(key, value, Duration)` 同时写值与 TTL，过期后不再读到旧值。`get` 返回 `null` 只表示缓存没有这个 Key，不能据此推断数据库中也没有员工。`delete` 的返回值表示本次是否实际删除了 Key，代码用 `Boolean.TRUE.equals(...)` 安全处理可能的 `null`。

学过 R03 的数据类型后，再按需求选择操作入口：Hash 用 `opsForHash()` 读写字段，Set 用 `opsForSet()` 判断成员，计数可用 `opsForValue().increment(key)`。这些方法不会替业务决定 Key 命名、过期时间或数据一致性；先掌握上面的 String 单点示例，再查对应类型的详细 API。

`RedisTemplate<K,V>` 能处理更一般的 Key/Value，但必须明确序列化器。默认 Java 序列化可能使 CLI 看到非直观的字节，也带来兼容性和安全边界；不要把任意 DTO 直接作为长期共享缓存。若要存对象，可先约定 JSON 格式并做字段版本兼容设计。连接失败通常会在实际执行 Redis 命令时暴露；不要捕获异常后伪造“查到员工”的成功结果。

## 5. 缓存何时使用：Cache Aside 与 Spring Cache

缓存解决的是**重复读取同一份数据的成本**，不是替代数据库。比如员工详情在短时间被很多人查询、更新却很少，原始查询又确实有成本，可考虑把详情暂存 Redis。决定是否缓存，先看四件事：

| 判断问题 | 比较适合缓存 | 应谨慎或暂不缓存 |
|---|---|---|
| 相同数据会反复读取吗 | 热门详情、配置或字典数据 | 几乎每次查询都不同 |
| 原始查询成本高吗 | 重复查询明显增加数据库负载 | 查询已很轻，缓存命中率低 |
| 能接受多长时间旧值 | 可接受短暂旧值，能设置 TTL 和更新失效 | 必须立即准确，如关键结算金额 |
| 权限与故障如何处理 | 缓存内容对使用者相同，故障时有明确策略 | 用户可见字段不同，或 Redis 故障会破坏安全边界 |

表中条件不是“满足一项就必须用”的公式。先观察重复访问和原始数据源压力，再考虑维护 Key、TTL、序列化、失效和容量的额外成本。MySQL 等原始数据源仍是权威来源。

### 手动 Cache Aside：读与写分别发生什么

Cache Aside 的**读取**顺序是：①根据业务标识组成 Key；②查 Redis；③命中就返回；④未命中才读原始数据源；⑤查到数据后写 Redis 并设置 TTL；⑥返回结果。下面是 Service 方法中的核心片段，`loadNameFromSource(id)` 代表从原始数据源查询，省略了类、依赖及异常处理，不能单独粘贴运行：

```java
String key = "training:employee:name:" + id;
String cachedName = redis.opsForValue().get(key);
if (cachedName != null) {
    return cachedName;
}

String name = loadNameFromSource(id);
if (name != null) {
    redis.opsForValue().set(key, name, Duration.ofMinutes(5));
}
return name;
```

第一次查询会回源；第二次在 TTL 内查询同一 Key 才有机会命中。示例不缓存不存在的姓名：若大量请求反复查询不存在的 ID，仍会持续回源，这是“缓存穿透”，见第 7 节。TTL 设得过短会频繁回源，过长则可能放大旧值；应按业务允许的陈旧时间选值，5 分钟只是练习数值。

**更新**不能只改 Redis：应先让权威数据源成功更新，再使旧缓存失效，例如 `redis.delete(key)`；下次读取重新回源。若希望直接更新缓存，必须确认写入的是已成功保存的最新值。若数据库写入处于事务中，还要考虑物理提交与缓存失效的先后；简单的“更新后删除”也不能保证绝对没有并发旧值回填。

### Spring Cache：什么时候改用注解

手动方式适合先理解数据流，或确实需要精细控制 Key、序列化与异常策略。Spring Cache 用注解表达同一类需求，适合在已有 Spring 架构中统一处理方法级缓存。它是缓存**抽象**，不等于 Redis 本身。在第 4 节的 Redis Starter 之外，`pom.xml` 还需增加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

然后启用 `@EnableCaching`，并确认实际使用的是 Redis CacheManager。例如在独立练习工程的 `application-local.yml` 中，将下面字段并入第 4 节已有的 `spring:`，不要出现两个顶层 `spring:`：

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 5m
```

`type: redis` 明确缓存提供者；`time-to-live: 5m` 为 Redis Cache 设置默认过期时间。`@EnableCaching` 负责启用注解处理，可放在练习工程的配置类上，而不必把缓存变成每个运行场景都必须启用的功能：

```java
@Configuration
@EnableCaching
public class CacheConfig {}
```

这是类级片段；在练习工程的 `com.example.redispractice` 包中创建 `CacheConfig.java`，导入 `org.springframework.context.annotation.Configuration` 和 `org.springframework.cache.annotation.EnableCaching`。下面把数据源、构造器和三个缓存方法放在同一个完整的 Service 类中，保存为 `src/main/java/com/example/redispractice/EmployeeNameCacheService.java`：

```java
package com.example.redispractice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class EmployeeNameCacheService {
    // 只作独立练习的数据源；正式业务数据通常保存在数据库。
    private final Map<Long, String> source = new ConcurrentHashMap<>();

    public EmployeeNameCacheService() {
        source.put(1001L, "Tanaka");
    }

    @Cacheable(cacheNames = "employeeNames", key = "#id")
    public String findName(Long id) {
        return source.get(id);
    }

    @CachePut(cacheNames = "employeeNames", key = "#id")
    public String changeName(Long id, String newName) {
        source.put(id, newName);
        return newName;
    }

    @CacheEvict(cacheNames = "employeeNames", key = "#id")
    public void removeName(Long id) {
        source.remove(id);
    }
}
```

`source` 现在明确是类内的线程安全 `Map`，构造器放入 `1001 → Tanaka` 作为起始数据；它不是数据库，重启应用会重新初始化。首次练习前应在**个人** Redis 中清理此前可能存在的 `employeeNames::1001`，避免旧缓存遮盖起始数据。由 Controller 或另一个 Spring Bean 调用 `findName(1001L)`：首次从 `source` 读取并缓存，再次调用命中缓存；调用 `changeName(1001L, "Tanaka Updated")` 后，返回值和缓存都更新；调用 `removeName(1001L)` 后，数据源与缓存项都被删除。`cacheNames` 区分缓存区域，`key = "#id"` 使用方法参数 `id` 作为条目标识。不要把此类的注解与第 4 节的手动 Redis 操作叠加在同一个方法上。

| 注解 | 执行时机 | 适用情形 |
|---|---|---|
| `@Cacheable` | 命中时不执行原方法；未命中才执行并缓存结果 | 重复读取同一数据 |
| `@CachePut` | 总会执行原方法，再将返回值放入缓存 | 写入成功后返回值就是最新数据 |
| `@CacheEvict` | 默认在方法成功执行后删除指定缓存项 | 更新或删除后让下次读取重新取数 |

缓存注解通常通过 Spring 代理生效；同类中的 `this.findName(id)` 可能绕过 `@Cacheable`。三个方法的 Key 必须一致，否则更新或删除的是另一个缓存项。默认 Redis Cache 的值序列化方式是 JDK 序列化；示例的 `String` 可以序列化，自定义 DTO 则要设计安全、兼容的序列化方式。真实数据库事务、Redis 删除失败或权限差异都不能靠注解自动解决；遇到强一致或敏感数据，先评估是否根本不应缓存。

## 6. 什么时候用 Redis 保存 Session

单实例应用的 `HttpSession` 通常在该实例内存中；多个实例若要共享基于 Cookie 的会话，可考虑 Spring Session Redis。浏览器保存的是 Session ID Cookie，服务器保存 Session 内容；Cookie 本身不是 JWT，也不代表用户已经通过认证。

Spring Boot 的 Servlet 应用可加入 `spring-boot-starter-session-data-redis`，由 Spring Session 接管 `HttpSession` 存储。它与普通查询缓存不同：Redis 故障可能直接影响登录状态，不能通过“跳过认证”来降级。Cookie 自动随请求发送时仍需考虑 CSRF；本机 HTTP 演示与正式 HTTPS 的 Secure Cookie 设置也应区分。

如果只有一个实例、没有跨实例共享会话的需求，不必为了使用 Redis 而迁移 Session。采用无状态 Bearer Token 的项目，也不能把 Spring Session 当作必选组件。

## 7. 缓存常见问题与处理方向

| 现象 | 含义 | 优先考虑 |
|---|---|---|
| 不存在的 ID 每次都查原始数据 | 缓存穿透 | 输入校验、短 TTL 空结果；必要时布隆过滤器 |
| 一个热点 Key 过期后请求集中回源 | 缓存击穿 | 同 Key 单飞加载、互斥或限流 |
| 大量 Key 同时过期或 Redis 整体故障 | 缓存雪崩 | 分散 TTL、限流、容量与高可用设计 |
| 原始数据已更新，缓存仍返回旧值 | 一致性问题 | 更新成功后失效、检查并发旧值回填 |

“先更新数据库、再删除缓存”是常见基本策略，但仍可能出现旧请求在删除之后把旧数据回填的竞态。需要严格实时一致的数据可绕过普通缓存。缓存故障时是否回源，也要看原始数据源承受能力；Session 或权限状态不能照搬查询缓存的降级方式。

## 8. 持久化、内存与排查：知道看什么

RDB 是快照，AOF 记录写入操作；两者的配置和故障条件会影响可恢复的数据范围。TTL 到期与内存压力下的 Eviction 是两回事。若 Redis 同时保存 Session，不能随意使用会淘汰重要 Session 的内存策略。

在个人练习环境中可用 `INFO memory`、`INFO stats`、`SLOWLOG GET 10`、`TTL key` 和限定前缀的 `SCAN` 了解内存、请求、慢命令与 Key 状态。`SLOWLOG` 反映 Redis 执行命令的耗时，不等同于接口端到端耗时。排查连接失败先看地址、端口、容器状态、认证/TLS；排查旧值先核对原始数据、缓存 Key、TTL 和失效调用。不要在共享或生产 Redis 上运行 `FLUSHALL`、`FLUSHDB` 或随意全量查 Key。

## 学完后的判断

面对一个需求，先问四个问题：数据是否被重复读取？能容忍多长时间的旧值？Redis 不可用时业务还能否安全运行？Key 与内容是否涉及权限或敏感数据？能说清这些答案，再决定是否使用 Redis，以及选缓存、Session、计数或其他结构。

需要逐步操作、更多示例与排错说明时，可继续阅读原八章：[R01 基础](redis/R01_redis_overview_and_architecture.md) → [R02 CLI 与 TTL](redis/R02_setup_cli_and_ttl.md) → [R03 数据类型](redis/R03_data_types_and_key_design.md) → [R04 Spring Boot 调用](redis/R04_spring_boot_integration.md) → [R05 缓存](redis/R05_employee_cache_and_spring_cache.md) → [R06 Session](redis/R06_spring_session_redis.md) → [R07 故障模式](redis/R07_cache_consistency_and_failure_modes.md) → [R08 运维](redis/R08_operations_and_capstone.md)。
