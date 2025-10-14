# SQL 基础入门 —— 语法结构与基本查询

## 什么是 SQL  

**SQL（Structured Query Language，结构化查询语言）**  
是一种用于操作关系型数据库（如 MySQL、PostgreSQL、Oracle、SQL Server 等）的标准语言。

SQL 的主要功能包括：

- 定义数据库和表的结构；
- 插入、更新、删除数据；
- 查询和分析数据；
- 管理用户权限与安全性。

---

## SQL 语法结构

| 类型 | 名称 | 说明 | 常见命令 |
|------|------|------|----------|
| DDL | 数据定义语言 | 定义数据库结构 | `CREATE`, `ALTER`, `DROP` |
| DML | 数据操作语言 | 增删改数据 | `INSERT`, `UPDATE`, `DELETE` |
| DQL | 数据查询语言 | 查询数据 | `SELECT` |
| DCL | 数据控制语言 | 控制权限 | `GRANT`, `REVOKE` |

**提示**：  
SQL 对大小写不敏感，但建议关键字使用 **大写**，以提高可读性。

---

## 常用数据类型  

| 类型 | 说明 | 示例 |
|------|------|------|
| INT | 整数 | `age INT` |
| DECIMAL(M,D) | 定点小数（共 M 位，小数 D 位） | `price DECIMAL(8,2)` |
| VARCHAR(n) | 可变长度字符串 | `name VARCHAR(50)` |
| CHAR(n) | 固定长度字符串 | `gender CHAR(1)` |
| DATE | 日期类型 | `birth DATE` |
| DATETIME | 日期 + 时间 | `created_at DATETIME` |
| BOOLEAN | 布尔值（真/假） | `is_active BOOLEAN` |

---

## 创建数据库

虽然我们叫创建数据库，但并不是真正意义上的建一个数据库系统，而是创建一个逻辑命名空间。

### 建库语法(mysql/postgresql/sqlserve)

```sql
CREATE DATABASE 数据库名称
```

示例

```sql
CREATE DATABASE my_db
```

### 建库语法(oracle)

```sql
CREATE USER <schema> IDENTIFIED BY <password>;
```

示例

```sql
CREATE USER mydb IDENTIFIED BY pwd;
```

## 创建表（CREATE TABLE）  

### 建表语法

```sql
CREATE TABLE 表名称
(
    列名称1 数据类型,
    列名称2 数据类型,
    列名称3 数据类型,
    ....
)
```

### 建表示例

```sql
CREATE TABLE 表名 (
    id INT ,
    name VARCHAR(50) NOT NULL,
    age INT,
    列名 DECIMAL(5,2)
);
```

---

## 约束（CONSTRAINT）  

约束用于 **保证数据的正确性与一致性**。

| 约束类型 | 说明 | 示例 |
|-----------|------|------|
| **PRIMARY KEY** | 主键，唯一标识一条记录 | `PRIMARY KEY (id)` |
| **UNIQUE** | 唯一约束，防止重复 | `UNIQUE (列名)` |
| **NOT NULL** | 不允许为空 | `name VARCHAR(30) NOT NULL` |
| **DEFAULT** | 默认值 | `status VARCHAR(10) DEFAULT 'active'` |
| **CHECK** | 检查条件（部分数据库支持） | `CHECK (age >= 0)` |
| **FOREIGN KEY** | 外键，建立表之间的关系 | `FOREIGN KEY (dept_id) REFERENCES department(id)` |

---

### 创建表时直接定义约束（CREATE TABLE）

> 在创建表时，可以直接定义各类约束，无需使用 ALTER TABLE。以下对不同数据库的语法进行对比。

---

#### 1️⃣ PRIMARY KEY（主键）

