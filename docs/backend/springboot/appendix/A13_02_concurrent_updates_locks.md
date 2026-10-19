# A13-2 从旧页面覆盖到悲观锁、乐观锁与双请求验证

建议在[A13-1 事务原理](A13_01_transaction_spring_mechanisms.md)之后学习本篇，再学习[A13-3 隔离级别与MVCC](A13_03_isolation_mvcc.md)。先复现问题和两个数据库连接的等待，再讨论如何在Spring Boot项目中落地，并把记录锁、Gap Lock、死锁等机制放到最后解释。

**项目边界：** 主线没有`version`列及休假余额表；它们属于独立扩展实验。不要把片段直接粘贴到主线项目而假定可以编译，不能在共享/生产数据库操作。正式改修必须同步更新DTO、Mapper、Service、异常处理和测试。

## 一、先复现：两名管理员提交旧表单

**前置知识：** 先完成[A13-1 事务边界](A13_01_transaction_spring_mechanisms.md)。请先在**个人专用、可以重建的MySQL 8 InnoDB练习库**创建独立实验数据，不直接覆盖主线员工、不在共享库执行。若练习库复用了主线`employees`，必须按本章后面的恢复清单记录旧值、履历及表结构。

| 时间 | A | B | 观察重点 |
| --- | --- | --- | --- |
| T0 | 读id=1001 | 读id=1001 | 两个页面均拿到`Tanaka/old@example.com` |
| T1 | 姓名改为`Tanaka Updated`并提交 | 仍在旧页面 | A提交成功 |
| T2 | — | 只修改邮箱，连同旧姓名一起完整提交 | B提交成功，A姓名可能被覆盖 |

```sql
-- 无版本检查的完整更新，只在隔离实验数据执行
UPDATE employees SET name='Tanaka Updated', email='old@example.com' WHERE id=1001;
UPDATE employees SET name='Tanaka', email='new@example.com' WHERE id=1001;
SELECT id,name,email FROM employees WHERE id=1001;
```

教学结论：**请求没有并行执行也可能发生过期数据覆盖**。`@Transactional`保证每次更新及履历的原子提交，不知道B的请求内容基于旧页面。问学员：“给每个更新方法加事务就安全了吗？”答案是否定的。

## 二、先用两个连接看见悲观锁

不急着背锁类型。打开两个MySQL CMD窗口，各建立独立连接；确认数据库为InnoDB、实验记录存在、未启用自动结束的脚本工具。先在A运行：

```sql
START TRANSACTION;
SELECT id,name,email FROM employees WHERE id=1001 FOR UPDATE;
-- 不要COMMIT，保持A持锁
```

接着在B运行（B的命令会等待，不要误以为客户端坏了）：

```sql
START TRANSACTION;
UPDATE employees SET email='new@example.com' WHERE id=1001;
-- 等待A释放锁，之后再COMMIT或ROLLBACK
```

回到A：

```sql
UPDATE employees SET name='Tanaka Updated' WHERE id=1001;
COMMIT;
```

观察B的UPDATE恢复执行，然后B显式`COMMIT;`，最后独立查询姓名与邮箱。此实验只演示**同一主键记录上的冲突锁等待**，不是旧页面冲突的解决方案。锁由InnoDB事务获取，通常在`COMMIT/ROLLBACK`释放；普通非锁定SELECT通常仍能读MVCC快照。

**课堂重点：** 谁拿锁？MySQL事务。锁什么？本例通常为主键索引记录，而不是整个Java对象。何时释放？事务结束。为什么不能用Java的`synchronized`代替？它无法跨多个JVM实例协调数据库事务。

## 三、悲观锁保护的是哪段业务操作

把上一节的数据库实验对应到项目代码时，典型流程是：**Spring代理进入Service事务 → Mapper用主键`FOR UPDATE`锁定读取 → 根据最新状态校验 → UPDATE员工和INSERT履历 → 提交或回滚并释放锁**。`FOR UPDATE`与后续写入必须处于同一数据库事务；如果锁定读结束后就提交，后续更新已不再受这次锁保护。

这里先理解事务边界，不需要新增Java方法。第五节“方案4”再对照现有 `EmployeeServiceImpl.update()` 给出Mapper声明、XML和准确的替换位置；原有的部门、邮箱校验、重复键异常转换及履历写入都要保留。

**关键反例：** B在浏览器停留十分钟后提交旧姓名，虽然锁定读能取得最新数据库行，如果仍直接以旧请求体无条件更新整行，照样能覆盖A。锁保护的是当前事务的读-判定-写窗口，不会自动识别旧页面。这就引出`version`。

## 四、乐观锁：用version拒绝旧页面

```text
GET详情：A和B都得到 version=0
A提交 version=0 → UPDATE影响1行，数据库version变成1
B提交 version=0 → UPDATE影响0行，不修改任何员工字段
API：A成功，B返回409 Conflict，冲突不写“成功修改履历”
```

```sql
UPDATE employees
SET name = ?, department = ?, email = ?, version = version + 1
WHERE id = ? AND version = ?;
```

这里的`?`表示用于概念说明的JDBC占位符；在MyBatis XML中使用`#{...}`，并且version**必须是客户端详情读取时保存并原样提交的旧值**，不可在更新前从数据库重新取最新version替换，否则冲突检测失效。SQL自身会取得写入所需的数据库锁；“乐观”不是“不加任何数据库锁”。

这一步先观察“成功1行、冲突0行”的效果。第五节“方案3”再按数据库列、DTO、Mapper、Service和异常处理的顺序说明如何改修；只增加一个 `version` 列或只替换一条SQL，都不足以让现有接口完成乐观锁。

