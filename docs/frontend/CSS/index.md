# CSS 零基础教程

CSS 用来控制网页的外观和布局。HTML 负责页面中“有什么内容”，CSS 负责这些内容“如何显示”，例如颜色、字号、间距、边框、背景和排列方式。

本课程的顺序参照 `backup/HTML CSS` 中的 CSS 主线重新整理：先讲 CSS 基础语法、选择器、字体文本、引入方式，再讲层叠、盒模型、显示模式、定位、Flex、Grid、响应式和简单动效。示例优先沿用原课程中的基础代码和小案例，避免另起一套与原资料差距过大的示例体系。

## 学完后可以完成什么

完成课程后，你可以：

- 使用外部 CSS 文件控制 HTML 页面样式
- 通过标签、类、ID 和复合选择器选中页面元素
- 看懂多 class 组合、属性选择器和常见伪类状态
- 设置字体、文本、颜色、背景和链接状态
- 理解层叠、继承、优先级和 `!important`
- 使用盒模型处理宽高、边框、内边距和外边距
- 处理块元素、行内元素、行内块元素和溢出内容
- 看懂旧代码中的少量浮动布局
- 使用定位处理固定区域、遮罩、角标和层级
- 使用 Flexbox 和 Grid 完成常见布局
- 使用媒体查询完成基础响应式调整
- 使用 CSS 变量整理颜色等重复值
- 整理 CSS 命名和文件组织，为后续 CSS 框架和 UI 框架学习打基础

## 学习范围

### 必须掌握

- CSS 语法和代码书写格式
- CSS 的三种引入方式，主线使用外部样式表
- 基础选择器、复合选择器、属性选择器、常见伪类和多 class 组合
- 字体属性、文本属性、颜色和背景
- 层叠性、继承性和优先级
- 盒模型、`box-sizing`、宽高和内外边距
- 显示模式、正常文档流、表单控件样式和溢出
- 定位、`z-index`、显示与隐藏
- Flexbox 一维布局
- Grid 二维布局基础：列、`fr`、`repeat()` 和 `gap`
- 媒体查询和基础响应式思路
- CSS 变量、命名、文件组织和基础覆盖思路

### 会看懂即可

- `float` 和清除浮动在旧页面中的写法
- 背景图定位和背景复合写法
- `transition`、`transform` 和简单 `animation`
- 单行文本省略
- Grid 的区域命名、跨行跨列等复杂写法

### 不作为本课程主线

- CSS 精灵图
- 字体图标下载和追加流程
- CSS 三角形技巧
- 大量私有浏览器前缀
- 3D 转换效果
- `rem` + flexible.js 适配方案
- Less 安装和编译流程
- Bootstrap 栅格和组件类
- 旧式电商 PC 站、移动端单独站完整仿站

这些内容可能出现在历史资料或旧项目中，但不是新人进入日本 IT 企业项目时最需要优先掌握的主线能力。遇到旧代码时先能读懂和小范围修改；新页面优先使用清晰的选择器、盒模型、Flexbox、Grid 和响应式规则。

## 课程顺序

1. [认识 CSS 与引入样式](01_css_intro.md)
2. [选择器与 CSS 的三大特性](02_selectors_cascade.md)
3. [文字、颜色与背景](03_text_color_background.md)
4. [盒模型与间距](04_box_model_spacing.md)
5. [显示模式、文档流与溢出](05_display_flow_overflow.md)
6. [定位、层级与显示隐藏](06_position_zindex_visibility.md)
7. [Flexbox 一维布局](07_flexbox_layout.md)
8. [Grid 二维布局](08_grid_layout.md)
9. [响应式布局与媒体查询](09_responsive_media.md)
10. [变量、命名与样式组织](10_variables_naming_organization.md)
11. [过渡、动画与可用性](11_transition_animation_accessibility.md)
12. [CSS 新人综合练习：有給休暇申請システム](12_css_project.md)

## 练习目录

建议使用下面的练习目录：

```text
css-demo/
├─ index.html
├─ css/
│  └─ style.css
└─ images/
   └─ bg.jpg
```

每章可以新建一个独立 HTML 文件，也可以在 `index.html` 中替换本章示例。练习时优先使用外部样式表；只有在观察单个知识点时，才临时使用内部样式表。

课程最后的综合练习使用统一的前端递进练习：

- HTML 阶段：[HTML 递进练习：有給休暇申請システム](../training/01_html_task.md)
- CSS 阶段：[CSS 递进练习：有給休暇申請システム](../training/02_css_task.md)
- JavaScript 阶段：[JavaScript 递进练习：有給休暇申請システム](../training/03_javascript_task.md)
