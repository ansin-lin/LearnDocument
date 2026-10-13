# 第9章 数据库记录怎样成为接口响应

> 本章目标：通过“数据库记录变成员工详情JSON”的连续案例，理解Spring Boot、DataSource、MyBatis、Mapper代理、Service和DTO转换怎样共同工作。

前几章的员工详情来自Service中的固定对象。本章只把 `GET /employees/{id}` 接入MySQL；列表和预览接口暂时保持第8章状态，第10章再完成全部CRUD。

## 一、开始状态与最终调用链

阅读与操作顺序：按案例建立表、配置连接、创建Entity与Mapper，直到详情接口真正从数据库返回数据；每一步先验证，再进入下一步。“章末完整代码”用于最后核对最终文件，不是案例9-01的起始工程，不要提前整体替换。

本章开始时，Controller已经依赖 `EmployeeService`，404和统一响应也已存在。完成后，详情请求沿下面的路线执行：

```text
Postman
  → EmployeeController
  → EmployeeService接口
  → EmployeeServiceImpl
  → EmployeeMapper代理
  → MyBatis读取EmployeeMapper.xml
  → DataSource取得数据库连接
  → MySQL执行SQL
  → Employee Entity
  → EmployeeResponse DTO
  → ApiResponse和HTTP 200或404
```

本章新增或替换的文件包括数据库脚本、Maven依赖、`application.yml`、Entity、Mapper接口、Mapper XML、Service接口和实现类。下面按数据库、连接配置、Entity、Mapper、Service、HTTP响应的顺序逐步建立；每一步说明当前能够验证的范围。

## 二、案例9-01：建立员工表与样例数据

第8章工程中的接口仍使用固定样例。本案例只在本地MySQL建立数据库、受限应用账号、员工表和两条样例数据；Java代码暂不修改。

### 建立数据库、账号、表和样例数据

以下脚本只适用于MySQL 8.0.16及以上版本的本地练习环境。先使用拥有建库和创建用户权限的管理账号，在MySQL Workbench等数据库客户端中执行：

```sql
CREATE DATABASE IF NOT EXISTS employee_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'employee_app'@'localhost'
    IDENTIFIED BY 'replace_with_local_password';

GRANT SELECT, INSERT, UPDATE, DELETE
ON employee_db.*
TO 'employee_app'@'localhost';

USE employee_db;

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT ck_employees_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
) AUTO_INCREMENT = 1001;

INSERT INTO employees (name, department, email)
VALUES
    ('Tanaka', 'Sales', 'tanaka@example.com'),
    ('Suzuki', 'Development', 'suzuki@example.com');
```

把 `replace_with_local_password` 换成本机练习账号的密码，不要把真实密码写入项目文件或提交记录。`CREATE USER IF NOT EXISTS` 遇到同名既有账号不会重设它的密码；若账号已存在，先由有权限人员确认它的来源和凭据，不能直接覆盖。应用账号只获得当前数据库的查询和增删改权限，不使用MySQL管理员账号运行应用。

如果 `employees` 表已经存在，`CREATE TABLE` 会失败。这是在阻止脚本意外覆盖已有表。不要为了省事在共享数据库执行 `DROP TABLE`；需要重做本地练习时，先确认表中数据可以丢弃并自行备份。

执行下面的查询，确认表和数据已经建立：

```sql
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
ORDER BY id;
```

首次创建时，前两条记录的编号应为1001和1002。如果数据库以前已经使用过该表，应以后续查询得到的实际编号为准。

### 表结构与字段说明

#### 1. 为什么先查数据库

Postman只能证明接口对外返回了什么；数据库客户端可以证明底层真实数据是什么。先创建 `employee_db.employees` 和应用账号，再使用应用账号执行：

```sql
SELECT id, name, department, email
FROM employees
WHERE id = 1001;
```

预期得到一行Tanaka员工。后面的接口响应必须与这行记录一致，才足以证明数据不是Service中的固定文本。

#### 2. 表字段与Java对象的边界

| 数据库列 | MySQL类型 | Java属性 | 用途 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | `Long id` | 主键 |
| `name` | `VARCHAR(50)` | `String name` | 姓名 |
| `department` | `VARCHAR(50)` | `String department` | 部门 |
| `email` | `VARCHAR(100)` | `String email` | 唯一邮箱 |
| `status` | `VARCHAR(20)` | `String status` | 状态 |
| `created_at` | `DATETIME` | `LocalDateTime createdAt` | 创建时间 |
| `updated_at` | `DATETIME` | `LocalDateTime updatedAt` | 更新时间 |

主键保证每行身份唯一，`UNIQUE` 约束保护邮箱不重复，`NOT NULL` 保护必要字段。应用层的友好校验不能替代数据库约束，数据库约束也不能替代接口层的清楚错误消息。

数据库账号只授予当前数据库需要的权限，不在应用中使用root。密码通过Eclipse运行配置中的环境变量传入，不写进Java类或提交到仓库。


