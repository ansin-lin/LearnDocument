# React 基础到进阶面试教程

> 适合：刚学完 React 基础、准备前端面试、需要系统复习 React 核心概念的人。  
> 写法：每个问题都按「概念解释 → 关键点 → 简单示例 / 面试回答」整理。

---

## 一、React 基础

### 1. React 是什么？核心思想是什么？

React 是 Facebook 开源的一个用于构建用户界面的 JavaScript 库，主要用于构建单页应用中的视图层。

React 的核心思想是：

1. **组件化**
   - 页面被拆分成一个个独立组件。
   - 每个组件负责自己的结构、样式、状态和逻辑。

2. **声明式 UI**
   - 开发者只需要描述“页面应该长什么样”。
   - React 会根据数据变化自动更新 DOM。

3. **数据驱动视图**
   - 页面不是手动操作 DOM 更新的。
   - 而是通过 state / props 的变化触发重新渲染。

4. **虚拟 DOM**
   - React 会先在内存中生成虚拟 DOM。
   - 再通过 diff 算法比较变化，最后只更新真正需要变化的 DOM。

面试回答：

> React 是一个用于构建用户界面的 JavaScript 库，核心思想是组件化、声明式 UI 和数据驱动视图。开发者通过 state 和 props 描述界面状态，React 负责根据数据变化高效更新页面。

---

### 2. React 的设计理念是什么？

React 的设计理念主要包括：

1. **声明式**
   - 不直接告诉浏览器每一步怎么操作 DOM。
   - 只描述当前状态下 UI 应该是什么样。

2. **组件化**
   - 把复杂页面拆成多个可复用的小组件。
   - 降低代码复杂度，提高维护性。

3. **一次学习，多端使用**
   - React 本身只关注 UI。
   - React DOM 用于 Web。
   - React Native 用于移动端。

4. **单向数据流**
   - 数据通常从父组件流向子组件。
   - 数据变化更容易追踪。

5. **函数式思想**
   - UI 可以理解为 state 的函数：

   ```js
   UI = f(state)
   ```

---

### 3. React 是 MVC 吗？

严格来说，React 不是完整的 MVC 框架。

MVC 包括：

- Model：数据层
- View：视图层
- Controller：控制层

React 主要负责 View 层，也就是用户界面的渲染。

但是 React 组件中也可能包含状态和事件逻辑，所以在实际开发中，React 不只是单纯的 View，但它本身不是完整的 MVC 框架。

面试回答：

> React 不是完整的 MVC 框架，它更偏向于 View 层，用于构建 UI。数据管理、路由、请求等能力通常需要配合 Redux、React Router、Axios 等工具实现。

---

### 4. 什么是声明式 UI？

声明式 UI 指的是开发者只描述“结果是什么”，不需要关心“具体怎么一步步实现”。

传统命令式写法：

```js
const div = document.createElement('div')
div.innerText = 'Hello'
document.body.appendChild(div)
```

React 声明式写法：

```jsx
function App() {
  return <div>Hello</div>
}
```

React 会根据 JSX 自动创建和更新 DOM。

面试回答：

> 声明式 UI 是指开发者只描述某个状态下页面应该展示什么，而不用手动操作 DOM。React 会根据状态变化自动完成 UI 更新。

---

### 5. 什么是组件？

组件是 React 应用的基本组成单位。

一个组件可以包含：

- UI 结构
- 状态 state
- 属性 props
- 事件处理
- 生命周期 / Hooks 逻辑

示例：

```jsx
function UserCard(props) {
  return (
    <div>
      <h3>{props.name}</h3>
      <p>{props.age}</p>
    </div>
  )
}
```

组件的好处：

1. 复用性高
2. 结构清晰
3. 方便维护
4. 方便拆分复杂页面

---

### 6. 函数组件和类组件的区别？

### 函数组件

```jsx
function Hello() {
  return <div>Hello</div>
}
```

特点：

- 写法简单
- 通过 Hooks 管理状态和副作用
- 现在 React 官方更推荐

### 类组件

```jsx
class Hello extends React.Component {
  render() {
    return <div>Hello</div>
  }
}
```

特点：

- 使用 class 语法
- 通过 this.state 管理状态
- 通过生命周期方法处理副作用

区别总结：

| 对比项 | 函数组件 | 类组件 |
| --- | --- | --- |
| 写法 | function | class |
| 状态 | useState | this.state |
| 生命周期 | useEffect 等 Hooks | componentDidMount 等 |
| this | 没有 this 问题 | 需要处理 this |
| 推荐程度 | 推荐 | 老项目常见 |

---

### 7. 为什么现在推荐函数组件？

主要原因：

1. 写法更简单
2. 不需要处理 this 指向
3. Hooks 可以复用逻辑
4. 更适合 React 未来的发展方向
5. 代码更容易拆分和测试

类组件中的逻辑复用比较麻烦，常见方式有 HOC、Render Props，但代码容易嵌套复杂。

Hooks 出现后，可以通过自定义 Hook 抽离逻辑：

```jsx
function useUser() {
  const [user, setUser] = useState(null)

  useEffect(() => {
    // 请求用户信息
  }, [])

  return user
}
```

---

### 8. JSX 是什么？

JSX 是 JavaScript XML 的缩写，是一种 JavaScript 的语法扩展。

它允许我们在 JavaScript 中写类似 HTML 的结构。

```jsx
const element = <h1>Hello React</h1>
```

JSX 不是浏览器原生支持的语法，需要通过 Babel 编译成 JavaScript。

---

### 9. JSX 为什么不是 HTML？

JSX 看起来像 HTML，但它本质上是 JavaScript。

区别：

1. class 要写成 className
2. for 要写成 htmlFor
3. 事件使用驼峰命名，例如 onClick
4. 可以使用 JavaScript 表达式
5. 标签必须闭合
6. 只能返回一个根节点

示例：

```jsx
<label htmlFor="name" className="label">
  用户名
</label>
```

---

### 10. JSX 最终会被编译成什么？

旧版 React 中，JSX 会被编译成 `React.createElement`。

```jsx
const element = <h1>Hello</h1>
```

会被编译成：

```js
const element = React.createElement('h1', null, 'Hello')
```

React 17 之后引入了新的 JSX 转换方式，不一定需要手动引入 React，但本质上仍然会被编译成创建 React Element 的代码。

---

## 二、JSX & 渲染

### 1. JSX 中如何写条件渲染？

常见方式有三种。

### if 判断

```jsx
function App({ isLogin }) {
  if (isLogin) {
    return <div>欢迎回来</div>
  }
  return <div>请登录</div>
}
```

### 三元表达式

```jsx
{isLogin ? <div>欢迎回来</div> : <div>请登录</div>}
```

### && 短路渲染

```jsx
{isLogin && <div>欢迎回来</div>}
```

注意：

```jsx
{count && <div>有数据</div>}
```

如果 count 是 0，页面可能会渲染出 0。

更安全写法：

```jsx
{count > 0 && <div>有数据</div>}
```

---

### 2. JSX 中如何循环渲染列表？

使用数组的 `map` 方法。

```jsx
const users = [
  { id: 1, name: 'Tom' },
  { id: 2, name: 'Jack' }
]

function UserList() {
  return (
    <ul>
      {users.map(user => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  )
}
```

React 中循环渲染时，每一项都需要加 key。

---

### 3. key 的作用是什么？

key 是 React 用来识别列表中每个元素身份的标识。

作用：

1. 帮助 React 判断哪些元素新增了
2. 帮助 React 判断哪些元素删除了
3. 帮助 React 判断哪些元素移动了
4. 提高 diff 性能
5. 避免组件状态错乱

示例：

```jsx
<li key={user.id}>{user.name}</li>
```

面试回答：

> key 的作用是帮助 React 在 diff 过程中识别列表元素的唯一身份，从而更准确地复用 DOM 和组件实例，提升性能并避免状态错乱。

---

### 4. key 可以用 index 吗？

可以，但不推荐。

