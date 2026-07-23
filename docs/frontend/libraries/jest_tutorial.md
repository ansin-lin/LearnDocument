
# Jest 配置顺序详细说明

本章节以 **React + Vite + TypeScript + Jest + React Testing Library** 为例，说明：

- Jest 需要安装哪些包
- 需要创建哪些文件
- 每个文件负责什么
- 在哪些文件中如何导入
- 如何让 `.test.ts` / `.test.tsx` 文件生效
- Jest 和 Vitest 配置上的区别
- 测试文件不生效时如何排查

---

## 一、Jest 的整体执行流程

Jest 的执行顺序可以这样理解：

```text
package.json 命令
        ↓
执行 jest
        ↓
读取 jest.config.ts / jest.config.js
        ↓
根据 transform 配置处理 TS / TSX / JSX
        ↓
加载 jest.setup.ts
        ↓
根据 testMatch / testRegex 扫描测试文件
        ↓
执行测试文件里的 describe / test / expect
```

和 Vitest 不同的是：

```text
Vitest：通常读取 vite.config.ts
Jest：通常读取 jest.config.ts / jest.config.js
```

所以在 Vite 项目中使用 Jest 时，不能以为配置了 `vite.config.ts`，Jest 就会自动识别全部配置。

---

## 二、推荐项目结构

以 React + Vite + TypeScript 项目为例：

```text
my-app/
├─ package.json
├─ vite.config.ts
├─ jest.config.ts
├─ babel.config.cjs
├─ tsconfig.json
├─ src/
│  ├─ test/
│  │  └─ jest.setup.ts
│  ├─ utils/
│  │  ├─ sum.ts
│  │  └─ sum.test.ts
│  ├─ components/
│  │  ├─ Button.tsx
│  │  ├─ Button.css
│  │  └─ Button.test.tsx
│  └─ main.tsx
```

重点文件如下：

| 文件 | 作用 |
| --- | --- |
| `package.json` | 定义 Jest 测试命令 |
| `jest.config.ts` | Jest 的主配置文件 |
| `babel.config.cjs` | 让 Jest 能处理 TypeScript / JSX / TSX |
| `src/test/jest.setup.ts` | 测试启动前的共通设定 |
| `*.test.ts / *.test.tsx` | 真正的测试文件 |
| `tsconfig.json` | 让 TypeScript 认识 Jest 类型 |

---

## 三、第一步：安装依赖

React + TypeScript 项目中，Jest 常用安装如下：

```bash
npm install -D jest jest-environment-jsdom @types/jest
```

React Testing Library 相关：

