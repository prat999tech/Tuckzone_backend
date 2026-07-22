package com.school.canteen.service;

import com.school.canteen.dto.wallet.TopupInitResponse;
import com.school.canteen.dto.wallet.TopupRequest;
import com.school.canteen.dto.wallet.MockTopupCompleteRequest;
import com.school.canteen.dto.wallet.VerifyTopupRequest;
import com.school.canteen.dto.wallet.WalletResponse;
import com.school.canteen.dto.wallet.WalletTransactionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {

    WalletResponse getWallet(UUID userId);

    List<WalletTransactionResponse> getTransactions(UUID userId);

    TopupInitResponse initiateTopup(UUID userId, TopupRequest request);

    /** Idempotent: verifies the payment and credits the wallet exactly once. */
    WalletResponse verifyTopup(UUID userId, VerifyTopupRequest request);

    /** Dev-only helper that simulates a successful gateway callback when provider=mock. */
    WalletResponse mockCompleteTopup(UUID userId, MockTopupCompleteRequest request);

    /** Debit under a row lock; throws if the balance would go negative. Used by ordering. */
    WalletResponse debit(UUID userId, BigDecimal amount, String referenceType,
                         String referenceId, String description);

    /** Credit under a row lock. Used by refunds. */
    WalletResponse credit(UUID userId, BigDecimal amount, String referenceType,
                          String referenceId, String description);
}
