# 文字、颜色与背景

## 本章目标

完成本章后，你可以：

- 使用 `font-family`、`font-size`、`font-weight`、`font-style` 和 `font` 设置字体
- 使用 `color`、`text-align`、`text-decoration`、`text-indent` 和 `line-height` 设置文本外观
- 使用 `background-color`、`background-image`、`background-repeat`、`background-position` 和 `background` 设置背景
- 区分内容图片和背景图片
- 在浏览器中观察每个属性带来的具体变化

## 1. 字体属性解决什么问题

字体属性控制“文字本身怎么显示”。例如：字体是什么、字号多大、是否加粗、是否倾斜。

先看一个完整例子：

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CSS 字体属性</title>
    <style>
        body {
            font-family: "Microsoft YaHei", Arial, sans-serif;
            font-size: 16px;
        }

        h2 {
            font-size: 28px;
            font-weight: 400;
        }

        .important {
            font-weight: 700;
            font-style: normal;
        }
    </style>
</head>
<body>
    <h2>JERRY 的秘密</h2>
    <p>姓名：周吉瑞</p>
    <p>生日：2000 年 5 月 4 日</p>
    <p class="important">婚姻状况：单身</p>
</body>
</html>
```

观察结果：

- 页面默认字体使用微软雅黑，系统没有该字体时再尝试 Arial，最后使用浏览器默认无衬线字体
- `h2` 比正文更大，但被设置为不加粗
- `.important` 段落被加粗

### 1.1 字体系列 `font-family`

`font-family` 用来指定文字使用什么字体：

```css
p {
    font-family: "Microsoft YaHei";
}

div {
    font-family: Arial, "Microsoft YaHei", sans-serif;
}
```

多个字体之间用英文逗号隔开。浏览器会从左到右查找：第一个字体能用就用第一个，不能用就看第二个，直到找到可用字体。

常见写法：

```css
body {
    font-family: "Microsoft YaHei", Arial, sans-serif;
}
```

说明：

- 中文字体名建议加引号，例如 `"Microsoft YaHei"`
- 最后通常写一个通用字体族，例如 `sans-serif`
- 不要随便使用用户电脑上不一定安装的冷门字体
- 全局字体一般写在 `body` 上，普通正文会继承它

常见通用字体族：

| 值 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `serif` | 通用字体族 | 浏览器有默认字体 | 衬线字体，笔画末端有装饰 |
| `sans-serif` | 通用字体族 | 常作为兜底字体 | 无衬线字体，页面正文常用 |
| `monospace` | 通用字体族 | 代码展示常用 | 等宽字体，每个字符宽度接近 |

练习：把 `body` 的 `font-family` 改成下面几种，刷新浏览器观察文字变化。

```css
body {
    font-family: "Times New Roman", serif;
}
```

```css
body {
    font-family: Arial, "Microsoft YaHei", sans-serif;
}
```

### 1.2 字号 `font-size`

`font-size` 用来控制文字大小：

```css
p {
    font-size: 20px;
}
```

`px` 是 CSS 初学阶段最常用的长度单位。浏览器默认正文通常接近 `16px`，但项目中最好明确设置基础字号，避免不同浏览器显示差异过大。

示例：

```html
<style>
    body {
        font-size: 24px;
    }

    h2 {
        font-size: 54px;
    }
</style>

<h2>JERRY 的秘密</h2>
<p>姓名：周吉瑞</p>
<p>生日：2000 年 5 月 4 日</p>
```

注意：

- `font-size: 20px;` 不能写成 `font-size: 20;`，长度值通常要带单位
- 标题标签有浏览器默认字号，给 `body` 设置字号后，标题不一定变成同样大小
- 页面中字号不宜过多，正文、辅助文字、标题形成清楚层级即可

### 1.3 字体粗细 `font-weight`

`font-weight` 控制文字是否加粗：

```css
p {
    font-weight: bold;
}
```

常用值：

| 值 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `normal` | 关键字 | 默认值 | 正常粗细 |
| `bold` | 关键字 | 手动设置 | 加粗 |
| `400` | 数字 | 等同于 `normal` | 正常粗细 |
| `700` | 数字 | 等同于 `bold` | 常用加粗 |

实际开发中经常使用数字：

```css
.bold {
    font-weight: 700;
}

h2 {
    font-weight: 400;
}
```

注意：`700` 后面不要加单位，不能写成 `700px`。

### 1.4 字体样式 `font-style`

`font-style` 控制文字是否倾斜：

```css
p {
    font-style: italic;
}

