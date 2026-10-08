# A11 Java Web项目中的日期时间与时区

第9章为了接收数据库时间列，在 `Employee` Entity中使用了 `LocalDateTime`。这一选择只解决当前项目的本地业务时间映射；真实项目还必须分清“日历日期”“本地时间”“UTC瞬间”和“带偏移的接口时间”。

开始前应完成第9章，能够说明MySQL列、Entity属性和Mapper结果映射的关系。本附录不修改Employee主线代码和数据库表。

## 一、先确定业务要表达什么

选择时间类型前，先回答：这个值是日期、每天固定时刻、某地区看到的本地时间，还是世界时间轴上的唯一瞬间？不能先选择一个看起来方便的类型，再猜测它代表什么。

| Java类型 | 示例 | 包含的信息 | 常见用途 |
| --- | --- | --- | --- |
| `LocalDate` | `2026-09-16` | 日期，无时间、无时区 | 生日、营业日、开始日期 |
| `LocalTime` | `09:30:00` | 一天中的时间，无日期、无时区 | 每日营业时间 |
| `LocalDateTime` | `2026-09-16T09:30:00` | 日期和时间，无时区 | 已明确业务地区的本地业务时间 |
| `OffsetDateTime` | `2026-09-16T09:30:00+09:00` | 日期、时间和UTC偏移 | API中携带偏移的时间 |
| `Instant` | `2026-09-16T00:30:00Z` | 时间轴上的唯一瞬间 | 事件发生时刻、跨时区系统交换 |

这些类型都来自Java 17的 `java.time` 包。`LocalDateTime` 不保存时区，不能在没有额外offset或zone信息时确定唯一瞬间，参见[Java 17 LocalDateTime API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDateTime.html)。

```text
2026-09-16T09:30:00+09:00  带日本标准时间偏移
2026-09-16T00:30:00Z       同一瞬间的UTC表示
2026-09-16T09:30:00        只有本地日期时间，无法单独确定瞬间
```

## 二、UTC、固定偏移和区域时区

- `UTC` 是协调世界时基准，Java中常用 `ZoneOffset.UTC`；
- `+09:00` 是固定offset，只表示比UTC快9小时，不包含地区规则；
- `Asia/Tokyo` 是区域时区ID，由 `ZoneId` 表示，能够根据日期取得该地区适用的offset规则。

当前日本标准时间通常是 `+09:00`，但区域时区和固定offset在概念上仍不同。其他区域可能使用夏令时，同一个 `ZoneId` 在不同日期对应不同offset。跨区域系统应保存明确的时间语义，不能看到 `09:30:00` 就默认是日本时间。

## 三、转换时必须补充缺少的信息

下面是独立Java片段，展示如何明确指定“这个本地时间按东京规则解释”：

```java
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

ZoneId tokyo = ZoneId.of("Asia/Tokyo");
LocalDateTime local = LocalDateTime.of(2026, 9, 16, 9, 30);

Instant instant = local.atZone(tokyo).toInstant();
OffsetDateTime offsetDateTime = instant.atZone(tokyo).toOffsetDateTime();
LocalDateTime restoredLocal = LocalDateTime.ofInstant(instant, tokyo);
```

`ZoneId.of(...)` 取得区域时区规则；`atZone(tokyo)` 给无时区的本地时间补充解释前提；`toInstant()` 得到唯一瞬间。反向转换时，`ofInstant(instant, tokyo)` 必须再次指定希望观察哪个地区的本地时间。

不能把任意 `LocalDateTime` 直接当作UTC或东京时间。转换前必须从接口规格、数据库定义或业务规则确认它原本代表什么。

## 四、从Browser到数据库逐段确认

```text
Browser中的日期时间
  → HTTP JSON字符串及offset约定
  → Jackson解析为Java时间类型
  → Service按业务时区转换或校验
  → MyBatis/JDBC绑定参数
  → MySQL DATE、DATETIME或TIMESTAMP
```

| 阶段 | 常见问题 | 调查证据 |
| --- | --- | --- |
| Browser→JSON | 浏览器本地时区、格式或offset丢失 | Network中的原始请求 |
| JSON→Java | 类型与文本格式不匹配 | 400响应、Jackson异常、DTO类型 |
| Service | 把无时区值错误解释为UTC | 业务规格、转换代码、测试时区 |
| JDBC→DB | Java类型与列类型不一致 | Mapper参数、JDBC URL、表定义 |
| DB→查询结果 | 连接时区或列类型语义不同 | `session time_zone`、原始列值 |

A07中的 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 只规定JSON文本格式，不会为 `LocalDateTime` 增加时区。需要表达offset时，应在接口规格中采用可携带offset的ISO-8601形式，并选择 `OffsetDateTime` 等对应类型，而不是只改变显示格式。

## 五、MySQL DATETIME与TIMESTAMP

MySQL `DATETIME` 保存日期和时间字段，不像 `TIMESTAMP` 那样按照连接时区与UTC进行存取转换。具体行为见[MySQL 8.0日期时间类型说明](https://dev.mysql.com/doc/refman/8.0/en/datetime.html)。

Employee项目把 `created_at`、`updated_at` 定义为日本业务环境中的数据库本地时间，并映射成 `LocalDateTime`。跨国家事件、审计时间或多地区API通常应先确定统一的UTC/Instant策略，再明确显示时区。

## 六、验证与Review任务

1. 写出 `2026-09-16T09:30:00+09:00` 对应的UTC表示；
2. 说明为什么 `2026-09-16T09:30:00` 不能独立证明一个瞬间；
3. 沿Browser→JSON→Java→JDBC→MySQL列出Employee创建时间的类型和格式；
4. 把测试进程时区临时设为UTC，检查依赖系统默认时区的断言是否失败，随后恢复原设置；
5. Review一个通过字符串拼接 `+09:00` 的转换方案，指出应由 `ZoneId`、`OffsetDateTime` 或 `Instant` 处理的部分。

任何时间列类型或时间语义变更，都必须先调查既有数据、MyBatis映射、JSON规格、服务器/JVM/数据库时区和回归测试。本附录完成后不应留下Employee表或主线代码改动。