只有在以下情况可以使用 index：

1. 列表是静态的
2. 不会新增、删除、排序
3. 每一项没有内部状态

不推荐原因：

如果列表发生删除或排序，index 会变化，React 可能错误复用组件，导致页面状态错乱。

错误示例：

```jsx
{list.map((item, index) => (
  <input key={index} defaultValue={item.name} />
))}
```

如果删除第一项，后面的 input 可能复用错误。

推荐使用稳定唯一 id：

```jsx
{list.map(item => (
  <input key={item.id} defaultValue={item.name} />
))}
```

---

### 5. 为什么 key 不能乱用？

因为 key 影响 React 对组件的复用。

如果 key 每次都随机：

```jsx
<li key={Math.random()}>{item.name}</li>
```

React 会认为每次都是全新的元素，导致：

1. 无法复用 DOM
2. 组件频繁卸载和重新创建
3. 性能变差
4. 组件内部状态丢失

所以 key 必须稳定、唯一、可预测。

---

### 6. Fragment 是什么？

Fragment 是 React 提供的一个占位组件，用来包裹多个元素，但不会生成额外 DOM。

```jsx
function App() {
  return (
    <>
      <h1>标题</h1>
      <p>内容</p>
    </>
  )
}
```

等价于：

```jsx
<React.Fragment>
  <h1>标题</h1>
  <p>内容</p>
</React.Fragment>
```

适合避免多余的 div。

---

### 7. dangerouslySetInnerHTML 是什么？

`dangerouslySetInnerHTML` 用于在 React 中直接插入 HTML 字符串。

```jsx
function App() {
  return (
    <div dangerouslySetInnerHTML={{ __html: '<strong>Hello</strong>' }} />
  )
}
```

为什么叫 dangerously？

因为如果 HTML 字符串来自用户输入，可能造成 XSS 攻击。

例如用户输入：

```html
<img src=x onerror=alert(1)>
```

如果直接渲染，就可能执行恶意脚本。

使用时必须保证内容安全，最好先做过滤。

---

### 8. JSX 中为什么必须包一层？

React 组件的 return 只能返回一个 React Element。

错误写法：

```jsx
return (
  <h1>标题</h1>
  <p>内容</p>
)
```

正确写法：

```jsx
return (
  <>
    <h1>标题</h1>
    <p>内容</p>
  </>
)
```

原因：

JSX 最终会被编译成函数调用，一个函数不能直接返回多个并列值。

---

## 三、状态与数据流

### 1. 什么是 state？

state 是组件内部的数据状态。

当 state 变化时，React 会重新渲染组件。

```jsx
function Counter() {
  const [count, setCount] = useState(0)

  return (
    <button onClick={() => setCount(count + 1)}>
      {count}
    </button>
  )
}
```

state 适合保存会影响页面显示的数据，例如：

- 输入框内容
- 是否登录
- 弹窗是否显示
- 列表数据
- 当前选中的 tab

---

### 2. state 和 props 的区别？

| 对比项 | state | props |
| --- | --- | --- |
| 来源 | 组件内部 | 父组件传入 |
| 是否可修改 | 可通过 setState / setX 修改 | 子组件不能直接修改 |
| 作用 | 管理组件自身状态 | 父子组件传递数据 |
| 是否触发渲染 | 会 | 会 |

示例：

```jsx
function Parent() {
  const [name, setName] = useState('Tom')
  return <Child name={name} />
}

function Child(props) {
  return <div>{props.name}</div>
}
```

---

### 3. state 为什么不能直接修改？

错误写法：

```jsx
state.count = 1
```

函数组件中：

```jsx
count = count + 1
```

原因：

1. React 无法知道数据变化
2. 不会触发重新渲染
3. 可能破坏不可变数据原则
4. 影响性能优化和 diff 判断

正确写法：

```jsx
setCount(count + 1)
```

对象更新：

```jsx
setUser({
  ...user,
  name: 'Jack'
})
```

数组更新：

```jsx
setList([...list, newItem])
```

---

### 4. setState 是同步还是异步？

React 中 setState / setX 更准确地说是“调度一次状态更新”，不是立即修改变量。

在 React 18 中，大多数情况下会自动批处理。

示例：

```jsx
setCount(count + 1)
console.log(count)
```

这里打印的还是旧值。

因为当前这次函数执行时，count 的值已经固定。状态更新会在后续重新渲染时生效。

面试回答：

> setState 不是简单的同步或异步问题，它是一次状态更新调度。React 会根据场景进行批处理，更新后触发重新渲染。在当前执行上下文中通常拿到的是旧值。

---

### 5. setState 批量更新原理？

React 会把同一轮事件中的多个状态更新合并处理，减少重复渲染。

```jsx
setCount(count + 1)
setName('Tom')
setAge(20)
```

React 不会每调用一次 set 就立即渲染一次，而是合并后统一渲染。

好处：

1. 减少渲染次数
2. 提高性能
3. 保证状态更新的一致性

React 18 后，自动批处理范围更广，包括 Promise、setTimeout、原生事件等。

---

### 6. 多次 setState 会怎样？

### 直接使用旧值

```jsx
setCount(count + 1)
setCount(count + 1)
setCount(count + 1)
```

如果 count 初始为 0，最终结果通常是 1。

因为三次使用的都是同一个旧 count。

### 使用函数形式

```jsx
setCount(prev => prev + 1)
setCount(prev => prev + 1)
setCount(prev => prev + 1)
```

最终结果是 3。

因为每次都会基于上一次更新后的值继续计算。

---

### 7. 为什么 setState 要用函数形式？

当新状态依赖旧状态时，应该使用函数形式。

```jsx
setCount(prev => prev + 1)
```

原因：

1. 避免闭包中的旧值问题
2. 多次更新时结果更准确
3. 在批量更新中更安全

适合场景：

- 计数器 +1
- 数组追加
- 根据旧对象修改字段

```jsx
setList(prev => [...prev, newItem])
```

---

### 8. props 是只读的吗？

是的。

子组件不能直接修改 props。

错误写法：

```jsx
function Child(props) {
  props.name = 'Jack'
}
```

React 数据流是单向的，props 只能由父组件传入，子组件只能读取。

如果子组件想修改父组件数据，需要父组件传递修改函数。

---

### 9. 如何子组件修改父组件状态？

父组件把修改 state 的函数传给子组件，子组件调用这个函数。

```jsx
function Parent() {
  const [name, setName] = useState('Tom')

  return <Child changeName={setName} />
}

function Child({ changeName }) {
  return (
    <button onClick={() => changeName('Jack')}>
      修改父组件状态
    </button>
  )
}
```

本质：

> 子组件不能直接修改父组件 state，只能通过调用父组件传下来的函数，让父组件自己修改。

---

### 10. 什么是受控组件？

受控组件指表单元素的值由 React state 控制。

```jsx
function App() {
  const [value, setValue] = useState('')

  return (
    <input
      value={value}
      onChange={e => setValue(e.target.value)}
    />
  )
}
```

特点：

1. 表单值存储在 state 中
2. React 控制输入框显示
3. 方便校验、联动、提交

---

### 11. 什么是非受控组件？

非受控组件指表单值由 DOM 自己管理，React 不直接控制。

通常通过 ref 获取值。

```jsx
function App() {
  const inputRef = useRef(null)

  const handleSubmit = () => {
    console.log(inputRef.current.value)
  }

  return (
    <>
      <input ref={inputRef} />
      <button onClick={handleSubmit}>提交</button>
    </>
  )
}
```

特点：

1. 写法简单
2. React 不实时管理输入值
3. 适合简单表单或文件上传

---

### 12. 表单受控与非受控的区别？

| 对比项 | 受控组件 | 非受控组件 |
| --- | --- | --- |
| 数据来源 | React state | DOM |
| 获取方式 | state | ref |
| 是否实时控制 | 是 | 否 |
| 校验联动 | 方便 | 不方便 |
| 代码量 | 较多 | 较少 |

