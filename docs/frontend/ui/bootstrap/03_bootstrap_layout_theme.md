# 第3章 Bootstrap工具类、主题与响应式业务页面

> 🎯 教学目标：  
>
> - 掌握 Bootstrap 的响应式布局机制与断点系统  
> - 理解 Flexbox 与 Grid 布局在 Bootstrap 中的用法  
> - 熟悉常用 Utility 工具类（颜色、间距、隐藏）  
> - 学会自定义主题颜色与暗黑模式切换  
> - 综合实战：构建响应式后台管理布局  

---

## 一、Bootstrap 布局系统概览

Bootstrap 的布局系统基于 **Flexbox** 与 **12列栅格系统（Grid System）**，
可实现灵活的响应式设计。

核心结构：

```text
container → row → col
```

| 元素 | 功能 | 示例类 |
|------|------|--------|
| `.container` | 固定宽度容器（居中） | `.container`, `.container-fluid` |
| `.row` | 行容器，用于包裹列 | `.row g-3` |
| `.col` | 列容器（最多12列） | `.col-4`, `.col-md-6` |
| `.d-flex` | 启用 Flex 布局 | `.d-flex justify-content-center` |

---

## 二、响应式断点（Breakpoints）

Bootstrap 5.3提供六种移动优先断点。

| 名称 | 前缀 | 最小宽度 | 设备类型 |
|------|------|-----------|----------|
| Extra small | 无 | <576px | 手机 |
| Small | sm | ≥576px | 小平板 |
| Medium | md | ≥768px | 平板 |
| Large | lg | ≥992px | 桌面 |
| Extra large | xl | ≥1200px | 大屏 |
| Extra extra large | xxl | ≥1400px | 超大屏 |

📘 **示例：响应式三栏布局**

```html
<div class="container">
  <div class="row">
    <div class="col-12 col-md-6 col-lg-4 bg-primary text-white p-3">区块1</div>
    <div class="col-12 col-md-6 col-lg-4 bg-success text-white p-3">区块2</div>
    <div class="col-12 col-md-12 col-lg-4 bg-danger text-white p-3">区块3</div>
  </div>
</div>
```

💡 不同屏幕下自动折叠。

---

## 三、Flexbox 布局与对齐

### 1️⃣ 启用 Flex 布局

使用 `.d-flex` 激活弹性盒模型。

📘 **示例：**

```html
<div class="d-flex justify-content-between align-items-center bg-light p-3 border">
  <span>左侧文字</span>
  <button class="btn btn-primary">操作按钮</button>
</div>
```

### 2️⃣ 常用对齐类

| 类名 | 功能 |
|------|------|
| `justify-content-start` | 左对齐 |
| `justify-content-center` | 居中 |
| `justify-content-end` | 右对齐 |
| `justify-content-between` | 两端对齐 |
| `align-items-start` | 顶部对齐 |
| `align-items-center` | 垂直居中 |
| `align-items-end` | 底部对齐 |

📘 **垂直布局示例：**

```html
<div class="d-flex flex-column align-items-center bg-secondary p-4 text-white">
  <div>元素1</div>
  <div>元素2</div>
  <div>元素3</div>
</div>
```

---

## 四、常用 Utility 工具类

Bootstrap 提供大量工具类，无需写 CSS 即可调整外观。

### 1️⃣ 间距（Spacing）

| 类名 | 功能 |
|------|------|
| `p-*` | 内边距（padding） |
| `m-*` | 外边距（margin） |
| `*-0~5` | 间距等级（0=0px, 5=48px） |

📘 示例：

```html
<div class="p-3 m-2 bg-info text-white">带间距的块</div>
```

### 2️⃣ 颜色与背景

| 类名 | 作用 |
|------|------|
| `text-primary` | 蓝色文字 |
| `bg-success` | 绿色背景 |
| `text-danger` | 红色文字 |
| `bg-dark text-white` | 深底白字 |

📘 示例：

```html
<p class="text-success">操作成功</p>
<div class="bg-warning p-3">警告区域</div>
```

### 3️⃣ 显示控制

| 类名 | 说明 |
|------|------|
| `d-none` | 隐藏元素 |
| `d-block` | 块级显示 |
| `d-md-none` | 从md开始隐藏 |
| `d-none d-lg-block` | 小屏隐藏，从lg开始显示 |

📘 示例：

```html
<p class="d-none d-md-block">从md宽度开始显示</p>
```

---

## 五、主题与配色系统

Bootstrap 内置颜色系统，可通过类名快速使用。

| 类别 | 类名 | 示例 |
|------|------|------|
| 主色 | `text-primary` / `bg-primary` | 蓝色 |
| 成功 | `text-success` / `bg-success` | 绿色 |
| 警告 | `text-warning` / `bg-warning` | 黄色 |
| 危险 | `text-danger` / `bg-danger` | 红色 |
| 信息 | `text-info` / `bg-info` | 青色 |
| 暗色 | `text-dark` / `bg-dark` | 深灰 |
| 浅色 | `text-light` / `bg-light` | 浅灰 |

