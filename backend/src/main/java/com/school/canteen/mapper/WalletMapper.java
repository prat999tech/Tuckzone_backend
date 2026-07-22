package com.school.canteen.mapper;

import com.school.canteen.config.PaymentProperties;
import com.school.canteen.dto.wallet.WalletResponse;
import com.school.canteen.dto.wallet.WalletTransactionResponse;
import com.school.canteen.entity.Wallet;
import com.school.canteen.entity.WalletTransaction;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    private final PaymentProperties paymentProperties;

    public WalletMapper(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
    }

    public WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getUser().getId(),
                wallet.getBalance(),
                paymentProperties.currency());
    }

    public WalletTransactionResponse toTransactionResponse(WalletTransaction tx) {
        return new WalletTransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getReferenceType(),
                tx.getReferenceId(),
                tx.getDescription(),
                tx.getCreatedAt());
    }
}
