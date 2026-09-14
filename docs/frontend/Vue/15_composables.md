# 第 15 章 Composables：组合式函数与逻辑复用

组件负责界面结构，但一个组件中的响应式状态和操作也可能越来越多。当一组逻辑具有明确职责、需要在多个组件复用，或适合独立测试时，可以把它提取为Composable（组合式函数）。

Composable通常是名称以`use`开头的TypeScript函数。它可以使用`ref()`、`computed()`、`watch()`和生命周期钩子，负责复用有状态的Vue逻辑，但不负责复用HTML结构。

## 本章目标与前置知识

- 【必须掌握】识别适合提取的响应式逻辑，并建立一个职责明确的Composable。
- 【必须掌握】设计参数和返回值，在组件中调用Composable。
- 【必须掌握】理解函数内部状态与模块共享状态的区别。
- 【会使用、能看懂】在Composable内部管理生命周期和异步状态。
- 【会读即可】判断逻辑更适合组件、普通TypeScript模块、API模块还是Pinia。

需要掌握`ref()`、`computed()`、`watch()`、生命周期、TypeScript函数和ES模块。本章示例使用TypeScript，并优先依赖类型推断。

## 1. 先观察组件中的问题

任务页面可能同时包含搜索关键字和筛选结果：

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Task } from './types/task'

const tasks = ref<Task[]>([
  { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'doing', dueDate: '2026-10-05' },
])
const keyword = ref('')
const filteredTasks = computed(() =>
  tasks.value.filter((task) => task.title.includes(keyword.value)),
)
</script>
```

如果多个页面都需要相同筛选逻辑，复制代码会产生多处修改点。即使目前只使用一次，当筛选规则已经是一项可独立说明和测试的职责时，也可以考虑提取。

## 2. 什么是Composable

Composable具有以下特点：

- 是普通TypeScript函数，可以接收参数并返回结果；
- 函数内部使用Vue组合式API管理响应式状态或生命周期；
- 名称通常以`use`开头，例如`useTaskFilter`；
- 复用状态和操作，不包含`template`；
- 在组件的`script setup`中调用。

普通工具函数只做数据计算时，不需要写成Composable：

```ts
export function formatTaskTitle(title: string): string {
  return title.trim()
}
```

这个函数没有Vue响应式状态，放在普通TypeScript模块中更简单。

## 3. 提取useTaskFilter

新建`src/composables/useTaskFilter.ts`：

```ts
import { computed, ref, type Ref } from 'vue'
import type { Task } from '../types/task'

export function useTaskFilter(tasks: Ref<Task[]>) {
  const keyword = ref('')

  const filteredTasks = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase()

    if (!normalizedKeyword) {
      return tasks.value
    }

    return tasks.value.filter((task) =>
      task.title.toLowerCase().includes(normalizedKeyword),
    )
  })

  return {
    keyword,
    filteredTasks,
  }
}
```

`useTaskFilter(tasks)`接收任务数组的ref，因此内部通过`tasks.value`读取最新数组。它返回`keyword`和`filteredTasks`两个响应式对象，让调用组件决定怎样显示和操作它们。

不要写成`return { keyword: keyword.value }`。这样只会返回调用当时的普通字符串，后续修改不会保持响应式联系。

## 4. 在组件中使用Composable

`TaskListView.vue`：

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useTaskFilter } from './composables/useTaskFilter'
import type { Task } from './types/task'

const tasks = ref<Task[]>([
  { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'doing', dueDate: '2026-10-05' },
])

const { keyword, filteredTasks } = useTaskFilter(tasks)
</script>

<template>
  <main>
    <label for="task-keyword">任务名称</label>
    <input id="task-keyword" v-model.trim="keyword">

    <p v-if="filteredTasks.length === 0">没有符合条件的任务</p>
    <ul v-else>
      <li v-for="task in filteredTasks" :key="task.id">
        {{ task.title }}
      </li>
    </ul>
  </main>
</template>
```

输入“页面”后只显示“页面实现”；清空输入后再次显示全部任务。模板会自动解包ref，所以写`keyword`和`filteredTasks`；TypeScript脚本中仍要通过`.value`访问。

数据关系如下：

```text
组件的tasks ref
      ↓ 参数
useTaskFilter
      ↓ 返回
keyword ref + filteredTasks computed
      ↓
组件模板显示和操作
```

## 5. 参数和返回值怎样设计

Composable的参数和返回值就是它对调用方公开的接口。

### 5.1 参数保持明确

本例明确要求调用方传入任务数组ref，不同时兼容普通数组、ref和函数。接口越“万能”，内部判断越多，新人和维护者越难确认实际输入。

### 5.2 返回调用方真正需要的内容

返回对象比依赖数组位置更容易阅读：

```ts
return { keyword, filteredTasks }
```

如果返回项不断增加，通常说明Composable承担了筛选、请求、分页、编辑等多个职责，应考虑继续拆分。

### 5.3 不暗中修改调用方状态

Composable可以读取调用方传入的`tasks`，但不应该在没有清晰名称和说明的情况下删除或覆盖任务。需要修改时，应返回名称明确的操作函数，例如`removeTask(id)`。

## 6. 每次调用是否共享状态

状态声明在函数内部时，每次调用都会创建独立状态：

```ts
import { ref, type Ref } from 'vue'
import type { Task } from '../types/task'

export function useTaskFilter(tasks: Ref<Task[]>) {
  const keyword = ref('')
  // 省略筛选逻辑
  return { keyword }
}
```

