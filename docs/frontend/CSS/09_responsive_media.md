# 第九章 响应式布局与媒体查询

## 学习目标

完成本章后，学员应能够：

- 理解响应式布局要解决的问题。
- 正确设置移动端视口。
- 使用百分比、`max-width`、`fr` 等方式让布局具备弹性。
- 使用媒体查询在不同屏幕宽度下调整样式。
- 处理图片、卡片列表、表格在小屏幕下的常见问题。

## 1. 什么是响应式布局

响应式布局是指同一套页面能根据屏幕宽度自动调整显示效果。

例如：

- 在电脑屏幕上，卡片一行显示 4 个。
- 在平板上，卡片一行显示 2 个。
- 在手机上，卡片一行显示 1 个。

响应式不是简单把页面缩小，而是根据屏幕空间重新组织内容。

## 2. 移动端视口

移动端页面通常需要在 HTML 的 `head` 中写：

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

这句的作用：

- `width=device-width`：让页面宽度等于设备屏幕宽度。
- `initial-scale=1.0`：初始缩放比例为 1。

如果缺少这句，手机浏览器可能会按桌面页面宽度渲染，再整体缩小，导致文字很小、布局难以控制。

## 3. 固定宽度与弹性宽度

固定宽度：

```css
.container {
  width: 1200px;
}
```

问题：在小屏幕上可能超出屏幕，出现横向滚动。

更常见的写法：

```css
.container {
  width: 90%;
  max-width: 1200px;
  margin: 0 auto;
}
```

含义：

- 宽屏时最大不超过 1200px。
- 窄屏时使用屏幕宽度的 90%。
- 左右自动居中。

## 4. 媒体查询基本语法

媒体查询用于在满足条件时应用一组 CSS。

它的基本结构是：

```css
@media 媒体类型 and (条件) {
  选择器 {
    属性: 值;
  }
}
```

可以先把它理解成一句话：当屏幕满足某个条件时，才执行大括号里面的 CSS。

示例：

```css
@media screen and (max-width: 768px) {
  .nav {
    display: none;
  }
}
```

含义：

- 当屏幕宽度小于等于 768px 时。
- `.nav` 隐藏。

常见条件：

| 写法 | 含义 |
| --- | --- |
| `(max-width: 768px)` | 宽度小于等于 768px |
| `(min-width: 768px)` | 宽度大于等于 768px |
| `screen` | 屏幕设备 |
| `and` | 同时满足条件 |

注意：`@media` 不是选择器，它是 CSS 的条件规则。普通选择器仍然写在 `@media` 的大括号里面。

## 5. 版心 container

在传统 PC 页面中，经常会根据屏幕宽度设置不同版心。

示例：

```css
.container {
  width: 750px;
  margin: 0 auto;
}

@media screen and (min-width: 992px) {
  .container {
    width: 970px;
  }
}

@media screen and (min-width: 1200px) {
  .container {
    width: 1170px;
  }
}
```

这段代码中：

- 默认情况下，`.container` 宽度是 750px。
- 当屏幕宽度大于等于 992px 时，`.container` 宽度改为 970px。
- 当屏幕宽度大于等于 1200px 时，`.container` 宽度改为 1170px。

观察结果：

- 屏幕较窄时，版心较窄。
- 屏幕较宽时，版心变宽。
- 内容始终在中间区域显示。

这种写法在很多旧版响应式框架中很常见。新项目也可以根据实际设计稿决定断点和版心宽度。

## 6. max-width 和 min-width

`max-width` 常用于从大屏向小屏调整：

```css
@media screen and (max-width: 768px) {
  .card {
    width: 100%;
  }
}
```

`min-width` 常用于移动优先：

```css
.card {
  width: 100%;
}

@media screen and (min-width: 768px) {
  .card {
    width: 50%;
  }
}
```

初学阶段建议先掌握一种写法。维护现有页面时，先观察项目原本是使用 `max-width` 还是 `min-width`，不要混乱叠加。

## 7. 响应式卡片列表

HTML：

```html
<div class="course-list">
  <div class="course-card">HTML</div>
  <div class="course-card">CSS</div>
  <div class="course-card">JavaScript</div>
  <div class="course-card">项目练习</div>
</div>
```

CSS：

