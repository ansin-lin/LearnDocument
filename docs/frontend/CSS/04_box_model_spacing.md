# 第四章 盒子模型、边框与间距

## 学习目标

完成本章后，学员应能够：

- 说清楚页面中的“盒子”由内容区、内边距、边框和外边距组成。
- 使用 `width`、`height`、`border`、`padding`、`margin` 控制元素大小和间距。
- 判断一个盒子在页面中实际占用的宽高。
- 处理常见的外边距合并、盒子撑大、列表默认间距等问题。
- 在写页面前先做必要的清除默认样式，减少浏览器默认样式带来的干扰。

## 1. 什么是盒子模型

HTML 负责页面结构，CSS 负责表现。浏览器在显示每一个标签时，都会把它当成一个矩形区域来处理。

这个矩形区域就可以理解为“盒子”。

例如：

- 一个标题是一个盒子。
- 一段文字是一个盒子。
- 一个按钮是一个盒子。
- 一个商品卡片也是一个盒子。

学习盒子模型后，才能解释这些常见问题：

- 为什么设置了 `width: 200px`，页面上看起来却不止 200px？
- 为什么两个盒子之间有空隙？
- 为什么给盒子加了内边距后，整体变宽了？
- 为什么页面顶部和浏览器边缘之间有默认空白？

## 2. 盒子模型的四个部分

一个标准盒子从里到外包括四层：

| 部分 | CSS 属性 | 作用 |
| --- | --- | --- |
| 内容区 | `width`、`height` | 放文字、图片或子元素的区域 |
| 内边距 | `padding` | 内容和边框之间的距离 |
| 边框 | `border` | 盒子的边界线 |
| 外边距 | `margin` | 盒子和其他盒子之间的距离 |

可以先记住一句话：

> `padding` 管盒子里面的空白，`margin` 管盒子外面的空白。

## 3. 内容区：width 和 height

`width` 设置内容区宽度，`height` 设置内容区高度。

```css
.box {
  width: 200px;
  height: 100px;
  background-color: pink;
}
```

对应 HTML：

```html
<div class="box">这是一个盒子</div>
```

页面效果：

- 盒子的内容区宽 200px。
- 盒子的内容区高 100px。
- 背景色会覆盖内容区，也会覆盖后面设置的内边距区域。

注意：如果盒子里面的内容超过了高度，内容不会自动消失，可能会溢出。溢出内容会在第五章继续处理。

## 4. 边框：border

边框由三部分组成：

| 部分 | 示例 | 含义 |
| --- | --- | --- |
| 粗细 | `1px` | 边框的宽度 |
| 样式 | `solid` | 实线 |
| 颜色 | `red` | 边框颜色 |

常用写法：

```css
.box {
  width: 200px;
  height: 100px;
  border: 1px solid red;
}
```

边框样式常见值：

| 值 | 含义 | 使用频率 |
| --- | --- | --- |
| `solid` | 实线 | 最常用 |
| `dashed` | 虚线 | 偶尔用于提示区域 |
| `dotted` | 点线 | 较少 |
| `none` | 无边框 | 常用于清除默认边框 |

也可以单独设置某一条边：

```css
.box {
  border-top: 1px solid #ccc;
  border-bottom: 1px solid #ccc;
}
```

这种写法在导航栏、分割线、列表项中很常见。

### 4.1 圆角边框：border-radius

`border-radius` 用来把盒子的边角变圆。

```css
.card {
  width: 300px;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}
```

观察结果：

- 盒子仍然有边框。
- 四个角从直角变成圆角。

如果想做圆形，可以让盒子宽高相等，并设置较大的圆角：

```css
.circle {
  width: 60px;
  height: 60px;
  border: 2px solid green;
  border-radius: 50%;
}
```

`border-radius` 只改变边角显示效果，不会改变盒子在普通流中的排列方式。

## 5. 表格边框合并

表格默认相邻单元格边框会分开显示，看起来像双线。

```css
table {
  border-collapse: collapse;
}

td,
th {
  border: 1px solid #999;
  padding: 8px 12px;
}
```

`border-collapse: collapse;` 的作用是把相邻边框合并成一条线。

企业页面中虽然不建议大量使用表格做布局，但表格仍然适合展示数据，例如清单、报表、对照表。

## 6. 内边距：padding

`padding` 控制内容和边框之间的距离。

