# Shell 脚本基础（教学完整版 · 详细版）

> 本章定位：**从 0 到能写脚本**  
> 适用：教学 / 自学 / 面试 / 实战  
> 默认解释器：**bash**

---

## 1. 什么是 Shell 与 Shell 脚本

### 1.1 Shell 是什么

Shell 是 **命令解释器**，作用是：

- 接收用户命令
- 解析并交给 Linux 内核执行
- 输出执行结果

常见 Shell：

- sh
- **bash（默认）**
- zsh
- fish

---

### 1.2 什么是 Shell 脚本

Shell 脚本就是：
> **把一系列 Linux 命令写进文件，由 Shell 自动执行**

常见用途：

- 自动化运维
- 批处理任务
- 定时任务
- 部署脚本

---

## 2. 第一个 Shell 脚本（必须会）

### 2.1 基本结构

```bash
#!/bin/bash
echo "Hello Shell"
```

### 2.2 说明

- `#!/bin/bash`：指定解释器（shebang）
- 第一行必须写
- 决定脚本用哪个 Shell 执行

---

### 2.3 执行方式

#### 方式一（推荐）

```bash
chmod +x hello.sh
./hello.sh
```

#### 方式二

```bash
bash hello.sh
```

---

## 3. 变量（重点）

### 3.1 定义变量

```bash
name="Tom"
```

⚠️ 等号两边 **不能有空格**

---

### 3.2 使用变量

```bash
echo $name
echo "${name}"
```

---

### 3.3 只读变量

```bash
readonly age=18
```

---

### 3.4 删除变量

```bash
unset name
```

---

## 4. 脚本参数（位置参数）

### 4.1 基本参数

| 参数 | 含义 |
| --- | --- |
| $0 | 脚本名 |
| $1 | 第一个参数 |
| $2 | 第二个参数 |
| $# | 参数个数 |
| $@ | 所有参数 |

---

### 4.2 示例

```bash
#!/bin/bash
echo "脚本名：$0"
echo "第一个参数：$1"
echo "参数个数：$#"
```

执行：

```bash
./test.sh hello world
```

---

## 5. 输入（read）

### 5.1 基本语法

```bash
read name
echo "Hello $name"
```

---

### 5.2 带提示

```bash
read -p "请输入用户名：" user
```

---

## 6. 条件判断（if）

### 6.1 基本语法

```bash
if [ 条件 ]; then
  命令
fi
```

⚠️ 中括号 **必须有空格**

---

### 6.2 数值判断

| 运算符 | 含义 |
| --- | --- |
| -eq | 等于 |
| -ne | 不等于 |
| -gt | 大于 |
| -lt | 小于 |
| -ge | 大于等于 |
| -le | 小于等于 |

示例：

```bash
if [ $age -ge 18 ]; then
  echo "成年"
fi
```

---

### 6.3 字符串判断

```bash
if [ "$name" = "Tom" ]; then
  echo "Hello Tom"
fi
```

---

### 6.4 文件判断（非常常用）

| 判断 | 含义 |
| --- | --- |
| -f | 是否普通文件 |
| -d | 是否目录 |
| -e | 是否存在 |

示例：

```bash
if [ -f app.log ]; then
  echo "日志存在"
fi
```

---

## 7. case 分支判断

### 7.1 基本语法

```bash
case $var in
  1)
    echo "one"
    ;;
  2)
    echo "two"
    ;;
  *)
    echo "other"
    ;;
esac
```

---

### 7.2 使用场景

- 菜单
- 参数分发

---

## 8. 循环结构（重点）

### 8.1 for 循环

```bash
for i in 1 2 3
do
  echo $i
done
```

---

### 8.2 for + 范围

```bash
for i in {1..5}
do
  echo $i
done
```

---

### 8.3 while 循环

```bash
i=1
while [ $i -le 3 ]
do
  echo $i
  i=$((i+1))
done
```

---

## 9. 函数（基础）

### 9.1 定义函数

```bash
hello() {
  echo "Hello $1"
}
```

---

### 9.2 调用函数

```bash
hello Tom
```

---

## 10. 常用内置变量

| 变量 | 含义 |
| --- | --- |
| $? | 上一条命令返回值 |
| $$ | 当前脚本 PID |
| $! | 上一个后台进程 PID |

---

## 11. 重定向与管道（基础）

### 11.1 输出重定向

```bash
echo "test" > a.txt
echo "append" >> a.txt
```

---

### 11.2 错误重定向

```bash
command 2> error.log
```

---

### 11.3 管道

```bash
grep ERROR app.log | wc -l
```

---

## 12. 实战示例（教学重点）

### 示例：统计日志错误数

```bash
#!/bin/bash

if [ ! -f app.log ]; then
  echo "日志不存在"
  exit 1
fi

count=$(grep ERROR app.log | wc -l)
echo "ERROR 数量：$count"
```

---

## 13. 常见错误总结

- 变量等号有空格
- if 中括号无空格
- 忘记执行权限
- Windows 换行符问题

---

## 14. 本章小结

你现在应该已经能够：

- 理解 Shell 脚本结构
- 使用变量、判断、循环、函数
- 编写简单但实用的自动化脚本

---

## 15. 教学建议

> **Shell 不追求复杂，追求稳定和可读**

推荐原则：

- 50 行以内
- 清晰注释
- 明确退出码
