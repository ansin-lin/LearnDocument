# 第10章 查询与分页

> 本章目标：完成员工列表的条件查询、分页和排序，让接口能支持真实业务列表页面。

## 一、列表接口需求

员工列表页面通常需要：

- 按姓名模糊查询
- 按部门筛选
- 按状态筛选
- 分页显示
- 按创建时间排序

接口示例：

```text
GET /employees?name=Tanaka&department=Sales&page=1&pageSize=20
```

本章使用 MySQL 的 `LIMIT` 和 `OFFSET` 实现分页。

## 二、查询请求 DTO

修改文件：

```text
src/main/java/com/example/employee/dto/EmployeeSearchRequest.java
```

示例代码：

```java
package com.example.employee.dto; // DTO 所在包

public class EmployeeSearchRequest { // 员工查询条件

    private String name; // 姓名关键字，支持模糊查询
    private Long departmentId; // 部门 ID，用于按部门筛选
    private String status; // 员工状态，例如 ACTIVE、INACTIVE
    private int page = 1; // 当前页，默认第 1 页
    private int pageSize = 20; // 每页数量，默认 20

    public String getName() { // 获取姓名关键字
        return name; // 返回姓名关键字
    }

    public void setName(String name) { // 设置姓名关键字
        this.name = name; // 保存姓名关键字
    }

    public Long getDepartmentId() { // 获取部门 ID
        return departmentId; // 返回部门 ID
    }

    public void setDepartmentId(Long departmentId) { // 设置部门 ID
        this.departmentId = departmentId; // 保存部门 ID
    }

    public String getStatus() { // 获取员工状态
        return status; // 返回员工状态
    }

    public void setStatus(String status) { // 设置员工状态
        this.status = status; // 保存员工状态
    }

    public int getPage() { // 获取当前页
        return page; // 返回当前页
    }

    public void setPage(int page) { // 设置当前页
        this.page = page; // 保存当前页
    }

    public int getPageSize() { // 获取每页数量
        return pageSize; // 返回每页数量
    }

    public void setPageSize(int pageSize) { // 设置每页数量
        this.pageSize = pageSize; // 保存每页数量
    }

    public int getOffset() { // 计算 MySQL OFFSET
        return (page - 1) * pageSize; // 根据页码和每页数量计算起始位置
    }
}
```

## 三、分页参数

MySQL 分页使用 `LIMIT` 和 `OFFSET`。

```text
offset = (page - 1) * pageSize
```

例如第 2 页，每页 20 条：

```text
offset = (2 - 1) * 20 = 20
```

分页参数必须做限制。

建议规则：

| 参数 | 规则 |
| --- | --- |
| `page` | 小于 1 时按 1 处理 |
| `pageSize` | 小于 1 时按 20 处理 |
| `pageSize` | 大于 100 时按 100 处理 |

这样可以避免一次查询过多数据。

## 四、列表响应对象

修改文件：

```text
src/main/java/com/example/employee/dto/PageResponse.java
```

示例代码：

```java
package com.example.employee.dto; // DTO 所在包

import java.util.List; // 导入 List

public class PageResponse<T> { // 通用分页响应对象

    private List<T> items; // 当前页数据
    private int page; // 当前页码
    private int pageSize; // 每页数量
    private long total; // 总件数

    public PageResponse(List<T> items, int page, int pageSize, long total) { // 构造分页响应
        this.items = items; // 保存当前页数据
        this.page = page; // 保存当前页码
        this.pageSize = pageSize; // 保存每页数量
        this.total = total; // 保存总件数
    }

    public List<T> getItems() { // 获取当前页数据
        return items; // 返回当前页数据
    }

    public int getPage() { // 获取页码
        return page; // 返回页码
    }

    public int getPageSize() { // 获取每页数量
        return pageSize; // 返回每页数量
    }

    public long getTotal() { // 获取总件数
        return total; // 返回总件数
    }
}
```

接口响应示例：

```json
{
  "items": [
    {
      "id": 1,
      "name": "Tanaka",
      "departmentId": 10,
      "email": "tanaka@example.com",
      "status": "ACTIVE"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

## 五、Mapper 接口

修改文件：

```text
src/main/java/com/example/employee/mapper/EmployeeMapper.java
```

追加方法：

```java
List<Employee> selectPage(EmployeeSearchRequest request); // 根据条件查询当前页员工

long countByCondition(EmployeeSearchRequest request); // 根据条件统计总件数
```

`selectPage` 查询当前页数据。

`countByCondition` 查询符合条件的数据总数，用于前端显示总页数。

## 六、Mapper XML

```sql
SELECT
    id,
    name,
    department_id AS departmentId,
    email,
    status