部门列上限为50字符，与第8章DTO一致；允许值暂按现有Service的 `Sales`、`Development`、`Support`，正式业务规格待确认。数据库此时只限制长度，并没有部门枚举约束；业务允许值仍由Service判断。

验证：在数据库客户端使用应用账号查询编号1001和1002，分别核对 `Tanaka/Sales` 与 `Suzuki/Development`。如果本地表原先存在，以实际查询编号为准，不覆盖既有数据。

## 三、案例9-02：配置DataSource与MyBatis整合

在案例9-01基础上，在 `pom.xml` 的 `<dependencies>` 中追加MyBatis Starter和MySQL驱动；完整替换 `src/main/resources/application.yml`。其他Web与Validation依赖保留。当前还没有Mapper，故本案例只检查配置和应用启动，不能用员工详情响应证明数据库查询。

~~~xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
~~~

### 完整替换application.yml

文件位置：

```text
src/main/resources/application.yml
```

完整内容：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 10
```

在Eclipse中打开 **Run → Run Configurations...**，选择当前启动类对应的Java Application或Spring Boot App运行配置，在 **Environment** 页添加：

| Name | Value | 是否必需 |
| --- | --- | --- |
| `DB_USERNAME` | `employee_app` | 否；实际账号不同则必须设置 |
| `DB_PASSWORD` | 本机练习账号密码 | 是；无默认值，不写入文档或Git |
| `DB_URL` | `jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo` | 否；实际地址不同则必须设置 |

`DB_USERNAME` 和 `DB_URL` 的“否”仅表示本章配置有本地默认值，不保证其他数据库环境也适用；若实际账号或地址不同，就要显式设置。`DB_PASSWORD` 没有默认值，必须由本机运行配置提供。点击 **Apply** 保存到本机Eclipse运行配置，再用该配置启动应用。环境变量只提供给这个运行进程；不要把真实密码写进 `application.yml`、截图、测试证据或提交文件。更换电脑或Eclipse工作区后需要重新配置。

### 配置和数据源说明

#### 1. 三个依赖分别负责什么

- Spring Boot Web Starter继续提供Web应用和Spring容器；
- MyBatis Spring Boot Starter把MyBatis接入Spring Boot，创建会话相关对象并扫描Mapper；
- MySQL Connector/J是JDBC驱动，把标准JDBC调用转换为MySQL通信。

这些依赖不能互相替代。Starter不包含数据库服务器，驱动也不会替代MyBatis的SQL映射。

先抓住本案例必须理解的两组配置：`spring.datasource.*` 告诉Spring Boot连接哪个数据库、使用什么账号；`mybatis.mapper-locations` 告诉MyBatis到哪里查找XML。其他配置项的读取者与作用在第一次详情查询成功后再一起回看。

`${DB_PASSWORD}` 没有默认值，缺失时配置读取应失败。`${DB_URL:...}` 和 `${DB_USERNAME:employee_app}` 都表示变量不存在时使用冒号后的本地默认值；实际数据库地址或账号不同就必须设置对应变量。凭据不应硬编码在Java类中，因为环境不同、权限不同，而且源码可能被多人读取。

“创建DataSource对象”和“成功连接数据库”不是同一个检查点。缺少环境变量通常在配置阶段阻止启动；地址或账号问题可能在启动阶段出现，也可能在首次借用连接时出现。因此必须同时验证应用启动、数据库客户端连接和详情请求。


验证：先用数据库客户端确认应用账号能登录，再在Eclipse运行配置中设置 `DB_PASSWORD` 并启动应用。若启动阶段没有实际借用连接，启动成功只能证明配置已被读取；真实SQL连接要在后面的详情查询中确认。故意使用错误密码观察认证错误后，立即恢复正确密码。

## 四、案例9-03：建立Employee Entity

新建 `src/main/java/com/example/employee/entity/Employee.java`，其他文件保持上一案例状态。

### 新建Employee.java

文件位置：

```text
src/main/java/com/example/employee/entity/Employee.java
```

完整内容：

```java
package com.example.employee.entity;

import java.time.LocalDateTime;

public class Employee {

    private Long id;
    private String name;
    private String department;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

### Employee Entity的职责

`Employee` 表示数据库中的员工记录，因此包含表中的状态和审计时间。它不是请求DTO，也不是最终响应DTO。

```java
public class Employee {
    private Long id;
    private String name;
    private String department;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // getter和setter见上方完整文件
}
```

MyBatis读取结果后通过setter写入属性。Entity保留无参数构造能力。不要让Controller直接返回Entity，否则以后新增内部备注、删除标志或审计字段时可能意外公开。


`BIGINT` 对应Java `Long`，`DATETIME` 对应 `LocalDateTime`；Entity保留内部的 `status` 与审计时间，API响应DTO只公开接口规格允许的字段。此时尚未有Mapper结果，先在Eclipse检查类和导入可编译；数据库映射效果留到后续案例确认。

## 五、案例9-04：用Mapper接口和XML查询编号

在已有Entity和配置上，新建Mapper接口与XML。下面两个完整文件必须一起建立；`namespace`、方法名、参数名、结果映射要对应。

### 新建EmployeeMapper.java

文件位置：

```text
src/main/java/com/example/employee/mapper/EmployeeMapper.java
```

完整内容：

```java
package com.example.employee.mapper;

import com.example.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeMapper {

    Employee findById(@Param("id") Long id);
}
```

### 新建EmployeeMapper.xml

文件位置：

```text
src/main/resources/mapper/EmployeeMapper.xml
```

完整内容：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeMapper">

