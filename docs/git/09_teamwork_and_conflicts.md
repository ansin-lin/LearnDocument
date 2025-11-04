# 09 团队协作与代码评审（GitHub Flow）

## 9.1 GitHub Flow（推荐简洁流程）

1) 从 `main` 拉分支：`feature/...`  
2) 提交并推送到远程同名分支  
3) 发起 Pull Request（PR）→ 代码评审  
4) 通过后 **Squash and Merge**（保持历史干净）  
5) 删除远程特性分支

## 9.2 PR 规范与检查项

- PR 标题遵循 Conventional Commits（如 `feat: 用户登录`）  
- 描述包含：动机、方案、影响范围、测试方式、截图  
- CI 通过、Review 通过

## 9.3 常见协作冲突与应对

- 同一文件同一处改动 → 走冲突解决流程  
- `rebase` 自己的分支以保持线性历史：`git pull --rebase`

---
