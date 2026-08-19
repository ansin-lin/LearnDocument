# 第15章 Linux 部署基础

> 本章目标：把 Spring Boot jar 放到 Linux 上运行，能启动、停止并查看日志。

## 一、部署前准备

部署前需要确认：

- Linux 服务器可以登录
- 已安装 Java 17
- MySQL 可以访问
- jar 文件已经打包完成
- 生产配置已经准备好

## 二、部署目录

示例目录：

```text
/opt/employee-management-api/
```

建议目录结构：

```text
/opt/employee-management-api/
├── employee-management-api.jar
├── logs/
└── start.sh
```

创建目录：

```bash
sudo mkdir -p /opt/employee-management-api/logs
```

复制 jar 到部署目录：

```bash
sudo cp target/employee-management-api-0.0.1-SNAPSHOT.jar /opt/employee-management-api/employee-management-api.jar
```

## 三、配置环境变量

生产环境数据库连接信息建议通过环境变量传入。

```bash
export DB_URL="jdbc:mysql://数据库地址:3306/employee_db"
export DB_USERNAME="employee_user"
export DB_PASSWORD="数据库密码"
```

这些值会被 `application-prod.yml` 中的 `${DB_URL}`、`${DB_USERNAME}`、`${DB_PASSWORD}` 读取。

## 四、启动命令

```bash
nohup java -jar employee-management-api.jar --spring.profiles.active=prod > app.log 2>&1 &
```

说明：

| 命令片段 | 作用 |
| --- | --- |
| `nohup` | 关闭终端后程序继续运行 |
| `java -jar` | 运行 Spring Boot jar |
| `--spring.profiles.active=prod` | 使用生产配置 |
| `> app.log 2>&1` | 把输出写入日志文件 |
| `&` | 后台运行 |

在部署目录中执行：

```bash
cd /opt/employee-management-api
nohup java -jar employee-management-api.jar --spring.profiles.active=prod > logs/app.log 2>&1 &
```

## 五、查看进程

```bash
ps -ef | grep employee-management-api
```

如果服务已经启动，可以看到 Java 进程。

## 六、查看日志

```bash
tail -f logs/app.log
```

看到启动完成日志后，再访问健康检查接口：

```text
http://服务器IP:8080/health
```

## 七、停止服务

```bash
ps -ef | grep employee-management-api
kill 进程ID
```

如果普通停止无效，再确认是否需要强制停止：

```bash
kill -9 进程ID
```

`kill -9` 会强制结束进程，生产环境不要优先使用。

## 八、常见错误

| 错误 | 原因 | 修正 |
| --- | --- | --- |
| `java: command not found` | 没安装 Java 或 PATH 不正确 | 安装 Java 17 并配置 PATH |
| 数据库连接失败 | 环境变量或网络不通 | 检查 `DB_URL`、账号、密码、防火墙 |
| 端口访问不到 | 服务没启动或端口未开放 | 查进程、日志、防火墙 |
| 关闭终端服务停止 | 没使用后台运行 | 使用 `nohup` 和 `&` |
| 日志文件没有内容 | 输出路径错误或无权限 | 检查目录和写权限 |

## 九、本章练习

请完成：

1. 在 Linux 目录中运行 jar。
2. 查看启动日志。
3. 访问 `/health` 接口。
4. 停止服务并再次确认进程不存在。
5. 说明 `nohup`、`&`、`tail -f` 的作用。

## 十、本章总结

- Spring Boot 项目可以打包成 jar 后部署到 Linux。
- 生产环境配置应通过环境变量注入。
- 启动后必须检查进程、日志和健康检查接口。
- 停止服务前应确认目标进程，避免误杀其他 Java 程序。
