# 第 21 章 Vue项目中的TypeScript写法与项目质量

前20章用JavaScript建立Vue开发与测试能力。本章把已经能运行的项目渐进加强为Vue + TypeScript项目，并完成质量检查与交付。

## 本章目标

- 【必须掌握】理解TypeScript在Vue项目中解决的问题，并能渐进迁移。
- 【必须掌握】为业务数据、响应式状态、组件契约、Composable、API、Pinia和Router补充类型。
- 【必须掌握】区分静态类型检查与运行时检查。
- 【必须掌握】完成Lint、类型检查、测试、构建、预览和Git差异确认。

## 1. 为什么此时导入TypeScript

JavaScript版本可以运行，但字段拼错、事件参数顺序错误和空值遗漏可能到运行时才暴露。TypeScript在开发阶段检查这些约定，并提供可靠的编辑器补全。

```js
const task = { id: 1, title: '规格确认' }
task.titel
```

把`task`声明为`Task`后，编辑器能指出`titel`不是合法字段。不过，TypeScript不会验证服务器真实返回的数据，也不能代替测试。

## 2. 为现有JavaScript项目接入TypeScript

### 2.1 安装并准备配置

先在包含`package.json`的项目目录安装开发依赖：

```bash
npm install -D typescript vue-tsc @vue/tsconfig
```

`typescript`提供类型系统，`vue-tsc`负责同时检查TypeScript文件和Vue单文件组件，`@vue/tsconfig`提供适合Vue项目的基础配置。

新建`tsconfig.json`：

