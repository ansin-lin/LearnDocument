# 第 11 章 Props与Emits：建立单向数据流

## 本章目标与前置知识

【必须掌握】使用`defineProps()`和`defineEmits()`建立单向数据流，并能判断状态应该保存在父组件还是子组件。需要掌握第10章的父子组件关系。本章使用JavaScript运行时声明，重点理解组件职责和数据方向。

第10章已经把页面拆成多个组件，但各组件只能使用自己的局部数据。本章学习父子组件最重要的通信方式：父组件通过Props把数据传给子组件，子组件通过自定义事件把用户操作通知父组件。

```text
父组件状态 ──Props──> 子组件显示
父组件函数 <──事件──── 子组件操作
```

这个方向称为单向数据流。状态由一个明确位置拥有，修改入口也更容易调查和测试。

## 1. 先理解当前问题

`TaskItem.vue`需要显示任务，但下面的变量只存在于父组件：

```vue
<!-- App.vue -->
<script setup>
import { ref } from 'vue'
import TaskItem from './components/TaskItem.vue'

const task = ref({
  id: 101,
  title: '规格确认',
  assignee: '田中',
  priority: 'normal',
  status: 'todo',
  dueDate: '2026-09-30',
})
</script>

<template>
  <TaskItem />
</template>
```

子组件不会自动读取父组件的`task`。组件具有独立作用域，必须明确声明并传递公开输入。

## 2. 使用defineProps声明输入

### 2.1 声明第一个Prop

先只解决一个问题：让子组件声明自己需要接收一个名为`title`的数据。

`src/components/TaskItem.vue`：

```vue
<script setup>
const props = defineProps({
  title: { type: String, required: true },
})
</script>

<template>
  <li>{{ props.title }}</li>
</template>
```

`defineProps()`声明组件允许接收的Prop。这里的`type: String`表示运行时期望字符串，`required: true`表示父组件必须传入。

`defineProps()`会返回Props对象，所以：

```js
const props = defineProps({
  title: { type: String, required: true },
})
```

可以拆成下面两点理解：

1. 对象的键声明允许接收的Prop名称，`type`和`required`说明运行时要求。
2. `props.title`读取父组件实际传入的值。

`defineProps()`是`script setup`编译宏，不需要从`vue`导入。模板也可以直接写`{{ title }}`，但本课程优先写`props.title`，让数据来自父组件这一点更明显。

`required: true`表示必填；没有设置时表示可以省略。单向数据流不会因声明方式改变。

### 2.2 可选Prop与默认值

可选Prop可以在运行时声明中使用`default`提供默认值：

```vue
<script setup>
const props = defineProps({
  title: { type: String, required: true },
  readonly: { type: Boolean, default: false },
})
</script>

<template>
  <li>{{ props.title }}</li>
</template>
```

这段对象写法说明：

- Prop名称是`title`；
- 运行时期待字符串；
- 父组件必须传入。

可以把两种写法对比为：

```js
// 基础写法：只声明名称
const props = defineProps(['title'])

// 对象简写：声明名称和运行时类型
const props = defineProps({
  title: String,
})

// 完整对象写法：声明类型和更多选项
const props = defineProps({
  title: {
    type: String,
    required: true,
  },
})
```

学习顺序是“数组名称 → 对象类型 → 完整选项”。正常业务项目建议使用对象写法，让组件需要什么数据更清楚。`type`、`required`、`default`和`validator`会在第5～7节分别展开。

## 3. 父组件传递Props

```vue
<script setup>
import TaskItem from './components/TaskItem.vue'

const taskTitle = '规格确认'
</script>

<template>
  <TaskItem :title="taskTitle" />
</template>
```

`:title="taskTitle"`把JavaScript变量传给子组件。如果传固定字符串，可以不写冒号：

```vue
<TaskItem title="规格确认" />
```

下面两种写法结果不同：

