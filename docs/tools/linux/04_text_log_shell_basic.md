# 第 4 章：文本处理、日志查看与 Shell 基础

本章模拟一个 Java / Spring Boot 项目：测试人员报告员工查询接口在 09:05 左右失败，开发者需要在获准使用的练习环境中查看应用日志、保存证据，并修改一份**配置副本**。下面的日志是教学样例，不代表真实项目的完整输出；命令在 Bash 中、从 `~/learndoc-linux-lab` 执行，不连接真实服务器。在线环境若没有 Bash，脚本部分请改用 WSL2 或教师提供的 Linux 环境。

## 4.1 文本编辑

先复制第 2 章的配置样例，只编辑副本：

```bash
cd "$HOME/learndoc-linux-lab"
cp -i -- input/app.conf output/app.conf.edit
vi output/app.conf.edit
```

`vi 文件` 在终端打开文件；环境若提供 `vim`，也可用 `vim 文件`。打开后处于**普通模式**，按 `i` 进入插入模式，移动光标并用退格键把 `batch` 改成 `web`；按 `Esc` 回普通模式，再输入 `:wq` 并回车，保存退出。若不想保存，按 `Esc` 后输入 `:q!` 并回车。`cp -i` 在副本已存在时会询问，避免无意覆盖上次编辑结果。退出后运行 `cat output/app.conf.edit`，应看到 `mode=web`；原件 `input/app.conf` 仍是 `mode=batch`。如果在线环境没有 `vi`，本节先阅读操作，改用已提供编辑器的练习环境完成。

## 4.2 查看日志

先在模拟项目中建立一份**教学用**异常日志；末尾单独一行的 `EOF` 是结束输入的标记，重复运行会覆盖这份练习日志：

```bash
cat > training-app/logs/application.log <<'EOF'
2026-09-01T09:04:59+09:00 INFO  request_id=R100 employee query completed
2026-09-01T09:05:10+09:00 INFO  request_id=R101 employee query started
2026-09-01T09:05:11+09:00 ERROR request_id=R101 employee query failed
java.lang.IllegalStateException: employee data unavailable
    at com.example.employee.EmployeeService.find(EmployeeService.java:42)
2026-09-01T09:05:12+09:00 INFO  request_id=R102 employee query completed
EOF
```

每行开头的时间戳用于对照报告时间；`+09:00` 表示该时间使用的时区。`INFO` 记录一般处理过程，`ERROR` 表示应用记录了错误，但不等于已经找到根因。相同的 `request_id=R101` 把“开始”和“失败”两行关联为同一次请求；`R100`、`R102` 则是其他请求。`java.lang.IllegalStateException` 是这次抛出的异常类型；堆栈中的 `EmployeeService.find` 和 `EmployeeService.java:42` 指向出现异常的类、方法和源码行，仍需结合代码与上下文判断原因。真实日志格式由项目决定，不能按此路径或格式猜生产环境。

```bash
less training-app/logs/application.log
tail -n 100 training-app/logs/application.log
tail -f training-app/logs/application.log
```

`less` 分页阅读，按 `q` 退出；`tail -n 100` 查看末尾最多 100 行；`tail -f` 持续等待新日志，按 `Ctrl+C` 停止。现有练习文件不会自动增长，因此运行 `-f` 后没有新行是正常的，不是命令卡死。精简在线环境若没有 `less`，可先用第 2 章的 `cat`、`head`、`tail` 查看。