```bash
npm install -D @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

Babel 转换 TypeScript / React 相关：

```bash
npm install -D babel-jest @babel/core @babel/preset-env @babel/preset-react @babel/preset-typescript
```

如果使用 `jest.config.ts`，还建议安装：

```bash
npm install -D ts-node
```

如果组件中导入了 CSS，建议安装：

```bash
npm install -D identity-obj-proxy
```

完整安装命令可以写成：

```bash
npm install -D jest jest-environment-jsdom @types/jest \
@testing-library/react @testing-library/jest-dom @testing-library/user-event \
babel-jest @babel/core @babel/preset-env @babel/preset-react @babel/preset-typescript \
ts-node identity-obj-proxy
```

Windows PowerShell 中可以写成一行：

```bash
npm install -D jest jest-environment-jsdom @types/jest @testing-library/react @testing-library/jest-dom @testing-library/user-event babel-jest @babel/core @babel/preset-env @babel/preset-react @babel/preset-typescript ts-node identity-obj-proxy
```

---

## 四、依赖包作用说明

| 包 | 作用 |
| --- | --- |
| `jest` | Jest 测试框架本体 |
| `jest-environment-jsdom` | 提供浏览器 DOM 模拟环境 |
| `@types/jest` | 让 TypeScript 认识 `describe`、`test`、`expect` |
| `@testing-library/react` | 测试 React 组件 |
| `@testing-library/jest-dom` | 提供 DOM 断言，例如 `toBeInTheDocument()` |
| `@testing-library/user-event` | 模拟用户点击、输入等操作 |
| `babel-jest` | 让 Jest 通过 Babel 转换代码 |
| `@babel/core` | Babel 本体 |
| `@babel/preset-env` | 转换现代 JavaScript |
| `@babel/preset-react` | 转换 JSX |
| `@babel/preset-typescript` | 转换 TypeScript |
| `ts-node` | 让 Jest 可以读取 `jest.config.ts` |
| `identity-obj-proxy` | 处理测试中导入 CSS Module 的情况 |

---

## 五、第二步：配置 package.json

打开 `package.json`，添加：

```json
{
  "scripts": {
    "test:jest": "jest --watch",
    "test:jest:run": "jest",
    "test:jest:coverage": "jest --coverage"
  }
}
```

如果项目只使用 Jest，不使用 Vitest，也可以写成：

```json
{
  "scripts": {
    "test": "jest --watch",
    "test:run": "jest",
    "test:coverage": "jest --coverage"
  }
}
```

如果项目中已经有 Vitest，建议避免命令冲突，可以使用：

```json
{
  "scripts": {
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage",
    "test:jest": "jest --watch",
    "test:jest:run": "jest",
    "test:jest:coverage": "jest --coverage"
  }
}
```

---

## 六、三个 Jest 命令说明

### 1. npm run test:jest

```bash
npm run test:jest
```

对应：

```bash
jest --watch
```

作用：

```text
进入监听模式，适合开发中使用。
```

特点：

- 不会执行完就退出
- 会监听文件变化
- 修改代码后可以自动重新执行相关测试
- 适合一边开发一边测试

---

### 2. npm run test:jest:run

```bash
npm run test:jest:run
```

对应：

```bash
jest
```

作用：

```text
只执行一次测试，执行完自动退出。
```

适合场景：

```text
提交代码前检查
合并代码前检查
CI / CD 自动化测试
```

---

### 3. npm run test:jest:coverage

```bash
npm run test:jest:coverage
```

对应：

```bash
jest --coverage
```

作用：

```text
执行测试，并生成覆盖率报告。
```

覆盖率通常会生成到：

```text
coverage/
```

常见入口文件：

```text
coverage/lcov-report/index.html
```

可以用浏览器打开查看哪些代码没有测试到。

---

## 七、第三步：创建 jest.config.ts

创建文件：

```text
jest.config.ts
```

推荐配置如下：

```ts
import type { Config } from 'jest';

const config: Config = {
  testEnvironment: 'jsdom',

  setupFilesAfterEnv: ['<rootDir>/src/test/jest.setup.ts'],

  testMatch: [
    '<rootDir>/src/**/*.(test|spec).(ts|tsx|js|jsx)',
  ],

  transform: {
    '^.+\\.(ts|tsx|js|jsx)$': 'babel-jest',
  },

  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|webp|svg)$': '<rootDir>/src/test/fileMock.ts',
    '^@/(.*)$': '<rootDir>/src/$1',
  },

  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/main.tsx',
    '!src/vite-env.d.ts',
  ],
};

