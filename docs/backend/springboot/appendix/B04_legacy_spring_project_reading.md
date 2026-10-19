# B04 日本既存Spring项目的新旧结构识读

日本企业项目中仍可能存在Boot 2、Boot 3、WAR、外部Tomcat或XML配置。本附录只建立识读能力，不要求把第18章的Java 17、Spring Boot 4.1.x可执行JAR主线改成旧结构，也不要求重新开发一套XML项目。

前置：先按[B02](B02_existing_project_reading.md)掌握README、构建文件、配置和调用链调查。下表是常见线索而非版本判定规则；版本以有效POM/BOM、依赖树和运行容器为准。

| 调查项 | Boot 4主线常见形态 | Boot 3项目常见形态 | Boot 2或传统MVC项目可能出现 |
| --- | --- | --- | --- |
| Java/Spring | Java 17、Boot 4.1.1、Framework 7 | 按项目POM，Framework 6系 | 按项目POM；传统MVC可能无Boot |
| 启动与打包 | main + 可执行JAR | main + JAR，也可WAR | main/JAR、ServletInitializer/WAR或web.xml/WAR |
| Bean注册 | 注解、自动配置、Java Config | 同左 | 注解、Java Config和XML可混用 |
| 配置文件 | application.yml及外部配置 | 同左 | properties、XML、JNDI等也可能出现 |
| Servlet容器与部署 | 常见内嵌容器，`java -jar` | 内嵌或外部容器 | 外部Tomcat部署WAR较常见，但不是版本标识 |
| Controller映射 | `@RequestMapping`等 | 同左 | 注解映射或更早配置方式；需沿DispatcherServlet检查 |

`javax.*`/`jakarta.*`、WAR/JAR、XML/注解均不能单独推出Boot版本；同一个项目可能混合配置。

## 一、先确认版本，再判断代码是否正确

Spring Boot 4基于Spring Framework 7并继续使用Jakarta命名空间；Boot 3基于Spring Framework 6，同样使用Jakarta命名空间。看到下面的导入时，不能只凭 `jakarta` 判断项目究竟是Boot 3还是Boot 4，还要检查 `pom.xml`：

```java
import jakarta.servlet.Filter;
import jakarta.validation.Valid;
import jakarta.persistence.Entity;
```

Boot 2以及更老的既存项目常看到：

```java
import javax.servlet.Filter;
import javax.validation.Valid;
import javax.persistence.Entity;
```

看到 `javax.*` 不能直接判断代码错误。先从父POM、BOM、依赖树和部署说明确认Spring与Java版本，再查对应版本的官方文档。升级也不是全局替换import，还涉及最低Java版本、Servlet容器、第三方库、配置属性和测试。

## 二、可执行JAR与WAR

| 项目 | 可执行JAR | WAR |
| --- | --- | --- |
| 常见构建配置 | `packaging`省略或为`jar` | `<packaging>war</packaging>` |
| 常见启动 | `java -jar app.jar` | 把`app.war`部署到外部Servlet容器 |
| Tomcat | 常见为应用内嵌 | 常见由外部Tomcat提供 |
| 主要调查点 | main类、端口、启动参数 | 容器版本、context path、部署目录、JNDI、ServletInitializer |

WAR并不自动表示“非Boot”或“代码落后”。Boot应用也可以打成WAR；某些WAR还保留可执行能力。判断方式应来自构建配置和实际启动手册，而不是只看扩展名。

## 三、SpringBootServletInitializer是什么

外部Servlet容器部署的Boot应用可能包含：

```java
package com.example.employee;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(
            SpringApplicationBuilder application) {
        return application.sources(EmployeeApplication.class);
    }
}
```

`@Override` 是Java标准注解，写在重写的方法上，由Java编译器处理。它要求 `configure` 确实覆盖父类或接口中的方法；方法名、参数或返回类型写错时可在编译期发现。它不是Spring注解，也不会注册Bean。

`SpringBootServletInitializer` 是Boot为传统Servlet容器部署提供的入口适配。`configure` 指出应用配置源。看到它时继续检查WAR packaging、Tomcat依赖scope、容器版本和部署文档；它不意味着要深入Servlet容器生命周期源码。

## 四、传统Spring XML从哪里读

旧项目可能使用以下文件，真实名称可以不同：

| 常见文件 | 常见职责 | 阅读入口 |
| --- | --- | --- |
| `WEB-INF/web.xml` | 注册Servlet、Filter、Listener和context参数 | 找`DispatcherServlet`、filter mapping和配置文件位置 |
| `applicationContext.xml` | 声明Service、数据源、事务等根Context Bean | 看bean、component-scan、import和事务配置 |
| `dispatcher-servlet.xml` | Spring MVC Controller、Handler、视图解析等 | 看MVC扫描范围、映射和视图配置 |

这些只是常见命名。应从 `web.xml` 的初始化参数、XML的 `import`、Java启动代码和日志追踪真实加载关系，不能因为没有名为 `applicationContext.xml` 的文件就断言没有Spring Context。

### 传统WAR请求入口的最小识读样本

以下是**传统Servlet 4 / Spring MVC 5风格的片段**，供阅读旧项目，不可复制到Boot 4主线。示例目录：

```text
src/main/webapp/WEB-INF/web.xml
src/main/webapp/WEB-INF/applicationContext.xml
src/main/webapp/WEB-INF/dispatcher-servlet.xml
src/main/java/com/example/legacy/web/EmployeeController.java
src/main/java/com/example/legacy/service/EmployeeService.java
```

