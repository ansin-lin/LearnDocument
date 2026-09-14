# 第 21 章 WorkHub综合项目

前20章分别学习了Vue项目中的各项职责。本章不再逐段提供完成代码，而是给出一份可验收的业务规格，要求你独立把这些能力连接为一个Vue 3 + TypeScript项目。

## 本章目标与开始条件

- 【必须掌握】根据业务规格设计组件、页面、路由、API和Store的职责。
- 【必须掌握】实现登录、任务CRUD、搜索与筛选，以及加载、空数据和失败状态。
- 【必须掌握】编写关键测试并完成Lint、类型检查、构建和交付说明。
- 【会使用、能看懂】根据培训环境连接Mock API或Spring Boot API。

开始前应完成第1～20章。项目统一使用Vue 3、TypeScript、Vue Router、Axios、Pinia、Vitest和Vue Test Utils。

## 1. 项目成果与业务范围

你要完成WorkHub任务管理前端，至少包含：

- 登录页面；
- 任务列表、详情、新增和修改页面；
- 删除任务及删除前确认；
- 按任务名称搜索；
- 按任务状态筛选；
- URL可复现的列表查询条件；
- 登录状态与任务状态的跨页面共享；
- 正常、加载、空数据、失败和重试界面；
- 表单校验、防重复提交和失败后保留输入。

身份认证可以使用培训用Mock实现，但必须在README说明边界。前端保存登录状态和导航守卫不等于真正的服务端认证与权限控制。

## 2. 统一业务模型

`src/types/task.ts`是任务字段的唯一来源：

```ts
export type TaskStatus = 'todo' | 'doing' | 'done'

export type Priority = 'low' | 'normal' | 'high'

export interface Task {
  id: number
  title: string
  assignee: string
  priority: Priority
  status: TaskStatus
  dueDate: string
}
```

组件、Store、API响应和测试Mock都必须符合该契约。不要另建`completed: boolean`版本，也不要使用`any`掩盖字段不一致。

## 3. 目标目录与职责

```text
src/
├─ api/
│  ├─ http.ts
│  └─ tasks.ts
├─ assets/
├─ components/
│  ├─ TaskForm.vue
│  ├─ TaskItem.vue
│  ├─ TaskList.vue
│  └─ TaskSearch.vue
├─ composables/
│  └─ useTaskFilter.ts
├─ router/
│  └─ index.ts
├─ stores/
│  └─ tasks.ts
├─ types/
│  └─ task.ts
├─ views/
│  ├─ LoginView.vue
│  ├─ TaskListView.vue
│  ├─ TaskDetailView.vue
│  ├─ TaskCreateView.vue
│  ├─ TaskEditView.vue
│  └─ NotFoundView.vue
├─ App.vue
└─ main.ts
```

职责边界应能解释为：

```text
View / Component
    ↓ 调用操作、显示状态
Composable或Pinia Store
    ↓ 调用接口
API Module
    ↓ 使用共用实例
Axios
    ↓
Backend或Mock API
```

- 组件负责界面、局部输入和用户操作。
- Composable负责可复用的局部响应式逻辑。
- Router负责URL与页面关系，筛选条件放入query后应可刷新复现。
- Pinia保存确实需要跨页面共享的任务和登录状态。
- API模块负责URL、参数和响应边界，组件不能各自创建Axios实例。

## 4. 实现阶段

### 4.1 项目骨架

1. 用`create-vue`创建TypeScript项目。
2. 安装Router、Pinia、Axios、Vitest与Vue Test Utils。
3. 建立目录、统一`Task`类型、共用Axios实例和环境变量。
4. 先提交只包含项目骨架的可运行版本。

可观察结果：`npm run dev`可打开根页面，未知URL显示404页面，Console无错误。

### 4.2 列表、搜索与筛选

