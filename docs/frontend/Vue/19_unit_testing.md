# 第 19 章 Vue单元测试与组件测试

测试代码会自动准备输入、执行操作，并比较实际结果与预期结果。修改组件、Composable或Store后，重新执行测试就能快速发现原有功能是否被破坏。

## 本章目标与前置知识

- 【必须掌握】区分单元测试、组件测试和端到端测试。
- 【必须掌握】使用Vitest组织用例、执行测试并阅读失败信息。
- 【必须掌握】使用Vue Test Utils验证页面显示、Props、用户操作和Emits。
- 【必须掌握】正确等待Vue更新和Promise完成。
- 【必须掌握】独立测试Composable和Pinia Store。
- 【会使用、能看懂】使用Mock控制API结果，并查看覆盖率。

需要掌握组件、Props与Emits、表单、Composable、Promise、API模块和Pinia。本章统一使用TypeScript。

## 1. 测试要解决什么问题

修改任务表单的校验规则后，需要重新确认：空标题不能提交、合法标题能够提交、事件携带正确数据、API失败时显示错误，而且失败后加载状态能够恢复。

手工点击可以验证一次，却难以在每次改修后稳定重复所有条件。自动测试把输入、操作和预期结果保存为代码，适合反复执行和提交给团队Review。

### 1.1 常见测试层次

| 测试层次 | 主要对象 | 运行环境 | 适合发现的问题 |
| --- | --- | --- | --- |
| 单元测试 | 函数、Composable、Store的独立逻辑 | 通常不打开浏览器 | 计算、分支和状态变化错误 |
| 组件测试 | 一个Vue组件及必要子组件 | 通常使用模拟DOM | 显示、交互、Props和Emits错误 |
| 端到端测试（E2E） | 从页面到路由、API和后端的完整流程 | 真实浏览器 | 多页面和真实环境组合问题 |

本章主线是单元测试和组件测试。E2E测试不能由大量单元测试完全代替，也不需要把同一行为在所有层次重复测试。

### 1.2 测试公开行为

组件的公开行为包括接收的Props、显示的内容、用户操作、发出的事件，以及加载、空数据、成功和失败状态。不要直接断言内部`ref`的变量名或内部方法调用。内部实现重构后，只要用户看到的行为没有变化，测试通常不应失败。

## 2. 一条测试怎样工作

测试通常按Arrange、Act、Assert三个阶段思考：

```ts
import { expect, it } from 'vitest'

it('两个任务中有一个已完成', () => {
  // Arrange：准备输入
  const tasks = [
    { id: 1, title: '规格确认', assignee: '田中', priority: 'normal', status: 'done', dueDate: '2026-09-30' },
    { id: 2, title: 'API确认', assignee: '佐藤', priority: 'high', status: 'todo', dueDate: '2026-10-05' },
  ]

  // Act：执行处理
  const count = tasks.filter((task) => task.status === 'done').length

  // Assert：比较实际结果与预期结果
  expect(count).toBe(1)
})
```

- `it()`定义一条测试，第一个参数描述行为，第二个参数是测试函数；
- `expect()`接收实际结果；
- `toBe()`是匹配器，用于判断原始值是否严格相等。

不必每次都保留三段注释，但测试中应能看出输入、操作和断言。

## 3. 准备Vue测试环境

### 3.1 安装工具

在Vite项目根目录执行：

```bash
npm install --save-dev vitest @vue/test-utils jsdom @vitest/coverage-v8
```

| 工具 | 作用 |
| --- | --- |
| `vitest` | 查找并执行测试，提供断言和Mock |
| `@vue/test-utils` | 挂载Vue组件、查询元素并模拟操作 |
| `jsdom` | 在Node.js中提供模拟浏览器DOM |
| `@vitest/coverage-v8` | 生成覆盖率报告 |

这些依赖只用于开发和测试，因此使用`--save-dev`。

### 3.2 配置和命令

在项目已有的`vite.config.ts`中增加`test`，不要创建第二份Vite配置：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    clearMocks: true,
  },
})
```

`environment`指定模拟DOM环境；`clearMocks`在每条测试前清除Mock调用记录。

在`package.json`的`scripts`中追加，不要覆盖已有脚本：

```json
{
  "scripts": {
    "test:unit": "vitest run",
    "test:unit:watch": "vitest",
    "test:coverage": "vitest run --coverage"
  }
}
```

`npm run test:unit`执行一次后结束，适合提交前检查和CI；开发时可以使用`npm run test:unit:watch`在文件变化后自动重测。

### 3.3 测试文件位置

Vitest识别`*.test.ts`和`*.spec.ts`。本课程把测试放在被测文件旁边：

```text
src/
├─ components/
│  ├─ TaskItem.vue
│  └─ TaskItem.spec.ts
├─ composables/
│  ├─ useTaskFilter.ts
│  └─ useTaskFilter.spec.ts
└─ stores/
   ├─ tasks.ts
   └─ tasks.spec.ts
