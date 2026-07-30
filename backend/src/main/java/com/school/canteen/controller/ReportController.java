package com.school.canteen.controller;

import com.school.canteen.dto.report.DashboardResponse;
import com.school.canteen.dto.report.ExpenseRequest;
import com.school.canteen.dto.report.ExpenseResponse;
import com.school.canteen.dto.report.SalesReportResponse;
import com.school.canteen.service.ReportService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Business reporting. Canteen-admin only — this is the operator's commercial data. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('CANTEEN_ADMIN')")
public class ReportController {

    private final ReportService reportService;
    private final Clock clock;

    public ReportController(ReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.dashboard(date != null ? date : LocalDate.now(clock));
    }

    /** Revenue, cost of goods, overheads and net profit for a period. */
    @GetMapping("/reports/sales")
    public SalesReportResponse sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesReport(from, to);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse addExpense(@Valid @RequestBody ExpenseRequest request) {
        return reportService.addExpense(request);
    }

    @GetMapping("/expenses")
    public List<ExpenseResponse> listExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.listExpenses(from, to);
    }

    @DeleteMapping("/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable UUID id) {
        reportService.deleteExpense(id);
    }
}
