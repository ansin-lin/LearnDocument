# 第2节：Bootstrap 常用组件与交互 JS 功能

> 🎯 教学目标：  
>
> - 掌握 Bootstrap 的常用 UI 组件（按钮、表单、导航栏、模态框等）  
> - 理解组件的 HTML 结构与常用类名  
> - 学会使用 Bootstrap 自带的 JavaScript 功能  
> - 掌握交互组件（Modal、Collapse、Toast）的触发与控制方法  

---

## 一、按钮（Button）

Bootstrap 提供多种按钮样式，通过类名快速控制颜色、大小与形态。

### 1️⃣ 基本按钮样式

| 类名 | 效果说明 |
|------|-----------|
| `btn-primary` | 主要按钮（蓝色） |
| `btn-secondary` | 次要按钮（灰色） |
| `btn-success` | 成功操作（绿色） |
| `btn-danger` | 错误或警告（红色） |
| `btn-warning` | 提示（黄色） |
| `btn-info` | 信息类（青色） |
| `btn-light` | 浅色按钮 |
| `btn-dark` | 深色按钮 |

📘 **示例：**

```html
<div class="p-3">
  <button class="btn btn-primary">主按钮</button>
  <button class="btn btn-success">成功</button>
  <button class="btn btn-outline-danger">边框按钮</button>
</div>
```

---

### 2️⃣ 按钮大小与状态

| 类名 | 功能 |
|------|------|
| `btn-lg` | 大号按钮 |
| `btn-sm` | 小号按钮 |
| `disabled` | 禁用状态 |

📘 **示例：**

```html
<button class="btn btn-primary btn-lg">大号按钮</button>
<button class="btn btn-secondary btn-sm">小号按钮</button>
<button class="btn btn-danger" disabled>禁用按钮</button>
```

---

## 二、表单组件（Form）

Bootstrap 提供一系列输入组件，支持自动样式与表单验证。

### 1️⃣ 基本输入框

📘 **示例：**

```html
<form class="p-3">
  <div class="mb-3">
    <label for="email" class="form-label">邮箱地址</label>
    <input type="email" class="form-control" id="email" placeholder="name@example.com">
  </div>
  <div class="mb-3">
    <label for="password" class="form-label">密码</label>
    <input type="password" class="form-control" id="password" placeholder="输入密码">
  </div>
  <button type="submit" class="btn btn-primary">提交</button>
</form>
```

---

### 2️⃣ 选择框与复选框

📘 **示例：**

```html
<div class="mb-3">
  <label class="form-label">选择城市</label>
  <select class="form-select">
    <option>东京</option>
    <option>大阪</option>
    <option>京都</option>
  </select>
</div>

<div class="form-check">
  <input class="form-check-input" type="checkbox" value="" id="check1">
  <label class="form-check-label" for="check1">
    我已阅读并同意条款
  </label>
</div>
```

---

### 3️⃣ 表单验证样式

📘 **示例：**

```html
<form class="was-validated">
  <div class="mb-3">
    <label for="username" class="form-label">用户名</label>
    <input type="text" class="form-control" id="username" required>
    <div class="invalid-feedback">请输入用户名。</div>
  </div>
  <button class="btn btn-success">确认</button>
</form>
```

💡 `was-validated` 会在提交后显示验证提示。

---

## 三、导航栏（Navbar）

导航栏是网站顶部常用组件。

📘 **示例：**

```html
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <div class="container-fluid">
    <a class="navbar-brand" href="#">MySite</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNav">
      <ul class="navbar-nav ms-auto">
        <li class="nav-item"><a class="nav-link active" href="#">首页</a></li>
        <li class="nav-item"><a class="nav-link" href="#">功能</a></li>
        <li class="nav-item"><a class="nav-link" href="#">联系</a></li>
      </ul>
    </div>
  </div>
</nav>
```

🧠 **讲解：**

