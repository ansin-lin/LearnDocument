
# 修改表结构（ALTER TABLE）

在关系型数据库中，`ALTER TABLE` 语句用于修改已有表的结构，常见操作包括：添加列、修改列名、修改数据类型、删除列。

---

## 添加列（ADD COLUMN）

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

## 修改列名（RENAME COLUMN / CHANGE COLUMN）

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

## 修改数据类型（MODIFY / ALTER TYPE）

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

## 删除列（DROP COLUMN）

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

## 删除数据(TRUNCATE TABLE)

```sql
TRUNCATE TABLE 表名;
```

---