推荐：

- 复杂表单用受控组件
- 简单表单或文件上传可用非受控组件

---

## 四、Hooks

### 1. 什么是 Hooks？

Hooks 是 React 16.8 引入的特性，允许函数组件使用 state、生命周期、副作用等能力。

常见 Hooks：

- useState
- useEffect
- useMemo
- useCallback
- useRef
- useContext
- useReducer

示例：

```jsx
const [count, setCount] = useState(0)
```

Hooks 出现之前，函数组件没有状态，只能做展示组件。

---

### 2. 为什么要有 Hooks？

Hooks 解决了类组件的一些问题：

1. this 指向复杂
2. 生命周期中逻辑容易分散
3. 逻辑复用困难
4. 类组件代码较重
5. 组件嵌套容易复杂

Hooks 的优势：

1. 函数组件也能有状态
2. 逻辑可以通过自定义 Hook 复用
3. 代码更简洁
4. 更容易组合逻辑

---

### 3. useState 原理？

useState 用于在函数组件中声明状态。

```jsx
const [count, setCount] = useState(0)
```

React 内部会为每个组件维护一个 Hooks 链表或队列。

每次渲染时，React 按照 Hooks 调用顺序找到对应状态。

这也是为什么 Hook 不能写在条件语句中。

简单理解：

```js
hooks[0] = count
hooks[1] = name
hooks[2] = effect
```

如果调用顺序变了，React 就找错状态了。

---

### 4. useEffect 的执行时机？

useEffect 用于处理副作用。

副作用包括：

- 请求数据
- 设置定时器
- 订阅事件
- 手动操作 DOM
- 修改 document.title

执行时机：

```jsx
useEffect(() => {
  console.log('组件渲染后执行')
})
```

默认每次渲染后都会执行。

```jsx
useEffect(() => {
  console.log('只在挂载后执行')
}, [])
```

依赖数组为空，只在组件挂载后执行一次。

```jsx
useEffect(() => {
  console.log('count 变化后执行')
}, [count])
```

依赖变化时执行。

---

### 5. useEffect 的依赖数组作用？

依赖数组用于控制 effect 什么时候重新执行。

### 不传依赖数组

```jsx
useEffect(() => {
  console.log('每次渲染后执行')
})
```

### 空数组

```jsx
useEffect(() => {
  console.log('只执行一次')
}, [])
```

### 有依赖

```jsx
useEffect(() => {
  console.log('count 变化时执行')
}, [count])
```

如果 effect 中用到了某个外部变量，通常应该放进依赖数组中，避免闭包旧值问题。

---

### 6. useEffect 和 componentDidMount 对比？

类组件：

```jsx
componentDidMount() {
  // 组件挂载后执行
}
```

Hooks：

```jsx
useEffect(() => {
  // 组件挂载后执行
}, [])
```

相似点：

- 都可以在组件挂载后执行请求、订阅等逻辑。

区别：

- useEffect 更灵活，可以根据依赖变化执行。
- useEffect 还可以返回清理函数，模拟卸载逻辑。

```jsx
useEffect(() => {
  const timer = setInterval(() => {}, 1000)

  return () => {
    clearInterval(timer)
  }
}, [])
```

---

### 7. useLayoutEffect vs useEffect？

二者都用于处理副作用，但执行时机不同。

### useEffect

- 浏览器完成绘制之后执行
- 不会阻塞页面渲染
- 大多数场景推荐使用

### useLayoutEffect

- DOM 更新之后、浏览器绘制之前执行
- 会阻塞页面绘制
- 适合读取布局、同步修改 DOM

示例：

```jsx
useLayoutEffect(() => {
  const width = ref.current.offsetWidth
}, [])
```

面试回答：

> useEffect 在浏览器绘制后异步执行，useLayoutEffect 在 DOM 更新后、浏览器绘制前同步执行。一般优先用 useEffect，只有需要读取布局并同步修改 DOM 时才使用 useLayoutEffect。

---

### 8. useMemo 的作用？

useMemo 用于缓存计算结果。

```jsx
const total = useMemo(() => {
  return list.reduce((sum, item) => sum + item.price, 0)
}, [list])
```

只有 list 变化时才重新计算 total。

适合场景：

1. 复杂计算
2. 避免每次渲染都重新计算
3. 配合 React.memo 避免引用变化

不适合滥用：

简单计算不需要 useMemo，否则反而增加复杂度。

---

### 9. useCallback 的作用？

useCallback 用于缓存函数引用。

```jsx
const handleClick = useCallback(() => {
  console.log(count)
}, [count])
```

它等价于：

```jsx
const handleClick = useMemo(() => {
  return () => console.log(count)
}, [count])
```

适合场景：

1. 函数传给子组件
2. 子组件使用 React.memo
3. 避免子组件因为函数引用变化而重新渲染

---

### 10. useRef 的作用？

useRef 可以保存一个可变值，修改它不会触发重新渲染。

常见用途：

### 获取 DOM

```jsx
const inputRef = useRef(null)

<input ref={inputRef} />
```

### 保存定时器 ID

```jsx
const timerRef = useRef(null)

timerRef.current = setInterval(() => {}, 1000)
```

### 保存上一次的值

```jsx
const prevCount = useRef(count)

useEffect(() => {
  prevCount.current = count
}, [count])
```

特点：

- `.current` 可以被修改
- 修改不会触发渲染
- 在组件整个生命周期中保持同一个引用

---

### 11. useImperativeHandle 是什么？

useImperativeHandle 用于配合 forwardRef，自定义父组件通过 ref 能访问到的子组件方法。

```jsx
const Child = forwardRef((props, ref) => {
  const inputRef = useRef(null)

  useImperativeHandle(ref, () => ({
    focus() {
      inputRef.current.focus()
    }
  }))

  return <input ref={inputRef} />
})
```

父组件：

```jsx
function Parent() {
  const childRef = useRef(null)

  return (
    <>
      <Child ref={childRef} />
      <button onClick={() => childRef.current.focus()}>
        聚焦
      </button>
    </>
  )
}
```

适合场景：

- 暴露 focus
- 暴露 reset
- 暴露表单校验方法
- 暴露弹窗打开关闭方法

注意：不要滥用，否则会破坏组件封装。

---

### 12. 自定义 Hook 如何写？

自定义 Hook 是以 `use` 开头的函数，用于复用组件逻辑。

示例：封装窗口宽度。

```jsx
function useWindowWidth() {
  const [width, setWidth] = useState(window.innerWidth)

  useEffect(() => {
    const handleResize = () => setWidth(window.innerWidth)

    window.addEventListener('resize', handleResize)

    return () => {
      window.removeEventListener('resize', handleResize)
    }
  }, [])

  return width
}
```

使用：

```jsx
function App() {
  const width = useWindowWidth()

  return <div>{width}</div>
}
```

自定义 Hook 的本质：

> 把组件中可复用的状态逻辑抽出来。

---

### 13. Hook 使用规则有哪些？

Hooks 有两个重要规则：

1. 只能在函数组件或自定义 Hook 中使用
2. 不能在条件语句、循环、嵌套函数中使用

错误写法：

```jsx
if (show) {
  useEffect(() => {}, [])
}
```

正确写法：

```jsx
useEffect(() => {
  if (show) {
    // 逻辑写在 Hook 内部
  }
}, [show])
```

---

### 14. 为什么不能在条件语句中使用 Hook？

因为 React 依赖 Hook 的调用顺序来保存状态。

错误示例：

```jsx
if (flag) {
  const [name, setName] = useState('Tom')
}

const [age, setAge] = useState(18)
```

如果 flag 一会儿 true，一会儿 false，Hook 的调用顺序就变了。

React 会把状态对应错，导致 bug。

面试回答：

> React 内部根据 Hook 的调用顺序来关联每个状态。如果 Hook 写在条件语句中，某次渲染可能调用，某次不调用，顺序就会改变，React 无法正确找到对应状态。

---

