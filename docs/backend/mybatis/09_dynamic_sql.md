# 第9章 动态 SQL

> 本章目标：掌握 `<if>`、`<where>`、`<set>`、`<foreach>`、`<choose>` 等动态 SQL 标签，能够根据条件生成 SQL。

## 一、为什么需要动态 SQL

查询条件经常不是固定的。

例如员工查询页面：

- 可以按姓名查询。
- 可以按部门查询。
- 可以按邮箱查询。
- 也可以什么条件都不输入。

如果用 Java 字符串拼接 SQL，代码会很混乱，也容易出错。

MyBatis 动态 SQL 可以在 XML 中根据条件生成 SQL。

## 二、if

`<if>` 用于条件判断。

```xml
<select id="selectByCondition" parameterType="Employee" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    WHERE 1 = 1
    <if test="name != null and name != ''">
        AND name LIKE CONCAT('%', #{name}, '%')
    </if>
    <if test="departmentId != null">
        AND department_id = #{departmentId}
    </if>
</select>
```

如果 `name` 为 `null`，姓名条件不会出现在 SQL 中。

## 三、where

`<where>` 会自动添加 `WHERE`，并去掉开头多余的 `AND` 或 `OR`。

```xml
<select id="selectByCondition" parameterType="Employee" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="departmentId != null">
            AND department_id = #{departmentId}
        </if>
    </where>
</select>
```

生成 SQL 示例：

```sql
SELECT id, name, department_id AS departmentId, email
FROM employees
WHERE name LIKE CONCAT('%', ?, '%')
  AND department_id = ?
```

## 四、set

`<set>` 用于动态更新。

```xml
<update id="updateSelective" parameterType="Employee">
    UPDATE employees
    <set>
        <if test="name != null and name != ''">
            name = #{name},
        </if>
        <if test="departmentId != null">
            department_id = #{departmentId},
        </if>
        <if test="email != null">
            email = #{email},
        </if>
    </set>
    WHERE id = #{id}
</update>
```

`<set>` 会自动处理最后多余的逗号。

## 五、foreach

`<foreach>` 常用于 `IN` 查询。

