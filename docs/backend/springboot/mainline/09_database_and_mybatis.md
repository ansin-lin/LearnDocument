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

第8章的固定样例只能演示流程：查询编号1002不应由代码凭空编造结果。接入真实数据时，运行方向是 `HTTP → Controller → EmployeeService接口 → EmployeeServiceImpl → EmployeeMapper代理 → MyBatis → DataSource → MySQL`，查询结果再经Entity、响应DTO返回。Spring Boot依据 `spring.datasource` 配置准备 `DataSource`；MyBatis Starter利用它建立SQL会话并注册Mapper代理。Starter负责Spring与MyBatis的整合，MySQL JDBC Driver负责与MySQL通信，两种依赖不能互相替代。

下面先准备数据库，再配置依赖与连接，接着把Entity、Mapper接口和同名XML放在一起核对，最后将Service改为接口与实现类。`@Mapper`使MyBatis把接口注册为可注入的代理对象，无需手写Mapper实现；`@Service`让Spring管理实现类。Controller构造方法仍要求 `EmployeeService`，Spring按接口类型找到 `EmployeeServiceImpl`，所以Controller不必随实现方式改变。下文完整代码是本章最终状态；学习时按这条链逐段检查，不把MyBatis XML基础语法重复当作新知识。

### 1. 建立数据库、账号、表和样例数据

以下脚本只适用于MySQL 8.0.16及以上版本的本地练习环境。先使用拥有建库和创建用户权限的管理账号，在MySQL Workbench或命令行客户端中执行：

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
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 10
```

`mybatis` 下的配置分成两类：

- `mapper-locations`、`type-aliases-package` 是MyBatis Spring Boot Starter提供的集成配置，负责告诉Starter去哪里找Mapper XML和类型别名；
- `configuration` 下面是MyBatis核心运行设置，Starter会把这些值写入MyBatis的 `Configuration` 对象。

`map-underscore-to-camel-case: true` 允许自动映射时把 `created_at` 对应到 `createdAt`。当前 `EmployeeMapper.xml` 使用了明确的 `resultMap`，所以即使开启该设置，仍以 `resultMap` 中写出的 `column` 和 `property` 为准。`default-statement-timeout: 10` 把未单独指定超时的SQL默认等待时间设为10秒；它用于限制等待时间，不保证SQL一定在10秒内完成，具体终止行为还受JDBC驱动和数据库影响。

在Eclipse中打开 **Run → Run Configurations...**，选择当前启动类对应的Java Application或Spring Boot App运行配置，在 **Environment** 页添加：

| Name | Value | 是否必需 |
| --- | --- | --- |
| `DB_USERNAME` | `employee_app` | 是 |
| `DB_PASSWORD` | 本机练习账号密码 | 是，不写入文档或Git |
| `DB_URL` | `jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo` | 数据库不在默认地址时设置 |

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

import java.util.ArrayList;
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
- `CHECK` 限制状态只能是 `ACTIVE` 或 `INACTIVE`。本章以MySQL 8.0.16及以上版本为基线；更早版本可能接受语法却不实际执行约束，不能照搬本章结论。

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

## 五、Spring Boot怎样读取数据库和MyBatis配置

### 1. 两组配置分别由谁处理

`spring.datasource` 是Spring Boot配置前缀，不是Java包。启动时，Spring Boot读取URL、账号、密码和驱动，创建 `DataSource`；MyBatis再通过它取得连接。Spring Boot的数据源属性和连接池选择可参考[Spring Boot 3.5 SQL数据库说明](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html)。

`mybatis` 是MyBatis Spring Boot Starter使用的配置前缀。应用启动时，两组配置按照下面的方向生效：

```text
application.yml
  ├─ spring.datasource.*
  │    → Spring Boot创建DataSource
  └─ mybatis.*
       → Starter读取并绑定MyBatis配置
       → 创建SqlSessionFactory
       → 创建SqlSessionTemplate
       → 加载Mapper XML并注册@Mapper代理对象
