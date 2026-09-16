# Git 命令速查表

本页用于复习已经学过的命令，不代替各章中的风险和场景说明。示例默认使用 Windows CMD；`FILE_PATH`、`COMMIT_ID`、`BRANCH_NAME` 等大写名称必须替换为实际值。

## 状态与差异

```cmd
git status
git status --short
git diff
git diff --staged
git diff HEAD
```

## 暂存与提交

```cmd
git add FILE_PATH
REM 进阶：同一文件中只暂存部分修改
git add -p FILE_PATH
git diff --staged
git commit -m "type: clear change summary"
git show --stat HEAD
```

## 查看历史

```cmd
git log --oneline --graph --decorate --all
git show COMMIT_ID
git log -- FILE_PATH
git blame FILE_PATH
```

## 分支与合并

```cmd
git branch -a
git branch -vv
git switch -c feature/NAME
git switch main
git merge feature/NAME
git merge --abort
git branch --merged
git branch -d feature/NAME
```

## 远程仓库

```cmd
git remote -v
git fetch origin
git pull --ff-only
git push -u origin feature/NAME
git branch -vv
git fetch --prune origin
```

## Local、Remote tracking 与 PR/MR

```text
Local branch:
feature/APP-123

Remote-tracking branch（保存在本地）:
origin/feature/APP-123

Remote server branch（GitHub/GitLab）:
feature/APP-123
```

```text
Commit = 修改进入本地分支
Push   = 本地分支 → 服务器分支
PR/MR  = 服务器功能分支 → 请求合入服务器目标分支
Merge  = 服务器目标分支取得修改
```

## 取消暂存和丢弃文件修改

执行 `restore` 前先查看差异；未提交内容可能无法恢复。

```cmd
git restore --staged FILE_PATH
git diff -- FILE_PATH
git restore -- FILE_PATH
```

## 修正提交

```cmd
REM 仅适合尚未共享的最近提交
git commit --amend

REM 将最近一次本地提交拆回暂存区
git reset --soft HEAD~1

REM 通过新提交修正公共历史
git revert COMMIT_ID
```

`git reset --hard` 会覆盖暂存区和工作区，不列为日常速查命令。需要使用时先阅读[撤销与恢复](07_undo_and_reset.md)。

## 冲突处理

```cmd
git status
git add RESOLVED_FILE
git merge --continue
git rebase --continue
git rebase --abort
git cherry-pick --abort
git revert --abort
```

完成冲突处理后还要运行项目测试、构建或检查命令。

## stash

```cmd
git stash push -m "WIP: short description"
git stash list
git stash show -p "stash@{0}"
git stash apply "stash@{0}"
git stash drop "stash@{0}"
```

## 标签

```cmd
git tag --list
git tag -a v1.0.0 -m "release: version 1.0.0"
git show v1.0.0
git push origin v1.0.0
```

## 恢复入口

```cmd
git reflog --date=local
git branch rescue/NAME COMMIT_ID
```

[返回课程入口](index.md)
