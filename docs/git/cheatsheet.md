# Git 命令速查表

本页用于复习已经学过的命令，不代替各章中的风险和场景说明。示例默认使用 Windows PowerShell；占位符必须替换为实际值。

## 状态与差异

```powershell
git status
git status --short
git diff
git diff --staged
git diff HEAD
```

## 暂存与提交

```powershell
git add <file>
git add -p <file>
git diff --staged
git commit -m "type: clear change summary"
git show --stat HEAD
```

## 查看历史

```powershell
git log --oneline --graph --decorate --all
git show <commit-id>
git log -- <file>
```

## 分支与合并

```powershell
git branch -vv
git switch -c feature/<name>
git switch main
git merge feature/<name>
git merge --abort
git branch --merged
git branch -d feature/<name>
```

## 远程仓库

```powershell
git remote -v
git fetch origin
git pull --ff-only
git push -u origin feature/<name>
git branch -vv
git fetch --prune origin
```

## 取消暂存和丢弃文件修改

执行 `restore` 前先查看差异；未提交内容可能无法恢复。

```powershell
git restore --staged <file>
git diff -- <file>
git restore -- <file>
```

## 修正提交

```powershell
# 仅适合尚未共享的最近提交
git commit --amend

# 将最近一次本地提交拆回暂存区
git reset --soft HEAD~1

# 通过新提交修正公共历史
git revert <commit-id>
```

`git reset --hard` 会覆盖暂存区和工作区，不列为日常速查命令。需要使用时先阅读[撤销与恢复](07_undo_and_reset.md)。

## 冲突处理

```powershell
git status
git add <resolved-file>
git merge --continue
git rebase --continue
git rebase --abort
git cherry-pick --abort
git revert --abort
```

完成冲突处理后还要运行项目测试、构建或检查命令。

## stash

```powershell
git stash push -m "WIP: short description"
git stash list
git stash show -p 'stash@{0}'
git stash apply 'stash@{0}'
git stash drop 'stash@{0}'
```

## 标签

```powershell
git tag --list
git tag -a v1.0.0 -m "release: version 1.0.0"
git show v1.0.0
git push origin v1.0.0
```

## 恢复入口

```powershell
git reflog --date=local
git branch rescue/<name> <commit-id>
```

[返回课程入口](index.md)
