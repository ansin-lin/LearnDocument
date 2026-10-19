# A13-3 用MVCC解释读写并发：隔离级别与快照

**进阶机制篇，建议最后学习：** 先完成[A13-1 事务](A13_01_transaction_spring_mechanisms.md)及[A13-2 并发更新与锁](A13_02_concurrent_updates_locks.md)，再用本篇解释观察到的现象。以MySQL 8.0 InnoDB为主，不直接照搬到其他数据库；不修改员工管理主线工程。

## 一、先带着已完成的实验提出三个问题

学习本篇前，学员应已在[A13-2](A13_02_concurrent_updates_locks.md)亲手观察：A的`FOR UPDATE`会让B的冲突更新等待；B拿旧`version`更新影响0行；事务提交后锁会释放。现在再问：

1. A持有排他锁时，B为什么可能仍能普通`SELECT`读取数据？
2. 同一事务内连续两次普通`SELECT`，为什么有时读到相同值、有时读到不同值？
3. `REPEATABLE READ`会不会自动阻止用户停留十分钟后提交旧表单？

这些问题分别需要**MVCC与快照读、隔离级别、业务并发版本校验**来回答。重点不是把四个隔离级别当成四种“锁”，而是观察不同事务在什么时间点看见了哪些数据。

## 二、并发读取现象：脏读、不可重复读、幻读、丢失更新

Web服务器可用不同线程同时处理多个HTTP请求，这些请求可能借用连接池中的不同MySQL连接；各连接上的事务会交错执行。并发并不要求两段Java代码在同一纳秒执行。例如两名管理员打开同一员工编辑画面：A、B先后读取旧数据，A提交修改，B的画面仍保留旧值，几分钟后B提交就可能覆盖A。普通SELECT与UPDATE之间的时间窗口可持续到用户点击保存，单个事务的原子性不能自动识别“画面是旧的”。

下面用事务A和事务B表示两个独立数据库连接上的操作。四种现象的区别是“读到未提交值”“同一行重复读变化”“相同条件的行集合变化”以及“旧值覆盖新值”；最后一种不是前三种的同义词。

### 1. 脏读 Dirty Read

```text
A：把部门改为Development，尚未提交
B：读到了Development
A：回滚
```

B读到了一个从未正式提交、后来又被撤销的值。这就是脏读。

原因是读取允许看到别的事务尚未提交的修改。若据此向员工发送通知或生成报表，通知可能指向后来回滚的部门；MySQL InnoDB的`READ_COMMITTED`及更高隔离级别避免这种脏读。它与“已经提交后第二次读取变了”不同。

### 2. 不可重复读 Non-repeatable Read

```text
A：第一次读取部门，得到Sales
B：修改为Development并提交
A：同一事务第二次读取，得到Development
```

A在同一事务中两次读取同一行却得到不同结果。

原因是第二次语句采用新的可见性时点，B的提交落在两次读取之间。若A先按旧部门计算再按新部门更新，逻辑可能前后不一致；需要一致视图时应考虑实际数据库的`REPEATABLE_READ`实现或明确锁定，不能只把两条SELECT写在同一Java方法里。

### 3. 幻读 Phantom Read

```text
A：查询Development部门共有10人
B：新增一名Development员工并提交
A：用相同条件再次查询，得到11人
```

变化来自“符合范围条件的行集合”增加或减少，而不只是某一行字段改变，因此称为幻读。

例如A两次统计Development人数得到不同总数，后续按第一次数量分配名额会有偏差。SQL标准把相同条件查询的新增/删除行视为幻读；具体数据库在`REPEATABLE_READ`下的快照与锁定读实现可能更强或不同，不能仅看隔离级别名称推断结果。

### 4. 丢失更新 Lost Update

```text
A、B都读取 version=3 的员工
A：修改部门并提交
B：基于旧画面提交完整员工数据
结果：B覆盖了A的修改
```

丢失更新不等同于前三种读取现象。即使每个事务都能提交和回滚，也必须用原子UPDATE、乐观锁或悲观锁等手段阻止错误覆盖。

问题根源通常是B提交完整旧画面对象时，UPDATE只按`id`定位，没有比较读取时的`version`或旧值。提高隔离级别不一定覆盖“请求之间隔着几分钟人工编辑”的窗口；[A13-2](A13_02_concurrent_updates_locks.md)用version、条件UPDATE或锁处理具体写入竞争。

