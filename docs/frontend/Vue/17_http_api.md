# 第 17 章 Vue项目中的API层与异步状态管理

你已经学习过HTTP、Axios、Promise和`async/await`。本章不重复请求语法，而是解决Vue业务项目中的组织问题：请求应该放在哪里，页面怎样表示加载、空数据、成功和失败，以及多个请求怎样共用URL、超时和错误处理。

## 本章目标与前置知识

- 【必须掌握】建立`http.ts`和业务API模块，避免组件中散落Axios配置。
- 【必须掌握】使用环境变量区分开发与生产API地址。
- 【必须掌握】在Vue页面处理加载、空数据、成功、失败、重试和重复提交。
- 【必须掌握】通过运行时检查确认外部Task数据。
- 【会使用、能看懂】使用拦截器、AbortController和Vite开发代理。
- 【会读即可】理解请求竞态以及生产环境CORS和反向代理边界。

需要掌握第14章生命周期、第16章Router，以及Axios和HTTP基础。本章使用TypeScript。

## 1. 组件为什么不应直接到处调用Axios

如果每个组件分别书写服务器地址、超时、认证头和错误转换，会出现配置不一致，也难以统一修改。

```text
Component / Pinia Action
        ↓ 调用业务函数
src/api/tasks.ts
        ↓ 使用共用实例
src/api/http.ts
        ↓ HTTP
Backend
```

- 组件负责显示和用户操作；
- Pinia负责跨页面共享业务状态；
- `tasks.ts`表达任务API契约；
- `http.ts`保存整个项目共用的Axios设置；
- 后端最终执行权限和业务校验。

## 2. 安装Axios并建立目录

在现有项目根目录执行：

```bash
npm install axios
```

```text
src/
├─ api/
│  ├─ http.ts
│  ├─ taskValidator.ts
│  └─ tasks.ts
└─ types/
   └─ task.ts
```

## 3. 使用环境变量保存API地址

项目根目录可以按环境准备：

```text
.env                 所有模式共用
.env.development     npm run dev时使用
.env.production      npm run build时使用
```

`.env.development`：

```text
VITE_API_BASE_URL=/api
```

`.env.production`示例：

```text
VITE_API_BASE_URL=https://example.invalid/api
```

`import.meta.env.VITE_API_BASE_URL`读取当前模式的值。只有`VITE_`开头的变量会暴露给客户端代码，因此它们最终可以被浏览器用户看到，不能保存密码、密钥或真正的秘密。

修改`.env`后重新启动开发服务器。真实项目的生产地址以部署设计和项目配置为准，不把培训示例地址直接投入使用。

## 4. 建立共用Axios实例

`src/api/http.ts`：

```ts
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10_000,
  headers: {
    Accept: 'application/json',
  },
})
```

`axios.create()`创建独立Axios实例。`baseURL`会与`/tasks`等相对接口路径组合；`timeout`超过毫秒数后使请求失败；`headers`保存项目共用请求头。

不要在每个组件重新`axios.create()`，也不要把服务器URL写死在多个文件。

## 5. 请求与响应拦截器

拦截器适合处理所有请求共同需要的技术逻辑：

```ts
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(error),
)
```

请求拦截器接收配置并必须返回配置；响应拦截器第一个函数处理成功响应，第二个函数处理失败。示例只说明添加研修Token的位置，不应在日志中输出Token。

不要在拦截器中直接控制某个页面的弹窗或局部状态。401统一退出、错误格式转换等规则必须根据项目设计实现，避免拦截器与页面重复提示同一错误。

## 6. 定义统一Task类型与运行时检查

`src/types/task.ts`：

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

API响应来自项目外部，TypeScript不能证明服务器真实返回值正确。新建`src/api/taskValidator.ts`，先按`unknown`接收，再检查结构：

```ts
import type { Priority, Task, TaskStatus } from '@/types/task'

const priorities: Priority[] = ['low', 'normal', 'high']
const statuses: TaskStatus[] = ['todo', 'doing', 'done']

export function isTask(value: unknown): value is Task {
  if (typeof value !== 'object' || value === null) return false

  const task = value as Record<string, unknown>

  return typeof task.id === 'number'
    && typeof task.title === 'string'
    && typeof task.assignee === 'string'
    && priorities.includes(task.priority as Priority)
    && statuses.includes(task.status as TaskStatus)
    && typeof task.dueDate === 'string'
}
```

这里的断言只用于让检查代码读取未知对象，真正是否属于`Priority`或`TaskStatus`仍由数组包含判断决定。不能直接写`response.data as Task[]`后跳过检查。

## 7. 建立任务API模块

`src/api/tasks.ts`：

```ts
import { http } from './http'
import { isTask } from './taskValidator'
import type { Task } from '@/types/task'

export type CreateTaskInput = Omit<Task, 'id'>

export async function getTasks(signal?: AbortSignal): Promise<Task[]> {
  const response = await http.get<unknown>('/tasks', { signal })

  if (!Array.isArray(response.data) || !response.data.every(isTask)) {
    throw new Error('任务数据格式不正确')
  }

  return response.data
}

export async function createTask(input: CreateTaskInput): Promise<Task> {
  const response = await http.post<unknown>('/tasks', input)

  if (!isTask(response.data)) {
    throw new Error('新增任务结果格式不正确')
  }

  return response.data
}
```

