# 09 常用高级操作

本章介绍 stash、cherry-pick、rebase 和 submodule。它们能解决特定问题，但也更容易造成冲突或历史混乱。操作前先保持状态可确认，并优先在个人练习仓库验证。

## 9.1 stash：临时保存未提交工作

需要临时切换任务，又不适合创建正式提交时，可以使用 stash。示例中的 WIP 是 Work in Progress 的缩写，表示“尚未完成的工作”：

```powershell
git status
git stash push -m "WIP: login validation"
git stash list
```

默认情况下，stash 保存已跟踪文件的修改，不包含普通未跟踪文件。确实需要同时保存未跟踪文件时：

```powershell
git stash push -u -m "WIP: include new login file"
```

不要用 `-a` 随意保存被忽略的依赖或构建目录，这可能产生巨大 stash。

查看和应用：

```powershell
git stash show -p 'stash@{0}'
git stash apply 'stash@{0}'
git status
```

PowerShell 中应给 `stash@{0}` 加引号，避免花括号被 PowerShell 解析。确认应用结果并完成测试后，再删除对应 stash：

```powershell
git stash drop 'stash@{0}'
```

`pop` 相当于尝试应用后删除；发生冲突时仍需要检查 stash 列表，不能假定它已经删除：

```powershell
git stash pop
git status
git stash list
```

stash 适合短期切换，不适合代替清晰提交或长期备份。

## 9.2 cherry-pick：复制指定提交的变更

cherry-pick 会把指定提交的变更应用到当前分支，并创建一个新的提交：

```powershell
git status
git cherry-pick <commit-id>
```

常见场景是把已经确认的修复提交应用到另一个维护分支。它不是“移动原提交”，新提交会有不同 ID。

发生冲突后：

```powershell
git status
# 编辑并测试冲突文件
git add <conflicted-file>
git cherry-pick --continue
```

取消操作：

```powershell
git cherry-pick --abort
```

不要 cherry-pick 一个大型 merge commit 或大量相互依赖的提交来代替正常合并；这容易遗漏依赖并造成重复提交。

## 9.3 交互式 rebase：整理个人提交

在尚未共享的个人分支上，可以整理最近几次提交：

```powershell
git status
git rebase -i HEAD~3
```

假设整理前最近三次提交为：

```text
A -- B -- C  feature
```

把 C 标记为 `squash` 合并到 B 后，Git 会创建新的提交 D：

```text
A -- D  feature
```

B、C 的旧提交 ID 不再位于当前分支历史中，因此整理前后要分别使用 `git log --oneline` 比较结果。

编辑器中常见动作包括：

- `pick`：保留提交
- `reword`：修改提交信息
- `squash`：合并到前一个提交并编辑说明
- `fixup`：合并到前一个提交并丢弃当前说明

交互式 rebase 会重写提交 ID。不要擅自整理公共分支，发生问题时使用：

```powershell
git rebase --abort
```

操作完成后检查：

```powershell
git log --oneline --graph --decorate
git status
```

## 9.4 reflog：恢复入口

reset、rebase 或误删本地分支后，使用：

```powershell
git reflog --date=local
git branch rescue/<name> <commit-id>
```

详细恢复原则参见[第 07 章](07_undo_and_reset.md)。reflog 是本地、会过期的引用日志，不是远程备份，也不能保证恢复未提交文件。

## 9.5 submodule：固定引用另一个仓库

submodule 让主仓库记录另一个仓库的特定提交。它不是普通目录复制，主仓库保存的是子模块地址和提交指针。

只有项目明确采用 submodule 时才使用：

```powershell
git submodule add <repository-url> libs/shared-lib
git status
git commit -m "build: add shared library submodule"
```

克隆包含 submodule 的项目：

```powershell
git clone --recurse-submodules <repository-url>
```

已经克隆后初始化：

```powershell
git submodule update --init --recursive
```

子模块默认检出主仓库记录的特定提交，常处于 detached HEAD（分离头指针）状态：`HEAD` 直接指向提交，而不是指向可移动分支。此时创建新提交后如果没有建立分支或标签，后续切换位置可能让提交失去容易找到的引用。

更新子模块版本需要在子模块中取得目标提交，再回到主仓库提交新的子模块指针。删除 submodule 涉及 `.gitmodules`、索引和模块元数据，应遵循项目文档，不要只删除目录。

## 9.6 实验：串联 stash、cherry-pick 和 reflog

**环境与范围：** Windows PowerShell；在新的 `git-advanced-lab` 中执行，不连接远程仓库。

```powershell
New-Item -ItemType Directory git-advanced-lab
Set-Location git-advanced-lab
git init -b main
git config user.name "Git Learner"
git config user.email "learner@example.com"
Set-Content app.txt "base"
git add app.txt
git commit -m "feat: add base"

git switch -c fix/message
Set-Content fix.txt "fixed"
git add fix.txt
git commit -m "fix: add message fix"
$fixCommit = git rev-parse HEAD

git switch main
git cherry-pick $fixCommit
Add-Content app.txt "unfinished"
git stash push -m "WIP: unfinished app change"
git status
git stash apply 'stash@{0}'
git status
git reflog -5
```

验证结果：`main` 中应包含 `fix.txt`；stash 后工作区应干净；apply 后 `app.txt` 应再次显示为已修改；reflog 应列出最近的 cherry-pick、分支切换和提交位置。

## 9.7 常见错误

```text
No local changes to save
```

当前没有可由默认 stash 保存的已跟踪修改。如果只有未跟踪文件，需要确认是否应使用 `git stash push -u`。

cherry-pick 或 rebase 冲突后如果继续执行其他历史操作，Git 会提示当前操作尚未完成。先用 `git status` 判断是应解决并 `--continue`，还是使用对应的 `--abort`。

## 9.8 本章总结

- stash 用于短期保存未提交工作，默认不包含未跟踪文件。
- cherry-pick 在当前分支创建内容相同但 ID 不同的新提交。
- 交互式 rebase 只用于允许改写的个人历史。
- submodule 记录其他仓库的固定提交，需要独立初始化和更新。

## 练习

1. 分别 stash 已跟踪修改和未跟踪文件，观察 `-u` 的区别。
2. 创建两个分支，将一个小提交 cherry-pick 到另一个分支。
3. 说明为什么 PowerShell 中 `stash@{0}` 要加引号。

### 自检提示

- `git stash list` 应显示带有自定义说明的 stash 条目。
- cherry-pick 后新提交内容相同，但提交 ID通常与来源提交不同。
- detached HEAD 状态下需要先创建分支，才能让新提交长期具有清晰引用。

[上一章：标签与版本发布](08_tags_and_release.md) · [返回课程入口](index.md)
