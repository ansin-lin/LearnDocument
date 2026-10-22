# 第 5 章：进程管理与系统服务

本章继续使用第 2 章的 `~/learndoc-linux-lab`，在 Bash 中以普通用户练习。完成后，你应能区分程序文件、进程和系统服务；找到进程并读懂 PID、资源、服务状态及日志；在自己的练习环境结束自己启动的 `sleep` 进程。对 Java 应用、端口和接口的调查以阅读已有环境为主，不在项目主机上练习服务启停。

## 5.1 程序、进程与系统服务

**程序文件**保存在磁盘上，**进程**是程序启动后正在运行的实例。Java 应用由 JVM 执行；项目构建出的 Spring Boot JAR 可在有匹配 Java 运行环境的机器上通过 `java -jar 实际文件.jar` 尝试启动。第 2 章的 `training-app` 只是模拟目录，**没有可运行的 JAR**。

```text
Spring Boot JAR 文件
        ↓ java -jar 启动
Java 进程（有 PID）
        ↑
systemd 可以按已有服务配置管理该进程
```

**服务**是按管理规则启动、停止和观察的运行单元，不等同于一个普通进程。JAR 存在不代表进程在运行；Java 进程存在不代表 API 可用；systemd 显示 `active` 也不能保证所有业务接口正常。本章不创建服务配置。

## 5.2 PID 与 PPID

**PID（Process ID）**是当前机器上一个进程的编号；**PPID（Parent Process ID）**是其父进程的编号。一个进程可以启动其他进程，被启动的进程通常会记录启动它的父进程 PID。先在 Bash 终端输入：

```bash
ps -f
```

`ps` 显示执行这一刻的进程信息，`-f` 让输出包含较完整的字段。下面是为了认识字段而**简化的模拟输出**，实际结果还可能包含时间、终端等列：

```text
UID       PID  PPID  CMD
student  1234  1000  bash
student  1250  1234  ps -f
```

`UID` 是运行用户，`PID` 是本进程编号，`PPID` 是父进程编号，`CMD` 是启动命令。例如模拟结果中，`ps -f` 的父进程是 PID 为 `1234` 的 Bash。PID 会变化，进程退出后编号也可能被复用，不要把教材数字当作自己机器的实际 PID。

## 5.3 查看和查找进程

在当前机器只读查看进程：

```bash
ps -f
ps aux
pgrep -af java
```

`ps -f` 通常查看当前终端相关进程的详细信息；`ps aux` 扩大查看范围，`a` 包含其他用户的终端进程，`u` 用用户相关格式显示，`x` 再包含无控制终端的进程。它会显示较多列，其中 `USER` 是运行用户、`PID` 是进程编号、`%CPU` 和 `%MEM` 是资源使用情况、`COMMAND` 是启动命令。`ps` 是执行时的**快照**，不会持续刷新；项目中也常见 `ps -ef`，其中 `-e` 表示查看所有进程、`-f` 表示详细格式。

`pgrep` 查找进程；`-a` 显示匹配进程的命令行，`-f` 按完整命令行匹配，`java` 是搜索文字。没有匹配时可能不输出任何内容；匹配到多个 Java 进程时，结合用户、JAR 路径和启动命令确认目标。例如下面是**模拟结果**：

```text
2410 java -jar /opt/training-app/training-app.jar
```

若已从当前机器确认某个 PID，可按编号查看。下面的 `2410` 只配合上述模拟结果说明写法，实际执行时必须替换成当次查到的 PID：

```bash
ps -p 2410 -o pid,ppid,user,stat,etime,cmd
```

`-p` 指定 PID；`-o` 指定显示列：PID、PPID、用户、`stat`（状态）、`etime`（已运行时间）、`cmd`（启动命令）。输出只说明该进程在查询时刻存在。项目手顺中还可能看到 `ps aux | grep '[j]ava'`，它用 `grep` 过滤进程列表并避免通常情况下把搜索命令自身匹配出来；初学时优先用 `pgrep -af java`。当前机器查不到进程时，也要先核对主机或容器是否正确。

## 5.4 前台、后台与结束进程

先在个人 Bash 环境输入 `sleep 10`：命令在**前台**运行，当前终端大约等待 10 秒后才重新出现提示符。行尾加 `&` 则放到**后台**，终端可以继续接收命令。下面只对自己启动的 `sleep` 做一次完整练习；不要对未知 PID 或项目中的 Java 进程照做。