## 三、MVCC与快照读、当前读

MVCC是多版本并发控制。可以把它理解为：数据库保留足够的历史版本信息，让普通读取在合适情况下看到事务自己的快照，而不是每次都锁住最新行等待写入结束。

以MySQL 8.0 InnoDB为限，记录中有与最近修改事务、旧版本链相关的隐藏信息；事务更新一行时，旧值可由Undo Log（回滚日志）记录关联。Undo Log既帮助撤销尚未提交的改动，也让一致性读取必要时重建旧版本。Read View（读取视图）记录创建视图时活跃事务等可见性信息；普通SELECT判断某版本的修改事务在该视图下是否可见，不可见就沿旧版本链寻找可见版本。这里是机制示意，不是说每次SELECT复制整张表，也不是把Undo Log当作长期业务履历表。

在InnoDB的`READ COMMITTED`下，普通一致性读取通常**每条语句**建立新的Read View，因此第二次SELECT可能看到B刚提交的部门；在`REPEATABLE READ`下，事务内第一次一致性读取建立的视图通常供后续一致性读取复用，所以仍可能看到旧部门。自己在同一事务中作出的修改可见；`SELECT ... FOR UPDATE`、`FOR SHARE`和UPDATE/DELETE不简单沿用普通快照读结果。MVCC不消除写锁，也不能凭“有历史版本”就保证没有旧画面覆盖。

MVCC带来的重要认识是：

- 普通 `SELECT` 不加 `FOR UPDATE` 时，通常不会因为要读一行就长期阻塞写入；
- “读取没有阻塞”不代表写入不需要锁；
- 快照读看到的可能不是数据库此刻最新已提交状态；
- `UPDATE`和锁定读需要处理当前记录，行为与普通快照读不同；
- MVCC不自动解决两个用户基于旧画面提交造成的Lost Update。

| 读取方式（MySQL 8.0 InnoDB） | 常见SQL | 本实验应怎样理解 |
| --- | --- | --- |
| 普通快照读 | `SELECT id, department FROM employees WHERE id=1001` | 在适用隔离级别下按Read View找可见版本 |
| 当前锁定读 | `SELECT ... FOR SHARE` | 对当前满足条件的记录请求共享锁 |
| 当前锁定读 | `SELECT ... FOR UPDATE` | 读取当前可锁定记录并请求排他锁 |
| 写操作 | `UPDATE`、`DELETE` | 根据当前记录状态写入并取得必要的锁 |

实际锁范围还受索引、WHERE、隔离级别和执行计划影响，详见[A13-2](A13_02_concurrent_updates_locks.md)。这张表不代表PostgreSQL、Oracle或SQL Server使用完全相同的Undo Log、Read View或锁机制。

需要“读取最新值并马上据此写入”时，应根据业务选择原子UPDATE或锁定读，而不是假定普通SELECT已经锁住数据。

### 两个连接先对照READ COMMITTED与REPEATABLE READ

以下只在**个人专用MySQL 8.0 InnoDB练习库**操作，两个客户端窗口分别是独立连接A、B。确认`employees.id=1001`存在、先记录原`department`，实验期间无人修改此行。下面用`Sales`作为统一起点；若原值不同，实验后必须按记录恢复，不在共享库运行。每轮开始前确保两连接都已`COMMIT`或`ROLLBACK`，并在B上恢复起点：

```sql
UPDATE employees SET department = 'Sales' WHERE id = 1001;
COMMIT;
```

**RC轮：** A在事务开始前执行`SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;`，再执行：

```sql
START TRANSACTION;
SELECT id, department FROM employees WHERE id = 1001; -- Sales
```

保持A事务打开。B执行：

```sql
START TRANSACTION;
UPDATE employees SET department = 'Development' WHERE id = 1001;
COMMIT;
```

A再次执行相同的普通SELECT，预期`Development`，随后`ROLLBACK;`结束A。原因是本轮普通读取按语句取得新可见性视图；A没有撤销B已提交的修改。

**RR轮：** B先把部门恢复成`Sales`并提交；A确认上一轮事务已结束，执行`SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;`，再`START TRANSACTION;`并第一次普通SELECT取得`Sales`。B在另一连接把部门改为`Development`并`COMMIT;`。A第二次**普通SELECT**通常仍得`Sales`，然后`ROLLBACK;`。这里比较的都是普通SELECT；不要把`FOR UPDATE`混进RC/RR对照。

