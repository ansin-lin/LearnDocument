# 09 常用高级操作

本章处理日常主线之外的特殊场景。重点是识别问题、知道应该查什么，并理解历史改写风险；不是要求新人把所有命令当作每天使用的工具。

## 本章学习目标

### Level A：本章无新增必会命令

- 特殊操作前仍应先确认 `status`、当前 Branch、共享范围和恢复方式。

### Level B：理解并能够查资料操作

- stash：短期保存尚不能 Commit 的工作。
- cherry-pick：把指定 Commit 的修改应用到当前 Branch，并创建新 Commit。

### Level C：了解用途与风险

- rebase、interactive rebase、reflog 和 submodule。
- 已共享 Commit 不应在不了解项目规则时自行改写。

## 9.1 stash：开发一半需要切换任务

场景：正在 `feature/APP-123` 开发，修改尚不能形成正式 Commit，负责人要求立即切换到 hotfix Branch 调查紧急障碍。

```text
未完成 Working Tree 修改
        │ stash
        ▼
短期保存记录 + 较干净的 Working Tree
        │ 处理紧急任务后恢复
        ▼
继续原任务
```

执行前确认状态并添加可识别说明：

```cmd
git status
git stash push -m "WIP: APP-123 department display"
git stash list
git status
```

- `stash push`：创建一条临时保存记录，并把被保存的修改从 Working Tree 移开。
- `-m`：添加说明，避免多个 stash 难以区分。
- `stash list`：按新到旧列出记录。

stash 不是 Commit，也不是 Remote 备份。默认主要保存已跟踪文件的修改，不包含普通 Untracked 文件。确实需要包含 Untracked 文件时：

```cmd
git stash push -u -m "WIP: APP-123 include new file"
```

不要随意使用 `-a` 保存被忽略的依赖或构建目录，这可能生成巨大 stash。

恢复前先查看内容：

```cmd
git stash show -p "stash@{0}"
git stash apply "stash@{0}"
git status
```

- `apply`：应用修改，但保留 stash 记录，便于确认成功后再删除。
- `stash@{0}`：列表中最新一条；使用前仍应核对说明和补丁。

确认应用、测试和后续 Commit 完成后：

```cmd
git stash drop "stash@{0}"
```

也可以使用：

```cmd
git stash pop
git status
git stash list
```

`pop` 会尝试应用并删除记录；如果发生 Conflict，不能假定记录已经删除，必须检查 `stash list`。

stash 适合短期任务切换，不适合长期保存重要工作。

## 9.2 cherry-pick：只取得一个 Commit

另一个 Branch 有一个已经确认的 Bug Fix，但当前 Branch 不需要整合它的全部历史：

```text
来源 Branch：A ── B ── C
                         ↑
                      Bug Fix

当前 Branch：A ── D ── E
```

只把 C 的修改应用到当前 Branch：

```cmd
git status
git cherry-pick COMMIT_ID
```

结果：

```text
当前 Branch：A ── D ── E ── C'
```

`C'` 的内容来源于 C，但它是在当前位置创建的新 Commit，因此 Commit ID 通常不同。

发生 Conflict 后：

```cmd
git status
REM 根据规格编辑和测试冲突文件
git add CONFLICTED_FILE
git cherry-pick --continue
```

放弃尚未完成的操作：

```cmd
git cherry-pick --abort
```

不要 cherry-pick 大型 Merge Commit 或大量相互依赖的 Commit 来代替正常 Merge，这可能遗漏依赖或制造重复历史。

## 9.3 rebase：把个人提交重放到新基线

场景：`feature/APP-123` 从较早的 `develop` 创建，之后 `develop` 增加了 Commit F：

```text
          D ── E  feature/APP-123
         /
A ── B ── C ── F  develop
```

Rebase 会把 D、E 的修改重新应用到 F 后面，产生新的 D'、E'：

```text
A ── B ── C ── F ── D' ── E'  feature/APP-123
```

在项目明确允许、且确认是个人未共享历史时，形式上可能执行：

```cmd
git status
git switch feature/APP-123
git fetch origin
git rebase origin/develop
```

发生 Conflict 时：

```cmd
git status
REM 编辑、测试并暂存冲突文件
git add CONFLICTED_FILE
git rebase --continue
```

放弃整个 rebase：

```cmd
git rebase --abort
```

【风险】rebase 会重新创建 Commit，改变 Commit ID。对已共享历史执行会让其他成员的历史与服务器不一致。

【什么时候可以使用】项目明确采用 rebase，且目标 Commit 属于自己尚未共享的个人历史。

