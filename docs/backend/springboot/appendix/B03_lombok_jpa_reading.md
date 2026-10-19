# B03 Lombok与Spring Data JPA识读

本附录帮助你进入使用Lombok或JPA的既存项目。第18章稳定状态仍使用显式Java代码和MyBatis；下面的示例是独立识读，不要求加入Employee主线。

## 一、Lombok在编译期生成代码

Lombok是编译期代码生成工具。源码中没有手写的方法，编译后的类中可能存在，因此阅读时要同时检查注解、字段修饰符、构建依赖和IDE的annotation processing状态。

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
}
```

`lombok.RequiredArgsConstructor` 会为需要初始化的字段生成构造器。上例在概念上相当于：

```java
public EmployeeService(EmployeeMapper employeeMapper) {
    this.employeeMapper = employeeMapper;
}
```

所以它仍然是构造器注入，不是字段注入。存在多个同类型Bean时，仍要按项目设计处理Qualifier或Primary，Lombok不会替Spring选择Bean。

再看普通DTO：手写 `private String name; public String getName(){ return name; } public void setName(String name){ this.name=name; }`，可在声明字段的类上写 `@Getter @Setter` 让Lombok在编译时概念上生成这两个方法。它不在运行时拦截getter，也不会自动加业务校验。将示例放入已引入Lombok的**既存项目副本**编译，用IDE反编译class或查看编译产物；主线项目不因此增加Lombok依赖。

这里两个类级注解由不同机制处理：`@Service` 属于Spring，应用启动进行组件扫描时把业务类注册为Bean；`@RequiredArgsConstructor` 属于Lombok，在Java编译期生成构造器，Spring随后使用这个构造器注入 `EmployeeMapper`。一个负责“对象由谁管理”，另一个负责“源码中省略哪段构造器代码”。

## 二、常见Lombok注解

以下注解都属于Lombok，由编译器的annotation processing在编译期处理；它们不是Spring运行时功能。

| 注解 | 可写位置与常见生成内容 | 既存项目阅读重点 |
| --- | --- | --- |
| `@Getter` | 类或字段；生成getter | 检查访问级别；类级写法通常覆盖全部合适字段 |
| `@Setter` | 类或字段；生成setter | final字段不会得到普通setter |
| `@NoArgsConstructor` | 类；生成无参构造器 | 检查final字段、强制初始化选项和框架要求 |
| `@AllArgsConstructor` | 类；生成包含所有字段的构造器 | 字段顺序变化会影响参数顺序 |
| `@RequiredArgsConstructor` | 类；生成包含final字段和带 `@NonNull` 且未初始化字段的构造器 | 常用于Spring构造器注入 |
| `@NonNull` | 字段、参数等；让Lombok生成空值检查，并影响required constructor | 它不是Jakarta Validation的 `@NotNull`；后者由Validator执行“值不能为null”的约束，因此Lombok注解不能代替HTTP请求校验 |
| `@Builder` | 类、构造器或方法；生成Builder API | 对象构建方式，不是Spring功能，也不会自动校验 |
| `@Data` | 类；组合生成getter、非final字段setter、`toString`、`equals`、`hashCode`和required constructor相关能力 | 不只是Getter+Setter，具体行为按Lombok版本定义确认 |

Builder识读示例：

```java
EmployeeResponse response = EmployeeResponse.builder()
        .id(employee.getId())
        .name(employee.getName())
        .build();
