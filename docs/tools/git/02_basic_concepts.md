# 02 基本概念与工作原理

第 01 章说明了为什么需要 Git。本章先从整体上区分工作区、暂存区、本地仓库和远程仓库，再通过修改一个文件理解内容怎样在这些位置之间流转。本章以理解状态为主，第 03 章再完整执行命令。

## 本章学习目标

### Level A：必须掌握

- 解释 Working Tree、Staging Area、Local Repository 和 Remote Repository。
- 说明 `git add`、`git commit`、`git push` 分别改变哪里。
- 区分 Untracked、Modified、Staged 和 Committed。
- 初步理解 Commit、Branch 和 `HEAD`。

### Level B：理解并能够查资料操作

- 理解同一文件可以同时存在已暂存和未暂存修改。
- 看懂 `HEAD~1`、Commit ID、分支名和标签名代表的版本。
- 理解 `--` 用于分隔版本或选项与文件路径。

### Level C：后续章节再学习

- 本地分支、Remote-tracking branch 和服务器分支的详细区别。
- Git Object、复杂 revision 表达式和 detached HEAD。

## 开始前先理解：Git 的四个位置

学习 Git 命令之前，必须先知道修改可能处于哪个位置。工作区、暂存区和本地仓库都在开发者自己的电脑上；远程仓库通常位于 GitHub、GitLab 或公司的 Git 服务器上。

```text
Working Tree（工作区）
正在编辑的实际文件
        │
        │ git add
        ▼
Staging Area（暂存区）
下一次 Commit 准备包含的内容
        │
        │ git commit
        ▼
Local Repository（本地仓库）
本机已经创建的 Commit 和历史
        │
        │ git push
        ▼
Remote Repository（远程仓库）
服务器上供团队共享的 Commit 和历史
```

### 四个位置分别有什么作用

| 位置 | 概念 | 主要作用 | 典型动作 |
|---|---|---|---|
| Working Tree（工作区） | 当前项目中可以直接看到和编辑的文件 | 编写、修改、运行和测试代码 | 使用编辑器修改并保存文件 |
| Staging Area（暂存区） | 本机上为下一次 Commit 准备内容的区域，也称 Git Index | 从所有修改中选择这一次准备提交的内容 | `git add` |
| Local Repository（本地仓库） | 本机保存 Git 数据和 Commit 历史的仓库，通常由项目中的 `.git` 目录管理 | 保存已经创建的 Commit，供本机查看、比较和恢复 | `git commit` |
| Remote Repository（远程仓库） | GitHub、GitLab 或公司服务器上的共享仓库 | 让团队成员交换 Commit，并支持 Branch、PR/MR 和 Review | `git push`、`git fetch` |

### 它们最核心的区别

四个位置解决的是四个不同问题：

```text
工作区：我现在正在修改什么？
暂存区：下一次 Commit 准备包含什么？
本地仓库：我的电脑已经记录了哪些 Commit？
远程仓库：团队服务器已经共享了哪些 Commit？
```

以修改 `employee-list.txt` 为例：

1. 在编辑器中保存文件，只改变 Working Tree。
2. 执行 `git add employee-list.txt`，把执行当时的内容放入 Staging Area。
3. 执行 `git commit`，用暂存区的内容在 Local Repository 创建 Commit。
4. 执行 `git push`，才会尝试把本地 Commit 发送到 Remote Repository。

因此下面四个动作不能混为一谈：

```text
保存文件 ≠ git add ≠ git commit ≠ git push
```

同一个文件的内容也可能在这些位置中不同。例如，文件已经 add 后又继续编辑：暂存区仍保存 add 当时的内容，而工作区保存后来继续修改的内容。本章后面会用实际场景详细说明这种状态。

## 2.1 从修改一个文件开始

员工管理项目中有一个已经提交过的文件：

```text
employee-list.txt
```

原内容：

```text
1001,山田太郎
```

开发者增加一名员工并保存文件：

```text
1001,山田太郎
1002,佐藤花子
```

保存完成只表示磁盘上的文件发生了变化。此时还没有创建新的 Git 版本记录，也没有上传到 GitLab。

Git 接下来需要解决三个问题：

1. 当前哪些文件发生了变化？
2. 哪些变化应该进入下一次 Commit？
3. 怎样把选中的变化保存为一次可追踪的历史记录？

这三个问题分别对应 Working Tree、Staging Area 和 Local Repository。

## 2.2 Working Tree：实际编辑文件的位置

Working Tree（工作区）是当前项目目录中可以直接编辑、编译和运行的文件。

```text
employee-system\
├─ employee-list.txt
├─ README.md
└─ .git\
```

其中 `employee-list.txt` 和 `README.md` 是工作区文件。隐藏的 `.git` 目录属于本地仓库的数据，不应像普通业务文件一样手工修改或删除。

当你在编辑器中修改并保存 `employee-list.txt` 时，首先变化的是 Working Tree。

```text
编辑器保存文件
      ↓
Working Tree 中的 employee-list.txt 发生变化
```

