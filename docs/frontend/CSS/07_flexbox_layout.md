# 第七章 Flex 弹性布局

## 学习目标

完成本章后，学员应能够：

- 理解 Flex 是用来处理“一行或一列元素排列”的布局方式。
- 使用 `display: flex;` 创建弹性容器。
- 使用主轴、侧轴解释元素排列方向。
- 使用 `justify-content`、`align-items` 控制对齐。
- 使用 `gap`、`flex-wrap`、`flex` 完成常见导航栏、卡片列表布局。

## 1. 为什么需要 Flex

如果要把几个盒子横向排列，早期经常使用浮动或行内块。

这些写法能实现效果，但会遇到很多细节问题：

- 需要清除浮动。
- 垂直居中不方便。
- 多个元素间距不好统一。
- 容器宽度变化时换行控制麻烦。

Flex 的作用是让父元素直接管理子元素的排列。

最常见的使用场景：

- 导航栏横向排列。
- 按钮左右分布。
- 卡片列表一行多个。
- 图标和文字垂直居中。
- 页面顶部左侧 logo、右侧菜单。

## 2. 创建 Flex 容器

只要给父元素设置：

```css
.box {
  display: flex;
}
```

它的直接子元素就会成为 Flex 项目。

示例：

```html
<div class="box">
  <div>大毛</div>
  <div>二毛</div>
  <div>三毛</div>
</div>
```

```css
.box {
  display: flex;
  width: 600px;
  height: 200px;
  background-color: #eee;
}

.box div {
  width: 100px;
  height: 100px;
  background-color: pink;
}
```

观察结果：

- 三个子盒子不再上下排列。
- 它们会默认从左到右排成一行。

注意：Flex 只直接影响“亲儿子”。孙子元素不会自动成为 Flex 项目。

## 3. 主轴和侧轴

Flex 中有两个方向：

- 主轴：项目主要排列的方向。
- 侧轴：和主轴垂直的方向。

默认情况下：

- 主轴是从左到右。
- 侧轴是从上到下。

很多 Flex 属性都和轴有关。理解主轴后，`justify-content` 才不会记混。

## 4. flex-direction 排列方向

`flex-direction` 用来改变主轴方向。

| 值 | 效果 |
| --- | --- |
| `row` | 默认值，从左到右 |
| `row-reverse` | 从右到左 |
| `column` | 从上到下 |
| `column-reverse` | 从下到上 |

示例：

```css
.box {
  display: flex;
  flex-direction: column;
}
```

设置为 `column` 后，主轴变成竖直方向，子元素会上下排列。

## 5. justify-content 主轴对齐

`justify-content` 控制项目在主轴上的对齐方式。

```css
.box {
  display: flex;
  justify-content: center;
}
```

常用值：

| 值 | 效果 |
| --- | --- |
| `flex-start` | 靠主轴起点 |
| `flex-end` | 靠主轴终点 |
| `center` | 居中 |
| `space-between` | 两端贴边，中间平均分配 |
| `space-around` | 每个项目两侧都有间距 |
| `space-evenly` | 所有间距完全相等 |

### 5.1 导航栏两端对齐

```html
<div class="header">
  <div class="logo">Logo</div>
  <div class="nav">首页 课程 联系</div>
</div>
```

```css
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 20px;
  background-color: #f5f5f5;
}
```

观察结果：

- Logo 在左侧。
- 导航在右侧。
- 两者在垂直方向居中。

## 6. align-items 侧轴对齐

`align-items` 控制项目在侧轴上的对齐方式。

常用值：

| 值 | 效果 |
| --- | --- |
| `stretch` | 默认值，拉伸填满侧轴 |
| `flex-start` | 靠侧轴起点 |
| `flex-end` | 靠侧轴终点 |
| `center` | 侧轴居中 |
| `baseline` | 按文字基线对齐 |

最常用写法：

```css
.box {
  display: flex;
  align-items: center;
}
```

这句经常用来做垂直居中。

## 7. gap 间距

`gap` 用来设置 Flex 项目之间的间距。

```css
.box {
  display: flex;
  gap: 20px;
}
```

和给每个子元素写 `margin-right` 相比，`gap` 更直接：

- 不需要处理最后一个元素的右边距。
- 横向、纵向间距都可以统一管理。
- 在 Flex 和 Grid 中都能使用。

也可以分别设置：

```css
.box {
  row-gap: 20px;
  column-gap: 30px;
}
```

