# Vue 面试题教程版（基础～工程化）

> 适合对象：刚学完 Vue 基础、准备 Vue/Vue3 前端面试、1～3 年经验复习。  
> 使用方式：先看“核心回答”，再看“详细解释”，最后尝试自己用 1～2 分钟复述。

---

## 目录

1. [Vue 基础](#一vue-基础)
2. [响应式系统](#二响应式系统)
3. [生命周期](#三生命周期)
4. [组件通信](#四组件通信)
5. [Vue Router](#五vue-router)
6. [状态管理：Vuex / Pinia](#六状态管理vuex--pinia)
7. [性能优化](#七性能优化)
8. [Vue3 新特性](#八vue3-新特性重点)
9. [真实项目与场景题](#九真实项目--场景题)
10. [工程化与构建](#十工程化--构建)

---

## 一、Vue 基础

### 1. Vue 是什么？核心思想是什么？

#### 核心回答

Vue 是一个用于构建用户界面的 JavaScript 渐进式框架。它的核心思想是**数据驱动视图**和**组件化开发**。

#### 详细解释

在传统 DOM 开发中，开发者经常需要手动操作 DOM，例如：

```js
document.querySelector('#title').innerText = 'Hello'
```

Vue 的思路是：开发者只需要维护数据，页面会根据数据自动更新。

```vue
<template>
  <h1>{{ title }}</h1>
</template>

<script setup>
import { ref } from 'vue'

const title = ref('Hello')
</script>
```

当 `title` 改变时，页面会自动重新渲染。  
这就是“数据驱动视图”。

Vue 另一个核心思想是组件化。页面可以拆成很多独立组件，例如：

- Header
- Sidebar
- UserList
- UserForm
- Pagination

每个组件负责自己的结构、样式和逻辑，方便复用和维护。

#### 面试表达

Vue 是一个构建前端页面的渐进式框架，核心思想主要有两个：第一是数据驱动视图，也就是数据变化后视图自动更新；第二是组件化开发，把复杂页面拆成多个可复用、可维护的组件。

---

### 2. Vue 的优缺点？

#### 优点

1. 学习成本相对低，模板语法接近 HTML。
2. 数据响应式，开发效率高。
3. 组件化清晰，适合中大型项目维护。
4. 生态成熟，有 Vue Router、Pinia、Vite 等工具。
5. Vue3 对 TypeScript、组合式 API、性能都有更好支持。

#### 缺点

1. 生态规模整体小于 React。
2. Vue2 到 Vue3 有一定迁移成本。
3. 过度使用响应式和 watch，可能导致逻辑分散。
4. 大型项目中，如果没有规范，组件和状态管理容易混乱。

#### 面试表达

Vue 的优点是上手快、响应式和组件化开发效率高，生态也比较完整。缺点是大型项目中需要良好的代码规范，否则组件通信和状态管理容易变复杂。另外 Vue2 到 Vue3 在写法和响应式原理上有变化，迁移时需要注意。

---

### 3. MVC / MVP / MVVM 区别？

#### MVC

MVC 分为：

- Model：数据和业务逻辑
- View：视图
- Controller：控制器，负责接收用户操作并更新 Model 或 View

在 MVC 中，Controller 是 View 和 Model 的协调者。

#### MVP

MVP 分为：

- Model
- View
- Presenter

Presenter 负责业务逻辑，View 通常比较被动。  
View 和 Model 不直接通信，而是通过 Presenter 交互。

#### MVVM

MVVM 分为：

- Model：数据模型
- View：视图
- ViewModel：连接 View 和 Model 的桥梁

Vue 更接近 MVVM。  
在 Vue 中，模板可以看作 View，响应式数据和方法可以看作 ViewModel。

```vue
<template>
  <p>{{ name }}</p>
</template>

<script setup>
import { ref } from 'vue'

const name = ref('Tom')
</script>
```

`name` 改变后，视图自动更新。开发者不用直接操作 DOM。

#### 面试表达

MVC、MVP、MVVM 都是为了分离视图和业务逻辑。Vue 更接近 MVVM，通过响应式系统把数据和视图绑定起来，数据变化后视图自动更新。

---

### 4. Vue 为什么是渐进式框架？

#### 核心回答

渐进式是指 Vue 可以按需使用。可以只在页面中引入 Vue 做局部增强，也可以配合 Vue Router、Pinia、Vite 做完整单页应用。

#### 详细解释

Vue 的使用方式可以从简单到复杂：

1. 只在普通 HTML 页面中引入 Vue。
2. 使用组件开发页面。
3. 使用 Vue Router 做单页应用。
4. 使用 Pinia 管理全局状态。
5. 使用 Vite、TypeScript、测试、工程化工具构建大型项目。

这就是“渐进式”：不强制一次性接受完整生态。

#### 面试表达

Vue 是渐进式框架，因为它可以根据项目复杂度逐步引入功能。简单页面可以只使用响应式和模板，大型项目可以继续引入路由、状态管理、构建工具和 TypeScript。

---

### 5. Vue 和 React 的核心思想区别？

#### 核心回答

Vue 更强调模板、响应式和渐进式开发；React 更强调 JavaScript 函数式思想和单向数据流。

#### 对比

| 方面 | Vue | React |
| --- | --- | --- |
| 视图写法 | 模板语法 | JSX |
| 数据更新 | 响应式系统自动追踪依赖 | setState / hooks 触发渲染 |
| 学习曲线 | 相对平缓 | 更偏 JS 思维 |
| 官方生态 | Vue Router、Pinia 等较统一 | 生态选择更多 |
| 组件逻辑复用 | Composition API / hooks 风格函数 | Hooks |

#### 面试表达

Vue 和 React 都是组件化框架。Vue 更偏模板和响应式，数据变化后依赖自动更新；React 更强调 UI 是状态的函数，通过 state 变化重新执行组件函数生成 UI。Vue 对初学者更友好，React 更偏 JavaScript 表达能力。

---

### 6. 什么是单向数据流？

#### 核心回答

单向数据流是指数据按固定方向流动，通常是父组件通过 props 传给子组件，子组件不能直接修改父组件数据，只能通过事件通知父组件修改。

#### 示例

```vue
<!-- Parent.vue -->
<Child :count="count" @update="count++" />
```

```vue
<!-- Child.vue -->
<template>
  <button @click="$emit('update')">增加</button>
</template>
```

数据方向：

```text
父组件数据 → props → 子组件显示
子组件事件 → emit → 父组件修改数据
```

#### 好处

1. 数据来源清晰。
2. 问题排查方便。
3. 避免子组件随意修改父组件状态。

---

### 7. 什么是响应式数据？

#### 核心回答

响应式数据是指数据被 Vue 代理或包装后，Vue 能追踪它的读取和修改。当数据变化时，依赖它的视图或计算逻辑会自动更新。

#### 示例

```js
import { ref } from 'vue'

const count = ref(0)

count.value++
```

当模板中使用 `count` 时，`count` 改变后页面会自动更新。

#### 本质

响应式系统主要做两件事：

1. 读取数据时收集依赖。
2. 修改数据时触发依赖更新。

---

### 8. 什么是模板编译？

#### 核心回答

模板编译是指 Vue 把 `<template>` 模板转换成渲染函数 `render` 的过程。

#### 详细解释

例如模板：

```vue
<template>
  <div>{{ message }}</div>
</template>
```

会被编译成类似：

```js
function render() {
  return h('div', message)
}
```

Vue 最终不是直接运行模板，而是执行渲染函数生成虚拟 DOM。

#### 面试表达

模板编译就是把 Vue 模板解析成 AST，再生成 render 函数。组件渲染时执行 render 函数，生成虚拟 DOM，最后更新真实 DOM。

---

### 9. 什么是虚拟 DOM？

#### 核心回答

虚拟 DOM 是用 JavaScript 对象描述真实 DOM 结构的一种方式。

#### 示例

真实 DOM：

```html
<div id="app">
  <p>Hello</p>
</div>
```

虚拟 DOM 可以理解成：

```js
{
  type: 'div',
  props: { id: 'app' },
  children: [
    { type: 'p', children: 'Hello' }
  ]
}
```

#### 作用

1. 用 JS 对象描述 UI。
2. 数据变化后生成新的虚拟 DOM。
3. 通过 diff 算法比较新旧虚拟 DOM。
4. 找出最小变更后更新真实 DOM。

---

### 10. 虚拟 DOM 一定比真实 DOM 快吗？

#### 核心回答

不一定。虚拟 DOM 的优势不是每次都比手写 DOM 快，而是让复杂应用的更新更加可预测、可维护，并通过 diff 减少不必要的 DOM 操作。

#### 详细解释

如果只是简单修改一个 DOM：

```js
document.querySelector('#title').innerText = 'Hello'
```

手写真实 DOM 可能更快。

但在复杂页面中，数据变化可能影响多个节点。虚拟 DOM 可以统一计算差异，再批量更新 DOM，降低开发复杂度。

#### 面试表达

虚拟 DOM 不一定比直接操作真实 DOM 快。它的价值主要是抽象 DOM 操作，让框架可以用 diff 算法优化更新，并提升复杂项目的可维护性。

---

### 11. v-if 和 v-show 的区别？

#### 核心回答

`v-if` 是条件渲染，条件为 false 时元素不会存在于 DOM 中。  
`v-show` 是条件显示，元素一直存在，只是通过 `display: none` 控制显示隐藏。

#### 对比

| 对比项 | v-if | v-show |
| --- | --- | --- |
| DOM 是否存在 | false 时不存在 | 始终存在 |
| 切换成本 | 较高 | 较低 |
| 初始渲染成本 | 较低 | 较高 |
| 适用场景 | 条件很少变化 | 频繁切换显示隐藏 |

#### 示例

```vue
<p v-if="isLogin">已登录</p>
<p v-show="isLogin">已登录</p>
```

---

### 12. v-for 为什么要加 key？

#### 核心回答

`key` 是 Vue 识别节点身份的标识，用于 diff 算法判断哪些节点可以复用、移动或删除。

#### 为什么需要 key？

没有 key 时，Vue 可能按位置复用 DOM。  
如果列表顺序变化，可能导致输入框状态、组件状态错乱。

```vue
<li v-for="item in list" :key="item.id">
  {{ item.name }}
</li>
```

#### 面试表达

key 的作用是给每个节点一个稳定身份，帮助 Vue 在 diff 时更准确地复用节点，提高更新准确性和性能。

---

### 13. v-for 中 key 可以用 index 吗？

#### 核心回答

可以，但不推荐。只有列表不会新增、删除、排序时才可以用 index。

#### 原因

如果使用 index：

```vue
<li v-for="(item, index) in list" :key="index">
  <input v-model="item.name" />
</li>
```

当删除中间一项时，后面的 index 会变化，Vue 可能复用错误的 DOM，导致输入框内容错乱。

#### 推荐

使用唯一且稳定的业务 ID：

```vue
<li v-for="item in list" :key="item.id">
  {{ item.name }}
</li>
```

---

### 14. v-if 和 v-for 为什么不推荐一起用？

#### 核心回答

因为可读性差，并且 Vue2 中 `v-for` 优先级高于 `v-if`，会导致每次循环都判断条件，性能不好。Vue3 中优先级变化，但仍不推荐混用。

#### 不推荐写法

```vue
<li v-for="user in users" v-if="user.active" :key="user.id">
  {{ user.name }}
</li>
```

#### 推荐写法

用 computed 先过滤：

```js
const activeUsers = computed(() => users.value.filter(user => user.active))
```

```vue
<li v-for="user in activeUsers" :key="user.id">
  {{ user.name }}
</li>
```

---

### 15. v-bind 和 v-model 的区别？

#### v-bind

`v-bind` 是单向绑定，把数据绑定到属性上。

```vue
<input :value="name" />
```

数据变化会影响 input 的 value，但用户输入不会自动修改 `name`。

#### v-model

`v-model` 是双向绑定，本质是：

```vue
<input :value="name" @input="name = $event.target.value" />
```

#### 面试表达

`v-bind` 是单向绑定，主要用于绑定属性；`v-model` 是双向绑定，常用于表单元素，本质是 value 绑定加事件更新。

---

### 16. v-html 有什么安全问题？

#### 核心回答

`v-html` 会把字符串当作 HTML 插入页面，如果内容来自用户输入，可能导致 XSS 攻击。

#### 示例

```vue
<div v-html="content"></div>
```

如果 `content` 是：

```html
<img src=x onerror="alert('xss')" />
```

就可能执行恶意代码。

#### 解决方案

1. 尽量不用 `v-html`。
2. 如果必须使用，需要服务端或前端过滤危险标签。
3. 不要渲染用户未经过滤的输入内容。

---

### 17. 插值表达式中能写复杂逻辑吗？

#### 核心回答

技术上可以写简单表达式，但不推荐写复杂逻辑。复杂逻辑应放到 computed 或 methods 中。

#### 不推荐

```vue
<p>{{ user.age > 18 ? user.name.toUpperCase() + '成年' : '未成年' }}</p>
```

#### 推荐

```js
const userText = computed(() => {
  return user.age > 18 ? `${user.name.toUpperCase()} 成年` : '未成年'
})
```

```vue
<p>{{ userText }}</p>
```

---

### 18. 事件修饰符有哪些？

常见事件修饰符：

| 修饰符 | 作用 |
| --- | --- |
| `.stop` | 阻止事件冒泡 |
| `.prevent` | 阻止默认行为 |
| `.capture` | 使用捕获模式 |
| `.self` | 只有事件源是自身时触发 |
| `.once` | 事件只触发一次 |
| `.passive` | 提高滚动性能，不阻止默认行为 |

#### 示例

```vue
<a href="https://example.com" @click.prevent="handleClick">点击</a>
<button @click.stop="handleClick">按钮</button>
```

---

### 19. 按键修饰符有哪些？

常见按键修饰符：

| 修饰符 | 作用 |
| --- | --- |
| `.enter` | 回车 |
| `.tab` | Tab |
| `.delete` | Delete 或 Backspace |
| `.esc` | Esc |
| `.space` | 空格 |
| `.up` | 上 |
| `.down` | 下 |
| `.left` | 左 |
| `.right` | 右 |

#### 示例

```vue
<input @keyup.enter="search" />
```

---

### 20. .sync 是什么？（Vue2）

#### 核心回答

`.sync` 是 Vue2 中父子组件双向绑定的一种语法糖，本质是父组件传 prop，子组件触发 `update:xxx` 事件。

#### 示例

```vue
<Child :title.sync="title" />
```

等价于：

```vue
<Child :title="title" @update:title="title = $event" />
```

在子组件中：

```js
this.$emit('update:title', 'new title')
```

Vue3 中更推荐使用 `v-model:xxx`。

---

## 二、响应式系统

### 1. Vue2 响应式原理？

#### 核心回答

Vue2 使用 `Object.defineProperty` 对对象属性进行 getter/setter 拦截。读取属性时收集依赖，修改属性时通知依赖更新。

#### 简化原理

```js
function defineReactive(obj, key, value) {
  Object.defineProperty(obj, key, {
    get() {
      console.log('收集依赖')
      return value
    },
    set(newValue) {
      value = newValue
      console.log('通知更新')
    }
  })
}
```

#### 缺点

Vue2 不能天然监听：

1. 新增对象属性。
2. 删除对象属性。
3. 通过数组下标修改数组。
4. 修改数组 length。

因此 Vue2 中需要：

```js
Vue.set(obj, 'name', 'Tom')
this.$set(obj, 'name', 'Tom')
```

---

### 2. Vue3 响应式原理？

#### 核心回答

Vue3 使用 `Proxy` 实现响应式。Proxy 可以代理整个对象，拦截属性读取、修改、删除、in 操作等。

#### 简化原理

```js
const state = new Proxy({ count: 0 }, {
  get(target, key) {
    console.log('收集依赖')
    return target[key]
  },
  set(target, key, value) {
    target[key] = value
    console.log('触发更新')
    return true
  }
})
```

#### 优点

1. 可以监听新增属性。
2. 可以监听删除属性。
3. 对数组支持更好。
4. 不需要初始化时递归劫持所有属性。
5. 性能和类型支持更好。

---

### 3. Object.defineProperty 有什么缺点？

主要缺点：

1. 只能劫持对象已有属性，不能直接监听新增和删除。
2. 需要递归遍历对象每个属性。
3. 数组变化监听有限。
4. 对 Map、Set 等数据结构支持不好。
5. API 设计上是属性级别，不是对象级别。

#### 面试表达

`Object.defineProperty` 的问题是它只能针对具体属性做 getter/setter，新增属性和删除属性无法自动监听，数组下标和 length 的变化也不能很好处理，所以 Vue2 需要额外处理数组方法和 Vue.set。

---

### 4. Proxy 为什么可以监听数组变化？

#### 核心回答

因为 Proxy 是代理整个对象或数组，可以拦截数组的下标访问、下标修改、length 修改以及数组方法调用引起的变化。

#### 示例

```js
const arr = new Proxy([], {
  set(target, key, value) {
    console.log('修改了', key)
    target[key] = value
    return true
  }
})

arr[0] = 'A'
arr.length = 0
```

`arr[0] = 'A'` 和 `arr.length = 0` 都可以被 Proxy 拦截。

---

### 5. ref 和 reactive 的区别？

#### ref

`ref` 用于包装基本类型，也可以包装对象。

```js
const count = ref(0)
count.value++
```

#### reactive

`reactive` 用于创建对象类型的响应式代理。

```js
const user = reactive({
  name: 'Tom',
  age: 20
})

user.age++
```

#### 对比

| 对比项 | ref | reactive |
| --- | --- | --- |
| 适合数据 | 基本类型、对象都可以 | 主要是对象、数组 |
| 访问方式 | JS 中需要 `.value` | 直接访问属性 |
| 解构影响 | 解构需要注意 | 直接解构会丢失响应式 |
| 常用场景 | 单个值 | 表单、对象状态 |

---

### 6. ref 为什么要用 .value？

#### 核心回答

因为基本类型不是对象，不能被 Proxy 直接代理。Vue 需要把基本类型包装成对象，通过 `.value` 的 getter/setter 实现依赖追踪和更新。

#### 示例

```js
const count = ref(0)

// 实际可以理解成
const count = {
  value: 0
}
```

在 `<template>` 中 Vue 会自动解包，所以模板里可以直接写：

```vue
<p>{{ count }}</p>
```

在 JS 中必须写：

```js
count.value++
```

---

### 7. reactive 可以解构吗？

#### 核心回答

可以解构，但直接解构会丢失响应式。

#### 错误示例

```js
const user = reactive({ name: 'Tom', age: 20 })

const { name } = user
```

此时 `name` 是普通变量，不再和 `user.name` 保持响应式联系。

#### 正确方式

使用 `toRefs`：

```js
import { reactive, toRefs } from 'vue'

const user = reactive({ name: 'Tom', age: 20 })
const { name, age } = toRefs(user)
```

模板或 JS 中使用：

```js
name.value = 'Jack'
```

---

### 8. shallowRef / shallowReactive 是什么？

#### shallowRef

只让 `.value` 本身是响应式，不会深层代理对象内部属性。

```js
const state = shallowRef({ count: 0 })

state.value.count++ // 不一定触发更新
state.value = { count: 1 } // 触发更新
```

#### shallowReactive

只代理对象第一层属性，不深层代理嵌套对象。

```js
const state = shallowReactive({
  user: { name: 'Tom' }
})

state.user = { name: 'Jack' } // 触发
state.user.name = 'Lucy' // 深层不触发
```

#### 使用场景

1. 大型对象。
2. 第三方库对象。
3. 不希望深层响应式带来性能开销。

---

### 9. watch 和 watchEffect 的区别？

#### watch

`watch` 需要明确指定监听源。

```js
watch(count, (newVal, oldVal) => {
  console.log(newVal, oldVal)
})
```

特点：

1. 可以拿到新值和旧值。
2. 默认懒执行。
3. 适合监听指定数据变化。

#### watchEffect

`watchEffect` 会立即执行，并自动收集内部用到的响应式依赖。

```js
watchEffect(() => {
  console.log(count.value)
})
```

特点：

1. 自动收集依赖。
2. 默认立即执行。
3. 不方便直接拿 oldVal。
4. 适合副作用逻辑。

---

### 10. computed 是怎么实现缓存的？

#### 核心回答

`computed` 会基于响应式依赖进行缓存。依赖没有变化时，多次读取会直接返回缓存结果；依赖变化后，才重新计算。

#### 示例

```js
const total = computed(() => {
  console.log('重新计算')
  return price.value * count.value
})
```

如果 `price` 和 `count` 没变，多次访问 `total.value` 不会重复执行函数。

#### 面试表达

computed 内部会记录依赖，并维护一个 dirty 标记。依赖变化时 dirty 变为 true，下次读取 computed 时重新计算；依赖没变化时直接返回缓存值。

---

### 11. computed 能否修改？

#### 核心回答

默认 computed 是只读的，不能直接修改。如果需要修改，可以使用带 getter 和 setter 的 computed。

#### 示例

```js
const fullName = computed({
  get() {
    return firstName.value + ' ' + lastName.value
  },
  set(value) {
    const arr = value.split(' ')
    firstName.value = arr[0]
    lastName.value = arr[1]
  }
})
```

---

### 12. watch 的 immediate 和 deep 有什么用？

#### immediate

让 watch 在初始化时立即执行一次。

```js
watch(userId, fetchUser, { immediate: true })
```

常用于页面加载时先请求一次接口。

#### deep

深度监听对象内部属性变化。

```js
watch(
  () => form,
  () => {
    console.log('form changed')
  },
  { deep: true }
)
```

#### 注意

`deep` 会递归监听对象内部变化，数据很大时可能影响性能。  
能监听具体字段时，尽量不要深度监听整个对象。

---

### 13. Vue 是如何追踪依赖的？

#### 核心回答

Vue 在响应式数据被读取时收集当前正在执行的副作用函数，在数据被修改时触发这些副作用函数重新执行。

#### 简化流程

```text
组件渲染 / computed / watchEffect 执行
        ↓
读取响应式数据
        ↓
track 收集依赖
        ↓
数据变化
        ↓
trigger 触发更新
```

#### 示例理解

```js
watchEffect(() => {
  console.log(count.value)
})
```

执行 `watchEffect` 时读取了 `count.value`，Vue 记录“这个函数依赖 count”。  
之后 `count.value++` 时，Vue 重新执行这个函数。

---

## 三、生命周期

### 1. Vue 生命周期有哪些？

#### Vue2 常见生命周期

| 生命周期 | 说明 |
| --- | --- |
| beforeCreate | 实例初始化前 |
| created | 实例创建完成，数据可用，DOM 未挂载 |
| beforeMount | 挂载前 |
| mounted | DOM 挂载完成 |
| beforeUpdate | 数据更新后，DOM 更新前 |
| updated | DOM 更新完成 |
| beforeDestroy | 销毁前 |
| destroyed | 销毁完成 |

#### Vue3 常见生命周期

| Vue3 Hook | 说明 |
| --- | --- |
| setup | 组件初始化时执行 |
| onBeforeMount | 挂载前 |
| onMounted | 挂载后 |
| onBeforeUpdate | 更新前 |
| onUpdated | 更新后 |
| onBeforeUnmount | 卸载前 |
| onUnmounted | 卸载后 |

---

### 2. created 和 mounted 的区别？

#### created

组件实例已经创建，响应式数据、methods、computed 等可以使用，但真实 DOM 还没有挂载。

#### mounted

组件已经挂载到页面上，真实 DOM 可以访问。

#### 适用场景

| 场景 | 生命周期 |
| --- | --- |
| 初始化数据 | created / setup |
| 请求接口 | created / mounted / setup |
| 操作 DOM | mounted |
| 初始化图表 | mounted |

---

### 3. 请求接口放在哪个生命周期？

#### 常见回答

一般可以放在 `created`、`mounted` 或 Vue3 的 `setup` 中。

#### 实际项目建议

1. 如果请求不依赖 DOM，可以在 `setup` 或 `created` 中。
2. 如果请求依赖 DOM 或图表容器尺寸，可以放在 `mounted` 中。
3. SSR 场景需要考虑服务端数据获取方式。

#### Vue3 示例

```vue
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const list = ref([])

onMounted(async () => {
  const res = await axios.get('/api/users')
  list.value = res.data
})
</script>
```

---

### 4. 父子组件生命周期执行顺序？

#### 初次渲染

```text
父 beforeCreate
父 created
父 beforeMount
子 beforeCreate
子 created
子 beforeMount
子 mounted
父 mounted
```

Vue3 可理解为：

```text
父 setup
父 onBeforeMount
子 setup
子 onBeforeMount
子 onMounted
父 onMounted
```

#### 更新时

通常是：

```text
父 beforeUpdate
子 beforeUpdate
子 updated
父 updated
```

#### 销毁时

通常是：

```text
父 beforeUnmount
子 beforeUnmount
子 unmounted
父 unmounted
```

---

### 5. keep-alive 生命周期？

#### 核心回答

`keep-alive` 用于缓存组件。被缓存的组件不会真正卸载，而是在激活和失活之间切换。

#### 特有生命周期

| 生命周期 | 说明 |
| --- | --- |
| activated | 组件被激活时触发 |
| deactivated | 组件被缓存失活时触发 |

#### 示例

```vue
<keep-alive>
  <RouterView />
</keep-alive>
```

```js
onActivated(() => {
  console.log('页面重新进入')
})

onDeactivated(() => {
  console.log('页面离开但被缓存')
})
```

---

### 6. activated / deactivated 什么时候触发？

#### activated

缓存组件重新显示时触发。  
例如从详情页返回列表页，列表页被 keep-alive 缓存，重新显示时触发 `activated`。

#### deactivated

缓存组件离开当前视图时触发，但不会被销毁。

#### 使用场景

1. 返回页面时刷新数据。
2. 记录滚动位置。
3. 暂停定时器。
4. 恢复页面状态。

---

### 7. setup 是什么时候执行的？

#### 核心回答

`setup` 在组件实例创建之前执行，比 `beforeCreate` 和 `created` 更早。它是 Composition API 的入口。

#### 注意点

在 `setup` 中：

1. 没有 `this`。
2. 可以定义响应式数据。
3. 可以注册生命周期。
4. 可以使用 props 和 emit。

```js
export default {
  setup(props, { emit }) {
    // 没有 this
  }
}
```

`<script setup>` 是 `setup` 的语法糖。

---

### 8. beforeUnmount 和 unmounted 的区别？

#### beforeUnmount

组件卸载前触发。  
此时组件实例还在，DOM 也还在，适合清理定时器、取消请求、解绑事件。

#### unmounted

组件卸载完成后触发。  
组件相关 DOM 已经从页面移除。

#### 示例

```js
let timer = null

onMounted(() => {
  timer = setInterval(() => {}, 1000)
})

onBeforeUnmount(() => {
  clearInterval(timer)
})
```

---

## 四、组件通信

### 1. 父 → 子通信方式？

常见方式：

1. props
2. v-model
3. ref + defineExpose
4. provide/inject
5. 全局状态 Pinia

#### props 示例

```vue
<Child :title="title" />
```

```vue
<script setup>
defineProps({
  title: String
})
</script>
```

#### 面试表达

最常见的父传子是 props。对于表单类组件可以用 v-model。跨层级可以用 provide/inject 或 Pinia。如果父组件需要调用子组件方法，可以用 ref 配合 defineExpose。

---

### 2. 子 → 父通信方式？

常见方式：

1. emit 事件
2. v-model
3. 回调函数 props
4. Pinia

#### emit 示例

```vue
<!-- Child.vue -->
<script setup>
const emit = defineEmits(['submit'])

function handleClick() {
  emit('submit', 'hello')
}
</script>
```

```vue
<!-- Parent.vue -->
<Child @submit="handleSubmit" />
```

---

### 3. 兄弟组件通信？

常见方案：

1. 共同父组件中转。
2. Pinia / Vuex。
3. event bus。
4. provide/inject。
5. URL 参数或缓存。

#### 推荐

简单场景：父组件中转。  
复杂共享状态：Pinia。  
不推荐大量使用 event bus。

---

### 4. 跨层级通信方案？

跨层级通信可以使用：

1. provide / inject
2. Pinia / Vuex
3. props 逐层传递
4. mitt 事件总线
5. 组合式函数封装共享状态

#### 建议

- 层级少：props。
- 层级深但范围局部：provide/inject。
- 多页面、多组件共享：Pinia。

---

### 5. provide / inject 使用场景？

#### 核心回答

`provide/inject` 用于祖先组件向后代组件传递数据，避免 props 层层传递。

#### 示例

```js
// 父组件
provide('theme', 'dark')
```

```js
// 子孙组件
const theme = inject('theme')
```

#### 使用场景

1. 主题配置。
2. 表单上下文。
3. 表格上下文。
4. 多层组件共享局部状态。

#### 注意

`provide/inject` 会让数据来源不如 props 直观，所以不适合滥用。

---

### 6. event bus 的缺点？

主要缺点：

1. 事件来源不清晰。
2. 事件越来越多后难维护。
3. 组件销毁时容易忘记解绑事件。
4. 调试困难。
5. TypeScript 支持不如显式 props/emit 清晰。

#### 面试表达

event bus 适合小项目或简单跨组件事件，但大型项目中容易造成隐式依赖。现在 Vue3 项目更推荐 Pinia、props/emit 或 provide/inject。

---

### 7. v-model 的实现原理？

#### Vue2

默认等价于：

```vue
<Child :value="msg" @input="msg = $event" />
```

#### Vue3

默认等价于：

```vue
<Child :modelValue="msg" @update:modelValue="msg = $event" />
```

子组件：

```vue
<script setup>
const props = defineProps({
  modelValue: String
})

const emit = defineEmits(['update:modelValue'])

function update(value) {
  emit('update:modelValue', value)
}
</script>
```

---

### 8. 多个 v-model 如何实现？

Vue3 支持多个 v-model：

```vue
<UserForm v-model:name="name" v-model:age="age" />
```

子组件：

```js
const props = defineProps({
  name: String,
  age: Number
})

const emit = defineEmits(['update:name', 'update:age'])

emit('update:name', 'Tom')
emit('update:age', 20)
```

---

### 9. 插槽（slot）的作用？

#### 核心回答

插槽用于让父组件向子组件传入一段结构内容，提高组件灵活性。

#### 示例

```vue
<Card>
  <p>这是卡片内容</p>
</Card>
```

```vue
<!-- Card.vue -->
<div class="card">
  <slot></slot>
</div>
```

#### 使用场景

1. 弹窗内容。
2. 表格列自定义。
3. 卡片组件。
4. 布局组件。

---

### 10. 具名插槽和作用域插槽？

#### 具名插槽

用于定义多个插槽位置。

```vue
<Layout>
  <template #header>头部</template>
  <template #default>内容</template>
  <template #footer>底部</template>
</Layout>
```

子组件：

```vue
<header><slot name="header"></slot></header>
<main><slot></slot></main>
<footer><slot name="footer"></slot></footer>
```

#### 作用域插槽

子组件向父组件暴露数据，让父组件决定如何渲染。

```vue
<Child v-slot="{ user }">
  {{ user.name }}
</Child>
```

子组件：

```vue
<slot :user="user"></slot>
```

---

### 11. slot 为什么是函数？

#### 核心回答

在 Vue3 中，slot 本质上是函数。这样可以让插槽内容的渲染时机由子组件控制，也有利于依赖收集和性能优化。

#### 理解

父组件传入的是“如何渲染内容”的函数，子组件在需要的位置调用这个函数。

```js
slots.default?.()
```

这样可以避免不必要的渲染，也能让作用域插槽更自然地传参。

---

## 五、Vue Router

### 1. hash 模式和 history 模式区别？

#### hash 模式

URL 中带 `#`：

```text
https://example.com/#/user
```

特点：

1. 不需要后端特殊配置。
2. 刷新不会 404。
3. URL 不够美观。

#### history 模式

URL 没有 `#`：

```text
https://example.com/user
```

特点：

1. URL 更美观。
2. 需要后端配置 fallback。
3. 刷新页面时服务器需要返回 `index.html`。

---

### 2. history 模式刷新 404 如何解决？

#### 原因

用户访问：

```text
https://example.com/user
```

服务器会去找 `/user` 这个真实路径。  
如果服务器没有这个路径，就会返回 404。

#### 解决方案

配置服务器把所有前端路由都返回 `index.html`。

#### Nginx 示例

```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

---

### 3. 路由懒加载如何实现？

#### 核心回答

使用动态 `import()` 按需加载页面组件。

```js
const routes = [
  {
    path: '/user',
    component: () => import('@/views/User.vue')
  }
]
```

#### 好处

1. 减少首屏 bundle 体积。
2. 用户访问对应路由时才加载页面代码。
3. 配合打包工具自动拆分 chunk。

---

### 4. 路由守卫有哪些？

#### 全局守卫

```js
router.beforeEach((to, from, next) => {})
router.beforeResolve((to, from, next) => {})
router.afterEach((to, from) => {})
```

#### 路由独享守卫

```js
{
  path: '/admin',
  component: Admin,
  beforeEnter: (to, from) => {}
}
```

#### 组件内守卫

```js
beforeRouteEnter(to, from) {}
beforeRouteUpdate(to, from) {}
beforeRouteLeave(to, from) {}
```

Vue3 Composition API 中也有：

```js
onBeforeRouteLeave(() => {})
onBeforeRouteUpdate(() => {})
```

---

### 5. 全局 / 路由独享 / 组件内守卫区别？

| 类型 | 作用范围 | 使用场景 |
| --- | --- | --- |
| 全局守卫 | 所有路由 | 登录鉴权、权限判断、页面标题 |
| 路由独享守卫 | 某个路由配置 | 某个页面单独权限 |
| 组件内守卫 | 某个组件 | 离开确认、组件复用参数变化 |

---

### 6. 路由传参的几种方式？

常见方式：

1. query
2. params
3. path 参数
4. props 解耦
5. state 或缓存

#### query

```js
router.push({
  path: '/user',
  query: { id: 1 }
})
```

URL：

```text
/user?id=1
```

#### params

```js
router.push({
  name: 'user',
  params: { id: 1 }
})
```

路由配置：

```js
{
  path: '/user/:id',
  name: 'user',
  component: User
}
```

URL：

```text
/user/1
```

---

### 7. query 和 params 区别？

| 对比项 | query | params |
| --- | --- | --- |
| URL 表现 | `/user?id=1` | `/user/1` |
| 是否需要配置路径参数 | 不需要 | 通常需要 |
| 刷新是否保留 | 保留 | 路径参数保留 |
| 适用场景 | 搜索、筛选、分页 | 详情页 ID |

#### 注意

使用 `params` 时，推荐用命名路由：

```js
router.push({ name: 'user', params: { id: 1 } })
```

---

### 8. 路由跳转方式有哪些？

#### 声明式导航

```vue
<RouterLink to="/home">首页</RouterLink>
```

#### 编程式导航

```js
router.push('/home')
router.replace('/login')
router.back()
router.forward()
router.go(-1)
```

#### push 和 replace 区别

- `push` 会新增历史记录。
- `replace` 会替换当前历史记录，返回时不会回到当前页。

---

### 9. 动态路由是什么？

#### 核心回答

动态路由是指路由路径中包含动态参数，或者运行时根据权限动态添加路由。

#### 路径参数

```js
{
  path: '/user/:id',
  component: UserDetail
}
```

访问：

```text
/user/1001
```

#### 动态添加路由

```js
router.addRoute({
  path: '/admin',
  component: () => import('@/views/Admin.vue')
})
```

---

### 10. 如何做权限路由？

#### 常见流程

1. 用户登录，获取 token。
2. 根据 token 获取用户信息和权限列表。
3. 根据权限过滤路由表。
4. 使用 `router.addRoute` 动态添加可访问路由。
5. 在全局路由守卫中判断是否登录、是否有权限。
6. 根据权限生成菜单。

#### 简化示例

```js
router.beforeEach(async (to) => {
  const token = getToken()

  if (!token && to.path !== '/login') {
    return '/login'
  }

  if (token && !permissionStore.hasRoutes) {
    await permissionStore.generateRoutes()
    return to.fullPath
  }
})
```

---

## 六、状态管理（Vuex / Pinia）

### 1. Vuex 核心概念有哪些？

Vuex 常见核心概念：

| 概念 | 说明 |
| --- | --- |
| state | 状态数据 |
| getter | 类似 computed，对 state 派生计算 |
| mutation | 同步修改 state |
| action | 处理异步逻辑，提交 mutation |
| module | 模块化 store |

#### 流程

```text
组件 dispatch action
        ↓
action 异步请求
        ↓
commit mutation
        ↓
mutation 修改 state
        ↓
组件视图更新
```

---

### 2. 为什么 mutation 必须同步？

#### 核心回答

因为 Vuex 需要通过 mutation 记录每一次状态变化，方便 devtools 调试和时间旅行。如果 mutation 是异步的，就无法准确追踪状态是在什么时候改变的。

#### 面试表达

mutation 必须同步，是为了保证状态变更可追踪。异步逻辑应该放在 action 中，异步完成后再 commit mutation 修改 state。

---

### 3. action 和 mutation 区别？

| 对比项 | action | mutation |
| --- | --- | --- |
| 是否可异步 | 可以 | 不建议，必须同步 |
| 作用 | 业务逻辑、接口请求 | 修改 state |
| 调用方式 | dispatch | commit |
| 是否直接改 state | 不直接 | 直接修改 |

---

### 4. Vuex 的缺点？

常见缺点：

1. 写法较繁琐。
2. mutation/action/getter 概念较多。
3. TypeScript 支持不够自然。
4. 模块化使用起来比较重。
5. 小项目中容易显得复杂。

---

### 5. Pinia 相比 Vuex 的优势？

#### 核心回答

Pinia 是 Vue 官方推荐的新一代状态管理方案。相比 Vuex，它写法更简单，去掉 mutation，TypeScript 支持更好，模块化更自然。

#### 优势

1. 没有 mutation，action 可同步也可异步。
2. TypeScript 类型推导更好。
3. 每个 store 天然模块化。
4. 代码更简洁。
5. 支持 Vue Devtools。

#### 示例

```js
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    name: 'Tom'
  }),
  actions: {
    setName(name) {
      this.name = name
    }
  }
})
```

---

### 6. Pinia 如何做模块化？

#### 核心回答

Pinia 通过多个 store 文件天然实现模块化。

#### 示例目录

```text
src/stores/
  user.js
  permission.js
  product.js
```

#### user store

```js
export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null
  }),
  actions: {
    setToken(token) {
      this.token = token
    }
  }
})
```

#### permission store

```js
export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: []
  })
})
```

---

### 7. Pinia 如何持久化？

常见方式：

1. 手动使用 localStorage/sessionStorage。
2. 使用插件，例如 pinia-plugin-persistedstate。
3. 自己写 Pinia plugin。

#### 手动示例

```js
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || ''
  }),
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    }
  }
})
```

#### 插件思路

```js
export const useUserStore = defineStore('user', {
  state: () => ({
    token: ''
  }),
  persist: true
})
```

---

### 8. 多个组件共享状态的最佳方式？

#### 简单场景

父子组件：props / emit。  
兄弟组件：父组件中转。

#### 中等复杂度

局部跨层级：provide/inject。

#### 大型项目

全局共享状态：Pinia。

#### 面试表达

如果只是父子通信，我会优先用 props 和 emit。如果多个页面或多个无直接关系组件共享登录信息、权限、用户信息、购物车等状态，我会使用 Pinia。

---

## 七、性能优化

### 1. 如何减少组件重新渲染？

常见方法：

1. 合理拆分组件。
2. 使用 computed 缓存派生数据。
3. 避免在模板中写复杂表达式。
4. 列表加稳定 key。
5. 使用 `v-once` 渲染静态内容。
6. 使用 `v-memo` 跳过不必要更新。
7. 大列表使用虚拟滚动。
8. 不滥用 deep watch。
9. 避免父组件频繁传递新对象或新函数。
10. 使用 keep-alive 缓存页面。

---

### 2. v-once 的作用？

#### 核心回答

`v-once` 只渲染元素或组件一次，后续数据变化不会再更新该部分。

#### 示例

```vue
<h1 v-once>{{ title }}</h1>
```

#### 使用场景

1. 静态标题。
2. 不会改变的说明文字。
3. 一次性渲染的静态内容。

---

### 3. v-memo 是什么？

#### 核心回答

`v-memo` 是 Vue3 的性能优化指令，用于根据依赖数组判断是否跳过某一部分的更新。

#### 示例

```vue
<div v-for="item in list" :key="item.id" v-memo="[item.id === selectedId]">
  {{ item.name }}