    <resultMap id="employeeResultMap" type="Employee">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="department" column="department"/>
        <result property="email" column="email"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="findById"
            parameterType="long"
            resultMap="employeeResultMap">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at,
            updated_at
        FROM employees
        WHERE id = #{id}
    </select>

</mapper>
```

### Mapper接口与XML如何对应

Java接口：

```java
@Mapper
public interface EmployeeMapper {
    Employee findById(@Param("id") Long id);
}
```

对应XML片段：

```xml
<mapper namespace="com.example.employee.mapper.EmployeeMapper">
    <select id="findById" resultMap="employeeResultMap">
        SELECT id, name, department, email, status, created_at, updated_at
        FROM employees
        WHERE id = #{id}
    </select>
</mapper>
```

上面的XML是教学片段，完整文件还必须包含 `employeeResultMap` 和DOCTYPE。对应关系是：

| Java/XML位置 | 必须对应 |
| --- | --- |
| 接口完整类名 | `<mapper namespace>` |
| `findById` 方法名 | `<select id>` |
| `@Param("id")` | `#{id}` |
| `Employee` 返回类型 | `employeeResultMap`映射结果 |

`@Mapper` 来自MyBatis。应用启动时，MyBatis扫描接口并创建运行时代理，再把代理注册为Spring Bean。`@Param("id")` 为SQL参数提供稳定名称。

`#{id}` 使用预编译参数绑定，形成类似 `WHERE id = ?` 的SQL；不要改成 `${id}`。后者直接插入文本，接收用户输入时可能改变SQL结构并造成SQL注入。

### Mapper代理如何产生

调用 `employeeMapper.findById(id)` 时，对象是MyBatis生成的代理：

```text
接口完整名和方法名
  → 找到namespace和select id
  → 绑定#{id}
  → 执行SQL
  → 按resultMap创建Employee
```

因此开发者不写Mapper实现类。这个代理对象仍是Spring Bean，所以可以通过构造器注入Service。它只负责数据访问，不判断“员工不存在应返回404”。


验证：在Eclipse重新启动应用，检查XML解析和Mapper Bean创建没有错误；此时Service尚未调用Mapper，不能把原有详情响应当成数据库查询证据。下一案例接通Service后再验证SQL结果。

## 六、案例9-05：把Service具体类改为接口和实现类

第8章的 `EmployeeService.java` 是带 `@Service` 的具体类，本案例必须完整替换同一路径文件为接口，再新建 `service/impl/EmployeeServiceImpl.java`。不要同时保留旧类和新接口。Controller原来依赖 `EmployeeService`，其构造器和接口路径不变。

### 完整替换EmployeeService.java

第8章的 `EmployeeService` 是一个具体类。本章把同一路径的文件完整替换为接口：

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse findById(Long id);

    List<EmployeeListItemResponse> findList(String department);

    String previewCreate(EmployeeCreateRequest request);
}
```

### 新建EmployeeServiceImpl.java

文件位置：

```text
src/main/java/com/example/employee/service/impl/EmployeeServiceImpl.java
```

完整内容：

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final List<EmployeeResponse> SAMPLE_EMPLOYEES = List.of(
            new EmployeeResponse(
                    1001L,
                    "Tanaka",
                    "Sales",
                    "tanaka@example.com"));

    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeMapper.findById(id);

        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }

        return toResponse(employee);
    }

    @Override
    public List<EmployeeListItemResponse> findList(String department) {
        List<EmployeeListItemResponse> results = new ArrayList<>();
        for (EmployeeResponse employee : SAMPLE_EMPLOYEES) {
            if (employee.getDepartment().equals(department)) {
                results.add(new EmployeeListItemResponse(
                        employee.getId(),
                        employee.getName(),
                        employee.getDepartment()));
            }
        }
        return results;
    }

    @Override
    public String previewCreate(EmployeeCreateRequest request) {
        if (!isAllowedDepartment(request.getDepartment())) {
            throw new InvalidDepartmentException(
                    request.getDepartment());
        }

        if ("used@example.com".equals(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        return request.getName()
                + " / " + request.getDepartment()
                + " / " + request.getEmail();
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getEmail());
    }

    private boolean isAllowedDepartment(String department) {
        return "Sales".equals(department)
                || "Development".equals(department)
                || "Support".equals(department);
    }
}
```

