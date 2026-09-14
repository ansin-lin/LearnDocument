# 第 8 章 表单绑定与校验

## 本章目标

- 【必须掌握】对常见表单控件使用`v-model`。
- 【必须掌握】完成必填、长度、选择项校验以及提交和重置。
- 【必须掌握】防止重复提交，并在失败时保留用户输入。

## 前置知识

需要掌握`ref`、事件绑定、条件渲染和JavaScript字符串、数组、对象操作。

## 1. v-model解决什么问题

表单既要把状态显示到控件，也要把用户输入写回状态。`v-model`提供这两个方向的同步：

```text
响应式状态 ↔ 表单控件
```

```vue
<script setup lang="ts">
import { ref } from 'vue'
const title = ref('')
</script>

<template>
  <label for="title">任务标题</label>
  <input id="title" v-model="title">
  <p>当前输入：{{ title }}</p>
</template>
```

`v-model`不会自动校验、发送HTTP或保存数据库。它只处理控件值与状态的同步。

### 1.1 v-model简化了什么

第6、7章为了同步输入框，分别写了属性绑定和事件处理：

```vue
<input :value="title" @input="title = $event.target.value">
```

在普通文本输入框上，下面写法表达相同的主要关系：

```vue
<input v-model="title">
```

可以把它理解成“用状态设置控件值，控件输入后再更新状态”的组合。不同控件使用的DOM属性和事件并不完全相同，Vue会根据`input`、`checkbox`、`radio`或`select`处理对应规则。

### 1.2 输入框默认得到字符串

```vue
<script setup lang="ts">
import { ref } from 'vue'

const quantity = ref('1')
</script>

<template>
  <input v-model="quantity" type="number">
  <p>{{ typeof quantity }}：{{ quantity }}</p>
</template>
```

即使`type="number"`，普通`v-model`取得的仍通常是字符串。需要数字时可使用`.number`，但业务仍要检查空值、范围和转换结果。

## 2. 常见控件

```vue
<script setup lang="ts">
import { ref } from 'vue'
import type { Priority, TaskStatus } from './types/task'

const title = ref('')
const description = ref('')
const status = ref<TaskStatus>('todo')
const labels = ref<string[]>([])
const priority = ref<Priority>('normal')
</script>

<template>
  <input v-model.trim="title">
  <textarea v-model="description"></textarea>

  <label><input v-model="status" true-value="done" false-value="todo" type="checkbox"> 完成</label>
  <label><input v-model="labels" type="checkbox" value="frontend"> 前端</label>
  <label><input v-model="labels" type="checkbox" value="review"> Review</label>

  <label><input v-model="priority" type="radio" value="normal"> 普通</label>
  <label><input v-model="priority" type="radio" value="high"> 高</label>

  <select v-model="status">
    <option value="todo">未着手</option>
    <option value="doing">进行中</option>
    <option value="done">完成</option>
  </select>
</template>
```

单个checkbox通常对应布尔值，多个checkbox对应数组，radio和单选select保存被选项的`value`。多选select则绑定数组。

### 2.1 为什么不同控件需要不同状态形态

| 控件 | 常见状态类型 | 状态中保存什么 |
| --- | --- | --- |
| text、textarea | 字符串 | 当前文字 |
| 单个checkbox | 布尔值 | 是否选中 |
| 多个checkbox | 数组 | 所有选中项的value |
| radio | 字符串、数字或对象 | 当前唯一选项的value |
| 单选select | 字符串、数字或对象 | 当前option的value |
| 多选select | 数组 | 所有选中option的value |

多个checkbox共用同一个数组时，勾选会加入对应`value`，取消会移除。radio共用同一个状态时，只能有一个值与状态相等。

### 2.2 固定value与动态:value

HTML属性`value="101"`得到字符串`"101"`；动态绑定`:value="101"`得到数字`101`：

```vue
<label><input v-model="assigneeId" type="radio" :value="101"> 田中</label>
<label><input v-model="assigneeId" type="radio" :value="102"> 佐藤</label>
```

接口编号要求数字时，应使用动态`:value`保留数字类型。对象也能作为动态值，但业务表单通常优先保存稳定ID，避免选项重新创建后对象引用不同。

### 2.3 多选select完整示例

```vue
<script setup lang="ts">
import { ref } from 'vue'

const selectedLabels = ref<string[]>([])
</script>

<template>
  <label for="labels">标签</label>
  <select id="labels" v-model="selectedLabels" multiple>
    <option value="frontend">前端</option>
    <option value="review">Review</option>
    <option value="urgent">紧急</option>
  </select>
  <p>已选择：{{ selectedLabels.join('、') || '无' }}</p>
</template>
```

`multiple`允许选择多个option，因此绑定值必须是数组。按操作系统方式多选后，数组会与当前选项同步。

### 2.4 checkbox的自定义值（会读即可）

```vue
<input
  v-model="publishStatus"
  type="checkbox"
  true-value="published"
  false-value="draft"
>
```

`true-value`和`false-value`让单个checkbox保存指定值，而不是布尔值。它们不会改变表单原生提交规则；简单的是否状态仍优先使用布尔值。

## 3. 修饰符

| 写法 | 作用 | 注意事项 |
| --- | --- | --- |
| `.trim` | 去掉输入首尾空白 | 不会删除文字中间空白 |
| `.number` | 尝试把输入转为数字 | 转换失败时仍需业务校验 |
| `.lazy` | 从每次`input`改为`change`时同步 | 不适合需要即时反馈的字段 |

修饰符只处理输入同步方式，不能代替必填、范围和业务规则校验。

