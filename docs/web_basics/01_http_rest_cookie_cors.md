# 第1章 HTTP、REST、Cookie、Session 与 CORS

> 本章目标：掌握 Web 开发中最常用的 HTTP 请求响应知识，理解 REST 接口、Cookie、Session 和 CORS 的作用，并能看懂前端请求代码。

## 一、HTTP 是什么

HTTP 是浏览器和服务器之间传输数据的规则。

在 Web 系统中，前端不会直接调用后端函数，而是通过 HTTP 请求访问后端接口。

```text
浏览器或前端页面
  ↓ HTTP Request
后端接口
  ↓ HTTP Response
浏览器或前端页面
```

例如：

```text
GET /api/employees/1001
```

这表示前端请求后端查询员工编号为 `1001` 的员工信息。

## 二、URL 的组成

URL 是请求地址。

示例：

```text
https://example.com:443/api/employees?department=sales&page=1
```

组成说明：

| 部分 | 示例 | 说明 |
| --- | --- | --- |
| 协议 | `https` | 使用 HTTP 还是 HTTPS |
| 域名 | `example.com` | 服务器地址 |
| 端口 | `443` | 服务入口编号，HTTPS 默认是 443 |
| 路径 | `/api/employees` | 后端接口路径 |
| 查询参数 | `department=sales&page=1` | 过滤、分页等查询条件 |

前端请求接口时，最容易出错的是路径和查询参数。

## 三、HTTP 请求

HTTP 请求包含请求方法、请求地址、请求头和请求体。

### 3.1 请求方法

| 方法 | 常见用途 | 示例 |
| --- | --- | --- |
| `GET` | 查询数据 | 查询员工列表 |
| `POST` | 新增数据 | 新增员工 |
| `PUT` | 整体更新数据 | 更新员工完整信息 |
| `PATCH` | 局部更新数据 | 只修改员工邮箱 |
| `DELETE` | 删除数据 | 删除员工 |

请求方法不是强制业务规则，但企业项目中通常会按这个习惯设计接口。

### 3.2 请求头

请求头用于描述请求的附加信息。

常见请求头：

| 请求头 | 示例值 | 作用 |
| --- | --- | --- |
| `Content-Type` | `application/json` | 告诉后端请求体格式 |
| `Accept` | `application/json` | 告诉后端前端希望接收 JSON |
| `Cookie` | `SESSIONID=abc123` | 浏览器自动携带的 Cookie |

### 3.3 请求体

请求体用于提交较复杂的数据。新增、修改数据时经常使用请求体。

```json
{
  "name": "Tanaka",
  "department": "Sales"
}
```

这段 JSON 可以表示新增员工时提交的员工姓名和部门。

## 四、HTTP 响应

HTTP 响应包含状态码、响应头和响应体。

### 4.1 状态码

| 状态码 | 含义 | 常见场景 |
| --- | --- | --- |
| `200` | 成功 | 查询成功、更新成功 |
| `201` | 创建成功 | 新增员工成功 |
| `204` | 成功但没有响应体 | 删除成功 |
| `400` | 请求错误 | 参数格式不正确 |
| `401` | 未登录 | 没有登录或登录过期 |
| `403` | 无权限 | 已登录但没有操作权限 |
| `404` | 找不到资源 | URL 错误或数据不存在 |
| `500` | 服务器异常 | 后端程序出错 |

排查接口问题时，先看状态码，再看响应体和后端日志。

### 4.2 响应体

后端常用 JSON 返回数据。

```json
{
  "id": 1001,
  "name": "Tanaka",
  "department": "Sales"
}
```

前端拿到响应体后，再把数据展示到页面上。

## 五、JSON 是什么

JSON 是前后端传输数据时最常见的格式。

JavaScript 对象示例：

```javascript
const employee = { // 创建 JavaScript 对象
  id: 1001, // 员工编号
  name: "Tanaka" // 员工姓名
};
```

JSON 字符串示例：

```json
{
  "id": 1001,
  "name": "Tanaka"
}
```

前端发送请求时，经常需要把 JavaScript 对象转换成 JSON 字符串。

```javascript
const employee = { id: 1001, name: "Tanaka" }; // 创建 JavaScript 对象
const jsonText = JSON.stringify(employee); // 转换成 JSON 字符串
console.log(jsonText); // 输出：{"id":1001,"name":"Tanaka"}
```

前端接收响应时，经常需要把 JSON 字符串转换成 JavaScript 对象。

