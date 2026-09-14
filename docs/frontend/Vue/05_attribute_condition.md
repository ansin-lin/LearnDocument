# 第 5 章 属性、样式与条件渲染

## 本章目标与前置知识

【必须掌握】根据响应式状态绑定HTML属性、Boolean Attribute、class、style和条件分支，并根据切换频率选择`v-if`或`v-show`。批量`v-bind`只要求【会使用、能看懂】。需要掌握ref、事件和JavaScript真假判断。

第2章已经使用`v-bind`绑定过链接，第3章建立了响应式状态，第4章已经能够接收事件。本章系统学习怎样让状态决定HTML属性、CSS类、行内样式以及DOM结构。

本章示例直接使用第3章的响应式状态，并通过第4章的事件修改它，从而观察属性、样式和DOM结构在运行期间怎样变化。

## 1. 固定属性与动态属性

### 1.1 固定属性

```vue
<a href="/help" title="打开帮助页面">帮助</a>
```

没有`v-bind`的属性值是固定字符串。即使写成`href="detailUrl"`，浏览器得到的也是文字`detailUrl`，不是JavaScript变量。

### 1.2 使用v-bind

```vue
<script setup lang="ts">
const detailUrl = '/tasks/101'
const linkLabel = '查看规格确认任务'
</script>

<template>
  <a v-bind:href="detailUrl" v-bind:aria-label="linkLabel">查看详情</a>
</template>
```

`v-bind:href`表示计算表达式`detailUrl`，把结果设置到`href`。`v-bind:`通常简写成冒号：

```vue
<a :href="detailUrl" :aria-label="linkLabel">查看详情</a>
```

| 写法 | Vue如何解释引号内容 |
| --- | --- |
| `href="detailUrl"` | 固定字符串`detailUrl` |
| `:href="detailUrl"` | JavaScript变量的值 |
| `:href="'/tasks/101'"` | JavaScript字符串表达式 |

第三种虽然能运行，但固定值直接写普通HTML属性更清楚。

## 2. 属性表达式可以写什么

属性绑定的值可以是单个TypeScript/JavaScript表达式：

```vue
<script setup lang="ts">
const taskId = 101
const status = 'todo'
const assignee = '田中'
</script>

<template>
  <a :href="'/tasks/' + taskId">详情</a>
  <span :title="assignee + '担当'">{{ assignee }}</span>
  <button :aria-pressed="status === 'done' ? 'true' : 'false'" type="button">
    状态
  </button>
</template>
```

模板表达式应保持短小。长字符串组合和复杂判断会在第7章移入计算属性。属性名称也可以动态绑定，如`:[attributeName]="value"`，但普通业务页面属性名基本固定，只需能读懂这种写法。

## 3. 布尔属性

`disabled`、`checked`、`required`、`readonly`和`multiple`属于常见布尔属性。它们关注属性是否存在，而不是字符串内容：

```vue
<script setup lang="ts">
const saving = true
const canEdit = false
</script>

<template>
  <button type="button" :disabled="saving">保存</button>
  <input :readonly="!canEdit" value="规格确认">
</template>
```

绑定值为真时包含属性，为假时移除属性。固定写法`disabled="false"`仍然存在`disabled`属性，可能继续被浏览器视为禁用，因此必须使用`:disabled="false"`或直接不写该属性。

ARIA属性不是HTML布尔属性。例如`aria-expanded`通常需要字符串`'true'`或`'false'`，不能因为值为假就随意删除。无障碍属性应按照对应ARIA规范使用。

## 4. null和undefined怎样处理

```vue
<script setup lang="ts">
const description = null
const testId = undefined
</script>

<template>
  <button :title="description" :data-testid="testId" type="button">确认</button>
</template>
```

绑定值为`null`或`undefined`时，Vue通常移除对应属性。这适合表达“当前没有这个可选属性”，也能避免把文字`null`显示到HTML中。

空字符串`''`与`null`含义不同。是否允许空值、应该删除属性还是提供默认文案，要按画面规格决定。

## 5. 同时绑定多个属性

```vue
<script setup lang="ts">
const buttonAttributes = {
  id: 'save-button',
  disabled: false,
  'aria-label': '保存任务',
}
</script>

<template>
  <button v-bind="buttonAttributes" type="button">保存</button>
</template>
```

不带参数的`v-bind="对象"`会按对象的键绑定多个属性。它适合已经整理好的HTML属性集合，不应用来隐藏组件真正依赖的业务数据。第12章会在组件Attributes透传中再次使用这种写法。

对象键要使用真实属性名；包含连字符的`aria-label`需要加引号。对象来源不可信时不能直接全部绑定，应先挑选允许的属性。

## 6. class绑定解决什么问题

CSS已经负责定义外观，Vue只需要根据值决定使用哪些类。这样比在JavaScript中手工修改`classList`更符合声明式模板。

