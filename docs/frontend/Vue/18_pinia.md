# 第 18 章 Pinia：跨页面状态管理

第17章由页面自己保存`tasks`、读取用的`loading`、保存用的`saving`和`errorMessage`。当任务列表、任务详情和首页都需要访问同一份任务数据时，状态需要提升到独立的Store。本章使用Pinia完成这次重构，并继续区分读取状态与保存状态。

## 本章目标与前置知识

- 【必须掌握】判断状态应放在Component、Router Query、Pinia、API模块还是Backend；
- 【必须掌握】理解Backend与Pinia的Source of Truth关系；
- 【必须掌握】使用`defineStore()`建立Setup Store；
- 【必须掌握】区分state、getter和action；
- 【必须掌握】让Store调用第17章API模块；
- 【会使用、能看懂】使用`storeToRefs()`、重置Store和处理详情页重新读取。

## 1. 先判断状态应该放在哪里

不是“能不能放Pinia”，而是“是否有多个无直接父子关系的组件或页面需要共享和修改”。

| 状态 | 推荐位置 | 原因 |
| --- | --- | --- |
| 当前输入框 | Component | 只属于当前画面 |
| 当前弹窗状态 | Component | 属于局部UI |
| 多页面共享任务 | Pinia | 多个页面读取和修改 |
| 当前用户 | Pinia | 导航栏和多个页面共用 |
| URL筛选条件 | Router Query | 刷新、复制URL和前进后退时应恢复 |
| API URL、请求方法和HTTP处理 | API模块 | 属于服务器通信契约 |
| 永久业务数据 | Backend / Database | 应跨刷新和跨设备保存 |

一个父组件向一两层子组件传Props并不需要Pinia；一棵组件树内部共享表单上下文也可以使用`provide / inject`。

## 2. Source of Truth：谁才是真正的数据源

```text
Backend / Database
        ↓ API Response
      Pinia
        ↓
    Component
```

Pinia通常保存前端应用运行期间需要共享的状态，以及服务器数据在前端的当前副本。需要永久保存的业务数据，通常仍以Backend / Database为最终数据源（Source of Truth）。

因此：

```text
刷新浏览器
→ Pinia重新创建
→ 必要时重新请求Backend
```

只修改Pinia不等于数据已经保存到数据库。新增、修改或删除业务数据时，应先按API规格请求Backend，再用成功响应更新Store。

## 3. 第17章到第18章是一次重构

第17章：

```text
TaskListView
├─ tasks
├─ loading
├─ errorMessage
└─ loadTasks()
      ↓
   API Module
```

第18章：

```text
TaskListView / TaskDetailView
           ↓
       Task Store
       ├─ tasks
       ├─ loading
       ├─ saving
       ├─ errorMessage
       ├─ loadTasks()
       └─ createTask()
           ↓
       API Module
           ↓
      Axios Instance
```

第17章写法并没有错。数据只属于一个页面时，组件自己管理更简单；出现真实共享需求时，才提升到Pinia。

## 4. Store、Component与API模块的分工

- Component负责输入、显示、点击和页面局部状态；
- Store负责共享业务状态、派生结果和跨页面操作；
- API模块负责URL、HTTP方法、请求体和响应转换；
- Backend负责权限、业务校验和永久保存。

Store可以调用API模块，但不要在Store中重新创建Axios实例；Component也不要绕过Store维护另一份相同任务数组。

## 5. 安装并注册Pinia

```cmd
npm install pinia
```

`src/main.js`：

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
```

`createPinia()`创建Pinia容器，`app.use()`在应用挂载前完成注册。

## 6. 建立最小Store

先只学习本地状态，暂时不接入Backend。`src/stores/tasks.js`：

```js
import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useTaskStore = defineStore('tasks', () => {
  const tasks = ref([])

  function addTaskLocally(title) {
    tasks.value.push({
      id: Date.now(),
      title,
      assignee: '未分配',
      priority: 'normal',
      status: 'todo',
      dueDate: '',
    })
  }

  return { tasks, addTaskLocally }
})
```

`defineStore(id, setup)`定义Store。`id`在项目中唯一；Setup Store中的`ref`成为state，`computed`成为getter，函数成为action；只有返回的成员才能被组件使用。

这里的`addTaskLocally()`只用于理解Pinia响应性，不代表接入Backend后的真实新增流程。

## 7. 在组件中使用Store

```vue
<script setup>
import { useTaskStore } from '@/stores/tasks'

