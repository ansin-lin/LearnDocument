# 图片标签

## 本章目标

完成本章后，你可以：

- 使用 `img` 在页面中显示图片
- 正确填写 `src` 和 `alt`
- 使用 `width`、`height` 提供图片尺寸
- 区分信息图片和装饰图片
- 排查图片路径和替代文本问题

## 1. 使用 `img` 显示图片

`img` 是空元素：

```html
<img src="images/meeting-room.jpg" alt="公司三楼会议室">
```

它通过 `src` 找到图片，通过 `alt` 说明图片表达的信息。

## 2. `img` 的常用属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `src` | 相对路径、绝对 URL | 必须填写 | 指定图片文件的位置 |
| `alt` | 普通文本或空字符串 `""` | 必须根据图片用途填写 | 提供图片的文字替代 |
| `width` | 大于 0 的整数，单位是 CSS 像素 | 选填；默认使用图片自身尺寸 | 提供图片显示宽度 |
| `height` | 大于 0 的整数，单位是 CSS 像素 | 选填；默认使用图片自身尺寸 | 提供图片显示高度 |
| `loading` | `eager`、`lazy` | 选填；默认通常相当于立即加载 | 指定是否允许延迟加载 |

### 2.1 `src`：图片路径

项目目录：

```text
html-event-site/
├─ index.html
└─ images/
   └─ meeting-room.jpg
```

在 `index.html` 中使用：

```html
<img src="images/meeting-room.jpg" alt="公司三楼会议室">
```

浏览器会从当前文件所在目录进入 `images`，再查找 `meeting-room.jpg`。

### 2.2 `alt`：图片替代文本

`alt` 的写法取决于图片在当前页面中的作用。

#### 信息图片

图片提供了正文没有表达的信息：

```html
<img src="images/meeting-room.jpg" alt="设有投影屏幕和四十个座位的三楼会议室">
```

替代文本应表达图片对当前内容的意义，不需要写“这是一张图片”。

#### 装饰图片

图片只用于装饰，不增加信息：

```html
<img src="images/decoration-line.png" alt="">
```

装饰图片仍然保留 `alt`，但属性值为空字符串。

#### 不合适的写法

```html
<!-- 错误：缺少 alt -->
<img src="images/meeting-room.jpg">

<!-- 不清楚：没有表达图片用途 -->
<img src="images/meeting-room.jpg" alt="图片">
```

### 2.3 `width` 和 `height`：图片尺寸

```html
<img
    src="images/meeting-room.jpg"
    alt="公司三楼会议室"
    width="640"
    height="360"
>
```

属性值只写正整数，不写 `px`：

```html
<!-- 正确 -->
<img src="images/meeting-room.jpg" alt="公司三楼会议室" width="640" height="360">

<!-- 错误 -->
<img src="images/meeting-room.jpg" alt="公司三楼会议室" width="640px" height="360px">
```

最好按照图片原始比例同时填写宽高。随意填写不匹配的比例会让图片变形。

### 2.4 `loading="lazy"`：延迟加载

页面下方的非关键图片可以写：

```html
<img
    src="images/meeting-room.jpg"
    alt="公司三楼会议室"
    width="640"
    height="360"
    loading="lazy"
>
```

| 值 | 作用 |
| --- | --- |
| `eager` | 立即加载 |
| `lazy` | 浏览器可以等图片接近可见区域时再加载 |

本项目首页只有一张主要图片，不要求使用 `lazy`。会看懂即可。

## 3. 带说明的图片：`figure` 和 `figcaption`

当图片需要可见说明时，可以使用：

```html
<figure>
    <img
        src="images/meeting-room.jpg"
        alt="公司三楼会议室内部"
        width="640"
        height="360"
    >
    <figcaption>活动会场：公司三楼会议室</figcaption>
</figure>
```

- `figure`：包含一项可以独立引用的内容
- `figcaption`：这项内容的可见说明

`figcaption` 不能代替 `alt`。图片无法显示时，`alt` 仍然负责提供替代信息。

## 4. 图片作为链接

图片也可以放在链接中：

```html
<a href="index.html">
    <img src="images/event-logo.png" alt="公司技术交流会首页">
</a>
```

此时 `alt` 应说明链接的目的，而不是只描述图片外观。

## 5. 当前只需了解的图片属性

| 属性 | 简单说明 |
| --- | --- |
| `srcset` | 为不同显示条件提供多个图片文件，后续扩展课程再学习 |
| `sizes` | 与 `srcset` 配合说明图片显示尺寸 |
| `usemap` | 为图片设置可点击区域，普通项目很少使用 |
| `referrerpolicy` | 控制加载图片时发送的来源信息，当前不展开 |

过时的 `border`、`align`、`hspace`、`vspace` 不应在新 HTML 中使用。

## 6. 更新贯穿项目

准备一张自己有权使用的会议室图片，保存为：

```text
html-event-site/images/meeting-room.jpg
```

在 `index.html` 的“活动介绍”段落后添加：

```html
<figure>
    <img
        src="images/meeting-room.jpg"
        alt="公司三楼技术交流会会议室"
        width="640"
        height="360"
    >
    <figcaption>活动会场：公司三楼会议室</figcaption>
</figure>
```

如果没有可使用的图片，可以先跳过显示效果验证，但必须保留正确目录和代码结构，之后再补充资源。

## 7. 验证与排错

### 成功验证

1. 刷新首页。
2. 图片应正常显示。
3. 图片下方应显示会场说明。
4. 临时把文件名改错，确认浏览器会显示替代文本。
5. 恢复正确文件名，再次确认图片显示。

### 常见失败

| 现象 | 可能原因 | 修正方式 |
| --- | --- | --- |
| 显示破损图片图标 | `src` 路径或文件名错误 | 对照目录逐级检查 |
| 图片在自己电脑可用，交给别人失效 | 使用了 `C:\...` 等本机绝对路径 | 改用项目内相对路径 |
| 图片变形 | `width` 与 `height` 比例错误 | 使用图片原始比例 |
| 图片失效时没有说明 | 缺少 `alt` 或内容无意义 | 根据图片用途重写 `alt` |

## 8. 改修练习

假设活动 Logo 位于 `images/event-logo.png`。请在首页增加一个返回首页的 Logo 链接，并确保：

- `href` 指向 `index.html`
- `src` 指向正确图片
- `alt` 说明“返回公司技术交流会首页”
- 不使用本机绝对路径