`findList()` 继续按固定样例筛选，`previewCreate()` 继续使用第8章的临时实现，确保既有列表、校验和异常练习仍然可以执行。此时只有 `findById()` 读取数据库；本章列表结果不反映数据库中的所有记录，第10章才将列表接入Mapper。不要把固定列表响应当成数据库查询证据。

### Service结构与注入说明

#### 1. Service为什么改为接口和实现类

第8章的 `EmployeeService` 是具体类。本章改为：

```text
EmployeeService       → 接口，声明业务能力
EmployeeServiceImpl   → 实现类，组织Mapper、业务判断和DTO转换
```

接口片段：

```java
public interface EmployeeService {
    EmployeeResponse findById(Long id);
    List<EmployeeListItemResponse> findList(String department);
    String previewCreate(EmployeeCreateRequest request);
}
```

实现类使用构造器注入：

```java
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }
}
```

`@Service` 由Spring处理，使实现类成为Bean。`implements` 要求实现接口方法，`@Override` 帮助编译器核对签名。Controller仍依赖 `EmployeeService` 接口；容器发现只有一个实现Bean时，会把它注入原来的构造器，所以Controller无需因实现方式变化而修改。

这种结构便于后续测试或替换实现，但不是所有Spring项目都必须机械建立“接口＋Impl”。

#### 2. 查询不到时仍沿用第7章404

```java
Employee employee = employeeMapper.findById(id);
if (employee == null) {
    throw new EmployeeNotFoundException(id);
}
```

Mapper用 `null` 表示没有记录，Service把它转换为具有业务意义的异常。第7章的全局异常处理器继续把异常转换为404，Mapper不需要理解HTTP。


调用关系为 `Controller → EmployeeService接口 → EmployeeServiceImpl → EmployeeMapper → MySQL`。`@Service` 只标注实现类；Spring找到唯一实现Bean后通过Controller构造器注入。实现类的 `findList` 和 `previewCreate` 仍保留第8章行为。

验证：重启应用，用Postman发送 `GET {{baseUrl}}/employees/1001`（无参数和Body）应返回200；请求不存在编号 `999999` 应返回404。此时详情已经接入数据库，列表和预览还未接入。

## 七、案例9-06：观察完整数据库调用链与DTO转换（不新增代码）

本案例不新增或修改Java文件，继续使用案例9-05完成的工程。通过Postman请求、Eclipse断点和代码追踪，观察数据库记录怎样成为HTTP响应。请在上个案例的 `EmployeeServiceImpl.findById` 与 `toResponse` 中定位下面两段，不要重复复制Service代码：

~~~java
Employee employee = employeeMapper.findById(id);
if (employee == null) {
    throw new EmployeeNotFoundException(id);
}
return toResponse(employee);
~~~

~~~java
private EmployeeResponse toResponse(Employee employee) {
    return new EmployeeResponse(
            employee.getId(), employee.getName(),
            employee.getDepartment(), employee.getEmail());
}
~~~

### Entity到响应DTO的说明

查询成功后，Service显式选择允许公开的字段：

```java
private EmployeeResponse toResponse(Employee employee) {
    return new EmployeeResponse(
            employee.getId(),
            employee.getName(),
            employee.getDepartment(),
            employee.getEmail());
}
```

`Employee` 表示数据库记录；`EmployeeResponse` 表示API契约。当前响应不公开 `status`、`createdAt` 和 `updatedAt`。这样数据库表增加内部字段时，不会自动改变接口响应。

转换通常放在Service或专门的转换组件中。本章规模较小，放在Service私有方法即可：

```text
Postman请求
  → EmployeeController.findById() 调用EmployeeService接口
  → Spring注入的EmployeeServiceImpl.findById()
  → EmployeeMapper代理调用MyBatis
  → DataSource提供连接，MyBatis执行SQL，MySQL返回数据行
  → MyBatis映射为Employee Entity
  → Service的toResponse()转换为EmployeeResponse
  → Controller包装为ApiResponse
  → HTTP 200
```

若MySQL没有该编号，Mapper返回 `null`，Service抛出 `EmployeeNotFoundException`，`GlobalExceptionHandler` 将它转换为HTTP 404。Mapper只负责数据访问，不决定HTTP状态。可在Eclipse给Controller、Service的 `findById()` 和 `toResponse()` 设置断点，观察正常与不存在编号各经过哪些位置。

Postman发送 `GET {{baseUrl}}/employees/1001`，预期200、`data.name=Tanaka`、`data.department=Sales`；响应不应出现 `status`、`createdAt`、`updatedAt`。再请求 `999999`，预期404。这条链的各层职责是：Mapper返回Entity或null，Service转换DTO或抛业务异常，Controller包装 `ApiResponse`，第7章Advice处理404。

