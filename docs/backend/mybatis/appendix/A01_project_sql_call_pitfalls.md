# 附录A01 项目常见SQL调用陷阱与Review

> 本附录目标：进入实际项目前，能够从Java调用、Mapper接口、Mapper XML和SQL日志中识别常见的数据访问问题，并用SQL次数、影响行数和数据库结果证明修改有效。

本附录是完成MyBatis第12章后的知识扩展，不改变前面员工管理练习的稳定状态。示例继续使用 `departments` 和 `employees`，重点不是背诵更多标签，而是建立下面的检查习惯：

```text
业务要取得什么
  → Java调用了几次Mapper
  → 每个Mapper生成什么SQL
  → SQL一共执行几次
  → 数据库读取或修改多少行
  → Java最终返回多少对象
```

危险的UPDATE、DELETE和批量写入只能在个人练习数据库中验证，并应放在可以回滚的事务中。不要在共享数据库或包含重要数据的环境中制造故障。

## 使用前准备

开始前应完成MyBatis第12章，并保留其中的 `departments`、`employees`、Entity、Mapper接口和Mapper XML。第12章的2个部门和3名员工已经足够观察N+1：主查询1次、子查询2次，总计3次SQL。

文中的Java和XML均标明为代码片段，需要加入第12章对应类或Mapper，不能把所有同名实验方法一次复制到正式代码中。每完成一节应恢复临时方法和测试数据；综合练习应使用个人练习数据库，并先记录当前数据数量。

部分日志示意使用3个部门，50个部门只用于计算SQL次数增长，不要求为了练习制造50条正式数据。

## 一、先建立SQL调用的观察方法

### 1. Mapper调用次数不等于接口数量

一个接口请求可能只调用一次Service，但Service内部可能循环调用Mapper。一次HTTP请求、一次Java方法调用和一次SQL执行不是同一个计数单位。

例如：

```java
// Service方法中的代码片段
List<Department> departments = departmentMapper.selectAll();

for (Department department : departments) {
    List<Employee> employees =
            employeeMapper.selectByDepartmentId(department.getId());
    department.setEmployees(employees);
}
```

假设有3个部门：

```text
selectAll()                       → 1次SQL
selectByDepartmentId(10)         → 1次SQL
selectByDepartmentId(20)         → 1次SQL
selectByDepartmentId(30)         → 1次SQL
合计                              → 4次SQL
```

如果部门增加到50个，相同代码会执行51次SQL。测试数据只有两三条时，功能结果可能完全正确，因此只看页面或返回JSON无法发现问题。

### 2. 最少保存四类证据

调查SQL调用时至少记录：

| 证据 | 要确认的内容 |
| --- | --- |
| 调用位置 | 哪个Service方法、循环或对象映射触发Mapper |
| SQL日志 | 实际执行的SQL和参数，而不是只看XML源码 |
| 数量 | 一次业务操作执行几次SQL、每次返回或影响几行 |
| 结果 | Java对象、接口响应和数据库最终状态是否符合规格 |

MyBatis基础练习可以通过SQL日志中的 `Preparing` 和 `Parameters` 观察执行。实际项目可能使用SLF4J、日志代理或监控平台，但判断方法相同。

### 3. 本附录的Review顺序

```text
先确认结果正确
  → 再确认SQL次数
  → 再确认读取或修改范围
  → 再看执行计划和索引
  → 最后比较修改前后的证据
```

不能只凭“用了JOIN”“加了缓存”或“建了索引”判断性能已经改善。

## 二、N+1查询

### 1. N+1是怎样产生的

N+1表示：

```text
先执行1次主查询
  → 主查询得到N条记录
  → 每条记录再执行1次关联查询
  → 总计1 + N次SQL
```

MyBatis的嵌套查询可以写成：

```xml
<resultMap id="departmentWithEmployeesNestedMap"
           type="Department">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <collection property="employees"
                ofType="Employee"
                column="id"
                select="com.example.mybatis.mapper.EmployeeMapper.selectByDepartmentId"/>
</resultMap>

<select id="selectAllWithEmployees"
        resultMap="departmentWithEmployeesNestedMap">
    SELECT id, name
    FROM departments
    ORDER BY id
</select>
```

子查询：

```java
List<Employee> selectByDepartmentId(
        @Param("departmentId") Long departmentId);
```

