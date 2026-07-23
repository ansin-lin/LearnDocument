# 01 安装与初始配置

## 1.1 安装并验证 Git

从可信来源安装 Git。安装页面和包版本可能变化，以官方说明和所在公司的软件管理规则为准。

- Windows：从 [Git for Windows](https://gitforwindows.org/) 安装，或使用公司软件中心
- macOS：可使用 Xcode Command Line Tools 或 Homebrew
- Ubuntu/Debian：使用系统包管理器安装 `git`
- Fedora/RHEL：使用系统包管理器安装 `git`

本教程后续命令默认在 Windows PowerShell 中执行。安装完成后重新打开终端：

```powershell
git --version
Get-Command git
```

`git --version` 应输出已安装版本；`Get-Command git` 可以确认实际调用的程序路径。

## 1.2 配置提交身份

提交身份会记录在新提交中，它不等同于 GitHub 登录凭据。请使用团队认可的姓名和邮箱。

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
git config --global --list
```

`--global` 作用于当前操作系统用户。某个仓库需要使用不同身份时，在该仓库目录执行不带 `--global` 的配置：

```powershell
git config user.name "Project Name"
git config user.email "project@example.com"
git config --local --list
```

配置优先级通常是系统级、全局、仓库本地依次覆盖。查看某项配置来自哪里：

```powershell
git config --show-origin --get user.email
```

## 1.3 编辑器和换行符

使用 Visual Studio Code 作为 Git 编辑器时：

```powershell
git config --global core.editor "code --wait"
```

文本文件用不可见字符表示“这一行结束”。Windows 常用 CRLF（Carriage Return + Line Feed，回车加换行），Linux/macOS 常用 LF（Line Feed，换行）。同一文件混用不同换行符时，Git 可能显示整份文件都发生了变化。

不要只依靠每位开发者的个人配置决定仓库换行符；团队项目应优先提交 `.gitattributes`，统一文本文件规则。例如：

```gitattributes
* text=auto
*.sh text eol=lf
*.bat text eol=crlf
```

- `* text=auto`：让 Git 自动识别普通文本文件并规范仓库存储。
- `*.sh text eol=lf`：Shell 脚本检出时使用 LF，避免 Linux 执行异常。
- `*.bat text eol=crlf`：Windows 批处理文件检出时使用 CRLF。

修改已有项目的换行策略会产生大量差异，必须先与团队确认并单独提交。不要在不了解项目规则时机械设置 `core.autocrlf`。

## 1.4 选择 HTTPS 或 SSH

远程仓库常用两种连接方式：

| 方式 | 示例 | 特点 |
|---|---|---|
| HTTPS | `https://github.com/user/repo.git` | 常配合凭据管理器或访问令牌 |
| SSH | `git@github.com:user/repo.git` | 使用本机私钥认证 |

凭据管理器是在操作系统中安全保存登录凭据的工具；访问令牌是平台生成、具有指定权限和有效期的认证字符串，不能当作普通密码写入远程 URL、脚本或仓库文件。

公司项目还可能要求单点登录（一次企业身份验证访问多个系统）、VPN（连接公司受控网络）或网络代理。遇到这些环境要求时，应使用公司指定的账号和工具。

## 1.5 安全配置 SSH

先检查是否已有密钥，不要直接覆盖：

```powershell
Get-ChildItem -Force "$env:USERPROFILE\.ssh"
```

如果 `.ssh` 目录尚不存在，PowerShell 会报告找不到路径；这通常表示本机还没有创建过 SSH 配置，不代表 Git 安装失败。

需要新密钥时执行：

```powershell
ssh-keygen -t ed25519 -C "you@example.com"
```

建议设置密码短语。默认情况下会创建：

- `id_ed25519`：私钥，只保存在受保护的本机，绝不能发送或提交
- `id_ed25519.pub`：公钥，可以添加到 GitHub/GitLab 账号

读取公钥内容：

```powershell
Get-Content "$env:USERPROFILE\.ssh\id_ed25519.pub"
```

将完整公钥添加到远程平台后测试 GitHub：

```powershell
ssh -T git@github.com
```

首次连接会显示主机指纹。应先对照平台官方公布的指纹，再确认连接。GitHub 成功认证时会显示用户名和“does not provide shell access”，命令仍可能返回退出码 1，这是测试接口的正常行为。

GitLab 项目应使用 GitLab 提供的主机名，例如：

```powershell
ssh -T git@gitlab.com
```

## 1.6 帮助与诊断

```powershell
git help -a
git help commit
git commit -h
git config --list --show-origin
```

如果 `ssh -T` 返回 `Permission denied (publickey)`，依次检查：

1. 公钥是否添加到了正确账号。
2. 远程地址是否使用 SSH 格式。
3. SSH 是否选中了正确私钥。
4. 公司网络、代理或单点登录是否有限制。

调试时可以临时使用详细输出，但不要把包含内部主机名、用户名或路径的完整日志公开发布：

```powershell
ssh -vT git@github.com
```

## 1.7 安装与配置自检

依次执行：

```powershell
git --version
git config --get user.name
git config --get user.email
git config --get init.defaultBranch
git config --list --show-origin
```

自检标准：Git 能输出版本；用户名和邮箱符合项目要求；默认分支输出 `main`；最后一条能显示每项配置来自哪个文件。

常见错误及处理：

```text
git : 无法将“git”项识别为 cmdlet、函数、脚本文件或可运行程序的名称
```

原因通常是 Git 未安装、安装后终端未重启，或 Git 路径没有加入 `PATH`。重新确认安装来源并打开新的 PowerShell。

```text
Permission denied (publickey).
```

这表示远程平台没有接受当前 SSH 密钥。检查公钥是否添加到正确账号、远程主机名是否正确，以及 SSH 是否选择了对应私钥。

## 1.8 本章总结

- 提交身份与平台登录凭据是不同概念。
- 全局配置可被仓库本地配置覆盖。
- 换行符应由团队规则和 `.gitattributes` 管理。
- 私钥不得共享，首次 SSH 连接需要验证主机指纹。

## 练习

1. 查看 Git 版本和程序路径。
2. 用 `--show-origin` 确认 `user.email` 来自哪个配置文件。
3. 解释 SSH 公钥和私钥中哪一个可以上传到平台。

### 自检提示

- `git config --show-origin --get user.email` 应同时显示配置文件路径和邮箱。
- 可以上传的是 `.pub` 结尾的公钥；无 `.pub` 后缀的私钥不能离开受保护的本机。

[下一章：基本概念与工作原理](02_basic_concepts.md)
