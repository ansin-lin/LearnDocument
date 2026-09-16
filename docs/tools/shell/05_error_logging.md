# 05 错误处理、日志与清理

项目脚本不能只在成功时运行。调用方需要通过退出状态判断结果，调查人员需要从错误信息理解失败原因，临时文件也不能留在现场。

## 5.1 设计退出状态

为脚本预先定义状态，比在各处随意写数字更容易交接：

| 状态 | 含义 | 调用方处理示例 |
|---:|---|---|
| 0 | 成功并找到记录 | 继续后续处理 |
| 1 | 正常执行，但没有匹配记录 | 确认 ID 或调查范围 |
| 2 | 参数或格式错误 | 修正调用方式 |
| 3 | 输入文件错误或搜索失败 | 确认路径、权限和命令错误 |
| 4 | 无法创建或保存输出 | 确认输出目录和磁盘状态 |

非 0 不一定等于系统故障。例如 `grep` 返回 1 表示“没有匹配行”，返回大于 1 才表示实际错误。

```bash
if grep -n -F 'request_id=R001' input/app.log > output/result.txt; then
  printf 'matched\n'
else
  grep_status=$?
  if [[ $grep_status -eq 1 ]]; then
    printf 'no match\n' >&2
    exit 1
  fi
  printf 'grep failed: status=%s\n' "$grep_status" >&2
  exit 3
fi
```

## 5.2 `set` 选项不是完整错误处理

```bash
set -u
set -o pipefail
```

- `set -u`：读取未定义变量时失败，能较早发现拼写错误。
- `pipefail`：管道中任一命令失败时，整条管道返回非 0。

`set -e` 的行为会受条件、管道和命令替换等上下文影响。本课程的最终脚本不把它当作完整方案，而是对关键命令显式判断并输出业务上可理解的错误。

## 5.3 临时文件与 `trap`

先写临时文件，成功后再移动到正式文件，可以避免失败时留下半份结果。

```bash
tmp_file=""

cleanup() {
  if [[ -n "$tmp_file" && -f "$tmp_file" ]]; then
    rm -- "$tmp_file"
  fi
}

trap cleanup EXIT

tmp_file="$(mktemp 'output/.check-log.XXXXXX')" || {
  printf 'ERROR: cannot create temporary file\n' >&2
  exit 4
}
```

`mktemp` 创建名称不冲突的临时文件；`trap ... EXIT` 让脚本在正常结束或错误退出时执行清理。`rm -- "$tmp_file"` 中的 `--` 表示后续内容都是路径，即使文件名以 `-` 开头也不会被当作选项。

临时文件必须创建在已确认的输出目录内，不要把未经校验的输入直接拼进删除路径。

## 5.4 可调查的消息

```bash
write_message() {
  local level="$1"
  local message="$2"
  printf '%s level=%s message=%s\n' \
    "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$level" "$message" >&2
}
```

错误消息至少说明“发生了什么”和“涉及哪个对象”，但不得输出密码、令牌、个人信息或完整生产日志。stderr 可由作业平台或调用脚本单独收集。

## 5.5 可重复执行

可重复执行（idempotent，幂等）是指相同输入重复运行，不会不断追加重复数据或破坏原始文件。本课程脚本每次生成完整临时结果，再替换同名输出；不会修改输入日志。

## 5.6 本章任务

为上一章的 `validate_input.sh` 增加：

- `set -u` 和 `pipefail`
- 带时间、级别和消息的 stderr 输出
- 用 `mktemp` 创建临时文件
- 用 `trap` 在退出时清理临时文件

故意制造一次失败，确认退出状态正确、错误信息可理解，并且没有遗留 `.check-log.*` 文件。

[上一章：条件、循环与函数](04_control_functions.md) · [下一章：日志检查脚本](06_log_checker.md)
