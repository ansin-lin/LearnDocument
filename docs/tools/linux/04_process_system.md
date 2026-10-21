# 04 进程、退出状态与资源确认

PG 需要确认应用或批处理是否运行，并安全处理自己启动的练习进程。本章不包含系统服务启停。

## 4.1 进程和 PID

进程是正在运行的程序，每个进程有 PID。查看自己的进程：

```bash
ps -f
ps -u "$(whoami)" -f
```

常见字段包括 UID、PID、PPID 和 CMD。查找时优先使用 `pgrep`，避免把 `grep` 自身算进去：

```bash
pgrep -a -u "$(id -u)" bash
```

`ps -f` 显示当前终端相关进程；`ps -u 用户名 -f` 显示指定用户的进程。PID 是进程编号，PPID 是父进程编号，CMD 是启动命令。`pgrep` 按名称找进程，`-u "$(id -u)"` 限定为当前用户，`-a` 同时显示命令行；看到多个 Bash 进程不代表异常。

调查既有 Java 应用时，还可能读到以下**只读**命令；先用 `hostname` 核对服务器，不要将查到的 PID 直接交给 `kill`：

```bash
ps -ef
ps aux
pgrep -af java
```

`ps -ef` 中 `-e` 选全部进程，`-f` 用完整格式显示。`ps aux` 是另一套常见写法：`a` 包含有终端的其他用户进程，`u` 显示用户、CPU/内存等列，`x` 还包含无控制终端的进程；列表可能很长。`pgrep -af java` 按**完整命令行**查含 `java` 的进程，`-a` 显示命令行，`-f` 扩大匹配范围，因此可能包括非目标应用；要再核对用户、完整启动命令和项目手顺。

`ps -ef` 的简化输出示例：

```text
UID       PID  PPID  C STIME TTY          TIME CMD
appuser  2410     1  0 09:00 ?        00:00:12 java -jar employee-app.jar
```

`UID` 是运行账号，`PID` 是当前进程编号，`PPID` 是启动它的父进程编号，`CMD` 是启动命令。`STAT` 是 `ps aux` 或本章 `ps -o stat` 形式中可见的状态列，并非上面 `ps -ef` 输出的列。仅看到 `java` 不够，还要把账号、JAR 名称、主机和项目手顺对上；PID 可能变化，不要凭一次截图直接结束进程。

## 4.2 建立可控练习进程

```bash
sleep 300 &
demo_pid=$!
printf 'demo_pid=%s\n' "$demo_pid"
ps -p "$demo_pid" -o pid,ppid,user,stat,etime,cmd
```

`&` 让命令在后台运行，`$!` 是最近启动的后台进程 PID。只对刚刚保存的 `demo_pid` 操作，不复制其他环境的 PID。
`sleep 300` 会等待 300 秒，便于观察；`ps -p "$demo_pid"` 只查看该 PID，`-o` 指定要显示的列。应看到命令为 `sleep 300`。若没看到，先确认进程是否已经结束，不要猜测其他 PID。

## 4.3 安全结束进程

```bash
kill -TERM "$demo_pid"
wait "$demo_pid"
printf 'wait_status=%s\n' "$?"
```

`TERM` 请求进程正常结束，使其有机会清理资源。只有负责人确认进程无法响应、影响和恢复方法都明确时，才考虑 `KILL`；不要在共享环境按名称批量结束进程。
`kill` 在这里是“发送结束请求”，不是按名称删除文件。`wait` 等待自己启动的后台进程结束；被 TERM 结束时，其状态通常为非 0，这是信号结束的结果，不代表 `kill` 没有生效。

确认进程已结束：

```bash
ps -p "$demo_pid"
printf 'ps_status=%s\n' "$?"
```

`ps` 返回非 0 表示没有找到该 PID。

## 4.4 命令退出状态

每条命令都会返回退出状态，必须紧接命令读取：

```bash
grep 'ERROR' input/app.log
status=$?
printf 'grep_status=%s\n' "$status"
```

后续任何命令都会覆盖 `$?`，所以要立即保存。脚本应根据命令文档解释状态，而不是把所有非 0 都当成同一种错误。

## 4.5 只读资源确认

```bash
uptime
free -h
df -h "$HOME"
du -sh "$HOME/learndoc-linux-lab"
```

这些命令分别帮助确认负载概况、内存、主目录所在文件系统容量和练习目录大小。PG 只负责记录异常现象；扩容、挂载和系统调优不在本课程范围。
`uptime` 还显示系统已运行时间；`free -h` 中的 `-h` 使用易读单位；`df -h "$HOME"` 查看主目录所在文件系统的可用空间；`du -sh` 汇总练习目录自身的大小。这些都是只读观察，不等于已经确认应用故障原因。

需要实时观察进程时可运行 `top`，主要看 CPU、内存、PID 和命令名，按 `q` 退出；它是观察工具，不负责修复资源问题。

## 4.6 端口是否在监听

端口是同一台主机上区分网络服务的数字；进程存在不等于应用已在预期端口提供服务。`ss [选项]` 可读取当前主机的网络连接信息：

```bash
ss -lnt
ss -lntp
```