`http.get()`读取列表，`http.post()`发送新增数据。泛型写`unknown`表示必须检查响应；函数最终只返回已经验证的`Task`。PUT、PATCH和DELETE按接口规格增加，不在组件中自行猜测URL或请求体。

## 8. Vue页面的四种基本状态

```vue
<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { getTasks } from '@/api/tasks'
import type { Task } from '@/types/task'

type LoadStatus = 'idle' | 'loading' | 'success' | 'error'

const tasks = ref<Task[]>([])
const loadStatus = ref<LoadStatus>('idle')
const errorMessage = ref('')
let controller: AbortController | null = null

async function loadTasks(): Promise<void> {
  controller?.abort()
  controller = new AbortController()
  loadStatus.value = 'loading'
  errorMessage.value = ''

  try {
    tasks.value = await getTasks(controller.signal)
    loadStatus.value = 'success'
  } catch (error: unknown) {
    if (controller.signal.aborted) return
    loadStatus.value = 'error'
    errorMessage.value = error instanceof Error ? error.message : '读取失败'
  }
}

onMounted(loadTasks)
onUnmounted(() => controller?.abort())
</script>

<template>
  <p v-if="loadStatus === 'loading'">读取中...</p>
  <p v-else-if="loadStatus === 'error'" role="alert">
    {{ errorMessage }}
    <button type="button" @click="loadTasks">重试</button>
  </p>
  <p v-else-if="loadStatus === 'success' && tasks.length === 0">
    没有任务
  </p>
  <ul v-else-if="loadStatus === 'success'">
    <li v-for="task in tasks" :key="task.id">{{ task.title }}</li>
  </ul>
</template>
```

空数据是成功响应后数组为空，不是错误。失败时保留错误状态并提供真实重试；组件卸载或新请求开始时取消旧请求，避免旧响应覆盖新结果。

## 9. 防止重复提交并保留输入

提交函数按“校验→saving→请求→成功更新→finally恢复”执行。失败时不要清空表单：

```ts
const saving = ref(false)
const saveError = ref('')

async function submitTask(): Promise<void> {
  if (saving.value || !validateForm()) return

  saving.value = true
  saveError.value = ''

  try {
    const created = await createTask({ ...form })
    tasks.value.push(created)
    resetForm()
  } catch (error: unknown) {
    saveError.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}
```

`validateForm()`和`resetForm()`来自第8章表单实现。这里只展示请求状态增量，不是独立完整组件。

## 10. CORS与Vite开发代理

开发时常见：

```text
Vue开发服务器  http://localhost:5173
Spring Boot    http://localhost:8080
```

协议、主机或端口任一不同就是不同Origin。浏览器是否允许跨Origin请求由CORS响应头决定。

本地开发可在`vite.config.ts`配置代理：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

浏览器请求`/api/tasks`，Vite开发服务器再转发到后端。代理只服务本地开发，不是生产权限机制，也不能代替后端认证、授权和CORS设计。生产环境由Web服务器、网关或后端部署结构决定。

## 11. 超时、取消和竞态

- Axios实例的`timeout`限制等待时间；
- `AbortController.signal`允许组件卸载或新查询开始时主动取消；
- 连续搜索要取消旧请求，或只采纳最新请求结果；
- 被取消的请求通常不显示为普通系统故障；
- 自动重试必须限制次数，写入请求不能在不确认幂等性的情况下自动重试。

使用Network面板确认URL、方法、请求头、请求体、状态码、响应和耗时。

## 12. 常见错误

- 在每个组件直接创建Axios实例。
- 把`VITE_`环境变量当作秘密。
- 用类型断言跳过外部数据检查。
- 把错误转换成空数组，导致页面误显示“没有数据”。
- 保存失败后清空表单或永久保持saving。
- 在拦截器和页面重复显示同一错误。
- 本地Proxy可以工作，就误认为生产部署不需要配置。

## 13. WorkHub练习与检查点

1. 创建`http.ts`、`tasks.ts`和环境变量文件。
2. 使用Spring Boot研修API或团队提供的Mock API验证列表成功、空数组、500和错误结构。
3. 完成新增成功、失败保留输入和防重复提交。
4. 快速连续检索并确认旧请求被取消。
5. 分别通过直接CORS配置和Vite Proxy理解请求路径。
6. 使用Network记录一条成功和一条失败请求证据。

- [ ] 能说明Component、Store、业务API模块和Axios实例的职责。
- [ ] 能配置环境变量、超时和共用请求头。
- [ ] 能解释CORS和开发Proxy解决的问题。
- [ ] 能显示加载、空数据、成功、失败与重试。
- [ ] 能取消旧请求并防止重复提交。
- [ ] 能说明TypeScript不能代替运行时数据校验。

