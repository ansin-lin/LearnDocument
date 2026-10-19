# 第13章 用事务保证多步写入一致

> 本章目标：把“修改员工”扩展为“修改员工并写入变更履历”，使用Spring事务保证两次写入全部成功或全部回滚，并用数据库状态和自动化测试证明结果。

## 一、一个业务动作可能包含多次SQL

现在的PUT只修改 `employees` 一张表。业务规格增加一项要求：

```text
修改员工
  1. UPDATE employees
  2. INSERT employee_change_logs
```

变更履历用于回答“哪个员工发生了什么操作”。本章还没有登录用户，因此不虚构操作人字段；后续认证功能建立后再扩展。

两条SQL分别都是原子操作，但整个业务动作仍可能半完成：

```text
UPDATE成功
  → INSERT履历失败
  → 如果没有共同事务，员工已改但没有履历
```

本章的验收条件：

| 场景 | employees | employee_change_logs | HTTP |
| --- | --- | --- | ---: |
| 两次写入成功 | 保存新值 | 新增1条 | 200 |
| 第二次写入失败 | 恢复旧值 | 不新增 | 500 |

## 二、按案例完成两次写入

从第12章稳定工程继续。先建立履历表，再新建实体、Mapper接口和XML；随后修改Service构造器并完整替换原 `update()`，不要新增重名方法。初次实验暂时不加 `@Transactional`，案例二再加回。以下先给出本案例需要的文件和代码，章末保留最终完整文件用于核对其他未改动方法。

### 案例13-01：第二次写入失败时发生什么

一次PUT既更新员工又写履历，两条SQL属于一个业务动作。先在个人练习用的MySQL 8.0 `employee_db` 创建履历表，不能在共享或生产库执行：

```sql
CREATE TABLE employee_change_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_employee_change_logs PRIMARY KEY (id),
    INDEX idx_employee_change_logs_employee_id (employee_id)
);
```

`employee_id` 指向发生变更的员工，`action` 固定为 `EMPLOYEE_UPDATED`，`detail` 只记录部门变化；完整字段字典在章末。此表暂不建外键，避免现有物理删除员工时与履历保留规则冲突。

新建 `src/main/java/com/example/employee/entity/EmployeeChangeLog.java`：

```java
package com.example.employee.entity;

public class EmployeeChangeLog {
    private Long id;
    private Long employeeId;
    private String action;
    private String detail;

    public EmployeeChangeLog(Long employeeId, String action, String detail) {
        this.employeeId = employeeId;
        this.action = action;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
}
```

新建 `src/main/java/com/example/employee/mapper/EmployeeChangeLogMapper.java`：

```java
package com.example.employee.mapper;

import com.example.employee.entity.EmployeeChangeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeChangeLogMapper {
    int insert(EmployeeChangeLog changeLog);
}
```

新建 `src/main/resources/mapper/EmployeeChangeLogMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeChangeLogMapper">
    <insert id="insert"
            parameterType="com.example.employee.entity.EmployeeChangeLog"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO employee_change_logs (employee_id, action, detail)
        VALUES (#{employeeId}, #{action}, #{detail})
    </insert>
</mapper>
```

`insert()` 返回受影响行数；XML的 `id` 对应接口方法，`#{...}` 读取对象属性，`useGeneratedKeys` 与 `keyProperty` 将新主键写回 `changeLog.id`。在原 `EmployeeServiceImpl.java` 中新增导入 `EmployeeChangeLog`、`EmployeeChangeLogMapper`，把原来仅接收 `EmployeeMapper` 的字段和构造器替换为：

```java
private static final String ACTION_EMPLOYEE_UPDATED = "EMPLOYEE_UPDATED";
private final EmployeeMapper employeeMapper;
private final EmployeeChangeLogMapper employeeChangeLogMapper;

public EmployeeServiceImpl(
        EmployeeMapper employeeMapper,
        EmployeeChangeLogMapper employeeChangeLogMapper) {
    this.employeeMapper = employeeMapper;
    this.employeeChangeLogMapper = employeeChangeLogMapper;
}
```

Spring用构造器注入两个Mapper；原有CRUD方法暂不改动。接着用下面的方法完整替换 `EmployeeServiceImpl.update()`；本案例**暂时没有** `@Transactional`，其余CRUD方法保持原样：

```java
    @Override
    public EmployeeResponse update(
            Long id,
            EmployeeUpdateRequest request) {
        String department = request.getDepartment().trim();
        validateDepartment(department);
        Employee beforeUpdate = findEmployeeOrThrow(id);

        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
        employee.setEmail(normalizeEmail(request.getEmail()));

        try {
            int affectedRows = employeeMapper.update(employee);
            if (affectedRows > 1) {
                throw new EmployeeSystemException("修改员工影响多行");
            }
            if (affectedRows == 0
                    && employeeMapper.findById(id) == null) {
                throw new EmployeeNotFoundException(id);
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }

        EmployeeChangeLog changeLog = new EmployeeChangeLog(
                id,
                ACTION_EMPLOYEE_UPDATED,
                "department=" + beforeUpdate.getDepartment()
                        + "->" + employee.getDepartment());
        int logRows = employeeChangeLogMapper.insert(changeLog);
        if (logRows != 1 || changeLog.getId() == null) {
            throw new EmployeeSystemException("员工变更履历写入失败");
        }

        log.info(
                "employee_updated employeeId={} department={}",
                id,
                employee.getDepartment());
        return findById(id);
    }
```

本方法先读取旧员工，调用 `EmployeeMapper.update()`，再调用 `EmployeeChangeLogMapper.insert()`；正常路径返回更新后员工。先去掉部门两端空格，再按第10章同一规则校验，所以 `" Sales "` 仍可保存为 `Sales`。