```

`builder()`、链式字段方法和 `build()` 由Lombok生成，用来分步骤构造对象。它不负责Validation、权限或业务完整性；一个Builder创建出的对象仍可能缺少业务必填值。

## 三、Lombok的Review边界

在数据库Entity上直接使用 `@Data` 可能让 `equals`、`hashCode` 或 `toString` 包含不适合的字段。对象关系、代理对象、可变主键、大字段、密码和个人信息都要按项目约定审查。自动生成的 `toString` 也可能把敏感值写入日志。

例如 `Employee` 有 `passwordHash`、`department` 关系和数据库生成的 `id`：`log.info("{}", employee)` 可能通过生成的 `toString()` 输出哈希或遍历关联；保存前后id变化又可能改变 `hashCode()`，使对象在HashSet中无法按预期查找。JPA实体通常保留供框架使用的无参构造器，且关联字段、代理与相等性要按项目约定单独设计；不要无审查地给Entity添加 `@Data`。Review要检查生成方法实际包含哪些字段，必要时只用 `@Getter`/局部 `@Setter` 或显式方法。

IDE标红但命令行构建成功时，检查插件和annotation processing；命令行也失败时，再查Lombok版本、依赖scope、编译器配置和手写构造器冲突。不要为了减少行数统一替换既存类。

## 四、JPA从Entity映射开始

```java
package com.example.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    protected Employee() {
    }

    // 业务需要的构造器、getter和方法省略
}
```

这些注解都属于Jakarta Persistence。JPA在建立持久化单元和处理实体状态时读取映射信息，而不是由Spring MVC处理。

| 注解 | 可写位置 | 作用与本例效果 |
| --- | --- | --- |
| `@Entity` | 实体类 | 表示该类由JPA作为持久化实体管理 |
| `@Table` | 实体类 | 指定表名 `employees`；省略时按实现和命名策略推断 |
| `@Id` | 字段或getter | 标记主键属性 |
| `@GeneratedValue` | 主键字段或getter | 声明主键生成策略；本例使用数据库自增的 `IDENTITY` |
| `@Column` | 字段或getter | 指定列名、可空性、长度等映射信息；本例把 `name` 映射为非空且最长100 |

Boot 3和Boot 4项目使用 `jakarta.persistence.*`；Boot 2旧项目常见 `javax.persistence.*`。这些不是Spring MVC请求注解。

## 五、Repository怎样出现

```java
package com.example.employee.repository;

