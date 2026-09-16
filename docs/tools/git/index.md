# Git 教程

Git 是用于记录文件变更、协同开发和追踪发布版本的分布式版本控制系统。本教程面向第一次接触 Git 的学习者，示例默认在 **Windows CMD** 中执行。

## 学习目标

完成本教程后，你能够：

- 解释工作区、暂存区、本地仓库、远程仓库、提交、分支和 `HEAD`
- 独立完成修改、检查、暂存、提交和查看历史
- 使用分支开发，并处理常见合并冲突
- 与 GitHub、GitLab 等远程仓库安全地同步代码
- 根据“是否提交、是否推送”选择合适的撤销方式
- 按团队流程创建分支、提交、发起 Pull Request 并处理 Review

## 开始前准备

- 能够打开 CMD，并使用 `cd`、`dir` 等基础命令
- 准备一个练习目录，不要在重要项目中试验 `reset --hard`、rebase 或强制推送
- 需要练习远程协作时，再准备 GitHub 或 GitLab 账号

> 本教程使用 `example.com` 邮箱和示例仓库地址。不要复制真实密码、访问令牌、私钥或公司内部地址到教学仓库。

## 常用协作术语预览

这些词会在后续章节反复出现：

| 术语 | 含义 |
|---|---|
| Repository（仓库） | 保存项目文件和 Git 历史的位置，简称 repo |
| Issue | GitHub/GitLab 中记录任务、问题或需求的条目 |
| Pull Request / Merge Request | 请求把一个分支合并到另一个分支，并在合并前进行审查；分别简称 PR、MR |
| Review / Reviewer | 对变更进行检查的过程 / 执行检查的人 |
| CI | Continuous Integration，持续集成；自动执行构建、测试和检查 |
| CD | Continuous Delivery/Deployment，持续交付或持续部署；把验证后的版本交付或部署到环境 |
| Pipeline（流水线） | 按顺序执行构建、测试、发布等自动化步骤的流程 |
| Credential（凭据） | 用于证明身份的信息，例如 SSH 私钥或由凭据管理器保存的访问令牌 |
| Protected branch（受保护分支） | 由平台限制直接推送、删除或强制改写的分支 |
| Draft PR | 尚未准备合并、用于提前沟通和获得反馈的草稿 PR |

术语表只帮助首次识别，具体使用方式会在对应章节展开。

## 学习顺序

1. [安装与初始配置](01_install_and_config.md)
2. [基本概念与工作原理](02_basic_concepts.md)
3. [基本命令与日常提交](03_common_commands.md)
4. [分支、合并与冲突](04_branches_and_merge.md)
5. [远程仓库与同步](05_remote_repo.md)
6. [团队协作与代码评审](06_teamwork_and_conflicts.md)
7. [撤销与恢复](07_undo_and_reset.md)
8. [标签与版本发布](08_tags_and_release.md)
9. [常用高级操作](09_advanced_operations.md)
10. [Git 命令速查表](cheatsheet.md)

章节仍按从基础到团队协作的顺序学习，但不同内容不要求一次达到相同熟练度：

### Level 1：新人必须掌握

- 工作区、暂存区、提交、分支和 `HEAD`
- `status`、`diff`、`add`、`commit` 和 `.gitignore`
- `clone`、`fetch`、`pull`、`push`、`origin` 和 `origin/main`
- `branch`、`switch`、merge 和常见 conflict
- PR/MR 与 Review 的基本流程

### Level 2：日本项目必须理解

- Ticket → Branch、Source / Target Branch
- Review 指摘、指摘対応、再 Review
- CI、Protected Branch 与 Merge Gate
- Branch Strategy、影响范围和既有项目参画
- 使用 Git 历史调查既有代码

### Level 3：工作后逐步掌握

- stash、amend、reset、revert、cherry-pick
- rebase、reflog、`force-with-lease` 和 tag

### Level 4：项目需要时

- submodule、Git LFS、fork 与名为 `upstream` 的远程仓库、interactive rebase、signed commit

Level 4 只表示识别这些名称，不代表本教程逐项展开。遇到采用这些能力的项目时，再按照项目手顺和对应专题学习；不要为了练习主动把它们引入普通项目。

## 完成主线后的关键自检

学完第 06 章后，应能不依赖命令速查回答：

1. `git push` 把哪个本地分支的提交发送到哪个服务器分支？
2. `origin/main` 保存在哪里，它与服务器上的 `main` 为什么可能不同？
3. PR/MR 的 source branch 和 target branch 分别是什么？
4. PR 合并后，为什么自己电脑上的目标分支还需要 fetch/pull？

如果无法画出“本地分支 → 服务器功能分支 → PR/MR → 服务器目标分支 → 本地更新”，请回看[远程仓库与同步](05_remote_repo.md)和[团队协作与代码评审](06_teamwork_and_conflicts.md)，并在练习仓库中用 `git branch -vv` 与提交图核对实际状态。

## 命令示例约定

- `FILE_PATH`、`COMMIT_ID`、`REPOSITORY_URL` 等全大写名称是占位符，执行时必须替换为实际值。CMD 会把 `<` 和 `>` 解释为重定向符号，因此命令示例不使用尖括号占位符。
- `main` 表示示例默认分支；真实项目可能使用其他名称，以仓库设置为准。
- 命令执行前先确认当前目录：

```cmd
cd
git status
```

- 看到错误时先阅读完整信息，不要立即执行强制删除、强制推送或历史改写命令。

## 练习目录清理

练习仓库统一使用 `git-*-lab` 名称。完成练习后：

1. 用 `git status` 确认是否还有需要保留的内容。
2. 执行 `cd ..` 离开练习目录。
3. 通过文件资源管理器删除对应练习目录。

不要在不熟悉路径和递归删除行为时复制删除命令。删除练习目录会同时删除其中的本地 Git 历史。
