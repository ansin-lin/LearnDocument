# 第 3 章 响应式状态：ref 与 reactive

## 本章目标与前置知识

【必须掌握】解释ref对象与`.value`的关系，使用ref保存基本值、对象和数组，并理解模板自动解包；`reactive`和`toRefs`达到【会使用、能看懂】。需要掌握普通变量、对象、数组与SFC结构。

普通变量发生变化时，Vue不会持续跟踪它，也就无法可靠更新模板。本章先建立响应式状态；第4章再用事件把用户操作与状态修改连接起来。

本章重点掌握`ref()`。`reactive()`需要会使用和阅读，但主线项目优先采用规则更统一的`ref()`。

从本章开始，WorkHub示例统一使用同一套任务字段：`id`、`title`、`assignee`、`priority`、`status`和`dueDate`。`priority`使用`low`、`normal`、`high`，`status`使用`todo`、`doing`、`done`。本阶段使用普通JavaScript对象，不重新使用`completed: boolean`表示同一业务状态。

## 1. 什么是响应式

先看普通JavaScript变量：

```js
let taskCount = 1
taskCount = 2
```

JavaScript知道变量从`1`变成了`2`，但Vue无法仅凭普通赋值判断模板中的哪些位置需要更新。

响应式状态可以理解为Vue正在跟踪的数据：

```text
声明响应式状态
      ↓
模板读取状态并建立依赖
      ↓
程序修改状态
      ↓
Vue更新使用该状态的界面
```

Vue不会重新创建整个网页，而是更新受影响的部分。开发者主要修改数据，不必反复使用`querySelector()`查找元素并手工修改文字。

## 2. 使用ref声明一个响应式值

### 2.1 创建ref

```vue
<script setup>
import { ref } from 'vue'

const taskCount = ref(1)
</script>

<template>
  <p>任务数量：{{ taskCount }}</p>
</template>
```

`ref()`是Vue提供的函数，需要从`vue`导入。`ref(1)`创建一个响应式引用，括号中的`1`是初始值。

这里有两个不同概念：

- `taskCount`是保存响应式关系的ref对象。
- `taskCount.value`才是ref当前保存的实际值`1`。

### 2.2 在JavaScript中读写value

```js
import { ref } from 'vue'

const taskCount = ref(1)

console.log(taskCount.value) // 1

taskCount.value = 2
console.log(taskCount.value) // 2
```

在`script setup`的JavaScript代码中，读取和修改ref都要使用`.value`。如果写成`taskCount = 2`，相当于试图替换`const`变量本身，不是在修改ref保存的值。

### 2.3 在模板中自动解包

```vue
<template>
  <p>{{ taskCount }}</p>
</template>
```

模板会自动取得顶层ref中的值，所以写`taskCount`，不写`taskCount.value`。这种行为叫作**自动解包**。

| 使用位置 | 读取 | 修改 |
| --- | --- | --- |
| `script setup`中的JavaScript | `taskCount.value` | `taskCount.value = 2` |
| `template`模板 | `taskCount` | 后续通过事件或表单修改 |

初学者最常见的错误就是在脚本中漏写`.value`，或者在模板中写了多余的`.value`。

## 3. 观察状态变化后的页面更新

先在脚本中修改ref，观察模板读取的是修改后的状态：

```vue
<script setup>
import { ref } from 'vue'

const statusMessage = ref('等待处理')

function completeTask() {
  statusMessage.value = '处理完成'
}

completeTask()
</script>

<template>
  <p>{{ statusMessage }}</p>
</template>
```

组件初始化时函数把`statusMessage.value`改为“处理完成”，模板显示修改后的值。第4章再把同一个函数交给按钮点击事件。

## 4. ref可以保存哪些数据

`ref()`不仅能保存数字，也能保存JavaScript中的各种值：

```js
import { ref } from 'vue'

const title = ref('规格确认')
const taskCount = ref(3)
const status = ref('todo')
const selectedId = ref(null)
const tasks = ref([])
const currentTask = ref(null)
```

| 初始值 | 当前含义 |
| --- | --- |
| `''` | 初始为空的文字输入 |
| `0` | 数量或初始数值 |
| `false` | 初始关闭、未完成或未处理 |
| `null` | 当前还没有选择或取得数据 |
| `[]` | 初始为空的列表 |
| `{}`或具体对象 | 一组有关联的数据 |

初始值应该表达真实业务状态。尚未取得详情时，`null`通常比虚构一个字段全为空的对象更清楚。JavaScript阶段先理解这些初始状态；第20章再说明空数组和可空对象为什么需要显式类型。

