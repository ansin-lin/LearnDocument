# 第 9 章 watch与副作用

## 本章目标

- 【必须掌握】使用`watch()`观察ref或getter，并取得新旧值。
- 【必须掌握】区分“得到一个值”的computed与“变化后执行动作”的watch。
- 【必须掌握】使用`immediate`基础写法，并理解为什么需要清理旧操作。
- 【会读即可】识别`deep`、`watchEffect()`和`flush`的高级用法，不把它们作为默认方案。

## 前置知识

需要掌握`ref`、`computed`、函数、定时器、`localStorage`和异步函数。

## 1. 为什么需要watch

状态变化会自动更新模板，但有时还要执行更新页面以外的动作，例如保存搜索条件、重新请求数据或取消上一次延迟处理。这些动作称为副作用。

```text
需要得到一个值 → computed
某个值变化后执行动作 → watch
```

## 2. 观察ref

```vue
<script setup>
import { ref, watch } from 'vue'

const keyword = ref('')

watch(keyword, (newValue, oldValue) => {
  console.log(`关键字由“${oldValue}”变为“${newValue}”`)
})
</script>

<template>
  <input v-model="keyword" aria-label="任务关键字">
</template>
```

`watch(source, callback)`的第一个参数是观察来源，第二个参数在来源变化后执行。回调依次收到新值和旧值。

执行过程是：用户修改输入框 → `keyword.value`变化 → Vue安排watch回调 → 回调收到本次新值和上一次旧值。默认watch不会在声明时立即调用。

```text
初始keyword：""
输入v       → newValue="v"，oldValue=""
继续输入ue  → newValue="vue"，oldValue="v"
```

## 3. 观察对象属性要使用getter

```js
import { reactive, watch } from 'vue'

const condition = reactive({ keyword: '', status: 'all' })

watch(
  () => condition.status,
  (newStatus, oldStatus) => {
    console.log(oldStatus, '→', newStatus)
  },
)
```

`() => condition.status`是getter，告诉Vue读取哪个值。不要把当前普通字符串`condition.status`直接作为观察来源。

直接写`watch(condition.status, ...)`时，传入的只是调用当时的字符串`"all"`，后续属性变化无法从这个普通值追踪回原对象。getter会在Vue需要检查来源时重新读取属性。

### 3.1 同时观察多个来源

```js
const keyword = ref('')
const status = ref('all')

watch(
  [keyword, status],
  ([newKeyword, newStatus], [oldKeyword, oldStatus]) => {
    console.log(
      `${oldKeyword}/${oldStatus} → ${newKeyword}/${newStatus}`,
    )
  },
)
```

来源数组中每一项都必须是可观察来源，例如ref或getter。任意一项变化都会执行回调；新值数组和旧值数组的位置与来源数组一一对应。

### 3.2 watch返回停止函数

```js
const stopWatching = watch(keyword, value => {
  console.log(value)
})

function stop() {
  stopWatching()
}
```

在组件`setup`期间同步创建的watch通常会随组件卸载自动停止。需要提前结束一次临时观察时，可以调用返回的停止函数。

## 4. immediate与执行时机

```js
watch(
  keyword,
  newKeyword => {
    localStorage.setItem('workhub.keyword', newKeyword)
  },
  { immediate: true },
)
```

`immediate: true`让回调在监听建立时先执行一次，之后再响应变化。默认情况下不会在初始化时执行。

首次立即执行时没有“上一次变化”，因此旧值通常是`undefined`。回调如果使用旧值，要先处理这种情况：

```js
watch(keyword, (newValue, oldValue) => {
  if (oldValue === undefined) {
    console.log('首次读取：', newValue)
    return
  }
  console.log('发生变化：', oldValue, '→', newValue)
}, { immediate: true })
```

一次状态变化大致经过：

```text
修改响应式状态
↓
Vue安排本轮更新
↓
默认watch回调（flush: 'pre'）
↓
组件DOM更新
↓
flush: 'post'的watch回调
```

需要在回调中读取更新后的DOM时，可指定`{ flush: 'post' }`。`flush: 'sync'`会同步执行，容易产生频繁调用或循环更新，只要求能看懂，不作为主线写法。

下面的完整示例在回调中读取更新后的预览文字，因此使用`post`：

```vue
<script setup>
import { ref, watch } from 'vue'

const keyword = ref('')

watch(keyword, () => {
  const text = document.querySelector('#keyword-preview')?.textContent
  console.log('DOM更新后的文字：', text)
}, { flush: 'post' })
</script>

<template>
  <input v-model="keyword" aria-label="任务关键字">
  <p id="keyword-preview">当前关键字：{{ keyword }}</p>
</template>
```