```vue
<TaskItem task-id="101" />
<TaskItem :task-id="101" />
```

- 第一行传入字符串`'101'`。
- 第二行传入数字`101`。

布尔值、数字、数组和对象等非固定字符串通常使用`v-bind`。

## 4. Prop名称的写法

JavaScript中使用camelCase，模板属性通常使用kebab-case：

```vue
<script setup>
const props = defineProps({
  taskId: Number,
  displayName: String,
})
</script>
```

父组件：

```vue
<TaskItem :task-id="101" display-name="规格确认" />
```

在单文件组件模板中camelCase通常也能工作，但团队项目应选择一种一致写法。组件标签使用PascalCase，模板Prop属性使用kebab-case是常见组合。

## 5. 常用运行时Props类型

```js
const props = defineProps({
  title: String,
  taskId: Number,
  status: String,
  tags: Array,
  task: Object,
  formatter: Function,
})
```

常见构造函数包括`String`、`Number`、`Boolean`、`Array`、`Object`和`Function`。这是开发期运行时检查，不会自动把任意错误数据转换为正确类型，也不能替代接口数据校验。

业务组件不应接收大量互相无关的Prop。如果组件需要十几个控制开关，应检查职责是否过大或数据结构是否需要整理。

## 6. required和default

```js
const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  emptyMessage: {
    type: String,
    default: '没有数据',
  },
})
```

- `required: true`表示父组件必须提供。
- `default`用于父组件未传值时的默认值。
- 必填Prop通常不再提供默认值，否则“必须传入”的意义会变得不清楚。

数组和对象默认值应通过函数创建，避免多个组件实例共享同一个对象：

```js
const props = defineProps({
  tags: {
    type: Array,
    default: () => [],
  },
  options: {
    type: Object,
    default: () => ({ showStatus: true }),
  },
})
```

## 7. 会使用、能看懂：validator

```js
const props = defineProps({
  priority: {
    type: String,
    required: true,
    validator: (value) => ['low', 'normal', 'high'].includes(value),
  },
})
```

`validator`返回`true`表示值符合组件要求。它适合少量固定候选值，能在开发环境帮助发现传值错误。

校验器不是安全边界，也不能代替第17章的接口字段检查。复杂业务规则不要全部塞进组件Prop校验器。

## 8. 传递一个业务对象

任务项需要多个相关字段时，通常传递一个任务对象：

```vue
<!-- 父组件 -->
<TaskItem :task="task" />
```

```vue
<!-- TaskItem.vue -->
<script setup>

const props = defineProps({
  task: { type: Object, required: true },
})
</script>

<template>
  <li>
    <span>{{ props.task.title }}</span>
    <span>{{ props.task.status === 'done' ? '完成' : '未完成' }}</span>
  </li>
</template>
```

父组件继续传入字段完整的WorkHub任务对象。`type: Object`只能做基础运行时检查，接口数据仍需第17章的字段校验。

## 9. Props是只读输入

下面的写法错误：

```js
props.title = '新标题'
```

Props由父组件提供，子组件不能给Prop重新赋值。否则数据来源和修改责任会变得不清楚，而且父组件更新时可能覆盖子组件改动。

对象Prop还要注意嵌套修改：

```js
// 技术上可能修改到父级对象，但业务组件不应这样做
props.task.status = 'done'
```

Vue只能直接阻止给Prop本身赋值，难以完全阻止对象内部修改。子组件仍应把Props视为只读：显示数据或发送操作意图，不直接修改父级对象。

## 10. 根据Prop得到显示结果

子组件可以基于Prop创建计算属性：

```vue
<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
})

const statusLabel = computed(() =>
  props.status === 'done' ? '完成' : '未完成',
)
</script>

<template>
  <span>{{ statusLabel }}</span>
</template>
```

这不是复制Prop，而是从Prop派生显示值。不要为了方便写`const localCompleted = ref(props.status === 'done')`后长期分别维护；那只取得初始值，后续父级变化可能无法正确同步。

