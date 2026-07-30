package com.school.canteen.dto.report;

/** Order volume by hour of day, used to staff the kitchen at the right times. */
public record PeakHourRow(
        int hour,
        long orders) {
}
