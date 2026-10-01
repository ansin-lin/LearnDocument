# 第 20 章 文件、环境变量与安全

## 本章目标

- 完成上传与下载，并正确处理失败和资源释放。
- 使用 Vite 环境变量配置公开运行参数。
- 理解认证、权限与前端安全边界。

本章包含三个相对独立但常在企业项目中同时出现的主题：

```text
Part A：文件处理
Part B：环境变量与项目配置
Part C：前端安全基础
```

## Part A 文件上传与下载

### 1. 浏览器中的 File 与 FileList

```jsx
<input
  type="file"
  onChange={(event) => {
    console.log(event.target.files);
  }}
/>
```

用户选择文件后，浏览器不会直接给 React 一个文件路径字符串，而是通过 `files` 提供 `FileList`。单文件上传通常取得第一项：

```js
const file = event.target.files?.[0] ?? null;
```

`File` 常用属性：

| 属性 | 含义 | 示例 |
| --- | --- | --- |
| `name` | 原文件名 | `employees.csv` |
| `size` | 字节数 | `1048576` 表示 1 MiB |
| `type` | 浏览器提供的 MIME Type | `text/csv` |
| `lastModified` | 最后修改时间戳 | 用于显示或辅助判断 |

浏览器出于安全原因不会允许页面任意读取用户电脑文件。只有用户通过文件控件等方式明确选择后，页面才能取得对应的 `File` 对象。

### 2. FormData 是什么

JSON 适合发送文字、数字和普通对象；文件上传通常使用 `multipart/form-data`。`FormData` 用于构造这种请求：

```jsx
async function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  await httpClient.post('/employees/avatar', formData);
}
```

`FormData` 用于构造 `multipart/form-data` 请求。`append('file', file)` 的字段名必须与后台规格一致；浏览器会自动生成 multipart boundary。

不要手工设置 multipart boundary。Axios 和浏览器会根据 `FormData` 生成正确边界；手工写错后，Backend 可能无法解析文件。

### 3. 文件上传的完整流程

```text
用户选择文件
  ↓
前端检查文件名、类型和大小
  ↓
FormData.append('file', file)
  ↓
POST API
  ↓
uploading + progress
  ├─ success
  ├─ error → retry
  └─ cancel → retry
```

上传 UI 应展示文件名、进度或处理中状态、取消/重试入口。不要读取或记录文件内容到 Console。

#### 3.1 文件 Validation

```js
const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ALLOWED_TYPES = new Set(['image/png', 'image/jpeg']);

function validateFile(file) {
  if (!file) return '请选择文件';
  if (file.size > MAX_FILE_SIZE) return '文件不能超过 5 MiB';
  if (!ALLOWED_TYPES.has(file.type)) return '只允许 PNG 或 JPEG';
  return '';
}
```

前端可以检查文件名、扩展名、MIME Type 和 Size，但这些值不能构成安全保证。攻击者可以绕过前端，也可能伪装扩展名或类型。Backend 必须重新检查实际内容、大小、权限，并采用项目规定的恶意文件检测。

#### 3.2 Progress、Cancel、Error 与 Retry

下面是 `AvatarUploader` 组件内部片段。它复用第 10 章的 Axios 实例，并用 AbortController 取消当前上传：

```jsx
const [file, setFile] = useState(null);
const [status, setStatus] = useState('idle');
const [progress, setProgress] = useState(0);
const controllerRef = useRef(null);

async function startUpload(targetFile) {
  controllerRef.current?.abort();
  const controller = new AbortController();
  controllerRef.current = controller;

  const formData = new FormData();
  formData.append('file', targetFile);
  setStatus('uploading');
  setProgress(0);

  try {
    await httpClient.post('/employees/avatar', formData, {
      signal: controller.signal,
      onUploadProgress(event) {
        if (!event.total) return;
        setProgress(Math.round((event.loaded / event.total) * 100));
      },
    });
    if (!controller.signal.aborted) setStatus('success');
  } catch {
    if (controllerRef.current === controller) {
      setStatus(controller.signal.aborted ? 'canceled' : 'error');
    }
  } finally {
    if (controllerRef.current === controller) controllerRef.current = null;
  }
}

function cancelUpload() {
  controllerRef.current?.abort();
}

useEffect(() => {
  return () => {
    const controller = controllerRef.current;
    controllerRef.current = null;
    controller?.abort();
  };
}, []);

// JSX 片段
<input
  type="file"
  accept="image/png,image/jpeg"
  onChange={(event) => setFile(event.target.files?.[0] ?? null)}
/>
<button type="button" disabled={!file || status === 'uploading'}
  onClick={() => file && void startUpload(file)}>
  {status === 'error' || status === 'canceled' ? '重试' : '上传'}
</button>
<button type="button" disabled={status !== 'uploading'} onClick={cancelUpload}>
  取消
</button>
{status === 'uploading' && <progress max="100" value={progress}>{progress}%</progress>}
{status === 'canceled' && <p role="status">上传已取消，可以重试</p>}
{status === 'error' && <p role="alert">上传失败，请重试</p>}
```