</div>
```

当依赖没有变化时，Vue 可以跳过该部分更新。

#### 使用场景

大列表中只有部分项会变化时，可以用 `v-memo` 减少更新成本。

---

### 4. 如何做列表虚拟滚动？

#### 核心回答

虚拟滚动是指只渲染当前可视区域内的数据，而不是一次性渲染全部列表。

#### 原理

假设有 10000 条数据，但屏幕上只能看到 20 条。  
虚拟滚动只渲染这 20 条附近的数据，并用占位高度模拟完整滚动条。

#### 实现方式

1. 使用第三方库，例如 `vue-virtual-scroller`。
2. 自己根据滚动位置计算 startIndex 和 endIndex。

#### 简化思路

```text
容器滚动位置 scrollTop
        ↓
计算开始索引 startIndex
        ↓
计算结束索引 endIndex
        ↓
只渲染 list.slice(startIndex, endIndex)
```

---

### 5. 大列表如何优化？

常见方案：

1. 分页加载。
2. 虚拟滚动。
3. 懒加载。
4. 后端分页和搜索。
5. 减少每一行组件复杂度。
6. 避免每一项绑定大量事件。
7. 使用稳定 key。
8. 图片懒加载。
9. 批量更新，避免频繁 setState。
10. 使用 Web Worker 处理复杂计算。

---

### 6. 图片懒加载怎么做？

#### 原生方式

```html
<img src="a.jpg" loading="lazy" />
```

#### IntersectionObserver

```js
const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const img = entry.target
      img.src = img.dataset.src
      observer.unobserve(img)
    }
  })
})
```

#### Vue 指令方式

可以封装自定义指令：

```js
app.directive('lazy', {
  mounted(el, binding) {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        el.src = binding.value
        observer.unobserve(el)
      }
    })
    observer.observe(el)
  }
})
```

---

### 7. keep-alive 使用场景？

适合缓存不希望频繁销毁的组件，例如：

1. 列表页返回后保持筛选条件和滚动位置。
2. 多 tab 页面切换。
3. 表单编辑页面临时保存输入。
4. 成本较高的组件，例如复杂图表。

#### 示例

```vue
<keep-alive include="UserList">
  <RouterView />