- **MySQL / PostgreSQL / SqlServe / Oracle**
  - **普通定义**

    ```sql
    CREATE TABLE 表名 (
        id INT PRIMARY KEY,
        name VARCHAR(50)
    );
    ```

  - **命名定义**
  
    ```sql
    CREATE TABLE 表名 (
        id INT,
        name VARCHAR(50),
        CONSTRAINT pk_表名 PRIMARY KEY (id)
    );
    ```

---

#### 2️⃣ UNIQUE（唯一约束）

- **MySQL / SQL Server / PostgreSQL / Oracle**
  - **普通定义**

    ```sql
    CREATE TABLE 表名 (
        列名 VARCHAR(100) UNIQUE
    );
    ```

  - **命名定义**

    ```sql
    CREATE TABLE 表名 (
        列名 VARCHAR(100),
        CONSTRAINT uq_列名 UNIQUE (列名)
    );
    ```

---

#### 3️⃣ NOT NULL（非空约束）

> NOT NULL 作为列属性定义，不支持命名。

- 通用语法

```sql
CREATE TABLE 表名 (
    name VARCHAR(50) NOT NULL,
    age INT
);
```

---

#### 4️⃣ DEFAULT（默认值）

- **MySQL / PostgreSQL / Oracle / SqlServe**
  - **普通定义**

    ```sql
    CREATE TABLE 表名 (
        name VARCHAR(50),
        status VARCHAR(10) DEFAULT 'active'
    );
    ```

  - **命名定义**
  
    ```sql
    CREATE TABLE 表名 (
        name NVARCHAR(50),
        status NVARCHAR(10) CONSTRAINT df_status DEFAULT 'active'
    );
    ```

---

#### 5️⃣ CHECK（检查约束）

- **SQL Server / PostgreSQL / Oracle / MySQL**

  - **普通定义**

    ```sql
    CREATE TABLE 表名 (
        age INT CHECK (age >= 0)
    );
    ```

  - **命名定义**

    ```sql
    CREATE TABLE 表名 (
        age INT,
        CONSTRAINT ck_age CHECK (age >= 0)
    );
    ```

---

#### 6️⃣ FOREIGN KEY（外键）

- **MySQL / Oracle / PostgreSql /SqlServe**

  - **普通定义**

    ```sql
    CREATE TABLE 表名 (
        id INT PRIMARY KEY,
        dept_id INT,
        FOREIGN KEY (dept_id) REFERENCES department(id)
    );
    ```

  - **命名定义**

    ```sql
    CREATE TABLE 表名 (
        id INT PRIMARY KEY,
        dept_id INT,
        CONSTRAINT fk_dept FOREIGN KEY (dept_id) REFERENCES department(id)
    );
    ```

---

### 添加（ADD）

#### 1. PRIMARY KEY（主键）

- **MySQL / Oracle / SQL Server / PostgreSQL**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ADD PRIMARY KEY (列名);
    ```

  - **命名语法**

    ```sql
    ALTER TABLE 表名 ADD CONSTRAINT 约束名 PRIMARY KEY (列名);
    ```

---

#### 2. UNIQUE（唯一）

- **MySQL / PostgreSql / sqlserve / oracle**

  - **普通语法**
  
    ```sql
    ALTER TABLE 表名 ADD UNIQUE (列名);
    ```

  - **命名语法**

    ```sql
    ALTER TABLE 表名 ADD CONSTRAINT 约束名 UNIQUE (列名);
    ```

---

#### 3. NOT NULL（非空）

> 说明：NOT NULL 为列属性，不支持“命名约束”。

- **MySQL / Oracle**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 MODIFY 列名 数据类型 NOT NULL;
    ```

- **SQL Server / PostgreSQL**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 数据类型 NOT NULL;
    ```

---

#### 4. DEFAULT（默认值）

- **MySQL**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 SET DEFAULT 默认值;
    ```

  - **命名语法**
    - 不支持命名 DEFAULT。