`web.xml` 指定根Context和MVC Servlet。这里用 `javax.servlet` 时代的web-app命名空间；实际项目应核对其Servlet规范与Tomcat版本。

```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="4.0">
  <context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>/WEB-INF/applicationContext.xml</param-value>
  </context-param>
  <listener><listener-class>org.springframework.web.context.ContextLoaderListener</listener-class></listener>
  <servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
      <param-name>contextConfigLocation</param-name>
      <param-value>/WEB-INF/dispatcher-servlet.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
  </servlet>
  <servlet-mapping><servlet-name>dispatcher</servlet-name><url-pattern>/</url-pattern></servlet-mapping>
</web-app>
```

根Context扫描Service；MVC子Context扫描Controller。两个XML片段分别保存到上面指明的文件：

```xml
<!-- applicationContext.xml：示意；xmlns/xsi/schemaLocation按既存项目版本补齐 -->
<context:component-scan base-package="com.example.legacy.service" />
```

```xml
<!-- dispatcher-servlet.xml：示意；xmlns/xsi/schemaLocation按既存项目版本补齐 -->
<context:component-scan base-package="com.example.legacy.web" />
<mvc:annotation-driven />
```

这两个XML是**识读片段而非完整可运行配置**，因此不能直接当成部署文件。请求 `/employees/1001` 进入外部Tomcat → DispatcherServlet → MVC映射的EmployeeController → 注入的EmployeeService；若Controller未注册，先查Servlet映射和MVC扫描包，若Service未注入，再查根Context扫描与可见性。

## 五、Java Config与XML Config怎样对应

Java Config：

```java
@Configuration
public class AppConfig {

    @Bean
    public EmployeePolicy employeePolicy() {
        return new EmployeePolicy();
    }
}
```

| 注解 | 所属与位置 | 处理时机和本例效果 |
| --- | --- | --- |
| `@Configuration` | Spring；配置类 | Spring容器加载配置时把该类作为Bean定义来源 |
| `@Bean` | Spring；配置类的方法 | 容器创建配置时调用方法，把返回的 `EmployeePolicy` 注册为Bean；默认Bean名是 `employeePolicy` |

XML中的相近含义：

```xml
<bean id="employeePolicy"
      class="com.example.employee.service.EmployeePolicy" />
```

两者都是向Spring容器声明Bean的方式。既存项目可能同时使用组件扫描、Java Config和XML；阅读时记录Bean名称、类型、依赖、加载顺序和Profile，不要为了“统一风格”擅自迁移配置。

## 六、旧项目调查顺序

1. 从README、POM/Gradle和部署手册确认Java、Spring、packaging和服务器。
2. 判断是main启动、外部Tomcat还是其他容器入口。
3. 从web.xml、ServletInitializer、Java Config和XML追踪Context来源。
4. 沿一个URL找到DispatcherServlet、Controller、Service和数据访问。
5. 查外部配置、JNDI、日志、Security、事务和测试。
6. 把不能确认的行为写成確認事項，不用新项目经验替旧项目补答案。

练习：给出一个使用Boot 2、WAR和外部Tomcat的仓库，只做只读调查。提交版本证据、启动入口、XML/Java Config加载图、一条业务调用链和迁移风险；不修改主线版本，也不把 `javax.*` 机械替换为 `jakarta.*`。

给定目录 `pom.xml → src/main/webapp/WEB-INF/web.xml、dispatcher-servlet.xml、applicationContext.xml → src/main/java/com/example/legacy/web/EmployeeController.java → service/EmployeeService.java → resources/jdbc.properties`，写出五项证据：入口由哪条Servlet映射接收、根Context从哪里加载、Controller由哪个扫描声明注册、数据库连接值来自何处、WAR部署到哪个容器及其版本。目录名只是线索；`jdbc.properties` 是否真正被引用必须查看XML中的占位符配置。迁移前核对Boot/Spring/JDK、Servlet与Jakarta依赖、第三方库、容器和回归测试，不进行全局文字替换。

### 练习参考答案（按给定目录作调查，不是实测项目结论）

```text
pom.xml                         packaging=war与依赖版本的首要线索
src/main/webapp/WEB-INF/web.xml Servlet映射、ContextLoaderListener与contextConfigLocation
src/main/webapp/WEB-INF/dispatcher-servlet.xml  MVC子Context及Controller扫描候选
src/main/webapp/WEB-INF/applicationContext.xml   Root Context及Service/数据源候选
src/main/resources/jdbc.properties              候选配置值，不证明运行时使用
src/main/java/com/example/legacy/web/EmployeeController.java  URL映射
src/main/java/com/example/legacy/service/EmployeeService.java  业务调用
```

先从 `web.xml` 的 `servlet-name`、`servlet-class=DispatcherServlet` 和 `url-pattern` 判定入口；再查 `context-param` 和 `ContextLoaderListener` 确定Root Context的XML，查DispatcherServlet的 `init-param` 或命名默认规则确定MVC子Context的XML。Root通常放Service与数据源，子Context通常放Controller，但**实际Bean来源必须从XML的`component-scan`/`bean`及Java Config逐项确认**，不能仅凭目录推断。数据源继续沿占位符/JNDI/环境变量找到最终配置来源，不复制凭据。WAR部署需同时核对POM的Servlet API作用域、部署手册和目标容器实际版本/日志；只有目录不能回答“究竟部署在哪台Tomcat”。无法取得这些证据的格子填“待确认”。这是传统项目识读题，不应把Spring MVC 5/Servlet 4的`javax.*`配置直接移入Boot 4/Jakarta主线。
