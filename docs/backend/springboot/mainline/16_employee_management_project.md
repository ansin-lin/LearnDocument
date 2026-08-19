# 第16章 项目整理与验收

> 本章目标：整理员工管理后端 API，确认接口、数据库、日志、测试和部署说明满足交付要求。

## 一、最终功能清单

项目应包含：

- 健康检查接口
- 员工详情查询
- 员工列表查询
- 新增员工
- 修改员工
- 删除员工
- 参数校验
- 统一响应
- 统一异常
- 基础日志
- 登录与权限基础
- 测试用例
- 多环境配置
- 打包运行说明

## 二、最终项目目录检查

项目目录应至少包含：

```text
employee-management-api/
├── pom.xml
├── src/main/java/com/example/employee/
│   ├── EmployeeManagementApplication.java
│   ├── common/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── exception/
│   ├── mapper/
│   └── service/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── mapper/
└── src/test/java/
```

如果目录混乱，改修时很难判断代码应该写在哪里。

## 三、接口验收表

| 功能 | 方法 | URL | 预期 |
| --- | --- | --- | --- |
| 健康检查 | `GET` | `/health` | 返回 `OK` |
| 查询详情 | `GET` | `/employees/{id}` | 返回员工信息 |
| 查询列表 | `GET` | `/employees` | 返回分页列表 |
| 新增员工 | `POST` | `/employees` | 返回新增结果 |
| 修改员工 | `PUT` | `/employees/{id}` | 返回修改结果 |
| 删除员工 | `DELETE` | `/employees/{id}` | 返回删除结果 |

## 四、数据库验收

需要确认：

- `employees` 表存在。
- 主键可以自增。
- 必填字段有约束。
- 查询、插入、更新、删除都能正常执行。
- 测试数据能覆盖正常和异常场景。

示例确认 SQL：

```sql
SELECT id,
       name,
       department_id,
       email,
       status
FROM employees
ORDER BY id DESC;
```

## 五、测试验收

执行：

```bash
mvn test
```

预期：

```text
BUILD SUCCESS
```

如果测试失败，不能直接交付。

需要先确认失败的是：

- 代码逻辑错误
- 测试数据错误
- 配置错误
- 断言没有同步修改

## 六、自测证据

交付前建议保留：

- 接口请求和响应截图
- 数据库执行前后数据
- 测试执行结果
- 启动日志
- 异常场景验证结果

## 七、交付说明

交付文档应包含：

- 项目名称
- 运行环境
- 数据库初始化 SQL
- 配置文件说明
- 打包命令
- 启动命令
- 接口清单
- 已知问题

## 八、验收顺序

建议按下面顺序验收：

```text
1. 确认数据库表和测试数据
2. 启动本地开发环境
3. 验证健康检查接口
4. 验证员工 CRUD 接口
5. 验证查询分页接口
6. 验证异常返回
7. 执行自动化测试
8. Maven 打包
9. 使用 prod 配置启动 jar
10. 整理自测证据
```

## 九、本章练习

请完成：

1. 整理接口清单。
2. 整理数据库初始化 SQL。
3. 执行测试并保存结果。
4. 写出项目启动和停止步骤。
5. 根据接口验收表逐项记录结果。

## 十、本章总结

- 项目验收不是只看能否启动。
- 后端交付需要确认接口、数据库、异常、日志、测试和部署。
- 自测证据可以降低 Review 和交付沟通成本。