## 11. 为什么需要组件事件

任务项中有“设为完成”按钮，但任务状态属于父组件。子组件不能直接修改Prop，因此发送一个事件，表达用户做了什么：

```text
子组件：用户点击了任务101的完成按钮
父组件：收到事件后决定怎样修改任务数据
```

组件事件表达操作意图，不负责规定父级数据存放在哪里。

## 12. 使用defineEmits声明事件

`TaskItem.vue`：

```vue
<script setup>

const props = defineProps({
  task: { type: Object, required: true },
})

const emit = defineEmits(['remove', 'changeStatus'])

function requestComplete() {
  emit('changeStatus', props.task.id, 'done')
}

function requestRemove() {
  emit('remove', props.task.id)
}
</script>

<template>
  <li>
    <span>{{ props.task.title }}</span>
    <button type="button" @click="requestComplete">设为完成</button>
    <button type="button" @click="requestRemove">删除</button>
  </li>
</template>
```

`defineEmits()`也是编译宏，不需要导入。数组声明组件会发出的事件名称。它返回`emit`函数：

```text
emit(事件名称, 参数1, 参数2, ...)
```

第一个参数是事件名，后续参数是交给父组件的数据。

模板中直接写`@click="emit('remove', props.task.id)"`也能运行，但命名函数在逻辑增加时更容易维护和测试。

## 13. 事件名称怎样设计

事件名应该表达已经发生的动作或请求，例如：

- `save`
- `remove`
- `select`
- `complete`
- `change-status`

避免使用`click`作为业务组件事件名，因为父组件无法判断点击代表什么。基础按钮封装可以保留`click`语义，任务组件则使用业务事件。

模板监听器推荐使用kebab-case：

```vue
<TaskItem @change-status="changeTaskStatus" />
```

脚本中声明和发出事件统一使用`changeStatus`，父组件模板使用对应的kebab-case监听器`@change-status`。不要在不同组件中任意交换单词顺序。

## 14. 父组件接收事件

```vue
<script setup>
import { ref } from 'vue'
import TaskItem from './components/TaskItem.vue'

const tasks = ref([
  { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'todo', dueDate: '2026-10-05' },
])

function changeTaskStatus(id, status) {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return
  task.status = status
}

function removeTask(id) {
  tasks.value = tasks.value.filter((item) => item.id !== id)
}
</script>

<template>
  <ul>
    <TaskItem
      v-for="task in tasks"
      :key="task.id"
      :task="task"
      @change-status="changeTaskStatus"
      @remove="removeTask"
    />
  </ul>
</template>
```

`:task="task"`向下传数据，`@change-status`和`@remove`向上接收事件。父组件拥有`tasks`，所以由父组件完成查找、修改和删除。

## 15. 父组件监听时怎样接收参数

子组件发送：

```js
emit('remove', props.task.id)
```

父组件可以把函数直接作为监听器：

```vue
<TaskItem @remove="removeTask" />
```

Vue会把事件参数交给`removeTask(id)`。如果父级还要补充自己的参数，可以使用`$event`代表第一个事件参数：

```vue
<TaskItem @remove="recordAndRemove('task-list', $event)" />
```

事件参数应保持稳定的名称、顺序和含义。本章的状态变更事件统一发送任务编号和新状态：

```js
emit('changeStatus', props.task.id, 'done')
```

不要在不同子组件中把同一事件临时改成另一种参数结构。

## 16. 为事件参数增加运行时校验

```js
const emit = defineEmits({
  remove: (id) => Number.isInteger(id) && id > 0,
  changeStatus: (id, status) => {
    return Number.isInteger(id)
      && ['todo', 'doing', 'done'].includes(status)
  },
})
```

对象形式可以在开发阶段检查事件参数，返回`true`表示有效。它帮助发现组件内部发错参数，但不能代替父级业务判断或服务端校验。