```

## 4. 测试分组与常用断言

```ts
import { describe, expect, it } from 'vitest'

describe('任务状态判断', () => {
  it('status为done时显示已完成', () => {
    const label = true ? '已完成' : '未完成'
    expect(label).toBe('已完成')
  })
})
```

`describe()`把同一对象的测试归为一组。名称应描述业务行为，不要只写“测试1”。

| 匹配器 | 用途 | 示例 |
| --- | --- | --- |
| `toBe(value)` | 原始值严格相等 | `expect(count).toBe(2)` |
| `toEqual(value)` | 数组、对象内容相等 | `expect(ids).toEqual([1, 2])` |
| `toMatchObject(object)` | 对象至少包含指定字段 | `expect(task).toMatchObject({ title: '规格确认' })` |
| `toContain(value)` | 字符串或数组包含内容 | `expect(text).toContain('失败')` |
| `toHaveLength(number)` | 数组或字符串长度 | `expect(tasks).toHaveLength(2)` |
| `toBeUndefined()` | 结果为`undefined` | `expect(events).toBeUndefined()` |
| `toThrow()` | 函数抛出错误 | `expect(() => parseTask('')).toThrow()` |

对象内容通常使用`toEqual()`，不要误用`toBe()`比较两个分别创建的对象。

```bash
npm run test:unit:run
```

失败时先看用例名称，再比较Expected（预期）和Received（实际）。

## 5. 第一个组件测试

`src/components/TaskItem.vue`：

```vue
<script setup lang="ts">
import type { Task } from '@/types/task'

defineProps<{ task: Task }>()
</script>

<template>
  <article>
    <h3>{{ task.title }}</h3>
    <p v-if="task.status === 'done'">已完成</p>
    <button type="button">完成任务</button>
  </article>
</template>
```

`src/components/TaskItem.spec.ts`：

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TaskItem from './TaskItem.vue'

describe('TaskItem', () => {
  it('显示传入的任务标题', () => {
    const task = { id: 1, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' }
    const wrapper = mount(TaskItem, { props: { task } })

    expect(wrapper.get('h3').text()).toBe('规格确认')
  })
})
```

`mount()`渲染组件并返回wrapper。wrapper是测试代码查询和操作组件的入口。

### 5.1 查询元素

- `get(selector)`用于必须存在的元素，找不到时立即失败；
- `find(selector)`用于可能不存在的元素，配合`exists()`判断；
- `text()`读取文字；
- `attributes(name)`读取HTML属性；
- `classes()`读取class列表。

优先使用语义元素、表单`name`或稳定的`data-test`属性，不要用只负责布局的CSS class定位业务控件。

### 5.2 测试条件显示

```ts
it('已完成任务显示状态文字', () => {
  const task = { id: 1, title: '规格确认', assignee: '田中', priority: 'normal', status: 'done', dueDate: '2026-09-30' }
  const wrapper = mount(TaskItem, { props: { task } })
  expect(wrapper.text()).toContain('已完成')
})

it('未完成任务不显示状态文字', () => {
  const task = { id: 1, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' }
  const wrapper = mount(TaskItem, { props: { task } })
  expect(wrapper.find('p').exists()).toBe(false)
})
```

条件渲染至少检查显示和不显示两个分支。

## 6. 测试点击与Emits

给`TaskItem.vue`增加事件：

```vue
<script setup lang="ts">
import type { Task } from '@/types/task'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{
  changeStatus: [id: number, status: Task['status']]
}>()

function completeTask() {
  emit('changeStatus', props.task.id, 'done')
}
</script>

<template>
  <article>
    <h3>{{ task.title }}</h3>
    <button type="button" @click="completeTask">完成任务</button>
  </article>
</template>
```

```ts
it('点击完成按钮后发送任务编号和状态', async () => {
  const task = { id: 1, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' }
  const wrapper = mount(TaskItem, { props: { task } })

  await wrapper.get('button').trigger('click')

  expect(wrapper.emitted('changeStatus')).toHaveLength(1)
  expect(wrapper.emitted('changeStatus')?.[0]).toEqual([1, 'done'])
})
```

`trigger()`触发DOM事件；`emitted()`读取组件事件记录。每次事件的参数保存为一个数组，所以第一次`complete`事件的参数是`[1]`。不要直接调用内部`completeTask()`，测试目标是用户点击后的公开行为。

## 7. 测试表单输入与校验

组件测试应按照真实操作顺序填写输入、提交表单，再检查错误或事件：

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TaskForm from './TaskForm.vue'

