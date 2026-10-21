# 第3章 无模板账票：用 POI 生成従業員一覧表

## 本章定位

本章通过短小代码理解无模板账票。要求能够找到“创建工作簿、循环明细、写出文件”的关键位置，并看懂 Mapper 到 Generator 的数据传递；不要求从零搭建完整生产系统。

| 等级 | 本章要求 |
| --- | --- |
| A：必须理解 | CSV 与 Excel 的差异；Workbook、Sheet、Row、Cell；动态明细流程 |
| B：能看懂和简单修改 | 最小 POI 代码、Mapper → Service → Generator → Controller |
| C：认识即可 | 完整 CSV 转义、安全字符处理、大文件和完整自动化测试 |

## 学习目标

学完后，你能够：

- 说明无模板账票适合什么场景；
- 用最小 POI 示例创建 Excel；
- 解释 POI 核心对象之间的关系；
- 看懂循环生成明细和基本样式代码；
- 说明 MyBatis 查询结果如何传给 Generator；
- 找出 Spring Boot 返回文件的关键处理。

## 真实业务背景

人事管理员需要下载全部在籍员工。现有画面有分页，而账票需要完整输出。従業員一覧表版式简单，主要是标题、表头和动态明细，因此不需要事先准备 Excel 模板。

本章继续使用员工管理项目已有字段：`id`、`name`、`department`、`email`、`status`、`created_at`。

## 1. 无模板账票是什么

无模板表示程序通过 `new XSSFWorkbook()` 创建一个空白工作簿，再依次创建工作表、行和单元格。

适合：

- 従業員一覧表等规则表格；
- 列固定、明细数量变化的一览；
- 不要求复杂印刷版式的内部资料。

不适合：复杂請求書、固定通知书、设计部门频繁调整版式的文件。

## 2. CSV 基础：最简单的文本账票

CSV 是文本，不需要 POI。下面片段只用于理解“逐行输出”，没有覆盖完整引号和换行转义。

```java
// 所属位置：CsvEmployeeReportGenerator 的 write 方法
writer.write("社员ID,姓名,部门\r\n");
for (EmployeeReportRow employee : employees) {
    writer.write(employee.id() + ","
            + employee.name() + ","
            + employee.department() + "\r\n");
}
writer.flush();
```

- **调用者**：Service 或 Batch 文件写出步骤。
- **输入**：`List<EmployeeReportRow>` 和 `Writer`。
- **输出**：CSV 文本。
- **业务环节**：把查询结果转换为交换文件。
- **注意**：正式 CSV 必须处理逗号、双引号、换行、编码和公式注入风险。完整参考见[无模板账票参考代码](reporting-reference/employee-list-reference.md)。

## 3. Apache POI 对象体系

Apache POI 是 Java 读写 Microsoft Office 文件的常用库。本课程使用 `poi-ooxml` 处理 `.xlsx`。

```text
Workbook  整个Excel文件
└─ Sheet  一个工作表
   └─ Row  一行
      └─ Cell  一个单元格
```

| 对象 | 主要方法 | 当前示例作用 |
| --- | --- | --- |
| `XSSFWorkbook` | 构造器、`write()`、`close()` | 新建 `.xlsx` 工作簿 |
| `Sheet` | `createRow()` | 创建标题、表头和明细行 |
| `Row` | `createCell()` | 创建一行中的各列 |
| `Cell` | `setCellValue()` | 写入文本、数字或日期 |
| `CellStyle` | 边框、填充、数字格式 | 统一显示格式 |

`Workbook` 必须关闭；输出流也由创建它的代码关闭。本例统一使用 try-with-resources。

## 4. 创建最简单的 Excel

下面是可独立放入 `main` 方法运行的核心代码。它只创建一个工作表和一个单元格。

```java
Path output = Path.of("employee-list.xlsx");

try (XSSFWorkbook workbook = new XSSFWorkbook();
     OutputStream stream = Files.newOutputStream(output)) {
    Sheet sheet = workbook.createSheet("従業員一覧");
    Row row = sheet.createRow(0);
    Cell cell = row.createCell(0);
    cell.setCellValue("従業員一覧表");
    workbook.write(stream);
}
```

- **所属类**：可先放在独立实验类 `PoiMinimumExample`；整合后逻辑进入 `EmployeeListReportGenerator`。
- **调用者**：独立实验由 `main` 调用；项目中由 Service 调用 Generator。
- **输入**：输出路径。
- **输出**：包含一个 Sheet 的 XLSX。
- **业务环节**：建立文件结构。
- **关键方法**：`createSheet`、`createRow`、`createCell`、`setCellValue`、`write`。

运行后确认文件存在，并用 Excel 或 LibreOffice 打开，A1 应显示“従業員一覧表”。

## 5. 循环生成动态明细

员工数量不是固定的，所以从第5行开始循环创建明细。

```java
// 所属类：EmployeeListReportGenerator
int rowIndex = 4; // Java从0开始，4表示Excel第5行
for (EmployeeReportRow employee : employees) {
    Row row = sheet.createRow(rowIndex++);
    row.createCell(0).setCellValue(employee.id());
    row.createCell(1).setCellValue(employee.name());
    row.createCell(2).setCellValue(employee.department());
    row.createCell(3).setCellValue(employee.email());
    row.createCell(4).setCellValue(statusLabel(employee.status()));
    row.createCell(5).setCellValue(employee.createdAt());
}
```

- **调用者**：Generator 的 `write` 方法。
- **输入**：按设计书排序的员工列表。
- **输出**：每名员工对应一行。
- **业务环节**：动态明细输出。
- **常见错误**：把 Excel 行号直接当 Java 下标；循环中一直写同一行；字段列号与设计书不一致。

