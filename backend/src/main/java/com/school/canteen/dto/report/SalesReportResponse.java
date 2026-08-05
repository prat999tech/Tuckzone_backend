package com.school.canteen.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Profit and loss for a period.
 *
 * grossProfit = revenue - cost of goods sold.
 * netProfit   = grossProfit - recorded overheads, i.e. what the canteen actually keeps.
 */
public record SalesReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal revenue,
        BigDecimal costOfGoods,
        BigDecimal grossProfit,
        BigDecimal expenses,
        BigDecimal netProfit,
        long orderCount,
        List<DailySalesRow> daily,
        List<TopItemRow> topItems,
        List<TopItemRow> topDailyItems,
        List<TopItemRow> topFixedItems,
        List<PeakHourRow> peakHours) {
}