两个组件分别调用`useTaskFilter()`，修改一个组件的关键字不会影响另一个组件。

状态声明在函数外部时，所有调用方会取得同一个状态：

```ts
const keyword = ref('')

export function useSharedTaskKeyword() {
  return { keyword }
}
```

这种写法会形成模块级共享状态，并不一定错误，但必须是明确的业务需求。不要因为把`ref`写在错误位置而意外共享。跨页面共享且需要统一操作入口的业务状态，通常交给第18章的Pinia管理。

## 7. 带生命周期的useWindowSize

Composable可以把资源的注册和清理放在一起。新建`src/composables/useWindowSize.ts`：

```ts
import { onMounted, onUnmounted, ref } from 'vue'

export function useWindowSize() {
  const width = ref(0)

  function updateWidth() {
    width.value = window.innerWidth
  }

  onMounted(() => {
    updateWidth()
    window.addEventListener('resize', updateWidth)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateWidth)
  })

  return { width }
}
```

组件中使用：

```vue
<script setup lang="ts">
import { useWindowSize } from './composables/useWindowSize'

const { width } = useWindowSize()
</script>

<template>
  <p>窗口宽度：{{ width }}px</p>
</template>
```

组件挂载时取得初始宽度并注册监听，卸载时清理。调用方只关心返回的`width`，不需要重复管理浏览器事件。

包含生命周期钩子的Composable必须在组件`script setup`执行期间同步调用，不能等到点击事件中才临时调用。

## 8. 带异步状态的useTaskLoader

请求通常同时具有加载中、成功和失败状态。Composable可以管理这些状态，但具体URL和响应检查仍应放在API模块中。

`src/composables/useTaskLoader.ts`：

```ts
import { ref } from 'vue'
import type { Task } from '../types/task'

export function useTaskLoader(loadTasks: () => Promise<Task[]>) {
  const tasks = ref<Task[]>([])
  const loading = ref(false)
  const errorMessage = ref('')

  async function execute(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    try {
      tasks.value = await loadTasks()
    } catch (error) {
      errorMessage.value = error instanceof Error
        ? error.message
        : '任务读取失败'
    } finally {
      loading.value = false
    }
  }

  return {
    tasks,
    loading,
    errorMessage,
    execute,
  }
}
```

`loadTasks`由调用方传入，必须是一个返回Promise的函数。`execute()`调用该函数，并保证成功或失败后都把`loading`恢复为`false`。

组件中的最小调用方式：

```vue
<script setup lang="ts">
import { onMounted } from 'vue'
import { useTaskLoader } from './composables/useTaskLoader'
import type { Task } from './types/task'

async function loadTasks(): Promise<Task[]> {
  return [
    { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
    { id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'doing', dueDate: '2026-10-05' },
  ]
}

const { tasks, loading, errorMessage, execute } = useTaskLoader(loadTasks)

onMounted(execute)
</script>

<template>
  <p v-if="loading">读取中...</p>
  <p v-else-if="errorMessage" role="alert">{{ errorMessage }}</p>
  <ul v-else>
    <li v-for="task in tasks" :key="task.id">{{ task.title }}</li>
  </ul>
</template>
```

这里用本地异步函数演示状态变化，不依赖服务器。真实HTTP请求、超时和取消将在第17章学习。

## 9. 代码应该放在哪里

| 内容 | 推荐位置 | 判断依据 |
| --- | --- | --- |
| 可复用HTML外观 | 组件 | 需要复用模板结构 |
| 可复用响应式状态和逻辑 | Composable | 使用Vue响应式API或生命周期 |
| 无Vue依赖的格式化、计算 | 普通TypeScript模块 | 输入普通值，返回普通结果 |
| URL、请求发送和响应检查 | API模块 | 负责后端通信契约 |
| 多页面共享业务状态 | Pinia Store | 多个页面共同读取和修改 |

不要为了“分层”而机械抽取只有一两行、只使用一次的表达式。日本项目维护中应先阅读既有目录、命名和改修范围，再决定是否重构。

## 10. 常见错误

- 返回`ref.value`，导致调用方失去响应式联系。
- 把状态意外声明在函数外，使多个组件共享数据。
- 一个Composable同时处理筛选、请求、路由和全部表单操作。
- 在模块导入时立即发送请求，调用方无法控制执行时机。
- 注册事件监听器或定时器后没有清理。
- 在事件回调中临时调用带生命周期钩子的Composable。
- 把普通字符串处理函数也包装成Composable。

## 11. WorkHub练习与检查点

1. 从任务页面提取`useTaskFilter()`，在两个组件中分别调用，确认关键字互不影响。
2. 故意返回`keyword.value`，观察模板不再保持响应式后修正。
3. 实现`useWindowSize()`，通过条件渲染反复创建和卸载组件，确认监听器没有累积。
4. 使用`useTaskLoader()`分别模拟成功和抛出错误，确认加载、成功和失败界面都能显示。
5. 把“日期格式化”“任务筛选”“接口请求”“跨页面登录用户”分别归类到普通模块、Composable、API模块或Pinia，并说明理由。

- [ ] 能解释Composable与组件、普通TypeScript函数的区别。
- [ ] 能设计清楚的参数和返回值，并在组件中调用。
- [ ] 能说明函数内状态和模块共享状态的差异。
- [ ] 能保证返回的ref没有被提前解包。
- [ ] 能在Composable中成对管理外部资源。
- [ ] 能判断逻辑应放在组件、Composable、普通模块、API模块还是Pinia。





