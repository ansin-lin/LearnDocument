# 第八章 Grid 网格布局

## 学习目标

完成本章后，你应能够：

- 判断什么时候使用 Grid，什么时候使用 Flex。
- 使用 `display: grid;` 创建网格容器。
- 使用 `grid-template-columns` 定义列。
- 使用 `grid-template-rows` 定义行。
- 使用 `fr`、`repeat()` 和 `gap` 写出常见卡片网格。
- 看懂简单的 `grid-column`、`grid-row` 和 `grid-template-areas`。

## 1. Grid 解决什么问题

Flex 主要解决“一条线上的排列”：

- 一行导航菜单。
- 一列侧边栏菜单。
- 一排按钮。

Grid 主要解决“行和列同时存在”的布局：

- 商品列表：多行多列。
- 图片墙：多行多列。
- 后台统计卡片：多行多列。
- 页面整体骨架：头部、侧边栏、主内容、底部。

简单判断：

| 布局需求 | 更适合 |
| --- | --- |
| 只控制一行或一列 | Flex |
| 同时控制行和列 | Grid |
| 导航栏、按钮组、左右对齐 | Flex |
| 卡片列表、图片墙、页面区域划分 | Grid |

## 2. 创建 Grid 容器

Grid 和 Flex 一样，先从父元素开始。给父元素设置 `display: grid;` 后，它的直接子元素会变成网格项目。

HTML：

```html
<div class="grid">
  <div>1</div>
  <div>2</div>
  <div>3</div>
  <div>4</div>
</div>
```

CSS：

```css
.grid {
  display: grid;
}

.grid div {
  background-color: pink;
}
```

此时只是创建了 Grid 容器，还没有明确指定几列。浏览器会按默认规则自动摆放子元素。实际开发中，通常会继续设置列、行和间距。

常见 Grid 属性先看这张表：

| 属性 | 写在哪 | 解决什么问题 |
| --- | --- | --- |
| `display: grid` | 父元素 | 创建 Grid 容器 |
| `grid-template-columns` | 父元素 | 定义有几列、每列多宽 |
| `grid-template-rows` | 父元素 | 定义有几行、每行多高 |
| `gap` | 父元素 | 定义行和列之间的间距 |
| `grid-column` | 子元素 | 指定某个子元素横向占几列 |
| `grid-row` | 子元素 | 指定某个子元素纵向占几行 |
| `grid-template-areas` | 父元素 | 用名字规划页面区域 |
| `grid-area` | 子元素 | 把子元素放到指定区域 |

## 3. 定义列：grid-template-columns

`grid-template-columns` 写在 Grid 父元素上，用来定义“有几列、每列多宽”。

```css
.grid {
  display: grid;
  grid-template-columns: 200px 200px 200px;
}
```

这行代码表示：

- 一共有 3 列。
- 每列宽度都是 200px。
- 如果有 6 个子元素，会自动排成 2 行 3 列。

可以把它理解成：

```text
第1列 200px | 第2列 200px | 第3列 200px
```

也可以让每列宽度不同：

```css
.grid {
  display: grid;
  grid-template-columns: 200px 1fr 100px;
}
```

含义：

- 第一列固定 200px。
- 第三列固定 100px。
- 中间列使用 `1fr`，占用剩余空间。

常见取值：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `200px 200px` | 两列，每列 200px | 固定宽度布局 |
| `200px 1fr` | 左侧固定，右侧自适应 | 侧边栏 + 内容区 |
| `1fr 1fr 1fr` | 三列平均分配 | 卡片列表 |
| `repeat(4, 1fr)` | 四列平均分配 | 统计卡片、商品列表 |

## 4. 定义行：grid-template-rows

`grid-template-rows` 写在 Grid 父元素上，用来定义“有几行、每行多高”。

```css
.grid {
  display: grid;
  grid-template-columns: 100px 100px;
  grid-template-rows: 80px 120px;
}
```

含义：

- `grid-template-columns: 100px 100px;` 定义 2 列。
- `grid-template-rows: 80px 120px;` 定义 2 行。
- 第一行高 80px。
- 第二行高 120px。

可以把它理解成：

```text
第1行 80px
第2行 120px
```

很多卡片列表只需要控制列，不一定要写 `grid-template-rows`。因为卡片高度通常由内容、`height` 或 `padding` 决定。