业务代码准备好后，先按章末完整代码建立或更新 `src/test/resources/test-data.sql`、`EmployeeCrudIntegrationTest.java`，并根据新增的Mapper构造器依赖更新 `EmployeeServiceImplTest.java`。此时**只采用案例13-01没有 `@Transactional` 的 `update()`**；不要提前复制章末Service的最终注解。最终测试类只保留一份 `updateRollsBackWhenChangeLogInsertFails()`：它要求HTTP为500、员工1001的部门保持Sales，不为无事务阶段另建长期测试类，也不要把断言改为Development。

在Eclipse中右键运行这个测试方法。它的 `@Sql` 先在第12章建立、且通过 `MySqlTestGuard` 保护的专用MySQL测试库重建两张表及员工1001（原部门Sales）；测试代码随后删除**该测试库的**履历表，再向 `PUT /employees/1001` 发送 `{"name":"Tanaka","department":"Development","email":"tanaka@example.com"}`。第一次UPDATE成功，第二次INSERT因表不存在而失败。在两个Mapper使用同一真实MySQL数据源、异常继续向外传播且测试本身没有事务的前提下，HTTP预期为500，独立查询得到的部门却是Development。因此最终测试的Sales断言应显示红色：它正确发现了部分提交，不是JUnit坏了，更不能修改断言来取得绿色。只在专用测试库执行删表实验；个人开发库只验证正常PUT及履历，不删表。下次运行此方法时，`@Sql` 会重新准备表和数据。

| 实验阶段 | Service代码状态 | HTTP预期 | 独立查询的最终部门 | 同一个回滚测试 |
| --- | --- | --- | --- | --- |
| 案例13-01 | `update()` 暂无 `@Transactional` | 500 | Development | 红色：实际值不符合Sales断言 |
| 案例13-02 | `update()` 增加 `@Transactional` | 500 | Sales | 绿色：Sales断言通过 |
| 案例13-03 | 有事务，但临时吞掉履历异常 | 观察实际结果，可能变化 | 根据实际事务状态确认 | 记录现象并分析，不改最终断言 |

这是故意构造的教学实验；章末保留的正式业务代码必须是案例13-02恢复后的正确状态。

### 案例13-02：给业务方法建立事务边界

在同一 `update()` 方法的 `@Override` 后加上 `@Transactional`，导入 `org.springframework.transaction.annotation.Transactional`。它标记“这个公开的Service方法是事务边界”，不是一条需要自己编写commit的SQL；Spring管理的Service代理在调用方法前开始事务。这里不设置属性，默认采用 `REQUIRED`（没有事务就新建，已有事务就加入）、数据库默认隔离级别、非只读，并在运行时异常或 `Error` 向外抛出时回滚；常用属性及为什么采用这些默认值见下文第五、六、九节及[A13-1事务机制附录](../appendix/A13_01_transaction_spring_mechanisms.md)。

只新增这一行注解，放在原方法的 `@Override` 与 `public EmployeeResponse update(...)` 之间；案例13-01的完整方法体一行也不删。两次Mapper写入正常结束时共同提交，第二次抛出运行时异常时共同回滚。**不要修改测试代码或断言**，在Eclipse中重新运行刚才的 `updateRollsBackWhenChangeLogInsertFails()`：HTTP仍为500，独立查询的员工部门应从无事务时的Development变为Sales，同一个测试由红色变为绿色。再运行 `updateCommitsEmployeeAndChangeLogTogether()`，确认正常更新时部门变为Development且履历新增一条。正常测试证明共同成功；只在失败测试中才能证明中途异常时共同回滚，两者缺一不可。测试类本身不得加 `@Transactional`，否则测试外层回滚会掩盖Service缺少事务的错误。

### 案例13-03：捕获并吞掉异常的对照实验

只在练习用的 `EmployeeServiceImpl.update()` 中做临时对照，保留案例13-02加上的 `@Transactional`。找到 `EmployeeChangeLog changeLog = ...` 后面、`log.info(...)` 前面的原代码：

```java
int logRows = employeeChangeLogMapper.insert(changeLog);
if (logRows != 1 || changeLog.getId() == null) {
    throw new EmployeeSystemException("员工变更履历写入失败");
}
```

临时替换为以下错误写法，并导入 `org.springframework.dao.DataAccessException`；`update()` 的其他行不变：

```java
try {
    int logRows = employeeChangeLogMapper.insert(changeLog);
    if (logRows != 1 || changeLog.getId() == null) {
        throw new EmployeeSystemException("员工变更履历写入失败");
    }
} catch (DataAccessException exception) {
    log.warn("change_log_failed employeeId={}", id, exception);
    // 故意吞掉异常，继续走下面的成功返回；仅用于失败实验。
}
```

在专用MySQL测试库重跑章末 `updateRollsBackWhenChangeLogInsertFails()`，记录履历Mapper是否抛出异常、Service是否捕获、HTTP实际返回200还是500、员工部门最终值，以及是否出现额外事务异常，再解释最终回滚测试为何通过或失败。若事务未被其他环节标记为仅回滚，方法正常返回可能让先前的UPDATE提交；若已经被标记为仅回滚，则仍可能回滚或产生额外事务异常。捕获位置、底层异常和后续数据库操作都会影响结果，不能推断“所有 `catch` 必然提交或返回200”。实验结束把临时代码**原样恢复为上方不捕获异常的两句判断**，移除不再使用的导入，保留 `@Transactional`，然后重跑同一回滚测试：应返回500且员工部门为Sales。

### 案例13-04：沿代理调用路径追踪事务

在Eclipse中给Controller的PUT入口、`EmployeeServiceImpl.update()` 和两个Mapper调用处设置断点，沿“Controller → Spring管理的Service代理 → 真实update方法 → Mapper”检查。注解是事务元数据，事务拦截器围绕经代理进入的方法提交或回滚；`new EmployeeServiceImpl(...)` 的普通Java对象和同类内部 `this.update(...)` 都不能据此期待新事务启动。保留Controller注入的Service调用方式，不新增演示用入口。

