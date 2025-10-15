# 索引（INDEX）  

索引用于 **加快查询速度**，类似于书本的“目录”。
但索引过多会增加写入、更新成本，因此要合理设计。

---

## 添加索引（CREATE INDEX）

- **MySQL/SQL Server/PostgreSql/Oracle**

```sql
-- 创建普通索引
CREATE INDEX 索引名 ON 表名(列名);

-- 创建唯一索引
CREATE UNIQUE INDEX 索引名 ON 表名(列名);

```

---

## 修改索引名（RENAME）

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

## 删除索引（DROP INDEX）

- **MySQL**
  
```sql
-- 删除索引
DROP INDEX 索引名 ON 表名;
或者
ALTER TABLE 表名 DROP INDEX 索引名
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

## 总结对比

| 操作类型 | MySQL | SQL Server | PostgreSQL | Oracle |
|-----------|--------|-------------|--------------|----------|
| 添加索引 | `CREATE [UNIQUE] INDEX ... ON 表名(列)` | 同上 | 同上 | 同上 |
| 修改索引名 | `ALTER TABLE RENAME INDEX` | `sp_rename` | `ALTER INDEX RENAME TO` | `ALTER INDEX RENAME TO` |
| 删除索引 | `DROP INDEX 索引名 ON 表名` | `DROP INDEX 索引名 ON 表名` | `DROP INDEX 索引名` | `DROP INDEX 索引名` |

 **实战建议：**

- 查询频繁的字段（WHERE / JOIN / ORDER BY）可加索引。
- 不建议为低选择性（重复率高）的字段建索引，如性别、是否删除等。

---
