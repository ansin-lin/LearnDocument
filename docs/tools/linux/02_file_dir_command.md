# 第 2 章：Linux 目录结构与基础文件操作

本章从第 1 章打开的 Linux 终端继续。完成后，你应能在自己的练习目录中切换位置，创建、查看、复制、移动、查找和删除文件，并把练习文件打包。所有写入和删除操作都只在自己的用户目录中进行。第 1 章的在线环境可用于尝试命令；如果环境重置，练习文件需要重新建立。

学习时要能独立使用 `pwd`、`cd`、`ls`、`mkdir`、`touch`、`cp`、`mv`、`rm`、`rmdir`，并根据文件名、类型或修改时间写出 `find` 命令。`cat`、`head`、`tail` 用于日常查看；`less` 可按示例使用。`which`、`tar`、`gzip`、`man` 先认识用途，不要求背下全部选项。

## 2.1 目录结构

Linux 的目录从 **`/`（根目录）** 开始，其他目录都位于它的下方。常见目录先认用途，不在本章修改系统文件：

| 路径 | 先怎样认识 |
|---|---|
| `/` | 整棵目录树的起点 |
| `/home` | 普通用户的家目录通常位于这里，例如 `/home/student` |
| `/etc` | 系统及部分软件的配置文件常见位置 |
| `/var` | 日志等经常变化的数据常见位置 |
| `/tmp` | 临时文件位置，内容可能被清理 |
| `/opt` | 部分项目或第三方软件的安装位置 |

**`/` 和 `/root` 不一样**：`/` 是目录树的起点；`/root` 通常是管理员用户 `root` 的家目录。普通学员不要为了练习进入或修改 `/root`、`/etc` 等目录；项目中的实际文件位置以项目说明为准。

## 2.2 路径概念

路径用 `/` 分隔目录名。**绝对路径**从 `/` 开始，不依赖当前位置；**相对路径**从当前目录开始。例如当前位置为 `/home/student/learndoc-linux-lab` 时，`training-app/config` 与 `/home/student/learndoc-linux-lab/training-app/config` 指向同一处。

| 写法 | 含义 |
|---|---|
| `.` | 当前目录 |
| `..` | 上一级目录 |
| `~` | 当前用户的家目录；用户为 `student` 时通常是 `/home/student` |

示例中的 `student` 只是用户名，不要把 `/home/student` 当成每个人都相同的路径。自己的家目录以第 1 章 `pwd` 等命令看到的实际环境为准。

## 2.3 目录切换

先在自己的家目录尝试以下命令，每条输入后按回车：

```bash
cd ~
pwd
ls
ls -la
cd ..
pwd
cd ~
```

`cd 路径` 切换当前目录；不写路径时 `cd` 通常也会回到家目录。`pwd` 显示当前目录的完整路径，用它确认 `cd` 是否成功。`ls` 列出当前目录中的名称；`ls -la` 中，`-l` 显示详细信息，`-a` 还显示以 `.` 开头的隐藏项目，包括表示当前目录和上级目录的 `.`、`..`。`cd ..` 后，第二次 `pwd` 应比第一次少一级；最后的 `cd ~` 回到家目录。输出的用户名、文件名和时间会因环境而异。

命令一般写作 `命令 [选项] [参数]`，方括号表示可省略，不需要输入。若 `cd` 提示目录不存在，先用 `pwd` 确认当前位置，再检查路径拼写。

## 2.4 创建文件及目录

在自己的家目录下建立课程共用的 `learndoc-linux-lab`，并在其中建立模拟项目 `training-app`。后续章节还会使用同一练习目录下的 `input`、`output`。

```bash
mkdir -p "$HOME/learndoc-linux-lab"
cd "$HOME/learndoc-linux-lab"
mkdir -p training-app/config training-app/logs training-app/backup input output
touch training-app/backup/practice.txt
pwd
ls -la training-app
```

`$HOME` 表示当前用户的家目录，双引号保证路径中有空格时仍作为一个参数。`mkdir 路径` 创建目录；`-p` 允许同时创建缺少的上级目录，目录已存在时不报错。`touch 路径` 创建空文件；若文件已存在，则会更新它的修改时间，因此不要对项目真实文件随意练习。这里的 `practice.txt` 是临时练习文件，2.6 节会删除。

用户为 `student` 时，模拟项目大致如下；其他用户的前缀会不同：

```text
/home/student/learndoc-linux-lab/
└── training-app/
    ├── config/
    ├── logs/
    └── backup/
        └── practice.txt
```

`pwd` 应以 `/learndoc-linux-lab` 结尾；`ls -la training-app` 应能看到三个目录。以下示例都从这个练习目录继续。

## 2.5 文件复制和移动

先在临时文件上练习复制与改名，不碰真实配置：

