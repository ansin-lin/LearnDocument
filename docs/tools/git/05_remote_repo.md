# 05 远程仓库与同步

第 04 章在本地完成了 Branch 和 Merge。本章解决团队协作的下一问题：本机 Commit 怎样与 GitHub/GitLab 服务器交换，并且怎样确认每个 Branch 当前位于哪里。

## 本章学习目标

### Level A：必须掌握

- 区分 Local Repository、Remote Repository 和 `origin`。
- 区分本地 `develop`、本地 `origin/develop` 和服务器 `develop`。
- 使用 `remote`、`fetch`、`pull --ff-only`、`push` 和 `clone`。
- 说明 Push 与 PR/MR 不是同一个动作。

### Level B：理解并能够查资料操作

- 理解 upstream/tracking，并跟踪已有 Remote Branch。
- 根据状态调查 non-fast-forward 拒绝。
- 按项目规则删除、清理或重命名 Remote Branch。

### Level C：项目规则决定

- 分叉后的 rebase、共享历史改写和强制推送。
- 新人不应把这些操作作为 Push 失败时的默认解决方案。

## 5.1 为什么需要 Remote Repository

Local Repository 能保存个人 Commit，但其他成员无法自动看到本机历史。如果电脑损坏或只有一人持有 Commit，团队也无法稳定协作。

Remote Repository（远程仓库）提供团队通过网络共享的 Git 仓库，常由 GitHub、GitLab 或公司内部平台托管。

```text
开发者 A 的 Local Repository
              │
              ├── Remote Repository
              │
开发者 B 的 Local Repository
```

Remote Repository 不是自动同步网盘。开发者需要明确执行 fetch、pull 或 push。

## 5.2 origin 是什么

一个本地仓库可以登记多个 Remote Repository。`origin` 是 clone 时常见的默认远程名称，只是一个可更改的别名，不是 Git 关键字。

查看已经登记的远程：

```cmd
git remote -v
git remote get-url origin
```

- `remote -v`：显示 Remote 名称以及 fetch、push 使用的 URL。
- `remote get-url origin`：只读取 `origin` 的 URL，不连接服务器。

没有 `origin` 的本地仓库，可以按项目提供的真实地址添加：

```cmd
git remote add origin REPOSITORY_URL
git remote -v
```

`REPOSITORY_URL` 必须替换为项目 HTTPS 或 SSH 地址。地址错误时修改：

```cmd
git remote set-url origin NEW_REPOSITORY_URL
```

这些命令只修改本地 Remote 配置，不会移动 Branch 或上传 Commit。

## 5.3 三个 develop 不是同一个对象

连接远程仓库以后，必须区分：

```text
本地 Branch：              develop
本地 Remote-tracking Branch：origin/develop
服务器 Branch：            develop
```

- 本地 `develop`：可以切换并创建本地 Commit。
- `origin/develop`：保存在本机、记录最近一次取得的服务器 `develop` 状态。
- 服务器 `develop`：团队 Remote Repository 中的真实 Branch。

```text
本地 develop  ────────────── push ──────────────> 服务器 develop
                                                       │
本地 origin/develop <──────── fetch 更新本地记录 ──────┘
```

`origin/develop` 不是服务器 Branch 本身，也不能像普通本地 Branch 一样直接在上面继续开发。

## 5.4 fetch：先取得服务器状态

假设服务器已经前进到 C，本地仍停留在 B：

```text
服务器 develop：       A ── B ── C
本地 develop：         A ── B
本地 origin/develop：  A ── B
```

只想先取得并调查远程变化：

```cmd
git fetch origin
git log --oneline --graph --decorate --all
git diff develop..origin/develop
```

- `fetch origin`：从 `origin` 获取 Commit 和引用，更新 `origin/develop` 等 Remote-tracking Branch。
- `log ... --all`：查看本地各引用的相对位置。
- `diff develop..origin/develop`：比较两个 Branch 端点的文件差异。

执行后：

```text
服务器 develop：       A ── B ── C
本地 develop：         A ── B
本地 origin/develop：  A ── B ── C
```

