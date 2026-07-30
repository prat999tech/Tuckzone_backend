package com.school.canteen.dto.report;

/** An item close to selling out, so the canteen can restock before it does. */
public record LowStockRow(
        String itemName,
        int remainingQuantity,
        int totalQuantity) {
}