### 第一次SQL成功后回看DataSource与配置

现在沿已运行的请求回看基础设施：`DataSource` 是 `javax.sql.DataSource` 接口，负责提供数据库连接。Spring Boot根据 `spring.datasource.*` 创建实现对象；本项目依赖环境下默认使用HikariCP连接池复用连接。

```text
application.yml
  → Spring Boot创建DataSource
  → MyBatis Starter创建SqlSessionFactory和SqlSessionTemplate
  → Mapper代理通过DataSource取得连接
  → JDBC Driver与MySQL通信
```

业务代码不需要手动调用 `DriverManager`，也不手动 `openSession()`、`commit()` 或 `close()`。本章还没有建立显式 `@Transactional` 事务边界；事务将在第13章处理。

| 配置项 | 读取者 | 当前作用 |
| --- | --- | --- |
| `spring.datasource.url` | Spring Boot | 指定JDBC地址和数据库 |
| `username`、`password` | Spring Boot | 提供数据库身份 |
| `driver-class-name` | Spring Boot | 指定MySQL JDBC驱动类 |
| `mybatis.mapper-locations` | MyBatis Starter | 查找Mapper XML |
| `mybatis.type-aliases-package` | MyBatis Starter | 扫描类型别名 |
| `map-underscore-to-camel-case` | MyBatis | 支持下划线列名与驼峰属性名 |
| `default-statement-timeout` | MyBatis | 为SQL提供默认超时秒数 |

`mybatis` 下的 `mapper-locations`、`type-aliases-package` 是Starter整合配置；`configuration` 下的值写入MyBatis核心 `Configuration` 对象。`map-underscore-to-camel-case: true` 支持自动映射时把 `created_at` 对应到 `createdAt`，但本章XML明确使用 `resultMap`，因此以其中写出的 `column` 和 `property` 为准。`default-statement-timeout: 10` 是SQL默认等待上限，不保证数据库一定在10秒内完成终止处理。

## 八、案例9-07：修改数据库并观察接口响应

1. 在数据库客户端读取并记录编号1001的原始姓名，同时在Postman发送 `GET {{baseUrl}}/employees/1001`，预期200；
2. 在数据库客户端把1001的姓名临时改为 `Tanaka Taro`；
3. 再次发送相同请求，响应姓名也应改变；
4. 把数据库姓名恢复为 `Tanaka`；
5. 请求不存在的 `999999`，预期404。

这个案例用两种证据相互确认：

- 数据库查询证明底层记录状态；
- Postman响应证明Controller、Service、Mapper、DataSource和MySQL组成的外部调用链。

本章列表接口仍返回固定样例，不能用它证明列表已经接入数据库。第10章才会替换列表、新增、修改和删除逻辑。

## 九、章末完整代码

以下文件是七个案例完成后的最终状态，用于逐文件核对。已经按案例完成的文件无需再次覆盖；不要让第8章的Service具体类与本章接口同时存在。

### 1. 建立数据库、账号、表和样例数据

以下脚本只适用于MySQL 8.0.16及以上版本的本地练习环境。先使用拥有建库和创建用户权限的管理账号，在MySQL Workbench或命令行客户端中执行：

```sql
CREATE DATABASE IF NOT EXISTS employee_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'employee_app'@'localhost'
    IDENTIFIED BY 'replace_with_local_password';

GRANT SELECT, INSERT, UPDATE, DELETE
ON employee_db.*
TO 'employee_app'@'localhost';

USE employee_db;

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT ck_employees_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
) AUTO_INCREMENT = 1001;

INSERT INTO employees (name, department, email)
VALUES
    ('Tanaka', 'Sales', 'tanaka@example.com'),
    ('Suzuki', 'Development', 'suzuki@example.com');
```

把 `replace_with_local_password` 换成本机练习账号的密码，不要把真实密码写入项目文件或提交记录。`CREATE USER IF NOT EXISTS` 遇到同名既有账号不会重设它的密码；若账号已存在，先由有权限人员确认它的来源和凭据，不能直接覆盖。应用账号只获得当前数据库的查询和增删改权限，不使用MySQL管理员账号运行应用。

如果 `employees` 表已经存在，`CREATE TABLE` 会失败。这是在阻止脚本意外覆盖已有表。不要为了省事在共享数据库执行 `DROP TABLE`；需要重做本地练习时，先确认表中数据可以丢弃并自行备份。

执行下面的查询，确认表和数据已经建立：

```sql
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
ORDER BY id;
```

首次创建时，前两条记录的编号应为1001和1002。如果数据库以前已经使用过该表，应以后续查询得到的实际编号为准。

### 2. 完整替换pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>employee-management-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>employee-management-api</name>
    <description>Employee management REST API</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.5</version>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. 完整替换application.yml

文件位置：

```text
src/main/resources/application.yml
```

