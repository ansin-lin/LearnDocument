# 第2章 MyBatis 实战

---

## 一、项目创建与结构

### 1️⃣ 使用 Maven 创建项目

**命令方式：**

```bash
mvn archetype:generate -DgroupId=com.example     -DartifactId=mybatis-helloworld     -DarchetypeArtifactId=maven-archetype-quickstart     -DinteractiveMode=false
```

**目录结构生成：**

```java
mybatis-helloworld/
 ├─ pom.xml
 ├─ src/
 │   ├─ main/
 │   │   ├─ java/com/example/
 │   │   │   ├─ entity/User.java
 │   │   │   ├─ mapper/UserMapper.java
 │   │   └─ resources/
 │   │       ├─ mybatis-config.xml
 │   │       └─ mapper/UserMapper.xml
 │   └─ test/java/com/example/MainTest.java
```

---

### 2️⃣ 使用 Gradle 创建项目

**命令方式：**

```bash
gradle init --type java-application
```

修改 `build.gradle`：

```groovy
plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.mybatis:mybatis:3.5.15'
    implementation 'mysql:mysql-connector-j:8.4.0'
    testImplementation 'junit:junit:4.13.2'
}
```



### 3️⃣ 使用 Eclipse 创建项目

**步骤说明：**

1. **打开 Eclipse → File → New → Maven Project**  
   - 如果提示「Create a simple project」，勾选后点击 **Next**。  
   - 选择 **Use default Workspace location**（默认路径）或自定义路径。  

2. **配置项目信息：**
   - **Group Id**：`com.example`  
   - **Artifact Id**：`mybatis-helloworld`  
   - **Version**：保持默认（如 `1.0-SNAPSHOT`）  
   - **Packaging**：选择 `jar`  
   - **Name**：可填 `MyBatis HelloWorld`  
   - 点击 **Finish**。

3. **添加 MyBatis 依赖：**  
   打开项目根目录下的 `pom.xml`，在 `<dependencies>` 标签中添加：

   ```xml
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.15</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>5.1.49</version>
    </dependency>
   ```

4. **创建包结构：**
   在 `src/main/java` 下手动创建：

   ```java
   com.example.entity
   com.example.mapper
   ```

   在 `src/main/resources` 下创建：

   ```java
   mapper/
   mybatis-config.xml
   db.properties
   ```

5. **右键项目 → Build Path → Configure Build Path → Libraries → Add Library → JRE System Library**  
   确保 JDK 已正确配置。

6. **执行测试：**
   - 右键 `MainTest.java` → `Run As → Java Application`
   - 控制台输出：

     ```java
     User{id=1, name='Alice', age=20}
     User{id=2, name='Bob', age=22}
     ```


---

## 二、数据库准备

```sql
CREATE DATABASE test CHARACTER SET utf8mb4;
USE test;

CREATE TABLE user (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50),
  age INT
);

INSERT INTO user(name, age)
VALUES ('Alice', 20), ('Bob', 22);
```

---

## 三、配置文件准备

### 1️⃣ `resources/db.properties`

```properties
driver=com.mysql.jdbc.Driver
url=jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=Asia/Tokyo
username=root
password=123456
```

---

### 2️⃣ `resources/mybatis-config.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "https://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>

  <!-- 引入数据库配置文件 -->
  <properties resource="mapper/db.properties"/>

  <!-- 类型别名 -->
  <typeAliases>
    <package name="com.example.entity"/>
  </typeAliases>

  <!-- 环境配置 -->
  <environments default="dev">
    <environment id="dev">
      <transactionManager type="JDBC"/>
      <dataSource type="POOLED">
        <property name="driver" value="${driver}"/>
        <property name="url" value="${url}"/>
        <property name="username" value="${username}"/>
        <property name="password" value="${password}"/>
      </dataSource>
    </environment>
  </environments>

  <!-- 映射文件注册 -->
  <mappers>
    <mapper resource="mapper/UserMapper.xml"/>
  </mappers>

</configuration>
```

---

## 四、代码编写

### 1️⃣ 实体类  

**路径：** `src/main/java/com/example/entity/User.java`

```java
package com.example.entity;

public class User {
    private Integer id;
    private String name;
    private Integer age;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', age=" + age + '}';
    }
}
```

---

### 2️⃣ Mapper 接口  

**路径：** `src/main/java/com/example/mapper/UserMapper.java`

```java
package com.example.mapper;

import com.example.entity.User;
import java.util.List;

public interface UserMapper {
    List<User> selectAllUsers();
}
```

---

### 3️⃣ Mapper 映射文件  

**路径：** `src/main/resources/mapper/UserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.UserMapper">

  <!-- 查询全部用户 -->
  <select id="selectAllUsers" resultType="com.example.entity.User">
    SELECT id, name, age FROM user;
  </select>

</mapper>
```

---

### 4️⃣ 测试类（主方法）  

**路径：** `src/test/java/com/example/MainTest.java`

```java
package com.example;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;

import java.io.InputStream;
import java.util.List;

public class MainTest {
    public static void main(String[] args) throws Exception {
        // 1. 读取 MyBatis 配置文件
        InputStream is = Resources.getResourceAsStream("mapper/mybatis-config.xml");

        // 2. 构建 SqlSessionFactory
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(is);

        // 3. 打开 SqlSession
        try (SqlSession session = factory.openSession()) {
            // 4. 获取 Mapper 接口
            UserMapper mapper = session.getMapper(UserMapper.class);

            // 5. 执行查询
            List<User> list = mapper.selectAllUsers();

            // 6. 输出结果
            list.forEach(System.out::println);
        }
    }
}
```

---

## 五、运行结果

控制台输出：

```java
User{id=1, name='Alice', age=20}
User{id=2, name='Bob', age=22}
```

---

## 六、项目运行总结

- **核心流程：**
  1. MyBatis 读取配置文件  
  2. 构建 SqlSessionFactory  
  3. 获取 SqlSession（会话）  
  4. 获取 Mapper 代理对象  
  5. 执行 SQL，返回结果  
  6. 关闭会话  

- **文件位置对应关系：**

| 文件名 | 类路径/资源路径 | 作用 |
|---------|-----------------|------|
| `User.java` | `com/example/entity/User.java` | 实体类 |
| `UserMapper.java` | `com/example/mapper/UserMapper.java` | Mapper 接口 |
| `UserMapper.xml` | `resources/mapper/UserMapper.xml` | SQL 映射文件 |
| `mybatis-config.xml` | `resources/mybatis-config.xml` | 全局配置 |
| `db.properties` | `resources/db.properties` | 数据库连接配置 |
| `MainTest.java` | `com/example/MainTest.java` | 测试入口 |