若不读取DOM，保持默认即可。不要为了“保险”把全部watch都设为`post`或`sync`。

## 5. 清理旧操作

用户连续输入时，应取消上一次尚未执行的延迟任务：

```js
watch(keyword, (newKeyword, _oldKeyword, onCleanup) => {
  const timerId = window.setTimeout(() => {
    console.log('查询：', newKeyword)
  }, 300)

  onCleanup(() => {
    window.clearTimeout(timerId)
  })
})
```

在下一次回调执行前或侦听器停止时，Vue会调用已登记的清理函数。网络请求也可以使用相同思路取消旧请求。

### 5.1 网络请求也需要清理

搜索条件变化后发出的旧请求可能比新请求更晚返回，导致旧结果覆盖新结果。处理思路仍是：每次watch回调登记清理函数，在下一次查询前取消上一次请求。

本章先通过定时器掌握cleanup的职责。第17章建立统一Axios API层后，再把`AbortController`的`signal`交给API函数，避免在组件和watch示例中混用另一套请求实现。

## 6. deep与watchEffect（会读即可）

`deep: true`用于跟踪对象内部的深层变化，但会扩大观察范围：

```js
const form = reactive({ title: '', assignee: { id: 101, name: '田中' } })

watch(
  form,
  () => console.log('表单内部发生变化'),
  { deep: true },
)
```

深层对象中任意相关属性变化都可能触发回调，数据量大时也会增加遍历成本。只关心负责人编号时，优先`watch(() => form.assignee.id, ...)`。

`watchEffect()`会立即执行函数，并自动收集同步执行期间读取的响应式依赖：

```js
watchEffect(() => {
  console.log(`当前查询：${keyword.value} / ${status.value}`)
})
```

这里没有显式来源数组，Vue根据函数中读取的`keyword.value`和`status.value`建立依赖。业务关键逻辑通常优先显式`watch()`，因为触发来源、旧值和执行条件更容易看清。异步回调在第一个`await`之后才读取的值，不会按直觉自动成为依赖。

## 7. WorkHub示例

```js
const keyword = ref(localStorage.getItem('workhub.keyword') ?? '')

watch(keyword, newKeyword => {
  localStorage.setItem('workhub.keyword', newKeyword)
})
```

这里保存的是跨刷新仍有意义的查询条件。筛选结果仍应使用computed，不使用watch把结果复制到另一个ref。

### 7.1 三类常见副作用

| 状态变化 | 适合执行的动作 | 注意事项 |
| --- | --- | --- |
| 查询条件变化 | 保存到`localStorage` | 只保存允许跨刷新保留的非敏感条件 |
| 关键字、状态变化 | 重新发送查询请求 | 延迟、取消旧请求并处理失败 |
| 路由筛选条件变化 | 更新URL query | 避免与路由读取形成循环更新 |

如果只是计算`filteredTasks`，仍使用computed；只有要影响组件外部资源或执行异步动作时才考虑watch。

### 7.2 循环更新为什么危险

```js
watch(firstName, value => {
  fullName.value = `${value} ${lastName.value}`
})

watch(fullName, value => {
  firstName.value = value.split(' ')[0]
})
```

两个watch互相修改来源，可能重复触发且难以判断谁是真实状态。`fullName`能由姓名计算，应改为computed；需要修改姓名时使用一个明确的提交函数。

## 8. 常见错误与项目注意事项

- 用watch维护本可以computed得到的结果。
- 双向watch两个状态，形成循环修改。
- 每次按键立即发送请求，没有延迟、取消或竞态处理。
- 为整个大对象添加`deep: true`，却说不清真正关心哪个字段。
- 回调修改自己正在观察的来源，造成重复执行。

## 9. 练习

1. 观察任务关键字，输出新值和旧值。
2. 把关键字保存到`localStorage`，刷新页面后恢复。
3. 使用清理函数实现300毫秒延迟查询，快速输入时只输出最后一次。
4. 把一个用watch同步筛选数组的实现改为computed。
5. 同时观察关键字和状态，正确解构新旧值数组。
6. 编写`deep: true`示例，再改为只观察具体字段的getter。
7. 使用AbortController取消旧请求，确认快速输入时旧响应不会覆盖新响应。
8. 建立一个互相修改的watch错误示例，再用computed或单一修改函数修复。

## 本章检查点

- [ ] 能根据需求选择computed或watch。
- [ ] 能观察ref和对象的指定属性。
- [ ] 能解释`immediate`和默认执行时机。
- [ ] 能为延迟任务登记清理函数。
- [ ] 能读懂`deep`、`watchEffect`和`flush: 'post'`。
- [ ] 能同时观察多个来源并按位置取得新旧值。
- [ ] 能停止临时watch，并使用cleanup取消旧定时器或请求。





