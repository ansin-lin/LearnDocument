# JavaScript 零基础教程

本课程面向已经完成 HTML 和 CSS 基础的学员。目标不是背完整 API，而是能用 JavaScript 控制页面、处理表单、保存浏览器数据，并为后续 TypeScript、请求发送、Vue 和 React 打好基础。

课程最后会继续使用前端递进练习项目：[JavaScript 递进练习：有給休暇申請システム](../training/03_javascript_task.md)。

## 学完后可以完成什么

完成本课程后，你应该能够：

- 在 HTML 页面中正确加载 JavaScript 文件。
- 使用变量、数组、对象和函数组织业务数据。
- 使用 DOM API 读取和更新页面内容。
- 处理按钮点击、表单提交和输入变化。
- 完成注册、登录、申请、确认、保存、筛选和取消等前端交互。
- 使用 `localStorage` 和 `sessionStorage` 保存练习数据。
- 使用浏览器开发者工具定位常见错误。
- 看懂后续 TypeScript、Vue、React 中常见的 JS 写法。

## 必须掌握

- `let`、`const`、字符串、数字、布尔值、`null`、`undefined`
- `===`、`!==`、`&&`、`||`、`!`
- `if`、循环、数组、对象
- 函数、参数、返回值、箭头函数、回调函数
- `querySelector`、`textContent`、`value`、`classList`、属性操作
- `addEventListener`、`click`、`submit`、`input`、`change`
- 表单校验、错误显示、`aria-invalid`
- `JSON.stringify`、`JSON.parse`
- `localStorage`、`sessionStorage`
- `try...catch` 和基础调试

## 需要掌握

- `find`、`filter`、`map`、`some`、`includes`
- 解构、展开、剩余参数
- 正则基础
- `Date`、编号生成、补零
- `location.href`、页面跳转、登录状态检查
- Promise、`async` / `await`
- `fetch` 基础
- Axios 基础请求
- ES 模块
- `Set`、`Map`

## 了解即可

- `var` 和变量提升
- 复杂 `this`
- 原型链
- 递归和深拷贝
- Class 继承体系
- `Symbol`、`Proxy`、`Reflect`
- Cookie 细节
- Babel、Webpack

## 课程目录

1. [认识 JavaScript 与运行方式](01_js_intro_runtime.md)
2. [变量、值与数据类型](02_values_variables_types.md)
3. [运算符、条件判断与真值判断](03_operators_conditions.md)
4. [循环与数组基础](04_loops_arrays.md)
5. [数组基本方法](05_array_methods.md)
6. [函数、作用域、闭包与递归](06_functions_scope_callbacks.md)
7. [数组回调方法](07_array_callback_methods.md)
8. [字符串常用方法](08_string_methods.md)
9. [DOM 基础、元素获取与节点操作](09_dom_query_content.md)
10. [事件监听、事件传播与表单操作](10_events_forms.md)
11. [表单校验与错误显示](11_form_validation_errors.md)
12. [对象与数据结构](12_objects_data_structure.md)
13. [BOM、页面跳转与浏览器信息](13_bom_navigation_session.md)
14. [日期、编号与业务工具函数](14_date_number_utils.md)
15. [正则表达式基础](15_regex_validation.md)
16. [错误处理与调试](16_error_debugging.md)
17. [异步基础、Promise 与 async/await](17_async_promise_async_await.md)
18. [JSON 与浏览器存储](18_json_browser_storage.md)
19. [HTTP 请求、fetch 与 Axios 基础](19_fetch_http_intro.md)
20. [ES6+ 常用进阶语法](20_destructuring_spread_modern_syntax.md)
21. [Set、Map 与元编程基础](21_set_map_modern_objects.md)
22. [ES 模块与项目脚本组织](22_modules_script_organization.md)
23. [JavaScript 新人综合练习](23_js_project.md)

## 贯穿项目

课程中的示例会逐步靠近 `有給休暇申請システム`。前半部分先用小例子理解语法，后半部分会围绕用户、申请、登录状态、浏览器存储和页面交互展开。