完整内容：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 10
```

在Eclipse中打开 **Run → Run Configurations...**，选择当前启动类对应的Java Application或Spring Boot App运行配置，在 **Environment** 页添加：

| Name | Value | 是否必需 |
| --- | --- | --- |
| `DB_USERNAME` | `employee_app` | 否；实际账号不同则必须设置 |
| `DB_PASSWORD` | 本机练习账号密码 | 是；无默认值，不写入文档或Git |
| `DB_URL` | `jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo` | 否；实际地址不同则必须设置 |

点击 **Apply** 保存到本机Eclipse运行配置，再用该配置启动应用。环境变量只提供给这个运行进程；不要把真实密码写进 `application.yml`、截图、测试证据或提交文件。更换电脑或Eclipse工作区后需要重新配置。

### 4. 新建Employee.java

文件位置：

```text
src/main/java/com/example/employee/entity/Employee.java
```

完整内容：

```java
package com.example.employee.entity;

import java.time.LocalDateTime;

public class Employee {

    private Long id;
    private String name;
    private String department;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

### 5. 新建EmployeeMapper.java

文件位置：

```text
src/main/java/com/example/employee/mapper/EmployeeMapper.java
```

完整内容：

```java
package com.example.employee.mapper;

import com.example.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeMapper {

    Employee findById(@Param("id") Long id);
}
```

### 6. 新建EmployeeMapper.xml

文件位置：

```text
src/main/resources/mapper/EmployeeMapper.xml
```

完整内容：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeMapper">

    <resultMap id="employeeResultMap" type="Employee">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="department" column="department"/>
        <result property="email" column="email"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="findById"
            parameterType="long"
            resultMap="employeeResultMap">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at,
            updated_at
        FROM employees
        WHERE id = #{id}
    </select>

</mapper>
```

### 7. 完整替换EmployeeService.java

第8章的 `EmployeeService` 是一个具体类。本章把同一路径的文件完整替换为接口：

```java
package com.example.employee.service;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse findById(Long id);

    List<EmployeeListItemResponse> findList(String department);

    String previewCreate(EmployeeCreateRequest request);
}
```

### 8. 新建EmployeeServiceImpl.java

文件位置：

```text
src/main/java/com/example/employee/service/impl/EmployeeServiceImpl.java
```

完整内容：

```java
package com.example.employee.service.impl;

