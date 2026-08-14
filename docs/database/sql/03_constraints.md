# 第3章 约束基础

> 本章目标：掌握主键、非空、唯一、默认值、检查约束和外键的作用，能够在建表时声明约束，也能够在表已存在时添加、删除和修改约束。

## 一、约束是什么

约束用于限制表中数据的规则。

如果没有约束，错误数据很容易进入数据库。

例如：

- 员工 ID 不能重复。
- 员工姓名不能为空。
- 邮箱不能重复。
- 工资不能小于 0。
- 员工所属部门必须存在。

## 二、常见约束

| 约束 | 英文 | 作用 |
| --- | --- | --- |
| 主键 | `PRIMARY KEY` | 唯一识别一行数据 |
| 非空 | `NOT NULL` | 字段必须有值 |
| 唯一 | `UNIQUE` | 字段值不能重复 |
| 默认值 | `DEFAULT` | 没有指定值时使用默认值 |
| 检查 | `CHECK` | 限制字段值范围 |
| 外键 | `FOREIGN KEY` | 限制字段必须引用另一张表的已有数据 |

约束可以在两个时机定义：

| 时机 | 说明 |
| --- | --- |
| 建表时定义 | 在 `CREATE TABLE` 中直接写约束 |
| 表已存在时添加 | 使用 `ALTER TABLE` 给已有表追加约束 |

## 三、建表时声明约束

### 3.1 MySQL

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);
```

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    email VARCHAR(200) UNIQUE,
    salary DECIMAL(10, 2) DEFAULT 0,
    CONSTRAINT chk_employees_salary CHECK (salary >= 0),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

### 3.2 Oracle

```sql
CREATE TABLE departments (
    id NUMBER(19) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL UNIQUE
);
```

```sql
CREATE TABLE employees (
    id NUMBER(19) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    department_id NUMBER(19) NOT NULL,
    email VARCHAR2(200) UNIQUE,
    salary NUMBER(10, 2) DEFAULT 0,
    CONSTRAINT chk_employees_salary CHECK (salary >= 0),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

### 3.3 PostgreSQL

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);
```

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    email VARCHAR(200) UNIQUE,
    salary NUMERIC(10, 2) DEFAULT 0,
    CONSTRAINT chk_employees_salary CHECK (salary >= 0),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

### 3.4 SQL Server

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);
```

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    email VARCHAR(200) UNIQUE,
    salary DECIMAL(10, 2) DEFAULT 0,
    CONSTRAINT chk_employees_salary CHECK (salary >= 0),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

## 四、三种常见约束写法

约束写法可以分成三类：

- 添加有名称的约束
- 添加无名称的约束
- 添加联合约束

### 4.1 添加有名称的约束

有名称的约束会通过 `CONSTRAINT 约束名` 明确指定约束名称。

语法：

```sql
CONSTRAINT 约束名 约束类型 (列名)
```

示例：

```sql
CREATE TABLE employees (
    id BIGINT,
    email VARCHAR(200),
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email)
);
```

这个写法的优点是后续删除或修改约束时更容易定位。

例如删除唯一约束时，可以直接使用约束名：

```sql
ALTER TABLE 表名
DROP CONSTRAINT 唯一约束名;
```

实际项目中更推荐给主键、外键、唯一约束、检查约束起清晰名称。

### 4.2 添加无名称的约束

无名称约束是不使用 `CONSTRAINT 约束名` 的写法。

示例：

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(200) UNIQUE,
    salary DECIMAL(10, 2) CHECK (salary >= 0)
);
```

这种写法更短，适合学习语法或简单建表。

但是数据库会自动生成约束名。后续删除约束时，需要先查询数据库自动生成的名称。

例如：

```sql
SHOW CREATE TABLE employees;
```

上面写法可以在 MySQL 中查看建表语句和约束名称。

### 4.3 添加联合约束

联合约束是指一个约束同时作用于多个列。

常见场景：

- 一个员工在同一天只能有一个订单编号。
- 同一个部门下员工姓名不能重复。
- 多个字段组合起来才能唯一识别数据。

联合唯一约束语法：

```sql
CONSTRAINT 约束名 UNIQUE (列名1, 列名2)
```

示例：同一个部门中员工姓名不能重复。

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    email VARCHAR(200),
    CONSTRAINT uk_employees_department_name UNIQUE (department_id, name)
);
```

这表示：

| department_id | name | 是否允许 |
| --- | --- | --- |
| 10 | Tanaka | 允许 |
| 20 | Tanaka | 允许 |
| 10 | Tanaka | 不允许 |