- **SQL Server**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ADD DEFAULT 默认值 FOR 列名;
    ```

  - **命名语法**

    ```sql
    ALTER TABLE 表名 ADD CONSTRAINT 约束名 DEFAULT 默认值 FOR 列名;
    ```

- **PostgreSQL**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 SET DEFAULT 默认值;
    ```

  - **命名语法**
    - 不支持命名 DEFAULT。

- **Oracle**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 MODIFY 列名 DEFAULT 默认值;
    ```

  - **命名语法**
    - 不支持命名 DEFAULT（作为列属性保存）。

---

#### 5. CHECK（检查约束）

- **MySQL /SQL Server/PostGreSql / Oracle**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ADD CHECK (条件表达式);
    ```

  - **命名语法**

    ```sql
    ALTER TABLE 表名 ADD CONSTRAINT 约束名 CHECK (条件表达式);
    ```

---

#### 6. FOREIGN KEY（外键）

- **MySQL / SQL Server / PostGreSql / Oracle**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ADD FOREIGN KEY (列名) REFERENCES 关联表(关联列);
    ```

  - **命名语法**

    ```sql
    ALTER TABLE 表名 ADD CONSTRAINT 约束名 FOREIGN KEY (列名) REFERENCES 关联表(关联列);
    ```

---

### 备注与命名建议

- 常用命名前缀：
  - 主键：`pk_表名`
  - 唯一：`uq_列名`
  - 外键：`fk_列名`
  - 检查：`ck_列名`
  - 默认：`df_列名`
- NOT NULL/DEFAULT 在 MySQL、PostgreSQL、Oracle 中本质是 **列属性**，不可命名（SQL Server 的 DEFAULT 可命名）。

---

### 修改（MODIFY / ALTER）

> 大多数约束“修改”需要 **先删除原约束，再按新的定义添加**。下面给出通用方式与各库要点。

#### PRIMARY KEY（主键）

- **通用思路**

  ```sql
  -- 先删除，再新增
  （见“删除 PRIMARY KEY”）
  （见“添加 PRIMARY KEY”）
  ```

#### UNIQUE（唯一）

- **通用思路**
  
  ```sql
  -- 先删除旧唯一，再新增新唯一
  （见“删除 UNIQUE”）
  （见“添加 UNIQUE”）
  ```

#### NOT NULL（非空）

- **通用语法**
  - MySQL / Oracle：
  
    ```sql
    ALTER TABLE 表名 MODIFY 列名 新数据类型 [NOT NULL | NULL];
    ```

  - SQL Server：
  
    ```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 新数据类型 [NOT NULL | NULL];
    ```

  - PostgreSQL：

    ```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 [SET NOT NULL | DROP NOT NULL];
    ```

#### DEFAULT（默认值）

- **通用思路**
  - MySQL / PostgreSQL：直接 **SET DEFAULT** 改值；或 **DROP DEFAULT** 后再 **SET DEFAULT**。
  - SQL Server：需先 **DROP CONSTRAINT** 再以新值 **ADD CONSTRAINT DEFAULT**。
  - Oracle：直接 **MODIFY 列名 DEFAULT 新值**。

#### CHECK（检查）

- **通用思路**
  
  ```sql
  -- 先 DROP 旧检查，再 ADD 新检查
  （见“删除 CHECK”）
  （见“添加 CHECK”）
  ```

#### FOREIGN KEY（外键）

- **通用思路**
  
  ```sql
  -- 先 DROP 外键，再 ADD 外键（含 ON DELETE/UPDATE 规则）
  （见“删除 FOREIGN KEY”）
  （见“添加 FOREIGN KEY”）
  ```

---

### 删除（DROP）

#### PRIMARY KEY

- **MySQL**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 DROP PRIMARY KEY;
    ```

- **SQL Server/PostgreSQL/Oracle**

  - **普通 / 命名语法**

    ```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
    ```

---

#### UNIQUE

- **mysql**

  - **普通 / 命名语法**

    ```sql
    ALTER TABLE 表名 DROP INDEX 索引名;
    ```

