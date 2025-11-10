# 第1章 MyBatis 简介与框架结构

## 一、MyBatis 是什么

MyBatis 是一款优秀的 **持久层框架**，通过 XML 或注解将 SQL 与 Java 对象映射。

- **全称**：MyBatis（前身 iBatis）
- **特点**：
  1. SQL 控制灵活
  2. 学习成本低
  3. 支持动态 SQL、缓存、插件扩展
  4. 轻量级、可与 SpringBoot 无缝整合

## 二、与 JDBC、Hibernate 对比

| 框架 | SQL 控制 | 难度 | 性能优化 | 场景 |
|------|----------|------|----------|------|
| JDBC | 手写 SQL | 低 | 手动优化 | 小型项目 |
| Hibernate | 自动 SQL | 高 | 受限 | 大型项目 |
| MyBatis | 手写+映射 | 中 | 可控 | 企业开发 |

## 三、架构图

```java
   SqlSession
       ↓
   Executor
       ↓
   StatementHandler
       ↓
   JDBC
```

## 四、运行流程

1. 读取配置
2. 构建 SqlSessionFactory
3. 获取 SqlSession
4. 执行 SQL
5. 关闭资源

## 五、Maven 依赖

```xml
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis</artifactId>
  <version>3.5.15</version>
</dependency>
```
