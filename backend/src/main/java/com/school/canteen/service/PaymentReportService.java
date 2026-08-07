package com.school.canteen.service;

import com.school.canteen.dto.payment.PlatformRevenueResponse;
import java.time.LocalDate;

/** Platform-fee revenue reporting — separate from ReportService, which reports on the
 *  restaurant's own sales/cost/profit and has no notion of payment provider or fee. */
public interface PaymentReportService {

    PlatformRevenueResponse revenue();

    /** CSV ledger of every payment attempt (any status) created within [from, to]. */
    byte[] exportCsv(LocalDate from, LocalDate to);
}
