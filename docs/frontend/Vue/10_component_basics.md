# 第 10 章 组件基础：拆分和组合页面

## 本章目标与前置知识

【必须掌握】按职责而非DOM数量拆分组件，完成局部导入与组合；全局注册达到【会使用、能看懂】。需要掌握SFC、列表和表单。本章示例使用JavaScript。

前面章节的示例主要写在`App.vue`中。页面变大后，把所有模板、状态、事件和样式放在一个文件里会难以阅读、修改和测试。本章学习怎样按职责拆成多个组件，再把它们组合成页面。

本章只讲组件本身的创建和组织。父子传值放在第11章，组件`v-model`与Attributes放在第12章，Slots与provide/inject放在第13章。

## 1. 什么是Vue组件

组件是可以独立维护和重复使用的界面单元。一个组件通常包含：

- 自己负责的HTML结构；
- 自己需要的状态和操作；
- 与该结构有关的样式；
- 对父组件公开的输入和输出契约。

当前章节先处理前三项。输入和输出契约下一章再学习。

例如任务一览页面可以拆成：

```text
TaskListView.vue
├─ PageTitle.vue
├─ TaskSearch.vue
├─ TaskList.vue
│  └─ TaskItem.vue
└─ TaskForm.vue
```

这个结构表示组件使用关系，不代表最终DOM一定有相同的嵌套标签。

## 2. 为什么需要拆分组件

### 2.1 降低单个文件的职责

`TaskForm.vue`只负责新增表单，`TaskList.vue`只负责显示任务列表。修改表单校验时，不必在一个巨大文件中寻找相关代码。

### 2.2 复用界面

统一按钮、错误提示、卡片等结构可以在多个页面使用。修正组件后，使用它的位置一起获得更新。

### 2.3 独立测试

职责明确的组件可以单独准备输入、触发操作并验证输出。第19章学习测试Case设计和手动打鍵，第20章再把适合的Case改成自动测试。

### 2.4 明确团队改修范围

日本项目常要求先调查影响范围。组件边界清楚时，更容易说明“修改了哪个组件、被哪些页面使用、需要回归哪些功能”。

## 3. 什么情况下适合拆组件

出现以下信号时可以考虑拆分：

- 一段界面在多个位置重复；
- 具有独立业务职责，例如搜索条件或任务表单；
- 模板和脚本过长，已经难以快速定位；
- 可以独立说明输入、输出和完成标准；
- 需要独立测试或由不同成员维护。

下面情况通常不需要立即拆分：

- 只有一个没有独立含义的`div`；
- 拆出后仍需要读取父组件的大量内部状态；
- 只为了让文件行数变少；
- 项目已有明确结构，而本次改修没有重构授权。

组件不是越小越好。边界应该跟随职责，而不是跟随每一个HTML标签。

## 4. 创建第一个子组件

新建`src/components/PageTitle.vue`：

```vue
<template>
  <header class="page-title">
    <h1>任务一览</h1>
    <p>确认并维护担当任务。</p>
  </header>
</template>

<style scoped>
.page-title {
  margin-bottom: 24px;
}

.page-title h1 {
  margin-bottom: 8px;
}
</style>
```

这个组件当前只有模板和样式，因此不需要空的`script setup`。单文件组件可以只包含实际需要的区域。

## 5. 导入并使用组件

在`App.vue`中导入：

```vue
<script setup>
import PageTitle from './components/PageTitle.vue'
</script>

<template>
  <main class="page">
    <PageTitle />
  </main>
</template>
```

使用`script setup`时，导入的组件可以直接用于当前模板，不需要另外编写`components: { PageTitle }`。

处理过程是：

```text
导入PageTitle组件
       ↓
模板使用<PageTitle />
       ↓
Vue创建组件实例
       ↓
组件模板生成真实DOM
```

浏览器Elements中看到的是`header`、`h1`和`p`，不会保留名为`PageTitle`的HTML元素。Vue DevTools则可以查看`PageTitle`组件实例。

## 6. 组件命名规则

### 6.1 文件和模板使用PascalCase

```text
PageTitle.vue
TaskSearch.vue
TaskList.vue
TaskForm.vue
```

模板中写：

```vue
<TaskSearch />
<TaskList />
```

PascalCase能明显区分Vue组件与原生HTML元素。文件名、导入名和模板名保持一致，减少大小写错误。

### 6.2 名称表达职责

