# 账票处理：从业务规格到可安全下载的文件

“账票”在日本 IT 项目中常指按确定的版式和业务规则输出的一覧、明细、集计或凭证。它不是“把数据库查出的对象直接转 Excel”。先确认使用者、用途、期间、字段、排序、合计、空数据、格式、权限、交付与保存期限；否则文件能打开，也可能是业务错误。本章沿用[扩展入口](index.md)的员工项目，先做小数据 CSV，再做 Excel，最后解释大文件与 PDF 的选择。

## 1. 用可验收的规格开始

示例名称：“月度员工变更一覧”。输入 `month=YYYY-MM`，只允许管理员申请。数据来自 `employee_change_logs`；一条履历一行。期间为日本业务本地时间当月第一天 00:00（含）到次月第一天 00:00（不含）。按 `created_at ASC, id ASC` 排序。列为“履历ID、员工ID、操作、变更内容、发生时间”；另写出“期间、生成时间、总件数”。没有履历时仍生成带表头、总件数 0 的文件。若要显示员工姓名，必须规定员工后来改名/删除时用“当前姓名”还是“变更当时姓名”；当前履历没有历史姓名，本例**不擅自添加**。

对账样例：9 月 1 日 00:00 一条、9 月 30 日 23:59:59 一条、10 月 1 日 00:00 一条，9 月账票应仅有前两条。两条相同 `created_at` 用 ID 确定顺序。验收者可用同一过滤条件核对数据库 count 与文件明细件数。格式版本记为 `v1`；变更列名或顺序需更新规格和回归样例。

## 2. CSV：先把内容规则做对

