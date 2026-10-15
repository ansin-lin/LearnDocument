# A13-3 并发更新与锁

完成[第13章：用事务保证多步写入一致](../mainline/13_transaction_and_consistency.md)后，本附录处理多个请求同时修改数据时的约束、原子UPDATE、乐观锁、悲观锁、锁等待与死锁。事务基本机制见[A13-1](A13_01_transaction_spring_mechanisms.md)，读取现象、隔离级别和MVCC见[A13-2](A13_02_isolation_mvcc.md)。课程环境按MySQL 8的InnoDB说明；其他数据库的锁范围和异常行为应以项目文档和实测为准。

第13章工程没有 `version` 列和休假余额表；下文相关代码是**独立扩展实验或设计片段**，不能直接追加到主线工程。实验仅在个人专用练习库进行，开始前记录原数据，结束后恢复表、代码和测试。

| 业务要求 | 主要机制 |
| --- | --- |
| 员工更新与履历写入共同成功或失败 | Service业务方法上的 `@Transactional` |
| 两名用户不能基于旧画面相互覆盖更新 | 原子条件UPDATE、乐观锁或悲观锁等并发控制 |
| email不能重复 | 数据库 `UNIQUE` 约束 |
| 余额扣减后不能为负数 | 带条件的原子UPDATE |

`@Transactional` 保护的是一次业务操作的提交/回滚边界，不会自动比较两个用户各自读取的旧数据，也不能单独解决丢失更新。

## 一、锁的基本概念

### 1. 共享锁与排他锁

| 概念 | 目的 | 直观效果 |
| --- | --- | --- |
| 共享锁，S Lock | 保护读取的记录不被冲突方式修改 | 多个事务可共享读取，但与需要修改的排他锁冲突 |
| 排他锁，X Lock | 保护即将或正在修改的记录 | 其他冲突读写需要等待事务结束 |

InnoDB内部还使用表级意向锁来表示事务准备在表内取得哪类行锁。新人阶段不需要手工管理意向锁，但应知道“行锁”和“表上完全没有锁”不是同一件事。

### 2. 记录锁、间隙锁和next-key lock

| 锁 | 保护对象 | 常见目的 |
| --- | --- | --- |
| Record Lock | 索引中的某条记录 | 阻止同一记录发生冲突修改 |
| Gap Lock | 两条索引记录之间的间隙 | 阻止其他事务向范围插入新记录 |
| Next-key Lock | 记录锁加其前方间隙锁 | 在范围扫描中保护已有记录与插入范围 |

InnoDB的“行锁”实际与索引记录密切相关。若 `WHERE` 条件没有合适索引，数据库可能扫描并锁定比预想更多的记录或范围。因此索引不仅影响速度，也会影响并发等待范围。

### 3. 锁什么时候释放

写锁和锁定读获得的锁通常保持到事务提交或回滚，而不是Java方法执行完某一行就立刻释放。下列写法会无谓拉长锁时间：

```text
取得FOR UPDATE锁
  → 调用外部审批API并等待5秒
  → 写数据库
  → 提交
```

应先在事务外准备可准备的信息，再进入短事务完成必须原子执行的读取与写入。

## 二、四类常用并发控制方案

### 方案1：唯一约束——防止最终重复

第9章建立 `employees` 表时，`email` 已有 `uk_employees_email` 唯一约束。可以用数据库客户端检查现有表定义，不要对同一表再次执行 `ADD CONSTRAINT`：

```sql
SHOW CREATE TABLE employees;
```

适合“email不能重复”这类最终不变量。即使两个请求同时通过事前检查，数据库也只允许一个冲突值成功。Service需要把重复键异常转换为明确的业务冲突响应。

唯一约束不负责合并两次更新，也不替代多SQL事务。

### 方案2：原子条件UPDATE——在一条SQL中判断并修改

休假余额扣减可以避免“先查再改”的并发空隙。下例使用**主线不存在**的 `leave_balances(employee_id, remaining_days)` 表，以及相应Mapper和冲突异常，只用于说明条件UPDATE；若要实际运行，需先设计表、Mapper、异常到HTTP的映射和并发测试：

```sql
UPDATE leave_balances
SET remaining_days = remaining_days - #{requestDays}
WHERE employee_id = #{employeeId}
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

单条条件UPDATE通常比“SELECT后在Java判断再UPDATE”更短、更容易保持原子性。影响0行可能有多种原因，错误消息和后续调查应按规格区分。

### 方案3：乐观锁——提交时验证version

乐观锁适合冲突不频繁、用户可能在编辑画面停留较久的场景。它不在用户编辑期间持有数据库锁，而是在提交UPDATE时确认数据仍是自己读取的版本。

以下是独立实验的关键改动，不是只执行一条DDL就完成乐观锁。必须同时改详情查询、返回DTO、更新请求DTO、Mapper与Service，并为冲突异常配置409响应。主线第13、14章的员工接口尚无version字段，因此不要把下面的SQL单独放入现有Mapper XML。

先在个人练习库增加实验列：

```sql
ALTER TABLE employees
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

