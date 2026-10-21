# 08 Web/批处理项目调查演习

本演习整合前七章的命令，模拟日本项目中的障害调查和结果报告。所有操作限定在 `~/learndoc-linux-lab`。

## 8.1 任务说明

现象：测试环境中，员工查询请求 `R001` 返回失败。请调查样例日志，确认文件和权限状态，并提交可复核的调查结果。禁止修改原始 `input/app.log`。
已知条件：本次只在自己的练习目录操作；报告时间为样例日志的 `2026-09-01 09:00` 附近。先自己完成 R001 调查，再区分“已确认事实”和“原因候选”。

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

`{ ... } > environment.txt` 把四条确认命令的输出保存为一份环境证据。`grep -n` 抽取与 R001 相关的日志并保留原行号；`wc -l` 应显示 2。运行前先确认 `input/app.log` 是第 02 章建立的练习日志，不能对生产日志照搬示例路径。

根据第 06 章模板编写 `output/final-investigation/report.md`。

## 8.4 验证

```bash
test -s output/final-investigation/environment.txt && printf '环境证据：通过\n'
test "$(wc -l < output/final-investigation/R001.log)" -eq 2 && printf '日志行数：通过\n'
grep -q 'employee not found' output/final-investigation/R001.log && printf '错误内容：通过\n'
grep -q '影响范围' output/final-investigation/report.md && printf '影响范围：通过\n'
grep -q '残课题' output/final-investigation/report.md && printf '残课题：通过\n'
```

`test -s` 检查文件存在且非空；`-eq` 比较整数；`grep -q` 只报告是否找到文字，不打印匹配行。`&&` 表示左边成功才打印“通过”。应看到五条“通过”；缺少任何一条就回到对应文件调查，不要只看最后一条命令的退出状态。

## 8.5 Web API 的一次切り分け（初步定位）

测试人员反馈「社員一覧 API にアクセスできない（无法访问员工列表 API）」。本节是**模拟输出阅读题**，无需启动 Spring Boot 或连接未知服务器。项目手顺确认目标为测试主机 `test-app-01`、本机端口 `8080`，且 `/health` 是已知健康接口：

```text
$ hostname
test-app-01
$ pgrep -af java
2410 java -jar employee-app.jar
$ ss -lnt
LISTEN 0 100 127.0.0.1:8080 0.0.0.0:*
$ curl --connect-timeout 3 --max-time 10 -i http://localhost:8080/health
HTTP/1.1 200 OK
Content-Type: text/plain

UP
$ curl --connect-timeout 3 --max-time 10 -i http://localhost:8080/employees
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{"message":"unexpected error"}
```

以上 `$` 仅表示**示例输出中的终端提示符**，不是给学员复制的命令。先独立填写“进程、监听、健康接口、业务接口”四项观察及下一步命令，再核对：可以确认当前主机正确、有一个 Java 进程、只在 `127.0.0.1:8080` 监听、健康接口返回 200，而员工列表接口返回 500。**不能确认**监听进程一定就是该 Java 应用，也不能仅凭 500 确定根因。下一步应按请求时间或 request ID 查对应应用日志，并核对外部访问路径；`127.0.0.1` 只接受本机连接，不证明测试人员所在机器能直连。

若 `curl` 显示连接拒绝，先核对主机、地址、端口和进程；收到 HTTP 404，说明有 HTTP 响应但该路径未找到；收到 500，再查应用日志和请求标识。将三种现象分别写成已确认事实，不把“接口失败”笼统归因于网络。

## 8.6 夜间批处理异常（模拟资料）

障害票写着「夜間バッチが異常終了した（夜间批处理异常结束）」。项目手顺与执行记录给出：

```text
ジョブ：employee-import
予定：02:00；実行履歴：02:00～02:01
実行ユーザー：batchuser；終了コード：3
標準出力：0 行
標準エラー：ERROR input missing: input/employees.csv
出力ファイル：未生成
```

先依据这份资料自己写出：已确认的事实、可能原因、仍缺少的证据各一项，并说明为何不能马上重跑；再阅读下段核对。

应确认作业名、计划和实际时间、执行账号、退出状态、stdout/stderr 及输入输出文件的存在状态。这里能确认“脚本报告输入文件缺失并以 3 结束”，但文件为何缺失、是否影响后续ジョブ、是否允许再执行仍是残课题。**不要立即重跑**：批处理可能产生重复导入、发送或更新，应先核对作业手顺、依赖关系和业务负责人意见。本场景只分析已给证据，不要求登录真实批处理服务器。

## 8.7 交接内容

向团队提交：环境、现象、调查时间范围、执行命令、确认事实、证据路径、影响范围、暂定对策和残课题。不要把个人信息、令牌或未经脱敏的生产日志提交到 Git。

最终交付包括基础 R001 实操证据，以及对上述 Web、Batch 两段模拟资料的简短判断：哪些是事实、哪些是原因候选、哪些仍待确认。**必须掌握**环境与日志核对；**应当掌握**按进程→端口→HTTP→日志逐层调查；**了解即可**批处理调度与再执行授权边界。

课堂检查：① R001 的 ERROR 能直接证明根因吗？② 端口在 `127.0.0.1` 监听，能证明测试人员可从别的机器直连吗？③ Batch 返回 3 和输入缺失消息能确认什么、不能确认什么？参考：不能；不能；能确认脚本报告的失败现象，不能确认文件缺失原因或是否可安全重跑。

[上一章：Vim 最小编辑操作](07_vi_vim_basic.md) · [进入 Shell 课程](../shell/index.md)
