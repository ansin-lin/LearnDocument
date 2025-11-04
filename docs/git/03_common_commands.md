# 03 基本命令与常见操作

## 3.1 常用命令速览

| 操作 | 命令 | 说明 |
|---|---|---|
| 初始化 | `git init` | 新建本地仓库 |
| 克隆 | `git clone <url>` | 从远端复制 |
| 查看状态 | `git status` | 文件状态 |
| 添加暂存 | `git add <file/dir> / .` | 放入暂存区 |
| 提交 | `git commit -m "msg"` | 记录快照 |
| 历史 | `git log --oneline --graph --decorate --all` | 图形化日志 |
| 对比 | `git diff` / `git diff --staged` | 工作区/暂存区差异 |
| 重命名 | `git mv old new` | 保留历史 |

## 3.2 忽略文件 `.gitignore`

```java
# 例如
node_modules/
*.log
target/
.idea/
.DS_Store
```

> 已被跟踪的文件需先 `git rm --cached <file>` 再生效。

## 3.3 提交信息规范（推荐 Conventional Commits）

```java
feat: 新增用户登录接口
fix: 修复用户空邮箱报错
docs: 完善 README
refactor: 重构 Service 层
test: 补充单元测试
chore: 升级依赖
```

---

## 实验：第一次提交

```powershell
mkdir demo03; cd demo03
git init
echo "# demo03" > README.md
echo "target/" > .gitignore
git add .
git commit -m "feat: init project with README and .gitignore"
git log --oneline
```

修改 README，再观察 `git status` / `git diff` / `git add` / `git commit` 的行为。