| 名称 | 问题或含义 |
| --- | --- |
| `Component1.vue` | 无法判断负责什么 |
| `Common.vue` | 范围过大，职责不清楚 |
| `TaskSearch.vue` | 任务搜索区域 |
| `TaskStatusBadge.vue` | 显示任务状态标签 |
| `BaseButton.vue` | 项目通用基础按钮 |

通用基础组件常使用`Base`前缀，业务组件使用业务名称。最终以项目既有命名规范为准。

## 7. 自闭合与完整标签

单文件组件模板中通常可以写自闭合组件：

```vue
<PageTitle />
```

也可以写：

```vue
<PageTitle></PageTitle>
```

原生HTML元素应遵守HTML规则，例如不要随意把`div`写成自闭合。组件标签与原生元素的解析规则不要混淆。

## 8. 每次使用都会创建独立实例

新建`src/components/TaskCounter.vue`：

```vue
<script setup>
import { ref } from 'vue'

const count = ref(0)

function increment() {
  count.value++
}
</script>

<template>
  <section class="counter">
    <p>当前次数：{{ count }}</p>
    <button type="button" @click="increment">增加</button>
  </section>
</template>
```

父组件使用两次：

```vue
<script setup>
import TaskCounter from './components/TaskCounter.vue'
</script>

<template>
  <TaskCounter />
  <TaskCounter />
</template>
```

页面会创建两个`TaskCounter`实例。点击第一个组件的按钮只修改第一个实例的`count`，第二个不会变化。

组件文件像创建实例的设计说明，每次使用标签都会产生一个实例。局部状态默认属于各自实例，不是自动共享的全局状态。

## 9. 父组件和子组件

在下面的模板中，`App`是父组件，`PageTitle`和`TaskCounter`是直接子组件：

```vue
<template>
  <PageTitle />
  <TaskCounter />
</template>
```

“父子”描述的是组件使用关系，不一定等同于业务组织关系。一个组件只能直接访问自己的脚本内容，不能因为显示在父组件内部就自动读取父组件变量。

父级数据怎样传入子级、子级怎样通知父级，将在第11章通过Props和组件事件实现。

## 10. 组件树与DOM树不同

组件树展示Vue组件之间的关系：

```text
App
├─ PageTitle
├─ TaskSearchPanel
└─ TaskListPanel
```

DOM树展示最终HTML元素：

```text
main
├─ header
├─ form
└─ ul
```

调查布局和CSS时看浏览器Elements；调查组件状态、Props和组件层级时看Vue DevTools。不要在Elements里寻找`.vue`文件名。

## 11. scoped样式的组件边界

```vue
<style scoped>
.task-card {
  border: 1px solid #d1d5db;
}
</style>
```

`scoped`让选择器主要匹配当前组件模板，减少不同组件同名类冲突。但它不是完全隔离环境：

- CSS继承仍然存在；
- 父组件需要对子组件根元素进行布局；
- 全局样式仍可能产生影响；
- 它不是Shadow DOM，也不是安全边界。

组件内部外观写在组件中，全局重置、颜色变量和页面基础规则写入公共CSS。不要使用大量深层选择器强行修改另一个组件内部结构，这会破坏组件边界。

## 12. 导入路径和大小写

```js
import TaskList from './components/TaskList.vue'
```

相对路径从当前文件所在目录开始：

- `./`表示当前目录；
- `../`表示上一级目录；
- 项目配置后，`@/`通常表示`src/`。

不要假定所有项目都配置了`@`别名。查看`vite.config.js`和现有导入方式后保持一致。

Windows文件系统有时不明显区分大小写，而Linux构建环境通常区分。`TaskList.vue`不能写成`tasklist.vue`，否则本地可能正常、CI或服务器构建失败。

## 13. 局部注册与全局注册

组件必须在Vue能够识别的范围内注册后才能使用。常见方式分为局部注册和应用级全局注册。

### 13.1 局部注册

在使用组件的单文件组件中直接导入：

```vue
<script setup>
import PageTitle from './components/PageTitle.vue'
import TaskCounter from './components/TaskCounter.vue'
</script>

<template>
  <PageTitle />
  <TaskCounter />
</template>
```

`script setup`会让导入内容直接暴露给当前模板。这个注册只在当前组件内有效；另一个组件需要使用`PageTitle`时，也要明确导入。

局部注册适合绝大多数业务组件：

