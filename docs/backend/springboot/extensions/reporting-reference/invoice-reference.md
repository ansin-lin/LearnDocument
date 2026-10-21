# 有模板請求書参考实现

本参考实现对应[第4章](../04_report_with_template.md)。它展示模板校验、动态插行、样式复制、公式和 Spring Boot 调用。新人只需理解用途，不要求逐行实现。

## 1. 模板契约

资源路径：`src/main/resources/report-templates/invoice-v1.xlsx`。

| 位置 | 内容 |
| --- | --- |
| A1:E1 | 請求書标题 |
| B2 | 請求番号 |
| B3 | 発行日 |
| E3 | 支払期限 |
| B5 | 請求先 |
| A10:E10 | `No.`、`摘要`、`数量`、`単価`、`金額` |
| A11:E11 | 明细原型行 |
| D12:E12 | 小計 |
| D13:E13 | 消費税 |
| D14:E14 | 合計 |

当前仓库没有提供该二进制模板；必须先按契约创建模板，代码才能运行。

## 2. MySQL 教学表

請求書使用独立的教学表，不修改现有 `employees` 表：

```sql
CREATE TABLE invoices (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  invoice_no VARCHAR(30) NOT NULL,
  customer_name VARCHAR(100) NOT NULL,
  issue_date DATE NOT NULL,
  payment_due_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  UNIQUE KEY uk_invoices_invoice_no (invoice_no),
  CONSTRAINT ck_invoices_status
    CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'CANCELLED'))
);

CREATE TABLE invoice_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  description VARCHAR(200) NOT NULL,
  quantity DECIMAL(10, 2) NOT NULL,
  unit_price DECIMAL(12, 0) NOT NULL,
  UNIQUE KEY uk_invoice_items_line (invoice_id, line_no),
  CONSTRAINT fk_invoice_items_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);
```

这是 MySQL 8.0.16+ 教学 DDL。只能在个人练习库或受控迁移中执行；不要在共享环境手工建表。

## 3. 数据对象

```java
package com.example.employee.report.domain;

import java.time.LocalDate;

public record InvoiceHeader(
        Long invoiceId,
        String invoiceNo,
        String customerName,
        LocalDate issueDate,
        LocalDate paymentDueDate,
        String status) {
}
```

```java
package com.example.employee.report.domain;

import java.math.BigDecimal;

public record InvoiceItemRow(
        Long itemId,
        int lineNo,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice) {
    public BigDecimal amount() {
        return quantity.multiply(unitPrice);
    }
}
```

```java
package com.example.employee.report.domain;

import java.util.List;

public record InvoiceReportData(
        InvoiceHeader header, List<InvoiceItemRow> items) {
    public InvoiceReportData {
        items = List.copyOf(items);
    }
}
```

## 4. Mapper

```java
package com.example.employee.report.mapper;

import com.example.employee.report.domain.InvoiceHeader;
import com.example.employee.report.domain.InvoiceItemRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvoiceReportMapper {
    InvoiceHeader selectHeader(@Param("invoiceId") Long invoiceId);
    List<InvoiceItemRow> selectItems(@Param("invoiceId") Long invoiceId);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.report.mapper.InvoiceReportMapper">
    <select id="selectHeader"
            resultType="com.example.employee.report.domain.InvoiceHeader">
        SELECT id AS invoiceId,
               invoice_no AS invoiceNo,
               customer_name AS customerName,
               issue_date AS issueDate,
               payment_due_date AS paymentDueDate,
               status
        FROM invoices
        WHERE id = #{invoiceId}
    </select>

    <select id="selectItems"
            resultType="com.example.employee.report.domain.InvoiceItemRow">
        SELECT id AS itemId,
               line_no AS lineNo,
               description,
               quantity,
               unit_price AS unitPrice
        FROM invoice_items
        WHERE invoice_id = #{invoiceId}
        ORDER BY line_no ASC, id ASC
    </select>
</mapper>
```

## 5. 完整 Generator