读取详情时把 `version` 从数据库映射到Java对象并返回给前端；前端编辑时保存这个值，更新请求再带回原version。Service不能在提交前重新查询version并覆盖客户端传回的旧值，否则冲突检测会失效。Mapper XML中的 `#{version}` 必须来自**请求携带的旧版本**：

```xml
<update id="updateWithVersion">
    UPDATE employees
    SET name = #{name},
        email = #{email},
        department = #{department},
        status = #{status},
        version = version + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
      AND version = #{version}
</update>
```

```java
int updatedRows = employeeMapper.updateWithVersion(request);
if (updatedRows == 0) {
    throw new DataConflictException(
            "员工信息已被其他用户更新，请重新读取");
}
```

`updateWithVersion` 与 `DataConflictException` 是本附录假定新增的Mapper方法和业务异常，不存在于第13章主线代码。只有在新增异常处理、明确把该异常映射为HTTP 409，并完成请求/响应对象的version字段后，才可期待下面的接口结果。影响0行也可能是员工不存在；正式实现应按接口规格区分404与409，但不能用“重新查到的version”代替客户端旧version。

并发过程是：

```text
A读取version=3                 B读取version=3
A以WHERE version=3更新成功
数据库version变为4            B以WHERE version=3更新0行
A返回成功                      B返回409 Conflict
```

不要在B失败后静默重试完整UPDATE，否则仍可能覆盖A的变更。正确做法通常是让用户读取最新数据、比较差异并重新决定。

若只做附录实验，先记录员工原部门；结束时恢复这条练习数据，再移除实验Mapper方法、DTO字段及异常映射，确认没有代码依赖version，最后在个人练习库恢复主线表结构。删除列会丢失实验版本值，不得在共享或生产库执行：

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

这两个声明放在实验用 `EmployeeMapper` 中，需导入 `org.apache.ibatis.annotations.Param`。`@Param` 为多个或单个方法参数指定XML里 `#{id}`、`#{department}` 使用的名称；这里用它明确对应关系。对应的Mapper XML片段为：

```sql
SELECT id, name, email, department, status
FROM employees
WHERE id = #{id}
FOR UPDATE;

UPDATE employees
SET department = #{department},
    updated_at = CURRENT_TIMESTAMP
WHERE id = #{id};
```

上面两条是MyBatis映射中使用的SQL主体，`#{...}` 是MyBatis参数占位符，不能原样贴到数据库客户端执行。实际加入Mapper XML时，分别放入 `<select id="findByIdForUpdate" resultType="com.example.employee.entity.Employee">` 与 `<update id="updateDepartment">`，不要把两条语句放进同一个映射标签。第一条是锁定读，第二条是受同一事务保护的更新。

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

## 三、锁等待与死锁

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

## 四、两个会话怎样验证并发现象

并发问题不能只发送一次Postman请求就下结论。可以在专用学习数据库打开数据库客户端的连接A和连接B，明确控制提交顺序。不要在开发共享库或生产库执行锁实验。

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

先在个人练习库按“方案3”增加 `version`，确认员工1001存在并记下原部门；不要使用共享数据。连接A、B各执行 `SELECT id, department, version FROM employees WHERE id = 1001`，记下相同的旧版本，例如0。然后在A执行：

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

实验结束按本附录第二节“方案3：乐观锁”中的恢复顺序处理：确认连接A、B都已提交或回滚，用实验前记录的原部门恢复员工1001（若原来是Sales，可在个人练习库执行 `UPDATE employees SET department = 'Sales' WHERE id = 1001`），移除临时Mapper方法、DTO字段和异常映射，确认工程不再引用version，最后执行前文的 `ALTER TABLE employees DROP COLUMN version`。重新查询员工数据、核对表结构，并运行第13章原有事务测试，确认回到主线稳定状态。不要在共享库或生产库执行这些恢复操作。

## 五、事务和并发控制的常见误区

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

## 六、Review与故障调查清单

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

## 七、练习任务

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

- 开始数据与version；
- A、B的执行顺序；
- 每次UPDATE影响行数或HTTP状态；
- 最终数据库记录；
- 为什么该结果符合规格；
- 实验后恢复了哪些结构和代码。

## 八、本附录完成标准

完成本附录后，不要求把所有锁机制加入Employee Management主线。你需要能够根据业务问题选择最小且可验证的机制：用事务保护多步业务原子性，用约束保护最终不变量，用原子SQL、乐观锁或悲观锁处理具体并发竞争，并用隔离级别、索引、锁范围和失败处理解释运行结果。

进一步排查事务未回滚、锁等待或数据库异常时，可结合[B07 Spring Boot常见问题与排查索引](B07_troubleshooting_review_index.md)确定调查入口。

## 九、官方参考资料

- [Spring Framework：使用 `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [MySQL 8.4：InnoDB事务模型](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-model.html)
- [MySQL 8.4：InnoDB事务隔离级别](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)
- [MySQL 8.4：InnoDB锁](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking.html)
- [MySQL 8.4：InnoDB死锁](https://dev.mysql.com/doc/refman/8.4/en/innodb-deadlocks.html)