export default config;
```

注意：如果你没有使用 `@/` 路径别名，可以删除这一行：

```ts
'^@/(.*)$': '<rootDir>/src/$1',
```

如果你没有图片导入，也可以先不配置图片 mock。

---

## 八、jest.config.ts 配置项详细解释

### 1. testEnvironment

```ts
testEnvironment: 'jsdom',
```

作用：

```text
让 Jest 在类似浏览器的 DOM 环境中运行测试。
```

React 组件测试通常需要这个配置。

如果没有配置，可能会报：

```text
ReferenceError: document is not defined
```

---

### 2. setupFilesAfterEnv

```ts
setupFilesAfterEnv: ['<rootDir>/src/test/jest.setup.ts'],
```

作用：

```text
在测试框架初始化之后、每个测试文件执行之前，加载共通 setup 文件。
```

通常用于统一导入：

```ts
import '@testing-library/jest-dom';
```

这样测试文件中就可以使用：

```ts
expect(button).toBeInTheDocument();
expect(button).toBeDisabled();
expect(button).toBeVisible();
```

---

### 3. testMatch

```ts
testMatch: [
  '<rootDir>/src/**/*.(test|spec).(ts|tsx|js|jsx)',
],
```

作用：

```text
告诉 Jest 哪些文件是测试文件。
```

这个配置会识别：

```text
sum.test.ts
sum.spec.ts
Button.test.tsx
Button.spec.tsx
user.test.js
user.spec.jsx
```

如果你的测试文件不在 `src` 下面，而是在 `__tests__` 目录中，可以改成：

```ts
testMatch: [
  '<rootDir>/src/**/*.(test|spec).(ts|tsx|js|jsx)',
  '<rootDir>/__tests__/**/*.(test|spec).(ts|tsx|js|jsx)',
],
```

---

### 4. transform

```ts
transform: {
  '^.+\\.(ts|tsx|js|jsx)$': 'babel-jest',
},
```

作用：

```text
让 Jest 使用 babel-jest 转换 TS / TSX / JS / JSX 文件。
```

Jest 本身不能直接理解所有 TypeScript / JSX 写法，所以需要转换。

---

### 5. moduleNameMapper：CSS 处理

```ts
moduleNameMapper: {
  '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
},
```

作用：

```text
让 Jest 测试时可以处理 CSS 导入。
```

例如组件里有：

```ts
import './Button.css';
```

或者：

```ts
import styles from './Button.module.css';
```

Jest 默认不理解 CSS 文件，所以需要通过 `moduleNameMapper` 映射掉。

---

### 6. moduleNameMapper：图片处理

```ts
'\\.(jpg|jpeg|png|gif|webp|svg)$': '<rootDir>/src/test/fileMock.ts',
```

作用：

```text
处理测试中导入图片、SVG 等静态资源的情况。
```

例如组件中有：

```tsx
import logo from './logo.svg';
```

Jest 默认也不理解这些资源文件，所以需要 mock。

---

### 7. moduleNameMapper：路径别名

```ts
'^@/(.*)$': '<rootDir>/src/$1',
```

作用：

```text
让 Jest 识别项目中的 @ 路径别名。
```

例如业务代码中使用：

```ts
import { sum } from '@/utils/sum';
```

那么 Jest 也需要知道：

```text
@/utils/sum 等于 src/utils/sum
```

否则测试时可能会报：

```text
Cannot find module '@/utils/sum'
```

---

### 8. collectCoverageFrom

```ts
collectCoverageFrom: [
  'src/**/*.{ts,tsx}',
  '!src/**/*.d.ts',
  '!src/main.tsx',
  '!src/vite-env.d.ts',
],
```

作用：

```text
指定哪些文件要纳入覆盖率统计。
```

说明：

| 配置 | 含义 |
| --- | --- |
| `src/**/*.{ts,tsx}` | 统计 src 下所有 ts / tsx 文件 |
| `!src/**/*.d.ts` | 排除类型声明文件 |
| `!src/main.tsx` | 排除应用入口文件 |
| `!src/vite-env.d.ts` | 排除 Vite 类型声明文件 |

---

## 九、第四步：创建 babel.config.cjs

创建文件：

```text
babel.config.cjs
```

内容：

```js
module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' } }],
    ['@babel/preset-react', { runtime: 'automatic' }],
    '@babel/preset-typescript',
  ],
};
```

作用：

```text
告诉 Babel 如何转换现代 JavaScript、React JSX、TypeScript。
```

逐个解释：

```js
['@babel/preset-env', { targets: { node: 'current' } }]
```

让代码转换为当前 Node.js 环境能执行的语法。

```js
['@babel/preset-react', { runtime: 'automatic' }]
```

让 Babel 能处理 JSX / TSX。

`runtime: 'automatic'` 表示 React 17+ 的新 JSX 转换方式，不需要每个文件都手动：

```tsx
import React from 'react';
```

```js
'@babel/preset-typescript'
```

让 Babel 能处理 TypeScript 语法。

注意：

```text
Babel 只负责转换 TypeScript，不负责类型检查。
```

所以实际项目中，建议提交前同时执行：

```bash
npm run typecheck
```

例如：

```json
{
  "scripts": {
    "typecheck": "tsc --noEmit"
  }
}
```

---

## 十、第五步：创建 jest.setup.ts

创建文件：

```text
src/test/jest.setup.ts
```

内容：

```ts
import '@testing-library/jest-dom';
```

作用：

```text
扩展 Jest 的 DOM 断言能力。
```

导入后可以使用：

```ts
expect(element).toBeInTheDocument();
expect(element).toBeVisible();
expect(button).toBeDisabled();
expect(input).toHaveValue('abc');
```

注意和 Vitest 的区别：

Vitest 中一般写：

```ts
import '@testing-library/jest-dom/vitest';
```

Jest 中一般写：

```ts
import '@testing-library/jest-dom';
```

---

## 十一、第六步：创建 fileMock.ts

如果你在 `jest.config.ts` 中配置了：

```ts
'\\.(jpg|jpeg|png|gif|webp|svg)$': '<rootDir>/src/test/fileMock.ts',
```

就需要创建文件：

```text
src/test/fileMock.ts
```

内容：

```ts
export default 'test-file-stub';
```

作用：

```text
测试时把图片、SVG 等资源替换成一个普通字符串。
```

例如业务代码：

```tsx
import logo from './logo.svg';