## 五、项目实现：约束、条件更新与两种锁

### 方案1：唯一约束——防止最终重复

第9章建立 `employees` 表时，`email` 已有 `uk_employees_email` 唯一约束。可以用数据库客户端检查现有表定义，不要对同一表再次执行 `ADD CONSTRAINT`：

```sql
SHOW CREATE TABLE employees;
```

适合“email不能重复”这类最终不变量。即使两个请求同时通过事前检查，数据库也只允许一个冲突值成功。Service需要把重复键异常转换为明确的业务冲突响应。

两个请求A、B都先查询“邮箱不存在”，随后同时INSERT相同邮箱：应用层的两次检查都可能通过，`UNIQUE`约束最终只允许一个提交。对当前员工项目，应保留第10章Service的邮箱规范化和重复冲突处理：MyBatis/Spring数据访问层抛出的重复键异常由Service转换为业务冲突，统一异常处理按既有API规格返回409；不要把数据库表名、SQL或堆栈返回客户端。正常系验证一条新邮箱可创建；竞争系验证只有一行、另一请求为约定冲突且数据库没有重复；若看到500，先检查异常转换路径，不能删掉唯一约束。

唯一约束不负责合并两次更新，也不替代多SQL事务。

### 方案2：原子条件UPDATE——在一条SQL中判断并修改

休假余额扣减可以避免“先查再改”的并发空隙。下例使用**主线不存在**的 `leave_balances(employee_id, remaining_days)` 表，以及相应Mapper和冲突异常，只用于说明条件UPDATE；若要实际运行，需先设计表、Mapper、异常到HTTP的映射和并发测试：

```sql
UPDATE leave_balances
SET remaining_days = remaining_days - #{requestDays}
WHERE employee_id = #{employeeId}
  AND #{requestDays} > 0
  AND remaining_days >= #{requestDays};
```

Mapper返回影响行数：

```java
int updatedRows = leaveBalanceMapper.deduct(
        employeeId,
        requestDays);
if (updatedRows == 0) {
    throw new DataConflictException(
            "剩余天数不足或数据已变化");
}
```

`requestDays`必须是合法正数：请求DTO可用`@NotNull`与`@Positive`（`jakarta.validation.constraints`）约束，Service仍要校验整数/小数单位和请假规则，SQL里的`#{requestDays} > 0`再守住一次边界。若传入负数而只检查`remaining_days >= requestDays`，减去负数反而可能增加余额。原子条件UPDATE保护判断与修改的竞争窗口，**不能替代输入校验**；表上的非负`CHECK`或等效数据库约束可作为最终不变量保护，具体支持须按数据库版本核对。

单条条件UPDATE通常比“SELECT后在Java判断再UPDATE”更短、更容易保持原子性。影响1行才是扣减成功；0行可能是余额不足、员工不存在、输入非法或状态不允许，不能一律告诉用户“余额不足”。先按规格决定是否额外查询区分原因，注意并发时事后查询只供错误分类，不回溯改变本次UPDATE结果。两个并发请求各要7天而余额为10天时，最多一个影响1行，最终余额应为3且不为负；用两个独立连接或并发JUnit请求验证影响行数、最终余额和失败响应。该表只是独立设计片段，不永久加入员工主线。

### 方案3：乐观锁——提交时验证version

乐观锁适合冲突不频繁、用户可能在编辑画面停留较久的场景。它不在用户编辑期间持有数据库锁，而是在提交UPDATE时确认数据仍是自己读取的版本。

以下是独立实验的关键改动，不是只执行一条DDL就完成乐观锁。必须同时改详情查询、返回DTO、更新请求DTO、Mapper与Service，并为冲突异常配置409响应。主线第13、14章的员工接口尚无version字段，因此不要把下面的SQL单独放入现有Mapper XML。

先在个人练习库增加实验列：

```sql
ALTER TABLE employees
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

读取详情时把 `version` 从数据库映射到Java对象并返回给前端；前端编辑时保存这个值，更新请求再带回原version。Service不能在提交前重新查询version并覆盖客户端传回的旧值，否则冲突检测会失效。Mapper XML中的 `#{version}` 必须来自**请求携带的旧版本**。

对应到员工项目，需要同时调整这些位置，而不只是增加数据库列：

| 文件或位置 | 必须增加或修改什么 | 验证点 |
| --- | --- | --- |
| `employees` 表 | `version BIGINT NOT NULL DEFAULT 0` | 旧行初始化为0 |
| `Employee`、`EmployeeMapper.xml` 的 `employeeResultMap` 与详情SELECT | 增加 `version` 属性、映射和查询列 | Mapper读出的版本与数据库一致 |
| `EmployeeResponse` | 返回当前 `version` | 前端能保留读取时的版本 |
| `EmployeeUpdateRequest` | 增加必填旧版本，建议 `@NotNull @Min(0)` | 漏传或负值被请求校验拒绝 |
| `EmployeeMapper.updateWithVersion(...)` | SQL同时匹配 `id` 与旧 `version`，成功时版本加1 | 两个旧版本请求仅一个影响1行 |
| `EmployeeServiceImpl.update(...)` | 保留部门校验、邮箱冲突转换、履历写入和 `@Transactional`；根据影响行数区分不存在与版本冲突 | 冲突时不写履历，成功时两表共同提交 |
| `GlobalExceptionHandler` 与测试 | 将版本冲突映射为409，并测试正常、旧版本及不存在id | HTTP、数据库、履历三者一致 |

