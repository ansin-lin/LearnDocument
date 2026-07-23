# 第11章 PyMySQL 数据库连接基础

> 本章目标：掌握 Python 使用 `PyMySQL` 连接 MySQL 的基本方式，能够完成查询、插入、更新、删除、事务提交、回滚和基础封装。

## 一、学习目标

- 知道 `PyMySQL` 是什么，为什么 Python 项目要学习数据库驱动
- 能安装并使用 `PyMySQL` 连接 MySQL
- 能使用连接对象和游标对象执行 SQL
- 能完成查询、插入、更新、删除操作
- 能理解 `commit()`、`rollback()`、异常处理和资源关闭
- 能写出一个简单可复用的数据库访问工具函数

## 二、为什么先学习 PyMySQL

在 Web 项目里，后面我们会继续学习 `SQLAlchemy`、Django ORM 这类更上层的工具。

但是对于新人来说，如果一开始只看到 ORM，很容易只会“调用方法”，却不知道底层到底发生了什么。

`PyMySQL` 这一章的作用就是先把最基本的数据库访问过程讲清楚：

1. Python 程序如何连接 MySQL
2. 如何把 SQL 发送给数据库
3. 数据库返回的结果是什么
4. 为什么插入和更新后要提交事务
5. 程序报错时为什么要回滚
6. 为什么最后一定要关闭连接和游标

学完这一章，再进入 `SQLAlchemy` 和 Web 框架，会更容易理解。

## 三、PyMySQL 是什么

`PyMySQL` 是一个 Python 连接 MySQL 数据库的驱动库。

可以把它理解成：

- Python 程序和 MySQL 之间的“翻译器”
- 负责建立连接、发送 SQL、接收结果
- 让 Python 可以直接操作数据库

它本身不是 ORM。

也就是说：

- `PyMySQL` 负责“执行 SQL”
- 不负责把数据库表自动映射成 Python 类

这也是它适合作为入门章节的原因。

## 四、安装 PyMySQL

安装命令：

```bash
python -m pip install pymysql
```

确认是否安装成功：

```bash
python -m pip show pymysql
```

也可以直接在 Python 中测试导入：

```python
import pymysql  # 导入 PyMySQL

print(pymysql.__version__)  # 例如：1.1.1
```

## 五、连接 MySQL 之前要准备什么

在使用 `PyMySQL` 之前，需要先确认：

- MySQL 服务已经启动
- 已经有数据库账号和密码
- 知道主机地址、端口、数据库名
- 数据库中已经存在需要访问的表

常见连接信息如下：

| 参数 | 作用 | 常见示例 |
| --- | --- | --- |
| `host` | 数据库主机地址 | `127.0.0.1` |
| `port` | 数据库端口 | `3306` |
| `user` | 用户名 | `root` |
| `password` | 密码 | `root123` |
| `database` | 数据库名 | `employee_management` |
| `charset` | 字符集 | `utf8mb4` |

企业项目里通常不会把这些值直接写死在代码里，后面会放到配置文件或环境变量中。

## 六、先看最基本的连接代码

```python
import pymysql  # 导入 PyMySQL

connection = pymysql.connect(  # 创建数据库连接
    host="127.0.0.1",  # MySQL 主机地址
    port=3306,  # MySQL 端口
    user="root",  # 数据库用户名
    password="root123",  # 数据库密码
    database="employee_management",  # 要连接的数据库名
    charset="utf8mb4",  # 使用 utf8mb4 处理中文和表情等字符
)

print(connection)  # <pymysql.connections.Connection ...>

connection.close()  # 关闭数据库连接
```

### 6.1 这段代码做了什么

1. 导入 `pymysql`
2. 调用 `pymysql.connect()` 创建连接
3. 把连接信息传给 MySQL
4. 如果账号密码正确、数据库可访问，就得到一个连接对象
5. 使用完成后调用 `close()` 关闭连接

### 6.2 `connect()` 常用参数

| 参数 | 作用 | 是否常用 |
| --- | --- | --- |
| `host` | MySQL 服务器地址 | 很常用 |
| `port` | MySQL 端口 | 很常用 |
| `user` | 数据库用户名 | 很常用 |
| `password` | 数据库密码 | 很常用 |
| `database` | 默认连接的数据库 | 很常用 |
| `charset` | 字符编码 | 很常用 |
| `cursorclass` | 指定结果集返回形式 | 很常用 |
| `connect_timeout` | 连接超时时间 | 项目中常用 |
| `autocommit` | 是否自动提交事务 | 项目中常用 |

## 七、游标是什么

连接对象建立成功之后，还不能直接拿来执行 SQL。

通常还需要先创建“游标对象”。