import com.example.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {
}
```

`Employee` 是Entity类型，`Long` 是主键类型。Spring Data在运行时提供Repository实现，常见基础方法包括：

```java
employeeRepository.findById(id);
employeeRepository.findAll();
employeeRepository.save(employee);
employeeRepository.deleteById(id);
```

`findById` 返回 `Optional<Employee>`。`save` 可能表示新增或更新，实际SQL和执行时机受实体状态和事务影响，不能把每次Java方法调用简单等同为立刻执行一条固定SQL。

### 为什么方法调用不等于立即发SQL

`EntityManager` 管理的 **Persistence Context（持久化上下文）** 是当前工作单元里实体的管理范围；从Repository读取的实体通常成为 **managed entity**。在事务内修改它的字段，JPA实现可在 **flush** 时通过 **dirty checking（脏检查）** 比较状态并生成UPDATE；flush通常发生在提交前，也可能因查询或显式调用而提前发生。`save()` 对新实体和已有实体的处理路径不同，数据库生成主键等策略也会影响INSERT时机。因此调查时要同时看实体状态、事务边界和SQL日志，不把 `save()` 行数当成SQL行数。事务决定提交/回滚，flush仅把待执行变更同步到数据库连接，不等于提交。

最小识读片段（既存JPA项目的Service方法体，不是主线可直接粘贴代码）：

```java
@Transactional
public void rename(Long id, String name) {
    Employee employee = employeeRepository.findById(id).orElseThrow();
    employee.changeName(name); // managed实体变化；可在flush/提交前发UPDATE
}
```

应找到 `changeName`、Entity映射、Repository和事务管理器，再打开测试环境SQL日志验证实际语句。若对象已脱离管理或使用只读事务，行为可能不同；不要仅凭片段断言一定更新。

`@Repository` 属于Spring，通常写在数据访问实现类上，由组件扫描在启动时注册Bean，并可参与Spring的数据访问异常转换。继承Spring Data Repository的接口由Spring Data扫描并在运行时生成代理实现，因此通常不需要显式写 `@Repository`；“没写注解”不等于没有Repository Bean。

## 六、MyBatis与JPA对比

| 项目 | MyBatis | Spring Data JPA |
| --- | --- | --- |
| 核心思路 | SQL中心 | Entity/ORM中心 |
| SQL | 开发者经常明确编写 | 很多基础SQL由ORM生成 |
| 数据入口 | Mapper | Repository |
| 映射 | resultMap、别名等 | Entity Mapping |
| 当前课程 | 主线 | 既存项目识读 |

两者都需要数据库约束、事务、索引、权限、测试和日志。项目也可能同时使用JPA与MyBatis，必须调查一个业务用例实际走哪条路径。

同一“按ID读取并修改姓名”的调用对照：MyBatis中 `Service → EmployeeMapper.findById → 明写SELECT → 修改对象 → EmployeeMapper.updateName → 明写UPDATE`；JPA中 `Service → EmployeeRepository.findById → Entity受管理 → 修改属性 → flush/事务提交阶段生成UPDATE`。两者的Service都应明确业务规则与事务边界，但“修改Java对象后是否自动写库”不同。用测试环境SQL日志和数据库查询验证，不能只看Controller返回200。

### N+1 的最小日志实验

假设 `Employee` 关联 `Department`，查询10名员工后循环读取 `employee.getDepartment().getName()`。如果关联按需加载，可能先出现1条员工列表SELECT，再出现10条按部门ID查询的SELECT；即使Java只调用一次 `findAll()`，SQL也可能有11条。日志中的重复 `select ... from departments where id=?` 是调查线索，实际条数仍由映射、缓存和数据重复情况决定。先在测试环境记录SQL条数、关联映射和访问代码，再考虑查询方案；本章不要求实现EntityGraph或缓存优化。

```text
-- 教学模拟日志，不是本仓库实测输出
SELECT id, name, department_id FROM employees;             -- 第1条
SELECT id, name FROM departments WHERE id = ?;             -- 员工1触发
SELECT id, name FROM departments WHERE id = ?;             -- 员工2触发
...                                                       -- 继续重复
```

验收时提交一次真实测试环境的SQL条数和调用栈，说明重复SELECT由哪个关联访问触发；不能只凭“页面慢”断言一定是N+1。

本附录不展开Hibernate内部状态机、高级Lazy Loading、一级/二级缓存、Entity Graph和复杂N+1专题。遇到这些内容时按项目版本登记专项调查，不在基础识读中猜测。

## 七、识读练习

### JPA SQL观察单：代码识读示例，未在本仓库执行

本仓库没有独立JPA实验工程，以下是给接手**确实使用JPA的既存项目**时的观察步骤，不能算本教材实测。前置：授权的测试库、可回滚的专用员工数据、SQL日志已启用，已确认Repository和事务配置。先记录 `findById(1001)` 是否发SELECT，再在同一事务内修改Managed Entity的姓名；在显式 `flush()` 前后各截取SQL日志，确认UPDATE出现的时机。随后分别提交和故意抛异常回滚，另开事务查询最终姓名：flush发出SQL不等于commit，rollback仍应撤销。再查询员工列表并逐个访问department，数重复SELECT，连同映射和缓存状态解释是否N+1。最后对Lombok类用IDE生成代码视图或编译产物检查无参构造器/getter；`@Data` 同时带来的 `equals`/`hashCode`/`toString` 需检查懒加载与递归关系。

提交物：每一步的代码位置、脱敏SQL片段、事务结果及“已确认/待确认”标记。对照MyBatis：Java对象字段变化通常不会自行生成UPDATE，必须查显式Mapper调用；JPA受管理对象在脏检查/flush阶段可能生成UPDATE。完成后回滚或删除**仅限专用测试数据**，关闭临时详细SQL日志，保留无个人信息的证据；没有测试库时仅交代码识读报告，不填写虚构SQL条数。

1. 阅读一个带 `@Data`、`@Builder`、`@RequiredArgsConstructor` 的类，写出预计生成的能力，再用编译结果验证。
2. 把第9章“按id读取员工”的MyBatis调用链画成JPA识读图，列出Entity、Repository、Service和测试。
3. 对一个同时存在Mapper和Repository的Service做影响调查，说明事务、异常和测试分别覆盖哪条数据访问路径。
