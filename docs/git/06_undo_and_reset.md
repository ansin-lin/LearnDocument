# 06 撤销与恢复（reset/revert/restore）

## 6.1 三种 reset

- `--soft`：仅移动分支指针，保留暂存与工作区
- `--mixed`（默认）：清空暂存，保留工作区
- `--hard`：丢弃暂存与工作区改动（危险）

```powershell
git reset --soft HEAD~1
git reset --mixed HEAD~1
git reset --hard HEAD~1
```

## 6.2 revert（用一次新的提交来“反做”）

```powershell
git revert <commit>
```

## 6.3 restore（文件级撤销，现代替代）

```powershell
git restore <file>              # 丢弃工作区改动
git restore --staged <file>     # 取消暂存
git restore --source=HEAD~1 <file>
```

## 6.4 reflog（救命草）

- `git reflog` 可以看到 **移动过的 HEAD/分支** 历史，常用于找回。

---
