# 第8章 MyBatis 缓存与插件机制（超详细版）

---

## 一、前言：为什么要有缓存和插件？

MyBatis 在执行 SQL 时，可能会反复查询相同的数据或执行相似的逻辑。  
为了解决这些性能与扩展性问题，MyBatis 提供了：

- **缓存机制（Cache）** —— 提高性能，减少数据库访问；
- **插件机制（Interceptor）** —— 扩展底层行为，例如 SQL 日志、分页、性能分析等。

---

## 二、缓存机制原理概述

### 1️⃣ 类比理解：银行取钱模型

> **没有缓存时：** 每次取钱都要跑到银行柜台。  
> **有缓存时：** 第一次取钱时柜台记下你的余额，下一次直接告诉你结果，不再查数据库。

### 2️⃣ 缓存分层结构

- **一级缓存（SqlSession 级别）**
    - 默认开启。
    - 作用范围是一次会话（`SqlSession`）。
- **二级缓存（Mapper 命名空间级别）**
    - 需手动开启。
    - 在同一命名空间内多个会话共享。

---

## 三、一级缓存（Local Cache）

### 1️⃣ 工作原理

- 默认开启，作用范围是当前 `SqlSession`。
- 第一次查询后结果会存入缓存（key 为 SQL + 参数 + 映射环境）。
- 相同查询会直接从缓存中取结果，而不再访问数据库。

### 2️⃣ 示例

```java
try (SqlSession session = factory.openSession()) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    User u1 = mapper.selectUserById(1);
    User u2 = mapper.selectUserById(1);
    System.out.println(u1 == u2); // true，命中一级缓存
}
```

### 3️⃣ 缓存失效的情况

- 执行了 `update` / `insert` / `delete`（会刷新缓存）。
- 手动调用 `session.clearCache()`。
- 不同的 `SqlSession` 实例。
- 查询参数或 SQL 环境不同。

---

## 四、二级缓存（Global Cache）

### 1️⃣ 概念与特点

- 命名空间（Mapper）级别的缓存。
- 不同 `SqlSession` 可以共享。
- **生命周期更长**，但需要开发者显式启用。

### 2️⃣ 开启步骤

**（1）在全局配置中启用缓存：**

```xml
<settings>
  <setting name="cacheEnabled" value="true"/>
</settings>
```

**（2）在 Mapper 文件中声明：**

```xml
<mapper namespace="com.example.mapper.UserMapper">
  <cache eviction="LRU" flushInterval="600000" size="512" readOnly="false"/>
</mapper>
```

**（3）开启序列化：**

- 参与缓存的实体类必须实现 `Serializable` 接口。

```java
public class User implements Serializable {
    private Integer id;
    private String name;
    private Integer age;
}
```

### 3️⃣ `<cache>` 标签属性

- `eviction`：缓存清理策略，默认 `LRU`。可选：`LRU`、`FIFO`、`SOFT`、`WEAK`。
    - **LRU（Least Recently Used，最近最少使用）**  
        - 优先移除最近最久未被访问的对象。  
        - 访问数据时刷新其时间戳。  
        - ✅ **优点**：命中率高，是默认策略。  
        - ⚠️ **适用场景**：热点数据明显的系统。  
    - **FIFO（First In First Out，先进先出）**  
        - 最早放入缓存的对象最先被清理。  
        - ✅ **优点**：实现简单，性能稳定。  
        - ⚠️ **缺点**：可能淘汰刚被访问过的对象。  
    - **SOFT（软引用策略）**  
        - 使用 Java 的 `SoftReference` 实现，内存不足时释放缓存。  
        - ✅ **优点**：防止内存溢出。  
        - ⚠️ **适用场景**：内存敏感系统，缓存非关键数据。  
    - **WEAK（弱引用策略）**  
        - 使用 Java 的 `WeakReference` 实现，只要 GC 运行就会回收对象。  
        - ✅ **优点**：极致节省内存。  
        - ⚠️ **缺点**：命中率极低，缓存存活时间短。  
  
- `flushInterval`：刷新间隔（毫秒）。默认无自动刷新。  
- `size`：最多可缓存的对象数。  
- `readOnly`：是否只读（`true` 表示返回相同实例）。  
- `blocking`：若缓存命中失败是否阻塞其他线程读取。  

