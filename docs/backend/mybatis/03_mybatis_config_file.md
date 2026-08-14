# 第3章 MyBatis 配置文件

> 本章目标：理解 `mybatis-config.xml` 和 `db.properties` 的作用，掌握 MyBatis 主配置文件中的常用标签。

## 一、配置文件的作用

MyBatis 配置文件用于告诉 MyBatis：

- 数据库连接信息在哪里。
- 使用哪个数据库环境。
- 实体类别名怎么配置。
- Mapper XML 文件在哪里。
- 是否开启某些全局设置。

常见配置文件：

| 文件 | 作用 |
| --- | --- |
| `db.properties` | 保存数据库连接信息 |
| `mybatis-config.xml` | MyBatis 主配置文件 |
| `EmployeeMapper.xml` | SQL 映射文件 |

## 二、db.properties

```properties
driver=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/mybatis_training?serverTimezone=Asia/Tokyo
username=root
password=password
```

说明：

| 配置 | 作用 |
| --- | --- |
| `driver` | MySQL JDBC 驱动类 |
| `url` | 数据库连接地址 |
| `username` | 数据库用户名 |
| `password` | 数据库密码 |

## 三、mybatis-config.xml 基本结构

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <properties resource="db.properties"/>
    <settings/>
    <typeAliases/>
    <environments default="development"/>
    <mappers/>
</configuration>
```

常用标签：

| 标签 | 作用 |
| --- | --- |
| `<properties>` | 读取外部属性文件 |
| `<settings>` | 配置 MyBatis 全局行为 |
| `<typeAliases>` | 设置类型别名 |
| `<environments>` | 配置数据库环境 |
| `<mappers>` | 注册 Mapper XML |

## 四、properties

```xml
<properties resource="db.properties"/>
```

读取后可以使用 `${配置名}`：

```xml
<property name="driver" value="${driver}"/>
```

这里的 `${driver}` 来自 `db.properties`。

## 五、settings

常用设置：

```xml
<settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
</settings>
```

`mapUnderscoreToCamelCase` 表示下划线字段自动映射为驼峰属性。

例如：

| 数据库列 | Java 属性 |
| --- | --- |
| `department_id` | `departmentId` |
| `create_time` | `createTime` |

### 5.1 settings 常用 name 属性

`<setting>` 的 `name` 表示要配置的全局设置项。

语法：

```xml
<setting name="设置项名称" value="设置值"/>
```

常用设置：

| name | 常见 value | 作用 |
| --- | --- | --- |
| `mapUnderscoreToCamelCase` | `true` / `false` | 是否开启下划线转驼峰 |
| `cacheEnabled` | `true` / `false` | 是否开启二级缓存总开关 |
| `lazyLoadingEnabled` | `true` / `false` | 是否开启懒加载 |
| `aggressiveLazyLoading` | `true` / `false` | 是否触发对象中所有懒加载属性 |
| `logImpl` | `STDOUT_LOGGING` / `SLF4J` / `LOG4J2` | 指定 MyBatis 日志实现 |
| `defaultStatementTimeout` | 秒数 | SQL 执行默认超时时间 |
| `defaultFetchSize` | 数字 | 查询时建议每次抓取的行数 |
| `jdbcTypeForNull` | `NULL` / `VARCHAR` / `OTHER` | 参数为 `null` 时使用的 JDBC 类型 |
| `useGeneratedKeys` | `true` / `false` | 是否默认允许 JDBC 自动生成主键 |
| `autoMappingBehavior` | `NONE` / `PARTIAL` / `FULL` | 自动映射字段和属性的策略 |

### 5.2 常用配置示例

```xml
<settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
    <setting name="logImpl" value="STDOUT_LOGGING"/>
    <setting name="defaultStatementTimeout" value="30"/>
</settings>
```

说明：

- `mapUnderscoreToCamelCase`：让 `department_id` 自动映射到 `departmentId`。
- `logImpl`：输出 MyBatis 执行 SQL 的日志，学习阶段常用 `STDOUT_LOGGING`。
- `defaultStatementTimeout`：设置 SQL 默认超时时间，避免 SQL 长时间卡住。

### 5.3 学习阶段建议

学习 MyBatis 基础时，建议先掌握：

| 设置项 | 掌握程度 |
| --- | --- |
| `mapUnderscoreToCamelCase` | 必须掌握 |
| `logImpl` | 建议掌握 |
| `cacheEnabled` | 学到缓存章节时掌握 |
| `lazyLoadingEnabled` | 学到关联查询时了解 |
| 其他设置 | 能看懂即可 |

## 六、typeAliases

类型别名可以简化 XML 中的 Java 类名。

```xml
<typeAliases>
    <typeAlias type="com.example.mybatis.entity.Employee" alias="Employee"/>
</typeAliases>
```

配置后可以写：

```xml
<select id="selectAll" resultType="Employee">
```

不需要写完整类名。

## 七、environments

```xml
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
```

说明：

| 标签 | 作用 |
| --- | --- |
| `<environments>` | 可以配置多个数据库环境 |
| `<environment>` | 一个具体环境 |
| `<transactionManager>` | 事务管理方式 |
| `<dataSource>` | 数据源配置 |

本课程使用 `JDBC` 事务管理和 `POOLED` 数据源。

## 八、mappers

```xml
<mappers>
    <mapper resource="mapper/EmployeeMapper.xml"/>
</mappers>
```

`resource` 表示从 `src/main/resources` 下查找 Mapper XML。

## 九、完整配置示例

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <properties resource="db.properties"/>

    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>

    <typeAliases>
        <typeAlias type="com.example.mybatis.entity.Employee" alias="Employee"/>
        <typeAlias type="com.example.mybatis.entity.Department" alias="Department"/>
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
        <mapper resource="mapper/DepartmentMapper.xml"/>
    </mappers>
</configuration>
```

## 十、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| `${driver}` 读取不到 | `db.properties` 路径错误 | 确认文件在 `resources` 下 |
| Mapper XML 未生效 | `<mappers>` 没注册 | 添加 `<mapper resource="..."/>` |
| 驼峰映射不生效 | 没开启设置或字段不匹配 | 开启 `mapUnderscoreToCamelCase` |
| 环境 ID 写错 | `default` 找不到对应环境 | 确认 `default` 和 `environment id` 一致 |

## 十一、本章练习

请完成：

1. 创建 `db.properties`。
2. 创建 `mybatis-config.xml`。
3. 开启下划线转驼峰。
4. 注册 `EmployeeMapper.xml`。

## 十二、本章总结

- `mybatis-config.xml` 是 MyBatis 主配置文件。
- `<properties>` 用于读取外部配置。
- `<environments>` 配置数据库连接。
- `<mappers>` 注册 SQL 映射文件。