export function Header() {
  return <img src={logo} alt="logo" />;
}
```

测试时 Jest 不会真的读取 SVG 文件内容，而是把它当成：

```text
test-file-stub
```

---

## 十二、第七步：配置 tsconfig.json

如果使用 TypeScript，建议在 `tsconfig.json` 中加入 Jest 类型。

示例：

```json
{
  "compilerOptions": {
    "types": [
      "jest",
      "@testing-library/jest-dom"
    ]
  },
  "include": [
    "src",
    "src/test/jest.setup.ts",
    "jest.config.ts"
  ]
}
```

作用：

```text
让 TypeScript 认识 Jest 的全局变量和 jest-dom 的断言类型。
```

否则可能出现：

```text
Cannot find name 'describe'
Cannot find name 'test'
Cannot find name 'expect'
Property 'toBeInTheDocument' does not exist
```

如果你的项目同时使用 Vitest 和 Jest，不建议把所有测试类型都混在同一个 `tsconfig.json` 中，否则可能出现类型冲突。

可以考虑分开：

```text
tsconfig.json
tsconfig.vitest.json
tsconfig.jest.json
```

教学项目中为了简单，可以先放在同一个 `tsconfig.json` 中。

---

## 十三、第八步：创建被测试代码

创建文件：

```text
src/utils/sum.ts
```

内容：

```ts
export function sum(a: number, b: number) {
  return a + b;
}
```

注意：

```text
被测试的函数需要 export。
```

因为测试文件要导入它：

```ts
import { sum } from './sum';
```

---

## 十四、第九步：创建测试文件

创建文件：

```text
src/utils/sum.test.ts
```

内容：

```ts
import { sum } from './sum';

describe('sum', () => {
  test('两个数字相加', () => {
    expect(sum(1, 2)).toBe(3);
  });
});
```

Jest 默认提供全局的：

```text
describe
test
it
expect
jest
```

所以测试文件里一般不需要导入：

```ts
import { describe, test, expect } from '@jest/globals';
```

但是如果团队要求显式导入，也可以写成：

```ts
import { describe, expect, test } from '@jest/globals';
import { sum } from './sum';