```javascript
const jsonText = '{"id":1001,"name":"Tanaka"}'; // 准备 JSON 字符串
const employee = JSON.parse(jsonText); // 转换成 JavaScript 对象
console.log(employee.name); // 输出：Tanaka
```

## 六、REST 是什么

REST 是一种常见的接口设计风格。

REST 的核心思想是：用 URL 表示资源，用 HTTP 方法表示操作。

以员工资源为例：

| 操作 | HTTP 方法 | URL | 说明 |
| --- | --- | --- | --- |
| 查询员工列表 | `GET` | `/api/employees` | 查询多个员工 |
| 查询单个员工 | `GET` | `/api/employees/1001` | 查询员工编号为 1001 的员工 |
| 新增员工 | `POST` | `/api/employees` | 创建一个员工 |
| 修改员工 | `PUT` | `/api/employees/1001` | 修改员工编号为 1001 的员工 |
| 删除员工 | `DELETE` | `/api/employees/1001` | 删除员工编号为 1001 的员工 |

不推荐的写法：

```text
/getEmployee?id=1001
/createEmployee
/deleteEmployee
```

更推荐的 REST 风格：

```text
GET /api/employees/1001
POST /api/employees
DELETE /api/employees/1001
```

REST 不是某个框架，也不等于 JSON。Django、Spring Boot、FastAPI、Vue、React 都可以使用 REST 风格接口。

## 七、使用 fetch 调用 REST 接口

### 7.1 查询员工

```javascript
const response = await fetch("/api/employees/1001", { // 调用查询员工接口
  method: "GET", // GET 表示查询
  headers: {
    "Accept": "application/json" // 告诉后端希望接收 JSON
  }
});

if (!response.ok) { // 判断 HTTP 状态码是否为 200 到 299
  throw new Error(`请求失败：${response.status}`); // 状态码异常时抛出错误
}

const employee = await response.json(); // 把响应体解析成 JavaScript 对象
console.log(employee.name); // 输出员工姓名，例如：Tanaka
```

### 7.2 新增员工

```javascript
const requestBody = { // 准备请求体数据
  name: "Suzuki", // 员工姓名
  department: "Development" // 所属部门
};

const response = await fetch("/api/employees", { // 调用新增员工接口
  method: "POST", // POST 表示新增
  headers: {
    "Content-Type": "application/json", // 告诉后端请求体是 JSON
    "Accept": "application/json" // 告诉后端希望接收 JSON
  },
  body: JSON.stringify(requestBody) // 把对象转换成 JSON 字符串作为请求体
});

const result = await response.json(); // 解析后端响应
console.log(result); // 输出新增后的员工数据或处理结果
```

## 八、使用 axios 调用 REST 接口

### 8.1 查询员工

```javascript
import axios from "axios"; // 导入 axios 请求库

const response = await axios.get("/api/employees/1001", { // 调用查询员工接口
  headers: {
    "Accept": "application/json" // 设置请求头
  }
});

console.log(response.data.name); // 输出员工姓名，例如：Tanaka
```

### 8.2 新增员工

```javascript
import axios from "axios"; // 导入 axios 请求库

const requestBody = { // 准备请求体数据
  name: "Suzuki", // 员工姓名
  department: "Development" // 所属部门
};

const response = await axios.post("/api/employees", requestBody, { // 调用新增员工接口
  headers: {
    "Content-Type": "application/json" // 指定请求体格式
  }
});

console.log(response.data); // 输出后端返回的数据
```

axios 常用返回属性：

| 属性 | 说明 |
| --- | --- |
| `response.data` | 后端返回的响应体数据 |
| `response.status` | HTTP 状态码 |
| `response.headers` | 响应头 |
| `response.config` | 本次请求的配置信息 |

## 九、Cookie 是什么

Cookie 是浏览器保存的一小段数据。

后端可以通过响应头让浏览器保存 Cookie。之后浏览器访问同一个网站时，会自动把 Cookie 带回后端。

```text
第一次登录：
后端响应 Set-Cookie: SESSIONID=abc123

之后请求：
浏览器自动发送 Cookie: SESSIONID=abc123
```

Cookie 常用于保存会话标识。Cookie 本身保存在浏览器端，所以不要把密码等敏感明文数据直接放进 Cookie。

## 十、Session 是什么

Session 是后端保存用户登录状态的一种方式。

常见流程：

