package com.school.canteen.controller;

import com.school.canteen.enums.OrderStatus;
import com.school.canteen.service.OrderExportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Export Orders" — shared by Canteen Admin and Sub Admin. The generated files only ever
 * contain the columns {@link com.school.canteen.service.impl.OrderExportServiceImpl}
 * hardcodes, sourced from the same field-restricted admin order DTO the list API returns.
 */
@RestController
@RequestMapping("/api/admin/orders/export")
@PreAuthorize("hasAnyRole('CANTEEN_ADMIN','SUB_ADMIN')")
public class OrderExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final OrderExportService orderExportService;

    public OrderExportController(OrderExportService orderExportService) {
        this.orderExportService = orderExportService;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search) {
        byte[] file = orderExportService.exportExcel(date, status, search);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        attachment("orders-" + date + ".xlsx"))
                .body(file);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search) {
        byte[] file = orderExportService.exportPdf(date, status, search);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        attachment("orders-" + date + ".pdf"))
                .body(file);
    }

    private String attachment(String filename) {
        return ContentDisposition.attachment().filename(filename).build().toString();
    }
}
