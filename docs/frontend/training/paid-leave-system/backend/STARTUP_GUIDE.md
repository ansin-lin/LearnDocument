# Node.js 后台程序启动手顺

本手顺用于在 Windows 电脑上首次启动“有給休暇申請システム”的 Node.js 后台程序。后台启动后，Vue 前端才能进行注册、登录、申请和一览查询。

## 1. 启动前需要准备什么

电脑上需要安装：

- Node.js `22.12.0` 或更高的兼容版本；
- npm，安装 Node.js 时会一起安装；
- MySQL 8.4 LTS；
- Visual Studio Code 或其他文本编辑器。

打开 Windows CMD（命令提示符），执行：

```cmd
node --version
npm --version
mysql --version
```

正常情况下，三条命令都会显示版本号。

如果出现“`node`、`npm` 或 `mysql` 不是内部或外部命令”，先确认对应程序已安装，然后重新打开 CMD。如果 MySQL 已安装但仍无法执行 `mysql`，需要将 MySQL 的 `bin` 目录加入 Windows `PATH`。

## 2. 进入后台目录

在 CMD 中进入后台项目目录。请根据实际保存位置调整路径：

```cmd
cd /d "D:\ansin\教案\kejian\LearnDocument\docs\frontend\training\paid-leave-system\backend"
```

执行以下命令确认目录：

```cmd
dir
```

应该能看到：

```text
db
src
package.json
package-lock.json
.env.example
```

后续命令都在这个 `backend` 目录中执行。

## 3. 启动 MySQL 并初始化培训数据库

启动 MySQL，进入 MySQL 命令行后，使用 `source` 依次执行 `db` 文件夹中的两个 SQL 文件：

```sql
source db/01_schema.sql;
source db/02_seed.sql;
```

`01_schema.sql` 用于创建数据库和数据表，`02_seed.sql` 用于写入部门主数据。

## 4. 创建 `.env` 配置文件

在 CMD 中执行：

```cmd
copy .env.example .env
```

如果 `.env` 已经存在，不要直接覆盖。先确认现有设置再决定是否修改。

用 Visual Studio Code 打开 `.env`，填写本机环境：

```dotenv
PORT=3000
NODE_ENV=development
FRONTEND_ORIGIN=http://localhost:5174

DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=paid_leave_training
DB_USER=root
DB_PASSWORD=本机MySQL的root密码

SESSION_SECRET=至少32个字符的随机字符串
```

各项配置的作用：

| 配置 | 必填 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `PORT` | 否 | `1`～`65535`，默认 `3000` | Node.js API 监听端口 |
| `NODE_ENV` | 否 | `development`、`test`、`production` | 指定运行环境 |
| `FRONTEND_ORIGIN` | 否 | 完整 URL，默认 `http://localhost:5174` | 允许发送 Cookie 请求的前端来源 |
| `DB_HOST` | 否 | IP 地址或主机名 | MySQL 所在主机 |
| `DB_PORT` | 否 | `1`～`65535`，默认 `3306` | MySQL 监听端口 |
| `DB_NAME` | 否 | 非空数据库名 | 当前使用的数据库 |
| `DB_USER` | 是 | 非空账号，本练习使用 `root` | MySQL 登录账号 |
| `DB_PASSWORD` | 是 | MySQL 账号密码；无密码时留空 | MySQL 登录密码 |
| `SESSION_SECRET` | 是 | 至少 32 个字符 | Session Cookie 签名密钥 |

可以执行以下命令生成 `SESSION_SECRET`：

```cmd
node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
```

把命令输出的字符串复制到 `.env` 的 `SESSION_SECRET=` 后面。不要把 `.env` 提交到 Git，也不要把密码写入 Vue 前端的 `VITE_` 环境变量。

## 5. 启动前检查

执行：

```cmd
npm run check
npm test
```

`npm run check` 检查 JavaScript 语法。成功时会显示：

```text
Syntax check passed
```

`npm test` 执行当前的日期工具测试。结果中的 `fail` 应为 `0`。

## 6. 启动后台

开发练习时执行：

```cmd
npm run dev
```

`npm run dev` 使用 Node.js 的监视模式。修改后台 JavaScript 后，进程会自动重启。

启动成功时显示：

```text
Paid Leave API started: http://localhost:3000
```