```xml
<select id="selectByDepartmentId"
        parameterType="long"
        resultType="Employee">
    SELECT id, name, department_id, email
    FROM employees
    WHERE department_id = #{departmentId}
    ORDER BY id
</select>
```

`<collection>` 中的 `column="id"` 把当前部门编号传给 `selectByDepartmentId`。写法容易理解，但读取部门列表并访问每个员工集合时可能形成N+1。

延迟加载只能推迟子查询。如果程序随后遍历全部部门并访问 `employees`，查询仍会被逐个触发，因此不能把延迟加载当作N+1的根本修复。

### 2. 方案一：一次JOIN和嵌套结果映射

```xml
<resultMap id="departmentWithEmployeesJoinMap"
           type="Department">
    <id property="id" column="department_id"/>
    <result property="name" column="department_name"/>
    <collection property="employees" ofType="Employee">
        <id property="id" column="employee_id"/>
        <result property="name" column="employee_name"/>
        <result property="departmentId" column="department_id"/>
        <result property="email" column="employee_email"/>
    </collection>
</resultMap>

<select id="selectAllWithEmployeesByJoin"
        resultMap="departmentWithEmployeesJoinMap">
    SELECT
        d.id AS department_id,
        d.name AS department_name,
        e.id AS employee_id,
        e.name AS employee_name,
        e.email AS employee_email
    FROM departments d
    LEFT JOIN employees e
        ON e.department_id = d.id
    ORDER BY d.id, e.id
</select>
```

这会把SQL次数降为1次。主对象的 `<id>` 非常重要，MyBatis用它识别重复的部门行并把不同员工加入同一个集合。

JOIN也有代价：一个部门有多名员工时，部门列会在结果集中重复。关联层数多或子记录很多时，结果集可能快速膨胀。

### 3. 方案二：主表查询加一次批量子查询

先查询部门，再一次查询这些部门的全部员工：

```java
List<Department> departments = departmentMapper.selectAll();

List<Long> departmentIds = departments.stream()
        .map(Department::getId)
        .toList();

List<Employee> employees =
        departmentIds.isEmpty()
                ? List.of()
                : employeeMapper.selectByDepartmentIds(departmentIds);
```

Mapper接口片段：

```java
List<Employee> selectByDepartmentIds(
        @Param("departmentIds") List<Long> departmentIds);
```

Mapper XML片段：

```xml
<select id="selectByDepartmentIds"
        resultType="Employee">
    SELECT id, name, department_id, email
    FROM employees
    WHERE department_id IN
    <foreach collection="departmentIds"
             item="departmentId"
             open="("
             separator=","
             close=")">
        #{departmentId}
    </foreach>
    ORDER BY department_id, id
</select>
```

随后在Java中按 `departmentId` 分组，再放回对应的Department。这样无论有多少部门，通常都是2次SQL。

### 4. 三种方案怎样选择

| 方案 | 典型SQL次数 | 适合情况 | 主要风险 |
| --- | ---: | --- | --- |
| 嵌套查询 | `1 + N` | 单条详情、关联对象不一定会访问 | 列表访问全部子对象时形成N+1 |
| JOIN | 1 | 关联规模明确、需要一次取得完整结果 | 重复行、结果集膨胀、分页困难 |
| 两次批量查询 | 2 | 主对象需要分页、子对象可以按外键批量取得 | Java需要完成分组和组装 |

优化目标不是永远追求“一次SQL”。应同时考虑SQL次数、返回行数、对象组装、分页和代码复杂度。

## 三、循环查询一组已知编号

下面不是对象映射产生的N+1，但根因相同：Java循环制造了大量数据库往返。

```java
// 问题代码片段
List<Employee> employees = new ArrayList<>();
for (Long id : employeeIds) {
    employees.add(employeeMapper.selectById(id));
}
```

已有全部编号时，优先使用集合参数：

```java
List<Employee> employees = employeeIds.isEmpty()
        ? List.of()
        : employeeMapper.selectByIds(employeeIds);
```

```java
List<Employee> selectByIds(@Param("ids") List<Long> ids);
```

```xml
<select id="selectByIds" resultType="Employee">
    SELECT id, name, department_id, email
    FROM employees
    WHERE id IN
    <foreach collection="ids"
             item="id"
             open="("
             separator=","
             close=")">
        #{id}
    </foreach>
    ORDER BY id
</select>
```

必须处理两个边界：