因为 `department_id` 和 `name` 组合后不能重复。

联合主键也属于联合约束。

语法：

```sql
CONSTRAINT 主键约束名 PRIMARY KEY (列名1, 列名2)
```

示例：

```sql
CREATE TABLE employee_roles (
    employee_id BIGINT,
    role_code VARCHAR(50),
    CONSTRAINT pk_employee_roles PRIMARY KEY (employee_id, role_code)
);
```

这表示同一个员工不能重复拥有同一个角色。

## 五、表已存在时添加约束

实际项目中，经常会遇到表已经存在，但后续需要追加约束的情况。

添加约束前要确认已有数据是否满足约束规则。

例如：如果 `employees.email` 已经存在重复值，就不能直接添加唯一约束。

### 5.1 添加主键

MySQL / Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 表名
ADD CONSTRAINT 主键约束名 PRIMARY KEY (主键列名);
```

说明：

- `ALTER TABLE 表名` 表示修改指定表。
- `ADD CONSTRAINT 主键约束名` 表示添加一个有名称的约束。
- `PRIMARY KEY (主键列名)` 表示把指定列设置为主键。

示例：

```sql
ALTER TABLE employees
ADD CONSTRAINT pk_employees PRIMARY KEY (id);
```

### 5.2 添加唯一约束

MySQL / Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 表名
ADD CONSTRAINT 唯一约束名 UNIQUE (列名);
```

这个约束表示 `email` 不能重复。

示例：

```sql
ALTER TABLE employees
ADD CONSTRAINT uk_employees_email UNIQUE (email);
```

### 5.3 添加检查约束

MySQL / Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 表名
ADD CONSTRAINT 检查约束名 CHECK (检查条件);
```

这个约束表示 `salary` 不能小于 0。

示例：

```sql
ALTER TABLE employees
ADD CONSTRAINT chk_employees_salary CHECK (salary >= 0);
```

### 5.4 添加外键约束

MySQL / Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 子表名
ADD CONSTRAINT 外键约束名
FOREIGN KEY (子表列名)
REFERENCES 父表名(父表列名);
```

这个约束表示 `employees.department_id` 必须引用 `departments.id` 中已存在的值。

示例：

```sql
ALTER TABLE employees
ADD CONSTRAINT fk_employees_department
FOREIGN KEY (department_id)
REFERENCES departments(id);
```

### 5.5 添加非空约束

非空约束的添加方式在四种数据库中差异比较明显。

| 数据库 | 写法 |
| --- | --- |
| MySQL | `ALTER TABLE 表名 MODIFY 列名 数据类型 NOT NULL;` |
| Oracle | `ALTER TABLE 表名 MODIFY (列名 数据类型 NOT NULL);` |
| PostgreSQL | `ALTER TABLE 表名 ALTER COLUMN 列名 SET NOT NULL;` |
| SQL Server | `ALTER TABLE 表名 ALTER COLUMN 列名 数据类型 NOT NULL;` |

添加 `NOT NULL` 前，必须确认该列没有 `NULL` 数据。

### 5.6 添加默认值

MySQL：

```sql
ALTER TABLE 表名
ALTER 列名 SET DEFAULT 默认值;
```

Oracle：

```sql
ALTER TABLE 表名
MODIFY (列名 DEFAULT 默认值);
```

PostgreSQL：

```sql
ALTER TABLE 表名
ALTER COLUMN 列名 SET DEFAULT 默认值;
```

SQL Server：

```sql
ALTER TABLE 表名
ADD CONSTRAINT 默认值约束名 DEFAULT 默认值 FOR 列名;
```

SQL Server 的默认值通常会生成一个默认约束，建议明确命名。

## 六、删除约束

删除约束时，通常需要知道约束名。

因此项目中建议给约束起清晰名称。

### 6.1 删除主键

MySQL：

```sql
ALTER TABLE 表名
DROP PRIMARY KEY;
```

Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 表名
DROP CONSTRAINT 主键约束名;
```

### 6.2 删除唯一约束

MySQL：

```sql
ALTER TABLE 表名
DROP INDEX 唯一索引名;
```

Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 表名
DROP CONSTRAINT 唯一约束名;
```

MySQL 中唯一约束通常表现为唯一索引，所以删除时常用 `DROP INDEX`。