</keep-alive>
```

---

### 8. 如何减少首屏加载时间？

常见方案：

1. 路由懒加载。
2. 组件按需加载。
3. 第三方库按需引入。
4. 打包拆包。
5. CDN 加速静态资源。
6. 图片压缩和懒加载。
7. 开启 gzip / brotli。
8. SSR 或 SSG。
9. 预加载关键资源。
10. 删除无用代码。

---

### 9. 如何分析页面性能？

常用工具：

1. Chrome DevTools Performance。
2. Lighthouse。
3. Network 面板分析资源加载。
4. Vue Devtools 查看组件更新。
5. 打包分析工具，例如 rollup-plugin-visualizer。
6. Web Vitals 指标。

#### 常见指标

| 指标 | 含义 |
| --- | --- |
| FCP | 首次内容绘制 |
| LCP | 最大内容绘制 |
| CLS | 布局偏移 |
| INP | 交互响应 |
| TTFB | 首字节时间 |

---

### 10. Vue 项目如何拆包？

常见方式：

1. 路由级懒加载。
2. 组件异步加载。
3. 第三方库单独拆包。
4. 使用 Vite/Rollup 的 manualChunks。
5. CDN 外链大型库。

#### Vite 示例

```js
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          echarts: ['echarts']
        }
      }
    }
  }
}
```

---

## 八、Vue3 新特性（重点）

### 1. Composition API 优点？

#### 核心回答

Composition API 可以按功能组织代码，而不是按 options 分类组织代码，适合复杂组件逻辑复用和 TypeScript 类型推导。

#### Options API 问题

```js
export default {
  data() {},
  computed: {},
  methods: {},
  watch: {}
}
```

一个功能可能分散在 data、methods、watch 中。

#### Composition API

```js
function useUser() {
  const user = ref(null)

  async function fetchUser() {}

  return { user, fetchUser }
}
```

同一个功能可以集中维护。

---

### 2. setup 为什么更好？

#### 优点

1. 更适合逻辑复用。
2. 更接近普通 JavaScript 函数。
3. TypeScript 支持更好。
4. 可以把业务逻辑拆成自定义 Hook。
5. 避免 Options API 中 this 指向问题。

#### 注意

`setup` 不是一定比 Options API 好。  
简单组件使用 Options API 也可以。大型复杂逻辑组件中，Composition API 优势更明显。

---

### 3. script setup 是什么？

#### 核心回答

`<script setup>` 是 Vue3 单文件组件中使用 Composition API 的编译时语法糖。

#### 示例

普通 setup：

```vue
<script>
export default {
  setup() {
    const count = ref(0)
    return { count }
  }
}
</script>
```

script setup：

```vue
<script setup>
import { ref } from 'vue'