```text
1. 用户提交账号和密码
2. 后端校验成功
3. 后端创建 Session 数据
4. 后端把 Session ID 放进 Cookie 返回给浏览器
5. 浏览器后续请求自动携带 Cookie
6. 后端根据 Session ID 找到用户登录状态
```

Cookie 和 Session 的关系：

| 项目 | Cookie | Session |
| --- | --- | --- |
| 保存位置 | 浏览器 | 后端 |
| 常见用途 | 保存 Session ID | 保存登录状态 |
| 是否自动随请求发送 | 是 | 否 |
| 安全重点 | 不保存敏感明文 | 控制过期时间和权限 |

## 十一、CORS 是什么

CORS 用来处理浏览器跨域请求限制。

浏览器会限制网页随意访问不同来源的接口。

来源由三部分决定：

```text
协议 + 域名 + 端口
```

例如：

| 前端地址 | 后端地址 | 是否同源 |
| --- | --- | --- |
| `http://localhost:3000` | `http://localhost:3000` | 是 |
| `http://localhost:3000` | `http://localhost:8000` | 否 |
| `http://localhost:3000` | `https://localhost:3000` | 否 |
| `http://localhost:3000` | `http://127.0.0.1:3000` | 否 |

当前后端分离开发时，前端开发服务器和后端服务器经常端口不同，所以容易出现 CORS 错误。

浏览器常见错误：

```text
Access to fetch at 'http://localhost:8000/api/employees'
from origin 'http://localhost:3000' has been blocked by CORS policy
```

这类错误不是 JavaScript 语法错误，而是浏览器根据安全规则拦截了跨域响应。通常需要后端明确允许指定前端来源。

## 十二、localStorage 在请求中的常见用法

`localStorage` 可以保存前端本地状态，例如页面主题、查询条件、上次选择的部门等。

示例：

```javascript
localStorage.setItem("selectedDepartment", "Sales"); // 保存用户选择的部门

const selectedDepartment = localStorage.getItem("selectedDepartment"); // 读取保存的部门

const response = await fetch(`/api/employees?department=${selectedDepartment}`, { // 把部门作为查询参数发送给后端
  method: "GET", // GET 表示查询
  headers: {
    "Accept": "application/json" // 希望接收 JSON
  }
});

const employees = await response.json(); // 解析员工列表
console.log(employees); // 输出员工列表
```

注意：`localStorage` 是前端存储，不是后端 Session，也不能代替数据库。

## 十三、常见错误与排查

| 错误现象 | 可能原因 | 排查方法 |
| --- | --- | --- |
| `404 Not Found` | URL 路径错误 | 检查前端请求地址和后端路由 |
| `400 Bad Request` | 参数格式错误 | 检查请求体和查询参数 |
| `401 Unauthorized` | 没有登录或登录过期 | 检查 Cookie 是否发送 |
| `403 Forbidden` | 没有权限 | 检查当前用户权限 |
| `500 Internal Server Error` | 后端程序异常 | 查看后端日志 |
| CORS 错误 | 前端和后端不同源 | 检查后端 CORS 配置 |
| `response.json()` 报错 | 响应体不是 JSON | 检查响应头和响应内容 |

## 十四、基础练习

请完成以下练习：

1. 写出查询员工列表的 REST 风格 URL 和 HTTP 方法。
2. 说明 `GET /api/employees?department=Sales` 中路径和查询参数分别是什么。
3. 说明 Cookie 和 Session 的区别。
4. 写一个 `fetch` POST 请求，向 `/api/employees` 提交员工姓名和部门。
5. 说明为什么 `http://localhost:3000` 请求 `http://localhost:8000` 可能出现 CORS 错误。

## 十五、综合练习

请根据下面需求设计请求：

需求：员工管理页面需要查询开发部门的第 2 页员工数据，每页 20 条。

要求：

- 使用 REST 风格 URL
- 指定 HTTP 方法
- 写出查询参数
- 说明可能的成功状态码
- 说明如果用户未登录可能返回什么状态码

参考形式：

```text
GET /api/employees?department=Development&page=2&pageSize=20
```

## 十六、本章总结

- HTTP 是前端和后端通信的基础。
- 请求包含方法、URL、请求头和请求体。
- 响应包含状态码、响应头和响应体。
- REST 用 URL 表示资源，用 HTTP 方法表示操作。
- Cookie 保存在浏览器，Session 状态保存在后端。
- CORS 是浏览器对跨域请求的安全限制。
- `fetch` 和 `axios` 都可以调用后端 REST 接口。
- `localStorage` 适合保存非敏感的前端本地数据。
