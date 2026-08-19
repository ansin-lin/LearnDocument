# 第2章 创建Spring Boot项目与第一个接口

> 本章目标：从空目录创建一个Maven版Spring Boot工程，完成IDE导入、首次启动和健康检查接口，并能根据启动日志与HTTP响应判断项目是否正常。

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

| 页面选项 | 填写值 | 说明 |
| --- | --- | --- |
| Project | Maven | 使用 `pom.xml` 管理构建和依赖 |
| Language | Java | 后端代码使用Java |
| Spring Boot | 生成器提供的稳定3.x版本 | 不选择4.x、SNAPSHOT、M或RC版本 |
| Group | `com.example` | 组织标识，也是包名前缀 |
| Artifact | `employee-management-api` | 工程和构建产物的基础名称 |
| Name | `employee-management-api` | 项目显示名称 |
| Description | `Employee management REST API` | 项目说明 |
| Package name | `com.example.employee` | 项目根包，必须手动确认 |
| Packaging | Jar | 使用内置Web服务器运行 |
| Java | 17 | 与项目运行环境一致 |

本项目使用Spring Boot 3。生成器页面可能默认选中更高的大版本，因此创建时要主动检查版本。选择稳定版时，版本名称不应带 `SNAPSHOT`、`M` 或 `RC`。

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

### pom.xml中的关键内容

打开 `pom.xml`，确认存在Spring Web依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

起步依赖会带入当前Web项目所需的一组兼容依赖。不要从网络文章复制一批带独立版本号的Spring JAR覆盖它们。

## 五、先运行生成的空工程

第一次启动使用生成后的原始状态，用来验证JDK、Maven Wrapper和Spring Boot启动链路是否正常。打开PowerShell，进入包含 `pom.xml` 的项目根目录：

```powershell
cd D:\workspace\employee-management-api
.\mvnw.cmd spring-boot:run
```

项目自带Maven Wrapper，因此不必先单独安装Maven。Wrapper第一次运行时会下载指定的Maven版本，需要能够访问Maven仓库。

也可以在IDE中运行 `EmployeeManagementApiApplication` 的 `main` 方法。无论使用哪种方式，成功日志中应看到类似信息：

```text
Tomcat started on port 8080
Started EmployeeManagementApiApplication
```

日志中的耗时和其他细节会因版本与电脑而不同。看到应用启动完成后，使用 `Ctrl+C` 可以停止终端中的服务。

此时访问 `http://localhost:8080/` 可能返回404。这不代表启动失败，只表示项目还没有定义根路径接口。

## 六、检查生成的启动类

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

`SpringApplication.run()` 创建并启动Spring应用。`@SpringBootApplication` 同时启用配置类、自动配置和组件扫描。扫描默认从启动类所在包向下进行，所以Controller要放在 `com.example.employee.controller` 等子包中，不要放到无关的平级包。

## 七、把配置文件改为YAML

生成器默认创建：

```text
src/main/resources/application.properties
```

本项目使用YAML管理配置。把该文件重命名为：

```text
src/main/resources/application.yml
```

不要同时保留两份包含重复配置的文件。编辑 `application.yml`：

```yaml
server:
  port: 8080
```

YAML使用空格表示层级，不能使用Tab。`server.port` 指定内置Web服务器监听的本地端口。

## 八、创建第一个接口

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

`@RestController` 让Spring把该类注册为处理HTTP请求的组件，并把方法返回值写入响应体。`@GetMapping("/health")` 表示收到 `GET /health` 时调用 `health()`。

重新启动项目。PowerShell方式仍然是：

```powershell
.\mvnw.cmd spring-boot:run
```

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

1. 新增 `GET /hello`，返回 `Hello Spring Boot`。
2. 把端口改成8081，重新启动并访问两个接口。
3. 故意把 `HealthController` 移到 `com.example.other`，观察404，再移回根包的子包。
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
src/main/java/com/example/employee/controller/HelloController.java
src/main/resources/application.yml
pom.xml
```

当前稳定状态是一个可以快速启动、便于排错的最小Web工程。数据库访问和分层业务代码需要在具备接口基础后再接入。