```css
.course-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media screen and (max-width: 992px) {
  .course-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 576px) {
  .course-list {
    grid-template-columns: 1fr;
  }
}
```

观察结果：

- 大屏一行 4 个。
- 中等屏幕一行 2 个。
- 小屏一行 1 个。

这种写法比手动给每个卡片写复杂宽度更清晰。

## 8. 响应式图片

图片如果固定宽度，可能在手机上超出屏幕。

常见写法：

```css
img {
  max-width: 100%;
  height: auto;
}
```

含义：

- 图片最大不超过父元素宽度。
- 高度自动按比例缩放。

不要随意同时固定图片宽度和高度，否则图片可能被压扁或拉伸。

## 9. vw 和 vh

`vw` 和 `vh` 是和视口相关的单位。

| 单位 | 含义 |
| --- | --- |
| `1vw` | 视口宽度的 1% |
| `1vh` | 视口高度的 1% |

示例：

```css
.hero {
  height: 100vh;
}
```

表示 `.hero` 高度等于一屏高度。

注意：

- `vw`、`vh` 适合大块视觉区域。
- 普通文字字号不建议完全依赖 `vw`，否则在极小或极大屏幕上可能难读。

## 10. 小屏表格处理

数据表格在手机上很容易超出屏幕。

常见处理方式是让表格外层可以横向滚动：

```html
<div class="table-wrap">
  <table>
    <tr>
      <th>姓名</th>
      <th>课程</th>
      <th>成绩</th>
      <th>备注</th>
    </tr>
  </table>
</div>
```

```css
.table-wrap {
  overflow-x: auto;
}

table {
  min-width: 600px;
  border-collapse: collapse;
}
```

这样小屏幕不会把整个页面撑出横向滚动，而是只让表格区域滚动。

## 11. 键盘焦点样式

响应式页面不能只考虑鼠标操作。很多用户会使用键盘在链接、按钮和表单控件之间移动。

当元素获得键盘焦点时，浏览器通常会显示默认轮廓。项目中可以统一设置清晰的焦点样式：

```css
a:focus,
button:focus,
input:focus,
select:focus,
textarea:focus {
  outline: 3px solid #f5b301;
  outline-offset: 2px;
}
```

`outline` 表示轮廓线，`outline-offset` 表示轮廓线和元素边缘之间的距离。

这段代码的效果：

- 用键盘 Tab 移动时，当前操作位置更明显。
- 焦点样式不会挤占盒子大小。
- 用户不需要依赖鼠标就能判断当前控件。

注意：不要随意写 `outline: none;`。如果去掉浏览器默认焦点样式，必须提供同样清楚的新焦点样式。

## 12. 综合练习：响应式课程页面

练习要求：

1. 页面外层使用 `.container`，设置 `width: 90%; max-width: 1200px; margin: 0 auto;`。
2. 课程列表大屏一行 4 个。
3. 屏幕宽度小于等于 992px 时一行 2 个。
4. 屏幕宽度小于等于 576px 时一行 1 个。
5. 图片设置 `max-width: 100%; height: auto;`。
6. 为链接、按钮和表单控件设置清晰的焦点样式。

检查时请分别把浏览器宽度调整到：

- 1200px 左右。
- 768px 左右。
- 390px 左右。

观察卡片列数是否符合预期。

## 13. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| 手机页面显示很小 | 缺少 viewport meta | 添加移动端视口设置 |
| 小屏出现横向滚动 | 固定宽度过大 | 使用百分比、`max-width` 或媒体查询 |
| 媒体查询不生效 | 条件写反或顺序覆盖 | 检查 `max-width` / `min-width` 和代码顺序 |
| 图片超出容器 | 图片固定宽度 | 设置 `max-width: 100%; height: auto;` |
| 表格撑破页面 | 表格列太多 | 外层增加横向滚动容器 |
| 键盘操作看不出当前位置 | 去掉或没有设置焦点样式 | 使用 `outline` 和 `outline-offset` |

## 本章检查点

请确认你已经能够完成以下操作：

- 能写出 viewport meta。
- 能使用媒体查询修改布局。
- 能让卡片列表在不同屏幕下改变列数。
- 能处理响应式图片。
- 能说明固定宽度在小屏下的问题。
- 能为链接、按钮和表单控件设置可见焦点样式。
