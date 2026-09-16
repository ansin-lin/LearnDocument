# 08 Web/批处理项目调查演习

本演习整合前七章的命令，模拟日本项目中的障害调查和结果报告。所有操作限定在 `~/learndoc-linux-lab`。

## 8.1 任务说明

现象：测试环境中，员工查询请求 `R001` 返回失败。请调查样例日志，确认文件和权限状态，并提交可复核的调查结果。禁止修改原始 `input/app.log`。

## 8.2 验收要求

- 记录用户、主机和当前目录
- 记录日志文件权限、大小和更新时间
- 提取 `R001` 的全部相关行
- 说明确认到的错误、影响范围和未确认事项
- 所有证据保存在 `output/final-investigation/`
- 不使用 sudo，不修改系统目录，不删除原始数据

## 8.3 执行

```bash
cd "$HOME/learndoc-linux-lab"
mkdir -p output/final-investigation

{
  whoami
  hostname
  pwd
  ls -l -- input/app.log
} > output/final-investigation/environment.txt

grep -n 'request_id=R001' input/app.log \
  > output/final-investigation/R001.log

wc -l output/final-investigation/R001.log
```

根据第 06 章模板编写 `output/final-investigation/report.md`。

## 8.4 验证

```bash
test -s output/final-investigation/environment.txt
test "$(wc -l < output/final-investigation/R001.log)" -eq 2
grep -q 'employee not found' output/final-investigation/R001.log
grep -q '影响范围' output/final-investigation/report.md
grep -q '残课题' output/final-investigation/report.md
printf 'acceptance_status=%s\n' "$?"
```

最后一条为 `0` 只证明最后一个检查成功。正式验收应逐条确认，或使用后续 Shell 课程把检查组合成脚本。

## 8.5 交接内容

向团队提交：环境、现象、调查时间范围、执行命令、确认事实、证据路径、影响范围、暂定对策和残课题。不要把个人信息、令牌或未经脱敏的生产日志提交到 Git。

[上一章：Vim 最小编辑操作](07_vi_vim_basic.md) · [进入 Shell 课程](../shell/index.md)