## 五、生命周期（类组件 & Hooks）

### 1. 类组件生命周期有哪些？

类组件生命周期分为三个阶段：

### 挂载阶段

- constructor
- static getDerivedStateFromProps
- render
- componentDidMount

### 更新阶段

- static getDerivedStateFromProps
- shouldComponentUpdate
- render
- getSnapshotBeforeUpdate
- componentDidUpdate

### 卸载阶段

- componentWillUnmount

---

### 2. componentDidMount / componentDidUpdate / componentWillUnmount 用途？

### componentDidMount

组件挂载后执行。

常用于：

- 请求数据
- 订阅事件
- 设置定时器

### componentDidUpdate

组件更新后执行。

常用于：

- 根据 props / state 变化执行逻辑
- 操作更新后的 DOM

### componentWillUnmount

组件卸载前执行。

常用于：

- 清除定时器
- 取消请求
- 移除事件监听
- 取消订阅

---

### 3. getDerivedStateFromProps 是什么？

`getDerivedStateFromProps` 是一个静态生命周期方法，用于根据 props 派生 state。

```jsx
static getDerivedStateFromProps(nextProps, prevState) {
  if (nextProps.value !== prevState.value) {
    return {
      value: nextProps.value
    }
  }
  return null
}
```

注意：

- 它是静态方法，不能使用 this。
- 使用场景较少。
- 滥用容易导致 props 和 state 数据重复，增加复杂度。

---

### 4. shouldComponentUpdate 作用？

`shouldComponentUpdate` 用于控制组件是否需要重新渲染。

```jsx
shouldComponentUpdate(nextProps, nextState) {
  return nextProps.count !== this.props.count
}
```

返回：

- true：允许更新
- false：阻止更新

作用：

1. 减少不必要渲染
2. 优化性能

函数组件中类似能力：

```jsx
React.memo(Component)
```

---

### 5. React 16 之后废弃了哪些生命周期？

React 16 之后，一些旧生命周期被标记为不安全：

- componentWillMount
- componentWillReceiveProps
- componentWillUpdate

原因：

React Fiber 支持异步渲染和可中断渲染，这些生命周期可能被多次调用或产生副作用问题。

替代方案：

- componentDidMount
- componentDidUpdate
- getDerivedStateFromProps
- getSnapshotBeforeUpdate
- useEffect

---

### 6. Hooks 如何模拟生命周期？

### componentDidMount

```jsx
useEffect(() => {
  console.log('挂载')
}, [])
```

### componentDidUpdate

```jsx
useEffect(() => {
  console.log('count 更新')
}, [count])
```

### componentWillUnmount

```jsx
useEffect(() => {
  return () => {
    console.log('卸载')
  }
}, [])
```

### 挂载 + 更新

```jsx
useEffect(() => {
  console.log('每次渲染后执行')
})
```

注意：

Hooks 不是完全模拟生命周期，而是按“副作用”和“依赖”来组织逻辑。

---

## 六、组件通信

### 1. 父子组件通信方式？

父传子：通过 props。

```jsx
function Parent() {
  return <Child name="Tom" />
}

function Child({ name }) {
  return <div>{name}</div>
}
```

子传父：父组件传函数给子组件。

```jsx
function Parent() {
  const [msg, setMsg] = useState('')

  return <Child onChange={setMsg} />
}

function Child({ onChange }) {
  return <button onClick={() => onChange('hello')}>发送</button>
}
```

---

### 2. 兄弟组件如何通信？

兄弟组件通常通过共同父组件通信。

```jsx
function Parent() {
  const [value, setValue] = useState('')

  return (
    <>
      <A setValue={setValue} />
      <B value={value} />
    </>
  )
}
```

A 修改父组件状态，B 接收父组件状态。

也可以使用：

- Context
- Redux
- Zustand
- EventEmitter
- URL 参数
- localStorage

---

### 3. 跨层级组件通信方式？

常见方式：

1. props 层层传递
2. Context
3. Redux / Zustand
4. 自定义事件
5. ref 暴露方法

如果只是主题、语言、用户信息，可以用 Context。

如果是复杂业务状态，可以用 Redux / Zustand。

---

### 4. Context 的作用？

Context 用于跨层级传递数据，避免 props drilling。

创建：

```jsx
const UserContext = React.createContext(null)
```

提供数据：

```jsx
<UserContext.Provider value={user}>
  <App />
</UserContext.Provider>
```

消费数据：

```jsx
const user = useContext(UserContext)
```

适合：

- 用户信息
- 主题
- 语言
- 权限
- 全局配置

---

### 5. Context 会导致性能问题吗？

会。

当 Provider 的 value 变化时，所有使用这个 Context 的子组件都会重新渲染。

```jsx
<UserContext.Provider value={{ user, setUser }}>
```

如果每次渲染都创建新对象，消费者组件可能频繁更新。

---

### 6. 如何避免 Context 频繁渲染？

常见方法：

1. 拆分 Context

```jsx
UserContext
ThemeContext
PermissionContext
```

1. 使用 useMemo 缓存 value

```jsx
const value = useMemo(() => ({ user, setUser }), [user])
```

1. 把不常变和常变的数据分开
2. 使用状态管理库
3. 只在必要组件中使用 useContext

---

### 7. props drilling 如何解决？

props drilling 指数据需要通过多层组件传递，但中间组件并不使用这个数据。

解决方式：

1. Context
2. 组件组合
3. 状态提升
4. Redux / Zustand
5. 自定义 Hook

示例：

```jsx
<UserContext.Provider value={user}>
  <DeepChild />
</UserContext.Provider>
```

---

### 8. forwardRef 是什么？

forwardRef 用于让父组件把 ref 传递给子组件内部的 DOM 或组件实例。

```jsx
const MyInput = forwardRef((props, ref) => {
  return <input ref={ref} />
})
```

父组件：

```jsx
const inputRef = useRef(null)

<MyInput ref={inputRef} />
```

适合封装基础组件时使用，例如 Input、Modal、Form。

---

### 9. memo 是什么？

`React.memo` 是一个高阶组件，用于缓存函数组件的渲染结果。

```jsx
const Child = React.memo(function Child({ name }) {
  console.log('Child render')
  return <div>{name}</div>
})
```

当 props 没有变化时，React 可以跳过子组件重新渲染。

注意：

- React.memo 只做 props 浅比较。
- 如果 props 是对象或函数，每次创建新引用，memo 可能失效。

---

### 10. memo 和 useMemo 的区别？

| 对比项 | React.memo | useMemo |
| --- | --- | --- |
| 类型 | 高阶组件 | Hook |
| 作用 | 缓存组件渲染结果 | 缓存计算结果 |
| 使用对象 | 组件 | 值 |
| 场景 | 避免子组件重渲染 | 避免复杂计算重复执行 |

示例：

```jsx
const Child = React.memo(ChildComponent)

const total = useMemo(() => calc(list), [list])
```

---

## 七、事件机制 & 合成事件

### 1. React 事件和原生事件区别？

React 事件不是直接绑定到真实 DOM 上的原生事件，而是 React 自己封装的一套事件系统。

区别：

| 对比项 | React 事件 | 原生事件 |
| --- | --- | --- |
| 命名 | onClick | onclick |
| 兼容性 | React 统一处理 | 浏览器自己处理 |
| 事件对象 | SyntheticEvent | Event |
| 绑定方式 | JSX 属性 | addEventListener |
| 默认 this | 需要注意 | 根据调用方式 |

示例：

```jsx
<button onClick={handleClick}>点击</button>
```

---

### 2. 什么是合成事件？

合成事件是 React 对浏览器原生事件的封装。

```jsx
function handleClick(e) {
  console.log(e)
}
```

这里的 e 是 SyntheticEvent，不是原生 Event。

React 封装合成事件的目的：

1. 统一不同浏览器的行为
2. 提供一致 API
3. 优化事件绑定
4. 更好配合 React 更新机制

如果需要原生事件对象：

```jsx
e.nativeEvent
```

