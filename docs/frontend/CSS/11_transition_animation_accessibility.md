# 第十一章 过渡、动画与可访问性

## 学习目标

完成本章后，学员应能够：

- 使用 `transition` 制作简单的悬停过渡。
- 使用 `transform` 完成移动、缩放、旋转等常见视觉效果。
- 使用 `@keyframes` 和 `animation` 制作基础动画。
- 了解动画对性能和可访问性的影响。
- 知道复杂 3D、私有前缀等内容不作为本课程主线。

## 1. 为什么需要过渡和动画

页面不是只能静态显示。适当的动态效果可以帮助用户理解交互状态。

例如：

- 按钮 hover 时颜色平滑变化。
- 卡片 hover 时轻微上移。
- 弹窗出现时淡入。
- 加载状态有简单提示。

但动画不是越多越好。教学主线只要求掌握常见、实用、可维护的效果。

## 2. transition 过渡

`transition` 用来让 CSS 属性变化时有一个过程，而不是瞬间变化。

没有过渡：

```css
.btn {
  background-color: #333;
}

.btn:hover {
  background-color: #ff6700;
}
```

有过渡：

```css
.btn {
  background-color: #333;
  transition: background-color 0.3s;
}

.btn:hover {
  background-color: #ff6700;
}
```

观察结果：

- 鼠标移入时，背景色平滑变成橙色。
- 鼠标移出时，背景色平滑恢复。

## 3. transition 的组成

完整写法包括：

```css
transition: 属性名 持续时间 速度曲线 延迟时间;
```

示例：

```css
.box {
  transition: width 0.3s ease 0s;
}
```

常见部分：

| 部分 | 对应属性 | 可接受的值 | 示例 | 含义 |
| --- | --- | --- | --- | --- |
| 属性名 | `transition-property` | CSS 属性名、`all`、`none` | `background-color` | 哪个属性变化要过渡 |
| 持续时间 | `transition-duration` | 时间，常用 `0.2s`、`0.3s`、`1s` | `0.3s` | 变化过程多久 |
| 速度曲线 | `transition-timing-function` | `ease`、`linear`、`ease-in`、`ease-out`、`ease-in-out` 等 | `ease` | 变化速度节奏 |
| 延迟时间 | `transition-delay` | 时间，常用 `0s`、`0.2s` | `0s` | 等多久再开始 |

速度曲线常用值：

| 值 | 含义 | 常见用途 |
| --- | --- | --- |
| `ease` | 默认值，先快后慢 | 大多数普通 hover 效果 |
| `linear` | 匀速 | 加载图标、持续移动 |
| `ease-in` | 开始慢，后面快 | 元素离开、收起 |
| `ease-out` | 开始快，后面慢 | 元素进入、展开 |
| `ease-in-out` | 两头慢，中间快 | 弹窗、卡片轻微移动 |
| `steps(4)` | 分 4 步跳变，不平滑 | 打字机、逐帧效果，初学能看懂即可 |
| `cubic-bezier(0.4, 0, 0.2, 1)` | 自定义速度曲线 | 设计系统中常见，初学先会识别 |

常见过渡属性名：

| 写法 | 含义 | 建议 |
| --- | --- | --- |
| `transition: background-color 0.3s;` | 只让背景色过渡 | 推荐，范围清楚 |
| `transition: color 0.3s;` | 只让文字颜色过渡 | 推荐 |
| `transition: transform 0.3s;` | 只让变换过渡 | 推荐 |
| `transition: opacity 0.3s;` | 只让透明度过渡 | 推荐 |
| `transition: all 0.3s;` | 所有可过渡属性都参与 | 能看懂即可，不建议作为默认习惯 |
| `transition: none;` | 不使用过渡 | 需要关闭动效时使用 |

如果要让多个属性过渡：

```css
.card {
  transition: transform 0.3s, box-shadow 0.3s;
}
```

初学阶段不建议直接写 `transition: all 0.3s;` 作为习惯。它虽然省事，但可能让不该动画的属性也参与过渡。

## 4. transform 变换

`transform` 可以改变元素的视觉位置、大小或角度。

常见值：

| 写法 | 可接受的值 | 效果 | 常见用途 |
| --- | --- | --- | --- |
| `translateX(10px)` | 长度或百分比 | 向右移动 10px | 左右滑动 |
| `translateY(-5px)` | 长度或百分比 | 向上移动 5px | 卡片 hover 上移 |
| `translate(10px, 20px)` | 两个长度或百分比 | 向右 10px，向下 20px | 同时控制横向和纵向移动 |
| `scale(1.05)` | 数字，`1` 表示原大小 | 放大到 1.05 倍 | hover 轻微放大 |
| `scaleX(1.2)` | 数字 | 横向放大 | 特殊视觉效果 |
| `scaleY(0.8)` | 数字 | 纵向压缩 | 特殊视觉效果 |
| `rotate(10deg)` | 角度，常用 `deg` | 顺时针旋转 10 度 | 图标、箭头旋转 |
| `skewX(10deg)` | 角度 | 横向倾斜 | 装饰效果，初学能看懂即可 |
| `none` | 固定值 | 不进行变换 | 清除已有 transform |