最后在B连接把部门恢复为**实验前记录的原值**并提交，查询核对；A、B均无未结束事务，必要时把A会话隔离设置恢复为实验前记录的值，或关闭两个实验连接。若结果不符，先查表引擎、实际`@@SESSION.transaction_isolation`、是否同一连接、第一次读取和B提交的顺序、是否用了锁定读，不要直接改教材预期。此实验说明MySQL InnoDB的两种典型读视图时机，不是所有数据库RC/RR的通用脚本。

### 两个连接观察快照读与当前读

下面是**个人MySQL 8.0练习库的独立实验**，不是第13章主线改修。先确认 `employees` 中 `id=1001` 存在、记录原部门，并保证无人同时修改这条练习数据。示例假设原部门为 `Sales`；若不是，先换一条原部门为Sales的专用练习记录，不在共享库或生产库执行。

连接A先确认级别，然后开始事务并进行第一次普通读取：

```sql
SELECT @@SESSION.transaction_isolation;
START TRANSACTION;
SELECT id, department FROM employees WHERE id = 1001;
```

本实验要求结果为 `REPEATABLE-READ` 且第一次读到 `Sales`。如果当前连接不是这个级别，应先按项目允许的练习设置调整，或者停止实验；不要把其他级别的结果当成下方预期。连接B独立执行：

```sql
START TRANSACTION;
UPDATE employees SET department = 'Development' WHERE id = 1001;
COMMIT;
```

回到仍未结束的连接A：

```sql
SELECT id, department FROM employees WHERE id = 1001;
SELECT id, department FROM employees WHERE id = 1001 FOR UPDATE;
ROLLBACK;
```

在本实验前提下，A第二次**普通SELECT**仍从此前建立的快照读到 `Sales`；随后的 `FOR UPDATE` 是锁定当前读，通常看到B已提交的 `Development`。A用 `ROLLBACK` 结束事务并释放锁，但B的 `COMMIT` 不会被A撤销。若原部门确实是 `Sales`，最后仅在个人练习库执行 `UPDATE employees SET department = 'Sales' WHERE id = 1001;` 并独立查询确认恢复；原值不同则按记录的原值恢复。关闭两个实验连接。这个对照说明“同一事务内的普通读取稳定”与“当前读要处理最新行”不是同一件事，也不能据此认为旧画面更新已经安全。

## 四、隔离级别与不同数据库的差异

### 1. SQL标准的四种隔离级别

隔离级别是在“并发性”和“读到稳定结果”之间选择平衡。下表先按SQL标准理解：

Spring注解的 `isolation` 有五个可填写值：`Isolation.DEFAULT` 表示沿用数据源默认值；其余四个值与下表四种隔离级别一一对应。`DEFAULT` **不是第五种数据库隔离级别**。本课程的MySQL InnoDB默认通常为 `REPEATABLE READ`，但项目应查询当前连接确认，不能把Spring的 `DEFAULT` 固定翻译成任何一种级别。

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 典型理解 |
| --- | --- | --- | --- | --- |
| `READ_UNCOMMITTED` | 可能 | 可能 | 可能 | 几乎不隔离读取，不适合作为业务系统默认选择 |
| `READ_COMMITTED` | 防止 | 可能 | 可能 | 每次查询读取当时已提交的数据 |
| `REPEATABLE_READ` | 防止 | 防止 | 标准模型中仍可能 | 同一事务的重复快照读保持一致 |
| `SERIALIZABLE` | 防止 | 防止 | 防止 | 尽量像事务串行执行，等待和冲突成本最高 |

四级是SQL标准的概念框架，不是所有产品的实现承诺。`READ_UNCOMMITTED`可能读到后来回滚的值，业务系统通常不选；`READ_COMMITTED`每条普通查询可看到新近提交的结果，适合不要求跨语句稳定视图的常规读；`REPEATABLE_READ`强调同一事务重复读的稳定性，但具体实现与幻读处理因产品而异；`SERIALIZABLE`提供更强隔离，也会增加等待、冲突或重试成本。邮箱唯一性仍靠`UNIQUE`，余额不扣负靠条件UPDATE，旧画面覆盖可用version；不能把最高隔离级别当成所有业务的默认修复。

### 2. 不同数据库默认隔离级别与同名差异

