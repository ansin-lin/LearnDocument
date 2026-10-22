# 第 8 章：Linux 综合实战与常见故障排查

本章先使用**模拟命令输出**判断员工 API 故障，再在第 2～4 章建立的个人练习目录中独立编写文件与日志调查命令。不要求登录服务器或部署应用。学完后，你应能保存可核对的调查证据，区分已确认事实、原因候选和下一步事项。真实项目的处理和回退应遵循项目手顺。

调查时先确认报告中的**环境、发生时间与接口**，再按需要核对进程/服务、监听端口、HTTP 响应和同一时间或 `request_id` 的日志；最后向负责人报告。已有证据足够定位某一层时，不必机械重复所有检查。进程存在、端口监听、健康接口成功，分别只能证明各自那一步，不能代替业务接口验证。

## 8.1 员工接口故障：只根据模拟证据判断

测试人员报告测试环境的员工列表 API 失败。以下是**模拟证据**，不是要求你执行的命令输出：

```text
环境：测试环境；主机：test-app-01；应用：training-app
进程：2410 java -jar /opt/training-app/app.jar
服务：training-app.service active (running)
监听：127.0.0.1:8080 LISTEN
GET http://127.0.0.1:8080/health        → HTTP 200，{"status":"UP"}
GET http://127.0.0.1:8080/api/employees → HTTP 500
journal：employee list failed
```

请写出：① Java 进程和服务是否在运行；② 8080 是否监听、监听在哪个地址；③ 健康接口和员工接口各是什么状态；④ 目前确认的问题与影响范围；⑤ 仍需调查的原因；⑥ 向负责人交接哪些证据。证据没有给出故障发生时间或 `request_id`，这两项应向报告人或日志进一步确认，不能自行补造。

参考判断：Java 进程存在，systemd 报 active，`127.0.0.1:8080` 正在监听；健康接口 `200`，员工接口 `500`，因此**已确认本次员工列表请求失败**。日志表明应用记录了失败，但不能仅凭这一行断定是代码、配置还是外部依赖造成的。还需核对对应时间的错误堆栈、相关请求及近期变更。`200` 只证明健康接口这次请求成功，不代表全部业务正常。

## 8.2 按证据继续调查与交接

如果项目已授权你查看该主机，且已告知服务名、端口和接口，可以按第 5、6 章的方法读取状态。下面只是**用于理解调查顺序的命令示意**；本课程没有运行中的示例服务：

```bash
pgrep -af 'training-app/app.jar'
systemctl status training-app.service --no-pager
ss -lnt
curl -i --connect-timeout 3 --max-time 10 http://127.0.0.1:8080/health
curl -i --connect-timeout 3 --max-time 10 http://127.0.0.1:8080/api/employees
journalctl -u training-app.service -n 100 --no-pager
```

`pgrep` 与 `systemctl` 分别查看目标进程和服务状态；`ss` 查看监听地址；两条 `curl` 分别检查健康和业务接口；`journalctl` 查看最近服务日志。日志也可能在项目文件或集中平台，实际位置以项目说明为准。`curl` 默认收到 HTTP 500 时也可能以命令状态 `0` 结束，所以必须阅读**HTTP 状态行**。只有进程与端口而没有成功的业务响应，不能报告“接口已恢复”。

交接时记录环境、主机、报告时间、接口与 HTTP 状态、进程/服务、监听地址、对应日志、已确认事实、影响范围、原因候选和待确认事项。事实可写“员工接口返回 500”；“数据库故障”在没有依赖证据时只能列为候选。若近期刚发布，旧版本是否已验证、能否回退也应由负责人按手顺判断；**回退后仍需验证业务接口**，不能只看服务是否 active。

## 8.3 其他常见故障：先切分问题

| 现象或模拟报错 | 先查的证据 | 可以确认什么；还不能确认什么 |
|---|---|---|
| `Address already in use: bind` | `ss -lntp`，核对目标端口的占用者与启动参数 | 可确认端口已有监听者；不能据此认定可以结束它，更不能杀掉未知进程 |
| `Permission denied`，日志写入失败 | 实际运行用户、日志文件及父目录的 `ls -l`/`ls -ld` | 可确认权限与运行身份是否匹配；未核对完整路径前，不能断定只需改目标文件权限，更不能直接 `chmod 777` |
| 健康接口 200，业务 API 500 | 对应请求的应用日志、版本与配置 | 可确认两种请求的结果不同；不能断定所有网络路径正常或数据库一定故障 |
| 项目型应用报数据库连接失败 | 数据库地址、端口、凭据来源、日志时间及依赖服务状态 | 可确认应用记录了连接失败；不能仅凭一条报错断定数据库宕机，也不能擅改生产密码 |
| 服务监听 `127.0.0.1:8080`，外部浏览器直连失败 | 项目预定入口、绑定地址、代理与访问控制 | 可确认应用仅在本机地址监听；不能断定应直接开放防火墙或云安全组 |

