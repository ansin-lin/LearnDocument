# 表单基础

## 本章目标

完成本章后，你可以：

- 使用 `form` 创建表单范围
- 使用 `label` 为输入框提供明确名称
- 解释 `action`、`method`、`type`、`id`、`name`、`value`
- 创建一个可以提交姓名的最小表单
- 从地址栏观察 GET 表单提交的数据

## 1. 表单用来收集信息

报名页面需要收集姓名、邮箱和参加场次。HTML 表单负责：

- 显示输入控件
- 标识每一项数据
- 检查部分填写规则
- 按照表单属性提交数据

HTML 本身不会把报名信息保存进数据库。本课程先观察浏览器怎样组织并提交数据。

## 2. 最小表单

```html
<form action="success.html" method="get">
    <label for="employee-name">姓名</label>
    <input id="employee-name" name="employeeName" type="text">
    <button type="submit">提交报名</button>
</form>
```

输入姓名并提交后，浏览器会打开 `success.html`。地址栏可能出现：

```text
success.html?employeeName=山田太郎
```

这说明 `name` 成为了数据名称，输入内容成为了对应的值。

## 3. `form` 的常用属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `action` | 提交目标的 URL 或文件路径 | 建议明确填写；空值表示当前页面 | 指定表单提交到哪里 |
| `method` | `get`、`post` | 选填；默认 `get` | 指定提交方式 |
| `autocomplete` | `on`、`off` | 选填；默认通常为 `on` | 提示浏览器是否可以自动填充 |

### 3.1 `action`

本项目提交后打开本地完成页：

```html
<form action="success.html">
```

这只是课程中的可观察结果。实际项目通常由服务器接收和处理数据。

### 3.2 `method`

```html
<form action="success.html" method="get">
```

| 值 | 初学阶段需要理解的行为 |
| --- | --- |
| `get` | 数据通常出现在目标地址的查询部分，适合本课程观察提交结果 |
| `post` | 数据不会放在地址的查询部分，通常需要服务器接收 |

本课程使用 `get`，因为本地文件也能观察结果。不要通过 GET 提交密码等敏感信息。

## 4. `label` 与输入框关联

`label` 说明输入框要求填写什么：

```html
<label for="employee-name">姓名</label>
<input id="employee-name" name="employeeName" type="text">
```

### `label` 的 `for`

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `for` | 页面中某个表单控件的 `id` | 显式关联时必须填写 | 把标签与对应控件关联 |

`for="employee-name"` 必须与输入框的 `id="employee-name"` 完全一致。

关联正确后：

- 点击“姓名”文字也可以把光标放入输入框
- 浏览器和辅助工具能理解标签属于哪个控件

`placeholder` 不能替代 `label`，因为输入内容后提示会消失。

## 5. `input` 的核心属性

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `type` | `text`、`email`、`radio` 等 | 选填；默认 `text` | 指定输入控件类型 |
| `id` | 页面中唯一的名称 | 与 `label for` 配合时必须填写 | 让标签找到这个控件 |
| `name` | 提交数据使用的名称 | 需要提交该数据时必须填写 | 标识提交的数据项 |
| `value` | 文本，含义随控件类型变化 | 文本框默认是用户输入内容 | 指定控件当前或提交时的值 |

### `type="text"`

```html
<input id="employee-name" name="employeeName" type="text">
```

用于输入单行普通文字。

### `name` 决定提交数据的名称

```html
<input id="employee-name" name="employeeName" type="text">
```

如果没有 `name`，输入框仍然显示，但它的值不会作为这项表单数据提交。

### 文本框中的 `value`

```html
<input id="employee-name" name="employeeName" type="text" value="示例姓名">
```

文本框会预先显示“示例姓名”，用户可以修改。普通报名表通常不需要预填别人的姓名，所以当前项目不设置该 `value`。

## 6. 提交按钮

推荐使用 `button`：

```html
<button type="submit">提交报名</button>
```

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `type` | `submit`、`button`、`reset` | 建议明确填写；在表单中默认通常是 `submit` | 指定按钮行为 |

| 值 | 行为 |
| --- | --- |
| `submit` | 提交表单 |
| `button` | 普通按钮；没有脚本时通常不产生操作 |
| `reset` | 恢复表单初始值，容易误清除用户修改，普通表单慎用 |

本项目只使用 `type="submit"`。

## 7. 更新 `register.html`

用下面内容替换原文件：

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>活动报名｜公司技术交流会</title>
</head>
<body>
    <h1>活动报名</h1>

    <p>
        <a href="index.html">活动首页</a>
        <a href="schedule.html">活动日程</a>
        <a href="register.html">活动报名</a>
    </p>

    <form action="success.html" method="get">
        <p>
            <label for="employee-name">姓名</label>
            <input id="employee-name" name="employeeName" type="text">
        </p>

        <button type="submit">提交报名</button>
    </form>
</body>
</html>
```

## 8. 验证

1. 从首页打开报名页。
2. 点击“姓名”文字，输入框应获得光标。
3. 输入“山田太郎”并提交。
4. 浏览器打开 `success.html`。
5. 地址栏中应能找到 `employeeName` 和输入的姓名。

然后删除 `input` 的 `name`，再次提交。输入框仍然显示，但地址中不再包含 `employeeName`。验证后恢复 `name`。

## 9. 常见失败

| 现象 | 原因 | 修正方式 |
| --- | --- | --- |
| 点击标签不能聚焦输入框 | `for` 与 `id` 不一致 | 让两个值完全相同 |
| 提交后没有某项数据 | 控件缺少 `name` | 为需要提交的控件添加 `name` |
| 点击普通按钮提交了表单 | 没有明确设置按钮类型 | 按职责填写 `type` |
| 提交后找不到完成页 | `action` 路径错误 | 对照文件目录修正 |

## 10. 改修练习

在姓名输入框后增加“员工编号”输入框：

- 标签文字为“员工编号”
- `id` 使用 `employee-id`
- `name` 使用 `employeeId`
- `type` 使用 `text`

提交后确认地址栏同时出现 `employeeName` 和 `employeeId`。

