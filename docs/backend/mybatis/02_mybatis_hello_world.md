# 第2章 第一个 MyBatis 程序

> 本章目标：使用 Maven、MySQL 和 MyBatis 完成一个最小查询程序，理解项目中每个文件的作用。

## 一、本章完成结果

本章完成后，可以通过 Java 程序查询 `employees` 表，并在控制台输出员工信息。

目标结果：

```text
Employee{id=1, name='Tanaka', departmentId=10, email='tanaka@example.com'}
Employee{id=2, name='Suzuki', departmentId=20, email='suzuki@example.com'}
```

## 二、项目结构

本章使用 Maven 项目。

```text
mybatis-basic
├── pom.xml
└── src
    └── main
        ├── java
        │   └── com
        │       └── example
        │           └── mybatis
        │               ├── Main.java
        │               ├── entity
        │               │   └── Employee.java
        │               └── mapper
        │                   └── EmployeeMapper.java
        └── resources
            ├── db.properties
            ├── mybatis-config.xml
            └── mapper
                └── EmployeeMapper.xml
```

## 三、Maven 依赖

`pom.xml`：

```xml
<dependencies>
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.15</version>
    </dependency>

    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.4.0</version>
    </dependency>
</dependencies>
```

## 四、准备数据库

MySQL：

```sql
CREATE DATABASE mybatis_training;

USE mybatis_training;

CREATE TABLE departments (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    email VARCHAR(200),
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
);

INSERT INTO departments (id, name)
VALUES
    (10, 'Sales'),
    (20, 'Development');

INSERT INTO employees (name, department_id, email)
VALUES
    ('Tanaka', 10, 'tanaka@example.com'),
    ('Suzuki', 20, 'suzuki@example.com');
```

## 五、数据库连接配置

`src/main/resources/db.properties`：

```properties
driver=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/mybatis_training?serverTimezone=Asia/Tokyo
username=root
password=password
```

## 六、MyBatis 主配置文件

`src/main/resources/mybatis-config.xml`：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <properties resource="db.properties"/>

    <typeAliases>
        <typeAlias type="com.example.mybatis.entity.Employee" alias="Employee"/>
    </typeAliases>

    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${driver}"/>
                <property name="url" value="${url}"/>
                <property name="username" value="${username}"/>
                <property name="password" value="${password}"/>
            </dataSource>
        </environment>
    </environments>

    <mappers>
        <mapper resource="mapper/EmployeeMapper.xml"/>
    </mappers>
</configuration>
```

## 七、实体类

`src/main/java/com/example/mybatis/entity/Employee.java`：

```java
package com.example.mybatis.entity;

public class Employee {
    private Long id;
    private String name;
    private Long departmentId;
    private String email;

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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Employee{id=" + id
                + ", name='" + name + '\''
                + ", departmentId=" + departmentId
                + ", email='" + email + '\''
                + '}';
    }
}
```

## 八、Mapper 接口

`src/main/java/com/example/mybatis/mapper/EmployeeMapper.java`：

```java
package com.example.mybatis.mapper;

import com.example.mybatis.entity.Employee;
import java.util.List;

public interface EmployeeMapper {
    List<Employee> selectAll();
}
```

## 九、Mapper XML

`src/main/resources/mapper/EmployeeMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mybatis.mapper.EmployeeMapper">
    <select id="selectAll" resultType="Employee">
        SELECT
            id,
            name,
            department_id AS departmentId,
            email
        FROM employees
        ORDER BY id
    </select>
</mapper>
```

`namespace` 必须和 Mapper 接口的全限定名一致。

`id="selectAll"` 必须和接口方法名一致。

## 十、运行程序

`src/main/java/com/example/mybatis/Main.java`：

```java
package com.example.mybatis;

import com.example.mybatis.entity.Employee;
import com.example.mybatis.mapper.EmployeeMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            EmployeeMapper employeeMapper = sqlSession.getMapper(EmployeeMapper.class);
            List<Employee> employees = employeeMapper.selectAll();

            for (Employee employee : employees) {
                System.out.println(employee);
            }
        }
    }
}
```

## 十一、运行流程

1. 读取 `mybatis-config.xml`。
2. 创建 `SqlSessionFactory`。
3. 打开 `SqlSession`。
4. 获取 `EmployeeMapper` 代理对象。
5. 调用 `selectAll()`。
6. 执行 `EmployeeMapper.xml` 中的 SQL。
7. 把查询结果转换成 `Employee` 对象。
8. 输出结果。

## 十二、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 找不到 XML | `mapper resource` 路径错误 | 确认 `resources/mapper/EmployeeMapper.xml` |
| Mapper 方法找不到 | `namespace` 或 `id` 不一致 | namespace 对应接口全限定名，id 对应方法名 |
| 字段没有值 | 数据库列名和 Java 属性名不一致 | 使用别名或 `resultMap` |
| 连接失败 | 数据库地址、账号或密码错误 | 检查 `db.properties` |

## 十三、本章练习

请完成：

1. 按本章结构创建项目。
2. 查询所有员工。
3. 修改 SQL，只查询 `department_id = 20` 的员工。
4. 故意改错 `namespace`，观察错误信息。

## 十四、本章总结

- MyBatis 最小程序需要配置文件、实体类、Mapper 接口和 Mapper XML。
- Mapper 接口方法通过 XML 中的 `namespace` 和 `id` 找到 SQL。
- 查询结果可以映射成 Java 对象。
