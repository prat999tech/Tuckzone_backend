package com.school.canteen.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** At-a-glance view of one day's trading. */
public record DashboardResponse(
        LocalDate date,
        long totalOrders,
        long pendingOrders,
        long completedOrders,
        long rejectedOrders,
        long cancelledOrders,
        BigDecimal revenue,
        BigDecimal costOfGoods,
        BigDecimal grossProfit,
        BigDecimal expenses,
        BigDecimal netProfit,
        long totalCustomers,
        List<TopItemRow> topItems,
        List<LowStockRow> lowStock) {
}
