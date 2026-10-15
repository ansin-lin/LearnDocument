# A13-2 隔离级别与MVCC

完成[第13章：用事务保证多步写入一致](../mainline/13_transaction_and_consistency.md)后，再看多个事务同时读取数据时能观察到什么。本附录以MySQL 8的InnoDB为例；其他数据库的默认隔离级别和具体实现可能不同。Spring的事务边界与传播方式见[A13-1](A13_01_transaction_spring_mechanisms.md)，防止旧数据覆盖、锁等待和死锁的处理见[A13-3](A13_03_concurrent_updates_locks.md)。本附录不修改员工管理主线工程。

学完后应能区分脏读、不可重复读、幻读和丢失更新，并结合隔离级别、快照读与当前读解释观察到的结果。

## 一、并发读取会出现什么现象

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

## 二、数据库隔离级别

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

## 三、MVCC：为什么普通SELECT不一定阻塞写入

MVCC是多版本并发控制。可以把它理解为：数据库保留足够的历史版本信息，让普通读取在合适情况下看到事务自己的快照，而不是每次都锁住最新行等待写入结束。

MVCC带来的重要认识是：

- 普通 `SELECT` 不加 `FOR UPDATE` 时，通常不会因为要读一行就长期阻塞写入；
- “读取没有阻塞”不代表写入不需要锁；
- 快照读看到的可能不是数据库此刻最新已提交状态；
- `UPDATE`和锁定读需要处理当前记录，行为与普通快照读不同；
- MVCC不自动解决两个用户基于旧画面提交造成的Lost Update。

需要“读取最新值并马上据此写入”时，应根据业务选择原子UPDATE或锁定读，而不是假定普通SELECT已经锁住数据。

## 四、检查与练习

在个人练习用的MySQL连接执行 `SELECT @@SESSION.transaction_isolation;`，记录实际级别。再分别判断：同一事务两次普通SELECT看到不同值、两个用户基于旧画面相互覆盖，是否属于同一种问题；说明为什么只提高隔离级别不一定能解决后者。不要在共享库或生产库改变隔离级别。

## 五、官方参考资料

- [MySQL 8.4：InnoDB事务模型](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-model.html)
- [MySQL 8.4：InnoDB事务隔离级别](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)
