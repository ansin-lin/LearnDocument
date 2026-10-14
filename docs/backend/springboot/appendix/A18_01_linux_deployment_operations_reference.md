# A18-1 Linux部署与运维手顺参考

本附录承接[第18章](../mainline/18_deployment_operations_check.md)中较深的Linux部署操作。示例以Ubuntu 24.04 LTS和systemd为背景，必须在专用练习机或获得授权的环境使用；生产目录、账号、端口和安全要求以项目手顺为准。

## 一、建议的服务器对象

```text
/opt/employee-api/
├── releases/
│   └── 20261013/employee-api.jar
├── current -> releases/20261013
└── config/application-prod.yml

/var/log/employee-api/
└── application.log
```

多版本目录和 `current` 软链接便于识别当前版本，但不是唯一发布方式。运行账号应只拥有读取程序与配置、写指定日志目录所需的最小权限。

创建账号、目录、所有者和权限属于管理员操作。`install`、`chown`、`chmod`、`namei` 等命令应由批准手顺给出精确目标，不允许学员自行猜测后在生产执行。

## 二、systemd Unit参考

```ini
[Unit]
Description=Employee Management API
After=network.target

[Service]
Type=simple
User=employee-api
WorkingDirectory=/opt/employee-api/current
EnvironmentFile=/etc/employee-api/employee-api.env
ExecStart=/usr/bin/java -jar /opt/employee-api/current/employee-api.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

关键字段：

| 字段 | 作用 |
| --- | --- |
| `User` | 指定非root运行账号 |
| `WorkingDirectory` | 指定应用工作目录 |
| `EnvironmentFile` | 从受控文件读取环境变量 |
| `ExecStart` | 实际启动命令 |
| `Restart` | 异常退出后的重启策略 |

Unit文件不应包含明文生产密码。修改Unit后通常需要管理员执行daemon reload；启停、开机启用和日志权限均由项目手顺决定。

## 三、发布前后检查

发布前只读确认：

```bash
java -version
ls -l /opt/employee-api/current
sudo systemctl status employee-api
```

发布后按顺序验证：

1. systemd服务状态；
2. Java进程；
3. 预期端口；
4. 健康接口；
5. 登录、数据库查询和授权；
6. 应用日志无持续异常；
7. 监控或相关系统无异常。

不要因为systemd显示active就跳过业务验证。

## 四、更新与切り戻し

典型更新思路：

1. 核对批准版本与摘要；
2. 保存当前版本、配置和必要数据备份；
3. 安装新JAR到独立版本目录；
4. 在不接收正式流量的条件下验证启动；
5. 按手顺切换版本并重启；
6. 执行四层检查和业务回归；
7. 达到停止条件时执行切り戻し。

切り戻し旧JAR前必须确认数据库兼容性。若新版本已经执行不可逆DDL或写入新格式数据，单独切换软链接可能无法恢复。数据库迁移必须有独立的备份、兼容和恢复方案。

## 五、故障调查命令的边界

常见入口包括：

```bash
sudo systemctl status employee-api
sudo journalctl -u employee-api
ps
ss
```

它们分别观察服务、日志、进程和端口。项目手顺应限定时间范围、输出保存位置和是否允许使用sudo。不要用强制结束进程、递归改权限或删除目录作为第一反应。

调查记录至少包含发生时间、版本、环境、现象、影响范围、requestId、日志依据、临时处置、最终原因和复验结果。敏感信息需要脱敏。

## 六、反向代理与HTTPS

正式系统可能在应用前放置反向代理或负载均衡，并由它处理公开域名和TLS证书。此时：

- 外部端口与Spring Boot监听端口可能不同；
- 健康检查路径可能分内部与外部；
- 转发头需要按可信代理范围配置；
- Session Cookie的Secure、SameSite等属性需结合HTTPS与前端部署方式确认。

这不是要求在本课程安装Nginx。网络、证书、代理和防火墙由对应基础设施设计与发布手顺管理。

## 七、运维交付记录

| 项目 | 应记录内容 |
| --- | --- |
| 发布对象 | 应用名、版本、JAR摘要 |
| 环境 | 服务器、Profile、端口 |
| 配置 | 非秘密配置版本、秘密来源 |
| 操作 | 开始结束时间、执行者、批准号 |
| 验证 | 进程、端口、HTTP、业务结果 |
| 日志 | 关键启动信息与异常有无 |
| 恢复 | 切り戻し条件、版本和结果 |

本附录是识读参考，不替代组织批准的リリース手順書。
