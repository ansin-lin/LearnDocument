# 03 基本命令与日常提交

第 02 章建立了 Git 的状态模型。本章在一个本地练习仓库中，把这套模型真正操作一遍，最终形成一套可以重复使用的提交习惯。

## 本章学习目标

### Level A：必须掌握

- 独立完成 `status → diff → add → diff --staged → commit → log → show`。
- 根据状态判断修改位于 Working Tree、Staging Area 还是 Local Repository。
- 优先按文件暂存，只提交当前 Ticket 需要的内容。
- 创建一个目的单一、说明清楚的小 Commit。

### Level B：理解并能够查资料操作

- 使用 `.gitignore` 排除不应跟踪的文件。
- 看懂 `status --short`，并在需要时使用 `diff HEAD`、`add -p` 和文件历史调查。

### Level C：后续章节再学习

- Push、PR/MR、Branch 合并、Conflict、rebase 和历史改写。

## 3.1 本章在 Git 流程中的位置

本章只处理本地操作：

```text
修改文件
    ↓
git status：有哪些状态变化
    ↓
git diff：具体改了什么
    ↓
git add：选择下一次 Commit 的内容
    ↓
git diff --staged：Commit 前检查
    ↓
git commit：写入 Local Repository
    ↓
git log / git show：确认历史
```

本章暂不学习 `push`、PR/MR 和 merge。完成 Commit 后，GitLab 上仍然不会自动出现修改。

## 3.2 准备本地练习仓库

**环境：** Windows CMD。

**范围：** 在新建的 `git-daily-lab` 目录中操作，不连接远程仓库，不在已有项目中运行。

创建目录和仓库：

```cmd
mkdir git-daily-lab
cd git-daily-lab
git init -b main
git config user.name "Git Learner"
git config user.email "learner@example.com"
```

- `mkdir` 和 `cd` 是 CMD 命令，分别创建目录和进入目录。
- `git init -b main` 在当前目录创建 Local Repository，并把第一个 Branch 命名为 `main`。
- 这里不带 `--global` 的 `git config` 只设置当前练习仓库身份。

使用编辑器在 `git-daily-lab` 中创建两个文件。

`employee-list.txt`：

```text
1001,山田太郎
```

`README.md`：

```text
# Git Daily Lab
```

下面这组命令只用于准备本章的开始状态。课堂中可以由讲师预先完成；自学时可以先照着执行，不要求此刻记忆 `add` 和 `commit`，后续小节会逐一解释。执行后两个文件已经有一个初始 Commit，才能清楚观察 Modified 状态：

```cmd
git add employee-list.txt README.md
git commit -m "chore: initialize Git daily lab"
git status
```

预期最后看到：

```text
nothing to commit, working tree clean
```

这表示 Working Tree、Staging Area 与当前 Commit 之间没有新差异。现在才开始 APP-123 的正式练习。

## 3.3 第一次改修任务

Ticket 内容：

```text
チケット：APP-123
件名：社員一覧に社員を追加する

改修内容：
社員番号 1002、氏名 佐藤花子を社員一覧に追加する。
```

使用编辑器把 `employee-list.txt` 修改为：

```text
1001,山田太郎
1002,佐藤花子
```

保存文件以后，修改只存在于 Working Tree：

```text
employee-list.txt = Modified
Staging Area      = 尚未更新
Local Repository  = 尚未更新
```

## 3.4 git status：现在发生了什么

**为什么需要：** 修改完成后，先确认当前 Branch、哪些文件变化以及它们处于什么状态。

```cmd
git status
```

典型输出的关键部分：

```text
On branch main
Changes not staged for commit:
  modified:   employee-list.txt
```

- `On branch main`：当前位于本地 `main` Branch。
- `modified`：已跟踪文件发生了修改。
- `not staged for commit`：修改还没有进入 Staging Area。

`git status` 主要回答：

> 现在有哪些文件处于什么状态？

它只读取状态，不会修改文件、暂存内容或创建 Commit。日常操作中，每个关键步骤前后都可以安全地使用它确认当前位置。

## 3.5 git diff：具体修改了什么

**为什么需要：** `status` 告诉我们文件变了，但没有展开具体行。提交前还要确认内容是否符合 Ticket。

```cmd
git diff -- employee-list.txt
```

典型差异的关键部分：

```diff
 1001,山田太郎
+1002,佐藤花子
```

- 以 `+` 开头的内容是新增行。
- 以 `-` 开头的内容是删除行。
- 没有 `+` 或 `-` 的上下文行用于帮助理解位置。

