package com.school.canteen.dto.order;

import java.util.UUID;

/**
 * Aggregated demand for one item on one date — the view the canteen uses to decide how
 * much to cook and whether to add stock before reopening ordering.
 *
 * @param shortfall how many units are already ordered beyond what was stocked; positive
 *                  means the admin must add stock or those orders cannot be fulfilled
 */
public record DemandRow(
        UUID menuItemId,
        String itemName,
        long orderedQuantity,
        int totalQuantity,
        int remainingQuantity,
        long shortfall) {
}
