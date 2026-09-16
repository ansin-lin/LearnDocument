# 第18章 部署与运行检查

> 本章目标：把第17章生成的员工管理API可执行JAR部署到Ubuntu 24.04 LTS练习服务器，使用systemd受控启停，并通过进程、端口、HTTP和业务检查判断服务是否可用，最后完成一次可恢复的发布与回滚演练。

## 一、部署不是把JAR复制到服务器就结束

一次后端发布至少经过下面的状态：

```text
已测试的JAR和配置清单
        ↓ 传输与摘要核对
服务器上的独立发布目录
        ↓ 前台启动验证
systemd管理的后台服务
        ↓ 进程、端口、HTTP、业务四层检查
确认发布成功，或按条件回滚到上一版本
```

只看到Java进程，不能证明端口已监听；端口已监听，不能证明HTTP正常；`/health` 返回OK，也不能证明数据库查询、登录和权限功能正常。

本章示例使用专用Ubuntu 24.04 LTS练习机和Bash。写入 `/opt`、`/etc`、`/var/log`、`/var/backups` 与systemd系统单元需要管理员权限，只能由有权限人员按批准手顺执行。不要在共享测试环境或生产环境直接照抄练习命令。

## 二、部署规格和角色边界

### 1. 本次发布规格

| 项目 | 本章固定值 |
| --- | --- |
| 应用 | `employee-management-api` |
| 发布编号 | `20260915-01`，实际作业时替换为批准编号 |
| JAR目标名 | `employee-api.jar` |
| Java | 17 |
| Spring Boot | 3.5.16 |
| 应用监听 | `127.0.0.1:8080` |
| 临时前台验证端口 | `127.0.0.1:18080` |
| 服务名 | `employee-api.service` |
| 服务用户/组 | `employee-api` |
| Profile | `prod` |
| 成功标准 | 服务active、8080监听、health 200、登录及权限业务检查通过 |

### 2. 谁负责什么

| 工作 | 开发人员 | 发布/运维人员 | DBA或数据库负责人 |
| --- | --- | --- | --- |
| 构建、测试、提供SHA-256 | 主责 | 核对 | — |
| 创建系统用户和目录 | 提供要求 | 主责 | — |
| 写入秘密和systemd单元 | 不接触真实值 | 主责 | — |
| 提供数据库变更SQL | 主责/Review | 按手顺协调 | 审核、备份、执行或授权 |
| 发布后接口验证 | 共同 | 共同 | 必要时确认数据库 |
| 决定回滚 | 提供技术判断 | 按发布规则执行 | 涉及数据时共同判断 |

本章假设管理员已经创建不可交互登录的 `employee-api` 服务账号，并已批准当前发布窗口、服务器、数据库和端口。学员不自行创建账号、修改sudo策略或开放防火墙。

## 三、完整部署示例

### 1. 最终目录结构

```text
/opt/employee-api/
├── current -> /opt/employee-api/releases/20260915-01
└── releases/
    ├── 20260901-01/                       ← 上一已知可用版本
    │   └── employee-api.jar
    └── 20260915-01/                       ← 本次新版本
        └── employee-api.jar

/etc/employee-api/
├── application-prod.yml                  ← 非秘密外部配置
└── employee-api.env                      ← 秘密和环境变量，受限读取

/var/log/employee-api/
└── employee-api.log

/var/backups/employee-api/                 ← 经批准的配置/数据备份位置

/etc/systemd/system/
└── employee-api.service
```

`current` 是指向当前发布目录的符号链接。新旧JAR分别保留，切换版本不覆盖上一文件；数据库备份不放在应用发布目录中。

### 2. 完整的外部application-prod.yml

准备文件：`application-prod.yml`

```yaml
spring:
  config:
    activate:
      on-profile: prod
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  address: 127.0.0.1
  port: ${SERVER_PORT:8080}
  shutdown: graceful
  servlet:
    session:
      cookie:
        http-only: true
        secure: true
        same-site: lax

logging:
  file:
    name: ${APP_LOG_FILE}

app:
  deployment:
    environment-name: ${APP_ENVIRONMENT_NAME}
    release-id: ${APP_RELEASE_ID}
```

数据库URL、账号、密码和共同配置仍由第17章JAR内 `application-prod.yml` 与环境变量提供；外部文件只覆盖当前服务器确实需要明确的监听、停机、Cookie和日志设置。

### 3. 完整的employee-api.env模板

