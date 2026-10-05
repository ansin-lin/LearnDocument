# 04 分支、合并与冲突

第 03 章完成了本地 Commit。本章进入多人开发的第一个核心问题：怎样让每个任务独立开发，再把正确结果整合回目标 Branch。

## 本章学习目标

### Level A：必须掌握

- 说明团队开发为什么需要 Branch。
- 使用 `branch`、`switch`、`switch -c` 和 `merge` 完成本地分支流程。
- 根据规格解决基本 Conflict，而不是机械选择 Current 或 Incoming。

### Level B：理解并能够查资料操作

- 区分 fast-forward 和 merge commit。
- 确认 Branch 已合并后，安全删除本地任务 Branch。
- 识别 modify/delete 和 add/add 等特殊 Conflict。

### Level C：后续章节再学习

- rebase、已经共享的历史改写和强制推送。
- 本章只知道这些操作可能改变 Commit History，正式说明见第 09 章。

## 4.1 为什么不能所有人都直接修改 develop

假设两名开发者都直接在 `develop` 上工作：

```text
张三：APP-123 增加部门名称
李四：APP-124 修改员工搜索
```

任何一方提交半成品、调试代码或错误修改，都会立即混入共同开发线。团队将难以单独 Review、测试、撤销和交付某一个 Ticket。

更安全的做法是让每个 Ticket 使用独立任务 Branch：

```text
                    C ── D  feature/APP-123
                   /
A ── B  develop
                   \
                    E ── F  feature/APP-124
```

每条任务线可以独立 Commit、测试和 Review，确认完成后再整合回 `develop`。

## 4.2 Branch 是什么

第 02 章已经初步认识 Branch：它是指向某个 Commit、并会随新 Commit 前进的名称。

```text
A ── B
     ↑
  develop
     ↑
    HEAD
```

从 B 创建 `feature/APP-123` 后：

```text
A ── B
     ↑
  develop
     ↑
feature/APP-123
     ↑
    HEAD
```

此时两个 Branch 指向同一个 Commit。Branch 不是一整套项目目录副本，因此创建通常很快。

在任务 Branch 上创建新 Commit 后：

```text
A ── B  develop
     \
      C  feature/APP-123 ← HEAD
```

`develop` 仍指向 B，任务 Branch 向前移动到 C。

## 4.3 查看、创建和切换 Branch

开始状态：Working Tree 干净，当前项目规定从 `develop` 派生任务 Branch。

先确认：

```cmd
git status
git branch -vv
```

- `git branch -vv`：列出本地 Branch；`*` 表示当前 Branch，并显示最新 Commit 和 upstream 信息。
- 如果 `status` 显示未提交修改，不要直接切换任务，应先判断修改归属。

切换到派生元 Branch，再创建任务 Branch：

```cmd
git switch develop
git switch -c feature/APP-123
git branch -vv
```

- `git switch develop`：让 `HEAD` 指向已有的 `develop`，并更新 Working Tree 以匹配它。
- `git switch -c feature/APP-123`：从当前位置创建并切换到新 Branch；`-c` 表示 create。

验证结果：`branch -vv` 的 `*` 应位于 `feature/APP-123`。

> **现场注意：** 第 05 章会学习怎样先把本地 `develop` 更新到远程最新状态。本章只练习本地 Branch 行为。

## 4.4 在任务 Branch 上开发

APP-123 要求给员工一览增加部门名称。修改前：

```text
1001,山田太郎
1002,佐藤花子
```

修改后：

```text
1001,山田太郎,営業部
1002,佐藤花子,総務部
```

按照第 03 章形成本地 Commit：

```cmd
git status
git diff -- employee-list.txt
git add employee-list.txt
git diff --staged
git commit -m "feat: add employee departments"
git log --oneline --decorate -3
```

预期只有 `feature/APP-123` 指向新 Commit，`develop` 仍停留在原位置。

## 4.5 Merge 为什么存在

任务完成后，需要把任务 Branch 的成果整合回目标 Branch。这个操作叫 Merge（合并）。

方向取决于当前 Branch：

```text
当前 Branch ← 要合入的 Branch
develop      ← feature/APP-123
```

先切换到接收修改的 `develop`，再执行 Merge：

```cmd
git switch develop
git merge feature/APP-123
```

执行前必须使用 `git branch` 或 `git status` 确认当前 Branch。如果方向弄反，历史结果可能与预期不同。

执行后验证：

```cmd
git log --oneline --graph --decorate --all
git status
```

## 4.6 Fast-forward Merge

如果创建任务 Branch 后，`develop` 没有新 Commit：

```text
A ── B  develop
     \
      C ── D  feature/APP-123
```

合并时 Git 只需把 `develop` 指针移动到 D：

```text
A ── B ── C ── D
               ↑
 develop / feature/APP-123
```

这叫 fast-forward（快进）Merge。它不会额外创建 Merge Commit。

## 4.7 Merge Commit

如果两个 Branch 都继续产生 Commit：

```text
      C ── D  feature/APP-123
     /
A ── B ── E  develop
```

Git 可能创建一个同时连接 D 和 E 的 Merge Commit：

```text
      C ── D
     /       \
A ── B ── E ── M  develop
```

Merge Commit 保留“这里整合了两条开发历史”的信息。项目是否允许 fast-forward、是否总是创建 Merge Commit、是否由平台执行 Merge，应遵守仓库规则。

## 4.8 Conflict 为什么发生

如果两个 Branch 修改了同一位置，而且 Git 无法自动判断最终结果，就会发生 Conflict（冲突）。

例如：

```text
feature/APP-123：1002,佐藤花子,総務部
develop：        1002,佐藤花子,人事部
```

