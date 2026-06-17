package com.nanobank.ledger.wallet.application.dto;

import com.nanobank.ledger.wallet.domain.model.Wallet;
import com.nanobank.ledger.wallet.domain.model.WalletCategory;
import com.nanobank.ledger.wallet.domain.model.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        String name,
        WalletCategory category,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getName(),
                wallet.getCategory(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getDescription(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
