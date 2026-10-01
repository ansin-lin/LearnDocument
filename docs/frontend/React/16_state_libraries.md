# 第 16 章 Zustand 与 Redux Toolkit

## 本章目标

- 理解 Store、State、Action、Selector 和 Dispatch 分别是什么。
- 判断数据应该保存在组件中，还是共享状态库中。
- 从零建立 Zustand Store，并在组件中读取和修改状态。
- 使用 Zustand 管理任务列表和异步请求状态。
- 从零建立 Redux Toolkit 的 Slice、Store 和 Provider。
- 使用 Redux Toolkit 完成同步修改和异步读取。

本章重点不是继续改造前面章节的代码，而是通过可以单独练习的小示例，掌握状态库到底怎样使用。

Zustand 和 Redux Toolkit 是两种不同方案。学习时可以分别建立示例进行比较，实际项目通常按照团队规范选择其中一种，不需要把同一份状态同时保存在两个 Store 中。

## 1. 为什么需要状态库

### 1.1 只有一个组件使用时

如果数据只由当前组件使用，直接使用 `useState()` 最简单：

```jsx
import { useState } from 'react';

export function Counter() {
  const [count, setCount] = useState(0);

  return (
    <button type="button" onClick={() => setCount(count + 1)}>
      点击次数：{count}
    </button>
  );
}
```

`count` 只属于 `Counter`，没有必要为了它引入状态库。

### 1.2 多个无直接父子关系的组件共同使用时

假设页面中有以下组件：

```text
Header：显示未完成任务数
TaskList：显示和修改任务
TaskToolbar：清除已完成任务
```

三个组件都要读取或修改同一份任务数据。继续逐层传递 Props 会让中间组件也承担传递工作。此时可以把共享数据放入 Store：

```text
Header ────────┐
TaskList ──────┼→ Task Store
TaskToolbar ───┘
```

Store 不是数据库。页面刷新以后，普通 Store 中的数据会重新初始化；需要永久保存的业务数据仍应由 Backend / Database 管理。

### 1.3 不应放入 Store 的常见数据

- 当前输入框中尚未提交的文字；
- 只在一个组件中使用的 Dialog 开关；
- 只由父组件和一个子组件使用的数据；
- 可以根据现有 State 直接计算出的值；
- 需要永久保存的数据库数据本身。

判断重点不是“能不能放”，而是“是否有多个无直接父子关系的组件需要共享和修改”。

## 2. 先理解五个术语

| 术语 | 含义 | 任务列表示例 |
| --- | --- | --- |
| Store | 保存共享状态和操作的容器 | Task Store |
| State | 当前保存的数据 | `tasks`、`loading` |
| Action | 有业务含义的状态操作 | `addTask()`、`removeTask()` |
| Selector | 从 Store 选择组件需要的数据 | 只读取 `tasks` |
| Dispatch | 把 Action 发送给 Redux Store | `dispatch(addTask('确认设计书'))` |

Zustand 通常直接调用 Store 中的 Action。Redux Toolkit 通常先由 Action Creator 建立 Action，再通过 `dispatch()` 发送给 Store。

## 3. Zustand：先完成最小计数器

Zustand 的 API 较少，建立 Store 时不需要额外的 Provider。本章使用以下版本：

```bash
npm install --save-exact zustand@5.0.15
```

### 3.1 建立 Store 文件

新建 `src/stores/counterStore.js`：

```js
import { create } from 'zustand';

export const useCounterStore = create((set) => ({
  count: 0,

  increase: () => set((state) => ({
    count: state.count + 1,
  })),

  decrease: () => set((state) => ({
    count: state.count - 1,
  })),

  reset: () => set({ count: 0 }),
}));
```

这段代码可以按以下顺序阅读：

1. `create()` 建立一个 Zustand Store，并返回供 React 组件使用的 Hook。
2. `count` 是 Store 中的 State，初始值为 `0`。
3. `increase`、`decrease` 和 `reset` 是 Action。
4. `set()` 用来更新 Store 中的 State。
5. 新值依赖旧值时，向 `set()` 传入函数，通过 `state` 取得更新前的状态。

