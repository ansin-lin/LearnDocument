# 07 Vim 最小编辑操作

Vim 是常见终端编辑器之一。PG 只需先掌握小范围、安全修改；系统配置变更仍需项目作业手顺。

## 7.1 在练习副本上编辑

```bash
cd "$HOME/learndoc-linux-lab"
cp -- input/app.conf output/app.conf.edit
vim output/app.conf.edit
```

如果没有 Vim，可尝试环境已有的 `vi`；不要为课程自行安装软件包。

## 7.2 三种状态

- 普通模式：打开后的默认状态，用于移动、复制和删除
- 插入模式：按 `i` 进入，输入文字
- 命令行模式：普通模式按 `:`，用于保存、退出和替换

不知道当前状态时按 `Esc` 回到普通模式。

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

## 7.3 修改并验证

把 `mode=batch` 修改为 `mode=web`，保存退出后执行：

```bash
grep '^mode=web$' output/app.conf.edit
diff -u input/app.conf output/app.conf.edit
```

`diff -u` 应只显示一行变化。实际项目修改配置时，应先备份、限定变更范围，并运行项目提供的语法检查；本课程不直接编辑 `/etc`。

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
