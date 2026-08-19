# 第13章 测试基础

> 本章目标：掌握 Spring Boot 中 Service 测试和 Controller 接口测试的基本思路。

## 一、为什么要写测试

测试用于确认代码修改后功能仍然正确。

企业项目中常见测试目标：

- Service 业务逻辑
- Mapper SQL 结果
- Controller 请求响应
- 异常和边界值

## 二、测试依赖

Spring Boot 项目通常包含：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId> <!-- Spring Boot 官方依赖组 -->
    <artifactId>spring-boot-starter-test</artifactId> <!-- 测试依赖 -->
    <scope>test</scope> <!-- 只在测试时使用 -->
</dependency>
```

`spring-boot-starter-test` 通常包含 JUnit 5、Spring Test、Mockito、AssertJ 等测试工具。

## 三、测试文件放在哪里

测试代码放在：

```text
src/test/java
```

正式代码放在：

```text
src/main/java
```

示例：

```text
src/main/java/com/example/employee/service/EmployeeService.java
src/test/java/com/example/employee/service/EmployeeServiceTest.java
```

测试类名通常是：

```text
被测试类名 + Test
```

## 四、Service 测试关注点

Service 测试重点：

- 输入是否被正确处理
- 员工不存在时是否抛出业务异常
- 新增、修改、删除是否调用正确 Mapper
- 事务边界是否清晰

Service 测试示例：

```java
package com.example.employee.service; // 测试类所在包

import com.example.employee.entity.Employee; // 员工实体
import com.example.employee.exception.BusinessException; // 业务异常
import com.example.employee.mapper.EmployeeMapper; // Mapper 接口
import org.junit.jupiter.api.Test; // JUnit 测试注解
import org.mockito.Mockito; // Mockito 工具

import static org.junit.jupiter.api.Assertions.assertThrows; // 异常断言
import static org.mockito.Mockito.when; // 模拟方法返回值

class EmployeeServiceTest { // EmployeeService 测试类

    @Test // 表示这是一个测试方法
    void findById_shouldThrowException_whenEmployeeNotFound() { // 员工不存在时应该抛出异常
        EmployeeMapper employeeMapper = Mockito.mock(EmployeeMapper.class); // 创建 Mapper 模拟对象
        EmployeeService employeeService = new EmployeeService(employeeMapper); // 创建被测试的 Service

        when(employeeMapper.selectById(999L)).thenReturn(null); // 模拟数据库查不到员工

        assertThrows(BusinessException.class, () -> employeeService.findById(999L)); // 断言会抛出业务异常
    }
}
```

这个测试不连接真实数据库。

`Mockito.mock()` 创建的是模拟对象，用于只测试 Service 逻辑。

## 五、Controller 测试关注点

Controller 测试重点：

- URL 是否正确
- 请求方法是否正确
- JSON 请求体是否能绑定
- 响应状态码是否正确
- 响应 JSON 字段是否正确

Controller 测试示例：

```java
package com.example.employee.controller; // 测试类所在包

import org.junit.jupiter.api.Test; // JUnit 测试注解
import org.springframework.beans.factory.annotation.Autowired; // 自动注入
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // 自动配置 MockMvc
import org.springframework.boot.test.context.SpringBootTest; // 启动 Spring Boot 测试环境
import org.springframework.test.web.servlet.MockMvc; // 模拟 HTTP 请求

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // 构造 GET 请求
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // 校验 HTTP 状态码

@SpringBootTest // 启动完整 Spring Boot 上下文
@AutoConfigureMockMvc // 自动配置 MockMvc
class EmployeeControllerTest { // 员工接口测试

    @Autowired // 从 Spring 容器注入 MockMvc
    private MockMvc mockMvc; // 用于模拟 HTTP 请求

    @Test // 测试方法
    void health_shouldReturnOk() throws Exception { // 健康检查应该返回 200
        mockMvc.perform(get("/health")) // 发送 GET /health 请求
                .andExpect(status().isOk()); // 断言 HTTP 状态码是 200
    }
}
```

## 六、运行测试

在项目根目录执行：

```bash
mvn test
```

成功时可以看到类似结果：

```text
BUILD SUCCESS
```

如果测试失败，需要先看失败的测试方法名，再看断言错误或异常堆栈。

## 七、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 测试类找不到 Bean | 测试包路径不在启动类扫描范围下 | 保持测试包名和正式代码包名一致 |
| Controller 测试 404 | URL 或请求方法写错 | 检查 Controller 的 `@RequestMapping` |
| 断言和实际返回不一致 | 响应结构变化 | 根据统一响应结构调整断言 |
| 测试依赖无法下载 | Maven 配置或网络问题 | 检查 Maven 仓库配置 |

## 八、本章练习

请完成：

1. 为员工详情查询写一个正常测试。
2. 为员工不存在写一个异常测试。
3. 为新增员工接口写一个请求响应测试。
4. 执行 `mvn test`，保存测试结果。

## 九、本章总结

- Service 测试重点验证业务逻辑。
- Controller 测试重点验证 URL、请求、状态码和响应结构。
- `MockMvc` 可以模拟 HTTP 请求。
- 每次改修后都应执行相关测试，确认没有破坏既有功能。
