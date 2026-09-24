# 第 17 章 Vue项目中的API层与异步处理

Vue页面经常需要从服务器读取数据或保存用户输入。本章继续使用WorkHub，完成“确认API规格→建立Axios共用实例→建立业务API模块→页面调用→显示各种请求状态→使用Network调查”的完整流程。

## 本章目标与前置知识

### 新人必须掌握

- 使用`axios.create()`建立项目共用HTTP实例；
- 用业务API模块封装GET和POST请求；
- 使用`try / catch / finally`处理异步请求；
- 区分`loading`、`success`、`empty`和`error`；
- 防止重复提交，并在失败时保留用户输入；
- 使用浏览器Network面板调查API问题。

### 真实项目常用

- 使用Vite环境变量管理API地址；
- 理解拦截器、Vite开发代理和CORS的职责边界。

### 进阶理解

- 运行时检查外部数据；
- 使用`AbortController`取消请求；
- 避免旧响应覆盖新结果；
- 理解幂等性与自动重试的关系。

需要掌握Promise、`async / await`、HTTP基础、第14章生命周期和第16章Router。本章继续使用JavaScript。

## 1. 先确认API规格

写请求代码前，先确认URL、方法、输入、输出和错误响应。WorkHub使用以下契约：

| 功能 | 方法与URL | 输入 | 成功结果 |
| --- | --- | --- | --- |
| 读取任务 | `GET /tasks` | 无 | Task数组 |
| 新增任务 | `POST /tasks` | `title`、`priority` | 新增后的完整Task |

新增时，前端只发送标题和优先级。`id`由后端生成，`assignee`由当前用户或业务规则决定，`status`初始为`todo`，`dueDate`由后端确定。不能只看页面字段就猜测请求体。

## 2. 为什么需要API层

如果每个组件都直接写服务器地址、超时和错误转换，同一项目很快就会出现不一致。推荐分层如下：

```text
Vue Component
      ↓ 调用业务函数
Business API Module
      ↓ 使用共用实例
Axios Instance
      ↓ HTTP
Backend
```

- Component负责显示、输入和当前页面状态；
- 业务API模块表达`getTasks()`、`createTask()`等业务操作；
- Axios实例保存所有请求共用的技术设置；
- Backend负责权限、业务校验和永久保存。

安装Axios：

```cmd
npm install axios
```

```text
src/
└─ api/
   ├─ http.js
   └─ tasks.js
```

## 3. 建立共用Axios实例

`src/api/http.js`：

```js
import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10_000,
  headers: { Accept: 'application/json' },
})
```

`axios.create()`创建可复用的Axios实例。`baseURL`会与`/tasks`组合，`timeout`表示最长等待毫秒数，`headers`是共用请求头。不要在每个组件重复创建实例。

## 4. 建立业务API模块

`src/api/tasks.js`：

```js
import { http } from './http'

export async function getTasks() {
  const response = await http.get('/tasks')
  return response.data
}

export async function createTask(input) {
  const response = await http.post('/tasks', {
    title: input.title,
    priority: input.priority,
  })
  return response.data
}
```

`http.get()`读取数据，`http.post()`通过请求体发送数据。API模块返回页面需要的`response.data`，组件不需要了解Axios响应对象的全部结构。

## 5. 页面处理四种基本状态

任务一览当前只由一个页面使用，因此先由Component保存状态：

```vue
<script setup>
import { onMounted, ref } from 'vue'
import { getTasks } from '@/api/tasks'

const tasks = ref([])
const loading = ref(false)
const loaded = ref(false)
const errorMessage = ref('')

async function loadTasks() {
  loading.value = true
  loaded.value = false
  errorMessage.value = ''

  try {
    tasks.value = await getTasks()
    loaded.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '任务读取失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadTasks)
</script>

<template>
  <p v-if="loading">读取中...</p>
  <div v-else-if="errorMessage" role="alert">
    <p>{{ errorMessage }}</p>
    <button type="button" @click="loadTasks">重试</button>
  </div>
  <p v-else-if="loaded && tasks.length === 0">没有任务</p>
  <ul v-else-if="loaded">
    <li v-for="task in tasks" :key="task.id">{{ task.title }}</li>
  </ul>
</template>
```

`try`执行请求，`catch`处理失败。空数据是成功响应后数组为空，不是系统错误。

## 6. POST请求与防止重复提交

提交顺序应是“校验→进入保存中→发送请求→成功处理→恢复保存状态”：

```js
const saving = ref(false)
const saveError = ref('')

async function submitTask() {
  if (saving.value || !validateForm()) return

  saving.value = true
  saveError.value = ''

  try {
    const created = await createTask({ title: form.title, priority: form.priority })
    tasks.value.push(created)
    resetForm()
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}
```

按钮用`:disabled="saving"`阻止请求处理中的重复点击。失败时不要清空表单。前端防重复点击不等于后端接口具有幂等性，写入请求不能在结果不明时自动重试。

## 7. API失败时使用Network调查

