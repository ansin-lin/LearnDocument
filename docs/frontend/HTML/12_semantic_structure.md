# 语义化页面结构

## 本章目标

完成本章后，你可以：

- 使用 HTML 标签说明页面各区域的职责
- 正确使用 `header`、`nav`、`main`、`section`、`footer`
- 判断什么时候需要 `article`、`aside`、`div`、`span`
- 保持标题层级和页面阅读顺序清楚
- 把贯穿项目整理成完整语义结构

## 1. 什么是语义

语义就是“这个内容是什么、负责什么”。

下面两段代码都可能显示文字，但表达能力不同：

```html
<div>活动日程</div>
```

```html
<h1>活动日程</h1>
```

第二段明确说明这是页面主标题。浏览器、搜索工具和辅助工具都更容易理解。

优先选择职责明确的原生 HTML 标签，不用 `div` 代替标题、链接、按钮、列表、表格或表单控件。

## 2. 页面主要区域

```html
<body>
    <header>
        <!-- 页面或网站的开头 -->
    </header>

    <nav>
        <!-- 主要导航 -->
    </nav>

    <main>
        <!-- 当前页面的主要内容 -->
    </main>

    <footer>
        <!-- 页尾信息 -->
    </footer>
</body>
```

### `header`

放置页面或某一内容区域的标题、简介等开头信息。一个页面可以出现多个 `header`，但初学项目先使用一个页面头部。

### `nav`

包含一组主要导航链接：

```html
<nav>
    <ul>
        <li><a href="index.html">活动首页</a></li>
        <li><a href="schedule.html">活动日程</a></li>
        <li><a href="register.html">活动报名</a></li>
    </ul>
</nav>
```

少量正文中的普通链接不需要全部放进 `nav`。

### `main`

包含当前页面最主要且独有的内容。一个页面应只有一个可见的 `main`。

### `footer`

包含页尾说明、联系信息、版权信息或相关链接。

## 3. 内容区域标签

### `section`

`section` 表示一个有明确主题的内容区域，通常有自己的标题：

```html
<section>
    <h2>活动介绍</h2>
    <p>本次活动面向刚加入项目的开发人员。</p>
</section>
```

不要只为了包一层标签就使用 `section`。如果没有主题或标题，可能不需要它。

### `article`

`article` 表示可以独立阅读或单独发布的完整内容，例如一篇新闻、公告或博客文章：

```html
<article>
    <h2>九月技术交流会通知</h2>
    <p>本月活动将介绍项目开发流程。</p>
</article>
```

本项目首页整体是一份活动说明，不需要把每一个小节都写成 `article`。

### `aside`

`aside` 表示与主要内容有关、但不是主线的补充信息：

```html
<aside>
    <h2>参加提示</h2>
    <p>请提前十分钟入场。</p>
</aside>
```

## 4. `div` 和 `span`

### `div`

当一组内容确实需要放在一起，但没有更合适的语义标签时，可以使用 `div`。

```html
<div class="contact-block">
    <p>负责人：山田</p>
    <p>联系邮箱：event@example.com</p>
</div>
```

不要形成“所有内容都使用 `div`”的习惯。

### `span`

`span` 用于短文本中的一小段，没有额外语义：

```html
<p>剩余名额：<span>12</span> 人</p>
```

重要内容应使用 `strong`，强调内容应使用 `em`，不要用 `span` 代替它们。

## 5. 联系信息 `address`

`address` 用于当前页面、文章或网站的联系信息：

```html
<address>
    活动负责人：山田<br>
    <a href="mailto:event@example.com">event@example.com</a>
</address>
```

普通地点地址不一定使用 `address`；它的重点是“联系方式”。

## 6. 整理首页

用以下 `body` 结构替换 `index.html` 当前正文。原有内容没有删除，只是按照职责重新组合。

```html
<body>
    <header>
        <h1 id="page-title">公司技术交流会</h1>
        <p>面向项目开发新人的内部交流活动</p>
    </header>

    <nav>
        <ul>
            <li><a href="index.html">活动首页</a></li>
            <li><a href="schedule.html">活动日程</a></li>
            <li><a href="register.html">活动报名</a></li>
        </ul>
    </nav>

    <main>
        <section>
            <h2>活动介绍</h2>
            <p>本次活动面向刚加入项目的开发人员。</p>
            <p>参加者可以了解项目开发流程和常见沟通方式。</p>

            <figure>
                <img
                    src="images/meeting-room.jpg"
                    alt="公司三楼技术交流会会议室"
                    width="640"
                    height="360"
                >
                <figcaption>活动会场：公司三楼会议室</figcaption>
            </figure>
        </section>

        <section>
            <h2>活动信息</h2>
            <p>活动时间：2026年9月18日 14:00</p>
            <p>
                活动地点：东京都千代田区示例町1-2-3<br>
                示例大厦3楼
            </p>
            <p id="registration-deadline">
                <strong>报名截止：2026年9月15日</strong>
            </p>
        </section>

        <section>
            <h2>分享主题</h2>
            <ul>
                <li>项目开发流程</li>
                <li>代码审查中的常见指摘</li>
                <li>新人如何整理自测结果</li>
            </ul>
        </section>

        <section>
            <h2>报名步骤</h2>
            <ol>
                <li><a href="schedule.html">查看活动日程</a></li>
                <li><a href="register.html">打开报名页面</a></li>
                <li>填写并提交报名信息</li>
            </ol>
        </section>

        <aside>
            <h2>参加提示</h2>
            <p>请在活动开始前 <em>十分钟</em> 入场。</p>
            <p><small>活动安排可能根据现场情况调整。</small></p>
        </aside>
    </main>

    <footer>
        <address>
            活动负责人：山田<br>
            <a href="mailto:event@example.com">event@example.com</a>
        </address>
    </footer>
</body>
```

如果尚未准备会议室图片，可以临时保留 `figure` 代码并继续检查其他结构。

## 7. 整理其他页面

`schedule.html` 和 `register.html` 也使用相同外层顺序：

```html
<body>
    <header>
        <h1>当前页面标题</h1>
    </header>

    <nav>
        <!-- 三个页面导航链接组成的列表 -->
    </nav>

    <main>
        <!-- 原来的表格或表单 -->
    </main>

    <footer>
        <p><a href="mailto:event@example.com">联系活动负责人</a></p>
    </footer>
</body>
```

只能替换外层结构，不要丢失已经完成的日程表和报名表。

## 8. 验证

- 每个页面都有一个明确的 `h1`
- 标题等级按内容关系排列
- 主要导航放在 `nav` 中
- 当前页面主要内容放在 `main` 中
- 表格仍然只用于日程数据
- 表单仍然保留所有 `label` 和控件属性
- 使用键盘的 Tab 键可以依次到达链接和表单控件

## 9. Review 改修任务

收到以下 Review 指摘：

> “活动日程”只是普通 `div`，不能明确表示页面标题。

请查找并改为 `h1`。修改后检查页面中是否出现了两个 `h1`，以及后续标题是否从 `h2` 开始。