```json
{
  "extends": "@vue/tsconfig/tsconfig.dom.json",
  "include": [
    "env.d.ts",
    "src/**/*.ts",
    "src/**/*.vue"
  ],
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

新建`env.d.ts`：

```ts
/// <reference types="vite/client" />
```

如果项目没有使用`@` alias，应删除对应的`paths`，继续使用相对路径。已有团队项目应优先沿用仓库现有配置，不要用本示例覆盖它。

在`package.json`已有的`scripts`对象中追加：

```json
{
  "scripts": {
    "type-check": "vue-tsc --noEmit"
  }
}
```

这是追加一个script，不要覆盖已有的`dev`、`lint`、`test:unit`和`build`等命令。先执行一次：

```bash
npm run type-check
```

确认命令能够启动后，再开始迁移源码。当前还没有TypeScript源码时，检查内容较少是正常现象。

### 2.2 按模块渐进迁移

在独立分支或可恢复的Git状态中操作：

1. 建立`src/types/task.ts`。
2. 把`api/tasks.js`迁移为`api/tasks.ts`，同步增加API输入、返回值和外部数据类型。
3. 把`stores/tasks.js`迁移为`stores/tasks.ts`。
4. 把需要类型约束的`composables/*.js`逐个迁移为`.ts`。
5. 逐个把Vue组件的`<script setup>`改为`<script setup lang="ts">`。
6. 根据项目需要迁移`main.js`、`router/index.js`等剩余普通模块。
7. 每完成一个阶段，都执行`npm run type-check`、`npm run test:unit`和`npm run build`。

模块迁移时，文件扩展名和文件内部的类型写法必须同时改变。不要先在`.js`中加入`Task[]`或`Promise<Task[]>`，也不要一次性重命名全部文件。

## 3. 统一业务类型

`src/types/task.ts`：

```ts
export type Priority = 'low' | 'normal' | 'high'
export type TaskStatus = 'todo' | 'doing' | 'done'

export interface Task {
  id: number
  title: string
  assignee: string
  priority: Priority
  status: TaskStatus
  dueDate: string
}

export interface CreateTaskInput {
  title: string
  priority: Priority
}
```

页面、API、Store和测试都导入同一类型。`import type { Task }`表示只导入类型，不生成运行时代码。

## 4. ref、reactive与空值

先对照前20章的JavaScript写法：

```js
const loading = ref(false)
const tasks = ref([])
const selectedTask = ref(null)
```

迁移为TypeScript后，初始值明确的`loading`仍可推断；空数组和尚未取得的对象需要显式类型：

```ts
const loading = ref(false)
const tasks = ref<Task[]>([])
const selectedTask = ref<Task | null>(null)

interface TaskForm {
  title: string
  priority: Priority
}

const form: TaskForm = reactive({
  title: '',
  priority: 'normal',
})
```

`Task | null`说明挂载前或读取前确实可能没有对象，使用时必须先判断。

## 5. Props、Emits与组件v-model

JavaScript阶段使用运行时声明：

```js
defineProps({
  task: {
    type: Object,
    required: true,
  },
})

defineEmits(['changeStatus'])
```

迁移为TypeScript后使用同一业务契约：

```vue
<script setup lang="ts">
import type { Task, TaskStatus } from '@/types/task'

defineProps<{
  task: Task
  readonly?: boolean
}>()

const emit = defineEmits<{
  changeStatus: [id: number, status: TaskStatus]
}>()

const keyword = defineModel<string>('keyword', { required: true })
</script>
```

可选Prop写`?`；事件元组按顺序描述参数；`defineModel<string>()`规定父子双方传递字符串。

## 6. DOM事件与Template Ref

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'

const inputRef = ref<HTMLInputElement | null>(null)

function handleInput(event: Event) {
  const input = event.currentTarget as HTMLInputElement
  console.log(input.value)
}

onMounted(() => inputRef.value?.focus())
</script>

<template>
  <input ref="inputRef" @input="handleInput">
</template>
```

事件先标注为`Event`，确定绑定对象是input后再缩小为`HTMLInputElement`。Template Ref在挂载前为`null`，所以类型和访问方式都要处理空值。

## 7. Composable

```ts
import { computed, type Ref } from 'vue'
import type { Task } from '@/types/task'

export function useTaskFilter(tasks: Ref<Task[]>, keyword: Ref<string>) {
  const filteredTasks = computed(() =>
    tasks.value.filter((task) => task.title.includes(keyword.value)),
  )
  return { filteredTasks }
}
```

参数类型规定调用方要传入哪种响应式数据，返回值通常可以自动推断。

## 8. API：先unknown，再运行时检查

```ts
export async function getTasks(signal?: AbortSignal): Promise<Task[]> {
  const response = await http.get<unknown>('/tasks', { signal })

  if (!Array.isArray(response.data) || !response.data.every(isTask)) {
    throw new Error('任务数据格式不正确')
  }
  return response.data
}
```

`unknown`表示尚未确认，必须检查后才能使用。第17章的`isTask()`仍要保留，因为服务器响应、localStorage和URL不会被TypeScript自动验证。不要写`response.data as Task[]`跳过检查。

## 9. Pinia与Router边界

```ts
const tasks = ref<Task[]>([])

function changeTaskStatus(id: number, status: TaskStatus): boolean {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return false
  task.status = status
  return true
}

const taskId = computed<number | null>(() => {
  const value = Number(route.params.id)
  return Number.isInteger(value) && value > 0 ? value : null
})
```

Store参数表达业务契约；Router参数来自URL，仍要转换和验证。

## 10. 常见类型写法的边界

| 写法 | 含义 | 使用原则 |
| --- | --- | --- |
| 类型推断 | 由初始值推断 | 信息充分时优先 |
| `unknown` | 当前类型未知 | 外部数据边界使用并检查 |
| `any` | 关闭检查 | 不用来临时消除报错 |
| `as` | 要求编译器按某类型看待 | 程序已有证明时谨慎使用 |
| `!` | 声称值不是空 | 不会消除运行时空值 |

先补判断或调整数据设计，再考虑断言。

## 11. type-check与build不同

`vue-tsc`检查TypeScript、组件脚本和模板之间的类型关系；Vite Build负责生成部署资源。两者都要执行。

```bash
npm run type-check
npm run build
```

脚本名称以`package.json`为准。Build成功不等于类型检查成功。

## 12. 完整交付流程

```text
开发完成 → 自测 → Lint → Type Check → Unit Test
→ Build → Preview → Review → 修正 → 回归测试
```

```bash
npm run lint
npm run type-check
npm run test:unit
npm run build
npm run preview
git status
git diff
```

确认只包含任务范围内文件；移除临时日志、测试账号和临时URL；不提交密钥、令牌、`node_modules`或包含秘密的环境文件。README至少说明环境要求、安装启动、主要功能、API设置、检查命令和已知限制。

## 13. 调查与质量底线

- Console检查异常、Vue警告和失败导入。
- Network检查URL、方法、状态、请求体、响应和耗时。
- Vue DevTools检查组件、Props、事件和Pinia状态。
- 前端校验、隐藏按钮和Router Guard不能代替后端权限控制。
- 表单要有可关联label，异步失败要提供恢复方式。

## 14. WorkHub渐进迁移练习

1. 为JavaScript版WorkHub建立可恢复的Git状态。
2. 建立统一Task与CreateTaskInput类型。
3. 依次迁移API、Store、Composable和两个组件，每步运行检查和相关测试。
4. 保留`isTask()`并验证错误响应仍会被拒绝。
5. 制造Props事件参数错误和空值错误，根据提示修复。
6. 执行完整交付命令，检查Git差异并更新README。

## 本章检查点

- [ ] 能解释TypeScript能解决和不能解决的问题。
- [ ] 能渐进迁移，不一次性改写全部文件。
- [ ] 能标注响应式状态、组件契约、Composable、API、Pinia和Router边界。
- [ ] 能说明运行时校验为何仍然必要。
- [ ] 能完成Lint、类型检查、测试、构建、预览和差异确认。