const count = ref(0)
</script>
```

优点：

1. 写法更简洁。
2. 顶层变量可直接在模板中使用。
3. defineProps / defineEmits 等宏更方便。
4. TypeScript 支持更自然。

---

### 4. Teleport 的作用？

#### 核心回答

Teleport 可以把组件模板的一部分渲染到当前组件 DOM 层级之外的指定位置。

#### 示例

```vue
<Teleport to="body">
  <div class="modal">弹窗内容</div>
</Teleport>
```

#### 使用场景

1. 弹窗。
2. 消息提示。
3. 下拉浮层。
4. 全局 loading。

#### 为什么需要？

弹窗如果放在深层组件内部，可能受到父元素的 `overflow:hidden`、`z-index` 影响。  
Teleport 可以直接渲染到 `body` 下。

---

### 5. Suspense 的使用场景？

#### 核心回答

Suspense 用于处理异步组件或异步 setup，在异步内容加载完成前显示 fallback 内容。

#### 示例

```vue
<Suspense>
  <template #default>
    <AsyncComponent />
  </template>

  <template #fallback>
    <div>加载中...</div>
  </template>
</Suspense>
```

#### 使用场景

1. 异步组件加载。
2. 页面骨架屏。
3. 异步 setup。
4. 需要统一 loading 状态的组件树。

---

### 6. Fragment 是什么？

#### 核心回答

Fragment 指 Vue3 组件支持多个根节点，不再要求组件模板必须只有一个根元素。

#### Vue2

```vue
<template>
  <div>
    <Header />
    <Main />
  </div>