准备文件：`employee-api.env`。下面所有值都是教学占位符，实际文件由批准的秘密交付方式生成：

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://db-host.example.invalid:3306/employee_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo
DB_USERNAME=employee_app
DB_PASSWORD=REPLACE_BY_APPROVED_SECRET_DELIVERY
APP_LOG_FILE=/var/log/employee-api/employee-api.log
APP_ENVIRONMENT_NAME=training-linux
APP_RELEASE_ID=20260915-01
SERVER_PORT=8080
```

systemd的EnvironmentFile使用 `KEY=value`，不写Bash的 `export`。真实数据库主机、账号和密码不得出现在Git、聊天记录、截图、发布证据或shell历史中。

### 4. 完整的employee-api.service

准备文件：`employee-api.service`

```ini
[Unit]
Description=Employee Management API
Wants=network-online.target
After=network-online.target
StartLimitIntervalSec=60
StartLimitBurst=3

[Service]
Type=simple
User=employee-api
Group=employee-api
WorkingDirectory=/opt/employee-api/current
EnvironmentFile=/etc/employee-api/employee-api.env
ExecStart=/usr/bin/java -jar /opt/employee-api/current/employee-api.jar --spring.config.additional-location=file:/etc/employee-api/
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
TimeoutStopSec=40
UMask=0027
NoNewPrivileges=true
PrivateTmp=true
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

`ExecStart` 必须使用服务器上实际的Java 17绝对路径。若 `readlink -f "$(command -v java)"` 不是 `/usr/bin/java`，应由运维人员确认并修改单元，不要猜路径。

### 5. 部署前只读检查

登录批准的练习服务器后先确认身份、系统和目录，不执行修改：

```bash
whoami
pwd
cat /etc/os-release
java -version
readlink -f "$(command -v java)"
id employee-api
df -h /opt /etc /var/log
ss -ltnp 'sport = :8080'
ss -ltnp 'sport = :18080'
systemctl is-active employee-api.service
```

若是升级发布，再记录当前版本：

```bash
readlink -f /opt/employee-api/current
systemctl status employee-api.service --no-pager --full
```

数据库客户端可用且已获授权时，使用只读语句验证连通性。`-p` 后不写密码，由客户端安全提示输入：

```bash
mysql --connect-timeout=5 \
  --host=<approved-db-host> \
  --port=3306 \
  --user=employee_app \
  -p \
  --execute='SELECT 1;'
```

`<approved-db-host>` 必须替换为发布手顺中的地址。连接失败后记录错误并停止发布，不循环猜密码，不自行修改数据库、防火墙或网络规则。

### 6. 核对传入的JAR

假设批准的传输工具已把JAR放入当前登录用户的专用交付收件目录。先进入该目录并核对：

```bash
pwd
ls -l -- employee-management-api-0.0.1-SNAPSHOT.jar
sha256sum -- employee-management-api-0.0.1-SNAPSHOT.jar
```

输出摘要必须与第17章同一次构建的交付记录完全一致。文件名相同但摘要不同，应停止并重新确认来源。

### 7. 创建受控目录并安装文件

以下命令只由有sudo权限的发布人员在批准练习机执行：

```bash
sudo install -d -o root -g root -m 0755 \
  /opt/employee-api/releases
sudo install -d -o root -g root -m 0755 \
  /opt/employee-api/releases/20260915-01
sudo install -d -o root -g employee-api -m 0750 \
  /etc/employee-api
sudo install -d -o employee-api -g employee-api -m 0750 \
  /var/log/employee-api
sudo install -d -o root -g root -m 0700 \
  /var/backups/employee-api
```

升级发布且旧的非秘密配置已经存在时，先备份即将被替换的YAML。首次部署跳过这一步：

```bash
sudo cp --preserve=mode,ownership,timestamps -- \
  /etc/employee-api/application-prod.yml \
  /var/backups/employee-api/application-prod.yml.before-20260915-01
```

`employee-api.env` 含数据库密码，不复制到普通备份中；它的旧版本应由获批的秘密管理方式恢复。然后安装本次文件：

```bash
sudo install -o root -g root -m 0644 \
  employee-management-api-0.0.1-SNAPSHOT.jar \
  /opt/employee-api/releases/20260915-01/employee-api.jar
sudo install -o root -g employee-api -m 0640 \
  application-prod.yml \
  /etc/employee-api/application-prod.yml
sudo install -o root -g employee-api -m 0640 \
  employee-api.env \
  /etc/employee-api/employee-api.env
sudo install -o root -g root -m 0644 \
  employee-api.service \
  /etc/systemd/system/employee-api.service
```

