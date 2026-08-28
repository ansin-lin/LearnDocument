# 第六章 定位、层级与显示隐藏

## 学习目标

完成本章后，学员应能够：

- 理解普通流、定位元素之间的区别。
- 使用 `relative`、`absolute`、`fixed`、`sticky` 完成常见定位效果。
- 正确使用“子绝父相”实现局部定位。
- 使用 `z-index` 控制重叠元素的前后层级。
- 区分 `display: none`、`visibility: hidden`、`opacity: 0` 的效果。

## 1. 为什么需要定位

普通流适合让元素自然排列，但有些效果需要把元素放到指定位置：

- 商品卡片右上角的“新品”标签。
- 图片右下角的播放按钮。
- 页面右下角的“返回顶部”按钮。
- 顶部固定导航栏。
- 表格或页面中的粘性标题。

这些效果不能只靠普通流完成，需要使用 CSS 定位。

## 2. position 属性

`position` 决定元素使用哪种定位方式。

| 值 | 含义 | 是否脱离普通流 | 常见用途 |
| --- | --- | --- | --- |
| `static` | 默认值，不定位 | 否 | 普通元素 |
| `relative` | 相对定位 | 否 | 配合绝对定位的父元素 |
| `absolute` | 绝对定位 | 是 | 角标、遮罩、局部按钮 |
| `fixed` | 固定定位 | 是 | 固定导航、返回顶部 |
| `sticky` | 粘性定位 | 视滚动状态而定 | 粘性标题、表头 |

定位通常配合下面四个方向属性使用：

```css
top: 10px;
right: 20px;
bottom: 10px;
left: 20px;
```

这些属性只有在元素设置了非 `static` 定位后才会生效。

## 3. 相对定位 relative

相对定位是相对于元素原来的位置移动。

```css
.box {
  position: relative;
  top: 20px;
  left: 30px;
  width: 100px;
  height: 100px;
  background-color: pink;
}
```

观察结果：

- 盒子从原来的位置向下移动 20px。
- 盒子从原来的位置向右移动 30px。
- 原来的位置仍然保留，其他元素不会自动补上来。

相对定位最常见的用途不是单独移动元素，而是作为绝对定位子元素的参照物。

## 4. 绝对定位 absolute

绝对定位会让元素脱离普通流。

```css
.box {
  position: absolute;
  top: 20px;
  right: 20px;
}
```

绝对定位元素会寻找最近的“已定位祖先元素”作为参照。

已定位祖先元素是指 `position` 不是 `static` 的祖先，例如：

- `position: relative`
- `position: absolute`
- `position: fixed`
- `position: sticky`

如果找不到已定位祖先元素，它通常会以浏览器视口或页面初始包含块作为参照。

## 5. 子绝父相

“子绝父相”是实际项目中非常常用的定位组合：

- 父元素设置 `position: relative;`
- 子元素设置 `position: absolute;`

示例：

```html
<div class="card">
  <img src="images/product.jpg" alt="商品">
  <span class="tag">新品</span>
</div>
```

```css
.card {
  position: relative;
  width: 240px;
  height: 160px;
  background-color: #f5f5f5;
}

.card img {
  width: 100%;
  height: 100%;
}

.tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 8px;
  color: #fff;
  background-color: #ff6700;
}
```

观察结果：

- `.tag` 会出现在 `.card` 的右上角。
- 父元素 `.card` 仍然留在普通流中。
- 子元素 `.tag` 不会占用普通流位置。

如果删除 `.card` 的 `position: relative;`，标签可能会跑到页面右上角或其他非预期位置。

## 6. 固定定位 fixed

固定定位相对于浏览器窗口定位。页面滚动时，它仍然停留在指定位置。

```css
.back-top {
  position: fixed;
  right: 30px;
  bottom: 30px;
  width: 50px;
  height: 50px;
  line-height: 50px;
  text-align: center;
  color: #fff;
  background-color: #333;
}
```

常见用途：

- 返回顶部按钮。
- 固定客服按钮。
- 固定顶部导航。

注意：

- 固定元素可能遮挡页面内容。
- 顶部固定导航常需要给正文增加上边距或内边距。