启动后，这个 CMD 窗口会持续运行。不要关闭，另外打开一个 CMD 窗口启动 Vue 前端。

## 7. 确认后台是否正常

在新的 CMD 窗口中执行：

```cmd
curl http://localhost:3000/api/health
```

也可以直接在浏览器打开：

```text
http://localhost:3000/api/health
```

正常响应：

```json
{
  "data": {
    "status": "ok"
  }
}
```

这表示 Node.js 已经监听 `3000` 端口，并且后台可以连接 MySQL。

## 8. 启动 Vue 前端

保持后台窗口运行，另外打开一个 CMD，进入 `vue` 目录。请根据实际保存位置调整路径：

```cmd
cd /d "D:\ansin\教案\kejian\LearnDocument\docs\frontend\training\paid-leave-system\vue"
npm run dev
```

前端固定使用：

```text
http://localhost:5174
```

不要将一方的 `localhost` 改成 `127.0.0.1`。前端 URL 必须与后台 `.env` 中的 `FRONTEND_ORIGIN` 完全一致，包括协议、主机和端口。

## 9. 停止程序

在运行后台的 CMD 窗口中按：

```text
Ctrl + C
```

如果终端询问 `Terminate batch job (Y/N)?`，输入 `Y` 并回车。

前端也使用相同方法停止。停止 Node.js 不会删除 MySQL 中的用户和申请数据。

## 10. 以后每天的启动顺序

第一次初始化完成后，以后不需要每次重新创建数据库。

1. 确认 MySQL 服务已启动。
2. 在 `backend` 目录执行 `npm run dev`。
3. 访问 `/api/health` 确认后台和数据库连接正常。
4. 在 `vue` 目录执行 `npm run dev`。
5. 打开 `http://localhost:5174`。

## 11. 修改 `.env` 后必须重启

`.env` 只在 Node.js 进程启动时读取。修改数据库地址、密码、端口或 `FRONTEND_ORIGIN` 后：

1. 在后台窗口按 `Ctrl + C`；
2. 重新执行 `npm run dev`；
3. 重新访问 `/api/health`。

仅刷新浏览器不会让新的 `.env` 生效。

## 12. 常见问题

### 12.1 `ER_ACCESS_DENIED_ERROR`

现象：

```text
Access denied for user 'root'
```

检查 `.env` 中的 `DB_USER`、`DB_PASSWORD` 和 `DB_HOST`。先使用相同账号和密码执行 `mysql -u root -p` 确认能否登录。

### 12.2 `ECONNREFUSED 127.0.0.1:3306`

后台无法连接 MySQL。检查 MySQL 服务是否启动，以及 `DB_HOST` 和 `DB_PORT` 是否正确。

### 12.3 `Unknown database 'paid_leave_training'`

尚未执行 `db/01_schema.sql`，或 `.env` 中的 `DB_NAME` 不正确。返回第 4 节初始化数据库。

### 12.4 `EADDRINUSE: address already in use :::3000`

`3000` 端口已被其他程序占用。检查是否已经启动了一个后台窗口。不要重复启动同一个后台。

### 12.5 浏览器显示 CORS 错误

确认 Vue 实际地址与 `.env` 中的 `FRONTEND_ORIGIN` 完全相同。本项目默认都使用：

```text
http://localhost:5174
```

修改 `.env` 后必须重启后台。

### 12.6 `/api/auth/me` 返回 `data: null`

第一次进入系统时没有 Cookie 和 Session，这是正常的未登录状态。先从注册页创建用户，再进行登录。

### 12.7 修改后台代码后没有变化

确认使用的是 `npm run dev`。如果修改的是 `.env`，即使使用监视模式，也应手动停止后重新启动。

## 13. 启动成功的最终检查

开始 Vue 页面操作前，确认：

- [ ] `node --version`、`npm --version` 和 `mysql --version` 均可执行；
- [ ] MySQL 服务正在运行；
- [ ] `.env` 中的数据库连接信息正确；
- [ ] `SESSION_SECRET` 不是示例占位文字；
- [ ] `npm run check` 和 `npm test` 成功；
- [ ] 终端显示 `Paid Leave API started: http://localhost:3000`；
- [ ] `/api/health` 返回 `status: ok`；
- [ ] Vue 页面运行在 `http://localhost:5174`；
- [ ] 浏览器没有 CORS 或网络连接错误。