```

业务代码不需要自己读取YAML，也不需要手动使用 `SqlSessionFactoryBuilder`。Starter检测到 `DataSource` 后，把它交给自动创建的 `SqlSessionFactory`，再让Mapper代理通过Spring管理的 `SqlSessionTemplate` 执行SQL。这个流程与普通Java项目手动读取 `mybatis-config.xml`、手动构建工厂的方式不同。

### 2. 本章application.yml中的主要配置

| 配置项 | 可接受的值 | 默认值或必填性 | 当前作用 |
| --- | --- | --- | --- |
| `spring.datasource.url` | 有效JDBC URL | 默认连接本机3306端口的 `employee_db` | 指定数据库地址和连接参数 |
| `username` | 有权限的MySQL账号 | 默认 `employee_app` | 指定应用身份 |
| `password` | 账号密码 | 必填，无默认值 | 从环境变量读取凭据 |
| `driver-class-name` | 可加载的JDBC驱动类 | 本章固定MySQL驱动 | 明确使用Connector/J |
| `mybatis.mapper-locations` | 一个或多个Spring资源路径或通配模式 | Starter没有自动填写本项目路径，本章明确设置 | 查找并加载Mapper XML |
| `mybatis.type-aliases-package` | 一个或多个Java包名 | 默认不扫描指定别名包，本章固定Entity包 | 允许XML用 `Employee` 代替完整类名 |
| `mybatis.configuration.map-underscore-to-camel-case` | `true`、`false` | MyBatis默认 `false`，本章设置 `true` | 自动映射时把下划线列名转换为驼峰属性名 |
| `mybatis.configuration.default-statement-timeout` | 正整数秒数 | MyBatis默认不设置统一值，本章设置10秒 | 为没有单独配置超时的SQL提供默认等待上限 |

`${DB_URL:默认值}` 表示优先读取环境变量，变量不存在时使用冒号后的默认值；`${DB_PASSWORD}` 没有默认值，缺失时应用应启动失败。生产环境应使用部署平台的密钥管理或受控环境变量，并为不同环境使用不同账号。

JDBC URL中的 `connectionTimeZone=Asia/Tokyo` 设置连接解释时间值时使用的时区。数据库列使用 `DATETIME`，Java使用不携带时区的 `LocalDateTime`；这表示业务上的本地日期时间，不代表UTC瞬间。具体转换边界可参考[MySQL Connector/J日期时间说明](https://dev.mysql.com/doc/connector-j/en/connector-j-time-instants.html)。

YAML使用短横线命名，例如 `map-underscore-to-camel-case`；对应的MyBatis核心设置名称是 `mapUnderscoreToCamelCase`。Spring Boot的配置绑定会完成这种命名转换。

本章没有设置下面这些选项：

- 不设置 `log-impl: STDOUT_LOGGING`，因为项目使用Spring Boot的SLF4J日志体系，第11章统一配置SQL日志；
- 不为了展示而启用懒加载或二级缓存，这些行为会改变对象加载和数据一致性判断；
- 不把 `default-fetch-size` 当成查询行数限制，它只是给JDBC驱动的抓取提示，不能替代SQL中的分页或 `LIMIT`。

MyBatis全部核心设置及默认值应以[MyBatis官方Configuration说明](https://mybatis.org/mybatis-3/configuration)为准，不应把所有设置复制进项目。

### 3. 项目使用独立mybatis-config.xml时怎样读取

有些既存项目把MyBatis核心设置放在 `mybatis-config.xml` 中。Spring Boot不会仅凭文件名自动读取它，必须通过 `mybatis.config-location` 指定资源位置。`mybatis.configuration.*` 和 `mybatis.config-location` 是两种核心设置来源，不能同时使用；`mapper-locations` 和 `type-aliases-package` 仍可保留在 `application.yml` 中。

当前主线继续使用结构更直观的 `mybatis.configuration.*`。独立XML的完整文件、配置切换、`<environments>`差异、故障观察和恢复步骤见附录[Spring Boot怎样读取独立MyBatis配置](../appendix/A12_springboot_mybatis_config.md)。

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

这是对比片段，表示用它**替换**当前 `findById` 的 `<select>`；不能直接追加到同一个 `<mapper>`，否则相同的 `namespace + id` 会重复。列别名适合简单且字段较少的结果；可复用的完整记录映射使用 `resultMap` 更容易集中核对。本章主线只采用一个明确的 `resultMap`，不同时维护两套正式映射。

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

Starter还会自动配置 `SqlSessionFactory` 和 `SqlSessionTemplate`。前者保存解析后的MyBatis配置并创建会话，后者是MyBatis-Spring提供的线程安全调用入口，负责让Mapper调用在已有Spring事务中共用会话。业务代码不手动调用 `openSession()`、`commit()`或 `close()`。

这里的“支持Spring事务”不等于所有Service方法已经自动具有事务边界。当前还没有使用 `@Transactional`；发生在Spring事务之外的每次Mapper调用会各自提交。第10章先完成CRUD功能闭环，第13章再为多步写入建立共同提交或回滚的事务边界。

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

## 十一、启动并验证完整调用链

### 1. 先验证数据库

使用应用账号连接后执行：

```sql
SELECT id, name, department, email
FROM employees
WHERE id = 1001;
```

确认能查到目标记录。如果实际编号不是1001，记下真实编号。

### 2. 在Eclipse中启动

确认Eclipse运行配置已经包含 `DB_USERNAME` 和 `DB_PASSWORD`，保存代码后从该运行配置启动应用。Console中应先出现数据库连接池和Spring Boot启动成功信息；如果密码、URL或驱动错误，应先修复启动问题，不继续发送接口请求。

### 3. 请求详情

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
| GET | `{{baseUrl}}/employees` | `department=Sales` | 无 | 200，列表数据来自数据库 |
| POST | `{{baseUrl}}/employees/preview` | 无 | `{"name":"Sato","department":"Development","email":"sato@example.com"}` | 200，预览成功 |
| POST | `{{baseUrl}}/employees/preview` | 无 | `{"name":"Sato","department":"Other","email":"sato@example.com"}` | 400，非法部门消息 |
| POST | `{{baseUrl}}/employees/preview` | 无 | `{"name":"Sato","department":"Development","email":"used@example.com"}` | 409，邮箱冲突消息 |
| GET | `{{baseUrl}}/health` | 无 | 无 | 200，正文 `OK` |

三个POST请求的Body均选择 **raw → JSON**，并确认 `Content-Type: application/json`。

完成这一步后，数据库记录到HTTP响应的主线已经实际运行。下面两节补充项目命名和当前时间字段边界，不再阻塞本章核心验证。

## 十二、DAO、Repository和Mapper是不是三个层

它们通常都是“数据访问代码”的命名，不表示必须再创建三个依次调用的架构层：

- `DAO` 是Data Access Object的通用叫法，强调封装数据访问。
- `Repository` 常见于领域设计或Spring Data项目，语义偏向对象集合；不同框架对它的实现方式不同。
- `Mapper` 是MyBatis项目的常见名称，强调方法、SQL参数和结果之间的映射。

本项目使用MyBatis，因此统一采用 `mapper` 包和 `EmployeeMapper`。不要再创建内容完全相同的 `EmployeeDao`和 `EmployeeRepository`，否则只会增加转发代码。阅读既有日本项目时，应先看接口、SQL和调用关系，再判断名称实际承担什么职责。

## 十三、当前项目的日期时间映射

Employee表的 `created_at`、`updated_at` 使用MySQL `DATETIME`，Entity使用Java 17的 `LocalDateTime`。两者都表示日期和时间，但都不携带时区或UTC偏移；当前项目把它们定义为日本业务环境中的数据库本地时间。

这只是当前项目规格，不是所有系统的通用答案。跨国家事件、审计时间或多地区API需要另外确定UTC、区域时区和接口格式，不能看到 `LocalDateTime` 就默认它代表东京时间。`DATETIME`、`TIMESTAMP`、`LocalDateTime`、`OffsetDateTime`、`Instant` 及Browser到数据库的转换与验证，统一放在附录[Java Web项目中的日期时间与时区](../appendix/A11_java_web_datetime.md)。

本章只需确认MyBatis能够把两列写入Entity对应属性；它们不会进入当前详情响应。任何时间列类型或语义变更，都必须同时调查表定义、既有数据、Mapper映射、JDBC配置和接口规格。

## 十四、按阶段排查数据库问题

| 现象 | 所在阶段 | 常见原因 | 检查和修正 |
| --- | --- | --- | --- |
| 提示无法解析 `DB_PASSWORD` | 配置读取 | Eclipse运行配置中未设置环境变量 | 在当前启动类的Run Configuration中添加变量并重启 |
| 提示找不到MyBatis核心配置 | 配置读取 | `config-location` 路径错误，且启用了存在检查 | 核对 `src/main/resources` 下的位置和 `classpath:` 路径 |
| 创建 `SqlSessionFactory` 时提示两种配置并存 | MyBatis自动配置 | 同时写了 `config-location` 和 `configuration` | 二选一，删除另一套核心设置来源 |
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
9. 说明当前 `DATETIME` 与 `LocalDateTime` 都不携带时区，并知道何时进入日期时间附录继续学习。

下一章会在这条稳定数据访问链上实现真实新增、修改和删除，并用数据库状态验证每次写操作。