```css
.box {
  width: 200px;
  height: 100px;
  border: 1px solid red;
  padding: 20px;
}
```

观察结果：

- 文字不会紧贴边框。
- 盒子整体看起来更宽、更高。
- 在默认盒模型下，整体宽度不再只是 200px。

### 6.1 padding 的四种写法

| 写法 | 含义 |
| --- | --- |
| `padding: 20px;` | 上、右、下、左都是 20px |
| `padding: 10px 20px;` | 上下 10px，左右 20px |
| `padding: 10px 20px 30px;` | 上 10px，左右 20px，下 30px |
| `padding: 10px 20px 30px 40px;` | 上、右、下、左，顺时针 |

顺序可以记为：从上开始，顺时针。

### 6.2 单独设置某一边

```css
.box {
  padding-left: 20px;
  padding-top: 10px;
}
```

当只需要让文字离左边远一点时，设置 `padding-left` 就够了，不需要同时改四个方向。

## 7. 外边距：margin

`margin` 控制盒子和其他盒子之间的距离。

```css
.one {
  width: 200px;
  height: 100px;
  background-color: pink;
  margin-bottom: 20px;
}

.two {
  width: 200px;
  height: 100px;
  background-color: skyblue;
}
```

观察结果：

- `.one` 和 `.two` 之间有 20px 的距离。
- 这个距离在两个盒子外部，不属于任何一个盒子的内容区。

### 7.1 margin 的写法

`margin` 的简写规则和 `padding` 一样：

```css
.box {
  margin: 10px 20px 30px 40px;
}
```

含义仍然是：上、右、下、左。

也可以单独设置某一边：

```css
.tag {
  margin-left: 4px;
}
```

常见单边写法：

| 属性 | 作用 |
| --- | --- |
| `margin-top` | 设置上外边距 |
| `margin-right` | 设置右外边距 |
| `margin-bottom` | 设置下外边距 |
| `margin-left` | 设置左外边距 |

### 7.2 块级盒子水平居中

固定宽度的块级盒子可以使用左右外边距自动分配：

```css
.box {
  width: 500px;
  margin: 0 auto;
}
```

这句代码非常常用，含义是：

- 上下外边距为 0。
- 左右外边距自动计算。
- 盒子在父元素中水平居中。

注意：`margin: 0 auto;` 对没有固定宽度的普通块级元素通常看不出效果，因为块级元素默认已经占满整行。

## 8. 默认外边距与清除默认样式

很多 HTML 标签自带默认样式，例如：

- `body` 默认有外边距。
- `h1`、`p` 默认有上下外边距。
- `ul` 默认有外边距和内边距。

如果不清除默认样式，初学者会经常遇到“我没有写间距，为什么页面有空白”的问题。

常见清除方式：

```css
* {
  margin: 0;
  padding: 0;
}
```

这段代码适合教学和小型页面练习。真实项目中可能会使用更完整的 reset 或 normalize 文件。

列表还经常需要清除项目符号：

```css
ul {
  list-style: none;
}
```

链接经常需要清除下划线：

```css
a {
  text-decoration: none;
  color: inherit;
}
```

## 9. 外边距合并与父子外边距塌陷

外边距合并是盒模型中很容易让初学者困惑的问题。它主要发生在垂直方向，常见有两个场景：

- 相邻兄弟元素的上下外边距合并。
- 父元素和第一个或最后一个子元素的上下外边距合并，也常被叫作“父子外边距塌陷”。

注意：`padding` 本身不会塌陷。平时说的“塌陷”通常指 `margin` 的现象，不是 `padding`。

### 9.1 相邻兄弟元素的外边距合并

相邻的块级元素上下外边距可能会合并。

```css
.one {
  margin-bottom: 30px;
}

.two {
  margin-top: 20px;
}
```

很多初学者会以为两个盒子之间的距离是 50px，但实际常见结果是 30px。

原因是：垂直方向相邻外边距会取较大的值，而不是简单相加。

### 9.2 父子外边距塌陷

父元素里面有一个子元素，如果父元素没有边框、内边距或其他隔离方式，子元素的 `margin-top` 可能不会把自己和父元素内部拉开，而是把父元素一起向下推。

HTML：

```html
<div class="father">
  <div class="son">子盒子</div>
</div>
```

CSS：

