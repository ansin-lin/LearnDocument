# 链接与文件路径

## 本章目标

完成本章后，你可以：

- 使用 `a` 创建页面链接
- 解释 `href`、`target`、`download` 的作用
- 区分同级、下级和上级相对路径
- 创建页面内跳转
- 连接项目中的多个 HTML 页面

## 1. 链接标签 `a`

链接让用户从当前位置前往另一个页面或另一个位置。

```html
<a href="schedule.html">查看活动日程</a>
```

`a` 是 anchor 的缩写。链接文字要说明点击后会去哪里，不要只写“点击这里”。

## 2. `a` 的常用属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `href` | 页面路径、完整 URL、`#id`、邮箱地址等 | 创建链接时必须填写 | 指定链接目标 |
| `target` | `_self`、`_blank` | 选填；默认 `_self` | 指定在当前页或新标签页打开 |
| `download` | 空值或建议文件名 | 选填；默认不下载 | 提示浏览器下载链接资源 |

### 2.1 `href`：链接目标

链接到同一目录中的页面：

```html
<a href="schedule.html">查看活动日程</a>
```

链接到外部网站：

```html
<a href="https://www.w3.org/">访问 W3C 网站</a>
```

`href` 不应该留空，也不要使用链接标签假装按钮。前往其他地址使用链接，提交表单等操作使用后续课程中的 `button`。

### 2.2 `target`：打开位置

```html
<a href="schedule.html" target="_self">在当前页面打开日程</a>
<a href="https://www.w3.org/" target="_blank">在新标签页打开 W3C</a>
```

| 值 | 结果 |
| --- | --- |
| `_self` | 在当前页面打开，也是默认值 |
| `_blank` | 通常在新的浏览器标签页打开 |

站内页面通常使用默认的 `_self`。需要让用户保留当前页面时，才考虑 `_blank`。

### 2.3 `download`：下载资源

```html
<a href="files/event-guide.pdf" download>下载活动资料</a>
```

带有 `download` 时，浏览器通常会尝试下载资源。实际行为也可能受到浏览器和资源所在网站的限制。当前项目不需要准备下载文件，认识写法即可。

## 3. 相对路径

相对路径以当前 HTML 文件的位置为起点。

### 3.1 同级文件

```text
html-event-site/
├─ index.html
└─ schedule.html
```

从 `index.html` 链接到 `schedule.html`：

```html
<a href="schedule.html">活动日程</a>
```

### 3.2 下级目录

```text
html-event-site/
├─ index.html
└─ images/
   └─ meeting-room.jpg
```

从 `index.html` 引用图片：

```html
<img src="images/meeting-room.jpg" alt="公司三楼会议室">
```

### 3.3 上级目录

```text
html-event-site/
├─ index.html
└─ pages/
   └─ detail.html
```

从 `pages/detail.html` 返回上一级的 `index.html`：

```html
<a href="../index.html">返回首页</a>
```

`../` 表示上一级目录。路径中统一使用正斜杠 `/`。

## 4. 页面内跳转

先给目标元素设置唯一的 `id`：

```html
<h2 id="registration">报名说明</h2>
```

再使用 `#id` 创建链接：

```html
<a href="#registration">跳到报名说明</a>
```

也可以从另一个页面跳到指定位置：

```html
<a href="index.html#registration">查看报名说明</a>
```

如果 `href` 中的名称与目标 `id` 不一致，浏览器就无法跳到目标位置。

## 5. 邮箱链接

`mailto:` 可以请求系统打开邮件应用：

```html
<a href="mailto:event@example.com">联系活动负责人</a>
```

| `href` 值 | 结果 |
| --- | --- |
| `mailto:event@example.com` | 请求打开系统默认邮件应用并填写收件人 |

邮箱地址会直接出现在 HTML 中，因此公开网站可能收到垃圾邮件。课程示例只使用示例地址。

## 6. 创建项目页面

在 `html-event-site` 中新建 `schedule.html`、`register.html` 和 `success.html`。

### `schedule.html`

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>活动日程｜公司技术交流会</title>
</head>
<body>
    <h1>活动日程</h1>
    <p>详细日程将在表格章节中完成。</p>
    <p><a href="index.html">返回活动首页</a></p>
</body>
</html>
```

### `register.html`

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>活动报名｜公司技术交流会</title>
</head>
<body>
    <h1>活动报名</h1>
    <p>报名表将在表单章节中完成。</p>
    <p><a href="index.html">返回活动首页</a></p>
</body>
</html>
```

### `success.html`

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>报名完成｜公司技术交流会</title>
</head>
<body>
    <h1>报名完成</h1>
    <p>已收到你的报名信息。</p>
    <p><a href="index.html">返回活动首页</a></p>
</body>
</html>
```

## 7. 更新首页导航

在 `index.html` 的 `h1` 后面添加：

```html
<p>
    <a href="index.html">活动首页</a>
    <a href="schedule.html">活动日程</a>
    <a href="register.html">活动报名</a>
</p>
```

在“参加提示”之后添加：

```html
<h2 id="registration">报名说明</h2>
<p><a href="register.html">前往报名页面</a></p>
```

### 验证

1. 从首页依次打开日程页和报名页。
2. 从两个页面都能返回首页。
3. 在地址栏中输入 `index.html#registration`，页面应跳到报名说明。
4. 检查所有页面的浏览器标签页标题是否不同。

## 8. 路径排错

| 现象 | 常见原因 | 修正方式 |
| --- | --- | --- |
| 点击链接显示找不到文件 | 文件名或路径拼写错误 | 对照目录检查每个字符 |
| Windows 上可用，换到其他系统失败 | 文件名大小写不一致 | 统一文件名和链接中的大小写 |
| 页内链接没有跳转 | `href="#名称"` 与目标 `id` 不一致 | 让两处名称完全一致 |
| 链接打开了错误页面 | 复制链接后忘记修改 `href` | 同时检查链接文字和 `href` |

## 9. 改修练习

在 `schedule.html` 和 `register.html` 中都加入三个导航链接，使每个页面都能前往首页、日程页和报名页。逐一点击验证，不允许出现失效链接。