常见取值：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `80px 80px` | 两行，每行 80px | 固定高度网格 |
| `60px 1fr 50px` | 头部 60px，中间自适应，底部 50px | 页面骨架 |
| `auto auto` | 行高由内容决定 | 内容不固定的列表 |

## 5. fr 单位

`fr` 是 Grid 中很常用的单位，表示“剩余空间的一份”。

```css
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
}
```

含义：

- 容器宽度去掉间距后，剩余空间平均分成 3 份。
- 每一列拿 1 份。
- 最终得到 3 个等宽列。

再看一个比例不同的例子：

```css
.grid {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;
}
```

含义：

- 总份数是 `1 + 2 + 1 = 4`。
- 第一列占 1 份。
- 第二列占 2 份。
- 第三列占 1 份。
- 第二列大约是第一列的 2 倍。

`fr` 的优势是不用手动计算像素。容器变宽或变窄时，列宽会自动变化。

## 6. repeat() 简写

`repeat()` 用来减少重复书写。它常和 `fr` 一起使用。

```css
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}
```

这行代码等价于：

```css
.grid {
  grid-template-columns: 1fr 1fr 1fr 1fr;
}
```

`repeat(4, 1fr)` 的意思是：

- 重复 4 次。
- 每次都是 `1fr`。
- 最终得到 4 个等宽列。

常见写法：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `repeat(2, 1fr)` | 2 个等宽列 | 两列卡片 |
| `repeat(3, 1fr)` | 3 个等宽列 | 三列商品 |
| `repeat(4, 1fr)` | 4 个等宽列 | 后台统计卡片 |
| `repeat(3, 200px)` | 3 个 200px 固定列 | 固定宽度网格 |

初学阶段先掌握 `repeat(数量, 每列宽度)` 这个格式即可。

## 7. 网格间距：gap

`gap` 写在 Grid 父元素上，用来控制网格项目之间的距离。

```css
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
```

含义：

- 每一行之间相隔 20px。
- 每一列之间也相隔 20px。
- 不需要给每个子项单独写 `margin`。

如果行间距和列间距不同，可以分开写：

```css
.grid {
  row-gap: 20px;
  column-gap: 30px;
}
```

也可以用两个值写在 `gap` 中：

```css
.grid {
  gap: 20px 30px;
}
```

这表示：

- 第一个值 `20px` 是行间距。
- 第二个值 `30px` 是列间距。

常见取值：

| 写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `gap: 20px` | 行列间距都是 20px | 最常见 |
| `row-gap: 20px` | 只设置行间距 | 行距需要单独控制 |
| `column-gap: 30px` | 只设置列间距 | 列距需要单独控制 |
| `gap: 20px 30px` | 行间距 20px，列间距 30px | 行列间距不同 |

## 8. 指定子项跨列：grid-column

`grid-column` 写在 Grid 子元素上，用来指定某个子元素横向占几列。

先看一个 3 列网格：

```css
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.first {
  grid-column: 1 / 3;
}
```

`grid-column: 1 / 3;` 的意思是：

- 从第 1 条列线开始。
- 到第 3 条列线结束。
- 实际占用第 1 列和第 2 列。

三列网格会有 4 条列线：

```text
| 第1条线 | 第2条线 | 第3条线 | 第4条线 |
|  第1列  |  第2列  |  第3列  |
```

所以：

| 写法 | 含义 |
| --- | --- |
| `grid-column: 1 / 2` | 占第 1 列 |
| `grid-column: 1 / 3` | 占第 1 列和第 2 列 |
| `grid-column: 1 / 4` | 占 3 列整行 |
| `grid-column: span 2` | 从当前位置开始，横向占 2 列 |

初学阶段重点掌握 `1 / 3` 和 `span 2`。复杂线号后续看项目代码时再逐步熟悉。

## 9. 指定子项跨行：grid-row

`grid-row` 和 `grid-column` 类似，只是方向从“列”变成“行”。

```css
.first {
  grid-row: 1 / 3;
}
```

含义：

- 从第 1 条行线开始。
- 到第 3 条行线结束。
- 实际占用第 1 行和第 2 行。

常见写法：

| 写法 | 含义 |
| --- | --- |
| `grid-row: 1 / 2` | 占第 1 行 |
| `grid-row: 1 / 3` | 占第 1 行和第 2 行 |
| `grid-row: span 2` | 从当前位置开始，纵向占 2 行 |

实际项目中，卡片“横向跨列”更常见，纵向跨行相对少一些。先能看懂即可。