em {
    font-style: normal;
}
```

常用值：

| 值 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `normal` | 关键字 | 默认值 | 不倾斜 |
| `italic` | 关键字 | 手动设置 | 倾斜 |

项目中不常给大段正文设置斜体。更常见的情况是把 `em`、`i` 这类默认倾斜的标签改为正常显示。

### 1.5 字体复合属性 `font`

多个字体属性可以合并写到 `font` 中：

```css
div {
    font: italic 700 16px "Microsoft YaHei";
}
```

完整顺序：

```css
font: font-style font-weight font-size/line-height font-family;
```

可以省略 `font-style` 和 `font-weight`，但必须保留 `font-size` 和 `font-family`：

```css
body {
    font: 16px/1.5 "Microsoft YaHei", Arial, sans-serif;
}
```

不推荐初学时马上大量使用复合写法。先分别写属性，确认效果后再合并。

### 1.6 字体属性总结

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `font-family` | 字体名、通用字体族 | 默认由浏览器决定 | 设置字体 |
| `font-size` | `16px`、`20px` 等长度值 | 默认由浏览器决定 | 设置字号 |
| `font-weight` | `normal`、`bold`、`400`、`700` 等 | 默认 `normal` | 设置粗细 |
| `font-style` | `normal`、`italic` | 默认 `normal` | 设置倾斜 |
| `font` | 按顺序组合字体相关值 | 必须包含字号和字体族 | 简写字体属性 |

## 2. 文本属性解决什么问题

文本属性控制“文字段落怎么排版”。例如颜色、水平对齐、下划线、首行缩进和行高。

### 2.1 文本颜色 `color`

`color` 设置文字颜色：

```css
div {
    color: red;
}
```

常见颜色写法：

| 表示方式 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| 预定义颜色名 | `red`、`green`、`blue`、`black`、`white`、`gray` | 默认由浏览器或继承决定 | 便于演示和少量常见色 |
| 十六进制 | `#ff0000`、`#333333`、`#ffffff` | 项目中最常用 | 精确表示颜色 |
| RGB | `rgb(255, 0, 0)` | 可用 | 用红绿蓝数值表示颜色 |
| RGBA | `rgba(0, 0, 0, 0.3)` | 背景半透明常用 | 多一个透明度 |

示例：

```css
div {
    /* color: deeppink; */
    /* color: #ff1493; */
    color: rgb(255, 20, 147);
}
```

项目中更常用十六进制。颜色不要只看“好不好看”，还要确认文字和背景对比度足够，尤其是提示、错误信息和按钮。

### 2.2 文本对齐 `text-align`

`text-align` 设置元素内部文本或行内内容的水平对齐方式：

```css
h1 {
    text-align: right;
}
```

常用值：

| 值 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `left` | 关键字 | 默认值通常为左对齐 | 左对齐 |
| `center` | 关键字 | 手动设置 | 居中对齐 |
| `right` | 关键字 | 手动设置 | 右对齐 |

示例：

```html
<style>
    div {
        text-align: center;
    }
</style>

<div>
    <p>zhoujiruizhoujirui</p>
</div>
```

这里给 `div` 设置 `text-align: center`，里面的文字会居中。`text-align` 控制的是盒子内部的行内内容，不是把整个块级盒子移动到页面中间。

### 2.3 文本装饰 `text-decoration`

`text-decoration` 控制下划线、删除线、上划线等：

```css
div {
    text-decoration: underline;
}
```

常用值：

| 值 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `none` | 关键字 | 普通文本默认无装饰 | 取消装饰线 |
| `underline` | 关键字 | 链接默认常见 | 下划线 |
| `line-through` | 关键字 | 手动设置 | 删除线 |
| `overline` | 关键字 | 很少使用 | 上划线 |

示例：

```css
div {
    text-decoration: underline;
}

a {
    text-decoration: none;
    color: #333333;
}
```

取消链接下划线后，要通过颜色、悬停状态、焦点状态等方式让用户仍然看得出它可以点击。

### 2.4 文本缩进 `text-indent`

`text-indent` 设置第一行文字缩进：

```css
p {
    text-indent: 2em;
}
```

`em` 是相对单位，`1em` 等于当前元素的一个字号大小。`text-indent: 2em;` 常用于段落首行缩进两个字。

示例：

```html
<style>
    p {
        font-size: 24px;
        text-indent: 2em;
    }
</style>

<p>打开北京、上海与广州的地铁地图，你会看见三张纵横交错的线路网络。</p>
<p>可即使是这样，在北上广生活的人依然少不了对地铁的抱怨。</p>
```

注意：

- `text-indent: 48px;` 是固定缩进
- `text-indent: 2em;` 会跟随当前字号变化
- 不要用一堆空格手动制造缩进

### 2.5 行高 `line-height`

`line-height` 设置一行文字占用的高度：

```css
p {
    line-height: 26px;
}
```

可以简单理解为：

- 行高越大，行与行之间越松
- 行高越小，文字越挤
- 多行正文通常需要比字号更大的行高