</template>
```

#### Vue3

```vue
<template>
  <Header />
  <Main />
</template>
```

Vue3 不需要额外包一层无意义的 div。

---

### 7. 自定义 Hook 如何写？

#### 核心回答

自定义 Hook 是把可复用的组合式逻辑封装成函数，通常以 `useXxx` 命名。

#### 示例：useCounter

```js
import { ref } from 'vue'

export function useCounter() {
  const count = ref(0)

  function increment() {
    count.value++
  }

  return {
    count,
    increment
  }
}
```

使用：

```vue
<script setup>
import { useCounter } from '@/hooks/useCounter'

const { count, increment } = useCounter()
</script>
```

#### 使用场景

1. 请求逻辑。
2. 表单逻辑。
3. 分页逻辑。
4. 权限逻辑。
5. WebSocket 逻辑。

---

### 8. defineExpose 的作用？

#### 核心回答

`defineExpose` 用于在 `<script setup>` 中显式暴露组件内部属性或方法给父组件通过 ref 调用。

#### 子组件

```vue
<script setup>
function open() {
  console.log('open modal')
}

defineExpose({
  open
})
</script>
```

#### 父组件

```vue
<Child ref="childRef" />
```

```js
const childRef = ref(null)
childRef.value.open()
```

#### 注意

不要滥用 ref 调用子组件方法。优先 props/emit。  
只有弹窗打开、表单校验等命令式场景比较适合。

---

### 9. defineProps / defineEmits 是什么？

#### defineProps

用于在 `<script setup>` 中声明 props。

```js
const props = defineProps({
  title: String
})
```

#### defineEmits

用于声明子组件可以触发的事件。

```js
const emit = defineEmits(['submit'])
emit('submit', data)
```

#### 特点

它们是编译器宏，不需要 import，编译后会被处理掉。

---

### 10. Vue3 为什么更快？

主要原因：

1. Proxy 响应式系统更高效。
2. 编译器优化更多。
3. 静态节点提升。
4. Patch Flag 精准标记动态节点。
5. Tree-shaking 支持更好。
6. Fragment 减少无意义 DOM。
7. Composition API 更利于逻辑组织和压缩。
8. diff 算法优化。

#### 面试表达

Vue3 更快不是单一原因，而是响应式系统、编译器和运行时一起优化。比如 Proxy 改善响应式能力，编译阶段会标记动态节点，运行时 diff 时可以更精准地更新。

---

## 九、真实项目 & 场景题

### 1. 表格编辑如何实现？

#### 常见方案

1. 每一行维护编辑状态。
2. 点击编辑时复制当前行数据。
3. 编辑完成后校验并提交。
4. 保存成功后更新表格数据。
5. 取消时恢复原数据。

#### 示例结构

```js
const tableData = ref([])
const editingId = ref(null)
const editForm = reactive({})