### 案例13-05：用测试和数据库状态验收

章末完整 `EmployeeCrudIntegrationTest` 含成功提交与第二次写入失败两个测试。先确保测试方法、测试类都没有 `@Transactional`；每个方法由 `@Sql` 在专用MySQL测试库准备固定数据。成功测试断言HTTP、`employees.department`、履历条数和内容；失败测试故意删除履历表，因此只能断言HTTP与员工部门没有改变，不能再查询已删除的履历表。个人开发库只用受控练习数据观察正常PUT；删表失败实验只能在通过保护检查的测试库执行，实验后恢复临时方法改动。

这里有两种不同的事务位置：

| 位置 | `@Transactional` 的作用 | 本章采用吗 |
| --- | --- | --- |
| `EmployeeServiceImpl.update()` | 把员工UPDATE和履历INSERT放进一个业务事务 | 采用 |
| JUnit测试类或方法 | 可能让测试操作处于测试框架管理的事务中，并在测试结束后回滚 | 不采用 |

本章要验证的是**Service自身**能否回滚。因此通过真实Spring管理的Service Bean执行接口请求，再由 `JdbcTemplate` 独立查询数据库最终状态；不在测试类或测试方法上增加 `@Transactional`。第12章的Mockito测试直接 `new EmployeeServiceImpl(...)`，得到普通Java对象而非Spring事务代理，它能验证Service逻辑和异常传播，却不能证明真实MySQL事务已回滚。前者属于单体测试；Controller、Service、Mapper与MySQL共同参与的事务验证属于后端内部结合测试，两者都通过Eclipse的JUnit功能运行。

## 三、事务、提交和回滚

事务是数据库对一组操作的一致性边界：

```text
事务开始
  → UPDATE employees
  → INSERT employee_change_logs
  ├─ 方法正常结束：commit，两次写入同时可见
  └─ 出现需回滚异常：rollback，撤销两次写入
```

commit是确认当前事务的改变；rollback是撤销当前事务中尚未提交的改变。回滚不是把数据库恢复到任意历史时间，也不能撤销事务开始前已提交的数据。

## 四、为什么事务边界放在Service

Controller只知道HTTP请求，单个Mapper只知道自己的SQL。只有Service知道“修改员工和写履历是一个业务动作”：

```text
Controller
  → EmployeeServiceImpl.update()  ← 事务边界
      ├─ EmployeeMapper.update()
      └─ EmployeeChangeLogMapper.insert()
```

把 `@Transactional` 写在两个Mapper方法上会把一个业务动作拆成两个边界；写在Controller则会让HTTP层承担业务一致性。

`delete()` 目前只执行一条DELETE，单条SQL由数据库保证原子性，本章不因为“写方法”就机械加注解。`create()` 在INSERT后还会回查刚创建的员工，因此本章也把新增与回查放在一个事务内。

## 五、@Transactional由谁处理

`@Transactional` 的完整名称是 `org.springframework.transaction.annotation.Transactional`。它是事务元数据，不是Java编译器直接执行的commit命令。

Spring Boot根据已有DataSource和事务相关依赖配置事务管理器。Spring在Service Bean外部创建代理：

```text
Controller调用Service Bean
  → 事务代理读取@Transactional
  → 开始或加入事务
  → 调用真实EmployeeServiceImpl.update()
  → 根据返回或异常提交/回滚
```

本章把注解写在具体实现类的 `public` 方法上，并从Controller注入的Service Bean调用。这是容易识别且符合默认代理模式的写法。注解位置和代理调用可参考[Spring官方文档](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)。