浏览器/React → Linux → Spring Boot → 数据库只是可能的项目链路；实际系统还可能经过代理、TLS 和其他组件。本章员工接口案例没有提供数据库证据，表中的数据库故障是另一种项目场景。记录证据后按负责范围交接，不越权修改网络或数据库。

## 8.4 最终练习

先提交 8.1 节六个问题的判断，再从 8.3 节选两种故障，各写出“现象、要看的证据、已确认事实、尚不能确认的原因、下一步”。接着完成下面的**个人目录实操**。若练习环境曾重置，先按第 2 章重建 `~/learndoc-linux-lab` 和四行日志，再按第 4 章 4.2 节建立 `training-app/logs/application.log`；全程从 `~/learndoc-linux-lab` 执行，不触碰真实项目日志。

测试人员反馈员工查询 API 在样例时间 `09:05` 左右失败，请提交 `output/final-investigation/` 下的证据与简短报告。已知日志在 `training-app` 下，但没有给出具体文件名，只知道文件名含 `app` 且以 `.log` 结尾；同目录可能有多份日志。学员先不要看参考命令，自行完成：

1. 用 `find` 定位候选日志，再用内容中的 `request_id=R101` 判断哪份与本次报告有关；记录所选路径。
2. 用 `tail` 查看该日志最后 100 行；用 `grep` 查 ERROR、R101、09:05 及 ERROR 前后文，并统计含 ERROR 的行数。
3. 把相关 ERROR 和异常上下文用 `>` 保存到证据文件，再用 `>>` 追加同一请求的其他记录；用 `cat` 读回证据，并确认所有写入目标都位于 `output`，不要改写原始日志。
4. 将 `input/app.conf` 复制为 `output/final-investigation/` 内的备份和待编辑副本；只用 `vi` 将待编辑副本的 `mode=batch` 改为 `mode=web`，再用 `diff -u` 核对原件与副本差异。若没有 `vi`，先完成其他步骤，编辑任务到有编辑器的练习环境再做。
5. 在调查记录中先用 `>` 写入“调查对象”，再用 `>>` 追加“已确认事实”和“未确认原因”，最后读回文件。不要把 HTTP 500 直接写成“数据库故障”。

验收时应能看到：候选日志中包含 `training-app/logs/application.log`；该文件含 R101 的 ERROR、异常类型和调用位置；含 ERROR 的行数为 `1`；原件 `input/app.conf` 仍为 `mode=batch`，待编辑副本为 `mode=web`；证据文件和调查记录位于 `output/final-investigation/`。不同命令组合只要结果一致且未覆盖原始资料，都可以接受。

<details><summary>参考命令（独立完成后展开）</summary>

```bash
cd "$HOME/learndoc-linux-lab"
mkdir -p output/final-investigation
find training-app -type f -name '*app*.log'
grep -n 'request_id=R101' training-app/logs/application.log
tail -n 100 training-app/logs/application.log
grep -n 'ERROR' training-app/logs/application.log
grep -n '2026-09-01T09:05' training-app/logs/application.log
grep -n -C 2 'ERROR request_id=R101' training-app/logs/application.log > output/final-investigation/R101-evidence.txt
grep -n 'request_id=R101' training-app/logs/application.log >> output/final-investigation/R101-evidence.txt
grep 'ERROR' training-app/logs/application.log | wc -l
cat output/final-investigation/R101-evidence.txt
cp -i -- input/app.conf output/final-investigation/app.conf.bak
cp -i -- input/app.conf output/final-investigation/app.conf.edit
vi output/final-investigation/app.conf.edit
diff -u input/app.conf output/final-investigation/app.conf.edit
printf '调查对象：员工查询 API\n' > output/final-investigation/investigation.txt
printf '已确认事实：R101 记录 ERROR 和异常堆栈\n未确认原因：仍需核对代码和依赖\n' >> output/final-investigation/investigation.txt
cat output/final-investigation/investigation.txt
```

`diff -u` 显示预期差异时返回状态 `1`，不代表文件读取失败。若重复练习，`cp -i` 可能询问是否覆盖已有副本，先核对再回答；`>` 会重建证据和调查记录，原始 `input`、`training-app/logs` 文件不应被修改。

</details>

[上一章：运行环境与 Spring Boot 部署](07_java_deployment.md) · [返回 Linux 课程入口](index.md)
