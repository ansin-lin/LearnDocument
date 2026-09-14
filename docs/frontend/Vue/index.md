# Vue 3 + TypeScript 新人教程

本课程面向已经完成HTML、CSS、JavaScript、ES6、DOM/Event、Promise、Axios、HTTP和TypeScript基础的学员。主线从第1章开始统一使用Vue 3、Composition API、`<script setup lang="ts">`和Vite，不重复系统教授前置课程。

## 1. 最终成果

学员将逐步完成WorkHub任务管理前端，并在综合项目中连接Router、Axios API层、Pinia、表单校验和自动化测试。最终应能阅读、修改和实现普通Vue 3 + TypeScript前后端分离项目，并留下可供Review的自测与交付证据。

## 2. 课程路线

### Vue基础

1. [认识Vue与创建项目](01_intro_project_setup.md)
2. [单文件组件与模板语法](02_sfc_template_syntax.md)
3. [响应式状态：ref与reactive](03_reactivity_ref_reactive.md)
4. [事件绑定](04_events.md)
5. [属性、样式与条件渲染](05_attribute_condition.md)
6. [列表渲染与CRUD](06_list_crud.md)
7. [computed与派生状态](07_computed.md)
8. [表单绑定与校验](08_forms_validation.md)
9. [watch与副作用](09_watch_effects.md)

### 组件开发

10. [组件基础：拆分和组合页面](10_component_basics.md)
11. [Props与Emits](11_props_emits.md)
12. [组件v-model与Attributes](12_component_vmodel_attrs.md)
13. [Slots与provide/inject](13_slots_provide_inject.md)
14. [生命周期与模板引用](14_lifecycle_template_refs.md)
15. [Composables](15_composables.md)

### 单页应用工程

16. [Vue Router路由](16_vue_router.md)
17. [Vue项目中的API层与异步状态管理](17_http_api.md)
18. [Pinia状态管理](18_pinia.md)
19. [Vue单元测试与组件测试](19_unit_testing.md)
20. [构建、质量与项目交付](20_build_quality_delivery.md)

### 综合实践

21. [WorkHub综合项目](21_vue_project.md)
22. [SES改修与影响范围调查](22_ses_change_practice.md)

[附录：阅读既有Vue项目中的Options API](appendix_options_api.md)

## 3. 学习范围

- 【必须掌握】模板、响应式状态、事件、条件与列表、表单、computed、watch基础、组件职责、Props/Emits、组件v-model、Attributes、基础插槽、生命周期清理、Router、Axios API层、Pinia、测试和交付。
- 【会使用、能看懂】`reactive`、作用域插槽、provide/inject、Composable中的生命周期和异步状态、嵌套路由及组件内路由守卫。
- 【会读即可】`useSlots()`、Symbol注入键、完整生命周期顺序、复杂导航守卫和Options API。

Render Function、JSX、复杂动画、响应式底层实现等低频高级内容不进入第一阶段主线。Options API只用于帮助阅读既有项目，不作为从零开发方式。

## 4. 贯穿项目约定

WorkHub主线统一使用`src/types/task.ts`中的`Task`、`TaskStatus`和`Priority`。组件、Composable、Router、API模块与Pinia各自承担明确职责，不在组件中散落请求配置，也不把所有状态都放进Store。

每章都应形成可观察结果，并保留实际项目需要的空数据、加载、失败、调查、Review和回归测试意识。

## 5. 官方参考

- [Vue 3指南](https://cn.vuejs.org/guide/introduction.html)
- [Vue Router指南](https://router.vuejs.org/zh/guide/)
- [Pinia指南](https://pinia.vuejs.org/zh/)

课程依据官方知识范围重新组织，示例与练习为本项目的原创教学内容。