MyBatis-Spring使Mapper使用与Spring事务管理器对应的DataSource和SqlSession。同一事务中的两个Mapper调用因此可共同提交或回滚，业务代码不能手工调用 `SqlSession.commit()`、`rollback()` 或 `close()`。详见[MyBatis-Spring事务文档](https://mybatis.org/spring/transactions.html)。

## 六、正常结束与异常回滚

`@Transactional` 的默认回滚规则：

| 方法离开方式 | 默认结果 |
| --- | --- |
| 正常 `return` | commit |
| 抛出 `RuntimeException` 或其子类 | rollback |
| 抛出 `Error` | rollback |
| 抛出受检 `Exception` | 默认不回滚 |

`EmployeeSystemException`、`DuplicateEmailException` 和MyBatis-Spring转换后的 `DataAccessException` 都是运行时异常，向上抛出时会触发默认回滚。默认规则和自定义回滚规则可参考[Spring回滚规则官方说明](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)。

如果业务方法确实会抛出受检异常，应按规格明确回滚类型。下面是规则片段，不加入当前项目：

```java
@Transactional(rollbackFor = java.io.IOException.class)
public void importEmployees(java.nio.file.Path file)
        throws java.io.IOException {
    // 读取文件并写入数据库
}
```

`java.nio.file.Path` 表示文件系统路径，`java.io.IOException` 表示文件读写可能发生的受检异常。这里写完整类名是为了把重点放在回滚规则上；它们不是本章员工项目新增的业务类型。

| 属性 | 当前值 | 可接受的值 | 默认值与作用 |
| --- | --- | --- | --- |
| `rollbackFor` | `java.io.IOException.class` | `Throwable` 子类的Class数组 | 默认空数组；增加必须回滚的异常类型 |

不要无理由统一写 `rollbackFor = Exception.class`。先区分当前方法会抛出哪种异常、失败后是应回滚还是允许提交，再定义规则。

## 七、为什么吞掉异常会误提交

下面是错误片段：

```java
try {
    employeeChangeLogMapper.insert(changeLog);
} catch (DataAccessException exception) {
    log.warn("change_log_failed");
}
```

捕获后方法继续正常结束，事务代理看不到这个异常，前面的employees更新**可能**被提交；但若事务已被其他环节标记为仅回滚，仍可能回滚或出现额外事务异常。必须结合HTTP、日志与数据库状态确认，不能只看 `catch` 就断言一定提交。本章的最终代码让 `DataAccessException` 继续向外抛出，交给事务代理回滚，再由全局异常处理器返回500。

如果必须在当前层转换异常，应抛出能触发回滚的新异常，不能只写日志后继续返回成功。

## 八、同类内部调用为什么会绕过代理

默认代理模式只能拦截经过Spring代理的外部调用。下面的 `updateWithoutTransaction()` 如果本身没有事务，它在同一对象内调用 `update()` 时不会再经过外层代理：

```java
public EmployeeResponse updateWithoutTransaction(
        Long id,
        EmployeeUpdateRequest request) {
    return update(id, request);
}
```

因此不要依赖“同类内部调用带注解方法”来启动事务。可以把完整业务边界放在对外的Service方法，或将需要独立事务的职责拆到另一个Bean。

本章 `create()` 和 `update()` 已经由Controller经Service代理调用。它们内部调用 `findById()` 不需要新建第二个事务；查询会在外层已开始的事务中执行。

## 九、默认传播与已有事务

`@Transactional` 默认使用 `Propagation.REQUIRED`：

```text
调用时没有事务 → 开始新事务
调用时已有事务 → 加入已有事务
```

这适合“一个Service业务边界组织多个数据访问”的常见调用。Spring对REQUIRED的完整语义见[官方传播文档](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)。

`REQUIRES_NEW` 会暂停外层事务并使用独立物理事务，它可能额外占用数据库连接；`NESTED` 通常依赖JDBC保存点实现局部回滚。它们都不符合本章“两次写入共同成败”的规格，因此只需能识别，不替换默认REQUIRED。


## 十、用数据库状态证明回滚

在Eclipse中分别运行 `EmployeeServiceImplTest`、`EmployeeCrudIntegrationTest`，再对项目执行 **Run As → JUnit Test** 回归其他测试类。确认测试连接的是受保护的专用MySQL测试库；不要为了每次验证接口都先使用命令行重新编译和启动服务。Maven构建与打包仍在第17章单独学习。

在第12章已有测试上增加本章事务场景。不同学员完成的练习数量可能不同，先核对事务成功与失败场景都被执行，再看最终汇总；不要用固定总数判断是否通过。日常流程是修改Service、在Eclipse运行相关JUnit方法、阅读失败堆栈与数据库结果、修复后重跑原方法，最后运行整个测试类及相关回归并保存证据。预期：

Eclipse的JUnit视图应显示全部已运行测试成功、失败数为0；若某项失败，先查看该测试的异常和数据库最终状态，不凭绿色/红色图标猜原因。

回滚测试不在测试方法或测试类上加 `@Transactional`。否则测试框架自己的外层事务可能在方法结束时统一回滚，无法证明Service的事务边界真正生效。Service的Mockito单体测试也不能代替这项真实数据库验证。

证明回滚需要同时保存：

1. PUT实际返500；
2. 日志中有履历表不存在的原因；
3. 失败前员工1001的部门为Sales；
4. 失败后部门仍为Sales；
5. 下一个测试前两张表已恢复。

只看500不能证明回滚；只看日志也不能证明数据库状态。

## 十一、常见失败与定位

| 现象 | 定位点 | 原因 | 修正 |
| --- | --- | --- | --- |
| 履历失败后employees仍改变 | Service Bean调用路径 | 方法没有事务或同类内部调用绕过代理 | 让外部调用经过带注解的Service Bean方法 |
| 异常已记录但数据仍提交 | catch后的控制流 | 捕获异常后正常返回 | 继续抛出可触发回滚的异常 |
| 一个Mapper回滚、另一个没有 | DataSource和事务管理器 | Mapper使用不同DataSource或绕过MyBatis-Spring | 确保SqlSessionFactory与事务管理器使用同一DataSource |
| 测试总是回滚 | 测试类注解 | 测试方法自己带 `@Transactional` | 移除外层测试事务，通过 `@Sql` 隔离数据 |
| 启动时找不到新Mapper | 包名、`@Mapper`、XML namespace | Java接口和XML名称不一致 | 按完整类名核对namespace |

## 十二、规格理解、影响调查、Review与练习

### 练习1：Review事务边界

审查“Controller调用两个Service，每个Service各自提交”的设计。指出半完成时的数据状态，将一个完整业务动作放到一个Service边界，并列出需回归的HTTP与数据库结果。

### 练习2：验证捕获异常的影响

在专用测试分支中临时捕获履历Mapper异常且不再抛出，运行同一个回滚测试，记录HTTP、异常、数据库部门与JUnit断言的实际结果，并解释原因；不要预设一定变成Development。随后恢复正确代码，重跑并确认Sales保持不变。

### 练习3：受检异常规格

假设批量导入方法遇到 `IOException` 时必须撤销已写数据。写出方法签名和 `rollbackFor`，再写一个“异常抛出后表中件数不变”的测试规格。

### 练习4：影响调查

规格变更：删除员工时也必须写 `EMPLOYEE_DELETED` 履历。调查Service、履历数据、物理删除与履历保留顺序、事务、接口响应和回归测试的影响；先不实施，提交调查表。

### 练习5：整理事务证据

分别保存成功与中途失败场景的前置数据、HTTP状态、employees结果、异常原因和测试结果。成功场景还要记录履历条数与内容；失败实验会临时删除履历表，不能再查询该表，应记录删表操作及下次测试前由 `@Sql` 恢复的证据。结论必须指向数据库证据，不能只写“已加 `@Transactional`”。

### 练习6：选择并发控制方式

对“管理员在编辑画面停留10分钟后提交”和“夜间作业读取后立即更新”两个场景，分别比较不加锁、乐观锁和悲观锁。写出选择、失败时的接口状态、用户可见信息、超时或重试策略以及需补充的并发测试，不只回答机制名称。

## 十三、章末完整代码

### 最终文件清单与完整实现

先理解本章要保护的单位是“一次修改员工业务”，不是单独一条SQL。Spring Boot依据DataSource准备事务管理能力；外部调用进入Spring管理的Service代理时，代理读取 `@Transactional`，开始事务，再让方法依次调用两个Mapper。两次调用使用同一个受事务管理的数据库连接：方法正常结束则共同提交；抛出运行时异常则默认共同回滚。因此注解放在包含两个写操作的Service方法上，不能只贴在其中一个Mapper方法上。

先完成成功路径，再故意让第二次写入失败；测试时在Service方法返回或抛异常之后，另行查询 `employees` 与 `employee_change_logs`。测试方法自身不能再加一个外层 `@Transactional`，否则测试框架最后的回滚可能掩盖Service事务是否生效。捕获异常后正常返回、同类内部直接调用绕过代理是主线陷阱；传播方式只先掌握REQUIRED，隔离级别、乐观锁、悲观锁及并发覆盖属于后面的扩展，不是基本提交/回滚的同义词。

以下仍保留完整代码和测试，按“履历表与Mapper→Service事务方法→成功与失败测试”阅读。

本章保留第12章的接口契约和测试配置，增加一张表和一个Mapper，并替换Service实现与直接受影响的测试文件。替换测试类时须保留原有正常系、异常系回归案例，不因新增事务测试而删减第12章覆盖范围。

```text
src/main/java/com/example/employee/
├── entity/EmployeeChangeLog.java                  ← 新建
├── mapper/EmployeeChangeLogMapper.java            ← 新建
└── service/impl/EmployeeServiceImpl.java          ← 完整替换

src/main/resources/mapper/
└── EmployeeChangeLogMapper.xml                     ← 新建

src/test/
├── java/com/example/employee/
│   ├── integration/EmployeeCrudIntegrationTest.java  ← 完整替换
│   └── service/EmployeeServiceImplTest.java          ← 完整替换
└── resources/test-data.sql                         ← 完整替换
```

### 1. 在MySQL 8.0创建履历表

只在个人练习用的 `employee_db` 执行下面DDL；先确认当前连接的不是共享或生产数据库。

```sql
USE employee_db;

CREATE TABLE employee_change_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_employee_change_logs PRIMARY KEY (id),
    INDEX idx_employee_change_logs_employee_id (employee_id)
);
```

| 字段 | 类型 | 必填 | 生成者 | 本章含义 |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | 是 | MySQL | 履历主键 |
| `employee_id` | BIGINT | 是 | Service | 发生变更的员工编号 |
| `action` | VARCHAR(30) | 是 | Service | 固定操作类型 `EMPLOYEE_UPDATED` |
| `detail` | VARCHAR(200) | 是 | Service | 仅记录部门由旧值变为新值，不记姓名或邮箱 |
| `created_at` | DATETIME | 是 | MySQL | 写入履历的时间 |

本表暂不建外键。员工目前使用物理删除，而履历可能需要在员工删除后保留；是否使用外键、限制删除或逻辑删除应由正式数据保留规格决定。

`INDEX idx_employee_change_logs_employee_id (employee_id)` 为员工编号建立普通索引，便于以后按员工查找履历。索引会占用存储空间，也会增加写入时的维护成本，因此应根据实际查询条件建立，不能给每个字段都机械加索引。

### 2. 新建EmployeeChangeLog.java

`EmployeeChangeLog` 是履历表一行数据在Java中的表示，职责与第9章的 `Employee` 相同，但对应的是另一张表。它不是接口请求DTO：Controller不会直接接收它，Service根据已经确认的业务结果创建它，再交给Mapper保存。

```java
package com.example.employee.entity;

public class EmployeeChangeLog {

    private Long id;
    private Long employeeId;
    private String action;
    private String detail;

    public EmployeeChangeLog(
            Long employeeId,
            String action,
            String detail) {
        this.employeeId = employeeId;
        this.action = action;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }
}
```

### 3. 新建EmployeeChangeLogMapper.java

这里复用第9章已经学过的Mapper模式：接口声明数据库操作，MyBatis根据方法名和XML中的 `id` 找到SQL。`insert()` 返回受影响行数，Service据此确认履历是否真的写入一行。

```java
package com.example.employee.mapper;

import com.example.employee.entity.EmployeeChangeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeChangeLogMapper {

    int insert(EmployeeChangeLog changeLog);
}
```

### 4. 新建EmployeeChangeLogMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeChangeLogMapper">

    <insert id="insert"
            parameterType="com.example.employee.entity.EmployeeChangeLog"
            useGeneratedKeys="true"
            keyProperty="id">
        INSERT INTO employee_change_logs (
            employee_id,
            action,
            detail
        ) VALUES (
            #{employeeId},
            #{action},
            #{detail}
        )
    </insert>

</mapper>
```

### 5. 完整替换EmployeeServiceImpl.java

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeChangeLog;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.EmployeeSystemException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeChangeLogMapper;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final String ACTION_EMPLOYEE_UPDATED =
            "EMPLOYEE_UPDATED";

    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeMapper employeeMapper;
    private final EmployeeChangeLogMapper employeeChangeLogMapper;

    public EmployeeServiceImpl(
            EmployeeMapper employeeMapper,
            EmployeeChangeLogMapper employeeChangeLogMapper) {
        this.employeeMapper = employeeMapper;
        this.employeeChangeLogMapper = employeeChangeLogMapper;
    }

    @Override
    public EmployeeResponse findById(Long id) {
        return toResponse(findEmployeeOrThrow(id));
    }

    @Override
    public List<EmployeeListItemResponse> findList(String department) {
        String filter = normalizeDepartmentFilter(department);
        List<Employee> employees = employeeMapper.findList(filter);
        List<EmployeeListItemResponse> responses = new ArrayList<>();
        for (Employee employee : employees) {
            responses.add(toListItemResponse(employee));
        }
        log.debug(
                "employee_list_completed filterApplied={} resultCount={}",
                filter != null,
                responses.size());
        return responses;
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        String department = request.getDepartment().trim();
        validateDepartment(department);
        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
        employee.setEmail(normalizeEmail(request.getEmail()));
        employee.setStatus("ACTIVE");
        try {
            int affectedRows = employeeMapper.insert(employee);
            if (affectedRows != 1 || employee.getId() == null) {
                throw new EmployeeSystemException("新增员工结果异常");
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        log.info(
                "employee_created employeeId={} department={}",
                employee.getId(),
                employee.getDepartment());
        return findById(employee.getId());
    }

    @Override
    @Transactional
    public EmployeeResponse update(
            Long id,
            EmployeeUpdateRequest request) {
        String department = request.getDepartment().trim();
        validateDepartment(department);
        Employee beforeUpdate = findEmployeeOrThrow(id);

        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(request.getName().trim());
        employee.setDepartment(department);
        employee.setEmail(normalizeEmail(request.getEmail()));

        try {
            int affectedRows = employeeMapper.update(employee);
            if (affectedRows > 1) {
                throw new EmployeeSystemException("修改员工影响多行");
            }
            if (affectedRows == 0
                    && employeeMapper.findById(id) == null) {
                throw new EmployeeNotFoundException(id);
            }
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(employee.getEmail());
        }

        EmployeeChangeLog changeLog = new EmployeeChangeLog(
                id,
                ACTION_EMPLOYEE_UPDATED,
                "department=" + beforeUpdate.getDepartment()
                        + "->" + employee.getDepartment());
        int logRows = employeeChangeLogMapper.insert(changeLog);
        if (logRows != 1 || changeLog.getId() == null) {
            throw new EmployeeSystemException("员工变更履历写入失败");
        }

        log.info(
                "employee_updated employeeId={} department={}",
                id,
                employee.getDepartment());
        return findById(id);
    }

    @Override
    public void delete(Long id) {
        int affectedRows = employeeMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new EmployeeNotFoundException(id);
        }
        if (affectedRows != 1) {
            throw new EmployeeSystemException("删除员工影响多行");
        }
        log.info("employee_deleted employeeId={}", id);
    }

    private Employee findEmployeeOrThrow(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }
        return employee;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getEmail());
    }

    private EmployeeListItemResponse toListItemResponse(
            Employee employee) {
        return new EmployeeListItemResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment());
    }

    private void validateDepartment(String department) {
        if (!"Sales".equals(department)
                && !"Development".equals(department)
                && !"Support".equals(department)) {
            throw new InvalidDepartmentException(department);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDepartmentFilter(String department) {
        if (department == null || department.isBlank()) {
            return null;
        }
        return department.trim();
    }
}
```

### 6. 完整替换EmployeeServiceImplTest.java

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.request.EmployeeUpdateRequest;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeChangeLog;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeChangeLogMapper;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmployeeServiceImplTest {

    private EmployeeMapper employeeMapper;
    private EmployeeChangeLogMapper employeeChangeLogMapper;
    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeMapper = mock(EmployeeMapper.class);
        employeeChangeLogMapper = mock(EmployeeChangeLogMapper.class);
        employeeService = new EmployeeServiceImpl(
                employeeMapper,
                employeeChangeLogMapper);
    }

    @Test
    void findByIdReturnsResponseWhenEmployeeExists() {
        Employee employee = new Employee();
        employee.setId(1001L);
        employee.setName("Tanaka");
        employee.setDepartment("Sales");
        employee.setEmail("tanaka@example.com");
        when(employeeMapper.findById(1001L)).thenReturn(employee);

        EmployeeResponse actual = employeeService.findById(1001L);

        assertThat(actual.getId()).isEqualTo(1001L);
        assertThat(actual.getName()).isEqualTo("Tanaka");
        assertThat(actual.getDepartment()).isEqualTo("Sales");
        assertThat(actual.getEmail()).isEqualTo("tanaka@example.com");
        verify(employeeMapper).findById(1001L);
        verifyNoInteractions(employeeChangeLogMapper);
    }

    @Test
    void findByIdThrowsWhenEmployeeDoesNotExist() {
        when(employeeMapper.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.findById(9999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("员工不存在：9999");
        verify(employeeMapper).findById(9999L);
        verifyNoInteractions(employeeChangeLogMapper);
    }

    @Test
    void createConvertsDuplicateKeyToBusinessConflict() {
        EmployeeCreateRequest request = createRequest("Sales");
        request.setEmail("tanaka@example.com");
        when(employeeMapper.insert(any(Employee.class)))
                .thenThrow(new DuplicateKeyException("duplicate email"));

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("邮箱已被使用：tanaka@example.com");
        verify(employeeMapper).insert(any(Employee.class));
        verifyNoInteractions(employeeChangeLogMapper);
    }

    @Test
    void createRejectsInvalidDepartmentBeforeMapperCall() {
        EmployeeCreateRequest request = createRequest("Unknown");

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(InvalidDepartmentException.class)
                .hasMessage("不允许的部门：Unknown");
        verifyNoInteractions(employeeMapper, employeeChangeLogMapper);
    }

    @Test
    void createReturnsGeneratedEmployee() {
        EmployeeCreateRequest request = createRequest("Sales");
        when(employeeMapper.insert(any(Employee.class))).thenAnswer(invocation -> {
            Employee inserted = invocation.getArgument(0);
            inserted.setId(2001L);
            return 1;
        });
        Employee stored = employee(2001L, "Sales");
        stored.setName("Suzuki");
        stored.setEmail("suzuki@example.com");
        when(employeeMapper.findById(2001L)).thenReturn(stored);

        EmployeeResponse actual = employeeService.create(request);

        assertThat(actual.getId()).isEqualTo(2001L);
        assertThat(actual.getName()).isEqualTo("Suzuki");
        verify(employeeMapper).insert(any(Employee.class));
        verify(employeeMapper).findById(2001L);
        verifyNoInteractions(employeeChangeLogMapper);
    }

    @Test
    void updateMissingEmployeeStopsBeforeWriting() {
        when(employeeMapper.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.update(
                9999L, updateRequest("Sales")))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(employeeMapper).findById(9999L);
        verify(employeeMapper, org.mockito.Mockito.never())
                .update(any(Employee.class));
        verifyNoInteractions(employeeChangeLogMapper);
    }

    @Test
    void updateWritesDepartmentChangeLog() {
        Employee before = employee(1001L, "Sales");
        Employee after = employee(1001L, "Development");
        when(employeeMapper.findById(1001L)).thenReturn(before, after);
        when(employeeMapper.update(any(Employee.class))).thenReturn(1);
        when(employeeChangeLogMapper.insert(any(EmployeeChangeLog.class)))
                .thenAnswer(invocation -> {
                    EmployeeChangeLog inserted = invocation.getArgument(0);
                    inserted.setId(3001L);
                    return 1;
                });

        EmployeeResponse actual = employeeService.update(
                1001L, updateRequest(" Development "));

        assertThat(actual.getDepartment()).isEqualTo("Development");
        ArgumentCaptor<EmployeeChangeLog> logCaptor =
                ArgumentCaptor.forClass(EmployeeChangeLog.class);
        verify(employeeChangeLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getEmployeeId()).isEqualTo(1001L);
        assertThat(logCaptor.getValue().getAction())
                .isEqualTo("EMPLOYEE_UPDATED");
        assertThat(logCaptor.getValue().getDetail())
                .isEqualTo("department=Sales->Development");
    }

    @Test
    void updatePropagatesChangeLogDatabaseFailure() {
        when(employeeMapper.findById(1001L))
                .thenReturn(employee(1001L, "Sales"));
        when(employeeMapper.update(any(Employee.class))).thenReturn(1);
        when(employeeChangeLogMapper.insert(any(EmployeeChangeLog.class)))
                .thenThrow(new DataIntegrityViolationException("insert failed"));

        assertThatThrownBy(() -> employeeService.update(
                1001L, updateRequest("Development")))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(employeeChangeLogMapper).insert(any(EmployeeChangeLog.class));
    }

    private EmployeeCreateRequest createRequest(String department) {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setName("Suzuki");
        request.setDepartment(department);
        request.setEmail("suzuki@example.com");
        return request;
    }

    private EmployeeUpdateRequest updateRequest(String department) {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setName("Tanaka");
        request.setDepartment(department);
        request.setEmail("tanaka@example.com");
        return request;
    }

    private Employee employee(Long id, String department) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName("Tanaka");
        employee.setDepartment(department);
        employee.setEmail("tanaka@example.com");
        return employee;
    }
}
```

这里是不用Spring容器的Service单体测试：它证明参数处理、Mapper协作、履历内容和异常**没有被吞掉**，但普通 `new EmployeeServiceImpl(...)` 不会创建事务代理，不能用它证明数据库回滚。回滚必须由下面的MySQL集成测试验证。`ArgumentCaptor.forClass()` 建立参数捕获器，`capture()` 记录实际交给Mapper的对象，`getValue()` 读取它以核对履历字段；这是第12章Mockito断言的延伸。

### 7. 完整替换test-data.sql

```sql
DROP TABLE IF EXISTS employee_change_logs;
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employees_email UNIQUE (email)
);