### 4️⃣ `<cache-ref>` 共享缓存

```xml
<cache-ref namespace="com.example.mapper.OrderMapper"/>
```

表示当前 Mapper 复用另一个 Mapper 的缓存。

### 5️⃣ 二级缓存执行顺序

1. 查询二级缓存是否有结果；  
2. 若无 → 查询数据库并写入一级缓存；  
3. 会话关闭或提交时 → 一级缓存数据写入二级缓存。

---

## 五、缓存清理与优化建议

**缓存失效的触发条件：**

- 执行更新操作（会刷新缓存）。
- Mapper 中 `flushCache="true"`。

**优化建议：**

- 不要缓存频繁变动的数据（例如库存、余额）。  
- 对静态数据（如省份、配置项）可长期缓存。  
- 结合 Redis 等外部缓存提高可靠性。

---

## 六、插件机制（Interceptor）

### 1️⃣ 插件的作用

插件允许你在 MyBatis 执行 SQL 的四大核心阶段插入自定义逻辑，常见用途：

- 打印 SQL 日志；
- 统计 SQL 执行时间；
- 自动分页；
- 动态切换数据源。

### 2️⃣ 原理概述

MyBatis 在执行 SQL 时，会创建以下 4 类核心对象：

- Executor：执行 SQL 语句；
- StatementHandler：管理 SQL 语句；
- ParameterHandler：设置参数；
- ResultSetHandler：处理查询结果。

插件通过 **动态代理（JDK Proxy）**，在这些对象方法调用前后注入逻辑。

```plaintext
MyBatis Object → [ Plugin Proxy ] → [ Original Object ]
```

---

## 七、自定义插件示例：日志拦截器

### 1️⃣ 插件代码

```java
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class LogInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed(); // 执行原方法
        long end = System.currentTimeMillis();
        System.out.println("[MyBatis SQL 执行耗时]：" + (end - start) + " ms");
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        System.out.println("插件属性：" + properties);
    }
}
```

### 2️⃣ 在 mybatis-config.xml 注册

```xml
<plugins>
  <plugin interceptor="com.example.plugin.LogInterceptor">
    <property name="level" value="DEBUG"/>
  </plugin>
</plugins>
```

### 3️⃣ 输出结果

```bash
[MyBatis SQL 执行耗时]：12 ms
```

**说明：**

- `@Intercepts`：声明拦截器。  
- `@Signature`：指定要拦截的接口、方法、参数类型。  
- `plugin()`：包装原始对象，返回代理对象。  
- `setProperties()`：读取配置参数。  

---

## 八、常见插件案例

- **分页插件（PageHelper / 自定义）**  
    - 拦截 `Executor.query()` 方法，修改 SQL 为 `SELECT ... LIMIT ...`。  
- **性能监控插件**  
    - 记录每条 SQL 的执行时间并打印慢查询警告。  
- **字段加解密插件**  
    - 拦截参数和结果集，在入库前加密、出库后解密。  

---

## 九、缓存 vs 插件：差异对比

| 对比项 | 缓存 | 插件 |
|--------|------|------|
| 目的 | 提升性能 | 扩展功能 |
| 生命周期 | SqlSession / Mapper | 全局 |
| 开启方式 | 自动 / 手动 | 在配置中注册 |
| 常见应用 | 结果复用、数据查询 | 分页、日志、加密、动态路由 |
| 实现原理 | 内存存储 + 命名空间管理 | 动态代理 + 拦截器链 |

---

## 十、小结

- 一级缓存自动开启，作用范围为 **SqlSession**。  
- 二级缓存需手动启用，作用范围为 **Mapper 命名空间**。  
- 插件可拦截四大核心对象方法，灵活扩展 MyBatis 行为。  
- **缓存 = 性能优化**，**插件 = 功能增强**。  

---

> ✅ 记忆口诀：  
>
> - 一级缓存自己用（会话级）  
> - 二级缓存大家用（共享级）  
> - 插件像“插电扩展口”，能让 MyBatis 做更多事。
