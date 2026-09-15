# 第 4 章 事件绑定：接收用户操作

## 本章目标与前置知识

【必须掌握】使用`v-on`缩写、事件函数、业务参数、`$event`、`.prevent`、`.stop`、`.once`和键盘修饰符，并通过事件修改ref。动态事件名只要求【会读即可】。需要掌握第3章ref及JavaScript DOM事件基础。

前一章已经使用ref建立响应式状态。本章把用户操作、事件函数、状态修改和页面更新连接起来，完成第一个真正可交互的Vue页面。

事件对象、默认行为、冒泡和捕获已经在[JavaScript第10章：事件与表单](../JavaScript/10_events_forms.md)详细学习。本章只简短回顾这些概念，重点掌握Vue模板如何使用已有的DOM事件知识。

## 1. 什么是事件绑定

浏览器会在用户操作时产生事件。下面只作为选用事件时的速查，不重复讲解DOM事件原理：

| 事件 | 何时发生 | 常见用途 |
| --- | --- | --- |
| `click` | 点击按钮或链接 | 新增、删除、打开详情 |
| `input` | 输入内容变化 | 实时取得输入内容 |
| `change` | 控件值确认变化 | 下拉框、复选框处理 |
| `submit` | 表单提交 | 校验并保存数据 |
| `keydown` | 键盘按下 | Enter搜索、Esc关闭 |
| `focus`、`blur` | 获得、失去焦点 | 输入提示、离开字段校验 |

Vue使用`v-on`绑定事件，`v-on:click`通常简写为`@click`：

```vue
<script setup>
function showMessage() {
  window.alert('确认了任务内容')
}
</script>

<template>
  <button type="button" @click="showMessage">确认</button>
</template>
```

`@click="showMessage"`表示点击时调用`showMessage()`。函数本身仍是普通JavaScript函数，Vue负责把它连接到按钮。函数名应表达动作，如`openDetail`、`submitTask`，不要使用含义不明的`doIt`。

## 2. 内联处理与函数处理

只有一个简单操作时可以写内联表达式：

```vue
<button type="button" @click="console.log('点击了按钮')">记录操作</button>
```

包含判断、多步处理、复用或需要测试时，使用命名函数：

```vue
<script setup>
function confirmTask() {
  console.log('开始确认')
  window.alert('任务已确认')
}
</script>

<template>
  <button type="button" @click="confirmTask">确认任务</button>
</template>
```

业务项目通常优先使用命名函数，避免模板中出现难以Review的长表达式。

## 3. 传递业务参数

```vue
<script setup>
function showTask(id, title) {
  console.log('任务编号：', id)
  console.log('任务名称：', title)
}
</script>

<template>
  <button type="button" @click="showTask(101, '规格确认')">查看任务</button>
</template>
```

需要参数时，在模板中写函数调用。传递任务编号能让处理函数知道操作对象。

## 4. 取得原生事件对象

没有显式传参时，处理函数可以接收浏览器事件对象：

```vue
<script setup>
function handleInput(event) {
  const input = event.currentTarget
  console.log('当前输入：', input.value)
}
</script>

<template>
  <label for="task-title">任务名称</label>
  <input id="task-title" @input="handleInput">
</template>
```

事件对象及`target`、`currentTarget`的区别参见JavaScript事件章节。Vue中特别需要掌握的是：同时传递业务参数和原生事件对象时，使用模板提供的`$event`：

```vue
<button type="button" @click="showTaskEvent(101, $event)">查看</button>
```

只有确实需要目标元素、按键或坐标时才读取事件对象。

## 5. 表单提交事件

HTML表单默认提交后会跳转或刷新页面。单页应用通常先阻止默认行为：

```vue
<script setup>
function submitTask() {
  console.log('执行表单校验和保存')
}
</script>

<template>
  <form @submit.prevent="submitTask">
    <label for="title">任务名称</label>
    <input id="title" name="title" required>
    <button type="submit">保存</button>
  </form>
</template>
```

`submit`绑定在`form`上，点击提交按钮或在输入框按Enter都进入同一流程。普通按钮明确写`type="button"`，避免在表单中意外提交。

## 6. 常用事件修饰符

| 写法 | 作用 | 常见场景 |
| --- | --- | --- |
| `@submit.prevent` | 阻止默认行为 | SPA表单提交 |
| `@click.stop` | 阻止事件继续冒泡 | 嵌套点击区域需要分离 |
| `@click.once` | 当前监听器只执行一次 | 一次性引导操作 |
| `@click.self` | 只有点击元素自身才执行 | 点击遮罩关闭弹窗 |
| `@click.capture` | 在捕获阶段处理 | 少量需要外层先处理的场景 |
| `@scroll.passive` | 表明不会阻止默认滚动 | 滚动性能相关场景，只需会读 |
| `@keydown.enter` | 只处理Enter键 | 搜索确认 |
| `@keyup.esc` | 只处理Esc键 | 关闭弹窗 |

