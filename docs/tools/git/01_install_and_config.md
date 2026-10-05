# 01 安装与初始配置

本章先回答“为什么开发项目需要 Git”，再完成最小环境准备。第一次学习不要求记住远程认证、换行符和企业网络的全部细节。

## 本章学习目标

### Level A：必须掌握

- 用自己的话说明 Git 解决什么问题。
- 区分 Git、GitHub/GitLab 和 Git 图形工具。
- 安装 Git，并用 `git --version` 确认可以运行。
- 配置提交作者姓名、邮箱和新仓库默认分支名。

### Level B：理解并能够查资料操作

- 理解 Global 与 Local 配置的覆盖关系。
- 第一次连接远程仓库时，根据项目选择 HTTPS 或 SSH。
- 理解 Credential Manager、访问令牌和 SSH Key 的作用。

### Level C：项目需要时再学习

- CRLF/LF、`.gitattributes` 和 `core.autocrlf`。
- Proxy、VPN、SSO 和 SSH 详细排错。

## 1.1 为什么开发项目需要 Git

假设一个员工管理项目连续修改了四天：

```text
星期一：完成员工一览初版
星期二：增加部门名称
星期三：调整显示格式
星期四：发现星期二的实现才符合规格
```

如果只靠复制目录保存版本，很容易出现：

```text
employee-system
employee-system-old
employee-system-new
employee-system-final
employee-system-final2
employee-system-最终版
```

这时很难回答：

- 哪个目录才是正确版本？
- 星期二具体改了哪些行？
- 谁修改了这些文件，为什么修改？
- 修改错误以后怎样恢复？
- 两个人同时开发时怎样组合成果？
- 测试环境或生产环境对应哪个代码版本？

这些问题说明项目需要的不是更多副本，而是能够记录和比较变化的版本控制。

## 1.2 什么是版本控制与 Git

版本控制用于记录项目在不同时间点的重要变化，使开发人员能够查看历史、比较差异、恢复内容并进行多人协作。

```text
项目初始化
    ↓
增加员工一览
    ↓
增加部门名称
    ↓
修复空部门显示
```

Git 是一种分布式版本控制系统（Distributed Version Control System）。第一次学习时，“分布式”只需要理解为：Git 仓库和提交历史可以保存在开发者自己的电脑中，查看状态、创建提交等操作不一定需要连接服务器。

本章不深入 Blob、Tree 等 Git 内部对象。当前只需要知道，Git 可以帮助团队：

- 记录代码变化和修改原因。
- 比较不同版本。
- 找到错误从何时开始出现。
- 恢复错误修改。
- 使用不同分支并行开发。
- 合并成员成果并进行 Code Review。
- 将本地历史与团队远程仓库同步。

## 1.3 Git 与 GitHub、GitLab 的区别

Git 是版本控制工具；GitHub、GitLab 是托管 Git 仓库并提供团队协作能力的平台。

```text
Git                              GitHub / GitLab
├─ Commit                        ├─ Remote Repository
├─ Branch                        ├─ PR / MR
├─ Merge                         ├─ Review
└─ Version History               ├─ Issue / Ticket
                                 └─ CI/CD
```

- Repository（仓库）：由 Git 管理的项目及其版本历史。
- Remote Repository（远程仓库）：团队可以通过网络访问的共享仓库。
- PR/MR：Pull Request / Merge Request，请求把一个分支的修改合入另一个分支。
- CI/CD：自动构建、测试和交付的一类流水线机制。

这里先认识这些名称。远程同步、PR/MR 和 CI/CD 会在后续章节逐步学习。

> **新人常见误解：** 会使用 GitHub 或 GitLab 网页，不等于理解 Git；没有 GitHub/GitLab，Git 仍然可以在本地记录版本。

## 1.4 Git 与 VS Code、IntelliJ、TortoiseGit 的关系

Git 是核心版本控制工具。开发工具可以通过命令或图形界面调用 Git：

```text
Git
├─ CMD / Git Bash 等命令行
├─ VS Code Source Control
├─ IntelliJ IDEA / Eclipse Git 功能
└─ TortoiseGit 等图形工具
```

本教程主要使用 Windows CMD 演示 Git 命令。理解命令执行前后的状态以后，即使项目改用 VS Code、IntelliJ、Eclipse 或 TortoiseGit，也能判断图形按钮背后发生了什么。

> **新人常见误解：** VS Code Source Control 和 TortoiseGit 是操作 Git 的界面或辅助工具，不是 Git 本身。

## 1.5 先看一次完整开发流程

下面是一张学习地图，现在不要求记住全部步骤：