进度计算：

```text
loaded = 已经上传的字节数
total  = 预计总字节数
percent = loaded / total × 100

例如：loaded=5 MiB、total=10 MiB
percent=50%
```

```text
选择文件 → idle
开始上传 → uploading + progress
           ├─ success
           ├─ cancel → canceled → retry
           └─ error  → error    → retry
```

上传进度依赖浏览器与传输环境，`event.total` 可能不存在，不能除以 `undefined`。取消不是错误 Toast；界面应明确显示“已取消”并允许使用同一文件重试。组件卸载时先清空当前 controller 引用再 abort，使请求结束后不会更新已卸载组件的状态。

同一文件上传完成后若需要再次选择，应清空文件 State 和文件控件：

```jsx
const fileInputRef = useRef(null);

function clearFile() {
  setFile(null);
  if (fileInputRef.current) fileInputRef.current.value = '';
}
```

#### 3.3 AbortController 三个对象的关系

```text
AbortController
├─ controller.signal → 交给 Axios 请求
└─ controller.abort() → 发出取消通知
                           ↓
                  signal 变为 aborted
                           ↓
                    Axios 取消请求
```

`signal` 本身不负责创建请求，它只是把取消状态传给支持 AbortSignal 的 API。同一 Controller 适合控制同一组需要一起取消的操作；新的独立上传通常创建新的 Controller。

### 4. 文件下载

```jsx
const response = await httpClient.get('/employees/export', {
  responseType: 'blob',
});
const url = URL.createObjectURL(response.data);
const link = document.createElement('a');
link.href = url;
link.download = 'employees.csv';
link.click();
URL.revokeObjectURL(url);
```

文件名应由可信规则生成；若读取响应头中的名称，要处理编码与不安全路径字符。大文件、流式下载与错误响应格式应按接口规格处理。

`responseType: 'blob'` 让 Axios 把响应作为二进制 Blob。`URL.createObjectURL()` 建立临时地址；下载后必须释放：

```jsx
async function downloadEmployees() {
  let objectUrl = '';

  try {
    const response = await httpClient.get('/employees/export', {
      responseType: 'blob',
    });
    objectUrl = URL.createObjectURL(response.data);

    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = 'employees.csv';
    document.body.append(link);
    link.click();
    link.remove();
  } finally {
    if (objectUrl) URL.revokeObjectURL(objectUrl);
  }
}
```

错误响应即使是 JSON，也可能因 `responseType` 被读取为 Blob。应先检查 Status 和 `Content-Type`，再按接口规格转换错误，不能把错误 JSON 当成 CSV 保存。

完整过程是：

```text
HTTP Response
  ↓ responseType: 'blob'
Blob
  ↓ URL.createObjectURL()
临时 object URL
  ↓ 赋给 a.href 并触发下载
浏览器保存文件
  ↓ URL.revokeObjectURL()
释放临时 URL 占用的资源
```

`revokeObjectURL()` 不会删除已经保存的文件，只是通知浏览器释放当前页面建立的临时 URL。长期不释放会造成不必要的内存占用。

## Part B 环境变量与项目配置

### 5. 为什么需要环境变量

不同环境的 API 地址通常不同：

```text
开发环境：http://localhost:8080/api
测试环境：https://test-api.example.com/api
生产环境：https://api.example.com/api
```

如果把地址散落写死在每个 Service，切换环境时必须修改源码。环境变量把“随环境变化的公开配置”从业务代码中分离出来。

### 6. Vite 环境变量

```dotenv
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api
```

```js
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
```

`.env`、`.env.development`、`.env.production` 用于不同构建模式。Vite 暴露到客户端的变量通常需要 `VITE_` 前缀；它们会出现在浏览器可下载的资源中，所以 API 密钥、数据库密码、私钥和真正秘密绝不能放进去。环境改变后通常要重启开发服务器。