function editRow(row) {
  editingId.value = row.id
  Object.assign(editForm, row)
}

async function saveRow() {
  await api.update(editForm)
  const index = tableData.value.findIndex(item => item.id === editForm.id)
  tableData.value[index] = { ...editForm }
  editingId.value = null
}
```

#### 注意点

1. 不要直接修改原始行数据，避免取消时无法恢复。
2. 表单校验要和保存动作结合。
3. 大表格要考虑分页或虚拟滚动。
4. 并发编辑要以后端数据为准。

---

### 2. 表单校验怎么做？

#### 常见方式

1. 使用组件库表单校验，例如 Element Plus。
2. 自己封装校验函数。
3. 前端校验 + 后端校验双重保证。

#### 校验内容

1. 必填。
2. 长度。
3. 格式，如邮箱、手机号。
4. 数值范围。
5. 自定义业务规则。

#### 流程

```text
用户提交
  ↓
前端校验
  ↓
校验通过
  ↓
调用接口
  ↓
后端校验
  ↓
保存成功或显示错误
```

---

### 3. 登录鉴权流程？

#### 常见流程

1. 用户输入账号密码。
2. 调用登录接口。
3. 后端返回 token。
4. 前端保存 token。
5. 请求拦截器携带 token。
6. 路由守卫判断是否登录。
7. 获取用户信息和权限。
8. token 过期时刷新或退出登录。

#### 示例

```js
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

