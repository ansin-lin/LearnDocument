# 07 撤销与恢复

Git 撤销操作必须先判断两件事：变更是否已经提交，以及提交是否已经推送给团队。未提交内容可能不在 Git 历史中，丢弃后通常比提交更难恢复。

本章使用的 `HEAD~1`、提交 ID 和 `--` 路径分隔符已经在[第 02 章](02_basic_concepts.md)解释。执行前如果不能确定目标提交，先用 `git log --oneline --decorate` 查看历史。

## 7.1 先检查，再撤销

任何撤销操作前先执行：

```powershell
git status
git diff
git diff --staged
git log --oneline --decorate -5
```

如果内容重要但状态复杂，可以先复制文件到仓库外，或创建临时分支和提交。不要把 `reset --hard` 当成清理工作区的第一选择。

## 7.2 场景选择表

| 当前情况 | 常用操作 | 是否改写历史 |
|---|---|---|
| 未暂存修改不需要了 | `git restore <file>` | 否，但未提交内容会丢失 |
| 已暂存，想取消暂存 | `git restore --staged <file>` | 否 |
| 最近提交信息或漏文件，尚未共享 | `git commit --amend` | 是 |
| 本地提交要拆回暂存区 | `git reset --soft HEAD~1` | 是 |
| 公共分支已有错误提交 | `git revert <commit>` | 否，新增反向提交 |
| reset/rebase 后找不到提交 | `git reflog` | 先定位，再恢复 |

## 7.3 恢复未提交文件

取消暂存不会删除工作区修改：

```powershell
git restore --staged README.md
git status
```

丢弃工作区修改：

```powershell
git diff -- README.md
git restore -- README.md
```

第二条命令会用暂存区内容覆盖工作区文件。未提交修改可能无法由 Git 找回，必须先确认差异。

从指定提交取出某个文件会覆盖该文件的工作区内容：

```powershell
git restore --source=HEAD~1 -- README.md
git diff -- README.md
```

恢复后仍需决定是否暂存和提交。

## 7.4 修改最近一次本地提交

只修改最近提交信息：

```powershell
git commit --amend -m "docs: correct setup instructions"
```

补充漏掉的文件：

```powershell
git add missing-file.md
git commit --amend --no-edit
```

amend 会创建新提交并替换分支末端的旧提交。只应直接用于尚未共享的提交；已经推送的提交应遵守团队历史策略。

## 7.5 reset 的三种模式

`reset` 会移动当前分支指针，因此会改变当前分支看到的提交历史。

| 模式 | 分支指针 | 暂存区 | 工作区 |
|---|---|---|---|
| `--soft` | 移动 | 保留 | 保留 |
| `--mixed` | 移动 | 重置到目标提交 | 保留 |
| `--hard` | 移动 | 重置到目标提交 | 重置到目标提交 |

把最近一次本地提交拆回暂存区：

```powershell
git reset --soft HEAD~1
git status
git diff --staged
```

把最近一次本地提交拆回工作区：

```powershell
git reset --mixed HEAD~1
git status
git diff
```

以下命令会丢弃目标提交之后的提交引用、暂存修改和工作区修改，禁止在含有未保存成果的仓库中照抄：

```powershell
git reset --hard HEAD~1
```

公共分支通常使用 `revert`，而不是 reset 后强制推送覆盖团队历史。

## 7.6 用 revert 修正公共历史

```powershell
git status
git revert <commit>
```

`revert` 创建一个新提交，反向应用目标提交的变更，因此保留“发生过错误以及如何修正”的记录，适合已经推送的提交。

发生冲突时，解决并继续：

```powershell
git add <conflicted-file>
git revert --continue
```

不想继续：

```powershell
git revert --abort
```

回退 merge commit 需要选择主线父提交，不能在不了解分支关系时机械执行 `git revert -m`，应先查看提交图并与负责人确认。

## 7.7 使用 reflog 找回移动过的分支

reflog 记录本地引用的移动历史，常用于找回 reset、rebase 或误删分支前的位置：

```powershell
git reflog --date=local
```

找到目标提交 ID 后，先创建救援分支，不要再次 reset 覆盖当前状态：

```powershell
git branch rescue/<name> <commit-id>
git show <commit-id>
```

确认救援分支内容正确后，再决定 merge、cherry-pick 或切换分支。reflog 主要是本地记录，并会按 Git 的保留和清理规则过期，不能当作永久备份，也不能保证找回从未提交的文件。

## 7.8 实验：比较 soft、mixed 和 revert

**范围：** 请在独立练习仓库中执行，不要使用工作项目。每次危险实验前记录 `git log` 和 `git status`。

```powershell
New-Item -ItemType Directory git-undo-lab
Set-Location git-undo-lab
git init -b main
git config user.name "Git Learner"
git config user.email "learner@example.com"
Set-Content app.txt "version 1"
git add app.txt
git commit -m "feat: add version 1"
Set-Content app.txt "version 2"
git commit -am "feat: add version 2"
git log --oneline
```

记录最新提交 ID 后尝试 soft reset：

```powershell
git reset --soft HEAD~1
git status
git diff --staged
git commit -m "feat: restore version 2 commit"
```

再使用 revert 创建反向提交：

```powershell
git revert --no-edit HEAD
git log --oneline
Get-Content app.txt
```

验证：soft reset 后修改仍在暂存区；revert 后历史中同时保留原提交和反向提交。

## 7.9 常见错误

```text
fatal: ambiguous argument 'HEAD~1': unknown revision or path not in the working tree.
```

当前仓库可能只有一个提交，因而不存在上一代提交；也可能是引用拼写错误。先运行 `git log --oneline` 确认提交数量。

```text
error: commit <commit-id> is a merge but no -m option was given.
```

目标是 merge commit，Git 不知道应以哪个父提交为主线。不要猜测 `-m` 数字；先查看提交图并与项目负责人确认回退目标。

## 7.10 本章总结

- 未提交修改、已暂存修改、本地提交和已共享提交的撤销方式不同。
- `restore` 处理文件，`reset` 移动分支，`revert` 用新提交修正历史。
- `--hard` 会覆盖暂存区和工作区，不能作为常规清理手段。
- reflog 可以帮助寻找移动过的本地引用，但不是永久备份。

## 练习

1. 取消一次暂存，确认工作区修改仍存在。
2. 比较 `reset --soft` 与 `reset --mixed` 后 `git status` 的差异。
3. 解释为什么已推送到公共分支的错误通常优先使用 revert。

### 自检提示

- `restore --staged` 后修改应仍保留在工作区。
- `reset --soft HEAD~1` 后，最近提交消失，但其修改应出现在暂存区。
- `revert` 后历史会新增一个提交，而不是删除原提交。

[上一章：团队协作与代码评审](06_teamwork_and_conflicts.md) · [下一章：标签与版本发布](08_tags_and_release.md)