Git 会使用 `<<<<<<< HEAD`、`=======`、`>>>>>>> feature/APP-123` 标出双方区域。例如：

```text
Current（HEAD）:
1002,佐藤花子,人事部

Incoming（feature/APP-123）:
1002,佐藤花子,総務部
```

实际文件中的 `<<<<<<< HEAD` 到 `=======` 是当前 Branch 一侧，`=======` 到 `>>>>>>> ...` 是要合入 Branch 一侧。

这些标记只说明双方内容，不能告诉你业务上哪一个正确。

> **新人常见误解：** Conflict 的目标不是让红色提示消失，也不是固定选择 Current 或 Incoming，而是根据最新规格形成正确代码。

## 4.9 Conflict 解决流程

Merge 停止后先确认状态：

```cmd
git status
```

按照以下顺序处理：

```text
读取双方修改
→ 确认 Ticket、规格和影响范围
→ 必要时向负责人或修改者确认
→ 编辑正确的最终内容
→ 删除全部 Conflict Marker
→ 运行测试
→ git add 标记已解决
→ 检查暂存差异
→ 完成 Merge Commit
```

命令示例：

```cmd
git add employee-list.txt
git diff --staged
git commit -m "merge: resolve employee department conflict"
git status
```

存在进行中的 Merge 且所有 Conflict 已解决时，也可以使用 `git merge --continue`。它会继续完成 Merge 并打开提交说明编辑器；仍然不能代替测试和差异检查。

如果确认本次 Merge 不应继续，并且尚未创建 Merge Commit：

```cmd
git merge --abort
```

`--abort` 尝试恢复到 Merge 开始前。执行前仍要用 `status` 确认当前确实处于 Merge 中。

## 4.10 实验：制造并解决 Conflict

**环境：** Windows CMD，新建独立目录 `git-branch-lab`，不连接远程仓库。

### 准备基线

```cmd
mkdir git-branch-lab
cd git-branch-lab
git init -b develop
git config user.name "Git Learner"
git config user.email "learner@example.com"
```

使用编辑器创建 `employee-list.txt`：

```text
1001,山田太郎
1002,佐藤花子
```

提交基线：

```cmd
git add employee-list.txt
git commit -m "chore: initialize employee list"
git switch -c feature/APP-123
```

### 在任务 Branch 修改

把 `1002` 行改为：

```text
1002,佐藤花子,総務部
```

```cmd
git add employee-list.txt
git commit -m "feat: add employee department"
```

### 在 develop 修改同一行

```cmd
git switch develop
```

把 `1002` 行改为：

```text
1002,佐藤花子,人事部
```

```cmd
git add employee-list.txt
git commit -m "fix: update employee department"
git merge feature/APP-123
git status
```

此时应出现 Conflict。假设最新规格确认部门为 `人事部`，编辑文件形成正确结果，删除全部标记，再执行：

```cmd
git add employee-list.txt
git diff --staged
git commit -m "merge: resolve employee department conflict"
git log --oneline --graph --decorate --all
git status
```

验收标准：最终文件符合规格、Conflict Marker 全部消失、测试通过、`status` 显示工作区干净，并能在提交图中看见两条历史被整合。

## 4.11 Level B：特殊 Conflict 与 Branch 清理

Conflict 不只发生在同一行：

- modify/delete：一侧修改文件，另一侧删除文件。
- add/add：两侧在同一路径创建不同内容。
- rename/modify：一侧重命名，另一侧继续修改。

处理原则仍然相同：确认双方意图和业务规格，再决定最终文件状态。

确认任务 Branch 已经合入当前 Branch 后：

```cmd
git branch --merged
git branch -d feature/APP-123
```

- `--merged`：列出已经合入当前 `HEAD` 的本地 Branch。
- `-d`：安全删除本地 Branch；Git 判断未合并时会拒绝。

`-D` 会强制删除，可能让仅由该 Branch 引用的 Commit 难以找回，不属于新人日常操作。

## 4.12 常见错误

### 在错误 Branch 上开发

发现后先停止继续 Commit，执行 `git status` 和 `git log --oneline --decorate -5` 记录状态，再根据是否已经共享向负责人确认处理方式。

### 切换 Branch 被拒绝

当前未提交修改可能被目标 Branch 覆盖。不要为了切换而强制丢弃；先确认修改归属，必要时使用第 09 章的 stash 或创建合理 Commit。

### Conflict 标记仍在文件中

提交前搜索 `<<<<<<<`、`=======`、`>>>>>>>`，运行测试并检查 `git diff --staged`。仅删除标记不代表业务结果正确。

## 本章必须掌握

- Branch 让不同 Ticket 的开发相互隔离。
- `switch -c` 从当前位置创建并切换 Branch。
- Merge 的方向是“当前 Branch 接收参数 Branch”。
- Conflict 必须根据规格解决并重新测试。

## 本章不要求现在掌握

- rebase 和历史改写。
- 复杂 Merge Strategy。
- 强制删除尚未合并 Branch。

## 进入下一章前确认

1. 为什么不应让所有开发者直接在 `develop` 上提交？
2. 创建 `feature/APP-123` 前为什么要确认当前 Branch？
3. `switch` 会改变 `HEAD` 和什么内容？
4. Merge 前如何判断谁接收谁？
5. Fast-forward 为什么不需要额外 Merge Commit？
6. Conflict 为什么不能机械选择 Current 或 Incoming？
7. 解决 Conflict 后为什么仍要测试和检查暂存差异？

[上一章：基本命令与日常提交](03_common_commands.md) · [下一章：远程仓库与同步](05_remote_repo.md)