---

### 3. 事件冒泡和捕获？

事件传播有三个阶段：

1. 捕获阶段：从外层到目标元素
2. 目标阶段：到达触发元素
3. 冒泡阶段：从目标元素向外层传播

React 默认使用冒泡事件：

```jsx
<div onClick={parentClick}>
  <button onClick={childClick}>点击</button>
</div>
```

点击 button 时，先触发 childClick，再触发 parentClick。

捕获事件：

```jsx
<div onClickCapture={handleCapture}>
```

---

### 4. 为什么 React 要自己实现事件系统？

原因：

1. 统一浏览器兼容性
2. 减少事件监听数量
3. 方便做事件委托
4. 更好控制事件优先级
5. 方便和 React 更新调度结合

React 不给每个 DOM 都单独绑定事件，而是通过事件委托统一处理。

---

### 5. 事件绑定在 document 上的意义？

React 17 之前，事件主要委托到 document 上。

React 17 之后，事件委托到 React 根容器上。

事件委托的意义：

1. 减少事件监听器数量
2. 提高性能
3. 动态添加元素也能响应事件
4. 统一管理事件传播

面试注意：

> 旧版本 React 事件绑定在 document 上，React 17 以后改为绑定在 root 容器上，更方便多个 React 版本共存。

---

### 6. 如何阻止事件冒泡？

使用：

```jsx
e.stopPropagation()
```

示例：

```jsx
function ChildClick(e) {
  e.stopPropagation()
  console.log('child')
}
```

阻止默认行为：

```jsx
e.preventDefault()
```

例如阻止表单提交刷新页面：

```jsx
<form onSubmit={e => e.preventDefault()}>
```

---

### 7. 合成事件和原生事件混用会怎样？

合成事件和原生事件混用时，要注意事件触发顺序和绑定位置。

可能出现：

1. stopPropagation 不能完全阻止另一套事件系统
2. 原生事件和 React 事件执行顺序不同
3. document 上的原生事件可能仍然触发

建议：

- 尽量不要混用
- 必须混用时，明确事件绑定位置
- 使用 nativeEvent 时谨慎处理

---

## 八、性能优化

### 1. React 性能瓶颈通常在哪？

常见瓶颈：

1. 组件频繁重新渲染
2. 大列表渲染
3. 复杂计算重复执行
4. 图片资源过大
5. 首屏资源体积过大
6. Context 更新导致大面积渲染
7. 不合理的 key
8. 不必要的全局状态更新

性能优化本质：

> 减少不必要的渲染，减少不必要的计算，减少首屏资源体积。

---

### 2. 如何减少组件重渲染？

常见方法：

1. React.memo
2. useMemo
3. useCallback
4. 合理拆分组件
5. state 下放
6. 避免父组件频繁创建新对象和新函数
7. Context 拆分
8. 使用虚拟列表

示例：

```jsx
const Child = React.memo(function Child({ name }) {
  return <div>{name}</div>
})
```

---

### 3. React.memo 的使用场景？

适合：

1. 子组件渲染成本较高
2. 子组件 props 不经常变化
3. 父组件频繁更新
4. 子组件是纯展示组件

不适合：

1. 组件很简单
2. props 每次都变化
3. 过度使用导致代码复杂

---

### 4. useCallback 的正确使用方式？

useCallback 不是为了让函数执行更快，而是为了保持函数引用稳定。

典型搭配：

```jsx
const Child = React.memo(({ onClick }) => {
  return <button onClick={onClick}>点击</button>
})

function Parent() {
  const [count, setCount] = useState(0)

  const handleClick = useCallback(() => {
    console.log('click')
  }, [])

  return <Child onClick={handleClick} />
}
```

如果子组件没有 memo，useCallback 可能意义不大。

---

### 5. useMemo 的使用场景？

适合：

1. 复杂计算
2. 过滤、排序大数组
3. 生成稳定对象
4. 配合 memo 避免子组件重渲染

示例：

```jsx
const filteredList = useMemo(() => {
  return list.filter(item => item.active)
}, [list])
```

---

### 6. key 对性能的影响？

合理 key 可以帮助 React 准确复用 DOM。

不合理 key 会导致：

1. DOM 频繁销毁重建
2. 组件状态丢失
3. diff 判断错误
4. 性能下降

推荐：

```jsx
key={item.id}
```

避免：

```jsx
key={Math.random()}
key={Date.now()}
```

---

### 7. 虚拟列表如何实现？

虚拟列表只渲染当前可视区域附近的数据，而不是一次性渲染所有列表项。

适合：

- 几千条数据
- 几万条数据
- 表格
- 长列表

核心思想：

1. 计算容器高度
2. 根据滚动距离计算可见数据范围
3. 只渲染可见区域
4. 用占位高度撑开滚动条

常用库：

- react-window
- react-virtualized

---

### 8. 懒加载组件怎么做？

使用动态 import。

```jsx
const UserPage = React.lazy(() => import('./UserPage'))
```

然后配合 Suspense：

```jsx
<Suspense fallback={<div>加载中...</div>}>
  <UserPage />
</Suspense>
```

好处：

1. 减少首屏包体积
2. 按需加载页面
3. 提升首屏速度

---

### 9. React.lazy 和 Suspense？

`React.lazy` 用于懒加载组件。

`Suspense` 用于在组件加载期间显示 fallback。

```jsx
const About = React.lazy(() => import('./About'))

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <About />
    </Suspense>
  )
}
```

常用于路由懒加载。

---

### 10. 如何避免重复渲染？

方法：

1. 状态放在最小必要范围
2. 合理拆分组件
3. 使用 React.memo
4. 使用 useMemo / useCallback
5. 避免不必要的 Context 更新
6. 避免父组件每次传新对象
7. key 保持稳定
8. 使用状态管理库的 selector

---

### 11. 如何做首屏优化？

常见方案：

1. 路由懒加载
2. Code Splitting
3. Tree Shaking
4. 图片压缩
5. CDN 加速
6. 开启 gzip / brotli
7. 减少首屏请求
8. SSR / SSG
9. 预加载关键资源
10. 骨架屏
11. 缓存静态资源

---

## 九、React Router

### 1. BrowserRouter vs HashRouter？

### BrowserRouter

使用 HTML5 history API。

URL 示例：

```text
https://example.com/user
```

优点：

- URL 更美观
- 更接近真实路径

缺点：

- 需要服务端配置 fallback
- 刷新页面可能 404

### HashRouter

使用 URL hash。

URL 示例：

```text
https://example.com/#/user
```

优点：

- 不需要服务端特殊配置
- 刷新不会 404

缺点：

- URL 不够美观
- SEO 不友好

---

### 2. Route / Routes 的区别？

React Router v6 中使用 Routes 包裹 Route。

```jsx
<Routes>
  <Route path="/" element={<Home />} />
  <Route path="/user" element={<User />} />
</Routes>
```

v5 中是 Switch：

```jsx
<Switch>
  <Route path="/" component={Home} />
</Switch>
```

v6 的变化：

1. 使用 element
2. 使用 Routes 替代 Switch
3. 路由匹配规则更简洁
4. 嵌套路由更清晰

---

### 3. useParams / useSearchParams？

### useParams

用于获取动态路由参数。

```jsx
<Route path="/user/:id" element={<User />} />
```

```jsx
const { id } = useParams()
```

访问：

```text
/user/100
```

id 就是 100。

### useSearchParams

用于获取查询参数。

```text
/user?id=100&name=tom
```

```jsx
const [searchParams] = useSearchParams()

const id = searchParams.get('id')
```

---

### 4. useNavigate 如何使用？

useNavigate 用于编程式跳转。

```jsx
const navigate = useNavigate()

navigate('/login')
```

带参数：

```jsx
navigate('/user/100')
```

返回上一页：

```jsx
navigate(-1)
```

替换当前历史记录：

```jsx
navigate('/login', { replace: true })
```

---

### 5. 路由守卫如何实现？

