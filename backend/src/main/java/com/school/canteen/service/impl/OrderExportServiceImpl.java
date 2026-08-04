package com.school.canteen.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.school.canteen.dto.order.AdminOrderResponse;
import com.school.canteen.dto.order.OrderItemResponse;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.service.OrderExportService;
import com.school.canteen.service.OrderService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Excel/PDF generation for the "Export Orders" feature. Columns are hardcoded to exactly
 * the set the spec allows — there is no code path here that can add a new column without a
 * source-level change, which is what keeps this feature from ever leaking a field the JSON
 * API doesn't already expose.
 */
@Service
public class OrderExportServiceImpl implements OrderExportService {

    private static final List<String> COLUMNS = List.of(
            "Student Name", "Class/Section", "Roll Number", "Ordered Items",
            "Quantity", "Order Total", "Order Status", "Payment Status",
            "Order Date", "Order Time");

    private final OrderService orderService;
    private final Clock clock;

    public OrderExportServiceImpl(OrderService orderService, Clock clock) {
        this.orderService = orderService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel(LocalDate date, OrderStatus status, String search) {
        List<AdminOrderResponse> orders = orderService.adminListForExport(date, status, search);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Orders");
            CreationHelper helper = workbook.getCreationHelper();

            CellStyle headerStyle = boldHeaderStyle(workbook);
            CellStyle currencyStyle = dataFormatStyle(workbook, helper, "₹#,##0.00");
            CellStyle dateStyle = dataFormatStyle(workbook, helper, "dd-mm-yyyy");
            CellStyle timeStyle = dataFormatStyle(workbook, helper, "hh:mm AM/PM");

            Row header = sheet.createRow(0);
            for (int col = 0; col < COLUMNS.size(); col++) {
                Cell cell = header.createCell(col);
                cell.setCellValue(COLUMNS.get(col));
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (AdminOrderResponse order : orders) {
                Row row = sheet.createRow(rowNum++);
                LocalDateTime placedAt = order.createdAt().atZone(clock.getZone()).toLocalDateTime();

                row.createCell(0).setCellValue(nullToDash(order.studentName()));
                row.createCell(1).setCellValue(classSection(order));
                row.createCell(2).setCellValue(nullToDash(order.studentRollNumber()));
                row.createCell(3).setCellValue(itemsSummary(order.items()));
                row.createCell(4).setCellValue(totalQuantity(order.items()));

                Cell totalCell = row.createCell(5);
                totalCell.setCellValue(order.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);

                row.createCell(6).setCellValue(order.status().name());
                row.createCell(7).setCellValue(order.paymentStatus().name());

                Cell dateCell = row.createCell(8);
                dateCell.setCellValue(placedAt);
                dateCell.setCellStyle(dateStyle);

                Cell timeCell = row.createCell(9);
                timeCell.setCellValue(placedAt);
                timeCell.setCellStyle(timeStyle);
            }

            for (int col = 0; col < COLUMNS.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate orders spreadsheet", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(LocalDate date, OrderStatus status, String search) {
        List<AdminOrderResponse> orders = orderService.adminListForExport(date, status, search);

        Document document = new Document(PageSize.A4.rotate(), 24, 24, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            PageNumberEvent pageEvent = new PageNumberEvent();
            writer.setPageEvent(pageEvent);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            Paragraph title = new Paragraph("TuckZone", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String exportedAt = LocalDateTime.now(clock)
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            Paragraph subtitle = new Paragraph(
                    "Orders export for " + date + "  —  generated " + exportedAt, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(16f);
            document.add(subtitle);

            document.add(buildTable(orders));

            int totalQuantity = orders.stream()
                    .mapToInt(order -> totalQuantity(order.items()))
                    .sum();
            Paragraph summary = new Paragraph(
                    "Total Orders: " + orders.size() + "   |   Total Items Ordered: " + totalQuantity,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            summary.setSpacingBefore(12f);
            document.add(summary);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate orders PDF", e);
        }
    }

    private PdfPTable buildTable(List<AdminOrderResponse> orders) throws DocumentException {
        PdfPTable table = new PdfPTable(COLUMNS.size());
        table.setWidthPercentage(100);
        table.setWidths(new float[] {14, 10, 8, 22, 7, 9, 9, 9, 8, 8});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String column : COLUMNS) {
            PdfPCell cell = new PdfPCell(new Paragraph(column, headerFont));
            cell.setBackgroundColor(new Color(0xF9, 0x73, 0x16)); // matches the app's --primary
            cell.setPadding(5f);
            table.addCell(cell);
        }

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        for (AdminOrderResponse order : orders) {
            LocalDateTime placedAt = order.createdAt().atZone(clock.getZone()).toLocalDateTime();
            addCell(table, nullToDash(order.studentName()), bodyFont);
            addCell(table, classSection(order), bodyFont);
            addCell(table, nullToDash(order.studentRollNumber()), bodyFont);
            addCell(table, itemsSummary(order.items()), bodyFont);
            addCell(table, String.valueOf(totalQuantity(order.items())), bodyFont);
            addCell(table, "₹" + order.totalAmount().toPlainString(), bodyFont);
            addCell(table, order.status().name(), bodyFont);
            addCell(table, order.paymentStatus().name(), bodyFont);
            addCell(table, placedAt.format(dateFmt), bodyFont);
            addCell(table, placedAt.format(timeFmt), bodyFont);
        }
        return table;
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private String classSection(AdminOrderResponse order) {
        if (order.studentClass() == null) {
            return "-";
        }
        String section = (order.studentSection() == null) ? "" : "-" + order.studentSection();
        return order.studentClass() + section;
    }

    private String itemsSummary(List<OrderItemResponse> items) {
        return items.stream()
                .map(item -> item.itemName() + " x" + item.quantity())
                .collect(Collectors.joining(", "));
    }

    private int totalQuantity(List<OrderItemResponse> items) {
        return items.stream().mapToInt(OrderItemResponse::quantity).sum();
    }

    private String nullToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private CellStyle boldHeaderStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle dataFormatStyle(XSSFWorkbook workbook, CreationHelper helper, String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(helper.createDataFormat().getFormat(format));
        return style;
    }

    /** Standard OpenPDF "Page X of Y" idiom: the running page number is drawn immediately,
     *  the final total is drawn into a reserved template once the document closes. */
    private static final class PageNumberEvent extends PdfPageEventHelper {
        private PdfTemplate totalPagesTemplate;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(50, 16);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException e) {
                throw new IllegalStateException("Failed to load PDF page-number font", e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte content = writer.getDirectContent();
            String text = "Page " + writer.getPageNumber() + " of ";
            float textWidth = baseFont.getWidthPoint(text, 8);
            float x = document.right() - textWidth - 50;
            float y = document.bottom() - 18;

            content.beginText();
            content.setFontAndSize(baseFont, 8);
            content.setTextMatrix(x, y);
            content.showText(text);
            content.endText();
            content.addTemplate(totalPagesTemplate, x + textWidth, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(baseFont, 8);
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber()));
            totalPagesTemplate.endText();
        }
    }
}
