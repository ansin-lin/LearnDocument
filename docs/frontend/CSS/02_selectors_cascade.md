# 选择器与 CSS 的三大特性

## 本章目标

完成本章后，你可以：

- 使用标签选择器、类选择器、ID 选择器和通配符选择器
- 使用后代选择器、子选择器、并集选择器、伪类选择器、属性选择器和多 class 组合
- 看懂 UI 框架中常见的状态 class、结构伪类和伪元素写法
- 说明层叠、继承和优先级的判断顺序
- 避免把 `!important` 当作默认修复方式

## 1. 基础选择器

### 1.1 标签选择器

标签选择器会选中页面中所有同名标签：

```css
p {
    color: green;
}

div {
    color: pink;
}
```

它适合统一控制某一类 HTML 元素，但不能区分同类标签中的个别元素。

### 1.2 类选择器

类选择器以 `.` 开头，HTML 中通过 `class` 调用：

```css
.red {
    color: red;
}
```

```html
<div class="red">变红色</div>
```

一个元素可以同时使用多个类名：

```css
.red {
    color: red;
}

.font35 {
    font-size: 35px;
}
```

```html
<div class="red font35">zhoujirui</div>
```

类选择器是项目样式中最常用的写法。

### 1.3 多 class 组合

一个元素可以同时有多个 class。CSS 可以分别设置每个 class，也可以选择“同时拥有多个 class 的元素”。

HTML：

```html
<button class="button primary">保存</button>
<button class="button secondary">戻る</button>
```

CSS：

```css
.button {
  padding: 10px 16px;
  border: 1px solid transparent;
}

.primary {
  color: #fff;
  background-color: #1f4e79;
}

.secondary {
  color: #1f4e79;
  background-color: #fff;
  border-color: #1f4e79;
}
```

观察结果：

- 两个按钮都拥有 `.button` 的基础样式。
- `.primary` 和 `.secondary` 再分别控制不同外观。

也可以写成组合选择器：

```css
.button.primary {
  background-color: #1f4e79;
}
```

`.button.primary` 中间没有空格，表示选择同时拥有 `button` 和 `primary` 两个 class 的同一个元素。

如果写成：

```css
.button .primary {
  background-color: #1f4e79;
}
```

中间有空格，就变成后代选择器，表示选择 `.button` 里面的 `.primary` 子孙元素。两者含义不同。

UI 框架中经常出现多个 class 叠加，例如：

```html
<button class="btn btn-primary active">保存</button>
```

读这类代码时，先把它拆成：

- 基础类：`btn`
- 外观类：`btn-primary`
- 状态类：`active`

这样更容易判断每个 class 分别负责什么。

### 1.4 ID 选择器

ID 选择器以 `#` 开头：

```css
#pink {
    color: pink;
}
```

```html
<div id="pink">迈克尔·杰克逊</div>
```

ID 在同一个页面中应保持唯一。普通样式优先用类选择器，避免 ID 选择器导致后续覆盖成本过高。

### 1.5 通配符选择器

通配符选择器选中所有元素：

```css
* {
    margin: 0;
    padding: 0;
}
```

它常用于简单初始化，但不要随意写会影响所有元素的样式。

## 2. 复合选择器

### 2.1 后代选择器

后代选择器用空格连接：

```css
ol li {
    color: pink;
}

ol a {
    color: red;
}

.nav li a {
    color: yellow;
}
```

它会选中某个元素内部的所有匹配后代。层级不要写得过深，否则 HTML 结构稍微变化就容易失效。

### 2.2 子选择器

子选择器只选择最近一级子元素：

```css
.nav > a {
    color: red;
}
```

```html
<div class="nav">
    <a href="#">我是儿子</a>
    <p>
        <a href="#">我是孙子</a>
    </p>
</div>
```

上面的规则只会选中 `.nav` 下面第一层的 `a`。

### 2.3 并集选择器

并集选择器用逗号连接，用来给多个选择器设置相同样式：