## 5. ref保存对象时怎样修改

### 5.1 修改对象属性

```js
import { ref } from 'vue'

const task = ref({
  id: 101,
  title: '规格确认',
  assignee: '田中',
  priority: 'normal',
  status: 'todo',
  dueDate: '2026-09-30',
})

task.value.status = 'done'
task.value.title = '规格再确认'
```

脚本中先通过`task.value`取得对象，再访问对象字段。Vue也会跟踪对象内部属性的变化。

模板中可以直接写：

```vue
<p>{{ task.title }}</p>
<p>{{ task.status === 'done' ? '完成' : '未完成' }}</p>
```

### 5.2 整体替换对象

```js
task.value = {
  id: 102,
  title: '测试结果确认',
  assignee: '佐藤',
  priority: 'high',
  status: 'todo',
  dueDate: '2026-10-05',
}
```

ref既可以修改内部字段，也可以整体替换为新对象。这对“重新读取详情”“清空当前选择”等场景很实用。

不要漏掉`.value`：

```js
// 错误：task是const变量，不能这样替换
task = { id: 102, title: '测试', assignee: '佐藤', priority: 'normal', status: 'todo', dueDate: '' }
```

## 6. ref保存数组时怎样修改

```js
import { ref } from 'vue'

const tasks = ref([
  { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
])

tasks.value.push({ id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'doing', dueDate: '2026-10-05' })
tasks.value.splice(0, 1)
```

`push()`、`pop()`、`splice()`等会修改原数组，Vue能够跟踪这些变化。

也可以用一个新数组整体替换：

```js
tasks.value = tasks.value.filter((task) => task.id !== 101)
```

`filter()`返回新数组，所以必须把结果重新赋给`tasks.value`。如果只写`tasks.value.filter(...)`而不接收结果，原列表不会改变。

数组筛选和列表渲染会在第6章详细学习。本节只需要掌握：脚本中操作ref数组，先写`.value`。

## 7. 使用reactive管理对象

### 7.1 reactive返回代理对象

```js
import { reactive } from 'vue'

const form = reactive({
  title: '',
  assignee: '',
  priority: 'normal',
})

form.title = '接口确认'
form.assignee = '田中'
```

`reactive()`接收对象，并返回一个Vue可以跟踪的**代理对象（Proxy）**。读写时直接使用`form.title`，不使用`.value`。

这里的代理对象可以先理解为：Vue包装后的对象外观仍像普通对象，但Vue能够知道哪些属性被读取和修改。`Proxy`的JavaScript原理已在JavaScript课程中讲解，本章只关注Vue中的使用。

### 7.2 嵌套属性也是响应式的

```js
const taskState = reactive({
  selectedTask: {
    id: 101,
    title: '规格确认',
  },
  messages: [],
})

taskState.selectedTask.title = '规格再确认'
taskState.messages.push('修改完成')
```

普通`ref()`和`reactive()`默认都会处理对象内部的嵌套变化。数据非常大或需要与外部库保留原对象时才会考虑浅层API，不属于新人主线。

## 8. reactive不能随意整体替换

```js
import { reactive } from 'vue'

const form = reactive({ title: '', priority: 'normal' })

// 不推荐：新的普通对象替换了变量，原来的响应式代理关系被丢弃
// form = { title: '新任务', priority: 'high' }
```

而且这里的`form`通常用`const`声明，本身也不能重新赋值。需要重置字段时可以逐项修改：

```js
form.title = ''
form.priority = 'normal'
```

也可以把同名字段复制进去：

```js
Object.assign(form, {
  title: '',
  priority: 'normal',
})
```

如果业务上经常需要整体替换对象，使用`ref({ ... })`通常更直观。

## 9. ref和reactive怎样选择

| 数据情况 | 推荐选择 | 原因 |
| --- | --- | --- |
| 字符串、数字、布尔值、`null` | `ref` | `reactive`只适合对象类型 |
| 数组 | 优先`ref` | 可以整体替换，规则统一 |
| 可能重新从接口取得的对象 | 优先`ref` | 方便整体替换或设为`null` |
| 字段始终一起维护的表单对象 | `reactive`也合适 | 直接使用`form.title` |
| 团队已经统一一种方式 | 遵守团队约定 | 一致性比个人偏好重要 |

本课程采用以下简单规则：

1. 默认优先使用`ref()`。
2. 字段较多、作为一个整体逐项编辑的表单可使用`reactive()`。
3. 不为了减少`.value`而随意切换写法。

## 10. 解构为什么可能失去响应性

