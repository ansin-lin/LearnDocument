# 第 17 章 Vue项目中的API层与异步状态管理

你已经学习过HTTP、Axios、Promise和`async/await`。本章解决Vue业务项目中的组织问题：请求放在哪里，页面怎样表示请求状态，以及怎样避免重复提交和请求竞态。

## 本章目标与前置知识

- 【必须掌握】建立`http.js`和业务API模块，避免组件中散落Axios配置。
- 【必须掌握】使用环境变量区分开发与生产API地址。
- 【必须掌握】处理加载、空数据、成功、失败、重试和重复提交。
- 【必须掌握】通过运行时检查确认外部Task数据。
- 【会使用、能看懂】拦截器、AbortController和Vite开发代理。
- 【会读即可】请求竞态以及生产环境CORS和反向代理边界。

需要掌握第14章生命周期、第16章Router，以及Axios和HTTP基础。本章继续使用普通JavaScript。

## 1. 为什么不应在组件中到处调用Axios

如果每个组件分别书写服务器地址、超时、认证头和错误转换，会出现配置不一致，也难以统一修改。

```text
Component / Pinia Action
        ↓ 调用业务函数
src/api/tasks.js
        ↓ 使用共用实例
src/api/http.js
        ↓ HTTP
Backend
```

- 组件负责显示和用户操作；
- Pinia负责跨页面共享业务状态；
- `tasks.js`表达任务API契约；
- `http.js`保存整个项目共用的Axios设置；
- 后端最终执行权限和业务校验。

## 2. 安装Axios并建立目录

```bash
npm install axios
```

```text
src/
└─ api/
   ├─ http.js
   ├─ taskValidator.js
   └─ tasks.js
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

`import.meta.env.VITE_API_BASE_URL`读取当前模式的值。只有`VITE_`开头的变量会暴露给客户端代码，因此不能保存密码、密钥或真正的秘密。修改`.env`后要重新启动开发服务器。

## 4. 建立共用Axios实例

`src/api/http.js`：

```js
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10_000,
  headers: {
    Accept: 'application/json',
  },
})
```

`axios.create()`创建独立Axios实例。`baseURL`会与`/tasks`等相对路径组合；`timeout`规定最长等待毫秒数；`headers`保存项目共用请求头。不要在每个组件重新创建实例，也不要把服务器URL写死在多个文件。

## 5. 请求与响应拦截器

拦截器适合处理所有请求共同需要的技术逻辑：

```js
http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
)
```

请求拦截器接收配置并必须返回配置；响应拦截器的两个函数分别处理成功和失败。示例只说明添加研修Token的位置，不应在日志中输出Token。不要在拦截器中直接控制某个页面的弹窗或局部状态。

## 6. 用JavaScript检查外部Task数据

前面章节一直使用`id`、`title`、`assignee`、`priority`、`status`和`dueDate`。服务器数据来自应用外部，不能因为接口文档写了这些字段就直接相信实际响应。

`src/api/taskValidator.js`：

```js
const priorities = ['low', 'normal', 'high']
const statuses = ['todo', 'doing', 'done']

export function isTask(value) {
  if (typeof value !== 'object' || value === null) return false

  return typeof value.id === 'number'
    && typeof value.title === 'string'
    && typeof value.assignee === 'string'
    && priorities.includes(value.priority)
    && statuses.includes(value.status)
    && typeof value.dueDate === 'string'
}
```

`isTask()`在程序运行时逐个检查字段和值。它与第20章的静态类型检查解决不同问题；即使以后导入TypeScript，外部数据仍需要运行时检查。

## 7. 建立任务API模块

`src/api/tasks.js`：

```js
import { http } from './http'
import { isTask } from './taskValidator'

export async function getTasks(signal) {
  const response = await http.get('/tasks', { signal })

  if (!Array.isArray(response.data) || !response.data.every(isTask)) {
    throw new Error('任务数据格式不正确')
  }
  return response.data
}