import com.example.employee.dto.request.EmployeeCreateRequest;
import com.example.employee.dto.response.EmployeeListItemResponse;
import com.example.employee.dto.response.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidDepartmentException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final List<EmployeeResponse> SAMPLE_EMPLOYEES = List.of(
            new EmployeeResponse(
                    1001L,
                    "Tanaka",
                    "Sales",
                    "tanaka@example.com"));

    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeMapper.findById(id);

        if (employee == null) {
            throw new EmployeeNotFoundException(id);
        }

        return toResponse(employee);
    }

    @Override
    public List<EmployeeListItemResponse> findList(String department) {
        List<EmployeeListItemResponse> results = new ArrayList<>();
        for (EmployeeResponse employee : SAMPLE_EMPLOYEES) {
            if (employee.getDepartment().equals(department)) {
                results.add(new EmployeeListItemResponse(
                        employee.getId(),
                        employee.getName(),
                        employee.getDepartment()));
            }
        }
        return results;
    }

    @Override
    public String previewCreate(EmployeeCreateRequest request) {
        if (!isAllowedDepartment(request.getDepartment())) {
            throw new InvalidDepartmentException(
                    request.getDepartment());
        }

        if ("used@example.com".equals(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        return request.getName()
                + " / " + request.getDepartment()
                + " / " + request.getEmail();
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getEmail());
    }

    private boolean isAllowedDepartment(String department) {
        return "Sales".equals(department)
                || "Development".equals(department)
                || "Support".equals(department);
    }
}
```

`findList()` 继续按固定样例筛选，`previewCreate()` 继续使用第8章的临时实现，确保既有列表、校验和异常练习仍然可以执行。此时只有 `findById()` 读取数据库；本章列表结果不反映数据库中的所有记录，第10章才将列表接入Mapper。不要把固定列表响应当成数据库查询证据。

## 十、本章综合验证

### 1. 先验证数据库

使用应用账号连接后执行：

```sql
SELECT id, name, department, email
FROM employees
WHERE id = 1001;
```

确认能查到目标记录。如果实际编号不是1001，记下真实编号。

### 2. 在Eclipse中启动

确认Eclipse运行配置包含必需的 `DB_PASSWORD`；仅当实际账号或地址不同于本地默认值时添加 `DB_USERNAME` 或 `DB_URL`。保存代码后从该运行配置启动应用。先确认Console中出现Spring Boot启动成功信息；连接池是否立即建立连接取决于实际初始化时机，不能把某一条连接池日志当成唯一通过条件。若启动时报配置或驱动错误，先修复再发请求；若启动成功但详情请求报连接错误，则按第十一节检查数据库服务、URL和账号。

### 3. 请求数据库详情

在Postman中把编号替换为数据库中实际存在的值：

| 项目 | 内容 |
| --- | --- |
| HTTP方法 | GET |
| URL | `{{baseUrl}}/employees/1001` |
| 查询参数 | 无 |
| 请求体 | 无 |
| 预期状态码 | 200 |
| 预期响应 | `data`中的姓名、部门和邮箱与数据库记录一致 |

随后在数据库客户端临时修改这行员工的姓名：

```sql
UPDATE employees
SET name = 'Tanaka Taro'
WHERE id = 1001;
```

再次在Postman点击 **Send**，响应姓名也应改变。这证明详情不再来自Service中的固定文本。完成验证后在数据库客户端恢复样例值：

```sql
UPDATE employees
SET name = 'Tanaka'
WHERE id = 1001;
```

### 4. 验证不存在记录和既有接口

在Postman完成不存在记录和既有接口回归：

| HTTP方法 | URL | Params | 请求体 | 预期状态与响应 |
| --- | --- | --- | --- | --- |
| GET | `{{baseUrl}}/employees/999999` | 无 | 无 | 404，员工不存在结构 |
| GET | `{{baseUrl}}/employees` | `department=Sales` | 无 | 200，返回固定样例Tanaka；此接口尚未读取数据库 |
| POST | `{{baseUrl}}/employees/preview` | 无 | `{"name":"Sato","department":"Development","email":"sato@example.com"}` | 200，预览成功 |
| POST | `{{baseUrl}}/employees/preview` | 无 | `{"name":"Sato","department":"Other","email":"sato@example.com"}` | 400，非法部门消息 |
| POST | `{{baseUrl}}/employees/preview` | 无 | `{"name":"Sato","department":"Development","email":"used@example.com"}` | 409，邮箱冲突消息 |
| GET | `{{baseUrl}}/health` | 无 | 无 | 200，正文 `OK` |

三个POST请求的Body均选择 **raw → JSON**，并确认 `Content-Type: application/json`。

完成后将数据库记录、Mapper执行结果和HTTP响应三处证据放在一起核对；本章列表仍是固定样例，不作为数据库查询证据。

## 十一、按阶段排查数据库问题

| 现象 | 所在阶段 | 常见原因 | 检查和修正 |
| --- | --- | --- | --- |
| 提示无法解析 `DB_PASSWORD` | 配置读取 | Eclipse运行配置中未设置环境变量 | 在当前启动类的Run Configuration中添加变量并重启 |
| 提示找不到MyBatis核心配置 | 配置读取 | `config-location` 路径错误，且启用了存在检查 | 核对 `src/main/resources` 下的位置和 `classpath:` 路径 |
| 创建 `SqlSessionFactory` 时提示两种配置并存 | MyBatis自动配置 | 同时写了 `config-location` 和 `configuration` | 二选一，删除另一套核心设置来源 |
| `Access denied for user` | 建立连接时的数据库认证；可能在启动或首次查询出现 | 账号、密码、主机范围或权限错误 | 用应用账号单独登录并检查授权，不改用root绕过 |
| `Unknown database` | 建立连接 | 数据库名错误或建库脚本未执行 | 在MySQL中检查 `employee_db` |
| 连接被拒绝或超时 | 网络与MySQL服务 | 服务未启动、端口或地址错误 | 先用数据库客户端连接同一地址 |
| `Invalid bound statement` | 首次调用对应Mapper方法时的语句定位 | XML未扫描、namespace或id不一致 | 逐项核对资源路径、接口完整名和方法名 |
| `Table ... doesn't exist` | SQL执行 | 当前数据库错误或表未创建 | 查询当前数据库并检查表名 |
| `Unknown column` | SQL执行 | SQL列名与DDL不一致 | 对照数据字典和实际表结构 |
| 属性为 `null`但列有值 | 结果映射 | `resultMap`的column或property写错 | 对照列名、Java属性和setter |
| 应返回一条却查询到多条 | SQL与约束 | 查询条件不唯一 | 使用主键或唯一约束，检查数据 |

排错顺序应是“配置读取→建立连接→找到Mapper语句→执行SQL→结果映射→业务转换”。异常堆栈通常从外层框架异常开始，真正原因常在最后一个 `Caused by` 附近。

## 十二、规格理解、影响调查与操作练习

### 练习1：建立字段追踪表

从数据字典出发，整理“数据库列→Entity属性→响应字段”。至少说明 `email` 为什么不出现在列表响应，以及 `status`、`created_at`、`updated_at` 为什么停在Entity。

### 练习2：制造并修复Mapper对应错误

临时把XML中的 `id="findById"` 改为 `id="findOne"`，请求详情并记录异常根因；恢复方法名后重新验证200。不能通过新增一个无意义接口方法掩盖不一致。

### 练习3：观察结果映射错误

