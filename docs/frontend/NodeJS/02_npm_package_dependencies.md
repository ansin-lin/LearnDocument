# 第 2 章 npm、package.json 与项目依赖

## 1. npm 是什么

npm 是 Node.js 生态中常用的包管理工具。它负责安装项目所需的软件包、记录依赖和执行项目命令。

“包”可能是：

- TypeScript 编译器；
- Vite 构建工具；
- Vue、React；
- Axios；
- ESLint；
- 测试工具；
- 普通 JavaScript 库。

## 2. 创建项目说明

```bash
mkdir npm-practice
cd npm-practice
npm init -y
```

- `mkdir` 创建目录；
- `cd` 进入目录；
- `npm init -y` 使用默认值创建 `package.json`。

生成后建议把练习项目设置为不发布：

```json
{
  "name": "npm-practice",
  "private": true
}
```

## 3. package.json

常见结构：

```json
{
  "name": "npm-practice",
  "private": true,
  "type": "module",
  "scripts": {
    "start": "node src/app.js"
  },
  "dependencies": {},
  "devDependencies": {}
}
```

| 字段 | 作用 |
| --- | --- |
| `name` | 项目或包名称 |
| `private` | `true` 时防止误发布 |
| `type` | Node.js 如何解释 `.js` 模块 |
| `scripts` | 项目命令 |
| `dependencies` | 应用运行直接需要的包 |
| `devDependencies` | 开发、检查、测试、构建需要的包 |

## 4. 安装普通依赖

```bash
npm install axios
```

Axios 会记录到 `dependencies`。应用运行时直接使用的包通常放在这里。

## 5. 安装开发依赖

```bash
npm install --save-dev typescript
```

也可以缩写：

```bash
npm install -D typescript
```

TypeScript 编译器只在开发和构建阶段使用，因此记录到 `devDependencies`。

判断依据不是“这个包重要不重要”，而是部署后的应用运行是否直接需要它。

## 6. node_modules

安装后出现：

```text
npm-practice/
├─ node_modules/
├─ package.json
└─ package-lock.json
```

`node_modules` 保存实际依赖文件：

- 体积可能很大；
- 可以通过项目说明重新安装；
- 通常加入 `.gitignore`；
- 不应手工修改。

## 7. package-lock.json

锁定文件记录精确解析后的依赖版本和关系。

应用项目通常提交：

```text
package.json
package-lock.json
```

通常不提交：

```text
node_modules/
```

团队成员取得项目后执行：

```bash
npm install
```

CI 或要求严格按锁定文件安装时常用：

```bash
npm ci
```

`npm ci` 要求锁定文件与 `package.json` 一致，并会按锁定结果进行干净安装，适合自动化环境。

## 8. 删除依赖

```bash
npm uninstall axios
```

不要只删除 `node_modules` 中某个目录。卸载命令会同步更新项目说明和锁定文件。

## 9. 版本范围

`package.json` 中可能看到：

```json
{
  "dependencies": {
    "axios": "^1.0.0"
  }
}
```

版本范围影响后续允许安装的版本，锁定文件记录当前精确结果。不要手工猜测版本；新增或升级依赖应使用团队规定的 npm 命令并执行测试。

## 10. 常见错误

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 没有 `package.json` | 当前目录不是项目 | 进入正确目录 |
| 安装到了错误项目 | 终端目录错误 | 安装前确认路径 |
| 提交大量依赖文件 | `node_modules` 未忽略 | 更新 `.gitignore` |
| 团队安装结果异常 | 说明与锁定文件不一致 | 检查变更并重新安装 |
| 直接改依赖源码 | 修改会被重新安装覆盖 | 修改自己的代码或依赖版本 |

## 11. 练习

1. 创建 npm 项目；
2. 安装 TypeScript 为开发依赖；
3. 安装 Axios 为普通依赖；
4. 比较两个依赖所在字段；
5. 查看锁定文件和依赖目录；
6. 卸载 Axios并确认两个项目文件同步变化。
