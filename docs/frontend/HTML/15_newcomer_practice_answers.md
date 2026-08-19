# 新人综合练习参考答案

## 1. 使用方法

本章对应[第十四章新人综合练习](14_html_project.md)。请先完成自己的版本、浏览器测试和交付记录，再使用本章核对。

核对时不要只比较文字是否完全相同，应重点检查：

- 文档结构是否完整
- 标签是否表达正确职责
- 文件路径和页面导航是否一致
- 图片是否有合适的替代文本
- 表格的表头与数据是否对应
- 表单的 `for`、`id`、`name`、`value` 是否一致
- 常用属性是否符合练习规格

## 2. 四个页面的完整参考代码

只有在独立实现和排错后，再使用本节核对最终状态。

### 2.1 `index.html`

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="面向项目开发新人的公司技术交流会介绍与报名入口">
    <link rel="icon" href="images/favicon.ico">
    <title>公司技术交流会</title>
</head>
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
</html>
```

如果没有 `favicon.ico`，删除对应的 `link`。会场图片是项目验收资源，需要准备。

### 2.2 `schedule.html`

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="公司技术交流会的时间、分享主题和讲师日程">
    <link rel="icon" href="images/favicon.ico">
    <title>活动日程｜公司技术交流会</title>
</head>
<body>
    <header>
        <h1>活动日程</h1>
    </header>

    <nav>
        <ul>
            <li><a href="index.html">活动首页</a></li>
            <li><a href="schedule.html">活动日程</a></li>
            <li><a href="register.html">活动报名</a></li>
        </ul>
    </nav>

    <main>
        <table>
            <caption>2026年9月18日活动日程</caption>
            <thead>
                <tr>
                    <th scope="col">时间</th>
                    <th scope="col">主题</th>
                    <th scope="col">讲师</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>14:00</td>
                    <td>项目开发流程</td>
                    <td>田中</td>
                </tr>
                <tr>
                    <td>14:30</td>
                    <td>开发环境准备</td>
                    <td>佐藤</td>
                </tr>
                <tr>
                    <td>15:00</td>
                    <td>代码审查经验</td>
                    <td>李</td>
                </tr>
                <tr>
                    <td>16:00</td>
                    <td colspan="2">全体提问与交流</td>
                </tr>
            </tbody>
        </table>

        <p><a href="register.html">确认日程后前往报名</a></p>
    </main>

    <footer>
        <p><a href="mailto:event@example.com">联系活动负责人</a></p>
    </footer>
</body>
</html>
```

### 2.3 `register.html`

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="公司技术交流会报名表">
    <link rel="icon" href="images/favicon.ico">
    <title>活动报名｜公司技术交流会</title>
