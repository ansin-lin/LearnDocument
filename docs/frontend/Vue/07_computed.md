# 第 7 章 computed与派生状态

## 本章目标

- 【必须掌握】区分原始状态与派生状态，使用`computed()`生成统计、筛选和排序结果。
- 【必须掌握】判断什么时候不应额外保存一份状态。
- 【会使用、能看懂】说明计算属性的缓存与普通函数的差异。

## 前置知识

需要掌握`ref`、数组`filter()`与`map()`、事件绑定和第6章的任务列表操作。

## 1. 为什么需要派生状态

WorkHub已经用`tasks`保存任务。任务总数、完成数和筛选结果都能从`tasks`计算出来。如果同时保存`tasks`和`remainingCount`，每次新增、删除、切换状态都必须手动同步，漏改一次界面就会矛盾。

```text
tasks（原始状态）
  ├─ totalCount
  ├─ completedCount
  └─ filteredTasks
       （派生状态）
```

能可靠计算出来的数据通常不重复保存，而是使用计算属性。

## 2. 最小示例

`App.vue`：

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Task, TaskStatus } from './types/task'

type StatusFilter = 'all' | TaskStatus

const price = ref(1200)
const quantity = ref(2)
const total = computed(() => price.value * quantity.value)
</script>

<template>
  <p>合计：{{ total }}円</p>
  <button @click="quantity++">增加数量</button>
</template>
```

`computed(getter)`接收一个返回结果的函数，并返回计算属性ref。脚本中读取结果使用`total.value`，模板会自动解包，所以写`{{ total }}`。依赖的`price`或`quantity`变化时，Vue重新计算并更新页面。

### 2.1 computed返回的仍然是ref

```ts
console.log(total.value) // 2400
quantity.value = 3
console.log(total.value) // 3600
```

`total`不是普通数字，而是只读的计算属性ref。脚本读取它仍然写`.value`；模板与普通ref一样自动解包。不要写`total = 3600`或`total.value = 3600`，结果应该由`price`和`quantity`决定。

### 2.2 Vue怎样知道依赖发生了变化

第一次读取`total`时，getter读取了`price.value`和`quantity.value`，Vue便把它们记录为依赖：

```text
读取total
↓
执行getter
↓
读取price.value、quantity.value
↓
Vue记录依赖
↓
任一依赖变化时，将total标记为需要重新计算
```

没有在getter执行期间读取的状态，不会成为这个计算属性的依赖。

### 2.3 缓存是什么意思

依赖没有变化时，多次读取`total.value`会复用上一次结果，不重复执行getter。依赖变化后，下一次需要结果时才重新计算。缓存不是永久保存，也不是把结果写入浏览器存储。

## 3. WorkHub统计与筛选

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'

const keyword = ref('')
const status = ref<StatusFilter>('all')
const tasks = ref<Task[]>([
  { id: 101, title: 'API仕様確認', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '一覧画面実装', assignee: '佐藤', priority: 'high', status: 'done', dueDate: '2026-10-05' },
])

const totalCount = computed(() => tasks.value.length)
const completedCount = computed(
  () => tasks.value.filter(task => task.status === 'done').length,
)
const remainingCount = computed(() => totalCount.value - completedCount.value)
const filteredTasks = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return tasks.value.filter(task => {
    const matchesKeyword = !normalizedKeyword
      || task.title.toLowerCase().includes(normalizedKeyword)
    const matchesStatus = status.value === 'all' || task.status === status.value
    return matchesKeyword && matchesStatus
  })
})

function updateKeyword(event: Event): void {
  keyword.value = (event.currentTarget as HTMLInputElement).value
}

function updateStatus(event: Event): void {
  const value = (event.currentTarget as HTMLSelectElement).value
  if (value === 'all' || value === 'todo' || value === 'doing' || value === 'done') {
    status.value = value
  }
}
</script>

<template>
  <label>
    任务关键字
    <input :value="keyword" @input="updateKeyword">
  </label>
  <label>
    状态
    <select :value="status" @change="updateStatus">
      <option value="all">全部</option>
      <option value="todo">未着手</option>
      <option value="doing">进行中</option>
      <option value="done">已完成</option>
    </select>
  </label>
  <p>全部{{ totalCount }}件 / 完成{{ completedCount }}件 / 未完成{{ remainingCount }}件</p>
  <ul>
    <li v-for="task in filteredTasks" :key="task.id">{{ task.title }}</li>
  </ul>
  <p v-if="filteredTasks.length === 0">符合条件的任务不存在。</p>
</template>
```

