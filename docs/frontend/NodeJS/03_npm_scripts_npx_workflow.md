# 第 3 章 npm scripts、npx 与团队工作流

## 1. npm scripts

`package.json` 的 `scripts` 用于给项目命令统一名称。

```json
{
  "scripts": {
    "start": "node src/app.js",
    "typecheck": "tsc --noEmit",
    "build": "tsc"
  }
}
```

运行：

```bash
npm run typecheck
npm run build
npm start
```

`start` 可以使用 `npm start`，其他自定义脚本通常使用 `npm run 名称`。

## 2. 为什么使用 scripts

团队不需要记住每个工具的全部参数，只需执行项目约定命令：

```text
npm run typecheck
npm run test
npm run build
```

脚本名称相同，内部工具可以根据项目调整。

## 3. npx

```bash
npx tsc --version
```

`npx` 用于运行软件包提供的命令。项目已经安装 TypeScript 时，它会使用项目本地的 `tsc`。

这比依赖全局安装更稳定：

```text
项目 A → 自己的 TypeScript 版本
项目 B → 自己的 TypeScript 版本
```

不要把 `npx 某包` 理解成一定安全。执行陌生包前要确认名称、来源和项目说明，防止拼写错误运行到其他包。

## 4. npm install 与 npm ci

| 命令 | 常见用途 |
| --- | --- |
| `npm install` | 本地开发、增加或调整依赖 |
| `npm ci` | CI、交付验证、严格恢复锁定依赖 |
| `npm install 包名` | 安装普通依赖 |
| `npm install -D 包名` | 安装开发依赖 |
| `npm uninstall 包名` | 删除依赖 |
| `npm run 名称` | 执行项目脚本 |
| `npx 命令` | 运行包提供的命令 |

## 5. 取得项目后的操作

```bash
git clone <repository-url>
cd <project-directory>
npm ci
npm run typecheck
npm run test
npm run build
```

实际项目不一定拥有全部脚本。执行前先阅读：

- `README.md`
- `package.json`
- Node.js 版本文件
- 项目交接说明

不要看到 `package.json` 就自行假设命令名称。

## 6. 提交依赖变更

增加依赖后通常需要同时提交：

```text
package.json
package-lock.json
```

Review 时说明：

- 为什么需要新依赖；
- 是普通依赖还是开发依赖；
- 使用位置；
- 验证命令；
- 是否影响构建和交付。

不要只提交锁定文件，也不要在没有原因时顺手升级大量依赖。

## 7. 常见问题

### Missing script

```text
Missing script: "build"
```

说明 `package.json` 没有对应脚本。检查真实脚本名称，不要凭空补命令。

### command not found

本地依赖可能尚未安装。先执行项目规定的安装命令。

### Node.js 版本不一致

项目工具可能要求特定 Node.js 版本。检查 README、`.nvmrc`、`.node-version` 或 `package.json` 的 `engines`。

### 锁定文件冲突

不要随意删除锁定文件解决冲突。确认依赖变更来源，使用正确版本的 npm 重新生成，并执行完整验证。

## 8. 日本项目式确认记录

```text
対象案件：
Node.jsバージョン：
npmバージョン：
実行コマンド：
変更した依存関係：
package.json変更：有 / 無
package-lock.json変更：有 / 無
確認結果：
未解決事項：
```

## 9. 练习

1. 为TypeScript项目添加 `typecheck` 和 `build`；
2. 分别通过 npx 和 npm script 运行编译器；
3. 删除 `node_modules` 后使用锁定文件恢复；
4. 故意执行不存在的脚本并阅读错误；
5. 整理一次依赖变更说明和验证记录。