了解即可：项目日志可能定期**轮转**，例如旧的 `application.log` 改名后，由新的同名文件继续接收记录；旧日志也可能被压缩。调查过去的时间段时，要先核对记录在哪份文件中。GNU `tail -f` 默认跟随已打开的文件，轮转后可能继续看旧文件；需要按文件名持续跟随时，可在确认路径和权限后使用 `tail -F training-app/logs/application.log`，它会尝试重新打开同名文件。按 `Ctrl+C` 停止；本课程不配置日志轮转。[GNU Coreutils 官方说明](https://www.gnu.org/s/coreutils/manual/html_node/tail-invocation.html)解释了两种跟随方式。

## 4.3 搜索内容

`grep [选项] 搜索文字 文件路径` 在**文件内容**中选出匹配的整行；第 2 章的 `find` 则按文件名等条件找**文件路径**。先在第 2 章建立的四行日志中练习：

```bash
grep 'ERROR' input/app.log
grep -n 'ERROR' input/app.log
grep -i 'error' input/app.log
grep -E 'ERROR|WARN' input/app.log
grep -v 'INFO' input/app.log
grep -F 'request_id=R001' input/app.log
grep -c 'ERROR' input/app.log
```

`ERROR` 是搜索文字，`input/app.log` 是被读的文件；第一条应输出 R001 的 ERROR 整行，`-n` 使第二条带上行号 `2:`。`-i` 忽略大小写，因此小写 `error` 也能找到它。`-E` 使用扩展正则，**引号里的** `ERROR|WARN` 表示二选一；这不是 Shell 管道。`-v` 输出**不**含 INFO 的行；`-F` 把 `request_id=R001` 当普通字符串，不解释正则特殊字符。`-c` 只输出匹配**行数**，这里为 `1`，不等于独立故障数，也不是关键字出现总次数。没有匹配时 `grep` 通常返回状态 `1`；文件读取失败则不能当成“无匹配”。

需要从目录中找含错误的日志，或查看 Java 异常上下文时，继续在限定的练习目录中搜索：

```bash
grep -rn 'ERROR' training-app/logs
grep -rl 'ERROR' training-app/logs
grep -n -A 2 'ERROR' training-app/logs/application.log
grep -n -B 2 'ERROR' training-app/logs/application.log
grep -n -C 2 'ERROR' training-app/logs/application.log
```

`-r` 递归读取目录中的文件；与 `-n` 组合显示路径、行号和匹配行，与 `-l` 组合只列出含匹配内容的文件路径。`-A 2` 看匹配行**后**两行，能看到样例异常类型和调用位置；`-B 2` 看**前**两行，`-C 2` 看前后各两行。数字可按所需上下文调整。若只查时间 `grep -n '2026-09-01T09:05' training-app/logs/application.log`，没有时间戳的堆栈行不会直接命中，仍要结合上下文查看。搜索不到时先核对环境、时间、路径和大小写，不能立刻认定应用没有报错。

Windows CMD 中类似用途的命令是 `findstr`，但它**不是 Linux 命令**，选项写法也不同。例如在各自环境中搜索并显示行号：Linux Bash 用 `grep -ni 'ERROR' input/app.log`，Windows CMD 用 `findstr /n /i "ERROR" app.log`；递归搜索可分别用 `grep -ri 'ERROR' training-app/logs` 和 `findstr /s /i "ERROR" *.log`（后者需在目标目录执行）。本课程的练习命令仍以 Bash 为准。

## 4.4 管道操作

管道符 `|` 把左侧命令的**标准输出**交给右侧命令作为**标准输入**；错误输出默认不经过普通管道。先在四行样例日志中观察：

```bash
cat input/app.log | grep 'ERROR'
grep 'ERROR' input/app.log | wc -l
grep 'ERROR' input/app.log | sort
grep -E 'ERROR|WARN' input/app.log | wc -l
```

第一条由 `cat` 读文件，再把文本传给 `grep`；此处直接写 `grep 'ERROR' input/app.log` 效果相同，不必每次都先加 `cat`。第二条先由 `grep` 选行，再由 `wc -l` 统计收到的行数，应为 `1`；第三条把选出的行交给 `sort` 排序。第四条的引号内 `|` 属于 `grep -E` 的二选一条件，引号外 `|` 才是 Shell 管道，结果应为 `2`。这些命令只读原日志。若左侧找不到文件，右侧仍可能显示 `0`，所以还要检查错误信息，不能把 `0` 直接解释为“没有错误”。

## 4.5 重定向

命令读写数据时有三个常见去处：**标准输入** `stdin`（编号 0，命令从这里读取数据）、**标准输出** `stdout`（编号 1，正常结果）、**标准错误** `stderr`（编号 2，错误信息）。默认通常从键盘读、把两种输出显示在终端；重定向改变其中一个去处。`<` 和 `<<` 是**提供输入**，`>` 和 `>>` 是**保存正常输出**，不要把输入和输出方向混淆。

先把第 2 章的日志作为输入交给 `wc`：

```bash
wc -l < input/app.log
```

`<` 让 `wc -l` 从文件读取内容，结果应只显示行数 `4`，不带文件名；它不会修改 `input/app.log`。相对地，`wc -l input/app.log` 也能统计，但通常还会显示文件名。

下面把搜索结果保存到自己的 `output` 目录：

```bash
grep -n 'ERROR' training-app/logs/application.log > output/error-lines.txt
grep -n 'request_id=R101' training-app/logs/application.log >> output/error-lines.txt
cat output/error-lines.txt
```

`>` 把标准输出写入目标文件；若已存在就**覆盖**。`>>` 在文件末尾**追加**，不会清空已有内容。这里第一条得到一行 ERROR，第二条追加两行 R101 记录，最后应看到三行；重复运行会先覆盖再追加。保存调查证据前先确认目标文件是否已有需要保留的内容。

多行文字也能成为命令的输入，下面只创建自己的练习报告：

```bash
cat > output/report.txt <<'EOF'
调查对象：员工查询 API
调查状态：处理中
EOF
cat output/report.txt
```

`<<` 称为 **Here Document**：把后续多行交给 `cat` 的标准输入，直到遇到单独一行的结束标记 `EOF`；标记可以换成其他一致的名称。`<<'EOF'` 中的引号让正文里的 `$变量` 等保持原样、不由 Bash 展开。`>` **另行负责**把 `cat` 的输出写入 `output/report.txt`；`<<` 本身不会写文件。最后的 `cat` 应显示两行报告，重复执行会覆盖旧报告。

下面两个示例故意搜索不存在的**练习路径**，用于观察错误流；命令返回非 0 是预期结果：

```bash
grep 'ERROR' input/missing.log 2> output/search-error.txt
grep 'ERROR' input/missing.log > output/search-all.txt 2>&1
cat output/search-error.txt
cat output/search-all.txt
```

`2>` 只保存标准错误，第一条的错误文字进入 `search-error.txt`；`2>&1` 把标准错误送到此时的标准输出目标，所以第二条把两类输出都写进 `search-all.txt`。顺序有意义：先用 `>` 指定目标，再写 `2>&1`。不要把真实项目中的完整日志或敏感错误信息直接复制到可公开的文件。

记住：`|` 把正常输出交给下一个命令继续处理；`<`、`<<` 提供输入；`>`、`>>` 保存正常输出；`2>`、`2>&1` 改变错误输出的去处。这些符号只控制数据流，不会自动判断故障原因。

## 4.6 常用文本工具

`wc` 计数，`sort` 排序，`uniq` 合并**相邻**的相同行，`cut` 按分隔符取字段。先用一份固定的小数据练习，避免把格式不稳定的 Java 堆栈按空格硬切：

```bash
printf 'INFO,R100\nERROR,R101\nINFO,R102\n' > output/levels.csv
wc -l output/levels.csv
cut -d, -f1 output/levels.csv | sort | uniq -c
```

`printf` 建立三行逗号分隔的数据；`wc -l` 应为 `3`。`cut -d, -f1` 以逗号为分隔符取第 1 列级别；`sort` 让相同级别排到一起；`uniq -c` 计数，应得到 `ERROR` 1 次、`INFO` 2 次（显示顺序和空格可能略有不同）。这只统计练习记录，不等于统计独立故障数。

需要**边看边保存**搜索结果时，使用 `tee`；需要确认配置副本改了什么时，使用 `diff`：

```bash
grep -n 'ERROR' input/app.log | tee output/errors.txt
grep -n 'WARN' input/app.log | tee -a output/errors.txt
diff -u input/app.conf output/app.conf.edit
```

`tee 文件` 接收管道中的正常输出，同时显示在终端并写入文件；默认覆盖，`-a` 才追加。前两条后，`output/errors.txt` 应有一行 ERROR 和一行 WARN。`diff 旧文件 新文件` 比较内容，`-u` 以统一差异格式显示，`-` 开头是旧内容、`+` 开头是新内容；若 4.1 节已把副本改为 `mode=web`，这里应看到 `mode=batch` 变成 `mode=web`。有差异时 `diff` 返回状态 `1` 属于正常比较结果，不等于命令损坏；两文件相同时返回 `0`。`tee` 不会自动收集管道左侧的标准错误，不能仅凭保存的文件判断搜索是否成功。

## 4.7 环境变量

**环境变量**是当前 Shell 提供给它启动的程序的设置。`PATH` 保存寻找命令的目录列表，查看时给变量加引号：

```bash
echo "$PATH"
export APP_ENV=training
echo "$APP_ENV"
```

`echo "$PATH"` 显示当前搜索路径，具体内容随环境变化；`export APP_ENV=training` 把自建的 `APP_ENV` 设为 `training`，并让之后启动的子进程也能读取。它只影响当前终端会话及其子进程，不会修改项目服务器的永久配置；不要把密码或令牌当作练习值。关闭在线环境后这项临时设置可能消失。

## 4.8 Shell 脚本认识

Shell 脚本是保存命令的文本文件，常用 `.sh` 扩展名。下面在自己的 `output` 目录中创建一个最小脚本：

```bash
cat > output/check_log_intro.sh <<'EOF'
#!/usr/bin/env bash
log_file='training-app/logs/application.log'
grep -n 'ERROR' "$log_file"
EOF
bash output/check_log_intro.sh
```

第一行 **Shebang** 指定直接执行脚本时使用 Bash；`log_file=...` 把路径保存到变量，等号两边不能有空格；`"$log_file"` 读取变量并保持路径是一个参数。这里用 `bash 文件` 显式调用 Bash，**不依赖文件的执行权限**；若要用 `./output/check_log_intro.sh` 直接执行，还要有执行权限（见第 3 章），并且环境需要能找到 Shebang 指定的 Bash。运行后应输出样例日志第 3 行。

## 4.9 Shell 简单控制

把刚才的练习脚本**替换**为接受日志路径的版本，仍只读取文件。先看执行顺序：

```text
接收第 1 个参数（日志路径）
        ↓
判断它是否为普通文件
   ├─ 否：提示路径错误，返回状态 2
   └─ 是：搜索 ERROR，返回搜索结果对应的状态
```

按这个顺序阅读脚本：

```bash
cat > output/check_log_intro.sh <<'EOF'
#!/usr/bin/env bash
log_file=$1
if [ ! -f "$log_file" ]; then
    printf 'file not found: %s\n' "$log_file" >&2
    exit 2
fi
grep -n 'ERROR' "$log_file"
exit $?
EOF
bash output/check_log_intro.sh training-app/logs/application.log
printf 'status=%s\n' "$?"
bash output/check_log_intro.sh input/app.conf
printf 'status=%s\n' "$?"
bash output/check_log_intro.sh input/missing.log
printf 'status=%s\n' "$?"
```

`$1` 是运行脚本时给出的第 1 个参数；`"$log_file"` 加引号，让含空格的路径仍作为一个文件名传给命令。`-f` 判断路径是不是普通文件，前面的 `!` 把结果取反；`if ... then ... fi` 在文件不存在时进入提示和退出分支。`exit 2` 立即结束脚本并返回状态 `2`，`>&2` 把错误提示送到标准错误。`$?` 是**紧前一条命令**的退出状态，必须马上读取；脚本把 `grep` 的状态原样返回。上述三次运行依次应得到：找到 ERROR、状态 `0`；文件存在但无 ERROR、状态 `1`；文件不存在、状态 `2`。重复运行只读取输入，不修改日志。本节只认识最小结构；参数引用、异常处理和更可靠的脚本会在 [Bash 脚本课程](../shell/index.md)继续练习。

## 4.10 实际日志分析

回到测试人员报告的“09:05 左右员工查询失败”：先确认报告对应的环境、时间与时区，再定位该环境的应用日志；按时间找 `ERROR`，用 `request_id` 关联同一请求的记录，最后把异常和堆栈一起看并记录结论。本例已知日志在 `training-app/logs/application.log`，因此直接从查看该文件开始：

```bash
tail -n 100 training-app/logs/application.log
grep -n '2026-09-01T09:05' training-app/logs/application.log
grep -n -A 2 'ERROR request_id=R101' training-app/logs/application.log
```

`-A 2` 在匹配的 ERROR 行后再显示 2 行，所以能够看到异常类型和调用位置；这比只搜 `ERROR` 多出必要上下文。样例能确认“R101 在 09:05:11 记录失败，并有 `IllegalStateException`”，**不能仅凭这几行断定数据库、配置或代码哪一处是根因**。真实项目还需核对环境、时区、相关请求及测试步骤，并按授权范围保留证据。

课堂练习：不照抄 4.10 节的三条命令，自行选择已学命令完成：① 查看日志最后 100 行；② 找到 ERROR；③ 用 `request_id=R101` 找相关记录；④ 查看 ERROR 后面的异常与调用位置；⑤ 将 ERROR 及上下文保存到 `output/R101-evidence.txt`，再用 `>>` 追加一条相关请求记录；⑥ 统计日志中含 ERROR 的行数；⑦ 读回证据文件，写一句“已确认事实”和一句“尚未确认原因”。样例中应统计到 1 行 ERROR，证据应包含 R101、异常类型和调用位置。只处理练习日志，不修改 `training-app/logs/application.log`；保存前确认目标文件是否有旧内容。更多编辑操作见[选读：Vim 常用操作](supplement/02_vi_vim_basic.md)。

[上一章：用户、用户组与文件权限](03_user_permission.md) · [下一章：进程管理与系统服务](05_process_system.md)
