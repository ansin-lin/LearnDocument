# Appendix C：日本既存Spring项目的新旧结构识读

日本企业项目中仍可能存在Boot 2、WAR、外部Tomcat或XML配置。本附录只建立识读能力，不要求把第20章的Java 17、Spring Boot 3.5.16可执行JAR主线改成旧结构，也不要求重新开发一套XML项目。

## 一、先确认版本，再判断代码是否正确

Spring Boot 3基于Spring Framework 6，Servlet、Validation和Persistence等API使用Jakarta命名空间：

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

`SpringBootServletInitializer` 是Boot为传统Servlet容器部署提供的入口适配。`configure` 指出应用配置源。看到它时继续检查WAR packaging、Tomcat依赖scope、容器版本和部署文档；它不意味着要深入Servlet容器生命周期源码。

## 四、传统Spring XML从哪里读

旧项目可能使用以下文件，真实名称可以不同：

| 常见文件 | 常见职责 | 阅读入口 |
| --- | --- | --- |
| `WEB-INF/web.xml` | 注册Servlet、Filter、Listener和context参数 | 找`DispatcherServlet`、filter mapping和配置文件位置 |
| `applicationContext.xml` | 声明Service、数据源、事务等根Context Bean | 看bean、component-scan、import和事务配置 |
| `dispatcher-servlet.xml` | Spring MVC Controller、Handler、视图解析等 | 看MVC扫描范围、映射和视图配置 |

这些只是常见命名。应从 `web.xml` 的初始化参数、XML的 `import`、Java启动代码和日志追踪真实加载关系，不能因为没有名为 `applicationContext.xml` 的文件就断言没有Spring Context。

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