这张表是**实验改修清单**，不是已完成的主线文件。`@Min(0)` 与 `@NotNull` 分别来自 `jakarta.validation.constraints`；Controller原有的 `@Valid` 才会触发请求校验。`version` 是并发控制字段，不应由Service用提交前新查到的值替换客户端的旧值。在 `Employee` 中增加 `Long version` 及getter/setter，在只读的 `EmployeeResponse` 中增加字段、构造器参数和getter，并更新构造响应对象的位置；在 `EmployeeUpdateRequest` 中增加 `@NotNull @Min(0) private Long version;` 及getter/setter。详情SELECT和 `employeeResultMap` 同时增加 `version` 列与属性映射。

把下列声明加入 `EmployeeMapper.java`，参数是已经经过Service规范化、且保留请求旧版本的 `Employee`；XML中的每个 `#{...}` 都从这个对象读取：

```java
int updateWithVersion(Employee employee);
```

```xml
<update id="updateWithVersion">
    UPDATE employees
    SET name = #{name},
        email = #{email},
        department = #{department},
        version = version + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
      AND version = #{version}
</update>
```

在原 `EmployeeServiceImpl.update(...)` 中按主线做完部门和邮箱规范化后，构造 `Employee employee` 时照旧设置 `id`、`name`、`department`、`email`，**另外执行 `employee.setVersion(request.getVersion())`**。保留 `@Transactional`，用以下片段替换原 `employeeMapper.update(employee)` 的调用及其影响行数判断，后面的履历INSERT仍只在成功时执行：

```java
int updatedRows = employeeMapper.updateWithVersion(employee);
if (updatedRows == 0) {
    if (employeeMapper.findById(id) == null) {
        throw new EmployeeNotFoundException(id);
    }
    throw new DataConflictException(
            "员工信息已被其他用户更新，请重新读取");
}
if (updatedRows != 1) {
    throw new EmployeeSystemException("修改员工影响多行");
}
```

这里只替换影响行数判断片段，原来的 `try/catch (DuplicateKeyException)` 及履历写入不得删除。`DataConflictException` 应作为运行时业务异常，由项目异常处理器映射为409；成功后再次读取员工响应时必须包含数据库新版本。上面是**原方法内部的增量片段**，不是可单独编译的完整方法。

`updateWithVersion` 与 `DataConflictException` 是本附录假定新增的Mapper方法和业务异常，不存在于第13章主线代码。只有在完成上面列出的字段、异常处理和测试后，才可期待下面的接口结果。影响0行也可能是员工不存在；本例再次查询只用于区分404与409，不能用“重新查到的version”代替客户端旧version。并发删除等更复杂竞态需按接口规格另行处理。

并发过程是：

```text
A读取version=3                 B读取version=3
A以WHERE version=3更新成功
数据库version变为4            B以WHERE version=3更新0行
A返回成功                      B返回409 Conflict
```

不要在B失败后静默重试完整UPDATE，否则仍可能覆盖A的变更。正确做法通常是让用户读取最新数据、比较差异并重新决定。

实验前后的员工、履历及临时`version`列按第八节统一记录与恢复。以下删列SQL**仅用于隔离且允许重建的专用实验库**，必须在实验事务全部结束、相关Mapper/DTO/异常映射均已移除且代码不再引用`version`后执行；删除列会丢失实验版本值，不得在共享或生产库执行：

```sql
ALTER TABLE employees DROP COLUMN version;
```

### 方案4：悲观锁——先锁定最新记录再处理

冲突概率高、必须读取最新状态后立即计算并写入时，可以在同一事务中使用锁定读。下列 `findByIdForUpdate`、`updateDepartment` 是需要另外声明Mapper接口方法和编写SQL的实验方法，不是第13章已有方法：

```java
Employee findByIdForUpdate(@Param("id") Long id);
int updateDepartment(
        @Param("id") Long id,
        @Param("department") String department);
```

这两个声明放在实验用 `EmployeeMapper` 中，需导入 `org.apache.ibatis.annotations.Param`。`@Param` 为多个或单个方法参数指定XML里 `#{id}`、`#{department}` 使用的名称；这里用它明确对应关系。对应的 `src/main/resources/mapper/EmployeeMapper.xml` 在现有 `<mapper>` 内、普通 `findById` 映射旁边增加**两个独立标签**；锁定读复用主线已有的 `employeeResultMap`：

```xml
<select id="findByIdForUpdate" parameterType="long"
        resultMap="employeeResultMap">
    SELECT id, name, department, email, status,
           created_at, updated_at
    FROM employees
    WHERE id = #{id}
    FOR UPDATE
</select>

<update id="updateDepartment">
    UPDATE employees
    SET department = #{department},
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
</update>
```

XML的 `id` 必须与接口方法名一致；`#{...}` 是MyBatis参数占位符，不能原样贴到数据库客户端执行。普通详情查询继续使用原来的 `findById`，不能因为新增锁定读而把所有GET请求都改为 `FOR UPDATE`。

```java
@Transactional
public void changeDepartment(Long id, String department) {
    Employee employee = employeeMapper.findByIdForUpdate(id);
    if (employee == null) {
        throw new EmployeeNotFoundException(id);
    }
    employeeMapper.updateDepartment(id, department);
}
```

`FOR UPDATE` 和后续UPDATE必须处于同一事务。若查询方法先结束并提交，再执行更新，锁已经释放，设计目标没有实现。该例只展示锁定读与写入的边界；若把它用于主线员工更新，还必须保留第13章的履历写入与部门校验，不能用此片段替换完整 `update()`。