Vite 在开发服务器启动或构建时读取这些值。共通配置模块应尽早检查必需变量：

```js
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

if (!apiBaseUrl) {
  throw new Error('VITE_API_BASE_URL 未设置');
}

export const appConfig = { apiBaseUrl };
```

`.env.example` 只提交变量名和安全示例值；个人环境文件按项目规则排除。是否提交都不改变一个事实：前端变量不能保存真正秘密。

#### 6.1 环境变量不是秘密

下面的写法是错误的：

```dotenv
VITE_DB_PASSWORD=123456
VITE_SECRET_KEY=do-not-write-here
```

```text
Frontend Environment Variable
  ↓ Vite Build
JavaScript 静态资源
  ↓ 下载
Browser / User
```

只要值需要在浏览器运行时使用，用户就有机会读取它。数据库密码、私钥和真正的第三方服务 Secret 必须保存在 Backend 或安全的服务端配置中。

## Part C 前端安全基础

### 7. Authentication 与 Authorization

```text
Authentication（认证）
→ 你是谁？

Authorization（授权）
→ 你能做什么？
```

Employee 系统中，登录确认当前用户身份属于 Authentication；确认 `USER` 是否允许删除员工属于 Authorization。

```jsx
{currentUser.role === 'ADMIN' && (
  <button type="button">删除员工</button>
)}
```

这段代码只是 UI Control，可以避免普通用户看到无效操作。用户仍可能绕过页面直接调用 API，因此 Backend 必须再次确认身份、角色、资源范围和操作权限。

### 8. 前端安全边界

- 前端控制显示，后端决定授权。
- HTTPS 保护传输，但不修复 XSS、CSRF 或越权。
- 不用 `dangerouslySetInnerHTML` 渲染未经可信净化的内容。
- 日志、Toast 和错误页不泄漏令牌、个人信息或内部地址。
- 依赖升级先看变更与安全公告，执行测试和构建，不盲目更新主版本。

### 9. XSS

XSS 是攻击者设法让不可信内容在其他用户的浏览器中作为脚本执行。可能造成页面内容篡改、冒用用户操作或读取 JavaScript 能访问的数据。

例如攻击者把类似脚本标签的内容填写为员工姓名。如果应用把它直接当 HTML 插入页面，就可能产生风险。

普通 JSX 插值会把字符串作为文字显示：

```jsx
<p>{employee.name}</p>
```

不要随意使用 `dangerouslySetInnerHTML`。业务必须显示 HTML 时，应采用团队批准的净化方案，并明确允许的标签和属性。

普通 JSX 插值会转义字符串，因此 `<p>{employee.name}</p>` 默认把内容当文字显示。但这不代表应用不会有 XSS：URL、第三方库、DOM API 和未经净化的 HTML 都需要继续按安全规则处理。

### 10. Cookie 的常见属性

| 属性 | 作用 | 不能解决的问题 |
| --- | --- | --- |
| `HttpOnly` | 禁止 JavaScript 直接读取 Cookie | 不能单独阻止 CSRF |
| `Secure` | 只通过 HTTPS 发送 | 不能修复 XSS 或越权 |
| `SameSite` | 限制跨站请求携带 Cookie | 仍需按架构评估 CSRF |

Cookie 是否由前端创建、保存什么内容、生命周期多长，应按照认证架构和 Backend 规格决定。`HttpOnly` Cookie 通常由服务器通过响应头设置，前端 JavaScript 无法直接读取。

### 11. CSRF

Cookie Session 可能由浏览器自动携带，因此写请求需要按后端方案考虑 SameSite、CSRF Token、Origin 检查等防护。`HttpOnly` 阻止 JavaScript 直接读取 Cookie，但不能单独解决 CSRF；`Secure` 表示只通过 HTTPS 发送。

```text
用户已经登录 A 网站
  ↓
Browser 保存 A 的认证 Cookie
  ↓
用户访问恶意网站 B
  ↓
B 尝试向 A 发送写请求
  ↓
Browser 可能自动携带 A 的 Cookie
```

常见防护包括适当的 SameSite、CSRF Token、Origin / Referer 检查等。具体组合由 Backend 框架和项目安全设计决定，前端不能自行关闭保护来解决调用错误。

### 12. CORS 与 CSRF 不相同

