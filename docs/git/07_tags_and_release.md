# 07 标签与版本发布

## 7.1 轻量标签与附注标签

```powershell
git tag v1.0                 # 轻量
git tag -a v1.1 -m "release" # 附注
git show v1.1
git push origin v1.0 v1.1
git push origin --tags       # 一次性推送所有标签
```

## 7.2 删除与移动

```powershell
git tag -d v1.0
git push origin :refs/tags/v1.0
```

---
