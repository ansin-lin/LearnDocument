# 第 12 章 组件v-model与Attributes

本章按难度学习：【必须掌握】`modelValue`、`update:modelValue`、`defineModel()`、`$attrs`、Attribute Fallthrough、多根组件属性落点和`inheritAttrs`；【会读即可】命名v-model与`useAttrs()`。必须先理解Prop与事件契约，再使用这些封装能力。

## 本章目标与前置知识

完成后应能实现一个支持v-model的输入组件，控制Attributes落点，并明确多根组件中的属性落点。需要掌握第11章Props、Emits和单向数据流。本章示例使用TypeScript。

第11章已经能够通过Props向下传值、通过组件事件向上通知。本章学习两种组件封装能力：用组件`v-model`简化输入值通信，用Attributes保留原生HTML能力。Slots与provide/inject在第13章学习。

## 1. 组件v-model解决什么问题

父组件使用原生输入框时可以写：

```vue
<input v-model="keyword">
```

封装成`SearchInput`组件后，父组件仍希望保持相同用法：

```vue
<SearchInput v-model="keyword" />
```

这里的`keyword`仍由父组件拥有。子组件只负责显示当前值，并在用户输入后通知父组件更新。

```text
父组件keyword ──模型值──> SearchInput
父组件更新    <──更新事件── SearchInput
```

## 2. 先理解底层Prop与事件写法

组件`v-model`本质上是特定名称的Prop和事件组合。`SearchInput.vue`可以先用第11章知识实现：

```vue
<script setup lang="ts">
const props = defineProps<{ modelValue: string }>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function handleInput(event: Event): void {
  const input = event.currentTarget as HTMLInputElement
  emit('update:modelValue', input.value)
}
</script>

<template>
  <input
    type="search"
    :value="props.modelValue"
    @input="handleInput"
  >
</template>
```

父组件写：

```vue
<SearchInput v-model="keyword" />
```

Vue会把它理解为近似下面的写法：

```vue
<SearchInput
  :model-value="keyword"
  @update:model-value="keyword = $event"
/>
```

- `modelValue`保存父组件传入的当前值。
- `update:modelValue`表示子组件请求父组件更新该值。
- 子组件不能直接修改`props.modelValue`。

这种完整写法在旧项目和部分组件库中很常见，必须能读懂。

## 3. 使用defineModel简化

较新的Vue 3项目可以使用`defineModel()`：

```vue
<script setup lang="ts">
const model = defineModel<string>({ required: true })
</script>

<template>
  <input v-model.trim="model" type="search">
</template>
```

`defineModel()`是`script setup`编译宏，不需要导入。它声明模型Prop和对应更新事件，并返回一个可读写ref。模板输入修改`model`时，父组件绑定的`keyword`也会更新。

| 项目情况 | 常见写法 |
| --- | --- |
| 当前Vue版本支持且项目已采用 | `defineModel()` |
| 旧Vue 3项目 | `modelValue`与`update:modelValue` |
| 团队已经统一写法 | 遵守既有规范 |

不要为使用新语法而大范围改写稳定旧组件。

## 4. 会读即可：带名称的组件v-model

组件确实需要两个可独立更新的值时，可以给模型命名：

```vue
<DateRangeInput
  v-model:start-date="startDate"
  v-model:end-date="endDate"
/>
```

子组件：

```vue
<script setup lang="ts">
const startDate = defineModel<string>('startDate', { required: true })
const endDate = defineModel<string>('endDate', { required: true })
</script>

<template>
  <input v-model="startDate" type="date">
  <input v-model="endDate" type="date">
</template>
```

多个模型不是越多越好。组件需要大量可双向修改字段时，应检查是否应该传入表单对象、拆分组件，或改为提交事件。

## 5. 模型默认值和同步问题

如果子组件模型提供默认值，而父组件绑定值初始为`undefined`，父子初始状态可能不一致。业务输入组件优先由父组件明确提供初始值，或把模型声明为必填。

输入组件只负责值同步时使用组件`v-model`；“保存”“删除”“确认”等业务动作仍应使用明确的自定义事件。

## 6. 为什么需要属性透传

假设父组件使用一个封装后的按钮：

```vue
<BaseButton
  id="save-button"
  class="task-form__submit"
  aria-label="保存任务"
  disabled
>
  保存
</BaseButton>
```