const taskStore = useTaskStore()
</script>

<template>
  <ul>
    <li v-for="task in taskStore.tasks" :key="task.id">{{ task.title }}</li>
  </ul>
</template>
```

`useTaskStore()`取得当前应用中的Store实例。基础阶段优先写`taskStore.tasks`，可以清楚看出状态来源。

## 8. state、getter和action

### 8.1 state保存原始状态

```js
const tasks = ref([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
```

`loading`表示列表或详情正在读取，`saving`表示新增数据正在保存。不要用一个`loading`表示所有异步操作，这样页面才能分别控制列表Loading和保存按钮Disabled。

### 8.2 getter计算派生结果

```js
const completedCount = computed(() =>
  tasks.value.filter((task) => task.status === 'done').length,
)
```

能从`tasks`计算出的值不要再保存一份普通state，否则容易不同步。getter不应发送请求、修改state或操作DOM。

### 8.3 action表达状态操作

```js
function changeTaskStatusLocally(id, status) {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return false
  task.status = status
  return true
}
```

真实项目接入Backend后，状态修改通常还需要调用更新API。action名称应表达业务含义，不使用`setData`之类模糊名称。

## 9. 从本地push改为Backend新增

学习阶段1只修改本地状态；接入Backend后的阶段2如下：

```text
Component
    ↓
store.createTask()
    ↓
API createTask()
    ↓
Backend
    ↓ 返回创建结果
Store更新tasks
```

```js
import { createTask as createTaskApi } from '@/api/tasks'

async function createTask(input) {
  if (saving.value) return null

  saving.value = true
  errorMessage.value = ''
  try {
    const created = await createTaskApi(input)
    tasks.value.push(created)
    return created
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '任务新增失败'
    return null
  } finally {
    saving.value = false
  }
}
```

不要先生成一个假的前端Task再假装已经保存。Store使用后端返回的`id`和完整字段更新列表。`saving`为`true`时直接返回，可以防止保存处理中再次调用新增API。

## 10. 异步action调用API模块

```js
import { getTasks } from '@/api/tasks'

async function loadTasks() {
  loading.value = true
  errorMessage.value = ''

  try {
    tasks.value = await getTasks()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '任务读取失败'
  } finally {
    loading.value = false
  }
}
```

API模块负责产生错误，Store捕获错误并保存共享的`errorMessage`，Component读取并显示该状态。本课程不再从Store重新抛出同一个错误，避免Store和Component重复处理。真实项目也可以选择向调用方重新抛出，但一条项目主线必须采用统一策略。

## 11. 列表与详情的数据读取

从已经加载的列表进入详情时，可以先查找Store：

```js
const currentTask = computed(() =>
  taskStore.tasks.find((task) => task.id === Number(route.params.id)),
)
```

但这种方式只适用于列表已经完整加载，并且详情字段与列表相同。真实项目经常分别提供：

```text
GET /tasks       读取列表
GET /tasks/{id}  读取一条详情
```

列表可能分页、字段可能精简，用户也可能直接打开详情URL。因此API模块应根据规格提供：

```js
export async function getTask(id) {
  const response = await http.get(`/tasks/${id}`)
  return response.data
}
```

Store再提供详情action：

```js
async function loadTask(id) {
  loading.value = true
  errorMessage.value = ''
  try {
    const task = await getTask(id)
    const index = tasks.value.findIndex((item) => item.id === task.id)
    if (index >= 0) tasks.value[index] = task
    else tasks.value.push(task)
    return task
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '任务详情读取失败'
    return null
  } finally {
    loading.value = false
  }
}
```

必须约定何时复用已有数据、何时重新请求，以及失败后由哪个页面提供重试。

## 12. `storeToRefs()`保持解构响应性

```js
import { storeToRefs } from 'pinia'

const taskStore = useTaskStore()
const { tasks, loading, saving, completedCount } = storeToRefs(taskStore)
const { loadTasks, createTask } = taskStore
```

`storeToRefs()`只处理state和getter。action是函数，直接从Store取得。也可以始终使用`taskStore.xxx`，不必为了少写字符强制解构。

## 13. 刷新、重置和持久化边界

Pinia默认保存在内存，刷新后状态消失，所以路由页面应在需要时重新请求Backend。退出登录或切换用户时应清理共享状态：

```js
function reset() {
  tasks.value = []
  loading.value = false
  saving.value = false
  errorMessage.value = ''
}
```

不要随意把整个Store写入`localStorage`。持久化必须考虑有效期、敏感信息、用户切换、旧数据和恢复规则。

## 14. 课程最终Task Store示例

为了让本章所有知识可以在一个文件中回顾，下面的课程示例仍保留`addTaskLocally()`和`changeTaskStatusLocally()`。这两个Action主要用于学习同步Pinia状态操作。真实项目接入Backend以后，如果不存在本地修改需求，可以删除这些教学用途Action，改为使用对应Backend API。

前面逐段学习后，`src/stores/tasks.js`的课程最终状态如下：

```js
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createTask as createTaskApi,
  getTask,
  getTasks,
} from '@/api/tasks'

export const useTaskStore = defineStore('tasks', () => {
  const tasks = ref([])
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')

  const completedCount = computed(() =>
    tasks.value.filter((task) => task.status === 'done').length,
  )

  function addTaskLocally(title) {
    tasks.value.push({
      id: Date.now(),
      title,
      assignee: '未分配',
      priority: 'normal',
      status: 'todo',
      dueDate: '',
    })
  }

  function changeTaskStatusLocally(id, status) {
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
      errorMessage.value = error instanceof Error ? error.message : '任务读取失败'
    } finally {
      loading.value = false
    }
  }

  async function loadTask(id) {
    loading.value = true
    errorMessage.value = ''
    try {
      const task = await getTask(id)
      const index = tasks.value.findIndex((item) => item.id === task.id)
      if (index >= 0) tasks.value[index] = task
      else tasks.value.push(task)
      return task
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '任务详情读取失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function createTask(input) {
    if (saving.value) return null

    saving.value = true
    errorMessage.value = ''
    try {
      const created = await createTaskApi(input)
      tasks.value.push(created)
      return created
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '任务新增失败'
      return null
    } finally {
      saving.value = false
    }
  }

  function reset() {
    tasks.value = []
    loading.value = false
    saving.value = false
    errorMessage.value = ''
  }

  return {
    tasks,
    loading,
    saving,
    errorMessage,
    completedCount,
    addTaskLocally,
    changeTaskStatusLocally,
    loadTasks,
    loadTask,
    createTask,
    reset,
  }
})
```

`addTaskLocally()`和`changeTaskStatusLocally()`只演示同步Pinia状态操作。需要永久保存的新增或状态变更，应调用Backend对应API，并在成功后使用响应更新Store。

## 15. 调试与常见错误

使用Vue DevTools查看调用了哪个action、参数是什么、state怎样变化以及页面是否读取同一Store。

- 把所有输入框、弹窗和hover状态都放进Pinia；
- 只修改Pinia就认为数据库已更新；
- 列表页和详情页分别维护一份任务数组；
- 在Store中重新书写Axios配置；
- 直接打开详情时只从空列表`find()`；
- action结束后没有在`finally`恢复对应的`loading`或`saving`；
- Setup Store忘记返回需要公开的成员。

## 16. WorkHub练习与检查点

1. 列出当前状态并决定Component、Router Query、Pinia、API模块或Backend归属。
2. 注册Pinia并建立最小Task Store。
3. 从两个页面读取同一Store，观察共享状态。
4. 将第17章组件中的任务状态和`loadTasks()`重构到Store。
5. 将本地新增改为`createTask()`请求成功后再更新Store。
6. 增加`getTask(id)`并验证直接打开详情URL。
7. 刷新页面并说明为什么需要重新请求。
8. 退出时执行`reset()`。

- [ ] 能判断状态应该放在Component、Router还是Pinia。
- [ ] 能说明Backend与Pinia的Source of Truth关系。
- [ ] 能解释刷新后为什么通常需要重新请求。
- [ ] 能区分Store与API模块的职责。
- [ ] 能区分本地演示push和真实POST流程。
- [ ] 能实现列表读取、详情读取和新增后的Store更新。
- [ ] 能用`loading`表示读取中，用`saving`表示保存中。
- [ ] 能说明为什么不能把所有状态都放进Pinia。
