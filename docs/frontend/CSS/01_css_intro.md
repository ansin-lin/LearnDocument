# 认识 CSS 与引入样式

## 本章目标

完成本章后，你可以：

- 说明 CSS 的作用
- 写出一条基本 CSS 规则
- 区分行内样式表、内部样式表和外部样式表
- 使用外部 CSS 文件控制页面样式

## 1. CSS 的作用

HTML 可以写出页面结构，但只靠 HTML 很难统一控制页面外观。CSS 用来设置网页元素的颜色、大小、位置和布局。

一条 CSS 规则通常由选择器和声明组成：

```css
h1 {
    color: red;
    font-size: 25px;
}
```

这段代码做了两件事：

- 找到所有 `h1` 标签
- 把这些标签的文字颜色设为红色，字号设为 `25px`

## 2. CSS 代码格式

推荐使用展开格式：

```css
h3 {
    color: deeppink;
    font-size: 20px;
}
```

不推荐把多条声明都挤在一行：

```css
h3 { color: deeppink; font-size: 20px; }
```

展开格式更容易阅读、修改和 Review。企业项目中即使文件较长，也优先保证源码可维护。

## 3. 行内样式表

行内样式写在标签的 `style` 属性中：

```html
<div style="color: red; font-size: 12px;">
    青春不常在，抓紧谈恋爱
</div>
```

它只影响当前这个元素。行内样式写起来快，但不利于复用，也容易覆盖外部样式。项目中不要大量使用。

## 4. 内部样式表

内部样式表写在 HTML 的 `style` 标签中：

```html
<style>
    div {
        color: red;
        font-size: 12px;
    }
</style>
```

内部样式适合单页实验。正式课程主线会逐步改用外部样式表。

## 5. 外部样式表

外部样式表把 CSS 写到单独文件中，再用 `link` 引入：

```html
<link rel="stylesheet" href="css/style.css">
```

`style.css` 中写：

```css
h1 {
    color: red;
    font-size: 25px;
}
```

外部样式表能让多个页面共用样式，也是企业项目中最常见的基础写法。

## 6. 本章练习

新建 `css-demo/index.html`：

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CSS 初体验</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <h1>CSS 初体验</h1>
    <p>CSS 可以控制网页中文字的颜色和大小。</p>
</body>
</html>
```

新建 `css-demo/css/style.css`：

```css
h1 {
    color: red;
    font-size: 25px;
}

p {
    color: green;
}
```

## 7. 验证

用浏览器打开 `index.html`，确认：

- `h1` 显示为红色
- `p` 显示为绿色
- 修改 `style.css` 后刷新页面可以看到变化

如果样式没有生效，优先检查 `href="css/style.css"` 的路径是否正确。