- **Oracle/SQL Server/PostgreSQL**

  - **普通 / 命名语法**

    ```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
    ```

---

#### NOT NULL

- **mysql/Oracle**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 MODIFY 列名 数据类型 NULL;
    ```

- **SQL Server/PostgreSQL**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 数据类型 NULL;
    ```

---

#### DEFAULT

- **MySQL/PostgreSQL**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 DROP DEFAULT;
    ```

- **SQLServer**

  - **普通 / 命名语法**

    ```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
    ```

- **oracle**

  - **普通语法**

    ```sql
    ALTER TABLE 表名 MODIFY 列名 DEFAULT NULL;  -- 通过设为 NULL 清除默认值
    ```

---

#### CHECK

- **MySQL / SQLServer / PostgreSQL / Oracle**

  - **普通 / 命名语法**

    ```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
    ```

---

#### FOREIGN KEY

- **mysql**

  - **普通 / 命名语法**

    ```sql
    ALTER TABLE 表名 DROP FOREIGN KEY 约束名;
    ```

- **SQL Server / PostgreSQL / Oracle**

  - **普通 / 命名语法**

    ```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
    ```

---

## 修改表结构（ALTER TABLE）

在关系型数据库中，`ALTER TABLE` 语句用于修改已有表的结构，常见操作包括：添加列、修改列名、修改数据类型、删除列。

---

### 添加列（ADD COLUMN）

- **MySQL / PostgreSQL**

```sql
ALTER TABLE 表名 ADD COLUMN 列名 VARCHAR(100);
```

- **SQL Server**

```sql
ALTER TABLE 表名 ADD 列名 NVARCHAR(100);
```

- **Oracle**

```sql
ALTER TABLE 表名 ADD (列名 VARCHAR2(100));
```

---

### 修改列名（RENAME COLUMN / CHANGE COLUMN）

- **MySQL**
  
```sql
ALTER TABLE 表名 CHANGE COLUMN 旧列名 新列名 VARCHAR(100);
```

- **SQL Server**

```sql
EXEC sp_rename '表名.旧列名', '新列名', 'COLUMN';
```

- **PostgreSQL / Oracle**
  
```sql
ALTER TABLE 表名 RENAME COLUMN 旧列名 TO 新列名;
```

---

### 修改数据类型（MODIFY / ALTER TYPE）

- **MySQL**

```sql
ALTER TABLE 表名 MODIFY COLUMN 列名 FLOAT;
```

- **SQL Server**
  
```sql
ALTER TABLE 表名 ALTER COLUMN 列名 FLOAT;
```

- **PostgreSQL**

```sql
ALTER TABLE 表名 ALTER COLUMN 列名 TYPE FLOAT;
```

- **Oracle**

```sql
ALTER TABLE 表名 MODIFY (列名 FLOAT);
```

---

### 删除列（DROP COLUMN）

- **MySQL/SqlServe/PostgreSQL/Oracle**

```sql
ALTER TABLE 表名 DROP COLUMN 列名;
```

---

### 删除表（DROP TABLE）

```sql
DROP TABLE 表名;
```

⚠️ 删除表后 **所有数据不可恢复**，操作前请备份。

---

### 删除数据(TRUNCATE TABLE)

```sql
TRUNCATE TABLE 表名;
```

---

## 索引（INDEX）  

索引用于 **加快查询速度**，类似于书本的“目录”。
但索引过多会增加写入、更新成本，因此要合理设计。

---

### 添加索引（CREATE INDEX）

- **MySQL/SQL Server/PostgreSql/Oracle**

```sql
-- 创建普通索引
CREATE INDEX 索引名 ON 表名(列名);

-- 创建唯一索引
CREATE UNIQUE INDEX 索引名 ON 表名(列名);