父组件写的是Vue组件`BaseButton`，但浏览器真正能够识别`disabled`和`aria-label`的是组件内部的原生`button`。因此需要把这些属性传递到最终DOM元素，这个过程称为**属性透传**。

```text
父组件的<BaseButton disabled aria-label="保存任务">
                         ↓ 透传
子组件内部的<button disabled aria-label="保存任务">
                         ↓ 渲染
浏览器中的真实button元素
```

如果封装组件丢失了这些属性，可能出现按钮无法禁用、表单字段无法关联、自动化测试找不到元素或无障碍信息缺失等问题。

## 7. 哪些内容会进入Attributes

父组件交给子组件的内容可以分成三类：

```vue
<BaseButton
  label="保存"
  aria-label="保存任务"
  @click="saveTask"
/>
```

假设子组件声明：

```ts
defineProps<{ label: string }>()
defineEmits<{ click: [] }>()
```

那么：

| 父组件传入内容 | 子组件是否声明 | 归属 |
| --- | --- | --- |
| `label` | 在`defineProps()`中声明 | Props |
| `@click` | 在`defineEmits()`中声明 | 组件事件监听器 |
| `aria-label` | 没有声明 | Attributes |

也就是说，Attributes主要是父组件传入、但没有被Props或emits接收的内容。常见内容包括：

- `class`和`style`；
- `id`、`title`、`name`；
- `disabled`、`required`、`readonly`；
- `aria-label`、`aria-describedby`；
- `data-testid`；
- 没有被声明为组件事件的原生事件监听器。

组件真正依赖并参与业务判断的数据应声明为Props。例如按钮的业务权限、任务编号和显示模式不应为了少写声明而藏进`$attrs`。

## 8. 单根组件会自动透传

`BaseButton.vue`只有一个根元素：

```vue
<script setup lang="ts">
defineProps<{ label: string }>()
</script>

<template>
  <button class="base-button" type="button">
    {{ label }}
  </button>
</template>
```

父组件这样使用：

```vue
<BaseButton
  label="保存"
  id="save-button"
  aria-label="保存任务"
  disabled
/>
```

因为子组件只有一个根元素，Vue会把没有声明为Prop的`id`、`aria-label`和`disabled`自动放到根`button`上。浏览器最终得到近似结果：

```html
<button
  id="save-button"
  class="base-button"
  type="button"
  aria-label="保存任务"
  disabled
>
  保存
</button>
```

这种自动行为称为Attributes继承。它让简单包装组件继续保留原生元素的大部分能力。

### 8.1 class和style会合并

子组件已有`class="base-button"`，父组件又传入：

```vue
<BaseButton class="task-form__submit" label="保存" />
```

最终元素会同时包含两个类，而不是父级覆盖子级：

```html
<button class="base-button task-form__submit">保存</button>
```

`style`也会合并；同一个CSS属性发生冲突时，要结合最终生成顺序和CSS优先级调查。业务组件不要依赖复杂覆盖关系维持外观。

### 8.2 事件监听器也可能透传

如果子组件没有声明`click`组件事件，父组件传入的`@click`可以作为监听器落到根`button`：

```vue
<BaseButton label="保存" @click="saveTask" />
```

如果组件需要把`click`作为自己的公开组件事件处理，应在`defineEmits<{ click: [] }>()`中声明并主动`emit('click')`。是否属于原生透传还是组件事件，取决于组件契约，不能只看父组件都写成`@click`。

## 9. 自动透传为什么有时会传错位置

下面的输入组件虽然只有一个根元素，但根元素是包装`div`：

```vue
<template>
  <div class="field">
    <label for="keyword">关键字</label>
    <input id="keyword">
  </div>
</template>
```

父组件写：

```vue
<SearchField disabled aria-label="任务关键字" />
```

默认情况下，`disabled`和`aria-label`会落到根`div`，而不是内部`input`。`div`不具备输入框的禁用行为，因此页面看起来“传了disabled”，输入框却仍可操作。

这说明：单根组件能够自动透传，不代表自动落点一定符合组件设计。包装型组件必须确认属性真正应该交给哪个原生元素。

## 10. 必须掌握：关闭自动继承并指定落点