```css
div,
p,
.pig li {
    color: pink;
}
```

逗号后建议换行，便于阅读。

### 2.4 链接伪类和焦点伪类

```css
a {
    color: gray;
}

a:hover {
    color: red;
}

input:focus {
    background-color: yellow;
}
```

`a:hover` 表示鼠标经过链接时的状态，`input:focus` 表示输入框获得焦点时的状态。企业页面中不要只设置鼠标悬停，也要保留键盘焦点的可见反馈。

常见状态伪类：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `:link` | 还没有访问过的链接 | 设置普通链接样式 |
| `:visited` | 已经访问过的链接 | 区分已访问链接 |
| `:hover` | 鼠标经过 | 链接、按钮悬停效果 |
| `:active` | 鼠标按下但还没松开 | 按钮点击瞬间反馈 |
| `:focus` | 元素获得焦点 | 输入框、按钮、链接的键盘操作状态 |
| `:focus-visible` | 键盘操作产生的明显焦点 | 保留键盘用户的焦点轮廓 |
| `:focus-within` | 元素内部有子元素获得焦点 | 表单区域、搜索框组合高亮 |
| `:enabled` | 表单控件可用 | 可点击按钮、可输入文本框 |
| `:disabled` | 表单控件被禁用 | 禁用按钮、不可编辑输入框 |
| `:checked` | 单选框或复选框被选中 | 单选、复选状态样式 |
| `:required` | 必填表单控件 | 必填输入框提示 |
| `:optional` | 非必填表单控件 | 可选输入框提示 |
| `:valid` | 表单控件当前输入合法 | 输入正确时的反馈 |
| `:invalid` | 表单控件当前输入不合法 | 输入错误时的反馈 |
| `:placeholder-shown` | 输入框正在显示占位提示 | 未输入内容时的样式 |

示例：

```css
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

input:checked {
  outline: 2px solid #1f4e79;
}

.search-box:focus-within {
  border-color: #1f4e79;
}
```

状态伪类是理解表单、按钮和 UI 组件状态的基础。

### 2.5 属性选择器

属性选择器根据 HTML 属性选择元素。它使用方括号 `[]`。

最简单的写法是：选择带有某个属性的元素。

```css
input[required] {
  border-color: red;
}
```

对应 HTML：

```html
<input type="text" name="userName" required>
<input type="text" name="memo">
```

观察结果：

- 第一个输入框带有 `required`，会被选中。
- 第二个输入框没有 `required`，不会被选中。

也可以选择“属性值等于某个值”的元素。比如只选中日期输入框：

```css
input[type="date"] {
  border-color: #1f4e79;
}
```

对应 HTML：

```html
<input type="date" name="startDate">
<input type="text" name="userName">
```

观察结果：

- 第一个输入框的 `type` 是 `date`，会被选中。
- 第二个输入框的 `type` 是 `text`，不会被选中。

也可以选择会打开新窗口的链接：

```css
a[target="_blank"] {
  color: #1f4e79;
}
```

对应 HTML：

```html
<a href="manual.pdf" target="_blank">操作手册</a>
<a href="index.html">首页</a>
```

第一个链接带有 `target="_blank"`，会被选中。

常见属性选择器：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `[required]` | 选择带有 `required` 属性的元素 | 必填控件 |
| `[disabled]` | 选择带有 `disabled` 属性的元素 | 禁用按钮、禁用输入框 |
| `[checked]` | 选择 HTML 中写了 `checked` 的元素 | 默认选中的单选框、复选框 |
| `input[type="text"]` | 选择文本输入框 | 统一普通文本框样式 |
| `input[type="date"]` | 选择日期输入框 | 统一日期控件样式 |
| `input[type="search"]` | 选择搜索输入框 | 统一搜索控件样式 |
| `input[name="email"]` | 选择 `name` 为 `email` 的输入框 | 对指定字段做临时样式或调试 |
| `a[target="_blank"]` | 选择新窗口打开的链接 | 给外部链接添加提示样式 |
| `img[alt]` | 选择带有替代文本的图片 | 检查或统一图片样式 |
| `[data-status="error"]` | 选择自定义状态为错误的元素 | JavaScript 改变状态后配合 CSS 显示 |