</head>
<body>
    <header>
        <h1>活动报名</h1>
        <p>请填写以下信息。标有“必填”的项目必须完成。</p>
    </header>

    <nav>
        <ul>
            <li><a href="index.html">活动首页</a></li>
            <li><a href="schedule.html">活动日程</a></li>
            <li><a href="register.html">活动报名</a></li>
        </ul>
    </nav>

    <main>
        <form action="success.html" method="get">
            <p>
                <label for="employee-name">姓名（必填）</label>
                <input
                    id="employee-name"
                    name="employeeName"
                    type="text"
                    minlength="2"
                    maxlength="40"
                    autocomplete="name"
                    required
                >
            </p>

            <p>
                <label for="employee-id">员工编号（必填）</label>
                <input
                    id="employee-id"
                    name="employeeId"
                    type="text"
                    minlength="4"
                    maxlength="10"
                    required
                >
            </p>

            <p>
                <label for="email">联系邮箱（必填）</label>
                <input
                    id="email"
                    name="email"
                    type="email"
                    placeholder="name@example.com"
                    autocomplete="email"
                    required
                >
            </p>

            <p>
                <label for="department">所属部门（必填）</label>
                <select id="department" name="department" required>
                    <option value="">请选择</option>
                    <option value="development">开发部</option>
                    <option value="quality">品质管理部</option>
                    <option value="operations">运营部</option>
                </select>
            </p>

            <fieldset>
                <legend>参加方式（必填）</legend>

                <input
                    id="onsite"
                    name="attendanceType"
                    type="radio"
                    value="onsite"
                    checked
                    required
                >
                <label for="onsite">现场参加</label>

                <input
                    id="online"
                    name="attendanceType"
                    type="radio"
                    value="online"
                >
                <label for="online">在线参加</label>
            </fieldset>

            <fieldset>
                <legend>希望了解的主题</legend>

                <input id="topic-review" name="topics" type="checkbox" value="review">
                <label for="topic-review">代码审查</label>

                <input id="topic-test" name="topics" type="checkbox" value="test">
                <label for="topic-test">自测方法</label>

                <input
                    id="topic-communication"
                    name="topics"
                    type="checkbox"
                    value="communication"
                >
                <label for="topic-communication">项目沟通</label>
            </fieldset>

            <p>
                <label for="question">希望现场解答的问题</label><br>
                <textarea
                    id="question"
                    name="question"
                    rows="5"
                    cols="40"
                    maxlength="500"
                ></textarea>
            </p>

            <button type="submit">提交报名</button>
        </form>
    </main>

    <footer>
        <p><a href="mailto:event@example.com">联系活动负责人</a></p>
    </footer>
</body>
</html>
```

### 2.4 `success.html`

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="公司技术交流会报名提交完成提示">
    <link rel="icon" href="images/favicon.ico">
    <title>报名完成｜公司技术交流会</title>
</head>
<body>
    <header>
        <h1>报名完成</h1>
    </header>

    <nav>
        <ul>
            <li><a href="index.html">活动首页</a></li>
            <li><a href="schedule.html">活动日程</a></li>
            <li><a href="register.html">活动报名</a></li>
        </ul>
    </nav>

    <main>
        <p>已完成浏览器中的报名提交操作。</p>
        <p>这是纯 HTML 学习示例，输入内容没有保存到服务器。</p>
        <p><a href="index.html">返回活动首页</a></p>
    </main>

    <footer>
        <p><a href="mailto:event@example.com">联系活动负责人</a></p>
    </footer>
</body>
</html>
```
## 3. SES 改修参考答案

第十四章要求增加“上午场”和“下午场”，并且默认不选择。应在 `register.html` 的报名表中加入：

```html
<fieldset>
    <legend>参加场次（必填）</legend>

    <input
        id="session-morning"
        name="session"
        type="radio"
        value="morning"
        required
    >
    <label for="session-morning">上午场</label>

    <input
        id="session-afternoon"
        name="session"
        type="radio"
        value="afternoon"
    >
    <label for="session-afternoon">下午场</label>
</fieldset>
```

核对要点：

- 两个单选框的 `name` 都是 `session`，因此只能选择一个。
- 两个 `id` 不同，并分别与各自的 `label for` 一致。
- 两个 `value` 分别是 `morning` 和 `afternoon`。
- 两个选项都没有 `checked`，所以打开页面时默认不选择。
- 同组中的一个控件带有 `required`，提交前必须选择一个场次。

自测时至少执行三次：

1. 不选择场次，确认浏览器阻止提交。
2. 选择上午场，确认提交结果中包含 `session=morning`。
3. 选择下午场，确认提交结果中包含 `session=afternoon`。

## 4. 答案核对记录

```text
[ ] 我的页面可以独立打开
[ ] 我的导航链接与答案一致
[ ] 我的图片路径和 alt 符合规格
[ ] 我的日程表行列关系正确
[ ] 我的表单标签和属性对应正确
[ ] 我的必填与格式检查通过
[ ] 我的 SES 改修通过三种测试
[ ] 我已经记录与参考答案的差异
```

发现差异后，应修改自己的文件并重新测试，而不是只在记录中说明。
