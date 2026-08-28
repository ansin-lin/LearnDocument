# 第五章 元素显示模式、普通流与溢出

## 学习目标

完成本章后，学员应能够：

- 区分块级元素、行内元素和行内块元素。
- 使用 `display` 改变元素的显示模式。
- 理解普通流中元素默认从上到下、从左到右排列。
- 使用 `overflow` 处理内容超出盒子的情况。
- 写出常见表单控件的基础样式。
- 了解浮动的历史作用，并知道新页面优先使用 Flex 或 Grid。

## 1. 为什么要学习显示模式

同样是 HTML 标签，有的会独占一行，有的会和其他内容排在同一行。

例如：

```html
<div>大毛</div>
<div>二毛</div>
<span>大毛</span>
<span>二毛</span>
```

页面现象通常是：

- 两个 `div` 会上下排列。
- 两个 `span` 会在同一行排列。

这不是因为文字不同，而是因为元素默认显示模式不同。

## 2. 三种常见显示模式

| 显示模式 | 常见标签 | 特点 |
| --- | --- | --- |
| 块级元素 | `div`、`p`、`h1`、`ul`、`li` | 独占一行，可以设置宽高 |
| 行内元素 | `span`、`a`、`strong` | 一行可以放多个，默认不能直接设置宽高 |
| 行内块元素 | `input`、`button`、`select`、`textarea` | 一行可以放多个，也可以设置宽高 |

### 2.1 块级元素

常见默认块级元素：

| 元素 | 常见用途 | 默认表现 |
| --- | --- | --- |
| `div` | 通用容器 | 独占一行 |
| `p` | 段落 | 独占一行，并带有默认上下外边距 |
| `h1` ~ `h6` | 标题 | 独占一行，并有默认字号和外边距 |
| `ul`、`ol` | 列表容器 | 独占一行，并有默认内边距或外边距 |
| `li` | 列表项 | 默认作为列表项显示 |
| `header`、`nav`、`main`、`section`、`article`、`aside`、`footer` | 语义化区域 | 独占一行，适合做页面结构 |
| `form` | 表单区域 | 独占一行 |
| `blockquote` | 引用内容 | 独占一行，并有默认缩进 |
| `pre` | 保留格式文本 | 独占一行，并保留空格和换行 |

```css
div {
  width: 200px;
  height: 50px;
  background-color: pink;
}
```

块级元素的主要特点：

- 默认独占一行。
- 如果不设置宽度，默认宽度通常会占满父元素。
- 可以设置 `width`、`height`、`padding`、`margin`。

### 2.2 行内元素

常见默认行内元素：

| 元素 | 常见用途 | 默认表现 |
| --- | --- | --- |
| `span` | 一段文字中的局部样式 | 不独占一行 |
| `a` | 链接 | 不独占一行，默认有链接颜色和下划线 |
| `strong`、`b` | 加粗文字 | 不独占一行 |
| `em`、`i` | 强调或斜体文字 | 不独占一行 |
| `label` | 表单标签文字 | 不独占一行，常和输入控件一起使用 |
| `small` | 辅助说明文字 | 不独占一行，默认字号较小 |
| `code` | 行内代码 | 不独占一行，常用于技术文本 |
| `br` | 强制换行 | 不显示盒子，只产生换行效果 |

```css
span {
  width: 200px;
  height: 50px;
  background-color: pink;
}
```

对 `span` 直接设置宽高，通常不会得到预期效果。

行内元素的主要特点：

- 不独占一行。
- 宽高主要由内容撑开。
- 常用于一段文字中的局部样式，例如标红、加粗、链接。

### 2.3 行内块元素

常见默认行内块元素主要是表单控件：

| 元素 | 常见用途 | 默认表现 |
| --- | --- | --- |
| `input` | 文本框、日期框、搜索框、单选框、复选框 | 可以和文字在一行，也可以设置宽高 |
| `button` | 按钮 | 可以和文字在一行，也可以设置宽高 |
| `select` | 下拉框 | 可以和文字在一行，也可以设置宽高 |
| `textarea` | 多行文本框 | 可以设置宽高，默认可以拉伸 |

`img` 默认不是 `inline-block`，而是 `inline`。但是图片属于“替换元素”，可以设置 `width` 和 `height`，视觉表现很像行内块元素，所以入门阶段经常和行内块一起理解。

```css
img {
  width: 120px;
  height: 80px;
}
```

行内块元素的特点：

- 可以和其他元素在一行显示。
- 可以设置宽高。
- 常见于表单输入框、按钮类效果；图片虽然默认不是 `inline-block`，但也可以设置宽高。

## 3. display 属性

`display` 可以改变元素的显示模式。

| 值 | 含义 | 常见用途 |
| --- | --- | --- |
| `block` | 显示为块级元素 | 让链接变成可点击的大区域 |
| `inline` | 显示为行内元素 | 很少用于布局 |
| `inline-block` | 显示为行内块元素 | 做按钮、标签、菜单项 |
| `none` | 不显示元素 | 隐藏元素且不占位置 |
| `flex` | 弹性布局容器 | 一维布局，后面章节详细讲 |
| `grid` | 网格布局容器 | 二维布局，后面章节详细讲 |

