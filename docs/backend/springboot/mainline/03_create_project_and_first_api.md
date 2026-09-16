# 第3章 创建并运行第一个Spring Boot工程

> 本章目标：从空目录创建一个Maven版Spring Boot工程，完成IDE导入、构建、首次启动和健康检查接口，并能根据构建结果、启动日志与HTTP响应判断项目是否正常。

`employee-management-api` 是本套 Spring Boot 主线的基础工程。后端接口、业务代码、数据库访问和测试都会在这个工程内继续扩展；重复生成工程容易造成包名、配置和文件路径不一致。

## 一、开始状态与完成结果

开始前准备：

- 已安装JDK 17。
- 可以访问Spring Initializr。
- 准备好IntelliJ IDEA、Eclipse或其他支持Maven的Java IDE。
- 准备一个保存练习项目的目录，例如 `D:\workspace`。

完成本章后，项目应满足：

- Maven能够完成构建。
- Spring Boot能够在本机8080端口启动。
- 浏览器访问 `GET /health` 返回 `OK`。
- 项目根包为 `com.example.employee`，Controller、Service和Mapper都放在它的子包中。

本章先按步骤生成并导入工程，再依次给出 `pom.xml`、启动类、YAML配置和Controller的完整最终内容。每个完整文件后紧接着解释其中第一次出现的依赖、注解、方法和配置，最后统一进行构建、启动和请求验证。第一次学习时不要跳过空工程启动，它可以把环境问题与代码问题分开。

先分清本章使用的三个工具：JDK负责把Java源码编译为字节码并运行程序；Maven负责读取 `pom.xml`、下载依赖、执行测试和构建；IDE用于编辑、浏览和启动工程，但不能代替JDK和Maven。Spring Initializr则负责生成工程骨架。

先在PowerShell确认当前终端使用的Java版本：

```powershell
java -version
```

主版本必须是17。如果IDE使用另一套JDK，还需要在IDE的项目设置中把项目SDK和Maven运行JDK都改为17。

## 二、使用Spring Initializr生成工程

打开Spring官方项目生成器：