命令末尾的 `-- employee-list.txt` 表示只检查这个路径。此时 `git diff` 比较：

```text
Working Tree
    vs
Staging Area
```

它主要回答：

> 尚未完整暂存的内容具体改了什么？

`git diff` 只显示差异，不会修改代码。

## 3.6 git add：选择下一次 Commit 的内容

**为什么需要：** 当前 Ticket 只要求提交 `employee-list.txt`，因此只把这个文件选择进下一次 Commit。

执行前：

```text
Working Tree：employee-list.txt = Modified
Staging Area：没有 APP-123 的修改
```

执行：

```cmd
git add employee-list.txt
```

执行后：

```text
Working Tree：文件仍然存在
Staging Area：保存了 add 执行当时的 employee-list.txt 内容
Local Repository：尚未创建 APP-123 Commit
```

使用状态验证：

```cmd
git status
```

关键输出会变为：

```text
Changes to be committed:
  modified:   employee-list.txt
```

`Changes to be committed` 表示该修改已经 Staged，准备进入下一次 Commit。

> **新人常见误解：** `git add` 不是上传，也不是“Git 已经永久保存”。它只是更新 Staging Area。

## 3.7 git diff --staged：Commit 前最后检查

**为什么需要：** 已经执行 add 后，要确认下一次 Commit 实际会包含什么，而不是凭记忆直接提交。

```cmd
git diff --staged
```

它比较：

```text
Staging Area
    vs
当前 Commit（HEAD）
```

预期只看到 `employee-list.txt` 新增：

```diff
+1002,佐藤花子
```

此时再次执行普通 `git diff`，通常没有输出，因为 Working Tree 与 Staging Area 已经一致。这不表示修改消失；它已经移动到普通 `diff` 不负责显示的比较范围。

```text
git diff
    = 检查尚未完整进入 Staging Area 的修改

git diff --staged
    = 检查已经进入 Staging Area、准备 Commit 的修改
```

Commit 前检查 `git diff --staged` 是本章最重要的现场习惯之一。

## 3.8 git commit：创建本地版本记录

确认暂存内容正确后执行：

```cmd
git commit -m "feat: add employee"
```

- `git commit`：使用 Staging Area 中的内容创建新 Commit。
- `-m`：直接提供一行 Commit Message。
- `feat: add employee`：说明这次修改的目的。

状态变化：

```text
Staging Area
      │ git commit
      ▼
Local Repository

A ── B
     ↑
  新 Commit
```

新 Commit 包含修改内容、作者、时间、Commit Message 和 Commit ID。它不会自动包含没有暂存的修改。

提交后验证工作区：

```cmd
git status
```

预期结果：

```text
nothing to commit, working tree clean
```

> **新人常见误解：** `git commit` 不等于 `git push`。此时 APP-123 只进入 Local Repository，没有上传到 GitLab。

## 3.9 Commit Message 为什么重要

半年后调查历史时，下面的说明很难判断修改目的：

```text
a81d123 update
91bc456 fix
77aa890 change
```

更清楚的示例：

```text
feat: add employee search
fix: handle empty department
docs: update setup guide
```

Commit Message 至少应让其他开发者理解“这次修改做了什么”。修改原因无法从标题看出时，可以在 Commit 正文、Ticket 或 PR/MR 中补充。

Conventional Commits 是常见团队约定，不是 Git 强制语法：

- `feat`：增加功能。
- `fix`：修复问题。
- `docs`：只修改文档。
- `refactor`：调整实现但不改变预期功能。
- `test`：增加或修改测试。
- `chore`：工具、维护等辅助修改。

项目已有提交规则时，应优先遵守项目规则。

```text
一个 Commit = 一个逻辑完整的修改目的
```

不要把功能开发、全文件格式化、依赖升级和另一个 Bug Fix 混入同一个 Commit。

## 3.10 git log：确认历史

**为什么需要：** Commit 成功后，要确认它已经进入本地历史，并取得 Commit ID。

```cmd
git log --oneline
```

典型输出：

```text
a82c310 feat: add employee
9bc1721 chore: initialize Git daily lab
```

- 左侧是缩短显示的 Commit ID。
- 右侧是 Commit Message 标题。
- 最新 Commit 默认显示在最上方。

它对应一条简单历史：

```text
9bc1721 ── a82c310
初始状态      增加员工
```

`git log --oneline` 只读取历史，不修改仓库。

