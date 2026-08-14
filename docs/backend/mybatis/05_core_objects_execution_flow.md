# 第5章 核心对象与执行流程

> 本章目标：理解 `SqlSessionFactoryBuilder`、`SqlSessionFactory`、`SqlSession` 和 Mapper 代理对象的作用。

## 一、核心对象

MyBatis 常见核心对象：

| 对象 | 作用 |
| --- | --- |
| `SqlSessionFactoryBuilder` | 根据配置文件创建 `SqlSessionFactory` |
| `SqlSessionFactory` | 创建 `SqlSession` |
| `SqlSession` | 数据库会话，执行 SQL 或获取 Mapper |
| Mapper 代理对象 | MyBatis 动态生成的接口实现 |

## 二、SqlSessionFactoryBuilder

```java
InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
```

`SqlSessionFactoryBuilder` 用完即可丢弃。

它主要负责读取配置并构建 `SqlSessionFactory`。

## 三、SqlSessionFactory

`SqlSessionFactory` 用于创建 `SqlSession`。

```java
SqlSession sqlSession = sqlSessionFactory.openSession();
```

在普通 Java 项目中，`SqlSessionFactory` 通常创建一次，重复使用。

## 四、SqlSession

`SqlSession` 表示一次数据库会话。

常见方法：

| 方法 | 作用 |
| --- | --- |
| `getMapper(Class<T> type)` | 获取 Mapper 代理对象 |
| `selectOne()` | 查询一条数据 |
| `selectList()` | 查询多条数据 |
| `insert()` | 新增 |
| `update()` | 修改 |
| `delete()` | 删除 |
| `commit()` | 提交 |
| `rollback()` | 回滚 |
| `close()` | 关闭会话 |

推荐使用 try-with-resources：

```java
try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
    EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
    Employee employee = mapper.selectById(1L);
    System.out.println(employee);
}
```

### 4.1 SqlSession 的 insert、update、delete 方法

`SqlSession` 也可以直接执行写操作。

语法：

```java
int count = sqlSession.insert("SQL语句ID", 参数对象);
int count = sqlSession.update("SQL语句ID", 参数对象);
int count = sqlSession.delete("SQL语句ID", 参数对象);
```

说明：

| 方法 | 作用 | 返回值 |
| --- | --- | --- |
| `insert()` | 执行新增 SQL | 影响行数 |
| `update()` | 执行修改 SQL | 影响行数 |
| `delete()` | 执行删除 SQL | 影响行数 |

示例：

```java
int count = sqlSession.delete(
        "com.example.mybatis.mapper.EmployeeMapper.deleteById",
        1L
);
sqlSession.commit();
```

这里：

- 第一个参数是 SQL 语句 ID。
- 第二个参数是传给 SQL 的参数。
- 返回值 `count` 表示影响行数。
- 写操作后需要调用 `commit()`。

实际项目中更常使用 Mapper 接口方式：

```java
EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
int count = mapper.deleteById(1L);
sqlSession.commit();
```

## 五、Mapper 代理对象

Mapper 接口没有实现类。

```java
EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
```

MyBatis 会在运行时生成代理对象。

调用：

```java
mapper.selectById(1L);
```

MyBatis 会根据：

- Mapper 接口全名
- 方法名
- XML 中的 `namespace`
- XML 中 SQL 标签的 `id`

找到对应 SQL 并执行。

## 六、完整执行流程

```text
Resources 读取 mybatis-config.xml
  ↓
SqlSessionFactoryBuilder 创建 SqlSessionFactory
  ↓
SqlSessionFactory 打开 SqlSession
  ↓
SqlSession 获取 Mapper 代理对象
  ↓
调用 Mapper 方法
  ↓
根据 namespace + id 找到 SQL
  ↓
执行 SQL
  ↓
结果映射为 Java 对象
```

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| `SqlSession` 没关闭 | 数据库资源未释放 | 使用 try-with-resources |
| 写操作没有提交 | 默认不会自动提交 | 写操作后调用 `commit()` |
| Mapper 找不到 SQL | `namespace` 或 `id` 错误 | 检查接口和 XML |

## 八、本章练习

请完成：

1. 说明四个核心对象的作用。
2. 画出 MyBatis 查询执行流程。
3. 使用 `getMapper()` 获取 Mapper 对象并查询员工。

## 九、本章总结

- `SqlSessionFactoryBuilder` 创建工厂。
- `SqlSessionFactory` 创建会话。
- `SqlSession` 管理一次数据库操作会话。
- Mapper 代理对象负责把接口方法调用转成 SQL 执行。
