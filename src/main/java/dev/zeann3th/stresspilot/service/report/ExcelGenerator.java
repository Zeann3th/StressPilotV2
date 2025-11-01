package dev.zeann3th.stresspilot.service.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j(topic = "[Excel Generator]")
@Getter
@Setter
public class ExcelGenerator<T> {

    private static final String FONT_NAME = "Times New Roman";
    private static final int HEADER_FONT_SIZE = 14;
    private static final int BODY_FONT_SIZE = 12;
    private static final int WINDOW_SIZE = 100;

    private final List<T> data;
    private final SXSSFWorkbook workbook;
    private SXSSFSheet sheet;
    private int totalColumns = 0;

    private final ObjectMapper mapper;

    public ExcelGenerator(List<T> data) {
        this.data = data;
        this.workbook = new SXSSFWorkbook(WINDOW_SIZE);
        this.workbook.setCompressTempFiles(true);

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public ExcelGenerator<T> writeHeaderLines(String[] headers) {
        sheet = workbook.createSheet("Sheet1");

        Row row = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) HEADER_FONT_SIZE);
        font.setFontName(FONT_NAME);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int col = 0;
        createCell(row, col++, "No", style);
        for (String header : headers) {
            createCell(row, col++, header, style);
        }

        this.totalColumns = headers.length + 1;
        return this;
    }

    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);

        switch (value) {
            case null -> cell.setCellValue("");
            case Number num -> {
                long longVal = num.longValue();
                if (isPossibleEpoch(longVal)) {
                    LocalDateTime ldt = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(longVal), ZoneId.systemDefault());
                    cell.setCellValue(ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } else {
                    cell.setCellValue(num.doubleValue());
                }
            }
            case Boolean bool -> cell.setCellValue(bool);
            case Date date -> cell.setCellValue(date);
            case LocalDateTime ldt -> cell.setCellValue(ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            default -> cell.setCellValue(value.toString());
        }

        cell.setCellStyle(style);
    }

    private boolean isPossibleEpoch(long value) {
        long start = 946684800000L;
        long end = 4102444800000L;
        return value >= start && value <= end;
    }

    public ExcelGenerator<T> writeDataLines(String[] fields) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) BODY_FONT_SIZE);
        font.setFontName(FONT_NAME);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        int rowCount = 1;
        for (T obj : data) {
            Row row = sheet.createRow(rowCount);
            int colCount = 0;

            createCell(row, colCount++, rowCount, style);

            Map<String, Object> map = mapper.convertValue(obj, new TypeReference<>() {});
            for (String field : fields) {
                createCell(row, colCount++, map.get(field), style);
            }

            rowCount++;

            if (rowCount % WINDOW_SIZE == 0) {
                try {
                    sheet.flushRows(WINDOW_SIZE / 2);
                } catch (IOException e) {
                    log.error("Error flushing rows to disk", e);
                }
            }
        }

        return this;
    }

    public void export(HttpServletResponse response) throws IOException {
        sheet.trackAllColumnsForAutoSizing();
        for (int col = 0; col < totalColumns; col++) {
            sheet.autoSizeColumn(col);
            int width = sheet.getColumnWidth(col);
            sheet.setColumnWidth(col, Math.min(width + 512, 255 * 256));
        }

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
        } finally {
            workbook.close();
        }
    }
}