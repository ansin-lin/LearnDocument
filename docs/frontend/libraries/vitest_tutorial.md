# Vitest 配置顺序详细说明

本文档以 **React + Vite + TypeScript + Vitest** 项目为例，说明：

- Vitest 需要安装哪些包
- 需要创建哪些文件
- 每个文件负责什么
- 在哪些文件中如何导入
- 测试文件为什么会生效
- 测试文件不生效时如何排查

---

## 一、Vitest 的整体执行流程

Vitest 的执行顺序可以这样理解：

```text
package.json 命令
        ↓
执行 vitest
        ↓
读取 vite.config.ts 或 vitest.config.ts
        ↓
加载 test.setup.ts
        ↓
自动扫描 *.test.ts / *.spec.ts 文件
        ↓
执行测试文件里的 describe / test / expect
```

也就是说，测试文件**不需要在业务代码里手动导入**，Vitest 会根据文件命名规则自动扫描并执行。

---

## 二、推荐项目结构

以 React + Vite + TypeScript 为例：

```text
my-app/
├─ package.json
├─ vite.config.ts
├─ tsconfig.json
├─ src/
│  ├─ test/
│  │  └─ setup.ts
│  ├─ utils/
│  │  ├─ sum.ts
│  │  └─ sum.test.ts
│  ├─ components/
│  │  ├─ Button.tsx
│  │  └─ Button.test.tsx
│  └─ main.tsx
```

重点文件如下：

| 文件 | 作用 |
| --- | --- |
| `package.json` | 定义测试命令 |
| `vite.config.ts` | 配置 Vitest |
| `src/test/setup.ts` | 测试启动前的共通设定 |
| `*.test.ts / *.test.tsx` | 真正的测试文件 |
| `tsconfig.json` | 让 TypeScript 认识 Vitest 类型 |

---

## 三、第一步：安装依赖

React 项目常用安装命令：

```bash
npm install -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event @vitest/coverage-v8
```

各依赖作用如下：

| 包 | 作用 |
| --- | --- |
| `vitest` | 测试框架本体 |
| `jsdom` | 模拟浏览器 DOM 环境 |
| `@testing-library/react` | 测试 React 组件 |
| `@testing-library/jest-dom` | 提供 `toBeInTheDocument()` 等 DOM 断言 |
| `@testing-library/user-event` | 模拟用户点击、输入 |
| `@vitest/coverage-v8` | 生成测试覆盖率 |

### 为什么需要 jsdom？

Vitest 默认测试环境是 `node`。

Node 环境中没有：

```text
document
window
localStorage
HTMLElement
```

但是 React 组件测试经常需要这些浏览器对象。

如果没有配置 `jsdom`，测试 React 组件时可能会报错：

```text
ReferenceError: document is not defined
```

---

## 四、第二步：配置 package.json

打开 `package.json`，添加：

```json
{
  "scripts": {
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage"
  }
}
```

### 1. npm run test

```bash
npm run test
```

作用：

```text
进入监听模式，适合开发中使用。
```

特点：

- 不会执行完就退出
- 会一直监听文件变化
- 修改代码或测试文件后，会自动重新执行相关测试

适合场景：

```text
一边写代码，一边写测试。
```

---

### 2. npm run test:run

```bash
npm run test:run
```

作用：

```text
只执行一次测试，执行完自动退出。
```

特点：

- 只跑一次
- 不进入监听模式
- 执行完会返回成功或失败结果

适合场景：

```text
提交代码前确认
CI / CD 自动化测试
合并代码前检查
```

---

### 3. npm run test:coverage

```bash
npm run test:coverage
```

作用：

```text
执行测试，并生成覆盖率报告。
```

覆盖率可以看到：

| 指标 | 含义 |
| --- | --- |
| Statements | 语句覆盖率 |
| Branches | 分支覆盖率 |
| Functions | 函数覆盖率 |
| Lines | 行覆盖率 |

---

## 五、第三步：配置 vite.config.ts

如果项目已有 `vite.config.ts`，直接在里面添加 `test` 配置。

推荐写法：

```ts
/// <reference types="vitest/config" />

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],

  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
});
```