```css
.father {
  width: 300px;
  height: 200px;
  background-color: pink;
}

.son {
  width: 100px;
  height: 100px;
  margin-top: 50px;
  background-color: skyblue;
}
```

初学者的预期通常是：

- 子盒子距离父盒子顶部 50px。
- 父盒子位置不变。

但实际可能看到：

- 子盒子没有在父盒子内部向下移动。
- 父盒子整体被向下推了 50px。

这就是父子外边距塌陷。

### 9.3 处理父子外边距塌陷

常见处理方式有三种。

方式一：给父元素添加内边距。

```css
.father {
  padding-top: 50px;
}

.son {
  margin-top: 0;
}
```

这种写法语义很清楚：父盒子内部顶部留出 50px 空间。

方式二：给父元素添加边框。

```css
.father {
  border: 1px solid transparent;
}
```

这种方式能阻止外边距合并，但为了处理间距专门加透明边框，可读性不如直接使用 `padding`。

方式三：让父元素形成新的布局环境。

```css
.father {
  overflow: hidden;
}
```

这种写法在旧代码中比较常见，但它还会影响溢出内容显示。使用前要确认父元素里没有需要显示到外面的内容。

处理思路：

- 两个盒子之间的距离尽量只由一个方向控制，例如只写 `margin-bottom`。
- 父子之间的内部距离优先考虑给父元素写 `padding`。
- 给父元素加 `padding`、`border` 或合适的布局隔离方式，避免父子外边距合并。
- 在布局复杂时使用 Flex 或 Grid 管理间距，减少依赖外边距。

## 10. box-sizing

默认情况下：

```css
.box {
  width: 200px;
  padding: 20px;
  border: 1px solid red;
}
```

盒子的实际占用宽度是：

```text
200 + 20 * 2 + 1 * 2 = 242px
```

也就是说，`width` 只控制内容区，不包含 `padding` 和 `border`。

这对初学者很容易造成困扰。实际项目中常用下面的写法：

```css
* {
  box-sizing: border-box;
}
```

设置后：

- `width` 包含内容区、内边距和边框。
- 写页面时更容易控制整体宽度。
- 不容易因为加了 `padding` 导致盒子超出父容器。

推荐在练习页面开头写：

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
```

## 11. 综合练习：商品卡片盒子

请使用下面的 HTML：

```html
<div class="card">
  <h3>CSS 入门课程</h3>
  <p>学习选择器、盒子模型和基础布局。</p>
  <a href="#">查看详情</a>
</div>
```

参考 CSS：

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.card {
  width: 300px;
  margin: 50px auto;
  padding: 20px;
  border: 1px solid #ddd;
  background-color: #fff;
}

.card h3 {
  margin-bottom: 12px;
}

.card p {
  margin-bottom: 16px;
  line-height: 1.8;
  color: #666;
}

.card a {
  display: inline-block;
  padding: 8px 16px;
  background-color: #ff6700;
  color: #fff;
  text-decoration: none;
}
```

练习要求：

1. 观察 `.card` 是否在页面中水平居中。
2. 修改 `padding`，观察卡片内部空白变化。
3. 修改 `margin`，观察卡片与浏览器边缘的距离变化。
4. 注释掉 `box-sizing: border-box;`，观察盒子宽度是否发生变化。

## 12. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| 页面四周有空白 | `body` 默认外边距 | 清除默认 `margin` |
| 盒子加 padding 后变宽 | 默认盒模型不包含 padding | 使用 `box-sizing: border-box` |
| 两个盒子间距不是相加结果 | 外边距合并 | 统一只设置一个方向的 margin |
| 子元素设置 `margin-top` 后父元素一起下移 | 父子外边距塌陷 | 父元素使用 `padding`、`border` 或合适的布局隔离方式 |
| 文字贴着边框 | 没有设置 padding | 给盒子增加内边距 |
| 列表前面有圆点和缩进 | `ul` 默认样式 | 设置 `list-style: none; margin: 0; padding: 0;` |

## 本章检查点

请确认你已经能够完成以下操作：

- 能画出内容区、内边距、边框、外边距的关系。
- 能解释 `padding` 和 `margin` 的区别。
- 能说明 `padding` 不会塌陷，父子外边距塌陷属于 `margin` 问题。
- 能计算默认盒模型下盒子的实际宽度。
- 能使用 `box-sizing: border-box;` 控制盒子尺寸。
- 能写出一个有边框、内边距、外边距的卡片样式。
