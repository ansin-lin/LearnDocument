# 第20章 综合验收与交付

> 本章目标：把第1～19章完成的员工管理API整理成可重建、可运行、可测试、可部署、可回退的交付包，并由另一位学员只依据交付资料完成独立验收。

## 一、最终验收不是再看一次“能否启动”

服务启动只证明进程没有立即退出。最终交付要同时回答：

```text
需求是否都有实现
  → 接口、权限和数据规则是否一致
  → 源码能否从干净环境重新构建
  → 数据库能否按脚本建立
  → 自动化测试能否重复通过
  → JAR能否脱离IDE运行
  → 部署、停止和回退是否有手顺
  → 另一位开发者能否只看资料接手
```

本章不增加新的Controller、框架或业务功能。开始状态是第19章EMP-241已经完成并通过Review；若前面仍有失败测试或未确认规格，应先解决，不能用“最终整理”掩盖未完成项。

## 二、完整交付成果

### 1. 交付包目录

最终交付包使用发布编号 `20260915-01`。实际日期和编号由项目规则决定：

```text
employee-management-api-20260915-01/
├── README.md
├── RELEASE_NOTES.md
├── MANIFEST.sha256.txt
├── source/
│   └── employee-management-api/
│       ├── .mvn/
│       ├── mvnw
│       ├── mvnw.cmd
│       ├── pom.xml
│       └── src/
├── database/
│   ├── 01_schema_mysql.sql
│   └── 02_training_seed_mysql.sql
├── artifact/
│   └── employee-management-api-0.0.1-SNAPSHOT.jar
├── operations/
│   ├── application-prod.yml.template
│   ├── employee-api.env.template
│   ├── employee-api.service
│   └── DEPLOYMENT.md
└── evidence/
    ├── ACCEPTANCE_RECORD.md
    ├── automated-test.txt
    ├── api-test.md
    └── deployment-test.md
```

不交付IDE工作区、`target/`中间文件、真实 `.env`、生产密码、Cookie、Session ID、CSRF令牌、个人数据或本机日志。artifact目录只放本次已核对摘要的可执行JAR。

### 2. 最终源码目录

`source/employee-management-api` 应至少具有下面的稳定结构：

```text
employee-management-api/
├── .mvn/wrapper/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/main/java/com/example/employee/
│   ├── EmployeeManagementApiApplication.java
│   ├── common/ApiResponse.java
│   ├── config/
│   │   ├── DeploymentProperties.java
│   │   ├── RequestLoggingFilter.java
│   │   ├── SecurityConfig.java
│   │   └── StartupConfigurationLogger.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── EmployeeController.java
│   │   └── HealthController.java
│   ├── dto/request/
│   │   ├── EmployeeCreateRequest.java
│   │   ├── EmployeeSearchRequest.java
│   │   ├── EmployeeUpdateRequest.java
│   │   └── LoginRequest.java
│   ├── dto/response/
│   │   ├── CsrfResponse.java
│   │   ├── CurrentUserResponse.java
│   │   ├── EmployeeListItemResponse.java
│   │   ├── EmployeeResponse.java
│   │   └── PageResponse.java
│   ├── entity/
│   │   ├── AppUser.java
│   │   ├── Employee.java
│   │   └── EmployeeChangeLog.java
│   ├── exception/
│   │   ├── AuthExceptionHandler.java
│   │   ├── DuplicateEmailException.java
│   │   ├── EmployeeNotFoundException.java
│   │   ├── EmployeeSystemException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InvalidDepartmentException.java
│   │   ├── RestAccessDeniedHandler.java
│   │   └── RestAuthenticationEntryPoint.java
│   ├── mapper/
│   │   ├── AppUserMapper.java
│   │   ├── EmployeeChangeLogMapper.java
│   │   └── EmployeeMapper.java
│   ├── security/
│   │   ├── AppUserDetailsService.java
│   │   └── EmployeeAccess.java
│   └── service/
│       ├── EmployeeService.java
│       └── impl/EmployeeServiceImpl.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── mapper/
│       ├── AppUserMapper.xml
│       ├── EmployeeChangeLogMapper.xml
│       └── EmployeeMapper.xml
├── src/test/java/com/example/employee/
│   ├── config/DeploymentPropertiesTest.java
│   ├── controller/EmployeeControllerWebTest.java
│   ├── integration/
│   │   ├── EmployeeCrudIntegrationTest.java
│   │   └── EmployeeSearchIntegrationTest.java
│   ├── security/
│   │   ├── AuthorizationIntegrationTest.java
│   │   └── SessionLoginTest.java
│   └── service/EmployeeServiceImplTest.java
└── src/test/resources/
    ├── application-test.yml
    ├── search-test-data.sql
    └── test-data.sql
```