## 10. 区域布局：grid-template-areas

`grid-template-areas` 用名字描述页面区域，适合做页面整体骨架。

HTML：

```html
<div class="page">
  <header>头部</header>
  <aside>侧边栏</aside>
  <main>主内容</main>
  <footer>底部</footer>
</div>
```

CSS：

```css
.page {
  display: grid;
  grid-template-columns: 200px 1fr;
  grid-template-rows: 60px 1fr 50px;
  grid-template-areas:
    "header header"
    "aside main"
    "footer footer";
  min-height: 100vh;
}

header {
  grid-area: header;
}

aside {
  grid-area: aside;
}

main {
  grid-area: main;
}

footer {
  grid-area: footer;
}
```

分开理解：

```css
grid-template-columns: 200px 1fr;
```

表示页面有 2 列：

- 左列 200px。
- 右列占剩余空间。

```css
grid-template-rows: 60px 1fr 50px;
```

表示页面有 3 行：

- 第一行 60px，用来放头部。
- 第二行占剩余高度，用来放侧边栏和主内容。
- 第三行 50px，用来放底部。

```css
grid-template-areas:
  "header header"
  "aside main"
  "footer footer";
```

这三行字符串对应 3 行网格，每一行里有 2 个名字，对应 2 列：

| 网格行 | 第 1 列 | 第 2 列 |
| --- | --- | --- |
| 第 1 行 | `header` | `header` |
| 第 2 行 | `aside` | `main` |
| 第 3 行 | `footer` | `footer` |

所以最终效果是：

- `header` 横跨两列。
- `aside` 在中间左侧。
- `main` 在中间右侧。
- `footer` 横跨两列。

子元素通过 `grid-area` 进入对应区域：

```css
header {
  grid-area: header;
}
```

这里的 `header` 名字必须和 `grid-template-areas` 中的名字一致。

## 11. 综合练习：统计卡片网格

HTML：

```html
<div class="dashboard">
  <div class="card">用户数</div>
  <div class="card">订单数</div>
  <div class="card">销售额</div>
  <div class="card">访问量</div>
</div>
```

CSS：

```css
.dashboard {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  width: 1000px;
  margin: 50px auto;
}

.card {
  height: 120px;
  padding: 20px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
}
```

观察结果：

- `.dashboard` 是 Grid 父容器。
- `repeat(4, 1fr)` 表示 4 个等宽列。
- `gap: 20px` 表示卡片之间有 20px 间距。
- 4 个 `.card` 会排成一行。

练习要求：

1. 把 `repeat(4, 1fr)` 改为 `repeat(2, 1fr)`，观察是否变成两列。
2. 把 `gap` 改为 `40px`，观察卡片之间的距离是否变大。
3. 给第一个 `.card` 增加类名 `big`。
4. 添加 `.big { grid-column: 1 / 3; }`，观察第一个卡片是否跨两列。

## 12. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| Grid 属性没效果 | 没有给父元素设置 `display: grid` | 先确认属性写在父容器上 |
| 子元素没有按预期成列 | 没有定义 `grid-template-columns` | 明确设置列数和列宽 |
| `grid-template-columns` 看不懂 | 把每个值当成属性，而不是每一列的尺寸 | 记住一个值代表一列 |
| `fr` 看不懂 | 不知道它表示剩余空间份数 | 把 `1fr 2fr 1fr` 理解成 1:2:1 |
| `repeat()` 看不懂 | 不知道第一个参数是次数 | `repeat(4, 1fr)` 就是重复 4 个 `1fr` |
| 间距不统一 | 给每个子项单独写 `margin` | Grid 内部优先使用 `gap` |
| 跨列结果看不懂 | 混淆“列”和“列线” | 三列有四条列线，`1 / 3` 占两列 |
| 简单横向排列也用 Grid | 布局方式选择不当 | 一维布局优先 Flex |

## 本章检查点

请确认你已经能够完成以下操作：

- 能写出 `display: grid;`。
- 能说明 `grid-template-columns: repeat(3, 1fr);` 表示 3 个等宽列。
- 能说明 `grid-template-rows` 是控制行高。
- 能说明 `gap: 20px 30px;` 中第一个值是行间距，第二个值是列间距。
- 能看懂 `grid-column: 1 / 3;` 表示横向占两列。
- 能看懂 `grid-template-areas` 的区域名称如何对应到 `grid-area`。
