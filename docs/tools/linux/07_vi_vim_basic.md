# 07 Vim 最小编辑操作

Vim 是常见终端编辑器之一。PG 只需先掌握小范围、安全修改；系统配置变更仍需项目作业手顺。

## 7.1 在练习副本上编辑

```bash
cd "$HOME/learndoc-linux-lab"
cp -- input/app.conf output/app.conf.edit
vim output/app.conf.edit
```

如果没有 Vim，可尝试环境已有的 `vi`；不要为课程自行安装软件包。
`cp` 先制作副本，`vim` 只打开 `output/app.conf.edit`；原始 `input/app.conf` 保持不变。编辑器打开后先看左下角状态，不要把终端命令直接敲进文件。

## 7.2 三种状态

- 普通模式：打开后的默认状态，用于移动、复制和删除
- 插入模式：按 `i` 进入，输入文字
- 命令行模式：普通模式按 `:`，用于保存、退出和替换

不知道当前状态时按 `Esc` 回到普通模式。

先只做一轮基本操作：`vim output/app.conf.edit` 打开 → 按 `i` 进入插入模式 → 把 `batch` 改为 `web` → 按 `Esc` 回普通模式 → 输入 `:wq` 并回车保存退出 → 用下一节的 `diff` 核对。若输入了意外文字，先按 `Esc`，不要反复敲命令；可以 `u` 撤销或确认放弃后 `:q!` 退出。下面的搜索和行编辑操作是在完成这轮操作后按需练习。

| 任务 | 操作 |
|---|---|
| 输入 | `i` |
| 保存 | `Esc` 后 `:w` 回车 |
| 保存退出 | `Esc` 后 `:wq` 回车 |
| 放弃未保存修改 | `Esc` 后 `:q!` 回车 |
| 删除当前行 | 普通模式 `dd` |
| 复制/粘贴行 | `yy`、`p` |
| 撤销/重做 | `u`、`Ctrl+r` |
| 查找 | `/keyword`，再按 `n` |
| 下一行新建并进入插入模式 | `o` |
| 到行尾并进入插入模式 | `A` |
| 到文件开头／末尾 | `gg`／`G` |
| 显示行号 | `:set number` 后回车 |

`o`、`A`、`gg`、`G` 在普通模式输入；`:set number` 在命令行模式输入。全文替换只在**新的练习副本**上演示：先运行 `cp -- input/app.conf output/app.conf.replace-demo`，再用 `vim output/app.conf.replace-demo` 打开；普通模式输入 `:%s/batch/web/g` 并回车，会把其中的 `batch` 替换为 `web`。执行前确认匹配范围，误操作可按 `u` 撤销，再用 `diff -u input/app.conf output/app.conf.replace-demo` 检查差异。真实配置中不要未经 Review 就全文替换。

## 7.3 修改并验证

把 `mode=batch` 修改为 `mode=web`，保存退出后执行：

```bash
grep '^mode=web$' output/app.conf.edit
diff -u input/app.conf output/app.conf.edit
```

`diff -u` 应只显示一行变化。实际项目修改配置时，应先备份、限定变更范围，并运行项目提供的语法检查；本课程不直接编辑 `/etc`。
`grep '^mode=web$'` 检查文件里是否存在完整的目标行；`^` 和 `$` 分别限定行首、行尾。`diff -u 旧文件 新文件` 显示差异，`-` 开头是旧内容，`+` 开头是新内容；有差异时它通常返回状态 1，不表示比较命令损坏。

## 7.4 常见问题

- 无法输入：按 `i` 进入插入模式。
- 无法退出：按 `Esc`，再输入 `:q`；有未保存修改时选择 `:wq` 或确认放弃后 `:q!`。
- 文件只读：不要用 sudo 强行打开；确认路径、权限和作业权限。
- 修改过多：退出不保存，重新复制练习副本。

## 7.5 本章任务

1. 在副本中修改一个配置值。
2. 使用 `diff -u` 保存差异到 `output/config.diff`。
3. 验证差异只包含预期字段。

```bash
diff -u input/app.conf output/app.conf.edit > output/config.diff || test "$?" -eq 1
grep 'mode=web' output/config.diff
```

[上一章：日志查看与调查](06_log_management.md) · [下一章：Web/批处理项目调查演习](08_web_batch_investigation.md)

本章自查：**必须掌握**进入编辑、保存、退出与放弃；**应当掌握**查找、撤销并用 `diff` 核对；**了解即可**全文替换，且仅在副本上练习。

课堂检查：① 打开后按什么进入可输入文字的模式？② 怎样回普通模式并保存退出？③ `diff -u` 中 `-`、`+` 各表示什么？④ 误改项目文件时能直接 `:wq` 吗？参考：`i`；`Esc`、`:wq`；旧行、新行；先停止并按手顺确认，练习中可放弃副本修改。
