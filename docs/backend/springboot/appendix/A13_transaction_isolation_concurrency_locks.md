# A13 事务、隔离级别、并发更新与锁

本附录建议在完成[第13章：用事务保证多步写入一致](../mainline/13_transaction_and_consistency.md)后学习。第13章解决“一次员工更新要执行多条SQL时怎样共同提交或共同回滚”；本附录继续解决“多个请求同时读写相同数据时怎样避免覆盖、重复、超卖和长时间等待”。

课程环境按MySQL 8的InnoDB存储引擎说明。其他数据库即使使用相同SQL标准，MVCC、锁范围、默认隔离级别和异常行为也可能不同，应以项目使用的数据库文档和实测结果为准。

## 一、本附录的学习成果

完成后，你应能够：

1. 用一次业务操作解释事务边界、提交和回滚；
2. 用员工更新案例说明ACID四个特点；
3. 区分脏读、不可重复读、幻读和丢失更新；
4. 说明四种标准隔离级别，以及MySQL InnoDB默认行为；
5. 区分普通快照读、锁定读、乐观锁、悲观锁和数据库约束；
6. 根据冲突频率选择唯一约束、原子UPDATE、version或 `FOR UPDATE`；
7. 识别锁等待和死锁，并提出缩短事务、统一加锁顺序和有限重试等措施；
8. Review一段事务代码时指出边界过大、异常被吞、缺少并发控制等问题。

## 二、先理解事务是什么

### 1. 事务是一个业务工作单元

事务不是“某条SQL的别名”，而是需要作为一个整体完成的一组数据库操作。例如第13章的员工更新包含：

```text
读取员工旧数据
  → 更新employees
  → 写入employee_change_logs
  → 两步都成功时COMMIT
  → 任一步失败时ROLLBACK
```

如果员工表已经更新，而履历表写入失败后仍提交，数据库就进入“当前状态有变化，但没有审计履历”的不一致状态。因此这两次写入属于同一个业务工作单元，应放在同一事务边界中。

### 2. 提交和回滚

```text
START TRANSACTION
  → 执行SQL 1
  → 执行SQL 2
  ├─ 全部成功：COMMIT，变化对其他事务正式生效
  └─ 发生失败：ROLLBACK，撤销当前事务尚未提交的变化
```

MySQL连接通常启用 `autocommit`。此时每条单独执行的SQL默认各自提交。Spring的 `@Transactional` 会让一个方法边界内的多次数据库访问参加同一事务，并在方法正常结束时提交、符合回滚规则的异常离开方法时回滚。

事务边界必须对应业务含义。不是SQL越多越应该放在一起，也不是给所有Service方法统一加事务就更安全。

## 三、事务的ACID特点

ACID不是四个互不相关的口号，而是事务能够可靠处理数据变化的四个观察角度。

| 特点 | 含义 | 员工管理中的例子 |
| --- | --- | --- |
| Atomicity，原子性 | 一个事务中的操作要么全部完成，要么全部撤销 | 员工更新和履历写入不能只成功一个 |
| Consistency，一致性 | 事务前后都满足业务规则、数据库约束和数据关系 | email仍唯一，status仍是允许值，履历引用的员工存在 |
| Isolation，隔离性 | 并发事务互相影响的程度受到控制 | A修改员工时，B能读到什么、是否必须等待，由隔离和锁决定 |
| Durability，持久性 | 已提交的结果在数据库正常恢复后仍应保留 | 接口返回成功并提交后，重启应用不应丢失更新 |

需要特别注意：

- 原子性不等于“不会并发覆盖”。A和B的两个事务都可以各自完整提交，却仍发生Lost Update。
- 一致性不是数据库自动理解全部业务。数据库可以执行唯一约束、外键和CHECK；“离职员工不能再申请休假”等规则仍要由应用与数据库设计共同保证。
- 隔离级别越高通常等待和冲突越多，不应无依据地把所有事务改为最高级别。
- 持久性是数据库能力与运行配置共同提供的，不能用“接口返回200”代替数据库结果确认。

## 四、项目中设计事务的基本原则

### 原则1：边界对应一个完整用例

通常把事务放在Service的公开业务方法上，因为Service最清楚一次用例要完成哪些Mapper调用。

```java
@Transactional
public EmployeeDetailResponse update(
        Long id,
        EmployeeUpdateRequest request) {
    // 读取旧值、更新员工、写履历
}
```

