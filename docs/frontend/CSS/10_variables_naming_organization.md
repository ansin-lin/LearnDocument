# 第十章 CSS 变量、命名与样式组织

## 学习目标

完成本章后，学员应能够：

- 理解为什么页面变大后需要统一组织 CSS。
- 使用 CSS 变量管理颜色、间距等重复值。
- 编写语义清晰的类名。
- 按稳定顺序组织属性，减少维护成本。
- 说明进入 CSS 框架和 UI 框架前需要掌握的基础 CSS 能力。
- 避免过深选择器、随意使用 `!important` 等常见问题。

## 1. 为什么要整理 CSS

小练习中，CSS 只有几十行，看起来怎么写都能运行。

但页面变大后，如果没有组织方式，会出现这些问题：

- 同一个颜色在很多地方重复写，后期改主题很麻烦。
- 类名叫 `.box1`、`.box2`，看不出用途。
- 选择器写得很长，改一个结构就全部失效。
- 到处使用 `!important`，后面很难覆盖。
- 新人接手时不知道样式应该写在哪里。

CSS 组织不是为了形式，而是为了让页面后续能维护。

这里说的“硬编码”，是指把具体值直接写死在很多地方。例如很多按钮、标题、边框里都直接写 `#ff6700`。这样页面能显示，但后期如果主题色要改，就必须到处查找和替换。

## 2. CSS 变量

CSS 变量可以保存重复使用的值。它的作用和其他编程语言中的变量类似：先给一个名字保存值，后面需要这个值时通过名字引用。

先看完整写法：

```css
:root {
  --main-color: #ff6700;
}

.button {
  background-color: var(--main-color);
}
```

这段代码分成三部分理解：

| 写法 | 含义 |
| --- | --- |
| `:root` | 表示整份 HTML 文档的根元素，通常可以理解为“全页面范围” |
| `--main-color` | 定义一个 CSS 变量，变量名必须以两个短横线 `--` 开头 |
| `var(--main-color)` | 读取并使用 `--main-color` 这个变量的值 |

### 2.1 `:root` 是什么

`:root` 是一个伪类选择器，表示页面的根元素。在 HTML 页面中，根元素就是 `<html>`。

下面两种写法很接近：

```css
html {
  font-size: 16px;
}

:root {
  --main-color: #ff6700;
}
```

区别是：

- `html` 选择的是 `<html>` 标签。
- `:root` 表示文档根元素，写 CSS 变量时更常见。

变量写在 `:root` 中，页面中大多数元素都能使用这些变量，所以全站通用颜色、间距、圆角、版心宽度通常会放在这里。

### 2.2 `--变量名` 为什么这样写

CSS 变量的正式名称是“自定义属性”。自定义属性必须以两个短横线 `--` 开头。

```css
:root {
  --main-color: #ff6700;
  --page-width: 1200px;
}
```

原因很直接：浏览器需要通过 `--` 区分“普通 CSS 属性”和“开发者自己定义的属性”。

对比一下：

| 写法 | 类型 |
| --- | --- |
| `color` | CSS 内置属性 |
| `background-color` | CSS 内置属性 |
| `--main-color` | 开发者自定义属性，也就是 CSS 变量 |
| `--page-width` | 开发者自定义属性，也就是 CSS 变量 |

命名建议：

- 用英文小写。
- 多个单词用短横线连接。
- 名字表达用途，不只表达颜色本身。

例如 `--main-color` 比 `--orange` 更容易维护，因为以后主色可能不再是橙色。

### 2.3 `var()` 是干什么的

定义变量后，不能直接把变量名写到属性值里，必须使用 `var()` 读取变量。

```css
.button {
  background-color: var(--main-color);
}
```

这句的意思是：

- 找到名为 `--main-color` 的变量。
- 取出它保存的值。
- 把这个值用作 `.button` 的背景色。

如果前面定义了：

```css
:root {
  --main-color: #ff6700;
}
```

那么：

```css
background-color: var(--main-color);
```

实际效果就相当于：

```css
background-color: #ff6700;
```

### 2.4 常见变量写法

```css
:root {
  --main-color: #ff6700;
  --text-color: #333;
  --muted-color: #666;
  --border-color: #ddd;
  --page-width: 1200px;
}
```

使用这些变量：

```css
.button {
  color: #fff;
  background-color: var(--main-color);
}
```

好处：

- 颜色统一。
- 修改主题时只改一处。
- 样式含义更清楚。

## 3. var() 和默认值

`var()` 除了读取变量，还可以提供默认值：

```css
.box {
  color: var(--text-color, #333);
}
```

含义：

- 如果 `--text-color` 存在，就使用变量值。
- 如果不存在，就使用 `#333`。

