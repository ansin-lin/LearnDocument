# 第 14 章 生命周期与模板引用

组件从出现到离开页面，会经历创建、挂载、更新和卸载。大多数页面内容只需要响应式状态和模板；只有必须访问真实DOM或管理浏览器外部资源时，才需要生命周期钩子。

## 本章目标与前置知识

- 【必须掌握】理解组件挂载、更新和卸载的含义。
- 【必须掌握】使用`onMounted()`访问挂载后的DOM。
- 【必须掌握】使用Template Ref取得DOM元素，并在元素存在后安全访问。
- 【必须掌握】注册并清理事件监听器、定时器等外部资源。
- 【会使用、能看懂】通过`nextTick()`等待一次DOM更新完成。
- 【会读即可】`onUpdated()`以及其他生命周期钩子的执行时机。

需要掌握`ref()`、条件渲染、事件绑定、`watch()`和组件基础。本章示例使用JavaScript。

## 1. 组件为什么有生命周期

Vue组件不是页面打开后永远不变的文件。它可能因为路由切换、条件渲染或父组件更新而被创建、更新和移除。

```text
创建组件
  ↓ 执行script setup
注册各生命周期回调
  ↓ onBeforeMount（真实DOM尚未挂载）
生成并插入真实DOM
  ↓ onMounted（真实DOM已经挂载）
  ↓ 响应式状态变化
onBeforeUpdate（DOM仍是更新前内容）
  ↓ 更新真实DOM
onUpdated（DOM已经更新）
  ↓ 组件离开页面
onBeforeUnmount（DOM仍然存在）
  ↓ 移除组件
onUnmounted（卸载完成）
```

`script setup`中的顶层代码在创建组件实例时执行。此时Vue还没有把该组件的真实DOM放进页面，因此不能立即读取输入框尺寸或调用`focus()`。

## 2. 使用onMounted处理挂载后的工作

`onMounted(callback)`注册一个回调。当组件第一次生成的DOM已经挂载到页面后，Vue调用该回调。

```vue
<script setup>
import { onMounted } from 'vue'

console.log('1. 创建组件')

onMounted(() => {
  console.log('2. DOM挂载完成')
})
</script>

<template>
  <p>任务编辑画面</p>
</template>
```

刷新页面后，控制台依次输出：

```text
1. 创建组件
2. DOM挂载完成
```

`onMounted()`接收一个函数，不要写成`onMounted(run())`。后者会立刻调用`run()`，传给Vue的反而是函数返回值。

### 2.1 注册钩子不等于执行回调

下面六个`onXxx()`调用都在`script setup`执行期间完成，它们只是把回调登记给Vue。回调会等到对应阶段才执行。

`LifecycleDemo.vue`：

```vue
<script setup>
import {
  onBeforeMount,
  onBeforeUnmount,
  onBeforeUpdate,
  onMounted,
  onUnmounted,
  onUpdated,
  ref,
} from 'vue'

console.log('1. setup')

const count = ref(0)

onBeforeMount(() => console.log('2. onBeforeMount'))
onMounted(() => console.log('3. onMounted'))
onBeforeUpdate(() => console.log('4. onBeforeUpdate'))
onUpdated(() => console.log('5. onUpdated'))
onBeforeUnmount(() => console.log('6. onBeforeUnmount'))
onUnmounted(() => console.log('7. onUnmounted'))
</script>

<template>
  <section>
    <p>当前次数：{{ count }}</p>
    <button type="button" @click="count++">增加</button>
  </section>
</template>
```

父组件控制子组件是否存在：

```vue
<script setup>
import { ref } from 'vue'
import LifecycleDemo from './components/LifecycleDemo.vue'

const visible = ref(true)
</script>

<template>
  <button type="button" @click="visible = !visible">切换演示组件</button>
  <LifecycleDemo v-if="visible" />
</template>
```

清空控制台后按顺序操作，可以观察：

```text
首次显示：setup → onBeforeMount → onMounted
点击“增加”：onBeforeUpdate → onUpdated
隐藏组件：onBeforeUnmount → onUnmounted
再次显示：setup → onBeforeMount → onMounted
```

状态更新不会重新执行`setup`，但使用`v-if`移除后再次显示，会创建新的组件实例。

## 3. 什么是模板引用

模板引用让脚本取得模板生成的真实DOM元素。它适合聚焦、滚动、测量尺寸或接入第三方界面库，不应用来代替数据绑定。

`TaskEditor.vue`：

```vue
<script setup>
import { onMounted } from 'vue'

const titleInput = ref(null)

onMounted(() => {
  titleInput.value?.focus()
})
</script>

<template>
  <label for="task-title">任务名称</label>
  <input id="task-title" ref="titleInput">
</template>
```

模板中的`ref="titleInput"`与脚本中的`const titleInput = ref(null)`使用相同名称建立引用。挂载前`titleInput.value`是`null`，挂载后才指向真实输入元素，所以示例使用可选链`?.`安全调用`focus()`。第21章再说明怎样为这个引用增加DOM类型。