describe('sum', () => {
  test('两个数字相加', () => {
    expect(sum(1, 2)).toBe(3);
  });
});
```

---

## 十五、React 组件测试完整例子

### 1. 创建组件

文件：

```text
src/components/Button.tsx
```

内容：

```tsx
type ButtonProps = {
  label: string;
  disabled?: boolean;
  onClick?: () => void;
};

export function Button({ label, disabled = false, onClick }: ButtonProps) {
  return (
    <button disabled={disabled} onClick={onClick}>
      {label}
    </button>
  );
}
```

---

### 2. 创建测试文件

文件：

```text
src/components/Button.test.tsx
```

内容：

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from './Button';

describe('Button', () => {
  test('按钮文字能够显示', () => {
    render(<Button label="保存" />);

    expect(screen.getByText('保存')).toBeInTheDocument();
  });

  test('disabled 为 true 时，按钮不可点击', () => {
    render(<Button label="保存" disabled />);

    expect(screen.getByText('保存')).toBeDisabled();
  });

  test('点击按钮时，调用 onClick', async () => {
    const user = userEvent.setup();
    const handleClick = jest.fn();

    render(<Button label="保存" onClick={handleClick} />);

    await user.click(screen.getByText('保存'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

---

## 十六、测试文件中的 import 说明

### 1. render / screen

```ts
import { render, screen } from '@testing-library/react';
```

作用：

```text
render：把 React 组件渲染到测试环境中
screen：从页面中查找元素
```

例如：

```tsx
render(<Button label="保存" />);
expect(screen.getByText('保存')).toBeInTheDocument();
```

---

### 2. userEvent

```ts
import userEvent from '@testing-library/user-event';
```

作用：

```text
模拟用户操作。
```

例如：

```ts
await user.click(button);
await user.type(input, 'test');
```

---

### 3. jest.fn()

```ts
const handleClick = jest.fn();
```

作用：

```text
创建一个 mock 函数。
```

可以检查函数有没有被调用：

```ts
expect(handleClick).toHaveBeenCalledTimes(1);
```

Vitest 中对应的是：

```ts
const handleClick = vi.fn();
```

Jest 中对应的是：

```ts
const handleClick = jest.fn();
```

---

### 4. 被测试组件

```ts
import { Button } from './Button';
```

作用：

```text
导入要测试的业务组件。
```

---

## 十七、测试文件为什么会生效？

Jest 会根据 `jest.config.ts` 中的 `testMatch` 找测试文件。

例如：

```ts
testMatch: [
  '<rootDir>/src/**/*.(test|spec).(ts|tsx|js|jsx)',
],
```

会识别：

```text
src/utils/sum.test.ts
src/utils/sum.spec.ts
src/components/Button.test.tsx
src/components/Button.spec.tsx
```

不会识别：

```text
src/utils/sum.ts
src/utils/sum-test.ts
src/utils/sum_test.ts
src/components/Button.check.tsx
```

所以推荐命名：

```text
xxx.test.ts
xxx.test.tsx
xxx.spec.ts
xxx.spec.tsx
```

---

## 十八、测试文件是否需要在 main.tsx 中导入？

不需要。

不要这样写：

```ts
import './components/Button.test';
```

正确理解：

```text
业务代码由 main.tsx 启动。
测试代码由 jest 命令启动。
```

Jest 会自动扫描测试文件并执行。

测试文件生效的条件：

```text
1. package.json 中有 Jest 命令
2. jest.config.ts 配置正确
3. 测试文件名符合 testMatch 规则
4. 测试文件没有被 testPathIgnorePatterns 排除
5. 执行了 npm run test:jest 或 npm run test:jest:run
```

---

## 十九、Jest 和 Vitest 配置上的主要区别

| 对比项 | Vitest | Jest |
| --- | --- | --- |
| 主配置文件 | `vite.config.ts` 或 `vitest.config.ts` | `jest.config.ts` 或 `jest.config.js` |
| 是否复用 Vite 配置 | 通常可以 | 通常不直接复用 |
| 浏览器环境配置 | `environment: 'jsdom'` | `testEnvironment: 'jsdom'` |
| setup 配置 | `setupFiles` | `setupFilesAfterEnv` |
| mock 函数 | `vi.fn()` | `jest.fn()` |
| 测试命令 | `vitest` / `vitest run` | `jest --watch` / `jest` |
| TS/TSX 转换 | Vite/esbuild 体系 | 通常用 `babel-jest` 或 `ts-jest` |
| 覆盖率命令 | `vitest run --coverage` | `jest --coverage` |

---

## 二十、Jest 中 Babel 和 ts-jest 怎么选？

Jest 测试 TypeScript 常见有两种方式：

```text
方式一：babel-jest + @babel/preset-typescript
方式二：ts-jest
```

### 方式一：babel-jest

优点：

```text
速度较快
适合 React / Vite / Babel 项目
配置和前端项目比较接近
```

缺点：

```text
只转换 TypeScript，不做类型检查
```

所以需要另外执行：

```bash
tsc --noEmit
```

---

### 方式二：ts-jest

优点：

```text
更接近 TypeScript 编译器
可以结合 TypeScript 类型检查
```

缺点：

```text
配置可能更复杂
速度可能更慢
```

教学和普通 React 组件测试中，推荐先学习：

```text
babel-jest + @babel/preset-typescript
```

因为更容易理解整体流程。

---

## 二十一、Jest 测试文件不生效时的排查方法

### 1. 检查 package.json 命令

确认有：

```json
{
  "scripts": {
    "test:jest": "jest --watch",
    "test:jest:run": "jest"
  }
}
```

执行：

```bash
npm run test:jest
```

或者：

```bash
npm run test:jest:run
```

---

### 2. 检查 jest.config.ts 是否存在

确认项目根目录有：

```text
jest.config.ts
```

不是放在：

```text
src/jest.config.ts
```

一般应该放项目根目录。

---

### 3. 检查 testMatch 是否能匹配测试文件

如果配置：

```ts
testMatch: [
  '<rootDir>/src/**/*.(test|spec).(ts|tsx|js|jsx)',
],
```

那么测试文件要放在：

```text
src/
```

下面，并且文件名要类似：

```text
Button.test.tsx
sum.test.ts
```

---

### 4. 检查 transform 是否配置

如果报错：

```text
Jest encountered an unexpected token
```

或者：

```text
SyntaxError: Cannot use import statement outside a module
```

可能是没有正确配置 Babel 转换。

确认有：

```ts
transform: {
  '^.+\\.(ts|tsx|js|jsx)$': 'babel-jest',
},
```

并且有：

```text
babel.config.cjs
```

---

### 5. 检查 jsdom 是否安装和配置

如果报错：

```text
ReferenceError: document is not defined
```

确认安装：

```bash
npm install -D jest-environment-jsdom
```

并且配置：

```ts
testEnvironment: 'jsdom',
```

---

### 6. 检查 jest.setup.ts 是否被加载

如果报错：

```text
Property 'toBeInTheDocument' does not exist
```

或者运行时报：

```text
expect(...).toBeInTheDocument is not a function
```

确认配置：

```ts
setupFilesAfterEnv: ['<rootDir>/src/test/jest.setup.ts'],
```

并且文件内容是：

```ts
import '@testing-library/jest-dom';
```

---

### 7. 检查 CSS / 图片导入

如果报错：

```text
Cannot find module './Button.css'
```

或者：

```text
Unexpected token '.'
```

确认配置：

```ts
moduleNameMapper: {
  '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
  '\\.(jpg|jpeg|png|gif|webp|svg)$': '<rootDir>/src/test/fileMock.ts',
},
```

并且安装：

```bash
npm install -D identity-obj-proxy
```

---

### 8. 检查路径别名

如果报错：

```text
Cannot find module '@/utils/sum'
```

确认配置：

```ts
moduleNameMapper: {
  '^@/(.*)$': '<rootDir>/src/$1',
},
```

---

## 二十二、完整最小配置汇总

### package.json

```json
{
  "scripts": {
    "test:jest": "jest --watch",
    "test:jest:run": "jest",
    "test:jest:coverage": "jest --coverage"
  }
}
```

---

### jest.config.ts

```ts
import type { Config } from 'jest';