> **新人常见误解：** 保存文件不等于 Commit。保存只更新工作区中的文件内容。

## 2.3 为什么需要 Staging Area

假设当前同时修改了：

```text
employee-list.txt：APP-123 要求增加员工
README.md：自己顺手修改了说明文字
```

当前 Ticket 只要求提交 `employee-list.txt`。如果 Git 只能把所有修改一次性记录，`README.md` 就会混入无关变更。

因此 Git 在 Working Tree 和 Commit 之间提供了 Staging Area（暂存区）：

```text
Working Tree                         Staging Area
employee-list.txt：Modified  ──选择──> employee-list.txt：Staged
README.md：Modified                  README.md：未选择
```

Staging Area 用来准备“下一次 Commit 要包含的内容”。它不是远程服务器，也不是临时备份目录。

把指定文件当前内容选择进暂存区：

```cmd
git add employee-list.txt
```

执行前：

```text
employee-list.txt 的修改只在 Working Tree
```

执行后：

```text
employee-list.txt 执行 add 当时的内容进入 Staging Area
```

技术上更准确地说，`git add` 会用当前工作区内容更新 Git Index；Index 是 Staging Area 的另一个常见名称。

> **新人常见误解：** `git add` 不会上传文件，也不会创建 Commit。它只是选择下一次 Commit 的内容。

## 2.4 为什么需要 Commit

暂存区已经准备好以后，还需要把这些内容记录为一次正式历史。这个动作叫 Commit：

```cmd
git commit -m "feat: add employee"
```

Commit 可以先理解为：

> 一次有作者、有时间、有说明、能够追踪的项目版本记录。

一段历史可能是：

```text
A 项目初始化
│
B 增加员工一览
│
C 增加部门名称
```

每个 Commit 至少关联：

- 本次记录的项目内容。
- 作者和时间。
- Commit Message（提交说明）。
- 前一个 Commit，形成历史关系。
- Commit ID，也称 Commit Hash，用于唯一识别该 Commit。

Commit ID 通常显示为十六进制字符串，例如 `a82c310`。日常输出经常只显示较短前缀，只要在当前仓库中能够唯一识别即可。

## 2.5 Local Repository：Commit 保存在哪里

`git commit` 创建的历史保存在 Local Repository（本地仓库）中。它通常位于项目的 `.git` 目录，由 Git 管理。

至此形成三个位置：

```text
Working Tree
      │
      │ git add
      ▼
Staging Area
      │
      │ git commit
      ▼
Local Repository
```

- Working Tree：编辑实际文件。
- Staging Area：准备下一次 Commit。
- Local Repository：保存已经创建的 Commit 和相关历史。

Commit 完成以后，即使没有网络，本机仍然可以查看这段历史。

> **新人常见误解：** Commit 完成不代表代码已经上传。它只说明 Local Repository 新增了 Commit。

## 2.6 Remote Repository：团队如何看到 Commit

团队成员需要共享历史时，通常会使用 GitHub 或 GitLab 上的 Remote Repository（远程仓库）。

```text
Working Tree
      │ git add
      ▼
Staging Area
      │ git commit
      ▼
Local Repository
      │ git push
      ▼
Remote Repository
```

`git push` 才会把本地 Commit 发送到远程仓库。本章只理解它发生在哪两个位置之间，具体连接、同步和错误处理将在第 05 章学习。

四个位置承担不同职责：

| 位置 | 当前阶段的理解 | 是否在本机 |
|---|---|---|
| Working Tree | 正在编辑和运行的文件 | 是 |
| Staging Area | 下一次 Commit 的候选内容 | 是 |
| Local Repository | 本机已经创建的 Commit 历史 | 是 |
| Remote Repository | 团队通过网络共享的仓库 | 通常否 |

## 2.7 文件状态如何变化

Git 首先区分文件是否已经被跟踪。

### 新文件

新建 `employee-list.txt` 后，典型状态变化是：

```text
Untracked
    │ git add
    ▼
Staged
    │ git commit
    ▼
Committed
```

- Untracked：Git 看到了新文件，但它还没有进入跟踪范围。
- Staged：文件当前内容已经进入暂存区，准备进入下一次 Commit。
- Committed：该内容已经进入本地 Git 历史。

### 已提交文件再次修改

文件提交后再次编辑，典型状态变化是：

```text
Committed / Unchanged
    │ 修改并保存
    ▼
Modified
    │ git add
    ▼
Staged
    │ git commit
    ▼
Committed
```

- Unchanged：工作区内容与当前 Commit 一致。
- Modified：已跟踪文件发生修改，但新内容尚未完整进入暂存区。

“Committed”主要是帮助新人理解内容已经进入历史；`git status` 在没有新变化时通常显示 `working tree clean`，而不是逐个文件显示 `Committed`。

## 2.8 为什么暂存后继续修改仍要检查

`git add` 选择的是执行当时的文件内容。假设：

```text
1. 修改 employee-list.txt
2. git add employee-list.txt
3. 再次修改 employee-list.txt
```

此时同一个文件可以同时包含：