如果确实需要在第13章员工更新中读取**事务内最新的旧部门**来写履历，最小的实验性接入位置是 `EmployeeServiceImpl.update(Long id, EmployeeUpdateRequest request)` 中的读取行：保留方法原有的 `@Transactional`、校验、UPDATE、异常转换、履历INSERT和返回值，只把

```java
Employee beforeUpdate = findEmployeeOrThrow(id);
```

替换为

```java
Employee beforeUpdate = employeeMapper.findByIdForUpdate(id);
if (beforeUpdate == null) {
    throw new EmployeeNotFoundException(id);
}
```

同时在原 `EmployeeMapper.java` 增加 `Employee findByIdForUpdate(@Param("id") Long id);` 并加入上面的同名XML `<select>`；这个路径不需要新增 `updateDepartment`，仍调用主线原有的 `employeeMapper.update(employee)` 和 `employeeChangeLogMapper.insert(changeLog)`。执行顺序是“Service代理开启事务 → 锁定读员工行 → 更新员工 → 写履历 → 提交并释放锁”。第二个请求要修改同一员工时会等待第一个事务结束，然后读取最新部门，履历才能记录正确的前后变化；如果履历写入失败，事务回滚时员工修改也撤销。

但这**不能解决用户在旧画面停留十分钟后的覆盖问题**：第二个请求虽然等到锁并读到最新数据库行，却仍可能用旧请求体中的姓名、邮箱覆盖别人刚提交的值。旧画面编辑应采用上面的version比较，或在取得锁后把客户端携带的旧版本/旧值与当前行比较，不一致时返回冲突而不是继续UPDATE。悲观锁保护的是事务内的读—改—写窗口，不自动判断浏览器数据是否过期。

若采用锁定读，还需在设计书明确：按主键还是范围查找、是否有索引、锁持有到哪一步、哪些异常回滚、等待多久后报错、是否允许有限重试，以及多个员工同时更新时的固定加锁顺序。`FOR UPDATE NOWAIT` 在无法立即取得锁时直接报错；`SKIP LOCKED` 会跳过已锁行，更适合工作队列，不能用于“指定员工不存在就跳过”的普通详情或更新接口。本练习保留默认等待行为，先用两个数据库会话观察，再根据项目规格决定超时与响应。

悲观锁会让竞争事务等待，因此需要：

- 使用主键或有合适索引的条件缩小锁范围；
- 保持事务短；
- 多行操作保持统一顺序；
- 设计锁等待超时和死锁后的响应；
- 不把它当作所有更新的默认方案。

### 方案选择表

| 问题 | 优先考虑 | 主要判断 |
| --- | --- | --- |
| email、业务编号不能重复 | 唯一约束 | 数据库必须守住最终不变量 |
| 余额、库存必须满足条件后扣减 | 原子条件UPDATE | 判断和修改可否合并成一条SQL |
| 编辑画面停留久、冲突较少 | 乐观锁version | 冲突时允许用户重读再决定 |
| 必须读最新状态后立刻写、冲突较高 | 悲观锁 `FOR UPDATE` | 可否接受等待，事务能否保持很短 |
| 多条SQL必须共同成功 | 事务 | 它与上面并发控制方案可以组合使用 |
| 所有事务仿佛逐个执行 | `SERIALIZABLE` | 成本高，只在明确规格与验证后选择 |

日本业务系统中的选型还应把失败后动作写进规格，而不是只选技术名：

| 改修任务 | 优先调查的机制 | 必须写清的失败结果 |
| --- | --- | --- |
| 社员编号/邮箱不得重复 | `UNIQUE`及异常转换 | 冲突响应，库中仍只有一行 |
| 库存或休假余额不能扣负 | 输入校验＋原子条件UPDATE | 影响0行的原因分类，最终值不为负 |
| 两名管理员编辑同一员工 | version乐观锁 | 旧版本409、不覆盖、不写履历 |
| 后台审批状态迁移 | 状态条件UPDATE或短事务锁定读 | 状态已变化时冲突，不重复审批 |
| 多员工批量更新 | 事务＋固定访问顺序 | 失败批次回滚，重试范围明确 |
| Batch与在线同时更新 | 竞争范围调查＋事务＋并发控制 | 在线/Batch各自的冲突或重试契约 |

在设计评审中，把选择写成可验收的规则。例如“员工编辑：请求必须带读取时的version，旧版本返回409且不写履历”比“使用乐观锁”更能指导开发；“后台状态迁移：按主键 `FOR UPDATE`，短事务内判断最新状态并更新，等待超时按约定失败”比“加悲观锁”更能指导测试。锁方案不仅是SQL，还要同时落实索引、Service事务边界、异常响应和证据。

| 验证场景 | 应观察的证据 | 不能只看什么 |
| --- | --- | --- |
| 两个旧版本更新同一员工 | 一个UPDATE影响1行，另一个0行/409；成功者数据和履历一致 | 两个请求都返回了响应 |
| 两个事务锁同一员工 | 第二个在第一个提交前等待；提交后读到最新部门 | 单次 `FOR UPDATE` 成功 |
| 锁内履历INSERT失败 | 员工更新回滚；锁释放；后续请求可继续 | 仅看HTTP 500 |
| 修改不同员工 | 不应因错误的范围扫描而无谓互相等待 | 只看索引是否存在 |
| 旧画面提交 | 检测旧version或旧值并拒绝覆盖 | 只确认使用了 `FOR UPDATE` |

