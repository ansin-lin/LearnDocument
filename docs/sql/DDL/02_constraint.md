# 约束（CONSTRAINT）  

约束用于 **保证数据的正确性与一致性**

| 约束类型 | 说明 | 示例 |
|-----------|------|------|
| **PRIMARY KEY** | 主键，唯一标识一条记录 | `PRIMARY KEY (id)` |
| **UNIQUE** | 唯一约束，防止重复 | `UNIQUE (列名)` |
| **NOT NULL** | 不允许为空 | `name VARCHAR(30) NOT NULL` |
| **DEFAULT** | 默认值 | `status VARCHAR(10) DEFAULT 'active'` |
| **CHECK** | 检查条件（部分数据库支持） | `CHECK (age >= 0)` |
| **FOREIGN KEY** | 外键，建立表之间的关系 | `FOREIGN KEY (dept_id) REFERENCES department(id)` |

---

## 创建表时直接定义约束（CREATE TABLE）

> 在创建表时，可以直接定义各类约束，无需使用 ALTER TABLE。
> 在MySQL / PostgreSQL / SqlServe / Oracle中语法相同

---

### 1️⃣ PRIMARY KEY（主键）

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

### 2️⃣ UNIQUE（唯一约束）

- **普通定义**

```sql
    CREATE TABLE 表名 (
        name VARCHAR(100) UNIQUE
    );
```

- **命名定义**

```sql
    CREATE TABLE 表名 (
        name VARCHAR(100),
        CONSTRAINT uq_列名 UNIQUE (列名)
    );
```

---

### 3️⃣ NOT NULL（非空约束）

> NOT NULL 作为列属性定义，不支持命名。

- 通用语法

```sql
CREATE TABLE 表名 (
    name VARCHAR(50) NOT NULL,
    age INT
);
```

---

### 4️⃣ DEFAULT（默认值）

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

### 5️⃣ CHECK（检查约束）

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

### 6️⃣ FOREIGN KEY（外键）

- **普通定义**

```sql
    CREATE TABLE 表名 (
        id INT PRIMARY KEY,
        dept_id INT,
        FOREIGN KEY (dept_id) REFERENCES 其他表(主键列)
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

MySQL / Oracle / SQL Server / PostgreSQL

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

MySQL / Oracle / SQL Server / PostgreSQL

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

MySQL / Oracle

- **普通语法**

```sql
    ALTER TABLE 表名 MODIFY 列名 数据类型 NOT NULL;
```

SQL Server / PostgreSQL

- **普通语法**

```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 数据类型 NOT NULL;
```

---

#### 4. DEFAULT（默认值）

MySQL

- **普通语法**

```sql
    ALTER TABLE 表名 MODIFY 列名 数据类型 DEFAULT 'active';
    或
    ALTER TABLE 表名 ALTER COLUMN 列名 SET DEFAULT 默认值;
```

- **命名语法**
    - 不支持命名 DEFAULT。

SQL Server

- **普通语法**

```sql
    ALTER TABLE 表名 ADD DEFAULT 默认值 FOR 列名;
```

- **命名语法**

```sql
    ALTER TABLE 表名 ADD CONSTRAINT 约束名 DEFAULT 默认值 FOR 列名;
```

PostgreSQL

- **普通语法**

```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 SET DEFAULT 默认值;
```

- **命名语法**
    - 不支持命名 DEFAULT。

Oracle

- **普通语法**

```sql
    ALTER TABLE 表名 MODIFY 列名 DEFAULT 默认值;
```

- **命名语法**
    - 不支持命名 DEFAULT（作为列属性保存）。

---

#### 5. CHECK（检查约束）

MySQL /SQL Server/PostGreSql / Oracle

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

MySQL / SQL Server / PostGreSql / Oracle

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
MySQL / Oracle：
  
```sql
    ALTER TABLE 表名 MODIFY 列名 新数据类型 [NOT NULL | NULL];
```

SQL Server：
  
```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 新数据类型 [NOT NULL | NULL];
```

PostgreSQL：

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

MySQL

- **普通语法**

```sql
    ALTER TABLE 表名 DROP PRIMARY KEY;
```

SQL Server/PostgreSQL/Oracle

- **普通 / 命名语法**

```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
```

---

#### UNIQUE

mysql

- **普通 / 命名语法**

```sql
    ALTER TABLE 表名 DROP INDEX 约束名;
```

Oracle/SQL Server/PostgreSQL

- **普通 / 命名语法**

```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
```

---

#### NOT NULL

mysql/Oracle

- **普通语法**

```sql
    ALTER TABLE 表名 MODIFY 列名 数据类型 NULL;
```

SQL Server/PostgreSQL

- **普通语法**

```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 数据类型 NULL;
```

---

#### DEFAULT

MySQL/PostgreSQL

- **普通语法**

```sql
    ALTER TABLE 表名 ALTER COLUMN 列名 DROP DEFAULT;
```

SQLServer

- **普通 / 命名语法**

```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
```

oracle

- **普通语法**

```sql
    ALTER TABLE 表名 MODIFY 列名 DEFAULT NULL;  -- 通过设为 NULL 清除默认值
```

---

#### CHECK

MySQL / SQLServer / PostgreSQL / Oracle

- **普通 / 命名语法**

```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
```

---

#### FOREIGN KEY

mysql

- **普通 / 命名语法**

```sql
    ALTER TABLE 表名 DROP FOREIGN KEY 约束名;
```

SQL Server / PostgreSQL / Oracle

- **普通 / 命名语法**

```sql
    ALTER TABLE 表名 DROP CONSTRAINT 约束名;
```

---