Controller只负责HTTP输入输出，Mapper只负责一项数据库访问。若在Controller中随意扩大事务，连接和锁可能一直持有到响应组装结束；若每个Mapper各自提交，又无法保证多步业务共同回滚。

### 原则2：事务尽量短

事务中只保留保持一致性所必需的数据库操作和快速业务判断。下面这些工作通常不应放在持锁区间内：

- 等待用户再次输入；
- 调用响应时间不可控的外部API；
- 发送大文件或生成耗时报表；
- 与本次数据一致性无关的批量处理；
- 可以在事务外完成的格式转换。

事务越长，占用数据库连接和锁的时间越长，其他请求等待、超时和死锁的概率越高。

### 原则3：失败必须能够离开事务方法

如果捕获异常后只记录日志并正常返回，Spring会把方法视为正常结束，事务可能提交。

```java
try {
    changeLogMapper.insert(changeLog);
} catch (RuntimeException ex) {
    log.error("履历写入失败", ex);
    // 错误：吞掉异常后方法继续正常返回
}
```

需要回滚时，应让符合规则的异常继续抛出，或转换为新的运行时异常后抛出。不要用空的 `catch` 隐藏数据库失败。

### 原则4：用数据库约束守住最终底线

“先SELECT确认没有重复，再INSERT”存在并发空隙：两个请求可能同时确认不存在，然后一起插入。唯一性应由 `UNIQUE` 约束最终保证，应用的事前检查只用于提供更友好的提示。

### 原则5：读后再写时必须考虑并发

下面这种逻辑如果没有原子SQL、version或锁，并发时可能出错：

```text
读取剩余天数10
  → Java判断可以扣除7
  → 写回3
```

两个请求都读到10并分别扣除7时，单次判断都正确，组合结果却可能错误。看到“先读、计算、再写”时，应主动调查并发控制。

### 原则6：所有路径保持一致的加锁顺序

若业务必须更新多名员工或多张表，所有代码路径都按相同顺序访问。例如始终先按较小员工ID加锁，再处理较大ID。相反顺序会增加死锁概率。

## 五、Spring的 `@Transactional`

### 1. 这个注解由谁处理

需要导入Spring事务注解：

```java
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
```

`@Transactional` 本身不执行SQL，也不是MyBatis注解。Spring读取它的元数据，通过代理在方法调用前开启或加入事务，在方法结束后决定提交或回滚。

因此要同时满足：

- 对象由Spring容器管理；
- 调用经过Spring代理；
- 数据访问使用同一事务管理体系中的数据源；
- 异常满足回滚规则并离开事务方法。

同一对象内部用 `this.otherMethod()` 调用另一个带注解的方法，通常不会再次经过代理，不能假定内部方法声明的新事务属性一定生效。

### 2. 默认行为

直接写 `@Transactional` 时，重要默认值是：

| 属性 | 默认行为 | 项目含义 |
| --- | --- | --- |
| `propagation` | `REQUIRED` | 有事务就加入，没有就新建 |
| `isolation` | `DEFAULT` | 使用底层数据库默认隔离级别 |
| `readOnly` | `false` | 事务不是只读提示 |
| `timeout` | 事务系统默认值 | 不是HTTP超时，也不代替SQL优化 |
| 回滚规则 | `RuntimeException`和`Error`回滚 | checked exception默认不触发回滚 |

### 3. 常用属性的一行示例

```java
@Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.DEFAULT,
        readOnly = false,
        timeout = 10,
        rollbackFor = Exception.class,
        noRollbackFor = BusinessNoticeException.class)
```

- `propagation`：方法与已有事务怎样组合。
- `isolation`：新事务采用的隔离级别；加入已有事务时不能把外层事务随意改成另一隔离级别。
- `readOnly`：给事务基础设施的只读提示，不是权限控制，也不保证数据库必然拒绝写SQL。
- `timeout`：事务超时秒数；具体支持和异常行为取决于事务管理器与数据库驱动。
- `rollbackFor`：指定原本可能不回滚、但业务要求回滚的异常类型。
- `noRollbackFor`：指定不触发回滚的异常类型，应有明确规格，不能为“让测试通过”而使用。

上面只为集中展示属性，不代表所有项目都应该显式填写。属性越多，越需要说明业务依据。第13章使用默认 `REQUIRED` 和默认运行时异常回滚即可。

### 4. 常见传播方式

