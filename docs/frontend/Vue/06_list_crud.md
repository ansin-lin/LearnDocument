# 第 6 章 列表渲染与CRUD

## 本章目标

- 【必须掌握】使用`v-for`和稳定`:key`渲染任务列表。
- 【必须掌握】通过`push()`、`find()`、属性赋值和`filter()`完成新增、修改、删除。
- 【必须掌握】区分有数据和空列表状态。

## 前置知识

需要掌握`ref`数组、事件绑定、属性绑定、`v-if`以及JavaScript数组方法。

## 1. 为什么需要列表渲染

手写多个`<li>`会让页面结构与任务数据重复。任务增加、删除或排序后，还要手动同步DOM。`v-for`让Vue根据数组生成重复结构。

## 2. 最小示例

```vue
<script setup lang="ts">
import { ref } from 'vue'
import type { Task } from './types/task'

const tasks = ref<Task[]>([
  { id: 101, title: 'API仕様確認', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '一覧画面実装', assignee: '佐藤', priority: 'high', status: 'doing', dueDate: '2026-10-05' },
])
</script>

<template>
  <ul>
    <li v-for="task in tasks" :key="task.id">
      {{ task.title }}
    </li>
  </ul>
</template>
```

`task in tasks`表示依次取得数组元素；`task`只在当前循环模板中使用。需要序号时可写`(task, index) in tasks`，`index`从0开始。

### 2.1 同时取得元素和下标

```vue
<li v-for="(task, index) in tasks" :key="task.id">
  {{ index + 1 }}. {{ task.title }}
</li>
```

- `task`是当前遍历到的任务对象；
- `index`是它在当前数组中的下标，从`0`开始；
- `index + 1`只用于显示行号，不作为任务身份。

删除或排序以后，同一任务的`index`可能变化，因此业务操作仍然传递`task.id`。

## 3. key为什么必须稳定

`:key`帮助Vue识别“更新前后是不是同一条任务”。新增、删除和排序时，Vue会据此复用正确的DOM与输入状态。

```vue
<li v-for="task in tasks" :key="task.id">...</li>
```

业务ID在任务生命周期内稳定，适合作为key。数组下标会在删除或排序后改变，可能让输入框状态、焦点或组件内部状态错误地留在另一行。只有不会增删、不会排序的纯静态列表才可谨慎使用下标。

例如删除第一行后，原来的第二行会移动到下标`0`。如果使用`:key="index"`，Vue可能把旧的第0行DOM继续复用于新的第0行；行内输入值、焦点或子组件状态就可能跟错任务。使用`:key="task.id"`时，Vue能确认移动的是同一条业务数据。

```text
删除前：key 101 → 规格确认，key 102 → 页面实现
删除后：             key 102 → 页面实现

稳定ID：Vue知道102仍是原来的任务
数组下标：新的第0行可能被当成旧的第0行
```

## 4. WorkHub列表CRUD

```vue
<script setup lang="ts">
import { ref } from 'vue'
import type { Task } from './types/task'

let nextId = 103
const newTitle = ref('')
const tasks = ref<Task[]>([
  { id: 101, title: 'API仕様確認', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '一覧画面実装', assignee: '佐藤', priority: 'high', status: 'done', dueDate: '2026-10-05' },
])

function addTask() {
  const title = newTitle.value.trim()
  if (!title) return

  tasks.value.push({
    id: nextId++,
    title,
    assignee: '',
    priority: 'normal',
    status: 'todo',
    dueDate: '',
  })
  newTitle.value = ''
}

function updateNewTitle(event: Event): void {
  const input = event.currentTarget as HTMLInputElement
  newTitle.value = input.value
}

function toggleTask(id: number): void {
  const task = tasks.value.find(item => item.id === id)
  if (task) task.status = task.status === 'done' ? 'todo' : 'done'
}

function renameTask(id: number, title: string): void {
  const task = tasks.value.find(item => item.id === id)
  if (task && title.trim()) task.title = title.trim()
}

function removeTask(id: number): void {
  tasks.value = tasks.value.filter(item => item.id !== id)
}
</script>

<template>
  <form @submit.prevent="addTask">
    <label for="new-title">任务标题</label>
    <input
      id="new-title"
      :value="newTitle"
      @input="updateNewTitle"
    >
    <button type="submit">新增</button>
  </form>

  <p v-if="tasks.length === 0">任务尚未登记。</p>
  <ul v-else>
    <li v-for="task in tasks" :key="task.id">
      <button type="button" @click="toggleTask(task.id)">
        {{ task.status === 'done' ? '恢复' : '完成' }}
      </button>
      <span :class="{ 'is-completed': task.status === 'done' }">{{ task.title }}</span>
      <button type="button" @click="removeTask(task.id)">删除</button>
    </li>
  </ul>
</template>
```

