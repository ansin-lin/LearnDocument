# A09 使用@Controller返回HTML页面

Employee主线使用`@RestController`返回JSON。本附录使用独立实验说明另一种常见项目形态：Spring MVC选择HTML模板，把Java数据放入页面，再把渲染后的HTML返回浏览器。

本附录只讲服务器端页面渲染，不接入数据库、不处理表单提交，也不改变Employee API主线。

## 一、完成结果与处理流程

开始前应完成第6章，能够运行`employee-management-api`，并理解`@GetMapping`和路径参数。

完成后将得到两个页面：

| 请求 | 页面内容 | 是否使用Model数据 |
| --- | --- | --- |
| `GET /pages/welcome` | 固定欢迎页面 | 否 |
| `GET /pages/employees/1001` | 员工编号、姓名、部门和邮箱 | 是 |

```text
浏览器发送GET请求
  → Spring MVC找到@Controller方法
  → Controller返回视图名称
  → 视图解析器找到templates中的HTML模板
  → Thymeleaf把Model数据填入模板
  → 生成HTML并返回浏览器
```

## 二、@Controller、@ResponseBody和@RestController

| 写法 | `String`返回值的默认含义 | 适合场景 |
| --- | --- | --- |
| `@Controller` | 视图名称 | 返回HTML模板 |
| `@Controller`方法加`@ResponseBody` | HTTP响应正文 | 页面Controller中少量直接返回内容的方法 |
| `@RestController` | HTTP响应正文 | 主要返回JSON或文本的API |

`@Controller`的完整名称是`org.springframework.stereotype.Controller`，写在类上。它说明这个类承担Spring MVC请求入口职责，也让组件扫描把该类注册为Spring Bean。

`@ResponseBody`的完整名称是`org.springframework.web.bind.annotation.ResponseBody`，可以写在Controller类或方法上。它告诉Spring MVC不要把返回字符串解释成视图名称，而是通过消息转换器把返回值写入HTTP响应正文。

`@RestController`同时包含`@Controller`和`@ResponseBody`的语义。因此下面两种写法的响应正文行为相近：

```java
@RestController
public class HealthController {
}
```

```java
@Controller
@ResponseBody
public class HealthController {
}
```

本实验使用单独的`@Controller`。如果误改成`@RestController`，方法返回的`"welcome"`会直接成为响应正文，Thymeleaf模板不会被渲染。

## 三、加入Thymeleaf模板引擎

`spring-boot-starter-web`提供Spring MVC，但不会单独决定使用哪一种服务器端模板技术。本实验选择Thymeleaf。

在`pom.xml`的`<dependencies>`中追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

该Starter由Spring Boot管理兼容版本，本课程不单独填写`<version>`。加入依赖后，Spring Boot会准备Thymeleaf视图解析支持，并默认从`src/main/resources/templates/`查找模板。

执行`.\mvnw.cmd clean test`确认依赖能够解析。若失败，先检查`dependency`是否位于`dependencies`内部，再让IDE重新加载Maven工程。

## 四、完整示例

创建三个文件：

```text
src/main/
├── java/com/example/employee/pagelab/
│   ├── EmployeePageController.java
│   └── EmployeePageView.java
└── resources/templates/
    ├── welcome.html
    └── employee-detail.html
```

`pagelab`表示独立页面实验，不修改现有员工Controller和Service。

### 1. EmployeePageView.java

新建`src/main/java/com/example/employee/pagelab/EmployeePageView.java`：

```java
package com.example.employee.pagelab;

public class EmployeePageView {

    private final Long id;
    private final String name;
    private final String department;
    private final String email;

    public EmployeePageView(
            Long id,
            String name,
            String department,
            String email) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmail() {
        return email;
    }
}
```

这个类只携带页面需要显示的数据。它不是数据库Entity，也不是Spring Bean。Thymeleaf通过getter读取四个属性。

### 2. EmployeePageController.java

新建`src/main/java/com/example/employee/pagelab/EmployeePageController.java`：

```java
package com.example.employee.pagelab;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class EmployeePageController {

    @GetMapping("/pages/welcome")
    public String welcome() {
        return "welcome";
    }

    @GetMapping("/pages/employees/{id}")
    public String employeeDetail(
            @PathVariable Long id,
            Model model) {
        EmployeePageView employee = new EmployeePageView(
                id,
                "Tanaka",
                "Sales",
                "tanaka@example.com");

        model.addAttribute("employee", employee);
        return "employee-detail";
    }
}
```

首次出现的内容：

- `@Controller`使返回字符串可以作为视图名称处理。
- `welcome()`返回`"welcome"`，视图解析器寻找`templates/welcome.html`。
- `employeeDetail()`接收路径中的`id`和Spring提供的`Model`。
- `model.addAttribute("employee", employee)`以名称`employee`保存页面数据。
- 返回`"employee-detail"`后，视图解析器寻找`templates/employee-detail.html`。

`Model`的完整名称是`org.springframework.ui.Model`，是Spring MVC提供的接口。Controller不需要自己`new Model()`；Spring MVC调用方法时提供当前请求使用的Model对象。

