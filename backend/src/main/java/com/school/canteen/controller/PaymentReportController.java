package com.school.canteen.controller;

import com.school.canteen.dto.payment.PlatformRevenueResponse;
import com.school.canteen.service.PaymentReportService;
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

/** Platform-fee revenue reporting. Canteen-admin only. */
@RestController
@RequestMapping("/api/admin/reports/payments")
@PreAuthorize("hasRole('CANTEEN_ADMIN')")
public class PaymentReportController {

    private final PaymentReportService paymentReportService;

    public PaymentReportController(PaymentReportService paymentReportService) {
        this.paymentReportService = paymentReportService;
    }

    @GetMapping
    public PlatformRevenueResponse revenue() {
        return paymentReportService.revenue();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] file = paymentReportService.exportCsv(from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("payments-" + from + "-to-" + to + ".csv").build().toString())
                .body(file);
    }
}