CORS 是浏览器对跨 Origin 前端读取响应的访问控制机制。当前页面是 `http://localhost:5173`，API 是 `http://localhost:3000` 时，协议、Host、Port 任一不同都可能形成跨 Origin 请求。

出现 CORS Error 时检查：

1. Console 的完整错误；
2. Network 中是否出现 OPTIONS 预检；
3. Request Origin；
4. Response 的 `Access-Control-Allow-Origin`；
5. 使用 Cookie 时是否同时正确配置 credentials。

不要用“关闭浏览器安全功能”作为项目解决方案。CORS 也不是 CSRF 防护的替代品：两者关注的问题不同。

### 13. Token 保存位置

| 位置 | 特点 | 主要注意点 |
| --- | --- | --- |
| Memory | 页面运行期间保存，刷新后消失 | 需要恢复认证流程 |
| HttpOnly Cookie | JavaScript 不能直接读取 | 浏览器自动携带时评估 CSRF |
| `sessionStorage` | 当前 Tab 会话可由 JS 读取 | XSS 发生时可能被读取 |
| `localStorage` | 跨浏览器会话保留且可由 JS 读取 | XSS 风险与清理策略 |

不存在适合所有项目的唯一答案。应按照 Session / Token 架构、威胁模型、Backend 支持和团队安全规范选择，不要擅自把 HttpOnly Cookie 改成 Web Storage。

### 14. HTTPS 能解决什么

HTTPS 保护浏览器与服务器之间传输的数据，降低内容被窃听或篡改的风险。但 HTTPS 不能自动解决：

- XSS；
- CSRF；
- 用户越权；
- Backend 错误授权；
- 把 Secret 打包进前端；
- 应用主动记录敏感信息。

Bearer Token 的风险和处理方式不同，不要自行把项目从 HttpOnly Cookie 改成 `localStorage` Token。

### 15. 调查方法

#### 文件上传失败

```text
确认选择的 File
→ 前端 Validation
→ Network Request Payload / Form Data
→ 字段名和 Content-Type
→ Status / Response
→ Backend 文件限制与日志
```

#### 环境地址错误

```text
.env 文件与启动 mode
→ import.meta.env
→ appConfig
→ Axios baseURL
→ Network Request URL
```

#### 认证或权限错误

```text
Network Status 401 / 403
→ Cookie 或 Authorization Header
→ 当前用户与角色
→ API 权限规格
→ Backend 授权日志
```

### 16. 练习

1. 上传 CSV，处理类型、过大、取消、成功和 413。
2. 下载 CSV 并确认 object URL 被释放。
3. 在开发/生产模式打印非敏感 API base URL，检查构建产物可见性。
4. 在前端隐藏按钮后直接请求接口，记录后端授权应返回的结果。
5. 删除 `VITE_API_BASE_URL`，确认项目能立即报告配置错误。
6. 把包含 HTML 标签的文字作为姓名显示，确认普通 JSX 不会执行它。
7. 在 Network 中比较 401、403 与 CORS 失败时是否存在 HTTP Response。
8. 画出 Cookie 认证下的 CSRF 攻击流程，并标出项目采用的防护位置。
9. Review 一组 `.env` 变量，找出不应该进入前端构建的秘密。

### 17. 常见错误

- 把前端文件类型检查当成安全验证：后端必须重新检查实际内容与权限。
- 取消后仍显示“上传失败”：需要区分 canceled 与 error。
- 新上传开始时不取消旧上传：旧结果可能覆盖新文件状态。
- 把秘密写入 `VITE_` 变量：构建后浏览器用户可以读取。
- 只凭扩展名接受文件：后端必须检查真实内容、大小和权限。
- 把 HttpOnly 当作完整安全方案：仍需 HTTPS、SameSite、CSRF 和后端授权。
- 把 CORS 与 CSRF 当成同一个问题：先确认请求来源、响应和认证方式。
- 认为隐藏按钮就是权限控制：Backend 必须重新授权。
- 认为 HTTPS 能修复应用漏洞：它主要保护网络传输。

## 本章检查点

- [ ] 能实现上传进度、取消、失败与重试状态。
- [ ] 能在下载后释放 object URL。
- [ ] 能解释 Vite 环境变量为什么不能保存秘密。
- [ ] 能解释 Authentication 与 Authorization 的区别。
- [ ] 能区分 XSS、CSRF 与 CORS 的基本目的和调查位置。
- [ ] 能说明 HttpOnly、Secure、SameSite 各自解决什么问题。