### 3.2 在组件中使用 Store

新建 `src/components/CounterPanel.jsx`：

```jsx
import { useCounterStore } from '../stores/counterStore';

export function CounterPanel() {
  const count = useCounterStore((state) => state.count);
  const increase = useCounterStore((state) => state.increase);
  const decrease = useCounterStore((state) => state.decrease);
  const reset = useCounterStore((state) => state.reset);

  return (
    <section>
      <h2>Zustand 计数器</h2>
      <p>当前数值：{count}</p>

      <button type="button" onClick={decrease}>
        -1
      </button>
      <button type="button" onClick={increase}>
        +1
      </button>
      <button type="button" onClick={reset}>
        重置
      </button>
    </section>
  );
}
```

在 `App.jsx` 中显示组件：

```jsx
import { CounterPanel } from './components/CounterPanel';

export function App() {
  return <CounterPanel />;
}
```

运行后可以观察到：

- 点击 `+1`，`count` 增加；
- 点击 `-1`，`count` 减少；
- 点击“重置”，`count` 变回 `0`；
- Action 更新 Store 后，订阅 `count` 的组件自动重新渲染。

### 3.3 Selector 是什么

以下函数就是 Selector：

```js
(state) => state.count
```

它表示“从整个 Store 中只选择 `count`”。推荐组件只订阅自己需要的字段：

```js
const count = useCounterStore((state) => state.count);
```

不要习惯性订阅整个 Store：

```js
// 不推荐：Store 中任何字段变化都可能使组件重新渲染
const store = useCounterStore();
```

## 4. Zustand：管理任务列表

计数器只能说明最小写法。下面用任务列表练习新增、状态切换和删除。

### 4.1 建立任务 Store

新建 `src/stores/taskStore.js`：

```js
import { create } from 'zustand';

export const useTaskStore = create((set) => ({
  tasks: [
    { id: 'task-1', title: '确认画面设计书', completed: false },
  ],

  addTask: (title) => set((state) => ({
    tasks: [
      ...state.tasks,
      {
        id: crypto.randomUUID(),
        title,
        completed: false,
      },
    ],
  })),

  toggleTask: (id) => set((state) => ({
    tasks: state.tasks.map((task) => (
      task.id === id
        ? { ...task, completed: !task.completed }
        : task
    )),
  })),

  removeTask: (id) => set((state) => ({
    tasks: state.tasks.filter((task) => task.id !== id),
  })),
}));
```

三个 Action 分别做以下工作：

| Action | 使用场景 | 数组处理方法 |
| --- | --- | --- |
| `addTask(title)` | 新增任务 | 展开旧数组，再加入新对象 |
| `toggleTask(id)` | 切换完成状态 | 用 `map()` 建立新数组 |
| `removeTask(id)` | 删除任务 | 用 `filter()` 排除目标任务 |

这些写法不会直接修改原数组，更符合 React 状态更新的基本原则。

### 4.2 建立任务组件

新建 `src/components/TaskPanel.jsx`：

```jsx
import { useState } from 'react';
import { useTaskStore } from '../stores/taskStore';

export function TaskPanel() {
  const [title, setTitle] = useState('');

  const tasks = useTaskStore((state) => state.tasks);
  const addTask = useTaskStore((state) => state.addTask);
  const toggleTask = useTaskStore((state) => state.toggleTask);
  const removeTask = useTaskStore((state) => state.removeTask);

  function handleSubmit(event) {
    event.preventDefault();

    const normalizedTitle = title.trim();
    if (!normalizedTitle) return;

    addTask(normalizedTitle);
    setTitle('');
  }

  return (
    <section>
      <h2>任务列表</h2>

      <form onSubmit={handleSubmit}>
        <label htmlFor="task-title">任务名称</label>
        <input
          id="task-title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
        />
        <button type="submit">新增</button>
      </form>

      {tasks.length === 0 ? (
        <p>暂无任务</p>
      ) : (
        <ul>
          {tasks.map((task) => (
            <li key={task.id}>
              <label>
                <input
                  type="checkbox"
                  checked={task.completed}
                  onChange={() => toggleTask(task.id)}
                />
                <span>{task.title}</span>
              </label>
              <button
                type="button"
                onClick={() => removeTask(task.id)}
              >
                删除
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
```

