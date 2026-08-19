# 第0章 前后端与请求响应基础

> 本章目标：理解 Web 系统中前端、后端、浏览器和服务器的关系，能够说明一次页面操作如何变成请求，又如何得到响应。

## 一、什么是 Web 系统

Web 系统是通过浏览器或客户端访问的应用程序。

常见例子：

- 公司员工管理系统
- 商品管理系统
- 订单管理系统
- 登录系统
- 报表查询系统

Web 系统通常不是一个单独的程序，而是由多个部分配合完成。

```text
用户
  ↓
浏览器
  ↓
前端页面
  ↓
HTTP 请求
  ↓
后端程序
  ↓
数据库
  ↓
HTTP 响应
  ↓
前端页面展示结果
```

在企业项目中，学员需要先理解这个整体流程。后续学习 Django、Spring Boot、Vue、React、FastAPI 或 Axios 时，本质上都离不开这个请求响应过程。

## 二、前端是做什么的

前端主要负责用户看得见、点得到、输入得了的部分。

常见职责：

- 显示页面
- 收集用户输入
- 校验简单输入格式
- 发送请求给后端
- 接收后端响应
- 把响应数据展示到页面上

常见技术：

| 技术 | 作用 |
| --- | --- |
| HTML | 定义页面结构 |
| CSS | 控制页面样式 |
| JavaScript | 控制页面行为 |
| TypeScript | 给 JavaScript 增加类型约束 |
| Vue | 前端框架，适合组件化开发 |
| React | 前端框架，适合组件化开发 |
| fetch | 浏览器内置的请求方法 |
| axios | 常用的前端请求库 |
| localStorage | 浏览器本地存储 |

前端不能直接相信自己的校验结果。比如页面上限制“年龄必须大于 0”，后端仍然要再次校验，因为用户可以绕过页面直接发送请求。

## 三、后端是做什么的

后端主要负责业务规则、数据处理和系统安全。

常见职责：

- 接收前端请求
- 判断请求是否合法
- 执行业务逻辑
- 读取或修改数据库
- 生成响应结果
- 处理异常和日志
- 控制登录状态和权限

常见技术：

| 技术 | 作用 |
| --- | --- |
| Java / Python | 编写后端业务代码 |
| Spring Boot / Django / FastAPI | 后端 Web 框架 |
| MyBatis / JPA / ORM | 操作数据库 |
| MySQL / PostgreSQL / Oracle | 保存业务数据 |
| Redis | 保存缓存或临时状态 |
| Nginx | 转发请求、静态资源服务 |

后端通常不负责页面样式，但必须负责业务结果是否正确。

## 四、请求和响应是什么

前端和后端之间通过 HTTP 通信。

- 请求：浏览器或前端程序发给后端的信息
- 响应：后端处理后返回给前端的信息

请求通常包含：

| 内容 | 说明 |
| --- | --- |
| URL | 请求地址 |
| Method | 请求方法，例如 `GET`、`POST` |
| Header | 请求附加信息，例如登录 Cookie、数据格式 |
| Query String | URL 后面的查询参数 |
| Request Body | 请求体，常用于提交 JSON 数据 |

响应通常包含：

| 内容 | 说明 |
| --- | --- |
| Status Code | 状态码，例如 `200`、`404`、`500` |
| Header | 响应附加信息 |
| Response Body | 响应体，通常是 HTML 或 JSON |

## 五、一次按钮点击发生了什么

以下是“查询员工信息”的典型过程：

```text
1. 用户在页面输入员工编号
2. 用户点击查询按钮
3. 前端读取输入框中的员工编号
4. 前端使用 fetch 或 axios 发送 HTTP 请求
5. 后端接收请求
6. 后端检查员工编号是否合法
7. 后端查询数据库
8. 后端把员工信息转换成响应数据
9. 前端接收响应
10. 前端把员工姓名、部门、邮箱显示到页面
```

这个流程中，前端和后端的职责不同：

| 步骤 | 主要负责 |
| --- | --- |
| 输入、点击、展示 | 前端 |
| 接收请求、校验、查询数据库 | 后端 |
| 保存员工数据 | 数据库 |
| 传输请求和响应 | HTTP |

## 六、使用 fetch 发送请求

`fetch` 是浏览器内置的网络请求方法，不需要额外安装。

### 6.1 GET 请求示例

```html
<button id="loadButton">查询员工</button>
<pre id="result"></pre>

<script>
  const loadButton = document.querySelector("#loadButton"); // 获取按钮元素
  const resultArea = document.querySelector("#result"); // 获取结果显示区域

  loadButton.addEventListener("click", async () => { // 监听按钮点击事件
    const response = await fetch("/api/employees/1001"); // 发送 GET 请求，返回 Response 对象
    const employee = await response.json(); // 把响应体解析成 JavaScript 对象

    resultArea.textContent = JSON.stringify(employee, null, 2); // 把对象格式化后显示到页面
  });
</script>
```

`fetch(url, options)` 的常用参数：

| 参数 | 可接受的值 | 作用 |
| --- | --- | --- |
| `url` | 字符串 | 请求地址 |
| `method` | `GET`、`POST`、`PUT`、`DELETE` 等 | 指定请求方法 |
| `headers` | 对象 | 设置请求头 |
| `body` | 字符串、`FormData` 等 | 设置请求体 |