### 6.3 删除检查约束

Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 表名
DROP CONSTRAINT 检查约束名;
```

MySQL：

```sql
ALTER TABLE 表名
DROP CHECK 检查约束名;
```

### 6.4 删除外键约束

MySQL：

```sql
ALTER TABLE 表名
DROP FOREIGN KEY 外键约束名;
```

Oracle / PostgreSQL / SQL Server：

```sql
ALTER TABLE 表名
DROP CONSTRAINT 外键约束名;
```

### 6.5 删除非空约束

| 数据库 | 写法 |
| --- | --- |
| MySQL | `ALTER TABLE 表名 MODIFY 列名 数据类型 NULL;` |
| Oracle | `ALTER TABLE 表名 MODIFY (列名 数据类型 NULL);` |
| PostgreSQL | `ALTER TABLE 表名 ALTER COLUMN 列名 DROP NOT NULL;` |
| SQL Server | `ALTER TABLE 表名 ALTER COLUMN 列名 数据类型 NULL;` |

### 6.6 删除默认值

MySQL：

```sql
ALTER TABLE 表名
ALTER 列名 DROP DEFAULT;
```

Oracle：

```sql
ALTER TABLE 表名
MODIFY (列名 DEFAULT NULL);
```

PostgreSQL：

```sql
ALTER TABLE 表名
ALTER COLUMN 列名 DROP DEFAULT;
```

SQL Server：

```sql
ALTER TABLE 表名
DROP CONSTRAINT 默认值约束名;
```

SQL Server 删除默认值时需要删除默认约束名。

## 七、修改约束

多数数据库不能直接“修改约束内容”。

常见做法是：

1. 删除旧约束。
2. 添加新约束。

例如原来工资要求大于等于 0：

```sql
CONSTRAINT chk_employees_salary CHECK (salary >= 0)
```

现在改成工资必须大于等于 100000。

Oracle / PostgreSQL / SQL Server 常见思路：

```sql
ALTER TABLE 表名
DROP CONSTRAINT 旧检查约束名;
```

```sql
ALTER TABLE 表名
ADD CONSTRAINT 新检查约束名 CHECK (新检查条件);
```

MySQL 删除检查约束时使用：

```sql
ALTER TABLE 表名
DROP CHECK 旧检查约束名;
```

然后再添加新检查约束：

```sql
ALTER TABLE 表名
ADD CONSTRAINT 新检查约束名 CHECK (新检查条件);
```

## 八、查看约束

不同数据库查看约束的方式不同。

MySQL：

```sql
SHOW CREATE TABLE employees;
```

Oracle：

```sql
SELECT constraint_name, constraint_type
FROM user_constraints
WHERE table_name = 'EMPLOYEES';
```

PostgreSQL：

```sql
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'employees';
```

SQL Server：

```sql
SELECT name, type_desc
FROM sys.objects
WHERE parent_object_id = OBJECT_ID('employees');
```

## 九、外键的作用

外键保证员工的 `department_id` 必须来自 `departments.id`。

如果部门表中没有 `id = 99` 的部门，下面 SQL 会失败：

```sql
INSERT INTO employees (id, name, department_id, email, salary)
VALUES (1, 'Tanaka', 99, 'tanaka@example.com', 300000);
```

这样可以避免员工被分配到不存在的部门。

## 十、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| 主键重复 | `PRIMARY KEY` 不允许重复 | 确认 ID 生成规则 |
| 必填字段为空 | `NOT NULL` 不允许空值 | 插入时提供值 |
| 添加唯一约束失败 | 已有数据存在重复值 | 先查重并清理数据 |
| 添加非空约束失败 | 已有数据存在 `NULL` | 先更新空值 |
| 外键插入失败 | 引用的部门不存在 | 先插入部门数据 |
| 删除约束失败 | 约束名写错 | 先查询约束名 |
| 约束名不清晰 | 后续维护困难 | 使用 `pk_`、`fk_`、`uk_`、`chk_`、`df_` 前缀 |

## 十一、本章练习

请完成：

1. 创建部门表并设置主键。
2. 创建员工表并设置非空、唯一、默认值、检查约束。
3. 给已存在的员工表添加部门外键。
4. 给已存在的员工表添加邮箱唯一约束。
5. 删除邮箱唯一约束。
6. 修改工资检查约束，从 `salary >= 0` 改成 `salary >= 100000`。
7. 查询当前表中已经存在的约束。

## 十二、本章总结

- 约束用于保证数据正确性。
- 约束既可以在建表时声明，也可以在表存在后添加。
- 删除和修改约束通常需要知道约束名。
- 修改约束通常采用“删除旧约束，再添加新约束”的方式。
- 四种数据库在 `NOT NULL`、`DEFAULT`、删除约束等语法上差异较明显。