这里同时使用了两种状态：

| 数据 | 保存位置 | 原因 |
| --- | --- | --- |
| 输入中的 `title` | 当前组件的 `useState()` | 尚未提交，只属于当前表单 |
| 已提交的 `tasks` | Zustand Store | 可能被列表、Header 等多个组件共享 |

状态库并不取代 `useState()`。局部状态继续留在组件，共享状态才放入 Store。

### 4.3 让另一个组件共享任务数据

新建 `src/components/TaskSummary.jsx`：

```jsx
import { useTaskStore } from '../stores/taskStore';

export function TaskSummary() {
  const totalCount = useTaskStore((state) => state.tasks.length);
  const completedCount = useTaskStore((state) => (
    state.tasks.filter((task) => task.completed).length
  ));

  return (
    <p>
      全部：{totalCount}，已完成：{completedCount}
    </p>
  );
}
```

然后同时显示两个组件：

```jsx
import { TaskPanel } from './components/TaskPanel';
import { TaskSummary } from './components/TaskSummary';

export function App() {
  return (
    <>
      <TaskSummary />
      <TaskPanel />
    </>
  );
}
```

在 `TaskPanel` 中新增、完成或删除任务后，`TaskSummary` 会同步变化。这正是共享 Store 的用途。

## 5. Zustand：从 API 异步读取数据

### 5.1 先约定 API 层

下面假设 `GET /tasks` 成功时直接返回任务数组：

```json
[
  { "id": 1, "title": "确认画面设计书", "completed": false },
  { "id": 2, "title": "执行单体测试", "completed": true }
]
```

新建 `src/api/taskService.js`：

```js
import { httpClient } from './httpClient';

export async function getTasks() {
  const response = await httpClient.get('/tasks');
  return response.data;
}
```

`getTasks()` 负责发送 HTTP 请求并返回数据。Store 不需要知道 URL、Header 等 HTTP 细节。

### 5.2 建立异步 Store

为了与前面的本地任务示例区分，新建 `src/stores/remoteTaskStore.js`：

```js
import { create } from 'zustand';
import { getTasks } from '../api/taskService';

export const useRemoteTaskStore = create((set, get) => ({
  tasks: [],
  loading: false,
  errorMessage: '',

  loadTasks: async () => {
    if (get().loading) return;

    set({
      loading: true,
      errorMessage: '',
    });

    try {
      const tasks = await getTasks();
      set({ tasks });
    } catch {
      set({
        tasks: [],
        errorMessage: '任务读取失败，请稍后重试。',
      });
    } finally {
      set({ loading: false });
    }
  },
}));
```

`create((set, get) => ...)` 中：

- `set()` 用于更新 State；
- `get()` 用于读取 Store 当前的 State；
- `loading` 表示正在读取；
- `errorMessage` 保存读取失败时要显示的消息；
- `if (get().loading) return` 防止读取过程中重复发送请求。

### 5.3 页面首次显示时读取

新建 `src/pages/TaskListPage.jsx`：

```jsx
import { useEffect } from 'react';
import { useRemoteTaskStore } from '../stores/remoteTaskStore';

export function TaskListPage() {
  const tasks = useRemoteTaskStore((state) => state.tasks);
  const loading = useRemoteTaskStore((state) => state.loading);
  const errorMessage = useRemoteTaskStore(
    (state) => state.errorMessage,
  );
  const loadTasks = useRemoteTaskStore((state) => state.loadTasks);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  if (loading) {
    return <p role="status">读取中...</p>;
  }

  if (errorMessage) {
    return (
      <section>
        <p role="alert">{errorMessage}</p>
        <button type="button" onClick={loadTasks}>
          再次读取
        </button>
      </section>
    );
  }

  if (tasks.length === 0) {
    return <p>暂无任务</p>;
  }

  return (
    <ul>
      {tasks.map((task) => (
        <li key={task.id}>{task.title}</li>
      ))}
    </ul>
  );
}
```

