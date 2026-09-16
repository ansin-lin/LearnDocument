# 第3章 Vuetify 3布局、响应式与主题

本章在前两章的WorkHub页面上整理栅格、不同宽度下的显示方式和亮暗主题。布局从内容需求出发，不按“手机、平板、电脑”的名称猜测断点。

## 1. 容器、行与列

Vuetify栅格以12列为基础：

```vue
<template>
  <v-container>
    <v-row>
      <v-col cols="12" md="4">
        <v-card class="pa-4">待处理：3</v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card class="pa-4">处理中：2</v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card class="pa-4">已完成：8</v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
```

`cols="12"`作为最小宽度的规则，每项占满一行；`md="4"`从md开始每项占4列，因此一行显示三项。`v-row`应包住`v-col`，不要跳过这一层级。

## 2. 常用布局组件

| 组件 | 作用 | 常见场景 |
| --- | --- | --- |
| `v-container` | 页面内容宽度和左右间距 | 页面主体 |
| `v-row` / `v-col` | 响应式12列布局 | 表单、统计卡片 |
| `v-spacer` | 占用剩余空间 | 工具栏两端对齐 |
| `v-responsive` | 保持内容比例 | 图片、视频 |

```vue
<template>
  <v-toolbar>
    <v-toolbar-title>任务一览</v-toolbar-title>
    <v-spacer />
    <v-btn color="primary">新增任务</v-btn>
  </v-toolbar>

  <v-responsive :aspect-ratio="16 / 9" max-width="640">
    <v-img src="/images/workhub-help.png" alt="WorkHub操作说明" cover />
  </v-responsive>
</template>
```

装饰图片可使用空`alt`；传达信息的图片必须提供等价替代文字。

## 3. 断点与useDisplay

Vuetify 3默认提供`xs`、`sm`、`md`、`lg`、`xl`和`xxl`。具体阈值可以配置，所以真实项目要查看Vuetify插件设置，不把设备名称当作固定规则。

模板需要根据宽度切换组件结构时，使用`useDisplay()`：

```vue
<script setup>
import { useDisplay } from 'vuetify'

const { mdAndUp } = useDisplay()
</script>

<template>
  <v-navigation-drawer v-if="mdAndUp" permanent>
    <v-list nav>
      <v-list-item title="任务一览" />
    </v-list>
  </v-navigation-drawer>

  <v-btn v-else prepend-icon="mdi-menu">
    打开菜单
  </v-btn>
</template>
```

`useDisplay()`返回响应式的视口信息，窗口宽度变化时模板会重新判断。仅调整列宽时优先使用`cols`、`sm`、`md`等响应式Props，不必为每项布局都写JavaScript。

## 4. 间距与显示工具类

```vue
<template>
  <section class="pa-4 pa-md-6">
    <h2 class="text-h5 mb-4">筛选条件</h2>
    <div class="d-flex flex-column flex-md-row ga-3">
      <v-text-field label="任务名" hide-details />
      <v-select label="状态" hide-details />
    </div>
  </section>
</template>
```

`pa-4`设置基础内边距，`pa-md-6`从md开始扩大；`flex-column flex-md-row`先纵向排列，再从md改成横向。工具类过多且互相覆盖时，应提取职责明确的自定义class。

## 5. 在插件中定义主题

修改`src/plugins/vuetify.js`中的`createVuetify()`配置：

```js
export const vuetify = createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'workhubLight',
    themes: {
      workhubLight: {
        dark: false,
        colors: {
          primary: '#3155A6',
          secondary: '#52606D',
          error: '#B42318',
          background: '#F7F8FA',
          surface: '#FFFFFF',
        },
      },
      workhubDark: {
        dark: true,
        colors: {
          primary: '#9DB5FF',
          secondary: '#BAC4D0',
          error: '#FFB4AB',
          background: '#111318',
          surface: '#1A1C21',
        },
      },
    },
  },
})
```

`defaultTheme`指定初始主题，`themes`按名称保存主题配置，`dark`决定该主题使用亮色还是暗色语义。颜色仍要检查文字与背景对比度。

## 6. 使用和切换主题

```vue
<script setup>
import { computed } from 'vue'
import { useTheme } from 'vuetify'

const theme = useTheme()
const isDark = computed(() => theme.global.current.value.dark)

function toggleTheme() {
  theme.global.name.value = isDark.value
    ? 'workhubLight'
    : 'workhubDark'
}
</script>

<template>
  <v-btn
    variant="text"
    :prepend-icon="isDark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
    @click="toggleTheme"
  >
    {{ isDark ? '切换亮色' : '切换暗色' }}
  </v-btn>
</template>
```

`useTheme()`取得当前Vuetify主题服务；修改`theme.global.name.value`会切换主题。是否保存用户选择、是否跟随系统主题，应由产品规格决定。

## 7. WorkHub响应式页面

```vue
<script setup>
import { ref } from 'vue'
import { useDisplay } from 'vuetify'

const drawer = ref(false)
const { mdAndUp } = useDisplay()
</script>

<template>
  <v-app>
    <v-app-bar color="primary">
      <v-app-bar-nav-icon
        v-if="!mdAndUp"
        aria-label="打开导航"
        @click="drawer = !drawer"
      />
      <v-app-bar-title>WorkHub</v-app-bar-title>
    </v-app-bar>

    <v-navigation-drawer v-model="drawer" :permanent="mdAndUp">
      <v-list nav>
        <v-list-item title="任务一览" prepend-icon="mdi-format-list-checks" />
      </v-list>
    </v-navigation-drawer>

    <v-main>
      <v-container class="py-6">
        <v-row>
          <v-col cols="12" md="4"><v-card class="pa-4">待处理：3</v-card></v-col>
          <v-col cols="12" md="4"><v-card class="pa-4">处理中：2</v-card></v-col>
          <v-col cols="12" md="4"><v-card class="pa-4">已完成：8</v-card></v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>
```

小屏通过按钮控制Drawer，大屏保持Drawer显示；统计卡片在小屏单列、md以上三列。

## 8. 练习与验收

1. 为WorkHub定义亮色和暗色主题，并实现切换按钮。
2. 小屏使用可开关Drawer，md以上永久显示。
3. 让筛选区和统计卡片在360px、960px和1280px附近合理排列。
4. 检查长任务名、空数据、读取失败、禁用按钮和Dialog。
5. 只使用键盘完成导航、主题切换和主要操作。
6. 执行`npm run build`并确认Console无错误。