示例：

```html
<style>
    p {
        font-size: 16px;
        line-height: 25px;
    }
</style>

<p>打开北京、上海与广州的地铁地图，你会看见三张纵横交错的线路网络，
这代表了中国最成熟的三套城市轨道交通系统。</p>
```

行高也常写成不带单位的数字：

```css
body {
    font: 16px/1.5 "Microsoft YaHei", Arial, sans-serif;
}
```

`1.5` 表示行高是当前字号的 1.5 倍。这个写法适合写在 `body` 上，让不同字号的子元素按自己的字号计算行高。

### 2.6 文本属性总结

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `color` | 颜色名、十六进制、`rgb()` | 默认继承或浏览器决定 | 文本颜色 |
| `text-align` | `left`、`center`、`right` | 默认与书写方向有关 | 水平对齐 |
| `text-decoration` | `none`、`underline`、`line-through`、`overline` | 默认按元素决定 | 文本装饰线 |
| `text-indent` | `2em`、`20px` 等 | 默认 `0` | 首行缩进 |
| `line-height` | 数字、长度值、百分比 | 默认 `normal` | 行高 |

## 3. 背景属性解决什么问题

背景属性控制“盒子背后显示什么”。可以是颜色，也可以是图片。

内容图片和背景图片要分清：

- 这张图是页面内容的一部分，需要被用户理解，用 HTML 的 `img`
- 这张图只是装饰、纹理、图标背景或大背景，用 CSS 背景

### 3.1 背景颜色 `background-color`

```css
div {
    width: 300px;
    height: 300px;
    background-color: pink;
}
```

默认背景是透明的，也可以明确写：

```css
div {
    background-color: transparent;
}
```

背景颜色常用于区分区域，例如头部、侧边栏、提示块。设置背景后要检查文字是否仍然清楚可读。

### 3.2 背景图片 `background-image`

```css
body {
    background-image: url(images/bg.jpg);
}
```

`url()` 中写图片路径。路径相对于当前 CSS 文件所在位置计算。如果 CSS 文件在 `css/style.css`，图片在 `images/bg.jpg`，路径通常要写成：

```css
body {
    background-image: url("../images/bg.jpg");
}
```

常见错误：

- 忘记写 `url()`
- 路径从 HTML 文件位置开始算，而不是从 CSS 文件位置开始算
- 图片文件名大小写或后缀写错

### 3.3 背景平铺 `background-repeat`

背景图片默认会平铺，也就是重复显示：

```css
body {
    background-image: url("../images/bg.jpg");
    background-repeat: repeat;
}
```

常用值：

| 值 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `repeat` | 关键字 | 默认值 | 横向和纵向都平铺 |
| `no-repeat` | 关键字 | 手动设置 | 不平铺 |
| `repeat-x` | 关键字 | 手动设置 | 只横向平铺 |
| `repeat-y` | 关键字 | 手动设置 | 只纵向平铺 |

常见写法：

```css
body {
    background-image: url("../images/bg.jpg");
    background-repeat: no-repeat;
}
```

### 3.4 背景位置 `background-position`

`background-position` 控制背景图在盒子中的位置：

```css
body {
    background-image: url("../images/bg.jpg");
    background-repeat: no-repeat;
    background-position: center top;
}
```

可以使用方位词：

```css
background-position: left center;
background-position: center top;
background-position: right bottom;
```

也可以使用精确单位：

```css
background-position: 20px 40px;
```

规则：

- 两个方位词的顺序通常可以交换，例如 `left top` 和 `top left`
- 使用两个数值时，第一个是水平位置，第二个是垂直位置
- 只写一个值时，另一个方向通常按居中处理

### 3.5 背景固定 `background-attachment`

`background-attachment` 控制背景图是否跟着页面滚动：

```css
body {
    background-image: url("../images/bg.jpg");
    background-repeat: no-repeat;
    background-position: center top;
    background-attachment: fixed;
}
```

常用值：

| 值 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `scroll` | 关键字 | 默认值 | 背景跟随页面滚动 |
| `fixed` | 关键字 | 手动设置 | 背景相对浏览器窗口固定 |

`fixed` 可以做出背景固定的效果，但不适合滥用。页面文字如果盖在复杂背景上，要特别检查可读性。

### 3.6 背景复合写法 `background`

多个背景属性可以合并：

```css
body {
    background: transparent url("../images/bg.jpg") no-repeat fixed top;
}
```

常见顺序：

```text
background: 背景颜色 背景图片地址 背景平铺 背景滚动 背景位置;
```

初学时建议先分开写：

```css
body {
    background-color: transparent;
    background-image: url("../images/bg.jpg");
    background-repeat: no-repeat;
    background-attachment: fixed;
    background-position: center top;
}
```

