package com.school.canteen.service.impl;

import com.school.canteen.config.PaymentProperties;
import com.school.canteen.dto.payment.PaymentInitiationResponse;
import com.school.canteen.dto.payment.VerifyPaymentRequest;
import com.school.canteen.dto.wallet.MockTopupCompleteRequest;
import com.school.canteen.dto.wallet.TopupInitResponse;
import com.school.canteen.dto.wallet.TopupRequest;
import com.school.canteen.dto.wallet.VerifyTopupRequest;
import com.school.canteen.dto.wallet.WalletResponse;
import com.school.canteen.dto.wallet.WalletTransactionResponse;
import com.school.canteen.entity.Wallet;
import com.school.canteen.entity.WalletTransaction;
import com.school.canteen.enums.TransactionType;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.InsufficientBalanceException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.WalletMapper;
import com.school.canteen.repository.PaymentRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.repository.WalletRepository;
import com.school.canteen.repository.WalletTransactionRepository;
import com.school.canteen.service.CreatePaymentCommand;
import com.school.canteen.service.PaymentService;
import com.school.canteen.service.WalletService;
import com.school.canteen.util.PageRequests;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The wallet's own REST contract (WalletController) is unchanged — every method here still
 * has the exact signature it always did. Internally, top-up creation/verification now goes
 * through {@link PaymentService} (useCase=WALLET_RECHARGE) instead of talking to a
 * {@code PaymentGateway} directly; {@code wallet_topups} is no longer written to (see
 * V15__payment_platform.sql — it is left as frozen historical data, superseded by the
 * generic {@code payments} table). {@link #debit}/{@link #credit} are untouched: they are
 * the ledger primitives {@link PaymentServiceImpl} itself calls back into, so ordering's
 * existing wallet-only fast path is not affected by any of this.
 */
@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentProperties paymentProperties;
    private final PaymentService paymentService;
    private final WalletMapper walletMapper;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository walletTransactionRepository,
                             PaymentRepository paymentRepository,
                             UserRepository userRepository,
                             PaymentProperties paymentProperties,
                             // @Lazy breaks the WalletServiceImpl <-> PaymentServiceImpl
                             // cycle: PaymentServiceImpl depends on WalletService (to move
                             // money), and this class depends on PaymentService (to create
                             // one) — without @Lazy here, Spring cannot construct either
                             // bean first and the context fails to start.
                             @Lazy PaymentService paymentService,
                             WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.paymentProperties = paymentProperties;
        this.paymentService = paymentService;
        this.walletMapper = walletMapper;
    }

    @Override
    @Transactional
    public WalletResponse getWallet(UUID userId) {
        return walletMapper.toWalletResponse(getOrCreateWallet(userId));
    }

    @Override
    @Transactional
    public List<WalletTransactionResponse> getTransactions(UUID userId, Integer page, Integer size) {
        Wallet wallet = getOrCreateWallet(userId);
        return walletTransactionRepository
                .findByWallet_IdOrderByCreatedAtDesc(wallet.getId(), PageRequests.of(page, size))
                .stream()
                .map(walletMapper::toTransactionResponse)
                .toList();
    }

    @Override
    @Transactional
    public TopupInitResponse initiateTopup(UUID userId, TopupRequest request) {
        if (request.amount().compareTo(paymentProperties.maxTopupAmount()) > 0) {
            throw new BadRequestException(
                    "Maximum top-up amount is " + paymentProperties.maxTopupAmount());
        }
        // Ensures the wallet row exists before payment creation touches anything wallet-shaped.
        getOrCreateWallet(userId);

        PaymentInitiationResponse payment = paymentService.createPayment(
                CreatePaymentCommand.walletRecharge(userId, request.amount(), null));

        return new TopupInitResponse(
                payment.paymentId(),
                payment.providerOrderId(),
                payment.pricing().subtotal(),
                payment.pricing().currency(),
                payment.providerKeyId(),
                payment.pricing().platformFee(),
                payment.pricing().grandTotal());
    }

    @Override
    @Transactional
    public WalletResponse verifyTopup(UUID userId, VerifyTopupRequest request) {
        // A scalar id lookup, not a full entity fetch — see findIdByProviderOrderId's
        // javadoc for why loading the entity here would break the PESSIMISTIC_WRITE lock
        // that paymentService.verifyPayment takes next, in this same transaction.
        UUID paymentId = paymentRepository.findIdByProviderOrderId(request.gatewayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Top-up not found"));
        paymentService.verifyPayment(userId, paymentId, new VerifyPaymentRequest(
                request.gatewayOrderId(), request.gatewayPaymentId(), request.signature()));
        return walletMapper.toWalletResponse(getOrCreateWallet(userId));
    }

    @Override
    @Transactional
    public WalletResponse mockCompleteTopup(UUID userId, MockTopupCompleteRequest request) {
        // Delegates to the general-purpose PaymentService.mockComplete (same gating on
        // app.payment.allow-mock-topup, same self-signed HMAC scheme) — kept as its own
        // wallet-shaped endpoint only for API-contract backward compatibility.
        UUID paymentId = paymentRepository.findIdByProviderOrderId(request.gatewayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Top-up not found"));
        paymentService.mockComplete(userId, paymentId);
        return walletMapper.toWalletResponse(getOrCreateWallet(userId));
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

    private Wallet createWallet(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        // ON CONFLICT DO NOTHING lets PostgreSQL absorb a concurrent creation silently.
        // The previous approach (insert, catch DataIntegrityViolationException, re-select)
        // could never work: in PostgreSQL a constraint violation aborts the whole
        // transaction, so the recovery query failed with "current transaction is aborted".
        // That is visible in the production logs whenever a new user's app fired
        // GET /wallet and GET /wallet/transactions in parallel.
        walletRepository.insertIfAbsent(UUID.randomUUID(), userId);
        return walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }
}