```java
package com.example.employee.report.file;

import com.example.employee.report.domain.InvoiceItemRow;
import com.example.employee.report.domain.InvoiceReportData;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class InvoiceTemplateReportGenerator {
    private static final String SHEET_NAME = "請求書";
    private static final int DETAIL_START_ROW = 10;
    private static final int TEMPLATE_SUBTOTAL_ROW = 11;
    private static final int LAST_TEMPLATE_ROW = 17;
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    public void write(InputStream template,
            InvoiceReportData data,
            OutputStream output) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(template)) {
            Sheet sheet = requireSheet(workbook);
            validateTemplate(sheet);
            writeFixedFields(sheet, data);
            Layout layout = prepareRows(sheet, data.items().size());
            writeItems(sheet, data.items());
            writeTotals(sheet, layout, data.items());
            configurePrint(workbook, sheet, layout.lastPrintRow());
            workbook.setForceFormulaRecalculation(true);
            workbook.write(output);
        }
    }

    private static Sheet requireSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            throw new IllegalStateException("模板缺少工作表: " + SHEET_NAME);
        }
        return sheet;
    }

    private static void validateTemplate(Sheet sheet) {
        requireText(sheet, 1, 0, "請求番号");
        requireText(sheet, 2, 0, "発行日");
        requireText(sheet, 2, 3, "支払期限");
        requireText(sheet, 4, 0, "請求先");
        requireText(sheet, 9, 0, "No.");
        requireText(sheet, 9, 1, "摘要");
        requireText(sheet, 9, 2, "数量");
        requireText(sheet, 9, 3, "単価");
        requireText(sheet, 9, 4, "金額");
        requireText(sheet, 11, 3, "小計");
        requireText(sheet, 12, 3, "消費税");
        requireText(sheet, 13, 3, "合計");
        if (sheet.getRow(DETAIL_START_ROW) == null) {
            throw new IllegalStateException("模板缺少明细原型行");
        }
    }

    private static void requireText(Sheet sheet, int rowIndex,
            int columnIndex, String expected) {
        Row row = sheet.getRow(rowIndex);
        Cell cell = row == null ? null : row.getCell(columnIndex);
        String actual = cell == null ? "" : cell.getStringCellValue();
        if (!expected.equals(actual)) {
            throw new IllegalStateException("模板标签不一致: " + expected);
        }
    }

    private static void writeFixedFields(Sheet sheet,
            InvoiceReportData data) {
        setText(sheet, 1, 1, safeText(data.header().invoiceNo()));
        sheet.getRow(2).getCell(1).setCellValue(data.header().issueDate());
        sheet.getRow(2).getCell(4)
                .setCellValue(data.header().paymentDueDate());
        setText(sheet, 4, 1, safeText(data.header().customerName()));
    }

    private static Layout prepareRows(Sheet sheet, int itemCount) {
        int visibleRows = Math.max(1, itemCount);
        int extraRows = visibleRows - 1;
        if (extraRows > 0) {
            sheet.shiftRows(TEMPLATE_SUBTOTAL_ROW, LAST_TEMPLATE_ROW,
                    extraRows, true, false);
            Row prototype = sheet.getRow(DETAIL_START_ROW);
            for (int offset = 1; offset < visibleRows; offset++) {
                copyRowStyle(prototype,
                        sheet.createRow(DETAIL_START_ROW + offset));
            }
        }
        clearValues(sheet.getRow(DETAIL_START_ROW));
        int subtotalRow = TEMPLATE_SUBTOTAL_ROW + extraRows;
        return new Layout(visibleRows, subtotalRow,
                subtotalRow + 1, subtotalRow + 2,
                LAST_TEMPLATE_ROW + extraRows);
    }

    private static void writeItems(Sheet sheet,
            List<InvoiceItemRow> items) {
        for (int index = 0; index < items.size(); index++) {
            InvoiceItemRow item = items.get(index);
            Row row = sheet.getRow(DETAIL_START_ROW + index);
            row.getCell(0).setCellValue(item.lineNo());
            setText(row.getCell(1), safeText(item.description()));
            row.getCell(2).setCellValue(item.quantity().doubleValue());
            row.getCell(3).setCellValue(item.unitPrice().doubleValue());
            row.getCell(4).setCellFormula("C" + (row.getRowNum() + 1)
                    + "*D" + (row.getRowNum() + 1));
        }
    }

    private static void writeTotals(Sheet sheet, Layout layout,
            List<InvoiceItemRow> items) {
        int firstExcelRow = DETAIL_START_ROW + 1;
        int lastExcelRow = DETAIL_START_ROW + layout.visibleRows();
        sheet.getRow(layout.subtotalRow()).getCell(4)
                .setCellFormula("SUM(E" + firstExcelRow
                        + ":E" + lastExcelRow + ")");
        sheet.getRow(layout.taxRow()).getCell(4)
                .setCellFormula("ROUND(E" + (layout.subtotalRow() + 1)
                        + "*10%,0)");
        sheet.getRow(layout.totalRow()).getCell(4)
                .setCellFormula("E" + (layout.subtotalRow() + 1)
                        + "+E" + (layout.taxRow() + 1));

        BigDecimal subtotal = items.stream()
                .map(InvoiceItemRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(TAX_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);
        if (total.compareTo(subtotal.add(tax)) != 0) {
            throw new IllegalStateException("合计计算不一致");
        }
    }

    private static void configurePrint(XSSFWorkbook workbook,
            Sheet sheet, int lastPrintRow) {
        PrintSetup print = sheet.getPrintSetup();
        print.setPaperSize(PrintSetup.A4_PAPERSIZE);
        print.setLandscape(false);
        print.setFitWidth((short) 1);
        print.setFitHeight((short) 0);
        sheet.setFitToPage(true);
        sheet.setRepeatingRows(CellRangeAddress.valueOf("10:10"));
        workbook.setPrintArea(workbook.getSheetIndex(sheet),
                0, 4, 0, lastPrintRow);
    }

    private static void copyRowStyle(Row source, Row target) {
        target.setHeight(source.getHeight());
        for (int column = 0; column < 5; column++) {
            Cell sourceCell = source.getCell(column,
                    Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Cell targetCell = target.createCell(column, CellType.BLANK);
            CellStyle style = sourceCell.getCellStyle();
            targetCell.setCellStyle(style);
        }
    }

    private static void clearValues(Row row) {
        for (int column = 0; column < 5; column++) {
            row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .setBlank();
        }
    }

    private static void setText(Sheet sheet, int row, int column,
            String value) {
        setText(sheet.getRow(row).getCell(column), value);
    }

    private static void setText(Cell cell, String value) {
        cell.setCellValue(value == null ? "" : value);
    }

    private static String safeText(String value) {
        if (value == null) return "";
        String trimmed = value.stripLeading();
        return !trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0
                ? "'" + value : value;
    }

    private record Layout(int visibleRows, int subtotalRow,
            int taxRow, int totalRow, int lastPrintRow) {
    }
}
```

