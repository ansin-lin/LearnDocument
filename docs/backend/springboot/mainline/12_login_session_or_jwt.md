# 第12章 登录与权限基础

> 本章目标：理解登录、认证、授权的区别，掌握后端判断当前用户和权限的基本思路。

## 一、认证与授权

| 概念 | 含义 | 示例 |
| --- | --- | --- |
| 认证 | 判断用户是谁 | 用户名密码登录 |
| 授权 | 判断用户能做什么 | 管理员可以删除员工 |

认证成功不代表拥有所有权限。

## 二、登录接口解决什么问题

登录接口的核心目标是：

1. 校验账号和密码是否正确。
2. 登录成功后生成登录凭证。
3. 前端保存登录凭证。
4. 之后的请求携带登录凭证。
5. 后端根据登录凭证识别当前用户。

如果没有登录凭证，后端无法知道请求来自哪个用户。

## 三、Session 和 JWT 的选择

| 方式 | 特点 | 常见场景 |
| --- | --- | --- |
| Session | 状态保存在后端 | 传统 Web、一体式系统 |
| JWT | 状态主要在 Token 中 | 前后端分离、移动端 API |

前后端分离 API 常使用 Token 方式传递登录状态。

Session 适合 JSP、Thymeleaf 等前后台一体式系统。

## 四、登录接口基本流程

```text
1. 前端提交用户名和密码
2. 后端查询用户
3. 后端校验密码
4. 校验成功后创建登录状态
5. 前端之后的请求携带登录凭证
6. 后端根据凭证判断当前用户
```

## 五、登录请求和响应

登录请求 DTO：

```java
package com.example.employee.dto; // DTO 所在包

public class LoginRequest { // 登录请求对象

    private String username; // 用户名
    private String password; // 密码

    public String getUsername() { // 获取用户名
        return username; // 返回用户名
    }

    public void setUsername(String username) { // 设置用户名
        this.username = username; // 保存用户名
    }

    public String getPassword() { // 获取密码
        return password; // 返回密码
    }

    public void setPassword(String password) { // 设置密码
        this.password = password; // 保存密码
    }
}
```

登录响应 DTO：

```java
package com.example.employee.dto; // DTO 所在包

public class LoginResponse { // 登录响应对象

    private String token; // 登录成功后返回给前端的 Token
    private String username; // 当前登录用户名
    private String role; // 当前用户角色

    public LoginResponse(String token, String username, String role) { // 构造登录响应
        this.token = token; // 保存 Token
        this.username = username; // 保存用户名
        this.role = role; // 保存角色
    }

    public String getToken() { // 获取 Token
        return token; // 返回 Token
    }

    public String getUsername() { // 获取用户名
        return username; // 返回用户名
    }

    public String getRole() { // 获取角色
        return role; // 返回角色
    }
}
```

响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "token": "sample-token",
    "username": "admin",
    "role": "ADMIN"
  }
}
```

这里的 `sample-token` 是示例值。真实项目中 Token 需要使用安全方式生成，不能写固定字符串。

## 六、登录 Controller 示例

```java
package com.example.employee.controller; // Controller 所在包

import com.example.employee.common.ApiResponse; // 统一响应
import com.example.employee.dto.LoginRequest; // 登录请求
import com.example.employee.dto.LoginResponse; // 登录响应
import com.example.employee.service.AuthService; // 登录业务 Service
import org.springframework.web.bind.annotation.PostMapping; // POST 映射
import org.springframework.web.bind.annotation.RequestBody; // 接收 JSON 请求体
import org.springframework.web.bind.annotation.RequestMapping; // 路径前缀
import org.springframework.web.bind.annotation.RestController; // REST Controller

@RestController // 返回 JSON
@RequestMapping("/auth") // 登录相关接口前缀
public class AuthController { // 登录 Controller

    private final AuthService authService; // 登录业务对象

    public AuthController(AuthService authService) { // 构造器注入
        this.authService = authService; // 保存登录业务对象
    }

    @PostMapping("/login") // 处理 POST /auth/login
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) { // 接收登录 JSON
        LoginResponse response = authService.login(request); // 调用登录业务
        return ApiResponse.success(response); // 返回统一响应
    }
}
```

## 七、权限判断位置

权限判断不应只写在前端。

后端必须判断：

- 当前用户是否登录
- 当前用户是否有访问接口的权限
- 当前用户是否能操作目标数据

例如删除员工接口：

```java
@DeleteMapping("/{id}") // 处理 DELETE /employees/{id}
public ApiResponse<Void> delete(@PathVariable Long id, @RequestHeader("Authorization") String token) { // 接收路径 ID 和 Token
    authService.checkAdmin(token); // 判断当前用户是否是管理员
    employeeService.delete(id); // 权限通过后执行删除
    return ApiResponse.success(null); // 返回成功响应
}
```

上面代码说明：

| 写法 | 作用 |
| --- | --- |
| `@RequestHeader("Authorization")` | 从请求头中获取登录凭证 |
| `checkAdmin(token)` | 判断当前用户是否拥有管理员权限 |
| `employeeService.delete(id)` | 权限通过后才执行删除 |

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 只隐藏前端按钮 | 前端代码可以被绕过 | 后端接口必须做权限判断 |
| Token 写死 | 任何人都能伪造登录 | 使用安全 Token 生成和校验 |
| 密码明文保存 | 数据泄露风险高 | 真实项目使用加密哈希保存 |
| 所有登录失败都返回详细原因 | 容易被枚举账号 | 返回统一登录失败提示 |

## 九、本章练习

请完成：

1. 说明认证和授权的区别。
2. 设计登录接口的请求字段和响应字段。
3. 说明为什么隐藏前端按钮不能代替后端权限控制。
4. 编写 `POST /auth/login` 的 Controller 方法。
5. 说明删除员工接口为什么需要管理员权限。

## 十、本章总结

- 认证是判断用户是谁，授权是判断用户能做什么。
- 前后端分离项目常用 Token 传递登录状态。
- 权限判断必须在后端完成。
- 密码、Token 和权限逻辑属于安全敏感内容，不能随意简化到生产项目中。