1. API模块读取任务。
2. Store管理任务、loading和error。
3. 列表页区分加载、失败、空数据和正常列表。
4. `TaskSearch`处理关键字与状态条件，并同步到Router query。
5. 刷新带query的URL后筛选条件仍能恢复。

不得用数组index作为任务`key`。无原始数据与筛选后无结果应显示不同说明。

### 4.3 详情与CRUD

1. 通过动态路由打开详情。
2. 把路由参数转换并验证后再查找任务。
3. 新增与修改共用`TaskForm`，但保存动作使用明确Emit或页面函数。
4. 表单至少校验标题、担当者、优先级、状态和期限。
5. 删除前显示确认，成功后导航回列表；失败时显示可恢复错误。

提交中使用`submitting`禁用按钮，防止重复请求。失败后不能清空用户输入。

### 4.4 登录与导航边界

1. 未登录访问业务页时转到登录页，并保留原目标地址。
2. 登录成功后返回原目标或任务列表。
3. 导航守卫只改善前端导航体验；README必须说明真实权限仍由后端检查。

### 4.5 测试与交付

至少覆盖：

- `TaskItem`根据Props显示统一Task字段并发出操作事件；
- `TaskForm`的必填校验、有效提交和防重复提交；
- `useTaskFilter`或等价筛选逻辑；
- Pinia异步Action的成功与失败；
- API Mock返回的数据符合`Task`契约；
- 一个关键页面的loading、empty和error分支。

测试公开行为，不依赖组件内部ref名称或私有函数名称。

## 5. API与环境约定

开发地址从`VITE_API_BASE_URL`读取。`VITE_`变量会进入浏览器，不得存放密码或令牌。若前端和后端端口不同，可按第17章配置Vite开发代理；代理不是生产权限或安全机制。

使用Mock API时，接口形状仍应尽量接近实际契约：

| 操作 | 示例方法与路径 |
| --- | --- |
| 任务一览 | `GET /tasks` |
| 任务详情 | `GET /tasks/:id` |
| 新增任务 | `POST /tasks` |
| 修改任务 | `PUT /tasks/:id` |
| 删除任务 | `DELETE /tasks/:id` |

实际端点、状态码和错误格式以培训提供的API规格为准，不得只根据表格猜测后端实现。

## 6. 故障调查任务

在完成正常流程后，主动制造并调查以下问题：

1. 把API地址写错，使用Network确认实际URL和状态。
2. 让Mock响应缺少`dueDate`，确认运行时校验能够发现外部数据异常。
3. 快速切换筛选条件，确认旧请求不会覆盖新结果。
4. 制造一个Props类型错误，使用`npm run type-check`定位。
5. 让一个测试失败，记录预期、实际结果和修正内容。

## 7. 最终验收

### 7.1 功能验收

- [ ] 登录和退出流程可操作。
- [ ] 任务列表、详情、新增、修改、删除均可运行。
- [ ] 搜索和状态筛选能够组合，并可由URL恢复。
- [ ] 刷新详情页不会只依赖上一页内存状态。
- [ ] loading、empty、error、retry和防重复提交均可观察。
- [ ] 所有任务对象、API Mock和测试数据符合统一`Task`类型。

### 7.2 质量验收

```bash
npm run lint
npm run type-check
npm run test:unit
npm run build
npm run preview
git status
git diff
```

所有命令必须成功，预览环境应验证首页、直接打开详情URL、刷新和404。Console不得留下未处理错误。

### 7.3 提交物

- 源码与锁文件；
- README（环境、安装、命令、目录、API、测试和限制）；
- 功能自测表和测试结果；
- 关键页面与异常状态证据；
- 改修内容、影响范围和已知限制说明。

## 8. 完成标准

你不仅要让页面“看起来能用”，还要能向Reviewer解释每一层的职责、关键状态放置位置、外部数据验证边界、测试范围和Git差异。若另一名学员能够仅根据README安装、运行、测试并复现主要功能，才算完成交付。
