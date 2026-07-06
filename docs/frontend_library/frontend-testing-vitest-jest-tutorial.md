# 前端测试工具教学文档：Vitest 与 Jest 入门教程

> 适合对象：刚开始学习 React、前端开发、前端单体测试的新同学。  
> 学习目标：理解前端测试的作用，掌握 Vitest / Jest 的基本用法，并能写出简单的 React 组件测试。

---

## 目录

1. [为什么前端也需要写测试](#1-为什么前端也需要写测试)
2. [前端测试分为哪几类](#2-前端测试分为哪几类)
3. [Vitest 和 Jest 是什么](#3-vitest-和-jest-是什么)
4. [Vitest 与 Jest 的区别](#4-vitest-与-jest-的区别)
5. [测试代码的基本结构](#5-测试代码的基本结构)
6. [Vitest 入门教程](#6-vitest-入门教程)
7. [Jest 入门教程](#7-jest-入门教程)
8. [React 组件测试入门](#8-react-组件测试入门)
9. [React Testing Library 是什么](#9-react-testing-library-是什么)
10. [Mock 的概念和用法](#10-mock-的概念和用法)
11. [API 请求测试](#11-api-请求测试)
12. [异步测试](#12-异步测试)
13. [覆盖率 Coverage](#13-覆盖率-coverage)
14. [Snapshot 快照测试](#14-snapshot-快照测试)
15. [真实项目中应该测什么](#15-真实项目中应该测什么)
16. [常见错误与注意点](#16-常见错误与注意点)
17. [学习路线](#17-学习路线)
18. [面试回答模板](#18-面试回答模板)
19. [练习题](#19-练习题)

---

## 1. 为什么前端也需要写测试

很多刚学习前端的人会觉得：

> 页面能打开，按钮能点，不就可以了吗？为什么还要写测试代码？

但是在真实项目里，前端页面会越来越复杂，例如：

- 表单输入校验
- 按钮点击后的状态变化
- API 请求成功 / 失败后的显示
- 登录状态判断
- 权限控制
- 列表查询、分页、排序、筛选
- 金额、日期、状态转换
- 多人协作修改同一个页面

如果没有测试，改一个地方可能会影响其他功能。尤其是项目变大以后，开发者不可能每次都手动点完整个系统。

前端测试的作用就是：

```text
用代码自动验证功能是否符合预期。
```

例如：

- 点击“保存”按钮后，是否调用了保存方法？
- 输入空用户名时，是否显示“用户名不能为空”？
- API 返回错误时，页面是否显示错误提示？
- 修改公共组件后，其他页面是否还能正常使用？

测试代码可以帮助我们更早发现问题，减少手动确认的成本。

---

## 2. 前端测试分为哪几类

前端测试常见分为以下几类。

| 类型 | 说明 | 常用工具 |
| --- | --- | --- |
| 单体测试 / Unit Test | 测试一个函数、一个小模块 | Vitest / Jest |
| 组件测试 / Component Test | 测试一个 React 组件的显示和行为 | Vitest / Jest + React Testing Library |
| 集成测试 / Integration Test | 测试多个模块组合后的行为 | Vitest / Jest + Mock |
| E2E 测试 / End to End Test | 模拟真实用户从页面入口操作完整流程 | Playwright / Cypress |

本教程主要讲：

```text
单体测试 + React 组件测试
```

也就是：

```text
Vitest / Jest + React Testing Library
```

---

## 3. Vitest 和 Jest 是什么

### 3.1 Jest 是什么

Jest 是一个老牌的 JavaScript 测试框架，在 React 项目中使用非常广泛。

它可以做这些事情：

- 运行测试文件
- 提供 `describe`、`test`、`expect`
- 提供 mock 功能
- 生成测试覆盖率
- 进行快照测试
- 支持 watch mode

可以简单理解为：

```text
Jest = 测试运行器 + 断言库 + Mock 工具 + 覆盖率工具
```

很多老项目、企业项目、Create React App 项目、Next.js 项目里都经常看到 Jest。

---

### 3.2 Vitest 是什么

Vitest 是 Vite 生态里的测试框架。

如果你的项目是：

```text
Vite + React
Vite + Vue
Vite + TypeScript
```

那么 Vitest 通常更适合。

Vitest 的优点：

- 和 Vite 配合好
- 启动速度快
- 配置相对简单
- API 风格接近 Jest
- 支持 TypeScript
- 支持 mock、coverage、snapshot

可以简单理解为：

```text
Vitest = 适合 Vite 项目的 Jest 风格测试框架
```

---

## 4. Vitest 与 Jest 的区别

| 对比项 | Vitest | Jest |
| --- | --- | --- |
| 主要适合 | Vite 项目、新项目 | 老项目、既存项目、很多企业项目 |
| 速度 | 通常更快 | 稳定，但大项目可能较慢 |
| 配置 | Vite 项目中比较简单 | 有时需要 Babel / ts-jest / SWC |
| TypeScript | 支持较自然 | 需要根据项目配置 |
| Mock 写法 | `vi.fn()` / `vi.mock()` | `jest.fn()` / `jest.mock()` |
| 学习难度 | 和 Jest 很像 | 资料多，项目中常见 |
| 生态 | 新项目越来越常见 | 使用历史长，资料非常多 |

一句话记忆：

```text
新项目用 Vite，可以优先选择 Vitest。
老项目已经用了 Jest，就继续用 Jest。
```

再记一个重点：

```text
Vitest 和 Jest 的测试写法非常像。
会一个，另一个很容易上手。
```

---

## 5. 测试代码的基本结构

无论 Vitest 还是 Jest，最常见的测试结构都是：

```ts
import { describe, test, expect } from 'vitest';

describe('测试对象名称', () => {
  test('测试用例说明', () => {
    expect(实际结果).toBe(期待结果);
  });
});
```

### 5.1 describe 是什么

`describe` 表示一组相关测试。

例如：

```ts
describe('金额计算工具', () => {
  // 这里放很多和金额计算相关的测试
});
```

可以理解为：

```text
describe = 测试分组
```

---

### 5.2 test / it 是什么

`test` 表示一个具体的测试用例。

例如：

```ts
test('两个数字相加', () => {
  expect(1 + 2).toBe(3);
});
```

`it` 和 `test` 基本一样。

```ts
it('两个数字相加', () => {
  expect(1 + 2).toBe(3);
});
```

可以理解为：

```text
test / it = 一个测试用例
```

---

### 5.3 expect 是什么

`expect` 用来断言结果是否符合预期。

例如：

```ts
expect(1 + 2).toBe(3);
```

意思是：

```text
我期待 1 + 2 的结果是 3。
```

如果结果不是 3，测试就会失败。

---

## 6. Vitest 入门教程

下面以 React + Vite + TypeScript 项目为例。

---

### 6.1 安装依赖

```bash
npm install -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

各依赖的作用：

| 依赖 | 作用 |
| --- | --- |
| `vitest` | 测试运行器 |
| `jsdom` | 在 Node 环境中模拟浏览器 DOM |
| `@testing-library/react` | 渲染 React 组件 |
| `@testing-library/jest-dom` | 提供 DOM 相关断言 |
| `@testing-library/user-event` | 模拟用户点击、输入等操作 |

---

### 6.2 配置 package.json

```json
{
  "scripts": {
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage"
  }
}
```

说明：

| 命令 | 说明 |
| --- | --- |
| `npm run test` | 进入监听模式，适合开发中使用 |
| `npm run test:run` | 只执行一次测试，适合提交代码前使用 |
| `npm run test:coverage` | 执行测试并生成覆盖率 |

---

### 6.3 配置 vite.config.ts

```ts
/// <reference types="vitest" />

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
    },
  },
});
```

重点说明：

| 配置 | 说明 |
| --- | --- |
| `environment: 'jsdom'` | 让测试环境像浏览器一样支持 DOM |
| `globals: true` | 可以不手动 import `describe`、`test`、`expect` |
| `setupFiles` | 测试执行前先加载的初始化文件 |
| `coverage` | 覆盖率配置 |

---

### 6.4 创建 setup 文件

```ts
// src/test/setup.ts
import '@testing-library/jest-dom/vitest';
```

这个文件的作用是让我们可以使用这些断言：

```ts
expect(element).toBeInTheDocument();
expect(input).toHaveValue('abc');
expect(button).toBeDisabled();
```

---

### 6.5 第一个 Vitest 测试

业务代码：

```ts
// src/utils/calc.ts
export function add(a: number, b: number): number {
  return a + b;
}
```

测试代码：

```ts
// src/utils/calc.test.ts
import { describe, expect, test } from 'vitest';
import { add } from './calc';

describe('add', () => {
  test('两个数字相加', () => {
    expect(add(1, 2)).toBe(3);
  });
});
```

运行：

```bash
npm run test
```

如果看到测试通过，说明配置成功。

---

## 7. Jest 入门教程

如果你的项目不是 Vite，或者既存项目已经用了 Jest，就可以继续使用 Jest。

---

### 7.1 安装依赖

```bash
npm install -D jest jest-environment-jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

如果项目使用 TypeScript，还可能需要：

```bash
npm install -D ts-jest @types/jest
```

或者项目也可能使用 Babel / SWC 来处理 TypeScript 和 JSX。

---

### 7.2 package.json 配置

```json
{
  "scripts": {
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  }
}
```

---

### 7.3 Jest 配置示例

```ts
// jest.config.ts
import type { Config } from 'jest';

const config: Config = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/src/test/setup.ts'],
  clearMocks: true,
  collectCoverage: true,
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
  ],
};

export default config;
```

setup 文件：

```ts
// src/test/setup.ts
import '@testing-library/jest-dom';
```

---

### 7.4 第一个 Jest 测试

业务代码：

```ts
// src/utils/multiply.ts
export function multiply(a: number, b: number): number {
  return a * b;
}
```

测试代码：

```ts
// src/utils/multiply.test.ts
import { multiply } from './multiply';

describe('multiply', () => {
  test('两个数字相乘', () => {
    expect(multiply(2, 3)).toBe(6);
  });
});
```

在 Jest 中，很多项目默认可以直接使用 `describe`、`test`、`expect`，不需要从 Jest 中 import。

---

## 8. React 组件测试入门

测试 React 组件时，我们通常不是直接测试内部变量，而是测试用户能看到什么、能做什么。

例如有一个计数器组件：

```tsx
// src/components/Counter.tsx
import { useState } from 'react';

export function Counter() {
  const [count, setCount] = useState(0);

  return (
    <div>
      <p>count: {count}</p>
      <button onClick={() => setCount(count + 1)}>加一</button>
    </div>
  );
}
```

测试代码：

```tsx
// src/components/Counter.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test } from 'vitest';
import { Counter } from './Counter';

describe('Counter', () => {
  test('点击按钮后 count 增加', async () => {
    const user = userEvent.setup();

    render(<Counter />);

    expect(screen.getByText('count: 0')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '加一' }));

    expect(screen.getByText('count: 1')).toBeInTheDocument();
  });
});
```

这段测试做了三件事：

1. 渲染 `Counter` 组件
2. 模拟用户点击“加一”按钮
3. 判断页面是否显示 `count: 1`

这比测试内部代码更有价值，因为它验证的是用户真实看到的结果。

---

## 9. React Testing Library 是什么

React Testing Library 不是 Vitest，也不是 Jest。

它的作用是：

```text
把 React 组件渲染出来，并提供查询页面元素的方法。
```

例如：

```tsx
render(<Counter />);
```

表示把组件渲染到测试环境里。

```tsx
screen.getByText('count: 0');
```

表示从页面上查找文字是 `count: 0` 的元素。

---

### 9.1 常用查询方法

| 方法 | 说明 | 例子 |
| --- | --- | --- |
| `getByText` | 根据文字查找 | `screen.getByText('保存')` |
| `getByRole` | 根据语义角色查找 | `screen.getByRole('button', { name: '保存' })` |
| `getByLabelText` | 根据 label 查找表单元素 | `screen.getByLabelText('用户名')` |
| `getByPlaceholderText` | 根据 placeholder 查找 | `screen.getByPlaceholderText('请输入用户名')` |
| `queryByText` | 查找可能不存在的元素 | `screen.queryByText('错误')` |
| `findByText` | 异步等待元素出现 | `await screen.findByText('查询成功')` |

推荐优先使用：

```tsx
screen.getByRole('button', { name: '保存' })
```

而不是：

```tsx
container.querySelector('.save-button')
```

因为用户关心的是“保存按钮”，不是 class 名。

---

### 9.2 getBy / queryBy / findBy 的区别

| 方法 | 找不到时 | 是否异步 | 适用场景 |
| --- | --- | --- | --- |
| `getBy...` | 报错 | 否 | 元素必须存在 |
| `queryBy...` | 返回 `null` | 否 | 判断元素不存在 |
| `findBy...` | 等待后还没有才报错 | 是 | 异步渲染、API 返回后显示 |

例子：

```tsx
expect(screen.getByText('保存')).toBeInTheDocument();
```

表示“保存”必须存在。

```tsx
expect(screen.queryByText('错误信息')).not.toBeInTheDocument();
```

表示“错误信息”不应该存在。

```tsx
expect(await screen.findByText('加载完成')).toBeInTheDocument();
```

表示等待异步内容出现。

---

## 10. Mock 的概念和用法

Mock 可以理解为“假的替身”。

真实项目中，有些东西测试时不适合真的执行，例如：

- 真实 API 请求
- 真实登录
- 真实跳转
- 真实时间
- 浏览器 localStorage
- 第三方库
- 父组件传进来的回调函数

这时就可以用 mock。

---

### 10.1 Mock 回调函数

组件：

```tsx
// src/components/SubmitButton.tsx
type Props = {
  onSubmit: () => void;
};

export function SubmitButton({ onSubmit }: Props) {
  return <button onClick={onSubmit}>提交</button>;
}
```

Vitest 测试：

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';
import { SubmitButton } from './SubmitButton';

describe('SubmitButton', () => {
  test('点击按钮后调用 onSubmit', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(<SubmitButton onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: '提交' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
  });
});
```

Jest 写法只需要把：

```ts
vi.fn()
```

换成：

```ts
jest.fn()
```

---

### 10.2 Vitest 与 Jest Mock 对照

| 功能 | Vitest | Jest |
| --- | --- | --- |
| 创建 mock 函数 | `vi.fn()` | `jest.fn()` |
| mock 模块 | `vi.mock()` | `jest.mock()` |
| 监听对象方法 | `vi.spyOn()` | `jest.spyOn()` |
| 清理 mock 调用记录 | `vi.clearAllMocks()` | `jest.clearAllMocks()` |
| 恢复 mock | `vi.restoreAllMocks()` | `jest.restoreAllMocks()` |

记忆方式：

```text
Vitest 基本是 vi。
Jest 基本是 jest。
```

---

## 11. API 请求测试

假设有一个 API 方法：

```ts
// src/api/userApi.ts
export async function fetchUserName(): Promise<string> {
  const res = await fetch('/api/user');
  const data = await res.json();
  return data.name;
}
```

测试代码：

```ts
// src/api/userApi.test.ts
import { afterEach, describe, expect, test, vi } from 'vitest';
import { fetchUserName } from './userApi';

describe('fetchUserName', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('API 返回用户名', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      json: async () => ({ name: 'Taro' }),
    } as Response);

    const result = await fetchUserName();

    expect(result).toBe('Taro');
  });
});
```

这里没有真的访问 `/api/user`。

而是用 mock 模拟了一个假的返回值：

```ts
{ name: 'Taro' }
```

这样测试更稳定，也不依赖后端服务是否启动。

---

## 12. 异步测试

前端项目里很多逻辑都是异步的，例如：

- API 请求
- setTimeout
- 页面 loading
- 用户输入
- 数据保存

所以测试异步逻辑时，要使用 `async / await`。

---

### 12.1 异步函数测试

```ts
async function getMessage() {
  return 'hello';
}

test('返回 hello', async () => {
  const result = await getMessage();
  expect(result).toBe('hello');
});
```

---

### 12.2 异步页面测试

组件：

```tsx
import { useEffect, useState } from 'react';

export function UserName() {
  const [name, setName] = useState('');

  useEffect(() => {
    setTimeout(() => {
      setName('Taro');
    }, 100);
  }, []);

  return <div>{name ? name : 'loading...'}</div>;
}
```

测试：

```tsx
import { render, screen } from '@testing-library/react';
import { expect, test } from 'vitest';
import { UserName } from './UserName';

test('加载后显示用户名', async () => {
  render(<UserName />);

  expect(screen.getByText('loading...')).toBeInTheDocument();

  expect(await screen.findByText('Taro')).toBeInTheDocument();
});
```

重点：

```tsx
await screen.findByText('Taro')
```

因为 `Taro` 不是一开始就显示，而是异步出现的。

---

## 13. 覆盖率 Coverage

覆盖率表示测试代码覆盖了多少业务代码。

常见指标：

| 指标 | 说明 |
| --- | --- |
| Statements | 语句覆盖率 |
| Branches | 分支覆盖率 |
| Functions | 函数覆盖率 |
| Lines | 行覆盖率 |

运行命令：

```bash
npm run test:coverage
```

可能看到：

```text
Statements   : 85%
Branches     : 70%
Functions    : 90%
Lines        : 86%
```

注意：

```text
覆盖率高，不代表测试质量一定高。
```

例如你只是把组件 render 一下，没有点击按钮、没有输入、没有验证错误提示，覆盖率可能也会上升，但测试价值不一定高。

更好的测试应该验证真实业务行为。

---

## 14. Snapshot 快照测试

Snapshot 快照测试会把组件渲染结果保存下来，下次测试时进行对比。

示例：

```tsx
import { render } from '@testing-library/react';
import { expect, test } from 'vitest';
import { Counter } from './Counter';

test('Counter snapshot', () => {
  const { container } = render(<Counter />);
  expect(container).toMatchSnapshot();
});
```

适合使用 Snapshot 的场景：

- 小型展示组件
- UI 结构稳定的组件
- 需要防止 DOM 结构被意外修改

不适合滥用 Snapshot 的场景：

- 大页面
- 复杂表单
- 内容经常变化的组件
- 生成的 snapshot 太长没人认真看

项目中如果 snapshot 太多，维护成本会变高。

---

## 15. 真实项目中应该测什么

新人刚开始写测试时，容易不知道应该测哪里。

建议优先测试：

1. 工具函数
2. 表单校验
3. 按钮点击行为
4. API 成功后的页面显示
5. API 失败后的错误提示
6. loading 状态
7. 权限控制
8. 金额计算
9. 日期格式化
10. 状态转换逻辑

---

### 15.1 推荐测试的例子

### 表单校验

```text
用户名为空时，点击提交，显示“用户名不能为空”。
```

### 按钮行为

```text
点击保存按钮后，调用 onSave 方法一次。
```

### API 成功

```text
接口返回用户列表后，页面显示用户名称。
```

### API 失败

```text
接口返回错误后，页面显示错误提示。
```

### 权限控制

```text
普通用户看不到删除按钮，管理员可以看到删除按钮。
```

---

### 15.2 不太推荐测试的内容

不建议花太多时间测试：

- React 的 `useState` 本身
- 第三方库内部行为
- 没有业务逻辑的纯静态文字
- className 是否等于某个值，除非和业务强相关
- 实现细节，例如组件内部变量名

错误倾向：

```tsx
expect(container.querySelector('.save-button')).toBeTruthy();
```

更推荐：

```tsx
expect(screen.getByRole('button', { name: '保存' })).toBeInTheDocument();
```

因为用户关心的是页面上有没有“保存按钮”，不是 class 名。

---

## 16. 常见错误与注意点

### 16.1 没有配置 jsdom

错误现象：

```text
document is not defined
window is not defined
```

原因：

测试默认运行在 Node 环境里，没有浏览器 DOM。

解决：

Vitest：

```ts
test: {
  environment: 'jsdom'
}
```

Jest：

```ts
testEnvironment: 'jsdom'
```

---

### 16.2 忘记引入 jest-dom

错误现象：

```text
toBeInTheDocument is not a function
```

原因：

没有加载 `@testing-library/jest-dom`。

解决：

Vitest：

```ts
import '@testing-library/jest-dom/vitest';
```

Jest：

```ts
import '@testing-library/jest-dom';
```

---

### 16.3 异步操作没有 await

错误写法：

```tsx
user.click(button);
expect(screen.getByText('保存成功')).toBeInTheDocument();
```

推荐写法：

```tsx
await user.click(button);
expect(await screen.findByText('保存成功')).toBeInTheDocument();
```

---

### 16.4 过度依赖 testId

不推荐：

```tsx
screen.getByTestId('submit-button');
```

优先推荐：

```tsx
screen.getByRole('button', { name: '提交' });
```

只有在没有更好选择时，再使用 `data-testid`。

---

## 17. 学习路线

建议新人按下面顺序学习：

## 第一阶段：测试基础

- 理解为什么要写测试
- 理解 `describe`
- 理解 `test` / `it`
- 理解 `expect`
- 会测试普通函数

## 第二阶段：Vitest / Jest 基础

- 会安装测试依赖
- 会配置测试命令
- 会运行测试
- 会看测试失败信息
- 会写简单断言

## 第三阶段：React 组件测试

- 会使用 `render`
- 会使用 `screen.getByText`
- 会使用 `screen.getByRole`
- 会使用 `userEvent.click`
- 会测试按钮点击
- 会测试输入框输入

## 第四阶段：Mock

- 会使用 `vi.fn()` 或 `jest.fn()`
- 会判断方法是否被调用
- 会 mock API 请求
- 会清理 mock

## 第五阶段：项目实战

- 测试表单校验
- 测试 loading 状态
- 测试 API 成功 / 失败
- 测试权限显示
- 查看覆盖率
- 在提交代码前运行测试

---

## 18. 面试回答模板

### 18.1 中文回答

```text
前端测试主要分为单体测试、组件测试和 E2E 测试。
我使用过 Jest / Vitest 做单体测试和组件测试。

如果是普通工具函数，我会直接通过 describe、test 和 expect 来验证输入和输出。
如果是 React 组件，我会配合 React Testing Library，先用 render 渲染组件，
然后通过 screen.getByRole、getByText、findByText 等方法获取页面元素，
再使用 user-event 模拟用户点击和输入，最后通过 expect 判断页面显示或回调函数是否符合预期。

如果测试中涉及 API 请求，我一般会使用 mock 来模拟接口返回，避免测试依赖真实后端环境。
```

---

### 18.2 日语回答

```text
フロントエンドのテストでは、主に単体テストとコンポーネントテストを行いました。
テストツールとしては Jest や Vitest を使用しました。

通常の関数であれば、describe、test、expect を使って入力値と戻り値を検証します。
React コンポーネントの場合は、React Testing Library を利用してコンポーネントを render し、
screen.getByRole や getByText で要素を取得します。
その後、user-event でクリックや入力操作を再現し、最後に expect で期待結果を確認します。

API 呼び出しがある場合は、mock を使ってレスポンスを再現し、
実際のバックエンド環境に依存しないようにテストを作成しました。
```

---

## 19. 练习题

## 练习 1：测试普通函数

业务代码：

```ts
export function isAdult(age: number): boolean {
  return age >= 18;
}
```

要求：

1. 年龄为 18 时返回 `true`
2. 年龄为 20 时返回 `true`
3. 年龄为 17 时返回 `false`

---

## 练习 2：测试金额格式化

业务代码：

```ts
export function formatPrice(price: number): string {
  return `${price.toLocaleString()}円`;
}
```

要求：

1. `1000` 返回 `1,000円`
2. `25000` 返回 `25,000円`

---

## 练习 3：测试按钮点击

组件：

```tsx
type Props = {
  onClick: () => void;
};

export function SaveButton({ onClick }: Props) {
  return <button onClick={onClick}>保存</button>;
}
```

要求：

1. 页面显示“保存”按钮
2. 点击按钮后，`onClick` 被调用一次

---

## 练习 4：测试输入框

组件：

```tsx
export function NameInput() {
  return (
    <div>
      <label htmlFor="name">姓名</label>
      <input id="name" />
    </div>
  );
}
```

要求：

1. 通过 label 找到输入框
2. 输入 `Taro`
3. 判断输入框的值是 `Taro`

---

## 练习 5：测试条件渲染

组件：

```tsx
type Props = {
  error?: string;
};

export function ErrorMessage({ error }: Props) {
  if (!error) return null;
  return <p>{error}</p>;
}
```

要求：

1. 有 error 时显示错误信息
2. 没有 error 时不显示错误信息

---

## 总结

Vitest 和 Jest 都是前端测试中非常重要的工具。

对于新人来说，先不要急着学很复杂的测试架构，先掌握下面几个核心点：

```text
1. describe 是测试分组
2. test 是测试用例
3. expect 是断言
4. render 用来渲染 React 组件
5. screen 用来查找页面元素
6. userEvent 用来模拟用户操作
7. mock 用来模拟函数、API、外部依赖
```

真正好的前端测试，不是为了追求覆盖率数字，而是为了验证业务行为。

最重要的一句话：

```text
前端测试应该尽量接近用户真实使用页面的方式。
```