核对目标，不用 `chmod -R 777` 绕过权限问题：

```bash
namei -l /opt/employee-api/releases/20260915-01/employee-api.jar
namei -l /etc/employee-api/employee-api.env
namei -l /var/log/employee-api
```

### 8. 前台运行新版本

升级时不要先替换current。使用新发布目录和临时端口18080进行前台验证：

```bash
sudo -u employee-api -- /bin/bash -c '
set -a
source /etc/employee-api/employee-api.env
set +a
SERVER_PORT=18080 exec /usr/bin/java \
  -jar /opt/employee-api/releases/20260915-01/employee-api.jar \
  --spring.config.additional-location=file:/etc/employee-api/
'
```

只允许source由root管理且内容限定为本章 `KEY=value` 格式的文件。不要把外部用户可写文件当作shell脚本执行。

在另一个终端检查：

```bash
curl --fail --silent --show-error --max-time 5 \
  http://127.0.0.1:18080/health
printf '\nexit_status=%s\n' "$?"
```

预期正文为 `OK`、退出状态为0。临时端口只用于确认新JAR、外部配置和基本HTTP启动，不在这里做正式Session业务验收：prod Cookie带有Secure属性，业务验收要等服务切换后从正式HTTPS入口执行。完成前台确认后回到运行JAR的终端按 `Ctrl+C`，确认优雅停止日志并检查18080不再监听。

### 9. 校验并加载systemd单元

```bash
sudo systemd-analyze verify \
  /etc/systemd/system/employee-api.service
sudo systemctl daemon-reload
```

`systemd-analyze verify` 没有错误后才能继续。`daemon-reload` 让systemd重新读取单元文件；它不会重启应用，也不会重新读取应用YAML或环境变量。

### 10. 切换current并启动服务

首次部署时，当前没有旧服务需要停止，创建current后启动：

```bash
sudo ln -sfnT \
  /opt/employee-api/releases/20260915-01 \
  /opt/employee-api/current
readlink -f /opt/employee-api/current

sudo systemctl start employee-api.service
sudo systemctl status employee-api.service --no-pager --full
```

升级发布时，先确认第5步已经记录上一目标，再明确执行“停止旧版→切换链接→启动新版”。不要只切换链接后执行start；服务本来就在运行时，start不会让旧Java进程自动改用新JAR：

```bash
sudo systemctl stop employee-api.service
sudo ln -sfnT \
  /opt/employee-api/releases/20260915-01 \
  /opt/employee-api/current
readlink -f /opt/employee-api/current
sudo systemctl start employee-api.service
sudo systemctl status employee-api.service --no-pager --full
```

预期current解析到 `20260915-01`，服务状态为 `active (running)`。首次发布通过验收且批准开机自动启动后再执行：

```bash
sudo systemctl enable employee-api.service
systemctl is-enabled employee-api.service
```

### 11. 完成四层运行检查

```bash
systemctl is-active employee-api.service
systemctl show employee-api.service \
  --property=MainPID,ActiveState,SubState,ExecMainStatus
ss -ltnp 'sport = :8080'
curl --fail --silent --show-error --max-time 5 \
  http://127.0.0.1:8080/health
printf '\nexit_status=%s\n' "$?"
```

再从正式访问路径完成业务检查：

| 层级 | 检查 | 通过标准 |
| --- | --- | --- |
| 进程 | systemd状态和MainPID | active/running，PID存在且属于目标服务 |
| 监听 | `127.0.0.1:8080` | 目标Java进程监听，不是其他程序 |
| HTTP | `GET /health` | 5秒内200并返回OK |
| 业务 | csrf→login→me→员工接口 | Session、数据库、权限和响应均符合第15～16章规格 |

业务检查必须使用批准的测试账号和脱敏证据。`/health` 当前只证明Web应用能够响应，不访问数据库，因此不能代替业务检查。

### 12. 查看服务日志和应用日志

```bash
sudo journalctl -u employee-api.service \
  --since '10 minutes ago' --no-pager
sudo tail -n 100 -- /var/log/employee-api/employee-api.log
```

只截取与本次发布编号、时间和错误相关的必要行。提交证据前删除密码、Cookie、Session ID、CSRF令牌、数据库完整连接串和个人信息。

### 13. 正常停止、启动和重启