前面的数组形式适合先理解事件通信；对象形式可以进一步增加运行时参数检查。

## 17. 组件事件不会自动冒泡

DOM的`click`事件可以向祖先元素冒泡，但组件自定义事件只会交给直接父组件：

```text
TaskItem --remove--> TaskList
TaskList --不会自动继续--> TaskListView
```

如果页面组件也需要知道，`TaskList`要明确再次发送事件：

```js
const emit = defineEmits(['remove'])

function handleRemove(id) {
  emit('remove', id)
}
```

通信需要连续跨越很多层时，先检查组件边界；跨页面共享业务状态到第18章再使用Pinia。不要因为传递一两层Props就立即使用全局状态。

## 18. 状态应该放在哪里

学完Props和组件事件后，才能根据数据流判断状态归属。基本原则是：状态放在真正拥有它、负责修改它，并能把结果传给需要组件的最近位置。

| 状态 | 常见位置 |
| --- | --- |
| 只影响一个组件的展开开关 | 该组件内部 |
| 一张表单的输入值 | 表单组件或负责提交的父组件 |
| 列表和统计组件共同使用的任务数组 | 最近的共同父组件 |
| 多个路由页面共享的业务数据 | 第18章评估Pinia |

不要在父子组件各复制一份相同业务状态，也不要因为传递一两层Props就立即使用全局Store。

## 19. 状态提升

如果两个兄弟组件需要使用同一份状态，通常把状态放到最近的共同父组件：

```text
TaskListView（拥有tasks）
├─ TaskSummary（通过Props读取）
└─ TaskList（通过Props读取，通过事件请求修改）
```

这种做法称为状态提升。父级作为唯一数据来源，两个子组件不会分别保存互相矛盾的任务数组。

## 20. Props过多时怎样判断

一个组件接收多个Prop不一定错误，但应检查它们是否属于同一职责：

- 多个字段共同描述一条任务，可以传`task`对象。
- 多个显示开关没有明确关系，可能说明组件职责过大。
- 为了避免传值而把所有内容放全局，会让依赖更加隐藏。
- 不要仅为减少Prop数量就传递包含大量无关字段的整个页面对象。

接口对象、页面状态和组件显示模型不一定完全相同。实际项目应按职责决定组件最小输入。

## 21. DOM事件与组件事件的区别

| 对比 | DOM事件 | 组件事件 |
| --- | --- | --- |
| 来源 | 浏览器元素 | 子组件调用`emit()` |
| 示例 | `click`、`input` | `remove`、`save` |
| 是否冒泡 | 通常可以 | 不会自动跨组件层级 |
| 参数 | 原生Event对象 | 组件定义的数据 |
| 监听 | `@click` | `@remove` |

如果组件没有在`emits`中声明某个监听器，Vue可能把它当作Attributes继续传到根元素。第12章学习Attributes后会进一步理解这个边界。因此组件公开事件应该明确声明。

## 22. 完整子组件示例

`src/components/TaskItem.vue`：

```vue
<script setup>
import { computed } from 'vue'

const props = defineProps({
  task: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['changeStatus', 'remove'])

const statusLabel = computed(() =>
  props.task.status === 'done' ? '完成' : '未完成',
)

function requestComplete() {
  emit('changeStatus', props.task.id, 'done')
}

function requestRemove() {
  emit('remove', props.task.id)
}
</script>

<template>
  <li :class="{ 'is-completed': props.task.status === 'done' }">
    <span>{{ props.task.title }}</span>
    <span>{{ statusLabel }}</span>
    <button
      type="button"
      :disabled="props.readonly || props.task.status === 'done'"
      @click="requestComplete"
    >
      设为完成
    </button>
    <button
      type="button"
      :disabled="props.readonly"
      @click="requestRemove"
    >
      删除
    </button>
  </li>
</template>

<style scoped>
.is-completed {
  color: #6b7280;
  text-decoration: line-through;
}
</style>
```

