# Java 通过 JDBC 连接数据库（纯 Java 项目示例）

> 本章目标：学习如何在 **非 Maven 的纯 Java 项目** 中，通过 **JDBC** 连接 MySQL 数据库，并在 **Eclipse 环境中运行与调试**。

---

## 一、JDBC 简介

**JDBC（Java Database Connectivity）** 是 Java 官方提供的数据库访问标准接口。  
通过 JDBC，Java 程序可以与各种数据库（MySQL、Oracle、PostgreSQL、SQL Server 等）进行交互。

常见核心组件：

| 类 / 接口 | 功能说明 |
|------------|-----------|
| `DriverManager` | 管理数据库驱动和连接 |
| `Connection` | 表示数据库连接 |
| `Statement` | 执行 SQL 语句 |
| `PreparedStatement` | 执行预编译 SQL，防止 SQL 注入 |
| `ResultSet` | 保存查询结果集 |
| `SQLException` | 异常类，用于捕获数据库错误 |

---

## 二、环境准备

### 1. 安装 MySQL 并创建测试表

```sql
CREATE DATABASE testdb;
USE testdb;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50),
    age INT
);

INSERT INTO users (name, age) VALUES ('Alice', 23), ('Bob', 30);
```

### 2. 下载 MySQL JDBC 驱动

从官网下载最新版：  
👉 [https://dev.mysql.com/downloads/connector/j/](https://dev.mysql.com/downloads/connector/j/)

下载后得到文件：

```java
mysql-connector-j-8.0.33.jar
```

将其放入项目目录下的 `lib` 文件夹中。

---

## 三、项目结构

```java
JdbcDemo/
│
├─ lib/
│    └─ mysql-connector-j-8.0.33.jar
│
└─ src/
     └─ JdbcTest.java
```

---

## 四、编写 Java 代码

**文件：`src/JdbcTest.java`**

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JdbcTest {
    public static void main(String[] args) {
        // 1. 数据库连接信息
        String url = "jdbc:mysql://localhost:3306/testdb?useSSL=false&serverTimezone=Asia/Tokyo";
        String user = "root";
        String password = "your_password";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // 2. 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 3. 建立连接
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ 数据库连接成功！");

            // 4. 创建 Statement 对象
            stmt = conn.createStatement();

            // 5. 执行 SQL 查询
            String sql = "SELECT * FROM users";
            rs = stmt.executeQuery(sql);

            // 6. 处理结果集
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                System.out.printf("ID: %d, 姓名: %s, 年龄: %d%n", id, name, age);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 7. 释放资源
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
```

---

## 五、在 Eclipse 中运行 JDBC 程序

### 1️⃣ 创建 Java 项目

1. 打开 **Eclipse IDE**
2. 菜单栏选择：  
   **File → New → Java Project**
3. 输入项目名称，例如：**JdbcDemo**
4. 点击 **Finish**

---

### 2️⃣ 新建 `src` 源文件夹和测试类

1. 在左侧的 **Package Explorer** 中展开 `JdbcDemo`  
2. 右键 `src` → **New → Class**
3. 类名填写：**JdbcTest**
4. 勾选 “public static void main(String[] args)”  
   点击 **Finish**

---

### 3️⃣ 粘贴上述测试代码

将前面第四步提供的 `JdbcTest` 代码粘贴进来。

---

### 4️⃣ 导入 MySQL JDBC 驱动

> Eclipse 默认不会自动识别 `.jar` 文件，需手动添加到 Build Path。

#### （方法一）项目内 jar（推荐）

1. 将下载好的文件放入项目：

   ```java
   JdbcDemo/lib/mysql-connector-j-8.0.33.jar
   ```

2. 在 Eclipse 中：
   - 右键项目名 `JdbcDemo`
   - 选择 **Build Path → Configure Build Path...**
   - 在弹窗中选择 **Libraries** 页签
   - 点击 **Add JARs...**
   - 找到刚放入的 `lib/mysql-connector-j-8.0.33.jar`
   - 点击 **Apply and Close**

#### （方法二）外部 jar

如果 `.jar` 不在项目内：

1. 右键项目名 → **Build Path → Configure Build Path...**
2. 选择 **Add External JARs...**
3. 定位到驱动文件路径（如 D:\Java\mysql-connector-j-8.0.33.jar）
4. 点击 **Apply and Close**

> ✅ 成功后，你会在 “Referenced Libraries” 中看到 `mysql-connector-j-8.0.33.jar`。

---

### 5️⃣ 运行程序

1. 打开 `JdbcTest.java`
2. 右键 → 选择 **Run As → Java Application**
3. 控制台输出示例：

```bash
✅ 数据库连接成功！
ID: 1, 姓名: Alice, 年龄: 23
ID: 2, 姓名: Bob, 年龄: 30
```

---

### 6️⃣ 常见问题排查

| 问题提示 | 解决办法 |
|-----------|-----------|
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | 未添加 JDBC 驱动到 Build Path |
| `Access denied for user 'root'@'localhost'` | 数据库用户名或密码错误 |
| `Communications link failure` | 数据库未启动或端口号错误 |
| `No suitable driver found for jdbc:mysql://...` | URL 格式错误或驱动未导入 |

---

## 六、使用 PreparedStatement 插入数据

**文件：`src/InsertDemo.java`**

```java
import java.sql.*;

public class InsertDemo {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/testdb?useSSL=false&serverTimezone=Asia/Tokyo";
        String user = "root";
        String password = "your_password";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "INSERT INTO users (name, age) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "Charlie");
            ps.setInt(2, 25);
            int rows = ps.executeUpdate();
            System.out.println("插入成功，影响行数：" + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

> 在 Eclipse 中同样可右键文件 → **Run As → Java Application** 运行。

---

## 七、常见问题排查

| 错误提示 | 原因与解决方案 |
|-----------|----------------|
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | 未正确导入 `mysql-connector-j` 驱动 |
| `Access denied for user 'root'@'localhost'` | 用户名/密码错误或权限不足 |
| `Communications link failure` | 数据库未启动或端口错误 |
| `SSL connection error` | URL 加上参数 `?useSSL=false&serverTimezone=Asia/Tokyo` |

---

## 八、总结

JDBC 访问数据库的标准流程：

1. **加载驱动类**：`Class.forName()`  
2. **建立连接**：`DriverManager.getConnection()`  
3. **执行 SQL**：`Statement` 或 `PreparedStatement`  
4. **处理结果集**：`ResultSet`  
5. **关闭资源**：`close()`

---