---

### 4. 权限菜单怎么做？

#### 常见方案

1. 后端返回用户权限码或菜单树。
2. 前端根据权限过滤路由。
3. 根据过滤后的路由生成菜单。
4. 按钮级权限用指令或函数判断。

#### 示例

```js
function hasPermission(code) {
  return userStore.permissions.includes(code)
}
```

模板：

```vue
<button v-if="hasPermission('user:add')">新增用户</button>
```

#### 自定义指令

```js
app.directive('permission', {
  mounted(el, binding) {
    if (!hasPermission(binding.value)) {
      el.remove()
    }
  }
})
```

---

### 5. 接口失败怎么统一处理？

#### 使用 axios 响应拦截器

```js
axios.interceptors.response.use(
  response => response.data,
  error => {
    const status = error.response?.status

    if (status === 401) {
      // 跳转登录或刷新 token
    } else if (status === 403) {
      // 无权限
    } else {
      // 通用错误提示
    }

    return Promise.reject(error)
  }
)
```

#### 常见处理

1. 401：未登录或 token 过期。
2. 403：无权限。
3. 404：资源不存在。
4. 500：服务器错误。
5. 网络错误：提示检查网络。
6. 业务错误码：根据后端约定处理。

---

### 6. 请求防抖节流怎么做？

#### 防抖 debounce

一段时间内多次触发，只执行最后一次。  
适合搜索输入框。

```js
function debounce(fn, delay) {
  let timer = null
  return function (...args) {
    clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}
```

#### 节流 throttle

一段时间内最多执行一次。  
适合滚动、窗口 resize。

```js
function throttle(fn, delay) {
  let last = 0
  return function (...args) {
    const now = Date.now()
    if (now - last >= delay) {
      last = now
      fn.apply(this, args)
    }
  }
}
```

---

### 7. token 过期如何刷新？

#### 常见流程

1. access token 用于接口请求，时间较短。
2. refresh token 用于刷新 access token，时间较长。
3. 接口返回 401 时，前端调用刷新接口。
4. 刷新成功后重试原请求。
5. 刷新失败则退出登录。

#### 注意并发问题

多个请求同时 401 时，不应该同时刷新 token。  
通常需要一个刷新队列。

#### 简化思路

```text
请求返回 401
  ↓
判断是否正在刷新 token
  ↓
如果没有，发起刷新
  ↓
如果正在刷新，把请求加入队列
  ↓
刷新成功后重放队列请求
```

---

### 8. 多标签页登录同步？

#### 场景

用户在一个标签页退出登录，其他标签页也应该同步退出。

#### 方案一：storage 事件

```js
window.addEventListener('storage', (event) => {
  if (event.key === 'token' && !event.newValue) {
    router.push('/login')
  }
})
```

当一个标签页修改 localStorage，其他标签页会收到 storage 事件。

#### 方案二：BroadcastChannel

```js
const channel = new BroadcastChannel('auth')

channel.postMessage({ type: 'logout' })

channel.onmessage = (event) => {
  if (event.data.type === 'logout') {
    router.push('/login')
  }
}
```

---

### 9. 如何封装 axios？

#### 常见封装内容

1. baseURL。
2. timeout。
3. 请求头。
4. 请求拦截器。
5. 响应拦截器。
6. 错误统一处理。
7. token 自动携带。
8. token 刷新。
9. loading 控制。
10. 取消重复请求。