多个变换可以写在一起：

```css
.card:hover {
  transform: translateY(-5px) scale(1.03);
}
```

含义是：卡片上移，同时轻微放大。

卡片 hover 上移示例：

```css
.card {
  transition: transform 0.3s;
}

.card:hover {
  transform: translateY(-5px);
}
```

观察结果：

- 鼠标移到卡片上时，卡片轻微上移。
- 卡片原本在普通流中的占位不变。

## 5. transform-origin

`transform-origin` 设置变换中心点。

```css
.box {
  transform-origin: left top;
}
```

常见值：

| 值 | 含义 | 常见用途 |
| --- | --- | --- |
| `center` | 中心点，默认值 | 普通缩放、旋转 |
| `left top` | 左上角 | 从左上角开始缩放或旋转 |
| `right bottom` | 右下角 | 从右下角开始缩放或旋转 |
| `50% 50%` | 横向 50%、纵向 50%，等同中心点 | 用百分比精确控制 |
| `0 0` | 左上角，等同 `left top` | 代码中常见 |
| `100% 100%` | 右下角，等同 `right bottom` | 代码中常见 |

初学阶段只需要知道：旋转和缩放时，如果效果看起来不是围绕预期位置变化，可以检查 `transform-origin`。

## 6. animation 动画

`animation` 用来播放一组预先定义好的关键帧。

动画通常分两步：

1. 用 `@keyframes` 定义动画过程。
2. 用 `animation` 或 `animation-*` 属性把动画应用到元素上。

先定义关键帧：

```css
@keyframes move {
  from {
    transform: translateX(0);
  }

  to {
    transform: translateX(100px);
  }
}
```

再使用动画：

```css
.box {
  animation: move 1s linear infinite;
}
```

含义：

- 使用名为 `move` 的动画。
- 每次播放 1 秒。
- 匀速播放。
- 无限循环。

`animation-name` 可以写什么？

| 写法 | 含义 |
| --- | --- |
| 自己定义的关键帧名称 | 例如 `move`、`fadeIn`、`spin`、`shake`，必须和 `@keyframes` 后面的名称一致 |
| `none` | 不使用动画 |

命名建议：

- 用英文，表达动画效果。
- 常见名称可以写成 `fade-in`、`slide-up`、`rotate-loading`。
- 不要写成 `aaa`、`test1` 这类看不出含义的名字。

## 7. animation 常用属性

| 属性 | 可接受的值 | 默认值 | 作用 | 示例 |
| --- | --- | --- | --- | --- |
| `animation-name` | `@keyframes` 名称、`none` | `none` | 指定播放哪个动画 | `move` |
| `animation-duration` | 时间，例如 `0.3s`、`1s`、`2s` | `0s` | 动画一次播放多久 | `1s` |
| `animation-timing-function` | `ease`、`linear`、`ease-in`、`ease-out`、`ease-in-out`、`steps()`、`cubic-bezier()` | `ease` | 动画速度节奏 | `linear` |
| `animation-delay` | 时间，例如 `0s`、`0.3s`、`1s` | `0s` | 延迟多久开始播放 | `0.3s` |
| `animation-iteration-count` | 数字、`infinite` | `1` | 播放次数 | `infinite` |
| `animation-direction` | `normal`、`reverse`、`alternate`、`alternate-reverse` | `normal` | 播放方向 | `alternate` |
| `animation-fill-mode` | `none`、`forwards`、`backwards`、`both` | `none` | 动画开始前或结束后是否保留关键帧样式 | `forwards` |
| `animation-play-state` | `running`、`paused` | `running` | 动画播放或暂停 | `paused` |

常用属性值说明：

| 属性 | 常用值 | 含义 |
| --- | --- | --- |
| `animation-name` | `move`、`fade-in`、`spin`、`none` | 使用对应名称的 `@keyframes`，或不使用动画 |
| `animation-duration` | `0.3s`、`1s`、`2s` | 时间越大，动画越慢 |
| `animation-timing-function` | `linear` | 匀速，常用于旋转加载 |
| `animation-iteration-count` | `1` | 播放 1 次 |
| `animation-iteration-count` | `infinite` | 无限循环 |
| `animation-direction` | `normal` | 从起点到终点 |
| `animation-direction` | `reverse` | 从终点到起点 |
| `animation-direction` | `alternate` | 正向、反向交替播放 |
| `animation-fill-mode` | `forwards` | 动画结束后保留最后一帧 |
| `animation-play-state` | `paused` | 暂停动画 |

