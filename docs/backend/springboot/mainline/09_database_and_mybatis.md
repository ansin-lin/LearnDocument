# 第9章 数据库记录怎样成为接口响应

> 本章目标：为员工详情接口接入MySQL 8.0和MyBatis，使 `GET /employees/{id}` 返回真实数据库记录，并能解释表、Entity、Mapper、Service和响应对象之间的数据转换。

第8章结束时，Controller、DTO校验、业务规则和统一异常响应已经能够工作，但员工详情仍由Service写死返回。数据库中的一行记录不会自动成为JSON；程序需要建立下面这条链：

```text
MySQL一行数据
  → Employee Entity
  → EmployeeMapper
  → EmployeeServiceImpl
  → EmployeeResponse
  → Controller与HTTP响应
```

本章只把“按编号查询详情”接入数据库。列表和新增预览继续保持第8章实现；新增、修改和删除将在下一章接入数据库。

## 一、开始状态与本章改动

开始前应完成第8章，并保留其中的Controller、DTO、响应包装和异常处理。本章需要：

```text
项目根目录/
├── pom.xml                                           ← 完整替换
└── src/main/
    ├── java/com/example/employee/
    │   ├── entity/Employee.java                     ← 新建
    │   ├── mapper/EmployeeMapper.java                ← 新建
    │   └── service/
    │       ├── EmployeeService.java                  ← 由类改为接口
    │       └── impl/EmployeeServiceImpl.java         ← 新建实现类
    └── resources/
        ├── application.yml                           ← 完整替换
        └── mapper/EmployeeMapper.xml                 ← 新建
```

`EmployeeController` 不需要修改。它仍然依赖 `EmployeeService`，只是从本章开始，Spring注入的实际对象变为 `EmployeeServiceImpl`。

## 二、完整示例

先完成本节全部文件，再从第三节开始逐项理解第一次出现的数据库、配置、对象和方法。

### 1. 建立数据库、账号、表和样例数据

以下脚本只适用于MySQL 8.0本地练习环境。先使用拥有建库和创建用户权限的管理账号，在MySQL Workbench或命令行客户端中执行：

```sql
CREATE DATABASE IF NOT EXISTS employee_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'employee_app'@'localhost'
    IDENTIFIED BY 'replace_with_local_password';

ALTER USER 'employee_app'@'localhost'
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

把 `replace_with_local_password` 换成本机练习账号的密码，不要把真实密码写入项目文件或提交记录。应用账号只获得当前数据库的查询和增删改权限，不使用MySQL管理员账号运行应用。

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
```

在将要启动应用的PowerShell窗口中设置本地环境变量：

```powershell
$env:DB_USERNAME = "employee_app"
$env:DB_PASSWORD = "填写本机练习账号的密码"
```

只有数据库不在本机默认地址时才需要设置 `DB_URL`：

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo"
```

这些变量只对当前PowerShell及其启动的子进程有效。关闭窗口后不会永久保存。

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

接口上不再写 `@Service`，因为接口本身没有业务方法体。真正需要注册为Spring Bean的是下面的实现类。

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

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

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
        if ("Unknown".equals(department)) {
            return List.of();
        }

        return List.of(new EmployeeListItemResponse(
                1001L,
                "Tanaka",
                department));
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

`findList()` 和 `previewCreate()` 保留第8章的临时实现，确保既有列表、校验和异常练习仍然可以执行。只有 `findById()` 在本章改为读取数据库。

## 三、先读懂表定义和数据字典

关系数据库把同一种业务记录保存在表中。`employees` 是表，每一行表示一名员工，每一列表示一个固定含义的字段。数据库中的 `NULL` 表示没有值，不等于空字符串。

### 1. 本章用到的DDL、DML和查询

- `CREATE DATABASE`、`CREATE TABLE`、`CREATE USER`和 `GRANT` 用于定义数据库结构或权限，属于DDL或管理操作。
- `INSERT` 用于写入记录，属于DML。
- `SELECT` 用于读取记录，本章应用只实现这一种SQL调用。

`IF NOT EXISTS` 只用于数据库和账号；表故意不使用它，因为表已存在但结构错误时，安静跳过会让问题更难发现。

### 2. 字段数据字典

| 数据库列 | MySQL类型与约束 | Java属性 | 生成或填写者 | 业务含义 | API范围 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT`、主键、自增、非空 | `Long id` | MySQL | 员工唯一编号 | 路径参数、列表和详情响应 |
| `name` | `VARCHAR(50)`、非空 | `String name` | 客户端输入 | 员工姓名 | 请求、列表和详情 |
| `department` | `VARCHAR(50)`、非空 | `String department` | 客户端输入 | 部门名称 | 请求、列表和详情 |
| `email` | `VARCHAR(100)`、非空、唯一 | `String email` | 客户端输入 | 联系邮箱，不允许重复 | 请求和详情；列表不公开 |
| `status` | `VARCHAR(20)`、非空、默认 `ACTIVE`、值域检查 | `String status` | MySQL默认值或系统 | 当前状态 | 当前接口不公开 |
| `created_at` | `DATETIME`、非空、默认当前时间 | `LocalDateTime createdAt` | MySQL | 创建时间 | 当前接口不公开 |
| `updated_at` | `DATETIME`、非空、默认当前时间、更新时刷新 | `LocalDateTime updatedAt` | MySQL | 最后更新时间 | 当前接口不公开 |

