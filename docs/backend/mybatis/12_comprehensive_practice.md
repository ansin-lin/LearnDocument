# 第12章 MyBatis 综合练习

> 本章目标：综合使用配置文件、Mapper 接口、XML、CRUD、参数映射、动态 SQL、ResultMap 和缓存基础。

## 一、练习目标

完成一个独立 MyBatis 员工管理练习。

需要实现：

- 查询员工列表
- 根据 ID 查询员工
- 新增员工
- 修改员工
- 删除员工
- 条件查询员工
- 根据 ID 列表查询员工
- 查询员工和部门
- 查询部门和员工列表

## 二、统一表结构

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    email VARCHAR(200),
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
);
```

## 三、初始数据

```sql
INSERT INTO departments (id, name)
VALUES
    (10, 'Sales'),
    (20, 'Development');

INSERT INTO employees (name, department_id, email)
VALUES
    ('Tanaka', 10, 'tanaka@example.com'),
    ('Suzuki', 20, 'suzuki@example.com'),
    ('Sato', 20, NULL);
```

## 四、需要创建的 Java 文件

```text
com.example.mybatis.entity.Employee
com.example.mybatis.entity.Department
com.example.mybatis.mapper.EmployeeMapper
com.example.mybatis.mapper.DepartmentMapper
com.example.mybatis.Main
```

## 五、需要创建的资源文件

```text
db.properties
mybatis-config.xml
mapper/EmployeeMapper.xml
mapper/DepartmentMapper.xml
```

## 六、EmployeeMapper 任务

请实现：

```java
List<Employee> selectAll();

Employee selectById(Long id);

int insert(Employee employee);

int update(Employee employee);

int deleteById(Long id);

List<Employee> selectByCondition(Employee condition);

List<Employee> selectByIds(@Param("ids") List<Long> ids);

Employee selectEmployeeWithDepartment(Long id);
```

## 七、DepartmentMapper 任务

请实现：

```java
Department selectDepartmentWithEmployees(Long id);
```

## 八、验证要求

请确认：

1. 查询员工列表能输出 3 条数据。
2. 根据 ID 查询能查到 `Tanaka`。
3. 新增员工后能得到自增 ID。
4. 修改邮箱后查询结果变化。
5. 删除员工后列表数量减少。
6. 条件查询不会生成错误 SQL。
7. ID 列表查询能使用 `<foreach>`。
8. 员工查询能带出部门对象。
9. 部门查询能带出员工列表。

## 九、常见问题排查

| 问题 | 检查点 |
| --- | --- |
| Mapper 找不到 | namespace、id、mappers 注册 |
| 字段为空 | resultMap、别名、驼峰映射 |
| 新增没保存 | 是否调用 `commit()` |
| IN 查询报错 | 集合参数是否为空，`@Param` 是否一致 |
| 关联对象为空 | SQL 别名和 resultMap 是否一致 |

## 十、本章总结

- MyBatis 学习重点是接口、XML、SQL、实体类之间的一致性。
- CRUD、参数映射、动态 SQL 和 ResultMap 是基础主线。
- 缓存需要理解作用范围和失效规则，不要作为默认优化。