- 空集合先在Java中返回空列表，避免生成 `IN ()` 或没有条件的SQL；
- 集合非常大时不要无限生成占位符，应根据数据库限制、数据量和业务时限分批处理。

分批大小没有适用于所有项目的固定数字。需要使用目标数据库、JDBC驱动和真实数据验证。

## 四、循环执行INSERT或UPDATE

### 1. 事务不能自动减少SQL次数

```java
// Mapper调用片段
for (Employee employee : employees) {
    employeeMapper.insert(employee);
}
```

即使外层有事务，这段代码仍可能执行多次INSERT。事务解决“全部成功或全部回滚”，不是自动批量化工具。

### 2. 共通重点：使用ExecutorType.BATCH

跨数据库项目中，更共通的做法是使用JDBC Batch。MyBatis通过 `ExecutorType.BATCH` 提供这一执行方式。

它不会把Java循环自动改成一条多值SQL。业务代码仍然逐个调用同一个Mapper方法，但MyBatis会把同类写语句及其参数暂存起来，再通过JDBC批量执行：

```text
循环调用mapper.insert(employee)
            ↓
MyBatis暂存同类PreparedStatement及参数
            ↓
flushStatements()触发JDBC批量执行
            ↓
检查BatchResult
            ↓
commit()或rollback()
```

这种方式使用JDBC标准批处理接口，因此比某一种数据库的多值INSERT语法更容易迁移。但具体发送方式、性能、错误计数和生成主键能力仍受JDBC驱动与数据库影响，必须在目标环境验证。

### 3. 继续复用单条INSERT

不需要为批处理重新编写一条集合参数SQL。继续使用第7章的单条Mapper方法：

```java
int insert(Employee employee);
```

以及原有XML：

```xml
<insert id="insert"
        parameterType="Employee"
        useGeneratedKeys="true"
        keyProperty="id"
        keyColumn="id">
    INSERT INTO employees (name, department_id, email)
    VALUES (#{name}, #{departmentId}, #{email})
</insert>
```

普通执行器中，循环调用这个方法会逐次执行。批量执行器中，MyBatis会暂存这些调用，直到需要flush。

### 4. 完整批量调用片段

下面是普通MyBatis环境中的调用片段。`employees` 是已经完成必填、长度、邮箱格式和部门规则检查的员工集合：

```java
import java.sql.Statement;
import java.util.List;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;

if (employees == null || employees.isEmpty()) {
    throw new IllegalArgumentException("批量新增员工不能为空");
}

try (SqlSession sqlSession = sqlSessionFactory.openSession(
        ExecutorType.BATCH, false)) {
    try {
        EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);

        for (Employee employee : employees) {
            mapper.insert(employee);
        }

        List<BatchResult> batchResults = sqlSession.flushStatements();

        for (BatchResult batchResult : batchResults) {
            for (int updateCount : batchResult.getUpdateCounts()) {
                if (updateCount == Statement.EXECUTE_FAILED) {
                    throw new IllegalStateException("批量新增中存在执行失败的数据");
                }
            }
        }

        sqlSession.commit();
    } catch (RuntimeException exception) {
        sqlSession.rollback();
        throw exception;
    }
}
```

这段代码必须作为一个整体理解，不能只复制循环部分。

#### 4.1 `openSession(ExecutorType.BATCH, false)`

第一个参数选择批量执行器。第二个参数 `false` 表示不自动提交，由代码在全部批次成功后统一调用 `commit()`。

如果使用自动提交，或者每插入一条就提交一次，就失去了“整批共同成功或共同回滚”的事务边界。

#### 4.2 循环中的返回值不是最终影响行数

在Batch执行器中，下面的调用主要表示“把本次语句和参数加入批次”：

```java
mapper.insert(employee);
```

它的即时 `int` 返回值不能按普通执行器的方式理解为已经插入的行数。不要在循环中用 `mapper.insert(employee) == 1` 判断批量写入成功。

真正执行批次并取得结果的是：

```java
List<BatchResult> batchResults = sqlSession.flushStatements();
```

#### 4.3 怎样阅读BatchResult

一个 `BatchResult` 对应一组被批量执行的映射语句和SQL。`getUpdateCounts()` 返回JDBC驱动报告的每次更新结果。

常见值包括：