## 8. flex-wrap 换行

默认情况下，Flex 项目会尽量挤在一行。

如果希望一行放不下时自动换行：

```css
.box {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}
```

常用于商品列表、图片列表、课程卡片列表。

示例：

```css
.list {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.item {
  width: 200px;
  height: 120px;
  background-color: pink;
}
```

## 9. flex 子项伸缩

`flex` 设置子项如何分配父容器剩余空间。它写在 Flex 子项上，不是写在父容器上。

`flex` 是一个复合属性，可以同时控制三个值：

```css
.item {
  flex: flex-grow flex-shrink flex-basis;
}
```

| 组成部分 | 可接受的值 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `flex-grow` | 非负数字，例如 `0`、`1`、`2` | `0` | 有剩余空间时，是否放大以及按什么比例放大 |
| `flex-shrink` | 非负数字，例如 `0`、`1` | `1` | 空间不够时，是否缩小以及按什么比例缩小 |
| `flex-basis` | 长度、百分比或 `auto`，例如 `200px`、`30%`、`auto` | `auto` | 分配空间前，子项在主轴方向上的基础尺寸 |

常见写法：

| 写法 | 展开理解 | 常见用途 |
| --- | --- | --- |
| `flex: 1` | 通常可理解为 `1 1 0%` | 多个子项平均分配剩余空间 |
| `flex: 2` | 通常可理解为 `2 1 0%` | 当前子项占更多剩余空间 |
| `flex: none` | `0 0 auto` | 固定内容尺寸，不放大也不缩小 |
| `flex: auto` | `1 1 auto` | 先按自身尺寸计算，再参与伸缩 |
| `flex: 0 0 200px` | 不放大、不缩小，基础宽度 200px | 固定侧边栏、固定按钮宽度 |
| `flex: 1 1 300px` | 基础宽度 300px，可以放大也可以缩小 | 自适应卡片、内容区 |

初学阶段最常用的是 `flex: 1` 和 `flex: 0 0 固定宽度`。

```css
.main {
  display: flex;
}

.left {
  flex: 0 0 200px;
}

.right {
  flex: 1;
}
```

含义：

- `.left` 使用 `flex: 0 0 200px`，表示不放大、不缩小，基础宽度固定为 200px。
- `.right` 使用 `flex: 1`，表示占用剩余空间。

这种结构常用于后台页面：

- 左侧菜单固定宽度。
- 右侧内容区域自适应。

如果有三个子项都写 `flex: 1`，它们会平均分配空间：

```css
.card {
  flex: 1;
}
```

如果希望其中一个子项更宽，可以设置更大的数字：

```css
.main-card {
  flex: 2;
}

.side-card {
  flex: 1;
}
```

这里 `.main-card` 分到的剩余空间大约是 `.side-card` 的 2 倍。

## 10. 综合练习：课程卡片列表

HTML：

```html
<div class="course-list">
  <div class="course-card">HTML 基础</div>
  <div class="course-card">CSS 基础</div>
  <div class="course-card">JavaScript 入门</div>
  <div class="course-card">项目练习</div>
</div>
```

CSS：

```css
.course-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  width: 680px;
  margin: 50px auto;
}

.course-card {
  width: 320px;
  height: 120px;
  padding: 20px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
}
```

练习要求：

1. 删除 `display: flex;`，观察卡片排列变化。
2. 修改 `gap`，观察卡片之间的距离。
3. 删除 `flex-wrap: wrap;`，观察容器宽度不足时卡片如何表现。
4. 把 `.course-card` 的宽度改为 `200px`，观察一行能放几个。

## 11. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| Flex 属性写了没效果 | 属性写在了子元素上 | `display: flex` 应写在父元素上 |
| `justify-content` 方向不对 | 没有判断主轴方向 | 先看 `flex-direction` |
| 垂直居中失败 | 只设置了 `text-align` | 使用 `align-items: center` |
| 子元素不换行 | 没有设置 `flex-wrap` | 增加 `flex-wrap: wrap` |
| 间距写得很乱 | 给每个子项手动写 margin | 优先使用 `gap` |

## 本章检查点

请确认你已经能够完成以下操作：

- 能写出一个横向排列的 Flex 容器。
- 能说明主轴和侧轴的区别。
- 能用 `justify-content` 控制主轴对齐。
- 能用 `align-items` 控制侧轴对齐。
- 能用 `gap` 和 `flex-wrap` 完成卡片列表。