计算属性应当只计算并返回值，不在其中修改`tasks`、发送请求或写入存储。此类动作属于副作用，第9章使用`watch`处理。

筛选可以分三步理解：先标准化关键字，再分别计算“关键字是否匹配”和“状态是否匹配”，最后要求两个条件都成立。这里继续使用已学过的事件绑定；第8章会用`v-model`简化控件同步。

### 3.1 computed可以依赖另一个computed

`remainingCount`读取了`totalCount.value`和`completedCount.value`，因此这两个计算属性就是它的依赖。任务改变后，相关计算结果会按依赖关系重新得到，不需要手动规定执行顺序。

### 3.2 一个常见错误：手动保存统计值

```ts
const tasks = ref<Task[]>([])
const remainingCount = ref(0)

function addTask(task: Task): void {
  tasks.value.push(task)
  // 如果忘记remainingCount.value++，两个状态立即不一致
}
```

问题不是加一代码难写，而是以后新增、删除、批量更新等每条路径都必须同步。改成`computed(() => tasks.value.filter(task => task.status !== 'done').length)`后，只维护任务数组即可。

## 4. 排序时不要修改原数组

`sort()`会修改原数组。派生排序结果时先复制：

```ts
const sortedTasks = computed(() =>
  [...tasks.value].sort((a, b) => a.id - b.id),
)
```

这样显示顺序的计算不会偷偷改变原始任务顺序。

如果还要按关键字筛选，可让排序计算属性依赖`filteredTasks.value`：

```ts
const sortedTasks = computed(() =>
  [...filteredTasks.value].sort((a, b) => a.id - b.id),
)
```

执行关系由实际读取的依赖形成，不需要watch把筛选结果复制到另一个ref。

## 5. computed、普通函数与普通变量

| 写法 | 适合用途 | 是否跟踪响应式依赖 | 是否缓存结果 |
| --- | --- | --- | --- |
| `computed()` | 根据状态得到一个值 | 是 | 是，依赖未变时复用结果 |
| 普通函数 | 每次调用都要重新执行的处理 | 由调用位置决定 | 否 |
| 普通变量 | 固定值或一次性结果 | 否 | 不适用 |

不要为了“有缓存”而把所有函数改成computed。只有结果要在模板或其他计算中作为响应式值持续使用时才适合。

### 5.1 可写computed（会读即可）

项目中偶尔会看到同时提供`get`和`set`的计算属性：

```ts
const fullName = computed({
  get: () => `${firstName.value} ${lastName.value}`,
  set: value => {
    const [first, last = ''] = value.split(' ')
    firstName.value = first
    lastName.value = last
  },
})
```

它允许给`fullName.value`赋值，再由setter修改真正的原始状态。新人主线优先使用只读computed和明确的修改函数，只有读写映射确实稳定时才使用可写computed。

## 6. 常见错误与项目注意事项

- 手动维护可计算的计数，导致列表与计数不同步。
- 在computed中`push()`、赋值、请求API或写`localStorage`。
- 对原数组直接`sort()`，导致其他组件看到的顺序也变化。
- 把无筛选结果误写成“系统中没有任何任务”；应区分原始空列表与筛选为空。

## 7. 练习

1. 在第6章任务列表上增加全部、完成、未完成三个数量。
2. 增加关键字和完成状态筛选，并显示无匹配结果提示。
3. 增加按编号排序的计算属性，确认原数组顺序没有变化。
4. 故意手动保存`remainingCount`并遗漏一次同步，再改为computed，记录修正前后的现象。
5. 增加完成状态筛选，并让排序结果依赖筛选结果。
6. 在getter中加入一次`console.log()`，多次读取结果并修改依赖，观察缓存什么时候重新计算。

## 本章检查点

- [ ] 能解释原始状态和派生状态的区别。
- [ ] 能独立编写统计、筛选和排序计算属性。
- [ ] 能说明为什么不重复保存可以计算出来的数据。
- [ ] 能判断普通函数、普通变量与computed的使用场景。
- [ ] 能发现computed中的副作用和原数组修改问题。