`-l` 只列监听端口，`-n` 用数字显示地址与端口，`-t` 限定 TCP，`-p` 尝试显示关联进程。查看 `Local Address:Port` 是否包含项目指定的 `:8080`；当前用户看不到其他用户的进程信息时，`-p` 可能不显示进程，不能因此断定没有进程。`ss` 只反映本机端口状态，不证明 API 业务处理正常。本课程不修改防火墙或网络配置。

例如 `ss -lnt` 的简化输出：

```text
State  Recv-Q Send-Q Local Address:Port Peer Address:Port
LISTEN 0      100    127.0.0.1:8080     0.0.0.0:*
```

`LISTEN` 表示本机有进程等待 TCP 连接；`127.0.0.1` 只接受本机访问，`8080` 是端口。`Recv-Q`、`Send-Q` 是队列相关数值，本课不据此判断业务正常。下一步仍需用已知地址请求接口，不能把“端口在监听”写成“员工 API 正常”。

## 4.7 用 curl 检查已知接口

HTTP 客户端 `curl` 可以向**项目指定的测试接口**发送请求。下面的 `localhost` 仅指执行命令的这台机器；没有启动中的练习应用时只阅读命令，不会得到示例成功结果。详细 HTTP 概念见 [Web 基础](../../web_basics/01_http_rest_cookie_cors.md)。

```bash
curl --connect-timeout 3 --max-time 10 -i http://localhost:8080/health
curl --connect-timeout 3 --max-time 10 -I http://localhost:8080/health
curl --connect-timeout 3 --max-time 10 -v http://localhost:8080/health
```

`curl [选项] URL` 中 URL 由协议 `http`、主机 `localhost`、端口 `8080` 和路径 `/health` 组成。`--connect-timeout 3` 把建立连接的等待限制为 3 秒，`--max-time 10` 把整个请求限制为 10 秒。`-i` 在正文前显示 HTTP 响应头；`-I` 通常发送 HEAD，只请求响应头，有些接口不支持 HEAD，不能据此断定 GET 请求失败；`-v` 把连接和协议细节写到 stderr，可能包含敏感头，不能原样提交。优先使用 `-i`，仅在需要连接细节时使用 `-v`。

若测试服务正常，`-i` 可能显示 `HTTP/1.1 200 OK`、空行和正文 `UP`；`-I` 主要看到响应头，`-v` 还可能显示 `* Connected to ...` 等连接过程。三种结果关注点不同；未启动本地服务时会出现连接错误，不应为了凑出 `200` 去访问未知地址。

连接拒绝表示此目标地址和端口未建立连接；收到 `404` 表示服务器响应但该路径未找到；`500` 表示服务器返回内部错误。HTTP 状态码和 `curl` 进程的退出状态是两回事：普通 HTTP `404/500` 不一定让 `curl` 返回非 0，必须同时查看响应与命令错误信息。不要用 `-k` 绕过 HTTPS 证书检查。

## 4.8 只读查看已知服务

项目环境使用 systemd、且作业手顺给出了服务名称时，可读状态：

```bash
systemctl status <service-name> --no-pager
systemctl is-active <service-name>
```

`systemctl` 与服务管理器通信；`status` 显示详细状态，`is-active` 只回答是否活跃。`<service-name>` 必须替换为项目提供的服务名，不能原样复制；`--no-pager` 避免进入分页查看。服务显示 `active` 只说明服务管理器认为它在运行，**不证明业务 API 正常**。没有 systemd、没有权限或没有已知服务名时跳过；本课程不执行 `start/stop/restart`。

调查 Web API 无法访问时，顺序是：核对环境与主机 → 查 Java 进程 → 查预期端口 → 用已知健康接口检查 HTTP → 关联应用日志。每一步记录事实和未知项，不因单项失败就猜测根因。

```text
没有目标 Java 进程 → 可能未启动或已退出，核对部署与日志
有进程但端口未监听 → 核对启动过程、端口配置与日志
端口监听但请求失败 → 核对地址、协议、路径与应用响应
健康接口正常但业务接口 500 → 调查该请求的业务日志
```

## 4.9 本章任务

1. 启动一个 `sleep 300` 后台进程。
2. 使用保存的 PID 显示进程信息。
3. 使用 `TERM` 结束并验证进程不存在。
4. 把 PID、结束前状态和验证结果写入 `output/process-evidence.txt`。
5. 如练习环境有项目指定的测试应用，只读执行 `ss -lnt` 和一条限时 `curl -i` 命令，记录“进程、端口、HTTP 响应”三个不同层面的观察；没有应用则解释上面的命令分别能证明什么，留到第 08 章分析模拟输出。

[上一章：用户、组与文件权限](03_user_permission.md) · [下一章：用户级定时任务](05_crontab_scheduler.md)

本章自查：**必须掌握**`ps`、PID、退出状态和只读资源确认；**应当掌握**根据手顺使用 `ss`、`curl`；**了解即可**`top` 与 `systemctl` 的只读状态。

课堂检查：① `PID` 与 `PPID` 各是什么？② `127.0.0.1:8080` 监听能证明远程用户能访问吗？③ `curl -I` 与 `-i` 输出关注点有什么不同？④ HTTP 500 必然让 `curl` 返回非 0 吗？参考：当前／父进程编号；不能；HEAD 响应头／通常 GET 的头和正文；不必然。