React Router 没有 Vue Router 那种内置全局守卫，通常通过封装组件实现。

```jsx
function AuthRoute({ children }) {
  const token = localStorage.getItem('token')

  if (!token) {
    return <Navigate to="/login" replace />
  }

  return children
}
```

使用：

```jsx
<Route
  path="/dashboard"
  element={
    <AuthRoute>
      <Dashboard />
    </AuthRoute>
  }
/>
```

---

### 6. 嵌套路由如何写？

父路由：

```jsx
<Route path="/user" element={<UserLayout />}>
  <Route path="profile" element={<Profile />} />
  <Route path="setting" element={<Setting />} />
</Route>
```

父组件中使用 Outlet：

```jsx
function UserLayout() {
  return (
    <div>
      <h2>用户中心</h2>
      <Outlet />
    </div>
  )
}
```

---

### 7. 动态路由原理？

动态路由是通过路径占位符匹配不同参数。

```jsx
<Route path="/user/:id" element={<User />} />
```

当访问：

```text
/user/1
/user/2
```

都会匹配同一个组件，但参数不同。

组件内部通过 useParams 获取参数。

---

### 8. 路由懒加载如何做？

```jsx
const Home = React.lazy(() => import('./pages/Home'))
const User = React.lazy(() => import('./pages/User'))

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/user" element={<User />} />
      </Routes>
    </Suspense>
  )
}
```

---

## 十、状态管理（Redux / Zustand / Recoil）

### 1. Redux 核心思想？

Redux 是一个可预测的状态管理库。

核心思想：

1. 全局状态集中管理
2. 状态只读
3. 通过 action 描述修改意图
4. reducer 根据 action 返回新状态
5. 单向数据流

---

### 2. Redux 三大原则？

1. **单一数据源**
   - 整个应用状态存储在一个 store 中。

2. **State 是只读的**
   - 不能直接修改 state。
   - 必须通过 dispatch action 修改。

3. **使用纯函数修改状态**
   - reducer 是纯函数。
   - 输入旧 state 和 action，返回新 state。

---

### 3. Redux 的数据流？

流程：

1. 组件触发事件
2. dispatch 一个 action
3. reducer 接收 action
4. reducer 返回新 state
5. store 更新
6. 组件重新渲染

```text
View -> dispatch(action) -> reducer -> store -> View
```

---

### 4. Redux 中间件是什么？

Redux 中间件用于增强 dispatch。

默认 Redux 只能 dispatch 普通对象。

中间件可以处理：

- 异步请求
- 日志
- 错误上报
- 权限校验
- 埋点

常见中间件：

- redux-thunk
- redux-saga
- redux-logger

---

### 5. Redux-thunk 和 redux-saga 区别？

### redux-thunk

允许 dispatch 一个函数。

```js
dispatch(async (dispatch) => {
  const res = await api.getUser()
  dispatch({ type: 'setUser', payload: res })
})
```

特点：

- 简单
- 适合中小项目
- 学习成本低

### redux-saga

使用 Generator 管理异步流程。

特点：

- 更适合复杂异步
- 可以处理取消、竞态、监听
- 学习成本较高

对比：

| 对比项 | thunk | saga |
| --- | --- | --- |
| 写法 | 函数 | Generator |
| 难度 | 简单 | 较高 |
| 适合 | 简单异步 | 复杂异步流程 |
| 可测试性 | 一般 | 较好 |

---

### 6. Redux Toolkit 优势？

Redux Toolkit 是官方推荐的 Redux 写法。

优势：

1. 简化 Redux 配置
2. 内置 Immer，可以写类似可变代码
3. 内置 thunk
4. 减少样板代码
5. createSlice 自动生成 action 和 reducer

示例：

```js
const counterSlice = createSlice({
  name: 'counter',
  initialState: { value: 0 },
  reducers: {
    increment(state) {
      state.value += 1
    }
  }
})
```

虽然看起来直接修改 state，但底层通过 Immer 生成新对象。

---

### 7. Zustand 与 Redux 对比？

Zustand 是一个轻量级状态管理库。

特点：

1. API 简单
2. 不需要 Provider
3. 样板代码少
4. 使用 Hook 访问状态
5. 适合中小型项目

示例：

```js
const useStore = create(set => ({
  count: 0,
  add: () => set(state => ({ count: state.count + 1 }))
}))
```

对比：

| 对比项 | Redux | Zustand |
| --- | --- | --- |
| 复杂度 | 较高 | 较低 |
| 样板代码 | 多 | 少 |
| 生态 | 成熟 | 轻量 |
| 适合 | 大型复杂项目 | 中小项目 |

---

### 8. Recoil 是什么？

Recoil 是 Facebook 推出的 React 状态管理库。

核心概念：

- atom：最小状态单元
- selector：派生状态

示例：

```js
const countState = atom({
  key: 'countState',
  default: 0
})
```

组件中使用：

```jsx
const [count, setCount] = useRecoilState(countState)
```

特点：

1. 状态粒度更细
2. 更贴近 React
3. 支持派生状态
4. 适合复杂状态依赖

---

### 9. 状态提升 vs 全局状态？

### 状态提升

把多个组件共享的状态提升到最近的共同父组件。

适合：

- 共享范围小
- 只在局部页面使用

### 全局状态

把状态放到 Redux / Zustand / Context 等全局管理。

适合：

- 用户信息
- 登录状态
- 权限
- 主题
- 多页面共享数据

原则：

> 能局部就不要全局，避免全局状态过多导致维护困难。

---

## 十一、工程化

### 1. Webpack 与 Vite 区别？

### Webpack

- 老牌打包工具
- 功能强大
- 配置灵活
- 开发启动较慢

### Vite

- 基于原生 ESM
- 开发环境启动快
- 生产构建使用 Rollup
- 配置更简单

对比：

| 对比项 | Webpack | Vite |
| --- | --- | --- |
| 开发启动 | 较慢 | 很快 |
| 配置复杂度 | 较高 | 较低 |
| 生态 | 非常成熟 | 越来越成熟 |
| 生产构建 | Webpack | Rollup |

---

### 2. Tree Shaking 原理？

Tree Shaking 是打包时删除未使用代码的优化技术。

依赖 ESM 的静态结构：

```js
import { add } from './utils'
```

因为 import / export 是静态的，打包工具可以分析哪些代码没有被使用，然后删除。

注意：

- CommonJS 不利于 Tree Shaking
- 副作用代码可能影响 Tree Shaking

---

### 3. Code Splitting 是什么？

Code Splitting 是代码分割。

目的：

- 把一个大 bundle 拆成多个小 chunk
- 按需加载
- 减少首屏加载体积

常见方式：

```js
import('./UserPage')
```

React 中：

```jsx
const UserPage = React.lazy(() => import('./UserPage'))
```

---

### 4. chunk 是什么？

chunk 是打包后的代码块。

一个项目经过打包后，可能生成：

- main chunk
- vendor chunk
- async chunk
- runtime chunk

例如：

```text
main.js
vendor.js
user-page.js
```

chunk 的作用是配合缓存和按需加载。

---

### 5. ESM 和 CJS 区别？

### ESM

```js
import React from 'react'
export default App
```

特点：

- 静态导入
- 支持 Tree Shaking
- 浏览器原生支持
- 编译时确定依赖

### CJS

```js
const React = require('react')
module.exports = App
```

特点：

- 运行时加载
- Node.js 传统模块规范
- 不利于 Tree Shaking

---

### 6. Babel 的作用？

Babel 是 JavaScript 编译器。

主要作用：

1. 把新语法转换成旧语法
2. 编译 JSX
3. 支持 TypeScript 转换
4. 配合 polyfill 处理兼容性

示例：

```jsx
const element = <div>Hello</div>
```

会被 Babel 编译成 JavaScript 代码。

---

### 7. Polyfill 是什么？

Polyfill 是用于补充旧浏览器缺失 API 的代码。

例如旧浏览器没有 Promise，可以通过 polyfill 补上。

