# TypeScript 零基础教程

本课程面向已学过 JavaScript、尚未使用 TypeScript 的学员。先用小例子理解语法，再补齐进入 React 或 Vue 项目前需要的类型能力。

## 1. 开始前需要什么

需要掌握 JavaScript 的变量、数组、对象、函数、DOM、事件、Promise 和 ES 模块。不熟悉 Node.js、npm 和 package.json 时，先完成 [Node.js 与 npm 入门](../NodeJS/index.md)。

TypeScript 不是另一套浏览器运行语言：开发时检查类型，编译后仍由 JavaScript 执行。接口返回的数据不会因为写了类型就自动变安全。

## 2. 学完能完成什么

学完后能够为变量、函数、对象、组件数据和异步结果编写类型，能够阅读项目配置，并能在使用外部数据前完成必要校验。

本课程不重复制作原生 DOM 页面，也不提前讲 React、Vue 的组件 API。完成本课程后，应直接进入所选框架课程，在组件中继续学习 Props、状态、事件和请求层。

## 3. 课程路线

### 3.1 基础语法：先知道怎么写、表示什么

1. [认识 TypeScript 与运行代码](01_intro_setup_compile.md)
2. [类型标注与基本类型](02_annotations_basic_types.md)
3. [数组与元组](03_arrays_tuples.md)
4. [对象类型与类型别名](04_object_types_aliases.md)
5. [联合类型与字面量类型](05_unions_literals.md)
6. [类型收窄与类型断言](06_narrowing_assertions.md)
7. [函数类型](07_function_types.md)
8. [接口与类型组合](08_interfaces_composition.md)
9. [类的类型语法（会阅读）](09_classes.md)

阶段结果：能独立给变量、集合、对象和函数写类型，能解释报错并修正。不要求先设计业务系统。

### 3.2 常用类型能力：理解通用类型写法

10. [泛型](10_generics.md)
11. [常量与固定值的表达](11_constants_enum.md)

阶段结果：能使用泛型表达类型关系，区别固定值在类型层和运行时的作用。

### 3.3 进入框架前的准备

12. [模块与项目配置](12_modules_config_declarations.md)
13. [组件数据与界面状态建模](13_business_state_modeling.md)
14. [异步结果与外部数据](14_async_external_data.md)

阶段结果：能看懂框架项目的模块与 TypeScript 配置，能为组件数据、界面状态和异步结果设计类型。

[附录：高级类型与低频机制](appendix_advanced_types.md)用于选学，不是项目验收前置条件。

## 4. 示例怎么使用

第 1～11 章的普通 TypeScript 代码块是独立实验，使用第 1 章的方法逐个替换 example.ts 后编译运行，不要把同名声明全部拼在一起。注释中的错误写法用于对比，需要观察报错时再单独取消注释。

第 12 章的多文件代码按标注分别保存；第 13～14 章仍是独立实验。完成后转入 React 或 Vue 项目，不需要把这些示例拼成原生 DOM 页面。

## 5. 掌握要求

- 必须掌握：标注与推断、基本类型、数组与对象、联合与收窄、函数、接口、泛型基础、模块、状态建模和外部数据校验。
- 会使用并能阅读：元组、类与访问控制、枚举和严格检查配置。
- 按需选学：自定义映射与条件类型、infer、模板字面量类型、namespace、Mixin、装饰器。

每章练习应实际修改代码、检查报错或运行结果。框架组件和页面成果由后续 React/Vue 课程验收。
