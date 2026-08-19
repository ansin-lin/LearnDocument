# 第7章 MyBatis连接MySQL并完成详情查询

> 本章目标：在第6章工程中接入MySQL和MyBatis，让现有 `GET /employees/{id}` 从数据库读取员工，并能解释DataSource、Mapper代理、XML SQL和结果映射的执行关系。

## 一、开始状态

第6章已经保留以下文件：

```text
common/ApiResponse.java
controller/EmployeeController.java
dto/EmployeeCreateRequest.java
exception/ValidationExceptionHandler.java
service/EmployeeService.java
vo/EmployeeResponse.java
```

接口已有校验和统一响应，但Service仍返回固定数据。本章将新建或替换：

```text
pom.xml                                      # 增加两个依赖
src/main/resources/application.yml          # 增加数据源和MyBatis配置
src/main/java/com/example/employee/
├── entity/Employee.java                    # 新建
├── mapper/EmployeeMapper.java              # 新建
└── service/EmployeeService.java            # 完整替换
src/main/resources/mapper/
└── EmployeeMapper.xml                      # 新建
```

Controller、请求DTO和响应结构保持不变。数据库产品使用MySQL 8.0或兼容版本，Java继续使用17。

## 二、先建立数据库和测试数据

使用MySQL Workbench、命令行客户端或其他数据库工具，以有建库权限的本地账号执行下面的MySQL 8.0脚本：

```sql
CREATE DATABASE IF NOT EXISTS employee_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE employee_db;

CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO employees (name, department, email, status)
VALUES
    ('Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    ('Suzuki', 'Development', 'suzuki@example.com', 'ACTIVE');
```

应用不应使用管理员账号。仍在管理员连接中执行下面的本地开发账号设置，并把示例密码替换成仅用于本机的密码：

```sql
CREATE USER IF NOT EXISTS 'employee_app'@'localhost'
    IDENTIFIED BY 'replace_with_local_password';

ALTER USER 'employee_app'@'localhost'
    IDENTIFIED BY 'replace_with_local_password';

GRANT SELECT, INSERT, UPDATE, DELETE
ON employee_db.*
TO 'employee_app'@'localhost';
```

`CREATE USER` 创建只能从本机连接的应用账号；账号已经存在时，`ALTER USER` 把密码调整为本次指定值。`GRANT` 只授予应用CRUD需要的四种数据权限，不授予建库、改表或管理其他用户的权限。把相同的本地密码设置到后面的 `DB_PASSWORD` 环境变量，但不要把密码原文保存到项目文件。

这是会创建数据库、表并写入两行数据的DDL和DML脚本，只应在本地练习数据库执行。重复执行建表语句会因表已存在而失败；需要重做时应明确确认测试数据可以丢弃后，再自行删除测试表，不要对共享数据库执行清理。

### 第一次出现：表、列和约束

| SQL写法 | 当前值 | 作用与执行结果 |
| --- | --- | --- |
| `CREATE DATABASE IF NOT EXISTS` | 数据库名必须填写；本例为 `employee_db` | 数据库不存在时创建，已存在时不重复创建 |
| `CHARACTER SET utf8mb4` | MySQL字符集名称 | 支持完整Unicode字符 |
| `CREATE TABLE employees` | 表名必须填写 | 创建保存员工记录的表 |
| `BIGINT`、`VARCHAR(n)`、`DATETIME` | 分别接受整数、指定最大长度文本、日期时间 | 决定数据库列可保存的数据类型 |
| `PRIMARY KEY` | 写在主键列上 | 保证每行可由唯一编号识别 |
| `AUTO_INCREMENT` | 写在整数键上 | 插入时省略 `id`，由MySQL生成新编号 |
| `NOT NULL` | 写在不允许空值的列上 | 阻止数据库保存SQL `NULL` |
| `DEFAULT` | 后面必须给默认表达式 | INSERT省略该列时使用默认值 |
| `ON UPDATE CURRENT_TIMESTAMP` | MySQL写法 | 当前行被更新时自动刷新 `updated_at` |

`INSERT INTO ... VALUES ...` 按列名顺序写入数据。本例不写 `id`、创建时间和更新时间，它们由数据库生成。数据库字段长度与第6章DTO的50、50、100字符限制保持一致，但应用校验不能替代数据库约束。

执行后验证：

```sql
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
ORDER BY id;
```

预期至少得到：