游标可以理解成：

- 当前连接上的一个操作窗口
- 通过它发送 SQL
- 通过它读取查询结果

基本写法：

```python
import pymysql  # 导入 PyMySQL

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机地址
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 字符编码
)

cursor = connection.cursor()  # 创建游标对象

print(cursor)  # <pymysql.cursors.Cursor ...>

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

## 八、查询数据的完整示例

先看一个最常见的查询示例：

```python
import pymysql  # 导入 PyMySQL

connection = pymysql.connect(  # 创建数据库连接
    host="127.0.0.1",  # 主机地址
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 字符编码
)

cursor = connection.cursor()  # 创建游标

sql = "SELECT employee_id, employee_name, department_name FROM employees"  # 查询 SQL
cursor.execute(sql)  # 执行 SQL

rows = cursor.fetchall()  # 读取全部结果
print(rows)  # 例如：(('E001', 'Tanaka', 'Sales'), ('E002', 'Suzuki', 'IT'))

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

### 8.1 `execute()` 的作用

`execute()` 用来把 SQL 语句发送给数据库执行。

返回值通常是“影响行数”或“查询结果行数”，例如：

```python
count = cursor.execute("SELECT employee_id FROM employees")  # 执行查询
print(count)  # 例如：2
```

### 8.2 `fetchall()`、`fetchone()`、`fetchmany()`

| 方法 | 作用 | 返回值特点 |
| --- | --- | --- |
| `fetchone()` | 读取一行 | 返回一条记录或 `None` |
| `fetchmany(size)` | 读取多行 | 返回列表或元组集合 |
| `fetchall()` | 读取全部结果 | 返回全部记录 |

示例：

```python
import pymysql  # 导入 PyMySQL

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 编码
)

cursor = connection.cursor()  # 创建游标
cursor.execute("SELECT employee_id, employee_name FROM employees")  # 执行查询

first_row = cursor.fetchone()  # 读取第一行
print(first_row)  # 例如：('E001', 'Tanaka')

remaining_rows = cursor.fetchall()  # 读取剩余全部结果
print(remaining_rows)  # 例如：[('E002', 'Suzuki')]

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

## 九、让查询结果返回字典

默认情况下，查询结果通常是元组。

例如：

```python
('E001', 'Tanaka', 'Sales')
```

这种形式能用，但对于新人来说，可读性一般。

如果想让结果按字段名返回，可以使用 `DictCursor`：

```python
import pymysql  # 导入 PyMySQL
from pymysql.cursors import DictCursor  # 导入字典游标

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 编码
    cursorclass=DictCursor,  # 让结果按字典返回
)

cursor = connection.cursor()  # 创建游标
cursor.execute(  # 执行查询
    "SELECT employee_id, employee_name, department_name FROM employees"
)

rows = cursor.fetchall()  # 获取全部结果
print(rows)  # 例如：[{'employee_id': 'E001', 'employee_name': 'Tanaka', 'department_name': 'Sales'}]

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

企业项目中，字典结果通常更方便后续组装 JSON 或业务对象。

## 十、参数化查询

不要把用户输入直接拼进 SQL 字符串中。

错误示例：

```python
employee_id = "E001"  # 假设来自外部输入
sql = f"SELECT employee_id, employee_name FROM employees WHERE employee_id = '{employee_id}'"  # 不推荐
```

这种写法有 SQL 注入风险。

正确做法是使用参数化查询：

```python
import pymysql  # 导入 PyMySQL
from pymysql.cursors import DictCursor  # 导入字典游标

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 编码
    cursorclass=DictCursor,  # 结果转字典
)

cursor = connection.cursor()  # 创建游标

sql = """
SELECT employee_id, employee_name, department_name
FROM employees
WHERE employee_id = %s
"""  # 参数化查询 SQL

cursor.execute(sql, ("E001",))  # 把参数单独传入
row = cursor.fetchone()  # 读取一条结果
print(row)  # 例如：{'employee_id': 'E001', 'employee_name': 'Tanaka', 'department_name': 'Sales'}

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

### 10.1 为什么这里使用 `%s`

在 `PyMySQL` 中，参数占位符统一使用 `%s`。

即使参数实际是：

- 字符串
- 整数
- 日期

也仍然写 `%s`。

## 十一、插入数据

```python
import pymysql  # 导入 PyMySQL

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 编码
)

cursor = connection.cursor()  # 创建游标

sql = """
INSERT INTO employees (
    employee_id,
    employee_name,
    department_name
) VALUES (%s, %s, %s)
"""  # 插入 SQL

cursor.execute(sql, ("E003", "Sato", "HR"))  # 执行插入
connection.commit()  # 提交事务，让插入真正生效