```xml
<select id="selectByIds" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

参数：

| 属性 | 作用 |
| --- | --- |
| `collection` | 集合参数名 |
| `item` | 每次循环的变量名 |
| `open` | 开始符号 |
| `separator` | 分隔符 |
| `close` | 结束符号 |

## 六、choose

`<choose>` 类似 Java 中的 `if else if else`。

```xml
<select id="selectByKeyword" resultType="Employee">
    SELECT id, name, department_id AS departmentId, email
    FROM employees
    <where>
        <choose>
            <when test="name != null and name != ''">
                name LIKE CONCAT('%', #{name}, '%')
            </when>
            <when test="email != null and email != ''">
                email = #{email}
            </when>
            <otherwise>
                id IS NOT NULL
            </otherwise>
        </choose>
    </where>
</select>
```

只会执行第一个满足条件的分支。

## 七、sql

`<sql>` 用于定义可以重复使用的 SQL 片段。

在实际项目中，多个查询经常会使用相同的字段列表或相同的查询条件。如果每个 SQL 都重复写一遍，后续字段变更时需要修改很多位置，容易漏改。

`<sql>` 本身不会单独执行，需要配合 `<include>` 引用。

### 7.1 基本语法

定义公共字段：

```xml
<sql id="employeeColumns">
    id,
    name,
    department_id AS departmentId,
    email
</sql>
```

引用公共字段：

```xml
<select id="selectAll" resultType="Employee">
    SELECT
    <include refid="employeeColumns" />
    FROM employees
</select>
```

说明：

| 标签或属性 | 作用 |
| --- | --- |
| `<sql>` | 定义可复用 SQL 片段 |
| `id` | SQL 片段的名称，同一个 Mapper XML 中不能重复 |
| `<include>` | 引用已经定义好的 SQL 片段 |
| `refid` | 指定要引用的 `<sql>` 片段 ID |

生成后的 SQL 可以理解为：

```sql
SELECT
id,
name,
department_id AS departmentId,
email
FROM employees
```

### 7.2 复用查询字段

字段列表是 `<sql>` 最常见的使用场景。

```xml
<sql id="employeeColumns">
    id,
    name,
    department_id AS departmentId,
    email
</sql>

<select id="selectById" parameterType="long" resultType="Employee">
    SELECT
    <include refid="employeeColumns" />
    FROM employees
    WHERE id = #{id}
</select>

<select id="selectByDepartmentId" parameterType="long" resultType="Employee">
    SELECT
    <include refid="employeeColumns" />
    FROM employees
    WHERE department_id = #{departmentId}
</select>
```

这样写的好处是：如果以后员工表需要增加 `phone` 字段，只需要修改 `employeeColumns`，使用这个片段的查询都可以统一调整。

### 7.3 复用动态查询条件

`<sql>` 也可以和 `<where>`、`<if>` 一起使用。

```xml
<sql id="employeeSearchCondition">
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="departmentId != null">
            AND department_id = #{departmentId}
        </if>
        <if test="email != null and email != ''">
            AND email = #{email}
        </if>
    </where>
</sql>

<select id="selectByCondition" parameterType="Employee" resultType="Employee">
    SELECT
    <include refid="employeeColumns" />
    FROM employees
    <include refid="employeeSearchCondition" />
</select>
```

这里的执行过程可以理解为：

1. MyBatis 先读取 `selectByCondition`。
2. 看到 `<include refid="employeeColumns" />`，把 `employeeColumns` 的内容插入当前位置。
3. 看到 `<include refid="employeeSearchCondition" />`，把动态条件插入当前位置。
4. 根据 `Employee` 参数中的值判断 `<if>` 是否成立。
5. 最后生成真正发送给数据库执行的 SQL。

### 7.4 include 的 refid

`refid` 用来指定引用哪个 SQL 片段。

同一个 Mapper XML 文件中引用：

```xml
<include refid="employeeColumns" />
```

引用其他 Mapper XML 中的 SQL 片段时，需要写完整命名空间：

```xml
<include refid="com.example.mybatis.mapper.EmployeeMapper.employeeColumns" />
```

基础阶段建议先在同一个 Mapper XML 中定义和引用，等项目结构稳定后再考虑跨 Mapper 复用。

### 7.5 常用场景

| 场景 | 写法 | 说明 |
| --- | --- | --- |
| 多个查询使用相同字段 | 把字段列表放入 `<sql>` | 避免重复维护字段 |
| 多个查询使用相同条件 | 把 `<where>` 条件放入 `<sql>` | 保持查询条件一致 |
| 多个查询使用相同 JOIN | 把 JOIN 片段放入 `<sql>` | 关联查询中常见 |
| 多个分页查询共用基础 SQL | 把基础查询放入 `<sql>` | 分页、统计可以复用主体 SQL |

### 7.6 使用注意点

- `<sql>` 只是 XML 片段，不是可以直接执行的 SQL。
- `<sql>` 必须通过 `<include>` 引用后才会参与 SQL 生成。
- `id` 命名要表达业务含义，例如 `employeeColumns`、`employeeSearchCondition`。
- 不要把过长、过复杂的 SQL 都塞进一个 `<sql>`，否则阅读时反而更难理解。
- 字段片段中要注意逗号位置，避免引用后生成错误 SQL。
- 动态条件片段建议配合 `<where>` 使用，避免手动处理多余的 `AND`。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| WHERE 后多出 AND | 手动拼接条件 | 使用 `<where>` |
| UPDATE 后多出逗号 | 手动拼接 SET | 使用 `<set>` |
| foreach 集合为空 | SQL 变成 `IN ()` | Java 侧先判断空集合 |
| test 属性写错 | OGNL 表达式不正确 | 检查属性名和空值判断 |
| include 找不到 SQL 片段 | `refid` 写错或片段不在当前命名空间 | 检查 `<sql id>` 和 `<include refid>` |
| 引用字段片段后 SQL 报错 | 字段逗号位置不正确 | 检查 `<sql>` 片段拼接后的完整 SQL |

## 九、本章练习

请完成：

1. 使用 `<if>` 根据姓名查询员工。
2. 使用 `<where>` 根据姓名和部门查询员工。
3. 使用 `<set>` 动态修改员工。
4. 使用 `<foreach>` 查询多个 ID。
5. 使用 `<choose>` 完成优先条件查询。
6. 使用 `<sql>` 定义员工查询字段，并在两个 `<select>` 中通过 `<include>` 复用。

## 十、本章总结

- 动态 SQL 用于根据条件生成 SQL。
- `<where>` 处理动态查询条件。
- `<set>` 处理动态更新字段。
- `<foreach>` 处理集合参数。
- `<sql>` 用于定义可复用 SQL 片段。
- `<include>` 用于引用 `<sql>` 片段。