```js
import 'core-js/stable'
```

Babel 负责转换语法，Polyfill 负责补 API。

区别：

| 工具 | 作用 |
| --- | --- |
| Babel | 转换语法 |
| Polyfill | 补充 API |

---

### 8. 浏览器兼容怎么处理？

常见方式：

1. Babel 转换新语法
2. core-js 提供 polyfill
3. PostCSS / Autoprefixer 添加 CSS 前缀
4. browserslist 配置目标浏览器
5. 使用兼容性好的 API
6. 做真机和多浏览器测试

---

### 9. 如何配置多环境？

常见环境：

- development
- test
- staging
- production

Vite 中：

```text
.env.development
.env.production
```

变量命名：

```env
VITE_API_BASE_URL=https://api.example.com
```

代码中使用：

```js
import.meta.env.VITE_API_BASE_URL
```

Webpack 中通常通过 DefinePlugin 或 dotenv 实现。

---

### 10. 如何做前端监控？

前端监控包括：

1. JS 错误监控
2. Promise 错误监控
3. 资源加载错误
4. 接口错误
5. 性能指标
6. 用户行为日志
7. 白屏监控

常见 API：

```js
window.onerror = function(message, source, lineno, colno, error) {}

window.addEventListener('unhandledrejection', event => {})
```

性能监控：

- FP
- FCP
- LCP
- CLS
- INP
- TTFB

常用平台：

- Sentry
- Fundebug
- 阿里 ARMS
- 自建埋点系统

---

## 十二、React 18 新特性

### 1. 并发模式是什么？

React 18 引入并发渲染能力。

并发不是多线程，而是 React 可以把渲染任务拆分、暂停、恢复、放弃。

目标：

1. 保持页面响应
2. 优先处理用户交互
3. 延后低优先级更新
4. 避免长时间阻塞主线程

---

### 2. useTransition 用法？

useTransition 用于把某些更新标记为低优先级。

```jsx
const [isPending, startTransition] = useTransition()

function handleChange(e) {
  const value = e.target.value

  setInput(value)

  startTransition(() => {
    setList(filterList(value))
  })
}
```

input 更新是高优先级，列表过滤是低优先级。

适合：

- 搜索过滤
- 大列表更新
- 页面切换

---

### 3. useDeferredValue 作用？

useDeferredValue 用于延迟某个值的更新。

```jsx
const deferredValue = useDeferredValue(input)
```

适合：

- 输入框实时输入
- 大列表根据输入过滤
- 避免输入卡顿

区别：

- useTransition 控制一段更新逻辑
- useDeferredValue 延迟某个值

---

### 4. startTransition 是什么？

startTransition 用于把状态更新标记为非紧急更新。

```jsx
startTransition(() => {
  setPage(nextPage)
})
```

紧急更新：

- 输入框内容
- 点击反馈
- 动画响应

非紧急更新：

- 搜索结果
- 大列表
- 页面内容切换

---

### 5. 自动批处理（Automatic Batching）？

React 18 中，更多场景下的 setState 会自动合并。

以前只有 React 事件中会批处理。

React 18 后：

```jsx
setTimeout(() => {
  setCount(c => c + 1)
  setFlag(f => !f)
}, 1000)
```

也会自动批处理成一次渲染。

好处：

1. 减少渲染次数
2. 提升性能
3. 行为更一致

---

### 6. React 18 对性能的提升？

主要来自：

1. 自动批处理
2. 并发渲染
3. startTransition
4. useDeferredValue
5. Suspense 能力增强
6. 更好的调度机制

注意：

React 18 不代表所有代码都会自动变快，还是要合理拆分组件和优化渲染。

---

### 7. Suspense 的新能力？

Suspense 最早主要用于组件懒加载。

```jsx
<Suspense fallback={<Loading />}>
  <LazyComponent />
</Suspense>
```

React 18 中 Suspense 和并发渲染、SSR 结合更紧密，可以支持更细粒度的加载状态和流式渲染。

在普通前端项目中，最常见的用法仍然是路由懒加载。

---

## 十三、源码 & 原理

### 1. Fiber 架构是什么？

Fiber 是 React 16 引入的新协调架构。

它把渲染任务拆分成一个个小单元，每个组件对应一个 Fiber 节点。

Fiber 节点包含：

- 组件类型
- props
- state
- child
- sibling
- return
- effect 信息

简单理解：

> Fiber 是 React 内部用来描述组件树和调度任务的数据结构。

---

### 2. Fiber 解决了什么问题？

老版本 React 递归更新组件树，一旦开始渲染就不能中断。

如果组件树很大，可能长时间阻塞主线程，导致页面卡顿。

Fiber 解决：

1. 渲染任务可拆分
2. 渲染过程可中断
3. 可以恢复任务
4. 可以丢弃过期任务
5. 可以根据优先级调度

---

### 3. React 的调度机制？

React 会给不同更新分配不同优先级。

例如：

高优先级：

- 点击
- 输入
- 动画

低优先级：

- 列表过滤
- 页面内容更新
- 后台数据刷新

React 调度器会优先执行高优先级任务，低优先级任务可以被延后或中断。

---

### 4. requestIdleCallback 的作用？

`requestIdleCallback` 是浏览器提供的 API，用于在浏览器空闲时执行任务。

```js
requestIdleCallback(() => {
  // 空闲时执行
})
```

React Fiber 的思想和它类似：利用空闲时间处理低优先级任务。

但 React 并不完全依赖 requestIdleCallback，因为它兼容性和控制能力有限，React 有自己的 Scheduler。

---

### 5. diff 算法原理？

React diff 用于比较新旧虚拟 DOM 的差异。

为了提高性能，React 做了三个假设：

1. 不同类型的元素会生成不同树
2. 同层级比较，不跨层级移动
3. 通过 key 判断列表元素身份

示例：

```jsx
<div></div>
```

变成：

```jsx
<span></span>
```

类型不同，直接销毁旧节点，创建新节点。

列表比较时使用 key 判断复用。

---

### 6. 协调（Reconciliation）过程？

协调是 React 根据新旧虚拟 DOM 计算变化的过程。

流程：

1. state / props 变化
2. 生成新的 React Element
3. 与旧 Fiber 树比较
4. 标记需要插入、更新、删除的节点
5. 提交阶段更新真实 DOM

协调阶段可以被中断。

提交阶段不能被中断，因为 DOM 更新必须保持一致。

---

### 7. setState 到视图更新流程？

流程：

1. 调用 setState / setX
2. 创建 update 对象
3. update 加入更新队列
4. React 调度更新
5. 执行 render，生成新的虚拟 DOM / Fiber
6. diff 比较新旧结果
7. commit 阶段更新真实 DOM
8. 浏览器绘制页面

简化：

```text
setState -> 调度 -> render -> diff -> commit -> DOM 更新
```

---

### 8. 为什么 React 可以中断渲染？

因为 Fiber 把渲染工作拆成了很多小任务。

React 可以在执行一部分后暂停，把主线程让给更紧急的任务，例如用户输入。

当空闲时再继续。

注意：

- render 阶段可以中断
- commit 阶段不能中断

因为 commit 阶段会真正修改 DOM，一旦开始必须完成。

---

## 十四、真实业务场景

### 1. 登录鉴权怎么做？

常见流程：

1. 用户输入账号密码
2. 调用登录接口
3. 后端返回 token
4. 前端保存 token
5. 请求接口时带上 token
6. 路由守卫判断是否登录

保存位置：

- localStorage
- sessionStorage
- Cookie
- 内存变量

示例：

```js
localStorage.setItem('token', token)
```

请求时：

```js
headers: {
  Authorization: `Bearer ${token}`
}
```

---

### 2. Token 失效怎么处理？

常见处理：

1. 响应拦截器判断 401
2. 清除本地 token
3. 跳转登录页
4. 提示用户重新登录

Axios 示例：

```js
axios.interceptors.response.use(
  res => res,
  error => {
    if (error.response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
```