CREATE TABLE employee_change_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_employee_change_logs PRIMARY KEY (id),
    INDEX idx_employee_change_logs_employee_id (employee_id)
);

INSERT INTO employees (
    id,
    name,
    department,
    email,
    status
) VALUES
    (1001, 'Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    (1002, 'Sato', 'Development', 'sato@example.com', 'ACTIVE');
```

### 8. 完整替换EmployeeCrudIntegrationTest.java

```java
package com.example.employee.integration;

import com.example.employee.support.MySqlTestGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = "/test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeCrudIntegrationTest extends MySqlTestGuard {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completeCrudKeepsHttpAndDatabaseConsistent()
            throws Exception {
        String createJson =
                "{\"name\":\"Suzuki\","
                        + "\"department\":\" Support \","
                        + "\"email\":\"suzuki@example.com\"}";
        MvcResult createResult = mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.department").value("Support"))
                .andReturn();

        String location = createResult.getResponse()
                .getHeader("Location");
        assertThat(location).isNotBlank();
        long employeeId = Long.parseLong(
                location.substring(location.lastIndexOf('/') + 1));
        assertThat(countEmployee(employeeId)).isEqualTo(1);

        mockMvc.perform(get("/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name")
                        .value("Suzuki"));

        String updateJson =
                "{\"name\":\"Suzuki Ichiro\","
                        + "\"department\":\" Development \","
                        + "\"email\":\"suzuki@example.com\"}";
        mockMvc.perform(put("/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.department")
                        .value("Development"));
        assertThat(countChangeLogs(employeeId)).isEqualTo(1);

        mockMvc.perform(delete("/employees/{id}", employeeId))
                .andExpect(status().isNoContent());
        assertThat(countEmployee(employeeId)).isZero();
        assertThat(countChangeLogs(employeeId)).isEqualTo(1);

        mockMvc.perform(get("/employees/{id}", employeeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailReturns409AndDoesNotInsert() throws Exception {
        String requestJson =
                "{\"name\":\"Another Tanaka\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"TANAKA@example.com\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void duplicateEmailOnUpdateReturns409AndKeepsOriginalFields()
            throws Exception {
        String requestJson =
                "{\"name\":\"Tanaka Updated\","
                        + "\"department\":\"Support\","
                        + "\"email\":\"sato@example.com\"}";

        mockMvc.perform(put("/employees/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM employees WHERE id = ?",
                String.class, 1001L)).isEqualTo("Tanaka");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT department FROM employees WHERE id = ?",
                String.class, 1001L)).isEqualTo("Sales");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT email FROM employees WHERE id = ?",
                String.class, 1001L)).isEqualTo("tanaka@example.com");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees", Integer.class);
        assertThat(count).isEqualTo(2);
        assertThat(countChangeLogs(1001L)).isZero();
    }

    @Test
    void invalidRequestReturns400AndDoesNotInsert() throws Exception {
        String requestJson =
                "{\"name\":\"\","
                        + "\"department\":\"Sales\","
                        + "\"email\":\"bad\"}";

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.email").exists());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void updateCommitsEmployeeAndChangeLogTogether()
            throws Exception {
        String updateJson =
                "{\"name\":\"Tanaka\","
                        + "\"department\":\"Development\","
                        + "\"email\":\"tanaka@example.com\"}";

        mockMvc.perform(put("/employees/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        String department = jdbcTemplate.queryForObject(
                "SELECT department FROM employees WHERE id = 1001",
                String.class);
        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_change_logs "
                        + "WHERE employee_id = 1001 "
                        + "AND action = 'EMPLOYEE_UPDATED'",
                Integer.class);
        assertThat(department).isEqualTo("Development");
        assertThat(logCount).isEqualTo(1);
        String detail = jdbcTemplate.queryForObject(
                "SELECT detail FROM employee_change_logs "
                        + "WHERE employee_id = 1001",
                String.class);
        assertThat(detail).isEqualTo("department=Sales->Development");
    }

    @Test
    void updateRollsBackWhenChangeLogInsertFails()
            throws Exception {
        jdbcTemplate.execute("DROP TABLE employee_change_logs");
        String updateJson =
                "{\"name\":\"Tanaka\","
                        + "\"department\":\"Development\","
                        + "\"email\":\"tanaka@example.com\"}";

        mockMvc.perform(put("/employees/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isInternalServerError());

        String department = jdbcTemplate.queryForObject(
                "SELECT department FROM employees WHERE id = 1001",
                String.class);
        assertThat(department).isEqualTo("Sales");
    }

    private int countEmployee(long employeeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE id = ?",
                Integer.class,
                employeeId);
        return count == null ? 0 : count;
    }

    private int countChangeLogs(long employeeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_change_logs WHERE employee_id = ?",
                Integer.class,
                employeeId);
        return count == null ? 0 : count;
    }
}
```

`DROP TABLE` 只在第12章创建、并通过 `MySqlTestGuard` 检查的专用MySQL测试库中制造第二步失败，不得把这段测试连接到开发库、共享库或生产库。`@Sql` 会在下一个测试方法前重建两张表。第12章的PUT重复邮箱409回归测试也保留在完整类中；它额外确认失败后1001号员工的姓名、部门、邮箱和员工总数均不变，且没有虚假的 `EMPLOYEE_UPDATED` 履历。

`JdbcTemplate.execute(String sql)` 直接执行不需要返回结果集的SQL。本例用它删除测试库中的履历表，是为了稳定地制造“第一条UPDATE成功、第二条INSERT失败”；它不是业务代码，也不是生产环境的删表方案。其余测试中使用的 `queryForObject()` 则会执行查询并把单个结果转换为指定Java类型。

完整代码中的几个选择都来自业务规格：

- `beforeUpdate` 在UPDATE前保存旧记录，因为履历需要同时写出旧部门和新部门；更新后再查已经拿不到旧值。
- `ACTION_EMPLOYEE_UPDATED` 是不会随对象变化的固定操作代码，因此声明为 `private static final` 常量，避免在多处散写容易拼错的字符串。
- 履历INSERT必须恰好影响一行，并且MyBatis必须回填主键；任一条件不满足都作为系统异常抛出，不能假装接口成功。
- `created_at` 由数据库生成，本章写入后不立即返回履历详情，所以 `EmployeeChangeLog` 暂时不增加没有被代码使用的 `createdAt` 属性。
- 成功响应仍通过 `findById(id)` 读取数据库最终状态，保持第10章已经确定的响应规则。

## 十四、本章稳定状态

完成后，PUT会在同一事务中更新员工并写变更履历：

```text
src/main/java/com/example/employee/
├── entity/EmployeeChangeLog.java
├── mapper/EmployeeChangeLogMapper.java
└── service/impl/EmployeeServiceImpl.java
src/main/resources/mapper/EmployeeChangeLogMapper.xml
src/test/java/com/example/employee/
├── integration/EmployeeCrudIntegrationTest.java
└── service/EmployeeServiceImplTest.java
src/test/resources/test-data.sql
```

此时你应能够：

1. 区分单条SQL原子性和多步业务一致性；
2. 把commit、rollback和Service事务边界对应到真实数据变化；
3. 解释 `@Transactional` 的代理、外部调用和默认回滚规则；
4. 识别同类内部调用、吞掉异常和测试外层事务的风险；
5. 说明默认REQUIRED为何适合当前多步写入；
6. 区分事务和唯一约束的责任；
7. 用成功和中途失败测试的数据库状态证明提交与回滚。

需要深入事务原理与Spring事务规则时，进入[A13-1 事务原理与Spring事务机制](../appendix/A13_01_transaction_spring_mechanisms.md)；处理丢失更新、锁等待和死锁时查阅[A13-2 并发更新与锁](../appendix/A13_02_concurrent_updates_locks.md)；再到[A13-3 隔离级别与MVCC](../appendix/A13_03_isolation_mvcc.md)解释并发读取现象。附录实验不改变本章主线工程的稳定状态。需要复习本章测试使用的JUnit注解、生命周期和断言时，查阅第12章的[A12-3 JUnit Jupiter常用注解与方法](../appendix/A12_03_junit_jupiter_annotations_methods.md)。

下一章会在员工数量增多的前提下实现条件查询、稳定排序和分页，并继续用自动化测试保护旧有CRUD。
