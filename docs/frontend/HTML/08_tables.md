# 表格标签

## 本章目标

完成本章后，你可以：

- 使用表格展示行列数据
- 编写 `caption`、`thead`、`tbody`、`tr`、`th`、`td`
- 使用 `scope` 说明表头与数据的关系
- 在必要时使用 `colspan` 和 `rowspan`
- 完成活动日程表

## 1. 表格适合什么内容

活动日程包含时间、主题和讲师，适合使用表格：

| 时间 | 主题 | 讲师 |
| --- | --- | --- |
| 14:00 | 项目开发流程 | 田中 |
| 15:00 | 代码审查经验 | 李 |

表格只用于有明确行列关系的数据，不能用于排列整个页面或对齐表单。

## 2. 最小表格

```html
<table>
    <tr>
        <th>时间</th>
        <th>主题</th>
        <th>讲师</th>
    </tr>
    <tr>
        <td>14:00</td>
        <td>项目开发流程</td>
        <td>田中</td>
    </tr>
</table>
```

- `table`：整个表格
- `tr`：一行
- `th`：表头单元格
- `td`：数据单元格

每一行应具有与表头相对应的单元格数量。

## 3. 添加表格标题 `caption`

`caption` 是表格的可见标题，写在 `table` 内最前面：

```html
<table>
    <caption>2026年9月18日活动日程</caption>
    <tr>
        <th>时间</th>
        <th>主题</th>
        <th>讲师</th>
    </tr>
</table>
```

一个表格最多有一个 `caption`。标题应直接说明表格展示什么数据。

## 4. 区分表头和表体

完整结构：

```html
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
            <td>15:00</td>
            <td>代码审查经验</td>
            <td>李</td>
        </tr>
    </tbody>
</table>
```

- `thead`：表头区域
- `tbody`：主要数据区域
- `tfoot`：合计等表尾区域，当前项目暂不需要

## 5. 表头的 `scope` 属性

`scope` 说明 `th` 是哪一组数据的表头。

| 属性 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- |
| `scope` | `col`、`row`；复杂表格还有 `colgroup`、`rowgroup` | 简单表格建议填写 | 说明表头对应列还是对应行 |

列标题：

```html
<th scope="col">时间</th>
```

行标题：

```html
<tr>
    <th scope="row">第一会场</th>
    <td>项目开发流程</td>
</tr>
```

本项目第一行是列标题，所以使用 `scope="col"`。

## 6. 合并单元格

### 6.1 `colspan`：横向跨列

```html
<tr>
    <td>16:00</td>
    <td colspan="2">全体提问与交流</td>
</tr>
```

“全体提问与交流”占用“主题”和“讲师”两列。

### 6.2 `rowspan`：纵向跨行

```html
<tr>
    <th scope="row" rowspan="2">第一会场</th>
    <td>14:00</td>
    <td>项目开发流程</td>
</tr>
<tr>
    <td>15:00</td>
    <td>代码审查经验</td>
</tr>
```

“第一会场”占用两行。

### 属性说明

| 属性 | 适用标签 | 可接受的值 | 是否必填/默认值 | 作用 |
| --- | --- | --- | --- | --- |
| `colspan` | `th`、`td` | 大于 0 的整数 | 选填；默认跨 1 列 | 横向合并单元格 |
| `rowspan` | `th`、`td` | 非负整数；初学阶段使用大于 0 的整数 | 选填；默认跨 1 行 | 纵向合并单元格 |

合并后必须删除被占用位置上多余的单元格。初学阶段优先使用不合并的简单表格。

## 7. 过时的表格属性

旧资料中常见：

```html
<table border="1" cellpadding="5" cellspacing="0" align="center">
```

这些属性主要控制外观，不应写入现代 HTML：

- `border`
- `cellpadding`
- `cellspacing`
- `align`
- `bgcolor`
- `width`、`height` 用于表格外观时

本课程保持浏览器默认外观。表格边框和间距属于 CSS 课程。

## 8. 完成 `schedule.html`

用下面的完整内容替换该文件：

```html
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>活动日程｜公司技术交流会</title>
</head>
<body>
    <h1>活动日程</h1>

    <p>
        <a href="index.html">活动首页</a>
        <a href="schedule.html">活动日程</a>
        <a href="register.html">活动报名</a>
    </p>

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
</body>
</html>
```

## 9. 验证

1. 从首页打开日程页。
2. 表格标题显示“2026年9月18日活动日程”。
3. 表头与三行日程正确对应。
4. 最后一行的说明横跨两列。
5. 页面之间的链接可以往返。

## 10. 排错练习

活动新增 14:30 的“开发环境准备”分享，讲师是“佐藤”。请在 14:00 和 15:00 之间添加一行。

检查新行必须有三个 `td`，并与三个列标题对应。

