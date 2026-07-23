# 第24章 前端联调、Fetch 与 CORS

## 本章成果

使用一个最小前端页面调用员工 API，能在浏览器 Network 中区分前端错误、CORS、认证、权限、输入和后端异常；正确配置允许来源，而不是关闭浏览器安全限制。

## 1. 联调前先固定契约

前后端至少确认 API 基础地址、路径、方法、字段、日期格式、分页结构、认证头、错误结构和环境。前端环境变量中的 API 地址不是秘密；真正的密钥不能打包进浏览器代码。

本地示例：

```text
前端：http://localhost:5173
后端：http://127.0.0.1:8000
API_BASE_URL=http://127.0.0.1:8000/api
```

协议、域名和端口任一不同就是不同 origin。浏览器同源策略限制页面读取跨源响应，CORS 是服务端声明允许哪些 origin 的机制，不是 Django 权限系统。

## 2. 安装并配置 `django-cors-headers`

```powershell
python -m pip install django-cors-headers
```

settings：

```python
INSTALLED_APPS = [
    "corsheaders",
    # ...
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    # ...
]

CORS_ALLOWED_ORIGINS = [
    "http://localhost:5173",
]
```

生产来源通过环境配置并使用实际 HTTPS 域名。不使用 `CORS_ALLOW_ALL_ORIGINS=True` 解决开发问题后直接上线。若使用 Cookie 凭据，还要单独处理允许凭据、CSRF 可信来源和 Cookie 属性；JWT Authorization 头与 Cookie Session 的风险模型不同。

## 3. 最小 Fetch 封装

```javascript
const API_BASE_URL = "http://127.0.0.1:8000/api";

export async function apiFetch(path, options = {}) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 10000);
  const token = sessionStorage.getItem("access_token");

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        Accept: "application/json",
        ...(options.body instanceof FormData
          ? {}
          : { "Content-Type": "application/json" }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });

    const contentType = response.headers.get("content-type") ?? "";
    const body = contentType.includes("application/json")
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      throw new ApiError(response.status, body);
    }
    return body;
  } finally {
    window.clearTimeout(timeoutId);
  }
}
```

`fetch()` 遇到404/500不会自动 reject，所以必须检查 `response.ok`。上传 `FormData` 时不手写 `Content-Type`，浏览器需要补 multipart boundary。示例的 sessionStorage 只用于课程观察；真实应用应由团队安全设计决定 token 存储和刷新方式。

## 4. 页面要有四类状态

```javascript
async function loadEmployees() {
  showLoading();
  try {
    const data = await apiFetch("/employees/");
    if (data.results.length === 0) {
      showEmpty("没有符合条件的员工");
    } else {
      renderEmployees(data.results);
    }
  } catch (error) {
    if (error.status === 401) redirectToLogin();
    else if (error.status === 403) showForbidden();
    else showError("员工列表读取失败，请稍后重试");
  }
}
```

加载、空数据、成功和失败必须分别处理。不要把服务端原始堆栈或内部错误直接显示给用户。DOM 输出使用 `textContent` 或框架默认转义，不把 API 字符串直接拼入 `innerHTML`。

## 5. 预检请求

跨源且带 Authorization、非简单方法或特定请求头时，浏览器可能先发送 OPTIONS 预检。预检失败时业务 View 可能根本没执行。Network 中先找 OPTIONS：

- origin 是否完全匹配。
- 允许的方法和请求头是否包含实际请求。
- 中间件顺序是否正确。
- 反向代理是否拦截 OPTIONS。

CORS 错误只发生在浏览器执行的跨源读取；curl 能成功不证明浏览器一定成功。

## 6. CSRF 与 CORS 不同

CORS 控制页面能否读取跨源响应；CSRF 防止浏览器自动携带 Cookie 凭据进行跨站写操作。JWT 放在 Authorization 头时通常不使用 Django Session CSRF 流程，但仍要防 XSS、token 泄露和错误来源配置。若项目使用 Cookie 保存认证凭据，则必须设计 CSRF token、SameSite 和可信来源。

## 7. 联调调查顺序

1. Console 是否是 JavaScript/DOM 错误。
2. Network 是否发出请求；方法、URL、参数、头、体是否符合契约。
3. 是否先有失败的 OPTIONS。
4. 响应是401、403、404、400还是500。
5. 后端日志与请求 ID 是否对应。
6. 同一请求用 HTTP 客户端能否复现。

## 现场任务

故障：前端新增员工返回400，但页面只显示“系统错误”。要求在 Network 找到字段错误，将400映射到对应输入项；401跳转登录、403显示无权限、500显示通用错误。提交请求/响应样例和前后端责任判断。

## 完成检查

- [ ] 能解释 origin、同源策略、CORS和预检。
- [ ] Fetch 检查状态码并处理超时与错误。
- [ ] FormData 不手写 multipart Content-Type。
- [ ] CORS、CSRF、认证和权限没有混淆。

下一章把过滤、排序、分页和 API 文档依赖组合成企业列表能力。