| id | name | department | email | status |
| ---: | --- | --- | --- | --- |
| 1 | Tanaka | Sales | tanaka@example.com | ACTIVE |
| 2 | Suzuki | Development | suzuki@example.com | ACTIVE |

如果该数据库以前插入过数据，自动编号可能不是1和2。接口请求应使用查询结果中真实存在的编号。

## 三、添加MyBatis和MySQL驱动

打开根目录 `pom.xml`，在 `<dependencies>` 中加入：

```xml
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
```

`mybatis-spring-boot-starter` 是MyBatis官方的Spring Boot集成依赖。3.0.x用于Java 17和Spring Boot 3.2—3.5；示例使用3.0.5，以保持构建结果稳定。它会根据数据源自动配置 `SqlSessionFactory`、`SqlSessionTemplate`，扫描Mapper并把代理对象注册为Spring Bean。

`mysql-connector-j` 是MySQL官方JDBC驱动。JDBC是Java访问关系数据库的标准接口，驱动负责把JDBC调用转换为MySQL通信。`runtime` 表示运行时需要驱动，但业务源码通常不直接导入它的类；驱动版本由Spring Boot依赖管理统一选择。

保存后刷新Maven，并先验证依赖能够解析：

```powershell
.\mvnw.cmd test
```

## 四、配置数据源和MyBatis

打开：

```text
src/main/resources/application.yml
```

在已有配置中合并以下内容；如果文件已有 `spring:`，必须放到同一个 `spring:` 节点下，不能重复创建顶层键：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/employee_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo}
    username: ${DB_USERNAME:employee_app}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.employee.entity
```

### 第一次出现：DataSource配置对象

`spring.datasource` 不是Java包，而是Spring Boot配置前缀。启动时Spring Boot读取这些值，创建实现 `javax.sql.DataSource` 接口的数据源对象；MyBatis再从数据源取得数据库连接。

| 配置项 | 可接受的值、默认值与必填性 | 当前作用 |
| --- | --- | --- |
| `url` | 必须是有效JDBC URL；`${DB_URL:默认值}` 表示环境变量可覆盖，未设置时使用冒号后的本地地址 | 指定MySQL主机、端口、数据库和连接参数 |
| `username` | 必填；本例默认 `employee_app`，可由 `DB_USERNAME` 覆盖 | 指定数据库账号 |
| `password` | 必填；`${DB_PASSWORD}` 没有默认值 | 只从环境读取密码，避免把真实凭据提交到Git |
| `driver-class-name` | 必须是JDBC驱动类名；本例为MySQL Connector/J的 `com.mysql.cj.jdbc.Driver` | 指定连接MySQL使用的驱动；通常也可由URL推断，本例显式写出便于检查 |
| `mapper-locations` | 接受一个或多个资源路径；本例扫描 `src/main/resources/mapper` 下的XML | 告诉MyBatis到哪里寻找SQL映射文件 |
| `type-aliases-package` | 接受Java包名；本例无默认值，必须与Entity包一致 | 让XML可写 `Employee`，不必写完整类名 |

在启动应用的同一个PowerShell窗口设置本地环境变量：

```powershell
$env:DB_USERNAME = "employee_app"
$env:DB_PASSWORD = "仅填写本地开发账号的密码"
```

环境变量只对当前PowerShell进程及其启动的子进程有效，关闭窗口后不会继续保留。生产环境应使用部署平台的密钥管理能力，并使用最小权限账号；不要使用root账号运行应用，也不要把真实密码写入文档、源码或提交记录。

## 五、创建Employee实体对象

新建：

```text
src/main/java/com/example/employee/entity/Employee.java
```

完整代码：

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

### 第一次出现：Entity和LocalDateTime

`Employee` 是项目自己创建的Entity，用于承接一行 `employees` 查询结果。MyBatis通常先创建该对象，再通过setter写入每一列；它不是请求DTO，也不直接作为长期API响应返回。

`LocalDateTime` 来自JDK的 `java.time.LocalDateTime`，表示不附带时区的日期和时间。MySQL `DATETIME` 也不保存时区，所以两者可以直接映射；`serverTimezone=Asia/Tokyo` 是连接解释时间值时使用的时区设置，不会让 `LocalDateTime` 自身拥有时区。

数据库使用下划线列名 `created_at`，Java使用驼峰属性 `createdAt`。本章稍后在SELECT中使用 `AS createdAt` 明确建立对应关系。

## 六、创建Mapper接口

新建：

```text
src/main/java/com/example/employee/mapper/EmployeeMapper.java
```

完整代码：

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

### 第一次出现：@Mapper、@Param和Mapper代理对象

`@Mapper` 和 `@Param` 都来自 `org.apache.ibatis.annotations`，由MyBatis依赖提供。

| 代码 | 可接受的值、默认值与必填性 | 作用与返回值 |
| --- | --- | --- |
| `@Mapper` | 写在Mapper接口上，不需要参数 | 启动时让MyBatis发现接口，并创建实现它的代理对象作为Spring Bean |
| `@Param("id")` | 必须填写非空参数名；本例固定为 `id` | 让XML中的 `#{id}` 稳定对应Java参数，不依赖编译器是否保留参数名 |
| `findById(Long id)` | 调用时必须传员工编号；查询不到也属于正常查询结果 | 返回一个由查询行映射出的 `Employee`，无匹配行时返回 `null` |