确认每个属性作用后，再改成复合写法。

### 3.7 背景色半透明 `rgba()`

```css
div {
    width: 300px;
    height: 300px;
    background: rgba(0, 0, 0, 0.3);
}
```

`rgba()` 前三个值是红、绿、蓝，最后一个值是透明度：

- `0` 表示完全透明
- `1` 表示完全不透明
- `0.3` 表示 30% 不透明

背景半透明只影响背景，不会让盒子里的文字一起透明。

不要用 `opacity: 0.3` 代替背景半透明：

```css
div {
    opacity: 0.3;
}
```

`opacity` 会让整个元素，包括里面的文字一起透明。

### 3.8 背景小案例：图标加文字

这个例子参照 backup 中“成长守护平台”案例。它用背景图片放一个小图标，再通过缩进让文字避开图标。

```html
<h3><a href="#">成长守护平台</a></h3>
```

```css
h3 {
    width: 118px;
    height: 40px;
    font-size: 14px;
    font-weight: 400;
    line-height: 40px;
    background-image: url("../images/icon.png");
    background-repeat: no-repeat;
    background-position: left center;
    text-indent: 2em;
}

h3 a {
    color: #000000;
    text-decoration: none;
}
```

这段代码的关键点：

- `width` 和 `height` 给标题盒子确定范围
- `line-height: 40px` 让单行文字垂直居中
- `background-position: left center` 让图标在左侧垂直居中
- `text-indent: 2em` 让文字向右缩进，不压住图标
- `h3 a` 单独设置链接颜色和取消下划线

### 3.9 背景属性总结

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `background-color` | 颜色名、十六进制、`rgb()`、`rgba()` | 默认 `transparent` | 背景颜色 |
| `background-image` | `none`、`url(...)` | 默认 `none` | 背景图片 |
| `background-repeat` | `repeat`、`no-repeat`、`repeat-x`、`repeat-y` | 默认 `repeat` | 是否平铺 |
| `background-position` | 方位词、长度值、百分比 | 默认 `0% 0%` | 背景位置 |
| `background-attachment` | `scroll`、`fixed` | 默认 `scroll` | 背景是否固定 |
| `background` | 多个背景相关值 | 各子属性有默认值 | 背景复合写法 |

## 4. 本章综合练习

在 `css-demo/index.html` 中写入：

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文字、颜色与背景练习</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <h1>CSS 文字与背景练习</h1>
    <h3><a href="#">成长守护平台</a></h3>
    <p>打开北京、上海与广州的地铁地图，你会看见三张纵横交错的线路网络。</p>
    <p>CSS 可以控制文字的字体、大小、颜色、对齐、缩进、行高和背景。</p>
</body>
</html>
```

在 `css/style.css` 中写入：

```css
body {
    font: 16px/1.5 "Microsoft YaHei", Arial, sans-serif;
    color: #333333;
    background-color: #f5f5f5;
}

h1 {
    font-size: 28px;
    font-weight: 700;
    text-align: center;
}

h3 {
    width: 118px;
    height: 40px;
    font-size: 14px;
    font-weight: 400;
    line-height: 40px;
    background-image: url("../images/icon.png");
    background-repeat: no-repeat;
    background-position: left center;
    text-indent: 2em;
}

h3 a {
    color: #000000;
    text-decoration: none;
}

p {
    text-indent: 2em;
    line-height: 26px;
}
```

## 5. 验证

逐项确认：

1. `body` 的字体和正文颜色生效。
2. `h1` 居中，字号比正文大。
3. `h3` 高度为 `40px`，文字垂直居中。
4. `icon.png` 路径正确时，图标显示在标题左侧。
5. 临时改错背景图路径，正文仍然可读。
6. 段落首行缩进两个字。
7. 段落行距比默认效果更舒展。
8. 链接取消下划线后仍能通过颜色和位置识别。

## 6. 常见错误

| 现象 | 常见原因 | 修正方式 |
| --- | --- | --- |
| 字号没有变化 | `font-size` 漏写单位 | 写成 `font-size: 20px;` |
| 字体没有变化 | 用户电脑没有该字体 | 在后面追加备用字体和通用字体族 |
| 链接仍有下划线 | 没有选中 `a` | 使用 `a { text-decoration: none; }` 或限定范围的链接选择器 |
| 段落没有缩进 | 写成手动空格或选择器没命中 | 使用 `p { text-indent: 2em; }` |
| 背景图不显示 | 路径按 HTML 位置计算了 | 按 CSS 文件所在位置重新写路径 |
| 背景图重复铺满 | 没有设置 `background-repeat` | 添加 `background-repeat: no-repeat;` |
| 半透明后文字也透明 | 使用了 `opacity` | 改用 `background: rgba(...);` |
