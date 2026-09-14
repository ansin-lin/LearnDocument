# 第 2 章 单文件组件与模板语法

## 本章目标与前置知识

【必须掌握】说明组件与SFC的含义，区分`script setup`、`template`、`style scoped`，使用插值和基础`v-bind`。`v-html`只要求【会读即可】并理解安全风险。

```text
script setup提供数据和函数 → template使用 → Vue编译 → 浏览器DOM
```

模板不是JavaScript字符串，也不是浏览器最终收到的原始HTML，而是Vue编译的组件结构。

第一章创建的项目中出现了`App.vue`。本章先解释组件和`.vue`文件是什么，再分别认识逻辑、模板和样式区域，最后学习模板中最基本的数据绑定语法。

## 1. 什么是组件

一个完整页面通常包含导航、搜索条件、任务列表、任务项和表单等部分。Vue可以把这些部分拆成独立的**组件（Component）**。

组件不是某一个HTML标签，而是一个可以独立维护和重复使用的界面单元。它通常同时包含：

- 要显示什么结构；
- 显示和操作需要的数据；
- 用户操作时执行的逻辑；
- 只属于这部分界面的样式。

例如任务管理页面可以逐步拆成：

```text
App.vue
├─ TaskSearch.vue
├─ TaskList.vue
│  └─ TaskItem.vue
└─ TaskForm.vue
```

当前只修改根组件`App.vue`。第10章再学习怎样创建和组合多个组件。

## 2. 什么是单文件组件

**单文件组件（Single-File Component，简称SFC）**是以`.vue`结尾的组件文件。它把一个组件相关的JavaScript、HTML模板和CSS样式放在同一个文件中。

```vue
<script setup lang="ts">
// TypeScript逻辑
</script>

<template>
  <!-- HTML结构和Vue模板语法 -->
</template>

<style scoped>
/* CSS样式 */
</style>
```

这里的“单文件”不是说整个网站只能有一个文件，而是说**一个组件的相关内容集中在一个`.vue`文件中**。实际项目会包含许多单文件组件。

单文件组件不是浏览器原生格式。Vite通过Vue插件把它编译成浏览器能够执行的JavaScript和CSS，所以不能直接双击`.vue`文件运行。

## 3. 单文件组件的三个部分

### 3.1 script：组件逻辑

本课程统一使用`<script setup lang="ts">`：

```vue
<script setup lang="ts">
const taskTitle = 'Vue教程整理'
const assignee = '田中'
const status = 'todo'
</script>
```

`<script setup lang="ts">`内部编写TypeScript。已经学过的JavaScript语法仍可直接使用，并能获得静态类型检查。

其中的顶层变量、函数和导入内容可以直接在同一文件的`<template>`中使用，不需要再手工组成对象或写`return`。

```vue
<script setup lang="ts">
const dueDate = '2026-09-30'
const displayDate = dueDate.replaceAll('-', '/')
</script>
```

`script setup`不是在HTML中插入普通`<script>`标签。它由Vue编译器处理，并在每个组件实例创建时执行组件的初始化逻辑。

### 3.2 template：组件结构

`<template>`描述组件最终要显示的HTML结构：

```vue
<template>
  <article class="task-card">
    <h2>{{ taskTitle }}</h2>
    <p>负责人：{{ assignee }}</p>
  </article>
</template>
```

模板以HTML为基础，因此仍要遵守正确的元素语义和嵌套规则。在HTML基础上，Vue增加了插值、属性绑定和指令等模板语法。

模板不会作为一个名为`template`的元素显示在最终页面中。Vue会把它编译为渲染逻辑，根据其中的元素生成和更新DOM。

一个单文件组件只能有一个顶层`<template>`区域。模板内部可以包含一个或多个根元素，但使用职责清楚的容器通常更容易理解组件结构。

### 3.3 style：组件样式

```vue
<style scoped>
.task-card {
  padding: 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
}
</style>
```

`<style>`中编写普通CSS。添加`scoped`后，Vite和Vue编译器会为选择器和当前组件生成匹配标记，让样式主要作用于这个组件的模板，减少与其他组件同名选择器冲突。

`scoped`不是Shadow DOM，也不是安全边界。父组件布局、继承属性以及后续学习的子组件根元素仍可能受到相关样式影响。

没有`scoped`的样式属于全局样式。页面重置、颜色变量等真正需要全局生效的内容通常放在`src/assets`中的公共CSS，再由`main.ts`导入；组件局部外观写在对应组件中。

## 4. 完整读取一个单文件组件

将`src/App.vue`替换为下面的完整文件：

```vue
<script setup lang="ts">
const taskTitle = 'Vue教程整理'
const assignee = '田中'
const status = 'todo'
const detailUrl = '/tasks/1'
</script>

<template>
  <main class="page">
    <article class="task-card">
      <h1>{{ taskTitle }}</h1>
      <p>负责人：{{ assignee }}</p>
      <p>状态：{{ status === 'done' ? '完成' : '未完成' }}</p>
      <a :href="detailUrl">查看任务地址</a>
    </article>
  </main>
</template>

<style scoped>
.page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
}

.task-card {
  padding: 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
}
</style>
```

这段代码的处理关系是：

1. `script setup`准备标题、负责人、状态和链接地址。
2. `template`读取这些值并描述页面结构。
3. Vue把模板编译为渲染逻辑。
4. `style scoped`为当前组件生成局部样式。
5. Vite把处理结果提供给浏览器。

