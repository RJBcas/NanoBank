package com.nanobank.ledger.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletTransferRequest(
        @NotNull UUID destinationWalletId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