```bash
sleep 300 &
demo_pid=$!
jobs -l
ps -p "$demo_pid" -o pid,user,cmd
kill -TERM "$demo_pid"
wait "$demo_pid"
ps -p "$demo_pid" -o pid,cmd
```

`sleep 300` 等待 300 秒；`&` 把任务放到后台；`$!` 是当前 Bash 刚启动的后台任务 PID，`demo_pid=$!` 把它保存下来。`jobs -l` 显示**当前 Shell** 管理的后台任务及 PID，不等于全系统进程列表；`ps -p` 再确认 PID 对应自己的 `sleep`。`kill -TERM` 发送 **SIGTERM**，请求进程正常结束，但发送成功不保证任意目标都会立即退出。`wait` 只等待当前 Shell 可以等待的子任务，这里正是刚启动的 `sleep`；被信号结束时它可能返回非 0。最后 `ps` 应只显示表头或不显示进程行，表示该 PID 此刻已不在运行。若 `sleep` 已提前结束，先重新核对，不改用其他 PID。

`kill` 是**发送信号**，不是删除文件。省略信号的 `kill PID` 通常也发送 SIGTERM；`kill -KILL PID` 发送 **SIGKILL**，强制结束而不给程序正常清理的机会，本章不执行。`$!`、变量赋值和 `wait` 的系统用法留到 [Shell 课程](../shell/index.md)；在这里能看懂这一次练习即可。

项目手顺中可能出现下面的组合写法，**仅阅读，不在本章执行**：

```text
nohup command > output.log 2>&1 &
```

`nohup` 使程序忽略终端挂断信号，不会自行把程序放到后台；末尾 `&` 才负责后台运行。`>` 保存正常输出，`2>&1` 将错误输出送到相同位置。`command`、`output.log` 是示意名称，不能原样用于项目；输入输出的完整语法见[第 4 章](04_text_log_shell_basic.md)。

## 5.5 查看系统资源

应用变慢或启动后退出时，先观察资源现象。以下命令只读取当前机器的信息，不修改系统设置：

```bash
top
free -h
df -h "$HOME"
du -sh "$HOME/learndoc-linux-lab"
```

| 命令 | 作用及当前写法 |
|---|---|
| `top` | 持续查看进程的 CPU、内存使用，按 `q` 退出 |
| `free -h` | 查看系统内存和 swap；`-h` 使用易读单位 |
| `df -h "$HOME"` | 查看家目录所在文件系统的容量与剩余空间；`-h` 使用易读单位 |
| `du -sh "$HOME/learndoc-linux-lab"` | 查看练习目录占用空间；`-s` 汇总，`-h` 使用易读单位 |

`free` 看系统内存，`df` 看文件系统空间，`du` 看指定目录占用。`df` 与 `du` 的数字不是同一种指标，不能直接比较；也不能仅凭 `free` 的某一列就断定内存不足。

## 5.6 systemd 服务管理

本节用于读懂项目中的服务管理命令。**systemd** 是部分 Linux 环境使用的系统与服务管理器；在线练习环境未必支持它，也未必存在示例服务。只有项目手顺确认目标机器使用 systemd、服务名称正确且当前用户有读取权限时，才运行状态查询：

```bash
systemctl status training-app.service --no-pager
```

`systemctl` 用于查询或操作 systemd 服务；`status` 查看状态，`--no-pager` 直接显示结果，`training-app.service` 是**假设已存在**的示例服务名。没有该服务或没有 systemd 时，不要求命令执行成功；本章不编写 `.service` 文件。

下面三条只用于识别作业手顺中的动作，**不在项目环境练习**：

```text
systemctl start training-app.service    # 请求启动已停止的服务
systemctl stop training-app.service     # 请求停止服务，可能中断请求
systemctl restart training-app.service  # 停止后重新启动，可能造成短暂中断
```