- `push()`在数组末尾新增任务。
- `find()`取得第一条符合条件的任务；找不到时返回`undefined`，所以先判断。
- 修改任务对象属性会触发界面更新。
- `filter()`返回不包含指定任务的新数组，再整体赋给`tasks.value`。

这里使用第4章学过的`:value`和`@input`保持输入状态同步，没有提前使用第8章的`v-model`。第8章会把这两部分简化为`v-model`。

### 4.1 四种修改方式分别改变什么

```text
新增：push()改变原数组内容
查找：find()返回原数组中符合条件的对象或undefined
修改：给找到的对象属性重新赋值
删除：filter()创建新数组，再替换tasks.value
```

`push()`和对象属性赋值属于在原数组、原对象上修改；`filter()`不会删除原数组内容，而是返回新数组，所以必须接收结果。两种方式都能被Vue跟踪，选择时应先符合JavaScript方法的真实行为。

### 4.2 根据ID修改一条任务

修改过程不要省略“查找失败”的分支：

```ts
function renameTask(id: number, nextTitle: string): boolean {
  const task = tasks.value.find(item => item.id === id)

  if (!task) {
    console.warn(`任务不存在：${id}`)
    return false
  }

  const title = nextTitle.trim()
  if (!title) return false

  task.title = title
  return true
}
```

返回布尔值可以让调用方判断修改是否成功。真实项目还要根据规格决定“不存在”时显示错误、重新读取还是返回列表。

### 4.3 删除确认与处理中状态

删除属于不可轻易恢复的操作。是否弹出确认框、是否允许撤销，由画面规格决定。异步删除时应记录正在处理的ID，避免同一行重复执行：

```ts
const deletingId = ref<number | null>(null)

async function removeTask(id: number): Promise<void> {
  if (deletingId.value !== null) return

  deletingId.value = id
  try {
    // 后续HTTP章节会在这里调用删除API
    tasks.value = tasks.value.filter(task => task.id !== id)
  } finally {
    deletingId.value = null
  }
}
```

当前只认识状态设计，不发送请求。模板可以用`:disabled="deletingId === task.id"`禁用正在删除的那一行。

## 5. template循环与条件的位置

多个相邻元素需要一起循环时可以使用不生成额外DOM的`<template v-for>`。不要把`v-if`和`v-for`写在同一个元素上；需要过滤时，第7章使用computed先得到目标数组。

```vue
<template v-for="task in tasks" :key="task.id">
  <h3>{{ task.title }}</h3>
  <p>{{ task.status === 'done' ? '完成' : '未完成' }}</p>
</template>
```

`<template>`本身不会生成DOM节点，`:key`写在`<template v-for>`上。

### 5.1 原始空列表和筛选为空不是一回事

- `tasks.length === 0`：系统当前没有任务；
- `filteredTasks.length === 0`：系统可能有任务，但没有符合查询条件的结果。

第6章先处理原始空列表，第7章增加筛选后要使用不同提示，避免用户误以为数据被删除。

## 6. 常见错误与项目注意事项

- 漏写`:key`或使用会变化的下标。
- 写`tasks.push()`而忘记脚本中的`tasks.value`。
- `find()`可能返回`undefined`却直接修改属性。
- 调用`filter()`但没有接收返回的新数组。
- 删除前没有按规格确认，或只从页面隐藏却没有更新真实状态。
- 把接口分页的全部数据一次加载到浏览器；大列表应遵守后端分页规格。

## 7. 练习

1. 渲染至少3条任务，并显示编号、标题和完成状态。
2. 实现新增、切换完成状态和删除。
3. 删除中间一条任务，确认其他行的DOM状态没有串行。
4. 清空数组并显示空列表提示。
5. 实现`renameTask()`，分别验证存在和不存在的ID。
6. 暂时把`:key`改为`index`，在每行放一个输入框后删除第一行，观察DOM复用风险，再恢复业务ID。
7. 增加`deletingId`，确认处理中只能禁用对应任务的删除按钮。

## 本章检查点

- [ ] 能独立使用`v-for`渲染数组。
- [ ] 能解释稳定业务ID为什么比index更适合key。
- [ ] 能使用数组方法完成基本CRUD。
- [ ] 能安全处理`find()`未找到数据的情况。
- [ ] 能实现并验证空列表状态。