```text
修改代码
    ↓
git add：选择下一次提交的内容
    ↓
git commit：创建本地版本记录
    ↓
git push：把本地提交发送到远程仓库
    ↓
创建 PR / MR
    ↓
Review 与 CI
    ↓
Merge 到目标分支
```

本章只准备 Git 环境；第 02 章解释 Git 如何看待文件和提交；第 03 章完成第一次本地 Commit。

必须先记住三个不同动作：

```text
Commit ≠ Push ≠ PR/MR
```

## 1.6 安装并验证 Git

从可信来源安装 Git。安装页面和版本可能变化，以官方说明和公司软件管理规则为准。

- Windows：从 [Git for Windows](https://gitforwindows.org/) 安装，或使用公司软件中心。
- macOS：可以使用 Xcode Command Line Tools 或 Homebrew。
- Ubuntu/Debian、Fedora/RHEL：使用系统包管理器安装 `git`。

本教程后续命令默认在 Windows 命令提示符（Command Prompt，简称 CMD）中执行。按 `Win + R`，输入 `cmd` 后回车即可打开。

**为什么执行：** 确认 Git 已安装，并确认 Windows 能找到实际的 `git.exe`。

```cmd
git --version
where git
```

- `git --version`：显示当前执行的 Git 版本。
- `where git`：显示 CMD 找到的 Git 程序路径，不修改任何配置。

预期会看到类似输出：

```text
git version 2.x.x.windows.x
C:\Program Files\Git\cmd\git.exe
```

版本号会随安装版本变化，不要求与示例完全一致。

如果出现：

```text
'git' 不是内部或外部命令，也不是可运行的程序或批处理文件。
```

通常表示 Git 尚未正确安装、安装后没有重新打开 CMD，或者安装路径没有加入 `PATH`。先确认安装来源并重新打开终端，不要在未知目录重复安装多个 Git。

## 1.7 为什么需要配置姓名和邮箱

Git 创建一次 Commit 时，需要记录“谁创建了这次版本记录”。因此第一次提交前要配置作者信息。

```cmd
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
```

- `git config`：读取或写入 Git 配置。
- `--global`：设置当前操作系统用户的默认配置。
- `user.name`、`user.email`：新 Commit 使用的作者姓名和邮箱。
- `init.defaultBranch main`：以后新建仓库时默认把第一个分支命名为 `main`；不会修改已有仓库。

执行后验证：

```cmd
git config --get user.name
git config --get user.email
git config --get init.defaultBranch
```

`--get` 只读取指定配置，不会修改配置。输出应与刚才设置的值一致。

> **新人常见误解：** `user.name` 和 `user.email` 是 Commit 作者信息，不是 GitHub/GitLab 密码，也不会自动完成平台登录。

## 1.8 Global 与 Local 配置

假设个人电脑通常使用个人邮箱，但某个公司项目要求使用公司邮箱：

```text
Global：当前操作系统用户的默认配置
Local：当前 Git Repository 专用配置，可以覆盖 Global
```

进入具体仓库后，可以不带 `--global` 设置 Local 配置：

```cmd
git config user.name "Project Name"
git config user.email "project@example.com"
git config --local --list
```

这些命令必须在 Git 仓库中执行。查看最终读取到的邮箱来自哪个配置文件：

```cmd
git config --show-origin --get user.email
```

`--show-origin` 会同时显示配置来源；配置优先级通常由系统级、Global 到 Local 逐层覆盖。

使用 VS Code 作为 Git 多行说明编辑器时，可以配置：

```cmd
git config --global core.editor "code --wait"
```

`core.editor` 指定 Git 需要输入多行内容时启动的编辑器；`code --wait` 表示等待 VS Code 编辑窗口关闭。这是便利配置，不是完成第一次 Commit 的必需条件。

## 1.9 Level B：HTTPS、SSH 与 Credential

连接 Remote Repository 时常见两种地址：

| 方式 | 示例 | 常见认证方式 |
|---|---|---|
| HTTPS | `https://github.com/user/repo.git` | Credential Manager、访问令牌、企业登录 |
| SSH | `git@github.com:user/repo.git` | 本机 SSH 私钥和平台公钥 |

Credential Manager 是由操作系统或 Git 工具安全保存登录凭据的工具。访问令牌是平台生成、具有权限范围和有效期的认证字符串，不能写入远程 URL、脚本或仓库文件。

项目使用 SSH 时，先检查本机是否已有配置：

```cmd
dir /a "%USERPROFILE%\.ssh"
```

`dir /a` 显示包括隐藏项在内的目录内容。如果目录不存在，CMD 会报告找不到文件；这通常只表示本机还没有 SSH 配置。

确认确实需要新密钥，并且不会覆盖已有同名文件后执行：

```cmd
ssh-keygen -t ed25519 -C "you@example.com"
```

- `ssh-keygen`：生成 SSH 密钥对。
- `-t ed25519`：选择 Ed25519 密钥类型。
- `-C`：添加便于识别的注释。

建议设置密码短语。默认生成：

- `id_ed25519`：私钥，只保存在受保护的本机，绝不能发送、上传或提交。
- `id_ed25519.pub`：公钥，可以添加到 GitHub/GitLab 账号。

读取公钥内容：

```cmd
type "%USERPROFILE%\.ssh\id_ed25519.pub"
```

将公钥添加到对应平台后，可以测试认证：

```cmd
ssh -T git@github.com
ssh -T git@gitlab.com
```

`-T` 表示不分配交互终端，只测试认证。首次连接必须先按平台官方资料核对主机指纹，再决定是否接受。

## 1.10 Level C：换行符与企业网络

Windows 常使用 CRLF 表示换行，Linux/macOS 常使用 LF。同一文件的换行规则不一致时，Git 可能把整份文件显示为已修改。

团队项目应优先使用仓库中的 `.gitattributes` 统一规则，例如：

```gitattributes
* text=auto
*.sh text eol=lf
*.bat text eol=crlf
```

- `* text=auto`：让 Git 识别普通文本并规范仓库存储。
- `*.sh text eol=lf`：Shell 脚本检出时使用 LF。
- `*.bat text eol=crlf`：Windows 批处理文件检出时使用 CRLF。

修改既有项目的换行策略可能产生大量差异，必须先与团队确认并单独处理。不要在不了解项目规则时机械设置 `core.autocrlf`。

公司项目还可能要求 VPN、Proxy 或 SSO。这些属于项目环境要求，不需要第一次学习时背诵配置。应使用公司指定账号、工具和手顺，不把内部地址或诊断日志公开发布。

SSH 认证失败时可以按顺序检查：

1. 公钥是否添加到正确账号。
2. 远程主机名和地址类型是否正确。
3. SSH 是否选择了正确私钥。
4. VPN、Proxy 或 SSO 是否有项目限制。

需要详细日志时可以临时执行：

```cmd
ssh -vT git@github.com
```

`-v` 输出详细连接过程。日志可能包含用户名、主机名和本机路径，只能在受控范围内用于排查。

## 1.11 帮助与安装自检

不知道命令用法时，可以先查看 Git 自带帮助：

```cmd
git help -a
git help commit
git commit -h
git config --list --show-origin
```

- `git help -a`：列出可用 Git 命令。
- `git help commit`：打开 `commit` 的完整帮助。
- `git commit -h`：在终端显示简短帮助，不创建 Commit。
- `git config --list --show-origin`：显示最终读取到的配置和来源文件。

本章 Level A 自检：

```cmd
git --version
git config --get user.name
git config --get user.email
git config --get init.defaultBranch
```

验收标准：Git 能输出版本；姓名和邮箱符合项目要求；默认分支输出 `main`。

## 1.12 新人常见误解

| 误解 | 正确认识 |
|---|---|
| Git 就是 GitHub/GitLab | Git 是版本控制工具，GitHub/GitLab 是协作平台 |
| VS Code Source Control 就是 Git | 它是操作 Git 的界面 |
| Git 必须联网才能使用 | 本地状态检查和 Commit 等操作可以离线完成 |
| Commit 就是上传 | Commit 写入本地仓库，Push 才发送到远程 |
| `user.email` 是登录密码 | 它是 Commit 作者信息，不能用于代替认证 |
| 公钥和私钥都可以上传 | 只能向平台添加公钥，私钥不能离开受保护的本机 |

## 1.13 本章练习与总结

请根据场景回答，不要求背诵完整命令参数：

1. 团队使用 GitLab 托管项目。Git 和 GitLab 分别承担什么职责？
2. 在 VS Code 中点击 Commit 时，核心版本控制动作由谁完成？
3. 已经在本地创建 Commit，但没有执行 Push，其他成员能否在服务器看到它？
4. 为什么 Git 需要 `user.name` 和 `user.email`？
5. 公司项目要求使用公司邮箱，个人默认邮箱应通过哪一级配置覆盖？
6. SSH 公钥和私钥中，哪一个可以添加到平台？

完成本章后，应能说明：

- Git 用来记录、比较和追踪项目变化。
- GitHub/GitLab 和图形工具不是 Git 本身。
- Commit、Push 和 PR/MR 是三个不同动作。
- 第一次本地 Commit 前只需要完成 Git 安装和作者身份配置。

[下一章：基本概念与工作原理](02_basic_concepts.md)
