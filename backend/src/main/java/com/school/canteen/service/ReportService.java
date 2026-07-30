package com.school.canteen.service;

import com.school.canteen.dto.report.DashboardResponse;
import com.school.canteen.dto.report.ExpenseRequest;
import com.school.canteen.dto.report.ExpenseResponse;
import com.school.canteen.dto.report.SalesReportResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Business reporting for the canteen operator. */
public interface ReportService {

    DashboardResponse dashboard(LocalDate date);

    SalesReportResponse salesReport(LocalDate from, LocalDate to);

    ExpenseResponse addExpense(ExpenseRequest request);

    List<ExpenseResponse> listExpenses(LocalDate from, LocalDate to);

    void deleteExpense(UUID id);
}
