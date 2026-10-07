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

动态 SQL 的重点不是“让数据库自己选择条件”，而是 MyBatis 在执行 SQL 之前，根据 Java 参数生成一条最终 SQL。数据库只会收到生成后的 SQL 和参数。

本章继续使用第8章的参数映射知识。示例中的 `Employee` 是查询条件对象，`name`、`departmentId`、`email` 等属性由 MyBatis 读取。

## 二、if

`<if>` 用于条件判断。

Mapper 接口：

```java
List<Employee> selectByCondition(Employee condition);
```

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

`test` 中使用的是 OGNL 表达式。基础阶段需要掌握以下写法：

| 写法 | 含义 |
| --- | --- |
| `name != null` | 姓名不是 `null` |
| `name != ''` | 姓名不是空字符串 |
| `name != null and name != ''` | 姓名既不是 `null`，也不是空字符串 |
| `ids != null and !ids.isEmpty()` | 集合存在并且至少包含一个元素 |

OGNL 中的属性名必须和 Java 对象属性一致。`department_id` 是数据库列名，不能代替 Java 属性名 `departmentId`。

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

不同输入会生成不同 SQL：

| `name` | `departmentId` | 最终条件 |
| --- | --- | --- |
| `null` | `null` | 没有 `WHERE`，查询全部员工 |
| `"Tanaka"` | `null` | `WHERE name LIKE ?` |
| `null` | `20L` | `WHERE department_id = ?` |
| `"Tanaka"` | `20L` | 两个条件都出现，并用 `AND` 连接 |

“两个条件都为空时查询全部数据”是否允许，应由查询规格决定。如果接口不允许无条件查询，就必须在调用 Mapper 前校验，或者在 SQL 中提供明确的限制条件。

## 四、set

`<set>` 用于动态更新。

Mapper 接口：

```java
int updateSelective(Employee employee);
```

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

但是，如果 `name`、`departmentId`、`email` 全部为 `null`，`<set>` 中不会生成任何赋值语句，最终 SQL 将无效。调用 Mapper 前应确认至少有一个允许修改的字段：

```java
boolean hasUpdateValue = employee.getName() != null
        || employee.getDepartmentId() != null
        || employee.getEmail() != null;

if (!hasUpdateValue) {
    throw new IllegalArgumentException("至少指定一个需要修改的字段");
}

int updatedRows = employeeMapper.updateSelective(employee);
```

还要注意：上面的示例把 `email == null` 理解为“不修改邮箱”，因此不能用它表达“把邮箱更新成 `NULL`”。如果业务同时需要“不修改”和“更新为空”，更新参数对象必须增加能够区分这两种状态的设计，不能只依靠一个 `null` 值猜测意图。

## 五、foreach

`<foreach>` 常用于 `IN` 查询。

Mapper 接口：

```java
List<Employee> selectByIds(@Param("ids") List<Long> ids);
```

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

调用 `selectByIds(List.of(1L, 2L, 3L))` 时，循环部分会生成：

```sql
WHERE id IN (?, ?, ?)
```

如果集合为空，循环中不会生成元素，可能得到无效的 `IN` 条件。不要把“空 ID 列表”自动解释为“查询全部员工”；应在调用 Mapper 前返回空列表或按照接口规格报告参数错误。

## 六、choose

`<choose>` 类似 Java 中的 `if else if else`。

Mapper 接口：

```java
List<Employee> selectByKeyword(Employee condition);
```

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

多个独立 `<if>` 与 `<choose>` 的区别是：

- 多个 `<if>` 可以同时成立，适合组合多个查询条件。
- `<choose>` 最多选择一个分支，适合存在明确优先级或互斥规则的场景。

在当前示例中，即使姓名和邮箱都有值，也只使用姓名条件。这个优先级必须符合接口规格，不能只是为了使用 `<choose>` 而随意决定。

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
| 所有更新字段都为空 | `<set>` 没有生成赋值内容 | 调用 Mapper 前拒绝无修改字段的请求 |
| 无条件查询返回大量数据 | 所有 `<if>` 都不成立 | 根据规格拒绝空条件或增加明确限制 |
| 把数据库列名写进 `test` | OGNL 读取的是 Java 属性 | 使用 `departmentId` 等 Java 属性名 |

## 九、如何确认动态 SQL 是否正确

不要只阅读 XML 猜测结果。应准备不同输入并观察 SQL 日志：

1. 所有查询条件为空。
2. 只有姓名有值。
3. 只有部门 ID 有值。
4. 姓名和部门 ID 都有值。
5. 更新一个字段。
6. 所有更新字段为空。
7. ID 集合有多个元素。
8. ID 集合为空。

检查日志中的 `Preparing` 和 `Parameters`：

```text
Preparing: SELECT id, name, department_id AS departmentId, email FROM employees WHERE department_id = ?
Parameters: 20(Long)
```

`Preparing` 用于确认 SQL 结构，`Parameters` 用于确认参数顺序和值。两者都正确，才能说明动态 SQL 生成符合预期。

## 十、本章练习

请完成：

1. 使用 `<if>` 和 `<where>` 完成姓名、部门组合查询，至少验证四组不同输入及最终 SQL。
2. 使用 `<set>` 动态修改员工，并在 Java 侧拒绝“所有修改字段都为空”的输入。
3. 使用 `<foreach>` 查询多个 ID，并按照规格处理空集合。
4. 使用 `<choose>` 实现“优先按姓名，否则按邮箱”的查询，并验证两个值同时存在时只生成姓名条件。
5. 使用 `<sql>` 定义员工查询字段，并在两个 `<select>` 中通过 `<include>` 复用。
6. 故意把 `test` 中的 `departmentId` 写成 `department_id`，根据错误或异常结果定位并修正。
7. 提交 SQL 日志作为自测证据，标出最终 SQL、参数值和实际返回条数。

## 十一、本章总结

- 动态 SQL 用于根据条件生成 SQL。
- `<where>` 处理动态查询条件。
- `<set>` 处理动态更新字段。
- `<foreach>` 处理集合参数。
- `<sql>` 用于定义可复用 SQL 片段。
- `<include>` 用于引用 `<sql>` 片段。
- `test` 读取的是 Java 参数对象的属性，而不是数据库列名。
- 动态更新字段全空、查询条件全空和集合为空时，都必须按照接口规格处理。
- SQL 日志可以用来确认最终 SQL 结构和参数是否正确。
