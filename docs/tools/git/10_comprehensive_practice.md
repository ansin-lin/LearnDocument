# 10 Git 新人综合实战

前九章已经完成 Git 基础教学。本章不新增 Git 概念，而是验证学员能否根据 Repository、Ticket 和项目规则，安全完成一次日本项目中的开发流程。

## 本章学习目标

完成后应能解释并独立完成：

```text
取得项目
→ 调查规则和基线
→ 更新 develop
→ 创建任务 Branch
→ 修改与自查
→ Commit
→ Push
→ PR/MR
→ Review 指摘対応
→ Conflict 解决
→ CI / Merge
→ 本地同步
→ Branch 清理
```

本章禁止把 rebase、reset、force push 等高级命令作为完成任务的捷径。

## 10.1 实战环境与角色

### 环境

- Windows CMD 与已经配置作者身份的 Git。
- 教师准备的练习 Remote Repository。
- 默认检出 Branch 和开发目标 Branch 均为 `develop`。
- 仓库不包含生产数据、真实凭据或内部地址。

### 角色

- Developer：学员，负责 APP-123。
- Reviewer：教师或另一名学员，负责指摘、冲突准备和 Approve。
- CI：可以是真实练习 Pipeline，也可以由教师提供模拟结果。

### Ticket

```text
チケット：APP-123
件名：社員一覧に部署名を追加する

改修内容：
社員一覧データの各社員に部署名を追加する。

Target Branch：develop
```

开始状态 `src\employee-list.txt`：

```text
1001,山田太郎
1002,佐藤花子
1003,鈴木一郎
```

第一次实现目标：

```text
1001,山田太郎,営業部
1002,佐藤花子,総務部
1003,鈴木一郎,
```

Review 后最终目标：

```text
1001,山田太郎,営業部
1002,佐藤花子,総務部
1003,鈴木一郎,（未設定）
```

仓库还会出现不属于 Ticket 的 `debug.log` 或 `local-config.txt`。学员必须发现并排除，不能使用 `git add .` 无条件提交全部文件。

## 10.2 三种练习层级

本章提供三种使用方式，不要求同一次课堂重复完成相同任务。

### Level 1：教师带练

适合第一次串联完整流程。教材提供命令，但每一步都要回答“为什么现在执行、执行后哪里变化、怎样验证”。

### Level 2：只给流程目标

适合复习。教师重置练习仓库，只给阶段成果和检查点，不直接给命令。

### Level 3：独立实战

最终验收使用等价但未提前做过的新 Ticket。只提供 Repository、Ticket、Target Branch、项目规则和验收条件。

## 10.3 Level 1：取得并调查项目

```cmd
git clone REPOSITORY_URL
cd REPOSITORY_DIRECTORY
git status
git branch -a
git remote -v
git log --oneline --graph --decorate -10
```

必须说明：

- clone 后为什么仍要阅读 README 和开发手顺。
- `origin` 指向哪里。
- 当前 Branch 是什么。
- 是否能看到 `origin/develop`。
- Working Tree 是否干净。
- 改修前构建和测试是否通过。

基线异常时，记录命令、Commit ID、错误和日志位置，先向负责人确认，不要立即修改代码掩盖既有问题。

## 10.4 Level 1：更新 develop 并创建任务 Branch

```cmd
git switch develop
git fetch origin
git pull --ff-only
git status
git switch -c feature/APP-123
git branch -vv
```

检查点：

- 本地 `develop` 已更新到项目期望状态。
- `feature/APP-123` 从最新 `develop` 创建。
- `*` 位于任务 Branch。
- 创建任务 Branch 前没有遗留修改。

学员应能回答：为什么不能在落后的 `develop` 上开始任务？

## 10.5 Level 1：修改并排除无关文件

使用编辑器修改 `src\employee-list.txt`，同时检查教师准备的 `debug.log` 或 `local-config.txt`：

```cmd
git status
git diff -- src\employee-list.txt
git check-ignore -v debug.log
```

如果无关文件没有被 `.gitignore` 排除，也不能因为它存在就提交。应只暂存 Ticket 文件，并把项目级忽略规则问题记录给负责人。

检查：