## 六、锁的底层机制：S/X、Record、Gap、Next-Key

### 1. 共享锁与排他锁

| 概念 | 目的 | 直观效果 |
| --- | --- | --- |
| 共享锁，S Lock | 保护读取的记录不被冲突方式修改 | 多个事务可共享读取，但与需要修改的排他锁冲突 |
| 排他锁，X Lock | 保护即将或正在修改的记录 | 其他冲突读写需要等待事务结束 |

下面是同一资源上典型的S/X兼容关系；InnoDB还有意向锁和其他锁模式，不能用这张简表推断全部内部锁：

| 已有锁 | 新S锁 | 新X锁 |
| --- | --- | --- |
| S锁 | 兼容 | 冲突 |
| X锁 | 冲突 | 冲突 |

InnoDB内部还使用表级意向锁来表示事务准备在表内取得哪类行锁。新人阶段不需要手工管理意向锁，但应知道“行锁”和“表上完全没有锁”不是同一件事。

普通 `SELECT` 通常是MVCC一致性读，并不会因为读到某行就替业务长期锁住它；`SELECT ... FOR SHARE` 是共享锁定读，`SELECT ... FOR UPDATE` 是排他锁定读，`UPDATE`/`DELETE` 也会为其写入取得必要的锁。锁的获取由数据库执行SQL时完成，不由Java对象上的 `synchronized` 或 `@Transactional` 注解单独完成。`@Transactional` 的关键作用是让锁定读和后续写入留在**同一个数据库事务**内，直到提交或回滚才释放锁。

### 2. 记录锁、间隙锁和next-key lock

| 锁 | 保护对象 | 常见目的 |
| --- | --- | --- |
| Record Lock | 索引中的某条记录 | 阻止同一记录发生冲突修改 |
| Gap Lock | 两条索引记录之间的间隙 | 阻止其他事务向范围插入新记录 |
| Next-key Lock | 记录锁加其前方间隙锁 | 在范围扫描中保护已有记录与插入范围 |

InnoDB的“行锁”实际与索引记录密切相关。若 `WHERE` 条件没有合适索引，数据库可能扫描并锁定比预想更多的记录或范围。因此索引不仅影响速度，也会影响并发等待范围。

比较下面三个**MySQL 8.0 InnoDB锁定读设计片段**；不要直接在共享库执行。它们的实际锁数不是固定常量，须结合`EXPLAIN`、表索引、行分布和隔离级别观察：

```sql
SELECT id, name, department FROM employees WHERE id = 1001 FOR UPDATE;
SELECT id, name, department FROM employees
WHERE department = 'Sales' FOR UPDATE;
SELECT id, name, department FROM employees
WHERE name LIKE '%Tanaka%' FOR UPDATE;
```

第一条按主键等值查找，若记录存在，通常能较精确地定位目标索引记录。第二条如果部门索引和条件选择性不同，可能扫描一段索引范围，`REPEATABLE READ`下还可能涉及间隙/next-key锁；第三条前导通配符通常难以利用普通B-tree索引作前缀定位，扫描及锁范围可能扩大。`READ COMMITTED`与`REPEATABLE READ`的间隙锁使用也不同。不能宣称“WHERE id只锁一行”或“LIKE必锁全表”；用执行计划和两个连接的等待现象核对。

### 3. 锁什么时候释放

写锁和锁定读获得的锁通常保持到事务提交或回滚，而不是Java方法执行完某一行就立刻释放。下列写法会无谓拉长锁时间：

```text
取得FOR UPDATE锁
  → 调用外部审批API并等待5秒
  → 写数据库
  → 提交
```

应先在事务外准备可准备的信息，再进入短事务完成必须原子执行的读取与写入。

## 七、锁等待与死锁

### 1. 锁等待不是死锁

事务A锁住员工1001尚未提交，事务B也要修改1001时，B等待A释放锁属于普通锁等待。A提交后，B可能继续执行。若A长时间不结束，B可能发生锁等待超时。

首先调查为什么事务持续过久，而不是只增大超时时间。常见原因包括慢SQL、缺少索引、外部调用放在事务内、批量范围过大，以及忘记提交的手工数据库会话。

### 2. 死锁怎样产生

```text
事务A：锁住员工1001 → 等待员工1002
事务B：锁住员工1002 → 等待员工1001
```

双方都等待对方释放资源，无法自行继续。InnoDB通常会检测死锁，并选择一个事务作为牺牲者回滚，让另一个继续。

数据库能解除这一次僵局，不代表应用无需处理。项目应准备接收死锁异常；如果业务操作具备幂等性并且规格允许，可以有限次数重试整个事务，而不是只重放中间一条SQL。

锁等待与死锁要分开：等待可能在持锁者提交后继续，也可能因超时失败；死锁形成循环等待，InnoDB在启用死锁检测时通常选择一个事务回滚。两者都不是“数据库文件损坏”的直接证据。MySQL对**锁等待超时**默认可能只回滚当前语句而非整个事务，具体受配置影响，因此应用捕获到异常后不能假定事务状态仍可安全继续；应让业务事务按明确回滚规则结束，再决定是否重试。

```text
发生等待/死锁 → 记录requestId、业务键、SQL阶段、数据库错误码
  → 查哪个事务持锁、执行计划与访问顺序
  → Spring/MyBatis接收并转换异常
  → 判断业务是否允许重试、整个事务是否已回滚
  → 允许：在事务边界外有限次数重试整个业务用例
  → 不允许：返回约定错误，并核对最终数据
```