目录清单用于发现缺文件，不能代替编译和测试。多出文件也要说明用途；无法说明的临时类、演示接口和旧配置不进入交付。

### 3. 完整的01_schema_mysql.sql

该脚本适用于MySQL 8.0.16及以上版本的全新、专用验收数据库。课程依赖 `CHECK` 约束限制status，而MySQL从8.0.16起才真正支持并执行该约束，参见[MySQL CHECK_CONSTRAINTS说明](https://dev.mysql.com/doc/refman/8.0/en/information-schema-check-constraints-table.html)。数据库和最小权限账号由有权限人员事先创建，脚本本身不创建账号、不保存密码，也不删除已有表：

```sql
CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT ck_employees_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
) AUTO_INCREMENT = 1001;

CREATE TABLE employee_change_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_employee_change_logs PRIMARY KEY (id),
    INDEX idx_employee_change_logs_employee_id (employee_id)
);

CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    employee_id BIGINT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT uk_app_users_employee_id UNIQUE (employee_id)
);
```

### 4. 完整的02_training_seed_mysql.sql

该脚本只能放入个人验收环境。下面的固定哈希对应课程练习密码 `TrainingPass123!`，不得用于生产或共享账号：

```sql
INSERT INTO employees (
    id, name, department, email, status
) VALUES
    (1001, 'Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    (1002, 'Sato', 'Development', 'sato@example.com', 'ACTIVE'),
    (1003, 'Yamada', 'Support', 'yamada@example.com', 'INACTIVE');

INSERT INTO app_users (
    id, username, password_hash, employee_id, role, enabled
) VALUES
    (1, 'tanaka',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1001, 'USER', TRUE),
    (2, 'sato',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1002, 'USER', TRUE),
    (3, 'admin',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     NULL, 'ADMIN', TRUE);
```

生产初始化不执行此文件。生产账号由批准的账号管理流程创建，初始凭据不得固定在交付包中。

### 5. 完整替换测试用test-data.sql

最终验收要求H2测试表的字段长度和关键约束与MySQL数据字典一致。完整文件如下：

```sql
DROP TABLE IF EXISTS employee_change_logs;
DROP TABLE IF EXISTS app_users;
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT ck_employees_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE employee_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_change_logs_employee_id
    ON employee_change_logs (employee_id);

CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    employee_id BIGINT UNIQUE,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO employees (
    id, name, department, email, status
) VALUES
    (1001, 'Tanaka', 'Sales', 'tanaka@example.com', 'ACTIVE'),
    (1002, 'Sato', 'Development', 'sato@example.com', 'ACTIVE');

INSERT INTO app_users (
    id, username, password_hash, employee_id, role, enabled
) VALUES
    (1, 'tanaka',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1001, 'USER', TRUE),
    (2, 'sato',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     1002, 'USER', TRUE),
    (3, 'admin',
     '$2a$10$nljJFXRpHlZ2gPsH7lM12eIan96c0Y03Y/YCucFA/SA6z.1Lv7UTC',
     NULL, 'ADMIN', TRUE);
```

这份H2脚本对齐了列长度、非空、唯一和状态值约束，但它不会模拟MySQL的全部行为。例如，H2中的 `updated_at` 不会自动复现MySQL `ON UPDATE CURRENT_TIMESTAMP` 的更新时间逻辑。因此：快速回归使用H2，数据库方言、自动更新时间和生产DDL仍要在MySQL 8.0.16及以上版本的验收库确认。测试替身与生产数据库“关键规则一致”不等于“两者完全相同”。

### 6. 完整的README.md

下面是交付包根目录README的完整模板。尖括号字段必须替换为本次实际值：

```markdown
# Employee Management API 20260915-01

## 1. 交付版本

- 票号：EMP-241
- Git提交：<40位提交ID>
- Java：17
- Spring Boot：3.5.16
- MyBatis Spring Boot Starter：3.0.5
- MySQL：8.0.16+
- 构建时间：<带时区的时间>
- JAR摘要：<64位SHA-256>

## 2. 功能

- 健康检查
- 员工详情、增删改查
- 条件查询、稳定排序和分页
- department与status重复参数多选
- 字段校验、统一响应和统一异常
- 更新员工与变更履历的事务一致性
- 数据库账号登录、Session、CSRF
- ADMIN操作权限和USER本人数据范围
- 请求日志与requestId调查

## 3. 目录

- source：可重建源码
- database：MySQL结构与仅限练习环境的种子数据
- artifact：已通过本次构建的可执行JAR
- operations：生产配置模板、systemd单元和部署手顺
- evidence：测试、接口与部署验收记录

## 4. 构建

在source/employee-management-api执行：

    .\mvnw.cmd clean verify

成功标准：测试全部通过、BUILD SUCCESS，并生成target下的可执行JAR。

## 5. 数据库

由有权限人员先创建专用数据库和最小权限账号。仅在全新验收库执行：

1. database/01_schema_mysql.sql
2. database/02_training_seed_mysql.sql

生产环境禁止执行training seed。脚本不负责删除旧表或保存数据库密码。

## 6. 本机验收运行

设置dev Profile、验收数据库账号和本次发布编号后执行：

    java -jar artifact\employee-management-api-0.0.1-SNAPSHOT.jar

先检查/health，再按evidence/ACCEPTANCE_RECORD.md验证登录、权限、CRUD、分页和异常。

## 7. Linux部署

按operations/DEPLOYMENT.md执行。正式服务使用非root账号和systemd，应用只监听127.0.0.1:8080，外部通过HTTPS反向代理访问。

## 8. 停止与回退

本机前台运行按Ctrl+C正常停止。Linux使用systemctl stop。回退时停止服务、把current切回记录中的上一已知可用release、重新启动并完成四层检查。数据库结构变化不能只靠切回JAR恢复。

## 9. 配置与秘密

配置键见operations模板。真实DB_PASSWORD不进入Git、交付包、截图、命令参数或证据。prod缺少必填配置时应启动失败。

## 10. 已知限制

- Session当前保存在单个应用进程，多实例共享未实现。
- 员工删除为物理删除，数据保留策略需由实际项目确定。
- /health只证明Web入口响应，不检查数据库。
- 当前未引入数据库迁移工具、容器和云部署。

## 11. 支持信息

- 负责人：<姓名或团队>
- 资料位置：<批准的项目位置>
- 未解决问题：<无，或问题编号、影响、负责人和期限>
```

### 7. 完整的RELEASE_NOTES.md

```markdown
# Release 20260915-01

## 变更

- EMP-241：员工列表department、status支持重复参数多选。
- 原单值查询、分页响应和ADMIN权限保持兼容。

## 数据库

- DDL：无变更。
- DML：无生产数据修正。
- 练习环境可使用02_training_seed_mysql.sql建立验收数据。

## 配置与依赖

- 配置键：无新增、无删除。
- 依赖版本：无变更。
- 部署方式：沿用systemd与current链接。

## 测试

- 自动化测试：<实际总数、失败数和结果>
- 接口验收：<证据位置>
- 部署与回退：<证据位置>

## 兼容性

- 旧的单值department、status请求继续支持。
- 响应JSON无变化。

## 已知限制与残留事项

<没有时写“无”；有时写问题编号、影响、负责人和期限>
```

### 8. 完整的ACCEPTANCE_RECORD.md

```markdown
# Employee Management API 验收记录

## 基本信息

| 项目 | 实际值 |
| --- | --- |
| 发布编号 | <release-id> |
| Git提交 | <commit-id> |
| JAR SHA-256 | <hash> |
| 验收人员 | <name> |
| 验收时间 | <timestamp-with-timezone> |
| Java / OS | <actual> |
| 数据库 | MySQL 8.0.16+ / <database-name> |
| Profile | <dev/prod-simulation> |

## 自动化测试

| 命令 | 测试总数 | Failed | Errors | 结果 | 证据 |
| --- | ---: | ---: | ---: | --- | --- |
| .\mvnw.cmd clean verify | <n> | 0 | 0 | PASS | automated-test.txt |

## 功能与异常

| ID | 条件 | 预期 | 实际 | 结果 | 证据 |
| --- | --- | --- | --- | --- | --- |
| HLT-01 | GET /health | 200、OK | <actual> | <PASS/FAIL> | <ref> |
| AUTH-01 | csrf→admin登录→me | 200、同一Session | <actual> | <PASS/FAIL> | <ref> |
| AUTH-02 | 错误密码 | 401、统一消息 | <actual> | <PASS/FAIL> | <ref> |
| PERM-01 | USER查询列表 | 403 | <actual> | <PASS/FAIL> | <ref> |
| PERM-02 | USER本人/他人详情 | 200/403 | <actual> | <PASS/FAIL> | <ref> |
| EMP-01 | ADMIN查询详情 | 200、规定字段 | <actual> | <PASS/FAIL> | <ref> |
| EMP-02 | ADMIN新增员工 | 201、Location、DB新增 | <actual> | <PASS/FAIL> | <ref> |
| EMP-03 | ADMIN修改该员工 | 200、DB更新、履历1条 | <actual> | <PASS/FAIL> | <ref> |
| EMP-04 | ADMIN删除该员工 | 204、再次查询404 | <actual> | <PASS/FAIL> | <ref> |
| SEARCH-01 | 单值筛选 | 200、原行为兼容 | <actual> | <PASS/FAIL> | <ref> |
| SEARCH-02 | 部门和状态多选 | 200、items/total一致 | <actual> | <PASS/FAIL> | <ref> |
| SEARCH-03 | 越界页 | 200、空items、真实total | <actual> | <PASS/FAIL> | <ref> |
| ERR-01 | 非法字段和分页参数 | 400、字段错误 | <actual> | <PASS/FAIL> | <ref> |
| ERR-02 | 重复邮箱 | 409 | <actual> | <PASS/FAIL> | <ref> |
| ERR-03 | 不存在员工 | 404 | <actual> | <PASS/FAIL> | <ref> |
| TX-01 | 履历写入失败 | 员工更新回滚 | <test-result> | <PASS/FAIL> | <ref> |
| LOG-01 | 带/不带X-Request-Id | requestId可追踪 | <actual> | <PASS/FAIL> | <ref> |

## 构建、配置与部署

| ID | 检查 | 预期 | 实际 | 结果 | 证据 |
| --- | --- | --- | --- | --- | --- |
| BLD-01 | 干净源码执行verify | BUILD SUCCESS | <actual> | <PASS/FAIL> | <ref> |
| PKG-01 | java -jar | 脱离IDE启动 | <actual> | <PASS/FAIL> | <ref> |
| CFG-01 | dev/prod配置 | Profile和release-id正确 | <actual> | <PASS/FAIL> | <ref> |
| CFG-02 | prod缺少必填变量 | 启动失败且指出缺失项 | <actual> | <PASS/FAIL> | <ref> |
| DEP-01 | systemd发布 | current、active、8080正确 | <actual> | <PASS/FAIL> | <ref> |
| DEP-02 | 正常停止 | 优雅停止且端口释放 | <actual> | <PASS/FAIL> | <ref> |
| DEP-03 | 回退演练 | 旧版本恢复四层检查 | <actual> | <PASS/FAIL> | <ref> |

## 问题与结论

- 发现问题：<无，或问题编号、现象、影响、证据>
- 临时处置：<无，或处置内容>
- 残留事项：<无，或负责人和期限>
- 最终结论：<ACCEPT / REJECT / CONDITIONAL ACCEPT>
- 结论依据：<未通过项和风险判断，不能只写“整体正常”>
```

### 9. operations目录从哪里取得

operations中的四个文件不是临时凭记忆编写。它们直接使用第18章完成并验证过的最终版本：

| 交付文件 | 第18章对应内容 | 本章复核点 |
| --- | --- | --- |
| `application-prod.yml.template` | 生产Profile完整配置 | 占位符名称与配置清单一致，不含真实秘密 |
| `employee-api.env.template` | 外部环境变量模板 | 只有键、说明和安全占位符 |
| `employee-api.service` | systemd单元完整示例 | 用户、路径、EnvironmentFile和停止方式一致 |
| `DEPLOYMENT.md` | 部署、检查、停止和回退手顺 | 发布编号、JAR名、端口和验收步骤已更新 |

制作交付包时，应从本次获批源码和资料中复制这些文件，再逐项替换发布编号等非秘密信息；不能从某台服务器反向复制可能含真实密码的文件。完整内容和每个首次出现的systemd指令、Linux命令都在[第18章](18_deployment_operations_check.md)讲解，本章只验证它们与最终版本一致。

## 三、完整成果之后再理解每类交付物

| 交付物 | 回答的问题 | 不能替代什么 |
| --- | --- | --- |
| source | 如何重新构建和Review | 不能代替已构建JAR |
| database | 如何建立结构和练习数据 | 不能包含生产密码或擅自清库 |
| artifact | 本次实际发布哪个二进制 | 不能证明源码与JAR对应 |
| operations | 如何配置、启停、检查和回退 | 不能代替环境授权 |
| evidence | 哪些行为被实际验证 | 不能用截图代替可重复步骤 |
| README与release notes | 怎样接手、本次改了什么 | 不能隐藏已知限制 |
| SHA-256清单 | 文件内容是否与记录一致 | 不能证明功能正确或来源可信 |

交付完成要求这些材料互相对应：README中的提交、JAR摘要和发布编号必须与源码、MANIFEST、启动日志及验收记录一致。

## 四、从需求追到实现和证据

最终验收使用追踪表，不靠“我记得做过”：

| 需求 | 主要实现 | 自动化测试 | 手工/运行证据 | 来源章节 |
| --- | --- | --- | --- | --- |
| 请求与分层 | Controller→Service→Mapper | Web与集成测试 | 请求响应 | 2～6 |
| 统一成功失败 | ApiResponse、全局异常 | 400/404/409断言 | 错误JSON | 7～8 |
| MySQL与CRUD | Entity、Mapper、XML | CRUD集成测试 | SQL前后状态 | 9～10 |
| 日志调查 | RequestLoggingFilter、requestId | 日志相关测试/验证 | 脱敏日志 | 11 |
| 回归保护 | Service、Web、数据库测试 | clean verify | 测试报告 | 12 |
| 更新与履历一致 | @Transactional、履历Mapper | 成功提交和失败回滚 | employees与履历 | 13 |
| 筛选与分页 | SearchRequest、动态SQL、PageResponse | 查询集成测试 | items/total/排序 | 14、19 |
| 登录与Session | Security、AuthController、账号Mapper | SessionLoginTest | csrf→login→me→logout | 15 |
| 角色和数据范围 | SecurityConfig、EmployeeAccess | AuthorizationIntegrationTest | USER/ADMIN对照 | 16 |
| 多环境和JAR | Profile、DeploymentProperties、Maven Plugin | 配置绑定测试 | Profile、摘要、java -jar | 17 |
| Linux运行和回退 | systemd、current、外部配置 | 练习机验收 | 四层检查、回退记录 | 18 |
| 改修流程 | EMP-241四文件差分 | 新旧回归用例 | Review与改修报告 | 19 |

某一需求只有代码没有测试，或只有测试没有规格，都不能形成完整对应关系。

## 五、交付者先建立可复现版本

在项目根目录使用PowerShell执行只读状态检查和最终构建：

```powershell
git status --short
git rev-parse HEAD
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

成功后核对JAR：

```powershell
$jar = Get-Item -LiteralPath `
    '.\target\employee-management-api-0.0.1-SNAPSHOT.jar'
$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $jar.FullName
$jar | Select-Object FullName, Length, LastWriteTime
$hash | Select-Object Algorithm, Hash, Path
```

| 命令或对象 | 当前参数 | 可接受的值 | 结果 |
| --- | --- | --- | --- |
| `git rev-parse HEAD` | HEAD | 能解析为提交的引用 | 当前40位提交ID |
| `clean verify` | Maven clean和default生命周期 | 项目支持的Wrapper命令 | 清理旧产物，执行到verify并生成JAR |
| `Get-Item` | JAR路径 | 本次构建实际存在的文件 | FileInfo对象 |
| `Get-FileHash` | SHA256、JAR绝对路径 | PowerShell支持的算法和可读文件 | 哈希算法、摘要和路径 |
| `Select-Object` | 指定属性 | 对象存在的属性名 | 只显示证据需要的字段 |

Maven执行某一生命周期阶段时，会先执行它之前的阶段；`verify` 会经过编译、测试和打包，再执行配置到verify阶段的检查，参见[Maven构建生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)。Spring Boot Maven Plugin生成的可执行JAR可以由 `java -jar` 运行，参见[Spring Boot 3.5 Maven Plugin](https://docs.spring.io/spring-boot/3.5/maven-plugin/index.html)。

工作区不干净时先判断差分归属，不删除或覆盖他人的修改。交付JAR必须来自记录中的提交和这次成功构建，不能从聊天附件、旧target或未知共享目录替换。

## 六、接手者从干净环境重建

验收者使用另一个空目录，从获批代码库取得README记录的精确提交。下面的URL和提交ID必须替换为实际值：

```powershell
git clone '<approved-repository-url>' employee-management-api
Set-Location .\employee-management-api
git checkout --detach '<40-character-commit-id>'
git status --short
git rev-parse HEAD
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

`git clone` 在新目录取得仓库；`checkout --detach` 让验收者停在精确提交而不是随分支继续变化。该目录只做接收验收，不在detached HEAD上开发。`git status --short` 应没有输出，提交ID必须与README一致。

如果接手者必须询问“缺哪个配置”“先运行哪个脚本”或“哪个JAR才是新的”，交付资料尚不完整。记录阻塞点并退回修正文档，不靠口头答案掩盖缺口。

## 七、在全新验收数据库初始化

只使用已批准、确认为空的个人验收库。连接后先执行：

```sql
SELECT DATABASE() AS current_database;
SELECT CURRENT_USER() AS authenticated_account;
SHOW TABLES;
```

预期数据库名称与验收记录一致，`SHOW TABLES` 没有旧业务表。然后依次执行 `01_schema_mysql.sql` 和仅限练习环境的 `02_training_seed_mysql.sql`。完成后验证：

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;

SELECT id, name, department, email, status
FROM employees
ORDER BY id;

SELECT id, username, employee_id, role, enabled
FROM app_users
ORDER BY id;
```

应得到employees、employee_change_logs、app_users三张表，3名员工和3个练习账号。查询账号时不选择 `password_hash`；证据不需要展示它。

脚本故意不写 `DROP TABLE`。它面向全新数据库，重复执行遇到“表已存在”应停止调查，不能为了通过验收擅自删除已有环境。通用SQL继续从[数据库与SQL入门](../../../database/sql/00_database_introduction.md)复习；账号和权限操作必须遵守项目环境的审批规则。

## 八、脱离IDE运行本次JAR

在交付包根目录打开PowerShell，只设置个人验收环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DB_URL = 'jdbc:mysql://localhost:3306/employee_db_acceptance?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Tokyo'
$env:DB_USERNAME = 'employee_app_acceptance'
$env:DB_PASSWORD = '<输入个人验收库密码>'
$env:APP_RELEASE_ID = '20260915-01-acceptance'

java -jar `
    .\artifact\employee-management-api-0.0.1-SNAPSHOT.jar
```

`<输入个人验收库密码>` 必须替换，但不得复制到README、截图或共享记录。启动成功后确认active Profile、release-id、端口和JAR版本；不要只看最后一行。

在第二个PowerShell窗口执行：

```powershell
Invoke-RestMethod -Method Get -TimeoutSec 5 `
    -Uri 'http://127.0.0.1:8080/health'
```

| 参数 | 当前值 | 可接受的值 | 作用 |
| --- | --- | --- | --- |
| `-Method` | Get | 有效HTTP方法 | 发送健康检查GET请求 |
| `-TimeoutSec` | 5 | 大于0的秒数 | 限制等待时间 |
| `-Uri` | 本机health URL | 有效绝对URL | 指定请求目标 |

预期返回 `OK`。这只证明HTTP入口响应；数据库、登录和权限要在下一节验证。

## 九、按业务顺序完成接口验收

HTTP客户端必须保存Cookie，并按第15章先取得CSRF令牌。使用ACCEPTANCE_RECORD逐项执行：

```text
1. health
2. csrf→错误密码401
3. csrf→USER登录→me
4. USER本人详情200、他人详情403、列表403
5. logout→me 401
6. csrf→ADMIN登录→me
7. 列表默认、单值、多值、空结果、越界页、非法参数
8. 新增一名验收专用员工
9. 查询该员工并确认Location和响应字段
10. 修改该员工并确认employee_change_logs增加1条
11. 使用重复邮箱确认409
12. 删除该员工并确认再次查询404
13. logout并确认受保护接口401
```

新增、修改、删除使用同一名验收专用员工，记录其实际ID；不要改动1001～1003基准数据。验收结束时删除该临时员工是业务测试步骤，不直接在数据库执行无条件DELETE。

事务失败回滚、Mapper异常等不适合在共享运行环境人为破坏，使用第13章自动化测试证明。手工验收和自动化测试各有范围，不能互相冒充。

## 十、同时核对HTTP和数据库状态

写接口通过后，用限定主键的只读SQL确认：

```sql
SELECT id, name, department, email, status, created_at, updated_at
FROM employees
WHERE id = <acceptance_employee_id>;

SELECT id, employee_id, action, detail, created_at
FROM employee_change_logs
WHERE employee_id = <acceptance_employee_id>
ORDER BY id;
```

尖括号必须替换为本次接口返回的数字ID，不能拼接外部用户输入。更新成功后员工记录是新值，并有一条 `EMPLOYEE_UPDATED` 履历；删除后employees中没有该ID，履历是否保留按第13章当前设计核对。

接口200但数据库未变化、数据库变化但接口500、履历条数不符都算失败。记录requestId后沿Controller→Service→Mapper→数据库链调查。

## 十一、核对规格、数据字典、代码和测试一致

### 1. 最终字段数据字典

| 业务字段 | MySQL | Java/接口 | 规则 | 公开范围 |
| --- | --- | --- | --- | --- |
| employee.id | BIGINT | Long | 数据库生成、唯一 | 详情和列表 |
| name | VARCHAR(50) | String | 必填、非空白、最多50 | 详情和列表 |
| department | VARCHAR(50) | String | Sales/Development/Support | 详情和列表 |
| email | VARCHAR(100) | String | 必填、邮箱格式、唯一 | 仅详情 |
| status | VARCHAR(20) | String | ACTIVE/INACTIVE | 查询条件；当前响应不公开 |
| created_at/updated_at | DATETIME | LocalDateTime | 数据库生成/更新 | 当前响应不公开 |
| app_users.username | VARCHAR(50) | String | 唯一 | 当前用户响应可返回用户名 |
| password_hash | VARCHAR(100) | String | BCrypt哈希 | 永不返回、永不记录 |
| employee_id | BIGINT nullable | Long | USER用于本人数据范围 | 不由客户端决定权限 |
| role | VARCHAR(30) | String/authority | USER或ADMIN | 只用于服务端授权 |

若测试表把name改成100、email改成255，测试就无法发现超过生产列长度的数据，所以本章提供了与最终数据字典一致的完整test-data.sql。

### 2. 配置清单

| 配置 | dev | test | prod | 是否秘密 |
| --- | --- | --- | --- | --- |
| SPRING_PROFILES_ACTIVE | dev | test注解激活 | prod | 否 |
| DB_URL | 可有默认localhost | H2内存库 | 必填外部值 | 地址通常不公开 |
| DB_USERNAME | 练习账号 | sa | 必填外部值 | 敏感配置 |
| DB_PASSWORD | 必填 | 空测试值 | 必填秘密来源 | 是 |
| APP_LOG_FILE | 默认logs路径 | 测试配置 | 必填外部路径 | 否 |
| APP_ENVIRONMENT_NAME | local-dev | automated-test | 必填 | 否 |
| APP_RELEASE_ID | 必填 | test-suite | 必填 | 否 |
| SERVER_PORT | 默认8080 | 测试随机/配置 | 默认或批准端口 | 否 |

配置名要与YAML占位符、DeploymentProperties、systemd EnvironmentFile和README一致。生产模板只放键和安全占位符，不放真实值。

## 十二、检查不应进入交付的内容

交付前检查已知名称和常见敏感模式：

```powershell
git status --short
git diff --check
rg -n --hidden `
    -g '!target/**' `
    -g '!.git/**' `
    'DB_PASSWORD=|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|JSESSIONID=|XSRF-TOKEN=' `
    .
```

`rg` 只帮助发现候选内容，不证明“没有秘密”。命中后人工判断，不把结果自动删除。还要检查：

- 调试Controller和临时permitAll规则；
- `System.out.println`、断点说明和临时异常；
- 真实内部URL、账号、密码和令牌；
- 未使用配置、旧Mapper XML和重复类；
- `target`、IDE文件、大日志和数据库导出；
- 生产配置中的练习密码与固定测试账号；
- README、release notes与实际差分不一致。

若秘密曾经提交到Git，仅增加 `.gitignore` 或删除当前文件并不能让历史中的秘密失效；应先撤销凭据，再按项目批准流程处理历史。

## 十三、生成并复核交付摘要

对JAR、数据库脚本、运维模板和主要说明生成SHA-256。摘要文件必须由交付流程依据实际文件生成，不能复制本章示例中的占位符：

```text
<sha256>  artifact/employee-management-api-0.0.1-SNAPSHOT.jar
<sha256>  database/01_schema_mysql.sql
<sha256>  database/02_training_seed_mysql.sql
<sha256>  operations/application-prod.yml.template
<sha256>  operations/employee-api.env.template
<sha256>  operations/employee-api.service
<sha256>  operations/DEPLOYMENT.md
<sha256>  README.md
<sha256>  RELEASE_NOTES.md
```

接手者重新计算并逐项比较。摘要不同先停止验收并确认文件来源；摘要相同只证明内容相同，不证明该内容安全、正确或通过测试。

## 十四、部署和回退验收

只有获批的Ubuntu 24.04练习机由有权限人员执行第18章手顺：

```text
核对JAR摘要
→ 安装分版本release和外部配置
→ 服务账号在18080前台验证
→ systemd校验
→ 停旧版、切current、启新版
→ 进程、8080监听、health、正式HTTPS业务四层检查
→ 日志和发布记录
→ 按批准条件执行一次回退演练
```

ACCEPTANCE_RECORD中的DEP-01～03必须指向实际证据。没有Ubuntu/systemd环境时标记“未执行”和原因，不能写PASS；课程最终实操验收则要求补做后才能判定完全通过。

## 十五、由另一位学员进行黑盒接手验收

交付者把包和精确提交交给未参与整理的学员。接手者只使用包内README和手顺完成：

1. 校验摘要。
2. 在空目录取得精确源码版本。
3. 从Wrapper执行clean verify。
4. 在全新验收库执行结构与练习数据脚本。
5. 脱离IDE启动JAR。
6. 完成功能、异常、事务、登录和权限验收。
7. 正常停止应用。
8. 在批准练习机完成部署与回退，或明确记录未执行条件。
9. 填写ACCEPTANCE_RECORD并给出结论。

接手者需要口头补充才能继续时，把问题记录为文档缺陷。交付者修正文档后，接手者从失败步骤重新验证；不能在记录中删除第一次失败。

## 十六、验收结论怎样判断

| 结论 | 条件 | 后续动作 |
| --- | --- | --- |
| ACCEPT | 必须项全部PASS，无阻断残留 | 接受交付并保存版本与证据 |
| REJECT | 构建、数据库、核心业务、安全或回退存在阻断失败 | 修正后重新完整验收 |
| CONDITIONAL ACCEPT | 仅非阻断项未完成，风险、负责人和期限已批准 | 到期跟踪并补充证据 |

下面情况不能判定ACCEPT：

- clean verify失败；
- 源码提交与JAR摘要无法对应；
- 初始化脚本需要现场猜测或破坏旧库；
- 登录、权限或CSRF被绕过；
- CRUD、事务回滚或分页总数不符合规格；
- prod配置包含真实秘密或缺少必填校验；
- 要求部署验收但没有运行与回退证据；
- 存在没有影响、负责人或期限的残留事项。

## 十七、常见验收失败与排查入口

| 现象 | 先确认 | 进入章节 |
| --- | --- | --- |
| 无法构建或JAR不能运行 | Java、Wrapper、pom、Main-Class | 第3、17章 |
| Bean找不到 | 启动类包位置、组件注解、构造器 | 第3、4章 |
| 400或JSON绑定失败 | 输入、DTO、Valid、全局异常 | 第6～8章 |
| 401、403或Session丢失 | Cookie、CSRF、认证和权限矩阵 | 第15～16章 |
| 查询为空或total不对 | 测试数据、共享条件、SQL参数 | 第9、14、19章 |
| 更新后履历不一致 | 事务入口、异常传播、两次写入 | 第13章 |
| 500无法定位 | requestId、最早异常、调用链 | 第11章 |
| JAR与IDE行为不同 | Profile、外部配置、产物来源 | 第17章 |
| systemd active但业务失败 | 四层检查、日志、数据库 | 第18章 |

更完整的症状索引见[Spring Boot常见问题与排查索引](../appendix/troubleshooting_review_index.md)。

## 十八、最终实践任务

### 任务1：制作完整交付包

按本章目录制作一次真实交付包。所有尖括号占位符必须替换，JAR和文档摘要必须可复算，包内不得含秘密、target中间文件和未知来源产物。

### 任务2：交换项目验收

两名学员交换交付包。接手者不得查看对方IDE运行配置，也不接受口头步骤，按README完成构建、数据库、运行、接口和停止。记录每个阻塞点和修正文档后的再验收结果。

### 任务3：执行安全Review

从权限矩阵、CSRF、Session、密码哈希、数据公开范围、日志脱敏、配置秘密和数据库账号权限八个方面检查。至少提交一条“有证据无问题”的结论或一条具体指摘，不能只写“安全OK”。

### 任务4：执行故障恢复演练

在个人环境制造错误数据库地址，保存启动失败证据，恢复配置并完成四层检查。在批准Ubuntu练习机再执行一次JAR回退。不得在共享或生产环境注入故障。

### 任务5：给出最终结论

填写完整ACCEPTANCE_RECORD，列出所有FAIL、未执行项和残留事项，最后根据第十六节标准选择ACCEPT、REJECT或CONDITIONAL ACCEPT并说明依据。

## 十九、课程最终稳定状态

完成后，另一位学员能够只依据交付包，在干净环境取得精确源码、建立验收数据库、运行全部自动化测试、生成并核对可执行JAR、脱离IDE启动、验证健康检查与完整业务、确认登录权限和事务、正常停止，并在获批Linux练习机完成部署与回退。需求、数据字典、配置、源码、测试、产物、手顺和证据之间具有可追踪关系；无法外部验证的事项被明确标为未执行，不会用“启动成功”代替完整交付。
