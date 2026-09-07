# 第 1 章 认识 TypeScript 与运行代码

## 1. TypeScript 增加了什么

JavaScript负责运行程序。TypeScript在JavaScript的基础上增加类型语法，帮助我们在运行前发现“把字符串交给需要数字的位置”等问题。

**静态类型检查**是分析源代码，而不是执行每个业务分支。**编译**在这里是把TypeScript源代码转换为JavaScript。类型检查通过，不代表业务逻辑一定正确。

```ts
const message: string = "Hello TypeScript";
console.log(message);
```

`: string` 表示变量保存字符串。冒号及其后的类型不会保留在生成的JavaScript中。`console.log()` 在控制台输出值；本例输出 `Hello TypeScript`。

## 2. 准备运行环境

先完成[Node.js与npm基础](../NodeJS/index.md)。Node.js运行编译器和本章生成的JavaScript；npm安装依赖；npx调用项目中的工具。

在终端检查：

```bash
node --version
npm --version
```

如果找不到命令，先检查安装和终端环境，不要继续输入编译命令。

## 3. 创建独立练习项目

在准备保存练习的目录中执行：

```bash
mkdir typescript-practice
cd typescript-practice
npm init -y
npm install --save-dev typescript@5.9.3
mkdir src
```

本课程的示例按TypeScript 5.9.3验证，这是固定的教学环境，不代表最新版本。`--save-dev` 将编译器记为开发依赖；提交`package-lock.json`可以固定实际安装的版本。

确认当前项目编译器：

```bash
npx tsc --version
```

应显示`Version 5.9.3`。不需要再安装全局TypeScript。

## 4. 写入、编译和运行

### 4.1 写入源文件

创建`src/example.ts`，写入第1节的完整代码。`src`保存源文件，`dist`保存编译结果。

### 4.2 编译源文件

在包含`package.json`的项目目录执行：

```bash
npx tsc src/example.ts --strict --target ES2022 --module ES2022 --noEmitOnError --outDir dist
```

| 参数 | 可接受的值 | 本例作用 |
| --- | --- | --- |
| 输入路径 | 存在的`.ts`文件 | 检查并编译`src/example.ts` |
| `--strict` | 布尔开关，本例开启 | 启用严格类型检查 |
| `--target` | JavaScript语法级别，本例`ES2022` | 指定输出语法级别 |
| `--module` | 模块输出形式，本例`ES2022` | 使用标准ES模块输出规则 |
| `--noEmitOnError` | 布尔开关，本例开启 | 有错误时不生成新文件 |
| `--outDir` | 目录路径，本例`dist` | 保存生成文件 |

此时无需创建`tsconfig.json`；项目配置在第12章统一讲解。

### 4.3 运行输出文件

只有编译成功后才执行：

```bash
node dist/example.js
```

应输出`Hello TypeScript`。打开`dist/example.js`，确认`: string`已经消失。不要手工修改生成文件。

## 5. 观察一次类型错误

用下面代码替换`src/example.ts`：

```ts
let count: number = 2;
// count = "two"; // 取消注释后，字符串不能赋给number
console.log(count); // 2
```

取消第二行注释并编译，编译器会指出字符串不能赋给数字。恢复注释后重新编译、运行。

报错时`dist/example.js`可能仍然是上一次的文件。不要因为旧文件还能运行，就认为新代码已经编译成功。

类型也不会检查“折扣计算公式是否写反”“用户是否有审批权限”等业务规则，这些仍需测试和运行时校验。

## 6. 后续基础示例的统一用法

第2～12章每个`ts`代码块默认是一个独立实验：每次完整替换`src/example.ts`，使用第4节命令编译和运行。不要把不同示例拼接，否则同名类型或变量可能重复声明。

- 注释中的错误写法用于观察编译提示，正常运行时保持注释。
- 除明确标为片段的内容外，示例包含自身需要的类型和变量定义。
- 只有运行时代码产生控制台输出；类型别名、接口等声明本身没有输出。
- 第12章起，多文件示例会另外说明文件位置与运行步骤。

## 7. 常见问题与练习

| 现象 | 检查位置 |
| --- | --- |
| 找不到`node`或`npm` | Node.js安装和终端环境 |
| `tsc`找不到输入文件 | 当前工作目录与`src/example.ts`路径 |
| 编译报错后输出没有变化 | 是否误运行`dist`中的旧文件 |
| 重复声明变量 | 是否把多个独立示例拼到了同一文件 |

练习：修改输出文字，编译并核对结果；主动制造一次字符串赋给数字的错误，记录提示位置，然后修复。提交源文件、编译命令和成功输出。
