# 08 标签与版本发布

项目历史可能包含数百个 Commit。本章解决一个发布现场问题：怎样清楚标记“客户验收或生产环境对应哪一个 Commit”。

## 本章学习目标

### Level A：必须理解

- 说明为什么发布版本需要 Tag。
- 区分会移动的 Branch 与通常固定的 Tag。
- 理解本地 Tag、Remote Tag 和平台 Release 不是同一个概念。

### Level B：理解并能够查资料操作

- 创建、查看和 Push 一个经过确认的 Tag。
- 区分 Lightweight Tag 与 Annotated Tag。

### Level C：项目需要时再学习

- 删除或修正已共享 Tag、签名 Tag 和 Release Automation。
- 新人不得自行移动已经发布或被流水线使用的 Tag。

## 8.1 为什么需要 Tag

客户询问“生产环境运行的是哪个版本”时，只回答一段较长 Commit ID 不便于沟通和发布管理。Tag（标签）可以给重要 Commit 一个稳定、易识别的名称：

```text
A ── B ── C ── D ── E
          ↑           ↑
        v1.0.0       develop
```

`v1.0.0` 标记 Commit C。以后 `develop` 继续前进，Tag 通常仍指向 C，因此团队可以追踪发布、验收或交付基线。

## 8.2 Branch 与 Tag 的区别

```text
Branch：通常随着新 Commit 向前移动
Tag：通常固定指向某个重要 Commit
```

```text
A ── B ── C ── D
          ↑       ↑
        v1.0.0   develop
```

Tag 不是代码副本。它是指向某个 Git 对象的名称，日常发布 Tag 通常指向 Commit。

## 8.3 Tag 的常见用途

- 发布版本，例如 `v1.2.3`。
- 系统测试或客户验收基线。
- 里程碑和交付版本。
- 部署流水线的触发条件。

> **新人常见误解：** 创建 Tag 不一定等于已经 Build、Deploy 或 Release。具体行为由项目 CI/CD 和发布手顺决定。

## 8.4 Lightweight 与 Annotated Tag

Lightweight Tag（轻量标签）只是一个名称引用：

```cmd
git tag v1.0.0
```

Annotated Tag（附注标签）还保存 Tagger、时间和 Message 等元数据，正式发布通常优先采用：

```cmd
git tag -a v1.1.0 -m "release: version 1.1.0"
```

- `-a`：创建 Annotated Tag。
- `-m`：提供 Tag Message。
- 未指定 Commit 时，默认标记当前 `HEAD`。

为已经确认的历史 Commit 创建 Tag：

```cmd
git tag -a v1.0.1 COMMIT_ID -m "release: version 1.0.1"
```

执行前必须使用 `log` 和 `show` 核对 Commit、测试结果及发布审批，不能只看当前 Working Tree。

## 8.5 查看和验证 Tag

```cmd
git tag --list
git show v1.1.0
```

- `tag --list`：列出本地 Tag 名称。
- `show TAG_NAME`：显示 Tag 元数据及其指向的 Commit 内容。

两条命令都只读取本地状态，不会连接服务器。

## 8.6 本地 Tag 与 Remote Tag

普通 `git push` 不保证上传所有本地 Tag。Push 单个已确认 Tag：

```cmd
git push origin v1.1.0
```

状态变化：

```text
Local Tag v1.1.0
        │ push
        ▼
Remote Tag v1.1.0
```

Push 后在平台确认 Tag 指向的 Commit，以及是否触发发布流水线。

下面命令会 Push 所有本地 Tag：

```cmd
git tag --list
git push origin --tags
```

它可能把练习、内部或错误 Tag 一起上传，因此不是新人默认操作，只能按项目发布手顺使用。

读取 Remote 公布的 Tag：

```cmd
git ls-remote --tags origin
```

该命令需要网络和仓库读取权限，不修改本地 Tag。

## 8.7 Tag 与 Release 的区别

Git Tag 是 Git 中的引用。GitHub/GitLab Release 是平台基于某个 Tag 提供的发布记录，可能包含：

- 发布说明。
- 构建产物或附件。
- 下载链接。
- 发布状态和时间。

```text
Git Commit
   ↑
Git Tag
   ↑ 平台可基于该 Tag 创建
GitHub / GitLab Release
```

是否自动构建、部署或创建 Release，取决于项目流水线，Tag 本身不会保证这些动作完成。

## 8.8 版本号基础

项目常见 `MAJOR.MINOR.PATCH`，例如 `2.3.1`：

- `MAJOR`：包含不兼容变更。
- `MINOR`：增加向后兼容功能。
- `PATCH`：向后兼容的问题修复。

是否严格采用语义化版本、是否使用 `v` 前缀以及预发布版本怎样命名，以项目规则为准。

## 8.9 Level C：删除或修正 Tag

删除本地 Tag：

```cmd
git tag -d v1.1.0
```

删除 Remote Tag：

```cmd
git push origin --delete v1.1.0
```

二者是独立状态变化。删除 Remote Tag 会影响其他成员和流水线，必须经过发布流程确认。

【风险】移动或重建同名已共享 Tag，会让不同成员看到不同的“同一版本”，破坏可追溯性。

【什么时候可以使用】明确确认 Tag 尚未发布、尚未被他人使用，且项目允许修正。

【什么时候不应该使用】Tag 已交付客户、部署生产或触发发布流水线。

【执行前确认】Tag 指向、Remote 状态、发布记录、审批和影响范围。

【更安全的替代方案】创建新的修正版本，例如 `v1.1.1`。

## 8.10 实验：创建本地练习 Tag

**范围：** 使用个人练习仓库，只创建本地 Tag，不 Push、不触发发布。

```cmd
git status
git log --oneline -3
git tag -a v0.1.0 -m "release: practice version 0.1.0"
git tag --list
git show v0.1.0
```

确认完成后删除练习 Tag：

```cmd
git tag -d v0.1.0
```

验收标准：能够说明 Tag 指向哪个 Commit，并确认实验没有改变 Branch 历史或 Remote Repository。

## 本章必须掌握

- Tag 用来稳定标记重要 Commit。
- Branch 通常移动，发布 Tag 通常保持固定。
- Local Tag 需要单独 Push 才会出现在 Remote。
- Tag、Release、Build 和 Deploy 不是同一个动作。

## 本章不要求现在掌握

- 复杂 Tag 修复和签名。
- Release Automation。
- 已发布 Tag 的删除或移动。

## 进入下一章前确认

1. 为什么不能只用“最新 develop”说明生产版本？
2. Branch 和 Tag 的移动方式有什么区别？
3. 创建本地 Tag 后，服务器是否自动出现？
4. 为什么不应默认使用 `git push --tags`？
5. Tag 是否一定表示已经部署成功？
6. 已发布 Tag 指错 Commit 时，为什么通常优先创建新版本？

[上一章：撤销与恢复](07_undo_and_reset.md) · [下一章：常用高级操作](09_advanced_operations.md)
