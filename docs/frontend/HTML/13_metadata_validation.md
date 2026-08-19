# 页面元数据与 HTML 检查

## 本章目标

完成本章后，你可以：

- 为每个页面设置语言、字符编码和独立标题
- 使用 `description`、`viewport` 和网站图标
- 区分正文内容和 `head` 中的页面信息
- 使用浏览器和 HTML 检查工具发现问题
- 完成项目交付前的 HTML 自测

## 1. `head` 中的信息

本项目的完整 `head` 可以写成：

```html
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="面向项目开发新人的公司技术交流会介绍与报名页面">
    <link rel="icon" href="images/favicon.ico">
    <title>公司技术交流会</title>
</head>
```

这些信息主要帮助浏览器和其他工具理解页面，不作为普通正文显示。

## 2. 字符编码

```html
<meta charset="UTF-8">
```

| 标签/属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `meta charset` | 本课程统一 `UTF-8` | 完整页面必须明确设置 | 声明 HTML 文件字符编码 |

它应放在 `head` 的前部。文件本身也应以 UTF-8 保存。

## 3. 页面标题 `title`

每个页面都要有能区分内容的 `title`：

```html
<!-- index.html -->
<title>公司技术交流会</title>

<!-- schedule.html -->
<title>活动日程｜公司技术交流会</title>

<!-- register.html -->
<title>活动报名｜公司技术交流会</title>

<!-- success.html -->
<title>报名完成｜公司技术交流会</title>
```

`title` 显示在浏览器标签页中。它与正文中的 `h1` 职责不同，两者都需要。

## 4. 页面说明 `description`

```html
<meta name="description" content="面向项目开发新人的公司技术交流会介绍与报名页面">
```

### `meta` 的常用属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `name` | 元数据名称，如 `description`、`viewport` | 使用名称型元数据时必须填写 | 说明这条信息的种类 |
| `content` | 与 `name` 对应的文本 | 使用名称型元数据时必须填写 | 提供具体内容 |

每个页面的 `description` 应准确说明当前页面，不要机械复制无关文字。

旧资料中可能出现 `meta keywords`。当前课程不使用它，也不把它列为必备元数据。

## 5. 视口信息 `viewport`

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

它告诉移动设备按照设备可用宽度显示页面，并使用正常的初始缩放。当前只需掌握这条标准写法，不展开响应式布局。

| `content` 片段 | 作用 |
| --- | --- |
| `width=device-width` | 让页面视口宽度跟随设备宽度 |
| `initial-scale=1.0` | 设置正常的初始缩放比例 |

不要添加禁止用户缩放的设置。

## 6. 网站图标

准备图标：

```text
html-event-site/images/favicon.ico
```

在 `head` 中引用：

```html
<link rel="icon" href="images/favicon.ico">
```

### `link` 的常用属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `rel` | 当前关系值使用 `icon` | 必须填写 | 说明链接资源与页面的关系 |
| `href` | 文件路径或 URL | 必须填写 | 指定资源位置 |
| `type` | MIME 类型，如 `image/png` | 选填；通常可由资源判断 | 提示资源类型 |

`rel="shortcut icon"` 是旧资料中常见写法，新页面使用 `rel="icon"` 即可。

没有图标文件时，先不要添加失效的 `link`。准备资源后再加入。

## 7. 当前不使用的历史内容

下面内容不进入本项目：

- `<meta http-equiv="X-UA-Compatible" content="IE=edge">`
- `meta keywords`
- 旧式页面编码写法
- 在 `head` 中写页面正文
- 使用 HTML 外观属性控制字体、颜色和布局

## 8. 浏览器检查

### 8.1 检查页面结果

逐页确认：

- 标签页标题与页面内容一致
- 中文和日文没有乱码
- 链接都能到达正确页面
- 图片显示正常，失效时有合适的替代文本
- 表格行列对应
- 表单标签可以点击并聚焦正确控件

### 8.2 查看页面源代码

浏览器通常提供“查看网页源代码”。打开后检查：

- `doctype` 是否位于第一行
- `html` 是否有 `lang`
- `head` 和 `body` 是否各有一个
- 页面正文是否都在 `body` 中
- 是否意外留下敏感注释

### 8.3 键盘检查

不使用鼠标，只使用 Tab、Shift+Tab、Enter 和空格：

1. 导航链接能够获得焦点。
2. Enter 可以打开链接。
3. 表单控件按照阅读顺序获得焦点。
4. 空格可以切换复选框。
5. 单选框可以在同组项目之间切换。
6. 提交按钮可以使用键盘操作。

## 9. 使用 HTML 检查工具

可以使用 W3C Markup Validation Service：

<https://validator.w3.org/>

选择上传文件，逐个检查 `index.html`、`schedule.html`、`register.html` 和 `success.html`。

检查工具常见提示包括：

- 结束标签缺失
- 标签嵌套顺序错误
- 属性重复
- 某个属性不能用于当前标签
- `id` 重复

自动工具无法判断标题文字是否合适、`alt` 是否真正表达图片作用，也不能代替人工检查。

## 10. 项目元数据要求

为四个页面分别补充：

| 文件 | `title` | `description` 要点 |
| --- | --- | --- |
| `index.html` | 公司技术交流会 | 活动介绍与报名入口 |
| `schedule.html` | 活动日程｜公司技术交流会 | 活动时间、主题和讲师 |
| `register.html` | 活动报名｜公司技术交流会 | 公司技术交流会报名表 |
| `success.html` | 报名完成｜公司技术交流会 | 报名提交完成提示 |

四个页面都保留：

```html
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

## 11. 交付前自测记录

完成检查后记录：

```text
检查日期：
检查人：

[ ] 4 个 HTML 文件均可打开
[ ] 页面之间的导航全部可用
[ ] 图片路径和 alt 已确认
[ ] 日程表结构已确认
[ ] 报名表标签和必填验证已确认
[ ] 键盘操作已确认
[ ] HTML 检查工具未发现结构错误
```

下一章会提供完整项目规格、测试步骤和验收标准。请先独立完成练习，再到第十五章核对参考答案。
