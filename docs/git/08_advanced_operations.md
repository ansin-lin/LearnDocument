# 08 常用高级操作（stash/cherry-pick/reflog/submodule）

## 8.1 stash：临时搁置工作

```powershell
git stash push -m "WIP: 登录页"
git stash list
git stash apply  stash@{0}
git stash drop   stash@{0}
git stash pop    # apply + drop
```

## 8.2 cherry-pick：挑一条提交到当前分支

```powershell
git cherry-pick <commit>
```

## 8.3 reflog：参见第 06 章（找回一切移动过的头）

## 8.4 submodule：引入其它仓库为子模块

```powershell
git submodule add git@github.com:someone/lib.git libs/lib
git submodule update --init --recursive
```

---

