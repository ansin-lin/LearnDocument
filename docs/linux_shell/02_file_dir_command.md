# 第2章 文件与目录操作命令

## 常用命令

| 命令 | 功能 |
|------|------|
| ls | 列出目录内容 |
| cd | 切换目录 |
| pwd | 当前路径 |
| mkdir/rmdir | 创建或删除目录 |
| cp/mv/rm | 复制、移动、删除文件 |
| touch | 创建空文件 |

## 实战演示

```bash
mkdir test_dir
cd test_dir
touch file1.txt file2.txt
ls -alh
cp file1.txt copy.txt
mv copy.txt ../
rm -rf test_dir
```

## 练习题

1. 创建 test 目录并生成 3 个文件。
2. 删除文件时加入 -i 参数以确认删除。