执行顺序如下：

```text
页面显示
  ↓
useEffect() 调用 loadTasks()
  ↓
loading 变为 true
  ↓
getTasks() 发送请求
  ↓
成功：保存 tasks
失败：保存 errorMessage
  ↓
finally 中把 loading 恢复为 false
  ↓
组件显示列表、空状态或错误状态
```

Zustand 只是帮助管理共享状态，不会自动提供请求缓存、失效判断或重试策略。这些能力需要按照项目要求另外设计。

## 6. Redux Toolkit：先完成最小计数器

Redux Toolkit 的组成比 Zustand 多，但 Action 的发生顺序更加明确，适合需要统一约束、详细调试记录，或已经采用 Redux 的项目。

```bash
npm install --save-exact @reduxjs/toolkit@2.12.0 react-redux@9.3.0
```

### 6.1 写代码前先理解整体结构

Redux Toolkit 的代码分散在几个文件中，是因为它把“状态更新规则”“全局状态容器”和“React 组件”分开管理。第一次学习时，先不要急着记住所有方法，先理解每一部分负责什么。

#### 6.1.1 必须先理解的概念

| 概念 | 作用 | 计数器中的例子 |
| --- | --- | --- |
| State | 当前保存的数据 | `{ value: 0 }` |
| Slice | 某一类业务状态及其更新规则 | `counterSlice` |
| Reducer | 根据 Action 更新 State 的函数 | 把 `value` 增加 `1` |
| Action | 描述“发生了什么”的普通对象 | `{ type: 'counter/increment' }` |
| Action Creator | 用来建立 Action 的函数 | `increment()` |
| Store | 汇总并保存全局 State 的容器 | `store` |
| Provider | 把 Store 提供给 React 组件 | `<Provider store={store}>` |
| Selector | 从 Store 中读取需要的数据 | `state.counter.value` |
| Dispatch | 把 Action 发送给 Store | `dispatch(increment())` |

可以把它们简单理解为：

```text
Slice
├─ 定义初始数据 State
├─ 定义更新规则 Reducer
└─ 自动生成 Action Creator

Store
└─ 汇总一个或多个 Slice Reducer

Provider
└─ 让 React 组件能够访问 Store

Component
├─ 通过 Selector 读取 State
└─ 通过 Dispatch 发送 Action
```

其中最容易混淆的是 Action、Action Creator 和 Reducer：

```js
increment();
// Action Creator 被调用，返回 Action：
// { type: 'counter/increment', payload: undefined }
```

组件把这个 Action 交给 `dispatch()`。Store 再把 Action 交给对应的 Reducer，由 Reducer 决定 State 应该怎样改变。

#### 6.1.2 创建 Redux 功能时的顺序

第一次建立 Redux Toolkit 功能时，按照以下顺序操作：

```text
第 1 步：建立 Slice
定义初始 State、Reducer，并导出 Action Creator 和 Reducer
        ↓
第 2 步：建立 Store
把 Slice Reducer 注册到 configureStore()
        ↓
第 3 步：配置 Provider
在 main.jsx 中把 Store 提供给整个 React 应用
        ↓
第 4 步：组件读取 State
使用 useSelector()
        ↓
第 5 步：组件修改 State
使用 useDispatch() 发送 Action
```

本节后面的代码也严格按照这个顺序展开：

| 顺序 | 文件 | 负责的内容 |
| --- | --- | --- |
| 1 | `counterSlice.js` | 定义计数值和加减规则 |
| 2 | `store.js` | 注册 `counterReducer` |
| 3 | `main.jsx` | 使用 `Provider` 提供 Store |
| 4 | `ReduxCounter.jsx` | 读取数值并发送加减 Action |

