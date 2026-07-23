# 第二节：组件

> 🎯 教学目标：  
>
> - 理解 Vuetify 的最小框架结构（必须组件）  
> - 掌握常用 UI 组件  
> - 学会在 Vue 3 + Vuetify 中结合 JavaScript（v-model、事件、数据）完成交互页面  

---

## 一、必须组件（应用框架基础）

### 1️⃣ `<v-app>`

- **功能**：所有 Vuetify 应用的根容器，提供主题、响应式和样式上下文。  
- **作用**：是 Vuetify 应用的入口，所有组件必须放在其中，否则样式会失效。  
- **常用属性**：  
    - `theme`：应用主题（light/dark）  
    - `id`：定义唯一 ID，用于 CSS 选择器  

📘 **示例：**

```vue
<v-app>
  <v-main>
    <v-container>
      <v-btn color="primary">Hello Vuetify</v-btn>
    </v-container>
  </v-main>
</v-app>
```

---

### 2️⃣ `<v-main>`

- **功能**：定义主内容区，自动避开导航栏或页脚，保持页面布局整齐。  
- **常用属性**：  
    - `tag`：自定义 HTML 标签（默认是 `<main>`）  
    - `class`：应用自定义样式  

📘 **示例：**

```vue
<v-app>
  <v-main>
    <v-container>这里是主内容</v-container>
  </v-main>
</v-app>
```

---

### 3️⃣ `<v-container>`

- **功能**：页面的主要布局容器，提供左右边距、上下留白与响应式宽度。  
- **常用属性**：  
    - `fluid`：让容器宽度 100%，无边距  
    - `class="pa-4"`：控制内边距（padding）  
    - `style`：自定义样式  

📘 **示例：**

```vue
<v-container fluid>
  <v-card class="pa-3">自适应宽度容器</v-card>
</v-container>
```

---

### 4️⃣ `<v-row>` 与 `<v-col>`

- **功能**：基于 12 列栅格系统的布局方式，实现响应式排列。  
- **常用属性**：
    - `cols`：每列宽度（1~12）  
    - `sm / md / lg`：不同屏幕尺寸下的列宽  
    - `align / justify`：垂直与水平对齐方式  

📘 **示例：**

```vue
<v-container>
  <v-row>
    <v-col cols="12" sm="6">
      <v-card>左侧内容</v-card>
    </v-col>
    <v-col cols="12" sm="6">
      <v-card>右侧内容</v-card>
    </v-col>
  </v-row>
</v-container>
```

---

## 二、常用组件详解（UI 与交互）

### 1️⃣ `<v-btn>` 按钮

- **功能**：触发用户操作事件。  
- **常用属性**：  
    - `color`：按钮颜色（primary / error / success 等）  
    - `variant`：样式类型（flat / outlined / tonal / text）  
    - `icon`：设置图标按钮  
    - `disabled`：禁用状态  
- **JS 交互**：通过 `@click` 绑定事件。

📘 **示例：**

```vue
<v-btn color="primary" @click="submitForm">提交</v-btn>

<script setup>
function submitForm() {
  alert('已提交！')
}
</script>
```

---

### 2️⃣ `<v-text-field>` 文本输入框

- **功能**：用户输入单行文本的控件。  
- **常用属性**：  
    - `v-model`：绑定变量，实现双向数据同步  
    - `label`：显示输入框标题  
    - `type`：输入类型（text / password / email）  
    - `clearable`：显示清除按钮  
    - `rules`：验证规则数组（如必填、格式）  

📘 **示例：**

```vue
<v-text-field
  label="用户名"
  v-model="username"
  clearable
  :rules="[v => !!v || '请输入用户名']"
/>

<script setup>
import { ref } from 'vue'
const username = ref('')
</script>
```

---

### 3️⃣ `<v-select>` 下拉选择框

- **功能**：用于从多个选项中选择一个。  
- **常用属性**：  
    - `v-model`：当前选中值  
    - `:items`：选项数组  
    - `label`：标题文字  
    - `multiple`：允许多选  
- **JS 交互**：结合数组或字符串变量，响应选择变化。  

📘 **示例：**

```vue
<v-select
  label="性别"
  :items="['男', '女', '其他']"
  v-model="gender"
/>

<script setup>
import { ref } from 'vue'
const gender = ref('')
</script>
```

---

### 4️⃣ `<v-card>` 卡片组件

- **功能**：展示信息区块的容器，可包含标题、内容、操作。  
- **结构**：  
    - `<v-card-title>`：标题区域  
    - `<v-card-text>`：内容区域  
    - `<v-card-actions>`：按钮区域  

📘 **示例：**

