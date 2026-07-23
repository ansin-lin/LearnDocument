# 第10章 前后端联调与 CORS

> 本章目标：理解前后端分离中的接口联调流程，掌握 CORS 的作用、配置方式和常见错误定位。

## 一、前后端联调是什么

前后端分离项目中：

```text
Vue / React 页面
-> 发送 HTTP 请求
-> FastAPI 接口
-> 数据库
-> 返回 JSON
-> 前端渲染页面
```

联调要确认：

- URL 是否正确
- HTTP 方法是否正确
- 请求参数是否正确
- Header 是否正确
- 响应结构是否符合约定
- 错误状态码是否能被前端处理

## 二、CORS 是什么

CORS 是浏览器的跨域访问控制机制。

例如：

| 前端地址 | 后端地址 | 是否跨域 |
| --- | --- | --- |
| `http://localhost:5173` | `http://localhost:8000` | 是 |
| `http://localhost:8000` | `http://localhost:8000` | 否 |

CORS 不是认证，也不是授权。它只决定浏览器是否允许前端 JavaScript 读取跨域响应。

## 三、配置 CORS

文件位置：

```text
app/main.py
```

```python
from fastapi import FastAPI  # 导入 FastAPI
from fastapi.middleware.cors import CORSMiddleware  # 导入 CORS 中间件

app = FastAPI()  # 创建应用对象

app.add_middleware(  # 添加 CORS 中间件
    CORSMiddleware,  # 指定中间件类
    allow_origins=["http://localhost:5173"],  # 允许访问 API 的前端地址
    allow_credentials=True,  # 是否允许携带 Cookie 或认证信息
    allow_methods=["*"],  # 允许的 HTTP 方法
    allow_headers=["*"],  # 允许的请求头
)
```

生产环境不要随意使用 `allow_origins=["*"]` 配合认证信息。

## 四、前端请求示例

Fetch 示例：

```javascript
const response = await fetch("http://127.0.0.1:8000/employees"); // 发送 GET 请求
const employees = await response.json(); // 把响应 JSON 转成对象
console.log(employees); // 输出员工列表
```

Axios 示例：

```javascript
import axios from "axios"; // 导入 axios

const response = await axios.get("http://127.0.0.1:8000/employees"); // 发送 GET 请求
console.log(response.data); // 输出响应数据
```

## 五、携带 Token

```javascript
const token = localStorage.getItem("access_token"); // 从 localStorage 读取 Token

const response = await fetch("http://127.0.0.1:8000/employees", { // 发送员工列表请求
  headers: { // 设置请求头
    Authorization: `Bearer ${token}`, // 携带 Bearer Token
  }, // 请求头结束
}); // 请求结束
```

注意：前端保存 Token 不是绝对安全，真实项目需要结合安全策略。

## 六、常见联调问题

| 现象 | 常见原因 | 处理方式 |
| --- | --- | --- |
| 浏览器提示 CORS | 后端没有允许前端域名 | 检查 `allow_origins` |
| 404 | URL 路径错误 | 对照 `/docs` |
| 405 | HTTP 方法错误 | 检查 GET/POST/PUT/DELETE |
| 422 | 参数校验失败 | 查看响应 detail |
| 401 | 未认证 | 检查 Token |
| 403 | 无权限 | 检查角色或权限 |
| 500 | 后端异常 | 查看后端日志 |

## 七、基础练习

请完成：

1. 配置 CORS 允许 `http://localhost:5173`
2. 用浏览器访问 `/docs`
3. 用 Fetch 调用员工列表接口
4. 故意传错参数，观察 422
5. 故意不传 Token，观察 401

## 八、本章总结

- 前后端联调要检查 URL、方法、参数、Header 和响应
- CORS 是浏览器跨域机制，不是权限控制
- FastAPI 使用 `CORSMiddleware` 配置 CORS
- 认证接口需要前端携带 `Authorization: Bearer <token>`
