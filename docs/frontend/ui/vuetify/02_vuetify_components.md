# 第2章 Vuetify 3常用业务组件

本章在第1章已经注册Vuetify的项目中，使用按钮、表单、卡片、数据表格、Dialog和Snackbar完成WorkHub任务页面。所有示例都是Vue单文件组件结构，继续使用JavaScript。

## 1. 按钮与状态

```vue
<script setup>
import { ref } from 'vue'

const saving = ref(false)

async function saveTask() {
  if (saving.value) return
  saving.value = true
  try {
    await new Promise((resolve) => window.setTimeout(resolve, 500))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <v-btn
    color="primary"
    prepend-icon="mdi-content-save"
    :loading="saving"
    :disabled="saving"
    @click="saveTask"
  >
    保存
  </v-btn>
</template>
```

`color`使用主题颜色，`loading`显示处理中状态，`disabled`阻止重复操作。请求失败时还必须显示错误，不能只停止loading。

## 2. 表单输入与校验

`rules`接收校验函数数组。规则返回`true`表示通过，返回字符串表示错误信息。

```vue
<script setup>
import { ref } from 'vue'

const formRef = ref(null)
const title = ref('')
const priority = ref('normal')
const priorities = [
  { title: '低', value: 'low' },
  { title: '普通', value: 'normal' },
  { title: '高', value: 'high' },
]
const titleRules = [
  (value) => Boolean(value?.trim()) || '请输入任务标题',
  (value) => (value ?? '').length <= 50 || '任务标题不能超过50个字符',
]

async function submitForm() {
  const result = await formRef.value.validate()
  if (!result.valid) return
  console.log({ title: title.value.trim(), priority: priority.value })
}
</script>

<template>
  <v-form ref="formRef" @submit.prevent="submitForm">
    <v-text-field
      v-model="title"
      label="任务标题"
      :rules="titleRules"
      maxlength="50"
    />
    <v-select
      v-model="priority"
      label="优先级"
      :items="priorities"
    />
    <v-btn type="submit" color="primary">保存</v-btn>
  </v-form>
</template>
```

`v-form.validate()`执行已注册规则并返回校验结果。组件校验改善操作体验，后端仍要重新校验。

## 3. 卡片与空状态

```vue
<script setup>
defineProps({
  tasks: {
    type: Array,
    required: true,
  },
})
</script>

<template>
  <v-alert v-if="tasks.length === 0" type="info" variant="tonal">
    没有符合条件的任务
  </v-alert>

  <template v-else>
    <v-card v-for="task in tasks" :key="task.id" class="mb-3">
      <v-card-title>{{ task.title }}</v-card-title>
      <v-card-text>
        担当者：{{ task.assignee }}／期限：{{ task.dueDate }}
      </v-card-text>
      <v-card-actions>
        <v-btn variant="text">编辑</v-btn>
      </v-card-actions>
    </v-card>
  </template>
</template>
```

空数组是成功读取后没有数据，不应显示成系统错误。

## 4. 数据表格

```vue
<script setup>
defineProps({
  tasks: {
    type: Array,
    required: true,
  },
  loading: Boolean,
})

const headers = [
  { title: '任务名', key: 'title' },
  { title: '担当者', key: 'assignee' },
  { title: '优先级', key: 'priority' },
  { title: '状态', key: 'status' },
  { title: '期限', key: 'dueDate' },
]
</script>

<template>
  <v-data-table
    :headers="headers"
    :items="tasks"
    :loading="loading"
    item-value="id"
  >
    <template #no-data>
      没有任务
    </template>
  </v-data-table>
</template>
```

`headers`规定显示列，`items`接收数据，`item-value="id"`指定稳定标识。具体分页、排序和服务端查询规则应根据API规格实现。

## 5. 删除确认Dialog

```vue
<script setup>
import { ref } from 'vue'

const dialog = ref(false)
const deleting = ref(false)

async function confirmDelete() {
  if (deleting.value) return
  deleting.value = true
  try {
    await new Promise((resolve) => window.setTimeout(resolve, 500))
    dialog.value = false
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <v-btn color="error" variant="outlined" @click="dialog = true">
    删除
  </v-btn>

  <v-dialog v-model="dialog" max-width="420">
    <v-card>
      <v-card-title>删除任务</v-card-title>
      <v-card-text>确定删除“规格确认”吗？此操作无法撤销。</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn :disabled="deleting" @click="dialog = false">取消</v-btn>
        <v-btn
          color="error"
          :loading="deleting"
          :disabled="deleting"
          @click="confirmDelete"
        >
          删除
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
```

危险操作应说明对象和影响。确认期间禁用重复操作；API失败时保留Dialog并显示错误。

## 6. Snackbar反馈

```vue
<script setup>
import { ref } from 'vue'

const snackbar = ref(false)
const message = ref('')
const messageType = ref('success')

function showResult(text, type = 'success') {
  message.value = text
  messageType.value = type
  snackbar.value = true
}
</script>

<template>
  <v-btn @click="showResult('任务已保存')">显示成功结果</v-btn>
  <v-btn @click="showResult('保存失败，请重试', 'error')">显示失败结果</v-btn>

  <v-snackbar v-model="snackbar" :color="messageType" timeout="3000">
    {{ message }}
    <template #actions>
      <v-btn variant="text" @click="snackbar = false">关闭</v-btn>
    </template>
  </v-snackbar>
</template>
```

Snackbar适合短暂操作反馈。必须立即处理的重要错误应保留在页面或表单附近，不能只依赖自动消失的通知。

## 7. 页面状态组合

业务页面至少区分：

| 状态 | 建议显示 |
| --- | --- |
| loading | Progress与禁用的重复操作 |
| success + data | 表格或卡片 |
| success + empty | 明确的空数据说明 |
| error | 错误信息和重试按钮 |
| saving/deleting | 操作中的按钮状态 |

Vuetify负责表现这些状态，状态本身仍由组件、Composable、Pinia或API层按照Vue课程中的职责管理。

## 8. 练习与验收

1. 使用`v-form`实现Task标题与优先级校验。
2. 用`v-data-table`显示六个统一Task字段。
3. 增加加载、空数据和失败三种状态。
4. 使用Dialog确认删除，失败时不关闭Dialog。
5. 使用Snackbar分别显示成功和失败反馈。
6. 通过键盘完成输入、提交、取消和关闭操作，并检查Console。
