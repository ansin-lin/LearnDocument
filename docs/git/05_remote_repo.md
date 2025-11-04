# 05 远程仓库（GitHub/GitLab）

## 5.1 关联远程

```powershell
git remote add origin git@github.com:<you>/hello-git.git
git remote -v
```

## 5.2 推送/拉取/抓取

```powershell
git push -u origin main    # 首次推送并建立跟踪
git pull                   # 拉取并合并
git fetch                  # 只下载不合并
git push origin feature/login
```

## 5.3 克隆与跟踪分支

```powershell
git clone git@github.com:<you>/repo.git
git branch -r
git switch -c feature origin/feature
```

## 5.4 远程分支删除与重命名

```powershell
git push origin --delete old-branch
git branch -m old new
git push origin -u new
```

---

## 实验：把本地仓库推到 GitHub

1) 本地创建 `demo05`，提交 README  
2) 在 GitHub 新建 `demo05` 空仓库（SSH）  
3) `git remote add origin ...` → `git push -u origin main`  
4) 在 GitHub 网页修改文件并 `Commit`，本地 `git pull` 同步

### 练习题

1. `pull` 与 `fetch` 的区别？
2. 删除远程分支的命令是什么？
