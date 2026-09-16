# 第1章 Vuetify 3安装、插件注册与应用结构

Vuetify是面向Vue的UI组件库，提供按钮、表单、数据表格、反馈组件、布局和主题系统。本课程统一使用Vue 3、Vite、Composition API和Vuetify 3。开始前应已经能创建并运行Vue项目，理解组件、Props、事件、插槽和`v-model`。

## 1. Vuetify承担什么任务

Vuetify把常见界面结构封装成Vue组件，并通过Props、事件、插槽和`v-model`使用。它能统一外观和常见交互，但不能代替：

- 业务数据和状态设计；
- 表单与后端校验；
- Router和API层；
- 权限控制；
- 键盘操作、焦点和错误状态检查。

## 2. 在现有Vue项目中安装

在包含`package.json`的项目目录执行：

```bash
npm install vuetify @mdi/font
```

`vuetify`提供组件库，`@mdi/font`提供本课程示例使用的Material Design Icons。依赖的具体版本以项目`package.json`和团队规则为准，不在同一项目混用Vuetify 2与Vuetify 3写法。

## 3. 建立Vuetify插件

新建`src/plugins/vuetify.js`：

```js
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

export const vuetify = createVuetify({
  components,
  directives,
})
```

`createVuetify(options)`创建插件实例。这里显式注册全部组件和指令，便于新人理解；采用自动导入插件的既有项目应遵守原配置，不重复注册。

修改`src/main.js`：

```js
import { createApp } from 'vue'
import App from './App.vue'
import { vuetify } from './plugins/vuetify'

createApp(App)
  .use(vuetify)
  .mount('#app')
```

`app.use(vuetify)`必须在`mount()`之前调用。运行：

```bash
npm run dev
```

如果出现组件无法解析，依次检查依赖是否安装、插件路径是否正确、样式是否导入以及`use(vuetify)`是否存在。

## 4. 最小可运行组件

用以下内容替换`src/App.vue`：

```vue
<template>
  <v-app>
    <v-main>
      <v-container>
        <h1 class="text-h4 mb-4">WorkHub任务管理</h1>
        <v-btn color="primary" prepend-icon="mdi-plus">
          新增任务
        </v-btn>
      </v-container>
    </v-main>
  </v-app>
</template>
```

页面显示标题和带图标按钮，说明组件、基础样式与图标都已加载。

## 5. 应用结构

```text
v-app
├─ v-app-bar
├─ v-navigation-drawer
└─ v-main
   └─ v-container
      └─ 页面内容
```

| 组件 | 作用 | 是否必须 |
| --- | --- | --- |
| `v-app` | 提供应用级主题、默认值和布局上下文 | 应用根组件使用 |
| `v-main` | 放置主要内容并配合应用布局 | 常规页面推荐 |
| `v-container` | 控制内容宽度和间距 | 按页面需要 |
| `v-app-bar` | 顶部操作和导航区域 | 可选 |
| `v-navigation-drawer` | 侧边导航区域 | 可选 |
| `v-footer` | 页脚区域 | 可选 |

## 6. WorkHub应用骨架

```vue
<template>
  <v-app>
    <v-app-bar color="primary">
      <v-app-bar-title>WorkHub</v-app-bar-title>
    </v-app-bar>

    <v-navigation-drawer permanent>
      <v-list nav>
        <v-list-item
          title="任务一览"
          prepend-icon="mdi-format-list-checks"
        />
        <v-list-item title="设置" prepend-icon="mdi-cog" />
      </v-list>
    </v-navigation-drawer>

    <v-main>
      <v-container class="py-6">
        <h1 class="text-h4">任务一览</h1>
      </v-container>
    </v-main>
  </v-app>
</template>
```

`v-app-bar`和`v-navigation-drawer`参与Vuetify布局，`v-main`会为它们保留内容区域。是否永久显示Drawer应根据后续响应式规格调整。

## 7. 练习与验收

1. 在现有Vue JavaScript项目安装并注册Vuetify 3。
2. 建立WorkHub应用栏、侧边导航和主内容。
3. 增加一个带`mdi-plus`图标的新增按钮。
4. 删除`use(vuetify)`观察错误，再恢复并记录原因。
5. 执行`npm run build`，确认生产构建成功。

验收时确认页面可打开、图标可见、Console无错误，并能说明`plugins/vuetify.js`和`main.js`各自职责。