```bash
sudo systemctl stop employee-api.service
systemctl is-active employee-api.service
ss -ltnp 'sport = :8080'

sudo systemctl start employee-api.service
sudo systemctl restart employee-api.service
```

stop后预期为inactive且8080不再监听。start和restart后必须重新执行四层检查。修改 `application-prod.yml` 或 `employee-api.env` 后需要restart；只有修改systemd单元时才需要先执行daemon-reload。

### 14. 回滚到上一已知可用版本

如果新版本出现阻断性故障，且发布负责人决定回滚，保留故障日志后执行：

```bash
sudo systemctl stop employee-api.service
sudo ln -sfnT \
  /opt/employee-api/releases/20260901-01 \
  /opt/employee-api/current
readlink -f /opt/employee-api/current
sudo systemctl start employee-api.service
```

然后重新执行四层检查，并记录新版本失败证据、回滚时间、旧版本摘要和恢复结果。不要立即删除失败发布目录，否则会失去调查材料。

本章发布不包含数据库结构变化，因此JAR回滚即可恢复应用代码。若未来发布修改了表结构或数据，必须先按批准的数据库回滚方案判断兼容性；不能假定切回旧JAR就一定能读取新结构。

## 四、先理解服务器上的五类对象

完整示例中有五种不同对象：

| 对象 | 当前例子 | 谁使用 | 能否互相替代 |
| --- | --- | --- | --- |
| 程序产物 | releases下的JAR | JVM | 不能保存环境秘密 |
| current链接 | 指向一个release | systemd ExecStart | 只选择版本，不复制JAR |
| 外部配置 | `/etc/employee-api` | Spring Boot/systemd | 不放进发布目录随JAR覆盖 |
| 日志 | `/var/log`和journal | 开发、运维 | 不是服务状态本身 |
| 备份 | `/var/backups/employee-api` | 恢复手顺 | 不是长期发布目录 |

源码、IDE工程和本机target目录不是服务器交付物。现场发生问题时也不在服务器上直接修改Java源码或class；应回到受控源码完成修改、测试、重新构建和发布。

### 外部配置中的本章新增项

第17章已经讲过Profile、占位符、日志文件和发布信息，本章第一次把它们放到Linux部署边界中：

| 配置 | 当前值 | 作用 | 本章边界 |
| --- | --- | --- | --- |
| `spring.lifecycle.timeout-per-shutdown-phase` | 30s | 给每个关闭阶段最多30秒 | 需要小于systemd的40秒停止上限 |
| `server.address` | 127.0.0.1 | 只在本机回环地址监听 | 外部客户端必须经过反向代理 |
| `server.port` | `${SERVER_PORT:8080}` | 环境变量可覆盖，缺省8080 | 临时验证覆盖为18080 |
| `server.shutdown` | graceful | 收到正常停止信号时优雅停机 | 不等同于SIGKILL强制结束 |
| Session Cookie三项 | HttpOnly、Secure、SameSite=Lax | 降低脚本读取、明文传输和跨站请求风险 | 正式业务检查必须走HTTPS |
| `logging.file.name` | `${APP_LOG_FILE}` | 由环境指定应用日志绝对路径 | 服务账号必须能够写入其父目录 |
| `app.deployment.*` | 环境名、发布编号 | 让日志和证据识别当前部署 | 不是访问控制或秘密 |

`--spring.config.additional-location=file:/etc/employee-api/` 是传给Spring Boot的命令行参数：保留JAR内默认配置，再追加读取这个外部目录。末尾 `/` 表示目录；这里不加 `optional:`，因此目录不存在时应启动失败，避免生产服务静默使用错误配置。

环境文件里的 `SPRING_PROFILES_ACTIVE` 激活prod，`DB_*` 供数据源读取，`APP_*` 对应第17章的占位符，`SERVER_PORT` 对应上表端口。变量名和值之间不能留多余空格；真实秘密不展示、不提交，也不在命令行重复输入。

## 五、部署前检查命令分别回答什么