```vue
<v-card elevation="2" class="pa-3">
  <v-card-title>用户信息</v-card-title>
  <v-card-text>姓名：{{ user }}<br>邮箱：{{ email }}</v-card-text>
  <v-card-actions>
    <v-btn color="primary">编辑</v-btn>
  </v-card-actions>
</v-card>

<script setup>
const user = 'Tom'
const email = 'tom@mail.com'
</script>
```

---

### 5️⃣ `<v-data-table>` 数据表格

- **功能**：展示结构化数据列表，支持分页、排序、搜索。  
- **常用属性**：  
    - `:headers`：表头配置  
    - `:items`：数据数组  
    - `items-per-page`：每页显示行数  
    - `class="elevation-2"`：添加阴影  

📘 **示例：**

```vue
<v-data-table 
    :headers="headers" 
    :items="users" 
    class="elevation-2" 
/>

<script setup>
const headers = [
  { title: '姓名', key: 'name' },
  { title: '邮箱', key: 'email' },
  { title: '角色', key: 'role' },
]

const users = [
  { name: 'Tom', email: 'tom@mail.com', role: '管理员' },
  { name: 'Jerry', email: 'jerry@mail.com', role: '用户' },
]
</script>
```

---

### 6️⃣ `<v-dialog>` 弹出对话框

- **功能**：在当前页面上弹出模态窗口。  
- **常用属性**：  
    - `v-model`：控制弹窗开关  
    - `persistent`：点击背景不关闭  
    - `max-width`：限制弹窗宽度  

📘 **示例：**

```vue
<v-btn color="primary" @click="show = true">打开弹窗</v-btn>

<v-dialog v-model="show" persistent max-width="400">
  <v-card>
    <v-card-title>提示</v-card-title>
    <v-card-text>确定要删除该用户吗？</v-card-text>
    <v-card-actions>
      <v-btn color="error" @click="confirm">确认</v-btn>
      <v-btn @click="show = false">取消</v-btn>
    </v-card-actions>
  </v-card>
</v-dialog>

<script setup>
import { ref } from 'vue'
const show = ref(false)
function confirm() {
  alert('已删除')
  show.value = false
}
</script>
```

---

### 7️⃣ `<v-snackbar>` 操作提示条

- **功能**：显示操作结果或提醒信息。  
- **常用属性**：  
    - `v-model`：控制显示  
    - `timeout`：自动关闭时间（ms）  
    - `color`：提示颜色  

📘 **示例：**

```vue
<v-snackbar v-model="snackbar" color="success" timeout="2000">
  操作成功！
</v-snackbar>

<script setup>
import { ref } from 'vue'
const snackbar = ref(false)
</script>
```

---

## 三、综合实战（小型用户管理页面）

```vue
<template>
  <v-container>
    <v-btn color="primary" @click="dialog = true">新增用户</v-btn>

    <v-dialog v-model="dialog" max-width="400">
      <v-card>
        <v-card-title>添加用户</v-card-title>
        <v-card-text>
          <v-text-field label="姓名" v-model="newUser.name" />
          <v-text-field label="邮箱" v-model="newUser.email" />
        </v-card-text>
        <v-card-actions>
          <v-btn color="success" @click="addUser">保存</v-btn>
          <v-btn @click="dialog = false">取消</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-data-table :headers="headers" :items="users" class="mt-5" />
    <v-snackbar v-model="snackbar" color="success" timeout="2000">
      用户已添加！
    </v-snackbar>
  </v-container>
</template>

<script setup>
import { ref } from 'vue'

const dialog = ref(false)
const snackbar = ref(false)
const newUser = ref({ name: '', email: '' })
const users = ref([{ name: 'Tom', email: 'tom@mail.com' }])
const headers = [
  { title: '姓名', key: 'name' },
  { title: '邮箱', key: 'email' },
]

function addUser() {
  if (newUser.value.name && newUser.value.email) {
    users.value.push({ ...newUser.value })
    newUser.value = { name: '', email: '' }
    dialog.value = false
    snackbar.value = true
  }
}
</script>
```

---

## 四、总结

| 分类 | 组件 | 功能说明 |
|------|------|-----------|
| 框架结构 | v-app / v-main / v-container | 应用布局骨架 |
| 输入类 | v-text-field / v-select | 获取用户输入 |
| 操作类 | v-btn | 触发事件 |
| 展示类 | v-card / v-data-table | 显示信息或数据 |
| 交互类 | v-dialog / v-snackbar | 弹窗与提示反馈 |

✅ **一句话总结：**  
> Vuetify 的学习顺序应从「框架组件」→「交互组件」→「数据组件」→「反馈组件」，  
> 再结合 Vue 的 v-model 与事件机制，即可构建完整应用。
