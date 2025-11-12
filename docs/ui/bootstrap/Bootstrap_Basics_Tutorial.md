# 第1节：Bootstrap 入门与基础结构

> 🎯 教学目标：  
>
> - 了解 Bootstrap 的作用与核心理念  
> - 掌握 Bootstrap 的引入方式（CDN / 本地）  
> - 熟悉容器、行、列的布局系统  
> - 理解响应式断点与常用排版类  
> - 完成一个两栏卡片布局的小型页面  

---

## 一、Bootstrap 简介

**Bootstrap** 是全球最流行的前端 UI 框架之一，基于 **HTML、CSS、JavaScript** 构建，  
可以帮助开发者快速实现响应式、美观的页面。

📌 **主要特点：**

- 响应式布局（自动适配手机、平板、桌面端）  
- 丰富的 UI 组件（按钮、表单、模态框等）  
- 统一的视觉主题与交互逻辑  
- 可通过 Sass 定制主题变量  

📘 当前推荐版本： **Bootstrap 5.x**  
（Bootstrap 5 移除了 jQuery 依赖，全面支持现代浏览器与 ES6）

---

## 二、项目引入方式

### 1️⃣ CDN 方式（最常用）

在 HTML 的 `<head>` 与 `<body>` 中添加以下代码即可：

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bootstrap 入门</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  </head>
  <body>
    <h1 class="text-center text-primary">Hello Bootstrap!</h1>
    <!-- Bootstrap JS Bundle (含 Popper) -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
  </body>
</html>
```

✅ **优点**：简单、快速  
⚠️ **缺点**：需要联网  

---

### 2️⃣ 本地引入方式

1. 访问官网 [https://getbootstrap.com](https://getbootstrap.com) 下载源码  
2. 将 `bootstrap.min.css` 与 `bootstrap.bundle.min.js` 放入项目目录  
3. 在 HTML 中使用相对路径引入：

```html
<link rel="stylesheet" href="./css/bootstrap.min.css">
<script src="./js/bootstrap.bundle.min.js"></script>
```

---

## 三、Bootstrap 布局核心：Container / Row / Col

Bootstrap 使用 **12 栏栅格系统** 来组织页面。  
页面布局核心结构：

```text
container → row → col
```

### 1️⃣ `<div class="container">`

- 固定宽度并居中显示。  
- 内容在不同屏幕下自动调整最大宽度。

📘 示例：

```html
<div class="container bg-light border">
  固定宽度容器
</div>
```

### 2️⃣ `<div class="container-fluid">`

- 100% 宽度容器（铺满全屏）。

📘 示例：

```html
<div class="container-fluid bg-secondary text-white p-3">
  全屏宽度容器
</div>
```

### 3️⃣ `<div class="row">`

- 行容器，子元素必须是 `.col-*` 类。  
- 自动换行并对齐。

### 4️⃣ `<div class="col">`

- 列容器，每行最多 12 列。  
- 可以指定列宽，如 `col-4`, `col-6` 等。

📘 示例：

```html
<div class="container">
  <div class="row">
    <div class="col-4 bg-primary text-white">左侧</div>
    <div class="col-8 bg-warning">右侧</div>
  </div>
</div>
```

---

## 四、响应式断点与自适应布局

Bootstrap 提供 5 种断点：

| 尺寸 | 类前缀 | 适配设备 | 宽度范围 |
|------|----------|-----------|-----------|
| Extra small | 无 | 手机 | <576px |
| Small | sm | 小平板 | ≥576px |
| Medium | md | 平板 | ≥768px |
| Large | lg | 桌面 | ≥992px |
| Extra large | xl | 大屏 | ≥1200px |

📘 **示例：响应式列布局**

```html
<div class="container">
  <div class="row">
    <div class="col-12 col-md-6 col-lg-4 bg-info text-white p-3">区块1</div>
    <div class="col-12 col-md-6 col-lg-4 bg-success text-white p-3">区块2</div>
    <div class="col-12 col-md-12 col-lg-4 bg-danger text-white p-3">区块3</div>
  </div>
</div>
```

💡 不同设备下会自动换行、调整列宽。

---

## 五、常用排版与对齐类

| 类名 | 功能 |
|------|------|
| `text-center` | 水平居中 |
| `text-end` | 右对齐 |
| `fw-bold` | 加粗文本 |
| `text-muted` | 灰色说明文字 |
| `p-3`, `m-2` | 内外边距（Padding / Margin） |
| `d-flex` | 启用 Flex 布局 |
| `justify-content-between` | 元素两端对齐 |
| `align-items-center` | 垂直居中 |

📘 **示例：Flex 对齐**

```html
<div class="d-flex justify-content-between align-items-center bg-light p-3 border">
  <span>左侧文字</span>
  <button class="btn btn-primary">按钮</button>
</div>
```

---

## 六、实战：两栏卡片布局

📘 **完整示例：**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bootstrap 布局示例</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  </head>
  <body>
    <div class="container mt-4">
      <div class="row g-4">
        <div class="col-12 col-md-6">
          <div class="card shadow-sm">
            <div class="card-body">
              <h5 class="card-title text-primary">左侧卡片</h5>
              <p class="card-text">使用 Bootstrap 栅格实现两栏布局。</p>
              <a href="#" class="btn btn-outline-primary">查看更多</a>
            </div>
          </div>
        </div>
        <div class="col-12 col-md-6">
          <div class="card shadow-sm">
            <div class="card-body">
              <h5 class="card-title text-success">右侧卡片</h5>
              <p class="card-text">在小屏幕下会自动换行。</p>
              <a href="#" class="btn btn-outline-success">详细信息</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
```

💡 **讲解要点：**

- `.g-4`：设置行列间距（gap）  
- `.shadow-sm`：添加轻微阴影  
- `.card`：常用内容容器组件  

---

## 七、总结

| 模块 | 关键标签 / 类 | 功能说明 |
|------|----------------|----------|
| 容器 | `.container`, `.container-fluid` | 页面主体结构 |
| 栅格 | `.row`, `.col-*` | 布局基础 |
| 响应式 | `.col-md-*`, `.col-lg-*` | 不同设备自动调整 |
| 排版 | `.text-*`, `.fw-*`, `.p-*`, `.m-*` | 控制文字与间距 |
| Flex 布局 | `.d-flex`, `.justify-content-*` | 快速对齐布局 |

✅ **一句话总结：**  
> Bootstrap 的 Container + Row + Col 是布局核心，搭配响应式断点与排版类，  
> 可以快速搭建整洁、美观的现代网页结构。
