# 第 21 章 文件、环境变量与安全

## 本章目标

- 完成上传与下载，并正确处理失败和资源释放。
- 使用 Vite 环境变量配置公开运行参数。
- 理解认证、权限与前端安全边界。

## 1. 文件上传

```jsx
async function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  await httpClient.post('/employees/avatar', formData);
}
```

选择文件后检查前端可提示的大小和类型，但后端必须重新验证内容、大小、扩展名、权限和恶意文件。不要手工为 FormData 固定 multipart boundary。

`FormData` 用于构造 `multipart/form-data` 请求。`append('file', file)` 的字段名必须与后台规格一致；浏览器会自动生成 multipart boundary。

上传 UI 应展示文件名、进度或处理中状态、取消/重试入口。不要读取或记录文件内容到 Console。

### 1.1 Progress、Cancel、Error 与 Retry

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

## 2. 下载

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

## 3. Vite 环境变量

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

## 4. 认证与权限复核

- 前端控制显示，后端决定授权。
- HTTPS 保护传输，但不修复 XSS、CSRF 或越权。
- 不用 `dangerouslySetInnerHTML` 渲染未经可信净化的内容。
- 日志、Toast 和错误页不泄漏令牌、个人信息或内部地址。
- 依赖升级先看变更与安全公告，执行测试和构建，不盲目更新主版本。

### 4.1 XSS

普通 JSX 插值会把字符串作为文字显示：

```jsx
<p>{employee.name}</p>
```

不要随意使用 `dangerouslySetInnerHTML`。业务必须显示 HTML 时，应采用团队批准的净化方案，并明确允许的标签和属性。

### 4.2 CSRF 与 Cookie

Cookie Session 可能由浏览器自动携带，因此写请求需要按后端方案考虑 SameSite、CSRF Token、Origin 检查等防护。`HttpOnly` 阻止 JavaScript 直接读取 Cookie，但不能单独解决 CSRF；`Secure` 表示只通过 HTTPS 发送。

Bearer Token 的风险和处理方式不同，不要自行把项目从 HttpOnly Cookie 改成 `localStorage` Token。

### 4.3 认证与授权

隐藏按钮和 Protected Route 只改善体验。后端仍必须确认身份、资源所有者和权限。401 表示需要恢复认证，403 表示当前身份没有权限。

## 5. 练习

1. 上传 CSV，处理类型、过大、取消、成功和 413。
2. 下载 CSV 并确认 object URL 被释放。
3. 在开发/生产模式打印非敏感 API base URL，检查构建产物可见性。
4. 在前端隐藏按钮后直接请求接口，记录后端授权应返回的结果。
5. 删除 `VITE_API_BASE_URL`，确认项目能立即报告配置错误。
6. 把包含 HTML 标签的文字作为姓名显示，确认普通 JSX 不会执行它。

## 6. 常见错误

- 把前端文件类型检查当成安全验证：后端必须重新检查实际内容与权限。
- 取消后仍显示“上传失败”：需要区分 canceled 与 error。
- 新上传开始时不取消旧上传：旧结果可能覆盖新文件状态。
- 把秘密写入 `VITE_` 变量：构建后浏览器用户可以读取。
- 只凭扩展名接受文件：后端必须检查真实内容、大小和权限。
- 把 HttpOnly 当作完整安全方案：仍需 HTTPS、SameSite、CSRF 和后端授权。

## 本章检查点

- [ ] 能实现上传进度、取消、失败与重试状态。
- [ ] 能在下载后释放 object URL。
- [ ] 能解释 Vite 环境变量为什么不能保存秘密。
