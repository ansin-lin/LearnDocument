# 第9章 Service 与事务

> 本章目标：理解事务边界为什么通常放在 Service 层，并使用 `@Transactional` 保护写入操作。

## 一、事务解决什么问题

事务用于保证一组数据库操作要么全部成功，要么全部失败。

例如新增员工时，如果还需要同时写入员工权限，不能出现员工新增成功但权限新增失败的半完成状态。

在员工管理项目中，下面这些操作通常需要事务：

- 新增员工
- 修改员工
- 删除员工
- 新增员工同时写入关联信息
- 修改员工状态同时写入操作日志

查询接口通常不改变数据，不一定需要事务。

## 二、事务边界放在哪里

推荐放在 Service 层。

```text
Controller → Service(@Transactional) → Mapper → Database
```

原因：

- Controller 只处理 HTTP
- Mapper 只执行 SQL
- Service 承担业务流程，最适合作为事务边界

## 三、@Transactional 是什么

`@Transactional` 是 Spring 提供的事务注解。

它通常写在 Service 的 `public` 方法上。

方法开始执行时，Spring 开启事务。

方法正常结束时，Spring 提交事务。

方法抛出运行时异常时，Spring 回滚事务。

常用属性：

| 属性 | 可接受的值 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `rollbackFor` | 异常类型，例如 `Exception.class` | 运行时异常和 Error 回滚 | 指定哪些异常需要回滚 |
| `readOnly` | `true` / `false` | `false` | 标记是否只读事务 |
| `timeout` | 秒数，例如 `30` | 使用事务管理器默认值 | 设置事务超时时间 |

基础阶段最常用的是：

```java
@Transactional
```

如果希望受检异常也回滚，可以写：

```java
@Transactional(rollbackFor = Exception.class)
```

## 四、在项目中添加事务

修改文件：

```text
src/main/java/com/example/employee/service/EmployeeService.java
```

示例代码：

```java
package com.example.employee.service; // Service 所在包

import com.example.employee.dto.EmployeeCreateRequest; // 新增员工请求 DTO
import com.example.employee.dto.EmployeeResponse; // 员工响应对象
import com.example.employee.entity.Employee; // 员工实体类
import com.example.employee.exception.BusinessException; // 业务异常
import com.example.employee.mapper.EmployeeMapper; // 员工 Mapper
import org.springframework.stereotype.Service; // 声明当前类是 Service
import org.springframework.transaction.annotation.Transactional; // 导入事务注解

@Service // 把当前类交给 Spring 容器管理
public class EmployeeService { // 员工业务处理类

    private final EmployeeMapper employeeMapper; // Mapper 用于访问数据库

    public EmployeeService(EmployeeMapper employeeMapper) { // 构造器注入 Mapper
        this.employeeMapper = employeeMapper; // 保存 Mapper 对象
    }

    @Transactional // 新增员工属于写操作，发生异常时需要回滚
    public EmployeeResponse create(EmployeeCreateRequest request) { // 处理新增员工业务
        Employee employee = new Employee(); // 创建员工实体对象
        employee.setName(request.getName()); // 设置姓名
        employee.setDepartmentId(request.getDepartmentId()); // 设置部门 ID
        employee.setEmail(request.getEmail()); // 设置邮箱
        employee.setStatus("ACTIVE"); // 新增员工默认设置为在职

        int count = employeeMapper.insert(employee); // 执行 INSERT，返回影响行数
        if (count != 1) { // 正常新增一条员工时影响行数应该是 1
            throw new BusinessException("员工新增失败"); // 抛出业务异常，事务回滚
        }

        return EmployeeResponse.from(employee); // 把 Entity 转换为响应对象
    }
}
```

## 五、事务回滚示例

为了确认事务是否生效，可以临时在新增后抛出异常。

```java
@Transactional // 开启事务
public EmployeeResponse createForRollbackTest(EmployeeCreateRequest request) { // 回滚测试方法
    Employee employee = new Employee(); // 创建员工对象
    employee.setName(request.getName()); // 设置姓名
    employee.setDepartmentId(request.getDepartmentId()); // 设置部门 ID
    employee.setEmail(request.getEmail()); // 设置邮箱
    employee.setStatus("ACTIVE"); // 设置状态

    employeeMapper.insert(employee); // 先执行新增

    throw new BusinessException("模拟异常，确认事务回滚"); // 抛出异常，确认数据库不会留下数据
}
```

验证方式：

1. 调用测试接口或测试方法。
2. 确认接口返回错误。
3. 查询数据库，确认员工数据没有被插入。

测试完成后，应删除这个临时代码。

## 六、常见注意点

| 问题 | 说明 |
| --- | --- |
| `@Transactional` 放在 private 方法上 | 通常不会按预期生效 |
| 自己调用同类方法 | 可能绕过 Spring 代理 |
| 捕获异常后不抛出 | Spring 可能认为方法成功 |
| 只读查询加事务 | 普通查询不一定需要 |
| 把事务写在 Controller | Controller 责任过重，业务边界不清晰 |

## 七、Code Review 关注点

| 关注点 | 检查内容 |
| --- | --- |
| 事务位置 | 写操作事务是否放在 Service 层 |
| 异常处理 | 发生错误时是否抛出异常，而不是静默失败 |
| 影响行数 | INSERT / UPDATE / DELETE 是否检查影响行数 |
| 方法可见性 | 事务方法是否是 Spring 能代理的 `public` 方法 |
| 业务边界 | 一个事务中是否包含同一个业务动作需要保持一致的数据操作 |

## 八、本章练习

请完成：

1. 给新增、修改、删除方法添加事务。
2. 模拟新增后抛出异常，确认数据是否回滚。
3. 说明为什么事务不建议写在 Controller 层。
4. 修改员工邮箱时，如果影响行数不是 1，抛出业务异常。

## 九、本章总结

- 事务用于保证一组数据库操作的一致性。
- Spring Boot 项目中，事务通常写在 Service 层。
- `@Transactional` 正常结束提交，运行时异常回滚。
- 写操作建议检查影响行数。
- 捕获异常后不继续抛出，可能导致事务无法回滚。
