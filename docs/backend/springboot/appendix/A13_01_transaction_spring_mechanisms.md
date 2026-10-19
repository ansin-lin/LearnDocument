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

单条SQL通常作为一次数据库语句执行；但“员工UPDATE成功”和“履历INSERT成功”是两条SQL，自动提交时前一条不会因为后一条失败而自动撤销。是否把多条语句组成一个事务，由“员工变更必须留履历”的业务规格决定，而不是由SQL条数自动决定。事务负责这一组语句的共同成败，不自动识别两个编辑画面之间的旧值覆盖。

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

```text
Controller → Spring管理的EmployeeService代理 → 事务拦截器
          → 获取新事务或加入现有事务 → Service方法 → MyBatis Mapper
          → 按退出方式和回滚规则尝试COMMIT或ROLLBACK
```

事务管理器协调当前线程上的数据库连接；MyBatis Mapper必须使用Spring管理的同一数据源和事务体系，才能参与这次提交或回滚。直接`new EmployeeServiceImpl(...)`没有代理增强；同一对象里的`this.otherMethod()`也通常不重新经过代理。Service方法正常返回只表示拦截器将尝试提交，不保证数据库物理提交一定成功：提交阶段仍可能因约束、连接故障或rollback-only而失败。

因此要同时满足：

- 对象由Spring容器管理；
- 调用经过Spring代理；
- 数据访问使用同一事务管理体系中的数据源；
- 异常满足回滚规则并离开事务方法。

同一对象内部用 `this.otherMethod()` 调用另一个带注解的方法，通常不会再次经过代理，不能假定内部方法声明的新事务属性一定生效。

### 2. 属性、可填写值与默认行为

`@Transactional` 可以写在Spring管理的类或方法上；方法上的设置用于该方法。下面列出注解的全部属性，先看第13章实际用到的默认值，再把其余属性当作阅读既有项目代码时的查阅表。`Class<? extends Throwable>[]` 表示异常类数组，`String[]` 表示字符串数组。

| 属性 | 类型；可接受的值或写法 | 默认值 | 作用、适用场景与不适用情况 |
| --- | --- | --- | --- |
| `value` | `String`；事务管理器Bean名或限定符 | `""` | `transactionManager`的别名；多数据源需明确选择时用，单数据源不必写；两者不要给冲突值 |
| `transactionManager` | `String`；如`"employeeTransactionManager"` | `""` | 指定事务管理器；多个数据源各有事务管理器时使用，不会自动把两个数据库变成一个全局事务 |
| `label` | `String[]`；如`{"employee-update", "audit"}` | `{}` | 给支持标签的事务管理器提供分类信息；一般业务无需填写，不会自动建审计日志，也不是锁名称 |
| `propagation` | `Propagation`七种枚举值 | `REQUIRED` | 决定与调用方事务如何组合；员工更新＋履历共同成败用默认值；不为“性能优化”随意改`REQUIRES_NEW` |
| `isolation` | `Isolation`五种枚举值 | `DEFAULT` | 新事务请求的隔离级别；默认沿用连接/数据库设置，不是独立的第五种级别；无并发证据不随意升到`SERIALIZABLE` |
| `timeout` | `int`秒数；正整数或`-1` | `-1` | 新事务超时，`-1`沿用事务系统默认；用于限制有边界的数据库工作，不替代HTTP超时、锁等待超时或慢SQL优化 |
| `timeoutString` | `String`秒数字符串或占位符，如`"${app.tx-timeout:10}"` | `""` | 需要从配置读取秒数时使用；与`timeout`表达同一设置，不同时填冲突值 |
| `readOnly` | `boolean`；`true`/`false` | `false` | 查询事务可给只读提示；不是数据库写权限，不保证拒绝写SQL；更新员工与履历绝不能为了“查询快”设为true |
| `rollbackFor` | `Class<? extends Throwable>[]`；如`IOException.class` | `{}` | 受检异常也须让多步操作共同回滚时使用；不要所有方法机械写`Exception.class` |
| `rollbackForClassName` | `String[]`；异常全限定名或子串模式 | `{}` | 不能直接引用异常类时才谨慎使用；不是正则，宽泛模式可能意外命中，通常优先`rollbackFor` |
| `noRollbackFor` | `Class<? extends Throwable>[]`；如`BusinessNoticeException.class` | `{}` | 异常传给调用方但已做写入仍被规格允许提交时才使用；员工更新缺履历不适合 |
| `noRollbackForClassName` | `String[]`；异常名称模式 | `{}` | 与`noRollbackFor`类似但有名称误匹配风险；通常优先类型安全写法 |