### 3.1 把链接做成菜单按钮

备份示例中的小米侧边栏就使用了类似思路：把 `a` 标签变成块级元素，让整行都能点击。

```html
<a href="#">手机 电话卡</a>
<a href="#">电视 盒子</a>
<a href="#">笔记本 平板</a>
```

```css
a {
  display: block;
  width: 230px;
  height: 40px;
  padding-left: 30px;
  line-height: 40px;
  color: #fff;
  text-decoration: none;
  background-color: #55585a;
}

a:hover {
  background-color: #ff6700;
}
```

需要观察的点：

- `a` 原本是行内元素，默认不会独占一行。
- 设置 `display: block;` 后，每个链接独占一行。
- 设置 `height` 和 `line-height` 后，文字在每一行中垂直居中。
- 鼠标移上去时，背景色变化。

## 4. 普通流

普通流是浏览器默认的页面排列方式。

在没有定位、浮动、Flex、Grid 等特殊布局时：

- 块级元素从上到下排列。
- 行内元素和行内块元素从左到右排列。
- 一行放不下时，会自动换行。

示例：

```html
<div class="box">盒子一</div>
<div class="box">盒子二</div>
<span>文字一</span>
<span>文字二</span>
```

```css
.box {
  width: 200px;
  height: 60px;
  margin-bottom: 10px;
  background-color: pink;
}
```

观察结果：

- 两个 `.box` 上下排列。
- 两个 `span` 排在同一行。

普通流是学习布局的基础。后面的 Flex、Grid、定位，本质上都是在普通流基础上改变元素排列方式。

## 5. line-height 的常见用途

当一行文字的高度和盒子高度相同，文字看起来会垂直居中。

```css
.nav a {
  display: block;
  height: 40px;
  line-height: 40px;
}
```

这在导航栏、按钮、侧边菜单中很常见。

注意：

- 这种方法适合单行文字。
- 多行文字不要依赖 `line-height = height` 做垂直居中。
- 多行内容更适合使用 `padding` 或 Flex。

## 6. overflow 内容溢出

当内容超过盒子的宽度或高度时，就会发生溢出。

```css
.box {
  width: 200px;
  height: 80px;
  border: 1px solid red;
}
```

如果盒子里放很多文字，文字可能会超出边框。

### 6.1 overflow 常见值

| 值 | 效果 |
| --- | --- |
| `visible` | 默认值，内容超出仍然显示 |
| `hidden` | 超出部分隐藏 |
| `scroll` | 始终显示滚动条 |
| `auto` | 内容超出时才显示滚动条 |

示例：

```css
.box {
  width: 200px;
  height: 80px;
  border: 1px solid red;
  overflow: auto;
}
```

`overflow: auto;` 在后台管理页面、弹窗内容区域、表格外层容器中很常见。

### 6.2 单行文字省略号

单行文字过长时，可以使用省略号：

```css
.title {
  width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
```

三行代码分别负责：

- `white-space: nowrap;`：文字不换行。
- `overflow: hidden;`：超出部分隐藏。
- `text-overflow: ellipsis;`：隐藏处显示省略号。

这类写法常用于商品标题、文章标题、列表项标题。

### 6.3 长文本换行：word-break

有些内容很长，中间没有空格，例如受付番号、URL、长英文字符串。如果不处理，可能会撑破盒子。

```css
.receipt-number {
  word-break: break-word;
}
```

作用：

- 当一段长文本超出容器宽度时，允许在合适位置断开换行。
- 避免整个页面因为一段长文本出现横向滚动。

它常用于编号、备注、申请理由、表格单元格等区域。

注意：普通中文和日文文本本身通常比较容易换行，`word-break` 更多是为长编号、英文和符号混合内容兜底。

### 6.4 表格单元格垂直对齐：vertical-align

表格内容较多时，单元格默认对齐方式可能看起来不统一。可以使用 `vertical-align` 控制单元格内容在垂直方向的位置。

```css
th,
td {
  vertical-align: top;
}
```

作用：

- 表格单元格内容从顶部开始对齐。
- 当某一列内容换行较多时，其他列不会看起来居中悬空。

`vertical-align` 在普通块级布局中不是主要对齐方式。普通盒子布局优先使用 Flex 或 Grid；表格单元格对齐时再使用它。

## 7. 鼠标样式 cursor

`cursor` 用来设置鼠标移动到元素上时显示的指针样式。

```css
.button {
  cursor: pointer;
}
```

常见值：

| 值 | 含义 | 常见用途 |
| --- | --- | --- |
| `auto` | 浏览器默认样式 | 默认值 |
| `pointer` | 手型指针 | 可点击按钮、链接样式元素 |
| `not-allowed` | 禁止操作 | 禁用按钮 |

注意：鼠标样式只能提示用户“看起来能不能点”，不能真正控制权限或业务逻辑。按钮是否能提交，仍然要由 HTML 属性或 JavaScript 逻辑控制。

## 8. 文本域拉伸 resize

`textarea` 默认可能允许用户拖动改变大小。项目中可以根据页面布局决定是否允许拉伸。

