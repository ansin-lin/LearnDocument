# 06 团队协作与代码评审

本章把前面学过的 Branch、本地 Commit 和 Remote 同步连接成日本项目中的一次完整 Ticket 流程。重点不是背命令，而是理解每一步为什么存在、怎样验证以及何时需要停止并确认。

## 本章学习目标

### Level A：必须掌握

- 从最新 `develop` 创建 `feature/APP-123`。
- 完成自查、Commit、Push 和 PR/MR。
- 正确判断 Source Branch 与 Target Branch。
- 在原 PR/MR 中处理 Review 指摘、重新测试并回复 Reviewer。
- 理解 CI、Merge Gate、Merge 后同步和 Branch 清理。

### Level B：现场逐步掌握

- Protected Branch、事故处理、新人参画既有 Repository。
- Branch Strategy、Merge Strategy、历史调查和影响范围调查。

### Level C：项目规则决定

- 对已 Push 历史执行 rebase 或 `force-with-lease`。
- 新人不得在不了解项目规则时改写共享历史。

## 6.1 从收到 Ticket 开始

贯穿任务：

```text
チケット：APP-123
件名：社員一覧に部署名を追加する

Target Branch：develop
Task Branch：feature/APP-123
```

开始写代码前，先确认：

- Ticket 的改修内容和验收条件。
- Target Branch 与 Branch 命名规则。
- 项目构建、测试和 Commit Message 规则。
- PR/MR 模板、Reviewer 和 CI 要求。
- 是否有既有失败或未提交修改。

完整流程：

```text
Ticket
→ 更新 develop
→ 创建 feature/APP-123
→ 开发与自查
→ Commit
→ Push
→ PR/MR
→ Review 指摘対応
→ CI
→ Merge
→ 本地同步
→ Branch 清理
```

## 6.2 开始任务前的标准流程

先确认当前状态，再更新目标 Branch：

```cmd
git status
git switch develop
git fetch origin
git pull --ff-only
git log --oneline --decorate -5
```

检查点：

- Working Tree 干净。
- 当前 Branch 是本地 `develop`。
- `pull --ff-only` 成功，或失败原因已经调查。
- 最近 Commit 与平台上的 `develop` 一致。

然后创建任务 Branch：

```cmd
git switch -c feature/APP-123
git branch -vv
```

`branch -vv` 中的 `*` 应位于 `feature/APP-123`。不要在落后的 `develop` 上创建任务 Branch，也不要根据个人习惯自行改用 `main`。

## 6.3 开发与 Commit 前自查

修改 `employee-list.txt` 前：

```text
1001,山田太郎
1002,佐藤花子
1003,鈴木一郎
```

APP-123 修改后：

```text
1001,山田太郎,営業部
1002,佐藤花子,総務部
1003,鈴木一郎,
```

按照第 03 章的固定流程：

```cmd
git status
git diff -- employee-list.txt
git add employee-list.txt
git diff --staged
git commit -m "feat: display department name"
git show HEAD
git status
```

自查必须回答：

- 修改是否只覆盖 APP-123？
- 是否误包含日志、本地配置、格式化或其他 Ticket？
- 空部门的显示规格是否已经明确？
- 项目规定的构建和测试是否完成？
- `show HEAD` 是否只包含预期文件和差异？

如果需求不明确，不要凭个人判断补写规格；记录疑问并向负责人确认。

## 6.4 Push 功能 Branch

首次 Push：

```cmd
git push -u origin feature/APP-123
git branch -vv
```

执行后：

```text
Local feature/APP-123
        │ Push
        ▼
Remote feature/APP-123
```

此时服务器已经有功能 Branch，但 `develop` 仍未改变。Push 不是 Review 请求，也不是 Merge。

## 6.5 为什么还需要 PR/MR

Pull Request（PR）或 Merge Request（MR / マージリクエスト）用于请求把一个 Remote Branch 的修改合入另一个 Remote Branch，并提供：

- 差异和 Commit 的集中 Review。
- Reviewer 的指摘和讨论记录。
- CI 构建、测试和质量检查。
- Approve、权限和 Merge Gate。
- Ticket 与修改证据的可追溯记录。

```text
Remote feature/APP-123
        │ PR / MR
        ▼
Remote develop
```

## 6.6 Source Branch 与 Target Branch

APP-123 的正确方向：

```text
Source Branch / マージ元：feature/APP-123
Target Branch / マージ先：develop
```

- Source：提供修改的 Branch。
- Target：接收修改的 Branch。

方向选反可能让 PR/MR 出现大量无关差异。创建前必须核对 Source、Target、Commits 和 Changes。

## 6.7 PR/MR 创建前检查

在平台创建 PR/MR 前：

```cmd
git status
git log --oneline --decorate -5
git diff origin/develop...HEAD
```

