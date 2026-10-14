# A03-2 使用DevTools加快本地开发

Spring Boot DevTools是一组本地开发辅助功能。它可以在Eclipse重新编译代码后自动重启Spring应用，并调整部分适合开发环境的默认配置，从而减少反复手动停止、启动应用的操作。

DevTools不是应用运行的必要依赖，也不能代替编译、自动化测试、Postman验证和正式打包。本附录使用第3章的 `/health` 接口做一个可恢复实验；完成后可以按团队约定保留或移除依赖。

## 一、DevTools主要解决什么问题

没有DevTools时，修改Java代码后的基本流程是：

```text
保存代码
  → Eclipse编译
  → 手动停止应用
  → 重新运行启动类
  → 再次验证接口
```

加入DevTools后，本地开发流程可以变成：

```text
保存代码
  → Eclipse编译并更新classpath
  → DevTools检测到变化
  → 自动重启Spring应用上下文
  → 再次验证接口
```

DevTools常用能力及边界如下：

| 能力 | 当前作用 | 边界 |
| --- | --- | --- |
| 自动重启 | classpath中的已编译内容变化后重启应用 | 必须先由IDE完成编译；只保存但没有更新classpath时不会触发 |
| 开发期属性默认值 | 调整模板缓存、静态资源缓存等开发期设置，便于观察修改 | 这些是开发辅助设置，不能据此推断生产配置 |
| 条件评估变化日志 | 重启时显示自动配置条件发生了哪些变化 | 信息较多时可关闭，但应先确认是否仍需要排查自动配置 |
| LiveReload | 曾用于资源变化后通知浏览器刷新 | 从Spring Boot 4.1.0起已弃用，本课程不把它作为开发主流程 |

自动重启不是修改一行代码后继续使用原来的所有对象。DevTools使用两个类加载器：第三方JAR等稳定内容由base classloader加载，正在开发的项目类由restart classloader加载；重启时丢弃并重新建立restart classloader，因此通常比完整冷启动更快。

应用上下文会重新创建，Bean也会重新初始化。内存中的临时状态、尚未完成的请求和本地缓存不能假设会保留；数据库中的已提交数据则不会因为DevTools重启而自动恢复或删除。

## 二、添加Maven依赖

在 `pom.xml` 的 `<dependencies>` 内追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

各项含义：

| 项目 | 当前值 | 可接受的值 | 默认或必填性 | 作用 |
| --- | --- | --- | --- | --- |
| `groupId` | `org.springframework.boot` | Maven坐标中有效的组织标识 | 必填 | 指定Spring Boot组件组 |
| `artifactId` | `spring-boot-devtools` | 当前需要的组件名称 | 必填 | 引入DevTools开发辅助模块 |
| `version` | 不单独填写 | 与课程Spring Boot基线兼容的版本 | 由Spring Boot父POM管理 | 与Spring Boot 4.1.1保持一致 |
| `optional` | `true` | `true`、`false` | 默认false，本例明确设为true | 防止其他模块依赖本项目时自动传递DevTools |

`optional=true` 不表示当前工程运行时无法使用DevTools；它主要控制依赖传递。Spring Boot Maven Plugin默认也会把DevTools排除在重新打包的可执行归档之外，不需要为了本地自动重启修改打包插件设置。

保存 `pom.xml` 后，在Eclipse中右键项目，选择 **Maven → Update Project...**，更新依赖。DevTools由Spring Boot父POM管理版本，不要单独复制不一致的版本号。

## 三、在Eclipse验证自动重启

### 1. 确认自动编译

打开Eclipse的 **Project** 菜单，确认 **Build Automatically** 已勾选。DevTools监视的是更新后的classpath，不是编辑器中尚未编译的源码文本；Eclipse保存Java文件并完成编译后，才会触发重启检查。

### 2. 启动当前工程

打开启动类 `EmployeeManagementApiApplication`，选择 **Run As → Java Application**。安装了Spring Tools时也可以使用 **Run As → Spring Boot App**。

启动完成后，用Postman确认当前接口：