| 命令 | 当前参数 | 可接受的值 | 输出或状态 |
| --- | --- | --- | --- |
| `whoami` | 无 | 无 | 当前登录用户名 |
| `pwd` | 无 | 无 | 当前绝对目录 |
| `cat /etc/os-release` | 系统发行信息文件 | 当前系统存在的可读文件 | Ubuntu版本等文本 |
| `java -version` | 无 | 无 | Java运行时版本，应为17 |
| `command -v java` | 命令名java | PATH中可查找的命令名 | Java命令路径；找不到时非0 |
| `readlink -f path` | 符号链接或路径 | 已存在且可解析的路径 | 最终绝对目标 |
| `id employee-api` | 账号名 | 已存在的本机账号 | UID、GID和组；不存在时非0 |
| `df -h path...` | 一个或多个挂载内路径 | 已存在路径 | 对应文件系统容量和可用空间 |
| `ss -ltnp filter` | TCP监听过滤条件 | `ss`支持的过滤表达式 | 监听地址、端口和有权查看的进程 |
| `systemctl is-active unit` | 服务单元名 | 已加载或尚未加载的单元 | active时返回0，inactive/unknown等返回非0 |
| `ls -l -- file` | 待部署JAR | 已存在文件 | 显示类型、权限、所有者、大小和时间 |
| `sha256sum -- file` | 待部署JAR | 可读文件 | 计算内容摘要，用来核对交付物未被替换 |
| `mysql ... --execute='SELECT 1;'` | 获批地址、端口和账号 | 只在已授权且客户端存在时执行 | 用只读SQL确认客户端到数据库的连接与认证 |

`$(command -v java)` 是Bash命令替换：先执行括号内命令，再把输出作为 `readlink` 参数。双引号防止路径被错误分词。

命令失败后立即用 `printf 'exit_status=%s\n' "$?"` 记录上一条命令的退出状态；0通常表示成功，非0需要按该命令含义判断。不要用sudo重复执行所有失败命令。

## 六、install、权限和所有者

`install` 可以创建目录或复制文件，同时明确所有者、组和权限。它不是安装Java包的命令。

| 参数 | 当前值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `-d` | 创建目录时使用 | 无附加值 | 把目标作为目录创建 |
| `-o` | root或employee-api | 已存在的用户 | 设置所有者 |
| `-g` | root或employee-api | 已存在的组 | 设置所属组 |
| `-m` | 0755、0750、0700、0644、0640 | 有效八进制权限 | 设置初始读写执行权限 |

目录需要执行权限才能进入；普通文件的执行权限本章不需要。JAR由root管理且所有人只读，服务不能自行覆盖程序；日志目录由服务用户写入；秘密文件只允许root和服务组读取。

`namei -l` 逐层显示路径中每个目录和最终文件的权限，适合定位“文件本身可读但父目录进不去”。`chmod -R 777` 会扩大整个目录树权限，既不能解释根因，也可能泄露秘密或允许篡改JAR。

`cp --preserve=mode,ownership,timestamps` 在复制旧YAML时保留权限、所有者和时间，便于恢复与审计；它只用于明确存在的非秘密配置。多个命令中的独立 `--` 表示后面都是路径或参数值，避免以连字符开头的文件名被误当成命令选项。

`ln -sfnT 新版本 current` 创建符号链接并把current作为一个链接目标替换：`-s` 创建符号链接，`-f` 允许替换既有目标，`-n` 不跟随既有目录链接，`-T` 明确把current当成普通目标而不是目录。执行前必须记录旧目标，执行后必须用 `readlink -f` 核对实际指向。

## 七、前台运行为什么在systemd之前

前台运行能直接观察启动错误，并把“应用本身是否能启动”和“systemd单元是否正确”分开：

```text
前台也失败 → 先查JAR、Profile、配置、权限、端口、数据库
前台成功而服务失败 → 再查systemd用户、路径、EnvironmentFile和单元
```

`sudo -u employee-api` 以服务账号执行，能提前发现日志目录和配置读取权限问题。`set -a` 让随后source得到的变量自动导出给Java子进程，`set +a` 关闭该行为，`exec` 用Java进程替换当前Bash进程。

临时使用18080避免升级时和现有8080服务冲突。`curl` 参数含义：

| 参数 | 当前值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `--fail` | 无值 | 无 | HTTP 400及以上返回非0 |
| `--silent` | 无值 | 无 | 不显示进度条 |
| `--show-error` | 无值 | 无 | silent时仍显示错误 |
| `--max-time` | 5秒 | 大于0的秒数 | 限制整个请求最长时间 |

超时防止发布手顺永久等待；失败时保留curl退出码和应用日志，不无限重试。

## 八、systemd单元怎样管理Java进程

systemd是Ubuntu 24.04使用的系统与服务管理器。本章单元的关键项如下：

