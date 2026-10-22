# Linux 命令速查表

本页供学完八章后回看写法，不代替正文讲解。除环境查询外，示例默认在 Bash 的 `~/learndoc-linux-lab` 中执行，并使用第 2、4 章建立的样例；实际项目路径、服务名和权限以项目手顺为准。先确认 `pwd`，覆盖、删除或改权限前再核对目标。

## 1. 环境与路径

```bash
whoami
pwd
cd "$HOME/learndoc-linux-lab"
cd ..
ls -la
ls -ld training-app/logs
```

`pwd` 看当前位置；`cd` 切换目录；`ls -la` 连隐藏项目一起显示详细信息，`ls -ld` 看目录**自身**。

## 2. 文件创建、复制、移动与删除

```bash
mkdir -p output/backup
touch output/temp.txt
cp -i -- input/app.conf output/app.conf.bak
mv -i -- output/temp.txt output/backup/temp.txt
rm -i -- output/backup/temp.txt
rmdir output/backup
```

`cp` 保留源文件，`mv` 移动或改名；`rm` 删除指定文件，`rmdir` 只删空目录。`-i` 在覆盖或删除前询问。目录复制用 `cp -r`；递归删除只按项目手顺处理，不在这里提供命令。

## 3. 文件内容查看

```bash
cat input/app.conf
head -n 2 input/app.log
tail -n 10 input/app.log
less input/app.log
```

`cat` 看短文件，`head` 看开头，`tail` 看末尾，`less` 分页查看；`less` 按 `q` 退出。持续观察新增日志用 `tail -f 文件`，按 `Ctrl+C` 停止。

## 4. grep：在文件内容中搜索

```bash
grep -n 'ERROR' input/app.log
grep -i 'error' input/app.log
grep -E 'ERROR|WARN' input/app.log
grep -v 'INFO' input/app.log
grep -F 'request_id=R001' input/app.log
grep -rn 'ERROR' training-app/logs
grep -C 2 'ERROR' training-app/logs/application.log
```

`-n` 加行号，`-i` 忽略大小写，`-E` 支持二选一等扩展正则，`-v` 排除，`-F` 按普通字符串，`-r` 递归，`-C 2` 看前后各两行。只数匹配**行数**用 `grep -c`；只列含匹配内容的文件用 `grep -rl`。引号内的 `|` 属于搜索条件，引号外的 `|` 才是 Shell 管道。

## 5. find：按文件名、类型和时间查找

```bash
find training-app -type f -name '*.log'
find training-app -type d -name 'backup'
find training-app -type f -iname '*.LOG'
find training-app -type f -mtime -1
find training-app -maxdepth 2 -type f
```

先写**限定的查找起点**，再写条件；`-type f/d` 区分文件和目录，`-iname` 忽略大小写，`-mtime -1` 指过去 24 小时内修改，`-maxdepth 2` 限制深度。`find` 找路径，`grep` 找文件内容。

## 6. 管道与重定向

```bash
grep 'ERROR' input/app.log | wc -l
wc -l < input/app.log
grep 'ERROR' input/app.log > output/errors.txt
grep 'WARN' input/app.log >> output/errors.txt
grep 'ERROR' input/missing.log 2> output/error.txt
grep 'ERROR' input/missing.log > output/all.txt 2>&1
```

`|` 把正常输出交给下一命令；`<` 从文件提供输入；`>` 覆盖，`>>` 追加；`2>` 保存错误，`2>&1` 把错误送到当前标准输出位置。`missing.log` 是故意不存在的练习路径，后两条预期失败。

多行输入可用 Here Document；`<<` 提供输入，`>` 才负责保存文件：

```bash
cat > output/report.txt <<'EOF'
调查对象：员工查询 API
调查状态：处理中
EOF
```

## 7. 文本统计、处理与差异

```bash
wc -l input/app.log
cut -d, -f1 output/levels.csv | sort | uniq -c
grep -n 'ERROR' input/app.log | tee output/errors.txt
grep -n 'WARN' input/app.log | tee -a output/errors.txt
diff -u input/app.conf output/app.conf.edit
```

`uniq` 只处理相邻重复行，先 `sort` 才能按相同值汇总；`tee` 同时显示并保存，`-a` 才追加；`diff -u` 显示文件差异，有差异时状态 `1` 是正常结果。`levels.csv` 和编辑副本由第 4 章建立。

## 8. vi/vim 最小操作

```bash
vi output/app.conf.edit
```

按 `i` 插入，按 `Esc` 回普通模式；`:wq` 保存退出，`:q!` 放弃修改。先复制原文件，只编辑副本。

## 9. 权限与用户

```bash
id
groups
ls -l -- input/app.conf
ls -ld -- input
chmod 600 output/permission-demo.conf
```

读写失败先核对实际用户、目标文件和父目录；`chmod` 只对自己的练习副本操作，不用 `777` 作为通用修复。

## 10. 日志和 BUG 调查组合

```bash
find training-app -type f -name '*app*.log'
tail -n 100 training-app/logs/application.log
grep -n 'request_id=R101' training-app/logs/application.log
grep -n -C 2 'ERROR' training-app/logs/application.log
grep 'ERROR' training-app/logs/application.log | wc -l
grep -n -C 2 'ERROR' training-app/logs/application.log > output/R101-evidence.txt
```

先核对环境、时间与日志路径，再看请求关联和异常上下文；最后读回保存的证据，区分事实与原因候选。

## 11. 进程与网络：按示例识读

```bash
ps aux
pgrep -af java
top
free -h
df -h "$HOME"
ss -lnt
curl -i --connect-timeout 3 --max-time 10 http://127.0.0.1:8080/health
```

进程存在、端口监听与 HTTP 成功是不同结果。`curl` 的地址仅是假设已有本机服务时的示意；没有服务时不要求成功。

## 12. 运维与部署：了解即可

```bash
systemctl status training-app.service --no-pager
journalctl -u training-app.service -n 100 --no-pager
crontab -l
```

只有目标主机确实使用 systemd、存在对应服务并允许读取时才运行前两条。`crontab -l` 只查看当前用户的定时任务。软件安装、服务启停、正式部署与回退遵循项目手顺，不是本速查表的实操要求。

[返回 Linux 课程入口](index.md)