### 10.1 单根包装组件

```vue
<script setup lang="ts">
defineOptions({ inheritAttrs: false })
</script>

<template>
  <div class="field">
    <label for="keyword">关键字</label>
    <input id="keyword" v-bind="$attrs">
  </div>
</template>
```

逐项理解：

- `inheritAttrs: false`阻止Vue把Attributes自动放到根`div`。
- `$attrs`表示当前组件收到的Attributes集合。
- `v-bind="$attrs"`把集合中的属性批量绑定到`input`。

父组件传入的`disabled`、`required`和`aria-label`现在会落到真实输入框。

### 10.2 多根组件

Vue 3允许多个根元素：

```vue
<template>
  <label for="keyword">关键字</label>
  <input id="keyword">
</template>
```

组件有两个根元素时，Vue无法判断Attributes应给`label`还是`input`，因此不会自动选择，并可能给出警告。解决方式仍是明确指定：

```vue
<script setup lang="ts">
defineOptions({ inheritAttrs: false })
</script>

<template>
  <label for="keyword">关键字</label>
  <input id="keyword" v-bind="$attrs">
</template>
```

多根组件不是错误，但组件开发者必须决定属性落点。

### 10.3 部分属性需要给不同元素怎么办

`v-bind="$attrs"`会把全部Attributes交给一个元素。如果`class`应该控制外层布局，而`disabled`应该交给内部输入框，就不适合不加区分地全部透传。

此时更清楚的设计通常是：

- 把组件真正需要控制的值声明为Props；
- 为外层样式提供明确Prop或组件约定；
- 让剩余原生输入属性通过`$attrs`交给`input`；
- 避免设计一个无法说明每项属性归属的万能包装组件。

### 10.4 扩展阅读：在脚本中读取Attributes

```vue
<script setup lang="ts">
import { useAttrs } from 'vue'

const attrs = useAttrs()

console.log(attrs.id)
console.log(attrs['aria-label'])
</script>
```

`useAttrs()`返回当前组件的Attributes对象。带连字符的名称使用方括号读取。

只有脚本确实需要检查或转交Attributes时才使用`useAttrs()`。模板中整体转发直接使用`$attrs`更清楚。`attrs`对象会反映最新Attributes，但它不是用于`watch()`的普通响应式状态；需要根据某个值执行业务逻辑时，应把该值声明为Prop。

### 10.5 调查透传问题的步骤

遇到“父组件明明传了属性却没有效果”时，按以下顺序检查：

1. 父组件传入的是固定字符串还是动态值。
2. 该名称是否已经被`defineProps()`或`defineEmits()`接收。
3. 子组件有一个根元素还是多个根元素。
4. 是否设置了`inheritAttrs: false`。
5. `v-bind="$attrs"`实际写在哪个原生元素上。
6. 使用浏览器Elements确认最终DOM是否具有该属性。
7. 检查属性是否适用于该元素，例如`disabled`对`div`没有按钮禁用效果。

不要只在Vue DevTools里看到组件收到属性就结束调查，最终行为由实际DOM元素决定。

### 10.6 封装时的边界

- 按钮组件明确`type`，避免在表单内意外提交。
- 输入组件把`disabled`、`required`、`name`和ARIA属性交给真实`input`。
- 不把来源不可信的任意对象整体`v-bind`到元素。
- `class`究竟控制外层还是内部元素，要形成稳定组件约定。
- Props负责业务契约，Attributes负责通用HTML能力，两者不要混用。

## 11. 常见错误

- 直接修改 modelValue Prop。
- 用组件 v-model 表达保存、删除或提交等业务动作。
- Attributes 落到包装元素而不是真正控件。
- 多根组件没有明确指定 Attributes 落点。

## 12. WorkHub练习与检查点

1. 用 modelValue 和 update:modelValue 实现一个文本输入组件。
2. 改用 defineModel<string>()，确认父组件使用方式不变。
3. 让 label、aria-describedby、class 和事件正确透传到内部 input。
4. 制造多根组件透传警告，使用 inheritAttrs 和 attrs 修正。

- [ ] 能解释组件 v-model 的 Prop 与事件本质。
- [ ] 能区分值同步与保存、删除等业务事件。
- [ ] 能控制 Attributes 的正确落点。






