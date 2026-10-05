# 07 撤销与恢复

Git 操作出错时，最重要的不是立即尝试某个“撤销命令”，而是先判断错误位于 Working Tree、Staging Area、本地 Commit，还是已经进入团队共享历史。

## 本章学习目标

### Level A：必须掌握

- 撤销前先使用 `status`、`diff`、`diff --staged` 和 `log` 判断状态。
- 使用 `restore` 恢复未暂存文件，使用 `restore --staged` 取消暂存。
- 已共享错误 Commit 通常优先考虑 `revert`。

### Level B：理解并能够查资料操作

- 使用 `commit --amend` 修正尚未共享的最近一次 Commit。
- 理解 `reset --soft` 与 `reset --mixed` 的影响范围。

### Level C：危险操作，了解即可

- `reset --hard` 和 reflog 恢复入口。
- 历史改写、clean 和强制推送不是新人普通修复手段。

## 7.1 撤销之前先判断状态

先执行只读检查：

```cmd
git status
git diff
git diff --staged
git log --oneline --decorate -5
```

然后按状态判断：

```text
文件改错，但还没有 add？
→ restore FILE

误 add，但代码修改仍要保留？
→ restore --staged FILE

最近一次本地 Commit 有小问题，且尚未共享？
→ amend，必要时再查 reset

错误 Commit 已经 Push 或进入共享 Branch？
→ 通常优先 revert
```

如果状态不清楚、正在 Merge/Rebase/Cherry-pick，或操作可能影响其他成员，先停止并向负责人确认。

## 7.2 场景一：文件改错但还没 add

`employee-list.txt` 已修改，但没有进入 Staging Area。先检查将被丢弃的内容：

```cmd
git status
git diff -- employee-list.txt
```

确认这些未暂存修改全部不需要后：

```cmd
git restore -- employee-list.txt
git status
```

状态变化：

```text
Working Tree：Modified
      │ restore
      ▼
恢复为 Staging Area 中的版本
```

【风险】未提交、未暂存的修改可能无法通过普通 Git 历史找回。

【执行前确认】阅读 `git diff`，确认路径和内容。

【更安全的替代方案】如果仍可能需要，先复制到明确的临时位置、创建合理 Commit，或按第 09 章判断是否适合 stash。

从其他 Commit 取出某个文件属于 Level B：

```cmd
git restore --source=HEAD~1 -- employee-list.txt
git diff -- employee-list.txt
```

它把指定版本的文件内容写入当前 Working Tree，仍需要后续检查和 Commit。

## 7.3 场景二：不小心 add 了

文件误进入 Staging Area，但工作内容仍然需要保留：

```cmd
git status
git diff --staged -- employee-list.txt
git restore --staged employee-list.txt
git status
git diff -- employee-list.txt
```

状态变化：

```text
Staged
  │ restore --staged
  ▼
Modified
```

这是取消暂存，不是删除 Working Tree 修改。执行后普通 `git diff` 应重新显示该文件差异。

## 7.4 场景三：最近一次本地 Commit 有小问题

Commit Message 写错，而且 Commit 尚未 Push：

```cmd
git status
git commit --amend -m "feat: display department name"
git log --oneline -3
```

漏掉一个本来就应该属于同一逻辑目的的小文件：

```cmd
git add MISSING_FILE
git diff --staged
git commit --amend --no-edit
git show HEAD
```

`amend` 不是编辑原 Commit，而是创建一个新的替代 Commit，因此 Commit ID 会改变。

【风险】已经 Push 或被其他成员基于其继续开发时，amend 会造成历史不一致。

【什么时候可以使用】最近一次 Commit 尚未共享，且修改确实属于同一逻辑目的。

【什么时候不应该使用】公共 Branch 或已共享功能 Branch，除非项目规则和负责人明确允许。

【执行前确认】`status`、`diff --staged`、`log` 和共享范围。

【更安全的替代方案】创建一个新的修正 Commit。

## 7.5 场景四：错误 Commit 已经共享

假设 Commit C 已经进入团队共享历史：

```text
A ── B ── C
          ↑
       错误修改
```

使用 revert 创建一个新 Commit，反向应用 C 的效果：

```cmd
git status
git revert COMMIT_ID
git log --oneline --decorate -5
```

结果：

```text
A ── B ── C ── D
               ↑
        Revert C 的效果
```

历史 C 仍然存在，团队成员可以看见发生过什么以及怎样修正。`revert` 不会把 Branch 指针偷偷移回旧位置。

发生 Conflict 时：

```cmd
git status
REM 根据规格编辑并测试冲突文件
git add CONFLICTED_FILE
git revert --continue
```

放弃尚未完成的 revert：

```cmd
git revert --abort
```

Revert 前仍应确认目标 Commit、影响范围和测试方式；反向修改也可能与当前代码产生冲突。

## 7.6 restore、amend、revert 的选择

| 错误位置 | 修改是否保留 | 常见选择 |
|---|---|---|
| Working Tree，未 add | 不保留 | `restore FILE` |
| Staging Area，代码要保留 | 保留 | `restore --staged FILE` |
| 最近本地 Commit，未共享 | 视情况 | `commit --amend` |
| 已共享 Commit | 用新历史修正 | `revert COMMIT_ID` |