print("insert completed")  # insert completed

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

### 11.1 为什么插入后要 `commit()`

`INSERT`、`UPDATE`、`DELETE` 这类操作会修改数据库中的数据。

默认情况下，执行 SQL 并不等于“永久写入”。

通常还需要：

```python
connection.commit()
```

这样数据库才会真正保存这次变更。

## 十二、更新和删除

更新示例：

```python
import pymysql  # 导入 PyMySQL

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 编码
)

cursor = connection.cursor()  # 创建游标

sql = """
UPDATE employees
SET department_name = %s
WHERE employee_id = %s
"""  # 更新 SQL

affected_rows = cursor.execute(sql, ("Finance", "E003"))  # 执行更新
connection.commit()  # 提交事务

print(affected_rows)  # 1

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

删除示例：

```python
import pymysql  # 导入 PyMySQL

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 编码
)

cursor = connection.cursor()  # 创建游标

sql = "DELETE FROM employees WHERE employee_id = %s"  # 删除 SQL
affected_rows = cursor.execute(sql, ("E003",))  # 执行删除
connection.commit()  # 提交事务

print(affected_rows)  # 1

cursor.close()  # 关闭游标
connection.close()  # 关闭连接
```

## 十三、事务、回滚和异常处理

项目里最常见的问题不是“SQL 不会写”，而是：

- 执行到一半报错了怎么办
- 前面已经改了一部分数据怎么办
- 连接和游标忘记关闭怎么办

所以实际开发中，必须把异常处理和事务控制一起写。

```python
import pymysql  # 导入 PyMySQL

connection = None  # 先定义连接变量，避免 finally 中未定义
cursor = None  # 先定义游标变量

try:
    connection = pymysql.connect(  # 创建连接
        host="127.0.0.1",  # 主机
        port=3306,  # 端口
        user="root",  # 用户名
        password="root123",  # 密码
        database="employee_management",  # 数据库名
        charset="utf8mb4",  # 编码
    )

    cursor = connection.cursor()  # 创建游标

    insert_sql = """
    INSERT INTO employees (employee_id, employee_name, department_name)
    VALUES (%s, %s, %s)
    """  # 插入 SQL

    cursor.execute(insert_sql, ("E004", "Yamada", "Sales"))  # 执行插入
    connection.commit()  # 提交事务

    print("transaction success")  # transaction success

except pymysql.MySQLError as error:  # 捕获数据库相关异常
    if connection is not None:  # 确认连接已创建
        connection.rollback()  # 发生异常时回滚事务

    print(error)  # 例如：(1062, "Duplicate entry 'E004' for key 'PRIMARY'")

finally:
    if cursor is not None:  # 游标存在时关闭
        cursor.close()  # 关闭游标

    if connection is not None:  # 连接存在时关闭
        connection.close()  # 关闭连接
```

### 13.1 `rollback()` 的作用

`rollback()` 表示回滚事务。

意思是：

- 本次事务里已经做但还没有最终确认的修改
- 全部撤销

这在以下场景很重要：

- 新增员工成功了，但后续写日志失败
- 订单主表插入成功，但明细表插入失败
- 一组更新只完成了一部分

如果不回滚，数据库可能会留下“不完整的数据”。

## 十四、使用 `with connection.cursor()` 简化游标管理

`PyMySQL` 常见写法里，也会把游标放进 `with` 中管理。

```python
import pymysql  # 导入 PyMySQL
from pymysql.cursors import DictCursor  # 导入字典游标

connection = pymysql.connect(  # 创建连接
    host="127.0.0.1",  # 主机
    port=3306,  # 端口
    user="root",  # 用户名
    password="root123",  # 密码
    database="employee_management",  # 数据库名
    charset="utf8mb4",  # 编码
    cursorclass=DictCursor,  # 使用字典游标
)

with connection.cursor() as cursor:  # 进入游标上下文
    cursor.execute(  # 执行查询
        "SELECT employee_id, employee_name FROM employees WHERE department_name = %s",
        ("Sales",),
    )
    rows = cursor.fetchall()  # 读取结果
    print(rows)  # 例如：[{'employee_id': 'E001', 'employee_name': 'Tanaka'}]

connection.close()  # 关闭连接
```

这种写法的好处是：

- 游标作用域更清楚
- 退出 `with` 后游标会自动关闭
- 比手动 `cursor.close()` 更稳定

## 十五、封装一个简单的查询函数

如果每次查询都重复写连接代码，项目会很快变乱。

所以至少应该学会做一层简单封装。

```python
import pymysql  # 导入 PyMySQL
from pymysql.cursors import DictCursor  # 导入字典游标


