package com.nanobank.ledger.transaction.infrastructure.rest;

import com.nanobank.ledger.auth.domain.model.User;
import com.nanobank.ledger.shared.dto.ApiResponse;
import com.nanobank.ledger.transaction.application.dto.CreateTransactionRequest;
import com.nanobank.ledger.transaction.application.dto.MoveTransactionRequest;
import com.nanobank.ledger.transaction.application.dto.PagedResponse;
import com.nanobank.ledger.transaction.application.dto.TransactionResponse;
import com.nanobank.ledger.transaction.application.service.TransactionService;
import com.nanobank.ledger.transaction.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Record incomes, expenses and retrieve transaction history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Create a transaction (INCOME or EXPENSE)")
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        TransactionResponse response = transactionService.create(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "List transactions for a wallet with filters (real-time filter support)")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> listByWallet(
            @AuthenticationPrincipal User user,
            @PathVariable UUID walletId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<TransactionResponse> response = transactionService.findByWallet(
                user.getId(), walletId, type, category, dateFrom, dateTo, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get a transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> findById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID transactionId
    ) {
        TransactionResponse response = transactionService.findById(user.getId(), transactionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{transactionId}/move")
    @Operation(summary = "Move a transaction to another wallet (Drag & Drop)")
    public ResponseEntity<ApiResponse<TransactionResponse>> move(
            @AuthenticationPrincipal User user,
            @PathVariable UUID transactionId,
            @Valid @RequestBody MoveTransactionRequest request
    ) {
        TransactionResponse response = transactionService.move(user.getId(), transactionId, request.destinationWalletId());
        return ResponseEntity.ok(ApiResponse.ok("Transaction moved successfully", response));
    }
}