### 6.1 对象写法

```vue
<script setup lang="ts">
const status = 'todo'
const priority = 'high'
</script>

<template>
  <p
    class="task"
    :class="{ 'is-completed': status === 'done', urgent: priority === 'high' }"
  >
    规格确认
  </p>
</template>

<style scoped>
.task { padding: 8px; }
.is-completed { text-decoration: line-through; }
.urgent { color: #b91c1c; font-weight: 700; }
</style>
```

对象的键是类名，值转换为`true`时添加该类。固定的`task`类和动态类会合并，不会互相覆盖。

类名含连字符时加引号：

```vue
<span :class="{ 'is-completed': status === 'done' }">任务状态</span>
```

### 6.2 数组写法

```vue
<script setup lang="ts">
const sizeClass = 'badge--large'
const priorityClass = 'badge--high'
</script>

<template>
  <span :class="['badge', sizeClass, priorityClass]">高</span>
</template>
```

数组适合组合多个已经确定的类名。数组中也可以包含对象：

```vue
<span :class="['badge', { 'is-active': true }]">处理中</span>
```

### 6.3 字符串写法

```vue
<span :class="priorityClass">优先级</span>
```

单个类名直接绑定字符串即可。项目中选择最容易理解的形式，不必为了展示语法把简单类名写成复杂对象。

## 7. style绑定

### 7.1 对象写法

```vue
<script setup lang="ts">
const progress = 60
const barColor = '#2563eb'
</script>

<template>
  <div
    :style="{
      width: progress + '%',
      backgroundColor: barColor,
    }"
  >
    {{ progress }}%
  </div>
</template>
```

属性名可以使用JavaScript形式`backgroundColor`，也可以写`'background-color'`。需要单位的CSS值必须包含单位；`width: 60`不能表达`60%`。

部分CSS属性允许数字，由Vue按规则补充`px`，但不同属性规则不同。新人项目建议把单位明确写出来，避免误解。

### 7.2 数组写法和多值

```vue
<script setup lang="ts">
const baseStyle = { padding: '8px', color: '#111827' }
const emphasisStyle = { fontWeight: '700' }
</script>

<template>
  <p :style="[baseStyle, emphasisStyle]">重要任务</p>
</template>
```

数组中的后一个对象会覆盖前面同名属性。固定颜色、间距和字号仍应放在CSS类中；行内`style`主要用于进度、坐标、用户选择主题值等真正来自数据的样式。

## 8. class还是style

| 需求 | 推荐 |
| --- | --- |
| 完成、错误、选中等有限状态 | 动态`class` |
| 团队设计系统中的颜色和间距 | CSS类或UI框架类 |
| 进度宽度、坐标等连续数值 | 动态`style` |
| 大量固定样式 | `<style>`或公共CSS |

优先使用有业务含义的类名，例如`is-error`、`is-completed`。这有利于统一视觉规则、响应式布局和Review。

## 9. v-if、v-else-if和v-else

```vue
<script setup lang="ts">
const status = 'loading'
</script>

<template>
  <p v-if="status === 'idle'">尚未读取</p>
  <p v-else-if="status === 'loading'">读取中…</p>
  <p v-else-if="status === 'error'">读取失败</p>
  <p v-else>读取完成</p>
</template>
```

`v-if`条件为真时创建DOM，为假时不创建。`v-else-if`和`v-else`必须紧邻前一分支，中间不能插入另一个普通元素。

依次通过事件把`status`改为`idle`、`loading`、`error`和`success`，可以观察每次只有一个分支存在。

## 10. 使用template组合多个元素

```vue
<script setup lang="ts">
const canEdit = true
</script>

<template>
  <section>
    <h2>任务详情</h2>

    <template v-if="canEdit">
      <button type="button">编辑</button>
      <button type="button">删除</button>
    </template>
  </section>
</template>
```

这里的`template`只用于组织条件分支，不会在最终DOM中生成额外标签。不要为了条件判断增加没有语义的多层`div`。

## 11. v-show与v-if的区别

```vue
<p v-show="showHelp">请输入100字以内的任务名称。</p>
```

`v-show`始终创建DOM，只切换CSS的`display`；不支持`v-else`，也不能直接用于`template`。

| 情况 | 推荐 |
| --- | --- |
| 初次可能完全不需要创建 | `v-if` |
| 内容复杂、包含子组件 | `v-if` |
| 简单提示频繁显示和隐藏 | `v-show` |
| 需要使用`v-else`分支 | `v-if` |

可以在浏览器Elements中确认：`v-if="false"`的元素不存在，`v-show="false"`的元素仍存在但具有隐藏样式。

使用`v-if`移除输入框或组件后，其中尚未保存的DOM状态也会消失。是否保留状态要根据画面规格决定。