```text
Staging Area：第 2 步 add 时的内容
Working Tree：第 3 步继续修改后的内容
```

因此 Commit 前不能只看文件名，还要区分：

- `git diff`：Working Tree 与 Staging Area 之间尚未暂存的差异。
- `git diff --staged`：Staging Area 与当前 Commit 之间准备提交的差异。

这两个命令只读取和显示差异，不修改文件。本章先理解比较对象，第 03 章会实际观察输出。

## 2.9 Commit、Branch 和 HEAD 初识

Commit 是历史中的版本节点。Branch（分支）可以先理解为指向某个 Commit、并会随着新 Commit 向前移动的名称。

```text
A ── B ── C
          ↑
         main
          ↑
         HEAD
```

- `A`、`B`、`C`：三个 Commit。
- `main`：当前指向 Commit C 的 Branch。
- `HEAD`：可以先理解为“我当前所在的位置”，通常指向当前 Branch。

第 04 章会正式学习 Branch 的创建、切换、合并和冲突。本章不要求操作 Branch，也不学习 rebase 或 detached HEAD。

## 2.10 本地 main、origin/main 与服务器 main 预告

连接远程仓库后，还会看到三个容易混淆的名称：

```text
本地 main
本地 origin/main
服务器 main
```

当前只需要知道它们不是同一个对象：

- `main`：本地 Branch。
- `origin/main`：本机记录的 Remote-tracking branch。
- 服务器 `main`：GitHub/GitLab 远程仓库中的 Branch。

`origin/main` 不是服务器分支本身，只有执行 fetch 等获取操作后，它才会反映本机最近取得的服务器状态。详细同步过程留到第 05 章。

## 2.11 Level B：如何指定某个版本

Git 命令有时需要指定某个 Commit，这类写法通常称为 revision（版本引用）。第一次学习只需能够查阅：

| 写法 | 含义 |
|---|---|
| `HEAD` | 当前所在的 Commit |
| `HEAD~1` | 沿第一父提交向前一代，简单线性历史中常理解为上一个 Commit |
| `COMMIT_ID` | 使用 Commit ID 指定版本 |
| `main` | `main` 当前指向的 Commit |
| `v1.0.0` | 标签 `v1.0.0` 指向的 Commit |

例如查看上一个 Commit：

```cmd
git show HEAD~1
```

如果仓库只有一个 Commit，`HEAD~1` 不存在，命令会失败。

独立的 `--` 常用于分隔版本或选项与文件路径：

```cmd
git diff HEAD~1 -- README.md
```

它表示只查看 `README.md` 相对于上一个 Commit 的差异，也可以避免路径名与分支名相同时产生歧义。这些写法不属于第二章 Level A 考点。

## 2.12 新人常见误解

| 误解 | 正确认识 |
|---|---|
| 保存文件就是 Commit | 保存只更新 Working Tree |
| `git add` 是上传 | `add` 更新 Staging Area |
| `git commit` 是上传 | `commit` 更新 Local Repository |
| Staging Area 就是 Local Repository | 暂存区准备内容，本地仓库保存 Commit 历史 |
| Local Repository 就是 GitLab | Local Repository 在本机，GitLab 通常承载 Remote Repository |
| `git status` 会整理或修改代码 | 它只读取并报告状态 |
| `git diff` 会修改文件 | 它只读取并显示差异 |
| `origin/main` 就是服务器 `main` | 它是保存在本机的远程状态记录 |

## 2.13 场景练习

当前工作区有三个变化：

```text
A.java：APP-123 要求修改
B.java：APP-123 要求修改
README.md：与 APP-123 无关的个人修改
```

请回答：

1. 哪些文件应该进入本次 Staging Area？
2. `README.md` 应该进入本次 Commit 吗？为什么？
3. 执行 `git add A.java B.java` 后，内容进入了哪里？
4. 执行 Commit 后，GitLab 是否已经出现这些修改？
5. 如果 add 后又继续修改 `A.java`，暂存区会自动更新吗？
6. 想确认下一次 Commit 包含什么，应查看哪一组差异？

参考判断：

- 只有 `A.java` 和 `B.java` 属于当前 Ticket。
- `git add` 选择的是执行当时的内容，不会上传，也不会跟随之后的编辑自动更新。
- Commit 只进入 Local Repository；尚未 Push 时，Remote Repository 不会更新。
- Commit 前应使用 `git diff --staged` 检查暂存内容。

## 2.14 本章总结

```text
编辑和保存
    ↓
Working Tree
    │ git add
    ▼
Staging Area
    │ git commit
    ▼
Local Repository
    │ git push
    ▼
Remote Repository
```

- Working Tree、Staging Area、Local Repository 和 Remote Repository 不能混为一谈。
- Untracked、Modified、Staged、Committed 描述内容所处的不同阶段。
- Branch 指向 Commit；`HEAD` 可以先理解为当前所在位置。
- 下一章将真正执行 `status → diff → add → diff --staged → commit → log → show`。

[上一章：安装与初始配置](01_install_and_config.md) · [下一章：基本命令与日常提交](03_common_commands.md)
