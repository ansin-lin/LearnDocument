# 06 团队协作与代码评审

团队使用 Git 的目标不只是“把代码推上去”，而是让每次变更可以理解、验证、审查、合并和追溯。具体分支命名、审批人数和合并方式以所在项目规则为准。

## 6.1 一个完整的功能开发流程

以下流程适用于常见的短生命周期功能分支。短生命周期表示分支只服务一个任务，并尽快通过审查合并，避免长期偏离主分支。

1. 确认任务、验收条件和目标分支。
2. 更新本地默认分支。
3. 从最新默认分支创建功能分支。
4. 完成小而清晰的提交，并在本地验证。
5. 推送分支，创建 Pull Request 或 Merge Request。
6. 处理 CI 和 Review 意见。
7. 按团队策略合并，确认部署或发布结果。
8. 更新本地默认分支并清理已合并分支。

GitHub 通常称 Pull Request（PR），GitLab 通常称 Merge Request（MR），核心目的都是在合并前审查分支差异。Issue 是平台中记录需求、缺陷或任务的条目，分支和 PR 通常会引用对应 Issue 编号。

## 6.2 从最新 main 创建任务分支

开始前确认没有遗留修改：

```powershell
git status
git switch main
git fetch origin
git pull --ff-only
git switch -c feature/123-user-login
```

分支名中的 `123` 可以对应 Issue 或任务编号。日本项目中可能使用工单番号、チケット番号或团队规定的前缀，必须以仓库规范为准。

不要在本地落后的 `main` 上长期开发，也不要为了更新代码而在状态不清楚时执行 rebase 或强制推送。

## 6.3 形成可审查提交

每完成一个逻辑完整的修改：

```powershell
git status
git diff
git add <changed-file>
git diff --staged
# 运行项目规定的测试、格式检查或构建
git commit -m "feat: validate login request"
```

一个提交应尽量只包含一个目的。以下内容通常应拆开：

- 功能开发与大规模格式化
- 问题修复与无关依赖升级
- 代码重构与行为变更

拆分后，Reviewer 更容易判断每次修改是否正确，也更容易独立回退。

## 6.4 推送并创建 PR

```powershell
git push -u origin feature/123-user-login
```

PR 描述至少应包含：

- 目的：解决哪个任务或问题
- 变更：主要修改和设计选择
- 影响范围：接口、数据库、配置、画面或兼容性
- 验证：执行了哪些测试及结果
- 补充材料：必要的截图、日志片段或迁移说明

不要在 PR 中粘贴访问令牌、个人信息、生产数据或完整内部日志。尚未完成但希望提前获得方向反馈时，可以创建 Draft PR（草稿 PR）；它表示变更尚未准备合并，但允许团队提前查看方向和提出意见。完成开发、自测和说明后，再把它标记为 Ready for review。

## 6.5 处理 Review

Review 是对代码质量和风险的共同确认，不只是寻找语法错误；Reviewer 是负责检查本次变更的人。收到意见后：

1. 先理解问题和期望结果。
2. 不明确时在对应评论中确认。
3. 在同一功能分支完成修改和测试。
4. 推送新提交，让 PR 自动更新。
5. 回复修改内容和验证方式。

不要为了隐藏 Review 过程而频繁强制改写已共享分支。团队要求合并前整理提交时，再按规定 squash 或 rebase。

## 6.6 同步 main 并处理冲突

功能分支开发期间，目标分支可能继续前进。先取得最新状态：

```powershell
git fetch origin
git log --oneline --graph --decorate --all
```

团队允许 merge 时：

```powershell
git switch feature/123-user-login
git merge origin/main
```

团队要求个人功能分支保持线性时：

```powershell
git switch feature/123-user-login
git rebase origin/main
```

rebase 会改变功能分支提交 ID。分支已推送后，只有在确定该分支由自己独占且团队允许时，才使用：

```powershell
git push --force-with-lease
```

`--force-with-lease` 会在远程分支出现意外新提交时拒绝覆盖，比 `--force` 安全，但仍属于历史改写，不能绕过团队策略。

冲突处理后必须重新运行测试。冲突标记删除不代表业务逻辑已经正确。

## 6.7 CI 与合并策略

