# 第 13 章 Slots与provide/inject

插槽让父组件提供结构内容，provide/inject让同一组件树中的深层成员共享依赖。两者解决不同问题，都不能代替应用级状态管理。

## 本章目标与前置知识

- 【必须掌握】使用默认插槽和具名插槽组织可复用结构。
- 【会使用、能看懂】使用作用域插槽让父组件决定数据怎样显示。
- 【会使用、能看懂】使用 provide/inject 在当前组件树共享依赖。
- 【会读即可】识别 useSlots() 和 Symbol 注入键。

需要掌握组件、Props、Emits和TypeScript基础。

## 1. 什么是插槽

Props适合传数据，插槽适合让父组件提供一段模板内容。子组件使用`<slot>`声明内容位置。

`BaseCard.vue`：

```vue
<template>
  <section class="base-card">
    <slot />
  </section>
</template>
```

父组件：

```vue
<BaseCard>
  <h2>任务信息</h2>
  <p>担当：田中</p>
</BaseCard>
```

父组件标签之间的内容会进入默认插槽。内容结构由父组件编写，外层卡片结构由子组件提供。

## 2. 插槽后备内容

```vue
<button type="button">
  <slot>确定</slot>
</button>
```

父组件没有提供插槽内容时显示“确定”；提供内容时替换后备内容：

```vue
<BaseButton>保存任务</BaseButton>
```

后备内容适合合理默认文案，但重要业务文字是否允许省略仍应由组件规格决定。

## 3. 具名插槽

一个组件有多个内容位置时使用具名插槽：

```vue
<!-- BaseCard.vue -->
<template>
  <section class="base-card">
    <header><slot name="title" /></header>
    <div><slot /></div>
    <footer><slot name="actions" /></footer>
  </section>
</template>
```

父组件使用`v-slot`简写`#`：

```vue
<BaseCard>
  <template #title>
    <h2>任务信息</h2>
  </template>

  <p>担当：田中</p>

  <template #actions>
    <button type="button">编辑</button>
  </template>
</BaseCard>
```

没有名称的内容进入默认插槽。插槽名称应表达布局职责，例如`title`、`actions`，不要使用`top2`等与结构强绑定的模糊名称。

## 4. 插槽内容使用谁的数据

父组件提供的插槽内容默认使用父组件作用域中的数据：

```vue
<BaseCard>
  <p>{{ selectedTask.title }}</p>
</BaseCard>
```

即使这段HTML最终显示在`BaseCard`内部，它仍由父组件编写，因此不能直接读取`BaseCard`内部变量。

记忆方式是：模板写在哪个组件文件中，就默认使用哪个组件的作用域。

## 5. 作用域插槽

普通插槽只能使用父组件自己的数据。如果父组件要定制子组件循环中的“当前一行”，子组件必须把当前数据通过插槽Prop交出来。

一个常见场景是通用任务表格：子组件统一负责表格列和行结构，但每个页面需要的操作按钮不同。

```vue
<!-- TaskTable.vue -->
<script setup lang="ts">
import type { Task } from './types/task'

const props = defineProps<{
  tasks: Task[]
}>()
</script>

<template>
  <table>
    <thead>
      <tr>
        <th scope="col">编号</th>
        <th scope="col">任务名称</th>
        <th scope="col">状态</th>
        <th scope="col">操作</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="task in props.tasks" :key="task.id">
        <td>{{ task.id }}</td>
        <td>{{ task.title }}</td>
        <td>{{ task.status === 'done' ? '完成' : '未完成' }}</td>
        <td>
          <slot name="actions" :task="task">
            <span>—</span>
          </slot>
        </td>
      </tr>
    </tbody>
  </table>
</template>
```

`TaskTable`知道当前正在渲染哪条任务，所以它在操作列中写`:task="task"`，把当前任务作为名为`task`的插槽Prop提供给父组件。

父组件接收这个值，并决定当前页面显示哪些操作：

```vue
<script setup lang="ts">
import { ref } from 'vue'
import TaskTable from './components/TaskTable.vue'
import type { Task } from './types/task'

const tasks = ref<Task[]>([
  { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'done', dueDate: '2026-10-05' },
])

function editTask(id: number) {
  console.log('编辑任务：', id)
}

function removeTask(id: number) {
  tasks.value = tasks.value.filter((task) => task.id !== id)
}
</script>

<template>
  <TaskTable :tasks="tasks">
    <template #actions="{ task }">
      <button type="button" @click="editTask(task.id)">编辑</button>
      <button type="button" @click="removeTask(task.id)">删除</button>
    </template>
  </TaskTable>
</template>
```

这里的对应关系是：

```text
子组件：<slot name="actions" :task="task">
                              ↓
父组件：<template #actions="{ task }">
```

- `actions`是插槽名称。
- 子组件的`:task="task"`提供当前行数据。
- 父组件的`{ task }`接收当前行数据。
- 编辑、删除函数属于父组件，因此可以直接使用父组件作用域中的函数。
- 父组件没有提供`actions`插槽时，表格显示后备内容`—`。

这个例子中作用域插槽有明确意义：表格结构保持统一，不同页面可以定制操作列。作用域插槽常见于表格、列表和UI组件库，但会增加阅读难度；如果每个页面的行结构和操作完全相同，直接使用普通业务组件会更清楚。

## 6. 会读即可：useSlots

```vue
<script setup lang="ts">
import { useSlots } from 'vue'

const slots = useSlots()
</script>

<template>
  <header v-if="slots.title">
    <slot name="title" />
  </header>
  <slot />
</template>
```

