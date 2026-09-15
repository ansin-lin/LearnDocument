# 第 18 章 Pinia：跨页面状态管理

Pinia是Vue的状态管理库。它允许多个组件或页面共享同一份响应式状态，并把读取、计算和修改规则集中到Store中。

## 本章目标与前置知识

- 【必须掌握】判断状态应该留在组件还是进入Store。
- 【必须掌握】安装并注册Pinia，使用`defineStore()`建立Setup Store。
- 【必须掌握】理解state、getter和action的职责。
- 【必须掌握】在组件中使用Store，并通过`storeToRefs()`保持解构后的响应性。
- 【会使用、能看懂】在action中调用API、处理加载和错误，并重置Store。

需要掌握`ref()`、`computed()`、组件状态归属和第17章API模块。本章继续使用普通JavaScript。

## 1. 为什么需要Pinia

单个输入框内容只属于一个组件，可以用组件自己的`ref()`保存。但任务一览和任务详情都需要读取、修改同一批任务时，如果两个页面分别保存一份数组，就可能出现不同步。

```text
TaskListView自己的tasks   ← 修改后只更新列表页
TaskDetailView自己的tasks ← 仍然保留旧数据
```

Pinia把共同状态放进独立Store：

```text
                 ┌─ TaskListView读取任务列表
后端API → Task Store
                 └─ TaskDetailView读取并修改同一状态
```

Store不属于某一个页面，因此路由切换后，只要应用仍在运行，其他页面可以继续读取同一个Store实例。

### 1.1 Pinia负责什么

- 保存跨组件或跨页面共享的业务状态；
- 集中定义状态的计算结果和修改操作；
- 让不同页面取得同一份响应式数据；
- 通过Vue DevTools观察状态和action执行过程；
- 为状态逻辑测试提供明确入口。

### 1.2 Pinia不负责什么

- 不负责URL与页面切换，那是Vue Router的职责；
- 不负责发送HTTP请求，请求细节属于API模块；
- 不会自动把状态永久保存到数据库；
- 刷新浏览器后，默认内存状态会消失；
- 不应该保存所有组件的临时变量。

## 2. 什么是Store

Store可以理解为一组有明确业务职责的共享状态和操作。任务Store只管理任务相关数据，不同时管理登录用户、系统主题和所有表单。

Pinia中的三个核心概念是：

| 概念 | 作用 | Vue组件中的相似概念 |
| --- | --- | --- |
| state | 保存原始状态 | `ref()`、`reactive()` |
| getter | 根据state得到派生结果 | `computed()` |
| action | 执行修改或异步业务操作 | 普通函数、事件处理函数 |

本课程使用Setup Store，它与已经学习的Composition API写法一致：`ref`成为state，`computed`成为getter，函数成为action。

## 3. 安装并注册Pinia

在项目根目录执行：

```bash
npm install pinia
```

修改`src/main.js`：

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.mount('#app')
```

`createPinia()`创建Pinia实例；`app.use(pinia)`把它安装到Vue应用，必须在`mount()`前执行。安装后，各组件才能取得相同Store。

## 4. 建立第一个最小Store

先只建立一项state和一个action，不要一次加入请求、getter和全部业务操作。

新建`src/stores/tasks.js`：

```js
import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useTaskStore = defineStore('tasks', () => {
  const tasks = ref([
    {
      id: 101,
      title: '规格确认',
      assignee: '田中',
      priority: 'high',
      status: 'doing',
      dueDate: '2026-09-30',
    },
  ])

  function addTask(title) {
    tasks.value.push({
      id: Date.now(),
      title,
      assignee: '未分配',
      priority: 'normal',
      status: 'todo',
      dueDate: '',
    })
  }

  return {
    tasks,
    addTask,
  }
})
```

`defineStore(id, setup)`定义Store：

- `id`是项目内唯一的Store标识，这里是`tasks`；
- 第二个参数是建立Store内容的函数；
- 函数中创建state和action；
- 最后必须返回需要让组件使用的内容。

`defineStore()`返回的是`useTaskStore`函数。组件调用它时，才取得当前应用中的任务Store实例。

## 5. 在组件中使用Store

`TaskListView.vue`：

```vue
<script setup>
import { useTaskStore } from '@/stores/tasks'

const taskStore = useTaskStore()
</script>

<template>
  <main>
    <h1>任务一览</h1>
    <button type="button" @click="taskStore.addTask('代码Review')">
      添加演示任务
    </button>

    <ul>
      <li v-for="task in taskStore.tasks" :key="task.id">
        {{ task.title }}
      </li>
    </ul>
  </main>