Spring数据访问异常层级可能出现`PessimisticLockingFailureException`、`CannotAcquireLockException`等；旧的`DeadlockLoserDataAccessException`已被标为过时。具体转换还取决于MyBatis/Spring集成、驱动、SQLState和数据库错误码，不要断言“所有死锁都必然是同一个Java类”。调查时保留根因错误码与数据库端证据，再决定错误响应。有限重试应有次数上限、间隔和幂等性保障；外部通知、文件交付及不可重复操作不能仅因数据库事务回滚就机械重做。

### 3. 降低死锁和等待的方法

1. 让事务短、小，完成后立即提交；
2. 所有路径按同一顺序访问表和记录；
3. 为锁定读和UPDATE的检索条件建立合适索引；
4. 一次只处理必要行，避免无边界批量更新；
5. 不在事务中等待用户或慢速外部系统；
6. 记录错误时间、SQL场景和requestId，结合数据库信息调查；
7. 对重试设置次数上限，并确认操作可以安全重做。

调查最近一次InnoDB死锁时，具有数据库权限的人员可以在批准的环境执行：

```sql
SHOW ENGINE INNODB STATUS;
```

该结果可能包含表名、SQL和运行信息。正式环境应按权限和脱敏规则保存证据，不要把整段生产信息直接贴入公开票据。

## 八、两个会话与API的验证实验

并发问题不能只发送一次Postman请求就下结论。优先使用可按已知初始化脚本重建的**专用实验数据库或独立实验记录**；其次才是在个人专用练习库中使用明确标记的实验员工。两个数据库客户端连接A、B必须属于同一被允许的实验库，并明确控制提交顺序。禁止在生产、共享开发或其他学员共用数据库执行清空、DROP、批量DELETE或锁实验。若第12章的测试脚本包含DROP，只能在其既有专用测试库和保护规则下运行，本附录不修改`MySqlTestGuard`或扩大脚本执行范围。

### 实验前记录：先区分SQL与真实API

| 实验方式 | `employees`可能变化 | `employee_change_logs`可能变化 | 恢复重点 |
| --- | --- | --- | --- |
| 数据库客户端直接执行本节原生SQL | SQL明确修改的员工字段、临时`version` | 通常不会调用履历Mapper，因此不因这两条UPDATE新增业务履历 | 按记录核对并恢复SQL直接改动的值，不虚构履历变化 |
| 通过真实员工HTTP API调用第13章Service | 员工字段；若完成乐观锁改修，成功时还更新`version` | 成功请求可能新增变更履历；冲突请求不应新增 | 同时核对员工、版本和新增履历，按环境恢复或保留审计证据 |

在**建`version`列之前**记录数据库名称及其是否为个人专用实验库、实验员工id、原姓名、部门、邮箱、必要的其他业务字段、`SHOW CREATE TABLE employees`结果、该员工履历原有条数与可辨认的履历id；为避免他人同时改动，实验期间只使用自己控制的记录。MySQL 8.0 InnoDB示例读取如下，`1001`须替换为自己的实验员工id：

```sql
SHOW CREATE TABLE employees;
SELECT id, name, department, email, status
FROM employees WHERE id = 1001;
SELECT id, action, detail, created_at
FROM employee_change_logs WHERE employee_id = 1001 ORDER BY id;
```

**主线尚无`version`列，建列前不要执行`SELECT version`。** 按第五节“方案3”在专用实验库建列后，再执行`SELECT id, version FROM employees WHERE id = 1001;`记录本次版本起点；同时保留建列前的表结构记录。若只做`FOR UPDATE`等待实验而不做乐观锁，不需要建列。记录实验前履历条数及id，是为了区分原有履历和可能由真实API新增的履历，**不是授权按id范围批量删除**。

### 实验1：观察 `FOR UPDATE` 等待

连接A：

```sql
START TRANSACTION;

SELECT id, name, department
FROM employees
WHERE id = 1001
FOR UPDATE;

UPDATE employees
SET department = 'Development'
WHERE id = 1001;
```

此时先不提交。连接B执行：

```sql
START TRANSACTION;

SELECT id, name, department
FROM employees
WHERE id = 1001
FOR UPDATE;
```

B会等待A释放冲突锁。回到A执行：

```sql
COMMIT;
```

观察B继续执行后，B也应明确 `COMMIT` 或 `ROLLBACK`，不要让实验事务一直占用连接和锁。

### 实验2：验证乐观锁冲突

先按本节“实验前记录”核对员工1001及履历、保存建列前表结构，再在允许的专用实验库按“方案3”增加`version`；不要使用共享数据。建列后连接A、B各执行 `SELECT id, department, version FROM employees WHERE id = 1001`，记下相同的旧版本，例如0。然后在A执行：

```sql
UPDATE employees
SET department = 'Development', version = version + 1
WHERE id = 1001 AND version = 0;
```

A应影响1行；若客户端未开启自动提交，先在A执行 `COMMIT`。然后B仍带**最初读到的0**执行：

```sql
UPDATE employees
SET department = 'Support', version = version + 1
WHERE id = 1001 AND version = 0;
```

B应影响0行。再次查询应看到Development与version=1，不能把B的0行当成功。若实际起始版本不是0，把两条SQL中的0都替换为**实验前读到的同一个旧版本**，最终版本应为旧版本加1。随后通过已完成version传递与异常映射的实验接口验证B返回409；只执行这两条SQL不能证明HTTP响应。

这两条原生UPDATE只改员工行，不经过第13章的Service及履历Mapper，通常不会新增`employee_change_logs`；若再调用了真实HTTP API，则按下方统一恢复手顺分别核对两类变化，不能只恢复部门。

