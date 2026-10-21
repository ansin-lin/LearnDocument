# 02 变量、引用与参数

## 2.1 变量必须安全引用

```bash
log_file="$HOME/learndoc-linux-lab/input/app.log"
printf 'log_file=%s\n' "$log_file"
```

赋值等号两侧不能有空格。使用变量时优先写 `"$log_file"`；未加引号会触发单词分割和通配符展开，带空格或 `*` 的路径可能被拆成多个参数。
变量是在脚本里暂存一个值的名字。这里 `log_file=...` 保存日志路径，`$log_file` 读取它；`$HOME` 先由 Bash 替换成当前用户主目录。`printf` 不读取日志，只把路径打印出来，便于确认变量是否指向预期文件。

### 普通变量、环境变量和引用

```bash
name='learner'
export APP_ENV='test'
printf 'name=%s env=%s\n' "$name" "$APP_ENV"
```

赋值在当前 Bash 中完成；`export` 让随后启动的子进程也收到 `APP_ENV`，普通变量 `name` 默认不会自动传给子进程。`APP_ENV` 是练习用环境名，不是秘密。双引号 `"$name"` 会展开变量且保留一个参数；单引号 `'$name'` 会原样输出 `$name`；不加引号的 `$name` 可能发生空白分词和通配符展开，路径和用户输入尤其危险。可以分别运行 `printf '%s\n' "$name"`、`printf '%s\n' '$name'` 对照输出。

用含空格的值观察区别：

```bash
name='hello world'
printf '<%s>\n' "$name"
printf '<%s>\n' '$name'
printf '<%s>\n' $name
```

预期依次是 `<hello world>`、`<$name>`，最后一条输出两行 `<hello>` 和 `<world>`：未加引号时被拆为两个参数。文件路径若同样含空格，命令就可能找错文件，因此实际调用要写 `"$log_file"`。

## 2.2 命令替换

```bash
line_count="$(wc -l < "$log_file")"
printf 'line_count=%s\n' "$line_count"
```

`$(...)` 执行其中命令并取得标准输出。命令失败状态不会自动被所有外层写法正确处理，关键命令仍需显式检查。
`wc -l < "$log_file"` 统计文件行数；`<` 把文件作为标准输入，因此结果只有数字、不带文件名。`line_count` 保存这个数字，再由 `printf` 打印。使用前先用 `test -f "$log_file"` 确认文件存在。

## 2.3 位置参数

创建 `show_args.sh`：

```bash
#!/usr/bin/env bash

printf 'script=%s\n' "$0"
printf 'count=%s\n' "$#"
printf 'first=%s\n' "${1:-}"
printf 'second=%s\n' "${2:-}"
```

- `$0`：脚本调用名称
- `$#`：参数数量
- `$1`、`$2`：第一个、第二个参数
- `${1:-}`：第一个参数缺失或为空时给出空串，避免直接引用缺失参数
- `"$@"`：代表全部位置参数，并保留每个参数的边界；第 04 章学完循环后再用于逐个处理

本章先只查看固定位置的前两个参数，不使用尚未讲解的循环。

```bash
bash show_args.sh file1 file2
bash show_args.sh 'input/app.log' 'request id=R001'
```

先看第一个调用：`$#` 应为 2，`first=file1`、`second=file2`。第二个调用仍应为 2 个参数，第二个值完整显示 `request id=R001`；调用时的单引号让它保持为一个参数，脚本内的双引号保留这个边界。Bash 在执行命令前展开变量，所以要在**传入时**和**使用时**都保持正确引用。

## 2.4 缺失参数的默认值

```bash
log_file="${1:-}"
request_id="${2:-}"
printf 'log_file=<%s> request_id=<%s>\n' "$log_file" "$request_id"
```

`${1:-}` 在第一个参数缺失或为空时使用空字符串；`${2:-}` 对第二参数同理。上面是**观察默认值的片段**，可在 Bash 中给变量赋值后打印；缺参数时会显示空的尖括号。它还没有拒绝错误调用：第 03 章讲错误输出，第 04 章学 `if` 后再把“缺参数则显示 Usage 并退出”的判断加入脚本。

## 2.5 本章任务

运行 `show_args.sh` 的两种双参数调用和一次无参数调用，核对 `count`、`first`、`second`；再把第二参数换成含空格的路径，验证仍显示为一个值。第 04 章学完 `if` 和 `for` 后，再升级为校验参数并遍历全部参数。

[上一章：脚本结构与执行方式](01_shell_basic.md) · [下一章：输入输出、重定向与管道](03_io_pipelines.md)

本章自查：**必须掌握**变量赋值、双引号、`$1`、`$#`、`"$@"`；**应当掌握**环境变量与命令替换；**了解即可**变量在子进程间的传递细节。

课堂检查：① `$1` 与 `$#` 分别是什么？② `"$name"`、`'$name'` 的输出为何不同？③ 含空格的路径为何要用双引号？④ 何时会用 `"$@"`？参考：首个参数／参数个数；前者展开、后者原样；保留一个路径参数；需要逐个处理全部参数时，第 04 章实践。
