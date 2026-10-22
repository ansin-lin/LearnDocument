# 第 1 章：Shell 基础与脚本执行

## 本章学习目标与前置知识

你将分清终端、Shell、Bash 和脚本，保存并运行一份完整的 `.sh` 文件，检查语法与退出状态。先完成 [Linux 的目录、文件和权限基础](../linux/index.md)；本章只在个人练习目录操作。

## 为什么日本项目需要

日次処理（每天运行的处理）通常由ジョブ（作业）调用脚本。PG 接到シェル改修（Shell 修改）任务时，必须知道脚本由谁解释、如何执行、成功与否怎样传给调用方，不必先学习服务器管理。

## 概念与最小示例

终端是输入窗口；Shell 负责解释命令；Bash 是 Shell 的一种；Shell Script 是保存了命令与 Bash 语法的文本。`.sh` 后缀只是约定，并不决定解释器。`#!/usr/bin/env bash` 称为 Shebang：直接执行文件时指定 Bash；`bash 文件名` 则由命令显式指定 Bash。

下面是**完整可执行脚本**。在个人练习目录创建 `hello_batch.sh`，保存为 LF 换行、UTF-8 文本：

```bash
#!/usr/bin/env bash
# 输出准备完成的提示；格式串中的 \n 让结果换行。
printf 'sales import: preparation complete\n'
```

Shebang 指定解释器；`printf` 输出文字，格式串中的 `\n` 表示换行。此时只是演示“作业准备完成”，尚未导入数据。执行顺序是“Bash 读取脚本 → 执行 `printf` → 返回最后一条命令的状态”。

在保存文件的目录执行：

```bash
# 只检查脚本语法，不执行其中的命令。
bash -n hello_batch.sh
# 显式调用 Bash 运行脚本，不要求文件有执行权限。
bash hello_batch.sh
# 立即读取上一条命令的退出状态，0 表示本次成功。
printf 'status=%s\n' "$?"
# 给当前用户增加这个练习脚本的执行权限。
chmod u+x hello_batch.sh
# 从当前目录直接执行，使用脚本首行指定的 Bash。
./hello_batch.sh
```

`bash -n` 只检查语法、不运行；`bash 文件` 不要求文件自身有执行权限；`chmod u+x` 给所有者增加执行权限；`./` 指当前目录，直接执行需要权限和正确 Shebang。**示例输出**：两次实际运行各显示 `sales import: preparation complete`，紧跟第一次运行的 `status=0`。`$?` 是紧前一条命令的状态，0 通常表示成功，非 0 表示未按预期完成；另一条命令执行后它会改变。

调试时先用 `bash -n`，再对不含凭据的练习脚本用 `bash -x hello_batch.sh` 查看执行轨迹；`-x` 会暴露展开后的参数，真实项目日志不可随意分享。

## 常见错误与项目关联

`Permission denied`：直接执行缺少执行权限或目录访问权限，先看 `ls -l`；`bad interpreter`：Shebang 路径或 CRLF 换行有问题；`command not found`：拼写或 PATH 环境不同。不要用 `sudo` 绕过项目权限。今后调度系统调用销售导入脚本时，也要确定是 `bash script.sh` 还是直接执行，并读取它的结束状态。

## 本章练习与完成标准

把输出改为 `sales import: ready`，保存后分别运行语法检查、两种执行方式，记录输出和紧随运行取得的状态。能解释终端、Bash、脚本、Shebang 和两种运行方式的区别，并展示修改后的输出，即完成本章。

[返回课程入口](index.md) · [下一章：变量与参数](02_variables_arguments.md)
