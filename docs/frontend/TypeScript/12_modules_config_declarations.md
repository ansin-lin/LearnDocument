# 第 12 章 模块与项目配置

React 和 Vue 项目都不会把全部代码写在一个文件中。本章只学习进入框架前必须掌握的两件事：怎样在文件之间导入、导出，以及怎样看懂项目中的 TypeScript 配置。

## 1. 为什么要拆分模块

一个 `.ts` 文件就是一个模块。把数据类型、工具函数和页面代码分开放置，可以减少重复，也便于多人协作。

```text
src/
├─ types.ts       # 公共类型
├─ price.ts       # 计算函数
└─ main.ts        # 使用这些内容的入口
```

文件名和目录结构由项目决定；重点是让一个文件集中负责一类内容。

## 2. 导出与导入

### 2.1 导出类型

`types.ts`：

```ts
export interface Product {
  id: number;
  name: string;
  price: number;
}
```

`export`表示允许其他模块使用这个声明。

### 2.2 导出运行时的值

`price.ts`：

```ts
export function formatPrice(price: number): string {
  return `${price.toLocaleString()}円`;
}
```

函数在程序运行时需要执行，因此属于“值”。常量、函数和类也都是运行时的值。

### 2.3 在其他文件中导入

`main.ts`：

```ts
import type { Product } from "./types";
import { formatPrice } from "./price";

const product: Product = { id: 1, name: "キーボード", price: 5000 };
console.log(product.name, formatPrice(product.price)); // キーボード 5,000円
```

- `import type`只导入类型，编译后这条导入会被删除。
- 普通`import`导入程序运行时需要的值。
- `{ Product }`这种写法表示**命名导出**，名称必须和导出侧一致。

React、Vue 和 Vite 会处理模块路径。源码中是否写扩展名应遵守当前项目已有写法，不要把某一个环境的规则套到所有项目。

## 3. 命名导出与默认导出

```ts
export const taxRate = 0.1;
export default function setup() {
  console.log("setup");
}
```

```ts
import setup, { taxRate } from "./config";
```

一个模块最多有一个默认导出，可以有多个命名导出。初学阶段优先跟随现有项目规范；在公共工具和类型文件中，命名导出更容易统一名称并搜索引用。

## 4. 看懂 tsconfig.json

`tsconfig.json`指定哪些文件接受检查，以及编译器采用什么规则。React、Vue 项目通常已经由创建工具生成配置，学员应先理解和遵守，不需要从零背写完整文件。

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "strict": true,
    "noEmit": true
  },
  "include": ["src"]
}
```

| 配置 | 作用 | 新人需要知道什么 |
| --- | --- | --- |
| `target` | 指定可使用的 JavaScript 语法级别 | 一般沿用项目设置 |
| `module` | 指定模块代码的处理方式 | Vite 项目通常使用现代 ES 模块 |
| `moduleResolution` | 指定怎样查找导入的模块 | `Bundler`适合由 Vite 等构建工具处理的项目 |
| `strict` | 开启一组严格类型检查 | 新项目建议保持`true` |
| `noEmit` | 只做类型检查，不由 TypeScript 直接生成文件 | React/Vue 项目通常由构建工具负责输出 |
| `include` | 指定参与检查的文件范围 | 通常包含`src`目录 |

有些项目还会开启下面两项更严格的规则：

- `noUncheckedIndexedAccess`：数组或对象按下标读取时，结果会考虑`undefined`。
- `exactOptionalPropertyTypes`：更严格地区分“属性不存在”和“属性存在但值为`undefined`”。

这些规则会影响写法，但不是学习 React/Vue 前必须自行配置的内容。以团队仓库中的配置为准。

## 5. 项目中的类型检查

常见的 `package.json` 脚本如下，具体名称以项目为准：

```json
{
  "scripts": {
    "typecheck": "tsc --noEmit"
  }
}
```

```bash
npm run typecheck
```

`tsc --noEmit`会按照项目配置检查类型，但不输出 JavaScript。框架项目还可能把类型检查整合进 `build` 命令，因此提交代码前要执行仓库说明中要求的命令。

## 6. 本章不展开的内容

`.d.ts`声明文件、`declare`、模块补充和路径别名属于库接入或项目配置专题。初学 React/Vue 时先会使用已有类型即可，遇到没有类型的第三方库时再根据该库官方文档处理，不要用全局`any`隐藏问题。

## 7. 练习

1. 建立`types.ts`，导出一个`User`接口。
2. 建立`format.ts`，导出一个接收姓名并返回问候语的函数。
3. 在`main.ts`中用`import type`导入`User`，用普通`import`导入函数。
4. 执行当前项目的类型检查命令，确认没有错误。

完成后，应能说明：类型导入为什么使用`import type`，以及`strict`和`noEmit`分别解决什么问题。