简写：

```css
.box {
  animation: move 1s linear infinite alternate;
}
```

这句可以拆成：

```css
.box {
  animation-name: move;
  animation-duration: 1s;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
  animation-direction: alternate;
}
```

初学阶段优先掌握：

- `animation-name`
- `animation-duration`
- `animation-timing-function`
- `animation-iteration-count`

其他属性先能看懂即可。

## 8. 常见按钮和卡片效果

按钮：

```css
.btn {
  display: inline-block;
  padding: 10px 20px;
  color: #fff;
  text-decoration: none;
  background-color: #333;
  transition: background-color 0.3s;
}

.btn:hover {
  background-color: #ff6700;
}
```

卡片：

```css
.card {
  padding: 20px;
  border: 1px solid #ddd;
  transition: transform 0.3s, box-shadow 0.3s;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}
```

这些效果足够覆盖多数入门页面练习。

## 9. 动画的可访问性

部分用户对明显运动效果敏感。页面应尊重用户系统中的“减少动态效果”设置。

常见写法：

```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation: none;
    transition: none;
  }
}
```

这段代码的作用：

- 当用户系统设置减少动态效果时。
- 页面取消动画和过渡。

实际项目中可以更精细地控制，而不是所有动画都关闭。初学阶段先理解这个意识即可。

## 10. 性能和使用边界

动画要注意性能和可维护性。

建议优先动画这些属性：

| 属性 | 可接受的值 | 适合原因 |
| --- | --- | --- |
| `transform` | `translate()`、`scale()`、`rotate()` 等 | 常用于移动、缩放、旋转，性能较好 |
| `opacity` | `0` 到 `1` | 常用于淡入淡出，性能较好 |

谨慎频繁动画这些属性：

| 属性 | 示例 | 问题 |
| --- | --- | --- |
| `width` | `width: 300px` | 可能引起重新布局 |
| `height` | `height: 200px` | 可能引起重新布局 |
| `margin` | `margin-left: 20px` | 可能影响周围元素位置 |
| `top`、`left` | `left: 100px` | 依赖定位，可能引起布局计算 |

原因是它们可能引起页面重新布局，复杂页面中性能更差。

低频内容处理方式：

- 复杂 3D 变换不放入主线。
- 私有浏览器前缀不作为本课程重点。
- 大量动画编排适合放入专题或项目扩展。

## 11. 综合练习：按钮和卡片动效

HTML：

```html
<a class="btn" href="#">查看课程</a>

<div class="card">
  <h3>CSS 基础</h3>
  <p>学习选择器、盒子模型、布局和响应式。</p>
</div>
```

CSS：

```css
.btn {
  display: inline-block;
  padding: 10px 20px;
  color: #fff;
  text-decoration: none;
  background-color: #333;
  transition: background-color 0.3s;
}

.btn:hover {
  background-color: #ff6700;
}

.card {
  width: 300px;
  margin-top: 30px;
  padding: 20px;
  border: 1px solid #ddd;
  transition: transform 0.3s, box-shadow 0.3s;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}
```

练习要求：

1. 修改按钮过渡时间为 `1s`，观察变化是否变慢。
2. 删除 `.card` 中的 `transition`，观察 hover 是否变成瞬间变化。
3. 把 `translateY(-5px)` 改为 `scale(1.05)`，观察卡片变换效果。
4. 增加减少动态效果的媒体查询。

## 12. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| 过渡没有效果 | 没有发生属性变化 | 检查 hover 前后属性是否不同 |
| 鼠标移出没有过渡 | `transition` 写在了 `:hover` 上 | 通常写在元素默认状态上 |
| 动画不播放 | `@keyframes` 名称和 `animation-name` 不一致 | 检查名称 |
| 动画名称看不懂 | `animation-name` 写成无意义名称 | 使用 `fade-in`、`slide-up`、`spin` 等表达效果的名称 |
| 动画速度不符合预期 | 速度曲线选择不合适 | hover 常用 `ease`，加载旋转常用 `linear` |
| 动效太夸张 | 移动距离、缩放比例过大 | 控制在轻微变化 |
| 页面运动影响用户 | 没有考虑减少动态效果 | 使用 `prefers-reduced-motion` |

## 本章检查点

请确认你已经能够完成以下操作：

- 能使用 `transition` 写出 hover 过渡。
- 能使用 `transform` 完成上移、缩放、旋转中的至少一种效果。
- 能写出基础 `@keyframes` 动画。
- 能说明为什么动画应优先使用 `transform` 和 `opacity`。
- 能添加减少动态效果的媒体查询。