CSV 是文本，适合机器交换与小型账票，不保留字体、合并单元格等视觉版式。约定 UTF-8、逗号分隔、CRLF 行结束、首行列名。包含逗号、引号或换行的字段必须整体加双引号，内部引号写成两个双引号。不能用 `String.join(",", fields)` 代替合规编码。若文件会被 Excel 打开，用户可控文本以 `=`, `+`, `-`, `@` 等开头可能被解释为公式；在输出适配层按约定转义，并把该行为写入规格。不要把未经处理的履历 `detail` 写进公式单元格。可参考 [OWASP CSV Injection](https://owasp.org/www-community/attacks/CSV_Injection)。

最小练习：从三条**虚构**履历生成 CSV，其中 `detail` 分别包含普通文字、逗号、双引号/换行。用 CSV 解析器重新读回，逐字段与原值比较；再按组织规定用目标表格软件打开，确认公式样例不会执行。输出到个人练习目录，不将含真实个人数据的文件提交仓库。大数据时采用流式写入，不先把所有行装进 `List`。

## 3. Excel：用 POI 创建明确的单元格

Excel 适合有标题、列宽、日期格式、冻结表头或多工作表的交付。使用 Apache POI 的 `poi-ooxml`；本练习在 `pom.xml` 固定 5.5.1，不依赖传递依赖偶然提供：

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.5.1</version>
</dependency>
```

升级时核对[官方发布页](https://poi.apache.org/download.html)并运行文件回归。小型文件用 `XSSFWorkbook`，大文件改用 `SXSSFWorkbook`（滚动窗口会使用磁盘临时文件，必须关闭并清理）。不要把完整原始数据加载到内存后才开始建表。

最小示例在已完成权限确认和数据筛选后运行，`rows` 是最多 3 条的练习数据：

```java
try (XSSFWorkbook book = new XSSFWorkbook()) {
    Sheet sheet = book.createSheet("员工变更");
    Row header = sheet.createRow(0);
    String[] names = {"履历ID", "员工ID", "操作", "变更内容", "发生时间"};
    for (int c = 0; c < names.length; c++)
        header.createCell(c).setCellValue(names[c]);

    int index = 1;
    for (ChangeRow item : rows) {
        Row row = sheet.createRow(index++);
        row.createCell(0).setCellValue(item.logId());
        row.createCell(1).setCellValue(item.employeeId());
        row.createCell(2).setCellValue(safeText(item.action()));
        row.createCell(3).setCellValue(safeText(item.detail()));
        row.createCell(4).setCellValue(item.changedAt().toString());
    }
    sheet.createFreezePane(0, 1);
    try (OutputStream out = Files.newOutputStream(target,
            StandardOpenOption.CREATE_NEW)) {
        book.write(out);
    }
}
```

这里 `ChangeRow` 是只含五个账票字段的记录，`target` 是已验证的个人输出路径。`safeText` 对可能被表格软件当作公式的文本加前缀并按项目规格记录转换；建议对不可信字段统一按文本处理并测试目标表格软件。数字 ID 超过表格软件精确整数范围时应按文本写出，避免尾数变化。正式版可使用日期样式而非字符串，但要明确时区、单元格格式和比较方式；不能在一处输出日本本地时间、另一处用服务器默认时区。用 POI 重新打开生成文件，检查 sheet 名称、单元格值、行数和顺序，而不只检查文件存在。

本例的 `safeText` 仅负责空值与前导公式字符；POI 的 `setCellValue(String)` 必须保持字符串单元格，绝不调用 `setCellFormula`。CSV 的转义与前导字符规则另按上一节实现并测试，不能把此方法当成完整 CSV 编码器：

```java
private static String safeText(String value) {
    if (value == null) return "";
    String trimmed = value.stripLeading();
    if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0)
        return "'" + value;
    return value;
}
```

有公司模板时，先冻结模板版本和字段映射，使用 POI 打开模板后只填指定单元格；不要对未知宏或外部链接启用执行。模板改版需有样例比对。样式对象应复用，避免为每一行创建新 CellStyle 导致工作簿限制或内存问题。

## 4. 大文件与文件生命周期

XLSX 单工作表有行数上限（1,048,576 行，含表头）。超出时应按规格拆分工作表、拆分文件或改用 CSV；不能截断后报告成功。`SXSSFWorkbook` 只保留有限窗口中的行，可控制内存，但不支持任意回头修改已刷出的行。先计算总数、标题和摘要的放置位置；失败时关闭 workbook、清理临时文件与未发布文件。POI 的临时文件可能包含敏感数据，运行目录权限和磁盘容量属于部署检查。

生成流程建议：先写同目录临时文件 `report-<id>.part`，写完关闭并重新打开校验，再原子移动到最终文件名；只在成功后把 MySQL 元数据标记为 `COMPLETED`。文件系统与 MySQL 事务不能自动一起提交：若移动成功但数据库更新失败，需有恢复/清理流程。持久化 `report_id`、月份、格式版本、文件名、字节数、SHA-256、创建时间、过期时间和状态；不要保存用户传入的绝对路径。定期清理只针对已过期且未被引用的文件，先核对目标位于专用账票目录内，保留审计记录。备份和保留期限依组织数据政策决定。

## 5. 下载接口的安全边界

`GET /reports/{id}/file` 应先认证，再在 Service 检查“管理员或该账票允许的申请者”权限，并确认账票已完成。数据库按 ID 找到服务器生成的文件名；不要接受请求参数 `path=../../...` 或直接拼接用户提供的路径。解析后的绝对路径必须仍位于配置的专用账票目录，文件存在且大小/校验值符合记录。返回 `Content-Type`（XLSX 为 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`；CSV 为 `text/csv`）和安全的 `Content-Disposition: attachment` 文件名。不存在返回 404、无权访问返回 403、未完成返回约定的 409，不把实际磁盘路径和堆栈泄露给客户端。敏感账票设置 `Cache-Control: no-store`，访问记录保留用户、报表 ID、时间和结果，不记录文件正文。

文件由服务端流式返回；不要把整个大文件读成 `byte[]`。下载时文件可能被清理，需协调“下载中”和过期清理。API 测试至少覆盖匿名、普通员工、管理员、完成/未完成/过期、不存在、非法 ID、路径穿越尝试以及文件损坏。

## 6. PDF 何时加入

PDF 适合固定版式、打印和签章要求；不是 Excel 的“更高级版本”。PDF 需求应另列页尺寸、字体嵌入、换页、页眉页脚、日文/中文字符、数字格式和打印样张。可选 Apache PDFBox 或受控模板引擎；先用官方示例生成一页，再核对字体授权、字形、换页与搜索/复制文字。不要先把 Excel 截图拼成 PDF；图片式 PDF 难检索、文件大。没有 PDF 交付需求时，本扩展只保留此判断，不增加生产依赖。

## 7. 阶段练习

完成 CSV 边界字段读回；生成 0、3、1000 条 XLSX，重新打开并与 SQL 件数/顺序核对；模拟写盘失败，确认无 `COMPLETED` 元数据和无可下载残留；验证权限矩阵与路径防护。留下稳定状态：已规定的账票版本、可测试的文件生成器和下载规则。Batch 负责采集、文件组件负责表现与交付；下一章才将两者串联。

参考：[Apache POI Spreadsheet](https://poi.apache.org/components/spreadsheet/)、[POI HOWTO](https://poi.apache.org/components/spreadsheet/how-to.html)、[Spring MVC 文件响应](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)。