---

## 六、vite.config.ts 配置项详细解释

### 1. reference types

```ts
/// <reference types="vitest/config" />
```

作用：

```text
让 TypeScript 认识 vite.config.ts 中的 test 配置。
```

否则可能会出现：

```text
Object literal may only specify known properties, and 'test' does not exist
```

---

### 2. globals: true

```ts
globals: true
```

作用：

```text
允许在测试文件中直接使用 describe、test、expect。
```

配置后可以这样写：

```ts
describe('sum', () => {
  test('两个数字相加', () => {
    expect(1 + 2).toBe(3);
  });
});
```

不需要每个测试文件都写：

```ts
import { describe, test, expect } from 'vitest';
```

---

### 3. environment: 'jsdom'

```ts
environment: 'jsdom'
```

作用：

```text
让测试环境模拟浏览器环境。
```

React 组件测试通常需要这个配置。

否则测试组件时可能会报：

```text
ReferenceError: document is not defined
```

---

### 4. setupFiles

```ts
setupFiles: './src/test/setup.ts'
```

作用：

```text
在每个测试文件执行前，先执行 setup.ts。
```

通常用于统一导入测试扩展，例如：

```ts
import '@testing-library/jest-dom/vitest';
```

这样所有测试文件都可以使用：

```ts
expect(element).toBeInTheDocument();
expect(element).toBeVisible();
expect(element).toBeDisabled();
```

---

### 5. css: true

```ts
css: true
```

作用：

```text
让测试时可以处理组件中导入的 CSS 文件。
```

例如组件中有：

```ts
import './Button.css';
```

如果测试时 CSS 导入出问题，可以加上这个配置。

---

## 七、第四步：创建 setup 文件

创建文件：

```text
src/test/setup.ts
```

内容：

```ts
import '@testing-library/jest-dom/vitest';
```

这个文件的作用是：

```text
给 Vitest 增加 DOM 相关断言能力。
```

比如：

```ts
expect(button).toBeInTheDocument();
expect(button).toBeDisabled();
expect(button).toHaveTextContent('保存');
```

如果不导入 `@testing-library/jest-dom/vitest`，这些方法可能会报错。

---

## 八、第五步：配置 tsconfig.json

如果使用了：

```ts
globals: true
```

建议在 `tsconfig.json` 中配置类型。

```json
{
  "compilerOptions": {
    "types": [
      "vitest/globals",
      "vitest/jsdom",
      "@testing-library/jest-dom"
    ]
  },
  "include": [
    "src",
    "src/test/setup.ts"
  ]
}
```

作用：

```text
让 TypeScript 认识 Vitest 和 jest-dom 的类型。
```

否则可能出现：

```text
Cannot find name 'describe'
Cannot find name 'test'
Cannot find name 'expect'
Property 'toBeInTheDocument' does not exist
```

---

## 九、第六步：创建被测试代码

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

因为测试文件需要导入它：

```ts
import { sum } from './sum';
```

---

## 十、第七步：创建测试文件

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

因为前面配置了：

```ts
globals: true
```

所以这里可以不写：

```ts
import { describe, test, expect } from 'vitest';
```

---

## 十一、测试文件为什么会自动生效？

Vitest 默认会自动扫描文件名中包含：

```text
.test.
.spec.
```

的文件。

默认可以识别这些文件：

```text
sum.test.ts
sum.spec.ts
Button.test.tsx
Button.spec.tsx
user.test.js
user.spec.jsx
```

不能默认识别这些文件：

```text
sum.ts
sum-test.ts
sum_test.ts
test-sum.ts
Button.check.tsx
```

所以建议测试文件命名统一为：

```text
xxx.test.ts
xxx.test.tsx
xxx.spec.ts
xxx.spec.tsx
```

---