- 从文件顶部能立即看出依赖；
- 同名冲突较少；
- 未使用组件更容易被构建工具排除；
- 移动或删除组件时容易调查引用位置。

本课程的业务组件统一优先局部注册。

局部注册的优缺点可以整理为：

| 方面 | 说明 |
| --- | --- |
| 优点：依赖清楚 | 查看文件顶部的`import`就知道当前组件使用了什么 |
| 优点：影响范围较小 | 组件只在明确导入的位置使用，重命名和删除更容易调查 |
| 优点：减少冲突 | 不同页面可以使用各自需要的组件名称和实现 |
| 优点：有利于构建优化 | 没有导入和使用的组件更容易被构建工具排除 |
| 缺点：需要重复导入 | 多个组件都要使用时，每个文件都要写`import` |
| 缺点：路径需要维护 | 文件移动后，需要同步修改相关导入路径 |

重复写导入虽然增加少量代码，但能让依赖保持明确。正常业务组件通常更重视可调查性，因此优先局部注册。

### 13.2 应用级全局注册

在`src/main.js`创建应用后、挂载前调用`app.component()`：

```js
import { createApp } from 'vue'
import App from './App.vue'
import BaseButton from './components/base/BaseButton.vue'
import BaseIcon from './components/base/BaseIcon.vue'

const app = createApp(App)

app.component('BaseButton', BaseButton)
app.component('BaseIcon', BaseIcon)

app.mount('#app')
```

注册后，应用内任意后代组件的模板都可以直接使用：

```vue
<template>
  <BaseButton>保存</BaseButton>
</template>
```

`app.component(组件名称, 组件定义)`的第一个参数是模板使用的名称，第二个参数是导入的组件。注册必须在`mount()`之前完成。

全局注册适合项目中几乎所有页面都会使用、命名和行为已经统一的少量基础组件，例如基础按钮、图标或布局组件。是否采用仍以团队规范和UI框架接入方式为准。

全局注册的优缺点可以整理为：

| 方面 | 说明 |
| --- | --- |
| 优点：使用方便 | 后代组件不需要逐个导入即可使用 |
| 优点：统一公共入口 | 少量基础组件可以在应用入口集中注册 |
| 优点：适合稳定基础设施 | 项目级按钮、图标等高频组件可以保持统一名称 |
| 缺点：依赖不直观 | 只看当前文件无法判断组件从哪里提供，需要调查`main.js`或插件 |
| 缺点：名称容易冲突 | 全局名称被整个应用共享，命名不清楚时可能覆盖或混淆 |
| 缺点：影响范围较大 | 修改全局组件可能影响大量页面，需要扩大回归范围 |
| 缺点：可能增加构建内容 | 全局注册但实际未使用的组件不一定容易被排除 |
| 缺点：测试准备增加 | 独立挂载组件时，可能还要在测试环境注册相同的全局组件 |

因此，全局注册不等于“更高级”或“默认更好”。它只是用集中注册换取模板使用便利，同时增加了隐藏依赖和影响范围。

### 13.3 怎样选择

| 情况 | 推荐方式 |
| --- | --- |
| 只在一个页面或少数组件使用 | 局部注册 |
| 任务、员工等具体业务组件 | 局部注册 |
| 几乎所有页面都会用的稳定基础组件 | 可评估全局注册 |
| 第三方UI库要求通过插件安装 | 按官方和项目既有方式 |
| 无法确定组件从哪里注册 | 检查当前文件、`main.js`和插件文件 |

不要为了少写一行`import`就把所有组件全局注册。全局组件的依赖不会直接出现在使用文件顶部，名称冲突和影响范围也更难调查。

实际判断可以使用以下顺序：

1. 只要是具体业务组件，默认局部注册。
2. 统计它是否真的被绝大多数页面使用。
3. 确认名称、行为和样式是否已经稳定。
4. 确认团队是否已有全局基础组件规范。
5. 评估修改后需要回归的全部页面。
6. 满足这些条件后，再考虑全局注册。

例如`TaskForm`、`EmployeeList`属于具体业务组件，应局部注册；稳定的`BaseButton`如果全项目高频使用，可以按团队规范全局注册。

### 13.4 注册与导入不是一回事

`import`把组件模块加载到当前JavaScript文件；注册让Vue模板能够按组件名称使用它。`script setup`中的局部导入同时完成了当前模板所需的注册关系，而`main.js`中的全局方式需要显式调用`app.component()`。

