# 第20章 Java JDBC 数据库连接

> 本章目标：在 Maven 项目中使用 JDBC 连接 MySQL，掌握查询、新增、修改、删除、参数绑定和资源关闭的基本写法。

## 一、JDBC 是什么

JDBC 是 Java 连接数据库的标准 API。

Java 程序不能直接操作 MySQL 表，需要通过 JDBC 建立连接、发送 SQL、接收执行结果。

JDBC 常见操作：

- 查询数据：`SELECT`
- 新增数据：`INSERT`
- 修改数据：`UPDATE`
- 删除数据：`DELETE`

这四类操作通常称为 CRUD。

| 操作 | 英文 | SQL |
| --- | --- | --- |
| 新增 | Create | `INSERT` |
| 查询 | Read | `SELECT` |
| 修改 | Update | `UPDATE` |
| 删除 | Delete | `DELETE` |

## 二、准备数据库表

数据库：MySQL

下面的 SQL 用于创建员工表，并准备两条测试数据。

```sql
CREATE DATABASE IF NOT EXISTS employee_db;

USE employee_db;

DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    email VARCHAR(200)
);

INSERT INTO employees (name, department, email)
VALUES
    ('Tanaka', 'Sales', 'tanaka@example.com'),
    ('Suzuki', 'Development', 'suzuki@example.com');
```

表结构说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 员工 ID，主键，自动增长 |
| `name` | `VARCHAR(100)` | 员工姓名，不允许为空 |
| `department` | `VARCHAR(100)` | 部门名称，不允许为空 |
| `email` | `VARCHAR(200)` | 邮箱，可以为空 |

## 三、添加 MySQL 驱动

JDBC 是 Java 的标准 API，但连接 MySQL 需要 MySQL 驱动。

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
    <scope>runtime</scope>
</dependency>
```

`scope` 使用 `runtime`，表示编译代码时通常不直接依赖驱动类，但程序运行连接 MySQL 时需要这个驱动。

## 四、JDBC 基本执行流程

JDBC 操作数据库一般按下面顺序：

1. 准备数据库连接信息。
2. 编写 SQL。
3. 通过 `DriverManager` 获取 `Connection`。
4. 通过 `Connection` 创建 `PreparedStatement`。
5. 给 SQL 中的 `?` 设置参数。
6. 执行 SQL。
7. 处理结果。
8. 关闭资源。

在代码中推荐使用 try-with-resources 自动关闭资源。

## 五、查询数据 SELECT

查询用于从数据库中读取数据。

`SELECT` 通常使用 `executeQuery()` 执行，返回 `ResultSet`。

完整示例：

```java
import java.sql.Connection; // 导入数据库连接对象
import java.sql.DriverManager; // 导入连接管理工具
import java.sql.PreparedStatement; // 导入预编译 SQL 对象
import java.sql.ResultSet; // 导入查询结果对象

public class JdbcSelectDemo {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/employee_db?serverTimezone=Asia/Tokyo"; // 数据库连接地址
        String username = "root"; // 数据库用户名
        String password = "password"; // 数据库密码，学习时使用占位值

        String sql = "SELECT id, name, department, email FROM employees WHERE id = ?"; // 查询指定 ID 员工

        try (Connection connection = DriverManager.getConnection(url, username, password); // 获取数据库连接
             PreparedStatement statement = connection.prepareStatement(sql)) { // 创建 PreparedStatement

            statement.setLong(1, 1L); // 给第 1 个 ? 设置 long 类型参数

            try (ResultSet resultSet = statement.executeQuery()) { // 执行查询，返回结果集
                if (resultSet.next()) { // 判断是否查询到下一行数据
                    System.out.println(resultSet.getLong("id")); // 输出：员工 ID
                    System.out.println(resultSet.getString("name")); // 输出：员工姓名
                    System.out.println(resultSet.getString("department")); // 输出：部门名称
                    System.out.println(resultSet.getString("email")); // 输出：邮箱
                }
            }
        }
    }
}
```

关键点：

- `?` 是参数占位符。
- `statement.setLong(1, 1L)` 表示给第 1 个 `?` 设置值。
- `executeQuery()` 用于执行查询 SQL。
- `ResultSet` 保存查询结果。
- `resultSet.next()` 会移动到下一行，有数据返回 `true`，没有数据返回 `false`。

## 六、新增数据 INSERT

新增用于向表中插入一条新数据。

`INSERT` 通常使用 `executeUpdate()` 执行，返回影响行数。

完整示例：

```java
import java.sql.Connection; // 导入数据库连接对象
import java.sql.DriverManager; // 导入连接管理工具
import java.sql.PreparedStatement; // 导入预编译 SQL 对象

public class JdbcInsertDemo {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/employee_db?serverTimezone=Asia/Tokyo"; // 数据库连接地址
        String username = "root"; // 数据库用户名
        String password = "password"; // 数据库密码，学习时使用占位值

        String sql = "INSERT INTO employees (name, department, email) VALUES (?, ?, ?)"; // 新增员工 SQL

        try (Connection connection = DriverManager.getConnection(url, username, password); // 获取数据库连接
             PreparedStatement statement = connection.prepareStatement(sql)) { // 创建 PreparedStatement

            statement.setString(1, "Yamada"); // 给第 1 个 ? 设置员工姓名
            statement.setString(2, "General Affairs"); // 给第 2 个 ? 设置部门名称
            statement.setString(3, "yamada@example.com"); // 给第 3 个 ? 设置邮箱

            int affectedRows = statement.executeUpdate(); // 执行新增，返回影响行数

            System.out.println(affectedRows); // 输出：1
        }
    }
}
```

关键点：

- `executeUpdate()` 用于执行 `INSERT`、`UPDATE`、`DELETE`。
- 返回值是影响行数。
- 新增一条成功时，通常返回 `1`。

## 七、修改数据 UPDATE

修改用于更新表中已有数据。

执行修改时，必须写清楚 `WHERE` 条件。没有 `WHERE` 条件可能会修改整张表。

完整示例：

```java
import java.sql.Connection; // 导入数据库连接对象
import java.sql.DriverManager; // 导入连接管理工具
import java.sql.PreparedStatement; // 导入预编译 SQL 对象