数据对象保持简单：

```java
public record EmployeeReportRow(
        Long id,
        String name,
        String department,
        String email,
        String status,
        LocalDateTime createdAt) {
}
```

## 6. 基本格式和样式

日期不能只依靠 `toString()`。把日期作为日期单元格写入，并设置显示格式：

```java
CellStyle dateStyle = workbook.createCellStyle();
dateStyle.setDataFormat(workbook.getCreationHelper()
        .createDataFormat().getFormat("yyyy/mm/dd hh:mm"));

Cell createdAtCell = row.createCell(5);
createdAtCell.setCellValue(employee.createdAt());
createdAtCell.setCellStyle(dateStyle);
```

样式在循环外创建并复用。若每一行都创建新 `CellStyle`，文件会变大，还可能超过工作簿样式数量限制。

常用的基本设置：

```java
sheet.createFreezePane(0, 4);      // 冻结前4行
sheet.setColumnWidth(0, 12 * 256); // 固定列宽
sheet.setColumnWidth(1, 20 * 256);
```

边框、颜色、筛选和打印设置可在需要时增加，不是理解主流程的前提。

## 7. Mapper 怎样把数据交给 Generator

### Mapper 接口片段

```java
// 所属类：EmployeeReportMapper
List<EmployeeReportRow> selectForReport(
        @Param("department") String department,
        @Param("status") String status);
```

### MyBatis XML 片段

```xml
SELECT id, name, department, email, status,
       created_at AS createdAt
FROM employees
WHERE status = #{status}
<if test="department != null and department != ''">
    AND department = #{department}
</if>
ORDER BY department, name, id
```

- **调用者**：`EmployeeReportService`。
- **输入**：部门和状态。
- **输出**：`List<EmployeeReportRow>`。
- **业务环节**：数据抽取，不负责 Excel。
- **注意**：使用 `#{}` 参数绑定；字段名、别名和 record 组件必须一致。

## 8. Spring Boot 调用 Generator

Service 连接“查询”和“生成”两个动作：

```java
// 所属类：EmployeeReportService
@Transactional(readOnly = true)
public void writeExcel(EmployeeReportCondition condition,
        OutputStream output) throws IOException {
    List<EmployeeReportRow> employees = mapper.selectForReport(
            condition.department(), condition.status());
    generator.write(condition, employees, output);
}
```

- **调用者**：`EmployeeReportController`。
- **输入**：输出条件和 HTTP 输出流。
- **输出**：Generator 向输出流写入 XLSX。
- **业务环节**：组织一次在线账票生成。
- **注意**：Service 不操作具体单元格，Generator 不查询数据库。

Controller 只保留 HTTP 责任：

```java
// 所属类：EmployeeReportController
@GetMapping("/employees.xlsx")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<StreamingResponseBody> download(
        @RequestParam(required = false) String department,
        @RequestParam String status) {
    EmployeeReportCondition condition =
            new EmployeeReportCondition(department, status);
    StreamingResponseBody body = output ->
            service.writeExcel(condition, output);

    return ResponseEntity.ok()
            .contentType(XLSX_MEDIA_TYPE)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=employee-list.xlsx")
            .body(body);
}
```

该片段省略了 UTF-8 文件名构建和统一异常处理，完整写法见[参考代码](reporting-reference/employee-list-reference.md)。认证和角色来源复用第15～16章既有 JWT 机制。

## 9. 完整处理流程

```mermaid
flowchart LR
    A[管理员请求下载] --> B[Controller接收条件]
    B --> C[Service组织处理]
    C --> D[Mapper查询MySQL]
    D --> E[List<EmployeeReportRow>]
    E --> F[Generator创建Workbook]
    F --> G[循环写入明细]
    G --> H[workbook.write]
    H --> I[浏览器收到XLSX]
```

## 实际项目使用方式

在既有员工项目中，账票功能通常作为 `report` 包加入，不重写登录、员工 CRUD 或通用 MyBatis 教学。阅读代码时先找到 Controller 路径，再沿 Service、Mapper 和 Generator 追踪。

完整参考实现保存在：

- [従業員一覧表参考代码](reporting-reference/employee-list-reference.md)
- [参考代码说明](reporting-reference/README.md)

这些文件用于查阅和教师演示，不要求新人记住每个样式或安全辅助方法。

## 常见错误与注意事项

- 忘记 `workbook.write(output)`，文件大小为 0。
- 忘记关闭 Workbook 或文件流。
- 使用 `getRow()` 读取不存在的行后直接调用，出现 `NullPointerException`。
- Java 行下标从 0 开始，Excel 显示行号从 1 开始。
- 把数字、日期全部写成字符串，后续无法正确排序或计算。
- Mapper 没有稳定排序，生成文件每次顺序可能变化。
- 正式 CSV 未处理逗号、引号、换行和前导公式字符。

## 理解练习

1. 在最小 Excel 示例中，哪一行代码真正把内存中的工作簿写入文件？
2. `rowIndex = 4` 为什么表示 Excel 第5行？
3. 找出 Mapper、Service 和 Generator 各自的输入与输出。
4. 若要增加“登记时间”列，需要检查设计书、SQL、record、Generator 中哪些位置？
5. 下面错误代码会发生什么？怎样修正？

```java
Row row = sheet.getRow(20);
row.createCell(0).setCellValue("test");
```

## 本章总结

无模板 Excel 的核心顺序是：创建 Workbook → 创建 Sheet → 创建 Row → 创建 Cell → 写值 → `workbook.write()`。Spring Boot 中，Mapper 提供数据，Service 组织处理，Generator 负责文件内容，Controller 负责下载响应。
