# 08 标签与版本发布

## 8.1 标签与分支的区别

分支会随着新提交向前移动，标签通常固定指向某个确定提交。团队常用标签标记已经发布、测试通过或交付给客户的版本，例如 `v1.4.0`。

标签本身不会自动构建、部署或发布软件。实际发布通常由 CI/CD 触发：CI（持续集成）负责自动构建和测试，CD（持续交付或持续部署）负责把验证后的版本交付或部署到目标环境。具体行为取决于项目流水线配置。

## 8.2 轻量标签与附注标签

轻量标签只是一个名称指针：

```powershell
git tag v1.0.0
```

附注标签还保存创建者、时间和说明，正式发布通常优先使用：

```powershell
git tag -a v1.1.0 -m "release: version 1.1.0"
```

查看标签和对应内容：

```powershell
git tag --list
git show v1.1.0
```

默认在 `HEAD` 创建标签。为指定提交创建附注标签：

```powershell
git tag -a v1.0.1 <commit-id> -m "release: version 1.0.1"
```

创建前先确认提交 ID、测试结果和当前分支，不要只根据工作区文件判断发布内容。

## 8.3 推送标签

普通 `git push` 默认不会推送所有本地标签。推送单个已确认的标签：

```powershell
git push origin v1.1.0
```

以下命令会推送所有本地标签，可能把实验或内部标签一起上传，因此只在确认列表后使用：

```powershell
git tag --list
git push origin --tags
```

推送后在远程平台核对标签指向的提交和发布流水线结果。

## 8.4 版本号基础

项目常见 `MAJOR.MINOR.PATCH` 形式，例如 `2.3.1`：

- `MAJOR`：包含不兼容变更
- `MINOR`：增加向后兼容功能
- `PATCH`：向后兼容的问题修复

是否严格采用语义化版本、是否添加 `v` 前缀，以及预发布版本如何命名，应以项目约定为准。

## 8.5 删除或修正标签

删除尚未推送的本地标签：

```powershell
git tag -d v1.1.0
```

删除远程标签：

```powershell
git push origin --delete v1.1.0
```

已经交付或被流水线使用的标签不应擅自移动。移动同名标签会让不同开发者看到不同的“同一版本”，破坏可追溯性。发现发布标签错误时，优先与团队确认是否创建新的修正版本，例如 `v1.1.1`。

仅在明确允许修正未发布标签时：

```powershell
git tag -d v1.1.0
git tag -a v1.1.0 <correct-commit-id> -m "release: corrected version 1.1.0"
```

若旧标签已经推送，还需要按平台和团队流程处理远程标签，不要直接强制覆盖。

## 8.6 实验：在本地创建发布标签

**环境与范围：** 使用第 06 章练习仓库或新建仓库。本实验只创建本地标签，不触发远程发布。

```powershell
git status
git log --oneline -3
git tag -a v0.1.0 -m "release: practice version 0.1.0"
git tag --list
git show v0.1.0
```

验证标签指向预期提交后删除练习标签：

```powershell
git tag -d v0.1.0
```

## 8.7 本章总结

- 分支会移动，发布标签通常保持固定。
- 附注标签适合保存正式发布说明。
- 标签需要单独推送，推送后还要检查平台和流水线结果。
- 已共享的发布标签不应随意删除或移动。

## 练习

1. 分别创建轻量标签和附注标签，比较 `git show` 输出。
2. 为历史提交创建标签，而不是为当前 `HEAD` 创建。
3. 说明为什么修复已发布版本通常应创建新版本号。

### 自检提示

- `git show <annotated-tag>` 应同时显示标签说明和目标提交。
- 普通 `git push` 后不要假定标签已上传，使用远程平台或 `git ls-remote --tags origin` 检查。
- 已发布标签指错提交时，优先创建新的修正版本，而不是静默移动原标签。

[上一章：撤销与恢复](07_undo_and_reset.md) · [下一章：常用高级操作](09_advanced_operations.md)