```bash
cp -i -- training-app/backup/practice.txt training-app/backup/practice-copy.txt
ls training-app/backup
mv -i -- training-app/backup/practice-copy.txt training-app/backup/practice-renamed.txt
ls training-app/backup
```

`cp 源路径 目标路径` **复制**文件，原文件仍在；`mv 源路径 目标路径` **移动或改名**文件，原路径通常不再存在。这里先复制 `practice.txt`，再给副本改名。`-i` 表示目标已存在时先询问是否覆盖；`--` 表示后面都是路径，不再把以 `-` 开头的文件名当选项。第一次运行通常不会询问；重复练习时若出现覆盖提示，先核对目标，再决定是否输入 `y`。最后应看到 `practice.txt` 和 `practice-renamed.txt`，看不到 `practice-copy.txt`。

复制**目录**需要 `-r` 递归复制其中的文件与子目录。下面只复制自己的模拟配置目录；目标 `backup/` 已存在：

```bash
cp -ri -- training-app/config training-app/backup/
ls -ld training-app/config training-app/backup/config
```

`-r` 让 `cp` 进入目录逐项复制，`-i` 在目标已有同名项目时询问。执行后原目录 `training-app/config` 仍在，新目录是 `training-app/backup/config`；此时两者都是空目录，2.7 节才会建立配置样例。若目标写成一个**尚不存在**的路径，新路径本身会成为复制出的目录名，因此复制前要先确认目标是否已存在。此副本只供观察目录结构，不作为项目备份。

## 2.6 文件删除

只删除前面创建的临时文件，并额外练习删除空目录：

```bash
mkdir training-app/backup/empty-dir
ls -la training-app/backup
rm -i -- training-app/backup/practice.txt training-app/backup/practice-renamed.txt
rmdir training-app/backup/empty-dir
ls -la training-app/backup
```

`rm 路径` 删除文件；`-i` 会逐个询问，确认是这两个临时文件后再输入 `y`。`rmdir 路径` 只能删除**空目录**，目录里还有文件时会拒绝。最后一次 `ls` 应看不到临时文件和 `empty-dir`。`rm` 删除的文件通常不能直接恢复；若误删练习文件，可按 2.4 或 2.5 节重新创建。`rm -r` 会递归删除目录，`rm -f` 会强制处理；本章只识别风险，**不要在共享或项目环境照着练习递归删除**。

## 2.7 查看文件内容

`touch` 创建的是空文件。为了练习查看内容，并给后续日志、权限和 Shell 章节准备相同的样例，在 `input` 中写入一份四行日志和一行配置。下面代码块应从 `~/learndoc-linux-lab` 执行；最后单独一行的 `EOF` 也是需要输入的结束标记。重复执行会覆盖这两份练习数据。

```bash
cat > input/app.log <<'EOF'
2026-09-01T09:00:00 INFO  request_id=R001 start
2026-09-01T09:00:01 ERROR request_id=R001 employee not found
2026-09-01T09:01:00 INFO  request_id=R002 completed
2026-09-01T09:02:00 WARN  request_id=R003 slow response
EOF
printf 'mode=batch\n' > input/app.conf
cp -- input/app.log training-app/logs/app.log
cp -- input/app.conf training-app/config/app.conf
```

上面的 `EOF` 只用来标记多行样例内容的结束；`printf` 中的 `\n` 是换行。`>` 会创建或覆盖自己的练习文件，`cp` 再把样例复制进模拟项目。输入与重定向的详细用法留到 [Shell 课程](../shell/04_file_text_processing.md)。

到这里，`input` 中有 `app.log`、`app.conf` 两份原始样例，`training-app/logs` 与 `training-app/config` 中各有一份对应副本；`backup` 中还没有必须保留的文件。

现在查看内容：

```bash
cat input/app.conf
head -n 2 input/app.log
tail -n 2 input/app.log
less input/app.log
```

`cat 文件` 把短文件全部显示出来，第一条应显示 `mode=batch`。`head -n 2` 显示前 2 行，应包含 R001 的 INFO 和 ERROR；`tail -n 2` 显示最后 2 行，应包含 R002 和 R003。`-n` 后的数字可换成需要的行数。`less 文件` 适合长文件分页查看，按 `q` 退出；部分精简的在线环境可能没有 `less`，这时先用 `cat`、`head`、`tail` 完成本节。查看命令不会修改原文件。

日志继续增长时还可用 `tail -f 文件` 持续查看新增内容，按 `Ctrl+C` 停止；它不会修改日志。第 4 章会在故障调查中使用这一写法。

## 2.8 文件查找

`find 查找起点 查找条件` 按文件系统中的**名称、类型或修改时间**查找文件和目录，默认也进入起点下面的子目录；它不搜索文件**内容**（内容搜索见第 4 章 `grep`）。先从自己已创建的 `training-app` 开始：