#### 6.1.3 用户点击按钮后的运行顺序

“创建顺序”说明代码应该先写什么，“运行顺序”说明页面操作后程序发生什么。两者不要混在一起。

用户点击 `+1` 时：

```text
用户点击按钮
    ↓
组件调用 increment()
    ↓
increment() 建立 Action
{ type: 'counter/increment' }
    ↓
dispatch() 把 Action 发送给 Store
    ↓
Store 调用 counterReducer
    ↓
Reducer 把 value 加 1
    ↓
Store 保存新的 State
    ↓
useSelector() 读到新 value
    ↓
组件重新渲染，页面显示新数值
```

组件不能直接调用 Reducer，也不应该直接修改 Store。组件只负责读取数据和发送“发生了什么”，具体更新规则由 Slice 管理。

### 6.2 建立 Slice

新建 `src/stores/counterSlice.js`：

```js
import { createSlice } from '@reduxjs/toolkit';

const counterSlice = createSlice({
  name: 'counter',
  initialState: {
    value: 0,
  },
  reducers: {
    increment: (state) => {
      state.value += 1;
    },
    decrement: (state) => {
      state.value -= 1;
    },
    incrementByAmount: (state, action) => {
      state.value += action.payload;
    },
    reset: (state) => {
      state.value = 0;
    },
  },
});

export const {
  increment,
  decrement,
  incrementByAmount,
  reset,
} = counterSlice.actions;

export default counterSlice.reducer;
```

`createSlice()` 会根据配置生成：

- Slice Reducer：根据 Action 更新 State；
- Action Creator：建立 Action 对象；
- Action Type：例如 `counter/increment`。

```js
console.log(increment());
// { type: 'counter/increment', payload: undefined }

console.log(incrementByAmount(5));
// { type: 'counter/incrementByAmount', payload: 5 }
```

`action.payload` 就是调用 Action Creator 时传入的数据。

Reducer 中虽然写了 `state.value += 1`，Redux Toolkit 会通过内部的 Immer 产生新的不可变 State。这个规则只适用于 Redux Toolkit 的 Reducer，不要因此直接修改普通 React State。

### 6.3 建立 Store

新建 `src/stores/store.js`：

```js
import { configureStore } from '@reduxjs/toolkit';
import counterReducer from './counterSlice';

export const store = configureStore({
  reducer: {
    counter: counterReducer,
  },
});
```

这里的 `counter` 决定数据保存在 `state.counter`：

```js
{
  counter: {
    value: 0
  }
}
```

### 6.4 用 Provider 提供 Store

修改 `src/main.jsx`：

```jsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { App } from './App';
import { store } from './stores/store';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </StrictMode>,
);
```

`Provider` 让下层组件访问同一个 Redux Store。忘记包裹时，`useSelector()` 和 `useDispatch()` 会报告找不到 Provider。

### 6.5 在组件中读取和修改

新建 `src/components/ReduxCounter.jsx`：

```jsx
import { useDispatch, useSelector } from 'react-redux';
import {
  decrement,
  increment,
  incrementByAmount,
  reset,
} from '../stores/counterSlice';

export function ReduxCounter() {
  const count = useSelector((state) => state.counter.value);
  const dispatch = useDispatch();

  return (
    <section>
      <h2>Redux Toolkit 计数器</h2>
      <p>当前数值：{count}</p>

      <button type="button" onClick={() => dispatch(decrement())}>
        -1
      </button>
      <button type="button" onClick={() => dispatch(increment())}>
        +1
      </button>
      <button
        type="button"
        onClick={() => dispatch(incrementByAmount(5))}
      >
        +5
      </button>
      <button type="button" onClick={() => dispatch(reset())}>
        重置
      </button>
    </section>
  );
}
```

`useSelector()` 用于读取 State，`useDispatch()` 用于取得 `dispatch` 函数。点击 `+5` 后发生的过程是：