- 业务文件内容符合第一次实现目标。
- 没有无关格式化和其他 Ticket 修改。
- `debug.log`、`local-config.txt`、令牌和本地环境内容没有进入提交范围。

## 10.6 Level 1：暂存、Commit 和自查

```cmd
git add src\employee-list.txt
git diff --staged
git commit -m "feat: display department name"
git show HEAD
git status
```

学员必须解释：

- 为什么按文件 add，而不是直接 `git add .`。
- `diff --staged` 检查哪两个位置。
- Commit 后为什么服务器仍然没有该修改。
- `show HEAD` 用来确认什么。

验收：最新 Commit 只包含目标文件，最后的 `status` 没有遗漏的 Ticket 修改。

## 10.7 Level 1：Push 功能 Branch

```cmd
git push -u origin feature/APP-123
git branch -vv
```

验证：平台出现 `feature/APP-123`，upstream 设置正确，Remote `develop` 尚未改变。

学员必须回答：为什么 Push 成功不表示已经完成 Merge？

## 10.8 Level 1：创建 PR/MR

```text
Source / マージ元：feature/APP-123
Target / マージ先：develop
```

创建前检查 Commits 和 Changes。PR/MR 描述：

```text
■ チケット
APP-123

■ 改修内容
社員一覧に部署名を追加

■ 変更ファイル
src/employee-list.txt

■ 影響範囲
社員一覧表示

■ テスト結果
T01：部署名あり OK

■ 未実施
部署名未設定時の表示仕様は確認待ち
```

如果 Source/Target 选反或出现大量无关差异，停止创建并重新调查 Branch 状态。

## 10.9 Level 1：处理 Review 指摘

Reviewer 指摘：

```text
部署名が未設定の場合の表示を再確認してください。
空文字ではなく「（未設定）」としてください。
```

把 `1003` 的空字段修改为 `（未設定）`，新增 T02 并重新执行 T01：

```cmd
git status
git diff -- src\employee-list.txt
git add src\employee-list.txt
git diff --staged
git commit -m "fix: handle missing department name"
git push
```

同一 Source Branch Push 后，原 PR/MR 自动更新，一般不需要重新创建。

回复：

```text
部署名未設定の場合は「（未設定）」を表示するよう修正しました。
T01、T02 を再実施し、正常終了を確認しました。
```

验收：回复包含修改内容和再次确认结果，不只写“対応済みです”。

## 10.10 Level 1：制造并解决 Conflict

Reviewer 通过另一条 PR/MR，把 Remote `develop` 中 `1002` 的部门更新为 `人事部`。学员 Branch 仍是 `総務部`。

取得最新目标 Branch 并按项目允许的 Merge 方式同步：

```cmd
git switch feature/APP-123
git fetch origin
git merge origin/develop
git status
type src\employee-list.txt
```

看到 `<<<<<<<`、`=======`、`>>>>>>>` 后：

```text
读取双方修改
→ 确认最新规格
→ 与 Reviewer 确认 1002 应为人事部
→ 编辑最终结果
→ 删除全部 Conflict Marker
→ 重新测试
→ 暂存并检查
→ 完成 Merge Commit
```

最终文件：

```text
1001,山田太郎,営業部
1002,佐藤花子,人事部
1003,鈴木一郎,（未設定）
```

```cmd
git add src\employee-list.txt
git diff --staged
git commit -m "merge: resolve employee department conflict"
git push
git status
```

Conflict 的验收标准是最终业务结果正确、测试通过，并且没有无关文件进入 Commit。

## 10.11 Level 1：CI、Merge 与本地清理

确认：

- Reviewer 已 Approve。
- Review Thread 已解决。
- CI 构建、测试和检查通过。
- Source/Target 正确。
- 最终差异和测试证据正确。

按平台规则完成 Merge。然后同步本地：

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

只有确认 APP-123 已进入本地 `develop` 后，才删除本地任务 Branch。Remote Branch 是否删除由平台和项目规则决定。

## 10.12 Level 2：只给流程目标

教师把练习仓库恢复到初始状态后，只提供以下任务，不给具体命令：

