# 第5章 MyBatis CRUD 操作与参数映射（详细属性版）

---

## 一、概述

MyBatis 提供四种基本 SQL 标签用于执行数据库操作：

- `<select>`：执行查询语句。
- `<insert>`：执行插入语句。
- `<update>`：执行更新语句。
- `<delete>`：执行删除语句。

本章将详细讲解每个标签的属性、作用与示例。

---

## 二、<select> 标签详解

**作用：** 执行查询操作，返回结果可以是单个对象、集合或 Map。

**常用属性说明（无序列表）：**

- `id`：唯一标识符，对应 Mapper 接口的方法名。  
- `parameterType`：指定输入参数类型（可选），可以是类全名或别名。  
- `resultType`：指定返回类型（简单结果映射时使用）。  
- `resultMap`：指定结果映射规则（复杂结构时使用，与 resultType 互斥）。  
- `statementType`：指定使用的 Statement 类型，可取值：`STATEMENT`、`PREPARED`（默认）、`CALLABLE`。  
- `fetchSize`：提示数据库每次抓取的记录数。  
- `timeout`：语句超时时间，单位为秒。  
- `useCache`：是否启用二级缓存（默认 `true`）。  
- `flushCache`：是否清空缓存（默认 `false`）。  
- `databaseId`：指定数据库厂商 ID，用于多数据库环境。

**示例：**

```xml
<select id="selectUserById" parameterType="int" resultType="com.example.entity.User">
  SELECT id, name, age FROM user WHERE id = #{id}
</select>
```

---

## 三、<insert> 标签详解

**作用：** 执行插入操作，支持主键回填。

**常用属性说明：**

- `id`：唯一标识符，对应 Mapper 方法名。  
- `parameterType`：指定插入参数类型。  
- `flushCache`：是否执行后清除缓存，默认 `true`。  
- `timeout`：语句执行超时时间。  
- `statementType`：`STATEMENT`、`PREPARED`（默认）、`CALLABLE`。  
- `keyProperty`：Java 对象中用于接收数据库自动生成主键的属性名。  
- `keyColumn`：数据库中主键列的列名。  
- `useGeneratedKeys`：是否启用 JDBC 自动生成主键，默认 `false`。  
- `databaseId`：用于区分不同数据库厂商。

**示例：**

```xml
<insert id="insertUser" parameterType="com.example.entity.User"
        useGeneratedKeys="true" keyProperty="id">
  INSERT INTO user(name, age) VALUES(#{name}, #{age})
</insert>
```

---

## 四、<update> 标签详解

**作用：** 执行修改数据操作。

**常用属性说明：**

- `id`：唯一标识符，对应接口方法名。  
- `parameterType`：传入的参数类型。  
- `flushCache`：是否清除缓存，默认 `true`。  
- `timeout`：SQL 执行超时时间。  
- `statementType`：语句类型，默认 `PREPARED`。  
- `databaseId`：数据库厂商标识。

**示例：**

```xml
<update id="updateUser" parameterType="com.example.entity.User">
  UPDATE user
  SET name = #{name}, age = #{age}
  WHERE id = #{id}
</update>
```

---

## 五、<delete> 标签详解

**作用：** 执行删除操作。

**常用属性说明：**

- `id`：唯一标识符，对应 Mapper 方法名。  
- `parameterType`：输入参数类型。  
- `flushCache`：是否清除缓存（默认 `true`）。  
- `timeout`：超时时间。  
- `statementType`：`STATEMENT`、`PREPARED`（默认）、`CALLABLE`。  
- `databaseId`：数据库厂商标识。

**示例：**

```xml
<delete id="deleteUser" parameterType="int">
  DELETE FROM user WHERE id = #{id}
</delete>
```

---

## 六、参数映射与多参数传递

- MyBatis 默认只接收一个参数对象。  
- 多参数可使用 `@Param("name")` 注解指定名称。  
- 集合参数通过 `<foreach>` 标签处理。

**示例：**

```java
List<User> selectByIds(@Param("ids") List<Integer> ids);
```

```xml
<select id="selectByIds" resultType="User">
  SELECT * FROM user WHERE id IN
  <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
</select>
```

---

## 七、主键回填机制

- `useGeneratedKeys="true"`：启用 JDBC 自动生成主键。  
- `keyProperty`：指定 Java 对象中接收主键值的字段。  
- `keyColumn`：数据库表中对应的列名。

**示例：**

```xml
<insert id="insertUser" useGeneratedKeys="true" keyProperty="id" keyColumn="id">
  INSERT INTO user(name, age) VALUES(#{name}, #{age})
</insert>
```

---

## 八、完整 Mapper 示例

**接口：**

```java
public interface UserMapper {
    User selectUserById(Integer id);
    List<User> selectAllUsers();
    int insertUser(User user);
    int updateUser(User user);
    int deleteUser(Integer id);
}
```

**XML 文件：**

```xml
<mapper namespace="com.example.mapper.UserMapper">

  <select id="selectAllUsers" resultType="User">
    SELECT * FROM user
  </select>

  <insert id="insertUser" parameterType="User" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user(name, age) VALUES(#{name}, #{age})
  </insert>

  <update id="updateUser" parameterType="User">
    UPDATE user SET name=#{name}, age=#{age} WHERE id=#{id}
  </update>

  <delete id="deleteUser" parameterType="int">
    DELETE FROM user WHERE id=#{id}
  </delete>

</mapper>
```

---

## 九、事务与提交

```java
try (SqlSession session = factory.openSession()) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    mapper.insertUser(new User(null, "Tom", 25));
    session.commit(); // 提交事务
}
```

---

## 十、小结

- 四大标签分别对应 CRUD 操作。  
- 每个标签都支持控制缓存、超时、语句类型等属性。  
- `<insert>` 独有主键回填属性。  
- 建议通过 Mapper 接口执行 SQL，避免硬编码 SQL id。  
- 操作完成后务必提交或回滚事务。