### 3.1 模板引用不适合做什么

下面的需求优先使用Vue声明式写法：

| 需求 | 推荐方式 |
| --- | --- |
| 显示文本 | 插值表达式 |
| 切换CSS类 | `:class` |
| 显示或隐藏元素 | `v-if`、`v-show` |
| 读取输入值 | `v-model` |
| 聚焦、测量、滚动 | 模板引用 |

直接通过DOM修改文本或类名，可能使真实DOM与Vue状态不一致。

## 4. 更新状态后DOM不会在当前语句中立即完成更新

Vue会把同一轮中的多次状态修改合并，再更新DOM。下面的输入框只有`editing`为`true`时才存在：

```vue
<script setup>
import { ref } from 'vue'

const editing = ref(false)
const titleInput = ref(null)

function openEditor() {
  editing.value = true
  titleInput.value?.focus()
}
</script>

<template>
  <button type="button" @click="openEditor">编辑</button>
  <input v-if="editing" ref="titleInput">
</template>
```

点击按钮时先把`editing`改为`true`，但当前函数还没有结束，Vue还未生成输入框。因此`titleInput.value`仍可能是`null`，聚焦不会成功。

## 5. 使用nextTick等待DOM更新

`nextTick()`返回一个Promise，在Vue完成当前这一轮DOM更新后结束等待。

```vue
<script setup>
import { nextTick, ref } from 'vue'

const editing = ref(false)
const titleInput = ref(null)

async function openEditor() {
  editing.value = true
  await nextTick()
  titleInput.value?.focus()
}
</script>

<template>
  <button type="button" @click="openEditor">编辑</button>
  <input v-if="editing" ref="titleInput" aria-label="任务名称">
</template>
```

执行顺序是：

```text
修改editing
→ Vue安排DOM更新
→ await nextTick()暂停当前函数
→ Vue生成输入框
→ 继续执行focus()
```

`nextTick()`不是延迟指定毫秒数，也不是`setTimeout()`的替代品。只有后续操作确实依赖更新后的DOM时才使用。

## 6. 状态变化与onUpdated

`onUpdated()`在组件因响应式状态变化而完成DOM更新后执行。

```vue
<script setup>
import { onUpdated, ref } from 'vue'

const count = ref(0)

onUpdated(() => {
  console.log(`DOM已更新，当前次数：${count.value}`)
})
</script>

<template>
  <button type="button" @click="count++">次数：{{ count }}</button>
</template>
```

每次点击按钮，页面文本更新后控制台输出当前次数。`onUpdated()`可能因组件内任意响应式变化而执行，因此不能把某个字段的普通业务处理都放进去。只关心特定状态时使用`watch()`更明确；只计算显示值时使用`computed()`。

不要在`onUpdated()`中无条件修改会参与页面显示的状态，否则可能形成“更新后再次修改、再次更新”的循环。

## 7. 卸载时清理外部资源

组件卸载后，浏览器注册的窗口监听器和定时器不会自动按业务意图消失。反复进入页面却不清理，会出现一次操作触发多次处理、内存占用增加等问题。

### 7.1 清理事件监听器

```vue
<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const windowWidth = ref(0)

function updateWidth() {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  updateWidth()
  window.addEventListener('resize', updateWidth)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateWidth)
})
</script>

<template>
  <p>窗口宽度：{{ windowWidth }}px</p>
</template>
```

`addEventListener()`和`removeEventListener()`必须使用同一个函数引用，所以示例把处理函数保存为`updateWidth`，没有分别书写两个匿名函数。

### 7.2 清理定时器

```js
import { onMounted, onUnmounted } from 'vue'

let timerId

onMounted(() => {
  timerId = window.setInterval(() => {
    console.log('检查任务更新')
  }, 5000)
})

onUnmounted(() => {
  if (timerId !== undefined) window.clearInterval(timerId)
})
```

`setInterval()`每隔指定毫秒数重复执行函数，并返回定时器编号；`clearInterval()`使用该编号停止定时器。DOM监听器、定时器、`ResizeObserver`、WebSocket连接和第三方图表实例都遵循“创建与释放成对出现”的原则。

## 8. 常用生命周期时机

| 钩子 | 执行时机 | 常见用途 | 掌握要求 |
| --- | --- | --- | --- |
| `onBeforeMount` | 首次渲染前，组件DOM尚未挂载 | 阅读既有代码，极少需要自行使用 | 会读即可 |
| `onMounted` | 第一次DOM挂载后 | 聚焦、读取尺寸、注册外部资源 | 必须掌握 |
| `onBeforeUpdate` | 状态已改变，DOM尚未更新 | 更新前记录滚动位置等少量场景 | 会读即可 |
| `onUpdated` | 响应式更新刷新DOM后 | 少量必须读取更新后DOM的场景 | 会读即可 |
| `onBeforeUnmount` | 卸载开始前，DOM仍存在 | 需要在DOM移除前进行的清理 | 会读即可 |
| `onUnmounted` | 组件卸载后 | 清理监听器、定时器和连接 | 必须掌握 |

