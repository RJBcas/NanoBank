package com.nanobank.ledger.transaction.application.dto;

import com.nanobank.ledger.transaction.domain.model.Transaction;
import com.nanobank.ledger.transaction.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID walletId,
        UUID userId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String category,
        String description,
        LocalDate occurredAt,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getWallet().getId(),
                t.getUser().getId(),
                t.getType(),
                t.getAmount(),
                t.getCurrency(),
                t.getCategory(),
                t.getDescription(),
                t.getOccurredAt(),
                t.getCreatedAt()
        );
    }
}
