# 第三节：Vuetify 布局与主题系统（响应式 + 暗黑模式）

> 🎯 教学目标：  
>
> - 掌握 Vuetify 布局系统（Grid / Flex / Container）  
> - 理解响应式断点（Breakpoints）与适配逻辑  
> - 掌握主题系统的自定义与暗黑模式切换  

---

## 一、布局系统总览

Vuetify 的布局系统基于 **Flexbox + 12 列栅格系统（Grid System）**，  
核心组件有：

| 组件 | 功能 |
|------|------|
| `<v-container>` | 页面内容容器（可固定宽度或全宽） |
| `<v-row>` | 行容器，用于控制子元素布局 |
| `<v-col>` | 列容器，用于分割空间 |
| `<v-spacer>` | 自动填充空白（用于左右对齐） |
| `<v-responsive>` | 自适应比例容器（常用于图片、视频） |

---

### 1️⃣ `<v-container>`：容器

- **作用**：控制页面宽度与边距。
- **常用属性**：
    - `fluid`：设置为 100% 宽度。
    - `class="pa-4"`：控制内边距。
    - `tag="section"`：改变 HTML 元素类型。

📘 **示例：**

```vue
<v-container class="pa-4">
  <v-card class="pa-3">固定宽度容器</v-card>
</v-container>

<v-container fluid>
  <v-card class="pa-3">全宽容器（fluid）</v-card>
</v-container>
```

---

### 2️⃣ `<v-row>` + `<v-col>`：网格布局

- **原理**：每一行被分成 12 份，通过 `cols` 属性定义每列占比。
- **响应式属性**：
    - `cols`：默认列宽（1~12）
    - `sm`：≥600px 时列宽
    - `md`：≥960px 时列宽
    - `lg`：≥1280px 时列宽
    - `xl`：≥1920px 时列宽

📘 **示例：响应式布局**

```vue
<v-container>
  <v-row>
    <v-col cols="12" sm="6" md="4">
      <v-card class="pa-3">区块1</v-card>
    </v-col>
    <v-col cols="12" sm="6" md="4">
      <v-card class="pa-3">区块2</v-card>
    </v-col>
    <v-col cols="12" sm="6" md="4">
      <v-card class="pa-3">区块3</v-card>
    </v-col>
  </v-row>
</v-container>
```

💡 当窗口变小时，每个区块会自动折叠为单列。

---

### 3️⃣ `<v-spacer>`：自动空白分配

- **作用**：自动填充剩余空间，用于对齐布局。

📘 **示例：**

```vue
<v-row align="center">
  <v-btn color="primary">返回</v-btn>
  <v-spacer></v-spacer>
  <v-btn color="success">下一步</v-btn>
</v-row>
```

📌 两个按钮会自动分布在两端。

---

### 4️⃣ `<v-responsive>`：响应式比例容器

- **作用**：保持图片或视频宽高比。
- **常用属性**：
    - `aspect-ratio`：比例（如 `16/9`, `4/3`, `1`）

📘 **示例：**

```vue
<v-responsive aspect-ratio="16/9">
  <v-img src="https://picsum.photos/800/400" cover></v-img>
</v-responsive>
```

---

## 二、响应式设计与断点系统

Vuetify 自动根据屏幕宽度进行布局切换。

| 名称 | 尺寸范围 | 说明 |
|------|-----------|------|
| xs | < 600px | 手机 |
| sm | ≥ 600px | 小平板 |
| md | ≥ 960px | 平板或小电脑 |
| lg | ≥ 1280px | 桌面 |
| xl | ≥ 1920px | 大屏 |

📘 **示例：显示不同内容**

```vue
<v-container>
  <v-alert v-if="$vuetify.display.smAndDown" type="info">
    当前为小屏设备
  </v-alert>
  <v-alert v-else type="success">
    当前为桌面端
  </v-alert>
</v-container>
```

🧠 **说明：**

- `$vuetify.display` 是 Vuetify 提供的响应式检测对象。
- 常用属性：
    - `xsOnly`, `smAndDown`, `mdAndUp`, `lgAndUp`, `xlOnly`

---

## 三、主题系统（Theme System）

Vuetify 允许在全局设置自定义主题颜色。

### 1️⃣ 基本配置

📘 **main.js**

```js
import 'vuetify/styles'
import { createVuetify } from 'vuetify'

const vuetify = createVuetify({
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        colors: {
          primary: '#1976D2',
          secondary: '#424242',
          accent: '#82B1FF',
        },
      },
      dark: {
        colors: {
          primary: '#90CAF9',
          background: '#121212',
        },
      },
    },
  },
})

export default vuetify
```

---

### 2️⃣ 在组件中使用主题颜色

📘 **示例：**

```vue
<v-btn color="primary">主按钮</v-btn>
<v-card color="secondary" class="pa-3">次要卡片</v-card>
<v-alert color="accent" variant="tonal">提示信息</v-alert>
```

---

### 3️⃣ 暗黑模式切换

可以通过 `$vuetify.theme.global.name` 动态切换主题。

📘 **示例：**

```vue
<template>
  <v-app>
    <v-app-bar color="primary" dark app>
      <v-app-bar-title>主题切换示例</v-app-bar-title>
      <v-spacer />
      <v-switch v-model="isDark" label="Dark Mode" />
    </v-app-bar>

    <v-main>
      <v-container>
        <v-card class="pa-3">当前主题：{{ themeName }}</v-card>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import { computed, watch, ref } from 'vue'
import { useTheme } from 'vuetify'

const theme = useTheme()
const isDark = ref(false)

const themeName = computed(() => theme.global.name.value)

watch(isDark, (val) => {
  theme.global.name.value = val ? 'dark' : 'light'
})
</script>
```

---

## 四、实战：响应式仪表盘布局 + 暗黑模式

```vue
<template>
  <v-app>
    <v-app-bar app color="primary" dark>
      <v-app-bar-title>管理后台</v-app-bar-title>
      <v-spacer />
      <v-switch v-model="isDark" label="暗黑模式" />
    </v-app-bar>

    <v-main>
      <v-container fluid>
        <v-row>
          <v-col cols="12" md="3">
            <v-card class="pa-3" color="primary" dark>菜单栏</v-card>
          </v-col>
          <v-col cols="12" md="9">
            <v-card class="pa-4">
              <v-card-title>仪表盘内容</v-card-title>
              <v-card-text>这里显示主要内容区。</v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useTheme } from 'vuetify'
const theme = useTheme()
const isDark = ref(false)
watch(isDark, val => theme.global.name.value = val ? 'dark' : 'light')
</script>
```

---

## 五、总结

| 功能 | 组件 / API | 说明 |
|------|-------------|------|
| 布局 | v-container / v-row / v-col | 控制页面结构 |
| 响应式 | $vuetify.display | 自动检测屏幕宽度 |
| 空白对齐 | v-spacer | 左右推开元素 |
| 比例控制 | v-responsive | 图片/视频自适应 |
| 主题系统 | createVuetify.theme | 定义全局颜色 |
| 动态切换 | useTheme() | 实现暗黑 / 明亮切换 |

✅ **一句话总结：**
> Vuetify 的布局系统让页面自适应各种设备，而主题系统则统一了视觉风格。  
> 掌握两者结合，你就能快速构建「响应式 + 多主题」的现代化界面。