## 12. 条件值与JavaScript真假判断

条件指令遵循JavaScript真假值规则。`false`、`0`、`''`、`null`和`undefined`会被当作假；空数组`[]`和空对象`{}`仍然是真。

因此不能用`v-if="tasks"`判断数组是否有数据，应明确判断长度：

```vue
<p v-if="tasks.length === 0">没有任务</p>
```

列表渲染会在第6章详细学习。本节先避免把“数组存在”和“数组有数据”混为一谈。

## 13. 用一个状态值表示页面阶段

页面常有`idle`、`loading`、`success`和`error`。如果使用多个布尔变量，可能出现“正在读取”和“读取失败”同时为真的矛盾状态：

```ts
import { ref } from 'vue'

const status = ref('idle')
```

用一个字段保存当前阶段，再通过互斥的`v-if`分支显示界面，更容易与接口规格和测试项目对应。第17章会把它用于真实请求。

## 14. 安全和权限边界

- `:href`和`:src`的数据来自外部时，应限制允许的协议和来源，不能直接接受危险URL。
- 不把不可信对象整体交给`v-bind`。
- 不把用户输入直接绑定为任意行内样式或类名。
- 隐藏编辑、删除按钮只改变画面，不能代替服务端权限校验。
- 不用颜色作为唯一状态提示，同时提供文字或其他可识别信息。

## 15. 常见错误

### 15.1 把变量名写成固定属性

```vue
<!-- 错误：跳转到文字detailUrl代表的相对地址 -->
<a href="detailUrl">详情</a>

<!-- 正确 -->
<a :href="detailUrl">详情</a>
```

### 15.2 在动态属性中写双花括号

```vue
<!-- 错误 -->
<a :href="{{ detailUrl }}">详情</a>

<!-- 正确 -->
<a :href="detailUrl">详情</a>
```

双花括号用于元素文本，冒号用于属性。

### 15.3 同一元素同时使用v-if和后续的v-for

条件和循环写在同一元素时优先级容易造成误解。第6章会先生成筛选结果，再渲染列表；整块显示条件写在外层。

### 15.4 用v-if处理响应式布局

屏幕宽度导致的桌面/手机排版优先使用CSS媒体查询。只有结构或业务逻辑确实不同才考虑条件渲染。

## 16. 本章完整示例

```vue
<script setup lang="ts">
const task = {
  id: 101,
  title: '规格确认',
  priority: 'high',
  status: 'todo',
}
const status = 'success'
const canEdit = true
const progress = 60
</script>

<template>
  <main class="page">
    <p v-if="status === 'loading'">读取中…</p>
    <p v-else-if="status === 'error'">读取失败</p>

    <article
      v-else
      class="task-card"
      :class="{
        'task-card--urgent': task.priority === 'high',
        'task-card--completed': task.status === 'done',
      }"
    >
      <h1>{{ task.title }}</h1>
      <a :href="'/tasks/' + task.id">查看详情</a>
      <div class="progress" :style="{ width: progress + '%' }">
        {{ progress }}%
      </div>

      <template v-if="canEdit">
        <button type="button">编辑</button>
        <button type="button">删除</button>
      </template>
    </article>
  </main>
</template>

<style scoped>
.task-card { padding: 16px; border: 1px solid #d1d5db; }
.task-card--urgent { border-color: #b91c1c; }
.task-card--completed { opacity: 0.65; }
.progress { margin-top: 12px; color: white; background: #2563eb; }
</style>
```

分别通过第4章事件修改`status`、`priority`、`canEdit`和`progress`，检查DOM、属性和样式如何随响应式状态变化。

## 17. 练习

1. 分别练习固定属性、`:属性`和对象形式`v-bind`。
2. 用字符串、对象和数组三种方式绑定class，并说明适用情况。
3. 用style对象绑定进度宽度，确认单位不可遗漏。
4. 根据`idle/loading/success/error`显示互斥页面状态。
5. 使用`template v-if`同时控制两个操作按钮。
6. 在Elements中比较`v-if="false"`和`v-show="false"`。
7. 检查一个动态链接、一个布尔属性和一个ARIA属性的最终HTML。
8. 为完整示例增加切换状态的按钮，验证响应式状态变化后属性、样式和条件分支同步更新。

## 本章检查点

- 能区分固定属性值和JavaScript表达式。
- 能说明布尔属性与普通字符串属性的不同。
- 知道`null`和`undefined`会使动态属性被移除。
- 能使用字符串、对象和数组形式绑定class与style。
- 能根据场景选择class或style。
- 能使用`v-if`、`v-else-if`、`v-else`和`template`组织分支。
- 能解释`v-if`与`v-show`在DOM和切换成本上的区别。
- 知道空数组是真值，判断空列表需要检查`length`。
- 知道属性绑定、按钮隐藏和动态样式都有安全边界。