| 返回值 | 含义 | 处理方式 |
| --- | --- | --- |
| `0`或正整数 | 驱动报告的影响行数 | 与接口规格和输入数据核对 |
| `Statement.SUCCESS_NO_INFO` | 执行成功，但驱动不提供具体行数 | 不能用行数合计证明全部写入，需要查询验证 |
| `Statement.EXECUTE_FAILED` | 该批次元素执行失败 | 回滚事务并调查失败数据 |

不同驱动可能返回不同形式的计数。即使没有 `EXECUTE_FAILED`，仍要在测试中查询数据库，确认实际插入行数和字段值。

#### 4.4 flush、commit和rollback各自负责什么

| 操作 | 作用 |
| --- | --- |
| `flushStatements()` | 把暂存的批次交给JDBC执行，并取得批处理结果 |
| `commit()` | 提交当前事务，使成功写入正式生效 |
| `rollback()` | 回滚当前事务，撤销本次事务中的写入 |

`commit()` 会处理尚未flush的语句，但示例显式调用 `flushStatements()`，是为了在提交前取得 `BatchResult` 并检查失败。

错误可能在循环中、`flushStatements()` 或 `commit()` 时出现。异常处理必须覆盖整个批量过程。

### 5. 批量执行时最容易忽略的边界

#### 5.1 空集合和输入校验

空集合是“不执行并返回成功”，还是“请求参数错误”，应由接口规格确定。本例选择报告参数错误。

批量执行不会代替字段校验和业务校验。姓名为空、邮箱格式错误或部门不存在等问题，应尽可能在进入数据库批次前发现；数据库唯一约束和外键约束仍作为最后防线。

#### 5.2 中间一条失败

准备三条数据，让第二条违反邮箱唯一约束。至少确认：

1. 异常在哪个阶段出现；
2. 是否执行了 `rollback()`；
3. 第一条和第三条是否也没有保留；
4. 日志能否定位输入批次和失败范围，同时不泄露敏感数据。

是否允许“成功一部分、失败一部分”属于另一种业务规格，不能通过捕获异常后继续提交来临时实现。

#### 5.3 批次不能无限增大

大量对象一直暂存在内存中，可能增加应用和驱动的内存压力。实际项目通常按固定上限切分输入，每批调用一次 `flushStatements()`，但批量大小没有适用于所有项目的统一数字。

需要用目标数据库、JDBC驱动和接近实际的数据量测试：

- 每批执行时间；
- 内存变化；
- 网络往返和数据库负载；
- 失败时需要回滚和重新处理的数据范围。

即使分成多个flush，只要还没有提交，它们仍可以属于同一个事务。是否整批统一提交，取决于业务是否要求全部成功或全部失败。

#### 5.4 不要在批量循环中穿插查询

批量写入过程中执行查询，可能使MyBatis为了保证行为可理解而提前flush已暂存的写语句。批量阶段应尽量先完成校验，再集中写入，不要形成“写一条、查一条”的新问题。

#### 5.5 自动生成主键必须实测

`useGeneratedKeys="true"` 不代表所有数据库和驱动都能以相同方式返回批量生成的主键。采用前验证：

- 每个 `Employee.id` 是否被正确回填；
- 回填顺序是否与输入一致；
- flush或提交失败后，对象中的ID是否还能代表已提交数据；
- 后续业务是否真的必须立即使用这些ID。

主键回填不稳定时，应根据规格选择单条插入、预先生成业务编号或写入后按唯一键查询，不能根据连续自增规律自行计算ID。

### 6. Spring项目中的使用边界

接入Spring后，`SqlSession` 和事务通常由MyBatis-Spring及Spring事务管理器负责。不要把上面的 `openSession()`、`commit()` 和 `rollback()` 原样复制进使用 `@Transactional` 的Service。

Spring项目需要按照项目配置创建使用 `ExecutorType.BATCH` 的批量会话或 `SqlSessionTemplate`，并保持执行器类型和事务边界一致。不要在同一事务中临时混用普通执行器和批量执行器。具体配置应结合项目现有数据源、事务管理器和MyBatis-Spring版本验证。

### 7. MySQL多值INSERT只作为补充方案

MySQL支持一条SQL包含多组VALUES：

```sql
INSERT INTO employees (name, department_id, email)
VALUES
    (?, ?, ?),
    (?, ?, ?);
```

MyBatis可以用 `<foreach>` 生成这种SQL，但它属于数据库SQL能力，不作为本节的共通主线。其他数据库可能使用不同语法，SQL长度、参数上限和批量生成主键行为也不同。