`EmployeeMapper` 是接口，项目不创建 `EmployeeMapperImpl`。运行时注入的对象是MyBatis生成的Mapper代理：收到 `findById()` 调用后，它根据“接口完整名＋方法名”找到XML语句，通过 `SqlSessionTemplate` 执行SQL，再把结果转换为Employee。

## 七、创建Mapper XML

新建：

```text
src/main/resources/mapper/EmployeeMapper.xml
```

完整代码：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.mapper.EmployeeMapper">

    <select id="findById" parameterType="long" resultType="Employee">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM employees
        WHERE id = #{id}
    </select>

</mapper>
```

### 第一次出现：XML映射元素和参数绑定

DOCTYPE声明当前文件遵循MyBatis Mapper 3格式，IDE可据此检查XML结构。运行时真正建立对应关系的是下面这些值：

| XML写法 | 可接受的值、默认值与必填性 | 当前作用 |
| --- | --- | --- |
| `<mapper namespace="...">` | 必须填写唯一命名空间；本例必须等于Mapper接口完整类名 | 把整个XML连接到 `EmployeeMapper` |
| `<select id="findById">` | `id` 必须在当前namespace内唯一，并与接口方法名一致 | 定义该方法执行的SELECT |
| `parameterType="long"` | 可写Java完整类名或MyBatis类型别名，也可让MyBatis推断；本例显式声明长整数 | 描述传入参数类型 |
| `resultType="Employee"` | 必须是可映射类型；别名来自 `type-aliases-package` | 每一行创建一个Employee对象；单行方法最终返回一个对象或 `null` |
| `AS createdAt` | 别名必须与Java属性名一致 | 把 `created_at` 结果列映射到 `setCreatedAt()` |
| `#{id}` | 名称必须与 `@Param("id")` 一致 | 使用JDBC预编译参数传值，而不是把文本直接拼接进SQL |

`#{id}` 会形成类似 `WHERE id = ?` 的预编译SQL，再单独绑定数值，因此可以正确处理类型并防止这里的SQL注入。JDBC的 `PreparedStatement` 完整类名是 `java.sql.PreparedStatement`；MyBatis和驱动负责创建并执行该对象，业务代码不直接调用它。`${id}` 是直接文本替换，不能用于接收用户输入，CRUD接口不使用它。

## 八、让Service真正调用Mapper

用下面代码完整替换 `EmployeeService.java`：

```java
package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.vo.EmployeeResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class EmployeeService {

    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    public EmployeeResponse findById(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在: " + id);
        }
        return toResponse(employee);
    }

    public List<EmployeeResponse> findList(String department) {
        String selectedDepartment = normalizeDepartment(department);
        return List.of(new EmployeeResponse(
                1001L,
                "Tanaka",
                selectedDepartment.equals("ALL") ? "Sales" : selectedDepartment,
                "tanaka@example.com"));
    }

    public EmployeeResponse create(EmployeeCreateRequest request) {
        return new EmployeeResponse(
                1002L,
                request.getName(),
                normalizeDepartment(request.getDepartment()),
                normalizeEmail(request.getEmail()));
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getEmail());
    }

    private String normalizeDepartment(String department) {
        if (department == null || department.isBlank()) {
            return "ALL";
        }
        return department.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
```

这一替换只把详情查询接到数据库；列表和新增仍保留临时实现，待CRUD接口统一接入数据库。

