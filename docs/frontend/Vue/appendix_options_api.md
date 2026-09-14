# 附录：阅读旧Vue项目（Options API）

Vue 3新开发主线使用Composition API与`<script setup>`。维护旧项目时仍可能看到Options API，本附录只要求能阅读和局部修改，不要求用它重新开发WorkHub。

## 1. 基本结构

```js
export default {
  props: {
    title: String,
  },
  data() {
    return {
      count: 0,
    }
  },
  computed: {
    doubledCount() {
      return this.count * 2
    },
  },
  watch: {
    count(newValue, oldValue) {
      console.log(oldValue, '→', newValue)
    },
  },
  created() {
    console.log('组件已创建')
  },
  mounted() {
    console.log('DOM已挂载')
  },
  methods: {
    increment() {
      this.count++
    },
  },
}
```

这里的`this`指向当前组件实例。不要把箭头函数机械用于`data`、`computed`和`methods`，否则`this`可能不是预期的组件实例。

## 2. 与Composition API对应

| Composition API | Options API | 作用 |
| --- | --- | --- |
| `ref()`、`reactive()` | `data()` | 声明响应式状态 |
| 普通函数 | `methods` | 处理事件和业务动作 |
| `computed()` | `computed` | 声明派生状态 |
| `watch()` | `watch` | 状态变化后执行副作用 |
| setup阶段代码 | `created` | DOM挂载前的初始化 |
| `onMounted()` | `mounted` | DOM挂载后的处理 |

两种API解决的是相同组件问题，但组织代码的方式不同。维护既有文件时遵守原项目风格，不在同一次小改修中无目的地整体迁移。

## 3. 阅读顺序

1. 从`props`确认组件输入。
2. 从`data`确认组件自己保存的状态。
3. 从`computed`确认派生值。
4. 从模板事件找到`methods`。
5. 查看`watch`和生命周期中的副作用及清理逻辑。

## 4. 练习与检查点

把示例中的`count`改为任务数量，增加一个清零方法，并说明若改为Composition API分别会放到哪里。

- [ ] 能找到Options API组件的输入、状态、派生值和方法。
- [ ] 能说明常见生命周期与Composition API的对应关系。
- [ ] 能局部修改旧代码而不无目的迁移整个组件。