只有项目已经确定数据库产品，并经过性能、失败回滚和主键回填验证时，再考虑把多值INSERT作为方言优化。

### 8. 本节验证要求

项目采用批量执行时至少验证：

1. 0条、1条和多条输入；
2. 中间一条违反唯一约束时是否整体回滚；
3. `flushStatements()` 返回的结果怎样解释；
4. 生成主键是否符合项目预期；
5. 批量大小增加时的内存和执行时间；
6. 批量循环中没有穿插不必要的查询；
7. 失败日志能否定位到具体批次和数据范围。

## 五、查询范围没有上限

### 1. SELECT星号和无条件列表

```sql
SELECT *
FROM employees
ORDER BY id;
```

数据较少时能够正常工作，但项目运行后可能同时增加：

- 数据库读取的列数和行数；
- 数据库到应用的网络传输；
- Java对象数量和内存；
- JSON序列化时间和响应体大小。

列表查询应明确所需列，并按照接口规格分页或限制数量：

```sql
SELECT id, name, department_id
FROM employees
WHERE department_id = #{departmentId}
ORDER BY id
LIMIT #{limit} OFFSET #{offset};
```

`fetchSize` 只是给JDBC驱动的抓取提示，不是SQL返回行数限制；`timeout` 限制等待时间，也不会自动减少结果数量。

### 2. 不要只在Java中截断

下面的做法仍然会把全部数据读入应用：

```java
List<Employee> all = employeeMapper.selectAll();
List<Employee> firstTwenty = all.stream()
        .limit(20)
        .toList();
```

数量限制应尽可能进入SQL，使数据库只返回需要的数据。

## 六、COUNT与列表查询条件不一致

分页通常需要：

```text
COUNT查询  → total
列表查询   → items
```

如果新增了部门条件但只修改列表SQL，就可能出现 `total` 与 `items` 不对应。

可以把共同条件集中为SQL片段：

```xml
<sql id="employeeSearchWhere">
    <where>
        <if test="departmentId != null">
            department_id = #{departmentId}
        </if>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
    </where>
</sql>

<select id="countByCondition" resultType="long">
    SELECT COUNT(*)
    FROM employees
    <include refid="employeeSearchWhere"/>
</select>

<select id="selectPage" resultType="Employee">
    SELECT id, name, department_id, email
    FROM employees
    <include refid="employeeSearchWhere"/>
    ORDER BY id
    LIMIT #{limit} OFFSET #{offset}
</select>
```

共享片段能降低遗漏概率，但仍要分别验证两条最终SQL。数据库数据可能在两次查询之间变化；是否需要更强的一致性，应根据接口规格和事务要求判断。

## 七、一对多JOIN与分页组合

假设一个部门有5名员工，JOIN会返回5行：

```text
数据库JOIN结果：5行
MyBatis组装结果：1个Department，包含5个Employee
```

如果直接在JOIN后使用 `LIMIT 3`，数据库先截取3个结果行，MyBatis只能看到3行，最终可能得到一个员工集合不完整的部门。

因此不能直接认为：

```text
LIMIT 20 = 返回20个部门
```

常见做法是：

1. 先按稳定顺序分页查询20个部门或部门ID；
2. 再用一次 `IN` 查询这些部门的员工；
3. 在Java中按部门编号组装。

另一种做法是列表接口只返回部门概要，进入详情接口后再查询员工。选择取决于画面和接口是否真的需要一次返回完整子集合。

## 八、动态SQL条件消失造成全表修改

下面是危险示例，不要直接在共享数据库执行：

```xml
<update id="updateDepartmentByCondition">
    UPDATE employees
    SET department_id = #{newDepartmentId}
    <where>
        <if test="id != null">
            id = #{id}
        </if>
    </where>
</update>
```

当 `id` 为 `null` 时，`<where>` 不会生成WHERE，最终可能成为：

```sql
UPDATE employees
SET department_id = ?;
```

对于按员工编号修改的业务，应让条件成为必需结构：

```xml
<update id="updateDepartmentById">
    UPDATE employees
    SET department_id = #{newDepartmentId}
    WHERE id = #{id}
</update>
```

同时在Java入口检查 `id`，并检查影响行数：