`fetch` 不会自动移动本地 `develop`，也不会自动改变当前 Working Tree。因此它适合“先看看服务器发生了什么”。

## 5.5 pull：取得并整合

`pull` 可以理解为：

```text
fetch
  +
把取得的 upstream 变化整合进当前 Branch
```

在本地 `develop` 没有独有 Commit、只需要安全快进时：

```cmd
git switch develop
git status
git pull --ff-only
```

- `switch develop`：确认接收更新的是本地 `develop`。
- `status`：确认没有遗留修改和进行中的操作。
- `--ff-only`：只允许 fast-forward；如果历史已经分叉则停止，不自动创建 Merge Commit。

成功后：

```text
本地 develop：         A ── B ── C
本地 origin/develop：  A ── B ── C
```

项目可能规定 pull 时使用 merge、rebase 或 `ff-only`。新人教程使用 `--ff-only` 避免在状态不清楚时自动产生合并；真实项目以开发手顺为准。

## 5.6 push：把本地 Commit 发送到服务器

假设本地任务 Branch 有一个服务器尚未拥有的 Commit D：

```text
本地 feature/APP-123：A ── B ── C ── D
服务器：尚无 feature/APP-123
```

首次 Push：

```cmd
git push -u origin feature/APP-123
```

- `origin`：目标 Remote。
- `feature/APP-123`：要上传的本地 Branch。
- `-u`：在 Push 成功后设置 upstream。

执行后：

```text
本地 feature/APP-123            → D
本地 origin/feature/APP-123     → D
服务器 feature/APP-123          → D
```

后续通常可以简写：

```cmd
git push
```

Push 后必须阅读完整输出，并在平台确认目标 Branch。命令成功只说明指定 Remote Branch 已更新。

## 5.7 Push 不等于 PR/MR

```text
Push：
Local feature/APP-123
        ↓
Remote feature/APP-123

PR / MR：
Remote feature/APP-123
        ↓ 请求 Review 和合并
Remote develop
```

因此：

> `git push` 成功以后，修改不一定已经进入 `develop`。

是否进入目标 Branch，还取决于 PR/MR、Review、CI 和 Merge。第 06 章正式学习这段流程。

## 5.8 upstream / tracking 是什么

Remote-tracking Branch 是具体的本地引用，例如 `origin/feature/APP-123`。upstream（上游）是本地 Branch 与默认同步目标之间的关联：

```text
本地 feature/APP-123
          │ upstream
          ▼
本地 origin/feature/APP-123
          │ 对应远程状态
          ▼
服务器 feature/APP-123
```

首次执行 `push -u` 后检查：

```cmd
git branch -vv
```

方括号中应出现类似 `[origin/feature/APP-123]`。之后不带 Branch 名的 `push` 或 `pull` 才能根据关联确定默认目标。

upstream 不是第四种 Branch，也不一定名为 `upstream`。某些 fork 工作流会把另一个 Remote 别名命名为 `upstream`，那只是 Remote 名称，与本节的 tracking 关系不要混淆。

## 5.9 clone：取得已有项目

加入既有项目时通常使用 clone，而不是先创建空目录执行 `git init`：

```cmd
git clone REPOSITORY_URL
cd REPOSITORY_DIRECTORY
git remote -v
git branch --all
git status
```

`git clone` 通常会：

- 创建目标目录。
- 下载 Commit 和引用。
- 检出 Remote 默认 Branch。
- 登记名为 `origin` 的 Remote。

`REPOSITORY_DIRECTORY` 是 clone 创建的实际目录名。执行后应阅读项目 README 和开发手顺，不能假设默认 Branch 一定是 `main` 或 `develop`。

跟踪一个已有 Remote Branch：

```cmd
git fetch origin
git switch --track origin/feature/APP-123
```

需要使用不同本地名称时：

```cmd
git switch -c local-app-123 --track origin/feature/APP-123
```

这些属于 Level B；新人日常流程通常自己从目标 Branch 创建任务 Branch。

## 5.10 Push 被拒绝时先调查

典型错误：