```text
incrementByAmount(5) 建立 Action
  ↓
{ type: 'counter/incrementByAmount', payload: 5 }
  ↓
dispatch(Action)
  ↓
counterSlice Reducer 更新 State
  ↓
Store 保存新 State
  ↓
Selector 结果变化
  ↓
组件重新渲染
```

## 7. Redux Toolkit：任务列表的同步操作

新建 `src/stores/taskSlice.js`：

```js
import { createSlice } from '@reduxjs/toolkit';

const taskSlice = createSlice({
  name: 'tasks',
  initialState: {
    items: [],
  },
  reducers: {
    taskAdded: (state, action) => {
      state.items.push({
        id: crypto.randomUUID(),
        title: action.payload,
        completed: false,
      });
    },
    taskToggled: (state, action) => {
      const task = state.items.find(
        (item) => item.id === action.payload,
      );

      if (task) {
        task.completed = !task.completed;
      }
    },
    taskRemoved: (state, action) => {
      state.items = state.items.filter(
        (task) => task.id !== action.payload,
      );
    },
  },
});

export const {
  taskAdded,
  taskToggled,
  taskRemoved,
} = taskSlice.actions;

export default taskSlice.reducer;
```

在 `store.js` 中注册 Task Reducer：

```js
import { configureStore } from '@reduxjs/toolkit';
import counterReducer from './counterSlice';
import taskReducer from './taskSlice';

export const store = configureStore({
  reducer: {
    counter: counterReducer,
    tasks: taskReducer,
  },
});
```

组件中的基本用法如下：

```jsx
import { useDispatch, useSelector } from 'react-redux';
import {
  taskAdded,
  taskRemoved,
  taskToggled,
} from '../stores/taskSlice';

export function ReduxTaskList() {
  const tasks = useSelector((state) => state.tasks.items);
  const dispatch = useDispatch();

  return (
    <section>
      <button
        type="button"
        onClick={() => dispatch(taskAdded('确认测试结果'))}
      >
        添加示例任务
      </button>

      <ul>
        {tasks.map((task) => (
          <li key={task.id}>
            <button
              type="button"
              onClick={() => dispatch(taskToggled(task.id))}
            >
              {task.completed ? '已完成' : '未完成'}
            </button>
            <span>{task.title}</span>
            <button
              type="button"
              onClick={() => dispatch(taskRemoved(task.id))}
            >
              删除
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
```

组件不直接修改 `tasks`。它只表达“发生了新增”“发生了状态切换”“发生了删除”，具体更新规则集中在 Slice 中。

## 8. Redux Toolkit：使用 createAsyncThunk 读取 API

### 8.1 为什么不能在 Reducer 中发送请求

Reducer 的任务是根据旧 State 和 Action 计算新 State。它不应该直接发送 HTTP 请求、操作 DOM 或读取浏览器存储。

Redux Toolkit 提供 `createAsyncThunk()` 描述常见的异步处理。一次异步请求会产生三种 Action：

| 状态 | 发生时间 | 常见处理 |
| --- | --- | --- |
| `pending` | 请求开始 | 显示 Loading、清除旧错误 |
| `fulfilled` | 请求成功 | 保存返回数据 |
| `rejected` | 请求失败 | 保存错误消息 |

### 8.2 建立异步 Task Slice

将 `src/stores/taskSlice.js` 改为以下版本：

