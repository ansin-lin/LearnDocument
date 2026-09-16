# 附录：旧项目与进阶能力导读

本附录用于阅读既有项目和识别后续专题，不作为新人主线前置要求。

## Class Component

旧项目可能使用 `class extends React.Component`、`this.props`、`this.state`、`setState` 和生命周期方法。阅读时把它们映射到“输入、状态、渲染、外部同步与清理”，但不要机械地把每个生命周期替换成一个 Effect。迁移应有回归测试并按组件逐步进行。

## Error Boundary

捕获子组件树渲染错误并显示备用 UI。传统实现使用类组件；现代 Router/框架可能提供自己的边界。它不替代请求错误处理。

## Lazy、Suspense

`lazy` 可按需加载组件，`Suspense` 显示等待边界。适合大型路由或支持 Suspense 的数据/资源；不要把任意 Effect 请求都假设为自动支持 Suspense。

## Portal

把 Dialog 等内容渲染到 DOM 的另一位置，但仍属于原 React 组件树，事件与 Context 关系按 React 树理解。使用时必须处理焦点和背景交互。

## useReducer

当一个组件有多种相关状态转换时，将“发生了什么”表达为 Action，把更新规则集中到纯 Reducer。它不自动成为全局状态库。

## Server State Library

本课程先手写 `useEffect + loading/error/success + AbortController`，目的是让你能解释请求何时发生、旧请求如何取消、结果如何进入 UI。实际项目可能使用 TanStack Query 等服务器状态库，集中解决：

- cache：复用已经取得的数据；
- stale：判断缓存是否需要重新确认；
- invalidate：写入后标记相关查询过期；
- retry/refetch：按规则重试或重新获取；
- background refresh：保留旧画面的同时后台更新；
- deduplication：多个使用方请求同一资源时复用进行中的工作。

它们管理的是“服务器数据在客户端的缓存与同步”，不等于把所有 API 数据变成普通 Global State。进入既有项目时，先找 Query Client、query key、query function、mutation 与 invalidate 规则，再判断页面为什么读取或重新请求。不同版本的 API 和默认值会变化，以 [TanStack Query 官方文档](https://tanstack.com/query/latest/docs/framework/react/overview) 与项目锁文件为准。本课程不要求在主线中重新实现一套 Query 缓存。

## 高级 Context 与 Router

包括拆分 Provider、稳定 value、嵌套路由、布局路由、数据加载/错误边界等。具体 API 变化较快，应以项目安装版本的官方文档为准。

## 高级 TypeScript

可进一步学习判别联合、泛型组件、类型守卫和 schema 推导。避免为了“类型高级”制造比业务更难读的抽象。

## React 新版本能力

React Compiler、Actions、Server Components 与框架集成会持续变化。先确认当前项目是否启用、运行在客户端 SPA 还是全栈框架，再阅读 [React 官方博客](https://react.dev/blog) 和对应框架文档；不要把实验或框架专有能力直接写进普通 Vite SPA 主线。

## 阅读任务

选择一个旧组件，记录它的 Props、State、事件、生命周期副作用、请求、清理和测试；提出最小迁移方案与风险，但没有授权时不进行全量重写。
