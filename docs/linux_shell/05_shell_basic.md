# 第5章 Shell 基础

## 第一个脚本

```bash
#!/bin/bash
echo "Hello Linux!"
```

## 变量

```bash
name="Linux"
echo $name
```

## 条件与循环

```bash
if [ $1 -gt 10 ]; then
  echo "大于10"
else
  echo "小于等于10"
fi

for i in {1..5}; do
  echo $i
done
```

## 练习题

1. 写脚本输出 1~10。
2. 判断传入参数是否大于 100。