```js
import {
  createAsyncThunk,
  createSlice,
} from '@reduxjs/toolkit';
import { getTasks } from '../api/taskService';

export const loadTasks = createAsyncThunk(
  'tasks/loadTasks',
  async () => {
    return getTasks();
  },
);

const taskSlice = createSlice({
  name: 'tasks',
  initialState: {
    items: [],
    loadStatus: 'idle',
    errorMessage: '',
  },
  reducers: {
    taskAdded: (state, action) => {
      state.items.push({
        id: crypto.randomUUID(),
        title: action.payload,
        completed: false,
      });
    },
    taskToggled: (state, action) => {
      const task = state.items.find(
        (item) => item.id === action.payload,
      );

      if (task) {
        task.completed = !task.completed;
      }
    },
    taskRemoved: (state, action) => {
      state.items = state.items.filter(
        (task) => task.id !== action.payload,
      );
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadTasks.pending, (state) => {
        state.loadStatus = 'loading';
        state.errorMessage = '';
      })
      .addCase(loadTasks.fulfilled, (state, action) => {
        state.loadStatus = 'succeeded';
        state.items = action.payload;
      })
      .addCase(loadTasks.rejected, (state) => {
        state.loadStatus = 'failed';
        state.items = [];
        state.errorMessage = '任务读取失败，请稍后重试。';
      });
  },
});

export const {
  taskAdded,
  taskToggled,
  taskRemoved,
} = taskSlice.actions;

export default taskSlice.reducer;
```

这里需要区分两部分：

- `reducers` 处理当前应用可以立即完成的同步操作；
- `extraReducers` 处理 `createAsyncThunk()` 产生的异步生命周期 Action。

请求成功时，`action.payload` 是 `getTasks()` 返回的任务数组。

### 8.3 在页面中触发异步读取

新建 `src/pages/ReduxTaskListPage.jsx`：

```jsx
import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { loadTasks } from '../stores/taskSlice';

export function ReduxTaskListPage() {
  const tasks = useSelector((state) => state.tasks.items);
  const loadStatus = useSelector(
    (state) => state.tasks.loadStatus,
  );
  const errorMessage = useSelector(
    (state) => state.tasks.errorMessage,
  );
  const dispatch = useDispatch();

  useEffect(() => {
    if (loadStatus === 'idle') {
      dispatch(loadTasks());
    }
  }, [dispatch, loadStatus]);

  if (loadStatus === 'idle' || loadStatus === 'loading') {
    return <p role="status">读取中...</p>;
  }

  if (loadStatus === 'failed') {
    return (
      <section>
        <p role="alert">{errorMessage}</p>
        <button
          type="button"
          onClick={() => dispatch(loadTasks())}
        >
          再次读取
        </button>
      </section>
    );
  }

  if (tasks.length === 0) {
    return <p>暂无任务</p>;
  }

  return (
    <ul>
      {tasks.map((task) => (
        <li key={task.id}>{task.title}</li>
      ))}
    </ul>
  );
}
```

首次显示时只在 `idle` 状态发送请求。请求开始后变成 `loading`，因此组件重新渲染也不会重复读取。

## 9. Zustand 与 Redux Toolkit 的写法对照

### 9.1 更新流程

Zustand：

```text
组件调用 Action
  ↓
Action 调用 set()
  ↓
Store 更新
  ↓
Selector 取得新值
```

Redux Toolkit：

```text
组件调用 Action Creator
  ↓
dispatch(Action)
  ↓
Reducer 更新 State
  ↓
Store 保存
  ↓
Selector 取得新值
```

### 9.2 选型参考

| 方案 | 适合情况 | 主要特点 |
| --- | --- | --- |
| Context | 主题、认证接口等低频全局依赖 | React 内置，机制较少 |
| Zustand | 轻量客户端共享状态 | API 简洁，可直接调用 Action |
| Redux Toolkit | Action 多、团队约束和调查要求高 | 数据流明确，DevTools 成熟 |

不要只根据项目大小选型，还要考虑既有技术栈、团队规范、调试要求和维护周期。

## 10. 已有认证代码怎样迁移

理解前面的独立示例后，再迁移认证状态会更容易。以 Zustand 为例：

```js
import { create } from 'zustand';

export const useAuthStore = create((set) => ({
  status: 'checking',
  user: null,

  setUser: (user) => set(
    user
      ? { status: 'authenticated', user }
      : { status: 'anonymous', user: null },
  ),

  clearAuth: () => set({
    status: 'anonymous',
    user: null,
  }),
}));
```

组件只选择自己需要的数据：

```jsx
const status = useAuthStore((state) => state.status);
const userName = useAuthStore((state) => state.user?.name ?? '');
```

