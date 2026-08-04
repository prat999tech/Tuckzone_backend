package com.school.canteen.service;

import com.school.canteen.enums.OrderStatus;
import java.time.LocalDate;

/**
 * Generates the "Export Orders" files (Excel and PDF). Both formats read the exact same
 * field-restricted {@link com.school.canteen.dto.order.AdminOrderResponse} rows that the
 * admin order-list API returns, so an exported file can never contain a field (phone,
 * email, wallet, address, etc.) that the JSON API itself does not expose.
 */
public interface OrderExportService {

    byte[] exportExcel(LocalDate date, OrderStatus status, String search);

    byte[] exportPdf(LocalDate date, OrderStatus status, String search);
}