不写自定义回滚规则时，`RuntimeException` 和 `Error` 默认触发回滚，受检异常默认不触发。`isolation` 与 `timeout` 主要作用于新建事务；加入已有事务时，不能认为内层声明会覆盖外层设置。类名模式不是正则式，也不是精确类匹配；例如过于宽泛的 `"Exception"` 容易匹配不该匹配的异常。

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

若确实需要从配置读取事务超时，可单独写 `@Transactional(timeoutString = "${app.tx-timeout:10}")`；若项目连接多个数据源，可在已配置相应Bean后写 `@Transactional(transactionManager = "employeeTransactionManager")`。这两行是**独立写法示意**，不能在没有配置项或Bean的第13章工程中直接替换原注解。

### 4. 七种传播值怎样选择

这七个值是 `Propagation` 枚举的完整取值；“外层”指调用当前方法时是否已有事务。

| 传播值 | 外层有事务 | 外层无事务 | 项目判断 |
| --- | --- | --- | --- |
| `REQUIRED` | 加入外层 | 新建事务 | 第13章多步写入采用此默认值 |
| `SUPPORTS` | 加入外层 | 非事务执行 | 不能用来保证写入必须有事务 |
| `MANDATORY` | 加入外层 | 抛出异常 | 仅在调用方必须负责开启事务的明确设计中使用 |
| `REQUIRES_NEW` | 暂停外层并新建独立事务 | 新建事务 | 内层提交不随外层回滚；还会额外占用连接，不随意用于履历写入 |
| `NOT_SUPPORTED` | 暂停外层并非事务执行 | 非事务执行 | 可能使数据库操作脱离原子边界 |
| `NEVER` | 抛出异常 | 非事务执行 | 明确禁止在事务中执行时才使用 |
| `NESTED` | 在支持时用保存点建立嵌套范围 | 像 `REQUIRED` 一样新建事务 | 是否支持取决于事务管理器与驱动，不是独立的 `REQUIRES_NEW` |

传播方式解决“方法调用之间怎样共享事务”，隔离级别解决“并发事务彼此能看到什么”。两者不要混淆。

### 5. 七种传播行为放进Service调用中

下表的“新物理事务”以**进入该方法时已有外层事务**为前提；没有外层事务时，`REQUIRED`、`REQUIRES_NEW`、`NESTED`会启动事务。`NESTED`在有外层时使用同一个物理事务内的保存点，不是另一条独立提交的连接；实际支持取决于事务管理器、JDBC驱动和数据库。调用必须经过另一个Spring Bean的代理，不能写成`this.inner()`后期待内层传播属性生效。

| 内层传播值 | 无外层事务 | 已有外层事务 | 异常时的关键影响 | 适用判断 |
| --- | --- | --- | --- | --- |
| `REQUIRED` | 新建物理事务 | 加入同一物理事务 | 内层代理标记rollback-only会影响整体 | 员工UPDATE与履历INSERT共同成败；默认首选 |
| `SUPPORTS` | 非事务运行 | 参加外层事务 | 无外层时没有多步回滚保障 | 只读辅助查询可以考虑，不用于“必须原子提交”的写操作 |
| `MANDATORY` | 立即抛“缺少事务”异常 | 参加外层事务 | 调用前提错误直接暴露 | 仅当设计规定调用者必须建立事务 |
| `REQUIRES_NEW` | 新建物理事务 | 暂停外层，另建独立物理事务 | 内层可独立提交；外层随后回滚不撤销它 | 允许独立提交的结果记录；不用于员工履历共同成败 |
| `NOT_SUPPORTED` | 非事务运行 | 暂停外层，非事务运行 | 此处写入不随外层回滚 | 只在明确不需要事务的调用段使用，不用于一致性写入 |
| `NEVER` | 非事务运行 | 立即抛异常 | 明确拒绝事务内调用 | 用于必须避免事务上下文的特殊接口 |
| `NESTED` | 新建事务 | 同一物理事务内建立保存点 | 可回到保存点，外层最后回滚仍撤销全部 | 允许局部失败但最终仍受外层提交控制的JDBC场景 |

