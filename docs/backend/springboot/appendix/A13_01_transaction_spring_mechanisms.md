# A13-1 事务原理与Spring事务机制

完成[第13章：用事务保证多步写入一致](../mainline/13_transaction_and_consistency.md)后，用本附录深入理解事务边界、ACID、Spring代理、默认规则和传播方式。第13章的员工更新与履历写入仍是可运行主线；这里的属性示例是独立说明，不要求改动主线工程。并发读取与更新另见[A13-2 隔离级别与MVCC](A13_02_isolation_mvcc.md)和[A13-3 并发更新与锁](A13_03_concurrent_updates_locks.md)。

学完后应能检查一次业务操作的事务边界，解释提交/回滚的依据，并判断异常捕获或传播设置是否可能破坏共同成败。

## 一、先理解事务是什么

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

## 二、事务的ACID特点

ACID不是四个互不相关的口号，而是事务能够可靠处理数据变化的四个观察角度。

| 特点 | 含义 | 员工管理中的例子 |
| --- | --- | --- |
| Atomicity，原子性 | 一个事务中的操作要么全部完成，要么全部撤销 | 员工更新和履历写入不能只成功一个 |
| Consistency，一致性 | 事务前后都满足业务规则、数据库约束和数据关系 | email仍唯一；“更新员工必须写履历”由同一事务保证；status允许值仍需应用规则或数据库约束保护 |
| Isolation，隔离性 | 并发事务互相影响的程度受到控制 | A修改员工时，B能读到什么、是否必须等待，由隔离和锁决定 |
| Durability，持久性 | 已提交的结果在数据库正常恢复后仍应保留 | 接口返回成功并提交后，重启应用不应丢失更新 |

需要特别注意：

- 原子性不等于“不会并发覆盖”。A和B的两个事务都可以各自完整提交，却仍发生Lost Update。
- 一致性不是数据库自动理解全部业务。数据库可以执行唯一约束、外键和CHECK；“离职员工不能再申请休假”等规则仍要由应用与数据库设计共同保证。
- 隔离级别越高通常等待和冲突越多，不应无依据地把所有事务改为最高级别。
- 持久性是数据库能力与运行配置共同提供的，不能用“接口返回200”代替数据库结果确认。

## 三、项目中设计事务的基本原则

### 原则1：边界对应一个完整用例

通常把事务放在Service的公开业务方法上，因为Service最清楚一次用例要完成哪些Mapper调用。

```java
@Transactional
public EmployeeResponse update(
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

## 四、Spring的 `@Transactional`

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
        noRollbackFor = IllegalArgumentException.class)
```

- `propagation`：方法与已有事务怎样组合。
- `isolation`：新事务采用的隔离级别；加入已有事务时不能把外层事务随意改成另一隔离级别。
- `readOnly`：给事务基础设施的只读提示，不是权限控制，也不保证数据库必然拒绝写SQL。
- `timeout`：事务超时秒数；具体支持和异常行为取决于事务管理器与数据库驱动。
- `rollbackFor`：指定原本可能不回滚、但业务要求回滚的异常类型。
- `noRollbackFor`：指定不触发回滚的异常类型，应有明确规格，不能为“让测试通过”而使用。

上面只为集中展示属性，不是第13章可以直接复制的注解配置；例如 `noRollbackFor = IllegalArgumentException.class` 会改变默认回滚规则，只有明确要求“抛出这个异常仍要提交”时才可考虑。第13章使用默认 `REQUIRED` 和默认运行时异常回滚即可。

### 4. 常见传播方式

| 传播方式 | 含义 | 使用提醒 |
| --- | --- | --- |
| `REQUIRED` | 加入已有事务；没有则创建 | 绝大多数Service写入用例的起点 |
| `REQUIRES_NEW` | 暂停外层事务并创建独立事务 | 内层提交后不会随外层一起回滚；还会额外占用连接，不能随意用于“强制保存日志” |
| `NESTED` | 在支持时用保存点形成嵌套范围 | 是否可用取决于事务管理器与驱动，不等于独立新事务 |

传播方式解决“方法调用之间怎样共享事务”，隔离级别解决“并发事务彼此能看到什么”。两者不要混淆。

## 五、检查与练习

对照第13章的 `EmployeeServiceImpl.update()`，指出事务从哪里开始、哪些Mapper调用在同一边界中，以及履历INSERT抛出运行时异常时员工UPDATE为何回滚。再检查“捕获异常后只记录日志并正常返回”的改动，写出需要核对的HTTP和数据库证据；不要只凭 `catch` 推断最终结果。

## 六、官方参考资料

- [Spring Framework：使用 `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Spring Framework：回滚规则](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
- [Spring Framework：事务传播](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)
