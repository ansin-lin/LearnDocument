# 日志系统与分析

> 本章目标：  
>
> - 理解 Linux 日志系统的作用与结构  
> - 熟悉常见日志文件与位置  
> - 掌握查看、过滤、统计日志的常用方法  
> - 能在实际工作中快速定位问题

---

## 1. 什么是日志（Log）？

日志是系统或程序**运行过程中产生的记录信息**，用于：

- 排错（Error / Exception）
- 审计（谁在什么时候做了什么）
- 监控（性能、访问情况）

一句话理解：
> **出问题先看日志**

---

## 2. Linux 日志存放位置

### 2.1 核心目录

```bash
/var/log
```

这是 Linux 中**最重要的日志目录**。

---

### 2.2 常见系统日志

| 日志文件 | 说明 |
| --- | --- |
| syslog / messages | 系统综合日志 |
| auth.log | 登录与认证日志 |
| kern.log | 内核日志 |
| dmesg | 启动/硬件相关日志 |

> 不同发行版名称略有差异（Ubuntu / CentOS）

---

### 2.3 应用日志

常见应用会在各自目录下产生日志，例如：

- Nginx：`/var/log/nginx/`
- MySQL：`/var/log/mysql/`
- Java 应用：项目目录下的 `logs/`

---

## 3. 查看日志的常用命令（重点）

### 3.1 cat（小文件）

```bash
cat app.log
```

不适合大日志文件。

---

### 3.2 less（强烈推荐）

```bash
less app.log
```

常用操作：

- `/关键字`：搜索
- `n / N`：下一个 / 上一个
- `q`：退出

---

### 3.3 tail（最常用）

```bash
tail -n 50 app.log
tail -f app.log
```

📌 `tail -f` 用于实时观察日志变化。

---

## 4. 日志过滤与分析（grep）

### 4.1 查找关键字

```bash
grep ERROR app.log
```

---

### 4.2 忽略大小写

```bash
grep -i error app.log
```

---

### 4.3 排除信息

```bash
grep -v DEBUG app.log
```

---

## 5. 日志统计（wc）

### 5.1 统计错误数量

```bash
grep ERROR app.log | wc -l
```

---

### 5.2 统计访问量

```bash
wc -l access.log
```

---

## 6. find + grep（实战组合）

### 6.1 在指定类型文件中查内容

```bash
find /var/log -name "*.log" -exec grep ERROR {} \;
```

📌 教学重点：
> find 找文件，grep 查内容

---

## 7. 实时排错常用组合（非常重要）

```bash
tail -f app.log | grep ERROR
```

用于：

- 接口调试
- 服务启动排错
- 线上问题复现

---

## 8. systemd 日志（journalctl）

### 8.1 查看系统日志

```bash
journalctl
```

---

### 8.2 查看指定服务日志

```bash
journalctl -u nginx
```

---

### 8.3 实时查看

```bash
journalctl -f
```

---

## 9. 常见日志排错思路

### 场景 1：服务启动失败

1. `systemctl status 服务名`
2. `journalctl -u 服务名`
3. 查 ERROR / FAIL

---

### 场景 2：接口报错

1. `tail -f 应用日志`
2. 重现操作
3. 看异常堆栈

---

## 10. 常见错误与提醒

- 用 cat 看大日志
- 忘记使用 grep 过滤
- 不区分 access / error 日志
- 忽略时间范围

---

## 11. 本章小结

你现在应该能够：

- 知道日志在哪
- 会看、会查、会跟
- 结合 grep / wc 分析日志
- 快速定位问题

---

## 12. 教学练习

### 练习 1

使用 tail 查看最新 100 行日志。

### 练习 2

统计 ERROR 日志数量。

### 练习 3

实时观察日志并触发一次错误。

---

👉 下一章：
**08_exercises_summary.md —— 综合练习与总结（教学完整版）**
