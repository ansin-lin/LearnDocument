# 常用表单控件

## 本章目标

完成本章后，你可以：

- 根据数据选择合适的 `input type`
- 正确设置单选框和复选框的 `name`、`value`
- 使用 `select`、`option`、`textarea`
- 使用 `fieldset` 和 `legend` 为相关控件分组
- 扩充活动报名表

## 1. 常用 `input type`

`input` 通过 `type` 显示不同输入控件。

| `type` 值 | 收集的数据 | 典型场景 | 当前掌握程度 |
| --- | --- | --- | --- |
| `text` | 单行普通文本 | 姓名、员工编号 | 必须掌握 |
| `email` | 邮箱地址 | 联系邮箱 | 必须掌握 |
| `tel` | 电话号码 | 联系电话 | 会使用 |
| `number` | 数值 | 人数、数量 | 会使用 |
| `date` | 日期 | 参加日期 | 会使用 |
| `radio` | 一组中的单个选项 | 参加方式 | 必须掌握 |
| `checkbox` | 一个或多个选项 | 同意事项、希望主题 | 必须掌握 |
| `file` | 本地文件 | 上传资料 | 当前了解 |
| `hidden` | 页面中不可见的值 | 页面需要随表单提交的固定标识 | 当前了解 |
| `password` | 遮挡显示的单行文本 | 密码 | 当前了解，本项目不用 |
| `submit` | 提交按钮 | 提交表单 | 会看懂，项目使用 `button` |
| `reset` | 恢复初始值 | 少数需要重置的表单 | 简单了解 |

不同 `type` 的外观可能因浏览器和操作系统而不同，这是正常现象。

## 2. 单行文本、邮箱和电话

```html
<p>
    <label for="employee-name">姓名</label>
    <input id="employee-name" name="employeeName" type="text">
</p>

<p>
    <label for="email">联系邮箱</label>
    <input id="email" name="email" type="email">
</p>

<p>
    <label for="phone">联系电话</label>
    <input id="phone" name="phone" type="tel">
</p>
```

`email` 会让浏览器执行基础邮箱格式检查。`tel` 不会自动规定全世界统一的电话号码格式。

## 3. 单选框 `radio`

一组选项只能选择一个时使用 `radio`：

```html
<fieldset>
    <legend>参加方式</legend>

    <input id="onsite" name="attendanceType" type="radio" value="onsite" checked>
    <label for="onsite">现场参加</label>

    <input id="online" name="attendanceType" type="radio" value="online">
    <label for="online">在线参加</label>
</fieldset>
```

### 单选框的关键属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `type` | `radio` | 必须填写 | 创建单选框 |
| `name` | 同一组使用完全相同的名称 | 必须填写 | 决定哪些单选框属于同一组 |
| `value` | 每个选项不同的提交值 | 必须填写 | 表示用户选择了哪个选项 |
| `checked` | 布尔属性 | 选填；默认未选中 | 设置初始选中项 |
| `id` | 页面内唯一名称 | 与标签关联时必须填写 | 对应 `label for` |

同组单选框必须使用相同 `name`，但 `id` 和 `value` 必须能区分每个选项。

错误：

```html
<!-- name 不同，会导致两个选项可以同时选中 -->
<input id="onsite" name="onsite" type="radio" value="onsite">
<input id="online" name="online" type="radio" value="online">
```

同一组通常只设置一个 `checked`。

## 4. 复选框 `checkbox`

可以选择多个项目时使用 `checkbox`：

```html
<fieldset>
    <legend>希望了解的主题</legend>

    <input id="topic-review" name="topics" type="checkbox" value="review">
    <label for="topic-review">代码审查</label>

    <input id="topic-test" name="topics" type="checkbox" value="test">
    <label for="topic-test">自测方法</label>

    <input id="topic-communication" name="topics" type="checkbox" value="communication">
    <label for="topic-communication">项目沟通</label>
</fieldset>
```

### 复选框的关键属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `type` | `checkbox` | 必须填写 | 创建复选框 |
| `name` | 提交时使用的数据名称 | 需要提交时必须填写 | 标识这组数据 |
| `value` | 当前选项对应的值 | 建议明确填写 | 标识选择了哪一项 |
| `checked` | 布尔属性 | 选填；默认未选中 | 设置初始选中项 |
| `id` | 页面内唯一名称 | 与标签关联时必须填写 | 对应 `label for` |

同类复选框可以使用同一个 `name`，被选中的多个值会分别提交。

## 5. 下拉选择 `select`

