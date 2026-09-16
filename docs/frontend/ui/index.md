# UI框架与组件库学习路线

本课程帮助你在已经掌握HTML、CSS和JavaScript的基础上，使用成熟的UI工具快速实现一致、响应式并且可操作的业务页面。组件库可以减少重复样式和常见交互代码，但不能代替HTML语义、CSS布局、业务校验与后端权限控制。

## 两条学习路线

| 路线 | 适合的项目 | 前置知识 | 学习结果 |
| --- | --- | --- | --- |
| Bootstrap 5.3 | 普通HTML页面、服务端模板、既有管理系统 | HTML、CSS、JavaScript | 能用栅格、工具类和交互组件完成响应式页面 |
| Vuetify 3 | Vue 3单页应用和管理系统 | Vue组件、Props、事件、插槽、表单和响应式状态 | 能用Vue组件完成表单、表格、Dialog、反馈、布局和主题 |

两条路线是并列选择，不要求先学完Bootstrap再学习Vuetify。进入实际项目后，应先确认项目已经采用的UI库及其主版本，不在同一页面随意混用两套组件体系。

## Bootstrap路线

1. [入门、引入与栅格](bootstrap/01_bootstrap_basics.md)
2. [表单与常用交互组件](bootstrap/02_bootstrap_components.md)
3. [工具类、主题与响应式业务页面](bootstrap/03_bootstrap_layout_theme.md)

## Vuetify路线

1. [Vuetify 3安装、插件注册与应用结构](vuetify/01_vuetify_app_structure.md)
2. [常用业务组件](vuetify/02_vuetify_components.md)
3. [布局、响应式与主题](vuetify/03_vuetify_layout_theme.md)

## 学习与验收方法

每一章都要实际运行示例，并至少检查：

- 桌面和手机宽度下的布局；
- 键盘能否到达并操作按钮、链接和表单；
- 长标题、空数据、加载、错误和禁用状态；
- 浏览器Console是否存在错误；
- 修改是否只影响预期组件和页面。

完成一条路线后，应能把WorkHub任务页面改造成统一的业务界面，并留下运行步骤、不同状态的验证结果和必要截图。
