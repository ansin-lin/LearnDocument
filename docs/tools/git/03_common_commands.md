# 03 基本命令与日常提交

## 3.1 Git 的日常循环

日常工作通常重复以下过程：

```text
确认状态 -> 修改文件 -> 检查差异 -> 选择性暂存 -> 再次检查 -> 提交
```

对应的基础命令是：

```powershell
git status
git diff
git add <file>
git diff --staged
git commit -m "说明本次变更"
git log --oneline
```

提交前检查差异能够避免把调试代码、生成文件、秘密信息或无关修改一起提交。

## 3.2 创建或取得仓库

新项目可以初始化本地仓库：

```powershell
New-Item -ItemType Directory git-daily-lab
Set-Location git-daily-lab
git init -b main
```

已有远程项目通常使用克隆，不要先在同一目录执行 `git init`：

```powershell
git clone <repository-url>
Set-Location <repository-directory>
```

## 3.3 查看状态和差异

```powershell
git status
git status --short
git diff
git diff --staged
git diff HEAD
```

- `git diff`：工作区与暂存区之间的差异
- `git diff --staged`：暂存区与当前提交之间的差异
- `git diff HEAD`：工作区和暂存区的整体结果与当前提交之间的差异

简短状态的两列分别表示暂存区和工作区状态。例如 `M  app.txt` 表示修改已暂存，` M app.txt` 表示工作区修改尚未暂存，`MM app.txt` 表示暂存后又继续修改。

## 3.4 选择要提交的内容

优先指定文件或目录：

```powershell
git add README.md
git add src
```

确认当前目录只有本次任务的修改后，才使用：

```powershell
git add .
```

一个文件包含多个不相关修改时，可以逐块选择：

```powershell
git add -p README.md
```

Git 会逐块显示差异并等待选择，常见提示类似：

```text
Stage this hunk [y,n,q,a,d,s,e,?]?
```

- `y`：暂存当前修改块
- `n`：跳过当前修改块
- `s`：尝试把当前块拆得更小
- `q`：退出，保留已经完成的选择
- `?`：显示全部可用选项

第一次练习时只需要掌握 `y`、`n`、`s` 和 `q`。完成后用 `git diff --staged` 确认实际暂存内容。

`git add` 保存的是执行当时的文件内容。暂存后继续编辑，需要再次执行 `git add` 才会更新暂存区。

## 3.5 编写清晰的提交

提交应尽量小而完整：完成一个可说明、可验证的变更，不混入格式化、依赖升级和其他任务。

```powershell
git commit -m "docs: explain staged changes"
```

Conventional Commits 是一种常见团队约定，不是 Git 强制语法：

```text
feat: add user login
fix: handle empty email
docs: update setup guide
refactor: simplify user service
test: cover invalid password
chore: update development tools
```

提交信息应说明“做了什么”，必要时在正文解释“为什么”。避免 `update`、`fix bug`、`修改` 等无法追踪目的的描述。

`git commit -am` 只会自动暂存已经跟踪文件的修改和删除，不包含未跟踪的新文件。初学阶段建议继续使用明确的 `git add` 和 `git commit`。

## 3.6 查看历史

```powershell
git log --oneline --graph --decorate --all
git show HEAD
git show --stat HEAD
git log -- README.md
```

`HEAD` 表示当前提交。`git show HEAD` 同时显示提交信息和差异，适合提交后自查。

## 3.7 使用 .gitignore

`.gitignore` 用于忽略不应由 Git 跟踪的文件。以下是配置片段，具体规则应按项目技术栈调整：

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

`.gitignore` 不能保护已经提交的秘密，也不会自动取消跟踪已有文件。文件已经被跟踪时，可以仅从暂存区索引移除、保留本地文件：

```powershell
git rm --cached .env
git status
git commit -m "chore: stop tracking local environment file"
```

目录需要使用受限路径：

```powershell
git rm -r --cached -- path\to\generated
```

执行后必须检查 `git status`，确认只包含预期删除。若秘密已经提交，应立即通知负责人并轮换凭据；仅加入 `.gitignore` 或删除最新文件不能清除历史中的秘密。

## 3.8 完整实验：完成一次可审查提交

**环境与初始状态：** Windows PowerShell；在新的 `git-daily-lab` 目录执行。如果已在 3.2 创建该目录，从身份配置开始。

```powershell
git config user.name "Git Learner"
git config user.email "learner@example.com"
Set-Content README.md "# Git Daily Lab"
Set-Content notes.log "temporary log"
Set-Content .gitignore "*.log"
git status --short
git diff
git add README.md .gitignore
git diff --staged
git commit -m "docs: initialize daily Git lab"
git show --stat HEAD
git status
```

验证结果：

- `notes.log` 不应出现在待提交文件中。
- 最新提交应包含 `README.md` 和 `.gitignore`。
- 最后的 `git status` 应显示工作区干净。

## 3.9 常见错误

- `nothing to commit`：没有新差异，或修改尚未保存到磁盘；先执行 `git status`。
- 提交了不需要的文件：如果尚未推送，参考[第 07 章](07_undo_and_reset.md)；不要立即使用 `reset --hard`。
- `.gitignore` 不生效：先用 `git ls-files <file>` 检查文件是否已经被跟踪。
- 提交作者错误：先检查本地和全局配置，修改之后只影响新提交。

```text
nothing to commit, working tree clean
```

这不是程序故障，而是当前提交、暂存区和工作区之间没有差异。如果预期有修改，检查文件是否保存、是否位于正确仓库，以及是否被 `.gitignore` 忽略。

```text
error: pathspec 'missing.txt' did not match any file(s) known to git
```

这表示路径拼写错误，或文件不在当前目录。使用 `Get-ChildItem` 和 `git status --short` 确认真实路径。

## 3.10 本章总结

- 提交前依次检查状态、未暂存差异和已暂存差异。
- 优先按文件暂存，确认范围后再使用 `git add .`。
- 小而清晰的提交更容易 Review、回退和排查问题。
- `.gitignore` 只影响未跟踪文件，不能代替秘密管理。

## 练习

1. 修改两个文件，但只把其中一个提交。
2. 暂存文件后再次修改，解释 `git status --short` 的两列。
3. 创建一个 `.log` 文件，使用 `git check-ignore -v <file>` 找出匹配规则。

### 自检提示

- 只提交一个文件后，另一个文件仍应出现在 `git status` 中。
- `MM` 表示同一文件既有已暂存修改，又有未暂存修改。
- `git check-ignore -v notes.log` 应显示命中的忽略规则及其文件位置。

[上一章：基本概念与工作原理](02_basic_concepts.md) · [下一章：分支、合并与冲突](04_branches_and_merge.md)