临时把 `createdAt` 的 `property` 改为错误名称，启动或请求后记录错误位置；恢复后使用调试器检查 `Employee.createdAt` 有值。不要把Entity直接返回给接口来观察内部字段。

### 练习4：完成一项影响调查

改修要求：“邮箱最大长度从100改为150，数据库和API都要支持。”本练习只调查，不执行结构变更。至少检查：

```text
接口字段规格
→ EmployeeCreateRequest的@Size
→ employees.email列
→ 数据字典
→ Employee属性类型
→ Mapper查询列与resultMap
→ EmployeeResponse
→ 边界值和数据库写入测试
```

对无需修改的文件也写明“确认无影响”的理由，不能只列要改的文件。

### 练习5：Review与自测证据

Review指摘：“为了减少类，详情和列表都直接返回Employee。”列出可能泄露的字段，并说明为何应保留两个响应类型。

自测证据至少包含：数据库产品与版本、执行的SELECT、实际员工编号、请求URL、预期状态、实际状态、响应关键字段、判定和恢复操作。不得记录数据库密码。

## 十三、本章稳定状态

完成临时练习并恢复代码与样例数据后，新增目录应为：

```text
src/main/java/com/example/employee/
├── entity/Employee.java
├── mapper/EmployeeMapper.java
└── service/
    ├── EmployeeService.java
    └── impl/EmployeeServiceImpl.java

src/main/resources/
├── application.yml
└── mapper/EmployeeMapper.xml
```

此时你应能够：

1. 根据表定义和数据字典核对数据库列、Java属性与API字段；
2. 区分JDBC、驱动、DataSource、连接池和MyBatis的职责；
3. 配置数据库连接而不把密码写进项目文件；
4. 解释Mapper接口、XML、参数和结果映射的对应关系；
5. 解释 `#{}`为何用于请求参数，以及 `${}`的风险边界；
6. 区分项目编写的Service实现与MyBatis生成的Mapper代理；
7. 把Entity明确转换为响应对象，并用404处理空查询结果；
8. 按连接、SQL定位、执行、映射和业务转换的顺序排查问题；
9. 说明当前 `DATETIME` 与 `LocalDateTime` 都不携带时区，并知道何时进入日期时间附录继续学习。

下一章会在这条稳定数据访问链上实现真实新增、修改和删除，并用数据库状态验证每次写操作。

## 十四、附录：既存项目中的其他做法

以下内容用于阅读既存项目，不改变本章 `application.yml`、Mapper和Service主线代码。

### A. 独立mybatis-config.xml


有些既存项目把MyBatis核心设置放在 `mybatis-config.xml` 中。Spring Boot不会仅凭文件名自动读取它，必须通过 `mybatis.config-location` 指定资源位置。`mybatis.configuration.*` 和 `mybatis.config-location` 是两种核心设置来源，不能同时使用；`mapper-locations` 和 `type-aliases-package` 仍可保留在 `application.yml` 中。

当前主线继续使用结构更直观的 `mybatis.configuration.*`。独立XML的完整文件、配置切换、`<environments>`差异、故障观察和恢复步骤见附录[Spring Boot怎样读取独立MyBatis配置](../appendix/A12_springboot_mybatis_config.md)。

### B. DAO、Repository和Mapper命名

它们通常都是“数据访问代码”的命名，不表示必须再创建三个依次调用的架构层：

- `DAO` 是Data Access Object的通用叫法，强调封装数据访问。
- `Repository` 常见于领域设计或Spring Data项目，语义偏向对象集合；不同框架对它的实现方式不同。
- `Mapper` 是MyBatis项目的常见名称，强调方法、SQL参数和结果之间的映射。

本项目使用MyBatis，因此统一采用 `mapper` 包和 `EmployeeMapper`。不要再创建内容完全相同的 `EmployeeDao`和 `EmployeeRepository`，否则只会增加转发代码。阅读既有日本项目时，应先看接口、SQL和调用关系，再判断名称实际承担什么职责。

### C. 日期时间映射的进阶边界

Employee表的 `created_at`、`updated_at` 使用MySQL `DATETIME`，Entity使用Java 17的 `LocalDateTime`。两者都表示日期和时间，但都不携带时区或UTC偏移；当前项目把它们定义为日本业务环境中的数据库本地时间。

这只是当前项目规格，不是所有系统的通用答案。跨国家事件、审计时间或多地区API需要另外确定UTC、区域时区和接口格式，不能看到 `LocalDateTime` 就默认它代表东京时间。`DATETIME`、`TIMESTAMP`、`LocalDateTime`、`OffsetDateTime`、`Instant` 及Browser到数据库的转换与验证，统一放在附录[Java Web项目中的日期时间与时区](../appendix/A11_java_web_datetime.md)。

本章只需确认MyBatis能够把两列写入Entity对应属性；它们不会进入当前详情响应。任何时间列类型或语义变更，都必须同时调查表定义、既有数据、Mapper映射、JDBC配置和接口规格。