## 3.11 git show HEAD：检查刚刚的 Commit

**为什么需要：** `log` 能确认 Commit 存在，但还要检查它实际包含的说明和差异。

```cmd
git show HEAD
```

- `HEAD` 表示当前所在的 Commit。
- `git show HEAD` 显示该 Commit 的作者、时间、说明和完整差异。

预期只看到 `employee-list.txt` 增加 `1002,佐藤花子`。如果出现无关文件，说明暂存范围没有控制好，应停止后续共享并参考第 07 章选择安全的修正方式。

至此第一次本地 Commit 完成：

```text
修改
→ status
→ diff
→ add 指定文件
→ diff --staged
→ commit
→ log
→ show HEAD
```

## 3.12 第二次修改：独立完成

新 Ticket：

```text
チケット：APP-124
件名：社員一覧に社員を追加する

改修内容：
社員番号 1003、氏名 鈴木一郎を社員一覧に追加する。
```

把 `employee-list.txt` 修改为：

```text
1001,山田太郎
1002,佐藤花子
1003,鈴木一郎
```

这一次请不照抄完整答案，独立完成：

```text
1. 确认状态
2. 检查未暂存差异
3. 只暂存目标文件
4. 检查已暂存差异
5. 使用清楚的说明创建 Commit
6. 查看历史
7. 展开最新 Commit
```

完成后的验证标准：

- `git status` 显示工作区干净。
- `git log --oneline -3` 的最上方是 APP-124 对应 Commit。
- `git show HEAD` 只显示新增 `1003,鈴木一郎`。

可以参考的 Commit Message：

```text
feat: add third employee
```

## 3.13 多文件场景：只提交 Ticket 需要的内容

现在同时进行两项修改：

1. 在 `employee-list.txt` 中把 `1002` 的姓名修正为 `佐藤華子`，属于 APP-125。
2. 在 `README.md` 中追加个人学习笔记，不属于 APP-125。

保存两个文件后先执行：

```cmd
git status
git diff
```

本次应只暂存 Ticket 需要的文件：

```cmd
git add employee-list.txt
git diff --staged
git status
```

确认：

- `employee-list.txt` 出现在 `Changes to be committed`。
- `README.md` 仍出现在 `Changes not staged for commit`。
- `git diff --staged` 中没有 README 修改。

确认无误后再提交：

```cmd
git commit -m "fix: correct employee name"
git status
```

提交后 `README.md` 仍应保持 Modified。这正是 Staging Area 的价值：一次 Commit 只包含一个逻辑修改目的。

### 什么时候可以使用 git add .

```cmd
git add .
```

`.` 表示当前目录及其子目录中的变化。它不是禁止命令，但只有确认当前范围全部属于同一任务时才使用。新人优先使用 `git add FILE_PATH`，避免调试文件、生成物或无关修改一起进入暂存区。

## 3.14 .gitignore：哪些文件不应纳入版本管理

项目通常不应提交依赖目录、构建产物、日志、IDE 临时文件和本地环境配置。例如：

```gitignore
# 构建产物
target/
dist/

# 依赖目录
node_modules/

# 本地工具与日志
.idea/
*.log

# 本地环境配置
.env
```

`.gitignore` 主要影响尚未跟踪的路径。创建 `notes.log` 后，可以检查它命中了哪条规则：

```cmd
git check-ignore -v notes.log
```

`-v` 同时显示规则所在文件和行号；命令只调查忽略规则，不修改文件。

> **安全边界：** `.gitignore` 不是秘密管理工具。令牌、密码或私钥一旦提交，应立即通知负责人并轮换凭据；把文件加入 `.gitignore` 不能清除历史中的秘密。

文件已经被跟踪时，新增忽略规则不会自动取消跟踪。确实需要保留本地文件、但停止 Git 跟踪时，可以查阅：

```cmd
git rm --cached .env
git status
git diff --staged
```

目录需要限制明确路径：

```cmd
git rm -r --cached -- path\to\generated
```

- `--cached`：只从 Git Index 移除，保留本地工作文件。
- `-r`：递归处理目录。
- 独立的 `--`：表示后面是路径。

这些命令会形成待提交删除，必须在提交前检查 `status` 和 `diff --staged`。

## 3.15 Level B：需要时再使用的命令

### 简短状态

```cmd
git status --short
```

两列分别表示 Staging Area 和 Working Tree。例如：

```text
M  employee-list.txt
 M README.md
MM app.txt
?? notes.txt
```

