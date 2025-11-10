# 第4章 MyBatis 主配置文件详解 (`mybatis-config.xml`)

---

## 一、配置文件的作用

`mybatis-config.xml` 是 **MyBatis 的全局配置文件**，用于定义和控制 MyBatis 运行行为。

包括：

* 环境配置（数据源、事务管理）
* 类型别名和处理器
* 全局设置项（日志、驼峰名转换）
* Mapper 映射文件注册

---

## 二、`<configuration>` 根标签

* **作用**：MyBatis 全部配置的根节点。
* **属性**：无。
* **注意**：必须包含 DTD 声明，否则不能被 MyBatis 正确解析。

**示例**：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    ...
</configuration>
```

---

## 三、`<properties>` 属性配置

* 作用：引入外部属性文件（如数据库连接信息）。
* 属性：

    * `resource`：指定类路径下的文件，如 `db.properties`
    * `url`：指定文件绝对路径（不常用）

**示例**：

```xml
<properties resource="db.properties" />
```

**db.properties** 内容：

```properties
driver=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/test
username=root
password=123456
```

**说明**：引入后，可在文件中使用 `${}` 占位符，如下：

```xml
<property name="url" value="${url}" />
```

---

## 四、`<settings>` 全局设置项

* 作用：控制 MyBatis 运行行为。
* 子标签：`<setting name="" value=""/>`

**示例**：

```xml
<settings>
  <setting name="cacheEnabled" value="true"/>
  <setting name="mapUnderscoreToCamelCase" value="true"/>
  <setting name="logImpl" value="STDOUT_LOGGING"/>
</settings>
```

**常用属性**：

* `cacheEnabled`：是否启用二级缓存，默认 `true`
* `lazyLoadingEnabled`：是否启用延迟加载，默认 `false`
* `mapUnderscoreToCamelCase`：自动下划线名称转驼峰，默认 `false`
* `logImpl`：日志实现：`STDOUT_LOGGING`，`LOG4J`，`SLF4J`，`NONE`

---

## 五、`<typeAliases>` 类型别名

* 作用：给 Java 类起别名，减少包名冗长写法。
* 子标签：`<typeAlias>` 和 `<package>`

**示例1：单个别名**

```xml
<typeAliases>
  <typeAlias type="com.example.entity.User" alias="User"/>
</typeAliases>
```

**示例2：包扫描**

```xml
<typeAliases>
  <package name="com.example.entity"/>
</typeAliases>
```

**属性**：

* `type`：类全名（必填）
* `alias`：别名（可选，默认类名）

---

## 六、`<typeHandlers>` 类型处理器

* 作用：定义 Java 和 JDBC 类型之间的自定义转换。
* 子标签：`<typeHandler handler="" javaType="" jdbcType=""/>`

**示例**：

```xml
<typeHandlers>
  <typeHandler handler="com.example.handler.SexTypeHandler"
               javaType="com.example.enums.SexEnum"
               jdbcType="VARCHAR"/>
</typeHandlers>
```

**属性**：

* `handler`：处理类完整路径，需继承 `BaseTypeHandler`
* `javaType`：Java 类型
* `jdbcType`：数据库字段类型

---

## 七、`<environments>` 环境配置

* 作用：定义多个数据库环境。
* 属性：

    * `default`：默认使用的环境 ID

**示例**：

```xml
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
```

### 1. `<environment>`

* 属性：`id`：环境标识符，如 `dev`、`prod`
* 包含：`<transactionManager>` 和 `<dataSource>`

### 2. `<transactionManager>`

* 作用：指定事务管理方式。
* 属性：

    * `type`：事务类型，取值：

        * `JDBC`：使用 JDBC 原生事务
        * `MANAGED`：由 Spring 或 JEE 容器管理

### 3. `<dataSource>`

* 作用：配置数据库连接池。
* 属性：

    * `type`：数据源类型，取值：

        * `POOLED`：使用连接池（推荐）
        * `UNPOOLED`：没有连接池
        * `JNDI`：使用容器 JNDI 数据源
* **子标签**：`<property name="" value=""/>`，定义驱动、URL、用户名、密码等。

---

## 八、`<mappers>` 映射文件注册

* 作用：注册 SQL 映射文件。
* 四种方式：

    * `<mapper resource="mapper/UserMapper.xml"/>`：指定路径
    * `<mapper class="com.example.mapper.UserMapper"/>`：通过接口类
    * `<mapper url="file:///D:/UserMapper.xml"/>`：通过绝对路径
    * `<package name="com.example.mapper"/>`：包扫描

**推荐**：在 Spring Boot 项目中使用 `@MapperScan` 和 `<package>` 方式。

---

## 九、通用配置示例

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "https://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
  <properties resource="db.properties" />
  <settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
    <setting name="logImpl" value="STDOUT_LOGGING"/>
  </settings>
  <typeAliases>
    <package name="com.example.entity"/>
  </typeAliases>
  <typeHandlers>
    <typeHandler handler="com.example.handler.DateTypeHandler"
                 javaType="java.util.Date"
                 jdbcType="TIMESTAMP"/>
  </typeHandlers>
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
  <mappers>
    <package name="com.example.mapper"/>
  </mappers>
</configuration>
```

---

✅ **总结**：

* 该文件控制 MyBatis 的全局行为，是整个 ORM 层的核心。
* 理解各标签和属性，是后续添加 Spring 集成、动态 SQL 和缓存机制的基础。