```java
int updated = employeeMapper.updateDepartmentById(
        id,
        newDepartmentId);
if (updated != 1) {
    throw new IllegalStateException(
            "员工部门更新件数异常：" + updated);
}
```

Mapper接口使用两个参数时明确命名：

```java
int updateDepartmentById(
        @Param("id") Long id,
        @Param("newDepartmentId") Long newDepartmentId);
```

批量修改确实允许多个结果时，也必须在规格中明确条件和预期范围。不能因为方法名包含“批量”就允许空条件。

安全验证至少覆盖：

- 正常主键；
- 不存在的主键；
- `null`；
- 空字符串或空集合；
- 实际影响行数；
- 回滚后的数据库状态。

## 九、使用文本替换拼接排序

危险写法：

```xml
ORDER BY ${sortBy} ${sortDirection}
```

这里使用的是MyBatis的美元花括号文本替换。它会把文本直接放入SQL，不能像井号参数绑定一样隔离值。即使前端使用下拉框，调用者仍可以绕过页面直接请求后端。

排序字段和方向应先通过接口白名单校验，再由XML生成固定SQL：

```xml
ORDER BY
<choose>
    <when test="sortBy == 'name'">name</when>
    <when test="sortBy == 'departmentId'">department_id</when>
    <otherwise>id</otherwise>
</choose>
<choose>
    <when test="sortDirection == 'asc'">ASC</when>
    <otherwise>DESC</otherwise>
</choose>
```

值条件继续使用参数绑定：

```xml
WHERE department_id = #{departmentId}
```

表名和列名无法作为普通值参数绑定时，也不能直接接受用户任意文本。应在后端把有限业务选项映射为固定SQL片段。

## 十、缓存不能修复调用结构

缓存可能减少一部分重复访问，但不能证明N+1已经消失：

- N个子查询使用不同部门编号时，缓存键也不同；
- 一级缓存只在同一个 `SqlSession` 范围内；
- 二级缓存还要考虑Mapper命名空间、写入失效和多实例一致性；
- 缓存命中可能让小规模测试暂时变快，却保留错误的调用结构。

Review顺序应是：

```text
先确认是否存在不必要的Mapper调用
  → 调整JOIN、批量查询或接口数据范围
  → 再根据读取频率和一致性要求评估缓存
```

不要以“开启二级缓存”作为N+1指摘的唯一修复。

## 十一、事务、超时和重试的常见误解

| 误解 | 正确认识 |
| --- | --- |
| 放进事务后SQL会自动变少 | 事务控制提交和回滚，不自动合并Mapper调用 |
| 增加超时时间就解决了慢查询 | 超时只是等待边界，应继续调查SQL次数、数据量和执行计划 |
| 查询失败可以无限重试 | 重试必须针对可恢复故障、限制次数，并考虑数据库压力 |
| 写入超时表示肯定没有写入 | 客户端超时时，数据库端结果可能仍需确认 |
| `fetchSize=100` 表示最多返回100行 | fetchSize通常只是驱动抓取提示 |

普通MyBatis项目由 `SqlSession` 的 `commit()`、`rollback()` 和关闭范围管理事务。接入Spring后，事务边界由Spring事务管理器接管；不要同时在业务代码中随意手动提交。

## 十二、怎样完成一次SQL调用问题调查

### 1. 调查流程

```text
确定一个具体业务操作
  → 准备能放大问题的数据量
  → 清空或标记本次日志范围
  → 执行一次操作
  → 统计SQL类型和次数
  → 对照Java循环、Mapper和XML
  → 提出候选修正
  → 比较SQL次数、结果和数据库状态
```

### 2. 什么时候使用EXPLAIN

SQL次数合理后，如果单条查询仍然慢，再使用MySQL `EXPLAIN` 观察访问方式、预计行数、索引候选和附加信息。索引是否有效取决于查询条件、数据分布和排序，不能因为看到全表扫描就机械增加索引。

`EXPLAIN ANALYZE` 会实际执行语句。只应对安全的查询并在受控环境中使用，不能对未知成本的写操作随意执行。