初学阶段常用在颜色、间距、宽度上即可，不需要把所有值都变量化。

## 4. 变量作用范围

变量写在 `:root` 中，通常全页面都可以使用。

```css
:root {
  --main-color: #ff6700;
}
```

也可以只在某个区域内生效：

```css
.dark-panel {
  --panel-bg: #222;
  --panel-text: #fff;
}

.dark-panel {
  color: var(--panel-text);
  background-color: var(--panel-bg);
}
```

初学阶段建议：

- 全站通用颜色写在 `:root`。
- 局部组件专用变量写在组件类下面。
- 不要为了炫技写过多层变量。

## 5. 类名命名

类名应该表达元素作用，而不是只表达颜色或位置。

不推荐：

```html
<div class="red-box"></div>
<div class="left"></div>
<div class="box1"></div>
```

更推荐：

```html
<div class="course-card"></div>
<div class="sidebar"></div>
<div class="product-list"></div>
```

常见页面区域命名：

| 类名 | 含义 |
| --- | --- |
| `.container` | 版心或内容容器 |
| `.header` | 页面头部 |
| `.nav` | 导航 |
| `.sidebar` | 侧边栏 |
| `.main` | 主内容 |
| `.footer` | 页脚 |
| `.card` | 卡片 |
| `.list` | 列表 |
| `.item` | 列表项 |

备份示例中常见的 `shortcut`、`header`、`nav`、`footer` 等命名都属于按页面区域命名。

## 6. 选择器不要写得过深

不推荐：

```css
.page .main .content .list .item a span {
  color: red;
}
```

问题：

- 结构稍微变化，样式就失效。
- 优先级过高，后面覆盖困难。
- 其他人很难判断它影响哪些元素。

更推荐：

```css
.course-title {
  color: var(--main-color);
}
```

经验规则：

- 能用一个类名解决，就不要写很长的后代选择器。
- 页面区域可以适当限定，例如 `.nav a`。
- 不要为了“保险”把选择器写得越来越长。

## 7. !important 的使用边界

`!important` 会强行提高声明优先级。

```css
.title {
  color: red !important;
}
```

初学阶段不建议依赖它解决问题。

常见误用：

- 不知道样式为什么没生效，就加 `!important`。
- 多处互相加 `!important`，后续无法维护。

更合理的处理方式：

- 检查选择器是否写错。
- 检查 CSS 引入顺序。
- 检查优先级是否被更高选择器覆盖。
- 必须覆盖第三方组件样式时，再谨慎使用。

## 8. 属性书写顺序

同一个选择器内，属性建议按类型分组。

推荐顺序：

1. 布局定位：`display`、`position`、`top`、`left`
2. 盒子尺寸：`width`、`height`、`margin`、`padding`
3. 边框背景：`border`、`background`
4. 文本样式：`font-size`、`line-height`、`color`
5. 交互和其他效果：`cursor`、`resize`、`outline`、`transition`

示例：

```css
.card {
  display: block;
  width: 300px;
  margin: 20px auto;
  padding: 20px;
  border: 1px solid var(--border-color);
  background-color: #fff;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-color);
}
```

这种顺序不是浏览器要求，而是团队维护习惯。保持一致比某一种顺序本身更重要。

在表单和后台页面中，也会看到一些交互相关属性：

```css
.button {
  cursor: pointer;
}

textarea {
  resize: vertical;
}

input:focus {
  outline: 3px solid #f5b301;
  outline-offset: 2px;
}
```

这些属性不要散落在文件各处。可以放在“表单”“按钮”“焦点状态”等对应区域，便于后续查找和维护。

## 9. CSS 文件组织

小练习可以只写一个 CSS 文件。

页面变多后，可以按用途拆分：

```text
css/
├── base.css      通用基础样式
├── common.css    头部、底部、版心等公共样式
├── index.css     首页样式
└── detail.css    详情页样式
```

常见做法：

- `base.css` 放 reset、变量、通用标签样式。
- `common.css` 放多个页面共用的头部、导航、页脚。
- 页面专用 CSS 只写当前页面独有样式。

这里的 `reset` 指“重置样式”。浏览器会给 `body`、`h1`、`p`、`ul` 等标签提供默认样式，例如默认外边距、字号、列表缩进。不同浏览器的默认值可能略有差异。项目中常会先写一组基础样式，把默认间距、盒模型等统一起来，这类代码就常放在 `base.css` 中。

不要把所有页面样式长期堆在一个文件里，也不要每个小组件都过度拆文件。

## 10. CSS 框架和 UI 框架的前置知识

后续学习 Bootstrap、Vuetify、Element Plus、Ant Design 等 CSS 框架或 UI 框架时，不是只记类名或组件名。框架底层仍然依赖 CSS 基础。