- `status`：确认没有忘记 Commit 的修改。
- `log`：确认最新 Commit 和当前 Branch。
- 三点 `origin/develop...HEAD`：查看从双方共同起点到当前 Branch 的整体改修差异，适合 PR 前检查。

如果尚未执行最近一次 fetch，先更新 `origin/develop`，否则比较基准可能过旧。

PR/MR 描述示例：

```text
■ チケット
APP-123

■ 改修内容
社員一覧に部署名を追加

■ 変更ファイル
employee-list.txt

■ 影響範囲
社員一覧表示

■ テスト結果
T01：部署名あり OK

■ 未実施
部署名未設定時の表示仕様は確認待ち
```

“未实施”不能为了看起来完整而写成“无”。未知、无法执行或等待确认的项目应如实记录。

## 6.8 Review 是什么

Review 不只是寻找语法错误，还要确认：

- 是否符合 Ticket 和规格。
- 是否包含无关修改。
- 异常、空值和边界情况是否处理。
- 测试证据是否覆盖影响范围。
- 命名、可读性、安全性和项目规则是否满足。

Reviewer 应针对具体位置说明问题和理由；Developer 应理解指摘后再修改，而不是只追求把讨论标记为 resolved。

## 6.9 Review 指摘后怎么办

Reviewer 指摘：

```text
部署名が未設定の場合の表示を再確認してください。
空文字ではなく「（未設定）」としてください。
```

处理流程：

```text
理解指摘
→ 修改原 Task Branch
→ 重新执行相关测试
→ Commit
→ Push
→ 原 PR/MR 自动更新
→ 回复修改内容和确认结果
```

命令示例：

```cmd
git status
git diff -- employee-list.txt
git add employee-list.txt
git diff --staged
git commit -m "fix: handle missing department name"
git push
```

一般不需要重新创建 PR/MR。同一 Source Branch 的新 Commit Push 后，原 PR/MR 会更新。

推荐回复：

```text
部署名未設定の場合は「（未設定）」を表示するよう修正しました。
T01、T02 を再実施し、正常終了を確認しました。
```

不要只写：

```text
対応済みです。
```

好的回复应说明“改了什么”和“确认了什么”。

## 6.10 CI 与 Merge Gate

CI（Continuous Integration，持续集成）通常在 Push 或 PR/MR 更新后自动执行构建、测试、静态检查等任务。

```text
Push 新 Commit
→ PR/MR 更新
→ CI 执行
→ 成功或失败
```

CI 失败时：

1. 阅读失败 Job、错误信息和测试名称。
2. 判断是本次修改、既有失败还是环境问题。
3. 在本地复现并修正。
4. 重新测试、Commit、Push。
5. 在 PR/MR 中说明结果。

不要无证据地重复运行流水线，也不要为了变绿而删除测试。

Protected Branch（受保护 Branch）可能要求：

- 禁止直接 Push `develop`。
- 必须有指定人数 Approve。
- CI 全部通过。
- 所有 Review Thread 已解决。
- Source Branch 已同步到允许的状态。

这是平台规则，不一定是本机 Git 故障。

## 6.11 Merge

满足项目 Merge Gate 后，由有权限的成员在平台完成 Merge。常见策略包括：

- Merge commit：保留分叉和 Merge Commit。
- Squash merge：把 PR/MR 的多个 Commit 整理成一个目标 Branch Commit。
- Rebase merge：重放 Commit 形成线性历史。

新人只需要识别这些名称；采用哪一种由项目规则决定，不应为了个人偏好改变共享历史。

Merge 前最后核对：

- Source 是 `feature/APP-123`。
- Target 是 `develop`。
- Review 已 Approve。
- CI 通过。
- 指摘已处理。
- 最终差异和测试证据正确。

## 6.12 Merge 后同步和清理

平台完成 Merge 后，服务器 `develop` 已更新，但本地不会自动变化：

```cmd
git switch develop
git fetch origin
git pull --ff-only
git log --oneline --graph --decorate -10
git branch --merged
git branch -d feature/APP-123
git fetch --prune origin
git status
```

只有确认 APP-123 已进入本地 `develop` 后，才删除本地任务 Branch。Remote Branch 是否由平台自动删除，服从项目设置；不要提前删除仍在 Review 的 Branch。

## 6.13 Commit、Push、PR/MR 与 Merge 的区别

| 动作 | 改变的位置 | 主要结果 |
|---|---|---|
| Commit | Local Repository | 创建本地版本记录 |
| Push | Remote 功能 Branch | 上传本地 Commit |
| PR/MR | 托管平台 | 请求 Review 和合并 |
| Merge | Target Branch | 把通过检查的修改整合进目标 Branch |

顺序通常是：

```text
Commit → Push → PR/MR → Review/CI → Merge
```

四者不能互相替代。

## 6.14 Level B：事故处理原则

### 错误 Commit 到功能 Branch

如果尚未共享，可以根据第 07 章判断 amend、restore 或其他本地修正；如果已 Push，不要擅自改写历史，先确认团队规则。