`useSlots()`让脚本或模板判断具名插槽是否存在。只需要在模板中判断时，也常见`$slots.title`。基础容器不需要无目的检查全部插槽。

## 7. provide与inject：组件树局部共享

Props适合直接父子通信。但当祖先组件需要向较深的后代提供同一项数据时，如果每一层都只为继续转交而声明Props，就会形成逐层传递。

```text
TaskPage
  └─ TaskLayout（不使用显示模式，只负责转交）
       └─ TaskToolbar（真正使用显示模式）
```

`provide()`让祖先组件提供数据，`inject()`让任意层级的后代取得数据，中间组件不需要接收和转交。

### 7.1 provide提供数据

`TaskPage.vue`：

```vue
<script setup lang="ts">
import { provide, readonly, ref, type Ref } from 'vue'
import TaskLayout from './TaskLayout.vue'

type DisplayMode = 'list' | 'card'

interface TaskDisplayContext {
  displayMode: Readonly<Ref<DisplayMode>>
  changeDisplayMode: (mode: DisplayMode) => void
}

const displayMode = ref<DisplayMode>('list')

function changeDisplayMode(mode: DisplayMode): void {
  displayMode.value = mode
}

provide<TaskDisplayContext>('taskDisplay', {
  displayMode: readonly(displayMode),
  changeDisplayMode,
})
</script>

<template>
  <TaskLayout />
</template>
```

`provide(key, value)`把值提供给当前组件的后代。第一个参数是查找键，第二个参数是提供的数据。`readonly()`返回只读的响应式引用，后代通过`changeDisplayMode()`请求提供方修改状态。

中间组件只负责组织界面，不需要声明显示模式Prop：

```vue
<!-- TaskLayout.vue -->
<script setup lang="ts">
import TaskToolbar from './TaskToolbar.vue'
</script>

<template>
  <section>
    <h2>任务显示设置</h2>
    <TaskToolbar />
  </section>
</template>
```

### 7.2 inject取得数据

`TaskToolbar.vue`：

```vue
<script setup lang="ts">
import { inject, type Ref } from 'vue'

type DisplayMode = 'list' | 'card'

interface TaskDisplayContext {
  displayMode: Readonly<Ref<DisplayMode>>
  changeDisplayMode: (mode: DisplayMode) => void
}

const taskDisplay = inject<TaskDisplayContext>('taskDisplay')
</script>

<template>
  <div v-if="taskDisplay">
    <p>当前显示：{{ taskDisplay.displayMode }}</p>
    <button type="button" @click="taskDisplay.changeDisplayMode('list')">列表</button>
    <button type="button" @click="taskDisplay.changeDisplayMode('card')">卡片</button>
  </div>
  <p v-else role="alert">无法取得显示模式。</p>
</template>
```

`inject(key)`从最近的同名提供者取得值。组件也可能被放在没有提供者的位置，因此返回值可能是`undefined`，示例先确认后再使用。

### 7.3 默认值和响应性

缺少提供者时仍能正常工作的内容，可以设置默认值：

```ts
const displayMode = inject('displayMode', 'list')
```

祖先提供`ref`或`reactive`数据时，后代取得的仍是响应式数据。必须存在的业务依赖不应通过随意默认值隐藏结构错误；共享状态也应由提供者暴露明确的修改函数。

### 7.4 会读即可：使用Symbol作为键

字符串键可能重名。较大的项目会在公共模块导出Symbol：

```ts
// src/keys/taskDisplay.ts
export const taskDisplayKey = Symbol('taskDisplay')
```

提供方和注入方必须导入同一个`taskDisplayKey`，不能分别创建两个Symbol。

### 7.5 使用边界

`provide / inject`适合主题、表单上下文、局部显示设置，或一组深层组件共同依赖的功能。它只在提供者的后代范围内有效，不会自动成为全局状态，也不应替代所有Props。

## 8. 组件通信方式怎样选择

| 需求 | 使用方式 |
| --- | --- |
| 父级提供业务数据 | Props |
| 子级报告保存、删除等动作 | emits |
| 封装输入控件的当前值 | 组件`v-model` |
| 传递`class`、`disabled`、ARIA等原生能力 | Attributes |
| 父级决定一块内容的HTML结构 | slot |
| 子级提供数据、父级决定怎样渲染 | 作用域插槽 |
| 祖先向深层后代提供局部依赖 | `provide / inject` |
| 多个页面共享并统一修改业务状态 | Pinia |

同一个组件可以组合这些方式，但每项都应有清楚契约。不要把业务数据藏进Attributes，也不要把简单字符串全部改成插槽。
## 9. 常见错误

- 在子组件中读取只属于父组件插槽作用域的变量。
- 用作用域插槽传递所有普通Props。
- 把provide/inject当作跨页面全局状态。
- 由inject接收方随意修改提供方状态。

## 10. WorkHub练习与检查点

1. 为业务卡片增加默认插槽和标题具名插槽。
2. 使用作用域插槽让任务列表的调用方决定状态显示。
3. 使用provide/inject向一组深层表单组件提供只读上下文和修改函数。
4. 判断一项状态应使用Props、Emits、插槽、provide/inject还是Pinia。

- [ ] 能选择默认插槽与具名插槽。
- [ ] 能解释作用域插槽的数据方向。
- [ ] 能说明provide/inject只适合组件树局部共享。
- [ ] 能区分provide/inject与Pinia。





