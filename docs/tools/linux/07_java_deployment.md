# 第 7 章：软件安装、运行环境与 Spring Boot 项目部署

本章读懂 Java 项目从源码变成 Linux 服务的全过程，并学会根据服务、端口、接口和日志判断部署结果。命令与配置片段用于识读，**本课程不提供可部署的练习项目，也不要求实际发布**；第 2 章的 `~/learndoc-linux-lab/training-app` 只是文件操作用的模拟目录。真实项目环境的部署必须按作业手顺执行。

## 7.1 Java 应用在 Linux 上如何运行

```text
Java 源码 → Maven 构建 → 可执行 Spring Boot JAR
                              ↓ 上传到 Linux（需要远程部署时）
Java 运行时执行 JAR → Java 进程 → systemd 管理服务（可选）
                              ↓
                     检查服务、端口、HTTP 与日志
```

**源码**是开发者编写的 `.java` 文件；**Maven**根据 `pom.xml` 编译、测试并打包；**JAR**是构建产物，不是正在运行的进程。执行 `java -jar` 才会尝试启动 Java 进程。systemd 可以按 `.service` 文件中的规则管理进程，但进程存在、服务显示 `active` 都不能单独证明业务 API 正常。

项目使用的 Java 和 Spring Boot 版本以其构建文件及作业手顺为准；构建机需要匹配的 JDK 与 Maven，运行 JAR 的服务器需要兼容的 Java 运行时。本课程不要求学员安装 Maven 或 Java 才能完成流程识读。

## 7.2 Java 运行环境

在有 Java 的 Bash 环境中查看；在线环境没有 Java 时，阅读命令与以下解释即可：

```bash
java -version
command -v java
echo "$JAVA_HOME"
echo "$PATH"
```

`java -version` 显示实际运行时版本；`command -v java` 显示当前 Shell 会调用的 `java` 路径。**JDK**包含编译工具和运行时，开发机构建项目需要 JDK；Linux 服务器运行已构建的 JAR 至少需要兼容的 Java 运行时。`PATH` 是查找命令的目录列表，`JAVA_HOME` 是部分工具使用的 Java 安装目录；`JAVA_HOME` 为空并不必然表示 Java 无法运行。服务配置中的 Java 路径还需与目标服务器上的实际路径一致。

## 7.3 软件安装基础

**软件包**是发行版提供的安装单位；**软件仓库**是系统配置的软件来源；**包管理器**负责查找包、安装包并处理依赖。Ubuntu 常用 `apt`，RHEL/Rocky Linux 常用 `dnf`，不能在同一台机器上随意互换。下面是**阅读示例**，不在在线环境或项目服务器执行：

```bash
sudo apt update
sudo apt install openjdk-17-jre-headless
java -version
```

`sudo` 请求按授权执行管理操作；`apt update` 更新包索引，不等于升级整个系统；`apt install` 安装 Java 17 运行时及依赖；最后检查版本。RHEL/Rocky 的对应写法通常是 `sudo dnf install java-17-openjdk-headless`。安装会修改系统并下载软件，本课程只要求看懂用途，不在练习环境执行。写法可查[Ubuntu 官方包管理说明](https://ubuntu.com/server/docs/how-to/software/package-management/)与[Red Hat DNF 说明](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/9/html/managing_software_with_the_dnf_tool/assembly_installing-rhel-9-content_managing-software-with-the-dnf-tool)。

## 7.4 Maven 构建与 JAR

下面是**假设项目已有 `pom.xml`，并且构建后生成 `target/app.jar`** 时的命令示意，不对应本课程中的实际文件：

```bash
mvn package
ls -l target/app.jar
java -jar target/app.jar
```

`mvn package` 在 Maven 项目根目录编译、运行项目配置的测试并打包；只有出现 `BUILD SUCCESS` 且实际 JAR 非空，才可考虑进入发布流程。`ls -l` 用于核对构建产物；真实文件名须以项目结果为准。`java -jar` 会尝试在前台启动 JAR；程序运行时终端通常被占用，按 `Ctrl+C` 可停止这一前台进程。JAR 存在本身不表示程序已启动，构建成功也不代表所有业务接口正常。

## 7.5 上传与部署

开发机与 Linux 服务器分离时，通常需要把**已经构建并验证的 JAR**上传到目标主机，再按项目手顺保存旧版、放置新版。第 6 章介绍过 SSH/SCP；`scp 本地文件 用户@主机:远端路径` 复制文件，不会自动启动应用。上传前必须确认目标环境、账号、路径和版本。

```text
开发机：已构建 JAR
   ↓ scp 上传
Linux：上传文件 → 当前运行件 app.jar → java -jar 或 systemd 启动
```

## 7.6 systemd 服务管理

Linux 环境若使用 systemd，可以用 service 单元描述启动方式。下面只是**配置片段示意**，不是完整可安装的 service 文件：

```ini
User=appuser
WorkingDirectory=/opt/app
Environment=SERVER_ADDRESS=127.0.0.1
ExecStart=/usr/bin/java -jar /opt/app/app.jar
```

`User` 指定运行账号；`WorkingDirectory` 是进程工作目录；`Environment` 设置运行时变量；`ExecStart` 指向 Java 程序和 JAR。片段中的账号、路径和变量只是示意，必须由项目配置决定；`SERVER_ADDRESS` 只有在应用实际读取它时才会生效。**Profile** 用于选择环境配置，具体文件和激活方式也以项目为准。真实项目的数据库密码等秘密不应写进 JAR、Git 或示例 unit。详细配置来源见[Spring Boot 官方说明](https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html)。

`systemctl status` 用于查看服务状态；`start`、`stop` 改变服务状态，均以目标机器使用 systemd 且服务名称正确为前提。本章只识读配置和状态，不安装或启停服务。可参考[Spring Boot 官方 systemd 说明](https://docs.spring.io/spring-boot/4.1/how-to/deployment/installing.html)。

## 7.7 部署后确认与回退思路

阅读一组按层检查的命令；`training-app.service`、8080 端口和两个 URL 都是**假设项目已提供**的示例值，不对应本课程中实际运行的应用：

```bash
systemctl status training-app.service --no-pager
ss -lnt
curl -i --connect-timeout 3 --max-time 10 http://127.0.0.1:8080/health
curl -i --connect-timeout 3 --max-time 10 http://127.0.0.1:8080/api/employees
journalctl -u training-app.service -n 50 --no-pager
```

它们分别观察服务、监听端口、健康接口、业务接口和最近服务日志。不能用 `active` 代替 HTTP 验证，也不能用健康接口 `200` 代替员工接口验证；收到 `500` 时应查对应时间的日志。第 5、6 章已解释这些命令的主要选项，本节重点是**组合判断**。`127.0.0.1` 只表示从服务器本机请求，不能据此判断外部入口。

发布前保存已验证的旧版本，是为了新版本验证失败时有恢复依据。回退不只是替换 JAR，还需按项目手顺恢复运行状态，并重新检查**健康与业务接口**。真实发布还可能涉及数据库迁移和流量切换，不能把替换文件视为通用回退手顺。

[上一章：网络、SSH 与远程访问](06_network_ssh.md) · [下一章：综合故障调查与回退](08_integrated_troubleshooting.md)