FROM employees
WHERE status = #{status}
ORDER BY id DESC
LIMIT #{pageSize}
OFFSET #{offset}
```

实际项目中需要动态条件。

修改文件：

```text
src/main/resources/mapper/EmployeeMapper.xml
```

示例代码：

```xml
<select id="selectPage" parameterType="com.example.employee.dto.EmployeeSearchRequest" resultType="com.example.employee.entity.Employee">
    SELECT
        id,
        name,
        department_id AS departmentId,
        email,
        status
    FROM employees
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="departmentId != null">
            AND department_id = #{departmentId}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
    </where>
    ORDER BY id DESC
    LIMIT #{pageSize}
    OFFSET #{offset}
</select>

<select id="countByCondition" parameterType="com.example.employee.dto.EmployeeSearchRequest" resultType="long">
    SELECT COUNT(*)
    FROM employees
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="departmentId != null">
            AND department_id = #{departmentId}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
    </where>
</select>
```

代码说明：

| 写法 | 作用 |
| --- | --- |
| `<where>` | 自动生成 `WHERE`，并处理开头多余的 `AND` |
| `<if>` | 条件成立时才拼接 SQL |
| `LIKE CONCAT('%', #{name}, '%')` | MySQL 模糊查询 |
| `LIMIT #{pageSize}` | 限制返回件数 |
| `OFFSET #{offset}` | 跳过前面的数据 |

## 七、Service 处理分页

修改文件：

```text
src/main/java/com/example/employee/service/EmployeeService.java
```

追加方法：

```java
public PageResponse<EmployeeResponse> search(EmployeeSearchRequest request) { // 查询员工分页列表
    normalizePageRequest(request); // 修正分页参数

    List<Employee> employees = employeeMapper.selectPage(request); // 查询当前页员工
    long total = employeeMapper.countByCondition(request); // 查询总件数

    List<EmployeeResponse> items = employees.stream() // 把 Entity 列表转换成 Response 列表
            .map(EmployeeResponse::from) // 每个 Employee 转换成 EmployeeResponse
            .toList(); // 收集成 List

    return new PageResponse<>(items, request.getPage(), request.getPageSize(), total); // 返回分页结果
}

private void normalizePageRequest(EmployeeSearchRequest request) { // 修正分页参数
    if (request.getPage() < 1) { // 页码小于 1 时
        request.setPage(1); // 修正为第 1 页
    }
    if (request.getPageSize() < 1) { // 每页数量小于 1 时
        request.setPageSize(20); // 修正为默认 20
    }
    if (request.getPageSize() > 100) { // 每页数量过大时
        request.setPageSize(100); // 限制最大 100
    }
}
```

## 八、Controller 接口

修改文件：

```text
src/main/java/com/example/employee/controller/EmployeeController.java
```

追加接口：

```java
@GetMapping // 处理 GET /employees
public ApiResponse<PageResponse<EmployeeResponse>> search(EmployeeSearchRequest request) { // Spring 自动绑定查询参数
    PageResponse<EmployeeResponse> response = employeeService.search(request); // 调用 Service 查询分页数据
    return ApiResponse.success(response); // 返回统一响应
}
```

请求示例：

```text
GET /employees?name=Tanaka&departmentId=10&status=ACTIVE&page=1&pageSize=20
```

## 九、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 第 1 页数据为空 | `offset` 计算错误 | 使用 `(page - 1) * pageSize` |
| 查询非常慢 | 没有限制 `pageSize` 或缺少索引 | 限制最大件数，检查查询条件索引 |
| 总件数不对 | 列表 SQL 和 count SQL 条件不一致 | 两个 SQL 使用相同筛选条件 |
| 模糊查询无结果 | 参数为空或字段不匹配 | 检查请求参数和数据库数据 |

## 十、本章练习

请完成：

1. 增加员工列表查询接口。
2. 支持按部门筛选。
3. 支持分页参数。
4. 返回列表数据和总件数。
5. 当 `pageSize` 大于 100 时，自动按 100 处理。

## 十一、本章总结

- 列表接口通常需要条件查询、分页和排序。
- MySQL 使用 `LIMIT` 和 `OFFSET` 实现分页。
- 分页接口需要同时返回当前页数据和总件数。
- `pageSize` 必须限制最大值，避免一次查询过多数据。
- 列表 SQL 和统计 SQL 的筛选条件必须保持一致。