| 传播方式 | 含义 | 使用提醒 |
| --- | --- | --- |
| `REQUIRED` | 加入已有事务；没有则创建 | 绝大多数Service写入用例的起点 |
| `REQUIRES_NEW` | 暂停外层事务并创建独立事务 | 内层提交后不会随外层一起回滚；还会额外占用连接，不能随意用于“强制保存日志” |
| `NESTED` | 在支持时用保存点形成嵌套范围 | 是否可用取决于事务管理器与驱动，不等于独立新事务 |

传播方式解决“方法调用之间怎样共享事务”，隔离级别解决“并发事务彼此能看到什么”。两者不要混淆。

## 六、并发读取会出现什么现象

下面用事务A和事务B表示两个几乎同时到达的请求。

### 1. 脏读 Dirty Read

```text
A：把部门改为Development，尚未提交
B：读到了Development
A：回滚
```

B读到了一个从未正式提交、后来又被撤销的值。这就是脏读。

### 2. 不可重复读 Non-repeatable Read

```text
A：第一次读取部门，得到Sales
B：修改为Development并提交
A：同一事务第二次读取，得到Development
```

A在同一事务中两次读取同一行却得到不同结果。

### 3. 幻读 Phantom Read

```text
A：查询Development部门共有10人
B：新增一名Development员工并提交
A：用相同条件再次查询，得到11人
```

变化来自“符合范围条件的行集合”增加或减少，而不只是某一行字段改变，因此称为幻读。

### 4. 丢失更新 Lost Update

```text
A、B都读取 version=3 的员工
A：修改部门并提交
B：基于旧画面提交完整员工数据
结果：B覆盖了A的修改
```

丢失更新不等同于前三种读取现象。即使每个事务都能提交和回滚，也必须用原子UPDATE、乐观锁或悲观锁等手段阻止错误覆盖。

## 七、数据库隔离级别

隔离级别是在“并发性”和“读到稳定结果”之间选择平衡。下表先按SQL标准理解：

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 典型理解 |
| --- | --- | --- | --- | --- |
| `READ_UNCOMMITTED` | 可能 | 可能 | 可能 | 几乎不隔离读取，不适合作为业务系统默认选择 |
| `READ_COMMITTED` | 防止 | 可能 | 可能 | 每次查询读取当时已提交的数据 |
| `REPEATABLE_READ` | 防止 | 防止 | 标准模型中仍可能 | 同一事务的重复快照读保持一致 |
| `SERIALIZABLE` | 防止 | 防止 | 防止 | 尽量像事务串行执行，等待和冲突成本最高 |

### 1. MySQL InnoDB的默认行为

InnoDB支持上述四个级别，默认是 `REPEATABLE READ`。在该级别中，同一事务的普通一致性读取通常使用第一次读取建立的快照，因此多次普通 `SELECT` 可以看到一致的结果。

可以在数据库客户端确认当前连接的级别：

```sql
SELECT @@SESSION.transaction_isolation;
```

不要只因为看到并发问题就全局修改数据库隔离级别。先确认：

1. 出错的是普通读取、锁定读取还是写入；
2. 是否缺少唯一约束、正确索引或version条件；
3. 事务是否过长；
4. 规格要求的是快照一致性、阻止覆盖，还是阻止新增范围记录。

### 2. MySQL实现和标准表格为什么看起来不完全一样

SQL标准表格用于建立共同概念；MySQL InnoDB还会结合MVCC、记录锁、间隙锁和next-key lock实现具体行为。例如在默认 `REPEATABLE READ` 中：

- 普通 `SELECT` 通常是非锁定的一致性读取，读取事务快照；
- `SELECT ... FOR UPDATE`、`UPDATE`、`DELETE` 属于当前读或写操作，会依据搜索条件与索引加锁；
- 唯一索引的唯一等值条件通常只锁目标索引记录；
- 范围条件可能锁扫描到的索引范围与间隙，防止其他事务在范围内插入。

因此不要只背“某级别防不防幻读”。Review真实项目时必须同时查看SQL、索引、隔离级别和读取方式。