## 7. 粘性定位 sticky

粘性定位可以理解为：元素在指定滚动位置前正常显示，滚动到阈值后固定在某个位置。

```css
.title {
  position: sticky;
  top: 0;
  background-color: #fff;
}
```

常见用途：

- 列表分组标题。
- 表格表头。
- 页面内导航。

使用时要注意：

- 必须设置 `top`、`bottom`、`left` 或 `right` 中至少一个。
- 父容器的滚动和高度会影响 sticky 的效果。
- 如果效果不生效，要检查父元素是否设置了特殊的 `overflow`。

## 8. z-index 层级

当多个元素重叠时，`z-index` 可以控制谁在上面。

```css
.one {
  position: absolute;
  z-index: 1;
}

.two {
  position: absolute;
  z-index: 2;
}
```

通常 `z-index` 值越大，显示越靠上。

注意：

- `z-index` 常用于定位元素。
- 不要随意写很大的值，例如 `999999`。
- 项目中最好约定层级范围，例如普通浮层、弹窗、确认框分别使用不同范围。

简单示例：

```css
.modal {
  position: fixed;
  z-index: 1000;
}

.tooltip {
  position: absolute;
  z-index: 100;
}
```

这样弹窗通常会显示在提示气泡之上。

## 9. 显示与隐藏

常见隐藏方式有三种：

| 写法 | 是否显示 | 是否占位置 | 是否能点击 |
| --- | --- | --- | --- |
| `display: none;` | 不显示 | 不占位置 | 不能 |
| `visibility: hidden;` | 不显示 | 占位置 | 通常不能 |
| `opacity: 0;` | 完全透明 | 占位置 | 仍可能能点击 |

示例：

```css
.box {
  display: none;
}
```

适合完全移除元素显示。

```css
.box {
  visibility: hidden;
}
```

适合隐藏但保留原来空间。

```css
.box {
  opacity: 0;
}
```

适合配合过渡动画做淡入淡出，但要注意它仍可能占位并响应鼠标事件。

## 10. 综合练习：商品卡片角标和返回顶部

HTML：

```html
<div class="card">
  <h3>CSS 基础课程</h3>
  <p>适合零基础学员学习页面样式。</p>
  <span class="tag">推荐</span>
</div>

<a class="back-top" href="#">Top</a>
```

CSS：

```css
.card {
  position: relative;
  width: 300px;
  margin: 80px auto;
  padding: 20px;
  border: 1px solid #ddd;
}

.tag {
  position: absolute;
  top: 0;
  right: 0;
  padding: 4px 8px;
  color: #fff;
  background-color: #ff6700;
}

.back-top {
  position: fixed;
  right: 30px;
  bottom: 30px;
  width: 50px;
  height: 50px;
  line-height: 50px;
  text-align: center;
  color: #fff;
  text-decoration: none;
  background-color: #333;
}
```

练习要求：

1. 删除 `.card` 的 `position: relative;`，观察角标位置变化。
2. 修改 `.tag` 的 `top` 和 `right`，观察角标移动。
3. 增加很多正文内容，让页面可以滚动，观察 `.back-top` 是否固定在窗口右下角。
4. 给 `.tag` 和 `.back-top` 分别设置不同 `z-index`，观察层级变化。

## 11. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| `top`、`left` 没效果 | 元素还是 `position: static` | 先设置定位方式 |
| 绝对定位跑到页面角落 | 父元素没有定位 | 使用“子绝父相” |
| 固定导航遮住内容 | fixed 脱离普通流 | 给正文预留空间 |
| `z-index` 没效果 | 元素未形成可控层级 | 检查定位、层叠上下文 |
| 元素透明后还能点 | 使用了 `opacity: 0` | 需要时同时处理点击事件或改用 `display: none` |

## 本章检查点

请确认你已经能够完成以下操作：

- 能说明相对定位和绝对定位的区别。
- 能使用“子绝父相”完成角标定位。
- 能使用 fixed 写出返回顶部按钮。
- 能用 `z-index` 调整重叠元素层级。
- 能区分三种隐藏方式的占位差异。