const config: Config = {
  testEnvironment: 'jsdom',

  setupFilesAfterEnv: ['<rootDir>/src/test/jest.setup.ts'],

  testMatch: [
    '<rootDir>/src/**/*.(test|spec).(ts|tsx|js|jsx)',
  ],

  transform: {
    '^.+\\.(ts|tsx|js|jsx)$': 'babel-jest',
  },

  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|webp|svg)$': '<rootDir>/src/test/fileMock.ts',
    '^@/(.*)$': '<rootDir>/src/$1',
  },

  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/main.tsx',
    '!src/vite-env.d.ts',
  ],
};

export default config;
```

---

### babel.config.cjs

```js
module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' } }],
    ['@babel/preset-react', { runtime: 'automatic' }],
    '@babel/preset-typescript',
  ],
};
```

---

### src/test/jest.setup.ts

```ts
import '@testing-library/jest-dom';
```

---

### src/test/fileMock.ts

```ts
export default 'test-file-stub';
```

---

### tsconfig.json

```json
{
  "compilerOptions": {
    "types": [
      "jest",
      "@testing-library/jest-dom"
    ]
  },
  "include": [
    "src",
    "src/test/jest.setup.ts",
    "jest.config.ts"
  ]
}
```

---

### src/utils/sum.ts

```ts
export function sum(a: number, b: number) {
  return a + b;
}
```

---

### src/utils/sum.test.ts

```ts
import { sum } from './sum';

