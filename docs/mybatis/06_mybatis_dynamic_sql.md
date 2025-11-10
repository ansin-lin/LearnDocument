# 第6章 MyBatis 动态 SQL 标签详解（详细属性版）

---

## 一、概述

动态 SQL 是 MyBatis 的一大核心特性。  
它允许开发者在 XML 中通过逻辑判断和循环来动态生成 SQL 语句，避免拼接字符串和条件判断带来的麻烦。

动态 SQL 主要用于：

- 根据条件动态拼接查询条件；
- 动态生成 `UPDATE` 或 `INSERT` 语句；
- 批量查询或批量插入。

---

## 二、常用动态 SQL 标签

MyBatis 提供了以下八种常用标签：  
`<if>`、`<choose>`、`<when>`、`<otherwise>`、`<trim>`、`<where>`、`<set>`、`<foreach>`、`<bind>`。

---

## 三、<if> 标签

**作用：**  
根据条件判断是否拼接某段 SQL。

**属性说明：**

- `test`：逻辑表达式，用于判断条件是否成立。支持 OGNL 表达式，例如 `age != null and age > 18`。

**示例：**

```xml
<select id="selectUser" resultType="User">
  SELECT * FROM user where 1 = 1
    <if test="name != null">AND name LIKE CONCAT('%', #{name}, '%')</if>
    <if test="age != null">AND age = #{age}</if>
</select>
```

---

## 四、<choose> / <when> / <otherwise> 标签

**作用：**  
实现类似 Java 中 `switch-case` 的多分支逻辑。

**属性说明：**

- `<choose>`：外层标签，用于包裹 `<when>` 与 `<otherwise>`。
- `<when>`：条件分支，属性：  
    - `test`：布尔表达式，成立时执行对应 SQL。
- `<otherwise>`：所有 `<when>` 条件都不满足时执行。

**示例：**

```xml
<select id="selectUserByCondition" resultType="User">
  SELECT * FROM user
  <choose>
    <when test="id != null">WHERE id = #{id}</when>
    <when test="name != null">WHERE name = #{name}</when>
    <otherwise>WHERE age > 18</otherwise>
  </choose>
</select>
```

---

## 五、<trim> 标签

**作用：**  
用于自定义 SQL 片段的前缀、后缀和多余字符的移除逻辑。

**属性说明：**

- `prefix`：指定要添加的前缀内容，例如 `"WHERE"`、`"SET"`。  
- `prefixOverrides`：指定要删除的前缀字符串，如 `"AND|OR"`。  
- `suffix`：指定要添加的后缀字符串。  
- `suffixOverrides`：指定要删除的后缀字符串，如 `","`。

**示例：**

```xml
<trim prefix="WHERE" prefixOverrides="AND|OR">
  <if test="name != null">AND name=#{name}</if>
  <if test="age != null">AND age=#{age}</if>
</trim>
```

---

## 六、<where> 标签

**作用：**  
智能生成 `WHERE` 语句，会自动去除多余的 `AND` 或 `OR`，避免语法错误。

**属性说明：**

- 无其他属性，主要用于简化条件拼接。

**示例：**

```xml
<select id="selectDynamic" resultType="User">
  SELECT * FROM user
  <where>
    <if test="name != null">AND name=#{name}</if>
    <if test="age != null">AND age=#{age}</if>
  </where>
</select>
```

---

## 七、<set> 标签

**作用：**  
用于 `UPDATE` 语句中动态拼接 `SET` 子句。  
会自动去除最后一个多余的逗号。

**属性说明：**

- 无额外属性，仅控制自动逗号去除。

**示例：**

```xml
<update id="updateUser">
  UPDATE user
  <set>
    <if test="name != null">name=#{name},</if>
    <if test="age != null">age=#{age}</if>
  </set>
  WHERE id=#{id}
</update>
```

---

## 八、<foreach> 标签

**作用：**  
在 SQL 中实现循环，用于 `IN` 查询、批量插入等场景。

**属性说明：**

- `collection`：要遍历的集合名称（如 list、array、map、@Param 指定名称）。  
- `item`：集合中每个元素的变量名。  
- `index`：当前索引或键名（可选）。  
- `open`：循环开始符，例如 `"("`。  
- `separator`：元素之间的分隔符，例如 `","`。  
- `close`：循环结束符，例如 `")"`。  

**示例：**

```xml
<select id="selectByIds" resultType="User">
  SELECT * FROM user WHERE id IN
  <foreach collection="ids" item="id" open="(" separator="," close=")">
    #{id}
  </foreach>
</select>
```

**批量插入示例：**

```xml
<insert id="insertBatch" parameterType="list">
  INSERT INTO user(name, age) VALUES
  <foreach collection="list" item="user" separator=",">
    (#{user.name}, #{user.age})
  </foreach>
</insert>
```

---

## 九、<bind> 标签

**作用：**  
在 OGNL 表达式中定义一个变量，可以在 SQL 中复用。常用于模糊搜索。

**属性说明：**

- `name`：变量名称。  
- `value`：OGNL 表达式计算结果。

**示例：**

```xml
<select id="selectLike" resultType="User">
  <bind name="pattern" value="'%' + name + '%'"/>
  SELECT * FROM user WHERE name LIKE #{pattern}
</select>
```

---

## 十、综合应用示例

```xml
<select id="selectComplex" resultType="User">
  SELECT * FROM user
  <where>
    <if test="name != null and name != ''">AND name LIKE CONCAT('%', #{name}, '%')</if>
    <if test="minAge != null">AND age &gt;= #{minAge}</if>
    <if test="maxAge != null">AND age &lt;= #{maxAge}</if>
  </where>
  <choose>
    <when test="sort == 'asc'">ORDER BY age ASC</when>
    <otherwise>ORDER BY age DESC</otherwise>
  </choose>
</select>
```

---

## 十一、最佳实践建议

- `<if>` 语句应尽量使用明确判断条件。  
- `<where>` 和 `<set>` 标签自动优化 SQL 结构，推荐优先使用。  
- `<foreach>` 循环时应避免 SQL 注入，确保传入集合安全。  
- `<bind>` 在模糊搜索或动态片段时非常高效。  
- 复杂场景中可结合 `<trim>` 自定义灵活拼接逻辑。

---

## 十二、小结

- 动态 SQL 提升了 MyBatis 的灵活性，减少大量 if-else 代码。  
- 每个标签都可在 SQL 中实现条件控制、循环拼接等功能。  
- 结合使用 `<where>`、`<set>` 与 `<foreach>` 能应对大多数动态 SQL 场景。  
