# Linux 常用命令速查表（Cheat Sheet）

> 适用对象：Linux 初学者 / 教学 / 面试 / 日常运维  
> 目标：**一页在手，80% 场景不慌**

---

## 一、目录与文件操作（最常用）

| 命令 | 作用 | 示例 |
| --- | --- | --- |
| pwd | 查看当前目录 | `pwd` |
| ls | 列出目录内容 | `ls -la` |
| cd | 切换目录 | `cd /var/log` |
| mkdir | 创建目录 | `mkdir -p a/b/c` |
| rmdir | 删除空目录 | `rmdir test` |
| touch | 创建文件 | `touch a.txt` |
| cp | 复制文件/目录 | `cp -r a b` |
| mv | 移动/重命名 | `mv a.txt b.txt` |
| rm | 删除 | `rm -rf dir` |

⚠️ `rm -rf` 极其危险

---

## 二、文件内容查看

| 命令 | 作用 | 示例 |
| --- | --- | --- |
| cat | 查看文件内容 | `cat file` |
| less | 分页查看（推荐） | `less file` |
| head | 看前几行 | `head -n 10 file` |
| tail | 看后几行 | `tail -n 20 file` |
| tail -f | 实时查看 | `tail -f app.log` |

---

## 三、查找与搜索

### 1. 查找文件（find）

```bash
find /var/log -name "*.log"
```

### 2. 查找内容（grep）

```bash
grep ERROR app.log
grep -r ERROR /var/log
```

### 3. 常用 grep 选项

| 选项 | 作用 |
| --- | --- |
| -i | 忽略大小写 |
| -n | 显示行号 |
| -v | 排除匹配 |
| -r | 递归搜索 |

---

## 四、统计与管道

| 命令 | 作用 | 示例 |
| --- | --- | --- |
| wc -l | 统计行数 | `wc -l file` |
| wc -w | 统计单词数 | `wc -w file` |
| wc -c | 统计字节数 | `wc -c file` |
| \| | 管道 | `grep ERROR log | wc -l` |

---

## 五、用户与权限

| 命令 | 作用 | 示例 |
| --- | --- | --- |
| whoami | 当前用户 | `whoami` |
| id | 用户/组信息 | `id user1` |
| useradd | 创建用户 | `sudo useradd user1` |
| passwd | 设置密码 | `sudo passwd user1` |
| groupadd | 创建组 | `sudo groupadd dev` |
| usermod | 修改用户 | `sudo usermod -aG dev user1` |

---

## 六、权限管理

### 1. chmod（权限）

```bash
chmod 755 file
chmod u+x script.sh
```

常见权限：

- 755：程序 / 目录
- 644：普通文件
- 700：私有目录

---

### 2. chown（归属）

```bash
sudo chown user:group file
sudo chown -R user:group dir
```

---

## 七、进程与系统

| 命令 | 作用 | 示例 |
| --- | --- | --- |
| ps -ef | 查看进程 | `ps -ef | grep java` |
| top | 实时进程 | `top` |
| kill | 结束进程 | `kill PID` |
| uptime | 系统负载 | `uptime` |
| free -h | 内存使用 | `free -h` |
| df -h | 磁盘使用 | `df -h` |
| du -sh | 目录大小 | `du -sh *` |

---

## 八、服务管理（systemctl）

| 命令 | 作用 |
| --- | --- |
| systemctl status | 查看状态 |
| systemctl start | 启动服务 |
| systemctl stop | 停止服务 |
| systemctl restart | 重启服务 |
| systemctl enable | 开机自启 |
| systemctl disable | 取消自启 |

示例：

```bash
sudo systemctl restart nginx
```

---

## 九、定时任务（crontab）

| 命令 | 作用 |
| --- | --- |
| crontab -e | 编辑任务 |
| crontab -l | 查看任务 |
| crontab -r | 删除任务 |

时间格式：

```text
* * * * *
分 时 日 月 周
```

---

## 十、vi / vim 常用速记

| 操作 | 命令 |
| --- | --- |
| 插入 | i / a / o |
| 保存 | :w |
| 退出 | :q |
| 保存退出 | :wq |
| 强退 | :q! |
| 删除行 | dd |
| 复制行 | yy |
| 粘贴 | p |
| 查找 | /关键字 |

📌 万能键：`ESC`

---

## 十一、教学记忆口诀（强烈推荐）

> **查文件用 find，查内容用 grep**  
> **看日志用 tail -f**  
> **改权限用 chmod，改归属用 chown**  
> **服务问题先 systemctl status**

---

## 十二、结语

这份速查表覆盖：

- 日常使用
- 教学讲解
- 面试高频
- 运维基础

建议：

- 打印
- 放桌面
- 做成浏览器书签