describe('TaskForm', () => {
  it('空标题提交时显示错误且不发送事件', async () => {
    const wrapper = mount(TaskForm)

    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('[role="alert"]').text()).toContain('任务标题为必填项')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('合法输入提交任务标题', async () => {
    const wrapper = mount(TaskForm)

    await wrapper.get('input[name="title"]').setValue('列表画面实现')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toHaveLength(1)
    expect(wrapper.emitted('submit')[0][0]).toMatchObject({
      title: '列表画面实现',
    })
  })
})
```

`setValue()`修改控件值并触发`v-model`需要的事件。表单至少覆盖合法和非法输入，边界长度、重复提交等条件根据规格增加。

## 8. 正确等待Vue和Promise

Vue不会保证在当前一行JavaScript结束前完成DOM更新。`trigger()`和`setValue()`返回可等待的Promise，因此要使用`await`。

如果组件还执行普通Promise或Mock API，使用`flushPromises()`等待已经开始的Promise：

```ts
import { flushPromises, mount } from '@vue/test-utils'

it('读取完成后显示任务', async () => {
  const wrapper = mount(TaskList)

  await flushPromises()

  expect(wrapper.text()).toContain('规格确认')
})
```

`flushPromises()`不负责启动请求，也不能替代必要的点击。不要使用固定`setTimeout()`等待，因为环境变慢时容易偶发失败。

异步页面通常检查加载、空数据、成功、失败和最终恢复五种状态。

## 9. 测试Composable

第15章的`useTaskFilter()`只使用响应式API，可以直接调用：

```ts
import { ref } from 'vue'
import { describe, expect, it } from 'vitest'
import { useTaskFilter } from './useTaskFilter'

describe('useTaskFilter', () => {
  const tasks = [
    { id: 1, title: 'Vue实现', assignee: '田中', priority: 'normal', status: 'doing', dueDate: '2026-09-30' },
    { id: 2, title: 'API确认', assignee: '佐藤', priority: 'high', status: 'todo', dueDate: '2026-10-05' },
  ]

  it('按标题关键字筛选任务', () => {
    const source = ref(tasks)
    const { keyword, filteredTasks } = useTaskFilter(source)
    keyword.value = 'Vue'
    expect(filteredTasks.value.map((task) => task.id)).toEqual([1])
  })

  it('没有匹配任务时返回空数组', () => {
    const source = ref(tasks)
    const { keyword, filteredTasks } = useTaskFilter(source)
    keyword.value = '不存在'
    expect(filteredTasks.value).toEqual([])
  })
})
```

如果Composable使用`onMounted()`或`inject()`，它依赖组件上下文，需要通过测试用宿主组件挂载。本章先保证纯响应式Composable能够独立测试。

## 10. 测试Pinia Store

每条Store测试创建新的Pinia，防止状态互相污染：

```ts
import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useTaskStore } from './tasks'

describe('任务Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('新增任务后列表增加', () => {
    const store = useTaskStore()
    store.addTask('Review对应')
    expect(store.tasks).toHaveLength(1)
    expect(store.tasks[0].title).toBe('Review对应')
  })
})
```

`beforeEach()`在组内每条测试前执行；`createPinia()`创建独立容器；`setActivePinia()`让Store使用该容器。Store应验证action后的state、getter结果、异步状态和`reset()`。

## 11. Mock API的成功与失败

单元测试不访问真实API。Mock用固定结果替换API模块，让成功和失败都能稳定重现。

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getTasks } from '@/api/tasks'
import { useTaskStore } from './tasks'

vi.mock('@/api/tasks', () => ({
  getTasks: vi.fn(),
}))

const getTasksMock = vi.mocked(getTasks)

describe('任务Store的异步读取', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('API成功时保存任务并恢复加载状态', async () => {
    getTasksMock.mockResolvedValue([
      { id: 1, title: '规格确认', assignee: '田中', priority: 'normal', status: 'todo', dueDate: '2026-09-30' },
    ])
    const store = useTaskStore()

    await store.loadTasks()

    expect(store.tasks).toHaveLength(1)
    expect(store.errorMessage).toBe('')
    expect(store.loading).toBe(false)
  })

  it('API失败时保存错误并恢复加载状态', async () => {
    getTasksMock.mockRejectedValue(new Error('服务器暂时不可用'))
    const store = useTaskStore()

    await expect(store.loadTasks()).rejects.toThrow('服务器暂时不可用')

    expect(store.tasks).toEqual([])
    expect(store.errorMessage).toBe('服务器暂时不可用')
    expect(store.loading).toBe(false)
  })
})
```

- `vi.mock()`替换模块；
- `vi.fn()`创建可控制的Mock函数；
- `mockResolvedValue()`设定成功结果；
- `mockRejectedValue()`设定失败结果；
- `rejects.toThrow()`断言Promise以错误结束。

只Mock当前测试需要控制的外部边界，不要把被测Store本身也Mock掉。Mock数据字段必须符合第17章API契约。

