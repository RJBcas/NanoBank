package com.nanobank.ledger.wallet.infrastructure.rest;

import com.nanobank.ledger.auth.domain.model.User;
import com.nanobank.ledger.shared.dto.ApiResponse;
import com.nanobank.ledger.wallet.application.dto.*;
import com.nanobank.ledger.wallet.application.service.WalletService;
import com.nanobank.ledger.wallet.domain.model.WalletCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet management — create, list, update, transfer")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @Operation(summary = "Create a new wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWalletRequest request
    ) {
        WalletResponse response = walletService.create(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List all wallets for the authenticated user")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) WalletCategory category
    ) {
        List<WalletResponse> wallets = walletService.findAll(user.getId(), category);
        return ResponseEntity.ok(ApiResponse.ok(wallets));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get wallet by ID")
    public ResponseEntity<ApiResponse<WalletResponse>> findById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID walletId
    ) {
        WalletResponse wallet = walletService.findById(user.getId(), walletId);
        return ResponseEntity.ok(ApiResponse.ok(wallet));
    }

    @PutMapping("/{walletId}")
    @Operation(summary = "Update wallet name, category or description")
    public ResponseEntity<ApiResponse<WalletResponse>> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID walletId,
            @Valid @RequestBody UpdateWalletRequest request
    ) {
        WalletResponse wallet = walletService.update(user.getId(), walletId, request);
        return ResponseEntity.ok(ApiResponse.ok(wallet));
    }

    @DeleteMapping("/{walletId}")
    @Operation(summary = "Deactivate a wallet (balance must be zero)")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @AuthenticationPrincipal User user,
            @PathVariable UUID walletId
    ) {
        walletService.deactivate(user.getId(), walletId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{walletId}/transfer")
    @Operation(summary = "Transfer funds between wallets (Drag & Drop)")
    public ResponseEntity<ApiResponse<Void>> transfer(
            @AuthenticationPrincipal User user,
            @PathVariable UUID walletId,
            @Valid @RequestBody WalletTransferRequest request
    ) {
        walletService.transfer(user.getId(), walletId, request.destinationWalletId(), request.amount());
        return ResponseEntity.ok(ApiResponse.ok("Transfer completed successfully", null));
    }
}