- `M `：修改已暂存。
- ` M`：修改尚未暂存。
- `MM`：同一文件暂存后又继续修改。
- `??`：未跟踪文件。

### 查看相对 HEAD 的整体差异

```cmd
git diff HEAD
```

它显示 Working Tree 和 Staging Area 的整体结果相对于当前 Commit 的差异。第一次提交练习仍优先分别使用 `git diff` 和 `git diff --staged`，因为两者更能说明修改位于哪里。

### 按修改块暂存

一个文件混有多个无关修改、且暂时无法在编辑器中拆分时，可以查阅：

```cmd
git add -p README.md
```

常见选择：`y` 暂存当前块、`n` 跳过、`s` 尝试拆分、`q` 退出、`?` 查看帮助。操作后必须使用 `git diff --staged` 检查结果。

### commit -am 的边界

```cmd
git commit -am "fix: update tracked files"
```

`-a` 只会自动暂存已跟踪文件的修改和删除，不包含未跟踪的新文件。它还会跳过独立的暂存检查步骤，因此新人主线继续使用明确的 `add → diff --staged → commit`。

### 查看更多历史

```cmd
git log --oneline --graph --decorate --all
git show --stat HEAD
git log -- employee-list.txt
```

- `--graph`：使用字符线显示历史分叉和合并。
- `--decorate`：显示 Branch、Tag 和 `HEAD` 名称。
- `--all`：显示所有本地引用可到达的历史。
- `show --stat`：只显示文件和增删行统计。
- `log -- FILE_PATH`：只调查指定路径的历史。

结合 `blame`、Ticket 和影响范围的完整调查流程见第 06 章。

## 3.16 常见错误与定位

### 当前目录不是仓库

```text
fatal: not a git repository (or any of the parent directories): .git
```

先用不带参数的 `cd` 查看当前目录，再进入正确仓库。不要因为看到错误就在不确定的位置再次执行 `git init`。

### 没有可提交内容

```text
nothing to commit, working tree clean
```

这通常表示当前 Commit、Staging Area 和 Working Tree 之间没有差异。如果预期有修改，检查文件是否已经保存、是否位于正确仓库，以及是否被 `.gitignore` 忽略。

### 路径不存在

```text
error: pathspec 'missing.txt' did not match any file(s) known to git
```

使用 `dir` 和 `git status` 确认真实文件名及当前位置。不要通过扩大 `git add .` 范围来掩盖路径错误。

### .gitignore 没有生效

```cmd
git ls-files FILE_PATH
git check-ignore -v FILE_PATH
```

`git ls-files` 有输出通常表示路径已经被跟踪；`git check-ignore -v` 用于查找命中的忽略规则。两者都只调查状态。

### 提交了不需要的文件

先停止 Push，记录当前状态和 Commit 是否已经共享，再参考第 07 章选择 restore、amend 或 revert。不要把 `reset --hard` 当作普通修复命令。

## 3.17 本章完整闭环

```text
编辑并保存 employee-list.txt
      ↓
Working Tree：Modified
      ↓ git status / git diff
确认文件状态和具体差异
      ↓ git add employee-list.txt
Staging Area：Staged
      ↓ git diff --staged
确认下一次 Commit 内容
      ↓ git commit
Local Repository：新增 Commit
      ↓ git log / git show HEAD
确认历史和实际差异
```

最后回答：GitLab 上已经有这个 Commit 了吗？

```text
没有。
```

因为本章没有执行 `git push`。第 05 章将学习 Local Repository 与 Remote Repository 之间的同步。

## 3.18 操作验收

在不查看本章完整命令序列的情况下，完成一个新的员工追加任务。合格标准：

| 检查项 | 合格标准 |
|---|---|
| 状态确认 | 修改前后能够用 `status` 判断文件状态 |
| 内容确认 | add 前使用 `diff` 检查具体差异 |
| 暂存范围 | 只 add 当前 Ticket 需要的文件 |
| Commit 前检查 | 使用 `diff --staged` 确认实际提交内容 |
| Commit | Commit Message 能说明修改目的 |
| 历史验证 | 使用 `log` 和 `show HEAD` 确认结果 |
| 概念解释 | 能说明 Commit 为什么不等于 Push |

练习完成后，可通过文件资源管理器删除独立的 `git-daily-lab` 目录进行清理。删除前确认路径确实是练习目录，并且其中没有需要保留的文件。

[上一章：基本概念与工作原理](02_basic_concepts.md) · [下一章：分支、合并与冲突](04_branches_and_merge.md)