```html
<p>
    <label for="department">所属部门</label>
    <select id="department" name="department">
        <option value="">请选择</option>
        <option value="development">开发部</option>
        <option value="quality">品质管理部</option>
        <option value="operations">运营部</option>
    </select>
</p>
```

### `select` 常用属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `id` | 页面内唯一名称 | 与标签关联时必须填写 | 对应 `label for` |
| `name` | 提交数据使用的名称 | 需要提交时必须填写 | 标识选择结果 |
| `multiple` | 布尔属性 | 选填；默认只能选择一个 | 允许选择多个选项 |
| `disabled` | 布尔属性 | 选填；默认可用 | 禁止用户操作该控件 |

普通部门选择只允许一个结果，不使用 `multiple`。

### `option` 常用属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `value` | 提交给目标的文本 | 建议明确填写；省略时通常使用选项文字 | 指定选项的提交值 |
| `selected` | 布尔属性 | 选填；默认通常选中第一项 | 设置初始选项 |
| `disabled` | 布尔属性 | 选填；默认可选 | 禁止选择该选项 |

示例：

```html
<option value="development" selected>开发部</option>
```

## 6. 多行文本 `textarea`

`textarea` 用于输入较长内容：

```html
<p>
    <label for="question">希望现场解答的问题</label><br>
    <textarea id="question" name="question" rows="5" cols="40"></textarea>
</p>
```

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `id` | 页面内唯一名称 | 与标签关联时必须填写 | 对应 `label for` |
| `name` | 提交数据使用的名称 | 需要提交时必须填写 | 标识输入内容 |
| `rows` | 大于 0 的整数 | 选填；浏览器有默认值 | 建议显示的文本行数 |
| `cols` | 大于 0 的整数 | 选填；浏览器有默认值 | 建议显示的字符列数 |
| `placeholder` | 提示文本 | 选填；默认没有 | 在空白时提示输入内容 |

`textarea` 的初始文字写在开始和结束标签之间：

```html
<textarea id="question" name="question" rows="5" cols="40">希望了解代码审查流程。</textarea>
```

普通空白输入框应确保两个标签之间没有意外文字。

## 7. 使用 `fieldset` 和 `legend` 分组

相关的一组控件应放在 `fieldset` 中，并用 `legend` 说明整组问题：

```html
<fieldset>
    <legend>参加方式</legend>
    <!-- 这一组单选框 -->
</fieldset>
```

不要只依靠普通段落说明一组单选框或复选框。

## 8. 文件和隐藏输入：当前了解

文件选择：

```html
<input id="attachment" name="attachment" type="file" accept=".pdf">
```

`accept` 可以提示允许选择的文件类型，但不能代替接收文件一方的检查。当前本地项目无法真正保存上传文件，所以不加入报名表。

隐藏输入：

```html
<input name="eventId" type="hidden" value="tech-meeting-2026-09">
```

`hidden` 输入不会显示，但访问者仍然可以查看和修改 HTML。绝不能把密码或秘密信息放在其中。

## 9. 扩充报名表

在 `register.html` 的姓名和员工编号之后，继续添加：

```html
<p>
    <label for="email">联系邮箱</label>
    <input id="email" name="email" type="email">
</p>

<p>
    <label for="department">所属部门</label>
    <select id="department" name="department">
        <option value="">请选择</option>
        <option value="development">开发部</option>
        <option value="quality">品质管理部</option>
        <option value="operations">运营部</option>
    </select>
</p>

<fieldset>
    <legend>参加方式</legend>

    <input id="onsite" name="attendanceType" type="radio" value="onsite" checked>
    <label for="onsite">现场参加</label>

    <input id="online" name="attendanceType" type="radio" value="online">
    <label for="online">在线参加</label>
</fieldset>

<fieldset>
    <legend>希望了解的主题</legend>

    <input id="topic-review" name="topics" type="checkbox" value="review">
    <label for="topic-review">代码审查</label>

    <input id="topic-test" name="topics" type="checkbox" value="test">
    <label for="topic-test">自测方法</label>

    <input id="topic-communication" name="topics" type="checkbox" value="communication">
    <label for="topic-communication">项目沟通</label>
</fieldset>

<p>
    <label for="question">希望现场解答的问题</label><br>
    <textarea id="question" name="question" rows="5" cols="40"></textarea>
</p>
```

保持提交按钮位于 `form` 的最后。

## 10. 验证

1. 点击每个可见标签，检查是否操作了正确控件。
2. 两个参加方式不能同时选中。
3. 三个希望主题可以选择多个。
4. 选择部门并输入问题后提交。
5. 地址栏中检查 `department`、`attendanceType`、`topics` 和 `question`。