### 3. Spring中怎样指定隔离级别

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void executeBusiness() {
    // 数据库操作
}
```

这表示新建事务时请求使用 `READ_COMMITTED`。主线没有改变默认级别的业务规格，所以应保留 `Isolation.DEFAULT`。提高隔离级别不是免费的“增强安全”按钮，降低隔离级别也不能只凭性能猜测；两者都需要并发场景、测试和数据库证据。

## 八、MVCC：为什么普通SELECT不一定阻塞写入

MVCC是多版本并发控制。可以把它理解为：数据库保留足够的历史版本信息，让普通读取在合适情况下看到事务自己的快照，而不是每次都锁住最新行等待写入结束。

MVCC带来的重要认识是：

- 普通 `SELECT` 不加 `FOR UPDATE` 时，通常不会因为要读一行就长期阻塞写入；
- “读取没有阻塞”不代表写入不需要锁；
- 快照读看到的可能不是数据库此刻最新已提交状态；
- `UPDATE`和锁定读需要处理当前记录，行为与普通快照读不同；
- MVCC不自动解决两个用户基于旧画面提交造成的Lost Update。

需要“读取最新值并马上据此写入”时，应根据业务选择原子UPDATE或锁定读，而不是假定普通SELECT已经锁住数据。

## 九、锁的基本概念

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

## 十、四类常用并发控制方案

### 方案1：唯一约束——防止最终重复

```sql
ALTER TABLE employees
    ADD CONSTRAINT uk_employees_email UNIQUE (email);
```

适合“email不能重复”这类最终不变量。即使两个请求同时通过事前检查，数据库也只允许一个冲突值成功。Service需要把重复键异常转换为明确的业务冲突响应。

唯一约束不负责合并两次更新，也不替代多SQL事务。

### 方案2：原子条件UPDATE——在一条SQL中判断并修改

休假余额扣减可以避免“先查再改”的并发空隙：

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

实验列：

```sql
ALTER TABLE employees
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

读取详情时把 `version` 返回给前端；更新请求再带回原version：

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

并发过程是：

```text
A读取version=3                 B读取version=3
A以WHERE version=3更新成功
数据库version变为4            B以WHERE version=3更新0行
A返回成功                      B返回409 Conflict
```

不要在B失败后静默重试完整UPDATE，否则仍可能覆盖A的变更。正确做法通常是让用户读取最新数据、比较差异并重新决定。

如果只做附录实验，结束后恢复主线表结构：

```sql
ALTER TABLE employees DROP COLUMN version;
```

### 方案4：悲观锁——先锁定最新记录再处理

冲突概率高、必须读取最新状态后立即计算并写入时，可以在同一事务中使用锁定读：

```sql
SELECT id, name, email, department, status
FROM employees
WHERE id = #{id}
FOR UPDATE;
```

```java
@Transactional
public void changeDepartment(Long id, String department) {
    Employee employee = employeeMapper.findByIdForUpdate(id);
    if (employee == null) {
        throw new ResourceNotFoundException("员工不存在");
    }
    employeeMapper.updateDepartment(id, department);
}
```

`FOR UPDATE` 和后续UPDATE必须处于同一事务。若查询方法先结束并提交，再执行更新，锁已经释放，设计目标没有实现。

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

## 十一、锁等待与死锁

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

## 十二、两个会话怎样验证并发现象

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

1. A和B都读取同一员工及同一version；
2. A用旧version更新，断言影响1行；
3. B仍用相同旧version更新，断言影响0行；
4. 查询最终记录，确认保留A结果；
5. 确认接口层把B转换为409，而不是200或500；
6. 实验结束后删除临时version列与实验Mapper方法，运行第13章原有测试。

## 十三、事务和并发控制的常见误区

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

## 十四、Review与故障调查清单

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

## 十五、练习任务

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

## 十六、本附录完成标准

完成本附录后，不要求把所有锁机制加入Employee Management主线。你需要能够根据业务问题选择最小且可验证的机制：用事务保护多步业务原子性，用约束保护最终不变量，用原子SQL、乐观锁或悲观锁处理具体并发竞争，并用隔离级别、索引、锁范围和失败处理解释运行结果。

进一步排查事务未回滚、锁等待或数据库异常时，可结合[B07 Spring Boot常见问题与排查索引](B07_troubleshooting_review_index.md)确定调查入口。

## 十七、官方参考资料

- [Spring Framework：使用 `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [MySQL 8.4：InnoDB事务模型](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-model.html)
- [MySQL 8.4：InnoDB事务隔离级别](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)
- [MySQL 8.4：InnoDB锁](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking.html)
- [MySQL 8.4：InnoDB死锁](https://dev.mysql.com/doc/refman/8.4/en/innodb-deadlocks.html)