</template>
```

`useTaskStore()`取得Store。模板可以直接读取`taskStore.tasks`，也可以调用`taskStore.addTask()`。添加任务后，所有使用同一Store的页面都会读取到更新后的数组。

先使用完整的`taskStore.xxx`写法，可以清楚看出数据来自Store。

## 6. state：Store保存的原始状态

Setup Store中的`ref()`是state：

```js
const tasks = ref([])
const loading = ref(false)
const errorMessage = ref('')
```

state应保存业务的原始事实，例如任务数组和请求状态。能够从现有state计算出来的数据不要重复保存，否则修改任务后还必须手动同步多个字段。

例如“已完成任务数”能从`tasks`计算出来，不应再维护一个需要手动加减的普通state。

## 7. action：集中执行状态修改

Store中的函数是action。action可以接收参数、修改state、调用其他action，也可以执行异步请求。

```js
function changeTaskStatus(id, status) {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return false

  task.status = status
  return true
}
```

把修改规则集中在`changeTaskStatus()`中，可以统一校验状态和任务编号。组件只表达“请求修改”，不在多个页面重复实现相同规则。

简单状态技术上可以直接修改，但复杂业务修改优先通过含义明确的action完成，便于调查影响范围和测试。

## 8. getter：根据state得到派生结果

Setup Store中的`computed()`是getter：

```js
import { computed, ref } from 'vue'

const tasks = ref([])

const completedCount = computed(() =>
  tasks.value.filter((task) => task.status === 'done').length,
)
```

`completedCount`依赖`tasks`。任务新增、删除或状态变化后，结果自动重新计算。getter不应该产生请求、修改state或操作DOM。

把getter加入Store返回值后，组件可以像读取state一样读取它：

```js
return {
  tasks,
  completedCount,
  addTask,
  changeTaskStatus,
}
```

## 9. storeToRefs：解构时保持响应性

不解构时可以直接写：

```js
const taskStore = useTaskStore()
console.log(taskStore.tasks)
```

如果希望单独取得state和getter，应使用`storeToRefs()`：

```js
import { storeToRefs } from 'pinia'
import { useTaskStore } from '@/stores/tasks'

const taskStore = useTaskStore()
const { tasks, completedCount } = storeToRefs(taskStore)
const { addTask, changeTaskStatus } = taskStore
```

`storeToRefs(store)`把Store中的state和getter转换成保持响应性的ref。action本身是函数，直接从Store解构，不放进`storeToRefs()`。

基础阶段也可以始终使用`taskStore.tasks`，不必为了少写字符强制解构。

## 10. 异步action与API模块

API模块负责URL、请求方法、HTTP状态和响应数据检查；Store action负责调用API，并维护需要跨页面共享的加载和错误状态。

在`src/stores/tasks.js`中增加：

```js
import { getTasks } from '@/api/tasks'

const loading = ref(false)
const errorMessage = ref('')

async function loadTasks() {
  loading.value = true
  errorMessage.value = ''

  try {
    tasks.value = await getTasks()
  } catch (error) {
    errorMessage.value = error instanceof Error
      ? error.message
      : '任务读取失败'
    throw error
  } finally {
    loading.value = false
  }
}
```

`getTasks()`来自第17章API模块。action不重新编写Axios请求细节。`finally`保证成功或失败后都恢复`loading`。

调用方和Store应约定谁显示错误。如果Store保存`errorMessage`供页面显示，页面不要再显示另一份含义相同的错误。

## 11. 列表页与详情页共享状态

列表页读取`taskStore.tasks`，详情页根据路由编号从同一个数组查找：

```js
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTaskStore } from '@/stores/tasks'

const route = useRoute()
const taskStore = useTaskStore()