| 项目 | 设置或预期 |
| --- | --- |
| HTTP方法 | GET |
| URL | `http://localhost:8080/health` |
| Query参数 | 无 |
| 请求体 | 无 |
| 预期状态码 | 200 OK |
| 预期响应体 | `OK` |

### 3. 修改代码并观察重启

打开 `HealthController.java`，只把返回值临时改为：

```java
@GetMapping("/health")
public String health() {
    return "DEVTOOLS_OK";
}
```

保存文件，不手动停止应用。观察Eclipse Console，预期看到应用重新启动以及新的 `Started EmployeeManagementApiApplication` 日志。重启期间接口可能暂时无法访问，这是正常的本地实验现象。

再次用Postman发送相同的GET请求，预期状态码仍为200，响应体变为 `DEVTOOLS_OK`。这证明“保存→编译→检测classpath变化→自动重启→新代码生效”的流程已经成立。

验证结束后，把返回值恢复为 `OK` 并再次保存，确认自动重启后响应也恢复。不要把 `DEVTOOLS_OK` 留在主线工程。

## 四、常用设置怎样选择

多数项目先使用默认设置，不要在还没有问题时复制大量DevTools配置。确有需要时，可以在本地配置中使用：

```yaml
spring:
  devtools:
    add-properties: true
    restart:
      enabled: true
      log-condition-evaluation-delta: true
```

| 配置项 | 可接受的值 | 默认值 | 作用与选择方式 |
| --- | --- | --- | --- |
| `spring.devtools.add-properties` | `true`、`false` | `true` | 是否应用DevTools提供的开发期属性默认值；需要完全按项目显式配置调查时才考虑关闭 |
| `spring.devtools.restart.enabled` | `true`、`false` | `true` | 是否监视变化并自动重启；与特定库发生类加载问题时可临时关闭比较 |
| `spring.devtools.restart.log-condition-evaluation-delta` | `true`、`false` | `true` | 是否在重启时记录自动配置条件变化；确认日志噪声无助于调查后才关闭 |

如果只想增加不触发重启的资源范围或使用trigger file，应先阅读项目手顺和[Spring Boot DevTools官方文档](https://docs.spring.io/spring-boot/reference/using/devtools.html)，不要在新人主线中预先加入复杂监视规则。

## 五、没有自动重启时怎样排查

按下面顺序确认：

1. `pom.xml` 中是否存在 `spring-boot-devtools`，Eclipse是否已经更新Maven工程；
2. 应用是否从Eclipse开发classpath启动，而不是只运行已经打包的JAR；
3. **Project → Build Automatically** 是否启用，保存后是否产生新的 `.class` 文件；
4. Console中是否出现DevTools或重启相关日志；
5. 是否设置了 `spring.devtools.restart.enabled=false`；
6. 关闭自动重启后问题是否消失，以判断是否存在双类加载器兼容问题。

不要看到接口仍返回旧结果就连续保存多次。先确认Eclipse是否编译成功，再区分“没有编译”“没有触发重启”“重启失败”和“请求到了另一个仍在运行的进程”。

## 六、生产环境边界

- DevTools只用于开发辅助，不能作为生产运行能力。
- 使用 `java -jar` 运行完整打包应用时，DevTools通常会自动禁用；重新打包的归档默认也不包含它。
- 不要在生产环境设置 `-Dspring.devtools.restart.enabled=true` 强制启用。
- 不启用远程DevTools。远程更新会扩大攻击面，课程部署流程使用正式构建、发布、验证和回退手顺。
- 自动重启成功不代表测试通过。提交代码前仍要执行对应自动化测试，并按接口规格用Postman或测试代码验证结果。

Spring Boot 4.1已经弃用LiveReload且没有替代功能，本课程不要求安装浏览器LiveReload扩展，也不围绕它设计页面开发流程。

## 七、完成检查

完成本附录后，应能够：

1. 说明DevTools是开发期辅助依赖，不是Spring Boot运行的必要条件；
2. 解释为什么保存源码后还需要IDE更新classpath；
3. 用Eclipse完成一次自动重启并用Postman验证新代码已经生效；
4. 在自动重启失败时按编译、依赖、配置和类加载器逐步排查；
5. 说明为什么不能在生产环境强制启用DevTools。