### 错误 Commit 到公共 Branch

立即停止继续操作并通知负责人。已共享历史通常优先考虑 revert，而不是 reset 后强制 Push。

### Push 了秘密信息

立即通知负责人、撤销或轮换凭据并按安全流程处理。删除最新文件或加入 `.gitignore` 不能清除历史中的秘密。

### CI 失败

保存失败 Job、Commit ID 和错误日志，区分本次问题、既有问题和环境问题。日志中如含内部地址、账号或路径，只在受控范围共享。

## 6.15 Level B：新人参画既有 Repository

第一次取得项目：

```cmd
git clone REPOSITORY_URL
cd REPOSITORY_DIRECTORY
git remote -v
git branch -a
git branch -vv
git log --oneline --graph --decorate -10
git status
```

然后阅读 README、开发手顺、贡献规则和 CI 配置，确认：

- 默认 Branch 与任务派生元 Branch。
- Branch、Commit 和 PR/MR 命名规则。
- 构建、测试和本地启动方法。
- Reviewer、Approve 和 Merge 规则。
- 禁止提交的本地配置和秘密信息。

改修前先运行项目规定的 build/test，保存基线结果。基线本身失败时，应记录证据并向负责人确认，不能把既有失败算作自己的改修结果。

## 6.16 Level B：Branch Strategy 与历史调查

项目可能采用：

```text
简单模式：develop ← feature/*

发布分支模式：main ← release/* ← develop ← feature/*
                         hotfix/* → main（回合流向按项目规定）

Trunk-based：短生命周期 Branch → main
```

不要根据模式名称猜测流程，应查阅项目真实规则。

维护既有代码时，Git 历史可以帮助调查“为什么存在”和“影响哪些位置”：

```cmd
git log -- path\to\File.java
git blame path\to\File.java
git show COMMIT_ID
git log --grep="APP-123"
git log -S "methodName" -- path\to\File.java
```

- `log -- PATH`：查看文件相关历史。
- `blame`：按行显示最近修改 Commit，用于寻找调查入口，不用于追责。
- `show COMMIT_ID`：查看一次 Commit 的说明和差异。
- `--grep`：按 Commit Message 搜索 Ticket 等文字。
- `-S`：搜索某段文字数量发生变化的 Commit。

调查结论应结合 Ticket、规格、测试和当前代码，不能只凭某一条 Git 输出下结论。

## 6.17 日本项目常见表达

| 表达 | 当前课程中的含义 |
|---|---|
| チケット | 任务或问题单 |
| 対象ブランチ | 本次操作面向的 Branch |
| マージ元 / Source | 提供修改的 Branch |
| マージ先 / Target | 接收修改的 Branch |
| 指摘 | Review 中提出的问题或确认事项 |
| 修正対応 | 根据指摘修改、测试并反馈 |
| 影響範囲 | 修改可能影响的功能、文件或测试 |
| テスト結果 | 已执行测试及结果证据 |

这些表达用于理解现场任务，不需要把 Git 教程变成日语词汇表。项目手顺中的自然语言也不能机械映射为唯一命令；例如“最新化”仍要结合当前 Branch、状态和项目规定判断。

## 6.18 场景练习

1. PR/MR 的 Source 误选为 `develop`、Target 误选为 `feature/APP-123`。说明风险并修正。
2. Reviewer 要求处理空部门。完成修改、测试、Commit、Push，并写出具体回复。
3. CI 显示既有测试失败。列出需要保存的证据，以及为什么不能直接删除测试。
4. PR/MR 已 Merge，但本地 `develop` 仍停留在旧 Commit。说明下一步检查和操作。
5. 使用一段历史调查命令，找到某文件与 APP-123 相关的修改入口。

## 本章必须掌握

- Ticket 到 Merge 的完整团队流程。
- Source/Target、Push/PR/Merge 的区别。
- Review 指摘后在原 Branch 修改、测试、Push 和具体回复。
- CI 与 Merge Gate 通过后，仍要同步本地并清理 Branch。

## 本章不要求现在掌握

- 设计 Branch Strategy 或 Merge Strategy。
- 改写已经 Push 的历史。
- 独立处理严重安全事故或复杂 CI 基础设施问题。

## 进入下一章前确认

1. 为什么创建任务 Branch 前要先更新 `develop`？
2. Commit 后为什么还需要 Push？
3. Push 后为什么还需要 PR/MR？
4. APP-123 的 Source 和 Target 分别是什么？
5. Review 指摘后为什么一般不需要新建 PR/MR？
6. Review 回复为什么不能只写“対応済みです”？
7. CI 失败时为什么要先调查失败证据？
8. Merge 后为什么还要同步本地 `develop`？

[上一章：远程仓库与同步](05_remote_repo.md) · [下一章：撤销与恢复](07_undo_and_reset.md)