调查API问题时，Network面板属于前端工程师的基础能力。

```text
F12
→ Network
→ 重新执行页面操作
→ 选择请求
→ 查看Request URL、Request Method、Status
→ 查看Request Headers、Request Payload、Response、Timing
```

| 状态或现象 | 基础理解 | 首先确认 |
| --- | --- | --- |
| 400 | 请求内容不正确 | Payload、字段名、格式、后台错误信息 |
| 401 | 未认证或登录失效 | Cookie或认证头是否发送 |
| 403 | 已认证但无权限 | 当前用户、角色和目标资源 |
| 404 | API路径或数据不存在 | URL、方法、路径参数 |
| 409 | 当前数据状态发生冲突 | 后台业务错误和最新数据 |
| 500 | 后端处理异常 | Response与后端日志 |
| timeout | 超过前端等待时间 | Timing、后端耗时、超时设置 |
| Network Error | 无法连接或被浏览器阻止 | 服务、地址、CORS、网络 |

不能只看到Console红字就判断“前端坏了”。先确认请求有没有发出、发到了哪里、服务器返回了什么。

## 8. 使用环境变量管理API地址

项目根目录可准备`.env.development`和`.env.production`：

```dotenv
VITE_API_BASE_URL=/api
```

共用实例读取：

```js
baseURL: import.meta.env.VITE_API_BASE_URL
```

只有`VITE_`开头的变量会暴露给客户端代码，因此其中不能保存密码、密钥或真正的秘密。修改`.env`后需要重启开发服务器。

## 9. 拦截器用于共通技术处理

没有统一处理需求时，不需要为了使用Interceptor而创建空拦截器。拦截器适合所有请求共同执行的技术处理。

```js
http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

Request Interceptor常用于统一添加认证头；Response Interceptor可统一转换API错误或按项目规则处理401。它不应该直接控制某个页面的Message、弹窗或局部UI状态。

`sessionStorage`只是Bearer Token示例。真实项目也可能使用由服务器设置的HttpOnly Cookie；不要理解成登录系统一定要把Token保存到`localStorage`或`sessionStorage`。

## 10. Vite Proxy与CORS

协议、主机或端口任一不同就是不同Origin。浏览器是否允许跨Origin请求由后端CORS响应头决定。本地开发可配置Vite Proxy：

```js
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

Proxy只解决本地开发转发，不能代替生产环境的认证、授权、CORS或反向代理设计。

## 11. API响应的运行时安全检查

【会使用、能理解】服务器实际返回的数据属于外部数据。TypeScript只能在开发阶段检查代码，不能保证运行时响应一定符合规格。重要接口可进行运行时检查：

```js
const priorities = ['low', 'normal', 'high']
const statuses = ['todo', 'doing', 'done']

export function isTask(value) {
  return typeof value === 'object'
    && value !== null
    && typeof value.id === 'number'
    && typeof value.title === 'string'
    && typeof value.assignee === 'string'
    && priorities.includes(value.priority)
    && statuses.includes(value.status)
    && typeof value.dueDate === 'string'
}
```

如果项目决定启用这项检查，`getTasks()`在返回数据前验证完整数组：

```js
if (!Array.isArray(response.data) || !response.data.every(isTask)) {
  throw new Error('任务数据格式不正确')
}
```

这不表示每个普通请求都必须手写`isTask()`。应根据项目风险、接口稳定性和团队采用的校验工具决定。

## 12. 请求取消与竞态

【进阶理解】连续搜索时，旧请求可能晚于新请求返回。可以把`AbortController`的`signal`交给Axios，并只采纳最新请求结果。组件卸载时也可取消仍在执行的读取请求。

- 取消请求通常不显示为普通系统故障；
- `timeout`与主动取消不是同一件事；
- 写入请求不能在未确认幂等性时自动重试；
- 普通页面不必一开始就加入复杂竞态控制。

## 13. 从Component状态过渡到Pinia

目前任务数据只由当前页面使用，因此由Component管理完全合理。下一章学习Pinia后，如果任务数据需要被列表页、详情页等多个页面共享，再把`tasks`、`loading`、`saving`、`errorMessage`以及对应的读取和保存操作提升到Store。

## 14. WorkHub练习与检查点

1. 建立Axios共用实例和任务API模块。
2. 完成任务一览的loading、empty、success、error和重试显示。
3. 完成新增成功、失败保留输入和防重复提交。
4. 使用Network分别调查一条成功请求和一条失败请求。
5. 判断环境变量、Proxy和运行时校验是否属于当前项目需要。

- [ ] 能判断代码应放在Component还是业务API模块。
- [ ] 能说明Axios实例与业务API模块的职责。
- [ ] 能处理loading、empty、success和error。
- [ ] 能通过Network确认URL、方法、Payload、状态码和响应。
- [ ] 能区分400、401、403、404、409和500。
- [ ] 能说明拦截器适合共通技术处理，不负责局部UI。
- [ ] 能说明当前组件状态何时需要提升到Pinia。