#### 示例

```js
import axios from 'axios'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

service.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  response => response.data,
  error => {
    return Promise.reject(error)
  }
)

export default service
```

---

### 10. 如何做错误监控？

#### 前端错误类型

1. JS 运行时错误。
2. Promise 未捕获错误。
3. Vue 组件错误。
4. 接口错误。
5. 资源加载错误。
6. 白屏错误。
7. 性能异常。

#### Vue 错误捕获

```js
app.config.errorHandler = (err, instance, info) => {
  console.error(err, info)
}
```

#### 全局错误捕获

```js
window.addEventListener('error', (event) => {
  console.log(event.message)
})

window.addEventListener('unhandledrejection', (event) => {
  console.log(event.reason)
})
```

#### 上报内容

1. 错误信息。
2. 堆栈。
3. 页面 URL。
4. 用户信息。
5. 浏览器信息。
6. 发生时间。
7. 接口请求信息。

---

## 十、工程化 & 构建

### 1. Vite 为什么快？

#### 核心回答

Vite 在开发环境中利用浏览器原生 ESM，不需要像传统打包工具一样先把整个项目打包完成再启动，所以冷启动更快。依赖预构建通常使用 esbuild，速度也很快。

#### 详细解释

传统工具：

```text
启动开发服务器
  ↓
扫描项目
  ↓
打包整个应用
  ↓
浏览器访问
```

Vite：

```text
启动开发服务器
  ↓
浏览器请求哪个模块
  ↓
Vite 按需转换哪个模块
```

所以项目越大，Vite 开发启动优势越明显。

---

### 2. Vite 和 Webpack 区别？

| 对比项 | Vite | Webpack |
| --- | --- | --- |
| 开发模式 | 原生 ESM，按需加载 | 通常先打包 |
| 冷启动 | 快 | 大项目较慢 |
| HMR | 快，模块级更新 | 依赖打包图 |
| 生产构建 | Rollup | Webpack |
| 配置复杂度 | 相对简单 | 功能强但配置复杂 |
| 生态 | 现代项目常用 | 老项目和复杂构建生态成熟 |

#### 面试表达

Vite 的开发体验更快，主要因为开发时不需要完整打包，而是基于原生 ESM 按需提供模块。Webpack 功能和生态非常成熟，适合复杂历史项目。现在 Vue3 新项目通常更推荐 Vite。

---

### 3. Vite 的 HMR 原理？

#### 核心回答

Vite 通过 WebSocket 通知浏览器某个模块发生变化，浏览器只重新请求并替换变化的模块，而不是刷新整个页面。

#### 流程

```text
文件修改
  ↓
Vite 监听到变化
  ↓
通过 WebSocket 通知浏览器
  ↓
浏览器重新请求变化模块
  ↓
局部更新页面
```

#### 好处

1. 更新速度快。
2. 尽量保留页面状态。
3. 不需要整个应用重新加载。

---

### 4. Tree Shaking 原理？

#### 核心回答

Tree Shaking 是打包时删除未使用代码的优化手段，主要依赖 ES Module 的静态结构。

#### 示例

```js
// utils.js
export function a() {}
export function b() {}
```

```js
import { a } from './utils'
a()
```

如果 `b` 没被使用，生产构建时可能被删除。

#### 注意

Tree Shaking 更适合 ESM，因为 import/export 是静态的。  
CommonJS 的 require 是运行时加载，静态分析较困难。

---

### 5. ESM 和 CommonJS 区别？

| 对比项 | ESM | CommonJS |
| --- | --- | --- |
| 导入 | import | require |
| 导出 | export | module.exports |
| 加载时机 | 静态编译分析 | 运行时加载 |
| 是否利于 Tree Shaking | 是 | 较差 |
| 常见环境 | 浏览器、现代前端 | Node.js 传统模块 |

#### 示例

ESM：

```js
import { add } from './math.js'
export const count = 1
```

CommonJS：

```js
const { add } = require('./math')
module.exports = { count: 1 }
```

---

### 6. import 和 require 区别？

#### import

1. ESM 语法。
2. 通常在文件顶部静态导入。
3. 支持 Tree Shaking。
4. 可以使用动态 import。

```js
import Vue from 'vue'
```

动态 import：

```js
const module = await import('./module.js')
```

#### require

1. CommonJS 语法。
2. 运行时加载。
3. 可以写在条件语句中。
4. 不利于静态分析。

```js
const fs = require('fs')
```

---

### 7. 如何配置环境变量？

#### Vite 环境变量文件

```text
.env
.env.development
.env.test
.env.production
```

#### 注意

Vite 中暴露给客户端的变量必须以 `VITE_` 开头。

```env
VITE_API_BASE_URL=https://api.example.com
```

使用：

```js
const baseURL = import.meta.env.VITE_API_BASE_URL
```

---

### 8. 如何区分开发 / 测试 / 生产环境？

#### package.json

```json
{
  "scripts": {
    "dev": "vite --mode development",
    "build:test": "vite build --mode test",
    "build": "vite build --mode production"
  }
}
```

#### 环境文件

```text
.env.development
.env.test
.env.production
```

#### 代码中判断

```js
if (import.meta.env.MODE === 'production') {
  console.log('生产环境')
}
```

常见变量：

```js
import.meta.env.MODE
import.meta.env.DEV
import.meta.env.PROD
```

---

### 9. 打包体积过大怎么优化？

常见方案：

1. 路由懒加载。
2. 组件按需加载。
3. 第三方库按需引入。
4. 使用 Tree Shaking。
5. 分析 bundle 体积。
6. 图片压缩。
7. 使用 CDN。
8. 删除无用依赖。
9. 拆分 vendor chunk。
10. gzip / brotli 压缩。

#### 分析工具

```bash
npm install rollup-plugin-visualizer -D
```

---

### 10. 代码分割怎么做？

#### 路由级代码分割

```js
const User = () => import('@/views/User.vue')
```

#### 组件级异步加载

```js
import { defineAsyncComponent } from 'vue'

const AsyncComp = defineAsyncComponent(() => import('./AsyncComp.vue'))
```

#### 手动拆包

```js
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            return 'vendor'
          }
        }
      }
    }
  }
}
```

---

## 面试复习建议

### 1. 回答结构

回答面试题时可以按这个结构：

```text
先说结论
  ↓
解释原理
  ↓
给一个例子
  ↓
说项目中怎么用
```

例如回答 `v-if 和 v-show 的区别`：

```text
v-if 是条件渲染，条件为 false 时 DOM 不存在；v-show 是显示隐藏，DOM 一直存在，只是 display 控制。
如果切换频率低用 v-if，如果频繁切换用 v-show。
比如弹窗内容很重且不常打开，可以用 v-if；tab 内容频繁切换，可以考虑 v-show 或 keep-alive。
```

### 2. Vue 面试重点优先级

#### 必须掌握

1. Vue 响应式原理。
2. ref 和 reactive。
3. computed / watch。
4. 生命周期。
5. props / emit / v-model。
6. Vue Router 路由守卫。
7. Pinia。
8. axios 封装。
9. 登录鉴权。
10. Vite 基础。

#### 加分项

1. 虚拟 DOM 和 diff。
2. keep-alive。
3. v-memo。
4. 权限路由。
5. token 刷新队列。
6. 性能优化。
7. 错误监控。
8. 打包优化。
9. TypeScript + Vue3。
10. 自定义 Hook。

---

## 参考资料

- Vue 官方文档：Vue 是渐进式 JavaScript 框架，提供可逐步采用的生态能力。
- Vue `<script setup>` 官方文档：`defineProps` 和 `defineEmits` 是 `<script setup>` 中的编译器宏。
- Pinia 官方文档：Pinia 的 state 通常定义为返回初始状态的函数，并支持插件扩展。
- Vite 官方文档：Vite 包含基于原生 ESM 的开发服务器和生产构建能力，开发环境支持快速 HMR。