子组件只负责显示任务和报告操作，不知道任务来自本地数组、API还是Pinia，也不直接修改任务对象。

## 23. 完整父组件示例

```vue
<script setup>
import { computed, ref } from 'vue'
import TaskItem from './components/TaskItem.vue'

const tasks = ref([
  { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
  { id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'done', dueDate: '2026-10-05' },
])
const processingId = ref(null)

const remainingCount = computed(() =>
  tasks.value.filter((task) => task.status !== 'done').length,
)

function changeTaskStatus(id, status) {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return
  task.status = status
}

function removeTask(id) {
  tasks.value = tasks.value.filter((item) => item.id !== id)
}
</script>

<template>
  <main>
    <h1>任务一览</h1>
    <p>未完成：{{ remainingCount }}件</p>

    <p v-if="tasks.length === 0">当前没有任务</p>
    <ul v-else>
      <TaskItem
        v-for="task in tasks"
        :key="task.id"
        :task="task"
        :readonly="processingId === task.id"
        @change-status="changeTaskStatus"
        @remove="removeTask"
      />
    </ul>
  </main>
</template>
```

父组件管理任务数组和修改函数。`processingId`暂时没有异步赋值，只用于展示“父组件可以根据自身状态控制子组件”的设计；第17章请求时再完整处理。

## 24. 常见错误

### 24.1 漏写动态绑定冒号

```vue
<!-- 传入字符串 -->
<TaskItem task-id="101" />

<!-- 传入数字 -->
<TaskItem :task-id="101" />
```

### 24.2 直接修改Prop

子组件不能通过`props.task.status = 'done'`代替事件。让拥有状态的父组件修改。

### 24.3 父子事件名称不一致

子组件`emit('remove', id)`，父组件必须监听`@remove`。浏览器控制台没有错误时也要检查字符串是否一致。

### 24.4 把组件事件当成DOM冒泡

祖父组件不会自动收到孙组件事件。需要明确逐层转发、重新调整组件边界，或在真正跨页面共享时使用Store。

### 24.5 用局部ref复制Prop

`ref(props.task)`不是自动双向同步方案。先确认是只读显示、可取消编辑副本，还是父级共同状态，再选择设计。

## 25. 练习

1. 为`TaskItem`声明字符串、数字、布尔值和对象Prop。
2. 为可选Prop设置默认值，为数组和对象使用默认值函数。
3. 为优先级增加运行时`validator`，故意传入错误值并观察警告。
4. 验证不带冒号的`task-id="101"`与`:task-id="101"`类型不同。
5. 子组件发出完成和删除事件，父组件查找任务后修改。
6. 将事件参数从编号改为包含编号和状态的对象，并保持统一字段。
7. 建立`TaskSummary`和`TaskList`两个兄弟组件，把共享任务状态提升到共同父级。
8. 增加一层`TaskList`组件，验证自定义事件不会自动传到页面组件，再明确转发。
9. 用Vue DevTools确认父组件状态、子组件Props和事件。
10. 写出本次组件改修的输入、输出、影响页面和回归项目。

## 本章检查点

- 能说明Props向下、事件向上的单向数据流。
- 能使用`defineProps()`声明类型、必填、默认值和校验器。
- 能正确传递固定字符串和动态JavaScript值。
- 知道数组和对象默认值需要函数。
- 把Props及其对象内容视为只读输入。
- 能使用`defineEmits()`和`emit()`声明并发送业务事件。
- 能设计明确的事件名称和参数结构。
- 能在父组件中接收事件并修改自己拥有的状态。
- 知道组件事件不会自动跨层冒泡。
- 能通过状态提升让兄弟组件共享唯一数据来源。
- 能区分DOM事件与组件自定义事件。
- 知道运行时Props检查和事件校验不能替代接口校验。