```bash
find training-app -type f -name '*.log'
find training-app -type f -name 'app.conf'
find training-app -type d -name 'logs'
find training-app -type f -iname '*.LOG'
find training-app -type f -mtime -1
find training-app -maxdepth 2 -type f
```

`training-app` 是查找起点，决定搜索范围；`-type f` 只要普通文件，`-type d` 只要目录。`-name` 区分大小写，`-iname` 不区分；`*.log` 的 `*` 匹配任意长度的文件名部分，引号防止 Bash 在运行 `find` 前展开它。第一条应找到 `training-app/logs/app.log`，第二条应找到 `training-app/config/app.conf`，第三条应找到 `logs` 目录，第四条即使用大写 `.LOG` 也能找到小写 `.log` 文件。

`-mtime -1` 按**过去 24 小时内**的修改时间筛选，不等于“今天”；因练习文件的建立时间不同，结果可能为空。`-maxdepth 2` 将深度限制为从起点算起最多两层：起点是第 0 层，`training-app/logs/app.log` 位于第 2 层。此选项在本课程参考的 GNU `find` 中可用；在线环境若不支持，以 `find --help` 为准。先在自己的练习目录限定起点，不要随意从 `/` 搜索整个系统。

`which` 与 `find` 目的不同：`which cat` 查找当前环境中 `cat` **命令程序**的路径，例如 `/usr/bin/cat`，不能寻找项目日志。执行后可用 `find training-app -type f -name '*.log'` 找到日志路径，再用 `tail -n 10 找到的路径` 查看内容；路径应替换为自己实际找到的结果。

## 2.9 打包与解压

`tar` 把目录和文件放进一个归档；`gzip` 负责压缩。常见的 `.tar.gz` 是经过 gzip 压缩的 tar 归档，不表示它已经是可靠的备份。下面只处理自己的模拟项目：

```bash
tar -czf training-app/backup/config.tar.gz -C training-app config
tar -tzf training-app/backup/config.tar.gz
mkdir -p training-app/backup/unpacked
tar -xzf training-app/backup/config.tar.gz -C training-app/backup/unpacked
ls training-app/backup/unpacked/config
```

`-c` 创建归档，`-t` 列出归档成员，`-x` 解压；每次只选一个。`-z` 表示使用 gzip，`-f` 后面紧跟归档文件路径；`-C` 指定操作目录。先用 `-t` 确认成员只有 `config/` 下的练习文件，再解到新建的 `unpacked` 目录，避免覆盖原文件。最后应看到 `app.conf`。不要解压来源不明的归档到项目目录。

单独压缩一个日志文件时，可以让 `gzip` 把压缩结果写到备份目录，同时保留原日志：

```bash
gzip -c training-app/logs/app.log > training-app/backup/app.log.gz
gzip -dc training-app/backup/app.log.gz
```

`gzip -c` 把压缩结果送到屏幕输出，由 `>` 保存为 `.gz` 文件；原日志仍在。`gzip -dc` 解压并把文字显示在终端，不覆盖原文件，应重新看到四行日志。再次执行第一条会覆盖同名练习压缩包。

## 2.10 帮助与命令选项

忘记选项时先看命令自带的帮助；某些环境还提供手册：

```bash
ls --help
man ls
```

`--help` 是传给 `ls` 的选项，通常会列出可用写法，不是一个独立命令。`man ls` 打开 `ls` 的手册，按 `q` 退出；精简的在线环境可能没有 `man`，此时使用命令的 `--help`，或查阅所用发行版的文档。不同发行版上的可用选项可能不完全相同，遇到本章示例不支持的选项时，以当前环境的帮助信息为准。

课堂练习：从 `~/learndoc-linux-lab` 出发，不先看参考命令，自行完成：① 在 `output` 下建立 `backup`；② 在 `output` 创建空文件 `temp.txt`；③ 复制到 `output/backup` 并把副本改名为 `temp-copy.txt`；④ 用 `find` 找出 `training-app` 下的 `.log` 文件并查看其中一个文件的最后 10 行；⑤ 只删除这两个临时文件及已清空的 `output/backup`。每步用 `pwd`、`ls` 或 `find` 核对结果。`rm` 只用于这两个明确的练习文件，保留其他样例供后续章节使用。

<details><summary>参考命令（完成后展开）</summary>

```bash
mkdir -p output/backup
touch output/temp.txt
cp -i -- output/temp.txt output/backup/temp.txt
mv -i -- output/backup/temp.txt output/backup/temp-copy.txt
find training-app -type f -name '*.log'
tail -n 10 training-app/logs/app.log
rm -i -- output/temp.txt output/backup/temp-copy.txt
rmdir output/backup
```

应只删除本练习新建的两个文件；`input`、`training-app` 和 `output` 本身仍在。

</details>

[上一章：Linux 基础认识与环境准备](01_linux_intro.md) · [下一章：用户、用户组与文件权限](03_user_permission.md)