进入框架前，必须能看懂这些内容：

| 前置知识 | 为什么重要 |
| --- | --- |
| 多 class 叠加 | 框架经常用 `btn btn-primary active` 这类写法组合基础、外观和状态 |
| 选择器优先级 | 覆盖框架样式时要判断为什么自己的样式没有生效 |
| 属性选择器和状态伪类 | 表单校验、禁用、选中、当前页面等状态常靠属性或伪类表达 |
| 盒模型和 `box-sizing` | 组件宽高、内边距、边框计算都依赖盒模型 |
| Flex 和 Grid | 框架布局类、栅格和组件排列都依赖布局基础 |
| 响应式断点 | 框架的手机、平板、电脑适配本质是媒体查询和断点 |
| CSS 变量 | 主题色、间距、圆角等经常通过变量或 token 管理 |
| 焦点和可访问性 | 组件必须能键盘操作，不能只靠鼠标 hover |

这里的 `token` 可以先理解为“设计值的名字”。例如主色、错误色、圆角大小、常用间距，在设计系统或 UI 框架中可能会被命名为 token。CSS 变量就是前端实现这些 token 的常见方式之一。

例如框架按钮可能长这样：

```html
<button class="btn btn-primary active" disabled>保存</button>
```

看这段 HTML 时，不要只把它当作一串类名。可以拆成：

- `btn`：按钮基础样式。
- `btn-primary`：主要按钮颜色。
- `active`：当前激活状态。
- `disabled`：HTML 禁用属性，也会影响 CSS 状态。

这种写法叫多 class 叠加：一个元素同时拥有多个 class，每个 class 负责一部分样式。前面第二章已经讲过 `.button.primary` 这类组合选择器；看框架代码时也要沿用这个思路拆解。

如果你要覆盖它的样式，应先检查：

1. 当前元素有哪些 class 和属性。
2. 框架原来的选择器优先级是多少。
3. 自己的 CSS 文件是否在框架 CSS 后面加载。
4. 是否可以通过主题变量解决，而不是直接写很长选择器。
5. 是否保留了焦点、禁用、错误等状态样式。

不建议一遇到框架样式覆盖失败就使用 `!important`。这会让后续维护越来越困难。

## 11. 综合练习：整理侧边栏菜单样式

HTML：

```html
<div class="sidebar-menu">
  <a href="#">手机 电话卡</a>
  <a href="#">电视 盒子</a>
  <a href="#">笔记本 平板</a>
</div>
```

CSS：

```css
:root {
  --menu-bg: #55585a;
  --menu-hover-bg: #ff6700;
  --menu-text: #fff;
}

.sidebar-menu {
  width: 230px;
  margin: 50px auto;
}

.sidebar-menu a {
  display: block;
  height: 42px;
  padding-left: 30px;
  line-height: 42px;
  color: var(--menu-text);
  text-decoration: none;
  background-color: var(--menu-bg);
}

.sidebar-menu a:hover {
  background-color: var(--menu-hover-bg);
}
```

练习要求：

1. 修改 `--menu-hover-bg`，观察所有 hover 颜色是否统一变化。
2. 把 `.sidebar-menu` 改名为 `.menu`，同步修改 HTML 和 CSS。
3. 按属性顺序重新整理 `.sidebar-menu a`。
4. 不使用 `!important` 完成样式调整。

## 12. 常见错误

| 问题 | 常见原因 | 处理方式 |
| --- | --- | --- |
| 类名看不懂 | 使用 `.box1`、`.red` | 改为表达业务或结构含义 |
| 改颜色要找很多地方 | 重复硬编码颜色 | 使用 CSS 变量 |
| 样式难覆盖 | 选择器过深 | 使用清晰类名降低耦合 |
| 到处写 `!important` | 没有分析优先级 | 先检查选择器和加载顺序 |
| CSS 文件越来越乱 | 没有按用途拆分 | 区分基础、公共、页面样式 |
| 看不懂框架类名 | 没有拆分基础类、外观类和状态类 | 按职责分析多个 class |
| 看不懂 `:root`、`--`、`var()` | 没有区分变量定义位置、变量名和变量读取方式 | `:root` 放全局变量，`--name` 定义变量，`var(--name)` 使用变量 |

## 本章检查点

请确认你已经能够完成以下操作：

- 能定义并使用 CSS 变量。
- 能说明 `:root`、`--变量名` 和 `var()` 分别负责什么。
- 能写出语义清晰的类名。
- 能识别过深选择器的问题。
- 能按较稳定的顺序整理属性。
- 能说明 CSS 框架和 UI 框架依赖哪些基础 CSS 能力。
- 能说明为什么不要随意使用 `!important`。