【什么时候不应该使用】公共 Branch，或其他成员已经基于这些 Commit 开发。

【执行前确认】当前 Branch、Working Tree、Commit 范围、是否 Push、项目策略和恢复入口。

【更安全的替代方案】按项目规则使用 Merge，或向负责人确认同步方式。

如果 rebase 后项目允许更新个人 Remote Branch，可能要求 `git push --force-with-lease`。它仍然是覆盖式更新，只比无保护 `--force` 多一层远程状态检查；新人不得自行使用。

## 9.4 Merge 与 rebase 的区别

```text
Merge：保留两条开发历史，再创建整合关系
Rebase：重新播放 Commit，形成较线性的历史，但 Commit ID 改变
```

两者都可能整合修改，也都可能发生 Conflict。哪一种“更好”没有脱离项目规则的统一答案。新人只需要根据仓库手顺操作，不参与复杂历史风格争论。

## 9.5 Interactive Rebase：整理个人历史

对尚未共享的最近三个个人 Commit，可以打开交互计划：

```cmd
git status
git rebase -i HEAD~3
```

常见动作：

- `pick`：保留 Commit。
- `reword`：修改 Commit Message。
- `squash`：合并到前一个 Commit，并编辑说明。
- `fixup`：合并到前一个 Commit，丢弃当前说明。

```text
整理前：A ── B ── C
整理后：A ── D
```

D 是新 Commit，B、C 的原 ID 不再位于当前 Branch 历史。该操作属于 Level C，只建议用于自己尚未共享的历史。

## 9.6 reflog：寻找看似丢失的 Commit

reset、rebase 或误删本地 Branch 后，可以查看本地 HEAD 和引用近期移动记录：

```cmd
git reflog --date=local
```

找到疑似 Commit 后，先建立恢复 Branch，再检查：

```cmd
git branch rescue/APP-123 COMMIT_ID
git show COMMIT_ID
```

reflog 是本机记录、会过期，并且通常不会在另一台电脑出现。它不是 Remote 备份，也不能保证恢复未提交文件。

## 9.7 submodule：引用另一个 Repository

Submodule 让主 Repository 记录另一个 Repository 的特定 Commit。它不是普通目录复制。

项目已经采用 Submodule 时，clone 可能需要：

```cmd
git clone --recurse-submodules REPOSITORY_URL
```

已经 clone 后初始化：

```cmd
git submodule update --init --recursive
```

主 Repository 记录的是子模块 Commit 指针，不会自动升级到子模块最新 Branch。子模块常处于 detached HEAD 状态；更新、删除和提交都应遵守项目手顺。

主动添加 Submodule：

```cmd
git submodule add REPOSITORY_URL libs/shared-lib
git status
git commit -m "build: add shared library submodule"
```

普通 Java、React 或 Vue 新人不要求主动引入 Submodule，只需知道不能把既有 Submodule 当作普通目录随意删除或提交。

## 9.8 场景练习

1. APP-123 开发一半，需要立即切换任务，且修改还不能形成正式 Commit。说明 stash 前后应检查什么。
2. 维护 Branch 只需要另一个 Branch 中一个已确认 Bug Fix。说明 cherry-pick 的结果为什么是新 Commit。
3. 已 Push 的个人 Branch 准备 rebase。列出执行前必须确认的事项。
4. reset 后看不到原 Commit。说明怎样使用 reflog 建立恢复入口，以及它为什么不是万能备份。
5. clone 后发现 Submodule 目录为空。说明应查阅什么命令和项目手顺。

## 本章必须掌握

- 识别 stash、cherry-pick、rebase、reflog 和 submodule 分别解决什么问题。
- 所有特殊操作前先确认状态、Branch 和共享范围。
- rebase 和覆盖式 Push 可能改变共享历史。

## 本章不要求现在掌握

- 独立整理复杂历史。
- 对已 Push Branch 执行 rebase 或 force push。
- 设计或维护 Submodule 架构。

## 进入下一章前确认

1. stash 与 Commit、Remote 备份有什么区别？
2. apply 与 pop 的基本区别是什么？
3. cherry-pick 为什么通常产生不同 Commit ID？
4. rebase 为什么会改变 Commit ID？
5. Merge 和 rebase 在历史形状上有什么区别？
6. 哪些历史不应自行 interactive rebase？
7. reflog 为什么只能作为本地恢复入口？

[上一章：标签与版本发布](08_tags_and_release.md) · [下一章：Git 新人综合实战](10_comprehensive_practice.md)
