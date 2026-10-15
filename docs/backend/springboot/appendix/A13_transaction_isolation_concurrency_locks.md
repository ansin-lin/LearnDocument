# A13 事务与并发专题导航

原A13附录已按知识体系拆分。完成[第13章：用事务保证多步写入一致](../mainline/13_transaction_and_consistency.md)后，按需要查阅：

1. [A13-1 事务原理与Spring事务机制](A13_01_transaction_spring_mechanisms.md)：事务边界、ACID、`@Transactional`、默认规则和传播方式。
2. [A13-2 隔离级别与MVCC](A13_02_isolation_mvcc.md)：并发读取现象、隔离级别、快照读与当前读。
3. [A13-3 并发更新与锁](A13_03_concurrent_updates_locks.md)：约束、原子UPDATE、乐观锁、悲观锁、锁等待、死锁和双会话实验。

第13章的员工更新与履历写入仍是主线；三个附录是独立扩展，不要求一次性全部加入主线工程。