同一个错误在不同状态下，安全做法不同。不要只根据“我想撤销”选择命令。

## 7.7 Level B/C：reset 的三种模式

`reset` 会移动当前 Branch 指针，并根据模式决定是否同时修改 Staging Area 和 Working Tree。

| 模式 | 移动 HEAD/Branch | 重置 Staging Area | 重置 Working Tree |
|---|---:|---:|---:|
| `--soft` | 是 | 否 | 否 |
| `--mixed` | 是 | 是 | 否 |
| `--hard` | 是 | 是 | 是 |

### reset --soft

```cmd
git reset --soft HEAD~1
git status
git diff --staged
```

最近 Commit 被移出当前 Branch 历史，但其内容保留在 Staging Area。适合尚未共享的个人历史重新组织，属于 Level B。

### reset --mixed

```cmd
git reset --mixed HEAD~1
git status
git diff
```

这是 `reset` 的默认模式。它移动 Branch 并重置 Staging Area，文件修改通常保留在 Working Tree，属于 Level B。

### reset --hard

```cmd
git reset --hard HEAD~1
```

> **危险操作：** 它同时移动 Branch、重置 Staging Area 和 Working Tree，未提交修改可能丢失。

【什么时候可以使用】范围明确的个人练习仓库，并且已经确认没有需要保留的本地修改。

【什么时候不应该使用】把它当作“Git 出问题先试一下”，或用于删除已共享公共历史。

【执行前确认】至少检查 `status`、两个 `diff`、`log`，确认 Branch、目标 Commit 和共享范围。

【更安全的替代方案】针对文件使用 restore；针对共享错误使用 revert；不确定时先创建备份 Branch 并向负责人确认。

不要组合 `reset + force push` 擅自处理公共历史。

## 7.8 reflog 作为本地恢复入口

reset、rebase 或误删本地 Branch 后，Commit 可能只是失去容易找到的 Branch 名。可以查看本地引用近期移动记录：

```cmd
git reflog --date=local
git branch rescue/NAME COMMIT_ID
git show COMMIT_ID
```

先创建 `rescue/NAME` 保存找到的位置，再检查内容。reflog 是本地、会过期的记录，不是 Remote 备份，也不能保证恢复未提交文件。完整场景放在第 09 章。

## 7.9 安全实验：比较 soft、mixed 和 revert

**环境：** Windows CMD，新建独立的 `git-undo-lab`，不连接 Remote。

先准备两个 Commit，并用 `lab/base` 保存第二个 Commit 的位置：

```cmd
mkdir git-undo-lab
cd git-undo-lab
git init -b develop
git config user.name "Git Learner"
git config user.email "learner@example.com"
echo version1>app.txt
git add app.txt
git commit -m "feat: add version 1"
echo version2>>app.txt
git commit -am "feat: add version 2"
git branch lab/base
```

在独立实验 Branch 观察 soft，并重新 Commit 以恢复干净状态：

```cmd
git switch -c experiment/soft
git reset --soft HEAD~1
git status
git diff --staged
git commit -m "lab: restore version 2 after soft"
```

从保存的 `lab/base` 建立另一个 Branch，观察 mixed：

```cmd
git switch lab/base
git switch -c experiment/mixed
git reset --mixed HEAD~1
git status
git diff
git add app.txt
git commit -m "lab: restore version 2 after mixed"
```

再次从 `lab/base` 建立 Branch，观察 revert：

```cmd
git switch lab/base
git switch -c experiment/revert
git revert --no-edit HEAD
git log --oneline --graph --decorate --all
```

验收：soft 后修改位于 Staging Area；mixed 后修改位于 Working Tree；revert 创建了新 Commit。实验不练习 `reset --hard`。完成后确认路径确实为独立练习目录，再通过文件资源管理器删除整个目录。

## 7.10 常见错误

- 不看 diff 就 restore：可能丢失仍需要的工作。
- 把取消暂存理解成删除修改：`restore --staged` 通常保留 Working Tree 内容。
- amend 已共享 Commit：会改变 Commit ID，引起历史不一致。
- revert 错误 Commit：先使用 `git show COMMIT_ID` 确认目标。
- 操作进行中又执行另一个历史命令：先用 `status` 判断应 continue 还是 abort。

## 本章必须掌握

- 先判断 Working Tree、Staging Area、Local History 和共享状态。
- restore、restore staged、amend 和 revert 解决不同层的问题。
- 已共享历史通常使用新的修正 Commit，而不是偷偷删除旧历史。

## 本章不要求现在掌握

- 独立使用 `reset --hard`。
- 使用 reflog 处理复杂事故。
- clean、rebase 和 force push。

## 进入下一章前确认

1. 文件改错但还没 add，执行 restore 前必须检查什么？
2. 误 add 后怎样保留代码、只取消暂存？
3. amend 为什么会改变 Commit ID？
4. 已共享错误 Commit 为什么通常优先 revert？
5. reset soft、mixed、hard 分别影响哪三层？
6. 为什么不能把 `reset --hard` 当作通用修复命令？
7. reflog 为什么不是可靠备份？

[上一章：团队协作与代码评审](06_teamwork_and_conflicts.md) · [下一章：标签与版本发布](08_tags_and_release.md)
