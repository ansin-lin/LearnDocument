# React + TypeScript 新人教程

本课程面向已完成 JavaScript、TypeScript、HTML 与 CSS 基础的新人。目标不是记住所有 Hook，而是能独立开发普通企业 Web 系统，并能进入既有 React 项目定位页面、数据、事件、请求和缺陷。

贯穿案例统一为 **Employee Management System**：从静态员工列表开始，逐步完成登录、查询、分页、详情、新增、编辑、删除、错误处理、权限和测试。

## 学完能完成什么

- 解释 `Component + Props + State` 如何生成 UI，以及为什么会重新渲染。
- 判断数据应放在局部组件、共同父组件、Context、全局 Store 还是服务器状态中。
- 使用 Router、Axios、表单和自定义 Hook 完成员工 CRUD。
- 正确处理加载、空数据、失败、取消、重复提交、认证与授权。
- 用 Vitest 和 React Testing Library 验证用户可观察行为。
- 按 `package.json → main.tsx → Router → Page → Component → Hook → Service → API` 调查既有项目。

## 前置检查

开始前应会使用 ES Module、对象/数组展开、`map`/`filter`、类型别名、联合类型、泛型、`async/await`、`try...catch`、HTTP/JSON 和 npm。需要复习时，回到 [JavaScript](../JavaScript/index.md)、[TypeScript](../TypeScript/index.md) 和 [Node.js 与 npm](../NodeJS/index.md)。

## 封版技术基线

本课程示例按下列版本封版（2026-09-16 通过 npm registry 核验）。仓库当前没有可继承的课程项目 `package.json`/锁文件，因此这里是**新建练习项目的固定基线**，不是对某个既有项目依赖的推测。

| 技术 | 封版版本 |
| --- | --- |
| Node.js | `20.19+` 或 `22.12+` |
| create-vite / Vite / React 插件 | `9.2.1` / `8.3.0` / `@vitejs/plugin-react 6.1.1` |
| React / React DOM | `19.3.0` / `19.3.0` |
| TypeScript | `7.0.2` |
| React Router DOM | `7.18.4` |
| Axios | `1.20.0` |
| Zustand | `5.0.15` |
| Redux Toolkit / React Redux | `2.12.0` / `9.3.0` |
| Vitest / jsdom | `5.0.1` / `30.0.1` |
| Testing Library | React `16.3.3`、DOM `10.4.2`、user-event `14.6.7`、jest-dom `7.0.1` |

首次创建后提交 `package.json` 与 `package-lock.json`。课程中的新增依赖命令使用精确版本；同一项目后续安装使用 `npm ci`，不得把 `@latest` 或另一台机器解析出的版本混入封版结果。Vite 8 的 Node.js 下限见 [Vite 8 发布说明](https://vite.dev/blog/announcing-vite8)。进入既有项目时，始终以该项目的清单、锁文件、运行时约束和官方迁移说明为准，不为套用本课程版本而擅自升级。

## 课程路线

### 阶段一：从数据到画面

1. [React 的定位与创建项目](01_intro_setup.md)
2. [JSX 与组件](02_jsx_components.md)
3. [Props 与组件拆分](03_props_components.md)
4. [条件渲染与列表](04_conditional_lists.md)
5. [事件与 useState](05_events_state.md)
6. [对象、数组状态与重新渲染](06_state_render.md)
7. [组件通信与状态提升](07_component_communication.md)
8. [业务表单与校验](08_forms_validation.md)

阶段成果：在不请求后端的情况下，完成可新增、编辑、删除和筛选的员工页面。

### 阶段二：外部系统、请求与页面

9. [useEffect 与外部系统同步](09_effects.md)
10. [REST API 与异步 UI](10_api_async_ui.md)
11. [员工 CRUD 数据流](11_crud.md)
12. [React Router 与页面导航](12_router_navigation.md)
13. [useRef 与自定义 Hook](13_refs_custom_hooks.md)
14. [Context、状态分类与 useReducer](14_context_state_types.md)
15. [认证路由与权限边界](15_auth_routes.md)

阶段成果：完成多页面员工 CRUD，并能解释 URL、请求、状态与画面之间的关系。

### 阶段三：状态与工程结构

16. [Zustand 与 Redux Toolkit](16_state_libraries.md)
17. [项目目录、API Layer 与类型](17_project_structure.md)
18. [错误处理与重复提交](18_error_handling.md)
19. [性能分析与优化](19_performance.md)
20. [常见业务 UI 功能](20_common_features.md)
21. [文件、环境变量与安全](21_files_env_security.md)
22. [React 测试](22_testing.md)
23. [调试与项目调查](23_debug_investigation.md)

阶段成果：能维护具备清晰目录、统一错误处理、全局状态与自动测试的业务项目。

### 阶段四：完整项目与现场实践

24. [实战项目规格与起始状态](24_project_spec.md)
25. [登录与员工列表](25_project_login_list.md)
26. [详情、新增、编辑与删除](26_project_crud.md)
27. [验收、既有项目改修与不具合调查](27_delivery_existing_project.md)

[附录：旧项目与进阶能力导读](appendix_legacy_advanced.md)

## 贯穿数据模型与接口

```ts
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';

export type Employee = {
  id: number;
  name: string;
  email: string;
  department: string;
  role: 'ADMIN' | 'USER';
  joinedDate: string;
  status: EmployeeStatus;
};
```

```text
POST   /api/login
GET    /api/employees?keyword=&page=1&size=10&sort=name,asc
GET    /api/employees/:id
POST   /api/employees
PUT    /api/employees/:id
DELETE /api/employees/:id
```

接口返回值来自外部系统。TypeScript 类型不能替代运行时校验，前端校验也不能替代后端的业务规则和权限检查。

## 学习和提交方式

每章都应留下可检查证据：页面结果、终端中的构建/测试结果、Network 请求或调查记录。代码片段会标明所属文件；综合项目章节给出最终组合方式。完成一章后，至少能回答：数据来自哪里、保存在何处、由什么操作修改、为什么画面会变化、如何确认成功。

## 官方参考

- [React 中文文档：学习 React](https://zh-hans.react.dev/learn)
- [React API 参考](https://zh-hans.react.dev/reference/react)
- [Vite 入门](https://vite.dev/guide/)
