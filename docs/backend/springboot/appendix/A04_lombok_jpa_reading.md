# Appendix D：Lombok与Spring Data JPA识读

本附录帮助你进入使用Lombok或JPA的既存项目。第20章稳定状态仍使用显式Java代码和MyBatis；下面的示例是独立识读，不要求加入Employee主线。

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

## 二、常见Lombok注解

| 注解 | 常见生成内容 | 既存项目阅读重点 |
| --- | --- | --- |
| `@Getter` | getter | 可写在类或字段，检查访问级别 |
| `@Setter` | setter | final字段不会得到普通setter |
| `@NoArgsConstructor` | 无参构造器 | 检查final字段和框架要求 |
| `@AllArgsConstructor` | 包含所有字段的构造器 | 字段顺序变化会影响参数顺序 |
| `@RequiredArgsConstructor` | final字段和带`@NonNull`且未初始化字段的构造器 | 常用于Spring构造器注入 |
| `@Builder` | Builder API | 对象构建方式，不是Spring功能，也不会自动校验 |
| `@Data` | getter、非final字段setter、`toString`、`equals`、`hashCode`和required constructor相关能力 | 不只是Getter+Setter，具体行为按Lombok版本定义确认 |

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

| 注解 | 所属 | 作用 |
| --- | --- | --- |
| `@Entity` | Jakarta Persistence | 表示该类由JPA作为持久化实体管理 |
| `@Table` | Jakarta Persistence | 指定表名；省略时按实现和命名策略推断 |
| `@Id` | Jakarta Persistence | 标记主键 |
| `@GeneratedValue` | Jakarta Persistence | 声明主键生成策略 |
| `@Column` | Jakarta Persistence | 指定列名、可空性、长度等映射信息 |

Boot 3项目使用 `jakarta.persistence.*`；Boot 2旧项目常见 `javax.persistence.*`。这些不是Spring MVC请求注解。

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

`findById` 返回 `Optional<Employee>`。`save` 可能表示新增或更新，实际SQL和执行时机受实体状态和事务影响，不能把每次Java方法调用简单等同为立刻执行一条固定SQL。继承Spring Data Repository的接口通常可被自动发现，不一定显式写 `@Repository`。

## 六、MyBatis与JPA对比

| 项目 | MyBatis | Spring Data JPA |
| --- | --- | --- |
| 核心思路 | SQL中心 | Entity/ORM中心 |
| SQL | 开发者经常明确编写 | 很多基础SQL由ORM生成 |
| 数据入口 | Mapper | Repository |
| 映射 | resultMap、别名等 | Entity Mapping |
| 当前课程 | 主线 | 既存项目识读 |

两者都需要数据库约束、事务、索引、权限、测试和日志。项目也可能同时使用JPA与MyBatis，必须调查一个业务用例实际走哪条路径。

本附录不展开Hibernate内部状态机、高级Lazy Loading、一级/二级缓存、Entity Graph和复杂N+1专题。遇到这些内容时按项目版本登记专项调查，不在基础识读中猜测。

## 七、识读练习

1. 阅读一个带 `@Data`、`@Builder`、`@RequiredArgsConstructor` 的类，写出预计生成的能力，再用编译结果验证。
2. 把第9章“按id读取员工”的MyBatis调用链画成JPA识读图，列出Entity、Repository、Service和测试。
3. 对一个同时存在Mapper和Repository的Service做影响调查，说明事务、异常和测试分别覆盖哪条数据访问路径。