```text
! [rejected] feature/APP-123 -> feature/APP-123 (non-fast-forward)
```

含义是服务器 Branch 包含本地没有的历史，直接 Push 可能覆盖变化。不要立即强制推送。

先执行只读调查：

```cmd
git fetch origin
git status
git log --oneline --graph --decorate --all
```

然后确认：

- 是否推送到了正确 Branch。
- 是否有其他成员更新了同名 Branch。
- 当前 Branch 是否允许共享。
- 项目要求 merge、rebase，还是重新建立 Branch。

rebase 和 `force-with-lease` 的风险见第 09 章。受保护的 `develop` 通常要求通过 PR/MR 合入，不允许开发者直接 Push。

## 5.11 Level B：Remote Branch 清理与重命名

删除已经确认不再需要的 Remote Branch：

```cmd
git push origin --delete feature/old-task
git fetch --prune origin
```

- `push origin --delete`：请求服务器删除指定 Branch，会影响其他成员。
- `fetch --prune`：清理本地已经不存在于服务器的 Remote-tracking Branch，不删除本地工作 Branch。

重命名一个尚未完成的个人功能 Branch：

```cmd
git branch -m feature/old-name feature/new-name
git push -u origin feature/new-name
git push origin --delete feature/old-name
```

这是本地重命名、创建新 Remote Branch、删除旧 Remote Branch 三个独立动作，每一步都要验证。默认 Branch 重命名还涉及平台设置、保护规则、CI 和文档，不应只执行以上命令。

## 5.12 实验：Push 一个练习 Branch

**前置条件：** 教师准备一个允许学员 Push 功能 Branch 的练习 Remote Repository；默认 Branch 为 `develop`。认证、网络和权限依赖外部环境，无法只靠本地审计验证。

```cmd
git clone REPOSITORY_URL
cd REPOSITORY_DIRECTORY
git remote -v
git switch develop
git pull --ff-only
git switch -c feature/APP-123
```

使用编辑器创建或修改教师指定文件，然后：

```cmd
git status
git diff
git add FILE_PATH
git diff --staged
git commit -m "feat: complete APP-123 practice"
git push -u origin feature/APP-123
git branch -vv
```

验证：平台上出现 `feature/APP-123`，最新 Commit ID 与本地一致；`develop` 尚未因这次 Push 自动变化。

## 5.13 常见错误

### origin 不存在或 URL 错误

```text
fatal: 'origin' does not appear to be a git repository
```

使用 `git remote -v` 检查名称和 URL。

### 当前 Branch 没有 upstream

```text
There is no tracking information for the current branch.
```

确认 Remote 和 Branch 名后，首次 Push 使用 `git push -u origin BRANCH_NAME`，或按项目手顺建立 tracking。

### 认证或权限失败

记录完整错误，检查 Remote URL、账号权限、HTTPS/SSH 凭据、VPN 和 SSO。不要把令牌、私钥或内部地址贴到公开渠道。

## 本章必须掌握

- `origin` 是 Remote 别名。
- fetch 更新 Remote-tracking Branch，不自动改变本地工作 Branch。
- pull 会获取并整合，执行前必须确认当前 Branch 和状态。
- push 上传 Commit 到指定 Remote Branch，不等于完成 PR/MR 或 Merge。

## 本章不要求现在掌握

- 分叉历史的 rebase 和强制推送。
- 默认 Branch 迁移。
- 复杂的 fork 与多 Remote 工作流。

## 进入下一章前确认

1. `develop`、`origin/develop` 和服务器 `develop` 有何区别？
2. 同事更新了服务器 `develop`，只想先查看变化，应使用什么操作？
3. 为什么 fetch 后 Working Tree 通常不变？
4. `pull --ff-only` 为什么可能停止？
5. 首次 Push 为什么常使用 `-u`？
6. Push 成功后，修改是否已经进入 `develop`？
7. non-fast-forward 时为什么不能立即强制推送？

[上一章：分支、合并与冲突](04_branches_and_merge.md) · [下一章：团队协作与代码评审](06_teamwork_and_conflicts.md)