- `navbar-expand-lg`：在大屏时展开导航  
- `navbar-toggler`：折叠按钮  
- `data-bs-toggle="collapse"`：折叠行为  
- `ms-auto`：右对齐菜单  

---

## 四、模态框（Modal）

模态框是 Bootstrap 自带的 JS 组件，用于显示弹出窗口。

📘 **示例：**

```html
<!-- 按钮触发 -->
<button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#myModal">打开弹窗</button>

<!-- 模态框结构 -->
<div class="modal fade" id="myModal" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">提示</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        这是一个模态框内容区域。
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
        <button class="btn btn-primary">确认</button>
      </div>
    </div>
  </div>
</div>
```

💡 Bootstrap JS 会自动处理打开/关闭逻辑，无需自写脚本。

---

## 五、折叠面板（Collapse）

📘 **示例：**

```html
<p>
  <a class="btn btn-info" data-bs-toggle="collapse" href="#collapseExample">展开内容</a>
</p>
<div class="collapse" id="collapseExample">
  <div class="card card-body">
    这是折叠面板内容，可通过按钮展开/隐藏。
  </div>
</div>
```

🧠 `data-bs-toggle="collapse"` 和 `href="#id"` 配合实现展开与收起。

---

## 六、提示框（Toast）

📘 **示例：**

```html
<button class="btn btn-success" id="showToast">显示提示</button>

<div class="toast-container position-fixed bottom-0 end-0 p-3">
  <div id="liveToast" class="toast" role="alert">
    <div class="toast-header">
      <strong class="me-auto text-primary">系统消息</strong>
      <small>刚刚</small>
      <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
    </div>
    <div class="toast-body">
      操作已成功完成！
    </div>
  </div>
</div>

<script>
  const toastTrigger = document.getElementById('showToast')
  const toastLive = document.getElementById('liveToast')
  if (toastTrigger) {
    toastTrigger.addEventListener('click', () => {
      const toast = new bootstrap.Toast(toastLive)
      toast.show()
    })
  }
</script>
```

💡 **提示框 (Toast)** 是轻量级通知组件，可以手动或自动显示。

---

## 七、实战示例：带登录模态框的导航页

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bootstrap 实战</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  </head>
  <body>
    <!-- 导航栏 -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
      <div class="container-fluid">
        <a class="navbar-brand" href="#">DemoSite</a>
        <button class="navbar-toggler" data-bs-toggle="collapse" data-bs-target="#menu">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="menu">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item"><a class="nav-link active" href="#">首页</a></li>
            <li class="nav-item"><a class="nav-link" href="#">功能</a></li>
            <li class="nav-item"><a class="nav-link" href="#" data-bs-toggle="modal" data-bs-target="#loginModal">登录</a></li>
          </ul>
        </div>
      </div>
    </nav>

    <!-- 登录模态框 -->
    <div class="modal fade" id="loginModal" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">用户登录</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <form>
              <div class="mb-3">
                <label class="form-label">用户名</label>
                <input type="text" class="form-control" required>
              </div>
              <div class="mb-3">
                <label class="form-label">密码</label>
                <input type="password" class="form-control" required>
              </div>
              <button type="submit" class="btn btn-primary w-100">登录</button>
            </form>
          </div>
        </div>
      </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
  </body>
</html>
```

---

## 八、总结

| 分类 | 组件 | 功能说明 |
|------|------|-----------|
| 基础 | Button | 按钮与交互触发 |
| 输入 | Form / Input / Select | 表单输入与验证 |
| 导航 | Navbar | 顶部导航菜单 |
| 弹窗 | Modal | 模态对话框 |
| 折叠 | Collapse | 可展开/隐藏区域 |
| 提示 | Toast | 提示框或通知 |

✅ **一句话总结：**
> Bootstrap 提供了丰富的 UI 组件，结合 `data-bs-*` 属性即可轻松实现交互效果，  
> 无需额外 JS 库，就能完成专业的网页动态体验。