`writeTotals` 中的 Java 金额用于业务核对；Excel 公式用于显示。真实项目应在测试中比较 Java 期待值与公式计算结果。

## 6. Service

```java
package com.example.employee.report.service;

import com.example.employee.report.domain.InvoiceHeader;
import com.example.employee.report.domain.InvoiceItemRow;
import com.example.employee.report.domain.InvoiceReportData;
import com.example.employee.report.file.InvoiceTemplateReportGenerator;
import com.example.employee.report.mapper.InvoiceReportMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceReportService {
    private static final String TEMPLATE =
            "classpath:report-templates/invoice-v1.xlsx";

    private final InvoiceReportMapper mapper;
    private final ResourceLoader resourceLoader;
    private final InvoiceTemplateReportGenerator generator;

    public InvoiceReportService(InvoiceReportMapper mapper,
            ResourceLoader resourceLoader,
            InvoiceTemplateReportGenerator generator) {
        this.mapper = mapper;
        this.resourceLoader = resourceLoader;
        this.generator = generator;
    }

    @Transactional(readOnly = true)
    public PreparedInvoice prepare(Long invoiceId) {
        InvoiceHeader header = mapper.selectHeader(invoiceId);
        if (header == null) {
            throw new IllegalArgumentException("請求書不存在: " + invoiceId);
        }
        if (!"ISSUED".equals(header.status())
                && !"PAID".equals(header.status())) {
            throw new IllegalStateException("当前状态不能正式输出");
        }
        List<InvoiceItemRow> items = mapper.selectItems(invoiceId);
        String safeNo = header.invoiceNo()
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return new PreparedInvoice("invoice-" + safeNo + ".xlsx",
                new InvoiceReportData(header, items));
    }

    public void write(PreparedInvoice prepared, OutputStream output)
            throws IOException {
        Resource resource = resourceLoader.getResource(TEMPLATE);
        try (InputStream template = resource.getInputStream()) {
            generator.write(template, prepared.data(), output);
        }
    }

    public record PreparedInvoice(
            String filename, InvoiceReportData data) {
    }
}
```

项目应把“未找到”和“状态不可输出”接入已有统一异常处理，而不是依赖异常文字判断 HTTP 状态。

## 7. Controller

```java
package com.example.employee.report.api;

import com.example.employee.report.service.InvoiceReportService;
import com.example.employee.report.service.InvoiceReportService.PreparedInvoice;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/reports/invoices")
public class InvoiceReportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final InvoiceReportService service;

    public InvoiceReportController(InvoiceReportService service) {
        this.service = service;
    }

    @GetMapping("/{invoiceId}.xlsx")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable Long invoiceId) {
        PreparedInvoice prepared = service.prepare(invoiceId);
        StreamingResponseBody body = output ->
                service.write(prepared, output);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(prepared.filename(), StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("no-store");
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
```

## 8. 验证建议

- 0、1、25 条明细分别生成并回读。
- 检查固定坐标、样式复制、公式引用、税额舍入和打印区域。
- 修改一个模板标签，确认模板校验失败。
- 使用目标 Excel 软件人工确认分页和日文字体。

本次文档任务没有创建 `invoice-v1.xlsx`，也没有把代码放入 Maven 工程实际编译，因此不能声明该参考实现已经运行通过。
