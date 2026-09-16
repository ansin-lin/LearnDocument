# 07 日本项目改修与交付演习

日本项目中的 PG 经常不是从空文件开始，而是根据仕様、障害票或 Review 指摘修改已有脚本，并提交可复现的自测证据。本章模拟一次小型改修。

## 7.1 开始状态

现有 `check_log.sh` 已完成第 6 章功能。收到以下改修票：

```text
件名：ログ抽出結果に調査情報を追加する

要件：
1. 成功输出的第一行增加执行时刻和输入文件名。
2. 日志明细仍保留原始行号。
3. 无匹配时不得创建新的正式结果文件。
4. 不得修改输入日志。
5. 既有退出状态保持不变。
```

这里的“既有”表示修改前已经存在的行为。改修不能只满足新要求，还要防止旧功能发生退化（デグレ，regression）。

## 7.2 修改前调查

先记录基线，而不是直接编辑：

```bash
git status
bash -n check_log.sh
bash check_log.sh input/app.log R001
printf 'status=%s\n' "$?"
wc -l input/app.log output/R001.log
```

在任务记录中写明：修改文件、调用方、输出格式变化、可能受影响的测试。若脚本不在 Git 仓库中，应使用教师提供的练习仓库，不要在真实项目仓库直接实验。

## 7.3 实现要求

建议将结果头先写入临时文件，再追加 `grep` 结果：

```text
executed_at=2026-09-01T10:15:00+0900 source=input/app.log
1:2026-09-01T09:00:00+0900 INFO request_id=R001 start
2:2026-09-01T09:00:01+0900 ERROR request_id=R001 database timeout
```

注意：不能先创建正式文件再判断 `grep` 是否匹配，否则失败途中可能留下半份成果。路径仍需双引号引用，搜索仍使用固定文本匹配。

## 7.4 自测观点评审表

| 编号 | 观点评审 | 输入 | 预期 |
|---|---|---|---|
| T01 | 正常系 | `app.log R001` | 状态 0；头信息 1 行；明细 2 行 |
| T02 | 0 件 | `app.log R999` | 状态 1；不新建 `R999.log` |
| T03 | 文件不存在 | `missing.log R001` | 状态 3；stderr 可定位路径 |
| T04 | ID 格式错误 | `app.log 'R 001'` | 状态 2；不执行搜索 |
| T05 | 路径含空格 | `input/app copy.log R001` | 状态 0；结果正确 |
| T06 | 再执行 | 连续执行 T01 两次 | 结果没有重复追加 |
| T07 | 退化确认 | 比较输入文件前后校验值 | 输入内容未变化 |

“正常系”是正常流程，“异常系”是参数错误等失败流程，“境界值”是边界输入。自测不能只展示成功画面，还要覆盖这些代表性分支。

可用以下方式保存输入日志的前后证据：

```bash
sha256sum input/app.log > output/input.before.sha256
# 执行全部测试
sha256sum input/app.log > output/input.after.sha256
diff -u output/input.before.sha256 output/input.after.sha256
```

`sha256sum` 生成内容摘要；摘要相同可以作为文件内容未变化的证据。

## 7.5 Git 提交与交付材料

在任务分支中检查并提交：

```bash
git status
git diff -- check_log.sh
bash -n check_log.sh
# 执行 T01～T07
git add check_log.sh
git diff --staged
git commit -m "fix: add investigation metadata to log result"
```

提交或 PR/MR 说明至少包含：

- 对应票号与改修目的
- 修改文件和影响范围
- T01～T07 的结果与退出状态
- 未执行项目及原因
- 已知限制，例如只支持 Bash 4 以上

Review 指摘対応时，回复“修改位置、修改理由、再测试结果”，不要只回复“已修复”。Git 的完整团队流程见[团队协作与代码评审](../git/06_teamwork_and_conflicts.md)。

## 7.6 验收标准

- 新要求和既有行为都有测试证据。
- 修改仅限任务需要的文件，没有混入格式化或无关改动。
- 另一名学习者可以根据说明复现测试。
- 输出不含秘密信息、个人信息或整段生产日志。

[上一章：日志检查脚本](06_log_checker.md) · [返回课程入口](index.md)