## 十二、React 组件测试完整例子

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
import { vi } from 'vitest';
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
    const handleClick = vi.fn();

    render(<Button label="保存" onClick={handleClick} />);

    await user.click(screen.getByText('保存'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

---

## 十三、测试文件中的 import 说明

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

```ts
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

### 3. vi

```ts
import { vi } from 'vitest';
```

作用：

```text
Vitest 提供的 mock 工具。
```

例如：

```ts
const handleClick = vi.fn();
```

可以检查函数有没有被调用：

```ts
expect(handleClick).toHaveBeenCalledTimes(1);
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

## 十四、测试文件是否需要在 main.tsx 里导入？

不需要。

不要这样写：

```ts
import './components/Button.test';
```

正确理解：

```text
业务代码由 main.tsx 启动。
测试代码由 vitest 命令启动。
```

也就是说：

```text
main.tsx 不负责执行测试。
Vitest 会自动扫描并执行测试文件。
```

测试文件只需要满足：

```text
1. 文件名符合 *.test.ts / *.spec.ts / *.test.tsx / *.spec.tsx
2. 文件在项目目录下
3. 没有被 exclude 排除
4. 执行了 npm run test
```

---

## 十五、不使用 globals 的写法

如果不想配置：

```ts
globals: true
```

那么每个测试文件都需要手动导入：

```ts
import { describe, test, expect } from 'vitest';
import { sum } from './sum';

describe('sum', () => {
  test('两个数字相加', () => {
    expect(sum(1, 2)).toBe(3);
  });
});
```

这种写法更明确。

一些团队喜欢这种写法，因为每个文件的依赖都很清楚。

---

## 十六、测试文件不生效时的排查方法

### 1. 检查文件名是否正确

正确：

```text
Button.test.tsx
sum.test.ts
user.spec.ts
```

错误：

```text
ButtonTest.tsx
Button_test.tsx
testButton.tsx
```

---

### 2. 检查 package.json 是否有 test 命令

确认有：

```json
{
  "scripts": {
    "test": "vitest"
  }
}
```

然后执行：

```bash
npm run test
```

---

### 3. 检查 vite.config.ts 是否配置正确

React 组件测试建议至少有：

```ts
test: {
  globals: true,
  environment: 'jsdom',
  setupFiles: './src/test/setup.ts',
}
```

---

### 4. 检查 setup 文件路径是否正确

如果配置是：

```ts
setupFiles: './src/test/setup.ts'
```

那么文件必须存在：

```text
src/test/setup.ts
```

路径写错会导致 setup 文件不加载。

---

### 5. 检查 TypeScript 类型配置

如果代码下面出现红线，检查：

```json
{
  "compilerOptions": {
    "types": [
      "vitest/globals",
      "vitest/jsdom",
      "@testing-library/jest-dom"
    ]
  }
}
```

---

### 6. 检查是否安装 jsdom

如果报错：

```text
ReferenceError: document is not defined
```

检查是否安装：

```bash
npm install -D jsdom
```

并确认配置：

```ts
environment: 'jsdom'
```

---

## 十七、完整最小配置汇总

### package.json

```json
{
  "scripts": {
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage"
  }
}
```

---

### vite.config.ts

```ts
/// <reference types="vitest/config" />

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],

  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
});
```

---

### src/test/setup.ts

```ts
import '@testing-library/jest-dom/vitest';
```

---

### tsconfig.json

```json
{
  "compilerOptions": {
    "types": [
      "vitest/globals",
      "vitest/jsdom",
      "@testing-library/jest-dom"
    ]
  },
  "include": [
    "src",
    "src/test/setup.ts"
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

## 十八、最终记忆版

Vitest 配置顺序：

```text
1. 安装依赖
2. 配置 package.json 测试命令
3. 配置 vite.config.ts 或 vitest.config.ts
4. 创建 src/test/setup.ts
5. 配置 tsconfig.json 类型
6. 创建业务代码
7. 创建 xxx.test.ts / xxx.test.tsx
8. 执行 npm run test
```

测试文件生效的关键条件：

```text
1. package.json 中有 vitest 命令
2. vite.config.ts / vitest.config.ts 配置正确
3. 测试文件名符合 *.test.ts / *.spec.ts / *.test.tsx / *.spec.tsx
4. 测试文件没有被排除
5. 执行了 npm run test 或 npm run test:run
```

一句话总结：

```text
Vitest 不需要你在 main.tsx 里导入测试文件。
它会根据文件名自动扫描并执行测试文件。
```

---
