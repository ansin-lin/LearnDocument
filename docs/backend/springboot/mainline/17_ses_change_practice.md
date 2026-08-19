# 第17章 SES 改修练习

> 本章目标：模拟日本项目中的小型改修任务，按既有代码调查、修改、测试和交付的流程完成变更。

## 一、改修任务

需求：

员工列表查询增加状态筛选。

现状：

```text
GET /employees?department=Sales&page=1&pageSize=20
```

改修后：

```text
GET /employees?department=Sales&status=ACTIVE&page=1&pageSize=20
```

## 二、改修前先做影响调查

不要直接改代码。

先确认这个字段会影响哪些位置。

影响调查的目标是：

- 确认是否已有字段。
- 确认接口是否已有参数。
- 确认 SQL 是否已有条件。
- 确认测试是否需要追加。
- 确认前端或接口说明是否需要同步。

## 三、影响范围

需要确认：

- Controller 查询参数
- 查询 DTO
- Service 查询条件
- Mapper 接口参数
- Mapper XML 动态 SQL
- 测试用例
- 接口说明

## 四、修改步骤

```text
1. 阅读现有员工列表接口
2. 确认 status 字段是否存在
3. 修改查询 DTO
4. 修改 Mapper SQL
5. 增加测试数据
6. 执行正常和异常测试
7. 更新接口说明
```

## 五、具体修改点

查询 DTO：

```java
private String status; // 员工状态查询条件
```

Mapper XML：

```xml
<if test="status != null and status != ''">
    AND status = #{status}
</if>
```

Controller 请求示例：

```text
GET /employees?status=ACTIVE&page=1&pageSize=20
```

接口响应仍然使用原来的分页结构。

不要因为新增一个查询条件就改变响应格式。

## 六、自测观点

| 测试条件 | 预期 |
| --- | --- |
| 不传 `status` | 返回全部状态数据 |
| 传 `ACTIVE` | 只返回在职员工 |
| 传不存在状态 | 返回空列表 |
| 分页参数正常 | 返回指定页数据 |

## 七、自测记录示例

| No | 测试内容 | 请求 | 结果 |
| --- | --- | --- | --- |
| 1 | 不传状态 | `GET /employees?page=1&pageSize=20` | 返回全部状态 |
| 2 | 查询在职 | `GET /employees?status=ACTIVE&page=1&pageSize=20` | 只返回在职 |
| 3 | 查询不存在状态 | `GET /employees?status=UNKNOWN&page=1&pageSize=20` | 返回空列表 |

## 八、改修报告写法

改修完成后，建议整理：

```text
改修内容：
员工列表查询追加 status 条件。

影响文件：
- EmployeeSearchRequest.java
- EmployeeMapper.xml
- EmployeeControllerTest.java

自测：
- 不传 status 查询正常
- status=ACTIVE 查询正常
- status=UNKNOWN 返回空列表
- mvn test 通过

残课题：
无
```

## 九、本章练习

请完成：

1. 增加 `status` 查询条件。
2. 增加至少 3 条测试数据。
3. 写出自测结果。
4. 说明本次改修影响了哪些文件。
5. 写一份简短改修报告。

## 十、本章总结

- 改修前先做影响调查。
- 小改修也要同步修改 DTO、SQL、测试和接口说明。
- 自测记录要能证明改修前后的行为符合要求。
