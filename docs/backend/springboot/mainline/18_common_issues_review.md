# 第18章 常见问题与复习

> 本章目标：整理 Spring Boot 后端开发中的常见问题，建立排查顺序和复习清单。

## 一、常见启动问题

| 现象 | 可能原因 | 排查 |
| --- | --- | --- |
| 端口占用 | 8080 已被使用 | 修改端口或停止旧进程 |
| Bean 找不到 | 包扫描范围不正确 | 检查启动类位置和包名 |
| 数据库连接失败 | 地址、账号、密码错误 | 检查 `application.yml` |
| Mapper XML 找不到 | 路径配置错误 | 检查 `mapper-locations` |

启动问题排查顺序：

```text
1. 看控制台最上面的异常类型
2. 看 Caused by 后面的真实原因
3. 判断是端口、Bean、配置、数据库还是 Mapper 问题
4. 修改后重新启动验证
```

## 二、常见接口问题

| 现象 | 可能原因 | 排查 |
| --- | --- | --- |
| 404 | URL 或请求方法错误 | 检查 Controller 映射 |
| 400 | 参数绑定或校验失败 | 检查请求体和 DTO |
| 500 | 后端异常 | 查看日志和异常堆栈 |
| 返回字段缺失 | VO 字段或 getter 缺失 | 检查响应对象 |

接口问题排查顺序：

```text
1. 确认请求 URL
2. 确认 HTTP 方法
3. 确认请求参数或 JSON
4. 看 Controller 是否进入
5. 看 Service 业务是否正常
6. 看 Mapper SQL 是否正常
7. 看统一异常返回
```

## 三、常见数据库问题

| 现象 | 可能原因 | 排查 |
| --- | --- | --- |
| 查询为空 | 条件错误或测试数据不存在 | 直接执行 SQL 验证 |
| 插入失败 | 字段非空或类型不匹配 | 检查表结构和 Entity |
| 事务不回滚 | 异常被吞掉或注解不生效 | 检查 Service 方法 |

数据库问题排查顺序：

```text
1. 直接在数据库客户端执行 SQL
2. 确认表名和字段名
3. 确认测试数据是否存在
4. 确认 Java 属性和数据库列映射
5. 确认事务是否提交或回滚
```

## 四、常见注解复习

| 注解 | 使用位置 | 作用 |
| --- | --- | --- |
| `@SpringBootApplication` | 启动类 | 启动 Spring Boot 应用 |
| `@RestController` | Controller 类 | 声明 REST 接口类 |
| `@RequestMapping` | 类或方法 | 设置请求路径 |
| `@GetMapping` | 方法 | 处理 GET 请求 |
| `@PostMapping` | 方法 | 处理 POST 请求 |
| `@PutMapping` | 方法 | 处理 PUT 请求 |
| `@DeleteMapping` | 方法 | 处理 DELETE 请求 |
| `@RequestParam` | 方法参数 | 接收查询参数 |
| `@PathVariable` | 方法参数 | 接收路径参数 |
| `@RequestBody` | 方法参数 | 接收 JSON 请求体 |
| `@Service` | Service 类 | 声明业务类 |
| `@Mapper` | Mapper 接口 | 声明 MyBatis Mapper |
| `@Transactional` | Service 方法 | 声明事务边界 |
| `@RestControllerAdvice` | 异常处理类 | 统一处理异常 |
| `@ExceptionHandler` | 异常处理方法 | 指定处理哪类异常 |

## 五、分层职责复习

| 层 | 职责 | 不应该做的事 |
| --- | --- | --- |
| Controller | 接收请求、返回响应 | 写复杂业务和 SQL |
| DTO | 接收请求参数 | 直接当数据库表对象使用 |
| Service | 编排业务流程、事务控制 | 处理 HTTP 细节 |
| Mapper | 执行数据库访问 | 写业务判断 |
| Entity | 表示数据库数据 | 承担接口返回格式 |
| VO / Response | 返回给前端的数据 | 直接执行数据库访问 |

## 六、复习清单

请确认自己能说明：

- Spring Boot 启动类的作用
- Controller、Service、Mapper 的职责
- DTO、VO、Entity 的区别
- `@RequestParam`、`@PathVariable`、`@RequestBody` 的区别
- `@Valid` 的作用
- MyBatis Mapper 接口和 XML 的关系
- `#{}` 参数绑定的作用
- 事务为什么放在 Service 层
- 统一异常处理的作用
- 日志排查的基本顺序

## 七、综合复习任务

请从零说明员工新增接口的完整流程：

```text
前端请求
→ Controller 接收 JSON
→ DTO 参数校验
→ Service 执行业务
→ Mapper 执行 INSERT
→ MySQL 保存数据
→ Service 返回 VO
→ Controller 返回统一响应
```

## 八、最终自查问题

请逐项确认：

1. 能否从零创建 Spring Boot 项目并启动。
2. 能否写一个 `GET` 接口和一个 `POST` 接口。
3. 能否说明 DTO、Entity、Response 的区别。
4. 能否使用 MyBatis 查询 MySQL。
5. 能否完成员工增删改查。
6. 能否给写操作加事务。
7. 能否使用统一异常返回业务错误。
8. 能否通过日志定位 500 错误。
9. 能否执行 `mvn test`。
10. 能否打包 jar 并在 Linux 上运行。

## 九、本章总结

- 排查问题要按请求链路逐层确认，不要只盯一个文件。
- Spring Boot 后端主线要掌握 Controller、Service、Mapper、数据库、异常、日志、测试和部署。
- 现场改修时，影响调查、自测证据和交付说明与代码修改同样重要。