📘 **示例：**

```html
<div class="p-3 mb-2 bg-primary text-white">主色主题</div>
<div class="p-3 mb-2 bg-dark text-light">暗色主题</div>
```

---

## 六、暗黑模式与自定义样式

Bootstrap 5.3支持颜色模式，通过根元素的`data-bs-theme`属性启用全局主题。

📘 **示例：**

```html
<html lang="zh-CN" data-bs-theme="dark">
<body>
  <div class="container py-5">
    <h3 class="text-light">暗黑模式演示</h3>
    <button class="btn btn-light">浅色按钮</button>
  </div>
</body>
</html>
```

💡 切换主题可使用 JS 动态修改属性：

```js
document.documentElement.setAttribute('data-bs-theme', 'dark')
document.documentElement.setAttribute('data-bs-theme', 'light')
```

---

## 七、自定义主题颜色（Sass 变量）

如果项目使用npm构建，可安装Bootstrap与Sass：

```bash
npm install bootstrap
npm install -D sass
```

随后在SCSS入口中先覆盖变量，再导入Bootstrap。具体编译命令取决于当前Vite或其他构建配置。

📘 **_custom.scss：**

```scss
$primary: #007bff;
$secondary: #6c757d;
$success: #28a745;
$danger: #dc3545;
$info: #17a2b8;

@import "bootstrap/scss/bootstrap";
```

编译后生成 `bootstrap.css` 即可使用自定义颜色。

---

## 八、实战：响应式后台管理布局

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WorkHub任务管理</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  </head>
  <body>
    <nav class="navbar navbar-expand-lg bg-primary" data-bs-theme="dark">
      <div class="container-fluid">
        <a class="navbar-brand" href="#">WorkHub</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav" aria-controls="nav" aria-expanded="false" aria-label="打开或关闭导航">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="nav">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item"><a class="nav-link active" aria-current="page" href="#">任务一览</a></li>
            <li class="nav-item"><a class="nav-link" href="#">设置</a></li>
            <li class="nav-item">
              <button id="themeToggle" class="btn btn-sm btn-light ms-3">切换主题</button>
            </li>
          </ul>
        </div>
      </div>
    </nav>

    <div class="container-fluid mt-4">
      <div class="row">
        <div class="col-md-3 col-lg-2 bg-light border-end p-3">
          <ul class="nav flex-column">
            <li class="nav-item"><a class="nav-link active" aria-current="page" href="#">任务一览</a></li>
            <li class="nav-item"><a class="nav-link" href="#">新增任务</a></li>
            <li class="nav-item"><a class="nav-link" href="#">设置</a></li>
          </ul>
        </div>
        <div class="col-md-9 col-lg-10 p-4">
          <h1 class="h4">任务概况</h1>
          <div class="row g-3 mt-3">
            <div class="col-md-4">
              <div class="card text-bg-success">
                <div class="card-body">
                  <h2 class="card-title h5">待处理</h2>
                  <p class="card-text fs-4">3 件</p>
                </div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="card text-bg-info">
                <div class="card-body">
                  <h2 class="card-title h5">处理中</h2>
                  <p class="card-text fs-4">2 件</p>
                </div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="card text-bg-warning">
                <div class="card-body">
                  <h2 class="card-title h5">已完成</h2>
                  <p class="card-text fs-4">8 件</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <script>
      const toggleBtn = document.getElementById('themeToggle');
      toggleBtn.addEventListener('click', () => {
        const root = document.documentElement;
        const current = root.getAttribute('data-bs-theme') || 'light';
        root.setAttribute('data-bs-theme', current === 'light' ? 'dark' : 'light');
        toggleBtn.textContent = current === 'light' ? '切换亮色' : '切换暗色';
      });
    </script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
  </body>
</html>
```

💡 特点：  

- 响应式侧边栏 + 主内容区  
- 动态主题切换（light/dark）  
- 使用 `.text-bg-*` 快速统一配色  

---

## 九、总结

| 分类 | 功能 | 关键类 / 特性 |
|------|------|----------------|
| 布局结构 | Container / Row / Col | 栅格系统 |
| 响应式 | Breakpoints | 自动调整布局 |
| 对齐方式 | Flex Utilities | 快速控制位置 |
| 主题样式 | text/bg 类 | 配色统一 |
| 暗黑模式 | data-bs-theme | 切换主题模式 |
| 自定义主题 | Sass 变量 | 个性化品牌色 |

掌握这些内容后，应能解释每个响应式类从哪个断点开始生效，并能在不破坏语义和交互的前提下完成主题调整。

## 十、练习与验收

1. 把WorkHub整理为顶部导航、筛选区、统计卡片和任务列表。
2. 使用`d-none d-lg-block`控制只在桌面显示的辅助说明。
3. 增加亮色/暗色切换。
4. 在360px、768px、992px和1400px附近检查布局。
5. 检查长标题、空列表、错误提示、禁用按钮和键盘焦点。