不要习惯性添加`.stop`或`.prevent`。应先说明需要阻止的默认行为或传播，否则可能破坏外层组件的处理。

修饰符可以连续书写，而且顺序会影响生成的处理逻辑。例如`@click.prevent.self`和`@click.self.prevent`的作用范围并不完全相同。项目中应选择最少且意图明确的组合。

### 6.1 键盘和系统按键修饰符

常见键名包括`.enter`、`.esc`、`.tab`、`.space`、`.up`、`.down`、`.left`、`.right`和`.delete`。系统按键可以使用`.ctrl`、`.alt`、`.shift`和`.meta`：

```vue
<input @keyup.enter="searchTasks">
<button type="button" @click.ctrl="openInNewMode">Ctrl + 点击</button>
<button type="button" @click.ctrl.exact="runOnlyWithCtrl">仅Ctrl + 点击</button>
```

`.exact`表示不能同时按下未声明的其他系统按键。组合快捷键必须考虑操作系统和浏览器既有快捷键，也要提供普通按钮操作，不能把关键功能只放在快捷键中。

鼠标按键修饰符`.left`、`.right`和`.middle`只需能读懂。不要把键盘方向修饰符与鼠标按键修饰符混为一谈。

## 7. 冒泡与捕获只做Vue写法回顾

传播过程已经在JavaScript课程讲过。Vue模板中常用`.stop`停止继续传播、`.self`限制为元素自身、`.capture`切换到捕获阶段：

```vue
<article @click="openTask">
  <button type="button" @click.stop="removeTask">删除</button>
</article>
```

这里`.stop`有明确目的：删除按钮不能同时打开详情。第11章的组件自定义事件不会像DOM事件一样自动跨多层冒泡。

## 8. 同一事件执行多个函数

Vue允许在内联表达式中调用多个函数：

```vue
<button type="button" @click="validateTask(), saveTask()">保存</button>
```

但正常业务更推荐提供一个入口函数，并在函数内部按顺序调用校验和保存。这样更容易处理提前返回、异常、异步流程和单元测试。

## 9. 动态事件名只需会读

```vue
<button type="button" @[eventName]="handleEvent">执行</button>
```

方括号表示事件名来自JavaScript表达式。绝大多数业务按钮的事件名固定，直接写`@click`更清楚；动态事件名只在封装通用功能时偶尔使用，不作为主线练习。

## 10. 模板监听与原生监听的管理区别

模板中的`@click`会随组件创建和卸载由Vue管理，一般不需要手工调用`removeEventListener()`。

如果代码直接对`window`、`document`或第三方对象调用`addEventListener()`，仍要在组件卸载时清理。具体注册时机和清理方法放在第14章生命周期中讲解。

## 11. 事件修改ref后更新页面

```vue
<script setup>
import { ref } from 'vue'

const count = ref(0)

function increment() {
  count.value++
}
</script>

<template>
  <button type="button" @click="increment">点击次数：{{ count }}</button>
</template>
```

点击后`increment()`修改`count.value`，Vue根据ref的变化重新更新按钮文字。这个流程是后续所有Vue交互的基础：用户操作 → 事件函数 → 修改状态 → Vue更新页面。

## 12. 常见错误与现场开发注意事项

- 一个操作使用一个明确入口，避免点击和提交执行两套逻辑。
- 模板负责绑定，复杂业务判断放函数或业务模块。
- 删除、提交要考虑重复执行；第8章处理表单状态，第17章结合请求。
- 调试时先确认事件是否触发、参数是否正确，再检查状态和接口。
- 不混用HTML的`onclick`字符串写法与Vue的`@click`。
- 不重复实现浏览器已经提供的键盘和表单行为，保持鼠标与键盘都能操作。

## 13. 练习

1. 分别绑定`click`、`input`、`submit`和`keydown.enter`。
2. 编写无参数、带业务参数和接收事件对象的函数。
3. 验证点击提交按钮和按Enter都触发`submit`。
4. 制作可点击任务卡片和删除按钮，只在有明确需要时使用`.stop`。
5. 运行普通`let count`示例，记录控制台和页面显示为何不同。
6. 为搜索输入增加Enter处理，为关闭按钮增加Esc处理，并保留普通可点击入口。
7. 说明模板`@click`和手工`addEventListener()`在清理责任上的区别。

## 本章检查点

- 能解释`v-on`、`@事件名`和处理函数的关系。
- 能区分内联表达式与命名函数。
- 能传递业务参数并在需要时读取`$event`。
- 能使用提交、传播和键盘修饰符。
- 知道普通变量变化不会自动刷新模板。