第13章是`REQUIRED`：`EmployeeServiceImpl.update()`开始业务事务，员工更新与履历写入同属它。若把履历写入改为独立Bean的`REQUIRES_NEW`，履历可在外层员工更新回滚后单独留下，违背当前规格。它还可能额外占用连接；并发线程都持有外层连接、再等待内层连接时会耗尽连接池。不要因“想确保日志一定留下”就把业务履历改成独立事务。

`REQUIRES_NEW`隔开的是**数据库物理事务**，不会自动截断Java异常的传播。以下是两个独立Bean经Spring代理调用时的流程示意，不是要求修改第13章员工与履历的共同事务：

```text
场景A：内层成功，外层随后失败
外层事务A开始 → 暂停A → 内层事务B开始并COMMIT
              → 恢复A → 外层抛RuntimeException → A ROLLBACK
结果：B已经独立提交的数据通常保留，不随A一起撤销。

场景B：内层失败，异常继续向外传播
外层事务A开始 → 暂停A → 内层事务B开始并抛RuntimeException
              → B ROLLBACK → 恢复A → 异常离开外层事务代理
结果：若符合外层回滚规则，A也ROLLBACK；这不是B的rollback-only直接传给A。
```

若外层**捕获**B抛出的异常、业务规格允许继续且A本身未被标记为rollback-only，A仍可能正常提交；若异常继续离开外层代理，默认运行时异常规则会使A回滚。不能把“内外事务可独立提交/回滚”误读成“内层异常不会影响调用方”。选择`REQUIRES_NEW`前，应同时评审独立提交是否符合业务规格，以及异常要继续抛出、转换还是在外层按明确规则处理。

`NESTED`可用“外层事务→保存点→局部操作失败→回到保存点→外层继续或最后整体回滚”理解。保存点不是独立提交：外层回滚仍撤销局部已成功部分。`SUPPORTS`和`NOT_SUPPORTED`都可能非事务运行，但前者有外层时会加入，后者有外层时会暂停；`MANDATORY`与`NEVER`分别约束“必须有”和“必须没有”外层事务。两种约束主要是让错误调用尽早失败，不是性能开关。

### 6. 内层REQUIRED失败、rollback-only与意外回滚

`rollback-only`（只能回滚标记）表示当前物理事务已经不允许正常提交。下面是**两个Spring Service Bean的方法结构片段**，只用于说明事务代理行为，不是替换第13章代码：

```java
// EmployeeChangeService：外层Bean
@Transactional
public void change(Long id) {
    employeeMapper.updateName(id, "Tanaka");
    try {
        changeLogService.write(id); // 调用另一个Spring Bean的代理
    } catch (RuntimeException ex) {
        log.warn("履历失败，外层捕获", ex);
    }
    // 方法看似正常返回，外层事务提交时仍可能报UnexpectedRollbackException
}

// ChangeLogService：内层Bean
@Transactional(propagation = Propagation.REQUIRED)
public void write(Long id) {
    changeLogMapper.insertFor(id);
    throw new IllegalStateException("模拟履历失败");
}
```

两个注解方法分别经过代理；内层`REQUIRED`加入外层物理事务。内层运行时异常离开其代理时，内层逻辑事务把共享的物理事务标为rollback-only。外层虽捕获异常并正常返回，提交时发现只能回滚，Spring抛出`UnexpectedRollbackException`，避免调用方误以为已提交。若异常在同一个事务方法内部被捕获、从未经过另一个事务代理，或传播方式不同，结果可能不同；不能把“所有catch都会触发意外回滚”当成规则。正确处理是明确失败规格，让必须共同回滚的异常离开外层事务方法，而不是吞掉异常后强行提交。

### 7. 异常类型与回滚规则

| 离开事务代理的异常/状态 | 未定制规则时的常见结果 | 项目判断 |
| --- | --- | --- |
| `RuntimeException`或`Error` | 回滚 | 第13章履历写入失败属于此类 |
| 受检`IOException` | 默认不因该异常回滚 | 多步写入若必须共同失败，声明`rollbackFor = IOException.class` |
| 受检`IOException`且配置上述`rollbackFor` | 回滚 | 同时确认异常确实离开代理 |
| `catch`后正常返回 | 可能尝试提交 | 若已有rollback-only，提交时仍回滚并可能报`UnexpectedRollbackException` |
| 内层`REQUIRED`已标记rollback-only | 外层不能靠`catch`恢复提交 | 应按整体失败处理 |