`addAttribute(String attributeName, Object attributeValue)`的第一个参数是模板读取数据时使用的名称，第二个参数是Java对象。它返回Model本身，便于连续添加属性，本例不保存返回值。

### 3. welcome.html

新建`src/main/resources/templates/welcome.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>员工页面实验</title>
</head>
<body>
    <main>
        <h1>员工页面实验</h1>
        <p>这个页面没有使用Model数据。</p>
        <p><a href="/pages/employees/1001">查看示例员工</a></p>
    </main>
</body>
</html>
```

这个模板没有动态数据。如果页面完全静态，不需要Controller、权限判断或Model，也可以放在`src/main/resources/static/`中直接作为静态资源提供。

### 4. employee-detail.html

新建`src/main/resources/templates/employee-detail.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>员工详情</title>
</head>
<body>
    <main>
        <h1>员工详情</h1>
        <dl>
            <dt>员工编号</dt>
            <dd th:text="${employee.id}">1001</dd>
            <dt>姓名</dt>
            <dd th:text="${employee.name}">示例姓名</dd>
            <dt>部门</dt>
            <dd th:text="${employee.department}">示例部门</dd>
            <dt>邮箱</dt>
            <dd th:text="${employee.email}">sample@example.com</dd>
        </dl>
        <p><a href="/pages/welcome">返回欢迎页面</a></p>
    </main>
</body>
</html>
```

`xmlns:th="http://www.thymeleaf.org"`声明模板中使用`th:*`属性。浏览器不会从这个地址下载脚本，它只是命名空间标识。

`th:text="${employee.name}"`会从Model取得`employee`，调用`getName()`，再用结果替换元素文本。标签中的`示例姓名`只是未渲染时的占位内容。

`th:text`会按HTML规则转义文本。不要为了显示用户输入而改用`th:utext`，否则未经处理的HTML可能形成跨站脚本风险。

## 五、运行和验证

在项目根目录执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

使用浏览器访问：

```text
http://localhost:8080/pages/welcome
http://localhost:8080/pages/employees/1001
```

应确认：

1. 欢迎页面显示固定标题、说明和链接。
2. 员工详情页面显示`1001`、`Tanaka`、`Sales`和`tanaka@example.com`。
3. 浏览器Network面板中的响应`Content-Type`包含`text/html`。
4. `GET /health`仍返回`OK`，原员工JSON接口不受影响。

PowerShell也可以检查响应：

```powershell
$page = Invoke-WebRequest `
    -Uri "http://localhost:8080/pages/employees/1001" `
    -Method Get

$page.StatusCode
$page.Headers["Content-Type"]
$page.Content
```

这里每行结尾的反引号是PowerShell续行符。预期状态码为200，响应类型包含`text/html`，正文包含员工数据。

## 六、理解Model与页面渲染

```text
GET /pages/employees/1001
  → @PathVariable把1001转换成Long
  → Spring MVC提供Model
  → Controller把EmployeePageView放入Model
  → Controller返回employee-detail视图名称
  → Thymeleaf读取employee-detail.html
  → 模板表达式读取Java对象
  → 返回渲染后的HTML
```

Model只负责在Controller和视图之间传递本次渲染需要的数据，不是数据库，也不应该保存在Controller字段中。真实项目通常由Service取得员工数据；本实验使用固定数据，只为集中观察页面渲染。

## 七、常见失败与Review

| 现象 | 常见原因 | 检查位置 |
| --- | --- | --- |
| 页面正文只有`welcome` | Controller误用了`@RestController`或`@ResponseBody` | Controller类和方法注解 |
| 返回500并提示找不到模板 | 视图名称、文件名或`templates`目录不一致 | return字符串和模板路径 |
| 页面显示占位文本或空值 | `addAttribute`名称与模板表达式不一致 | `employee`名称 |
| 模板无法读取属性 | getter缺失或属性名拼写错误 | `EmployeePageView` |
| `th:text`原样出现在浏览器中 | Thymeleaf依赖未生效，或模板被放进`static` | `pom.xml`和资源目录 |
| 页面路径与API冲突 | 两个Controller声明相同方法和路径 | 全项目映射注解 |

Review时还要确认Model只包含页面需要的数据、Controller没有直接访问数据库、用户数据通过转义输出，并回归原有JSON接口。

## 八、操作练习

为`EmployeePageView`增加`status`属性，在Controller中传入`"在职"`，并在详情模板中增加对应的`dt`和`dd`。分别访问编号1001和2001，确认路径编号随URL变化。

保存页面截图或HTML响应，然后确认`/health`和员工JSON接口仍正常。练习完成后恢复原实验状态。

## 九、清理实验

本附录不会成为Employee API主线的一部分。完成实验后：

1. 删除`pagelab`中的两个Java文件；
2. 删除`templates`中的两个HTML文件；
3. 从`pom.xml`删除`spring-boot-starter-thymeleaf`；
4. 执行`.\mvnw.cmd clean test`；
5. 确认`/health`和第6章三个接口仍正常。

如果实际项目本来采用服务器端页面，应以项目构建文件和画面规格为准，不要机械删除依赖。官方示例参见[Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content)，返回值和Model规则参见[Spring MVC Controller返回类型](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html)。
