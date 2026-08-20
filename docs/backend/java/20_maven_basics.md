# 第20章 Maven 基础

> 本章目标：理解 Maven 的作用，掌握 Maven 项目结构、`pom.xml`、依赖、scope 和常用构建命令。

## 一、Maven 是什么

Maven 是 Java 项目常用的构建和依赖管理工具。

它主要解决：

- 下载和管理第三方依赖
- 编译 Java 代码
- 执行测试
- 打包项目
- 统一项目目录结构

## 二、Maven 标准目录结构

```text
employee-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   └── resources
    └── test
        └── java
```

| 路径 | 作用 |
| --- | --- |
| `pom.xml` | Maven 配置文件 |
| `src/main/java` | 正式 Java 代码 |
| `src/main/resources` | 配置文件 |
| `src/test/java` | 测试代码 |

## 三、pom.xml 基本结构

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>employee-demo</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

| 标签 | 作用 |
| --- | --- |
| `groupId` | 公司或组织标识 |
| `artifactId` | 项目名 |
| `version` | 项目版本 |
| `dependencies` | 项目依赖列表 |
| `dependency` | 一个具体依赖 |

## 四、依赖是什么

依赖就是项目需要使用的外部库。

例如 JUnit 依赖用于写测试，MySQL 驱动用于连接 MySQL。

Maven 会根据 `pom.xml` 自动下载依赖到本地仓库。

## 五、scope

`scope` 表示依赖的使用范围。

也就是说，Maven 需要知道这个依赖是在什么阶段使用：

- 编写正式代码时是否需要
- 编译正式代码时是否需要
- 运行程序时是否需要
- 执行测试时是否需要

如果 `scope` 写错，代码可能会出现“编译时能用，运行时报错”或“测试依赖被放进正式程序”的问题。

| scope | 作用 |
| --- | --- |
| `compile` | 默认范围，主代码和测试都可用 |
| `test` | 只在测试代码中使用 |
| `runtime` | 编译不需要，运行需要 |
| `provided` | 编译需要，运行环境提供 |

例如 JUnit 只用于测试代码，所以通常写：

```xml
<scope>test</scope>
```

MySQL 驱动在代码中通常不直接调用驱动类，但程序运行时需要它连接数据库，所以通常写：

```xml
<scope>runtime</scope>
```

## 六、常用命令

| 命令 | 作用 |
| --- | --- |
| `mvn compile` | 编译主代码 |
| `mvn test` | 执行测试 |
| `mvn package` | 打包项目 |
| `mvn clean` | 清理构建结果 |
| `mvn clean package` | 清理后重新打包 |

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 依赖下载失败 | 网络或仓库配置问题 | 检查网络和 Maven settings |
| 类找不到 | 没有添加依赖 | 检查 `pom.xml` |
| scope 写错 | 运行时依赖不可用 | 根据依赖用途选择 scope |
| 项目目录不标准 | Maven 找不到源码 | 按标准目录放代码 |

## 八、本章练习

请完成：

1. 创建 Maven 标准目录结构。
2. 添加 JUnit 依赖。
3. 执行 `mvn test`。
4. 说明 `dependency` 和 `scope` 的作用。