构造器参数 `EmployeeMapper employeeMapper` 不是MyBatis接口本身“创建出的对象”，而是Spring从容器取得的Mapper代理。`employeeMapper.findById(id)` 调用代理并返回Entity；`toResponse(Employee employee)` 是项目自己的私有转换方法，接收Entity，调用getter读取四个公开字段，返回新建的 `EmployeeResponse`，避免Controller直接暴露状态和数据库时间列。

这里使用JDK的 `IllegalArgumentException` 表示找不到员工，访问不存在编号会成为500。正确的HTTP边界应使用项目业务异常并转换为404；当前验收只查询准备好的现有编号。

## 九、启动并观察完整执行链

确认MySQL正在运行，并在当前PowerShell设置好 `DB_USERNAME`、`DB_PASSWORD`，然后执行：

```powershell
.\mvnw.cmd spring-boot:run
```

另开PowerShell，请把URL中的1替换为前面SELECT得到的真实编号：

```powershell
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/employees/1" `
    -Method Get

$response.StatusCode
$response.Content
```

预期状态码为200，响应中的姓名、部门和邮箱来自数据库。请求执行链为：

```text
GET /employees/1
  → EmployeeController.findById(1)
  → EmployeeService.findById(1)
  → EmployeeMapper代理
  → SqlSessionTemplate与JDBC驱动
  → MySQL执行EmployeeMapper.xml中的SELECT
  → Employee Entity
  → EmployeeResponse
  → ApiResponse与HTTP 200
```

`SqlSessionFactory` 是MyBatis核心接口 `org.apache.ibatis.session.SqlSessionFactory`，保存解析后的配置并负责创建 `SqlSession` 会话。`SqlSessionTemplate` 的完整类名是 `org.mybatis.spring.SqlSessionTemplate`，是MyBatis-Spring提供的线程安全调用入口，会把Mapper调用连接到合适的会话和Spring事务。两个对象都由Starter自动配置，业务代码不自行 `new`，也不手写 `openSession()`、`commit()` 或 `close()`；事务边界应由Spring事务管理。

## 十、常见失败

| 现象 | 定位位置 | 常见原因 | 修正 |
| --- | --- | --- | --- |
| 启动提示无法配置DataSource | 启动日志最前面的根因 | `DB_PASSWORD` 未设置或驱动依赖未加载 | 在启动应用的同一终端设置变量并刷新Maven |
| `Access denied for user` | MySQL返回的连接错误 | 用户名、密码或账号权限错误 | 使用本地应用账号并确认其拥有目标库权限 |
| `Unknown database 'employee_db'` | JDBC连接阶段 | 建库脚本没有执行 | 先执行第二节SQL并确认数据库名 |
| `Invalid bound statement` | MyBatis启动或调用日志 | namespace、XML的id、接口名或扫描路径不一致 | 逐项核对完整接口名、方法名和 `mapper-locations` |
| `Table 'employee_db.employees' doesn't exist` | SQL执行阶段 | 表建在其他数据库或未执行DDL | 在同一连接中执行 `USE employee_db` 后检查表 |
| 查询结果时间字段为 `null` | 结果映射 | SQL缺少别名或Java属性名不一致 | 保留 `created_at AS createdAt` 等别名 |

排错时从异常最底部的根因开始看，并区分“依赖加载、数据库连接、SQL定位、SQL执行、结果映射”五个阶段，不要通过自行创建框架同名类掩盖问题。

## 十一、操作练习

初始状态：现有员工详情已经从MySQL返回。

任务：

1. 在数据库中插入一名新员工，先用SELECT记下生成编号，再通过GET接口查询。
2. 临时把XML中的 `id="findById"` 改为其他名称，记录启动或请求时的错误，再恢复。
3. 临时去掉 `created_at AS createdAt` 的别名，通过调试器或日志观察Entity属性，再恢复。
4. 在不打印密码的前提下，记录数据库地址、请求URL、HTTP状态和响应体作为自测证据。

验收标准：

- 能指出两个Maven依赖分别解决什么问题。
- 能说明DataSource、Mapper代理、XML和Entity的先后关系。
- 能解释 `namespace`、`id`、`resultType` 和 `#{id}` 的对应规则。
- 修改数据库行后，再次请求能观察到响应变化。

## 十二、当前稳定状态

当前工程应保留：

```text
entity/Employee.java
mapper/EmployeeMapper.java
resources/mapper/EmployeeMapper.xml
service/EmployeeService.java
```

详情查询已经使用真实数据库；列表和新增仍是临时实现。完整CRUD需要继续替换Mapper XML、Service和Controller，并补齐新增、修改、删除及404响应。