[https://start.spring.io](https://start.spring.io)

Spring Initializr用于生成工程骨架和构建配置，不会替你编写员工管理业务代码。

### 1. 填写项目选项

按下表填写：

| 页面选项 | 本课程填写值 | 页面可接受的值 | 作用 |
| --- | --- | --- | --- |
| Project | Maven | 生成器列出的Maven或Gradle | 选择构建工具；本课程用 `pom.xml` |
| Language | Java | 生成器列出的Java、Kotlin或Groovy | 选择源码语言 |
| Spring Boot | `3.5.16` | 页面仍提供的正式版本 | 固定框架基线；不选择SNAPSHOT、M或RC版本 |
| Group | `com.example` | 合法的Maven组织标识 | 作为项目坐标和包名前缀 |
| Artifact | `employee-management-api` | 合法的Maven产物名称 | 作为工程和构建产物的基础名称 |
| Name | `employee-management-api` | 普通项目名称文本 | 作为项目显示名称 |
| Description | `Employee management REST API` | 普通说明文本，也可留空 | 说明项目用途 |
| Package name | `com.example.employee` | 合法的Java包名 | 作为项目根包，必须手动确认 |
| Packaging | Jar | Jar或War | 使用可执行Jar和内置Web服务器 |
| Java | 17 | 该版本支持的生成器选项 | 与项目编译和运行环境一致 |

本项目固定使用Spring Boot 3.5.16。生成器页面可能默认选择Spring Boot 4，但4.x并不是错误版本；它需要配合另一套框架兼容组合。为了让全课程代码和依赖保持一致，本项目不要只升级Spring Boot大版本。若生成器以后不再提供3.5.16，不要自行换成其他大版本后继续照抄课程代码，应先按项目升级方针统一调整依赖和课程基线。

### 2. 添加依赖

单击 **Add Dependencies**，搜索并添加：

- **Spring Web**

`Spring Web` 是当前 Web API 工程的必要起步依赖。它提供Spring MVC、JSON转换支持和内置Tomcat，可以让项目接收HTTP请求。

当前最小 Web 工程只保留 `Spring Web` 和生成器默认的测试依赖。其他依赖只有在代码实际使用对应功能时再加入：

- MyBatis和MySQL Driver用于数据库访问，当前健康检查接口还不访问数据库。
- Validation用于请求参数校验，当前接口还没有接收业务参数。
- Lombok可以减少getter、setter等样板代码，但当前对象结构很简单，直接写普通Java代码更直观。
- DevTools是本地开发辅助工具，不是项目成功运行的必要条件。

生成器会创建测试目录和Spring Boot测试依赖，它们属于标准工程结构，保留即可。

### 3. 下载并解压

检查所有参数后单击 **Generate**。浏览器会下载：

```text
employee-management-api.zip
```

把压缩包解压到练习目录。解压后的正确状态应是：

```text
D:\workspace\employee-management-api\
├── .mvn\
├── src\
├── mvnw
├── mvnw.cmd
└── pom.xml
```

不要直接在ZIP压缩包内部打开工程，也不要形成重复目录：

```text
employee-management-api\employee-management-api\pom.xml
```

本文中的“项目根目录”，指直接包含 `pom.xml` 和 `mvnw.cmd` 的目录。

## 三、把工程导入IDE

### Eclipse

1. 选择 **File → Import**。
2. 选择 **Maven → Existing Maven Projects**。
3. Root Directory选择解压后的 `employee-management-api` 目录。
4. 勾选检测到的 `pom.xml` 并完成导入。
5. 在项目属性中确认Java Build Path和编译级别使用Java 17。

首次导入需要从Maven仓库下载依赖。IDE仍在下载时出现大量红色错误，不要立即修改代码；先等待下载结束并刷新Maven项目。

### IntelliJ IDEA

1. 选择 **File → Open**，打开直接包含 `pom.xml` 的 `employee-management-api` 目录。
2. IDEA询问是否作为Maven工程加载时选择确认。
3. 在 **Project Structure → Project SDK** 中选择JDK 17。
4. 在Maven设置中确认Runner使用项目JDK 17，然后等待依赖下载完成。

无论使用哪个IDE，都应以 `java -version`、Maven构建结果和启动日志为判断依据。IDE没有红线不能代替实际构建成功。

## 四、认识生成后的工程

工程导入后，主要结构应接近：

```text
employee-management-api/
├── .mvn/wrapper/                         # Maven Wrapper配置
├── src/
│   ├── main/
│   │   ├── java/com/example/employee/
│   │   │   └── EmployeeManagementApiApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/employee/
│           └── EmployeeManagementApiApplicationTests.java
├── mvnw                                   # macOS/Linux启动脚本
├── mvnw.cmd                               # Windows启动脚本
└── pom.xml                                # Maven项目与依赖配置
```

如果启动类不在 `com.example.employee` 中，说明生成项目时的Package name填写不一致。应修正为统一根包，否则组件扫描范围、示例路径和实际文件位置会不一致。

### 完整示例：pom.xml

用下面内容核对项目根目录中的 `pom.xml`。这是本章可构建工程所需的完整文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>employee-management-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>employee-management-api</name>
    <description>Employee management REST API</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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

### 第一次出现：Maven依赖和Starter

`pom.xml` 是Maven的项目说明文件。Maven读取它以后，知道项目名称、Java版本、需要下载哪些依赖，以及可以使用哪些构建插件。

- `parent` 指定Spring Boot父项目。这里的 `3.5.16` 统一管理Spring相关依赖的兼容版本。
- `relativePath` 为空，表示不要先到本项目的上级目录寻找父项目，而是按坐标解析Spring Boot父项目。
- `groupId` 通常表示组织或项目组，`artifactId` 表示具体构建产物；两者和 `version` 一起形成项目坐标。
- `properties` 中的 `java.version` 要求项目使用Java 17编译。
- `dependencies` 保存项目直接使用的依赖。
- `spring-boot-starter-web` 是Web起步依赖，会带入Spring MVC、JSON转换和内置Tomcat等相互兼容的组件。
- `spring-boot-starter-test` 提供测试所需组件；`scope` 为 `test` 表示只在编译和运行测试时使用，不作为应用正式运行代码的依赖。
- `spring-boot-maven-plugin` 为Maven增加Spring Boot构建和运行能力。本章稍后使用的 `spring-boot:run` 就由它支持。

Starter是经过组合的依赖入口，不是一个替你生成接口代码的工具。不要从网络文章复制一批带独立版本号的Spring JAR覆盖父项目管理的版本。

### 完整示例：生成的最小测试

Spring Initializr还会生成下面的测试文件。先保留原样：

```java
package com.example.employee;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmployeeManagementApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

`@SpringBootTest` 来自Spring Boot测试支持，表示执行测试时加载完整的Spring应用环境；`@Test` 来自JUnit Jupiter，表示 `contextLoads()` 是一个测试方法。该方法没有参数、没有返回值，方法体为空：只要应用环境能成功加载，测试就通过；加载失败则测试失败。系统化测试会在后续测试章节展开，本章只用这个生成的最小测试验证工程基础配置。

## 五、先构建并运行生成的空工程

第一次构建和启动使用生成后的原始状态，用来验证JDK、Maven Wrapper和Spring Boot启动链路是否正常。打开PowerShell，进入包含 `pom.xml` 的项目根目录：

```powershell
cd D:\workspace\employee-management-api
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

`cd` 把当前工作目录切换到项目根目录；路径应改成自己实际的解压位置。`.\mvnw.cmd` 运行当前目录中的Windows Maven Wrapper脚本，因此不必先单独安装Maven。Wrapper第一次运行时会下载工程指定的Maven版本，随后Maven还会下载 `pom.xml` 中声明的依赖，需要能够访问对应仓库。

`clean test` 包含两个Maven目标：`clean` 删除上一次构建产生的 `target` 目录，`test` 重新编译并执行测试。命令成功时末尾应出现 `BUILD SUCCESS`；失败时先处理错误，不继续启动。

`spring-boot:run` 中，`spring-boot` 是插件前缀，`run` 是运行目标。它启动当前Spring Boot应用，并保持进程运行以等待HTTP请求，不会在显示启动成功后自动结束。

也可以在IDE中运行 `EmployeeManagementApiApplication` 的 `main` 方法。无论使用哪种方式，成功日志中应看到类似信息：

```text
Tomcat started on port 8080
Started EmployeeManagementApiApplication
```

日志中的耗时和其他细节会因版本与电脑而不同。看到应用启动完成后，使用 `Ctrl+C` 可以停止终端中的服务。

此时访问 `http://localhost:8080/` 可能返回404。这不代表启动失败，只表示项目还没有定义根路径接口。

## 六、完整示例：检查生成的启动类

文件位置：

```text
src/main/java/com/example/employee/EmployeeManagementApiApplication.java
```

生成内容应接近：

```java
package com.example.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmployeeManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeManagementApiApplication.class, args);
    }
}
```

### 第一次出现：SpringApplication和@SpringBootApplication

`SpringApplication` 是由 `org.springframework.boot` 包提供的类。静态方法 `run(EmployeeManagementApiApplication.class, args)` 的第一个参数必须是主要配置类的 `Class` 对象，第二个参数是 `main` 收到的命令行参数数组；它创建并启动Spring应用，返回Spring应用上下文对象，本章的 `main` 方法不需要保存该返回值。应用上下文可以先理解为Spring保存配置和所管理对象的运行环境，完整对象管理过程在下一章说明。

`@SpringBootApplication` 来自 `org.springframework.boot.autoconfigure`，写在启动类上。它是一个**复合注解**：一个注解内部组合了其他注解，使用它就不必在启动类上重复写多个注解。

它主要由下面三个注解组成：

| 组成注解 | 各自功能 |
| --- | --- |
| `@SpringBootConfiguration` | 标记当前类是Spring Boot应用的主要配置类。它本身基于Spring的 `@Configuration`，因此这个类可以作为Bean定义和其他配置的来源。一个应用通常只设置一个主要配置类。 |
| `@EnableAutoConfiguration` | 开启Spring Boot自动配置。Spring Boot会根据项目中已有的依赖、配置项和Bean判断需要配置什么。例如存在Spring Web依赖时，它会准备Web应用运行所需的基础组件；如果开发者已经提供自己的配置，符合条件的自动配置会主动让开。 |
| `@ComponentScan` | 开启组件扫描。Spring会从启动类所在包开始，扫描当前包及其子包中带有 `@RestController`、`@Service`、`@Component` 等组件注解的类，并把符合条件的对象注册到Spring容器。 |

`@SpringBootConfiguration` 和 `@EnableAutoConfiguration` 由Spring Boot提供，`@ComponentScan` 由Spring Framework提供。它们作为 `@SpringBootApplication` 内部组合的元注解，在应用启动时由Spring读取并执行相应配置。这里的 `@Service`、`@Component` 与 `@RestController` 都是放在类上的职责标记；本章只创建Controller，其他组件的创建和连接在下一章完成。

三个注解组合后，`@SpringBootApplication` 同时告诉Spring Boot：**以当前类作为主要配置入口、启用按条件生效的自动配置、查找并注册项目中的组件**。随后执行 `SpringApplication.run(...)`，Spring Boot会据此创建Spring容器、完成配置和组件注册，并启动应用。

组件扫描默认从启动类所在包向下进行，所以本项目把启动类放在根包 `com.example.employee`，把Controller放在 `com.example.employee.controller` 等子包中。若把Controller放到无关的平级包，默认扫描不到它，对应接口也不会生效。

## 七、完整示例：把配置文件改为YAML

生成器默认创建：

```text
src/main/resources/application.properties
```

本项目使用YAML管理配置。把该文件重命名为：

```text
src/main/resources/application.yml
```

重命名不会自动转换文件中的语法。如果原文件已有配置，需要把原来的 `key=value` 写法改成YAML格式，不能只修改扩展名。当前工程先用下面内容完整替换 `application.yml`，不要同时保留两份包含重复配置的文件：

```yaml
server:
  port: 8080
```

### 第一次出现：YAML、properties和server.port

配置文件用于保存端口等运行设置，使修改这些设置时不必改动Java业务代码。Spring Boot支持properties和YAML两种配置格式；这里的 `application.properties` 与 `application.yml` 用途相同，主要区别在于内容的组织和书写方式。`.yml` 和 `.yaml` 都是YAML文件的扩展名，Spring Boot都支持，本项目统一使用 `.yml`。

上面的YAML把 `port` 放在 `server` 下面，表示配置项 `server.port` 的值是 `8080`。它指定内置Web服务器监听的端口；本项目默认也是8080，显式写出可以方便查看和修改。

同一个配置用properties格式书写如下。此处仅作对照，不要再新建一份文件：

```properties
# Web服务器端口
server.port=8080
```

其中 `server.port` 是完整配置键，`=` 分隔键和值，`8080` 是配置值。两种写法在本例中的生效结果相同，都会使用8080端口，并不需要修改Controller。

| 对比点 | properties | YAML |
| --- | --- | --- |
| 常用文件名 | `application.properties` | `application.yml` 或 `application.yaml` |
| 基本写法 | 通常一行一个 `key=value` | 使用 `key: value`，冒号后有值时要留空格 |
| 层级表达 | 在完整键中用点连接，如 `server.port` | 用缩进表达父子关系，如 `server` 下的 `port` |
| 阅读特点 | 单个配置键完整，便于逐项搜索 | 同组配置可以集中在一个父节点下，层级更直观 |

### YAML书写注意事项

- 缩进使用空格，不能用Tab。本项目每层使用两个空格，同级配置必须对齐。
- 本例 `server:` 后换行，表示下面还有子配置；`port: 8080` 的冒号后留一个空格。
- `#` 可以开始注释。YAML中的字符串若包含可能被解释为语法的内容，例如冒号后接空格，可以加引号，如 `"提示: 请重试"`。这只是语法示意，不需要添加到本项目配置中。
- 同一个父节点下不要重复定义同名键。增加其他 `server` 配置时，应放在已有的 `server:` 下，不要另写一段同名父节点。

两种格式都能用于实际项目，properties并不是已废弃格式。维护既有项目时遵循项目约定即可。同一位置、相同配置文件基本名下，两种格式同时存在时，重复配置以properties为准，例如 `application.properties` 中的端口会覆盖同目录 `application.yml` 中的端口；这不代表properties能覆盖所有其他配置来源。因此，本项目只保留一种格式，避免“改了配置却没生效”。参见 [Spring Boot 3.5外部化配置说明](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html)。

### 验证配置确实生效

1. 临时把 `port: 8080` 改为 `port: 8081`，保存文件。
2. 停止原进程，按第五节的方式重新启动，检查日志是否显示 `Tomcat started on port 8081`。普通配置修改不会自动改变已运行进程的端口。
3. 验证后改回 `port: 8080`，再次重启，确认日志恢复8080，再继续下一节。

如果启动报YAML解析错误，先检查行号、缩进和冒号后的空格；如果端口仍是原值，再检查是否同时保留了 `application.properties`，或运行的是另一个未停止的进程。

## 八、完整示例：创建第一个接口

在 `com.example.employee` 下新建 `controller` 包，再创建文件：

```text
src/main/java/com/example/employee/controller/HealthController.java
```

完整代码：

```java
package com.example.employee.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
```

### 第一次出现：@RestController和@GetMapping

`@RestController` 的完整名称是 `org.springframework.web.bind.annotation.RestController`，由Spring Web提供，写在类上。应用启动扫描到它后，会创建并管理 `HealthController` 对象；请求处理方法的返回值会写入HTTP响应体，而不是被当作后端页面名称。它组合了Web控制器标记和响应正文行为，本章先掌握当前效果。

`@GetMapping` 的完整名称是 `org.springframework.web.bind.annotation.GetMapping`，也由Spring Web提供，写在方法上。当前参数 `"/health"` 是必须匹配的请求路径；应用启动时Spring记录这个映射，运行期间收到 `GET /health` 才调用 `health()`。其他HTTP方法或其他路径不会匹配这个方法。

`health()` 是项目自己定义的方法，不是框架库中的方法。它不接收参数，返回类型是Java的 `String`；执行 `return "OK"` 后，Spring把字符串 `OK` 写入响应体。因为方法正常完成且没有另设状态，所以本例响应状态是200。

重新启动项目。PowerShell方式仍然是：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

先再次看到 `BUILD SUCCESS`，再确认应用能够启动。这样可以证明加入Controller和YAML后，完整工程仍能通过构建和最小测试。

## 九、验证HTTP响应

### 浏览器验证

访问：

```text
http://localhost:8080/health
```

页面应显示：

```text
OK
```

### PowerShell验证

保持启动服务的终端窗口不要关闭，另外打开一个PowerShell窗口执行：

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/health"
$response.StatusCode
$response.Content
```

`Invoke-WebRequest` 是PowerShell发送Web请求的命令。`-Uri` 是必填的目标地址，本例接受完整的HTTP URL；未写 `-Method` 时默认发送GET。命令把响应对象保存到变量 `$response`，`StatusCode` 属性是状态码，`Content` 属性是响应正文。

预期可观察结果：

```text
200
OK
```

浏览器只显示响应内容；PowerShell还可以确认HTTP状态码为200。完成验证后回到启动服务的终端按 `Ctrl+C` 停止程序。

## 十、常见失败与定位

| 现象 | 定位方法 | 原因 | 处理 |
| --- | --- | --- | --- |
| `java`命令找不到 | 执行 `java -version` | JDK未安装或PATH错误 | 安装JDK 17并修正环境变量 |
| `release version 17 not supported` | 检查IDE和Maven使用的JDK | 构建实际使用了旧JDK | 把项目SDK和Maven JDK都改为17 |
| Wrapper下载失败 | 查看命令中的下载地址和网络错误 | 无法访问Maven分发或依赖仓库 | 检查代理、网络和企业仓库配置后重试 |
| 8080端口被占用 | 启动日志出现 `Port 8080 was already in use` | 其他程序正在监听8080 | 停止占用程序，或临时修改 `server.port` |
| `/health`返回404 | 确认应用已启动并检查包路径 | URL错误或Controller不在扫描范围 | 使用 `/health`，并把Controller放在根包子包中 |
| YAML启动报错 | 查看错误中的行号 | 缩进错误或使用了Tab | 使用空格重新对齐层级 |
| IDE大量导入错误 | 查看Maven是否仍在下载 | 依赖尚未同步或导入方式错误 | 等待下载并刷新Maven项目 |

排错时先区分三个阶段：Maven能否构建、应用能否启动、HTTP接口能否访问。不要看到404就重新安装JDK，也不要在应用尚未启动时反复修改Controller。

## 十一、操作练习

初始状态：`GET /health` 已返回状态码200和正文 `OK`。

任务：

1. 模仿 `health()`，在 `HealthController` 中新增无参数的 `hello()` 方法；使用 `@GetMapping("/hello")`，返回 `Hello Spring Boot`。
2. 把端口改成8081，重新启动并访问两个接口。
3. 故意把 `HealthController.java` 移到与 `com.example.employee` 平级的 `com.example.other` 目录，并把文件首行改为 `package com.example.other;`；重启后观察404，再把目录和包声明一同恢复。
4. 保存一次成功启动日志和两条接口的状态码、响应正文，作为本章自测证据。

验收标准：

- `GET http://localhost:8081/health` 返回200和 `OK`。
- `GET http://localhost:8081/hello` 返回200和 `Hello Spring Boot`。
- 能说明 `pom.xml`、启动类、配置文件、Controller和Maven Wrapper分别负责什么。
- 能根据“构建失败、启动失败、接口404”判断问题发生在哪个阶段。

## 十二、当前稳定状态

完成练习并记录结果后，把端口恢复为8080。当前工程至少保留以下文件，作为继续增加请求参数和JSON接口的基础：

```text
src/main/java/com/example/employee/EmployeeManagementApiApplication.java
src/main/java/com/example/employee/controller/HealthController.java
src/main/resources/application.yml
pom.xml
```

`hello()` 是本章练习成果，可以保留，但不是后续主线依赖。当前必需稳定状态是一个可以快速启动、便于排错的最小Web工程。下一章将在这个工程中创建并连接Controller与Service；本章不要提前创建Mapper或数据库表。