属性选择器适合处理 HTML 已经带有明确状态或类型的元素。不要为了少写类名而滥用复杂属性选择器；普通模块样式仍然优先使用类选择器。

项目代码中还可能看到下面这种写法：

```css
.global-nav a[aria-current="page"] {
  font-weight: bold;
}

[aria-invalid="true"] {
  border-color: #b00020;
}
```

这仍然是属性选择器。`aria-current="page"` 表示当前页面链接，`aria-invalid="true"` 表示当前控件校验失败。初学时先掌握方括号选择属性的规则，再看这类无障碍属性会更容易。

### 2.6 结构伪类

结构伪类根据元素在父元素中的位置选择元素。

```html
<ul class="news-list">
  <li>お知らせ 1</li>
  <li>お知らせ 2</li>
  <li>お知らせ 3</li>
</ul>
```

```css
.news-list li:first-child {
  font-weight: bold;
}

.news-list li:last-child {
  border-bottom: none;
}

.news-list li:nth-child(2) {
  color: red;
}
```

常见结构伪类：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `:first-child` | 第一个子元素 | 第一项特殊样式 |
| `:last-child` | 最后一个子元素 | 去掉最后一项边框或外边距 |
| `:nth-child(2)` | 第 2 个子元素 | 指定某一项 |
| `:nth-child(odd)` | 奇数项 | 表格斑马纹 |
| `:nth-child(even)` | 偶数项 | 表格斑马纹 |

结构伪类适合处理列表和表格的规律样式。不要用它表达业务状态；业务状态更适合使用 class 或属性。

### 2.7 伪元素

伪元素用来选择元素中的某个“虚拟部分”，常见写法是双冒号 `::`。

```css
.required::after {
  content: " *";
  color: red;
}
```

对应 HTML：

```html
<label class="required" for="user-name">氏名</label>
```

观察结果：

- 页面上会在“氏名”后面显示红色星号。
- HTML 中没有真实写入这个星号。

常见伪元素：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `::before` | 元素内容前面的虚拟内容 | 图标、装饰符号 |
| `::after` | 元素内容后面的虚拟内容 | 必填星号、清除浮动 |

初学阶段重点是能看懂。不要用伪元素放重要业务文字，因为它不是真正的 HTML 内容。

## 3. CSS 的三大特性

CSS 解决样式冲突时，最常用的判断依据有三个：层叠性、继承性、优先级。看不懂样式为什么生效时，通常就从这三个方向排查。

### 3.1 层叠性

给同一个选择器设置相同样式时，后面的规则会覆盖前面的规则：

```css
div {
    color: red;
}

div {
    color: pink;
}
```

最终 `div` 显示为粉色。样式冲突时，源码顺序是判断因素之一。

### 3.2 继承性

子元素会继承父元素的某些样式。继承通常发生在“文字相关”的属性上，因为子元素里的文字一般应该和父元素保持一致。

```css
body {
    font: 12px/1.5 "Microsoft YaHei";
    color: pink;
}
```

如果子元素没有单独设置行高，会继承父元素的 `1.5`。实际行高会按子元素自己的字号计算。

常见会继承的属性：

| 属性 | 作用 | 示例 |
| --- | --- | --- |
| `color` | 文字颜色 | 父元素设置文字颜色，内部普通文字跟着变化 |
| `font-family` | 字体 | 页面统一设置微软雅黑、Arial 等字体 |
| `font-size` | 字号 | 父元素设置字号，内部文字默认沿用 |
| `font-weight` | 字重 | 父元素加粗，内部文字默认加粗 |
| `font-style` | 字体样式 | 父元素设置斜体，内部文字默认斜体 |
| `line-height` | 行高 | 页面统一文字行距 |
| `text-align` | 文本对齐 | 父元素居中，内部行内内容跟着居中 |
| `text-indent` | 首行缩进 | 段落内部文字首行缩进 |
| `letter-spacing` | 字符间距 | 统一文字疏密 |
| `word-spacing` | 单词间距 | 英文单词之间的间距 |
| `visibility` | 可见性 | 父元素隐藏时，子元素也不可见 |

