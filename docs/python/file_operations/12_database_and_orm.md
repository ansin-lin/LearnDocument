# 第12章 数据库与 ORM、对象序列化与反序列化

> 🎯 学习目标  
>
> - 掌握 SQLite 与 MySQL 的连接与操作  
> - 理解 ORM 概念与 SQLAlchemy 基础  
> - 熟悉对象序列化（pickle/json/yaml）在 Web 系统中的持久化应用

---

## 一、数据库模块

### 使用的模块

- **sqlite3**：内置轻量数据库
- **mysql.connector**：连接 MySQL
- **sqlalchemy**：ORM 框架

### SQLite 示例

```python
import sqlite3

conn = sqlite3.connect("test.db")
cur = conn.cursor()
cur.execute("CREATE TABLE IF NOT EXISTS users(id INTEGER, name TEXT)")
cur.execute("INSERT INTO users VALUES(1, 'Tom')")
conn.commit()
print(cur.execute("SELECT * FROM users").fetchall())
conn.close()
```

### MySQL 示例

```python
import mysql.connector
conn = mysql.connector.connect(host="localhost", user="root", password="123456", database="testdb")
cur = conn.cursor()
cur.execute("SELECT VERSION()")
print(cur.fetchone())
```

---

## 二、ORM（SQLAlchemy）

```python
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.orm import declarative_base, sessionmaker

engine = create_engine("sqlite:///orm.db")
Base = declarative_base()

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True)
    name = Column(String(50))

Base.metadata.create_all(engine)

Session = sessionmaker(bind=engine)
session = Session()
session.add(User(name="Alice"))
session.commit()
```

✅ **Web 应用场景：**  
Flask/FastAPI 后端数据库交互、用户管理、日志持久化。

---

## 三、对象序列化（pickle/json/yaml）

### pickle

```python
import pickle

data = {"a": 1, "b": [2, 3]}
with open("obj.pkl", "wb") as f:
    pickle.dump(data, f)
with open("obj.pkl", "rb") as f:
    obj = pickle.load(f)
```

### json

```python
import json
data = {"user": "Tom", "age": 22}
with open("user.json", "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
```

### yaml

```python
import yaml
config = {"debug": True, "port": 8080}
with open("config.yaml", "w") as f:
    yaml.dump(config, f)
```

---

## ✅ 小结

| 模块 | 功能 | 应用 |
|------|------|------|
| sqlite3 | 轻量数据库 | 本地缓存 |
| mysql.connector | 远程数据库 | 企业后端 |
| SQLAlchemy | ORM操作 | Flask/FastAPI |
| pickle/json/yaml | 对象序列化 | 缓存/配置 |
