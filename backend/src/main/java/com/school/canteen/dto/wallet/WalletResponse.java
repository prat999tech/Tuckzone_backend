package com.school.canteen.dto.wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID userId,
        BigDecimal balance,
        String currency) {
}
