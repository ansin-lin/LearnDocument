# 第3章 MyBatis 核心对象与执行流程

> 本章目标：掌握 MyBatis的核心接口与类、执行流程、映射器（Mapper）的作用与实现方式，
> 以及 SQL执行过程中的内部组件。

------------------------------------------------------------------------

## 一、MyBatis 三大核心要素

1. **核心接口与类**
2. **MyBatis 核心配置文件（mybatis-config.xml）**
3. **SQL 映射文件（mapper.xml）**

------------------------------------------------------------------------

## 二、MyBatis 核心对象

## 1️⃣ SqlSessionFactoryBuilder

**作用：**\
根据配置文件或 `Configuration` 实例创建 `SqlSessionFactory` 对象。

**典型用法：**

``` java
InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);
```

**源码中的主要方法：**

``` java
build(InputStream inputStream, String environment, Properties properties);
build(Reader reader, String environment, Properties properties);
build(Configuration config);
```

支持三种方式：

- 通过 `InputStream` 读取 XML
- 通过 `Reader` 读取 XML

```java
Reader reader =
Resources.getResourceAsReader("mybatis-config.xml");

SqlSessionFactory factory =
new SqlSessionFactoryBuilder().build(reader);
```

- 通过 Java 代码（Configuration 对象）构建

```java
DataSource dataSource = new PooledDataSource(
        "com.mysql.cj.jdbc.Driver",
        "jdbc:mysql://localhost:3306/test",
        "root",
        "123456"
);

TransactionFactory transactionFactory =
new JdbcTransactionFactory();

Environment environment =
new Environment("development",
                transactionFactory,
                dataSource);

Configuration configuration =
new Configuration(environment);

configuration.addMapper(UserMapper.class);

SqlSessionFactory factory =
new SqlSessionFactoryBuilder().build(configuration);
```

**生命周期：**

- 用完即丢
- 通常只在启动时创建
- 最佳作用域：**方法内部局部变量**

------------------------------------------------------------------------

## 2️⃣ SqlSessionFactory

**作用：**

创建 `SqlSession` 的工厂对象。

``` java
public interface SqlSessionFactory {
    SqlSession openSession();
    SqlSession openSession(boolean autoCommit);
    SqlSession openSession(Connection connection);
    SqlSession openSession(TransactionIsolationLevel level);
    SqlSession openSession(ExecutorType execType);
    Configuration getConfiguration();
}
```

**特点：**

- 线程安全
- 全局唯一
- 生命周期与应用程序一致

**最佳实践：**

通常在项目启动时创建一次，并作为 **单例对象** 使用。

------------------------------------------------------------------------

## 3️⃣ SqlSession

**作用：**

MyBatis 执行数据库操作的核心接口。

常用方法：

``` java
<T> T getMapper(Class<T> type);
int insert(String statement, Object parameter);
int update(String statement, Object parameter);
int delete(String statement, Object parameter);
<E> List<E> selectList(String statement, Object parameter);
<T> T selectOne(String statement, Object parameter);
void commit(boolean force);
void rollback(boolean force);
void clearCache();
```

**主要用途：**

1. 获取 Mapper 代理对象
2. 直接执行 SQL

**生命周期：**

- 对应一次数据库会话
- 线程不安全
- 建议在 **方法或请求级别使用**

------------------------------------------------------------------------

## 4️⃣ Mapper（映射器）

**定义：**

Mapper 由 **接口 + XML 文件（或注解）** 组成。

**主要作用：**

- 定义 SQL
- 定义参数类型
- 定义返回值类型
- 配置结果映射
- 支持动态 SQL

**命名空间规则：**

``` xml
<mapper namespace="com.example.mapper.UserMapper">
```

必须与接口 **全限定名一致**。

------------------------------------------------------------------------

## 三、SqlSessionFactoryBuilder → SqlSessionFactory → SqlSession 执行流程

``` mermaid
graph LR
A[SqlSessionFactoryBuilder] --> B[SqlSessionFactory]
B --> C[SqlSession]
C --> D[执行SQL]
D --> E[返回结果]
```

流程说明：

1. `SqlSessionFactoryBuilder` 读取配置文件
2. 构建 `SqlSessionFactory`
3. 通过工厂创建 `SqlSession`
4. SqlSession 执行 SQL
5. 返回查询结果

------------------------------------------------------------------------

## 四、MyBatis 内部执行组件

当 SqlSession 执行 SQL 时，MyBatis 内部会使用多个核心组件。

### 1️⃣ Executor（执行器）

**作用：**

负责真正执行 SQL，并管理缓存。

MyBatis 提供三种执行器：

### SimpleExecutor（默认）