接口校验和数据库约束保护的入口不同。第8章的 `@Size(max = 100)` 能在HTTP入口尽早拒绝过长邮箱；数据库 `VARCHAR(100)` 保护所有写入来源；唯一约束还能防止并发请求同时通过“是否重复”的事前查询。下一章处理写入时会把数据库约束失败转换为接口规定的响应。

### 3. 主键、默认值和约束

- `PRIMARY KEY` 保证每一行有唯一身份；`AUTO_INCREMENT` 让MySQL在插入时生成编号。
- `NOT NULL` 禁止SQL `NULL`，但不会自动禁止空字符串。
- `DEFAULT` 只在INSERT省略该列时使用；主动传入 `NULL` 仍会违反非空约束。
- `UNIQUE (email)` 保证表内邮箱不重复。
- `CHECK` 限制状态只能是 `ACTIVE` 或 `INACTIVE`。本章以MySQL 8.0为基线，不把这段DDL复制到不支持相同行为的旧数据库。

## 四、JDBC、DataSource、连接池和MyBatis分别做什么

数据库依赖加入后，应用中同时出现几组名称。它们不是同一个东西：

```text
EmployeeMapper方法
  → MyBatis生成并执行SQL调用
  → JDBC标准接口
  → MySQL Connector/J驱动
  → 数据库连接
  → MySQL 8.0
```

| 名称 | 类型或来源 | 本章职责 |
| --- | --- | --- |
| JDBC | Java访问关系数据库的标准API | 规定连接、预编译语句和结果读取等接口 |
| MySQL Connector/J | MySQL提供的JDBC驱动 | 把JDBC调用转换为MySQL通信 |
| `DataSource` | `javax.sql.DataSource` 接口 | 提供数据库连接；Spring Boot根据配置创建实现对象 |
| HikariCP | 默认连接池实现 | 保存并复用一定数量的数据库连接，避免每次请求都重新建立物理连接 |
| MyBatis | 数据访问框架 | 把Mapper方法、SQL参数、XML语句和Java结果对象连接起来 |
| MyBatis Starter | Spring Boot集成依赖 | 自动准备MyBatis与Spring协作所需的基础对象和Mapper代理 |

`mybatis-spring-boot-starter` 3.0系列支持Spring Boot 3.2～3.5和Java 17以上，本项目固定使用3.0.5。`mysql-connector-j` 的具体兼容版本由Spring Boot父项目管理，因此依赖中不单独填写版本。兼容范围可在[MyBatis Spring Boot Starter官方要求](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)中核对。

连接池复用连接，不等于所有请求共用一个正在执行SQL的连接。借出、归还和事务绑定由框架管理，业务代码不手动关闭连接池中的物理连接。

## 五、数据库连接配置怎样生效

`spring.datasource` 是Spring Boot配置前缀，不是Java包。启动时，Spring Boot读取URL、账号、密码和驱动，创建 `DataSource`；MyBatis再通过它取得连接。Spring Boot的数据源属性和连接池选择可参考[Spring Boot 3.5 SQL数据库说明](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html)。

| 配置项 | 可接受的值 | 默认值或必填性 | 当前作用 |
| --- | --- | --- | --- |
| `spring.datasource.url` | 有效JDBC URL | 默认连接本机3306端口的 `employee_db` | 指定数据库地址和连接参数 |
| `username` | 有权限的MySQL账号 | 默认 `employee_app` | 指定应用身份 |
| `password` | 账号密码 | 必填，无默认值 | 从环境变量读取凭据 |
| `driver-class-name` | 可加载的JDBC驱动类 | 本章固定MySQL驱动 | 明确使用Connector/J |
| `mapper-locations` | 一个或多个classpath资源模式 | 本章固定 `classpath:mapper/*.xml` | 查找Mapper XML |
| `type-aliases-package` | Java包名 | 本章固定Entity包 | 允许XML用 `Employee` 代替完整类名 |