def get_connection() -> pymysql.Connection:
    return pymysql.connect(  # 返回一个新的数据库连接
        host="127.0.0.1",  # 主机
        port=3306,  # 端口
        user="root",  # 用户名
        password="root123",  # 密码
        database="employee_management",  # 数据库名
        charset="utf8mb4",  # 编码
        cursorclass=DictCursor,  # 查询结果按字典返回
    )


def find_employee_by_id(employee_id: str) -> dict[str, object] | None:
    connection = get_connection()  # 创建连接

    try:
        with connection.cursor() as cursor:  # 创建并管理游标
            sql = """
            SELECT employee_id, employee_name, department_name
            FROM employees
            WHERE employee_id = %s
            """  # 查询 SQL

            cursor.execute(sql, (employee_id,))  # 执行参数化查询
            row = cursor.fetchone()  # 获取一条记录
            return row  # 返回员工数据或 None
    finally:
        connection.close()  # 无论成功失败都关闭连接


employee = find_employee_by_id("E001")  # 查询员工
print(employee)  # 例如：{'employee_id': 'E001', 'employee_name': 'Tanaka', 'department_name': 'Sales'}
```

### 15.1 这段封装的意义

- 把连接配置集中管理
- 业务代码不需要重复写一大段连接参数
- 后面切换到 `SQLAlchemy` 时，更容易理解“数据访问层”的概念

## 十六、项目里常见的进一步改进

学完本章基础后，企业项目中通常还会继续做这些改进：

1. 连接信息放入环境变量或配置文件
2. 统一封装数据库连接函数
3. 统一封装查询、插入、更新、删除操作
4. 日志记录 SQL 执行失败原因
5. 使用连接池减少重复建连开销
6. 在 Web 框架中把数据库访问放到专门的数据访问层
7. 大型项目逐步切换到 ORM 或 Repository 模式

本章先把最基础、最直接的数据库访问流程学会即可。

## 十七、常见错误

### 17.1 忘记提交事务

错误现象：

- 程序执行成功
- 但数据库里看不到新增或修改结果

常见原因：

- 执行了 `INSERT`、`UPDATE`、`DELETE`
- 但没有调用 `connection.commit()`

### 17.2 直接拼接 SQL

错误写法：

```python
employee_id = "E001"
sql = f"SELECT * FROM employees WHERE employee_id = '{employee_id}'"
```

问题：

- 可读性差
- 容易写错引号
- 有 SQL 注入风险

应改成参数化查询。

### 17.3 只关连接，不关游标

错误现象：

- 代码能跑
- 但资源管理不规范

推荐做法：

- 使用 `with connection.cursor() as cursor`
- 或手动关闭 `cursor.close()`

### 17.4 连接信息硬编码到多个文件

错误现象：

- 多个模块都写了 `host`、`user`、`password`
- 修改数据库信息时要全项目查找替换

推荐做法：

- 集中封装连接配置

## 十八、不同数据库常见连接库

本章重点是 MySQL 和 `PyMySQL`。

如果后面需要连接其他数据库，常见工具名如下：

| 数据库 | Python 常见连接库 |
| --- | --- |
| MySQL | `pymysql`、`mysqlclient` |
| PostgreSQL | `psycopg`、`psycopg2` |
| Oracle | `oracledb` |
| SQLite | `sqlite3`（标准库内置） |
| SQL Server | `pyodbc`、`pymssql` |

这里先知道名称即可，不展开细讲。

## 十九、练习题

### 基础练习

1. 使用 `PyMySQL` 连接你本地的 MySQL 数据库，并输出连接对象。
2. 编写一个查询语句，读取员工表中的员工编号和员工姓名。
3. 使用参数化查询，按照员工编号读取一条员工数据。

### 综合练习

编写一个简单函数 `create_employee(employee_id, employee_name, department_name)`，要求：

- 使用 `PyMySQL` 连接数据库
- 插入一条员工记录
- 成功时提交事务
- 失败时回滚事务
- 最后关闭连接

## 二十、本章总结

| 知识点 | 作用 |
| --- | --- |
| `pymysql.connect()` | 创建 MySQL 连接 |
| `connection.cursor()` | 创建游标对象 |
| `cursor.execute()` | 执行 SQL |
| `fetchone()` / `fetchall()` | 读取查询结果 |
| `commit()` | 提交事务 |
| `rollback()` | 回滚事务 |
| `close()` | 关闭游标和连接 |
| 参数化查询 | 提高安全性和可维护性 |
| `DictCursor` | 让查询结果按字典返回 |

学完这一章后，已经具备进入 `SQLAlchemy` 和 Web 框架数据库访问章节的基础。