```js
import { reactive } from 'vue'

const form = reactive({
  title: '规格确认',
  priority: 'normal',
})

const { title } = form
form.title = '规格再确认'

console.log(title) // 仍然是“规格确认”
```

解构时，字符串值被复制到了新的普通变量`title`。它不再和`form.title`保持响应式联系。

模板中直接使用`form.title`最清楚。确实需要保持联系时可以使用`toRefs()`：

```js
import { reactive, toRefs } from 'vue'

const form = reactive({ title: '', priority: 'normal' })
const { title, priority } = toRefs(form)

title.value = '规格确认'
```

`toRefs()`把对象各属性转换成与原属性保持连接的ref。它在组合式函数返回响应式对象时比较常见，新人阶段只要求能阅读，不必为了形式统一到处使用。

## 11. 判断和查看响应式对象

Vue提供一些调查用方法：

```js
import { isReactive, isRef, reactive, ref, toRaw } from 'vue'

const count = ref(0)
const form = reactive({ title: '' })

console.log(isRef(count))       // true
console.log(isReactive(form))   // true
console.log(toRaw(form))        // 查看原始对象
```

- `isRef()`判断一个值是否为ref。
- `isReactive()`判断对象是否由`reactive()`创建。
- `toRaw()`临时取得代理背后的原始对象，主要用于调查，不应长期保存后再直接修改。

日常开发更推荐使用Vue DevTools查看组件状态。浏览器控制台中看到`Proxy`不是错误，它通常说明该对象正在被Vue代理。

## 12. 常见错误与状态设计问题

### 12.1 把不会变化的数据全部写成ref

固定配置、固定标签和不会变化的常量可以继续使用普通`const`。只有运行期间会变化并影响界面的数据才需要响应式。

### 12.2 保存可以计算出来的重复状态

同时保存任务数组和未完成数量，容易只更新其中一个。由已有状态计算得到的数据会在第7章使用`computed()`处理。

### 12.3 多处保存同一份业务数据

父组件和子组件各复制一份任务列表，会产生不同步问题。第10～11章会学习组件状态归属和单向数据流，第18章再处理跨页面共享。

### 12.4 直接用代理对象作为接口数据结论

响应式只代表Vue能够跟踪变化，不代表数据格式正确或安全。接口返回值仍要在第17章进行HTTP检查和字段校验。

## 13. 本章完整示例

下面的完整示例只在脚本初始化时修改状态，重点观察`.value`与模板自动解包：

```vue
<script setup>
import { reactive, ref } from 'vue'

const status = ref('准备中')
const tasks = ref([
  { id: 101, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
])
const form = reactive({
  title: '',
  priority: 'normal',
})

function loadSample() {
  status.value = '读取完成'
  tasks.value.push({ id: 102, title: '页面实现', assignee: '佐藤', priority: 'high', status: 'doing', dueDate: '2026-10-05' })
  form.title = '测试结果确认'
}

loadSample()
</script>

<template>
  <main>
    <p>状态：{{ status }}</p>
    <p>任务数量：{{ tasks.length }}</p>
    <p>输入中的任务：{{ form.title || '未输入' }}</p>
  </main>
</template>
```

组件初始化时函数同时修改三个状态，模板显示修改后的结果。这里没有查询DOM，也没有设置`textContent`。第4章会把状态修改函数绑定到真实用户操作。

## 14. 练习

1. 分别用`ref()`保存字符串、数字、布尔值、`null`、数组和对象，并在模板中显示。
2. 编写并调用一个普通函数修改这些状态，记录模板最终显示结果。
3. 给ref数组增加和删除任务，说明`push()`与`filter()`写法的区别。
4. 用`reactive()`建立标题、负责人、优先级表单并逐项重置。
5. 解构`reactive`对象的字符串字段，观察为什么没有继续更新，再用`toRefs()`验证差异。
6. 在Vue DevTools中找到`App`组件，观察ref和reactive状态。
7. 根据本章选择表说明任务列表、当前任务、搜索关键字和表单分别适合哪种写法。

## 本章检查点

- 能解释普通变量与响应式状态的区别。
- 能说明`ref()`的参数、返回值和`.value`分别是什么。
- 能区分脚本中的`.value`与模板自动解包。
- 能修改ref保存的基本值、对象和数组。
- 能说明`reactive()`返回的是代理对象，并直接通过属性修改。
- 知道reactive对象不能随意整体替换。
- 能根据数据是否整体替换选择`ref`或`reactive`。
- 知道直接解构可能失去响应性，能读懂`toRefs()`。
- 知道响应式不等于接口数据已经通过校验。