describe('sum', () => {
  test('两个数字相加', () => {
    expect(sum(1, 2)).toBe(3);
  });
});
```

---

## 二十三、Jest 最终记忆版

Jest 配置顺序：

```text
1. 安装 jest、jest-environment-jsdom、@types/jest
2. 安装 React Testing Library 相关包
3. 安装 babel-jest 和 Babel presets
4. 配置 package.json 测试命令
5. 创建 jest.config.ts
6. 创建 babel.config.cjs
7. 创建 src/test/jest.setup.ts
8. 如有图片导入，创建 src/test/fileMock.ts
9. 配置 tsconfig.json 类型
10. 创建业务代码
11. 创建 xxx.test.ts / xxx.test.tsx
12. 执行 npm run test:jest 或 npm run test:jest:run
```

测试文件生效的关键条件：

```text
1. package.json 中有 Jest 命令
2. jest.config.ts 配置正确
3. transform 能处理 TS / TSX
4. setupFilesAfterEnv 路径正确
5. 测试文件名符合 testMatch 规则
6. 测试文件没有被排除
7. 执行了 Jest 命令
```

一句话总结：

```text
Jest 不会自动读取 Vite 的全部配置。
在 Vite + React + TypeScript 项目中使用 Jest，通常需要单独配置 jest.config.ts 和 babel.config.cjs。
```

---

## 二十四、Vitest 和 Jest 选择建议

如果是 Vite + React 新项目：

```text
优先推荐 Vitest。
```

原因：

```text
配置更简单
和 Vite 集成更自然
速度通常更快
学习成本较低
```

如果是老项目、CRA 项目、公司已有 Jest 规范：

```text
继续使用 Jest。
```

原因：

```text
生态成熟
老项目使用多
很多公司已有 Jest 测试基盘
资料和案例多
```

简单记忆：

```text
Vite 新项目：Vitest 更顺手
老 React 项目：Jest 更常见
公司已有规范：按项目规范来
```