const currentTask = computed(() =>
  taskStore.tasks.find((task) => task.id === Number(route.params.id)),
)
```

从列表页进入详情时可以复用已加载数据。但用户直接刷新详情页时，Store会重新创建，数组可能为空，因此详情页仍要按项目规则调用加载action。

需要提前约定：

- 哪个页面负责首次加载；
- Store已有数据时是否重新请求；
- 同一请求是否允许重复发送；
- 失败后由哪个页面提供重试。

## 12. 状态应该放在哪里

| 状态 | 推荐位置 | 原因 |
| --- | --- | --- |
| 多页面共用任务、当前用户 | Pinia | 跨页面读取和修改 |
| 只属于当前表单的输入值 | 组件 | 离开表单后通常无需保留 |
| 当前弹窗是否打开 | 通常组件 | 属于局部界面状态 |
| 深层组件共同使用的表单上下文 | `provide / inject` | 只在一棵组件树中使用 |
| URL中的筛选条件 | Router query | 需要复制、刷新和前进后退 |
| 请求URL和HTTP检查 | API模块 | 属于服务器通信契约 |

判断标准不是“能不能放进Pinia”，而是“是否需要被多个无直接父子关系的组件共同使用和修改”。

## 13. 重置、刷新与持久化边界

Setup Store需要自己提供重置action：

```js
function reset() {
  tasks.value = []
  loading.value = false
  errorMessage.value = ''
}
```

退出登录或切换业务上下文时可以调用`reset()`，避免上一用户的数据继续留在内存。

Pinia默认只保存在内存，刷新页面后状态会消失。需要持久化时必须设计过期时间、用户切换、敏感数据和恢复规则，不能随意把整个Store写入`localStorage`。真正的业务数据仍应由后端保存。

## 14. 完整Task Store示例

学习完各部分后，把Store整理成一个可以直接运行的版本。注意：Setup Store中的state、getter和action只有写在`return`中，组件才能访问。

```js
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getTasks } from '@/api/tasks'

export const useTaskStore = defineStore('tasks', () => {
  const tasks = ref([])
  const loading = ref(false)
  const errorMessage = ref('')

  const completedCount = computed(() =>
    tasks.value.filter((task) => task.status === 'done').length,
  )

  function addTask(title) {
    tasks.value.push({
      id: Date.now(),
      title,
      assignee: '未分配',
      priority: 'normal',
      status: 'todo',
      dueDate: '',
    })
  }

  function changeTaskStatus(id, status) {
    const task = tasks.value.find((item) => item.id === id)
    if (!task) return false
    task.status = status
    return true
  }

  async function loadTasks() {
    loading.value = true
    errorMessage.value = ''
    try {
      tasks.value = await getTasks()
    } catch (error) {
      errorMessage.value = error instanceof Error
        ? error.message
        : '任务读取失败'
      throw error
    } finally {
      loading.value = false
    }
  }

  function reset() {
    tasks.value = []
    loading.value = false
    errorMessage.value = ''
  }

  return {
    tasks,
    loading,
    errorMessage,
    completedCount,
    addTask,
    changeTaskStatus,
    loadTasks,
    reset,
  }
})
```

## 15. 调试与命名

使用Vue DevTools检查Store的state、getter和action执行记录。action使用`loadTasks`、`changeTaskStatus`等业务名称，不使用含义模糊的`setData`。

调查状态问题时按以下顺序确认：

1. 哪个组件调用了哪个action；
2. action接收了什么参数；
3. state在调用前后怎样变化；
4. getter是否根据新state重新计算；
5. 页面是否读取了同一个Store实例。

## 16. 常见错误

- 所有输入框、弹窗和hover状态都放进Store。
- 列表页和详情页分别维护一份任务数组。
- 直接解构state后误以为一定保持响应性。
- 在组件和Store中重复编写相同API请求。
- action失败后没有在`finally`恢复`loading`。
- 误以为Pinia会自动保存到数据库或`localStorage`。
- Setup Store漏掉返回值，导致组件无法访问对应state或action。

## 17. WorkHub练习与检查点

1. 安装并注册Pinia，创建只有任务数组和`addTask()`的最小Store。
2. 从两个组件读取同一个Store，确认一个组件添加任务后另一个组件同步显示。
3. 增加`changeTaskStatus()`和`completedCount`，分别验证action和getter。
4. 使用`storeToRefs()`解构state和getter，action保持直接解构。
5. 把第17章API模块接入异步action，分别模拟成功和失败。
6. 从详情页直接刷新，确认能够重新加载任务。
7. 增加`reset()`并验证状态恢复。
8. 列出当前页面状态，判断哪些应留在组件、Router、API模块或Pinia。

- [ ] 能说明Pinia解决的问题及其职责边界。
- [ ] 能安装、注册并定义Setup Store。
- [ ] 能区分state、getter和action。
- [ ] 能在多个组件或页面中使用同一个Store。
- [ ] 能正确使用`storeToRefs()`并处理action。
- [ ] 能实现带加载和错误处理的异步action。
- [ ] 能说明刷新、重置和持久化的边界。





