# Git 速查表（Cheat Sheet）

## 常用

```powershell
git status
git add .
git commit -m "msg"
git log --oneline --graph --decorate --all
git diff / git diff --staged
```

## 分支

```powershell
git branch -a
git switch -c feature/x
git merge feature/x
git rebase main
```

## 远程

```powershell
git remote -v
git push -u origin main
git pull --rebase
git fetch --all --prune
```

## 撤销

```powershell
git reset --soft/mixed/hard HEAD~1
git revert <commit>
git restore [--staged] <file>
git reflog
```

## 标签

```powershell
git tag -a v1.0 -m "release"
git push origin --tags
```