## 12. `mount()`与`shallowMount()`

`mount()`正常渲染子组件，默认优先使用。`shallowMount()`把子组件替换成占位，只在子组件复杂或当前测试明确只关心父组件接口时使用：

```ts
import { shallowMount } from '@vue/test-utils'

const wrapper = shallowMount(TaskList, {
  global: {
    stubs: { TaskItem: true },
  },
})
```

过度Stub会让测试通过，但真实父子组合仍可能出错。Pinia等插件应通过`global.plugins`提供：

```ts
const wrapper = mount(TaskList, {
  global: {
    plugins: [createPinia()],
  },
})
```

## 13. 保持测试隔离

一条测试必须能够单独运行，不能依赖执行顺序：

```ts
import { afterEach, beforeEach, vi } from 'vitest'

beforeEach(() => {
  sessionStorage.clear()
})

afterEach(() => {
  vi.restoreAllMocks()
})
```

`beforeEach()`准备共同初始状态；`afterEach()`负责清理；`restoreAllMocks()`恢复通过spy替换的实现。手工添加的全局事件、计时器和存储数据也要清理。

## 14. 覆盖率的用途与限制

```bash
npm run test:coverage
```

| 指标 | 含义 |
| --- | --- |
| Statements | 执行过多少语句 |
| Branches | 执行过多少条件分支 |
| Functions | 调用过多少函数 |
| Lines | 执行过多少代码行 |

覆盖率用于提示遗漏，不代表测试质量。即使行覆盖率为100%，错误断言或缺少业务结果检查仍会漏掉缺陷。先根据风险设计测试，再用覆盖率寻找遗漏分支。

## 15. 调查失败测试

```text
Expected: "任务标题为必填项"
Received: "请输入标题"
```

1. 根据用例名称确认业务条件；
2. 比较Expected与Received；
3. 检查准备数据和触发操作；
4. 单独执行失败文件，排除共享状态；
5. 确认异步处理是否正确等待；
6. 如果规格已改变，同步修改实现、测试和说明。

不要为了让测试通过而删除断言，或直接把预期改成当前错误结果。

## 16. 常见错误

| 现象 | 常见原因 | 修正方法 |
| --- | --- | --- |
| 找不到`document` | 未配置DOM环境 | 安装并设置`jsdom` |
| 修改值后断言仍是旧内容 | 未等待Vue更新 | 对`setValue()`、`trigger()`使用`await` |
| API用例偶发失败或很慢 | 访问了真实服务器 | 在API模块边界使用Mock |
| 单独运行通过、全部运行失败 | 共享Pinia、Mock或存储 | 在`beforeEach()`重建并清理 |
| 改CSS后大量测试失败 | 用布局class定位元素 | 使用语义、表单属性或`data-test` |
| `toBe()`比较对象失败 | 对象不是同一引用 | 使用`toEqual()`或`toMatchObject()` |
| 测试通过但页面组合出错 | Stub了过多子组件 | 对关键组合使用`mount()` |
| 覆盖率高仍出现缺陷 | 只执行代码而未检查结果 | 根据业务状态补充断言 |

## 17. WorkHub练习与提交证据

### 17.1 实现任务

1. 配置测试依赖、环境和三个测试脚本。
2. 为`TaskItem`测试Props、完成状态和`complete`事件参数。
3. 为`TaskForm`测试空标题、合法提交和重复提交限制。
4. 为`useTaskFilter`测试有结果、无结果和清空关键字。
5. 为Pinia测试同步action、getter、`reset()`和用例隔离。
6. Mock API，测试异步action的加载、成功和失败状态。
7. 人为修改一条断言，阅读失败信息后恢复。

### 17.2 验证命令

```bash
npm run test:unit:run
npm run test:coverage
npm run build
```

### 17.3 提交内容

- 测试文件与必要配置；
- 三条命令的成功结果；
- 测试对象、正常条件、异常条件和预期结果清单；
- 无法自动化的项目及手工确认方法。

## 本章检查点

- [ ] 能区分单元、组件和E2E测试。
- [ ] 能用Arrange、Act、Assert组织测试。
- [ ] 能说明Vitest、Vue Test Utils和jsdom的职责。
- [ ] 能选择匹配器并阅读失败信息。
- [ ] 能用`mount()`、查询、赋值和事件方法测试组件。
- [ ] 能测试Props、条件显示、Emits和表单校验。
- [ ] 能正确等待Vue更新与Promise。
- [ ] 能测试Composable和Pinia Store。
- [ ] 能Mock API成功与失败，并保持用例隔离。
- [ ] 能说明覆盖率的用途与限制。

官方参考：[Vue测试指南](https://vuejs.org/guide/scaling-up/testing.html)、[Vue Test Utils指南](https://test-utils.vuejs.org/guide/)、[Vitest指南](https://vitest.dev/guide/)。







