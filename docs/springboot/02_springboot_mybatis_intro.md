# Spring Boot + MyBatis 入门案例

## 一、项目结构

```java
springboot-mybatis-demo/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/                包名
│   │   │       ├── controller/                  包名
│   │   │       │   ├── UserController.java
│   │   │       │   └── ProductController.java
│   │   │       ├── entity/                      包名
│   │   │       │   ├── User.java
│   │   │       │   └── Product.java
│   │   │       ├── mapper/                      包名
│   │   │       │   ├── UserMapper.java
│   │   │       │   └── ProductMapper.java
│   │   │       ├── service/                     包名
│   │   │       │   ├── UserService.java
│   │   │       │   └── ProductService.java
│   │   │       └── DemoApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── mapper/ProductMapper.xml
│   └── test/java/
│       └── com/example/demo/                    包名
│           └── DemoApplicationTests.java
└── pom.xml
```

---

## 二、pom.xml 依赖配置

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>springboot-mybatis-demo</name>

    <properties>
        <java.version>23</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- MyBatis -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.4</version>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- MyBatis Test -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter-test</artifactId>
            <version>3.0.4</version>
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

---

## 三、application.properties 配置

```properties
# 服务器端口
server.port=8080

# 数据源配置
spring.datasource.url=jdbc:mysql://localhost:3306/mysql_learn?serverTimezone=UTC&characterEncoding=utf8&useSSL=false
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis Mapper 文件路径
mybatis.mapper-locations=classpath:mapper/*.xml
```

---

## 四、实体类

```java
// User.java
package com.example.demo.entity;

public class User {
    private Long id;
    private String name;
    private String email;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

// Product.java
package com.example.demo.entity;

public class Product {
    private Long id;
    private String name;
    private Double price;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
```

---

## 五、Mapper 接口

```java
// UserMapper.java
package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper //让 MyBatis 在启动时自动生成该接口的代理对象，并将其注册为 Spring 容器中的 Bean。
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User getUserById(@Param("id") Long id);

    @Select("SELECT * FROM users")
    List<User> getAllUsers();

    @Insert("INSERT INTO users(name, email) VALUES(#{name}, #{email})")
    void insertUser(User user);

    @Update("UPDATE users SET name = #{name}, email = #{email} WHERE id = #{id}")
    void updateUser(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteUser(@Param("id") Long id);
}

// ProductMapper.java
package com.example.demo.mapper;

import com.example.demo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ProductMapper {
    Product getProductById(Long id);
    List<Product> getAllProducts();
    void insertProduct(Product product);
    void updateProduct(Product product);
    void deleteProduct(Long id);
}
```

---

## 六、ProductMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.mapper.ProductMapper">

    <select id="getProductById" parameterType="long" resultType="com.example.demo.entity.Product">
        SELECT * FROM products WHERE id = #{id}
    </select>

    <select id="getAllProducts" resultType="com.example.demo.entity.Product">
        SELECT * FROM products
    </select>

    <insert id="insertProduct" parameterType="com.example.demo.entity.Product">
        INSERT INTO products(name, price) VALUES(#{name}, #{price})
    </insert>

    <update id="updateProduct" parameterType="com.example.demo.entity.Product">
        UPDATE products SET name = #{name}, price = #{price} WHERE id = #{id}
    </update>

    <delete id="deleteProduct" parameterType="long">
        DELETE FROM products WHERE id = #{id}
    </delete>
</mapper>
```

---

## 七、服务层

```java
// UserService.java
package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service  //标识一个类是业务逻辑层的Bean
public class UserService {
    private final UserMapper userMapper;
    @Autowired
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<User> getAllUsers() { return userMapper.getAllUsers(); }
    public User getUserById(Long id) { return userMapper.getUserById(id); }
    public void createUser(User user) { userMapper.insertUser(user); }
    public void updateUser(User user) { userMapper.updateUser(user); }
    public void deleteUser(Long id) { userMapper.deleteUser(id); }
}

// ProductService.java
package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<Product> getAllProducts() { return productMapper.getAllProducts(); }
    public Product getProductById(Long id) { return productMapper.getProductById(id); }
    public void createProduct(Product product) { productMapper.insertProduct(product); }
    public void updateProduct(Product product) { productMapper.updateProduct(product); }
    public void deleteProduct(Long id) { productMapper.deleteProduct(id); }
}
```

---

## 八、控制器层

```java
// UserController.java
package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController  //控制器,自动将方法返回值作为 HTTP 响应体
@RequestMapping("/users")  //表示该类中的所有方法都以此路径为前缀。
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() { return userService.getAllUsers(); }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) { return userService.getUserById(id); }

    @PostMapping
    public void createUser(@RequestBody User user) { userService.createUser(user); }

    @PutMapping
    public void updateUser(@RequestBody User user) { userService.updateUser(user); }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) { userService.deleteUser(id); }
}

// ProductController.java
package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAllProducts() { return productService.getAllProducts(); }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) { return productService.getProductById(id); }

    @PostMapping
    public void createProduct(@RequestBody Product product) { productService.createProduct(product); }

    @PutMapping
    public void updateProduct(@RequestBody Product product) { productService.updateProduct(product); }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) { productService.deleteProduct(id); }
}
```

---

## 九、主启动类

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication //主启动类，入口注解
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

---

## 十、数据库初始化 SQL

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    price DECIMAL(10, 2)
);
```

---

✅ **运行方式**：

1. 启动 MySQL 数据库并创建 `mysql_learn` 数据库。
2. 执行以上建表 SQL。
3. 运行 `DemoApplication` 启动项目。
4. 访问接口：
   - `GET http://localhost:8080/users`
   - `GET http://localhost:8080/products`

该案例涵盖了 **Spring Boot + MyBatis** 的核心结构：Controller、Service、Mapper、XML 配置、实体类、数据库映射，是入门级完整实战模板。