`fetch()` 返回的是 `Promise<Response>`。实际数据通常需要再调用 `response.json()`、`response.text()` 等方法解析。

### 6.2 POST 请求示例

```javascript
const employee = { // 准备要发送给后端的数据
  name: "Tanaka", // 员工姓名
  department: "Sales" // 所属部门
};

const response = await fetch("/api/employees", { // 发送新增员工请求
  method: "POST", // 使用 POST 表示新增数据
  headers: {
    "Content-Type": "application/json" // 告诉后端请求体是 JSON
  },
  body: JSON.stringify(employee) // 把 JavaScript 对象转换成 JSON 字符串
});

const result = await response.json(); // 解析后端返回的 JSON
console.log(result); // 输出新增结果
```

## 七、使用 axios 发送请求

`axios` 是常用的前端请求库。它不是浏览器内置功能，需要在项目中安装或通过页面引入。

在前端工程项目中常见安装方式：

```bash
npm install axios
```

### 7.1 GET 请求示例

```javascript
import axios from "axios"; // 导入 axios 请求库

const response = await axios.get("/api/employees/1001"); // 发送 GET 请求
console.log(response.data); // axios 会把响应数据放在 data 属性中
```

`axios.get(url, config)` 的常用参数：

| 参数 | 可接受的值 | 作用 |
| --- | --- | --- |
| `url` | 字符串 | 请求地址 |
| `config` | 对象 | 设置请求头、查询参数、超时时间等 |

### 7.2 POST 请求示例

```javascript
import axios from "axios"; // 导入 axios 请求库

const employee = { // 准备新增员工数据
  name: "Suzuki", // 员工姓名
  department: "Development" // 所属部门
};

const response = await axios.post("/api/employees", employee); // 发送 POST 请求，axios 会自动处理 JSON
console.log(response.data); // 输出后端返回的数据
```

`axios.post(url, data, config)` 的常用参数：

| 参数 | 可接受的值 | 作用 |
| --- | --- | --- |
| `url` | 字符串 | 请求地址 |
| `data` | 对象、数组、字符串等 | 请求体数据 |
| `config` | 对象 | 设置请求头、超时时间等 |

## 八、fetch 和 axios 的区别

| 对比项 | fetch | axios |
| --- | --- | --- |
| 是否内置 | 浏览器内置 | 需要安装或引入 |
| JSON 解析 | 需要手动调用 `response.json()` | 默认把 JSON 放到 `response.data` |
| 请求拦截 | 需要自己封装 | 支持拦截器 |
| 超时设置 | 需要额外处理 | 支持 `timeout` 配置 |
| 企业项目常见度 | 常见 | 非常常见 |

新人阶段需要先理解：二者都是前端向后端发送 HTTP 请求的方式。

## 九、localStorage 是什么

`localStorage` 是浏览器提供的本地存储功能。

它的特点：

- 数据保存在浏览器中
- 刷新页面后数据仍然存在
- 关闭浏览器后数据通常仍然存在
- 只能保存字符串
- 不适合保存密码、密钥等敏感信息

常用方法：

| 方法 | 可接受的值 | 作用 |
| --- | --- | --- |
| `localStorage.setItem(key, value)` | `key` 和 `value` 都是字符串 | 保存数据 |
| `localStorage.getItem(key)` | 字符串 key | 读取数据，返回字符串或 `null` |
| `localStorage.removeItem(key)` | 字符串 key | 删除指定数据 |
| `localStorage.clear()` | 无参数 | 清空当前网站下的所有 localStorage 数据 |

示例：

```javascript
localStorage.setItem("theme", "dark"); // 保存页面主题设置

const theme = localStorage.getItem("theme"); // 读取页面主题设置
console.log(theme); // 输出：dark

localStorage.removeItem("theme"); // 删除页面主题设置

const removedTheme = localStorage.getItem("theme"); // 再次读取已经删除的数据
console.log(removedTheme); // 输出：null
```

企业项目中，`localStorage` 常用于保存页面设置、查询条件、表格显示配置等非敏感信息。敏感信息必须谨慎处理，不能因为保存在前端就认为安全。

## 十、常见错误

| 错误现象 | 常见原因 | 排查方向 |
| --- | --- | --- |
| 页面点击后没有反应 | 事件没有绑定成功 | 检查浏览器控制台 |
| 请求发送了但没有数据 | URL 或参数错误 | 检查 Network 面板中的请求地址 |
| 后端返回 404 | 接口地址不存在 | 检查前端 URL 和后端路由 |
| 后端返回 500 | 后端程序异常 | 查看后端日志 |
| localStorage 读取结果是 `null` | key 写错或没有保存过 | 检查 key 名称是否一致 |

## 十一、基础练习

请完成以下练习：

1. 画出“点击查询按钮后显示员工信息”的请求响应流程。
2. 说明前端和后端在这个流程中分别负责什么。
3. 写一个 `fetch` GET 请求示例，请求 `/api/employees/1001`。
4. 使用 `localStorage` 保存一个页面主题 `theme=light`，再读取并输出。

## 十二、本章总结

- 前端负责页面展示、用户操作和发送请求。
- 后端负责业务逻辑、数据处理、安全控制和响应结果。
- 请求是前端发给后端的信息，响应是后端返回给前端的信息。
- `fetch` 和 `axios` 都可以发送 HTTP 请求。
- `localStorage` 可以保存前端本地数据，但不能当作安全存储使用。