export async function createTask(input) {
  const requestBody = {
    title: input.title,
    priority: input.priority,
  }
  const response = await http.post('/tasks', requestBody)

  if (!isTask(response.data)) {
    throw new Error('新增任务结果格式不正确')
  }
  return response.data
}
```

`http.get()`读取列表，`http.post()`发送新增数据。两个函数都先检查响应，再把结果交给页面。PUT、PATCH和DELETE要按接口规格增加，不在组件中自行猜测URL或请求体。

这里与第8章保持同一请求契约：新增时前端只发送`title`和`priority`。后端生成`id`，根据登录用户或业务规则确定`assignee`，把初始`status`设为`todo`并确定`dueDate`，最后返回六个字段齐全的Task。真实项目如果规格不同，应同时修改表单、请求函数、Mock和测试。

## 8. Vue页面的四种基本状态

```vue
<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { getTasks } from '@/api/tasks'

const tasks = ref([])
const loadStatus = ref('idle')
const errorMessage = ref('')
let controller = null

async function loadTasks() {
  controller?.abort()

  const currentController = new AbortController()
  controller = currentController
  loadStatus.value = 'loading'
  errorMessage.value = ''

  try {
    const result = await getTasks(currentController.signal)
    if (controller !== currentController) return

    tasks.value = result
    loadStatus.value = 'success'
  } catch (error) {
    if (currentController.signal.aborted) return
    if (controller !== currentController) return

    loadStatus.value = 'error'
    errorMessage.value = error instanceof Error ? error.message : '读取失败'
  } finally {
    if (controller === currentController) {
      controller = null
    }
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
  <p v-else-if="loadStatus === 'success' && tasks.length === 0">没有任务</p>
  <ul v-else-if="loadStatus === 'success'">
    <li v-for="task in tasks" :key="task.id">{{ task.title }}</li>
  </ul>
</template>
```

空数据是成功响应后数组为空，不是错误。每次调用都保存自己的`currentController`，收到响应后还要确认它仍是最新请求，才能更新页面。这样即使旧请求很晚才结束，也不会覆盖新请求结果。

## 9. 防止重复提交并保留输入

提交函数按“校验→saving→请求→成功更新→finally恢复”执行。失败时不要清空表单：

```js
const saving = ref(false)
const saveError = ref('')

async function submitTask() {
  if (saving.value || !validateForm()) return

  saving.value = true
  saveError.value = ''

  try {
    const created = await createTask({
      title: form.title,
      priority: form.priority,
    })
    tasks.value.push(created)
    resetForm()
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}
```

`validateForm()`和`resetForm()`来自第8章表单实现。这里只展示请求状态增量，不是独立完整组件。

## 10. CORS与Vite开发代理

协议、主机或端口任一不同就是不同Origin。浏览器是否允许跨Origin请求由CORS响应头决定。本地开发可在`vite.config.js`配置代理：

```js
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

浏览器请求`/api/tasks`，Vite开发服务器再转发到后端。代理只服务本地开发，不能代替生产环境的认证、授权和CORS设计。

## 11. 超时、取消和竞态

- Axios实例的`timeout`限制等待时间；
- `AbortController.signal`允许组件卸载或新查询开始时主动取消；
- 连续搜索要取消旧请求，并且只采纳最新请求结果；
- 被取消的请求通常不显示为普通系统故障；
- 写入请求不能在不确认幂等性的情况下自动重试。

使用Network面板确认URL、方法、请求头、请求体、状态码、响应和耗时。

## 12. 常见错误

- 在每个组件直接创建Axios实例。
- 把`VITE_`环境变量当作秘密。
- 不检查外部数据就直接交给页面。
- 把错误转换成空数组，导致页面误显示“没有数据”。
- 保存失败后清空表单或永久保持saving。
- 在拦截器和页面重复显示同一错误。
- 本地Proxy可以工作，就误认为生产部署不需要配置。

## 13. WorkHub练习与检查点

1. 创建`http.js`、`taskValidator.js`、`tasks.js`和环境变量文件。
2. 使用研修API或团队提供的Mock API验证列表成功、空数组、500和错误结构。
3. 完成新增成功、失败保留输入和防重复提交。
4. 快速连续检索并确认旧请求被取消，旧响应不会覆盖新结果。
5. 使用Network记录一条成功和一条失败请求证据。

- [ ] 能说明Component、Store、业务API模块和Axios实例的职责。
- [ ] 能配置环境变量、超时和共用请求头。
- [ ] 能显示加载、空数据、成功、失败与重试。
- [ ] 能取消旧请求并防止重复提交。
- [ ] 能说明运行时数据检查不能被静态类型检查代替。