维护既有项目时如果模板中使用了一个没有局部导入的组件，不要立即判断代码错误，应继续检查应用入口、插件安装和自动导入配置。

## 14. 拆分页面的推荐顺序

不要一次把整个页面全部拆完。可以按以下步骤进行：

1. 确认拆分前页面可以运行。
2. 找到职责最独立的一块模板和样式。
3. 新建组件并移动相关内容。
4. 在父组件中导入和使用。
5. 确认页面结构、样式和操作没有变化。
6. 再处理下一个区块。

如果移动后发现新组件需要大量父级变量，先停下来整理输入输出，不要依靠全局变量绕过组件通信。

### 14.1 WorkHub页面的连续拆分示例

拆分前，`TaskListView.vue`同时包含标题、搜索表单、新增表单和任务列表。先保持页面功能不变，再按职责逐步移动模板：

```text
第1步：PageTitle.vue     页面标题和说明
第2步：TaskSearch.vue    搜索条件区域
第3步：TaskForm.vue      新增任务表单
第4步：TaskList.vue      任务列表容器
第5步：TaskItem.vue      单项任务的显示和操作区
```

当前阶段可以先让各组件显示固定内容。例如`TaskItem.vue`只建立单项任务的语义结构：

```vue
<template>
  <li class="task-item">
    <h3>画面规格确认</h3>
    <p>担当：田中</p>
    <button type="button">完成</button>
  </li>
</template>
```

这里暂时不接收任务数据，也不发送完成事件。第11章学习Props和组件事件后，再把固定内容替换成父组件传入的数据。

每拆出一个组件，都进行以下确认：

1. 页面仍能打开，没有组件解析警告。
2. 浏览器Elements中的标题、表单和列表结构没有意外变化。
3. 原有CSS仍然生效，必要时把只属于该区域的样式一起移动。
4. Vue DevTools中能看到新组件及其父子关系。

## 15. 常见错误

### 15.1 忘记导入组件

模板写了`<TaskList />`但`script setup`没有导入时，Vue可能给出组件解析警告。先检查导入语句和路径。

### 15.2 导入名称和模板名称不一致

```js
import TaskList from './components/TaskList.vue'
```

对应模板应使用`<TaskList />`。命名保持一致能减少调查成本。

### 15.3 循环依赖

组件A导入B，B又直接导入A，可能造成循环依赖。重新检查父子职责和数据流，不要继续增加相互导入。

### 15.4 拆得过细

如果一个组件只有无意义的包装元素，也没有复用、独立职责或测试价值，拆分只会增加文件跳转。

## 16. 本章阶段成果

把当前单文件任务页面先拆成不需要通信的静态区域：

```text
src/
├─ components/
│  ├─ PageTitle.vue
│  ├─ TaskSearchPanel.vue
│  ├─ TaskListPanel.vue
│  └─ TaskFormPanel.vue
└─ App.vue
```

当前组件可以先显示固定内容或维护各自独立的演示状态。不要为了让数据立即贯通而提前使用未学习的Props、emits或全局状态。第11章会在这个开始状态上建立正式数据流。

## 17. 练习

1. 创建`PageTitle.vue`并在`App.vue`导入使用。
2. 创建包含局部ref的`TaskCounter.vue`，同时使用两次，验证状态互不影响。
3. 将页面静态区域拆成搜索、列表和表单三个组件，保持拆分前后DOM结构和样式一致。
4. 分别在浏览器Elements和Vue DevTools中画出DOM树与组件树。
5. 故意写错组件导入路径大小写，观察错误后修正。
6. 分别用局部注册和`app.component()`注册一个演示组件，说明使用范围。
7. 找出一个拆分过细的候选组件，并说明为什么不拆。
8. 为每个组件写一句职责说明，不能出现“处理所有页面功能”。
9. 记录本次拆分涉及的文件、确认项目和结果，形成简单改修说明。

## 本章检查点

- 能解释组件解决的维护、复用和测试问题。
- 能根据业务职责判断是否应该拆分。
- 能创建、导入并在`script setup`模板中使用组件。
- 能说明每次使用组件都会创建独立实例。
- 能区分父组件、子组件、组件树和DOM树。
- 能正确处理`scoped`样式、导入路径和文件名大小写。
- 能写出局部注册和应用级全局注册，并根据使用范围选择。