| 配置 | 当前值 | 可接受的值 | 运行效果 |
| --- | --- | --- | --- |
| `Description` | Employee Management API | 简短可识别文本 | status等输出中显示服务用途 |
| `Wants` | network-online.target | 已存在的单元名 | 启动时弱依赖网络就绪目标，其失败不必然阻止本服务 |
| `After` | network-online.target | 已存在的单元名 | 只规定启动顺序，不代表数据库一定可连接 |
| `StartLimitIntervalSec` | 60 | 非负时间 | 统计启动次数的60秒窗口 |
| `StartLimitBurst` | 3 | 非负整数 | 窗口内限制反复启动为3次 |
| `Type` | simple | systemd支持的服务类型 | ExecStart进程直接作为主进程 |
| `User`、`Group` | employee-api | 已存在账号和组 | 不以root运行Java |
| `WorkingDirectory` | current目录 | 服务用户可进入的绝对目录 | 确定相对路径基准 |
| `EnvironmentFile` | `/etc/...env` | 可读的环境文件绝对路径 | 启动前加载配置变量 |
| `ExecStart` | Java绝对路径＋JAR＋参数 | 一个有效启动命令 | 创建被管理的主进程 |
| `SuccessExitStatus` | 143 | 合理的额外成功状态 | 把Java收到TERM后的143也记录为正常退出 |
| `Restart` | on-failure | no、on-failure、always等 | 异常退出时尝试恢复，人工stop不重启 |
| `RestartSec` | 5 | 非负时间 | 重启前等待5秒 |
| `TimeoutStopSec` | 40 | 正时间或infinity | 最多等待40秒，之后可能强制结束 |
| `UMask` | 0027 | 有效umask | 限制进程新建文件的默认权限 |
| `NoNewPrivileges` | true | true或false | 阻止进程取得新的更高权限 |
| `PrivateTmp` | true | true或false | 为服务提供隔离的临时目录视图 |
| `StandardOutput`、`StandardError` | journal | systemd支持的输出目标 | 标准输出和错误进入journal |
| `WantedBy` | multi-user.target | 已存在的目标单元 | enable时建立开机启动关系 |