1. 取得 Repository，确认 Remote、当前 Branch、基线测试和项目规则。
2. 从最新 `develop` 创建符合规则的 APP-123 Branch。
3. 完成部门名称修改，并排除无关日志或本地配置。
4. 创建范围正确、目的清楚的 Commit。
5. 把功能 Branch 上传到 Remote。
6. 创建 Source/Target 正确、证据完整的 PR/MR。
7. 处理空部门 Review 指摘并具体回复。
8. 同步教师制造的同一行修改，按规格解决 Conflict。
9. 确认 CI、Approve 和最终差异后完成 Merge。
10. 同步本地 `develop` 并安全清理任务 Branch。

每个阶段只允许根据 `status`、`diff`、`log`、`branch`、`remote` 和平台状态判断下一步。命令没有报错不等于任务完成。

## 10.13 Level 3：独立实战

最终验收应使用学员没有提前照做过的新 Ticket。教师只提供：

```text
Repository：练习仓库地址
Ticket：新的员工一览改修票
Target Branch：develop
项目规则：Branch、Commit、PR、Review、CI、Merge 规则
验收条件：业务结果、测试范围和交付证据
```

教师应在不提前告诉具体时间的情况下加入：

- 一个不应提交的文件。
- 一次具体 Review 指摘。
- 一次简单内容 Conflict。
- 一次 CI 失败或模拟错误信息。

学员独立判断需要执行哪些操作。允许查阅 01～09 章和项目手顺，不允许通过 reset hard、无保护 force push 或删除测试绕过问题。

## 10.14 提交证据

学员提交：

1. 开始时的 Branch、Remote、Commit 和基线状态。
2. Commit ID 与 `git show HEAD`。
3. 未提交无关文件的证明。
4. PR/MR 的 Source、Target、描述和最终差异。
5. 首次测试、指摘后再测试和 Review 回复。
6. Conflict 双方内容、最终结果和解决 Commit。
7. CI、Approve 和 Merge 结果。
8. 清理后的本地 `develop`、Branch 列表和 `status`。

证据不得包含令牌、私钥、真实个人信息、生产日志或内部地址。

## 10.15 最终知识检查

1. 为什么要从最新 `develop` 创建任务 Branch？
2. `git add`、Commit、Push、PR/MR 和 Merge 分别改变哪里？
3. 本地 `develop`、`origin/develop` 和服务器 `develop` 有何区别？
4. 为什么 Commit 前必须检查普通 diff 和 staged diff？
5. 为什么 Push 成功不表示修改已经进入 `develop`？
6. APP-123 的 Source 和 Target 分别是什么？
7. Review 指摘后为什么通常不需要重新创建 PR/MR？
8. Conflict 为什么不能机械选择 Current 或 Incoming？
9. 已共享错误 Commit 为什么通常优先 revert？
10. Merge 后为什么还要同步本地 `develop`？

## 10.16 操作验收

| 能力 | 合格标准 |
|---|---|
| 项目调查 | 能确认 Remote、Branch、规则和改修前基线 |
| Branch 准备 | 从最新目标 Branch 创建正确任务 Branch |
| 修改与提交 | 排除无关文件，Commit 前后有差异检查，目的单一 |
| Remote 协作 | 正确 Push，并创建 Source/Target 无误的 PR/MR |
| Review | 在原 PR/MR 中完成修改、再测试、Push 和具体回复 |
| Conflict | 根据规格形成正确结果并提供测试证据 |
| CI 与 Merge | 能调查失败，满足 Gate 后完成 Merge |
| 同步与清理 | 更新本地目标 Branch，确认合入后安全清理 |
| 安全 | 未泄露秘密，未使用危险命令绕过问题 |

## 10.17 结业标准

- **A：能够独立完成**：clone、status、diff、add、diff staged、commit、branch/switch、fetch/pull/push、基本 Conflict、PR/MR、Review 指摘対応、Merge 后同步和清理。
- **B：知道场景并能查教程操作**：restore、amend、revert、stash、cherry-pick、历史调查和 Tag 基础。
- **C：理解用途与风险**：reset hard、rebase、interactive rebase、reflog、submodule 和覆盖式 Push。

达到 Level 3 操作验收全部合格，才表示能够在普通日本 Web 项目中安全完成一次基础 Git 开发流程。

[上一章：常用高级操作](09_advanced_operations.md) · [返回课程入口](index.md)
