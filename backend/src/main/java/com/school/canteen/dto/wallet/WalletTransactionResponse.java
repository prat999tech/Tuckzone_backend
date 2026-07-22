package com.school.canteen.dto.wallet;

import com.school.canteen.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String referenceType,
        String referenceId,
        String description,
        Instant createdAt) {
}