| 数据库与本文核对范围 | 官方通常默认隔离级别 | 不能机械类推的实现点 |
| --- | --- | --- |
| MySQL 8.0 InnoDB | `REPEATABLE READ` | 普通一致性SELECT与`FOR UPDATE`等当前读不同 |
| MariaDB 11.x InnoDB | `REPEATABLE READ` | 与MySQL有相似概念，但变量名、版本和锁细节须分别核对 |
| PostgreSQL当前文档 | `READ COMMITTED` | `READ UNCOMMITTED`按`READ COMMITTED`运行；其`REPEATABLE READ`通常还避免标准表允许的幻读 |
| Oracle Database 19c | `READ COMMITTED` | 语句级一致性读取；不直接照搬MySQL的四级设置和Gap Lock解释 |
| SQL Server 2022/当前文档 | `READ COMMITTED` | `READ_COMMITTED_SNAPSHOT`数据库选项为OFF时常用共享锁，ON时读使用行版本；Azure SQL等托管环境可能默认ON |

这些是指定产品文档的默认配置，不是任意项目的实际设置。连接池、Spring事务注解、会话初始化SQL、数据库管理员配置或托管服务默认值都可能改变应用连接的行为。PostgreSQL的`REPEATABLE READ`可在并发更新时产生序列化失败，不能套用MySQL的Next-key Lock来解释；SQL Server的`READ_COMMITTED_SNAPSHOT`是`READ COMMITTED`的数据库选项，独立的`SNAPSHOT`隔离还需另外启用，不是同一个开关。

### 3. 如何确认项目实际隔离级别

数据库客户端只代表**那条客户端连接**。调查项目时还应检查应用数据源、连接池初始化语句及`@Transactional(isolation = ...)`，必要时在受控开发环境的应用连接上查询：

```sql
-- MySQL 8.0，当前会话
SELECT @@SESSION.transaction_isolation;

-- PostgreSQL，当前会话
SHOW transaction_isolation;
```

MariaDB 11.1.1及之后通常可用`SELECT @@SESSION.transaction_isolation;`；较早版本使用`SELECT @@SESSION.tx_isolation;`，先查实际版本与系统变量。SQL Server可在有权限的会话上查询当前会话及所在数据库选项：

```sql
SELECT transaction_isolation_level
FROM sys.dm_exec_sessions
WHERE session_id = @@SPID;

SELECT name, is_read_committed_snapshot_on, snapshot_isolation_state_desc
FROM sys.databases
WHERE name = DB_NAME();
```

第一个结果代码`1/2/3/4/5`分别是ReadUncommitted/ReadCommitted/RepeatableRead/Serializable/Snapshot；第二个结果才说明`READ COMMITTED`用锁还是行版本。Oracle Database 19c官方文档说明默认`READ COMMITTED`，可用`ALTER SESSION SET ISOLATION_LEVEL = SERIALIZABLE`明确设置后再做行为实验；`SET TRANSACTION ISOLATION LEVEL ...`仅针对当前事务且须作为首条语句。Oracle没有与上述MySQL变量完全等价、可通用查询所有事务当前隔离行为的单条SQL，本教材不虚构一个；调查既存项目应核对数据源设置、会话初始化SQL、事务显式设置与实测结果。

### 4. MySQL InnoDB的默认行为

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

### 5. MySQL实现和SQL标准表格为什么不完全一样

SQL标准表格用于建立共同概念；MySQL InnoDB还会结合MVCC、记录锁、间隙锁和next-key lock实现具体行为。例如在默认 `REPEATABLE READ` 中：

- 普通 `SELECT` 通常是非锁定的一致性读取，读取事务快照；
- `SELECT ... FOR UPDATE`、`UPDATE`、`DELETE` 属于当前读或写操作，会依据搜索条件与索引加锁；
- 唯一索引的唯一等值条件通常只锁目标索引记录；
- 范围条件可能锁扫描到的索引范围与间隙，防止其他事务在范围内插入。

因此不要只背“某级别防不防幻读”。Review真实项目时必须同时查看SQL、索引、隔离级别和读取方式。

### 6. Spring中怎样指定隔离级别

```java
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.READ_COMMITTED)
public void executeBusiness() {
    // 数据库操作
}
```

