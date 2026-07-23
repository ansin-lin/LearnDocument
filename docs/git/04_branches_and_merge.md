# 04 分支、合并与冲突

## 4.1 分支解决什么问题

分支让不同任务在相互隔离的提交线上进行。开发登录功能时，可以从稳定的 `main` 创建 `feature/login`；修复紧急问题时，则创建独立的修复分支。

分支不是项目文件夹的完整复制，而是指向提交的轻量指针。新分支创建时通常与当前分支指向同一个提交。

## 4.2 查看、创建和切换分支

```powershell
git branch
git branch -v
git switch -c feature/login
git switch main
```

- `git branch`：列出本地分支，`*` 表示当前分支
- `git switch -c`：创建并切换到新分支
- `git switch`：切换到已有分支

切换前执行 `git status`。如果未提交修改会被切换覆盖或产生冲突，Git 通常会拒绝切换。此时应完成当前提交、谨慎使用 stash，或撤销不需要的修改。

命名应遵守团队规则。常见形式包括 `feature/login`、`fix/email-validation`、`docs/setup-guide`，名称应能对应任务或 Issue。

## 4.3 merge 的两种常见结果

在 `main` 上合并功能分支：

```powershell
git switch main
git merge feature/login
```

### 快进合并

如果 `main` 从分支创建后没有新提交，Git 只需把 `main` 指针向前移动，这叫 fast-forward。

### 合并提交

如果 `main` 和功能分支都产生了新提交，Git 可能创建一个有两个父提交的 merge commit，从而保留分叉历史。

团队也可能通过托管平台选择 Squash Merge 或 Rebase Merge。合并方式应由仓库规则决定，不能只凭个人喜好。

## 4.4 冲突是什么

Git 能自动合并不同文件或同一文件中互不影响的修改。当两个分支修改同一处内容且 Git 无法判断应保留哪一方时，会产生冲突。

冲突文件通常由三种标记分隔：`<<<<<<< HEAD` 表示当前分支一侧开始，`=======` 分隔双方内容，`>>>>>>> feature/login` 表示另一分支一侧结束。

这些标记不是最终答案。解决者需要理解双方意图，编辑为正确业务结果，并删除全部标记。

## 4.5 merge 冲突处理流程

```powershell
git merge feature/login
git status
```

典型冲突信息类似：

```text
CONFLICT (content): Merge conflict in <file>
Automatic merge failed; fix conflicts and then commit the result.
```

第一行指出冲突文件，第二行表示自动合并已经停止，需要人工处理。

发生冲突后：

1. 用 `git status` 找到 `both modified` 文件。
2. 阅读冲突两侧内容和相关需求。
3. 编辑文件，删除冲突标记并形成最终内容。
4. 运行测试、格式检查或构建。
5. 暂存解决后的文件并完成合并提交。

```powershell
git add <conflicted-file>
git diff --staged
git commit
```

不带 `-m` 的 `git commit` 会打开第 01 章配置的编辑器。保留或修改 Git 生成的合并说明，保存文件并关闭编辑器后才会完成提交。如果编辑器未配置或不熟悉操作，也可以明确填写说明：

```powershell
git commit -m "merge: resolve login conflict"
```

如果发现不应继续合并，可在尚未完成合并提交时返回合并前状态：

```powershell
git merge --abort
```

执行合并前应保持干净工作区，否则本地修改可能使恢复更困难。

## 4.6 实验：制造并解决冲突

**环境与范围：** Windows PowerShell；在新目录中执行。实验只影响 `git-merge-lab`。

```powershell
New-Item -ItemType Directory git-merge-lab
Set-Location git-merge-lab
git init -b main
git config user.name "Git Learner"
git config user.email "learner@example.com"
Set-Content message.txt "message=original"
git add message.txt
git commit -m "docs: add original message"

git switch -c feature/message
Set-Content message.txt "message=feature"
git commit -am "feat: change feature message"

git switch main
Set-Content message.txt "message=main"
git commit -am "fix: change main message"
git merge feature/message
```

`git merge` 应报告冲突。检查状态和文件：

```powershell
git status
Get-Content message.txt
```

把文件修改成双方确认的最终内容，例如：

```powershell
Set-Content message.txt "message=resolved"
git add message.txt
git diff --staged
git commit -m "merge: resolve message conflict"
git log --oneline --graph --decorate --all
git status
```

验证结果：历史图中应能看到两个分支和合并提交，工作区应保持干净。

## 4.7 rebase 的作用和边界

rebase 会把当前分支的提交重新应用到新的基础提交上，使历史更线性：

```powershell
git switch feature/login
git rebase main
```

rebase 会创建新的提交对象，因此提交 ID 会改变。适合整理尚未与他人共享的个人功能分支；不要擅自 rebase 团队成员正在基于其开发的公共分支。

发生冲突时：

```powershell
git status
# 编辑冲突文件并完成测试
git add <conflicted-file>
git rebase --continue
```

放弃整个 rebase：

```powershell
git rebase --abort
```

如果已经推送过个人分支，rebase 后是否允许使用 `git push --force-with-lease` 必须遵守团队规则。不要使用无保护的 `--force` 覆盖他人提交。

## 4.8 清理已合并分支

确认功能已经合并后再删除本地分支：

```powershell
git branch --merged
git branch -d feature/login
```

`-d` 会拒绝删除尚未合并的分支。`-D` 会强制删除，可能丢失仅由该分支引用的提交，使用前必须确认提交已存在于其他分支或远程仓库。

## 4.9 常见错误

```text
error: you need to resolve your current index first
```

当前存在尚未解决的冲突。执行 `git status`，完成冲突解决并提交，或者使用当前操作对应的 `--abort` 返回操作前状态。

```text
fatal: There is no merge to abort (MERGE_HEAD missing).
```

当前没有正在进行的 merge。先通过 `git status` 判断实际状态，不要把 `--abort` 当成普通撤销命令。

## 4.10 本章总结

- 分支是指向提交的轻量指针。
- merge 保留分支结合关系，rebase 会重写当前分支提交。
- 解决冲突的目标是形成正确代码，而不只是删除标记。
- 冲突处理后必须重新测试；不继续操作时使用对应的 `--abort`。

## 练习

1. 创建一个没有冲突的功能分支，观察是否发生快进合并。
2. 重复冲突实验，但在解决前执行 `git merge --abort`。
3. 解释为什么公共分支通常不应随意 rebase。

### 自检提示

- 快进合并后通常不会出现新的 merge commit。
- 冲突期间 `git status --short` 会用 `UU` 标记双方都修改的文件。
- `git merge --abort` 后，`git status` 应回到合并前状态。

[上一章：基本命令与日常提交](03_common_commands.md) · [下一章：远程仓库与同步](05_remote_repo.md)
