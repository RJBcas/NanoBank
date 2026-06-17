package com.nanobank.ledger.wallet.application.dto;

import com.nanobank.ledger.wallet.domain.model.WalletCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateWalletRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull WalletCategory category,
        @DecimalMin("0.00") BigDecimal initialBalance,
        @Size(max = 3) String currency,
        @Size(max = 500) String description
) {}