上方是**Service方法写法片段**，两个 `import` 应放在Java文件顶部，不能原样写进方法体。这表示新建事务时请求使用 `READ_COMMITTED`；若当前方法只是加入已有事务，内层声明通常不会改变已建立事务的隔离级别。主线没有改变默认级别的业务规格，所以应保留 `Isolation.DEFAULT`。提高隔离级别不是免费的“增强安全”按钮，降低隔离级别也不能只凭性能猜测；两者都需要并发场景、测试和数据库证据。

## 五、项目中怎样选择和调查

| 现场任务 | 先确认什么 | 不能只靠什么 |
| --- | --- | --- |
| 员工编辑 | 是否旧画面覆盖、UPDATE是否比较version | 只提高隔离级别；见[A13-2](A13_02_concurrent_updates_locks.md) |
| 订单查询/统计 | 多次查询是否必须属于同一时间点 | 只因“有多条SELECT”就设SERIALIZABLE |
| 夜间Batch与在线更新并行 | Batch批次边界、读取视图、写入范围和重试条件 | 一个无限大的事务 |
| 后台审批 | 是否需要读取最新状态并原子迁移 | 普通快照SELECT后无条件UPDATE |
| 列表和COUNT | 是否要求同一筛选与权限范围、是否允许观察到不同提交时点 | 假定两条RC查询必然看到同一时点 |

调查顺序是：记录数据库产品/版本和实际应用连接隔离级别 → 画出A、B事务交错顺序 → 标记每条SQL是普通快照读、锁定读还是写入 → 写明业务不变量 → 决定是否需要一致视图、约束、条件UPDATE、version或锁 → 用两个连接及JUnit/接口证据复核。提高隔离级别会改变等待和冲突，不能取代对问题类型的判断。

## 六、重要概念、Review与完成标准

| 容易混淆 | 区别 |
| --- | --- |
| 快照读与当前读 | 前者按可见版本读取；后者面向当前记录并可能加锁，不保证与先前快照相同 |
| 不可重复读与丢失更新 | 前者是重复读取观察值变化；后者是旧输入写回覆盖新提交 |
| `Isolation.DEFAULT`与`REPEATABLE_READ` | 前者沿用应用连接默认设置；后者是明确指定的级别 |
| 事务隔离与业务并发规则 | 隔离控制数据库事务可见性；旧画面、唯一性、余额等仍需具体控制 |

Review时检查数据库和连接配置是否有证据、SQL读类型是否标对、是否把MySQL特性套到其他产品、是否混淆普通SELECT与`FOR UPDATE`、是否给旧画面覆盖准备version或条件更新。完成标准：能画出RC与RR两次普通读取的时间线，解释Undo Log和Read View为何可能返回旧版本，说明MySQL与PostgreSQL默认值不同，并能用实际连接而非猜测确认本项目隔离设置。

## 七、检查与练习

在个人练习用的MySQL连接执行 `SELECT @@SESSION.transaction_isolation;`，记录实际级别。再分别判断：同一事务两次普通SELECT看到不同值、两个用户基于旧画面相互覆盖，是否属于同一种问题；说明为什么只提高隔离级别不一定能解决后者。不要在共享库或生产库改变隔离级别。

## 八、官方参考资料

- [MySQL 8.4：InnoDB事务模型](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-model.html)
- [MySQL 8.4：InnoDB事务隔离级别](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)
- [MySQL 8.0：一致性非锁定读](https://dev.mysql.com/doc/refman/8.0/en/innodb-consistent-read.html)
- [MySQL 8.4：Undo Log](https://dev.mysql.com/doc/refman/8.4/en/innodb-undo-logs.html)
- [PostgreSQL：事务隔离级别](https://www.postgresql.org/docs/current/transaction-iso.html)
- [MariaDB：事务隔离变量](https://mariadb.com/docs/server/server-management/variables-and-modes/server-system-variables)
- [Oracle Database 19c：一致性与隔离](https://docs.oracle.com/en/database/oracle/oracle-database/19/cncpt/data-concurrency-and-consistency.html)
- [SQL Server：隔离级别与行版本](https://learn.microsoft.com/en-us/sql/t-sql/statements/set-transaction-isolation-level-transact-sql?view=sql-server-ver17)
- [SQL Server：当前会话隔离级别](https://learn.microsoft.com/en-us/sql/relational-databases/system-dynamic-management-views/sys-dm-exec-sessions-transact-sql?view=sql-server-ver17)
