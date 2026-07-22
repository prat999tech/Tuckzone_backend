package com.school.canteen.controller;

import com.school.canteen.dto.wallet.MockTopupCompleteRequest;
import com.school.canteen.dto.wallet.TopupInitResponse;
import com.school.canteen.dto.wallet.TopupRequest;
import com.school.canteen.dto.wallet.VerifyTopupRequest;
import com.school.canteen.dto.wallet.WalletResponse;
import com.school.canteen.dto.wallet.WalletTransactionResponse;
import com.school.canteen.security.AppUserDetails;
import com.school.canteen.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A user's own wallet. Every operation acts on the caller's wallet (derived from the JWT
 * principal), so one user can never read or top up another's wallet.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse wallet(@AuthenticationPrincipal AppUserDetails principal) {
        return walletService.getWallet(currentUserId(principal));
    }

    @GetMapping("/transactions")
    public List<WalletTransactionResponse> transactions(
            @AuthenticationPrincipal AppUserDetails principal) {
        return walletService.getTransactions(currentUserId(principal));
    }

    @PostMapping("/topup")
    public TopupInitResponse initiateTopup(@AuthenticationPrincipal AppUserDetails principal,
                                           @Valid @RequestBody TopupRequest request) {
        return walletService.initiateTopup(currentUserId(principal), request);
    }

    @PostMapping("/topup/verify")
    public WalletResponse verifyTopup(@AuthenticationPrincipal AppUserDetails principal,
                                      @Valid @RequestBody VerifyTopupRequest request) {
        return walletService.verifyTopup(currentUserId(principal), request);
    }

    @PostMapping("/topup/mock-complete")
    public WalletResponse mockCompleteTopup(@AuthenticationPrincipal AppUserDetails principal,
                                            @Valid @RequestBody MockTopupCompleteRequest request) {
        return walletService.mockCompleteTopup(currentUserId(principal), request);
    }

    private UUID currentUserId(AppUserDetails principal) {
        return principal.getUser().getId();
    }
}