保存文件后，开发服务器应自动更新页面。当前变量都是普通常量，修改源码后页面才变化；第3章先建立响应式状态，第4章再用事件修改状态。

## 5. 文本插值 `{{ }}`

### 5.1 输出文本

双花括号称为**文本插值**：

```vue
<p>{{ taskTitle }}</p>
<p>负责人：{{ assignee }}</p>
```

Vue会计算双花括号中的表达式，并把结果作为文本显示。变量变化时，使用它的位置也会更新。

双花括号只能写在元素内容中，不能代替HTML属性：

```vue
<!-- 错误思路：不要这样绑定属性 -->
<a href="{{ detailUrl }}">详情</a>
```

属性需要使用下一节的`v-bind`。

### 5.2 使用JavaScript表达式

插值和指令值可以使用一个能够得到结果的JavaScript表达式：

```vue
<p>{{ status === 'done' ? '完成' : '未完成' }}</p>
<p>{{ taskTitle.toUpperCase() }}</p>
<p>{{ 1 + 2 }}</p>
```

模板中不能直接写变量声明、`if`语句或`for`语句：

```vue
<!-- 无效：这是语句，不是表达式 -->
<p>{{ const label = '任务' }}</p>
```

模板表达式应保持简短。复杂计算放到第7章的计算属性中，修改状态或发送请求等副作用也不应放进显示表达式。

## 6. 属性绑定 `v-bind`

### 6.1 固定属性与动态属性

```vue
<a href="/help">固定帮助地址</a>
<a v-bind:href="detailUrl">动态任务地址</a>
```

第一个`href`是固定字符串。第二个使用`v-bind:href`，表示把JavaScript表达式`detailUrl`的当前值绑定到`href`属性。

`v-bind:`常用简写是冒号：

```vue
<a :href="detailUrl">动态任务地址</a>
```

### 6.2 布尔属性

```vue
<script setup lang="ts">
const saving = true
</script>

<template>
  <button type="button" :disabled="saving">保存</button>
</template>
```

`disabled`属于布尔属性。当绑定结果为真时，按钮包含禁用属性；为假时不禁用。不要写`disabled="false"`，因为HTML只要存在该布尔属性，就可能仍被视为禁用。

当`v-bind`绑定值为`null`或`undefined`时，对应属性通常会从元素上移除。

## 7. 什么是Vue指令

以`v-`开头的特殊属性叫作**指令（Directive）**。指令根据表达式结果对DOM应用响应式行为。

指令的一般结构可以拆成：

```text
v-指令名:参数.修饰符="表达式"
```

例如：

```vue
<a v-bind:href="detailUrl">详情</a>
```

- `v-bind`是指令名，`href`是参数。
- `v-on`是事件监听指令，例如其中的`click`参数表示点击事件；它的简写是`@click`。
- 修饰符用于补充指令行为，例如后续表单中的`@submit.prevent`。

本章只掌握指令的结构以及`v-bind`。`v-if`、`v-for`、`v-on`和`v-model`会在对应章节结合具体问题讲解。

## 8. 原始HTML与安全边界

文本插值会把字符串作为普通文本处理。如果字符串是`<strong>重要</strong>`，页面会显示这些字符，而不会创建`strong`元素。

Vue提供`v-html`把字符串作为HTML插入，但它可能造成跨站脚本攻击（XSS）。不能把用户输入、接口返回的富文本或其他不可信内容直接交给`v-html`。

```vue
<!-- 只允许已经过可信处理的内容；普通业务文本不要使用 -->
<div v-html="trustedHtml"></div>
```

组件模板应使用真实HTML元素和组件组合，不要通过拼接HTML字符串代替组件。

## 9. 常见错误与项目注意事项

### 9.1 页面没有更新

先确认`npm run dev`仍在运行，再查看终端和浏览器Console。模板引用了不存在的变量、标签未闭合或JavaScript语法错误都会导致编译失败。

### 9.2 在属性中写双花括号

元素内容使用`{{ value }}`，属性使用`:attribute="value"`。两者位置不同。

### 9.3 误以为scoped会冻结全部外部样式

`scoped`主要限制当前样式选择器的匹配范围，不会阻止继承、父级布局或浏览器默认样式。

### 9.4 在模板中堆放复杂逻辑

模板负责描述界面。表达式越来越长时，把计算移入脚本中的命名函数或后续学习的计算属性。

## 10. 练习

1. 用自己的话解释组件、单文件组件和`.vue`文件的关系。
2. 在`App.vue`中分别指出逻辑、模板和样式区域，并说明Vite如何处理它们。
3. 增加负责人、截止日期和优先级变量，通过文本插值显示。
4. 增加详情地址和是否保存中的变量，分别绑定`href`和`disabled`。
5. 故意在属性中写双花括号，观察结果后改为`:href`。
6. 给组件增加局部样式，再暂时移除`scoped`，通过Elements观察生成结果后恢复。
7. 执行`npm run build`，确认单文件组件能够被编译为`dist`中的浏览器资源。

## 本章检查点

- 能说明组件解决什么问题。
- 能说明单文件组件不是整个项目只有一个文件。
- 能解释`script setup`、`template`和`style scoped`各自的职责。
- 能说明`.vue`文件为什么需要Vite编译。
- 能使用文本插值和简单JavaScript表达式。
- 能区分固定HTML属性与`v-bind`动态绑定。
- 能读懂指令名、参数、修饰符和表达式的位置。
- 知道不可信内容不能直接交给`v-html`。





