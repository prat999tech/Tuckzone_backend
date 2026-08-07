package com.school.canteen.service.impl;

import com.school.canteen.dto.payment.PlatformRevenueResponse;
import com.school.canteen.dto.payment.PlatformRevenueResponse.BreakdownRow;
import com.school.canteen.entity.Payment;
import com.school.canteen.repository.PaymentRepository;
import com.school.canteen.service.PaymentReportService;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReportServiceImpl implements PaymentReportService {

    private static final String[] CSV_COLUMNS = {
            "id", "createdAt", "useCase", "provider", "status", "subtotal", "platformFee",
            "discount", "tax", "walletUsed", "gatewayAmount", "grandTotal", "currency",
            "providerOrderId", "providerPaymentId"
    };

    private final PaymentRepository paymentRepository;
    private final Clock clock;
    private final ZoneId appZoneId;

    public PaymentReportServiceImpl(PaymentRepository paymentRepository, Clock clock, ZoneId appZoneId) {
        this.paymentRepository = paymentRepository;
        this.clock = clock;
        this.appZoneId = appZoneId;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformRevenueResponse revenue() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        Instant startOfToday = now.toLocalDate().atStartOfDay(appZoneId).toInstant();
        Instant startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay(appZoneId).toInstant();
        Instant epoch = Instant.EPOCH;
        Instant nowInstant = now.toInstant();

        BigDecimal today = paymentRepository.sumPlatformFeeBetween(startOfToday, nowInstant);
        BigDecimal month = paymentRepository.sumPlatformFeeBetween(startOfMonth, nowInstant);
        BigDecimal total = paymentRepository.sumPlatformFeeBetween(epoch, nowInstant);

        List<BreakdownRow> byProvider = paymentRepository.revenueByProviderBetween(epoch, nowInstant).stream()
                .map(row -> new BreakdownRow(String.valueOf(row[0]), (BigDecimal) row[1], (BigDecimal) row[2]))
                .toList();
        List<BreakdownRow> byUseCase = paymentRepository.revenueByUseCaseBetween(epoch, nowInstant).stream()
                .map(row -> new BreakdownRow(String.valueOf(row[0]), (BigDecimal) row[1], (BigDecimal) row[2]))
                .toList();

        return new PlatformRevenueResponse(today, month, total, byProvider, byUseCase);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(appZoneId).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(appZoneId).toInstant();
        List<Payment> payments = paymentRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromInstant, toInstant);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8)) {
            writer.println(String.join(",", CSV_COLUMNS));
            for (Payment p : payments) {
                writer.println(String.join(",",
                        csv(p.getId()), csv(p.getCreatedAt()), csv(p.getUseCase()), csv(p.getProvider()),
                        csv(p.getStatus()), csv(p.getSubtotal()), csv(p.getPlatformFee()), csv(p.getDiscount()),
                        csv(p.getTax()), csv(p.getWalletUsed()), csv(p.getGatewayAmount()), csv(p.getGrandTotal()),
                        csv(p.getCurrency()), csv(p.getProviderOrderId()), csv(p.getProviderPaymentId())));
            }
        }
        return out.toByteArray();
    }

    /** Quotes only when needed (a comma or quote is present) — every field here is either
     *  a number, an enum name, a UUID or a provider id, none of which legitimately contain
     *  a comma, so this stays simple without needing a full CSV-writing dependency. */
    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