### 3.1 `.number`的边界

```vue
<input v-model.number="estimateHours" type="number">
```

输入可以转换时，状态得到数字；输入为空或无法合理转换时，不能只凭“使用了`.number`”就认定数据有效。提交前仍要确认必填、`Number.isFinite()`、最小值和最大值。

`.lazy`会等到`change`事件再同步，输入期间依赖该状态的文字不会逐字更新；`.trim`只去掉首尾空白，不负责长度和禁止字符检查。

## 4. WorkHub表单校验

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { Priority } from './types/task'

interface TaskForm {
  title: string
  priority: Priority | ''
}

type FormField = keyof TaskForm

const form = reactive<TaskForm>({ title: '', priority: '' })
const errors = reactive<Record<FormField, string>>({ title: '', priority: '' })
const touched = reactive<Record<FormField, boolean>>({ title: false, priority: false })

const isSaving = ref(false)
const submitError = ref('')

function validate(): boolean {
  touched.title = true
  touched.priority = true
  errors.title = ''
  errors.priority = ''

  const title = form.title.trim()
  if (!title) errors.title = '任务标题为必填项。'
  else if (title.length > 50) errors.title = '任务标题不能超过50个字符。'

  if (!['normal', 'high'].includes(form.priority)) {
    errors.priority = '请选择优先级。'
  }

  return !errors.title && !errors.priority
}

function clearFieldError(field: FormField): void {
  errors[field] = ''
}

function resetForm(): void {
  form.title = ''
  form.priority = ''
  errors.title = ''
  errors.priority = ''
  submitError.value = ''
  touched.title = false
  touched.priority = false
}

async function submitForm(): Promise<void> {
  if (isSaving.value || !validate()) return

  isSaving.value = true
  submitError.value = ''
  try {
    await new Promise<void>((resolve) => window.setTimeout(resolve, 500))
    console.log('保存成功', { ...form })
    resetForm()
  } catch (error) {
    console.error('保存失败', error)
    submitError.value = '保存失败。请确认网络后重试。'
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <form novalidate @submit.prevent="submitForm">
    <label for="task-title">任务标题</label>
    <input
      id="task-title"
      v-model="form.title"
      :aria-describedby="errors.title ? 'title-error' : undefined"
      @blur="touched.title = true"
      @input="clearFieldError('title')"
    >
    <p v-if="touched.title && errors.title" id="title-error">{{ errors.title }}</p>

    <label for="priority">优先级</label>
    <select
      id="priority"
      v-model="form.priority"
      @blur="touched.priority = true"
      @change="clearFieldError('priority')"
    >
      <option value="">请选择</option>
      <option value="normal">普通</option>
      <option value="high">高</option>
    </select>
    <p v-if="touched.priority && errors.priority">{{ errors.priority }}</p>

    <p v-if="submitError" role="alert">{{ submitError }}</p>

    <button type="submit" :disabled="isSaving">
      {{ isSaving ? '保存中…' : '保存' }}
    </button>
    <button type="button" :disabled="isSaving" @click="resetForm">重置</button>
  </form>
</template>
```

提交顺序是：阻止重复提交 → 校验 → 进入保存中 → 执行保存 → 成功后重置；失败时显示错误并保留输入。前端校验改善操作体验，后端仍必须重新校验。

`errors`保存各字段错误，`submitError`保存请求整体失败。两类错误不要混用：标题不合法应显示在标题附近，服务器暂时不可用应显示表单级提示。

## 5. 错误清除策略

可以在再次提交时统一清除旧错误，也可以在用户修改对应字段后清除该字段错误。不要在用户还没操作时显示全部错误，也不要失败后立即清空输入。

常见策略是：首次进入不显示错误；字段离开焦点后标记为`touched`；再次输入时清除该字段旧错误；提交时校验全部字段。是否实时重新校验取决于项目规格，不要一边输入一边反复显示尚未完成的错误。

### 5.1 服务端字段错误怎样处理

后端可能返回“标题重复”“负责人不存在”等前端无法独立判断的错误。请求层先按接口规格解析错误，页面再把字段错误放入`errors.title`等对应位置，把无法对应某个字段的错误放入`submitError`。

前端不能通过复制后端规则保证永远一致；长度、格式等即时提示可以在前端执行，最终有效性仍以后端判断为准。

## 6. 常见错误与项目注意事项

- 忘记`@submit.prevent`导致浏览器刷新。
- 按钮没有写`type`，在表单内意外触发提交。
- 把`v-model.number`当作完整数字校验。
- 使用一个`isSaving`状态，却没有在`finally`恢复。
- 仅禁用按钮但提交函数没有再次判断，仍可能重复调用。
- 把前端校验当作权限或安全控制。

## 7. 练习

1. 创建包含标题、说明、优先级、标签和完成状态的任务表单。
2. 实现必填、50字符限制和选择项校验。
3. 实现错误显示、单字段错误清除和完整重置。
4. 连续点击提交，确认只执行一次保存。
5. 模拟保存失败，确认输入仍保留且按钮恢复可用。
6. 增加数字工时字段，验证空值、非数字、最小值和最大值。
7. 增加多选标签和数字负责人ID，确认状态分别是数组和数字。
8. 模拟一个字段错误和一个系统错误，确认它们显示在不同位置。

## 本章检查点

- [ ] 能为各种常见控件选择正确的v-model数据形态。
- [ ] 能解释三个v-model修饰符的作用和边界。
- [ ] 能实现提交前校验和可理解的错误信息。
- [ ] 能实现重置与防重复提交。
- [ ] 能说明v-model和前端校验不会自动保存数据库。