public class JdbcUpdateDemo {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/employee_db?serverTimezone=Asia/Tokyo"; // 数据库连接地址
        String username = "root"; // 数据库用户名
        String password = "password"; // 数据库密码，学习时使用占位值

        String sql = "UPDATE employees SET department = ?, email = ? WHERE id = ?"; // 根据 ID 修改部门和邮箱

        try (Connection connection = DriverManager.getConnection(url, username, password); // 获取数据库连接
             PreparedStatement statement = connection.prepareStatement(sql)) { // 创建 PreparedStatement

            statement.setString(1, "Human Resources"); // 给第 1 个 ? 设置新的部门名称
            statement.setString(2, "tanaka.hr@example.com"); // 给第 2 个 ? 设置新的邮箱
            statement.setLong(3, 1L); // 给第 3 个 ? 设置员工 ID

            int affectedRows = statement.executeUpdate(); // 执行修改，返回影响行数

            System.out.println(affectedRows); // 输出：1
        }
    }
}
```

关键点：

- `UPDATE` 用于修改已有数据。
- `WHERE id = ?` 用于限制修改范围。
- 如果返回 `0`，通常表示没有符合条件的数据。

## 八、删除数据 DELETE

删除用于从表中删除数据。

执行删除时，也必须写清楚 `WHERE` 条件。没有 `WHERE` 条件可能会删除整张表。

完整示例：

```java
import java.sql.Connection; // 导入数据库连接对象
import java.sql.DriverManager; // 导入连接管理工具
import java.sql.PreparedStatement; // 导入预编译 SQL 对象

public class JdbcDeleteDemo {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/employee_db?serverTimezone=Asia/Tokyo"; // 数据库连接地址
        String username = "root"; // 数据库用户名
        String password = "password"; // 数据库密码，学习时使用占位值

        String sql = "DELETE FROM employees WHERE id = ?"; // 根据 ID 删除员工

        try (Connection connection = DriverManager.getConnection(url, username, password); // 获取数据库连接
             PreparedStatement statement = connection.prepareStatement(sql)) { // 创建 PreparedStatement

            statement.setLong(1, 2L); // 给第 1 个 ? 设置员工 ID

            int affectedRows = statement.executeUpdate(); // 执行删除，返回影响行数

            System.out.println(affectedRows); // 输出：1
        }
    }
}
```

关键点：

- `DELETE` 用于删除数据。
- `WHERE id = ?` 用于限制删除范围。
- 删除前要确认条件是否正确。

## 九、关键对象说明

| 对象 | 作用 |
| --- | --- |
| `DriverManager` | 根据连接地址、用户名和密码创建数据库连接 |
| `Connection` | 表示 Java 程序和数据库之间的一次连接 |
| `PreparedStatement` | 表示预编译 SQL，可以安全设置参数 |
| `ResultSet` | 保存 `SELECT` 查询结果 |

`PreparedStatement` 比字符串拼接 SQL 更安全，可以降低 SQL 注入风险。

不推荐：

```java
String sql = "SELECT * FROM employees WHERE name = '" + name + "'";
```

推荐：

```java
String sql = "SELECT id, name, department, email FROM employees WHERE name = ?";
```

## 十、executeQuery 和 executeUpdate 的区别

| 方法 | 用途 | 常用 SQL | 返回值 |
| --- | --- | --- | --- |
| `executeQuery()` | 查询数据 | `SELECT` | `ResultSet` |
| `executeUpdate()` | 修改数据库数据 | `INSERT`、`UPDATE`、`DELETE` | `int`，表示影响行数 |

## 十一、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 连接失败 | 地址、端口、数据库名、账号或密码错误 | 检查连接字符串和 MySQL 服务 |
| 表不存在 | SQL 表名错误或未建表 | 先执行建表 SQL |
| 字段不存在 | Java 代码中的字段名和表字段不一致 | 检查 SQL 和表结构 |
| 中文乱码 | 编码配置不正确 | 确认数据库和连接编码 |
| SQL 注入风险 | 拼接 SQL 字符串 | 使用 `PreparedStatement` |
| 修改或删除范围过大 | 缺少 `WHERE` 条件 | 写操作先确认条件 |
| 连接未关闭 | 没有释放资源 | 使用 try-with-resources |

## 十二、本章练习

请完成：

1. 创建 `employee_db` 数据库。
2. 创建 `employees` 表。
3. 使用 JDBC 查询员工 ID 为 1 的数据。
4. 使用 JDBC 新增一名员工。
5. 使用 JDBC 修改一名员工的部门。
6. 使用 JDBC 删除一名员工。
7. 说明 `PreparedStatement` 的作用。
8. 说明 `executeQuery()` 和 `executeUpdate()` 的区别。

## 十三、本章总结

- JDBC 是 Java 连接数据库的标准 API。
- MySQL 需要添加 MySQL 驱动依赖。
- `SELECT` 使用 `executeQuery()`。
- `INSERT`、`UPDATE`、`DELETE` 使用 `executeUpdate()`。
- `PreparedStatement` 用于参数绑定，可以降低 SQL 注入风险。
- 写操作必须注意 `WHERE` 条件和影响行数。
- 数据库连接、SQL 对象和结果集都应该及时关闭。