生命周期钩子必须在`script setup`执行期间同步注册，不要等到点击按钮后才临时调用`onMounted()`或`onUnmounted()`。

### 8.1 执行次数和DOM状态

| 阶段 | 一次组件实例通常执行几次 | 当前能否读取已挂载DOM |
| --- | --- | --- |
| `script setup` | 1次 | 不能 |
| `onBeforeMount` | 1次 | 不能 |
| `onMounted` | 1次 | 能 |
| `onBeforeUpdate` | 可能多次 | 能，但还是更新前内容 |
| `onUpdated` | 可能多次 | 能，已经是更新后内容 |
| `onBeforeUnmount` | 1次 | 能 |
| `onUnmounted` | 1次 | 组件DOM已经移除 |

### 8.2 父子组件的执行关系

首次挂载父子组件时，可以观察到以下主要顺序：

```text
父组件setup
→ 父组件onBeforeMount
→ 子组件setup
→ 子组件onBeforeMount
→ 子组件onMounted
→ 父组件onMounted
```

父组件要等模板中的子组件挂载完成后，自己的`onMounted`才执行。维护项目时应通过控制台或调试器确认实际顺序，不要让父子组件依赖难以理解的隐含先后关系。

### 8.3 会读即可：扩展生命周期接口

- `onErrorCaptured()`用于捕获后代组件传播上来的错误，达到会读即可；统一错误页面和日志策略应在项目层面设计。
- `onActivated()`和`onDeactivated()`与`KeepAlive`缓存组件配合，当前只需知道缓存组件可能不会真正卸载。
- `onRenderTracked()`和`onRenderTriggered()`主要用于开发环境调查响应式渲染。
- `onServerPrefetch()`用于服务端渲染，不属于当前浏览器SPA主线。

这些接口有各自前置场景，不应为了“使用生命周期”而加入普通业务组件。

## 9. computed、watch和生命周期怎样选择

| 需求 | 使用方式 |
| --- | --- |
| 根据任务数组得到未完成数量 | `computed()` |
| 关键字变化后保存搜索条件 | `watch()` |
| 组件出现后聚焦输入框 | `onMounted()` |
| 条件输入框生成后立即聚焦 | 修改状态后`await nextTick()` |
| 页面出现时注册窗口监听 | `onMounted()`，并在`onUnmounted()`清理 |
| 更新前保存DOM滚动位置 | `onBeforeUpdate()` |
| 更新后测量DOM | `onUpdated()`，谨慎使用 |

普通响应式初始值可以直接在`script setup`中创建。不要为了“初始化”而把所有代码都塞进`onMounted()`。

## 10. WorkHub阶段成果与验证

把第12章的搜索输入组件放进一个可显示或隐藏的编辑区域：

1. 点击“编辑任务”后显示输入框。
2. 使用`nextTick()`等待输入框生成并自动聚焦。
3. 组件挂载时注册窗口尺寸监听。
4. 组件卸载时移除监听。

验证时反复显示和隐藏该组件，并在浏览器中改变窗口宽度。每次尺寸变化应只处理一次；重新显示组件后不应积累重复监听。

## 11. 常见错误

- 在`script setup`顶层直接读取尚未挂载的DOM。
- 修改`v-if`条件后立即访问新元素，却没有等待DOM更新。
- 注册监听器或定时器后没有清理。
- 清理事件时传入另一个匿名函数，导致原监听器没有移除。
- 在`onUpdated()`中无条件修改响应式状态形成循环。
- 用生命周期代替本应由`computed()`或`watch()`解决的问题。

## 12. 练习与检查点

1. 实现一个挂载后自动聚焦的任务名称输入框。
2. 通过`v-if`控制编辑框，先复现无法立即聚焦的问题，再使用`nextTick()`修正。
3. 注册窗口尺寸监听，并在组件卸载时清理。
4. 故意使用不同匿名函数移除监听，观察问题后改成同一个函数引用。
5. 分别为“未完成数”“保存搜索条件”“挂载后测量宽度”选择`computed`、`watch`或生命周期，并说明理由。
6. 运行父子生命周期示例，记录首次挂载、状态更新、卸载和再次挂载的控制台顺序。

- [ ] 能按顺序解释创建、挂载、更新和卸载。
- [ ] 能说明模板引用在挂载前为什么可能是`null`。
- [ ] 能使用`onMounted()`和`nextTick()`访问正确时机的DOM。
- [ ] 能成对注册和清理事件监听器、定时器等外部资源。
- [ ] 能说明六个核心生命周期钩子的执行时机、执行次数和DOM状态。