CI 是 Continuous Integration（持续集成）：每次推送或更新 PR 时，由平台自动执行构建、测试、代码格式、静态分析或安全检查。Pipeline（流水线）是这些自动化步骤按依赖关系组成的执行流程。

常见合并条件包括：

- 必要 Reviewer 已批准
- 自动测试、构建、静态检查和安全检查通过
- PR 分支已经满足目标分支同步要求
- 数据库迁移、配置和部署影响已有说明
- 没有未解决的 Review 线程

常见合并方式：

| 方式 | 特点 | 常见考虑 |
|---|---|---|
| Merge commit | 保留分支和合并关系 | 历史完整，但提交图更复杂 |
| Squash merge | 将 PR 压缩为一个提交 | 主分支简洁，但丢失分支内提交边界 |
| Rebase merge | 逐个重放提交 | 历史线性，要求提交本身足够清晰 |

没有一种方式适合所有团队。项目应统一策略，开发者按仓库设置执行。

## 6.8 合并后的清理

PR 合并后：

```powershell
git switch main
git pull --ff-only
git branch -d feature/123-user-login
git fetch --prune origin
```

如果远程平台没有自动删除功能分支，并且确认已合并：

```powershell
git push origin --delete feature/123-user-login
```

删除前先确认分支中没有未合并提交。默认分支和发布分支的删除由管理员管理。

## 6.9 事故处理原则

### 错误提交到功能分支

尚未推送时可以 amend、reset 或 rebase；已共享时优先新提交修正，是否改写由团队决定。

### 错误提交到公共分支

不要私自 reset 后强推。保留现场，通知负责人，通常通过 revert 和新的 PR 修正。

### 推送了秘密信息

立即通知负责人并撤销或轮换凭据。删除文件和修改历史不能让已经泄露的秘密重新安全；还需要按平台和安全流程清理历史、缓存与日志。

### CI 失败

先阅读失败步骤和首个根因错误，在本地复现并修复。一个红色“失败”状态只表示流水线中至少一个 Job（流水线中的独立作业）失败，应打开具体 Job 和步骤查看日志。不要通过重复推送或关闭检查绕过质量门禁。

## 6.10 日本项目常见 Git 术语

| 中文 | 日语现场常见表达 | 英文 |
|---|---|---|
| 分支 | ブランチ | branch |
| 提交 | コミット | commit |
| 拉取 | プル | pull |
| 推送 | プッシュ | push |
| 合并 | マージ | merge |
| 冲突 | コンフリクト | conflict |
| 代码评审 | コードレビュー | code review |
| 修改意见 | レビュー指摘 | review comment/finding |
| 处理意见 | 指摘対応 | address review feedback |
| 目标分支 | マージ先ブランチ | target branch |

这些词用于理解沟通，不代替项目的正式分支和 Review 规则。

## 6.11 团队实验建议

两名学习者可以使用练习仓库完成以下流程：

1. A 创建 Issue 和功能分支，提交并创建 Draft PR。
2. B 对同一行创建另一分支修改，提交 Review 意见。
3. A 同步 `main`，解决冲突并运行验证。
4. B 再次 Review，确认 CI 后合并。
5. 双方更新本地 `main` 并清理分支。

实验应使用无生产数据、无真实秘密的仓库。平台权限、分支保护和 CI 结果依赖外部配置，需要由教师或仓库管理员预先准备。

## 6.12 本章总结

- 团队协作以可理解、可验证和可追溯为目标。
- 小提交、清晰 PR、自动检查和认真 Review 共同降低合并风险。
- rebase 和强制推送只用于团队允许的范围。
- 公共分支事故应保留现场并协调修复，不能个人强行改写。

## 练习

1. 为一个小修改编写包含目的、影响和测试的 PR 描述。
2. 比较三种合并方式对主分支历史的影响。
3. 说明 `--force-with-lease` 比 `--force` 多了什么保护，以及为什么仍需谨慎。

### 自检提示

- PR 描述至少应包含目的、主要变更、影响范围和验证方式。
- `--force-with-lease` 会在远程分支出现本地未知的新提交时拒绝覆盖，但仍会改写已共享历史。
- 合并完成后，本地 `main` 应与 `origin/main` 同步，已合并功能分支可以安全删除。

[上一章：远程仓库与同步](05_remote_repo.md) · [下一章：撤销与恢复](07_undo_and_reset.md)
