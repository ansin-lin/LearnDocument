# Vuetify 基础结构

> 本文档教你从零开始了解 **Vuetify** 的核心结构和必备组件，包含安装、基本层级结构、必要组件、应用框架示例等。

---

## 📘 一、Vuetify 的基础概念

**Vuetify** 是一个基于 **Vue.js** 的界面 UI 框架，提供了完整的 **Material Design** 风格组件和响应式布局系统。

> 目标：通过 Vuetify，很快搭建出符合现代设计风格的网页和后台界面。

---

## ⚙️ 二、安装与环境搭建

### 1. 创建 Vue 3 项目

```bash
npm create vue@latest my-vuetify-app
cd my-vuetify-app
npm install
```

### 2. 安装 Vuetify

```bash
npm install vuetify @mdi/font
```

### 3. main.js 中引入

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import '@mdi/font/css/materialdesignicons.css'

const vuetify = createVuetify({ components, directives })
createApp(App).use(vuetify).mount('#app')
```

---

## 🧱 三、Vuetify 的基本层级结构

在一个标准的 Vuetify 应用中，基本层级结构如下：

```vue
<v-app>
  <v-app-bar>...</v-app-bar>      
  <v-navigation-drawer>...</v-navigation-drawer>
  <v-main>
    <v-container>...</v-container>
  </v-main>
  <v-footer>...</v-footer>
</v-app>
```

### 🔍 基本解释

| 组件名 | 是否必须 | 作用 |
|---------|-----------|------|
| `<v-app>` | ✅ 必须 | 应用的根容器 |
| `<v-main>` | ✅ 推荐 | 主内容容器 |
| `<v-container>` | ✅ 推荐 | 提供左右留白、响应式布局基础 |
| `<v-row>` / `<v-col>` | ⚙️ 布局辅助 | 构建网格系统 |
| `<v-app-bar>` | 可选 | 顶部导航栏 |
| `<v-navigation-drawer>` | 可选 | 左侧菜单栏 |
| `<v-footer>` | 可选 | 页脚 |

---

## 🧩 四、最小可运行结构

```vue
<template>
  <v-app>
    <v-main>
      <v-container>
        <v-btn color="primary">你好 Vuetify</v-btn>
      </v-container>
    </v-main>
  </v-app>
</template>
```

> 💡 解释：
>
> - `<v-app>`：Vuetify 的根容器，提供主题、CSS 重置和上下文。
> - `<v-main>`：定义主内容区域。
> - `<v-container>`：提供标准化的页面边距与布局间距。

---

## 🧭 五、标准页面结构示例

```vue
<template>
  <v-app>
    <v-app-bar color="primary" dark app>
      <v-app-bar-title>我的应用</v-app-bar-title>
    </v-app-bar>

    <v-navigation-drawer app permanent>
      <v-list>
        <v-list-item title="首页" prepend-icon="mdi-home" />
        <v-list-item title="设置" prepend-icon="mdi-cog" />
      </v-list>
    </v-navigation-drawer>

    <v-main>
      <v-container class="py-4">
        <v-card>
          <v-card-title>欢迎使用 Vuetify</v-card-title>
          <v-card-text>这里是主要内容区域。</v-card-text>
        </v-card>
      </v-container>
    </v-main>

    <v-footer app>
      <span class="mx-auto">© 2025 My App</span>
    </v-footer>
  </v-app>
</template>
```

---

## 🎨 六、推荐的基础组件总结

| 分类 | 必需组件 | 说明 |
|------|-----------|------|
| 根层级 | `v-app` | 所有页面必须包裹在它内 |
| 主内容层 | `v-main` | 承载主要内容区域 |
| 布局层 | `v-container`, `v-row`, `v-col` | 实现响应式布局 |
| 框架层 | `v-app-bar`, `v-navigation-drawer`, `v-footer` | 可选，构成完整应用骨架 |

---

## 🧠 七、最佳实践与提示

1. 所有页面的最外层必须有 `<v-app>`。
2. 主内容建议使用 `<v-main>` + `<v-container>`。
3. 使用 `<v-row>` + `<v-col>` 实现响应式栅格布局。
4. 在 `createVuetify()` 中配置主题（light/dark）。
5. 常见结构：导航栏 + 菜单 + 主内容 + 页脚。

---

## 🧾 八、总结

> 在 Vuetify 应用中：
>
> - **`<v-app>` 是唯一必须的顶级组件**；
> - **`<v-main>` + `<v-container>`** 构成主内容结构；
> - **`<v-row>` + `<v-col>`** 用于栅格布局；
> - **`<v-app-bar>`、`<v-navigation-drawer>`、`<v-footer>`** 构成完整框架。

✅ **一句话总结：**
> Vuetify 应用的根结构必须有 `<v-app>`，推荐配合 `<v-main>` 与 `<v-container>` 使用。