MyBatis的嵌套查询、嵌套结果映射和N+1说明可参考[MyBatis Mapper XML官方文档](https://mybatis.org/mybatis-3/sqlmap-xml.html)，动态SQL标签参考[MyBatis Dynamic SQL官方文档](https://mybatis.org/mybatis-3/dynamic-sql.html)，批量执行器与flush方法参考[MyBatis Java API](https://mybatis.org/mybatis-3/java-api.html)，JDBC批处理返回值参考[Java 17 Statement API](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/Statement.html)。MySQL多值语法只作为方言补充，可核对[MySQL 8.0 INSERT说明](https://dev.mysql.com/doc/refman/8.0/en/insert.html)。执行计划原理继续学习SQL课程，并可核对[MySQL 8.0 EXPLAIN说明](https://dev.mysql.com/doc/refman/8.0/en/explain.html)。

## 十三、综合Review练习

### 1. 初始问题

假设收到“部门员工一览响应较慢”的调查票。现有实现同时存在：

1. 查询部门后循环查询员工；
2. 收到一组员工编号后循环执行 `selectById`；
3. 列表查询没有分页；
4. COUNT遗漏部门条件；
5. 排序使用外部输入的文本替换；
6. 所属部门更新条件可以全部为空；
7. 开发者准备开启二级缓存解决全部问题。

### 2. 任务要求

完成以下工作：

1. 标出每个问题所在的Java、Mapper接口或XML位置；
2. 计算部门为50个时，当前部门员工查询至少执行多少次SQL；
3. 分别提出JOIN和“两次查询后组装”方案；
4. 选择一种方案实现，并说明选择理由；
5. 把编号循环查询改成集合参数查询；
6. 为员工列表规定明确分页和稳定排序；
7. 统一COUNT与列表查询条件；
8. 用白名单替换动态排序文本；
9. 阻止空条件UPDATE；
10. 说明为什么缓存不是这些问题的统一答案。

### 3. 验收证据

至少提交：

| 证据 | 最低要求 |
| --- | --- |
| 数据条件 | 部门数、员工数和关键分布 |
| 修改前日志 | 一次操作的SQL类型、参数和总次数 |
| 修改后日志 | 相同操作下的SQL次数 |
| 功能结果 | 部门、员工集合、分页total和items |
| 写入安全 | 空条件被拒绝、正常更新影响1行 |
| 数据库确认 | 修改前后查询结果及回滚结果 |
| Review结论 | 问题、影响、原因、修改和回归范围 |

不要只提交耗时数字。开发机器的一次耗时容易受缓存、后台进程和数据量影响；SQL次数和结果范围是更稳定的第一阶段证据。

## 十四、项目Review检查表

| 检查方向 | 确认问题 |
| --- | --- |
| Mapper调用 | 是否在循环中调用查询或写入Mapper？ |
| SQL次数 | 数据量为N时，SQL次数怎样增长？ |
| 查询列 | 是否使用无目的的 `SELECT *`？ |
| 查询行 | 是否有分页、上限或明确的单条条件？ |
| 集合参数 | 空集合和过大集合怎样处理？ |
| 关联查询 | JOIN、嵌套查询和批量查询的选择依据是什么？ |
| 一对多分页 | LIMIT限制的是主对象还是JOIN结果行？ |
| COUNT | 是否与列表使用相同筛选条件？ |
| 参数安全 | 外部输入是否进入文本替换？ |
| 写入条件 | 条件缺失时会不会全表UPDATE或DELETE？ |
| 影响行数 | INSERT、UPDATE、DELETE结果是否符合规格？ |
| 批量插入 | 是否处理空集合、单批上限、事务回滚、影响行数和主键回填？ |
| 事务 | 多步写入失败时能否整体回滚？ |
| 缓存 | 是否用缓存掩盖重复调用或引入旧数据风险？ |
| 超时与重试 | 是否有明确边界，写操作是否考虑重复执行？ |
| 证据 | 是否保存日志、SQL次数、参数、结果和恢复状态？ |

## 十五、本附录完成标准

完成本附录后，你应能够：

1. 根据Java循环和SQL日志识别N+1与重复Mapper调用；
2. 在JOIN、嵌套查询和两次批量查询之间说明取舍；
3. 区分多值INSERT、批量执行器、事务、缓存、超时和结果数量限制；
4. 识别一对多JOIN分页、COUNT条件漂移和空条件写入；
5. 使用参数绑定与白名单避免动态SQL注入；
6. 用SQL次数、影响行数和数据库结果完成一次可Review的改修说明。

这些检查用于阻止明显的数据访问问题进入项目。复杂索引、执行计划、数据库锁和生产容量评估仍需要结合SQL课程、真实数据库统计信息和项目运维标准继续调查。