`start`、`stop`、`restart` 会改变服务状态，可能中断请求并需要授权；看到异常不能直接重启。[systemd 官方手册](https://www.freedesktop.org/software/systemd/man/latest/systemctl.html)可查阅详细行为。

## 5.7 查看系统服务日志

若服务由 systemd 管理，且当前用户有读取权限，可以查看该服务最近的 journal 记录：

```bash
journalctl -u training-app.service -n 100 --no-pager
```

`journalctl` 读取 systemd journal；`-u` 限定服务单元，`-n 100` 取最近 100 条，`--no-pager` 直接显示。`training-app.service` 仍是示例名称；能看到哪些日志还取决于当前用户权限。应用日志也可能在项目文件或其他日志平台，不能只查 journal。[systemd 官方日志手册](https://www.freedesktop.org/software/systemd/man/latest/journalctl.html)说明了这些选项。

如果已确认故障发生的时间段，可进一步只读筛选（以下日期、时间和服务名仅作写法示例，不要求在练习环境执行）：

```bash
journalctl -u training-app.service --since '2026-09-01 09:00:00' --until '2026-09-01 09:10:00' --no-pager
```

`--since` 表示从指定时刻起，`--until` 表示截至指定时刻；执行前按项目记录核对环境、服务名和时区。没有 systemd、无权限或日志不在 journal 时，改用项目指定的日志来源，不要为练习创建服务。

## 5.8 Java 应用异常调查

测试人员反馈员工管理 API 无法访问时，先确认报告对应的环境、主机和时间，再逐层观察，不把“有 Java 进程”直接当作“接口正常”：

```text
确认目标 Linux 主机与时间
        ↓
查 Java 进程；若使用 systemd，再看服务状态
        ↓
查看 CPU、内存和磁盘现象
        ↓
确认目标端口是否监听
        ↓
请求项目已知的健康接口
        ↓
按时间与请求标识调查应用日志
```

前几层使用本章已讲的命令。`curl -i 地址` 的核心作用是发送 HTTP 请求并显示响应头与正文；实际项目查询还应加连接与总时长限制。只有项目手顺明确**当前主机**的监听端口和 `/health` 接口，并且确有测试应用时，才运行下面两条示意命令；本课程的模拟目录不提供服务或 8080 端口：

```bash
ss -lnt
curl --connect-timeout 3 --max-time 10 -i http://localhost:8080/health
```

`ss` 列出网络连接信息；`-l` 只看监听、`-n` 用数字显示地址和端口、`-t` 只看 TCP。看见端口监听只能确认有人在该地址与端口等待连接，不能证明就是目标 Java 应用。`curl` 发送 HTTP 请求；`--connect-timeout 3` 限制连接等待 3 秒，`--max-time 10` 限制整个请求 10 秒，`-i` 同时显示响应头与正文。收到 HTTP `200` 只说明这次健康请求成功，业务功能仍可能失败；`404` 表示路径未找到，`500` 表示服务器处理出错。默认情况下，收到 HTTP 500 时 `curl` 也可能以退出状态 `0` 结束，**HTTP 状态码与命令退出状态不是一回事**。详细 HTTP 概念见[Web 基础](../../web_basics/01_http_rest_cookie_cors.md)，网络命令将在[第 6 章](06_network_ssh.md)结合连接问题继续使用。

| 现象 | 先确认什么 | 不应直接下的结论 |
|---|---|---|
| Java 程序启动后自动结束 | 退出时刻、启动命令、服务状态和对应日志 | “一定是代码问题” |
| 日志写入失败或文件系统快满 | `df -h`、目标路径、写入用户和错误信息 | “只要删日志就能恢复” |
| 响应缓慢或疑似内存不足 | `top`、`free -h`、Java 进程和相关日志 | “free 的某个数字低就是内存泄漏” |

课堂练习：在自己的 Bash 环境先运行 `ps -f`、`pgrep -af java`，找不到 Java 进程也属正常；完成 5.4 节的后台 `sleep` 实验，核对 PID 后发送 SIGTERM，再确认进程已结束。最后运行 `free -h`、`df -h "$HOME"`、`du -sh "$HOME/learndoc-linux-lab"`，分别指出内存、文件系统容量与目录占用。`systemctl`、`journalctl`、`ss` 和 `curl` 只需读懂示例；没有真实服务时不要求执行成功。练习不会修改第 2 章的样例；下一章开始学习网络与远程连接。

[上一章：文本处理、日志查看与 Shell 基础](04_text_log_shell_basic.md) · [下一章：网络、SSH 与远程服务器操作](06_network_ssh.md)
