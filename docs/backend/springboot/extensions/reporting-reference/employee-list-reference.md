# 无模板従業員一覧表参考实现

本参考实现对应[第3章](../03_report_without_template.md)。课堂只阅读关键片段；这里保留完整类结构，方便在员工管理项目副本中组合。

## 1. Maven 依赖

Spring Boot Parent、Web、Security、MyBatis、MySQL 和测试依赖沿用主线，只增加 POI：

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.5.1</version>
</dependency>
```

## 2. 数据对象

### EmployeeReportCondition.java

```java
package com.example.employee.report.domain;

public record EmployeeReportCondition(String department, String status) {
    public EmployeeReportCondition {
        department = normalize(department);
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new IllegalArgumentException(
                    "status必须是ACTIVE或INACTIVE");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
```

### EmployeeReportRow.java

```java
package com.example.employee.report.domain;

import java.time.LocalDateTime;

public record EmployeeReportRow(
        Long id,
        String name,
        String department,
        String email,
        String status,
        LocalDateTime createdAt) {
}
```

## 3. Mapper

### EmployeeReportMapper.java

```java
package com.example.employee.report.mapper;

import com.example.employee.report.domain.EmployeeReportRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeReportMapper {
    List<EmployeeReportRow> selectForReport(
            @Param("department") String department,
            @Param("status") String status);
}
```

### EmployeeReportMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.employee.report.mapper.EmployeeReportMapper">
    <select id="selectForReport"
            resultType="com.example.employee.report.domain.EmployeeReportRow">
        SELECT
            id,
            name,
            department,
            email,
            status,
            created_at AS createdAt
        FROM employees
        WHERE status = #{status}
        <if test="department != null and department != ''">
            AND department = #{department}
        </if>
        ORDER BY department ASC, name ASC, id ASC
    </select>
</mapper>
```

## 4. CSV Generator

```java
package com.example.employee.report.file;

import com.example.employee.report.domain.EmployeeReportRow;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvEmployeeReportGenerator {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public void write(List<EmployeeReportRow> rows, Writer destination)
            throws IOException {
        BufferedWriter writer = destination instanceof BufferedWriter buffered
                ? buffered : new BufferedWriter(destination);
        writeLine(writer, List.of(
                "社员ID", "姓名", "部门", "邮箱", "状态", "登记时间"));
        for (EmployeeReportRow row : rows) {
            writeLine(writer, List.of(
                    String.valueOf(row.id()), safeText(row.name()),
                    safeText(row.department()), safeText(row.email()),
                    statusLabel(row.status()),
                    row.createdAt() == null
                            ? "" : DATE_TIME.format(row.createdAt())));
        }
        writer.flush();
    }

    private static void writeLine(BufferedWriter writer, List<String> fields)
            throws IOException {
        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) writer.write(',');
            writer.write(escape(fields.get(index)));
        }
        writer.write("\r\n");
    }

    static String escape(String value) {
        String text = safeText(value);
        boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0;
        String escaped = text.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    static String safeText(String value) {
        String text = value == null ? "" : value;
        String leadingRemoved = text.stripLeading();
        if (!leadingRemoved.isEmpty()
                && "=+-@".indexOf(leadingRemoved.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "ACTIVE" -> "在籍";
            case "INACTIVE" -> "退职";
            default -> throw new IllegalArgumentException("未知状态: " + status);
        };
    }
}
```

## 5. Excel Generator

```java
package com.example.employee.report.file;

import com.example.employee.report.domain.EmployeeReportCondition;
import com.example.employee.report.domain.EmployeeReportRow;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class EmployeeListReportGenerator {
    private static final String[] HEADERS =
            {"社员ID", "姓名", "部门", "邮箱", "状态", "登记时间"};
    private static final DateTimeFormatter OUTPUT_TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public void write(EmployeeReportCondition condition,
            List<EmployeeReportRow> employees,
            LocalDateTime generatedAt,
            OutputStream output) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("従業員一覧");
            Styles styles = createStyles(workbook);
            writeTitle(sheet, styles.title());
            writeCondition(sheet, condition, generatedAt, styles.label());
            writeHeader(sheet, styles.header());
            int nextRow = writeDetails(sheet, employees, styles);
            writeCount(sheet, nextRow + 1, employees.size(), styles.label());
            configureSheet(sheet, nextRow);
            workbook.write(output);
        }
    }

    private static void writeTitle(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("従業員一覧表");
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
    }

    private static void writeCondition(Sheet sheet,
            EmployeeReportCondition condition,
            LocalDateTime generatedAt,
            CellStyle style) {
        Row row = sheet.createRow(1);
        Cell cell = row.createCell(0);
        String department = condition.department() == null
                ? "全部门" : condition.department();
        cell.setCellValue("部门: " + department
                + " / 状态: " + condition.status()
                + " / 出力日時: " + OUTPUT_TIME.format(generatedAt));
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));
    }

    private static void writeHeader(Sheet sheet, CellStyle style) {
        Row header = sheet.createRow(3);
        for (int column = 0; column < HEADERS.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(HEADERS[column]);
            cell.setCellStyle(style);
        }
    }

    private static int writeDetails(Sheet sheet,
            List<EmployeeReportRow> employees, Styles styles) {
        int rowIndex = 4;
        for (EmployeeReportRow employee : employees) {
            Row row = sheet.createRow(rowIndex++);
            Cell id = row.createCell(0);
            id.setCellValue(employee.id());
            id.setCellStyle(styles.text());
            setText(row, 1, employee.name(), styles.text());
            setText(row, 2, employee.department(), styles.text());
            setText(row, 3, employee.email(), styles.text());
            setText(row, 4, statusLabel(employee.status()), styles.text());
            Cell date = row.createCell(5);
            if (employee.createdAt() != null) {
                date.setCellValue(employee.createdAt());
            }
            date.setCellStyle(styles.dateTime());
        }
        return rowIndex;
    }

    private static void setText(Row row, int column,
            String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(CsvEmployeeReportGenerator.safeText(value));
        cell.setCellStyle(style);
    }

    private static void writeCount(Sheet sheet, int rowIndex,
            int count, CellStyle style) {
        Cell cell = sheet.createRow(rowIndex).createCell(0);
        cell.setCellValue("合計件数: " + count);
        cell.setCellStyle(style);
    }

    private static void configureSheet(Sheet sheet, int nextRow) {
        int[] widths = {12, 20, 18, 32, 12, 20};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, widths[column] * 256);
        }
        sheet.createFreezePane(0, 4);
        sheet.setAutoFilter(new CellRangeAddress(
                3, Math.max(3, nextRow - 1), 0, 5));
    }

    private static Styles createStyles(XSSFWorkbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.CENTER);

        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle label = workbook.createCellStyle();
        label.setFont(bold);

        CellStyle header = workbook.createCellStyle();
        header.setFont(bold);
        header.setFillForegroundColor(
                IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        setBorders(header);

        CellStyle text = workbook.createCellStyle();
        setBorders(text);

        CellStyle dateTime = workbook.createCellStyle();
        dateTime.cloneStyleFrom(text);
        dateTime.setDataFormat(workbook.getCreationHelper()
                .createDataFormat().getFormat("yyyy/mm/dd hh:mm"));
        return new Styles(title, label, header, text, dateTime);
    }

    private static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "ACTIVE" -> "在籍";
            case "INACTIVE" -> "退职";
            default -> throw new IllegalArgumentException("未知状态: " + status);
        };
    }

    private record Styles(CellStyle title, CellStyle label,
            CellStyle header, CellStyle text, CellStyle dateTime) {
    }
}
```

## 6. Service

```java
package com.example.employee.report.service;

import com.example.employee.report.domain.EmployeeReportCondition;
import com.example.employee.report.domain.EmployeeReportRow;
import com.example.employee.report.file.EmployeeListReportGenerator;
import com.example.employee.report.mapper.EmployeeReportMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeReportService {
    private final EmployeeReportMapper mapper;
    private final EmployeeListReportGenerator generator;
    private final Clock clock;

    public EmployeeReportService(EmployeeReportMapper mapper,
            EmployeeListReportGenerator generator, Clock clock) {
        this.mapper = mapper;
        this.generator = generator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void writeExcel(EmployeeReportCondition condition,
            OutputStream output) throws IOException {
        List<EmployeeReportRow> employees = mapper.selectForReport(
                condition.department(), condition.status());
        generator.write(condition, employees,
                LocalDateTime.now(clock), output);
    }
}
```

项目配置类需要提供 `Clock`：

```java
@Bean
Clock businessClock() {
    return Clock.system(ZoneId.of("Asia/Tokyo"));
}
```

## 7. Controller

```java
package com.example.employee.report.api;

import com.example.employee.report.domain.EmployeeReportCondition;
import com.example.employee.report.service.EmployeeReportService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/reports")
public class EmployeeReportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final EmployeeReportService service;

    public EmployeeReportController(EmployeeReportService service) {
        this.service = service;
    }

    @GetMapping("/employees.xlsx")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(required = false) String department,
            @RequestParam String status) {
        EmployeeReportCondition condition =
                new EmployeeReportCondition(department, status);
        StreamingResponseBody body = output ->
                service.writeExcel(condition, output);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("employee-list.xlsx", StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("no-store");
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
```

## 8. 验证建议

- 生成 0、1、3 条数据并用 POI 重新读取。
- 检查 Sheet 名、表头、明细行数、稳定排序和日期类型。
- 用现有 JWT 测试匿名 401、普通用户 403、管理员 200。
- Mapper 结合测试使用 MySQL；不要用 H2 声称已经验证 MySQL 方言。

本参考文件没有附带 Maven 工程，因此上述代码尚未在本次文档任务中实际编译和运行。