`Restart=on-failure` 适合长时间服务，但启动配置错误时反复重启会制造日志噪音，所以同时限制启动频率。systemd对重启和停止超时的行为可参考[Ubuntu systemd.service手册](https://manpages.ubuntu.com/manpages/noble/man5/systemd.service.5.html)。

`systemd-analyze verify` 在加载前检查单元语法和部分依赖问题；它通过不代表Java路径、账号、配置或数据库在真实运行时一定正确。`daemon-reload` 才让systemd管理器重新读取磁盘上的单元定义。

`systemctl start` 只启动本次，`enable` 只配置开机启动，二者不是同一动作；`restart` 是停止后再启动。单元文件改变后执行 `daemon-reload`，应用配置改变只需重启服务。参考[Ubuntu systemctl手册](https://manpages.ubuntu.com/manpages/noble/man1/systemctl.1.html)。

`nohup java -jar ... &` 虽然能让进程在终端关闭后继续运行，但没有统一的服务身份、期望状态、失败重启、开机启动和标准操作入口，因此不作为本章的正式部署方式。排查时也不要用 `ps | grep` 后按模糊名称结束进程；systemd已经保存了这个服务的准确MainPID。

## 九、优雅停止和强制结束的区别

`systemctl stop` 默认向主进程发送SIGTERM。Spring Boot收到正常停止信号后关闭应用上下文，并在限定时间内让已接收请求完成：

```text
systemctl stop
  → SIGTERM
  → Spring Boot停止接收新请求
  → 等待进行中的请求，最长30秒
  → Java进程退出
```

外部 `TimeoutStopSec=40` 比应用30秒宽限更长，避免systemd在应用自己的优雅停止完成前就强制结束。Spring Boot 3.5已支持优雅停止，本章显式写出配置便于交付核对，参考[Spring Boot Graceful Shutdown](https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html)。

不要把 `kill -9` 作为正常停止方式。SIGKILL不能被Java处理，会跳过应用关闭流程；只有在批准的故障恢复手顺中、确认准确PID并保留证据后，才由负责人决定是否强制结束。

## 十、进程、监听、HTTP和业务健康

四层检查不能互相替代：

```text
systemd active
   └─ 只证明主进程尚未退出
127.0.0.1:8080 LISTEN
   └─ 只证明某进程监听端口
/health 200 OK
   └─ 只证明当前Web入口能响应
登录＋权限＋员工查询成功
   └─ 才进一步证明Session、安全链、Service、Mapper和数据库协作
```

若未来引入Spring Boot Actuator，可以设计更细的liveness/readiness，但当前 `pom.xml` 没有Actuator，不能在手顺中虚构 `/actuator/health`。

业务检查若失败而health成功，应从第11章requestId日志进入Controller→Service→Mapper→数据库链路调查，不要先重启多次。

## 十一、journal与应用文件日志

systemd会把服务标准输出和标准错误交给journal；Spring Boot还按第17章配置写入应用滚动日志。两者用途不同：

| 日志 | 适合查看 | 当前命令 |
| --- | --- | --- |
| journal | systemd启动失败、退出码、重启、控制台启动信息 | `journalctl -u ...` |
| 应用日志文件 | requestId、业务流程、Mapper和应用异常 | `tail -n 100 ...` |

`tail -n 100` 读取最后100行后退出，适合保存发布证据；`tail -f` 会持续等待新日志，适合短时间观察启动或复现故障，结束观察时按 `Ctrl+C`。正式调查仍要记录时间范围，避免把无关历史日志混入证据。

`journalctl -u` 按单元过滤，`--since` 限定时间，`--no-pager` 直接输出而不进入分页器。Ubuntu手册说明journal读取权限可能受root或特定组限制，参见[journalctl](https://manpages.ubuntu.com/manpages/noble/man1/journalctl.1.html)。

日志文件位置不是所有服务器都固定相同；必须以本次 `APP_LOG_FILE`、systemd和日志平台配置为准。磁盘已满时继续启动或反复输出异常可能扩大故障，应先按运维手顺处理容量和日志保留。

## 十二、反向代理、HTTPS和应用端口

本章应用只监听回环地址：

```text
浏览器
  → HTTPS 443
  → 反向代理（证书、TLS、访问日志）
  → HTTP 127.0.0.1:8080
  → Spring Boot
```

反向代理是对外入口，Spring Boot内置Tomcat是应用服务器。域名由DNS解析，证书保护HTTPS连接，防火墙限制网络访问；这些对象不能用“应用已经启动”代替检查。

第15章生产Cookie设置为Secure后，浏览器只会通过HTTPS发送。若直接用公网HTTP访问8080，既破坏安全边界，也可能造成登录会话无法正常工作。反向代理、证书和防火墙配置必须由对应负责人完成；本章不提供未经环境确认的Nginx配置。HTTP概念可回顾[HTTP、Cookie与CORS](../../../web_basics/01_http_rest_cookie_cors.md)。

## 十三、数据库准备和发布顺序

当前第18章没有新表、新列和数据修正，发布前只确认第9、13、15章需要的表和账号已经存在，JAR不会自动替你修改数据库。

未来发布包含数据库变更时，发布计划至少要说明：

1. 变更SQL适用MySQL 8.0，影响哪些表、行和索引。
2. 谁审核和执行，执行前采用什么备份，怎样验证备份可恢复。
3. 先部署兼容旧应用的扩展性变更，再发布新JAR。
4. 新旧JAR是否都能在变更后的结构上运行。
5. 数据写入后是否可逆，回滚JAR是否还需要回滚数据。
6. 删除列、改类型等破坏性清理是否延迟到后续独立发布。

本课程当前没有引入Flyway或Liquibase，不能声称JAR启动会自动执行版本化迁移。实际项目若使用迁移工具，应以项目既有规则和DBA手顺为准。

## 十四、发布后何时回滚

发布前先写清成功标准和回滚条件，不能发生故障后临时争论：

| 观察结果 | 判断 | 动作 |
| --- | --- | --- |
| 四层检查全部通过 | 发布成功候选 | 继续观察并完成记录 |
| 服务无法启动或反复重启 | 阻断 | 保留日志，按批准决定回滚 |
| health失败或超时 | 阻断 | 查端口与日志，未及时恢复则回滚 |
| 登录、权限或核心查询失败 | 核心业务故障 | 停止放量并回滚 |
| 非核心日志告警 | 依据规格判断 | 记录、评估，不擅自忽略或回滚 |
| 数据库结构与旧JAR不兼容 | 普通JAR回滚不可行 | 执行数据库专项恢复方案 |

回滚成功不是“命令执行完”，而是current指向旧版本、服务重新active、四层检查恢复，并留下时间、执行者、原因、证据和残留问题。

## 十五、常见故障的定位顺序

| 现象 | 先查 | 常见原因 | 恢复方向 |
| --- | --- | --- | --- |
| unit not found | 单元路径、daemon-reload | 文件未安装或未重载 | 修正单元安装并重载 |
| status显示203/EXEC | ExecStart和Java权限 | Java路径错误或不可执行 | 核对绝对路径和Java 17 |
| status快速重启 | journal最早错误 | Profile、占位符或数据库失败 | stop后修配置，避免重启风暴 |
| Permission denied | namei、User、Group | 父目录不可进入或日志不可写 | 按最小权限修目标，不用777 |
| Address already in use | ss和MainPID | 端口被旧进程或其他服务占用 | 确认归属后按负责人决定 |
| health连接拒绝 | systemd、ss | 进程退出或未监听 | 查status和journal |
| health 200但业务500 | requestId和应用日志 | 数据库、SQL或业务异常 | 沿调用链定位，不重复重启 |
| 浏览器登录后仍无Session | HTTPS、Cookie、代理头 | Secure Cookie经HTTP访问或代理配置不符 | 核对正式HTTPS路径 |
| stop长时间不结束 | 当前请求和停机日志 | 请求未完成或资源关闭阻塞 | 等待宽限并按手顺升级处理 |

发生故障先停止重复操作，记录命令、时间、退出状态、服务状态和必要日志。不要同时改权限、端口、数据库和systemd单元，否则无法判断哪一项真正解决问题。

## 十六、发布与恢复记录

一次发布记录至少包含：

```text
发布对象：employee-management-api
目标环境：training-linux
新版本：20260915-01
旧版本：20260901-01
JAR SHA-256：填写已核对摘要
配置变更：application-prod.yml / employee-api.env（只写键名）
数据库变更：无
前台验证：18080 health与核心业务结果
服务切换：current目标、开始/结束时间
发布后确认：进程、8080监听、health、登录权限业务
回滚条件：服务、health或核心业务阻断
回滚结果：未执行，或填写旧版本恢复证据
残留事项：填写实际问题；没有则写“无”
```

证据中的版本、摘要、配置键、日志时间和请求结果要能互相对应。只写“测试OK”“部署完成”不能让Reviewer复核。

## 十七、规格理解、影响调查、Review与练习

### 练习1：完成专用练习机部署

在批准的Ubuntu练习机按完整示例部署，提交JAR摘要、current目标、systemd状态、监听、health、业务检查和日志证据。然后正常stop/start一次，证明手顺可重复。

### 练习2：故障注入与恢复

在练习机把 `APP_RELEASE_ID` 临时改为空值并restart，记录启动失败、systemd状态和journal最早原因。恢复正确配置后restart并完成四层检查。不得删除第17章的配置校验。

### 练习3：执行一次JAR回滚演练

准备两个均已测试的练习版本，切换到新版本后按批准手顺回滚到旧版本。验收必须包含回滚前后的current目标、服务状态、接口结果和失败版本保留位置。

### 练习4：数据库变更影响调查

假设下一版本要把 `employees.status` 长度从20改为30。只提交调查：MySQL DDL、锁和停机风险、备份、旧JAR兼容性、数据验证、上线顺序及回滚限制，不在共享数据库执行。

### 练习5：Review危险手顺

对下面手顺提出指摘并给出安全替代：

```bash
sudo chmod -R 777 /opt
sudo pkill -9 java
nohup java -jar employee-api.jar > app.log 2>&1 &
curl http://public-ip:8080/health
```

Review至少覆盖：权限范围过大、误杀其他Java进程、没有服务账号与进程管理、无配置/摘要/超时、公开应用端口、只有health没有业务检查、没有回滚目标。

### 练习6：提交一次障害调查记录

场景：发布后systemd为active、health为200，但管理员查询员工列表返回500。按时间顺序记录请求、requestId、应用日志、数据库连通性、影响范围、临时处置、是否回滚和残课题。不要把“重启后恢复”当作根因。

## 十八、本章稳定状态

完成后，服务器保留分版本JAR、current链接、外部配置、受限秘密文件、应用日志和回滚目标；员工管理API由systemd使用非root账号管理，可以正常启停并接受优雅停止；发布结果由进程、监听、HTTP和业务四层证据确认；发生应用故障时能恢复上一已知可用JAR，涉及数据库结构时能识别普通回滚的限制。

下一章将在这套可构建、可测试、可部署、可回滚的项目上完成一次日本项目改修票，不在服务器上直接修改源码。