### 统一恢复：员工当前值与履历分别核对

1. 确认连接A、B及Postman实验请求已结束；两个数据库连接均明确`COMMIT`或`ROLLBACK`，没有遗留事务或锁等待。
2. 对照实验前记录查询员工当前姓名、部门、邮箱及其他被改字段；若建过`version`列，再查询当前版本和原记录的版本起点。不要把“只恢复部门”当成整个员工记录已恢复。
3. 分清原生SQL与真实API各自做了什么。原生SQL通常没有业务履历；成功的API更新会经第13章事务写入履历，409冲突分支不应写入。
4. 查询该员工`employee_change_logs`的条数和id，与实验前记录逐条对照；若有API新增履历，先确定是保留审计证据，还是在**可重建的专用实验数据库**中按既有初始化流程整体恢复已知状态。
5. 首选重建专用实验库或从实验前备份恢复独立实验记录；执行初始化脚本前再次核对数据库名称、现有专用测试库保护机制和允许的恢复范围。不得把含DROP的测试脚本拿到个人业务库、共享库或正式库执行。
6. 如果使用个人练习库，只恢复能明确识别且允许恢复的实验员工值；不要用模糊条件`DELETE FROM employee_change_logs WHERE employee_id = ...`清理履历。无法安全撤销API产生的履历时，保留并标记实验记录，不能声称数据库已回到完全相同的历史状态。
7. 移除临时Mapper方法、DTO/Entity字段、冲突异常映射和其他实验改动，并确认代码已不再引用`version`。只在隔离且允许重建的专用实验库中按第五节给出的SQL删除临时`version`列；若没有建列，就不要执行删列。
8. 重新查询员工值、履历条数及id，并对照建列前`SHOW CREATE TABLE employees`确认结构。运行第13章原事务回归测试；记录哪些数据完全恢复、哪些审计履历按规则保留。

只做本节**原生SQL**实验、确认实验前部门确为`Sales`、实验后确为`Development`，且期间没有通过API写入履历时，可在隔离实验库按已记录的id执行以下**定向恢复示意**；先核对实际值，条件不符时停止，不要改成无条件覆盖：

```sql
UPDATE employees
SET department = 'Sales'
WHERE id = 1001 AND department = 'Development';

SELECT id, name, department, email
FROM employees WHERE id = 1001;
```

第一条预期影响1行；若影响0行，先调查是否有人或其他实验改变了记录。若数据库客户端关闭了自动提交，在核对结果后还须明确`COMMIT`，并用新查询确认保存。该示意**不适用于已调用真实员工API的场景**，不能用它替代履历核对，也不能恢复未记录的姓名、邮箱或其他字段。

**恢复员工当前字段，不代表历史履历自动恢复。正式业务系统中的审计履历通常不能随意删除。** 本实验优先通过专用、可重建的数据环境恢复，而不是让学员对真实业务履历执行DELETE。若只执行了两条原生SQL且没有触发业务Mapper，应如实记录“履历未变化”，不假定每个并发实验都会新增履历。

## 九、综合案例

**验收起点。** 沿用第一节的员工1001：`Tanaka / Sales / old@example.com`。在可重建的专用MySQL 8.0 InnoDB实验库和实验工程分支中，A、B分别通过Postman或两个测试请求读取同一详情；A把姓名改成`Tanaka Updated`并先提交，B仍用旧完整表单只把邮箱改成`new@example.com`。先按第八节记录原字段、履历和表结构；做乐观锁实验时再记录建列后的version起点。对比原更新与第五节“方案3”改修后的结果：

| 验证项目 | 无version校验的原更新 | 使用version校验 |
| --- | --- | --- |
| A、B读取同一员工 | 都成功，各持一份旧画面 | 都成功，各持同一旧version |
| A先提交 | 成功 | UPDATE影响1行，version加1 |
| B随后提交旧姓名和新邮箱 | 可能把姓名恢复为`Tanaka` | 旧version与数据库不匹配 |
| B的UPDATE／HTTP结果 | 可能影响1行并返回200 | 影响0行，按规格返回409 Conflict |
| 最终员工数据 | 可能是`Tanaka / new@example.com` | 保留`Tanaka Updated / old@example.com` |
| 变更履历 | 两次成功更新可能各写一条 | A新增一条；B冲突不新增 |

**验证证据。** 保存A、B请求体中的旧version、发送顺序、HTTP状态与响应、每次UPDATE影响行数，以及最终`employees`数据和`employee_change_logs`条数。改修后预期A成功、B为409，最终姓名为`Tanaka Updated`、邮箱仍为`old@example.com`，version只增1、仅A新增一条履历；另外测不存在id仍按原规格404。若两请求都成功，先查请求是否真的携带相同旧version、Service有没有用新查到的version覆盖旧值、Mapper XML是否同时匹配id/version。B的邮箱输入应由前端保留供用户重新确认，不能自动覆盖数据库。

**恢复。** 按第八节“统一恢复”依次结束事务、对照姓名、部门、邮箱及版本、核对API可能新增的履历，再恢复专用实验数据和临时代码。不能只把姓名改回去就宣称实验完全恢复，也不能按员工id模糊删除审计履历。确认没有代码引用`version`后，才可在隔离且允许重建的专用实验库删除临时列；最后运行第13章原事务测试。共享库和生产库不得执行本实验的删列或履历清理。

## 十、事务和并发控制的常见误区