```css
textarea {
  resize: vertical;
}
```

常见值：

| 值 | 含义 | 常见用途 |
| --- | --- | --- |
| `both` | 横向和纵向都可拉伸 | 浏览器默认可能采用 |
| `vertical` | 只允许纵向拉伸 | 表单备注、申请理由 |
| `none` | 不允许拉伸 | 固定布局区域 |

在新人练习中，推荐使用 `resize: vertical;`。这样用户可以增加输入高度，但不会把页面横向撑乱。

## 9. 表单控件常见样式

企业后台和业务系统中，表单样式非常常见。新人至少要能统一设置输入框、下拉框、文本域和按钮的基础外观。

HTML：

```html
<div class="form-field">
  <label for="reason">申請理由</label>
  <textarea id="reason" name="reason"></textarea>
</div>
```

CSS：

```css
.form-field {
  margin-bottom: 20px;
}

.form-field label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.form-field input,
.form-field select,
.form-field textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}

.form-field textarea {
  resize: vertical;
}
```

这段样式做了几件事：

- `label` 设置为块级元素，让标签和控件上下排列。
- 控件宽度统一为父元素的 `100%`。
- `padding` 让输入内容不贴边。
- `border` 和 `border-radius` 统一边框。
- `textarea` 只允许纵向拉伸，避免破坏页面宽度。

错误状态可以和属性选择器配合：

```css
[aria-invalid="true"] {
  border-color: #b00020;
  background-color: #fff5f5;
}
```

按钮禁用状态可以和伪类配合：

```css
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
```

这些写法是后续使用 UI 框架时的前置知识。即使项目使用现成组件，也经常需要看懂输入框、错误状态、禁用状态和焦点状态是怎么来的。

## 10. 浮动 float

浮动早期常用于让多个盒子横向排列。

```css
.box {
  float: left;
  width: 200px;
  height: 100px;
  margin-right: 10px;
  background-color: pink;
}
```

现在新页面通常优先使用 Flex 或 Grid，原因是：

- Flex 和 Grid 更容易控制对齐、间距和换行。
- 浮动会脱离普通流，父元素高度可能塌陷。
- 浮动更适合了解和维护旧页面，不建议作为新布局主线。

如果维护旧页面时看到：

```css
.clearfix::after {
  content: "";
  display: block;
  clear: both;
}
```

它通常是在解决浮动导致的父元素高度塌陷问题。

本课程后续布局主线会使用 Flex 和 Grid。

## 11. 综合练习：侧边栏菜单

请使用下面的 HTML：

```html
<div class="menu">
  <a href="#">手机 电话卡</a>
  <a href="#">电视 盒子</a>
  <a href="#">笔记本 平板</a>
  <a href="#">出行 穿戴</a>
</div>
```

参考 CSS：

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.menu {
  width: 230px;
  margin: 50px auto;
}

.menu a {
  display: block;
  height: 42px;
  padding-left: 30px;
  line-height: 42px;
  color: #fff;
  text-decoration: none;
  background-color: #55585a;
}

.menu a:hover {
  background-color: #ff6700;
}
```

练习要求：

1. 删除 `display: block;`，观察链接是否仍然上下排列。
2. 修改 `line-height`，观察文字垂直位置变化。
3. 给 `.menu a` 添加 `overflow: hidden;`，再放入很长的菜单文字观察效果。
4. 给 `.menu a` 添加 `cursor: pointer;`，观察鼠标经过时的指针变化。

## 12. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| 给 `span` 设置宽高没效果 | `span` 默认是行内元素 | 改为 `display: inline-block` 或 `block` |
| 链接背景只包住文字 | `a` 默认是行内元素 | 菜单链接常设为 `display: block` |
| 文字没有垂直居中 | `line-height` 没有和单行盒子高度配合 | 单行文字可设置相同高度 |
| 内容跑出盒子 | 没有处理溢出 | 使用 `overflow` |
| 长编号撑破表格 | 没有处理长文本换行 | 使用 `word-break` 或表格横向滚动 |
| 禁用按钮看起来还能点 | 鼠标样式没有提示 | 可配合 `cursor: not-allowed` |
| 文本域横向拉伸破坏布局 | 默认允许横向改变大小 | 使用 `resize: vertical` |
| 表单控件宽度不一致 | 没有统一设置控件样式 | 给 `input`、`select`、`textarea` 设置共同规则 |
| 错误控件没有明显提示 | 只显示文字，不改控件状态 | 配合 `[aria-invalid="true"]` 设置错误样式 |
| 新布局大量使用 float | 沿用旧写法 | 新页面优先使用 Flex 或 Grid |

## 本章检查点

请确认你已经能够完成以下操作：

- 能区分块级、行内、行内块元素。
- 能说明 `display: block` 对链接菜单的作用。
- 能解释普通流中元素默认排列方式。
- 能使用 `overflow` 处理内容超出。
- 能写出单行文字省略号效果。
- 能使用 `word-break` 处理长编号换行。
- 能使用 `cursor` 和 `resize` 处理基础交互提示。
- 能写出一组基础表单控件样式。
