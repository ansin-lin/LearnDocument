# HTML 零基础教程

HTML 用来描述网页中“有什么内容”以及“这些内容分别是什么”。浏览器读取 HTML 后，会把标题、段落、链接、图片、列表、表格和表单显示成网页。

本课程不要求任何前端基础。课程只使用 HTML，不使用 CSS、JavaScript 或前端框架。页面会保持浏览器的默认外观，重点是把结构和内容写正确。

## 学完后可以完成什么

完成课程后，你可以独立制作一个“公司技术交流会报名网站”，其中包括：

- 活动介绍首页
- 活动日程页
- 报名表页面
- 报名完成提示页

你还可以检查标签嵌套、文件路径、图片替代文本、表格结构和表单属性是否正确。

## 学习方法

每章都包含一个可以在浏览器中观察的结果。建议按以下方式学习：

1. 手动输入示例，不只复制代码。
2. 保存文件后用浏览器打开。
3. 按照“验证”部分确认结果。
4. 故意制造一个常见错误，再尝试修正。
5. 完成本章练习后再进入下一章。

## 贯穿项目目录

课程中持续使用下面的目录：

```text
html-event-site/
├─ index.html
├─ schedule.html
├─ register.html
├─ success.html
└─ images/
   └─ meeting-room.jpg
```

开始学习时只创建 `html-event-site` 和 `index.html`。其他文件会在用到时逐步添加。

## 课程顺序

1. [认识 HTML 并创建第一个网页](01_html_intro.md)
2. [HTML 文档结构](02_document_structure.md)
3. [元素、属性与嵌套](03_elements_attributes.md)
4. [文本内容标签](04_text_content.md)
5. [链接与文件路径](05_links_paths.md)
6. [图片标签](06_images.md)
7. [列表标签](07_lists.md)
8. [表格标签](08_tables.md)
9. [表单基础](09_form_basics.md)
10. [常用表单控件](10_form_controls.md)
11. [HTML 表单验证属性](11_form_validation.md)
12. [语义化页面结构](12_semantic_structure.md)
13. [页面元数据与 HTML 检查](13_metadata_validation.md)
14. [HTML 新人综合练习](14_html_project.md)
15. [新人综合练习参考答案](15_newcomer_practice_answers.md)

## 学习范围

### 必须掌握

- 完整 HTML 文档结构
- 常用标签及其职责
- 常用属性、可接受的值和默认行为
- 标签的父子关系和正确嵌套
- 相对路径和页面之间的链接
- 图片、列表、表格和表单
- 原生 HTML 语义和基本可用性

### 当前只需了解

- 低频属性和过时属性的名称
- 浏览器会自动修正部分错误 HTML
- 表单最终通常需要服务器处理

浏览器能够显示错误代码，不代表代码就是正确的。课程中的验证步骤会帮助你区分“浏览器勉强显示”和“HTML 结构正确”。