迁移时应该让 Header、登录页和路由门禁统一使用同一个 Store，并移除原来保存同一用户数据的 Context State。这个示例用于说明已有代码的迁移方向，不是本章理解状态库的前提。

## 11. 调试方法

出现“点击后页面没变化”时，按以下顺序确认：

1. 点击事件是否执行；
2. 是否调用了正确的 Action；
3. Action 参数或 Redux `payload` 是否正确；
4. Store 中的 State 是否变化；
5. Selector 是否读取了正确路径；
6. 组件是否使用 Selector 返回的新值渲染。

Redux 项目可以用 Redux DevTools 查看 Action、Payload 和 State 的前后变化。Zustand 也可以另外配置开发工具中间件，但不是完成基本 Store 的前提。

## 12. 常见错误

### 12.1 把所有数据都放进 Store

状态库只解决需要共享的客户端状态。输入框等局部状态继续使用 `useState()`。

### 12.2 直接修改 Zustand 数组

不要这样写：

```js
// 错误示例
state.tasks.push(newTask);
```

应该通过 `set()` 返回新的数组：

```js
set((state) => ({
  tasks: [...state.tasks, newTask],
}));
```

### 12.3 忘记 Redux Provider

`useSelector()` 或 `useDispatch()` 报 Provider 相关错误时，确认根组件是否被 `<Provider store={store}>` 包裹。

### 12.4 Selector 路径写错

Store 注册方式是：

```js
reducer: {
  tasks: taskReducer,
}
```

因此读取路径是：

```js
state.tasks.items
```

不是 `state.items`。

### 12.5 在 Reducer 中发送请求

Reducer 只负责状态计算。HTTP 请求应放在 API Service、`createAsyncThunk()` 或项目采用的其他异步层中。

### 12.6 Store 保存后误以为数据库已经更新

只修改 Store 不等于数据已经保存到 Backend。刷新页面后是否能够恢复，要看是否调用保存 API，以及页面启动时是否重新读取。

## 13. 练习

### 练习 1：Zustand 计数器

完成 `counterStore.js` 和 `CounterPanel.jsx`，再增加“`+10`”按钮。要求通过新的 Action 修改数值，不在组件中直接修改 Store 数据。

### 练习 2：Zustand 任务列表

完成任务的新增、完成状态切换和删除。再建立一个独立的 `TaskSummary`，显示全部任务数和未完成任务数。

### 练习 3：异步状态

为任务列表实现 `loading`、`success`、`empty` 和 `error` 四种显示状态。断开 Backend 后确认错误消息和“再次读取”按钮能够显示。

### 练习 4：Redux Toolkit 数据流

完成 Redux 计数器，并使用 Redux DevTools 记录点击 `+5` 时的 Action Type、Payload、更新前 State 和更新后 State。

### 练习 5：状态归属判断

判断以下数据应放在组件、URL、Store 还是 Backend，并说明理由：

- 新增表单尚未提交的标题；
- 多个页面都要显示的当前用户；
- 可以分享给他人的搜索条件；
- 数据库中的正式任务记录；
- 只属于当前页面的确认 Dialog 开关。

参考：[Zustand 官方文档](https://zustand.docs.pmnd.rs/)、[Redux Toolkit Quick Start](https://redux-toolkit.js.org/tutorials/quick-start)。

## 本章检查点

- [ ] 能解释 Store、State、Action、Selector 和 Dispatch。
- [ ] 能判断局部 State 与共享 State 的保存位置。
- [ ] 能从零完成 Zustand 的 Store → Action → Selector → Component。
- [ ] 能使用 Zustand 管理数组的新增、修改和删除。
- [ ] 能使用 `loading` 和 `errorMessage` 表示异步请求状态。
- [ ] 能从零完成 Redux 的 Slice → Store → Provider → Component。
- [ ] 能解释 Redux Action、`payload` 和 Reducer 的关系。
- [ ] 能使用 `createAsyncThunk()` 处理请求的三种状态。
- [ ] 不会把同一份状态同时保存在多个状态方案中。