| 误区 | 为什么错误 | 正确方向 |
| --- | --- | --- |
| 加了 `@Transactional` 就不会并发覆盖 | 它保证单个事务的提交/回滚，不比较两个用户的旧版本 | 使用原子UPDATE、version或锁 |
| SELECT确认不存在后就能安全INSERT | 检查与INSERT之间有竞态窗口 | 数据库唯一约束兜底 |
| `readOnly = true` 可以防止所有写SQL | 它主要是只读提示，具体行为依赖基础设施 | 权限、Review和测试仍要负责 |
| 隔离级别越高越好 | 高隔离会增加等待、冲突和吞吐成本 | 根据并发现象和规格选择 |
| 出现死锁说明数据库坏了 | 正确应用在并发下也可能偶发死锁 | 缩短事务、统一顺序并处理重试 |
| 发生锁等待就增大timeout | 可能只是掩盖慢事务和缺失索引 | 先定位锁持有者和长事务原因 |
| 冲突后自动重试完整用户更新 | 可能再次覆盖别人已经提交的数据 | 让用户重读、比较和确认 |
| 普通SELECT已经锁住读到的数据 | InnoDB普通一致性读通常使用MVCC快照 | 需要当前锁定读时明确使用 `FOR UPDATE` |

## 十一、Review与故障调查清单

看到事务或并发改修时，按下面顺序检查：

### 业务边界

- 哪些数据变化必须共同成功或共同失败？
- 失败时调用方收到什么HTTP状态和错误信息？
- 是否存在不能重复、不能为负、不能覆盖的业务不变量？

### Spring边界

- `@Transactional` 是否放在Spring管理的Service公开方法上？
- 调用是否确实经过代理，而不是同类内部调用？
- 异常是否被吞掉？checked exception是否需要明确回滚？
- 传播方式和隔离级别是否有规格依据？

### SQL与数据库边界

- 是否存在“先SELECT再UPDATE/INSERT”的竞态窗口？
- 唯一性是否有数据库约束？
- UPDATE后是否检查影响行数？
- `FOR UPDATE` 和后续写入是否在同一事务？
- WHERE条件是否有合适索引，锁范围是否过大？
- 多表、多行操作的访问顺序是否稳定？

### 验证证据

- 正常路径是否证明所有变化都提交？
- 中途失败是否证明所有相关变化都回滚？
- 两个并发请求是否证明不会丢失更新或突破余额规则？
- 是否验证409、超时、死锁等失败路径？
- 测试是否使用专用数据库并清理实验结构？

## 十二、练习任务

### 练习1：判断应该使用什么机制

分别为下面场景选择事务、唯一约束、原子UPDATE、乐观锁或悲观锁，并写出理由：

1. 新增员工时email不得重复；
2. 员工更新和变更履历必须共同成功；
3. 两名管理员可能同时编辑同一员工资料；
4. 扣减休假余额后不得小于0；
5. 必须读取最新审批状态后立即完成状态迁移。

### 练习2：Review一段有问题的流程

```text
开启事务
  → SELECT剩余天数
  → 调用外部日历API
  → Java判断余额
  → UPDATE余额
  → 捕获所有异常后返回成功
```

至少指出四个问题，并给出修改后的事务边界、并发控制方式和失败响应。

### 练习3：整理并发自测证据

用两个独立请求或两个数据库会话验证一次并发冲突，证据至少包含：

- 建列前员工字段、表结构和履历条数／id；做乐观锁实验时再记录建列后的version起点；
- A、B的执行顺序；
- 每次UPDATE影响行数或HTTP状态；
- 最终员工字段、version（若存在）以及履历变化；标明使用原生SQL还是API；
- 为什么该结果符合规格；
- 实验后恢复了哪些数据、结构和代码，哪些审计履历按规则保留。

## 十三、重要概念与完成标准

| 容易混淆 | 区别 |
| --- | --- |
| 乐观锁与悲观锁 | 前者提交时比较旧版本；后者在短事务内先锁最新行 |
| 数据库约束与业务校验 | `UNIQUE`/`CHECK`守最终底线；请求与Service校验提供业务语义 |
| 锁等待与死锁 | 前者可能等到持锁者释放；后者形成循环等待，需要选牺牲事务解除 |
| 回滚与重试 | 回滚结束失败事务；重试是重新执行业务，需幂等与有限次数 |

### 本附录完成标准

完成本附录后，不要求把所有锁机制加入Employee Management主线。你需要能够根据业务问题选择最小且可验证的机制：用事务保护多步业务原子性，用约束保护最终不变量，用原子SQL、乐观锁或悲观锁处理具体并发竞争，并用隔离级别、索引、锁范围和失败处理解释运行结果。

进一步排查事务未回滚、锁等待或数据库异常时，可结合[B06 Spring Boot常见问题与排查索引](B06_troubleshooting_review_index.md)确定调查入口。

## 十四、官方参考资料

- [Spring Framework：使用 `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [MySQL 8.4：InnoDB事务模型](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-model.html)
- [MySQL 8.4：InnoDB事务隔离级别](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)
- [MySQL 8.4：InnoDB锁](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking.html)
- [MySQL 8.4：InnoDB死锁](https://dev.mysql.com/doc/refman/8.4/en/innodb-deadlocks.html)
- [MySQL 8.0：死锁调查与重试](https://dev.mysql.com/doc/refman/8.0/en/innodb-deadlocks-handling.html)
- [Spring Framework：悲观锁异常层级](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/dao/PessimisticLockingFailureException.html)
- [MySQL 8.0：锁定读与 `NOWAIT`/`SKIP LOCKED`](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html)
