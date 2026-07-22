package com.school.canteen.service.impl;

import com.school.canteen.config.PaymentProperties;
import com.school.canteen.dto.wallet.MockTopupCompleteRequest;
import com.school.canteen.dto.wallet.TopupInitResponse;
import com.school.canteen.dto.wallet.TopupRequest;
import com.school.canteen.dto.wallet.VerifyTopupRequest;
import com.school.canteen.dto.wallet.WalletResponse;
import com.school.canteen.dto.wallet.WalletTransactionResponse;
import com.school.canteen.entity.User;
import com.school.canteen.entity.Wallet;
import com.school.canteen.entity.WalletTopup;
import com.school.canteen.entity.WalletTransaction;
import com.school.canteen.enums.TopupStatus;
import com.school.canteen.enums.TransactionType;
import com.school.canteen.exception.ApiException;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.InsufficientBalanceException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.WalletMapper;
import com.school.canteen.payment.GatewayOrder;
import com.school.canteen.payment.PaymentGateway;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.repository.WalletRepository;
import com.school.canteen.repository.WalletTopupRepository;
import com.school.canteen.repository.WalletTransactionRepository;
import com.school.canteen.service.WalletService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTopupRepository walletTopupRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentProperties paymentProperties;
    private final WalletMapper walletMapper;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository walletTransactionRepository,
                             WalletTopupRepository walletTopupRepository,
                             UserRepository userRepository,
                             PaymentGateway paymentGateway,
                             PaymentProperties paymentProperties,
                             WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletTopupRepository = walletTopupRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.paymentProperties = paymentProperties;
        this.walletMapper = walletMapper;
    }

    @Override
    @Transactional
    public WalletResponse getWallet(UUID userId) {
        return walletMapper.toWalletResponse(getOrCreateWallet(userId));
    }

    @Override
    @Transactional
    public List<WalletTransactionResponse> getTransactions(UUID userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return walletTransactionRepository.findByWallet_IdOrderByCreatedAtDesc(wallet.getId())
                .stream()
                .map(walletMapper::toTransactionResponse)
                .toList();
    }

    @Override
    @Transactional
    public TopupInitResponse initiateTopup(UUID userId, TopupRequest request) {
        Wallet wallet = getOrCreateWallet(userId);
        long amountPaise = request.amount().movePointRight(2).longValueExact();
        GatewayOrder order = paymentGateway.createOrder(amountPaise, "topup_" + wallet.getId());

        WalletTopup topup = new WalletTopup();
        topup.setWallet(wallet);
        topup.setAmount(request.amount());
        topup.setGatewayOrderId(order.orderId());
        topup.setStatus(TopupStatus.CREATED);
        walletTopupRepository.save(topup);

        return new TopupInitResponse(
                topup.getId(),
                order.orderId(),
                request.amount(),
                order.currency(),
                order.keyId());
    }

    @Override
    @Transactional
    public WalletResponse verifyTopup(UUID userId, VerifyTopupRequest request) {
        // Locked read serializes concurrent verifications of the same top-up.
        WalletTopup topup = walletTopupRepository.lockByGatewayOrderId(request.gatewayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Top-up not found"));

        if (!topup.getWallet().getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This top-up does not belong to you");
        }
        // Already credited: return current state without crediting again (idempotent).
        if (topup.getStatus() == TopupStatus.PAID) {
            return walletMapper.toWalletResponse(topup.getWallet());
        }
        if (topup.getStatus() == TopupStatus.FAILED) {
            throw new BadRequestException("This top-up has already failed; start a new one");
        }

        boolean genuine = paymentGateway.verifyPayment(
                request.gatewayOrderId(), request.gatewayPaymentId(), request.signature());
        if (!genuine) {
            // Left CREATED (retryable). A real "payment.failed" webhook would mark FAILED.
            throw new BadRequestException("Payment verification failed");
        }

        topup.setGatewayPaymentId(request.gatewayPaymentId());
        topup.setStatus(TopupStatus.PAID);
        applyLocked(topup.getWallet().getId(), TransactionType.CREDIT, topup.getAmount(),
                "TOPUP", topup.getId().toString(), "Wallet top-up");

        return walletMapper.toWalletResponse(topup.getWallet());
    }

    @Override
    @Transactional
    public WalletResponse mockCompleteTopup(UUID userId, MockTopupCompleteRequest request) {
        if (!"mock".equalsIgnoreCase(paymentProperties.provider())) {
            throw new BadRequestException("Mock top-up completion is available only in mock mode");
        }

        String paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "");
        String signature = hmacSha256Hex(
                request.gatewayOrderId() + "|" + paymentId,
                paymentProperties.keySecret());

        return verifyTopup(userId, new VerifyTopupRequest(
                request.gatewayOrderId(),
                paymentId,
                signature));
    }

    @Override
    @Transactional
    public WalletResponse debit(UUID userId, BigDecimal amount, String referenceType,
                                String referenceId, String description) {
        Wallet wallet = getOrCreateWallet(userId);
        WalletTransaction tx = applyLocked(wallet.getId(), TransactionType.DEBIT, amount,
                referenceType, referenceId, description);
        return walletMapper.toWalletResponse(tx.getWallet());
    }

    @Override
    @Transactional
    public WalletResponse credit(UUID userId, BigDecimal amount, String referenceType,
                                 String referenceId, String description) {
        Wallet wallet = getOrCreateWallet(userId);
        WalletTransaction tx = applyLocked(wallet.getId(), TransactionType.CREDIT, amount,
                referenceType, referenceId, description);
        return walletMapper.toWalletResponse(tx.getWallet());
    }

    // --- helpers ---------------------------------------------------------------

    /**
     * The single choke point for any balance change. It re-loads the wallet WITH A ROW LOCK
     * so that concurrent callers on the same wallet are serialized: each sees the other's
     * committed balance, never a stale one. Writes the ledger entry in the same transaction.
     */
    private WalletTransaction applyLocked(UUID walletId, TransactionType type, BigDecimal amount,
                                          String referenceType, String referenceId,
                                          String description) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        Wallet wallet = walletRepository.lockById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        BigDecimal newBalance = (type == TransactionType.CREDIT)
                ? wallet.getBalance().add(amount)
                : wallet.getBalance().subtract(amount);
        if (newBalance.signum() < 0) {
            throw new InsufficientBalanceException();
        }
        wallet.setBalance(newBalance);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        tx.setDescription(description);
        return walletTransactionRepository.save(tx);
    }

    private Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByUser_Id(userId).orElseGet(() -> createWallet(userId));
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute mock payment signature", ex);
        }
    }

    private Wallet createWallet(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        try {
            return walletRepository.saveAndFlush(wallet);
        } catch (DataIntegrityViolationException race) {
            // Two first-time requests raced; the unique(user_id) constraint stopped the dup.
            return walletRepository.findByUser_Id(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        }
    }
}