- 每次执行 SQL 都创建新的 Statement

### ReuseExecutor

- 复用 Statement

### BatchExecutor

- 批量执行 SQL

示例：
    insert
    insert
    insert
    统一提交

------------------------------------------------------------------------

### 2️⃣ MappedStatement

**作用：**

封装一条 SQL 的全部信息。

例如 Mapper XML：

``` xml
<select id="selectUser" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>
```

MyBatis 会将其解析为 `MappedStatement` 对象。

包含信息：

- SQL 语句
- SQL id
- 参数类型
- 返回类型
- SQL 类型（select/insert/update/delete）

------------------------------------------------------------------------

### 3️⃣ StatementHandler

**作用：**

负责 JDBC Statement 的创建与执行。

执行流程：
    创建Statement
    ↓
    执行SQL
    ↓
    返回结果

------------------------------------------------------------------------

### 4️⃣ ParameterHandler

**作用：**

负责 SQL 参数绑定。
示例：
    SELECT * FROM user WHERE id = #{id}

转换为：
    SELECT * FROM user WHERE id = ?
并绑定参数：
    ? = 1

------------------------------------------------------------------------

## 5️⃣ ResultSetHandler

**作用：**

处理数据库返回的结果集，并映射为 Java 对象。

数据库返回：
    id name age
    1  Tom  20

转换为：

```java
User{
 id=1,
 name="Tom",
 age=20
}
```

------------------------------------------------------------------------

## 五、核心对象关系结构

```java
    SqlSessionFactoryBuilder
            ↓
    SqlSessionFactory
            ↓
    SqlSession
            ↓
    MapperProxy
            ↓
    Executor
            ↓
    StatementHandler
    ParameterHandler
    ResultSetHandler
            ↓
    JDBC
```

------------------------------------------------------------------------

## 六、Mapper 映射器实现方式

### 1 XML 实现

**接口：**

``` java
public interface WebsiteMapper {
    List<Website> selectAllWebsite();
}
```

**XML：**

``` xml
<mapper namespace="net.biancheng.mapper.WebsiteMapper">
    <select id="selectAllWebsite" resultType="net.biancheng.po.Website">
        select * from website
    </select>
</mapper>
```

------------------------------------------------------------------------

### 2 注解实现

``` java
public interface WebsiteMapper2 {

    @Select("select * from website")
    List<Website> selectAllWebsite();
}
```

优缺点：

优点：简单\
缺点：复杂 SQL 难维护

------------------------------------------------------------------------

## 七、SQL 执行的两种方式

### 1 通过 SqlSession 执行

``` java
Website website =
session.selectOne("net.biancheng.mapper.WebsiteMapper.getWebsite",1);
```

常用方法：

- selectOne
- selectList
- insert
- update
- delete

------------------------------------------------------------------------

### 2 通过 Mapper 接口执行

``` java
WebsiteMapper mapper = session.getMapper(WebsiteMapper.class);
List<Website> list = mapper.selectAllWebsite();
```

MyBatis 使用 **MapperProxy 动态代理**实现接口。

------------------------------------------------------------------------

## 八、两种方式对比

  | 对比项     | SqlSession方式   | Mapper方式 |
  | ---------- | ---------------- | -----------|
  |可读性      | 低               | 高         |
  |可维护性    | 低               | 高         |
  |类型安全    | 差               | 好         |
  |推荐程度    | ❌               | ✅         |

------------------------------------------------------------------------

## 九、综合示例

``` java
InputStream config =
Resources.getResourceAsStream("mybatis-config.xml");

SqlSessionFactory ssf =
new SqlSessionFactoryBuilder().build(config);

SqlSession ss = ssf.openSession();

WebsiteMapper mapper = ss.getMapper(WebsiteMapper.class);

List<Website> list = mapper.selectAllWebsite();

for (Website site : list) {
    System.out.println(site);
}

ss.commit();
ss.close();
```

------------------------------------------------------------------------

## 十、最佳实践

- SqlSessionFactory **全局单例**
- SqlSession **每次使用后关闭**
- Mapper namespace 必须与接口全限定名一致
- 推荐使用 **Mapper 接口方式执行 SQL**
- 复杂 SQL 推荐使用 **XML 映射文件**

------------------------------------------------------------------------

## 十一、本章总结

MyBatis 的整体架构可以理解为三层：

```java
    SqlSessionFactory
            ↓
    SqlSession
            ↓
    Mapper
```

内部通过 Executor、StatementHandler、ParameterHandler、ResultSetHandler
等组件完成 SQL 执行。

理解这些核心对象和执行流程，是学习 MyBatis **缓存机制、插件机制、动态
SQL 等高级特性**的基础。