```

---

### 修改索引名（RENAME）

- **MySQL**

```sql
-- 修改索引名称（MySQL 8.0+）
ALTER TABLE 表名 RENAME INDEX 旧索引名 TO 新索引名;
```

- **SQL Server**
  
```sql
-- 重命名索引
EXEC sp_rename '表名.旧索引名', '新索引名', 'INDEX';
```

- **PostgreSQL**

```sql
-- 修改索引名称
ALTER INDEX 旧索引名 RENAME TO 新索引名;
```

- **Oracle**

```sql
-- 修改索引名称
ALTER INDEX 旧索引名 RENAME TO 新索引名;
```

---

### 删除索引（DROP INDEX）

- **MySQL**
  
```sql
-- 删除索引
DROP INDEX 索引名 ON 表名;
```

- **SQL Server**

```sql
-- 删除索引
DROP INDEX 索引名 ON 表名;
```

- **PostgreSQL**

```sql
-- 删除索引
DROP INDEX 索引名;
```

- **Oracle**

```sql
-- 删除索引
DROP INDEX 索引名;
```

---

### 总结对比

| 操作类型 | MySQL | SQL Server | PostgreSQL | Oracle |
|-----------|--------|-------------|--------------|----------|
| 添加索引 | `CREATE [UNIQUE] INDEX ... ON 表名(列)` | 同上 | 同上 | 同上 |
| 修改索引名 | `ALTER TABLE RENAME INDEX` | `sp_rename` | `ALTER INDEX RENAME TO` | `ALTER INDEX RENAME TO` |
| 删除索引 | `DROP INDEX 索引名 ON 表名` | `DROP INDEX 索引名 ON 表名` | `DROP INDEX 索引名` | `DROP INDEX 索引名` |

 **实战建议：**

- 查询频繁的字段（WHERE / JOIN / ORDER BY）可加索引。  
- 不建议为低选择性（重复率高）的字段建索引，如性别、是否删除等。  

---

## 视图（VIEW）  

视图是 **基于查询结果的虚拟表**，不存储真实数据。

```sql
-- 创建视图
CREATE VIEW high_列名_表名 AS
SELECT name, 列名 FROM 表名 WHERE 列名 > 90; -- 检索语句

-- 查询视图
SELECT * FROM high_列名_表名;

-- 更新视图
CREATE OR REPLACE VIEW high_列名_表名 AS
SELECT name, 列名 FROM 表名 WHERE 列名 > 80; -- 新检索语句

-- 删除视图
DROP VIEW high_列名_表名;
```

---

## 1.9 基本数据操作  

### 插入数据（INSERT）

```sql
INSERT INTO 表名 (name, age, 列名)
VALUES ('Tom', 18, 89.5);
```

---

### 查询数据（SELECT）

```sql
SELECT name, 列名 FROM 表名
WHERE 列名 >= 80
ORDER BY 列名 DESC;
```

---

### 更新数据（UPDATE）

```sql
UPDATE 表名 SET 列名 = 95 WHERE id = 1;
```

---

### 删除数据（DELETE）

```sql
DELETE FROM 表名 WHERE id = 3;
```

⚠️ **没有 WHERE 条件时会删除整张表！**

---

## 1.10 小结  

| 主题 | 关键命令 | 作用 |
|------|-----------|------|
| 建表与结构 | CREATE, ALTER, DROP | 定义或修改表结构 |
| 约束控制 | PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK | 保证数据有效性 |
| 数据操作 | INSERT, UPDATE, DELETE | 增删改数据 |
| 数据查询 | SELECT | 查询数据 |
| 辅助机制 | INDEX, VIEW, AUTO_INCREMENT | 提升性能与便利性 |

---

✅ **延伸阅读建议**：  
- 下一章节将讲解「DQL 高级查询（WHERE、GROUP BY、JOIN）」  
- 推荐使用 `A5M2` 或 `DBeaver` 实际执行上述 SQL，观察结果变化。
