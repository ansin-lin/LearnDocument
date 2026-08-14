# 第1章 MyBatis 简介与使用场景

> 本章目标：理解 MyBatis 是什么、解决 JDBC 哪些问题，以及它在 Java 后端项目中的位置。

## 一、MyBatis 是什么

MyBatis 是 Java 中常用的持久层框架。

持久层负责把 Java 程序中的对象数据保存到数据库，也负责从数据库查询数据并转换成 Java 对象。

MyBatis 的核心特点是：

- SQL 由开发人员自己编写。
- Java 方法和 SQL 语句建立映射关系。
- 查询结果可以自动转换成 Java 对象。
- 可以通过 XML 编写 SQL。
- 支持动态 SQL。

## 二、MyBatis 解决 JDBC 的什么问题

使用 JDBC 时，开发人员需要手动处理很多重复代码。

例如：

- 获取数据库连接
- 创建 `PreparedStatement`
- 设置 SQL 参数
- 执行 SQL
- 遍历 `ResultSet`
- 手动把结果设置到 Java 对象
- 关闭资源

MyBatis 可以减少这些重复代码。

对比：

| 项目 | JDBC | MyBatis |
| --- | --- | --- |
| SQL | 自己写 | 自己写 |
| 参数设置 | 手动 `setXxx` | 使用 `#{}` 绑定 |
| 结果转换 | 手动从 `ResultSet` 取值 | 自动映射到对象 |
| 资源管理 | 手动处理 | 通过 `SqlSession` 管理 |
| 动态 SQL | Java 字符串拼接 | XML 标签生成 |

## 三、MyBatis 适合什么项目

MyBatis 适合需要灵活控制 SQL 的项目。

常见场景：

- 企业后台管理系统
- 订单系统
- 员工管理系统
- 报表查询
- 复杂条件查询
- 需要手写 SQL 优化的系统

日本项目中，MyBatis 常用于 Java Web 系统，尤其是需要明确 SQL、详细设计书中包含 SQL 或数据库访问逻辑较重的项目。

## 四、MyBatis 的基本组成

MyBatis 常见组成：

| 组成 | 作用 |
| --- | --- |
| `mybatis-config.xml` | MyBatis 主配置文件 |
| `db.properties` | 数据库连接信息 |
| Mapper 接口 | Java 中调用数据库的方法 |
| Mapper XML | 编写 SQL 的 XML 文件 |
| 实体类 | 接收查询结果的 Java 对象 |
| `SqlSessionFactory` | 创建数据库会话的工厂 |
| `SqlSession` | 执行 SQL 的会话对象 |

## 五、基本执行流程

MyBatis 的基本执行流程：

```text
读取配置文件
  ↓
创建 SqlSessionFactory
  ↓
打开 SqlSession
  ↓
获取 Mapper 代理对象
  ↓
调用 Mapper 方法
  ↓
执行 XML 中的 SQL
  ↓
返回 Java 对象
```

## 六、本课程统一示例

本课程统一使用：

| 项目 | 内容 |
| --- | --- |
| 构建工具 | Maven |
| 数据库 | MySQL |
| 包名 | `com.example.mybatis` |
| 表 | `employees`、`departments` |
| 实体类 | `Employee`、`Department` |
| Mapper | `EmployeeMapper`、`DepartmentMapper` |
| XML | `EmployeeMapper.xml`、`DepartmentMapper.xml` |

## 七、本章练习

请完成：

1. 说明 MyBatis 是什么。
2. 说明 MyBatis 相比 JDBC 减少了哪些重复代码。
3. 说明 Mapper 接口和 Mapper XML 分别负责什么。

## 八、本章总结

- MyBatis 是 Java 持久层框架。
- MyBatis 不替开发人员隐藏 SQL，而是帮助管理 SQL 和 Java 对象映射。
- MyBatis 的核心是 Mapper 接口、Mapper XML、`SqlSession` 和结果映射。