如果有 refresh token：

1. access token 过期
2. 使用 refresh token 请求新 token
3. 更新 token
4. 重试原请求

---

### 3. 权限系统如何设计？

常见权限：

1. 登录权限
2. 路由权限
3. 菜单权限
4. 按钮权限
5. 接口权限

前端流程：

1. 登录后获取用户信息
2. 获取角色和权限列表
3. 根据权限生成菜单
4. 根据权限生成路由
5. 页面中控制按钮显示

注意：

> 前端权限只能控制显示，真正的安全必须由后端接口权限控制。

---

### 4. 多 Tab 状态同步？

常见方案：

### localStorage storage 事件

```js
window.addEventListener('storage', event => {
  if (event.key === 'token') {
    // 其他 tab 的 token 变化
  }
})
```

适合：

- 退出登录同步
- 用户信息变化同步
- 主题变化同步

### BroadcastChannel

```js
const channel = new BroadcastChannel('app')

channel.postMessage({ type: 'logout' })

channel.onmessage = event => {
  console.log(event.data)
}
```

BroadcastChannel 更适合现代浏览器中的多 Tab 通信。

---

### 5. 表单性能优化？

常见方法：

1. 拆分表单项组件
2. 避免整个表单每次重渲染
3. 使用非受控组件
4. 使用 react-hook-form
5. 防抖校验
6. 按需校验
7. 大表单分步骤
8. 错误信息局部更新

复杂表单中，受控组件太多可能导致输入卡顿。

react-hook-form 通过非受控思想减少重渲染。

---

### 6. 大列表渲染优化？

方案：

1. 虚拟列表
2. 分页
3. 无限滚动
4. 懒加载
5. React.memo
6. 避免复杂 item 渲染
7. 图片懒加载
8. 后端分页

推荐库：

- react-window
- react-virtualized

---

### 7. 前端错误监控方案？

监控内容：

1. JS 运行错误
2. Promise 未捕获错误
3. 资源加载错误
4. 接口请求错误
5. React 组件错误
6. 白屏
7. 性能指标

React 组件错误可以用 ErrorBoundary：

```jsx
class ErrorBoundary extends React.Component {
  componentDidCatch(error, info) {
    // 上报错误
  }

  render() {
    return this.props.children
  }
}
```

---

### 8. 图片懒加载方案？

### 原生 loading

```html
<img src="a.jpg" loading="lazy" />
```

### IntersectionObserver

```js
const observer = new IntersectionObserver(entries => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const img = entry.target
      img.src = img.dataset.src
      observer.unobserve(img)
    }
  })
})
```

作用：

- 图片进入视口再加载
- 减少首屏请求
- 提升页面速度

---

### 9. SEO 如何做？

普通 React SPA 对 SEO 不友好，因为页面内容通常由 JavaScript 动态生成。

解决方案：

1. SSR：服务端渲染
2. SSG：静态生成
3. 预渲染
4. 设置 title / meta
5. 使用语义化 HTML
6. sitemap
7. robots.txt
8. 合理的 URL 结构

常用框架：

- Next.js
- Remix

---

### 10. SSR / CSR / SSG 区别？

### CSR：客户端渲染

浏览器下载 JS 后，由 JS 渲染页面。

优点：

- 前后端分离
- 交互体验好

缺点：

- 首屏可能慢
- SEO 较弱

### SSR：服务端渲染

服务端生成 HTML 返回给浏览器。

优点：

- 首屏快
- SEO 好

缺点：

- 服务端压力大
- 架构复杂

### SSG：静态生成

构建时生成 HTML 文件。

优点：

- 访问速度快
- SEO 好
- 部署简单

缺点：

- 不适合频繁变化的动态内容

对比：

| 类型 | 渲染位置 | SEO | 首屏 | 适合场景 |
| --- | --- | --- | --- | --- |
| CSR | 浏览器 | 一般 | 一般 | 后台系统 |
| SSR | 服务端 | 好 | 快 | 内容站、电商 |
| SSG | 构建时 | 好 | 很快 | 博客、文档 |

---

## 十五、面试速记总结

## React 核心

React 的核心是：

```text
组件化 + 声明式 UI + 数据驱动视图 + 虚拟 DOM
```

## Hooks 核心

Hooks 解决的是：

```text
函数组件状态管理 + 副作用管理 + 逻辑复用
```

## 性能优化核心

性能优化主要围绕：

```text
减少重渲染 + 减少重复计算 + 减少首屏资源 + 大列表虚拟化
```

## React 原理核心

React 更新流程：

```text
state/props 变化
-> render
-> 生成新虚拟 DOM / Fiber
-> diff
-> commit
-> 更新真实 DOM
```

## 业务开发核心

真实项目中常考：

```text
登录鉴权
权限控制
路由守卫
Token 失效
表单优化
大列表优化
错误监控
SEO
SSR/CSR/SSG
```

---

## 十六、推荐背诵版回答模板

## React 是什么？

React 是一个用于构建用户界面的 JavaScript 库，主要负责视图层。它的核心思想是组件化、声明式 UI 和数据驱动视图。开发者通过 state 和 props 描述页面状态，React 根据数据变化自动更新 UI。

## Hooks 是什么？

Hooks 是 React 16.8 引入的特性，让函数组件也可以使用 state、生命周期和副作用等能力。常见 Hooks 有 useState、useEffect、useMemo、useCallback、useRef 等。Hooks 的优势是代码更简洁，逻辑更容易复用。

## key 的作用？

key 是 React 在列表 diff 时用来识别元素身份的标识。稳定唯一的 key 可以帮助 React 准确复用 DOM 和组件实例，提高性能，并避免状态错乱。

## useEffect 的作用？

useEffect 用于处理组件中的副作用，例如请求数据、订阅事件、设置定时器、修改标题等。它可以通过依赖数组控制执行时机，也可以返回清理函数处理卸载逻辑。

## React.memo 和 useMemo 区别？

React.memo 用于缓存组件，避免 props 没变时子组件重复渲染。useMemo 用于缓存计算结果，避免复杂计算每次渲染都重新执行。

## Fiber 是什么？

Fiber 是 React 16 引入的新架构，是 React 内部描述组件树和调度任务的数据结构。它把渲染任务拆成小单元，使 React 可以中断、恢复和优先处理高优先级任务，从而提升页面响应性能。

---

## 十七、学习顺序建议

建议按下面顺序学习：

1. JSX 和组件
2. state 和 props
3. 事件处理和表单
4. Hooks
5. 组件通信
6. React Router
7. 状态管理
8. 性能优化
9. 工程化
10. React 18
11. Fiber 和 diff 原理
12. 真实业务场景

---

## 十八、常见面试答题技巧

回答 React 面试题时，可以按照这个结构：

```text
1. 先说定义
2. 再说作用
3. 然后说使用场景
4. 最后说注意点或原理
```

例如回答 useMemo：

```text
useMemo 是 React 提供的一个 Hook，用于缓存计算结果。
当依赖项没有变化时，它会复用上一次的计算结果，避免重复执行复杂计算。
常用于大数组过滤、排序、统计，也可以用来保持对象引用稳定。
但不应该滥用，简单计算没必要使用 useMemo，否则会增加代码复杂度。
```

---

## 十九、练习建议

学完本教程后，可以重点练习这些代码题：

1. useState 多次更新输出结果
2. useEffect 依赖数组执行顺序
3. React.memo + useCallback 是否会重新渲染
4. key 使用 index 导致的问题
5. 受控组件实现登录表单
6. 路由守卫实现登录鉴权
7. Redux 数据流手写
8. 自定义 Hook 封装请求逻辑
9. 虚拟列表原理实现
10. Token 失效自动跳转登录

---

## 二十、结束语

React 面试不是只背 API，更重要的是理解：

```text
数据如何变化
组件如何渲染
状态如何传递
性能如何优化
真实项目如何落地
```

掌握这些之后，大部分 React 基础和中级面试题都可以比较稳定地回答。