常见不会继承的属性：

| 属性类型 | 示例 | 原因 |
| --- | --- | --- |
| 盒模型 | `width`、`height`、`padding`、`margin`、`border` | 每个盒子的尺寸和间距通常需要单独计算 |
| 背景 | `background-color`、`background-image` | 子元素默认背景透明，看起来像继承，实际不是继承 |
| 布局 | `display`、`position`、`float`、`flex`、`grid` | 每个元素如何排列需要单独决定 |

判断继承时可以记住一句话：文字样式多数会继承，盒子尺寸、间距、边框、背景和布局多数不会继承。

### 3.3 优先级

选择器不同，则根据优先级判断。可以把优先级理解为四组数字，从左到右比较：

| 选择器 | 可接受的值 | 优先等级 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- | --- |
| 继承或通配符 | `*`、继承值 | `0,0,0,0` | 权重最低 | 提供基础样式 |
| 标签选择器 | `p`、`div` | `0,0,0,1` | 常用 | 控制同类元素 |
| 伪元素 | `::before`、`::after` | `0,0,0,1` | 能看懂 | 添加装饰性内容 |
| 类选择器、属性选择器、伪类选择器 | `.nav`、`[required]`、`:hover` | `0,0,1,0` | 常用 | 控制模块和状态 |
| ID 选择器 | `#demo` | `0,1,0,0` | 谨慎使用 | 优先级高 |
| 行内样式 | `style=""` | `1,0,0,0` | 不作为主线 | 优先级更高 |
| `!important` | `color: red !important` | 高于普通优先级 | 不推荐常规使用 | 强行提高优先级 |

复合选择器需要计算优先级，把每一部分的权重相加：

| 复合选择器 | 计算过程 | 最终优先等级 |
| --- | --- | --- |
| `div ul li` | 3 个标签选择器 | `0,0,0,3` |
| `.nav li` | 1 个类选择器 + 1 个标签选择器 | `0,0,1,1` |
| `.nav li a:hover` | 1 个类选择器 + 1 个伪类选择器 + 2 个标签选择器 | `0,0,2,2` |
| `input[required]` | 1 个标签选择器 + 1 个属性选择器 | `0,0,1,1` |
| `input[type="date"]` | 1 个标签选择器 + 1 个属性选择器 | `0,0,1,1` |
| `.button.primary` | 2 个类选择器 | `0,0,2,0` |
| `.list li:first-child` | 1 个类选择器 + 1 个伪类选择器 + 1 个标签选择器 | `0,0,2,1` |
| `#demo .nav a` | 1 个 ID 选择器 + 1 个类选择器 + 1 个标签选择器 | `0,1,1,1` |

权重不会进位。比如 `0,0,0,10` 仍然低于 `0,0,1,0`。

示例：

```css
.test {
    color: red;
}

#demo {
    color: green;
}
```

```html
<div class="test" id="demo">你笑起来真好看</div>
```

最终显示绿色，因为 ID 选择器优先级高于类选择器。

## 4. 验证

打开开发者工具选中目标元素，确认：

- 哪些选择器命中了元素
- 哪条声明被划掉
- 最终生效的颜色来自哪条规则
- 是否可以通过调整选择器或顺序解决，而不是直接加 `!important`
- 属性选择器是否真的匹配了 HTML 中的属性和值
- 多 class 组合是否写成了无空格的 `.button.primary`
- 伪类和伪元素是否只是用于状态或装饰，没有替代真实 HTML 内容