`${DB_URL:默认值}` 表示优先读取环境变量，变量不存在时使用冒号后的默认值；`${DB_PASSWORD}` 没有默认值，缺失时应用应启动失败。生产环境应使用部署平台的密钥管理或受控环境变量，并为不同环境使用不同账号。

JDBC URL中的 `connectionTimeZone=Asia/Tokyo` 设置连接解释时间值时使用的时区。数据库列使用 `DATETIME`，Java使用不携带时区的 `LocalDateTime`；这表示业务上的本地日期时间，不代表UTC瞬间。具体转换边界可参考[MySQL Connector/J日期时间说明](https://dev.mysql.com/doc/connector-j/en/connector-j-time-instants.html)。

## 六、Employee为什么不是请求或响应对象

`Employee` 是项目创建的持久化对象，用于承接一行 `employees` 查询结果。它包含表查询需要的状态和时间字段；请求DTO表示客户端允许提交的字段，响应对象表示接口允许公开的字段。

```text
EmployeeCreateRequest：客户端可以提交什么
Employee：数据库一行包含什么
EmployeeResponse：详情接口允许返回什么
```

因此不能为了少写一个类就让Controller直接返回Entity。否则以后给表增加内部备注、删除标志或审计字段时，接口可能意外公开这些字段。

`LocalDateTime` 来自JDK的 `java.time` 包。MySQL `DATETIME` 与它都不携带时区。MyBatis读取结果后调用setter写入属性，所以Entity保留无参数构造能力和各字段setter。

## 七、Mapper接口和XML怎样对应

### 1. Java接口

`@Mapper` 和 `@Param` 都来自 `org.apache.ibatis.annotations`：

- `@Mapper` 写在接口上，无参数。启动时MyBatis发现该接口并创建Mapper代理，再把代理注册为Spring Bean。
- `@Param("id")` 写在方法参数上，字符串不能为空。本例让XML中的 `#{id}` 稳定对应Java参数，不依赖编译器是否保留参数名。
- `findById()` 接收 `Long` 编号，查询到一行时返回 `Employee`，没有记录时返回 `null`。

### 2. XML对应关系

| XML位置 | 必须对应什么 | 本章值 |
| --- | --- | --- |
| `namespace` | Mapper接口完整类名 | `com.example.employee.mapper.EmployeeMapper` |
| `<select id>` | 接口方法名 | `findById` |
| `parameterType` | 方法输入类型 | `long`类型别名 |
| `resultMap` | 当前XML中的结果映射id | `employeeResultMap` |
| `#{id}` | `@Param`指定的参数名 | `id` |

DOCTYPE告诉编辑器和解析器该XML遵循MyBatis Mapper 3格式。`resultMap` 明确写出“数据库列→Java属性”的关系；其中 `<id>` 用于主键映射，`<result>` 用于普通列映射。

简单查询也可以使用列别名和 `resultType`：

```xml
<select id="findById" resultType="Employee">
    SELECT created_at AS createdAt
    FROM employees
    WHERE id = #{id}
</select>
```

列别名适合简单且字段较少的结果；可复用的完整记录映射使用 `resultMap` 更容易集中核对。本章主线只采用一个明确的 `resultMap`，不同时维护两套正式映射。

### 3. `#{}`和`${}`不是两种随意替换的写法

`#{id}` 会生成类似 `WHERE id = ?` 的预编译SQL，再通过JDBC单独绑定 `Long` 值。它能处理类型并防止参数内容改变SQL结构。

`${id}` 是把文本直接插入SQL。用户输入如果包含SQL片段，可能改变语句结构并造成SQL注入。本课程的请求参数一律不使用 `${}`。只有列名、排序方向等无法作为预编译值绑定的受控标识符场景才可能使用它，而且必须从服务器固定白名单选择，不能直接接收用户文本。

## 八、为什么Mapper接口没有Impl类

调用 `employeeMapper.findById(id)` 时，实际对象不是开发者编写的 `EmployeeMapperImpl`，而是MyBatis运行时生成的代理对象：

```text
接口完整名 + 方法名
  → 找到XML的namespace + id
  → 取得SQL和resultMap
  → 绑定参数并执行
  → 把结果映射为Employee
```

Starter还会自动配置 `SqlSessionFactory` 和 `SqlSessionTemplate`。前者保存解析后的MyBatis配置并创建会话，后者是MyBatis-Spring提供的线程安全调用入口，负责让Mapper调用参与Spring管理的会话和事务。业务代码不手动调用 `openSession()`、`commit()`或 `close()`。

Mapper代理只负责把接口调用连接到数据访问过程，不包含“员工不存在应返回404”这种接口业务判断。

## 九、Service接口与实现类是什么关系

本章的Service有两份Java类型：

```text
EmployeeService       接口：声明员工业务可以做什么
EmployeeServiceImpl   实现类：写出当前业务怎样完成
```

`implements EmployeeService` 表示实现类承诺实现接口中的全部方法；`@Override` 让编译器检查方法签名是否真的对应接口。`@Service` 写在实现类上，Spring创建的是实现类对象。Controller构造器虽然要求 `EmployeeService` 接口，容器仍能找到唯一的实现对象并注入。

Service并非任何规模的项目都必须创建“接口＋Impl”。本课程从数据库接入阶段采用这种组织，是为了让学员能够阅读企业项目中常见的接口边界，并在后续测试或替换实现时有清楚的依赖方向。如果一个项目明确只使用具体Service类，也不代表违反Spring规则。

Service接口与Mapper接口的关键区别是：

| 对比 | Service接口 | Mapper接口 |
| --- | --- | --- |
| 实现来源 | 项目编写 `EmployeeServiceImpl` | MyBatis运行时生成代理 |
| 主要职责 | 组织业务判断、转换和数据访问调用 | 把Java方法连接到SQL执行 |
| Spring Bean来源 | 实现类上的 `@Service` | MyBatis扫描 `@Mapper`后注册代理 |
| 是否应写业务规则 | 应写与用例有关的规则 | 不应承担HTTP或业务流程判断 |

## 十、Entity怎样转换成响应对象

`findById()` 按下面的顺序执行：

```text
1. Service调用employeeMapper.findById(id)
2. Mapper返回Employee或null
3. null时抛出EmployeeNotFoundException
4. 有记录时调用toResponse(employee)
5. 只选择id、name、department、email创建EmployeeResponse
6. 第7章的Controller和ApiResponse返回HTTP 200
```

转换代码逐字段选择接口允许公开的数据。`status`、`createdAt`和 `updatedAt` 留在Entity内，不进入当前详情响应。这种显式转换虽然多几行代码，但Review时可以直接核对数据库字段和接口字段的边界。

第8章的DTO校验仍然保护新增预览入口，但按编号查询没有请求DTO；路径文本转换为 `Long` 的400处理仍由Spring MVC负责。查询不到记录不是格式错误，Service继续抛出 `EmployeeNotFoundException`，全局异常处理器把它变成404。

## 十一、DAO、Repository和Mapper是不是三个层

它们通常都是“数据访问代码”的命名，不表示必须再创建三个依次调用的架构层：

- `DAO` 是Data Access Object的通用叫法，强调封装数据访问。
- `Repository` 常见于领域设计或Spring Data项目，语义偏向对象集合；不同框架对它的实现方式不同。
- `Mapper` 是MyBatis项目的常见名称，强调方法、SQL参数和结果之间的映射。

本项目使用MyBatis，因此统一采用 `mapper` 包和 `EmployeeMapper`。不要再创建内容完全相同的 `EmployeeDao`和 `EmployeeRepository`，否则只会增加转发代码。阅读既有日本项目时，应先看接口、SQL和调用关系，再判断名称实际承担什么职责。

## 十二、Java Web项目中的日期与时间

Employee表的 `created_at`、`updated_at` 使用MySQL `DATETIME`，Java对象使用 `LocalDateTime`。这种组合能保存“2026-09-16 09:30:00”这样的本地日期时间，但字符串看起来完整，不代表它已经说明时区和时间点。

### 1. 五种常见Java时间类型

| 类型 | 示例 | 包含的信息 | 常见用途 |
| --- | --- | --- | --- |
| `LocalDate` | `2026-09-16` | 日期，无时间、无时区 | 生日、营业日、开始日期 |
| `LocalTime` | `09:30:00` | 一天中的时间，无日期、无时区 | 每日营业时间 |
| `LocalDateTime` | `2026-09-16T09:30:00` | 日期和时间，无时区 | 已明确业务地区的本地业务时间 |
| `OffsetDateTime` | `2026-09-16T09:30:00+09:00` | 日期、时间和UTC偏移 | API中携带偏移的时间 |
| `Instant` | `2026-09-16T00:30:00Z` | 时间轴上的唯一瞬间 | 事件发生时刻、跨时区系统交换 |

这些类型都来自Java 17的 `java.time` 包。Oracle文档明确说明 `LocalDateTime` 不保存时区，不能在没有额外offset或zone信息时确定时间轴上的唯一瞬间，参见[LocalDateTime API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDateTime.html)。

```text
2026-09-16T09:30:00+09:00  日本偏移表示
2026-09-16T00:30:00Z       UTC表示，Z表示+00:00
2026-09-16T09:30:00        只有本地日期时间，单独看无法证明是哪一瞬间
```

### 2. Asia/Tokyo、UTC和+09:00不是同一种概念

- `UTC` 是协调世界时基准；Java中常用 `ZoneOffset.UTC`。
- `+09:00` 是固定offset，表示比UTC快9小时，本身不包含地区规则。
- `Asia/Tokyo` 是区域时区ID，由 `ZoneId` 表示，可通过时区规则为某个日期时间决定offset。

当前日本标准时间通常是+09:00，但区域时区和固定offset在概念上仍不同。其他区域可能随日期使用夏令时，同一 `ZoneId` 在不同日期对应的offset可能变化。因此跨区域系统应保存明确的时间语义，而不是看到 `09:30:00` 就默认是日本时间。

### 3. 常见转换必须提供缺少的信息

```java
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

ZoneId tokyo = ZoneId.of("Asia/Tokyo");
LocalDateTime local = LocalDateTime.of(2026, 9, 16, 9, 30);

Instant instant = local.atZone(tokyo).toInstant();
OffsetDateTime offsetDateTime = instant.atZone(tokyo).toOffsetDateTime();
LocalDateTime restoredLocal = LocalDateTime.ofInstant(instant, tokyo);
```

`ZoneId.of(...)` 根据区域名称取得时区规则；`atZone(tokyo)` 给没有时区的本地时间补充“按东京规则解释”的前提；`toInstant()` 得到唯一瞬间。反向转换时 `ofInstant(instant, tokyo)` 必须再次指定希望看到哪个地区的本地时间。

不能把任意 `LocalDateTime` 直接当UTC或东京时间。转换前必须从接口规格、数据库定义或业务规则确认它原本代表什么。

### 4. Browser到数据库的完整路径

```text
Browser中的日期时间
  → HTTP JSON字符串及offset约定
  → Jackson解析为Java时间类型
  → Service按业务时区转换或校验
  → MyBatis/JDBC绑定参数
  → MySQL DATE、DATETIME或TIMESTAMP
```

| 阶段 | 常见问题 | 调查证据 |
| --- | --- | --- |
| Browser→JSON | 浏览器本地时区、格式或offset丢失 | Network中的原始请求 |
| JSON→Java | 类型与文本格式不匹配 | 400响应、Jackson异常、DTO类型 |
| Service | 把无时区值错误解释为UTC | 业务规格、转换代码、测试时区 |
| JDBC→DB | Java类型与列类型不一致 | Mapper参数、JDBC URL、表定义 |
| DB→查询结果 | 连接时区或DATETIME/TIMESTAMP语义不同 | session time_zone、原始列值 |

第5章出现的 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 只规定JSON文本格式，不会为 `LocalDateTime` 增加时区。需要表达offset时，应优先在接口规格中采用可携带offset的ISO-8601形式并使用 `OffsetDateTime`，而不是只改变显示样式。

### 5. MySQL DATETIME与TIMESTAMP要按规格选择

MySQL `DATETIME` 保存日期和时间字段，不像 `TIMESTAMP` 那样按照连接时区与UTC进行存取转换。MySQL官方对两者的区别见[DATE、DATETIME和TIMESTAMP类型](https://dev.mysql.com/doc/refman/8.0/en/datetime.html)。

当前Employee项目把 `created_at`、`updated_at` 作为日本业务环境中的数据库本地时间，并映射为 `LocalDateTime`。这是一项项目规格，不是所有系统都必须采用的通用答案。跨国家事件、审计时间或多地区API更适合先确定统一的UTC/Instant策略，再明确显示时区。

### 6. 时间问题的验证任务

1. 写出 `2026-09-16T09:30:00+09:00` 对应的UTC表示。
2. 说明为什么 `2026-09-16T09:30:00` 不能独立证明一个瞬间。
3. 沿Browser→JSON→Java→JDBC→MySQL列出Employee创建时间的类型和格式。
4. 把测试进程时区临时设为UTC，检查依赖系统默认时区的断言是否失败；恢复原设置后重新执行全部测试。
5. Review一个只用字符串拼接“+09:00”的转换方案，指出应由 `ZoneId`、`OffsetDateTime` 或 `Instant` 明确处理的部分。

本节不要求修改Employee表。任何列类型或时间语义变更都必须先调查既有数据、MyBatis映射、JSON规格、服务器/JVM/数据库时区和回归测试。

## 十三、启动并验证完整调用链

### 1. 先验证数据库

使用应用账号连接后执行：

```sql
SELECT id, name, department, email
FROM employees
WHERE id = 1001;
```

确认能查到目标记录。如果实际编号不是1001，记下真实编号。

### 2. 构建和启动

在已经设置 `DB_USERNAME` 和 `DB_PASSWORD` 的同一个PowerShell中，从含有 `pom.xml` 的项目根目录执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

若生成工程没有Wrapper，可使用本机Maven执行等价的 `mvn.cmd clean test` 和 `mvn.cmd spring-boot:run`。

### 3. 请求详情

另开PowerShell，把编号替换为数据库中实际存在的值：

```powershell
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/1001" `
    -Method Get

$response.StatusCode
$response.Content
```

预期返回200，`data`中的姓名、部门和邮箱与数据库记录一致。随后临时修改这行员工的姓名：

```sql
UPDATE employees
SET name = 'Tanaka Taro'
WHERE id = 1001;
```

再次请求接口，响应姓名也应改变。这证明详情不再来自Service中的固定文本。完成验证后恢复样例值：

```sql
UPDATE employees
SET name = 'Tanaka'
WHERE id = 1001;
```

### 4. 验证不存在记录和既有接口

请求一个数据库中不存在的编号：

```powershell
try {
    Invoke-WebRequest `
        -Uri "http://localhost:8080/employees/999999" `
        -Method Get
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

预期返回404。还应回归验证：

- `GET /employees?department=Sales` 仍返回200；
- 合法的 `POST /employees/preview` 仍返回200；
- 非法部门仍返回400；
- 重复测试邮箱仍返回409；
- `GET /health` 仍返回200和 `OK`。

## 十四、按阶段排查数据库问题

| 现象 | 所在阶段 | 常见原因 | 检查和修正 |
| --- | --- | --- | --- |
| 提示无法解析 `DB_PASSWORD` | 配置读取 | 环境变量未设置 | 在启动应用的同一PowerShell设置变量 |
| `Access denied for user` | 数据库认证 | 账号、密码、主机范围或权限错误 | 用应用账号单独登录并检查授权，不改用root绕过 |
| `Unknown database` | 建立连接 | 数据库名错误或建库脚本未执行 | 在MySQL中检查 `employee_db` |
| 连接被拒绝或超时 | 网络与MySQL服务 | 服务未启动、端口或地址错误 | 先用数据库客户端连接同一地址 |
| `Invalid bound statement` | Mapper语句定位 | XML未扫描、namespace或id不一致 | 逐项核对资源路径、接口完整名和方法名 |
| `Table ... doesn't exist` | SQL执行 | 当前数据库错误或表未创建 | 查询当前数据库并检查表名 |
| `Unknown column` | SQL执行 | SQL列名与DDL不一致 | 对照数据字典和实际表结构 |
| 属性为 `null`但列有值 | 结果映射 | `resultMap`的column或property写错 | 对照列名、Java属性和setter |
| 应返回一条却查询到多条 | SQL与约束 | 查询条件不唯一 | 使用主键或唯一约束，检查数据 |

排错顺序应是“配置读取→建立连接→找到Mapper语句→执行SQL→结果映射→业务转换”。异常堆栈通常从外层框架异常开始，真正原因常在最后一个 `Caused by` 附近。

## 十五、规格理解、影响调查与操作练习

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

## 十六、本章稳定状态

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
9. 区分 `LocalDate`、`LocalTime`、`LocalDateTime`、`OffsetDateTime` 和 `Instant`；
10. 说明UTC、固定offset和区域时区的区别，并沿Browser到MySQL调查时间问题。

下一章会在这条稳定数据访问链上实现真实新增、修改和删除，并用数据库状态验证每次写操作。
