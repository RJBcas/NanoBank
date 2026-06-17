package com.nanobank.ledger.transaction.application.dto;

import com.nanobank.ledger.transaction.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull UUID walletId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 3) String currency,
        @Size(max = 100) String category,
        @Size(max = 500) String description,
        LocalDate occurredAt
) {}