Spring默认规则是运行时异常与`Error`回滚、受检异常不回滚；但Spring Framework支持全局修改默认回滚策略，应以本项目实际配置为准。`rollbackFor`、`noRollbackFor`都匹配时，最具体的匹配规则优先。类名模式是字符串包含匹配而非正则，`"Exception"`过宽会误命中；通常用Class类型更安全。不要给所有方法无差别写`rollbackFor = Exception.class`：它会让本来表示可提交业务告知的受检异常也回滚，改变契约。`noRollbackFor`只有“异常对外报告但已写入仍允许提交”的明确规格才考虑，不能用来掩盖履历失败。

`readOnly = true`主要是对事务基础设施的提示，不是禁止写SQL的权限控制。单条普通SELECT不一定需要显式事务；多次SELECT若业务要求同一一致性视图，才考虑事务及实际隔离级别，详见[A13-2](A13_02_isolation_mvcc.md)。`timeout`与`timeoutString`都是新事务秒数限制，非HTTP客户端超时、数据库锁等待配置，也不能替代慢SQL优化；内层加入已有`REQUIRED`事务时通常沿用外层特征。`label`是标签数组，只有支持它的事务管理器才赋予含义。

## 五、按业务场景决定事务边界

| 场景 | 首先确认的问题 | 常见选择 |
| --- | --- | --- |
| 单条查询 | 是否只有一次普通SELECT | 不机械增加事务 |
| 多次查询/统计 | 是否要求同一时间点的视图 | 结合数据库隔离级别设计只读事务 |
| 主表＋明细、员工＋履历 | 是否必须共同成败 | 一个短`REQUIRED`业务事务 |
| 批量导入 | 单批失败是否回滚整批，能否重试 | 明确分批边界与失败记录，不用无限大事务 |
| 数据库写入＋外部API | 外部调用失败如何补偿 | 本地事务不能自动回滚外部系统；尽量避免持锁等待API |
| 夜间Batch/定时任务 | 中断后从哪里恢复 | 明确提交批次、幂等键、有限重试及证据 |

## 六、重要概念、Review与验证

| 容易混淆 | 正确区分 |
| --- | --- |
| 事务与锁 | 事务划定共同提交/回滚边界；锁协调冲突访问 |
| 传播与隔离 | 传播决定方法间共享哪个事务；隔离决定并发事务的可见性 |
| 回滚与重试 | 回滚撤销本次未提交变化；重试是重新执行业务，必须考虑幂等性 |

在个人专用MySQL测试库，用第13章现有的员工UPDATE＋履历INSERT先运行正常系，再让履历INSERT抛运行时异常；在Eclipse运行对应JUnit并查询两表，正常系都改变，异常系都不改变。上述`rollback-only`片段只作机制阅读，若另建实验Bean，必须在个人练习分支完成、不要覆盖主线Service；实验后删除临时代码，重新运行第13章原测试。失败时先查调用是否经代理、异常是否离开事务方法、Mapper是否同一数据源、是否已被标记rollback-only。

Review时检查事务是否包住完整用例、是否吞异常、是否误用`REQUIRES_NEW`拆散共同成败、是否在锁内等外部API、是否把`readOnly`当权限、是否给了无依据的隔离/超时设置。完成标准：能用实际两表数据解释一次提交/回滚，能按调用关系选择传播值，并能解释内层失败被外层捕获后为何仍可能出现`UnexpectedRollbackException`。

## 七、检查与练习

对照第13章的 `EmployeeServiceImpl.update()`，指出事务从哪里开始、哪些Mapper调用在同一边界中，以及履历INSERT抛出运行时异常时员工UPDATE为何回滚。再检查“捕获异常后只记录日志并正常返回”的改动，写出需要核对的HTTP和数据库证据；不要只凭 `catch` 推断最终结果。

## 八、官方参考资料

- [Spring Framework：`@Transactional` 属性与默认值](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html)
- [Spring Framework：`Propagation` 全部枚举值](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html)
- [Spring Framework：`Isolation` 全部枚举值](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Isolation.html)
- [Spring Framework：使用 `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Spring Framework：回滚规则](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
- [Spring Framework：事务传播](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)
