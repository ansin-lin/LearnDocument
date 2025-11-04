# 02 安装与初始配置

## 2.1 安装

- Windows：安装 **Git for Windows**（包含 Git Bash）。
- macOS：`brew install git`
- Linux：`sudo apt/yum install git`

## 2.2 基本配置（首次必须）

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global core.autocrlf true   # Windows 推荐
git config --global init.defaultBranch main
git config --global core.editor "code --wait"
git config --global -l
```

## 2.3 生成 SSH Key 并绑定 GitHub/GitLab

```powershell
ssh-keygen -t ed25519 -C "you@example.com"
# 默认保存到 C:\Users\<用户名>\.ssh\id_ed25519
# 复制公钥内容添加到 GitHub -> Settings -> SSH and GPG keys
type $env:USERPROFILE\.ssh\id_ed25519.pub
ssh -T git@github.com
```

## 2.4 帮助系统

```powershell
git --version
git help
git help commit
git commit -h
```

---

## 实验：完成本机 Git 配置与 SSH 免密

1) 安装 Git，设置全局用户名/邮箱  
2) 生成 SSH Key，测试连接 GitHub  
3) 在 GitHub 创建空仓库 `hello-git`，复制 SSH 地址备用

